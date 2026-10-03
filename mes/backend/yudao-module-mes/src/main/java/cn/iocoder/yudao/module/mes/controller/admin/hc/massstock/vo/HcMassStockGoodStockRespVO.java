package cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 量产备货库存良品库存明细 Response VO")
@Data
public class HcMassStockGoodStockRespVO {

    @Schema(description = "成品库存ID")
    private Long id;

    @Schema(description = "库存编号")
    private String stockNo;

    @Schema(description = "外箱号")
    private String outerBoxNo;

    @Schema(description = "内单元号")
    private String innerUnitNo;

    @Schema(description = "片号")
    private String sliceBatchNo;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "母卷段号")
    private String batchNo;

    @Schema(description = "数量")
    private Integer qty;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "货架")
    private String warehouseName;

    @Schema(description = "货位编码")
    private String locationCode;

    @Schema(description = "货位")
    private String locationName;

    @Schema(description = "入库时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inboundTime;

}
