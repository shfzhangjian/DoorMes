package cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner;

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

@TableName("mes_hc_visual_print_label_binding")
@KeySequence("mes_hc_visual_print_label_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcVisualPrintLabelBindingDO extends BaseDO {

    @TableId
    private Long id;

    private Long customerInfoId;
    private Long productItemId;
    private String labelKind;
    private Long designId;
    private String imageId;
    private String imageFile;
    private Integer sourceRow;
    private Integer status;
    private String importBatchNo;
    private String rawJson;
    private Long tenantId;

}
