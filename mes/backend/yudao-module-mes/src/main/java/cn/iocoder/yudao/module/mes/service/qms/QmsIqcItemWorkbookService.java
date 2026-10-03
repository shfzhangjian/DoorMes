package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * IQC 检验项动态工作簿导入导出支撑。
 */
@Service
class QmsIqcItemWorkbookService {

    private static final String IMPORT_STATUS_SUCCESS = "SUCCESS";
    private static final String IMPORT_STATUS_FAILED = "FAILED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String ITEM_TYPE_QUALITATIVE = "QUALITATIVE";
    private static final String ITEM_TYPE_DATE = "DATE";
    private static final String TEMPLATE_SINGLE_VALUE = "SINGLE_VALUE";
    private static final String TEMPLATE_DENSITY_CALC = "DENSITY_CALC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final String FIELD_KIND_QUALITATIVE = "QUALITATIVE";
    private static final String FIELD_KIND_DATE = "DATE";
    private static final String FIELD_KIND_SINGLE = "SINGLE";
    private static final String FIELD_KIND_DENSITY = "DENSITY";
    private static final String FIELD_KIND_COMPRESSION = "COMPRESSION";
    private static final String FIELD_KIND_CUSTOM = "CUSTOM";
    private static final int DETAIL_HEADER_ROW_INDEX = 4;
    private static final String WORKBOOK_TITLE = "IQC 检验项明细导入模板";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId BUSINESS_ZONE_ID = ZoneId.of("Asia/Shanghai");

