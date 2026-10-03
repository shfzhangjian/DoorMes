package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工异常位置明细保存 Request VO")
@Data
public class HcWetReportAbnormalPositionSaveReqVO {

    @Schema(description = "异常位置")
    @NotBlank(message = "异常位置不能为空")
    private String positionText;

    @Schema(description = "异常米数")
    @DecimalMin(value = "0", message = "异常位置米数不能为负数")
    private BigDecimal abnormalLength;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sortOrder;
}
