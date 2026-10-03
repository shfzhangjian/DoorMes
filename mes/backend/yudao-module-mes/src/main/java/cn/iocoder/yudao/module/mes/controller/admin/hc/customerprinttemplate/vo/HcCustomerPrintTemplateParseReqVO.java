package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 客户打印模板变量解析 Request VO")
@Data
public class HcCustomerPrintTemplateParseReqVO {

    @Schema(description = "模板类型：PACKAGE/PIECE")
    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @Schema(description = "模板格式：ZPL/NLBL")
    private String templateFormat;

    @Schema(description = "模板内容")
    private String templateContent;

}
