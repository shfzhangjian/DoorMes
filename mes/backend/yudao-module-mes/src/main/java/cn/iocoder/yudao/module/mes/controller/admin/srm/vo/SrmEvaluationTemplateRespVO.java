package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmEvaluationTemplateRespVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String sceneType;
    private String materialType;
    private Long currentVersionId;
    private String currentVersionNo;
    private String status;
    private String remark;
    private Integer version;
    private Version currentVersion;
    private List<Version> versions;
    private List<Log> logs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Version {
        private Long id;
        private Long templateId;
        private String versionNo;
        private String status;
        private BigDecimal totalScore;
        private BigDecimal qualificationScore;
        private Long previousVersionId;
        private String changeSummary;
        private Long submitterId;
        private String submitterName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime submitTime;
        private Long auditorId;
        private String auditorName;
        private String auditOpinion;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime auditTime;
        private Long publisherId;
        private String publisherName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime publishTime;
        private String remark;
        private Integer version;
        private List<Item> items;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime updateTime;
    }

    @Data
    public static class Item {
        private Long id;
        private Long versionId;
        private String groupCode;
        private String groupName;
        private Integer groupSort;
        private BigDecimal groupMaxScore;
        private String vetoOperator;
        private BigDecimal vetoScore;
        private String vetoResult;
        private String indicatorCode;
        private String indicatorName;
        private Integer indicatorSort;
        private String scoringRule;
        private BigDecimal maxScore;
        private String defaultDeptNames;
        private Long defaultScorerUserId;
        private String defaultScorerUserName;
        private String defaultScorerUserIds;
        private String defaultScorerUserNames;
        private Boolean attachmentRequired;
    }

    @Data
    public static class Log {
        private Long id;
        private Long templateId;
        private Long versionId;
        private String action;
        private String actionDescription;
        private Long operatorId;
        private String operatorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }

}
