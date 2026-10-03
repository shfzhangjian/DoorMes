package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierResourceStatusLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierResourceStatusLogMapper extends BaseMapperX<SrmSupplierResourceStatusLogDO> {

    default List<SrmSupplierResourceStatusLogDO> selectBySupplier(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierResourceStatusLogDO>()
                .eq(SrmSupplierResourceStatusLogDO::getSourceType, "REGISTERED")
                .eqIfPresent(SrmSupplierResourceStatusLogDO::getSupplierId, supplierId)
                .orderByDesc(SrmSupplierResourceStatusLogDO::getId));
    }

}
