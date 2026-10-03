package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HcProcessReportServiceImplWetTimeSyncTest {

    private final HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();

    @Test
    void shouldOnlyReplaceFirstPoreSelfCheckTime() {
        String headerDataJson = """
                {
                  "records": [
                    {
                      "selfCheckTime": "2026-07-11 08:00:00",
                      "selfCheckResult": "OK",
                      "inspector": "张三",
                      "remark": "首条"
                    },
                    {
                      "selfCheckTime": "2026-07-11 09:00:00",
                      "selfCheckResult": "NG",
                      "inspector": "李四",
                      "remark": "第二条"
                    }
                  ],
                  "unrelatedField": "keep"
                }
                """;

        String updated = service.replaceFirstWetPoreSelfCheckTime(
                headerDataJson, "2026-07-11 07:30:15");
        JsonNode root = JsonUtils.parseTree(updated);

        assertEquals("2026-07-11 07:30:15", root.path("records").path(0).path("selfCheckTime").asText());
        assertEquals("2026-07-11 09:00:00", root.path("records").path(1).path("selfCheckTime").asText());
        assertEquals("OK", root.path("records").path(0).path("selfCheckResult").asText());
        assertEquals("张三", root.path("records").path(0).path("inspector").asText());
        assertEquals("keep", root.path("unrelatedField").asText());
    }

    @Test
    void shouldUseEditableEndTimeAsWetCompletionTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 3, 8, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 3, 16, 20, 0);
        LocalDateTime recorderTime = LocalDateTime.of(2026, 8, 3, 16, 30, 45);

        assertEquals(endTime, service.resolveWetCompletionTime(startTime, endTime, recorderTime));
    }

    @Test
    void shouldRejectMissingWetCompletionTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 3, 8, 0, 0);
        LocalDateTime recorderTime = LocalDateTime.of(2026, 8, 3, 16, 30, 45);

        assertThrows(RuntimeException.class,
                () -> service.resolveWetCompletionTime(startTime, null, recorderTime));
    }

    @Test
    void shouldRejectMissingWetFinalRecorderTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 3, 8, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 3, 16, 20, 0);

        assertThrows(RuntimeException.class,
                () -> service.resolveWetCompletionTime(startTime, endTime, null));
    }

    @Test
    void shouldRejectWetCompletionTimeBeforeStartTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 3, 8, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 3, 7, 59, 59);
        LocalDateTime recorderTime = LocalDateTime.of(2026, 8, 3, 16, 30, 45);

        assertThrows(RuntimeException.class,
                () -> service.resolveWetCompletionTime(startTime, endTime, recorderTime));
    }

    @Test
    void shouldRejectWetCompletionTimeAfterFinalRecorderTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 3, 8, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 3, 16, 31, 0);
        LocalDateTime recorderTime = LocalDateTime.of(2026, 8, 3, 16, 30, 45);

        assertThrows(RuntimeException.class,
                () -> service.resolveWetCompletionTime(startTime, endTime, recorderTime));
    }

}
