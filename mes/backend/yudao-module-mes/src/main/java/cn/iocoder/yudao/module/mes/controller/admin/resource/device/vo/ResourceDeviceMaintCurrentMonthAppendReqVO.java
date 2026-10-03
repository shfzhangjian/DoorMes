package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备本月保养追加 Request VO")
@Data
public class ResourceDeviceMaintCurrentMonthAppendReqVO {

    @Schema(description = "执行人")
    private String executor;

    @Valid
    @NotEmpty(message = "追加设备不能为空")
    private List<Candidate> candidates;

    @Schema(description = "设备本月保养追加明细")
    @Data
    public static class Candidate {

        @NotNull(message = "设备不能为空")
        private Long deviceId;

        @NotNull(message = "保养标准不能为空")
        private Long standardId;

        @Schema(description = "本次应检/应保养日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate taskDueDate;

    }

}
