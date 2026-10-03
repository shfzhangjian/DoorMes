package cn.iocoder.yudao.module.doormes.catalog;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.drawing.DrawingValidator;
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
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.doormes.catalog.CatalogModels.*;

class CatalogServiceTest {
    @TempDir Path directory;final ObjectMapper mapper=new ObjectMapper();JdbcTemplate jdbc;TransactionTemplate transaction;CatalogService service;CatalogFiles files;MockedStatic<SecurityFrameworkUtils> security;
    static Path validatorPath(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent()){Path file=p.resolve("frontend/vendor/doormes-engine/dist/validator.mjs");if(Files.isRegularFile(file))return file;}throw new IllegalStateException("Build engine first.");}
    @BeforeEach void setup(){
        JdbcDataSource data=new JdbcDataSource();data.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");jdbc=new JdbcTemplate(data);transaction=new TransactionTemplate(new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE dm_material_catalog(id CHAR(36) PRIMARY KEY,tenant_id BIGINT,category VARCHAR(20),code VARCHAR(60),name VARCHAR(160),revision INT,status VARCHAR(20),published_revision INT,updated_at VARCHAR(40),UNIQUE(tenant_id,code))");
        jdbc.execute("CREATE TABLE dm_material_catalog_version(tenant_id BIGINT,catalog_id CHAR(36),revision INT,status VARCHAR(20),json_path VARCHAR(160),sha256 CHAR(64),change_note VARCHAR(1000),changed_by BIGINT,updated_at VARCHAR(40),PRIMARY KEY(tenant_id,catalog_id,revision))");
        files=new CatalogFiles(directory.resolve("catalog").toString(),mapper);service=new CatalogService(jdbc,files,new DrawingValidator("C:/Program Files/nodejs/node.exe",validatorPath().toString(),directory.resolve("validation").toString(),mapper),mock(cn.iocoder.yudao.module.doormes.asset.AssetService.class));
        security=mockStatic(SecurityFrameworkUtils.class);security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1003L);TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear(){security.close();TenantContextHolder.clear();}
    <T>T tx(Supplier<T> call){return transaction.execute(status->call.get());}
    Input item(){return new Input("finish","TEST-FIN-001","深灰喷涂","RAL7016","", "metal","#374151",.7,.35,1,null,List.of());}
    Snapshot create(){return tx(()->service.create(new Mutation(0,"新建",item())));}
    Snapshot publish(Snapshot doc){return tx(()->service.publish(doc.id(),new Publish(doc.revision(),"发布选型")));}
    void code(int expected,Runnable call){assertEquals(expected,assertThrows(ServiceException.class,call::run).getCode());}
    @Test void publishCreatesImmutableVersionAndDraftDoesNotHideLatestPublished(){
        Snapshot first=create();assertEquals(0,service.page(null,null,true,1,20).total());Snapshot second=publish(first);assertEquals(2,second.revision());assertEquals(second,service.get(second.id(),2));assertEquals(first,service.get(second.id(),1));assertEquals(1,service.page("finish",null,true,1,20).total());
        Input modified=new Input("finish",item().code(),"白色喷涂","RAL9016","", "metal","#ffffff",.7,.35,1,null,List.of());Snapshot third=tx(()->service.revise(second.id(),new Mutation(2,"改色",modified)));
        assertEquals(3,third.revision());assertEquals(second,service.page("finish",null,true,1,20).list().get(0));
        service.validateReferences(second.data().get("appearance"));assertFalse(second.data().path("productionReady").asBoolean(true));assertEquals("design-only",second.data().path("scope").asText());
    }
    @Test void staleDuplicateAndIdentityChangesAreRejected(){Snapshot first=create();code(409,()->tx(()->service.create(new Mutation(0,"重复",item()))));code(409,()->tx(()->service.revise(first.id(),new Mutation(0,"过期",item()))));code(400,()->tx(()->service.revise(first.id(),new Mutation(1,"改型号",new Input("finish","OTHER","名称","规格","","metal","#ffffff",.1,.2,1,null,List.of())))));assertEquals(1,service.versions(first.id()).size());}
    @Test void draftAndForgedPublishedReferencesAreRejected(){Snapshot first=create();code(400,()->service.validateReferences(first.data().get("appearance")));Snapshot second=publish(first);service.validateReferences(second.data().get("appearance"));ObjectNode forged=second.data().get("appearance").deepCopy();forged.put("baseColor","#ffffff");code(400,()->service.validateReferences(forged));TenantContextHolder.setTenantId(2L);code(404,()->service.get(second.id(),null));code(404,()->service.validateReferences(second.data().get("appearance")));}
    @Test void rollbackAndHashChecksProtectImmutableFiles()throws Exception {Snapshot rolled=transaction.execute(status->{Snapshot doc=service.create(new Mutation(0,"回滚",item()));status.setRollbackOnly();return doc;});assertFalse(Files.exists(directory.resolve("catalog/tenant-1").resolve(rolled.id()).resolve("r1.json")));assertEquals(0,service.page(null,null,false,1,20).total());Snapshot first=create();code(409,()->files.write(first));Files.writeString(directory.resolve("catalog/tenant-1").resolve(first.id()).resolve("r1.json"),"{}");code(409,()->service.get(first.id(),null));}
    @Test void badParametersFailClosedAndGlassThicknessIsEnforced(){
        code(400,()->tx(()->service.create(new Mutation(0,"无效颜色",new Input("finish","BAD","名称","规格","","metal","url(fake)",.7,.35,1,null,List.of())))));
        Input glass=new Input("glass","TEST-GL-27","27mm玻璃","6+15+6","","glass","#c8e7f0",0,.1,.4,27.0,List.of("AL70"));Snapshot draft=tx(()->service.create(new Mutation(0,"玻璃",glass)));Snapshot selected=publish(draft);
        ObjectNode window=mapper.createObjectNode();window.set("defaultGlassSelection",selected.data().get("glassSelection"));window.putObject("sectionDimensions").put("glassDepthMm",24);ObjectNode drawing=mapper.createObjectNode();drawing.putArray("windows").add(window);
        code(400,()->service.validateReferences(drawing));window.withObject("sectionDimensions").put("glassDepthMm",27);service.validateReferences(drawing);
        code(400,()->service.page(null,null,false,1,101));code(400,()->service.page("unsafe",null,false,1,20));
    }
}
