package cn.iocoder.yudao.module.mes.service.hc.stationform;

import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormItemSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import java.util.List;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormSaveReqVO;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HcStationFormPressSlotItemIdentityTest {
    @Test void productionCheckAllowsThreeScopesAndRejectsDuplicatePublication() {
        var service = new HcStationFormServiceImpl();
        var forms = mock(HcStationFormMapper.class);
        ReflectionTestUtils.setField(service,"hcStationFormMapper",forms);
        when(forms.selectEnabledByProcess("PRESS_SLOT")).thenReturn(List.of());
        for (String scope : List.of("MODEL","PREFIX","COMMON")) {
            var request = new HcStationFormSaveReqVO();
            request.setProcessCode("PRESS_SLOT"); request.setStatus(1);
            request.setSchemaJson("{\"formType\":\"PRODUCTION_CHECK\",\"published\":true,\"modelScope\":\""+scope+"\",\"modelCode\":\"W26P0200\",\"modelPrefix\":\"W26P\"}");
            when(forms.selectEnabledByProcess("PRESS_SLOT")).thenReturn(List.of());
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,"validatePressSlotProductionScope",request));
            var existing = new HcStationFormDO(); existing.setId(1L); existing.setSchemaJson(request.getSchemaJson());
            when(forms.selectEnabledByProcess("PRESS_SLOT")).thenReturn(List.of(existing));
            assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service,"validatePressSlotProductionScope",request));
            request.setId(1L);
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,"validatePressSlotProductionScope",request));
        }
    }
    @Test void editingAndReorderingPreservesOwnedIdsAndDeletesOnlyRemovedItems() {
        var service = new HcStationFormServiceImpl();
        var forms = mock(HcStationFormMapper.class);
        var items = mock(HcStationFormItemMapper.class);
        ReflectionTestUtils.setField(service,"hcStationFormMapper",forms);
        ReflectionTestUtils.setField(service,"hcStationFormItemMapper",items);
        var form = new HcStationFormDO(); form.setId(174L); form.setProcessCode("PRESS_SLOT"); form.setSchemaJson("{\"formType\":\"PRODUCTION_CHECK\"}");
        when(forms.selectById(174L)).thenReturn(form);
        var retained = new HcStationFormItemDO(); retained.setId(10L); retained.setFormId(174L);
        var removed = new HcStationFormItemDO(); removed.setId(11L); removed.setFormId(174L);
        when(items.selectByFormId(174L)).thenReturn(List.of(retained,removed));
        var update = new HcStationFormItemSaveReqVO(); update.setId(10L); update.setItemSeq(2); update.setItemName("温度");
        var added = new HcStationFormItemSaveReqVO(); added.setItemSeq(1); added.setItemName("湿度");
        ReflectionTestUtils.invokeMethod(service,"replaceFormItems",174L,List.of(update,added));
        verify(items).updateById(argThat((HcStationFormItemDO item) -> item.getId().equals(10L) && item.getItemSeq()==2));
        verify(items).insert(argThat((HcStationFormItemDO item) -> item.getId()==null && item.getFormId().equals(174L)));
        verify(items).deleteById(11L);
        verify(items,never()).deleteByFormId(anyLong());
        verify(items,never()).deleteById(10L);
    }
}
