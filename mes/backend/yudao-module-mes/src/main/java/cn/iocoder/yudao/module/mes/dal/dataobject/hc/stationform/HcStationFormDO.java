package cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform;

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

@TableName("mes_md_station_form")
@KeySequence("mes_md_station_form_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcStationFormDO extends BaseDO {

    @TableId
    private Long id;

    private String formCode;
    private String formName;
    private String processCode;
    private String processName;
    private String triggerTimingCode;
    private String triggerTimingName;
    private Boolean needConfirm;
    private Integer sortNo;
    private Integer status;
    private String schemaJson;
    private String presetHeaderDataJson;
    private String presetItemsJson;
    private String remark;
    private Long tenantId;
}
