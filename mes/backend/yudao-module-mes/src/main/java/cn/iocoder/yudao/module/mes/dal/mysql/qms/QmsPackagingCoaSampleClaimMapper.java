package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsPackagingCoaSampleClaimDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsPackagingCoaSampleClaimMapper extends BaseMapperX<QmsPackagingCoaSampleClaimDO> {

    default List<QmsPackagingCoaSampleClaimDO> selectListByFaiIds(Collection<Long> faiIds) {
        if (faiIds == null || faiIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsPackagingCoaSampleClaimDO>()
                .in(QmsPackagingCoaSampleClaimDO::getFaiId, faiIds)
                .eq(QmsPackagingCoaSampleClaimDO::getDeleted, false)
                .orderByAsc(QmsPackagingCoaSampleClaimDO::getId));
    }

    default List<QmsPackagingCoaSampleClaimDO> selectListBySource(String sourceType,
                                                                    Collection<Long> sourceRecordIds) {
        if (sourceRecordIds == null || sourceRecordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsPackagingCoaSampleClaimDO>()
                .eq(QmsPackagingCoaSampleClaimDO::getSourceType, sourceType)
                .in(QmsPackagingCoaSampleClaimDO::getSourceRecordId, sourceRecordIds)
                .eq(QmsPackagingCoaSampleClaimDO::getDeleted, false));
    }
}
