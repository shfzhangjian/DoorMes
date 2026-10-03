package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 设备运行状态调整 Request VO")
@Data
public class HcEquipmentWorkStateAdjustReqVO {

    @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备ID不能为空")
    private Long id;

    @Schema(description = "运行状态", example = "IDLE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "运行状态不能为空")
    private String workStatus;

    @Schema(description = "是否解除当前工序挂接")
    private Boolean clearOperationBinding;

    @Schema(description = "备注")
    private String remark;
}
