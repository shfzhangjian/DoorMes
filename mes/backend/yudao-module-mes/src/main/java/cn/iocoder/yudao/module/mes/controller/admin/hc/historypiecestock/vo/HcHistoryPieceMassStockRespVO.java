package cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 历史片量产备货库存 Response VO")
@Data
public class HcHistoryPieceMassStockRespVO {

    @Schema(description = "汇总行唯一键", example = "HC-001|SEG-20260827-01")
    private String rowKey;

    @Schema(description = "型号", example = "HC-001")
    private String modelCode;

    @Schema(description = "分段批号", example = "SEG-20260827-01")
    private String segmentBatchNo;

    @Schema(description = "良品库存")
    private BigDecimal goodStockQty;

    @Schema(description = "最早生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate earliestProductionDate;

    @Schema(description = "最晚生产日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate latestProductionDate;

    @Schema(description = "备注")
    private String remark;

}
