// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/batch/BatchRuleServiceTest.java
package cn.iocoder.yudao.module.mes.service.batch;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.batch.BatchRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.batch.BatchRuleMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import(BatchRuleServiceImpl.class)
public class BatchRuleServiceTest extends BaseMockitoTest {

    @InjectMocks
    private BatchRuleServiceImpl batchRuleService;

    @Mock
    private BatchRuleMapper batchRuleMapper;

    // =========================================================
    // ✅ 用例 A: 按日重置流水号 (T02 压铸场景)
    // =========================================================
    @Test
    @DisplayName("Case A: T02压铸 - 每日流水号重置")
    public void testGenerate_DailyReset() {
        System.out.println("\n========== [开始测试] 场景 A: T02压铸 - 每日流水号重置 ==========");

        // 1. 准备规则
        BatchRuleDO rule = BatchRuleDO.builder()
                .id(1L).productId(102L)
                .ruleCode("R_AUTO_02").ruleType("NEW")
                .prefix("KNK-")
                .dateFmt("yyyyMMdd-")
                .seqLen(3).resetCycle("DAY")
                .currentVal(99)
                .version(1)
                .build();
        rule.setUpdateTime(LocalDateTime.now().minusDays(1)); // 模拟昨天更新

        System.out.println("1. 模拟规则: 前缀=KNK-, 格式=yyyyMMdd-, 当前流水=99, 上次更新=昨天");

        // 2. Mock
        when(batchRuleMapper.selectOne(any(SFunction.class), eq(102L))).thenReturn(rule);
        when(batchRuleMapper.updateById(any(BatchRuleDO.class))).thenReturn(1);

        // 3. 执行
        String batchNo = batchRuleService.generateBatchNo(102L);
        System.out.println("2. 生成结果: " + batchNo);

        // 4. 验证
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expected = "KNK-" + todayStr + "-001";

        System.out.println("3. 预期结果: " + expected);

        assertEquals(expected, batchNo);
        verify(batchRuleMapper).updateById(argThat((BatchRuleDO r) -> {
            boolean isReset = r.getCurrentVal() == 1;
            System.out.println("4. 数据库验证: 流水号是否重置为1? " + (isReset ? "✅ 是" : "❌ 否"));
            return isReset;
        }));
        System.out.println("✅ 场景 A 测试通过！");
    }

    // =========================================================
    // ✅ 用例 B: 父子继承 (T01 薄膜分切)
    // =========================================================
    @Test
    @DisplayName("Case B: T01分切 - 继承父卷批次")
    public void testGenerate_Inherit() {
        System.out.println("\n========== [开始测试] 场景 B: T01分切 - 继承父卷批次 ==========");

        // 1. 准备规则
        BatchRuleDO rule = BatchRuleDO.builder()
                .id(2L).productId(201L)
                .ruleType("INHERIT")
                .separator("-").inheritSuffixLen(2)
                .currentVal(5)
                .seqLen(3) // 必须补全，防止 NPE
                .version(1)
                .build();
        rule.setUpdateTime(LocalDateTime.now());

        System.out.println("1. 模拟规则: 继承模式, 分隔符='-', 后缀长度=2, 当前流水=5");
        String parentBatch = "COAT-20260217-088";
        System.out.println("   父卷批次: " + parentBatch);

        // 2. Mock
        when(batchRuleMapper.selectOne(any(SFunction.class), eq(201L))).thenReturn(rule);
        when(batchRuleMapper.updateById(any(BatchRuleDO.class))).thenReturn(1);

        // 3. 执行
        String childBatch = batchRuleService.generateChildBatchNo(201L, parentBatch);
        System.out.println("2. 生成结果: " + childBatch);

        // 4. 验证
        String expected = "COAT-20260217-088-06"; // 5 + 1 = 6
        System.out.println("3. 预期结果: " + expected);

        assertEquals(expected, childBatch);
        System.out.println("✅ 场景 B 测试通过！");
    }

    // =========================================================
    // ✅ 用例 C: 并发乐观锁重试 (Advanced)
    // =========================================================
    @Test
    @DisplayName("Case C: 并发冲突 - 触发乐观锁重试")
    public void testGenerate_OptimisticLockRetry() {
        System.out.println("\n========== [开始测试] 场景 C: 并发冲突 - 触发乐观锁重试 ==========");

        // 1. 准备规则
        BatchRuleDO rule = BatchRuleDO.builder()
                .id(3L).productId(303L).prefix("TEST-").seqLen(3)
                .currentVal(10)
                .version(1)
                .build();
        rule.setUpdateTime(LocalDateTime.now());

        System.out.println("1. 模拟规则: 前缀=TEST-, 当前流水=10, Version=1");

        // 2. Mock
        when(batchRuleMapper.selectOne(any(SFunction.class), eq(303L))).thenReturn(rule);

        // Mock 第一次更新失败 (模拟并发)，第二次成功
        when(batchRuleMapper.updateById(any(BatchRuleDO.class)))
                .thenAnswer(invocation -> {
                    System.out.println("   [模拟] 第1次尝试更新... 💥 失败 (Version不匹配)");
                    return 0;
                })
                .thenAnswer(invocation -> {
                    System.out.println("   [模拟] 第2次尝试更新... ✅ 成功");
                    return 1;
                });

        // 3. 执行
        System.out.println("2. 开始执行生成逻辑...");
        String batchNo = batchRuleService.generateBatchNo(303L);
        System.out.println("3. 最终生成结果: " + batchNo);

        // 4. 验证
        verify(batchRuleMapper, times(2)).updateById(any(BatchRuleDO.class));
        System.out.println("4. 验证: 数据库 Update 方法被调用次数 = 2 (符合预期)");

        assertTrue(batchNo.startsWith("TEST-"));
        System.out.println("✅ 场景 C 测试通过！");
    }
}
