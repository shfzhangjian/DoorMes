package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.fasterxml.jackson.annotation.JsonFormat;
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

@TableName("mes_sfc_press_slot_report")
@KeySequence("mes_sfc_press_slot_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPressSlotReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourceSlittingSliceId;
    private Long sourceStockId;
    private Long sourcePlanLockId;
    private String sourceStockBatchNo;
    private BigDecimal sourceLockQty;
    private BigDecimal sourceConsumeQty;
    private String sourceConsumeTxnNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceConsumeTime;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private LocalDate reportDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
    private BigDecimal inputLength;
    private BigDecimal startPosition;
    private BigDecimal endPosition;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private Long outputStockId;
    private String outputStockPostStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime outputStockPostTime;
    private String outputStockPostMessage;
    private BigDecimal napSampleLength;
    private String pressureRollerMaterialCode;
    private String pressureRollerBatchNo;
    private String bearingMaterialCode;
    private String bearingBatchNo;
    private Integer rollerCleanAccumulatedPcs;
    private Integer bearingReplaceAccumulatedPcs;
    private String selfCheck;
    private String defectCode;
    private String reportStatus;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String confirmerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
