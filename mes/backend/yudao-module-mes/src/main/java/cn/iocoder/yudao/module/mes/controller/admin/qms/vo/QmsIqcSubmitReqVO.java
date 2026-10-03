package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IQC提交检验 Request VO")
@Data
public class QmsIqcSubmitReqVO {

    @Schema(description = "IQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "IQC主单ID不能为空")
    private Long id;

    @Schema(description = "处置方式，NG时使用")
    private String disposalType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "留样状态：RETAINED/NOT_RETAINED")
    private String retentionStatus;

    @Schema(description = "提交时覆盖的检验项明细")
    @Valid
    private List<QmsIqcSaveReqVO.IqcItem> items;

    @Schema(description = "提交时覆盖的异常处置明细")
    @Valid
    private List<QmsIqcSaveReqVO.IqcAbnormal> abnormals;
}
