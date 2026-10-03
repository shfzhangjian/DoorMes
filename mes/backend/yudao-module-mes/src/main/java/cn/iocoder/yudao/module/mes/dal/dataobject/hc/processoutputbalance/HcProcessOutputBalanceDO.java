package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processoutputbalance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工序产出物结存台账只读视图 DO.
 */
@TableName("mes_v_sfc_process_output_balance")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProcessOutputBalanceDO extends BaseDO {

    @TableId(type = IdType.INPUT)
    private String ledgerId;

    private String stageCode;
    private String stageName;
    private Integer stageSort;
    private String sourceTable;
    private Long sourceId;
    private Long sourceReportId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String outputBatchNo;
    private String parentBatchNo;
    private String sourceBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private BigDecimal outputQty;
    private BigDecimal consumedQty;
    private BigDecimal remainingQty;
    private String uom;
    private String balanceStatus;
    private String reportStatus;
    private LocalDateTime reportTime;
    private String consumeSummary;
    @TableField(exist = false)
    private Long sourcePlanLockId;
    @TableField(exist = false)
    private String sourceLockStatus;
    @TableField(exist = false)
    private BigDecimal sourceLockedQty;
    @TableField(exist = false)
    private BigDecimal sourceLockRemainingQty;
    @TableField(exist = false)
    private String sourceLockTargetPlanNo;
    private Long tenantId;

}
