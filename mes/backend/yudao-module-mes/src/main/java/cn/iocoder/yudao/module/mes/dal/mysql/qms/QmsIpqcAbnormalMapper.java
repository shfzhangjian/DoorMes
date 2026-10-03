package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcAbnormalDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIpqcAbnormalMapper extends BaseMapperX<QmsIpqcAbnormalDO> {

    default List<QmsIpqcAbnormalDO> selectListByIpqcId(Long ipqcId) {
        return selectList(new LambdaQueryWrapper<QmsIpqcAbnormalDO>()
                .eq(QmsIpqcAbnormalDO::getIpqcId, ipqcId)
                .orderByAsc(QmsIpqcAbnormalDO::getId));
    }

    default int deleteByIpqcId(Long ipqcId) {
        return delete(new LambdaQueryWrapper<QmsIpqcAbnormalDO>()
                .eq(QmsIpqcAbnormalDO::getIpqcId, ipqcId));
    }

    default int deleteByIpqcIds(Collection<Long> ipqcIds) {
        return delete(new LambdaQueryWrapper<QmsIpqcAbnormalDO>()
                .in(QmsIpqcAbnormalDO::getIpqcId, ipqcIds));
    }
}
