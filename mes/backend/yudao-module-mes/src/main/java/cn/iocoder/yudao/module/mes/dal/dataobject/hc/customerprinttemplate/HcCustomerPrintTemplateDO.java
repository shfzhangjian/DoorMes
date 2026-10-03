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

@TableName("mes_hc_customer_print_template")
@KeySequence("mes_hc_customer_print_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcCustomerPrintTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String templateCode;
    private String templateName;
    private String customerCode;
    private String customerName;
    private String templateType;
    private String templateFormat;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String templateContent;
    private String variableJson;
    private Integer status;
    private String remark;
    private Long tenantId;

}
