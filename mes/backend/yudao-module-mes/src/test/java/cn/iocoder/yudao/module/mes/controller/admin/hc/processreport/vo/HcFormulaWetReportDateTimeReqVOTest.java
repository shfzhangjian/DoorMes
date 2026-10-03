package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HcFormulaWetReportDateTimeReqVOTest {

    private final ObjectMapper objectMapper = createObjectMapperWithGlobalTimestampDeserializer();

    @Test
    void shouldKeepFormulaDateTimeTextWhenGlobalDeserializerOnlySupportsTimestamp() throws Exception {
        HcFormulaReportSaveReqVO submitReq = objectMapper.readValue("""
                {
                  "planId": 94,
                  "planOperationId": 764,
                  "startTime": "2026-07-11 11:00:00",
                  "endTime": "2026-07-11 15:16:35",
                  "stirStartTime": "2026-07-11 11:10:00",
                  "stirEndTime": "2026-07-11 14:50:00",
                  "recorderTime": "2026-07-11 14:16:02",
                  "confirmerTime": "2026-07-11 15:20:00"
                }
                """, HcFormulaReportSaveReqVO.class);

        assertEquals(LocalDateTime.of(2026, 7, 11, 11, 0), submitReq.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 15, 16, 35), submitReq.getEndTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 11, 10), submitReq.getStirStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 14, 50), submitReq.getStirEndTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 14, 16, 2), submitReq.getRecorderTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 15, 20), submitReq.getConfirmerTime());

        HcFormulaReportTimeUpdateReqVO updateReq = objectMapper.readValue("""
                {
                  "id": 432,
                  "startTime": "2026-07-11 00:00:17",
                  "endTime": "2026-07-11 14:51:45"
                }
                """, HcFormulaReportTimeUpdateReqVO.class);

        assertEquals(LocalDateTime.of(2026, 7, 11, 0, 0, 17), updateReq.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 14, 51, 45), updateReq.getEndTime());
    }

    @Test
    void shouldKeepWetDateTimeTextWhenGlobalDeserializerOnlySupportsTimestamp() throws Exception {
        HcWetReportSaveReqVO submitReq = objectMapper.readValue("""
                {
                  "planId": 94,
                  "planOperationId": 765,
                  "startTime": "2026-07-11 08:00:00",
                  "endTime": "2026-07-11 16:00:00",
                  "inWashTime": "2026-07-11 08:10:00",
                  "outWashTime": "2026-07-11 08:20:00",
                  "inSolidifyTime": "2026-07-11 09:00:00",
                  "outSolidifyTime": "2026-07-11 10:00:00",
                  "inOvenTime": "2026-07-11 10:10:00",
                  "outOvenTime": "2026-07-11 11:10:00",
                  "recorderTime": "2026-07-11 16:01:00",
                  "confirmerTime": "2026-07-11 16:05:00"
                }
                """, HcWetReportSaveReqVO.class);

        assertEquals(LocalDateTime.of(2026, 7, 11, 8, 0), submitReq.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 16, 0), submitReq.getEndTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 8, 10), submitReq.getInWashTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 8, 20), submitReq.getOutWashTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 9, 0), submitReq.getInSolidifyTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 10, 0), submitReq.getOutSolidifyTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 10, 10), submitReq.getInOvenTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 11, 10), submitReq.getOutOvenTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 16, 1), submitReq.getRecorderTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 16, 5), submitReq.getConfirmerTime());
    }

    @Test
    void shouldAcceptWetProcessEpochMillisReloadedFromExtraJson() throws Exception {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDateTime inWashTime = LocalDateTime.of(2026, 7, 11, 8, 10);
        LocalDateTime outWashTime = LocalDateTime.of(2026, 7, 11, 8, 20);
        LocalDateTime inSolidifyTime = LocalDateTime.of(2026, 7, 11, 9, 0);
        LocalDateTime outSolidifyTime = LocalDateTime.of(2026, 7, 11, 10, 0);
        LocalDateTime inOvenTime = LocalDateTime.of(2026, 7, 11, 10, 10);
        LocalDateTime outOvenTime = LocalDateTime.of(2026, 7, 11, 11, 10);
        String payload = """
                {
                  "planId": 94,
                  "planOperationId": 765,
                  "startTime": "2026-07-11 08:00:00",
                  "endTime": "2026-07-11 16:00:00",
                  "inWashTime": %d,
                  "outWashTime": %d,
                  "inSolidifyTime": %d,
                  "outSolidifyTime": %d,
                  "inOvenTime": %d,
                  "outOvenTime": %d,
                  "recorderTime": "2026-07-11 16:01:00"
                }
                """.formatted(
                inWashTime.atZone(zoneId).toInstant().toEpochMilli(),
                outWashTime.atZone(zoneId).toInstant().toEpochMilli(),
                inSolidifyTime.atZone(zoneId).toInstant().toEpochMilli(),
                outSolidifyTime.atZone(zoneId).toInstant().toEpochMilli(),
                inOvenTime.atZone(zoneId).toInstant().toEpochMilli(),
                outOvenTime.atZone(zoneId).toInstant().toEpochMilli());

        HcWetReportSaveReqVO submitReq = objectMapper.readValue(payload, HcWetReportSaveReqVO.class);

        assertEquals(inWashTime, submitReq.getInWashTime());
        assertEquals(outWashTime, submitReq.getOutWashTime());
        assertEquals(inSolidifyTime, submitReq.getInSolidifyTime());
        assertEquals(outSolidifyTime, submitReq.getOutSolidifyTime());
        assertEquals(inOvenTime, submitReq.getInOvenTime());
        assertEquals(outOvenTime, submitReq.getOutOvenTime());
    }

    @Test
    void shouldKeepStartAndConfirmDateTimeText() throws Exception {
        HcFormulaReportStartReqVO formulaStartReq = objectMapper.readValue("""
                {
                  "planId": 94,
                  "planOperationId": 764,
                  "startTime": "2026-07-11 10:30:00",
                  "recorderTime": "2026-07-11 10:31:00"
                }
                """, HcFormulaReportStartReqVO.class);
        HcWetReportStartReqVO wetStartReq = objectMapper.readValue("""
                {
                  "planId": 94,
                  "planOperationId": 765,
                  "startTime": "2026-07-11 11:30:00",
                  "recorderTime": "2026-07-11 11:31:00"
                }
                """, HcWetReportStartReqVO.class);
        HcOperationReportConfirmReqVO confirmReq = objectMapper.readValue("""
                {
                  "id": 436,
                  "confirmerTime": "2026-07-11 16:30:00"
                }
                """, HcOperationReportConfirmReqVO.class);

        assertEquals(LocalDateTime.of(2026, 7, 11, 10, 30), formulaStartReq.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 10, 31), formulaStartReq.getRecorderTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 11, 30), wetStartReq.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 11, 31), wetStartReq.getRecorderTime());
        assertEquals(LocalDateTime.of(2026, 7, 11, 16, 30), confirmReq.getConfirmerTime());
    }

    private static ObjectMapper createObjectMapperWithGlobalTimestampDeserializer() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        mapper.registerModule(module);
        return mapper;
    }

}
