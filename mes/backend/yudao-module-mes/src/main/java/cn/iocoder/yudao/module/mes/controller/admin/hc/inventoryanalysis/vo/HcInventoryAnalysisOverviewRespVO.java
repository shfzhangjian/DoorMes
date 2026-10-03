package cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 库存分析报表总览 Response VO")
@Data
public class HcInventoryAnalysisOverviewRespVO {

    @Schema(description = "当前柱状图统计维度")
    private String dimension;

    @Schema(description = "成品库位库存汇总")
    private Summary summary;

    @Schema(description = "当前库存结构柱状图数据")
    private List<DistributionItem> distributionBars;

    @Schema(description = "出入库变化趋势数据")
    private List<MovementTrendItem> movementTrend;

    @Data
    public static class Summary {

        private Long recordCount;
        private Long occupiedLocationCount;
        private BigDecimal onHandQty;
        private BigDecimal qualifiedQty;
        private BigDecimal unqualifiedQty;
        private BigDecimal shippableQty;
        private BigDecimal lockedQty;

    }

    @Data
    public static class DistributionItem {

        private String dimension;
        private String dimensionKey;
        private String dimensionName;
        private Boolean other;
        private Long recordCount;
        private BigDecimal onHandQty;
        private BigDecimal shippableQty;
        private BigDecimal lockedQty;

    }

    @Data
    public static class MovementTrendItem {

        private String periodLabel;
        private BigDecimal inboundQty;
        private BigDecimal outboundQty;
        private BigDecimal repackReturnQty;
        private BigDecimal netChangeQty;

    }

}
