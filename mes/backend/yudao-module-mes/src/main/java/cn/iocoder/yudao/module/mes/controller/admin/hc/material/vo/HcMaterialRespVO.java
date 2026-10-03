package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 物料主数据 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcMaterialRespVO {

    @Schema(description = "物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "物料简称")
    @ExcelProperty("物料简称")
    private String materialShortName;

    @Schema(description = "物料类型")
    @ExcelProperty("物料类型")
    private Integer materialType;

    @Schema(description = "物料分类ID")
    @ExcelProperty("物料分类ID")
    private Long materialCategoryId;

    @Schema(description = "物料分类名称")
    @ExcelProperty("物料分类名称")
    private String materialCategoryName;

    @Schema(description = "产品层级")
    @ExcelProperty("产品层级")
    private String productLevel;

    @Schema(description = "规格型号描述")
    @ExcelProperty("规格型号描述")
    private String specModel;

    @Schema(description = "物料编号规则ID")
    @ExcelProperty("物料编号规则ID")
    private Long materialCodeRuleId;

    @Schema(description = "型号编码规则ID")
    @ExcelProperty("型号编码规则ID")
    private Long modelCodeRuleId;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "型号编码")
    @ExcelProperty("型号编码")
    private String modelCode;

    @Schema(description = "产品型号名称")
    @ExcelProperty("产品型号名称")
    private String productModelName;

    @Schema(description = "型号段值快照 JSON")
    private String modelSegmentsJson;

    @Schema(description = "基础单位ID")
    private Long baseUnitId;

    @Schema(description = "基础单位符号")
    private String baseUnitCode;

    @Schema(description = "基础单位名称")
    private String baseUnitName;

    @Schema(description = "基础计量单位")
    @ExcelProperty("基础计量单位")
    private String baseUom;

    @Schema(description = "库存单位ID")
    private Long stockUnitId;

    @Schema(description = "库存单位符号")
    private String stockUnitCode;

    @Schema(description = "库存单位名称")
    private String stockUnitName;

    @Schema(description = "库存单位")
    @ExcelProperty("库存单位")
    private String stockUom;

    @Schema(description = "生产单位ID")
    private Long produceUnitId;

    @Schema(description = "生产单位符号")
    private String produceUnitCode;

    @Schema(description = "生产单位名称")
    private String produceUnitName;

    @Schema(description = "生产单位")
    @ExcelProperty("生产单位")
    private String produceUom;

    @Schema(description = "是否批次管理")
    @ExcelProperty("是否批次管理")
    private Boolean batchManaged;

    @Schema(description = "是否条码管理")
    @ExcelProperty("是否条码管理")
    private Boolean barcodeManaged;

    @Schema(description = "默认工艺路线ID")
    @ExcelProperty("默认工艺路线ID")
    private Long defaultRouteId;

    @Schema(description = "默认工艺路线编码")
    @ExcelProperty("默认工艺路线编码")
    private String defaultRouteCode;

    private String defaultRouteName;

    @Schema(description = "默认配方ID")
    @ExcelProperty("默认配方ID")
    private Long defaultRecipeId;

    private String defaultRecipeCode;

    private String defaultRecipeName;

    private Long defaultBomId;

    private String defaultBomCode;

    private String defaultBomName;

    @Schema(description = "质检模式")
    @ExcelProperty("质检模式")
    private String qualityControlMode;

    @Schema(description = "物料状态")
    @ExcelProperty("物料状态")
    private Integer materialStatus;

    @Schema(description = "MES产品")
    @ExcelProperty("MES产品")
    private Boolean mesSelectVisible;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
