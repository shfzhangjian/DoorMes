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

@TableName("mes_pp_plan_order")
@KeySequence("mes_pp_plan_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanOrderDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String planNo;
    private LocalDate planDate;
    private String planMode;
    private String planStatus;
    private String sourceType;
    private Long salesOrderId;
    private String salesOrderNo;
    private String salesOrderErpNo;
    private String salesOrderLineNo;
    private Long customerId;
    private String customerName;
    private BigDecimal orderDueQty;
    private Long orderDueUnitId;
    private String orderDueUnitCode;
    private String orderDueUnitName;
    private LocalDate salesOrderDeliveryDate;
    private LocalDate productionStartDate;
    private LocalDate productionEndDate;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long motherMaterialId;
    private String motherMaterialCode;
    private String motherMaterialName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String prodType;
    private String prodTypeName;
    private Long modelId;
    private String modelCode;
    private String modelName;
    private Long motherModelId;
    private String motherModelCode;
    private String motherModelName;
    private Long recipeId;
    private String recipeCode;
    private String recipeName;
    private Long bomId;
    private String bomVersion;
    private Long routeId;
    private String routeCode;
    private String routeName;
    private String routeVersion;
    private String sizeSpec;
    private String sizeName;
    private BigDecimal targetQty;
    private Long targetUnitId;
    private String targetUnitCode;
    private String targetUnitName;
    private String targetUom;
    private BigDecimal fgDeductQty;
    private BigDecimal netPlanQty;
    private Integer operationCount;
    private BigDecimal totalLockQty;
    private Boolean frontProcessFlag;
    private Boolean postProcessFlag;
    private String inventorySourceBatchNos;
    private LocalDateTime plannedAt;
    private LocalDateTime releasedAt;
    private Long statusOperatorId;
    private String statusOperatorName;
    private LocalDateTime statusOperateTime;
    private String statusRemark;
    private String remark;
    private String routeSnapshotJson;
    private Long batchRuleId;
    private String batchRuleCode;
    private Integer batchRuleVersion;
    private String batchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private Long productionBatchRuleId;
    private String productionBatchRuleCode;
    private Integer productionBatchRuleVersion;
    private String productionBatchContextJson;
    private String batchStatus;
    private LocalDateTime batchGeneratedTime;
    private String salesOrderSnapshotJson;

}
