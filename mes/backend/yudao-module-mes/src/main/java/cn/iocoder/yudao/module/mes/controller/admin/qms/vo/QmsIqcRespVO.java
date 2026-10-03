package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IQC进料检验单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsIqcRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "IQC检验单号")
    @ExcelProperty("IQC检验单号")
    private String iqcNo;

    @Schema(description = "收料单ID")
    private Long receiptId;

    @Schema(description = "收料单号")
    @ExcelProperty("收料单号")
    private String receiptNo;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商编码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "收件人")
    @ExcelProperty("收件人")
    private String receiverName;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    @ExcelProperty("规格型号")
    private String specification;

    @Schema(description = "型号")
    @ExcelProperty("型号")
    private String modelNo;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    @ExcelProperty("产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    @ExcelProperty("产品型号名称")
    private String productModelName;

    @Schema(description = "批次号")
    @ExcelProperty("批次号")
    private String batchNo;

    @Schema(description = "到货数量")
    @ExcelProperty("到货数量")
    private BigDecimal receiveQty;

    @Schema(description = "来料日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("来料日期")
    private LocalDate arrivalDate;

    @Schema(description = "报检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("报检时间")
    private LocalDateTime inspectionApplyTime;

    @Schema(description = "送检附件URL数组")
    private List<String> inspectionApplyAttachmentUrls;

    @Schema(description = "生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("生产日期")
    private LocalDate productionDate;

    @Schema(description = "失效日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("失效日期")
    private LocalDate expiryDate;

    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "采用的检验标准ID")
    private Long standardId;

    @Schema(description = "检验标准编号")
    @ExcelProperty("检验标准编号")
    private String standardNo;

    @Schema(description = "检验标准名称")
    @ExcelProperty("检验标准名称")
    private String standardName;

    @Schema(description = "检验标准版本")
    @ExcelProperty("检验标准版本")
    private String standardVersion;

    @Schema(description = "标准快照生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime standardSnapshotTime;

    @Schema(description = "标准匹配模式")
    private String standardMatchMode;

    @Schema(description = "标准快照是否锁定")
    private Boolean standardSnapshotLocked;

    @Schema(description = "标准执行内容快照哈希")
    private String standardSnapshotHash;

    @Schema(description = "当前已审核标准内容是否已变化")
    private Boolean standardContentChanged;

    @Schema(description = "当前状态是否允许重新选择标准")
    private Boolean standardSwitchAllowed;

    @Schema(description = "标准变化或选择提示")
    private String standardSelectionMessage;

    @Schema(description = "抽样方案")
    @ExcelProperty("抽样方案")
    private String aqlStandard;

    @Schema(description = "单据状态")
    @ExcelProperty("单据状态")
    private String status;

    @Schema(description = "判定结果")
    @ExcelProperty("判定结果")
    private String judgment;

    @Schema(description = "留样状态")
    private String retentionStatus;

    @Schema(description = "留样确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime retentionConfirmTime;

    @Schema(description = "留样确认人ID")
    private Long retentionConfirmUserId;

    @Schema(description = "留样确认人")
    private String retentionConfirmUserName;

    @Schema(description = "留样物料分类ID")
    private Long retentionMaterialCategoryId;

    @Schema(description = "留样物料分类名称")
    @ExcelProperty("留样物料种类")
    private String retentionMaterialCategoryName;

    @Schema(description = "留样数量")
    @ExcelProperty("留样数量")
    private BigDecimal retentionQty;

    @Schema(description = "留样单位")
    @ExcelProperty("留样单位")
    private String retentionUnit;

    @Schema(description = "留样规则描述")
    @ExcelProperty("留样规则")
    private String retentionRuleDesc;

    @Schema(description = "留样周期数值")
    private Integer retentionPeriodValue;

    @Schema(description = "留样周期单位")
    private String retentionPeriodUnit;

    @Schema(description = "留样过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime retentionExpireTime;

    @Schema(description = "留样销毁状态")
    private String retentionDestroyStatus;

    @Schema(description = "留样销毁时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime retentionDestroyTime;

    @Schema(description = "留样销毁人ID")
    private Long retentionDestroyUserId;

    @Schema(description = "留样销毁人")
    private String retentionDestroyUserName;

    @Schema(description = "留样销毁备注")
    private String retentionDestroyRemark;

    @Schema(description = "检验员ID")
    private Long inspectorId;

    @Schema(description = "检验员名称")
    @ExcelProperty("检验员")
    private String inspectorName;

    @Schema(description = "检验完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("检验完成时间")
    private LocalDateTime inspectionTime;

    @Schema(description = "确认人ID")
    private Long qaInspectorId;

    @Schema(description = "确认人名称")
    @ExcelProperty("确认人")
    private String qaInspectorName;

    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("确认时间")
    private LocalDateTime qaTime;

    @Schema(description = "审核待办通知时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;

    @Schema(description = "处置方式")
    private String disposalType;

    @Schema(description = "采购合同号")
    @ExcelProperty("采购合同号")
    private String purchaseContractNo;

    @Schema(description = "退回次数")
    private Integer returnCount;

    @Schema(description = "最近退回原因")
    private String lastReturnReason;

    @Schema(description = "是否已驳回")
    private Boolean rejectFlag;

    @Schema(description = "是否复检单")
    private Boolean recheckFlag;

    @Schema(description = "复检链路ID")
    private Long recheckGroupId;

    @Schema(description = "复检轮次")
    private Integer recheckRoundNo;

    @Schema(description = "上一轮检验单ID")
    private Long rejectPrevInspectionId;

    @Schema(description = "上一轮检验单号")
    private String rejectPrevInspectionNo;

    @Schema(description = "驳回新检验单ID")
    private Long rejectNextInspectionId;

    @Schema(description = "驳回新检验单号")
    private String rejectNextInspectionNo;

    @Schema(description = "根检验单ID")
    private Long rejectRootInspectionId;

    @Schema(description = "根检验单号")
    private String rejectRootInspectionNo;

    @Schema(description = "复检结果")
    private String rejectRecheckResult;

    @Schema(description = "复检结果时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime rejectRecheckTime;

    @Schema(description = "驳回说明")
    private String rejectReason;

    @Schema(description = "驳回时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime rejectTime;

    @Schema(description = "驳回人ID")
    private Long rejectUserId;

    @Schema(description = "驳回人")
    private String rejectUserName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "检验项明细")
    private List<IqcItem> items;

    @Schema(description = "异常处置明细")
    private List<IqcAbnormal> abnormals;

    @Schema(description = "退回留痕记录")
    private List<IqcReturnRecord> returnRecords;

    @Schema(description = "管理后台 - IQC检验项明细 Response VO")
    @Data
    public static class IqcItem {
        private Long id;
        private Long iqcId;
        private String iqcNo;
        private Long standardItemId;
        private String inspectionItem;
        private String itemType;
        private Integer expiryDays;
        private Boolean attachmentEnabled;
        private List<String> attachmentUrls;
        private BigDecimal targetValue;
        private String standardDesc;
        private String unit;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String entryRuleType;
        private String ruleDescription;
        private String valueTemplate;
        private String valueTemplateName;
        private String templateParams;
        private String testTool;
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;

        /** 平均值内控快照；与单次测量限值独立判定。 */
        private BigDecimal avgMinLimit;
        private BigDecimal avgMaxLimit;
        private BigDecimal maxValue;
        private BigDecimal minValue;
        private BigDecimal averageValue;
        private String itemResult;

        private String judgmentReason;
        private Boolean isSpc;
        private Boolean recheckItemFlag;
        private Integer sort;
        private List<IqcSample> samples;
    }

    @Schema(description = "管理后台 - IQC样本实测 Response VO")
    @Data
    public static class IqcSample {
        private Long id;
        private Long iqcId;
        private Long iqcItemId;
        private String iqcNo;
        private Integer sampleSeq;
        private String sampleBarcode;
        private String rawValuesJson;
        private BigDecimal resultValue;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate dateValue;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate evaluationDate;
        private String sampleResult;
        private String defectCode;
        private String defectName;
        private String remark;
        private Boolean recheckItemFlag;
    }

    @Schema(description = "管理后台 - IQC异常处置 Response VO")
    @Data
    public static class IqcAbnormal {
        private Long id;
        private Long iqcId;
        private String iqcNo;
        private Long iqcItemId;
        private Long sampleId;
        private String sampleBarcode;
        private String defectCode;
        private String defectName;
        private String abnormalDesc;
        private String suggestedFlow;
        private String processStatus;
        private Long ncRecordId;
    }

    @Schema(description = "管理后台 - IQC退回留痕 Response VO")
    @Data
    public static class IqcReturnRecord {
        private Long id;
        private Long iqcId;
        private String iqcNo;
        private String returnReason;
        private Long returnUserId;
        private String returnUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime returnTime;
        private String beforeStatus;
        private String afterStatus;
    }
}
