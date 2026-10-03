package cn.iocoder.yudao.module.mes.service.hc.lotrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterInitializeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleCounterDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import java.util.List;
import java.util.Map;

public interface HcLotRuleService {
    Long createHcLotRule(HcLotRuleSaveReqVO createReqVO);
    void updateHcLotRule(HcLotRuleSaveReqVO updateReqVO);
    void deleteHcLotRule(Long id);
    void deleteHcLotRuleListByIds(List<Long> ids);
    HcLotRuleDO getHcLotRule(Long id);
    HcLotRuleDO matchEnabledRule(HcLotRuleMatchContext context);
    Long copyAsNewVersion(Long id);
    void publishHcLotRule(Long id);
    void disableHcLotRule(Long id);
    List<HcLotRuleDO> getHcLotRuleSimpleList();
    List<HcLotRuleDO> getHcLotRuleList(HcLotRulePageReqVO reqVO);
    PageResult<HcLotRuleDO> getHcLotRulePage(HcLotRulePageReqVO pageReqVO);
    List<HcLotRuleSegmentDO> getHcLotRuleSegmentListByParentId(Long parentId);
    List<HcLotRuleCounterDO> getHcLotRuleCounterListByRuleId(Long ruleId);
    PageResult<HcLotRuleCounterDO> getHcLotRuleCounterPage(HcLotRuleCounterPageReqVO pageReqVO);
    Long createHcLotRuleCounter(HcLotRuleCounterSaveReqVO createReqVO);
    void updateHcLotRuleCounter(HcLotRuleCounterSaveReqVO updateReqVO);
    void deleteHcLotRuleCounter(Long id);
    HcLotRuleCounterDO initializeHcLotRuleCounter(HcLotRuleCounterInitializeReqVO reqVO);
    HcLotRuleCounterDO adjustHcLotRuleCounter(HcLotRuleCounterAdjustReqVO reqVO);
    HcLotRuleCounterPreviewRespVO previewNextLotRuleCounter(HcLotRuleCounterPreviewReqVO reqVO);
    Map<String, Object> generateLotNo(HcLotRuleGenerateReqVO reqVO);
    Map<String, String> parseLotNo(HcLotRuleParseReqVO reqVO);
}
