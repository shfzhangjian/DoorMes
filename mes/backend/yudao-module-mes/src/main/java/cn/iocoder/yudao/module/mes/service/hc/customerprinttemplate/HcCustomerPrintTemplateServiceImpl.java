package cn.iocoder.yudao.module.mes.service.hc.customerprinttemplate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateFieldOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateVarRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateVarSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate.HcCustomerPrintTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate.HcCustomerPrintTemplateVarDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.customerprinttemplate.HcCustomerPrintTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.customerprinttemplate.HcCustomerPrintTemplateVarMapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCCUSTOMERPRINTTEMPLATE_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCCUSTOMERPRINTTEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCCUSTOMERPRINTTEMPLATE_VARIABLE_EMPTY;

@Service
@Validated
public class HcCustomerPrintTemplateServiceImpl implements HcCustomerPrintTemplateService {

    private static final String TYPE_PACKAGE = "PACKAGE";
    private static final String TYPE_PIECE = "PIECE";
    private static final String FORMAT_ZPL = "ZPL";
    private static final String FORMAT_NLBL = "NLBL";
    private static final String SCOPE_HEAD = "HEAD";
    private static final String SCOPE_DETAIL = "DETAIL";
    private static final String SCOPE_PRINT = "PRINT";
    private static final String SCOPE_CONST = "CONST";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private static final List<FieldDef> HEAD_FIELDS = List.of(
            field(SCOPE_HEAD, "noticeNo", "发货需求单号"),
            field(SCOPE_HEAD, "customerCode", "客户编号"),
            field(SCOPE_HEAD, "customerName", "客户名称"),
            field(SCOPE_HEAD, "productType", "产品类型"),
            field(SCOPE_HEAD, "materialCode", "物料编码"),
            field(SCOPE_HEAD, "materialName", "物料名称"),
            field(SCOPE_HEAD, "modelCode", "内部型号"),
            field(SCOPE_HEAD, "productSize", "产品尺寸"),
            field(SCOPE_HEAD, "orderNo", "销售订单号"),
            field(SCOPE_HEAD, "erpOrderNo", "ERP订单号"),
            field(SCOPE_HEAD, "shippingTime", "计划发货时间"),
            field(SCOPE_HEAD, "externalProductModel", "客户产品型号"),
            field(SCOPE_HEAD, "externalProductCode", "客户产品编码"),
            field(SCOPE_HEAD, "externalProductInfo", "客户产品信息"),
            field(SCOPE_HEAD, "requiredShipQty", "需求发货数量"),
            field(SCOPE_HEAD, "requiredSliceRange", "需求片号范围"),
            field(SCOPE_HEAD, "requiredBatchNo", "需求批号"),
            field(SCOPE_HEAD, "requiredProductionDate", "需求生产日期"),
            field(SCOPE_HEAD, "requiredExpiryDate", "需求有效期"),
            field(SCOPE_HEAD, "packingRequirement", "包装要求"),
            field(SCOPE_HEAD, "shippingConfirmName", "发货确认人"),
            field(SCOPE_HEAD, "noticeQty", "通知数量"),
            field(SCOPE_HEAD, "lockedQty", "锁定数量"),
            field(SCOPE_HEAD, "noticeStatus", "需求单状态"),
            field(SCOPE_HEAD, "recorderName", "录入人"),
            field(SCOPE_HEAD, "recorderTime", "录入时间"),
            field(SCOPE_HEAD, "remark", "备注"));

