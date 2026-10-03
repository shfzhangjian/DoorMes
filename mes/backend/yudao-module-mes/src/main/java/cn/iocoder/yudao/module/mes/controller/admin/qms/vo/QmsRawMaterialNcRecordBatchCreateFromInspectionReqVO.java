package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 批量从检验单生成原物料不合格处置单 Request VO")
@Data
public class QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO {

    @Schema(description = "检验类型：IQC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验类型不能为空")
    private String inspectionType;

    @Schema(description = "不合格等级")
    private String ncLevel;

    @Schema(description = "责任单位")
    private List<String> responsibleDeptNames;

    @Schema(description = "生成后直接提交")
    private Boolean directSubmit;

    @Schema(description = "直接提交时指定的再次确认人ID")
    private Long contentConfirmUserId;

    @Schema(description = "直接提交时指定的再次确认人名称")
    private String contentConfirmUserName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "检验单明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "检验类型：IQC")
        private String inspectionType;

        @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "检验单ID不能为空")
        private Long inspectionId;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称")
        private String defectName;

        @Schema(description = "不合格数量")
        private BigDecimal defectQty;

        @Schema(description = "不合格说明")
        private String ncDescription;
    }
}
