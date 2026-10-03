package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;

@Schema(description = "管理后台 - 型号编码规则新增/修改 Request VO")
@Data
public class HcModelRuleSaveReqVO {

    @Schema(description = "规则编码")
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    @Schema(description = "规则名称")
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @Schema(description = "规则分类")
    @NotBlank(message = "规则分类不能为空")
    private String ruleCategory;

    @Schema(description = "适用产品层级")
    @NotBlank(message = "适用产品层级不能为空")
    private String targetLevel;

    @Schema(description = "规则说明")
    private String ruleDesc;

    @Schema(description = "当前生效版本")
    private String effectiveVersion;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "型号规则字段列表")
    private List<HcModelRuleItemDO> modelRuleItems;

    @Schema(description = "型号规则字典列表")
    private List<HcModelRuleDictDO> modelRuleDicts;

}