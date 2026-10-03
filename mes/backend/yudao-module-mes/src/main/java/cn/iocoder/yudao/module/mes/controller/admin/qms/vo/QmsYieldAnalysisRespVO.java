package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 良品率分析 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsYieldAnalysisRespVO {

    @Schema(description = "总览")
    private Overview overview;

    @Schema(description = "母卷批号/分段/工序汇总")
    private List<SummaryRow> summaryRows;

    @Schema(description = "母卷批号/分段透视汇总行")
    private List<SegmentRow> segmentRows;

    @Schema(description = "主要缺陷分布")
    private List<DefectDistribution> defectDistribution;

    @Schema(description = "时间趋势")
    private List<TrendPoint> trendRows;

    @Schema(description = "公式说明")
    private String formulaText;

    @Schema(description = "公式示例")
    private String formulaExample;

    @Schema(description = "总览")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {

        private Integer confirmedTotal;
        private BigDecimal goodTotal;
        private BigDecimal ngTotal;
        private Integer selfCheckNgTotal;
        private Integer submissionNgTotal;
        private BigDecimal inputTotal;
        private BigDecimal outputGoodTotal;
        private BigDecimal outputNgTotal;
        private Integer inspectionTotal;
        private BigDecimal yieldRate;
        private BigDecimal targetQualifiedQty;
        private String targetUnit;
        private String targetType;
        private String targetModelCode;
        private String targetProcessName;
        private String primaryDefectName;
        private Integer primaryDefectCount;
    }

    @Schema(description = "母卷批号/分段/工序汇总行")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryRow {

        private String groupKey;
        private String modelCode;
        private String motherRollNo;
        private String segmentNo;
        private String processCode;
        private String processName;
        private String planNo;
        private Integer confirmedTotal;
        private BigDecimal goodTotal;
        private BigDecimal ngTotal;
        private Integer selfCheckNgTotal;
        private Integer submissionNgTotal;
        private BigDecimal inputTotal;
        private BigDecimal outputGoodTotal;
        private BigDecimal outputNgTotal;
        private Integer inspectionTotal;
        private BigDecimal yieldRate;
        private BigDecimal targetQualifiedQty;
        private String targetUnit;
        private String targetType;
        private BigDecimal targetAchievementRate;
        private BigDecimal targetDifference;
        private Boolean targetMatched;
        private Boolean targetReached;
        private Integer blackDotCount;
        private Integer blueDotCount;
        private Integer yellowDotCount;
        private Integer redDotCount;
        private Integer pinholeCount;
        private Integer stripeCount;
        private Integer wrinkleCount;
        private Integer waveCount;
        private Integer otherCount;
        private String defectSummary;
    }

    @Schema(description = "母卷批号/分段透视汇总行")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentRow {

        private String groupKey;
        private String modelCode;
        private String motherRollNo;
        private String segmentNo;
        private List<ProcessMetric> processMetrics;
    }

    @Schema(description = "工序指标")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessMetric {

        private String processCode;
        private String processName;
        private String planNo;
        private Integer confirmedTotal;
        private BigDecimal goodTotal;
        private BigDecimal ngTotal;
        private Integer selfCheckNgTotal;
        private Integer submissionNgTotal;
        private BigDecimal inputTotal;
        private BigDecimal outputGoodTotal;
        private BigDecimal outputNgTotal;
        private Integer inspectionTotal;
        private BigDecimal yieldRate;
        private BigDecimal targetQualifiedQty;
        private String targetUnit;
        private String targetType;
        private BigDecimal targetAchievementRate;
        private BigDecimal targetDifference;
        private Boolean targetMatched;
        private Boolean targetReached;
        private Integer blackDotCount;
        private Integer blueDotCount;
        private Integer yellowDotCount;
        private Integer redDotCount;
        private Integer pinholeCount;
        private Integer stripeCount;
        private Integer wrinkleCount;
        private Integer waveCount;
        private Integer otherCount;
        private String defectSummary;
    }

    @Schema(description = "缺陷分布")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectDistribution {

        private String defectName;
        private Integer defectCount;
        private BigDecimal ratio;
    }

    @Schema(description = "时间趋势")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {

        private String statDate;
        private Integer confirmedTotal;
        private BigDecimal ngTotal;
        private BigDecimal inputTotal;
        private BigDecimal outputGoodTotal;
        private BigDecimal outputNgTotal;
        private Integer inspectionTotal;
        private BigDecimal yieldRate;
    }
}
