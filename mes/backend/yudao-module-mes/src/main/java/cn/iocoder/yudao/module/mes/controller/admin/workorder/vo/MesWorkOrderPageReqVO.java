package cn.iocoder.yudao.module.mes.controller.admin.workorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产工单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesWorkOrderPageReqVO extends PageParam {

    @Schema(description = "工单编号 (模糊)")
    private String workOrderNo;

    @Schema(description = "工单类型")
    private String orderType;

    // ========== 来源搜索 (新增) ==========
    @Schema(description = "生产计划编号 (模糊)")
    private String planNo;

    @Schema(description = "销售订单编号 (模糊)")
    private String saleOrderNo;

    // ========== 产品搜索 ==========
    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品编码 (模糊)")
    private String productCode;

    @Schema(description = "产品名称 (模糊)")
    private String productName; // ✅ 利用反范式字段

    // ========== 生产属性 ==========
    @Schema(description = "生产批次号")
    private String lotNo;

    @Schema(description = "主生产车间ID")
    private Long workshopId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "要求完成日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] requestDate;

    @Schema(description = "创建时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] createTime;
}
