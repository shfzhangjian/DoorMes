package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.asset.AssetService;
import cn.iocoder.yudao.module.doormes.catalog.CatalogService;
import cn.iocoder.yudao.module.doormes.requirement.RequirementService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static cn.iocoder.yudao.module.doormes.drawing.DrawingModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Independent design tests use isolated H2/temp files and the real shared geometry validator. */
class DrawingStandaloneTest {
    @TempDir Path directory;
    final ObjectMapper mapper = new ObjectMapper();
    final AtomicLong actor = new AtomicLong(1003);
    JdbcTemplate jdbc;
    TransactionTemplate transaction;
    DrawingService drawings;
    DrawingFiles files;
    DrawingValidator validator;
    RequirementService requirements;
    PermissionService permissions;
    CatalogService catalog;
    AssetService assets;
    MockedStatic<SecurityFrameworkUtils> security;

    @BeforeEach void setup() {
        JdbcDataSource data = new JdbcDataSource();
        data.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(data);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE dm_design_requirement(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,assigned_to BIGINT,status VARCHAR(20))");
        // VARCHAR_IGNORECASE reproduces the live schema's case-insensitive business-number uniqueness.
        jdbc.execute("CREATE TABLE dm_drawing(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,requirement_id CHAR(36),requirement_revision INT,line_id CHAR(36),revision INT,status VARCHAR(20),created_by BIGINT,updated_at VARCHAR(40),drawing_number VARCHAR_IGNORECASE(60),name VARCHAR(200),note VARCHAR(1000),source_type VARCHAR(20),created_at VARCHAR(40),create_request_id CHAR(36),create_payload_hash CHAR(64),UNIQUE(tenant_id,requirement_id,line_id),UNIQUE(tenant_id,drawing_number),UNIQUE(tenant_id,created_by,create_request_id))");
        jdbc.execute("CREATE TABLE dm_drawing_version(tenant_id BIGINT,drawing_id CHAR(36),revision INT,json_path VARCHAR(160),sha256 CHAR(64),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,drawing_id,revision))");
        requirements = mock(RequirementService.class);
        permissions = mock(PermissionService.class);
        catalog = mock(CatalogService.class);
        assets = mock(AssetService.class);
        when(permissions.hasAnyPermissions(eq(1003L), any(String[].class))).thenReturn(true);
        when(permissions.hasAnyPermissions(eq(1009L), any(String[].class))).thenReturn(true);
        when(permissions.hasAnyPermissions(eq(1001L), any(String[].class))).thenReturn(true);
        when(permissions.hasAnyRoles(eq(1001L), any(String[].class))).thenReturn(true);
        files = new DrawingFiles(directory.resolve("drawings").toString(), mapper);
        validator = new DrawingValidator("C:/Program Files/nodejs/node.exe", DrawingServiceTest.validatorPath().toString(), directory.resolve("validation").toString(), mapper);
        drawings = service(validator);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(invocation -> actor.get());
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach void clear() {
        if (security != null) security.close();
        TenantContextHolder.clear();
    }

    DrawingService service(DrawingValidator gate) {
        return new DrawingService(jdbc, requirements, gate, files, permissions, catalog, assets);
    }
    <T> T tx(Supplier<T> action) { return transaction.execute(status -> action.get()); }
    Create input(String number) { return new Create(number, "自定义研发窗", "测试备注", "C1", 2, 1200, 1500, null); }
    Snapshot create(String number) { return tx(() -> drawings.create(input(number))); }
    void code(int expected, Runnable action) { assertEquals(expected, assertThrows(ServiceException.class, action::run).getCode()); }
    int revisions(String id) { return jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing_version WHERE drawing_id=?", Integer.class, id); }
    ObjectNode width(Snapshot original, int value) {
        ObjectNode doc = original.document().deepCopy();
        ((ObjectNode)doc.path("windows").get(0)).put("widthMm", value);
        return doc;
    }

    @Test void standaloneCreationNeedsNoSalesDemandAndPersistsMetadata() {
        Snapshot first = create("TEST-INDEPENDENT-001");
        assertEquals(SCHEMA, first.schemaVersion());
        assertNull(first.requirementId()); assertNull(first.lineId()); assertEquals(0, first.requirementRevision());
        assertEquals(1, first.revision()); assertEquals("DRAFT", first.status()); assertEquals(1003, first.createdBy());
        assertEquals(new Metadata("TEST-INDEPENDENT-001", "自定义研发窗", "测试备注", "INDEPENDENT"), first.metadata());
        assertTrue(first.editable()); assertEquals(first, drawings.get(first.id(), null));
        assertEquals(first.id(), first.document().path("designId").asText());
        assertEquals(1200, first.document().path("windows").get(0).path("widthMm").asInt());
        assertEquals(1500, first.document().path("windows").get(0).path("heightMm").asInt());
        assertEquals(2, first.document().path("windows").get(0).path("quantity").asInt());
        verifyNoInteractions(requirements);
    }

    @Test void blankNumberIsAutomaticallyGeneratedAndRemainsUnique() {
        Snapshot first = create(""); Snapshot second = create("");
        assertFalse(first.metadata().number().isBlank()); assertTrue(first.metadata().number().length() <= 60);
        assertNotEquals(first.metadata().number(), second.metadata().number()); assertNotEquals(first.id(), second.id());
        assertEquals(2, drawings.page("", "INDEPENDENT", true, 1, 20).total());
    }

    @Test void metadataGeometryAndImmutableHistoryAdvanceTogether() {
        Snapshot first = create("TEST-VERSION-001");
        Snapshot second = tx(() -> drawings.save(first.id(), new Save(1, "调整尺寸与图号", width(first, 1250), new Metadata("TEST-VERSION-002", "修订图纸", "新的备注", null))));
        assertEquals(2, second.revision()); assertEquals("TEST-VERSION-002", second.metadata().number());
        assertEquals("修订图纸", second.metadata().name()); assertEquals("新的备注", second.metadata().note());
        assertEquals("INDEPENDENT", second.metadata().source());
        assertEquals(1250, second.document().path("windows").get(0).path("widthMm").asInt());
        Snapshot history = drawings.get(first.id(), 1);
        assertEquals(first.metadata(), history.metadata()); assertEquals(first.document(), history.document()); assertFalse(history.editable());
        assertTrue(drawings.get(first.id(), 2).editable()); assertEquals(2, revisions(first.id()));
        code(409, () -> tx(() -> drawings.save(first.id(), new Save(1, "过期版本不能覆盖", first.document()))));
        assertEquals(second, drawings.get(first.id(), null));
    }

    @Test void caseInsensitiveNumbersAndRenameConflictsLeaveOriginalVersionsUntouched() {
        Snapshot first = create("TEST-NUMBER-A");
        code(409, () -> create("test-number-a"));
        Snapshot second = create("TEST-NUMBER-B");
        code(409, () -> tx(() -> drawings.save(second.id(), new Save(1, "冲突图号", second.document(), new Metadata("test-number-a", "另一个图", "", null)))));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing", Integer.class));
        assertEquals(first, drawings.get(first.id(), null)); assertEquals(second, drawings.get(second.id(), null));
        assertEquals(1, revisions(second.id())); assertFalse(Files.exists(directory.resolve("drawings/tenant-1/" + second.id() + "/r2.json")));
    }

    @Test void creatorAndAdminCanSaveButOtherDesignersAndReadOnlyRolesCannot() {
        Snapshot first = create("TEST-OWNER");
        actor.set(1009);
        assertFalse(drawings.get(first.id(), null).editable());
        code(403, () -> tx(() -> drawings.save(first.id(), new Save(1, "其他设计员越权", first.document()))));
        actor.set(1007);
        assertFalse(drawings.get(first.id(), null).editable());
        code(403, () -> tx(() -> drawings.save(first.id(), new Save(1, "审核只读", first.document()))));
        actor.set(1001);
        assertTrue(drawings.get(first.id(), null).editable());
        Snapshot second = tx(() -> drawings.save(first.id(), new Save(1, "管理员代修", first.document())));
        assertEquals(1003, second.createdBy()); assertEquals(1001, second.changedBy());
        actor.set(1003); assertTrue(drawings.get(first.id(), null).editable());
        when(permissions.hasAnyPermissions(eq(1003L), any(String[].class))).thenReturn(false);
        assertFalse(drawings.get(first.id(), null).editable());
        code(403, () -> tx(() -> drawings.save(first.id(), new Save(2, "权限已撤销", first.document()))));
    }

    @Test void pagePaginatesSortsAndFiltersLiteralKeywordsSourceAndCreator() {
        Snapshot first = create("TEST-PAGE-A"); Snapshot second = create("TEST-PAGE-B");
        actor.set(1009); Snapshot third = create("TEST-PAGE-C");
        jdbc.update("UPDATE dm_drawing SET updated_at=? WHERE id=?", "2026-10-01T10:00:00Z", first.id());
        jdbc.update("UPDATE dm_drawing SET updated_at=? WHERE id=?", "2026-10-01T11:00:00Z", second.id());
        jdbc.update("UPDATE dm_drawing SET updated_at=? WHERE id=?", "2026-10-01T12:00:00Z", third.id());
        actor.set(1003);
        Page page = drawings.page("TEST-PAGE", "INDEPENDENT", false, 1, 2);
        assertEquals(3, page.total()); assertEquals(2, page.list().size()); assertEquals(third.id(), page.list().get(0).id());
        assertFalse(page.list().get(0).editable()); assertTrue(page.list().get(1).editable());
        assertEquals(first.id(), drawings.page("TEST-PAGE", "INDEPENDENT", false, 2, 2).list().get(0).id());
        assertEquals(2, drawings.page("TEST-PAGE", "", true, 1, 20).total());
        assertEquals(0, drawings.page("TEST-PAGE", "REQUIREMENT", false, 1, 20).total());
        assertEquals(3, drawings.page("自定义研发窗", "", false, 1, 20).total());
        assertEquals(3, drawings.page("测试备注", "", false, 1, 20).total());
        assertEquals(0, drawings.page("%", "", false, 1, 20).total());
        assertEquals(0, drawings.page("_", "", false, 1, 20).total());
        code(400, () -> drawings.page("", "WRONG", false, 1, 20));
        code(400, () -> drawings.page("", "", false, 0, 20));
        code(400, () -> drawings.page("", "", false, 1, 101));
    }

    @Test void tenantsAreIsolatedEvenForIdenticalBusinessNumbers() {
        Snapshot first = create("TEST-TENANT");
        TenantContextHolder.setTenantId(2L);
        code(404, () -> drawings.get(first.id(), null)); code(404, () -> drawings.versions(first.id()));
        assertEquals(0, drawings.page("", "", false, 1, 20).total());
        Snapshot other = create("TEST-TENANT"); assertEquals(2, other.tenantId());
        TenantContextHolder.setTenantId(1L);
        assertEquals(1, drawings.page("TEST-TENANT", "", false, 1, 20).total()); code(404, () -> drawings.get(other.id(), null));
    }

    @Test void creationRequestIsIdempotentPerCreatorAndCannotBeReusedForDifferentPayload() {
        String request = UUID.randomUUID().toString();
        Create input = new Create("TEST-REQUEST", "首次图", "", "C1", 1, 1200, 1500, request);
        Snapshot first = tx(() -> drawings.create(input)); assertEquals(first, tx(() -> drawings.create(input)));
        Snapshot second = tx(() -> drawings.save(first.id(), new Save(1, "已绘制", width(first, 1250))));
        assertEquals(second, tx(() -> drawings.create(input))); assertEquals(2, revisions(first.id()));
        code(409, () -> tx(() -> drawings.create(new Create("TEST-REQUEST", "不同需求", "", "C1", 1, 1200, 1500, request))));
        actor.set(1009);
        Snapshot other = tx(() -> drawings.create(new Create("TEST-REQUEST-OTHER", "首次图", "", "C1", 1, 1200, 1500, request)));
        assertNotEquals(first.id(), other.id()); assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing", Integer.class));
    }

    @Test void invalidGeometryDuplicateIdentityAndChangingSourceAreRejected() {
        Snapshot first = create("TEST-GEOMETRY");
        code(400, () -> tx(() -> drawings.save(first.id(), new Save(1, "无效尺寸", width(first, 0)))));
        ObjectNode duplicate = first.document().deepCopy(); ((ArrayNode)duplicate.path("windows")).add(duplicate.path("windows").get(0).deepCopy());
        code(400, () -> tx(() -> drawings.save(first.id(), new Save(1, "重复 ID", duplicate))));
        ObjectNode wrongId = first.document().deepCopy(); wrongId.put("designId", UUID.randomUUID().toString());
        code(400, () -> tx(() -> drawings.save(first.id(), new Save(1, "替换内部 ID", wrongId))));
        code(400, () -> tx(() -> drawings.save(first.id(), new Save(1, "篡改来源", first.document(), new Metadata(first.metadata().number(), "图纸", "", "REQUIREMENT")))));
        assertEquals(first, drawings.get(first.id(), null)); assertEquals(1, revisions(first.id()));
    }

    @Test void sharedCatalogAndAssetGatesRejectRawAndCanonicalDataWithoutNewVersions() {
        Snapshot first = create("TEST-GATES");
        doThrow(new ServiceException(400, "raw catalog rejected")).when(catalog).validateReferences(any(JsonNode.class));
        code(400, () -> tx(() -> drawings.save(first.id(), new Save(1, "非法目录引用", first.document()))));
        reset(catalog);
        doThrow(new ServiceException(400, "raw asset rejected")).when(assets).validateReferences(any(JsonNode.class));
        code(400, () -> tx(() -> drawings.save(first.id(), new Save(1, "非法资产引用", first.document()))));
        reset(assets);
        DrawingValidator changedValidator = mock(DrawingValidator.class);
        JsonNode canonical = width(first, 1300); when(changedValidator.validate(any(JsonNode.class))).thenReturn(canonical);
        DrawingService withCanonical = service(changedValidator);
        doAnswer(invocation -> { if (invocation.getArgument(0).equals(canonical)) throw new ServiceException(400, "canonical catalog rejected"); return null; }).when(catalog).validateReferences(any(JsonNode.class));
        code(400, () -> tx(() -> withCanonical.save(first.id(), new Save(1, "规范化后目录非法", first.document()))));
        reset(catalog);
        doAnswer(invocation -> { if (invocation.getArgument(0).equals(canonical)) throw new ServiceException(400, "canonical asset rejected"); return null; }).when(assets).validateReferences(any(JsonNode.class));
        code(400, () -> tx(() -> withCanonical.save(first.id(), new Save(1, "规范化后资产非法", first.document()))));
        assertEquals(1, revisions(first.id())); assertEquals(first, drawings.get(first.id(), null));
    }

    @Test void rollbackAndFileHashProtectionPreserveOnlyCommittedVersions() throws Exception {
        Snapshot rolled = transaction.execute(status -> { Snapshot doc = drawings.create(input("TEST-ROLLBACK")); status.setRollbackOnly(); return doc; });
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing", Integer.class));
        assertFalse(Files.exists(directory.resolve("drawings/tenant-1/" + rolled.id() + "/r1.json")));
        Snapshot first = create("TEST-HASH"); code(409, () -> files.write(first));
        Files.writeString(directory.resolve("drawings/tenant-1/" + first.id() + "/r1.json"), "{}");
        code(409, () -> drawings.get(first.id(), null)); assertEquals(1, revisions(first.id()));
    }

    @Test void missingValidatorNeverCreatesIndependentDrawing() {
        DrawingValidator missing = new DrawingValidator("C:/Program Files/nodejs/node.exe", directory.resolve("missing.mjs").toString(), directory.resolve("validation").toString(), mapper);
        code(503, () -> tx(() -> service(missing).create(input("TEST-FAIL-CLOSED"))));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing", Integer.class));
    }
}
