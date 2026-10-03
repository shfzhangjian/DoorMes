package cn.iocoder.yudao.module.mes.service.hc.processform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormTemplateDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormTemplateOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcProcessFormServiceImpl implements HcProcessFormService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final Integer STATION_FORM_STATUS_ENABLED = 1;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String AREA_TYPE_HEADER = "HEADER";
    private static final String AREA_TYPE_DETAIL = "DETAIL";
    private static final String AREA_TYPE_FOOTER = "FOOTER";
    private static final String MODEL_SCOPE_COMMON = "COMMON";
    private static final String MODEL_SCOPE_MODEL = "MODEL";
    private static final String MODEL_SCOPE_PREFIX = "PREFIX";
    private static final String RECORD_STATUS_DRAFT = "DRAFT";
    private static final String RECORD_STATUS_SUBMITTED = "SUBMITTED";
    private static final String RECORD_STATUS_CONFIRMED = "CONFIRMED";
    private static final String FAI_PROCESS_SELF_CHECK_PROCESS_CODE = "FAI_PROCESS_SELF_CHECK";
    private static final String CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE = "CUT_ROUND_FQC_SELF_CHECK";
    private static final String RESULT_OK = "OK";
    private static final String RESULT_NG = "NG";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private HcProcessFormRecordMapper recordMapper;
    @Resource
    private HcProcessFormRecordItemMapper recordItemMapper;
    @Resource
    private HcStationFormMapper stationFormMapper;
    @Resource
    private HcStationFormItemMapper stationFormItemMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(HcProcessFormRecordSaveReqVO reqVO) {
        if ("PRESS_SLOT".equals(reqVO.getProcessCode()) && "PRODUCTION_CHECK".equals(reqVO.getFormType())) {
            throw invalidParamException("压槽生产点检请使用按送检片号关联的填写入口");
        }
        HcProcessFormTemplateDetailRespVO detail = getRuntimeTemplateDetail(reqVO.getTemplateId(), null,
                reqVO.getProcessCode(), reqVO.getModelCode(), reqVO.getFormType());
        if (detail == null || detail.getCurrentVersion() == null) {
            throw invalidParamException("表单模板未配置当前版本");
        }
        HcProcessFormRecordDO record = buildRecord(reqVO, detail);
        record.setRecordNo(generateRecordNo());
        record.setRecordStatus(RECORD_STATUS_DRAFT);
        record.setTenantId(currentTenantId());
        applyFillUser(record);
        applyFaiProcessSelfCheckManualHeader(record);
        recordMapper.insert(record);
        saveRecordItems(record, reqVO.getItems(), detail.getItems());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(HcProcessFormRecordSaveReqVO reqVO) {
        HcProcessFormRecordDO old = validateRecordExists(reqVO.getId());
        guardPressSlotProductionDedicatedWrite(old);
        if (RECORD_STATUS_CONFIRMED.equals(old.getRecordStatus())) {
            throw invalidParamException("已确认记录不允许修改");
        }
        HcProcessFormTemplateDetailRespVO detail = getRuntimeTemplateDetail(old.getTemplateId(), old.getTemplateCode(),
                old.getProcessCode(), old.getModelCode(), old.getFormType());
        HcProcessFormRecordDO updateObj = BeanUtils.toBean(reqVO, HcProcessFormRecordDO.class);
        updateObj.setId(old.getId());
        updateObj.setTemplateId(old.getTemplateId());
        updateObj.setVersionId(old.getVersionId());
        updateObj.setTemplateCode(old.getTemplateCode());
        updateObj.setTemplateName(old.getTemplateName());
        updateObj.setProcessCode(old.getProcessCode());
        updateObj.setProcessName(old.getProcessName());
        updateObj.setFormType(old.getFormType());
        updateObj.setFormTypeName(old.getFormTypeName());
        updateObj.setRecordStatus(old.getRecordStatus());
        updateObj.setFillUserId(firstNonNull(old.getFillUserId(), SecurityFrameworkUtils.getLoginUserId()));
        updateObj.setFillUserName(firstNotBlank(old.getFillUserName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setFillTime(firstNonNull(old.getFillTime(), LocalDateTime.now()));
        updateObj.setResultStatus(calcResult(reqVO.getItems()));
        if (updateObj.getRecordDate() == null) {
            updateObj.setRecordDate(old.getRecordDate());
        }
        updateObj.setModelCode(firstNotBlank(updateObj.getModelCode(), old.getModelCode(), "COMMON"));
        updateObj.setModelName(firstNotBlank(updateObj.getModelName(), old.getModelName(), "通用"));
        applyFaiProcessSelfCheckManualHeader(updateObj);
        recordMapper.updateById(updateObj);
        recordItemMapper.deleteByRecordId(old.getId());
        saveRecordItems(old, reqVO.getItems(), detail == null ? List.of() : detail.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        HcProcessFormRecordDO old = validateRecordExists(id);
        if (RECORD_STATUS_CONFIRMED.equals(old.getRecordStatus())) {
            throw invalidParamException("已确认记录不允许删除");
        }
        recordItemMapper.deleteByRecordId(id);
        recordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRecord(Long id) {
        HcProcessFormRecordDO old = validateRecordExists(id);
        guardPressSlotProductionDedicatedWrite(old);
        List<HcProcessFormRecordItemDO> items = recordItemMapper.selectListByRecordId(id);
        HcProcessFormRecordDO updateObj = new HcProcessFormRecordDO();
        updateObj.setId(id);
        updateObj.setRecordStatus(RECORD_STATUS_SUBMITTED);
        updateObj.setResultStatus(calcResultFromSaved(items));
        updateObj.setFillUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setFillUserName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setFillTime(old.getFillTime() == null ? LocalDateTime.now() : old.getFillTime());
        recordMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecord(Long id) {
        HcProcessFormRecordDO old = validateRecordExists(id);
        updateConfirmedRecord(old, SecurityFrameworkUtils.getLoginUserId(),
                firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecordBySigner(Long id, Long confirmUserId) {
        HcProcessFormRecordDO old = validateRecordExists(id);
        adminUserApi.validateUser(confirmUserId);
        AdminUserRespDTO signer = adminUserApi.getUser(confirmUserId);
        if (signer == null) {
            throw invalidParamException("确认人不存在或已禁用");
        }
        updateConfirmedRecord(old, signer.getId(), firstNotBlank(signer.getNickname(), "系统"));
    }

    private void guardPressSlotProductionDedicatedWrite(HcProcessFormRecordDO record) {
        if ("PRESS_SLOT".equals(record.getProcessCode()) && "PRODUCTION_CHECK".equals(record.getFormType())) {
            throw invalidParamException("压槽生产点检请在对应送检片号的表单中保存或确认");
        }
    }

    private void updateConfirmedRecord(HcProcessFormRecordDO old, Long confirmUserId, String confirmUserName) {
        guardPressSlotProductionDedicatedWrite(old);
        LocalDateTime confirmTime = LocalDateTime.now();
        HcProcessFormRecordDO updateObj = new HcProcessFormRecordDO();
        updateObj.setId(old.getId());
        updateObj.setRecordStatus(RECORD_STATUS_CONFIRMED);
        updateObj.setConfirmUserId(confirmUserId);
        updateObj.setConfirmUserName(confirmUserName);
        updateObj.setConfirmTime(confirmTime);
        updateObj.setHeaderDataJson(syncConfirmHeaderData(old.getHeaderDataJson(), confirmUserName, confirmTime));
        recordMapper.updateById(updateObj);
    }

    @Override
    public HcProcessFormRecordDO getRecord(Long id) {
        return recordMapper.selectById(id);
    }

    @Override
    public PageResult<HcProcessFormRecordDO> getRecordPage(HcProcessFormRecordPageReqVO reqVO) {
        return recordMapper.selectPage(reqVO);
    }

    @Override
    public HcProcessFormRecordDetailRespVO getRecordDetail(Long id) {
        HcProcessFormRecordDO record = recordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        HcProcessFormRecordDetailRespVO respVO = BeanUtils.toBean(record, HcProcessFormRecordDetailRespVO.class);
        respVO.setTemplate(getRuntimeTemplateDetail(record.getTemplateId(), record.getTemplateCode(),
                record.getProcessCode(), record.getModelCode(), record.getFormType()));
        respVO.setItems(BeanUtils.toBean(recordItemMapper.selectListByRecordId(id), HcProcessFormRecordItemRespVO.class));
        return respVO;
    }

    private HcProcessFormTemplateDetailRespVO getRuntimeTemplateDetail(Long templateId, String templateCode,
                                                                       String processCode, String modelCode,
                                                                       String formType) {
        HcStationFormDO form = null;
        if (StrUtil.isNotBlank(templateCode)) {
            form = stationFormMapper.selectEnabledByCode(templateCode);
        }
        if (form == null && templateId != null) {
            HcStationFormDO candidate = stationFormMapper.selectById(templateId);
            if (isEnabledStationForm(candidate)) {
                form = candidate;
            }
        }
        if (form == null) {
            form = resolveStationForm(processCode, modelCode, formType);
        }
        return form == null ? null : buildStationTemplateDetail(form);
    }

    private HcStationFormDO resolveStationForm(String processCode, String modelCode, String formType) {
        List<HcStationFormDO> candidates = stationFormMapper.selectEnabledList(processCode).stream()
                .filter(form -> {
                    Map<String, Object> schema = parseSchema(form.getSchemaJson());
                    return StrUtil.isBlank(formType) || Objects.equals(resolveStationFormType(form, schema), formType);
                })
                .filter(form -> {
                    Map<String, Object> schema = parseSchema(form.getSchemaJson());
                    return matchesModel(resolveStationModelScope(schema), resolveStationModelCode(schema), modelCode);
                })
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .max((left, right) -> Integer.compare(
                        modelMatchScore(resolveStationModelScope(parseSchema(left.getSchemaJson())),
                                resolveStationModelCode(parseSchema(left.getSchemaJson())), modelCode),
                        modelMatchScore(resolveStationModelScope(parseSchema(right.getSchemaJson())),
                                resolveStationModelCode(parseSchema(right.getSchemaJson())), modelCode)))
                .orElse(candidates.get(0));
    }

    private HcProcessFormTemplateOptionRespVO buildStationTemplateOption(HcStationFormDO form) {
        Map<String, Object> schema = parseSchema(form.getSchemaJson());
        HcProcessFormTemplateOptionRespVO respVO = new HcProcessFormTemplateOptionRespVO();
        respVO.setId(form.getId());
        respVO.setTemplateCode(form.getFormCode());
        respVO.setTemplateName(form.getFormName());
        respVO.setProcessCode(form.getProcessCode());
        respVO.setProcessName(form.getProcessName());
        respVO.setModelScope(resolveStationModelScope(schema));
        respVO.setModelCode(resolveStationModelCode(schema));
        respVO.setModelName(resolveStationModelName(schema, respVO.getModelCode()));
        respVO.setFormType(resolveStationFormType(form, schema));
        respVO.setFormTypeName(resolveStationFormTypeName(form, schema, respVO.getFormType()));
        return respVO;
    }

    private HcProcessFormTemplateDetailRespVO buildStationTemplateDetail(HcStationFormDO form) {
        HcProcessFormTemplateOptionRespVO option = buildStationTemplateOption(form);
        HcProcessFormTemplateDetailRespVO respVO = new HcProcessFormTemplateDetailRespVO();
        respVO.setId(option.getId());
        respVO.setTemplateCode(option.getTemplateCode());
        respVO.setTemplateName(option.getTemplateName());
        respVO.setProcessCode(option.getProcessCode());
        respVO.setProcessName(option.getProcessName());
        respVO.setModelScope(option.getModelScope());
        respVO.setModelCode(option.getModelCode());
        respVO.setModelName(option.getModelName());
        respVO.setFormType(option.getFormType());
        respVO.setFormTypeName(option.getFormTypeName());
        respVO.setCurrentVersionId(form.getId());
        respVO.setStatus(STATUS_ACTIVE);
        respVO.setNeedConfirm(form.getNeedConfirm());
        respVO.setSortNo(form.getSortNo());
        respVO.setRemark(form.getRemark());
        respVO.setCreateTime(form.getCreateTime());
        respVO.setCurrentVersion(buildStationCurrentVersion(form));
        respVO.setSections(List.of());
        respVO.setItems(stationFormItemMapper.selectByFormId(form.getId()).stream()
                .map(item -> buildStationTemplateItem(form, item))
                .toList());
        return respVO;
    }

    private HcProcessFormVersionRespVO buildStationCurrentVersion(HcStationFormDO form) {
        Map<String, Object> schema = parseSchema(form.getSchemaJson());
        HcProcessFormVersionRespVO version = new HcProcessFormVersionRespVO();
        version.setId(form.getId());
        version.setTemplateId(form.getId());
        version.setTemplateCode(form.getFormCode());
        version.setVersionNo(firstNotBlank(schemaText(schema, "version", "versionNo"), "station-form"));
        version.setIsCurrent(true);
        version.setEffectiveDate(form.getCreateTime() == null ? LocalDate.now() : form.getCreateTime().toLocalDate());
        version.setSourceFileName(schemaText(schema, "sourceExcel", "sourceFileName"));
        version.setSheetJson(form.getPresetItemsJson());
        version.setLayoutJson(form.getSchemaJson());
        version.setParseStatus("SUCCESS");
        return version;
    }

    private HcProcessFormItemRespVO buildStationTemplateItem(HcStationFormDO form, HcStationFormItemDO item) {
        HcProcessFormItemRespVO respVO = new HcProcessFormItemRespVO();
        respVO.setId(item.getId());
        respVO.setTemplateId(form.getId());
        respVO.setVersionId(form.getId());
        respVO.setAreaType(resolveStationAreaType(item));
        respVO.setItemSeq(item.getItemSeq());
        respVO.setFieldKey("station_" + form.getId() + "_" + firstNonNull(item.getId(), Long.valueOf(firstNonNull(item.getItemSeq(), 0))));
        respVO.setFieldLabel(item.getItemName());
        respVO.setItemCategory(item.getItemCategory());
        respVO.setStepNode(item.getStepNode());
        respVO.setStandardText(item.getStandardText());
        respVO.setValueMode(firstNotBlank(item.getValueMode(), "TEXT"));
        respVO.setControlType(resolveStationControlType(respVO.getValueMode()));
        respVO.setDefaultValue(resolveStationDefaultValue(item));
        respVO.setDefaultResult(resolveStationDefaultResult(item));
        respVO.setRequiredFlag(item.getRequiredFlag());
        respVO.setRemark(item.getRemark());
        return respVO;
    }

    private String resolveStationAreaType(HcStationFormItemDO item) {
        String category = firstNotBlank(item.getItemCategory(), "");
        if (containsAnyIgnoreCase(category, "HEADER", "表头")) {
            return AREA_TYPE_HEADER;
        }
        if (containsAnyIgnoreCase(category, "FOOTER", "页脚")) {
            return AREA_TYPE_FOOTER;
        }
        Integer itemSeq = item.getItemSeq();
        if (itemSeq != null && itemSeq >= 900) {
            return AREA_TYPE_FOOTER;
        }
        return AREA_TYPE_DETAIL;
    }

    private String resolveStationControlType(String valueMode) {
        String mode = StrUtil.blankToDefault(valueMode, "TEXT").toUpperCase();
        if ("OK_NG".equals(mode) || "RADIO".equals(mode) || "BOOLEAN".equals(mode)) {
            return "RADIO";
        }
        if ("DATE".equals(mode)) {
            return "DATE";
        }
        if ("TIME".equals(mode)) {
            return "TIME";
        }
        return "INPUT";
    }

    private String resolveStationDefaultValue(HcStationFormItemDO item) {
        String valueMode = StrUtil.blankToDefault(item.getValueMode(), "TEXT").toUpperCase();
        String defaultResult = item.getDefaultResult();
        if (StrUtil.isBlank(defaultResult) || "OK_NG".equals(valueMode) || isResultFlag(defaultResult)) {
            return null;
        }
        return defaultResult;
    }

    private String resolveStationDefaultResult(HcStationFormItemDO item) {
        String defaultResult = item.getDefaultResult();
        return isResultFlag(defaultResult) ? defaultResult.toUpperCase() : RESULT_OK;
    }

    private String resolveStationFormType(HcStationFormDO form, Map<String, Object> schema) {
        String configured = schemaText(schema, "processFormType", "formType", "adhesive1FormType",
                "adhesive2FormType", "pressSlotFormType", "runtimeFormType");
        if (StrUtil.isNotBlank(configured)) {
            return configured;
        }
        String categoryMapped = mapFormCategory(schemaText(schema, "formCategory", "category"));
        if (StrUtil.isNotBlank(categoryMapped)) {
            return categoryMapped;
        }
        String code = StrUtil.blankToDefault(form.getFormCode(), "").toUpperCase();
        if (code.contains("STARTUP_CHECK")) {
            return "STARTUP_CHECK";
        }
        if (code.contains("CLEANING_CHECK")) {
            return "CLEANING_CHECK";
        }
        if (code.contains("PRODUCTION_CHECK") || code.contains("PROCESS_CHECK")) {
            return "PRODUCTION_CHECK";
        }
        if (code.contains("INTERMEDIATE") || code.contains("MIDDLE_PRODUCT")) {
            return "INTERMEDIATE_RECORD";
        }
        if (code.contains("PROCESS_PARAM")) {
            return "PROCESS_PARAM";
        }
        return firstNotBlank(form.getTriggerTimingCode(), "OTHER");
    }

    private String resolveStationFormTypeName(HcStationFormDO form, Map<String, Object> schema, String formType) {
        return firstNotBlank(schemaText(schema, "formTypeName", "displayName"),
                mapFormTypeName(formType), form.getTriggerTimingName(), form.getFormName(), formType);
    }

    private String resolveStationModelScope(Map<String, Object> schema) {
        String scope = StrUtil.blankToDefault(schemaText(schema, "modelScope"), "");
        if (StrUtil.isBlank(scope)) {
            String modelCode = schemaText(schema, "modelCode", "modelCodes", "modelCodePrefix", "modelPrefix", "sourceModelCode");
            return StrUtil.isBlank(modelCode) || "COMMON".equalsIgnoreCase(modelCode) ? MODEL_SCOPE_COMMON : MODEL_SCOPE_MODEL;
        }
        String normalized = scope.toUpperCase();
        if ("ALL".equals(normalized) || "通用".equals(scope)) {
            return MODEL_SCOPE_COMMON;
        }
        if (MODEL_SCOPE_PREFIX.equals(normalized)) {
            return MODEL_SCOPE_PREFIX;
        }
        return MODEL_SCOPE_COMMON.equals(normalized) ? MODEL_SCOPE_COMMON : MODEL_SCOPE_MODEL;
    }

    private String resolveStationModelCode(Map<String, Object> schema) {
        String scope = resolveStationModelScope(schema);
        if (MODEL_SCOPE_COMMON.equals(scope)) {
            return "COMMON";
        }
        return firstNotBlank(schemaText(schema, "modelCode", "modelCodes", "modelCodePrefix", "modelPrefix", "sourceModelCode"), "COMMON");
    }

    private String resolveStationModelName(Map<String, Object> schema, String modelCode) {
        return firstNotBlank(schemaText(schema, "modelName", "sourceModelName"), "COMMON".equals(modelCode) ? "通用" : modelCode);
    }

    private boolean matchesModel(String modelScope, String templateModelCode, String modelCode) {
        return modelMatchScore(modelScope, templateModelCode, modelCode) > 0;
    }

    private int modelMatchScore(String modelScope, String templateModelCode, String modelCode) {
        if (StrUtil.isBlank(modelCode)) {
            return MODEL_SCOPE_COMMON.equals(modelScope) || "COMMON".equalsIgnoreCase(templateModelCode) ? 2 : 1;
        }
        if (MODEL_SCOPE_COMMON.equals(modelScope) || "COMMON".equalsIgnoreCase(templateModelCode)) {
            return 1;
        }
        for (String candidate : splitModelCodes(templateModelCode)) {
            if (candidate.equalsIgnoreCase(modelCode)) {
                return 4;
            }
            if (MODEL_SCOPE_PREFIX.equals(modelScope) && modelCode.toUpperCase().startsWith(candidate.toUpperCase())) {
                return 3;
            }
        }
        return 0;
    }

    private List<String> splitModelCodes(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return List.of(value.split("[,，;；/、|\\s_]+")).stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private Map<String, Object> parseSchema(String schemaJson) {
        if (StrUtil.isBlank(schemaJson)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObjectQuietly(schemaJson, MAP_TYPE);
        return parsed == null ? Map.of() : parsed;
    }

    private String syncConfirmHeaderData(String headerDataJson, String confirmUserName, LocalDateTime confirmTime) {
        Map<String, Object> header = new LinkedHashMap<>();
        Map<String, Object> parsed = JsonUtils.parseObjectQuietly(headerDataJson, MAP_TYPE);
        if (parsed != null) {
            header.putAll(parsed);
        }
        String formattedConfirmTime = confirmTime.format(DATETIME_FORMATTER);
        header.put("confirmer", confirmUserName);
        header.put("confirmerName", confirmUserName);
        header.put("confirmUserName", confirmUserName);
        header.put("confirmerTime", formattedConfirmTime);
        header.put("confirmTime", formattedConfirmTime);
        return JsonUtils.toJsonString(header);
    }

    private void applyFaiProcessSelfCheckManualHeader(HcProcessFormRecordDO record) {
        if (!isInspectionSelfCheckRecord(record)) {
            return;
        }
        Map<String, Object> header = new LinkedHashMap<>();
        Map<String, Object> parsed = JsonUtils.parseObjectQuietly(record.getHeaderDataJson(), MAP_TYPE);
        if (parsed != null) {
            header.putAll(parsed);
        }
        String recorderName = firstHeaderText(header, "recorder", "recorderName", "recordUserName", "checkerName", "fillUserName");
        if (StrUtil.isNotBlank(recorderName)) {
            record.setFillUserName(recorderName);
            header.put("recorder", recorderName);
            header.put("recorderName", recorderName);
            header.put("recordUserName", recorderName);
            header.put("checkerName", recorderName);
            header.put("fillUserName", recorderName);
        }
        LocalDateTime recorderTime = parseOptionalHeaderDateTime(header, "记录时间", "recorderTime", "recordTime", "checkerTime");
        if (recorderTime != null) {
            record.setFillTime(recorderTime);
            String formattedRecorderTime = recorderTime.format(DATETIME_FORMATTER);
            header.put("recorderTime", formattedRecorderTime);
            header.put("recordTime", formattedRecorderTime);
            header.put("checkerTime", formattedRecorderTime);
        }
        String confirmerName = firstHeaderText(header, "confirmer", "confirmerName", "confirmUserName");
        String confirmerTimeText = firstHeaderText(header, "confirmerTime", "confirmTime");
        if (StrUtil.isNotBlank(confirmerName) && StrUtil.isNotBlank(confirmerTimeText)) {
            LocalDateTime confirmTime = parseHeaderDateTime(confirmerTimeText, "确认时间");
            record.setRecordStatus(RECORD_STATUS_CONFIRMED);
            record.setConfirmUserName(confirmerName);
            record.setConfirmTime(confirmTime);
            String formattedConfirmTime = confirmTime.format(DATETIME_FORMATTER);
            header.put("confirmer", confirmerName);
            header.put("confirmerName", confirmerName);
            header.put("confirmUserName", confirmerName);
            header.put("confirmerTime", formattedConfirmTime);
            header.put("confirmTime", formattedConfirmTime);
            header.put("docStatus", RECORD_STATUS_CONFIRMED);
            header.put("recordStatus", RECORD_STATUS_CONFIRMED);
        }
        record.setHeaderDataJson(JsonUtils.toJsonString(header));
    }

    private boolean isInspectionSelfCheckRecord(HcProcessFormRecordDO record) {
        return record != null
                && (FAI_PROCESS_SELF_CHECK_PROCESS_CODE.equals(record.getTemplateCode())
                || FAI_PROCESS_SELF_CHECK_PROCESS_CODE.equals(record.getProcessCode())
                || CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE.equals(record.getTemplateCode())
                || CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE.equals(record.getProcessCode()));
    }

    private String firstHeaderText(Map<String, Object> header, String... keys) {
        if (header == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            String text = String.valueOf(header.get(key) == null ? "" : header.get(key)).trim();
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return "";
    }

    private LocalDateTime parseOptionalHeaderDateTime(Map<String, Object> header, String fieldName, String... keys) {
        String text = firstHeaderText(header, keys);
        return StrUtil.isBlank(text) ? null : parseHeaderDateTime(text, fieldName);
    }

    private LocalDateTime parseHeaderDateTime(String value, String fieldName) {
        String text = String.valueOf(value == null ? "" : value).trim().replace('T', ' ');
        if (text.length() >= 19) {
            text = text.substring(0, 19);
        } else if (text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
            text = text + ":00";
        } else if (text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            text = text + " 00:00:00";
        }
        try {
            return LocalDateTime.parse(text, DATETIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw invalidParamException(fieldName + "格式必须为yyyy-MM-dd HH:mm:ss");
        }
    }

    private String schemaText(Map<String, Object> schema, String... keys) {
        if (schema == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = schema.get(key);
            if (value instanceof List<?> list) {
                String joined = list.stream()
                        .map(String::valueOf)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining("_"));
                if (StrUtil.isNotBlank(joined)) {
                    return joined.trim();
                }
            } else if (value != null) {
                String text = String.valueOf(value).trim();
                if (StrUtil.isNotBlank(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String mapFormCategory(String category) {
        if (StrUtil.isBlank(category)) {
            return null;
        }
        String normalized = category.trim().toLowerCase().replace("_", "-");
        return switch (normalized) {
            case "startup-check" -> "STARTUP_CHECK";
            case "cleaning-check" -> "CLEANING_CHECK";
            case "production-check", "process-check" -> "PRODUCTION_CHECK";
            case "intermediate-record", "middle-product" -> "INTERMEDIATE_RECORD";
            case "process-param" -> "PROCESS_PARAM";
            default -> {
                if (category.contains("开机")) {
                    yield "STARTUP_CHECK";
                }
                if (category.contains("清洁")) {
                    yield "CLEANING_CHECK";
                }
                if (category.contains("中间品") || category.contains("半成品")) {
                    yield "INTERMEDIATE_RECORD";
                }
                if (category.contains("工艺参数")) {
                    yield "PROCESS_PARAM";
                }
                if (category.contains("生产") && category.contains("点检")) {
                    yield "PRODUCTION_CHECK";
                }
                yield null;
            }
        };
    }

    private String mapFormTypeName(String formType) {
        if (StrUtil.isBlank(formType)) {
            return null;
        }
        return switch (formType) {
            case "STARTUP_CHECK" -> "开机点检";
            case "CLEANING_CHECK" -> "设备清洁点检";
            case "MAINTENANCE_CHECK" -> "保养点检";
            case "PRODUCTION_CHECK" -> "生产点检";
            case "INTERMEDIATE_RECORD" -> "中间品记录";
            case "COAGULATION_SEMI_RECORD" -> "凝固半成品记录";
            case "OVEN_SEMI_RECORD" -> "烘箱半成品记录";
            case "PRODUCTION_RECORD" -> "生产记录";
            case "PROCESS_PARAM" -> "工艺参数";
            case "HYGIENE_CLEANING" -> "卫生清洁记录";
            case "APPEARANCE_RECORD" -> "产品表观记录";
            case "DMF_CHECK" -> "DMF浓度核对";
            case "WATER_CHANGE" -> "换水记录";
            case "PURE_WATER_CHECK" -> "纯水仪点检";
            case "GUIDE_CLOTH_CHANGE" -> "导布更换记录";
            case "ELECTRIC_HOIST_CHECK" -> "电动葫芦点检";
            case "EQUIPMENT_REPAIR" -> "设备维修单";
            default -> formType;
        };
    }

    private boolean containsAnyIgnoreCase(String text, String... keywords) {
        if (StrUtil.isBlank(text) || keywords == null) {
            return false;
        }
        String upperText = text.toUpperCase();
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && upperText.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isResultFlag(String value) {
        return RESULT_OK.equalsIgnoreCase(value) || RESULT_NG.equalsIgnoreCase(value);
    }

    private boolean isEnabledStationForm(HcStationFormDO form) {
        return form != null && STATION_FORM_STATUS_ENABLED.equals(form.getStatus());
    }

    private HcProcessFormRecordDO buildRecord(HcProcessFormRecordSaveReqVO reqVO, HcProcessFormTemplateDetailRespVO template) {
        HcProcessFormRecordDO record = BeanUtils.toBean(reqVO, HcProcessFormRecordDO.class);
        record.setTemplateId(template.getId());
        record.setVersionId(template.getCurrentVersion().getId());
        record.setTemplateCode(template.getTemplateCode());
        record.setTemplateName(template.getTemplateName());
        record.setProcessCode(template.getProcessCode());
        record.setProcessName(template.getProcessName());
        record.setFormType(template.getFormType());
        record.setFormTypeName(template.getFormTypeName());
        record.setModelCode(firstNotBlank(reqVO.getModelCode(), template.getModelCode(), "COMMON"));
        record.setModelName(firstNotBlank(reqVO.getModelName(), template.getModelName(), "通用"));
        record.setRecordDate(reqVO.getRecordDate() == null ? LocalDate.now() : reqVO.getRecordDate());
        record.setResultStatus(calcResult(reqVO.getItems()));
        return record;
    }

    private void saveRecordItems(HcProcessFormRecordDO record, List<HcProcessFormRecordItemReqVO> reqItems,
                                 List<HcProcessFormItemRespVO> templateItems) {
        Map<Long, HcProcessFormItemRespVO> templateItemMap = templateItems == null ? Map.of() : templateItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HcProcessFormItemRespVO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<HcProcessFormRecordItemReqVO> effectiveItems = CollUtil.isEmpty(reqItems)
                ? buildDefaultRecordItems(templateItems)
                : reqItems;
        Long tenantId = firstNonNull(record.getTenantId(), currentTenantId());
        for (HcProcessFormRecordItemReqVO reqItem : effectiveItems) {
            HcProcessFormItemRespVO templateItem = reqItem.getTemplateItemId() == null
                    ? null
                    : templateItemMap.get(reqItem.getTemplateItemId());
            HcProcessFormRecordItemDO item = BeanUtils.toBean(reqItem, HcProcessFormRecordItemDO.class);
            item.setId(null);
            item.setRecordId(record.getId());
            item.setTenantId(tenantId);
            if (templateItem != null) {
                applyTemplateItemSnapshot(item, templateItem);
            }
            if (item.getItemSeq() == null) {
                item.setItemSeq(0);
            }
            if (StrUtil.isBlank(item.getResultFlag())) {
                item.setResultFlag(RESULT_OK);
            }
            recordItemMapper.insert(item);
        }
    }

    private List<HcProcessFormRecordItemReqVO> buildDefaultRecordItems(List<HcProcessFormItemRespVO> templateItems) {
        if (templateItems == null) {
            return List.of();
        }
        return templateItems.stream().filter(item -> AREA_TYPE_DETAIL.equals(item.getAreaType())).map(item -> {
            HcProcessFormRecordItemReqVO reqVO = BeanUtils.toBean(item, HcProcessFormRecordItemReqVO.class);
            reqVO.setTemplateItemId(item.getId());
            reqVO.setActualValue(item.getDefaultValue());
            reqVO.setResultFlag(firstNotBlank(item.getDefaultResult(), RESULT_OK));
            return reqVO;
        }).toList();
    }

    private void applyTemplateItemSnapshot(HcProcessFormRecordItemDO item, HcProcessFormItemRespVO templateItem) {
        item.setTemplateItemId(templateItem.getId());
        item.setItemSeq(templateItem.getItemSeq());
        item.setFieldKey(templateItem.getFieldKey());
        item.setFieldLabel(templateItem.getFieldLabel());
        item.setItemCategory(templateItem.getItemCategory());
        item.setStepNode(templateItem.getStepNode());
        item.setStandardText(templateItem.getStandardText());
        item.setUnit(templateItem.getUnit());
        item.setValueMode(templateItem.getValueMode());
        item.setControlType(templateItem.getControlType());
        item.setSourceRowJson(templateItem.getSourceRowJson());
        if (StrUtil.isBlank(item.getResultFlag())) {
            item.setResultFlag(firstNotBlank(templateItem.getDefaultResult(), RESULT_OK));
        }
    }

    private String calcResult(List<HcProcessFormRecordItemReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        return items.stream().anyMatch(item -> RESULT_NG.equalsIgnoreCase(item.getResultFlag())) ? RESULT_NG : RESULT_OK;
    }

    private String calcResultFromSaved(List<HcProcessFormRecordItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        return items.stream().anyMatch(item -> RESULT_NG.equalsIgnoreCase(item.getResultFlag())) ? RESULT_NG : RESULT_OK;
    }

    private void applyFillUser(HcProcessFormRecordDO record) {
        record.setFillUserId(firstNonNull(record.getFillUserId(), SecurityFrameworkUtils.getLoginUserId()));
        record.setFillUserName(firstNotBlank(record.getFillUserName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        record.setFillTime(firstNonNull(record.getFillTime(), LocalDateTime.now()));
    }

    private HcProcessFormRecordDO validateRecordExists(Long id) {
        HcProcessFormRecordDO record = id == null ? null : recordMapper.selectById(id);
        if (record == null) {
            throw invalidParamException("填写记录不存在");
        }
        return record;
    }

    private String generateRecordNo() {
        return "PF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private Long currentTenantId() {
        return firstNonNull(TenantContextHolder.getTenantId(), 1L);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
