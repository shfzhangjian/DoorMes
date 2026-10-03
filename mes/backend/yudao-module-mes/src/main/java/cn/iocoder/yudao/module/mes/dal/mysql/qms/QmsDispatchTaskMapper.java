package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsDispatchTaskMapper extends BaseMapperX<QmsDispatchTaskDO> {

    default QmsDispatchTaskDO selectByExecution(String checkType, Long executionId) {
        return selectOne(new LambdaQueryWrapper<QmsDispatchTaskDO>()
                .eq(QmsDispatchTaskDO::getCheckType, checkType)
                .eq(QmsDispatchTaskDO::getExecutionId, executionId));
    }

    default QmsDispatchTaskDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(QmsDispatchTaskDO::getProcessInstanceId, processInstanceId);
    }

    default List<QmsDispatchTaskDO> selectListByIqcId(Long iqcId) {
        return selectList(new LambdaQueryWrapper<QmsDispatchTaskDO>()
                .eq(QmsDispatchTaskDO::getCheckType, "IQC")
                .and(wrapper -> wrapper
                        .eq(QmsDispatchTaskDO::getExecutionId, iqcId)
                        .or()
                        .eq(QmsDispatchTaskDO::getSourceExecutionId, iqcId)
                        .or()
                        .eq(QmsDispatchTaskDO::getRootExecutionId, iqcId)));
    }

    default int clearInspectionOperator(Long id) {
        return update(new QmsDispatchTaskDO(), new LambdaUpdateWrapper<QmsDispatchTaskDO>()
                .eq(QmsDispatchTaskDO::getId, id)
                .set(QmsDispatchTaskDO::getSourceInspectorName, null)
                .set(QmsDispatchTaskDO::getSourceInspectionTime, null));
    }
}
