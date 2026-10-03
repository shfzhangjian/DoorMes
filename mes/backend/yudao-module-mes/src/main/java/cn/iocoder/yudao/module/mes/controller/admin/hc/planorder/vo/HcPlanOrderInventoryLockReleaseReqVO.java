package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划中间边库挂接释放 Request VO")
@Data
public class HcPlanOrderInventoryLockReleaseReqVO {

    @Schema(description = "计划利库挂接ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划利库挂接ID不能为空")
    private Long lockId;

    @Schema(description = "释放数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "释放数量不能为空")
    @DecimalMin(value = "0.000001", message = "释放数量必须大于0")
    private BigDecimal releaseQty;

    @Schema(description = "释放原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "释放原因不能为空")
    private String releaseReason;

}
