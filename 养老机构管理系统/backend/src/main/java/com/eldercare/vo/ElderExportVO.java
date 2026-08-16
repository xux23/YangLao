package com.eldercare.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 老人名单导出 Excel 视图对象
 */
@Data
public class ElderExportVO {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("出生日期")
    private String birthday;

    @ExcelProperty("身份证号")
    private String idCard;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("房间号")
    private String roomNo;

    @ExcelProperty("床位号")
    private String bedNo;

    @ExcelProperty("入住日期")
    private String checkinTime;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("健康概况")
    private String healthSummary;
}