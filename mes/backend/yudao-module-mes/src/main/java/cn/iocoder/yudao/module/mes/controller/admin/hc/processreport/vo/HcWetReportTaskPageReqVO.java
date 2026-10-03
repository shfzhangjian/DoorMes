package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 湿法报工任务列表 Request VO")
@Data
public class HcWetReportTaskPageReqVO {

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

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "实际生产日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;
}
