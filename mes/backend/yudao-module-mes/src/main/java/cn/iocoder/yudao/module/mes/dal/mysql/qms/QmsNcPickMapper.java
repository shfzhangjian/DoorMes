package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceFactDTO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 母卷/加工段挑选：使用真实生产来源，处置范围本身作为对象级改判凭据。 */
@Mapper
public interface QmsNcPickMapper {
    @Select("SELECT * FROM (" + HcBatchTraceMapper.TRACE_SELECT + """
            ) f WHERE f.sourceType IN ('WET_REPORT', 'ROUGH_GRINDING_FIRST',
                'ROUGH_GRINDING_FIRST_ALLOCATION', 'ROUGH_GRINDING_SECOND', 'ADHESIVE_REPORT')
              AND (f.productionBatchNo = #{lotNo} OR f.sourceBatchNo = #{lotNo}
                   OR f.parentProductionBatchNo = #{lotNo}
                   OR f.productionBatchNo IN (CONCAT(#{lotNo}, '-J1'), CONCAT(#{lotNo}, '-J2')))
            ORDER BY f.id
            """)
    List<HcBatchTraceFactDTO> selectProductionObjects(@Param("lotNo") String lotNo,
                                                    @Param("tenantId") Long tenantId);

    @Select("""
            SELECT s.* FROM mes_qms_nc_disposition_scope s
            JOIN mes_qms_nc_record n ON n.id = s.nc_record_id AND n.tenant_id = s.tenant_id
            JOIN mes_qms_nc_disposition_execution e ON e.id = s.execution_id AND e.tenant_id = s.tenant_id
            WHERE s.tenant_id = #{tenantId} AND s.deleted = 0 AND n.deleted = 0 AND e.deleted = 0
              AND n.source_biz_type = 'FAI' AND n.source_id = #{faiId}
              AND (n.status IN ('PENDING_STOCK_DISPOSE', 'CLOSE_CONFIRM', 'CLOSED')
                   OR n.stock_dispose_status = 'DONE')
              AND s.disposition_type = 'PICK' AND s.execution_result = 'PICK_QUALIFIED'
              AND s.scope_role = 'PICK_ALLOW' AND e.execution_status = 'COMPLETED'
            """)
    List<QmsNcDispositionScopeDO> selectQualifiedScopes(@Param("faiId") Long faiId,
                                                      @Param("tenantId") Long tenantId);
    @Select("""
            <script>
            SELECT id FROM
            <choose>
              <when test="sourceType == 'WET_REPORT'">mes_sfc_operation_report</when>
              <when test="sourceType == 'ROUGH_GRINDING_FIRST'">mes_sfc_grinding_first_detail</when>
              <when test="sourceType == 'ROUGH_GRINDING_FIRST_ALLOCATION'">mes_sfc_grinding_first_allocation_detail</when>
              <when test="sourceType == 'ADHESIVE_REPORT'">mes_sfc_adhesive_report</when>
              <when test="sourceType == 'ROUGH_GRINDING_SECOND'">mes_sfc_grinding_second_detail</when>
              <otherwise>(SELECT NULL AS id, NULL AS tenant_id, 1 AS deleted) invalid_source</otherwise>
            </choose>
            WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 FOR UPDATE
            </script>
            """)
    Long lockProductionObject(@Param("sourceType") String sourceType, @Param("id") Long id,
                              @Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM mes_qms_nc_disposition_scope s
            JOIN mes_qms_nc_record n ON n.id = s.nc_record_id AND n.tenant_id = s.tenant_id
            WHERE s.tenant_id = #{tenantId} AND s.deleted = 0 AND n.deleted = 0
              AND n.id != #{ncId} AND n.status NOT IN ('CANCELED', 'CANCELLED', 'DRAFT')
              AND (s.object_key = #{objectKey} OR (s.scope_level = 'MOTHER_BATCH' AND s.mother_batch_no = #{mother}))
              AND (s.disposition_type IN ('SCRAP', 'REWORK') OR s.scope_role = 'PICK_OUTSIDE_SCRAP')
            """)
    int countConflictingScopes(@Param("ncId") Long ncId, @Param("objectKey") String objectKey,
                               @Param("mother") String mother, @Param("tenantId") Long tenantId);
}
