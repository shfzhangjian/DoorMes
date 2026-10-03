package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IQC进料检验单新增/修改 Request VO")
@Data
public class QmsIqcSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "IQC检验单号，不传则系统生成")
    private String iqcNo;

    @Schema(description = "收料单ID")
    private Long receiptId;

    @Schema(description = "收料单号；进料送检独立建单未填写时由系统按IQC单号兜底")
    private String receiptNo;

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "供应商编码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "收件人")
    private String receiverName;

    @Schema(description = "物料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "物料不能为空")
    private Long materialId;

    @Schema(description = "物料编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "物料编码不能为空")
    private String materialCode;

    @Schema(description = "物料名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "物料名称不能为空")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "型号")
    private String modelNo;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "来料批次号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    @Schema(description = "到货数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "到货数量不能为空")
    @Positive(message = "到货数量必须大于 0")
    private BigDecimal receiveQty;

    @Schema(description = "来料日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate arrivalDate;

    @Schema(description = "报检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime inspectionApplyTime;

    @Schema(description = "送检附件URL数组，最多10个")
    @Size(max = 10, message = "送检附件最多上传10个")
    private List<@Size(max = 2048, message = "送检附件地址不能超过2048个字符") String>
            inspectionApplyAttachmentUrls;

    @Schema(description = "生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionDate;

    @Schema(description = "失效日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate expiryDate;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "采用的检验标准ID")
    private Long standardId;

    @Schema(description = "检验标准编号")
    private String standardNo;

    @Schema(description = "检验标准名称")
    private String standardName;

    @Schema(description = "检验标准版本")
    private String standardVersion;

    @Schema(description = "标准匹配模式")
    private String standardMatchMode;

    @Schema(description = "抽样方案")
    private String aqlStandard;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "留样状态")
    private String retentionStatus;

    @Schema(description = "处置方式")
    private String disposalType;

    @Schema(description = "采购合同号")
    private String purchaseContractNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "检验项明细；创建时由后端标准快照生成，更新/提交时仅用于回传样本")
    private List<@Valid IqcItem> items;

    @Schema(description = "异常处置明细")
    private List<@Valid IqcAbnormal> abnormals;

    @Schema(description = "管理后台 - IQC检验项明细")
    @Data
    public static class IqcItem {

        @Schema(description = "主键ID")
        private Long id;

        @Schema(description = "来源标准项ID")
        private Long standardItemId;

        @Schema(description = "检验项目", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目不能为空")
        private String inspectionItem;

        @Schema(description = "项目类型", requiredMode = Schema.RequiredMode.REQUIRED)
        private String itemType;

        @Schema(description = "时间类型过期天数快照")
        private Integer expiryDays;

        @Schema(description = "是否允许上传附件快照")
        private Boolean attachmentEnabled;

        @Schema(description = "检验项附件URL数组")
        private List<String> attachmentUrls;

        @Schema(description = "目标值")
        private BigDecimal targetValue;

        @Schema(description = "标准要求")
        private String standardDesc;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "检验方法")
        private String inspectionMethod;

        @Schema(description = "检测频次/判定规则")
        private String testFrequencyJudgement;

        @Schema(description = "录入规则来源类型")
        private String entryRuleType;

        @Schema(description = "录入规则说明")
        private String ruleDescription;

        @Schema(description = "录入值模板")
        private String valueTemplate;

        @Schema(description = "录入值模板名称")
        private String valueTemplateName;

        @Schema(description = "模板参数")
        private String templateParams;

        @Schema(description = "检验工具/方法")
        private String testTool;

        @Schema(description = "应抽样数")
        private Integer sampleSize;

        @Schema(description = "下限")
        private BigDecimal minValueLimit;

        @Schema(description = "上限")
        private BigDecimal maxValueLimit;

        /** 平均值内控快照；与单次测量限值独立判定。 */
        private BigDecimal avgMinLimit;
        private BigDecimal avgMaxLimit;

        @Schema(description = "实测最大值")
        private BigDecimal maxValue;

        @Schema(description = "实测最小值")
        private BigDecimal minValue;

        @Schema(description = "实测平均值")
        private BigDecimal averageValue;

        @Schema(description = "单项结果")
        private String itemResult;

        private String judgmentReason;

        @Schema(description = "是否纳入SPC")
        private Boolean isSpc;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "样本实测")
        private List<@Valid IqcSample> samples;
    }

    @Schema(description = "管理后台 - IQC样本实测")
    @Data
    public static class IqcSample {

        @Schema(description = "主键ID")
        private Long id;

        @Schema(description = "样本序号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样本序号不能为空")
        private Integer sampleSeq;

        @Schema(description = "样本或单件条码")
        private String sampleBarcode;

        @Schema(description = "模板化原始录入值JSON")
        private String rawValuesJson;

        @Schema(description = "规则计算后的判定结果值")
        private BigDecimal resultValue;

        @Schema(description = "定量实测值")
        private BigDecimal measuredValue;

        @Schema(description = "定性值")
        private String qualitativeValue;

        @Schema(description = "时间类型录入日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate dateValue;

        @Schema(description = "后端判定基准日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate evaluationDate;

        @Schema(description = "样本判定")
        private String sampleResult;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称快照")
        private String defectName;

        @Schema(description = "备注")
        private String remark;
    }

    @Schema(description = "管理后台 - IQC异常处置")
    @Data
    public static class IqcAbnormal {

        @Schema(description = "主键ID")
        private Long id;

        @Schema(description = "对应检验项ID")
        private Long iqcItemId;

        @Schema(description = "对应样本ID")
        private Long sampleId;

        @Schema(description = "异常样本条码")
        private String sampleBarcode;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称快照")
        private String defectName;

        @Schema(description = "不合格描述", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "不合格描述不能为空")
        private String abnormalDesc;

        @Schema(description = "建议流向", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "建议流向不能为空")
        private String suggestedFlow;

        @Schema(description = "处理状态")
        private String processStatus;

        @Schema(description = "关联NCR记录ID")
        private Long ncRecordId;
    }
}
