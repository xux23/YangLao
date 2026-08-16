package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.AuditDTO;
import com.eldercare.dto.VisitDTO;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.SysUser;
import com.eldercare.entity.VisitAppointment;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.mapper.VisitAppointmentMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.VisitService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 探访预约业务实现：家属提交，机构审核，家属数据隔离
 */
@Service
public class VisitServiceImpl implements VisitService {

    @Autowired
    private VisitAppointmentMapper visitAppointmentMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Override
    public void addVisit(VisitDTO dto) {
        ElderInfo elder = elderInfoMapper.selectById(dto.getElderId());
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        // 家属只能替自己关联的老人预约探访
        if (elder.getFamilyId() == null || !elder.getFamilyId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只能为关联的老人提交探访预约");
        }
        if (dto.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "探访日期不能早于今天");
        }
        VisitAppointment visit = new VisitAppointment();
        BeanUtils.copyProperties(dto, visit);
        visit.setId(null);
        visit.setFamilyId(UserContext.getUserId());
        visit.setStatus(0); // 待审核
        visitAppointmentMapper.insert(visit);
    }

    @Override
    public Page<VisitAppointment> pageVisit(int page, int size, Integer status, Long elderId) {
        LambdaQueryWrapper<VisitAppointment> wrapper = new LambdaQueryWrapper<>();
        if ("family".equals(UserContext.getRole())) {
            // 家属只能看自己提交的预约，同样支持按状态筛选
            wrapper.eq(VisitAppointment::getFamilyId, UserContext.getUserId())
                    .eq(status != null, VisitAppointment::getStatus, status);
        } else {
            wrapper.eq(status != null, VisitAppointment::getStatus, status)
                    .eq(elderId != null, VisitAppointment::getElderId, elderId);
        }
        wrapper.orderByDesc(VisitAppointment::getCreateTime);
        Page<VisitAppointment> result = visitAppointmentMapper.selectPage(new Page<>(page, size), wrapper);
        fillNames(result.getRecords());
        return result;
    }

    @Override
    public void auditVisit(Long id, AuditDTO dto) {
        VisitAppointment visit = visitAppointmentMapper.selectById(id);
        if (visit == null) {
            throw new BusinessException(404, "探访预约不存在");
        }
        if (visit.getStatus() != null && visit.getStatus() != 0) {
            throw new BusinessException(400, "该预约已审核，请勿重复操作");
        }
        if (dto.getStatus() != 1 && dto.getStatus() != 2) {
            throw new BusinessException(400, "审核结果只能是 通过(1) 或 驳回(2)");
        }
        if (dto.getStatus() == 2 && (dto.getAuditRemark() == null || dto.getAuditRemark().isBlank())) {
            throw new BusinessException(400, "驳回时必须填写原因");
        }
        visit.setStatus(dto.getStatus());
        visit.setAuditRemark(dto.getAuditRemark());
        visitAppointmentMapper.updateById(visit);
    }

    @Override
    public void finishVisit(Long id) {
        VisitAppointment visit = visitAppointmentMapper.selectById(id);
        if (visit == null) {
            throw new BusinessException(404, "探访预约不存在");
        }
        if (visit.getStatus() == null || visit.getStatus() != 1) {
            throw new BusinessException(400, "仅已通过的预约可标记为完成");
        }
        visit.setStatus(3);
        visitAppointmentMapper.updateById(visit);
    }

    /**
     * 补充老人姓名与家属姓名
     */
    private void fillNames(List<VisitAppointment> visits) {
        if (visits == null || visits.isEmpty()) {
            return;
        }
        Set<Long> elderIds = new HashSet<>();
        Set<Long> familyIds = new HashSet<>();
        visits.forEach(v -> {
            elderIds.add(v.getElderId());
            familyIds.add(v.getFamilyId());
        });
        Map<Long, ElderInfo> elderMap = elderInfoMapper.selectBatchIds(elderIds).stream()
                .collect(Collectors.toMap(ElderInfo::getId, Function.identity()));
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(familyIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        visits.forEach(v -> {
            if (elderMap.containsKey(v.getElderId())) {
                v.setElderName(elderMap.get(v.getElderId()).getName());
            }
            if (userMap.containsKey(v.getFamilyId())) {
                v.setFamilyName(userMap.get(v.getFamilyId()).getRealName());
            }
        });
    }
}