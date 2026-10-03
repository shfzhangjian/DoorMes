package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FAI首件检验单整单审核 Request VO")
@Data
public class QmsFaiAuditReqVO {

    @Schema(description = "FAI主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FAI主单ID不能为空")
    private Long id;

    @Schema(description = "审核结果，PASS=通过，REJECT=驳回退回重填，FAIL=不合格终态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @Schema(description = "驳回原因/不合格说明")
    private String rejectReason;

    @Schema(description = "检验样本组审核确认明细；为空时兼容旧整单审核")
    private List<@Valid FaiGroupAudit> groups;

    @Schema(description = "管理后台 - FAI检验样本组审核确认")
    @Data
    public static class FaiGroupAudit {

        @Schema(description = "FAI检验项ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "FAI检验项ID不能为空")
        private Long itemId;

        @Schema(description = "样本组Key", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "样本组Key不能为空")
        private String groupKey;

        @Schema(description = "样本位置/组名")
        private String samplePosition;

        @Schema(description = "样本组审核结果，CONFIRM=确认，REJECT_RECHECK=驳回重检", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "样本组审核结果不能为空")
        private String auditResult;

        @Schema(description = "检验说明")
        private String auditRemark;
    }
}
