package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import lombok.Data;

@Data
public class HcProductModelSegmentRespVO {

    private Long id;
    private Long modelId;
    private String modelCode;
    private Long ruleId;
    private String ruleCode;
    private Long ruleItemId;
    private String itemCode;
    private String itemName;
    private String segmentValue;
    private String segmentText;
    private Long dictId;
    private Integer sort;

}
