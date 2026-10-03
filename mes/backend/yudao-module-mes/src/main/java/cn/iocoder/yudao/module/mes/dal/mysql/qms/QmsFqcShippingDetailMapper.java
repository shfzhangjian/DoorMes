package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcShippingDetailMapper extends BaseMapperX<QmsFqcShippingDetailDO> {

    default List<QmsFqcShippingDetailDO> selectListByFqcId(Long fqcId) {
        if (fqcId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcShippingDetailDO::getId));
    }

    default List<QmsFqcShippingDetailDO> selectListByFqcIds(List<Long> fqcIds) {
        if (fqcIds == null || fqcIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .in(QmsFqcShippingDetailDO::getFqcId, fqcIds)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcShippingDetailDO::getFqcId)
                .orderByAsc(QmsFqcShippingDetailDO::getId));
    }

    default List<QmsFqcShippingDetailDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .in(QmsFqcShippingDetailDO::getId, ids)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcShippingDetailDO::getId));
    }

    default QmsFqcShippingDetailDO selectByFqcIdAndActualSliceBatchNo(Long fqcId, String actualSliceBatchNo) {
        if (fqcId == null || StrUtil.isBlank(actualSliceBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getActualSliceBatchNo, actualSliceBatchNo)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcShippingDetailDO selectByFqcIdAndPickItemId(Long fqcId, Long shippingPickItemId) {
        if (fqcId == null || shippingPickItemId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getShippingPickItemId, shippingPickItemId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcShippingDetailDO selectByFqcIdAndNoticeItemId(Long fqcId, Long shippingNoticeItemId) {
        if (fqcId == null || shippingNoticeItemId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getShippingNoticeItemId, shippingNoticeItemId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcShippingDetailDO selectFirstByFqcId(Long fqcId) {
        if (fqcId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getFqcId, fqcId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByAsc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcShippingDetailDO selectLatestByActualSliceBatchNo(String actualSliceBatchNo) {
        if (StrUtil.isBlank(actualSliceBatchNo)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getActualSliceBatchNo, actualSliceBatchNo)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getFqcId)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcShippingDetailDO selectLatestByShippingPickItemId(Long shippingPickItemId) {
        if (shippingPickItemId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getShippingPickItemId, shippingPickItemId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getFqcId)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFqcShippingDetailDO selectLatestByShippingNoticeItemId(Long shippingNoticeItemId) {
        if (shippingNoticeItemId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getShippingNoticeItemId, shippingNoticeItemId)
                .eq(QmsFqcShippingDetailDO::getDeleted, false)
                .orderByDesc(QmsFqcShippingDetailDO::getFqcId)
                .orderByDesc(QmsFqcShippingDetailDO::getId)
                .last("LIMIT 1"));
    }

    default List<Long> selectFqcIdsByActualSliceBatchNo(String actualSliceBatchNo) {
        if (StrUtil.isBlank(actualSliceBatchNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .like(QmsFqcShippingDetailDO::getActualSliceBatchNo, actualSliceBatchNo)
                .eq(QmsFqcShippingDetailDO::getDeleted, false))
                .stream()
                .map(QmsFqcShippingDetailDO::getFqcId)
                .distinct()
                .toList();
    }

    default List<Long> selectFqcIdsByAlignmentStatus(String alignmentStatus) {
        if (StrUtil.isBlank(alignmentStatus)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .eq(QmsFqcShippingDetailDO::getAlignmentStatus, alignmentStatus)
                .eq(QmsFqcShippingDetailDO::getDeleted, false))
                .stream()
                .map(QmsFqcShippingDetailDO::getFqcId)
                .distinct()
                .toList();
    }

    default List<Long> selectFqcIdsByCustomerName(String customerName) {
        if (StrUtil.isBlank(customerName)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .like(QmsFqcShippingDetailDO::getCustomerName, customerName)
                .eq(QmsFqcShippingDetailDO::getDeleted, false))
                .stream()
                .map(QmsFqcShippingDetailDO::getFqcId)
                .distinct()
                .toList();
    }

    default List<Long> selectFqcIdsByErpOrderNo(String erpOrderNo) {
        if (StrUtil.isBlank(erpOrderNo)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .like(QmsFqcShippingDetailDO::getErpOrderNo, erpOrderNo)
                .eq(QmsFqcShippingDetailDO::getDeleted, false))
                .stream()
                .map(QmsFqcShippingDetailDO::getFqcId)
                .distinct()
                .toList();
    }

    default List<Long> selectFqcIdsByKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsFqcShippingDetailDO>()
                .and(wrapper -> wrapper.like(QmsFqcShippingDetailDO::getShippingNoticeNo, keyword)
                        .or().like(QmsFqcShippingDetailDO::getCustomerName, keyword)
                        .or().like(QmsFqcShippingDetailDO::getMaterialCode, keyword)
                        .or().like(QmsFqcShippingDetailDO::getModelCode, keyword)
                        .or().like(QmsFqcShippingDetailDO::getActualSliceBatchNo, keyword)
                        .or().like(QmsFqcShippingDetailDO::getPackageSliceNo, keyword))
                .eq(QmsFqcShippingDetailDO::getDeleted, false))
                .stream()
                .map(QmsFqcShippingDetailDO::getFqcId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }
}
