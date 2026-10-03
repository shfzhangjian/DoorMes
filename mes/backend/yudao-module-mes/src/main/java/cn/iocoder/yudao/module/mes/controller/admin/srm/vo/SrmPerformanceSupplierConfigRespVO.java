package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceSupplierConfigRespVO {

    private Long id;
    private String configNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Long currentTemplateId;
    private Long currentTemplateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionNoSnapshot;
    private String status;
    private String remark;
    private Integer version;
    private List<Item> items;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Item {
        private Long id;
        private Long configId;
        private Long templateId;
        private Long templateVersionId;
        private Long templateItemId;
        private String groupCodeSnapshot;
        private String groupNameSnapshot;
        private Integer groupSort;
        private BigDecimal groupMaxScoreSnapshot;
        private String indicatorCodeSnapshot;
        private String indicatorNameSnapshot;
        private Integer indicatorSort;
        private String scoringRuleSnapshot;
        private BigDecimal maxScoreSnapshot;
        private String defaultDeptNames;
        private String indicatorType;
        private BigDecimal targetValue;
        private String targetUnit;
        private BigDecimal redlineScore;
        private String scorerCandidateUserIds;
        private String scorerCandidateUserNames;
        private Long scorerUserId;
        private String scorerUserName;
        private String reporterCandidateUserIds;
        private String reporterCandidateUserNames;
        private Long reporterUserId;
        private String reporterUserName;
        private Long calcRuleId;
        private String remark;
        private CalcRule calcRule;
    }

    @Data
    public static class CalcRule {
        private Long id;
        private Long configId;
        private Long configItemId;
        private Long templateItemId;
        private String ruleCode;
        private String ruleName;
        private String formulaExpr;
        private String scoreFormulaExpr;
        private String periodScope;
        private String aggregateMethod;
        private String missingPolicy;
        private Boolean enabled;
        private String remark;
        private List<CalcNode> nodes;
    }

    @Data
    public static class CalcNode {
        private Long id;
        private Long ruleId;
        private Long parentNodeId;
        private String nodeKey;
        private String nodeName;
        private String nodeType;
        private String sourceMetricCode;
        private String sourceMetricName;
        private String operator;
        private String aggregateMethod;
        private String unit;
        private Integer sortNo;
        private Boolean requiredFlag;
    }

}
