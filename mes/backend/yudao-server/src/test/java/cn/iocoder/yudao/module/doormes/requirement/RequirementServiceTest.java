package cn.iocoder.yudao.module.doormes.requirement;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
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
import static cn.iocoder.yudao.module.doormes.requirement.RequirementModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated H2 and temporary JSON files; never uses live accounts, MySQL or Redis. */
class RequirementServiceTest {
    @TempDir Path directory;
    JdbcTemplate jdbc;
    TransactionTemplate transaction;
    RequirementFiles files;
    RequirementService service;
    MockedStatic<SecurityFrameworkUtils> security;
    AtomicLong actor = new AtomicLong(1002);
    @BeforeEach void setup() {
        JdbcDataSource data = new JdbcDataSource();
        data.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(data);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE dm_design_requirement (id CHAR(36) PRIMARY KEY,tenant_id BIGINT NOT NULL,number VARCHAR(40),customer VARCHAR(200),project VARCHAR(200),revision INT,status VARCHAR(20),created_by BIGINT,assigned_to BIGINT,line_count INT,quantity INT,created_at VARCHAR(40),updated_at VARCHAR(40),UNIQUE(tenant_id,number),UNIQUE(tenant_id,id))");
        jdbc.execute("CREATE TABLE dm_design_requirement_version (tenant_id BIGINT,requirement_id CHAR(36),revision INT,json_path VARCHAR(160),sha256 CHAR(64),action VARCHAR(20),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,requirement_id,revision),FOREIGN KEY(tenant_id,requirement_id) REFERENCES dm_design_requirement(tenant_id,id))");
        files = new RequirementFiles(directory.toString(),new ObjectMapper());
        service = new RequirementService(jdbc,files,mock(PermissionService.class));
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(invocation -> actor.get());
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear() { if (security!=null) security.close(); TenantContextHolder.clear(); }
    <T> T tx(Supplier<T> action) { return transaction.execute(status -> action.get()); }
    Input input(String number,double width) {
        return new Input(INPUT_SCHEMA,number,"测试客户","测试项目",List.of(new Line(null,"C1","custom",2,new Demand(width,1500,"AL70","GL24","HW-TT","RAL7016","2026-10-15","开启要求"))),"需求备注");
    }
    Document create() { return tx(() -> service.create(new Mutation(0,"新建",input("REQ-001",1200)))); }
    void code(int expected,Runnable action) { assertEquals(expected,assertThrows(ServiceException.class,action::run).getCode()); }

    @Test void draftRevisionSubmitClaimAndHistoricalRoundtrip() {
        Document first = create();
        assertEquals(first,service.get(first.id(),null));
        assertNotNull(first.demand().lines().get(0).id());
        Line line = first.demand().lines().get(0);
        Demand r = line.requirement();
        Input changed = new Input(INPUT_SCHEMA,"REQ-001","测试客户","测试项目",List.of(new Line(line.id(),line.mark(),"custom",2,new Demand(1250,r.heightMm(),r.material(),r.glass(),r.hardware(),r.finish(),r.dueDate(),r.note()))),"需求备注");
        Document second = tx(() -> service.update(first.id(),new Mutation(1,"调整宽度",changed)));
        assertEquals(2,second.revision());
        assertEquals(first,service.get(first.id(),1));
        code(409,() -> tx(() -> service.update(first.id(),new Mutation(1,"过期",changed))));
        Document submitted = tx(() -> service.submit(first.id(),new Action(2,"提交")));
        assertEquals("SUBMITTED",submitted.status());
        actor.set(1003);
        Document claimed = tx(() -> service.claim(first.id(),new Action(3,"领用")));
        assertEquals("IN_DESIGN",claimed.status()); assertEquals(1003L,claimed.assignedTo());
        actor.set(1002);
        code(409,() -> tx(() -> service.update(first.id(),new Mutation(4,"不应覆盖",changed))));
        assertEquals(4,service.versions(first.id()).size());
        assertEquals(1,service.page("测试客户","IN_DESIGN",1,20).getTotal());
    }
    @Test void tenantScopeIsAuthoritativeAndShortNumbersAreTenantLocal() {
        Document first = create(); TenantContextHolder.setTenantId(2L);
        assertEquals(0,service.page("","",1,20).getTotal());
        code(404,() -> service.get(first.id(),null));
        code(404,() -> service.versions(first.id()));
        code(404,() -> tx(() -> service.update(first.id(),new Mutation(1,"跨厂",first.demand()))));
        assertNotEquals(first.id(),create().id());
    }
    @Test void nonOwnerCannotReviseOrSubmit() {
        Document doc = create(); actor.set(1009);
        code(403,() -> tx(() -> service.update(doc.id(),new Mutation(1,"越权",doc.demand()))));
        code(403,() -> tx(() -> service.submit(doc.id(),new Action(1,"越权"))));
    }
    @Test void databaseRollbackRemovesOnlyItsNewSnapshot() throws Exception {
        Document doc = transaction.execute(status -> { Document result = service.create(new Mutation(0,"回滚测试",input("REQ-ROLLBACK",1200))); status.setRollbackOnly(); return result; });
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dm_design_requirement",Integer.class));
        assertFalse(Files.exists(directory.resolve("tenant-1").resolve(doc.id()).resolve("r1.json")));
        Document saved = create();
        code(409,() -> tx(() -> service.create(new Mutation(0,"重号",input("REQ-001",1200)))));
        assertEquals(saved,service.get(saved.id(),null));
    }
    @Test void immutableFilesRejectOverwriteAndTampering() throws Exception {
        Document doc = create(); code(409,() -> files.write(doc));
        Files.writeString(directory.resolve("tenant-1").resolve(doc.id()).resolve("r1.json"),"{}");
        code(409,() -> service.get(doc.id(),null));
        code(400,() -> service.get("../escape",null));
    }
    @Test void badDemandAndForgedLineIdentityAreRejected() {
        Input good = input("REQ-001",1200); Line line = good.lines().get(0);
        code(400,() -> RequirementService.normalize(new Input(INPUT_SCHEMA,good.number(),good.customer(),good.project(),List.of(line,new Line(null,"c1","custom",1,line.requirement())),""),null));
        code(501,() -> RequirementService.normalize(new Input(INPUT_SCHEMA,good.number(),good.customer(),good.project(),List.of(new Line(null,"C1","standard",1,line.requirement())),""),null));
        code(400,() -> RequirementService.normalize(new Input(INPUT_SCHEMA,good.number(),good.customer(),good.project(),List.of(new Line(UUID.randomUUID().toString(),"C1","custom",1,line.requirement())),""),null));
    }
    @Test void beanValidationRejectsInvalidGeometryAndNullLines() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new Mutation(0,"新建",input("REQ-001",0))).isEmpty());
            Input nullLine = new Input(INPUT_SCHEMA,"REQ-001","客户","",Arrays.asList((Line)null),"");
            assertFalse(factory.getValidator().validate(new Mutation(0,"新建",nullLine)).isEmpty());
        }
    }
    @Test void onlySubmittedCompleteDemandsCanBeClaimed() {
        Document doc = create();
        code(409,() -> tx(() -> service.claim(doc.id(),new Action(1,"未提交"))));
        Demand incomplete = new Demand(1200,1500,"","","","","","");
        Document other = tx(() -> service.create(new Mutation(0,"不完整",new Input(INPUT_SCHEMA,"REQ-002","客户","",List.of(new Line(null,"C1","custom",1,incomplete)),""))));
        code(400,() -> tx(() -> service.submit(other.id(),new Action(1,"缺材料"))));
        assertEquals(1,service.get(other.id(),null).revision());
    }
}
