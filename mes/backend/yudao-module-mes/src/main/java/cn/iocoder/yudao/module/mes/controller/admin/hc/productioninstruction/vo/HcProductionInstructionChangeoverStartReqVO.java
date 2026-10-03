package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 换型生产指令开始执行 Request VO")
@Data
public class HcProductionInstructionChangeoverStartReqVO {

    @NotNull(message = "生产指令ID不能为空")
    @Schema(description = "生产指令 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "执行备注")
    private String remark;

    @Schema(description = "认证执行人 ID")
    private Long executeUserId;

    @Schema(description = "认证执行人姓名")
    private String executeUserName;

}
