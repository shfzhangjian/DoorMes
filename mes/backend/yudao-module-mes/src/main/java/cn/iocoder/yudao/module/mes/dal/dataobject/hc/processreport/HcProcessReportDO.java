package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_operation_report")
@KeySequence("mes_sfc_operation_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationStatus;
    private Integer operationSeq;
    private String operationCode;
    private String operationName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long motherMaterialId;
    private String motherMaterialCode;
    private String motherMaterialName;
    private Long motherModelId;
    private String motherModelCode;
    private String motherModelName;
    private String batchingNo;
    private String feedBatchNo;
    private Long recipeId;
    private String recipeCode;
    private String recipeName;
    private String batchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private Long productionBatchRuleId;
    private String productionBatchRuleCode;
    private String productionBatchContextJson;
    private String parentBatchNo;
    private String sampleCode;
    private LocalDate reportDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal goodQty;
    private Long outputStockId;
    private String outputStockPostStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime outputStockPostTime;
    private String outputStockPostMessage;
    private BigDecimal scrapQty;
    private BigDecimal feedQty;
    private LocalDateTime stirStartTime;
    private LocalDateTime stirEndTime;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String confirmerName;
    private LocalDateTime confirmerTime;
    private Long mixerEquipmentId;
    private String mixerEquipmentCode;
    private String mixerEquipmentName;
    private Long foamingEquipmentId;
    private String foamingEquipmentCode;
    private String foamingEquipmentName;
    private BigDecimal viscosity;
    private BigDecimal slurryTemperature;
    private String filterBatchNo;
    private BigDecimal inputWeight;
    private String batchingTankNo;
    private String defoamingTankNo;
    private String reportUom;
    private Integer reportMinutes;
    private BigDecimal laborHours;
    private String reportType;
    private String sourceMenuCode;
    private Long faiId;
    private String faiNo;
    private String faiStatus;
    private String faiJudgment;
    private Long faiStandardId;
    private String faiStandardNo;
    private LocalDateTime faiApplyTime;
    private LocalDateTime faiReturnTime;
    private String faiRejectReason;
    private String extraJson;
    private String remark;
}
