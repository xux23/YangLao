package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.dto.AuditDTO;
import com.eldercare.dto.VisitDTO;
import com.eldercare.entity.VisitAppointment;

/**
 * 探访预约业务接口
 */
public interface VisitService {

    /**
     * 家属提交探访预约（自动绑定当前家属账号）
     */
    void addVisit(VisitDTO dto);

    /**
     * 预约分页查询：家属只看自己的，机构看全部
     */
    Page<VisitAppointment> pageVisit(int page, int size, Integer status, Long elderId);

    /**
     * 审核预约：通过 / 驳回
     */
    void auditVisit(Long id, AuditDTO dto);

    /**
     * 探访结束，标记为已完成
     */
    void finishVisit(Long id);
}