package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 配方 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcRecipeRespVO {

    @Schema(description = "配方编码")
    @ExcelProperty("配方编码")
    private String recipeCode;

    @Schema(description = "配方名称")
    @ExcelProperty("配方名称")
    private String recipeName;

    @Schema(description = "配方类型")
    @ExcelProperty("配方类型")
    private String recipeType;

    @Schema(description = "型号编号")
    @ExcelProperty("型号编号")
    private String modelCode;

    @Schema(description = "累计使用次数")
    @ExcelProperty("累计使用次数")
    private Long usageCount;

    @Schema(description = "历史兼容字段，可为空")
    @ExcelProperty("产品物料ID")
    private Long productMaterialId;

    @Schema(description = "历史兼容字段，可为空")
    @ExcelProperty("产品物料编码")
    private String productMaterialCode;

    @Schema(description = "历史兼容字段，可为空")
    @ExcelProperty("产品物料名称")
    private String productMaterialName;

    @Schema(description = "版本号")
    @ExcelProperty("版本号")
    private String versionNo;

    @Schema(description = "标准固含")
    @ExcelProperty("标准固含")
    private BigDecimal solidContentStd;

    @Schema(description = "标准粘度")
    @ExcelProperty("标准粘度")
    private BigDecimal viscosityStd;

    @Schema(description = "理论得率")
    @ExcelProperty("理论得率")
    private BigDecimal yieldRate;

    @Schema(description = "生效日期")
    @ExcelProperty("生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    @ExcelProperty("失效日期")
    private LocalDate expireDate;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
