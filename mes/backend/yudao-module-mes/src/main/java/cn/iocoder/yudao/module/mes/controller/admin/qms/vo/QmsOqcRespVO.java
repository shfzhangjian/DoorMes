package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - OQC出货检验 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsOqcRespVO {

    @ExcelProperty("主键ID")
    private Long id;
    @ExcelProperty("OQC出货检验单号")
    private String oqcNo;
    private String productType;
    private Long shippingNoticeId;
    private Long shippingNoticeItemId;
    @ExcelProperty("发货通知单")
    private String shippingNo;
    private String noticeNo;
    private Long customerId;
    private String customerCode;
    @ExcelProperty("客户名称")
    private String customerName;
    private Long materialId;
    @ExcelProperty("物料编码")
    private String materialCode;
    @ExcelProperty("物料名称")
    private String materialName;
    @ExcelProperty("规格型号")
    private String specification;
    private String modelCode;
    private String productSize;
    @ExcelProperty("出货批次")
    private String batchNo;
    private String customerBatchNo;
    @ExcelProperty("出货数量")
    private BigDecimal shippingQty;
    @ExcelProperty("出货片数")
    private BigDecimal shippingPieceQty;
    private String unitCode;
    private String unitName;
    private String aqlStandard;
    private Integer sampleQty;
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
    private String entryMode;
    private String entryLayout;
    private Integer entryProgress;
    private Integer requiredItemCount;
    private Integer completedItemCount;
    private Integer abnormalItemCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastSaveTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastCalculateTime;
    private Boolean sheetLocked;
    private String remark;
    private Boolean recheckFlag;
    private String originalInspectionNo;
    private String rejectRootInspectionNo;
    private String rejectPrevInspectionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    private List<OqcItem> items;
    private List<OqcAbnormal> abnormals;

    @Data
    public static class OqcItem {
        private Long id;
        private Long oqcId;
        private String oqcNo;
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
        private Integer requiredSampleCount;
        private Integer completedSampleCount;
        private Integer abnormalSampleCount;
        private String inputStatus;
        private Boolean isSpc;
        private Boolean recheckItemFlag;
        private Integer sort;
        private List<OqcSample> samples;
    }

    @Data
    public static class OqcSample {
        private Long id;
        private Long oqcId;
        private Long oqcItemId;
        private String oqcNo;
        private String sampleRole;
        private Integer sampleSeq;
        private String samplePosition;
        private String rawValuesJson;
        private BigDecimal resultValue;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        private String sampleResult;
        private Integer sampleGroupNo;
        private String valueSource;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inputTime;
        private String remark;
        private Boolean recheckItemFlag;
    }

    @Data
    public static class OqcAbnormal {
        private Long id;
        private Long oqcId;
        private String oqcNo;
        private Long oqcItemId;
        private Long sampleId;
        private String abnormalRole;
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
        private String relatedNcrNo;
        private String dispositionStatus;
    }
}
