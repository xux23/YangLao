package com.eldercare.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.CareRecordDTO;
import com.eldercare.entity.CareRecord;
import com.eldercare.security.RequireRole;
import com.eldercare.service.CareRecordService;
import com.eldercare.vo.CareRecordExportVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 护理记录接口
 */
@RestController
@RequestMapping("/api/care-records")
public class CareRecordController {

    @Autowired
    private CareRecordService careRecordService;

    /**
     * 护理记录分页查询（家属仅能查关联老人）
     */
    @GetMapping
    @RequireRole({"admin", "nurse", "family"})
    public Result<Page<CareRecord>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long elderId,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(
                careRecordService.pageCareRecord(page, size, elderId, planName, startTime, endTime));
    }

    /**
     * 新增护理记录（护理人员）
     */
    @OperLog("新增护理记录")
    @PostMapping
    @RequireRole({"admin", "nurse"})
    public Result<Void> add(@Valid @RequestBody CareRecordDTO dto) {
        careRecordService.addCareRecord(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 修改护理记录（护理人员，仅当天记录）
     */
    @OperLog("修改护理记录")
    @PutMapping("/{id}")
    @RequireRole({"admin", "nurse"})
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CareRecordDTO dto) {
        careRecordService.updateCareRecord(id, dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除护理记录（护理人员，仅当天记录）
     */
    @OperLog("删除护理记录")
    @DeleteMapping("/{id}")
    @RequireRole({"admin", "nurse"})
    public Result<Void> delete(@PathVariable Long id) {
        careRecordService.deleteCareRecord(id);
        return Result.success("删除成功", null);
    }

    /**
     * 护理记录导出 Excel
     */
    @GetMapping("/export")
    @RequireRole({"admin", "nurse"})
    public void export(@RequestParam(required = false) Long elderId,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws IOException {
        List<CareRecord> list = careRecordService.listCareRecordForExport(elderId, startTime, endTime);
        List<CareRecordExportVO> voList = list.stream().map(r -> {
            CareRecordExportVO vo = new CareRecordExportVO();
            vo.setElderName(r.getElderName());
            vo.setPlanName(r.getPlanName());
            vo.setPlanFrequency(r.getPlanFrequency());
            vo.setCareContent(r.getCareContent());
            vo.setNurseName(r.getNurseName());
            vo.setCareTime(r.getCareTime() != null
                    ? r.getCareTime().toLocalDate() + " " + r.getCareTime().toLocalTime().withSecond(0) : "");
            vo.setRemark(r.getRemark());
            return vo;
        }).toList();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("护理记录.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), CareRecordExportVO.class)
                .sheet("护理记录")
                .doWrite(voList);
    }
}