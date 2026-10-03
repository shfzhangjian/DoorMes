package cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 客户打印模板变量 Response VO")
@Data
public class HcCustomerPrintTemplateVarRespVO {

    private Long id;
    private Long templateId;
    private String variableName;
    private String variableExpr;
    private String variableScope;
    private String sourceField;
    private String sourceLabel;
    private Integer sort;
    private Boolean required;
    private String defaultValue;
    private String remark;

}
