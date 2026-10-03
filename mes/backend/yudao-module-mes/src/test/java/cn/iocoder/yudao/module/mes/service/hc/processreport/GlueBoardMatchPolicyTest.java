package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GlueBoardMatchPolicyTest {
    private HcFinishedGlueBoardMapItemDO item(String model, String code) {
        return HcFinishedGlueBoardMapItemDO.builder().glueBoardModel(model).glueBoardMaterialCode(code).build();
    }
    @Test void correctSdkAllowed() {
        assertNull(GlueBoardMatchPolicy.mismatch("W33P0300", "SDK", "01.02.00020", List.of(item("SDK", "01.02.00020"))));
    }
    @Test void wrongW250Blocked() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P0300", "W250改B", "01.02.00045", List.of(item("SDK", "01.02.00020"))));
    }
    @Test void matchingModelWrongMaterialBlocked() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P0300", "SDK", "01.02.00045", List.of(item("SDK", "01.02.00020"))));
    }
    @Test void nonPreferredAlternativeAllowed() {
        assertNull(GlueBoardMatchPolicy.mismatch("W33P0200", "W250改B", "01.02.00045",
                List.of(item("W250", "01.02.00025"), item("W250改B", "01.02.00045"))));
    }
    @Test void prefixMustNotMatch() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P0200", "W250改B", "01.02.00025", List.of(item("W250", "01.02.00025"))));
    }
    @Test void missingMappingBlocked() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P", "PT-217KV", "01.02.00002", List.of()));
    }
    @Test void missingMaterialConfigurationBlocked() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P0400", "SDK双面平纹胶", "X", List.of(item("SDK双面平纹胶", null))));
    }
    @Test void blankActualModelBlocked() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P0300", "", "01.02.00020", List.of(item("SDK", "01.02.00020"))));
    }
    @Test void whitespaceAndCaseNormalized() {
        assertNull(GlueBoardMatchPolicy.mismatch("W33P0300", " sdk ", "01.02.00020 ", List.of(item("SDK", "01.02.00020"))));
    }
    @Test void processOneMustNotUseProcessTwoBoard() {
        assertNotNull(GlueBoardMatchPolicy.mismatch("W33P0300", "SDK", "01.02.00020", List.of(item("PT-217KV", "01.02.00002"))));
    }
}
