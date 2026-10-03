package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsDispatchTaskItemMapper extends BaseMapperX<QmsDispatchTaskItemDO> {

    default List<QmsDispatchTaskItemDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapper<QmsDispatchTaskItemDO>()
                .eq(QmsDispatchTaskItemDO::getTaskId, taskId)
                .orderByAsc(QmsDispatchTaskItemDO::getSort)
                .orderByAsc(QmsDispatchTaskItemDO::getId));
    }

    default List<QmsDispatchTaskItemDO> selectListByTaskIdAndRoundNo(Long taskId, Integer roundNo) {
        LambdaQueryWrapper<QmsDispatchTaskItemDO> query = new LambdaQueryWrapper<QmsDispatchTaskItemDO>()
                .eq(QmsDispatchTaskItemDO::getTaskId, taskId);
        if (roundNo == null || roundNo == 0) {
            query.and(wrapper -> wrapper.eq(QmsDispatchTaskItemDO::getRoundNo, 0)
                    .or().isNull(QmsDispatchTaskItemDO::getRoundNo));
        } else {
            query.eq(QmsDispatchTaskItemDO::getRoundNo, roundNo);
        }
        return selectList(query.orderByAsc(QmsDispatchTaskItemDO::getSort)
                .orderByAsc(QmsDispatchTaskItemDO::getId));
    }
}
