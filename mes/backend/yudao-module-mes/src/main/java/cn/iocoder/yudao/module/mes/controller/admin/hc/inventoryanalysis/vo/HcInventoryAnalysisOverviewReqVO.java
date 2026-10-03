package cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - 库存分析报表总览 Request VO")
@Data
public class HcInventoryAnalysisOverviewReqVO {

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "成品库位编码")
    private String locationCode;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "分段批次，对应 mes_inv_finished_stock.batch_no")
    private String batchNo;

    @Schema(description = "片号，对应 mes_inv_finished_stock.slice_batch_no")
    private String sliceBatchNo;

    @Schema(description = "质量状态：OK/NG")
    @Pattern(regexp = "OK|NG", message = "质量状态只能为 OK 或 NG")
    private String qualityStatus;

    @Schema(description = "当前库存状态")
    @Pattern(regexp = "INBOUND_LOCKED|INBOUNDED|AVAILABLE|OUTBOUND_LOCKED|ALLOCATED",
            message = "不支持的成品库存状态")
    private String stockStatus;

    @Schema(description = "柱状图统计维度")
    @Pattern(regexp = "WAREHOUSE|LOCATION|RACK|LAYER|MODEL|SEGMENT_BATCH|SLICE_BATCH|MATERIAL|QUALITY_STATUS|STOCK_STATUS",
            message = "不支持的柱状图统计维度")
    private String dimension = "MODEL";

    @Schema(description = "柱状图每个单位保留的前 N 项")
    @Min(value = 5, message = "Top N 不能小于 5")
    @Max(value = 50, message = "Top N 不能大于 50")
    private Integer topN = 20;

    @Schema(description = "出入库趋势粒度：DAY/WEEK/MONTH")
    @Pattern(regexp = "DAY|WEEK|MONTH", message = "趋势粒度只能为 DAY、WEEK 或 MONTH")
    private String granularity = "DAY";

    @Schema(description = "统计开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "统计结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @AssertTrue(message = "统计开始日期不能晚于统计结束日期")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }

}
