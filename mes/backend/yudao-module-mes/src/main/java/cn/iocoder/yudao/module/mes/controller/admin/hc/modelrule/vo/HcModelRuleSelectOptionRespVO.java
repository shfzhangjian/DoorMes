package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 型号编码规则 Select Option Response VO")
@Data
public class HcModelRuleSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "规则编码")
    private String code;

    @Schema(description = "状态")
    private Integer status;

}