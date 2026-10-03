package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - NCR撤回修改 Request VO")
@Data
public class QmsNcRecordWithdrawReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "撤回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写撤回原因")
    @Size(max = 500, message = "撤回原因不能超过500个字符")
    private String reason;

}
