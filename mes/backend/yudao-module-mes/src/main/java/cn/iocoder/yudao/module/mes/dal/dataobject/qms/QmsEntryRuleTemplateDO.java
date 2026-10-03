package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_entry_rule_template")
@KeySequence("mes_qms_entry_rule_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsEntryRuleTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String templateName;

    private String itemType;

    private String testFrequencyJudgement;

    private String templateParams;

    private String ruleDescription;

    private Integer sampleSize;

    private Integer status;

    private Long tenantId;
}
