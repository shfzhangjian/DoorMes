package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_FINISHED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_ITEMS_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_ITEM_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_STANDARD_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIPQC_STANDARD_NOT_EXISTS;

@Service
@Validated
public class QmsIpqcServiceImpl implements QmsIpqcService {

    private static final String APPLY_TYPE_IPQC = "IPQC";
    private static final Integer ENABLED = 1;
    private static final Integer AUDITED = 20;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ABNORMAL = "ABNORMAL";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String PROCESS_PENDING = "PENDING";
    private static final String REPORT_ONLY = "REPORT_ONLY";
    private static final String PAUSE_MACHINE = "PAUSE_MACHINE";

    @Resource
    private QmsIpqcOrderMapper qmsIpqcOrderMapper;
    @Resource
    private QmsIpqcItemMapper qmsIpqcItemMapper;
    @Resource
    private QmsIpqcSampleMapper qmsIpqcSampleMapper;
    @Resource
    private QmsIpqcAbnormalMapper qmsIpqcAbnormalMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    private QmsAbnormalLockService qmsAbnormalLockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createIpqc(QmsIpqcSaveReqVO createReqVO) {
        String ipqcNo = StringUtils.hasText(createReqVO.getIpqcNo()) ? createReqVO.getIpqcNo().trim() : generateIpqcNo();
        validateIpqcNoUnique(null, ipqcNo);
        QmsQualityStandardDO standard = selectIpqcStandard(createReqVO);
        List<QmsQualityStandardItemDO> standardItems = selectStandardItems(standard.getId());
        QmsIpqcOrderDO entity = BeanUtils.toBean(createReqVO, QmsIpqcOrderDO.class);
        entity.setId(null);
        entity.setIpqcNo(ipqcNo);
        applyStandardSnapshot(entity, standard);
        entity.setStatus(defaultIfBlank(entity.getStatus(), STATUS_PENDING));
        entity.setJudgment(defaultIfBlank(entity.getJudgment(), JUDGMENT_PENDING));
        qmsIpqcOrderMapper.insert(entity);
        saveStandardSnapshotDetails(entity, standardItems, createReqVO.getAbnormals());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIpqc(QmsIpqcSaveReqVO updateReqVO) {
        QmsIpqcOrderDO old = validateIpqcExists(updateReqVO.getId());
        validateEditable(old);
        String ipqcNo = StringUtils.hasText(updateReqVO.getIpqcNo()) ? updateReqVO.getIpqcNo().trim() : old.getIpqcNo();
        validateIpqcNoUnique(updateReqVO.getId(), ipqcNo);
        QmsIpqcOrderDO updateObj = BeanUtils.toBean(updateReqVO, QmsIpqcOrderDO.class);
        updateObj.setIpqcNo(ipqcNo);
        updateObj.setStatus(defaultIfBlank(updateReqVO.getStatus(), old.getStatus()));
        updateObj.setJudgment(defaultIfBlank(updateReqVO.getJudgment(), old.getJudgment()));
        qmsIpqcOrderMapper.updateById(updateObj);
        if (updateReqVO.getItems() != null && !updateReqVO.getItems().isEmpty()) {
            updateExecutionDetails(validateIpqcExists(updateReqVO.getId()), updateReqVO.getItems(), updateReqVO.getAbnormals());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitIpqc(QmsIpqcSaveReqVO submitReqVO) {
        QmsIpqcOrderDO entity = validateIpqcExists(submitReqVO.getId());
        validateEditable(entity);
        updateExecutionDetails(entity, submitReqVO.getItems(), submitReqVO.getAbnormals());
        List<QmsIpqcItemDO> items = qmsIpqcItemMapper.selectListByIpqcId(entity.getId());
        String finalJudgment = calculateOrderJudgment(items);
        LocalDateTime now = LocalDateTime.now();

        QmsIpqcOrderDO updateObj = new QmsIpqcOrderDO();
        updateObj.setId(entity.getId());
        updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setInspectorName(resolveLoginUserName());
        updateObj.setInspectionTime(now);
        updateObj.setRemark(StringUtils.hasText(submitReqVO.getRemark()) ? submitReqVO.getRemark() : entity.getRemark());
        updateObj.setControlAction(defaultIfBlank(submitReqVO.getControlAction(), REPORT_ONLY));
        updateObj.setMachineControlResult(JUDGMENT_NG.equals(finalJudgment) && PAUSE_MACHINE.equals(updateObj.getControlAction()) ? "PAUSED" : "RUNNING");
        updateObj.setStatus(JUDGMENT_OK.equals(finalJudgment) ? STATUS_COMPLETED : STATUS_ABNORMAL);
        updateObj.setJudgment(finalJudgment);
        if (JUDGMENT_OK.equals(finalJudgment) && entity.getNextInspectionTime() == null) {
            updateObj.setNextInspectionTime(now.plusHours(2));
        }
        qmsIpqcOrderMapper.updateById(updateObj);
        if (JUDGMENT_NG.equals(finalJudgment)) {
            ensureNgAbnormal(entity.getId(), entity.getIpqcNo(), items, updateObj.getControlAction());
            qmsAbnormalLockService.syncFromIpqc(entity.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendIpqc(Long id) {
        QmsIpqcOrderDO entity = validateIpqcExists(id);
        validateEditable(entity);
        QmsIpqcOrderDO updateObj = new QmsIpqcOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SUSPENDED);
        qmsIpqcOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIpqc(Long id) {
        QmsIpqcOrderDO entity = validateIpqcExists(id);
        validateEditable(entity);
        qmsIpqcSampleMapper.deleteByIpqcId(id);
        qmsIpqcAbnormalMapper.deleteByIpqcId(id);
        qmsIpqcItemMapper.deleteByIpqcId(id);
        qmsIpqcOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIpqcList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> validateEditable(validateIpqcExists(id)));
        qmsIpqcSampleMapper.deleteByIpqcIds(ids);
        qmsIpqcAbnormalMapper.deleteByIpqcIds(ids);
        qmsIpqcItemMapper.deleteByIpqcIds(ids);
        qmsIpqcOrderMapper.deleteBatchIds(ids);
    }

    @Override
    public QmsIpqcRespVO getIpqcResp(Long id) {
        QmsIpqcOrderDO entity = validateIpqcExists(id);
        QmsIpqcRespVO respVO = BeanUtils.toBean(entity, QmsIpqcRespVO.class);
        fillDetails(respVO);
        return respVO;
    }

    @Override
    public PageResult<QmsIpqcOrderDO> getIpqcPage(QmsIpqcPageReqVO pageReqVO) {
        return qmsIpqcOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<QmsIpqcRespVO> getPendingIpqcList() {
        List<QmsIpqcOrderDO> list = qmsIpqcOrderMapper.selectListByStatuses(List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_SUSPENDED));
        return BeanUtils.toBean(list, QmsIpqcRespVO.class);
    }

    @Override
    public QmsIpqcStandardRespVO getIpqcStandard(String materialCode, String operationCode, String operationName) {
        QmsIpqcSaveReqVO reqVO = new QmsIpqcSaveReqVO();
        reqVO.setMaterialCode(materialCode);
        reqVO.setOperationCode(operationCode);
        reqVO.setOperationName(operationName);
        QmsQualityStandardDO standard = selectIpqcStandard(reqVO);
        List<QmsQualityStandardItemDO> standardItems = selectStandardItems(standard.getId());
        QmsIpqcStandardRespVO respVO = BeanUtils.toBean(standard, QmsIpqcStandardRespVO.class);
        respVO.setStandardId(standard.getId());
        respVO.setItems(standardItems.stream().map(this::buildStandardItem).collect(Collectors.toList()));
        return respVO;
    }

    private QmsQualityStandardDO selectIpqcStandard(QmsIpqcSaveReqVO reqVO) {
        if (reqVO.getStandardId() != null) {
            QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(reqVO.getStandardId());
            validateStandardUsable(standard);
            return standard;
        }
        QmsQualityStandardDO standard = qmsQualityStandardMapper.selectList(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                        .eq(QmsQualityStandardDO::getApplyType, APPLY_TYPE_IPQC)
                        .eq(QmsQualityStandardDO::getStatus, ENABLED)
                        .eq(QmsQualityStandardDO::getAuditStatus, AUDITED)
                        .orderByDesc(QmsQualityStandardDO::getId))
                .stream()
                .filter(item -> matchesMaterialScope(item, reqVO))
                .filter(item -> matchesProcessScope(item, reqVO))
                .max(Comparator.comparingInt((QmsQualityStandardDO item) -> calculateStandardMatchScore(item, reqVO))
                        .thenComparing(QmsQualityStandardDO::getId))
                .orElse(null);
        if (standard == null) {
            throw exception(HCIPQC_STANDARD_NOT_EXISTS);
        }
        return standard;
    }

    private void validateStandardUsable(QmsQualityStandardDO standard) {
        if (standard == null || !APPLY_TYPE_IPQC.equals(standard.getApplyType())
                || !ENABLED.equals(standard.getStatus()) || !AUDITED.equals(standard.getAuditStatus())) {
            throw exception(HCIPQC_STANDARD_NOT_EXISTS);
        }
    }

    private List<QmsQualityStandardItemDO> selectStandardItems(Long standardId) {
        List<QmsQualityStandardItemDO> standardItems = qmsQualityStandardItemMapper.selectListByStandardId(standardId);
        if (standardItems == null || standardItems.isEmpty()) {
            throw exception(HCIPQC_STANDARD_ITEMS_EMPTY);
        }
        return standardItems;
    }

    private boolean matchesMaterialScope(QmsQualityStandardDO standard, QmsIpqcSaveReqVO reqVO) {
        boolean hasMaterial = standard.getMaterialId() != null || StringUtils.hasText(standard.getMaterialCode());
        if (!hasMaterial) {
            return true;
        }
        if (standard.getMaterialId() != null && reqVO.getMaterialId() != null) {
            return standard.getMaterialId().equals(reqVO.getMaterialId());
        }
        return StringUtils.hasText(standard.getMaterialCode())
                && StringUtils.hasText(reqVO.getMaterialCode())
                && standard.getMaterialCode().equals(reqVO.getMaterialCode());
    }

    private boolean matchesProcessScope(QmsQualityStandardDO standard, QmsIpqcSaveReqVO reqVO) {
        boolean hasProcess = standard.getProcessId() != null || StringUtils.hasText(standard.getProcessCode()) || StringUtils.hasText(standard.getProcessName());
        if (!hasProcess) {
            return true;
        }
        return (StringUtils.hasText(standard.getProcessCode()) && StringUtils.hasText(reqVO.getOperationCode()) && standard.getProcessCode().equals(reqVO.getOperationCode()))
                || (StringUtils.hasText(standard.getProcessName()) && StringUtils.hasText(reqVO.getOperationName()) && standard.getProcessName().equals(reqVO.getOperationName()));
    }

    private int calculateStandardMatchScore(QmsQualityStandardDO standard, QmsIpqcSaveReqVO reqVO) {
        int score = 0;
        if (standard.getMaterialId() != null && standard.getMaterialId().equals(reqVO.getMaterialId())) {
            score += 4;
        } else if (StringUtils.hasText(standard.getMaterialCode()) && standard.getMaterialCode().equals(reqVO.getMaterialCode())) {
            score += 3;
        }
        if (StringUtils.hasText(standard.getProcessCode()) && standard.getProcessCode().equals(reqVO.getOperationCode())) {
            score += 8;
        } else if (StringUtils.hasText(standard.getProcessName()) && standard.getProcessName().equals(reqVO.getOperationName())) {
            score += 6;
        }
        return score;
    }

    private void applyStandardSnapshot(QmsIpqcOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId());
        order.setStandardNo(standard.getStandardNo());
        order.setStandardVersion(standard.getVersion());
        if (order.getMaterialId() == null) {
            order.setMaterialId(standard.getMaterialId());
        }
        if (!StringUtils.hasText(order.getMaterialCode())) {
            order.setMaterialCode(standard.getMaterialCode());
        }
        if (!StringUtils.hasText(order.getMaterialName())) {
            order.setMaterialName(standard.getMaterialName());
        }
        if (!StringUtils.hasText(order.getSpecification())) {
            order.setSpecification(standard.getSpecification());
        }
    }

    private void saveStandardSnapshotDetails(QmsIpqcOrderDO order, List<QmsQualityStandardItemDO> standardItems,
                                             List<QmsIpqcSaveReqVO.IpqcAbnormal> abnormals) {
        for (int i = 0; i < standardItems.size(); i++) {
            QmsQualityStandardItemDO standardItem = standardItems.get(i);
            QmsIpqcItemDO itemDO = new QmsIpqcItemDO();
            itemDO.setIpqcId(order.getId());
            itemDO.setIpqcNo(order.getIpqcNo());
            itemDO.setStandardItemId(standardItem.getId());
            itemDO.setCategory(resolveCategory(standardItem, "PRODUCT"));
            itemDO.setInspectionItem(standardItem.getInspectionItem());
            itemDO.setItemType(standardItem.getItemType());
            itemDO.setTargetValue(standardItem.getTargetValue());
            itemDO.setStandardDesc(standardItem.getStandardDesc());
            itemDO.setInspectionMethod(standardItem.getInspectionMethod());
            itemDO.setTestFrequencyJudgement(standardItem.getTestFrequencyJudgement());
            itemDO.setTestTool(standardItem.getTestTool());
            itemDO.setSampleSize(standardItem.getSampleSize());
            itemDO.setMinValueLimit(standardItem.getMinValue());
            itemDO.setMaxValueLimit(standardItem.getMaxValue());
            itemDO.setItemResult(JUDGMENT_PENDING);
            itemDO.setIsSpc(Boolean.TRUE.equals(standardItem.getIsSpc()));
            itemDO.setSort(standardItem.getSort() == null ? (i + 1) * 10 : standardItem.getSort());
            qmsIpqcItemMapper.insert(itemDO);
        }
        saveAbnormals(order, abnormals);
    }

    private void updateExecutionDetails(QmsIpqcOrderDO order, List<QmsIpqcSaveReqVO.IpqcItem> items,
                                        List<QmsIpqcSaveReqVO.IpqcAbnormal> abnormals) {
        validateItems(items);
        List<QmsIpqcItemDO> existingItems = qmsIpqcItemMapper.selectListByIpqcId(order.getId());
        validateItemsExist(existingItems);
        for (QmsIpqcSaveReqVO.IpqcItem item : items) {
            if (!matchesExistingItem(item, existingItems)) {
                throw exception(HCIPQC_ITEM_SOURCE_INVALID);
            }
        }
        qmsIpqcSampleMapper.deleteByIpqcId(order.getId());
        for (QmsIpqcItemDO existingItem : existingItems) {
            QmsIpqcSaveReqVO.IpqcItem incomingItem = findIncomingItem(existingItem, items);
            if (incomingItem == null || incomingItem.getSamples() == null || incomingItem.getSamples().isEmpty()) {
                continue;
            }
            applyItemStats(existingItem, incomingItem.getSamples());
            qmsIpqcItemMapper.updateById(existingItem);
            saveSamples(order, existingItem, incomingItem.getSamples());
        }
        if (abnormals != null) {
            qmsIpqcAbnormalMapper.deleteByIpqcId(order.getId());
            saveAbnormals(order, abnormals);
        }
    }

    private QmsIpqcSaveReqVO.IpqcItem findIncomingItem(QmsIpqcItemDO existingItem, List<QmsIpqcSaveReqVO.IpqcItem> items) {
        return items.stream().filter(item ->
                (item.getId() != null && item.getId().equals(existingItem.getId()))
                        || (item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId())))
                .findFirst().orElse(null);
    }

    private boolean matchesExistingItem(QmsIpqcSaveReqVO.IpqcItem item, List<QmsIpqcItemDO> existingItems) {
        return existingItems.stream().anyMatch(existingItem ->
                (item.getId() != null && item.getId().equals(existingItem.getId()))
                        || (item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId())));
    }

    private void applyItemStats(QmsIpqcItemDO itemDO, List<QmsIpqcSaveReqVO.IpqcSample> samples) {
        String result = calculateItemResult(itemDO, samples);
        List<BigDecimal> measuredValues = samples.stream()
                .map(QmsIpqcSaveReqVO.IpqcSample::getMeasuredValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!measuredValues.isEmpty()) {
            BigDecimal max = Collections.max(measuredValues);
            BigDecimal min = Collections.min(measuredValues);
            BigDecimal total = measuredValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            itemDO.setMaxValue(max);
            itemDO.setMinValue(min);
            itemDO.setAverageValue(total.divide(BigDecimal.valueOf(measuredValues.size()), 6, RoundingMode.HALF_UP));
        }
        itemDO.setItemResult(result);
    }

    private String calculateItemResult(QmsIpqcItemDO itemDO, List<QmsIpqcSaveReqVO.IpqcSample> samples) {
        if (samples == null || samples.size() < itemDO.getSampleSize()) {
            return JUDGMENT_PENDING;
        }
        boolean hasPending = false;
        for (QmsIpqcSaveReqVO.IpqcSample sample : samples) {
            if (JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue())) {
                return JUDGMENT_NG;
            }
            BigDecimal value = sample.getMeasuredValue();
            if (value != null) {
                if (itemDO.getMinValueLimit() != null && value.compareTo(itemDO.getMinValueLimit()) < 0) {
                    return JUDGMENT_NG;
                }
                if (itemDO.getMaxValueLimit() != null && value.compareTo(itemDO.getMaxValueLimit()) > 0) {
                    return JUDGMENT_NG;
                }
                continue;
            }
            if (!JUDGMENT_OK.equals(sample.getSampleResult()) && !JUDGMENT_OK.equals(sample.getQualitativeValue())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private String calculateOrderJudgment(List<QmsIpqcItemDO> items) {
        validateItemsExist(items);
        boolean hasPending = false;
        for (QmsIpqcItemDO item : items) {
            if (JUDGMENT_NG.equals(item.getItemResult())) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(item.getItemResult())) {
                hasPending = true;
            }
        }
        if (hasPending) {
            throw exception(HCIPQC_ITEMS_NOT_COMPLETED);
        }
        return JUDGMENT_OK;
    }

    private void saveSamples(QmsIpqcOrderDO order, QmsIpqcItemDO itemDO, List<QmsIpqcSaveReqVO.IpqcSample> samples) {
        List<QmsIpqcSampleDO> sampleList = samples.stream().map(sample -> {
            QmsIpqcSampleDO sampleDO = BeanUtils.toBean(sample, QmsIpqcSampleDO.class);
            sampleDO.setId(null);
            sampleDO.setIpqcId(order.getId());
            sampleDO.setIpqcItemId(itemDO.getId());
            sampleDO.setIpqcNo(order.getIpqcNo());
            sampleDO.setSampleResult(defaultIfBlank(sample.getSampleResult(), JUDGMENT_PENDING));
            return sampleDO;
        }).collect(Collectors.toList());
        qmsIpqcSampleMapper.insertBatch(sampleList);
    }

    private void saveAbnormals(QmsIpqcOrderDO order, List<QmsIpqcSaveReqVO.IpqcAbnormal> abnormals) {
        if (abnormals == null || abnormals.isEmpty()) {
            return;
        }
        List<QmsIpqcAbnormalDO> abnormalList = abnormals.stream().map(abnormal -> {
            QmsIpqcAbnormalDO abnormalDO = BeanUtils.toBean(abnormal, QmsIpqcAbnormalDO.class);
            abnormalDO.setId(null);
            abnormalDO.setIpqcId(order.getId());
            abnormalDO.setIpqcNo(order.getIpqcNo());
            abnormalDO.setProcessStatus(defaultIfBlank(abnormal.getProcessStatus(), PROCESS_PENDING));
            return abnormalDO;
        }).collect(Collectors.toList());
        qmsIpqcAbnormalMapper.insertBatch(abnormalList);
    }

    private void ensureNgAbnormal(Long ipqcId, String ipqcNo, List<QmsIpqcItemDO> items, String controlAction) {
        if (!qmsIpqcAbnormalMapper.selectListByIpqcId(ipqcId).isEmpty()) {
            return;
        }
        List<QmsIpqcAbnormalDO> abnormalList = items.stream()
                .filter(item -> JUDGMENT_NG.equals(item.getItemResult()))
                .map(item -> QmsIpqcAbnormalDO.builder()
                        .ipqcId(ipqcId)
                        .ipqcNo(ipqcNo)
                        .ipqcItemId(item.getId())
                        .abnormalDesc("过程抽检项判定不合格：" + item.getInspectionItem())
                        .processStatus(PROCESS_PENDING)
                        .actionRequired(defaultIfBlank(controlAction, REPORT_ONLY))
                        .build())
                .collect(Collectors.toList());
        if (!abnormalList.isEmpty()) {
            qmsIpqcAbnormalMapper.insertBatch(abnormalList);
        }
    }

    private void fillDetails(QmsIpqcRespVO respVO) {
        List<QmsIpqcItemDO> items = qmsIpqcItemMapper.selectListByIpqcId(respVO.getId());
        List<QmsIpqcSampleDO> samples = qmsIpqcSampleMapper.selectListByIpqcId(respVO.getId());
        Map<Long, List<QmsIpqcRespVO.IpqcSample>> sampleMap = BeanUtils.toBean(samples, QmsIpqcRespVO.IpqcSample.class)
                .stream()
                .collect(Collectors.groupingBy(QmsIpqcRespVO.IpqcSample::getIpqcItemId));
        List<QmsIpqcRespVO.IpqcItem> itemRespList = BeanUtils.toBean(items, QmsIpqcRespVO.IpqcItem.class);
        itemRespList.forEach(item -> item.setSamples(sampleMap.getOrDefault(item.getId(), Collections.emptyList())));
        respVO.setItems(itemRespList);
        respVO.setAbnormals(BeanUtils.toBean(qmsIpqcAbnormalMapper.selectListByIpqcId(respVO.getId()), QmsIpqcRespVO.IpqcAbnormal.class));
    }

    private QmsIpqcStandardRespVO.StandardItem buildStandardItem(QmsQualityStandardItemDO item) {
        QmsIpqcStandardRespVO.StandardItem resp = BeanUtils.toBean(item, QmsIpqcStandardRespVO.StandardItem.class);
        resp.setStandardItemId(item.getId());
        resp.setCategory(resolveCategory(item, "PRODUCT"));
        resp.setMinValueLimit(item.getMinValue());
        resp.setMaxValueLimit(item.getMaxValue());
        return resp;
    }

    private String resolveCategory(QmsQualityStandardItemDO item, String defaultCategory) {
        String text = (item.getInspectionMethod() == null ? "" : item.getInspectionMethod())
                + (item.getTestFrequencyJudgement() == null ? "" : item.getTestFrequencyJudgement());
        if (text.toUpperCase().contains("PROCESS") || text.contains("工艺") || text.contains("参数")) {
            return "PROCESS";
        }
        return defaultCategory;
    }

    private QmsIpqcOrderDO validateIpqcExists(Long id) {
        QmsIpqcOrderDO entity = qmsIpqcOrderMapper.selectById(id);
        if (entity == null) {
            throw exception(HCIPQC_NOT_EXISTS);
        }
        return entity;
    }

    private void validateEditable(QmsIpqcOrderDO entity) {
        if (STATUS_COMPLETED.equals(entity.getStatus()) || STATUS_ABNORMAL.equals(entity.getStatus()) || STATUS_CANCELED.equals(entity.getStatus())) {
            throw exception(HCIPQC_FINISHED_LOCKED);
        }
    }

    private void validateIpqcNoUnique(Long id, String ipqcNo) {
        if (qmsIpqcOrderMapper.selectByIpqcNo(ipqcNo, id) != null) {
            throw exception(HCIPQC_NO_EXISTS);
        }
    }

    private void validateItems(List<QmsIpqcSaveReqVO.IpqcItem> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCIPQC_ITEMS_EMPTY);
        }
    }

    private void validateItemsExist(Collection<QmsIpqcItemDO> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCIPQC_ITEMS_EMPTY);
        }
    }

    private String generateIpqcNo() {
        String prefix = "IPQC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String candidate;
        do {
            candidate = prefix + String.valueOf(System.currentTimeMillis() % 1_000_000L);
        } while (qmsIpqcOrderMapper.selectByIpqcNo(candidate, null) != null);
        return candidate;
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) && !"-".equals(value) ? value : defaultValue;
    }
}
