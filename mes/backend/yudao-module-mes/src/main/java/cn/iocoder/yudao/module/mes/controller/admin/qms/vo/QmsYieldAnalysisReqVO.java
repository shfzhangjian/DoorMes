package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 良品率分析查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsYieldAnalysisReqVO extends PageParam {

    @Schema(description = "开始日期", example = "2026-07-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-07-28")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDate;

    @Schema(description = "聚焦统计日期", example = "2026-07-28")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate statDate;

    @Schema(description = "工序编码：SLITTING/PRESS_SLOT/ADHESIVE2/CUT_ROUND")
    private String processCode;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "母卷批号")
    private String motherRollNo;

    @Schema(description = "分段批号")
    private String segmentNo;

    @Schema(description = "片号")
    private String pieceNo;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "物料/型号关键字")
    private String materialKeyword;

    @Schema(description = "缺陷名称")
    private String defectName;

    @Schema(description = "钻取指标字段")
    private String metricKey;

    @Schema(description = "综合关键字")
    private String keyword;
}
