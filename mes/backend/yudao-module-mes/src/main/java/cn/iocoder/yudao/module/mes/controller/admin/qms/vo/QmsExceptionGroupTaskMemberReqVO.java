package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件小组任务成员 Request VO")
@Data
public class QmsExceptionGroupTaskMemberReqVO {

    @Schema(description = "成员ID")
    private Long id;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "成员角色")
    private String memberRole;

    @Schema(description = "排序")
    private Integer sortNo;
}
