package cn.iocoder.yudao.module.mes.controller.admin.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - MES物料主数据 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesMaterialRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9152")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "物料编码 (ERP码)", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("物料编码 (ERP码)")
    private String code;

    @Schema(description = "物料名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("物料名称")
    private String name;

    @Schema(description = "物料分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "物料分类", converter = DictConvert.class)
    @DictFormat("mes_material_category")
    private String category;

    @Schema(description = "物料来源", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "物料来源", converter = DictConvert.class)
    @DictFormat("mes_material_source")
    private String materialSource;

    @Schema(description = "材质牌号")
    @ExcelProperty("材质牌号")
    private String materialGrade;

    @Schema(description = "规格型号")
    @ExcelProperty("规格型号")
    private String spec;

    @Schema(description = "计量单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("计量单位")
    private String unit;

    @Schema(description = "单重 (kg) ")
    @ExcelProperty("单重 (kg) ")
    private BigDecimal unitWeight;

    @Schema(description = "理论废品率 (%)")
    @ExcelProperty("理论废品率 (%)")
    private BigDecimal scrapRate;

    @Schema(description = "图纸/规范附件路径")
    @ExcelProperty("图纸路径")
    private String drawingUrl;

    @Schema(description = "采购/生产提前期(天)")
    @ExcelProperty("提前期")
    private Integer leadTime;

    @Schema(description = "默认供应商关联ID")
    @ExcelProperty("供应商ID")
    private Long supplierId;

    @Schema(description = "默认供应商关联名称")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat("common_status") // 假设状态用的是通用状态字典
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
