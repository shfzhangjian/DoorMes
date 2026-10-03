package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮一磨模式锁定 Request VO")
@Data
public class HcRoughConsoleAllocationModeSaveReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotBlank(message = "母批号不能为空")
    private String motherBatchNo;

    @NotBlank(message = "一磨模式不能为空")
    private String allocationMode;
}
