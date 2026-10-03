package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcFgShippingNoticeItemMapper extends BaseMapperX<HcFgShippingNoticeItemDO> {

    default List<HcFgShippingNoticeItemDO> selectListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticeItemDO::getDeleted, false)
                .orderByAsc(HcFgShippingNoticeItemDO::getId));
    }

    default List<HcFgShippingNoticeItemDO> selectLockedListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getNoticeId, noticeId)
                .in(HcFgShippingNoticeItemDO::getLockStatus, List.of("SUBMITTED", "PICKED", "INSPECTED", "OQC_INSPECTING",
                        "OQC_PASSED", "OQC_REJECTED", "PACKAGED", "LOCKED", "OUTBOUND"))
                .eq(HcFgShippingNoticeItemDO::getDeleted, false)
                .orderByAsc(HcFgShippingNoticeItemDO::getId));
    }

    default HcFgShippingNoticeItemDO selectLatestPackagedByNoticeId(Long noticeId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getNoticeId, noticeId)
                .isNotNull(HcFgShippingNoticeItemDO::getShippingPackageTime)
                .eq(HcFgShippingNoticeItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticeItemDO::getShippingPackageTime)
                .orderByDesc(HcFgShippingNoticeItemDO::getId)
                .last("LIMIT 1"));
    }

    default Integer countShippingInspectionCompletedByNoticeId(Long noticeId) {
        Long count = selectCount(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getNoticeId, noticeId)
                .isNotNull(HcFgShippingNoticeItemDO::getActualSliceBatchNo)
                .eq(HcFgShippingNoticeItemDO::getShippingInspectionResult, "OK")
                .eq(HcFgShippingNoticeItemDO::getDeleted, false));
        return count == null ? 0 : count.intValue();
    }

    @Delete("""
            DELETE FROM mes_inv_fg_shipping_notice_item
            WHERE notice_id = #{noticeId}
            """)
    int physicalDeleteByNoticeId(@Param("noticeId") Long noticeId);

    default HcFgShippingNoticeItemDO selectActiveByFinishedStockId(Long finishedStockId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getFinishedStockId, finishedStockId)
                .in(HcFgShippingNoticeItemDO::getLockStatus, List.of("PICKED", "INSPECTED", "OQC_INSPECTING",
                        "OQC_PASSED", "OQC_REJECTED", "PACKAGED", "LOCKED", "OUTBOUND"))
                .eq(HcFgShippingNoticeItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticeItemDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT COALESCE(SUM(COALESCE(locked_qty, 1)), 0)
            FROM mes_inv_fg_shipping_notice_item
            WHERE finished_stock_id = #{finishedStockId}
              AND lock_status IN ('PICKED', 'INSPECTED', 'OQC_INSPECTING', 'OQC_PASSED', 'OQC_REJECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
              AND deleted = 0
            """)
    Integer selectActiveLockedQtyByFinishedStockId(@Param("finishedStockId") Long finishedStockId);

    default HcFgShippingNoticeItemDO selectActiveByNoticeIdAndFinishedStockId(Long noticeId, Long finishedStockId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticeItemDO::getFinishedStockId, finishedStockId)
                .in(HcFgShippingNoticeItemDO::getLockStatus, List.of("PICKED", "INSPECTED", "OQC_INSPECTING",
                        "OQC_PASSED", "OQC_REJECTED", "PACKAGED", "LOCKED", "OUTBOUND"))
                .eq(HcFgShippingNoticeItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticeItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcFgShippingNoticeItemDO selectByNoticeIdAndActualSliceBatchNo(Long noticeId, String actualSliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticeItemDO::getActualSliceBatchNo, actualSliceBatchNo)
                .eq(HcFgShippingNoticeItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticeItemDO::getId)
                .last("LIMIT 1"));
    }

    @Update("""
            UPDATE mes_inv_fg_shipping_notice_item
            SET actual_finished_stock_id = NULL,
                actual_stock_no = NULL,
                outer_box_no = NULL,
                inner_unit_no = NULL,
                package_no = NULL,
                slice_batch_no = package_slice_no,
                actual_slice_batch_no = NULL,
                stock_qty = 1,
                available_qty = 1,
                quality_status = NULL,
                warehouse_code = NULL,
                warehouse_name = NULL,
                location_code = NULL,
                location_name = NULL,
                actual_location_code = NULL,
                actual_location_name = NULL,
                inbound_no = NULL,
                inbound_time = NULL,
                actual_ship_qty = 0,
                shipping_quality_no = NULL,
                oqc_order_id = NULL,
                oqc_status = NULL,
                shipping_inspector_name = NULL,
                shipping_inspection_result = NULL,
                shipping_inspection_remark = NULL,
                shipping_inspection_time = NULL,
                shipping_package_name = NULL,
                shipping_package_time = NULL,
                shipping_package_remark = NULL,
                shipped_name = NULL,
                shipped_time = NULL,
                lock_status = #{lockStatus},
                lock_name = #{operatorName},
                lock_time = #{lockTime},
                update_time = NOW()
            WHERE id = #{id}
              AND notice_id = #{noticeId}
              AND deleted = 0
            """)
    int clearReturnedPickActual(@Param("id") Long id,
                                @Param("noticeId") Long noticeId,
                                @Param("lockStatus") String lockStatus,
                                @Param("operatorName") String operatorName,
                                @Param("lockTime") java.time.LocalDateTime lockTime);
}
