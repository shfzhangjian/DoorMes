package cn.iocoder.yudao.module.mes.service.hc.stationform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormulaMultiFieldsTest {
    private String field(String key) {
        return "{\"key\":\"" + key + "\",\"label\":\"温度\",\"type\":\"NUMBER\",\"unit\":\"℃\",\"required\":true}";
    }
    @Test void supportsThirtyFieldsAndRejectsThirtyOne() {
        String fields = java.util.stream.IntStream.range(0, 30).mapToObj(i -> field("f" + i)).collect(java.util.stream.Collectors.joining(","));
        assertDoesNotThrow(() -> FormulaMultiFields.normalizeDefinitions("[" + fields + "]"));
        assertThrows(RuntimeException.class, () -> FormulaMultiFields.normalizeDefinitions("[" + fields + "," + field("extra") + "]"));
    }
    @Test void rejectsEmptyDuplicateAndUnsupportedDefinitions() {
        for (String json : java.util.List.of("[]", "{}", "[" + field("same") + "," + field("same") + "]", "[" + field("x").replace("NUMBER", "DATETIME") + "]")) {
            assertThrows(RuntimeException.class, () -> FormulaMultiFields.normalizeDefinitions(json));
        }
    }
    @Test void reorderingAndRenamingDoNotMoveValues() {
        String definitions = "[" + field("second") + "," + field("first").replace("温度", "新名称") + "]";
        String values = FormulaMultiFields.normalizeValues(definitions, "{\"first\":\"12\",\"second\":\"0\"}");
        assertTrue(values.contains("\"first\":\"12\""));
        assertTrue(values.contains("\"second\":\"0\""));
        assertEquals("", FormulaMultiFields.missing(definitions, values));
    }
}
