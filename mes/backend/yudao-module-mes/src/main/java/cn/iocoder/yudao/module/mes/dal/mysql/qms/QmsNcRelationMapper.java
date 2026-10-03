package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRelationDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QmsNcRelationMapper extends BaseMapperX<QmsNcRelationDO> {

    default List<QmsNcRelationDO> selectListByNcRecordId(Long ncRecordId) {
        return selectList(new LambdaQueryWrapperX<QmsNcRelationDO>()
                .eq(QmsNcRelationDO::getNcRecordId, ncRecordId)
                .orderByDesc(QmsNcRelationDO::getPrimaryFlag)
                .orderByDesc(QmsNcRelationDO::getRelationTime));
    }

    default QmsNcRelationDO selectByObjectNo(Long ncRecordId, String relationType, String relatedObjectNo) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRelationDO>()
                .eq(QmsNcRelationDO::getNcRecordId, ncRecordId)
                .eq(QmsNcRelationDO::getRelationType, relationType)
                .eq(QmsNcRelationDO::getRelatedObjectNo, relatedObjectNo));
    }

    default QmsNcRelationDO selectAttachment(Long ncRecordId, String relationType,
                                              String attachmentKey, String attachmentUrl) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRelationDO>()
                .eq(QmsNcRelationDO::getNcRecordId, ncRecordId)
                .eq(QmsNcRelationDO::getRelationType, relationType)
                .and(wrapper -> wrapper.eq(QmsNcRelationDO::getRelatedObjectNo, attachmentKey)
                        .or().eq(QmsNcRelationDO::getRemark, attachmentUrl))
                .last("LIMIT 1"));
    }

    default QmsNcRelationDO selectPrimaryRelation(Long ncRecordId, String relationType) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRelationDO>()
                .eq(QmsNcRelationDO::getNcRecordId, ncRecordId)
                .eq(QmsNcRelationDO::getRelationType, relationType)
                .eq(QmsNcRelationDO::getPrimaryFlag, true)
                .last("LIMIT 1"));
    }

    default QmsNcRelationDO selectPrimaryByRelatedObject(String relationType, Long relatedObjectId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRelationDO>()
                .eq(QmsNcRelationDO::getRelationType, relationType)
                .eq(QmsNcRelationDO::getRelatedObjectId, relatedObjectId)
                .eq(QmsNcRelationDO::getPrimaryFlag, true)
                .last("LIMIT 1"));
    }

    default QmsNcRelationDO selectByRelatedObject(String relationType, Long relatedObjectId) {
        return selectOne(new LambdaQueryWrapperX<QmsNcRelationDO>()
                .eq(QmsNcRelationDO::getRelationType, relationType)
                .eq(QmsNcRelationDO::getRelatedObjectId, relatedObjectId)
                .orderByDesc(QmsNcRelationDO::getPrimaryFlag)
                .orderByDesc(QmsNcRelationDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("""
            DELETE FROM mes_qms_nc_relation
            WHERE nc_record_id = #{ncRecordId}
              AND tenant_id = #{tenantId}
            """)
    int physicalDeleteByNcRecordId(@Param("ncRecordId") Long ncRecordId,
                                   @Param("tenantId") Long tenantId);
}
