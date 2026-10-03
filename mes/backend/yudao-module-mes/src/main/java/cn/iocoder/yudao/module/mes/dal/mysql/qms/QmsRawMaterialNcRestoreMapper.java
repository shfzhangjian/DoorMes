package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QmsRawMaterialNcRestoreMapper {

    @Select("""
            SELECT COUNT(*)
            FROM mes_qms_nc_record
            WHERE id <> #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND deleted = b'0'
              AND source_type = 'RAW_MATERIAL'
              AND source_biz_type = 'IQC'
              AND source_id = #{sourceIqcId}
              AND COALESCE(UPPER(status), '') <> 'CANCELLED'
            """)
    long selectOtherActiveNcrCount(@Param("ncRecordId") Long ncRecordId,
                                   @Param("sourceIqcId") Long sourceIqcId,
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
            UPDATE mes_qms_iqc_abnormal
            SET nc_record_id = NULL,
                process_status = 'PENDING',
                updater = #{operatorId},
                update_time = NOW()
            WHERE tenant_id = #{tenantId}
              AND (nc_record_id = #{ncRecordId}
                OR (iqc_id = #{sourceIqcId} AND nc_record_id IS NULL AND process_status = 'NCR_CREATED'))
            """)
    int restoreIqcAbnormalLink(@Param("ncRecordId") Long ncRecordId,
                               @Param("sourceIqcId") Long sourceIqcId,
                               @Param("tenantId") Long tenantId,
                               @Param("operatorId") String operatorId);

    @Delete("""
            DELETE FROM mes_qms_nc_record
            WHERE id = #{ncRecordId}
              AND tenant_id = #{tenantId}
              AND source_type = 'RAW_MATERIAL'
              AND source_biz_type = 'IQC'
            """)
    int deleteNcRecord(@Param("ncRecordId") Long ncRecordId,
                       @Param("tenantId") Long tenantId);
}
