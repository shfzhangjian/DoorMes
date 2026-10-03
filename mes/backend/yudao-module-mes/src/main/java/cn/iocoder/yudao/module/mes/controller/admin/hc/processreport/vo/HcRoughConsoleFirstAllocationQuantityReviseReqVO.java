package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 一次磨皮分段报工米数修正 Request VO")
@Data
public class HcRoughConsoleFirstAllocationQuantityReviseReqVO {

    @NotNull(message = "一次磨皮加工单元不能为空")
    private Long firstAllocationId;

    @NotNull(message = "修正后确认加工米数不能为空")
    @DecimalMin(value = "0.001", message = "修正后确认加工米数必须大于0")
    private BigDecimal confirmedLength;

    @NotBlank(message = "修正原因不能为空")
    private String reason;
}
