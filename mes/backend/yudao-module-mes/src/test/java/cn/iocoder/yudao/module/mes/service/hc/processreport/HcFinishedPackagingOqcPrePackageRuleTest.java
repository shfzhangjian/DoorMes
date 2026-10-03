package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcFinishedPackagingOqcPrePackageRuleTest {

    private final HcFinishedPackagingServiceImpl service = new HcFinishedPackagingServiceImpl();

    @Test
    void shouldNotTreatInheritedStockPackageNoAsShippingOuterPackage() throws Exception {
        HcFgShippingNoticeItemDO item = HcFgShippingNoticeItemDO.builder()
                .outerBoxNo("20260819-08")
                .packageNo("20260819-08")
                .build();

        assertFalse(hasShippingOuterPackageTrace(item));
    }

    @Test
    void shouldRecognizeActualShippingOuterPackageTrace() throws Exception {
        HcFgShippingNoticeItemDO packageTimeItem = HcFgShippingNoticeItemDO.builder()
                .shippingPackageTime(LocalDateTime.of(2026, 8, 26, 10, 0))
                .build();
        HcFgShippingNoticeItemDO packageOperatorItem = HcFgShippingNoticeItemDO.builder()
                .shippingPackageName("包装员")
                .build();

        assertTrue(hasShippingOuterPackageTrace(packageTimeItem));
        assertTrue(hasShippingOuterPackageTrace(packageOperatorItem));
    }

    private boolean hasShippingOuterPackageTrace(HcFgShippingNoticeItemDO item) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod(
                "hasShippingOuterPackageTrace", HcFgShippingNoticeItemDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, item);
    }
}