    private static final List<FieldDef> DETAIL_FIELDS = List.of(
            field(SCOPE_DETAIL, "noticeNo", "发货需求单号"),
            field(SCOPE_DETAIL, "stockNo", "库存编号"),
            field(SCOPE_DETAIL, "actualStockNo", "实际库存编号"),
            field(SCOPE_DETAIL, "outerBoxNo", "外箱号"),
            field(SCOPE_DETAIL, "innerUnitNo", "内包号"),
            field(SCOPE_DETAIL, "packageNo", "包装号"),
            field(SCOPE_DETAIL, "sliceBatchNo", "片批号"),
            field(SCOPE_DETAIL, "actualSliceBatchNo", "实际片批号"),
            field(SCOPE_DETAIL, "batchNo", "批号"),
            field(SCOPE_DETAIL, "internalModelCode", "内部型号"),
            field(SCOPE_DETAIL, "internalItemCode", "内部物料编码"),
            field(SCOPE_DETAIL, "customerProductBatchNo", "客户产品批号"),
            field(SCOPE_DETAIL, "packageSliceNo", "包装片号"),
            field(SCOPE_DETAIL, "materialCode", "物料编码"),
            field(SCOPE_DETAIL, "materialName", "物料名称"),
            field(SCOPE_DETAIL, "modelCode", "型号编码"),
            field(SCOPE_DETAIL, "productSize", "产品尺寸"),
            field(SCOPE_DETAIL, "stockQty", "库存数量"),
            field(SCOPE_DETAIL, "availableQty", "可用数量"),
            field(SCOPE_DETAIL, "lockedQty", "锁定数量"),
            field(SCOPE_DETAIL, "qualityStatus", "质量状态"),
            field(SCOPE_DETAIL, "warehouseCode", "仓库编码"),
            field(SCOPE_DETAIL, "warehouseName", "仓库名称"),
            field(SCOPE_DETAIL, "locationCode", "库位编码"),
            field(SCOPE_DETAIL, "locationName", "库位名称"),
            field(SCOPE_DETAIL, "actualShipQty", "实际发货数量"),
            field(SCOPE_DETAIL, "customerSliceBatchNo", "客户片批号"),
            field(SCOPE_DETAIL, "customerModelCode", "客户型号"),
            field(SCOPE_DETAIL, "shippingQualityNo", "出货检验单号"),
            field(SCOPE_DETAIL, "shippingInspectorName", "出货检验人"),
            field(SCOPE_DETAIL, "shippingInspectionResult", "出货检验结果"),
            field(SCOPE_DETAIL, "shippingPackageName", "发货包装人"),
            field(SCOPE_DETAIL, "shippingPackageTime", "发货包装时间"),
            field(SCOPE_DETAIL, "shippedName", "发货人"),
            field(SCOPE_DETAIL, "shippedTime", "发货时间"),
            field(SCOPE_DETAIL, "actualRemark", "实际备注"),
            field(SCOPE_DETAIL, "remark", "备注"));

    private static final List<FieldDef> PRINT_FIELDS = List.of(
            field(SCOPE_PRINT, "operatorName", "打印操作人"),
            field(SCOPE_PRINT, "printTime", "打印时间"),
            field(SCOPE_PRINT, "copies", "打印份数"),
            field(SCOPE_PRINT, "requestId", "打印请求号"));

    private static final Map<String, Map<String, FieldDef>> FIELD_MAP = buildFieldMap();

