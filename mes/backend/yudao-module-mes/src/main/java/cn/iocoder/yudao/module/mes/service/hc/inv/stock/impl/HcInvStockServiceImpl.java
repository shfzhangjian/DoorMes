package cn.iocoder.yudao.module.mes.service.hc.inv.stock.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.dto.HcWipOutputPostReq;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 实时库存余额 Service 实现类
 */
@Service
@Validated
public class HcInvStockServiceImpl implements HcInvStockService {

    private static final String STOCK_TYPE_WIP = "WIP";
    private static final String SOURCE_TYPE_GRINDING_SECOND = "GRINDING_SECOND";
    private static final String SOURCE_TABLE_GRINDING_SECOND = "mes_sfc_grinding_second_detail";
    private static final String TXN_TYPE_WIP_IN = "WIP_IN";
    private static final String TXN_TYPE_WIP_LOCK = "WIP_LOCK";
    private static final String TXN_TYPE_WIP_RELEASE = "WIP_RELEASE";
    private static final String TXN_TYPE_WIP_CONSUME = "WIP_CONSUME";
    private static final String LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String LOCK_STATUS_RELEASED = "RELEASED";
    private static final String DEFAULT_WIP_WAREHOUSE_CODE = "WH-WIP-LINE";
    private static final String DEFAULT_WIP_WAREHOUSE_NAME = "线边半成品库";
    private static final String DEFAULT_LOCATION_CODE = "WIP-UNKNOWN";
    private static final String DEFAULT_LOCATION_NAME = "未知工序位";
    private static final String DEFAULT_UOM_METER = "m";
    private static final String QUALITY_STATUS_OK = "合格";
    private static final String BIZ_STATUS_MASS = "量产";
    private static final String BIZ_STATUS_RND = "研发";
    private static final String PLAN_SPLIT_LOCK_MARK = "PLAN_SPLIT";
    private static final String DISCRETE_POST_PROCESS_LOCK_MARK = "DISCRETE_POST_PROCESS";
    private static final DateTimeFormatter TXN_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcInvTxnLogMapper hcInvTxnLogMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;

