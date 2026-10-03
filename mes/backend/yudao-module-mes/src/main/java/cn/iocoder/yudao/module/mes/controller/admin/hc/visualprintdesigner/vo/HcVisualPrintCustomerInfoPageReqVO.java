package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 可视化打印客户信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcVisualPrintCustomerInfoPageReqVO extends PageParam {

    @Schema(description = "客户")
    private String customer;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "尺寸/mm")
    private String sizeMm;

    @Schema(description = "标签类型")
    private String labelKind;

    @Schema(description = "状态")
    private Integer status;

}
