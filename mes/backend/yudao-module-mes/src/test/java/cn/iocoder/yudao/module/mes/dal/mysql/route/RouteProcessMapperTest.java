package cn.iocoder.yudao.module.mes.dal.mysql.route;

import cn.iocoder.yudao.framework.test.core.ut.BaseManualTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessMapper;
import cn.iocoder.yudao.module.mes.ut.MesTestConstants;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(RouteProcessMapper.class)
public class RouteProcessMapperTest extends BaseManualTest {

    @Resource
    private RouteProcessMapper routeProcessMapper;

    @BeforeEach
    public void clean() {
        jdbcTemplate.update("DELETE FROM mes_route_process WHERE id = 901");
    }

    @Test
    public void testRelation_Persistence() {
        // 1. 准备数据
        RouteProcessDO relation = RouteProcessDO.builder()
                .id(901L)
                .tenantId(MesTestConstants.TENANT_ID_FILM)
                .routeId(202L)
                .processId(101L)
                .seqNo(10)
                .nextProcessId(0L)
                .build();

        // 2. 插入
        routeProcessMapper.insert(relation);
        track("mes_route_process", relation.getId());

        // 3. 核心断言：验证关联 ID 是否准确
        RouteProcessDO dbRecord = routeProcessMapper.selectById(901L);
        assertNotNull(dbRecord);
        assertEquals(202L, dbRecord.getRouteId());
        assertEquals(101L, dbRecord.getProcessId());
    }
}
