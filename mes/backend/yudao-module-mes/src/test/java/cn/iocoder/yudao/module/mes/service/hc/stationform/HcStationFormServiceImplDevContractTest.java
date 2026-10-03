package cn.iocoder.yudao.module.mes.service.hc.stationform;

import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormSaveReqVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HcStationFormServiceImplDevContractTest {

    private final HcStationFormServiceImpl service = new HcStationFormServiceImpl();

    @Test
    void rejectDevOnlyFormWithoutDevSuffix() {
        assertThrows(RuntimeException.class, () -> service.createHcStationForm(form(
                "FORMULA_PROCESS_CHECK", "{\"devOnly\":true}")));
    }

    @Test
    void rejectDevSuffixFormWithoutDevOnly() {
        assertThrows(RuntimeException.class, () -> service.createHcStationForm(form(
                "FORMULA_PROCESS_CHECK_DEV", "{\"devOnly\":false}")));
    }

    private HcStationFormSaveReqVO form(String formCode, String schemaJson) {
        HcStationFormSaveReqVO form = new HcStationFormSaveReqVO();
        form.setFormCode(formCode);
        form.setFormName("测试动态表单");
        form.setProcessCode("FORMULA");
        form.setTriggerTimingCode("IN_PROCESS");
        form.setSchemaJson(schemaJson);
        return form;
    }
}
