package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 量检具月度待计量提醒 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolCalibrationDueHintRespVO {

    @Schema(description = "月份，格式 yyyy-MM")
    private String month;

    @Schema(description = "本月即将到期且未纳入本月任务数量")
    private Long dueSoonCount;

    @Schema(description = "过期未检且未纳入本月任务数量")
    private Long overdueCount;

    @Schema(description = "合计数量")
    private Long totalCount;

}
