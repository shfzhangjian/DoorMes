package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 量检具校准月度汇总 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolCalibrationMonthlySummaryRespVO {

    @Schema(description = "月份 yyyy-MM")
    private String month;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "应校准任务数")
    private Long taskCount;

    @Schema(description = "已校准记录数")
    private Long recordCount;

    @Schema(description = "合格数")
    private Long qualifiedCount;

    @Schema(description = "不合格数")
    private Long unqualifiedCount;

    @Schema(description = "限用数")
    private Long limitedCount;

    @Schema(description = "逾期完成数")
    private Long overdueCompletedCount;

    @Schema(description = "完成率")
    private BigDecimal completionRate;

}
