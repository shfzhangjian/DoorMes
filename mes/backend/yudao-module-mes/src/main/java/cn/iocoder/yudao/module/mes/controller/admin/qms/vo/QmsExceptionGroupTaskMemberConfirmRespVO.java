package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件小组成员确认 Response VO")
@Data
public class QmsExceptionGroupTaskMemberConfirmRespVO {

    @Schema(description = "异常事件ID")
    private Long exceptionId;

    @Schema(description = "异常单号")
    private String exceptionNo;

    @Schema(description = "小组任务ID")
    private Long taskId;

    @Schema(description = "确认状态 CONFIRMED/EXPIRED")
    private String confirmStatus;

    @Schema(description = "是否超期")
    private Boolean overdue;

    @Schema(description = "提示文案")
    private String message;
}
