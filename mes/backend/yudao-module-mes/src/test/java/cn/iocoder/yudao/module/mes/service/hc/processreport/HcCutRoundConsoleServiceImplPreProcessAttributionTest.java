package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HcCutRoundConsoleServiceImplPreProcessAttributionTest {

    private final HcCutRoundConsoleServiceImpl service = new HcCutRoundConsoleServiceImpl();

    @Test
    void shouldKeepSelectedPressSlotAttributionForAdhesive2Ng() throws Exception {
        HcAdhesive2ReportDO source = HcAdhesive2ReportDO.builder()
                .operationName("粘胶2")
                .selfCheck("NG")
                .extraJson("{\"ngAttributionType\":\"PRE_PROCESS_SELF_CHECK\","
                        + "\"sourceNgProcessName\":\"压槽\","
                        + "\"sourceNgReason\":\"压槽自检异常（加工前发现）\"}")
                .build();

        Map<String, Object> extra = buildCutRoundSourceExtra(source);

        assertEquals("压槽", extra.get("sourceNgProcessName"));
        assertEquals("压槽自检异常（加工前发现）", extra.get("sourceNgReason"));
    }

    @Test
    void shouldUseAdhesive2AttributionWhenNoSelectionExists() throws Exception {
        HcAdhesive2ReportDO source = HcAdhesive2ReportDO.builder()
                .operationName("粘胶2")
                .selfCheck("NG")
                .build();

        Map<String, Object> extra = buildCutRoundSourceExtra(source);

        assertEquals("粘胶2", extra.get("sourceNgProcessName"));
        assertEquals("粘胶2自检NG", extra.get("sourceNgReason"));
    }

    private Map<String, Object> buildCutRoundSourceExtra(HcAdhesive2ReportDO source) throws Exception {
        Method method = HcCutRoundConsoleServiceImpl.class.getDeclaredMethod(
                "buildCutRoundSourceExtraJson", HcAdhesive2ReportDO.class, boolean.class);
        method.setAccessible(true);
        String json = (String) method.invoke(service, source, false);
        return JsonUtils.parseObject(json, new TypeReference<>() {});
    }
}
