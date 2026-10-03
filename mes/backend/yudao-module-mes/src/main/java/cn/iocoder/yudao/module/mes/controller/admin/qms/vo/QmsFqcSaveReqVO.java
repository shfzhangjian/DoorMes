package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FQC成品检验新增/修改 Request VO")
@Data
public class QmsFqcSaveReqVO {

    private Long id;
    private String fqcNo;
    @NotBlank(message = "完工报检单号不能为空")
    private String reportNo;
    @NotBlank(message = "生产工单号不能为空")
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
    private String materialCode;
    private String materialName;
    private String specification;
    private String productModel;
    private String productBatchNo;
    @NotBlank(message = "成品批次不能为空")
    private String batchNo;
    private BigDecimal produceQty;
    private String unitCode;
    private String unitName;
    private String aqlStandard;
    private Integer sampleQty;
    private String inspectionCategory;
    private String submissionType;
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime submissionTime;
    private String submitterName;
    private Long standardId;
    private String status;
    private String judgment;
    private String releaseResult;
    private String relatedNcrNo;
    private String ncrStatus;
    private Long sheetTemplateId;
    private String entryMode;
    private String entryLayout;
    private String currentStepCode;
    private String returnReason;
    private String remark;
    private List<@Valid FqcItem> items;
    private List<@Valid FqcAbnormal> abnormals;

    @Data
    public static class FqcItem {
        private Long id;
        private Long standardItemId;
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
        private String category;
        @NotBlank(message = "检验项目不能为空")
        private String inspectionItem;
        @NotBlank(message = "项目类型不能为空")
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
        @NotNull(message = "取样数不能为空")
        @Positive(message = "取样数必须大于 0")
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private BigDecimal maxValue;
        private BigDecimal minValue;
        private BigDecimal averageValue;
        private String itemResult;
        private Boolean actualValueRequired;
        private String actualValue;
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
        private Integer sort;
        private List<@Valid FqcSample> samples;
    }

    @Data
    public static class FqcSample {
        private Long id;
        private Long sheetCellId;
        private Long submissionDetailId;
        private Long cutRoundInspectionDetailId;
        private Integer sliceSeqNo;
        private String productionBatchNo;
        private String parentProductionBatchNo;
        @NotNull(message = "样本序号不能为空")
        private Integer sampleSeq;
        private String samplePosition;
        private String stepCode;
        private String metricCode;
        private String metricGroupCode;
        private String inputComponent;
        private String sampleRole;
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
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        private LocalDateTime inputTime;
        private String defectCode;
        private String defectName;
        private List<@Valid FqcSampleDefect> defects;
        private String remark;
    }

    @Data
    public static class FqcSampleDefect {
        private Long id;
        private Long defectCodeId;
        private String defectCode;
        private String defectName;
        private String defectLevel;
        private Integer sort;
    }

    @Data
    public static class FqcAbnormal {
        private Long id;
        private Long fqcItemId;
        private Long sampleId;
        private String abnormalRole;
        private String defectCode;
        private String defectName;
        @NotBlank(message = "异常描述不能为空")
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
        private String relatedNcrNo;
        private String dispositionStatus;
    }
}
