package cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

public final class QmsCoaVO {

    private QmsCoaVO() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TemplatePageReq extends PageParam {
        private String keyword;
        private String customerName;
        private String materialCode;
        private String productModelCode;
        private String auditStatus;
        private Integer status;
    }

    @Data
    public static class TemplateSaveReq {
        private Long id;
        private String templateCode;
        @NotBlank(message = "模板名称不能为空")
        private String templateName;
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String customerProductCode;
        private String customerProductName;
        @NotNull(message = "内部物料必须从产品料号选择")
        private Long materialId;
        @NotBlank(message = "内部物料编码不能为空")
        private String materialCode;
        private String materialName;
        @NotNull(message = "内部产品型号必须从选择组件选择")
        private Long productModelId;
        @NotBlank(message = "内部产品型号编码不能为空")
        private String productModelCode;
        private String productModelName;
        @NotBlank(message = "语言类型不能为空")
        @Pattern(regexp = "ZH_CN|EN_US|ZH_CN_EN_US", message = "语言类型无效")
        private String languageType;
        @NotBlank(message = "模板版本不能为空")
        private String versionNo;
        @NotBlank(message = "报告标题不能为空")
        private String reportTitle;
        private String footerStatement;
        private String remark;
        @Valid
        @NotEmpty(message = "COA模板项目不能为空")
        private List<TemplateItemReq> items;
    }

    @Data
    public static class TemplateItemReq {
        private Long id;
        @NotBlank(message = "特性编码不能为空")
        private String metricCode;
        private String itemGroup;
        @NotBlank(message = "中文项目名称不能为空")
        private String itemNameCn;
        private String itemNameEn;
        private Long sourceProcessId;
        private String sourceProcessCode;
        private String sourceProcessName;
        private Long sourceStandardId;
        private String sourceStandardNo;
        private String sourceStandardVersion;
        private Long sourceStandardItemId;
        private String sourceInspectionItem;
        private String sourceItemType;
        /** 过程检验、上传照片、人工填写。与 valueStrategy 的聚合规则分开保存。 */
        @NotBlank(message = "取值方式不能为空")
        @Pattern(regexp = "PROCESS_INSPECTION|PHOTO_UPLOAD|MANUAL_ENTRY", message = "取值方式无效")
        private String valueSourceType;
        /** FAI / FQC，人工和照片项目为空。 */
        private String sourceStandardApplyType;
        @NotBlank(message = "取值策略不能为空")
        @Pattern(regexp = "QA_AVG|QA_MIN|QA_MAX|QA_RESULT|LATEST_SAMPLE|VARIANCE|MANUAL|PHOTO|FIXED", message = "取值策略无效")
        private String valueStrategy;
        private String fixedValue;
        private String specSource;
        private String specText;
        /** 面向客户的 COA Spec，可在模板和草稿报告中分别维护快照。 */
        private String coaSpecText;
        private BigDecimal targetValue;
        private BigDecimal lowerLimit;
        private BigDecimal upperLimit;
        private String unit;
        private String inspectionMethod;
        @Min(value = 0, message = "展示精度不能小于0")
        @Max(value = 10, message = "展示精度不能大于10")
        private Integer decimalPlaces;
        private Boolean requiredFlag;
        private Boolean allowCorrectionFlag;
        private Boolean coaDisplayFlag;
        private Integer sortNo;
        private String remark;
    }

    @Data
    public static class TemplateAuditReq {
        @NotNull(message = "模板ID不能为空")
        private Long id;
        @NotBlank(message = "审核结果不能为空")
        @Pattern(regexp = "PASS|REJECT", flags = Pattern.Flag.CASE_INSENSITIVE, message = "审核结果无效")
        private String result;
        private String opinion;
    }

    @Data
    public static class TemplateStatusReq {
        @NotNull(message = "模板ID不能为空")
        private Long id;
        @NotNull(message = "启停状态不能为空")
        @Min(value = 0, message = "启停状态只能为0或1")
        @Max(value = 1, message = "启停状态只能为0或1")
        private Integer status;
    }

    @Data
    public static class TemplateResp {
        private Long id;
        private String templateCode;
        private String templateName;
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String customerProductCode;
        private String customerProductName;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long productModelId;
        private String productModelCode;
        private String productModelName;
        private String languageType;
        private String versionNo;
        private Integer status;
        private String auditStatus;
        private String reportTitle;
        private String footerStatement;
        private Long auditorId;
        private String auditorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime auditTime;
        private String auditOpinion;
        private String remark;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
        private List<TemplateItemResp> items;
    }

    @Data
    public static class TemplateItemResp extends TemplateItemReq {
        private Long templateId;
        private String templateCode;
        private String templateVersion;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReportPageReq extends PageParam {
        private String keyword;
        private String customerName;
        private String shippingNoticeNo;
        private String productionBatchNo;
        private String productModelCode;
        private String reportStatus;
        private String reviewQueue;
    }

