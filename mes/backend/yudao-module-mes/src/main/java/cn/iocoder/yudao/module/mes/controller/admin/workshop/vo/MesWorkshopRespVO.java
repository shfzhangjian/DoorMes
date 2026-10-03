package cn.iocoder.yudao.module.mes.controller.admin.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - MES车间产线定义 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesWorkshopRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "136")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "父节点ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "26993")
    @ExcelProperty("父节点ID")
    private Long parentId;

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("编号")
    private String code;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @ExcelProperty("名称")
    private String name;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty(value = "节点类型", converter = DictConvert.class)
    @DictFormat("mes_workshop_type") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer type;

    @Schema(description = "负责人")
    @ExcelProperty("负责人")
    private String manager;

    @Schema(description = "面积(㎡)")
    @ExcelProperty("面积(㎡)")
    private BigDecimal area;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
