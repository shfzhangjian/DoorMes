package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
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

@TableName("mes_qms_fqc_order")
@KeySequence("mes_qms_fqc_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String fqcNo;
    private String reportNo;
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
    private String productModel;
    private String productBatchNo;
    private String batchNo;
    private BigDecimal produceQty;
    private String unitCode;
    private String unitName;
    private String aqlStandard;
    private Integer sampleQty;
    private Integer submissionDetailCount;
    private Integer okQty;
    private Integer ngQty;
    private String inspectionCategory;
    private String submissionType;
    private LocalDateTime submissionTime;
    private String submitterName;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    private String status;
    private String judgment;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectionTime;
    private Long qaInspectorId;
    private String qaInspectorName;
    private LocalDateTime qaTime;
    private String releaseResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;
    private String relatedNcrNo;
    private String ncrStatus;
    private String remark;
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
    private Long tenantId;
}
