package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D团队成员 Request VO")
@Data
public class QmsException8dTeamMemberReqVO {

    @Schema(description = "成员角色")
    private String memberRole;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "团队内职责说明")
    private String responsibility;

    @Schema(description = "排序")
    private Integer sort;
}
