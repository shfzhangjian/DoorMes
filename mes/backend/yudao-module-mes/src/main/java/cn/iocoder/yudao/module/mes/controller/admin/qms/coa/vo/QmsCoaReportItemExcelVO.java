package cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

/** COA 检验结果明细导出行，列顺序与 COA 模板项目配置保持一致。 */
@Data
@ExcelIgnoreUnannotated
public class QmsCoaReportItemExcelVO {

    @ExcelProperty(value = "序号", index = 0)
    private Integer rowNo;
    @ExcelProperty(value = "项目分类", index = 1)
    private String itemGroup;
    @ExcelProperty(value = "ITEM 项目名称", index = 2)
    private String itemNameCn;
    @ExcelProperty(value = "Unit 单位", index = 3)
    private String unit;
    @ExcelProperty(value = "Result 结果", index = 4)
    private String displayValue;
    @ExcelProperty(value = "内控 Spec", index = 5)
    private String specText;
    @ExcelProperty(value = "目标值", index = 6)
    private BigDecimal targetValue;
    @ExcelProperty(value = "COA Spec", index = 7)
    private String coaSpecText;
    @ExcelProperty(value = "测试方法", index = 8)
    private String inspectionMethod;
    @ExcelProperty(value = "取值方式", index = 9)
    private String valueSourceType;
    @ExcelProperty(value = "取值标准", index = 10)
    private String sourceStandard;
    @ExcelProperty(value = "检验工序", index = 11)
    private String sourceProcessName;
    @ExcelProperty(value = "取值项目", index = 12)
    private String sourceInspectionItem;
    @ExcelProperty(value = "取值规则", index = 13)
    private String valueStrategy;
}
