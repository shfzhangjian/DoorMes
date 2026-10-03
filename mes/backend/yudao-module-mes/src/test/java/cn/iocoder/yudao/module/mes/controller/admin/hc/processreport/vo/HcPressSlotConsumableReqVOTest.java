package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HcPressSlotConsumableReqVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeReplaceTimeAndInitialUseCount() throws Exception {
        String json = """
                {
                  "consumableType": "BEARING",
                  "batchNo": "BEARING-001",
                  "initialUseCount": 125,
                  "replaceTime": "2025-06-25 08:30:45"
                }
                """;

        HcPressSlotConsumableReplaceReqVO reqVO = objectMapper.readValue(json, HcPressSlotConsumableReplaceReqVO.class);

        assertEquals(125, reqVO.getInitialUseCount());
        assertEquals(LocalDateTime.of(2025, 6, 25, 8, 30, 45), reqVO.getReplaceTime());
    }

    @Test
    void shouldDeserializeCleanTimeAndInitialUseCount() throws Exception {
        String json = """
                {
                  "consumableType": "PRESS_ROLLER",
                  "batchNo": "ROLLER-001",
                  "initialUseCount": 18,
                  "cleanTime": "2025-06-25 09:15:30"
                }
                """;

        HcPressSlotConsumableCleanReqVO reqVO = objectMapper.readValue(json, HcPressSlotConsumableCleanReqVO.class);

        assertEquals(18, reqVO.getInitialUseCount());
        assertEquals(LocalDateTime.of(2025, 6, 25, 9, 15, 30), reqVO.getCleanTime());
    }

    @Test
    void shouldFilterDirtyEpochDateTime() throws Exception {
        String replaceJson = """
                {
                  "consumableType": "BEARING",
                  "batchNo": "BEARING-001",
                  "replaceTime": "1970-01-01 08:00:00"
                }
                """;
        String cleanJson = """
                {
                  "consumableType": "PRESS_ROLLER",
                  "batchNo": "ROLLER-001",
                  "cleanTime": 0
                }
                """;

        HcPressSlotConsumableReplaceReqVO replaceReqVO = objectMapper.readValue(replaceJson, HcPressSlotConsumableReplaceReqVO.class);
        HcPressSlotConsumableCleanReqVO cleanReqVO = objectMapper.readValue(cleanJson, HcPressSlotConsumableCleanReqVO.class);

        assertNull(replaceReqVO.getReplaceTime());
        assertNull(cleanReqVO.getCleanTime());
    }
}
