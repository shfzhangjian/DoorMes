package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dRelationDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Qms8dRelationMapper extends BaseMapperX<Qms8dRelationDO> {

    default List<Qms8dRelationDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<Qms8dRelationDO>()
                .eq(Qms8dRelationDO::getReportId, reportId)
                .orderByDesc(Qms8dRelationDO::getPrimaryFlag)
                .orderByAsc(Qms8dRelationDO::getId));
    }

    default Qms8dRelationDO selectByObjectNo(Long reportId, String relationType, String relatedObjectNo) {
        return selectOne(new LambdaQueryWrapperX<Qms8dRelationDO>()
                .eq(Qms8dRelationDO::getReportId, reportId)
                .eq(Qms8dRelationDO::getRelationType, relationType)
                .eq(Qms8dRelationDO::getRelatedObjectNo, relatedObjectNo));
    }

    default Qms8dRelationDO selectPrimaryByObject(String relationType, Long relatedObjectId, String relatedObjectNo) {
        return CollUtil.getFirst(selectList(new LambdaQueryWrapperX<Qms8dRelationDO>()
                .eq(Qms8dRelationDO::getRelationType, relationType)
                .eqIfPresent(Qms8dRelationDO::getRelatedObjectId, relatedObjectId)
                .eqIfPresent(Qms8dRelationDO::getRelatedObjectNo, relatedObjectNo)
                .eq(Qms8dRelationDO::getPrimaryFlag, true)
                .orderByDesc(Qms8dRelationDO::getId)));
    }

    default long selectCountByReportId(Long reportId) {
        return selectCount(Qms8dRelationDO::getReportId, reportId);
    }

    default void deleteByReportIdAndRelationType(Long reportId, String relationType) {
        if (reportId == null || relationType == null) {
            return;
        }
        delete(new LambdaQueryWrapperX<Qms8dRelationDO>()
                .eq(Qms8dRelationDO::getReportId, reportId)
                .eq(Qms8dRelationDO::getRelationType, relationType));
    }
}
