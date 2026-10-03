package cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory;

import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerRespVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/** 不合格品统一历史台账：合并 NG 仓、待包装区和成品仓三个事实流水。 */
@Mapper
public interface HcUnqualifiedHistoryLedgerMapper {

    @SelectProvider(type = SqlProvider.class, method = "selectPage")
    List<UnqualifiedHistoryLedgerRespVO> selectPage(@Param("req") UnqualifiedHistoryLedgerPageReqVO reqVO,
                                                     @Param("offset") long offset,
                                                     @Param("pageSize") int pageSize);

    @SelectProvider(type = SqlProvider.class, method = "count")
    long count(@Param("req") UnqualifiedHistoryLedgerPageReqVO reqVO);

    class SqlProvider {

        public String selectPage() {
            return "<script>" + selectSql() + " ORDER BY event_time DESC, source_sort_id DESC"
                    + " LIMIT #{offset}, #{pageSize}</script>";
        }

        public String count() {
            return "<script>SELECT COUNT(1) FROM (" + unionSql() + ") ledger " + whereSql() + "</script>";
        }

        private static String selectSql() {
            return "SELECT id, inventory_area AS inventoryArea, event_type AS eventType, event_time AS eventTime, "
                    + inventoryDirectionSql() + " AS inventoryDirection, "
                    + "slice_batch_no AS sliceBatchNo, segment_batch_no AS segmentBatchNo, plan_no AS planNo, "
                    + "process_name AS processName, material_code AS materialCode, material_name AS materialName, "
                    + "model_code AS modelCode, qty, fqc_result AS fqcResult, coa_result AS coaResult, "
                    + "quality_status AS qualityStatus, warehouse_code AS warehouseCode, warehouse_name AS warehouseName, "
                    + "location_code AS locationCode, after_status AS afterStatus, operator_name AS operatorName, "
                    + "ref_doc_no AS refDocNo, txn_no AS txnNo, remark FROM (" + unionSql() + ") ledger " + whereSql();
        }

        /** 仅展示实际出入库；移库归为内部流转。显示、筛选及分页计数共用方向口径。 */
        private static String inventoryDirectionSql() {
            return "CASE WHEN event_type IN ('NG_SHELF','FREEZE_SHELF','FG_INBOUND') THEN 'IN' "
                    + "WHEN event_type IN ('NG_SCRAP','NG_MANUAL_OUTBOUND','NG_REWORK_OUT',"
                    + "'FG_MANUAL_OUTBOUND','FG_SHIP','PACKAGING_MANUAL_OUTBOUND',"
                    + "'FG_PACKAGE_DIRECT_OUTBOUND','PACKAGING_DIRECT_OUTBOUND') THEN 'OUT' "
                    + "WHEN event_type IN ('NG_TRANSFER_IN','NG_TRANSFER_OUT','NG_UNSHELF','HISTORY_FREEZE_UNSHELF','NG_REWORK_PICK',"
                    + "'HISTORY_UNFREEZE_CLOSE','PACKAGING_NG_ENTER','PACKAGING_NG_EXIT',"
                    + "'PACKAGING_RETURN_TO_WAIT','PACKAGING_QUALITY_CHANGED','PACKAGING_VOID',"
                    + "'FG_UNSHELF','FG_OUTBOUND_LOCK','FG_OUTBOUND_UNLOCK','FG_OUTBOUND_ALLOCATE',"
                    + "'FG_PACKAGE_SPLIT_RETURN','FG_SHIPPING_CANCEL_RETURN') THEN 'NONE' "
                    + "ELSE 'UNKNOWN' END";
        }

