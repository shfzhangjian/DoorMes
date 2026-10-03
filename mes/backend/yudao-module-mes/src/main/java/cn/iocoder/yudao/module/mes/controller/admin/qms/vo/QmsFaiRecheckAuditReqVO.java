package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FAI复检申请审核 Request VO")
@Data
public class QmsFaiRecheckAuditReqVO {

    @Schema(description = "复检申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "复检申请不能为空")
    private Long id;

    @Schema(description = "是否审核通过", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核意见")
    private String auditOpinion;
}
