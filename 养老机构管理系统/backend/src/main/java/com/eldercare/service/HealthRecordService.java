package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.dto.HealthRecordDTO;
import com.eldercare.entity.HealthRecord;

/**
 * 健康体征记录业务接口
 */
public interface HealthRecordService {

    /**
     * 体征记录分页查询（家属仅能查关联老人）
     */
    Page<HealthRecord> pageHealthRecord(int page, int size, Long elderId, String startTime, String endTime);

    /**
     * 新增体征记录（自动记录录入人）
     */
    void addHealthRecord(HealthRecordDTO dto);

    /**
     * 修改体征记录
     */
    void updateHealthRecord(Long id, HealthRecordDTO dto);

    /**
     * 删除体征记录
     */
    void deleteHealthRecord(Long id);
}