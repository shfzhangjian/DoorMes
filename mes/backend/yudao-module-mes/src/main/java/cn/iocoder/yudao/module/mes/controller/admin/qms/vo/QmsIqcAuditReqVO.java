package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - IQC进料检验单审核 Request VO")
@Data
public class QmsIqcAuditReqVO {

    @Schema(description = "IQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "IQC主单ID不能为空")
    private Long id;

    @Schema(description = "审核结果，APPROVE=通过，RETURN=退回", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @Schema(description = "审核备注；退回时必填并写入退回留痕")
    private String auditRemark;
}
