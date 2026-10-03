package cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - OCAP模板 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcOcapTemplateRespVO {

    @Schema(description = "OCAP编码")
    @ExcelProperty("OCAP编码")
    private String ocapCode;

    @Schema(description = "OCAP名称")
    @ExcelProperty("OCAP名称")
    private String ocapName;

    @Schema(description = "业务工序")
    @ExcelProperty("业务工序")
    private String businessStage;

    @Schema(description = "触发项目编码")
    @ExcelProperty("触发项目编码")
    private String triggerItemCode;

    @Schema(description = "触发条件")
    @ExcelProperty("触发条件")
    private String triggerCondition;

    @Schema(description = "处置步骤")
    @ExcelProperty("处置步骤")
    private String actionSteps;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}