package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备保养批量确认 Request VO")
@Data
public class ResourceDeviceMaintConfirmReqVO {

    @Schema(description = "保养工单ID")
    @NotEmpty(message = "待确认记录不能为空")
    private List<Long> ids;

}
