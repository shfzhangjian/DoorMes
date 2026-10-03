package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiItemMapper extends BaseMapperX<QmsFaiItemDO> {

    default List<QmsFaiItemDO> selectListByFaiId(Long faiId) {
        return selectList(new LambdaQueryWrapper<QmsFaiItemDO>()
                .eq(QmsFaiItemDO::getFaiId, faiId)
                .orderByAsc(QmsFaiItemDO::getSort)
                .orderByAsc(QmsFaiItemDO::getId));
    }

    default List<QmsFaiItemDO> selectListByFaiIds(Collection<Long> faiIds) {
        return selectList(new LambdaQueryWrapper<QmsFaiItemDO>()
                .in(QmsFaiItemDO::getFaiId, faiIds)
                .orderByAsc(QmsFaiItemDO::getSort)
                .orderByAsc(QmsFaiItemDO::getId));
    }

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiItemDO>()
                .eq(QmsFaiItemDO::getFaiId, faiId));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiItemDO>()
                .in(QmsFaiItemDO::getFaiId, faiIds));
    }

    default QmsFaiItemDO selectByScanCode(Long faiId, String scanCode) {
        return selectOne(new LambdaQueryWrapper<QmsFaiItemDO>()
                .eq(QmsFaiItemDO::getFaiId, faiId)
                .and(wrapper -> wrapper
                        .eq(QmsFaiItemDO::getMetricCode, scanCode)
                        .or()
                        .eq(QmsFaiItemDO::getSheetMetricCode, scanCode)
                        .or()
                        .eq(QmsFaiItemDO::getInspectionItem, scanCode))
                .last("LIMIT 1"));
    }
}
