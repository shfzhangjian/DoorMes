package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 检验记录挂接或切换标准 Request VO")
@Data
public class QmsInspectionStandardSelectReqVO {

    @Schema(description = "检验记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验记录ID不能为空")
    private Long id;

    @Schema(description = "选择的检验标准ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验标准ID不能为空")
    private Long standardId;

    @Schema(description = "复检时选择的标准检验项目ID集合；非复检不传则使用标准全部项目")
    private List<Long> selectedStandardItemIds;

    @Schema(description = "选择或切换原因")
    @Size(max = 300, message = "标准选择原因不能超过300个字符")
    private String reason;
}
