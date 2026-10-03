package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 发货成品检验客户批号对齐保存 Request VO")
@Data
public class QmsFgShippingFqcAlignmentSaveReqVO {

    @Schema(description = "FQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FQC主单ID不能为空")
    private Long fqcId;

    @NotEmpty(message = "客户批号对齐明细不能为空")
    private List<@Valid Detail> details;

    @Data
    public static class Detail {

        @Schema(description = "发货通知单计划明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "发货通知单计划明细ID不能为空")
        private Long id;

        @Schema(description = "发货配货明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "实际片号来源不能为空")
        private Long shippingPickItemId;
    }
}
