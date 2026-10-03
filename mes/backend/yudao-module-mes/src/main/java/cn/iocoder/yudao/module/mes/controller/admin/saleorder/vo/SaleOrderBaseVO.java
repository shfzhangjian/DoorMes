package cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 销售订单 Base VO，提供给 CreateReqVO、UpdateReqVO、RespVO 继承
 */
@Data
public class SaleOrderBaseVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SO-20231027-001")
    @NotNull(message = "订单编号不能为空")
    private String orderNo;

    @Schema(description = "客户名称", example = "华为技术有限公司")
    private String customerName;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @Schema(description = "产品编码", example = "P-001")
    private String productCode;

    @Schema(description = "产品名称", example = "高性能电机")
    private String productName;

    @Schema(description = "订单数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "订单数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "pcs")
    private String unit;

    @Schema(description = "交货日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交货日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate deliveryDate;

    @Schema(description = "状态", example = "PENDING")
    private String status;

    @Schema(description = "备注", example = "加急订单")
    private String remark;

    @Schema(description = "排序", example = "10")
    private Integer sort;
}
