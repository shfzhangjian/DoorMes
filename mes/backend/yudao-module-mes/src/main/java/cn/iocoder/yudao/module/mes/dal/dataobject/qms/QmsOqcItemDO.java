package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_oqc_item")
@KeySequence("mes_qms_oqc_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsOqcItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long oqcId;
    private String oqcNo;
    private Long standardItemId;
    private String category;
    private String inspectionItem;
    private String itemType;
    private BigDecimal targetValue;
    private String standardDesc;
    private String unit;
    private String ruleDescription;
    private String inspectionMethod;
    private String testFrequencyJudgement;
    private String valueTemplate;
    private String valueTemplateName;
    private String judgmentMetric;
    private String templateParams;
    private BigDecimal avgMinLimit;
    private BigDecimal avgMaxLimit;
    private BigDecimal stdMinLimit;
    private BigDecimal stdMaxLimit;
    private String testTool;
    private Integer sampleSize;
    private BigDecimal minValueLimit;
    private BigDecimal maxValueLimit;
    private BigDecimal maxValue;
    private BigDecimal minValue;
    private BigDecimal averageValue;
    private String itemResult;
    private BigDecimal operatorMax;
    private BigDecimal operatorMin;
    private BigDecimal operatorAvg;
    private String operatorResult;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operatorTime;
    private BigDecimal qaMax;
    private BigDecimal qaMin;
    private BigDecimal qaAvg;
    private String qaResult;
    private Long qaInspectorId;
    private String qaInspectorName;
    private LocalDateTime qaTime;
    private BigDecimal calculatedAvg;
    private BigDecimal calculatedStd;
    private BigDecimal calculatedMin;
    private BigDecimal calculatedMax;
    private Integer requiredSampleCount;
    private Integer completedSampleCount;
    private Integer abnormalSampleCount;
    private String inputStatus;
    @TableField("is_spc")
    private Boolean isSpc;
    private Boolean recheckItemFlag;
    private Integer sort;
    private Long tenantId;
}
