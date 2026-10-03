package cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 班组新增/修改 Request VO")
@Data
public class HcTeamSaveReqVO {

    @Schema(description = "班组编码")
    @NotBlank(message = "班组编码不能为空")
    private String teamCode;

    @Schema(description = "班组名称")
    @NotBlank(message = "班组名称不能为空")
    private String teamName;

    @Schema(description = "默认工作中心ID")
    private Long workCenterId;

    @Schema(description = "默认工作中心编码")
    private String workCenterCode;

    @Schema(description = "班组长用户ID")
    private Long leaderUserId;

    @Schema(description = "班组长姓名")
    private String leaderName;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

}