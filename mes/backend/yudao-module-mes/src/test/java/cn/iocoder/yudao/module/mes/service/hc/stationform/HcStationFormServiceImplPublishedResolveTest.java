package cn.iocoder.yudao.module.mes.service.hc.stationform;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcStationFormServiceImplPublishedResolveTest {

    @InjectMocks
    private HcStationFormServiceImpl service;
    @Mock
    private HcStationFormMapper hcStationFormMapper;

    @Test
    void shouldResolveLegacyPrefixStoredInModelCodeBeforeCommonFallback() {
        HcStationFormDO generic = form(1L, "ROUGH_PROCESS_CHECK_FIRST", 99,
                "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                        + "\"grindingPass\":\"FIRST\",\"modelScope\":\"COMMON\","
                        + "\"modelCode\":null,\"allowCommonFallback\":true}");
        HcStationFormDO legacyPrefix = form(2L, "ROUGH_PROCESS_CHECK_FIRST_W26P0100", 32,
                "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                        + "\"grindingPass\":\"FIRST\",\"modelScope\":\"PREFIX\","
                        + "\"modelCode\":\"W26P\"}");
        when(hcStationFormMapper.selectEnabledByProcess("ROUGH_GRINDING"))
                .thenReturn(List.of(generic, legacyPrefix));

        HcStationFormDO resolved = service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W26P0200", "PROCESS_PARAM", "FIRST");

        assertEquals(legacyPrefix.getId(), resolved.getId());
    }

    @Test
    void shouldIgnoreJsonNullAndResolveExplicitCommonFallback() {
        HcStationFormDO generic = form(3L, "ROUGH_PROCESS_CHECK_FIRST", 99,
                "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                        + "\"grindingPass\":\"FIRST\",\"modelScope\":\"COMMON\","
                        + "\"modelCode\":null,\"allowCommonFallback\":true}");
        when(hcStationFormMapper.selectEnabledByProcess("ROUGH_GRINDING")).thenReturn(List.of(generic));

        HcStationFormDO resolved = service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W99P0001", "PROCESS_PARAM", "FIRST");

        assertEquals(generic.getId(), resolved.getId());
    }

    @Test
    void shouldPreferExactModelOverPrefixAndCommonFallback() {
        HcStationFormDO generic = form(4L, "ROUGH_PROCESS_CHECK_FIRST", 1,
                "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                        + "\"grindingPass\":\"FIRST\",\"modelScope\":\"COMMON\","
                        + "\"modelCode\":\"COMMON\",\"allowCommonFallback\":true}");
        HcStationFormDO prefix = form(5L, "ROUGH_PROCESS_CHECK_FIRST_W26P", 32,
                "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                        + "\"grindingPass\":\"FIRST\",\"modelScope\":\"PREFIX\","
                        + "\"modelPrefix\":\"W26P\"}");
        HcStationFormDO exact = form(6L, "ROUGH_PROCESS_CHECK_FIRST_W26P0200", 33,
                "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                        + "\"grindingPass\":\"FIRST\",\"modelScope\":\"MODEL\","
                        + "\"modelCode\":\"W26P0200\"}");
        when(hcStationFormMapper.selectEnabledByProcess("ROUGH_GRINDING"))
                .thenReturn(List.of(generic, prefix, exact));

        HcStationFormDO resolved = service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W26P0200", "PROCESS_PARAM", "FIRST");

        assertEquals(exact.getId(), resolved.getId());
    }

    @Test
    void shouldResolveRenamedCommonFormWithLegacyModelCodeAndKeepGuards() {
        String schema = "{\"published\":true,\"devOnly\":false,\"formType\":\"PROCESS_PARAM\","
                + "\"grindingPass\":\"FIRST\",\"modelScope\":\"COMMON\","
                + "\"modelCode\":\"COMMON\",\"allowCommonFallback\":true}";
        HcStationFormDO generic = form(7L, "ROUGH_PROCESS_CHECK_FIRST", 30, schema);
        generic.setFormName("CMP软垫通用一次磨皮工艺参数点检表");
        when(hcStationFormMapper.selectEnabledByProcess("ROUGH_GRINDING")).thenReturn(List.of(generic));
        assertEquals(generic.getId(), service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W99P0001", "PROCESS_PARAM", "FIRST").getId());
        assertNull(service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W99P0001", "PROCESS_PARAM", "SECOND"));
        assertNull(service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W99P0001", "OTHER", "FIRST"));
        for (String field : List.of("published", "allowCommonFallback")) {
            generic.setSchemaJson(schema.replace("\"" + field + "\":true", "\"" + field + "\":false"));
            assertNull(service.resolvePublishedHcStationForm(
                    "ROUGH_GRINDING", "W99P0001", "PROCESS_PARAM", "FIRST"), field);
        }
        generic.setSchemaJson(schema.replace("\"devOnly\":false", "\"devOnly\":true"));
        assertNull(service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W99P0001", "PROCESS_PARAM", "FIRST"));
        generic.setSchemaJson(schema);
        generic.setFormCode("ROUGH_PROCESS_CHECK_DEV");
        assertNull(service.resolvePublishedHcStationForm(
                "ROUGH_GRINDING", "W99P0001", "PROCESS_PARAM", "FIRST"));
    }

    private HcStationFormDO form(Long id, String formCode, Integer sortNo, String schemaJson) {
        return HcStationFormDO.builder()
                .id(id)
                .formCode(formCode)
                .processCode("ROUGH_GRINDING")
                .sortNo(sortNo)
                .status(1)
                .schemaJson(schemaJson)
                .build();
    }
}
