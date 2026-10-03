package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_equipment_consumable_rule")
@KeySequence("mes_sfc_equipment_consumable_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcEquipmentConsumableRuleDO extends BaseDO {

    @TableId
    private Long id;

    private String processCode;
    private String processName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String consumableType;
    private String ruleName;
    private Integer limitCount;
    private BigDecimal limitLength;
    private String warningMode;
    private Integer status;
    private String remark;
    private Long tenantId;
}
