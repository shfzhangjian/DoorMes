package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionNotifyDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcDispositionNotifyMapper extends BaseMapperX<QmsNcDispositionNotifyDO> {

    Collection<String> WORKBENCH_STATUSES = Arrays.asList("PENDING", "REPLIED");

    default PageResult<QmsNcDispositionNotifyDO> selectWorkbenchPage(QmsNcDispositionNotifyPageReqVO reqVO,
                                                                     Long notifyUserId,
                                                                     Collection<Long> ncRecordIds) {
        LambdaQueryWrapperX<QmsNcDispositionNotifyDO> wrapper =
                new LambdaQueryWrapperX<QmsNcDispositionNotifyDO>()
                        .eq(QmsNcDispositionNotifyDO::getNotifyUserId, notifyUserId == null ? -1L : notifyUserId)
                        .likeIfPresent(QmsNcDispositionNotifyDO::getNcNo, reqVO.getNcNo());
        if (CollUtil.isNotEmpty(ncRecordIds)) {
            wrapper.in(QmsNcDispositionNotifyDO::getNcRecordId, ncRecordIds);
        }
        String notifyStatus = reqVO.getNotifyStatus();
        if (StrUtil.isBlank(notifyStatus)) {
            wrapper.in(QmsNcDispositionNotifyDO::getNotifyStatus, WORKBENCH_STATUSES);
        } else if (WORKBENCH_STATUSES.contains(notifyStatus)) {
            wrapper.eq(QmsNcDispositionNotifyDO::getNotifyStatus, notifyStatus);
        } else {
            wrapper.eq(QmsNcDispositionNotifyDO::getId, -1L);
        }
        wrapper.orderByDesc(QmsNcDispositionNotifyDO::getNotifyTime)
                .orderByDesc(QmsNcDispositionNotifyDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default List<QmsNcDispositionNotifyDO> selectListByNcRecordId(Long ncRecordId) {
        return selectList(new LambdaQueryWrapperX<QmsNcDispositionNotifyDO>()
                .eq(QmsNcDispositionNotifyDO::getNcRecordId, ncRecordId)
                .orderByAsc(QmsNcDispositionNotifyDO::getId));
    }

    default List<QmsNcDispositionNotifyDO> selectListByNcRecordIds(Collection<Long> ncRecordIds) {
        if (CollUtil.isEmpty(ncRecordIds)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsNcDispositionNotifyDO>()
                .in(QmsNcDispositionNotifyDO::getNcRecordId, ncRecordIds)
                .orderByAsc(QmsNcDispositionNotifyDO::getNcRecordId)
                .orderByAsc(QmsNcDispositionNotifyDO::getId));
    }

    default List<QmsNcDispositionNotifyDO> selectPendingListByNotifyUserId(Long notifyUserId) {
        if (notifyUserId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsNcDispositionNotifyDO>()
                .eq(QmsNcDispositionNotifyDO::getNotifyUserId, notifyUserId)
                .eq(QmsNcDispositionNotifyDO::getNotifyStatus, "PENDING"));
    }

    default List<QmsNcDispositionNotifyDO> selectListByNotifyUserId(Long notifyUserId) {
        if (notifyUserId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsNcDispositionNotifyDO>()
                .and(wrapper -> wrapper.eq(QmsNcDispositionNotifyDO::getNotifyUserId, notifyUserId)
                        .or().eq(QmsNcDispositionNotifyDO::getReplyUserId, notifyUserId)));
    }

    default QmsNcDispositionNotifyDO selectPendingByNcRecordIdAndNotifyUserId(Long ncRecordId, Long notifyUserId) {
        if (ncRecordId == null || notifyUserId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsNcDispositionNotifyDO>()
                .eq(QmsNcDispositionNotifyDO::getNcRecordId, ncRecordId)
                .eq(QmsNcDispositionNotifyDO::getNotifyUserId, notifyUserId)
                .eq(QmsNcDispositionNotifyDO::getNotifyStatus, "PENDING")
                .last("LIMIT 1"));
    }

    default void cancelPendingByNcRecordId(Long ncRecordId, String remark) {
        if (ncRecordId == null) {
            return;
        }
        update(null, new LambdaUpdateWrapper<QmsNcDispositionNotifyDO>()
                .eq(QmsNcDispositionNotifyDO::getNcRecordId, ncRecordId)
                .eq(QmsNcDispositionNotifyDO::getNotifyStatus, "PENDING")
                .set(QmsNcDispositionNotifyDO::getNotifyStatus, "CANCELLED")
                .set(QmsNcDispositionNotifyDO::getRemark, remark));
    }
}
