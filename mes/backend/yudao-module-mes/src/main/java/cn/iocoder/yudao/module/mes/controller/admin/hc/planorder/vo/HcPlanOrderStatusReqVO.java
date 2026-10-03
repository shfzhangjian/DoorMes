package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划主表状态操作 Request VO")
@Data
public class HcPlanOrderStatusReqVO {

    @Schema(description = "生产计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生产计划ID不能为空")
    private Long id;

    @Schema(description = "目标状态：DRAFT/RELEASED/CLOSED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标状态不能为空")
    private String planStatus;

    @Schema(description = "状态操作备注", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态操作备注不能为空")
    private String reasonRemark;

}
