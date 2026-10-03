package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 配料报工任务列表 Request VO")
@Data
public class HcFormulaReportTaskPageReqVO {

    @Schema(description = "任务状态页签：ALL/PENDING/IN_PROGRESS/COMPLETED")
    private String taskStatus;

    @Schema(description = "任务编号关键字")
    private String taskKeyword;

    @Schema(description = "产品关键字")
    private String productKeyword;

    @Schema(description = "母料料号关键字")
    private String motherMaterialKeyword;

    @Schema(description = "母料型号关键字")
    private String motherModelKeyword;
}
