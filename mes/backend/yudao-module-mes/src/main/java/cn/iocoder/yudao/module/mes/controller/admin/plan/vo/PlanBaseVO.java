package cn.iocoder.yudao.module.mes.controller.admin.plan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Data
public class PlanBaseVO {

    @Schema(description = "计划单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PLN-20231027-001")
    @NotNull(message = "计划单号不能为空")
    private String planNo;

    @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "SALE")
    @NotNull(message = "来源不能为空")
    private String fromSource;

    @Schema(description = "关联销售订单ID", example = "10")
    private Long sourceId;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @Schema(description = "产品编码", example = "P-001")
    private String productCode;

    @Schema(description = "产品名称", example = "高性能电机")
    private String productName;

    @Schema(description = "选用BOM版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "BOM版本不能为空")
    private Long bomId;

    @Schema(description = "选用工艺路线", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "计划生产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    @NotNull(message = "计划生产数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "计划开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划开始日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDate;

    @Schema(description = "计划结束日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划结束日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDate;

    @Schema(description = "优先级", example = "10")
    private Integer priority;

    @Schema(description = "状态", example = "CREATED")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;
}