    @Data
    public static class ReportGenerateReq {
        @NotNull(message = "请人工选择已审核启用的COA模板")
        private Long templateId;
        private Long shippingNoticeId;
        private String shippingNoticeNo;
        @NotBlank(message = "生产批次不能为空")
        private String productionBatchNo;
        private String customerBatchNo;
        private String productSize;
        private String shelfLife;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate issueDate;
        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MotherBatchPageReq extends PageParam {
        @NotNull(message = "COA模板不能为空")
        private Long templateId;
        private String keyword;
    }

    @Data
    public static class MotherBatchResp {
        private String productionBatchNo;
        private String materialCode;
        private String materialName;
        private String productModelCode;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate manufactureDate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime latestInspectionTime;
        private Integer inspectionCount;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReportItemValueCandidatePageReq extends PageParam {
        @NotNull(message = "COA报告不能为空")
        private Long reportId;
        @NotNull(message = "COA报告项目不能为空")
        private Long reportItemId;
    }

    @Data
    public static class ReportItemValueCandidateResp {
        /** FAI:{首检单ID}:{项目ID} / FQC:{成品检验单ID}:{项目ID}。 */
        private String candidateKey;
        private String sourceType;
        private Long sourceOrderId;
        private String sourceOrderNo;
        private Long sourceItemId;
        private String sourceBatchNo;
        private String processCode;
        private String processName;
        private String standardNo;
        private String standardVersion;
        private String inspectionItem;
        private String actualValue;
        private BigDecimal averageValue;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String result;
        private String rawValuesJson;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
    }

    @Data
    public static class ReportItemValueApplyReq {
        @NotNull(message = "COA报告不能为空")
        private Long reportId;
        @NotNull(message = "COA报告项目不能为空")
        private Long reportItemId;
        private List<String> candidateKeys;
        @Pattern(regexp = "QA_AVG|QA_MIN|QA_MAX|QA_RESULT|LATEST_SAMPLE|VARIANCE|MANUAL|PHOTO", message = "取值规则无效")
        private String valueStrategy;
        private String manualValue;
        private List<String> attachmentUrls;
        private String coaSpecText;
    }

    @Data
    public static class ReportCorrectionReq {
        @NotNull(message = "报告ID不能为空")
        private Long reportId;
        @NotNull(message = "报告项目ID不能为空")
        private Long reportItemId;
        @NotBlank(message = "修正值不能为空")
        private String correctedValue;
        @Pattern(regexp = "OK|NG", flags = Pattern.Flag.CASE_INSENSITIVE, message = "修正判定只能为OK或NG")
        private String correctedResult;
        @NotBlank(message = "修正原因不能为空")
        private String correctionReason;
        private String coaSpecText;
    }

    @Data
    public static class ReportAuditReq {
        @NotNull(message = "报告ID不能为空")
        private Long id;
        @NotBlank(message = "审核结果不能为空")
        @Pattern(regexp = "PASS|REJECT", flags = Pattern.Flag.CASE_INSENSITIVE, message = "审核结果无效")
        private String result;
        private String opinion;
    }

    @Data
    public static class ReportActionReq {
        @NotNull(message = "报告ID不能为空")
        private Long id;
        private String reason;
    }

    @Data
    public static class ReportResp {
        private Long id;
        private String coaNo;
        private Integer revisionNo;
        private Long previousReportId;
        private Long rootReportId;
        private Long templateId;
        private String templateCode;
        private String templateName;
        private String templateVersion;
        private String languageType;
        private String reportTitle;
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String customerProductCode;
        private String customerProductName;
        private Long shippingNoticeId;
        private String shippingNoticeNo;
        private String salesOrderNo;
        private String erpOrderNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingTime;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long productModelId;
        private String productModelCode;
        private String productModelName;
        private String externalProductCode;
        private String externalProductModel;
        private String productionBatchNo;
        private String customerBatchNo;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate manufactureDate;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expiryDate;
        private String productSize;
        private String shelfLife;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate issueDate;
        private BigDecimal shippingQuantity;
        private String shippingUnit;
        private Integer shippingItemCount;
        private String reportStatus;
        private String processInstanceId;
        private String overallResult;
        private Boolean dataCompleteFlag;
        private Integer sourceFaiCount;
        private Integer reportItemCount;
        private Integer requiredIncompleteCount;
        private Integer correctedItemCount;
        private String dataSnapshotHash;
        private String printSnapshotHash;
        private String pdfFileUrl;
        private String verificationCode;
        private String reviewerName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reviewTime;
        private String reviewOpinion;
        private Long inspectorId;
        private String inspectorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectorTime;
        private Long confirmerId;
        private String confirmerName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime confirmTime;
        private String confirmOpinion;
        private String issuedByName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime issuedTime;
        private String voidedByName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime voidedTime;
        private String voidReason;
        private String footerStatement;
        private String remark;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
        private List<ReportItemResp> items;
        private List<ReportSourceResp> sources;
        private List<ShippingRelResp> shippingRelations;
        private List<CorrectionLogResp> correctionLogs;
        private List<AuditLogResp> auditLogs;
    }

