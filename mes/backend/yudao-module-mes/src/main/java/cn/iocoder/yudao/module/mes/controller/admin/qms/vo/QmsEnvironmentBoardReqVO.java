package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "管理后台 - 温湿度录入看板查询 Request VO")
@Data
public class QmsEnvironmentBoardReqVO {

    @Schema(description = "车间编码；字典 mes_qms_environment_workshop", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "车间不能为空")
    private String workshopCode;

    @Schema(description = "车间名称快照")
    private String workshopName;

    @Schema(description = "记录月份 yyyy-MM", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08")
    @NotBlank(message = "记录月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "记录月份格式必须为 yyyy-MM")
    private String recordMonth;
}
