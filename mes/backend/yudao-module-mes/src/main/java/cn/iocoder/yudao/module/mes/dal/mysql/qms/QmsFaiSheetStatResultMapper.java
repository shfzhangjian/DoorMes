package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetStatResultDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiSheetStatResultMapper extends BaseMapperX<QmsFaiSheetStatResultDO> {

    default List<QmsFaiSheetStatResultDO> selectListByFaiId(Long faiId) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetStatResultDO>()
                .eq(QmsFaiSheetStatResultDO::getFaiId, faiId)
                .orderByAsc(QmsFaiSheetStatResultDO::getSectionCode)
                .orderByAsc(QmsFaiSheetStatResultDO::getMetricCode));
    }

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetStatResultDO>()
                .eq(QmsFaiSheetStatResultDO::getFaiId, faiId));
    }

    default int deleteByFaiIdAndItemIds(Long faiId, Collection<Long> faiItemIds) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetStatResultDO>()
                .eq(QmsFaiSheetStatResultDO::getFaiId, faiId)
                .in(QmsFaiSheetStatResultDO::getFaiItemId, faiItemIds));
    }

    default List<QmsFaiSheetStatResultDO> selectListByFaiIdAndItemIds(Long faiId, Collection<Long> faiItemIds) {
        return selectList(new LambdaQueryWrapper<QmsFaiSheetStatResultDO>()
                .eq(QmsFaiSheetStatResultDO::getFaiId, faiId)
                .in(QmsFaiSheetStatResultDO::getFaiItemId, faiItemIds)
                .orderByAsc(QmsFaiSheetStatResultDO::getFaiItemId)
                .orderByAsc(QmsFaiSheetStatResultDO::getSectionCode)
                .orderByAsc(QmsFaiSheetStatResultDO::getMetricCode));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiSheetStatResultDO>()
                .in(QmsFaiSheetStatResultDO::getFaiId, faiIds));
    }
}
