package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcRuleDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceMetricCalcRuleMapper extends BaseMapperX<SrmPerformanceMetricCalcRuleDO> {

    default SrmPerformanceMetricCalcRuleDO selectByConfigItemId(Long configItemId) {
        return selectOne(new LambdaQueryWrapperX<SrmPerformanceMetricCalcRuleDO>()
                .eq(SrmPerformanceMetricCalcRuleDO::getConfigItemId, configItemId)
                .eq(SrmPerformanceMetricCalcRuleDO::getEnabled, Boolean.TRUE)
                .orderByDesc(SrmPerformanceMetricCalcRuleDO::getId)
                .last("LIMIT 1"));
    }

    default List<SrmPerformanceMetricCalcRuleDO> selectListByConfigId(Long configId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceMetricCalcRuleDO>()
                .eq(SrmPerformanceMetricCalcRuleDO::getConfigId, configId)
                .orderByAsc(SrmPerformanceMetricCalcRuleDO::getConfigItemId)
                .orderByDesc(SrmPerformanceMetricCalcRuleDO::getId));
    }

    default void deleteByConfigItemId(Long configItemId) {
        delete(SrmPerformanceMetricCalcRuleDO::getConfigItemId, configItemId);
    }

    default void deleteByConfigId(Long configId) {
        delete(SrmPerformanceMetricCalcRuleDO::getConfigId, configId);
    }

}
