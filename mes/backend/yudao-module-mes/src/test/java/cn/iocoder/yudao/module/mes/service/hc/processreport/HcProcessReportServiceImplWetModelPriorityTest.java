package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HcProcessReportServiceImplWetModelPriorityTest {

    private final HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();

    @Test
    void shouldPreferExactWetModelWithinSolidSlotButKeepOvenPrefixFallback() throws Exception {
        HcStationFormDO w26pSolidPrefix = wetForm("WET_SOLID_SEMI", "solid", "W26P0100", "W26P");
        HcStationFormDO w26p0200SolidExact = wetForm(
                "WET_SOLID_SEMI_COPY_20260908093343", "solid", "W26P0200", "");
        HcStationFormDO w26pOvenPrefix = wetForm("WET_OVEN_SEMI", "oven", "", "W26P");
        HcStationFormDO w26pProductionPrefix = wetForm("WET_PROCESS_CHECK", "", "", "W26P");

        assertEquals(List.of(
                        "WET_PROCESS_CHECK",
                        "WET_SOLID_SEMI_COPY_20260908093343",
                        "WET_OVEN_SEMI"),
                formCodes(select("W26P0200", w26pProductionPrefix, w26pSolidPrefix,
                        w26p0200SolidExact, w26pOvenPrefix)));
    }

    @Test
    void shouldUsePrefixTemplateWhenNoExactModelExists() throws Exception {
        HcStationFormDO w26pSolidPrefix = wetForm("WET_SOLID_SEMI", "solid", "W26P0100", "W26P");
        HcStationFormDO w26p0200SolidExact = wetForm(
                "WET_SOLID_SEMI_COPY_20260908093343", "solid", "W26P0200", "");

        assertEquals(List.of("WET_SOLID_SEMI"),
                formCodes(select("W26P0300", w26pSolidPrefix, w26p0200SolidExact)));
    }

    private HcStationFormDO wetForm(String formCode, String semiType, String modelCode, String modelPrefix) {
        String category = formCode.startsWith("WET_PROCESS_CHECK") ? "production-check" : "semi-finished";
        return HcStationFormDO.builder()
                .formCode(formCode)
                .formName(formCode)
                .schemaJson(String.format(
                        "{\"wetCategory\":\"%s\",\"semiType\":\"%s\",\"modelCode\":\"%s\",\"modelPrefix\":\"%s\"}",
                        category, semiType, modelCode, modelPrefix))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<HcStationFormDO> select(String modelCode, HcStationFormDO... forms) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "selectWetStationFormsByModel", List.class, String.class);
        method.setAccessible(true);
        return (List<HcStationFormDO>) method.invoke(service, List.of(forms), modelCode);
    }

    private List<String> formCodes(List<HcStationFormDO> forms) {
        return forms.stream().map(HcStationFormDO::getFormCode).collect(Collectors.toList());
    }
}
