package cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HcBatchTraceFactDTO {

    private Long id;
    private String sourceType;
    private String sourceTypeName;
    private String bizNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String workCenterName;
    private String equipmentCode;
    private String equipmentName;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String sourceBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private LocalDate reportDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal reportQty;
    private String reportUom;
    private String reportType;
    private BigDecimal inputLength;
    private BigDecimal outputLength;
    private BigDecimal lossLength;
    private BigDecimal napSampleLength;
    private BigDecimal startPosition;
    private BigDecimal endPosition;
    private BigDecimal processLength;
    private BigDecimal slittingRemainingLength;
    private String glueBoardModel;
    private String glueBoardBatchNo;
    private String productQualityStatus;
    private String qualityLockReason;
    private String sizeCode;
    private String sizeName;
    private String coaFlag;
    private String reportStatus;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String confirmerName;
    private LocalDateTime confirmerTime;
    private String inspectionNo;
    private String inspectionStatus;
    private String inspectionResult;
    private LocalDateTime inspectionApplyTime;
    private LocalDateTime inspectionReturnTime;
    private String inspectionRemark;
    private String selfCheck;
    private String defectCode;
    private String extraJson;
    private String remark;
    private LocalDateTime createTime;

}
