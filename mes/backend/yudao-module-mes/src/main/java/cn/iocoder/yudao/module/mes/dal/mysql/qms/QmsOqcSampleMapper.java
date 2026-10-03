package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcSampleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsOqcSampleMapper extends BaseMapperX<QmsOqcSampleDO> {

    default List<QmsOqcSampleDO> selectListByOqcId(Long oqcId) {
        return selectList(new LambdaQueryWrapper<QmsOqcSampleDO>()
                .eq(QmsOqcSampleDO::getOqcId, oqcId)
                .orderByAsc(QmsOqcSampleDO::getOqcItemId)
                .orderByAsc(QmsOqcSampleDO::getSampleSeq));
    }

    default int deleteByOqcId(Long oqcId) {
        return delete(new LambdaQueryWrapper<QmsOqcSampleDO>()
                .eq(QmsOqcSampleDO::getOqcId, oqcId));
    }

    default int deleteByOqcIds(Collection<Long> oqcIds) {
        return delete(new LambdaQueryWrapper<QmsOqcSampleDO>()
                .in(QmsOqcSampleDO::getOqcId, oqcIds));
    }
}
