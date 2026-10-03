package cn.iocoder.yudao.module.mes.dal.mysql.process;

import cn.iocoder.yudao.framework.test.core.ut.BaseManualTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.ut.factory.ProcessDataFactory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(ProcessMapper.class)
public class ProcessMapperTest extends BaseManualTest {

    @Resource
    private ProcessMapper processMapper;

    @BeforeEach
    public void clean() {
        jdbcTemplate.update("DELETE FROM mes_process WHERE id = 101");
    }

    @Test
    public void testMapping_BooleanAndRedundancy() {
        // 1. 准备数据 (模切 - T01)
        ProcessDO process = ProcessDataFactory.buildProcessDieCut();

        // 2. 插入
        processMapper.insert(process);
        track("mes_process", process.getId());

        // 3. 核心断言
        ProcessDO dbRecord = processMapper.selectById(process.getId());

        // 目标1: Boolean 映射 (Java Boolean <-> DB tinyint)
        assertTrue(dbRecord.getBindStation(), "Boolean 字段 bindStation 映射失败");

        // 目标2: 车间冗余字段
        assertEquals("WS_DIE_01", dbRecord.getWorkshopCode());
        assertEquals("模切一车间", dbRecord.getWorkshopName());
    }
}
