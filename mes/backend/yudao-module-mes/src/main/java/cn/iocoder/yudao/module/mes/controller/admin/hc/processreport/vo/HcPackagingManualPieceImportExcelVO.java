package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 成品包装历史片批量导入 Excel 行。
 */
@Data
@ExcelIgnoreUnannotated
public class HcPackagingManualPieceImportExcelVO {

    @ExcelProperty(value = "片号", index = 0)
    private String sliceBatchNo;

    @ExcelProperty(value = "分段批号", index = 1)
    private String segmentBatchNo;

    @ExcelProperty(value = "产品型号", index = 2)
    private String modelCode;

    @ExcelProperty(value = "产品料号", index = 3)
    private String materialCode;

    @ExcelProperty(value = "生产日期（yyyy-MM-dd）", index = 4)
    private String productionDate;

    @ExcelProperty(value = "有效期（可留空自动计算）", index = 5)
    private String expiryDate;

    @ExcelProperty(value = "裁切FQC结果（OK/NG）", index = 6)
    private String inspectionResult;

    @ExcelProperty(value = "COA送检结果（OK/NG）", index = 7)
    private String coaInspectionResult;

    @ExcelProperty(value = "补录原因", index = 8)
    private String backfillReason;

    @ExcelProperty(value = "备注", index = 9)
    private String remark;

}
