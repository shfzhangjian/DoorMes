package cn.iocoder.yudao.module.mes.controller.admin.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - MES物料主数据新增/修改 Request VO")
@Data
public class MesMaterialSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9152")
    private Long id;

    @Schema(description = "物料编码 (ERP码)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "物料编码 (ERP码)不能为空")
    private String code;

    @Schema(description = "物料名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "物料名称不能为空")
    private String name;

    @Schema(description = "物料分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "物料分类不能为空")
    private String category;

    @Schema(description = "物料来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "BUY")
    @NotEmpty(message = "物料来源不能为空")
    private String materialSource;

    @Schema(description = "材质牌号")
    private String materialGrade;

    @Schema(description = "规格型号")
    private String spec;

    @Schema(description = "计量单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "计量单位不能为空")
    private String unit;

    @Schema(description = "单重 (kg) ")
    private BigDecimal unitWeight;

    @Schema(description = "理论废品率 (%)")
    private BigDecimal scrapRate;

    @Schema(description = "图纸/规范附件路径", example = "https://oss.example.com/drawings/v1.pdf")
    private String drawingUrl;

    @Schema(description = "采购/生产提前期(天)", example = "7")
    private Integer leadTime;

    @Schema(description = "默认供应商关联ID", example = "1001")
    private Long supplierId;

    @Schema(description = "默认供应商关联名称", example = "xxx供应商")
    private String supplierName;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
