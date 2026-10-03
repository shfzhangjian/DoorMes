package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskConfirmLogDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionGroupTaskConfirmLogMapper extends BaseMapperX<QmsExceptionGroupTaskConfirmLogDO> {

    default List<QmsExceptionGroupTaskConfirmLogDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskConfirmLogDO>()
                .in(QmsExceptionGroupTaskConfirmLogDO::getTaskId, taskIds)
                .orderByAsc(QmsExceptionGroupTaskConfirmLogDO::getTaskId)
                .orderByAsc(QmsExceptionGroupTaskConfirmLogDO::getConfirmTime)
                .orderByAsc(QmsExceptionGroupTaskConfirmLogDO::getId));
    }

    default void deleteByTaskId(Long taskId) {
        delete(new LambdaQueryWrapperX<QmsExceptionGroupTaskConfirmLogDO>()
                .eq(QmsExceptionGroupTaskConfirmLogDO::getTaskId, taskId));
    }
}
