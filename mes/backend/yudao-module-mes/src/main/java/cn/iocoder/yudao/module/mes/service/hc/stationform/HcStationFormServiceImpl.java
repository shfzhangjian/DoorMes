package cn.iocoder.yudao.module.mes.service.hc.stationform;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageFormVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageImportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormConfigPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormImportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormProcessOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCSTATIONFORM_DEV_CONTRACT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCSTATIONFORM_FORMCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCSTATIONFORM_IMPORT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCSTATIONFORM_NOT_EXISTS;

@Service
@Validated
public class HcStationFormServiceImpl implements HcStationFormService {

    private static final int MAX_IMPORT_ITEM_COUNT = 500;
    private static final String DEV_FORM_CODE_SUFFIX = "_DEV";
    private static final String PRESS_SLOT_INTERMEDIATE_RUNTIME_DEV_FORM_CODE = "PRESS_SLOT_INTERMEDIATE_RECORD_11_2_94_DEV";
    private static final String CONFIG_PACKAGE_TYPE = "HC_MES_STATION_FORM_CONFIG";
    private static final String CONFIG_PACKAGE_VERSION = "1.0";

    private static final List<HcStationFormProcessOptionRespVO> REPORT_PROCESS_OPTIONS = List.of(
            processOption("FORMULA", "配料"),
            processOption("WET", "湿法"),
            processOption("ROUGH_GRINDING", "磨皮"),
            processOption("ADHESIVE", "粘胶1"),
            processOption("ADHESIVE2", "粘胶2"),
            processOption("SLITTING", "分切"),
            processOption("PRESS_SLOT", "压槽"),
            processOption("CUT_ROUND", "裁切"),
            processOption("PACKAGING", "包装")
    );

    private static final Map<String, String> PROCESS_NAME_MAP = buildProcessNameMap();

    private static final Map<String, String> TRIGGER_TIMING_NAME_MAP = Map.of(
            "BEFORE_START", "开工前",
            "BEFORE_PROCESS_CHECK", "首检",
            "IN_PROCESS", "生产中",
            "BEFORE_FINISH", "完工前"
    );

    @Resource
    private HcStationFormMapper hcStationFormMapper;

