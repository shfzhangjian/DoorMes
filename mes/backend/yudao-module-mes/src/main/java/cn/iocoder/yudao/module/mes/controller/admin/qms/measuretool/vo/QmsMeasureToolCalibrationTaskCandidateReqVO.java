package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "管理后台 - 量检具月度待计量候选台账 Request VO")
@Data
public class QmsMeasureToolCalibrationTaskCandidateReqVO {

    @Schema(description = "月份，格式 yyyy-MM")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式必须为 yyyy-MM")
    private String month;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "是否包含非本月到期台账；默认 false")
    private Boolean includeNonMonthDue;

}
