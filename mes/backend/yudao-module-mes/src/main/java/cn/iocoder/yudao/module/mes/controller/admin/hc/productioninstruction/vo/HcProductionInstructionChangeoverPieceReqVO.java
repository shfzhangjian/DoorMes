package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 换型生产指令扫码片号 Request VO")
@Data
public class HcProductionInstructionChangeoverPieceReqVO {

    @NotNull(message = "生产指令ID不能为空")
    @Schema(description = "生产指令 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "片号不能为空")
    @Schema(description = "扫码片号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pieceNo;

    @Schema(description = "粘胶2报工记录 ID")
    private Long adhesive2ReportId;

    @Schema(description = "备注")
    private String remark;

}
