package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotScanGateRespVO;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HcProcessReportServiceImplPressSlotReminderTest {
    private HcPressSlotScanGateRespVO gate(long count, int threshold) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "buildPressSlotContinuousCheckReminder", long.class, int.class);
        method.setAccessible(true);
        return (HcPressSlotScanGateRespVO) method.invoke(new HcProcessReportServiceImpl(), count, threshold);
    }
    @Test void belowTwentyAllowsWithoutReminder() throws Exception {
        var result = gate(19, 25);
        assertTrue(result.getAllowScan());
        assertNull(result.getWarningMessage());
    }
    @Test void twentyRemindsBeforeConfiguredThreshold() throws Exception {
        var result = gate(20, 25);
        assertTrue(result.getAllowScan());
        assertTrue(result.getWarningMessage().contains("20 片过程加检提醒"));
        assertFalse(result.getWarningMessage().contains("阻断"));
    }
    @Test void atAndBeyondThresholdAlwaysAllowsAndReminds() throws Exception {
        for (long count : new long[]{25, 26, 100}) {
            var result = gate(count, 25);
            assertTrue(result.getAllowScan());
            assertEquals(Long.valueOf(count), result.getConfirmedAfterInspectionCount());
            assertEquals(Integer.valueOf(25), result.getBlockThresholdCount());
            assertTrue(result.getWarningMessage().contains("本次允许继续报工"));
        }
    }
    @Test void defaultAndLowerBomThresholdOnlyRemind() throws Exception {
        for (int threshold : new int[]{10, 20}) {
            var result = gate(threshold, threshold);
            assertTrue(result.getAllowScan());
            assertNotNull(result.getWarningMessage());
        }
        assertNull(gate(0, 25).getWarningMessage());
    }
}
