package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCandidateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskItemSelectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskResultItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskWizardCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetCellValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSheetCellValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_REQUEST_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_STANDARD_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_TYPE_NOT_SUPPORTED;

/**
 * 质量任务专用检验单生成层。原有五类检验录入 Service 不参与修改。
 */
@Service
@Validated
public class QmsDispatchTaskInspectionService {

    private static final String PENDING = "PENDING";
    private static final String REJECTED = "REJECTED";
    private static final String TASK_SOURCE = "QUALITY_TASK_CENTER";
    private static final String GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_DATE = "DATE";
    private static final String IQC_MEASURED_DATA_RENDER_TYPE = "IQC_SAMPLE_VALUES";
    private static final Integer STANDARD_STATUS_ENABLED = 1;
    private static final Integer STANDARD_AUDIT_STATUS_AUDITED = 20;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Resource private QmsIqcOrderMapper iqcOrderMapper;
    @Resource private QmsIqcItemMapper iqcItemMapper;
    @Resource private QmsIqcSampleMapper iqcSampleMapper;
    @Resource private QmsFaiOrderMapper faiOrderMapper;
    @Resource private QmsFaiItemMapper faiItemMapper;
    @Resource private QmsFaiSampleMapper faiSampleMapper;
    @Resource private QmsIpqcOrderMapper ipqcOrderMapper;
    @Resource private QmsIpqcItemMapper ipqcItemMapper;
    @Resource private QmsIpqcSampleMapper ipqcSampleMapper;
    @Resource private QmsFqcOrderMapper fqcOrderMapper;
    @Resource private QmsFqcItemMapper fqcItemMapper;
    @Resource private QmsFqcSampleMapper fqcSampleMapper;
    @Resource private QmsFqcSheetCellValueMapper fqcSheetCellValueMapper;
    @Resource private QmsFqcShippingDetailMapper fqcShippingDetailMapper;
    @Resource private QmsOqcOrderMapper oqcOrderMapper;
    @Resource private QmsOqcItemMapper oqcItemMapper;
    @Resource private QmsOqcSampleMapper oqcSampleMapper;
    @Resource private QmsQualityStandardMapper standardMapper;
    @Resource private QmsQualityStandardItemMapper standardItemMapper;
    @Resource private QmsNoGeneratorService noGeneratorService;

