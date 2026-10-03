package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import lombok.Data;

@Data
public class HcProductModelSelectOptionRespVO {

    private Long value;
    private String label;
    private String code;
    private String modelName;
    private String modelLevel;
    private Long parentModelId;
    private String parentModelCode;
    private String parentModelName;
    private Long recipeId;
    private String recipeCode;
    private String recipeName;
    private String sizeSpec;
    private String sizeName;
    private String status;

}
