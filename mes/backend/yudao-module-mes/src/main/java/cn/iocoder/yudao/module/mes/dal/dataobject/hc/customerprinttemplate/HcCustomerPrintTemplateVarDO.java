package cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate;

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

@TableName("mes_hc_customer_print_template_var")
@KeySequence("mes_hc_customer_print_template_var_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcCustomerPrintTemplateVarDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateId;
    private String variableName;
    private String variableExpr;
    private String variableScope;
    private String sourceField;
    private String sourceLabel;
    private Integer sort;
    private Boolean required;
    private String defaultValue;
    private String remark;
    private Long tenantId;

}
