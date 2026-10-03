package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 型号编码规则 Detail Response VO")
@Data
public class HcModelRuleDetailRespVO extends HcModelRuleRespVO {

    @Schema(description = "型号规则字段列表")
    private List<HcModelRuleItemDO> modelRuleItems;

    @Schema(description = "型号规则字典列表")
    private List<HcModelRuleDictDO> modelRuleDicts;

}