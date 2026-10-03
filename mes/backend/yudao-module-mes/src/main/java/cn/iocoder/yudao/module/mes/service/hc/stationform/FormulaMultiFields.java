package cn.iocoder.yudao.module.mes.service.hc.stationform;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 配料多字段定义和值的统一校验；定义来自任务快照，不接受客户端覆写。 */
public final class FormulaMultiFields {
    public static final int MAX_FIELDS = 30;
    private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private FormulaMultiFields() { }

    private static JsonNode parse(String json, String fallback) {
        try {
            if (json != null && json.length() > 100_000) throw invalidParamException("多字段内容过长");
            return JSON.readTree(json == null || json.isBlank() ? fallback : json);
        } catch (Exception e) {
            throw invalidParamException("多字段 JSON 无效或含重复字段，请重新加载核对");
        }
    }

    public static String normalizeDefinitions(String json) {
        JsonNode fields = parse(json, "[]");
        if (!fields.isArray() || fields.isEmpty() || fields.size() > MAX_FIELDS) throw invalidParamException("每个项目须配置 1 至 30 个子字段");
        Set<String> keys = new HashSet<>();
        for (JsonNode field : fields) {
            String key = field.path("key").asText();
            String label = field.path("label").asText().trim();
            if (!field.isObject() || !key.matches("[A-Za-z][A-Za-z0-9_]{0,63}") || !keys.add(key)) throw invalidParamException("子字段标识无效或重复");
            if (!field.path("label").isTextual() || label.isEmpty() || label.length() > 80) throw invalidParamException("子字段名称须为 1 至 80 个字符");
            if (!Set.of("TEXT", "NUMBER").contains(field.path("type").asText())) throw invalidParamException("子字段仅支持文本或数字");
            if (!field.path("required").isBoolean()) throw invalidParamException("子字段必填配置无效");
            if (field.has("unit") && (!field.path("unit").isTextual() || field.path("unit").asText().length() > 20)) throw invalidParamException("子字段单位不能超过 20 个字符");
            ((ObjectNode) field).put("label", label);
        }
        return fields.toString();
    }

    public static String normalizeValues(String definitions, String json) {
        JsonNode fields = parse(normalizeDefinitions(definitions), "[]");
        JsonNode values = parse(json, "{}");
        if (!values.isObject()) throw invalidParamException("子字段实际值必须为对象");
        Set<String> keys = new HashSet<>();
        fields.forEach(f -> keys.add(f.path("key").asText()));
        values.fieldNames().forEachRemaining(key -> { if (!keys.contains(key)) throw invalidParamException("未知子字段：" + key); });
        ObjectNode normalized = JSON.createObjectNode();
        for (JsonNode field : fields) {
            String key = field.path("key").asText();
            JsonNode raw = values.get(key);
            if (raw != null && !raw.isNull() && !raw.isTextual() && !raw.isNumber()) throw invalidParamException("子字段值必须为文本或数字");
            String value = raw == null || raw.isNull() ? "" : raw.asText();
            if (value.length() > 2000) throw invalidParamException(field.path("label").asText() + "不能超过 2000 个字符");
            if ("NUMBER".equals(field.path("type").asText()) && !value.isBlank()) {
                try { new BigDecimal(value.trim()); }
                catch (NumberFormatException e) { throw invalidParamException(field.path("label").asText() + "必须填写数字"); }
            }
            normalized.put(key, value);
        }
        return normalized.toString();
    }

    public static String missing(String definitions, String values) {
        JsonNode normalized = parse(normalizeValues(definitions, values), "{}");
        List<String> missing = new ArrayList<>();
        for (JsonNode field : parse(definitions, "[]")) {
            if (field.path("required").asBoolean() && normalized.path(field.path("key").asText()).asText().isBlank()) missing.add(field.path("label").asText());
        }
        return String.join("、", missing);
    }
}
