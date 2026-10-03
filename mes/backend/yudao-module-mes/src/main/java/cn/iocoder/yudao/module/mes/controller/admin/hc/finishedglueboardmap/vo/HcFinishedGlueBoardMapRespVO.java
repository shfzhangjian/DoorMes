package cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 成品胶板对照表 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcFinishedGlueBoardMapRespVO {

    private Long id;
    private Long productModelId;

    @ExcelProperty("型号")
    private String productModelCode;

    @ExcelProperty("产品型号名称")
    private String productModelName;

    @ExcelProperty("成品规格")
    private String productSpec;

    private String sizeSpec;
    private String sizeName;

    @ExcelProperty("粘胶1胶板")
    private String adhesive1Summary;

    @ExcelProperty("粘胶2胶板")
    private String adhesive2Summary;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;

    private LocalDateTime createTime;

}
