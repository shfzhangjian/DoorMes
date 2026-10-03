package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import lombok.Data;

@Data
public class HcProductModelMaterialRespVO {

    private Long id;
    private Long modelId;
    private String modelCode;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Boolean isDefault;
    private String remark;

}
