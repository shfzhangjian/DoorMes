package cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工艺路线 Simple Response VO")
@Data
public class HcRouteSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "路线编码")
    private String routeCode;

    @Schema(description = "路线名称")
    private String routeName;

    @Schema(description = "状态")
    private Integer status;

}