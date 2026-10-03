package cn.iocoder.yudao.module.mes.dal.mysql.hc.location;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcLocationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HcLocationMapper extends BaseMapperX<HcLocationDO> {

    default PageResult<HcLocationDO> selectPage(HcLocationPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcLocationDO> selectList(HcLocationPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default List<HcLocationDO> selectListByBizScene(String bizScene) {
        return selectList(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getBizScene, bizScene)
                .eq(HcLocationDO::getDeleted, false)
                .orderByAsc(HcLocationDO::getGridNo)
                .orderByAsc(HcLocationDO::getId));
    }

    default HcLocationDO selectByLocationCode(String locationCode) {
        return selectOne(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getLocationCode, locationCode)
                .eq(HcLocationDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcLocationDO selectByLocationCodeForUpdate(String locationCode) {
        return selectOne(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getLocationCode, locationCode)
                .eq(HcLocationDO::getDeleted, false)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default List<HcLocationDO> selectListByRackId(Long rackId) {
        return selectList(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getRackId, rackId)
                .eq(HcLocationDO::getBizScene, "PACKAGE_FG")
                .eq(HcLocationDO::getDeleted, false)
                .orderByAsc(HcLocationDO::getGridNo)
                .orderByAsc(HcLocationDO::getId));
    }

    default List<HcLocationDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getWarehouseId, warehouseId)
                .eq(HcLocationDO::getBizScene, "PACKAGE_FG")
                .eq(HcLocationDO::getDeleted, false)
                .orderByAsc(HcLocationDO::getGridNo)
                .orderByAsc(HcLocationDO::getId));
    }

    default List<HcLocationDO> selectListByWarehouseIdForUpdate(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getWarehouseId, warehouseId)
                .eq(HcLocationDO::getBizScene, "PACKAGE_FG")
                .eq(HcLocationDO::getDeleted, false)
                .orderByAsc(HcLocationDO::getGridNo)
                .orderByAsc(HcLocationDO::getId)
                .last("FOR UPDATE"));
    }

    default List<HcLocationDO> selectListByLayerId(Long layerId) {
        return selectList(new LambdaQueryWrapperX<HcLocationDO>()
                .eq(HcLocationDO::getLayerId, layerId)
                .eq(HcLocationDO::getBizScene, "PACKAGE_FG")
                .eq(HcLocationDO::getDeleted, false)
                .orderByAsc(HcLocationDO::getGridNo)
                .orderByAsc(HcLocationDO::getId));
    }

    /**
     * 查询库位编码对应的记录，包含逻辑删除数据。
     *
     * <p>库位编码在数据库中全局唯一，重新启用历史库位时必须先识别逻辑删除记录，不能直接插入。</p>
     */
    @Select("SELECT * FROM mes_inv_location WHERE location_code = #{locationCode} LIMIT 1")
    HcLocationDO selectAnyIncludingDeletedByLocationCode(@Param("locationCode") String locationCode);

    /**
     * 显式恢复逻辑删除库位。
     *
     * <p>不能使用 {@code updateById} 恢复，因为 MyBatis-Plus 会自动追加 {@code deleted = 0} 条件。</p>
     */
    @Update("UPDATE mes_inv_location SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreDeletedById(@Param("id") Long id);

    default LambdaQueryWrapperX<HcLocationDO> buildQuery(HcLocationPageReqVO reqVO) {
        LambdaQueryWrapperX<HcLocationDO> query = new LambdaQueryWrapperX<>();
        query.and(wrapper -> wrapper.isNull(HcLocationDO::getBizScene)
                .or().ne(HcLocationDO::getBizScene, "PACKAGE_FG"));
        return query
                .likeIfPresent(HcLocationDO::getLocationCode, reqVO.getLocationCode())
                .likeIfPresent(HcLocationDO::getLocationName, reqVO.getLocationName())
                .likeIfPresent(HcLocationDO::getWarehouseCode, reqVO.getWarehouseCode())
                .likeIfPresent(HcLocationDO::getWarehouseName, reqVO.getWarehouseName())
                .eqIfPresent(HcLocationDO::getLocationType, reqVO.getLocationType())
                .eqIfPresent(HcLocationDO::getMixBatchFlag, reqVO.getMixBatchFlag())
                .eqIfPresent(HcLocationDO::getMixModelFlag, reqVO.getMixModelFlag())
                .eqIfPresent(HcLocationDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcLocationDO::getId);
    }
}
