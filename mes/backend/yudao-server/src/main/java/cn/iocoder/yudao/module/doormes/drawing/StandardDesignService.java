package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @Profile("doormes")
public class StandardDesignService {
    public record Submit(String drawingId,int revision) {}
    public record Decision(int revision,boolean approved,String note) {}
    public record Reuse(String drawingId,int revision) {}
    public record Entry(String drawingId,int revision,String status,boolean active,String number,String name,String note,
                        String reviewNote,String disableReason,long submittedBy,Long reviewedBy,String submittedAt,String reviewedAt) {}
    public record Page(List<Entry> list,long total) {}
    private final JdbcTemplate jdbc;
    private final DrawingService drawings;
    private final PermissionService permissions;
    public StandardDesignService(JdbcTemplate jdbc,DrawingService drawings,PermissionService permissions) {
        this.jdbc=jdbc;this.drawings=drawings;this.permissions=permissions;
    }
    private long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private long actor(){Long id=SecurityFrameworkUtils.getLoginUserId();if(id==null)throw new ServiceException(401,"请先登录");return id;}
    private void allowed(String permission){if(!permissions.hasAnyPermissions(actor(),permission))throw new ServiceException(403,"当前账号没有标准设计操作权限");}
    private void id(String value){DrawingFiles.uuid(value);}
    private Entry entry(String id,int revision){
        var found=jdbc.query("SELECT v.*,s.published_revision,s.status AS library_status,s.disable_reason FROM dm_standard_design_version v JOIN dm_standard_design s ON s.tenant_id=v.tenant_id AND s.drawing_id=v.drawing_id WHERE v.tenant_id=? AND v.drawing_id=? AND v.revision=?",
            (rs,n)->new Entry(rs.getString("drawing_id"),rs.getInt("revision"),rs.getString("status"),
                "PUBLISHED".equals(rs.getString("library_status")) && Objects.equals(rs.getObject("published_revision"),rs.getInt("revision")),
                rs.getString("number"),rs.getString("name"),rs.getString("note"),rs.getString("review_note"),rs.getString("disable_reason"),rs.getLong("submitted_by"),
                rs.getObject("reviewed_by")==null?null:rs.getLong("reviewed_by"),rs.getString("submitted_at"),rs.getString("reviewed_at")),tenant(),id,revision);
        if(found.isEmpty())throw new ServiceException(404,"标准设计版本不存在或不属于当前工厂");
        return found.get(0);
    }
    public Page page(boolean publishedOnly,String keyword,int pageNo,int pageSize){
        if(!publishedOnly)allowed("doormes:design:standard-review");
        if(pageNo<1||pageNo>100000||pageSize<1||pageSize>100||keyword!=null&&keyword.length()>160)throw new ServiceException(400,"查询参数无效");
        String where=" WHERE v.tenant_id=?"+(publishedOnly?" AND s.status='PUBLISHED' AND s.published_revision=v.revision":"");
        List<Object> args=new ArrayList<>();args.add(tenant());
        if(keyword!=null&&!keyword.isBlank()){
            String escaped=keyword.trim().replace("!","!!").replace("%","!%").replace("_","!_");
            where+=" AND (v.number LIKE ? ESCAPE '!' OR v.name LIKE ? ESCAPE '!')";
            args.add("%"+escaped+"%");args.add("%"+escaped+"%");
        }
        String from=" FROM dm_standard_design_version v JOIN dm_standard_design s ON s.tenant_id=v.tenant_id AND s.drawing_id=v.drawing_id"+where;
        long count=Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray()));
        args.add(pageSize);args.add((pageNo-1)*pageSize);
        var list=jdbc.query("SELECT v.drawing_id,v.revision,v.status,v.number,v.name,v.note,v.review_note,v.submitted_by,v.reviewed_by,v.submitted_at,v.reviewed_at,s.status AS library_status,s.published_revision,s.disable_reason"+from+" ORDER BY v.submitted_at DESC,v.drawing_id,v.revision DESC LIMIT ? OFFSET ?",
            (rs,n)->new Entry(rs.getString("drawing_id"),rs.getInt("revision"),rs.getString("status"),
                "PUBLISHED".equals(rs.getString("library_status")) && Objects.equals(rs.getObject("published_revision"),rs.getInt("revision")),
                rs.getString("number"),rs.getString("name"),rs.getString("note"),rs.getString("review_note"),rs.getString("disable_reason"),rs.getLong("submitted_by"),
                rs.getObject("reviewed_by")==null?null:rs.getLong("reviewed_by"),rs.getString("submitted_at"),rs.getString("reviewed_at")),args.toArray());
        return new Page(list,count);
    }
    @Transactional public Entry submit(Submit input){
        allowed("doormes:design:standard-submit");id(input.drawingId());
        if(input.revision()<1)throw new ServiceException(400,"图纸版本无效");
        var drawing=drawings.get(input.drawingId(),input.revision());
        if(!"INDEPENDENT".equals(drawing.metadata().source()))throw new ServiceException(400,"目前仅支持独立设计提交标准库");
        if(drawing.createdBy()!=actor()&&!permissions.hasAnyRoles(actor(),"super_admin"))throw new ServiceException(403,"只有图纸创建人可以提交标准设计");
        String now=Instant.now().toString();
        try {
            jdbc.update("INSERT INTO dm_standard_design(tenant_id,drawing_id,status,updated_at) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE drawing_id=drawing_id",tenant(),input.drawingId(),"DRAFT",now);
            var head=jdbc.queryForList("SELECT published_revision FROM dm_standard_design WHERE tenant_id=? AND drawing_id=? FOR UPDATE",tenant(),input.drawingId());
            if(head.isEmpty())throw new ServiceException(409,"标准设计目录并发创建失败，请重试");
            Object published=head.get(0).get("published_revision");
            if(published instanceof Number && input.revision()<=((Number)published).intValue())throw new ServiceException(409,"只能提交高于已发布版本的新图纸版本");
            Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM dm_standard_design_version WHERE tenant_id=? AND drawing_id=? AND status='SUBMITTED'",Long.class,tenant(),input.drawingId());
            if(pending!=null&&pending>0)throw new ServiceException(409,"该图纸已有待审核版本，请先完成审核");
            jdbc.update("INSERT INTO dm_standard_design_version(tenant_id,drawing_id,revision,status,number,name,note,submitted_by,submitted_at) VALUES(?,?,?,?,?,?,?,?,?)",
                tenant(),input.drawingId(),input.revision(),"SUBMITTED",drawing.metadata().number(),drawing.metadata().name(),drawing.metadata().note(),actor(),now);
            jdbc.update("UPDATE dm_standard_design SET updated_at=? WHERE tenant_id=? AND drawing_id=?",now,tenant(),input.drawingId());
        }catch(DuplicateKeyException ex){throw new ServiceException(409,"该图纸版本已经提交过标准库，请修订图纸后再提交");}
        return entry(input.drawingId(),input.revision());
    }
    @Transactional public Entry review(String drawingId,Decision input){
        allowed("doormes:design:standard-review");id(drawingId);
        String note=note(input.note());
        var head=jdbc.queryForList("SELECT published_revision FROM dm_standard_design WHERE tenant_id=? AND drawing_id=? FOR UPDATE",tenant(),drawingId);
        if(head.isEmpty())throw new ServiceException(404,"标准设计不存在");
        Entry current=entry(drawingId,input.revision());
        if(!"SUBMITTED".equals(current.status()))throw new ServiceException(409,"该版本已完成审核，请刷新列表");
        if(current.submittedBy()==actor())throw new ServiceException(403,"提交人不能审核自己的标准设计");
        if(input.approved())drawings.get(drawingId,input.revision());
        String now=Instant.now().toString(),status=input.approved()?"PUBLISHED":"REJECTED";
        jdbc.update("UPDATE dm_standard_design_version SET status=?,review_note=?,reviewed_by=?,reviewed_at=? WHERE tenant_id=? AND drawing_id=? AND revision=? AND status='SUBMITTED'",
            status,note,actor(),now,tenant(),drawingId,input.revision());
        if(input.approved())jdbc.update("UPDATE dm_standard_design SET status='PUBLISHED',published_revision=?,disabled_by=NULL,disabled_at=NULL,disable_reason=NULL,updated_at=? WHERE tenant_id=? AND drawing_id=?",input.revision(),now,tenant(),drawingId);
        else jdbc.update("UPDATE dm_standard_design SET updated_at=? WHERE tenant_id=? AND drawing_id=?",now,tenant(),drawingId);
        return entry(drawingId,input.revision());
    }
    @Transactional public Entry disable(String drawingId,int revision,String reason){
        allowed("doormes:design:standard-review");id(drawingId);String checkedReason=note(reason);
        var head=jdbc.queryForList("SELECT status,published_revision FROM dm_standard_design WHERE tenant_id=? AND drawing_id=? FOR UPDATE",tenant(),drawingId);
        if(head.isEmpty())throw new ServiceException(404,"标准设计不存在");
        var selected=head.get(0);
        if(!"PUBLISHED".equals(selected.get("status"))||!Objects.equals(selected.get("published_revision"),revision))throw new ServiceException(409,"目录已变化，请刷新后再停用");
        Long pending=jdbc.queryForObject("SELECT COUNT(*) FROM dm_standard_design_version WHERE tenant_id=? AND drawing_id=? AND status='SUBMITTED'",Long.class,tenant(),drawingId);
        if(pending!=null&&pending>0)throw new ServiceException(409,"请先审核待发布版本，再停用标准设计");
        String now=Instant.now().toString();
        jdbc.update("UPDATE dm_standard_design SET status='DISABLED',disabled_by=?,disabled_at=?,disable_reason=?,updated_at=? WHERE tenant_id=? AND drawing_id=?",actor(),now,checkedReason,now,tenant(),drawingId);
        return entry(drawingId,revision);
    }
    @Transactional public DrawingModels.Snapshot reuse(Reuse input){
        allowed("doormes:design:standard-reuse");id(input.drawingId());
        var head=jdbc.queryForList("SELECT status,published_revision FROM dm_standard_design WHERE tenant_id=? AND drawing_id=? FOR UPDATE",tenant(),input.drawingId());
        if(head.isEmpty())throw new ServiceException(404,"标准设计不存在");
        if(!"PUBLISHED".equals(head.get(0).get("status"))||!Objects.equals(head.get(0).get("published_revision"),input.revision()))throw new ServiceException(409,"标准设计已停用或版本已变化，请刷新后再引用");
        return drawings.copyFromStandard(drawings.get(input.drawingId(),input.revision()));
    }
    private String note(String value){if(value==null||value.isBlank()||value.length()>1000)throw new ServiceException(400,"请填写审核或停用原因，最多1000字");return value.trim();}
}
