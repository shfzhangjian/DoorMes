package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 良品率分析（新版）分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsYieldAnalysisV2PageReqVO extends QmsMotherRollGoodStatisticsPageReqVO {

    @Schema(description = "实际报工日期-开始", example = "2026-08-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualReportDateStart;

    @Schema(description = "实际报工日期-结束", example = "2026-08-18")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualReportDateEnd;

    @Schema(description = "工序编码；ALL 表示全部工序")
    private String processCode;

    @Schema(description = "片号")
    private String pieceNo;

    @Schema(description = "钻取指标字段")
    private String metricKey;

}
