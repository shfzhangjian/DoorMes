package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料分类分页 Request VO")
@Data
public class HcMaterialCategoryPageReqVO extends PageParam {

    @Schema(description = "父级分类ID")
    private Long parentId;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类路径")
    private String categoryPath;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

}