    @Resource
    private HcCustomerPrintTemplateMapper hcCustomerPrintTemplateMapper;
    @Resource
    private HcCustomerPrintTemplateVarMapper hcCustomerPrintTemplateVarMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcCustomerPrintTemplate(HcCustomerPrintTemplateSaveReqVO createReqVO) {
        validateTemplateCodeUnique(null, createReqVO.getTemplateCode());
        HcCustomerPrintTemplateDO entity = BeanUtils.toBean(createReqVO, HcCustomerPrintTemplateDO.class);
        List<HcCustomerPrintTemplateVarSaveReqVO> vars = normalizeForSave(entity, createReqVO.getVars());
        hcCustomerPrintTemplateMapper.insert(entity);
        replaceVars(entity.getId(), vars);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcCustomerPrintTemplate(HcCustomerPrintTemplateSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateTemplateCodeUnique(updateReqVO.getId(), updateReqVO.getTemplateCode());
        HcCustomerPrintTemplateDO updateObj = BeanUtils.toBean(updateReqVO, HcCustomerPrintTemplateDO.class);
        List<HcCustomerPrintTemplateVarSaveReqVO> vars = normalizeForSave(updateObj, updateReqVO.getVars());
        hcCustomerPrintTemplateMapper.updateById(updateObj);
        replaceVars(updateReqVO.getId(), vars);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcCustomerPrintTemplate(Long id) {
        validateExists(id);
        hcCustomerPrintTemplateVarMapper.physicalDeleteByTemplateId(id);
        hcCustomerPrintTemplateMapper.deleteById(id);
    }

    @Override
    public HcCustomerPrintTemplateDO getHcCustomerPrintTemplate(Long id) {
        return hcCustomerPrintTemplateMapper.selectById(id);
    }

    @Override
    public HcCustomerPrintTemplateRespVO getHcCustomerPrintTemplateDetail(Long id) {
        HcCustomerPrintTemplateDO entity = validateExists(id);
        HcCustomerPrintTemplateRespVO respVO = BeanUtils.toBean(entity, HcCustomerPrintTemplateRespVO.class);
        respVO.setVars(getVarsByTemplateId(id));
        return respVO;
    }

    @Override
    public PageResult<HcCustomerPrintTemplateDO> getHcCustomerPrintTemplatePage(HcCustomerPrintTemplatePageReqVO pageReqVO) {
        return hcCustomerPrintTemplateMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcCustomerPrintTemplateVarRespVO> getVarsByTemplateId(Long templateId) {
        validateExists(templateId);
        return BeanUtils.toBean(hcCustomerPrintTemplateVarMapper.selectListByTemplateId(templateId),
                HcCustomerPrintTemplateVarRespVO.class);
    }

    @Override
    public List<HcCustomerPrintTemplateRespVO> getActiveTemplates(String templateType, String customerCode, String customerName) {
        validateTemplateType(templateType);
        List<HcCustomerPrintTemplateDO> entities = hcCustomerPrintTemplateMapper.selectActiveList(
                normalizeType(templateType), StrUtil.trim(customerCode), StrUtil.trim(customerName));
        List<HcCustomerPrintTemplateRespVO> result = BeanUtils.toBean(entities, HcCustomerPrintTemplateRespVO.class);
        for (HcCustomerPrintTemplateRespVO item : result) {
            item.setVars(BeanUtils.toBean(hcCustomerPrintTemplateVarMapper.selectListByTemplateId(item.getId()),
                    HcCustomerPrintTemplateVarRespVO.class));
            if (FORMAT_NLBL.equals(normalizeFormat(item.getTemplateFormat()))) {
                item.setTemplateContent(null);
            }
        }
        return result;
    }

    @Override
    public List<HcCustomerPrintTemplateFieldOptionRespVO> getFieldOptions(String scope) {
        String normalizedScope = normalizeScope(scope);
        List<FieldDef> fields = switch (normalizedScope) {
            case SCOPE_HEAD -> HEAD_FIELDS;
            case SCOPE_DETAIL -> DETAIL_FIELDS;
            case SCOPE_PRINT -> PRINT_FIELDS;
            default -> {
                List<FieldDef> all = new ArrayList<>();
                all.addAll(HEAD_FIELDS);
                all.addAll(DETAIL_FIELDS);
                all.addAll(PRINT_FIELDS);
                yield all;
            }
        };
        return fields.stream().map(this::toFieldOptionRespVO).toList();
    }

    @Override
    public List<HcCustomerPrintTemplateVarRespVO> parseVariables(HcCustomerPrintTemplateParseReqVO reqVO) {
        validateTemplateType(reqVO.getTemplateType());
        String templateFormat = normalizeFormat(reqVO.getTemplateFormat());
        validateTemplateFormat(templateFormat);
        if (FORMAT_NLBL.equals(templateFormat)) {
            return List.of();
        }
        if (StrUtil.isBlank(reqVO.getTemplateContent())) {
            throw invalidParamException("ZPL 模板内容不能为空");
        }
        List<HcCustomerPrintTemplateVarSaveReqVO> vars = parseTemplateVariables(
                normalizeType(reqVO.getTemplateType()), reqVO.getTemplateContent(), null);
        return BeanUtils.toBean(vars, HcCustomerPrintTemplateVarRespVO.class);
    }

    private HcCustomerPrintTemplateDO validateExists(Long id) {
        HcCustomerPrintTemplateDO entity = hcCustomerPrintTemplateMapper.selectById(id);
        if (entity == null) {
            throw exception(HCCUSTOMERPRINTTEMPLATE_NOT_EXISTS);
        }
        return entity;
    }

    private void validateTemplateCodeUnique(Long id, String templateCode) {
        HcCustomerPrintTemplateDO entity = hcCustomerPrintTemplateMapper.selectByTemplateCode(StrUtil.trim(templateCode));
        if (entity != null && !entity.getId().equals(id)) {
            throw exception(HCCUSTOMERPRINTTEMPLATE_CODE_EXISTS);
        }
    }

    private List<HcCustomerPrintTemplateVarSaveReqVO> normalizeForSave(HcCustomerPrintTemplateDO entity,
            List<HcCustomerPrintTemplateVarSaveReqVO> requestVars) {
        entity.setTemplateCode(StrUtil.trim(entity.getTemplateCode()));
        entity.setTemplateName(StrUtil.trim(entity.getTemplateName()));
        entity.setCustomerCode(StrUtil.trim(entity.getCustomerCode()));
        entity.setCustomerName(StrUtil.trim(entity.getCustomerName()));
        entity.setTemplateType(normalizeType(entity.getTemplateType()));
        entity.setTemplateFormat(normalizeFormat(entity.getTemplateFormat()));
        entity.setFileName(StrUtil.trim(entity.getFileName()));
        entity.setFileUrl(StrUtil.trim(entity.getFileUrl()));
        entity.setRemark(StrUtil.trim(entity.getRemark()));
        entity.setStatus(entity.getStatus() == null ? 0 : entity.getStatus());
        validateTemplateType(entity.getTemplateType());
        validateTemplateFormat(entity.getTemplateFormat());
        List<HcCustomerPrintTemplateVarSaveReqVO> vars;
        if (FORMAT_NLBL.equals(entity.getTemplateFormat())) {
            if (StrUtil.isBlank(entity.getFileUrl()) && StrUtil.isBlank(entity.getTemplateContent())) {
                throw invalidParamException("NLBL 模板请上传模板文件");
            }
            vars = normalizeManualVariables(entity.getTemplateType(), requestVars);
        } else {
            if (StrUtil.isBlank(entity.getTemplateContent())) {
                throw invalidParamException("ZPL 模板内容不能为空");
            }
            vars = CollUtil.isNotEmpty(requestVars)
                    ? normalizeManualVariables(entity.getTemplateType(), requestVars)
                    : parseTemplateVariables(entity.getTemplateType(), entity.getTemplateContent(), null);
        }
        entity.setVariableJson(JsonUtils.toJsonString(vars));
        return vars;
    }

    private List<HcCustomerPrintTemplateVarSaveReqVO> normalizeManualVariables(String templateType,
            List<HcCustomerPrintTemplateVarSaveReqVO> requestVars) {
        if (CollUtil.isEmpty(requestVars)) {
            return List.of();
        }
        Set<String> variableNames = new LinkedHashSet<>();
        List<HcCustomerPrintTemplateVarSaveReqVO> result = new ArrayList<>();
        int rowIndex = 1;
        for (HcCustomerPrintTemplateVarSaveReqVO requestVar : requestVars) {
            if (requestVar == null) {
                continue;
            }
            HcCustomerPrintTemplateVarSaveReqVO item = copyVar(requestVar);
            item.setId(null);
            item.setVariableName(normalizeVariableName(item.getVariableName()));
            if (StrUtil.isBlank(item.getVariableName())) {
                throw invalidParamException("变量第 {} 行变量名不能为空", rowIndex);
            }
            if (!variableNames.add(item.getVariableName())) {
                throw invalidParamException("变量名重复：{}", item.getVariableName());
            }
            item.setSort(item.getSort() == null ? rowIndex : item.getSort());
            normalizeAndValidateVar(templateType, item, rowIndex);
            result.add(item);
            rowIndex++;
        }
        result.sort(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()));
        return result;
    }

    private List<HcCustomerPrintTemplateVarSaveReqVO> parseTemplateVariables(String templateType, String templateContent,
            List<HcCustomerPrintTemplateVarSaveReqVO> requestVars) {
        LinkedHashSet<String> variableNames = extractVariableNames(templateContent);
        if (variableNames.isEmpty()) {
            throw exception(HCCUSTOMERPRINTTEMPLATE_VARIABLE_EMPTY);
        }
        Map<String, HcCustomerPrintTemplateVarSaveReqVO> requestVarMap = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(requestVars)) {
            for (HcCustomerPrintTemplateVarSaveReqVO item : requestVars) {
                if (item != null && StrUtil.isNotBlank(item.getVariableName())) {
                    requestVarMap.put(StrUtil.trim(item.getVariableName()), item);
                }
            }
        }
        List<HcCustomerPrintTemplateVarSaveReqVO> result = new ArrayList<>();
        int index = 1;
        for (String variableName : variableNames) {
            HcCustomerPrintTemplateVarSaveReqVO item = requestVarMap.containsKey(variableName)
                    ? copyVar(requestVarMap.get(variableName))
                    : resolveVariable(templateType, variableName);
            item.setId(null);
            item.setVariableName(variableName);
            item.setSort(item.getSort() == null ? index : item.getSort());
            normalizeAndValidateVar(templateType, item, index);
            result.add(item);
            index++;
        }
        result.sort(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()));
        return result;
    }

