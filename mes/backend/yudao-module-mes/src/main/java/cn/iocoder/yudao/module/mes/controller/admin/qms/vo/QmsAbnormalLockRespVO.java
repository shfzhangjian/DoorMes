package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常锁定 Response VO")
@Data
public class QmsAbnormalLockRespVO {

    private Long id;
    private String lockNo;
    private String lockSourceType;
    private String inspectionOrderType;
    private Long inspectionOrderId;
    private String inspectionOrderNo;
    private String inspectionStatus;
    private String inspectionResult;
    private String sourceModule;
    private Long sourceReportId;
    private String sourceReportNo;
    private String sourceOperationCode;
    private String sourceOperationName;
    private String lockScope;
    private String affectedObjectType;
    private String affectedSourceTable;
    private Long affectedSourceId;
    private String affectedSourceKey;
    private String rootBatchNo;
    private String sourceBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String affectedBatchNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Integer processOrder;
    private Long glueBoardStockId;
    private Long glueBoardUsageId;
    private String glueBoardBatchNo;
    private BigDecimal glueBoardStartPosition;
    private BigDecimal glueBoardUseLength;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionSubmitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime ngConfirmTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockTime;

    private String lockStatus;
    private String lockReason;
    private Long releaseUserId;
    private String releaseUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;

    private String releaseReason;
    private String traceSnapshotJson;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
