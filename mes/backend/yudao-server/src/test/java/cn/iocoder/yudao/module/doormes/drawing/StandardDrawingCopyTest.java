package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.asset.AssetService;
import cn.iocoder.yudao.module.doormes.catalog.CatalogService;
import cn.iocoder.yudao.module.doormes.requirement.RequirementService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StandardDrawingCopyTest {
    @TempDir Path directory;
    private final AtomicLong actor=new AtomicLong(1003);
    private JdbcTemplate jdbc;
    private DrawingService drawings;
    private TransactionTemplate transaction;
    private MockedStatic<SecurityFrameworkUtils> security;

    private static Path node(){
        return Arrays.stream(System.getenv("PATH").split(File.pathSeparator)).flatMap(dir->Arrays.stream(new String[]{"node","node.exe"}).map(name->Path.of(dir,name)))
            .filter(Files::isRegularFile).findFirst().orElseThrow(()->new IllegalStateException("Node.js is required for drawing tests"));
    }
    @BeforeEach void setup(){
        try{directory=directory.toRealPath();}catch(java.io.IOException e){throw new IllegalStateException(e);}
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(source);transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE dm_drawing(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,requirement_id CHAR(36),requirement_revision INT,line_id CHAR(36),revision INT,status VARCHAR(20),created_by BIGINT,updated_at VARCHAR(40),drawing_number VARCHAR_IGNORECASE(60),name VARCHAR(200),note VARCHAR(1000),source_type VARCHAR(20),created_at VARCHAR(40),create_request_id CHAR(36),create_payload_hash CHAR(64),standard_source_id CHAR(36),standard_source_revision INT,UNIQUE(tenant_id,drawing_number))");
        jdbc.execute("CREATE TABLE dm_drawing_version(tenant_id BIGINT,drawing_id CHAR(36),revision INT,json_path VARCHAR(160),sha256 CHAR(64),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,drawing_id,revision))");
        PermissionService permissions=mock(PermissionService.class);when(permissions.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        ObjectMapper mapper=new ObjectMapper();
        DrawingValidator validator=new DrawingValidator(node().toString(),DrawingServiceTest.validatorPath().toString(),directory.resolve("validation").toString(),mapper);
        drawings=new DrawingService(jdbc,mock(RequirementService.class),validator,new DrawingFiles(directory.resolve("drawings").toString(),mapper),permissions,mock(CatalogService.class),mock(AssetService.class));
        security=mockStatic(SecurityFrameworkUtils.class);security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(invocation->actor.get());
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear(){security.close();TenantContextHolder.clear();}
    @Test void copyPreservesGeometryButCreatesNewIdentityAndProvenance(){
        var source=transaction.execute(status->drawings.create(new DrawingModels.Create("STD-TEST","标准窗","", "C1",2,1200,1500,UUID.randomUUID().toString())));
        actor.set(1009);
        var copy=transaction.execute(status->drawings.copyFromStandard(source));
        assertNotEquals(source.id(),copy.id());assertEquals(1009,copy.createdBy());assertEquals("INDEPENDENT",copy.metadata().source());
        assertEquals(copy.id(),copy.document().path("designId").asText());
        assertEquals(source.document().path("windows"),copy.document().path("windows"));
        assertEquals(source.document(),drawings.get(source.id(),1).document());
        assertEquals(source.id(),jdbc.queryForObject("SELECT standard_source_id FROM dm_drawing WHERE id=?",String.class,copy.id()));
        assertEquals(1,jdbc.queryForObject("SELECT standard_source_revision FROM dm_drawing WHERE id=?",Integer.class,copy.id()));
        assertEquals(copy.document(),drawings.get(copy.id(),1).document());
    }
}
