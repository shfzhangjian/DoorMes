package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 物料分类 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcMaterialCategoryRespVO {

    @Schema(description = "父级分类ID")
    @ExcelProperty("父级分类ID")
    private Long parentId;

    @Schema(description = "分类编码")
    @ExcelProperty("分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    @ExcelProperty("分类名称")
    private String categoryName;

    @Schema(description = "分类编号路径")
    @ExcelProperty("分类编号路径")
    private String categoryCodePath;

    @Schema(description = "分类名称路径")
    @ExcelProperty("分类名称路径")
    private String categoryNamePath;

    @Schema(description = "兼容旧字段的分类路径")
    @ExcelProperty("分类路径")
    private String categoryPath;

    @Schema(description = "排序号")
    @ExcelProperty("排序号")
    private Integer sort;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}