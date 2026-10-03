package cn.iocoder.yudao.module.mes.controller.admin.hc.productionrecordrevision.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 生产记录展示修订创建 Request VO")
@Data
public class HcProductionRecordRevisionCreateReqVO {

    @Schema(description = "生产记录模块编码", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "FORMULA")
    @NotBlank(message = "生产记录模块不能为空")
    @Size(max = 32, message = "生产记录模块长度不能超过 32 个字符")
    private String moduleCode;

    @Schema(description = "当前展示行的记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "生产记录编号不能为空")
    private Long recordId;

    @Schema(description = "原始展示快照，用于审计", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "原始展示快照不能为空")
    private Map<String, Object> originalSnapshot;

    @Schema(description = "需覆盖的展示字段", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少修订一个展示字段")
    private Map<String, Object> revisedData;

    @Schema(description = "修订原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修订原因不能为空")
    @Size(max = 500, message = "修订原因长度不能超过 500 个字符")
    private String reviseReason;

}
