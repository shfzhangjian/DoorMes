package cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule;

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

/**
 * 型号编码规则 DO
 */
@TableName("mes_md_model_rule")
@KeySequence("mes_md_model_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcModelRuleDO extends BaseDO {

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 规则分类 */
    private String ruleCategory;

    /** 适用产品层级 */
    private String targetLevel;

    /** 规则说明 */
    private String ruleDesc;

    /** 当前生效版本 */
    private String effectiveVersion;

    /** 状态 */
    private Integer status;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}