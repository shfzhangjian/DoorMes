package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcProductionRecorderNameResolverTest {
    @Mock private AdminUserMapper adminUserMapper;
    @InjectMocks private HcProductionRecorderNameResolver resolver;

    @Test
    void shouldCanonicalizeNewReportAndPreserveUnknownName() {
        AdminUserDO user = new AdminUserDO();
        user.setUsername("heliang");
        user.setNickname("何亮");
        when(adminUserMapper.selectByUsername("heliang")).thenReturn(user);
        assertEquals("何亮", resolver.resolveForSave(" heliang "));
        assertEquals("外部人员", resolver.resolveForSave("外部人员"));
        assertNull(resolver.resolveForSave(null));
    }

    @Test
    void shouldDeduplicateAliasesAndKeepOtherPeople() {
        var names = new HcProductionRecorderNameResolver.Names(Map.of("heliang", "何亮"));
        assertEquals("何亮；张三；旧账号", names.normalize("heliang；何亮;张三；旧账号"));
        assertTrue(names.matches("何亮；张三", "HELI"));
        assertTrue(names.matches("何亮", "何"));
        assertFalse(names.matches("张三", "heliang"));
        assertFalse(names.matches(null, "heliang"));
        assertTrue(names.matches(null, ""));
    }
}
