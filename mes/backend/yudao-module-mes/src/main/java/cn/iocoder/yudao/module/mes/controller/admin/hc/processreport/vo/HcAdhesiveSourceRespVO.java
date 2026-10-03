package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 粘双面胶来源二次磨皮批次 Response VO")
@Data
@Builder
public class HcAdhesiveSourceRespVO {

    private Long grindingSecondDetailId;
    private Long grindingPlanId;
    private String grindingPlanNo;
    private Long grindingPlanOperationId;
    private String rowUid;
    private String motherBatchNo;
    private String productionBatchNo;
    private String actualSizeRule;
    private String actualSizeSuffix;
    private String parentProductionBatchNo;
    private String modelCode;
    private String segmentMark;
    private BigDecimal processLength;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    private String selfCheck;
    private String defectCode;
    private String operationName;
    private String processStage;
    private String productQualityStatus;
    private String qualityLockReason;
    private String sourceMenuCode;
    private Boolean coaFlag;
    private String extraJson;
    private String confirmStatus;
    private String confirmedBatchNo;
    private Long stockId;
    private Long inventoryLockId;
    private String sourceMode;
    private String sourcePlanNo;
    private String sourceMotherBatchNo;
    private BigDecimal lockQty;
    private BigDecimal consumedQty;
    private BigDecimal remainingQty;
    private String lockStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private Long segmentTimingId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime segmentStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime segmentEndTime;

    private Long segmentStartOperatorId;
    private String segmentStartOperatorName;
    private Long segmentEndOperatorId;
    private String segmentEndOperatorName;

    private String downstreamStatus;
    private HcQtimeEvaluationRespVO qtime;
}
