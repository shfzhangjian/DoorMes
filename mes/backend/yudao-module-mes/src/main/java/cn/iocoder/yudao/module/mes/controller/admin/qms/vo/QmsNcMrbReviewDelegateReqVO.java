package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - NCR会签办理委托 Request VO")
@Data
public class QmsNcMrbReviewDelegateReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "会签记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会签记录ID不能为空")
    private Long reviewId;

    @Schema(description = "被委托代办人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "被委托代办人不能为空")
    private Long delegateUserId;

    @Schema(description = "被委托代办人姓名")
    private String delegateUserName;

}
