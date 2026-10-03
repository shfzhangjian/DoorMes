package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskLogDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsDispatchTaskLogMapper extends BaseMapperX<QmsDispatchTaskLogDO> {

    default List<QmsDispatchTaskLogDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapper<QmsDispatchTaskLogDO>()
                .eq(QmsDispatchTaskLogDO::getTaskId, taskId)
                .orderByDesc(QmsDispatchTaskLogDO::getId));
    }
}
