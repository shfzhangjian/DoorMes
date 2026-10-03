package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * FQC 检验项明细工作簿导入导出支撑。
 */
@Service
class QmsFqcItemWorkbookService {

    private static final String IMPORT_STATUS_SUCCESS = "SUCCESS";
    private static final String IMPORT_STATUS_FAILED = "FAILED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String ROLE_QA = "QA";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_QUALITATIVE = "QUALITATIVE";
    private static final String TEMPLATE_SINGLE_VALUE = "SINGLE_VALUE";
    private static final String TEMPLATE_DENSITY_CALC = "DENSITY_CALC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final String VALUE_SOURCE_ITEM_IMPORT = "ITEM_IMPORT";
    private static final String FIELD_KIND_QUALITATIVE = "QUALITATIVE";
    private static final String FIELD_KIND_SINGLE = "SINGLE";
    private static final String FIELD_KIND_DENSITY = "DENSITY";
    private static final String FIELD_KIND_COMPRESSION = "COMPRESSION";
    private static final String FIELD_KIND_CUSTOM = "CUSTOM";
    private static final BigDecimal DEFAULT_SAMPLE_DIAMETER_MM = BigDecimal.valueOf(39);

    List<QmsFqcItemImportExcelVO> buildItemTemplateRows(QmsFqcOrderDO order, List<QmsFqcItemDO> items,
                                                        String templateVersionHash,
                                                        Map<String, QmsFqcSampleDO> sampleMap) {
        List<QmsFqcItemImportExcelVO> rows = new ArrayList<>();
        for (QmsFqcItemDO item : items) {
            List<ItemImportPosition> positions = resolveItemImportPositions(item);
            for (ItemImportPosition position : positions) {
                QmsFqcItemImportExcelVO row = new QmsFqcItemImportExcelVO();
                row.setFqcId(order.getId());
                row.setFqcNo(order.getFqcNo());
                row.setTemplateVersionHash(templateVersionHash);
                row.setFqcItemId(item.getId());
                row.setSampleSeq(position.sampleSeq);
                row.setInspectionItemCode(resolveInspectionItemCode(item));
                row.setInspectionItem(item.getInspectionItem());
                row.setItemType(item.getItemType());
                row.setValueTemplate(resolveValueTemplate(item.getItemType(), item.getValueTemplate()));
                row.setPositionCode(position.code);
                row.setPositionName(position.name);
                row.setRepeatSeq(position.repeatSeq);
                row.setTargetValue(item.getTargetValue());
                row.setAvgLimitText(formatLimitText(item.getAvgMinLimit(), item.getAvgMaxLimit()));
                row.setStdLimitText(formatLimitText(item.getStdMinLimit(), item.getStdMaxLimit()));
                row.setCalculatedAvg(item.getCalculatedAvg());
                row.setCalculatedStd(item.getCalculatedStd());
                fillItemImportSample(row, sampleMap.get(sampleKey(item.getId(), position.sampleSeq)));
                rows.add(row);
            }
        }
        return rows;
    }

    byte[] buildItemOverviewWorkbook(QmsFqcOrderDO order, List<QmsFqcItemDO> items,
                                     Map<String, QmsFqcSampleDO> sampleMap) throws IOException {
        String templateVersionHash = buildItemTemplateVersionHash(order, items);
        List<QmsFqcItemImportExcelVO> rows = buildItemTemplateRows(order, items, templateVersionHash, sampleMap);
        return buildItemDetailWorkbook(order, items, rows, sampleMap);
    }

