package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护理记录新增/修改请求参数
 */
@Data
public class CareRecordDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    @NotNull(message = "护理项目不能为空")
    private String planName;

    /** 频次说明 */
    private String planFrequency;

    /** 护理内容 */
    private String careContent;

    /** 执行时间（不传默认当前时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime careTime;

    /** 交接班备注 */
    private String remark;
}