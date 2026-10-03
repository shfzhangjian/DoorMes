package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备本月保养追加候选 Response VO")
@Data
public class ResourceDeviceMaintCandidateRespVO {

    @Schema(description = "设备台账ID")
    private Long deviceId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备分类ID")
    private Long categoryId;

    @Schema(description = "设备分类")
    private String categoryName;

    @Schema(description = "保养标准ID")
    private Long standardId;

    @Schema(description = "保养标准编码")
    private String standardCode;

    @Schema(description = "保养标准名称")
    private String standardName;

    @Schema(description = "保养频率")
    private String frequency;

    @Schema(description = "保养类型")
    private String maintType;

    @Schema(description = "上次计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastPlanDate;

    @Schema(description = "上次完成日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastActualDate;

    @Schema(description = "建议本次计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate taskDueDate;

    @Schema(description = "是否本月到期")
    private Boolean dueInCurrentMonth;

    @Schema(description = "是否过期未检")
    private Boolean overdue;

    @Schema(description = "预警状态")
    private String warningStatus;

}
