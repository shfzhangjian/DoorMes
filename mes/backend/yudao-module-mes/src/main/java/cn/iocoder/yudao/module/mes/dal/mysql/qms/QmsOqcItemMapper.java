package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsOqcItemMapper extends BaseMapperX<QmsOqcItemDO> {

    default List<QmsOqcItemDO> selectListByOqcId(Long oqcId) {
        return selectList(new LambdaQueryWrapper<QmsOqcItemDO>()
                .eq(QmsOqcItemDO::getOqcId, oqcId)
                .orderByAsc(QmsOqcItemDO::getSort)
                .orderByAsc(QmsOqcItemDO::getId));
    }

    default int deleteByOqcId(Long oqcId) {
        return delete(new LambdaQueryWrapper<QmsOqcItemDO>()
                .eq(QmsOqcItemDO::getOqcId, oqcId));
    }

    default int deleteByOqcIds(Collection<Long> oqcIds) {
        return delete(new LambdaQueryWrapper<QmsOqcItemDO>()
                .in(QmsOqcItemDO::getOqcId, oqcIds));
    }
}
