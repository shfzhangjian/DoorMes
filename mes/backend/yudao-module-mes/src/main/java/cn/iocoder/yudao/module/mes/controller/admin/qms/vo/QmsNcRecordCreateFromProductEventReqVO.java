package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 从产品异常事件生成 NCR Request VO")
@Data
public class QmsNcRecordCreateFromProductEventReqVO {

    @Schema(description = "产品异常事件来源类型：FAI、GLUE_BOARD_FAI、CUT_ROUND_FQC、FG_SHIPPING_FQC、OQC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验单ID不能为空")
    private Long inspectionId;
}
