package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 粘双面胶报工记录 Response VO")
@Data
public class HcAdhesiveReportRespVO {

    @Schema(description = "上游湿法/二磨留样异常原因；允许加工，裁切报检及完工受限")
    private String upstreamSampleLockReason;

    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long sourceGrindingSecondDetailId;
    private String sourceType;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String productionBatchNo;
    private String actualSizeRule;
    private String actualSizeSuffix;

    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private BigDecimal inputLength;
    private BigDecimal startPosition;
    private BigDecimal endPosition;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private Long glueBoardUsageId;
    private BigDecimal glueBoardStartPosition;
    private BigDecimal glueBoardUseLength;
    private Long aqcTaskId;
    private String aqcStatus;
    private Long faiId;
    private String faiNo;
    private String faiStatus;
    private String faiJudgment;
    private Long faiStandardId;
    private String faiStandardNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiApplyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiReturnTime;

    private String faiRejectReason;
    private String productQualityStatus;
    private String qualityLockReason;
    private String selfCheck;
    private String defectCode;
    @Schema(description = "过程风险标记：NONE/ADHESIVE2_NG/CUT_ROUND_NG/BOTH_NG，仅用于追溯")
    private String qualityRiskFlag;
    @Schema(description = "过程风险快照 JSON")
    private String qualityRiskSnapshotJson;
    private String reportStatus;
    private Long fqcOrderId;
    private String fqcNo;
    private String fqcStatus;
    private String fqcJudgment;
    private Long inspectionTaskId;
    private String inspectionTaskNo;
    private String inspectionStatus;
    private String inspectionResult;
    private String inspectorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    private String inspectionRemark;
    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;

    private String remark;
    private String extraJson;
    private Boolean downstreamFeedbackAbnormal;
    private Long downstreamFeedbackReportId;
    private String downstreamFeedbackProcessCode;
    private String downstreamFeedbackProcessName;
    private String downstreamFeedbackReason;
    private String editBlockedReason;
    private List<HcAdhesiveCheckItemRespVO> checkItems;
    private List<HcWetReportAbnormalPositionRespVO> abnormalPositions;
}
