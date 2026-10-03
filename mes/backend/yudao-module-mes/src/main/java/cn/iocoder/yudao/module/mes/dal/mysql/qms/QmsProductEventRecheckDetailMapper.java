package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsProductEventRecheckDetailDO;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsProductEventRecheckDetailMapper extends BaseMapperX<QmsProductEventRecheckDetailDO> {

    default QmsProductEventRecheckDetailDO selectByInspection(String sourceType, Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsProductEventRecheckDetailDO>()
                .eq(QmsProductEventRecheckDetailDO::getSourceType, sourceType)
                .eq(QmsProductEventRecheckDetailDO::getInspectionId, inspectionId)
                .eq(QmsProductEventRecheckDetailDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<QmsProductEventRecheckDetailDO> selectListByGroupId(Long groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsProductEventRecheckDetailDO>()
                .eq(QmsProductEventRecheckDetailDO::getGroupId, groupId)
                .eq(QmsProductEventRecheckDetailDO::getDeleted, false)
                .orderByAsc(QmsProductEventRecheckDetailDO::getRoundNo)
                .orderByAsc(QmsProductEventRecheckDetailDO::getId));
    }

    default QmsProductEventRecheckDetailDO selectLatestByGroupId(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<QmsProductEventRecheckDetailDO>()
                .eq(QmsProductEventRecheckDetailDO::getGroupId, groupId)
                .eq(QmsProductEventRecheckDetailDO::getDeleted, false)
                .orderByDesc(QmsProductEventRecheckDetailDO::getRoundNo)
                .orderByDesc(QmsProductEventRecheckDetailDO::getId)
                .last("LIMIT 1"));
    }
}
