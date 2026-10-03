package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIpqcItemMapper extends BaseMapperX<QmsIpqcItemDO> {

    default List<QmsIpqcItemDO> selectListByIpqcId(Long ipqcId) {
        return selectList(new LambdaQueryWrapper<QmsIpqcItemDO>()
                .eq(QmsIpqcItemDO::getIpqcId, ipqcId)
                .orderByAsc(QmsIpqcItemDO::getSort)
                .orderByAsc(QmsIpqcItemDO::getId));
    }

    default int deleteByIpqcId(Long ipqcId) {
        return delete(new LambdaQueryWrapper<QmsIpqcItemDO>()
                .eq(QmsIpqcItemDO::getIpqcId, ipqcId));
    }

    default int deleteByIpqcIds(Collection<Long> ipqcIds) {
        return delete(new LambdaQueryWrapper<QmsIpqcItemDO>()
                .in(QmsIpqcItemDO::getIpqcId, ipqcIds));
    }
}
