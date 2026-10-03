package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewMonthPlanDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierReviewMonthPlanMapper extends BaseMapperX<SrmSupplierReviewMonthPlanDO> {

    default List<SrmSupplierReviewMonthPlanDO> selectListByYearPlanId(Long yearPlanId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewMonthPlanDO>()
                .eq(SrmSupplierReviewMonthPlanDO::getYearPlanId, yearPlanId)
                .orderByAsc(SrmSupplierReviewMonthPlanDO::getLineId)
                .orderByAsc(SrmSupplierReviewMonthPlanDO::getPlanMonth));
    }

    default List<SrmSupplierReviewMonthPlanDO> selectListByLineId(Long lineId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewMonthPlanDO>()
                .eq(SrmSupplierReviewMonthPlanDO::getLineId, lineId)
                .orderByAsc(SrmSupplierReviewMonthPlanDO::getPlanMonth));
    }

    default List<SrmSupplierReviewMonthPlanDO> selectListByLineIds(Collection<Long> lineIds) {
        if (lineIds == null || lineIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewMonthPlanDO>()
                .in(SrmSupplierReviewMonthPlanDO::getLineId, lineIds)
                .orderByAsc(SrmSupplierReviewMonthPlanDO::getLineId)
                .orderByAsc(SrmSupplierReviewMonthPlanDO::getPlanMonth));
    }

    default List<SrmSupplierReviewMonthPlanDO> selectListByIds(Collection<Long> ids) {
        return selectList(SrmSupplierReviewMonthPlanDO::getId, ids);
    }

    default int deleteByLineIds(Collection<Long> lineIds) {
        if (lineIds == null || lineIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<SrmSupplierReviewMonthPlanDO>()
                .in(SrmSupplierReviewMonthPlanDO::getLineId, lineIds));
    }

    default int clearMonthPlan(Long id) {
        return update(null, new LambdaUpdateWrapper<SrmSupplierReviewMonthPlanDO>()
                .eq(SrmSupplierReviewMonthPlanDO::getId, id)
                .set(SrmSupplierReviewMonthPlanDO::getPlannedFlag, false)
                .set(SrmSupplierReviewMonthPlanDO::getExecutionStatus, null)
                .set(SrmSupplierReviewMonthPlanDO::getPlanDesc, null)
                .set(SrmSupplierReviewMonthPlanDO::getLeadUserId, null)
                .set(SrmSupplierReviewMonthPlanDO::getLeadUserName, null)
                .set(SrmSupplierReviewMonthPlanDO::getRelatedUserIds, null)
                .set(SrmSupplierReviewMonthPlanDO::getRelatedUserNames, null)
                .set(SrmSupplierReviewMonthPlanDO::getAuditDate, null)
                .set(SrmSupplierReviewMonthPlanDO::getApproverUserId, null)
                .set(SrmSupplierReviewMonthPlanDO::getApproverUserName, null)
                .set(SrmSupplierReviewMonthPlanDO::getApprovalOpinion, null)
                .set(SrmSupplierReviewMonthPlanDO::getApprovalTime, null)
                .set(SrmSupplierReviewMonthPlanDO::getStatusRemark, null)
                .set(SrmSupplierReviewMonthPlanDO::getUpdateDescription, null)
                .set(SrmSupplierReviewMonthPlanDO::getRemark, null)
                .setSql("version = IFNULL(version, 0) + 1"));
    }

}
