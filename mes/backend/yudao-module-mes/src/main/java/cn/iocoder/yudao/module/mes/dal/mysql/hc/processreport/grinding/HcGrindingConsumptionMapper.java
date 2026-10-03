package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingConsumptionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcGrindingConsumptionMapper extends BaseMapperX<HcGrindingConsumptionDO> {
    default HcGrindingConsumptionDO byRequest(String key) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingConsumptionDO>()
                .eq(HcGrindingConsumptionDO::getRequestKey, key).last("FOR UPDATE"));
    }
    default HcGrindingConsumptionDO bySource(String type, Long id, boolean lock) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingConsumptionDO>()
                .eq(HcGrindingConsumptionDO::getSourceType, type)
                .eq(HcGrindingConsumptionDO::getSourceId, id).last(lock ? "FOR UPDATE" : ""));
    }
    default boolean referencesConsume(Long id) {
        return selectCount(new LambdaQueryWrapperX<HcGrindingConsumptionDO>()
                .eq(HcGrindingConsumptionDO::getCancelled, false)
                .and(q -> q.eq(HcGrindingConsumptionDO::getSandpaperConsumeId, id)
                        .or().eq(HcGrindingConsumptionDO::getGuideClothConsumeId, id))) > 0;
    }
}
