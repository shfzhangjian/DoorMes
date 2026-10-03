package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 从检验单生成原物料不合格处置单 Response VO")
@Data
public class QmsRawMaterialNcRecordCreateFromInspectionRespVO {

    @Schema(description = "是否已存在")
    private Boolean existed;

    @Schema(description = "检验类型")
    private String inspectionType;

    @Schema(description = "检验单ID")
    private Long inspectionId;

    @Schema(description = "NCR ID")
    private Long ncRecordId;

    @Schema(description = "NCR 单号")
    private String ncNo;
}
