package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcSampleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsIpqcSampleMapper extends BaseMapperX<QmsIpqcSampleDO> {

    default List<QmsIpqcSampleDO> selectListByIpqcId(Long ipqcId) {
        return selectList(new LambdaQueryWrapper<QmsIpqcSampleDO>()
                .eq(QmsIpqcSampleDO::getIpqcId, ipqcId)
                .orderByAsc(QmsIpqcSampleDO::getIpqcItemId)
                .orderByAsc(QmsIpqcSampleDO::getSampleSeq));
    }

    default int deleteByIpqcId(Long ipqcId) {
        return delete(new LambdaQueryWrapper<QmsIpqcSampleDO>()
                .eq(QmsIpqcSampleDO::getIpqcId, ipqcId));
    }

    default int deleteByIpqcIds(Collection<Long> ipqcIds) {
        return delete(new LambdaQueryWrapper<QmsIpqcSampleDO>()
                .in(QmsIpqcSampleDO::getIpqcId, ipqcIds));
    }
}
