package cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 计划用销售订单保存 Request VO")
@Data
public class PlanSaleOrderSaveReqVO {

    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "销售订单号")
    private String orderNo;

    @Schema(description = "ERP编号")
    private String erpNo;

    @Schema(description = "订单行号")
    private String orderLineNo;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "客户名称")
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @Schema(description = "产品 ID")
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @Schema(description = "产品编码")
    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    @Schema(description = "产品名称")
    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @Schema(description = "产品规格")
    private String productSpec;

    @Schema(description = "生产料号物料 ID")
    private Long materialId;

    @Schema(description = "生产料号")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "尺寸规格编码")
    private String sizeSpec;

    @Schema(description = "尺寸规格名称")
    private String sizeName;

    @Schema(description = "订单数量")
    @NotNull(message = "订单数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "已排产数量")
    private BigDecimal plannedQty;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位符号")
    private String unitCode;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "交货日期")
    @NotNull(message = "交货日期不能为空")
    private LocalDate deliveryDate;

    @Schema(description = "备注")
    private String remark;

}
