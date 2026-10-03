package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsOqcOrderMapper extends BaseMapperX<QmsOqcOrderDO> {

    default PageResult<QmsOqcOrderDO> selectPage(QmsOqcPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .likeIfPresent(QmsOqcOrderDO::getOqcNo, reqVO.getOqcNo())
                .likeIfPresent(QmsOqcOrderDO::getShippingNo, reqVO.getShippingNo())
                .likeIfPresent(QmsOqcOrderDO::getNoticeNo, reqVO.getNoticeNo())
                .likeIfPresent(QmsOqcOrderDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(QmsOqcOrderDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsOqcOrderDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(QmsOqcOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(QmsOqcOrderDO::getJudgment, reqVO.getJudgment())
                .eqIfPresent(QmsOqcOrderDO::getRecheckFlag, reqVO.getRecheckFlag())
                .betweenIfPresent(QmsOqcOrderDO::getInspectionTime, reqVO.getInspectionTime())
                .orderByDesc(QmsOqcOrderDO::getId));
    }

    default QmsOqcOrderDO selectByOqcNo(String oqcNo, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .eq(QmsOqcOrderDO::getOqcNo, oqcNo)
                .neIfPresent(QmsOqcOrderDO::getId, excludeId));
    }

    default QmsOqcOrderDO selectByNoticeItemId(Long shippingNoticeItemId) {
        return selectOne(new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .eq(QmsOqcOrderDO::getShippingNoticeItemId, shippingNoticeItemId)
                .orderByDesc(QmsOqcOrderDO::getId)
                .last("LIMIT 1"));
    }

    default QmsOqcOrderDO selectActiveByShippingNoticeId(Long shippingNoticeId, String canceledStatus) {
        return selectOne(new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .eq(QmsOqcOrderDO::getShippingNoticeId, shippingNoticeId)
                .ne(QmsOqcOrderDO::getStatus, canceledStatus)
                .orderByDesc(QmsOqcOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsOqcOrderDO> selectListByShippingNoticeId(Long shippingNoticeId) {
        if (shippingNoticeId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .eq(QmsOqcOrderDO::getShippingNoticeId, shippingNoticeId)
                .eq(QmsOqcOrderDO::getDeleted, false)
                .orderByAsc(QmsOqcOrderDO::getId));
    }

    default QmsOqcOrderDO selectFirstActiveByIds(Collection<Long> ids, String canceledStatus) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .in(QmsOqcOrderDO::getId, ids)
                .ne(QmsOqcOrderDO::getStatus, canceledStatus)
                .orderByDesc(QmsOqcOrderDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsOqcOrderDO> selectListByStatuses(Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .in(QmsOqcOrderDO::getStatus, statuses)
                .orderByAsc(QmsOqcOrderDO::getCreateTime));
    }

    default List<QmsOqcOrderDO> selectListByScanCode(String scanCode, Collection<String> statuses) {
        LambdaQueryWrapperX<QmsOqcOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.and(query -> query
                .eq(QmsOqcOrderDO::getShippingNo, scanCode)
                .or()
                .eq(QmsOqcOrderDO::getNoticeNo, scanCode)
                .or()
                .eq(QmsOqcOrderDO::getBatchNo, scanCode)
                .or()
                .eq(QmsOqcOrderDO::getCustomerBatchNo, scanCode)
                .or()
                .eq(QmsOqcOrderDO::getMaterialCode, scanCode));
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(QmsOqcOrderDO::getStatus, statuses);
        }
        return selectList(wrapper.orderByDesc(QmsOqcOrderDO::getId));
    }

    default void updateAuditNotifyTime(Long id, LocalDateTime auditNotifyTime) {
        update(null, new LambdaUpdateWrapper<QmsOqcOrderDO>()
                .eq(QmsOqcOrderDO::getId, id)
                .set(QmsOqcOrderDO::getAuditNotifyTime, auditNotifyTime));
    }

    default void clearAuditNotifyTime(Long id) {
        updateAuditNotifyTime(id, null);
    }
}
