package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.CheckinDTO;
import com.eldercare.dto.CheckoutDTO;
import com.eldercare.dto.ElderDTO;
import com.eldercare.dto.HealthDTO;
import com.eldercare.entity.CareRecord;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.HealthRecord;
import com.eldercare.entity.MedicinePlan;
import com.eldercare.entity.Message;
import com.eldercare.entity.SysUser;
import com.eldercare.entity.VisitAppointment;
import com.eldercare.mapper.CareRecordMapper;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.HealthRecordMapper;
import com.eldercare.mapper.MedicinePlanMapper;
import com.eldercare.mapper.MessageMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.mapper.VisitAppointmentMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.ElderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 老人档案业务实现
 * 含入住/退住登记、健康档案维护、名单导出查询、家属数据隔离
 */
@Service
public class ElderServiceImpl implements ElderService {

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private CareRecordMapper careRecordMapper;

    @Autowired
    private HealthRecordMapper healthRecordMapper;

    @Autowired
    private MedicinePlanMapper medicinePlanMapper;

    @Autowired
    private VisitAppointmentMapper visitAppointmentMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public Page<ElderInfo> pageElder(int page, int size, String name, String roomNo, Integer status) {
        LambdaQueryWrapper<ElderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), ElderInfo::getName, name)
                .like(StringUtils.hasText(roomNo), ElderInfo::getRoomNo, roomNo)
                .eq(status != null, ElderInfo::getStatus, status)
                .orderByDesc(ElderInfo::getId);
        Page<ElderInfo> result = elderInfoMapper.selectPage(new Page<>(page, size), wrapper);
        fillFamilyNames(result.getRecords());
        return result;
    }

    @Override
    public ElderInfo getElder(Long id) {
        ElderInfo elder = elderInfoMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        // 家属只能查看自己关联的老人
        checkFamilyOwnership(id);
        fillFamilyNames(List.of(elder));
        return elder;
    }

    @Override
    public ElderInfo getMyElder() {
        Long userId = UserContext.getUserId();
        ElderInfo elder = elderInfoMapper.selectOne(
                new LambdaQueryWrapper<ElderInfo>().eq(ElderInfo::getFamilyId, userId));
        if (elder == null) {
            throw new BusinessException(400, "您的账号还没有关联老人，请联系机构");
        }
        fillFamilyNames(List.of(elder));
        return elder;
    }

    @Override
    public void addElder(ElderDTO dto) {
        // 身份证号唯一校验
        if (StringUtils.hasText(dto.getIdCard())
                && elderInfoMapper.selectCount(
                        new LambdaQueryWrapper<ElderInfo>().eq(ElderInfo::getIdCard, dto.getIdCard())) > 0) {
            throw new BusinessException(400, "身份证号已存在");
        }
        ElderInfo elder = new ElderInfo();
        BeanUtils.copyProperties(dto, elder);
        elder.setId(null);
        elder.setStatus(1); // 新增默认在住，房间床位通过入住登记分配
        elderInfoMapper.insert(elder);
    }

    @Override
    public void updateElder(Long id, ElderDTO dto) {
        ElderInfo elder = elderInfoMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        // 身份证号：传了才校验并修改，不传保持不变
        if (StringUtils.hasText(dto.getIdCard()) && !dto.getIdCard().equals(elder.getIdCard())) {
            if (elderInfoMapper.selectCount(
                    new LambdaQueryWrapper<ElderInfo>().eq(ElderInfo::getIdCard, dto.getIdCard())) > 0) {
                throw new BusinessException(400, "身份证号已存在");
            }
            elder.setIdCard(dto.getIdCard());
        }
        elder.setName(dto.getName());
        elder.setGender(dto.getGender());
        elder.setBirthday(dto.getBirthday());
        elder.setPhone(dto.getPhone());
        elder.setEmergencyContact(dto.getEmergencyContact());
        elder.setEmergencyPhone(dto.getEmergencyPhone());
        elder.setHealthSummary(dto.getHealthSummary());
        elder.setFamilyId(dto.getFamilyId());
        elderInfoMapper.updateById(elder);
    }

    @Override
    public void deleteElder(Long id) {
        ElderInfo elder = elderInfoMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        // 已有业务数据的老人不允许删除，保证记录可追溯
        if (careRecordMapper.selectCount(
                new LambdaQueryWrapper<CareRecord>().eq(CareRecord::getElderId, id)) > 0
                || healthRecordMapper.selectCount(
                        new LambdaQueryWrapper<HealthRecord>().eq(HealthRecord::getElderId, id)) > 0
                || medicinePlanMapper.selectCount(
                        new LambdaQueryWrapper<MedicinePlan>().eq(MedicinePlan::getElderId, id)) > 0
                || visitAppointmentMapper.selectCount(
                        new LambdaQueryWrapper<VisitAppointment>().eq(VisitAppointment::getElderId, id)) > 0
                || messageMapper.selectCount(
                        new LambdaQueryWrapper<Message>().eq(Message::getElderId, id)) > 0) {
            throw new BusinessException(400, "该老人已有护理/健康/探访等业务数据，无法删除");
        }
        elderInfoMapper.deleteById(id);
    }

    @Override
    public void checkin(Long id, CheckinDTO dto) {
        ElderInfo elder = elderInfoMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        // 退住后重新入住也走这里，直接覆盖房间床位即可
        elder.setRoomNo(dto.getRoomNo());
        elder.setBedNo(dto.getBedNo());
        elder.setCheckinTime(dto.getCheckinTime() != null ? dto.getCheckinTime() : LocalDate.now());
        elder.setCheckoutTime(null);
        elder.setStatus(1);
        elderInfoMapper.updateById(elder);
    }

    @Override
    public void checkout(Long id, CheckoutDTO dto) {
        ElderInfo elder = elderInfoMapper.selectById(id);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        if (elder.getStatus() != null && elder.getStatus() == 0) {
            throw new BusinessException(400, "该老人已退住，请勿重复操作");
        }
        // 释放房间床位（房间号/床位号置空）
        // 注意：updateById 默认不更新 null 字段，必须用 update wrapper 显式 set null
        elderInfoMapper.update(null, new LambdaUpdateWrapper<ElderInfo>()
                .eq(ElderInfo::getId, id)
                .set(ElderInfo::getStatus, 0)
                .set(ElderInfo::getRoomNo, null)
                .set(ElderInfo::getBedNo, null)
                .set(ElderInfo::getCheckoutTime,
                        dto.getCheckoutTime() != null ? dto.getCheckoutTime() : LocalDate.now()));
    }

    @Override
    public List<ElderInfo> listElderForExport(String name, Integer status) {
        LambdaQueryWrapper<ElderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), ElderInfo::getName, name)
                .eq(status != null, ElderInfo::getStatus, status)
                .orderByAsc(ElderInfo::getId);
        List<ElderInfo> list = elderInfoMapper.selectList(wrapper);
        fillFamilyNames(list);
        return list;
    }

    @Override
    public String getHealthSummary(Long elderId) {
        ElderInfo elder = elderInfoMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        checkFamilyOwnership(elderId);
        return elder.getHealthSummary();
    }

    @Override
    public void updateHealthSummary(Long elderId, HealthDTO dto) {
        ElderInfo elder = elderInfoMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        elder.setHealthSummary(dto.getHealthSummary());
        elderInfoMapper.updateById(elder);
    }

    @Override
    public void checkElderExists(Long elderId) {
        if (elderInfoMapper.selectById(elderId) == null) {
            throw new BusinessException(404, "老人不存在");
        }
    }

    /**
     * 补充老人列表中的家属姓名
     */
    private void fillFamilyNames(List<ElderInfo> elders) {
        if (elders == null || elders.isEmpty()) {
            return;
        }
        List<Long> familyIds = elders.stream()
                .map(ElderInfo::getFamilyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (familyIds.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(familyIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        elders.forEach(elder -> {
            if (elder.getFamilyId() != null && userMap.containsKey(elder.getFamilyId())) {
                elder.setFamilyName(userMap.get(elder.getFamilyId()).getRealName());
            }
        });
    }

    /**
     * 家属数据隔离：家属访问老人数据前校验归属
     */
    private void checkFamilyOwnership(Long elderId) {
        if ("family".equals(UserContext.getRole())) {
            ElderInfo elder = elderInfoMapper.selectById(elderId);
            if (elder == null || elder.getFamilyId() == null
                    || !elder.getFamilyId().equals(UserContext.getUserId())) {
                throw new BusinessException(403, "无权访问该老人的数据");
            }
        }
    }
}