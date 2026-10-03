package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 设备年度保养计划月度周次保存 Request VO")
@Data
public class ResourceDeviceMaintPlanWeekSaveReqVO {

    @NotNull(message = "计划年份不能为空")
    private Integer planYear;

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份必须在1到12之间")
    @Max(value = 12, message = "月份必须在1到12之间")
    private Integer monthNo;

    @NotNull(message = "周次不能为空")
    @Min(value = 1, message = "周次必须在1到5之间")
    @Max(value = 5, message = "周次必须在1到5之间")
    private Integer weekNo;

    private Long deviceId;

    @NotBlank(message = "设备编号不能为空")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    private Long standardId;
    private String standardCode;
    private String standardName;
    private Long standardItemId;
    private String itemGroup;

    @NotBlank(message = "保养项目不能为空")
    private String itemName;

    private String method;
    private String requirement;
    private String frequency;
    private String maintType;
    private String sourceRule;
    private String remark;

}
