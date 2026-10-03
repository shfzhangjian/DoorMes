package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HcProcessReportServiceImplWetProductionCheckHeaderTest {

    private final HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();

    @Test
    void shouldReplaceClientIdentityFieldsAndKeepPerRecordTimes() throws Exception {
        HcStationFormDO form = HcStationFormDO.builder()
                .formCode("WET_PROCESS_CHECK_W26P")
                .schemaJson("{\"wetCategory\":\"production-check\"}")
                .build();
        HcPlanOrderDO plan = HcPlanOrderDO.builder()
                .motherMaterialCode("MAT-W26")
                .motherModelCode("W26P0100")
                .productionBatchNo("W26P0100-001")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .equipmentCode("WET-01")
                .build();
        HcStationRecordDO record = HcStationRecordDO.builder()
                .equipmentCode("WET-LEGACY")
                .build();
        HcWetPassWorkSaveReqVO request = new HcWetPassWorkSaveReqVO();
        request.setEquipmentCode("WET-02");
        request.setRecorderTime(LocalDateTime.of(2026, 9, 7, 9, 0));
        request.setHeaderDataJson("{\"materialCode\":\"CLIENT-MAT\",\"modelCode\":\"CLIENT-MODEL\","
                + "\"machine\":\"CLIENT-MACHINE\",\"batchNo\":\"CLIENT-BATCH\","
                + "\"startTime\":\"2026-09-07T08:00:00\",\"inWashTime\":\"2026-09-07 08:10:00\"}");

        var header = JsonUtils.parseTree(normalize(request, form, plan, operation, record));

        assertEquals("MAT-W26", header.path("materialCode").asText());
        assertEquals("W26P0100", header.path("modelCode").asText());
        assertEquals("WET-02", header.path("machine").asText());
        assertEquals("W26P0100-001", header.path("batchNo").asText());
        assertEquals("2026-09-07 08:00:00", header.path("startTime").asText());
        assertEquals("2026-09-07 08:10:00", header.path("inWashTime").asText());
    }

    private String normalize(HcWetPassWorkSaveReqVO request,
                             HcStationFormDO form,
                             HcPlanOrderDO plan,
                             HcPlanOrderOperationDO operation,
                             HcStationRecordDO record) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "normalizeWetPassWorkHeaderData", String.class, HcStationFormDO.class,
                HcPlanOrderDO.class, HcPlanOrderOperationDO.class, HcStationRecordDO.class,
                HcWetPassWorkSaveReqVO.class);
        method.setAccessible(true);
        return (String) method.invoke(service, request.getHeaderDataJson(), form, plan, operation, record, request);
    }
}
