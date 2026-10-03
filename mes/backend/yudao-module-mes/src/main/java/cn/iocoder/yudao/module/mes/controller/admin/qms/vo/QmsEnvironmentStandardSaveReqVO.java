package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 温湿度标准范围保存 Request VO")
@Data
public class QmsEnvironmentStandardSaveReqVO {

    @Schema(description = "车间编码；字典 mes_qms_environment_workshop", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "车间不能为空")
    private String workshopCode;

    @Schema(description = "车间名称快照")
    private String workshopName;

    @Schema(description = "温度下限", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "温度下限不能为空")
    private BigDecimal temperatureMin;

    @Schema(description = "温度上限", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "温度上限不能为空")
    private BigDecimal temperatureMax;

    @Schema(description = "湿度下限", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "湿度下限不能为空")
    private BigDecimal humidityMin;

    @Schema(description = "湿度上限", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "湿度上限不能为空")
    private BigDecimal humidityMax;

    @Schema(description = "备注")
    private String remark;
}
