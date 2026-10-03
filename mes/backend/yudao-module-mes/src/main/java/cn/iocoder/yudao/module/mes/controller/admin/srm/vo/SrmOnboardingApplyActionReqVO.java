package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM导入申请流程动作 Request VO")
public class SrmOnboardingApplyActionReqVO {

    @Data
    public static class Submit {
        @Schema(description = "导入申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入申请ID不能为空")
        private Long id;
    }

    @Data
    public static class Review {
        @Schema(description = "导入申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入申请ID不能为空")
        private Long id;

        @Schema(description = "审批结果：PASS同意、REJECT不同意", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "审批结果不能为空")
        private String result;

        @Schema(description = "审批意见")
        private String opinion;
    }

    @Data
    public static class PurchaseIntake {
        @Schema(description = "导入申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入申请ID不能为空")
        private Long id;

        @Schema(description = "使用部门负责人ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "使用部门负责人不能为空")
        private Long useDeptReviewerUserId;

        @Schema(description = "使用部门负责人")
        private String useDeptReviewerUserName;

        @Schema(description = "办理意见")
        private String opinion;

        @Valid
        @NotEmpty(message = "会签人员不能为空")
        private List<SignUser> signUsers;
    }

    @Data
    public static class SignUser {
        @Schema(description = "会签部门编码")
        private String deptCode;

        @Schema(description = "会签部门", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "会签部门不能为空")
        private String deptName;

        @Schema(description = "会签人员ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "会签人员不能为空")
        private Long userId;

        @Schema(description = "会签人员")
        private String userName;
    }

    @Data
    public static class Sign {
        @Schema(description = "导入申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入申请ID不能为空")
        private Long id;

        @Schema(description = "会签明细ID")
        private Long signId;

        @Schema(description = "会签结果：PASS同意、REJECT不同意", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "会签结果不能为空")
        private String result;

        @Schema(description = "会签意见")
        private String opinion;
    }

    @Data
    public static class PurchaseTransfer {
        @Schema(description = "导入申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入申请ID不能为空")
        private Long id;

        @Schema(description = "转办动作：GENERAL_MANAGER总经理审核、ASSIGN_ENTRY交办办理、ARCHIVE完成归档", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "转办动作不能为空")
        private String transferAction;

        @Schema(description = "总经理办理人ID")
        private Long generalManagerUserId;

        @Schema(description = "总经理办理人")
        private String generalManagerUserName;

        @Schema(description = "物料编码录入人ID")
        private Long materialEntryUserId;

        @Schema(description = "物料编码录入人")
        private String materialEntryUserName;

        @Schema(description = "供方清单录入人ID")
        private Long supplierRosterEntryUserId;

        @Schema(description = "供方清单录入人")
        private String supplierRosterEntryUserName;

        @Schema(description = "转办意见")
        private String opinion;
    }

    @Data
    public static class Entry {
        @Schema(description = "导入申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "导入申请ID不能为空")
        private Long id;

        @Schema(description = "物料编码")
        private String materialEntryCode;

        @Schema(description = "物料编码录入意见")
        private String materialEntryOpinion;

        @Schema(description = "供方编码")
        private String supplierRosterEntryCode;

        @Schema(description = "供方清单录入意见")
        private String supplierRosterEntryOpinion;
    }

}
