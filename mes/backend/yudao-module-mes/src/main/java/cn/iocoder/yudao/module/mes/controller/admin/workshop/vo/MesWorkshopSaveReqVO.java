package cn.iocoder.yudao.module.mes.controller.admin.workshop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - MES车间产线定义新增/修改 Request VO")
@Data
public class MesWorkshopSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "136")
    private Long id;

    @Schema(description = "父节点ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "26993")
    @NotNull(message = "父节点ID不能为空")
    private Long parentId;

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "编号不能为空")
    private String code;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "节点类型不能为空")
    private Integer type;

    @Schema(description = "负责人")
    private String manager;

    @Schema(description = "面积(㎡)")
    private BigDecimal area;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
