package cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_lot_rule_counter")
@KeySequence("mes_lot_rule_counter_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcLotRuleCounterDO extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private String ruleCode;

    private String counterType;

    private String bizDimensionKey;

    private String bizDimensionJson;

    private String counterKey;

    private String resetKey;

    private Integer currentSeq;

    private String lastLotNo;

    private Long tenantId;
}
