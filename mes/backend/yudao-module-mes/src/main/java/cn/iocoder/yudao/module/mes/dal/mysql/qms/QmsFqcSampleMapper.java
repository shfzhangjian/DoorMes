package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSampleMapper extends BaseMapperX<QmsFqcSampleDO> {

    default List<QmsFqcSampleDO> selectListByFqcId(Long FqcId) {
        return selectList(new LambdaQueryWrapper<QmsFqcSampleDO>()
                .eq(QmsFqcSampleDO::getFqcId, FqcId)
                .orderByAsc(QmsFqcSampleDO::getSliceSeqNo)
                .orderByAsc(QmsFqcSampleDO::getFqcItemId)
                .orderByAsc(QmsFqcSampleDO::getSampleSeq));
    }

    default List<QmsFqcSampleDO> selectListByFqcIdAndItemIdsAndRole(Long fqcId, Collection<Long> fqcItemIds,
                                                                    String sampleRole) {
        if (fqcItemIds == null || fqcItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcSampleDO>()
                .eq(QmsFqcSampleDO::getFqcId, fqcId)
                .in(QmsFqcSampleDO::getFqcItemId, fqcItemIds)
                .eq(QmsFqcSampleDO::getSampleRole, sampleRole)
                .orderByAsc(QmsFqcSampleDO::getFqcItemId)
                .orderByAsc(QmsFqcSampleDO::getSampleSeq));
    }

    default List<QmsFqcSampleDO> selectListByFqcIdAndItemIds(Long fqcId, Collection<Long> fqcItemIds) {
        if (fqcItemIds == null || fqcItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcSampleDO>()
                .eq(QmsFqcSampleDO::getFqcId, fqcId)
                .in(QmsFqcSampleDO::getFqcItemId, fqcItemIds)
                .orderByAsc(QmsFqcSampleDO::getFqcItemId)
                .orderByAsc(QmsFqcSampleDO::getSampleSeq));
    }

    default int deleteByFqcId(Long FqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcSampleDO>()
                .eq(QmsFqcSampleDO::getFqcId, FqcId));
    }

    default int deleteByFqcIds(Collection<Long> FqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcSampleDO>()
                .in(QmsFqcSampleDO::getFqcId, FqcIds));
    }

    default int deleteByFqcItemIds(Collection<Long> fqcItemIds) {
        if (fqcItemIds == null || fqcItemIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<QmsFqcSampleDO>()
                .in(QmsFqcSampleDO::getFqcItemId, fqcItemIds));
    }
}
