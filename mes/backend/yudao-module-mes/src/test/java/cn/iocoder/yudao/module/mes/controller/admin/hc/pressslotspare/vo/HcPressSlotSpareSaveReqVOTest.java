package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HcPressSlotSpareSaveReqVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeBusinessDateTimeText() throws Exception {
        String json = """
                {
                  "equipmentId": 7,
                  "spareType": "PRESS_ROLLER",
                  "batchNo": "AAA",
                  "lastReplaceTime": "2025-06-25 08:30:45",
                  "lastCleanTime": "2025-06-25 09:15:30"
                }
                """;

        HcPressSlotSpareSaveReqVO reqVO = objectMapper.readValue(json, HcPressSlotSpareSaveReqVO.class);

        assertEquals(LocalDateTime.of(2025, 6, 25, 8, 30, 45), reqVO.getLastReplaceTime());
        assertEquals(LocalDateTime.of(2025, 6, 25, 9, 15, 30), reqVO.getLastCleanTime());
    }

    @Test
    void shouldDeserializeIsoTimeWithShanghaiZone() throws Exception {
        String json = """
                {
                  "equipmentId": 7,
                  "spareType": "PRESS_ROLLER",
                  "batchNo": "AAA",
                  "lastReplaceTime": "2026-06-01T13:59:13.000Z",
                  "lastCleanTime": "2026-06-01T14:31:25.000Z"
                }
                """;

        HcPressSlotSpareSaveReqVO reqVO = objectMapper.readValue(json, HcPressSlotSpareSaveReqVO.class);

        assertEquals(LocalDateTime.of(2026, 6, 1, 21, 59, 13), reqVO.getLastReplaceTime());
        assertEquals(LocalDateTime.of(2026, 6, 1, 22, 31, 25), reqVO.getLastCleanTime());
    }

    @Test
    void shouldFilterDirtyEpochDateTime() throws Exception {
        String json = """
                {
                  "equipmentId": 7,
                  "spareType": "PRESS_ROLLER",
                  "batchNo": "AAA",
                  "lastReplaceTime": "1970-01-01 08:00:00",
                  "lastCleanTime": 0
                }
                """;

        HcPressSlotSpareSaveReqVO reqVO = objectMapper.readValue(json, HcPressSlotSpareSaveReqVO.class);

        assertNull(reqVO.getLastReplaceTime());
        assertNull(reqVO.getLastCleanTime());
    }
}
