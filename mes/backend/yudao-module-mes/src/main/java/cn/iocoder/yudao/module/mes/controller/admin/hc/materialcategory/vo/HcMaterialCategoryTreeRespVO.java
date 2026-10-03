package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 物料分类 Tree Response VO")
@Data
public class HcMaterialCategoryTreeRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "父级分类ID")
    private Long parentId;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类编号路径")
    private String categoryCodePath;

    @Schema(description = "分类名称路径")
    private String categoryNamePath;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "子节点")
    private List<HcMaterialCategoryTreeRespVO> children;

}