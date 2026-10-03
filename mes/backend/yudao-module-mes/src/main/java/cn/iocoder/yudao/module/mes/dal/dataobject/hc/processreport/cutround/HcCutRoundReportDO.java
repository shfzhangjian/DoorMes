package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround;

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

@TableName("mes_sfc_cut_round_report")
@KeySequence("mes_sfc_cut_round_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcCutRoundReportDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourceAdhesive2ReportId;
    private Long sourceStockId;
    private Long sourcePlanLockId;
    private String sourceStockBatchNo;
    private BigDecimal sourceLockQty;
    private BigDecimal sourceConsumeQty;
    private String sourceConsumeTxnNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceConsumeTime;
    private Long sourcePressSlotReportId;
    private Long sourceSlittingSliceId;
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
    private BigDecimal outputLength;
    private Long outputStockId;
    private String outputStockPostStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime outputStockPostTime;
    private String outputStockPostMessage;
    private String bladeMaterialCode;
    private String bladeBatchNo;
    private String feltMaterialCode;
    private String feltBatchNo;
    private Integer bladeUseCount;
    private Integer feltUseCount;
    private String selfCheck;
    private String defectCode;
    /**
     * 过程风险标记：NONE/ADHESIVE2_NG/CUT_ROUND_NG/BOTH_NG。
     * 仅用于追溯和检验提示，不等同于 FQC 最终质量结论。
     */
    private String qualityRiskFlag;
    /**
     * 粘胶2与裁切自检风险的不可变快照 JSON。
     */
    private String qualityRiskSnapshotJson;
    private String reportStatus;
    private Long inspectionTaskId;
    private String inspectionTaskNo;
    private String inspectionStatus;
    private String inspectionResult;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;
    private String inspectionRemark;
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
