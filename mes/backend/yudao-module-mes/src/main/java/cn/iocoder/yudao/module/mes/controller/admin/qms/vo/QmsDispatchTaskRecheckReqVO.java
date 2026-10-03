package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务退回重检 Request VO")
@Data
public class QmsDispatchTaskRecheckReqVO {

    @Schema(description = "质量任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "质量任务ID不能为空")
    private Long id;

    @Schema(description = "退回重检原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退回重检原因不能为空")
    @Size(max = 500, message = "退回重检原因不能超过500个字符")
    private String reason;
}
