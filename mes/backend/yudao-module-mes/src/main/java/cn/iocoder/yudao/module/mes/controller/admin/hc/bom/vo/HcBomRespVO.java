package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工艺用料清单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcBomRespVO {

    @Schema(description = "清单编码")
    @ExcelProperty("清单编码")
    private String bomCode;

    @Schema(description = "清单名称")
    @ExcelProperty("清单名称")
    private String bomName;

    @Schema(description = "产出物料ID")
    @ExcelProperty("产出物料ID")
    private Long productMaterialId;

    @Schema(description = "产出物料编码")
    @ExcelProperty("产出物料编码")
    private String productMaterialCode;

    @Schema(description = "产出物料名称")
    @ExcelProperty("产出物料名称")
    private String productMaterialName;

    @Schema(description = "产品型号ID")
    @ExcelProperty("产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    @ExcelProperty("产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    @ExcelProperty("产品型号名称")
    private String productModelName;

    @Schema(description = "成品规格/尺寸")
    @ExcelProperty("成品规格/尺寸")
    private String productSpec;

    @Schema(description = "关联配方ID")
    @ExcelProperty("关联配方ID")
    private Long recipeId;

    @Schema(description = "关联配方编码")
    @ExcelProperty("关联配方编码")
    private String recipeCode;

    @Schema(description = "关联配方名称")
    @ExcelProperty("关联配方名称")
    private String recipeName;

    @Schema(description = "关联工艺路线ID")
    @ExcelProperty("关联工艺路线ID")
    private Long routeId;

    @Schema(description = "关联工艺路线编码")
    @ExcelProperty("关联工艺路线编码")
    private String routeCode;

    @Schema(description = "版本号")
    @ExcelProperty("版本号")
    private String versionNo;

    @Schema(description = "清单类型")
    @ExcelProperty("清单类型")
    private String bomType;

    @Schema(description = "标准良率")
    @ExcelProperty("标准良率")
    private BigDecimal yieldRate;

    @Schema(description = "压槽连续作业加检数")
    @ExcelProperty("压槽连续作业加检数")
    private Integer pressSlotContinuousCheckCount;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
