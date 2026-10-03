package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemGroupAuditDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiItemGroupAuditMapper extends BaseMapperX<QmsFaiItemGroupAuditDO> {

    default List<QmsFaiItemGroupAuditDO> selectListByFaiId(Long faiId) {
        return selectList(new LambdaQueryWrapper<QmsFaiItemGroupAuditDO>()
                .eq(QmsFaiItemGroupAuditDO::getFaiId, faiId)
                .orderByAsc(QmsFaiItemGroupAuditDO::getFaiItemId)
                .orderByAsc(QmsFaiItemGroupAuditDO::getGroupKey));
    }

    default QmsFaiItemGroupAuditDO selectByGroup(Long faiId, Long faiItemId, String groupKey) {
        return selectOne(new LambdaQueryWrapper<QmsFaiItemGroupAuditDO>()
                .eq(QmsFaiItemGroupAuditDO::getFaiId, faiId)
                .eq(QmsFaiItemGroupAuditDO::getFaiItemId, faiItemId)
                .eq(QmsFaiItemGroupAuditDO::getGroupKey, groupKey));
    }

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiItemGroupAuditDO>()
                .eq(QmsFaiItemGroupAuditDO::getFaiId, faiId));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiItemGroupAuditDO>()
                .in(QmsFaiItemGroupAuditDO::getFaiId, faiIds));
    }
}
