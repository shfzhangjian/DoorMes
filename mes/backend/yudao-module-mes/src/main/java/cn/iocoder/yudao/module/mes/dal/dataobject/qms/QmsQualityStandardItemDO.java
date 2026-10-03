package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_quality_standard_item")
@KeySequence("mes_qms_quality_standard_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsQualityStandardItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long standardId;

    private Integer sort;

    private String inspectionItem;

    private String itemType;

    private Integer expiryDays;

    private Boolean attachmentEnabled;

    private Boolean actualValueRequired;

    private BigDecimal targetValue;

    private BigDecimal minValue;

    private Integer minValueScale;

    private BigDecimal maxValue;

    private Integer maxValueScale;

    private String standardDesc;

    private String unit;

    private String inspectionMethod;

    private String testFrequencyJudgement;

    private String entryRuleType;

    private Long entryRuleTemplateId;

    private String entryRuleTemplateName;

    private String ruleDescription;

    private String valueTemplate;

    private String judgmentMetric;

    private String templateParams;

    private BigDecimal avgMinLimit;

    private Integer avgMinLimitScale;

    private BigDecimal avgMaxLimit;

    private Integer avgMaxLimitScale;

    private BigDecimal stdMinLimit;

    private Integer stdMinLimitScale;

    private BigDecimal stdMaxLimit;

    private Integer stdMaxLimitScale;

    private Long sheetTemplateId;

    private String sheetSectionCode;

    private String sheetMetricCode;

    private String sheetFieldCode;

    private String testTool;

    private Integer sampleSize;

    @TableField("is_spc")
    private Boolean isSpc;

    private String standardNo;

    private String standardName;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private Long processId;

    private String processCode;

    private String processName;

    private String applyType;

    private String version;

    private Integer status;

    private Long tenantId;
}
