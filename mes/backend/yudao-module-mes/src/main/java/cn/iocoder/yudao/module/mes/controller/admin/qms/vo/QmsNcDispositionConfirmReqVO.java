package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR 终审处置范围确认 Request VO")
@Data
public class QmsNcDispositionConfirmReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "范围层级：MOTHER_BATCH/SEGMENT/PIECE，按来源工序校验", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择处置片号层级")
    private String scopeLevel;

    @Schema(description = "选中的候选对象 Key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择需要处置的片号")
    private List<String> selectedObjectKeys;

    @Schema(description = "片号处置说明/备注")
    @Valid
    private List<ScopeRemark> scopeRemarks;

    @Schema(description = "处置执行人 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择处置执行人")
    private Long executionUserId;

    @Schema(description = "处置执行人名称")
    private String executionUserName;

    @Schema(description = "处置执行分派通知人ID列表")
    private List<Long> dispositionNotifyUserIds;

    @Schema(description = "处置执行分派通知人名称列表")
    private List<String> dispositionNotifyUserNames;

    @Schema(description = "范围确认备注")
    @Size(max = 1000, message = "范围确认备注不能超过1000个字符")
    private String remark;

    @Schema(description = "改切目标尺寸")
    @Size(max = 128, message = "改切目标尺寸不能超过128个字符")
    private String recutTargetSize;

    @Schema(description = "改切尺寸公差")
    @Size(max = 128, message = "改切尺寸公差不能超过128个字符")
    private String recutTolerance;

    @Schema(description = "改切指令数量")
    private BigDecimal recutQty;

    @Schema(description = "特采理由")
    @Size(max = 1000, message = "特采理由不能超过1000个字符")
    private String concessionReason;

    @Data
    public static class ScopeRemark {

        @Schema(description = "候选对象 Key", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "片号对象Key不能为空")
        private String objectKey;

        @Schema(description = "该片号对应的处置方式")
        private String dispositionType;

        @Schema(description = "处置说明/备注")
        @Size(max = 1000, message = "单个片号处置说明不能超过1000个字符")
        private String remark;
    }
}
