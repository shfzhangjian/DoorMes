package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionFlowLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionFlowLogMapper extends BaseMapperX<QmsExceptionFlowLogDO> {

    default List<QmsExceptionFlowLogDO> selectListByExceptionId(Long exceptionId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionFlowLogDO>()
                .eq(QmsExceptionFlowLogDO::getExceptionId, exceptionId)
                .orderByAsc(QmsExceptionFlowLogDO::getHandleTime)
                .orderByAsc(QmsExceptionFlowLogDO::getId));
    }

    default List<QmsExceptionFlowLogDO> selectListByHandlerUserId(Long handlerUserId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionFlowLogDO>()
                .eq(QmsExceptionFlowLogDO::getHandlerUserId, handlerUserId));
    }
}
