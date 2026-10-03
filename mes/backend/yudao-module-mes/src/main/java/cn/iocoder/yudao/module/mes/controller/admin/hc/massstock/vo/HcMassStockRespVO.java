package cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 量产备货库存 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcMassStockRespVO {

    @Schema(description = "行键")
    private String rowKey;

    @Schema(description = "型号")
    @ExcelProperty("型号")
    private String modelCode;

    @Schema(description = "母卷批号")
    @ExcelProperty("母卷批号")
    private String motherBatchNo;

    @Schema(description = "母卷段号")
    @ExcelProperty("母卷段号")
    private String motherSegmentBatchNo;

    @Schema(description = "湿法报工米数")
    @ExcelProperty("湿法（m）")
    private BigDecimal wetMeter;

    @Schema(description = "一次磨皮报工米数")
    @ExcelProperty("一次磨皮（m）")
    private BigDecimal grindingFirstMeter;

    @Schema(description = "二次磨皮报工米数")
    @ExcelProperty("二次磨皮（m）")
    private BigDecimal grindingSecondMeter;

    @Schema(description = "HC-NAP层结果")
    @ExcelProperty("HC-NAP层结果")
    private String hcNapResult;

    @Schema(description = "CS-NAP层结果")
    @ExcelProperty("CS-NAP层结果")
    private String csNapResult;

    @Schema(description = "出货COA结果")
    @ExcelProperty("出货COA")
    private String shippingCoaResult;

    @Schema(description = "SEM结果")
    @ExcelProperty("SEM结果")
    private String semResult;

    @Schema(description = "粘胶1未加工米数")
    @ExcelProperty("粘胶1（m）")
    private BigDecimal adhesive1UnprocessedMeter;

    @Schema(description = "压槽确认片数")
    @ExcelProperty("压槽")
    private BigDecimal pressSlotQty;

    @Schema(description = "粘胶2确认片数")
    @ExcelProperty("粘胶2")
    private BigDecimal adhesive2Qty;

    @Schema(description = "裁切确认片数")
    @ExcelProperty("裁切")
    private BigDecimal cutRoundQty;

    @Schema(description = "裁切确认未检验片数")
    @ExcelProperty("待检验")
    private BigDecimal pendingInspectionQty;

    @Schema(description = "检验合格库存")
    @ExcelProperty("检验合格库存")
    private BigDecimal qualifiedInspectionStockQty;

    @Schema(description = "良品库存")
    @ExcelProperty("良品库存")
    private BigDecimal goodStockQty;

    @Schema(description = "最早生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("最早生产日期")
    private LocalDate earliestProductionDate;

    @Schema(description = "最晚生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("最晚生产日期")
    private LocalDate latestProductionDate;

    @Schema(description = "需备货数量")
    @ExcelProperty("需备货数量")
    private BigDecimal demandStockQty;

    @Schema(description = "订单发货配货数量")
    @ExcelProperty("订单发货配货数量")
    private BigDecimal shippingPickedQty;

    @Schema(description = "订单检验数量")
    @ExcelProperty("订单检验数量")
    private BigDecimal shippingInspectionQty;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "人工维护更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime manualUpdateTime;

}
