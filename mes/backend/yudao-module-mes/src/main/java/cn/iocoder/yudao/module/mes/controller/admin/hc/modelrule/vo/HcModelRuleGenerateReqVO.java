package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 型号编码规则测试生成 Request VO")
@Data
public class HcModelRuleGenerateReqVO {

    @Schema(description = "规则字段列表")
    @NotEmpty(message = "规则字段不能为空")
    private List<HcModelRuleItemDO> modelRuleItems;

    @Schema(description = "规则字典列表")
    private List<HcModelRuleDictDO> modelRuleDicts;

    @Schema(description = "测试输入值，key=itemCode")
    private Map<String, String> testValues;
}
