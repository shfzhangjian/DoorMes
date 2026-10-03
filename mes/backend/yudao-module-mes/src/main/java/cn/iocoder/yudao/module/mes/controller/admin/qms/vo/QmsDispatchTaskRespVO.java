package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务中心 Response VO")
@Data
public class QmsDispatchTaskRespVO {

    private Long id;
    private String taskNo;
    private String taskType;
    private String sourceMode;
    private String executionMode;
    private String objectMode;
    private String sampleSelectionMode;
    private Integer requiredSampleQty;
    private String triggerSource;
    private String checkType;
    private String inspectionScene;
    private String objectType;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long executionId;
    private String executionNo;
    private String executionRoute;
    private Long sourceExecutionId;
    private String sourceExecutionNo;
    private Long rootExecutionId;
    private String rootExecutionNo;
    private Long recheckGroupId;
    private Integer currentRoundNo;
    private Integer recheckCount;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialSpec;
    private String productModel;
    private String operationCode;
    private String operationName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private String lotNo;
    private String receiptNo;
    private String supplierName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate arrivalDate;

    private BigDecimal checkQty;
    private String unit;
    private Long standardId;
    private String standardNo;
    private String standardName;
    private String standardVersion;
    private Long assigneeUserId;
    private String assigneeUserName;
    private Long assigneeDeptId;
    private String assigneeDeptName;
    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime requiredFinishTime;

    private String dispatchStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dispatchTime;

    private String taskInstruction;
    private String processInstanceId;
    private String processStatus;
    private String sourceStatus;
    private String sourceJudgment;
    private String sourceInspectorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceInspectionTime;

    private Integer itemCount;
    private Integer abnormalCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastSyncTime;

    private String cancelReason;
    private String effectiveStatus;
    private Long creatorId;
    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
