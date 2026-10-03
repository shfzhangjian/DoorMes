package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 批量从产品异常事件生成 NCR Request VO")
@Data
public class QmsNcRecordBatchCreateFromProductEventReqVO {

    @Schema(description = "产品异常事件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "产品异常事件不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "不合格等级：MINOR、MAJOR、CRITICAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "不合格等级不能为空")
    private String ncLevel;

    @Schema(description = "责任单位名称列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "责任单位不能为空")
    private List<String> responsibleDeptNames;

    @Schema(description = "生成备注")
    private String remark;

    @Schema(description = "是否生成后直接提交")
    private Boolean directSubmit;

    @Schema(description = "直接提交时指定的再次确认人ID")
    private Long contentConfirmUserId;

    @Schema(description = "直接提交时指定的再次确认人名称")
    private String contentConfirmUserName;

    @Schema(description = "产品异常事件生成 NCR 明细")
    @Data
    public static class Item {

        @Schema(description = "产品异常事件来源类型", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "来源类型不能为空")
        private String sourceType;

        @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检验单ID不能为空")
        private Long inspectionId;

        @Schema(description = "不合格数量")
        private BigDecimal defectQty;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称")
        private String defectName;

        @Schema(description = "不良描述")
        private String ncDescription;
    }
}
