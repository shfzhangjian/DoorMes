package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 粘双面胶报工切换设备 Request VO")
@Data
public class HcAdhesiveReportSwitchEquipmentReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;

    @Schema(description = "换机原因")
    private String switchReason;
}
