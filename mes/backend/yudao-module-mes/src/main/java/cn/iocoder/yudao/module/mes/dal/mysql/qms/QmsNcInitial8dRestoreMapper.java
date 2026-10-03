package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dReportDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * NCR 管理员还原时，对“仅完成初始化、尚未办理”的直连 8D 做事务内物理清理。
 */
@Mapper
public interface QmsNcInitial8dRestoreMapper {

    @Select("""
            SELECT DISTINCT report.*
            FROM mes_qms_8d_report report
            LEFT JOIN mes_qms_8d_relation relation
              ON relation.report_id = report.id
             AND relation.tenant_id = #{tenantId}
             AND relation.deleted = b'0'
            WHERE report.tenant_id = #{tenantId}
              AND report.deleted = b'0'
              AND (
                    (UPPER(report.source_type) = 'NCR'
                     AND (report.source_id = #{ncRecordId}
                          OR HEX(report.source_no) = HEX(#{ncNo})))
                 OR (UPPER(relation.relation_type) = 'NCR'
                     AND (relation.related_object_id = #{ncRecordId}
                          OR HEX(relation.related_object_no) = HEX(#{ncNo})))
                 OR (#{related8dNo} IS NOT NULL
                     AND LENGTH(#{related8dNo}) > 0
                     AND HEX(report.report_no) = HEX(#{related8dNo}))
              )
            ORDER BY report.id
            FOR UPDATE
            """)
    List<Qms8dReportDO> selectLinkedReportsForUpdate(@Param("ncRecordId") Long ncRecordId,
                                                      @Param("ncNo") String ncNo,
                                                      @Param("related8dNo") String related8dNo,
                                                      @Param("tenantId") Long tenantId);

    @Select("""
            SELECT DISTINCT report.*
            FROM mes_qms_8d_report report
            LEFT JOIN mes_qms_8d_relation relation
              ON relation.report_id = report.id
             AND relation.tenant_id = #{tenantId}
             AND relation.deleted = b'0'
            WHERE report.tenant_id = #{tenantId}
              AND report.deleted = b'0'
              AND (
                    (UPPER(report.source_type) = 'NCR'
                     AND (report.source_id = #{ncRecordId}
                          OR HEX(report.source_no) = HEX(#{ncNo})))
                 OR (UPPER(relation.relation_type) = 'NCR'
                     AND (relation.related_object_id = #{ncRecordId}
                          OR HEX(relation.related_object_no) = HEX(#{ncNo})))
                 OR (#{related8dNo} IS NOT NULL
                     AND LENGTH(#{related8dNo}) > 0
                     AND HEX(report.report_no) = HEX(#{related8dNo}))
              )
            ORDER BY report.id
            """)
    List<Qms8dReportDO> selectLinkedReports(@Param("ncRecordId") Long ncRecordId,
                                            @Param("ncNo") String ncNo,
                                            @Param("related8dNo") String related8dNo,
                                            @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_8d_action_item WHERE report_id = #{reportId} AND tenant_id = #{tenantId}")
    int deleteActionItems(@Param("reportId") Long reportId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_8d_flow_log WHERE report_id = #{reportId} AND tenant_id = #{tenantId}")
    int deleteFlowLogs(@Param("reportId") Long reportId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_8d_relation WHERE report_id = #{reportId} AND tenant_id = #{tenantId}")
    int deleteRelations(@Param("reportId") Long reportId, @Param("tenantId") Long tenantId);

    @Update("""
            UPDATE mes_qms_8d_report
            SET source_type = NULL,
                source_id = NULL,
                source_no = NULL,
                updater = #{operatorId},
                update_time = NOW()
            WHERE id = #{reportId}
              AND tenant_id = #{tenantId}
              AND UPPER(source_type) = 'NCR'
              AND (source_id = #{ncRecordId} OR HEX(source_no) = HEX(#{ncNo}))
            """)
    int clearReportSourceLink(@Param("reportId") Long reportId,
                              @Param("ncRecordId") Long ncRecordId,
                              @Param("ncNo") String ncNo,
                              @Param("tenantId") Long tenantId,
                              @Param("operatorId") String operatorId);

    @Delete("""
            DELETE FROM mes_qms_8d_relation
            WHERE report_id = #{reportId}
              AND tenant_id = #{tenantId}
              AND UPPER(relation_type) = 'NCR'
              AND (related_object_id = #{ncRecordId} OR HEX(related_object_no) = HEX(#{ncNo}))
            """)
    int deleteNcrRelations(@Param("reportId") Long reportId,
                           @Param("ncRecordId") Long ncRecordId,
                           @Param("ncNo") String ncNo,
                           @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_qms_8d_team_member WHERE report_id = #{reportId} AND tenant_id = #{tenantId}")
    int deleteTeamMembers(@Param("reportId") Long reportId, @Param("tenantId") Long tenantId);

    @Delete("""
            DELETE FROM mes_qms_8d_report
            WHERE id = #{reportId}
              AND tenant_id = #{tenantId}
              AND UPPER(source_type) = 'NCR'
            """)
    int deleteReport(@Param("reportId") Long reportId, @Param("tenantId") Long tenantId);
}
