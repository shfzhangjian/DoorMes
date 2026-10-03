package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiAbnormalDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiAbnormalMapper extends BaseMapperX<QmsFaiAbnormalDO> {

    default List<QmsFaiAbnormalDO> selectListByFaiId(Long faiId) {
        return selectList(new LambdaQueryWrapper<QmsFaiAbnormalDO>()
                .eq(QmsFaiAbnormalDO::getFaiId, faiId)
                .orderByAsc(QmsFaiAbnormalDO::getId));
    }

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiAbnormalDO>()
                .eq(QmsFaiAbnormalDO::getFaiId, faiId));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiAbnormalDO>()
                .in(QmsFaiAbnormalDO::getFaiId, faiIds));
    }
}
