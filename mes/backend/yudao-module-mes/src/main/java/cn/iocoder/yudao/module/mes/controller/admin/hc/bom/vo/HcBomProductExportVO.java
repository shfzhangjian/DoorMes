package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcBomProductExportVO {

    @ExcelProperty({"成品型号", "型号"})
    private String productModelCode;

    @ExcelProperty({"成品规格（尺寸）", "成品规格"})
    private String productSpec;

    @ExcelProperty({"成品料号", "分切工序报工"})
    private String productMaterialCode;

    @ExcelProperty({"中间品", "磨皮工序报工"})
    private String roughGrindingMaterialCode;

    @ExcelProperty({"中间品", "粘胶1报工料号"})
    private String adhesive1IntermediateMaterialCode;

    @ExcelProperty({"辅料料号", "粘胶1使用胶板料号"})
    private String adhesive1AuxMaterialCode;

    @ExcelProperty({"辅料料号", "粘胶2使用胶板料号"})
    private String adhesive2AuxMaterialCode;

}
