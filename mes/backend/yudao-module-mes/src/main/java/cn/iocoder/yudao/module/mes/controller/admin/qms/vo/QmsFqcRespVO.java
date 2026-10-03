package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FQC成品检验 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsFqcRespVO {

    @ExcelProperty("主键ID")
    private Long id;
    @ExcelProperty("FQC成品检验单号")
    private String fqcNo;
    @ExcelProperty("完工报检单")
    private String reportNo;
    @ExcelProperty("生产工单")
    private String workOrderNo;
    private Long sourceReportId;
    private String sourceReportNo;
    private String sourceModule;
    private String sourceOperationCode;
    private String sourceOperationName;
    private Long planOrderId;
    private String operationCode;
    private String operationName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private Long materialId;
    @ExcelProperty("物料编码")
    private String materialCode;
    @ExcelProperty("物料名称")
    private String materialName;
    @ExcelProperty("规格型号")
    private String specification;
    private String productModel;
    private String productBatchNo;
    @ExcelProperty("成品批次")
    private String batchNo;
    @ExcelProperty("报检数量")
    private BigDecimal produceQty;
    private String unitCode;
    private String unitName;
    private String aqlStandard;
    private Integer sampleQty;
    private String inspectionCategory;
    private String submissionType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submissionTime;
    private String submitterName;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("判定")
    private String judgment;
    private Long inspectorId;
    @ExcelProperty("检验员")
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("检验时间")
    private LocalDateTime inspectionTime;
    private Long qaInspectorId;
    private String qaInspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime qaTime;
    private String releaseResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
    @Schema(description = "审核待办通知时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;
    private String relatedNcrNo;
    private String ncrStatus;
    private String remark;
    private Long sheetTemplateId;
    private String sheetTemplateCode;
    private String sheetTemplateName;
    private String sheetTemplateVersion;
    private String entryMode;
    private String entryLayout;
    private String currentStepCode;
    private Integer entryProgress;
    private Integer requiredItemCount;
    private Integer completedItemCount;
    private Integer abnormalItemCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastSaveTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastCalculateTime;
    private String lastImportBatchNo;
    private Boolean historicalBackfill;
    private Boolean sheetLocked;
    private Integer returnCount;
    private String lastReturnReason;
    private String lastScanCode;
    private String lastScanTargetType;
    private String lastScanScene;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastScanTime;
    private Long lastScanUserId;
    private String lastScanUserName;
    private Boolean recheckFlag;
    private String originalInspectionNo;
    private String rejectRootInspectionNo;
    private String rejectPrevInspectionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    private List<FqcItem> items;
    private List<FqcAbnormal> abnormals;

    @Data
    public static class FqcItem {
        private Long id;
        private Long fqcId;
        private String fqcNo;
        private Long submissionDetailId;
        private Long cutRoundInspectionDetailId;
        private Integer sliceSeqNo;
        private String productionBatchNo;
        private String parentProductionBatchNo;
        private String stepCode;
        private String stepName;
        private String metricCode;
        private String metricGroupCode;
        private String inputComponent;
        private Long standardItemId;
        private String category;
        private String inspectionItem;
        private String itemType;
        private BigDecimal targetValue;
        private String standardDesc;
        private String unit;
        private String ruleDescription;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String valueTemplate;
        private String valueTemplateName;
        private String judgmentMetric;
        private String templateParams;
        private BigDecimal avgMinLimit;
        private BigDecimal avgMaxLimit;
        private BigDecimal stdMinLimit;
        private BigDecimal stdMaxLimit;
        private String testTool;
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private BigDecimal maxValue;
        private BigDecimal minValue;
        private BigDecimal averageValue;
        private String itemResult;
        private Boolean actualValueRequired;
        private String actualValue;
        private Long operatorId;
        private String operatorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime operatorTime;
        private BigDecimal operatorMax;
        private BigDecimal operatorMin;
        private BigDecimal operatorAvg;
        private String operatorResult;
        private BigDecimal qaMax;
        private BigDecimal qaMin;
        private BigDecimal qaAvg;
        private String qaResult;
        private BigDecimal calculatedAvg;
        private BigDecimal calculatedStd;
        private BigDecimal calculatedMin;
        private BigDecimal calculatedMax;
        private String sheetSectionCode;
        private String sheetSectionName;
        private String sheetMetricCode;
        private String sheetMetricName;
        private String sheetFieldCode;
        private Integer cellRequiredCount;
        private Integer cellCompletedCount;
        private Integer requiredSampleCount;
        private Integer completedSampleCount;
        private Integer abnormalSampleCount;
        private String inputStatus;
        private Boolean isSpc;
        private Boolean recheckItemFlag;
        private Integer sort;
        private List<FqcSample> samples;
    }

    @Data
    public static class FqcSample {
        private Long id;
        private Long sheetCellId;
        private Long fqcId;
        private Long fqcItemId;
        private String fqcNo;
        private Long submissionDetailId;
        private Long cutRoundInspectionDetailId;
        private Integer sliceSeqNo;
        private String productionBatchNo;
        private String parentProductionBatchNo;
        private String stepCode;
        private String metricCode;
        private String metricGroupCode;
        private String inputComponent;
        private String sampleRole;
        private Integer sampleSeq;
        private String samplePosition;
        private String rawValuesJson;
        private BigDecimal resultValue;
        private BigDecimal densityValue;
        private BigDecimal compressionRate;
        private BigDecimal compressionElasticityRate;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        private String sampleResult;
        private String sheetSectionCode;
        private String sheetMetricCode;
        private Integer sampleGroupNo;
        private String sampleAxis;
        private Integer sampleColumnNo;
        private String importBatchNo;
        private String valueSource;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inputTime;
        private String defectCode;
        private String defectName;
        private List<FqcSampleDefect> defects;
        private String remark;
        private Boolean recheckItemFlag;
    }

    @Data
    public static class FqcSampleDefect {
        private Long id;
        private Long fqcId;
        private String fqcNo;
        private Long fqcItemId;
        private Long sampleId;
        private Integer sampleSeq;
        private String samplePosition;
        private Long defectCodeId;
        private String defectCode;
        private String defectName;
        private String defectLevel;
        private Integer sort;
    }

    @Data
    public static class FqcAbnormal {
        private Long id;
        private Long fqcId;
        private String fqcNo;
        private Long fqcItemId;
        private Long sampleId;
        private String abnormalRole;
        private String defectCode;
        private String defectName;
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
        private String relatedNcrNo;
        private String dispositionStatus;
    }
}
