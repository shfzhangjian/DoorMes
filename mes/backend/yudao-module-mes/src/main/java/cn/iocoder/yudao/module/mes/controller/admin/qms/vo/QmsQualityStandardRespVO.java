package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 检验标准 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsQualityStandardRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "标准编号")
    @ExcelProperty("标准编号")
    private String standardNo;

    @Schema(description = "标准名称")
    @ExcelProperty("标准名称")
    private String standardName;

    @Schema(description = "胶板型号")
    @ExcelProperty("胶板型号")
    private String glueBoardModel;

    @Schema(description = "关联物料ID")
    private Long materialId;

    @Schema(description = "关联物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "关联物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    @ExcelProperty("规格型号")
    private String specification;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    @ExcelProperty("产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    @ExcelProperty("产品型号名称")
    private String productModelName;

    @Schema(description = "生产类型编码")
    private String prodType;

    @Schema(description = "生产类型名称")
    @ExcelProperty("生产类型")
    private String prodTypeName;

    @Schema(description = "关联工序ID")
    private Long processId;

    @Schema(description = "关联工序编码")
    @ExcelProperty("工序编码")
    private String processCode;

    @Schema(description = "关联工序名称")
    @ExcelProperty("工序名称")
    private String processName;

    @Schema(description = "版本号")
    @ExcelProperty("版本号")
    private String version;

    @Schema(description = "适用环节")
    @ExcelProperty("适用环节")
    private String applyType;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "审核状态")
    private Integer auditStatus;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核人姓名")
    @ExcelProperty("审核人")
    private String auditorName;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "检验项明细")
    private List<StandardItem> items;

    @Schema(description = "管理后台 - 检验标准明细 Response VO")
    @Data
    public static class StandardItem {
        private Long id;
        private Long standardId;
        private Integer sort;
        private String inspectionItem;
        private String itemType;
        private Integer expiryDays;
        private Boolean attachmentEnabled;
        private Boolean actualValueRequired;
        private BigDecimal targetValue;
        private BigDecimal minValue;
        private Integer minValueScale;
        private BigDecimal maxValue;
        private Integer maxValueScale;
        private String standardDesc;
        private String unit;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String entryRuleType;
        private Long entryRuleTemplateId;
        private String entryRuleTemplateName;
        private String ruleDescription;
        private String valueTemplate;
        private String judgmentMetric;
        private String templateParams;
        private BigDecimal avgMinLimit;
        private Integer avgMinLimitScale;
        private BigDecimal avgMaxLimit;
        private Integer avgMaxLimitScale;
        private BigDecimal stdMinLimit;
        private Integer stdMinLimitScale;
        private BigDecimal stdMaxLimit;
        private Integer stdMaxLimitScale;
        private Long sheetTemplateId;
        private String sheetSectionCode;
        private String sheetMetricCode;
        private String sheetFieldCode;
        private String testTool;
        private Integer sampleSize;
        private Boolean isSpc;
        private Long processId;
        private String processCode;
        private String processName;
    }
}
