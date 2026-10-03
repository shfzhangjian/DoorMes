package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.idev.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 量检具月度待计量候选台账 Response VO")
@Data
public class QmsMeasureToolCalibrationTaskCandidateRespVO {

    @Schema(description = "量检具台账ID")
    private Long ledgerId;

    @Schema(description = "量检具编码")
    private String toolCode;

    @Schema(description = "量检具名称")
    private String toolName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类")
    private String categoryName;

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "保管人")
    private String keeperName;

    @Schema(description = "上次校准日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate lastCalibrationDate;

    @Schema(description = "台账下次校准日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate nextCalibrationDate;

    @Schema(description = "本次月度任务应执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate taskDueDate;

    @Schema(description = "是否本月到期")
    private Boolean dueInSelectedMonth;

    @Schema(description = "是否已过期未检")
    private Boolean overdue;

    @Schema(description = "预警状态")
    private String warningStatus;

}
