package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_hc_process_form_record_item")
@KeySequence("mes_hc_process_form_record_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessFormRecordItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Long templateItemId;
    private Integer itemSeq;
    private String fieldKey;
    private String fieldLabel;
    private String itemCategory;
    private String stepNode;
    private String standardText;
    private String unit;
    private String valueMode;
    private String controlType;
    private String actualValue;
    @TableField("actual_value_2")
    private String actualValue2;
    private BigDecimal actualNumber;
    private LocalDateTime actualTime;
    private String resultFlag;
    private String abnormalRemark;
    private String sourceRowJson;
    private Long tenantId;
}
