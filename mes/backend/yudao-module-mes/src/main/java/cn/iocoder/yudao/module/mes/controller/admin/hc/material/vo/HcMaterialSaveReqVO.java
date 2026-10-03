package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialExtAttrDO;

@Schema(description = "管理后台 - 物料主数据新增/修改 Request VO")
@Data
public class HcMaterialSaveReqVO {

    @Schema(description = "物料编码，留空时可按物料编号规则自动生成")
    private String materialCode;

    @Schema(description = "物料名称")
    @NotBlank(message = "物料名称不能为空")
    private String materialName;

    @Schema(description = "物料简称")
    private String materialShortName;

    @Schema(description = "物料类型")
    @NotNull(message = "物料类型不能为空")
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

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "型号段值快照 JSON，后端自动生成")
    private String modelSegmentsJson;

    @Schema(description = "基础单位ID")
    @NotNull(message = "基础单位不能为空")
    private Long baseUnitId;

    @Schema(description = "基础单位符号")
    private String baseUnitCode;

    @Schema(description = "基础单位名称")
    private String baseUnitName;

    @Schema(description = "基础计量单位")
    private String baseUom;

    @Schema(description = "库存单位ID")
    private Long stockUnitId;

    @Schema(description = "库存单位符号")
    private String stockUnitCode;

    @Schema(description = "库存单位名称")
    private String stockUnitName;

    @Schema(description = "库存单位")
    private String stockUom;

    @Schema(description = "生产单位ID")
    private Long produceUnitId;

    @Schema(description = "生产单位符号")
    private String produceUnitCode;

    @Schema(description = "生产单位名称")
    private String produceUnitName;

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

    private String defaultRouteName;

    @Schema(description = "默认配方ID")
    private Long defaultRecipeId;

    private String defaultRecipeCode;

    private String defaultRecipeName;

    private Long defaultBomId;

    private String defaultBomCode;

    private String defaultBomName;

    @Schema(description = "质检模式")
    private String qualityControlMode;

    @Schema(description = "物料状态")
    @NotNull(message = "物料状态不能为空")
    private Integer materialStatus;

    @Schema(description = "MES产品")
    private Boolean mesSelectVisible;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "物料扩展属性列表")
    private List<HcMaterialExtAttrDO> materialExtAttrs;

}
