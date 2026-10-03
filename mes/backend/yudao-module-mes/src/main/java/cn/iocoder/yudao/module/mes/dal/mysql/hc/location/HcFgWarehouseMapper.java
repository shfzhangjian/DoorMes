package cn.iocoder.yudao.module.mes.dal.mysql.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgWarehouseDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcFgWarehouseMapper extends BaseMapperX<HcFgWarehouseDO> {

    default List<HcFgWarehouseDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HcFgWarehouseDO>()
                .eq(HcFgWarehouseDO::getDeleted, false)
                .orderByAsc(HcFgWarehouseDO::getSortNo)
                .orderByAsc(HcFgWarehouseDO::getWarehouseCode)
                .orderByAsc(HcFgWarehouseDO::getId));
    }

    @Select("SELECT * FROM mes_inv_fg_warehouse WHERE warehouse_code = #{warehouseCode} LIMIT 1")
    HcFgWarehouseDO selectAnyByWarehouseCode(@Param("warehouseCode") String warehouseCode);

    @Update("UPDATE mes_inv_fg_warehouse SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreDeletedById(@Param("id") Long id);
}
