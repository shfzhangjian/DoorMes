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

@TableName("mes_sfc_grinding_second_detail")
@KeySequence("mes_sfc_grinding_second_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingSecondDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long grindingReportId;
    private Long firstDetailId;
    /** FIRST_ALLOCATED 模式下对应的一磨前置分配记录ID。 */
    private Long firstAllocationId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String rowUid;
    private String sourceRowUid;
    private String motherBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private Long productionBatchRuleId;
    private String productionBatchRuleCode;
    private String productionBatchContextJson;
    private BigDecimal processLength;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    /** 研发消耗米数，独立于固定损耗、NAP留样和异常米数。 */
    private BigDecimal researchConsumptionLength;
    private BigDecimal startPosition;
    private String segmentMark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal sandpaperLife;
    private BigDecimal sandpaperLifeDays;
    private String sandpaperBatchNo;
    private String currentSandpaperBatchNo;
    private String currentGuideClothBatchNo;
    private String pressure;
    private String lineSpeed;
    private String rotationSpeed;
    private String meterCounter;
    private String grindingThickness;
    private String afterGrindingThickness;
    private String qualityThickness;
    private String qualityWidth;
    private BigDecimal grindingMeters;
    private String selfCheck;
    private String defectCode;
    private String rowStatus;
    private String confirmStatus;
    private LocalDateTime confirmTime;
    private String confirmedBatchNo;
    private Long confirmOperatorId;
    private String confirmOperatorName;
    private String downstreamStatus;
    private Long downstreamPlanId;
    private Long downstreamPlanOperationId;
    private Long downstreamReportId;
    private String remark;
    private Long tenantId;
    private LocalDate reportDate;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String sourceProductionBatchNo;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;
    private Long checkRecordId;
    private Long sandpaperStateId;
    private Long guideClothStateId;
    private String printStatus;
    private Integer printCount;
    private LocalDateTime lastPrintTime;
    private Long inspectionId;
    private String inspectionNo;
    private String inspectionStatus;
    private String inspectionResult;
    private LocalDateTime inspectionApplyTime;
    private LocalDateTime inspectionReturnTime;
    private String inspectionRejectReason;
    private String detailStatus;
    private Long stockId;
    private String stockPostStatus;
    private LocalDateTime stockPostTime;
    private Long stockPostOperatorId;
    private String stockPostOperatorName;
    private String stockPostMessage;
}
