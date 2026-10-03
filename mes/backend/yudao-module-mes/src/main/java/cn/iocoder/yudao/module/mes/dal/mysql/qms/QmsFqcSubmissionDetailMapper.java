package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSubmissionDetailMapper extends BaseMapperX<QmsFqcSubmissionDetailDO> {

    default List<QmsFqcSubmissionDetailDO> selectListByFqcId(Long fqcId) {
        if (fqcId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default List<QmsFqcSubmissionDetailDO> selectListByFqcIds(List<Long> fqcIds) {
        if (fqcIds == null || fqcIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .in(QmsFqcSubmissionDetailDO::getFqcId, fqcIds)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getFqcId)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default List<QmsFqcSubmissionDetailDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .in(QmsFqcSubmissionDetailDO::getId, ids)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default List<QmsFqcSubmissionDetailDO> selectListByFqcIdAndProductionBatchNo(Long fqcId, String productionBatchNo) {
        if (fqcId == null || StrUtil.isBlank(productionBatchNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getProductionBatchNo, productionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default List<QmsFqcSubmissionDetailDO> selectListByFqcIdAndParentProductionBatchNo(Long fqcId, String parentProductionBatchNo) {
        if (fqcId == null || StrUtil.isBlank(parentProductionBatchNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getParentProductionBatchNo, parentProductionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default List<QmsFqcSubmissionDetailDO> selectListByProductionBatchNo(String productionBatchNo) {
        if (StrUtil.isBlank(productionBatchNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getProductionBatchNo, productionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcSubmissionDetailDO::getFqcId)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default List<QmsFqcSubmissionDetailDO> selectListByParentProductionBatchNo(String parentProductionBatchNo) {
        if (StrUtil.isBlank(parentProductionBatchNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getParentProductionBatchNo, parentProductionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcSubmissionDetailDO::getFqcId)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId));
    }

    default QmsFqcSubmissionDetailDO selectByFqcIdAndProductionBatchNo(Long fqcId, String productionBatchNo) {
        if (fqcId == null || StrUtil.isBlank(productionBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getProductionBatchNo, productionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcSubmissionDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcSubmissionDetailDO selectByFqcIdAndParentProductionBatchNo(Long fqcId, String parentProductionBatchNo) {
        if (fqcId == null || StrUtil.isBlank(parentProductionBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getParentProductionBatchNo, parentProductionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcSubmissionDetailDO selectFirstByFqcId(Long fqcId) {
        if (fqcId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getFqcId, fqcId)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcSubmissionDetailDO selectLatestByProductionBatchNo(String productionBatchNo) {
        if (StrUtil.isBlank(productionBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getProductionBatchNo, productionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcSubmissionDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcSubmissionDetailDO selectLatestByParentProductionBatchNo(String parentProductionBatchNo) {
        if (StrUtil.isBlank(parentProductionBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .eq(QmsFqcSubmissionDetailDO::getParentProductionBatchNo, parentProductionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcSubmissionDetailDO::getFqcId)
                .orderByAsc(QmsFqcSubmissionDetailDO::getSeqNo)
                .orderByAsc(QmsFqcSubmissionDetailDO::getId)
                .last("LIMIT 1"));
    }

    default List<Long> selectFqcIdsByProductionBatchNo(String productionBatchNo) {
        if (StrUtil.isBlank(productionBatchNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcSubmissionDetailDO>()
                .like(QmsFqcSubmissionDetailDO::getProductionBatchNo, productionBatchNo)
                .eq(QmsFqcSubmissionDetailDO::getDeleted, false))
                .stream()
                .map(QmsFqcSubmissionDetailDO::getFqcId)
                .distinct()
                .toList();
    }
}
