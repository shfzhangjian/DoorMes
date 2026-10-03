package cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_station_record_item")
@KeySequence("mes_sfc_station_record_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcStationRecordItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Integer itemSeq;
    private String itemCategory;
    private String stepNode;
    private String itemName;
    private String standardText;
    private String valueMode;
    @TableField("dual_label_1")
    private String dualLabel1;
    @TableField("dual_label_2")
    private String dualLabel2;

    /** 多字段定义快照，按稳定 key 对应实际值。 */
    private String fieldDefinitionsJson;
    private String fieldValuesJson;
    private String actualValue;
    @TableField("actual_value_2")
    private String actualValue2;
    private String resultFlag;
    private String abnormalRemark;
    private Long tenantId;
}
