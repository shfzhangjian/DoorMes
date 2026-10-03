package cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRulePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HcLotRuleMapper extends BaseMapperX<HcLotRuleDO> {

    @Select("SELECT * FROM mes_md_lot_rule WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    HcLotRuleDO selectByIdForUpdate(@Param("id") Long id);

    default PageResult<HcLotRuleDO> selectPage(HcLotRulePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcLotRuleDO> selectList(HcLotRulePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcLotRuleDO> buildQuery(HcLotRulePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcLotRuleDO>()
                .likeIfPresent(HcLotRuleDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(HcLotRuleDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(HcLotRuleDO::getBizType, reqVO.getBizType())
                .eqIfPresent(HcLotRuleDO::getProductCategoryCode, reqVO.getProductCategoryCode())
                .eqIfPresent(HcLotRuleDO::getProdType, reqVO.getProdType())
                .eqIfPresent(HcLotRuleDO::getModelMatchMode, reqVO.getModelMatchMode())
                .eqIfPresent(HcLotRuleDO::getGenerationTrigger, reqVO.getGenerationTrigger())
                .eqIfPresent(HcLotRuleDO::getGenerationScope, reqVO.getGenerationScope())
                .eqIfPresent(HcLotRuleDO::getBatchCardinality, reqVO.getBatchCardinality())
                .eqIfPresent(HcLotRuleDO::getPrefix, reqVO.getPrefix())
                .eqIfPresent(HcLotRuleDO::getDateFormat, reqVO.getDateFormat())
                .eqIfPresent(HcLotRuleDO::getSeqLength, reqVO.getSeqLength())
                .eqIfPresent(HcLotRuleDO::getResetCycle, reqVO.getResetCycle())
                .eqIfPresent(HcLotRuleDO::getSampleSegmentRule, reqVO.getSampleSegmentRule())
                .eqIfPresent(HcLotRuleDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcLotRuleDO::getRemark, reqVO.getRemark())
                .orderByDesc(HcLotRuleDO::getId);
    }
}
