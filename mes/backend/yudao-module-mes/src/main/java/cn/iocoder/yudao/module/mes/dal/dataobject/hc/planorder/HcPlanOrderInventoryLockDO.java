package cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
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

@TableName("mes_pp_plan_inv_lock")
@KeySequence("mes_pp_plan_inv_lock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanOrderInventoryLockDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long planId;
    private String targetPlanNo;
    private Long planOperationId;
    private String targetOpCode;
    private String targetOpName;
    private String lockType;
    private Long stockId;
    private Long ngPieceId;
    private String stockType;
    private String sourceType;
    private String sourceTable;
    private Long sourceId;
    private Long sourceReportId;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourcePlanOperationId;
    private String sourceBatchNo;
    private Integer opSeq;
    private String opCode;
    private String opName;
    private String segmentCode;
    private String segmentName;
    private BigDecimal thickness;
    private String lotNo;
    private String batchNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String modelNo;
    private String recipeCode;
    private String sizeSpec;
    private java.time.LocalDate productionDate;
    private java.time.LocalDate expiryDate;
    private Integer remainMonths;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private Long ownerId;
    private String ownerCode;
    private String ownerName;
    private BigDecimal availableQty;
    private BigDecimal lockQty;
    private BigDecimal consumedQty;
    private BigDecimal releasedQty;
    private BigDecimal remainingQty;
    private Long consumeReportId;
    private LocalDateTime consumeTime;
    private LocalDateTime releaseTime;
    private String releaseReason;
    private String lockTxnNo;
    private String consumeTxnNo;
    private String releaseTxnNo;
    private Long unitId;
    private String unitCode;
    private String unitName;
    private String uom;
    private Integer fifoRank;
    private String lockStatus;
    private String remark;

}
