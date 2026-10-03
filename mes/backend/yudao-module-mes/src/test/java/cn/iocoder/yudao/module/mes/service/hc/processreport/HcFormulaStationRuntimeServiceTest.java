package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.*;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HcFormulaStationRuntimeServiceTest {
    private final HcFormulaStationRuntimeService service = new HcFormulaStationRuntimeService();
    private final HcStationFormMapper formMapper = mock(HcStationFormMapper.class);
    private final HcStationFormItemMapper formItems = mock(HcStationFormItemMapper.class);
    private final HcStationRecordMapper recordMapper = mock(HcStationRecordMapper.class);
    private final HcStationRecordItemMapper recordItems = mock(HcStationRecordItemMapper.class);
    private final HcPlanOrderDO plan = HcPlanOrderDO.builder().id(1L).planNo("P1").tenantId(1L).build();
    private final HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder().id(11L).planId(1L).opName("配料").operationStatus("RUNNING").build();
    private final List<HcStationFormDO> forms = new ArrayList<>();
    private final List<HcStationRecordDO> records = new ArrayList<>();
    private final Map<Long, List<HcStationRecordItemDO>> values = new HashMap<>();
    private final Map<Long, HcStationFormItemDO> templates = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ReflectionTestUtils.setField(service, "formMapper", formMapper);
        ReflectionTestUtils.setField(service, "formItemMapper", formItems);
        ReflectionTestUtils.setField(service, "recordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "recordItemMapper", recordItems);
        forms.add(form(1L, "FORMULA_STARTUP_CHECK", "{}"));
        forms.add(form(2L, "FORMULA_CLEANING_CHECK", "{}"));
        forms.add(form(3L, "FORMULA_PROCESS_CHECK", "{\"modelPrefix\":\"W26P\",\"formulaInputModeVersion\":2}"));
        for (HcStationFormDO form : forms) templates.put(form.getId(), HcStationFormItemDO.builder().formId(form.getId()).itemSeq(1)
                .itemName("加入原料").standardText("原标准").valueMode("DUAL_TEXT").requiredFlag(true).defaultResult("OK").build());
        when(formMapper.selectEnabledByProcess("FORMULA")).thenAnswer(i -> forms);
        when(formItems.selectByFormIds(anyCollection())).thenAnswer(i -> ((Collection<Long>) i.getArgument(0)).stream().map(templates::get).toList());
        when(recordMapper.selectList(any(Wrapper.class))).thenAnswer(i -> new ArrayList<>(records));
        when(recordMapper.selectById(anyLong())).thenAnswer(i -> records.stream().filter(r -> r.getId().equals(i.getArgument(0))).findFirst().orElse(null));
        when(recordMapper.insert(any(HcStationRecordDO.class))).thenAnswer(i -> { HcStationRecordDO r = i.getArgument(0); r.setId(100L + records.size()); records.add(r); return 1; });
        when(recordItems.selectByRecordIds(anyCollection())).thenAnswer(i -> ((Collection<Long>) i.getArgument(0)).stream().flatMap(id -> values.getOrDefault(id, List.of()).stream()).toList());
    }

    private HcStationFormDO form(Long id, String code, String schema) {
        return HcStationFormDO.builder().id(id).formCode(code).formName(code).processCode("FORMULA").status(1).needConfirm(true).schemaJson(schema).build();
    }
    private HcFormulaPassWorkSaveReqVO request(String code, String first, String second) {
        HcFormulaPassWorkSaveReqVO req = new HcFormulaPassWorkSaveReqVO();
        req.setPlanId(1L); req.setPlanOperationId(11L); req.setFormCode(code);
        HcFormulaPassWorkItemReqVO item = new HcFormulaPassWorkItemReqVO();
        item.setItemSeq(1); item.setItem("伪造名称"); item.setStandard("伪造标准"); item.setValueMode("TEXT");
        item.setActualValue(first); item.setActualValue2(second); item.setStatus("OK");
        req.setDetails(List.of(item)); return req;
    }
    private HcStationRecordDO save(HcFormulaPassWorkSaveReqVO req, boolean confirm) {
        HcStationRecordDO r = service.prepare(plan, operation, "W26P0200", req, confirm);
        values.put(r.getId(), req.getDetails().stream().map(i -> HcStationRecordItemDO.builder().recordId(r.getId()).itemSeq(i.getItemSeq())
                .itemName(i.getItem()).standardText(i.getStandard()).valueMode(i.getValueMode()).actualValue(i.getActualValue())
                .actualValue2(i.getActualValue2()).fieldDefinitionsJson(i.getFieldDefinitionsJson()).fieldValuesJson(i.getFieldValuesJson()).resultFlag(i.getStatus()).build()).toList());
        return r;
    }
    private HcFormulaPassWorkRespVO production() { return service.list(plan, operation, "W26P0200").stream().filter(r -> "production".equals(r.getFormulaCategory())).findFirst().orElseThrow(); }

    @Test void selectExactThenLongestPrefixThenCommon() {
        HcStationFormDO common = form(4L, "FORMULA_PROCESS_CHECK_COMMON", "{\"modelScope\":\"COMMON\"}");
        HcStationFormDO prefix = form(5L, "FORMULA_PROCESS_CHECK_LONG", "{\"modelPrefix\":\"W26P02\"}");
        HcStationFormDO exact = form(6L, "FORMULA_PROCESS_CHECK_EXACT", "{\"modelScope\":\"MODEL\",\"modelCode\":\"W26P0200\"}");
        forms.addAll(List.of(common, prefix, exact));
        assertTrue(HcFormulaStationRuntimeService.select(forms, "W26P0200", Set.of()).contains(exact));
        forms.remove(exact);
        assertTrue(HcFormulaStationRuntimeService.select(forms, "W26P0200", Set.of()).contains(prefix));
        assertTrue(HcFormulaStationRuntimeService.select(forms, "OTHER", Set.of()).contains(common));
        assertEquals(-1, HcFormulaStationRuntimeService.score(exact, "W26P02001"));
    }
    @Test void rejectEqualPriorityConflictAndMissingCategory() {
        forms.add(form(4L, "FORMULA_PROCESS_CHECK_DUP", "{\"modelPrefix\":\"W26P\"}"));
        assertThrows(RuntimeException.class, () -> service.bind(plan, operation, "W26P0200"));
        forms.remove(3); forms.remove(1);
        assertThrows(RuntimeException.class, () -> service.bind(plan, operation, "W26P0200"));
        verify(recordMapper, never()).insert(any(HcStationRecordDO.class));
    }
    @Test void excludeEnabledDevAndPreserveLegacyPrefixPrecedence() {
        HcStationFormDO dev = form(4L, "FORMULA_PROCESS_CHECK_DEV", "{\"modelScope\":\"MODEL\",\"modelCode\":\"W26P0200\"}");
        forms.add(dev);
        assertFalse(HcFormulaStationRuntimeService.select(forms, "W26P0200", Set.of()).contains(dev));
        forms.get(2).setSchemaJson("{\"modelPrefix\":\"W26P\",\"modelCode\":\"W26P0100\"}");
        assertEquals(4, HcFormulaStationRuntimeService.score(forms.get(2), "W26P0200"));
    }
    @Test void runtimeEngineDoesNotDefineDevIdentity() {
        HcStationFormDO formalRuntime = form(4L, "FORMULA_PROCESS_CHECK_RUNTIME",
                "{\"runtimeEngine\":\"STATION_FORM_RUNTIME_LAYOUT_DEV\"}");
        HcStationFormDO schemaDev = form(5L, "FORMULA_PROCESS_CHECK_DEV", "{\"devOnly\":true}");
        assertFalse(HcFormulaStationRuntimeService.dev(formalRuntime));
        assertTrue(HcFormulaStationRuntimeService.dev(schemaDev));
    }
    @Test void allowEmptyDraftButBlockConfirmWithHalfDualValue() {
        assertEquals("DRAFT", save(request("FORMULA_PROCESS_CHECK", "", ""), false).getDocStatus());
        assertFalse(production().getComplete());
        assertThrows(RuntimeException.class, () -> save(request("FORMULA_PROCESS_CHECK", "10", ""), true));
        assertEquals("CONFIRMED", save(request("FORMULA_PROCESS_CHECK", "10", "B01"), true).getDocStatus());
        assertTrue(production().getComplete());
    }
    @Test void ignoreClientDefinitionsAndKeepSnapshotAfterTemplateEditOrDisable() {
        service.bind(plan, operation, "W26P0200");
        templates.get(3L).setStandardText("后来改的新标准"); templates.get(3L).setRequiredFlag(false);
        forms.clear();
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "10", "B01");
        save(req, false);
        assertEquals("原标准", req.getDetails().get(0).getStandard());
        assertEquals("DUAL_TEXT", req.getDetails().get(0).getValueMode());
        assertEquals("原标准", production().getDetails().get(0).getStandard());
        assertTrue(production().getDetails().get(0).getRequiredFlag());
    }
    @Test void rejectForeignRecordAndConfirmedOverwriteAndUnknownSequences() {
        service.bind(plan, operation, "W26P0200");
        HcFormulaPassWorkSaveReqVO foreign = request("FORMULA_PROCESS_CHECK", "10", "B01"); foreign.setRecordId(records.get(0).getId());
        assertThrows(RuntimeException.class, () -> save(foreign, false));
        HcFormulaPassWorkSaveReqVO unknown = request("FORMULA_PROCESS_CHECK", "10", "B01"); unknown.getDetails().get(0).setItemSeq(99);
        assertThrows(RuntimeException.class, () -> save(unknown, false));
        HcFormulaPassWorkSaveReqVO duplicate = request("FORMULA_PROCESS_CHECK", "10", "B01"); duplicate.setDetails(List.of(duplicate.getDetails().get(0), duplicate.getDetails().get(0)));
        assertThrows(RuntimeException.class, () -> save(duplicate, false));
        save(request("FORMULA_PROCESS_CHECK", "10", "B01"), true);
        assertThrows(RuntimeException.class, () -> save(request("FORMULA_PROCESS_CHECK", "11", "B02"), false));
    }
    @Test void noConfirmationRequiresExplicitCompleteAction() {
        forms.get(2).setNeedConfirm(false);
        save(request("FORMULA_PROCESS_CHECK", "10", "B01"), false);
        assertFalse(production().getComplete());
        assertThrows(RuntimeException.class, () -> save(request("FORMULA_PROCESS_CHECK", "10", "B01"), true));
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "10", "B01"); req.setSubmit(true);
        assertEquals("RECORDED", save(req, false).getDocStatus());
        assertTrue(production().getComplete());
    }
    @Test void judgeRequiredDoesNotInheritDefaultOk() {
        assertEquals("", service.list(plan, operation, "W26P0200").stream().filter(r -> "startup".equals(r.getFormulaCategory())).findFirst().orElseThrow().getDetails().get(0).getStatus());
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_STARTUP_CHECK", "", ""); req.getDetails().get(0).setStatus("");
        assertThrows(RuntimeException.class, () -> save(req, true));
    }
    @Test void ngMayBeDraftButCannotComplete() {
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "10", "B01"); req.getDetails().get(0).setStatus("NG");
        save(req, false);
        assertThrows(RuntimeException.class, () -> save(req, true));
    }
    @Test void finishGateChecksAllBoundForms() {
        save(request("FORMULA_PROCESS_CHECK", "10", "B01"), true);
        assertThrows(RuntimeException.class, () -> service.assertFinished(plan, operation, "W26P0200"));
        save(request("FORMULA_STARTUP_CHECK", "", ""), true);
        save(request("FORMULA_CLEANING_CHECK", "", ""), true);
        assertDoesNotThrow(() -> service.assertFinished(plan, operation, "W26P0200"));
        records.remove(0);
        assertThrows(RuntimeException.class, () -> service.assertFinished(plan, operation, "W26P0200"));
    }
    @Test void legacyRecordRetainsItsOwnDefinitionsAndNoNewRequiredRule() {
        HcStationRecordDO legacy = HcStationRecordDO.builder().id(9L).planId(1L).planOperationId(11L).operationName("配料")
                .formId(3L).formCode("FORMULA_PROCESS_CHECK").formName("原名称").docStatus("CONFIRMED").build();
        records.add(legacy);
        values.put(9L, List.of(HcStationRecordItemDO.builder().itemSeq(7).itemName("历史加入项").standardText("历史标准")
                .valueMode("TEXT").resultFlag("OK").build()));
        assertEquals("历史标准", production().getDetails().get(0).getStandard());
        assertEquals(7, production().getDetails().get(0).getItemSeq());
        assertFalse(production().getDetails().get(0).getRequiredFlag());
        assertEquals("DUAL_TEXT", production().getDetails().get(0).getValueMode());
        assertTrue(production().getComplete());
        verify(recordMapper, never()).insert(any(HcStationRecordDO.class));
    }
    @Test void explicitTextDoesNotBecomeDualBecauseOfItemName() {
        assertEquals("MULTI_FIELDS", HcFormulaStationRuntimeService.effectiveMode("MULTI_FIELDS", "加入原料", "production", false));
        assertEquals("TEXT", HcFormulaStationRuntimeService.effectiveMode("TEXT", "加入原料", "production", true));
        assertEquals("DUAL_TEXT", HcFormulaStationRuntimeService.effectiveMode("TEXT", "加入原料", "production", false));
    }
    @Test void previewReportsConflictsWithoutWriting() {
        forms.add(form(4L, "FORMULA_PROCESS_CHECK_DUP", "{\"modelPrefix\":\"W26P\"}"));
        assertFalse(service.preview("W26P0200").getErrors().isEmpty());
        assertTrue(service.preview("W26P0200").getCandidates().stream().anyMatch(i -> Boolean.TRUE.equals(i.get("selected"))));
        verify(recordMapper, never()).insert(any(HcStationRecordDO.class));
    }
    @Test void legacyEmptyDraftCanBindItemsWithoutRewritingFilledHistory() {
        HcStationRecordDO empty = HcStationRecordDO.builder().id(9L).planId(1L).planOperationId(11L).operationName("配料")
                .formId(3L).formCode("FORMULA_PROCESS_CHECK").formName("原名称").docStatus("RECORDED").build();
        records.add(empty);
        when(formMapper.selectById(3L)).thenReturn(forms.get(2));
        assertEquals(1, production().getDetails().size());
        save(request("FORMULA_PROCESS_CHECK", "10", "B01"), true);
        assertTrue(production().getComplete());
    }
    @Test void unknownOverallResultCannotBypassCompletionRules() {
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "10", "B01"); req.setResult("FAKE_PASS");
        assertThrows(RuntimeException.class, () -> save(req, true));
    }

    private void configureMultiFields() {
        templates.get(3L).setValueMode("MULTI_FIELDS");
        templates.get(3L).setRequiredFlag(false);
        templates.get(3L).setFieldDefinitionsJson("""
            [{"key":"weight","label":"重量","type":"NUMBER","unit":"kg","required":true},
             {"key":"batch","label":"批号","type":"TEXT","unit":"","required":true},
             {"key":"temperature","label":"温度","type":"NUMBER","unit":"℃","required":true},
             {"key":"solids","label":"固含量","type":"NUMBER","unit":"%","required":false},
             {"key":"note","label":"说明","type":"TEXT","unit":"","required":false}]
            """);
    }
    @Test void fiveFieldsRoundTripPreservesZeroLeadingZerosAndSnapshot() {
        configureMultiFields();
        service.bind(plan, operation, "W26P0200");
        templates.get(3L).setFieldDefinitionsJson("[]"); // 已绑定任务不受模板变更影响。
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "", "");
        req.getDetails().get(0).setFieldDefinitionsJson("[]"); // 忽略客户端伪造定义。
        req.getDetails().get(0).setFieldValuesJson("{\"weight\":\"0\",\"batch\":\"00123\",\"temperature\":\"25.5\",\"solids\":\"45\",\"note\":\"无异常\"}");
        save(req, true);
        HcFormulaPassWorkItemRespVO item = production().getDetails().get(0);
        assertEquals("MULTI_FIELDS", item.getValueMode());
        assertTrue(item.getFieldDefinitionsJson().contains("temperature"));
        assertTrue(item.getFieldValuesJson().contains("00123"));
        assertTrue(item.getFieldValuesJson().contains("无异常"));
        assertTrue(production().getComplete());
    }
    @Test void multiDraftAllowsMissingButCompleteChecksEachRequiredField() {
        configureMultiFields();
        HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "", "");
        save(req, false);
        assertFalse(production().getComplete());
        req = request("FORMULA_PROCESS_CHECK", "", "");
        req.getDetails().get(0).setFieldValuesJson("{\"weight\":\"0\",\"batch\":\"0001\"}");
        HcFormulaPassWorkSaveReqVO incomplete = req;
        assertThrows(RuntimeException.class, () -> save(incomplete, true));
        req.getDetails().get(0).setFieldValuesJson("{\"weight\":\"0\",\"batch\":\"0001\",\"temperature\":\"0\"}");
        assertEquals("CONFIRMED", save(req, true).getDocStatus());
    }
    @Test void multiRejectsUnknownKeysDuplicateKeysAndNonNumericValues() {
        configureMultiFields();
        for (String values : List.of("{\"fake\":\"x\"}", "{\"weight\":\"1\",\"weight\":\"2\"}", "{\"weight\":\"abc\"}", "[]")) {
            HcFormulaPassWorkSaveReqVO req = request("FORMULA_PROCESS_CHECK", "", "");
            req.getDetails().get(0).setFieldValuesJson(values);
            assertThrows(RuntimeException.class, () -> save(req, false));
        }
    }

}
