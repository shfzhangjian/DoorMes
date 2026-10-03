package cn.iocoder.yudao.module.mes.controller.admin.workshop.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES车间产线定义列表 Request VO")
@Data
public class MesWorkshopListReqVO {

    @Schema(description = "父节点ID", example = "26993")
    private Long parentId;

    @Schema(description = "编号")
    private String code;

    @Schema(description = "名称", example = "芋艿")
    private String name;

    @Schema(description = "节点类型", example = "2")
    private Integer type;

    @Schema(description = "负责人")
    private String manager;

    @Schema(description = "面积(㎡)")
    private BigDecimal area;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
