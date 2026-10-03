package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcPlanOrderRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "计划单号")
    @ExcelProperty("计划单号")
    private String planNo;

    @Schema(description = "计划业务日期")
    @ExcelProperty("计划业务日期")
    private LocalDate planDate;

    @Schema(description = "排产模式")
    @ExcelProperty("排产模式")
    private String planMode;

    @Schema(description = "计划状态")
    @ExcelProperty("计划状态")
    private String planStatus;

    @Schema(description = "来源类型")
    @ExcelProperty("来源类型")
    private String sourceType;

    @Schema(description = "销售订单ID")
    private Long salesOrderId;

    @Schema(description = "销售订单号快照")
    @ExcelProperty("销售订单号")
    private String salesOrderNo;

    @Schema(description = "销售订单ERP编号快照")
    @ExcelProperty("ERP编号")
    private String salesOrderErpNo;

    @Schema(description = "销售订单行号快照")
    private String salesOrderLineNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称快照")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "订单欠交量快照")
    @ExcelProperty("订单欠交量")
    private BigDecimal orderDueQty;

    @Schema(description = "订单欠交量单位ID")
    private Long orderDueUnitId;

    @Schema(description = "订单欠交量单位符号")
    @ExcelProperty("订单欠交量单位")
    private String orderDueUnitCode;

    @Schema(description = "订单欠交量单位名称")
    private String orderDueUnitName;

    @Schema(description = "销售订单交货日期快照")
    @ExcelProperty("交货日期")
    private LocalDate salesOrderDeliveryDate;

    @Schema(description = "生产开始日期")
    @ExcelProperty("生产开始日期")
    private LocalDate productionStartDate;

    @Schema(description = "生产结束日期")
    @ExcelProperty("生产结束日期")
    private LocalDate productionEndDate;

    @Schema(description = "目标成品物料ID")
    private Long materialId;

    @Schema(description = "物料编码快照")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "物料名称快照")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "母料物料ID")
    private Long motherMaterialId;

    @Schema(description = "母料物料编码快照")
    @ExcelProperty("母料料号")
    private String motherMaterialCode;

    @Schema(description = "母料物料名称快照")
    private String motherMaterialName;

    @Schema(description = "物料分类ID")
    private Long categoryId;

    @Schema(description = "物料分类编码快照")
    private String categoryCode;

    @Schema(description = "物料分类名称快照")
    private String categoryName;

    @Schema(description = "生产类型")
    @ExcelProperty("生产类型")
    private String prodType;

    @Schema(description = "生产类型描述")
    private String prodTypeName;

    @Schema(description = "产品型号ID快照")
    private Long modelId;

    @Schema(description = "型号编码快照")
    @ExcelProperty("型号编码")
    private String modelCode;

    @Schema(description = "产品型号名称快照")
    @ExcelProperty("产品型号名称")
    private String modelName;

    @Schema(description = "母料型号ID快照")
    private Long motherModelId;

    @Schema(description = "母料型号编码快照")
    @ExcelProperty("母料型号")
    private String motherModelCode;

    @Schema(description = "母料型号名称快照")
    private String motherModelName;

    @Schema(description = "配方ID")
    private Long recipeId;

    @Schema(description = "配方编码快照")
    @ExcelProperty("配方编码")
    private String recipeCode;

    @Schema(description = "配方名称快照")
    private String recipeName;

    @Schema(description = "BOM ID")
    private Long bomId;

    @Schema(description = "BOM 版本快照")
    private String bomVersion;

    @Schema(description = "工艺路线ID")
    private Long routeId;

    @Schema(description = "工艺路线编码快照")
    @ExcelProperty("工艺路线编码")
    private String routeCode;

    @Schema(description = "工艺路线名称快照")
    @ExcelProperty("工艺路线名称")
    private String routeName;

    @Schema(description = "工艺路线版本快照")
    private String routeVersion;

    @Schema(description = "尺寸规格")
    @ExcelProperty("尺寸规格")
    private String sizeSpec;

    @Schema(description = "尺寸规格描述")
    private String sizeName;

    @Schema(description = "计划目标量")
    @ExcelProperty("计划目标量")
    private BigDecimal targetQty;

    @Schema(description = "计划目标单位ID")
    private Long targetUnitId;

    @Schema(description = "计划目标单位符号")
    private String targetUnitCode;

    @Schema(description = "计划目标单位名称")
    private String targetUnitName;

    @Schema(description = "计划目标单位")
    @ExcelProperty("计划目标单位")
    private String targetUom;

    @Schema(description = "成品抵扣量")
    private BigDecimal fgDeductQty;

    @Schema(description = "净排产量")
    @ExcelProperty("净排产量")
    private BigDecimal netPlanQty;

    @Schema(description = "工序数量")
    private Integer operationCount;

    @Schema(description = "累计锁定量")
    @ExcelProperty("累计锁定量")
    private BigDecimal totalLockQty;

    @Schema(description = "是否包含前加工工序")
    private Boolean frontProcessFlag;

    @Schema(description = "是否包含后加工工序")
    private Boolean postProcessFlag;

    @Schema(description = "利库来源母卷批次，去重后以顿号分隔")
    private String inventorySourceBatchNos;

    @Schema(description = "完成排产时间")
    private LocalDateTime plannedAt;

    @Schema(description = "下发时间")
    @ExcelProperty("下发时间")
    private LocalDateTime releasedAt;

    @Schema(description = "最近一次状态操作人ID")
    private Long statusOperatorId;

    @Schema(description = "最近一次状态操作人名称")
    private String statusOperatorName;

    @Schema(description = "最近一次状态操作时间")
    private LocalDateTime statusOperateTime;

    @Schema(description = "最近一次状态操作备注")
    private String statusRemark;

    @Schema(description = "批次规则ID")
    private Long batchRuleId;

    @Schema(description = "批次规则编码")
    private String batchRuleCode;

    @Schema(description = "批次规则版本")
    private Integer batchRuleVersion;

    @Schema(description = "主批次号")
    @ExcelProperty("主批次号")
    private String batchNo;

    @Schema(description = "生产批号")
    private String productionBatchNo;

    @Schema(description = "父生产批号")
    private String parentProductionBatchNo;

    @Schema(description = "生产批号规则ID")
    private Long productionBatchRuleId;

    @Schema(description = "生产批号规则编码")
    private String productionBatchRuleCode;

    @Schema(description = "生产批号规则版本")
    private Integer productionBatchRuleVersion;

    @Schema(description = "生产批号上下文 JSON")
    private String productionBatchContextJson;

    @Schema(description = "批次状态")
    private String batchStatus;

    @Schema(description = "批次生成时间")
    private LocalDateTime batchGeneratedTime;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "工艺路线快照 JSON")
    private String routeSnapshotJson;

    @Schema(description = "销售订单挂接快照 JSON")
    private String salesOrderSnapshotJson;

    @Schema(description = "创建人")
    @ExcelProperty("创建人")
    private String creator;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
