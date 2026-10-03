package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "mes_qms_iqc_item", autoResultMap = true)
@KeySequence("mes_qms_iqc_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIqcItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long iqcId;

    private String iqcNo;

    private Long standardItemId;

    private String inspectionItem;

    private String itemType;

    private Integer expiryDays;

    private Boolean attachmentEnabled;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;

    private BigDecimal targetValue;

    private String standardDesc;

    private String unit;

    private String inspectionMethod;

    private String testFrequencyJudgement;

    private String entryRuleType;

    private String ruleDescription;

    private String valueTemplate;

    private String valueTemplateName;

    private String templateParams;

    private String testTool;

    private Integer sampleSize;

    private BigDecimal minValueLimit;

    private BigDecimal maxValueLimit;

    /** 平均值内控快照；与单次测量限值独立判定。 */
    private BigDecimal avgMinLimit;
    private BigDecimal avgMaxLimit;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal maxValue;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal minValue;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal averageValue;

    private String itemResult;

    private String judgmentReason;

    @TableField("is_spc")
    private Boolean isSpc;

    private Boolean recheckItemFlag;

    private Integer sort;

    private Long tenantId;
}
