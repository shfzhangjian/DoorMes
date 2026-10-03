package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_quarter_log")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceQuarterLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorName;
    private String actionDescription;
    private String detailJson;
    private Long tenantId;

}
