package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * FAI 检验项工作簿导入导出支撑。
 */
@Service
class QmsFaiItemWorkbookService {

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
    private static final BigDecimal PI = BigDecimal.valueOf(3.14);
    private static final int CALC_SCALE = 6;
    private static final int DETAIL_HEADER_ROW_INDEX = 4;
    private static final int ITEM_OVERVIEW_SAMPLE_COUNT = 15;
    private static final int[] ITEM_OVERVIEW_GROUP_COUNTS = {3, 3, 3, 3, 3};
    private static final String[] ITEM_OVERVIEW_GROUP_NAMES = {"L5", "L3", "圆\n心\nO", "R3", "R5"};

    List<QmsFaiItemImportExcelVO> buildItemTemplateRows(QmsFaiOrderDO order, List<QmsFaiItemDO> items,
                                                        String templateVersionHash,
                                                        Map<String, QmsFaiSampleDO> sampleMap) {
        List<QmsFaiItemImportExcelVO> rows = new ArrayList<>();
        for (QmsFaiItemDO item : items) {
            List<ItemImportPosition> positions = resolveItemImportPositions(item);
            for (ItemImportPosition position : positions) {
                QmsFaiItemImportExcelVO row = new QmsFaiItemImportExcelVO();
                row.setFaiId(order.getId());
                row.setFaiNo(order.getFaiNo());
                row.setTemplateVersionHash(templateVersionHash);
                row.setFaiItemId(item.getId());
                row.setSampleSeq(position.sampleSeq);
                row.setInspectionItemCode(resolveInspectionItemCode(item));
                row.setInspectionItem(item.getInspectionItem());
                row.setItemType(item.getItemType());
                row.setValueTemplate(QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(),
                        item.getValueTemplate()));
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

    byte[] buildItemOverviewWorkbook(QmsFaiOrderDO order, List<QmsFaiItemDO> items,
                                     Map<String, QmsFaiSampleDO> sampleMap) throws IOException {
        String templateVersionHash = buildItemTemplateVersionHash(order, items);
        List<QmsFaiItemImportExcelVO> rows = buildItemTemplateRows(order, items, templateVersionHash, sampleMap);
        return buildItemDetailWorkbook(order, items, rows, sampleMap);
    }

    private byte[] buildItemDetailWorkbook(QmsFaiOrderDO order, List<QmsFaiItemDO> items,
                                           List<QmsFaiItemImportExcelVO> rows,
                                           Map<String, QmsFaiSampleDO> sampleMap) throws IOException {
        DynamicWorkbookLayout layout = buildDynamicWorkbookLayout(items);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("检验项明细");
            CellStyle headerStyle = buildOverviewStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.GREY_25_PERCENT.getIndex());
            CellStyle titleStyle = buildOverviewStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            CellStyle infoLabelStyle = buildOverviewStyle(workbook, true, HorizontalAlignment.CENTER,
                    IndexedColors.LEMON_CHIFFON.getIndex());
            CellStyle normalStyle = buildOverviewStyle(workbook, false, HorizontalAlignment.LEFT, null);
            buildDocumentInfoRows(sheet, order, layout.columns.size(), titleStyle, infoLabelStyle, normalStyle);
            Row headerRow = sheet.createRow(DETAIL_HEADER_ROW_INDEX);
            for (int colIndex = 0; colIndex < layout.columns.size(); colIndex++) {
                DynamicColumn column = layout.columns.get(colIndex);
                setDetailCell(headerRow, colIndex, column.header, headerStyle);
                sheet.setColumnWidth(colIndex, itemDetailColumnWidth(column.header));
                if (column.hidden) {
                    sheet.setColumnHidden(colIndex, true);
                }
            }
            Map<Long, QmsFaiItemDO> itemMap = items.stream()
                    .collect(Collectors.toMap(QmsFaiItemDO::getId, Function.identity(), (first, ignored) -> first,
                            LinkedHashMap::new));
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                QmsFaiItemImportExcelVO row = rows.get(rowIndex);
                QmsFaiItemDO item = itemMap.get(row.getFaiItemId());
                Row dataRow = sheet.createRow(DETAIL_HEADER_ROW_INDEX + 1 + rowIndex);
                for (int colIndex = 0; colIndex < layout.columns.size(); colIndex++) {
                    setDetailCell(dataRow, colIndex,
                            itemDetailValue(order, item, row, layout.columns.get(colIndex), sampleMap), normalStyle);
                }
            }
            sheet.createFreezePane(0, DETAIL_HEADER_ROW_INDEX + 1);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void buildDocumentInfoRows(Sheet sheet, QmsFaiOrderDO order, int columnCount, CellStyle titleStyle,
                                       CellStyle labelStyle, CellStyle valueStyle) {
        int lastColumn = Math.max(columnCount - 1, 7);
        setMergedOverviewCell(sheet, 0, 0, 1, lastColumn + 1,
                "FAI 检验项明细导入模板", titleStyle);
        setInfoPair(sheet, 1, 0, "首检单号", order.getFaiNo(), labelStyle, valueStyle);
        setInfoPair(sheet, 1, 2, "工单号", order.getWorkOrderNo(), labelStyle, valueStyle);
        setInfoPair(sheet, 1, 4, "物料编码", order.getMaterialCode(), labelStyle, valueStyle);
        setInfoPair(sheet, 1, 6, "产品型号", order.getProductModel(), labelStyle, valueStyle);
        setInfoPair(sheet, 2, 0, "检测工序", order.getOperationName(), labelStyle, valueStyle);
        setInfoPair(sheet, 2, 2, "产品批次", order.getProductBatchNo(), labelStyle, valueStyle);
        setInfoPair(sheet, 2, 4, "送检数量", order.getInspectionQty() == null ? "" : String.valueOf(order.getInspectionQty()),
                labelStyle, valueStyle);
        setInfoPair(sheet, 2, 6, "模板说明", "请勿删除隐藏列；填写后直接导入当前 FAI 单", labelStyle, valueStyle);
    }

    private void setInfoPair(Sheet sheet, int rowIndex, int colIndex, String label, String value, CellStyle labelStyle,
                             CellStyle valueStyle) {
        setOverviewCell(sheet, rowIndex, colIndex, label, labelStyle);
        setOverviewCell(sheet, rowIndex, colIndex + 1, value == null ? "" : value, valueStyle);
    }

    private DynamicWorkbookLayout buildDynamicWorkbookLayout(List<QmsFaiItemDO> items) {
        List<DynamicColumn> columns = new ArrayList<>();
        columns.add(DynamicColumn.system("单据号", "ORDER_NO", false));
        columns.add(DynamicColumn.system("检验项目", "INSPECTION_ITEM", false));
        columns.add(DynamicColumn.system("样本序号", "SAMPLE_SEQ", false));
        columns.add(DynamicColumn.system("样本位置", "POSITION_NAME", false));
        columns.add(DynamicColumn.system("填写说明", "FILLING_INSTRUCTION", false));

        Map<String, DynamicFieldDefinition> dynamicFieldMap = new LinkedHashMap<>();
        for (QmsFaiItemDO item : items) {
            List<DynamicFieldDefinition> itemFields = resolveDynamicFieldDefinitions(item);
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
        return new DynamicWorkbookLayout(columns);
    }

    private Object itemDetailValue(QmsFaiOrderDO order, QmsFaiItemDO item, QmsFaiItemImportExcelVO row,
                                   DynamicColumn column, Map<String, QmsFaiSampleDO> sampleMap) {
        if (column.dynamicField != null) {
            return dynamicFieldValue(item, row, column.dynamicField, sampleMap);
        }
        return switch (column.systemKey) {
            case "ORDER_NO" -> row.getFaiNo();
            case "INSPECTION_ITEM" -> row.getInspectionItem();
            case "SAMPLE_SEQ" -> row.getSampleSeq();
            case "POSITION_NAME" -> row.getPositionName();
            case "FILLING_INSTRUCTION" -> buildFillingInstruction(item);
            case "SAMPLE_RESULT" -> row.getSampleResult();
            case "REMARK" -> row.getRemark();
            case "ORDER_ID" -> order.getId();
            case "TEMPLATE_HASH" -> row.getTemplateVersionHash();
            case "ITEM_ID" -> row.getFaiItemId();
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

    private Object dynamicFieldValue(QmsFaiItemDO item, QmsFaiItemImportExcelVO row, DynamicFieldDefinition field,
                                     Map<String, QmsFaiSampleDO> sampleMap) {
        if (item == null) {
            return "";
        }
        QmsFaiSampleDO sample = sampleMap.get(sampleKey(item.getId(), row.getSampleSeq()));
        Map<String, Object> rawValues = sample == null ? Collections.emptyMap()
                : QmsFaiRuleCalculationSupport.parseRawValues(sample.getRawValuesJson());
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

    private String buildFillingInstruction(QmsFaiItemDO item) {
        if (item == null) {
            return "";
        }
        List<DynamicFieldDefinition> fields = resolveDynamicFieldDefinitions(item);
        String fieldNames = fields.stream().map(field -> field.header).collect(Collectors.joining("、"));
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return "填写 OK 或 NG";
        }
        String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(), item.getValueTemplate());
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

    private String resolveJudgmentMetricText(QmsFaiItemDO item, String valueTemplate) {
        String fallback = QmsFaiRuleCalculationSupport.resolveJudgmentMetric(valueTemplate, item.getJudgmentMetric());
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

    private List<DynamicFieldDefinition> resolveDynamicFieldDefinitions(QmsFaiItemDO item) {
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            return List.of(new DynamicFieldDefinition("QUALITATIVE:qualitativeValue", "qualitativeValue",
                    "定性判定", FIELD_KIND_QUALITATIVE));
        }
        Map<String, Object> params = QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams());
        List<DynamicFieldDefinition> customFields = resolveCustomInputFields(params);
        if (!customFields.isEmpty()) {
            return customFields;
        }
        String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(), item.getValueTemplate());
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

    private String resolveSingleValueLabel(QmsFaiItemDO item, Map<String, Object> params) {
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

    private int itemDetailColumnWidth(String header) {
        int width = Math.max(header.length() + 4, 12);
        if ("模板校验码".equals(header) || "填写说明".equals(header)) {
            width = 68;
        } else if ("检验项目".equals(header) || "备注".equals(header)) {
            width = 24;
        }
        return Math.min(width, 80) * 256;
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

    String buildItemTemplateVersionHash(QmsFaiOrderDO order, List<QmsFaiItemDO> items) {
        String payload = order.getId() + "|" + order.getFaiNo() + "|" + items.stream()
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

    private void fillItemImportSample(QmsFaiItemImportExcelVO row, QmsFaiSampleDO sample) {
        if (sample == null) {
            return;
        }
        Map<String, Object> rawValues = QmsFaiRuleCalculationSupport.parseRawValues(sample.getRawValuesJson());
        row.setMeasuredValue(sample.getMeasuredValue() == null ? sample.getResultValue() : sample.getMeasuredValue());
        row.setThicknessMm(QmsFaiRuleCalculationSupport.toDecimal(rawValues.get("thicknessMm")));
        row.setWeightG(QmsFaiRuleCalculationSupport.toDecimal(rawValues.get("weightG")));
        row.setT1Mm(QmsFaiRuleCalculationSupport.toDecimal(rawValues.get("t1Mm")));
        row.setT2Mm(QmsFaiRuleCalculationSupport.toDecimal(rawValues.get("t2Mm")));
        row.setT3Mm(QmsFaiRuleCalculationSupport.toDecimal(rawValues.get("t3Mm")));
        row.setQualitativeValue(sample.getQualitativeValue());
        row.setRemark(sample.getRemark());
        row.setSampleResult(sample.getSampleResult());
        row.setValueSource(sample.getValueSource());
        row.setImportBatchNo(sample.getImportBatchNo());
    }

    ItemImportPlan buildItemImportPlan(QmsFaiOrderDO order, List<QmsFaiItemDO> items,
                                       Map<String, QmsFaiSampleDO> existingSampleMap, MultipartFile file,
                                       boolean allowOverwrite, boolean previewOnly, boolean importBlocked)
            throws IOException {
        Map<Long, QmsFaiItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsFaiItemDO::getId, Function.identity(), (first, ignored) -> first,
                        LinkedHashMap::new));
        String templateVersionHash = buildItemTemplateVersionHash(order, items);
        ItemImportPlan plan = new ItemImportPlan(order, itemMap, templateVersionHash,
                file == null ? null : file.getOriginalFilename(), allowOverwrite, previewOnly);
        if (file == null || file.isEmpty()) {
            plan.addFailure("请选择需要导入的 .xlsx 或 .xls 文件");
            return plan;
        }
        if (importBlocked) {
            plan.addFailure("当前 FAI 单已结案、待审核或已锁定，不允许导入覆盖");
            return plan;
        }

        Map<Long, Map<String, Integer>> positionSeqMap = items.stream()
                .collect(Collectors.toMap(QmsFaiItemDO::getId, this::buildPositionSeqMap));
        List<QmsFaiItemImportExcelVO> rows = readDynamicItemImportRows(plan, items, file).stream()
                .filter(this::isRecognizedItemImportRow)
                .collect(Collectors.toList());
        plan.totalCount = rows.size();
        if (rows.isEmpty()) {
            plan.addFailure("未识别到可导入的 FAI 检验项样本行");
            return plan;
        }

        for (int i = 0; i < rows.size(); i++) {
            QmsFaiItemImportExcelVO row = rows.get(i);
            int excelRowNo = DETAIL_HEADER_ROW_INDEX + i + 2;
            normalizeItemImportRow(row);
            List<String> errors = validateItemImportRow(plan, row, positionSeqMap);
            QmsFaiItemDO item = row.getFaiItemId() == null ? null : itemMap.get(row.getFaiItemId());
            Integer sampleSeq = resolveImportSampleSeq(row, positionSeqMap.get(row.getFaiItemId()));
            boolean hasValue = hasItemImportValue(plan, row, item);
            if (!hasValue) {
                plan.warningCount++;
                plan.messages.add("第 " + excelRowNo + " 行未填写业务值，已跳过");
            }
            QmsFaiSampleDO existingSample = item == null || sampleSeq == null ? null
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

    QmsFaiSaveReqVO.FaiSample buildImportedSample(ItemImportRow row, String importBatchNo,
                                                  QmsFaiSampleDO existing) {
        QmsFaiSaveReqVO.FaiSample sample = existing == null ? new QmsFaiSaveReqVO.FaiSample()
                : BeanUtils.toBean(existing, QmsFaiSaveReqVO.FaiSample.class);
        QmsFaiItemDO item = row.item;
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

    QmsFaiImportRespVO buildItemImportResp(ItemImportPlan plan, QmsFaiRespVO record) {
        QmsFaiImportRespVO respVO = new QmsFaiImportRespVO();
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

    private List<QmsFaiItemImportExcelVO> readDynamicItemImportRows(ItemImportPlan plan, List<QmsFaiItemDO> items,
                                                                    MultipartFile file) throws IOException {
        DynamicWorkbookLayout layout = buildDynamicWorkbookLayout(items);
        Map<Long, QmsFaiItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsFaiItemDO::getId, Function.identity(), (first, ignored) -> first,
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
            int headerRowIndex = findDynamicItemHeaderRow(sheet, formatter, layout);
            if (headerRowIndex < 0) {
                plan.addFailure("Excel 表头不匹配，请使用当前 FAI 单下载模板导入");
                return Collections.emptyList();
            }
            List<QmsFaiItemImportExcelVO> rows = new ArrayList<>();
            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (isBlankRow(dataRow, formatter, layout.columns.size())) {
                    continue;
                }
                QmsFaiItemImportExcelVO row = readDynamicItemRow(dataRow, formatter, layout, itemMap);
                rows.add(row);
                Map<String, Object> rawValues = readDynamicRawValues(dataRow, formatter, layout,
                        itemMap.get(row.getFaiItemId()), row);
                if (!rawValues.isEmpty()) {
                    plan.rawValuesMap.put(row, rawValues);
                }
            }
            return rows;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            plan.addFailure("Excel 解析失败，请确认文件来自当前 FAI 单下载模板");
            return Collections.emptyList();
        }
    }

    private int findDynamicItemHeaderRow(Sheet sheet, DataFormatter formatter, DynamicWorkbookLayout layout) {
        int maxRow = Math.min(sheet.getLastRowNum(), 12);
        for (int rowIndex = 0; rowIndex <= maxRow; rowIndex++) {
            Row header = sheet.getRow(rowIndex);
            if (header == null) {
                continue;
            }
            if (Objects.equals("单据号", readCellString(header, formatter, layout.indexOfSystem("ORDER_NO")))
                    && Objects.equals("检验项目", readCellString(header, formatter, layout.indexOfSystem("INSPECTION_ITEM")))
                    && Objects.equals("单据 ID", readCellString(header, formatter, layout.indexOfSystem("ORDER_ID")))
                    && Objects.equals("模板校验码", readCellString(header, formatter, layout.indexOfSystem("TEMPLATE_HASH")))) {
                return rowIndex;
            }
        }
        return -1;
    }

    private QmsFaiItemImportExcelVO readDynamicItemRow(Row dataRow, DataFormatter formatter,
                                                       DynamicWorkbookLayout layout,
                                                       Map<Long, QmsFaiItemDO> itemMap) {
        QmsFaiItemImportExcelVO row = new QmsFaiItemImportExcelVO();
        row.setFaiNo(readCellString(dataRow, formatter, layout.indexOfSystem("ORDER_NO")));
        row.setInspectionItem(readCellString(dataRow, formatter, layout.indexOfSystem("INSPECTION_ITEM")));
        row.setSampleSeq(parseInteger(readCellString(dataRow, formatter, layout.indexOfSystem("SAMPLE_SEQ"))));
        row.setPositionName(readCellString(dataRow, formatter, layout.indexOfSystem("POSITION_NAME")));
        row.setSampleResult(readCellString(dataRow, formatter, layout.indexOfSystem("SAMPLE_RESULT")));
        row.setRemark(readCellString(dataRow, formatter, layout.indexOfSystem("REMARK")));
        row.setFaiId(parseLong(readCellString(dataRow, formatter, layout.indexOfSystem("ORDER_ID"))));
        row.setTemplateVersionHash(readCellString(dataRow, formatter, layout.indexOfSystem("TEMPLATE_HASH")));
        row.setFaiItemId(parseLong(readCellString(dataRow, formatter, layout.indexOfSystem("ITEM_ID"))));
        row.setInspectionItemCode(readCellString(dataRow, formatter, layout.indexOfSystem("ITEM_CODE")));
        row.setItemType(readCellString(dataRow, formatter, layout.indexOfSystem("ITEM_TYPE_RAW")));
        row.setValueTemplate(readCellString(dataRow, formatter, layout.indexOfSystem("VALUE_TEMPLATE_RAW")));
        row.setPositionCode(readCellString(dataRow, formatter, layout.indexOfSystem("POSITION_CODE")));
        row.setRepeatSeq(parseInteger(readCellString(dataRow, formatter, layout.indexOfSystem("REPEAT_SEQ"))));
        row.setValueSource(readCellString(dataRow, formatter, layout.indexOfSystem("VALUE_SOURCE")));
        row.setImportBatchNo(readCellString(dataRow, formatter, layout.indexOfSystem("IMPORT_BATCH")));
        QmsFaiItemDO item = itemMap.get(row.getFaiItemId());
        if (item != null) {
            row.setInspectionItem(defaultIfBlank(row.getInspectionItem(), item.getInspectionItem()));
            row.setItemType(defaultIfBlank(row.getItemType(), item.getItemType()));
            row.setValueTemplate(defaultIfBlank(row.getValueTemplate(),
                    QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(), item.getValueTemplate())));
        }
        return row;
    }

    private Map<String, Object> readDynamicRawValues(Row dataRow, DataFormatter formatter,
                                                     DynamicWorkbookLayout layout, QmsFaiItemDO item,
                                                     QmsFaiItemImportExcelVO row) {
        if (item == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> rawValues = new LinkedHashMap<>();
        for (DynamicFieldDefinition field : resolveDynamicFieldDefinitions(item)) {
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

    private void applyDynamicValueToRow(QmsFaiItemImportExcelVO row, DynamicFieldDefinition field, BigDecimal value) {
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

    private List<String> validateItemImportRow(ItemImportPlan plan, QmsFaiItemImportExcelVO row,
                                               Map<Long, Map<String, Integer>> positionSeqMap) {
        List<String> errors = new ArrayList<>();
        if (!Objects.equals(plan.order.getId(), row.getFaiId())) {
            errors.add("FAI ID 与当前单据不匹配");
        }
        if (!Objects.equals(plan.order.getFaiNo(), row.getFaiNo())) {
            errors.add("FAI 单号与当前单据不匹配");
        }
        if (!Objects.equals(plan.templateVersionHash, row.getTemplateVersionHash())) {
            errors.add("模板校验码不匹配，请重新下载当前单据模板");
        }
        QmsFaiItemDO item = row.getFaiItemId() == null ? null : plan.itemMap.get(row.getFaiItemId());
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
        String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        if (StringUtils.hasText(row.getValueTemplate()) && !Objects.equals(template, row.getValueTemplate())) {
            errors.add("录入模板与当前快照不匹配");
        }
        Integer sampleSeq = resolveImportSampleSeq(row, positionSeqMap.get(item.getId()));
        if (sampleSeq == null) {
            errors.add("样本序号、位置编码或组次不属于当前检验项模板");
        }
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
                && hasValue && (row.getThicknessMm() == null || row.getWeightG() == null)) {
            errors.add("密度项必须填写厚度和重量");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_DENSITY_CALC.equals(template)
                && hasValue && QmsFaiRuleCalculationSupport.isMissingOrZero(row.getThicknessMm())) {
            errors.add("密度项厚度不能为 0");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_COMPRESSION_CALC.equals(template)
                && hasValue && (row.getT1Mm() == null || row.getT2Mm() == null || row.getT3Mm() == null)) {
            errors.add("压缩性能项必须填写 T1、T2、T3");
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) && TEMPLATE_COMPRESSION_CALC.equals(template)
                && hasValue && (QmsFaiRuleCalculationSupport.isMissingOrZero(row.getT1Mm())
                || Objects.equals(row.getT1Mm(), row.getT2Mm()))) {
            errors.add("压缩性能项 T1 不能为 0 且不能等于 T2");
        }
        return errors;
    }

    private Map<String, Object> buildItemImportRawValues(QmsFaiItemDO item, QmsFaiItemImportExcelVO row,
                                                         Map<String, Object> parsedRawValues) {
        Map<String, Object> rawValues = new LinkedHashMap<>();
        if (parsedRawValues != null && !parsedRawValues.isEmpty()) {
            rawValues.putAll(parsedRawValues);
        }
        String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        if (hasCustomInputFields(item)) {
            return rawValues;
        }
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            rawValues.put("thicknessMm", row.getThicknessMm());
            rawValues.put("weightG", row.getWeightG());
            rawValues.put("diameterMm", QmsFaiRuleCalculationSupport.getDecimal(
                    QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams()), "diameterMm",
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

    private void normalizeItemImportRow(QmsFaiItemImportExcelVO row) {
        row.setFaiNo(trimToNull(row.getFaiNo()));
        row.setTemplateVersionHash(trimToNull(row.getTemplateVersionHash()));
        row.setInspectionItemCode(trimToNull(row.getInspectionItemCode()));
        row.setPositionCode(trimToNull(row.getPositionCode()));
        row.setPositionName(trimToNull(row.getPositionName()));
        row.setQualitativeValue(normalizeJudgment(row.getQualitativeValue()));
    }

    private boolean isRecognizedItemImportRow(QmsFaiItemImportExcelVO row) {
        return row != null && (row.getFaiItemId() != null || StringUtils.hasText(row.getInspectionItem()));
    }

    private boolean hasItemImportValue(ItemImportPlan plan, QmsFaiItemImportExcelVO row, QmsFaiItemDO item) {
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
        String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(item.getItemType(), item.getValueTemplate());
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

    private boolean hasCustomInputFields(QmsFaiItemDO item) {
        return resolveDynamicFieldDefinitions(item).stream().anyMatch(field -> FIELD_KIND_CUSTOM.equals(field.kind));
    }

    private boolean hasMissingDynamicRequiredValue(ItemImportPlan plan, QmsFaiItemImportExcelVO row, QmsFaiItemDO item) {
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

    private boolean hasExistingSampleValue(QmsFaiSampleDO sample) {
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

    private void setMergedOverviewCell(Sheet sheet, int firstRow, int firstCol, int rowSpan, int colSpan,
                                       String value, CellStyle style) {
        for (int rowIndex = firstRow; rowIndex < firstRow + rowSpan; rowIndex++) {
            for (int colIndex = firstCol; colIndex < firstCol + colSpan; colIndex++) {
                setOverviewCell(sheet, rowIndex, colIndex, "", style);
            }
        }
        setOverviewCell(sheet, firstRow, firstCol, value, style);
        if (rowSpan > 1 || colSpan > 1) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, firstRow + rowSpan - 1,
                    firstCol, firstCol + colSpan - 1));
        }
    }

    private void setOverviewCell(Sheet sheet, int rowIndex, int colIndex, String value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        row.setHeightInPoints(24);
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String buildOverviewHeaderText(QmsFaiOrderDO order) {
        return "日期：" + formatOverviewDate(order.getQaTime() == null ? order.getSubmissionTime() : order.getQaTime())
                + "    产品型号：" + defaultIfBlank(order.getProductModel(), "")
                + "    批号：" + defaultIfBlank(order.getProductBatchNo(), "")
                + "    规格：" + defaultIfBlank(order.getSpecification(), "")
                + "    片数："
                + "    最终判定结果：□A □R"
                + "    检验：" + defaultIfBlank(order.getOperatorName(), "")
                + "    确认：" + defaultIfBlank(order.getQaInspectorName(), "");
    }

    private String buildOverviewTargetText(String label, QmsFaiItemDO item, String unit) {
        return label;
    }

    private String buildOverviewControlText(String label, QmsFaiItemDO item, String type, String unit) {
        BigDecimal min = item == null ? null : ("avg".equals(type) ? item.getAvgMinLimit() : item.getStdMinLimit());
        BigDecimal max = item == null ? null : ("avg".equals(type) ? item.getAvgMaxLimit() : item.getStdMaxLimit());
        if (min == null && max == null) {
            return label + "待定";
        }
        return label + (min == null ? "-" : formatOverviewNumber(min, 3)) + "~"
                + (max == null ? "-" : formatOverviewNumber(max, 3)) + unit;
    }

    private String formatOverviewDate(LocalDateTime value) {
        return value == null ? "" : value.toLocalDate().toString();
    }

    private QmsFaiItemDO findOverviewThicknessItem(List<QmsFaiItemDO> items) {
        List<QmsFaiItemDO> napRawItems = items.stream()
                .filter(item -> "NAP_RAW".equals(resolveOverviewStepCode(item)))
                .collect(Collectors.toList());
        return napRawItems.stream()
                .filter(item -> metricText(item).contains("厚度"))
                .filter(item -> !TEMPLATE_DENSITY_CALC.equals(item.getValueTemplate()))
                .findFirst()
                .orElse(napRawItems.stream()
                        .filter(item -> TEMPLATE_SINGLE_VALUE.equals(QmsFaiRuleCalculationSupport.resolveValueTemplate(
                                item.getItemType(), item.getValueTemplate())))
                        .findFirst()
                        .orElse(null));
    }

    private QmsFaiItemDO findOverviewDensityItem(List<QmsFaiItemDO> items) {
        List<QmsFaiItemDO> napRawItems = items.stream()
                .filter(item -> "NAP_RAW".equals(resolveOverviewStepCode(item)))
                .collect(Collectors.toList());
        return napRawItems.stream()
                .filter(item -> TEMPLATE_DENSITY_CALC.equals(item.getValueTemplate()))
                .findFirst()
                .orElse(napRawItems.stream()
                        .filter(item -> metricText(item).contains("密度"))
                        .findFirst()
                        .orElse(null));
    }

    private String resolveOverviewStepCode(QmsFaiItemDO item) {
        if (StringUtils.hasText(item.getStepCode())) {
            return item.getStepCode();
        }
        String source = String.join(" ",
                defaultIfBlank(item.getSheetSectionCode(), ""),
                defaultIfBlank(item.getSheetSectionName(), ""),
                defaultIfBlank(item.getStepName(), ""),
                defaultIfBlank(item.getInspectionItem(), ""));
        if (source.contains("NAP_RAW") || source.contains("未磨皮")) {
            return "NAP_RAW";
        }
        if (source.contains("NAP_POLISHED") || source.contains("磨皮后")) {
            return "NAP_POLISHED";
        }
        return "";
    }

    private String metricText(QmsFaiItemDO item) {
        return String.join(" ",
                defaultIfBlank(item.getSheetMetricName(), ""),
                defaultIfBlank(item.getSheetMetricCode(), ""),
                defaultIfBlank(item.getInspectionItem(), ""),
                defaultIfBlank(item.getStandardDesc(), ""));
    }

    private QmsFaiSampleDO getOverviewSample(QmsFaiItemDO item, Map<String, QmsFaiSampleDO> sampleMap, int sampleIndex) {
        if (item == null) {
            return null;
        }
        return sampleMap.get(sampleKey(item.getId(), sampleIndex + 1));
    }

    private List<BigDecimal> collectOverviewValues(QmsFaiItemDO item, Map<String, QmsFaiSampleDO> sampleMap) {
        List<BigDecimal> values = new ArrayList<>();
        for (int index = 0; index < ITEM_OVERVIEW_SAMPLE_COUNT; index++) {
            BigDecimal value = resolveOverviewSampleValue(item, getOverviewSample(item, sampleMap, index));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private BigDecimal resolveOverviewSampleValue(QmsFaiItemDO item, QmsFaiSampleDO sample) {
        if (sample == null) {
            return null;
        }
        if (item != null && TEMPLATE_DENSITY_CALC.equals(QmsFaiRuleCalculationSupport.resolveValueTemplate(
                item.getItemType(), item.getValueTemplate()))) {
            if (sample.getDensityValue() != null) {
                return sample.getDensityValue();
            }
            if (sample.getResultValue() != null) {
                return sample.getResultValue();
            }
            BigDecimal thicknessMm = resolveRawDecimal(sample, "thicknessMm");
            BigDecimal weightG = resolveRawDecimal(sample, "weightG");
            BigDecimal diameterMm = QmsFaiRuleCalculationSupport.getTemplateParam(item, "diameterMm",
                    DEFAULT_SAMPLE_DIAMETER_MM);
            if (QmsFaiRuleCalculationSupport.isMissingOrZero(thicknessMm)
                    || QmsFaiRuleCalculationSupport.isMissingOrZero(diameterMm) || weightG == null) {
                return null;
            }
            BigDecimal denominator = thicknessMm
                    .divide(BigDecimal.TEN, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(PI)
                    .multiply(diameterMm)
                    .multiply(diameterMm)
                    .divide(BigDecimal.valueOf(4), CALC_SCALE, RoundingMode.HALF_UP);
            return QmsFaiRuleCalculationSupport.isMissingOrZero(denominator) ? null
                    : weightG.divide(denominator, CALC_SCALE, RoundingMode.HALF_UP);
        }
        return sample.getMeasuredValue() == null ? sample.getResultValue() : sample.getMeasuredValue();
    }

    private BigDecimal resolveRawDecimal(QmsFaiSampleDO sample, String key) {
        if (sample == null) {
            return null;
        }
        return QmsFaiRuleCalculationSupport.getDecimal(
                QmsFaiRuleCalculationSupport.parseRawValues(sample.getRawValuesJson()), key, null);
    }

    private BigDecimal calculateOverviewAvg(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), CALC_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOverviewStd(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal avg = calculateOverviewAvg(values);
        double variance = values.stream()
                .map(value -> value.subtract(avg).doubleValue())
                .mapToDouble(diff -> diff * diff)
                .sum() / values.size();
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(CALC_SCALE, RoundingMode.HALF_UP);
    }

    private String formatOverviewNumber(BigDecimal value, int scale) {
        if (value == null) {
            return "";
        }
        return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private List<ItemImportPosition> resolveItemImportPositions(QmsFaiItemDO item) {
        Map<String, Object> params = QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams());
        int expectedCount = resolveExpectedSampleCount(item);
        Integer repeatCount = QmsFaiRuleCalculationSupport.toPositiveInt(params.get("repeatCount"));
        int repeat = repeatCount == null ? 1 : repeatCount;
        List<ItemImportPosition> positions = new ArrayList<>();
        Object positionObj = params.get("positions");
        if (positionObj instanceof Collection<?> collection && !collection.isEmpty()) {
            int positionIndex = 0;
            for (Object value : collection) {
                positionIndex++;
                Map<String, Object> positionMap = value instanceof Map<?, ?> map
                        ? map.entrySet().stream().collect(Collectors.toMap(
                                entry -> String.valueOf(entry.getKey()), Map.Entry::getValue,
                                (first, ignored) -> first, LinkedHashMap::new))
                        : Collections.emptyMap();
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

    private Map<String, Integer> buildPositionSeqMap(QmsFaiItemDO item) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (ItemImportPosition position : resolveItemImportPositions(item)) {
            map.put(position.code + "#" + position.repeatSeq, position.sampleSeq);
        }
        return map;
    }

    private Integer resolveImportSampleSeq(QmsFaiItemImportExcelVO row, Map<String, Integer> positionSeqMap) {
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

    private String resolveInspectionItemCode(QmsFaiItemDO item) {
        return defaultIfBlank(item.getMetricCode(),
                defaultIfBlank(item.getSheetMetricCode(), "FAI_ITEM_" + item.getId()));
    }

    private int resolveExpectedSampleCount(QmsFaiItemDO itemDO) {
        int baseFallback = itemDO.getSampleSize() == null || itemDO.getSampleSize() < 1 ? 1 : itemDO.getSampleSize();
        return QmsFaiRuleCalculationSupport.resolveTemplateExpectedSampleCount(itemDO, baseFallback);
    }

    private String formatLimitText(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "";
        }
        return (min == null ? "-" : min.stripTrailingZeros().toPlainString())
                + "~"
                + (max == null ? "-" : max.stripTrailingZeros().toPlainString());
    }

    private String sampleKey(QmsFaiSampleDO sample) {
        return sampleKey(sample.getFaiItemId(), sample.getSampleSeq());
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
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
            return new BigDecimal(value.replace(",", "").replace("%", "").trim());
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

    private String normalizeJudgment(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String upper = trimmed.toUpperCase();
        if ("PASS".equals(upper) || "Y".equals(upper) || "YES".equals(upper)
                || "合格".equals(trimmed) || "通过".equals(trimmed)) {
            return JUDGMENT_OK;
        }
        if ("FAIL".equals(upper) || "N".equals(upper) || "NO".equals(upper)
                || "不合格".equals(trimmed) || "不通过".equals(trimmed)) {
            return JUDGMENT_NG;
        }
        return upper;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private Map<String, Object> toStringKeyMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream().collect(Collectors.toMap(
                entry -> String.valueOf(entry.getKey()), Map.Entry::getValue,
                (first, ignored) -> first, LinkedHashMap::new));
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    static class ItemImportPlan {
        final QmsFaiOrderDO order;
        final Map<Long, QmsFaiItemDO> itemMap;
        final String templateVersionHash;
        final String fileName;
        final boolean allowOverwrite;
        final boolean previewOnly;
        final List<ItemImportRow> validRows = new ArrayList<>();
        final List<String> messages = new ArrayList<>();
        final Map<QmsFaiItemImportExcelVO, Map<String, Object>> rawValuesMap = new IdentityHashMap<>();
        int totalCount;
        int successCount;
        int failureCount;
        int warningCount;

        ItemImportPlan(QmsFaiOrderDO order, Map<Long, QmsFaiItemDO> itemMap, String templateVersionHash,
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
        final QmsFaiItemImportExcelVO row;
        final QmsFaiItemDO item;
        final Integer sampleSeq;
        final Map<String, Object> rawValues;

        ItemImportRow(QmsFaiItemImportExcelVO row, QmsFaiItemDO item, Integer sampleSeq,
                      Map<String, Object> rawValues) {
            this.row = row;
            this.item = item;
            this.sampleSeq = sampleSeq;
            this.rawValues = rawValues == null ? Collections.emptyMap() : rawValues;
        }
    }

    private static class DynamicWorkbookLayout {
        private final List<DynamicColumn> columns;

        private DynamicWorkbookLayout(List<DynamicColumn> columns) {
            this.columns = columns;
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
