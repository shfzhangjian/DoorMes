package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 检验标准新增/修改 Request VO")
@Data
public class QmsQualityStandardSaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "标准编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标准编号不能为空")
    private String standardNo;

    @Schema(description = "标准名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标准名称不能为空")
    private String standardName;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "关联物料ID")
    private Long materialId;

    @Schema(description = "关联物料编码")
    private String materialCode;

    @Schema(description = "关联物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "生产类型编码")
    private String prodType;

    @Schema(description = "生产类型名称")
    private String prodTypeName;

    @Schema(description = "关联工序ID")
    private Long processId;

    @Schema(description = "关联工序编码")
    private String processCode;

    @Schema(description = "关联工序名称")
    private String processName;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "版本号不能为空")
    private String version;

    @Schema(description = "适用环节", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "适用环节不能为空")
    private String applyType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "检验项明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少维护一条检验项目明细")
    private List<@Valid StandardItem> items;

    @Schema(description = "管理后台 - 检验标准明细")
    @Data
    public static class StandardItem {

        @Schema(description = "主键ID", example = "100")
        private Long id;

        @Schema(description = "检验项目", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "检验项目不能为空")
        private String inspectionItem;

        @Schema(description = "项目类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "项目类型不能为空")
        private String itemType;

        @Schema(description = "时间类型允许距当前日期的最大天数；仅IQC DATE项使用")
        @Min(value = 0, message = "过期天数不能小于 0")
        private Integer expiryDays;

        @Schema(description = "是否允许检验记录按检验项上传附件")
        private Boolean attachmentEnabled;

        @Schema(description = "裁切成品检验数据录入时是否必填实际值")
        private Boolean actualValueRequired;

        @Schema(description = "目标值")
        private BigDecimal targetValue;

        @Schema(description = "下限值")
        private BigDecimal minValue;

        @Schema(description = "下限值显示小数位数")
        private Integer minValueScale;

        @Schema(description = "上限值")
        private BigDecimal maxValue;

        @Schema(description = "上限值显示小数位数")
        private Integer maxValueScale;

        @Schema(description = "标准要求", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "标准要求不能为空")
        private String standardDesc;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "检验方法")
        private String inspectionMethod;

        @Schema(description = "测试频次判定")
        private String testFrequencyJudgement;

        @Schema(description = "录入规则来源类型；字典:mes_qms_entry_rule_type")
        private String entryRuleType;

        @Schema(description = "录入规则模板ID")
        private Long entryRuleTemplateId;

        @Schema(description = "录入规则模板名称快照")
        private String entryRuleTemplateName;

        @Schema(description = "规则说明；给执行端展示的填写说明")
        @Size(max = 1000, message = "规则说明不能超过 1000 字")
        private String ruleDescription;

        @Schema(description = "录入值模板；字典:mes_fai_value_template")
        private String valueTemplate;

        @Schema(description = "判定指标；字典:mes_fai_judgment_metric")
        private String judgmentMetric;

        @Schema(description = "模板参数JSON")
        private String templateParams;

        @Schema(description = "平均值下限")
        private BigDecimal avgMinLimit;

        @Schema(description = "平均值下限显示小数位数")
        private Integer avgMinLimitScale;

        @Schema(description = "平均值上限")
        private BigDecimal avgMaxLimit;

        @Schema(description = "平均值上限显示小数位数")
        private Integer avgMaxLimitScale;

        @Schema(description = "标准差下限")
        private BigDecimal stdMinLimit;

        @Schema(description = "标准差下限显示小数位数")
        private Integer stdMinLimitScale;

        @Schema(description = "标准差上限")
        private BigDecimal stdMaxLimit;

        @Schema(description = "标准差上限显示小数位数")
        private Integer stdMaxLimitScale;

        @Schema(description = "FAI原始记录表模板ID；仅定量项使用")
        private Long sheetTemplateId;

        @Schema(description = "FAI表格区块编码；仅定量项使用")
        private String sheetSectionCode;

        @Schema(description = "FAI表格指标编码；仅定量项使用")
        private String sheetMetricCode;

        @Schema(description = "FAI表格判定字段编码；仅定量项使用")
        private String sheetFieldCode;

        @Schema(description = "检验项适用工序ID；为空时继承主表工序")
        private Long processId;

        @Schema(description = "检验项适用工序编码；为空时继承主表工序")
        private String processCode;

        @Schema(description = "检验项适用工序名称；为空时继承主表工序")
        private String processName;

        @Schema(description = "检验仪器")
        private String testTool;

        @Schema(description = "检测数", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检测数不能为空")
        @Positive(message = "检测数必须大于 0")
        private Integer sampleSize;

        @Schema(description = "是否SPC管控")
        @NotNull(message = "SPC管控标识不能为空")
        private Boolean isSpc;
    }
}
