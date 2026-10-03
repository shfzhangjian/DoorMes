package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - FAI首件检验单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsFaiRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "FAI首检单号")
    @ExcelProperty("FAI首检单号")
    private String faiNo;

    @Schema(description = "生产工单号")
    @ExcelProperty("生产工单号")
    private String workOrderNo;

    @Schema(description = "来源湿法报工记录ID")
    private Long sourceReportId;

    @Schema(description = "来源湿法报工任务展示编号或报工单号")
    private String sourceReportNo;

    @Schema(description = "来源模块；如 WET_REPORT")
    private String sourceModule;

    @Schema(description = "来源工段/工序编码快照")
    private String sourceOperationCode;

    @Schema(description = "来源工段/工序名称快照")
    private String sourceOperationName;

    @Schema(description = "生产计划/工序任务ID")
    private Long planOrderId;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    @ExcelProperty("工序")
    private String operationName;

    @Schema(description = "机台ID")
    private Long machineId;

    @Schema(description = "机台编码")
    @ExcelProperty("机台编码")
    private String machineCode;

    @Schema(description = "机台名称")
    private String machineName;

    @Schema(description = "产品/物料ID")
    private Long materialId;

    @Schema(description = "产品/物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "产品/物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    @ExcelProperty("规格型号")
    private String specification;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "母料料号兼容字段；当前等同物料编码")
    @ExcelProperty("母料料号(物料编码)")
    private String productModel;

    @Schema(description = "产品批次")
    @ExcelProperty("产品批次")
    private String productBatchNo;

    @Schema(description = "胶板边库ID")
    private Long glueBoardStockId;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "胶板料号")
    @ExcelProperty("胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批次")
    @ExcelProperty("胶板批号")
    private String gluePlateBatchNo;

    @Schema(description = "送检米数")
    private BigDecimal sampleLength;

    @Schema(description = "送检数量")
    @ExcelProperty("送检数量")
    private BigDecimal inspectionQty;

    @Schema(description = "工序类别")
    private String processCategory;

    @Schema(description = "送检类型")
    private String submissionType;

    @Schema(description = "湿法送样类型")
    private String wetSampleType;

    @Schema(description = "触发原因")
    @ExcelProperty("触发原因")
    private String triggerReason;

    @Schema(description = "检验标准匹配方式")
    private String standardMatchMode;

    @Schema(description = "标准匹配类型：EXACT_MODEL/FAMILY_MODEL/MATERIAL/PROCESS/OVERRIDE")
    private String standardMatchType;

    @Schema(description = "实际匹配的型号或系列ID")
    private Long matchedModelId;

    @Schema(description = "实际匹配的型号或系列编码")
    private String matchedModelCode;

    @Schema(description = "标准匹配原因")
    private String standardMatchReason;

    @Schema(description = "采用的检验标准ID")
    private Long standardId;

    @Schema(description = "检验标准编号")
    private String standardNo;

    @Schema(description = "检验标准版本")
    private String standardVersion;

    @Schema(description = "标准执行内容快照哈希")
    private String standardSnapshotHash;

    @Schema(description = "当前已审核标准内容是否已变化")
    private Boolean standardContentChanged;

    @Schema(description = "当前状态是否允许重新选择标准")
    private Boolean standardSwitchAllowed;

    @Schema(description = "标准变化或选择提示")
    private String standardSelectionMessage;

    @Schema(description = "单据状态")
    @ExcelProperty("单据状态")
    private String status;

    @Schema(description = "判定结果")
    @ExcelProperty("判定结果")
    private String judgment;

    @Schema(description = "机长/操作员ID")
    private Long operatorId;

    @Schema(description = "机长/操作员名称")
    @ExcelProperty("机长")
    private String operatorName;

    @Schema(description = "自检完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime operatorTime;

    @Schema(description = "品质检验员ID")
    private Long qaInspectorId;

    @Schema(description = "品质检验员名称")
    @ExcelProperty("品质检验员")
    private String qaInspectorName;

    @Schema(description = "品质复核完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("品质复核时间")
    private LocalDateTime qaTime;

    @Schema(description = "送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submissionTime;

    @Schema(description = "送检人ID")
    private Long submitterId;

    @Schema(description = "送检人员")
    private String submitterName;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("检验时间")
    private LocalDateTime inspectionTime;

    @Schema(description = "留样状态；字典:mes_fai_retention_status")
    @ExcelProperty("留样状态")
    private String retentionStatus;

    @Schema(description = "留样确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("留样确认时间")
    private LocalDateTime retentionConfirmTime;

    @Schema(description = "留样确认人ID")
    private Long retentionConfirmUserId;

    @Schema(description = "留样确认人名称")
    @ExcelProperty("留样确认人")
    private String retentionConfirmUserName;

    @Schema(description = "留样时长数值")
    @ExcelProperty("留样时长")
    private Integer retentionPeriodValue;

    @Schema(description = "留样时长单位；DAY=天，MONTH=月")
    @ExcelProperty("留样单位")
    private String retentionPeriodUnit;

    @Schema(description = "留样过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("留样过期时间")
    private LocalDateTime retentionExpireTime;

    @Schema(description = "留样销毁状态；字典:mes_fai_retention_destroy_status")
    @ExcelProperty("销毁状态")
    private String retentionDestroyStatus;

    @Schema(description = "留样销毁时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("销毁时间")
    private LocalDateTime retentionDestroyTime;

    @Schema(description = "留样销毁操作人ID")
    private Long retentionDestroyUserId;

    @Schema(description = "留样销毁操作人")
    @ExcelProperty("销毁人")
    private String retentionDestroyUserName;

    @Schema(description = "留样销毁备注")
    @ExcelProperty("销毁备注")
    private String retentionDestroyRemark;

    @Schema(description = "放行回写结果")
    private String releaseResult;

    @Schema(description = "放行或锁定回写时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;

    @Schema(description = "审核待办通知时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最终审核说明")
    private String auditRemark;

    @Schema(description = "通用检验单摘要ID")
    private Long summaryInspectionId;

    @Schema(description = "FAI原始记录表模板ID")
    private Long sheetTemplateId;

    @Schema(description = "FAI原始记录表模板编码")
    private String sheetTemplateCode;

    @Schema(description = "FAI原始记录表模板名称")
    private String sheetTemplateName;

    @Schema(description = "FAI原始记录表模板版本")
    private String sheetTemplateVersion;

    @Schema(description = "录入方式")
    private String entryMode;

    @Schema(description = "录入布局")
    private String entryLayout;

    @Schema(description = "当前程序录入步骤编码")
    private String currentStepCode;

    @Schema(description = "程序录入完成率")
    private Integer entryProgress;

    @Schema(description = "必检指标总数")
    private Integer requiredItemCount;

    @Schema(description = "已完成指标数")
    private Integer completedItemCount;

    @Schema(description = "异常指标数")
    private Integer abnormalItemCount;

    @Schema(description = "最近草稿保存时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastSaveTime;

    @Schema(description = "最近服务端重算时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastCalculateTime;

    @Schema(description = "最近一次导入批次")
    private String lastImportBatchNo;

    @Schema(description = "是否历史补录单")
    private Boolean historicalBackfill;

    @Schema(description = "表格是否锁定")
    private Boolean sheetLocked;

    @Schema(description = "退回次数")
    private Integer returnCount;

    @Schema(description = "最近退回原因")
    private String lastReturnReason;

    @Schema(description = "最近一次扫码内容")
    private String lastScanCode;

    @Schema(description = "最近一次扫码对象类型")
    private String lastScanTargetType;

    @Schema(description = "最近一次扫码场景")
    private String lastScanScene;

    @Schema(description = "最近一次扫码时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastScanTime;

    @Schema(description = "最近一次扫码人ID")
    private Long lastScanUserId;

    @Schema(description = "最近一次扫码人名称")
    private String lastScanUserName;

    @Schema(description = "是否复检单")
    private Boolean recheckFlag;

    @Schema(description = "原检验单号")
    private String originalInspectionNo;

    private String rejectRootInspectionNo;

    private String rejectPrevInspectionNo;

    @Schema(description = "最近复检申请ID")
    private Long latestRecheckApplyId;

    @Schema(description = "最近复检申请单号")
    private String latestRecheckApplyNo;

    @Schema(description = "最近复检申请状态")
    private String latestRecheckApplyStatus;

    @Schema(description = "最近复检申请原因")
    private String latestRecheckApplyReason;

    @Schema(description = "最近复检申请审核意见")
    private String latestRecheckAuditOpinion;

    @Schema(description = "最近复检申请生成的复检单号")
    private String latestRecheckGeneratedFaiNo;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "检验项明细")
    private List<FaiItem> items;

    @Schema(description = "异常/驳回记录")
    private List<FaiAbnormal> abnormals;

    @Schema(description = "管理后台 - FAI检验项明细 Response VO")
    @Data
    public static class FaiItem {
        private Long id;
        private Long faiId;
        private String faiNo;
        private Long standardItemId;
        private String stepCode;
        private String stepName;
        private String metricCode;
        private String metricGroupCode;
        private String inputComponent;
        private String inspectionItem;
        private String itemType;
        private Boolean attachmentEnabled;
        private List<String> attachmentUrls;
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
        private BigDecimal operatorMax;
        private BigDecimal operatorMin;
        private BigDecimal operatorAvg;
        private String operatorResult;
        private Long operatorId;
        private String operatorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime operatorTime;
        private BigDecimal qaMax;
        private BigDecimal qaMin;
        private BigDecimal qaAvg;
        private String qaResult;
        private Long qaInspectorId;
        private String qaInspectorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime qaTime;
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
        private List<FaiSample> samples;
        private List<FaiGroupAudit> auditGroups;
    }

    @Schema(description = "管理后台 - FAI样本组审核确认 Response VO")
    @Data
    public static class FaiGroupAudit {
        private Long id;
        private Long faiId;
        private Long faiItemId;
        private String faiNo;
        private String inspectionItem;
        private String groupKey;
        private String samplePosition;
        private String auditResult;
        private String auditRemark;
        private Long auditUserId;
        private String auditUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime auditTime;
        private String itemRecheckStatus;
        private Boolean recheckItemFlag;
    }

    @Schema(description = "管理后台 - FAI样本实测 Response VO")
    @Data
    public static class FaiSample {
        private Long id;
        private Long sheetCellId;
        private Long faiId;
        private Long faiItemId;
        private String faiNo;
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
        private String defectCode;
        private String defectName;
        private String remark;
        private Boolean recheckItemFlag;
    }

    @Schema(description = "管理后台 - FAI异常/驳回记录 Response VO")
    @Data
    public static class FaiAbnormal {
        private Long id;
        private Long faiId;
        private String faiNo;
        private Long faiItemId;
        private Long sampleId;
        private String abnormalRole;
        private String defectCode;
        private String defectName;
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
    }
}
