package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.service.hc.stationform.FormulaMultiFields;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordItemMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 配料运行契约。写入入口必须在同一事务内先锁定计划工序；快照沿用现有记录表头 JSON。 */
@Service
public class HcFormulaStationRuntimeService {
    static final String SNAPSHOT_KEY = "formulaRuntimeV2";
    private static final List<String> REQUIRED_CATEGORIES = List.of("startup", "cleaning", "production");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern MODEL_PATTERN = Pattern.compile("[A-Z]+[0-9]+[A-Z]+[0-9]*");
    @Resource private HcStationFormMapper formMapper;
    @Resource private HcStationFormItemMapper formItemMapper;
    @Resource private HcStationRecordMapper recordMapper;
    @Resource private HcStationRecordItemMapper recordItemMapper;

    @Data
    public static class Snapshot {
        private Integer version = 2;
        private String category;
        private Boolean needConfirm;
        private String modelCode;
        private String matchReason;
        private List<String> boundFormCodes;
        private List<HcFormulaPassWorkItemRespVO> items = new ArrayList<>();
    }

    @Data
    public static class MatchPreview {
        private String modelCode;
        private List<Map<String, Object>> candidates = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
    }

    static Map<String, Object> json(String value) {
        if (StrUtil.isBlank(value)) return new LinkedHashMap<>();
        try {
            return new LinkedHashMap<>(JSONUtil.parseObj(value));
        } catch (RuntimeException e) {
            throw invalidParamException("配料配置或记录快照无法解析，请先修复配置");
        }
    }

    static String text(Object value) { return value == null ? "" : value.toString().trim(); }
    static String upper(Object value) { return text(value).toUpperCase(Locale.ROOT); }

    static String category(HcStationFormDO form) {
        String code = upper(form.getFormCode());
        String name = text(form.getFormName());
        String configured = text(json(form.getSchemaJson()).get("formulaCategory"));
        if ("production-check".equals(configured) || code.startsWith("FORMULA_PROCESS_CHECK") || name.contains("配料生产点检表")) return "production";
        if (code.startsWith("FORMULA_STARTUP_CHECK") || name.contains("开机点检")) return "startup";
        if (code.startsWith("FORMULA_CLEANING_CHECK") || name.contains("清洁保养") || name.contains("清洁点检")) return "cleaning";
        return "other:" + code;
    }

    static boolean dev(HcStationFormDO form) {
        Map<String, Object> schema = json(form.getSchemaJson());
        return upper(form.getFormCode()).endsWith("_DEV")
                || "true".equalsIgnoreCase(text(schema.get("devOnly")))
                || "1".equals(text(schema.get("devOnly")));
    }

    /** 精确优先，其次最长前缀，最后通用。旧 schema 双型号键继续优先 modelPrefix。 */
    static int score(HcStationFormDO form, String modelCode) {
        if (dev(form)) return -1;
        Map<String, Object> schema = json(form.getSchemaJson());
        String scope = upper(schema.get("modelScope"));
        String model = upper(modelCode);
        String code = upper(schema.get("modelCode"));
        String prefix = upper(schema.get("modelPrefix"));
        if ("COMMON".equals(scope)) return 0;
        if ("MODEL".equals(scope) || "EXACT".equals(scope)) return !code.isEmpty() && code.equals(model) ? Integer.MAX_VALUE : -1;
        if ("PREFIX".equals(scope)) return !prefix.isEmpty() && model.startsWith(prefix) ? prefix.length() : -1;
        // 旧开机、清洁模板共用；旧生产模板按原前缀匹配语义兼容。
        if (!"production".equals(category(form))) return 0;
        if (prefix.isEmpty() && "COMMON".equals(code)) return 0;
        if (prefix.isEmpty()) prefix = code;
        if (prefix.isEmpty()) {
            var matcher = MODEL_PATTERN.matcher(upper(form.getFormCode()) + " " + upper(form.getFormName()));
            if (matcher.find()) prefix = matcher.group();
        }
        return !model.isEmpty() && !prefix.isEmpty() && model.startsWith(prefix) ? prefix.length() : -1;
    }

