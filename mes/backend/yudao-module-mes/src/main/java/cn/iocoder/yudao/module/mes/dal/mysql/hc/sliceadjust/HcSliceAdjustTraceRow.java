package cn.iocoder.yudao.module.mes.dal.mysql.hc.sliceadjust;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HcSliceAdjustTraceRow {

    private String sliceNo;
    private String segmentBatchNo;
    private String processCode;
    private String processName;
    private Integer stageSort;
    private String sourceTable;
    private Long sourceId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String statusText;
    private String resultText;
    private LocalDateTime reportTime;
}
