package cn.iocoder.yudao.module.mes.service.hc.modelrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;

import java.util.List;

public interface HcModelRuleService {
    Long createHcModelRule(HcModelRuleSaveReqVO createReqVO);
    void updateHcModelRule(HcModelRuleSaveReqVO updateReqVO);
    void deleteHcModelRule(Long id);
    void deleteHcModelRuleListByIds(List<Long> ids);
    HcModelRuleDO getHcModelRule(Long id);
    List<HcModelRuleDO> getHcModelRuleSimpleList();
    List<HcModelRuleDO> getHcModelRuleList(HcModelRulePageReqVO reqVO);
    PageResult<HcModelRuleDO> getHcModelRulePage(HcModelRulePageReqVO pageReqVO);
    List<HcModelRuleItemDO> getHcModelRuleItemListByParentId(Long parentId);
    List<HcModelRuleDictDO> getHcModelRuleDictListByParentId(Long parentId);
    String generateModelRuleCode(HcModelRuleGenerateReqVO reqVO);
}
