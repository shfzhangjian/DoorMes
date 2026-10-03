package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR提交MRB Request VO")
@Data
public class QmsNcRecordSubmitReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "MRB会签明细")
    private List<QmsNcMrbReviewReqVO> reviews;

    @Schema(description = "NCR 再次确认人ID")
    private Long contentConfirmUserId;

    @Schema(description = "NCR 再次确认人名称")
    private String contentConfirmUserName;

    @Schema(description = "提交意见")
    private String opinion;
}
