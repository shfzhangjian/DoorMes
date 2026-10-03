package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2成品COA送检单撤回 Request VO")
@Data
public class HcAdhesive2CoaWithdrawReqVO {

    @Schema(description = "生产计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "生产计划不能为空")
    private Long planId;

    @Schema(description = "计划工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "计划工序不能为空")
    private Long planOperationId;

    @Schema(description = "FAI送检单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "FAI送检单不能为空")
    private Long faiId;

    @Schema(description = "撤回原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "片号录入错误")
    @NotBlank(message = "请填写撤回原因")
    @Size(max = 500, message = "撤回原因长度不能超过 500 个字符")
    private String withdrawReason;

}
