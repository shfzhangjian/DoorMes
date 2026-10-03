package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 批次实例台账年度流水初始化 Request VO")
@Data
public class HcLotRuleCounterInitializeReqVO {

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    @Schema(description = "计数器类型")
    private String counterType;

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不能小于 2000")
    private Integer year;

    @Schema(description = "业务维度键")
    private String bizDimensionKey;

    @Schema(description = "当前已使用计数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "当前计数不能为空")
    @Min(value = 0, message = "当前计数不能小于 0")
    private Integer currentSeq;

    @Schema(description = "最后生成批号")
    private String lastLotNo;

    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "初始化原因不能为空")
    private String reason;
}
