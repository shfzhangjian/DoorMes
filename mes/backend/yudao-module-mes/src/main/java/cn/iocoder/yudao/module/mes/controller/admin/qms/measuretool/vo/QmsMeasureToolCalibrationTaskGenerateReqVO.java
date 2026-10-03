package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 生成量检具校准预警任务 Request VO")
@Data
public class QmsMeasureToolCalibrationTaskGenerateReqVO {

    @Schema(description = "覆盖默认提前预警天数")
    @Min(value = 0, message = "预警天数不能小于0")
    private Integer warningDays;

    @Schema(description = "月度任务月份，格式 yyyy-MM；为空时沿用自动预警生成逻辑")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式必须为 yyyy-MM")
    private String month;

    @Schema(description = "手工勾选纳入本月待执行任务的量检具台账ID集合")
    private List<Long> ledgerIds;

}
