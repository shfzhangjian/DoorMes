package cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工艺路线 Select Option Response VO")
@Data
public class HcRouteSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "路线编码")
    private String code;

    @Schema(description = "状态")
    private Integer status;

}