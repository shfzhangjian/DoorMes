package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件会签办理委托 Request VO")
@Data
public class QmsExceptionGroupTaskMemberDelegateReqVO {

    @Schema(description = "小组任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "小组任务ID不能为空")
    private Long taskId;

    @Schema(description = "会签成员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会签成员ID不能为空")
    private Long memberId;

    @Schema(description = "被委托代办人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "被委托代办人不能为空")
    private Long delegateUserId;

    @Schema(description = "被委托代办人姓名")
    private String delegateUserName;

}
