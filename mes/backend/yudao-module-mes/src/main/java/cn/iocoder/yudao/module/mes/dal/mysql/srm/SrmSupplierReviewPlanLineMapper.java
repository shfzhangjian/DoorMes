package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewPlanLineDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierReviewPlanLineMapper extends BaseMapperX<SrmSupplierReviewPlanLineDO> {

    default List<SrmSupplierReviewPlanLineDO> selectListByYearPlanId(Long yearPlanId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierReviewPlanLineDO>()
                .eq(SrmSupplierReviewPlanLineDO::getYearPlanId, yearPlanId)
                .orderByAsc(SrmSupplierReviewPlanLineDO::getRowNo)
                .orderByAsc(SrmSupplierReviewPlanLineDO::getId));
    }

    default List<SrmSupplierReviewPlanLineDO> selectListByIds(Collection<Long> ids) {
        return selectList(SrmSupplierReviewPlanLineDO::getId, ids);
    }

    default SrmSupplierReviewPlanLineDO selectDuplicate(Long yearPlanId, Long supplierId, String supplierCode) {
        if (yearPlanId == null) {
            return null;
        }
        LambdaQueryWrapperX<SrmSupplierReviewPlanLineDO> query = new LambdaQueryWrapperX<SrmSupplierReviewPlanLineDO>()
                .eq(SrmSupplierReviewPlanLineDO::getYearPlanId, yearPlanId);
        if (supplierCode != null && !supplierCode.isBlank()) {
            query.eq(SrmSupplierReviewPlanLineDO::getSupplierCode, supplierCode);
        } else if (supplierId != null) {
            query.eq(SrmSupplierReviewPlanLineDO::getSupplierId, supplierId);
        } else {
            return null;
        }
        return selectOne(query.last("LIMIT 1"));
    }

    default Integer selectNextRowNo(Long yearPlanId) {
        SrmSupplierReviewPlanLineDO line = selectOne(new LambdaQueryWrapperX<SrmSupplierReviewPlanLineDO>()
                .eq(SrmSupplierReviewPlanLineDO::getYearPlanId, yearPlanId)
                .orderByDesc(SrmSupplierReviewPlanLineDO::getRowNo)
                .last("LIMIT 1"));
        return line == null || line.getRowNo() == null ? 1 : line.getRowNo() + 1;
    }

}
