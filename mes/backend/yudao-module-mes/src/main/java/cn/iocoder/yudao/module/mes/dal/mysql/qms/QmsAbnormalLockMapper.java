package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsAbnormalLockDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsAbnormalLockMapper extends BaseMapperX<QmsAbnormalLockDO> {

    default PageResult<QmsAbnormalLockDO> selectPage(QmsAbnormalLockPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsAbnormalLockDO>()
                .likeIfPresent(QmsAbnormalLockDO::getLockNo, reqVO.getLockNo())
                .eqIfPresent(QmsAbnormalLockDO::getInspectionOrderType, reqVO.getInspectionOrderType())
                .likeIfPresent(QmsAbnormalLockDO::getInspectionOrderNo, reqVO.getInspectionOrderNo())
                .eqIfPresent(QmsAbnormalLockDO::getLockSourceType, reqVO.getLockSourceType())
                .eqIfPresent(QmsAbnormalLockDO::getLockScope, reqVO.getLockScope())
                .eqIfPresent(QmsAbnormalLockDO::getLockStatus, reqVO.getLockStatus())
                .likeIfPresent(QmsAbnormalLockDO::getAffectedBatchNo, reqVO.getAffectedBatchNo())
                .likeIfPresent(QmsAbnormalLockDO::getSourceBatchNo, reqVO.getSourceBatchNo())
                .likeIfPresent(QmsAbnormalLockDO::getProductionBatchNo, reqVO.getProductionBatchNo())
                .likeIfPresent(QmsAbnormalLockDO::getParentProductionBatchNo, reqVO.getParentProductionBatchNo())
                .likeIfPresent(QmsAbnormalLockDO::getGlueBoardBatchNo, reqVO.getGlueBoardBatchNo())
                .likeIfPresent(QmsAbnormalLockDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(QmsAbnormalLockDO::getOperationCode, reqVO.getOperationCode())
                .likeIfPresent(QmsAbnormalLockDO::getOperationName, reqVO.getOperationName())
                .betweenIfPresent(QmsAbnormalLockDO::getLockTime, reqVO.getLockTime())
                .orderByDesc(QmsAbnormalLockDO::getLockTime)
                .orderByDesc(QmsAbnormalLockDO::getId));
    }

    default QmsAbnormalLockDO selectByUniqueKey(String inspectionOrderType, Long inspectionOrderId,
                                                Long tenantId,
                                                String affectedSourceKey) {
        return selectOne(new LambdaQueryWrapperX<QmsAbnormalLockDO>()
                .eq(QmsAbnormalLockDO::getInspectionOrderType, inspectionOrderType)
                .eq(QmsAbnormalLockDO::getInspectionOrderId, inspectionOrderId)
                .eqIfPresent(QmsAbnormalLockDO::getTenantId, tenantId)
                .eq(QmsAbnormalLockDO::getAffectedSourceKey, affectedSourceKey)
                .last("LIMIT 1"));
    }
}
