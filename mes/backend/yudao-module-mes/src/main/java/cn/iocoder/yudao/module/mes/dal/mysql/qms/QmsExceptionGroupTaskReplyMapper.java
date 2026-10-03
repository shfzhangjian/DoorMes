package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskReplyDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionGroupTaskReplyMapper extends BaseMapperX<QmsExceptionGroupTaskReplyDO> {

    default List<QmsExceptionGroupTaskReplyDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsExceptionGroupTaskReplyDO>()
                .in(QmsExceptionGroupTaskReplyDO::getTaskId, taskIds)
                .orderByAsc(QmsExceptionGroupTaskReplyDO::getTaskId)
                .orderByAsc(QmsExceptionGroupTaskReplyDO::getReplyNo)
                .orderByAsc(QmsExceptionGroupTaskReplyDO::getId));
    }

    default QmsExceptionGroupTaskReplyDO selectLatestByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<QmsExceptionGroupTaskReplyDO>()
                .eq(QmsExceptionGroupTaskReplyDO::getTaskId, taskId)
                .orderByDesc(QmsExceptionGroupTaskReplyDO::getReplyNo)
                .orderByDesc(QmsExceptionGroupTaskReplyDO::getId)
                .last("LIMIT 1"));
    }

    default void deleteByTaskId(Long taskId) {
        delete(new LambdaQueryWrapperX<QmsExceptionGroupTaskReplyDO>()
                .eq(QmsExceptionGroupTaskReplyDO::getTaskId, taskId));
    }
}
