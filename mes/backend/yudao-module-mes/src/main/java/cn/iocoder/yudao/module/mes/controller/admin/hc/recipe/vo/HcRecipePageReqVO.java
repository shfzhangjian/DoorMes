package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 配方分页 Request VO")
@Data
public class HcRecipePageReqVO extends PageParam {

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "配方名称")
    private String recipeName;

    @Schema(description = "配方类型")
    private String recipeType;

    @Schema(description = "型号编号")
    private String modelCode;

    @Schema(description = "历史兼容查询字段")
    private Long productMaterialId;

    @Schema(description = "历史兼容查询字段")
    private String productMaterialCode;

    @Schema(description = "历史兼容查询字段")
    private String productMaterialName;

    @Schema(description = "版本号")
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
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
