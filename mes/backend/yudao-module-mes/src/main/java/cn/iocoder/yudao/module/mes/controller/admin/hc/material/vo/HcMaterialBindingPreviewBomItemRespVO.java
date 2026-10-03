package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 物料工艺用料明细预览 Response VO")
@Data
public class HcMaterialBindingPreviewBomItemRespVO {

    @Schema(description = "明细ID")
    private Long bomItemId;

    @Schema(description = "行号")
    private Integer lineNo;

    @Schema(description = "投放工序编码")
    private String issueOperationCode;

    @Schema(description = "子件物料ID")
    private Long componentMaterialId;

    @Schema(description = "子件物料编码")
    private String componentMaterialCode;

    @Schema(description = "子件物料名称")
    private String componentMaterialName;

    @Schema(description = "基础用量")
    private BigDecimal baseQty;

    @Schema(description = "损耗率")
    private BigDecimal lossRate;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "用料命中分组编码")
    private String consumeGroupCode;

    @Schema(description = "命中条件JSON")
    private String conditionJson;

    @Schema(description = "是否必发")
    private Boolean requiredFlag;

    @Schema(description = "分组是否命中")
    private Boolean consumeGroupMatched;

    @Schema(description = "条件是否命中")
    private Boolean conditionMatched;

    @Schema(description = "最终是否命中")
    private Boolean matched;

}
