package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料主数据 Select Option Response VO")
@Data
public class HcMaterialSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "物料编码")
    private String code;

    @Schema(description = "物料状态")
    private Integer status;

}