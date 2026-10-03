package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsProductEventRecheckGroupDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsProductEventRecheckGroupMapper extends BaseMapperX<QmsProductEventRecheckGroupDO> {

    default QmsProductEventRecheckGroupDO selectByRoot(String sourceType, Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsProductEventRecheckGroupDO>()
                .eq(QmsProductEventRecheckGroupDO::getSourceType, sourceType)
                .eq(QmsProductEventRecheckGroupDO::getRootInspectionId, inspectionId)
                .eq(QmsProductEventRecheckGroupDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default QmsProductEventRecheckGroupDO selectByLatest(String sourceType, Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsProductEventRecheckGroupDO>()
                .eq(QmsProductEventRecheckGroupDO::getSourceType, sourceType)
                .eq(QmsProductEventRecheckGroupDO::getLatestInspectionId, inspectionId)
                .eq(QmsProductEventRecheckGroupDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<QmsProductEventRecheckGroupDO> selectListBySourceTypes(Collection<String> sourceTypes) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsProductEventRecheckGroupDO>()
                .in(QmsProductEventRecheckGroupDO::getSourceType, sourceTypes)
                .eq(QmsProductEventRecheckGroupDO::getDeleted, false)
                .orderByDesc(QmsProductEventRecheckGroupDO::getUpdateTime)
                .orderByDesc(QmsProductEventRecheckGroupDO::getId));
    }
}
