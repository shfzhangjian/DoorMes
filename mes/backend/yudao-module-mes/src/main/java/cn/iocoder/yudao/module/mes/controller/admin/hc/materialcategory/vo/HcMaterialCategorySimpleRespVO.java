package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料分类 Simple Response VO")
@Data
public class HcMaterialCategorySimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "状态")
    private Integer status;

}