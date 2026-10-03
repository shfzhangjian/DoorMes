package cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("mes_lot_rule_counter_adjust_log")
@KeySequence("mes_lot_rule_counter_adjust_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcLotRuleCounterAdjustLogDO extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long counterId;

    private Long ruleId;

    private String ruleCode;

    private String counterType;

    private String bizDimensionKey;

    private String counterKey;

    private String resetKey;

    private Integer beforeCurrentSeq;

    private Integer afterCurrentSeq;

    private String beforeLastLotNo;

    private String afterLastLotNo;

    private String adjustType;

    private String reason;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime adjustTime;

    private Long tenantId;
}
