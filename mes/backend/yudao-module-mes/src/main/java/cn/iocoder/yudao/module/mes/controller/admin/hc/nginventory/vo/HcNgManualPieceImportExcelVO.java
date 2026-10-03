package cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/** 分切压槽不合格品历史片 Excel 导入行。 */
@Data
@ExcelIgnoreUnannotated
public class HcNgManualPieceImportExcelVO {

    @ExcelProperty(value = "段批次", index = 0)
    private String segmentBatchNo;

    @ExcelProperty(value = "片号", index = 1)
    private String pieceNo;

    @ExcelProperty(value = "工序（分切/压槽）", index = 2)
    private String processType;

    @ExcelProperty(value = "来源批号", index = 3)
    private String sourceBatchNo;

    @ExcelProperty(value = "型号", index = 4)
    private String modelNo;

    @ExcelProperty(value = "垫型（黑垫/白垫）", index = 5)
    private String padType;

    @ExcelProperty(value = "NG原因", index = 6)
    private String defectSummary;

    @ExcelProperty(value = "入库类型（普通不合格品/冻结品）", index = 7)
    private String storageTarget;

    @ExcelProperty(value = "补录原因", index = 8)
    private String backfillReason;

    @ExcelProperty(value = "备注", index = 9)
    private String remark;

    @ExcelProperty(value = "料号（选填）", index = 10)
    private String materialCode;
}
