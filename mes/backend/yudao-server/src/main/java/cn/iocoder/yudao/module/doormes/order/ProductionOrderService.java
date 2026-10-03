package cn.iocoder.yudao.module.doormes.order;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.doormes.drawing.*;
import cn.iocoder.yudao.module.doormes.requirement.*;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static cn.iocoder.yudao.module.doormes.order.ProductionOrderModels.*;

@Service @Profile("doormes")
public class ProductionOrderService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final DrawingService drawings;
    private final DrawingValidator validator;
    private final RequirementService requirements;
    private final PermissionService permissions;
    public ProductionOrderService(JdbcTemplate jdbc,ObjectMapper mapper,DrawingService drawings,DrawingValidator validator,
        RequirementService requirements,PermissionService permissions) {
        this.jdbc=jdbc;this.mapper=mapper;this.drawings=drawings;this.validator=validator;this.requirements=requirements;this.permissions=permissions;
    }
    private long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private long actor(){Long id=SecurityFrameworkUtils.getLoginUserId();if(id==null)throw error(401,"请先登录");return id;}
    private boolean admin(){return permissions.hasAnyRoles(actor(),"super_admin");}
    private void permission(String permission){if(!admin()&&!permissions.hasAnyPermissions(actor(),permission))throw error(403,"当前账号没有此操作权限");}
    private void readable(){if(!admin()&&!permissions.hasAnyPermissions(actor(),"doormes:production:query","doormes:changes:query"))throw error(403,"没有生产订单查看权限");}
    private static ServiceException error(int code,String message){return new ServiceException(code,message);}
    private static String now(){return Instant.now().toString();}
    private static String note(String value){if(value==null||value.isBlank()||value.length()>1000)throw error(400,"操作说明不能为空或超过1000字");return value.trim();}
    private void expected(Order old,int value){if(old.revision()!=value)throw error(409,"订单已变化，请刷新后重试；未覆盖原版本");}
    private void owner(Order order){if(order.createdBy()!=actor()&&!admin())throw error(403,"只能操作本人创建的订单");}
    private void designer(Order order){if(!Objects.equals(order.assignedTo(),actor())&&!admin())throw error(403,"只有领用设计人员可以绑定本订单图纸");}
    private Order load(String id,Integer revision,boolean lock){
        DrawingFiles.uuid(id);
        var rows=jdbc.queryForList("SELECT revision FROM dm_production_order WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),tenant(),id);
        if(rows.isEmpty())throw error(404,"生产订单不存在或不属于当前工厂");
        int chosen=revision==null?((Number)rows.get(0).get("revision")).intValue():revision;
        var versions=jdbc.queryForList("SELECT payload_json,sha256 FROM dm_production_order_version WHERE tenant_id=? AND order_id=? AND revision=?",tenant(),id,chosen);
        if(versions.isEmpty())throw error(404,"订单版本不存在");
        String json=String.valueOf(versions.get(0).get("payload_json"));
        if(!digest(json).equals(versions.get(0).get("sha256")))throw error(409,"订单版本校验失败");
        try {
            Order result=mapper.readValue(json,Order.class);
            if(!SCHEMA.equals(result.schemaVersion())||result.tenantId()!=tenant()||!id.equals(result.id())||result.revision()!=chosen)throw error(409,"订单版本身份无效");
            return result;
        }catch(java.io.IOException e){throw error(500,"订单版本加载失败");}
    }
    public Order get(String id,Integer revision){readable();return load(id,revision,false);}
    public PageResult<Summary> page(String keyword,String type,String status,int pageNo,int pageSize){
        readable();keyword=keyword==null?"":keyword.trim();type=type==null?"":type;status=status==null?"":status;
        if(keyword.length()>100||pageNo<1||pageNo>10000||pageSize<1||pageSize>100||!Set.of("","STANDARD","CUSTOM").contains(type)||!Set.of("","DRAFT","SUBMITTED","IN_DESIGN","BOM_READY").contains(status))throw error(400,"查询条件无效");
        String where=" WHERE tenant_id=? AND (number LIKE ? OR customer LIKE ? OR project LIKE ?)";
        List<Object> args=new ArrayList<>(List.of(tenant(),"%"+keyword+"%","%"+keyword+"%","%"+keyword+"%"));
        if(!type.isEmpty()){where+=" AND order_type=?";args.add(type);}if(!status.isEmpty()){where+=" AND status=?";args.add(status);}
        long total=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM dm_production_order"+where,Long.class,args.toArray()));
        args.add(pageSize);args.add((pageNo-1)*pageSize);
        var ids=jdbc.query("SELECT id FROM dm_production_order"+where+" ORDER BY updated_at DESC,id LIMIT ? OFFSET ?",(rs,n)->rs.getString(1),args.toArray());
        return new PageResult<>(ids.stream().map(id->summary(load(id,null,false))).toList(),total);
    }
    private Summary summary(Order order){return new Summary(order.id(),order.revision(),order.number(),order.customer(),order.project(),order.type(),order.quantity(),order.note(),order.status(),order.requirement(),order.drawing(),false,order.createdBy(),order.assignedTo(),order.updatedAt(),order.changes().isEmpty()?null:order.changes().get(order.changes().size()-1).status(),order.changes().size());}
    /** The legacy demand endpoints must not bypass the order's version/state machine. */
    public void assertIndependentRequirement(String id){
        DrawingFiles.uuid(id);
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM dm_production_order WHERE tenant_id=? AND requirement_id=?",Long.class,tenant(),id);
        if(count!=null&&count>0)throw error(409,"此需求由生产设计订单管理，请回生产设计订单提交或领用，不可从独立需求入口修改");
    }
    public List<Version> versions(String id){readable();load(id,null,false);return jdbc.query("SELECT revision,action,change_note,changed_by,updated_at FROM dm_production_order_version WHERE tenant_id=? AND order_id=? ORDER BY revision DESC",(rs,n)->new Version(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getLong(4),rs.getString(5)),tenant(),id);}
    @Transactional public Order create(Create input){
        permission("doormes:orders:create");
        validate(input);
        String id=UUID.randomUUID().toString(),time=now();RequirementRef requirement=null;DrawingRef drawing=null;JsonNode bom=null;String status="DRAFT";
        if("STANDARD".equals(input.type())){
            if(input.drawingRevision()==null||input.drawingRevision()<1)throw error(400,"请选择明确的已保存图纸版本");
            var snapshot=drawings.get(input.drawingId(),input.drawingRevision());drawing=ref(snapshot);bom=bom(snapshot,input.quantity());status="BOM_READY";
        }else {
            Custom c=input.custom();
            // The drawing represents ONE set; order quantity is multiplied once at the order BOM boundary.
            var demand=new RequirementModels.Demand(c.widthMm(),c.heightMm(),c.material(),c.glass(),c.hardware(),c.finish(),c.dueDate(),c.note());
            var created=requirements.create(new RequirementModels.Mutation(0,"生产订单创建定制需求",new RequirementModels.Input(RequirementModels.INPUT_SCHEMA,"REQ-"+id.replace("-","").substring(0,16),input.customer(),input.project(),List.of(new RequirementModels.Line(null,c.mark(),"custom",1,demand)),input.note())));
            requirement=new RequirementRef(created.id(),created.revision(),created.demand().lines().get(0).id());
        }
        Order result=new Order(SCHEMA,id,tenant(),1,input.number().trim().toUpperCase(Locale.ROOT),input.customer().trim(),input.project().trim(),input.type(),input.quantity(),input.note().trim(),status,requirement,drawing,bom,List.of(),false,actor(),null,time,time,actor(),"CREATE","创建生产设计订单");
        persist(result,true);return result;
    }
    private static void validate(Create input){
        if(input.number()==null||!input.number().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,39}")||input.customer()==null||input.customer().isBlank()||input.customer().length()>200||input.project()==null||input.project().length()>200||input.note()==null||input.note().length()>2000||input.quantity()<1||input.quantity()>1000||input.type()==null||!Set.of("STANDARD","CUSTOM").contains(input.type()))throw error(400,"订单字段无效");
        if("CUSTOM".equals(input.type())){
            Custom c=input.custom();
            if(c==null||c.mark()==null||!c.mark().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,39}")||!Double.isFinite(c.widthMm())||!Double.isFinite(c.heightMm())||c.widthMm()<1||c.widthMm()>50000||c.heightMm()<1||c.heightMm()>50000||c.material()==null||c.material().length()>100||c.glass()==null||c.glass().length()>100||c.hardware()==null||c.hardware().length()>100||c.finish()==null||c.finish().length()>100||c.dueDate()==null||c.dueDate().length()>10||c.note()==null||c.note().length()>2000)throw error(400,"定制需求尺寸、材料或文字字段无效");
        }
    }
    @Transactional public Order update(String id,Update input){
        permission("doormes:orders:update");Order old=load(id,null,true);expected(old,input.expectedRevision());owner(old);
        if(!"DRAFT".equals(old.status())||!"CUSTOM".equals(old.type())||!"CUSTOM".equals(input.type())||old.requirement()==null)throw error(409,"只能修改未提交的定制订单草稿，订单类型不可变更");
        validate(new Create(input.number(),input.customer(),input.project(),input.type(),input.quantity(),input.note(),null,null,input.custom()));
        var previous=requirements.get(old.requirement().id(),old.requirement().revision());Custom c=input.custom();
        var demand=new RequirementModels.Demand(c.widthMm(),c.heightMm(),c.material(),c.glass(),c.hardware(),c.finish(),c.dueDate(),c.note());
        var changed=requirements.update(old.requirement().id(),new RequirementModels.Mutation(old.requirement().revision(),"修订生产设计订单草稿",new RequirementModels.Input(RequirementModels.INPUT_SCHEMA,previous.demand().number(),input.customer(),input.project(),List.of(new RequirementModels.Line(old.requirement().lineId(),c.mark(),"custom",1,demand)),input.note())));
        var result=new Order(SCHEMA,old.id(),tenant(),old.revision()+1,input.number().trim().toUpperCase(Locale.ROOT),input.customer().trim(),input.project().trim(),old.type(),input.quantity(),input.note().trim(),old.status(),new RequirementRef(changed.id(),changed.revision(),old.requirement().lineId()),null,null,old.changes(),false,old.createdBy(),null,old.createdAt(),now(),actor(),"UPDATE","修订订单草稿及关联需求");
        persist(result,false);return result;
    }
    @Transactional public Order submit(String id,Action input){
        permission("doormes:orders:submit");Order old=load(id,null,true);expected(old,input.expectedRevision());owner(old);
        if(!"DRAFT".equals(old.status())||old.requirement()==null)throw error(409,"只有定制订单草稿可以提交研发");
        var result=requirements.submit(old.requirement().id(),new RequirementModels.Action(old.requirement().revision(),note(input.note())));
        return saveNext(old,"SUBMITTED",new RequirementRef(result.id(),result.revision(),old.requirement().lineId()),null,old.drawing(),old.bom(),old.changes(),"SUBMIT",input.note());
    }
    @Transactional public Order claim(String id,Action input){
        permission("doormes:design:claim");Order old=load(id,null,true);expected(old,input.expectedRevision());
        if(!"SUBMITTED".equals(old.status())||old.requirement()==null)throw error(409,"订单尚未提交或已被领用");
        var result=requirements.claim(old.requirement().id(),new RequirementModels.Action(old.requirement().revision(),note(input.note())));
        return saveNext(old,"IN_DESIGN",new RequirementRef(result.id(),result.revision(),old.requirement().lineId()),actor(),old.drawing(),old.bom(),old.changes(),"CLAIM",input.note());
    }
    @Transactional public OpenResult openDrawing(String id,Action input){
        permission("doormes:design:drawing-save");Order old=load(id,null,true);expected(old,input.expectedRevision());designer(old);note(input.note());
        if(old.requirement()==null||!Set.of("IN_DESIGN","BOM_READY").contains(old.status()))throw error(409,"请先领用定制订单");
        var snapshot=drawings.open(new DrawingModels.Open(old.requirement().id(),old.requirement().lineId(),old.requirement().revision()));
        // Opening the editor must not silently replace the order's accepted drawing/BOM baseline.
        return new OpenResult(old,snapshot);
    }
    @Transactional public Order bind(String id,Bind input){
        permission("doormes:orders:bind");Order old=load(id,null,true);expected(old,input.expectedRevision());designer(old);
        if(!"IN_DESIGN".equals(old.status())||old.requirement()==null||old.drawing()!=null)throw error(409,"已有图纸基线的订单必须走变更申请");
        var snapshot=drawings.get(input.drawingId(),input.drawingRevision());
        if(!old.requirement().id().equals(snapshot.requirementId())||!old.requirement().lineId().equals(snapshot.lineId()))throw error(400,"图纸不属于该订单需求");
        return saveNext(old,"BOM_READY",old.requirement(),old.assignedTo(),ref(snapshot),bom(snapshot,old.quantity()),old.changes(),"BIND",input.note());
    }
    @Transactional public Order requestChange(String id,RequestChange input){
        permission("doormes:orders:change-request");Order old=load(id,null,true);expected(old,input.expectedRevision());
        if(!"BOM_READY".equals(old.status())||old.drawing()==null)throw error(409,"订单尚未建立图纸BOM基线");
        if(old.type().equals("CUSTOM"))designer(old);
        if(old.changes().stream().anyMatch(c->Set.of("PENDING","APPROVED").contains(c.status())))throw error(409,"存在尚未完成的变更，请先审核并应用或驳回");
        if(!old.drawing().id().equals(input.drawingId())||input.drawingRevision()<=old.drawing().revision())throw error(400,"请选择同一图纸更高的已保存版本");
        var snapshot=drawings.get(input.drawingId(),input.drawingRevision());JsonNode target=bom(snapshot,old.quantity());
        var change=new Change(UUID.randomUUID().toString(),"PENDING",note(input.reason()),old.drawing(),ref(snapshot),diff(old.bom(),target),target,actor(),now(),null,null,null,null,null);
        var changes=new ArrayList<>(old.changes());changes.add(change);
        return saveNext(old,old.status(),old.requirement(),old.assignedTo(),old.drawing(),old.bom(),changes,"CHANGE_REQUEST",input.reason());
    }
    @Transactional public Order review(String id,Review input){
        permission("doormes:orders:change-review");Order old=load(id,null,true);expected(old,input.expectedRevision());Change c=change(old,input.changeId());
        if(!"PENDING".equals(c.status()))throw error(409,"该变更已审核");
        if(c.requestedBy()==actor())throw error(403,"申请人不能审核本人申请的变更");
        var reviewed=new Change(c.id(),input.approved()?"APPROVED":"REJECTED",c.reason(),c.from(),c.to(),c.diff(),c.targetBom(),c.requestedBy(),c.requestedAt(),actor(),note(input.note()),now(),null,null);
        return saveNext(old,old.status(),old.requirement(),old.assignedTo(),old.drawing(),old.bom(),replace(old,reviewed),input.approved()?"CHANGE_APPROVE":"CHANGE_REJECT",input.note());
    }
    @Transactional public Order apply(String id,Apply input){
        permission("doormes:orders:change-apply");Order old=load(id,null,true);expected(old,input.expectedRevision());Change c=change(old,input.changeId());
        if(!"APPROVED".equals(c.status()))throw error(409,"只有审核通过的变更可以确认采用");
        if(!Objects.equals(old.drawing(),c.from()))throw error(409,"原图纸基线不一致，禁止应用过期变更");
        // Verify exact historical file/hash again; never fetch or adopt the latest revision.
        if(!ref(drawings.get(c.to().id(),c.to().revision())).equals(c.to()))throw error(409,"目标图纸版本校验失败");
        var applied=new Change(c.id(),"APPLIED",c.reason(),c.from(),c.to(),c.diff(),c.targetBom(),c.requestedBy(),c.requestedAt(),c.reviewedBy(),c.reviewNote(),c.reviewedAt(),actor(),now());
        return saveNext(old,old.status(),old.requirement(),old.assignedTo(),c.to(),c.targetBom(),replace(old,applied),"CHANGE_APPLY",input.note());
    }
    private Change change(Order order,String id){DrawingFiles.uuid(id);return order.changes().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow(()->error(404,"变更申请不存在"));}
    private List<Change> replace(Order old,Change replacement){return old.changes().stream().map(c->c.id().equals(replacement.id())?replacement:c).toList();}
    private DrawingRef ref(DrawingModels.Snapshot snapshot){
        String hash=jdbc.queryForObject("SELECT sha256 FROM dm_drawing_version WHERE tenant_id=? AND drawing_id=? AND revision=?",String.class,tenant(),snapshot.id(),snapshot.revision());
        return new DrawingRef(snapshot.id(),snapshot.revision(),snapshot.metadata().number(),snapshot.metadata().name(),hash);
    }
    private JsonNode bom(DrawingModels.Snapshot snapshot,int quantity){
        JsonNode calculated=validator.bom(snapshot.document());
        if(!calculated.isObject()||!calculated.path("lines").isArray()||calculated.path("lines").isEmpty())throw error(400,"图纸未能生成真实组成件BOM，禁止建立空基线");
        ObjectNode result=calculated.deepCopy();result.put("productionReady",false);result.put("orderQuantity",quantity);result.put("drawingId",snapshot.id());result.put("drawingRevision",snapshot.revision());
        Set<String> ids=new HashSet<>();
        for(JsonNode item:result.path("lines")){
            String key=item.path("objectId").asText();double value=item.path("quantity").asDouble(Double.NaN);
            if(!(item instanceof ObjectNode line)||key.isBlank()||!ids.add(key)||!Double.isFinite(value)||value<=0)throw error(400,"组成件BOM编号或数量无效");
            line.put("orderQuantity",quantity);line.put("totalQuantity",value*quantity);line.put("productionReady",false);
        }
        return result;
    }
    static Diff diff(JsonNode before,JsonNode after){
        Map<String,JsonNode> left=lines(before),right=lines(after);List<JsonNode> added=new ArrayList<>(),removed=new ArrayList<>();List<ChangedLine> modified=new ArrayList<>();
        right.forEach((id,line)->{if(!left.containsKey(id))added.add(line);else if(!left.get(id).equals(line))modified.add(new ChangedLine(left.get(id),line));});
        left.forEach((id,line)->{if(!right.containsKey(id))removed.add(line);});return new Diff(added,removed,modified);
    }
    private static Map<String,JsonNode> lines(JsonNode bom){Map<String,JsonNode> result=new LinkedHashMap<>();for(JsonNode line:bom.path("lines"))result.put(line.path("objectId").asText(),line);return result;}
    private Order saveNext(Order old,String status,RequirementRef requirement,Long assigned,DrawingRef drawing,JsonNode bom,List<Change> changes,String action,String changeNote){
        Order result=new Order(SCHEMA,old.id(),tenant(),old.revision()+1,old.number(),old.customer(),old.project(),old.type(),old.quantity(),old.note(),status,requirement,drawing,bom,List.copyOf(changes),false,old.createdBy(),assigned,old.createdAt(),now(),actor(),action,note(changeNote));
        persist(result,false);return result;
    }
    private void persist(Order order,boolean create){
        try {
            if(create)jdbc.update("INSERT INTO dm_production_order(id,tenant_id,number,customer,project,order_type,status,revision,created_by,assigned_to,created_at,updated_at,requirement_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",order.id(),tenant(),order.number(),order.customer(),order.project(),order.type(),order.status(),order.revision(),order.createdBy(),order.assignedTo(),order.createdAt(),order.updatedAt(),order.requirement()==null?null:order.requirement().id());
            else if(jdbc.update("UPDATE dm_production_order SET number=?,customer=?,project=?,status=?,revision=?,assigned_to=?,updated_at=? WHERE tenant_id=? AND id=? AND revision=?",order.number(),order.customer(),order.project(),order.status(),order.revision(),order.assignedTo(),order.updatedAt(),tenant(),order.id(),order.revision()-1)!=1)throw error(409,"订单并发修改冲突");
            String json=mapper.writeValueAsString(order);
            if(json.getBytes(StandardCharsets.UTF_8).length>20_000_000)throw error(400,"订单版本过大，请拆分订单");
            jdbc.update("INSERT INTO dm_production_order_version(tenant_id,order_id,revision,payload_json,sha256,action,change_note,changed_by,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",tenant(),order.id(),order.revision(),json,digest(json),order.action(),order.changeNote(),order.changedBy(),order.updatedAt());
        }catch(DuplicateKeyException e){throw error(409,"订单编号或版本已存在，请刷新并检查编号");}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw error(500,"订单版本保存失败");}
    }
    private static String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
