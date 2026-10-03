package cn.iocoder.yudao.module.mes.service.hc.processreport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HcPressSlotRndPadTypeTest {
 @Test void equipmentClassification() {
  assertEquals("WHITE_PAD", HcPressSlotRndPadType.forEquipment("WHITE_PAD", "CMP白垫压槽机1"));
  assertEquals("BLACK_PAD", HcPressSlotRndPadType.forEquipment("BLACK_PAD", "CMP黑垫压槽机2"));
  assertNull(HcPressSlotRndPadType.forEquipment("BLACK_PAD", "白垫压槽机"));
  assertNull(HcPressSlotRndPadType.forEquipment("COMMON", "白垫压槽机"));
  assertNull(HcPressSlotRndPadType.forEquipment(null, "白垫压槽机"));
  assertNull(HcPressSlotRndPadType.forEquipment("WHITE_PAD", "黑垫白垫通用机"));
 }
 @Test void historicalSnapshotIsStableAndUnknownIsNotGuessed() {
  assertEquals("WHITE_PAD", HcPressSlotRndPadType.forHistory("WHITE_PAD", "黑垫压槽机"));
  assertEquals("BLACK_PAD", HcPressSlotRndPadType.forHistory(null, "CMP黑垫压槽机2"));
  assertEquals("WHITE_PAD", HcPressSlotRndPadType.forHistory(null, "CMP白垫压槽机1"));
  assertEquals("UNCLASSIFIED", HcPressSlotRndPadType.forHistory(null, "压槽机"));
 }
}
