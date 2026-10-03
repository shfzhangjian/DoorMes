package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 产品不合格处置单管理员还原专用 Mapper。
 *
 * <p>所有删除语句都同时限定 NCR 主键、租户和产品单据类型，避免跨到原材料 NCR。</p>
 */
@Mapper
public interface QmsProductNcRestoreMapper {

    @Select("""
            SELECT COUNT(*)
            FROM mes_qms_nc_record
            WHERE id <> #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
              AND (source_type IS NULL OR source_type <> 'RAW_MATERIAL')
              AND HEX(COALESCE(source_biz_type, '')) = HEX(COALESCE(#{sourceBizType}, ''))
              AND source_id = #{sourceObjectId}
              AND COALESCE(UPPER(status), '') <> 'CANCELLED'
            """)
    long selectOtherActiveNcrCount(@Param("ncRecordId") Long ncRecordId,
                                   @Param("sourceBizType") String sourceBizType,
                                   @Param("sourceObjectId") Long sourceObjectId,
                                   @Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM mes_qms_nc_record
            WHERE source_nc_record_id = #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
            """)
    long selectChildNcrCount(@Param("ncRecordId") Long ncRecordId,
                             @Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM mes_qms_exception_event
            WHERE HEX(related_ncr_no) = HEX(#{ncNo})
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
            """)
    long selectExceptionLinkCount(@Param("ncNo") String ncNo,
                                  @Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM mes_qms_nc_workstation_command
            WHERE nc_record_id = #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
              AND (UPPER(command_status) IN ('ACKED', 'APPLIED', 'SUCCESS') OR apply_time IS NOT NULL)
            """)
    long selectAppliedCommandCount(@Param("ncRecordId") Long ncRecordId,
                                   @Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM mes_qms_nc_report_gate
            WHERE nc_record_id = #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
              AND effective_time IS NOT NULL
            """)
    long selectEffectiveGateCount(@Param("ncRecordId") Long ncRecordId,
                                  @Param("tenantId") Long tenantId);

    @Update("""
            UPDATE mes_qms_nc_record
            SET source_nc_record_id = NULL, updater = #{operatorId}, update_time = NOW()
            WHERE source_nc_record_id = #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
            """)
    int clearChildNcrSourceLink(@Param("ncRecordId") Long ncRecordId,
                                @Param("tenantId") Long tenantId,
                                @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_exception_event
            SET related_ncr_no = NULL, updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND (HEX(related_ncr_no) = HEX(#{ncNo})
                OR (#{relatedExceptionId} IS NOT NULL AND id = #{relatedExceptionId}))
            """)
    int clearExceptionEventLink(@Param("ncNo") String ncNo,
                                @Param("relatedExceptionId") Long relatedExceptionId,
                                @Param("tenantId") Long tenantId,
                                @Param("operatorId") String operatorId);

    @Delete("DELETE FROM mes_qms_nc_workstation_command WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteCommands(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_report_gate WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteReportGates(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_disposition_scope WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteDispositionScopes(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_disposition_execution WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteDispositionExecutions(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_defect WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteDefects(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_mrb_review WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteReviews(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_relation WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteRelations(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_nc_flow_log WHERE nc_record_id = #{ncRecordId} AND tenant_id = #{tenantId}")
    int deleteFlowLogs(@Param("ncRecordId") Long ncRecordId, @Param("tenantId") Long tenantId);

    @Update("""
            UPDATE mes_qms_fai_abnormal
            SET nc_record_id = NULL, updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId} AND nc_record_id = #{ncRecordId}
            """)
    int clearFaiAbnormalLink(@Param("ncRecordId") Long ncRecordId,
                             @Param("tenantId") Long tenantId,
                             @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_ipqc_abnormal
            SET nc_record_id = NULL, updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId} AND nc_record_id = #{ncRecordId}
            """)
    int clearIpqcAbnormalLink(@Param("ncRecordId") Long ncRecordId,
                              @Param("tenantId") Long tenantId,
                              @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_ipqc_order
            SET nc_record_id = NULL, updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId} AND nc_record_id = #{ncRecordId}
            """)
    int clearIpqcOrderLink(@Param("ncRecordId") Long ncRecordId,
                           @Param("tenantId") Long tenantId,
                           @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_fqc_abnormal
            SET nc_record_id = NULL,
                related_ncr_no = CASE WHEN HEX(related_ncr_no) = HEX(#{ncNo}) THEN NULL ELSE related_ncr_no END,
                updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND (nc_record_id = #{ncRecordId} OR HEX(related_ncr_no) = HEX(#{ncNo}))
            """)
    int clearFqcAbnormalLink(@Param("ncRecordId") Long ncRecordId,
                             @Param("ncNo") String ncNo,
                             @Param("tenantId") Long tenantId,
                             @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_fqc_order
            SET related_ncr_no = NULL, ncr_status = NULL,
                updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId} AND HEX(related_ncr_no) = HEX(#{ncNo})
            """)
    int clearFqcOrderLink(@Param("ncNo") String ncNo,
                          @Param("tenantId") Long tenantId,
                          @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_oqc_abnormal
            SET nc_record_id = NULL,
                related_ncr_no = CASE WHEN HEX(related_ncr_no) = HEX(#{ncNo}) THEN NULL ELSE related_ncr_no END,
                updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND (nc_record_id = #{ncRecordId} OR HEX(related_ncr_no) = HEX(#{ncNo}))
            """)
    int clearOqcAbnormalLink(@Param("ncRecordId") Long ncRecordId,
                             @Param("ncNo") String ncNo,
                             @Param("tenantId") Long tenantId,
                             @Param("operatorId") String operatorId);

    @Update("""
            UPDATE mes_qms_oqc_order
            SET related_ncr_no = NULL, ncr_status = NULL,
                updater = #{operatorId}, update_time = NOW()
            WHERE tenant_id = #{tenantId} AND HEX(related_ncr_no) = HEX(#{ncNo})
            """)
    int clearOqcOrderLink(@Param("ncNo") String ncNo,
                          @Param("tenantId") Long tenantId,
                          @Param("operatorId") String operatorId);

    @Delete("""
            DELETE FROM mes_qms_nc_record
            WHERE id = #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND (source_type IS NULL OR source_type <> 'RAW_MATERIAL')
            """)
    int deleteNcRecord(@Param("ncRecordId") Long ncRecordId,
                       @Param("tenantId") Long tenantId);
}
