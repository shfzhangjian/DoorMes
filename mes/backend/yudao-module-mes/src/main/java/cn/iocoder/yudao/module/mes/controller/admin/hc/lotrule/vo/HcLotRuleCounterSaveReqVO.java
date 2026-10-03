package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则实例台账新增/修改 Request VO")
@Data
public class HcLotRuleCounterSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    @Schema(description = "计数器类型")
    private String counterType;

    @Schema(description = "业务维度键")
    private String bizDimensionKey;

    @Schema(description = "业务维度上下文 JSON")
    private String bizDimensionJson;

    @Schema(description = "计数键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "计数键不能为空")
    private String counterKey;

    @Schema(description = "重置键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "重置键不能为空")
    private String resetKey;

    @Schema(description = "当前计数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "当前计数不能为空")
    @Min(value = 0, message = "当前计数不能小于 0")
    private Integer currentSeq;

    @Schema(description = "最后生成批号")
    private String lastLotNo;
}
