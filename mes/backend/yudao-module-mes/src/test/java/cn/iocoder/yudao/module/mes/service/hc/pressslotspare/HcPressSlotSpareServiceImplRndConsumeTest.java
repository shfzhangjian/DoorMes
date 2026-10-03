package cn.iocoder.yudao.module.mes.service.hc.pressslotspare;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class HcPressSlotSpareServiceImplRndConsumeTest {
 @InjectMocks HcPressSlotSpareServiceImpl service;
 @Mock HcEquipmentMapper hcEquipmentMapper;
 @Mock HcPressSlotSpareMapper hcPressSlotSpareMapper;
 @Mock HcPressSlotSpareRecordMapper hcPressSlotSpareRecordMapper;
 private HcPressSlotSpareRndConsumeSaveReqVO request() {
  var req = new HcPressSlotSpareRndConsumeSaveReqVO();
  req.setEquipmentId(1L); req.setPressSlotInputPcs(10); req.setPressSlotOutputPcs(9);
  req.setConsumeTime(LocalDateTime.now().minusMinutes(1));
  req.setModelCode("RND"); req.setProductionBatchNo("RND-1"); req.setRemark("研发"); return req;
 }
 @Test void bothSparesReceiveSameEquipmentSnapshot() {
  when(hcEquipmentMapper.selectById(1L)).thenReturn(HcEquipmentDO.builder().id(1L)
    .equipmentName("白垫压槽机").applicablePadType("WHITE_PAD").build());
  var roller = HcPressSlotSpareDO.builder().id(2L).equipmentId(1L).status("ACTIVE").spareType("PRESS_ROLLER").useCount(20).build();
  var bearing = HcPressSlotSpareDO.builder().id(3L).equipmentId(1L).status("ACTIVE").spareType("BEARING").useCount(30).build();
  when(hcPressSlotSpareMapper.selectOneByEquipmentAndType(1L,"PRESS_ROLLER")).thenReturn(roller);
  when(hcPressSlotSpareMapper.selectOneByEquipmentAndType(1L,"BEARING")).thenReturn(bearing);
  when(hcPressSlotSpareMapper.updateCurrentStateById(any())).thenReturn(1);
  String group = service.saveRndConsume(request());
  var capture = ArgumentCaptor.forClass(HcPressSlotSpareRecordDO.class);
  verify(hcPressSlotSpareRecordMapper,times(2)).insert(capture.capture());
  capture.getAllValues().forEach(r -> { assertEquals("WHITE_PAD",r.getPadType()); assertEquals(group,r.getRecordGroupNo()); });
  assertEquals(30,roller.getUseCount()); assertEquals(40,bearing.getUseCount());
 }
 @Test void conflictingEquipmentDoesNotWrite() {
  when(hcEquipmentMapper.selectById(1L)).thenReturn(HcEquipmentDO.builder().id(1L)
    .equipmentName("白垫压槽机").applicablePadType("BLACK_PAD").build());
  assertThrows(RuntimeException.class, () -> service.saveRndConsume(request()));
  verifyNoInteractions(hcPressSlotSpareMapper,hcPressSlotSpareRecordMapper);
 }
}
