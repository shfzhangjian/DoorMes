package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

public final class SrmPreliminaryEvaluationActionReqVO {

    private SrmPreliminaryEvaluationActionReqVO() {
    }

    @Data
    public static class AssignScorer {
        @NotNull(message = "初评单ID不能为空")
        private Long evaluationId;
        @Valid
        @NotEmpty(message = "评分人分配不能为空")
        private List<Assignment> assignments;
    }

    @Data
    public static class Assignment {
        @NotNull(message = "指标ID不能为空")
        private Long itemId;
        @NotNull(message = "评分人不能为空")
        private Long scorerUserId;
        private List<Long> scorerCandidateUserIds;
    }

    @Data
    public static class Score {
        @NotNull(message = "初评单ID不能为空")
        private Long evaluationId;
        @Valid
        @NotEmpty(message = "评分明细不能为空")
        private List<ScoreItem> items;
    }

    @Data
    public static class ScoreItem {
        @NotNull(message = "指标ID不能为空")
        private Long itemId;
        @NotNull(message = "实际得分不能为空")
        @DecimalMin(value = "0", message = "实际得分不能小于0")
        private BigDecimal actualScore;
        private String scoringDescription;
    }

    @Data
    public static class Decision {
        @NotNull(message = "初评单ID不能为空")
        private Long evaluationId;
        @NotNull(message = "最终得分不能为空")
        @DecimalMin(value = "0", message = "最终得分不能小于0")
        private BigDecimal totalScore;
        @NotEmpty(message = "最终导入判定不能为空")
        private String finalDecision;
        @NotEmpty(message = "最终判定说明不能为空")
        private String finalDescription;
        private Long generalManagerUserId;
    }

    @Data
    public static class GeneralManagerOpinion {
        @NotNull(message = "初评单ID不能为空")
        private Long evaluationId;
        @NotEmpty(message = "总经理意见不能为空")
        private String opinion;
    }

    @Data
    public static class Publish {
        @NotNull(message = "初评单ID不能为空")
        private Long evaluationId;
        private List<Long> ccUserIds;
    }

}
