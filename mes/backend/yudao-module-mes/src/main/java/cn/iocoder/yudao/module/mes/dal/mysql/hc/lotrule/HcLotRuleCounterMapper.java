package cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleCounterDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcLotRuleCounterMapper extends BaseMapperX<HcLotRuleCounterDO> {

    default HcLotRuleCounterDO selectByRuleAndCounterKey(Long ruleId, String counterKey) {
        return selectOne(new LambdaQueryWrapperX<HcLotRuleCounterDO>()
                .eq(HcLotRuleCounterDO::getRuleId, ruleId)
                .eq(HcLotRuleCounterDO::getCounterKey, counterKey)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_lot_rule_counter
            WHERE deleted = 0
              AND rule_id = #{ruleId}
              AND counter_key = #{counterKey}
            LIMIT 1
            FOR UPDATE
            """)
    HcLotRuleCounterDO selectByRuleAndCounterKeyForUpdate(@Param("ruleId") Long ruleId,
                                                          @Param("counterKey") String counterKey);

    @Select("""
            SELECT *
            FROM mes_lot_rule_counter
            WHERE deleted = 0
              AND id = #{id}
            LIMIT 1
            FOR UPDATE
            """)
    HcLotRuleCounterDO selectByIdForUpdate(@Param("id") Long id);

    default Long selectCountByRuleAndCounterKey(Long ruleId, String counterKey) {
        return selectCount(new LambdaQueryWrapperX<HcLotRuleCounterDO>()
                .eq(HcLotRuleCounterDO::getRuleId, ruleId)
                .eq(HcLotRuleCounterDO::getCounterKey, counterKey));
    }

    default List<HcLotRuleCounterDO> selectListByRuleId(Long ruleId) {
        return selectList(new LambdaQueryWrapperX<HcLotRuleCounterDO>()
                .eq(HcLotRuleCounterDO::getRuleId, ruleId)
                .orderByDesc(HcLotRuleCounterDO::getUpdateTime)
                .orderByDesc(HcLotRuleCounterDO::getId));
    }

    default PageResult<HcLotRuleCounterDO> selectPage(HcLotRuleCounterPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HcLotRuleCounterDO>()
                .likeIfPresent(HcLotRuleCounterDO::getRuleCode, reqVO.getRuleCode())
                .eqIfPresent(HcLotRuleCounterDO::getCounterType, reqVO.getCounterType())
                .likeIfPresent(HcLotRuleCounterDO::getBizDimensionKey, reqVO.getBizDimensionKey())
                .likeIfPresent(HcLotRuleCounterDO::getCounterKey, reqVO.getCounterKey())
                .likeIfPresent(HcLotRuleCounterDO::getResetKey, reqVO.getResetKey())
                .orderByDesc(HcLotRuleCounterDO::getUpdateTime)
                .orderByDesc(HcLotRuleCounterDO::getId));
    }
}
