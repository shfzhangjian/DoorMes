package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - SRM通用业务单据 Response VO")
@Data
public class SrmDocumentRespVO {

    private Long id;
    private String bizType;
    private String docNo;
    private String title;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String status;
    private Long applicantId;
    private String applicantName;
    private String applyDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate dueDate;

    private BigDecimal totalScore;
    private String evalGrade;
    private String bizCategory;
    private String bizLevel;
    private String periodType;
    private Integer evalYear;
    private Integer evalQuarter;
    private String payloadJson;
    private String remark;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
