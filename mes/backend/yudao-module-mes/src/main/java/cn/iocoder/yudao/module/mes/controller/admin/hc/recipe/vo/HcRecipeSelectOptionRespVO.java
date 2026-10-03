package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 配方 Select Option Response VO")
@Data
public class HcRecipeSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "配方编码")
    private String code;

    @Schema(description = "配方型号编码")
    private String modelCode;

    @Schema(description = "状态")
    private Integer status;
}
