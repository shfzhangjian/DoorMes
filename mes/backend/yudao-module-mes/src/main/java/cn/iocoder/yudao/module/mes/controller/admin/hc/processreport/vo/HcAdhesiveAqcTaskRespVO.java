package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶AQC首检任务 Response VO")
@Data
public class HcAdhesiveAqcTaskRespVO {

    private Long id;
    private String taskType;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long adhesiveReportId;
    private Long glueBoardUsageId;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private BigDecimal sampleStartPosition;
    private BigDecimal sampleLength;
    private String taskStatus;
    private Long submitterId;
    private String submitterName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submitTime;

    private String feedbackResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime feedbackTime;

    private String feedbackRemark;
    private BigDecimal lockStartPosition;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;
}
