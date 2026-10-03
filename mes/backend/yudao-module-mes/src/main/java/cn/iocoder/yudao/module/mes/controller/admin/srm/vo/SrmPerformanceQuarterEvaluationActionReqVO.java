package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

public final class SrmPerformanceQuarterEvaluationActionReqVO {

    private SrmPerformanceQuarterEvaluationActionReqVO() {
    }

    @Data
    public static class Score {
        @NotNull(message = "季度评价单ID不能为空")
        private Long evaluationId;
        @Valid
        @NotEmpty(message = "评分明细不能为空")
        private List<ScoreItem> items;
    }

    @Data
    public static class ScoreItem {
        @NotNull(message = "指标ID不能为空")
        private Long itemId;
        @NotNull(message = "人工评分不能为空")
        @DecimalMin(value = "0", message = "人工评分不能小于0")
        private BigDecimal manualScore;
        private String scoringDescription;
    }

    @Data
    public static class StartSign {
        @NotNull(message = "季度评价单ID不能为空")
        private Long evaluationId;
        @Valid
        @NotEmpty(message = "会签人员不能为空")
        private List<Signer> signers;
    }

    @Data
    public static class Signer {
        @NotNull(message = "会签人不能为空")
        private Long userId;
        private String userName;
        private String deptCode;
        private String deptName;
    }

    @Data
    public static class Sign {
        @NotNull(message = "季度评价单ID不能为空")
        private Long evaluationId;
        @NotEmpty(message = "会签结果不能为空")
        private String signResult;
        private String signOpinion;
    }

}
