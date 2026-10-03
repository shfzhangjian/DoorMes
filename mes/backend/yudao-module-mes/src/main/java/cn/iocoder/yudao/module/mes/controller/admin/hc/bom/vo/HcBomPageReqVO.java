package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - BOM分页 Request VO")
@Data
public class HcBomPageReqVO extends PageParam {

    @Schema(description = "BOM编码")
    private String bomCode;

    @Schema(description = "BOM名称")
    private String bomName;

    @Schema(description = "产出物料ID")
    private Long productMaterialId;

    @Schema(description = "产出物料编码")
    private String productMaterialCode;

    @Schema(description = "产出物料名称")
    private String productMaterialName;

    @Schema(description = "产品物料关键词")
    private String productMaterialKeyword;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "成品规格/尺寸")
    private String productSpec;

    @Schema(description = "关联配方ID")
    private Long recipeId;

    @Schema(description = "关联配方编码")
    private String recipeCode;

    @Schema(description = "关联配方名称")
    private String recipeName;

    @Schema(description = "配方关键词")
    private String recipeKeyword;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "BOM类型")
    private String bomType;

    @Schema(description = "标准良率")
    private BigDecimal yieldRate;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
