package cn.iocoder.yudao.module.mes.service.hc.stationform;

import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HcStationFormMultiFieldsTest {
    private final HcStationFormServiceImpl service = new HcStationFormServiceImpl();
    private HcStationFormSaveReqVO form() {
        HcStationFormSaveReqVO form = new HcStationFormSaveReqVO();
        form.setProcessCode("FORMULA"); form.setFormCode("FORMULA_PROCESS_CHECK_TEST"); form.setFormName("配料生产点检表");
        form.setSchemaJson("{}");
        HcStationFormItemSaveReqVO item = new HcStationFormItemSaveReqVO();
        item.setItemSeq(1); item.setItemName("原料加入"); item.setValueMode("MULTI_FIELDS");
        item.setFieldDefinitionsJson("[{\"key\":\"batch\",\"label\":\"批号\",\"type\":\"TEXT\",\"unit\":\"\",\"required\":true}]");
        form.setItems(List.of(item)); return form;
    }
    @Test void previewExcelRoundTripPreservesDefinitionsAndMode() throws Exception {
        HcStationFormSaveReqVO source = form();
        byte[] bytes = service.exportPreviewExcel(source);
        HcStationFormImportRespVO imported = service.importPreviewExcel(new MockMultipartFile("file", "配置.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes), "{}");
        assertEquals(1, imported.getSuccessCount());
        assertEquals("MULTI_FIELDS", imported.getForm().getItems().get(0).getValueMode());
        assertEquals(source.getItems().get(0).getFieldDefinitionsJson(), imported.getForm().getItems().get(0).getFieldDefinitionsJson());
    }
    @Test void onlyFormulaProductionAllowsMultiFields() {
        HcStationFormSaveReqVO source = form();
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "normalizeSaveForm", source));
        source.setProcessCode("WET");
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service, "normalizeSaveForm", source));
        source.setProcessCode("FORMULA"); source.setFormCode("FORMULA_STARTUP_CHECK"); source.setFormName("配料开机点检表");
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service, "normalizeSaveForm", source));
    }
}
