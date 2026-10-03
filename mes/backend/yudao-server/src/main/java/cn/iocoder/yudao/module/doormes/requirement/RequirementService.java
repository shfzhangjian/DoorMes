package cn.iocoder.yudao.module.doormes.requirement;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static cn.iocoder.yudao.module.doormes.requirement.RequirementModels.*;

@Service
@Profile("doormes")
public class RequirementService {
    private final JdbcTemplate jdbc;
    private final RequirementFiles files;
    private final PermissionService permissions;
    public RequirementService(JdbcTemplate jdbc,RequirementFiles files,PermissionService permissions) {
        this.jdbc=jdbc; this.files=files; this.permissions=permissions;
    }
    private long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private long actor() {
        Long id=SecurityFrameworkUtils.getLoginUserId();
        if (id==null) throw new ServiceException(401,"请先登录");
        return id;
    }
    public PageResult<Summary> page(String keyword,String status,int pageNo,int pageSize) {
        if (pageNo<1 || pageNo>10000 || pageSize<1 || pageSize>100) throw new ServiceException(400,"分页参数无效");
        if (keyword.length()>100 || !Set.of("","DRAFT","SUBMITTED","IN_DESIGN").contains(status)) throw new ServiceException(400,"查询条件无效");
        String where=" WHERE tenant_id=? AND (number LIKE ? OR customer LIKE ? OR project LIKE ?)";
        List<Object> args=new ArrayList<>(List.of(tenant(),"%"+keyword+"%","%"+keyword+"%","%"+keyword+"%"));
        if (!status.isBlank()) {where+=" AND status=?";args.add(status);}
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM dm_design_requirement"+where,Long.class,args.toArray());
        args.add(pageSize);args.add((pageNo-1)*pageSize);
        List<Summary> list=jdbc.query("SELECT * FROM dm_design_requirement"+where+" ORDER BY updated_at DESC,id LIMIT ? OFFSET ?",(rs,n)->
            new Summary(rs.getString("id"),rs.getString("number"),rs.getString("customer"),rs.getString("project"),
                rs.getInt("revision"),rs.getString("status"),rs.getLong("created_by"),(Long)rs.getObject("assigned_to"),
                rs.getInt("line_count"),rs.getInt("quantity"),rs.getString("updated_at")),args.toArray());
        return new PageResult<>(list,count);
    }
    private Document load(String id,Integer revision,boolean lock) {
        validateId(id);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT revision FROM dm_design_requirement WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),tenant(),id);
        if(rows.isEmpty()) throw new ServiceException(404,"需求不存在或不属于当前工厂");
        int chosen=revision==null?((Number)rows.get(0).get("revision")).intValue():revision;
        List<String> hashes=jdbc.query("SELECT sha256 FROM dm_design_requirement_version WHERE tenant_id=? AND requirement_id=? AND revision=?",(rs,n)->rs.getString(1),tenant(),id,chosen);
        if(hashes.isEmpty()) throw new ServiceException(404,"需求版本不存在");
        return files.read(tenant(),id,chosen,hashes.get(0));
    }
    public Document get(String id,Integer revision) { return load(id,revision,false); }
    public List<Version> versions(String id) {
        load(id,null,false);
        return jdbc.query("SELECT * FROM dm_design_requirement_version WHERE tenant_id=? AND requirement_id=? ORDER BY revision DESC",(rs,n)->
            new Version(rs.getInt("revision"),rs.getString("action"),rs.getString("change_note"),rs.getLong("changed_by"),rs.getString("updated_at")),tenant(),id);
    }
    @Transactional
    public Document create(Mutation request) {
        if(request.expectedRevision()!=0) throw new ServiceException(409,"新增需求的期望版本必须为 0");
        Input demand=normalize(request.demand(),null);
        String id=UUID.randomUUID().toString(),now=Instant.now().toString();
        Document doc=new Document(DOCUMENT_SCHEMA,id,tenant(),1,"DRAFT",actor(),null,now,now,actor(),"CREATE",request.changeNote().trim(),demand);
        try { persist(doc,true); }
        catch(DuplicateKeyException e) {throw new ServiceException(409,"当前工厂已存在同一需求单号，请修改单号");}
        return doc;
    }
    @Transactional
    public Document update(String id,Mutation request) {
        Document old=load(id,null,true);
        expected(old,request.expectedRevision());
        owner(old);
        if(old.status().equals("IN_DESIGN")) throw new ServiceException(409,"研发已领用，请通过后续设计变更流程修订需求，不能覆盖当前基线");
        Input demand=normalize(request.demand(),old.demand());
        Document doc=next(old,"DRAFT",null,"REVISE",request.changeNote(),demand);
        try {persist(doc,false);}
        catch(DuplicateKeyException e) {throw new ServiceException(409,"当前工厂已存在同一需求单号，请修改单号");}
        return doc;
    }
    @Transactional
    public Document submit(String id,Action request) {
        Document old=load(id,null,true);
        expected(old,request.expectedRevision()); owner(old);
        if(!old.status().equals("DRAFT")) throw new ServiceException(409,"只有需求草稿可以提交研发");
        for(Line line:old.demand().lines()) {
            Demand r=line.requirement();
            if(r.material().isBlank() || r.glass().isBlank() || r.hardware().isBlank() || r.finish().isBlank() || r.dueDate().isBlank()) {
                throw new ServiceException(400,"提交前请补齐每项门窗的型材、玻璃、五金、表面颜色和需求日期");
            }
        }
        Document doc=next(old,"SUBMITTED",null,"SUBMIT",request.note(),old.demand()); persist(doc,false); return doc;
    }
    @Transactional
    public Document claim(String id,Action request) {
        Document old=load(id,null,true);
        expected(old,request.expectedRevision());
        if(!old.status().equals("SUBMITTED") || old.assignedTo()!=null) throw new ServiceException(409,"需求已被领用或尚未提交，请刷新列表");
        Document doc=next(old,"IN_DESIGN",actor(),"CLAIM",request.note(),old.demand()); persist(doc,false); return doc;
    }
    private Document next(Document old,String status,Long assigned,String action,String note,Input input) {
        return new Document(DOCUMENT_SCHEMA,old.id(),old.tenantId(),old.revision()+1,status,old.createdBy(),assigned,
            old.createdAt(),Instant.now().toString(),actor(),action,note.trim(),input);
    }
    private void expected(Document old,int revision) {
        if(old.revision()!=revision) throw new ServiceException(409,"需求已被其他人修改，请重新加载后再操作");
    }
    private void owner(Document old) {
        if(old.createdBy()!=actor() && !permissions.hasAnyRoles(actor(),"super_admin")) throw new ServiceException(403,"只能修改本人创建的需求");
    }
    private static void validateId(String id) {
        try { if(!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
        catch(IllegalArgumentException e) {throw new ServiceException(400,"需求内部编号无效");}
    }
    static Input normalize(Input input,Input previous) {
        if(!INPUT_SCHEMA.equals(input.schemaVersion())) throw new ServiceException(400,"不支持的需求 JSON 格式版本");
        Set<String> marks=new HashSet<>(),ids=new HashSet<>();
        Set<String> existing=previous==null?Set.of():new HashSet<>(previous.lines().stream().map(Line::id).toList());
        List<Line> lines=new ArrayList<>();
        for(Line line:input.lines()) {
            if(!line.kind().equals("custom")) throw new ServiceException(501,"标准设计必须引用已发布目录版本；目录接口尚在接入，当前请使用定制需求");
            String mark=line.mark().trim();
            if(!marks.add(mark.toUpperCase(Locale.ROOT))) throw new ServiceException(400,"门窗短编号不能重复");
            String id=line.id();
            if(id==null || id.isBlank()) id=UUID.randomUUID().toString();
            else if(previous==null || !existing.contains(id)) throw new ServiceException(400,"明细内部编号不属于当前需求");
            if(!ids.add(id)) throw new ServiceException(400,"需求明细内部编号重复");
            Demand r=line.requirement();
            if(!Double.isFinite(r.widthMm()) || !Double.isFinite(r.heightMm())) throw new ServiceException(400,"尺寸必须是有限数值");
            if(!r.dueDate().isBlank()) try {LocalDate.parse(r.dueDate());} catch(RuntimeException e) {throw new ServiceException(400,"需求日期无效");}
            lines.add(new Line(id,mark,line.kind(),line.quantity(),new Demand(r.widthMm(),r.heightMm(),r.material().trim(),r.glass().trim(),r.hardware().trim(),r.finish().trim(),r.dueDate(),r.note().trim())));
        }
        return new Input(INPUT_SCHEMA,input.number().trim(),input.customer().trim(),input.project().trim(),lines,input.note().trim());
    }
    private void persist(Document document,boolean create) {
        // Unique number is checked before writing, in addition to its authoritative SQL constraint.
        Long duplicates=jdbc.queryForObject("SELECT COUNT(*) FROM dm_design_requirement WHERE tenant_id=? AND number=? AND id<>?",Long.class,tenant(),document.demand().number(),document.id());
        if(duplicates!=null && duplicates>0) throw new ServiceException(409,"当前工厂已存在同一需求单号");
        RequirementFiles.Stored stored=files.write(document);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) files.rollback(document); }
        });
        Input demand=document.demand(); int quantity=demand.lines().stream().mapToInt(Line::quantity).sum();
        if(create) jdbc.update("INSERT INTO dm_design_requirement (id,tenant_id,number,customer,project,revision,status,created_by,assigned_to,line_count,quantity,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
            document.id(),tenant(),demand.number(),demand.customer(),demand.project(),document.revision(),document.status(),document.createdBy(),document.assignedTo(),demand.lines().size(),quantity,document.createdAt(),document.updatedAt());
        else jdbc.update("UPDATE dm_design_requirement SET number=?,customer=?,project=?,revision=?,status=?,assigned_to=?,line_count=?,quantity=?,updated_at=? WHERE tenant_id=? AND id=?",
            demand.number(),demand.customer(),demand.project(),document.revision(),document.status(),document.assignedTo(),demand.lines().size(),quantity,document.updatedAt(),tenant(),document.id());
        jdbc.update("INSERT INTO dm_design_requirement_version (tenant_id,requirement_id,revision,json_path,sha256,action,change_note,changed_by,updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
            tenant(),document.id(),document.revision(),stored.relativePath(),stored.sha256(),document.action(),document.changeNote(),document.changedBy(),document.updatedAt());
    }
}
