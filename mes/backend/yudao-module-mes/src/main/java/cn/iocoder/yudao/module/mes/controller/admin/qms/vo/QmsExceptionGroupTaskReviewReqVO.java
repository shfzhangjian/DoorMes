package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件小组任务复核 Request VO")
@Data
public class QmsExceptionGroupTaskReviewReqVO {

    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Schema(description = "复核动作：ACCEPT/RETURN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "复核动作不能为空")
    private String actionCode;

    @Schema(description = "复核意见")
    private String opinion;
}
