package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcNodeDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceMetricCalcNodeMapper extends BaseMapperX<SrmPerformanceMetricCalcNodeDO> {

    default List<SrmPerformanceMetricCalcNodeDO> selectListByRuleId(Long ruleId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceMetricCalcNodeDO>()
                .eq(SrmPerformanceMetricCalcNodeDO::getRuleId, ruleId)
                .orderByAsc(SrmPerformanceMetricCalcNodeDO::getSortNo)
                .orderByAsc(SrmPerformanceMetricCalcNodeDO::getId));
    }

    default void deleteByRuleId(Long ruleId) {
        delete(SrmPerformanceMetricCalcNodeDO::getRuleId, ruleId);
    }

}
