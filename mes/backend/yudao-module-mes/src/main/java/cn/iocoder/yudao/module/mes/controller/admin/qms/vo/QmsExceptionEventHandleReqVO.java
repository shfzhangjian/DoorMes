package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - QMS异常事件办理 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsExceptionEventHandleReqVO extends QmsExceptionEventBaseVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long id;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "办理意见")
    private String opinion;

    @Schema(description = "下一处理人ID")
    private Long nextHandlerUserId;

    @Schema(description = "下一处理人名称")
    private String nextHandlerUserName;

    @Schema(description = "本次提交选择的抄送人ID列表")
    private List<Long> copyToUserIds;

    @Schema(description = "本次提交选择的抄送人名称列表")
    private List<String> copyToUserNames;

    @Schema(description = "临时调查小组")
    private List<QmsExceptionTeamMemberReqVO> teamMembers;

    @Schema(description = "围堵/责任部门会签任务")
    private List<QmsExceptionGroupTaskReqVO> groupTasks;

    @Schema(description = "关联对象/附件")
    private List<QmsExceptionRelationReqVO> relations;
}
