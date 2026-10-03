package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockDO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcFinishedStockMapper extends BaseMapperX<HcFinishedStockDO> {

    default HcFinishedStockDO selectBySliceBatchNo(String sliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .eq(HcFinishedStockDO::getSliceBatchNo, sliceBatchNo)
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByDesc(HcFinishedStockDO::getId)
                .last("LIMIT 1"));
    }

    default HcFinishedStockDO selectAvailableBySliceBatchNo(String sliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .eq(HcFinishedStockDO::getSliceBatchNo, sliceBatchNo)
                .eq(HcFinishedStockDO::getStockStatus, "AVAILABLE")
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByDesc(HcFinishedStockDO::getId)
                .last("LIMIT 1"));
    }

    default Long selectActiveCountByLocationCode(String locationCode) {
        return selectCount(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .eq(HcFinishedStockDO::getLocationCode, locationCode)
                .in(HcFinishedStockDO::getStockStatus, List.of("INBOUND_LOCKED", "INBOUNDED", "AVAILABLE", "OUTBOUND_LOCKED", "ALLOCATED"))
                .eq(HcFinishedStockDO::getDeleted, false));
    }

    default List<HcFinishedStockDO> selectActiveListByLocationCodes(List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .in(HcFinishedStockDO::getLocationCode, locationCodes)
                .in(HcFinishedStockDO::getStockStatus, List.of("INBOUND_LOCKED", "INBOUNDED", "AVAILABLE", "OUTBOUND_LOCKED", "ALLOCATED"))
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByDesc(HcFinishedStockDO::getId));
    }

    default List<HcFinishedStockDO> selectActiveListByLocationCodesForUpdate(List<String> locationCodes) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .in(HcFinishedStockDO::getLocationCode, locationCodes)
                .in(HcFinishedStockDO::getStockStatus,
                        List.of("INBOUND_LOCKED", "INBOUNDED", "AVAILABLE", "OUTBOUND_LOCKED", "ALLOCATED"))
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByDesc(HcFinishedStockDO::getId)
                .last("FOR UPDATE"));
    }

    /**
     * 查询指定入库日期范围内仍在位的成品库存。
     *
     * <p>用于成品库位库存分析的汇总和分布图。结束日期使用左闭右开区间，避免丢失结束日内的入库记录。</p>
     */
    default List<HcFinishedStockDO> selectInventoryAnalysisActiveListByLocationCodes(
            List<String> locationCodes, LocalDate inboundDateStart, LocalDate inboundDateEnd) {
        if (locationCodes == null || locationCodes.isEmpty()) {
            return List.of();
        }
        LocalDateTime startTime = inboundDateStart.atStartOfDay();
        LocalDateTime endTimeExclusive = inboundDateEnd.plusDays(1).atStartOfDay();
        return selectList(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .in(HcFinishedStockDO::getLocationCode, locationCodes)
                .in(HcFinishedStockDO::getStockStatus,
                        List.of("INBOUND_LOCKED", "INBOUNDED", "AVAILABLE", "OUTBOUND_LOCKED", "ALLOCATED"))
                .ge(HcFinishedStockDO::getInboundTime, startTime)
                .lt(HcFinishedStockDO::getInboundTime, endTimeExclusive)
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByDesc(HcFinishedStockDO::getInboundTime)
                .orderByDesc(HcFinishedStockDO::getId));
    }

    default List<HcFinishedStockDO> selectListByInnerUnitNo(String innerUnitNo) {
        return selectList(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .eq(HcFinishedStockDO::getInnerUnitNo, innerUnitNo)
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByAsc(HcFinishedStockDO::getId));
    }

    /**
     * 按包装单号批量查询成品库存，供成品包装入库列表组装使用。
     *
     * <p>禁止在列表循环中逐包查询库存，否则分类视图会随历史包装数线性放大 SQL 次数。</p>
     */
    default List<HcFinishedStockDO> selectListByInnerUnitNos(Collection<String> innerUnitNos) {
        if (innerUnitNos == null || innerUnitNos.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcFinishedStockDO>()
                .in(HcFinishedStockDO::getInnerUnitNo, innerUnitNos)
                .eq(HcFinishedStockDO::getDeleted, false)
                .orderByAsc(HcFinishedStockDO::getId));
    }

    @Select("""
            SELECT *
            FROM mes_inv_finished_stock
            WHERE inner_unit_no = #{innerUnitNo}
              AND deleted = 0
            ORDER BY id ASC
            FOR UPDATE
            """)
    List<HcFinishedStockDO> selectListByInnerUnitNoForUpdate(@Param("innerUnitNo") String innerUnitNo);

    @Select("""
            SELECT COUNT(DISTINCT COALESCE(NULLIF(inner_unit_no, ''), NULLIF(outer_box_no, ''), stock_no))
            FROM mes_inv_finished_stock
            WHERE deleted = 0
              AND location_code = #{locationCode}
              AND stock_status IN ('INBOUND_LOCKED', 'INBOUNDED', 'AVAILABLE', 'OUTBOUND_LOCKED', 'ALLOCATED')
            """)
    Long selectActivePackageCountByLocationCode(@Param("locationCode") String locationCode);

    @Select("""
            SELECT CAST(COALESCE(SUM(CASE WHEN qty IS NULL OR qty <= 0 THEN 1 ELSE qty END), 0) AS SIGNED)
            FROM mes_inv_finished_stock
            WHERE deleted = 0
              AND location_code = #{locationCode}
              AND stock_status IN ('INBOUND_LOCKED', 'INBOUNDED', 'AVAILABLE', 'OUTBOUND_LOCKED', 'ALLOCATED')
            """)
    Long selectActivePieceQtyByLocationCode(@Param("locationCode") String locationCode);

    @Select("""
            SELECT *
            FROM mes_inv_finished_stock
            WHERE id = #{id}
            FOR UPDATE
            """)
    HcFinishedStockDO selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM mes_inv_finished_stock
            WHERE slice_batch_no = #{sliceBatchNo}
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            FOR UPDATE
            """)
    HcFinishedStockDO selectBySliceBatchNoForUpdate(@Param("sliceBatchNo") String sliceBatchNo);

    @Select("""
            SELECT *
            FROM mes_inv_finished_stock
            WHERE slice_batch_no = #{sliceBatchNo}
              AND deleted = 0
            ORDER BY id DESC
            FOR UPDATE
            """)
    List<HcFinishedStockDO> selectListBySliceBatchNoForUpdate(@Param("sliceBatchNo") String sliceBatchNo);

    @Delete("""
            <script>
            DELETE FROM mes_inv_finished_stock
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int deletePhysicallyByIds(@Param("ids") Collection<Long> ids);
}
