package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 物料分类新增/修改 Request VO")
@Data
public class HcMaterialCategorySaveReqVO {

    @Schema(description = "父级分类ID")
    @NotNull(message = "父级分类不能为空")
    private Long parentId;

    @Schema(description = "分类编码")
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @Schema(description = "分类编号路径")
    private String categoryCodePath;

    @Schema(description = "分类名称路径")
    private String categoryNamePath;

    @Schema(description = "兼容旧字段的分类路径")
    private String categoryPath;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "主键")
    private Long id;

}