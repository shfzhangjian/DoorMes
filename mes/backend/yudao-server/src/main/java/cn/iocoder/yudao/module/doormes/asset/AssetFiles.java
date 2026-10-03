package cn.iocoder.yudao.module.doormes.asset;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.doormes.drawing.DrawingFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import static cn.iocoder.yudao.module.doormes.asset.AssetModels.*;

/** Immutable binary and JSON metadata. Client identifiers never become file paths. */
@Component @Profile("doormes")
public class AssetFiles {
    private final Path root;
    private final ObjectMapper mapper;
    public AssetFiles(@Value("${doormes.assets.root}") String root,ObjectMapper mapper) {
        this.root=Path.of(root).toAbsolutePath().normalize();this.mapper=mapper;
    }
    private Path path(long tenant,String blob,String filename)throws IOException {
        DrawingFiles.uuid(blob);if(tenant<1)throw new ServiceException(400,"资源租户无效");
        Path file=root.resolve("tenant-"+tenant).resolve(blob).resolve(filename);DrawingFiles.safe(root,file);return file;
    }
    public String write(Metadata metadata,byte[] bytes) {
        Path binary=null,json=null;
        try {
            binary=path(metadata.tenantId(),metadata.blobId(),"content.bin");json=path(metadata.tenantId(),metadata.blobId(),"descriptor.json");
            Files.createDirectories(binary.getParent());DrawingFiles.safe(root,binary);
            if(Files.exists(binary,LinkOption.NOFOLLOW_LINKS)||Files.exists(json,LinkOption.NOFOLLOW_LINKS))throw new ServiceException(409,"资源内部路径已存在，不能覆盖");
            Files.write(binary,bytes,StandardOpenOption.CREATE_NEW);
            Files.write(json,mapper.writeValueAsBytes(metadata),StandardOpenOption.CREATE_NEW);
            return digest(Files.readAllBytes(json));
        }catch(IOException e){rollback(metadata);throw new ServiceException(500,"资源保存失败，原文件未改变");}
    }
    public Metadata metadata(long tenant,String blob,String metadataHash) {
        try {
            byte[] bytes=Files.readAllBytes(path(tenant,blob,"descriptor.json"));
            if(!digest(bytes).equals(metadataHash))throw new ServiceException(409,"资源元数据已损坏");
            Metadata result=mapper.readValue(bytes,Metadata.class);
            if(!"doormes-visual-asset.v1".equals(result.schemaVersion())||result.tenantId()!=tenant||!blob.equals(result.blobId()))throw new ServiceException(409,"资源元数据身份不符");
            return result;
        }catch(IOException e){throw new ServiceException(500,"本地资源元数据无法读取");}
    }
    public byte[] read(Metadata metadata) {
        try {
            Path binary=path(metadata.tenantId(),metadata.blobId(),"content.bin");Descriptor descriptor=metadata.evidence().asset();
            if(Files.size(binary)!=descriptor.byteLength())throw new ServiceException(409,"资源大小与保存版本不符");
            byte[] bytes=Files.readAllBytes(binary);
            if(!("sha256:"+digest(bytes)).equals(descriptor.contentHash()))throw new ServiceException(409,"资源文件已损坏，不能替换历史版本");
            return bytes;
        }catch(IOException e){throw new ServiceException(500,"本地资源文件无法读取");}
    }
    public void rollback(Metadata metadata) {
        try {Files.deleteIfExists(path(metadata.tenantId(),metadata.blobId(),"descriptor.json"));Files.deleteIfExists(path(metadata.tenantId(),metadata.blobId(),"content.bin"));Files.deleteIfExists(path(metadata.tenantId(),metadata.blobId(),"content.bin").getParent());}catch(IOException ignored){}
    }
    public static String digest(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
