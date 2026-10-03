package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcReturnRecordDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcReturnRecordMapper extends BaseMapperX<QmsFqcReturnRecordDO> {

    default int deleteByFqcId(Long FqcId) {
        return delete(new LambdaQueryWrapper<QmsFqcReturnRecordDO>()
                .eq(QmsFqcReturnRecordDO::getFqcId, FqcId));
    }

    default int deleteByFqcIds(Collection<Long> FqcIds) {
        return delete(new LambdaQueryWrapper<QmsFqcReturnRecordDO>()
                .in(QmsFqcReturnRecordDO::getFqcId, FqcIds));
    }
}
