package cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate;

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

@TableName("mes_hc_print_field_template")
@KeySequence("mes_hc_print_field_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPrintFieldTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String templateCode;
    private String templateName;
    private String processCode;
    private String processName;
    private String documentType;
    private String documentName;
    private String usageScene;
    private Integer status;
    private String remark;
    private Long tenantId;

}
