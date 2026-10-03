package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HcAdhesiveGlueBoardStockImportExcelVO {

    @ExcelProperty("辅料类别编码")
    private String accessoryCategory;

    @ExcelProperty("辅料类别名称")
    private String accessoryCategoryName;

    @ExcelProperty("辅料料号")
    private String glueBoardMaterialCode;

    @ExcelProperty("辅料名称")
    private String glueBoardMaterialName;

    @ExcelProperty("胶板型号")
    private String glueBoardModel;

    @ExcelProperty("胶板批号")
    private String glueBoardBatchNo;

    @ExcelProperty("来源仓库编码")
    private String sourceWarehouseCode;

    @ExcelProperty("来源仓库名称")
    private String sourceWarehouseName;

    @ExcelProperty("边库编码")
    private String edgeWarehouseCode;

    @ExcelProperty("边库名称")
    private String edgeWarehouseName;

    @ExcelProperty("领料起位置(m)")
    private BigDecimal receiveStartPosition;

    @ExcelProperty("领料长度(m)")
    private BigDecimal receiveLength;

    @ExcelProperty("库存口径编码")
    private String stockMeasureMode;

    @ExcelProperty("库存口径名称")
    private String stockMeasureModeName;

    @ExcelProperty("领料数量")
    private BigDecimal receiveCount;

    @ExcelProperty("已分配长度(m)")
    private BigDecimal usedLength;

    @ExcelProperty("已分配数量")
    private BigDecimal usedCount;

    @ExcelProperty("损耗长度(m)")
    private BigDecimal lossLength;

    @ExcelProperty("损耗数量")
    private BigDecimal lossCount;

    @ExcelProperty("可用起位置(m)")
    private BigDecimal availableStartPosition;

    @ExcelProperty("当前可用长度(m)")
    private BigDecimal availableLength;

    @ExcelProperty("当前可用数量")
    private BigDecimal availableCount;

    @ExcelProperty("寿命口径编码")
    private String lifetimeMode;

    @ExcelProperty("寿命口径名称")
    private String lifetimeModeName;

    @ExcelProperty("寿命上限(m)")
    private BigDecimal lifetimeLimitLength;

    @ExcelProperty("寿命上限(次)")
    private BigDecimal lifetimeLimitCount;

    @ExcelProperty("寿命已用(m)")
    private BigDecimal lifeUsedLength;

    @ExcelProperty("寿命已用(次)")
    private BigDecimal lifeUsedCount;

    @ExcelProperty("库存状态编码")
    private String stockStatus;

    @ExcelProperty("库存状态名称")
    private String stockStatusName;

    @ExcelProperty("质量状态编码")
    private String qualityStatus;

    @ExcelProperty("质量状态名称")
    private String qualityStatusName;

    @ExcelProperty("送检时间")
    private String inspectionSubmitTime;

    @ExcelProperty("最新检验ID")
    private Long latestInspectionId;

    @ExcelProperty("最新检验单")
    private String latestInspectionNo;

    @ExcelProperty("最新判定")
    private String latestInspectionResult;

    @ExcelProperty("领料人ID")
    private Long receiverId;

    @ExcelProperty("领料人")
    private String receiverName;

    @ExcelProperty("领料日期")
    private String receiveDate;

    @ExcelProperty("领料时间")
    private String receiveTime;

    @ExcelProperty("打印次数")
    private Integer printCount;

    @ExcelProperty("最近打印时间")
    private String printTime;

    @ExcelProperty("ERP移库单号")
    private String erpTransferNo;

    @ExcelProperty("移库数量")
    private BigDecimal transferQty;

    @ExcelProperty("移库单位")
    private String transferUnit;

    @ExcelProperty("拆包量")
    private BigDecimal unpackQty;

    @ExcelProperty("拆包单位")
    private String unpackUnit;

    @ExcelProperty("ERP同步编码")
    private String erpTransferStatus;

    @ExcelProperty("ERP同步名称")
    private String erpTransferStatusName;

    @ExcelProperty("ERP同步时间")
    private String erpTransferTime;

    @ExcelProperty("ERP同步消息")
    private String erpTransferMessage;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("扩展JSON")
    private String extraJson;
}
