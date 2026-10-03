package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionGroupTaskMapper extends BaseMapperX<QmsExceptionGroupTaskDO> {

    default List<QmsExceptionGroupTaskDO> selectListByExceptionId(Long exceptionId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskDO>()
                .eq(QmsExceptionGroupTaskDO::getExceptionId, exceptionId)
                .orderByAsc(QmsExceptionGroupTaskDO::getId));
    }

    default List<QmsExceptionGroupTaskDO> selectListByExceptionIds(Collection<Long> exceptionIds) {
        if (CollUtil.isEmpty(exceptionIds)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskDO>()
                .in(QmsExceptionGroupTaskDO::getExceptionId, exceptionIds)
                .orderByAsc(QmsExceptionGroupTaskDO::getExceptionId)
                .orderByAsc(QmsExceptionGroupTaskDO::getId));
    }

    default List<QmsExceptionGroupTaskDO> selectListByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskDO>()
                .in(QmsExceptionGroupTaskDO::getId, ids));
    }

    default List<QmsExceptionGroupTaskDO> selectListByRootCauseOwnerId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskDO>()
                .eq(QmsExceptionGroupTaskDO::getRootCauseOwnerId, userId));
    }

    default List<QmsExceptionGroupTaskDO> selectListByExecutorUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskDO>()
                .eq(QmsExceptionGroupTaskDO::getExecutorUserId, userId));
    }

    default List<QmsExceptionGroupTaskDO> selectListByDispatcherUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskDO>()
                .eq(QmsExceptionGroupTaskDO::getDispatcherUserId, userId));
    }
}
