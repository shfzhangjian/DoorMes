package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备年度保养计划确认 Request VO")
@Data
public class ResourceDeviceMaintPlanConfirmReqVO {

    @NotEmpty(message = "计划ID不能为空")
    private List<Long> ids;

}
