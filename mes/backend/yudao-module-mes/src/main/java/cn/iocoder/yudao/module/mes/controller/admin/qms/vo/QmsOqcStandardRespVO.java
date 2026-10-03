package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - OQC出货检验标准 Response VO")
@Data
public class QmsOqcStandardRespVO {

    private Long standardId;
    private String standardNo;
    private String standardName;
    private String version;
    private String applyType;
    private String materialCode;
    private String materialName;
    private String specification;
    private String productModelCode;
    private String productModelName;
    private List<StandardItem> items;

    @Data
    public static class StandardItem {
        private Long standardItemId;
        private Integer sort;
        private String category;
        private String inspectionItem;
        private String itemType;
        private BigDecimal targetValue;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private String standardDesc;
        private String unit;
        private String ruleDescription;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String valueTemplate;
        private String judgmentMetric;
        private String templateParams;
        private BigDecimal avgMinLimit;
        private BigDecimal avgMaxLimit;
        private BigDecimal stdMinLimit;
        private BigDecimal stdMaxLimit;
        private String testTool;
        private Integer sampleSize;
        private Boolean isSpc;
    }
}
