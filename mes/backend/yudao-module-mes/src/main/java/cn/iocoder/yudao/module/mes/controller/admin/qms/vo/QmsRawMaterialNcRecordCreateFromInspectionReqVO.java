package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 从检验单生成原物料不合格处置单 Request VO")
@Data
public class QmsRawMaterialNcRecordCreateFromInspectionReqVO {

    @Schema(description = "检验类型：IQC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验类型不能为空")
    private String inspectionType;

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "检验单ID不能为空")
    private Long inspectionId;
}
