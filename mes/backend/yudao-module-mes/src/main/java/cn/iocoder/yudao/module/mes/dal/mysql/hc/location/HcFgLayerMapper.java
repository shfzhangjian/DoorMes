package cn.iocoder.yudao.module.mes.dal.mysql.hc.location;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgLayerDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcFgLayerMapper extends BaseMapperX<HcFgLayerDO> {

    default List<HcFgLayerDO> selectListByRackIds(Collection<Long> rackIds) {
        return selectList(new LambdaQueryWrapperX<HcFgLayerDO>()
                .inIfPresent(HcFgLayerDO::getRackId, rackIds)
                .eq(HcFgLayerDO::getDeleted, false)
                .orderByAsc(HcFgLayerDO::getSortNo)
                .orderByAsc(HcFgLayerDO::getLayerNo)
                .orderByAsc(HcFgLayerDO::getId));
    }

    default List<HcFgLayerDO> selectListByRackId(Long rackId) {
        return selectListByRackIds(List.of(rackId));
    }

    @Select("SELECT * FROM mes_inv_fg_layer WHERE rack_id = #{rackId} AND layer_no = #{layerNo} LIMIT 1")
    HcFgLayerDO selectAnyByRackAndLayerNo(@Param("rackId") Long rackId,
                                           @Param("layerNo") Integer layerNo);

    @Update("UPDATE mes_inv_fg_layer SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreDeletedById(@Param("id") Long id);
}
