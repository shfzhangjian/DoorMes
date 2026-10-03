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

@TableName("mes_hc_print_field_template_item")
@KeySequence("mes_hc_print_field_template_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPrintFieldTemplateItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateId;
    private String fieldKey;
    private String fieldLabel;
    private String valueKey;
    private Integer sort;
    private Boolean visible;
    private String defaultValue;
    private String formatType;
    private String suffix;
    private String remark;
    private Long tenantId;

}
