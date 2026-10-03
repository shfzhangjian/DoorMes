package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_calc_trace")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceCalcTraceDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long evaluationItemId;
    private Long calcRuleId;
    private String nodeKey;
    private String parentNodeKey;
    private String nodeType;
    private String displayName;
    private String formulaExpr;
    private Long sourceReportId;
    private Long sourceValueId;
    private String sourceMetricCode;
    private String sourceMetricName;
    private Integer periodYear;
    private Integer periodQuarter;
    private Integer periodMonth;
    private BigDecimal rawValue;
    private BigDecimal normalizedValue;
    private String unit;
    private Integer attachmentCount;
    private Long reporterUserId;
    private String reporterUserName;
    private LocalDateTime reportTime;
    private String resultFlag;
    private String resultMessage;
    private Long tenantId;

}
