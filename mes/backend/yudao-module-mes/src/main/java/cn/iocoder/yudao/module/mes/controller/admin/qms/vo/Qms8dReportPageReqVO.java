package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 8D报告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class Qms8dReportPageReqVO extends PageParam {

    @Schema(description = "页签类型：todo/initiated/processed/monitor")
    private String tabType;

    @Schema(description = "8D报告号")
    private String reportNo;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "当前阶段")
    private String currentStep;

    @Schema(description = "8D状态")
    private String status;

    @Schema(description = "立案日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] issueDate;

    @Schema(description = "要求结案日范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] targetDate;
}
