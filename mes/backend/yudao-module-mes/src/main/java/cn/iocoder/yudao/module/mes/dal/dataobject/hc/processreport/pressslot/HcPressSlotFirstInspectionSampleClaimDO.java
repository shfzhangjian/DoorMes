package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot;

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

/**
 * 压槽首检样片占用记录。
 *
 * <p>一条来源分切片只能占用一次，作为压槽首检样片送检；复检必须改用新的来源片。</p>
 */
@TableName("mes_sfc_press_slot_fai_sample_claim")
@KeySequence("mes_sfc_press_slot_fai_sample_claim_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPressSlotFirstInspectionSampleClaimDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;

    private Long planOperationId;

    private Long sourceSlittingSliceId;

    private String sampleBatchNo;

    private Long firstFaiId;

    private Long tenantId;
}