    @Override
    public PageResult<HcInvStockDO> getInvStockPage(HcInvStockPageReqVO pageReqVO) {
        return hcInvStockMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcInvStockDO> getInvStockList(HcInvStockPageReqVO reqVO) {
        return hcInvStockMapper.selectList(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcInvStockDO postGrindingSecondWip(HcGrindingSecondDetailDO detail,
                                              HcPlanOrderDO plan,
                                              HcPlanOrderOperationDO operation,
                                              LocalDateTime postTime,
                                              Long operatorId,
                                              String operatorName) {
        if (detail == null || detail.getId() == null) {
            throw invalidParamException("二磨明细不存在，无法入中间边库");
        }
        if (plan == null || plan.getId() == null) {
            throw invalidParamException("生产计划不存在，无法入中间边库");
        }
        if (operation == null || operation.getId() == null) {
            throw invalidParamException("计划工序不存在，无法入中间边库");
        }
        BigDecimal outputQty = normalizeQty(detail.getOutputLength());
        if (outputQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("二磨产出长度必须大于0，无法入中间边库");
        }
        String batchNo = firstNotBlank(detail.getConfirmedBatchNo(), detail.getProductionBatchNo(),
                detail.getMotherBatchNo(), plan.getProductionBatchNo(), plan.getBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            throw invalidParamException("二磨分段批号为空，无法入中间边库");
        }
        Long materialId = resolveWipMaterialId(null, plan);
        String materialCode = resolveWipMaterialCode(null, plan);
        String materialName = resolveWipMaterialName(null, plan);
        String modelNo = firstNotBlank(plan.getModelCode(), plan.getModelName(), operation.getMotherModelCode(),
                operation.getMotherModelName());
        if (StrUtil.isBlank(modelNo) && StrUtil.isBlank(materialCode) && StrUtil.isBlank(materialName)) {
            throw invalidParamException("生产计划型号信息不完整，无法入中间边库");
        }
        String locationCode = resolveWipLocationCode(operation);
        String locationName = resolveWipLocationName(operation);

        HcInvStockDO existed = hcInvStockMapper.selectOneBySource(SOURCE_TYPE_GRINDING_SECOND,
                SOURCE_TABLE_GRINDING_SECOND, detail.getId());
        if (existed != null) {
            fillMissingWipLocationIfNeeded(existed, operation);
            return existed;
        }
        if (materialId != null) {
            HcInvStockDO sameBatchStock = hcInvStockMapper.selectOneByStockKey(DEFAULT_WIP_WAREHOUSE_CODE,
                    materialId, batchNo, locationCode);
            if (sameBatchStock != null) {
                fillMissingWipLocationIfNeeded(sameBatchStock, operation);
                return sameBatchStock;
            }
        }

        LocalDateTime effectivePostTime = postTime == null ? LocalDateTime.now() : postTime;
        BigDecimal zero = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        BigDecimal initialShareableQty = resolveInitialShareableQty(plan, operation, outputQty);
        String txnNo = buildTxnNo(effectivePostTime, detail.getId());
        HcInvStockDO stock = HcInvStockDO.builder()
                .tenantId(plan.getTenantId())
                .stockType(STOCK_TYPE_WIP)
                .sourceType(SOURCE_TYPE_GRINDING_SECOND)
                .sourceTable(SOURCE_TABLE_GRINDING_SECOND)
                .sourceId(detail.getId())
                .sourceReportId(detail.getGrindingReportId())
                .sourcePlanId(plan.getId())
                .sourcePlanNo(firstNotBlank(plan.getPlanNo(), detail.getPlanNo()))
                .sourcePlanOperationId(operation.getId())
                .sourceBatchNo(batchNo)
                .sourceParentBatchNo(firstNotBlank(detail.getParentProductionBatchNo(), detail.getMotherBatchNo(),
                        detail.getSourceProductionBatchNo(), plan.getProductionBatchNo(), plan.getBatchNo()))
                .warehouseCode(DEFAULT_WIP_WAREHOUSE_CODE)
                .warehouseName(DEFAULT_WIP_WAREHOUSE_NAME)
                .locationCode(locationCode)
                .locationName(locationName)
                .materialId(materialId)
                .materialCode(materialCode)
                .materialName(materialName)
                .recipeCode(plan.getRecipeCode())
                .recipeName(plan.getRecipeName())
                .modelNo(modelNo)
                .specSize(plan.getSizeSpec())
                .opSeq(operation.getOpSeq())
                .opCode(operation.getOpCode())
                .opName(operation.getOpName())
                .segmentCode(firstNotBlank(detail.getSegmentMark(), "FULL"))
                .segmentName(resolveSegmentName(detail.getSegmentMark()))
                .thickness(parseThickness(firstNotBlank(detail.getAfterGrindingThickness(), detail.getQualityThickness(),
                        detail.getGrindingThickness())))
                .batchNo(batchNo)
                .productionDate(firstNonNull(detail.getReportDate(), plan.getProductionEndDate(),
                        plan.getProductionStartDate(), LocalDate.now()))
                .onHandQty(outputQty)
                .availableQty(outputQty)
                .shareableQty(initialShareableQty)
                .frozenQty(zero)
                .planLockedQty(zero)
                .qualityStatus(QUALITY_STATUS_OK)
                .bizStatus(resolveBizStatus(plan))
                .businessRemark(buildBusinessRemark(plan, operation, detail))
                .uom(DEFAULT_UOM_METER)
                .lastTxnNo(txnNo)
                .lastTxnTime(effectivePostTime)
                .build();
        stock.setCreateTime(effectivePostTime);
        stock.setUpdateTime(effectivePostTime);
        stock.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        stock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.insert(stock);

        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(plan.getTenantId())
                .stockId(stock.getId())
                .stockType(STOCK_TYPE_WIP)
                .txnNo(txnNo)
                .txnType(TXN_TYPE_WIP_IN)
                .txnTime(effectivePostTime)
                .warehouseCode(DEFAULT_WIP_WAREHOUSE_CODE)
                .warehouseName(DEFAULT_WIP_WAREHOUSE_NAME)
                .locationCode(locationCode)
                .materialId(materialId)
                .materialCode(materialCode)
                .materialName(materialName)
                .modelNo(stock.getModelNo())
                .batchNo(batchNo)
                .txnQty(outputQty)
                .beforeQty(zero)
                .afterQty(outputQty)
                .beforeAvailableQty(zero)
                .afterAvailableQty(outputQty)
                .beforeFrozenQty(zero)
                .afterFrozenQty(zero)
                .beforePlanLockedQty(zero)
                .afterPlanLockedQty(zero)
                .uom(DEFAULT_UOM_METER)
                .refDocType(SOURCE_TYPE_GRINDING_SECOND)
                .refDocId(detail.getId())
                .refDocNo(batchNo)
                .sourceType(SOURCE_TYPE_GRINDING_SECOND)
                .sourceTable(SOURCE_TABLE_GRINDING_SECOND)
                .sourceId(detail.getId())
                .sourceBatchNo(batchNo)
                .sourcePlanId(plan.getId())
                .sourcePlanNo(stock.getSourcePlanNo())
                .sourcePlanOperationId(operation.getId())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark("磨皮二磨确认自动入中间边库")
                .build();
        txnLog.setCreateTime(effectivePostTime);
        txnLog.setUpdateTime(effectivePostTime);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);
        return stock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcInvStockDO postProcessOutputWip(HcWipOutputPostReq req) {
        if (req == null) {
            throw invalidParamException("工序产出入账请求不能为空");
        }
        HcPlanOrderDO plan = req.getPlan();
        HcPlanOrderOperationDO operation = req.getOperation();
        if (plan == null || plan.getId() == null) {
            throw invalidParamException("生产计划不存在，无法入中间边库");
        }
        if (operation == null || operation.getId() == null) {
            throw invalidParamException("计划工序不存在，无法入中间边库");
        }
        String sourceType = StrUtil.trim(req.getSourceType());
        String sourceTable = StrUtil.trim(req.getSourceTable());
        if (StrUtil.isBlank(sourceType) || StrUtil.isBlank(sourceTable) || req.getSourceId() == null) {
            throw invalidParamException("工序产出缺少来源类型、来源表或来源ID，无法入中间边库");
        }
        BigDecimal outputQty = normalizeQty(req.getOutputQty());
        if (outputQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("工序产出数量必须大于0，无法入中间边库");
        }
        String batchNo = firstNotBlank(req.getBatchNo(), plan.getProductionBatchNo(), plan.getBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            throw invalidParamException("工序产出批号为空，无法入中间边库");
        }
        String modelNo = firstNotBlank(req.getModelNo(), plan.getModelCode(), plan.getModelName(),
                operation.getMotherModelCode(), operation.getMotherModelName());
        Long materialId = resolveWipMaterialId(req, plan);
        String materialCode = resolveWipMaterialCode(req, plan);
        String materialName = resolveWipMaterialName(req, plan);
        if (StrUtil.isBlank(modelNo) && StrUtil.isBlank(materialCode) && StrUtil.isBlank(materialName)) {
            throw invalidParamException("生产计划型号信息不完整，无法入中间边库");
        }

        HcInvStockDO existed = hcInvStockMapper.selectOneBySource(sourceType, sourceTable, req.getSourceId());
        if (existed != null) {
            fillMissingWipLocationIfNeeded(existed, operation);
            return existed;
        }

        LocalDateTime effectivePostTime = req.getPostTime() == null ? LocalDateTime.now() : req.getPostTime();
        BigDecimal zero = normalizeQty(BigDecimal.ZERO);
        BigDecimal initialShareableQty = resolveInitialShareableQty(plan, operation, outputQty);
        String txnNo = buildTxnNo("WIPIN", effectivePostTime, req.getSourceId());
        String segmentCode = firstNotBlank(req.getSegmentCode(), "FULL");
        String segmentName = firstNotBlank(req.getSegmentName(), resolveSegmentName(segmentCode));
        String parentBatchNo = firstNotBlank(req.getParentBatchNo(), plan.getParentProductionBatchNo(),
                plan.getProductionBatchNo(), plan.getBatchNo());
        String locationCode = resolveWipLocationCode(operation);
        String locationName = resolveWipLocationName(operation);

        if (materialId != null) {
            HcInvStockDO sameBusinessStock = hcInvStockMapper.selectOneByStockKeyForUpdate(
                    DEFAULT_WIP_WAREHOUSE_CODE, materialId, batchNo, locationCode);
            if (sameBusinessStock != null) {
                HcInvStockDO sameSourceStock = hcInvStockMapper.selectOneBySourceForUpdate(
                        sourceType, sourceTable, req.getSourceId());
                if (sameSourceStock != null) {
                    return sameSourceStock;
                }
                return appendProcessOutputWip(sameBusinessStock, req, plan, operation, sourceType, sourceTable,
                        batchNo, parentBatchNo, materialId, materialCode, materialName, outputQty,
                        initialShareableQty, effectivePostTime);
            }
        }

        HcInvStockDO stock = HcInvStockDO.builder()
                .tenantId(plan.getTenantId())
                .stockType(STOCK_TYPE_WIP)
                .sourceType(sourceType)
                .sourceTable(sourceTable)
                .sourceId(req.getSourceId())
                .sourceReportId(req.getSourceReportId())
                .sourcePlanId(plan.getId())
                .sourcePlanNo(plan.getPlanNo())
                .sourcePlanOperationId(operation.getId())
                .sourceBatchNo(batchNo)
                .sourceParentBatchNo(parentBatchNo)
                .warehouseCode(DEFAULT_WIP_WAREHOUSE_CODE)
                .warehouseName(DEFAULT_WIP_WAREHOUSE_NAME)
                .locationCode(locationCode)
                .locationName(locationName)
                .materialId(materialId)
                .materialCode(materialCode)
                .materialName(materialName)
                .recipeCode(plan.getRecipeCode())
                .recipeName(plan.getRecipeName())
                .modelNo(modelNo)
                .specSize(firstNotBlank(req.getSpecSize(), plan.getSizeSpec()))
                .opSeq(operation.getOpSeq())
                .opCode(operation.getOpCode())
                .opName(operation.getOpName())
                .segmentCode(segmentCode)
                .segmentName(segmentName)
                .thickness(req.getThickness())
                .batchNo(batchNo)
                .productionDate(firstNonNull(req.getProductionDate(), plan.getProductionEndDate(),
                        plan.getProductionStartDate(), LocalDate.now()))
                .onHandQty(outputQty)
                .availableQty(outputQty)
                .shareableQty(initialShareableQty)
                .frozenQty(zero)
                .planLockedQty(zero)
                .qualityStatus(firstNotBlank(req.getQualityStatus(), QUALITY_STATUS_OK))
                .bizStatus(firstNotBlank(req.getBizStatus(), resolveBizStatus(plan)))
                .businessRemark(req.getBusinessRemark())
                .uom(firstNotBlank(req.getUom(), operation.getUom(), operation.getUnitCode(), plan.getTargetUom()))
                .lastTxnNo(txnNo)
                .lastTxnTime(effectivePostTime)
                .build();
        stock.setCreateTime(effectivePostTime);
        stock.setUpdateTime(effectivePostTime);
        stock.setCreator(req.getOperatorId() == null ? null : String.valueOf(req.getOperatorId()));
        stock.setUpdater(req.getOperatorId() == null ? null : String.valueOf(req.getOperatorId()));
        try {
            hcInvStockMapper.insert(stock);
        } catch (DuplicateKeyException ex) {
            if (materialId == null) {
                throw ex;
            }
            HcInvStockDO sameSourceStock = hcInvStockMapper.selectOneBySourceForUpdate(
                    sourceType, sourceTable, req.getSourceId());
            if (sameSourceStock != null) {
                return sameSourceStock;
            }
            HcInvStockDO sameBusinessStock = hcInvStockMapper.selectOneByStockKeyForUpdate(
                    DEFAULT_WIP_WAREHOUSE_CODE, materialId, batchNo, locationCode);
            if (sameBusinessStock == null) {
                throw ex;
            }
            return appendProcessOutputWip(sameBusinessStock, req, plan, operation, sourceType, sourceTable,
                    batchNo, parentBatchNo, materialId, materialCode, materialName, outputQty,
                    initialShareableQty, effectivePostTime);
        }

        insertProcessOutputWipTxnLog(stock, req, plan, operation, sourceType, sourceTable, batchNo,
                materialId, materialCode, materialName, outputQty, zero, outputQty, zero, outputQty,
                zero, zero, zero, zero, effectivePostTime, txnNo);
        return stock;
    }

    private HcInvStockDO appendProcessOutputWip(HcInvStockDO stock,
                                                 HcWipOutputPostReq req,
                                                 HcPlanOrderDO plan,
                                                 HcPlanOrderOperationDO operation,
                                                 String sourceType,
                                                 String sourceTable,
                                                 String batchNo,
                                                 String parentBatchNo,
                                                 Long materialId,
                                                 String materialCode,
                                                 String materialName,
                                                 BigDecimal outputQty,
                                                 BigDecimal shareableQtyIncrease,
                                                 LocalDateTime effectivePostTime) {
        if (StrUtil.isNotBlank(stock.getStockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(stock.getStockType())) {
            throw invalidParamException("目标库存不是WIP中间品，无法合并工序产出");
        }
        String requestUom = firstNotBlank(req.getUom(), operation.getUom(), operation.getUnitCode(), plan.getTargetUom());
        if (StrUtil.isNotBlank(stock.getUom()) && StrUtil.isNotBlank(requestUom)
                && !stock.getUom().equalsIgnoreCase(requestUom)) {
            throw invalidParamException("目标库存单位与本次工序产出单位不一致，无法合并入库");
        }

        BigDecimal beforeQty = normalizeQty(stock.getOnHandQty());
        BigDecimal beforeAvailableQty = normalizeQty(stock.getAvailableQty());
        BigDecimal beforeShareableQty = normalizeQty(stock.getShareableQty());
        BigDecimal beforeFrozenQty = normalizeQty(stock.getFrozenQty());
        BigDecimal beforePlanLockedQty = normalizeQty(stock.getPlanLockedQty());
        BigDecimal afterQty = beforeQty.add(outputQty);
        BigDecimal afterAvailableQty = beforeAvailableQty.add(outputQty);
        BigDecimal afterShareableQty = minQty(beforeShareableQty.add(normalizeQty(shareableQtyIncrease)), afterAvailableQty);
        String txnNo = buildTxnNo("WIPIN", effectivePostTime, req.getSourceId());

        HcInvStockDO updateStock = new HcInvStockDO();
        updateStock.setId(stock.getId());
        updateStock.setOnHandQty(afterQty);
        updateStock.setAvailableQty(afterAvailableQty);
        updateStock.setShareableQty(afterShareableQty);
        updateStock.setLastTxnNo(txnNo);
        updateStock.setLastTxnTime(effectivePostTime);
        updateStock.setUpdateTime(effectivePostTime);
        updateStock.setUpdater(req.getOperatorId() == null ? null : String.valueOf(req.getOperatorId()));
        hcInvStockMapper.updateById(updateStock);

        stock.setOnHandQty(afterQty);
        stock.setAvailableQty(afterAvailableQty);
        stock.setShareableQty(afterShareableQty);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(effectivePostTime);
        stock.setUpdateTime(effectivePostTime);
        stock.setUpdater(updateStock.getUpdater());
        insertProcessOutputWipTxnLog(stock, req, plan, operation, sourceType, sourceTable, batchNo,
                materialId, materialCode, materialName, outputQty, beforeQty, afterQty,
                beforeAvailableQty, afterAvailableQty, beforeFrozenQty, beforeFrozenQty,
                beforePlanLockedQty, beforePlanLockedQty, effectivePostTime, txnNo);
        return stock;
    }

    private void insertProcessOutputWipTxnLog(HcInvStockDO stock,
                                               HcWipOutputPostReq req,
                                               HcPlanOrderDO plan,
                                               HcPlanOrderOperationDO operation,
                                               String sourceType,
                                               String sourceTable,
                                               String batchNo,
                                               Long materialId,
                                               String materialCode,
                                               String materialName,
                                               BigDecimal txnQty,
                                               BigDecimal beforeQty,
                                               BigDecimal afterQty,
                                               BigDecimal beforeAvailableQty,
                                               BigDecimal afterAvailableQty,
                                               BigDecimal beforeFrozenQty,
                                               BigDecimal afterFrozenQty,
                                               BigDecimal beforePlanLockedQty,
                                               BigDecimal afterPlanLockedQty,
                                               LocalDateTime effectivePostTime,
                                               String txnNo) {
        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(plan.getTenantId())
                .stockId(stock.getId())
                .stockType(STOCK_TYPE_WIP)
                .txnNo(txnNo)
                .txnType(TXN_TYPE_WIP_IN)
                .txnTime(effectivePostTime)
                .warehouseCode(DEFAULT_WIP_WAREHOUSE_CODE)
                .warehouseName(DEFAULT_WIP_WAREHOUSE_NAME)
                .locationCode(stock.getLocationCode())
                .materialId(materialId)
                .materialCode(materialCode)
                .materialName(materialName)
                .modelNo(stock.getModelNo())
                .batchNo(batchNo)
                .txnQty(txnQty)
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .beforeAvailableQty(beforeAvailableQty)
                .afterAvailableQty(afterAvailableQty)
                .beforeFrozenQty(beforeFrozenQty)
                .afterFrozenQty(afterFrozenQty)
                .beforePlanLockedQty(beforePlanLockedQty)
                .afterPlanLockedQty(afterPlanLockedQty)
                .uom(stock.getUom())
                .refDocType(sourceType)
                .refDocId(req.getSourceId())
                .refDocNo(batchNo)
                .sourceType(sourceType)
                .sourceTable(sourceTable)
                .sourceId(req.getSourceId())
                .sourceBatchNo(batchNo)
                .sourcePlanId(plan.getId())
                .sourcePlanNo(plan.getPlanNo())
                .sourcePlanOperationId(operation.getId())
                .creatorName(firstNotBlank(req.getOperatorName(), "系统"))
                .remark(firstNotBlank(req.getTxnRemark(), "工序确认产出自动入中间边库"))
                .build();
        txnLog.setCreateTime(effectivePostTime);
        txnLog.setUpdateTime(effectivePostTime);
        txnLog.setCreator(req.getOperatorId() == null ? null : String.valueOf(req.getOperatorId()));
        txnLog.setUpdater(req.getOperatorId() == null ? null : String.valueOf(req.getOperatorId()));
        hcInvTxnLogMapper.insert(txnLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcInvStockDO lockPlanWip(HcPlanOrderInventoryLockDO lock,
                                    BigDecimal lockQty,
                                    LocalDateTime lockTime,
                                    Long operatorId,
                                    String operatorName,
                                    String remark) {
        if (lock == null) {
            throw invalidParamException("计划挂接中间品不存在，无法锁定中间边库");
        }
        if (StrUtil.isNotBlank(lock.getLockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(lock.getLockType())) {
            throw invalidParamException("当前计划挂接记录不是WIP中间品，无法锁定中间边库");
        }
        if (lock.getStockId() == null) {
            throw invalidParamException("计划挂接中间品缺少库存ID，无法锁定中间边库");
        }
        BigDecimal normalizedLockQty = normalizeQty(lockQty);
        if (normalizedLockQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("中间边库锁定数量必须大于0");
        }

        HcInvStockDO stock = hcInvStockMapper.selectByIdForUpdate(lock.getStockId());
        if (stock == null) {
            throw invalidParamException("中间边库库存不存在或已删除，无法锁定");
        }
        if (StrUtil.isNotBlank(stock.getStockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(stock.getStockType())) {
            throw invalidParamException("当前库存不是WIP中间品，无法按计划挂接锁定");
        }
        BigDecimal beforeQty = normalizeQty(stock.getOnHandQty());
        BigDecimal beforeAvailableQty = normalizeQty(stock.getAvailableQty());
        BigDecimal beforeShareableQty = normalizeQty(stock.getShareableQty());
        BigDecimal beforeFrozenQty = normalizeQty(stock.getFrozenQty());
        BigDecimal beforePlanLockedQty = normalizeQty(stock.getPlanLockedQty());
        if (beforeAvailableQty.compareTo(normalizedLockQty) < 0) {
            throw invalidParamException("中间边库可用余量不足，可用 " + beforeAvailableQty + "，本次锁定 " + normalizedLockQty);
        }
        boolean crossPlanLock = isCrossPlanWipLock(stock, lock);
        boolean planSplitLock = isPlanSplitWipLock(lock);
        boolean discretePostProcessLock = isDiscretePostProcessWipLock(lock);
        boolean bypassShareableCheck = planSplitLock || discretePostProcessLock;
        if (crossPlanLock && !bypassShareableCheck && beforeShareableQty.compareTo(normalizedLockQty) < 0) {
            throw invalidParamException("中间边库可利库量不足，可利库 " + beforeShareableQty + "，本次跨计划锁定 " + normalizedLockQty);
        }

        BigDecimal afterAvailableQty = beforeAvailableQty.subtract(normalizedLockQty);
        BigDecimal afterShareableQty = crossPlanLock
                ? (bypassShareableCheck
                    ? beforeShareableQty.subtract(normalizedLockQty).max(normalizeQty(BigDecimal.ZERO))
                    : beforeShareableQty.subtract(normalizedLockQty))
                : minQty(beforeShareableQty, afterAvailableQty);
        afterShareableQty = minQty(afterShareableQty, afterAvailableQty);
        BigDecimal afterFrozenQty = beforeFrozenQty.add(normalizedLockQty);
        BigDecimal afterPlanLockedQty = beforePlanLockedQty.add(normalizedLockQty);
        LocalDateTime effectiveLockTime = lockTime == null ? LocalDateTime.now() : lockTime;
        String txnNo = buildTxnNo("WIPLOCK", effectiveLockTime, lock.getId() == null ? stock.getId() : lock.getId());

        HcInvStockDO updateStock = new HcInvStockDO();
        updateStock.setId(stock.getId());
        updateStock.setAvailableQty(afterAvailableQty);
        updateStock.setShareableQty(afterShareableQty);
        updateStock.setFrozenQty(afterFrozenQty);
        updateStock.setPlanLockedQty(afterPlanLockedQty);
        updateStock.setLastTxnNo(txnNo);
        updateStock.setLastTxnTime(effectiveLockTime);
        updateStock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.updateById(updateStock);

        if (lock.getId() != null) {
            HcPlanOrderInventoryLockDO updateLock = new HcPlanOrderInventoryLockDO();
            updateLock.setId(lock.getId());
            updateLock.setLockTxnNo(txnNo);
            updateLock.setLockStatus(LOCK_STATUS_ACTIVE);
            updateLock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
            hcPlanOrderInventoryLockMapper.updateById(updateLock);
        }

        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(firstNonNull(stock.getTenantId(), lock.getTenantId()))
                .stockId(stock.getId())
                .stockType(firstNotBlank(stock.getStockType(), lock.getStockType(), STOCK_TYPE_WIP))
                .txnNo(txnNo)
                .txnType(TXN_TYPE_WIP_LOCK)
                .txnTime(effectiveLockTime)
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .locationCode(stock.getLocationCode())
                .materialId(stock.getMaterialId())
                .materialCode(stock.getMaterialCode())
                .materialName(stock.getMaterialName())
                .modelNo(stock.getModelNo())
                .batchNo(stock.getBatchNo())
                .txnQty(normalizeQty(BigDecimal.ZERO))
                .beforeQty(beforeQty)
                .afterQty(beforeQty)
                .beforeAvailableQty(beforeAvailableQty)
                .afterAvailableQty(afterAvailableQty)
                .beforeFrozenQty(beforeFrozenQty)
                .afterFrozenQty(afterFrozenQty)
                .beforePlanLockedQty(beforePlanLockedQty)
                .afterPlanLockedQty(afterPlanLockedQty)
                .uom(firstNotBlank(stock.getUom(), lock.getUom()))
                .refDocType("PLAN_WIP_LOCK")
                .refDocId(lock.getId())
                .refDocNo(lock.getTargetPlanNo())
                .sourceType(firstNotBlank(lock.getSourceType(), stock.getSourceType()))
                .sourceTable(firstNotBlank(lock.getSourceTable(), stock.getSourceTable()))
                .sourceId(firstNonNull(lock.getSourceId(), stock.getSourceId()))
                .sourceBatchNo(firstNotBlank(lock.getSourceBatchNo(), stock.getSourceBatchNo(), stock.getBatchNo()))
                .sourcePlanId(firstNonNull(lock.getSourcePlanId(), stock.getSourcePlanId()))
                .sourcePlanNo(firstNotBlank(lock.getSourcePlanNo(), stock.getSourcePlanNo()))
                .sourcePlanOperationId(firstNonNull(lock.getSourcePlanOperationId(), stock.getSourcePlanOperationId()))
                .targetPlanId(lock.getPlanId())
                .targetPlanNo(lock.getTargetPlanNo())
                .targetPlanOperationId(lock.getPlanOperationId())
                .targetOpCode(lock.getTargetOpCode())
                .targetOpName(lock.getTargetOpName())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark(firstNotBlank(remark, "生产计划挂接中间边库，转为计划锁定量：" + normalizedLockQty))
                .build();
        txnLog.setCreateTime(effectiveLockTime);
        txnLog.setUpdateTime(effectiveLockTime);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);

        stock.setAvailableQty(afterAvailableQty);
        stock.setShareableQty(afterShareableQty);
        stock.setFrozenQty(afterFrozenQty);
        stock.setPlanLockedQty(afterPlanLockedQty);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(effectiveLockTime);
        return stock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcInvStockDO releasePlanLockedWip(HcPlanOrderInventoryLockDO lock,
                                             BigDecimal releaseQty,
                                             String releaseReason,
                                             LocalDateTime releaseTime,
                                             Long operatorId,
                                             String operatorName) {
        if (lock == null || lock.getId() == null) {
            throw invalidParamException("计划挂接中间品不存在，无法释放中间边库");
        }

        HcPlanOrderInventoryLockDO locked = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(lock.getId());
        if (locked == null) {
            throw invalidParamException("计划挂接中间品不存在或已删除，无法释放中间边库");
        }
        if (StrUtil.isNotBlank(locked.getLockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(locked.getLockType())) {
            throw invalidParamException("当前计划挂接记录不是WIP中间品，无法释放中间边库");
        }
        if (LOCK_STATUS_CONSUMED.equalsIgnoreCase(locked.getLockStatus())) {
            throw invalidParamException("计划挂接中间品已全部消耗，不能释放");
        }
        if (LOCK_STATUS_CANCELLED.equalsIgnoreCase(locked.getLockStatus())) {
            throw invalidParamException("计划挂接中间品已取消，不能重复释放");
        }
        if (LOCK_STATUS_RELEASED.equalsIgnoreCase(locked.getLockStatus())) {
            throw invalidParamException("计划挂接中间品已释放，不能重复释放");
        }
        if (locked.getStockId() == null) {
            throw invalidParamException("计划挂接中间品缺少库存ID，无法释放中间边库");
        }

        BigDecimal lockRemainingQty = calculateRemainingLockQty(locked);
        BigDecimal normalizedReleaseQty = releaseQty == null ? lockRemainingQty : normalizeQty(releaseQty);
        if (normalizedReleaseQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("中间边库释放数量必须大于0");
        }
        if (lockRemainingQty.compareTo(normalizedReleaseQty) < 0) {
            throw invalidParamException("计划挂接中间品余量不足，剩余 " + lockRemainingQty + "，本次释放 " + normalizedReleaseQty);
        }

        HcInvStockDO stock = hcInvStockMapper.selectByIdForUpdate(locked.getStockId());
        if (stock == null) {
            throw invalidParamException("中间边库库存不存在或已删除，无法释放");
        }
        BigDecimal beforeQty = normalizeQty(stock.getOnHandQty());
        BigDecimal beforeAvailableQty = normalizeQty(stock.getAvailableQty());
        BigDecimal beforeShareableQty = normalizeQty(stock.getShareableQty());
        BigDecimal beforeFrozenQty = normalizeQty(stock.getFrozenQty());
        BigDecimal beforePlanLockedQty = normalizeQty(stock.getPlanLockedQty());
        BigDecimal frozenReleaseQty = minQty(beforeFrozenQty, normalizedReleaseQty);
        BigDecimal planLockedReleaseQty = minQty(beforePlanLockedQty, normalizedReleaseQty);
        BigDecimal zero = normalizeQty(BigDecimal.ZERO);
        BigDecimal afterFrozenQty = beforeFrozenQty.subtract(frozenReleaseQty).max(zero);
        BigDecimal afterPlanLockedQty = beforePlanLockedQty.subtract(planLockedReleaseQty).max(zero);
        BigDecimal afterAvailableMaxQty = beforeQty.subtract(afterFrozenQty).max(zero);
        BigDecimal afterAvailableQty = beforeAvailableQty.add(normalizedReleaseQty);
        if (afterAvailableQty.compareTo(afterAvailableMaxQty) > 0) {
            afterAvailableQty = afterAvailableMaxQty;
        }
        BigDecimal afterShareableQty = beforeShareableQty.add(normalizedReleaseQty);
        if (afterShareableQty.compareTo(afterAvailableQty) > 0) {
            afterShareableQty = afterAvailableQty;
        }
        LocalDateTime effectiveReleaseTime = releaseTime == null ? LocalDateTime.now() : releaseTime;
        String txnNo = buildTxnNo("WIPREL", effectiveReleaseTime, locked.getId());

        HcInvStockDO updateStock = new HcInvStockDO();
        updateStock.setId(stock.getId());
        updateStock.setAvailableQty(afterAvailableQty);
        updateStock.setShareableQty(afterShareableQty);
        updateStock.setFrozenQty(afterFrozenQty);
        updateStock.setPlanLockedQty(afterPlanLockedQty);
        updateStock.setLastTxnNo(txnNo);
        updateStock.setLastTxnTime(effectiveReleaseTime);
        updateStock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.updateById(updateStock);

        BigDecimal releasedQty = normalizeQty(locked.getReleasedQty()).add(normalizedReleaseQty);
        BigDecimal consumedQty = normalizeQty(locked.getConsumedQty());
        BigDecimal remainingQty = normalizeQty(locked.getLockQty()).subtract(consumedQty).subtract(releasedQty).max(zero);
        HcPlanOrderInventoryLockDO updateLock = new HcPlanOrderInventoryLockDO();
        updateLock.setId(locked.getId());
        updateLock.setConsumedQty(consumedQty);
        updateLock.setReleasedQty(releasedQty);
        updateLock.setRemainingQty(remainingQty);
        updateLock.setReleaseTime(effectiveReleaseTime);
        updateLock.setReleaseReason(firstNotBlank(releaseReason, "人工释放计划挂接中间边库"));
        updateLock.setReleaseTxnNo(txnNo);
        updateLock.setLockStatus(remainingQty.compareTo(BigDecimal.ZERO) <= 0 ? LOCK_STATUS_RELEASED : LOCK_STATUS_ACTIVE);
        updateLock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcPlanOrderInventoryLockMapper.updateById(updateLock);

        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(firstNonNull(stock.getTenantId(), locked.getTenantId()))
                .stockId(stock.getId())
                .stockType(firstNotBlank(stock.getStockType(), locked.getStockType(), STOCK_TYPE_WIP))
                .txnNo(txnNo)
                .txnType(TXN_TYPE_WIP_RELEASE)
                .txnTime(effectiveReleaseTime)
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .locationCode(stock.getLocationCode())
                .materialId(stock.getMaterialId())
                .materialCode(stock.getMaterialCode())
                .materialName(stock.getMaterialName())
                .modelNo(stock.getModelNo())
                .batchNo(stock.getBatchNo())
                .txnQty(zero)
                .beforeQty(beforeQty)
                .afterQty(beforeQty)
                .beforeAvailableQty(beforeAvailableQty)
                .afterAvailableQty(afterAvailableQty)
                .beforeFrozenQty(beforeFrozenQty)
                .afterFrozenQty(afterFrozenQty)
                .beforePlanLockedQty(beforePlanLockedQty)
                .afterPlanLockedQty(afterPlanLockedQty)
                .uom(firstNotBlank(stock.getUom(), locked.getUom()))
                .refDocType("PLAN_WIP_RELEASE")
                .refDocId(locked.getId())
                .refDocNo(locked.getTargetPlanNo())
                .sourceType(firstNotBlank(locked.getSourceType(), stock.getSourceType()))
                .sourceTable(firstNotBlank(locked.getSourceTable(), stock.getSourceTable()))
                .sourceId(firstNonNull(locked.getSourceId(), stock.getSourceId()))
                .sourceBatchNo(firstNotBlank(locked.getSourceBatchNo(), stock.getSourceBatchNo(), stock.getBatchNo()))
                .sourcePlanId(firstNonNull(locked.getSourcePlanId(), stock.getSourcePlanId()))
                .sourcePlanNo(firstNotBlank(locked.getSourcePlanNo(), stock.getSourcePlanNo()))
                .sourcePlanOperationId(firstNonNull(locked.getSourcePlanOperationId(), stock.getSourcePlanOperationId()))
                .targetPlanId(locked.getPlanId())
                .targetPlanNo(locked.getTargetPlanNo())
                .targetPlanOperationId(locked.getPlanOperationId())
                .targetOpCode(locked.getTargetOpCode())
                .targetOpName(locked.getTargetOpName())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark(firstNotBlank(releaseReason, "释放计划挂接中间边库，退回可用量：" + normalizedReleaseQty))
                .build();
        txnLog.setCreateTime(effectiveReleaseTime);
        txnLog.setUpdateTime(effectiveReleaseTime);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);

        stock.setAvailableQty(afterAvailableQty);
        stock.setShareableQty(afterShareableQty);
        stock.setFrozenQty(afterFrozenQty);
        stock.setPlanLockedQty(afterPlanLockedQty);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(effectiveReleaseTime);
        return stock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcInvStockDO consumePlanLockedWip(HcPlanOrderInventoryLockDO lock,
                                             BigDecimal consumeQty,
                                             String refDocType,
                                             Long refDocId,
                                             String refDocNo,
                                             LocalDateTime consumeTime,
                                             Long operatorId,
                                             String operatorName,
                                             String remark) {
        if (lock == null || lock.getId() == null) {
            throw invalidParamException("计划挂接中间品不存在，无法消耗中间边库");
        }
        BigDecimal normalizedConsumeQty = normalizeQty(consumeQty);
        if (normalizedConsumeQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("中间边库消耗数量必须大于0");
        }

        HcPlanOrderInventoryLockDO locked = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(lock.getId());
        if (locked == null) {
            throw invalidParamException("计划挂接中间品不存在或已删除，无法消耗中间边库");
        }
        if (StrUtil.isNotBlank(locked.getLockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(locked.getLockType())) {
            throw invalidParamException("当前计划挂接记录不是WIP中间品，无法按中间边库消耗");
        }
        if (LOCK_STATUS_CANCELLED.equalsIgnoreCase(locked.getLockStatus())) {
            throw invalidParamException("计划挂接中间品已取消，无法消耗中间边库");
        }
        if (LOCK_STATUS_CONSUMED.equalsIgnoreCase(locked.getLockStatus())) {
            throw invalidParamException("计划挂接中间品已全部消耗，不能重复消耗");
        }
        BigDecimal lockRemainingQty = calculateRemainingLockQty(locked);
        if (lockRemainingQty.compareTo(normalizedConsumeQty) < 0) {
            throw invalidParamException("计划挂接中间品余量不足，剩余 " + lockRemainingQty + "，本次需 " + normalizedConsumeQty);
        }
        if (locked.getStockId() == null) {
            throw invalidParamException("计划挂接中间品缺少库存ID，无法消耗中间边库");
        }

        HcInvStockDO stock = hcInvStockMapper.selectByIdForUpdate(locked.getStockId());
        if (stock == null) {
            throw invalidParamException("中间边库库存不存在或已删除，无法消耗");
        }
        BigDecimal beforeQty = normalizeQty(stock.getOnHandQty());
        BigDecimal beforeAvailableQty = normalizeQty(stock.getAvailableQty());
        BigDecimal beforeShareableQty = normalizeQty(stock.getShareableQty());
        BigDecimal beforeFrozenQty = normalizeQty(stock.getFrozenQty());
        BigDecimal beforePlanLockedQty = normalizeQty(stock.getPlanLockedQty());
        if (beforeQty.compareTo(normalizedConsumeQty) < 0) {
            throw invalidParamException("中间边库在库余量不足，剩余 " + beforeQty + "，本次需 " + normalizedConsumeQty);
        }

        BigDecimal planLockedConsumeQty = minQty(beforePlanLockedQty, normalizedConsumeQty);
        BigDecimal frozenConsumeQty = minQty(beforeFrozenQty, planLockedConsumeQty);
        BigDecimal availableConsumeQty = normalizedConsumeQty.subtract(frozenConsumeQty);
        if (beforeAvailableQty.compareTo(availableConsumeQty) < 0) {
            throw invalidParamException("中间边库可用余量不足，可用 " + beforeAvailableQty + "，本次需 " + normalizedConsumeQty);
        }

        BigDecimal zero = normalizeQty(BigDecimal.ZERO);
        BigDecimal afterQty = beforeQty.subtract(normalizedConsumeQty).max(zero);
        BigDecimal afterAvailableQty = beforeAvailableQty.subtract(availableConsumeQty).max(zero);
        BigDecimal afterShareableQty = minQty(beforeShareableQty, afterAvailableQty);
        BigDecimal afterFrozenQty = beforeFrozenQty.subtract(frozenConsumeQty).max(zero);
        BigDecimal afterPlanLockedQty = beforePlanLockedQty.subtract(planLockedConsumeQty).max(zero);
        LocalDateTime effectiveConsumeTime = consumeTime == null ? LocalDateTime.now() : consumeTime;
        String txnNo = buildTxnNo("WIPOUT", effectiveConsumeTime, refDocId == null ? locked.getId() : refDocId);

        HcInvStockDO updateStock = new HcInvStockDO();
        updateStock.setId(stock.getId());
        updateStock.setOnHandQty(afterQty);
        updateStock.setAvailableQty(afterAvailableQty);
        updateStock.setShareableQty(afterShareableQty);
        updateStock.setFrozenQty(afterFrozenQty);
        updateStock.setPlanLockedQty(afterPlanLockedQty);
        updateStock.setLastTxnNo(txnNo);
        updateStock.setLastTxnTime(effectiveConsumeTime);
        updateStock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.updateById(updateStock);

        BigDecimal consumedQty = normalizeQty(locked.getConsumedQty()).add(normalizedConsumeQty);
        BigDecimal releasedQty = normalizeQty(locked.getReleasedQty());
        BigDecimal remainingQty = normalizeQty(locked.getLockQty()).subtract(consumedQty).subtract(releasedQty).max(zero);
        HcPlanOrderInventoryLockDO updateLock = new HcPlanOrderInventoryLockDO();
        updateLock.setId(locked.getId());
        updateLock.setConsumedQty(consumedQty);
        updateLock.setReleasedQty(releasedQty);
        updateLock.setRemainingQty(remainingQty);
        updateLock.setConsumeReportId(refDocId);
        updateLock.setConsumeTime(effectiveConsumeTime);
        updateLock.setConsumeTxnNo(txnNo);
        updateLock.setLockStatus(remainingQty.compareTo(BigDecimal.ZERO) <= 0 ? LOCK_STATUS_CONSUMED : LOCK_STATUS_ACTIVE);
        updateLock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcPlanOrderInventoryLockMapper.updateById(updateLock);

        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(firstNonNull(stock.getTenantId(), locked.getTenantId()))
                .stockId(stock.getId())
                .stockType(firstNotBlank(stock.getStockType(), locked.getStockType(), STOCK_TYPE_WIP))
                .txnNo(txnNo)
                .txnType(TXN_TYPE_WIP_CONSUME)
                .txnTime(effectiveConsumeTime)
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .locationCode(stock.getLocationCode())
                .materialId(stock.getMaterialId())
                .materialCode(stock.getMaterialCode())
                .materialName(stock.getMaterialName())
                .modelNo(stock.getModelNo())
                .batchNo(stock.getBatchNo())
                .txnQty(normalizedConsumeQty.negate())
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .beforeAvailableQty(beforeAvailableQty)
                .afterAvailableQty(afterAvailableQty)
                .beforeFrozenQty(beforeFrozenQty)
                .afterFrozenQty(afterFrozenQty)
                .beforePlanLockedQty(beforePlanLockedQty)
                .afterPlanLockedQty(afterPlanLockedQty)
                .uom(firstNotBlank(stock.getUom(), locked.getUom()))
                .refDocType(refDocType)
                .refDocId(refDocId)
                .refDocNo(refDocNo)
                .sourceType(firstNotBlank(locked.getSourceType(), stock.getSourceType()))
                .sourceTable(firstNotBlank(locked.getSourceTable(), stock.getSourceTable()))
                .sourceId(firstNonNull(locked.getSourceId(), stock.getSourceId()))
                .sourceBatchNo(firstNotBlank(locked.getSourceBatchNo(), stock.getSourceBatchNo(), stock.getBatchNo()))
                .sourcePlanId(firstNonNull(locked.getSourcePlanId(), stock.getSourcePlanId()))
                .sourcePlanNo(firstNotBlank(locked.getSourcePlanNo(), stock.getSourcePlanNo()))
                .sourcePlanOperationId(firstNonNull(locked.getSourcePlanOperationId(), stock.getSourcePlanOperationId()))
                .targetPlanId(locked.getPlanId())
                .targetPlanNo(locked.getTargetPlanNo())
                .targetPlanOperationId(locked.getPlanOperationId())
                .targetOpCode(locked.getTargetOpCode())
                .targetOpName(locked.getTargetOpName())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark(firstNotBlank(remark, "下游工序确认消耗计划挂接中间边库"))
                .build();
        txnLog.setCreateTime(effectiveConsumeTime);
        txnLog.setUpdateTime(effectiveConsumeTime);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);

        stock.setOnHandQty(afterQty);
        stock.setAvailableQty(afterAvailableQty);
        stock.setShareableQty(afterShareableQty);
        stock.setFrozenQty(afterFrozenQty);
        stock.setPlanLockedQty(afterPlanLockedQty);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(effectiveConsumeTime);
        return stock;
    }

    private String buildTxnNo(LocalDateTime txnTime, Long sourceId) {
        return buildTxnNo("WIPIN", txnTime, sourceId);
    }

    private String buildTxnNo(String prefix, LocalDateTime txnTime, Long sourceId) {
        return prefix + "-" + TXN_NO_TIME_FORMATTER.format(txnTime) + "-" + sourceId;
    }

    private BigDecimal normalizeQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : value.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRemainingLockQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal remainingQty = normalizeQty(lock.getLockQty())
                .subtract(normalizeQty(lock.getConsumedQty()))
                .subtract(normalizeQty(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? normalizeQty(BigDecimal.ZERO) : remainingQty;
    }

    private BigDecimal minQty(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private Long resolveWipMaterialId(HcWipOutputPostReq req, HcPlanOrderDO plan) {
        return firstNonNull(req == null ? null : req.getMaterialId(),
                plan.getMaterialId(), plan.getMotherMaterialId());
    }

    private String resolveWipMaterialCode(HcWipOutputPostReq req, HcPlanOrderDO plan) {
        return StrUtil.blankToDefault(firstNotBlank(req == null ? null : req.getMaterialCode(),
                plan.getMaterialCode(), plan.getMotherMaterialCode()), "");
    }

    private String resolveWipMaterialName(HcWipOutputPostReq req, HcPlanOrderDO plan) {
        return StrUtil.blankToDefault(firstNotBlank(req == null ? null : req.getMaterialName(),
                plan.getMaterialName(), plan.getMotherMaterialName()), "");
    }

    private BigDecimal resolveInitialShareableQty(HcPlanOrderDO plan, HcPlanOrderOperationDO operation, BigDecimal outputQty) {
        if (!shouldAutoShareWipOutput(plan, operation)) {
            return normalizeQty(BigDecimal.ZERO);
        }
        return normalizeQty(outputQty);
    }

    private boolean shouldAutoShareWipOutput(HcPlanOrderDO plan, HcPlanOrderOperationDO operation) {
        if (plan == null || plan.getId() == null || operation == null || operation.getOpSeq() == null) {
            return false;
        }
        if (isFinalProductOperation(operation)) {
            return false;
        }
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(plan.getId());
        if (operations == null || operations.isEmpty()) {
            return false;
        }
        Integer currentOpSeq = operation.getOpSeq();
        return operations.stream()
                .filter(item -> item != null && !Objects.equals(item.getId(), operation.getId()))
                .map(HcPlanOrderOperationDO::getOpSeq)
                .filter(Objects::nonNull)
                .noneMatch(opSeq -> opSeq > currentOpSeq);
    }

    private boolean isFinalProductOperation(HcPlanOrderOperationDO operation) {
        String opCode = StrUtil.blankToDefault(operation.getOpCode(), "");
        String opName = StrUtil.blankToDefault(operation.getOpName(), "");
        String text = (opCode + " " + opName).toLowerCase();
        return StrUtil.contains(text, "cut")
                || StrUtil.contains(opName, "裁切");
    }

    private boolean isCrossPlanWipLock(HcInvStockDO stock, HcPlanOrderInventoryLockDO lock) {
        Long sourcePlanId = firstNonNull(lock.getSourcePlanId(), stock.getSourcePlanId());
        Long targetPlanId = lock.getPlanId();
        if (sourcePlanId != null && targetPlanId != null) {
            return !Objects.equals(sourcePlanId, targetPlanId);
        }
        String sourcePlanNo = firstNotBlank(lock.getSourcePlanNo(), stock.getSourcePlanNo());
        String targetPlanNo = StrUtil.trim(lock.getTargetPlanNo());
        return StrUtil.isNotBlank(sourcePlanNo)
                && StrUtil.isNotBlank(targetPlanNo)
                && !StrUtil.equals(sourcePlanNo, targetPlanNo);
    }

    private boolean isPlanSplitWipLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null || StrUtil.isBlank(lock.getRemark())) {
            return false;
        }
        return StrUtil.contains(lock.getRemark(), PLAN_SPLIT_LOCK_MARK)
                || StrUtil.contains(lock.getRemark(), "拆批生成新计划");
    }

    private boolean isDiscretePostProcessWipLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null || StrUtil.isBlank(lock.getRemark())) {
            return false;
        }
        return StrUtil.contains(lock.getRemark(), DISCRETE_POST_PROCESS_LOCK_MARK)
                || StrUtil.contains(lock.getRemark(), "离散后加工");
    }

    private String resolveSegmentName(String segmentCode) {
        if (StrUtil.isBlank(segmentCode)) {
            return "整卷";
        }
        return StrUtil.trim(segmentCode) + "段";
    }

    private String resolveBizStatus(HcPlanOrderDO plan) {
        String prodType = StrUtil.trim(plan.getProdType());
        String prodTypeName = StrUtil.trim(plan.getProdTypeName());
        if ("RND".equalsIgnoreCase(prodType) || "RND_TRIAL".equalsIgnoreCase(prodType)
                || StrUtil.contains(prodTypeName, "研发")) {
            return BIZ_STATUS_RND;
        }
        return BIZ_STATUS_MASS;
    }

    private String resolveWipLocationCode(HcPlanOrderOperationDO operation) {
        if (operation == null) {
            return DEFAULT_LOCATION_CODE;
        }
        return firstNotBlank(operation.getOpCode(), operation.getWorkCenterCode(), operation.getEquipmentCode(),
                operation.getId() == null ? null : "OP-" + operation.getId(), DEFAULT_LOCATION_CODE);
    }

    private String resolveWipLocationName(HcPlanOrderOperationDO operation) {
        if (operation == null) {
            return DEFAULT_LOCATION_NAME;
        }
        return firstNotBlank(operation.getOpName(), operation.getWorkCenterName(), operation.getEquipmentName(),
                operation.getOpCode(), DEFAULT_LOCATION_NAME);
    }

    private void fillMissingWipLocationIfNeeded(HcInvStockDO stock, HcPlanOrderOperationDO operation) {
        if (stock == null || stock.getId() == null || StrUtil.isNotBlank(stock.getLocationCode())) {
            return;
        }
        String locationCode = resolveWipLocationCode(operation);
        String warehouseCode = firstNotBlank(stock.getWarehouseCode(), DEFAULT_WIP_WAREHOUSE_CODE);
        if (stock.getMaterialId() != null && StrUtil.isNotBlank(stock.getBatchNo())) {
            HcInvStockDO sameKeyStock = hcInvStockMapper.selectOneByStockKey(warehouseCode,
                    stock.getMaterialId(), stock.getBatchNo(), locationCode);
            if (sameKeyStock != null && !stock.getId().equals(sameKeyStock.getId())) {
                return;
            }
        }
        HcInvStockDO update = new HcInvStockDO();
        update.setId(stock.getId());
        update.setLocationCode(locationCode);
        update.setLocationName(resolveWipLocationName(operation));
        hcInvStockMapper.updateById(update);
        stock.setLocationCode(update.getLocationCode());
        stock.setLocationName(update.getLocationName());
    }

    private String buildBusinessRemark(HcPlanOrderDO plan, HcPlanOrderOperationDO operation, HcGrindingSecondDetailDO detail) {
        return "磨皮二磨确认自动入库；来源计划：" + firstNotBlank(plan.getPlanNo(), detail.getPlanNo(), "-")
                + "；来源工序：" + firstNotBlank(operation.getOpName(), operation.getOpCode(), "-")
                + "；母批：" + firstNotBlank(detail.getMotherBatchNo(), detail.getParentProductionBatchNo(), "-");
    }

    private BigDecimal parseThickness(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = StrUtil.trim(value).replaceAll("[^0-9.\\-]", "");
        if (StrUtil.isBlank(normalized) || "-".equals(normalized) || ".".equals(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
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
                return StrUtil.trim(value);
            }
        }
        return null;
    }

}
