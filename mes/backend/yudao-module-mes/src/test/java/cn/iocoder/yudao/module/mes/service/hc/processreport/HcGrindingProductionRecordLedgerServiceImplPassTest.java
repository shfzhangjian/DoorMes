package cn.iocoder.yudao.module.mes.service.hc.processreport;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HcGrindingProductionRecordLedgerServiceImplPassTest {

    @Test
    void shouldNormalizeThirdAndFourthGrindingPass() throws Exception {
        assertEquals("THIRD", normalizePass("三次磨皮"));
        assertEquals("THIRD", normalizePass("THIRD"));
        assertEquals("FOURTH", normalizePass("四磨"));
        assertEquals("FOURTH", normalizePass("FOURTH"));
        assertNull(normalizePass("第五次磨皮"));
    }

    @Test
    void shouldBuildDisplayNameForAllSupportedGrindingPasses() throws Exception {
        assertEquals("一次", passName("FIRST"));
        assertEquals("二次", passName("SECOND"));
        assertEquals("三次", passName("THIRD"));
        assertEquals("四次", passName("FOURTH"));
    }

    private String normalizePass(String value) throws Exception {
        Method method = HcGrindingProductionRecordLedgerServiceImpl.class
                .getDeclaredMethod("normalizePass", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, value);
    }

    private String passName(String value) throws Exception {
        Method method = HcGrindingProductionRecordLedgerServiceImpl.class
                .getDeclaredMethod("passName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, value);
    }

}
