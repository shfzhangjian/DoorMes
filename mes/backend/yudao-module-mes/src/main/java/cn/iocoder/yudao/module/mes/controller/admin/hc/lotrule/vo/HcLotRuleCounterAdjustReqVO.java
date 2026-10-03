package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 批次实例台账人工设置 Request VO")
@Data
public class HcLotRuleCounterAdjustReqVO {

    @Schema(description = "计数器ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计数器ID不能为空")
    private Long counterId;

    @Schema(description = "调整前当前已使用计数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调整前当前计数不能为空")
    @Min(value = 0, message = "调整前当前计数不能小于 0")
    private Integer oldCurrentSeq;

    @Schema(description = "调整后当前已使用计数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调整后当前计数不能为空")
    @Min(value = 0, message = "调整后当前计数不能小于 0")
    private Integer newCurrentSeq;

    @Schema(description = "调整后最后生成批号")
    private String lastLotNo;

    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}
