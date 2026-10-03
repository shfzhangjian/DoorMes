package cn.iocoder.yudao.module.mes.dal.dataobject.hc.intermediatestockledger;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序中间品库台账 DO，对应 mes_inv_stock 主账。
 */
@TableName("mes_inv_stock")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcIntermediateStockLedgerDO extends BaseDO {

    @TableId
    private Long id;
    private String stockType;
    private String sourceType;
    private String sourceTable;
    private Long sourceId;
    private Long sourceReportId;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourcePlanOperationId;
    private String sourceBatchNo;
    private String sourceParentBatchNo;
    private Long tenantId;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private String locationName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String recipeCode;
    private String recipeName;
    private String modelNo;
    private String specSize;
    private Integer opSeq;
    private String opCode;
    private String opName;
    private String segmentCode;
    private String segmentName;
    private BigDecimal thickness;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal onHandQty;
    private BigDecimal availableQty;
    private BigDecimal shareableQty;
    private BigDecimal frozenQty;
    private BigDecimal planLockedQty;
    private String qualityStatus;
    private String bizStatus;
    private String businessRemark;
    private String uom;
    private Long ownerId;
    private String ownerCode;
    private String ownerName;
    private String lastTxnNo;
    private LocalDateTime lastTxnTime;

    @TableField(exist = false)
    private String rowType;
    @TableField(exist = false)
    private String motherBatchNo;
    @TableField(exist = false)
    private Integer stockCount;
    @TableField(exist = false)
    private BigDecimal lockedQty;
    @TableField(exist = false)
    private BigDecimal consumedQty;
    @TableField(exist = false)
    private BigDecimal releasedQty;
    @TableField(exist = false)
    private BigDecimal lockRemainingQty;
    @TableField(exist = false)
    private String stockStatus;
    @TableField(exist = false)
    private Long activeLockId;
    @TableField(exist = false)
    private String activeLockStatus;
    @TableField(exist = false)
    private String activeLockTargetPlanNo;
    @TableField(exist = false)
    private String activeLockTargetOpName;
    @TableField(exist = false)
    private BigDecimal activeLockRemainingQty;
    @TableField(exist = false)
    private String txnSummary;
    @TableField(exist = false)
    private List<TxnDetail> txnDetails;

    @Data
    public static class TxnDetail {

        private String eventType;
        private String eventTypeName;
        private Long txnId;
        private String txnNo;
        private String txnType;
        private LocalDateTime txnTime;
        private Long stockId;
        private Long lockId;
        private String targetPlanNo;
        private Long targetPlanOperationId;
        private String targetOpName;
        private BigDecimal qty;
        private String uom;
        private String refDocType;
        private Long refDocId;
        private String refDocNo;
        private String displayBatchNo;
        private String summary;
        private String remark;

    }

}
