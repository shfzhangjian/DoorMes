package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionfactadjust;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产事实调账单。
 *
 * <p>首期只支持换型后的产品快照回修，计划主表及库存流水不在本单据的回写范围内。</p>
 */
@TableName("mes_pp_production_fact_adjust_order")
@KeySequence("mes_pp_production_fact_adjust_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcProductionFactAdjustOrderDO extends BaseDO {

    @TableId
    private Long id;
    private String adjustNo;
    private String adjustType;
    private String status;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String segmentBatchNo;
    private Long instructionId;
    private String instructionNo;
    private Long sourceProductModelId;
    private String sourceModelCode;
    private Long sourceMaterialId;
    private String sourceMaterialCode;
    private String sourceMaterialName;
    private Long targetProductModelId;
    private String targetModelCode;
    private Long targetMaterialId;
    private String targetMaterialCode;
    private String targetMaterialName;
    private String targetSpecification;
    private String adjustReason;
    private String evidenceRemark;
    private String impactSummaryJson;
    private String beforeSnapshotJson;
    private String afterSnapshotJson;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime appliedTime;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedTime;
    private String approveRemark;
    private Long executorId;
    private String executorName;
    private LocalDateTime executedTime;
    private String executionRemark;
    private Long tenantId;
}
