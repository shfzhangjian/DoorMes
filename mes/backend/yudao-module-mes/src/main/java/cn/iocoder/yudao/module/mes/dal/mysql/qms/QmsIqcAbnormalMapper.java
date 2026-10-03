package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcAbnormalDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIqcAbnormalMapper extends BaseMapperX<QmsIqcAbnormalDO> {

    default List<QmsIqcAbnormalDO> selectListByIqcId(Long iqcId) {
        return selectList(new LambdaQueryWrapper<QmsIqcAbnormalDO>()
                .eq(QmsIqcAbnormalDO::getIqcId, iqcId)
                .orderByAsc(QmsIqcAbnormalDO::getId));
    }

    default int deleteByIqcId(Long iqcId) {
        return delete(new LambdaQueryWrapper<QmsIqcAbnormalDO>()
                .eq(QmsIqcAbnormalDO::getIqcId, iqcId));
    }

    default int deleteByIqcIds(Collection<Long> iqcIds) {
        return delete(new LambdaQueryWrapper<QmsIqcAbnormalDO>()
                .in(QmsIqcAbnormalDO::getIqcId, iqcIds));
    }

    default void updateNcRecordIdByIqcId(Long iqcId, Long ncRecordId) {
        update(null, new LambdaUpdateWrapper<QmsIqcAbnormalDO>()
                .eq(QmsIqcAbnormalDO::getIqcId, iqcId)
                .set(QmsIqcAbnormalDO::getNcRecordId, ncRecordId)
                .set(QmsIqcAbnormalDO::getProcessStatus, "NCR_CREATED"));
    }
}
