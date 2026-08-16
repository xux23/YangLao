package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.dto.CareRecordDTO;
import com.eldercare.entity.CareRecord;

import java.util.List;

/**
 * 护理记录业务接口
 */
public interface CareRecordService {

    /**
     * 护理记录分页查询（家属仅能查关联老人）
     */
    Page<CareRecord> pageCareRecord(int page, int size, Long elderId, String planName,
                                    String startTime, String endTime);

    /**
     * 新增护理记录（自动记录操作人）
     */
    void addCareRecord(CareRecordDTO dto);

    /**
     * 修改护理记录（护士本人当天记录可改）
     */
    void updateCareRecord(Long id, CareRecordDTO dto);

    /**
     * 删除护理记录（仅当天记录可删）
     */
    void deleteCareRecord(Long id);

    /**
     * 按筛选条件查询护理记录（用于导出）
     */
    List<CareRecord> listCareRecordForExport(Long elderId, String startTime, String endTime);
}