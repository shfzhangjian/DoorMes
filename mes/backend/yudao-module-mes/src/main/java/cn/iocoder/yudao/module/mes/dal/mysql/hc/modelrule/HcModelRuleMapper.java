package cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRulePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcModelRuleMapper extends BaseMapperX<HcModelRuleDO> {

    default PageResult<HcModelRuleDO> selectPage(HcModelRulePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcModelRuleDO> selectList(HcModelRulePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcModelRuleDO> buildQuery(HcModelRulePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcModelRuleDO>()
                .likeIfPresent(HcModelRuleDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(HcModelRuleDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(HcModelRuleDO::getRuleCategory, reqVO.getRuleCategory())
                .eqIfPresent(HcModelRuleDO::getTargetLevel, reqVO.getTargetLevel())
                .eqIfPresent(HcModelRuleDO::getRuleDesc, reqVO.getRuleDesc())
                .eqIfPresent(HcModelRuleDO::getEffectiveVersion, reqVO.getEffectiveVersion())
                .eqIfPresent(HcModelRuleDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcModelRuleDO::getId);
    }
}