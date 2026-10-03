package cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水 DO
 * 对应表: mes_inv_txn_log
 */
@TableName("mes_inv_txn_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcInvTxnLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    /** 库存余额ID */
    private Long stockId;

    /** 库存类型：WIP/FG */
    private String stockType;

    /** 交易单号 */
    private String txnNo;

    /** 交易类型 */
    private String txnType;

    /** 交易时间 */
    private LocalDateTime txnTime;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 库位编码 */
    private String locationCode;

    /** 物料ID */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 型号 */
    private String modelNo;

    /** 批次号 */
    private String batchNo;

    /** 交易数量（正=入库，负=出库） */
    private BigDecimal txnQty;

    /** 交易前在库数量 */
    private BigDecimal beforeQty;

    /** 交易后在库数量 */
    private BigDecimal afterQty;

    /** 交易前可用数量 */
    private BigDecimal beforeAvailableQty;

    /** 交易后可用数量 */
    private BigDecimal afterAvailableQty;

    /** 交易前冻结数量 */
    private BigDecimal beforeFrozenQty;

    /** 交易后冻结数量 */
    private BigDecimal afterFrozenQty;

    /** 交易前计划锁定量 */
    private BigDecimal beforePlanLockedQty;

    /** 交易后计划锁定量 */
    private BigDecimal afterPlanLockedQty;

    /** 单位 */
    private String uom;

    /** 来源单据类型 */
    private String refDocType;

    /** 来源单据ID */
    private Long refDocId;

    /** 来源单据号 */
    private String refDocNo;

    /** 来源类型 */
    private String sourceType;

    /** 来源事实表名 */
    private String sourceTable;

    /** 来源事实表主键ID */
    private Long sourceId;

    /** 来源批次号 */
    private String sourceBatchNo;

    /** 来源计划ID */
    private Long sourcePlanId;

    /** 来源计划号 */
    private String sourcePlanNo;

    /** 来源计划工序ID */
    private Long sourcePlanOperationId;

    /** 目标计划ID */
    private Long targetPlanId;

    /** 目标计划号 */
    private String targetPlanNo;

    /** 目标计划工序ID */
    private Long targetPlanOperationId;

    /** 目标工序编码 */
    private String targetOpCode;

    /** 目标工序名称 */
    private String targetOpName;

    /** 操作人姓名 */
    private String creatorName;

    /** 备注 */
    private String remark;

}
