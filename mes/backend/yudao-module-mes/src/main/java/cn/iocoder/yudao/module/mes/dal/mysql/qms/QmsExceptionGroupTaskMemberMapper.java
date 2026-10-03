package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskMemberDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionGroupTaskMemberMapper extends BaseMapperX<QmsExceptionGroupTaskMemberDO> {

    default List<QmsExceptionGroupTaskMemberDO> selectListByExceptionId(Long exceptionId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .eq(QmsExceptionGroupTaskMemberDO::getExceptionId, exceptionId)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getTaskId)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getSortNo)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getId));
    }

    default List<QmsExceptionGroupTaskMemberDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .eq(QmsExceptionGroupTaskMemberDO::getTaskId, taskId)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getSortNo)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getId));
    }

    default List<QmsExceptionGroupTaskMemberDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .in(QmsExceptionGroupTaskMemberDO::getTaskId, taskIds)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getTaskId)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getSortNo)
                .orderByAsc(QmsExceptionGroupTaskMemberDO::getId));
    }

    default List<QmsExceptionGroupTaskMemberDO> selectListByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .and(wrapper -> wrapper.eq(QmsExceptionGroupTaskMemberDO::getUserId, userId)
                        .or()
                        .eq(QmsExceptionGroupTaskMemberDO::getDelegateUserId, userId)));
    }

    default QmsExceptionGroupTaskMemberDO selectByTaskIdAndUserId(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .eq(QmsExceptionGroupTaskMemberDO::getTaskId, taskId)
                .and(wrapper -> wrapper.eq(QmsExceptionGroupTaskMemberDO::getUserId, userId)
                        .or()
                        .eq(QmsExceptionGroupTaskMemberDO::getDelegateUserId, userId)));
    }

    default List<QmsExceptionGroupTaskMemberDO> selectHandledListByActualHandlerUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .eq(QmsExceptionGroupTaskMemberDO::getActualHandlerUserId, userId)
                .isNotNull(QmsExceptionGroupTaskMemberDO::getConfirmTime)
                .orderByDesc(QmsExceptionGroupTaskMemberDO::getConfirmTime)
                .orderByDesc(QmsExceptionGroupTaskMemberDO::getId));
    }

    default void resetConfirmStatusByTaskId(Long taskId, String confirmStatus) {
        update(null, new LambdaUpdateWrapper<QmsExceptionGroupTaskMemberDO>()
                .eq(QmsExceptionGroupTaskMemberDO::getTaskId, taskId)
                .set(QmsExceptionGroupTaskMemberDO::getActualFinishTime, null)
                .set(QmsExceptionGroupTaskMemberDO::getActionDescription, null)
                .set(QmsExceptionGroupTaskMemberDO::getRootCauseCategory, null)
                .set(QmsExceptionGroupTaskMemberDO::getRootCause, null)
                .set(QmsExceptionGroupTaskMemberDO::getPreventiveAction, null)
                .set(QmsExceptionGroupTaskMemberDO::getAttachmentUrls, null)
                .set(QmsExceptionGroupTaskMemberDO::getActualHandlerUserId, null)
                .set(QmsExceptionGroupTaskMemberDO::getActualHandlerUserName, null)
                .set(QmsExceptionGroupTaskMemberDO::getConfirmStatus, confirmStatus)
                .set(QmsExceptionGroupTaskMemberDO::getConfirmTime, null)
                .set(QmsExceptionGroupTaskMemberDO::getConfirmRemark, null)
                .set(QmsExceptionGroupTaskMemberDO::getOverdueFlag, false));
    }

    default void deleteByTaskId(Long taskId) {
        delete(new LambdaQueryWrapperX<QmsExceptionGroupTaskMemberDO>()
                .eq(QmsExceptionGroupTaskMemberDO::getTaskId, taskId));
    }
}
