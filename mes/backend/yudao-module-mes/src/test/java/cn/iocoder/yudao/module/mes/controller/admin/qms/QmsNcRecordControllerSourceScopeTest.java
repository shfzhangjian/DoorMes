package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordPageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QmsNcRecordControllerSourceScopeTest {

    @Test
    void productEndpointShouldAlwaysExcludeRawMaterial() {
        QmsNcRecordPageReqVO request = new QmsNcRecordPageReqVO();
        request.setExcludeRawMaterial(false);

        ReflectionTestUtils.invokeMethod(new QmsNcRecordController(), "markProductNc", request);

        assertTrue(request.getExcludeRawMaterial());
    }

    @Test
    void rawMaterialEndpointShouldAlwaysUseRawMaterialScope() {
        QmsNcRecordPageReqVO request = new QmsNcRecordPageReqVO();
        request.setSourceType("SEMI_FINISHED");
        request.setExcludeRawMaterial(true);

        ReflectionTestUtils.invokeMethod(new QmsRawMaterialNcRecordController(), "markRawMaterial", request);

        assertEquals("RAW_MATERIAL", request.getSourceType());
        assertFalse(request.getExcludeRawMaterial());
    }
}