        private static String unionSql() {
            return """
                    SELECT CONCAT('NG-', t.id) AS id,
                           'NG_WAREHOUSE' AS inventory_area,
                           t.txn_type AS event_type,
                           t.txn_time AS event_time,
                           p.piece_no AS slice_batch_no,
                           COALESCE(p.source_parent_batch_no, p.source_batch_no, t.source_batch_no, t.batch_no) AS segment_batch_no,
                           COALESCE(p.source_plan_no, t.source_plan_no) AS plan_no,
                           p.process_name AS process_name,
                           t.material_code AS material_code,
                           t.material_name AS material_name,
                           t.model_no AS model_code,
                           ABS(t.txn_qty) AS qty,
                           p.quality_result AS fqc_result,
                           NULL AS coa_result,
                           'NG' AS quality_status,
                           t.warehouse_code AS warehouse_code,
                           t.warehouse_name AS warehouse_name,
                           t.location_code AS location_code,
                           CASE t.txn_type
                             WHEN 'NG_SHELF' THEN 'STORED'
                             WHEN 'FREEZE_SHELF' THEN 'FROZEN'
                             WHEN 'NG_UNSHELF' THEN 'WAIT_SHELF'
                             WHEN 'HISTORY_FREEZE_UNSHELF' THEN 'WAIT_FREEZE_SHELF'
                             WHEN 'NG_TRANSFER_OUT' THEN 'TRANSFER_OUT'
                             WHEN 'NG_TRANSFER_IN' THEN 'STORED'
                             WHEN 'NG_SCRAP' THEN 'SCRAPPED'
                             WHEN 'NG_MANUAL_OUTBOUND' THEN 'OUTBOUNDED'
                             WHEN 'NG_REWORK_PICK' THEN 'REWORKING'
                             WHEN 'NG_REWORK_OUT' THEN 'RETURNED'
                             WHEN 'HISTORY_UNFREEZE_CLOSE' THEN 'UNFROZEN_CLOSED'
                             ELSE NULL
                           END AS after_status,
                           t.creator_name AS operator_name,
                           t.ref_doc_no AS ref_doc_no,
                           t.txn_no AS txn_no,
                           t.remark AS remark,
                           t.id AS source_sort_id
                    FROM mes_inv_txn_log t
                    INNER JOIN mes_inv_ng_piece p ON p.id = t.source_id AND p.deleted = b'0'
                    WHERE t.deleted = b'0'
                      AND t.source_type = 'NG_INVENTORY'
                      AND t.source_table = 'mes_inv_ng_piece'
                    UNION ALL
                    SELECT CONCAT('PKG-', e.id) AS id,
                           'WAIT_PACKAGING' AS inventory_area,
                           e.event_type AS event_type,
                           e.event_time AS event_time,
                           e.slice_batch_no AS slice_batch_no,
                           e.segment_batch_no AS segment_batch_no,
                           e.plan_no AS plan_no,
                           '包装' AS process_name,
                           e.material_code AS material_code,
                           e.material_name AS material_name,
                           e.model_code AS model_code,
                           1 AS qty,
                           e.fqc_result AS fqc_result,
                           e.coa_result AS coa_result,
                           e.quality_status AS quality_status,
                           NULL AS warehouse_code,
                           NULL AS warehouse_name,
                           NULL AS location_code,
                           e.after_status AS after_status,
                           e.operator_name AS operator_name,
                           e.ref_doc_no AS ref_doc_no,
                           e.event_no AS txn_no,
                           e.remark AS remark,
                           e.id AS source_sort_id
                    FROM mes_sfc_packaging_piece_event_log e
                    WHERE e.deleted = b'0'
                      AND e.ng_related = b'1'
                    UNION ALL
                    SELECT CONCAT('FG-', f.id) AS id,
                           'FG_WAREHOUSE' AS inventory_area,
                           f.txn_type AS event_type,
                           f.txn_time AS event_time,
                           f.slice_batch_no AS slice_batch_no,
                           f.batch_no AS segment_batch_no,
                           NULL AS plan_no,
                           '成品包装' AS process_name,
                           f.material_code AS material_code,
                           f.material_name AS material_name,
                           f.model_code AS model_code,
                           f.qty AS qty,
                           NULL AS fqc_result,
                           NULL AS coa_result,
                           f.quality_status AS quality_status,
                           f.warehouse_code AS warehouse_code,
                           f.warehouse_name AS warehouse_name,
                           f.location_code AS location_code,
                           f.after_stock_status AS after_status,
                           f.operator_name AS operator_name,
                           f.ref_doc_no AS ref_doc_no,
                           f.txn_no AS txn_no,
                           f.remark AS remark,
                           f.id AS source_sort_id
                    FROM mes_inv_finished_stock_txn_log f
                    WHERE f.deleted = b'0'
                      AND f.quality_status = 'NG'
                    """;
        }

        private static String whereSql() {
            return """
                    WHERE
                    """ + inventoryDirectionSql() + " IN ('IN', 'OUT') " + "<if test=\"req.inventoryDirection != null and req.inventoryDirection != ''\">"
                    + " AND " + inventoryDirectionSql() + " = #{req.inventoryDirection} </if>" + """
                    <if test="req.inventoryArea != null and req.inventoryArea != ''">
                      AND inventory_area = #{req.inventoryArea}
                    </if>
                    <if test="req.eventType != null and req.eventType != ''">
                      AND event_type = #{req.eventType}
                    </if>
                    <if test="req.afterStatus != null and req.afterStatus != ''">
                      AND after_status = #{req.afterStatus}
                    </if>
                    <if test="req.qualityStatus != null and req.qualityStatus != ''">
                      AND quality_status = #{req.qualityStatus}
                    </if>
                    <if test="req.materialCode != null and req.materialCode != ''">
                      AND material_code LIKE CONCAT('%', #{req.materialCode}, '%')
                    </if>
                    <if test="req.modelCode != null and req.modelCode != ''">
                      AND model_code LIKE CONCAT('%', #{req.modelCode}, '%')
                    </if>
                    <if test="req.sliceBatchNo != null and req.sliceBatchNo != ''">
                      AND slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%')
                    </if>
                    <if test="req.segmentBatchNo != null and req.segmentBatchNo != ''">
                      AND segment_batch_no LIKE CONCAT('%', #{req.segmentBatchNo}, '%')
                    </if>
                    <if test="req.planNo != null and req.planNo != ''">
                      AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
                    </if>
                    <if test="req.eventTimeStart != null">
                      AND event_time &gt;= #{req.eventTimeStart}
                    </if>
                    <if test="req.eventTimeEnd != null">
                      AND event_time &lt;= #{req.eventTimeEnd}
                    </if>
                    <if test="req.keyword != null and req.keyword != ''">
                      AND (slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                        OR segment_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                        OR plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                        OR material_code LIKE CONCAT('%', #{req.keyword}, '%')
                        OR model_code LIKE CONCAT('%', #{req.keyword}, '%')
                        OR ref_doc_no LIKE CONCAT('%', #{req.keyword}, '%')
                        OR txn_no LIKE CONCAT('%', #{req.keyword}, '%'))
                    </if>
                    """;
        }
    }
}
