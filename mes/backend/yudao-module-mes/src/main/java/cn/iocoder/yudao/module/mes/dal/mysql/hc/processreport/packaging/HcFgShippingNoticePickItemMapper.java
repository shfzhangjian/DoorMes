package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticePickItemDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcFgShippingNoticePickItemMapper extends BaseMapperX<HcFgShippingNoticePickItemDO> {

    List<String> ACTIVE_STATUSES = List.of("PICKED", "SHIP_CONFIRMED", "INSPECTED", "OQC_INSPECTING",
            "OQC_PASSED", "OQC_REJECTED", "PACKAGED", "LOCKED", "OUTBOUND");

    default List<HcFgShippingNoticePickItemDO> selectListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByAsc(HcFgShippingNoticePickItemDO::getId));
    }

    default List<HcFgShippingNoticePickItemDO> selectActiveListByNoticeId(Long noticeId) {
        return selectList(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getNoticeId, noticeId)
                .in(HcFgShippingNoticePickItemDO::getLockStatus, ACTIVE_STATUSES)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByAsc(HcFgShippingNoticePickItemDO::getId));
    }

    default List<HcFgShippingNoticePickItemDO> selectActiveListByNoticeIdAndIds(Long noticeId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getNoticeId, noticeId)
                .in(HcFgShippingNoticePickItemDO::getId, ids)
                .in(HcFgShippingNoticePickItemDO::getLockStatus, ACTIVE_STATUSES)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByAsc(HcFgShippingNoticePickItemDO::getId));
    }

    default HcFgShippingNoticePickItemDO selectActiveByFinishedStockId(Long finishedStockId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getFinishedStockId, finishedStockId)
                .in(HcFgShippingNoticePickItemDO::getLockStatus, ACTIVE_STATUSES)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticePickItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcFgShippingNoticePickItemDO selectActiveBySourceInnerPackItemId(Long sourceInnerPackItemId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getSourceInnerPackItemId, sourceInnerPackItemId)
                .in(HcFgShippingNoticePickItemDO::getLockStatus, ACTIVE_STATUSES)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticePickItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcFgShippingNoticePickItemDO selectActiveBySourceCutRoundReportId(Long sourceCutRoundReportId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getSourceCutRoundReportId, sourceCutRoundReportId)
                .in(HcFgShippingNoticePickItemDO::getLockStatus, ACTIVE_STATUSES)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticePickItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcFgShippingNoticePickItemDO selectByNoticeIdAndActualSliceBatchNo(Long noticeId, String actualSliceBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticePickItemDO::getActualSliceBatchNo, actualSliceBatchNo)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticePickItemDO::getId)
                .last("LIMIT 1"));
    }

    default HcFgShippingNoticePickItemDO selectActiveByNoticeIdAndFinishedStockId(Long noticeId, Long finishedStockId) {
        return selectOne(new LambdaQueryWrapperX<HcFgShippingNoticePickItemDO>()
                .eq(HcFgShippingNoticePickItemDO::getNoticeId, noticeId)
                .eq(HcFgShippingNoticePickItemDO::getFinishedStockId, finishedStockId)
                .in(HcFgShippingNoticePickItemDO::getLockStatus, ACTIVE_STATUSES)
                .eq(HcFgShippingNoticePickItemDO::getDeleted, false)
                .orderByDesc(HcFgShippingNoticePickItemDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT COALESCE(SUM(COALESCE(locked_qty, 1)), 0)
            FROM mes_inv_fg_shipping_pick_item
            WHERE finished_stock_id = #{finishedStockId}
              AND lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'OQC_INSPECTING', 'OQC_PASSED', 'OQC_REJECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
              AND deleted = 0
            """)
    Integer selectActiveLockedQtyByFinishedStockId(@Param("finishedStockId") Long finishedStockId);

    @Delete("""
            DELETE FROM mes_inv_fg_shipping_pick_item
            WHERE notice_id = #{noticeId}
            """)
    int physicalDeleteByNoticeId(@Param("noticeId") Long noticeId);
}
