package cn.iocoder.yudao.module.mes.dal.mysql.material;

import cn.iocoder.yudao.framework.test.core.ut.BaseManualTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.ut.factory.MaterialDataFactory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(MesMaterialMapper.class)
public class MesMaterialMapperTest extends BaseManualTest {

    @Resource
    private MesMaterialMapper mesMaterialMapper;

    @BeforeEach
    public void clean() {
        // 清理可能冲突的数据
        jdbcTemplate.update("DELETE FROM mes_material WHERE id IN (1001, 1002, 2001)");
    }

    @Test
    public void testInsert_Persistence_Check() {
        // 1. 准备数据 (铝锭 - T02)
        MesMaterialDO material = MaterialDataFactory.buildMaterialAlIngot();

        // 2. 执行插入
        mesMaterialMapper.insert(material);
        track("mes_material", material.getId()); // 注册清理

        // 3. 核心断言：验证数据保真度
        MesMaterialDO dbRecord = mesMaterialMapper.selectById(material.getId());
        assertNotNull(dbRecord);

        // 目标1: 验证反范式字段 (SupplierName)
        assertEquals("南山铝业", dbRecord.getSupplierName(), "反范式字段 SupplierName 丢失");

        // 目标2: 验证多租户拦截器 (TenantId)
        // 注意：BaseManualTest 模拟了 TenantContext，这里验证 DB 里的值是否正确
        assertEquals(1L, dbRecord.getTenantId(), "多租户 ID 插入错误");
    }
}