    public PageResult<QmsDispatchTaskSourceRespVO> getSourcePage(QmsDispatchTaskSourcePageReqVO reqVO) {
        return switch (normalizeType(reqVO.getCheckType())) {
            case "IQC" -> mapPage(selectIqcPage(reqVO), this::toSource);
            case "FAI" -> mapPage(selectFaiPage(reqVO), this::toSource);
            case GLUE_BOARD_FAI -> mapPage(selectGlueBoardFaiPage(reqVO), this::toGlueBoardSource);
            case "IPQC" -> mapPage(selectIpqcPage(reqVO), this::toSource);
            case "FQC" -> mapPage(selectFqcPage(reqVO), this::toSource);
            case "OQC" -> mapPage(selectShippingFqcPage(reqVO), this::toShippingSource);
            default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, reqVO.getCheckType());
        };
    }

    public List<QmsDispatchTaskCandidateItemRespVO> getSourceItems(String checkType, Long executionId) {
        return switch (normalizeType(checkType)) {
            case "IQC" -> buildIqcSourceItems(executionId);
            case "FAI" -> {
                requiredFaiOrder(executionId, "FAI");
                yield faiItemMapper.selectListByFaiId(executionId).stream().map(this::toCandidate).toList();
            }
            case GLUE_BOARD_FAI -> {
                requiredFaiOrder(executionId, GLUE_BOARD_FAI);
                yield faiItemMapper.selectListByFaiId(executionId).stream().map(this::toCandidate).toList();
            }
            case "IPQC" -> ipqcItemMapper.selectListByIpqcId(executionId).stream().map(this::toCandidate).toList();
            case "FQC" -> fqcItemMapper.selectListByFqcId(executionId).stream().map(this::toCandidate).toList();
            case "OQC" -> isShippingFqcExecution(executionId)
                    ? fqcItemMapper.selectListByFqcId(executionId).stream().map(this::toCandidate).toList()
                    : oqcItemMapper.selectListByOqcId(executionId).stream().map(this::toCandidate).toList();
            default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
        };
    }

    public List<QmsDispatchTaskCandidateItemRespVO> getSourceItemTree(String checkType, Long executionId) {
        return switch (normalizeType(checkType)) {
            case "FAI", GLUE_BOARD_FAI -> buildFaiItemTree(checkType, executionId);
            case "FQC" -> buildFqcItemTree(executionId);
            case "OQC" -> isShippingFqcExecution(executionId)
                    ? buildFqcItemTree(executionId)
                    : buildOqcItemTree(executionId);
            default -> getSourceItems(checkType, executionId).stream()
                    .peek(this::markItemNode).toList();
        };
    }

    public PageResult<QmsDispatchTaskStandardRespVO> getStandardPage(QmsDispatchTaskStandardPageReqVO reqVO) {
        String checkType = normalizeType(reqVO.getCheckType());
        LambdaQueryWrapperX<QmsQualityStandardDO> query = new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getStatus, STANDARD_STATUS_ENABLED)
                .eq(QmsQualityStandardDO::getAuditStatus, STANDARD_AUDIT_STATUS_AUDITED)
                .eq(QmsQualityStandardDO::getApplyType, checkType);
        String productModel = StrUtil.trimToEmpty(reqVO.getProductModel());
        if (StrUtil.isNotBlank(productModel)) {
            query.and(wrapper -> wrapper.like(QmsQualityStandardDO::getProductModelCode, productModel)
                    .or().like(QmsQualityStandardDO::getProductModelName, productModel)
                    .or().like(QmsQualityStandardDO::getGlueBoardModel, productModel)
                    .or().like(QmsQualityStandardDO::getMaterialCode, productModel)
                    .or().like(QmsQualityStandardDO::getMaterialName, productModel));
        }
        String operationCode = StrUtil.trimToEmpty(reqVO.getOperationCode());
        String operationName = StrUtil.trimToEmpty(reqVO.getOperationName());
        if (StrUtil.isNotBlank(operationCode) && StrUtil.isNotBlank(operationName)) {
            query.and(wrapper -> wrapper.like(QmsQualityStandardDO::getProcessCode, operationCode)
                    .or().like(QmsQualityStandardDO::getProcessName, operationName));
        } else if (StrUtil.isNotBlank(operationCode)) {
            query.and(wrapper -> wrapper.like(QmsQualityStandardDO::getProcessCode, operationCode)
                    .or().like(QmsQualityStandardDO::getProcessName, operationCode));
        } else if (StrUtil.isNotBlank(operationName)) {
            query.and(wrapper -> wrapper.like(QmsQualityStandardDO::getProcessName, operationName)
                    .or().like(QmsQualityStandardDO::getProcessCode, operationName));
        }
        if (StrUtil.isNotBlank(reqVO.getStandardName())) {
            query.and(wrapper -> wrapper.like(QmsQualityStandardDO::getStandardName, reqVO.getStandardName())
                    .or().like(QmsQualityStandardDO::getStandardNo, reqVO.getStandardName()));
        }
        query.orderByDesc(QmsQualityStandardDO::getId);
        PageResult<QmsQualityStandardDO> page = standardMapper.selectPage(reqVO, query);
        return mapPage(page, standard -> {
            QmsDispatchTaskStandardRespVO result = BeanUtils.toBean(standard, QmsDispatchTaskStandardRespVO.class);
            result.setOperationCode(standard.getProcessCode());
            result.setOperationName(standard.getProcessName());
            result.setGlueBoardModel(standard.getGlueBoardModel());
            result.setItemCount(standardItemMapper.selectListByStandardId(standard.getId()).size());
            return result;
        });
    }

    public List<QmsDispatchTaskCandidateItemRespVO> getStandardItems(Long standardId) {
        QmsQualityStandardDO standard = validateStandard(standardId);
        List<QmsDispatchTaskCandidateItemRespVO> items = standardItemMapper.selectListByStandardId(standard.getId())
                .stream().map(this::toCandidate).toList();
        if (Set.of("FAI", GLUE_BOARD_FAI).contains(normalizeType(standard.getApplyType()))) {
            items.forEach(this::attachStandardPositionNodes);
        }
        return items;
    }

    private List<QmsDispatchTaskCandidateItemRespVO> buildFaiItemTree(String checkType, Long executionId) {
        requiredFaiOrder(executionId, checkType);
        List<QmsFaiItemDO> sourceItems = faiItemMapper.selectListByFaiId(executionId);
        Map<Long, List<QmsFaiSampleDO>> sampleMap = faiSampleMapper.selectListByFaiId(executionId).stream()
                .collect(Collectors.groupingBy(QmsFaiSampleDO::getFaiItemId));
        List<QmsDispatchTaskCandidateItemRespVO> result = new ArrayList<>();
        for (QmsFaiItemDO sourceItem : sourceItems) {
            QmsDispatchTaskCandidateItemRespVO itemNode = toCandidate(sourceItem);
            List<QmsFaiSampleDO> samples = sampleMap.getOrDefault(sourceItem.getId(), List.of());
            Map<String, List<QmsFaiSampleDO>> positionMap = samples.stream()
                    .collect(Collectors.groupingBy(this::faiPositionKey));
            List<QmsDispatchTaskCandidateItemRespVO> positions = positionMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> buildFaiPositionNode(sourceItem, entry.getKey(), entry.getValue()))
                    .toList();
            itemNode.setChildren(positions);
            result.add(itemNode);
        }
        return result;
    }

    private QmsDispatchTaskCandidateItemRespVO buildFaiPositionNode(QmsFaiItemDO item, String position,
                                                                     List<QmsFaiSampleDO> samples) {
        QmsDispatchTaskCandidateItemRespVO node = new QmsDispatchTaskCandidateItemRespVO();
        node.setId(item.getId());
        node.setSourceItemId(item.getId());
        node.setStandardItemId(item.getStandardItemId());
        node.setNodeKey("POSITION:" + item.getId() + ":" + position);
        node.setNodeType("POSITION");
        node.setSelectable(true);
        node.setPositionCode(position);
        node.setPositionName(position);
        node.setInspectionItem(item.getInspectionItem());
        node.setStandardDesc(item.getStandardDesc());
        node.setUnit(item.getUnit());
        List<BigDecimal> values = samples.stream().map(this::faiSampleValue).filter(Objects::nonNull).toList();
        node.setAverageValue(average(values));
        node.setStandardDeviation(standardDeviation(values));
        node.setCurrentResult(resolveJudgment(samples, QmsFaiSampleDO::getSampleResult));
        node.setResult(node.getCurrentResult());
        node.setChildren(samples.stream()
                .sorted(Comparator.comparing(QmsFaiSampleDO::getSampleGroupNo,
                        Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QmsFaiSampleDO::getSampleSeq, Comparator.nullsLast(Integer::compareTo)))
                .map(sample -> buildFaiGroupNode(item, position, sample)).toList());
        return node;
    }

    private QmsDispatchTaskCandidateItemRespVO buildFaiGroupNode(QmsFaiItemDO item, String position,
                                                                  QmsFaiSampleDO sample) {
        int groupNo = sample.getSampleGroupNo() == null
                ? (sample.getSampleSeq() == null ? 1 : sample.getSampleSeq()) : sample.getSampleGroupNo();
        QmsDispatchTaskCandidateItemRespVO node = new QmsDispatchTaskCandidateItemRespVO();
        node.setId(sample.getId());
        node.setSourceItemId(item.getId());
        node.setStandardItemId(item.getStandardItemId());
        node.setNodeKey("GROUP:" + item.getId() + ":" + position + ":" + groupNo + ":" + sample.getId());
        node.setNodeType("GROUP");
        node.setSelectable(false);
        node.setPositionCode(position);
        node.setPositionName(position);
        node.setSampleGroupNo(groupNo);
        node.setInspectionItem("第" + groupNo + "组");
        node.setMeasuredValue(faiMeasuredValue(sample));
        node.setResultValue(sample.getResultValue());
        node.setCurrentResult(sample.getSampleResult());
        node.setResult(sample.getSampleResult());
        node.setUnit(item.getUnit());
        return node;
    }

    private List<QmsDispatchTaskCandidateItemRespVO> buildFqcItemTree(Long executionId) {
        required(fqcOrderMapper.selectById(executionId), "FQC", executionId);
        List<QmsFqcItemDO> sourceItems = fqcItemMapper.selectListByFqcId(executionId);
        Map<Long, List<QmsFqcSampleDO>> sampleMap = fqcSampleMapper.selectListByFqcId(executionId).stream()
                .collect(Collectors.groupingBy(QmsFqcSampleDO::getFqcItemId));
        Map<String, List<QmsFqcItemDO>> itemGroups = sourceItems.stream()
                .sorted(Comparator.comparing(QmsFqcItemDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QmsFqcItemDO::getId))
                .collect(Collectors.groupingBy(this::fqcItemGroupKey, LinkedHashMap::new, Collectors.toList()));
        List<QmsDispatchTaskCandidateItemRespVO> result = new ArrayList<>();
        for (List<QmsFqcItemDO> itemGroup : itemGroups.values()) {
            QmsFqcItemDO sourceItem = itemGroup.get(0);
            List<Long> sourceItemIds = itemGroup.stream().map(QmsFqcItemDO::getId).toList();
            QmsDispatchTaskCandidateItemRespVO itemNode = toCandidate(sourceItem);
            itemNode.setSourceItemIds(sourceItemIds);
            itemNode.setNodeKey("ITEM_GROUP:" + sourceItemIds.stream()
                    .map(String::valueOf).collect(Collectors.joining("_")));
            Map<String, List<QmsFqcSampleDO>> pieceMap = sourceItemIds.stream()
                    .flatMap(itemId -> sampleMap.getOrDefault(itemId, List.of()).stream())
                    .collect(Collectors.groupingBy(this::fqcPieceKey, LinkedHashMap::new, Collectors.toList()));
            itemNode.setChildren(pieceMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(entry -> buildFqcPieceNode(sourceItem, entry.getKey(), entry.getValue())).toList());
            int okPieceCount = (int) itemNode.getChildren().stream()
                    .filter(child -> "OK".equals(normalizeType(child.getCurrentResult()))).count();
            int ngPieceCount = (int) itemNode.getChildren().stream()
                    .filter(child -> "NG".equals(normalizeType(child.getCurrentResult()))).count();
            itemNode.setPieceCount(itemNode.getChildren().size());
            itemNode.setOkPieceCount(okPieceCount);
            itemNode.setNgPieceCount(ngPieceCount);
            itemNode.setPendingPieceCount(itemNode.getPieceCount() - okPieceCount - ngPieceCount);
            itemNode.setCurrentResult(ngPieceCount > 0 ? "NG"
                    : itemNode.getPendingPieceCount() > 0 ? PENDING : "OK");
            itemNode.setResult(itemNode.getCurrentResult());
            result.add(itemNode);
        }
        return result;
    }

    private QmsDispatchTaskCandidateItemRespVO buildFqcPieceNode(QmsFqcItemDO item, String pieceNo,
                                                                  List<QmsFqcSampleDO> samples) {
        QmsDispatchTaskCandidateItemRespVO node = new QmsDispatchTaskCandidateItemRespVO();
        List<Long> sourceItemIds = samples.stream()
                .map(QmsFqcSampleDO::getFqcItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        node.setId(samples.isEmpty() ? item.getId() : samples.get(0).getId());
        node.setSourceItemId(sourceItemIds.isEmpty() ? item.getId() : sourceItemIds.get(0));
        node.setSourceItemIds(sourceItemIds.isEmpty() ? List.of(item.getId()) : sourceItemIds);
        node.setStandardItemId(item.getStandardItemId());
        node.setNodeKey("PIECE:" + (sourceItemIds.isEmpty() ? item.getId() : sourceItemIds.stream()
                .map(String::valueOf).collect(Collectors.joining("_"))) + ":" + pieceNo);
        node.setNodeType("PIECE");
        node.setSelectable(true);
        node.setPieceNo(pieceNo);
        node.setInspectionItem(item.getInspectionItem());
        node.setStandardDesc(item.getStandardDesc());
        node.setUnit(item.getUnit());
        node.setCurrentResult(resolveJudgment(samples, QmsFqcSampleDO::getSampleResult));
        node.setResult(node.getCurrentResult());
        node.setDefectCode(joinDistinct(samples.stream().map(QmsFqcSampleDO::getDefectCode).toList()));
        node.setDefectName(joinDistinct(samples.stream().map(QmsFqcSampleDO::getDefectName).toList()));
        return node;
    }

    private List<QmsDispatchTaskCandidateItemRespVO> buildOqcItemTree(Long executionId) {
        required(oqcOrderMapper.selectById(executionId), "OQC", executionId);
        List<QmsOqcItemDO> sourceItems = oqcItemMapper.selectListByOqcId(executionId);
        Map<Long, List<QmsOqcSampleDO>> sampleMap = oqcSampleMapper.selectListByOqcId(executionId).stream()
                .collect(Collectors.groupingBy(QmsOqcSampleDO::getOqcItemId));
        List<QmsDispatchTaskCandidateItemRespVO> result = new ArrayList<>();
        for (QmsOqcItemDO sourceItem : sourceItems) {
            QmsDispatchTaskCandidateItemRespVO itemNode = toCandidate(sourceItem);
            markItemNode(itemNode);
            Map<String, List<QmsOqcSampleDO>> pieceMap = sampleMap.getOrDefault(sourceItem.getId(), List.of())
                    .stream()
                    .collect(Collectors.groupingBy(this::oqcPieceKey, LinkedHashMap::new, Collectors.toList()));
            itemNode.setChildren(pieceMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(entry -> buildOqcPieceNode(sourceItem, entry.getKey(), entry.getValue())).toList());
            int okPieceCount = (int) itemNode.getChildren().stream()
                    .filter(child -> "OK".equals(normalizeType(child.getCurrentResult()))).count();
            int ngPieceCount = (int) itemNode.getChildren().stream()
                    .filter(child -> "NG".equals(normalizeType(child.getCurrentResult()))).count();
            itemNode.setPieceCount(itemNode.getChildren().size());
            itemNode.setOkPieceCount(okPieceCount);
            itemNode.setNgPieceCount(ngPieceCount);
            itemNode.setPendingPieceCount(itemNode.getPieceCount() - okPieceCount - ngPieceCount);
            result.add(itemNode);
        }
        return result;
    }

    private QmsDispatchTaskCandidateItemRespVO buildOqcPieceNode(QmsOqcItemDO item, String pieceNo,
                                                                  List<QmsOqcSampleDO> samples) {
        QmsDispatchTaskCandidateItemRespVO node = new QmsDispatchTaskCandidateItemRespVO();
        node.setId(samples.isEmpty() ? item.getId() : samples.get(0).getId());
        node.setSourceItemId(item.getId());
        node.setSourceItemIds(List.of(item.getId()));
        node.setStandardItemId(item.getStandardItemId());
        node.setNodeKey("PIECE:" + item.getId() + ":" + pieceNo);
        node.setNodeType("PIECE");
        node.setSelectable(true);
        node.setPieceNo(pieceNo);
        node.setInspectionItem(item.getInspectionItem());
        node.setStandardDesc(item.getStandardDesc());
        node.setUnit(item.getUnit());
        node.setCurrentResult(resolveJudgment(samples, QmsOqcSampleDO::getSampleResult));
        node.setResult(node.getCurrentResult());
        return node;
    }

    private void attachStandardPositionNodes(QmsDispatchTaskCandidateItemRespVO item) {
        Map<String, Object> params = QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams());
        Object rawPositions = params.get("positions");
        if (!(rawPositions instanceof Collection<?> positions) || positions.isEmpty()) {
            return;
        }
        List<QmsDispatchTaskCandidateItemRespVO> children = new ArrayList<>();
        int index = 0;
        for (Object rawPosition : positions) {
            index++;
            Map<?, ?> positionMap = rawPosition instanceof Map<?, ?> map ? map : Map.of();
            String code = StrUtil.blankToDefault(text(positionMap.get("code")), "P" + index);
            String name = StrUtil.blankToDefault(text(positionMap.get("name")), code);
            QmsDispatchTaskCandidateItemRespVO position = new QmsDispatchTaskCandidateItemRespVO();
            position.setId(item.getId());
            position.setSourceItemId(item.getId());
            position.setStandardItemId(item.getStandardItemId());
            position.setNodeKey("POSITION:" + item.getId() + ":" + code);
            position.setNodeType("POSITION");
            position.setSelectable(true);
            position.setPositionCode(code);
            position.setPositionName(name);
            position.setInspectionItem(item.getInspectionItem());
            position.setStandardDesc(item.getStandardDesc());
            position.setUnit(item.getUnit());
            children.add(position);
        }
        item.setChildren(children);
    }

    /**
     * 将任务中心录入结果写入任务生成的新检验单。该入口独立于原有五类检验录入 Service。
     */
    public void saveResults(String checkType, Long executionId, Long operatorId, String operatorName,
                            List<QmsDispatchTaskItemDO> taskItems,
                            List<QmsDispatchTaskResultItemReqVO> requestItems) {
        Map<Long, QmsDispatchTaskItemDO> taskItemMap = taskItems.stream()
                .collect(Collectors.toMap(QmsDispatchTaskItemDO::getId, Function.identity()));
        Set<Long> requestItemIds = requestItems.stream()
                .map(QmsDispatchTaskResultItemReqVO::getTaskItemId).collect(Collectors.toSet());
        if (requestItems.size() != taskItems.size() || requestItemIds.size() != requestItems.size()
                || requestItems.stream().anyMatch(item -> !taskItemMap.containsKey(item.getTaskItemId()))) {
            throw exception(QMS_DISPATCH_ITEM_INVALID);
        }
        for (QmsDispatchTaskResultItemReqVO requestItem : requestItems) {
            QmsDispatchTaskItemDO taskItem = taskItemMap.get(requestItem.getTaskItemId());
            String result = normalizeResult(requestItem.getResult());
            switch (normalizeType(checkType)) {
                case "IQC" -> saveIqcResult(executionId, taskItem, requestItem, result);
                case "FAI", GLUE_BOARD_FAI -> saveFaiResult(executionId, operatorId, operatorName,
                        taskItem, requestItem, result);
                case "IPQC" -> saveIpqcResult(executionId, taskItem, requestItem, result);
                case "FQC" -> saveFqcResult(executionId, operatorId, operatorName, taskItem, requestItem, result);
                case "OQC" -> {
                    if (isShippingFqcExecution(executionId)) {
                        saveFqcResult(executionId, operatorId, operatorName, taskItem, requestItem, result);
                    } else {
                        saveOqcResult(executionId, operatorId, operatorName, taskItem, requestItem, result);
                    }
                }
                default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
            }
        }
    }

    /**
     * 流程关闭时完成任务生成的新检验单，并根据整单项目计算最终判定。
     */
    public void completeInspection(String checkType, Long executionId, Long operatorId, String operatorName) {
        LocalDateTime now = LocalDateTime.now();
        switch (normalizeType(checkType)) {
            case "IQC" -> {
                QmsIqcOrderDO order = required(iqcOrderMapper.selectById(executionId), "IQC", executionId);
                order.setStatus("COMPLETED");
                order.setJudgment(resolveJudgment(iqcItemMapper.selectListByIqcId(executionId), QmsIqcItemDO::getItemResult));
                order.setInspectorId(operatorId); order.setInspectorName(operatorName); order.setInspectionTime(now);
                iqcOrderMapper.updateById(order);
            }
            case "FAI", GLUE_BOARD_FAI -> {
                QmsFaiOrderDO order = requiredFaiOrder(executionId, checkType);
                order.setStatus("COMPLETED");
                order.setJudgment(resolveJudgment(faiItemMapper.selectListByFaiId(executionId),
                        item -> firstNonBlank(item.getOperatorResult(), item.getQaResult())));
                order.setOperatorId(operatorId); order.setOperatorName(operatorName); order.setOperatorTime(now);
                order.setInspectionTime(now); faiOrderMapper.updateById(order);
            }
            case "IPQC" -> {
                QmsIpqcOrderDO order = required(ipqcOrderMapper.selectById(executionId), "IPQC", executionId);
                order.setStatus("COMPLETED");
                order.setJudgment(resolveJudgment(ipqcItemMapper.selectListByIpqcId(executionId), QmsIpqcItemDO::getItemResult));
                order.setInspectorId(operatorId); order.setInspectorName(operatorName); order.setInspectionTime(now);
                ipqcOrderMapper.updateById(order);
            }
            case "FQC" -> {
                QmsFqcOrderDO order = required(fqcOrderMapper.selectById(executionId), "FQC", executionId);
                order.setStatus("COMPLETED");
                order.setJudgment(resolveJudgment(fqcItemMapper.selectListByFqcId(executionId),
                        item -> firstNonBlank(item.getOperatorResult(), item.getItemResult(), item.getQaResult())));
                order.setInspectorId(operatorId); order.setInspectorName(operatorName); order.setInspectionTime(now);
                fqcOrderMapper.updateById(order);
            }
            case "OQC" -> {
                if (isShippingFqcExecution(executionId)) {
                    QmsFqcOrderDO order = required(fqcOrderMapper.selectById(executionId), "OQC", executionId);
                    order.setStatus("COMPLETED");
                    order.setJudgment(resolveJudgment(fqcItemMapper.selectListByFqcId(executionId),
                            item -> firstNonBlank(item.getOperatorResult(), item.getItemResult(), item.getQaResult())));
                    order.setInspectorId(operatorId); order.setInspectorName(operatorName); order.setInspectionTime(now);
                    fqcOrderMapper.updateById(order);
                } else {
                    QmsOqcOrderDO order = required(oqcOrderMapper.selectById(executionId), "OQC", executionId);
                    order.setStatus("COMPLETED");
                    order.setJudgment(resolveJudgment(oqcItemMapper.selectListByOqcId(executionId),
                            item -> firstNonBlank(item.getOperatorResult(), item.getItemResult(), item.getQaResult())));
                    order.setInspectorId(operatorId); order.setInspectorName(operatorName); order.setInspectionTime(now);
                    oqcOrderMapper.updateById(order);
                }
            }
            default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
        }
    }

    /**
     * 结果确认退回重检时，仅将原检验单据状态标记为驳回，保留其检验明细和判定作为历史记录。
     */
    public void markInspectionRejected(String checkType, Long executionId) {
        switch (normalizeType(checkType)) {
            case "IQC" -> {
                QmsIqcOrderDO order = required(iqcOrderMapper.selectById(executionId), "IQC", executionId);
                order.setStatus(REJECTED);
                iqcOrderMapper.updateById(order);
            }
            case "FAI", GLUE_BOARD_FAI -> {
                QmsFaiOrderDO order = requiredFaiOrder(executionId, checkType);
                order.setStatus(REJECTED);
                faiOrderMapper.updateById(order);
            }
            case "IPQC" -> {
                QmsIpqcOrderDO order = required(ipqcOrderMapper.selectById(executionId), "IPQC", executionId);
                order.setStatus(REJECTED);
                ipqcOrderMapper.updateById(order);
            }
            case "FQC" -> {
                QmsFqcOrderDO order = required(fqcOrderMapper.selectById(executionId), "FQC", executionId);
                order.setStatus(REJECTED);
                fqcOrderMapper.updateById(order);
            }
            case "OQC" -> {
                if (isShippingFqcExecution(executionId)) {
                    QmsFqcOrderDO order = required(fqcOrderMapper.selectById(executionId), "OQC", executionId);
                    order.setStatus(REJECTED);
                    fqcOrderMapper.updateById(order);
                } else {
                    QmsOqcOrderDO order = required(oqcOrderMapper.selectById(executionId), "OQC", executionId);
                    order.setStatus(REJECTED);
                    oqcOrderMapper.updateById(order);
                }
            }
            default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
        }
    }

    public GeneratedInspection generate(QmsDispatchTaskWizardCreateReqVO reqVO) {
        String taskType = reqVO.getTaskType().trim().toUpperCase(Locale.ROOT);
        if ("RECHECK".equals(taskType)) {
            return cloneForRecheck(reqVO);
        }

        throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, reqVO.getTaskType());
    }

    /**
     * 加检任务只快照标准及选中项目，不创建字段不完整的原生检验单。
     */
    public TaskOwnedDefinition buildTaskOwnedDefinition(QmsDispatchTaskWizardCreateReqVO reqVO) {
        QmsQualityStandardDO standard = validateStandard(reqVO.getStandardId(), reqVO.getCheckType());
        Map<Long, QmsQualityStandardItemDO> itemMap = standardItemMapper.selectListByStandardId(standard.getId())
                .stream().collect(Collectors.toMap(QmsQualityStandardItemDO::getId, Function.identity()));
        Set<Long> selectedIds = new HashSet<>(reqVO.getSelectedItemIds());
        if (selectedIds.isEmpty() || selectedIds.size() != reqVO.getSelectedItemIds().size()
                || !itemMap.keySet().containsAll(selectedIds)) {
            throw exception(QMS_DISPATCH_ITEM_INVALID);
        }
        Map<Long, Set<String>> selectedPositions = selectedPositionMap(reqVO.getSelectedScopes());
        validateScopeItems(selectedIds, selectedPositions);
        List<GeneratedItem> items = new ArrayList<>();
        reqVO.getSelectedItemIds().stream()
                .map(itemMap::get)
                .sorted(Comparator.comparing(QmsQualityStandardItemDO::getSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .forEach(item -> {
                    Set<String> positions = selectedPositions.get(item.getId());
                    if (Set.of("FAI", GLUE_BOARD_FAI).contains(normalizeType(reqVO.getCheckType()))
                            && positions != null && !positions.isEmpty()) {
                        if (!standardPositionCodes(item).containsAll(positions)) {
                            throw exception(QMS_DISPATCH_ITEM_INVALID);
                        }
                        positions.stream().sorted().forEach(position -> items.add(new GeneratedItem(null, null,
                                item.getId(), position, item.getInspectionItem(), item.getStandardDesc(),
                                item.getUnit(), item.getItemType(), item.getInspectionMethod(), item.getTestTool(),
                                1, item.getSort())));
                    } else {
                        items.add(new GeneratedItem(null, null, item.getId(), null,
                                item.getInspectionItem(), item.getStandardDesc(), item.getUnit(), item.getItemType(),
                                item.getInspectionMethod(), item.getTestTool(), item.getSampleSize(), item.getSort()));
                    }
                });
        return new TaskOwnedDefinition(standard, items);
    }

    /**
     * 质量任务确认退回时，以当前轮检验单为来源复制下一轮检验单。
     */
    public GeneratedInspection cloneForTaskRecheck(String checkType, Long sourceExecutionId,
                                                    Collection<Long> selectedExecutionItemIds) {
        if (sourceExecutionId == null) {
            throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, checkType, null);
        }
        Set<Long> selected = new HashSet<>(selectedExecutionItemIds);
        return switch (normalizeType(checkType)) {
            case "IQC" -> cloneIqc(sourceExecutionId, selected);
            case "FAI" -> cloneFai(sourceExecutionId, selected);
            case GLUE_BOARD_FAI -> cloneFai(GLUE_BOARD_FAI, sourceExecutionId, selected, Map.of(), null);
            case "IPQC" -> cloneIpqc(sourceExecutionId, selected);
            case "FQC" -> cloneFqc(sourceExecutionId, selected);
            case "OQC" -> isShippingFqcExecution(sourceExecutionId)
                    ? cloneShippingFqc(sourceExecutionId, selected, null)
                    : cloneOqc(sourceExecutionId, selected);
            default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
        };
    }

    /**
     * 将产品异常事件专用复制器生成的新检验单适配为质量任务中心的执行对象。
     * 产品复制器负责保留片号、样品格和送检明细；任务中心只建立任务项目映射，不再重复复制检验单。
     */
    public GeneratedInspection describeGeneratedInspection(String checkType,
                                                            Long sourceExecutionId,
                                                            String sourceExecutionNo,
                                                            Long executionId,
                                                            String executionNo,
                                                            Map<Long, Long> itemIdMap,
                                                            Collection<Long> selectedSourceItemIds) {
        if (sourceExecutionId == null || executionId == null || itemIdMap == null) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "产品异常复检单映射不完整");
        }
        Set<Long> selected = new HashSet<>(selectedSourceItemIds);
        List<QmsDispatchTaskCandidateItemRespVO> sourceItems = getSourceItems(checkType, sourceExecutionId);
        validateSelected(sourceItems.stream().map(QmsDispatchTaskCandidateItemRespVO::getId).toList(), selected);
        Map<Long, QmsDispatchTaskCandidateItemRespVO> sourceMap = sourceItems.stream()
                .collect(Collectors.toMap(QmsDispatchTaskCandidateItemRespVO::getId, Function.identity()));
        Map<Long, QmsDispatchTaskCandidateItemRespVO> targetMap = getSourceItems(checkType, executionId).stream()
                .collect(Collectors.toMap(QmsDispatchTaskCandidateItemRespVO::getId, Function.identity()));
        List<GeneratedItem> generatedItems = selected.stream().map(sourceItemId -> {
            Long targetItemId = itemIdMap.get(sourceItemId);
            QmsDispatchTaskCandidateItemRespVO sourceItem = sourceMap.get(sourceItemId);
            QmsDispatchTaskCandidateItemRespVO targetItem = targetMap.get(targetItemId);
            if (sourceItem == null || targetItem == null) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID, "产品异常复检项目映射不存在");
            }
            return item(sourceItemId, targetItemId, sourceItem.getStandardItemId(), sourceItem.getPieceNo(),
                    sourceItem.getInspectionItem(), sourceItem.getStandardDesc(), sourceItem.getUnit(),
                    sourceItem.getItemType(), sourceItem.getSort());
        }).sorted(Comparator.comparing(GeneratedItem::sort,
                Comparator.nullsLast(Integer::compareTo))).toList();
        return generated(checkType, sourceExecutionId, sourceExecutionNo,
                executionId, executionNo, generatedItems);
    }

    private GeneratedInspection cloneForRecheck(QmsDispatchTaskWizardCreateReqVO reqVO) {
        if (reqVO.getSourceExecutionId() == null) {
            throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, reqVO.getCheckType(), null);
        }
        Set<Long> selected = new HashSet<>(reqVO.getSelectedItemIds());
        String checkType = normalizeType(reqVO.getCheckType());
        if (Set.of("FAI", GLUE_BOARD_FAI).contains(checkType)) {
            return cloneFai(checkType, reqVO.getSourceExecutionId(), selected,
                    selectedPositionMap(reqVO.getSelectedScopes()), reqVO.getCheckQty());
        }
        if ("FQC".equals(checkType)) {
            return cloneFqc(reqVO.getSourceExecutionId(), selected, reqVO.getCheckQty());
        }
        if ("OQC".equals(checkType) && isShippingFqcExecution(reqVO.getSourceExecutionId())) {
            return cloneShippingFqc(reqVO.getSourceExecutionId(), selected, reqVO.getCheckQty());
        }
        return cloneForTaskRecheck(reqVO.getCheckType(), reqVO.getSourceExecutionId(), selected);
    }

    private GeneratedInspection createAdditional(QmsDispatchTaskWizardCreateReqVO reqVO) {
        QmsQualityStandardDO standard = validateStandard(reqVO.getStandardId());
        Map<Long, QmsQualityStandardItemDO> itemMap = standardItemMapper.selectListByStandardId(standard.getId())
                .stream().collect(Collectors.toMap(QmsQualityStandardItemDO::getId, Function.identity()));
        List<QmsQualityStandardItemDO> selectedItems = reqVO.getSelectedItemIds().stream()
                .map(itemMap::get).filter(Objects::nonNull).toList();
        if (selectedItems.size() != new HashSet<>(reqVO.getSelectedItemIds()).size()) {
            throw exception(QMS_DISPATCH_ITEM_INVALID);
        }
        return switch (normalizeType(reqVO.getCheckType())) {
            case "IQC" -> createAdditionalIqc(reqVO, standard, selectedItems);
            case "FAI" -> createAdditionalFai(reqVO, standard, selectedItems);
            case "IPQC" -> createAdditionalIpqc(reqVO, standard, selectedItems);
            case "FQC" -> createAdditionalFqc(reqVO, standard, selectedItems);
            case "OQC" -> createAdditionalOqc(reqVO, standard, selectedItems);
            default -> throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, reqVO.getCheckType());
        };
    }

    private GeneratedInspection cloneIqc(Long sourceId, Set<Long> selected) {
        QmsIqcOrderDO source = required(iqcOrderMapper.selectById(sourceId), "IQC", sourceId);
        List<QmsIqcItemDO> sourceItems = iqcItemMapper.selectListByIqcId(sourceId);
        validateSelected(sourceItems.stream().map(QmsIqcItemDO::getId).toList(), selected);
        QmsIqcOrderDO target = copy(source, QmsIqcOrderDO.class);
        target.setId(null);
        target.setIqcNo(noGeneratorService.generateNo("IQC"));
        resetIqcOrder(target);
        iqcOrderMapper.insert(target);
        Map<Long, Long> itemIds = new HashMap<>();
        List<GeneratedItem> generated = new ArrayList<>();
        for (QmsIqcItemDO sourceItem : sourceItems) {
            QmsIqcItemDO item = copy(sourceItem, QmsIqcItemDO.class);
            item.setId(null);
            item.setIqcId(target.getId());
            item.setIqcNo(target.getIqcNo());
            if (selected.contains(sourceItem.getId())) resetIqcItem(item);
            iqcItemMapper.insert(item);
            itemIds.put(sourceItem.getId(), item.getId());
            if (selected.contains(sourceItem.getId())) generated.add(generated(sourceItem, item));
        }
        for (QmsIqcSampleDO sourceSample : iqcSampleMapper.selectListByIqcId(sourceId)) {
            QmsIqcSampleDO sample = copy(sourceSample, QmsIqcSampleDO.class);
            sample.setId(null);
            sample.setIqcId(target.getId());
            sample.setIqcItemId(itemIds.get(sourceSample.getIqcItemId()));
            sample.setIqcNo(target.getIqcNo());
            if (selected.contains(sourceSample.getIqcItemId())) resetIqcSample(sample);
            iqcSampleMapper.insert(sample);
        }
        return generated("IQC", source.getId(), source.getIqcNo(), target.getId(), target.getIqcNo(), generated);
    }

    private GeneratedInspection cloneFai(Long sourceId, Set<Long> selected) {
        return cloneFai("FAI", sourceId, selected, Map.of(), null);
    }

    private GeneratedInspection cloneFai(String checkType, Long sourceId, Set<Long> selected,
                                         Map<Long, Set<String>> selectedPositions,
                                         BigDecimal requestedInspectionQty) {
        QmsFaiOrderDO source = requiredFaiOrder(sourceId, checkType);
        List<QmsFaiItemDO> sourceItems = faiItemMapper.selectListByFaiId(sourceId);
        validateSelected(sourceItems.stream().map(QmsFaiItemDO::getId).toList(), selected);
        List<QmsFaiSampleDO> sourceSamples = faiSampleMapper.selectListByFaiId(sourceId);
        validateSelectedFaiPositions(selected, selectedPositions, sourceSamples);
        QmsFaiOrderDO target = copy(source, QmsFaiOrderDO.class);
        target.setId(null);
        target.setFaiNo(noGeneratorService.generateNo(
                GLUE_BOARD_FAI.equals(normalizeType(checkType)) ? GLUE_BOARD_FAI : "FAI"));
        target.setSourceModule(GLUE_BOARD_FAI.equals(normalizeType(checkType)) ? GLUE_BOARD_FAI : TASK_SOURCE);
        target.setSourceReportId(source.getId());
        target.setSourceReportNo(source.getFaiNo());
        resetFaiOrder(target);
        if (!GLUE_BOARD_FAI.equals(normalizeType(checkType))) {
            target.setInspectionQty(resolveFaiRecheckQty(source, requestedInspectionQty));
        }
        faiOrderMapper.insert(target);
        Map<Long, Long> itemIds = new HashMap<>();
        List<GeneratedItem> generated = new ArrayList<>();
        for (QmsFaiItemDO sourceItem : sourceItems) {
            QmsFaiItemDO item = copy(sourceItem, QmsFaiItemDO.class);
            item.setId(null);
            item.setFaiId(target.getId());
            item.setFaiNo(target.getFaiNo());
            if (selected.contains(sourceItem.getId())) resetFaiItem(item);
            faiItemMapper.insert(item);
            itemIds.put(sourceItem.getId(), item.getId());
            if (selected.contains(sourceItem.getId())) generated.add(generated(sourceItem, item));
        }
        for (QmsFaiSampleDO sourceSample : sourceSamples) {
            QmsFaiSampleDO sample = copy(sourceSample, QmsFaiSampleDO.class);
            sample.setId(null);
            sample.setSheetCellId(null);
            sample.setFaiId(target.getId());
            sample.setFaiItemId(itemIds.get(sourceSample.getFaiItemId()));
            sample.setFaiNo(target.getFaiNo());
            if (shouldResetFaiSample(sourceSample, selected, selectedPositions)) resetFaiSample(sample);
            faiSampleMapper.insert(sample);
        }
        return generated(checkType, source.getId(), source.getFaiNo(), target.getId(), target.getFaiNo(), generated);
    }

    private GeneratedInspection cloneIpqc(Long sourceId, Set<Long> selected) {
        QmsIpqcOrderDO source = required(ipqcOrderMapper.selectById(sourceId), "IPQC", sourceId);
        List<QmsIpqcItemDO> sourceItems = ipqcItemMapper.selectListByIpqcId(sourceId);
        validateSelected(sourceItems.stream().map(QmsIpqcItemDO::getId).toList(), selected);
        QmsIpqcOrderDO target = copy(source, QmsIpqcOrderDO.class);
        target.setId(null);
        target.setIpqcNo(noGeneratorService.generateNo("IPQC"));
        resetIpqcOrder(target);
        ipqcOrderMapper.insert(target);
        Map<Long, Long> itemIds = new HashMap<>();
        List<GeneratedItem> generated = new ArrayList<>();
        for (QmsIpqcItemDO sourceItem : sourceItems) {
            QmsIpqcItemDO item = copy(sourceItem, QmsIpqcItemDO.class);
            item.setId(null);
            item.setIpqcId(target.getId());
            item.setIpqcNo(target.getIpqcNo());
            if (selected.contains(sourceItem.getId())) resetIpqcItem(item);
            ipqcItemMapper.insert(item);
            itemIds.put(sourceItem.getId(), item.getId());
            if (selected.contains(sourceItem.getId())) generated.add(generated(sourceItem, item));
        }
        for (QmsIpqcSampleDO sourceSample : ipqcSampleMapper.selectListByIpqcId(sourceId)) {
            QmsIpqcSampleDO sample = copy(sourceSample, QmsIpqcSampleDO.class);
            sample.setId(null);
            sample.setIpqcId(target.getId());
            sample.setIpqcItemId(itemIds.get(sourceSample.getIpqcItemId()));
            sample.setIpqcNo(target.getIpqcNo());
            if (selected.contains(sourceSample.getIpqcItemId())) resetIpqcSample(sample);
            ipqcSampleMapper.insert(sample);
        }
        return generated("IPQC", source.getId(), source.getIpqcNo(), target.getId(), target.getIpqcNo(), generated);
    }

    private GeneratedInspection cloneFqc(Long sourceId, Set<Long> selected) {
        return cloneFqc(sourceId, selected, null);
    }

    private GeneratedInspection cloneFqc(Long sourceId, Set<Long> selected, BigDecimal requestedInspectionQty) {
        QmsFqcOrderDO source = required(fqcOrderMapper.selectById(sourceId), "FQC", sourceId);
        List<QmsFqcItemDO> sourceItems = fqcItemMapper.selectListByFqcId(sourceId);
        validateSelected(sourceItems.stream().map(QmsFqcItemDO::getId).toList(), selected);
        QmsFqcOrderDO target = copy(source, QmsFqcOrderDO.class);
        target.setId(null);
        target.setFqcNo(noGeneratorService.generateNo("FQC"));
        target.setSourceModule(TASK_SOURCE);
        target.setSourceReportId(source.getId());
        target.setSourceReportNo(source.getFqcNo());
        resetFqcOrder(target);
        target.setSampleQty(resolveFqcRecheckQty(source, requestedInspectionQty));
        fqcOrderMapper.insert(target);
        Map<Long, Long> itemIds = new HashMap<>();
        List<GeneratedItem> generated = new ArrayList<>();
        for (QmsFqcItemDO sourceItem : sourceItems) {
            QmsFqcItemDO item = copy(sourceItem, QmsFqcItemDO.class);
            item.setId(null);
            item.setFqcId(target.getId());
            item.setFqcNo(target.getFqcNo());
            if (selected.contains(sourceItem.getId())) resetFqcItem(item);
            fqcItemMapper.insert(item);
            itemIds.put(sourceItem.getId(), item.getId());
            if (selected.contains(sourceItem.getId())) generated.add(generated(sourceItem, item));
        }
        for (QmsFqcSampleDO sourceSample : fqcSampleMapper.selectListByFqcId(sourceId)) {
            QmsFqcSampleDO sample = copy(sourceSample, QmsFqcSampleDO.class);
            sample.setId(null);
            sample.setSheetCellId(null);
            sample.setFqcId(target.getId());
            sample.setFqcItemId(itemIds.get(sourceSample.getFqcItemId()));
            sample.setFqcNo(target.getFqcNo());
            if (selected.contains(sourceSample.getFqcItemId())) resetFqcSample(sample);
            fqcSampleMapper.insert(sample);
        }
        return generated("FQC", source.getId(), source.getFqcNo(), target.getId(), target.getFqcNo(), generated);
    }

    private GeneratedInspection cloneShippingFqc(Long sourceId, Set<Long> selected,
                                                  BigDecimal requestedInspectionQty) {
        QmsFqcOrderDO source = required(fqcOrderMapper.selectById(sourceId), "OQC", sourceId);
        if (!isShippingFqc(source)) {
            throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, "OQC", sourceId);
        }
        List<QmsFqcItemDO> sourceItems = fqcItemMapper.selectListByFqcId(sourceId);
        validateSelected(sourceItems.stream().map(QmsFqcItemDO::getId).toList(), selected);

        QmsFqcOrderDO target = copy(source, QmsFqcOrderDO.class);
        target.setId(null);
        target.setFqcNo(noGeneratorService.generateNo("FQC"));
        resetFqcOrder(target);
        target.setSampleQty(resolveFqcRecheckQty(source, requestedInspectionQty));
        fqcOrderMapper.insert(target);

        Set<Long> selectedDetailIds = sourceItems.stream()
                .filter(item -> selected.contains(item.getId()))
                .map(QmsFqcItemDO::getSubmissionDetailId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> detailIds = new HashMap<>();
        for (QmsFqcShippingDetailDO sourceDetail : fqcShippingDetailMapper.selectListByFqcId(sourceId)) {
            QmsFqcShippingDetailDO detail = copy(sourceDetail, QmsFqcShippingDetailDO.class);
            detail.setId(null);
            detail.setFqcId(target.getId());
            detail.setFqcNo(target.getFqcNo());
            if (selectedDetailIds.contains(sourceDetail.getId())) {
                resetFqcShippingDetail(detail);
            }
            fqcShippingDetailMapper.insert(detail);
            detailIds.put(sourceDetail.getId(), detail.getId());
        }

        Map<Long, Long> itemIds = new HashMap<>();
        List<GeneratedItem> generatedItems = new ArrayList<>();
        for (QmsFqcItemDO sourceItem : sourceItems) {
            QmsFqcItemDO item = copy(sourceItem, QmsFqcItemDO.class);
            item.setId(null);
            item.setFqcId(target.getId());
            item.setFqcNo(target.getFqcNo());
            item.setSubmissionDetailId(detailIds.get(sourceItem.getSubmissionDetailId()));
            if (selected.contains(sourceItem.getId())) {
                resetFqcItem(item);
            }
            fqcItemMapper.insert(item);
            itemIds.put(sourceItem.getId(), item.getId());
            if (selected.contains(sourceItem.getId())) {
                generatedItems.add(generated(sourceItem, item));
            }
        }

        Map<Long, Long> cellIds = new HashMap<>();
        for (QmsFqcSheetCellValueDO sourceCell : fqcSheetCellValueMapper.selectListByFqcId(sourceId)) {
            QmsFqcSheetCellValueDO cell = copy(sourceCell, QmsFqcSheetCellValueDO.class);
            cell.setId(null);
            cell.setFqcId(target.getId());
            cell.setFqcItemId(itemIds.get(sourceCell.getFqcItemId()));
            if (selected.contains(sourceCell.getFqcItemId())) {
                resetFqcCell(cell);
            }
            fqcSheetCellValueMapper.insert(cell);
            cellIds.put(sourceCell.getId(), cell.getId());
        }

        for (QmsFqcSampleDO sourceSample : fqcSampleMapper.selectListByFqcId(sourceId)) {
            QmsFqcSampleDO sample = copy(sourceSample, QmsFqcSampleDO.class);
            sample.setId(null);
            sample.setFqcId(target.getId());
            sample.setFqcItemId(itemIds.get(sourceSample.getFqcItemId()));
            sample.setFqcNo(target.getFqcNo());
            sample.setSubmissionDetailId(detailIds.get(sourceSample.getSubmissionDetailId()));
            sample.setSheetCellId(cellIds.get(sourceSample.getSheetCellId()));
            if (selected.contains(sourceSample.getFqcItemId())) {
                resetFqcSample(sample);
            }
            fqcSampleMapper.insert(sample);
        }
        return generated("OQC", source.getId(), source.getFqcNo(), target.getId(), target.getFqcNo(),
                "/mes/quality/fg-shipping-fqc", generatedItems);
    }

    private GeneratedInspection cloneOqc(Long sourceId, Set<Long> selected) {
        QmsOqcOrderDO source = required(oqcOrderMapper.selectById(sourceId), "OQC", sourceId);
        List<QmsOqcItemDO> sourceItems = oqcItemMapper.selectListByOqcId(sourceId);
        validateSelected(sourceItems.stream().map(QmsOqcItemDO::getId).toList(), selected);
        QmsOqcOrderDO target = copy(source, QmsOqcOrderDO.class);
        target.setId(null);
        target.setOqcNo(noGeneratorService.generateNo("OQC"));
        resetOqcOrder(target);
        oqcOrderMapper.insert(target);
        Map<Long, Long> itemIds = new HashMap<>();
        List<GeneratedItem> generated = new ArrayList<>();
        for (QmsOqcItemDO sourceItem : sourceItems) {
            QmsOqcItemDO item = copy(sourceItem, QmsOqcItemDO.class);
            item.setId(null);
            item.setOqcId(target.getId());
            item.setOqcNo(target.getOqcNo());
            if (selected.contains(sourceItem.getId())) resetOqcItem(item);
            oqcItemMapper.insert(item);
            itemIds.put(sourceItem.getId(), item.getId());
            if (selected.contains(sourceItem.getId())) generated.add(generated(sourceItem, item));
        }
        for (QmsOqcSampleDO sourceSample : oqcSampleMapper.selectListByOqcId(sourceId)) {
            QmsOqcSampleDO sample = copy(sourceSample, QmsOqcSampleDO.class);
            sample.setId(null);
            sample.setOqcId(target.getId());
            sample.setOqcItemId(itemIds.get(sourceSample.getOqcItemId()));
            sample.setOqcNo(target.getOqcNo());
            if (selected.contains(sourceSample.getOqcItemId())) resetOqcSample(sample);
            oqcSampleMapper.insert(sample);
        }
        return generated("OQC", source.getId(), source.getOqcNo(), target.getId(), target.getOqcNo(), generated);
    }

    private GeneratedInspection createAdditionalIqc(QmsDispatchTaskWizardCreateReqVO req,
                                                     QmsQualityStandardDO standard,
                                                     List<QmsQualityStandardItemDO> standards) {
        QmsIqcOrderDO order = new QmsIqcOrderDO();
        order.setIqcNo(noGeneratorService.generateNo("IQC"));
        applyCommon(order, req, standard);
        order.setBatchNo(req.getBatchNo());
        order.setReceiveQty(req.getCheckQty());
        order.setUnit(req.getUnit());
        order.setReceiptNo(req.getReceiptNo());
        order.setSupplierName(req.getSupplierName());
        order.setArrivalDate(req.getArrivalDate());
        order.setStatus(PENDING);
        order.setJudgment(PENDING);
        iqcOrderMapper.insert(order);
        List<GeneratedItem> items = new ArrayList<>();
        for (QmsQualityStandardItemDO standardItem : standards) {
            QmsIqcItemDO item = standardIqcItem(order, standardItem);
            iqcItemMapper.insert(item);
            insertIqcSamples(order, item);
            items.add(generated(standardItem, item));
        }
        return generated("IQC", null, null, order.getId(), order.getIqcNo(), items);
    }

    private GeneratedInspection createAdditionalFai(QmsDispatchTaskWizardCreateReqVO req,
                                                     QmsQualityStandardDO standard,
                                                     List<QmsQualityStandardItemDO> standards) {
        QmsFaiOrderDO order = new QmsFaiOrderDO();
        order.setFaiNo(noGeneratorService.generateNo("FAI"));
        applyCommon(order, req, standard);
        order.setProductBatchNo(req.getBatchNo());
        order.setInspectionQty(req.getCheckQty());
        order.setSourceModule(TASK_SOURCE);
        order.setOperationCode(req.getOperationCode());
        order.setOperationName(req.getOperationName());
        order.setStatus(PENDING);
        order.setJudgment(PENDING);
        order.setRequiredItemCount(standards.size());
        order.setCompletedItemCount(0);
        order.setAbnormalItemCount(0);
        faiOrderMapper.insert(order);
        List<GeneratedItem> items = new ArrayList<>();
        for (QmsQualityStandardItemDO standardItem : standards) {
            QmsFaiItemDO item = standardFaiItem(order, standardItem);
            faiItemMapper.insert(item);
            insertFaiSamples(order, item);
            items.add(generated(standardItem, item));
        }
        return generated("FAI", null, null, order.getId(), order.getFaiNo(), items);
    }

    private GeneratedInspection createAdditionalIpqc(QmsDispatchTaskWizardCreateReqVO req,
                                                      QmsQualityStandardDO standard,
                                                      List<QmsQualityStandardItemDO> standards) {
        QmsIpqcOrderDO order = new QmsIpqcOrderDO();
        order.setIpqcNo(noGeneratorService.generateNo("IPQC"));
        applyCommon(order, req, standard);
        order.setOperationCode(req.getOperationCode());
        order.setOperationName(req.getOperationName());
        order.setStatus(PENDING);
        order.setJudgment(PENDING);
        ipqcOrderMapper.insert(order);
        List<GeneratedItem> items = new ArrayList<>();
        for (QmsQualityStandardItemDO standardItem : standards) {
            QmsIpqcItemDO item = standardIpqcItem(order, standardItem);
            ipqcItemMapper.insert(item);
            insertIpqcSamples(order, item);
            items.add(generated(standardItem, item));
        }
        return generated("IPQC", null, null, order.getId(), order.getIpqcNo(), items);
    }

    private GeneratedInspection createAdditionalFqc(QmsDispatchTaskWizardCreateReqVO req,
                                                     QmsQualityStandardDO standard,
                                                     List<QmsQualityStandardItemDO> standards) {
        QmsFqcOrderDO order = new QmsFqcOrderDO();
        order.setFqcNo(noGeneratorService.generateNo("FQC"));
        applyCommon(order, req, standard);
        order.setProductBatchNo(req.getBatchNo());
        order.setBatchNo(req.getBatchNo());
        order.setProduceQty(req.getCheckQty());
        order.setSampleQty(req.getCheckQty() == null ? null : req.getCheckQty().intValue());
        order.setUnitCode(req.getUnit());
        order.setUnitName(req.getUnit());
        order.setOperationCode(req.getOperationCode());
        order.setOperationName(req.getOperationName());
        order.setSourceModule(TASK_SOURCE);
        order.setStatus(PENDING);
        order.setJudgment(PENDING);
        order.setRequiredItemCount(standards.size());
        order.setCompletedItemCount(0);
        order.setAbnormalItemCount(0);
        order.setOkQty(0);
        order.setNgQty(0);
        fqcOrderMapper.insert(order);
        List<GeneratedItem> items = new ArrayList<>();
        for (QmsQualityStandardItemDO standardItem : standards) {
            QmsFqcItemDO item = standardFqcItem(order, standardItem);
            fqcItemMapper.insert(item);
            insertFqcSamples(order, item);
            items.add(generated(standardItem, item));
        }
        return generated("FQC", null, null, order.getId(), order.getFqcNo(), items);
    }

    private GeneratedInspection createAdditionalOqc(QmsDispatchTaskWizardCreateReqVO req,
                                                     QmsQualityStandardDO standard,
                                                     List<QmsQualityStandardItemDO> standards) {
        QmsOqcOrderDO order = new QmsOqcOrderDO();
        order.setOqcNo(noGeneratorService.generateNo("OQC"));
        applyCommon(order, req, standard);
        order.setBatchNo(req.getBatchNo());
        order.setShippingQty(req.getCheckQty());
        order.setSampleQty(req.getCheckQty() == null ? null : req.getCheckQty().intValue());
        order.setUnitCode(req.getUnit());
        order.setUnitName(req.getUnit());
        order.setStatus(PENDING);
        order.setJudgment(PENDING);
        order.setRequiredItemCount(standards.size());
        order.setCompletedItemCount(0);
        order.setAbnormalItemCount(0);
        oqcOrderMapper.insert(order);
        List<GeneratedItem> items = new ArrayList<>();
        for (QmsQualityStandardItemDO standardItem : standards) {
            QmsOqcItemDO item = standardOqcItem(order, standardItem);
            oqcItemMapper.insert(item);
            insertOqcSamples(order, item);
            items.add(generated(standardItem, item));
        }
        return generated("OQC", null, null, order.getId(), order.getOqcNo(), items);
    }

    private PageResult<QmsIqcOrderDO> selectIqcPage(QmsDispatchTaskSourcePageReqVO req) {
        String keyword = StrUtil.trimToEmpty(req.getBatchNo());
        LambdaQueryWrapperX<QmsIqcOrderDO> query = new LambdaQueryWrapperX<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getStatus, "COMPLETED");
        if (StrUtil.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like(QmsIqcOrderDO::getIqcNo, keyword)
                    .or().like(QmsIqcOrderDO::getReceiptNo, keyword)
                    .or().like(QmsIqcOrderDO::getSupplierName, keyword)
                    .or().like(QmsIqcOrderDO::getMaterialName, keyword)
                    .or().like(QmsIqcOrderDO::getBatchNo, keyword));
        }
        query.orderByDesc(QmsIqcOrderDO::getQaTime)
                .orderByDesc(QmsIqcOrderDO::getId);
        return iqcOrderMapper.selectPage(req, query);
    }

    private PageResult<QmsFaiOrderDO> selectFaiPage(QmsDispatchTaskSourcePageReqVO req) {
        LambdaQueryWrapperX<QmsFaiOrderDO> query = new LambdaQueryWrapperX<>();
        query.eq(QmsFaiOrderDO::getStatus, "COMPLETED");
        query.and(wrapper -> wrapper.isNull(QmsFaiOrderDO::getSourceModule)
                .or().ne(QmsFaiOrderDO::getSourceModule, GLUE_BOARD_FAI));
        query.likeIfPresent(QmsFaiOrderDO::getOperationName, req.getOperationName());
        query.likeIfPresent(QmsFaiOrderDO::getProductBatchNo, req.getBatchNo());
        query.orderByDesc(QmsFaiOrderDO::getInspectionTime);
        query.orderByDesc(QmsFaiOrderDO::getId);
        return faiOrderMapper.selectPage(req, query);
    }

    private PageResult<QmsFaiOrderDO> selectGlueBoardFaiPage(QmsDispatchTaskSourcePageReqVO req) {
        String keyword = StrUtil.trimToEmpty(req.getBatchNo());
        LambdaQueryWrapperX<QmsFaiOrderDO> query = new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getStatus, "COMPLETED")
                .eq(QmsFaiOrderDO::getSourceModule, GLUE_BOARD_FAI)
                .likeIfPresent(QmsFaiOrderDO::getOperationName, req.getOperationName());
        if (StrUtil.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like(QmsFaiOrderDO::getFaiNo, keyword)
                    .or().like(QmsFaiOrderDO::getGlueBoardModel, keyword)
                    .or().like(QmsFaiOrderDO::getGlueBoardMaterialCode, keyword)
                    .or().like(QmsFaiOrderDO::getGluePlateBatchNo, keyword));
        }
        query.orderByDesc(QmsFaiOrderDO::getQaTime)
                .orderByDesc(QmsFaiOrderDO::getInspectionTime)
                .orderByDesc(QmsFaiOrderDO::getId);
        return faiOrderMapper.selectPage(req, query);
    }

    private PageResult<QmsIpqcOrderDO> selectIpqcPage(QmsDispatchTaskSourcePageReqVO req) {
        return ipqcOrderMapper.selectPage(req, new LambdaQueryWrapperX<QmsIpqcOrderDO>()
                .likeIfPresent(QmsIpqcOrderDO::getOperationName, req.getOperationName())
                .likeIfPresent(QmsIpqcOrderDO::getWorkOrderNo, req.getBatchNo())
                .orderByDesc(QmsIpqcOrderDO::getId));
    }

    private PageResult<QmsFqcOrderDO> selectFqcPage(QmsDispatchTaskSourcePageReqVO req) {
        return fqcOrderMapper.selectPage(req, new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getStatus, "COMPLETED")
                .eq(QmsFqcOrderDO::getSourceModule, QmsFqcOrderMapper.SOURCE_MODULE_CUT_ROUND_FQC)
                .likeIfPresent(QmsFqcOrderDO::getOperationName, req.getOperationName())
                .and(StrUtil.isNotBlank(req.getBatchNo()), wrapper -> wrapper
                        .like(QmsFqcOrderDO::getProductBatchNo, req.getBatchNo())
                        .or().like(QmsFqcOrderDO::getBatchNo, req.getBatchNo()))
                .orderByDesc(QmsFqcOrderDO::getInspectionTime)
                .orderByDesc(QmsFqcOrderDO::getId));
    }

    private PageResult<QmsFqcOrderDO> selectShippingFqcPage(QmsDispatchTaskSourcePageReqVO req) {
        String keyword = StrUtil.trimToEmpty(req.getBatchNo());
        List<Long> detailFqcIds = fqcShippingDetailMapper.selectFqcIdsByKeyword(keyword);
        LambdaQueryWrapperX<QmsFqcOrderDO> query = new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getStatus, "COMPLETED")
                .eq(QmsFqcOrderDO::getSourceModule, QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC);
        if (StrUtil.isNotBlank(keyword)) {
            query.and(wrapper -> {
                wrapper.like(QmsFqcOrderDO::getFqcNo, keyword)
                        .or().like(QmsFqcOrderDO::getMaterialCode, keyword)
                        .or().like(QmsFqcOrderDO::getProductModel, keyword);
                if (!detailFqcIds.isEmpty()) {
                    wrapper.or().in(QmsFqcOrderDO::getId, detailFqcIds);
                }
            });
        }
        query.orderByDesc(QmsFqcOrderDO::getQaTime)
                .orderByDesc(QmsFqcOrderDO::getInspectionTime)
                .orderByDesc(QmsFqcOrderDO::getId);
        return fqcOrderMapper.selectPage(req, query);
    }

    private PageResult<QmsOqcOrderDO> selectOqcPage(QmsDispatchTaskSourcePageReqVO req) {
        return oqcOrderMapper.selectPage(req, new LambdaQueryWrapperX<QmsOqcOrderDO>()
                .and(StrUtil.isNotBlank(req.getBatchNo()), wrapper -> wrapper
                        .like(QmsOqcOrderDO::getBatchNo, req.getBatchNo())
                        .or().like(QmsOqcOrderDO::getCustomerBatchNo, req.getBatchNo()))
                .orderByDesc(QmsOqcOrderDO::getId));
    }

    private QmsDispatchTaskSourceRespVO toSource(QmsIqcOrderDO order) {
        QmsDispatchTaskSourceRespVO result = source(order.getId(), order.getIqcNo(), null, "进料检验", order.getBatchNo(),
                firstNonBlank(order.getProductModelCode(), order.getModelNo()), order.getMaterialCode(),
                order.getMaterialName(), order.getReceiveQty(), countNg(iqcItemMapper.selectListByIqcId(order.getId()),
                        QmsIqcItemDO::getItemResult), order.getUnit(), order.getStatus(), order.getJudgment(),
                ngSummary(iqcItemMapper.selectListByIqcId(order.getId()), QmsIqcItemDO::getInspectionItem,
                        QmsIqcItemDO::getItemResult));
        result.setReceiptNo(order.getReceiptNo());
        result.setSupplierName(order.getSupplierName());
        result.setArrivalDate(order.getArrivalDate());
        result.setProductionDate(order.getProductionDate());
        result.setMaterialId(order.getMaterialId());
        result.setSpecification(order.getSpecification());
        applyStandard(result, order.getStandardId(), order.getStandardNo());
        result.setInspectorName(order.getInspectorName());
        result.setInspectionTime(order.getInspectionTime());
        result.setAuditorName(order.getQaInspectorName());
        result.setAuditTime(order.getQaTime());
        return result;
    }

    private QmsDispatchTaskSourceRespVO toSource(QmsFaiOrderDO order) {
        List<QmsFaiItemDO> items = faiItemMapper.selectListByFaiId(order.getId());
        QmsDispatchTaskSourceRespVO result = source(order.getId(), order.getFaiNo(), order.getOperationCode(), order.getOperationName(),
                order.getProductBatchNo(), order.getProductModel(), order.getMaterialCode(), order.getMaterialName(),
                order.getInspectionQty(), countNg(items, QmsFaiItemDO::getOperatorResult), "片",
                order.getStatus(), order.getJudgment(),
                ngSummary(items, QmsFaiItemDO::getInspectionItem, QmsFaiItemDO::getOperatorResult));
        result.setWorkOrderNo(order.getWorkOrderNo());
        result.setMaterialId(order.getMaterialId());
        result.setSpecification(order.getSpecification());
        applyStandard(result, order.getStandardId(), order.getStandardNo());
        result.setInspectorName(order.getOperatorName());
        result.setInspectionTime(firstNonNull(order.getInspectionTime(), order.getOperatorTime()));
        result.setAuditorName(order.getQaInspectorName());
        result.setAuditTime(order.getQaTime());
        return result;
    }

    private QmsDispatchTaskSourceRespVO toGlueBoardSource(QmsFaiOrderDO order) {
        List<QmsFaiItemDO> items = faiItemMapper.selectListByFaiId(order.getId());
        QmsDispatchTaskSourceRespVO result = source(order.getId(), order.getFaiNo(),
                order.getOperationCode(), order.getOperationName(), order.getGluePlateBatchNo(),
                order.getGlueBoardModel(), order.getGlueBoardMaterialCode(), order.getMaterialName(),
                firstNonNull(order.getSampleLength(), order.getInspectionQty()),
                countNg(items, item -> firstNonBlank(item.getQaResult(), item.getOperatorResult())), "m",
                order.getStatus(), order.getJudgment(),
                ngSummary(items, QmsFaiItemDO::getInspectionItem,
                        item -> firstNonBlank(item.getQaResult(), item.getOperatorResult())));
        result.setWorkOrderNo(order.getWorkOrderNo());
        result.setMaterialId(order.getMaterialId());
        result.setSpecification(order.getSpecification());
        result.setRemark(order.getRemark());
        applyStandard(result, order.getStandardId(), order.getStandardNo());
        result.setInspectorName(order.getOperatorName());
        result.setInspectionTime(firstNonNull(order.getInspectionTime(), order.getOperatorTime()));
        result.setAuditorName(order.getQaInspectorName());
        result.setAuditTime(order.getQaTime());
        return result;
    }

    private QmsDispatchTaskSourceRespVO toSource(QmsIpqcOrderDO order) {
        List<QmsIpqcItemDO> items = ipqcItemMapper.selectListByIpqcId(order.getId());
        return source(order.getId(), order.getIpqcNo(), order.getOperationCode(), order.getOperationName(),
                order.getWorkOrderNo(), null, order.getMaterialCode(), order.getMaterialName(),
                BigDecimal.valueOf(items.size()), countNg(items, QmsIpqcItemDO::getItemResult), "项",
                order.getStatus(), order.getJudgment(),
                ngSummary(items, QmsIpqcItemDO::getInspectionItem, QmsIpqcItemDO::getItemResult));
    }

    private QmsDispatchTaskSourceRespVO toSource(QmsFqcOrderDO order) {
        List<QmsFqcItemDO> items = fqcItemMapper.selectListByFqcId(order.getId());
        QmsDispatchTaskSourceRespVO result = source(order.getId(), order.getFqcNo(), order.getOperationCode(), order.getOperationName(),
                firstNonBlank(order.getProductBatchNo(), order.getBatchNo()), order.getProductModel(),
                order.getMaterialCode(), order.getMaterialName(), fqcSubmittedQty(order), decimal(order.getNgQty()),
                firstNonBlank(order.getUnitName(), order.getUnitCode()), order.getStatus(), order.getJudgment(),
                ngSummary(items, QmsFqcItemDO::getInspectionItem, QmsFqcItemDO::getItemResult));
        result.setPlanNo(firstNonBlank(order.getReportNo(),
                order.getPlanOrderId() == null ? null : String.valueOf(order.getPlanOrderId())));
        result.setWorkOrderNo(order.getWorkOrderNo());
        result.setMaterialId(order.getMaterialId());
        result.setSpecification(order.getSpecification());
        result.setOkQty(decimal(order.getOkQty()));
        applyStandard(result, order.getStandardId(), order.getStandardNo());
        result.setInspectorName(order.getInspectorName());
        result.setInspectionTime(order.getInspectionTime());
        result.setAuditorName(order.getQaInspectorName());
        result.setAuditTime(order.getQaTime());
        return result;
    }

    private QmsDispatchTaskSourceRespVO toShippingSource(QmsFqcOrderDO order) {
        List<QmsFqcShippingDetailDO> details = fqcShippingDetailMapper.selectListByFqcId(order.getId());
        QmsFqcShippingDetailDO firstDetail = details.stream().findFirst().orElse(null);
        List<QmsFqcItemDO> items = fqcItemMapper.selectListByFqcId(order.getId());
        String shippingNoticeNo = joinDistinct(details.stream()
                .map(QmsFqcShippingDetailDO::getShippingNoticeNo).toList());
        String customerName = joinDistinct(details.stream()
                .map(QmsFqcShippingDetailDO::getCustomerName).toList());
        BigDecimal checkQty = fqcSubmittedQty(order);
        if (checkQty.signum() <= 0 && order.getSampleQty() != null && order.getSampleQty() > 0) {
            checkQty = BigDecimal.valueOf(order.getSampleQty());
        }
        if (checkQty.signum() <= 0) {
            checkQty = BigDecimal.valueOf(details.stream()
                    .map(QmsFqcShippingDetailDO::getShippingQty)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum());
        }
        QmsDispatchTaskSourceRespVO result = source(order.getId(), order.getFqcNo(), null, "出货检验",
                shippingNoticeNo,
                firstNonBlank(order.getProductModel(), firstDetail == null ? null : firstDetail.getModelCode()),
                firstNonBlank(order.getMaterialCode(), firstDetail == null ? null : firstDetail.getMaterialCode()),
                firstNonBlank(order.getMaterialName(), firstDetail == null ? null : firstDetail.getMaterialName()),
                checkQty, decimal(order.getNgQty()), "片", order.getStatus(), order.getJudgment(),
                ngSummary(items, QmsFqcItemDO::getInspectionItem,
                        item -> firstNonBlank(item.getQaResult(), item.getOperatorResult(), item.getItemResult())));
        result.setShippingNoticeNo(shippingNoticeNo);
        result.setCustomerName(customerName);
        result.setOkQty(decimal(order.getOkQty()));
        result.setMaterialId(order.getMaterialId());
        result.setSpecification(firstNonBlank(order.getSpecification(),
                firstDetail == null ? null : firstDetail.getProductSize()));
        applyStandard(result, order.getStandardId(), order.getStandardNo());
        result.setInspectorName(order.getInspectorName());
        result.setInspectionTime(order.getInspectionTime());
        result.setAuditorName(order.getQaInspectorName());
        result.setAuditTime(order.getQaTime());
        return result;
    }

    private QmsDispatchTaskSourceRespVO toSource(QmsOqcOrderDO order) {
        List<QmsOqcItemDO> items = oqcItemMapper.selectListByOqcId(order.getId());
        return source(order.getId(), order.getOqcNo(), null, "出货检验",
                firstNonBlank(order.getBatchNo(), order.getCustomerBatchNo()), order.getModelCode(),
                order.getMaterialCode(), order.getMaterialName(), decimal(order.getSampleQty()),
                countNg(items, QmsOqcItemDO::getItemResult), firstNonBlank(order.getUnitName(), order.getUnitCode()),
                order.getStatus(), order.getJudgment(),
                ngSummary(items, QmsOqcItemDO::getInspectionItem, QmsOqcItemDO::getItemResult));
    }

    private QmsDispatchTaskSourceRespVO source(Long id, String no, String operationCode, String operationName,
                                               String batchNo, String productModel, String materialCode,
                                               String materialName, BigDecimal checkQty, BigDecimal ngQty,
                                               String unit, String status, String judgment, String summary) {
        QmsDispatchTaskSourceRespVO result = new QmsDispatchTaskSourceRespVO();
        result.setId(id);
        result.setExecutionNo(no);
        result.setOperationCode(operationCode);
        result.setOperationName(operationName);
        result.setBatchNo(batchNo);
        result.setProductModel(productModel);
        result.setMaterialCode(materialCode);
        result.setMaterialName(materialName);
        result.setCheckQty(checkQty);
        result.setNgQty(ngQty);
        result.setUnit(unit);
        result.setStatus(status);
        result.setJudgment(judgment);
        result.setAbnormalSummary(summary);
        return result;
    }

    private void saveIqcResult(Long executionId, QmsDispatchTaskItemDO taskItem,
                               QmsDispatchTaskResultItemReqVO request, String result) {
        QmsIqcItemDO item = required(iqcItemMapper.selectById(taskItem.getExecutionItemId()), "IQC", executionId);
        if (!Objects.equals(item.getIqcId(), executionId)) throw exception(QMS_DISPATCH_ITEM_INVALID);
        item.setAverageValue(request.getMeasuredValue()); item.setMinValue(request.getMeasuredValue());
        item.setMaxValue(request.getMeasuredValue()); item.setItemResult(result); iqcItemMapper.updateById(item);
        iqcSampleMapper.selectListByIqcId(executionId).stream()
                .filter(sample -> Objects.equals(sample.getIqcItemId(), item.getId()))
                .forEach(sample -> {
                    sample.setMeasuredValue(request.getMeasuredValue()); sample.setResultValue(request.getMeasuredValue());
                    sample.setQualitativeValue(request.getQualitativeValue()); sample.setSampleResult(result);
                    iqcSampleMapper.updateById(sample);
                });
    }

    private void saveFaiResult(Long executionId, Long operatorId, String operatorName,
                               QmsDispatchTaskItemDO taskItem, QmsDispatchTaskResultItemReqVO request,
                               String result) {
        QmsFaiItemDO item = required(faiItemMapper.selectById(taskItem.getExecutionItemId()), "FAI", executionId);
        if (!Objects.equals(item.getFaiId(), executionId)) throw exception(QMS_DISPATCH_ITEM_INVALID);
        item.setOperatorAvg(request.getMeasuredValue()); item.setOperatorMin(request.getMeasuredValue());
        item.setOperatorMax(request.getMeasuredValue()); item.setOperatorResult(result); item.setOperatorId(operatorId);
        item.setOperatorName(operatorName); item.setOperatorTime(LocalDateTime.now()); item.setInputStatus("COMPLETED");
        item.setCompletedSampleCount(Math.max(1, item.getRequiredSampleCount() == null ? 1 : item.getRequiredSampleCount()));
        item.setAbnormalSampleCount("NG".equals(result) ? item.getCompletedSampleCount() : 0);
        faiItemMapper.updateById(item);
        faiSampleMapper.selectListByFaiId(executionId).stream()
                .filter(sample -> Objects.equals(sample.getFaiItemId(), item.getId()))
                .forEach(sample -> {
                    sample.setMeasuredValue(request.getMeasuredValue()); sample.setResultValue(request.getMeasuredValue());
                    sample.setQualitativeValue(request.getQualitativeValue()); sample.setSampleResult(result);
                    sample.setInputTime(LocalDateTime.now()); faiSampleMapper.updateById(sample);
                });
    }

    private void saveIpqcResult(Long executionId, QmsDispatchTaskItemDO taskItem,
                                QmsDispatchTaskResultItemReqVO request, String result) {
        QmsIpqcItemDO item = required(ipqcItemMapper.selectById(taskItem.getExecutionItemId()), "IPQC", executionId);
        if (!Objects.equals(item.getIpqcId(), executionId)) throw exception(QMS_DISPATCH_ITEM_INVALID);
        item.setAverageValue(request.getMeasuredValue()); item.setMinValue(request.getMeasuredValue());
        item.setMaxValue(request.getMeasuredValue()); item.setItemResult(result); ipqcItemMapper.updateById(item);
        ipqcSampleMapper.selectListByIpqcId(executionId).stream()
                .filter(sample -> Objects.equals(sample.getIpqcItemId(), item.getId()))
                .forEach(sample -> {
                    sample.setMeasuredValue(request.getMeasuredValue()); sample.setQualitativeValue(request.getQualitativeValue());
                    sample.setSampleResult(result); ipqcSampleMapper.updateById(sample);
                });
    }

    private void saveFqcResult(Long executionId, Long operatorId, String operatorName,
                               QmsDispatchTaskItemDO taskItem, QmsDispatchTaskResultItemReqVO request,
                               String result) {
        QmsFqcItemDO item = required(fqcItemMapper.selectById(taskItem.getExecutionItemId()), "FQC", executionId);
        if (!Objects.equals(item.getFqcId(), executionId)) throw exception(QMS_DISPATCH_ITEM_INVALID);
        item.setAverageValue(request.getMeasuredValue()); item.setMinValue(request.getMeasuredValue());
        item.setMaxValue(request.getMeasuredValue()); item.setItemResult(result); item.setOperatorAvg(request.getMeasuredValue());
        item.setOperatorMin(request.getMeasuredValue()); item.setOperatorMax(request.getMeasuredValue());
        item.setOperatorResult(result); item.setOperatorId(operatorId); item.setOperatorName(operatorName);
        item.setOperatorTime(LocalDateTime.now()); item.setInputStatus("COMPLETED");
        item.setCompletedSampleCount(Math.max(1, item.getRequiredSampleCount() == null ? 1 : item.getRequiredSampleCount()));
        item.setAbnormalSampleCount("NG".equals(result) ? item.getCompletedSampleCount() : 0);
        fqcItemMapper.updateById(item);
        fqcSampleMapper.selectListByFqcId(executionId).stream()
                .filter(sample -> Objects.equals(sample.getFqcItemId(), item.getId()))
                .forEach(sample -> {
                    sample.setMeasuredValue(request.getMeasuredValue()); sample.setResultValue(request.getMeasuredValue());
                    sample.setQualitativeValue(request.getQualitativeValue()); sample.setSampleResult(result);
                    sample.setInputTime(LocalDateTime.now()); fqcSampleMapper.updateById(sample);
                });
    }

    private void saveOqcResult(Long executionId, Long operatorId, String operatorName,
                               QmsDispatchTaskItemDO taskItem, QmsDispatchTaskResultItemReqVO request,
                               String result) {
        QmsOqcItemDO item = required(oqcItemMapper.selectById(taskItem.getExecutionItemId()), "OQC", executionId);
        if (!Objects.equals(item.getOqcId(), executionId)) throw exception(QMS_DISPATCH_ITEM_INVALID);
        item.setAverageValue(request.getMeasuredValue()); item.setMinValue(request.getMeasuredValue());
        item.setMaxValue(request.getMeasuredValue()); item.setItemResult(result); item.setOperatorAvg(request.getMeasuredValue());
        item.setOperatorMin(request.getMeasuredValue()); item.setOperatorMax(request.getMeasuredValue());
        item.setOperatorResult(result); item.setOperatorId(operatorId); item.setOperatorName(operatorName);
        item.setOperatorTime(LocalDateTime.now()); item.setInputStatus("COMPLETED");
        item.setCompletedSampleCount(Math.max(1, item.getRequiredSampleCount() == null ? 1 : item.getRequiredSampleCount()));
        item.setAbnormalSampleCount("NG".equals(result) ? item.getCompletedSampleCount() : 0);
        oqcItemMapper.updateById(item);
        oqcSampleMapper.selectListByOqcId(executionId).stream()
                .filter(sample -> Objects.equals(sample.getOqcItemId(), item.getId()))
                .forEach(sample -> {
                    sample.setMeasuredValue(request.getMeasuredValue()); sample.setResultValue(request.getMeasuredValue());
                    sample.setQualitativeValue(request.getQualitativeValue()); sample.setSampleResult(result);
                    sample.setInputTime(LocalDateTime.now()); oqcSampleMapper.updateById(sample);
                });
    }

    private String normalizeResult(String value) {
        String result = normalizeType(value);
        if (!Set.of("OK", "NG").contains(result)) throw exception(QMS_DISPATCH_ITEM_INVALID);
        return result;
    }

    private <T> String resolveJudgment(List<T> items, Function<T, String> resultGetter) {
        List<String> results = items.stream().map(resultGetter).filter(StrUtil::isNotBlank)
                .map(this::normalizeType).toList();
        if (results.stream().anyMatch("NG"::equals)) return "NG";
        if (results.isEmpty() || results.stream().anyMatch(PENDING::equals)) return PENDING;
        return "OK";
    }

    private QmsDispatchTaskCandidateItemRespVO toCandidate(QmsIqcItemDO item) {
        QmsDispatchTaskCandidateItemRespVO candidate = candidate(item.getId(), item.getStandardItemId(), null, item.getInspectionItem(),
                item.getStandardDesc(), item.getUnit(), item.getItemType(), item.getAverageValue(), null,
                item.getItemResult(), item.getSort());
        candidate.setInspectionMethod(item.getInspectionMethod());
        candidate.setTestTool(item.getTestTool());
        candidate.setSampleSize(item.getSampleSize());
        return candidate;
    }

    private List<QmsDispatchTaskCandidateItemRespVO> buildIqcSourceItems(Long executionId) {
        List<QmsIqcItemDO> items = iqcItemMapper.selectListByIqcId(executionId);
        Map<Long, List<QmsIqcSampleDO>> samplesByItem = iqcSampleMapper.selectListByIqcId(executionId).stream()
                .collect(Collectors.groupingBy(QmsIqcSampleDO::getIqcItemId, LinkedHashMap::new, Collectors.toList()));
        return items.stream().map(item -> {
            QmsDispatchTaskCandidateItemRespVO candidate = toCandidate(item);
            candidate.setMeasuredData(formatIqcMeasuredData(item, samplesByItem.getOrDefault(item.getId(), List.of())));
            return candidate;
        }).toList();
    }

    private String formatIqcMeasuredData(QmsIqcItemDO item, List<QmsIqcSampleDO> samples) {
        List<Map<String, Object>> sampleValues = samples.stream()
                .sorted(Comparator.comparing(QmsIqcSampleDO::getSampleSeq,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(sample -> formatIqcSampleMeasuredData(item, sample))
                .filter(Objects::nonNull)
                .toList();
        if (sampleValues.isEmpty() && item.getMaxValue() == null && item.getMinValue() == null
                && item.getAverageValue() == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("renderType", IQC_MEASURED_DATA_RENDER_TYPE);
        payload.put("itemType", item.getItemType());
        putIfNotNull(payload, "maxValue", item.getMaxValue());
        putIfNotNull(payload, "minValue", item.getMinValue());
        putIfNotNull(payload, "averageValue", item.getAverageValue());
        putIfNotNull(payload, "minValueLimit", item.getMinValueLimit());
        putIfNotNull(payload, "maxValueLimit", item.getMaxValueLimit());
        payload.put("values", sampleValues);
        return JsonUtils.toJsonString(payload);
    }

    private Map<String, Object> formatIqcSampleMeasuredData(QmsIqcItemDO item, QmsIqcSampleDO sample) {
        Map<String, Object> rawValues = parseIqcRawValues(sample.getRawValuesJson());
        String itemType = normalizeType(item.getItemType());
        String displayValue = ITEM_TYPE_QUANTITATIVE.equals(itemType)
                ? firstNonBlank(text(sample.getResultValue()), text(sample.getMeasuredValue()), rawMeasuredText(rawValues))
                : (ITEM_TYPE_DATE.equals(itemType)
                        ? text(sample.getDateValue())
                        : firstNonBlank(sample.getQualitativeValue(), rawMeasuredText(rawValues), sample.getSampleResult()));
        if (StrUtil.isBlank(displayValue)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", displayValue);
        putIfNotNull(result, "sampleSeq", sample.getSampleSeq());
        putIfNotBlank(result, "samplePosition", objectText(rawValues.get("samplePosition")));
        putIfNotBlank(result, "samplePositionName", objectText(rawValues.get("samplePositionName")));
        putIfNotBlank(result, "samplePositionCode", objectText(rawValues.get("samplePositionCode")));
        putIfNotBlank(result, "sampleGroupNo", objectText(rawValues.get("sampleGroupNo")));
        putIfNotBlank(result, "sampleBarcode", sample.getSampleBarcode());
        putIfNotBlank(result, "sampleResult", sample.getSampleResult());
        putIfNotBlank(result, "qualitativeValue", sample.getQualitativeValue());
        putIfNotNull(result, "measuredValue", sample.getMeasuredValue());
        putIfNotNull(result, "resultValue", sample.getResultValue());
        putIfNotNull(result, "dateValue", sample.getDateValue());
        putIfNotNull(result, "evaluationDate", sample.getEvaluationDate());
        putIfNotBlank(result, "positionText", iqcPositionText(rawValues));
        return result;
    }

    private Map<String, Object> parseIqcRawValues(String rawValuesJson) {
        if (StrUtil.isBlank(rawValuesJson)) {
            return Map.of();
        }
        Map<String, Object> values = JsonUtils.parseObjectQuietly(rawValuesJson, MAP_TYPE);
        return values == null ? Map.of() : values;
    }

    private String rawMeasuredText(Map<String, Object> values) {
        return firstNonBlank(objectText(values.get("value")), objectText(values.get("sampleValue")),
                objectText(values.get("actualValue")), objectText(values.get("measuredValue")),
                objectText(values.get("resultValue")), objectText(values.get("qualitativeValue")),
                objectText(values.get("rawValue")));
    }

    private String iqcPositionText(Map<String, Object> values) {
        String position = firstNonBlank(objectText(values.get("samplePositionName")),
                objectText(values.get("samplePosition")));
        String code = objectText(values.get("samplePositionCode"));
        if (StrUtil.isNotBlank(position) && StrUtil.isNotBlank(code)) {
            return "位置" + position + "(" + code + ")";
        }
        if (StrUtil.isNotBlank(position)) {
            return "位置" + position;
        }
        return StrUtil.isBlank(code) ? null : "位置(" + code + ")";
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private QmsDispatchTaskCandidateItemRespVO toCandidate(QmsFaiItemDO item) {
        QmsDispatchTaskCandidateItemRespVO candidate = candidate(item.getId(), item.getStandardItemId(), null, item.getInspectionItem(),
                item.getStandardDesc(), item.getUnit(), item.getItemType(), item.getOperatorAvg(), null,
                firstNonBlank(item.getQaResult(), item.getOperatorResult()), item.getSort());
        candidate.setInspectionMethod(item.getInspectionMethod());
        candidate.setTestTool(item.getTestTool());
        candidate.setTemplateParams(item.getTemplateParams());
        candidate.setSampleSize(item.getSampleSize());
        candidate.setAvgMinLimit(item.getAvgMinLimit());
        candidate.setAvgMaxLimit(item.getAvgMaxLimit());
        candidate.setStdMinLimit(item.getStdMinLimit());
        candidate.setStdMaxLimit(item.getStdMaxLimit());
        candidate.setMinValueLimit(item.getMinValueLimit());
        candidate.setMaxValueLimit(item.getMaxValueLimit());
        candidate.setAverageValue(firstNonNull(item.getCalculatedAvg(), item.getOperatorAvg()));
        candidate.setStandardDeviation(item.getCalculatedStd());
        return candidate;
    }

    private QmsDispatchTaskCandidateItemRespVO toCandidate(QmsIpqcItemDO item) {
        return candidate(item.getId(), item.getStandardItemId(), null, item.getInspectionItem(),
                item.getStandardDesc(), null, item.getItemType(), item.getAverageValue(), null,
                item.getItemResult(), item.getSort());
    }

    private QmsDispatchTaskCandidateItemRespVO toCandidate(QmsFqcItemDO item) {
        QmsDispatchTaskCandidateItemRespVO candidate = candidate(item.getId(), item.getStandardItemId(), item.getProductionBatchNo(), item.getInspectionItem(),
                item.getStandardDesc(), item.getUnit(), item.getItemType(), item.getOperatorAvg(), null,
                firstNonBlank(item.getQaResult(), item.getOperatorResult(), item.getItemResult()), item.getSort());
        candidate.setInspectionMethod(item.getInspectionMethod());
        candidate.setTestTool(item.getTestTool());
        candidate.setTemplateParams(item.getTemplateParams());
        candidate.setSampleSize(item.getSampleSize());
        candidate.setAvgMinLimit(item.getAvgMinLimit());
        candidate.setAvgMaxLimit(item.getAvgMaxLimit());
        candidate.setStdMinLimit(item.getStdMinLimit());
        candidate.setStdMaxLimit(item.getStdMaxLimit());
        candidate.setMinValueLimit(item.getMinValueLimit());
        candidate.setMaxValueLimit(item.getMaxValueLimit());
        candidate.setAverageValue(firstNonNull(item.getCalculatedAvg(), item.getOperatorAvg(), item.getAverageValue()));
        candidate.setStandardDeviation(item.getCalculatedStd());
        return candidate;
    }

    private QmsDispatchTaskCandidateItemRespVO toCandidate(QmsOqcItemDO item) {
        return candidate(item.getId(), item.getStandardItemId(), null, item.getInspectionItem(),
                item.getStandardDesc(), item.getUnit(), item.getItemType(), item.getOperatorAvg(), null,
                firstNonBlank(item.getQaResult(), item.getOperatorResult(), item.getItemResult()), item.getSort());
    }

    private QmsDispatchTaskCandidateItemRespVO toCandidate(QmsQualityStandardItemDO item) {
        QmsDispatchTaskCandidateItemRespVO candidate = candidate(item.getId(), item.getId(), null,
                item.getInspectionItem(), item.getStandardDesc(),
                item.getUnit(), item.getItemType(), null, null, null, item.getSort());
        candidate.setInspectionMethod(item.getInspectionMethod());
        candidate.setTestTool(item.getTestTool());
        candidate.setTemplateParams(item.getTemplateParams());
        candidate.setSampleSize(item.getSampleSize());
        candidate.setAvgMinLimit(item.getAvgMinLimit());
        candidate.setAvgMaxLimit(item.getAvgMaxLimit());
        candidate.setStdMinLimit(item.getStdMinLimit());
        candidate.setStdMaxLimit(item.getStdMaxLimit());
        candidate.setMinValueLimit(item.getMinValue());
        candidate.setMaxValueLimit(item.getMaxValue());
        return candidate;
    }

    private QmsDispatchTaskCandidateItemRespVO candidate(Long id, Long standardItemId, String pieceNo,
                                                         String name, String desc, String unit, String itemType,
                                                         BigDecimal measuredValue, String qualitativeValue,
                                                         String result, Integer sort) {
        QmsDispatchTaskCandidateItemRespVO item = new QmsDispatchTaskCandidateItemRespVO();
        item.setId(id);
        item.setStandardItemId(standardItemId);
        item.setPieceNo(pieceNo);
        item.setInspectionItem(name);
        item.setStandardDesc(desc);
        item.setUnit(unit);
        item.setItemType(itemType);
        item.setMeasuredValue(measuredValue);
        item.setQualitativeValue(qualitativeValue);
        item.setResult(result);
        item.setCurrentResult(result);
        item.setSort(sort);
        markItemNode(item);
        return item;
    }

    private void resetIqcOrder(QmsIqcOrderDO order) {
        order.setStatus(PENDING); order.setJudgment(PENDING); order.setInspectorId(null); order.setInspectorName(null);
        order.setInspectionTime(null); order.setQaInspectorId(null); order.setQaInspectorName(null); order.setQaTime(null);
        order.setAuditNotifyTime(null); order.setDisposalType(null); order.setReturnCount(0); order.setLastReturnReason(null);
    }

    private void resetFaiOrder(QmsFaiOrderDO order) {
        order.setStatus(PENDING); order.setJudgment(PENDING); order.setOperatorId(null); order.setOperatorName(null);
        order.setOperatorTime(null); order.setQaInspectorId(null); order.setQaInspectorName(null); order.setQaTime(null);
        order.setInspectionTime(null); order.setReleaseResult(null); order.setReleaseTime(null); order.setAuditNotifyTime(null);
        order.setCurrentStepCode(null); order.setEntryProgress(0); order.setCompletedItemCount(0); order.setAbnormalItemCount(0);
        order.setLastSaveTime(null); order.setLastCalculateTime(null); order.setSheetLocked(false); order.setReturnCount(0);
        order.setLastReturnReason(null); order.setRejectFlag(false); order.setRejectPrevInspectionId(null);
        order.setRejectPrevInspectionNo(null); order.setRejectNextInspectionId(null); order.setRejectNextInspectionNo(null);
        order.setRejectRootInspectionId(null); order.setRejectRootInspectionNo(null); order.setRejectRecheckResult(null);
        order.setRejectRecheckTime(null); order.setRejectReason(null); order.setRejectTime(null); order.setRejectUserId(null);
        order.setRejectUserName(null);
    }

    private void resetIpqcOrder(QmsIpqcOrderDO order) {
        order.setStatus(PENDING); order.setJudgment(PENDING); order.setInspectionTime(null); order.setInspectorId(null);
        order.setInspectorName(null); order.setControlAction(null); order.setMachineControlResult(null); order.setNcRecordId(null);
    }

    private void resetFqcOrder(QmsFqcOrderDO order) {
        order.setStatus(PENDING); order.setJudgment(PENDING); order.setInspectorId(null); order.setInspectorName(null);
        order.setInspectionTime(null); order.setQaInspectorId(null); order.setQaInspectorName(null); order.setQaTime(null);
        order.setReleaseResult(null); order.setReleaseTime(null); order.setAuditNotifyTime(null); order.setRelatedNcrNo(null);
        order.setNcrStatus(null); order.setCurrentStepCode(null); order.setEntryProgress(0); order.setCompletedItemCount(0);
        order.setAbnormalItemCount(0); order.setLastSaveTime(null); order.setLastCalculateTime(null); order.setSheetLocked(false);
        order.setReturnCount(0); order.setLastReturnReason(null); order.setRejectFlag(false); order.setRejectPrevInspectionId(null);
        order.setRejectPrevInspectionNo(null); order.setRejectNextInspectionId(null); order.setRejectNextInspectionNo(null);
        order.setRejectRootInspectionId(null); order.setRejectRootInspectionNo(null); order.setRejectRecheckResult(null);
        order.setRejectRecheckTime(null); order.setRejectReason(null); order.setRejectTime(null); order.setRejectUserId(null);
        order.setRejectUserName(null); order.setOkQty(0); order.setNgQty(0);
    }

    private void resetOqcOrder(QmsOqcOrderDO order) {
        order.setStatus(PENDING); order.setJudgment(PENDING); order.setInspectorId(null); order.setInspectorName(null);
        order.setInspectionTime(null); order.setQaInspectorId(null); order.setQaInspectorName(null); order.setQaTime(null);
        order.setReleaseResult(null); order.setReleaseTime(null); order.setAuditNotifyTime(null); order.setRelatedNcrNo(null);
        order.setNcrStatus(null); order.setEntryProgress(0); order.setCompletedItemCount(0); order.setAbnormalItemCount(0);
        order.setLastSaveTime(null); order.setLastCalculateTime(null); order.setSheetLocked(false); order.setRejectFlag(false);
        order.setRejectPrevInspectionId(null); order.setRejectPrevInspectionNo(null); order.setRejectNextInspectionId(null);
        order.setRejectNextInspectionNo(null); order.setRejectRootInspectionId(null); order.setRejectRootInspectionNo(null);
        order.setRejectRecheckResult(null); order.setRejectRecheckTime(null); order.setRejectReason(null); order.setRejectTime(null);
        order.setRejectUserId(null); order.setRejectUserName(null);
    }

    private void resetIqcItem(QmsIqcItemDO item) {
        item.setJudgmentReason("");
        item.setMaxValue(null); item.setMinValue(null); item.setAverageValue(null); item.setItemResult(PENDING);
        item.setAttachmentUrls(null);
    }

    private void resetFaiItem(QmsFaiItemDO item) {
        item.setOperatorMax(null); item.setOperatorMin(null); item.setOperatorAvg(null); item.setOperatorResult(PENDING);
        item.setOperatorId(null); item.setOperatorName(null); item.setOperatorTime(null); item.setQaMax(null); item.setQaMin(null);
        item.setQaAvg(null); item.setQaResult(PENDING); item.setQaInspectorId(null); item.setQaInspectorName(null);
        item.setQaTime(null); item.setCalculatedAvg(null); item.setCalculatedStd(null); item.setCalculatedMin(null);
        item.setCalculatedMax(null); item.setCellCompletedCount(0); item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0); item.setInputStatus(PENDING);
    }

    private void resetIpqcItem(QmsIpqcItemDO item) {
        item.setMaxValue(null); item.setMinValue(null); item.setAverageValue(null); item.setItemResult(PENDING);
    }

    private void resetFqcItem(QmsFqcItemDO item) {
        item.setMaxValue(null); item.setMinValue(null); item.setAverageValue(null); item.setItemResult(PENDING);
        item.setOperatorMax(null); item.setOperatorMin(null); item.setOperatorAvg(null); item.setOperatorResult(PENDING);
        item.setOperatorId(null); item.setOperatorName(null); item.setOperatorTime(null); item.setQaMax(null); item.setQaMin(null);
        item.setQaAvg(null); item.setQaResult(PENDING); item.setQaInspectorId(null); item.setQaInspectorName(null);
        item.setQaTime(null); item.setCalculatedAvg(null); item.setCalculatedStd(null); item.setCalculatedMin(null);
        item.setCalculatedMax(null); item.setCellCompletedCount(0); item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0); item.setInputStatus(PENDING);
    }

    private void resetOqcItem(QmsOqcItemDO item) {
        item.setMaxValue(null); item.setMinValue(null); item.setAverageValue(null); item.setItemResult(PENDING);
        item.setOperatorMax(null); item.setOperatorMin(null); item.setOperatorAvg(null); item.setOperatorResult(PENDING);
        item.setOperatorId(null); item.setOperatorName(null); item.setOperatorTime(null); item.setQaMax(null); item.setQaMin(null);
        item.setQaAvg(null); item.setQaResult(PENDING); item.setQaInspectorId(null); item.setQaInspectorName(null);
        item.setQaTime(null); item.setCalculatedAvg(null); item.setCalculatedStd(null); item.setCalculatedMin(null);
        item.setCalculatedMax(null); item.setCompletedSampleCount(0); item.setAbnormalSampleCount(0); item.setInputStatus(PENDING);
    }

    private void resetIqcSample(QmsIqcSampleDO sample) {
        sample.setRawValuesJson(null); sample.setResultValue(null); sample.setMeasuredValue(null);
        sample.setQualitativeValue(null); sample.setDateValue(null); sample.setEvaluationDate(null);
        sample.setSampleResult(PENDING); sample.setDefectCode(null); sample.setDefectName(null);
    }

    private void resetFaiSample(QmsFaiSampleDO sample) {
        sample.setRawValuesJson(null); sample.setResultValue(null); sample.setDensityValue(null); sample.setCompressionRate(null);
        sample.setCompressionElasticityRate(null); sample.setMeasuredValue(null); sample.setQualitativeValue(null);
        sample.setSampleResult(PENDING); sample.setInputTime(null); sample.setDefectCode(null); sample.setDefectName(null);
    }

    private void resetIpqcSample(QmsIpqcSampleDO sample) {
        sample.setMeasuredValue(null); sample.setQualitativeValue(null); sample.setSampleResult(PENDING);
        sample.setDefectCode(null); sample.setDefectName(null);
    }

    private void resetFqcSample(QmsFqcSampleDO sample) {
        sample.setRawValuesJson(null); sample.setResultValue(null); sample.setDensityValue(null); sample.setCompressionRate(null);
        sample.setCompressionElasticityRate(null); sample.setMeasuredValue(null); sample.setQualitativeValue(null);
        sample.setSampleResult(PENDING); sample.setInputTime(null); sample.setDefectCode(null); sample.setDefectName(null);
    }

    private void resetFqcShippingDetail(QmsFqcShippingDetailDO detail) {
        detail.setRowJudgment(PENDING);
        detail.setDefectCode(null);
        detail.setDefectName(null);
        detail.setNgReason(null);
        detail.setMismatchReason(null);
        detail.setInspectorId(null);
        detail.setInspectorName(null);
        detail.setInspectionTime(null);
        detail.setRemark(null);
    }

    private void resetFqcCell(QmsFqcSheetCellValueDO cell) {
        cell.setRawValue(null);
        cell.setNumericValue(null);
        cell.setTextValue(null);
        cell.setValueSource(null);
        cell.setCellStatus(PENDING);
        cell.setJudgmentResult(PENDING);
        cell.setImportBatchNo(null);
        cell.setInputUserId(null);
        cell.setInputUserName(null);
        cell.setInputTime(null);
    }

    private void resetOqcSample(QmsOqcSampleDO sample) {
        sample.setRawValuesJson(null); sample.setResultValue(null); sample.setMeasuredValue(null);
        sample.setQualitativeValue(null); sample.setSampleResult(PENDING); sample.setInputTime(null);
    }

    private QmsIqcItemDO standardIqcItem(QmsIqcOrderDO order, QmsQualityStandardItemDO standard) {
        QmsIqcItemDO item = BeanUtils.toBean(standard, QmsIqcItemDO.class);
        item.setId(null); item.setIqcId(order.getId()); item.setIqcNo(order.getIqcNo()); item.setStandardItemId(standard.getId());
        item.setMinValueLimit(standard.getMinValue()); item.setMaxValueLimit(standard.getMaxValue()); item.setItemResult(PENDING);
        return item;
    }

    private QmsFaiItemDO standardFaiItem(QmsFaiOrderDO order, QmsQualityStandardItemDO standard) {
        QmsFaiItemDO item = BeanUtils.toBean(standard, QmsFaiItemDO.class);
        item.setId(null); item.setFaiId(order.getId()); item.setFaiNo(order.getFaiNo()); item.setStandardItemId(standard.getId());
        item.setMinValueLimit(standard.getMinValue()); item.setMaxValueLimit(standard.getMaxValue()); item.setOperatorResult(PENDING);
        item.setQaResult(PENDING); item.setInputStatus(PENDING); item.setRequiredSampleCount(sampleCount(standard));
        item.setCompletedSampleCount(0); item.setAbnormalSampleCount(0);
        return item;
    }

    private QmsIpqcItemDO standardIpqcItem(QmsIpqcOrderDO order, QmsQualityStandardItemDO standard) {
        QmsIpqcItemDO item = BeanUtils.toBean(standard, QmsIpqcItemDO.class);
        item.setId(null); item.setIpqcId(order.getId()); item.setIpqcNo(order.getIpqcNo()); item.setStandardItemId(standard.getId());
        item.setMinValueLimit(standard.getMinValue()); item.setMaxValueLimit(standard.getMaxValue()); item.setItemResult(PENDING);
        return item;
    }

    private QmsFqcItemDO standardFqcItem(QmsFqcOrderDO order, QmsQualityStandardItemDO standard) {
        QmsFqcItemDO item = BeanUtils.toBean(standard, QmsFqcItemDO.class);
        item.setId(null); item.setFqcId(order.getId()); item.setFqcNo(order.getFqcNo()); item.setStandardItemId(standard.getId());
        item.setMinValueLimit(standard.getMinValue()); item.setMaxValueLimit(standard.getMaxValue()); item.setItemResult(PENDING);
        item.setOperatorResult(PENDING); item.setQaResult(PENDING); item.setInputStatus(PENDING);
        item.setRequiredSampleCount(sampleCount(standard)); item.setCompletedSampleCount(0); item.setAbnormalSampleCount(0);
        return item;
    }

    private QmsOqcItemDO standardOqcItem(QmsOqcOrderDO order, QmsQualityStandardItemDO standard) {
        QmsOqcItemDO item = BeanUtils.toBean(standard, QmsOqcItemDO.class);
        item.setId(null); item.setOqcId(order.getId()); item.setOqcNo(order.getOqcNo()); item.setStandardItemId(standard.getId());
        item.setMinValueLimit(standard.getMinValue()); item.setMaxValueLimit(standard.getMaxValue()); item.setItemResult(PENDING);
        item.setOperatorResult(PENDING); item.setQaResult(PENDING); item.setInputStatus(PENDING);
        item.setRequiredSampleCount(sampleCount(standard)); item.setCompletedSampleCount(0); item.setAbnormalSampleCount(0);
        return item;
    }

    private void insertIqcSamples(QmsIqcOrderDO order, QmsIqcItemDO item) {
        for (int index = 1; index <= Math.max(1, item.getSampleSize() == null ? 1 : item.getSampleSize()); index++) {
            QmsIqcSampleDO sample = new QmsIqcSampleDO(); sample.setIqcId(order.getId()); sample.setIqcItemId(item.getId());
            sample.setIqcNo(order.getIqcNo()); sample.setSampleSeq(index); sample.setSampleResult(PENDING); iqcSampleMapper.insert(sample);
        }
    }

    private void insertFaiSamples(QmsFaiOrderDO order, QmsFaiItemDO item) {
        for (int index = 1; index <= Math.max(1, item.getRequiredSampleCount()); index++) {
            QmsFaiSampleDO sample = new QmsFaiSampleDO(); sample.setFaiId(order.getId()); sample.setFaiItemId(item.getId());
            sample.setFaiNo(order.getFaiNo()); sample.setSampleSeq(index); sample.setSampleResult(PENDING); faiSampleMapper.insert(sample);
        }
    }

    private void insertIpqcSamples(QmsIpqcOrderDO order, QmsIpqcItemDO item) {
        for (int index = 1; index <= Math.max(1, item.getSampleSize() == null ? 1 : item.getSampleSize()); index++) {
            QmsIpqcSampleDO sample = new QmsIpqcSampleDO(); sample.setIpqcId(order.getId()); sample.setIpqcItemId(item.getId());
            sample.setIpqcNo(order.getIpqcNo()); sample.setSampleSeq(index); sample.setSampleResult(PENDING); ipqcSampleMapper.insert(sample);
        }
    }

    private void insertFqcSamples(QmsFqcOrderDO order, QmsFqcItemDO item) {
        for (int index = 1; index <= Math.max(1, item.getRequiredSampleCount()); index++) {
            QmsFqcSampleDO sample = new QmsFqcSampleDO(); sample.setFqcId(order.getId()); sample.setFqcItemId(item.getId());
            sample.setFqcNo(order.getFqcNo()); sample.setSampleSeq(index); sample.setSampleResult(PENDING); fqcSampleMapper.insert(sample);
        }
    }

    private void insertOqcSamples(QmsOqcOrderDO order, QmsOqcItemDO item) {
        for (int index = 1; index <= Math.max(1, item.getRequiredSampleCount()); index++) {
            QmsOqcSampleDO sample = new QmsOqcSampleDO(); sample.setOqcId(order.getId()); sample.setOqcItemId(item.getId());
            sample.setOqcNo(order.getOqcNo()); sample.setSampleSeq(index); sample.setSampleResult(PENDING); oqcSampleMapper.insert(sample);
        }
    }

    private void applyCommon(QmsIqcOrderDO order, QmsDispatchTaskWizardCreateReqVO req, QmsQualityStandardDO standard) {
        order.setMaterialId(req.getMaterialId()); order.setMaterialCode(req.getMaterialCode()); order.setMaterialName(req.getMaterialName());
        order.setSpecification(req.getSpecification()); order.setProductModelId(req.getProductModelId());
        order.setProductModelCode(req.getProductModelCode()); order.setProductModelName(req.getProductModelName());
        applyStandard(order, standard);
    }

    private void applyCommon(QmsFaiOrderDO order, QmsDispatchTaskWizardCreateReqVO req, QmsQualityStandardDO standard) {
        order.setMaterialId(req.getMaterialId()); order.setMaterialCode(req.getMaterialCode()); order.setMaterialName(req.getMaterialName());
        order.setSpecification(req.getSpecification()); order.setProductModel(req.getProductModelCode()); applyStandard(order, standard);
    }

    private void applyCommon(QmsIpqcOrderDO order, QmsDispatchTaskWizardCreateReqVO req, QmsQualityStandardDO standard) {
        order.setMaterialId(req.getMaterialId()); order.setMaterialCode(req.getMaterialCode()); order.setMaterialName(req.getMaterialName());
        order.setSpecification(req.getSpecification()); applyStandard(order, standard);
    }

    private void applyCommon(QmsFqcOrderDO order, QmsDispatchTaskWizardCreateReqVO req, QmsQualityStandardDO standard) {
        order.setMaterialId(req.getMaterialId()); order.setMaterialCode(req.getMaterialCode()); order.setMaterialName(req.getMaterialName());
        order.setSpecification(req.getSpecification()); order.setProductModel(req.getProductModelCode()); applyStandard(order, standard);
    }

    private void applyCommon(QmsOqcOrderDO order, QmsDispatchTaskWizardCreateReqVO req, QmsQualityStandardDO standard) {
        order.setMaterialId(req.getMaterialId()); order.setMaterialCode(req.getMaterialCode()); order.setMaterialName(req.getMaterialName());
        order.setSpecification(req.getSpecification()); order.setModelCode(req.getProductModelCode()); applyStandard(order, standard);
    }

    private void applyStandard(QmsIqcOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId()); order.setStandardNo(standard.getStandardNo());
        order.setStandardName(standard.getStandardName()); order.setStandardVersion(standard.getVersion());
    }
    private void applyStandard(QmsFaiOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId()); order.setStandardNo(standard.getStandardNo()); order.setStandardVersion(standard.getVersion());
    }
    private void applyStandard(QmsIpqcOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId()); order.setStandardNo(standard.getStandardNo()); order.setStandardVersion(standard.getVersion());
    }
    private void applyStandard(QmsFqcOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId()); order.setStandardNo(standard.getStandardNo()); order.setStandardVersion(standard.getVersion());
    }
    private void applyStandard(QmsOqcOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId()); order.setStandardNo(standard.getStandardNo()); order.setStandardVersion(standard.getVersion());
    }

    private QmsQualityStandardDO validateStandard(Long id) {
        QmsQualityStandardDO standard = id == null ? null : standardMapper.selectById(id);
        if (standard == null || !Objects.equals(standard.getStatus(), STANDARD_STATUS_ENABLED)
                || !Objects.equals(standard.getAuditStatus(), STANDARD_AUDIT_STATUS_AUDITED)) {
            throw exception(QMS_DISPATCH_STANDARD_INVALID);
        }
        return standard;
    }

    private QmsQualityStandardDO validateStandard(Long id, String checkType) {
        QmsQualityStandardDO standard = validateStandard(id);
        if (!normalizeType(checkType).equals(normalizeType(standard.getApplyType()))) {
            throw exception(QMS_DISPATCH_STANDARD_INVALID);
        }
        return standard;
    }

    private void validateSelected(List<Long> available, Set<Long> selected) {
        if (selected.isEmpty() || !new HashSet<>(available).containsAll(selected)) {
            throw exception(QMS_DISPATCH_ITEM_INVALID);
        }
    }

    private <T> T required(T value, String checkType, Long id) {
        if (value == null) throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, checkType, id);
        return value;
    }

    private QmsFaiOrderDO requiredFaiOrder(Long id, String checkType) {
        QmsFaiOrderDO order = required(faiOrderMapper.selectById(id), checkType, id);
        boolean glueBoardOrder = GLUE_BOARD_FAI.equals(normalizeType(order.getSourceModule()));
        boolean glueBoardRequest = GLUE_BOARD_FAI.equals(normalizeType(checkType));
        if (glueBoardOrder != glueBoardRequest) {
            throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, checkType, id);
        }
        return order;
    }

    private <T extends BaseDO> T copy(T source, Class<T> type) {
        T target = BeanUtils.toBean(source, type);
        target.clean();
        target.setDeleted(false);
        return target;
    }

    private <T, R> PageResult<R> mapPage(PageResult<T> page, Function<T, R> converter) {
        return new PageResult<>(page.getList().stream().map(converter).toList(), page.getTotal());
    }

    private <T> BigDecimal countNg(List<T> items, Function<T, String> resultGetter) {
        return BigDecimal.valueOf(items.stream().map(resultGetter).filter("NG"::equalsIgnoreCase).count());
    }

    private <T> String ngSummary(List<T> items, Function<T, String> nameGetter, Function<T, String> resultGetter) {
        String summary = items.stream().filter(item -> "NG".equalsIgnoreCase(resultGetter.apply(item)))
                .map(nameGetter).filter(StrUtil::isNotBlank).distinct().limit(5).collect(Collectors.joining("、"));
        return StrUtil.blankToDefault(summary, "-");
    }

    private BigDecimal decimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private BigDecimal fqcSubmittedQty(QmsFqcOrderDO order) {
        return BigDecimal.valueOf(Objects.requireNonNullElse(order.getOkQty(), 0))
                .add(BigDecimal.valueOf(Objects.requireNonNullElse(order.getNgQty(), 0)));
    }

    private BigDecimal resolveFaiRecheckQty(QmsFaiOrderDO source, BigDecimal requestedInspectionQty) {
        if (requestedInspectionQty == null) {
            return source.getInspectionQty();
        }
        if (requestedInspectionQty.signum() <= 0 || requestedInspectionQty.stripTrailingZeros().scale() > 0) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "复检片数必须为大于零的整数");
        }
        BigDecimal sourceInspectionQty = source.getInspectionQty();
        if (sourceInspectionQty == null || sourceInspectionQty.signum() <= 0) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "原过程首检送检片数无效");
        }
        if (requestedInspectionQty.compareTo(sourceInspectionQty) > 0) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID,
                    "复检片数不能超过原送检片数 " + sourceInspectionQty.stripTrailingZeros().toPlainString());
        }
        return requestedInspectionQty;
    }

    private int resolveFqcRecheckQty(QmsFqcOrderDO source, BigDecimal requestedInspectionQty) {
        BigDecimal sourceInspectionQty = fqcSubmittedQty(source);
        // 任务流程关闭不会回写 FQC 的 OK/NG 数；旧的确认退回链路未显式传入复检片数时，
        // 兼容使用原单 sampleQty。向导显式填写片数仍严格以 OK+NG 为上限。
        if (requestedInspectionQty == null && sourceInspectionQty.signum() <= 0
                && source.getSampleQty() != null && source.getSampleQty() > 0) {
            sourceInspectionQty = BigDecimal.valueOf(source.getSampleQty());
        }
        if (sourceInspectionQty.signum() <= 0 || sourceInspectionQty.stripTrailingZeros().scale() > 0) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "原成品检验送检片数无效");
        }
        BigDecimal quantity = requestedInspectionQty == null ? sourceInspectionQty : requestedInspectionQty;
        if (quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 0) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "复检片数必须为大于零的整数");
        }
        if (quantity.compareTo(sourceInspectionQty) > 0) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID,
                    "复检片数不能超过原送检片数 " + sourceInspectionQty.stripTrailingZeros().toPlainString());
        }
        return quantity.intValueExact();
    }

    private int sampleCount(QmsQualityStandardItemDO item) {
        return Math.max(1, item.getSampleSize() == null ? 1 : item.getSampleSize());
    }

    private void markItemNode(QmsDispatchTaskCandidateItemRespVO item) {
        item.setSourceItemId(item.getId());
        item.setNodeKey("ITEM:" + item.getId());
        item.setNodeType("ITEM");
        item.setSelectable(true);
    }

    private Map<Long, Set<String>> selectedPositionMap(List<QmsDispatchTaskItemSelectionReqVO> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> result = new HashMap<>();
        for (QmsDispatchTaskItemSelectionReqVO scope : scopes) {
            if (scope == null || scope.getItemId() == null) {
                throw exception(QMS_DISPATCH_ITEM_INVALID);
            }
            String scopeType = normalizeType(scope.getScopeType());
            if ("ITEM".equals(scopeType)) {
                result.put(scope.getItemId(), Set.of());
                continue;
            }
            if (!"POSITION".equals(scopeType) || scope.getPositions() == null) {
                throw exception(QMS_DISPATCH_ITEM_INVALID);
            }
            if (result.containsKey(scope.getItemId()) && result.get(scope.getItemId()).isEmpty()) {
                continue;
            }
            Set<String> positions = scope.getPositions().stream().map(StrUtil::trim)
                    .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
            if (positions.isEmpty()) {
                throw exception(QMS_DISPATCH_ITEM_INVALID);
            }
            result.computeIfAbsent(scope.getItemId(), ignored -> new HashSet<>()).addAll(positions);
        }
        return result;
    }

    private void validateScopeItems(Set<Long> selected, Map<Long, Set<String>> selectedPositions) {
        if (!selected.containsAll(selectedPositions.keySet())) {
            throw exception(QMS_DISPATCH_ITEM_INVALID);
        }
    }

    private void validateSelectedFaiPositions(Set<Long> selected, Map<Long, Set<String>> selectedPositions,
                                              List<QmsFaiSampleDO> sourceSamples) {
        validateScopeItems(selected, selectedPositions);
        Map<Long, Set<String>> availablePositions = sourceSamples.stream()
                .filter(sample -> selected.contains(sample.getFaiItemId()))
                .collect(Collectors.groupingBy(QmsFaiSampleDO::getFaiItemId,
                        Collectors.mapping(this::faiPositionKey, Collectors.toSet())));
        selectedPositions.forEach((itemId, positions) -> {
            if (!positions.isEmpty()
                    && !availablePositions.getOrDefault(itemId, Set.of()).containsAll(positions)) {
                throw exception(QMS_DISPATCH_ITEM_INVALID);
            }
        });
    }

    private Set<String> standardPositionCodes(QmsQualityStandardItemDO item) {
        Map<String, Object> params = QmsFaiRuleCalculationSupport.parseRawValues(item.getTemplateParams());
        Object rawPositions = params.get("positions");
        if (!(rawPositions instanceof Collection<?> positions) || positions.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        int index = 0;
        for (Object rawPosition : positions) {
            index++;
            Map<?, ?> positionMap = rawPosition instanceof Map<?, ?> map ? map : Map.of();
            result.add(StrUtil.blankToDefault(text(positionMap.get("code")), "P" + index));
        }
        return result;
    }

    private boolean shouldResetFaiSample(QmsFaiSampleDO sample, Set<Long> selected,
                                         Map<Long, Set<String>> selectedPositions) {
        if (!selected.contains(sample.getFaiItemId())) {
            return false;
        }
        Set<String> positions = selectedPositions.get(sample.getFaiItemId());
        return positions == null || positions.isEmpty() || positions.contains(faiPositionKey(sample));
    }

    private String faiPositionKey(QmsFaiSampleDO sample) {
        return StrUtil.blankToDefault(StrUtil.trim(sample.getSamplePosition()),
                "位置" + (sample.getSampleSeq() == null ? 1 : sample.getSampleSeq()));
    }

    private String fqcPieceKey(QmsFqcSampleDO sample) {
        return firstNonBlank(sample.getProductionBatchNo(), sample.getParentProductionBatchNo(),
                sample.getSliceSeqNo() == null ? null : "第" + sample.getSliceSeqNo() + "片",
                sample.getSampleSeq() == null ? null : "样本" + sample.getSampleSeq(), "未指定片号");
    }

    private String oqcPieceKey(QmsOqcSampleDO sample) {
        return firstNonBlank(sample.getSamplePosition(),
                sample.getSampleSeq() == null ? null : "样本" + sample.getSampleSeq(), "未指定片号");
    }

    private String fqcItemGroupKey(QmsFqcItemDO item) {
        return normalizeType(item.getInspectionItem()) + "\u0001"
                + normalizeType(item.getStandardDesc());
    }

    private BigDecimal faiSampleValue(QmsFaiSampleDO sample) {
        return firstNonNull(sample.getResultValue(), sample.getMeasuredValue(), sample.getDensityValue(),
                sample.getCompressionRate(), sample.getCompressionElasticityRate());
    }

    private BigDecimal faiMeasuredValue(QmsFaiSampleDO sample) {
        return firstNonNull(sample.getMeasuredValue(), sample.getDensityValue(), sample.getCompressionRate(),
                sample.getCompressionElasticityRate());
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private BigDecimal standardDeviation(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal avg = average(values);
        double variance = values.stream().mapToDouble(value -> {
            double delta = value.subtract(avg).doubleValue();
            return delta * delta;
        }).sum() / values.size();
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(6, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private String joinDistinct(List<String> values) {
        String joined = values.stream().filter(StrUtil::isNotBlank).distinct().collect(Collectors.joining("、"));
        return StrUtil.blankToDefault(joined, null);
    }

    private void applyStandard(QmsDispatchTaskSourceRespVO result, Long standardId, String standardNo) {
        result.setStandardId(standardId);
        result.setStandardNo(standardNo);
        if (standardId == null) {
            return;
        }
        QmsQualityStandardDO standard = standardMapper.selectById(standardId);
        if (standard != null) {
            result.setStandardNo(StrUtil.blankToDefault(standardNo, standard.getStandardNo()));
            result.setStandardName(standard.getStandardName());
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String objectText(Object value) {
        return text(value);
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (StrUtil.isNotBlank(value)) return value;
        return null;
    }

    private String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isShippingFqcExecution(Long executionId) {
        return isShippingFqc(executionId == null ? null : fqcOrderMapper.selectById(executionId));
    }

    private boolean isShippingFqc(QmsFqcOrderDO order) {
        return order != null && QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC
                .equalsIgnoreCase(StrUtil.trimToEmpty(order.getSourceModule()));
    }

    private GeneratedInspection generated(String type, Long sourceId, String sourceNo, Long id, String no,
                                            List<GeneratedItem> items) {
        return new GeneratedInspection(type, sourceId, sourceNo, id, no,
                nativeExecutionRoute(type), List.copyOf(items));
    }

    private GeneratedInspection generated(String type, Long sourceId, String sourceNo, Long id, String no,
                                            String route, List<GeneratedItem> items) {
        return new GeneratedInspection(type, sourceId, sourceNo, id, no, route, List.copyOf(items));
    }

    private String nativeExecutionRoute(String type) {
        return switch (normalizeType(type)) {
            case "FAI" -> "/mes/quality/fai";
            case GLUE_BOARD_FAI -> "/mes/quality/glue-board-fai";
            case "FQC" -> "/mes/quality/fqc";
            case "IPQC" -> "/mes/quality/ipqc";
            case "IQC" -> "/mes/quality/iqc";
            case "OQC" -> "/mes/quality/oqc";
            default -> "/mes/quality/task-center/detail";
        };
    }

    private GeneratedItem generated(QmsIqcItemDO source, QmsIqcItemDO target) {
        return item(source.getId(), target.getId(), source.getStandardItemId(), null, source.getInspectionItem(),
                source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsFaiItemDO source, QmsFaiItemDO target) {
        return item(source.getId(), target.getId(), source.getStandardItemId(), null, source.getInspectionItem(),
                source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsIpqcItemDO source, QmsIpqcItemDO target) {
        return item(source.getId(), target.getId(), source.getStandardItemId(), null, source.getInspectionItem(),
                source.getStandardDesc(), null, source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsFqcItemDO source, QmsFqcItemDO target) {
        return item(source.getId(), target.getId(), source.getStandardItemId(), source.getProductionBatchNo(),
                source.getInspectionItem(), source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsOqcItemDO source, QmsOqcItemDO target) {
        return item(source.getId(), target.getId(), source.getStandardItemId(), null, source.getInspectionItem(),
                source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsQualityStandardItemDO source, QmsIqcItemDO target) {
        return item(null, target.getId(), source.getId(), null, source.getInspectionItem(), source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsQualityStandardItemDO source, QmsFaiItemDO target) {
        return item(null, target.getId(), source.getId(), null, source.getInspectionItem(), source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsQualityStandardItemDO source, QmsIpqcItemDO target) {
        return item(null, target.getId(), source.getId(), null, source.getInspectionItem(), source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsQualityStandardItemDO source, QmsFqcItemDO target) {
        return item(null, target.getId(), source.getId(), null, source.getInspectionItem(), source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem generated(QmsQualityStandardItemDO source, QmsOqcItemDO target) {
        return item(null, target.getId(), source.getId(), null, source.getInspectionItem(), source.getStandardDesc(), source.getUnit(), source.getItemType(), source.getSort());
    }
    private GeneratedItem item(Long sourceItemId, Long executionItemId, Long standardItemId, String pieceNo,
                               String name, String desc, String unit, String itemType, Integer sort) {
        return new GeneratedItem(sourceItemId, executionItemId, standardItemId, pieceNo, name, desc, unit,
                itemType, null, null, null, sort);
    }

    public record GeneratedInspection(String checkType, Long sourceExecutionId, String sourceExecutionNo,
                                      Long executionId, String executionNo, String executionRoute,
                                      List<GeneratedItem> items) {
    }

    public record GeneratedItem(Long sourceItemId, Long executionItemId, Long standardItemId, String pieceNo,
                                String inspectionItem, String standardDesc, String itemUnit, String itemType,
                                String inspectionMethod, String testTool, Integer sampleSize, Integer sort) {
    }

    public record TaskOwnedDefinition(QmsQualityStandardDO standard, List<GeneratedItem> items) {
    }
}
