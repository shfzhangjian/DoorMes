package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则 Simple Response VO")
@Data
public class HcLotRuleSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "状态")
    private Integer status;

}