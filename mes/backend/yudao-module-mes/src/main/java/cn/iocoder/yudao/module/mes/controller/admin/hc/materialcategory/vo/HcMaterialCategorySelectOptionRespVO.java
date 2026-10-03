package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料分类 Select Option Response VO")
@Data
public class HcMaterialCategorySelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "分类编码")
    private String code;

    @Schema(description = "状态")
    private Integer status;

}