package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 撤销已完成裁切FQC审核 Request VO")
@Data
public class QmsFqcAuditRevokeReqVO {

    @Schema(description = "FQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FQC主单ID不能为空")
    private Long id;

    @Schema(description = "撤销审核原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "撤销审核原因不能为空")
    @Size(max = 500, message = "撤销审核原因不能超过500个字符")
    private String revokeReason;
}
