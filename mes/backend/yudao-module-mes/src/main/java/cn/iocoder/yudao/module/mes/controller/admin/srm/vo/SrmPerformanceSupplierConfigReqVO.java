package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceSupplierConfigReqVO {

    @NotNull(message = "请选择供应商季度评分配置")
    private Long configId;
    @NotNull(message = "请选择季度评价模板版本")
    private Long templateVersionId;
    @Valid
    @NotEmpty(message = "请维护模板指标人员配置")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "模板指标不能为空")
        private Long templateItemId;
        private String indicatorType;
        private BigDecimal targetValue;
        private String targetUnit;
        private BigDecimal redlineScore;
        private Long scorerUserId;
        private List<Long> scorerCandidateUserIds;
        private Long reporterUserId;
        private List<Long> reporterCandidateUserIds;
        private String remark;
        private CalcRule calcRule;
    }

    @Data
    public static class CalcRule {
        private Long id;
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
