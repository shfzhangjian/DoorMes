package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "管理后台 - 流程实例可调整节点 Response VO")
@Data
public class BpmProcessInstanceAdjustableNodeRespVO {

    @Schema(description = "历史任务编号", example = "1024")
    private String taskId;

    @Schema(description = "任务定义 Key", example = "quality_confirm")
    private String taskDefinitionKey;

    @Schema(description = "任务名称", example = "品质确认")
    private String taskName;

    @Schema(description = "办理人编号", example = "1")
    private Long assigneeUserId;

    @Schema(description = "办理人姓名", example = "禾臣管理员")
    private String assigneeUserName;

    @Schema(description = "办理完成时间")
    private Date endTime;

}
