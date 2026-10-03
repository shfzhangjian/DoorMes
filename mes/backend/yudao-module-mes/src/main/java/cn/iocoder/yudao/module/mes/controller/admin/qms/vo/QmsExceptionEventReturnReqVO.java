package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件退回 Request VO")
@Data
public class QmsExceptionEventReturnReqVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long id;

    @Schema(description = "退回目标节点编码")
    private String targetNodeCode;

    @Schema(description = "退回目标节点名称")
    private String targetNodeName;

    @Schema(description = "退回意见", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退回意见不能为空")
    private String opinion;
}
