package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 配方新增/修改 Request VO")
@Data
public class HcRecipeSaveReqVO {

    @Schema(description = "配方编码")
    @NotBlank(message = "配方编码不能为空")
    private String recipeCode;

    @Schema(description = "配方名称")
    @NotBlank(message = "配方名称不能为空")
    private String recipeName;

    @Schema(description = "配方类型")
    private String recipeType;

    @Schema(description = "型号编号")
    private String modelCode;

    @Schema(description = "累计使用次数")
    private Long usageCount;

    @Schema(description = "历史兼容字段，可为空")
    private Long productMaterialId;

    @Schema(description = "历史兼容字段，可为空")
    private String productMaterialCode;

    @Schema(description = "历史兼容字段，可为空")
    private String productMaterialName;

    @Schema(description = "版本号")
    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    @Schema(description = "标准固含")
    private BigDecimal solidContentStd;

    @Schema(description = "标准粘度")
    private BigDecimal viscosityStd;

    @Schema(description = "理论得率")
    private BigDecimal yieldRate;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expireDate;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "配方明细列表")
    private List<HcRecipeItemDO> recipeItems;
}
