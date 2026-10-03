package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_press_slot_intermediate_detail")
@KeySequence("mes_sfc_press_slot_intermediate_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPressSlotIntermediateDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long intermediateRecordId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long pressSlotReportId;
    private Long sourceSlittingSliceId;
    private String samplePosition;
    private String samplePositionName;
    private String sliceBatchNo;
    private BigDecimal widthMm;
    private BigDecimal thickness1;
    private BigDecimal thickness2;
    private BigDecimal thickness3;
    private BigDecimal thickness4;
    private BigDecimal thickness5;
    private BigDecimal thickness6;
    private BigDecimal thickness7;
    private BigDecimal thickness8;
    private BigDecimal thickness9;
    private BigDecimal thickness10;
    private String remark;
    private Integer sortNo;
    private Long tenantId;
}
