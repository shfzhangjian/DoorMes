package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcReturnRecordDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIqcReturnRecordMapper extends BaseMapperX<QmsIqcReturnRecordDO> {

    default List<QmsIqcReturnRecordDO> selectListByIqcId(Long iqcId) {
        return selectList(new LambdaQueryWrapper<QmsIqcReturnRecordDO>()
                .eq(QmsIqcReturnRecordDO::getIqcId, iqcId)
                .orderByDesc(QmsIqcReturnRecordDO::getReturnTime)
                .orderByDesc(QmsIqcReturnRecordDO::getId));
    }

    default int deleteByIqcId(Long iqcId) {
        return delete(new LambdaQueryWrapper<QmsIqcReturnRecordDO>()
                .eq(QmsIqcReturnRecordDO::getIqcId, iqcId));
    }

    default int deleteByIqcIds(Collection<Long> iqcIds) {
        return delete(new LambdaQueryWrapper<QmsIqcReturnRecordDO>()
                .in(QmsIqcReturnRecordDO::getIqcId, iqcIds));
    }
}
