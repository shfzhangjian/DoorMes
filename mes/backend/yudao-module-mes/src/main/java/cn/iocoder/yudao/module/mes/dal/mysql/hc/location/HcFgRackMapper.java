package cn.iocoder.yudao.module.mes.dal.mysql.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgRackDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcFgRackMapper extends BaseMapperX<HcFgRackDO> {

    default List<HcFgRackDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<HcFgRackDO>()
                .eq(HcFgRackDO::getWarehouseId, warehouseId)
                .eq(HcFgRackDO::getDeleted, false)
                .orderByAsc(HcFgRackDO::getSortNo)
                .orderByAsc(HcFgRackDO::getRackNo)
                .orderByAsc(HcFgRackDO::getId));
    }

    @Select("SELECT * FROM mes_inv_fg_rack WHERE warehouse_id = #{warehouseId} AND rack_no = #{rackNo} LIMIT 1")
    HcFgRackDO selectAnyByWarehouseAndRackNo(@Param("warehouseId") Long warehouseId,
                                              @Param("rackNo") Integer rackNo);

    @Update("UPDATE mes_inv_fg_rack SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreDeletedById(@Param("id") Long id);
}
