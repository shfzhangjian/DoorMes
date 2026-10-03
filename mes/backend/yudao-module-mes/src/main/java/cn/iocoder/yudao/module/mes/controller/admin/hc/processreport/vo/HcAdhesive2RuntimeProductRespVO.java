package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2当前实际产品快照 Response VO")
@Data
public class HcAdhesive2RuntimeProductRespVO {

    @Schema(description = "实际产品型号")
    private String productModel;

    @Schema(description = "实际产品物料 ID")
    private Long materialId;

    @Schema(description = "实际产品料号")
    private String materialCode;

    @Schema(description = "实际产品物料名称")
    private String materialName;

    @Schema(description = "实际产品规格")
    private String specification;

    @Schema(description = "快照来源：CHANGEOVER_PIECE、CHANGEOVER_INSTRUCTION、PLAN")
    private String sourceType;

    @Schema(description = "换型指令 ID")
    private Long changeoverInstructionId;

    @Schema(description = "换型指令号")
    private String changeoverInstructionNo;
}
