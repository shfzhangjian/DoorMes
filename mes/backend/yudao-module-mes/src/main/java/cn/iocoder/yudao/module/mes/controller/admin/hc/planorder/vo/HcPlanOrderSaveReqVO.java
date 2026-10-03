package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 生产计划新增/修改 Request VO")
@Data
public class HcPlanOrderSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "计划单号；新增时后端自动生成，修改时必填")
    private String planNo;

    @Schema(description = "计划业务日期；由生产开始日期同步")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    @Schema(description = "排产模式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "排产模式不能为空")
    private String planMode;

    @Schema(description = "计划状态")
    private String planStatus;

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "销售订单ID")
    private Long salesOrderId;

    @Schema(description = "销售订单号快照")
    private String salesOrderNo;

    @Schema(description = "销售订单ERP编号快照")
    private String salesOrderErpNo;

    @Schema(description = "销售订单行号快照")
    private String salesOrderLineNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称快照")
    private String customerName;

    @Schema(description = "订单欠交量快照")
    private BigDecimal orderDueQty;

    @Schema(description = "订单欠交量单位ID")
    private Long orderDueUnitId;

    @Schema(description = "订单欠交量单位符号")
    private String orderDueUnitCode;

    @Schema(description = "订单欠交量单位名称")
    private String orderDueUnitName;

    @Schema(description = "销售订单交货日期快照")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate salesOrderDeliveryDate;

    @Schema(description = "生产开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionStartDate;

    @Schema(description = "生产结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionEndDate;

    @Schema(description = "目标成品物料ID")
    private Long materialId;

    @Schema(description = "物料编码快照")
    private String materialCode;

    @Schema(description = "物料名称快照")
    private String materialName;

    @Schema(description = "母料物料ID")
    private Long motherMaterialId;

    @Schema(description = "母料物料编码快照")
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
    private String prodType;

    @Schema(description = "生产类型描述")
    private String prodTypeName;

    @Schema(description = "产品型号ID快照")
    private Long modelId;

    @Schema(description = "型号编码快照")
    private String modelCode;

    @Schema(description = "产品型号名称快照")
    private String modelName;

    @Schema(description = "母料型号ID快照")
    private Long motherModelId;

    @Schema(description = "母料型号编码快照")
    private String motherModelCode;

    @Schema(description = "母料型号名称快照")
    private String motherModelName;

    @Schema(description = "配方ID")
    private Long recipeId;

    @Schema(description = "配方编码快照")
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
    private String routeCode;

    @Schema(description = "工艺路线名称快照")
    private String routeName;

    @Schema(description = "工艺路线版本快照")
    private String routeVersion;

    @Schema(description = "尺寸规格")
    private String sizeSpec;

    @Schema(description = "尺寸规格描述")
    private String sizeName;

    @Schema(description = "计划目标量")
    private BigDecimal targetQty;

    @Schema(description = "计划目标单位ID")
    private Long targetUnitId;

    @Schema(description = "计划目标单位符号")
    private String targetUnitCode;

    @Schema(description = "计划目标单位名称")
    private String targetUnitName;

    @Schema(description = "计划目标单位")
    private String targetUom;

    @Schema(description = "成品抵扣量")
    private BigDecimal fgDeductQty;

    @Schema(description = "净排产量")
    private BigDecimal netPlanQty;

    @Schema(description = "工序数量")
    private Integer operationCount;

    @Schema(description = "累计锁定量")
    private BigDecimal totalLockQty;

    @Schema(description = "是否包含前加工工序")
    private Boolean frontProcessFlag;

    @Schema(description = "是否包含后加工工序")
    private Boolean postProcessFlag;

    @Schema(description = "利库来源母卷批次，去重后以顿号分隔")
    private String inventorySourceBatchNos;

    @Schema(description = "批次规则ID")
    private Long batchRuleId;

    @Schema(description = "批次规则编码")
    private String batchRuleCode;

    @Schema(description = "批次规则版本")
    private Integer batchRuleVersion;

    @Schema(description = "主批次号")
    private String batchNo;

    @Schema(description = "批次状态")
    private String batchStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "工艺路线快照 JSON")
    private String routeSnapshotJson;

    @Schema(description = "销售订单挂接快照 JSON")
    private String salesOrderSnapshotJson;

    @Schema(description = "工序计划列表")
    @Valid
    private List<HcPlanOrderOperationReqVO> operations;

    @Schema(description = "库存锁定列表")
    @Valid
    private List<HcPlanOrderInventoryLockReqVO> inventoryLocks;

}
