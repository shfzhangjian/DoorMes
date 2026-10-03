package cn.iocoder.yudao.module.doormes.asset;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.drawing.DrawingFiles;
import cn.iocoder.yudao.module.doormes.drawing.DrawingValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.imageio.ImageIO;
import java.io.*;
import java.time.Instant;
import java.util.*;
import static cn.iocoder.yudao.module.doormes.asset.AssetModels.*;

@Service @Profile("doormes")
public class AssetService {
    private final JdbcTemplate jdbc;private final AssetFiles files;private final DrawingValidator validator;private final ObjectMapper mapper;
    public AssetService(JdbcTemplate jdbc,AssetFiles files,DrawingValidator validator,ObjectMapper mapper){this.jdbc=jdbc;this.files=files;this.validator=validator;this.mapper=mapper;}
    private long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private long actor(){Long id=SecurityFrameworkUtils.getLoginUserId();if(id==null)throw new ServiceException(401,"请先登录");return id;}
    private void identifier(String id){if(id==null||!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}"))throw new ServiceException(400,"资源编号只能使用字母数字点下划线短横线，最长100位");}
    private void hash(String value){if(value==null||!value.matches("sha256:[a-f0-9]{64}"))throw new ServiceException(400,"资源哈希无效");}
    @Transactional public Grant authorize(Authorize input) {
        identifier(input.assetId());hash(input.expectedContentHash());
        boolean image=Set.of("image/png","image/jpeg").contains(input.mediaType());
        if(!image&&!Set.of("model/gltf-binary","model/gltf+json").contains(input.mediaType())||input.byteLength()<2||input.byteLength()>(image?8*1024*1024:25*1024*1024))throw new ServiceException(400,"模型限25MiB，PNG/JPEG纹理限8MiB");
        long now=Instant.now().toEpochMilli();
        // Expired grants are capabilities only, not assets. Bound accumulation without deleting any files.
        jdbc.update("DELETE FROM dm_visual_asset_upload WHERE tenant_id=? AND expires_at<?",tenant(),now);
        int pending=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM dm_visual_asset_upload WHERE tenant_id=? AND actor_id=? AND expires_at>=?",Integer.class,tenant(),actor(),now));
        if(pending>=20)throw new ServiceException(429,"待上传资源过多，请等待授权过期后重试");
        Metadata existing=find(input.assetId());
        if(existing!=null){Descriptor descriptor=existing.evidence().asset();if(!descriptor.contentHash().equals(input.expectedContentHash())||!descriptor.mediaType().equals(input.mediaType())||descriptor.byteLength()!=input.byteLength())throw new ServiceException(409,"已有同编号不同内容，请使用新资源编号，禁止覆盖历史资源");}
        String grant=UUID.randomUUID().toString();long expires=now+5*60*1000;
        jdbc.update("INSERT INTO dm_visual_asset_upload(grant_id,tenant_id,actor_id,asset_id,content_hash,media_type,byte_length,expires_at) VALUES(?,?,?,?,?,?,?,?)",grant,tenant(),actor(),input.assetId(),input.expectedContentHash(),input.mediaType(),input.byteLength(),expires);
        return new Grant(grant,Instant.ofEpochMilli(expires).toString());
    }
    @Transactional public Evidence upload(String grantId,String mediaType,InputStream stream) {
        DrawingFiles.uuid(grantId);
        var grants=jdbc.queryForList("SELECT * FROM dm_visual_asset_upload WHERE tenant_id=? AND grant_id=? FOR UPDATE",tenant(),grantId);
        if(grants.isEmpty())throw new ServiceException(404,"上传授权不存在或不属于本工厂");var grant=grants.get(0);
        if(((Number)grant.get("actor_id")).longValue()!=actor())throw new ServiceException(403,"上传授权只能由本人使用");
        if(((Number)grant.get("expires_at")).longValue()<Instant.now().toEpochMilli())throw new ServiceException(409,"上传授权已过期，请重新选择文件");
        String selectedMedia=(String)grant.get("media_type"),assetId=(String)grant.get("asset_id"),expectedHash=(String)grant.get("content_hash");int size=((Number)grant.get("byte_length")).intValue();
        if(!selectedMedia.equals(mediaType))throw new ServiceException(400,"上传类型与授权不符");
        byte[] bytes;
        try{bytes=stream.readNBytes(size+1);}catch(IOException e){throw new ServiceException(400,"上传文件读取失败");}
        if(bytes.length!=size||!("sha256:"+AssetFiles.digest(bytes)).equals(expectedHash))throw new ServiceException(400,"上传长度或SHA-256与授权不符");
        Metadata existing=find(assetId);
        if(existing!=null){Descriptor descriptor=existing.evidence().asset();if(!descriptor.contentHash().equals(expectedHash)||!descriptor.mediaType().equals(selectedMedia))throw new ServiceException(409,"资源编号不能绑定不同内容");files.read(existing);jdbc.update("DELETE FROM dm_visual_asset_upload WHERE tenant_id=? AND grant_id=?",tenant(),grantId);return existing.evidence();}
        JsonNode inspection;
        boolean image=selectedMedia.startsWith("image/");
        if(image)inspection=inspectImage(bytes,selectedMedia,expectedHash);
        else {
            boolean binary=bytes.length>=4&&bytes[0]=='g'&&bytes[1]=='l'&&bytes[2]=='T'&&bytes[3]=='F';
            if(binary!=selectedMedia.equals("model/gltf-binary"))throw new ServiceException(400,"GLB/glTF文件类型与实际内容不符");
            inspection=validator.inspectAsset(bytes,expectedHash);
        }
        String now=Instant.now().toString(),blob=UUID.randomUUID().toString();
        Evidence evidence=new Evidence(new Descriptor(assetId,image?"texture-bundle":"component-model",selectedMedia,expectedHash,size,now),inspection,now);
        Metadata metadata=new Metadata("doormes-visual-asset.v1",blob,tenant(),actor(),evidence);
        String metadataHash=files.write(metadata,bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)files.rollback(metadata);}});
        try{jdbc.update("INSERT INTO dm_visual_asset(tenant_id,asset_id,blob_id,kind,media_type,content_hash,byte_length,metadata_hash,uploaded_by,stored_at) VALUES(?,?,?,?,?,?,?,?,?,?)",tenant(),assetId,blob,evidence.asset().kind(),selectedMedia,expectedHash,size,metadataHash,actor(),now);}
        catch(DuplicateKeyException e){throw new ServiceException(409,"并发上传编号冲突，请重新加载，不覆盖资源");}
        jdbc.update("DELETE FROM dm_visual_asset_upload WHERE tenant_id=? AND grant_id=?",tenant(),grantId);return evidence;
    }
    private JsonNode inspectImage(byte[] bytes,String media,String hash) {
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new ServiceException(400,"纹理必须为有效PNG/JPEG");var reader=readers.next();
            try{reader.setInput(stream);String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!format.equals(media.equals("image/png")?"png":"jpeg"))throw new ServiceException(400,"纹理类型与实际内容不符");
                int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||width>4096||height>4096||(long)width*height>16_777_216)throw new ServiceException(400,"纹理最多4096×4096像素");
                if(reader.read(0)==null)throw new ServiceException(400,"纹理不能完整解码");
                return mapper.valueToTree(Map.of("contentHash",hash,"byteLength",bytes.length,"width",width,"height",height,"map","color"));
            }finally{reader.dispose();}
        }catch(IOException|IllegalArgumentException e){throw new ServiceException(400,"纹理解码失败，不保存无效文件");}
    }
    private Metadata find(String id) {
        identifier(id);var rows=jdbc.queryForList("SELECT * FROM dm_visual_asset WHERE tenant_id=? AND asset_id=?",tenant(),id);
        if(rows.isEmpty())return null;var row=rows.get(0);Metadata metadata=files.metadata(tenant(),(String)row.get("blob_id"),(String)row.get("metadata_hash"));Descriptor descriptor=metadata.evidence().asset();
        if(!descriptor.assetId().equals(id)||!descriptor.contentHash().equals(row.get("content_hash"))||descriptor.byteLength()!=((Number)row.get("byte_length")).intValue()||!descriptor.mediaType().equals(row.get("media_type"))||!descriptor.kind().equals(row.get("kind")))throw new ServiceException(409,"资源文件与索引不一致");return metadata;
    }
    public Descriptor get(String id){Metadata value=find(id);if(value==null)throw new ServiceException(404,"资源不存在或不属于本工厂");files.read(value);return value.evidence().asset();}
    public byte[] read(String id,String expectedHash){hash(expectedHash);Metadata value=find(id);if(value==null)throw new ServiceException(404,"资源不存在或不属于本工厂");if(!value.evidence().asset().contentHash().equals(expectedHash))throw new ServiceException(409,"资源哈希与指定版本不符");return files.read(value);}
    public record Page(List<Descriptor> list,long total) {}
    public Page page(String kind,String keyword,int pageNo,int pageSize){
        if(pageNo<1||pageNo>100000||pageSize<1||pageSize>100||keyword!=null&&keyword.length()>100||kind!=null&&!kind.isBlank()&&!Set.of("component-model","texture-bundle").contains(kind))throw new ServiceException(400,"资源查询参数无效");
        String where=" WHERE tenant_id=?";List<Object> args=new ArrayList<>();args.add(tenant());if(kind!=null&&!kind.isBlank()){where+=" AND kind=?";args.add(kind);}if(keyword!=null&&!keyword.isBlank()){where+=" AND asset_id LIKE ?";args.add("%"+keyword.trim()+"%");}
        long total=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM dm_visual_asset"+where,Long.class,args.toArray()));args.add(pageSize);args.add((pageNo-1)*pageSize);
        return new Page(jdbc.query("SELECT * FROM dm_visual_asset"+where+" ORDER BY stored_at DESC LIMIT ? OFFSET ?",(rs,n)->new Descriptor(rs.getString("asset_id"),rs.getString("kind"),rs.getString("media_type"),rs.getString("content_hash"),rs.getInt("byte_length"),rs.getString("stored_at")),args.toArray()),total);
    }
    public void validateReferences(JsonNode node){validateReferences(node,new HashSet<>());}
    private void validateReferences(JsonNode node,Set<String> seen){
        if(node.isObject()){
            if(node.path("kind").asText().equals("gltf")){
                reference(node.path("assetId").asText(),node.path("contentHash").asText(),"component-model",seen);
                for(JsonNode lod:node.path("lodAssets"))reference(lod.path("assetId").asText(),lod.path("contentHash").asText(),"component-model",seen);
            }
            if(node.path("textureSetId").asText().startsWith("MES-TEXTURE-"))reference(node.path("textureSetId").asText(),node.path("textureContentHash").asText(),"texture-bundle",seen);
        }
        if(node.isContainerNode())node.elements().forEachRemaining(child->validateReferences(child,seen));
    }
    private void reference(String id,String hash,String kind,Set<String> seen){
        if(!seen.add(id+"|"+hash+"|"+kind))return;Descriptor descriptor=get(id);
        if(!descriptor.contentHash().equals(hash)||!descriptor.kind().equals(kind))throw new ServiceException(400,"资源引用哈希或分类不符，请重新选型");
    }
}
