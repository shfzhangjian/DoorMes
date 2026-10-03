package cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 班组分页 Request VO")
@Data
public class HcTeamPageReqVO extends PageParam {

    @Schema(description = "班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
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
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}