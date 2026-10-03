package cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_md_guide_cloth_record")
@KeySequence("mes_md_guide_cloth_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGuideClothRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String lineName;

    private String lineCode;

    /**
     * 绑定的湿法设备ID；由湿法报工自动写入，研发消耗据此累计寿命
     */
    private Long equipmentId;

    private LocalDateTime replaceTime;

    private String replacePlanNo;

    private String petBatchNo;

    private String guideClothBatchNo;

    private String petModel;

    private String replaceReason;

    private Integer useCount;

    private Integer currentFlag;

    private String remark;

    private Long tenantId;
}
