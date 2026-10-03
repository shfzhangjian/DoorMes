package cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** COA 报告台账导出行。 */
@Data
@ExcelIgnoreUnannotated
public class QmsCoaReportExcelVO {

    @ExcelProperty(value = "COA编号", index = 0)
    private String coaNo;
    @ExcelProperty(value = "修订版", index = 1)
    private Integer revisionNo;
    @ExcelProperty(value = "模板编码", index = 2)
    private String templateCode;
    @ExcelProperty(value = "模板名称", index = 3)
    private String templateName;
    @ExcelProperty(value = "模板版本", index = 4)
    private String templateVersion;
    @ExcelProperty(value = "客户", index = 5)
    private String customerName;
    @ExcelProperty(value = "客户产品型号", index = 6)
    private String customerProductCode;
    @ExcelProperty(value = "内部产品型号", index = 7)
    private String productModelCode;
    @ExcelProperty(value = "内部物料编码", index = 8)
    private String materialCode;
    @ExcelProperty(value = "物料名称", index = 9)
    private String materialName;
    @ExcelProperty(value = "母批号", index = 10)
    private String productionBatchNo;
    @ExcelProperty(value = "客户批次", index = 11)
    private String customerBatchNo;
    @ExcelProperty(value = "产品尺寸", index = 12)
    private String productSize;
    @ExcelProperty(value = "保质期", index = 13)
    private String shelfLife;
    @ExcelProperty(value = "生产日期", index = 14)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate manufactureDate;
    @ExcelProperty(value = "签发日期", index = 15)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate issueDate;
    @ExcelProperty(value = "报告状态", index = 16)
    private String reportStatus;
    @ExcelProperty(value = "总体判定", index = 17)
    private String overallResult;
    @ExcelProperty(value = "报告项目数", index = 18)
    private Integer reportItemCount;
    @ExcelProperty(value = "待填写项", index = 19)
    private Integer requiredIncompleteCount;
    @ExcelProperty(value = "检验人", index = 20)
    private String inspectorName;
    @ExcelProperty(value = "检验时间", index = 21)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime inspectorTime;
    @ExcelProperty(value = "确认人", index = 22)
    private String confirmerName;
    @ExcelProperty(value = "确认时间", index = 23)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;
    @ExcelProperty(value = "审核人", index = 24)
    private String reviewerName;
    @ExcelProperty(value = "审核时间", index = 25)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    @ExcelProperty(value = "创建时间", index = 26)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @ExcelProperty(value = "备注", index = 27)
    private String remark;
}
