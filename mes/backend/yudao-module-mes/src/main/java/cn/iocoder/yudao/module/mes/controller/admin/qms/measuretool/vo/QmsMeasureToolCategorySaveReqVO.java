package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 量检具分类新增/修改 Request VO")
@Data
public class QmsMeasureToolCategorySaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码不能超过64个字符")
    private String categoryCode;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称不能超过128个字符")
    private String categoryName;

    @Schema(description = "分类说明")
    @Size(max = 500, message = "分类说明不能超过500个字符")
    private String description;

    @Schema(description = "状态(1启用 0停用)")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

}
