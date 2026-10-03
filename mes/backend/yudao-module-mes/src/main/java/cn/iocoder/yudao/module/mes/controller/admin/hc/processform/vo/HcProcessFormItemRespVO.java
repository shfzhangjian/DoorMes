package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import lombok.Data;

@Data
public class HcProcessFormItemRespVO {

    private Long id;
    private Long templateId;
    private Long versionId;
    private Long sectionId;
    private String areaType;
    private Integer itemSeq;
    private String fieldKey;
    private String fieldLabel;
    private String itemCategory;
    private String stepNode;
    private String standardText;
    private String unit;
    private String valueMode;
    private String controlType;
    private String defaultValue;
    private String defaultResult;
    private Boolean requiredFlag;
    private String sourceSheet;
    private String sourceCell;
    private String sourceRowJson;
    private String bindSourceType;
    private String bindSourceKey;
    private String remark;
}
