package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.requirement.*;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.doormes.drawing.DrawingModels.*;

/** H2 + temporary snapshots + real shared Node geometry validation. No live SQL. */
class DrawingServiceTest {
    @TempDir Path directory;
    final ObjectMapper mapper=new ObjectMapper();final AtomicLong actor=new AtomicLong(1002);
    JdbcTemplate jdbc;TransactionTemplate transaction;RequirementService requirements;DrawingService drawings;DrawingFiles files;
    MockedStatic<SecurityFrameworkUtils> security;RequirementModels.Document requirement;
    static Path validatorPath() {
        for(Path part=Path.of("").toAbsolutePath();part!=null;part=part.getParent()) {
            Path script=part.resolve("frontend/vendor/doormes-engine/dist/validator.mjs");if(Files.isRegularFile(script))return script;
        }
        throw new IllegalStateException("Build the shared engine validator before backend tests.");
    }
    @BeforeEach void setup() {
        JdbcDataSource data=new JdbcDataSource();data.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(data);transaction=new TransactionTemplate(new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE dm_design_requirement(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,number VARCHAR(40),customer VARCHAR(200),project VARCHAR(200),revision INT,status VARCHAR(20),created_by BIGINT,assigned_to BIGINT,line_count INT,quantity INT,created_at VARCHAR(40),updated_at VARCHAR(40),UNIQUE(tenant_id,number),UNIQUE(tenant_id,id))");
        jdbc.execute("CREATE TABLE dm_design_requirement_version(tenant_id BIGINT,requirement_id CHAR(36),revision INT,json_path VARCHAR(160),sha256 CHAR(64),action VARCHAR(20),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,requirement_id,revision))");
        jdbc.execute("CREATE TABLE dm_drawing(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,requirement_id CHAR(36),requirement_revision INT,line_id CHAR(36),revision INT,status VARCHAR(20),created_by BIGINT,updated_at VARCHAR(40),drawing_number VARCHAR(60),name VARCHAR(200),note VARCHAR(1000),source_type VARCHAR(20),created_at VARCHAR(40),create_request_id CHAR(36),create_payload_hash CHAR(64),UNIQUE(tenant_id,requirement_id,line_id),UNIQUE(tenant_id,drawing_number),UNIQUE(tenant_id,created_by,create_request_id))");
        jdbc.execute("CREATE TABLE dm_drawing_version(tenant_id BIGINT,drawing_id CHAR(36),revision INT,json_path VARCHAR(160),sha256 CHAR(64),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,drawing_id,revision))");
        PermissionService permissions=mock(PermissionService.class);
        when(permissions.hasAnyPermissions(eq(1003L),any(String[].class))).thenReturn(true);
        requirements=new RequirementService(jdbc,new RequirementFiles(directory.resolve("requirements").toString(),mapper),permissions);
        files=new DrawingFiles(directory.resolve("drawings").toString(),mapper);
        DrawingValidator validator=new DrawingValidator("C:/Program Files/nodejs/node.exe",validatorPath().toString(),directory.resolve("validation").toString(),mapper);
        drawings=new DrawingService(jdbc,requirements,validator,files,permissions,mock(cn.iocoder.yudao.module.doormes.catalog.CatalogService.class),mock(cn.iocoder.yudao.module.doormes.asset.AssetService.class));
        security=mockStatic(SecurityFrameworkUtils.class);security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(invocation->actor.get());TenantContextHolder.setTenantId(1L);
        var demand=new RequirementModels.Demand(1200,1500,"材料自由文本","玻璃自由文本","五金自由文本","RAL7016","2026-10-15","左内开");
        requirement=tx(()->requirements.create(new RequirementModels.Mutation(0,"测试",new RequirementModels.Input(RequirementModels.INPUT_SCHEMA,"TEST-DRAWING","测试客户","",List.of(new RequirementModels.Line(null,"C1","custom",2,demand)),""))));
        requirement=tx(()->requirements.submit(requirement.id(),new RequirementModels.Action(1,"提交")));actor.set(1003);
        requirement=tx(()->requirements.claim(requirement.id(),new RequirementModels.Action(2,"领用")));
    }
    @AfterEach void clear(){if(security!=null)security.close();TenantContextHolder.clear();}
    <T>T tx(Supplier<T> action){return transaction.execute(status->action.get());}
    Open input(){return new Open(requirement.id(),requirement.demand().lines().get(0).id(),requirement.revision());}
    Snapshot open(){return tx(()->drawings.open(input()));}
    void code(int expected,Runnable action){assertEquals(expected,assertThrows(ServiceException.class,action::run).getCode());}
    @Test void seedRoundtripAndRevisionConflict() {
        assertNull(drawings.find(input().requirementId(),input().lineId()));Snapshot first=open();
        assertEquals(first,drawings.get(first.id(),null));assertEquals(first,open());
        assertEquals(first.id(),first.document().path("designId").asText());assertEquals(1200,first.document().path("windows").get(0).path("widthMm").asInt());
        assertEquals(2,first.document().path("windows").get(0).path("quantity").asInt());
        ObjectNode changed=first.document().deepCopy();((ObjectNode)changed.path("windows").get(0)).put("widthMm",1250);
        Snapshot second=tx(()->drawings.save(first.id(),new Save(1,"调整宽度",changed)));
        assertEquals(2,second.revision());assertEquals(1250,second.document().path("windows").get(0).path("widthMm").asInt());
        Snapshot history=drawings.get(first.id(),1);
        assertEquals(first.document(),history.document());assertEquals(first.metadata(),history.metadata());
        assertFalse(history.editable());assertEquals(2,drawings.versions(first.id()).size());
        code(409,()->tx(()->drawings.save(first.id(),new Save(1,"过期",changed))));
    }
    @Test void realGeometryValidatorRejectsBadSizeAndDuplicateIdentity() {
        Snapshot first=open();ObjectNode invalid=first.document().deepCopy();((ObjectNode)invalid.path("windows").get(0)).put("widthMm",0);
        code(400,()->tx(()->drawings.save(first.id(),new Save(1,"尺寸无效",invalid))));
        ObjectNode duplicate=first.document().deepCopy();((com.fasterxml.jackson.databind.node.ArrayNode)duplicate.path("windows")).add(duplicate.path("windows").get(0).deepCopy());
        code(400,()->tx(()->drawings.save(first.id(),new Save(1,"内部 ID 重复",duplicate))));
        assertEquals(first,drawings.get(first.id(),null));assertEquals(1,drawings.versions(first.id()).size());
    }
    @Test void identityOwnerTenantAndLineAreEnforced() {
        Snapshot first=open();ObjectNode changed=first.document().deepCopy();changed.put("designId",UUID.randomUUID().toString());
        code(400,()->tx(()->drawings.save(first.id(),new Save(1,"篡改图纸 ID",changed))));
        code(404,()->tx(()->drawings.open(new Open(requirement.id(),UUID.randomUUID().toString(),3))));
        actor.set(1009);code(403,()->tx(()->drawings.save(first.id(),new Save(1,"越权",first.document()))));
        TenantContextHolder.setTenantId(2L);code(404,()->drawings.get(first.id(),null));code(404,()->drawings.find(requirement.id(),input().lineId()));
    }
    @Test void rollbackCleansOnlyNewFileAndTamperingIsRejected() throws Exception {
        Snapshot rolled=transaction.execute(status->{Snapshot doc=drawings.open(input());status.setRollbackOnly();return doc;});
        assertFalse(Files.exists(directory.resolve("drawings/tenant-1").resolve(rolled.id()).resolve("r1.json")));assertNull(drawings.find(input().requirementId(),input().lineId()));
        Snapshot first=open();code(409,()->files.write(first));Files.writeString(directory.resolve("drawings/tenant-1").resolve(first.id()).resolve("r1.json"),"{}");
        code(409,()->drawings.get(first.id(),null));
    }
    @Test void staleRequirementAndMissingValidatorNeverCreateDrawing() {
        code(409,()->tx(()->drawings.open(new Open(requirement.id(),input().lineId(),2))));
        DrawingValidator missing=new DrawingValidator("C:/Program Files/nodejs/node.exe",directory.resolve("missing.mjs").toString(),directory.resolve("validation").toString(),mapper);
        code(503,()->missing.validate(mapper.createObjectNode()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing",Integer.class));
    }
}
