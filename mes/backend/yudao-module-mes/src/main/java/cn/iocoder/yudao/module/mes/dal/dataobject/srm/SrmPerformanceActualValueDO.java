package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_actual_value")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceActualValueDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private String metricCode;
    private String metricName;
    private String valueType;
    private BigDecimal numericValue;
    private String textValue;
    private String unit;
    private String sourceNodeKey;
    private Boolean evidenceRequired;
    private String valueStatus;
    private String remark;
    private Long tenantId;

}
