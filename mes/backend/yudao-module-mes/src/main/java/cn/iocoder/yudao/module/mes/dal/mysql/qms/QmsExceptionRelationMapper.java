package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionRelationDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsExceptionRelationMapper extends BaseMapperX<QmsExceptionRelationDO> {

    default List<QmsExceptionRelationDO> selectListByExceptionId(Long exceptionId) {
        return selectList(new LambdaQueryWrapperX<QmsExceptionRelationDO>()
                .eq(QmsExceptionRelationDO::getExceptionId, exceptionId)
                .orderByDesc(QmsExceptionRelationDO::getPrimaryFlag)
                .orderByDesc(QmsExceptionRelationDO::getRelationTime));
    }

    default QmsExceptionRelationDO selectByObjectNo(Long exceptionId, String relationType, String relatedObjectNo) {
        return selectOne(new LambdaQueryWrapperX<QmsExceptionRelationDO>()
                .eq(QmsExceptionRelationDO::getExceptionId, exceptionId)
                .eq(QmsExceptionRelationDO::getRelationType, relationType)
                .eq(QmsExceptionRelationDO::getRelatedObjectNo, relatedObjectNo));
    }

    default QmsExceptionRelationDO selectPrimaryRelation(Long exceptionId, String relationType) {
        return selectOne(new LambdaQueryWrapperX<QmsExceptionRelationDO>()
                .eq(QmsExceptionRelationDO::getExceptionId, exceptionId)
                .eq(QmsExceptionRelationDO::getRelationType, relationType)
                .eq(QmsExceptionRelationDO::getPrimaryFlag, true)
                .last("LIMIT 1"));
    }

    default Long selectCountByType(Long exceptionId, String relationType) {
        return selectCount(new LambdaQueryWrapperX<QmsExceptionRelationDO>()
                .eq(QmsExceptionRelationDO::getExceptionId, exceptionId)
                .eq(QmsExceptionRelationDO::getRelationType, relationType));
    }

    default void deleteByExceptionIdAndRelationTypes(Long exceptionId, Collection<String> relationTypes) {
        if (relationTypes == null || relationTypes.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<QmsExceptionRelationDO>()
                .eq(QmsExceptionRelationDO::getExceptionId, exceptionId)
                .in(QmsExceptionRelationDO::getRelationType, relationTypes));
    }
}
