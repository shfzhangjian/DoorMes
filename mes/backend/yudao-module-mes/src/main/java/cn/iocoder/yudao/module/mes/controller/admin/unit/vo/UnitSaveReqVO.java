package cn.iocoder.yudao.module.mes.controller.admin.unit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - MES计量单位新增/修改 Request VO")
@Data
public class UnitSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "11731")
    private Long id;

    @Schema(description = "单位符号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "单位符号不能为空")
    private String code;

    @Schema(description = "单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @NotEmpty(message = "单位名称不能为空")
    private String name;

    @Schema(description = "维度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "维度不能为空")
    private String category;

    @Schema(description = "基准单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "基准单位不能为空")
    private Boolean base;

    @Schema(description = "换算率", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "换算率不能为空")
    private BigDecimal ratio;

    @Schema(description = "保留小数位数")
    private Integer precision;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
