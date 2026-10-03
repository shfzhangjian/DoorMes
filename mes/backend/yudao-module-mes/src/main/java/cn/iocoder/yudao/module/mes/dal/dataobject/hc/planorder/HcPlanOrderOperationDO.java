package cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pp_plan_operation")
@KeySequence("mes_pp_plan_operation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanOrderOperationDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long planId;
    private Integer opSeq;
    private String opCode;
    private String opName;
    private Long routeOperationId;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long motherMaterialId;
    private String motherMaterialCode;
    private String motherMaterialName;
    private Long motherModelId;
    private String motherModelCode;
    private String motherModelName;
    private BigDecimal yieldRate;
    private BigDecimal requiredQty;
    private BigDecimal lockedQty;
    private BigDecimal dispatchQty;
    private Long unitId;
    private String unitCode;
    private String unitName;
    private String uom;
    private String instructionText;
    private Boolean hasLock;
    private String operationStatus;
    private LocalDateTime finishTime;
    private String finishRemark;
    private String splitMark;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourcePlanOperationId;
    private String sourceOperationCode;
    private String sourceOperationName;
    private String pauseScope;
    private LocalDate pauseStartDate;
    private LocalDate pauseEndDate;
    private String pauseRemark;
    private String cancelReason;
    private Long statusOperatorId;
    private String statusOperatorName;
    private LocalDateTime statusOperateTime;
    private String statusDateMarksJson;
    private String batchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private Long productionBatchRuleId;
    private String productionBatchRuleCode;
    private Integer productionBatchRuleVersion;
    private String productionBatchContextJson;
    private Integer productionBatchCount;
    private String parentBatchNo;
    private String sampleCode;
    private Integer sort;

}
