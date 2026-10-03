package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewReplyDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierReviewReplyMapper extends BaseMapperX<SrmSupplierReviewReplyDO> {

    default List<SrmSupplierReviewReplyDO> selectListByMonthPlanId(Long monthPlanId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewReplyDO>()
                .eq(SrmSupplierReviewReplyDO::getMonthPlanId, monthPlanId)
                .orderByDesc(SrmSupplierReviewReplyDO::getReplyTime)
                .orderByDesc(SrmSupplierReviewReplyDO::getId));
    }

    default int deleteByMonthPlanIds(Collection<Long> monthPlanIds) {
        if (monthPlanIds == null || monthPlanIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<SrmSupplierReviewReplyDO>()
                .in(SrmSupplierReviewReplyDO::getMonthPlanId, monthPlanIds));
    }

}
