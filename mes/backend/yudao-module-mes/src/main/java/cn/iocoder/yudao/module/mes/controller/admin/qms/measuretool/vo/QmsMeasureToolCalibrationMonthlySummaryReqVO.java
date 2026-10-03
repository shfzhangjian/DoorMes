package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 量检具校准月度汇总 Request VO")
@Data
public class QmsMeasureToolCalibrationMonthlySummaryReqVO {

    @Schema(description = "校准日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] calibrationDate;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "使用部门")
    private String usingDepartment;

}
