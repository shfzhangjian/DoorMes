package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PressSlotProductionCheckContractTest {
    HcStationFormDO form(long id, String prefix) {
        HcStationFormDO form = new HcStationFormDO();
        form.setId(id); form.setFormCode("PRESS_SLOT_" + id);
        form.setSchemaJson("{\"modelScope\":\"PREFIX\",\"modelPrefix\":\"" + prefix + "\",\"published\":true,\"formType\":\"PRODUCTION_CHECK\"}");
        return form;
    }
    @Test void prefixLongestPublishedOnlyAndCommonFallback() {
        var broad = form(1,"W26"); var narrow = form(2,"W26P");
        assertSame(narrow, PressSlotProductionCheckContract.resolve(List.of(broad,narrow)," w26p0200 "));
        var common = form(3,"W26P"); common.setSchemaJson("{\"modelScope\":\"COMMON\",\"published\":true,\"formType\":\"PRODUCTION_CHECK\",\"allowCommonFallback\":true}");
        assertSame(common, PressSlotProductionCheckContract.resolve(List.of(common),"W26P0200"));
        narrow.setFormCode("PRESS_SLOT_DEV");
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.resolve(List.of(narrow),"W26P0200"));
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.resolve(List.of(broad),"C12P0100"));
    }
    @Test void exactBeforePrefixBeforeCommonAndScopeValidation() {
        var prefix = form(1,"W26P");
        var exact = form(2,"unused");
        exact.setSchemaJson("{\"modelScope\":\"MODEL\",\"modelCode\":\"W26P0200\",\"published\":true,\"formType\":\"PRODUCTION_CHECK\"}");
        var common = form(3,"unused");
        common.setSchemaJson("{\"modelScope\":\"COMMON\",\"published\":true,\"formType\":\"PRODUCTION_CHECK\"}");
        assertSame(exact, PressSlotProductionCheckContract.resolve(List.of(common,prefix,exact),"w26p0200"));
        assertSame(prefix, PressSlotProductionCheckContract.resolve(List.of(common,prefix,exact),"W26P0100"));
        assertSame(common, PressSlotProductionCheckContract.resolve(List.of(common,prefix,exact),"C12P0100"));
        assertDoesNotThrow(() -> PressSlotProductionCheckContract.validateModel(PressSlotProductionCheckContract.schema(exact),"W26P0200"));
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateModel(PressSlotProductionCheckContract.schema(exact),"W26P0100"));
        assertDoesNotThrow(() -> PressSlotProductionCheckContract.validateModel(PressSlotProductionCheckContract.schema(common),"C12P0100"));
        var duplicate = form(4,"unused"); duplicate.setSchemaJson(exact.getSchemaJson());
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.resolve(List.of(exact,duplicate),"W26P0200"));
        duplicate.setSchemaJson(common.getSchemaJson());
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.resolve(List.of(common,duplicate),"C12P0100"));
    }
    @Test void duplicatePrefixIsConfigurationError() {
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.resolve(List.of(form(1,"W26P"),form(2,"W26P")),"W26P0200"));
    }
    @Test void submittedRowCannotRemoveRequiredOrSpoofTemplate() {
        var template = new HcStationFormItemDO(); template.setId(7L); template.setItemName("温度"); template.setRequiredFlag(true); template.setValueMode("NUMBER");
        var row = new HcPressSlotProcessParamSaveReqVO.Item(); row.setTemplateItemId(7L); row.setRequiredFlag(false); row.setCheckResult("OK");
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateItems(List.of(row),List.of(template),true,true));
        row.setActualValue("23.5");
        assertDoesNotThrow(() -> PressSlotProductionCheckContract.validateItems(List.of(row),List.of(template),true,true));
        assertTrue(row.getRequiredFlag()); assertEquals("station_item_7",row.getFieldKey());
        row.setActualValue("不是数值");
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateItems(List.of(row),List.of(template),true,true));
        row.setTemplateItemId(8L);
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateItems(List.of(row),List.of(template),false,true));
    }
    @Test void changedOrderUsesStableIdentityAndRejectsDuplicates() {
        var a = new HcStationFormItemDO(); a.setId(1L); a.setItemName("同名");
        var b = new HcStationFormItemDO(); b.setId(2L); b.setItemName("同名");
        var r1 = new HcPressSlotProcessParamSaveReqVO.Item(); r1.setTemplateItemId(2L);
        var r2 = new HcPressSlotProcessParamSaveReqVO.Item(); r2.setTemplateItemId(1L);
        assertDoesNotThrow(() -> PressSlotProductionCheckContract.validateItems(List.of(r1,r2),List.of(a,b),false,true));
        r2.setTemplateItemId(2L);
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateItems(List.of(r1,r2),List.of(a,b),false,true));
    }
    @Test void dateTimeIsStrictAndModelMustStayInPrefix() {
        assertDoesNotThrow(() -> PressSlotProductionCheckContract.validateHeader(Map.of("submitTime","2026-09-07 14:30:06")));
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateHeader(Map.of("submitTime","14:30:06")));
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateHeader(Map.of("submitTime","2026-02-30 14:30:06")));
        assertThrows(RuntimeException.class, () -> PressSlotProductionCheckContract.validateModel(Map.of("modelScope","PREFIX","modelPrefix","W26P"),"W33P0100"));
    }
}
