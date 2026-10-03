package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板报工时间修改 Request VO")
@Data
public class HcRoughConsoleReportTimeUpdateReqVO {

    @NotNull(message = "报工记录不能为空")
    private Long id;

    @NotBlank(message = "磨皮次数不能为空")
    private String passType;

    @Schema(description = "报工日期", example = "2026-06-23")
    private String reportDate;

    @Schema(description = "开始时间", example = "2026-06-23 01:08:52")
    private String startTime;

    @Schema(description = "结束时间", example = "2026-06-23 16:08:52")
    private String endTime;

}
