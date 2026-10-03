package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import lombok.Data;

@Data
public class HcProcessFormSectionRespVO {

    private Long id;
    private Long templateId;
    private Long versionId;
    private String sectionCode;
    private String sectionName;
    private String areaType;
    private String layoutType;
    private Integer sortNo;
    private String remark;
}
