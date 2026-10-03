package cn.iocoder.yudao.module.doormes.order;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.drawing.*;
import cn.iocoder.yudao.module.doormes.requirement.*;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.*;
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
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static cn.iocoder.yudao.module.doormes.order.ProductionOrderModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolated real JDBC/immutable requirement-file tests; no local production database is mutated. */
class ProductionOrderServiceTest {
    @TempDir Path directory;
    final ObjectMapper mapper=new ObjectMapper();
    final AtomicLong actor=new AtomicLong(1002);
    final Map<String,DrawingModels.Snapshot> snapshots=new HashMap<>();
    JdbcTemplate jdbc; TransactionTemplate transaction; ProductionOrderService service;
    DrawingService drawings; DrawingValidator validator; RequirementService requirements;
    PermissionService permissions; MockedStatic<SecurityFrameworkUtils> security;
    String drawingId;
    @BeforeEach void setup(){
        JdbcDataSource data=new JdbcDataSource();data.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(data);transaction=new TransactionTemplate(new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE dm_production_order(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,number VARCHAR_IGNORECASE(40),customer VARCHAR(200),project VARCHAR(200),order_type VARCHAR(20),status VARCHAR(20),revision INT,created_by BIGINT,assigned_to BIGINT,created_at VARCHAR(40),updated_at VARCHAR(40),requirement_id CHAR(36),UNIQUE(tenant_id,number))");
        jdbc.execute("CREATE TABLE dm_production_order_version(tenant_id BIGINT,order_id CHAR(36),revision INT,payload_json LONGTEXT,sha256 CHAR(64),action VARCHAR(40),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,order_id,revision))");
        jdbc.execute("CREATE TABLE dm_design_requirement(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,number VARCHAR(40),customer VARCHAR(200),project VARCHAR(200),revision INT,status VARCHAR(20),created_by BIGINT,assigned_to BIGINT,line_count INT,quantity INT,created_at VARCHAR(40),updated_at VARCHAR(40),UNIQUE(tenant_id,number))");
        jdbc.execute("CREATE TABLE dm_design_requirement_version(tenant_id BIGINT,requirement_id CHAR(36),revision INT,json_path VARCHAR(160),sha256 CHAR(64),action VARCHAR(20),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,requirement_id,revision))");
        jdbc.execute("CREATE TABLE dm_drawing_version(tenant_id BIGINT,drawing_id CHAR(36),revision INT,sha256 CHAR(64),PRIMARY KEY(tenant_id,drawing_id,revision))");
        drawings=mock(DrawingService.class);validator=mock(DrawingValidator.class);permissions=mock(PermissionService.class);
        when(permissions.hasAnyRoles(anyLong(),any(String[].class))).thenAnswer(i->i.<Long>getArgument(0)==1001L);
        when(permissions.hasAnyPermissions(anyLong(),any(String[].class))).thenAnswer(i->{
            long user=i.getArgument(0);String[] wanted=(String[])i.getRawArguments()[1];
            Set<String> allowed=new HashSet<>(Set.of("doormes:production:query","doormes:changes:query"));
            if(user==1002)allowed.addAll(Set.of("doormes:orders:create","doormes:orders:update","doormes:orders:submit"));
            if(user==1003)allowed.addAll(Set.of("doormes:design:claim","doormes:design:drawing-save","doormes:orders:bind","doormes:orders:change-request"));
            if(user==1007)allowed.add("doormes:orders:change-review");
            if(user==1006)allowed.add("doormes:orders:change-apply");
            return Arrays.stream(wanted).anyMatch(allowed::contains);
        });
        requirements=new RequirementService(jdbc,new RequirementFiles(directory.resolve("requirements").toString(),mapper),permissions);
        when(drawings.get(anyString(),anyInt())).thenAnswer(i->{var found=snapshots.get(i.getArgument(0)+":"+i.getArgument(1));if(found==null||found.tenantId()!=TenantContextHolder.getRequiredTenantId())throw new ServiceException(404,"版本不存在");return found;});
        when(validator.bom(any())).thenAnswer(i->{JsonNode doc=i.getArgument(0);ObjectNode result=mapper.createObjectNode().put("schemaVersion","doormes-order-bom.v1").put("designId",doc.path("designId").asText());
            var lines=result.putArray("lines");for(String side:List.of("left","right","top","bottom")){
                var line=lines.addObject().put("objectId",doc.path("designId").asText()+":"+side).put("displayCode","C1-FR-"+side).put("modelCode","AL70-FR").put("name",side).put("category","profile").put("unit","pcs").put("quantity",2).put("productionReady",false);
                line.putObject("dimensions").put("lengthMm",side.equals("top")||side.equals("bottom")?doc.path("widthMm").asInt():1500);
            }return result;
        });
        service=new ProductionOrderService(jdbc,mapper,drawings,validator,requirements,permissions);
        security=mockStatic(SecurityFrameworkUtils.class);security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(i->actor.get());TenantContextHolder.setTenantId(1L);
        drawingId=UUID.randomUUID().toString();snapshot(drawingId,1,1200,null,null);snapshot(drawingId,2,1250,null,null);snapshot(drawingId,3,1300,null,null);
    }
    @AfterEach void clear(){security.close();TenantContextHolder.clear();}
    <T>T tx(Supplier<T> action){return transaction.execute(status->action.get());}
    void code(int expected,Runnable action){assertEquals(expected,assertThrows(ServiceException.class,action::run).getCode());}
    DrawingModels.Snapshot snapshot(String id,int revision,int width,String requirementId,String lineId){
        ObjectNode doc=mapper.createObjectNode().put("designId",id).put("revision",revision).put("widthMm",width);
        var result=new DrawingModels.Snapshot(DrawingModels.SCHEMA,id,1L,requirementId,requirementId==null?0:3,lineId,revision,"DRAFT",1003,1003,"created","updated","test",doc,new DrawingModels.Metadata("DW-TEST","TEST","","INDEPENDENT"),true);
        snapshots.put(id+":"+revision,result);jdbc.update("INSERT INTO dm_drawing_version VALUES(?,?,?,?)",1L,id,revision,"hash-"+revision);return result;
    }
    Create standard(String number){return new Create(number,"TEST客户","TEST项目","STANDARD",3,"测试套数",drawingId,1,null);}
    Create custom(String number){return new Create(number,"TEST客户","TEST项目","CUSTOM",3,"定制测试",null,null,new Custom("C1",1200,1500,"AL70","GL24","HW-TT","白色","2026-12-01","测试备注"));}
    Order create(){return tx(()->service.create(standard("TEST-STANDARD")));}

    @Test void standardFreezesExplicitVersionAndMultipliesOnlyOrderSetCount(){
        Order first=create();assertEquals("BOM_READY",first.status());assertFalse(first.productionReady());assertEquals(1,first.drawing().revision());
        assertEquals(4,first.bom().path("lines").size());assertEquals(2,first.bom().path("lines").get(0).path("quantity").asInt());assertEquals(6,first.bom().path("lines").get(0).path("totalQuantity").asInt());
        assertEquals(first,service.get(first.id(),1));assertEquals(1,service.get(first.id(),null).drawing().revision());verify(drawings,never()).get(anyString(),isNull());
    }
    @Test void customCreatesRealDemandClaimsOpensAndBindsItsOwnDrawing(){
        Order draft=tx(()->service.create(custom("TEST-CUSTOM")));assertEquals("DRAFT",draft.status());
        code(409,()->service.assertIndependentRequirement(draft.requirement().id()));
        var demand=requirements.get(draft.requirement().id(),null);assertEquals(1,demand.demand().lines().get(0).quantity());assertEquals(3,draft.quantity());
        Order submitted=tx(()->service.submit(draft.id(),new Action(1,"提交研发")));actor.set(1003);
        Order claimed=tx(()->service.claim(draft.id(),new Action(submitted.revision(),"领用研发")));assertEquals(1003,claimed.assignedTo());
        var snapshot=snapshot(UUID.randomUUID().toString(),1,1200,claimed.requirement().id(),claimed.requirement().lineId());
        when(drawings.open(any())).thenReturn(snapshot);
        var opened=tx(()->service.openDrawing(claimed.id(),new Action(claimed.revision(),"绘图")));assertEquals(claimed.revision(),opened.order().revision());assertNull(opened.order().drawing());
        Order bound=tx(()->service.bind(claimed.id(),new Bind(claimed.revision(),snapshot.id(),1,"绑定组成件基线")));assertEquals("BOM_READY",bound.status());assertEquals(6,bound.bom().path("lines").get(0).path("totalQuantity").asInt());
        assertEquals("DRAFT",service.get(bound.id(),1).status());assertEquals("IN_DESIGN",requirements.get(bound.requirement().id(),null).status());
    }
    @Test void changeApprovalDoesNotReplaceBaselineUntilProductionExplicitlyApplies(){
        Order first=create();actor.set(1003);Order requested=tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,2,"加宽")));
        Change change=requested.changes().get(0);assertEquals(2,change.diff().modified().size());assertTrue(change.diff().added().isEmpty());assertEquals(1,requested.drawing().revision());
        actor.set(1007);Order approved=tx(()->service.review(first.id(),new Review(2,change.id(),true,"审核同意")));assertEquals(1,approved.drawing().revision());assertEquals("APPROVED",approved.changes().get(0).status());
        actor.set(1006);Order applied=tx(()->service.apply(first.id(),new Apply(3,change.id(),"确认采用R2")));assertEquals(2,applied.drawing().revision());assertFalse(applied.productionReady());
        assertEquals(1,service.get(first.id(),1).drawing().revision());assertEquals("PENDING",service.get(first.id(),2).changes().get(0).status());assertEquals(4,service.versions(first.id()).size());
    }
    @Test void rolesCannotCreateBindReviewOrApplyOutsideTheirAuthority(){
        actor.set(1003);code(403,()->tx(()->service.create(standard("NO-CREATE"))));actor.set(1002);Order first=create();
        code(403,()->tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,2,"销售越权"))));actor.set(1003);
        var requested=tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,2,"设计申请")));
        code(403,()->tx(()->service.review(first.id(),new Review(2,requested.changes().get(0).id(),true,"自审"))));
        code(403,()->tx(()->service.apply(first.id(),new Apply(2,requested.changes().get(0).id(),"越权应用"))));
    }
    @Test void optimisticVersionAndPendingChangePreventLostUpdates(){
        Order first=create();actor.set(1003);var requested=tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,2,"申请")));
        code(409,()->tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,3,"过期"))));
        code(409,()->tx(()->service.requestChange(first.id(),new RequestChange(2,drawingId,3,"重复"))));assertEquals(2,service.get(first.id(),null).revision());
        actor.set(1006);code(409,()->tx(()->service.apply(first.id(),new Apply(2,requested.changes().get(0).id(),"未审核"))));
    }
    @Test void rejectionAllowsASeparateNewRequestAndAuditRemains(){
        Order first=create();actor.set(1003);var requested=tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,2,"申请")));
        actor.set(1007);tx(()->service.review(first.id(),new Review(2,requested.changes().get(0).id(),false,"需重画")));actor.set(1003);
        var retried=tx(()->service.requestChange(first.id(),new RequestChange(3,drawingId,3,"重画后申请")));assertEquals(2,retried.changes().size());assertEquals("REJECTED",retried.changes().get(0).status());assertEquals(1,retried.drawing().revision());
    }
    @Test void crossTenantReadsAndCorruptSnapshotsAreRejected(){
        Order first=create();TenantContextHolder.setTenantId(2L);code(404,()->service.get(first.id(),null));TenantContextHolder.setTenantId(1L);
        jdbc.update("UPDATE dm_production_order_version SET payload_json='{}' WHERE order_id=?",first.id());code(409,()->service.get(first.id(),null));
    }
    @Test void duplicateOrderRollsBackItsNewRequirementAndImmutableFile(){
        tx(()->service.create(custom("TEST-DUPLICATE")));int before=jdbc.queryForObject("SELECT COUNT(*) FROM dm_design_requirement",Integer.class);
        code(409,()->tx(()->service.create(custom("test-duplicate"))));assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM dm_design_requirement",Integer.class));assertEquals(1,service.page("TEST-DUPLICATE","CUSTOM","DRAFT",1,20).getTotal());
    }
    @Test void wrongDrawingCannotBeBoundAndEmptyBomCannotEstablishBaseline(){
        Order draft=tx(()->service.create(custom("TEST-WRONG")));tx(()->service.submit(draft.id(),new Action(1,"提交")));actor.set(1003);Order claimed=tx(()->service.claim(draft.id(),new Action(2,"领用")));
        code(400,()->tx(()->service.bind(draft.id(),new Bind(claimed.revision(),drawingId,1,"不相关图纸"))));
        actor.set(1002);doReturn(mapper.createObjectNode().set("lines",mapper.createArrayNode())).when(validator).bom(any());code(400,()->tx(()->service.create(standard("EMPTY-BOM"))));assertEquals(1,service.page("","","",1,20).getTotal());
    }
    @Test void administratorCannotSelfApproveAndApplyRechecksExactDrawingHash(){
        Order first=create();actor.set(1001);Order request=tx(()->service.requestChange(first.id(),new RequestChange(1,drawingId,2,"管理员申请")));String changeId=request.changes().get(0).id();
        code(403,()->tx(()->service.review(first.id(),new Review(2,changeId,true,"管理员自审"))));actor.set(1007);tx(()->service.review(first.id(),new Review(2,changeId,true,"独立审核")));
        jdbc.update("UPDATE dm_drawing_version SET sha256='tampered' WHERE drawing_id=? AND revision=2",drawingId);actor.set(1006);code(409,()->tx(()->service.apply(first.id(),new Apply(3,changeId,"应用"))));assertEquals(1,service.get(first.id(),null).drawing().revision());
    }
    @Test void draftUpdateAdvancesBothBaselinesAndKeepsOriginalHistory(){
        var draft=tx(()->service.create(custom("TEST-EDIT")));var replacement=new Custom("C2",1300,1600,"AL90","GL30","HW-TT","黑色","2026-12-02","新要求");
        var updated=tx(()->service.update(draft.id(),new Update(1,"TEST-EDIT-2","新客户","新项目","CUSTOM",4,"新备注",replacement)));
        assertEquals(2,updated.revision());assertEquals(2,updated.requirement().revision());assertEquals(4,updated.quantity());assertEquals("TEST-EDIT-2",service.page("TEST-EDIT-2","CUSTOM","DRAFT",1,20).getList().get(0).number());
        assertEquals(1300,requirements.get(updated.requirement().id(),2).demand().lines().get(0).requirement().widthMm());assertEquals(1,requirements.get(updated.requirement().id(),2).demand().lines().get(0).quantity());
        assertEquals("TEST-EDIT",service.get(draft.id(),1).number());assertEquals(1200,requirements.get(draft.requirement().id(),1).demand().lines().get(0).requirement().widthMm());
        tx(()->service.submit(draft.id(),new Action(2,"提交")));code(409,()->tx(()->service.update(draft.id(),new Update(3,"TEST-EDIT-2","客户","项目","CUSTOM",4,"备注",replacement))));
    }
}
