package com.eldercare.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 护理记录导出 Excel 视图对象
 */
@Data
public class CareRecordExportVO {

    @ExcelProperty("老人姓名")
    private String elderName;

    @ExcelProperty("护理项目")
    private String planName;

    @ExcelProperty("频次")
    private String planFrequency;

    @ExcelProperty("护理内容")
    private String careContent;

    @ExcelProperty("执行人")
    private String nurseName;

    @ExcelProperty("执行时间")
    private String careTime;

    @ExcelProperty("交接备注")
    private String remark;
}