package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - BOM产品型号下拉选项 Response VO")
@Data
public class HcBomProductModelOptionRespVO {

    @Schema(description = "选项值，产品型号ID")
    private Long value;

    @Schema(description = "选项标签，产品型号编码")
    private String label;

    @Schema(description = "产品型号ID")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "产品型号编码")
    private String code;

    @Schema(description = "产品型号名称")
    private String modelName;

    @Schema(description = "BOM类型")
    private String bomType;

    @Schema(description = "状态")
    private Integer status;

}
