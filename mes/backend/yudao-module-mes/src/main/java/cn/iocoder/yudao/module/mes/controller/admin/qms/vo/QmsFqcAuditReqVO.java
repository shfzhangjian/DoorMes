package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FQC成品检验单整单审核 Request VO")
@Data
public class QmsFqcAuditReqVO {

    @Schema(description = "FQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FQC主单ID不能为空")
    private Long id;

    @Schema(description = "审核结果，PASS=通过，REJECT=驳回退回重填，FQCL=不合格终态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @Schema(description = "驳回原因/不合格说明")
    private String rejectReason;
}
