package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR库存处置 Request VO")
@Data
public class QmsNcRecordStockDisposeReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "库存处置状态")
    private String stockDisposeStatus;

    @Schema(description = "执行人明确确认已选母卷/加工段挑选合格")
    private Boolean confirmPickQualified;

    @Schema(description = "库存处置结果/确认说明")
    private String stockResult;

    @Schema(description = "办理意见")
    private String opinion;

    @Schema(description = "处置结果附件关联")
    private List<QmsNcRelationReqVO> relations;
}