    private void normalizeAndValidateVar(String templateType, HcCustomerPrintTemplateVarSaveReqVO item, int rowIndex) {
        item.setVariableName(StrUtil.trim(item.getVariableName()));
        item.setVariableScope(normalizeScope(item.getVariableScope()));
        item.setSourceField(StrUtil.trim(item.getSourceField()));
        item.setSourceLabel(StrUtil.trim(item.getSourceLabel()));
        item.setDefaultValue(StrUtil.trim(item.getDefaultValue()));
        item.setRemark(StrUtil.trim(item.getRemark()));
        item.setRequired(item.getRequired() == null ? Boolean.FALSE : item.getRequired());
        if (SCOPE_CONST.equals(item.getVariableScope())) {
            item.setVariableExpr(StrUtil.blankToDefault(StrUtil.trim(item.getVariableExpr()), "const." + item.getVariableName()));
            return;
        }
        if (TYPE_PACKAGE.equals(templateType) && SCOPE_DETAIL.equals(item.getVariableScope())) {
            throw invalidParamException("整包装模板不允许绑定明细字段：{}", item.getVariableName());
        }
        if (StrUtil.isBlank(item.getSourceField())) {
            throw invalidParamException("变量第 {} 行来源字段不能为空", rowIndex);
        }
        FieldDef fieldDef = findField(item.getVariableScope(), item.getSourceField());
        if (fieldDef == null) {
            throw invalidParamException("变量【{}】的来源字段不存在：{}.{}",
                    item.getVariableName(), item.getVariableScope(), item.getSourceField());
        }
        item.setSourceLabel(fieldDef.sourceLabel);
        item.setVariableExpr(fieldDef.scope.toLowerCase(Locale.ROOT) + "." + fieldDef.sourceField);
    }

