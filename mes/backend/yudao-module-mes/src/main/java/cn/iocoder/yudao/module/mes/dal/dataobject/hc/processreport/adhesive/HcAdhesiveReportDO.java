package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive;

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

@TableName("mes_sfc_adhesive_report")
@KeySequence("mes_sfc_adhesive_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesiveReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourceGrindingSecondDetailId;
    private Long sourceStockId;
    private Long sourcePlanLockId;
    private String sourceStockBatchNo;
    private BigDecimal sourceLockQty;
    private BigDecimal sourceConsumeQty;
    private String sourceConsumeTxnNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceConsumeTime;
    private Long sourceGrindingPlanId;
    private String sourceGrindingPlanNo;
    private Long sourceGrindingPlanOperationId;
    private String sourceType;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private LocalDate reportDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal inputLength;
    private BigDecimal startPosition;
    private BigDecimal endPosition;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal slittingRemainingLength;
    private Long outputStockId;
    private String outputStockPostStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime outputStockPostTime;
    private String outputStockPostMessage;
    private BigDecimal napSampleLength;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private Long glueBoardUsageId;
    private BigDecimal glueBoardStartPosition;
    private BigDecimal glueBoardUseLength;
    private Long aqcTaskId;
    private String aqcStatus;
    private Long faiId;
    private String faiNo;
    private String faiStatus;
    private String faiJudgment;
    private Long faiStandardId;
    private String faiStandardNo;
    private LocalDateTime faiApplyTime;
    private LocalDateTime faiReturnTime;
    private String faiRejectReason;
    private String productQualityStatus;
    private String qualityLockReason;
    private String selfCheck;
    private String defectCode;
    private String reportStatus;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String confirmerName;
    private LocalDateTime confirmerTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
