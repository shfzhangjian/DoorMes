package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 配料报工时间修改日志 Response VO")
@Data
public class HcFormulaReportTimeLogRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "报工记录ID")
    private Long operationReportId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "计划单号")
    private String planNo;

    @Schema(description = "修改前报工日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate beforeReportDate;

    @Schema(description = "修改后报工日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate afterReportDate;

    @Schema(description = "修改前开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime beforeStartTime;

    @Schema(description = "修改后开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime afterStartTime;

    @Schema(description = "修改前结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime beforeEndTime;

    @Schema(description = "修改后结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime afterEndTime;

    @Schema(description = "修改前报工时长分钟")
    private Integer beforeReportMinutes;

    @Schema(description = "修改后报工时长分钟")
    private Integer afterReportMinutes;

    @Schema(description = "修改人ID")
    private Long operatorId;

    @Schema(description = "修改人")
    private String operatorName;

    @Schema(description = "修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime changeTime;

}
