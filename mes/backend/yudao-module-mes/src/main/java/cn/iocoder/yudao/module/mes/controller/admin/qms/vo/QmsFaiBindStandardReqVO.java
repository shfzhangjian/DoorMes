package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - FAI检验单绑定检验标准 Request VO")
@Data
public class QmsFaiBindStandardReqVO {

    @Schema(description = "FAI检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FAI检验单ID不能为空")
    private Long id;

    @Schema(description = "检验标准ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验标准ID不能为空")
    private Long standardId;

    @Schema(description = "复检时选择的标准检验项目ID集合；非复检不传则使用标准全部项目")
    private List<Long> selectedStandardItemIds;

    @Schema(description = "标准与送检物料、型号或工段不完全匹配时的选择原因")
    @Size(max = 300, message = "标准选择原因不能超过300个字符")
    private String overrideReason;
}
