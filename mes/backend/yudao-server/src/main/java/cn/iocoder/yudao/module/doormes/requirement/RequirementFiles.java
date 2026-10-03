package cn.iocoder.yudao.module.doormes.requirement;

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
import static cn.iocoder.yudao.module.doormes.requirement.RequirementModels.*;

/** Immutable snapshots. Only server-generated tenant/UUID/revision paths are accepted. */
@Component
@Profile("doormes")
public class RequirementFiles {
    private final Path root;
    private final ObjectMapper mapper;
    public record Stored(String relativePath, String sha256) {}
    public RequirementFiles(@Value("${doormes.requirements.root}") String root, ObjectMapper mapper) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.mapper = mapper;
    }
    private Path path(long tenant, String id, int revision) throws IOException {
        if (tenant < 1 || revision < 1 || !UUID.fromString(id).toString().equals(id)) {
            throw new ServiceException(400, "需求编号或版本无效");
        }
        Path target = root.resolve("tenant-"+tenant).resolve(id).resolve("r"+revision+".json");
        safe(target);
        return target;
    }
    private void safe(Path target) throws IOException {
        if (!target.startsWith(root) || target.equals(root)) throw new ServiceException(400,"文件路径无效");
        for (Path part=target; part!=null; part=part.getParent()) {
            if (Files.exists(part, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(part) || !part.equals(part.toRealPath()))) {
                throw new ServiceException(400,"需求文件目录不允许链接或重定向");
            }
        }
    }
    public Stored write(Document document) {
        Path temporary=null;
        try {
            Path target=path(document.tenantId(),document.id(),document.revision());
            Files.createDirectories(target.getParent());
            safe(target);
            if (Files.exists(target,LinkOption.NOFOLLOW_LINKS)) throw new ServiceException(409,"版本文件已存在，不能覆盖");
            byte[] bytes=mapper.writeValueAsBytes(document);
            temporary=Files.createTempFile(target.getParent(),"revision-",".tmp");
            Files.write(temporary,bytes);
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);
            return new Stored(root.relativize(target).toString().replace('\\','/'),digest(bytes));
        } catch (IOException e) {
            throw new ServiceException(500,"需求 JSON 保存失败，数据库未提交，请重试");
        } finally {
            if (temporary!=null) try { Files.deleteIfExists(temporary); } catch(IOException ignored) {}
        }
    }
    public Document read(long tenant,String id,int revision,String expectedHash) {
        try {
            byte[] bytes=Files.readAllBytes(path(tenant,id,revision));
            if (!digest(bytes).equals(expectedHash)) throw new ServiceException(409,"需求文件校验失败，禁止使用损坏版本");
            Document doc=mapper.readValue(bytes,Document.class);
            if (!DOCUMENT_SCHEMA.equals(doc.schemaVersion()) || doc.tenantId()!=tenant || !doc.id().equals(id) || doc.revision()!=revision) {
                throw new ServiceException(409,"需求文件身份校验失败");
            }
            return doc;
        } catch (IOException e) { throw new ServiceException(500,"需求 JSON 加载失败，请检查本地存储"); }
    }
    // Called only for a newly created snapshot when its DB transaction rolls back.
    public void rollback(Document document) {
        try { Files.deleteIfExists(path(document.tenantId(),document.id(),document.revision())); }
        catch(IOException ignored) { /* Crash/orphan recovery never exposes files absent from committed SQL. */ }
    }
    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
