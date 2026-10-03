package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

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

@TableName("mes_sfc_grinding_report")
@KeySequence("mes_sfc_grinding_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
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
    private String batchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private Long productionBatchRuleId;
    private String productionBatchRuleCode;
    private String productionBatchContextJson;
    private String parentBatchNo;
    private LocalDate reportDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reportType;
    private String reportStatus;
    private BigDecimal inputLength;
    private BigDecimal reportQty;
    private BigDecimal firstProcessLength;
    private BigDecimal firstLossLength;
    private BigDecimal firstOutputLength;
    private BigDecimal firstNapSampleLength;
    private BigDecimal secondProcessLength;
    private BigDecimal secondLossLength;
    private BigDecimal secondOutputLength;
    private BigDecimal secondNapSampleLength;
    private String lastFirstSandpaperBatchNo;
    private BigDecimal lastFirstSandpaperLife;
    private BigDecimal lastFirstSandpaperLifeDays;
    private String lastSecondSandpaperBatchNo;
    private BigDecimal lastSecondSandpaperLife;
    private BigDecimal lastSecondSandpaperLifeDays;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String confirmerName;
    private LocalDateTime confirmerTime;
    private Long operationReportId;
    private String uiExtraJson;
    private String remark;
    private Long tenantId;
    private Integer currentFlag;
    private String summarySource;
    private Integer summaryVersion;
}
