package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 发货客户批号对齐保存 Request VO")
@Data
public class QmsFgShippingAlignmentSaveReqVO {

    @Schema(description = "发货通知单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发货通知单不能为空")
    private Long shippingNoticeId;

    @NotEmpty(message = "客户批号对齐明细不能为空")
    private List<@Valid Detail> details;

    @Data
    public static class Detail {

        @Schema(description = "发货通知单计划明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "发货通知单计划明细不能为空")
        private Long shippingNoticeItemId;

        @Schema(description = "已审核合格的发货配货明细ID；为空时解除当前计划片号的对齐关系")
        private Long shippingPickItemId;
    }
}
