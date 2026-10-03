package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsSampleAbnormalLockDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsSampleAbnormalLockMapper extends BaseMapperX<QmsSampleAbnormalLockDO> {

    default java.util.List<QmsSampleAbnormalLockDO> selectEffectiveUpstreamLocks(
            java.util.Set<String> motherBatches, java.util.Set<String> segmentBatches, Long tenantId) {
        return selectList(new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getLockStatus, "LOCKED")
                .eq(QmsSampleAbnormalLockDO::getAbnormalResult, "NG")
                .eqIfPresent(QmsSampleAbnormalLockDO::getTenantId, tenantId)
                .isNotNull(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .and(scope -> scope.nested(wet -> wet
                        .eq(QmsSampleAbnormalLockDO::getSourceProcessCode, "WET")
                        .eq(QmsSampleAbnormalLockDO::getObjectType, "MOTHER_ROLL")
                        .in(QmsSampleAbnormalLockDO::getObjectNo, motherBatches))
                    .or(grinding -> grinding
                        .eq(QmsSampleAbnormalLockDO::getSourceProcessCode, "ROUGH_GRINDING")
                        .eq(QmsSampleAbnormalLockDO::getObjectType, "SEGMENT")
                        .in(QmsSampleAbnormalLockDO::getObjectNo, segmentBatches)))
                .orderByDesc(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .orderByDesc(QmsSampleAbnormalLockDO::getId));
    }

    default PageResult<QmsSampleAbnormalLockDO> selectPage(QmsSampleAbnormalLockPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .likeIfPresent(QmsSampleAbnormalLockDO::getLockNo, reqVO.getLockNo())
                .eqIfPresent(QmsSampleAbnormalLockDO::getLockStatus, reqVO.getLockStatus())
                .eqIfPresent(QmsSampleAbnormalLockDO::getObjectType, reqVO.getObjectType())
                .likeIfPresent(QmsSampleAbnormalLockDO::getObjectNo, reqVO.getObjectNo())
                .eqIfPresent(QmsSampleAbnormalLockDO::getSourceProcessCode, reqVO.getSourceProcessCode())
                .likeIfPresent(QmsSampleAbnormalLockDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(QmsSampleAbnormalLockDO::getAbnormalInspectionNo, reqVO.getAbnormalInspectionNo())
                .eqIfPresent(QmsSampleAbnormalLockDO::getAbnormalResult, reqVO.getAbnormalResult())
                .betweenIfPresent(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime, reqVO.getAbnormalFeedbackTime())
                .likeIfPresent(QmsSampleAbnormalLockDO::getRecheckInspectionNo, reqVO.getRecheckInspectionNo())
                .eqIfPresent(QmsSampleAbnormalLockDO::getRecheckResult, reqVO.getRecheckResult())
                .betweenIfPresent(QmsSampleAbnormalLockDO::getRecheckFeedbackTime, reqVO.getRecheckFeedbackTime())
                .orderByDesc(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .orderByDesc(QmsSampleAbnormalLockDO::getId));
    }

    default QmsSampleAbnormalLockDO selectByAbnormalInspection(Long abnormalInspectionId, String objectType,
                                                              String objectNo, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getAbnormalInspectionId, abnormalInspectionId)
                .eq(QmsSampleAbnormalLockDO::getObjectType, objectType)
                .eq(QmsSampleAbnormalLockDO::getObjectNo, objectNo)
                .eqIfPresent(QmsSampleAbnormalLockDO::getTenantId, tenantId)
                .last("LIMIT 1"));
    }

    default QmsSampleAbnormalLockDO selectLatestLocked(String sourceProcessCode, String objectType,
                                                      String objectNo, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getLockStatus, "LOCKED")
                .eq(QmsSampleAbnormalLockDO::getSourceProcessCode, sourceProcessCode)
                .eq(QmsSampleAbnormalLockDO::getObjectType, objectType)
                .eq(QmsSampleAbnormalLockDO::getObjectNo, objectNo)
                .eqIfPresent(QmsSampleAbnormalLockDO::getTenantId, tenantId)
                .orderByDesc(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .orderByDesc(QmsSampleAbnormalLockDO::getId)
                .last("LIMIT 1"));
    }

    default QmsSampleAbnormalLockDO selectLatestEffectiveLocked(String sourceProcessCode, String objectType,
                                                               String objectNo, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getLockStatus, "LOCKED")
                .eq(QmsSampleAbnormalLockDO::getSourceProcessCode, sourceProcessCode)
                .eq(QmsSampleAbnormalLockDO::getObjectType, objectType)
                .eq(QmsSampleAbnormalLockDO::getObjectNo, objectNo)
                .eq(QmsSampleAbnormalLockDO::getAbnormalResult, "NG")
                .eqIfPresent(QmsSampleAbnormalLockDO::getTenantId, tenantId)
                .isNotNull(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .orderByDesc(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .orderByDesc(QmsSampleAbnormalLockDO::getId)
                .last("LIMIT 1"));
    }

    default QmsSampleAbnormalLockDO selectLatestByRecheckInspection(Long recheckInspectionId, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getRecheckInspectionId, recheckInspectionId)
                .eqIfPresent(QmsSampleAbnormalLockDO::getTenantId, tenantId)
                .orderByDesc(QmsSampleAbnormalLockDO::getId)
                .last("LIMIT 1"));
    }

    default QmsSampleAbnormalLockDO selectLatestByRecheckInspection(String recheckInspectionType,
                                                                    Long recheckInspectionId, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getRecheckInspectionType, recheckInspectionType)
                .eq(QmsSampleAbnormalLockDO::getRecheckInspectionId, recheckInspectionId)
                .eqIfPresent(QmsSampleAbnormalLockDO::getTenantId, tenantId)
                .orderByDesc(QmsSampleAbnormalLockDO::getId)
                .last("LIMIT 1"));
    }
}
