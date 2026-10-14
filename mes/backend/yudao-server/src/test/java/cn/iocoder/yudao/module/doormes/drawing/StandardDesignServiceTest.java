package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StandardDesignServiceTest {
    private static final String ID="52d5ecce-031c-419b-bb7b-4d183bc55c8c";
    private final AtomicLong actor=new AtomicLong(1003);
    private JdbcTemplate jdbc;
    private DrawingService drawings;
    private PermissionService permissions;
    private StandardDesignService standards;
    private MockedStatic<SecurityFrameworkUtils> security;

    @BeforeEach void setup(){
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE dm_standard_design(tenant_id BIGINT,drawing_id CHAR(36),published_revision INT,status VARCHAR(20),disabled_by BIGINT,disabled_at VARCHAR(40),disable_reason VARCHAR(1000),updated_at VARCHAR(40),PRIMARY KEY(tenant_id,drawing_id))");
        jdbc.execute("CREATE TABLE dm_standard_design_version(tenant_id BIGINT,drawing_id CHAR(36),revision INT,status VARCHAR(20),number VARCHAR(60),name VARCHAR(200),note VARCHAR(1000),review_note VARCHAR(1000),submitted_by BIGINT,reviewed_by BIGINT,submitted_at VARCHAR(40),reviewed_at VARCHAR(40),PRIMARY KEY(tenant_id,drawing_id,revision))");
        drawings=mock(DrawingService.class);permissions=mock(PermissionService.class);
        when(permissions.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        when(drawings.get(eq(ID),anyInt())).thenAnswer(invocation->snapshot(invocation.getArgument(1)));
        when(drawings.copyFromStandard(any())).thenAnswer(invocation->invocation.getArgument(0));
        standards=new StandardDesignService(jdbc,drawings,permissions);
        security=mockStatic(SecurityFrameworkUtils.class);security.when(SecurityFrameworkUtils::getLoginUserId).thenAnswer(invocation->actor.get());
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void clear(){security.close();TenantContextHolder.clear();}
    private DrawingModels.Snapshot snapshot(int revision){
        return new DrawingModels.Snapshot(DrawingModels.SCHEMA,ID,1,null,0,null,revision,"DRAFT",1003,1003,"2026-10-03T00:00:00Z","2026-10-03T00:00:00Z","test",null,
            new DrawingModels.Metadata("STD-001","标准窗","设计备注","INDEPENDENT"),false);
    }
    private int code(Runnable action){return assertThrows(ServiceException.class,action::run).getCode();}
    private StandardDesignService.Entry submit(int revision){return standards.submit(new StandardDesignService.Submit(ID,revision));}
    private StandardDesignService.Entry review(int revision,boolean approved){return standards.review(ID,new StandardDesignService.Decision(revision,approved,"审核意见"));}

    @Test void submitApproveReuseAndDisableKeepFrozenVersionAndAudit(){
        assertEquals("SUBMITTED",submit(2).status());
        assertEquals(0,standards.page(true,"",1,20).total());
        assertEquals(403,code(()->review(2,true)));
        actor.set(1007);
        var published=review(2,true);
        assertEquals("PUBLISHED",published.status());assertTrue(published.active());
        assertEquals("设计备注",published.note());assertEquals("审核意见",published.reviewNote());
        assertEquals(1,standards.page(true,"STD",1,20).total());
        actor.set(1009);
        var reused=standards.reuse(new StandardDesignService.Reuse(ID,2));
        assertEquals(2,reused.revision());verify(drawings).copyFromStandard(any());
        actor.set(1007);
        var disabled=standards.disable(ID,2,"型号失效");
        assertFalse(disabled.active());assertEquals("型号失效",disabled.disableReason());
        assertEquals(0,standards.page(true,"",1,20).total());
        actor.set(1009);
        assertEquals(409,code(()->standards.reuse(new StandardDesignService.Reuse(ID,2))));
    }

    @Test void pendingNewVersionDoesNotReplacePublishedUntilReview(){
        submit(1);actor.set(1007);review(1,true);
        actor.set(1003);submit(2);
        assertEquals(409,code(()->submit(3)));
        assertEquals(409,code(()->submit(1)));
        assertEquals(1,standards.page(true,"",1,20).list().get(0).revision());
        actor.set(1007);review(2,false);
        assertEquals(1,standards.page(true,"",1,20).list().get(0).revision());
        actor.set(1003);submit(3);
        actor.set(1007);review(3,true);
        assertEquals(3,standards.page(true,"",1,20).list().get(0).revision());
        assertEquals(409,code(()->standards.reuse(new StandardDesignService.Reuse(ID,1))));
        assertEquals(3,standards.page(false,"",1,20).total());
    }

    @Test void wrongOwnerAndTenantCannotPublishOrReuse(){
        actor.set(1009);assertEquals(403,code(()->submit(1)));
        actor.set(1003);submit(1);
        TenantContextHolder.setTenantId(2L);
        assertEquals(0,standards.page(false,"",1,20).total());
        actor.set(1007);assertEquals(404,code(()->review(1,true)));
        assertEquals(404,code(()->standards.reuse(new StandardDesignService.Reuse(ID,1))));
    }

    @Test void permissionsAndStaleDecisionsAreRejected(){
        submit(1);actor.set(1007);
        when(permissions.hasAnyPermissions(eq(1007L),any(String[].class))).thenReturn(false);
        assertEquals(403,code(()->review(1,true)));
        when(permissions.hasAnyPermissions(eq(1007L),any(String[].class))).thenReturn(true);
        review(1,true);
        assertEquals(409,code(()->review(1,true)));
        assertEquals(400,code(()->standards.disable(ID,1," ")));
        assertEquals(409,code(()->standards.disable(ID,0,"停用")));
    }
}
