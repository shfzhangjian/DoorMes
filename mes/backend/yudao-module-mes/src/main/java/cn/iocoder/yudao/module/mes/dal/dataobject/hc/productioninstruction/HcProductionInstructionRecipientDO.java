package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction;

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

@TableName("mes_pp_production_instruction_recipient")
@KeySequence("mes_pp_production_instruction_recipient_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProductionInstructionRecipientDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long instructionId;
    private String instructionNo;
    private Long recipientId;
    private String recipientName;
    private String recipientType;
    private String notifyStatus;
    private LocalDateTime notifyTime;
    private String remark;

}
