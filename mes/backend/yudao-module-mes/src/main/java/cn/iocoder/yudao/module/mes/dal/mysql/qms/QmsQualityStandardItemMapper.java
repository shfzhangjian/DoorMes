package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsQualityStandardItemMapper extends BaseMapperX<QmsQualityStandardItemDO> {

    default List<QmsQualityStandardItemDO> selectListByStandardId(Long standardId) {
        return selectList(new LambdaQueryWrapper<QmsQualityStandardItemDO>()
                .eq(QmsQualityStandardItemDO::getStandardId, standardId)
                .orderByAsc(QmsQualityStandardItemDO::getSort)
                .orderByAsc(QmsQualityStandardItemDO::getId));
    }

    default int deleteByStandardId(Long standardId) {
        return delete(new LambdaQueryWrapper<QmsQualityStandardItemDO>()
                .eq(QmsQualityStandardItemDO::getStandardId, standardId));
    }

    default int deleteByStandardIds(Collection<Long> standardIds) {
        return delete(new LambdaQueryWrapper<QmsQualityStandardItemDO>()
                .in(QmsQualityStandardItemDO::getStandardId, standardIds));
    }
}
