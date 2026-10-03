package cn.iocoder.yudao.module.mes.controller.admin.workorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 派工细单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesWorkOrderSubPageReqVO extends PageParam {

    @Schema(description = "主工单ID")
    private Long workOrderId;

    @Schema(description = "主工单号 (模糊)")
    private String workOrderNo; // ✅ 常用查询条件

    @Schema(description = "派工细单号 (模糊)")
    private String subOrderNo;

    // ========== 工艺过滤 ==========
    @Schema(description = "工序名称 (模糊)")
    private String processName; // ✅ 例如查所有"模切"任务

    // ========== 资源过滤 ==========
    @Schema(description = "指定工位ID")
    private Long stationId;

    @Schema(description = "工位名称 (模糊)")
    private String stationName;

    @Schema(description = "执行操作人 (模糊)")
    private String operatorUser;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "计划日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] planDate;
}
