package cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 计划用销售订单 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanSaleOrderRespVO {

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
    private String customerName;

    @Schema(description = "产品 ID")
    private Long productId;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
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
    private BigDecimal quantity;

    @Schema(description = "已排产数量")
    private BigDecimal plannedQty;

    @Schema(description = "欠交数量")
    private BigDecimal remainQty;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位符号")
    private String unitCode;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "交货日期")
    private LocalDate deliveryDate;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "审核人 ID")
    private Long auditorId;

    @Schema(description = "审核人")
    private String auditorName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
