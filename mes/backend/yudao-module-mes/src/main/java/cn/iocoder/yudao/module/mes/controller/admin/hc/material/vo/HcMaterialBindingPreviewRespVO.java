package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 物料工艺挂接预览 Response VO")
@Data
public class HcMaterialBindingPreviewRespVO {

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "型号规则ID")
    private Long modelCodeRuleId;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "型号段值快照JSON")
    private String modelSegmentsJson;

    @Schema(description = "当前默认配方ID")
    private Long defaultRecipeId;

    @Schema(description = "当前默认配方编码")
    private String defaultRecipeCode;

    @Schema(description = "当前默认配方名称")
    private String defaultRecipeName;

    @Schema(description = "规则推荐配方ID")
    private Long suggestedRecipeId;

    @Schema(description = "规则推荐配方编码")
    private String suggestedRecipeCode;

    @Schema(description = "规则推荐配方名称")
    private String suggestedRecipeName;

    @Schema(description = "当前默认工艺路线ID")
    private Long defaultRouteId;

    @Schema(description = "当前默认工艺路线编码")
    private String defaultRouteCode;

    @Schema(description = "当前默认工艺路线名称")
    private String defaultRouteName;

    @Schema(description = "规则推荐工艺路线ID")
    private Long suggestedRouteId;

    @Schema(description = "规则推荐工艺路线编码")
    private String suggestedRouteCode;

    @Schema(description = "规则推荐工艺路线名称")
    private String suggestedRouteName;

    @Schema(description = "命中的工艺用料分组编码")
    private List<String> consumeGroupCodes;

    @Schema(description = "工艺用料清单候选")
    private List<HcMaterialBindingPreviewBomRespVO> bomCandidates;

}
