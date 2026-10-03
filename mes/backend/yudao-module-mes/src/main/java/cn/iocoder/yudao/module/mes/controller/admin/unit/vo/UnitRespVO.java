package cn.iocoder.yudao.module.mes.controller.admin.unit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - MES计量单位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UnitRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "11731")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "单位符号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("单位符号")
    private String code;

    @Schema(description = "单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("单位名称")
    private String name;

    @Schema(description = "维度", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "维度", converter = DictConvert.class)
    @DictFormat("mes_unit_category") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String category;

    @Schema(description = "基准单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("基准单位")
    private Boolean base;

    @Schema(description = "换算率", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("换算率")
    private BigDecimal ratio;

    @Schema(description = "保留小数位数")
    @ExcelProperty("保留小数位数")
    private Integer precision;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
