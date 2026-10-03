package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QmsAbnormalLockImpactDTO {

    private Long sourceId;
    private String impactSourceType;
    private String sourceTable;
    private String sourceKey;
    private String affectedBatchNo;
    private String sourceBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Integer processOrder;
    private Long glueBoardUsageId;
    private String glueBoardBatchNo;
    private BigDecimal glueBoardStartPosition;
    private BigDecimal glueBoardUseLength;
    private LocalDateTime eventTime;
    private String snapshotJson;
}