    static List<HcStationFormDO> select(List<HcStationFormDO> forms, String model, Set<String> satisfied) {
        Map<String, List<HcStationFormDO>> groups = forms.stream().filter(f -> !dev(f))
                .collect(Collectors.groupingBy(HcFormulaStationRuntimeService::category, LinkedHashMap::new, Collectors.toList()));
        List<HcStationFormDO> result = new ArrayList<>();
        Set<String> categories = new LinkedHashSet<>(REQUIRED_CATEGORIES);
        categories.addAll(groups.keySet());
        for (String category : categories) {
            if (satisfied.contains(category)) continue;
            List<HcStationFormDO> candidates = groups.getOrDefault(category, List.of());
            int highest = candidates.stream().mapToInt(f -> score(f, model)).max().orElse(-1);
            List<HcStationFormDO> selected = candidates.stream().filter(f -> highest >= 0 && score(f, model) == highest).toList();
            if (selected.size() > 1) throw invalidParamException("配料型号 " + model + " 的 " + categoryName(category) + " 模板冲突："
                    + selected.stream().map(HcStationFormDO::getFormCode).collect(Collectors.joining("、")));
            if (selected.isEmpty() && REQUIRED_CATEGORIES.contains(category)) throw invalidParamException("配料型号 " + model + " 缺少可用的" + categoryName(category) + "模板");
            result.addAll(selected);
        }
        return result;
    }

    static String categoryName(String category) {
        return switch (category) { case "startup" -> "开机点检"; case "cleaning" -> "清洁保养"; case "production" -> "生产点检"; default -> category; };
    }

    public MatchPreview preview(String modelCode) {
        String model = upper(modelCode);
        if (model.isEmpty()) throw invalidParamException("请输入待匹配型号");
        List<HcStationFormDO> forms = formMapper.selectEnabledByProcess("FORMULA");
        MatchPreview preview = new MatchPreview();
        preview.setModelCode(model);
        Set<Long> selectedIds = new HashSet<>();
        // 分类别收集全部冲突和缺失，预览不写入配置或任务。
        Set<String> categories = new LinkedHashSet<>(REQUIRED_CATEGORIES);
        forms.stream().filter(f -> !dev(f)).map(HcFormulaStationRuntimeService::category).forEach(categories::add);
        for (String category : categories) {
            Set<String> satisfied = new HashSet<>(categories);
            satisfied.remove(category);
            try { select(forms, model, satisfied).forEach(f -> selectedIds.add(f.getId())); }
            catch (RuntimeException e) { preview.getErrors().add(e.getMessage()); }
        }
        for (HcStationFormDO form : forms) {
            Map<String, Object> row = new LinkedHashMap<>();
            int score = score(form, model);
            row.put("formCode", form.getFormCode()); row.put("formName", form.getFormName());
            row.put("category", categoryName(category(form))); row.put("selected", selectedIds.contains(form.getId()));
            row.put("reason", dev(form) ? "开发模板已排除" : score < 0 ? "不匹配" : score == Integer.MAX_VALUE ? "精确型号" : score == 0 ? "通用" : "前缀长度 " + score);
            preview.getCandidates().add(row);
        }
        return preview;
    }

