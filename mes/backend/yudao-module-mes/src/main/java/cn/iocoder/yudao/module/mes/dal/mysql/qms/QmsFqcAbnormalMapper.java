package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcAbnormalDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcAbnormalMapper extends BaseMapperX<QmsFqcAbnormalDO> {

    default List<QmsFqcAbnormalDO> selectListByFqcId(Long FqcId) {
        return selectList(new LambdaQueryWrapper<QmsFqcAbnormalDO>()
                .eq(QmsFqcAbnormalDO::getFqcId, FqcId)
                .orderByAsc(QmsFqcAbnormalDO::getId));
    }

    default int deleteByFqcId(Long FqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcAbnormalDO>()
                .eq(QmsFqcAbnormalDO::getFqcId, FqcId));
    }

    default int deleteByFqcIds(Collection<Long> FqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcAbnormalDO>()
                .in(QmsFqcAbnormalDO::getFqcId, FqcIds));
    }
}
