package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskSampleResultDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsDispatchTaskSampleResultMapper extends BaseMapperX<QmsDispatchTaskSampleResultDO> {

    default List<QmsDispatchTaskSampleResultDO> selectListByTaskIdAndRoundNo(Long taskId, Integer roundNo) {
        return selectList(new LambdaQueryWrapperX<QmsDispatchTaskSampleResultDO>()
                .eq(QmsDispatchTaskSampleResultDO::getTaskId, taskId)
                .eq(QmsDispatchTaskSampleResultDO::getRoundNo, roundNo)
                .orderByAsc(QmsDispatchTaskSampleResultDO::getSampleSeq)
                .orderByAsc(QmsDispatchTaskSampleResultDO::getTaskItemId));
    }
}
