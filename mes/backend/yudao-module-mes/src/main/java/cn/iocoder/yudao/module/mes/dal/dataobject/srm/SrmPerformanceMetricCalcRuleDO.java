package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_metric_calc_rule")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceMetricCalcRuleDO extends BaseDO {

    @TableId(type = IdType.AUTO)
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
    private Long tenantId;

}
