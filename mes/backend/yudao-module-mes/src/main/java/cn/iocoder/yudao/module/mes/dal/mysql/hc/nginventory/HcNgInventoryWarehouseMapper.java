package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryWarehouseDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcNgInventoryWarehouseMapper extends BaseMapperX<HcNgInventoryWarehouseDO> {

    default List<HcNgInventoryWarehouseDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HcNgInventoryWarehouseDO>()
                .eq(HcNgInventoryWarehouseDO::getDeleted, false)
                .orderByAsc(HcNgInventoryWarehouseDO::getSortNo)
                .orderByAsc(HcNgInventoryWarehouseDO::getWarehouseCode));
    }

    default HcNgInventoryWarehouseDO selectByPadType(String padType) {
        return selectOne(new LambdaQueryWrapperX<HcNgInventoryWarehouseDO>()
                .eq(HcNgInventoryWarehouseDO::getPadType, padType)
                .eq(HcNgInventoryWarehouseDO::getDeleted, false)
                .last("LIMIT 1"));
    }
}
