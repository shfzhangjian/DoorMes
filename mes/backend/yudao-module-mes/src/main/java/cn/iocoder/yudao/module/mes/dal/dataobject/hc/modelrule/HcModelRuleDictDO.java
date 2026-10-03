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
@TableName("mes_md_model_rule_dict")
@KeySequence("mes_md_model_rule_dict_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcModelRuleDictDO extends BaseDO {

    /** 规则ID */
    private Long ruleId;

    /** 规则字段ID */
    private Long ruleItemId;

    /** 字段编码 */
    private String itemCode;

    /** 编码值 */
    private String dictCode;

    /** 编码含义 */
    private String dictValue;

    /** 扩展属性JSON */
    private String extAttrJson;

    /** 排序 */
    private Integer sort;

    /** 是否启用 */
    private Boolean enabled;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}