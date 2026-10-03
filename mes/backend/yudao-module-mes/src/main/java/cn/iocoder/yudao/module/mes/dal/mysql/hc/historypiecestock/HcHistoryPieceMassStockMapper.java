package cn.iocoder.yudao.module.mes.dal.mysql.hc.historypiecestock;

import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HcHistoryPieceMassStockMapper {

    String AGGREGATE_SQL = """
            SELECT CONCAT(sourceRows.modelCode, '|', sourceRows.segmentBatchNo) AS rowKey,
                   sourceRows.modelCode AS modelCode,
                   sourceRows.segmentBatchNo AS segmentBatchNo,
                   SUM(sourceRows.goodStockQty) AS goodStockQty,
                   MIN(sourceRows.productionDate) AS earliestProductionDate,
                   MAX(sourceRows.productionDate) AS latestProductionDate,
                   NULLIF(GROUP_CONCAT(DISTINCT sourceRows.remark ORDER BY sourceRows.remark SEPARATOR '；'), '') AS remark
              FROM (
                    SELECT COALESCE(NULLIF(TRIM(manualPiece.model_code), ''),
                                    NULLIF(TRIM(finishedStock.model_code), '')) AS modelCode,
                           TRIM(manualPiece.segment_batch_no) AS segmentBatchNo,
                           COALESCE(finishedStock.qty, 0) AS goodStockQty,
                           COALESCE(finishedStock.production_date, manualPiece.production_date) AS productionDate,
                           NULLIF(TRIM(manualPiece.remark), '') AS remark
                      FROM mes_sfc_packaging_manual_piece manualPiece
                      INNER JOIN mes_sfc_inner_pack_unit_item innerPackItem
                              ON innerPackItem.source_manual_piece_id = manualPiece.id
                             AND innerPackItem.source_type = 'MANUAL_HISTORY'
                             AND innerPackItem.deleted = b'0'
                             AND innerPackItem.tenant_id = manualPiece.tenant_id
                      INNER JOIN mes_inv_finished_stock finishedStock
                              ON finishedStock.inner_unit_no = innerPackItem.inner_unit_no
                             AND finishedStock.slice_batch_no = innerPackItem.slice_batch_no
                             AND finishedStock.deleted = b'0'
                             AND finishedStock.tenant_id = manualPiece.tenant_id
                     WHERE manualPiece.deleted = b'0'
                       AND manualPiece.tenant_id = #{tenantId}
                       AND manualPiece.segment_batch_no IS NOT NULL
                       AND TRIM(manualPiece.segment_batch_no) <> ''
                       AND manualPiece.inspection_result IN ('OK', '合格')
                       AND manualPiece.coa_inspection_result IN ('OK', '合格')
                       AND finishedStock.quality_status IN ('OK', '合格')
                       AND (finishedStock.stock_status IS NULL
                            OR finishedStock.stock_status NOT IN ('CANCELLED', 'CANCELED', 'OUTBOUND', 'SHIPPED'))
                   ) sourceRows
             WHERE sourceRows.modelCode IS NOT NULL
               AND sourceRows.modelCode <> ''
               AND (#{reqVO.keyword} IS NULL OR #{reqVO.keyword} = ''
                    OR sourceRows.modelCode LIKE CONCAT('%', #{reqVO.keyword}, '%')
                    OR sourceRows.segmentBatchNo LIKE CONCAT('%', #{reqVO.keyword}, '%')
                    OR sourceRows.remark LIKE CONCAT('%', #{reqVO.keyword}, '%'))
               AND (#{reqVO.modelCode} IS NULL OR #{reqVO.modelCode} = ''
                    OR sourceRows.modelCode LIKE CONCAT('%', #{reqVO.modelCode}, '%'))
               AND (#{reqVO.segmentBatchNo} IS NULL OR #{reqVO.segmentBatchNo} = ''
                    OR sourceRows.segmentBatchNo LIKE CONCAT('%', #{reqVO.segmentBatchNo}, '%'))
             GROUP BY sourceRows.modelCode, sourceRows.segmentBatchNo
            """;

    @Select("""
            SELECT COUNT(1)
              FROM (
            """ + AGGREGATE_SQL + """
                   ) aggregatedRows
            """)
    long selectCount(@Param("tenantId") Long tenantId,
                     @Param("reqVO") HcHistoryPieceMassStockPageReqVO reqVO);

    @Select("""
            """ + AGGREGATE_SQL + """
             ORDER BY modelCode ASC, segmentBatchNo ASC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<HcHistoryPieceMassStockRespVO> selectPage(@Param("tenantId") Long tenantId,
                                                    @Param("reqVO") HcHistoryPieceMassStockPageReqVO reqVO,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    @Select("""
            SELECT finishedStock.id AS id,
                   finishedStock.stock_no AS stockNo,
                   finishedStock.outer_box_no AS outerBoxNo,
                   finishedStock.inner_unit_no AS innerUnitNo,
                   finishedStock.slice_batch_no AS sliceBatchNo,
                   COALESCE(NULLIF(TRIM(manualPiece.model_code), ''),
                            NULLIF(TRIM(finishedStock.model_code), '')) AS modelCode,
                   TRIM(manualPiece.segment_batch_no) AS segmentBatchNo,
                   finishedStock.qty AS qty,
                   finishedStock.quality_status AS qualityStatus,
                   finishedStock.warehouse_name AS warehouseName,
                   finishedStock.location_code AS locationCode,
                   finishedStock.location_name AS locationName,
                   finishedStock.inbound_time AS inboundTime
              FROM mes_sfc_packaging_manual_piece manualPiece
              INNER JOIN mes_sfc_inner_pack_unit_item innerPackItem
                      ON innerPackItem.source_manual_piece_id = manualPiece.id
                     AND innerPackItem.source_type = 'MANUAL_HISTORY'
                     AND innerPackItem.deleted = b'0'
                     AND innerPackItem.tenant_id = manualPiece.tenant_id
              INNER JOIN mes_inv_finished_stock finishedStock
                      ON finishedStock.inner_unit_no = innerPackItem.inner_unit_no
                     AND finishedStock.slice_batch_no = innerPackItem.slice_batch_no
                     AND finishedStock.deleted = b'0'
                     AND finishedStock.tenant_id = manualPiece.tenant_id
             WHERE manualPiece.deleted = b'0'
               AND manualPiece.tenant_id = #{tenantId}
               AND TRIM(manualPiece.segment_batch_no) = #{segmentBatchNo}
               AND manualPiece.inspection_result IN ('OK', '合格')
               AND manualPiece.coa_inspection_result IN ('OK', '合格')
               AND finishedStock.quality_status IN ('OK', '合格')
               AND (finishedStock.stock_status IS NULL
                    OR finishedStock.stock_status NOT IN ('CANCELLED', 'CANCELED', 'OUTBOUND', 'SHIPPED'))
               AND (#{modelCode} IS NULL OR #{modelCode} = ''
                    OR COALESCE(NULLIF(TRIM(manualPiece.model_code), ''),
                                NULLIF(TRIM(finishedStock.model_code), '')) = #{modelCode})
             ORDER BY finishedStock.inbound_time ASC, finishedStock.slice_batch_no ASC, finishedStock.stock_no ASC
            """)
    List<HcHistoryPieceMassStockGoodStockRespVO> selectGoodStockList(@Param("tenantId") Long tenantId,
                                                                       @Param("modelCode") String modelCode,
                                                                       @Param("segmentBatchNo") String segmentBatchNo);

}
