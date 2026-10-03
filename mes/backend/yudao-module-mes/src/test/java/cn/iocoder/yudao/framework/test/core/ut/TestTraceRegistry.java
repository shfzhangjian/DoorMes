package cn.iocoder.yudao.framework.test.core.ut;

import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 测试痕迹注册中心 (SDET 纯净自动化版)
 * 职责：记录测试产生的数据 ID，并在测试结束后自动执行物理擦除。
 * 配合 Debug 断点使用，无需控制台交互。
 */
public class TestTraceRegistry {

    private final Logger log = LoggerFactory.getLogger(TestTraceRegistry.class);
    private final JdbcTemplate jdbcTemplate;

    // 使用 LIFO (后进先出) 栈结构存储，确保先删子表，后删主表
    private final Deque<TraceItem> traceStack = new ConcurrentLinkedDeque<>();

    public TestTraceRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 追踪数据痕迹
     * @param tableName 表名
     * @param id 主键ID
     */
    public void track(String tableName, Long id) {
        if (id != null) {
            traceStack.push(new TraceItem(tableName, id));
        }
    }

    /**
     * 执行自动擦除
     * (在 @AfterEach 中调用)
     */
    public void executeWipe() {
        if (traceStack.isEmpty()) {
            return;
        }

        // 打印快照，方便在 Debug 时查看控制台知道删了什么
        Map<String, List<Long>> snapshot = new HashMap<>();
        for (TraceItem item : traceStack) {
            snapshot.computeIfAbsent(item.tableName, k -> new ArrayList<>()).add(item.id);
        }
        log.info("📝 [TraceRegistry] 本次测试产生的数据快照: {}", JSONUtil.toJsonStr(snapshot));

        log.info("🧹 [TraceRegistry] 开始执行自动清理...");
        while (!traceStack.isEmpty()) {
            TraceItem item = traceStack.pop();
            try {
                String sql = String.format("DELETE FROM %s WHERE id = ?", item.tableName);
                jdbcTemplate.update(sql, item.id);
                log.info("   ✅ Deleted -> Table: {}, ID: {}", item.tableName, item.id);
            } catch (Exception e) {
                log.error("   ❌ Delete Failed -> Table: {}, ID: {}", item.tableName, item.id, e);
            }
        }
    }

    private record TraceItem(String tableName, Long id) {}
}
