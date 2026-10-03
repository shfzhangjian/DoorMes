package cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实时库存余额 DO
 * 对应表: mes_inv_stock
 */
@TableName("mes_inv_stock")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcInvStockDO extends BaseDO {

    /** 主键ID */
    @TableId
    private Long id;

    /** 库存类型：WIP/FG */
    private String stockType;

    /** WIP 来源类型 */
    private String sourceType;

    /** 来源事实表名 */
    private String sourceTable;

    /** 来源事实表主键ID */
    private Long sourceId;

    /** 来源报工主记录ID */
    private Long sourceReportId;

    /** 来源计划ID */
    private Long sourcePlanId;

    /** 来源计划号 */
    private String sourcePlanNo;

    /** 来源计划工序ID */
    private Long sourcePlanOperationId;

    /** 来源生产批号/半成品批号 */
    private String sourceBatchNo;

    /** 来源上游批号/母批号 */
    private String sourceParentBatchNo;

    /** 租户ID */
    private Long tenantId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称（冗余） */
    private String warehouseName;

    /** 库位编码（为空表示仓库级管理） */
    private String locationCode;

    /** 物料ID */
    private Long materialId;

    /** 物料编码（冗余） */
    private String materialCode;

    /** 物料名称（冗余） */
    private String materialName;

    /** 配方编码（冗余） */
    private String recipeCode;

    /** 配方名称（冗余） */
    private String recipeName;

    /** 型号（冗余） */
    private String modelNo;

    /** 尺寸规格（冗余） */
    private String specSize;

    /** 所在工序顺序 */
    private Integer opSeq;

    /** 所在工序编码 */
    private String opCode;

    /** 所在工序名称 */
    private String opName;

    /** 段位编码 */
    private String segmentCode;

    /** 段位名称 */
    private String segmentName;

    /** 厚度 */
    private BigDecimal thickness;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 在库数量 = available_qty + frozen_qty */
    private BigDecimal onHandQty;

    /** 可用数量 */
    private BigDecimal availableQty;

    /** 可利库量：允许跨计划挂接使用的可用数量 */
    private BigDecimal shareableQty;

    /** 冻结数量 */
    private BigDecimal frozenQty;

    /** 计划锁定量（frozen_qty 的子集） */
    private BigDecimal planLockedQty;

    /** 质量状态：合格/待检/冻结 */
    private String qualityStatus;

    /** 业务状态：量产/研发/客诉冻结 */
    private String bizStatus;

    /** 库存业务备注 */
    private String businessRemark;

    /** 单位 */
    private String uom;

    /** 库位名称（冗余） */
    private String locationName;

    /** 货主ID */
    private Long ownerId;

    /** 货主编码 */
    private String ownerCode;

    /** 货主名称 */
    private String ownerName;

    /** 最近流水号（冗余） */
    private String lastTxnNo;

    /** 最近过账时间（冗余） */
    private LocalDateTime lastTxnTime;

}
