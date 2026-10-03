package cn.iocoder.yudao.module.mes.service.hc.intermediatestockledger.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.SortingField;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockHistoryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockHistoryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.intermediatestockledger.HcIntermediateStockLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.intermediatestockledger.HcIntermediateStockLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSecondDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.service.hc.intermediatestockledger.HcIntermediateStockLedgerService;
import cn.iocoder.yudao.module.mes.service.hc.processparam.HcProcessParamRecordService;
import cn.iocoder.yudao.module.mes.service.hc.processparam.dto.HcProcessParamRecordUpsertReq;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class HcIntermediateStockLedgerServiceImpl implements HcIntermediateStockLedgerService {

    private static final String LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String LOCK_STATUS_RELEASED = "RELEASED";

    private static final String STOCK_STATUS_AVAILABLE = "AVAILABLE";
    private static final String STOCK_STATUS_LOCKED = "LOCKED";
    private static final String STOCK_STATUS_FROZEN = "FROZEN";
    private static final String STOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String STOCK_STATUS_EMPTY = "EMPTY";

    private static final String TXN_TYPE_WIP_LOCK = "WIP_LOCK";
    private static final String TXN_TYPE_WIP_RELEASE = "WIP_RELEASE";
    private static final String TXN_TYPE_WIP_CONSUME = "WIP_CONSUME";
    private static final String TXN_TYPE_WIP_IN = "WIP_IN";
    private static final String SOURCE_TABLE_HISTORY_IMPORT = "history_intermediate_stock_import";
    private static final String DEFAULT_WIP_WAREHOUSE_CODE = "WH-WIP-LINE";
    private static final String DEFAULT_WIP_WAREHOUSE_NAME = "线边半成品库";
    private static final String DEFAULT_LOCATION_CODE = "WIP-UNKNOWN";
    private static final String DEFAULT_LOCATION_NAME = "未知工序位";
    private static final String DEFAULT_UOM = "m";
    private static final String DEFAULT_QUALITY_STATUS = "合格";
    private static final String DEFAULT_BIZ_STATUS = "量产";
    private static final DateTimeFormatter TXN_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String EVENT_TYPE_LOCK = "LOCK";
    private static final String EVENT_TYPE_CONSUME = "CONSUME";
    private static final String EVENT_TYPE_RELEASE = "RELEASE";
    private static final String SOURCE_TYPE_WET = "WET";
    private static final String SOURCE_TYPE_SLITTING = "SLITTING";
    private static final String REF_DOC_TYPE_ROUGH_FIRST = "ROUGH_GRINDING_FIRST";
    private static final String REF_DOC_TYPE_ROUGH_SECOND = "ROUGH_GRINDING_SECOND";

    private static final String ROW_TYPE_DETAIL = "DETAIL";
    private static final String ROW_TYPE_GROUP = "GROUP";
    private static final Set<String> MOTHER_BATCH_AGGREGATE_SOURCE_TYPES =
            Set.of("ADHESIVE1", "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");

    @Resource
    private HcIntermediateStockLedgerMapper intermediateStockLedgerMapper;
    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper planOrderInventoryLockMapper;
    @Resource
    private HcInvTxnLogMapper hcInvTxnLogMapper;
    @Resource
    private HcProcessParamRecordService hcProcessParamRecordService;
    @Resource
    private HcGrindingFirstDetailMapper hcGrindingFirstDetailMapper;
    @Resource
    private HcGrindingFirstAllocationMapper hcGrindingFirstAllocationMapper;
    @Resource
    private HcGrindingSecondDetailMapper hcGrindingSecondDetailMapper;
    @Resource
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;

    @Override
    public PageResult<HcIntermediateStockLedgerDO> getIntermediateStockLedgerPage(HcIntermediateStockLedgerPageReqVO pageReqVO) {
        PageResult<HcIntermediateStockLedgerDO> pageResult = intermediateStockLedgerMapper.selectPage(pageReqVO);
        fillLockSummary(pageResult.getList());
        pageResult.getList().forEach(row -> {
            row.setRowType(ROW_TYPE_DETAIL);
            row.setMotherBatchNo(resolveMotherBatchNo(row));
            row.setStockCount(1);
        });
        return pageResult;
    }

    @Override
    public PageResult<HcIntermediateStockLedgerDO> getIntermediateStockLedgerAggregatePage(HcIntermediateStockLedgerPageReqVO pageReqVO) {
        HcIntermediateStockLedgerPageReqVO queryReqVO = copyReqWithoutAggregateStatus(pageReqVO);
        List<HcIntermediateStockLedgerDO> rows = intermediateStockLedgerMapper
                .selectList(intermediateStockLedgerMapper.buildQuery(queryReqVO))
                .stream()
                .filter(row -> MOTHER_BATCH_AGGREGATE_SOURCE_TYPES.contains(StrUtil.trimToEmpty(row.getSourceType()).toUpperCase()))
                .toList();
        fillLockSummary(rows);
        List<HcIntermediateStockLedgerDO> aggregateRows = buildAggregateRows(rows);
        aggregateRows = filterAggregateRows(aggregateRows, pageReqVO);
        sortAggregateRows(aggregateRows, pageReqVO.getSortingFields());
        return pageAggregateRows(aggregateRows, pageReqVO);
    }

    @Override
    public List<HcIntermediateStockHistoryImportExcelVO> buildHistoryImportTemplate(HcIntermediateStockLedgerPageReqVO pageReqVO) {
        List<HcIntermediateStockLedgerDO> rows = intermediateStockLedgerMapper
                .selectList(intermediateStockLedgerMapper.buildQuery(pageReqVO));
        return rows.stream()
                .map(this::toHistoryImportExcel)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcIntermediateStockHistoryImportRespVO importHistoryStock(MultipartFile file) throws IOException {
        HcIntermediateStockHistoryImportRespVO respVO = new HcIntermediateStockHistoryImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }

        List<HcIntermediateStockHistoryImportExcelVO> excelRows =
                ExcelUtils.read(file, HcIntermediateStockHistoryImportExcelVO.class);
        List<HistoryImportRow> rows = new ArrayList<>();
        Set<String> importKeys = new HashSet<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcIntermediateStockHistoryImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            HistoryImportRow row = normalizeHistoryImportRow(excelRow, index + 2, respVO);
            if (row == null) {
                continue;
            }
            String importKey = buildImportDuplicateKey(row);
            if (!importKeys.add(importKey)) {
                addImportFailure(respVO, String.format("第%d行：同一文件内存在重复库存键 %s", row.rowNo, importKey));
                continue;
            }
            validateHistoryImportRow(row, respVO);
            rows.add(row);
        }

        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入任何历史库存数据");
            return respVO;
        }

        for (HistoryImportRow row : rows) {
            HistoryStockUpsertResult result = upsertHistoryStock(row);
            insertHistoryStockTxn(row, result);
            syncHistoryThickness(row, result.stock);
            if (result.created) {
                respVO.setCreatedCount(respVO.getCreatedCount() + 1);
            } else {
                respVO.setUpdatedCount(respVO.getUpdatedCount() + 1);
            }
        }
        respVO.setSuccessCount(rows.size());
        respVO.setFailureCount(0);
        respVO.getMessages().add(String.format("历史台账导入完成：成功 %d 行，新增 %d 条，更新 %d 条",
                respVO.getSuccessCount(), respVO.getCreatedCount(), respVO.getUpdatedCount()));
        return respVO;
    }

    private HcIntermediateStockHistoryImportExcelVO toHistoryImportExcel(HcIntermediateStockLedgerDO row) {
        HcIntermediateStockHistoryImportExcelVO excelVO = new HcIntermediateStockHistoryImportExcelVO();
        excelVO.setStockId(row.getId());
        excelVO.setSourcePlanNo(row.getSourcePlanNo());
        excelVO.setSourceType(row.getSourceType());
        excelVO.setOpSeq(row.getOpSeq());
        excelVO.setOpCode(row.getOpCode());
        excelVO.setOpName(row.getOpName());
        excelVO.setBatchNo(row.getBatchNo());
        excelVO.setSourceBatchNo(row.getSourceBatchNo());
        excelVO.setSourceParentBatchNo(row.getSourceParentBatchNo());
        excelVO.setMaterialCode(row.getMaterialCode());
        excelVO.setMaterialName(row.getMaterialName());
        excelVO.setModelNo(row.getModelNo());
        excelVO.setSpecSize(row.getSpecSize());
        excelVO.setSegmentCode(row.getSegmentCode());
        excelVO.setSegmentName(row.getSegmentName());
        excelVO.setThickness(row.getThickness());
        excelVO.setOnHandQty(row.getOnHandQty());
        excelVO.setAvailableQty(row.getAvailableQty());
        excelVO.setShareableQty(row.getShareableQty());
        excelVO.setFrozenQty(row.getFrozenQty());
        excelVO.setUom(row.getUom());
        excelVO.setQualityStatus(row.getQualityStatus());
        excelVO.setBizStatus(row.getBizStatus());
        excelVO.setWarehouseCode(row.getWarehouseCode());
        excelVO.setWarehouseName(row.getWarehouseName());
        excelVO.setLocationCode(row.getLocationCode());
        excelVO.setLocationName(row.getLocationName());
        excelVO.setProductionDate(formatDate(row.getProductionDate()));
        excelVO.setExpiryDate(formatDate(row.getExpiryDate()));
        excelVO.setLastTxnTime(formatDateTime(row.getLastTxnTime()));
        excelVO.setLastTxnNo(row.getLastTxnNo());
        excelVO.setBusinessRemark(row.getBusinessRemark());
        return excelVO;
    }

    private boolean isBlankImportRow(HcIntermediateStockHistoryImportExcelVO row) {
        if (row == null) {
            return true;
        }
        return row.getStockId() == null
                && StrUtil.isBlank(row.getSourcePlanNo())
                && StrUtil.isBlank(row.getSourceType())
                && row.getOpSeq() == null
                && StrUtil.isBlank(row.getOpCode())
                && StrUtil.isBlank(row.getOpName())
                && StrUtil.isBlank(row.getBatchNo())
                && StrUtil.isBlank(row.getSourceBatchNo())
                && StrUtil.isBlank(row.getSourceParentBatchNo())
                && StrUtil.isBlank(row.getMaterialCode())
                && StrUtil.isBlank(row.getMaterialName())
                && StrUtil.isBlank(row.getModelNo())
                && row.getOnHandQty() == null
                && row.getAvailableQty() == null
                && row.getShareableQty() == null;
    }

    private HistoryImportRow normalizeHistoryImportRow(HcIntermediateStockHistoryImportExcelVO excelRow,
                                                       int rowNo,
                                                       HcIntermediateStockHistoryImportRespVO respVO) {
        HistoryImportRow row = new HistoryImportRow();
        row.rowNo = rowNo;
        row.stockId = excelRow.getStockId();
        row.tenantId = currentTenantId();
        row.sourcePlanNo = StrUtil.trimToNull(excelRow.getSourcePlanNo());
        row.sourceType = normalizeSourceType(excelRow.getSourceType());
        row.opSeq = excelRow.getOpSeq();
        row.opCode = StrUtil.trimToNull(excelRow.getOpCode());
        row.opName = StrUtil.trimToNull(excelRow.getOpName());
        row.batchNo = firstNotBlank(excelRow.getBatchNo(), excelRow.getSourceBatchNo());
        row.sourceBatchNo = firstNotBlank(excelRow.getSourceBatchNo(), row.batchNo);
        row.sourceParentBatchNo = StrUtil.trimToNull(excelRow.getSourceParentBatchNo());
        row.materialCode = StrUtil.trimToNull(excelRow.getMaterialCode());
        row.materialName = StrUtil.trimToNull(excelRow.getMaterialName());
        row.modelNo = StrUtil.trimToNull(excelRow.getModelNo());
        row.specSize = StrUtil.trimToNull(excelRow.getSpecSize());
        row.segmentCode = firstNotBlank(excelRow.getSegmentCode(), "FULL");
        row.segmentName = firstNotBlank(excelRow.getSegmentName(), resolveSegmentName(row.segmentCode));
        row.thickness = normalizeQtyOrNull(excelRow.getThickness());
        row.onHandQty = normalizeQtyOrNull(excelRow.getOnHandQty());
        row.frozenQty = normalizeQty(firstNotNull(excelRow.getFrozenQty(), BigDecimal.ZERO));
        row.availableQty = normalizeQtyOrNull(excelRow.getAvailableQty());
        row.shareableQty = normalizeQtyOrNull(excelRow.getShareableQty());
        row.uom = firstNotBlank(excelRow.getUom(), DEFAULT_UOM);
        row.qualityStatus = firstNotBlank(excelRow.getQualityStatus(), DEFAULT_QUALITY_STATUS);
        row.bizStatus = firstNotBlank(excelRow.getBizStatus(), DEFAULT_BIZ_STATUS);
        row.warehouseCode = firstNotBlank(excelRow.getWarehouseCode(), DEFAULT_WIP_WAREHOUSE_CODE);
        row.warehouseName = firstNotBlank(excelRow.getWarehouseName(), DEFAULT_WIP_WAREHOUSE_NAME);
        row.locationCode = firstNotBlank(excelRow.getLocationCode(), DEFAULT_LOCATION_CODE);
        row.locationName = firstNotBlank(excelRow.getLocationName(), DEFAULT_LOCATION_NAME);
        row.productionDate = parseImportDate(excelRow.getProductionDate(), rowNo, "生产日期", respVO);
        row.expiryDate = parseImportDate(excelRow.getExpiryDate(), rowNo, "失效日期", respVO);
        row.lastTxnTime = firstNotNull(parseImportDateTime(excelRow.getLastTxnTime(), rowNo, "最近过账时间", respVO),
                LocalDateTime.now());
        row.externalTxnNo = StrUtil.trimToNull(excelRow.getLastTxnNo());
        row.businessRemark = firstNotBlank(excelRow.getBusinessRemark(), "历史中间品台账期初导入");
        if (row.onHandQty != null && row.availableQty == null) {
            row.availableQty = normalizeQty(row.onHandQty.subtract(row.frozenQty));
        }
        if (row.availableQty != null && row.shareableQty == null) {
            row.shareableQty = row.availableQty;
        }
        return row;
    }

    private void validateHistoryImportRow(HistoryImportRow row, HcIntermediateStockHistoryImportRespVO respVO) {
        if (StrUtil.isBlank(row.batchNo)) {
            addImportFailure(respVO, String.format("第%d行：中间品批号为空", row.rowNo));
        }
        if (StrUtil.isBlank(row.sourceType)) {
            addImportFailure(respVO, String.format("第%d行：工序来源类型为空", row.rowNo));
        }
        if (row.opSeq == null) {
            addImportFailure(respVO, String.format("第%d行：工序序号为空，计划利库无法按前序工序检索", row.rowNo));
        }
        if (StrUtil.isBlank(row.opCode) && StrUtil.isBlank(row.opName)) {
            addImportFailure(respVO, String.format("第%d行：工序编码和工序名称至少填写一个", row.rowNo));
        }
        if (StrUtil.isBlank(row.materialCode) && StrUtil.isBlank(row.materialName) && StrUtil.isBlank(row.modelNo)) {
            addImportFailure(respVO, String.format("第%d行：物料编码、物料名称、型号至少填写一个", row.rowNo));
        }
        if (row.onHandQty == null || row.onHandQty.compareTo(BigDecimal.ZERO) <= 0) {
            addImportFailure(respVO, String.format("第%d行：在库数量必须大于0", row.rowNo));
        }
        if (row.availableQty == null || row.availableQty.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：可用数量不能小于0", row.rowNo));
        }
        if (row.shareableQty == null || row.shareableQty.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：可利库数量不能小于0", row.rowNo));
        }
        if (row.frozenQty.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：冻结数量不能小于0", row.rowNo));
        }
        if (row.onHandQty != null && row.availableQty != null
                && row.availableQty.add(row.frozenQty).compareTo(row.onHandQty) > 0) {
            addImportFailure(respVO, String.format("第%d行：可用数量+冻结数量不能大于在库数量", row.rowNo));
        }
        if (row.availableQty != null && row.shareableQty != null
                && row.shareableQty.compareTo(row.availableQty) > 0) {
            addImportFailure(respVO, String.format("第%d行：可利库数量不能大于可用数量", row.rowNo));
        }
        HcInvStockDO existed = row.stockId == null ? selectHistoryStockByKey(row) : hcInvStockMapper.selectById(row.stockId);
        if (row.stockId != null && existed == null) {
            addImportFailure(respVO, String.format("第%d行：库存ID %d 不存在", row.rowNo, row.stockId));
            return;
        }
        if (existed != null && !isWipStock(existed)) {
            addImportFailure(respVO, String.format("第%d行：库存ID %d 不是WIP中间品库存", row.rowNo, existed.getId()));
        }
        if (existed != null && zeroIfNull(existed.getPlanLockedQty()).compareTo(BigDecimal.ZERO) > 0) {
            addImportFailure(respVO, String.format("第%d行：库存ID %d 已存在计划锁定量，不能通过历史导入覆盖", row.rowNo, existed.getId()));
        }
    }

    private HistoryStockUpsertResult upsertHistoryStock(HistoryImportRow row) {
        HcInvStockDO existed = row.stockId == null ? selectHistoryStockByKey(row) : hcInvStockMapper.selectById(row.stockId);
        LocalDateTime operateTime = row.lastTxnTime == null ? LocalDateTime.now() : row.lastTxnTime;
        String txnNo = buildHistoryTxnNo(operateTime, row.rowNo, firstNotNull(row.stockId, 0L));
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        HcInvStockDO before = copyStockSnapshot(existed);

        if (existed == null) {
            HcInvStockDO stock = new HcInvStockDO();
            fillImportedStock(stock, row, txnNo);
            stock.setTenantId(row.tenantId);
            stock.setStockType(HcIntermediateStockLedgerMapper.STOCK_TYPE_WIP);
            stock.setSourceTable(SOURCE_TABLE_HISTORY_IMPORT);
            stock.setFrozenQty(row.frozenQty);
            stock.setPlanLockedQty(normalizeQty(BigDecimal.ZERO));
            stock.setCreateTime(operateTime);
            stock.setUpdateTime(operateTime);
            stock.setCreator(operatorId == null ? null : String.valueOf(operatorId));
            stock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
            hcInvStockMapper.insert(stock);
            HcInvStockDO updateSource = new HcInvStockDO();
            updateSource.setId(stock.getId());
            updateSource.setSourceId(stock.getId());
            hcInvStockMapper.updateById(updateSource);
            stock.setSourceId(stock.getId());
            return new HistoryStockUpsertResult(stock, before, true, txnNo);
        }

        HcInvStockDO update = new HcInvStockDO();
        update.setId(existed.getId());
        fillImportedStock(update, row, txnNo);
        if (StrUtil.isBlank(existed.getSourceTable()) || SOURCE_TABLE_HISTORY_IMPORT.equalsIgnoreCase(existed.getSourceTable())) {
            update.setSourceTable(SOURCE_TABLE_HISTORY_IMPORT);
            update.setSourceId(firstNotNull(existed.getSourceId(), existed.getId()));
        }
        update.setFrozenQty(row.frozenQty);
        update.setPlanLockedQty(zeroIfNull(existed.getPlanLockedQty()));
        update.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.updateById(update);

        HcInvStockDO stock = hcInvStockMapper.selectById(existed.getId());
        return new HistoryStockUpsertResult(stock, before, false, txnNo);
    }

    private void fillImportedStock(HcInvStockDO stock, HistoryImportRow row, String txnNo) {
        stock.setSourceType(row.sourceType);
        stock.setSourcePlanNo(row.sourcePlanNo);
        stock.setSourceBatchNo(row.sourceBatchNo);
        stock.setSourceParentBatchNo(row.sourceParentBatchNo);
        stock.setWarehouseCode(row.warehouseCode);
        stock.setWarehouseName(row.warehouseName);
        stock.setLocationCode(row.locationCode);
        stock.setLocationName(row.locationName);
        stock.setMaterialCode(row.materialCode);
        stock.setMaterialName(row.materialName);
        stock.setModelNo(row.modelNo);
        stock.setSpecSize(row.specSize);
        stock.setOpSeq(row.opSeq);
        stock.setOpCode(row.opCode);
        stock.setOpName(row.opName);
        stock.setSegmentCode(row.segmentCode);
        stock.setSegmentName(row.segmentName);
        stock.setThickness(row.thickness);
        stock.setBatchNo(row.batchNo);
        stock.setProductionDate(row.productionDate);
        stock.setExpiryDate(row.expiryDate);
        stock.setOnHandQty(row.onHandQty);
        stock.setAvailableQty(row.availableQty);
        stock.setShareableQty(row.shareableQty);
        stock.setQualityStatus(row.qualityStatus);
        stock.setBizStatus(row.bizStatus);
        stock.setBusinessRemark(buildHistoryBusinessRemark(row));
        stock.setUom(row.uom);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(row.lastTxnTime);
    }

    private void insertHistoryStockTxn(HistoryImportRow row, HistoryStockUpsertResult result) {
        HcInvStockDO stock = result.stock;
        HcInvStockDO before = result.beforeStock;
        BigDecimal beforeOnHand = before == null ? BigDecimal.ZERO : zeroIfNull(before.getOnHandQty());
        BigDecimal beforeAvailable = before == null ? BigDecimal.ZERO : zeroIfNull(before.getAvailableQty());
        BigDecimal beforeFrozen = before == null ? BigDecimal.ZERO : zeroIfNull(before.getFrozenQty());
        BigDecimal beforePlanLocked = before == null ? BigDecimal.ZERO : zeroIfNull(before.getPlanLockedQty());
        BigDecimal afterOnHand = zeroIfNull(stock.getOnHandQty());
        BigDecimal afterAvailable = zeroIfNull(stock.getAvailableQty());
        BigDecimal afterFrozen = zeroIfNull(stock.getFrozenQty());
        BigDecimal afterPlanLocked = zeroIfNull(stock.getPlanLockedQty());
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();

        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(stock.getTenantId())
                .stockId(stock.getId())
                .stockType(HcIntermediateStockLedgerMapper.STOCK_TYPE_WIP)
                .txnNo(result.txnNo)
                .txnType(TXN_TYPE_WIP_IN)
                .txnTime(row.lastTxnTime)
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .locationCode(stock.getLocationCode())
                .materialId(stock.getMaterialId())
                .materialCode(stock.getMaterialCode())
                .materialName(stock.getMaterialName())
                .modelNo(stock.getModelNo())
                .batchNo(stock.getBatchNo())
                .txnQty(afterOnHand.subtract(beforeOnHand))
                .beforeQty(beforeOnHand)
                .afterQty(afterOnHand)
                .beforeAvailableQty(beforeAvailable)
                .afterAvailableQty(afterAvailable)
                .beforeFrozenQty(beforeFrozen)
                .afterFrozenQty(afterFrozen)
                .beforePlanLockedQty(beforePlanLocked)
                .afterPlanLockedQty(afterPlanLocked)
                .uom(stock.getUom())
                .refDocType("HISTORY_WIP_IMPORT")
                .refDocId(stock.getId())
                .refDocNo(firstNotBlank(row.externalTxnNo, stock.getBatchNo()))
                .sourceType(stock.getSourceType())
                .sourceTable(firstNotBlank(stock.getSourceTable(), SOURCE_TABLE_HISTORY_IMPORT))
                .sourceId(firstNotNull(stock.getSourceId(), stock.getId()))
                .sourceBatchNo(firstNotBlank(stock.getSourceBatchNo(), stock.getBatchNo()))
                .sourcePlanId(stock.getSourcePlanId())
                .sourcePlanNo(stock.getSourcePlanNo())
                .sourcePlanOperationId(stock.getSourcePlanOperationId())
                .creatorName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .remark(buildHistoryTxnRemark(row, result.created))
                .build();
        txnLog.setCreateTime(row.lastTxnTime);
        txnLog.setUpdateTime(row.lastTxnTime);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);
    }

    private void syncHistoryThickness(HistoryImportRow row, HcInvStockDO stock) {
        if (row.thickness == null || stock == null || stock.getTenantId() == null) {
            return;
        }
        String processCode = firstNotBlank(stock.getOpCode(), stock.getSourceType());
        String batchNo = firstNotBlank(stock.getBatchNo(), stock.getSourceBatchNo());
        if (StrUtil.isBlank(processCode) || StrUtil.isBlank(batchNo)) {
            return;
        }
        hcProcessParamRecordService.upsertProcessParam(HcProcessParamRecordUpsertReq.builder()
                .tenantId(stock.getTenantId())
                .planId(stock.getSourcePlanId())
                .planNo(stock.getSourcePlanNo())
                .planOperationId(stock.getSourcePlanOperationId())
                .batchNo(batchNo)
                .processCode(processCode)
                .processName(firstNotBlank(stock.getOpName(), stock.getSourceType()))
                .paramCode(HcProcessParamRecordService.PARAM_CODE_THICKNESS)
                .paramName(HcProcessParamRecordService.PARAM_NAME_THICKNESS)
                .paramValue(row.thickness.toPlainString())
                .paramValueNum(row.thickness)
                .uom("mm")
                .recordTime(firstNotNull(row.lastTxnTime, LocalDateTime.now()))
                .recorderId(SecurityFrameworkUtils.getLoginUserId())
                .recorderName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .sourceType("HISTORY_WIP_IMPORT")
                .sourceTable("mes_inv_stock")
                .sourceId(stock.getId())
                .sourceFormCode("HISTORY_WIP_IMPORT")
                .sourceFormName("历史中间品台账导入")
                .remark("历史中间品台账导入同步厚度")
                .build());
    }

    private HcInvStockDO selectHistoryStockByKey(HistoryImportRow row) {
        LambdaQueryWrapperX<HcInvStockDO> query = new LambdaQueryWrapperX<HcInvStockDO>()
                .eq(HcInvStockDO::getTenantId, row.tenantId)
                .eq(HcInvStockDO::getStockType, HcIntermediateStockLedgerMapper.STOCK_TYPE_WIP)
                .eq(HcInvStockDO::getSourceTable, SOURCE_TABLE_HISTORY_IMPORT)
                .eq(HcInvStockDO::getBatchNo, row.batchNo)
                .eq(HcInvStockDO::getSourceType, row.sourceType)
                .eq(HcInvStockDO::getOpSeq, row.opSeq)
                .eq(HcInvStockDO::getLocationCode, row.locationCode)
                .eq(HcInvStockDO::getDeleted, false)
                .eqIfPresent(HcInvStockDO::getSourcePlanNo, row.sourcePlanNo)
                .eqIfPresent(HcInvStockDO::getOpCode, row.opCode)
                .eqIfPresent(HcInvStockDO::getMaterialCode, row.materialCode)
                .eqIfPresent(HcInvStockDO::getModelNo, row.modelNo)
                .eqIfPresent(HcInvStockDO::getSegmentCode, row.segmentCode)
                .last("LIMIT 1");
        return hcInvStockMapper.selectOne(query);
    }

    private HcInvStockDO copyStockSnapshot(HcInvStockDO source) {
        if (source == null) {
            return null;
        }
        HcInvStockDO target = new HcInvStockDO();
        target.setId(source.getId());
        target.setOnHandQty(source.getOnHandQty());
        target.setAvailableQty(source.getAvailableQty());
        target.setShareableQty(source.getShareableQty());
        target.setFrozenQty(source.getFrozenQty());
        target.setPlanLockedQty(source.getPlanLockedQty());
        return target;
    }

    private String buildHistoryBusinessRemark(HistoryImportRow row) {
        String remark = firstNotBlank(row.businessRemark, "历史中间品台账期初导入");
        if (StrUtil.isNotBlank(row.externalTxnNo)) {
            return remark + "；外部历史流水：" + row.externalTxnNo;
        }
        return remark;
    }

    private String buildHistoryTxnRemark(HistoryImportRow row, boolean created) {
        String action = created ? "历史中间品台账期初导入" : "历史中间品台账导入更新";
        String remark = action + "；批次：" + row.batchNo + "；工序：" + firstNotBlank(row.opName, row.opCode, row.sourceType);
        if (StrUtil.isNotBlank(row.externalTxnNo)) {
            remark += "；外部历史流水：" + row.externalTxnNo;
        }
        if (StrUtil.isNotBlank(row.businessRemark)) {
            remark += "；备注：" + row.businessRemark;
        }
        return remark;
    }

    private String buildHistoryTxnNo(LocalDateTime txnTime, int rowNo, Long seed) {
        LocalDateTime effectiveTime = txnTime == null ? LocalDateTime.now() : txnTime;
        long suffix = Math.abs(firstNotNull(seed, 0L)) % 1000;
        return "HISWIP" + effectiveTime.format(TXN_NO_TIME_FORMATTER)
                + String.format("%03d%03d", Math.abs(rowNo) % 1000, suffix);
    }

    private String buildImportDuplicateKey(HistoryImportRow row) {
        if (row.stockId != null) {
            return "ID#" + row.stockId;
        }
        return String.join("#",
                StrUtil.trimToEmpty(row.sourcePlanNo).toUpperCase(),
                StrUtil.trimToEmpty(row.sourceType).toUpperCase(),
                String.valueOf(row.opSeq),
                StrUtil.trimToEmpty(row.opCode).toUpperCase(),
                StrUtil.trimToEmpty(row.locationCode).toUpperCase(),
                StrUtil.trimToEmpty(row.materialCode).toUpperCase(),
                StrUtil.trimToEmpty(row.modelNo).toUpperCase(),
                StrUtil.trimToEmpty(row.batchNo).toUpperCase(),
                StrUtil.trimToEmpty(row.segmentCode).toUpperCase());
    }

    private String normalizeSourceType(String value) {
        String text = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String upper = text.toUpperCase();
        return switch (upper) {
            case "湿法", "WET" -> "WET";
            case "磨皮", "磨皮二磨", "GRINDING", "GRINDING_SECOND", "ROUGH_GRINDING_SECOND" -> "GRINDING_SECOND";
            case "粘胶1", "粘胶一", "ADHESIVE1", "ADH1" -> "ADHESIVE1";
            case "分切", "SLITTING" -> "SLITTING";
            case "压槽", "PRESS_SLOT", "PRESSSLOT" -> "PRESS_SLOT";
            case "粘胶2", "粘胶二", "ADHESIVE2", "ADH2" -> "ADHESIVE2";
            case "裁切", "CUT_ROUND", "CUTROUND" -> "CUT_ROUND";
            default -> upper;
        };
    }

    private String resolveSegmentName(String segmentCode) {
        String code = StrUtil.trimToEmpty(segmentCode).toUpperCase();
        if (StrUtil.isBlank(code) || "FULL".equals(code)) {
            return "整卷";
        }
        return code;
    }

    private boolean isWipStock(HcInvStockDO stock) {
        return stock != null && HcIntermediateStockLedgerMapper.STOCK_TYPE_WIP.equalsIgnoreCase(stock.getStockType());
    }

    private LocalDate parseImportDate(String value, int rowNo, String fieldName,
                                      HcIntermediateStockHistoryImportRespVO respVO) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalizeDateText(text), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd", rowNo, fieldName));
            return null;
        }
    }

    private LocalDateTime parseImportDateTime(String value, int rowNo, String fieldName,
                                              HcIntermediateStockHistoryImportRespVO respVO) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            String normalized = normalizeDateText(text);
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized, DATE_FORMATTER).atStartOfDay();
            }
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd HH:mm:ss", rowNo, fieldName));
            return null;
        }
    }

    private String normalizeDateText(String value) {
        return StrUtil.trim(value).replace('/', '-');
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private BigDecimal normalizeQtyOrNull(BigDecimal value) {
        return value == null ? null : normalizeQty(value);
    }

    private BigDecimal normalizeQty(BigDecimal value) {
        return zeroIfNull(value).setScale(6, RoundingMode.HALF_UP);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? TenantContextHolder.getRequiredTenantId() : tenantId;
    }

    private void addImportFailure(HcIntermediateStockHistoryImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private HcIntermediateStockLedgerPageReqVO copyReqWithoutAggregateStatus(HcIntermediateStockLedgerPageReqVO source) {
        HcIntermediateStockLedgerPageReqVO target = new HcIntermediateStockLedgerPageReqVO();
        target.setKeyword(source.getKeyword());
        target.setSourcePlanNo(source.getSourcePlanNo());
        target.setSourceType(source.getSourceType());
        target.setBatchNo(source.getBatchNo());
        target.setSourceBatchNo(source.getSourceBatchNo());
        target.setSourceParentBatchNo(source.getSourceParentBatchNo());
        target.setMaterialCode(source.getMaterialCode());
        target.setModelNo(source.getModelNo());
        target.setQualityStatus(source.getQualityStatus());
        target.setTxnDateStart(source.getTxnDateStart());
        target.setTxnDateEnd(source.getTxnDateEnd());
        target.setSortingFields(source.getSortingFields());
        target.setStockStatus(null);
        target.setOnlyAvailable(false);
        return target;
    }

    private List<HcIntermediateStockLedgerDO> buildAggregateRows(List<HcIntermediateStockLedgerDO> rows) {
        Map<String, HcIntermediateStockLedgerDO> aggregateMap = new LinkedHashMap<>();
        for (HcIntermediateStockLedgerDO row : rows) {
            String motherBatchNo = resolveMotherBatchNo(row);
            if (StrUtil.isBlank(motherBatchNo)) {
                continue;
            }
            String aggregateKey = buildAggregateKey(row, motherBatchNo);
            HcIntermediateStockLedgerDO aggregate = aggregateMap.computeIfAbsent(aggregateKey,
                    key -> initAggregateRow(row, motherBatchNo, aggregateMap.size() + 1));
            mergeAggregateRow(aggregate, row);
        }
        List<HcIntermediateStockLedgerDO> aggregateRows = new ArrayList<>(aggregateMap.values());
        for (HcIntermediateStockLedgerDO row : aggregateRows) {
            row.setStockStatus(resolveStockStatus(row, null));
            row.setTxnSummary(buildAggregateTxnSummary(row));
            row.setBusinessRemark(buildAggregateTxnSummary(row));
        }
        return aggregateRows;
    }

    private HcIntermediateStockLedgerDO initAggregateRow(HcIntermediateStockLedgerDO source,
                                                        String motherBatchNo,
                                                        int index) {
        HcIntermediateStockLedgerDO row = new HcIntermediateStockLedgerDO();
        row.setId(-1L * index);
        row.setRowType(ROW_TYPE_GROUP);
        row.setStockType(source.getStockType());
        row.setSourceType(source.getSourceType());
        row.setSourcePlanNo(source.getSourcePlanNo());
        row.setSourcePlanId(source.getSourcePlanId());
        row.setSourcePlanOperationId(source.getSourcePlanOperationId());
        row.setSourceBatchNo(source.getSourceBatchNo());
        row.setSourceParentBatchNo(motherBatchNo);
        row.setMotherBatchNo(motherBatchNo);
        row.setWarehouseCode(source.getWarehouseCode());
        row.setWarehouseName(source.getWarehouseName());
        row.setLocationCode(source.getLocationCode());
        row.setLocationName(source.getLocationName());
        row.setMaterialId(source.getMaterialId());
        row.setMaterialCode(source.getMaterialCode());
        row.setMaterialName(source.getMaterialName());
        row.setModelNo(source.getModelNo());
        row.setSpecSize(source.getSpecSize());
        row.setOpSeq(source.getOpSeq());
        row.setOpCode(source.getOpCode());
        row.setOpName(source.getOpName());
        row.setBatchNo(motherBatchNo);
        row.setUom(source.getUom());
        row.setQualityStatus(source.getQualityStatus());
        row.setBizStatus(source.getBizStatus());
        row.setOnHandQty(BigDecimal.ZERO);
        row.setAvailableQty(BigDecimal.ZERO);
        row.setShareableQty(BigDecimal.ZERO);
        row.setFrozenQty(BigDecimal.ZERO);
        row.setPlanLockedQty(BigDecimal.ZERO);
        row.setLockedQty(BigDecimal.ZERO);
        row.setConsumedQty(BigDecimal.ZERO);
        row.setReleasedQty(BigDecimal.ZERO);
        row.setLockRemainingQty(BigDecimal.ZERO);
        row.setStockCount(0);
        row.setTxnDetails(List.of());
        return row;
    }

    private void mergeAggregateRow(HcIntermediateStockLedgerDO aggregate, HcIntermediateStockLedgerDO detail) {
        aggregate.setOnHandQty(zeroIfNull(aggregate.getOnHandQty()).add(zeroIfNull(detail.getOnHandQty())));
        aggregate.setAvailableQty(zeroIfNull(aggregate.getAvailableQty()).add(zeroIfNull(detail.getAvailableQty())));
        aggregate.setShareableQty(zeroIfNull(aggregate.getShareableQty()).add(zeroIfNull(detail.getShareableQty())));
        aggregate.setFrozenQty(zeroIfNull(aggregate.getFrozenQty()).add(zeroIfNull(detail.getFrozenQty())));
        aggregate.setPlanLockedQty(zeroIfNull(aggregate.getPlanLockedQty()).add(zeroIfNull(detail.getPlanLockedQty())));
        aggregate.setLockedQty(zeroIfNull(aggregate.getLockedQty()).add(zeroIfNull(detail.getLockedQty())));
        aggregate.setConsumedQty(zeroIfNull(aggregate.getConsumedQty()).add(zeroIfNull(detail.getConsumedQty())));
        aggregate.setReleasedQty(zeroIfNull(aggregate.getReleasedQty()).add(zeroIfNull(detail.getReleasedQty())));
        aggregate.setLockRemainingQty(zeroIfNull(aggregate.getLockRemainingQty()).add(zeroIfNull(detail.getLockRemainingQty())));
        aggregate.setStockCount(zeroIfNull(aggregate.getStockCount()) + 1);
        aggregate.setSourcePlanNo(mergeDisplayText(aggregate.getSourcePlanNo(), detail.getSourcePlanNo(), "多计划"));
        aggregate.setMaterialCode(mergeDisplayText(aggregate.getMaterialCode(), detail.getMaterialCode(), "多物料"));
        aggregate.setMaterialName(mergeDisplayText(aggregate.getMaterialName(), detail.getMaterialName(), "多物料"));
        aggregate.setModelNo(mergeDisplayText(aggregate.getModelNo(), detail.getModelNo(), "多型号"));
        aggregate.setQualityStatus(mergeDisplayText(aggregate.getQualityStatus(), detail.getQualityStatus(), "混合"));
        if (isAfter(detail.getLastTxnTime(), aggregate.getLastTxnTime())) {
            aggregate.setLastTxnTime(detail.getLastTxnTime());
            aggregate.setLastTxnNo(detail.getLastTxnNo());
        }
        if (aggregate.getCreateTime() == null || isAfter(aggregate.getCreateTime(), detail.getCreateTime())) {
            aggregate.setCreateTime(detail.getCreateTime());
        }
        if (isAfter(detail.getUpdateTime(), aggregate.getUpdateTime())) {
            aggregate.setUpdateTime(detail.getUpdateTime());
        }
    }

    private String buildAggregateKey(HcIntermediateStockLedgerDO row, String motherBatchNo) {
        return String.join("\u001F",
                StrUtil.trimToEmpty(motherBatchNo).toUpperCase(),
                StrUtil.trimToEmpty(row.getSourceType()).toUpperCase(),
                StrUtil.trimToEmpty(row.getMaterialCode()).toUpperCase(),
                StrUtil.trimToEmpty(row.getModelNo()).toUpperCase(),
                StrUtil.trimToEmpty(row.getUom()).toUpperCase());
    }

    private List<HcIntermediateStockLedgerDO> filterAggregateRows(List<HcIntermediateStockLedgerDO> rows,
                                                                  HcIntermediateStockLedgerPageReqVO reqVO) {
        if (rows.isEmpty()) {
            return rows;
        }
        return new ArrayList<>(rows.stream()
                .filter(row -> !Boolean.TRUE.equals(reqVO.getOnlyAvailable())
                        || zeroIfNull(row.getAvailableQty()).compareTo(BigDecimal.ZERO) > 0)
                .filter(row -> isAggregateStockStatusMatched(row, reqVO.getStockStatus()))
                .toList());
    }

    private boolean isAggregateStockStatusMatched(HcIntermediateStockLedgerDO row, String stockStatus) {
        if (StrUtil.isBlank(stockStatus)) {
            return true;
        }
        return switch (stockStatus) {
            case STOCK_STATUS_AVAILABLE -> zeroIfNull(row.getAvailableQty()).compareTo(BigDecimal.ZERO) > 0;
            case STOCK_STATUS_LOCKED -> zeroIfNull(row.getPlanLockedQty()).compareTo(BigDecimal.ZERO) > 0;
            case STOCK_STATUS_FROZEN -> zeroIfNull(row.getFrozenQty()).compareTo(BigDecimal.ZERO) > 0;
            case STOCK_STATUS_CONSUMED -> zeroIfNull(row.getOnHandQty()).compareTo(BigDecimal.ZERO) <= 0;
            default -> true;
        };
    }

    private void sortAggregateRows(List<HcIntermediateStockLedgerDO> rows, List<SortingField> sortingFields) {
        Comparator<HcIntermediateStockLedgerDO> comparator = null;
        if (sortingFields != null) {
            Set<String> usedFields = new HashSet<>();
            for (SortingField sortingField : sortingFields) {
                if (sortingField == null || StrUtil.isBlank(sortingField.getField())
                        || !usedFields.add(sortingField.getField())) {
                    continue;
                }
                Comparator<HcIntermediateStockLedgerDO> fieldComparator = buildAggregateComparator(sortingField.getField());
                if (fieldComparator == null) {
                    continue;
                }
                if (SortingField.ORDER_DESC.equalsIgnoreCase(sortingField.getOrder())) {
                    fieldComparator = fieldComparator.reversed();
                }
                comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
            }
        }
        if (comparator == null) {
            comparator = defaultAggregateComparator();
        }
        rows.sort(comparator.thenComparing(defaultAggregateComparator()));
    }

    private Comparator<HcIntermediateStockLedgerDO> buildAggregateComparator(String field) {
        if ("sourceType".equals(field)) {
            return Comparator.comparing(HcIntermediateStockLedgerDO::getOpSeq, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(row -> StrUtil.trimToEmpty(row.getOpName()), String.CASE_INSENSITIVE_ORDER);
        }
        return switch (field) {
            case "sourcePlanNo" -> stringComparator(HcIntermediateStockLedgerDO::getSourcePlanNo);
            case "sourceParentBatchNo", "motherBatchNo", "batchNo" -> stringComparator(HcIntermediateStockLedgerDO::getMotherBatchNo);
            case "modelNo" -> stringComparator(HcIntermediateStockLedgerDO::getModelNo);
            case "onHandQty" -> Comparator.comparing(row -> zeroIfNull(row.getOnHandQty()));
            case "availableQty" -> Comparator.comparing(row -> zeroIfNull(row.getAvailableQty()));
            case "shareableQty" -> Comparator.comparing(row -> zeroIfNull(row.getShareableQty()));
            case "planLockedQty" -> Comparator.comparing(row -> zeroIfNull(row.getPlanLockedQty()));
            case "consumedQty" -> Comparator.comparing(row -> zeroIfNull(row.getConsumedQty()));
            case "lastTxnTime" -> Comparator.comparing(HcIntermediateStockLedgerDO::getLastTxnTime,
                    Comparator.nullsLast(LocalDateTime::compareTo));
            case "businessRemark" -> stringComparator(HcIntermediateStockLedgerDO::getTxnSummary);
            default -> null;
        };
    }

    private Comparator<HcIntermediateStockLedgerDO> stringComparator(java.util.function.Function<HcIntermediateStockLedgerDO, String> getter) {
        return Comparator.comparing(row -> StrUtil.trimToEmpty(getter.apply(row)), String.CASE_INSENSITIVE_ORDER);
    }

    private Comparator<HcIntermediateStockLedgerDO> defaultAggregateComparator() {
        return Comparator.comparing(HcIntermediateStockLedgerDO::getMotherBatchNo,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(HcIntermediateStockLedgerDO::getOpSeq, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(row -> StrUtil.trimToEmpty(row.getOpName()), String.CASE_INSENSITIVE_ORDER);
    }

    private PageResult<HcIntermediateStockLedgerDO> pageAggregateRows(List<HcIntermediateStockLedgerDO> rows,
                                                                      HcIntermediateStockLedgerPageReqVO reqVO) {
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.max(1, reqVO.getPageSize());
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(fromIndex >= toIndex ? List.of() : rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private String resolveMotherBatchNo(HcIntermediateStockLedgerDO row) {
        return firstNotBlank(row.getMotherBatchNo(), row.getSourceParentBatchNo(), row.getSourceBatchNo(), row.getBatchNo());
    }

    private String buildAggregateTxnSummary(HcIntermediateStockLedgerDO row) {
        return "共 " + row.getStockCount() + " 片；可用 " + formatQty(row.getAvailableQty(), row.getUom())
                + "；锁定 " + formatQty(row.getPlanLockedQty(), row.getUom())
                + "；消耗 " + formatQty(row.getConsumedQty(), row.getUom())
                + "；释放 " + formatQty(row.getReleasedQty(), row.getUom());
    }

    private String mergeDisplayText(String exists, String incoming, String mixedText) {
        String left = StrUtil.trimToEmpty(exists);
        String right = StrUtil.trimToEmpty(incoming);
        if (StrUtil.isBlank(left)) {
            return StrUtil.isBlank(right) ? null : right;
        }
        if (StrUtil.isBlank(right) || StrUtil.equalsIgnoreCase(left, right) || StrUtil.equals(left, mixedText)) {
            return left;
        }
        return mixedText;
    }

    private boolean isAfter(LocalDateTime left, LocalDateTime right) {
        return left != null && (right == null || left.isAfter(right));
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private void fillLockSummary(List<HcIntermediateStockLedgerDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> stockIds = rows.stream()
                .map(HcIntermediateStockLedgerDO::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<HcPlanOrderInventoryLockDO>> lockMap = planOrderInventoryLockMapper.selectListByStockIds(stockIds)
                .stream()
                .filter(lock -> lock.getStockId() != null)
                .collect(Collectors.groupingBy(HcPlanOrderInventoryLockDO::getStockId));
        Map<Long, List<HcInvTxnLogDO>> txnLogMap = selectTxnLogMap(stockIds);
        for (HcIntermediateStockLedgerDO row : rows) {
            if (row == null || row.getId() == null) {
                continue;
            }
            List<HcPlanOrderInventoryLockDO> locks = lockMap.getOrDefault(row.getId(), List.of());
            List<HcIntermediateStockLedgerDO.TxnDetail> txnDetails = buildTxnDetails(row, locks,
                    txnLogMap.getOrDefault(row.getId(), List.of()));
            BigDecimal lockedQty = BigDecimal.ZERO;
            BigDecimal consumedQty = BigDecimal.ZERO;
            BigDecimal releasedQty = BigDecimal.ZERO;
            BigDecimal remainingQty = BigDecimal.ZERO;
            HcPlanOrderInventoryLockDO activeLock = null;
            for (HcPlanOrderInventoryLockDO lock : locks) {
                lockedQty = lockedQty.add(zeroIfNull(lock.getLockQty()));
                consumedQty = consumedQty.add(zeroIfNull(lock.getConsumedQty()));
                releasedQty = releasedQty.add(zeroIfNull(lock.getReleasedQty()));
                BigDecimal lockRemaining = calculateRemainingLockQty(lock);
                remainingQty = remainingQty.add(lockRemaining);
                if (activeLock == null && isActiveWipLock(lock)) {
                    activeLock = lock;
                }
            }
            HcPlanOrderInventoryLockDO displayLock = activeLock;
            if (displayLock == null && !locks.isEmpty()) {
                displayLock = locks.stream()
                        .max(Comparator.comparing(HcPlanOrderInventoryLockDO::getId, Comparator.nullsLast(Long::compareTo)))
                        .orElse(null);
            }
            row.setLockedQty(lockedQty);
            row.setConsumedQty(consumedQty);
            row.setReleasedQty(releasedQty);
            row.setLockRemainingQty(remainingQty);
            row.setStockStatus(resolveStockStatus(row, activeLock));
            if (displayLock != null) {
                row.setActiveLockId(displayLock.getId());
                row.setActiveLockStatus(displayLock.getLockStatus());
                row.setActiveLockTargetPlanNo(displayLock.getTargetPlanNo());
                row.setActiveLockTargetOpName(firstNotBlank(displayLock.getTargetOpName(), displayLock.getTargetOpCode()));
                row.setActiveLockRemainingQty(calculateRemainingLockQty(displayLock));
            }
            row.setTxnDetails(txnDetails);
            row.setTxnSummary(buildTxnSummary(row, displayLock, txnDetails));
        }
    }

    private Map<Long, List<HcInvTxnLogDO>> selectTxnLogMap(List<Long> stockIds) {
        if (stockIds == null || stockIds.isEmpty()) {
            return Map.of();
        }
        return hcInvTxnLogMapper.selectList(new LambdaQueryWrapperX<HcInvTxnLogDO>()
                        .in(HcInvTxnLogDO::getStockId, stockIds)
                        .in(HcInvTxnLogDO::getTxnType, List.of(TXN_TYPE_WIP_LOCK, TXN_TYPE_WIP_CONSUME, TXN_TYPE_WIP_RELEASE))
                        .eq(HcInvTxnLogDO::getDeleted, false)
                        .orderByAsc(HcInvTxnLogDO::getTxnTime)
                        .orderByAsc(HcInvTxnLogDO::getId))
                .stream()
                .filter(log -> log.getStockId() != null)
                .collect(Collectors.groupingBy(HcInvTxnLogDO::getStockId));
    }

    private boolean isActiveWipLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null || Boolean.TRUE.equals(lock.getDeleted())) {
            return false;
        }
        String status = lock.getLockStatus() == null ? LOCK_STATUS_ACTIVE : lock.getLockStatus();
        if (LOCK_STATUS_CONSUMED.equalsIgnoreCase(status)
                || LOCK_STATUS_CANCELLED.equalsIgnoreCase(status)
                || LOCK_STATUS_RELEASED.equalsIgnoreCase(status)) {
            return false;
        }
        return calculateRemainingLockQty(lock).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateRemainingLockQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal remainingQty = zeroIfNull(lock.getLockQty())
                .subtract(zeroIfNull(lock.getConsumedQty()))
                .subtract(zeroIfNull(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingQty;
    }

    private String resolveStockStatus(HcIntermediateStockLedgerDO row, HcPlanOrderInventoryLockDO activeLock) {
        if (zeroIfNull(row.getOnHandQty()).compareTo(BigDecimal.ZERO) <= 0) {
            return STOCK_STATUS_CONSUMED;
        }
        if (activeLock != null || zeroIfNull(row.getPlanLockedQty()).compareTo(BigDecimal.ZERO) > 0) {
            return STOCK_STATUS_LOCKED;
        }
        if (zeroIfNull(row.getFrozenQty()).compareTo(BigDecimal.ZERO) > 0) {
            return STOCK_STATUS_FROZEN;
        }
        if (zeroIfNull(row.getAvailableQty()).compareTo(BigDecimal.ZERO) > 0) {
            return STOCK_STATUS_AVAILABLE;
        }
        return STOCK_STATUS_EMPTY;
    }

    private List<HcIntermediateStockLedgerDO.TxnDetail> buildTxnDetails(HcIntermediateStockLedgerDO row,
                                                                         List<HcPlanOrderInventoryLockDO> locks,
                                                                         List<HcInvTxnLogDO> txnLogs) {
        List<HcIntermediateStockLedgerDO.TxnDetail> details = new ArrayList<>();
        Map<Long, HcPlanOrderInventoryLockDO> lockById = locks.stream()
                .filter(lock -> lock.getId() != null)
                .collect(Collectors.toMap(HcPlanOrderInventoryLockDO::getId, lock -> lock, (first, second) -> first));
        Set<String> detailTxnNos = new HashSet<>();
        for (HcInvTxnLogDO txnLog : txnLogs) {
            HcPlanOrderInventoryLockDO relatedLock = lockById.get(txnLog.getRefDocId());
            HcIntermediateStockLedgerDO.TxnDetail detail = buildTxnDetailFromLog(row, txnLog, relatedLock, locks);
            if (detail == null) {
                continue;
            }
            details.add(detail);
            if (StrUtil.isNotBlank(detail.getTxnNo())) {
                detailTxnNos.add(detail.getTxnNo());
            }
        }
        appendFallbackLockDetails(row, locks, detailTxnNos, details);
        appendRoughGrindingReportDetails(row, locks, details);
        details.sort(Comparator
                .comparing(HcIntermediateStockLedgerDO.TxnDetail::getTxnTime,
                        Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(HcIntermediateStockLedgerDO.TxnDetail::getRefDocType,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(HcIntermediateStockLedgerDO.TxnDetail::getRefDocId,
                        Comparator.nullsLast(Long::compareTo)));
        return details;
    }

    private void appendRoughGrindingReportDetails(HcIntermediateStockLedgerDO row,
                                                  List<HcPlanOrderInventoryLockDO> locks,
                                                  List<HcIntermediateStockLedgerDO.TxnDetail> details) {
        if (!SOURCE_TYPE_WET.equalsIgnoreCase(row.getSourceType()) || locks == null || locks.isEmpty()) {
            return;
        }
        Set<String> existedKeys = details.stream()
                .filter(Objects::nonNull)
                .map(detail -> firstNotBlank(detail.getRefDocType(), "-") + ":" + detail.getRefDocId())
                .collect(Collectors.toSet());
        Set<Long> targetOperationIds = locks.stream()
                .map(HcPlanOrderInventoryLockDO::getPlanOperationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        for (Long targetOperationId : targetOperationIds) {
            List<HcGrindingFirstDetailDO> firstDetails = hcGrindingFirstDetailMapper.selectListByPlanOperationId(targetOperationId)
                    .stream()
                    .filter(firstDetail -> matchesWetStockBatch(row, firstDetail))
                    .toList();
            if (firstDetails.isEmpty()) {
                continue;
            }
            Map<Long, HcGrindingFirstAllocationDO> allocationByFirstDetailId = hcGrindingFirstAllocationMapper
                    .selectList(new LambdaQueryWrapperX<HcGrindingFirstAllocationDO>()
                            .eq(HcGrindingFirstAllocationDO::getPlanOperationId, targetOperationId)
                            .eq(HcGrindingFirstAllocationDO::getDeleted, false))
                    .stream()
                    .filter(allocation -> !"VOID".equalsIgnoreCase(allocation.getDetailStatus()))
                    .filter(allocation -> allocation.getFirstDetailId() != null)
                    .collect(Collectors.toMap(HcGrindingFirstAllocationDO::getFirstDetailId,
                            allocation -> allocation, (left, right) -> left, LinkedHashMap::new));
            List<HcGrindingSecondDetailDO> secondDetails = hcGrindingSecondDetailMapper.selectListByPlanOperationId(targetOperationId);
            for (HcGrindingFirstDetailDO firstDetail : firstDetails) {
                HcGrindingFirstAllocationDO firstAllocation = allocationByFirstDetailId.get(firstDetail.getId());
                appendRoughFirstDetail(row, firstDetail, existedKeys, details);
                secondDetails.stream()
                        .filter(secondDetail -> matchesRoughSecondDetail(firstDetail, firstAllocation, secondDetail))
                        .forEach(secondDetail -> appendRoughSecondDetail(row, firstDetail, firstAllocation,
                                secondDetail, existedKeys, details));
            }
        }
    }

    private void appendRoughFirstDetail(HcIntermediateStockLedgerDO row,
                                        HcGrindingFirstDetailDO firstDetail,
                                        Set<String> existedKeys,
                                        List<HcIntermediateStockLedgerDO.TxnDetail> details) {
        if (firstDetail == null || firstDetail.getId() == null
                || !existedKeys.add(REF_DOC_TYPE_ROUGH_FIRST + ":" + firstDetail.getId())) {
            return;
        }
        HcIntermediateStockLedgerDO.TxnDetail detail = new HcIntermediateStockLedgerDO.TxnDetail();
        detail.setEventType(EVENT_TYPE_LOCK);
        detail.setEventTypeName("一磨报工");
        detail.setTxnTime(firstNotNull(firstDetail.getEndTime(), firstDetail.getStartTime(), firstDetail.getCreateTime()));
        detail.setStockId(row.getId());
        detail.setTargetPlanNo(firstDetail.getPlanNo());
        detail.setTargetPlanOperationId(firstDetail.getPlanOperationId());
        detail.setTargetOpName("一磨");
        detail.setQty(zeroIfNull(firstDetail.getProcessLength()));
        detail.setUom(firstNotBlank(row.getUom(), "m"));
        detail.setRefDocType(REF_DOC_TYPE_ROUGH_FIRST);
        detail.setRefDocId(firstDetail.getId());
        detail.setDisplayBatchNo(resolveStockBatchNo(row));
        detail.setRemark("一磨报工已占用湿法中间品，等待二磨扫码确认后销账");
        detail.setSummary("一磨工序锁定 " + formatQty(detail.getQty(), detail.getUom())
                + "，状态：已报工，中间品/片号：" + resolveDetailBatchNo(row, detail));
        details.add(detail);
    }

    private void appendRoughSecondDetail(HcIntermediateStockLedgerDO row,
                                         HcGrindingFirstDetailDO firstDetail,
                                         HcGrindingFirstAllocationDO firstAllocation,
                                         HcGrindingSecondDetailDO secondDetail,
                                         Set<String> existedKeys,
                                         List<HcIntermediateStockLedgerDO.TxnDetail> details) {
        if (secondDetail == null || secondDetail.getId() == null
                || !existedKeys.add(REF_DOC_TYPE_ROUGH_SECOND + ":" + secondDetail.getId())) {
            return;
        }
        boolean confirmed = "CONFIRMED".equalsIgnoreCase(secondDetail.getConfirmStatus());
        String statusName = confirmed ? "已扫码确认" : "报工未确认";
        BigDecimal displayQty = zeroIfNull(secondDetail.getProcessLength()).compareTo(BigDecimal.ZERO) > 0
                ? zeroIfNull(secondDetail.getProcessLength())
                : zeroIfNull(firstAllocation == null
                        ? (firstDetail == null ? null : firstDetail.getProcessLength())
                        : firstAllocation.getConfirmedLength());
        HcIntermediateStockLedgerDO.TxnDetail detail = new HcIntermediateStockLedgerDO.TxnDetail();
        detail.setEventType(EVENT_TYPE_CONSUME);
        detail.setEventTypeName(confirmed ? "消耗" : "待消耗");
        detail.setTxnTime(firstNotNull(secondDetail.getConfirmTime(), secondDetail.getEndTime(),
                secondDetail.getStartTime(), secondDetail.getCreateTime()));
        detail.setStockId(row.getId());
        detail.setTargetPlanNo(secondDetail.getPlanNo());
        detail.setTargetPlanOperationId(secondDetail.getPlanOperationId());
        detail.setTargetOpName("二磨");
        detail.setQty(displayQty);
        detail.setUom(firstNotBlank(row.getUom(), "m"));
        detail.setRefDocType(REF_DOC_TYPE_ROUGH_SECOND);
        detail.setRefDocId(secondDetail.getId());
        detail.setRefDocNo(firstNotBlank(secondDetail.getConfirmedBatchNo(), secondDetail.getProductionBatchNo()));
        detail.setDisplayBatchNo(resolveTxnDetailDisplayBatchNo(row, detail));
        detail.setRemark("二磨报工状态：" + statusName + "；批号："
                + firstNotBlank(secondDetail.getConfirmedBatchNo(), secondDetail.getProductionBatchNo(), "-"));
        detail.setSummary("二磨工序" + (confirmed ? "使用 " : "报工 ")
                + formatQty(displayQty, detail.getUom())
                + "，状态：" + statusName
                + (confirmed ? "" : "，待确认后消耗")
                + "，中间品/片号：" + resolveDetailBatchNo(row, detail));
        details.add(detail);
    }

    private boolean matchesWetStockBatch(HcIntermediateStockLedgerDO row, HcGrindingFirstDetailDO firstDetail) {
        if (row == null || firstDetail == null || Boolean.TRUE.equals(firstDetail.getDeleted())) {
            return false;
        }
        String firstBatchNo = firstNotBlank(firstDetail.getMotherBatchNo(), firstDetail.getSourceProductionBatchNo());
        return StrUtil.isBlank(firstBatchNo)
                || isSameBatch(firstBatchNo, row.getBatchNo())
                || isSameBatch(firstBatchNo, row.getSourceBatchNo())
                || isSameBatch(firstBatchNo, row.getSourceParentBatchNo());
    }

    private boolean matchesRoughSecondDetail(HcGrindingFirstDetailDO firstDetail,
                                             HcGrindingFirstAllocationDO firstAllocation,
                                             HcGrindingSecondDetailDO secondDetail) {
        if (firstDetail == null || secondDetail == null || Boolean.TRUE.equals(secondDetail.getDeleted())) {
            return false;
        }
        if (secondDetail.getFirstAllocationId() != null) {
            return firstAllocation != null
                    && Objects.equals(firstAllocation.getId(), secondDetail.getFirstAllocationId())
                    && Objects.equals(firstAllocation.getFirstDetailId(), firstDetail.getId());
        }
        // 已进入一磨前置分配的加工单元不允许退回母批关联，避免同母批 P/Q/R/S/NONE 串段。
        if (firstAllocation != null) {
            return false;
        }
        if (secondDetail.getFirstDetailId() != null) {
            return Objects.equals(firstDetail.getId(), secondDetail.getFirstDetailId());
        }
        if (StrUtil.isNotBlank(firstDetail.getRowUid()) && StrUtil.isNotBlank(secondDetail.getSourceRowUid())) {
            return StrUtil.equals(StrUtil.trim(firstDetail.getRowUid()), StrUtil.trim(secondDetail.getSourceRowUid()));
        }
        return isSameBatch(firstDetail.getMotherBatchNo(), secondDetail.getMotherBatchNo());
    }

    private HcIntermediateStockLedgerDO.TxnDetail buildTxnDetailFromLog(HcIntermediateStockLedgerDO row,
                                                                        HcInvTxnLogDO txnLog,
                                                                        HcPlanOrderInventoryLockDO relatedLock,
                                                                        List<HcPlanOrderInventoryLockDO> locks) {
        if (txnLog == null || StrUtil.isBlank(txnLog.getTxnType())) {
            return null;
        }
        String eventType = resolveEventType(txnLog.getTxnType());
        if (eventType == null) {
            return null;
        }
        BigDecimal qty = resolveTxnQty(txnLog, relatedLock);
        HcIntermediateStockLedgerDO.TxnDetail detail = new HcIntermediateStockLedgerDO.TxnDetail();
        detail.setEventType(eventType);
        detail.setEventTypeName(resolveEventTypeName(eventType));
        detail.setTxnId(txnLog.getId());
        detail.setTxnNo(txnLog.getTxnNo());
        detail.setTxnType(txnLog.getTxnType());
        detail.setTxnTime(txnLog.getTxnTime());
        detail.setStockId(txnLog.getStockId());
        detail.setLockId(resolveTxnLockId(txnLog, relatedLock, locks));
        detail.setTargetPlanNo(firstNotBlank(txnLog.getTargetPlanNo(),
                relatedLock == null ? null : relatedLock.getTargetPlanNo()));
        detail.setTargetPlanOperationId(txnLog.getTargetPlanOperationId() == null && relatedLock != null
                ? relatedLock.getPlanOperationId() : txnLog.getTargetPlanOperationId());
        detail.setTargetOpName(firstNotBlank(txnLog.getTargetOpName(), txnLog.getTargetOpCode(),
                relatedLock == null ? null : relatedLock.getTargetOpName(),
                relatedLock == null ? null : relatedLock.getTargetOpCode()));
        detail.setQty(qty);
        detail.setUom(firstNotBlank(txnLog.getUom(), relatedLock == null ? null : relatedLock.getUom(), row.getUom()));
        detail.setRefDocType(txnLog.getRefDocType());
        detail.setRefDocId(txnLog.getRefDocId());
        detail.setRefDocNo(txnLog.getRefDocNo());
        detail.setDisplayBatchNo(resolveTxnDetailDisplayBatchNo(row, detail, txnLog));
        detail.setRemark(txnLog.getRemark());
        detail.setSummary(buildTxnDetailSummary(row, detail));
        enrichRoughSecondTxnSummary(row, detail);
        return detail;
    }

    private void enrichRoughSecondTxnSummary(HcIntermediateStockLedgerDO row,
                                             HcIntermediateStockLedgerDO.TxnDetail detail) {
        if (detail == null || !REF_DOC_TYPE_ROUGH_SECOND.equalsIgnoreCase(detail.getRefDocType())
                || detail.getRefDocId() == null) {
            return;
        }
        HcGrindingSecondDetailDO secondDetail = hcGrindingSecondDetailMapper.selectById(detail.getRefDocId());
        boolean confirmed = secondDetail == null || "CONFIRMED".equalsIgnoreCase(secondDetail.getConfirmStatus());
        String statusName = confirmed ? "已扫码确认" : "报工未确认";
        detail.setTargetOpName("二磨");
        detail.setSummary("二磨工序使用 " + formatQty(detail.getQty(), detail.getUom())
                + "，状态：" + statusName
                + "，中间品/片号：" + resolveDetailBatchNo(row, detail));
    }

    private void appendFallbackLockDetails(HcIntermediateStockLedgerDO row,
                                           List<HcPlanOrderInventoryLockDO> locks,
                                           Set<String> detailTxnNos,
                                           List<HcIntermediateStockLedgerDO.TxnDetail> details) {
        for (HcPlanOrderInventoryLockDO lock : locks) {
            if (zeroIfNull(lock.getLockQty()).compareTo(BigDecimal.ZERO) > 0
                    && !containsTxnNo(detailTxnNos, lock.getLockTxnNo())) {
                details.add(buildTxnDetailFromLock(row, lock, EVENT_TYPE_LOCK, zeroIfNull(lock.getLockQty()),
                        lock.getLockTxnNo(), lock.getCreateTime(), "锁记录补充"));
            }
            if (zeroIfNull(lock.getConsumedQty()).compareTo(BigDecimal.ZERO) > 0
                    && !containsTxnNo(detailTxnNos, lock.getConsumeTxnNo())) {
                details.add(buildTxnDetailFromLock(row, lock, EVENT_TYPE_CONSUME, zeroIfNull(lock.getConsumedQty()),
                        lock.getConsumeTxnNo(), lock.getConsumeTime(), "锁记录补充"));
            }
            if (zeroIfNull(lock.getReleasedQty()).compareTo(BigDecimal.ZERO) > 0
                    && !containsTxnNo(detailTxnNos, lock.getReleaseTxnNo())) {
                details.add(buildTxnDetailFromLock(row, lock, EVENT_TYPE_RELEASE, zeroIfNull(lock.getReleasedQty()),
                        lock.getReleaseTxnNo(), lock.getReleaseTime(), firstNotBlank(lock.getReleaseReason(), "锁记录补充")));
            }
        }
    }

    private HcIntermediateStockLedgerDO.TxnDetail buildTxnDetailFromLock(HcIntermediateStockLedgerDO row,
                                                                         HcPlanOrderInventoryLockDO lock,
                                                                         String eventType,
                                                                         BigDecimal qty,
                                                                         String txnNo,
                                                                         java.time.LocalDateTime txnTime,
                                                                         String remark) {
        HcIntermediateStockLedgerDO.TxnDetail detail = new HcIntermediateStockLedgerDO.TxnDetail();
        detail.setEventType(eventType);
        detail.setEventTypeName(resolveEventTypeName(eventType));
        detail.setTxnNo(txnNo);
        detail.setTxnTime(txnTime);
        detail.setStockId(lock.getStockId());
        detail.setLockId(lock.getId());
        detail.setTargetPlanNo(lock.getTargetPlanNo());
        detail.setTargetPlanOperationId(lock.getPlanOperationId());
        detail.setTargetOpName(firstNotBlank(lock.getTargetOpName(), lock.getTargetOpCode()));
        detail.setQty(qty);
        detail.setUom(firstNotBlank(lock.getUom(), row.getUom()));
        detail.setRefDocType("PLAN_WIP_" + eventType);
        detail.setRefDocId(lock.getId());
        detail.setDisplayBatchNo(resolveStockBatchNo(row));
        detail.setRemark(remark);
        detail.setSummary(buildTxnDetailSummary(row, detail));
        return detail;
    }

    private Long resolveTxnLockId(HcInvTxnLogDO txnLog,
                                  HcPlanOrderInventoryLockDO relatedLock,
                                  List<HcPlanOrderInventoryLockDO> locks) {
        if (relatedLock != null) {
            return relatedLock.getId();
        }
        if ((TXN_TYPE_WIP_LOCK.equalsIgnoreCase(txnLog.getTxnType())
                || TXN_TYPE_WIP_RELEASE.equalsIgnoreCase(txnLog.getTxnType()))
                && txnLog.getRefDocId() != null) {
            return txnLog.getRefDocId();
        }
        return locks.size() == 1 ? locks.get(0).getId() : null;
    }

    private String resolveEventType(String txnType) {
        if (TXN_TYPE_WIP_LOCK.equalsIgnoreCase(txnType)) {
            return EVENT_TYPE_LOCK;
        }
        if (TXN_TYPE_WIP_CONSUME.equalsIgnoreCase(txnType)) {
            return EVENT_TYPE_CONSUME;
        }
        if (TXN_TYPE_WIP_RELEASE.equalsIgnoreCase(txnType)) {
            return EVENT_TYPE_RELEASE;
        }
        return null;
    }

    private String resolveEventTypeName(String eventType) {
        if (EVENT_TYPE_LOCK.equalsIgnoreCase(eventType)) {
            return "锁定";
        }
        if (EVENT_TYPE_CONSUME.equalsIgnoreCase(eventType)) {
            return "消耗";
        }
        if (EVENT_TYPE_RELEASE.equalsIgnoreCase(eventType)) {
            return "释放";
        }
        return eventType;
    }

    private BigDecimal resolveTxnQty(HcInvTxnLogDO txnLog, HcPlanOrderInventoryLockDO relatedLock) {
        BigDecimal qty = zeroIfNull(txnLog.getTxnQty()).abs();
        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            return qty;
        }
        if (TXN_TYPE_WIP_LOCK.equalsIgnoreCase(txnLog.getTxnType())) {
            qty = zeroIfNull(txnLog.getAfterPlanLockedQty()).subtract(zeroIfNull(txnLog.getBeforePlanLockedQty())).abs();
            return qty.compareTo(BigDecimal.ZERO) > 0 || relatedLock == null ? qty : zeroIfNull(relatedLock.getLockQty());
        }
        if (TXN_TYPE_WIP_RELEASE.equalsIgnoreCase(txnLog.getTxnType())) {
            qty = zeroIfNull(txnLog.getBeforePlanLockedQty()).subtract(zeroIfNull(txnLog.getAfterPlanLockedQty())).abs();
            return qty.compareTo(BigDecimal.ZERO) > 0 || relatedLock == null ? qty : zeroIfNull(relatedLock.getReleasedQty());
        }
        return qty;
    }

    private String buildTxnDetailSummary(HcIntermediateStockLedgerDO row,
                                         HcIntermediateStockLedgerDO.TxnDetail detail) {
        String opName = firstNotBlank(detail.getTargetOpName(), "下游");
        String verb = switch (detail.getEventType()) {
            case EVENT_TYPE_LOCK -> "锁定";
            case EVENT_TYPE_RELEASE -> "释放";
            default -> "使用";
        };
        return opName + "工序" + verb + " " + formatQty(detail.getQty(), detail.getUom())
                + "，中间品/片号：" + resolveDetailBatchNo(row, detail);
    }

    private String resolveDetailBatchNo(HcIntermediateStockLedgerDO row,
                                        HcIntermediateStockLedgerDO.TxnDetail detail) {
        String displayBatchNo = detail == null ? null : detail.getDisplayBatchNo();
        if (StrUtil.isNotBlank(displayBatchNo)) {
            return StrUtil.trim(displayBatchNo);
        }
        return resolveStockBatchNo(row);
    }

    private String resolveStockBatchNo(HcIntermediateStockLedgerDO row) {
        if (row == null) {
            return "-";
        }
        return firstNotBlank(row.getBatchNo(), row.getSourceBatchNo(), row.getSourceParentBatchNo(), "-");
    }

    private String resolveTxnDetailDisplayBatchNo(HcIntermediateStockLedgerDO row,
                                                 HcIntermediateStockLedgerDO.TxnDetail detail) {
        return resolveTxnDetailDisplayBatchNo(row, detail, null);
    }

    private String resolveTxnDetailDisplayBatchNo(HcIntermediateStockLedgerDO row,
                                                 HcIntermediateStockLedgerDO.TxnDetail detail,
                                                 HcInvTxnLogDO txnLog) {
        if (detail != null && EVENT_TYPE_CONSUME.equalsIgnoreCase(detail.getEventType())) {
            if (SOURCE_TYPE_SLITTING.equalsIgnoreCase(detail.getRefDocType())) {
                String consumedBatchNo = firstNotBlank(detail.getRefDocNo(), txnLog == null ? null : txnLog.getRefDocNo());
                if (StrUtil.isBlank(consumedBatchNo) && detail.getRefDocId() != null) {
                    HcSlittingSliceRecordDO sliceRecord = hcSlittingSliceRecordMapper.selectById(detail.getRefDocId());
                    consumedBatchNo = sliceRecord == null ? null : sliceRecord.getSliceSerialNo();
                }
                if (StrUtil.isNotBlank(consumedBatchNo)) {
                    return StrUtil.trim(consumedBatchNo);
                }
            }
            if (REF_DOC_TYPE_ROUGH_SECOND.equalsIgnoreCase(detail.getRefDocType())
                    && StrUtil.isNotBlank(detail.getRefDocNo())) {
                return StrUtil.trim(detail.getRefDocNo());
            }
        }
        return resolveStockBatchNo(row);
    }

    private boolean containsTxnNo(Set<String> txnNos, String txnNo) {
        return StrUtil.isNotBlank(txnNo) && txnNos.contains(txnNo);
    }

    private String buildTxnSummary(HcIntermediateStockLedgerDO row,
                                   HcPlanOrderInventoryLockDO displayLock,
                                   List<HcIntermediateStockLedgerDO.TxnDetail> txnDetails) {
        if (txnDetails != null && !txnDetails.isEmpty()) {
            String summary = txnDetails.stream()
                    .limit(2)
                    .map(HcIntermediateStockLedgerDO.TxnDetail::getSummary)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining("；"));
            if (txnDetails.size() > 2) {
                summary = summary + "；等" + txnDetails.size() + "笔";
            }
            if (StrUtil.isNotBlank(summary)) {
                return summary;
            }
        }
        StringBuilder summary = new StringBuilder();
        if (row.getLastTxnTime() != null || StrUtil.isNotBlank(row.getLastTxnNo())) {
            summary.append("最近流水")
                    .append(StrUtil.isBlank(row.getLastTxnNo()) ? "" : "：" + row.getLastTxnNo());
        }
        if (displayLock != null) {
            if (summary.length() > 0) {
                summary.append("；");
            }
            summary.append("锁定状态：")
                    .append(firstNotBlank(displayLock.getLockStatus(), "-"));
            if (StrUtil.isNotBlank(displayLock.getTargetPlanNo())) {
                summary.append("，计划：").append(displayLock.getTargetPlanNo());
            }
        }
        return summary.length() == 0 ? row.getBusinessRemark() : summary.toString();
    }

    private String formatQty(BigDecimal qty, String uom) {
        String value = zeroIfNull(qty).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return StrUtil.isBlank(uom) ? value : value + " " + StrUtil.trim(uom);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
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

    private boolean isSameBatch(String left, String right) {
        return StrUtil.isNotBlank(left) && StrUtil.isNotBlank(right)
                && StrUtil.equalsIgnoreCase(StrUtil.trim(left), StrUtil.trim(right));
    }

    private static final class HistoryImportRow {

        private int rowNo;
        private Long stockId;
        private Long tenantId;
        private String sourcePlanNo;
        private String sourceType;
        private Integer opSeq;
        private String opCode;
        private String opName;
        private String batchNo;
        private String sourceBatchNo;
        private String sourceParentBatchNo;
        private String materialCode;
        private String materialName;
        private String modelNo;
        private String specSize;
        private String segmentCode;
        private String segmentName;
        private BigDecimal thickness;
        private BigDecimal onHandQty;
        private BigDecimal availableQty;
        private BigDecimal shareableQty;
        private BigDecimal frozenQty;
        private String uom;
        private String qualityStatus;
        private String bizStatus;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String locationName;
        private LocalDate productionDate;
        private LocalDate expiryDate;
        private LocalDateTime lastTxnTime;
        private String externalTxnNo;
        private String businessRemark;

    }

    private static final class HistoryStockUpsertResult {

        private final HcInvStockDO stock;
        private final HcInvStockDO beforeStock;
        private final boolean created;
        private final String txnNo;

        private HistoryStockUpsertResult(HcInvStockDO stock, HcInvStockDO beforeStock, boolean created, String txnNo) {
            this.stock = stock;
            this.beforeStock = beforeStock;
            this.created = created;
            this.txnNo = txnNo;
        }

    }

}
