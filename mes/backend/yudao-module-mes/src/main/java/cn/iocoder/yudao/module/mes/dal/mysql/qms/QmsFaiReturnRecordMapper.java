package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiReturnRecordDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiReturnRecordMapper extends BaseMapperX<QmsFaiReturnRecordDO> {

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiReturnRecordDO>()
                .eq(QmsFaiReturnRecordDO::getFaiId, faiId));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiReturnRecordDO>()
                .in(QmsFaiReturnRecordDO::getFaiId, faiIds));
    }
}
