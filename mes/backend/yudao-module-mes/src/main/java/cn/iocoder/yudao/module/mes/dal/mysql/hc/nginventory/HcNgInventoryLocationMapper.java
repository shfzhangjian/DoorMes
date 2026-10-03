package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryLocationDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcNgInventoryLocationMapper extends BaseMapperX<HcNgInventoryLocationDO> {

    default List<HcNgInventoryLocationDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .orderByAsc(HcNgInventoryLocationDO::getGridNo)
                .orderByAsc(HcNgInventoryLocationDO::getId));
    }

    default List<HcNgInventoryLocationDO> selectActiveListForUpdate() {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .orderByAsc(HcNgInventoryLocationDO::getGridNo)
                .orderByAsc(HcNgInventoryLocationDO::getId)
                .last("FOR UPDATE"));
    }

    default HcNgInventoryLocationDO selectByLocationKeyForUpdate(String locationKey) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getLocationKey, locationKey)
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default HcNgInventoryLocationDO selectByWarehouseAndLocationCode(Long warehouseId, String locationCode) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getWarehouseId, warehouseId)
                .eq(HcNgInventoryLocationDO::getLocationCode, locationCode)
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcNgInventoryLocationDO> selectListByLocationKeys(Collection<String> locationKeys) {
        if (locationKeys == null || locationKeys.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .in(HcNgInventoryLocationDO::getLocationKey, locationKeys)
                .eq(HcNgInventoryLocationDO::getDeleted, false));
    }

    default List<HcNgInventoryLocationDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getWarehouseId, warehouseId)
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .orderByAsc(HcNgInventoryLocationDO::getGridNo)
                .orderByAsc(HcNgInventoryLocationDO::getId));
    }

    default List<HcNgInventoryLocationDO> selectListByRackId(Long rackId) {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getRackId, rackId)
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .orderByAsc(HcNgInventoryLocationDO::getGridNo)
                .orderByAsc(HcNgInventoryLocationDO::getId));
    }

    default HcNgInventoryLocationDO selectByRackAndLocationNo(Long rackId, Integer locationNo) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .eq(HcNgInventoryLocationDO::getRackId, rackId)
                .eq(HcNgInventoryLocationDO::getLocationNo, locationNo)
                .eq(HcNgInventoryLocationDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcNgInventoryLocationDO> selectListByIds(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryLocationDO>()
                .inIfPresent(HcNgInventoryLocationDO::getId, ids)
                .eq(HcNgInventoryLocationDO::getDeleted, false));
    }
}
