package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetStatResultDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSheetStatResultMapper extends BaseMapperX<QmsFqcSheetStatResultDO> {

    default List<QmsFqcSheetStatResultDO> selectListByFqcId(Long FqcId) {
        return selectList(new LambdaQueryWrapper<QmsFqcSheetStatResultDO>()
                .eq(QmsFqcSheetStatResultDO::getFqcId, FqcId)
                .orderByAsc(QmsFqcSheetStatResultDO::getSectionCode)
                .orderByAsc(QmsFqcSheetStatResultDO::getMetricCode));
    }

    default int deleteByFqcId(Long FqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcSheetStatResultDO>()
                .eq(QmsFqcSheetStatResultDO::getFqcId, FqcId));
    }

    default int deleteByFqcIdAndItemIds(Long FqcId, Collection<Long> FqcItemIds) {
        return delete(new LambdaQueryWrapper<QmsFqcSheetStatResultDO>()
                .eq(QmsFqcSheetStatResultDO::getFqcId, FqcId)
                .in(QmsFqcSheetStatResultDO::getFqcItemId, FqcItemIds));
    }

    default int deleteByFqcIds(Collection<Long> FqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcSheetStatResultDO>()
                .in(QmsFqcSheetStatResultDO::getFqcId, FqcIds));
    }
}
