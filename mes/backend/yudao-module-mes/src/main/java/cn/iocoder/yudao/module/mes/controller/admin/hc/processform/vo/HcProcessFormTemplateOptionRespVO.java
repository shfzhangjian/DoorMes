package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import lombok.Data;

@Data
public class HcProcessFormTemplateOptionRespVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String processCode;
    private String processName;
    private String modelScope;
    private String modelCode;
    private String modelName;
    private String formType;
    private String formTypeName;
}
