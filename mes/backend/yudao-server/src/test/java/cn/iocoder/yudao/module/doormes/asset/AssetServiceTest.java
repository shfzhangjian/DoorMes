package cn.iocoder.yudao.module.doormes.asset;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.drawing.DrawingValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.doormes.asset.AssetModels.*;

/** Isolated H2/temp files plus the real Node format gate, no live database fixtures. */
class AssetServiceTest {
 @TempDir Path root;final ObjectMapper mapper=new ObjectMapper();final AtomicLong actor=new AtomicLong(1003);JdbcTemplate jdbc;TransactionTemplate transaction;AssetFiles files;AssetService service;MockedStatic<SecurityFrameworkUtils> security;
 static Path validatorPath(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent()){Path candidate=p.resolve("frontend/vendor/doormes-engine/dist/validator.mjs");if(Files.isRegularFile(candidate))return candidate;}throw new IllegalStateException("Build engine first");}
 @BeforeEach void setup(){JdbcDataSource data=new JdbcDataSource();data.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");jdbc=new JdbcTemplate(data);transaction=new TransactionTemplate(new DataSourceTransactionManager(data));
  jdbc.execute("CREATE TABLE dm_visual_asset(tenant_id BIGINT,asset_id VARCHAR(100),blob_id CHAR(36),kind VARCHAR(30),media_type VARCHAR(40),content_hash VARCHAR(71),byte_length INT,metadata_hash CHAR(64),uploaded_by BIGINT,stored_at VARCHAR(40),PRIMARY KEY(tenant_id,asset_id))");
  jdbc.execute("CREATE TABLE dm_visual_asset_upload(grant_id CHAR(36) PRIMARY KEY,tenant_id BIGINT,actor_id BIGINT,asset_id VARCHAR(100),content_hash VARCHAR(71),media_type VARCHAR(40),byte_length INT,expires_at BIGINT)");
  files=new AssetFiles(root.resolve("assets").toString(),mapper);service=new AssetService(jdbc,files,new DrawingValidator("C:/Program Files/nodejs/node.exe",validatorPath().toString(),root.resolve("validation").toString(),mapper),mapper);
  security=mockStatic(SecurityFrameworkUtils.class);security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(i->actor.get());TenantContextHolder.setTenantId(1L);
 }
 @AfterEach void clear(){security.close();TenantContextHolder.clear();}
 <T>T tx(Supplier<T> action){return transaction.execute(s->action.get());}
 void code(int expected,Runnable action){assertEquals(expected,assertThrows(ServiceException.class,action::run).getCode());}
 byte[] gltf(){return "{\"asset\":{\"version\":\"2.0\"},\"nodes\":[{}]}".getBytes(StandardCharsets.UTF_8);}
 byte[] png(){try{var stream=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,3,BufferedImage.TYPE_INT_RGB),"png",stream);return stream.toByteArray();}catch(IOException e){throw new UncheckedIOException(e);}}
 Grant grant(String id,String media,byte[] bytes){return tx(()->service.authorize(new Authorize(id,"sha256:"+AssetFiles.digest(bytes),media,bytes.length)));}
 Evidence upload(String id,String media,byte[] bytes){Grant g=grant(id,media,bytes);return tx(()->service.upload(g.grantId(),media,new ByteArrayInputStream(bytes)));}
 @Test void exactModelRoundtripAndReplayRefused(){byte[] bytes=gltf();Grant g=grant("ASSET-MODEL","model/gltf+json",bytes);Evidence e=tx(()->service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream(bytes)));assertArrayEquals(bytes,service.read(e.asset().assetId(),e.asset().contentHash()));assertEquals(1,e.inspection().path("nodeCount").asInt());assertEquals(1,service.page(null,null,1,20).total());code(404,()->tx(()->service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream(bytes))));assertEquals(e.asset(),service.get("ASSET-MODEL"));}
 @Test void idCannotBeReboundAndContentPinCannotBeChanged(){Evidence e=upload("ASSET-MODEL","model/gltf+json",gltf());code(409,()->grant("ASSET-MODEL","model/gltf+json","{}".getBytes()));code(409,()->service.read("ASSET-MODEL","sha256:"+"f".repeat(64)));assertEquals(e,upload("ASSET-MODEL","model/gltf+json",gltf()));}
 @Test void grantsBoundToActorTenantAndExpiry(){Grant g=grant("ASSET-MODEL","model/gltf+json",gltf());actor.set(1005);code(403,()->tx(()->service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream(gltf()))));actor.set(1003);TenantContextHolder.setTenantId(2L);code(404,()->tx(()->service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream(gltf()))));TenantContextHolder.setTenantId(1L);jdbc.update("UPDATE dm_visual_asset_upload SET expires_at=0");code(409,()->tx(()->service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream(gltf()))));}
 @Test void mimeLengthHashExternalUriAndVersionRejected(){Grant g=grant("ASSET-MODEL","model/gltf+json",gltf());code(400,()->tx(()->service.upload(g.grantId(),"image/png",new ByteArrayInputStream(gltf()))));code(400,()->tx(()->service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream("{}".getBytes()))));code(400,()->upload("ASSET-BAD","model/gltf+json","{\"asset\":{\"version\":\"2.0\"},\"buffers\":[{\"uri\":\"secret.bin\"}]}".getBytes()));code(400,()->upload("ASSET-V1","model/gltf+json","{\"asset\":{\"version\":\"1.0\"}}".getBytes()));assertEquals(0,service.page(null,null,1,20).total());}
 @Test void textureIsActuallyDecodedAndReferencesVerified()throws Exception{Evidence e=upload("MES-TEXTURE-TEST","image/png",png());assertEquals(2,e.inspection().path("width").asInt());assertEquals(3,e.inspection().path("height").asInt());var reference=mapper.createObjectNode().put("textureSetId",e.asset().assetId()).put("textureContentHash",e.asset().contentHash());service.validateReferences(reference);reference.put("textureContentHash","sha256:"+"f".repeat(64));code(400,()->service.validateReferences(reference));code(400,()->upload("MES-TEXTURE-BAD","image/png",gltf()));code(400,()->upload("MES-TEXTURE-MIME","image/jpeg",png()));TenantContextHolder.setTenantId(2L);code(404,()->service.get(e.asset().assetId()));}
 @Test void oversizedAndBadIdentifiersRejectedBeforeWrite(){code(400,()->tx(()->service.authorize(new Authorize("../../outside","sha256:"+"f".repeat(64),"image/png",4))));code(400,()->tx(()->service.authorize(new Authorize("ASSET","sha256:"+"f".repeat(64),"image/png",9*1024*1024))));code(400,()->service.page(null,null,1,101));assertEquals(0,service.page(null,null,1,20).total());}
 @Test void rollbackRemovesOnlyNewOwnedFilesAndDamageRejectsReload()throws Exception{byte[] bytes=gltf();Grant g=grant("ASSET-ROLLBACK","model/gltf+json",bytes);transaction.execute(s->{service.upload(g.grantId(),"model/gltf+json",new ByteArrayInputStream(bytes));s.setRollbackOnly();return null;});assertEquals(0,service.page(null,null,1,20).total());try(var paths=Files.walk(root.resolve("assets"))){assertEquals(0,paths.filter(Files::isRegularFile).count());}Evidence e=upload("ASSET-REAL","model/gltf+json",bytes);String blob=jdbc.queryForObject("SELECT blob_id FROM dm_visual_asset WHERE asset_id='ASSET-REAL'",String.class);Files.write(root.resolve("assets/tenant-1").resolve(blob).resolve("content.bin"),new byte[bytes.length]);code(409,()->service.get(e.asset().assetId()));}
 @Test void modelReferencesRequireExactExistingAsset(){Evidence e=upload("ASSET-REF","model/gltf+json",gltf());var ref=mapper.createObjectNode().put("kind","gltf").put("assetId",e.asset().assetId()).put("contentHash",e.asset().contentHash());service.validateReferences(ref);ref.put("assetId","MISSING");code(404,()->service.validateReferences(ref));}
}
