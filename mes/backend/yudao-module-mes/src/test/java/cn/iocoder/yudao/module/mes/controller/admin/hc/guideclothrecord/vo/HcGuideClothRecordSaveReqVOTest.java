package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HcGuideClothRecordSaveReqVOTest {

    @Test
    void shouldKeepReplaceTimeTextWhenGlobalDeserializerOnlySupportsTimestamp() throws Exception {
        ObjectMapper objectMapper = createObjectMapper();

        HcGuideClothRecordSaveReqVO reqVO = objectMapper.readValue("""
                {
                  "lineName": "白垫线",
                  "lineCode": "W",
                  "replaceTime": "2026-07-11 15:30:45",
                  "useCount": 1,
                  "currentFlag": 0
                }
                """, HcGuideClothRecordSaveReqVO.class);

        assertEquals(LocalDateTime.of(2026, 7, 11, 15, 30, 45), reqVO.getReplaceTime());
    }

    @Test
    void shouldDiscardEpochPlaceholderReplaceTime() throws Exception {
        HcGuideClothRecordSaveReqVO reqVO = createObjectMapper().readValue("""
                {
                  "lineName": "白垫线",
                  "lineCode": "WHITE",
                  "replaceTime": "1970-01-01 08:00:00",
                  "useCount": 1,
                  "currentFlag": 0
                }
                """, HcGuideClothRecordSaveReqVO.class);

        assertNull(reqVO.getReplaceTime());
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        objectMapper.registerModule(module);
        return objectMapper;
    }

}
