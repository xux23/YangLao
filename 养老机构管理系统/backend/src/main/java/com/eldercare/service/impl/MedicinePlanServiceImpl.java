package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.MedicinePlanDTO;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.MedicinePlan;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.MedicinePlanMapper;
import com.eldercare.service.MedicinePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用药计划/任务业务实现
 *
 * 核心设计：任务按需生成，不提前占库。
 * 1. 录入计划：为当天每个服药时间点生成一行"待执行"任务；
 * 2. 查询某日任务：先做逾期扫描（plan_date 早于当天且未执行的状态置为已逾期），
 *    当天没有任务行时，复制"最近一天"的任务行生成当天任务（延续服药方案）；
 * 3. 确认执行：状态置为已执行并记录确认时间；
 * 4. 停用计划：删除今天及以后的任务行，历史行标记停用，次日不再自动延续。
 */
@Service
public class MedicinePlanServiceImpl implements MedicinePlanService {

    @Autowired
    private MedicinePlanMapper medicinePlanMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    @Override
    @Transactional
    public void addPlan(MedicinePlanDTO dto) {
        if (elderInfoMapper.selectById(dto.getElderId()) == null) {
            throw new BusinessException(404, "老人不存在");
        }
        LocalDate today = LocalDate.now();
        for (String time : dto.getTimes()) {
            LocalTime planTime = LocalTime.parse(time);
            // 同一天同一个时间点已生成过任务则跳过，避免重复
            Long count = medicinePlanMapper.selectCount(
                    new LambdaQueryWrapper<MedicinePlan>()
                            .eq(MedicinePlan::getElderId, dto.getElderId())
                            .eq(MedicinePlan::getMedicineName, dto.getMedicineName())
                            .eq(MedicinePlan::getPlanDate, today)
                            .eq(MedicinePlan::getPlanTime, planTime));
            if (count > 0) {
                continue;
            }
            MedicinePlan plan = new MedicinePlan();
            plan.setElderId(dto.getElderId());
            plan.setMedicineName(dto.getMedicineName());
            plan.setDosage(dto.getDosage());
            plan.setPlanDate(today);
            plan.setPlanTime(planTime);
            plan.setStatus(0);   // 待执行
            plan.setDisabled(0); // 正常
            medicinePlanMapper.insert(plan);
        }
    }

    @Override
    public List<MedicinePlan> listPlans(Long elderId) {
        if (elderId == null) {
            throw new BusinessException(400, "请先选择老人");
        }
        // 找到该老人"最近一天"的任务行（即当前生效的服药方案），排除已停用的
        MedicinePlan recent = medicinePlanMapper.selectOne(
                new LambdaQueryWrapper<MedicinePlan>()
                        .eq(MedicinePlan::getElderId, elderId)
                        .eq(MedicinePlan::getDisabled, 0)
                        .orderByDesc(MedicinePlan::getPlanDate)
                        .orderByDesc(MedicinePlan::getPlanTime)
                        .last("LIMIT 1"));
        if (recent == null) {
            return new ArrayList<>();
        }
        List<MedicinePlan> plans = medicinePlanMapper.selectList(
                new LambdaQueryWrapper<MedicinePlan>()
                        .eq(MedicinePlan::getElderId, elderId)
                        .eq(MedicinePlan::getPlanDate, recent.getPlanDate())
                        .eq(MedicinePlan::getDisabled, 0)
                        .orderByAsc(MedicinePlan::getPlanTime));
        fillElderNames(plans);
        return plans;
    }

