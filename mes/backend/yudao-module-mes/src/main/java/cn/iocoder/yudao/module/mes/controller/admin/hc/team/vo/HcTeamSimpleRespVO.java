package cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 班组 Simple Response VO")
@Data
public class HcTeamSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "状态")
    private Integer status;

}