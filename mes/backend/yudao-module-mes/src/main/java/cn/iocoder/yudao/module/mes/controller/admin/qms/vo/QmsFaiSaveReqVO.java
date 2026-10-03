package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - FAI首件检验单新增/修改 Request VO")
@Data
public class QmsFaiSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "FAI首检单号，不传则系统生成")
    private String faiNo;

    @Schema(description = "生产工单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生产工单号不能为空")
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

    @Schema(description = "检验标准匹配方式：PROCESS=工序比对标准工段，MATERIAL=母料料号比对标准物料编码，MATERIAL_PROCESS=物料+工序，PRODUCT_MODEL_PROCESS=产品型号+工序")
    private String standardMatchMode;

    @Schema(description = "工序编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序编码不能为空")
    private String operationCode;

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序名称不能为空")
    private String operationName;

    @Schema(description = "机台ID")
    private Long machineId;

    @Schema(description = "机台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "机台编码不能为空")
    private String machineCode;

    @Schema(description = "机台名称")
    private String machineName;

    @Schema(description = "产品/物料ID")
    private Long materialId;

    @Schema(description = "产品/物料编码")
    private String materialCode;

    @Schema(description = "产品/物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号名称兼容字段；优先写入真实型号名称，缺失时使用型号编码")
    private String productModel;

    @Schema(description = "产品批次", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品批次不能为空")
    private String productBatchNo;

    @Schema(description = "胶板边库ID")
    private Long glueBoardStockId;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批次")
    private String gluePlateBatchNo;

    @Schema(description = "送检米数")
    private BigDecimal sampleLength;

    @Schema(description = "送检数量")
    private BigDecimal inspectionQty;

    @Schema(description = "工序类别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序类别不能为空")
    private String processCategory;

    @Schema(description = "送检类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "送检类型不能为空")
    private String submissionType;

    @Schema(description = "湿法送样类型")
    private String wetSampleType;

    @Schema(description = "触发原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "触发原因不能为空")
    private String triggerReason;

    @Schema(description = "采用的检验标准ID")
    private Long standardId;

    @Schema(description = "检验标准编号")
    private String standardNo;

    @Schema(description = "检验标准版本")
    private String standardVersion;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "送检时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime submissionTime;

    @Schema(description = "送检人ID")
    private Long submitterId;

    @Schema(description = "送检人员")
    private String submitterName;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime inspectionTime;

    @Schema(description = "留样状态；字典:mes_fai_retention_status")
    private String retentionStatus;

    @Schema(description = "留样确认时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime retentionConfirmTime;

    @Schema(description = "留样确认人ID")
    private Long retentionConfirmUserId;

    @Schema(description = "留样确认人名称")
    private String retentionConfirmUserName;

    @Schema(description = "备注")
    private String remark;

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

    @Schema(description = "录入布局：PROGRAM_FORM程序化录入/SHEET_GRID历史表格兼容")
    private String entryLayout;

    @Schema(description = "当前程序录入步骤编码")
    private String currentStepCode;

    @Schema(description = "程序录入完成率")
    private Integer entryProgress;

    @Schema(description = "最近一次导入批次")
    private String lastImportBatchNo;

    @Schema(description = "是否历史补录单")
    private Boolean historicalBackfill;

    @Schema(description = "表格是否锁定")
    private Boolean sheetLocked;

    @Schema(description = "最近退回原因")
    private String lastReturnReason;

    @Schema(description = "检验项明细；创建时由后端按FAI检验标准生成，提交时仅承载样本结果")
    private List<@Valid FaiItem> items;

    @Schema(description = "异常/驳回记录")
    private List<@Valid FaiAbnormal> abnormals;

    @Schema(description = "管理后台 - FAI检验项明细")
    @Data
    public static class FaiItem {

        @Schema(description = "主键ID")
        private Long id;

        @Schema(description = "来源标准项ID")
        private Long standardItemId;

        @Schema(description = "程序录入步骤编码")
        private String stepCode;

        @Schema(description = "程序录入步骤名称")
        private String stepName;

        @Schema(description = "程序指标编码")
        private String metricCode;

        @Schema(description = "指标组编码")
        private String metricGroupCode;

        @Schema(description = "程序录入控件类型")
        private String inputComponent;

        @Schema(description = "检验项目", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目不能为空")
        private String inspectionItem;

        @Schema(description = "项目类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "项目类型不能为空")
        private String itemType;

        @Schema(description = "是否允许上传附件快照")
        private Boolean attachmentEnabled;

        @Schema(description = "检验项附件URL数组")
        private List<String> attachmentUrls;

        @Schema(description = "标准要求")
        private String standardDesc;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "填写说明")
        private String ruleDescription;

        @Schema(description = "目标值")
        private BigDecimal targetValue;

        @Schema(description = "检验方法")
        private String inspectionMethod;

        @Schema(description = "测试频次判定")
        private String testFrequencyJudgement;

        @Schema(description = "录入值模板；字典:mes_fai_value_template")
        private String valueTemplate;

        @Schema(description = "录入值模板名称快照")
        private String valueTemplateName;

        @Schema(description = "判定指标；字典:mes_fai_judgment_metric")
        private String judgmentMetric;

        @Schema(description = "模板参数JSON")
        private String templateParams;

        @Schema(description = "平均值下限")
        private BigDecimal avgMinLimit;

        @Schema(description = "平均值上限")
        private BigDecimal avgMaxLimit;

        @Schema(description = "标准差下限")
        private BigDecimal stdMinLimit;

        @Schema(description = "标准差上限")
        private BigDecimal stdMaxLimit;

        @Schema(description = "检验工具/方法")
        private String testTool;

        @Schema(description = "检测数", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检测数不能为空")
        @Positive(message = "检测数必须大于 0")
        private Integer sampleSize;

        @Schema(description = "下限")
        private BigDecimal minValueLimit;

        @Schema(description = "上限")
        private BigDecimal maxValueLimit;

        @Schema(description = "自检最大值")
        private BigDecimal operatorMax;

        @Schema(description = "自检最小值")
        private BigDecimal operatorMin;

        @Schema(description = "自检平均值")
        private BigDecimal operatorAvg;

        @Schema(description = "自检判定")
        private String operatorResult;

        @Schema(description = "复核最大值")
        private BigDecimal qaMax;

        @Schema(description = "复核最小值")
        private BigDecimal qaMin;

        @Schema(description = "复核平均值")
        private BigDecimal qaAvg;

        @Schema(description = "复核判定")
        private String qaResult;

        @Schema(description = "按判定指标计算出的平均值")
        private BigDecimal calculatedAvg;

        @Schema(description = "按判定指标计算出的标准差")
        private BigDecimal calculatedStd;

        @Schema(description = "计算最小值")
        private BigDecimal calculatedMin;

        @Schema(description = "计算最大值")
        private BigDecimal calculatedMax;

        @Schema(description = "表格区块编码")
        private String sheetSectionCode;

        @Schema(description = "表格区块名称")
        private String sheetSectionName;

        @Schema(description = "表格指标编码")
        private String sheetMetricCode;

        @Schema(description = "表格指标名称")
        private String sheetMetricName;

        @Schema(description = "判定字段编码")
        private String sheetFieldCode;

        @Schema(description = "必填单元格数量")
        private Integer cellRequiredCount;

        @Schema(description = "已填单元格数量")
        private Integer cellCompletedCount;

        @Schema(description = "必填样本数")
        private Integer requiredSampleCount;

        @Schema(description = "已填样本数")
        private Integer completedSampleCount;

        @Schema(description = "异常样本数")
        private Integer abnormalSampleCount;

        @Schema(description = "程序录入状态")
        private String inputStatus;

        @Schema(description = "是否纳入SPC")
        private Boolean isSpc;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "样本实测")
        private List<@Valid FaiSample> samples;
    }

    @Schema(description = "管理后台 - FAI样本实测")
    @Data
    public static class FaiSample {

        @Schema(description = "主键ID")
        private Long id;

        @Schema(description = "对应单元格值ID")
        private Long sheetCellId;

        @Schema(description = "样本角色：OPERATOR/QA", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "样本角色不能为空")
        private String sampleRole;

        @Schema(description = "程序录入步骤编码")
        private String stepCode;

        @Schema(description = "程序指标编码")
        private String metricCode;

        @Schema(description = "指标组编码")
        private String metricGroupCode;

        @Schema(description = "来源程序录入控件")
        private String inputComponent;

        @Schema(description = "样本序号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "样本序号不能为空")
        private Integer sampleSeq;

        @Schema(description = "模穴/点位/样本位置")
        private String samplePosition;

        @Schema(description = "模板化原始录入值JSON")
        private String rawValuesJson;

        @Schema(description = "通用判定结果值")
        private BigDecimal resultValue;

        @Schema(description = "密度计算结果")
        private BigDecimal densityValue;

        @Schema(description = "压缩率计算结果")
        private BigDecimal compressionRate;

        @Schema(description = "压缩弹性率计算结果")
        private BigDecimal compressionElasticityRate;

        @Schema(description = "定量实测值")
        private BigDecimal measuredValue;

        @Schema(description = "定性值")
        private String qualitativeValue;

        @Schema(description = "样本判定")
        private String sampleResult;

        @Schema(description = "来源表格区块")
        private String sheetSectionCode;

        @Schema(description = "来源指标")
        private String sheetMetricCode;

        @Schema(description = "样本组号")
        private Integer sampleGroupNo;

        @Schema(description = "网格方向：X/Y")
        private String sampleAxis;

        @Schema(description = "网格列号")
        private Integer sampleColumnNo;

        @Schema(description = "导入批次号")
        private String importBatchNo;

        @Schema(description = "值来源")
        private String valueSource;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称快照")
        private String defectName;

        @Schema(description = "备注")
        private String remark;
    }

    @Schema(description = "管理后台 - FAI异常/驳回记录")
    @Data
    public static class FaiAbnormal {

        @Schema(description = "主键ID")
        private Long id;

        @Schema(description = "对应检验项ID")
        private Long faiItemId;

        @Schema(description = "对应样本ID")
        private Long sampleId;

        @Schema(description = "异常来源：OPERATOR/QA/SYSTEM")
        private String abnormalRole;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称快照")
        private String defectName;

        @Schema(description = "异常描述", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "异常描述不能为空")
        private String abnormalDesc;

        @Schema(description = "处理状态")
        private String processStatus;

        @Schema(description = "处置动作")
        private String actionRequired;

        @Schema(description = "关联NCR记录ID")
        private Long ncRecordId;
    }
}
