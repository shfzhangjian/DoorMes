package cn.iocoder.yudao.module.mes.dal.mysql.hc.massstock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.massstock.HcMassStockManualDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcMassStockManualMapper extends BaseMapperX<HcMassStockManualDO> {

    default HcMassStockManualDO selectByStockKey(Long tenantId, String modelCode, String motherBatchNo,
                                                 String motherSegmentBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcMassStockManualDO>()
                .eq(HcMassStockManualDO::getTenantId, tenantId)
                .eq(HcMassStockManualDO::getModelCode, modelCode)
                .eq(HcMassStockManualDO::getMotherBatchNo, motherBatchNo)
                .eq(HcMassStockManualDO::getMotherSegmentBatchNo, motherSegmentBatchNo)
                .eq(HcMassStockManualDO::getDeleted, false)
                .last("LIMIT 1"));
    }

}
