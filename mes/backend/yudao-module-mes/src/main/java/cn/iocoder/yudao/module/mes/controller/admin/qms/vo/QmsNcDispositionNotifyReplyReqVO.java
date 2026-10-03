package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - NCR 处置执行通知人回复 Request VO")
@Data
public class QmsNcDispositionNotifyReplyReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "办理结论", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请填写通知人办理结论")
    @Size(max = 1000, message = "通知人办理结论不能超过1000个字符")
    private String replyConclusion;
}
