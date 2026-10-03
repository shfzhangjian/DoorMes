package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 产品异常事件驳回复检 Request VO")
@Data
public class QmsProductAbnormalEventRejectReqVO {

    @Schema(description = "检验来源类型：FAI、GLUE_BOARD_FAI、CUT_ROUND_FQC、FG_SHIPPING_FQC、OQC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验来源类型不能为空")
    private String sourceType;

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验单ID不能为空")
    private Long inspectionId;

    @Schema(description = "驳回说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "驳回说明不能为空")
    @Size(max = 500, message = "驳回说明不能超过 500 个字符")
    private String rejectReason;

    @Schema(description = "复检项目ID集合；过程首件FAI复检不传，在复检单选择标准后再选择具体项目；FQC/OQC片级选择时传对应片下的项目ID")
    private List<Long> selectedItemIds;

    @Schema(description = "复检范围；ITEM整项、POSITION位置、PIECE具体片/样本")
    private List<QmsDispatchTaskItemSelectionReqVO> selectedScopes;
}
