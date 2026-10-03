package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - HC 研发型号编码预览 Request VO")
@Data
public class HcResearchTaskCodePreviewReqVO {

    @Schema(description = "当前记录ID，修改时用于排除自身")
    private Long id;

    @Schema(description = "型号类型编码：RD黑垫、RA黑垫", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "型号类型不能为空")
    private String productClassCode;

    @Schema(description = "基准配方编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "基准配方编码不能为空")
    private String baseFormulaCode;

    @Schema(description = "湿法工艺编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "湿法工艺编码不能为空")
    private String wetProcessCode;

    @Schema(description = "磨皮工艺编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "磨皮工艺编码不能为空")
    private String grindingProcessCode;

    @Schema(description = "后工艺编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "后工艺编码不能为空")
    private String postProcessCode;

}
