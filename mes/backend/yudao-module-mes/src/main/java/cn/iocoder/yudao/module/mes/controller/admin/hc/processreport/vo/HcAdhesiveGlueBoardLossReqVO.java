package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶胶板损耗报备 Request VO")
@Data
public class HcAdhesiveGlueBoardLossReqVO {

    @NotNull(message = "胶板领用ID不能为空")
    private Long glueBoardUsageId;

    @NotNull(message = "损耗起位置不能为空")
    @DecimalMin(value = "0", message = "损耗起位置不能为负数")
    private BigDecimal startPosition;

    @NotNull(message = "损耗长度不能为空")
    @DecimalMin(value = "0.001", message = "损耗长度必须大于0")
    private BigDecimal lossLength;

    private BigDecimal endPosition;

    private String lossReason;
}