    private void replaceVars(Long templateId, List<HcCustomerPrintTemplateVarSaveReqVO> vars) {
        hcCustomerPrintTemplateVarMapper.physicalDeleteByTemplateId(templateId);
        if (CollUtil.isEmpty(vars)) {
            return;
        }
        for (HcCustomerPrintTemplateVarSaveReqVO item : vars) {
            HcCustomerPrintTemplateVarDO entity = BeanUtils.toBean(item, HcCustomerPrintTemplateVarDO.class);
            entity.setId(null);
            entity.setTemplateId(templateId);
            hcCustomerPrintTemplateVarMapper.insert(entity);
        }
    }

    private HcCustomerPrintTemplateVarSaveReqVO resolveVariable(String templateType, String variableName) {
        String scope = defaultScope(templateType);
        String sourceField = variableName;
        int dotIndex = variableName.indexOf('.');
        if (dotIndex > 0) {
            scope = normalizeScope(variableName.substring(0, dotIndex));
            sourceField = variableName.substring(dotIndex + 1);
        }
        if (SCOPE_CONST.equals(scope)) {
            return buildConstVar(variableName);
        }
        FieldDef fieldDef = findField(scope, sourceField);
        if (fieldDef == null || (TYPE_PACKAGE.equals(templateType) && SCOPE_DETAIL.equals(scope))) {
            return buildConstVar(variableName);
        }
        HcCustomerPrintTemplateVarSaveReqVO item = new HcCustomerPrintTemplateVarSaveReqVO();
        item.setVariableName(variableName);
        item.setVariableScope(fieldDef.scope);
        item.setSourceField(fieldDef.sourceField);
        item.setSourceLabel(fieldDef.sourceLabel);
        item.setVariableExpr(fieldDef.scope.toLowerCase(Locale.ROOT) + "." + fieldDef.sourceField);
        item.setRequired(Boolean.FALSE);
        return item;
    }

