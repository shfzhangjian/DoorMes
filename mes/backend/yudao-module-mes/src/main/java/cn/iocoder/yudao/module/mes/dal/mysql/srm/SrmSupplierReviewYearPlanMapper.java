package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewYearPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierReviewYearPlanMapper extends BaseMapperX<SrmSupplierReviewYearPlanDO> {

    default SrmSupplierReviewYearPlanDO selectByPlanYear(Integer planYear) {
        return selectOne(SrmSupplierReviewYearPlanDO::getPlanYear, planYear);
    }

}
