package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2;

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

@TableName("mes_sfc_adhesive2_report")
@KeySequence("mes_sfc_adhesive2_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesive2ReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourcePressSlotReportId;
    private Long sourceStockId;
    private Long sourcePlanLockId;
    private String sourceStockBatchNo;
    private BigDecimal sourceLockQty;
    private BigDecimal sourceConsumeQty;
    private String sourceConsumeTxnNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceConsumeTime;
    private Long sourceSlittingSliceId;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String productionBatchNo;
    /** 实际尺寸：775mm / 740mm。 */
    private String actualSizeRule;
    /** 与实际尺寸对应的片号尾号：A / B。 */
    private String actualSizeSuffix;
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
    private String glueBoardModel;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private Long glueBoardUsageId;
    private BigDecimal glueBoardStartPosition;
    private BigDecimal glueBoardUseLength;
    private String productQualityStatus;
    private String qualityLockReason;
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
