package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 配料报工切换设备 Request VO")
@Data
public class HcFormulaReportSwitchEquipmentReqVO {

    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @Schema(description = "搅拌机台ID")
    private Long mixerEquipmentId;

    @Schema(description = "搅拌机台编码")
    private String mixerEquipmentCode;

    @Schema(description = "搅拌机台名称")
    private String mixerEquipmentName;

    @Schema(description = "脱泡机台ID")
    private Long foamingEquipmentId;

    @Schema(description = "脱泡机台编码")
    private String foamingEquipmentCode;

    @Schema(description = "脱泡机台名称")
    private String foamingEquipmentName;
}
