package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 配方 Simple Response VO")
@Data
public class HcRecipeSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "配方名称")
    private String recipeName;

    @Schema(description = "状态")
    private Integer status;

}