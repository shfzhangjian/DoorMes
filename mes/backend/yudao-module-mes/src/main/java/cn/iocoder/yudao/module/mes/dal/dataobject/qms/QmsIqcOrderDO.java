package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "mes_qms_iqc_order", autoResultMap = true)
@KeySequence("mes_qms_iqc_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIqcOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String iqcNo;

    private Long receiptId;

    private String receiptNo;

    private Long supplierId;

    private String supplierCode;

    private String supplierName;

    private String receiverName;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private String specification;

    private String modelNo;

    private Long productModelId;

    private String productModelCode;

    private String productModelName;

    private String batchNo;

    private BigDecimal receiveQty;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate arrivalDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionApplyTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> inspectionApplyAttachmentUrls;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate expiryDate;

    private String unit;

    private Long standardId;

    private String standardNo;

    private String standardName;

    private String standardVersion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime standardSnapshotTime;

    private String standardMatchMode;

    private Boolean standardSnapshotLocked;

    private String standardSnapshotHash;

    private String aqlStandard;

    private String status;

    private String judgment;

    private String retentionStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime retentionConfirmTime;

    private Long retentionConfirmUserId;

    private String retentionConfirmUserName;

    private Long retentionMaterialCategoryId;

    private String retentionMaterialCategoryName;

    private BigDecimal retentionQty;

    private String retentionUnit;

    private String retentionRuleDesc;

    private Integer retentionPeriodValue;

    private String retentionPeriodUnit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime retentionExpireTime;

    private String retentionDestroyStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime retentionDestroyTime;

    private Long retentionDestroyUserId;

    private String retentionDestroyUserName;

    private String retentionDestroyRemark;

    private Long inspectorId;

    private String inspectorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    private Long qaInspectorId;

    private String qaInspectorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime qaTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;

    private String disposalType;

    private String purchaseContractNo;

    private Integer returnCount;

    private String lastReturnReason;

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

    private String remark;

    private Long tenantId;
}
