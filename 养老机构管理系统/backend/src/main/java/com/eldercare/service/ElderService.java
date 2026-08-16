package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.dto.CheckinDTO;
import com.eldercare.dto.CheckoutDTO;
import com.eldercare.dto.ElderDTO;
import com.eldercare.dto.HealthDTO;
import com.eldercare.entity.ElderInfo;

import java.util.List;

/**
 * 老人档案业务接口
 */
public interface ElderService {

    /**
     * 老人分页查询，支持姓名、房间号、状态筛选
     */
    Page<ElderInfo> pageElder(int page, int size, String name, String roomNo, Integer status);

    /**
     * 老人详情
     */
    ElderInfo getElder(Long id);

    /**
     * 家属查看自己关联的老人
     */
    ElderInfo getMyElder();

    /**
     * 新增老人
     */
    void addElder(ElderDTO dto);

    /**
     * 修改老人
     */
    void updateElder(Long id, ElderDTO dto);

    /**
     * 删除老人（仅允许删除无业务数据的记录，仅管理员）
     */
    void deleteElder(Long id);

    /**
     * 入住登记：分配房间床位，状态置为在住
     */
    void checkin(Long id, CheckinDTO dto);

    /**
     * 退住登记：释放房间床位，状态置为已退住
     */
    void checkout(Long id, CheckoutDTO dto);

    /**
     * 按筛选条件查询老人列表（用于导出）
     */
    List<ElderInfo> listElderForExport(String name, Integer status);

    /**
     * 查询老人健康档案
     */
    String getHealthSummary(Long elderId);

    /**
     * 修改老人健康档案
     */
    void updateHealthSummary(Long elderId, HealthDTO dto);

    /**
     * 校验老人是否存在
     */
    void checkElderExists(Long elderId);
}