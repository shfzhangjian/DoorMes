package cn.iocoder.yudao.module.doormes.catalog;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.drawing.DrawingFiles;
import cn.iocoder.yudao.module.doormes.drawing.DrawingValidator;
import cn.iocoder.yudao.module.doormes.asset.AssetService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.*;
import static cn.iocoder.yudao.module.doormes.catalog.CatalogModels.*;

@Service @Profile("doormes")
public class CatalogService {
    private final JdbcTemplate jdbc;private final CatalogFiles files;private final DrawingValidator validator;private final AssetService assets;
    public CatalogService(JdbcTemplate jdbc,CatalogFiles files,DrawingValidator validator,AssetService assets){this.jdbc=jdbc;this.files=files;this.validator=validator;this.assets=assets;}
    private long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private long actor(){Long id=SecurityFrameworkUtils.getLoginUserId();if(id==null)throw new ServiceException(401,"请先登录");return id;}
    public record Page(List<Snapshot> list,long total) {}
    public Page page(String category,String keyword,boolean published,int pageNo,int pageSize) {
        if(pageNo<1||pageNo>100000||pageSize<1||pageSize>100||keyword!=null&&keyword.length()>160)throw new ServiceException(400,"目录查询参数无效");
        if(category!=null&&!category.isEmpty()&&!Set.of("finish","glass").contains(category))throw new ServiceException(400,"目录分类无效");
        String where=" WHERE tenant_id=?"+(published?" AND published_revision IS NOT NULL":"");List<Object> args=new ArrayList<>();args.add(tenant());
        if(category!=null&&!category.isEmpty()){where+=" AND category=?";args.add(category);}
        if(keyword!=null&&!keyword.isBlank()){where+=" AND (code LIKE ? OR name LIKE ?)";args.add("%"+keyword.trim()+"%");args.add("%"+keyword.trim()+"%");}
        long count=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM dm_material_catalog"+where,Long.class,args.toArray()));
        args.add(pageSize);args.add((pageNo-1)*pageSize);
        var rows=jdbc.queryForList("SELECT id,"+(published?"published_revision":"revision")+" AS chosen FROM dm_material_catalog"+where+" ORDER BY code LIMIT ? OFFSET ?",args.toArray());
        return new Page(rows.stream().map(row->get((String)row.get("id"),((Number)row.get("chosen")).intValue())).toList(),count);
    }
    public Snapshot get(String id,Integer revision){return load(id,revision,false);}
    private Snapshot load(String id,Integer revision,boolean lock) {
        DrawingFiles.uuid(id);var heads=jdbc.queryForList("SELECT revision FROM dm_material_catalog WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),tenant(),id);
        if(heads.isEmpty())throw new ServiceException(404,"目录不存在或不属于本工厂");
        int chosen=revision==null?((Number)heads.get(0).get("revision")).intValue():revision;
        var hashes=jdbc.query("SELECT sha256 FROM dm_material_catalog_version WHERE tenant_id=? AND catalog_id=? AND revision=?",(rs,n)->rs.getString(1),tenant(),id,chosen);
        if(hashes.isEmpty())throw new ServiceException(404,"目录版本不存在");return files.read(tenant(),id,chosen,hashes.get(0));
    }
    public List<Version> versions(String id){get(id,null);return jdbc.query("SELECT * FROM dm_material_catalog_version WHERE tenant_id=? AND catalog_id=? ORDER BY revision DESC",(rs,n)->new Version(rs.getInt("revision"),rs.getString("status"),rs.getString("change_note"),rs.getLong("changed_by"),rs.getString("updated_at")),tenant(),id);}
    @Transactional public Snapshot create(Mutation input) {
        if(input.expectedRevision()!=0)throw new ServiceException(409,"新建目录版本应为0");
        Snapshot doc=make(UUID.randomUUID().toString(),1,"DRAFT",input.item(),input.changeNote());persist(doc,true);return doc;
    }
    @Transactional public Snapshot revise(String id,Mutation input) {
        Snapshot old=load(id,null,true);expected(old,input.expectedRevision());
        if(!old.item().code().equals(input.item().code())||!old.item().category().equals(input.item().category()))throw new ServiceException(400,"目录型号和分类不能修改；请新建另一型号");
        Snapshot doc=make(id,old.revision()+1,"DRAFT",input.item(),input.changeNote());persist(doc,false);return doc;
    }
    @Transactional public Snapshot publish(String id,Publish input) {
        Snapshot old=load(id,null,true);expected(old,input.expectedRevision());
        if(!old.status().equals("DRAFT"))throw new ServiceException(409,"该版本已发布，请先修订草稿");
        Snapshot doc=make(id,old.revision()+1,"PUBLISHED",old.item(),input.note());persist(doc,false);return doc;
    }
    private void expected(Snapshot doc,int revision){if(doc.revision()!=revision)throw new ServiceException(409,"目录已更新，请重新加载，不能覆盖");}
    private Snapshot make(String id,int revision,String status,Input item,String note) {
        JsonNode data=validator.catalog(id,revision,item);
        assets.validateReferences(data);
        return new Snapshot(SCHEMA,id,tenant(),revision,status,actor(),Instant.now().toString(),note.trim(),item,data);
    }
    private void persist(Snapshot doc,boolean create) {
        var stored=files.write(doc);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)files.rollback(doc);}});
        try {
            if(create)jdbc.update("INSERT INTO dm_material_catalog(id,tenant_id,category,code,name,revision,status,updated_at) VALUES(?,?,?,?,?,?,?,?)",doc.id(),tenant(),doc.item().category(),doc.item().code(),doc.item().name(),doc.revision(),doc.status(),doc.updatedAt());
            else jdbc.update("UPDATE dm_material_catalog SET name=?,revision=?,status=?,updated_at=?,published_revision=CASE WHEN ?='PUBLISHED' THEN ? ELSE published_revision END WHERE tenant_id=? AND id=?",doc.item().name(),doc.revision(),doc.status(),doc.updatedAt(),doc.status(),doc.revision(),tenant(),doc.id());
            jdbc.update("INSERT INTO dm_material_catalog_version(tenant_id,catalog_id,revision,status,json_path,sha256,change_note,changed_by,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",tenant(),doc.id(),doc.revision(),doc.status(),stored.relativePath(),stored.sha256(),doc.changeNote(),doc.changedBy(),doc.updatedAt());
        }catch(DuplicateKeyException e){throw new ServiceException(409,"本工厂已有该型号，不能重复新建");}
    }
    /** Validate every MES reference against its EXACT immutable published version, not latest. */
    public void validateReferences(JsonNode document){references(document,new HashMap<>());
        for(JsonNode window:document.path("windows")) {
            JsonNode selection=window.path("defaultGlassSelection");
            if(selection.path("catalogItemId").asText().startsWith("MES-CATALOG:")&&Double.compare(window.path("sectionDimensions").path("glassDepthMm").asDouble(-1),selection.path("thicknessMm").asDouble(-2))!=0)
                throw new ServiceException(400,"玻璃厚度与所选目录不一致，请重新应用玻璃选型");
        }
    }
    private void references(JsonNode node,Map<String,Snapshot> cached) {
        if(node.isObject()) {
            boolean glass=node.path("catalogItemId").asText().startsWith("MES-CATALOG:");
            String identity=glass?node.path("catalogItemId").asText():node.path("appearanceId").asText();
            if(identity.startsWith("MES-CATALOG:")) {
                String id=identity.substring("MES-CATALOG:".length()),version=node.path(glass?"catalogVersion":"appearanceVersion").asText();
                if(!version.matches("[1-9][0-9]{0,8}"))throw new ServiceException(400,"材质目录版本无效");
                String key=id+"@"+version;Snapshot selected=cached.computeIfAbsent(key,ignored->get(id,Integer.parseInt(version)));
                if(!selected.status().equals("PUBLISHED"))throw new ServiceException(400,"材质目录必须使用已发布选型版本");
                JsonNode expected=selected.data().get(glass?"glassSelection":"appearance");
                if(expected==null||!sameJson(node,expected))throw new ServiceException(400,"图纸材质与冻结目录版本不一致，请重新选型；如需改渲染参数请修订目录");
            }
        }
        if(node.isContainerNode())node.elements().forEachRemaining(child->references(child,cached));
    }
    private boolean sameJson(JsonNode actual,JsonNode expected) {
        if(actual.isNumber()&&expected.isNumber())return actual.decimalValue().compareTo(expected.decimalValue())==0;
        if(actual.isObject()&&expected.isObject()){if(actual.size()!=expected.size())return false;var names=actual.fieldNames();while(names.hasNext()){String key=names.next();if(!expected.has(key)||!sameJson(actual.get(key),expected.get(key)))return false;}return true;}
        if(actual.isArray()&&expected.isArray()){if(actual.size()!=expected.size())return false;for(int i=0;i<actual.size();i++)if(!sameJson(actual.get(i),expected.get(i)))return false;return true;}
        return actual.equals(expected);
    }
}