    @Resource
    private HcStationFormItemMapper hcStationFormItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcStationForm(HcStationFormSaveReqVO createReqVO) {
        normalizeSaveForm(createReqVO);
        validateDevFormContract(createReqVO);
        validatePressSlotProductionScope(createReqVO);
        validateFormCodeUnique(null, createReqVO.getFormCode());
        HcStationFormDO entity = BeanUtils.toBean(createReqVO, HcStationFormDO.class);
        entity.setPresetItemsJson(toPresetItemsJson(createReqVO.getPresetItems()));
        hcStationFormMapper.insert(entity);
        replaceFormItems(entity.getId(), createReqVO.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcStationForm(HcStationFormSaveReqVO updateReqVO) {
        validateHcStationFormExists(updateReqVO.getId());
        normalizeSaveForm(updateReqVO);
        validateDevFormContract(updateReqVO);
        validatePressSlotProductionScope(updateReqVO);
        validateFormCodeUnique(updateReqVO.getId(), updateReqVO.getFormCode());
        HcStationFormDO updateObj = BeanUtils.toBean(updateReqVO, HcStationFormDO.class);
        updateObj.setPresetItemsJson(toPresetItemsJson(updateReqVO.getPresetItems()));
        hcStationFormMapper.updateById(updateObj);
        // 粘胶1中间品的旧 Excel 解析项仅留作追溯，配置保存不重建它们。
        if (!isAdhesiveIntermediateColumnConfig(updateReqVO)) {
            replaceFormItems(updateReqVO.getId(), updateReqVO.getItems());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcStationForm(Long id) {
        validateHcStationFormExists(id);
        hcStationFormMapper.deleteById(id);
        hcStationFormItemMapper.deleteByFormId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcStationFormListByIds(List<Long> ids) {
        hcStationFormMapper.deleteByIds(ids);
        hcStationFormItemMapper.deleteByFormIds(ids);
    }

    @Override
    public HcStationFormDO getHcStationForm(Long id) {
        return hcStationFormMapper.selectById(id);
    }

    @Override
    public List<HcStationFormItemDO> getHcStationFormItemList(Long formId) {
        return hcStationFormItemMapper.selectByFormId(formId);
    }

    @Override
    public List<HcStationFormDO> getHcStationFormSimpleList(String processCode) {
        LambdaQueryWrapperX<HcStationFormDO> queryWrapper = new LambdaQueryWrapperX<HcStationFormDO>()
                .eq(HcStationFormDO::getStatus, 1)
                .eqIfPresent(HcStationFormDO::getProcessCode, processCode);
        queryWrapper.orderByAsc(HcStationFormDO::getProcessCode)
                .orderByAsc(HcStationFormDO::getSortNo)
                .orderByAsc(HcStationFormDO::getFormCode);
        return hcStationFormMapper.selectList(queryWrapper);
    }

    @Override
    public HcStationFormDO resolvePublishedHcStationForm(String processCode, String modelCode,
                                                          String formType, String grindingPass) {
        if (!StringUtils.hasText(processCode) || !StringUtils.hasText(formType)) {
            return null;
        }
        return hcStationFormMapper.selectEnabledByProcess(processCode).stream()
                .filter(form -> resolvePublishedFormScore(form, modelCode, formType, grindingPass) >= 0)
                .sorted(Comparator
                        .comparingInt((HcStationFormDO form) -> -resolvePublishedFormScore(form, modelCode, formType, grindingPass))
                        .thenComparing(form -> form.getSortNo() == null ? Integer.MAX_VALUE : form.getSortNo())
                        .thenComparing(HcStationFormDO::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
    }

    /**
     * 仅供正式运行时解析。约定：精确型号 > 型号前缀 > 明确允许的通用兜底。
     */
    private int resolvePublishedFormScore(HcStationFormDO form, String modelCode,
                                          String formType, String grindingPass) {
        JsonNode schema = parseSchemaNode(form == null ? null : form.getSchemaJson());
        if (schema == null || isDevFormCode(form == null ? null : form.getFormCode())
                || isSchemaBooleanTrue(schema, "devOnly")
                || !isSchemaBooleanTrue(schema, "published")) {
            return -1;
        }
        if (!formType.equalsIgnoreCase(runtimeSchemaText(schema, "formType", "processFormType", "runtimeFormType"))) {
            return -1;
        }
        if (StringUtils.hasText(grindingPass)
                && !grindingPass.equalsIgnoreCase(runtimeSchemaText(schema, "grindingPass", "passType"))) {
            return -1;
        }
        String normalizedModelCode = normalizeModelCode(modelCode);
        String scope = runtimeSchemaText(schema, "modelScope", "scope").toUpperCase(Locale.ROOT);
        // 通用范围以显式兜底开关为准，兼容编辑器曾写入的 modelCode=COMMON。
        if ("COMMON".equals(scope)) {
            return resolveCommonFallbackScore(schema);
        }
        if ("MODEL".equals(scope)) {
            String configuredModelCode = normalizeModelCode(runtimeSchemaText(schema, "modelCode", "sourceModelCode"));
            return StringUtils.hasText(normalizedModelCode) && normalizedModelCode.equals(configuredModelCode) ? 300 : -1;
        }
        if ("PREFIX".equals(scope)) {
            // 兼容早期设计器将前缀误存到根节点 modelCode 的已发布正式模板。
            // PREFIX 范围只读取根节点，避免被历史 modelMatch/businessBinding 中的旧精确型号覆盖。
            String configuredPrefix = normalizeModelCode(schemaText(schema, "modelPrefix", "sourceModelPrefix"));
            if (!StringUtils.hasText(configuredPrefix)) {
                configuredPrefix = normalizeModelCode(schemaText(schema, "modelCode", "sourceModelCode"));
            }
            return StringUtils.hasText(normalizedModelCode) && StringUtils.hasText(configuredPrefix)
                    && normalizedModelCode.startsWith(configuredPrefix) ? 200 : -1;
        }
        String configuredModelCode = normalizeModelCode(runtimeSchemaText(schema, "modelCode", "sourceModelCode"));
        if (StringUtils.hasText(configuredModelCode)) {
            return StringUtils.hasText(normalizedModelCode) && normalizedModelCode.equals(configuredModelCode) ? 300 : -1;
        }
        String configuredPrefix = normalizeModelCode(runtimeSchemaText(schema, "modelPrefix", "sourceModelPrefix"));
        if (StringUtils.hasText(configuredPrefix)) {
            return StringUtils.hasText(normalizedModelCode) && normalizedModelCode.startsWith(configuredPrefix) ? 200 : -1;
        }
        return resolveCommonFallbackScore(schema);
    }

    private int resolveCommonFallbackScore(JsonNode schema) {
        return isSchemaBooleanTrue(schema, "allowCommonFallback")
                || isSchemaBooleanTrue(schema.path("modelMatch"), "fallback")
                || isSchemaBooleanTrue(schema.path("businessBinding").path("filter"), "fallback") ? 100 : -1;
    }

    private JsonNode parseSchemaNode(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            return null;
        }
        try {
            JsonNode schema = JsonUtils.parseTree(schemaJson);
            return schema != null && schema.isObject() ? schema : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSchemaBooleanTrue(JsonNode schema, String field) {
        JsonNode value = schema == null ? null : schema.get(field);
        return value != null && (value.asBoolean(false)
                || "true".equalsIgnoreCase(value.asText())
                || "1".equals(value.asText()));
    }

    private String schemaText(JsonNode schema, String... fieldNames) {
        if (schema == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode value = schema.get(fieldName);
            if (value == null || value.isNull() || value.isMissingNode()) {
                continue;
            }
            String text = value.asText();
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return "";
    }

    private String runtimeSchemaText(JsonNode schema, String... fieldNames) {
        String directValue = schemaText(schema, fieldNames);
        if (StringUtils.hasText(directValue)) {
            return directValue;
        }
        String modelMatchValue = schemaText(schema == null ? null : schema.path("modelMatch"), fieldNames);
        if (StringUtils.hasText(modelMatchValue)) {
            return modelMatchValue;
        }
        return schemaText(schema == null ? null : schema.path("businessBinding").path("filter"), fieldNames);
    }

    private String normalizeModelCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    @Override
    public List<HcStationFormProcessOptionRespVO> getReportProcessOptions() {
        return REPORT_PROCESS_OPTIONS;
    }

    @Override
    public List<HcStationFormDO> getHcStationFormList(HcStationFormPageReqVO reqVO) {
        return hcStationFormMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcStationFormDO> getHcStationFormPage(HcStationFormPageReqVO pageReqVO) {
        return hcStationFormMapper.selectPage(pageReqVO);
    }

    @Override
    public HcStationFormImportRespVO previewExcelImport(MultipartFile file, String formCode, String formName,
                                                        String processCode, String triggerTimingCode,
                                                        String presetTemplate) throws IOException {
        HcStationFormImportRespVO respVO = parseExcelImport(file, formCode, formName, processCode,
                triggerTimingCode, presetTemplate);
        markExistingForm(respVO);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcStationFormImportRespVO confirmExcelImport(HcStationFormImportConfirmReqVO reqVO) {
        if (reqVO == null || reqVO.getForm() == null) {
            throw exception(HCSTATIONFORM_IMPORT_INVALID);
        }
        HcStationFormSaveReqVO form = reqVO.getForm();
        normalizeSaveForm(form);

        HcStationFormImportRespVO respVO = new HcStationFormImportRespVO();
        respVO.setForm(form);
        respVO.setSuccessCount(form.getItems() == null ? 0 : form.getItems().size());
        if (!StringUtils.hasText(form.getFormCode()) || !StringUtils.hasText(form.getFormName())
                || !StringUtils.hasText(form.getProcessCode()) || !StringUtils.hasText(form.getTriggerTimingCode())) {
            addImportFailure(respVO, "表单编码、表单名称、业务工序、触发时机均不能为空");
            return respVO;
        }
        if (form.getItems() == null || form.getItems().isEmpty()) {
            addImportFailure(respVO, "导入表单明细为空，未写入数据");
            return respVO;
        }

        HcStationFormDO existing = hcStationFormMapper.selectByFormCode(form.getFormCode());
        if (existing != null) {
            respVO.setExisting(true);
            respVO.setExistingId(existing.getId());
            if (!Boolean.TRUE.equals(reqVO.getOverwriteExisting())) {
                addImportFailure(respVO, String.format("表单编码 %s 已存在，请勾选覆盖已有表单后再导入", form.getFormCode()));
                return respVO;
            }
            form.setId(existing.getId());
            updateHcStationForm(form);
            respVO.getMessages().add(String.format("已覆盖更新动态表单：%s，明细 %d 条",
                    form.getFormName(), form.getItems().size()));
            return respVO;
        }

        form.setId(null);
        Long id = createHcStationForm(form);
        form.setId(id);
        respVO.setExisting(false);
        respVO.getMessages().add(String.format("已新增动态表单：%s，明细 %d 条",
                form.getFormName(), form.getItems().size()));
        return respVO;
    }

    @Override
    public byte[] exportPreviewExcel(HcStationFormSaveReqVO form) throws IOException {
        HcStationFormSaveReqVO safeForm = form == null ? new HcStationFormSaveReqVO() : form;
        List<HcStationFormItemSaveReqVO> items = safeForm.getItems() == null ? List.of() : safeForm.getItems();
        Map<String, Object> headerData = parseJsonMap(safeForm.getPresetHeaderDataJson());
        List<String> headerFields = resolveHeaderFields(safeForm.getSchemaJson(), headerData);
        if (isPressSlotIntermediateRuntimeDevForm(safeForm)) {
            return exportPressSlotIntermediateRuntimePreviewExcel(safeForm, items, headerData);
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(firstNotBlank(safeForm.getFormName(), "动态表单")));
            StationFormExcelStyles styles = buildStationFormExcelStyles(workbook);
            int[] widths = {10, 16, 18, 28, 36, 16, 16, 16, 16, 10, 28, 50};
            for (int index = 0; index < widths.length; index++) {
                sheet.setColumnWidth(index, widths[index] * 256);
            }

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(28);
            writeCell(titleRow, 0, firstNotBlank(safeForm.getFormName(), "动态表单预览"), styles.titleStyle);
            merge(sheet, titleRow.getRowNum(), titleRow.getRowNum(), 0, widths.length - 1);

            Row metaRow = sheet.createRow(rowIndex++);
            writeCell(metaRow, 0, "表单编码", styles.headerLabelStyle);
            writeCell(metaRow, 1, safeText(safeForm.getFormCode()), styles.bodyStyle);
            writeCell(metaRow, 2, "业务工序", styles.headerLabelStyle);
            writeCell(metaRow, 3, firstNotBlank(safeForm.getProcessName(), safeForm.getProcessCode()), styles.bodyStyle);
            writeCell(metaRow, 4, "触发时机", styles.headerLabelStyle);
            writeCell(metaRow, 5, firstNotBlank(safeForm.getTriggerTimingName(), safeForm.getTriggerTimingCode()), styles.bodyStyle);
            writeCell(metaRow, 6, "需确认", styles.headerLabelStyle);
            writeCell(metaRow, 7, Boolean.FALSE.equals(safeForm.getNeedConfirm()) ? "否" : "是", styles.bodyStyle);
            merge(sheet, metaRow.getRowNum(), metaRow.getRowNum(), 7, widths.length - 1);

            rowIndex++;
            Row headerTitleRow = sheet.createRow(rowIndex++);
            writeCell(headerTitleRow, 0, "表头信息", styles.panelStyle);
            merge(sheet, headerTitleRow.getRowNum(), headerTitleRow.getRowNum(), 0, widths.length - 1);

            Row headerTableRow = sheet.createRow(rowIndex++);
            writeCell(headerTableRow, 0, "字段Key", styles.tableHeaderStyle);
            writeCell(headerTableRow, 1, "字段名称", styles.tableHeaderStyle);
            writeCell(headerTableRow, 2, "字段值", styles.tableHeaderStyle);
            merge(sheet, headerTableRow.getRowNum(), headerTableRow.getRowNum(), 2, widths.length - 1);

            for (String field : headerFields) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, field, styles.bodyStyle);
                writeCell(row, 1, resolveHeaderFieldLabel(field), styles.bodyStyle);
                writeCell(row, 2, safeText(headerData.get(field)), styles.editableStyle);
                merge(sheet, row.getRowNum(), row.getRowNum(), 2, widths.length - 1);
            }

            rowIndex++;
            Row detailTitleRow = sheet.createRow(rowIndex++);
            writeCell(detailTitleRow, 0, "明细项配置", styles.panelStyle);
            merge(sheet, detailTitleRow.getRowNum(), detailTitleRow.getRowNum(), 0, widths.length - 1);

            Row itemHeaderRow = sheet.createRow(rowIndex++);
            String[] itemHeaders = {"序号", "分类", "步骤节点", "项目名称", "标准说明", "值模式", "标签1", "标签2", "默认结果", "必填", "备注", "子字段定义JSON"};
            for (int index = 0; index < itemHeaders.length; index++) {
                writeCell(itemHeaderRow, index, itemHeaders[index], styles.tableHeaderStyle);
            }
            sheet.createFreezePane(0, itemHeaderRow.getRowNum() + 1);

            for (int index = 0; index < items.size(); index++) {
                HcStationFormItemSaveReqVO item = items.get(index);
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, safeText(item.getItemSeq() == null ? index + 1 : item.getItemSeq()), styles.editableStyle);
                writeCell(row, 1, safeText(item.getItemCategory()), styles.editableStyle);
                writeCell(row, 2, safeText(item.getStepNode()), styles.editableStyle);
                writeCell(row, 3, safeText(item.getItemName()), styles.editableStyle);
                writeCell(row, 4, safeText(item.getStandardText()), styles.editableStyle);
                writeCell(row, 5, resolveValueModeLabel(item.getValueMode()), styles.editableStyle);
                writeCell(row, 6, safeText(item.getDualLabel1()), styles.editableStyle);
                writeCell(row, 7, safeText(item.getDualLabel2()), styles.editableStyle);
                writeCell(row, 8, safeText(item.getDefaultResult()), styles.editableStyle);
                writeCell(row, 9, Boolean.FALSE.equals(item.getRequiredFlag()) ? "否" : "是", styles.editableStyle);
                writeCell(row, 10, safeText(item.getRemark()), styles.editableStyle);
                writeCell(row, 11, safeText(item.getFieldDefinitionsJson()), styles.editableStyle);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @Override
    public HcStationFormImportRespVO importPreviewExcel(MultipartFile file, String formJson) throws IOException {
        HcStationFormImportRespVO respVO = new HcStationFormImportRespVO();
        HcStationFormSaveReqVO form = parsePreviewFormJson(formJson, respVO);
        respVO.setForm(form);
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        respVO.setSourceFileName(file.getOriginalFilename());

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            respVO.setSourceSheetName(sheet.getSheetName());
            if (isPressSlotIntermediateRuntimeDevForm(form)) {
                importPressSlotIntermediateRuntimePreviewExcel(sheet, formatter, form, respVO);
                return respVO;
            }

            int headerTitleRow = findRowByFirstCell(sheet, formatter, "表头信息");
            int itemTitleRow = findRowByFirstCell(sheet, formatter, "明细项配置");
            if (itemTitleRow < 0) {
                addImportFailure(respVO, "未识别到【明细项配置】区域");
                return respVO;
            }

            Map<String, Object> headerData = parseJsonMap(form.getPresetHeaderDataJson());
            if (headerTitleRow >= 0) {
                readPreviewHeaderRows(sheet, formatter, headerTitleRow + 2, itemTitleRow, headerData);
            }
            List<HcStationFormItemSaveReqVO> items = readPreviewItemRows(sheet, formatter, itemTitleRow + 2, respVO);
            if (items.isEmpty()) {
                addImportFailure(respVO, "未从 Excel 中读取到明细项");
                return respVO;
            }

            form.setPresetHeaderDataJson(headerData.isEmpty() ? null : JsonUtils.toJsonString(headerData));
            form.setItems(items);
            form.setPresetItems(items);
            respVO.setSuccessCount(items.size());
            respVO.getMessages().add(String.format("预览 Excel 已解析：表头 %d 项，明细 %d 行", headerData.size(), items.size()));
        } catch (Exception ex) {
            addImportFailure(respVO, "预览 Excel 导入失败：" + ex.getMessage());
        }
        return respVO;
    }

    @Override
    public byte[] exportConfigPackage(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw exception(HCSTATIONFORM_IMPORT_INVALID);
        }
        List<Long> exportIds = new ArrayList<>(new LinkedHashSet<>(ids));
        List<HcStationFormDO> forms = hcStationFormMapper.selectBatchIds(exportIds);
        if (forms == null || forms.isEmpty()) {
            throw exception(HCSTATIONFORM_IMPORT_INVALID);
        }
        Map<Long, Integer> orderMap = new LinkedHashMap<>();
        for (int index = 0; index < exportIds.size(); index++) {
            orderMap.put(exportIds.get(index), index);
        }
        forms.sort(Comparator.comparingInt(form -> orderMap.getOrDefault(form.getId(), Integer.MAX_VALUE)));

        HcStationFormConfigPackageRespVO configPackage = buildBaseConfigPackage();
        configPackage.setSource("HC-MES 动态表单配置导出");
        configPackage.setTotalCount(forms.size());
        configPackage.getMessages().add(String.format("已导出 %d 个动态表单配置。", forms.size()));
        if (forms.size() != exportIds.size()) {
            configPackage.getMessages().add(String.format("有 %d 个选择项未找到，已自动跳过。", exportIds.size() - forms.size()));
        }
        for (HcStationFormDO form : forms) {
            configPackage.getForms().add(buildConfigPackageForm(form));
        }
        return JsonUtils.toJsonString(configPackage).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public HcStationFormConfigPackageRespVO previewConfigPackageImport(MultipartFile file) throws IOException {
        HcStationFormConfigPackageRespVO configPackage = parseConfigPackage(file);
        if ("ERROR".equals(configPackage.getStatus())) {
            return configPackage;
        }
        return analyzeConfigPackage(configPackage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcStationFormConfigPackageRespVO confirmConfigPackageImport(HcStationFormConfigPackageImportReqVO reqVO) {
        if (reqVO == null || reqVO.getConfigPackage() == null) {
            HcStationFormConfigPackageRespVO respVO = buildBaseConfigPackage();
            addPackageFailure(respVO, "配置包不能为空");
            return respVO;
        }
        HcStationFormConfigPackageRespVO analyzed = analyzeConfigPackage(reqVO.getConfigPackage());
        if (analyzed.getFailureCount() > 0) {
            return analyzed;
        }
        boolean overwriteExisting = Boolean.TRUE.equals(reqVO.getOverwriteExisting());
        if (analyzed.getUpdateCount() > 0 && !overwriteExisting) {
            addPackageFailure(analyzed, "配置包包含同编码且内容不同的表单，请勾选“覆盖同编码配置”后再确认导入");
            return analyzed;
        }

        int importedNew = 0;
        int importedUpdate = 0;
        int skipped = 0;
        for (HcStationFormConfigPackageFormVO item : analyzed.getForms()) {
            HcStationFormSaveReqVO form = item.getForm();
            if (form == null) {
                continue;
            }
            String action = item.getAction();
            if ("NEW".equals(action)) {
                form.setId(null);
                createHcStationForm(form);
                importedNew++;
            } else if ("UPDATE".equals(action)) {
                form.setId(item.getExistingId());
                updateHcStationForm(form);
                importedUpdate++;
            } else {
                skipped++;
            }
        }

        HcStationFormConfigPackageRespVO respVO = analyzeConfigPackage(reqVO.getConfigPackage());
        respVO.setNewCount(importedNew);
        respVO.setUpdateCount(importedUpdate);
        respVO.setSkippedCount(skipped);
        respVO.getMessages().add(String.format("配置包导入完成：新增 %d 个，覆盖更新 %d 个，跳过 %d 个。",
                importedNew, importedUpdate, skipped));
        return respVO;
    }

    private HcStationFormConfigPackageRespVO buildBaseConfigPackage() {
        HcStationFormConfigPackageRespVO configPackage = new HcStationFormConfigPackageRespVO();
        configPackage.setPackageType(CONFIG_PACKAGE_TYPE);
        configPackage.setPackageVersion(CONFIG_PACKAGE_VERSION);
        configPackage.setExportTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return configPackage;
    }

    private HcStationFormConfigPackageFormVO buildConfigPackageForm(HcStationFormDO formDO) {
        HcStationFormConfigPackageFormVO item = new HcStationFormConfigPackageFormVO();
        HcStationFormSaveReqVO form = BeanUtils.toBean(formDO, HcStationFormSaveReqVO.class);
        form.setId(null);
        List<HcStationFormItemSaveReqVO> items = BeanUtils.toBean(
                hcStationFormItemMapper.selectByFormId(formDO.getId()), HcStationFormItemSaveReqVO.class);
        clearPackageItemIds(items);
        form.setItems(items);

        List<HcStationFormItemSaveReqVO> presetItems = parsePresetItemsJson(formDO.getPresetItemsJson());
        if (presetItems.isEmpty()) {
            presetItems = new ArrayList<>(items);
        }
        clearPackageItemIds(presetItems);
        form.setPresetItems(presetItems);
        normalizeSaveForm(form);
        form.setId(null);

        item.setForm(form);
        item.setChecksum(buildConfigChecksum(form));
        return item;
    }

    private HcStationFormConfigPackageRespVO parseConfigPackage(MultipartFile file) throws IOException {
        HcStationFormConfigPackageRespVO respVO = buildBaseConfigPackage();
        if (file == null || file.isEmpty()) {
            addPackageFailure(respVO, "配置包文件为空");
            return respVO;
        }
        respVO.setSourceFileName(file.getOriginalFilename());
        String json = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }
        try {
            HcStationFormConfigPackageRespVO parsed = JsonUtils.parseObject(json, HcStationFormConfigPackageRespVO.class);
            if (parsed == null || parsed.getForms() == null || parsed.getForms().isEmpty()) {
                addPackageFailure(respVO, "配置包未包含任何动态表单");
                return respVO;
            }
            if (!CONFIG_PACKAGE_TYPE.equals(parsed.getPackageType())) {
                addPackageFailure(respVO, "配置包类型不正确，请选择动态表单配置包 JSON 文件");
                return respVO;
            }
            parsed.setSourceFileName(file.getOriginalFilename());
            return parsed;
        } catch (Exception ex) {
            addPackageFailure(respVO, "配置包解析失败：" + ex.getMessage());
            return respVO;
        }
    }

    private HcStationFormConfigPackageRespVO analyzeConfigPackage(HcStationFormConfigPackageRespVO sourcePackage) {
        HcStationFormConfigPackageRespVO respVO = buildBaseConfigPackage();
        if (sourcePackage != null) {
            respVO.setPackageType(firstNotBlank(sourcePackage.getPackageType(), CONFIG_PACKAGE_TYPE));
            respVO.setPackageVersion(firstNotBlank(sourcePackage.getPackageVersion(), CONFIG_PACKAGE_VERSION));
            respVO.setExportTime(sourcePackage.getExportTime());
            respVO.setSource(sourcePackage.getSource());
            respVO.setSourceFileName(sourcePackage.getSourceFileName());
        }
        if (sourcePackage == null || sourcePackage.getForms() == null || sourcePackage.getForms().isEmpty()) {
            addPackageFailure(respVO, "配置包未包含任何动态表单");
            return respVO;
        }
        if (!CONFIG_PACKAGE_TYPE.equals(sourcePackage.getPackageType())) {
            addPackageFailure(respVO, "配置包类型不正确，请选择动态表单配置包 JSON 文件");
            return respVO;
        }

        for (HcStationFormConfigPackageFormVO sourceItem : sourcePackage.getForms()) {
            HcStationFormConfigPackageFormVO item = new HcStationFormConfigPackageFormVO();
            HcStationFormSaveReqVO form = sourceItem == null ? null : sourceItem.getForm();
            item.setForm(form);
            respVO.getForms().add(item);
            respVO.setTotalCount(respVO.getTotalCount() + 1);
            if (form != null) {
                normalizeSaveForm(form);
            }
            if (!validateConfigPackageForm(respVO, item)) {
                continue;
            }

            form.setId(null);
            String checksum = buildConfigChecksum(form);
            item.setChecksum(checksum);

            HcStationFormDO existing = hcStationFormMapper.selectByFormCode(form.getFormCode());
            if (existing == null) {
                item.setAction("NEW");
                respVO.setNewCount(respVO.getNewCount() + 1);
                respVO.getMessages().add(String.format("表单 %s（%s）将新增。", form.getFormName(), form.getFormCode()));
                continue;
            }

            HcStationFormConfigPackageFormVO existingItem = buildConfigPackageForm(existing);
            item.setExistingId(existing.getId());
            item.setExistingFormName(existing.getFormName());
            item.setExistingChecksum(existingItem.getChecksum());
            item.setDiffFields(resolveConfigDiffFields(form, existingItem.getForm()));
            if (Objects.equals(checksum, existingItem.getChecksum())) {
                item.setAction("SAME");
                respVO.setSameCount(respVO.getSameCount() + 1);
                respVO.getMessages().add(String.format("表单 %s（%s）无变化。", form.getFormName(), form.getFormCode()));
            } else {
                item.setAction("UPDATE");
                respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                respVO.getMessages().add(String.format("表单 %s（%s）将覆盖更新，差异：%s。",
                        form.getFormName(), form.getFormCode(),
                        item.getDiffFields().isEmpty() ? "配置内容" : String.join("、", item.getDiffFields())));
            }
        }
        return respVO;
    }

    private boolean validateConfigPackageForm(HcStationFormConfigPackageRespVO respVO,
                                              HcStationFormConfigPackageFormVO item) {
        HcStationFormSaveReqVO form = item.getForm();
        if (form == null) {
            item.setAction("INVALID");
            addPackageFailure(respVO, "配置包存在空表单项");
            return false;
        }
        List<String> missingFields = new ArrayList<>();
        if (!StringUtils.hasText(form.getFormCode())) {
            missingFields.add("表单编码");
        }
        if (!StringUtils.hasText(form.getFormName())) {
            missingFields.add("表单名称");
        }
        if (!StringUtils.hasText(form.getProcessCode())) {
            missingFields.add("业务工序");
        }
        if (!StringUtils.hasText(form.getTriggerTimingCode())) {
            missingFields.add("触发时机");
        }
        if (!missingFields.isEmpty()) {
            item.setAction("INVALID");
            addPackageFailure(respVO, String.format("表单 %s 缺少必填配置：%s",
                    firstNotBlank(form.getFormCode(), form.getFormName(), "-"), String.join("、", missingFields)));
            return false;
        }
        if (hasDevFormContractViolation(form)) {
            item.setAction("INVALID");
            addPackageFailure(respVO, String.format("表单 %s 的 DEV 标识与编码后缀不一致",
                    firstNotBlank(form.getFormCode(), form.getFormName(), "-")));
            return false;
        }
        return true;
    }

    private List<String> resolveConfigDiffFields(HcStationFormSaveReqVO incoming, HcStationFormSaveReqVO existing) {
        List<String> diffFields = new ArrayList<>();
        if (!Objects.equals(normalizeText(incoming.getFormName()), normalizeText(existing.getFormName()))) {
            diffFields.add("表单名称");
        }
        if (!Objects.equals(normalizeText(incoming.getProcessCode()), normalizeText(existing.getProcessCode()))
                || !Objects.equals(normalizeText(incoming.getProcessName()), normalizeText(existing.getProcessName()))) {
            diffFields.add("业务工序");
        }
        if (!Objects.equals(normalizeText(incoming.getTriggerTimingCode()), normalizeText(existing.getTriggerTimingCode()))
                || !Objects.equals(normalizeText(incoming.getTriggerTimingName()), normalizeText(existing.getTriggerTimingName()))) {
            diffFields.add("触发时机");
        }
        if (!Objects.equals(incoming.getNeedConfirm(), existing.getNeedConfirm())) {
            diffFields.add("需确认");
        }
        if (!Objects.equals(incoming.getSortNo(), existing.getSortNo())
                || !Objects.equals(incoming.getStatus(), existing.getStatus())) {
            diffFields.add("排序/状态");
        }
        if (!Objects.equals(canonicalJson(incoming.getSchemaJson()), canonicalJson(existing.getSchemaJson()))) {
            diffFields.add("设计器布局");
        }
        if (!Objects.equals(canonicalJson(incoming.getPresetHeaderDataJson()), canonicalJson(existing.getPresetHeaderDataJson()))) {
            diffFields.add("预设表头");
        }
        if (!Objects.equals(buildComparableItems(incoming.getPresetItems()), buildComparableItems(existing.getPresetItems()))) {
            diffFields.add("预设明细");
        }
        if (!Objects.equals(buildComparableItems(incoming.getItems()), buildComparableItems(existing.getItems()))) {
            diffFields.add("明细项");
        }
        if (!Objects.equals(normalizeText(incoming.getRemark()), normalizeText(existing.getRemark()))) {
            diffFields.add("备注");
        }
        return diffFields;
    }

    private String buildConfigChecksum(HcStationFormSaveReqVO form) {
        return sha256Hex(JsonUtils.toJsonString(buildComparableForm(form)));
    }

    private Map<String, Object> buildComparableForm(HcStationFormSaveReqVO form) {
        Map<String, Object> comparable = new LinkedHashMap<>();
        comparable.put("formCode", normalizeText(form.getFormCode()));
        comparable.put("formName", normalizeText(form.getFormName()));
        comparable.put("processCode", normalizeText(form.getProcessCode()));
        comparable.put("processName", normalizeText(form.getProcessName()));
        comparable.put("triggerTimingCode", normalizeText(form.getTriggerTimingCode()));
        comparable.put("triggerTimingName", normalizeText(form.getTriggerTimingName()));
        comparable.put("needConfirm", form.getNeedConfirm());
        comparable.put("sortNo", form.getSortNo());
        comparable.put("status", form.getStatus());
        comparable.put("schemaJson", canonicalJson(form.getSchemaJson()));
        comparable.put("presetHeaderDataJson", canonicalJson(form.getPresetHeaderDataJson()));
        comparable.put("presetItems", buildComparableItems(form.getPresetItems()));
        comparable.put("remark", normalizeText(form.getRemark()));
        comparable.put("items", buildComparableItems(form.getItems()));
        return comparable;
    }

    private List<Map<String, Object>> buildComparableItems(List<HcStationFormItemSaveReqVO> items) {
        List<Map<String, Object>> comparableItems = new ArrayList<>();
        if (items == null) {
            return comparableItems;
        }
        for (HcStationFormItemSaveReqVO item : items) {
            Map<String, Object> comparable = new LinkedHashMap<>();
            comparable.put("itemSeq", item.getItemSeq());
            comparable.put("itemCategory", normalizeText(item.getItemCategory()));
            comparable.put("stepNode", normalizeText(item.getStepNode()));
            comparable.put("itemName", normalizeText(item.getItemName()));
            comparable.put("standardText", normalizeText(item.getStandardText()));
            comparable.put("valueMode", normalizeText(item.getValueMode()));
            comparable.put("dualLabel1", normalizeText(item.getDualLabel1()));
            comparable.put("dualLabel2", normalizeText(item.getDualLabel2()));
            comparable.put("fieldDefinitionsJson", item.getFieldDefinitionsJson());
            comparable.put("defaultResult", normalizeText(item.getDefaultResult()));
            comparable.put("requiredFlag", item.getRequiredFlag());
            comparable.put("remark", normalizeText(item.getRemark()));
            comparableItems.add(comparable);
        }
        return comparableItems;
    }

    private String canonicalJson(String json) {
        if (!StringUtils.hasText(json)) {
            return "";
        }
        try {
            return JsonUtils.toJsonString(JsonUtils.parseTree(json));
        } catch (Exception ignored) {
            return normalizeText(json);
        }
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private void clearPackageItemIds(List<HcStationFormItemSaveReqVO> items) {
        if (items == null) {
            return;
        }
        for (HcStationFormItemSaveReqVO item : items) {
            item.setId(null);
        }
    }

    private List<HcStationFormItemSaveReqVO> parsePresetItemsJson(String presetItemsJson) {
        if (!StringUtils.hasText(presetItemsJson)) {
            return new ArrayList<>();
        }
        try {
            List<HcStationFormItemSaveReqVO> items = JsonUtils.parseArray(presetItemsJson, HcStationFormItemSaveReqVO.class);
            return items == null ? new ArrayList<>() : items;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private void addPackageFailure(HcStationFormConfigPackageRespVO respVO, String message) {
        respVO.setStatus("ERROR");
        respVO.setFailureCount(respVO.getFailureCount() + 1);
        respVO.getFailures().add(message);
        respVO.getMessages().add(message);
    }

    private boolean isPressSlotIntermediateRuntimeDevForm(HcStationFormSaveReqVO form) {
        return form != null
                && PRESS_SLOT_INTERMEDIATE_RUNTIME_DEV_FORM_CODE.equalsIgnoreCase(normalizeText(form.getFormCode()));
    }

    private byte[] exportPressSlotIntermediateRuntimePreviewExcel(HcStationFormSaveReqVO form,
                                                                  List<HcStationFormItemSaveReqVO> items,
                                                                  Map<String, Object> headerData) throws IOException {
        int thicknessCount = resolvePressSlotRuntimeThicknessCount(form.getSchemaJson());
        int intervalCm = resolvePressSlotRuntimeIntervalCm(form.getSchemaJson());
        int lastCol = Math.max(7, 2 + thicknessCount);
        String title = resolvePressSlotRuntimeTitle(form);
        String depthStandard = firstNotBlank(resolvePressSlotRuntimeStandard(items, "槽深"), "-");
        String thicknessStandard = firstNotBlank(resolvePressSlotRuntimeStandard(items, "厚度"), "-");
        List<Map<String, Object>> details = resolvePressSlotRuntimeDetails(headerData, thicknessCount);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(title));
            StationFormExcelStyles styles = buildStationFormExcelStyles(workbook);
            for (int index = 0; index <= lastCol; index++) {
                int width = index == 0 ? 14 : index == 1 ? 20 : index == 2 ? 14 : 22;
                sheet.setColumnWidth(index, width * 256);
            }

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(24);
            writeCell(titleRow, 0, "查看 " + title, styles.titleStyle);
            merge(sheet, titleRow.getRowNum(), titleRow.getRowNum(), 0, 1);
            writeCell(titleRow, 2, "计划号", styles.headerLabelStyle);
            writeCell(titleRow, 3, firstHeaderText(headerData, "planNo"), styles.bodyStyle);
            writeCell(titleRow, 4, "压槽片号", styles.headerLabelStyle);
            writeCell(titleRow, 5, firstHeaderText(headerData, "pressSlotSliceNo", "sliceBatchNo"), styles.bodyStyle);
            int sliceValueLastCol = Math.max(5, lastCol - 2);
            merge(sheet, titleRow.getRowNum(), titleRow.getRowNum(), 5, sliceValueLastCol);
            writeCell(titleRow, lastCol - 1, "状态", styles.headerLabelStyle);
            writeCell(titleRow, lastCol, firstNotBlank(firstHeaderText(headerData, "statusName", "status"), "预览"), styles.bodyStyle);

            rowIndex++;
            Row formInfoTitleRow = sheet.createRow(rowIndex++);
            writeCell(formInfoTitleRow, 0, "表单信息", styles.panelStyle);
            merge(sheet, formInfoTitleRow.getRowNum(), formInfoTitleRow.getRowNum(), 0, lastCol);

            Row formInfoRow = sheet.createRow(rowIndex++);
            writeCell(formInfoRow, 0, "生产日期", styles.headerLabelStyle);
            writeCell(formInfoRow, 1, firstHeaderText(headerData, "productionDate", "recordDate"), styles.bodyStyle);
            writeCell(formInfoRow, 2, "型号", styles.headerLabelStyle);
            writeCell(formInfoRow, 3, firstHeaderText(headerData, "modelCode", "modelName"), styles.bodyStyle);
            writeCell(formInfoRow, 4, "料号", styles.headerLabelStyle);
            writeCell(formInfoRow, 5, firstHeaderText(headerData, "materialCode"), styles.bodyStyle);
            merge(sheet, formInfoRow.getRowNum(), formInfoRow.getRowNum(), 5, lastCol);

            Row batchRow = sheet.createRow(rowIndex++);
            writeCell(batchRow, 0, "批号", styles.headerLabelStyle);
            writeCell(batchRow, 1, firstHeaderText(headerData, "batchNo", "productionBatchNo"), styles.bodyStyle);
            merge(sheet, batchRow.getRowNum(), batchRow.getRowNum(), 1, lastCol);

            rowIndex++;
            Row depthTitleRow = sheet.createRow(rowIndex++);
            writeCell(depthTitleRow, 0, "首件槽深/mm（标准：" + depthStandard + "）", styles.panelStyle);
            merge(sheet, depthTitleRow.getRowNum(), depthTitleRow.getRowNum(), 0, lastCol);
            Row depthHeaderRow = sheet.createRow(rowIndex++);
            Row depthValueRow = sheet.createRow(rowIndex++);
            writeMergedRuntimeCell(sheet, depthHeaderRow, 0, 2, "XY最小值", styles.tableHeaderStyle);
            writeMergedRuntimeCell(sheet, depthHeaderRow, 3, 5, "XY最大值", styles.tableHeaderStyle);
            writeMergedRuntimeCell(sheet, depthHeaderRow, 6, lastCol, "XY平均值", styles.tableHeaderStyle);
            writeMergedRuntimeCell(sheet, depthValueRow, 0, 2, firstHeaderText(headerData, "firstSlotDepthMin"), styles.editableStyle);
            writeMergedRuntimeCell(sheet, depthValueRow, 3, 5, firstHeaderText(headerData, "firstSlotDepthMax"), styles.editableStyle);
            writeMergedRuntimeCell(sheet, depthValueRow, 6, lastCol, firstHeaderText(headerData, "firstSlotDepthAvg"), styles.editableStyle);

            rowIndex++;
            Row attachmentTitleRow = sheet.createRow(rowIndex++);
            writeCell(attachmentTitleRow, 0, "原始导入附件", styles.panelStyle);
            merge(sheet, attachmentTitleRow.getRowNum(), attachmentTitleRow.getRowNum(), 0, lastCol);
            Row attachmentRow = sheet.createRow(rowIndex++);
            writeCell(attachmentRow, 0, formatPressSlotRuntimeAttachments(headerData), styles.bodyStyle);
            merge(sheet, attachmentRow.getRowNum(), attachmentRow.getRowNum(), 0, lastCol);

            rowIndex++;
            Row detailTitleRow = sheet.createRow(rowIndex++);
            writeCell(detailTitleRow, 0, "中间品记录明细", styles.panelStyle);
            merge(sheet, detailTitleRow.getRowNum(), detailTitleRow.getRowNum(), 0, lastCol);
            Row detailHeaderRow = sheet.createRow(rowIndex++);
            writeCell(detailHeaderRow, 0, "采样段", styles.tableHeaderStyle);
            writeCell(detailHeaderRow, 1, "片号", styles.tableHeaderStyle);
            writeCell(detailHeaderRow, 2, "宽幅/mm", styles.tableHeaderStyle);
            for (int index = 1; index <= thicknessCount; index++) {
                writeCell(detailHeaderRow, index + 2,
                        index * intervalCm + "cm厚度/mm(" + thicknessStandard + ")", styles.tableHeaderStyle);
            }
            sheet.createFreezePane(0, detailHeaderRow.getRowNum() + 1);

            for (Map<String, Object> detail : details) {
                Row detailRow = sheet.createRow(rowIndex++);
                writeCell(detailRow, 0, safeText(detail.get("samplePositionName")), styles.editableStyle);
                writeCell(detailRow, 1, safeText(detail.get("sliceBatchNo")), styles.editableStyle);
                writeCell(detailRow, 2, safeText(detail.get("widthMm")), styles.editableStyle);
                for (int index = 1; index <= thicknessCount; index++) {
                    writeCell(detailRow, index + 2, safeText(detail.get("thickness" + index)), styles.editableStyle);
                }
            }

            rowIndex++;
            Row signatureRow = sheet.createRow(rowIndex);
            writeCell(signatureRow, 0, "填写人", styles.headerLabelStyle);
            writeCell(signatureRow, 1, firstHeaderText(headerData, "recorderName", "recorder"), styles.bodyStyle);
            writeCell(signatureRow, 2, "填写时间", styles.headerLabelStyle);
            writeCell(signatureRow, 3, firstHeaderText(headerData, "recordTime", "recorderTime"), styles.bodyStyle);
            writeCell(signatureRow, 4, "确认人", styles.headerLabelStyle);
            writeCell(signatureRow, 5, firstHeaderText(headerData, "confirmerName", "confirmer"), styles.bodyStyle);
            writeCell(signatureRow, 6, "确认时间", styles.headerLabelStyle);
            writeCell(signatureRow, 7, firstHeaderText(headerData, "confirmTime", "confirmerTime"), styles.bodyStyle);
            if (lastCol > 7) {
                merge(sheet, signatureRow.getRowNum(), signatureRow.getRowNum(), 7, lastCol);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void importPressSlotIntermediateRuntimePreviewExcel(Sheet sheet, DataFormatter formatter,
                                                               HcStationFormSaveReqVO form,
                                                               HcStationFormImportRespVO respVO) {
        try {
            Map<String, Object> headerData = parseJsonMap(form.getPresetHeaderDataJson());
            readPressSlotRuntimePairRows(sheet, formatter, sheet.getFirstRowNum(), Math.min(sheet.getFirstRowNum() + 2, sheet.getLastRowNum()), headerData);

            int formInfoRow = findRowByFirstCellContains(sheet, formatter, "表单信息");
            if (formInfoRow >= 0) {
                readPressSlotRuntimePairRows(sheet, formatter, formInfoRow + 1, formInfoRow + 3, headerData);
            }

            int depthRow = findRowByFirstCellContains(sheet, formatter, "首件槽深");
            if (depthRow >= 0) {
                readPressSlotRuntimeDepthRows(sheet, formatter, depthRow + 1, headerData);
            }

            int attachmentRow = findRowByFirstCellContains(sheet, formatter, "原始导入附件");
            if (attachmentRow >= 0) {
                readPressSlotRuntimeAttachmentRow(sheet, formatter, attachmentRow + 1, headerData);
            }

            int detailRow = findRowByFirstCellContains(sheet, formatter, "中间品记录明细");
            if (detailRow < 0) {
                addImportFailure(respVO, "未识别到【中间品记录明细】区域");
                return;
            }

            List<Map<String, Object>> details = readPressSlotRuntimeDetailRows(sheet, formatter, detailRow + 1);
            if (details.isEmpty()) {
                addImportFailure(respVO, "未从运行时记录单读取到中间品明细");
                return;
            }
            readPressSlotRuntimePairRows(sheet, formatter, detailRow + 1, sheet.getLastRowNum(), headerData);

            headerData.put("previewDetails", details);
            form.setPresetHeaderDataJson(JsonUtils.toJsonString(headerData));
            respVO.setSuccessCount(details.size());
            respVO.getMessages().add(String.format("DEV 运行时记录单已解析：表头/签名 %d 项，明细 %d 行",
                    headerData.size(), details.size()));
        } catch (Exception ex) {
            addImportFailure(respVO, "DEV 运行时记录单导入失败：" + ex.getMessage());
        }
    }

    private void writeMergedRuntimeCell(Sheet sheet, Row row, int firstCol, int lastCol, String value, CellStyle style) {
        writeCell(row, firstCol, value, style);
        merge(sheet, row.getRowNum(), row.getRowNum(), firstCol, lastCol);
    }

    private String resolvePressSlotRuntimeTitle(HcStationFormSaveReqVO form) {
        try {
            JsonNode schema = JsonUtils.parseTree(form.getSchemaJson());
            return firstNotBlank(
                    schema.path("runtimeLayout").path("title").asText(""),
                    schema.path("displayName").asText(""),
                    form.getFormName(),
                    "压槽中间品记录表");
        } catch (Exception ignored) {
            return firstNotBlank(form.getFormName(), "压槽中间品记录表");
        }
    }

    private int resolvePressSlotRuntimeThicknessCount(String schemaJson) {
        try {
            int count = JsonUtils.parseTree(schemaJson).path("thicknessColumnCount").asInt(5);
            return Math.max(1, Math.min(count, 30));
        } catch (Exception ignored) {
            return 5;
        }
    }

    private int resolvePressSlotRuntimeIntervalCm(String schemaJson) {
        try {
            int interval = JsonUtils.parseTree(schemaJson).path("thicknessIntervalCm").asInt(100);
            return interval > 0 ? interval : 100;
        } catch (Exception ignored) {
            return 100;
        }
    }

    private String resolvePressSlotRuntimeStandard(List<HcStationFormItemSaveReqVO> items, String keyword) {
        if (items == null) {
            return "";
        }
        for (HcStationFormItemSaveReqVO item : items) {
            String text = safeText(item.getItemCategory()) + safeText(item.getItemName()) + safeText(item.getStandardText());
            if (normalizeText(text).contains(keyword) && StringUtils.hasText(item.getStandardText())) {
                return extractRuntimeStandard(item.getStandardText());
            }
        }
        return "";
    }

    private String extractRuntimeStandard(String text) {
        String value = normalizeText(text);
        int leftIndex = Math.max(value.indexOf('('), value.indexOf('（'));
        int rightIndex = Math.max(value.indexOf(')'), value.indexOf('）'));
        if (leftIndex >= 0 && rightIndex > leftIndex) {
            return value.substring(leftIndex + 1, rightIndex).trim();
        }
        return value;
    }

    private String firstHeaderText(Map<String, Object> headerData, String... keys) {
        if (headerData == null) {
            return "";
        }
        for (String key : keys) {
            String value = safeText(headerData.get(key));
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolvePressSlotRuntimeDetails(Map<String, Object> headerData, int thicknessCount) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object details = headerData == null ? null : headerData.get("previewDetails");
        if (details instanceof List<?> detailList) {
            for (Object detail : detailList) {
                if (detail instanceof Map<?, ?> detailMap) {
                    rows.add(new LinkedHashMap<>((Map<String, Object>) detailMap));
                }
            }
        }
        if (rows.isEmpty()) {
            rows.add(defaultPressSlotRuntimeDetail(1, "前段", thicknessCount));
            rows.add(defaultPressSlotRuntimeDetail(2, "中段", thicknessCount));
            rows.add(defaultPressSlotRuntimeDetail(3, "后段", thicknessCount));
        }
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            row.putIfAbsent("seq", index + 1);
            row.putIfAbsent("samplePositionName", List.of("前段", "中段", "后段").get(Math.min(index, 2)));
            row.putIfAbsent("sliceBatchNo", "-");
            row.putIfAbsent("widthMm", "-");
            for (int thicknessIndex = 1; thicknessIndex <= thicknessCount; thicknessIndex++) {
                row.putIfAbsent("thickness" + thicknessIndex, "-");
            }
        }
        return rows;
    }

    private Map<String, Object> defaultPressSlotRuntimeDetail(int seq, String samplePositionName, int thicknessCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("seq", seq);
        row.put("samplePositionName", samplePositionName);
        row.put("sliceBatchNo", "-");
        row.put("widthMm", "-");
        for (int index = 1; index <= thicknessCount; index++) {
            row.put("thickness" + index, "-");
        }
        return row;
    }

    private String formatPressSlotRuntimeAttachments(Map<String, Object> headerData) {
        Object attachments = headerData == null ? null : headerData.get("attachments");
        if (!(attachments instanceof List<?> attachmentList) || attachmentList.isEmpty()) {
            return "暂无原始导入附件";
        }
        List<String> names = new ArrayList<>();
        for (Object attachment : attachmentList) {
            if (attachment instanceof Map<?, ?> attachmentMap) {
                String name = safeText(attachmentMap.get("name"));
                String uploadTime = safeText(attachmentMap.get("uploadTime"));
                if (StringUtils.hasText(name)) {
                    names.add(StringUtils.hasText(uploadTime) ? name + "（导入时间：" + uploadTime + "）" : name);
                }
            } else if (StringUtils.hasText(safeText(attachment))) {
                names.add(safeText(attachment));
            }
        }
        return names.isEmpty() ? "暂无原始导入附件" : String.join("；", names);
    }

    private void readPressSlotRuntimePairRows(Sheet sheet, DataFormatter formatter, int startRow, int endRow,
                                              Map<String, Object> headerData) {
        for (int rowIndex = startRow; rowIndex <= Math.min(endRow, sheet.getLastRowNum()); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int col = Math.max(0, row.getFirstCellNum()); col < row.getLastCellNum() - 1; col++) {
                String key = resolvePressSlotRuntimeHeaderKey(readDirectCell(row, col, formatter));
                if (StringUtils.hasText(key)) {
                    headerData.put(key, readDirectCell(row, col + 1, formatter));
                }
            }
        }
    }

    private String resolvePressSlotRuntimeHeaderKey(String label) {
        String text = normalizeText(label).replace("：", "").replace(":", "");
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.contains("计划号")) {
            return "planNo";
        }
        if (text.contains("压槽片号")) {
            return "pressSlotSliceNo";
        }
        if (text.contains("状态")) {
            return "statusName";
        }
        if (text.contains("生产日期")) {
            return "productionDate";
        }
        if (text.contains("型号")) {
            return "modelCode";
        }
        if (text.contains("料号")) {
            return "materialCode";
        }
        if (text.contains("批号")) {
            return "batchNo";
        }
        if (text.contains("填写人")) {
            return "recorderName";
        }
        if (text.contains("填写时间")) {
            return "recordTime";
        }
        if (text.contains("确认人")) {
            return "confirmerName";
        }
        if (text.contains("确认时间")) {
            return "confirmTime";
        }
        return "";
    }

    private void readPressSlotRuntimeDepthRows(Sheet sheet, DataFormatter formatter, int headerRowIndex,
                                               Map<String, Object> headerData) {
        Row headerRow = sheet.getRow(headerRowIndex);
        Row valueRow = sheet.getRow(headerRowIndex + 1);
        if (headerRow == null || valueRow == null) {
            return;
        }
        for (int col = Math.max(0, headerRow.getFirstCellNum()); col < headerRow.getLastCellNum(); col++) {
            String label = normalizeText(readDirectCell(headerRow, col, formatter));
            if (!StringUtils.hasText(label)) {
                continue;
            }
            String value = readDirectCell(valueRow, col, formatter);
            if (label.contains("最小")) {
                headerData.put("firstSlotDepthMin", value);
            } else if (label.contains("最大")) {
                headerData.put("firstSlotDepthMax", value);
            } else if (label.contains("平均")) {
                headerData.put("firstSlotDepthAvg", value);
            }
        }
    }

    private void readPressSlotRuntimeAttachmentRow(Sheet sheet, DataFormatter formatter, int rowIndex,
                                                   Map<String, Object> headerData) {
        Row row = sheet.getRow(rowIndex);
        String text = normalizeText(joinDirectRowText(row, formatter));
        if (!StringUtils.hasText(text) || text.contains("暂无")) {
            return;
        }
        List<Map<String, Object>> attachments = new ArrayList<>();
        for (String name : text.split("[；;]")) {
            String normalizedName = normalizeText(name).replaceAll("（导入时间：.*?）", "");
            if (StringUtils.hasText(normalizedName)) {
                Map<String, Object> attachment = new LinkedHashMap<>();
                attachment.put("name", normalizedName);
                attachments.add(attachment);
            }
        }
        if (!attachments.isEmpty()) {
            headerData.put("attachments", attachments);
        }
    }

    private List<Map<String, Object>> readPressSlotRuntimeDetailRows(Sheet sheet, DataFormatter formatter,
                                                                     int headerRowIndex) {
        Row headerRow = sheet.getRow(headerRowIndex);
        Map<Integer, String> columnKeys = resolvePressSlotRuntimeDetailColumns(headerRow, formatter);
        List<Map<String, Object>> details = new ArrayList<>();
        int blankRows = 0;
        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || countDirectNonEmptyCells(row, formatter) == 0) {
                blankRows++;
                if (blankRows > 1 && !details.isEmpty()) {
                    break;
                }
                continue;
            }
            blankRows = 0;
            String firstCell = normalizeText(readDirectCell(row, 0, formatter));
            if (firstCell.contains("填写人") || firstCell.contains("确认人")) {
                break;
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> entry : columnKeys.entrySet()) {
                detail.put(entry.getValue(), readDirectCell(row, entry.getKey(), formatter));
            }
            if (!StringUtils.hasText(safeText(detail.get("samplePositionName")))
                    && !StringUtils.hasText(safeText(detail.get("sliceBatchNo")))
                    && !StringUtils.hasText(safeText(detail.get("widthMm")))) {
                continue;
            }
            detail.put("seq", details.size() + 1);
            details.add(detail);
        }
        return details;
    }

    private Map<Integer, String> resolvePressSlotRuntimeDetailColumns(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> columns = new LinkedHashMap<>();
        if (headerRow == null) {
            columns.put(0, "samplePositionName");
            columns.put(1, "sliceBatchNo");
            columns.put(2, "widthMm");
            for (int index = 1; index <= 5; index++) {
                columns.put(index + 2, "thickness" + index);
            }
            return columns;
        }
        int thicknessIndex = 1;
        for (int col = Math.max(0, headerRow.getFirstCellNum()); col < headerRow.getLastCellNum(); col++) {
            String text = normalizeText(readDirectCell(headerRow, col, formatter));
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (text.contains("采样段")) {
                columns.put(col, "samplePositionName");
            } else if (text.contains("片号")) {
                columns.put(col, "sliceBatchNo");
            } else if (text.contains("宽幅")) {
                columns.put(col, "widthMm");
            } else if (text.contains("厚度")) {
                columns.put(col, "thickness" + thicknessIndex++);
            } else if (text.contains("备注")) {
                columns.put(col, "remark");
            }
        }
        return columns;
    }

    private int findRowByFirstCellContains(Sheet sheet, DataFormatter formatter, String text) {
        if (sheet == null) {
            return -1;
        }
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (normalizeText(readDirectCell(row, 0, formatter)).contains(text)) {
                return rowIndex;
            }
        }
        return -1;
    }

    private void validateHcStationFormExists(Long id) {
        if (hcStationFormMapper.selectById(id) == null) {
            throw exception(HCSTATIONFORM_NOT_EXISTS);
        }
    }

    private void validateFormCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcStationFormDO entity = hcStationFormMapper.selectOne(
                new LambdaQueryWrapperX<HcStationFormDO>()
                        .eq(HcStationFormDO::getFormCode, value)
                        .neIfPresent(HcStationFormDO::getId, id));
        if (entity != null) {
            throw exception(HCSTATIONFORM_FORMCODE_EXISTS);
        }
    }

    /**
     * DEV 身份只由 schemaJson.devOnly 表示，编码后缀是其可审计的命名契约。
     * runtimeEngine 仅用于选择布局渲染器，不能再借其名称推断 DEV/正式身份。
     */
    private void validateDevFormContract(HcStationFormSaveReqVO form) {
        if (hasDevFormContractViolation(form)) {
            throw exception(HCSTATIONFORM_DEV_CONTRACT_INVALID);
        }
    }

    private boolean hasDevFormContractViolation(HcStationFormSaveReqVO form) {
        JsonNode schema = parseSchemaNode(form == null ? null : form.getSchemaJson());
        boolean devOnly = isSchemaBooleanTrue(schema, "devOnly");
        boolean devCode = isDevFormCode(form == null ? null : form.getFormCode());
        return devOnly != devCode;
    }

    private boolean isDevFormCode(String formCode) {
        return StringUtils.hasText(formCode)
                && formCode.trim().toUpperCase(Locale.ROOT).endsWith(DEV_FORM_CODE_SUFFIX);
    }

    private HcStationFormImportRespVO parseExcelImport(MultipartFile file, String formCode, String formName,
                                                       String processCode, String triggerTimingCode,
                                                       String presetTemplate) throws IOException {
        HcStationFormImportRespVO respVO = new HcStationFormImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        respVO.setSourceFileName(file.getOriginalFilename());

        DataFormatter formatter = new DataFormatter();
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = findImportSheet(workbook, formatter);
            if (sheet == null) {
                addImportFailure(respVO, "Excel 未包含可解析的 Sheet");
                return respVO;
            }
            respVO.setSourceSheetName(sheet.getSheetName());

            String resolvedFormName = StringUtils.hasText(formName)
                    ? normalizeText(formName)
                    : inferFormName(sheet, formatter, file.getOriginalFilename());
            String resolvedProcessCode = StringUtils.hasText(processCode) ? normalizeText(processCode) : "WET";
            String resolvedTriggerTimingCode = StringUtils.hasText(triggerTimingCode)
                    ? normalizeText(triggerTimingCode)
                    : "IN_PROCESS";
            String resolvedFormCode = StringUtils.hasText(formCode)
                    ? normalizeFormCode(formCode)
                    : buildImportFormCode(resolvedProcessCode, resolvedFormName, file.getOriginalFilename(), sheet.getSheetName());

            List<HcStationFormItemSaveReqVO> items = parseSheetItems(sheet, formatter, respVO);
            if (items.isEmpty()) {
                addImportFailure(respVO, "未从 Excel 中识别到可导入的表单明细");
            }

            HcStationFormSaveReqVO form = new HcStationFormSaveReqVO();
            form.setFormCode(resolvedFormCode);
            form.setFormName(resolvedFormName);
            form.setProcessCode(resolvedProcessCode);
            form.setProcessName(resolveProcessName(resolvedProcessCode));
            form.setTriggerTimingCode(resolvedTriggerTimingCode);
            form.setTriggerTimingName(resolveTriggerTimingName(resolvedTriggerTimingCode));
            form.setNeedConfirm(true);
            form.setSortNo(10);
            form.setStatus(1);
            form.setSchemaJson(buildImportSchemaJson(sheet, formatter, presetTemplate, file.getOriginalFilename()));
            form.setPresetHeaderDataJson(buildPresetHeaderDataJson(form.getSchemaJson()));
            form.setPresetItems(items);
            form.setItems(items);
            form.setRemark(String.format("由 Excel 导入：%s / %s", file.getOriginalFilename(), sheet.getSheetName()));
            respVO.setForm(form);
            respVO.setSuccessCount(items.size());
            if (respVO.getFailures().isEmpty()) {
                respVO.getMessages().add(String.format("解析完成：Sheet【%s】，明细 %d 条", sheet.getSheetName(), items.size()));
            }
        } catch (Exception ex) {
            addImportFailure(respVO, "Excel 解析失败：" + ex.getMessage());
        }
        return respVO;
    }

    private Sheet findImportSheet(Workbook workbook, DataFormatter formatter) {
        if (workbook == null || workbook.getNumberOfSheets() == 0) {
            return null;
        }
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = normalizeText(sheet.getSheetName());
            if (sheetName.contains("改定") || sheetName.contains("履历") || sheetName.contains("修订")) {
                continue;
            }
            if (hasSheetContent(sheet, formatter)) {
                return sheet;
            }
        }
        return workbook.getSheetAt(0);
    }

    private boolean hasSheetContent(Sheet sheet, DataFormatter formatter) {
        if (sheet == null) {
            return false;
        }
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 30); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (countDirectNonEmptyCells(row, formatter) > 0) {
                return true;
            }
        }
        return false;
    }

    private String inferFormName(Sheet sheet, DataFormatter formatter, String fileName) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 8); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            short lastCellNum = row.getLastCellNum();
            for (int col = Math.max(0, row.getFirstCellNum()); col < lastCellNum; col++) {
                String text = normalizeText(readDirectCell(row, col, formatter));
                if (looksLikeFormTitle(text)) {
                    return limitText(text, 120);
                }
            }
        }
        String fallback = normalizeText(fileName);
        if (fallback.endsWith(".xlsx") || fallback.endsWith(".xls")) {
            fallback = fallback.substring(0, fallback.lastIndexOf('.'));
        }
        return StringUtils.hasText(fallback) ? limitText(fallback, 120) : "Excel导入动态表单";
    }

    private boolean looksLikeFormTitle(String text) {
        if (!StringUtils.hasText(text) || text.length() < 3 || text.length() > 120) {
            return false;
        }
        String compact = text.replace(" ", "");
        if (containsAny(compact, "编号", "版本", "页码", "日期", "制表", "审核", "批准")) {
            return false;
        }
        return containsAny(compact, "表", "记录单", "点检", "自检");
    }

    private List<HcStationFormItemSaveReqVO> parseSheetItems(Sheet sheet, DataFormatter formatter,
                                                             HcStationFormImportRespVO respVO) {
        ExcelHeader header = findDetailHeader(sheet, formatter);
        if (header == null) {
            respVO.setWarningCount(respVO.getWarningCount() + 1);
            respVO.getMessages().add("未识别到标准明细表头，已按非空行尝试兜底解析");
            return parseFallbackItems(sheet, formatter, respVO);
        }

        List<HcStationFormItemSaveReqVO> items = new ArrayList<>();
        String lastCategory = "";
        String lastNode = "";
        int blankRows = 0;
        for (int rowIndex = header.rowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || countDirectNonEmptyCells(row, formatter) == 0) {
                blankRows++;
                if (blankRows > 5 && !items.isEmpty()) {
                    break;
                }
                continue;
            }
            blankRows = 0;
            String rowText = joinDirectRowText(row, formatter);
            if (isFooterRow(rowText)) {
                if (!items.isEmpty()) {
                    break;
                }
                continue;
            }
            respVO.setTotalRows(respVO.getTotalRows() + 1);

            String category = readCellText(sheet, row, header.categoryCol, formatter);
            String node = readCellText(sheet, row, header.nodeCol, formatter);
            String itemName = readCellText(sheet, row, header.itemCol, formatter);
            if (!StringUtils.hasText(itemName)) {
                itemName = findFallbackItemName(sheet, row, formatter, header);
            }
            if (StringUtils.hasText(category)) {
                lastCategory = category;
            }
            if (StringUtils.hasText(node)) {
                lastNode = node;
            }
            if (!StringUtils.hasText(itemName) || isNoiseItemName(itemName)) {
                continue;
            }
            String standard = readCellText(sheet, row, header.standardCol, formatter);
            if (!StringUtils.hasText(standard) && header.itemCol >= 0) {
                standard = readCellText(sheet, row, header.itemCol + 1, formatter);
            }
            String resultText = readCellText(sheet, row, header.resultCol, formatter);
            String remark = readCellText(sheet, row, header.remarkCol, formatter);
            if (looksLikeSectionOnly(row, formatter, itemName, standard, resultText)) {
                lastCategory = itemName;
                continue;
            }

            items.add(buildImportItem(items.size() + 1, lastCategory, lastNode, itemName, standard, resultText, remark));
            if (items.size() >= MAX_IMPORT_ITEM_COUNT) {
                respVO.setWarningCount(respVO.getWarningCount() + 1);
                respVO.getMessages().add(String.format("明细超过 %d 行，已截取前 %d 行", MAX_IMPORT_ITEM_COUNT, MAX_IMPORT_ITEM_COUNT));
                break;
            }
        }
        return items;
    }