    private HcCustomerPrintTemplateVarSaveReqVO buildConstVar(String variableName) {
        HcCustomerPrintTemplateVarSaveReqVO item = new HcCustomerPrintTemplateVarSaveReqVO();
        item.setVariableName(variableName);
        item.setVariableScope(SCOPE_CONST);
        item.setVariableExpr("const." + variableName);
        item.setRequired(Boolean.FALSE);
        return item;
    }

    private HcCustomerPrintTemplateVarSaveReqVO copyVar(HcCustomerPrintTemplateVarSaveReqVO source) {
        HcCustomerPrintTemplateVarSaveReqVO target = new HcCustomerPrintTemplateVarSaveReqVO();
        target.setId(source.getId());
        target.setVariableName(source.getVariableName());
        target.setVariableExpr(source.getVariableExpr());
        target.setVariableScope(source.getVariableScope());
        target.setSourceField(source.getSourceField());
        target.setSourceLabel(source.getSourceLabel());
        target.setSort(source.getSort());
        target.setRequired(source.getRequired());
        target.setDefaultValue(source.getDefaultValue());
        target.setRemark(source.getRemark());
        return target;
    }

    private LinkedHashSet<String> extractVariableNames(String templateContent) {
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(StrUtil.nullToEmpty(templateContent));
        while (matcher.find()) {
            String variableName = StrUtil.trim(matcher.group(1));
            if (StrUtil.isNotBlank(variableName)) {
                variables.add(variableName);
            }
        }
        return variables;
    }

    private void validateTemplateType(String templateType) {
        String normalizedType = normalizeType(templateType);
        if (!TYPE_PACKAGE.equals(normalizedType) && !TYPE_PIECE.equals(normalizedType)) {
            throw invalidParamException("模板类型不正确：{}", templateType);
        }
    }

