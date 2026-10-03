package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryRackDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcNgInventoryRackMapper extends BaseMapperX<HcNgInventoryRackDO> {

    default List<HcNgInventoryRackDO> selectListByWarehouseIds(Collection<Long> warehouseIds) {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryRackDO>()
                .inIfPresent(HcNgInventoryRackDO::getWarehouseId, warehouseIds)
                .eq(HcNgInventoryRackDO::getDeleted, false)
                .orderByAsc(HcNgInventoryRackDO::getSortNo)
                .orderByAsc(HcNgInventoryRackDO::getRackNo));
    }

    default HcNgInventoryRackDO selectByWarehouseAndNo(Long warehouseId, Integer rackNo) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryRackDO>()
                .eq(HcNgInventoryRackDO::getWarehouseId, warehouseId)
                .eq(HcNgInventoryRackDO::getRackNo, rackNo)
                .eq(HcNgInventoryRackDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcNgInventoryRackDO selectByWarehouseAndCode(Long warehouseId, String rackCode) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryRackDO>()
                .eq(HcNgInventoryRackDO::getWarehouseId, warehouseId)
                .eq(HcNgInventoryRackDO::getRackCode, rackCode)
                .eq(HcNgInventoryRackDO::getDeleted, false)
                .last("LIMIT 1"));
    }
}
