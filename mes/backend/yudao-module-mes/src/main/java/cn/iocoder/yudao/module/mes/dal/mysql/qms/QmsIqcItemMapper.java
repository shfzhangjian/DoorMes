package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIqcItemMapper extends BaseMapperX<QmsIqcItemDO> {

    default List<QmsIqcItemDO> selectListByIqcId(Long iqcId) {
        return selectList(new LambdaQueryWrapper<QmsIqcItemDO>()
                .eq(QmsIqcItemDO::getIqcId, iqcId)
                .orderByAsc(QmsIqcItemDO::getSort)
                .orderByAsc(QmsIqcItemDO::getId));
    }

    default List<QmsIqcItemDO> selectListByIqcIds(Collection<Long> iqcIds) {
        return selectList(new LambdaQueryWrapper<QmsIqcItemDO>()
                .in(QmsIqcItemDO::getIqcId, iqcIds)
                .orderByAsc(QmsIqcItemDO::getSort)
                .orderByAsc(QmsIqcItemDO::getId));
    }

    default int deleteByIqcId(Long iqcId) {
        return delete(new LambdaQueryWrapper<QmsIqcItemDO>()
                .eq(QmsIqcItemDO::getIqcId, iqcId));
    }

    default int deleteByIqcIds(Collection<Long> iqcIds) {
        return delete(new LambdaQueryWrapper<QmsIqcItemDO>()
                .in(QmsIqcItemDO::getIqcId, iqcIds));
    }
}
