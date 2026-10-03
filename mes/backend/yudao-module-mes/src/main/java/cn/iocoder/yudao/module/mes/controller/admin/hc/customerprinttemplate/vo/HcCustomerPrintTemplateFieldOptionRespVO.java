package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 客户打印模板字段选项 Response VO")
@Data
public class HcCustomerPrintTemplateFieldOptionRespVO {

    @Schema(description = "字段范围：HEAD/DETAIL/PRINT")
    private String scope;

    @Schema(description = "来源字段")
    private String sourceField;

    @Schema(description = "字段名称")
    private String sourceLabel;

    @Schema(description = "变量表达式")
    private String variableExpr;

}
