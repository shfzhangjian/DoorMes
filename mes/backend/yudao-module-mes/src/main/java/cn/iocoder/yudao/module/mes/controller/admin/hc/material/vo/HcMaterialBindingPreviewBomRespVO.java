package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 物料工艺用料清单预览 Response VO")
@Data
public class HcMaterialBindingPreviewBomRespVO {

    @Schema(description = "清单ID")
    private Long bomId;

    @Schema(description = "清单编码")
    private String bomCode;

    @Schema(description = "清单名称")
    private String bomName;

    @Schema(description = "清单类型")
    private String bomType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "适用物料ID")
    private Long productMaterialId;

    @Schema(description = "适用物料编码")
    private String productMaterialCode;

    @Schema(description = "适用工艺路线ID")
    private Long routeId;

    @Schema(description = "适用工艺路线编码")
    private String routeCode;

    @Schema(description = "物料是否匹配")
    private Boolean materialMatched;

    @Schema(description = "工艺路线是否匹配")
    private Boolean routeMatched;

    @Schema(description = "是否命中至少一条明细")
    private Boolean matched;

    @Schema(description = "命中的明细数")
    private Integer matchedItemCount;

    @Schema(description = "明细列表")
    private List<HcMaterialBindingPreviewBomItemRespVO> bomItems;

}
