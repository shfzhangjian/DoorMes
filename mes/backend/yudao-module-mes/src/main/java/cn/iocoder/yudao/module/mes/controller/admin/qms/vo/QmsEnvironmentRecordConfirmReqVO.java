package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 温湿度每日确认 Request VO")
@Data
public class QmsEnvironmentRecordConfirmReqVO {

    @Schema(description = "车间编码；字典 mes_qms_environment_workshop", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "车间不能为空")
    private String workshopCode;

    @Schema(description = "车间名称快照")
    private String workshopName;

    @Schema(description = "记录日期 yyyy-MM-dd", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记录日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    @Schema(description = "认证确认人用户ID")
    private Long confirmerId;

    @Schema(description = "认证确认人用户名")
    private String confirmerUsername;

    @Schema(description = "认证确认人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认人不能为空")
    private String confirmerName;
}
