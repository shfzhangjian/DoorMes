package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * 受控公式工具：只允许引用当前规则字段、四则运算、括号和少量白名单函数。
 */
final class QmsEntryRuleFormulaSupport {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> FUNCTIONS = Set.of("avg", "std", "round");
    private static final String DATA_RULE_KEY = "dataRule";
    private static final String INPUT_FIELDS_KEY = "inputFields";
    private static final String RESULT_FIELDS_KEY = "resultFields";
    private static final String JUDGMENT_METRIC_KEY = "judgmentMetric";

    private QmsEntryRuleFormulaSupport() {
    }

    static void validateTemplateParams(String templateParams) {
        if (!StringUtils.hasText(templateParams)) {
            return;
        }
        RuleConfig config = parseRule(templateParams);
        if (!config.hasDataRule()) {
            return;
        }
        if (config.inputFields.isEmpty()) {
            throw new IllegalArgumentException("数据处理规则必须维护输入字段");
        }
        if (config.resultFields.isEmpty()) {
            throw new IllegalArgumentException("数据处理规则必须维护结果定义");
        }
        Set<String> availableCodes = new HashSet<>(config.inputFields);
        boolean hasJudgment = false;
        for (ResultField resultField : config.resultFields) {
            if (!StringUtils.hasText(resultField.code) || !StringUtils.hasText(resultField.formula)) {
                throw new IllegalArgumentException("结果定义必须维护编码和公式");
            }
            new FormulaParser(resultField.formula, availableCodes, Collections.emptyMap(), true).parseForValidation();
            availableCodes.add(resultField.code);
            hasJudgment = hasJudgment || resultField.judgment;
        }
        if (StringUtils.hasText(config.judgmentMetric)) {
            String judgmentMetric = config.judgmentMetric;
            hasJudgment = config.resultFields.stream().anyMatch(item -> judgmentMetric.equals(item.code));
        }
        if (!hasJudgment) {
            throw new IllegalArgumentException("数据处理规则必须指定判定结果");
        }
    }

    static boolean hasDataRule(String templateParams) {
        return parseRule(templateParams).hasDataRule();
    }

    static String resolveJudgmentMetric(String templateParams, String fallback) {
        RuleConfig config = parseRule(templateParams);
        if (!config.hasDataRule()) {
            return fallback;
        }
        if (StringUtils.hasText(config.judgmentMetric)) {
            return config.judgmentMetric;
        }
        return config.resultFields.stream()
                .filter(item -> item.judgment)
                .map(item -> item.code)
                .findFirst()
                .orElse(fallback);
    }

    static CalculationResult calculate(String templateParams, Map<String, Object> rawValues) {
        RuleConfig config = parseRule(templateParams);
        if (!config.hasDataRule()) {
            return CalculationResult.empty();
        }
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (String inputCode : config.inputFields) {
            BigDecimal value = toDecimal(rawValues.get(inputCode));
            if (value == null && config.requiredFields.contains(inputCode)) {
                throw new IllegalArgumentException("缺少必填字段：" + inputCode);
            }
            if (value != null) {
                values.put(inputCode, value);
            }
        }
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        for (ResultField resultField : config.resultFields) {
            BigDecimal value = new FormulaParser(resultField.formula, values.keySet(), values, false).parse();
            Integer precision = resultField.precision == null ? 6 : resultField.precision;
            value = value.setScale(Math.max(0, precision), RoundingMode.HALF_UP);
            values.put(resultField.code, value);
            results.put(resultField.code, value);
        }
        String judgmentMetric = resolveJudgmentMetric(templateParams, null);
        return new CalculationResult(results, judgmentMetric, results.get(judgmentMetric));
    }

