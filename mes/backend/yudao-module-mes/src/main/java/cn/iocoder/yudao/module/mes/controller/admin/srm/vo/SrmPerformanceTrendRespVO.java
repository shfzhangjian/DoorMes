package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商绩效趋势 Response VO")
@Data
public class SrmPerformanceTrendRespVO {

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商编号")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "年度")
    private Integer evalYear;

    @Schema(description = "指标树")
    private List<TreeNode> tree;

    @Schema(description = "已选指标编码")
    private List<String> selectedIndicatorCodes;

    @Schema(description = "趋势图表")
    private List<Panel> panels;

    @Data
    public static class TreeNode {

        @Schema(description = "节点Key")
        private String key;

        @Schema(description = "节点标题")
        private String title;

        @Schema(description = "指标编码")
        private String indicatorCode;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "维度")
        private String groupName;

        @Schema(description = "图表类型")
        private String chartType;

        @Schema(description = "是否可选")
        private Boolean selectable;

        @Schema(description = "子节点")
        private List<TreeNode> children;

    }

    @Data
    public static class Panel {

        @Schema(description = "指标编码")
        private String indicatorCode;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "维度")
        private String groupName;

        @Schema(description = "图表类型")
        private String chartType;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "汇总列标题")
        private String summaryLabel;

        @Schema(description = "月份")
        private List<String> months;

        @Schema(description = "图表序列")
        private List<Series> series;

        @Schema(description = "明细行")
        private List<Row> rows;

    }

    @Data
    public static class Series {

        @Schema(description = "序列名称")
        private String name;

        @Schema(description = "bar/line")
        private String type;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "是否百分比")
        private Boolean percent;

        @Schema(description = "月度值")
        private List<BigDecimal> values;

    }

    @Data
    public static class Row {

        @Schema(description = "行标题")
        private String label;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "是否百分比")
        private Boolean percent;

        @Schema(description = "月度展示值")
        private List<String> values;

        @Schema(description = "汇总展示值")
        private String summary;

    }

}
