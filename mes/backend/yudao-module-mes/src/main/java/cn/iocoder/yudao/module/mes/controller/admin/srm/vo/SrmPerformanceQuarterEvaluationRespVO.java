package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceQuarterEvaluationRespVO {

    private Long id;
    private String evaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Integer evalYear;
    private Integer evalQuarter;
    private Long templateId;
    private Long templateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionSnapshot;
    private BigDecimal totalScoreBaseline;
    private BigDecimal qualificationScoreSnapshot;
    private String status;
    private Long initiatorId;
    private String initiatorName;
    private BigDecimal autoScore;
    private BigDecimal manualScore;
    private BigDecimal totalScore;
    private String totalScoreDisplay;
    private String evalGrade;
    private Boolean redlineTriggered;
    private String redlineDescription;
    private String finalOpinion;
    private String remark;
    private Integer version;
    private String viewerScope;
    private Boolean canMaintain;
    private Boolean canPullActuals;
    private Boolean canScore;
    private Boolean canCalculate;
    private Boolean canStartSign;
    private Boolean canSign;
    private List<Item> items;
    private List<Trace> traces;
    private List<Sign> signs;
    private List<Log> logs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime autoCalculatedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime allScoredTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime calculatedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime signStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime archivedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Item {
        private Long id;
        private Long evaluationId;
        private Long templateItemId;
        private String groupCodeSnapshot;
        private String groupNameSnapshot;
        private Integer groupSort;
        private BigDecimal groupMaxScoreSnapshot;
        private String vetoOperatorSnapshot;
        private BigDecimal vetoScoreSnapshot;
        private String vetoResultSnapshot;
        private String indicatorCodeSnapshot;
        private String indicatorNameSnapshot;
        private Integer indicatorSort;
        private String scoringRuleSnapshot;
        private BigDecimal maxScoreSnapshot;
        private String defaultDeptNamesSnapshot;
        private Boolean attachmentRequiredSnapshot;
        private String indicatorTypeSnapshot;
        private BigDecimal targetValueSnapshot;
        private String targetUnitSnapshot;
        private BigDecimal redlineScoreSnapshot;
        private String scorerCandidateUserIds;
        private String scorerCandidateUserNames;
        private Long scorerUserId;
        private String scorerUserName;
        private String scorerUserNameDisplay;
        private String reporterCandidateUserIds;
        private String reporterCandidateUserNames;
        private Long reporterUserId;
        private String reporterUserName;
        private Long calcRuleId;
        private BigDecimal calcActualValue;
        private BigDecimal calcScore;
        private BigDecimal manualScore;
        private BigDecimal finalScore;
        private String finalScoreDisplay;
        private String scoreStatus;
        private String scoringDescription;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime actualScoreTime;
        private String dataStatus;
        private Boolean currentUserItem;
    }

    @Data
    public static class Trace {
        private Long id;
        private Long evaluationId;
        private Long evaluationItemId;
        private Long calcRuleId;
        private String nodeKey;
        private String parentNodeKey;
        private String nodeType;
        private String displayName;
        private String formulaExpr;
        private Long sourceReportId;
        private Long sourceValueId;
        private String sourceMetricCode;
        private String sourceMetricName;
        private Integer periodYear;
        private Integer periodQuarter;
        private Integer periodMonth;
        private BigDecimal rawValue;
        private BigDecimal normalizedValue;
        private String unit;
        private Integer attachmentCount;
        private Long reporterUserId;
        private String reporterUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reportTime;
        private String resultFlag;
        private String resultMessage;
    }

    @Data
    public static class Sign {
        private Long id;
        private Long evaluationId;
        private String deptCode;
        private String deptName;
        private Long userId;
        private String userName;
        private String signStatus;
        private String signResult;
        private String signOpinion;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime signTime;
        private Boolean currentUserSign;
    }

    @Data
    public static class Log {
        private Long id;
        private Long evaluationId;
        private String action;
        private String fromStatus;
        private String toStatus;
        private Long operatorId;
        private String operatorName;
        private String actionDescription;
        private String detailJson;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }

}