    private ExcelHeader findDetailHeader(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 80); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || countDirectNonEmptyCells(row, formatter) == 0) {
                continue;
            }
            ExcelHeader header = new ExcelHeader();
            header.rowIndex = rowIndex;
            short lastCellNum = row.getLastCellNum();
            for (int col = Math.max(0, row.getFirstCellNum()); col < lastCellNum; col++) {
                String text = normalizeText(readCellText(sheet, row, col, formatter));
                classifyHeaderColumn(header, text, col);
            }
            if (header.score() >= 4 && header.itemCol >= 0) {
                return header;
            }
        }
        return null;
    }

    private void classifyHeaderColumn(ExcelHeader header, String text, int col) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String compact = text.replace(" ", "");
        if (containsAny(compact, "点检类别", "项目分类", "类别", "分类")) {
            header.categoryCol = col;
            return;
        }
        if (containsAny(compact, "确认节点", "步骤节点", "工序节点", "节点", "时机", "阶段")) {
            header.nodeCol = col;
            return;
        }
        if (containsAny(compact, "判定标准", "点检标准", "检验标准", "标准", "规格", "要求", "范围", "管控值")) {
            header.standardCol = col;
            return;
        }
        if (containsAny(compact, "点检结果", "检验结果", "检查结果", "确认结果", "结果", "判定")) {
            header.resultCol = col;
            return;
        }
        if (containsAny(compact, "备注", "说明", "异常")) {
            header.remarkCol = col;
            return;
        }
        if (containsAny(compact, "点检项目", "检验项目", "检查项目", "确认项目", "参数名称", "项目", "内容")) {
            header.itemCol = col;
        }
    }

    private List<HcStationFormItemSaveReqVO> parseFallbackItems(Sheet sheet, DataFormatter formatter,
                                                                HcStationFormImportRespVO respVO) {
        List<HcStationFormItemSaveReqVO> items = new ArrayList<>();
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || countDirectNonEmptyCells(row, formatter) < 2) {
                continue;
            }
            String rowText = joinDirectRowText(row, formatter);
            if (isFooterRow(rowText) || looksLikeFormTitle(rowText)) {
                continue;
            }
            List<String> cells = readDirectRowCells(row, formatter);
            if (cells.size() < 2) {
                continue;
            }
            String itemName = "";
            String standard = "";
            for (String cellText : cells) {
                if (!StringUtils.hasText(cellText) || isSerialText(cellText)) {
                    continue;
                }
                if (!StringUtils.hasText(itemName)) {
                    itemName = cellText;
                } else {
                    standard = cellText;
                    break;
                }
            }
            if (!StringUtils.hasText(itemName) || isNoiseItemName(itemName)) {
                continue;
            }
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            items.add(buildImportItem(items.size() + 1, "", "", itemName, standard, "", ""));
            if (items.size() >= MAX_IMPORT_ITEM_COUNT) {
                break;
            }
        }
        return items;
    }

    private HcStationFormItemSaveReqVO buildImportItem(Integer itemSeq, String category, String node,
                                                       String itemName, String standard, String resultText,
                                                       String remark) {
        HcStationFormItemSaveReqVO item = new HcStationFormItemSaveReqVO();
        item.setItemSeq(itemSeq);
        item.setItemCategory(limitText(normalizeText(category), 100));
        item.setStepNode(limitText(normalizeText(node), 100));
        item.setItemName(limitText(normalizeText(itemName), 255));
        item.setStandardText(limitText(normalizeText(standard), 1000));
        item.setValueMode(inferValueMode(resultText, standard, itemName));
        item.setDualLabel1("");
        item.setDualLabel2("");
        item.setDefaultResult("OK_NG".equals(item.getValueMode()) ? "OK" : "");
        item.setRequiredFlag(true);
        item.setRemark(limitText(normalizeText(remark), 500));
        return item;
    }

    private String inferValueMode(String resultText, String standard, String itemName) {
        String text = normalizeText(resultText + " " + standard + " " + itemName).toUpperCase(Locale.ROOT);
        if (containsAny(text, "OK", "NG", "PASS", "FAIL", "合格", "不合格", "正常", "异常", "是/否", "√", "×")) {
            return "OK_NG";
        }
        return "TEXT";
    }

    private String buildImportSchemaJson(Sheet sheet, DataFormatter formatter, String presetTemplate, String fileName) {
        List<String> headerFields = inferHeaderFields(sheet, formatter, presetTemplate);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("version", StringUtils.hasText(presetTemplate) ? normalizeText(presetTemplate) : "excel-import-v1");
        schema.put("presetTemplate", StringUtils.hasText(presetTemplate) ? normalizeText(presetTemplate) : "excel-import-v1");
        schema.put("headerLayout", "GRID_4");
        schema.put("headerFields", headerFields);
        schema.put("sourceExcel", fileName);
        schema.put("sourceSheet", sheet.getSheetName());
        schema.put("importMode", "station-form-excel");
        return JsonUtils.toJsonString(schema);
    }

    private List<String> inferHeaderFields(Sheet sheet, DataFormatter formatter, String presetTemplate) {
        if ("wet-startup-v1".equals(presetTemplate) || "wet-cleaning-v1".equals(presetTemplate)) {
            return List.of("machine", "recorder", "recorderTime", "confirmer", "confirmerTime");
        }
        if ("wet-process-v1".equals(presetTemplate)) {
            return List.of("materialCode", "modelCode", "batchNo", "productionDate", "startTime", "endTime",
                    "inWashTime", "outWashTime", "inSolidifyTime", "outSolidifyTime", "inOvenTime", "outOvenTime");
        }
        if ("wet-solid-semi-v1".equals(presetTemplate)) {
            return List.of("productionDate", "modelCode", "batchNo", "semiWidth", "poreDevelopment", "finalResult");
        }
        if ("wet-oven-semi-v1".equals(presetTemplate)) {
            return List.of("productionDate", "modelCode", "batchNo", "semiWidth", "finalResult");
        }

        String topText = collectTopText(sheet, formatter);
        Set<String> fields = new LinkedHashSet<>();
        if (containsAny(topText, "料号", "物料")) {
            fields.add("materialCode");
        }
        if (containsAny(topText, "型号", "规格")) {
            fields.add("modelCode");
        }
        if (containsAny(topText, "批号", "批次")) {
            fields.add("batchNo");
        }
        if (containsAny(topText, "机台", "设备")) {
            fields.add("machine");
        }
        if (containsAny(topText, "日期", "生产日期")) {
            fields.add("productionDate");
        }
        if (containsAny(topText, "记录人", "担当", "填表")) {
            fields.add("recorder");
            fields.add("recorderTime");
        }
        if (containsAny(topText, "确认人", "审核")) {
            fields.add("confirmer");
            fields.add("confirmerTime");
        }
        if (fields.isEmpty()) {
            fields.addAll(List.of("productionDate", "modelCode", "batchNo", "recorder", "recorderTime",
                    "confirmer", "confirmerTime"));
        }
        return new ArrayList<>(fields);
    }

    private String buildPresetHeaderDataJson(String schemaJson) {
        Map<String, Object> header = new LinkedHashMap<>();
        try {
            JsonNode schema = JsonUtils.parseTree(schemaJson);
            JsonNode fieldsNode = schema.path("headerFields");
            if (fieldsNode.isArray()) {
                fieldsNode.forEach(fieldNode -> {
                    if (fieldNode.isTextual() && StringUtils.hasText(fieldNode.asText())) {
                        header.put(fieldNode.asText(), "");
                    }
                });
            }
        } catch (Exception ignored) {
            // Excel 导入的表头默认值允许为空，解析失败时只是不生成预设头。
        }
        return header.isEmpty() ? null : JsonUtils.toJsonString(header);
    }

    private boolean isAdhesiveIntermediateColumnConfig(HcStationFormSaveReqVO form) {
        return "ADHESIVE".equals(form.getProcessCode())
                && form.getFormCode() != null && form.getFormCode().contains("ADHESIVE1_INTERMEDIATE_RECORD");
    }

    private void normalizeSaveForm(HcStationFormSaveReqVO form) {
        form.setFormCode(normalizeFormCode(form.getFormCode()));
        form.setFormName(normalizeText(form.getFormName()));
        form.setProcessCode(normalizeText(form.getProcessCode()));
        form.setProcessName(StringUtils.hasText(form.getProcessName())
                ? normalizeText(form.getProcessName())
                : resolveProcessName(form.getProcessCode()));
        form.setTriggerTimingCode(normalizeText(form.getTriggerTimingCode()));
        form.setTriggerTimingName(StringUtils.hasText(form.getTriggerTimingName())
                ? normalizeText(form.getTriggerTimingName())
                : resolveTriggerTimingName(form.getTriggerTimingCode()));
        form.setNeedConfirm(form.getNeedConfirm() == null || form.getNeedConfirm());
        form.setSortNo(form.getSortNo() == null ? 10 : form.getSortNo());
        form.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        if (!StringUtils.hasText(form.getSchemaJson())) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("version", "excel-import-v1");
            schema.put("presetTemplate", "excel-import-v1");
            schema.put("headerLayout", "GRID_4");
            schema.put("headerFields", List.of("productionDate", "modelCode", "batchNo", "recorder", "recorderTime"));
            form.setSchemaJson(JsonUtils.toJsonString(schema));
        }
        if (!StringUtils.hasText(form.getPresetHeaderDataJson())) {
            form.setPresetHeaderDataJson(buildPresetHeaderDataJson(form.getSchemaJson()));
        }
        if (isAdhesiveIntermediateColumnConfig(form)) {
            JsonNode schema = JsonUtils.parseTree(form.getSchemaJson());
            JsonNode labels = schema == null ? null : schema.get("thicknessLabels");
            if (labels == null || !labels.isArray() || labels.size() != 2
                    || !labels.get(0).isTextual() || !labels.get(1).isTextual()
                    || !StringUtils.hasText(labels.get(0).asText()) || !StringUtils.hasText(labels.get(1).asText())) {
                throw new cn.iocoder.yudao.framework.common.exception.ServiceException(
                        HCSTATIONFORM_IMPORT_INVALID.getCode(), "请填写完整的左右两个厚度列标题");
            }
        }
        validateMultiFieldScope(form);
        normalizeItems(form.getItems());
        if (form.getPresetItems() == null || form.getPresetItems().isEmpty()) {
            form.setPresetItems(form.getItems());
        } else {
            normalizeItems(form.getPresetItems());
        }
    }

    private void validateMultiFieldScope(HcStationFormSaveReqVO form) {
        List<HcStationFormItemSaveReqVO> all = new ArrayList<>();
        if (form.getItems() != null) all.addAll(form.getItems());
        if (form.getPresetItems() != null) all.addAll(form.getPresetItems());
        if (all.stream().noneMatch(item -> "MULTI_FIELDS".equals(item.getValueMode()))) return;
        boolean production = "FORMULA".equals(form.getProcessCode()) && (
                String.valueOf(form.getFormCode()).startsWith("FORMULA_PROCESS_CHECK")
                || String.valueOf(form.getFormName()).contains("配料生产点检表")
                || "production-check".equals(cn.hutool.json.JSONUtil.parseObj(
                    StringUtils.hasText(form.getSchemaJson()) ? form.getSchemaJson() : "{}").getStr("formulaCategory")));
        for (HcStationFormItemSaveReqVO item : all) {
            if ("MULTI_FIELDS".equals(item.getValueMode())) {
                if (!production) throw invalidParamException("多字段填写仅适用于配料生产点检表");
                item.setFieldDefinitionsJson(FormulaMultiFields.normalizeDefinitions(item.getFieldDefinitionsJson()));
            }
        }
    }

    private void normalizeItems(List<HcStationFormItemSaveReqVO> items) {
        if (items == null) {
            return;
        }
        int index = 1;
        for (HcStationFormItemSaveReqVO item : items) {
            item.setId(null);
            item.setItemSeq(item.getItemSeq() == null ? index : item.getItemSeq());
            item.setItemCategory(normalizeText(item.getItemCategory()));
            item.setStepNode(normalizeText(item.getStepNode()));
            item.setItemName(normalizeText(item.getItemName()));
            item.setStandardText(normalizeText(item.getStandardText()));
            item.setValueMode(StringUtils.hasText(item.getValueMode()) ? normalizeText(item.getValueMode()) : "TEXT");
            item.setDefaultResult(normalizeText(item.getDefaultResult()));
            item.setDualLabel1(normalizeText(item.getDualLabel1()));
            item.setDualLabel2(normalizeText(item.getDualLabel2()));
            item.setRemark(normalizeText(item.getRemark()));
            item.setRequiredFlag(item.getRequiredFlag() == null || item.getRequiredFlag());
            index++;
        }
    }

    private void markExistingForm(HcStationFormImportRespVO respVO) {
        HcStationFormSaveReqVO form = respVO.getForm();
        if (form == null || !StringUtils.hasText(form.getFormCode())) {
            return;
        }
        HcStationFormDO existing = hcStationFormMapper.selectByFormCode(form.getFormCode());
        if (existing == null) {
            return;
        }
        respVO.setExisting(true);
        respVO.setExistingId(existing.getId());
        form.setId(existing.getId());
        respVO.setWarningCount(respVO.getWarningCount() + 1);
        respVO.getMessages().add(String.format("表单编码 %s 已存在，可勾选覆盖更新", form.getFormCode()));
    }

    private void addImportFailure(HcStationFormImportRespVO respVO, String message) {
        respVO.setStatus("ERROR");
        respVO.setFailureCount(respVO.getFailureCount() + 1);
        respVO.getFailures().add(message);
        respVO.getMessages().add(message);
    }

    private String resolveProcessName(String processCode) {
        return PROCESS_NAME_MAP.getOrDefault(processCode, processCode);
    }

    private static HcStationFormProcessOptionRespVO processOption(String value, String label) {
        HcStationFormProcessOptionRespVO option = new HcStationFormProcessOptionRespVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private static Map<String, String> buildProcessNameMap() {
        Map<String, String> map = new LinkedHashMap<>();
        REPORT_PROCESS_OPTIONS.forEach(option -> map.put(option.getValue(), option.getLabel()));
        map.put("ADHESIVE1", "粘胶1");
        map.put("TAPE", "粘胶1");
        map.put("FINE_GRINDING", "精磨");
        map.put("BACK_GLUE", "粘胶2");
        return map;
    }

    private String resolveTriggerTimingName(String triggerTimingCode) {
        return TRIGGER_TIMING_NAME_MAP.getOrDefault(triggerTimingCode, triggerTimingCode);
    }

    private String readCellText(Sheet sheet, Row row, int columnIndex, DataFormatter formatter) {
        if (row == null || columnIndex < 0) {
            return "";
        }
        String direct = readDirectCell(row, columnIndex, formatter);
        if (StringUtils.hasText(direct)) {
            return normalizeText(direct);
        }
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            if (!range.isInRange(row.getRowNum(), columnIndex)) {
                continue;
            }
            Row firstRow = sheet.getRow(range.getFirstRow());
            return normalizeText(readDirectCell(firstRow, range.getFirstColumn(), formatter));
        }
        return "";
    }

    private String readDirectCell(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null || columnIndex < 0) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private int countDirectNonEmptyCells(Row row, DataFormatter formatter) {
        if (row == null || row.getLastCellNum() < 0) {
            return 0;
        }
        int count = 0;
        for (int col = Math.max(0, row.getFirstCellNum()); col < row.getLastCellNum(); col++) {
            if (StringUtils.hasText(normalizeText(readDirectCell(row, col, formatter)))) {
                count++;
            }
        }
        return count;
    }

    private List<String> readDirectRowCells(Row row, DataFormatter formatter) {
        List<String> cells = new ArrayList<>();
        if (row == null || row.getLastCellNum() < 0) {
            return cells;
        }
        for (int col = Math.max(0, row.getFirstCellNum()); col < row.getLastCellNum(); col++) {
            String text = normalizeText(readDirectCell(row, col, formatter));
            if (StringUtils.hasText(text)) {
                cells.add(text);
            }
        }
        return cells;
    }

    private String joinDirectRowText(Row row, DataFormatter formatter) {
        return String.join(" ", readDirectRowCells(row, formatter));
    }

    private String collectTopText(Sheet sheet, DataFormatter formatter) {
        StringBuilder builder = new StringBuilder();
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 12); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                builder.append(' ').append(joinDirectRowText(row, formatter));
            }
        }
        return normalizeText(builder.toString());
    }

    private String findFallbackItemName(Sheet sheet, Row row, DataFormatter formatter, ExcelHeader header) {
        if (row == null || row.getLastCellNum() < 0) {
            return "";
        }
        for (int col = Math.max(0, row.getFirstCellNum()); col < row.getLastCellNum(); col++) {
            if (col == header.categoryCol || col == header.nodeCol || col == header.standardCol
                    || col == header.resultCol || col == header.remarkCol) {
                continue;
            }
            String text = readCellText(sheet, row, col, formatter);
            if (StringUtils.hasText(text) && !isSerialText(text)) {
                return text;
            }
        }
        return "";
    }

    private boolean looksLikeSectionOnly(Row row, DataFormatter formatter, String itemName, String standard,
                                         String resultText) {
        return countDirectNonEmptyCells(row, formatter) <= 2
                && StringUtils.hasText(itemName)
                && !StringUtils.hasText(standard)
                && !StringUtils.hasText(resultText)
                && itemName.length() <= 40
                && containsAny(itemName, "确认", "点检", "检查", "清洁", "开机", "参数", "外观", "设备");
    }

    private boolean isFooterRow(String rowText) {
        String compact = normalizeText(rowText).replace(" ", "");
        return containsAny(compact, "改定履历", "修订履历", "修订记录", "制定部门", "制订部门", "本资料")
                || (compact.contains("编制") && compact.contains("审核") && compact.contains("批准"));
    }

    private boolean isNoiseItemName(String itemName) {
        String compact = normalizeText(itemName).replace(" ", "");
        return !StringUtils.hasText(compact)
                || isSerialText(compact)
                || containsAny(compact, "点检项目", "检验项目", "检查项目", "确认项目", "参数名称");
    }

    private boolean isSerialText(String text) {
        String compact = normalizeText(text);
        return compact.matches("\\d+")
                || compact.matches("[A-Za-z]")
                || compact.matches("[一二三四五六七八九十]+")
                || containsAny(compact, "序号", "No.", "NO.");
    }

    private String buildImportFormCode(String processCode, String formName, String fileName, String sheetName) {
        String prefix = normalizeFormCode(processCode);
        if (!StringUtils.hasText(prefix)) {
            prefix = "FORM";
        }
        long hash = Integer.toUnsignedLong((normalizeText(fileName) + "|" + normalizeText(sheetName) + "|" + normalizeText(formName)).hashCode());
        return limitText(prefix + "_EXCEL_" + Long.toHexString(hash).toUpperCase(Locale.ROOT), 64);
    }

    private String normalizeFormCode(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limitText(String value, int maxLength) {
        String normalized = normalizeText(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void validatePressSlotProductionScope(HcStationFormSaveReqVO request) {
        JsonNode schema = parseSchemaNode(request.getSchemaJson());
        if (!"PRESS_SLOT".equals(request.getProcessCode()) || schema == null
                || !"PRODUCTION_CHECK".equalsIgnoreCase(schemaText(schema, "formType", "pressSlotFormType"))
                || !Integer.valueOf(1).equals(request.getStatus()) || isSchemaBooleanTrue(schema, "devOnly")) return;
        String scope = schemaText(schema, "modelScope").toUpperCase(java.util.Locale.ROOT);
        String key = "MODEL".equals(scope) ? "modelCode" : "modelPrefix";
        String value = schemaText(schema, key).trim();
        if (!List.of("MODEL", "PREFIX", "COMMON").contains(scope)
                || (!"COMMON".equals(scope) && value.isEmpty())) {
            throw invalidParamException("请配置有效的指定型号、型号前缀或通用适用范围");
        }
        if (!isSchemaBooleanTrue(schema, "published")) return;
        boolean duplicate = hcStationFormMapper.selectEnabledByProcess("PRESS_SLOT").stream()
                .filter(form -> !java.util.Objects.equals(form.getId(), request.getId()))
                .anyMatch(form -> {
                    JsonNode other = parseSchemaNode(form.getSchemaJson());
                    return other != null && isSchemaBooleanTrue(other, "published") && !isSchemaBooleanTrue(other, "devOnly")
                            && "PRODUCTION_CHECK".equalsIgnoreCase(schemaText(other, "formType", "pressSlotFormType"))
                            && scope.equalsIgnoreCase(schemaText(other, "modelScope"))
                            && ("COMMON".equals(scope) || value.equalsIgnoreCase(schemaText(other, key)));
                });
        if (duplicate) throw invalidParamException("该适用范围已有启用的正式生产点检模板，请勿重复发布");
    }

    private void replaceFormItems(Long formId, List<HcStationFormItemSaveReqVO> items) {
        HcStationFormDO form = hcStationFormMapper.selectById(formId);
        JsonNode schema = parseSchemaNode(form == null ? null : form.getSchemaJson());
        if (form != null && "PRESS_SLOT".equals(form.getProcessCode()) && schema != null
                && "PRODUCTION_CHECK".equalsIgnoreCase(schemaText(schema, "formType", "pressSlotFormType"))) {
            List<HcStationFormItemDO> existing = hcStationFormItemMapper.selectByFormId(formId);
            java.util.Set<Long> retained = new java.util.HashSet<>();
            for (HcStationFormItemSaveReqVO item : items == null ? List.<HcStationFormItemSaveReqVO>of() : items) {
                HcStationFormItemDO entity = BeanUtils.toBean(item, HcStationFormItemDO.class);
                entity.setFormId(formId);
                boolean owned = item.getId() != null && existing.stream().anyMatch(row -> item.getId().equals(row.getId()));
                if (owned) {
                    if (!retained.add(item.getId())) throw invalidParamException("生产点检模板明细 ID 重复");
                    hcStationFormItemMapper.updateById(entity);
                } else {
                    entity.setId(null);
                    hcStationFormItemMapper.insert(entity);
                }
            }
            existing.stream().filter(row -> !retained.contains(row.getId())).forEach(row -> hcStationFormItemMapper.deleteById(row.getId()));
            return;
        }
        hcStationFormItemMapper.deleteByFormId(formId);
        if (items == null || items.isEmpty()) {
            return;
        }
        List<HcStationFormItemDO> createList = new ArrayList<>();
        for (HcStationFormItemSaveReqVO item : items) {
            HcStationFormItemDO itemDO = BeanUtils.toBean(item, HcStationFormItemDO.class);
            itemDO.setId(null);
            itemDO.setFormId(formId);
            createList.add(itemDO);
        }
        hcStationFormItemMapper.insertBatch(createList);
    }

    private String toPresetItemsJson(List<HcStationFormItemSaveReqVO> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return JsonUtils.toJsonString(items);
    }

    private HcStationFormSaveReqVO parsePreviewFormJson(String formJson, HcStationFormImportRespVO respVO) {
        if (!StringUtils.hasText(formJson)) {
            return new HcStationFormSaveReqVO();
        }
        try {
            HcStationFormSaveReqVO form = JsonUtils.parseObject(formJson, HcStationFormSaveReqVO.class);
            return form == null ? new HcStationFormSaveReqVO() : form;
        } catch (Exception ex) {
            addImportFailure(respVO, "当前表单草稿解析失败：" + ex.getMessage());
            return new HcStationFormSaveReqVO();
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> parsed = JsonUtils.parseObjectQuietly(json, new TypeReference<Map<String, Object>>() {});
        return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
    }

    private List<String> resolveHeaderFields(String schemaJson, Map<String, Object> headerData) {
        List<String> fields = new ArrayList<>();
        if (StringUtils.hasText(schemaJson)) {
            try {
                JsonNode headerFields = JsonUtils.parseTree(schemaJson).path("headerFields");
                if (headerFields.isArray()) {
                    headerFields.forEach(fieldNode -> {
                        if (fieldNode.isTextual() && StringUtils.hasText(fieldNode.asText())) {
                            fields.add(fieldNode.asText());
                        }
                    });
                }
            } catch (Exception ignored) {
                // 预览导出允许旧配置 schema 不完整，失败时退回 presetHeaderDataJson 的 key。
            }
        }
        if (fields.isEmpty() && headerData != null) {
            fields.addAll(headerData.keySet());
        }
        return fields;
    }

    private String resolveHeaderFieldLabel(String field) {
        return switch (field) {
            case "batchNo" -> "产品批号";
            case "confirmer" -> "确认人";
            case "confirmerTime" -> "确认时间";
            case "endTime" -> "投料结束时间";
            case "finalResult" -> "综合判定";
            case "generatedLength" -> "半成品长度/m";
            case "inOvenTime" -> "入烘箱时间";
            case "inSolidifyTime" -> "入凝固槽时间";
            case "inWashTime" -> "入水洗槽时间";
            case "machine" -> "机台编号";
            case "materialCode" -> "母料料号";
            case "modelCode" -> "母料型号";
            case "outOvenTime" -> "出烘箱时间";
            case "outSolidifyTime" -> "出凝固槽时间";
            case "outWashTime" -> "出水洗槽时间";
            case "poreDevelopment" -> "泡孔发育";
            case "productionDate" -> "生产日期";
            case "recorder" -> "记录人";
            case "recorderTime" -> "记录时间";
            case "semiWidth" -> "宽幅";
            case "startTime" -> "投料开始时间";
            default -> field;
        };
    }

    private String resolveValueModeLabel(String valueMode) {
        return switch (normalizeText(valueMode).toUpperCase(Locale.ROOT)) {
            case "OK_NG" -> "OK/NG";
            case "DUAL_LABEL", "DUAL_TEXT" -> "双标签";
            case "MULTI_FIELDS" -> "多字段填写";
            case "READONLY" -> "只读";
            case "TIME" -> "时间";
            case "NUMBER" -> "数值";
            default -> StringUtils.hasText(valueMode) ? valueMode : "文本";
        };
    }

    private String normalizeValueModeFromExcel(String valueMode) {
        String text = normalizeText(valueMode).toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(text) || "文本".equals(valueMode)) {
            return "TEXT";
        }
        if ("MULTI_FIELDS".equals(text) || "多字段填写".equals(valueMode)) return "MULTI_FIELDS";
        if (text.contains("OK") || text.contains("NG")) {
            return "OK_NG";
        }
        if (text.contains("DUAL") || valueMode.contains("双")) {
            return "DUAL_LABEL";
        }
        if (text.contains("READ") || valueMode.contains("只读")) {
            return "READONLY";
        }
        if (text.contains("TIME") || valueMode.contains("时间")) {
            return "TIME";
        }
        if (text.contains("NUMBER") || valueMode.contains("数值")) {
            return "NUMBER";
        }
        return normalizeText(valueMode);
    }

    private int findRowByFirstCell(Sheet sheet, DataFormatter formatter, String text) {
        if (sheet == null) {
            return -1;
        }
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (text.equals(normalizeText(readDirectCell(row, 0, formatter)))) {
                return rowIndex;
            }
        }
        return -1;
    }

    private void readPreviewHeaderRows(Sheet sheet, DataFormatter formatter, int startRow, int endRow,
                                       Map<String, Object> headerData) {
        for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            String key = normalizeText(readDirectCell(row, 0, formatter));
            if (!StringUtils.hasText(key) || "字段Key".equals(key)) {
                continue;
            }
            headerData.put(key, readDirectCell(row, 2, formatter));
        }
    }

    private List<HcStationFormItemSaveReqVO> readPreviewItemRows(Sheet sheet, DataFormatter formatter,
                                                                 int startRow, HcStationFormImportRespVO respVO) {
        List<HcStationFormItemSaveReqVO> items = new ArrayList<>();
        int blankRows = 0;
        for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || countDirectNonEmptyCells(row, formatter) == 0) {
                blankRows++;
                if (blankRows > 5 && !items.isEmpty()) {
                    break;
                }
                continue;
            }
            blankRows = 0;
            String seqText = normalizeText(readDirectCell(row, 0, formatter));
            String itemName = normalizeText(readDirectCell(row, 3, formatter));
            String standardText = normalizeText(readDirectCell(row, 4, formatter));
            if ("序号".equals(seqText) || (!StringUtils.hasText(itemName) && !StringUtils.hasText(standardText))) {
                continue;
            }
            HcStationFormItemSaveReqVO item = new HcStationFormItemSaveReqVO();
            item.setItemSeq(parseInteger(seqText, items.size() + 1));
            item.setItemCategory(limitText(readDirectCell(row, 1, formatter), 100));
            item.setStepNode(limitText(readDirectCell(row, 2, formatter), 100));
            item.setItemName(limitText(itemName, 255));
            item.setStandardText(limitText(standardText, 1000));
            item.setValueMode(normalizeValueModeFromExcel(readDirectCell(row, 5, formatter)));
            item.setDualLabel1(limitText(readDirectCell(row, 6, formatter), 100));
            item.setDualLabel2(limitText(readDirectCell(row, 7, formatter), 100));
            item.setDefaultResult(limitText(readDirectCell(row, 8, formatter), 100));
            item.setRequiredFlag(parseRequiredFlag(readDirectCell(row, 9, formatter)));
            item.setRemark(limitText(readDirectCell(row, 10, formatter), 500));
            item.setFieldDefinitionsJson(readDirectCell(row, 11, formatter));
            items.add(item);
            respVO.setTotalRows(respVO.getTotalRows() + 1);
        }
        return items;
    }

    private Integer parseInteger(String value, Integer fallback) {
        try {
            return Integer.valueOf(normalizeText(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Boolean parseRequiredFlag(String value) {
        String text = normalizeText(value);
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String upperText = text.toUpperCase(Locale.ROOT);
        return !("否".equals(text) || "FALSE".equals(upperText) || "NO".equals(upperText)
                || "N".equals(upperText) || "0".equals(upperText));
    }

    private StationFormExcelStyles buildStationFormExcelStyles(Workbook workbook) {
        StationFormExcelStyles styles = new StationFormExcelStyles();
        styles.titleStyle = createStationFormStyle(workbook, true, HorizontalAlignment.CENTER, IndexedColors.WHITE.getIndex());
        styles.panelStyle = createStationFormStyle(workbook, true, HorizontalAlignment.LEFT, IndexedColors.GREY_25_PERCENT.getIndex());
        styles.tableHeaderStyle = createStationFormStyle(workbook, true, HorizontalAlignment.CENTER, IndexedColors.PALE_BLUE.getIndex());
        styles.headerLabelStyle = createStationFormStyle(workbook, true, HorizontalAlignment.CENTER, IndexedColors.GREY_25_PERCENT.getIndex());
        styles.bodyStyle = createStationFormStyle(workbook, false, HorizontalAlignment.LEFT, IndexedColors.WHITE.getIndex());
        styles.editableStyle = createStationFormStyle(workbook, false, HorizontalAlignment.LEFT, IndexedColors.LEMON_CHIFFON.getIndex());
        return styles;
    }

    private CellStyle createStationFormStyle(Workbook workbook, boolean bold, HorizontalAlignment alignment, short fillColor) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(fillColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(bold);
        style.setFont(font);
        return style;
    }

    private void writeCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void merge(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        if (lastRow < firstRow || lastCol <= firstCol) {
            return;
        }
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeSheetName(String value) {
        String normalized = firstNotBlank(value, "表单")
                .replaceAll("[\\\\/:*?\\[\\]]", "_");
        return normalized.length() > 31 ? normalized.substring(0, 31) : normalized;
    }

    private static class StationFormExcelStyles {

        private CellStyle titleStyle;
        private CellStyle panelStyle;
        private CellStyle tableHeaderStyle;
        private CellStyle headerLabelStyle;
        private CellStyle bodyStyle;
        private CellStyle editableStyle;
    }

    private static class ExcelHeader {

        private int rowIndex;
        private int categoryCol = -1;
        private int nodeCol = -1;
        private int itemCol = -1;
        private int standardCol = -1;
        private int resultCol = -1;
        private int remarkCol = -1;

        private int score() {
            int score = itemCol >= 0 ? 3 : 0;
            score += standardCol >= 0 ? 2 : 0;
            score += categoryCol >= 0 ? 1 : 0;
            score += nodeCol >= 0 ? 1 : 0;
            score += resultCol >= 0 ? 1 : 0;
            score += remarkCol >= 0 ? 1 : 0;
            return score;
        }
    }
}
