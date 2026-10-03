package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcFinishedPackagingMapperCurrentStockSqlTest {

    @Test
    void shouldUseExecutableCurrentStockConditionsForCountAndPage() {
        assertCurrentStockSql(selectSql("countFgStockLedgerPage"));
        assertCurrentStockSql(selectSql("selectFgStockLedgerPage"));
    }

    private void assertCurrentStockSql(String sql) {
        assertFalse(sql.contains("&lt;"));
        assertFalse(sql.contains("&gt;"));
        assertTrue(sql.contains("fs.stock_status IN ('INBOUNDED', 'AVAILABLE')"));
        assertTrue(sql.contains("fs.location_code IS NOT NULL"));
        assertTrue(sql.contains("fs.location_code != ''"));
        assertTrue(sql.contains("COALESCE(fs.qty, 0) > 0"));
        assertTrue(sql.contains("#{req.keyword}"));
    }

    private String selectSql(String methodName) {
        Method method = Arrays.stream(HcFinishedPackagingMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Select select = method.getAnnotation(Select.class);
        assertNotNull(select);
        return String.join("\n", select.value());
    }
}
