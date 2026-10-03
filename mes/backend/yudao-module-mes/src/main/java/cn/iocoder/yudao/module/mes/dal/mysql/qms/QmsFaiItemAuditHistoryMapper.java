package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemAuditHistoryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiItemAuditHistoryMapper extends BaseMapperX<QmsFaiItemAuditHistoryDO> {

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiItemAuditHistoryDO>()
                .eq(QmsFaiItemAuditHistoryDO::getFaiId, faiId));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiItemAuditHistoryDO>()
                .in(QmsFaiItemAuditHistoryDO::getFaiId, faiIds));
    }
}
