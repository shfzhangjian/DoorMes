package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 分切来源段完成 Request VO")
@Data
public class HcSlittingSourceCompleteReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotNull(message = "来源粘胶报工ID不能为空")
    private Long sourceAdhesiveReportId;

    @NotNull(message = "确认人不能为空")
    @Schema(description = "认证确认人用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long confirmerUserId;

    @Schema(description = "是否在完成时处置剩余尾料")
    private Boolean disposeRemainingTail;

    @Schema(description = "尾料处置长度；不传时按当前剩余可加工长度处置")
    private BigDecimal tailDisposalLength;

    @Schema(description = "尾料处置原因")
    private String tailDisposalReason;
}
