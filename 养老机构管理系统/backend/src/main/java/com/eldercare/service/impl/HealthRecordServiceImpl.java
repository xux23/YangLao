package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.HealthRecordDTO;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.HealthRecord;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.HealthRecordMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.HealthRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 健康体征记录业务实现
 */
@Service
public class HealthRecordServiceImpl implements HealthRecordService {

    @Autowired
    private HealthRecordMapper healthRecordMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<HealthRecord> pageHealthRecord(int page, int size, Long elderId, String startTime, String endTime) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        // 家属只能查关联老人的体征记录
        if ("family".equals(UserContext.getRole())) {
            wrapper.eq(HealthRecord::getElderId, getMyElderId());
        } else {
            wrapper.eq(elderId != null, HealthRecord::getElderId, elderId);
        }
        try {
            if (StringUtils.hasText(startTime)) {
                wrapper.ge(HealthRecord::getRecordTime, LocalDateTime.parse(startTime, DATE_TIME_FORMATTER));
            }
            if (StringUtils.hasText(endTime)) {
                wrapper.le(HealthRecord::getRecordTime, LocalDateTime.parse(endTime, DATE_TIME_FORMATTER));
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        wrapper.orderByDesc(HealthRecord::getRecordTime);

        Page<HealthRecord> result = healthRecordMapper.selectPage(new Page<>(page, size), wrapper);
        fillElderNames(result.getRecords());
        return result;
    }

    @Override
    public void addHealthRecord(HealthRecordDTO dto) {
        if (elderInfoMapper.selectById(dto.getElderId()) == null) {
            throw new BusinessException(404, "老人不存在");
        }
        HealthRecord record = new HealthRecord();
        BeanUtils.copyProperties(dto, record);
        record.setId(null);
        record.setRecorderId(UserContext.getUserId()); // 自动记录录入人
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }
        healthRecordMapper.insert(record);
    }

    @Override
    public void updateHealthRecord(Long id, HealthRecordDTO dto) {
        HealthRecord record = healthRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "体征记录不存在");
        }
        record.setBloodPressure(dto.getBloodPressure());
        record.setHeartRate(dto.getHeartRate());
        record.setTemperature(dto.getTemperature());
        record.setBloodSugar(dto.getBloodSugar());
        record.setRecordTime(dto.getRecordTime() != null ? dto.getRecordTime() : LocalDateTime.now());
        record.setRemark(dto.getRemark());
        healthRecordMapper.updateById(record);
    }

    @Override
    public void deleteHealthRecord(Long id) {
        if (healthRecordMapper.selectById(id) == null) {
            throw new BusinessException(404, "体征记录不存在");
        }
        healthRecordMapper.deleteById(id);
    }

    /**
     * 补充老人姓名
     */
    private void fillElderNames(List<HealthRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> elderIds = records.stream().map(HealthRecord::getElderId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, ElderInfo> elderMap = elderInfoMapper.selectBatchIds(elderIds).stream()
                .collect(Collectors.toMap(ElderInfo::getId, Function.identity()));
        records.forEach(r -> {
            if (elderMap.containsKey(r.getElderId())) {
                r.setElderName(elderMap.get(r.getElderId()).getName());
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