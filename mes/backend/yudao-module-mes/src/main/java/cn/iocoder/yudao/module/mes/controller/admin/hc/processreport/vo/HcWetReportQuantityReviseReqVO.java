package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工米数修正 Request VO")
@Data
public class HcWetReportQuantityReviseReqVO {

    @NotNull(message = "湿法报工记录不能为空")
    private Long id;

    @NotNull(message = "修正后收卷米数不能为空")
    @DecimalMin(value = "0.001", message = "修正后收卷米数必须大于0")
    private BigDecimal receiveLength;

    @NotBlank(message = "修正原因不能为空")
    private String reason;
}
