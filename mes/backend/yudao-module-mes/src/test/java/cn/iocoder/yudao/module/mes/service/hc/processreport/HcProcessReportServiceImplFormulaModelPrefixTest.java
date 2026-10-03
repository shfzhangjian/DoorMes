package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcProcessReportServiceImplFormulaModelPrefixTest {

    private final HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();

    @Test
    void shouldKeepCompleteProductModelCode() throws Exception {
        HcPlanOrderDO planOrder = HcPlanOrderDO.builder()
                .motherModelCode("W26P0100")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .motherModelCode(" ra12p0100 ")
                .build();

        assertEquals("RA12P0100", resolveProductModelCode(planOrder, operation));
    }

    @Test
    void shouldMatchProductionFormByVariableLengthPrefix() throws Exception {
        HcStationFormDO raForm = productionForm("RA");
        HcStationFormDO w26pForm = productionForm("W26P");

        assertTrue(matches(raForm, "RA"));
        assertTrue(matches(raForm, "RA12P0100"));
        assertFalse(matches(raForm, "RX21"));
        assertTrue(matches(w26pForm, "W26P0100"));
        assertFalse(matches(w26pForm, "W33P0100"));
    }

    @Test
    void shouldNotTruncateExplicitLongPrefix() throws Exception {
        HcStationFormDO exactModelForm = productionForm("W26P0100");

        assertTrue(matches(exactModelForm, "W26P0100"));
        assertFalse(matches(exactModelForm, "W26P0200"));
    }

    @Test
    void shouldKeepLegacyFourCharacterFallbackWithoutConfiguredPrefix() throws Exception {
        HcStationFormDO legacyForm = HcStationFormDO.builder()
                .formCode("FORMULA_PROCESS_CHECK_W26P")
                .formName("配料生产点检表")
                .schemaJson("{\"formulaCategory\":\"production-check\"}")
                .build();

        assertTrue(matches(legacyForm, "W26P0100"));
        assertFalse(matches(legacyForm, "W33P0100"));
    }

    private HcStationFormDO productionForm(String modelPrefix) {
        return HcStationFormDO.builder()
                .formCode("FORMULA_PROCESS_CHECK_C12P0100")
                .formName("CMP软垫（C12P0100）配料生产点检表")
                .schemaJson(String.format(
                        "{\"formulaCategory\":\"production-check\",\"modelPrefix\":\"%s\"}", modelPrefix))
                .build();
    }

    private String resolveProductModelCode(HcPlanOrderDO planOrder,
                                           HcPlanOrderOperationDO operation) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "resolveFormulaProductModelCode", HcPlanOrderDO.class, HcPlanOrderOperationDO.class);
        method.setAccessible(true);
        return (String) method.invoke(service, planOrder, operation);
    }

    private boolean matches(HcStationFormDO form, String modelCode) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "matchFormulaStationFormModelPrefix", HcStationFormDO.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, form, modelCode);
    }
}
