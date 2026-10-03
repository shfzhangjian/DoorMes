package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件撤回修改 Request VO")
@Data
public class QmsExceptionEventWithdrawReqVO {

    @Schema(description = "异常事件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常事件ID不能为空")
    private Long id;

    @Schema(description = "撤回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "撤回原因不能为空")
    @Size(max = 500, message = "撤回原因不能超过 500 个字符")
    private String reason;

}
