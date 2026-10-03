package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dFlowLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Qms8dFlowLogMapper extends BaseMapperX<Qms8dFlowLogDO> {

    default List<Qms8dFlowLogDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<Qms8dFlowLogDO>()
                .eq(Qms8dFlowLogDO::getReportId, reportId)
                .orderByAsc(Qms8dFlowLogDO::getHandleTime)
                .orderByAsc(Qms8dFlowLogDO::getId));
    }

    default List<Qms8dFlowLogDO> selectListByHandlerUserId(Long handlerUserId) {
        return selectList(new LambdaQueryWrapperX<Qms8dFlowLogDO>()
                .eq(Qms8dFlowLogDO::getHandlerUserId, handlerUserId));
    }
}
