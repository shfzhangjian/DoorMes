package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_metric_calc_node")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceMetricCalcNodeDO extends BaseDO {

    @TableId(type = IdType.AUTO)
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
    private Long tenantId;

}
