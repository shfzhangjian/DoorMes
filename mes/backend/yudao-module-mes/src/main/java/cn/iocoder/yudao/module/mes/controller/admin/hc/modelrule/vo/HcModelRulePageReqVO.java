package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 型号编码规则分页 Request VO")
@Data
public class HcModelRulePageReqVO extends PageParam {

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则分类")
    private String ruleCategory;

    @Schema(description = "适用产品层级")
    private String targetLevel;

    @Schema(description = "规则说明")
    private String ruleDesc;

    @Schema(description = "当前生效版本")
    private String effectiveVersion;

    @Schema(description = "状态")
    private Integer status;

}