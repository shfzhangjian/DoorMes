package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@TableName("mes_qms_fai_order")
@KeySequence("mes_qms_fai_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String faiNo;

    private String workOrderNo;

    private Long sourceReportId;

    private String sourceReportNo;

    private String sourceModule;

    private String sourceOperationCode;

    private String sourceOperationName;

    private Long planOrderId;

    private String operationCode;

    private String operationName;

    private Long machineId;

    private String machineCode;

    private String machineName;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private String specification;

    private Long productModelId;

    private String productModel;

    private String productBatchNo;

    private Long glueBoardStockId;

    private String glueBoardModel;

    private String glueBoardMaterialCode;

    private String gluePlateBatchNo;

    private BigDecimal sampleLength;

    private BigDecimal inspectionQty;

    private String processCategory;

    private String submissionType;

    private String wetSampleType;

    private String triggerReason;

    private String standardMatchMode;

    private String standardMatchType;

    private Long matchedModelId;

    private String matchedModelCode;

    private String standardMatchReason;

    private Long standardId;

    private String standardNo;

    private String standardVersion;

    private String standardSnapshotHash;

    private String status;

    private String judgment;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operatorTime;

    private Long qaInspectorId;

    private String qaInspectorName;

    private LocalDateTime qaTime;

    private LocalDateTime submissionTime;

    private Long submitterId;

    private String submitterName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inspectionTime;

    private String releaseResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;

    private String retentionStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime retentionConfirmTime;

    private Long retentionConfirmUserId;

    private String retentionConfirmUserName;

    private Integer retentionPeriodValue;

    private String retentionPeriodUnit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime retentionExpireTime;

    private String retentionDestroyStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime retentionDestroyTime;

    private Long retentionDestroyUserId;

    private String retentionDestroyUserName;

    private String retentionDestroyRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;

    private String remark;

    private String auditRemark;

    private Long summaryInspectionId;

    private Long sheetTemplateId;

    private String sheetTemplateCode;

    private String sheetTemplateName;

    private String sheetTemplateVersion;

    private String entryMode;

    private String entryLayout;

    private String currentStepCode;

    private Integer entryProgress;

    private Integer requiredItemCount;

    private Integer completedItemCount;

    private Integer abnormalItemCount;

    private LocalDateTime lastSaveTime;

    private LocalDateTime lastCalculateTime;

    private String lastImportBatchNo;

    private Boolean historicalBackfill;

    private Boolean sheetLocked;

    private Integer returnCount;

    private String lastReturnReason;

    private String lastScanCode;

    private String lastScanTargetType;

    private String lastScanScene;

    private LocalDateTime lastScanTime;

    private Long lastScanUserId;

    private String lastScanUserName;

    private Boolean rejectFlag;

    private Boolean recheckFlag;

    private Long recheckGroupId;

    private Integer recheckRoundNo;

    private Long rejectPrevInspectionId;

    private String rejectPrevInspectionNo;

    private Long rejectNextInspectionId;

    private String rejectNextInspectionNo;

    private Long rejectRootInspectionId;

    private String rejectRootInspectionNo;

    private String rejectRecheckResult;

    private LocalDateTime rejectRecheckTime;

    private String rejectReason;

    private LocalDateTime rejectTime;

    private Long rejectUserId;

    private String rejectUserName;

    @TableField(exist = false)
    private Long latestRecheckApplyId;

    @TableField(exist = false)
    private String latestRecheckApplyNo;

    @TableField(exist = false)
    private String latestRecheckApplyStatus;

    @TableField(exist = false)
    private String latestRecheckApplyReason;

    @TableField(exist = false)
    private String latestRecheckAuditOpinion;

    @TableField(exist = false)
    private String latestRecheckGeneratedFaiNo;

    private Long tenantId;
}
