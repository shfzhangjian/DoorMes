package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmPreliminaryEvaluationRespVO {

    private Long id;
    private String evaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long templateId;
    private Long templateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionSnapshot;
    private BigDecimal totalScoreBaseline;
    private BigDecimal qualificationScoreSnapshot;
    private String status;
    private String processInstanceId;
    private Long initiatorId;
    private String initiatorName;
    private BigDecimal totalScore;
    private String totalScoreDisplay;
    private String autoDecision;
    private Boolean vetoTriggered;
    private String vetoDescription;
    private String finalDecision;
    private String finalDescription;
    private Long generalManagerUserId;
    private String generalManagerUserName;
    private String generalManagerOpinion;
    private String generalManagerOpinionDisplay;
    private Long publisherId;
    private String publisherName;
    private String remark;
    private Integer version;
    private String viewerScope;
    private Boolean canMaintain;
    private Boolean canScore;
    private Boolean canCalculate;
    private Boolean canHandleGeneralManager;
    private Boolean canPublish;
    private List<Item> items;
    private List<Cc> ccUsers;
    private List<Log> logs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime allScoredTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime calculatedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generalManagerHandleTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;
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
        private String indicatorCodeSnapshot;
        private String indicatorNameSnapshot;
        private Integer indicatorSort;
        private String scoringRuleSnapshot;
        private BigDecimal maxScoreSnapshot;
        private String defaultDeptNamesSnapshot;
        private String scorerCandidateUserIds;
        private String scorerCandidateUserNames;
        private Boolean attachmentRequiredSnapshot;
        private Long scorerUserId;
        private String scorerUserName;
        private String scorerUserNameDisplay;
        private String scoreStatus;
        private BigDecimal actualScore;
        private String actualScoreDisplay;
        private String scoringDescription;
        private String scoringDescriptionDisplay;
        private Boolean currentUserItem;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime actualScoreTime;
    }

    @Data
    public static class Cc {
        private Long id;
        private Long userId;
        private String userName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime readTime;
    }

    @Data
    public static class Log {
        private Long id;
        private String action;
        private String fromStatus;
        private String toStatus;
        private String actionDescription;
        private Long operatorId;
        private String operatorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }

}
