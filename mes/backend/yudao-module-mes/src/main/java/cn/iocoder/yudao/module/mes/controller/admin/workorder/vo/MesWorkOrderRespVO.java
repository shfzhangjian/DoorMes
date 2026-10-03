package cn.iocoder.yudao.module.mes.controller.admin.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 生产工单 Response VO")
@Data
public class MesWorkOrderRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "5001")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO_MTS_01_A")
    @ExcelProperty("工单编号")
    private String workOrderNo;

    @Schema(description = "工单类型", example = "MTS")
    @ExcelProperty(value = "工单类型", converter = DictConvert.class)
    @DictFormat("mes_work_order_type")
    private String orderType;

    // ========== 来源信息 ==========
    @Schema(description = "关联生产计划ID", example = "8001")
    @ExcelProperty("生产计划ID")
    private Long planId;

    @Schema(description = "关联生产计划编号（冗余）", example = "PLn_202602_01")
    @ExcelProperty("生产计划编号")
    private String planNo;

    @Schema(description = "关联销售订单ID", example = "7001")
    @ExcelProperty("销售订单ID")
    private Long saleOrderId;

    @Schema(description = "关联销售订单编号（冗余）", example = "SO_2026_999")
    @ExcelProperty("销售订单号")
    private String saleOrderNo;

    // ========== 产品信息 ==========
    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    @ExcelProperty("产品ID")
    private Long productId;

    @Schema(description = "产品编码（冗余）", example = "M_SLIT_ROLL")
    @ExcelProperty("产品编码")
    private String productCode;

    @Schema(description = "产品名称（冗余）", example = "分切子卷_1000m")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "产品规格（冗余）", example = "1000m*50um")
    @ExcelProperty("产品规格")
    private String productSpec;

    // ========== 生产资源 ==========
    @Schema(description = "主生产车间ID", example = "101")
    @ExcelProperty("车间ID")
    private Long workshopId;

    @Schema(description = "主生产车间名称（冗余）", example = "一号车间")
    @ExcelProperty("车间名称")
    private String workshopName;

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "202")
    @ExcelProperty("工艺路线ID")
    private Long routeId;

    @Schema(description = "工艺路线编码（冗余）", example = "RT_FILM_STD")
    @ExcelProperty("工艺路线编码")
    private String routeCode;

    @Schema(description = "工艺路线名称（冗余）", example = "标准薄膜工艺")
    @ExcelProperty("工艺路线名称")
    private String routeName;

    // ========== 生产参数 ==========
    @Schema(description = "生产批次号", example = "LOT_20260216_01")
    @ExcelProperty("生产批次号")
    private String lotNo;

    @Schema(description = "排产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.0")
    @ExcelProperty("排产数量")
    private BigDecimal quantity;

    @Schema(description = "计量单位", example = "kg")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "要求完成日期")
    @ExcelProperty("要求完成日期")
    private LocalDate requestDate;

    @Schema(description = "排程优先级")
    @ExcelProperty("优先级")
    private Integer priority;

    @Schema(description = "状态", example = "PENDING")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat("mes_order_status")
    private String status;

    // ========== 进度与时间 ==========
    @Schema(description = "实际开工时间")
    @ExcelProperty("实际开工时间")
    private LocalDateTime realStartTime;

    @Schema(description = "实际完工时间")
    @ExcelProperty("实际完工时间")
    private LocalDateTime realEndTime;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
