package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcPackagingPieceRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface QmsNcPackagingFlowMapper {
    // UNION 各来源表可能采用不同字符集/排序规则；文本在合并前统一，禁止依赖连接默认值。
    // 已确认范围优先；未确认时仅显示有检验/异常锁证据的对象，不按片号前缀扩散。
    String RELATIONS = """

            SELECT s.nc_record_id, s.tenant_id,
                   CONVERT(s.scope_level USING utf8mb4) COLLATE utf8mb4_unicode_ci AS scope_level,
                   CASE WHEN s.scope_level = 'PIECE' THEN COALESCE(NULLIF(CONVERT(s.piece_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, ''),
                     NULLIF(CONVERT(s.source_object_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, ''), NULLIF(REPLACE(CONVERT(s.object_key USING utf8mb4) COLLATE utf8mb4_unicode_ci, 'PIECE:', ''), ''))
                     ELSE CONVERT(s.piece_no USING utf8mb4) COLLATE utf8mb4_unicode_ci END AS piece_no,
                   CONVERT(s.segment_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS segment_batch_no,
                   CONVERT(s.mother_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS mother_batch_no,
                   CONVERT(s.disposition_type USING utf8mb4) COLLATE utf8mb4_unicode_ci AS disposition_type,
                   CONVERT(s.scope_role USING utf8mb4) COLLATE utf8mb4_unicode_ci AS scope_role,
                   CONVERT(s.execution_result USING utf8mb4) COLLATE utf8mb4_unicode_ci AS execution_result,
                   CONVERT(s.remark USING utf8mb4) COLLATE utf8mb4_unicode_ci AS remark, 1 AS scope_confirmed
            FROM mes_qms_nc_disposition_scope s
            WHERE s.deleted = 0
            UNION ALL
            SELECT n.id, n.tenant_id, CONVERT('PIECE' USING utf8mb4) COLLATE utf8mb4_unicode_ci, CONVERT(d.production_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                   NULL, NULL, NULL, NULL, NULL, CONVERT('来源裁切检验异常，待确认处置范围' USING utf8mb4) COLLATE utf8mb4_unicode_ci, 0
            FROM mes_qms_nc_record n
            JOIN mes_qms_fqc_submission_detail d ON d.fqc_id = n.source_id
              AND d.tenant_id = n.tenant_id AND d.deleted = 0
            WHERE n.deleted = 0 AND n.source_biz_type = 'CUT_ROUND_FQC'
              AND d.row_judgment = 'NG'
              AND NOT EXISTS (SELECT 1 FROM mes_qms_nc_disposition_execution e
                  WHERE e.nc_record_id = n.id AND e.tenant_id = n.tenant_id AND e.deleted = 0)
            UNION ALL
            SELECT n.id, n.tenant_id, CONVERT('PIECE' USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                   COALESCE(NULLIF(CONVERT(i.production_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, ''), CONVERT(d.production_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci),
                   NULL, NULL, NULL, NULL, NULL, CONVERT('来源裁切检验异常，待确认处置范围' USING utf8mb4) COLLATE utf8mb4_unicode_ci, 0
            FROM mes_qms_nc_record n
            JOIN mes_qms_fqc_item i ON i.fqc_id = n.source_id
              AND i.tenant_id = n.tenant_id AND i.deleted = 0
            LEFT JOIN mes_qms_fqc_submission_detail d ON d.id = i.submission_detail_id
              AND d.tenant_id = n.tenant_id AND d.deleted = 0
            WHERE n.deleted = 0 AND n.source_biz_type = 'CUT_ROUND_FQC'
              AND (i.item_result IN ('NG', 'ABNORMAL') OR i.qa_result IN ('NG', 'ABNORMAL')
                OR i.operator_result IN ('NG', 'ABNORMAL') OR i.input_status IN ('NG', 'ABNORMAL')
                OR i.abnormal_sample_count > 0)
              AND NOT EXISTS (SELECT 1 FROM mes_qms_nc_disposition_execution e
                  WHERE e.nc_record_id = n.id AND e.tenant_id = n.tenant_id AND e.deleted = 0)
            UNION ALL
            SELECT n.id, n.tenant_id, CONVERT('PIECE' USING utf8mb4) COLLATE utf8mb4_unicode_ci, CONVERT(i.production_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                   NULL, CONVERT(i.mother_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, NULL, NULL, NULL, CONVERT('来源异常锁定，待确认处置范围' USING utf8mb4) COLLATE utf8mb4_unicode_ci, 0
            FROM mes_qms_nc_record n
            JOIN mes_sfc_press_slot_abnormal_lock_item i ON i.abnormal_fai_id = n.source_id
              AND i.tenant_id = n.tenant_id AND i.deleted = 0
            WHERE n.deleted = 0 AND n.source_biz_type IN ('FAI', 'GLUE_BOARD_FAI')
              AND NOT EXISTS (SELECT 1 FROM mes_qms_nc_disposition_execution e
                  WHERE e.nc_record_id = n.id AND e.tenant_id = n.tenant_id AND e.deleted = 0)
            """;

    @Select("SELECT DISTINCT r.scope_level, r.piece_no, r.segment_batch_no, r.mother_batch_no, "
            + "r.disposition_type, r.scope_role, r.execution_result, r.remark, r.scope_confirmed, "
            + "EXISTS (SELECT 1 FROM mes_sfc_inner_pack_unit_item pi "
            + "JOIN mes_sfc_inner_pack_unit p ON p.id = pi.inner_unit_id AND p.tenant_id = pi.tenant_id "
            + "WHERE CONVERT(pi.slice_batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci = r.piece_no AND pi.tenant_id = r.tenant_id "
            + "AND pi.deleted = 0 AND p.deleted = 0 "
            + "AND p.unit_status IN ('PACKED', 'INBOUND_LOCKED', 'INBOUNDED')) AS packaged "
            + "FROM (" + RELATIONS + ") r WHERE r.nc_record_id = #{id} AND r.tenant_id = #{tenantId} "
            + "ORDER BY r.scope_level, r.piece_no, r.segment_batch_no, r.mother_batch_no")
    List<QmsNcPackagingPieceRespVO> selectPieces(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
