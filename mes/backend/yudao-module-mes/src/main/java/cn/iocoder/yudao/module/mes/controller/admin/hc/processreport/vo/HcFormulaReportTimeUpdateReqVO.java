package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 配料报工时间修改 Request VO")
@Data
public class HcFormulaReportTimeUpdateReqVO {

    @NotNull(message = "报工记录不能为空")
    private Long id;

    @Schema(description = "报工日期", example = "2026-07-11")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @Schema(description = "开始时间", example = "2026-07-11 08:30:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-07-11 16:30:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

}
