package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIqcSampleMapper extends BaseMapperX<QmsIqcSampleDO> {

    default List<QmsIqcSampleDO> selectListByIqcId(Long iqcId) {
        return selectList(new LambdaQueryWrapper<QmsIqcSampleDO>()
                .eq(QmsIqcSampleDO::getIqcId, iqcId)
                .orderByAsc(QmsIqcSampleDO::getIqcItemId)
                .orderByAsc(QmsIqcSampleDO::getSampleSeq));
    }

    default int deleteByIqcId(Long iqcId) {
        return delete(new LambdaQueryWrapper<QmsIqcSampleDO>()
                .eq(QmsIqcSampleDO::getIqcId, iqcId));
    }

    default int deleteByIqcIds(Collection<Long> iqcIds) {
        return delete(new LambdaQueryWrapper<QmsIqcSampleDO>()
                .in(QmsIqcSampleDO::getIqcId, iqcIds));
    }

    default int deleteByIqcIdAndItemIds(Long iqcId, Collection<Long> itemIds) {
        return delete(new LambdaQueryWrapper<QmsIqcSampleDO>()
                .eq(QmsIqcSampleDO::getIqcId, iqcId)
                .in(QmsIqcSampleDO::getIqcItemId, itemIds));
    }
}
