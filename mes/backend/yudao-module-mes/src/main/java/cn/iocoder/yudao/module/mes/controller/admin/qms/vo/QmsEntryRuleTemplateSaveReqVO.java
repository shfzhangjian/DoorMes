package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 录入规则模板新增 Request VO")
@Data
public class QmsEntryRuleTemplateSaveReqVO {

    @Schema(description = "模板编号")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过 100 字")
    private String templateName;

    @Schema(description = "项目类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目类型不能为空")
    private String itemType;

    @Schema(description = "位置/录入规则摘要", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "位置/录入规则摘要不能为空")
    @Size(max = 255, message = "位置/录入规则摘要不能超过 255 字")
    private String testFrequencyJudgement;

    @Schema(description = "模板参数JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板参数不能为空")
    private String templateParams;

    @Schema(description = "规则说明")
    @Size(max = 1000, message = "规则说明不能超过 1000 字")
    private String ruleDescription;

    @Schema(description = "取样数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "取样数不能为空")
    @Positive(message = "取样数必须大于 0")
    private Integer sampleSize;
}
