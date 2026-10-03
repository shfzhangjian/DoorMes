package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingPieceEventLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPackagingPieceEventLogMapper extends BaseMapperX<HcPackagingPieceEventLogDO> {

    default boolean existsByEventNo(String eventNo) {
        return selectCount(new LambdaQueryWrapperX<HcPackagingPieceEventLogDO>()
                .eq(HcPackagingPieceEventLogDO::getEventNo, eventNo)) > 0;
    }

    default boolean hasNgLifecycle(String sourceType, Long sourceRecordId) {
        return selectCount(new LambdaQueryWrapperX<HcPackagingPieceEventLogDO>()
                .eq(HcPackagingPieceEventLogDO::getSourceType, sourceType)
                .eq(HcPackagingPieceEventLogDO::getSourceRecordId, sourceRecordId)
                .eq(HcPackagingPieceEventLogDO::getNgRelated, true)) > 0;
    }
}
