package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 压槽生产点检专属契约，不改变其他工序与表单类型的解析。 */
final class PressSlotProductionCheckContract {
    private PressSlotProductionCheckContract() {}

    static Map<String, Object> schema(HcStationFormDO form) {
        Map<String, Object> result = JsonUtils.parseObjectQuietly(form.getSchemaJson(), new TypeReference<Map<String, Object>>() {});
        return result == null ? Collections.emptyMap() : result;
    }

    static String text(Object value) { return value == null ? "" : value.toString().trim(); }

    static int modelScore(Map<String, Object> schema, String model) {
        String normalized = text(model).toUpperCase(Locale.ROOT);
        String scope = text(schema.get("modelScope")).toUpperCase(Locale.ROOT);
        if ("COMMON".equals(scope)) return 0;
        if (normalized.isEmpty()) return -1;
        if ("MODEL".equals(scope)) {
            String exact = text(schema.get("modelCode"));
            return !exact.isEmpty() && exact.equalsIgnoreCase(normalized) ? Integer.MAX_VALUE : -1;
        }
        String prefix = text(schema.get("modelPrefix")).toUpperCase(Locale.ROOT);
        return "PREFIX".equals(scope) && !prefix.isEmpty() && normalized.startsWith(prefix) ? prefix.length() : -1;
    }

    static HcStationFormDO resolve(List<HcStationFormDO> forms, String model) {
        List<HcStationFormDO> matches = forms.stream().filter(form -> {
            Map<String, Object> s = schema(form);
            return "true".equalsIgnoreCase(text(s.get("published")))
                    && !"true".equalsIgnoreCase(text(s.get("devOnly")))
                    && !text(form.getFormCode()).toUpperCase(Locale.ROOT).endsWith("_DEV")
                    && "PRODUCTION_CHECK".equalsIgnoreCase(text(s.getOrDefault("formType", s.get("pressSlotFormType"))))
                    && modelScore(s, model) >= 0;
        }).sorted(Comparator.comparingInt((HcStationFormDO form) -> modelScore(schema(form), model)).reversed()).toList();
        if (matches.isEmpty()) throw invalidParamException("未配置型号 " + text(model) + " 对应的已发布压槽生产点检模板");
        if (matches.size() > 1 && modelScore(schema(matches.get(0)), model) == modelScore(schema(matches.get(1)), model)) {
            throw invalidParamException("压槽生产点检存在同等优先级模板，请核查重复配置");
        }
        return matches.get(0);
    }

    static void validateModel(Map<String, Object> schema, String model) {
        if (modelScore(schema, model) < 0) {
            throw invalidParamException("填写型号与当前点检模板适用范围不匹配，请重新选择送检记录");
        }
    }

    static void validateHeader(Map<String, Object> header) {
        for (String field : List.of("submitTime")) {
            String value = text(header.get(field));
            if (!value.isEmpty()) {
                try {
                    LocalDateTime time = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT));
                    if (time.getYear() < 2000) throw new IllegalArgumentException();
                } catch (Exception error) { throw invalidParamException("送检时间格式必须为 yyyy-MM-dd HH:mm:ss"); }
            }
        }
    }

    static void validateItems(List<HcPressSlotProcessParamSaveReqVO.Item> rows, List<HcStationFormItemDO> templates,
                              boolean confirm, boolean strictIdentity) {
        Set<Long> used = new HashSet<>();
        for (HcPressSlotProcessParamSaveReqVO.Item row : rows) {
            if (row == null) throw invalidParamException("生产点检明细不能为空");
            HcStationFormItemDO template = templates.stream().filter(item -> Objects.equals(item.getId(), row.getTemplateItemId())).findFirst().orElse(null);
            if (template == null && !strictIdentity) {
                List<HcStationFormItemDO> candidates = templates.stream().filter(item -> Objects.equals(item.getItemName(), row.getItemName())
                        && Objects.equals(item.getItemSeq(), row.getSortNo() == null ? row.getSeq() : row.getSortNo())).toList();
                if (candidates.size() == 1) template = candidates.get(0);
            }
            if (template == null || !used.add(template.getId())) throw invalidParamException("点检明细与模板不一致或重复，请重新打开表单");
            row.setTemplateItemId(template.getId());
            row.setFieldKey("station_item_" + template.getId());
            row.setItemName(template.getItemName());
            row.setItemCategory(template.getItemCategory());
            row.setStandardValue(template.getStandardText());
            row.setRequiredFlag(Boolean.TRUE.equals(template.getRequiredFlag()));
            row.setValueMode(template.getValueMode());
            row.setDualLabel1(template.getDualLabel1());
            row.setDualLabel2(template.getDualLabel2());
            String mode = text(template.getValueMode()).toUpperCase(Locale.ROOT);
            if (confirm && Boolean.TRUE.equals(template.getRequiredFlag())) {
                boolean empty = "OK_NG".equals(mode) ? StrUtil.isBlank(row.getCheckResult()) : StrUtil.isBlank(row.getActualValue()) || "-".equals(row.getActualValue());
                if (mode.contains("DUAL")) empty |= StrUtil.isBlank(row.getActualValue2());
                if (empty) throw invalidParamException("请填写必填点检项目：" + template.getItemName());
            }
            if (StrUtil.isNotBlank(row.getCheckResult()) && !List.of("OK", "NG").contains(row.getCheckResult())) throw invalidParamException("点检结果只能为 OK 或 NG");
            if ("NUMBER".equals(mode) && StrUtil.isNotBlank(row.getActualValue())) {
                try { new java.math.BigDecimal(row.getActualValue()); } catch (NumberFormatException error) { throw invalidParamException("请输入有效数值：" + template.getItemName()); }
            }
        }
        if (used.size() != templates.size()) throw invalidParamException("点检明细不完整，请重新打开表单");
    }
}
