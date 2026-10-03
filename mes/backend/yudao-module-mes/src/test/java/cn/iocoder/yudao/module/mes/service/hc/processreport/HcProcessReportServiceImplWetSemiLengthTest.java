package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkItemReqVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HcProcessReportServiceImplWetSemiLengthTest {

    private final HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();

    @Test
    void shouldExcludeMarkedLengthFromWetSemiThicknessAverage() throws Exception {
        HcWetPassWorkItemReqVO item = createItem("LENGTH_M");

        BigDecimal result = calculateWetSemiThickness(List.of(item));

        assertEquals(0, new BigDecimal("1.25").compareTo(result));
    }

    @Test
    void shouldKeepUnmarkedSecondValueInWetSemiThicknessAverage() throws Exception {
        HcWetPassWorkItemReqVO item = createItem("");

        BigDecimal result = calculateWetSemiThickness(List.of(item));

        assertEquals(0, new BigDecimal("20.8").compareTo(result));
    }

    private HcWetPassWorkItemReqVO createItem(String dualLabel2) {
        HcWetPassWorkItemReqVO item = new HcWetPassWorkItemReqVO();
        item.setItem("1.1");
        item.setStandard("1.2");
        item.setActualValue("1.3");
        item.setNode("1.4");
        item.setActualValue2("99");
        item.setDualLabel2(dualLabel2);
        return item;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal calculateWetSemiThickness(List<HcWetPassWorkItemReqVO> items) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "calculateWetSemiThickness", List.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, items);
    }

}
