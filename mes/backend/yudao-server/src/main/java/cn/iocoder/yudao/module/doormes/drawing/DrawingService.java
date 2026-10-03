package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.requirement.RequirementService;
import cn.iocoder.yudao.module.doormes.requirement.RequirementModels;
import cn.iocoder.yudao.module.doormes.catalog.CatalogService;
import cn.iocoder.yudao.module.doormes.asset.AssetService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.*;
import static cn.iocoder.yudao.module.doormes.drawing.DrawingModels.*;

@Service @Profile("doormes")
public class DrawingService {
    private final JdbcTemplate jdbc;private final RequirementService requirements;private final DrawingValidator validator;
    private final DrawingFiles files;private final PermissionService permissions;
    private final CatalogService catalog;
    private final AssetService assets;
    public DrawingService(JdbcTemplate jdbc,RequirementService requirements,DrawingValidator validator,DrawingFiles files,PermissionService permissions,CatalogService catalog,AssetService assets) {
        this.jdbc=jdbc;this.requirements=requirements;this.validator=validator;this.files=files;this.permissions=permissions;
        this.catalog=catalog;
        this.assets=assets;
    }
    private long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private long actor(){Long id=SecurityFrameworkUtils.getLoginUserId();if(id==null)throw new ServiceException(401,"请先登录");return id;}
    public Page page(String keyword,String source,boolean mine,int pageNo,int pageSize) {
        if(pageNo<1 || pageNo>100000 || pageSize<1 || pageSize>100)throw new ServiceException(400,"分页参数超出范围");
        if(keyword!=null && keyword.length()>200)throw new ServiceException(400,"搜索文字过长");
        if(source!=null && !source.isBlank() && !Set.of("INDEPENDENT","REQUIREMENT").contains(source))throw new ServiceException(400,"图纸来源无效");
        String from=" FROM dm_drawing d LEFT JOIN dm_design_requirement r ON r.tenant_id=d.tenant_id AND r.id=d.requirement_id WHERE d.tenant_id=?";
        List<Object> args=new ArrayList<>();args.add(tenant());
        if(keyword!=null && !keyword.isBlank()) {
            String escaped=keyword.trim().replace("!","!!").replace("%","!%").replace("_","!_");
            from+=" AND (LOWER(d.drawing_number) LIKE LOWER(?) ESCAPE '!' OR LOWER(d.name) LIKE LOWER(?) ESCAPE '!' OR LOWER(d.note) LIKE LOWER(?) ESCAPE '!')";
            args.add("%"+escaped+"%");args.add("%"+escaped+"%");args.add("%"+escaped+"%");
        }
        if(source!=null && !source.isBlank()){from+=" AND d.source_type=?";args.add(source);}
        if(mine){from+=" AND ((d.source_type='INDEPENDENT' AND d.created_by=?) OR (d.source_type='REQUIREMENT' AND r.assigned_to=?))";args.add(actor());args.add(actor());}
        long total=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray()));
        boolean write=permissions.hasAnyPermissions(actor(),"doormes:design:drawing-save");boolean admin=permissions.hasAnyRoles(actor(),"super_admin");long user=actor();
        args.add(pageSize);args.add((pageNo-1)*pageSize);
        var list=jdbc.query("SELECT d.*,r.assigned_to,r.status AS demand_status"+from+" ORDER BY d.updated_at DESC,d.id ASC LIMIT ? OFFSET ?",(rs,n)->{
            String kind=rs.getString("source_type");Object assigned=rs.getObject("assigned_to");
            boolean owned="INDEPENDENT".equals(kind)?rs.getLong("created_by")==user:
                "IN_DESIGN".equals(rs.getString("demand_status")) && assigned instanceof Number && ((Number)assigned).longValue()==user;
            return new Summary(rs.getString("id"),rs.getInt("revision"),rs.getString("status"),rs.getString("drawing_number"),rs.getString("name"),rs.getString("note"),kind,
                rs.getString("requirement_id"),rs.getInt("requirement_revision"),rs.getString("line_id"),rs.getLong("created_by"),rs.getString("updated_at"),write && "DRAFT".equals(rs.getString("status")) &&
                    ("INDEPENDENT".equals(kind) || "IN_DESIGN".equals(rs.getString("demand_status"))) && (admin||owned));
        },args.toArray());
        return new Page(list,total);
    }
    public Snapshot find(String requirementId,String lineId) {
        DrawingFiles.uuid(requirementId);DrawingFiles.uuid(lineId);
        var requirement=requirements.get(requirementId,null);
        line(requirement,lineId);
        List<String> ids=jdbc.query("SELECT id FROM dm_drawing WHERE tenant_id=? AND requirement_id=? AND line_id=?",(rs,n)->rs.getString(1),tenant(),requirementId,lineId);
        return ids.isEmpty()?null:get(ids.get(0),null);
    }
    public Snapshot get(String id,Integer revision){
        Snapshot doc=load(id,revision,false);
        int head=Objects.requireNonNull(jdbc.queryForObject("SELECT revision FROM dm_drawing WHERE tenant_id=? AND id=?",Integer.class,tenant(),id));
        return decorate(doc,doc.revision()==head && canEdit(doc));
    }
    private Snapshot load(String id,Integer revision,boolean lock) {
        DrawingFiles.uuid(id);
        var heads=jdbc.queryForList("SELECT revision FROM dm_drawing WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),tenant(),id);
        if(heads.isEmpty())throw new ServiceException(404,"图纸不存在或不属于当前工厂");
        int chosen=revision==null?((Number)heads.get(0).get("revision")).intValue():revision;
        var hashes=jdbc.query("SELECT sha256 FROM dm_drawing_version WHERE tenant_id=? AND drawing_id=? AND revision=?",(rs,n)->rs.getString(1),tenant(),id,chosen);
        if(hashes.isEmpty())throw new ServiceException(404,"图纸版本不存在");
        return files.read(tenant(),id,chosen,hashes.get(0));
    }
    public List<Version> versions(String id){get(id,null);return jdbc.query("SELECT * FROM dm_drawing_version WHERE tenant_id=? AND drawing_id=? ORDER BY revision DESC",(rs,n)->new Version(rs.getInt("revision"),rs.getString("change_note"),rs.getLong("changed_by"),rs.getString("updated_at")),tenant(),id);}
    @Transactional public Snapshot open(Open input) {
        DrawingFiles.uuid(input.requirementId());DrawingFiles.uuid(input.lineId());
        // Serialize first creation for this requirement; repeat open returns its existing drawing.
        var locks=jdbc.queryForList("SELECT id FROM dm_design_requirement WHERE tenant_id=? AND id=? FOR UPDATE",tenant(),input.requirementId());
        if(locks.isEmpty())throw new ServiceException(404,"需求不存在或不属于当前工厂");
        var requirement=requirements.get(input.requirementId(),null);owner(requirement);
        if(requirement.revision()!=input.expectedRequirementRevision())throw new ServiceException(409,"需求基线已变化，请重新加载");
        Snapshot existing=find(input.requirementId(),input.lineId());if(existing!=null)return existing;
        var line=line(requirement,input.lineId());var demand=line.requirement();
        String id=UUID.randomUUID().toString(),now=Instant.now().toString();
        JsonNode geometry=validator.seed(id,line.id(),line.mark(),line.quantity(),demand.widthMm(),demand.heightMm());
        Snapshot doc=new Snapshot(SCHEMA,id,tenant(),requirement.id(),requirement.revision(),line.id(),1,"DRAFT",actor(),actor(),now,now,"按需求尺寸创建图纸，材料型号待设计确认",geometry,
            new Metadata(autoNumber(id),"门窗设计 "+line.mark(),"","REQUIREMENT"),false);
        persist(doc,true,null,null);return decorate(doc,canEdit(doc));
    }
    @Transactional public Snapshot create(Create input) {
        if(!permissions.hasAnyPermissions(actor(),"doormes:design:drawing-create"))throw new ServiceException(403,"当前账号没有新建独立设计权限");
        if(input.mark()==null || !input.mark().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,39}") || input.quantity()<1 || input.quantity()>1000 ||
            !Double.isFinite(input.widthMm()) || !Double.isFinite(input.heightMm()) || input.widthMm()<1 || input.widthMm()>50000 || input.heightMm()<1 || input.heightMm()>50000)
            throw new ServiceException(400,"窗编号、数量或尺寸无效");
        String requestId=input.clientRequestId();if(requestId!=null){DrawingFiles.uuid(requestId);requestId=requestId.toLowerCase(Locale.ROOT);}
        String id=UUID.randomUUID().toString();Metadata meta=metadata(new Metadata(input.number(),input.name(),input.note(),"INDEPENDENT"),"INDEPENDENT",autoNumber(id));
        String hash=payloadHash(input,meta);
        if(requestId!=null){
            var retry=jdbc.queryForList("SELECT id,create_payload_hash FROM dm_drawing WHERE tenant_id=? AND created_by=? AND create_request_id=?",tenant(),actor(),requestId);
            if(!retry.isEmpty()) {
                if(!hash.equals(retry.get(0).get("create_payload_hash")))throw new ServiceException(409,"本次新建请求已使用其他参数，请重新开始新建设计");
                return get((String)retry.get(0).get("id"),null);
            }
        }
        unique(meta.number(),id);
        String now=Instant.now().toString();JsonNode geometry=validator.seed(id,UUID.randomUUID().toString(),input.mark(),input.quantity(),input.widthMm(),input.heightMm());
        Snapshot doc=new Snapshot(SCHEMA,id,tenant(),null,0,null,1,"DRAFT",actor(),actor(),now,now,"新建独立门窗设计",geometry,meta,false);
        persist(doc,true,requestId,hash);return decorate(doc,canEdit(doc));
    }
    @Transactional public Snapshot save(String id,Save input) {
        Snapshot old=load(id,null,true);
        if(!permissions.hasAnyPermissions(actor(),"doormes:design:drawing-save"))throw new ServiceException(403,"当前账号没有保存图纸权限");
        if(old.requirementId()==null){if(old.createdBy()!=actor() && !permissions.hasAnyRoles(actor(),"super_admin"))throw new ServiceException(403,"只有创建设计人员或管理员可以修改该图纸");}
        else owner(requirements.get(old.requirementId(),null));
        if(old.revision()!=input.expectedRevision())throw new ServiceException(409,"图纸已被其他操作修订，请重新加载后核对，不能覆盖");
        if(input.document()==null || input.changeNote()==null || input.changeNote().isBlank() || input.changeNote().length()>1000)throw new ServiceException(400,"图纸文档和修订说明不能为空");
        if(!old.id().equals(input.document().path("designId").asText()))throw new ServiceException(400,"图纸内部编号不能修改");
        if(!old.status().equals("DRAFT"))throw new ServiceException(409,"已发布图纸不能直接修改，请发起设计变更");
        catalog.validateReferences(input.document());
        assets.validateReferences(input.document());
        JsonNode document=validator.validate(input.document());
        catalog.validateReferences(document);
        assets.validateReferences(document);
        Metadata previous=effectiveMetadata(old);Metadata meta=input.metadata()==null?previous:metadata(input.metadata(),previous.source(),previous.number());unique(meta.number(),id);
        Snapshot doc=new Snapshot(SCHEMA,old.id(),tenant(),old.requirementId(),old.requirementRevision(),old.lineId(),old.revision()+1,"DRAFT",old.createdBy(),actor(),old.createdAt(),Instant.now().toString(),input.changeNote().trim(),document,meta,false);
        persist(doc,false,null,null);return decorate(doc,canEdit(doc));
    }
    private RequirementModels.Line line(RequirementModels.Document requirement,String lineId){return requirement.demand().lines().stream().filter(item->item.id().equals(lineId)).findFirst().orElseThrow(()->new ServiceException(404,"需求明细不存在"));}
    private void owner(RequirementModels.Document requirement) {
        if(!"IN_DESIGN".equals(requirement.status()))throw new ServiceException(409,"请先由研发领用需求，再绘制图纸");
        if(!Objects.equals(requirement.assignedTo(),actor()) && !permissions.hasAnyRoles(actor(),"super_admin"))throw new ServiceException(403,"只有领用研发人员或管理员可以修改该图纸");
    }
    private String autoNumber(String id){return "DW-"+id.replace("-","").substring(0,12).toUpperCase(Locale.ROOT);}
    private Metadata metadata(Metadata input,String source,String fallback) {
        String number=input.number()==null || input.number().isBlank()?fallback:input.number().trim().toUpperCase(Locale.ROOT);
        String name=input.name()==null?"":input.name().trim(),note=input.note()==null?"":input.note().trim();
        if(!number.matches("[A-Z0-9\u4e00-\u9fff][A-Z0-9\u4e00-\u9fff._/-]{0,59}") || name.isEmpty() || name.length()>200 || note.length()>1000)throw new ServiceException(400,"编号、名称或备注格式无效");
        if(input.source()!=null && !source.equals(input.source()))throw new ServiceException(400,"图纸来源不能修改");
        return new Metadata(number,name,note,source);
    }
    private Metadata effectiveMetadata(Snapshot doc){
        // Legacy history is derived from that immutable document, never from mutable head metadata.
        if(doc.metadata()!=null)return doc.metadata();
        return new Metadata(autoNumber(doc.id()),"门窗设计 "+doc.document().path("windows").path(0).path("mark").asText("C1"),"",doc.requirementId()==null?"INDEPENDENT":"REQUIREMENT");
    }
    private Snapshot decorate(Snapshot doc,boolean editable){return new Snapshot(doc.schemaVersion(),doc.id(),doc.tenantId(),doc.requirementId(),doc.requirementRevision(),doc.lineId(),doc.revision(),doc.status(),doc.createdBy(),doc.changedBy(),doc.createdAt(),doc.updatedAt(),doc.changeNote(),doc.document(),effectiveMetadata(doc),editable);}
    private boolean canEdit(Snapshot doc){
        if(!"DRAFT".equals(doc.status()) || !permissions.hasAnyPermissions(actor(),"doormes:design:drawing-save"))return false;
        if(doc.requirementId()==null)return doc.createdBy()==actor() || permissions.hasAnyRoles(actor(),"super_admin");
        var req=requirements.get(doc.requirementId(),null);
        return "IN_DESIGN".equals(req.status()) && (Objects.equals(req.assignedTo(),actor()) || permissions.hasAnyRoles(actor(),"super_admin"));
    }
    private void unique(String number,String id){
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM dm_drawing WHERE tenant_id=? AND LOWER(drawing_number)=LOWER(?) AND id<>?",Long.class,tenant(),number,id);
        if(count!=null && count>0)throw new ServiceException(409,"图纸编号已存在，请使用当前工厂内唯一的编号");
    }
    private String payloadHash(Create input,Metadata meta){
        try {
            String requested=input.number()==null || input.number().isBlank()?"":meta.number();
            byte[] bytes=new ObjectMapper().writeValueAsBytes(List.of(requested,meta.name(),meta.note(),input.mark(),input.quantity(),input.widthMm(),input.heightMm()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(Exception ex){throw new ServiceException(500,"新建请求校验失败");}
    }
    private void persist(Snapshot doc,boolean create,String requestId,String payloadHash) {
        var stored=files.write(doc);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)files.rollback(doc);}});
        try {
            Metadata meta=effectiveMetadata(doc);
            if(create)jdbc.update("INSERT INTO dm_drawing (id,tenant_id,requirement_id,requirement_revision,line_id,revision,status,created_by,updated_at,drawing_number,name,note,source_type,created_at,create_request_id,create_payload_hash) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",doc.id(),tenant(),doc.requirementId(),doc.requirementRevision(),doc.lineId(),doc.revision(),doc.status(),doc.createdBy(),doc.updatedAt(),meta.number(),meta.name(),meta.note(),meta.source(),doc.createdAt(),requestId,payloadHash);
            else jdbc.update("UPDATE dm_drawing SET revision=?,updated_at=?,drawing_number=?,name=?,note=? WHERE tenant_id=? AND id=?",doc.revision(),doc.updatedAt(),meta.number(),meta.name(),meta.note(),tenant(),doc.id());
            jdbc.update("INSERT INTO dm_drawing_version (tenant_id,drawing_id,revision,json_path,sha256,change_note,changed_by,updated_at) VALUES (?,?,?,?,?,?,?,?)",tenant(),doc.id(),doc.revision(),stored.relativePath(),stored.sha256(),doc.changeNote(),doc.changedBy(),doc.updatedAt());
        }catch(DuplicateKeyException ex){throw new ServiceException(409,"图纸编号或本次新建请求已存在，请重新加载后重试");}
    }
}
