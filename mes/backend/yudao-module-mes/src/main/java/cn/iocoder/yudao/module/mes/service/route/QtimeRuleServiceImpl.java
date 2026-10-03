// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.route.QtimeRuleServiceImpl.java
package cn.iocoder.yudao.module.mes.service.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.QtimeRulePageReqVO; // 此处省略PageReqVO代码，结构同上
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.QtimeRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.QtimeRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.route.QtimeRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

@Service
@Validated
class QtimeRuleServiceImpl implements QtimeRuleService {

    @Resource
    private QtimeRuleMapper qtimeRuleMapper;

    @Override
    public Long createQtimeRule(QtimeRuleSaveReqVO createReqVO) {
        QtimeRuleDO ruleDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, QtimeRuleDO.class);
        qtimeRuleMapper.insert(ruleDO);
        return ruleDO.getId();
    }

    @Override
    public void updateQtimeRule(QtimeRuleSaveReqVO updateReqVO) {
        QtimeRuleDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, QtimeRuleDO.class);
        qtimeRuleMapper.updateById(updateObj);
    }

    @Override
    public void deleteQtimeRule(Long id) {
        qtimeRuleMapper.deleteById(id);
    }

    @Override
    public PageResult<QtimeRuleDO> getQtimeRulePage(QtimeRulePageReqVO pageReqVO) {
        return qtimeRuleMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<QtimeRuleDO>()
                .eqIfPresent(QtimeRuleDO::getRouteId, pageReqVO.getRouteId())
                .eqIfPresent(QtimeRuleDO::getConstraintType, pageReqVO.getConstraintType())
                .orderByDesc(QtimeRuleDO::getId));
    }
}
