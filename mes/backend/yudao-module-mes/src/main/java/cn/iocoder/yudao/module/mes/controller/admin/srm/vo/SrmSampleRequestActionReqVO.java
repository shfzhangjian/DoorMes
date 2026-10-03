package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM样品需求流程动作 Request VO")
public class SrmSampleRequestActionReqVO {

    @Data
    public static class Submit {
        @Schema(description = "样品需求ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样品需求ID不能为空")
        private Long id;
    }

    @Data
    public static class Review {
        @Schema(description = "样品需求ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样品需求ID不能为空")
        private Long id;

        @Schema(description = "审核意见")
        private String opinion;

        @Schema(description = "下一个采购审核办理人ID")
        private Long nextPurchaseOwnerUserId;

        @Schema(description = "下一个采购审核办理人名称")
        private String nextPurchaseOwnerUserName;

        @Schema(description = "采购开发难点")
        private String purchaseDifficulty;
    }

    @Data
    public static class InitiatorDecision {
        @Schema(description = "样品需求ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样品需求ID不能为空")
        private Long id;

        @Schema(description = "是否直接归档")
        private Boolean directArchive;

        @Schema(description = "最终批准人ID")
        private Long finalApproverUserId;

        @Schema(description = "最终批准人名称")
        private String finalApproverUserName;

        @Schema(description = "归档或流转说明")
        private String opinion;
    }

    @Data
    public static class ArchiveConfirm {
        @Schema(description = "样品需求ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样品需求ID不能为空")
        private Long id;

        @Schema(description = "归档确认说明")
        private String opinion;
    }

    @Data
    public static class Copy {
        @Schema(description = "样品需求ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样品需求ID不能为空")
        private Long id;

        @Schema(description = "抄送人ID")
        private List<Long> ccUserIds;
    }

}
