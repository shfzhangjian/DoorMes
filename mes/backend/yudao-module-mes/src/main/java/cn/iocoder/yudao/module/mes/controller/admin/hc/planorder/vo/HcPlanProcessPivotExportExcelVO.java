package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcPlanProcessPivotExportExcelVO {

    @ExcelProperty("计划号")
    private String planNo;

    @ExcelProperty("母批批号")
    private String motherRollBatchNo;

    @ExcelProperty("状态")
    private String planStatus;

    @ExcelProperty("型号")
    private String modelCode;

    @ExcelProperty("料号")
    private String materialCode;

    @ExcelProperty("尺寸")
    private String sizeSpec;

    @ExcelProperty("配料完工")
    private String formulaDoneQty;

    @ExcelProperty("配料未加工")
    private String formulaPendingQty;

    @ExcelProperty("湿法完工")
    private String wetDoneQty;

    @ExcelProperty("湿法未加工")
    private String wetPendingQty;

    @ExcelProperty("磨皮分段批号")
    private String grindingBatchNo;

    @ExcelProperty("磨皮完工")
    private String grindingDoneQty;

    @ExcelProperty("磨皮未加工")
    private String grindingPendingQty;

    @ExcelProperty("粘胶1完工")
    private String adhesive1DoneQty;

    @ExcelProperty("粘胶1未加工")
    private String adhesive1PendingQty;

    @ExcelProperty("分切完工")
    private String slittingDoneQty;

    @ExcelProperty("分切未加工")
    private String slittingPendingQty;

    @ExcelProperty("压槽完工")
    private String pressSlotDoneQty;

    @ExcelProperty("压槽未加工")
    private String pressSlotPendingQty;

    @ExcelProperty("粘胶2完工")
    private String adhesive2DoneQty;

    @ExcelProperty("粘胶2未加工")
    private String adhesive2PendingQty;

    @ExcelProperty("裁切完工")
    private String cutRoundDoneQty;

    @ExcelProperty("裁切未加工")
    private String cutRoundPendingQty;

}
