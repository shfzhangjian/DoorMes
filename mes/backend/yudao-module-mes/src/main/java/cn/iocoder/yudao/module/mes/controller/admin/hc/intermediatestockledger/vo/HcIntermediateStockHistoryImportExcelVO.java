package cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HcIntermediateStockHistoryImportExcelVO {

    @ExcelProperty("库存ID")
    private Long stockId;

    @ExcelProperty("来源计划号")
    private String sourcePlanNo;

    @ExcelProperty("工序来源类型")
    private String sourceType;

    @ExcelProperty("工序序号")
    private Integer opSeq;

    @ExcelProperty("工序编码")
    private String opCode;

    @ExcelProperty("工序名称")
    private String opName;

    @ExcelProperty("中间品批号")
    private String batchNo;

    @ExcelProperty("来源批号")
    private String sourceBatchNo;

    @ExcelProperty("母卷批次号")
    private String sourceParentBatchNo;

    @ExcelProperty("物料编码")
    private String materialCode;

    @ExcelProperty("物料名称")
    private String materialName;

    @ExcelProperty("型号")
    private String modelNo;

    @ExcelProperty("尺寸规格")
    private String specSize;

    @ExcelProperty("段位编码")
    private String segmentCode;

    @ExcelProperty("段位名称")
    private String segmentName;

    @ExcelProperty("厚度/mm")
    private BigDecimal thickness;

    @ExcelProperty("在库数量")
    private BigDecimal onHandQty;

    @ExcelProperty("可用数量")
    private BigDecimal availableQty;

    @ExcelProperty("可利库数量")
    private BigDecimal shareableQty;

    @ExcelProperty("冻结数量")
    private BigDecimal frozenQty;

    @ExcelProperty("单位")
    private String uom;

    @ExcelProperty("质量状态")
    private String qualityStatus;

    @ExcelProperty("业务状态")
    private String bizStatus;

    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("库位编码")
    private String locationCode;

    @ExcelProperty("库位名称")
    private String locationName;

    @ExcelProperty("生产日期")
    private String productionDate;

    @ExcelProperty("失效日期")
    private String expiryDate;

    @ExcelProperty("最近过账时间")
    private String lastTxnTime;

    @ExcelProperty("历史流水号")
    private String lastTxnNo;

    @ExcelProperty("备注")
    private String businessRemark;

}
