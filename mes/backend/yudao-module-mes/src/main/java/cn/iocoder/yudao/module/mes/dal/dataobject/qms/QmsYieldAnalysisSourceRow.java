package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QmsYieldAnalysisSourceRow {

    private String processCode;
    private String processName;
    private String sourceTable;
    private Long sourceId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String motherRollNo;
    private String segmentNo;
    private String pieceNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private BigDecimal inputQty;
    private BigDecimal outputGoodQty;
    private BigDecimal outputNgQty;
    private String selfCheck;
    private String defectCode;
    private String visualResultJson;
    private String extraJson;
    private String submissionResult;
    private String reportStatus;
    private LocalDateTime confirmTime;
    private Long inspectionId;
    private String inspectionNo;
    private String inspectionSourceType;
}
