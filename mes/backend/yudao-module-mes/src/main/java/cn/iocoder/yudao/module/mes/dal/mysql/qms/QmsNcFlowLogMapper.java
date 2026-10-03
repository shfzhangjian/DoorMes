package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcFlowLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsNcFlowLogMapper extends BaseMapperX<QmsNcFlowLogDO> {

    default List<QmsNcFlowLogDO> selectListByNcRecordId(Long ncRecordId) {
        return selectList(new LambdaQueryWrapperX<QmsNcFlowLogDO>()
                .eq(QmsNcFlowLogDO::getNcRecordId, ncRecordId)
                .orderByAsc(QmsNcFlowLogDO::getHandleTime)
                .orderByAsc(QmsNcFlowLogDO::getId));
    }

    default List<QmsNcFlowLogDO> selectListByHandlerUserId(Long handlerUserId) {
        return selectList(new LambdaQueryWrapperX<QmsNcFlowLogDO>()
                .eq(QmsNcFlowLogDO::getHandlerUserId, handlerUserId));
    }
}
