package cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工艺路线分页 Request VO")
@Data
public class HcRoutePageReqVO extends PageParam {

    @Schema(description = "路线编码")
    private String routeCode;

    @Schema(description = "路线名称")
    private String routeName;

    @Schema(description = "适用范围")
    private String applicableScope;

    @Schema(description = "适用物料ID")
    private Long productMaterialId;

    @Schema(description = "适用物料编码")
    private String productMaterialCode;

    @Schema(description = "适用层级")
    private String productLevel;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "路线类型")
    private String routeType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
