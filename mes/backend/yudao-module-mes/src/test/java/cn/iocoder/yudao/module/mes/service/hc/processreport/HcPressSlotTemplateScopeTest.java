package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcPressSlotTemplateScopeTest {
    private HcStationFormDO form(long id, String type, String config) {
        var form = new HcStationFormDO();
        form.setId(id);
        form.setFormCode("PRESS_SLOT_" + type + "_" + id);
        form.setSchemaJson(config);
        return form;
    }
    @Test
    void exactPrefixCommonAndNoMatchAcrossAllThreeTypes() {
        var service = new HcProcessReportServiceImpl();
        var mapper = mock(HcStationFormMapper.class);
        ReflectionTestUtils.setField(service, "hcStationFormMapper", mapper);
        for (String type : List.of("INTERMEDIATE_RECORD", "PROCESS_PARAM")) {
            var exact = form(1, type, "{\"modelScope\":\"MODEL\",\"modelCode\":\"W33P0100\",\"modelPrefix\":\"W33P\"}");
            var common = form(2, type, "{\"modelCode\":\"COMMON\"}");
            var prefix = form(3, type, "{\"modelScope\":\"PREFIX\",\"modelPrefix\":\"W33P\"}");
            when(mapper.selectEnabledByProcess("PRESS_SLOT")).thenReturn(List.of(exact, common));
            assertSame(common, ReflectionTestUtils.invokeMethod(service, "resolvePressSlotStationFormConfig", type, "W33P0300"));
            assertSame(exact, ReflectionTestUtils.invokeMethod(service, "resolvePressSlotStationFormConfig", type, "w33p0100 "));
            when(mapper.selectEnabledByProcess("PRESS_SLOT")).thenReturn(List.of(common, prefix, exact));
            assertSame(prefix, ReflectionTestUtils.invokeMethod(service, "resolvePressSlotStationFormConfig", type, "W33P0300"));
            assertSame(exact, ReflectionTestUtils.invokeMethod(service, "resolvePressSlotStationFormConfig", type, "W33P0100"));
            when(mapper.selectEnabledByProcess("PRESS_SLOT")).thenReturn(List.of(exact));
            assertNull(ReflectionTestUtils.invokeMethod(service, "resolvePressSlotStationFormConfig", type, "W33P0300"));
        }
    }
    @Test
    void legacyBlankCommonAndNestedScope() {
        var service = new HcProcessReportServiceImpl();
        assertEquals(20, (Integer) ReflectionTestUtils.invokeMethod(service, "getPressSlotStationFormModelMatchScore", form(1, "PROCESS_PARAM", "{}"), ""));
        var nested = form(2, "PRODUCTION_CHECK", "{\"modelMatch\":{\"scope\":\"MODEL\",\"modelCode\":\"W33P0100\",\"modelPrefix\":\"W33P\"}}");
        assertEquals(-1, (Integer) ReflectionTestUtils.invokeMethod(service, "getPressSlotStationFormModelMatchScore", nested, "W33P0300"));
    }
    @Test
    void adhesive2LegacySeriesFallbackIsUnchanged() {
        var service = new HcProcessReportServiceImpl();
        var exact = form(1, "INTERMEDIATE_RECORD", "{\"modelCode\":\"W33P0100\"}");
        assertEquals(5, (Integer) ReflectionTestUtils.invokeMethod(service, "getLegacyStationFormModelMatchScore", exact, "W33P0300"));
        assertEquals(-1, (Integer) ReflectionTestUtils.invokeMethod(service, "getPressSlotStationFormModelMatchScore", exact, "W33P0300"));
    }
}
