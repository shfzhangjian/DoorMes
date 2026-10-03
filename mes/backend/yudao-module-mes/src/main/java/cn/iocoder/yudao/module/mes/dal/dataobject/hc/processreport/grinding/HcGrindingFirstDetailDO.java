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

@TableName("mes_sfc_grinding_first_detail")
@KeySequence("mes_sfc_grinding_first_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingFirstDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long grindingReportId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String rowUid;
    private String sourceType;
    private String motherBatchNo;
    private String sourcePlanNo;
    private BigDecimal remainStartMeter;
    private BigDecimal remainLength;
    private BigDecimal processLength;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    private String grindingPass;
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
    private String remark;
    private Long tenantId;
    private LocalDate reportDate;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private Long sourcePlanId;
    private Long sourcePlanOperationId;
    private String sourceProductionBatchNo;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;
    private Long checkRecordId;
    private Long sandpaperStateId;
    private Long guideClothStateId;
    private String detailStatus;
}
