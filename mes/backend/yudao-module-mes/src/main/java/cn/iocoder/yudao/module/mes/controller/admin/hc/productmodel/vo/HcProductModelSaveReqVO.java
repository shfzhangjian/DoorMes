package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 产品型号字典新增/修改 Request VO")
@Data
public class HcProductModelSaveReqVO {

    private Long id;

    @NotBlank(message = "产品型号编码不能为空")
    private String modelCode;

    private String modelName;
    private String modelAlias;
    private String modelLevel;
    private Long parentModelId;

    @NotNull(message = "型号规则不能为空")
    private Long modelRuleId;

    private String modelRuleCode;
    private String modelRuleName;
    private String productType;
    private String prodType;
    private String prodTypeName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String sizeSpec;
    private String sizeName;
    private Integer retentionPeriodValue;
    private String retentionPeriodUnit;
    private Long recipeId;
    private String recipeCode;
    private String recipeName;
    private Long defaultRouteId;
    private String defaultRouteCode;
    private String defaultRouteName;
    private String sourceType;
    private String status;
    private String remark;
    private List<HcProductModelSegmentReqVO> modelSegments;
    private List<HcProductModelMaterialReqVO> modelMaterials;

}