    private static RuleConfig parseRule(String templateParams) {
        Map<String, Object> params = parseMap(templateParams);
        Map<String, Object> dataRule = asMap(params.get(DATA_RULE_KEY));
        if (dataRule.isEmpty()
                && (params.containsKey(INPUT_FIELDS_KEY) || params.containsKey(RESULT_FIELDS_KEY))) {
            dataRule = params;
        }
        RuleConfig config = new RuleConfig();
        config.dataRule = dataRule;
        if (dataRule.isEmpty()) {
            return config;
        }
        for (Object item : asCollection(dataRule.get(INPUT_FIELDS_KEY))) {
            Map<String, Object> field = asMap(item);
            String code = asString(field.get("code"));
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("输入字段编码不能为空");
            }
            config.inputFields.add(code);
            if (!Boolean.FALSE.equals(field.get("required"))) {
                config.requiredFields.add(code);
            }
        }
        for (Object item : asCollection(dataRule.get(RESULT_FIELDS_KEY))) {
            Map<String, Object> field = asMap(item);
            ResultField resultField = new ResultField();
            resultField.code = asString(field.get("code"));
            resultField.formula = asString(field.get("formula"));
            resultField.judgment = Boolean.TRUE.equals(field.get("judgment"));
            resultField.precision = toInteger(field.get("precision"));
            config.resultFields.add(resultField);
        }
        config.judgmentMetric = asString(dataRule.get(JUDGMENT_METRIC_KEY));
        return config;
    }

    private static Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = JsonUtils.parseObjectQuietly(json, MAP_TYPE);
        return map == null ? Collections.emptyMap() : map;
    }

    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, child) -> result.put(String.valueOf(key), child));
        return result;
    }

    private static Collection<?> asCollection(Object value) {
        return value instanceof Collection<?> collection ? collection : Collections.emptyList();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static Integer toInteger(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    static final class CalculationResult {
        private final Map<String, BigDecimal> results;
        private final String judgmentMetric;
        private final BigDecimal judgmentValue;

        private CalculationResult(Map<String, BigDecimal> results, String judgmentMetric, BigDecimal judgmentValue) {
            this.results = results;
            this.judgmentMetric = judgmentMetric;
            this.judgmentValue = judgmentValue;
        }

        static CalculationResult empty() {
            return new CalculationResult(Collections.emptyMap(), null, null);
        }

        Map<String, BigDecimal> getResults() {
            return results;
        }

        String getJudgmentMetric() {
            return judgmentMetric;
        }

        BigDecimal getJudgmentValue() {
            return judgmentValue;
        }
    }

    private static final class RuleConfig {
        private Map<String, Object> dataRule = Collections.emptyMap();
        private final List<String> inputFields = new ArrayList<>();
        private final Set<String> requiredFields = new HashSet<>();
        private final List<ResultField> resultFields = new ArrayList<>();
        private String judgmentMetric;

        private boolean hasDataRule() {
            return !dataRule.isEmpty();
        }
    }

    private static final class ResultField {
        private String code;
        private String formula;
        private boolean judgment;
        private Integer precision;
    }

    private static final class FormulaParser {
        private final String formula;
        private final Set<String> availableCodes;
        private final Map<String, BigDecimal> values;
        private final boolean validationOnly;
        private int index;

        private FormulaParser(String formula, Set<String> availableCodes, Map<String, BigDecimal> values, boolean validationOnly) {
            this.formula = formula == null ? "" : formula;
            this.availableCodes = availableCodes;
            this.values = values;
            this.validationOnly = validationOnly;
        }

        private void parseForValidation() {
            parse();
        }

        private BigDecimal parse() {
            BigDecimal value = parseExpression();
            skipBlank();
            if (index != formula.length()) {
                throw new IllegalArgumentException("公式存在不支持的字符");
            }
            return value;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (true) {
                skipBlank();
                if (match('+')) {
                    value = value.add(parseTerm());
                } else if (match('-')) {
                    value = value.subtract(parseTerm());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();
            while (true) {
                skipBlank();
                if (match('*')) {
                    value = value.multiply(parseFactor());
                } else if (match('/')) {
                    BigDecimal right = parseFactor();
                    if (!validationOnly && BigDecimal.ZERO.compareTo(right) == 0) {
                        throw new IllegalArgumentException("公式除数不能为 0");
                    }
                    value = validationOnly ? BigDecimal.ONE : value.divide(right, 10, RoundingMode.HALF_UP);
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseFactor() {
            skipBlank();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return parseFactor().negate();
            }
            if (match('(')) {
                BigDecimal value = parseExpression();
                expect(')');
                return value;
            }
            if (isIdentifierStart(peek())) {
                String identifier = parseIdentifier();
                skipBlank();
                if (match('(')) {
                    return parseFunction(identifier);
                }
                if (!availableCodes.contains(identifier)) {
                    throw new IllegalArgumentException("公式引用了未定义字段：" + identifier);
                }
                if (values.isEmpty()) {
                    return BigDecimal.ONE;
                }
                BigDecimal value = values.get(identifier);
                if (value == null) {
                    throw new IllegalArgumentException("字段尚未填写：" + identifier);
                }
                return value;
            }
            return parseNumber();
        }

        private BigDecimal parseFunction(String name) {
            String function = name.toLowerCase(Locale.ROOT);
            if (!FUNCTIONS.contains(function)) {
                throw new IllegalArgumentException("公式函数不在白名单：" + name);
            }
            List<BigDecimal> args = new ArrayList<>();
            skipBlank();
            if (!match(')')) {
                do {
                    args.add(parseExpression());
                    skipBlank();
                } while (match(','));
                expect(')');
            }
            if ("round".equals(function)) {
                if (args.isEmpty()) {
                    throw new IllegalArgumentException("round 函数至少需要 1 个参数");
                }
                int scale = args.size() > 1 ? args.get(1).intValue() : 2;
                return args.get(0).setScale(Math.max(0, scale), RoundingMode.HALF_UP);
            }
            if (args.isEmpty()) {
                throw new IllegalArgumentException(function + " 函数至少需要 1 个参数");
            }
            BigDecimal total = args.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = total.divide(BigDecimal.valueOf(args.size()), 10, RoundingMode.HALF_UP);
            if ("avg".equals(function)) {
                return avg;
            }
            double variance = args.stream()
                    .map(value -> value.subtract(avg).doubleValue())
                    .mapToDouble(diff -> diff * diff)
                    .sum() / args.size();
            return BigDecimal.valueOf(Math.sqrt(variance));
        }

        private BigDecimal parseNumber() {
            int start = index;
            while (Character.isDigit(peek()) || peek() == '.') {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("公式语法错误");
            }
            return new BigDecimal(formula.substring(start, index));
        }

        private String parseIdentifier() {
            int start = index;
            index++;
            while (Character.isLetterOrDigit(peek()) || peek() == '_') {
                index++;
            }
            return formula.substring(start, index);
        }

        private void expect(char expected) {
            skipBlank();
            if (!match(expected)) {
                throw new IllegalArgumentException("公式缺少：" + expected);
            }
        }

        private boolean match(char expected) {
            if (peek() != expected) {
                return false;
            }
            index++;
            return true;
        }

        private char peek() {
            return index >= formula.length() ? '\0' : formula.charAt(index);
        }

        private void skipBlank() {
            while (Character.isWhitespace(peek())) {
                index++;
            }
        }

        private boolean isIdentifierStart(char value) {
            return Character.isLetter(value) || value == '_';
        }
    }
}
