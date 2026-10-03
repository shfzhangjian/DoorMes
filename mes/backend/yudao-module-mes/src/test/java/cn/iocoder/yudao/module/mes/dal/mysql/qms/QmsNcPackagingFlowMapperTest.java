package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 在隔离内存库执行真实关联 SQL，覆盖片号证据与租户边界。 */
class QmsNcPackagingFlowMapperTest {
    private Connection db;

    @BeforeEach void setup() throws Exception {
        db = DriverManager.getConnection("jdbc:h2:mem:packaging_flow;MODE=MySQL;DB_CLOSE_DELAY=0");
        sql("CREATE TABLE mes_qms_nc_record(id BIGINT, tenant_id BIGINT, source_id BIGINT, source_biz_type VARCHAR, deleted INT)");
        sql("CREATE TABLE mes_qms_nc_disposition_execution(nc_record_id BIGINT, tenant_id BIGINT, deleted INT)");
        sql("CREATE TABLE mes_qms_nc_disposition_scope(nc_record_id BIGINT, tenant_id BIGINT, scope_level VARCHAR, piece_no VARCHAR, source_object_no VARCHAR, object_key VARCHAR, segment_batch_no VARCHAR, mother_batch_no VARCHAR, disposition_type VARCHAR, scope_role VARCHAR, execution_result VARCHAR, remark VARCHAR, deleted INT)");
        sql("CREATE TABLE mes_qms_fqc_submission_detail(id BIGINT, fqc_id BIGINT, tenant_id BIGINT, production_batch_no VARCHAR, row_judgment VARCHAR, deleted INT)");
        sql("CREATE TABLE mes_qms_fqc_item(fqc_id BIGINT, tenant_id BIGINT, submission_detail_id BIGINT, production_batch_no VARCHAR, item_result VARCHAR, qa_result VARCHAR, operator_result VARCHAR, input_status VARCHAR, abnormal_sample_count INT, deleted INT)");
        sql("CREATE TABLE mes_sfc_press_slot_abnormal_lock_item(abnormal_fai_id BIGINT, tenant_id BIGINT, production_batch_no VARCHAR, mother_batch_no VARCHAR, deleted INT)");
        sql("CREATE TABLE mes_sfc_inner_pack_unit_item(inner_unit_id BIGINT, tenant_id BIGINT, slice_batch_no VARCHAR, deleted INT)");
        sql("CREATE TABLE mes_sfc_inner_pack_unit(id BIGINT, tenant_id BIGINT, unit_status VARCHAR, deleted INT)");
        sql("INSERT INTO mes_qms_nc_record VALUES(1,1,100,'CUT_ROUND_FQC',0)");
    }
    @AfterEach void close() throws Exception { db.close(); }
    void sql(String sql) throws Exception { try (var s = db.createStatement()) { s.execute(sql); } }
    List<String> pieces(long id, long tenant) throws Exception {
        String query = String.join(" ", QmsNcPackagingFlowMapper.class
                .getMethod("selectPieces", Long.class, Long.class).getAnnotation(Select.class).value())
                .replace("#{id}", "?").replace("#{tenantId}", "?")
                // H2不支持MySQL字符集表达式；此处仅验证关联逻辑，排序规则另用MySQL回归。
                .replaceAll("CONVERT\\(([^()]*) USING utf8mb4\\) COLLATE utf8mb4_unicode_ci", "$1");
        try (var s = db.prepareStatement(query)) {
            s.setLong(1, id); s.setLong(2, tenant);
            try (ResultSet rs = s.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) result.add(rs.getString("piece_no") + ":" + rs.getBoolean("scope_confirmed") + ":" + rs.getBoolean("packaged"));
                return result;
            }
        }
    }
    @Test void unconfirmedUsesOnlyAbnormalEvidenceAndDeduplicates() throws Exception {
        sql("INSERT INTO mes_qms_fqc_submission_detail VALUES(10,100,1,'A', 'NG',0),(11,100,1,'B','OK',0),(12,100,2,'OTHER','NG',0),(13,100,1,'DELETED','NG',1)");
        sql("INSERT INTO mes_qms_fqc_item VALUES(100,1,10,'A','NG',NULL,NULL,NULL,0,0),(100,1,11,NULL,NULL,'NG',NULL,NULL,0,0)");
        assertEquals(List.of("A:false:false", "B:false:false"), pieces(1,1));
        assertTrue(pieces(1,2).isEmpty());
    }
    @Test void confirmedSnapshotOverridesChangingSourceAndKeepsOutsideScrap() throws Exception {
        sql("INSERT INTO mes_qms_nc_disposition_execution VALUES(1,1,0)");
        sql("INSERT INTO mes_qms_fqc_submission_detail VALUES(10,100,1,'SOURCE_ONLY','NG',0)");
        sql("INSERT INTO mes_qms_nc_disposition_scope(nc_record_id,tenant_id,scope_level,piece_no,disposition_type,scope_role,deleted) VALUES(1,1,'PIECE','A','PICK','SELECTED',0),(1,1,'PIECE','B','SCRAP','PICK_OUTSIDE_SCRAP',0)");
        assertEquals(List.of("A:true:false", "B:true:false"), pieces(1,1));
    }
    @Test void packagingRequiresLivePackageInSameTenantAndValidState() throws Exception {
        sql("INSERT INTO mes_qms_nc_disposition_scope(nc_record_id,tenant_id,scope_level,piece_no,deleted) VALUES(1,1,'PIECE','A',0),(1,1,'PIECE','B',0),(1,1,'PIECE','C',0)");
        sql("INSERT INTO mes_sfc_inner_pack_unit_item VALUES(10,1,'A',0),(11,1,'B',0),(12,2,'C',0)");
        sql("INSERT INTO mes_sfc_inner_pack_unit VALUES(10,1,'PACKED',0),(11,1,'PACKED',1),(12,2,'INBOUNDED',0)");
        assertEquals(List.of("A:true:true", "B:true:false", "C:true:false"), pieces(1,1));
    }
    @Test void sourceIdCollisionDoesNotLinkFqcToFaiLock() throws Exception {
        sql("INSERT INTO mes_sfc_press_slot_abnormal_lock_item VALUES(100,1,'LOCKED','MOTHER',0)");
        assertTrue(pieces(1,1).isEmpty());
        sql("INSERT INTO mes_qms_nc_record VALUES(2,1,100,'FAI',0)");
        assertEquals(List.of("LOCKED:false:false"), pieces(2,1));
    }
    @Test void legacyPieceKeyIsResolvedWithoutPrefixSearch() throws Exception {
        sql("INSERT INTO mes_qms_nc_disposition_scope(nc_record_id,tenant_id,scope_level,object_key,deleted) VALUES(1,1,'PIECE','PIECE:A001',0)");
        assertEquals(List.of("A001:true:false"), pieces(1,1));
    }
}
