package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 设备年度保养计划执行 Request VO")
@Data
public class ResourceDeviceMaintPlanExecuteReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long id;

    @Schema(description = "完成情况")
    private String executeRemark;

}
