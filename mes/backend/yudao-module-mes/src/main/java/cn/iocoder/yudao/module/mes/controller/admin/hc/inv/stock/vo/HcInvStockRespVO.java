package cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 实时库存余额 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcInvStockRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "库存类型：WIP/FG")
    private String stockType;

    @Schema(description = "WIP 来源类型")
    private String sourceType;

    @Schema(description = "来源事实表名")
    private String sourceTable;

    @Schema(description = "来源事实表主键ID")
    private Long sourceId;

    @Schema(description = "来源报工主记录ID")
    private Long sourceReportId;

    @Schema(description = "来源计划ID")
    private Long sourcePlanId;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "来源计划工序ID")
    private Long sourcePlanOperationId;

    @Schema(description = "来源生产批号/半成品批号")
    private String sourceBatchNo;

    @Schema(description = "来源上游批号/母批号")
    private String sourceParentBatchNo;

    @Schema(description = "仓库编码")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "库位编码")
    @ExcelProperty("库位编码")
    private String locationCode;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "配方名称")
    private String recipeName;

    @Schema(description = "型号")
    @ExcelProperty("型号")
    private String modelNo;

    @Schema(description = "尺寸规格")
    @ExcelProperty("尺寸规格")
    private String specSize;

    @Schema(description = "所在工序顺序")
    private Integer opSeq;

    @Schema(description = "所在工序编码")
    private String opCode;

    @Schema(description = "所在工序名称")
    private String opName;

    @Schema(description = "段位编码")
    private String segmentCode;

    @Schema(description = "段位名称")
    private String segmentName;

    @Schema(description = "厚度")
    private BigDecimal thickness;

    @Schema(description = "批次号")
    @ExcelProperty("批次号")
    private String batchNo;

    @Schema(description = "生产日期")
    @ExcelProperty("生产日期")
    private LocalDate productionDate;

    @Schema(description = "失效日期")
    @ExcelProperty("失效日期")
    private LocalDate expiryDate;

    @Schema(description = "在库数量")
    @ExcelProperty("在库数量")
    private BigDecimal onHandQty;

    @Schema(description = "可用数量")
    @ExcelProperty("可用数量")
    private BigDecimal availableQty;

    @Schema(description = "可利库量")
    @ExcelProperty("可利库量")
    private BigDecimal shareableQty;

    @Schema(description = "冻结数量")
    @ExcelProperty("冻结数量")
    private BigDecimal frozenQty;

    @Schema(description = "计划锁定量")
    @ExcelProperty("计划锁定量")
    private BigDecimal planLockedQty;

    @Schema(description = "质量状态")
    @ExcelProperty("质量状态")
    private String qualityStatus;

    @Schema(description = "业务状态")
    @ExcelProperty("业务状态")
    private String bizStatus;

    @Schema(description = "库存业务备注")
    private String businessRemark;

    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String uom;

    @Schema(description = "库位名称")
    private String locationName;

    @Schema(description = "货主ID")
    private Long ownerId;

    @Schema(description = "货主编码")
    private String ownerCode;

    @Schema(description = "货主名称")
    private String ownerName;

    @Schema(description = "最近流水号")
    @ExcelProperty("最近流水号")
    private String lastTxnNo;

    @Schema(description = "最近过账时间")
    @ExcelProperty("最近过账时间")
    private LocalDateTime lastTxnTime;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
