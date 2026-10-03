package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 客户打印模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcCustomerPrintTemplatePageReqVO extends PageParam {

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "客户编号")
    private String customerCode;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "模板类型：PACKAGE/PIECE")
    private String templateType;

    @Schema(description = "状态")
    private Integer status;

}
