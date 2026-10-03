package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 提交质量任务当前流程节点 Request VO")
@Data
public class QmsDispatchTaskProcessSubmitReqVO {

    @Schema(description = "质量任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "质量任务编号不能为空")
    private Long id;

    @Schema(description = "办理意见", example = "检验记录填写完成")
    private String reason;
}
