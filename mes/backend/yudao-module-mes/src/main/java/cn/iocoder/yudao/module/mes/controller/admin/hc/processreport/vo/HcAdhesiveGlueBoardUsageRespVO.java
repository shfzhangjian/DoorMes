package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶胶板领用 Response VO")
@Data
public class HcAdhesiveGlueBoardUsageRespVO {

    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long glueBoardStockId;
    private String glueBoardMaterialCode;
    private String glueBoardModel;
    private String glueBoardBatchNo;
    private String stockMeasureMode;
    private BigDecimal receiveStartPosition;
    private BigDecimal receiveLength;
    private BigDecimal receiveCount;
    private BigDecimal aqcSampleLength;
    private BigDecimal consumedLength;
    private BigDecimal consumedCount;
    private BigDecimal lossLength;
    private BigDecimal lossCount;
    private BigDecimal availableStartPosition;
    private BigDecimal availableLength;
    private BigDecimal availableCount;
    private BigDecimal returnedStartPosition;
    private BigDecimal returnedLength;
    private BigDecimal returnedCount;
    private String lifetimeMode;
    private BigDecimal lifetimeLimitLength;
    private BigDecimal lifetimeLimitCount;
    private BigDecimal lifeUsedLength;
    private BigDecimal lifeUsedCount;
    private String usageStatus;
    private String qualityStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionSubmitTime;

    private Long latestInspectionId;
    private String latestInspectionNo;
    private String latestInspectionResult;

    private BigDecimal qualityLockStartPosition;
    private String qualityLockReason;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    private Long recorderId;
    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    private HcAdhesiveAqcTaskRespVO latestAqcTask;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