    private byte[] buildItemDetailWorkbook(QmsFqcOrderDO order, List<QmsFqcItemDO> items,
                                           List<QmsFqcItemImportExcelVO> rows,
                                           Map<String, QmsFqcSampleDO> sampleMap) throws IOException {
        DynamicWorkbookLayout layout = buildDynamicWorkbookLayout(items);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("检验项明细");
            CellStyle headerStyle = buildOverviewStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.GREY_25_PERCENT.getIndex());
            CellStyle normalStyle = buildOverviewStyle(workbook, false, HorizontalAlignment.LEFT, null);
            Row headerRow = sheet.createRow(0);
            for (int colIndex = 0; colIndex < layout.columns.size(); colIndex++) {
                DynamicColumn column = layout.columns.get(colIndex);
                setDetailCell(headerRow, colIndex, column.header, headerStyle);
                sheet.setColumnWidth(colIndex, itemDetailColumnWidth(column.header));
                if (column.hidden) {
                    sheet.setColumnHidden(colIndex, true);
                }
            }
            Map<Long, QmsFqcItemDO> itemMap = items.stream()
                    .collect(Collectors.toMap(QmsFqcItemDO::getId, Function.identity(), (first, ignored) -> first,
                            LinkedHashMap::new));
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                QmsFqcItemImportExcelVO row = rows.get(rowIndex);
                QmsFqcItemDO item = itemMap.get(row.getFqcItemId());
                Row dataRow = sheet.createRow(rowIndex + 1);
                for (int colIndex = 0; colIndex < layout.columns.size(); colIndex++) {
                    setDetailCell(dataRow, colIndex,
                            itemDetailValue(order, item, row, layout.columns.get(colIndex), sampleMap), normalStyle);
                }
            }
            sheet.createFreezePane(0, 1);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle buildOverviewStyle(Workbook workbook, boolean bold, HorizontalAlignment alignment, Short fillColor) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (fillColor != null) {
            style.setFillForegroundColor(fillColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        Font font = workbook.createFont();
        font.setBold(bold);
        style.setFont(font);
        return style;
    }

    private void setDetailCell(Row row, int colIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.stripTrailingZeros().toPlainString());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private int itemDetailColumnWidth(String header) {
        int width = Math.max(header.length() + 4, 12);
        if ("模板校验码".equals(header) || "填写说明".equals(header)) {
            width = 68;
        } else if ("检验项目".equals(header) || "备注".equals(header)) {
            width = 24;
        }
        return Math.min(width, 80) * 256;
    }

    private DynamicWorkbookLayout buildDynamicWorkbookLayout(List<QmsFqcItemDO> items) {
        List<DynamicColumn> columns = new ArrayList<>();
        columns.add(DynamicColumn.system("单据号", "ORDER_NO", false));
        columns.add(DynamicColumn.system("检验项目", "INSPECTION_ITEM", false));
        columns.add(DynamicColumn.system("样本序号", "SAMPLE_SEQ", false));
        columns.add(DynamicColumn.system("样本位置", "POSITION_NAME", false));
        columns.add(DynamicColumn.system("填写说明", "FILLING_INSTRUCTION", false));

        Map<String, DynamicFieldDefinition> dynamicFieldMap = new LinkedHashMap<>();
        Map<Long, List<DynamicFieldDefinition>> itemFieldMap = new LinkedHashMap<>();
        for (QmsFqcItemDO item : items) {
            List<DynamicFieldDefinition> itemFields = resolveDynamicFieldDefinitions(item);
            itemFieldMap.put(item.getId(), itemFields);
            for (DynamicFieldDefinition field : itemFields) {
                dynamicFieldMap.putIfAbsent(field.columnKey, field);
            }
        }
        dynamicFieldMap.values().forEach(field -> columns.add(DynamicColumn.dynamic(field)));

        columns.add(DynamicColumn.system("样本判定", "SAMPLE_RESULT", false));
        columns.add(DynamicColumn.system("备注", "REMARK", false));
        columns.add(DynamicColumn.system("单据 ID", "ORDER_ID", true));
        columns.add(DynamicColumn.system("模板校验码", "TEMPLATE_HASH", true));
        columns.add(DynamicColumn.system("检验项 ID", "ITEM_ID", true));
        columns.add(DynamicColumn.system("检验项编码", "ITEM_CODE", true));
        columns.add(DynamicColumn.system("项目类型原始值", "ITEM_TYPE_RAW", true));
        columns.add(DynamicColumn.system("录入模板原始值", "VALUE_TEMPLATE_RAW", true));
        columns.add(DynamicColumn.system("位置编码", "POSITION_CODE", true));
        columns.add(DynamicColumn.system("组次", "REPEAT_SEQ", true));
        columns.add(DynamicColumn.system("值来源", "VALUE_SOURCE", true));
        columns.add(DynamicColumn.system("导入批次", "IMPORT_BATCH", true));
        return new DynamicWorkbookLayout(columns, itemFieldMap);
    }

    private Object itemDetailValue(QmsFqcOrderDO order, QmsFqcItemDO item, QmsFqcItemImportExcelVO row,
                                   DynamicColumn column, Map<String, QmsFqcSampleDO> sampleMap) {
        if (column.dynamicField != null) {
            return dynamicFieldValue(item, row, column.dynamicField, sampleMap);
        }
        return switch (column.systemKey) {
            case "ORDER_NO" -> row.getFqcNo();
            case "INSPECTION_ITEM" -> row.getInspectionItem();
            case "SAMPLE_SEQ" -> row.getSampleSeq();
            case "POSITION_NAME" -> row.getPositionName();
            case "FILLING_INSTRUCTION" -> buildFillingInstruction(item);
            case "SAMPLE_RESULT" -> row.getSampleResult();
            case "REMARK" -> row.getRemark();
            case "ORDER_ID" -> order.getId();
            case "TEMPLATE_HASH" -> row.getTemplateVersionHash();
            case "ITEM_ID" -> row.getFqcItemId();
            case "ITEM_CODE" -> row.getInspectionItemCode();
            case "ITEM_TYPE_RAW" -> row.getItemType();
            case "VALUE_TEMPLATE_RAW" -> row.getValueTemplate();
            case "POSITION_CODE" -> row.getPositionCode();
            case "REPEAT_SEQ" -> row.getRepeatSeq();
            case "VALUE_SOURCE" -> row.getValueSource();
            case "IMPORT_BATCH" -> row.getImportBatchNo();
            default -> "";
        };
    }

    private Object dynamicFieldValue(QmsFqcItemDO item, QmsFqcItemImportExcelVO row, DynamicFieldDefinition field,
                                     Map<String, QmsFqcSampleDO> sampleMap) {
        if (item == null) {
            return "";
        }
        QmsFqcSampleDO sample = sampleMap.get(sampleKey(item.getId(), row.getSampleSeq()));
        Map<String, Object> rawValues = sample == null ? Collections.emptyMap()
                : parseRawValues(sample.getRawValuesJson());
        if (FIELD_KIND_QUALITATIVE.equals(field.kind)) {
            return row.getQualitativeValue();
        }
        if (FIELD_KIND_SINGLE.equals(field.kind)) {
            return row.getMeasuredValue() == null ? rawValues.get("value") : row.getMeasuredValue();
        }
        if (FIELD_KIND_DENSITY.equals(field.kind) || FIELD_KIND_COMPRESSION.equals(field.kind)
                || FIELD_KIND_CUSTOM.equals(field.kind)) {
            return rawValues.get(field.code);
        }
        return "";
    }

    private String buildFillingInstruction(QmsFqcItemDO item) {
        if (item == null) {
            return "";
        }
        List<DynamicFieldDefinition> fields = resolveDynamicFieldDefinitions(item);
        String fieldNames = fields.stream().map(field -> field.header).collect(Collectors.joining("、"));
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return "填写 OK 或 NG";
        }
        String template = resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        String metricText = resolveJudgmentMetricText(item, template);
        if (fields.stream().anyMatch(field -> FIELD_KIND_CUSTOM.equals(field.kind))) {
            return "按字段填写：" + fieldNames + metricText;
        }
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            return "填写厚度、重量，系统计算密度" + metricText;
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            return "填写 T1、T2、T3，系统计算压缩指标" + metricText;
        }
        return "填写 " + defaultIfBlank(fieldNames, "实测值") + metricText;
    }

    private String resolveJudgmentMetricText(QmsFqcItemDO item, String valueTemplate) {
        String fallback = resolveJudgmentMetric(valueTemplate, item.getJudgmentMetric());
        String metric = QmsEntryRuleFormulaSupport.resolveJudgmentMetric(item.getTemplateParams(), fallback);
        if (!StringUtils.hasText(metric)) {
            return "";
        }
        String label = switch (metric) {
            case "RESULT_VALUE" -> "结果值";
            case "DENSITY_VALUE" -> "密度";
            case "COMPRESSION_RATE" -> "压缩率";
            case "COMPRESSION_ELASTICITY_RATE" -> "压缩弹性率";
            default -> metric;
        };
        return "；判定指标：" + label;
    }

    private List<DynamicFieldDefinition> resolveDynamicFieldDefinitions(QmsFqcItemDO item) {
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return List.of(new DynamicFieldDefinition("QUALITATIVE:qualitativeValue", "qualitativeValue",
                    "定性判定", FIELD_KIND_QUALITATIVE));
        }
        Map<String, Object> params = parseRawValues(item.getTemplateParams());
        List<DynamicFieldDefinition> customFields = resolveCustomInputFields(params);
        if (!customFields.isEmpty()) {
            return customFields;
        }
        String template = resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            return List.of(
                    new DynamicFieldDefinition("DENSITY:thicknessMm", "thicknessMm", "厚度", FIELD_KIND_DENSITY),
                    new DynamicFieldDefinition("DENSITY:weightG", "weightG", "重量", FIELD_KIND_DENSITY));
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            return List.of(
                    new DynamicFieldDefinition("COMPRESSION:t1Mm", "t1Mm", "T1", FIELD_KIND_COMPRESSION),
                    new DynamicFieldDefinition("COMPRESSION:t2Mm", "t2Mm", "T2", FIELD_KIND_COMPRESSION),
                    new DynamicFieldDefinition("COMPRESSION:t3Mm", "t3Mm", "T3", FIELD_KIND_COMPRESSION));
        }
        String label = resolveSingleValueLabel(item, params);
        return List.of(new DynamicFieldDefinition("SINGLE:value:" + label, "value", label, FIELD_KIND_SINGLE));
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
            String label = defaultIfBlank(firstText(field, "label", "name", "title"), code);
            fields.add(new DynamicFieldDefinition("CUSTOM:" + code + ":" + label, code, label, FIELD_KIND_CUSTOM));
        }
        return fields;
    }

    private String resolveSingleValueLabel(QmsFqcItemDO item, Map<String, Object> params) {
        String label = firstText(params, "label", "name", "title", "fieldLabel", "fieldName");
        if (!StringUtils.hasText(label)) {
            label = firstNestedFieldText(params, "fields", "inputFields");
        }
        if (StringUtils.hasText(label)) {
            return label;
        }
        return StringUtils.hasText(item.getInspectionItem()) ? item.getInspectionItem() + "实测值" : "实测值";
    }

    private String firstNestedFieldText(Map<String, Object> params, String... collectionKeys) {
        for (String key : collectionKeys) {
            Object value = params.get(key);
            if (value instanceof Collection<?> collection && !collection.isEmpty()) {
                for (Object item : collection) {
                    String label = firstText(toStringKeyMap(item), "label", "name", "title");
                    if (StringUtils.hasText(label)) {
                        return label;
                    }
                }
            }
        }
        return null;
    }

    private String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            String value = toText(map.get(key));
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    ItemImportPlan buildItemImportPlan(QmsFqcOrderDO order, List<QmsFqcItemDO> items,
                                       Map<String, QmsFqcSampleDO> existingSampleMap,
                                       MultipartFile file, boolean allowOverwrite,
                                       boolean previewOnly, boolean importBlocked) throws IOException {
        Map<Long, QmsFqcItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsFqcItemDO::getId, Function.identity(), (first, ignored) -> first,
                        LinkedHashMap::new));
        String templateVersionHash = buildItemTemplateVersionHash(order, items);
        ItemImportPlan plan = new ItemImportPlan(order, itemMap, templateVersionHash,
                file == null ? null : file.getOriginalFilename(), allowOverwrite, previewOnly);
        if (file == null || file.isEmpty()) {
            plan.addFailure("请选择需要导入的 .xlsx 文件");
            return plan;
        }
        if (importBlocked) {
            plan.addFailure("当前 FQC 单已结案、待审核或已锁定，不允许导入覆盖");
            return plan;
        }

        Map<Long, Map<String, Integer>> positionSeqMap = items.stream()
                .collect(Collectors.toMap(QmsFqcItemDO::getId, this::buildPositionSeqMap));
        List<QmsFqcItemImportExcelVO> rows = readDynamicItemImportRows(plan, items, file).stream()
                .filter(this::isRecognizedItemImportRow)
                .collect(Collectors.toList());
        plan.totalCount = rows.size();
        if (rows.isEmpty()) {
            plan.addFailure("未识别到可导入的 FQC 检验项样本行");
            return plan;
        }

        for (int i = 0; i < rows.size(); i++) {
            QmsFqcItemImportExcelVO row = rows.get(i);
            int excelRowNo = i + 2;
            normalizeItemImportRow(row);
            List<String> errors = validateItemImportRow(plan, row, positionSeqMap);
            QmsFqcItemDO item = row.getFqcItemId() == null ? null : itemMap.get(row.getFqcItemId());
            Integer sampleSeq = resolveImportSampleSeq(row, positionSeqMap.get(row.getFqcItemId()));
            boolean hasValue = hasItemImportValue(plan, row, item);
            if (!hasValue) {
                plan.warningCount++;
                plan.messages.add("第 " + excelRowNo + " 行未填写业务值，已跳过");
            }
            QmsFqcSampleDO existingSample = item == null || sampleSeq == null ? null
                    : existingSampleMap.get(sampleKey(item.getId(), sampleSeq));
            if (hasExistingSampleValue(existingSample)) {
                if (previewOnly || allowOverwrite) {
                    plan.warningCount++;
                    plan.messages.add("第 " + excelRowNo + " 行将覆盖已有样本值");
                } else {
                    errors.add("存在已有样本值，需确认覆盖后再导入");
                }
            }
            if (!errors.isEmpty()) {
                plan.failureCount++;
                plan.messages.add("第 " + excelRowNo + " 行：" + String.join("；", errors));
                continue;
            }
            if (hasValue) {
                plan.validRows.add(new ItemImportRow(row, item, sampleSeq, plan.rawValuesMap.get(row)));
            }
        }
        plan.successCount = plan.validRows.size();
        return plan;
    }

    QmsFqcSaveReqVO.FqcSample buildImportedSample(ItemImportRow row, String importBatchNo,
                                                  QmsFqcSampleDO existing) {
        QmsFqcSaveReqVO.FqcSample sample = existing == null ? new QmsFqcSaveReqVO.FqcSample()
                : BeanUtils.toBean(existing, QmsFqcSaveReqVO.FqcSample.class);
        QmsFqcItemDO item = row.item;
        sample.setSampleRole(ROLE_QA);
        sample.setSampleSeq(row.sampleSeq);
        sample.setStepCode(item.getStepCode());
        sample.setMetricCode(item.getMetricCode());
        sample.setMetricGroupCode(item.getMetricGroupCode());
        sample.setInputComponent(item.getInputComponent());
        sample.setSamplePosition(defaultIfBlank(row.row.getPositionName(), row.row.getPositionCode()));
        sample.setSampleGroupNo(row.row.getRepeatSeq());
        sample.setSheetSectionCode(item.getSheetSectionCode());
        sample.setSheetMetricCode(item.getSheetMetricCode());
        sample.setImportBatchNo(importBatchNo);
        sample.setValueSource(VALUE_SOURCE_ITEM_IMPORT);
        sample.setRemark(row.row.getRemark());
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            sample.setQualitativeValue(normalizeJudgment(row.row.getQualitativeValue()));
            sample.setSampleResult(defaultIfBlank(sample.getQualitativeValue(), JUDGMENT_PENDING));
            sample.setRawValuesJson(null);
            sample.setMeasuredValue(null);
            sample.setResultValue(null);
            sample.setDensityValue(null);
            sample.setCompressionRate(null);
            sample.setCompressionElasticityRate(null);
            return sample;
        }
        sample.setRawValuesJson(JsonUtils.toJsonString(buildItemImportRawValues(item, row.row, row.rawValues)));
        sample.setMeasuredValue(row.row.getMeasuredValue());
        sample.setResultValue(row.row.getMeasuredValue());
        sample.setQualitativeValue(null);
        sample.setSampleResult(JUDGMENT_PENDING);
        return sample;
    }

    QmsFqcImportRespVO buildItemImportResp(ItemImportPlan plan, QmsFqcRespVO record) {
        QmsFqcImportRespVO respVO = new QmsFqcImportRespVO();
        respVO.setFileName(plan.fileName);
        respVO.setStatus(plan.failureCount > 0 || plan.validRows.isEmpty() ? IMPORT_STATUS_FAILED : IMPORT_STATUS_SUCCESS);
        respVO.setPreviewOnly(plan.previewOnly);
        respVO.setTotalCount(plan.totalCount);
        respVO.setSuccessCount(plan.successCount);
        respVO.setFailureCount(plan.failureCount);
        respVO.setWarningCount(plan.warningCount);
        respVO.setValidateSummary(JsonUtils.toJsonString(plan.messages));
        respVO.setMessages(plan.messages);
        respVO.setRecord(record);
        return respVO;
    }

    String buildItemTemplateVersionHash(QmsFqcOrderDO order, List<QmsFqcItemDO> items) {
        String payload = order.getId() + "|" + order.getFqcNo() + "|" + items.stream()
                .map(item -> item.getId() + ":" + resolveInspectionItemCode(item) + ":" + item.getInspectionItem()
                        + ":" + item.getItemType() + ":" + item.getValueTemplate() + ":" + item.getJudgmentMetric()
                        + ":" + item.getTemplateParams() + ":" + resolveExpectedSampleCount(item)
                        + ":" + resolveDynamicFieldDefinitions(item).stream()
                        .map(field -> field.columnKey + "=" + field.header)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("|"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return String.valueOf(payload.hashCode());
        }
    }

    private void fillItemImportSample(QmsFqcItemImportExcelVO row, QmsFqcSampleDO sample) {
        if (sample == null) {
            return;
        }
        Map<String, Object> rawValues = parseRawValues(sample.getRawValuesJson());
        row.setMeasuredValue(sample.getMeasuredValue() == null ? sample.getResultValue() : sample.getMeasuredValue());
        row.setThicknessMm(toDecimal(rawValues.get("thicknessMm")));
        row.setWeightG(toDecimal(rawValues.get("weightG")));
        row.setT1Mm(toDecimal(rawValues.get("t1Mm")));
        row.setT2Mm(toDecimal(rawValues.get("t2Mm")));
        row.setT3Mm(toDecimal(rawValues.get("t3Mm")));
        row.setQualitativeValue(sample.getQualitativeValue());
        row.setRemark(sample.getRemark());
        row.setSampleResult(sample.getSampleResult());
        row.setValueSource(sample.getValueSource());
        row.setImportBatchNo(sample.getImportBatchNo());
    }

    private List<QmsFqcItemImportExcelVO> readDynamicItemImportRows(ItemImportPlan plan, List<QmsFqcItemDO> items,
                                                                    MultipartFile file) throws IOException {
        DynamicWorkbookLayout layout = buildDynamicWorkbookLayout(items);
        Map<Long, QmsFqcItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsFqcItemDO::getId, Function.identity(), (first, ignored) -> first,
                        LinkedHashMap::new));
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("检验项明细");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            }
            if (sheet == null) {
                return Collections.emptyList();
            }
            DataFormatter formatter = new DataFormatter();
            if (!isDynamicItemSheet(sheet, formatter, layout)) {
                plan.addFailure("Excel 表头不匹配，请使用当前单据下载模板导入");
                return Collections.emptyList();
            }
            List<QmsFqcItemImportExcelVO> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (isBlankRow(dataRow, formatter, layout.columns.size())) {
                    continue;
                }
                QmsFqcItemImportExcelVO row = readDynamicItemRow(dataRow, formatter, layout, itemMap);
                rows.add(row);
                Map<String, Object> rawValues = readDynamicRawValues(dataRow, formatter, layout,
                        itemMap.get(row.getFqcItemId()), row);
                if (!rawValues.isEmpty()) {
                    plan.rawValuesMap.put(row, rawValues);
                }
            }
            return rows;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            plan.addFailure("Excel 解析失败，请确认文件来自当前 FQC 单下载模板");
            return Collections.emptyList();
        }
    }

    private boolean isDynamicItemSheet(Sheet sheet, DataFormatter formatter, DynamicWorkbookLayout layout) {
        Row header = sheet.getRow(0);
        if (header == null) {
            return false;
        }
        return Objects.equals("单据号", readCellString(header, formatter, layout.indexOfSystem("ORDER_NO")))
                && Objects.equals("检验项目", readCellString(header, formatter, layout.indexOfSystem("INSPECTION_ITEM")))
                && Objects.equals("单据 ID", readCellString(header, formatter, layout.indexOfSystem("ORDER_ID")))
                && Objects.equals("模板校验码", readCellString(header, formatter, layout.indexOfSystem("TEMPLATE_HASH")));
    }

    private QmsFqcItemImportExcelVO readDynamicItemRow(Row dataRow, DataFormatter formatter,
                                                       DynamicWorkbookLayout layout,
                                                       Map<Long, QmsFqcItemDO> itemMap) {
        QmsFqcItemImportExcelVO row = new QmsFqcItemImportExcelVO();
        row.setFqcNo(readCellString(dataRow, formatter, layout.indexOfSystem("ORDER_NO")));
        row.setInspectionItem(readCellString(dataRow, formatter, layout.indexOfSystem("INSPECTION_ITEM")));
        row.setSampleSeq(parseInteger(readCellString(dataRow, formatter, layout.indexOfSystem("SAMPLE_SEQ"))));
        row.setPositionName(readCellString(dataRow, formatter, layout.indexOfSystem("POSITION_NAME")));
        row.setSampleResult(readCellString(dataRow, formatter, layout.indexOfSystem("SAMPLE_RESULT")));
        row.setRemark(readCellString(dataRow, formatter, layout.indexOfSystem("REMARK")));
        row.setFqcId(parseLong(readCellString(dataRow, formatter, layout.indexOfSystem("ORDER_ID"))));
        row.setTemplateVersionHash(readCellString(dataRow, formatter, layout.indexOfSystem("TEMPLATE_HASH")));
        row.setFqcItemId(parseLong(readCellString(dataRow, formatter, layout.indexOfSystem("ITEM_ID"))));
        row.setInspectionItemCode(readCellString(dataRow, formatter, layout.indexOfSystem("ITEM_CODE")));
        row.setItemType(readCellString(dataRow, formatter, layout.indexOfSystem("ITEM_TYPE_RAW")));
        row.setValueTemplate(readCellString(dataRow, formatter, layout.indexOfSystem("VALUE_TEMPLATE_RAW")));
        row.setPositionCode(readCellString(dataRow, formatter, layout.indexOfSystem("POSITION_CODE")));
        row.setRepeatSeq(parseInteger(readCellString(dataRow, formatter, layout.indexOfSystem("REPEAT_SEQ"))));
        row.setValueSource(readCellString(dataRow, formatter, layout.indexOfSystem("VALUE_SOURCE")));
        row.setImportBatchNo(readCellString(dataRow, formatter, layout.indexOfSystem("IMPORT_BATCH")));
        QmsFqcItemDO item = itemMap.get(row.getFqcItemId());
        if (item != null) {
            row.setInspectionItem(defaultIfBlank(row.getInspectionItem(), item.getInspectionItem()));
            row.setItemType(defaultIfBlank(row.getItemType(), item.getItemType()));
            row.setValueTemplate(defaultIfBlank(row.getValueTemplate(),
                    resolveValueTemplate(item.getItemType(), item.getValueTemplate())));
        }
        return row;
    }

    private Map<String, Object> readDynamicRawValues(Row dataRow, DataFormatter formatter,
                                                     DynamicWorkbookLayout layout, QmsFqcItemDO item,
                                                     QmsFqcItemImportExcelVO row) {
        if (item == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> rawValues = new LinkedHashMap<>();
        for (DynamicFieldDefinition field : layout.itemFieldMap.getOrDefault(item.getId(), Collections.emptyList())) {
            int columnIndex = layout.indexOfDynamic(field.columnKey);
            if (FIELD_KIND_QUALITATIVE.equals(field.kind)) {
                row.setQualitativeValue(normalizeJudgment(readCellString(dataRow, formatter, columnIndex)));
                continue;
            }
            BigDecimal value = readCellDecimal(dataRow, formatter, columnIndex);
            if (value != null) {
                rawValues.put(field.code, value);
            }
            applyDynamicValueToRow(row, field, value);
        }
        return rawValues;
    }

    private void applyDynamicValueToRow(QmsFqcItemImportExcelVO row, DynamicFieldDefinition field, BigDecimal value) {
        if (FIELD_KIND_SINGLE.equals(field.kind)) {
            row.setMeasuredValue(value);
        } else if ("thicknessMm".equals(field.code)) {
            row.setThicknessMm(value);
        } else if ("weightG".equals(field.code)) {
            row.setWeightG(value);
        } else if ("t1Mm".equals(field.code)) {
            row.setT1Mm(value);
        } else if ("t2Mm".equals(field.code)) {
            row.setT2Mm(value);
        } else if ("t3Mm".equals(field.code)) {
            row.setT3Mm(value);
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < columnCount; i++) {
            if (StringUtils.hasText(readCellString(row, formatter, i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> validateItemImportRow(ItemImportPlan plan, QmsFqcItemImportExcelVO row,
                                               Map<Long, Map<String, Integer>> positionSeqMap) {
        List<String> errors = new ArrayList<>();
        if (!Objects.equals(plan.order.getId(), row.getFqcId())) {
            errors.add("FQC ID 与当前单据不匹配");
        }
        if (!Objects.equals(plan.order.getFqcNo(), row.getFqcNo())) {
            errors.add("FQC 单号与当前单据不匹配");
        }
        if (!Objects.equals(plan.templateVersionHash, row.getTemplateVersionHash())) {
            errors.add("模板校验码不匹配，请重新下载当前单据模板");
        }
        QmsFqcItemDO item = row.getFqcItemId() == null ? null : plan.itemMap.get(row.getFqcItemId());
        if (item == null) {
            errors.add("检验项不存在或不属于当前单据");
            return errors;
        }
        if (StringUtils.hasText(row.getInspectionItemCode())
                && !Objects.equals(resolveInspectionItemCode(item), row.getInspectionItemCode())) {
            errors.add("检验项编码与当前快照不匹配");
        }
        if (StringUtils.hasText(row.getItemType()) && !Objects.equals(item.getItemType(), row.getItemType())) {
            errors.add("项目类型与当前快照不匹配");
        }
        if (StringUtils.hasText(row.getValueTemplate()) && !Objects.equals(
                resolveValueTemplate(item.getItemType(), item.getValueTemplate()), row.getValueTemplate())) {
            errors.add("录入模板与当前快照不匹配");
        }
        Integer sampleSeq = resolveImportSampleSeq(row, positionSeqMap.get(item.getId()));
        if (sampleSeq == null) {
            errors.add("样本序号、位置编码或组次不属于当前检验项模板");
        }
        String template = resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        boolean hasValue = hasItemImportValue(plan, row, item);
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType()) && hasValue
                && !JUDGMENT_OK.equals(row.getQualitativeValue()) && !JUDGMENT_NG.equals(row.getQualitativeValue())) {
            errors.add("定性判定只能填写 OK 或 NG");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && hasValue
                && hasCustomInputFields(item) && hasMissingDynamicRequiredValue(plan, row, item)) {
            errors.add("自定义字段存在未填写项，请按当前检验项填写说明完整录入");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_DENSITY_CALC.equals(template)
                && hasValue
                && (row.getThicknessMm() == null || row.getWeightG() == null)) {
            errors.add("密度项必须填写厚度和重量");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_DENSITY_CALC.equals(template)
                && hasValue && isMissingOrZero(row.getThicknessMm())) {
            errors.add("密度项厚度不能为 0");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_COMPRESSION_CALC.equals(template)
                && hasValue
                && (row.getT1Mm() == null || row.getT2Mm() == null || row.getT3Mm() == null)) {
            errors.add("压缩性能项必须填写 T1、T2、T3");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_COMPRESSION_CALC.equals(template)
                && hasValue
                && (isMissingOrZero(row.getT1Mm()) || Objects.equals(row.getT1Mm(), row.getT2Mm()))) {
            errors.add("压缩性能项 T1 不能为 0 且不能等于 T2");
        }
        return errors;
    }

    private Map<String, Object> buildItemImportRawValues(QmsFqcItemDO item, QmsFqcItemImportExcelVO row,
                                                         Map<String, Object> parsedRawValues) {
        Map<String, Object> rawValues = new LinkedHashMap<>();
        if (parsedRawValues != null && !parsedRawValues.isEmpty()) {
            rawValues.putAll(parsedRawValues);
        }
        String template = resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        if (hasCustomInputFields(item)) {
            return rawValues;
        }
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            rawValues.put("thicknessMm", row.getThicknessMm());
            rawValues.put("weightG", row.getWeightG());
            rawValues.put("diameterMm", getDecimal(parseRawValues(item.getTemplateParams()), "diameterMm",
                    DEFAULT_SAMPLE_DIAMETER_MM));
            return rawValues;
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            rawValues.put("t1Mm", row.getT1Mm());
            rawValues.put("t2Mm", row.getT2Mm());
            rawValues.put("t3Mm", row.getT3Mm());
            return rawValues;
        }
        rawValues.put("value", row.getMeasuredValue());
        return rawValues;
    }

    private void normalizeItemImportRow(QmsFqcItemImportExcelVO row) {
        row.setFqcNo(trimToNull(row.getFqcNo()));
        row.setTemplateVersionHash(trimToNull(row.getTemplateVersionHash()));
        row.setInspectionItemCode(trimToNull(row.getInspectionItemCode()));
        row.setPositionCode(trimToNull(row.getPositionCode()));
        row.setPositionName(trimToNull(row.getPositionName()));
        row.setQualitativeValue(normalizeJudgment(row.getQualitativeValue()));
    }

    private boolean isRecognizedItemImportRow(QmsFqcItemImportExcelVO row) {
        return row != null && (row.getFqcItemId() != null || StringUtils.hasText(row.getInspectionItem()));
    }

    private boolean hasItemImportValue(ItemImportPlan plan, QmsFqcItemImportExcelVO row, QmsFqcItemDO item) {
        Map<String, Object> rawValues = plan.rawValuesMap.get(row);
        if (item == null) {
            return row.getMeasuredValue() != null
                    || row.getThicknessMm() != null
                    || row.getWeightG() != null
                    || row.getT1Mm() != null
                    || row.getT2Mm() != null
                    || row.getT3Mm() != null
                    || StringUtils.hasText(row.getQualitativeValue())
                    || hasAnyRawValue(rawValues);
        }
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return StringUtils.hasText(row.getQualitativeValue());
        }
        if (hasCustomInputFields(item)) {
            return hasAnyRawValue(rawValues);
        }
        String template = resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            return row.getThicknessMm() != null || row.getWeightG() != null;
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            return row.getT1Mm() != null || row.getT2Mm() != null || row.getT3Mm() != null;
        }
        return row.getMeasuredValue() != null;
    }

    private boolean hasAnyRawValue(Map<String, Object> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return false;
        }
        return rawValues.values().stream().anyMatch(value -> value != null && StringUtils.hasText(String.valueOf(value)));
    }

    private boolean hasCustomInputFields(QmsFqcItemDO item) {
        return resolveDynamicFieldDefinitions(item).stream().anyMatch(field -> FIELD_KIND_CUSTOM.equals(field.kind));
    }

    private boolean hasMissingDynamicRequiredValue(ItemImportPlan plan, QmsFqcItemImportExcelVO row, QmsFqcItemDO item) {
        Map<String, Object> rawValues = plan.rawValuesMap.get(row);
        for (DynamicFieldDefinition field : resolveDynamicFieldDefinitions(item)) {
            if (!FIELD_KIND_CUSTOM.equals(field.kind)) {
                continue;
            }
            Object value = rawValues == null ? null : rawValues.get(field.code);
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExistingSampleValue(QmsFqcSampleDO sample) {
        if (sample == null) {
            return false;
        }
        return sample.getMeasuredValue() != null
                || sample.getResultValue() != null
                || sample.getDensityValue() != null
                || sample.getCompressionRate() != null
                || sample.getCompressionElasticityRate() != null
                || hasMeaningfulText(sample.getRawValuesJson())
                || hasMeaningfulText(sample.getQualitativeValue())
                || JUDGMENT_OK.equals(sample.getSampleResult())
                || JUDGMENT_NG.equals(sample.getSampleResult());
    }

    private boolean hasMeaningfulText(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return !"-".equals(trimmed) && !"{}".equals(trimmed) && !"[]".equals(trimmed);
    }

    private List<ItemImportPosition> resolveItemImportPositions(QmsFqcItemDO item) {
        Map<String, Object> params = parseRawValues(item.getTemplateParams());
        int expectedCount = resolveExpectedSampleCount(item);
        Integer repeatCount = toPositiveInt(params.get("repeatCount"));
        int repeat = repeatCount == null ? 1 : repeatCount;
        List<ItemImportPosition> positions = new ArrayList<>();
        Object positionObj = params.get("positions");
        if (positionObj instanceof Collection<?> collection && !collection.isEmpty()) {
            int positionIndex = 0;
            for (Object value : collection) {
                positionIndex++;
                Map<String, Object> positionMap = toStringKeyMap(value);
                String code = defaultIfBlank(toText(positionMap.get("code")), "P" + positionIndex);
                String name = defaultIfBlank(toText(positionMap.get("name")), code);
                for (int repeatSeq = 1; repeatSeq <= repeat; repeatSeq++) {
                    positions.add(new ItemImportPosition(code, name, repeatSeq, positions.size() + 1));
                }
            }
        }
        while (positions.size() < expectedCount) {
            int next = positions.size() + 1;
            String code = ITEM_TYPE_QUALITATIVE.equals(item.getItemType()) && expectedCount == 1 ? "QUALITATIVE" : "P" + next;
            String name = ITEM_TYPE_QUALITATIVE.equals(item.getItemType()) && expectedCount == 1 ? "判定" : String.valueOf(next);
            positions.add(new ItemImportPosition(code, name, 1, next));
        }
        return positions.size() > expectedCount ? positions.subList(0, expectedCount) : positions;
    }

    private Map<String, Integer> buildPositionSeqMap(QmsFqcItemDO item) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (ItemImportPosition position : resolveItemImportPositions(item)) {
            map.put(position.code + "#" + position.repeatSeq, position.sampleSeq);
        }
        return map;
    }

    private Integer resolveImportSampleSeq(QmsFqcItemImportExcelVO row, Map<String, Integer> positionSeqMap) {
        if (positionSeqMap == null || positionSeqMap.isEmpty()) {
            return null;
        }
        Integer positionSampleSeq = null;
        if (StringUtils.hasText(row.getPositionCode()) && row.getRepeatSeq() != null) {
            positionSampleSeq = positionSeqMap.get(row.getPositionCode() + "#" + row.getRepeatSeq());
            if (positionSampleSeq == null) {
                return null;
            }
        }
        if (row.getSampleSeq() != null && row.getSampleSeq() > 0) {
            if (!positionSeqMap.containsValue(row.getSampleSeq())) {
                return null;
            }
            return positionSampleSeq == null || Objects.equals(positionSampleSeq, row.getSampleSeq())
                    ? row.getSampleSeq() : null;
        }
        return positionSampleSeq;
    }

    private String resolveInspectionItemCode(QmsFqcItemDO item) {
        return defaultIfBlank(item.getMetricCode(),
                defaultIfBlank(item.getSheetMetricCode(), "FQC_ITEM_" + item.getId()));
    }

    private int resolveExpectedSampleCount(QmsFqcItemDO item) {
        int baseFallback = item.getSampleSize() == null || item.getSampleSize() < 1 ? 1 : item.getSampleSize();
        Map<String, Object> templateParams = parseRawValues(item.getTemplateParams());
        Integer sampleSize = toPositiveInt(templateParams.get("sampleSize"));
        if (sampleSize != null) {
            return sampleSize;
        }
        Object positions = templateParams.get("positions");
        if (positions instanceof Collection<?> collection && !collection.isEmpty()) {
            Integer repeatCount = toPositiveInt(templateParams.get("repeatCount"));
            return collection.size() * (repeatCount == null ? 1 : repeatCount);
        }
        return baseFallback;
    }

    private String resolveValueTemplate(String itemType, String valueTemplate) {
        if (!ITEM_TYPE_QUANTITATIVE.equals(itemType)) {
            return null;
        }
        String template = defaultIfBlank(valueTemplate, TEMPLATE_SINGLE_VALUE);
        return TEMPLATE_DENSITY_CALC.equals(template) || TEMPLATE_COMPRESSION_CALC.equals(template)
                ? template : TEMPLATE_SINGLE_VALUE;
    }

    private String resolveJudgmentMetric(String valueTemplate, String judgmentMetric) {
        if (StringUtils.hasText(judgmentMetric)) {
            return judgmentMetric;
        }
        if (TEMPLATE_DENSITY_CALC.equals(valueTemplate)) {
            return "DENSITY_VALUE";
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(valueTemplate)) {
            return "COMPRESSION_RATE";
        }
        return "RESULT_VALUE";
    }

    private String formatLimitText(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "";
        }
        return (min == null ? "-" : min.stripTrailingZeros().toPlainString())
                + "~"
                + (max == null ? "-" : max.stripTrailingZeros().toPlainString());
    }

    private Map<String, Object> parseRawValues(String rawValuesJson) {
        if (!StringUtils.hasText(rawValuesJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> rawValues = JsonUtils.parseObjectQuietly(rawValuesJson,
                new TypeReference<Map<String, Object>>() {});
        return rawValues == null ? Collections.emptyMap() : rawValues;
    }

    private Map<String, Object> toStringKeyMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream().collect(Collectors.toMap(
                entry -> String.valueOf(entry.getKey()), Map.Entry::getValue,
                (first, ignored) -> first, LinkedHashMap::new));
    }

    private BigDecimal getDecimal(Map<String, Object> values, String key, BigDecimal defaultValue) {
        Object value = values.get(key);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String readCellString(Row row, DataFormatter formatter, int columnIndex) {
        if (row == null || columnIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        String value = cell == null ? null : formatter.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal readCellDecimal(Row row, DataFormatter formatter, int columnIndex) {
        String value = readCellString(row, formatter, columnIndex);
        if (!StringUtils.hasText(value) || value.startsWith("=")) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim()).longValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim()).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer toPositiveInt(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        try {
            int result = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
            return result > 0 ? result : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isMissingOrZero(BigDecimal value) {
        return value == null || BigDecimal.ZERO.compareTo(value) == 0;
    }

    private String normalizeJudgment(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String sampleKey(QmsFqcSampleDO sample) {
        return sampleKey(sample.getFqcItemId(), sample.getSampleSeq());
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    static class ItemImportPlan {
        final QmsFqcOrderDO order;
        final Map<Long, QmsFqcItemDO> itemMap;
        final String templateVersionHash;
        final String fileName;
        final boolean allowOverwrite;
        final boolean previewOnly;
        final List<ItemImportRow> validRows = new ArrayList<>();
        final List<String> messages = new ArrayList<>();
        final Map<QmsFqcItemImportExcelVO, Map<String, Object>> rawValuesMap = new IdentityHashMap<>();
        int totalCount;
        int successCount;
        int failureCount;
        int warningCount;

        ItemImportPlan(QmsFqcOrderDO order, Map<Long, QmsFqcItemDO> itemMap, String templateVersionHash,
                       String fileName, boolean allowOverwrite, boolean previewOnly) {
            this.order = order;
            this.itemMap = itemMap;
            this.templateVersionHash = templateVersionHash;
            this.fileName = fileName;
            this.allowOverwrite = allowOverwrite;
            this.previewOnly = previewOnly;
        }

        void addFailure(String message) {
            failureCount++;
            messages.add(message);
        }
    }

    static class ItemImportRow {
        final QmsFqcItemImportExcelVO row;
        final QmsFqcItemDO item;
        final Integer sampleSeq;
        final Map<String, Object> rawValues;

        ItemImportRow(QmsFqcItemImportExcelVO row, QmsFqcItemDO item, Integer sampleSeq,
                      Map<String, Object> rawValues) {
            this.row = row;
            this.item = item;
            this.sampleSeq = sampleSeq;
            this.rawValues = rawValues == null ? Collections.emptyMap() : rawValues;
        }
    }

    private static class DynamicWorkbookLayout {
        private final List<DynamicColumn> columns;
        private final Map<Long, List<DynamicFieldDefinition>> itemFieldMap;

        private DynamicWorkbookLayout(List<DynamicColumn> columns,
                                      Map<Long, List<DynamicFieldDefinition>> itemFieldMap) {
            this.columns = columns;
            this.itemFieldMap = itemFieldMap;
        }

        private int indexOfSystem(String systemKey) {
            for (int i = 0; i < columns.size(); i++) {
                DynamicColumn column = columns.get(i);
                if (Objects.equals(systemKey, column.systemKey)) {
                    return i;
                }
            }
            return -1;
        }

        private int indexOfDynamic(String columnKey) {
            for (int i = 0; i < columns.size(); i++) {
                DynamicColumn column = columns.get(i);
                if (column.dynamicField != null && Objects.equals(columnKey, column.dynamicField.columnKey)) {
                    return i;
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

        private DynamicColumn(String header, String systemKey, DynamicFieldDefinition dynamicField, boolean hidden) {
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

        private DynamicFieldDefinition(String columnKey, String code, String header, String kind) {
            this.columnKey = columnKey;
            this.code = code;
            this.header = header;
            this.kind = kind;
        }
    }

    private static class ItemImportPosition {
        private final String code;
        private final String name;
        private final Integer repeatSeq;
        private final Integer sampleSeq;

        private ItemImportPosition(String code, String name, Integer repeatSeq, Integer sampleSeq) {
            this.code = code;
            this.name = name;
            this.repeatSeq = repeatSeq;
            this.sampleSeq = sampleSeq;
        }
    }
}
