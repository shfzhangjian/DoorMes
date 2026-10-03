package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - QMS异常事件创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsExceptionEventCreateReqVO extends QmsExceptionEventBaseVO {

    @NotBlank(message = "异常描述不能为空")
    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Schema(description = "临时调查小组")
    private List<QmsExceptionTeamMemberReqVO> teamMembers;

    @Schema(description = "围堵与根因临时小组任务")
    private List<QmsExceptionGroupTaskReqVO> groupTasks;

    @Schema(description = "关联对象/附件")
    private List<QmsExceptionRelationReqVO> relations;
}
