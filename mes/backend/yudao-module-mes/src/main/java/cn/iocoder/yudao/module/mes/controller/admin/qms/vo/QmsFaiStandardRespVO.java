package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FAI检验标准带出 Response VO")
@Data
public class QmsFaiStandardRespVO {

    @Schema(description = "检验标准ID")
    private Long standardId;

    @Schema(description = "检验标准编号")
    private String standardNo;

    @Schema(description = "检验标准名称")
    private String standardName;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "适用环节")
    private String applyType;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "检验项明细")
    private List<StandardItem> items;

    @Schema(description = "管理后台 - FAI检验标准项 Response VO")
    @Data
    public static class StandardItem {
        private Long standardItemId;
        private String inspectionItem;
        private String itemType;
        private Boolean attachmentEnabled;
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
        private Long sheetTemplateId;
        private String sheetSectionCode;
        private String sheetMetricCode;
        private String sheetFieldCode;
        private String testTool;
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private Boolean isSpc;
        private Integer sort;
    }
}
