package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class QmsYieldTargetConfigExcelVO {

    @ExcelProperty("产品型号")
    private String modelCode;

    @ExcelProperty("工序编码")
    private String processCode;

    @ExcelProperty("工序名称")
    private String processName;

    @ExcelProperty("计量单位")
    private String measureUnit;

    @ExcelProperty("目标类型")
    private String targetTypeName;

    @ExcelProperty("母卷分段数")
    private String segmentCountName;

    @ExcelProperty("理论产量")
    private BigDecimal targetQualifiedQty;

    @ExcelProperty("状态")
    private String statusName;

    @ExcelProperty("排序")
    private Integer sort;

    @ExcelProperty("备注")
    private String remark;
}
