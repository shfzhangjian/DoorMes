package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDefectDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSampleDefectMapper extends BaseMapperX<QmsFqcSampleDefectDO> {

    default List<QmsFqcSampleDefectDO> selectListByFqcId(Long fqcId) {
        return selectList(new LambdaQueryWrapper<QmsFqcSampleDefectDO>()
                .eq(QmsFqcSampleDefectDO::getFqcId, fqcId)
                .orderByAsc(QmsFqcSampleDefectDO::getFqcItemId)
                .orderByAsc(QmsFqcSampleDefectDO::getSampleSeq)
                .orderByAsc(QmsFqcSampleDefectDO::getSort)
                .orderByAsc(QmsFqcSampleDefectDO::getId));
    }

    default List<QmsFqcSampleDefectDO> selectListByFqcIdAndItemIds(Long fqcId, Collection<Long> fqcItemIds) {
        if (fqcItemIds == null || fqcItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcSampleDefectDO>()
                .eq(QmsFqcSampleDefectDO::getFqcId, fqcId)
                .in(QmsFqcSampleDefectDO::getFqcItemId, fqcItemIds)
                .orderByAsc(QmsFqcSampleDefectDO::getFqcItemId)
                .orderByAsc(QmsFqcSampleDefectDO::getSampleSeq)
                .orderByAsc(QmsFqcSampleDefectDO::getSort)
                .orderByAsc(QmsFqcSampleDefectDO::getId));
    }

    default int deleteByFqcId(Long fqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcSampleDefectDO>()
                .eq(QmsFqcSampleDefectDO::getFqcId, fqcId));
    }

    default int deleteByFqcIds(Collection<Long> fqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcSampleDefectDO>()
                .in(QmsFqcSampleDefectDO::getFqcId, fqcIds));
    }

    default int deleteByFqcItemIds(Collection<Long> fqcItemIds) {
        if (fqcItemIds == null || fqcItemIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<QmsFqcSampleDefectDO>()
                .in(QmsFqcSampleDefectDO::getFqcItemId, fqcItemIds));
    }
}
