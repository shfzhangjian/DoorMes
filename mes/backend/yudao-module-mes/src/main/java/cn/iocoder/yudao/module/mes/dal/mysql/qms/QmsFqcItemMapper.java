package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcItemMapper extends BaseMapperX<QmsFqcItemDO> {

    default List<QmsFqcItemDO> selectListByFqcId(Long FqcId) {
        return selectList(new LambdaQueryWrapper<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, FqcId)
                .orderByAsc(QmsFqcItemDO::getSliceSeqNo)
                .orderByAsc(QmsFqcItemDO::getSort)
                .orderByAsc(QmsFqcItemDO::getId));
    }

    default List<QmsFqcItemDO> selectListByFqcIdAndSubmissionDetailId(Long fqcId, Long submissionDetailId) {
        if (fqcId == null || submissionDetailId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, fqcId)
                .eq(QmsFqcItemDO::getSubmissionDetailId, submissionDetailId)
                .orderByAsc(QmsFqcItemDO::getSort)
                .orderByAsc(QmsFqcItemDO::getId));
    }

    default List<QmsFqcItemDO> selectListByFqcIdAndSubmissionDetailIds(Long fqcId, Collection<Long> submissionDetailIds) {
        if (fqcId == null || submissionDetailIds == null || submissionDetailIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, fqcId)
                .in(QmsFqcItemDO::getSubmissionDetailId, submissionDetailIds)
                .orderByAsc(QmsFqcItemDO::getSliceSeqNo)
                .orderByAsc(QmsFqcItemDO::getSort)
                .orderByAsc(QmsFqcItemDO::getId));
    }

    default int deleteByFqcId(Long FqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcItemDO>()
                .eq(QmsFqcItemDO::getFqcId, FqcId));
    }

    default int deleteByFqcIds(Collection<Long> FqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcItemDO>()
                .in(QmsFqcItemDO::getFqcId, FqcIds));
    }
}
