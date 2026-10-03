package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionrecordrevision;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionrecordrevision.HcProductionRecordRevisionDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductionRecordRevisionMapper extends BaseMapperX<HcProductionRecordRevisionDO> {

    default Integer selectMaxRevisionNo(String moduleCode, String recordKey) {
        HcProductionRecordRevisionDO latest = selectOne(new LambdaQueryWrapperX<HcProductionRecordRevisionDO>()
                .eq(HcProductionRecordRevisionDO::getModuleCode, moduleCode)
                .eq(HcProductionRecordRevisionDO::getRecordKey, recordKey)
                .orderByDesc(HcProductionRecordRevisionDO::getRevisionNo)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getRevisionNo();
    }

    default List<HcProductionRecordRevisionDO> selectLatestList(String moduleCode, Collection<String> recordKeys) {
        if (recordKeys == null || recordKeys.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcProductionRecordRevisionDO>()
                .eq(HcProductionRecordRevisionDO::getModuleCode, moduleCode)
                .in(HcProductionRecordRevisionDO::getRecordKey, recordKeys)
                .orderByAsc(HcProductionRecordRevisionDO::getRecordKey)
                .orderByDesc(HcProductionRecordRevisionDO::getRevisionNo));
    }

}
