package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料主数据分页 Request VO")
@Data
public class HcMaterialPageReqVO extends PageParam {

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "物料简称")
    private String materialShortName;

    @Schema(description = "物料类型")
    private Integer materialType;

    @Schema(description = "物料分类ID")
    private Long materialCategoryId;

    @Schema(description = "物料分类名称")
    private String materialCategoryName;

    @Schema(description = "产品层级")
    private String productLevel;

    @Schema(description = "规格型号描述")
    private String specModel;

    @Schema(description = "物料编号规则ID")
    private Long materialCodeRuleId;

    @Schema(description = "型号编码规则ID")
    private Long modelCodeRuleId;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "基础计量单位")
    private String baseUom;

    @Schema(description = "库存单位")
    private String stockUom;

    @Schema(description = "生产单位")
    private String produceUom;

    @Schema(description = "是否批次管理")
    private Boolean batchManaged;

    @Schema(description = "是否条码管理")
    private Boolean barcodeManaged;

    @Schema(description = "默认工艺路线ID")
    private Long defaultRouteId;

    @Schema(description = "默认工艺路线编码")
    private String defaultRouteCode;

    @Schema(description = "默认配方编码")
    private String defaultRecipeCode;

    @Schema(description = "默认配方名称")
    private String defaultRecipeName;

    @Schema(description = "默认BOM/配方ID")
    private Long defaultBomId;

    @Schema(description = "质检模式")
    private String qualityControlMode;

    @Schema(description = "物料状态")
    private Integer materialStatus;

    @Schema(description = "MES产品")
    private Boolean mesSelectVisible;

    @Schema(description = "备注")
    private String remark;

}
