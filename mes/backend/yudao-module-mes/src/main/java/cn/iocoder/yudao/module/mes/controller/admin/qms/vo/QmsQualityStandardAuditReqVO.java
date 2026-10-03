package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 检验标准审核 Request VO")
@Data
public class QmsQualityStandardAuditReqVO {

    @Schema(description = "检验标准ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "检验标准ID不能为空")
    private Long id;

    @Schema(description = "审核结果；PASS-通过，REJECT-驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASS")
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @Schema(description = "驳回原因；审核结果为 REJECT 时必填")
    @Size(max = 300, message = "驳回原因不能超过 300 字")
    private String rejectReason;
}
