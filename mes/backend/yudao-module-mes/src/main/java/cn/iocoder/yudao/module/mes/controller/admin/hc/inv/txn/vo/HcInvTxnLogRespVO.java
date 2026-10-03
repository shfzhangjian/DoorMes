package cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 库存流水 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcInvTxnLogRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "交易单号")
    @ExcelProperty("单据号")
    private String txnNo;

    @Schema(description = "交易类型")
    @ExcelProperty("交易类型")
    private String txnType;

    @Schema(description = "交易时间")
    @ExcelProperty("交易时间")
    private LocalDateTime txnTime;

    @Schema(description = "仓库编码")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "库位编码")
    @ExcelProperty("库位编码")
    private String locationCode;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "型号")
    @ExcelProperty("型号")
    private String modelNo;

    @Schema(description = "批次号")
    @ExcelProperty("批次号")
    private String batchNo;

    @Schema(description = "交易数量")
    @ExcelProperty("交易数量")
    private BigDecimal txnQty;

    @Schema(description = "交易前在库数量")
    @ExcelProperty("交易前在库")
    private BigDecimal beforeQty;

    @Schema(description = "交易后在库数量")
    @ExcelProperty("交易后在库")
    private BigDecimal afterQty;

    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String uom;

    @Schema(description = "来源单据类型")
    private String refDocType;

    @Schema(description = "来源单据ID")
    private Long refDocId;

    @Schema(description = "操作人姓名")
    @ExcelProperty("操作人")
    private String creatorName;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