    @Data
    public static class ReportItemResp {
        private Long id;
        private Long reportId;
        private String metricCode;
        private String itemGroup;
        private String itemNameCn;
        private String itemNameEn;
        private String itemType;
        private String valueSourceType;
        private String sourceStandardApplyType;
        private String valueStrategy;
        private String specText;
        private String coaSpecText;
        private BigDecimal targetValue;
        private BigDecimal lowerLimit;
        private BigDecimal upperLimit;
        private String rawValueJson;
        private String sourceValue;
        private String actualValue;
        private String displayValue;
        private String unit;
        private Integer decimalPlaces;
        private String inspectionMethod;
        private String result;
        private String attachmentUrls;
        private String sourceType;
        private Long sourceOrderId;
        private String sourceOrderNo;
        private Long sourceOrderItemId;
        private Boolean requiredFlag;
        private Boolean sourceDataCompleteFlag;
        private Boolean allowCorrectionFlag;
        private Boolean correctedFlag;
        private Integer correctionCount;
        private String lastCorrectionReason;
        private String sourceProcessCode;
        private String sourceProcessName;
        private Long sourceFaiId;
        private String sourceFaiNo;
        private Long sourceFaiItemId;
        private String sourceStandardNo;
        private String sourceStandardVersion;
        private Long sourceStandardItemId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime sourceQaTime;
        private Integer sortNo;
    }

    @Data
    public static class ReportSourceResp {
        private Long id;
        private String sourceType;
        private Long sourceId;
        private String sourceNo;
        private String sourceBatchNo;
        private String processCode;
        private String processName;
        private String materialCode;
        private String materialName;
        private String productModelCode;
        private String standardNo;
        private String standardVersion;
        private String sourceStatus;
        private String sourceResult;
        private String releaseResult;
        private String qaInspectorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime qaTime;
        private Boolean historicalBackfill;
        private Boolean recheckFlag;
        private Integer recheckRoundNo;
        private Boolean selectedFlag;
        private String effectiveReason;
    }

    @Data
    public static class ShippingRelResp {
        private Long id;
        private String shippingNoticeNo;
        private Long shippingNoticeItemId;
        private String stockNo;
        private String outerBoxNo;
        private String sliceBatchNo;
        private String actualSliceBatchNo;
        private String productionBatchNo;
        private String customerBatchNo;
        private BigDecimal shippingQuantity;
        private String qualityStatus;
        private String oqcStatus;
        private String shippingInspectionResult;
    }

    @Data
    public static class CorrectionLogResp {
        private Long id;
        private Long reportItemId;
        private String metricCode;
        private String itemName;
        private String sourceValue;
        private String beforeActualValue;
        private String afterActualValue;
        private String beforeDisplayValue;
        private String afterDisplayValue;
        private String beforeResult;
        private String afterResult;
        private String correctionReason;
        private String correctorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime correctionTime;
        private String sourceFaiNo;
    }

    @Data
    public static class AuditLogResp {
        private Long id;
        private String actionType;
        private String beforeStatus;
        private String afterStatus;
        private String operatorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime actionTime;
        private String opinion;
        private String reason;
    }

    @Data
    public static class FaiStandardResp {
        private Long id;
        private String standardNo;
        private String standardName;
        private String version;
        private String materialCode;
        private String materialName;
        private String productModelCode;
        private String productModelName;
        private String processCode;
        private String processName;
        private List<FaiStandardItemResp> items;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class StandardItemPageReq extends PageParam {
        @NotBlank(message = "产品型号不能为空")
        private String productModelCode;
        @Pattern(regexp = "FAI|FQC", flags = Pattern.Flag.CASE_INSENSITIVE, message = "标准类别无效")
        private String standardApplyType;
        private String processCode;
        private String keyword;
    }

    @Data
    public static class StandardItemResp {
        private Long id;
        private Long standardId;
        private String standardNo;
        private String standardName;
        private String standardVersion;
        private String standardApplyType;
        private Long processId;
        private String processCode;
        private String processName;
        private String productModelCode;
        private String productModelName;
        private Integer sort;
        private String inspectionItem;
        private String itemType;
        private BigDecimal targetValue;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String standardDesc;
        private String unit;
        private String inspectionMethod;
        private String sheetMetricCode;
        private String ruleDescription;
    }

    @Data
    public static class FaiStandardItemResp {
        private Long id;
        private Long standardId;
        private Integer sort;
        private String inspectionItem;
        private String itemType;
        private BigDecimal targetValue;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String standardDesc;
        private String unit;
        private String inspectionMethod;
        private String sheetMetricCode;
        private Integer sampleSize;
    }
}
