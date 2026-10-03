package cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 班组 Select Option Response VO")
@Data
public class HcTeamSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "班组编码")
    private String code;

    @Schema(description = "状态")
    private Integer status;

}