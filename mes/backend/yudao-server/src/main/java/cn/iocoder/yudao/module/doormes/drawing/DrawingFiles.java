package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import static cn.iocoder.yudao.module.doormes.drawing.DrawingModels.*;

@Component @Profile("doormes")
public class DrawingFiles {
    private final Path root;private final ObjectMapper mapper;
    public record Stored(String relativePath,String sha256) {}
    public DrawingFiles(@Value("${doormes.drawings.root}") String root,ObjectMapper mapper) {this.root=Path.of(root).toAbsolutePath().normalize();this.mapper=mapper;}
    public static void safe(Path root,Path target) throws IOException {
        if(!target.startsWith(root) || target.equals(root)) throw new ServiceException(400,"图纸文件路径无效");
        for(Path part=target;part!=null;part=part.getParent()) {
            if(Files.exists(part,LinkOption.NOFOLLOW_LINKS) && (Files.isSymbolicLink(part) || !part.equals(part.toRealPath())))
                throw new ServiceException(400,"图纸目录不允许链接或重定向");
        }
    }
    public static void uuid(String value) {
        try {if(!UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException();}
        catch(RuntimeException e) {throw new ServiceException(400,"图纸或明细内部编号无效");}
    }
    private Path path(long tenant,String id,int revision) throws IOException {
        uuid(id);if(tenant<1 || revision<1) throw new ServiceException(400,"图纸版本无效");
        Path target=root.resolve("tenant-"+tenant).resolve(id).resolve("r"+revision+".json");safe(root,target);return target;
    }
    public Stored write(Snapshot doc) {
        Path temporary=null;
        try {
            Path target=path(doc.tenantId(),doc.id(),doc.revision());Files.createDirectories(target.getParent());safe(root,target);
            if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)) throw new ServiceException(409,"图纸版本文件已存在，不能覆盖");
            byte[] bytes=mapper.writeValueAsBytes(doc);temporary=Files.createTempFile(target.getParent(),"revision-",".tmp");
            Files.write(temporary,bytes);Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);
            return new Stored(root.relativize(target).toString().replace('\\','/'),digest(bytes));
        } catch(IOException e) {throw new ServiceException(500,"图纸 JSON 保存失败，数据库未提交");}
        finally {if(temporary!=null) try{Files.deleteIfExists(temporary);}catch(IOException ignored){}}
    }
    public Snapshot read(long tenant,String id,int revision,String hash) {
        try {
            byte[] bytes=Files.readAllBytes(path(tenant,id,revision));
            if(!digest(bytes).equals(hash)) throw new ServiceException(409,"图纸文件校验失败，禁止使用损坏版本");
            Snapshot doc=mapper.readValue(bytes,Snapshot.class);
            if(!SCHEMA.equals(doc.schemaVersion()) || doc.tenantId()!=tenant || !id.equals(doc.id()) || doc.revision()!=revision || !id.equals(doc.document().path("designId").asText()))
                throw new ServiceException(409,"图纸文件身份校验失败");
            return doc;
        } catch(IOException e) {throw new ServiceException(500,"图纸 JSON 加载失败，请检查本地存储");}
    }
    public void rollback(Snapshot doc) {try {Files.deleteIfExists(path(doc.tenantId(),doc.id(),doc.revision()));}catch(IOException ignored){}}
    private static String digest(byte[] bytes) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
