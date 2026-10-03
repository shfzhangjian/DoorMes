package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板第二次磨皮记录 Response VO")
@Data
public class HcRoughConsoleSecondReportRespVO {
    private HcGrindingConsumptionVO consumption;


    private Long id;
    private Long grindingReportId;
    private Long firstDetailId;
    private Long firstAllocationId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String rowUid;
    private String sourceRowUid;
    private String motherBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String sourceProductionBatchNo;
    private String segmentMark;
    private BigDecimal processLength;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    /** 研发消耗米数，独立于固定损耗、NAP留样和异常米数。 */
    private BigDecimal researchConsumptionLength;
    private BigDecimal startPosition;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private String sandpaperBatchNo;
    private String pressure;
    private String lineSpeed;
    private String rotationSpeed;
    private String meterCounter;
    private String grindingThickness;
    private String afterGrindingThickness;
    private String qualityThickness;
    private String qualityWidth;
    private BigDecimal grindingMeters;
    private String selfCheck;
    private String defectCode;
    private String printStatus;
    private Integer printCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;

    private Long inspectionId;
    private String inspectionNo;
    private String inspectionStatus;
    private String inspectionResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionApplyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionReturnTime;

    private String inspectionRejectReason;

    private String confirmStatus;
    private String downstreamStatus;
    private String detailStatus;
    private String remark;
    private Long middleProductRecordId;
    private String middleProductStatus;
    private String middleProductRecorder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime middleProductRecordTime;

    private Integer middleProductGeneratedLength;
}