    byte[] buildWorkbook(QmsIqcOrderDO order, List<QmsIqcItemDO> items,
                         Map<String, QmsIqcSampleDO> sampleMap) throws IOException {
        DynamicWorkbookLayout layout = buildDynamicWorkbookLayout(items);
        String templateHash = buildTemplateHash(order, items);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("检验项明细");
            CellStyle headerStyle = buildStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.GREY_25_PERCENT.getIndex());
            CellStyle titleStyle = buildStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            CellStyle infoLabelStyle = buildStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.LEMON_CHIFFON.getIndex());
            CellStyle normalStyle = buildStyle(workbook, false, HorizontalAlignment.LEFT, null);
            buildDocumentInfoRows(sheet, order, layout.columns.size(), titleStyle, infoLabelStyle, normalStyle);

            Row headerRow = sheet.createRow(DETAIL_HEADER_ROW_INDEX);
            for (int columnIndex = 0; columnIndex < layout.columns.size(); columnIndex++) {
                DynamicColumn column = layout.columns.get(columnIndex);
                setCell(headerRow, columnIndex, column.header, headerStyle);
                sheet.setColumnWidth(columnIndex, columnWidth(column.header));
                if (column.hidden) {
                    sheet.setColumnHidden(columnIndex, true);
                }
            }

            int rowIndex = DETAIL_HEADER_ROW_INDEX + 1;
            for (QmsIqcItemDO item : items) {
                for (ItemPosition position : resolveItemPositions(item)) {
                    QmsIqcSampleDO sample = sampleMap.get(sampleKey(item.getId(), position.sampleSeq));
                    Row row = sheet.createRow(rowIndex++);
                    for (int columnIndex = 0; columnIndex < layout.columns.size(); columnIndex++) {
                        Object value = resolveCellValue(order, item, position, sample, templateHash,
                                layout.columns.get(columnIndex));
                        setCell(row, columnIndex, value, normalStyle);
                    }
                }
            }
            sheet.createFreezePane(0, DETAIL_HEADER_ROW_INDEX + 1);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    boolean supports(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return false;
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("检验项明细");
            if (sheet == null && workbook.getNumberOfSheets() > 0) {
                sheet = workbook.getSheetAt(0);
            }
            return findHeaderRow(sheet, new DataFormatter()) >= 0;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ignored) {
            return false;
        }
    }

    ItemImportPlan buildImportPlan(QmsIqcOrderDO order, List<QmsIqcItemDO> items,
                                   Map<String, QmsIqcSampleDO> existingSampleMap,
                                   MultipartFile file, boolean allowOverwrite,
                                   boolean previewOnly) throws IOException {
        Map<Long, QmsIqcItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsIqcItemDO::getId, item -> item,
                        (first, ignored) -> first, LinkedHashMap::new));
        DynamicWorkbookLayout layout = buildDynamicWorkbookLayout(items);
        ItemImportPlan plan = new ItemImportPlan(file == null ? null : file.getOriginalFilename(), previewOnly,
                allowOverwrite, buildTemplateHash(order, items));
        if (file == null || file.isEmpty()) {
            plan.addFailure("请选择需要导入的 .xlsx 或 .xls 文件");
            return plan;
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("检验项明细");
            if (sheet == null && workbook.getNumberOfSheets() > 0) {
                sheet = workbook.getSheetAt(0);
            }
            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findHeaderRow(sheet, formatter);
            if (headerRowIndex < 0) {
                plan.addFailure("Excel 表头不匹配，请使用当前 IQC 单下载模板导入");
                return plan;
            }
            if (!validateLayoutHeaders(sheet.getRow(headerRowIndex), formatter, layout)) {
                plan.addFailure("Excel 动态字段与当前检验模板不匹配，请重新下载当前 IQC 单模板");
                return plan;
            }
            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter, layout.columns.size())) {
                    continue;
                }
                readImportRow(order, itemMap, existingSampleMap, row, rowIndex + 1,
                        formatter, layout, plan);
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            plan.addFailure("Excel 解析失败，请确认文件来自当前 IQC 单下载模板");
        }
        if (plan.totalCount == 0) {
            plan.addFailure("未识别到可导入的 IQC 检验项样本行");
        }
        plan.successCount = plan.validRows.size();
        return plan;
    }

    private void readImportRow(QmsIqcOrderDO order, Map<Long, QmsIqcItemDO> itemMap,
                               Map<String, QmsIqcSampleDO> existingSampleMap,
                               Row row, int excelRowNo, DataFormatter formatter,
                               DynamicWorkbookLayout layout, ItemImportPlan plan) {
        plan.totalCount++;
        List<String> errors = new ArrayList<>();
        Long orderId = parseLong(readCell(row, formatter, layout.indexOfSystem("ORDER_ID")));
        String orderNo = readCell(row, formatter, layout.indexOfSystem("ORDER_NO"));
        String templateHash = readCell(row, formatter, layout.indexOfSystem("TEMPLATE_HASH"));
        Long itemId = parseLong(readCell(row, formatter, layout.indexOfSystem("ITEM_ID")));
        Integer sampleSeq = parseInteger(readCell(row, formatter, layout.indexOfSystem("SAMPLE_SEQ")));
        String itemType = readCell(row, formatter, layout.indexOfSystem("ITEM_TYPE_RAW"));
        String valueTemplate = readCell(row, formatter, layout.indexOfSystem("VALUE_TEMPLATE_RAW"));
        String positionName = readCell(row, formatter, layout.indexOfSystem("POSITION_NAME"));
        String positionCode = readCell(row, formatter, layout.indexOfSystem("POSITION_CODE"));
        Integer repeatSeq = parseInteger(readCell(row, formatter, layout.indexOfSystem("REPEAT_SEQ")));
        String remark = readCell(row, formatter, layout.indexOfSystem("REMARK"));

        if (!Objects.equals(order.getId(), orderId)) {
            errors.add("IQC ID 与当前单据不匹配");
        }
        if (!Objects.equals(order.getIqcNo(), orderNo)) {
            errors.add("IQC 单号与当前单据不匹配");
        }
        if (!Objects.equals(plan.templateHash, templateHash)) {
            errors.add("模板校验码不匹配，请重新下载当前单据模板");
        }
        QmsIqcItemDO item = itemId == null ? null : itemMap.get(itemId);
        if (item == null) {
            errors.add("检验项不存在或不属于当前单据");
        } else {
            if (StringUtils.hasText(itemType) && !Objects.equals(item.getItemType(), itemType)) {
                errors.add("项目类型与当前快照不匹配");
            }
            if (StringUtils.hasText(valueTemplate)
                    && !Objects.equals(resolveValueTemplate(item), valueTemplate)) {
                errors.add("录入模板与当前快照不匹配");
            }
            if (!isValidSamplePosition(item, sampleSeq, positionCode, repeatSeq)) {
                errors.add("样本序号、位置编码或组次不属于当前检验项模板");
            }
        }

        ImportedValue importedValue = item == null ? new ImportedValue()
                : readImportedValue(item, row, formatter, layout, errors);
        if (!importedValue.hasBusinessValue && !StringUtils.hasText(remark)) {
            plan.warningCount++;
            plan.messages.add("第 " + excelRowNo + " 行未填写业务值，已跳过");
        }
        QmsIqcSampleDO existing = item == null || sampleSeq == null ? null
                : existingSampleMap.get(sampleKey(item.getId(), sampleSeq));
        if (hasExistingValue(existing) && (importedValue.hasBusinessValue || StringUtils.hasText(remark))) {
            if (plan.previewOnly || plan.allowOverwrite) {
                plan.warningCount++;
                plan.messages.add("第 " + excelRowNo + " 行将覆盖已有样本值");
            } else {
                errors.add("存在已有样本值，需确认覆盖后再导入");
            }
        }
        if (!errors.isEmpty()) {
            plan.failureCount++;
            plan.messages.add("第 " + excelRowNo + " 行：" + String.join("；", errors));
            return;
        }
        if (!importedValue.hasBusinessValue && !StringUtils.hasText(remark)) {
            return;
        }

        QmsIqcSaveReqVO.IqcSample sample = existing == null ? new QmsIqcSaveReqVO.IqcSample()
                : BeanUtils.toBean(existing, QmsIqcSaveReqVO.IqcSample.class);
        sample.setSampleSeq(sampleSeq);
        sample.setRemark(remark);
        if (importedValue.hasBusinessValue) {
            applyImportedValue(sample, item, importedValue, positionName, positionCode, repeatSeq);
        }
        plan.validRows.add(new ItemImportRow(item.getId(), sampleSeq, sample));
        plan.affectedItemIds.add(item.getId());
    }

    private ImportedValue readImportedValue(QmsIqcItemDO item, Row row, DataFormatter formatter,
                                             DynamicWorkbookLayout layout, List<String> errors) {
        ImportedValue result = new ImportedValue();
        List<DynamicFieldDefinition> fields = resolveDynamicFieldDefinitions(item);
        for (DynamicFieldDefinition field : fields) {
            String text = readCell(row, formatter, layout.indexOfDynamic(field.columnKey));
            if (!StringUtils.hasText(text)) {
                if (field.required) {
                    result.missingRequiredFields.add(field.header);
                }
                continue;
            }
            result.hasBusinessValue = true;
            if (FIELD_KIND_QUALITATIVE.equals(field.kind)) {
                String judgment = normalizeJudgment(text);
                if (!JUDGMENT_OK.equals(judgment) && !JUDGMENT_NG.equals(judgment)) {
                    errors.add("定性判定只能填写 OK 或 NG");
                } else {
                    result.qualitativeValue = judgment;
                }
                continue;
            }
            if (FIELD_KIND_DATE.equals(field.kind)) {
                result.dateValue = parseLocalDate(text);
                if (result.dateValue == null) {
                    errors.add("日期必须为 yyyy-MM-dd");
                } else if (result.dateValue.isAfter(LocalDate.now(BUSINESS_ZONE_ID))) {
                    errors.add("时间检验项日期不能晚于当前日期");
                }
                continue;
            }
            BigDecimal numericValue = parseDecimal(text);
            if (numericValue == null) {
                errors.add("“" + field.header + "”必须为数值");
                continue;
            }
            result.rawValues.put(field.code, numericValue);
            if (FIELD_KIND_SINGLE.equals(field.kind)) {
                result.measuredValue = numericValue;
            }
        }
        if (result.hasBusinessValue && !result.missingRequiredFields.isEmpty()) {
            errors.add("必填字段未完整填写：" + String.join("、", result.missingRequiredFields));
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType())
                && result.hasBusinessValue && (item.getExpiryDays() == null || item.getExpiryDays() < 0)) {
            errors.add("时间检验项缺少过期天数快照");
        }
        return result;
    }

    private void applyImportedValue(QmsIqcSaveReqVO.IqcSample sample, QmsIqcItemDO item,
                                    ImportedValue value, String positionName,
                                    String positionCode, Integer repeatSeq) {
        sample.setMeasuredValue(null);
        sample.setResultValue(null);
        sample.setQualitativeValue(null);
        sample.setDateValue(null);
        sample.setSampleResult(JUDGMENT_PENDING);
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            sample.setQualitativeValue(value.qualitativeValue);
            sample.setSampleResult(value.qualitativeValue);
            sample.setRawValuesJson(null);
            return;
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            sample.setDateValue(value.dateValue);
            sample.setRawValuesJson(null);
            return;
        }
        Map<String, Object> rawValues = new LinkedHashMap<>(value.rawValues);
        putIfNotBlank(rawValues, "samplePosition", positionName);
        putIfNotBlank(rawValues, "samplePositionCode", positionCode);
        if (repeatSeq != null) {
            rawValues.put("sampleGroupNo", repeatSeq);
        }
        sample.setRawValuesJson(rawValues.isEmpty() ? null : JsonUtils.toJsonString(rawValues));
        sample.setMeasuredValue(value.measuredValue);
        sample.setResultValue(value.measuredValue);
    }

    private Object resolveCellValue(QmsIqcOrderDO order, QmsIqcItemDO item, ItemPosition position,
                                    QmsIqcSampleDO sample, String templateHash, DynamicColumn column) {
        if (column.dynamicField != null) {
            return dynamicFieldValue(item, sample, column.dynamicField);
        }
        return switch (column.systemKey) {
            case "ORDER_NO" -> order.getIqcNo();
            case "INSPECTION_ITEM" -> item.getInspectionItem();
            case "SAMPLE_SEQ" -> position.sampleSeq;
            case "POSITION_NAME" -> position.name;
            case "FILLING_INSTRUCTION" -> buildFillingInstruction(item);
            case "AVG_LIMIT" -> QmsIqcQuantitativeJudgment.range(item.getAvgMinLimit(), item.getAvgMaxLimit());
            case "ITEM_RESULT" -> item.getItemResult();
            case "JUDGMENT_REASON" -> item.getJudgmentReason();
            case "SAMPLE_RESULT" -> sample == null ? "" : sample.getSampleResult();
            case "REMARK" -> sample == null ? "" : sample.getRemark();
            case "ORDER_ID" -> order.getId();
            case "TEMPLATE_HASH" -> templateHash;
            case "ITEM_ID" -> item.getId();
            case "ITEM_TYPE_RAW" -> item.getItemType();
            case "VALUE_TEMPLATE_RAW" -> resolveValueTemplate(item);
            case "POSITION_CODE" -> position.code;
            case "REPEAT_SEQ" -> position.repeatSeq;
            default -> "";
        };
    }

    private Object dynamicFieldValue(QmsIqcItemDO item, QmsIqcSampleDO sample,
                                     DynamicFieldDefinition field) {
        if (sample == null) {
            return "";
        }
        if (FIELD_KIND_QUALITATIVE.equals(field.kind)) {
            return sample.getQualitativeValue();
        }
        if (FIELD_KIND_DATE.equals(field.kind)) {
            return sample.getDateValue() == null ? "" : DATE_FORMATTER.format(sample.getDateValue());
        }
        Map<String, Object> rawValues = QmsFaiRuleCalculationSupport.parseRawValues(sample.getRawValuesJson());
        if (FIELD_KIND_SINGLE.equals(field.kind)) {
            return sample.getMeasuredValue() == null
                    ? rawValues.getOrDefault("value", sample.getResultValue()) : sample.getMeasuredValue();
        }
        return rawValues.get(field.code);
    }

    private DynamicWorkbookLayout buildDynamicWorkbookLayout(List<QmsIqcItemDO> items) {
        List<DynamicColumn> columns = new ArrayList<>();
        columns.add(DynamicColumn.system("单据号", "ORDER_NO", false));
        columns.add(DynamicColumn.system("检验项目", "INSPECTION_ITEM", false));
        columns.add(DynamicColumn.system("样本序号", "SAMPLE_SEQ", false));
        columns.add(DynamicColumn.system("样本位置", "POSITION_NAME", false));
        columns.add(DynamicColumn.system("填写说明", "FILLING_INSTRUCTION", false));
        Map<String, DynamicFieldDefinition> dynamicFieldMap = new LinkedHashMap<>();
        for (QmsIqcItemDO item : items) {
            for (DynamicFieldDefinition field : resolveDynamicFieldDefinitions(item)) {
                dynamicFieldMap.putIfAbsent(field.columnKey, field);
            }
        }
        dynamicFieldMap.values().forEach(field -> columns.add(DynamicColumn.dynamic(field)));
        columns.add(DynamicColumn.system("平均值内控", "AVG_LIMIT", false));
        columns.add(DynamicColumn.system("项目判定", "ITEM_RESULT", false));
        columns.add(DynamicColumn.system("判定原因", "JUDGMENT_REASON", false));
        columns.add(DynamicColumn.system("样本判定", "SAMPLE_RESULT", false));
        columns.add(DynamicColumn.system("备注", "REMARK", false));
        columns.add(DynamicColumn.system("单据 ID", "ORDER_ID", true));
        columns.add(DynamicColumn.system("模板校验码", "TEMPLATE_HASH", true));
        columns.add(DynamicColumn.system("检验项 ID", "ITEM_ID", true));
        columns.add(DynamicColumn.system("项目类型原始值", "ITEM_TYPE_RAW", true));
        columns.add(DynamicColumn.system("录入模板原始值", "VALUE_TEMPLATE_RAW", true));
        columns.add(DynamicColumn.system("位置编码", "POSITION_CODE", true));
        columns.add(DynamicColumn.system("组次", "REPEAT_SEQ", true));
        return new DynamicWorkbookLayout(columns);
    }

    private List<DynamicFieldDefinition> resolveDynamicFieldDefinitions(QmsIqcItemDO item) {
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return List.of(new DynamicFieldDefinition("QUALITATIVE:qualitativeValue", "qualitativeValue",
                    "定性判定", FIELD_KIND_QUALITATIVE, true));
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            return List.of(new DynamicFieldDefinition("DATE:dateValue", "dateValue",
                    "日期", FIELD_KIND_DATE, true));
        }
        Map<String, Object> params = QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams());
        List<DynamicFieldDefinition> customFields = resolveCustomInputFields(params);
        if (!customFields.isEmpty()) {
            return customFields;
        }
        String template = resolveValueTemplate(item);
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            return List.of(
                    new DynamicFieldDefinition("DENSITY:thicknessMm", "thicknessMm",
                            "厚度", FIELD_KIND_DENSITY, true),
                    new DynamicFieldDefinition("DENSITY:weightG", "weightG",
                            "重量", FIELD_KIND_DENSITY, true));
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            return List.of(
                    new DynamicFieldDefinition("COMPRESSION:t1Mm", "t1Mm", "T1",
                            FIELD_KIND_COMPRESSION, true),
                    new DynamicFieldDefinition("COMPRESSION:t2Mm", "t2Mm", "T2",
                            FIELD_KIND_COMPRESSION, true),
                    new DynamicFieldDefinition("COMPRESSION:t3Mm", "t3Mm", "T3",
                            FIELD_KIND_COMPRESSION, true));
        }
        return List.of(new DynamicFieldDefinition("SINGLE:value", "value",
                "实测值", FIELD_KIND_SINGLE, true));
    }

    private List<DynamicFieldDefinition> resolveCustomInputFields(Map<String, Object> params) {
        Map<String, Object> dataRule = toStringKeyMap(params.get("dataRule"));
        if (dataRule.isEmpty() && (params.containsKey("inputFields") || params.containsKey("resultFields"))) {
            dataRule = params;
        }
        Object inputFields = dataRule.get("inputFields");
        if (!(inputFields instanceof Collection<?> collection) || collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<DynamicFieldDefinition> fields = new ArrayList<>();
        for (Object value : collection) {
            Map<String, Object> field = toStringKeyMap(value);
            String code = firstText(field, "code", "fieldCode", "key", "name");
            if (!StringUtils.hasText(code)) {
                continue;
            }
            String label = defaultIfBlank(firstText(field, "name", "label", "title"), code);
            boolean required = !Boolean.FALSE.equals(field.get("required"));
            fields.add(new DynamicFieldDefinition("CUSTOM:" + code + ":" + label,
                    code, label, FIELD_KIND_CUSTOM, required));
        }
        return fields;
    }

    private List<ItemPosition> resolveItemPositions(QmsIqcItemDO item) {
        Map<String, Object> params = QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams());
        int expectedCount = resolveExpectedSampleCount(item, params);
        Integer repeatCount = QmsFaiRuleCalculationSupport.toPositiveInt(params.get("repeatCount"));
        int repeat = repeatCount == null ? 1 : repeatCount;
        List<ItemPosition> positions = new ArrayList<>();
        Object positionObject = params.get("positions");
        if (positionObject instanceof Collection<?> collection && !collection.isEmpty()) {
            int positionIndex = 0;
            for (Object value : collection) {
                positionIndex++;
                Map<String, Object> positionMap = toStringKeyMap(value);
                String code = defaultIfBlank(text(positionMap.get("code")), "P" + positionIndex);
                String name = defaultIfBlank(text(positionMap.get("name")), code);
                for (int repeatSeq = 1; repeatSeq <= repeat; repeatSeq++) {
                    positions.add(new ItemPosition(code, name, repeatSeq, positions.size() + 1));
                }
            }
        }
        while (positions.size() < expectedCount) {
            int next = positions.size() + 1;
            String code = ITEM_TYPE_QUALITATIVE.equals(item.getItemType()) && expectedCount == 1
                    ? "QUALITATIVE" : "P" + next;
            String name = ITEM_TYPE_QUALITATIVE.equals(item.getItemType()) && expectedCount == 1
                    ? "判定" : String.valueOf(next);
            positions.add(new ItemPosition(code, name, 1, next));
        }
        return positions.size() > expectedCount ? positions.subList(0, expectedCount) : positions;
    }

    private int resolveExpectedSampleCount(QmsIqcItemDO item, Map<String, Object> params) {
        Integer configuredSampleSize = QmsFaiRuleCalculationSupport.toPositiveInt(params.get("sampleSize"));
        if (configuredSampleSize != null) {
            return configuredSampleSize;
        }
        Object positions = params.get("positions");
        if (positions instanceof Collection<?> collection && !collection.isEmpty()) {
            Integer repeatCount = QmsFaiRuleCalculationSupport.toPositiveInt(params.get("repeatCount"));
            return collection.size() * (repeatCount == null ? 1 : repeatCount);
        }
        return item.getSampleSize() == null || item.getSampleSize() < 1 ? 1 : item.getSampleSize();
    }

    private boolean isValidSamplePosition(QmsIqcItemDO item, Integer sampleSeq,
                                          String positionCode, Integer repeatSeq) {
        if (sampleSeq == null) {
            return false;
        }
        for (ItemPosition position : resolveItemPositions(item)) {
            if (Objects.equals(position.sampleSeq, sampleSeq)
                    && (!StringUtils.hasText(positionCode) || Objects.equals(position.code, positionCode))
                    && (repeatSeq == null || Objects.equals(position.repeatSeq, repeatSeq))) {
                return true;
            }
        }
        return false;
    }

    private String buildFillingInstruction(QmsIqcItemDO item) {
        List<DynamicFieldDefinition> fields = resolveDynamicFieldDefinitions(item);
        String fieldNames = fields.stream().map(field -> field.header).collect(Collectors.joining("、"));
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return "填写 OK 或 NG";
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            return "填写 yyyy-MM-dd 日期";
        }
        if (fields.stream().anyMatch(field -> FIELD_KIND_CUSTOM.equals(field.kind))) {
            return "按字段填写：" + fieldNames + "；最终判定以后端重算为准";
        }
        if (TEMPLATE_DENSITY_CALC.equals(resolveValueTemplate(item))) {
            return "填写厚度、重量，系统计算密度";
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(resolveValueTemplate(item))) {
            return "填写 T1、T2、T3，系统计算压缩指标";
        }
        return "填写 " + defaultIfBlank(fieldNames, "实测值");
    }

    private String buildTemplateHash(QmsIqcOrderDO order, List<QmsIqcItemDO> items) {
        String payload = order.getId() + "|" + order.getIqcNo() + "|" + items.stream()
                .map(item -> item.getId() + ":" + item.getInspectionItem() + ":" + item.getItemType()
                        + ":" + resolveValueTemplate(item) + ":" + item.getTemplateParams()
                        + ":" + resolveExpectedSampleCount(item,
                        QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams()))
                        + ":" + resolveDynamicFieldDefinitions(item).stream()
                        .map(field -> field.columnKey + "=" + field.header)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("|"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return String.valueOf(payload.hashCode());
        }
    }

    private String resolveValueTemplate(QmsIqcItemDO item) {
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return ITEM_TYPE_QUALITATIVE;
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            return ITEM_TYPE_DATE;
        }
        return defaultIfBlank(item.getValueTemplate(), TEMPLATE_SINGLE_VALUE);
    }

    private boolean hasExistingValue(QmsIqcSampleDO sample) {
        return sample != null && (sample.getMeasuredValue() != null
                || sample.getResultValue() != null
                || sample.getDateValue() != null
                || StringUtils.hasText(sample.getRawValuesJson())
                || StringUtils.hasText(sample.getQualitativeValue())
                || StringUtils.hasText(sample.getRemark()));
    }

    private boolean validateLayoutHeaders(Row header, DataFormatter formatter, DynamicWorkbookLayout layout) {
        if (header == null) {
            return false;
        }
        for (int index = 0; index < layout.columns.size(); index++) {
            if (!Objects.equals(layout.columns.get(index).header, readCell(header, formatter, index))) {
                return false;
            }
        }
        return true;
    }

    private int findHeaderRow(Sheet sheet, DataFormatter formatter) {
        if (sheet == null) {
            return -1;
        }
        int maxRow = Math.min(sheet.getLastRowNum(), 12);
        for (int rowIndex = 0; rowIndex <= maxRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Set<String> headers = new java.util.HashSet<>();
            for (Cell cell : row) {
                headers.add(formatter.formatCellValue(cell).trim());
            }
            if (headers.contains("单据号") && headers.contains("检验项目")
                    && headers.contains("单据 ID") && headers.contains("模板校验码")
                    && headers.contains("检验项 ID")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < columnCount; index++) {
            if (StringUtils.hasText(readCell(row, formatter, index))) {
                return false;
            }
        }
        return true;
    }

    private void buildDocumentInfoRows(Sheet sheet, QmsIqcOrderDO order, int columnCount,
                                       CellStyle titleStyle, CellStyle labelStyle, CellStyle valueStyle) {
        int lastColumn = Math.max(columnCount - 1, 7);
        setMergedCell(sheet, 0, 0, lastColumn + 1, WORKBOOK_TITLE, titleStyle);
        setInfoPair(sheet, 1, 0, "IQC单号", order.getIqcNo(), labelStyle, valueStyle);
        setInfoPair(sheet, 1, 2, "收料单号", order.getReceiptNo(), labelStyle, valueStyle);
        setInfoPair(sheet, 1, 4, "物料编码", order.getMaterialCode(), labelStyle, valueStyle);
        setInfoPair(sheet, 1, 6, "物料名称", order.getMaterialName(), labelStyle, valueStyle);
        setInfoPair(sheet, 2, 0, "供应商", order.getSupplierName(), labelStyle, valueStyle);
        setInfoPair(sheet, 2, 2, "来料批次", order.getBatchNo(), labelStyle, valueStyle);
        setInfoPair(sheet, 2, 4, "检验标准", joinNotBlank(" / ", order.getStandardNo(), order.getStandardVersion()),
                labelStyle, valueStyle);
        setInfoPair(sheet, 2, 6, "模板说明", "请勿删除隐藏列；填写后直接导入当前 IQC 单", labelStyle, valueStyle);
    }

    private void setMergedCell(Sheet sheet, int rowIndex, int firstColumn, int columnSpan,
                               String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        for (int columnIndex = firstColumn; columnIndex < firstColumn + columnSpan; columnIndex++) {
            setCell(row, columnIndex, "", style);
        }
        row.getCell(firstColumn).setCellValue(value);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                rowIndex, rowIndex, firstColumn, firstColumn + columnSpan - 1));
    }

    private void setInfoPair(Sheet sheet, int rowIndex, int columnIndex, String label, String value,
                             CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        setCell(row, columnIndex, label, labelStyle);
        setCell(row, columnIndex + 1, value, valueStyle);
    }

    private CellStyle buildStyle(Workbook workbook, boolean bold, HorizontalAlignment alignment,
                                 Short fillColor) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(bold);
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (fillColor != null) {
            style.setFillForegroundColor(fillColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private void setCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.stripTrailingZeros().toPlainString());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private int columnWidth(String header) {
        int width = Math.max(header.length() + 4, 12);
        if ("模板校验码".equals(header) || "填写说明".equals(header)) {
            width = 68;
        } else if ("检验项目".equals(header) || "备注".equals(header)) {
            width = 24;
        }
        return Math.min(width, 80) * 256;
    }

    private String readCell(Row row, DataFormatter formatter, int columnIndex) {
        if (row == null || columnIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        String value = cell == null ? null : formatter.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long parseLong(String text) {
        BigDecimal value = parseDecimal(text);
        return value == null ? null : value.longValue();
    }

    private Integer parseInteger(String text) {
        BigDecimal value = parseDecimal(text);
        return value == null ? null : value.intValue();
    }

    private BigDecimal parseDecimal(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DATE_FORMATTER,
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"));
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(text.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种兼容格式。
            }
        }
        return null;
    }

    private String normalizeJudgment(String value) {
        String text = value == null ? "" : value.trim().toUpperCase();
        if (List.of("OK", "PASS", "Y", "YES", "合格", "通过").contains(text)) {
            return JUDGMENT_OK;
        }
        if (List.of("NG", "FAIL", "N", "NO", "不合格", "不通过").contains(text)) {
            return JUDGMENT_NG;
        }
        return text;
    }

    private Map<String, Object> toStringKeyMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private String firstText(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            String value = text(values.get(key));
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void putIfNotBlank(Map<String, Object> values, String key, String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value);
        }
    }

    private String joinNotBlank(String separator, String... values) {
        return java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(separator));
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    static class ItemImportPlan {
        final String fileName;
        final boolean previewOnly;
        final boolean allowOverwrite;
        final String templateHash;
        int totalCount;
        int successCount;
        int failureCount;
        int warningCount;
        final List<String> messages = new ArrayList<>();
        final List<ItemImportRow> validRows = new ArrayList<>();
        final Set<Long> affectedItemIds = new java.util.LinkedHashSet<>();

        private ItemImportPlan(String fileName, boolean previewOnly, boolean allowOverwrite, String templateHash) {
            this.fileName = fileName;
            this.previewOnly = previewOnly;
            this.allowOverwrite = allowOverwrite;
            this.templateHash = templateHash;
        }

        private void addFailure(String message) {
            failureCount++;
            messages.add(message);
        }

        QmsIqcImportRespVO toResp(QmsIqcRespVO record) {
            QmsIqcImportRespVO resp = new QmsIqcImportRespVO();
            resp.setFileName(fileName);
            resp.setPreviewOnly(previewOnly);
            resp.setTotalCount(totalCount);
            resp.setSuccessCount(successCount);
            resp.setFailureCount(failureCount);
            resp.setWarningCount(warningCount);
            resp.setStatus(failureCount > 0 || validRows.isEmpty()
                    ? IMPORT_STATUS_FAILED : IMPORT_STATUS_SUCCESS);
            resp.setValidateSummary("成功 " + successCount + " 行，失败 " + failureCount
                    + " 行，警告 " + warningCount + " 行");
            resp.setMessages(messages);
            resp.setRecord(record);
            return resp;
        }
    }

    static class ItemImportRow {
        final Long itemId;
        final Integer sampleSeq;
        final QmsIqcSaveReqVO.IqcSample sample;

        private ItemImportRow(Long itemId, Integer sampleSeq, QmsIqcSaveReqVO.IqcSample sample) {
            this.itemId = itemId;
            this.sampleSeq = sampleSeq;
            this.sample = sample;
        }
    }

    private static class ImportedValue {
        private boolean hasBusinessValue;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        private LocalDate dateValue;
        private final Map<String, Object> rawValues = new LinkedHashMap<>();
        private final List<String> missingRequiredFields = new ArrayList<>();
    }

    private static class DynamicWorkbookLayout {
        private final List<DynamicColumn> columns;

        private DynamicWorkbookLayout(List<DynamicColumn> columns) {
            this.columns = columns;
        }

        private int indexOfSystem(String systemKey) {
            for (int index = 0; index < columns.size(); index++) {
                if (Objects.equals(systemKey, columns.get(index).systemKey)) {
                    return index;
                }
            }
            return -1;
        }

        private int indexOfDynamic(String columnKey) {
            for (int index = 0; index < columns.size(); index++) {
                DynamicFieldDefinition field = columns.get(index).dynamicField;
                if (field != null && Objects.equals(columnKey, field.columnKey)) {
                    return index;
                }
            }
            return -1;
        }
    }

    private static class DynamicColumn {
        private final String header;
        private final String systemKey;
        private final DynamicFieldDefinition dynamicField;
        private final boolean hidden;

        private DynamicColumn(String header, String systemKey,
                              DynamicFieldDefinition dynamicField, boolean hidden) {
            this.header = header;
            this.systemKey = systemKey;
            this.dynamicField = dynamicField;
            this.hidden = hidden;
        }

        private static DynamicColumn system(String header, String systemKey, boolean hidden) {
            return new DynamicColumn(header, systemKey, null, hidden);
        }

        private static DynamicColumn dynamic(DynamicFieldDefinition field) {
            return new DynamicColumn(field.header, null, field, false);
        }
    }

    private static class DynamicFieldDefinition {
        private final String columnKey;
        private final String code;
        private final String header;
        private final String kind;
        private final boolean required;

        private DynamicFieldDefinition(String columnKey, String code, String header,
                                       String kind, boolean required) {
            this.columnKey = columnKey;
            this.code = code;
            this.header = header;
            this.kind = kind;
            this.required = required;
        }
    }

    private static class ItemPosition {
        private final String code;
        private final String name;
        private final Integer repeatSeq;
        private final Integer sampleSeq;

        private ItemPosition(String code, String name, Integer repeatSeq, Integer sampleSeq) {
            this.code = code;
            this.name = name;
            this.repeatSeq = repeatSeq;
            this.sampleSeq = sampleSeq;
        }
    }
}
