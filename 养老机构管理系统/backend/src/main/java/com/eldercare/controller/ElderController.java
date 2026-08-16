package com.eldercare.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.CheckinDTO;
import com.eldercare.dto.CheckoutDTO;
import com.eldercare.dto.ElderDTO;
import com.eldercare.dto.HealthDTO;
import com.eldercare.entity.ElderInfo;
import com.eldercare.security.RequireRole;
import com.eldercare.service.ElderService;
import com.eldercare.vo.ElderExportVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老人档案接口
 */
@RestController
@RequestMapping("/api/elders")
public class ElderController {

    @Autowired
    private ElderService elderService;

    /**
     * 老人分页查询（管理员、护理人员）
     */
    @GetMapping
    @RequireRole({"admin", "nurse"})
    public Result<Page<ElderInfo>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String roomNo,
            @RequestParam(required = false) Integer status) {
        return Result.success(elderService.pageElder(page, size, name, roomNo, status));
    }

    /**
     * 家属查看自己关联的老人（扩展接口：家属入口需要老人 ID）
     */
    @GetMapping("/my")
    @RequireRole("family")
    public Result<ElderInfo> getMyElder() {
        return Result.success(elderService.getMyElder());
    }

    /**
     * 老人详情（家属仅能查关联老人）
     */
    @GetMapping("/{id}")
    @RequireRole({"admin", "nurse", "family"})
    public Result<ElderInfo> detail(@PathVariable Long id) {
        return Result.success(elderService.getElder(id));
    }

    /**
     * 新增老人（管理员、护理人员）
     */
    @OperLog("新增老人")
    @PostMapping
    @RequireRole({"admin", "nurse"})
    public Result<Void> add(@Valid @RequestBody ElderDTO dto) {
        elderService.addElder(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 修改老人
     */
    @OperLog("修改老人")
    @PutMapping("/{id}")
    @RequireRole({"admin", "nurse"})
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ElderDTO dto) {
        elderService.updateElder(id, dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除老人（仅管理员，且无业务数据才允许删除）
     */
    @OperLog("删除老人")
    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        elderService.deleteElder(id);
        return Result.success("删除成功", null);
    }

    /**
     * 入住登记：分配房间床位
     */
    @OperLog("入住登记")
    @PostMapping("/{id}/checkin")
    @RequireRole({"admin", "nurse"})
    public Result<Void> checkin(@PathVariable Long id, @Valid @RequestBody CheckinDTO dto) {
        elderService.checkin(id, dto);
        return Result.success("入住登记成功", null);
    }

    /**
     * 退住登记：释放房间床位
     */
    @OperLog("退住登记")
    @PostMapping("/{id}/checkout")
    @RequireRole({"admin", "nurse"})
    public Result<Void> checkout(@PathVariable Long id, @Valid @RequestBody CheckoutDTO dto) {
        elderService.checkout(id, dto);
        return Result.success("退住登记成功", null);
    }

    /**
     * 老人名单导出 Excel
     */
    @GetMapping("/export")
    @RequireRole({"admin", "nurse"})
    public void export(@RequestParam(required = false) String name,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        List<ElderInfo> list = elderService.listElderForExport(name, status);
        // 转成导出视图对象
        List<ElderExportVO> voList = list.stream().map(e -> {
            ElderExportVO vo = new ElderExportVO();
            vo.setName(e.getName());
            vo.setGender(e.getGender() != null && e.getGender() == 1 ? "男" : "女");
            vo.setBirthday(e.getBirthday() != null ? e.getBirthday().toString() : "");
            vo.setIdCard(e.getIdCard());
            vo.setPhone(e.getPhone());
            vo.setRoomNo(e.getRoomNo());
            vo.setBedNo(e.getBedNo());
            vo.setCheckinTime(e.getCheckinTime() != null ? e.getCheckinTime().toString() : "");
            vo.setStatus(e.getStatus() != null && e.getStatus() == 1 ? "在住" : "已退住");
            vo.setHealthSummary(e.getHealthSummary());
            return vo;
        }).toList();

        setExcelResponseHeader(response, "老人名单.xlsx");
        EasyExcel.write(response.getOutputStream(), ElderExportVO.class)
                .sheet("老人名单")
                .doWrite(voList);
    }

    /**
     * 查询老人健康档案
     */
    @GetMapping("/{id}/health")
    @RequireRole({"admin", "nurse", "family"})
    public Result<Map<String, Object>> getHealth(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("elderId", id);
        data.put("healthSummary", elderService.getHealthSummary(id));
        return Result.success(data);
    }

    /**
     * 修改老人健康档案（护理人员维护）
     */
    @OperLog("修改健康档案")
    @PutMapping("/{id}/health")
    @RequireRole({"admin", "nurse"})
    public Result<Void> updateHealth(@PathVariable Long id, @Valid @RequestBody HealthDTO dto) {
        elderService.updateHealthSummary(id, dto);
        return Result.success("健康档案已更新", null);
    }

    /**
     * 设置 Excel 下载响应头（UTF-8 编码文件名，支持中文）
     */
    private void setExcelResponseHeader(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedName);
    }
}