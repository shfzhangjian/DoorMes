package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcCutRoundConsoleServiceImplConsumableLifetimeTest {

    private final HcCutRoundConsoleServiceImpl service = new HcCutRoundConsoleServiceImpl();

    @Test
    void shouldIgnoreBladeLifetimeDays() throws Exception {
        HcCutRoundSpareDO blade = HcCutRoundSpareDO.builder()
                .spareType("CUTTING_BLADE")
                .useCount(1)
                .limitCount(5000)
                .limitDays(180)
                .lastReplaceTime(LocalDateTime.now().minusDays(181))
                .build();

        assertFalse(isConsumableNeedReplace(blade));
        assertFalse(isConsumableNeedReminder(blade));
    }

    @Test
    void shouldKeepFeltLifetimeDays() throws Exception {
        HcCutRoundSpareDO felt = HcCutRoundSpareDO.builder()
                .spareType("CUTTING_FELT")
                .useCount(1)
                .limitCount(2000)
                .limitDays(90)
                .lastReplaceTime(LocalDateTime.now().minusDays(90))
                .build();

        assertTrue(isConsumableNeedReplace(felt));
    }

    private boolean isConsumableNeedReplace(HcCutRoundSpareDO state) throws Exception {
        Method method = HcCutRoundConsoleServiceImpl.class.getDeclaredMethod(
                "isConsumableNeedReplace", HcCutRoundSpareDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, state);
    }

    private boolean isConsumableNeedReminder(HcCutRoundSpareDO state) throws Exception {
        Method method = HcCutRoundConsoleServiceImpl.class.getDeclaredMethod(
                "isConsumableNeedReminder", HcCutRoundSpareDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, state);
    }
}