    private List<HcStationRecordDO> records(Long planId, Long operationId) {
        List<HcStationRecordDO> all = recordMapper.selectList(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getPlanId, planId).eq(HcStationRecordDO::getPlanOperationId, operationId)
                .eq(HcStationRecordDO::getOperationName, "配料").orderByDesc(HcStationRecordDO::getId));
        Map<String, HcStationRecordDO> latest = new LinkedHashMap<>();
        all.forEach(r -> latest.putIfAbsent(r.getFormCode(), r));
        return new ArrayList<>(latest.values());
    }

    private Snapshot storedSnapshot(HcStationRecordDO record) {
        Object value = json(record.getHeaderDataJson()).get(SNAPSHOT_KEY);
        if (value == null) return null;
        try {
            Snapshot snapshot = JSONUtil.toBean(JSONUtil.toJsonStr(value), Snapshot.class);
            if (!Integer.valueOf(2).equals(snapshot.getVersion()) || snapshot.getCategory() == null || snapshot.getItems() == null) throw new IllegalArgumentException();
            return snapshot;
        } catch (RuntimeException e) { throw invalidParamException("配料记录快照损坏：" + record.getFormCode()); }
    }

    private List<HcStationRecordItemDO> recordItems(HcStationRecordDO record) {
        return recordItemMapper.selectByRecordIds(List.of(record.getId()));
    }

    private Snapshot snapshot(HcStationRecordDO record) {
        Snapshot stored = storedSnapshot(record);
        if (stored != null) return stored;
        // 历史记录仅使用已保存的项目定义，不反向套用现模板的必填和确认开关。
        Snapshot legacy = new Snapshot();
        HcStationFormDO identity = new HcStationFormDO();
        identity.setFormCode(record.getFormCode()); identity.setFormName(record.getFormName());
        legacy.setCategory(category(identity)); legacy.setNeedConfirm(true); legacy.setMatchReason("沿用历史记录");
        List<HcStationRecordItemDO> savedItems = recordItems(record);
        if (savedItems.isEmpty() && !"CONFIRMED".equals(record.getDocStatus())) {
            HcStationFormDO form = formMapper.selectById(record.getFormId());
            if (form != null && "FORMULA".equals(form.getProcessCode()) && !dev(form)) {
                Snapshot unfilled = templateSnapshot(form, "");
                unfilled.setMatchReason("旧空草稿首次绑定现有项目");
                return unfilled;
            }
        }
        for (HcStationRecordItemDO item : savedItems) {
            HcFormulaPassWorkItemRespVO detail = new HcFormulaPassWorkItemRespVO();
            detail.setItemSeq(item.getItemSeq()); detail.setCategory(item.getItemCategory()); detail.setNode(item.getStepNode());
            detail.setItem(item.getItemName()); detail.setStandard(item.getStandardText());
            detail.setValueMode(effectiveMode(item.getValueMode(), item.getItemName(), legacy.getCategory(), false));
            detail.setDualLabel1(item.getDualLabel1()); detail.setDualLabel2(item.getDualLabel2());
            detail.setFieldDefinitionsJson(item.getFieldDefinitionsJson());
            detail.setRequiredFlag(false); detail.setExplicitValueMode(true);
            legacy.getItems().add(detail);
        }
        return legacy;
    }

    static String effectiveMode(String mode, String name, String category, boolean explicit) {
        if (!"production".equals(category)) return "OK_NG";
        if ("MULTI_FIELDS".equalsIgnoreCase(mode)) return "MULTI_FIELDS";
        if (!explicit && text(name).contains("加入")) return "DUAL_TEXT";
        return StrUtil.blankToDefault(mode, "TEXT").toUpperCase(Locale.ROOT);
    }

    private Snapshot templateSnapshot(HcStationFormDO form, String model) {
        Snapshot snapshot = new Snapshot();
        snapshot.setCategory(category(form)); snapshot.setNeedConfirm(!Boolean.FALSE.equals(form.getNeedConfirm()));
        snapshot.setModelCode(model);
        int score = score(form, model);
        snapshot.setMatchReason(score == Integer.MAX_VALUE ? "精确型号 " + model : score == 0 ? "通用模板" : "最长前缀（" + score + " 位）");
        boolean explicit = "2".equals(text(json(form.getSchemaJson()).get("formulaInputModeVersion")));
        for (HcStationFormItemDO item : formItemMapper.selectByFormIds(List.of(form.getId()))) {
            HcFormulaPassWorkItemRespVO detail = new HcFormulaPassWorkItemRespVO();
            detail.setItemSeq(item.getItemSeq()); detail.setCategory(item.getItemCategory()); detail.setNode(item.getStepNode());
            detail.setItem(item.getItemName()); detail.setStandard(item.getStandardText());
            detail.setValueMode(effectiveMode(item.getValueMode(), item.getItemName(), snapshot.getCategory(), explicit));
            detail.setDualLabel1(item.getDualLabel1()); detail.setDualLabel2(item.getDualLabel2());
            detail.setFieldDefinitionsJson(item.getFieldDefinitionsJson());
            detail.setRequiredFlag(Boolean.TRUE.equals(item.getRequiredFlag())); detail.setExplicitValueMode(true);
            detail.setStatus(Boolean.TRUE.equals(item.getRequiredFlag()) ? "" : StrUtil.blankToDefault(item.getDefaultResult(), "OK"));
            snapshot.getItems().add(detail);
        }
        if (snapshot.getItems().isEmpty()) throw invalidParamException("配料模板没有检查项目：" + form.getFormCode());
        Set<Integer> seqs = new HashSet<>();
        for (HcFormulaPassWorkItemRespVO item : snapshot.getItems()) {
            if (item.getItemSeq() == null || !seqs.add(item.getItemSeq())) throw invalidParamException("配料模板项目序号为空或重复：" + form.getFormCode());
            if (!Set.of("TEXT", "DUAL_TEXT", "TIME", "OK_NG", "MULTI_FIELDS").contains(item.getValueMode())) throw invalidParamException("配料模板填写方式暂不支持：" + item.getValueMode());
        }
        for (HcFormulaPassWorkItemRespVO item : snapshot.getItems()) {
            if ("MULTI_FIELDS".equals(item.getValueMode())) item.setFieldDefinitionsJson(FormulaMultiFields.normalizeDefinitions(item.getFieldDefinitionsJson()));
        }
        return snapshot;
    }

    private HcStationRecordDO shell(HcPlanOrderDO plan, HcPlanOrderOperationDO op, HcStationFormDO form, Snapshot snapshot) {
        HcStationRecordDO record = HcStationRecordDO.builder().planId(plan.getId()).planNo(plan.getPlanNo()).planOperationId(op.getId())
                .operationName("配料").formId(form.getId()).formCode(form.getFormCode()).formName(form.getFormName())
                .triggerTimingCode(form.getTriggerTimingCode()).triggerTimingName(form.getTriggerTimingName())
                .equipmentId(op.getEquipmentId()).equipmentName(op.getEquipmentName()).tenantId(plan.getTenantId()).docStatus("PENDING_CHECK").build();
        writeSnapshot(record, snapshot);
        return record;
    }

    private void writeSnapshot(HcStationRecordDO record, Snapshot snapshot) {
        Map<String, Object> header = json(record.getHeaderDataJson());
        header.put(SNAPSHOT_KEY, snapshot); record.setHeaderDataJson(JSONUtil.toJsonStr(header));
    }

    private List<HcStationRecordDO> resolve(HcPlanOrderDO plan, HcPlanOrderOperationDO op, String model) {
        List<HcStationRecordDO> records = records(plan.getId(), op.getId());
        boolean bound = records.stream().anyMatch(r -> storedSnapshot(r) != null && storedSnapshot(r).getBoundFormCodes() != null);
        if (bound) {
            Set<String> codes = records.stream().map(HcStationRecordDO::getFormCode).collect(Collectors.toSet());
            for (HcStationRecordDO record : records) {
                Snapshot snapshot = storedSnapshot(record);
                if (snapshot != null && snapshot.getBoundFormCodes() != null && !codes.containsAll(snapshot.getBoundFormCodes()))
                    throw invalidParamException("配料已绑定表单记录缺失，请核对任务记录");
            }
            return records;
        }
        // 已完工历史只展示其原始记录，避免补上当前版本的表单。
        if ("FINISHED".equals(op.getOperationStatus())) return records;
        Set<String> satisfied = records.stream().map(this::snapshot).map(Snapshot::getCategory).collect(Collectors.toSet());
        List<HcStationFormDO> selected = select(formMapper.selectEnabledByProcess("FORMULA"), model, satisfied);
        for (HcStationFormDO form : selected) records.add(shell(plan, op, form, templateSnapshot(form, model)));
        return records;
    }

    /** 开工时固定整套模板；旧在制任务在第一次有效保存时补齐绑定，不覆盖历史项目。 */
    public void bind(HcPlanOrderDO plan, HcPlanOrderOperationDO op, String model) {
        List<HcStationRecordDO> records = resolve(plan, op, model);
        List<String> codes = records.stream().map(HcStationRecordDO::getFormCode).toList();
        for (HcStationRecordDO record : records) {
            Snapshot snapshot = snapshot(record);
            if (snapshot.getBoundFormCodes() != null) continue;
            snapshot.setBoundFormCodes(codes);
            writeSnapshot(record, snapshot);
            if (record.getId() == null) recordMapper.insert(record); else recordMapper.updateById(record);
        }
    }

    public List<HcFormulaPassWorkRespVO> list(HcPlanOrderDO plan, HcPlanOrderOperationDO op, String model) {
        List<HcFormulaPassWorkRespVO> result = new ArrayList<>();
        for (HcStationRecordDO record : resolve(plan, op, model)) {
            Snapshot snapshot = snapshot(record);
            List<HcFormulaPassWorkItemRespVO> details = details(record, snapshot);
            HcFormulaPassWorkRespVO row = new HcFormulaPassWorkRespVO();
            row.setRecordId(record.getId()); row.setFormId(record.getFormId()); row.setFormCode(record.getFormCode());
            row.setId(record.getId() == null ? record.getFormCode() : record.getId().toString());
            row.setName(record.getFormName()); row.setDisplayName("production".equals(snapshot.getCategory()) ? "配料生产点检表" : record.getFormName());
            row.setTiming(record.getTriggerTimingName()); row.setStatus(StrUtil.blankToDefault(record.getDocStatus(), "PENDING_CHECK"));
            row.setResult(record.getResultStatus()); row.setInspectionResult(record.getInspectionResult());
            row.setRecorder(record.getRecordUserName()); row.setRecorderTime(record.getRecordTime() == null ? "" : TIME_FORMAT.format(record.getRecordTime()));
            row.setConfirmer(record.getConfirmUserName()); row.setConfirmerTime(record.getConfirmTime() == null ? "" : TIME_FORMAT.format(record.getConfirmTime()));
            row.setFormRemark(record.getFormRemark()); row.setConfirmRemark(record.getConfirmRemark());
            row.setNeedConfirm(snapshot.getNeedConfirm()); row.setFormulaCategory(snapshot.getCategory()); row.setMatchReason(snapshot.getMatchReason());
            row.setFrozen(record.getId() != null && storedSnapshot(record) != null); row.setDetails(details);
            String missing = incomplete(details);
            boolean statusComplete = Boolean.FALSE.equals(snapshot.getNeedConfirm()) ? "RECORDED".equals(record.getDocStatus()) : "CONFIRMED".equals(record.getDocStatus());
            boolean ng = hasNg(details) || "NG".equals(record.getResultStatus()) || "NG".equals(record.getInspectionResult());
            row.setComplete(statusComplete && missing.isEmpty() && !ng && !details.isEmpty());
            row.setCompletionMessage(!missing.isEmpty() ? missing : ng ? "存在 NG，请处理异常" : row.getComplete() ? "已完成" : Boolean.FALSE.equals(snapshot.getNeedConfirm()) ? "待提交" : "待确认");
            result.add(row);
        }
        result.sort(Comparator.comparingInt(row -> {
            int index = REQUIRED_CATEGORIES.indexOf(row.getFormulaCategory());
            return index < 0 ? REQUIRED_CATEGORIES.size() : index;
        }));
        return result;
    }

    private List<HcFormulaPassWorkItemRespVO> details(HcStationRecordDO record, Snapshot snapshot) {
        Map<Integer, HcStationRecordItemDO> values = record.getId() == null ? Map.of() : recordItems(record).stream()
                .collect(Collectors.toMap(HcStationRecordItemDO::getItemSeq, i -> i, (a, b) -> a));
        List<HcFormulaPassWorkItemRespVO> result = new ArrayList<>();
        for (HcFormulaPassWorkItemRespVO definition : snapshot.getItems()) {
            HcFormulaPassWorkItemRespVO detail = JSONUtil.toBean(JSONUtil.toJsonStr(definition), HcFormulaPassWorkItemRespVO.class);
            HcStationRecordItemDO value = values.get(definition.getItemSeq());
            if (value != null) {
                detail.setActualValue(value.getActualValue()); detail.setActualValue2(value.getActualValue2());
                detail.setFieldValuesJson(value.getFieldValuesJson());
                detail.setStatus(value.getResultFlag()); detail.setRemark(value.getAbnormalRemark());
            }
            result.add(detail);
        }
        return result;
    }

    static String incomplete(List<HcFormulaPassWorkItemRespVO> details) {
        List<String> missing = new ArrayList<>();
        for (HcFormulaPassWorkItemRespVO item : details) {
            if ("MULTI_FIELDS".equals(item.getValueMode())) {
                String error = FormulaMultiFields.missing(item.getFieldDefinitionsJson(), item.getFieldValuesJson());
                if (!error.isEmpty()) missing.add(item.getItemSeq() + "." + item.getItem() + "（" + error + "）");
                continue;
            }
            if (!Boolean.TRUE.equals(item.getRequiredFlag())) continue;
            boolean empty = switch (text(item.getValueMode())) {
                case "OK_NG" -> !Set.of("OK", "NG").contains(text(item.getStatus()));
                case "DUAL_TEXT" -> StrUtil.isBlank(item.getActualValue()) || StrUtil.isBlank(item.getActualValue2());
                default -> StrUtil.isBlank(item.getActualValue());
            };
            if (empty) missing.add(item.getItemSeq() + "." + item.getItem());
        }
        return missing.isEmpty() ? "" : "请填写必填项目：" + String.join("、", missing);
    }

    static boolean hasNg(List<HcFormulaPassWorkItemRespVO> details) { return details.stream().anyMatch(i -> "NG".equals(i.getStatus())); }

    /** 忽略客户端的名称、标准、模式等定义，只接收序号对应的填写值。 */
    public HcStationRecordDO prepare(HcPlanOrderDO plan, HcPlanOrderOperationDO op, String model, HcFormulaPassWorkSaveReqVO req, boolean confirm) {
        if (req.getRecordId() != null) {
            HcStationRecordDO supplied = recordMapper.selectById(req.getRecordId());
            if (supplied == null || !Objects.equals(plan.getId(), supplied.getPlanId()) || !Objects.equals(op.getId(), supplied.getPlanOperationId())
                    || !Objects.equals(req.getFormCode(), supplied.getFormCode())) throw invalidParamException("配料记录不属于当前计划、工序或表单");
        }
        bind(plan, op, model);
        HcStationRecordDO record = records(plan.getId(), op.getId()).stream().filter(r -> Objects.equals(req.getFormCode(), r.getFormCode())).findFirst()
                .orElseThrow(() -> invalidParamException("该表单未绑定当前配料任务"));
        if (req.getRecordId() != null && !Objects.equals(req.getRecordId(), record.getId())) throw invalidParamException("配料记录已更新，请重新加载");
        if ("CONFIRMED".equals(record.getDocStatus())) throw invalidParamException("已确认的配料记录只允许查看");
        Snapshot snapshot = snapshot(record);
        if (confirm && Boolean.FALSE.equals(snapshot.getNeedConfirm())) throw invalidParamException("该表单无需确认，请使用提交完成");
        if (Boolean.TRUE.equals(req.getSubmit()) && !Boolean.FALSE.equals(snapshot.getNeedConfirm())) throw invalidParamException("该表单需要确认，不能直接提交完成");
        Map<Integer, HcFormulaPassWorkItemReqVO> incoming = new LinkedHashMap<>();
        Set<Integer> allowed = snapshot.getItems().stream().map(HcFormulaPassWorkItemRespVO::getItemSeq).collect(Collectors.toSet());
        for (HcFormulaPassWorkItemReqVO item : req.getDetails() == null ? List.<HcFormulaPassWorkItemReqVO>of() : req.getDetails()) {
            if (item == null || !allowed.contains(item.getItemSeq()) || incoming.putIfAbsent(item.getItemSeq(), item) != null) throw invalidParamException("配料明细序号无效或重复，请重新加载表单");
            if (StrUtil.isNotBlank(item.getStatus()) && !Set.of("OK", "NG").contains(item.getStatus())) throw invalidParamException("检查结果只允许 OK 或 NG");
        }
        List<HcFormulaPassWorkItemReqVO> canonical = new ArrayList<>();
        List<HcFormulaPassWorkItemRespVO> checking = new ArrayList<>();
        for (HcFormulaPassWorkItemRespVO definition : snapshot.getItems()) {
            HcFormulaPassWorkItemReqVO item = JSONUtil.toBean(JSONUtil.toJsonStr(definition), HcFormulaPassWorkItemReqVO.class);
            HcFormulaPassWorkItemReqVO value = incoming.get(definition.getItemSeq());
            item.setActualValue(value == null ? null : value.getActualValue()); item.setActualValue2(value == null ? null : value.getActualValue2());
            if ("MULTI_FIELDS".equals(definition.getValueMode())) {
                item.setFieldValuesJson(FormulaMultiFields.normalizeValues(definition.getFieldDefinitionsJson(), value == null ? null : value.getFieldValuesJson()));
            }
            item.setStatus(value == null ? "" : text(value.getStatus())); item.setRemark(value == null ? null : value.getRemark());
            canonical.add(item);
            HcFormulaPassWorkItemRespVO check = JSONUtil.toBean(JSONUtil.toJsonStr(item), HcFormulaPassWorkItemRespVO.class);
            check.setRequiredFlag(definition.getRequiredFlag()); checking.add(check);
        }
        for (String status : List.of(text(req.getResult()), text(req.getInspectionResult()))) {
            if (!status.isEmpty() && !Set.of("OK", "NG").contains(status)) throw invalidParamException("表单结果只允许 OK 或 NG");
        }
        boolean completeAction = confirm || Boolean.TRUE.equals(req.getSubmit());
        if (completeAction) {
            String missing = incomplete(checking);
            if (!missing.isEmpty()) throw invalidParamException(missing);
            if (checking.isEmpty()) throw invalidParamException("配料记录没有检查项目，请核对历史记录");
            if (hasNg(checking) || "NG".equals(req.getResult()) || "NG".equals(req.getInspectionResult())) throw invalidParamException("存在 NG，请先保存并处理异常后再完成表单");
        }
        req.setDetails(canonical);
        record.setDocStatus(confirm ? "CONFIRMED" : Boolean.TRUE.equals(req.getSubmit()) ? "RECORDED" : "DRAFT");
        return record;
    }

    public void assertFinished(HcPlanOrderDO plan, HcPlanOrderOperationDO op, String model) {
        List<HcFormulaPassWorkRespVO> rows = list(plan, op, model);
        Set<String> categories = rows.stream().map(HcFormulaPassWorkRespVO::getFormulaCategory).collect(Collectors.toSet());
        if (!categories.containsAll(REQUIRED_CATEGORIES)) throw invalidParamException("配料必要表单不完整，不能报工");
        List<String> pending = rows.stream().filter(row -> !Boolean.TRUE.equals(row.getComplete()))
                .map(row -> row.getDisplayName() + "：" + row.getCompletionMessage()).toList();
        if (!pending.isEmpty()) throw invalidParamException(String.join("；", pending));
    }
}