    private void validateTemplateFormat(String templateFormat) {
        String normalizedFormat = normalizeFormat(templateFormat);
        if (!FORMAT_ZPL.equals(normalizedFormat) && !FORMAT_NLBL.equals(normalizedFormat)) {
            throw invalidParamException("模板格式不正确：{}", templateFormat);
        }
    }

    private String normalizeType(String templateType) {
        return StrUtil.nullToEmpty(StrUtil.trim(templateType)).toUpperCase(Locale.ROOT);
    }

    private String normalizeVariableName(String variableName) {
        String normalizedName = StrUtil.nullToEmpty(StrUtil.trim(variableName));
        if (normalizedName.startsWith("{{") && normalizedName.endsWith("}}") && normalizedName.length() >= 4) {
            normalizedName = StrUtil.sub(normalizedName, 2, normalizedName.length() - 2);
        }
        return StrUtil.trim(normalizedName);
    }

    private String normalizeFormat(String templateFormat) {
        String normalizedFormat = StrUtil.nullToEmpty(StrUtil.trim(templateFormat)).toUpperCase(Locale.ROOT);
        return StrUtil.blankToDefault(normalizedFormat, FORMAT_ZPL);
    }

    private String normalizeScope(String scope) {
        String normalizedScope = StrUtil.nullToEmpty(StrUtil.trim(scope)).toUpperCase(Locale.ROOT);
        if ("H".equals(normalizedScope) || "MAIN".equals(normalizedScope)) {
            return SCOPE_HEAD;
        }
        if ("D".equals(normalizedScope) || "ITEM".equals(normalizedScope)) {
            return SCOPE_DETAIL;
        }
        if ("P".equals(normalizedScope)) {
            return SCOPE_PRINT;
        }
        if (SCOPE_HEAD.equals(normalizedScope) || SCOPE_DETAIL.equals(normalizedScope)
                || SCOPE_PRINT.equals(normalizedScope) || SCOPE_CONST.equals(normalizedScope)) {
            return normalizedScope;
        }
        return SCOPE_CONST;
    }

    private String defaultScope(String templateType) {
        return TYPE_PIECE.equals(templateType) ? SCOPE_DETAIL : SCOPE_HEAD;
    }

    private FieldDef findField(String scope, String sourceField) {
        Map<String, FieldDef> scopeMap = FIELD_MAP.get(normalizeScope(scope));
        return scopeMap == null ? null : scopeMap.get(StrUtil.trim(sourceField));
    }

    private HcCustomerPrintTemplateFieldOptionRespVO toFieldOptionRespVO(FieldDef fieldDef) {
        HcCustomerPrintTemplateFieldOptionRespVO respVO = new HcCustomerPrintTemplateFieldOptionRespVO();
        respVO.setScope(fieldDef.scope);
        respVO.setSourceField(fieldDef.sourceField);
        respVO.setSourceLabel(fieldDef.sourceLabel);
        respVO.setVariableExpr(fieldDef.scope.toLowerCase(Locale.ROOT) + "." + fieldDef.sourceField);
        return respVO;
    }

    private static FieldDef field(String scope, String sourceField, String sourceLabel) {
        return new FieldDef(scope, sourceField, sourceLabel);
    }

    private static Map<String, Map<String, FieldDef>> buildFieldMap() {
        Map<String, Map<String, FieldDef>> result = new LinkedHashMap<>();
        putFieldDefs(result, HEAD_FIELDS);
        putFieldDefs(result, DETAIL_FIELDS);
        putFieldDefs(result, PRINT_FIELDS);
        return result;
    }

    private static void putFieldDefs(Map<String, Map<String, FieldDef>> target, List<FieldDef> fieldDefs) {
        for (FieldDef fieldDef : fieldDefs) {
            target.computeIfAbsent(fieldDef.scope, key -> new LinkedHashMap<>()).put(fieldDef.sourceField, fieldDef);
        }
    }

    private record FieldDef(String scope, String sourceField, String sourceLabel) {
    }

}
