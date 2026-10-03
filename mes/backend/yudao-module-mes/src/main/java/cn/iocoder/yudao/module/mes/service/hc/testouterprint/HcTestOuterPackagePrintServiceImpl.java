package cn.iocoder.yudao.module.mes.service.hc.testouterprint;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintDesignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintPayloadItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPrintPayloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterVariableCheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterWaitPieceListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterWaitSegmentPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintCustomerInfoDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintDesignDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintLabelBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner.HcVisualPrintProductItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.testouterprint.HcTestOuterPackagePrintMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintCustomerInfoMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintDesignMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintLabelBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.visualprintdesigner.HcVisualPrintProductItemMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.time.LocalDate;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcTestOuterPackagePrintServiceImpl implements HcTestOuterPackagePrintService {

    private static final int MAX_WAIT_PIECES = 10000;
    private static final int MAX_SEGMENT_PIECES = 1000;
    private static final int MAX_SELECTED_PIECES = 100;
    private static final int MAX_CUSTOMER_PRODUCTS = 5000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*\\}\\}");
    private static final Set<String> LABEL_KINDS = Set.of("padBack", "cleanBag", "boxFront", "customerSide");
    private static final Set<String> REQUIRED_VARIABLES = Set.of(
            "productModel", "productTypeName", "productNo", "productInfo", "quantity", "padNo", "batchNo",
            "productionDate", "expirationDate", "pn", "materialDescription", "expDate", "shippingDate");
    private static final Map<String, String> LABEL_KIND_NAMES = Map.of(
            "padBack", "Pad背标",
            "cleanBag", "洁净袋",
            "boxFront", "盒正标",
            "customerSide", "客户侧标");

    @Resource
    private HcTestOuterPackagePrintMapper testOuterPackagePrintMapper;
    @Resource
    private HcVisualPrintCustomerInfoMapper customerInfoMapper;
    @Resource
    private HcVisualPrintProductItemMapper productItemMapper;
    @Resource
    private HcVisualPrintDesignMapper designMapper;
    @Resource
    private HcVisualPrintLabelBindingMapper labelBindingMapper;

    @Override
    public PageResult<TestOuterSegmentRespVO> getWaitSegmentPage(TestOuterWaitSegmentPageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        String segmentBatchNo = normalizeCoaSegmentBatchNo(reqVO.getSegmentBatchNo());
        List<TestOuterPieceRespVO> pieces = testOuterPackagePrintMapper.selectQualifiedWaitPieces(
                keyword, segmentBatchNo, null, MAX_WAIT_PIECES);
        Map<String, List<TestOuterPieceRespVO>> grouped = new LinkedHashMap<>();
        for (TestOuterPieceRespVO piece : pieces) {
            normalizePieceSegment(piece);
            grouped.computeIfAbsent(piece.getSegmentBatchNo(), key -> new ArrayList<>()).add(piece);
        }
        List<TestOuterSegmentRespVO> rows = grouped.values().stream()
                .map(this::buildSegment)
                .sorted(Comparator.comparing(TestOuterSegmentRespVO::getSegmentBatchNo,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    public List<TestOuterPieceRespVO> getWaitSegmentPieceList(TestOuterWaitPieceListReqVO reqVO) {
        String segmentBatchNo = normalizeCoaSegmentBatchNo(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            return List.of();
        }
        List<TestOuterPieceRespVO> rows = testOuterPackagePrintMapper.selectQualifiedWaitPieces(
                StrUtil.trimToNull(reqVO.getKeyword()), segmentBatchNo, null, MAX_SEGMENT_PIECES);
        rows.forEach(this::normalizePieceSegment);
        return rows;
    }

    @Override
    public PageResult<TestOuterCustomerProductRespVO> getCustomerProductPage(TestOuterCustomerProductPageReqVO reqVO) {
        String labelKind = StrUtil.trimToNull(reqVO.getLabelKind());
        if (labelKind != null) {
            labelKind = normalizeLabelKind(labelKind);
        }
        List<TestOuterCustomerProductRespVO> rows = testOuterPackagePrintMapper.selectCustomerProductCandidates(
                StrUtil.trimToNull(reqVO.getKeyword()), labelKind, MAX_CUSTOMER_PRODUCTS);
        return page(rows, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    public List<TestOuterPrintDesignRespVO> getTemplateList(Long customerInfoId, Long productItemId,
                                                            String labelKind) {
        String normalizedKind = normalizeLabelKind(labelKind);
        HcVisualPrintCustomerInfoDO customer = customerInfoMapper.selectById(customerInfoId);
        if (customer == null) {
            throw invalidParamException("客户打印信息不存在");
        }
        HcVisualPrintProductItemDO productItem = productItemMapper.selectByIdAndCustomerInfoId(productItemId,
                customerInfoId);
        if (productItem == null) {
            throw invalidParamException("客户产品不存在或不属于所选客户");
        }

        Map<Long, TestOuterPrintDesignRespVO> result = new LinkedHashMap<>();
        for (HcVisualPrintLabelBindingDO binding : labelBindingMapper.selectListByProductItemId(productItemId)) {
            if (!normalizedKind.equals(binding.getLabelKind()) || !isEnabled(binding.getStatus())
                    || binding.getDesignId() == null) {
                continue;
            }
            addTemplate(result, designMapper.selectById(binding.getDesignId()), customerInfoId, normalizedKind,
                    "产品挂接模板");
        }
        for (HcVisualPrintDesignDO design : designMapper.selectListByInfoId(customerInfoId, productItemId)) {
            addTemplate(result, design, customerInfoId, normalizedKind, "产品专属模板");
        }
        for (HcVisualPrintDesignDO design : designMapper.selectListByInfoId(customerInfoId, null)) {
            addTemplate(result, design, customerInfoId, normalizedKind, "客户共享模板");
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public TestOuterPrintPayloadRespVO buildPrintPayload(Long customerInfoId, Long productItemId, Long designId,
                                                         String labelKind,
                                                         String sliceBatchNos) {
        String normalizedKind = normalizeLabelKind(labelKind);
        if (customerInfoId == null) {
            throw invalidParamException("请选择客户");
        }
        if (productItemId == null) {
            throw invalidParamException("请选择客户产品");
        }
        List<String> selectedSliceBatchNos = parseSliceBatchNos(sliceBatchNos);
        if (selectedSliceBatchNos.isEmpty()) {
            throw invalidParamException("请选择片号");
        }
        if (selectedSliceBatchNos.size() > MAX_SELECTED_PIECES) {
            throw invalidParamException("单次最多打印{}个片号", MAX_SELECTED_PIECES);
        }

        HcVisualPrintCustomerInfoDO customer = customerInfoMapper.selectById(customerInfoId);
        if (customer == null) {
            throw invalidParamException("客户打印信息不存在");
        }
        HcVisualPrintProductItemDO productItem = productItemMapper.selectByIdAndCustomerInfoId(productItemId,
                customerInfoId);
        if (productItem == null) {
            throw invalidParamException("客户产品不存在或不属于所选客户");
        }
        HcVisualPrintDesignDO design = resolveDesign(customerInfoId, productItemId, normalizedKind, designId);
        if (design == null) {
            throw invalidParamException("所选客户产品未维护{}模板，请先在可视化打印设计器保存/挂接模板", labelKindName(normalizedKind));
        }
        Map<String, Object> rendererTemplate = extractRendererTemplate(design);
        Set<String> rendererFieldKeys = extractRendererFieldKeys(rendererTemplate);
        Map<String, Object> rendererBaseData = extractRendererBaseData(rendererTemplate);

        String sliceCsv = String.join(",", selectedSliceBatchNos);
        List<TestOuterPieceRespVO> pieces = testOuterPackagePrintMapper.selectQualifiedWaitPieces(
                null, null, sliceCsv, MAX_SELECTED_PIECES);
        pieces.forEach(this::normalizePieceSegment);
        Map<String, TestOuterPieceRespVO> pieceMap = new LinkedHashMap<>();
        for (TestOuterPieceRespVO piece : pieces) {
            pieceMap.put(piece.getSliceBatchNo(), piece);
        }
        List<String> missing = selectedSliceBatchNos.stream()
                .filter(sliceBatchNo -> !pieceMap.containsKey(sliceBatchNo))
                .toList();
        if (!missing.isEmpty()) {
            throw invalidParamException("以下片号已不在合格待包装范围：{}", String.join("、", missing));
        }

        TestOuterCustomerProductRespVO customerProduct = buildCustomerProduct(customer, productItem, normalizedKind,
                design.getId());
        TestOuterPrintPayloadRespVO respVO = new TestOuterPrintPayloadRespVO();
        respVO.setCustomerProduct(customerProduct);
        respVO.setDesign(toDesignResp(design, "已选择模板"));
        respVO.setRendererTemplate(rendererTemplate);
        List<TestOuterPrintPayloadItemRespVO> items = new ArrayList<>();
        int errorCount = 0;
        int warningCount = 0;
        int total = selectedSliceBatchNos.size();
        for (int i = 0; i < selectedSliceBatchNos.size(); i++) {
            TestOuterPieceRespVO piece = pieceMap.get(selectedSliceBatchNos.get(i));
            TestOuterPrintPayloadItemRespVO item = new TestOuterPrintPayloadItemRespVO();
            item.setRequestId("TEST-OUTER-" + piece.getSliceBatchNo() + "-" + System.currentTimeMillis());
            item.setSliceBatchNo(piece.getSliceBatchNo());
            item.setSegmentBatchNo(piece.getSegmentBatchNo());
            item.setLabelKind(normalizedKind);
            item.setLabelName(labelKindName(normalizedKind));
            item.setTemplateName(firstNotBlank(design.getLabelName(), labelKindName(normalizedKind)));
            Map<String, Object> data = buildDynamicData(customer, productItem, customerProduct, piece,
                    normalizedKind, i + 1, total);
            item.setData(data);
            List<TestOuterVariableCheckRespVO> variableChecks = buildVariableChecks(rendererFieldKeys,
                    rendererBaseData, data);
            item.setVariableChecks(variableChecks);
            errorCount += (int) variableChecks.stream().filter(check -> "ERROR".equals(check.getStatus())).count();
            warningCount += (int) variableChecks.stream().filter(check -> "WARNING".equals(check.getStatus())).count();
            items.add(item);
        }
        respVO.setItems(items);
        respVO.setErrorCount(errorCount);
        respVO.setWarningCount(warningCount);
        return respVO;
    }

    private TestOuterSegmentRespVO buildSegment(List<TestOuterPieceRespVO> pieces) {
        TestOuterSegmentRespVO segment = new TestOuterSegmentRespVO();
        TestOuterPieceRespVO first = pieces.get(0);
        segment.setSegmentBatchNo(first.getSegmentBatchNo());
        segment.setPieceCount(pieces.size());
        segment.setMaterialCode(firstNotBlank(first.getMaterialCode()));
        segment.setMaterialName(firstNotBlank(first.getMaterialName()));
        segment.setModelCode(firstNotBlank(first.getModelCode()));
        segment.setPackagingQualityStatus("OK");
        segment.setProductionDateStart(pieces.stream()
                .map(TestOuterPieceRespVO::getProductionDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null));
        segment.setProductionDateEnd(pieces.stream()
                .map(TestOuterPieceRespVO::getProductionDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(null));
        segment.setExpiryDate(pieces.stream()
                .map(TestOuterPieceRespVO::getExpiryDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(null));
        List<String> samples = pieces.stream()
                .map(TestOuterPieceRespVO::getSliceBatchNo)
                .filter(StrUtil::isNotBlank)
                .limit(3)
                .toList();
        segment.setSampleSliceBatchNos(samples);
        segment.setSampleSliceBatchNo(samples.isEmpty() ? null : samples.get(0));
        return segment;
    }

    private void normalizePieceSegment(TestOuterPieceRespVO piece) {
        String segmentBatchNo = normalizeCoaSegmentBatchNo(piece.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            segmentBatchNo = normalizeCoaSegmentBatchNo(firstNotBlank(piece.getParentProductionBatchNo(),
                    piece.getProductionBatchNo(), piece.getSliceBatchNo()));
        }
        piece.setSegmentBatchNo(segmentBatchNo);
        piece.setPackagingQualityStatus("OK");
    }

    private HcVisualPrintDesignDO resolveDesign(Long customerInfoId, Long productItemId, String labelKind,
                                                Long designId) {
        List<TestOuterPrintDesignRespVO> templates = getTemplateList(customerInfoId, productItemId, labelKind);
        if (templates.isEmpty()) {
            return null;
        }
        Long resolvedDesignId = designId == null ? templates.get(0).getId() : designId;
        boolean eligible = templates.stream().anyMatch(template -> Objects.equals(template.getId(), resolvedDesignId));
        if (!eligible) {
            throw invalidParamException("所选模板不存在、已停用或未挂接到当前客户产品");
        }
        return designMapper.selectById(resolvedDesignId);
    }

    private void addTemplate(Map<Long, TestOuterPrintDesignRespVO> result, HcVisualPrintDesignDO design,
                             Long customerInfoId, String labelKind, String templateSource) {
        if (design == null || design.getId() == null || !Objects.equals(customerInfoId, design.getCustomerInfoId())
                || !labelKind.equals(design.getLabelKind()) || !isEnabled(design.getStatus())) {
            return;
        }
        result.putIfAbsent(design.getId(), toDesignResp(design, templateSource));
    }

    private boolean isEnabled(Integer status) {
        return status == null || status == 0;
    }

    private Map<String, Object> extractRendererTemplate(HcVisualPrintDesignDO design) {
        if (StrUtil.isBlank(design.getDesignJson())) {
            throw invalidParamException("模板{}未保存设计JSON", firstNotBlank(design.getLabelName(), design.getId()));
        }
        Map<String, Object> draft = JsonUtils.parseObject(design.getDesignJson(),
                new TypeReference<Map<String, Object>>() {});
        Object rendererTemplate = draft == null ? null : draft.get("rendererTemplate");
        if (!(rendererTemplate instanceof Map<?, ?> templateMap)) {
            throw invalidParamException("模板{}缺少rendererTemplate，请在可视化打印设计器打开后重新保存一次",
                    firstNotBlank(design.getLabelName(), design.getId()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        templateMap.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private Set<String> extractRendererFieldKeys(Map<String, Object> rendererTemplate) {
        Set<String> result = new LinkedHashSet<>();
        collectRendererFieldKeys(rendererTemplate.get("elements"), result);
        return result;
    }

    private void collectRendererFieldKeys(Object node, Set<String> result) {
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(value -> collectRendererFieldKeys(value, result));
            return;
        }
        if (node instanceof Iterable<?> iterable) {
            iterable.forEach(value -> collectRendererFieldKeys(value, result));
            return;
        }
        if (!(node instanceof String text)) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
    }

    private Map<String, Object> extractRendererBaseData(Map<String, Object> rendererTemplate) {
        Object data = rendererTemplate.get("data");
        if (!(data instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private List<TestOuterVariableCheckRespVO> buildVariableChecks(Set<String> fieldKeys,
                                                                    Map<String, Object> rendererBaseData,
                                                                    Map<String, Object> requestData) {
        Map<String, Object> mergedData = new LinkedHashMap<>(rendererBaseData);
        mergedData.putAll(requestData);
        List<TestOuterVariableCheckRespVO> result = new ArrayList<>();
        for (String fieldKey : fieldKeys) {
            ResolvedVariable resolved = resolveVariable(mergedData, fieldKey);
            boolean required = REQUIRED_VARIABLES.contains(fieldKey);
            TestOuterVariableCheckRespVO check = new TestOuterVariableCheckRespVO();
            check.setFieldKey(fieldKey);
            check.setFieldLabel(variableLabel(fieldKey));
            check.setSource(variableSource(fieldKey));
            check.setRequired(required);
            check.setValue(resolved.value());
            if (!resolved.exists()) {
                check.setStatus("ERROR");
                check.setMessage("模板引用的变量未提供");
            } else if (StrUtil.isBlank(resolved.value() == null ? null : String.valueOf(resolved.value()))) {
                check.setStatus(required ? "ERROR" : "WARNING");
                check.setMessage(required ? "必填变量缺少值" : "可选变量为空，打印时留白");
            } else {
                check.setStatus("OK");
                check.setMessage("已匹配");
            }
            result.add(check);
        }
        return result;
    }

    private ResolvedVariable resolveVariable(Map<String, Object> data, String fieldKey) {
        Object current = data;
        for (String part : fieldKey.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap) || !currentMap.containsKey(part)) {
                return new ResolvedVariable(false, null);
            }
            current = currentMap.get(part);
        }
        return new ResolvedVariable(true, current);
    }

    private String variableLabel(String fieldKey) {
        return switch (fieldKey) {
            case "serialNo" -> "客户序号";
            case "customer" -> "客户";
            case "productType", "productTypeName" -> "客户产品类型";
            case "productModel" -> "产品型号";
            case "sizeMm" -> "产品尺寸";
            case "productNo" -> "产品编码";
            case "productInfo" -> "产品信息";
            case "quantity" -> "数量";
            case "padNo", "sliceBatchNo" -> "片号";
            case "batchNo", "segmentBatchNo" -> "生产批号/段号";
            case "productionDate" -> "生产日期";
            case "expirationDate", "expDate" -> "失效日期";
            case "shippingDate" -> "发货日期";
            case "plant" -> "Plant";
            case "pn" -> "PN";
            case "materialDescription" -> "物料描述";
            case "vendorPn" -> "客户物料号";
            case "info" -> "INFO";
            case "packageIndex" -> "包装序号/总数";
            case "purUom" -> "采购单位";
            case "po" -> "采购订单号";
            default -> fieldKey;
        };
    }

    private String variableSource(String fieldKey) {
        return switch (fieldKey) {
            case "serialNo", "customer", "productType", "productTypeName", "sizeMm", "customerSideSize",
                    "customerSideMethod", "shippingMethod", "needPaperCoa", "needEcoa", "hasMark",
                    "deliveryNote", "shipmentFilePackageMethod", "customerSideTemplate", "specialRemark" ->
                    "客户产品配置";
            case "productModel", "productNo", "padNo", "batchNo", "sliceBatchNo", "segmentBatchNo", "pn",
                    "materialDescription", "productionDate", "expirationDate", "expDate", "sourceType",
                    "inspectionResult", "coaInspectionResult", "packagingQualityStatus" -> "MES片号数据";
            case "quantity", "packageIndex", "shippingDate", "labelKind", "labelName" -> "本次打印上下文";
            case "plant", "vendorPn", "info", "purUom", "po" -> "待维护/本次补充";
            default -> "打印数据";
        };
    }

    private Map<String, Object> buildDynamicData(HcVisualPrintCustomerInfoDO customer,
                                                 HcVisualPrintProductItemDO productItem,
                                                 TestOuterCustomerProductRespVO customerProduct,
                                                 TestOuterPieceRespVO piece,
                                                 String labelKind,
                                                 int packageIndex,
                                                 int total) {
        String productType = firstNotBlank(productItem.getProductType(), customer.getProductType());
        String sizeMm = firstNotBlank(productItem.getSizeMm(), customer.getSizeMm());
        Map<String, Object> data = new LinkedHashMap<>();
        put(data, "serialNo", customer.getSerialNo());
        put(data, "customer", customer.getCustomer());
        put(data, "productType", productType);
        put(data, "productTypeName", productType);
        put(data, "productModel", firstNotBlank(piece.getModelCode(), productType));
        put(data, "sizeMm", sizeMm);
        put(data, "customerSideSize", customer.getCustomerSideSize());
        put(data, "customerSideMethod", customer.getCustomerSideMethod());
        put(data, "shippingMethod", customer.getShippingMethod());
        put(data, "needPaperCoa", customer.getNeedPaperCoa());
        put(data, "needEcoa", customer.getNeedEcoa());
        put(data, "hasMark", customer.getHasMark());
        put(data, "deliveryNote", customer.getDeliveryNote());
        put(data, "shipmentFilePackageMethod", customer.getShipmentFilePackageMethod());
        put(data, "customerSideTemplate", customer.getCustomerSideTemplate());
        put(data, "specialRemark", customer.getSpecialRemark());
        put(data, "productNo", piece.getMaterialCode());
        put(data, "productInfo", firstNotBlank(piece.getModelCode(), productType) + " " + firstNotBlank(sizeMm));
        put(data, "quantity", "1");
        put(data, "padNo", piece.getSliceBatchNo());
        put(data, "batchNo", firstNotBlank(piece.getSegmentBatchNo(), piece.getParentProductionBatchNo()));
        put(data, "sliceBatchNo", piece.getSliceBatchNo());
        put(data, "segmentBatchNo", piece.getSegmentBatchNo());
        put(data, "productionDate", formatDate(piece.getProductionDate()));
        put(data, "expirationDate", formatDate(piece.getExpiryDate()));
        put(data, "expDate", formatDate(piece.getExpiryDate()));
        put(data, "shippingDate", formatDate(LocalDate.now()));
        put(data, "plant", "");
        put(data, "pn", piece.getMaterialCode());
        put(data, "materialDescription", firstNotBlank(piece.getMaterialName(), piece.getModelCode(), productType));
        put(data, "vendorPn", "");
        put(data, "info", "");
        put(data, "packageIndex", packageIndex + "/" + total);
        put(data, "purUom", "");
        put(data, "po", "");
        put(data, "labelKind", labelKind);
        put(data, "labelName", labelKindName(labelKind));
        put(data, "sourceType", piece.getSourceType());
        put(data, "inspectionResult", piece.getInspectionResult());
        put(data, "coaInspectionResult", piece.getCoaInspectionResult());
        put(data, "packagingQualityStatus", piece.getPackagingQualityStatus());
        put(data, "labelImageId", customerProduct.getLabelImageId());
        put(data, "labelImageFile", customerProduct.getLabelImageFile());
        putAllLabelImages(data, customer, productItem);
        return data;
    }

    private void putAllLabelImages(Map<String, Object> data, HcVisualPrintCustomerInfoDO customer,
                                   HcVisualPrintProductItemDO item) {
        put(data, "padBackLabelImageId", firstNotBlank(item.getPadBackLabelImageId(), customer.getPadBackLabelImageId()));
        put(data, "padBackLabelImageFile", firstNotBlank(item.getPadBackLabelImageFile(), customer.getPadBackLabelImageFile()));
        put(data, "cleanBagLabelImageId", firstNotBlank(item.getCleanBagLabelImageId(), customer.getCleanBagLabelImageId()));
        put(data, "cleanBagLabelImageFile", firstNotBlank(item.getCleanBagLabelImageFile(), customer.getCleanBagLabelImageFile()));
        put(data, "boxFrontLabelImageId", firstNotBlank(item.getBoxFrontLabelImageId(), customer.getBoxFrontLabelImageId()));
        put(data, "boxFrontLabelImageFile", firstNotBlank(item.getBoxFrontLabelImageFile(), customer.getBoxFrontLabelImageFile()));
        put(data, "customerSideLabelImageId", firstNotBlank(item.getCustomerSideLabelImageId(), customer.getCustomerSideLabelImageId()));
        put(data, "customerSideLabelImageFile", firstNotBlank(item.getCustomerSideLabelImageFile(), customer.getCustomerSideLabelImageFile()));
    }

    private TestOuterCustomerProductRespVO buildCustomerProduct(HcVisualPrintCustomerInfoDO customer,
                                                                HcVisualPrintProductItemDO productItem,
                                                                String labelKind,
                                                                Long designId) {
        TestOuterCustomerProductRespVO respVO = new TestOuterCustomerProductRespVO();
        respVO.setRowKey(customer.getId() + "-" + productItem.getId());
        respVO.setCustomerInfoId(customer.getId());
        respVO.setProductItemId(productItem.getId());
        respVO.setSerialNo(customer.getSerialNo());
        respVO.setSourceRow(productItem.getSourceRow());
        respVO.setCustomer(customer.getCustomer());
        respVO.setProductType(firstNotBlank(productItem.getProductType(), customer.getProductType()));
        respVO.setSizeMm(firstNotBlank(productItem.getSizeMm(), customer.getSizeMm()));
        respVO.setDesignId(designId);
        respVO.setLabelImageId(resolveLabelImageId(customer, productItem, labelKind));
        respVO.setLabelImageFile(resolveLabelImageFile(customer, productItem, labelKind));
        respVO.setCustomerSideSize(customer.getCustomerSideSize());
        respVO.setCustomerSideMethod(customer.getCustomerSideMethod());
        respVO.setShippingMethod(customer.getShippingMethod());
        respVO.setNeedPaperCoa(customer.getNeedPaperCoa());
        respVO.setNeedEcoa(customer.getNeedEcoa());
        respVO.setHasMark(customer.getHasMark());
        respVO.setDeliveryNote(customer.getDeliveryNote());
        respVO.setShipmentFilePackageMethod(customer.getShipmentFilePackageMethod());
        respVO.setCustomerSideTemplate(customer.getCustomerSideTemplate());
        respVO.setSpecialRemark(customer.getSpecialRemark());
        return respVO;
    }

    private TestOuterPrintDesignRespVO toDesignResp(HcVisualPrintDesignDO design, String templateSource) {
        TestOuterPrintDesignRespVO respVO = new TestOuterPrintDesignRespVO();
        respVO.setId(design.getId());
        respVO.setLabelKind(design.getLabelKind());
        respVO.setLabelName(design.getLabelName());
        respVO.setTemplateSource(templateSource);
        respVO.setWidthMm(design.getWidthMm());
        respVO.setHeightMm(design.getHeightMm());
        respVO.setDpi(design.getDpi());
        respVO.setImageId(design.getImageId());
        respVO.setImageFile(design.getImageFile());
        respVO.setDesignJson(design.getDesignJson());
        return respVO;
    }

    private List<String> parseSliceBatchNos(String sliceBatchNos) {
        Set<String> values = new LinkedHashSet<>();
        for (String value : StrUtil.splitToArray(sliceBatchNos, ',')) {
            String sliceBatchNo = StrUtil.trimToNull(value);
            if (sliceBatchNo != null) {
                values.add(sliceBatchNo);
            }
        }
        return new ArrayList<>(values);
    }

    private String normalizeLabelKind(String labelKind) {
        String value = StrUtil.trim(labelKind);
        if (!LABEL_KINDS.contains(value)) {
            throw invalidParamException("标签类型不正确：{}", labelKind);
        }
        return value;
    }

    private String labelKindName(String labelKind) {
        return LABEL_KIND_NAMES.getOrDefault(labelKind, labelKind);
    }

    private String normalizeCoaSegmentBatchNo(String batchNo) {
        String value = StrUtil.trimToEmpty(batchNo).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(value)) {
            return "";
        }
        value = value.replaceFirst("-J\\d+$", "");
        value = value.replaceFirst("-S\\d+$", "");
        value = value.replaceFirst("^(.+[PQRS])\\d{3}[A-Z]?$", "$1");
        return value;
    }

    private <T> PageResult<T> page(List<T> rows, Integer pageNo, Integer pageSize) {
        int currentPageNo = pageNo == null ? 1 : Math.max(1, pageNo);
        int currentPageSize = pageSize == null ? 10 : Math.min(200, Math.max(1, pageSize));
        int fromIndex = Math.min(Math.max(0, (currentPageNo - 1) * currentPageSize), rows.size());
        int toIndex = Math.min(fromIndex + currentPageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private String resolveLabelImageId(HcVisualPrintCustomerInfoDO customer, HcVisualPrintProductItemDO item,
                                       String labelKind) {
        return switch (labelKind) {
            case "padBack" -> firstNotBlank(item.getPadBackLabelImageId(), customer.getPadBackLabelImageId());
            case "cleanBag" -> firstNotBlank(item.getCleanBagLabelImageId(), customer.getCleanBagLabelImageId());
            case "boxFront" -> firstNotBlank(item.getBoxFrontLabelImageId(), customer.getBoxFrontLabelImageId());
            case "customerSide" -> firstNotBlank(item.getCustomerSideLabelImageId(),
                    customer.getCustomerSideLabelImageId());
            default -> "";
        };
    }

    private String resolveLabelImageFile(HcVisualPrintCustomerInfoDO customer, HcVisualPrintProductItemDO item,
                                         String labelKind) {
        return switch (labelKind) {
            case "padBack" -> firstNotBlank(item.getPadBackLabelImageFile(), customer.getPadBackLabelImageFile());
            case "cleanBag" -> firstNotBlank(item.getCleanBagLabelImageFile(), customer.getCleanBagLabelImageFile());
            case "boxFront" -> firstNotBlank(item.getBoxFrontLabelImageFile(), customer.getBoxFrontLabelImageFile());
            case "customerSide" -> firstNotBlank(item.getCustomerSideLabelImageFile(),
                    customer.getCustomerSideLabelImageFile());
            default -> "";
        };
    }

    private void put(Map<String, Object> data, String key, Object value) {
        data.put(key, value == null ? "" : value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private String firstNotBlank(Object... values) {
        if (values == null) {
            return "";
        }
        for (Object value : values) {
            String text = value == null ? "" : String.valueOf(value);
            if (StrUtil.isNotBlank(text)) {
                return text;
            }
        }
        return "";
    }

    private record ResolvedVariable(boolean exists, Object value) {
    }

}
