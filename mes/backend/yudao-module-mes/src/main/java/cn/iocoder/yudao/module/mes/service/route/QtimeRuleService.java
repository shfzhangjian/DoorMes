// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.route.QtimeRuleServiceImpl.java
package cn.iocoder.yudao.module.mes.service.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.QtimeRulePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.QtimeRuleDO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.QtimeRuleSaveReqVO;
import jakarta.validation.Valid;

public interface QtimeRuleService {
    Long createQtimeRule(@Valid QtimeRuleSaveReqVO createReqVO);
    void updateQtimeRule(@Valid QtimeRuleSaveReqVO updateReqVO);
    void deleteQtimeRule(Long id);
    PageResult<QtimeRuleDO> getQtimeRulePage(QtimeRulePageReqVO pageReqVO);
}
