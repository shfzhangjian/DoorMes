package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 生产指令撤下 Request VO")
@Data
public class HcProductionInstructionRevokeReqVO {

    @NotNull(message = "生产指令ID不能为空")
    @Schema(description = "生产指令 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "撤下原因")
    private String revokeReason;

}
