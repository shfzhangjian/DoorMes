package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

public final class SrmSampleEvaluationActionReqVO {

    private SrmSampleEvaluationActionReqVO() {
    }

    @Data
    public static class Submit {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        @NotNull(message = "检测办理人不能为空")
        private Long inspectorUserId;
        private String inspectorUserName;
    }

    @Data
    public static class InspectionReport {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        private Long confirmUserId;
        private String confirmUserName;
        @Valid
        @NotEmpty(message = "检验项目不能为空")
        private List<SrmSampleEvaluationSaveReqVO.Item> items;
    }

    @Data
    public static class ValueConfirm {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        @NotNull(message = "确认结果不能为空")
        private Boolean passed;
        private String opinion;
        @Valid
        private List<SignUser> signUsers;
    }

    @Data
    public static class SignUser {
        private String deptCode;
        @NotEmpty(message = "会签部门不能为空")
        private String deptName;
        @NotNull(message = "会签人员不能为空")
        private Long userId;
        private String userName;
    }

    @Data
    public static class Sign {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        private Long signId;
        @NotEmpty(message = "会签结论不能为空")
        private String result;
        private String opinion;
    }

    @Data
    public static class InitiatorDecision {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        private Boolean directArchive;
        private Long finalApproverUserId;
        private String finalApproverUserName;
        private String opinion;
    }

    @Data
    public static class FinalApprove {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        @NotEmpty(message = "批准意见不能为空")
        private String opinion;
    }

    @Data
    public static class ArchiveConfirm {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        private String opinion;
    }

    @Data
    public static class WithdrawConfirm {
        @NotNull(message = "样品评价单ID不能为空")
        private Long id;
        private String opinion;
    }

}
