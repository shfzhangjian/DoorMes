package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** 更正与上架复用同一片记录/包装主表行锁。所有 SQL 显式限定租户。 */
@Mapper
public interface QmsNcClosedCorrectionMapper {
    @Select("SELECT * FROM mes_inv_ng_piece WHERE tenant_id = #{tenantId} AND piece_no = #{pieceNo} ORDER BY id FOR UPDATE")
    List<HcNgInventoryPieceDO> lockNgPieces(@Param("tenantId") Long tenantId, @Param("pieceNo") String pieceNo);

    @Select("""
        SELECT * FROM mes_sfc_inner_pack_unit WHERE tenant_id = #{tenantId} AND id IN
          (SELECT inner_unit_id FROM mes_sfc_inner_pack_unit_item
           WHERE tenant_id = #{tenantId} AND (slice_batch_no = #{pieceNo} OR production_batch_no = #{pieceNo}))
        ORDER BY id FOR UPDATE
        """)
    List<HcInnerPackUnitDO> lockPackages(@Param("tenantId") Long tenantId, @Param("pieceNo") String pieceNo);

    @Select("""
        SELECT COUNT(*) FROM mes_inv_finished_stock WHERE tenant_id = #{tenantId}
          AND slice_batch_no = #{pieceNo} AND (inbound_time IS NOT NULL OR stock_status <> 'INBOUND_LOCKED')
        """)
    int countShelvedStock(@Param("tenantId") Long tenantId, @Param("pieceNo") String pieceNo);

    @Update("""
        UPDATE mes_sfc_inner_pack_unit_item SET quality_status = #{quality}, updater = #{operator}, update_time = NOW()
        WHERE tenant_id = #{tenantId} AND deleted = 0
          AND (slice_batch_no = #{pieceNo} OR production_batch_no = #{pieceNo})
        """)
    int updatePackageQuality(@Param("tenantId") Long tenantId, @Param("pieceNo") String pieceNo,
                             @Param("quality") String quality, @Param("operator") String operator);

    @Update("""
        UPDATE mes_inv_finished_stock SET quality_status = #{quality}, updater = #{operator}, update_time = NOW()
        WHERE tenant_id = #{tenantId} AND deleted = 0 AND slice_batch_no = #{pieceNo}
          AND stock_status = 'INBOUND_LOCKED' AND inbound_time IS NULL
        """)
    int updatePendingStockQuality(@Param("tenantId") Long tenantId, @Param("pieceNo") String pieceNo,
                                  @Param("quality") String quality, @Param("operator") String operator);
}
