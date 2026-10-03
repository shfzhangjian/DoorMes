package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 磨皮操作看板加载 Request VO")
@Data
public class HcRoughConsoleBoardReqVO {

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "记录日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;
}
