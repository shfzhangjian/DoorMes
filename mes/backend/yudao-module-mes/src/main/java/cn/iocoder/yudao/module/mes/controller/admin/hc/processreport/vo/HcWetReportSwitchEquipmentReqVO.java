package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工切换设备 Request VO")
@Data
public class HcWetReportSwitchEquipmentReqVO {

    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @Schema(description = "机台编号ID")
    private Long equipmentId;

    @Schema(description = "机台编号编码")
    private String equipmentCode;

    @Schema(description = "机台编号名称")
    private String equipmentName;
}
