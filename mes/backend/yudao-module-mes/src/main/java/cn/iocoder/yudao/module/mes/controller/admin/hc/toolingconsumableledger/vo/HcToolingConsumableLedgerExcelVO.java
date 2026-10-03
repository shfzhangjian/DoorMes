package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HcToolingConsumableLedgerExcelVO {

    @ExcelProperty("领用ID")
    private Long ledgerId;

    @ExcelProperty("耗材种类编码")
    private String consumableType;

    @ExcelProperty("耗材种类")
    private String consumableTypeName;

    @ExcelProperty("工序编码")
    private String processCode;

    @ExcelProperty("工序")
    private String processName;

    @ExcelProperty("型号")
    private String model;

    @ExcelProperty("耗材批次号")
    private String batchNo;

    @ExcelProperty("ERP料号")
    private String erpMaterialCode;

    @ExcelProperty("领用量")
    private BigDecimal receiveQty;

    @ExcelProperty("已消耗")
    private BigDecimal consumedQty;

    @ExcelProperty("当前剩余量")
    private BigDecimal balanceQty;

    @ExcelProperty("计量单位")
    private String uom;

    @ExcelProperty("领用时间")
    private String receiveTime;

    @ExcelProperty("领用人ID")
    private Long receiverId;

    @ExcelProperty("领用人")
    private String receiverName;

    @ExcelProperty("使用状态编码")
    private String usageStatus;

    @ExcelProperty("使用状态")
    private String usageStatusName;

    @ExcelProperty("消耗ID")
    private Long consumeId;

    @ExcelProperty("消耗耗材种类编码")
    private String consumeConsumableType;

    @ExcelProperty("消耗耗材种类")
    private String consumeConsumableTypeName;

    @ExcelProperty("消耗工序编码")
    private String consumeProcessCode;

    @ExcelProperty("消耗工序")
    private String consumeProcessName;

    @ExcelProperty("消耗型号")
    private String consumeModel;

    @ExcelProperty("消耗耗材批次号")
    private String consumeBatchNo;

    @ExcelProperty("消耗量")
    private BigDecimal consumeQty;

    @ExcelProperty("消耗时间")
    private String consumeTime;

    @ExcelProperty("计划号")
    private String planNo;

    @ExcelProperty("生产批次号")
    private String productionBatchNo;

    @ExcelProperty("备注")
    private String remark;
}
