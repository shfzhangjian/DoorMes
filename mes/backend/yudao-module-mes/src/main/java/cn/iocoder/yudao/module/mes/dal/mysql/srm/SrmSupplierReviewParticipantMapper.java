package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewParticipantDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierReviewParticipantMapper extends BaseMapperX<SrmSupplierReviewParticipantDO> {

    default List<SrmSupplierReviewParticipantDO> selectListByMonthPlanId(Long monthPlanId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewParticipantDO>()
                .eq(SrmSupplierReviewParticipantDO::getMonthPlanId, monthPlanId)
                .orderByAsc(SrmSupplierReviewParticipantDO::getRelationType)
                .orderByAsc(SrmSupplierReviewParticipantDO::getUserName)
                .orderByAsc(SrmSupplierReviewParticipantDO::getId));
    }

    default List<SrmSupplierReviewParticipantDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewParticipantDO>()
                .eq(SrmSupplierReviewParticipantDO::getUserId, userId)
                .orderByDesc(SrmSupplierReviewParticipantDO::getPlanYear)
                .orderByAsc(SrmSupplierReviewParticipantDO::getPlanMonth));
    }

    default void deleteByMonthPlanId(Long monthPlanId) {
        delete(SrmSupplierReviewParticipantDO::getMonthPlanId, monthPlanId);
    }

    default int deleteByMonthPlanIds(Collection<Long> monthPlanIds) {
        if (monthPlanIds == null || monthPlanIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<SrmSupplierReviewParticipantDO>()
                .in(SrmSupplierReviewParticipantDO::getMonthPlanId, monthPlanIds));
    }

}
