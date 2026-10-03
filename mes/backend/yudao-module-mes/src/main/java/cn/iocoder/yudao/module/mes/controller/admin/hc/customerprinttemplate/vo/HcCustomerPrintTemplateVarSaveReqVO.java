package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 客户打印模板变量新增/修改 Request VO")
@Data
public class HcCustomerPrintTemplateVarSaveReqVO {

    private Long id;

    @NotBlank(message = "变量名不能为空")
    private String variableName;

    private String variableExpr;

    @NotBlank(message = "变量范围不能为空")
    private String variableScope;

    private String sourceField;
    private String sourceLabel;
    private Integer sort;
    private Boolean required;
    private String defaultValue;
    private String remark;

}
