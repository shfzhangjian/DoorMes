package cn.iocoder.yudao.module.mes.controller.admin.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 生产工单保存 Request VO")
@Data
public class MesWorkOrderSaveReqVO {

    @Schema(description = "主键ID", example = "5001")
    private Long id;

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO_MTS_01_A")
    @NotBlank(message = "工单编号不能为空")
    private String workOrderNo;

    @Schema(description = "工单类型: MTS(备库), MTO(订单), NPI(试产), URG(加急), REP(返工)", example = "MTS")
    private String orderType;

    // ========== 来源信息 ==========
    @Schema(description = "关联生产计划ID", example = "8001")
    private Long planId;

    @Schema(description = "关联生产计划编号（冗余）", example = "PLn_202602_01")
    private String planNo; // ➕ 新增

    @Schema(description = "关联销售订单ID", example = "7001")
    private Long saleOrderId;

    @Schema(description = "关联销售订单编号（冗余）", example = "SO_2026_999")
    private String saleOrderNo; // ➕ 新增

    // ========== 产品信息 ==========
    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @Schema(description = "产品编码（冗余）", example = "M_SLIT_ROLL")
    private String productCode; // ➕ 新增

    @Schema(description = "产品名称（冗余）", example = "分切子卷_1000m")
    private String productName; // ➕ 新增

    @Schema(description = "产品规格（冗余）", example = "1000m*50um")
    private String productSpec; // ➕ 新增

    // ========== 生产资源 ==========
    @Schema(description = "主生产车间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "生产车间ID不能为空")
    private Long workshopId;

    @Schema(description = "主生产车间名称（冗余）", example = "一号车间")
    private String workshopName; // ➕ 新增

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "202")
    @NotNull(message = "工艺路线ID不能为空")
    private Long routeId;

    @Schema(description = "工艺路线编码（冗余）", example = "RT_FILM_STD")
    private String routeCode; // ➕ 新增

    @Schema(description = "工艺路线名称（冗余）", example = "标准薄膜工艺")
    private String routeName; // ➕ 新增

    // ========== 生产参数 ==========
    @Schema(description = "生产批次号 (Lot Number)", example = "LOT_20260216_01")
    private String lotNo;

    @Schema(description = "排产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.0")
    @NotNull(message = "排产数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "计量单位", example = "kg")
    private String unit;

    @Schema(description = "要求完成日期")
    private LocalDate requestDate;

    @Schema(description = "状态", example = "PENDING")
    private String status;

    @Schema(description = "排程优先级", example = "10")
    private Integer priority;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "预占流水号-始")
    private Integer seqStart;

    @Schema(description = "预占流水号-止")
    private Integer seqEnd;
}
