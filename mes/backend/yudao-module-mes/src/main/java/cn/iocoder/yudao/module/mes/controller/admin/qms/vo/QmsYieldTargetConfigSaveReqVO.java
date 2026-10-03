package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 工序理论产量配置保存 Request VO")
@Data
public class QmsYieldTargetConfigSaveReqVO {

    private Long id;

    @Schema(description = "产品型号前缀，如 W26、W33")
    @NotBlank(message = "产品型号不能为空")
    private String modelCode;

    @Schema(description = "工序编码")
    @NotBlank(message = "工序不能为空")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "计量单位")
    @NotBlank(message = "计量单位不能为空")
    private String measureUnit;

    @Schema(description = "目标类型：OUTPUT_QTY 产出绝对值，YIELD_RATE 良品率百分比")
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @Schema(description = "母卷分段数：W33 必填 2/3/4，其他型号为 0")
    private Integer segmentCount;

    @Schema(description = "理论产量")
    @NotNull(message = "理论产量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "理论产量必须大于0")
    private BigDecimal targetQualifiedQty;

    @Schema(description = "状态：0 启用，1 禁用")
    private Integer status;

    private Integer sort;
    private String remark;
}
