package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewStatusLogDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierReviewStatusLogMapper extends BaseMapperX<SrmSupplierReviewStatusLogDO> {

    default List<SrmSupplierReviewStatusLogDO> selectListByMonthPlanId(Long monthPlanId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewStatusLogDO>()
                .eq(SrmSupplierReviewStatusLogDO::getMonthPlanId, monthPlanId)
                .orderByDesc(SrmSupplierReviewStatusLogDO::getCreateTime)
                .orderByDesc(SrmSupplierReviewStatusLogDO::getId));
    }

    default int deleteByMonthPlanIds(Collection<Long> monthPlanIds) {
        if (monthPlanIds == null || monthPlanIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<SrmSupplierReviewStatusLogDO>()
                .in(SrmSupplierReviewStatusLogDO::getMonthPlanId, monthPlanIds));
    }

}
