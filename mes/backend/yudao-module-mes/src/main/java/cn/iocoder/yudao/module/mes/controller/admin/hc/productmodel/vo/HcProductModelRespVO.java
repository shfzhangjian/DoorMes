package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 产品型号字典 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcProductModelRespVO {

    @ExcelProperty("产品型号编码")
    private String modelCode;
    @ExcelProperty("产品型号名称")
    private String modelName;
    private String modelAlias;
    @ExcelProperty("型号层级")
    private String modelLevel;
    private Long parentModelId;
    @ExcelProperty("所属系列编码")
    private String parentModelCode;
    @ExcelProperty("所属系列名称")
    private String parentModelName;
    private Long modelRuleId;
    @ExcelProperty("型号规则编码")
    private String modelRuleCode;
    @ExcelProperty("型号规则名称")
    private String modelRuleName;
    private String productType;
    private String prodType;
    @ExcelProperty("生产类型")
    private String prodTypeName;
    private Long categoryId;
    private String categoryCode;
    @ExcelProperty("物料类型")
    private String categoryName;
    private String sizeSpec;
    @ExcelProperty("尺寸规格")
    private String sizeName;
    @ExcelProperty("留样时长")
    private Integer retentionPeriodValue;
    @ExcelProperty("留样单位")
    private String retentionPeriodUnit;
    private Long recipeId;
    @ExcelProperty("配方编码")
    private String recipeCode;
    private String recipeName;
    private Long defaultRouteId;
    private String defaultRouteCode;
    private String defaultRouteName;
    private String segmentSnapshotJson;
    private String sourceType;
    @ExcelProperty("状态")
    private String status;
    private Boolean referencedFlag;
    private String remark;
    private Long id;
    private LocalDateTime createTime;

}
