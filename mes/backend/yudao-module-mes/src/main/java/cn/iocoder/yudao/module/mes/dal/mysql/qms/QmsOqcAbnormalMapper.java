package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcAbnormalDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsOqcAbnormalMapper extends BaseMapperX<QmsOqcAbnormalDO> {

    default List<QmsOqcAbnormalDO> selectListByOqcId(Long oqcId) {
        return selectList(new LambdaQueryWrapper<QmsOqcAbnormalDO>()
                .eq(QmsOqcAbnormalDO::getOqcId, oqcId)
                .orderByAsc(QmsOqcAbnormalDO::getId));
    }

    default int deleteByOqcId(Long oqcId) {
        return delete(new LambdaQueryWrapper<QmsOqcAbnormalDO>()
                .eq(QmsOqcAbnormalDO::getOqcId, oqcId));
    }

    default int deleteByOqcIds(Collection<Long> oqcIds) {
        return delete(new LambdaQueryWrapper<QmsOqcAbnormalDO>()
                .in(QmsOqcAbnormalDO::getOqcId, oqcIds));
    }
}
