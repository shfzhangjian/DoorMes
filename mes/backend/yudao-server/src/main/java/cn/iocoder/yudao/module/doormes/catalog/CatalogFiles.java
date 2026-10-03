package cn.iocoder.yudao.module.doormes.catalog;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.doormes.drawing.DrawingFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import static cn.iocoder.yudao.module.doormes.catalog.CatalogModels.*;

@Component @Profile("doormes")
public class CatalogFiles {
    private final Path root;private final ObjectMapper mapper;
    public record Stored(String relativePath,String sha256) {}
    public CatalogFiles(@Value("${doormes.catalog.root}") String root,ObjectMapper mapper){this.root=Path.of(root).toAbsolutePath().normalize();this.mapper=mapper;}
    private Path path(long tenant,String id,int revision)throws IOException {
        DrawingFiles.uuid(id);if(tenant<1||revision<1)throw new ServiceException(400,"目录版本无效");
        Path target=root.resolve("tenant-"+tenant).resolve(id).resolve("r"+revision+".json");DrawingFiles.safe(root,target);return target;
    }
    public Stored write(Snapshot doc) {
        Path temporary=null;
        try {
            Path target=path(doc.tenantId(),doc.id(),doc.revision());Files.createDirectories(target.getParent());DrawingFiles.safe(root,target);
            if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))throw new ServiceException(409,"目录版本已存在，不能覆盖");
            byte[] bytes=mapper.writeValueAsBytes(doc);temporary=Files.createTempFile(target.getParent(),"revision-",".tmp");Files.write(temporary,bytes);
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);
            return new Stored(root.relativize(target).toString().replace('\\','/'),digest(bytes));
        }catch(IOException e){throw new ServiceException(500,"目录 JSON 保存失败，数据库未提交");}
        finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){}}
    }
    public Snapshot read(long tenant,String id,int revision,String hash) {
        try {
            byte[] bytes=Files.readAllBytes(path(tenant,id,revision));if(!digest(bytes).equals(hash))throw new ServiceException(409,"目录版本文件校验失败");
            Snapshot doc=mapper.readValue(bytes,Snapshot.class);
            if(!SCHEMA.equals(doc.schemaVersion())||doc.tenantId()!=tenant||!id.equals(doc.id())||doc.revision()!=revision)throw new ServiceException(409,"目录文件身份无效");
            return doc;
        }catch(IOException e){throw new ServiceException(500,"目录版本加载失败，请检查本地文件");}
    }
    public void rollback(Snapshot doc){try{Files.deleteIfExists(path(doc.tenantId(),doc.id(),doc.revision()));}catch(IOException ignored){}}
    private static String digest(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
