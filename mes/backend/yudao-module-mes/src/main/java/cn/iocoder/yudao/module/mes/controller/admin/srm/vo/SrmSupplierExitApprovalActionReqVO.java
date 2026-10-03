package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM供方退出审批流程动作 Request VO")
public class SrmSupplierExitApprovalActionReqVO {

    @Data
    public static class Submit {
        @Schema(description = "退出审批ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "退出审批ID不能为空")
        private Long id;
    }

    @Data
    public static class Review {
        @Schema(description = "退出审批ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "退出审批ID不能为空")
        private Long id;

        @Schema(description = "审批结果：PASS同意、REJECT不同意", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "审批结果不能为空")
        private String result;

        @Schema(description = "审批意见")
        private String opinion;
    }

    @Data
    public static class PurchaseIntake {
        @Schema(description = "退出审批ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "退出审批ID不能为空")
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
        @Schema(description = "退出审批ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "退出审批ID不能为空")
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
        @Schema(description = "退出审批ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "退出审批ID不能为空")
        private Long id;

        @Schema(description = "转办动作：GENERAL_MANAGER总经理审核、ASSIGN_ENTRY交办办理、ARCHIVE完成归档", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "转办动作不能为空")
        private String transferAction;

        @Schema(description = "总经理办理人ID")
        private Long generalManagerUserId;

        @Schema(description = "总经理办理人")
        private String generalManagerUserName;

        @Schema(description = "合格供方物料清单移除人ID")
        private Long materialEntryUserId;

        @Schema(description = "合格供方物料清单移除人")
        private String materialEntryUserName;

        @Schema(description = "库存/账务处理人ID")
        private Long supplierRosterEntryUserId;

        @Schema(description = "库存/账务处理人")
        private String supplierRosterEntryUserName;

        @Schema(description = "转办意见")
        private String opinion;
    }

    @Data
    public static class Entry {
        @Schema(description = "退出审批ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "退出审批ID不能为空")
        private Long id;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "合格供方物料清单移除完成日期")
        private LocalDate materialCodeCompleteDate;

        @Schema(description = "合格供方物料清单移除说明")
        private String materialEntryOpinion;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "库存/账务处理完成日期")
        private LocalDate supplierRosterCompleteDate;

        @Schema(description = "库存/账务处理说明")
        private String supplierRosterEntryOpinion;
    }

}
