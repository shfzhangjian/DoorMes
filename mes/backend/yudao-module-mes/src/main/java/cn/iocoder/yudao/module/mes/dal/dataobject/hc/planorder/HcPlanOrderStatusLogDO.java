package cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pp_plan_order_status_log")
@KeySequence("mes_pp_plan_order_status_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanOrderStatusLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long planId;
    private String planNo;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private String reasonRemark;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;

}