    @Override
    @Transactional
    public void disablePlan(Long id) {
        MedicinePlan plan = medicinePlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(404, "用药计划不存在");
        }
        // 1. 删除该老人该药今天及以后的任务行，历史任务保留作为档案
        medicinePlanMapper.delete(new LambdaQueryWrapper<MedicinePlan>()
                .eq(MedicinePlan::getElderId, plan.getElderId())
                .eq(MedicinePlan::getMedicineName, plan.getMedicineName())
                .ge(MedicinePlan::getPlanDate, LocalDate.now()));
        // 2. 剩余历史行标记停用，防止次日查询按需生成出新任务
        medicinePlanMapper.update(null, new LambdaUpdateWrapper<MedicinePlan>()
                .eq(MedicinePlan::getElderId, plan.getElderId())
                .eq(MedicinePlan::getMedicineName, plan.getMedicineName())
                .set(MedicinePlan::getDisabled, 1));
    }

    @Override
    @Transactional
    public List<MedicinePlan> listTasks(LocalDate date, Long elderId) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 1. 逾期扫描：把"早于今天"仍未执行的任务标记为已逾期。
        //    注意按真实今天比较，而不是查询的目标日期，
        //    否则查询明天任务时会把今天还没执行的任务误判为逾期。
        LambdaUpdateWrapper<MedicinePlan> updateWrapper = new LambdaUpdateWrapper<MedicinePlan>()
                .eq(elderId != null, MedicinePlan::getElderId, elderId)
                .lt(MedicinePlan::getPlanDate, LocalDate.now())
                .eq(MedicinePlan::getStatus, 0)
                .set(MedicinePlan::getStatus, 2);
        medicinePlanMapper.update(null, updateWrapper);

        // 2. 查询当天任务
        LambdaQueryWrapper<MedicinePlan> queryWrapper = new LambdaQueryWrapper<MedicinePlan>()
                .eq(elderId != null, MedicinePlan::getElderId, elderId)
                .eq(MedicinePlan::getPlanDate, targetDate)
                .orderByAsc(MedicinePlan::getPlanTime);
        List<MedicinePlan> tasks = medicinePlanMapper.selectList(queryWrapper);

        // 3. 当天还没有任务且查询的是今天或未来 → 按最近一天的方案复制生成。
        //    可为空（管理员查询全部老人当天任务）时不生成，避免无限复制。
        if (elderId != null && tasks.isEmpty() && !targetDate.isBefore(LocalDate.now())) {
            tasks = generateTasks(elderId, targetDate);
        }
        fillElderNames(tasks);
        return tasks;
    }

    @Override
    public MedicinePlan completeTask(Long id) {
        MedicinePlan task = medicinePlanMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "用药任务不存在");
        }
        if (task.getStatus() != null && task.getStatus() == 1) {
            throw new BusinessException(400, "该任务已确认执行，请勿重复操作");
        }
        task.setStatus(1);
        task.setConfirmTime(LocalDateTime.now());
        medicinePlanMapper.updateById(task);
        return task;
    }

    @Override
    @Transactional
    public List<MedicinePlan> listOverdueTasks(Long elderId) {
        // 查询前先做一次逾期扫描
        medicinePlanMapper.update(null, new LambdaUpdateWrapper<MedicinePlan>()
                .eq(elderId != null, MedicinePlan::getElderId, elderId)
                .lt(MedicinePlan::getPlanDate, LocalDate.now())
                .eq(MedicinePlan::getStatus, 0)
                .set(MedicinePlan::getStatus, 2));
        List<MedicinePlan> tasks = medicinePlanMapper.selectList(
                new LambdaQueryWrapper<MedicinePlan>()
                        .eq(elderId != null, MedicinePlan::getElderId, elderId)
                        .eq(MedicinePlan::getStatus, 2)
                        .orderByDesc(MedicinePlan::getPlanDate)
                        .orderByAsc(MedicinePlan::getPlanTime));
        fillElderNames(tasks);
        return tasks;
    }

    /**
     * 按"最近一天"的任务行复制生成目标日期的任务。
     * 最近一天必须是未停用的方案，否则说明计划已停用，不再延续。
     */
    private List<MedicinePlan> generateTasks(Long elderId, LocalDate targetDate) {
        // 找最近一天（日期小于目标日期）且未停用的任务行
        MedicinePlan recent = medicinePlanMapper.selectOne(
                new LambdaQueryWrapper<MedicinePlan>()
                        .eq(MedicinePlan::getElderId, elderId)
                        .eq(MedicinePlan::getDisabled, 0)
                        .lt(MedicinePlan::getPlanDate, targetDate)
                        .orderByDesc(MedicinePlan::getPlanDate)
                        .last("LIMIT 1"));
        if (recent == null) {
            return new ArrayList<>();
        }
        List<MedicinePlan> sourceRows = medicinePlanMapper.selectList(
                new LambdaQueryWrapper<MedicinePlan>()
                        .eq(MedicinePlan::getElderId, elderId)
                        .eq(MedicinePlan::getPlanDate, recent.getPlanDate())
                        .eq(MedicinePlan::getDisabled, 0));
        List<MedicinePlan> generated = new ArrayList<>();
        for (MedicinePlan source : sourceRows) {
            MedicinePlan copy = new MedicinePlan();
            copy.setElderId(elderId);
            copy.setMedicineName(source.getMedicineName());
            copy.setDosage(source.getDosage());
            copy.setPlanDate(targetDate);
            copy.setPlanTime(source.getPlanTime());
            copy.setStatus(0);
            copy.setDisabled(0);
            medicinePlanMapper.insert(copy);
            generated.add(copy);
        }
        // 按服药时间点排序，保证列表顺序稳定
        generated.sort((a, b) -> a.getPlanTime().compareTo(b.getPlanTime()));
        return generated;
    }

    /**
     * 补充老人姓名
     */
    private void fillElderNames(List<MedicinePlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        Set<Long> elderIds = plans.stream().map(MedicinePlan::getElderId)
                .collect(Collectors.toSet());
        List<ElderInfo> elders = elderInfoMapper.selectBatchIds(elderIds);
        Map<Long, ElderInfo> elderMap = elders.stream()
                .collect(Collectors.toMap(ElderInfo::getId, Function.identity()));
        plans.forEach(p -> {
            if (elderMap.containsKey(p.getElderId())) {
                p.setElderName(elderMap.get(p.getElderId()).getName());
            }
        });
    }
}