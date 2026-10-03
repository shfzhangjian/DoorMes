package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskRoundDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsDispatchTaskRoundMapper extends BaseMapperX<QmsDispatchTaskRoundDO> {

    default QmsDispatchTaskRoundDO selectByTaskIdAndRoundNo(Long taskId, Integer roundNo) {
        return selectOne(new LambdaQueryWrapper<QmsDispatchTaskRoundDO>()
                .eq(QmsDispatchTaskRoundDO::getTaskId, taskId)
                .eq(QmsDispatchTaskRoundDO::getRoundNo, roundNo));
    }

    default List<QmsDispatchTaskRoundDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapper<QmsDispatchTaskRoundDO>()
                .eq(QmsDispatchTaskRoundDO::getTaskId, taskId)
                .orderByAsc(QmsDispatchTaskRoundDO::getRoundNo));
    }
}
