package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIqcOrderMapper extends BaseMapperX<QmsIqcOrderDO> {

    default PageResult<QmsIqcOrderDO> selectPage(QmsIqcPageReqVO reqVO) {
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        var wrapper = new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .likeIfPresent(QmsIqcOrderDO::getIqcNo, reqVO.getIqcNo())
                .likeIfPresent(QmsIqcOrderDO::getReceiptNo, reqVO.getReceiptNo())
                .likeIfPresent(QmsIqcOrderDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(QmsIqcOrderDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsIqcOrderDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(QmsIqcOrderDO::getBatchNo, reqVO.getBatchNo())
                .likeIfPresent(QmsIqcOrderDO::getStandardNo, reqVO.getStandardNo())
                .eqIfPresent(QmsIqcOrderDO::getStandardMatchMode, reqVO.getStandardMatchMode())
                .eqIfPresent(QmsIqcOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsIqcOrderDO::getJudgment, reqVO.getJudgment())
                .eqIfPresent(QmsIqcOrderDO::getRecheckFlag, reqVO.getRecheckFlag())
                .eqIfPresent(QmsIqcOrderDO::getRetentionDestroyStatus, reqVO.getRetentionDestroyStatus())
                .betweenIfPresent(QmsIqcOrderDO::getInspectionTime, reqVO.getInspectionTime())
                .betweenIfPresent(QmsIqcOrderDO::getRetentionExpireTime, reqVO.getRetentionExpireTime())
                .and(Boolean.TRUE.equals(reqVO.getRetentionActiveOnly()), active -> active
                        .isNull(QmsIqcOrderDO::getRetentionExpireTime)
                        .or()
                        .ge(QmsIqcOrderDO::getRetentionExpireTime, tomorrowStart))
                .and(Boolean.TRUE.equals(reqVO.getRetentionActiveOnly()), notDestroyed -> notDestroyed
                        .isNull(QmsIqcOrderDO::getRetentionDestroyStatus)
                        .or()
                        .ne(QmsIqcOrderDO::getRetentionDestroyStatus, "DESTROYED"))
                .isNotNull(Boolean.TRUE.equals(reqVO.getRetentionExpiredOnly()),
                        QmsIqcOrderDO::getRetentionExpireTime)
                .le(Boolean.TRUE.equals(reqVO.getRetentionExpiredOnly()),
                        QmsIqcOrderDO::getRetentionExpireTime, tomorrowStart.minusNanos(1));
        if ("RETAINED".equals(reqVO.getRetentionStatus())) {
            wrapper.eq(QmsIqcOrderDO::getRetentionStatus, "RETAINED");
        } else if ("NOT_RETAINED".equals(reqVO.getRetentionStatus())) {
            wrapper.and(status -> status
                    .isNull(QmsIqcOrderDO::getRetentionStatus)
                    .or()
                    .ne(QmsIqcOrderDO::getRetentionStatus, "RETAINED"));
        }
        wrapper.orderByDesc(QmsIqcOrderDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default QmsIqcOrderDO selectByIqcNo(String iqcNo, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getIqcNo, iqcNo)
                .neIfPresent(QmsIqcOrderDO::getId, excludeId));
    }

    default List<QmsIqcOrderDO> selectListByStatuses(Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .in(QmsIqcOrderDO::getStatus, statuses)
                .orderByAsc(QmsIqcOrderDO::getCreateTime));
    }

    default List<QmsIqcOrderDO> selectListByIqcNo(String iqcNo, Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getIqcNo, iqcNo)
                .inIfPresent(QmsIqcOrderDO::getStatus, statuses)
                .orderByDesc(QmsIqcOrderDO::getId));
    }

    default List<QmsIqcOrderDO> selectListByReceiptNo(String receiptNo, Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getReceiptNo, receiptNo)
                .inIfPresent(QmsIqcOrderDO::getStatus, statuses)
                .orderByDesc(QmsIqcOrderDO::getId));
    }

    default List<QmsIqcOrderDO> selectListByBatchNo(String batchNo, Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getBatchNo, batchNo)
                .inIfPresent(QmsIqcOrderDO::getStatus, statuses)
                .orderByDesc(QmsIqcOrderDO::getId));
    }

    default List<QmsIqcOrderDO> selectListByMaterialCode(String materialCode, Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getMaterialCode, materialCode)
                .inIfPresent(QmsIqcOrderDO::getStatus, statuses)
                .orderByDesc(QmsIqcOrderDO::getId));
    }

    default void updateAuditNotifyTime(Long id, LocalDateTime auditNotifyTime) {
        update(null, new LambdaUpdateWrapper<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getId, id)
                .set(QmsIqcOrderDO::getAuditNotifyTime, auditNotifyTime));
    }

    default void clearAuditNotifyTime(Long id) {
        updateAuditNotifyTime(id, null);
    }
}
