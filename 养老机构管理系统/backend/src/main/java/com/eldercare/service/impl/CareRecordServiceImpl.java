package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.CareRecordDTO;
import com.eldercare.entity.CareRecord;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.SysUser;
import com.eldercare.mapper.CareRecordMapper;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.CareRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 护理记录业务实现
 */
@Service
public class CareRecordServiceImpl implements CareRecordService {

    @Autowired
    private CareRecordMapper careRecordMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    @Autowired
    private SysUserMapper userMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<CareRecord> pageCareRecord(int page, int size, Long elderId, String planName,
                                           String startTime, String endTime) {
        LambdaQueryWrapper<CareRecord> wrapper = new LambdaQueryWrapper<>();
        // 家属只能查关联老人的护理记录
        if ("family".equals(UserContext.getRole())) {
            Long myElderId = getMyElderId();
            wrapper.eq(CareRecord::getElderId, myElderId);
        } else {
            wrapper.eq(elderId != null, CareRecord::getElderId, elderId);
        }
        wrapper.like(StringUtils.hasText(planName), CareRecord::getPlanName, planName);
        try {
            if (StringUtils.hasText(startTime)) {
                wrapper.ge(CareRecord::getCareTime, LocalDateTime.parse(startTime, DATE_TIME_FORMATTER));
            }
            if (StringUtils.hasText(endTime)) {
                wrapper.le(CareRecord::getCareTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        wrapper.orderByDesc(CareRecord::getCareTime);

        Page<CareRecord> result = careRecordMapper.selectPage(new Page<>(page, size), wrapper);
        fillNames(result.getRecords());
        return result;
    }

    @Override
    public void addCareRecord(CareRecordDTO dto) {
        if (elderInfoMapper.selectById(dto.getElderId()) == null) {
            throw new BusinessException(404, "老人不存在");
        }
        CareRecord record = new CareRecord();
        BeanUtils.copyProperties(dto, record);
        record.setId(null);
        record.setNurseId(UserContext.getUserId()); // 自动记录操作人
        if (record.getCareTime() == null) {
            record.setCareTime(LocalDateTime.now());
        }
        careRecordMapper.insert(record);
    }

    @Override
    public void updateCareRecord(Long id, CareRecordDTO dto) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "护理记录不存在");
        }
        // 业务约束：仅可修改当天的记录
        if (!record.getCareTime().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessException(400, "仅可修改当天的护理记录");
        }
        record.setPlanName(dto.getPlanName());
        record.setPlanFrequency(dto.getPlanFrequency());
        record.setCareContent(dto.getCareContent());
        record.setRemark(dto.getRemark());
        careRecordMapper.updateById(record);
    }

    @Override
    public void deleteCareRecord(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "护理记录不存在");
        }
        if (!record.getCareTime().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessException(400, "仅可删除当天的护理记录");
        }
        careRecordMapper.deleteById(id);
    }

    @Override
    public List<CareRecord> listCareRecordForExport(Long elderId, String startTime, String endTime) {
        LambdaQueryWrapper<CareRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(elderId != null, CareRecord::getElderId, elderId);
        try {
            if (StringUtils.hasText(startTime)) {
                wrapper.ge(CareRecord::getCareTime, LocalDateTime.parse(startTime, DATE_TIME_FORMATTER));
            }
            if (StringUtils.hasText(endTime)) {
                wrapper.le(CareRecord::getCareTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        wrapper.orderByAsc(CareRecord::getCareTime);
        List<CareRecord> list = careRecordMapper.selectList(wrapper);
        fillNames(list);
        return list;
    }

    /**
     * 补充老人姓名与护理人员姓名
     */
    private void fillNames(List<CareRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> elderIds = new HashSet<>();
        Set<Long> nurseIds = new HashSet<>();
        records.forEach(r -> {
            elderIds.add(r.getElderId());
            if (r.getNurseId() != null) {
                nurseIds.add(r.getNurseId());
            }
        });
        Map<Long, ElderInfo> elderMap = elderInfoMapper.selectBatchIds(elderIds).stream()
                .collect(Collectors.toMap(ElderInfo::getId, Function.identity()));
        // nurseIds 可能为空，selectBatchIds 传空集合会生成非法 SQL，需先判断
        Map<Long, SysUser> nurseMap;
        if (nurseIds.isEmpty()) {
            nurseMap = new HashMap<>();
        } else {
            nurseMap = userMapper.selectBatchIds(nurseIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        }
        records.forEach(r -> {
            if (elderMap.containsKey(r.getElderId())) {
                r.setElderName(elderMap.get(r.getElderId()).getName());
            }
            if (r.getNurseId() != null && nurseMap.containsKey(r.getNurseId())) {
                r.setNurseName(nurseMap.get(r.getNurseId()).getRealName());
            }
        });
    }

    /**
     * 获取当前家属关联的老人 ID
     */
    private Long getMyElderId() {
        ElderInfo elder = elderInfoMapper.selectOne(
                new LambdaQueryWrapper<ElderInfo>().eq(ElderInfo::getFamilyId, UserContext.getUserId()));
        if (elder == null) {
            throw new BusinessException(400, "您的账号还没有关联老人，请联系机构");
        }
        return elder.getId();
    }
}