package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingAddInnerItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingAddOuterUnitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingCreateInnerUnitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingCreateOuterBoxReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingInnerUnitItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingInnerUnitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingOuterBoxItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingOuterBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingUnitActionReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcLabelPrintLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcOuterPackBoxDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcOuterPackBoxItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcLabelPrintLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcOuterPackBoxItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcOuterPackBoxMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingSourceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import cn.iocoder.yudao.module.mes.service.hc.equipment.HcEquipmentService;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.qms.QmsCutRoundFqcService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcPackagingConsoleServiceImpl implements HcPackagingConsoleService {

    private static final String PROCESS_CODE = "PACKAGING";
    private static final String SOURCE_MENU_CODE = "PACKAGING_REPORT";
    private static final String OP_STATUS_RELEASED = "RELEASED";
    private static final String OP_STATUS_RUNNING = "RUNNING";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String REPORT_TYPE_START = "START";
    private static final String REPORT_TYPE_END = "END";
    private static final String LABEL_TYPE_INNER = "INNER";
    private static final String LABEL_TYPE_OUTER = "OUTER";
    private static final String PLAN_LOCK_TYPE_WIP = "WIP";
    private static final String PLAN_LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String PLAN_LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String PLAN_LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String PLAN_LOCK_STATUS_RELEASED = "RELEASED";
    private static final String SOURCE_TYPE_CUT_ROUND = "CUT_ROUND";
    private static final String SOURCE_TYPE_PACKAGING = "PACKAGING";
    private static final String SOURCE_TABLE_CUT_ROUND_REPORT = "mes_sfc_cut_round_report";

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;
    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcInvStockService hcInvStockService;
    @Resource
    private HcProcessReportMapper hcProcessReportMapper;
    @Resource
    private HcPackagingSourceMapper hcPackagingSourceMapper;
    @Resource
    private HcPackReportMapper hcPackReportMapper;
    @Resource
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Resource
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Resource
    private HcOuterPackBoxMapper hcOuterPackBoxMapper;
    @Resource
    private HcOuterPackBoxItemMapper hcOuterPackBoxItemMapper;
    @Resource
    private HcLabelPrintLogMapper hcLabelPrintLogMapper;
    @Resource
    private HcStationFormMapper hcStationFormMapper;
    @Resource
    private HcStationFormItemMapper hcStationFormItemMapper;
    @Resource
    private HcStationRecordMapper hcStationRecordMapper;
    @Resource
    private HcStationRecordItemMapper hcStationRecordItemMapper;
    @Resource
    private HcEquipmentService hcEquipmentService;
    @Resource
    private QmsCutRoundFqcService qmsCutRoundFqcService;

    @Override
    public List<HcPackagingTaskRespVO> getTaskList(HcPackagingTaskPageReqVO reqVO) {
        String taskStatus = StrUtil.blankToDefault(reqVO.getTaskStatus(), "ALL");
        return hcProcessReportMapper.selectPackagingTaskList(
                taskStatus, reqVO.getTaskKeyword(), reqVO.getProductKeyword(),
                reqVO.getMotherMaterialKeyword(), reqVO.getMotherModelKeyword(), reqVO.getProductionDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(HcPackagingStartReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        if (OP_STATUS_FINISHED.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前包装工序已完工，不能再开工");
        }
        if (OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前包装工序已开工");
        }
        LocalDate reportDate = reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = normalizeReportDateTime(reqVO.getStartTime());
        LocalDateTime recorderTime = normalizeReportDateTime(reqVO.getRecorderTime());
        if (startTime == null) {
            startTime = now;
        }
        if (recorderTime == null) {
            recorderTime = now;
        }
        HcProcessReportDO reportDO = HcProcessReportDO.builder()
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationStatus(operation.getOperationStatus())
                .operationSeq(operation.getOpSeq())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .workCenterId(operation.getWorkCenterId())
                .workCenterCode(operation.getWorkCenterCode())
                .workCenterName(operation.getWorkCenterName())
                .equipmentId(reqVO.getEquipmentId())
                .equipmentCode(firstNotBlank(reqVO.getEquipmentCode(), operation.getEquipmentCode()))
                .equipmentName(firstNotBlank(reqVO.getEquipmentName(), operation.getEquipmentName()))
                .materialId(planOrder.getMaterialId())
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                .productionBatchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo()))
                .parentProductionBatchNo(firstNotBlank(operation.getParentProductionBatchNo(), planOrder.getParentProductionBatchNo(), planOrder.getProductionBatchNo()))
                .reportDate(reportDate)
                .startTime(startTime)
                .goodQty(BigDecimal.ZERO)
                .scrapQty(BigDecimal.ZERO)
                .recorderName(firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(recorderTime)
                .reportUom(firstNotBlank(operation.getUom(), operation.getUnitCode(), operation.getUnitName(), "片"))
                .reportType(REPORT_TYPE_START)
                .sourceMenuCode(SOURCE_MENU_CODE)
                .build();
        hcProcessReportMapper.insert(reportDO);

        HcPlanOrderOperationDO updateObj = new HcPlanOrderOperationDO();
        updateObj.setId(operation.getId());
        updateObj.setOperationStatus(OP_STATUS_RUNNING);
        updateObj.setEquipmentId(reqVO.getEquipmentId());
        updateObj.setEquipmentCode(firstNotBlank(reqVO.getEquipmentCode(), operation.getEquipmentCode()));
        updateObj.setEquipmentName(firstNotBlank(reqVO.getEquipmentName(), operation.getEquipmentName()));
        updateObj.setStatusOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setStatusOperatorName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "system"));
        updateObj.setStatusOperateTime(now);
        hcPlanOrderOperationMapper.updateById(updateObj);
        if (reqVO.getEquipmentId() != null) {
            hcEquipmentService.occupyEquipment(reqVO.getEquipmentId(), planOrder.getPlanNo(), operation.getOpCode(),
                    operation.getOpName(), reportDO.getStartTime(), reportDO.getRecorderName(), reportDO.getRecorderTime());
        }
        upsertPackReport(planOrder, operation, "RUNNING", reportDO.getRecorderName(), now, null, null);
        return reportDO.getId();
    }

    @Override
    public List<HcPackagingPassWorkRespVO> getPassWorkList(Long planId, Long planOperationId) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(planId);
        HcPlanOrderOperationDO operation = validateOperation(planOrder, planOperationId);
        Long equipmentId = operation.getEquipmentId();
        LocalDate recordDate = LocalDate.now();
        List<HcStationFormDO> forms = hcStationFormMapper.selectEnabledByProcess(PROCESS_CODE);
        List<HcPackagingPassWorkRespVO> rows = new ArrayList<>();
        for (HcStationFormDO form : forms) {
            HcStationRecordDO record = equipmentId == null ? null
                    : hcStationRecordMapper.selectOneByEquipmentDaily(equipmentId, recordDate, form.getFormCode());
            rows.add(buildPassWorkResp(form, record));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePassWork(HcPackagingPassWorkSaveReqVO reqVO) {
        return upsertPassWork(reqVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmPassWork(HcPackagingPassWorkSaveReqVO reqVO) {
        return upsertPassWork(reqVO, true);
    }

    @Override
    public HcPackagingSummaryRespVO getSummary(Long planId, Long planOperationId) {
        if (planId == null || planOperationId == null) {
            return new HcPackagingSummaryRespVO();
        }
        HcPackagingSummaryRespVO resp = new HcPackagingSummaryRespVO();
        List<HcPackagingSourceRespVO> sources = hcPackagingSourceMapper.selectConfirmedListByPlanId(planId);
        List<HcInnerPackUnitDO> innerUnits = hcInnerPackUnitMapper.selectListByPlanOperationId(planOperationId);
        List<HcInnerPackUnitItemDO> innerItems = hcInnerPackUnitItemMapper.selectListByPlanOperationId(planOperationId);
        List<HcOuterPackBoxDO> outerBoxes = hcOuterPackBoxMapper.selectListByPlanOperationId(planOperationId);
        resp.setSourcePieceCount(sources.size());
        resp.setInnerUnitCount(innerUnits.size());
        resp.setInnerPieceCount(innerItems.size());
        resp.setUnpackedPieceCount(Math.max(0, sources.size() - innerItems.size()));
        resp.setReviewedInnerUnitCount((int) innerUnits.stream().filter(row -> "REVIEWED".equals(row.getUnitStatus()) || "BOXED".equals(row.getUnitStatus())).count());
        resp.setOuterBoxCount(outerBoxes.size());
        resp.setOuterPieceCount(outerBoxes.stream().mapToInt(row -> intValue(row.getCurrentQty())).sum());
        resp.setReviewedOuterBoxCount((int) outerBoxes.stream().filter(row -> "REVIEWED".equals(row.getBoxStatus()) || "WAIT_INBOUND".equals(row.getBoxStatus()) || "INBOUNDED".equals(row.getBoxStatus())).count());
        return resp;
    }

    @Override
    public List<HcPackagingSourceRespVO> getSourceList(Long planId, Long planOperationId) {
        if (planId == null || planOperationId == null) {
            return Collections.emptyList();
        }
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(planId);
        validateOperation(planOrder, planOperationId);
        List<HcPackagingSourceRespVO> sources = hcPackagingSourceMapper.selectConfirmedListByPlanId(planId);
        sources.forEach(source -> source.setQualityStatus(resolvePackagingFqcStatus(source.getProductionBatchNo())));
        return sources;
    }

    @Override
    public HcPackagingSourceRespVO scanSource(String sliceBatchNo) {
        String normalized = StrUtil.trimToEmpty(sliceBatchNo);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("请扫描或输入裁切已确认片号");
        }
        HcPackagingSourceRespVO source = hcPackagingSourceMapper.selectConfirmedByProductionBatchNo(normalized);
        if (source == null) {
            throw invalidParamException("未找到已确认的裁切片号，不能进入包装");
        }
        validatePackagingFqcRelease(source.getProductionBatchNo());
        source.setQualityStatus(resolvePackagingFqcStatus(source.getProductionBatchNo()));
        return source;
    }

    @Override
    public List<HcPackagingInnerUnitRespVO> getInnerUnitList(Long planOperationId) {
        if (planOperationId == null) {
            return Collections.emptyList();
        }
        return hcInnerPackUnitMapper.selectListByPlanOperationId(planOperationId).stream()
                .map(this::buildInnerUnitResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInnerUnit(HcPackagingCreateInnerUnitReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateRunningOperation(planOrder, reqVO.getPlanOperationId());
        int packageSpec = reqVO.getPackageSpec() == null ? 1 : reqVO.getPackageSpec();
        if (packageSpec != 1 && packageSpec != 2) {
            throw invalidParamException("当前内包装规格仅支持 1片/包 或 2片/包");
        }
        LocalDateTime now = LocalDateTime.now();
        HcInnerPackUnitDO unit = HcInnerPackUnitDO.builder()
                .tenantId(planOrder.getTenantId())
                .innerUnitNo(nextNo("IP"))
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .packageSpec(packageSpec)
                .targetQty(packageSpec)
                .currentQty(0)
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .modelCode(firstNotBlank(planOrder.getModelCode(), planOrder.getModelName()))
                .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                .productSize(firstNotBlank(planOrder.getSizeName(), planOrder.getSizeSpec()))
                .packageDate(LocalDate.now())
                .unitStatus("WAITING_PIECE")
                .printCount(0)
                .backfillFlag(Boolean.TRUE.equals(reqVO.getBackfillFlag()))
                .backfillReason(reqVO.getBackfillReason())
                .actualWorkTime(reqVO.getActualWorkTime())
                .recorderName(firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(now)
                .remark(reqVO.getRemark())
                .build();
        hcInnerPackUnitMapper.insert(unit);
        upsertPackReport(planOrder, operation, "RUNNING", unit.getRecorderName(), now, null, null);
        return unit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addInnerItem(HcPackagingAddInnerItemReqVO reqVO) {
        HcInnerPackUnitDO unit = getInnerUnit(reqVO.getInnerUnitId());
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(unit.getPlanId());
        HcPlanOrderOperationDO operation = validateRunningOperation(planOrder, unit.getPlanOperationId());
        if (!List.of("WAITING_PIECE", "DRAFT", "PACKED").contains(StrUtil.blankToDefault(unit.getUnitStatus(), ""))) {
            throw invalidParamException("当前内包装单元状态不允许继续扫描片号");
        }
        if (intValue(unit.getCurrentQty()) >= intValue(unit.getTargetQty())) {
            throw invalidParamException("当前内包装单元已满，不能继续加入片号");
        }
        HcPackagingSourceRespVO source = hcPackagingSourceMapper.selectConfirmedByProductionBatchNo(StrUtil.trimToEmpty(reqVO.getSliceBatchNo()));
        if (source == null) {
            throw invalidParamException("未找到已确认的裁切片号");
        }
        if (!Objects.equals(source.getPlanId(), unit.getPlanId())) {
            throw invalidParamException("裁切片号不属于当前包装计划");
        }
        if (hcInnerPackUnitItemMapper.selectBySliceBatchNo(source.getProductionBatchNo()) != null) {
            throw invalidParamException("该裁切片号已完成内包装，不能重复扫描");
        }
        validatePackagingFqcRelease(source.getProductionBatchNo());
        String selfCheck = StrUtil.blankToDefault(source.getQualityStatus(), "OK");
        if (!"OK".equalsIgnoreCase(selfCheck)) {
            throw invalidParamException("该裁切片号质量状态不是OK，不能包装");
        }
        LocalDateTime now = LocalDateTime.now();
        HcPlanOrderInventoryLockDO sourceLock = ensureSourceWipLockedForOperation(planOrder, operation,
                SOURCE_TYPE_CUT_ROUND, SOURCE_TABLE_CUT_ROUND_REPORT, source.getSourceCutRoundReportId(),
                BigDecimal.ONE, firstNotBlank(reqVO.getScanUserName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                "内包扫描自动锁定裁切中间品");
        HcInvStockDO consumedStock = consumeSourceWipLock(sourceLock, BigDecimal.ONE,
                SOURCE_TYPE_PACKAGING, null, source.getProductionBatchNo(), now,
                firstNotBlank(reqVO.getScanUserName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                "内包扫描消耗裁切中间品；来源片号：" + firstNotBlank(source.getProductionBatchNo(), "-"));
        HcInnerPackUnitItemDO item = HcInnerPackUnitItemDO.builder()
                .tenantId(unit.getTenantId())
                .innerUnitId(unit.getId())
                .innerUnitNo(unit.getInnerUnitNo())
                .planId(unit.getPlanId())
                .planNo(unit.getPlanNo())
                .planOperationId(unit.getPlanOperationId())
                .sourceCutRoundReportId(source.getSourceCutRoundReportId())
                .sourceStockId(sourceLock == null ? null : sourceLock.getStockId())
                .sourcePlanLockId(sourceLock == null ? null : sourceLock.getId())
                .sourceConsumeQty(consumedStock == null ? null : BigDecimal.ONE)
                .sourceConsumeTxnNo(consumedStock == null ? null : consumedStock.getLastTxnNo())
                .sourceConsumeTime(consumedStock == null ? null : consumedStock.getLastTxnTime())
                .sliceBatchNo(source.getProductionBatchNo())
                .productionBatchNo(source.getProductionBatchNo())
                .qualityStatus("OK")
                .scanUserName(firstNotBlank(reqVO.getScanUserName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .scanTime(now)
                .build();
        hcInnerPackUnitItemMapper.insert(item);
        refreshInnerUnitQty(unit.getId());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmInnerUnit(HcPackagingUnitActionReqVO reqVO) {
        HcInnerPackUnitDO unit = getInnerUnit(reqVO.getId());
        if (intValue(unit.getCurrentQty()) < intValue(unit.getTargetQty())) {
            throw invalidParamException("当前内包装单元未满，不能确认正式包装");
        }
        updateInnerStatus(unit, "PACKED", null, null);
        return unit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long printInnerUnit(HcPackagingUnitActionReqVO reqVO) {
        HcInnerPackUnitDO unit = getInnerUnit(reqVO.getId());
        if (!List.of("PACKED", "PRINTED", "REVIEWED", "BOXED").contains(StrUtil.blankToDefault(unit.getUnitStatus(), ""))) {
            throw invalidParamException("当前内包装单元未确认完成，不能打印标签");
        }
        LocalDateTime now = LocalDateTime.now();
        String labelNo = firstNotBlank(unit.getLabelNo(), "LBL-" + unit.getInnerUnitNo());
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(unit.getId());
        update.setLabelNo(labelNo);
        update.setPrintCount(intValue(unit.getPrintCount()) + 1);
        update.setLastPrintTime(now);
        if (!"REVIEWED".equals(unit.getUnitStatus()) && !"BOXED".equals(unit.getUnitStatus())) {
            update.setUnitStatus("PRINTED");
        }
        hcInnerPackUnitMapper.updateById(update);
        savePrintLog(LABEL_TYPE_INNER, unit.getId(), unit.getInnerUnitNo(), labelNo,
                intValue(unit.getPrintCount()) + 1, reqVO.getReason(), unit.getPlanId(), unit.getPlanNo(), unit.getPlanOperationId());
        return unit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviewInnerUnit(HcPackagingUnitActionReqVO reqVO) {
        HcInnerPackUnitDO unit = getInnerUnit(reqVO.getId());
        if (StrUtil.isNotBlank(reqVO.getScanNo()) && !reqVO.getScanNo().equals(unit.getInnerUnitNo())
                && !reqVO.getScanNo().equals(unit.getLabelNo())) {
            throw invalidParamException("扫码复核结果与内包装单元不一致");
        }
        if (!List.of("PRINTED", "REVIEWED", "BOXED").contains(StrUtil.blankToDefault(unit.getUnitStatus(), ""))) {
            throw invalidParamException("请先打印内包装标签后再复核");
        }
        updateInnerStatus(unit, "REVIEWED", firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"), LocalDateTime.now());
        return unit.getId();
    }

    @Override
    public List<HcPackagingOuterBoxRespVO> getOuterBoxList(Long planOperationId) {
        if (planOperationId == null) {
            return Collections.emptyList();
        }
        return hcOuterPackBoxMapper.selectListByPlanOperationId(planOperationId).stream()
                .map(this::buildOuterBoxResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOuterBox(HcPackagingCreateOuterBoxReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateRunningOperation(planOrder, reqVO.getPlanOperationId());
        LocalDateTime now = LocalDateTime.now();
        HcOuterPackBoxDO box = HcOuterPackBoxDO.builder()
                .tenantId(planOrder.getTenantId())
                .outerBoxNo(nextNo("OP"))
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .packMethod(firstNotBlank(reqVO.getPackMethod(), "BOX"))
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .modelCode(firstNotBlank(planOrder.getModelCode(), planOrder.getModelName()))
                .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                .productSize(firstNotBlank(planOrder.getSizeName(), planOrder.getSizeSpec()))
                .standardQty(reqVO.getStandardQty() == null ? 10 : reqVO.getStandardQty())
                .currentQty(0)
                .tailBoxFlag(Boolean.TRUE.equals(reqVO.getTailBoxFlag()))
                .boxStatus("DRAFT")
                .printCount(0)
                .recorderName(firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(now)
                .remark(reqVO.getRemark())
                .build();
        hcOuterPackBoxMapper.insert(box);
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addOuterUnit(HcPackagingAddOuterUnitReqVO reqVO) {
        HcOuterPackBoxDO box = getOuterBox(reqVO.getOuterBoxId());
        validateRunningOperation(hcPlanOrderMapper.selectById(box.getPlanId()), box.getPlanOperationId());
        if (!List.of("DRAFT", "PACKING", "PACKED").contains(StrUtil.blankToDefault(box.getBoxStatus(), ""))) {
            throw invalidParamException("当前外包装状态不允许继续加入内包装");
        }
        HcInnerPackUnitDO unit = hcInnerPackUnitMapper.selectByUnitNo(StrUtil.trimToEmpty(reqVO.getInnerUnitNo()));
        if (unit == null) {
            throw invalidParamException("内包装单元不存在");
        }
        if (!Objects.equals(unit.getPlanId(), box.getPlanId())) {
            throw invalidParamException("内包装单元不属于当前包装计划");
        }
        if (!"REVIEWED".equals(unit.getUnitStatus())) {
            throw invalidParamException("内包装单元未完成扫码复核，不能装入外包装");
        }
        if (hcOuterPackBoxItemMapper.selectByInnerUnitNo(unit.getInnerUnitNo()) != null) {
            throw invalidParamException("该内包装单元已装入外包装，不能重复扫描");
        }
        int nextQty = intValue(box.getCurrentQty()) + intValue(unit.getCurrentQty());
        if (!Boolean.TRUE.equals(box.getTailBoxFlag()) && nextQty > intValue(box.getStandardQty())) {
            throw invalidParamException("当前外包装数量超过标准箱规，请创建尾数箱或调整箱规");
        }
        LocalDateTime now = LocalDateTime.now();
        List<String> slices = hcInnerPackUnitItemMapper.selectListByInnerUnitId(unit.getId()).stream()
                .map(HcInnerPackUnitItemDO::getSliceBatchNo)
                .toList();
        HcOuterPackBoxItemDO item = HcOuterPackBoxItemDO.builder()
                .tenantId(box.getTenantId())
                .outerBoxId(box.getId())
                .outerBoxNo(box.getOuterBoxNo())
                .innerUnitId(unit.getId())
                .innerUnitNo(unit.getInnerUnitNo())
                .planId(box.getPlanId())
                .planNo(box.getPlanNo())
                .planOperationId(box.getPlanOperationId())
                .innerPackageSpec(unit.getPackageSpec())
                .pieceQty(unit.getCurrentQty())
                .sliceBatchListJson(JsonUtils.toJsonString(slices))
                .scanUserName(firstNotBlank(reqVO.getScanUserName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .scanTime(now)
                .build();
        hcOuterPackBoxItemMapper.insert(item);
        HcInnerPackUnitDO innerUpdate = new HcInnerPackUnitDO();
        innerUpdate.setId(unit.getId());
        innerUpdate.setUnitStatus("BOXED");
        hcInnerPackUnitMapper.updateById(innerUpdate);
        refreshOuterBoxQty(box.getId());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmOuterBox(HcPackagingUnitActionReqVO reqVO) {
        HcOuterPackBoxDO box = getOuterBox(reqVO.getId());
        if (intValue(box.getCurrentQty()) <= 0) {
            throw invalidParamException("当前外包装没有内包装单元，不能确认");
        }
        updateOuterStatus(box, "PACKED", null, null);
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long printOuterBox(HcPackagingUnitActionReqVO reqVO) {
        HcOuterPackBoxDO box = getOuterBox(reqVO.getId());
        if (!List.of("PACKED", "PRINTED", "REVIEWED", "WAIT_INBOUND").contains(StrUtil.blankToDefault(box.getBoxStatus(), ""))) {
            throw invalidParamException("当前外包装未确认完成，不能打印标签");
        }
        LocalDateTime now = LocalDateTime.now();
        String labelNo = firstNotBlank(box.getOuterLabelNo(), "LBL-" + box.getOuterBoxNo());
        HcOuterPackBoxDO update = new HcOuterPackBoxDO();
        update.setId(box.getId());
        update.setOuterLabelNo(labelNo);
        update.setPrintCount(intValue(box.getPrintCount()) + 1);
        update.setLastPrintTime(now);
        if (!"REVIEWED".equals(box.getBoxStatus()) && !"WAIT_INBOUND".equals(box.getBoxStatus())) {
            update.setBoxStatus("PRINTED");
        }
        hcOuterPackBoxMapper.updateById(update);
        savePrintLog(LABEL_TYPE_OUTER, box.getId(), box.getOuterBoxNo(), labelNo,
                intValue(box.getPrintCount()) + 1, reqVO.getReason(), box.getPlanId(), box.getPlanNo(), box.getPlanOperationId());
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviewOuterBox(HcPackagingUnitActionReqVO reqVO) {
        HcOuterPackBoxDO box = getOuterBox(reqVO.getId());
        if (StrUtil.isNotBlank(reqVO.getScanNo()) && !reqVO.getScanNo().equals(box.getOuterBoxNo())
                && !reqVO.getScanNo().equals(box.getOuterLabelNo())) {
            throw invalidParamException("扫码复核结果与外包装箱/板不一致");
        }
        if (!List.of("PRINTED", "REVIEWED", "WAIT_INBOUND").contains(StrUtil.blankToDefault(box.getBoxStatus(), ""))) {
            throw invalidParamException("请先打印外包装标签后再复核");
        }
        updateOuterStatus(box, "REVIEWED", firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"), LocalDateTime.now());
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(HcPackagingSubmitReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateRunningOperation(planOrder, reqVO.getPlanOperationId());
        List<HcInnerPackUnitDO> innerUnits = hcInnerPackUnitMapper.selectListByPlanOperationId(operation.getId());
        boolean hasHalfUnit = innerUnits.stream().anyMatch(row -> intValue(row.getCurrentQty()) > 0
                && intValue(row.getCurrentQty()) < intValue(row.getTargetQty()));
        if (hasHalfUnit) {
            throw invalidParamException("存在未满包的内包装单元，不能工单完工");
        }
        List<HcOuterPackBoxDO> boxes = hcOuterPackBoxMapper.selectListByPlanOperationId(operation.getId());
        List<HcOuterPackBoxDO> reviewedBoxes = boxes.stream()
                .filter(row -> "REVIEWED".equals(row.getBoxStatus()) || "WAIT_INBOUND".equals(row.getBoxStatus()))
                .toList();
        if (reviewedBoxes.isEmpty()) {
            throw invalidParamException("没有已复核外包装，不能工单完工");
        }
        int totalPieces = reviewedBoxes.stream().mapToInt(row -> intValue(row.getCurrentQty())).sum();
        LocalDate reportDate = reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = normalizeReportDateTime(reqVO.getEndTime());
        LocalDateTime recorderTime = normalizeReportDateTime(reqVO.getRecorderTime());
        LocalDateTime confirmerTime = normalizeReportDateTime(reqVO.getConfirmerTime());
        if (endTime == null) {
            endTime = now;
        }
        if (recorderTime == null) {
            recorderTime = now;
        }
        if (confirmerTime == null) {
            confirmerTime = now;
        }
        HcProcessReportDO reportDO = HcProcessReportDO.builder()
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationStatus(operation.getOperationStatus())
                .operationSeq(operation.getOpSeq())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .workCenterId(operation.getWorkCenterId())
                .workCenterCode(operation.getWorkCenterCode())
                .workCenterName(operation.getWorkCenterName())
                .equipmentId(operation.getEquipmentId())
                .equipmentCode(operation.getEquipmentCode())
                .equipmentName(operation.getEquipmentName())
                .materialId(planOrder.getMaterialId())
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                .productionBatchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo()))
                .parentProductionBatchNo(firstNotBlank(operation.getParentProductionBatchNo(), planOrder.getParentProductionBatchNo(), planOrder.getProductionBatchNo()))
                .reportDate(reportDate)
                .endTime(endTime)
                .goodQty(BigDecimal.valueOf(totalPieces))
                .scrapQty(BigDecimal.ZERO)
                .recorderName(firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(recorderTime)
                .confirmerName(firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .confirmerTime(confirmerTime)
                .reportUom(firstNotBlank(operation.getUom(), operation.getUnitCode(), operation.getUnitName(), "片"))
                .reportType(REPORT_TYPE_END)
                .sourceMenuCode(SOURCE_MENU_CODE)
                .remark(reqVO.getRemark())
                .extraJson(JsonUtils.toJsonString(Map.of("outerBoxCount", reviewedBoxes.size(), "outerPieceCount", totalPieces)))
                .build();
        hcProcessReportMapper.insert(reportDO);
        HcPlanOrderOperationDO updateObj = new HcPlanOrderOperationDO();
        updateObj.setId(operation.getId());
        updateObj.setOperationStatus(OP_STATUS_FINISHED);
        updateObj.setFinishTime(reportDO.getEndTime());
        updateObj.setFinishRemark(reqVO.getRemark());
        updateObj.setStatusOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setStatusOperatorName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "system"));
        updateObj.setStatusOperateTime(now);
        hcPlanOrderOperationMapper.updateById(updateObj);
        if (operation.getEquipmentId() != null) {
            hcEquipmentService.releaseEquipment(operation.getEquipmentId(), reportDO.getEndTime(),
                    reportDO.getConfirmerName(), reportDO.getConfirmerTime());
        }
        for (HcOuterPackBoxDO box : reviewedBoxes) {
            HcOuterPackBoxDO update = new HcOuterPackBoxDO();
            update.setId(box.getId());
            update.setBoxStatus("WAIT_INBOUND");
            hcOuterPackBoxMapper.updateById(update);
        }
        upsertPackReport(planOrder, operation, "FINISHED", reportDO.getRecorderName(), reportDO.getRecorderTime(),
                reportDO.getConfirmerName(), reportDO.getConfirmerTime());
        return reportDO.getId();
    }

    private Long upsertPassWork(HcPackagingPassWorkSaveReqVO reqVO, boolean confirm) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        HcStationFormDO form = hcStationFormMapper.selectEnabledByCode(reqVO.getFormCode());
        if (form == null || !PROCESS_CODE.equals(form.getProcessCode())) {
            throw invalidParamException("包装工作准备表单不存在或未启用");
        }
        LocalDate recordDate = reqVO.getRecordDate() == null ? LocalDate.now() : reqVO.getRecordDate();
        Long equipmentId = reqVO.getEquipmentId() == null ? operation.getEquipmentId() : reqVO.getEquipmentId();
        HcStationRecordDO record = reqVO.getRecordId() == null ? null : hcStationRecordMapper.selectById(reqVO.getRecordId());
        if (record == null && equipmentId != null) {
            record = hcStationRecordMapper.selectOneByEquipmentDaily(equipmentId, recordDate, form.getFormCode());
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recorderTime = normalizeReportDateTime(reqVO.getRecorderTime());
        LocalDateTime confirmerTime = normalizeReportDateTime(reqVO.getConfirmerTime());
        if (recorderTime == null) {
            recorderTime = now;
        }
        if (confirm && confirmerTime == null) {
            confirmerTime = now;
        }
        HcStationRecordDO save = HcStationRecordDO.builder()
                .id(record == null ? null : record.getId())
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .formId(form.getId())
                .formCode(form.getFormCode())
                .formName(form.getFormName())
                .triggerTimingCode(form.getTriggerTimingCode())
                .triggerTimingName(form.getTriggerTimingName())
                .docStatus(confirm ? "CONFIRMED" : "FILLED")
                .resultStatus(firstNotBlank(reqVO.getResult(), reqVO.getInspectionResult(), "OK"))
                .inspectionResult(reqVO.getInspectionResult())
                .equipmentId(equipmentId)
                .equipmentCode(firstNotBlank(reqVO.getEquipmentCode(), operation.getEquipmentCode()))
                .equipmentName(firstNotBlank(reqVO.getEquipmentName(), operation.getEquipmentName()))
                .workCenterId(operation.getWorkCenterId())
                .workCenterCode(operation.getWorkCenterCode())
                .workCenterName(operation.getWorkCenterName())
                .recordUserName(firstNotBlank(reqVO.getRecorder(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recordTime(recorderTime)
                .confirmUserName(confirm ? firstNotBlank(reqVO.getConfirmer(), SecurityFrameworkUtils.getLoginUserNickname(), "系统") : reqVO.getConfirmer())
                .confirmTime(confirm ? confirmerTime : normalizeReportDateTime(reqVO.getConfirmerTime()))
                .headerDataJson(reqVO.getHeaderDataJson())
                .formRemark(reqVO.getFormRemark())
                .confirmRemark(reqVO.getConfirmRemark())
                .recordScope("EQUIPMENT_DAILY")
                .recordDate(recordDate)
                .build();
        if (record == null) {
            hcStationRecordMapper.insert(save);
        } else {
            hcStationRecordMapper.updateById(save);
        }
        hcStationRecordItemMapper.deleteByRecordId(save.getId());
        if (reqVO.getDetails() != null) {
            for (HcPackagingPassWorkItemReqVO item : reqVO.getDetails()) {
                hcStationRecordItemMapper.insert(HcStationRecordItemDO.builder()
                        .tenantId(planOrder.getTenantId())
                        .recordId(save.getId())
                        .itemSeq(item.getItemSeq())
                        .itemCategory(item.getCategory())
                        .stepNode(item.getNode())
                        .itemName(item.getItem())
                        .standardText(item.getStandard())
                        .valueMode(item.getValueMode())
                        .dualLabel1(item.getDualLabel1())
                        .dualLabel2(item.getDualLabel2())
                        .actualValue(item.getActualValue())
                        .actualValue2(item.getActualValue2())
                        .resultFlag(firstNotBlank(item.getStatus(), "OK"))
                        .abnormalRemark(item.getRemark())
                        .build());
            }
        }
        return save.getId();
    }

    private HcPackagingPassWorkRespVO buildPassWorkResp(HcStationFormDO form, HcStationRecordDO record) {
        HcPackagingPassWorkRespVO resp = new HcPackagingPassWorkRespVO();
        resp.setRecordId(record == null ? null : record.getId());
        resp.setFormId(form.getId());
        resp.setFormCode(form.getFormCode());
        resp.setId(form.getFormCode());
        resp.setName(form.getFormName());
        resp.setTiming(form.getTriggerTimingName());
        resp.setStatus(record == null ? "未填写" : record.getDocStatus());
        resp.setResult(record == null ? null : record.getResultStatus());
        resp.setInspectionResult(record == null ? null : record.getInspectionResult());
        resp.setCanFill(true);
        resp.setCanConfirm(record != null);
        resp.setCanView(record != null);
        resp.setRecorder(record == null ? null : record.getRecordUserName());
        resp.setRecorderTime(record == null || record.getRecordTime() == null ? null : record.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resp.setConfirmer(record == null ? null : record.getConfirmUserName());
        resp.setConfirmerTime(record == null || record.getConfirmTime() == null ? null : record.getConfirmTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resp.setPresetHeaderDataJson(form.getPresetHeaderDataJson());
        resp.setHeaderDataJson(record == null ? form.getPresetHeaderDataJson() : firstNotBlank(record.getHeaderDataJson(), form.getPresetHeaderDataJson()));
        resp.setSchemaJson(form.getSchemaJson());
        resp.setPresetDetails(hcStationFormItemMapper.selectByFormId(form.getId()).stream().map(this::buildPresetItem).toList());
        resp.setDetails(record == null ? resp.getPresetDetails()
                : hcStationRecordItemMapper.selectByRecordIds(List.of(record.getId())).stream().map(this::buildRecordItem).toList());
        return resp;
    }

    private HcPackagingPassWorkItemRespVO buildPresetItem(HcStationFormItemDO item) {
        HcPackagingPassWorkItemRespVO resp = new HcPackagingPassWorkItemRespVO();
        resp.setItemSeq(item.getItemSeq());
        resp.setCategory(item.getItemCategory());
        resp.setNode(item.getStepNode());
        resp.setItem(item.getItemName());
        resp.setStandard(item.getStandardText());
        resp.setValueMode(item.getValueMode());
        resp.setDualLabel1(item.getDualLabel1());
        resp.setDualLabel2(item.getDualLabel2());
        resp.setStatus(firstNotBlank(item.getDefaultResult(), "OK"));
        return resp;
    }

    private HcPackagingPassWorkItemRespVO buildRecordItem(HcStationRecordItemDO item) {
        HcPackagingPassWorkItemRespVO resp = new HcPackagingPassWorkItemRespVO();
        resp.setItemSeq(item.getItemSeq());
        resp.setCategory(item.getItemCategory());
        resp.setNode(item.getStepNode());
        resp.setItem(item.getItemName());
        resp.setStandard(item.getStandardText());
        resp.setValueMode(item.getValueMode());
        resp.setDualLabel1(item.getDualLabel1());
        resp.setDualLabel2(item.getDualLabel2());
        resp.setActualValue(item.getActualValue());
        resp.setActualValue2(item.getActualValue2());
        resp.setStatus(item.getResultFlag());
        resp.setRemark(item.getAbnormalRemark());
        return resp;
    }

    private HcPackagingInnerUnitRespVO buildInnerUnitResp(HcInnerPackUnitDO unit) {
        HcPackagingInnerUnitRespVO resp = new HcPackagingInnerUnitRespVO();
        resp.setId(unit.getId());
        resp.setInnerUnitNo(unit.getInnerUnitNo());
        resp.setPlanId(unit.getPlanId());
        resp.setPlanNo(unit.getPlanNo());
        resp.setPlanOperationId(unit.getPlanOperationId());
        resp.setPackageSpec(unit.getPackageSpec());
        resp.setTargetQty(unit.getTargetQty());
        resp.setCurrentQty(unit.getCurrentQty());
        resp.setMaterialCode(unit.getMaterialCode());
        resp.setMaterialName(unit.getMaterialName());
        resp.setModelCode(unit.getModelCode());
        resp.setBatchNo(unit.getBatchNo());
        resp.setProductSize(unit.getProductSize());
        resp.setPackageDate(unit.getPackageDate());
        resp.setLabelNo(unit.getLabelNo());
        resp.setUnitStatus(unit.getUnitStatus());
        resp.setPrintCount(unit.getPrintCount());
        resp.setLastPrintTime(unit.getLastPrintTime());
        resp.setReviewerName(unit.getReviewerName());
        resp.setReviewTime(unit.getReviewTime());
        resp.setBackfillFlag(unit.getBackfillFlag());
        resp.setBackfillReason(unit.getBackfillReason());
        resp.setRecorderName(unit.getRecorderName());
        resp.setRecorderTime(unit.getRecorderTime());
        resp.setRemark(unit.getRemark());
        resp.setItems(hcInnerPackUnitItemMapper.selectListByInnerUnitId(unit.getId()).stream()
                .map(this::buildInnerItemResp)
                .toList());
        return resp;
    }

    private HcPackagingInnerUnitItemRespVO buildInnerItemResp(HcInnerPackUnitItemDO item) {
        HcPackagingInnerUnitItemRespVO resp = new HcPackagingInnerUnitItemRespVO();
        resp.setId(item.getId());
        resp.setInnerUnitId(item.getInnerUnitId());
        resp.setInnerUnitNo(item.getInnerUnitNo());
        resp.setSourceCutRoundReportId(item.getSourceCutRoundReportId());
        resp.setSliceBatchNo(item.getSliceBatchNo());
        resp.setProductionBatchNo(item.getProductionBatchNo());
        resp.setQualityStatus(item.getQualityStatus());
        resp.setScanUserName(item.getScanUserName());
        resp.setScanTime(item.getScanTime());
        return resp;
    }

    private HcPackagingOuterBoxRespVO buildOuterBoxResp(HcOuterPackBoxDO box) {
        HcPackagingOuterBoxRespVO resp = new HcPackagingOuterBoxRespVO();
        resp.setId(box.getId());
        resp.setOuterBoxNo(box.getOuterBoxNo());
        resp.setPlanId(box.getPlanId());
        resp.setPlanNo(box.getPlanNo());
        resp.setPlanOperationId(box.getPlanOperationId());
        resp.setPackMethod(box.getPackMethod());
        resp.setMaterialCode(box.getMaterialCode());
        resp.setMaterialName(box.getMaterialName());
        resp.setModelCode(box.getModelCode());
        resp.setBatchNo(box.getBatchNo());
        resp.setProductSize(box.getProductSize());
        resp.setStandardQty(box.getStandardQty());
        resp.setCurrentQty(box.getCurrentQty());
        resp.setTailBoxFlag(box.getTailBoxFlag());
        resp.setOuterLabelNo(box.getOuterLabelNo());
        resp.setBoxStatus(box.getBoxStatus());
        resp.setPrintCount(box.getPrintCount());
        resp.setLastPrintTime(box.getLastPrintTime());
        resp.setReviewerName(box.getReviewerName());
        resp.setReviewTime(box.getReviewTime());
        resp.setRecorderName(box.getRecorderName());
        resp.setRecorderTime(box.getRecorderTime());
        resp.setRemark(box.getRemark());
        resp.setItems(hcOuterPackBoxItemMapper.selectListByOuterBoxId(box.getId()).stream()
                .map(this::buildOuterItemResp)
                .toList());
        return resp;
    }

    private HcPackagingOuterBoxItemRespVO buildOuterItemResp(HcOuterPackBoxItemDO item) {
        HcPackagingOuterBoxItemRespVO resp = new HcPackagingOuterBoxItemRespVO();
        resp.setId(item.getId());
        resp.setOuterBoxId(item.getOuterBoxId());
        resp.setOuterBoxNo(item.getOuterBoxNo());
        resp.setInnerUnitId(item.getInnerUnitId());
        resp.setInnerUnitNo(item.getInnerUnitNo());
        resp.setInnerPackageSpec(item.getInnerPackageSpec());
        resp.setPieceQty(item.getPieceQty());
        resp.setSliceBatchListJson(item.getSliceBatchListJson());
        resp.setScanUserName(item.getScanUserName());
        resp.setScanTime(item.getScanTime());
        return resp;
    }

    private HcPlanOrderOperationDO validateOperation(HcPlanOrderDO planOrder, Long planOperationId) {
        if (planOrder == null) {
            throw invalidParamException("生产计划不存在");
        }
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(planOperationId);
        if (operation == null || !Objects.equals(operation.getPlanId(), planOrder.getId())) {
            throw invalidParamException("计划工序不存在");
        }
        String opCode = StrUtil.trimToEmpty(operation.getOpCode());
        String opName = StrUtil.trimToEmpty(operation.getOpName());
        if (!"OP-PACKAGING".equalsIgnoreCase(opCode) && !"OP-PACK".equalsIgnoreCase(opCode)
                && !opName.contains("包装") && !opName.contains("内包") && !opName.contains("外包")) {
            throw invalidParamException("当前仅允许包装工序使用该功能");
        }
        return operation;
    }

    private HcPlanOrderOperationDO validateRunningOperation(HcPlanOrderDO planOrder, Long planOperationId) {
        HcPlanOrderOperationDO operation = validateOperation(planOrder, planOperationId);
        if (!OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            throw invalidParamException(OP_STATUS_FINISHED.equals(operation.getOperationStatus())
                    ? "当前工单此工序已完工，请勿再包装报工！" : "当前工单此工序未开工，请先执行开工确认");
        }
        return operation;
    }

    private HcInnerPackUnitDO getInnerUnit(Long id) {
        HcInnerPackUnitDO unit = hcInnerPackUnitMapper.selectById(id);
        if (unit == null || Boolean.TRUE.equals(unit.getDeleted())) {
            throw invalidParamException("内包装单元不存在");
        }
        return unit;
    }

    private HcOuterPackBoxDO getOuterBox(Long id) {
        HcOuterPackBoxDO box = hcOuterPackBoxMapper.selectById(id);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("外包装箱/板不存在");
        }
        return box;
    }

    private void refreshInnerUnitQty(Long innerUnitId) {
        HcInnerPackUnitDO unit = getInnerUnit(innerUnitId);
        int qty = hcInnerPackUnitItemMapper.selectListByInnerUnitId(innerUnitId).size();
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(innerUnitId);
        update.setCurrentQty(qty);
        update.setUnitStatus(qty >= intValue(unit.getTargetQty()) ? "PACKED" : "WAITING_PIECE");
        hcInnerPackUnitMapper.updateById(update);
    }

    private void refreshOuterBoxQty(Long outerBoxId) {
        HcOuterPackBoxDO box = getOuterBox(outerBoxId);
        int qty = hcOuterPackBoxItemMapper.selectListByOuterBoxId(outerBoxId).stream()
                .mapToInt(row -> intValue(row.getPieceQty()))
                .sum();
        HcOuterPackBoxDO update = new HcOuterPackBoxDO();
        update.setId(outerBoxId);
        update.setCurrentQty(qty);
        update.setBoxStatus(qty >= intValue(box.getStandardQty()) ? "PACKED" : "PACKING");
        hcOuterPackBoxMapper.updateById(update);
    }

    private void updateInnerStatus(HcInnerPackUnitDO unit, String status, String reviewer, LocalDateTime reviewTime) {
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(unit.getId());
        update.setUnitStatus(status);
        if (reviewer != null) {
            update.setReviewerName(reviewer);
            update.setReviewTime(reviewTime);
        }
        hcInnerPackUnitMapper.updateById(update);
    }

    private void updateOuterStatus(HcOuterPackBoxDO box, String status, String reviewer, LocalDateTime reviewTime) {
        HcOuterPackBoxDO update = new HcOuterPackBoxDO();
        update.setId(box.getId());
        update.setBoxStatus(status);
        if (reviewer != null) {
            update.setReviewerName(reviewer);
            update.setReviewTime(reviewTime);
        }
        hcOuterPackBoxMapper.updateById(update);
    }

    private void upsertPackReport(HcPlanOrderDO planOrder, HcPlanOrderOperationDO operation, String reportStatus,
                                  String recorderName, LocalDateTime recorderTime,
                                  String confirmerName, LocalDateTime confirmerTime) {
        HcPackagingSummaryRespVO summary = getSummary(planOrder.getId(), operation.getId());
        HcPackReportDO existed = hcPackReportMapper.selectLatestByPlanOperationId(operation.getId());
        HcPackReportDO save = HcPackReportDO.builder()
                .id(existed == null ? null : existed.getId())
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .modelCode(firstNotBlank(planOrder.getModelCode(), planOrder.getModelName()))
                .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                .productionBatchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo()))
                .packageDate(LocalDate.now())
                .innerUnitCount(summary.getInnerUnitCount())
                .innerPieceCount(summary.getInnerPieceCount())
                .outerBoxCount(summary.getOuterBoxCount())
                .outerPieceCount(summary.getOuterPieceCount())
                .inboundPieceCount(0)
                .reportStatus(reportStatus)
                .recorderName(recorderName)
                .recorderTime(recorderTime)
                .confirmerName(confirmerName)
                .confirmerTime(confirmerTime)
                .build();
        if (existed == null) {
            hcPackReportMapper.insert(save);
        } else {
            hcPackReportMapper.updateById(save);
        }
    }

    private void savePrintLog(String labelType, Long bizId, String bizNo, String labelNo, int printCount,
                              String reason, Long planId, String planNo, Long planOperationId) {
        hcLabelPrintLogMapper.insert(HcLabelPrintLogDO.builder()
                .labelType(labelType)
                .labelNo(labelNo)
                .bizId(bizId)
                .bizNo(bizNo)
                .planId(planId)
                .planNo(planNo)
                .planOperationId(planOperationId)
                .printCount(printCount)
                .printReason(reason)
                .printerName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .printTime(LocalDateTime.now())
                .labelContentJson(JsonUtils.toJsonString(Map.of("labelType", labelType, "labelNo", labelNo, "bizNo", bizNo)))
                .build());
    }

    private String nextNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private boolean isActivePlanWipLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null || Boolean.TRUE.equals(lock.getDeleted())) {
            return false;
        }
        boolean wipType = PLAN_LOCK_TYPE_WIP.equalsIgnoreCase(lock.getLockType())
                || PLAN_LOCK_TYPE_WIP.equalsIgnoreCase(lock.getStockType());
        if (!wipType) {
            return false;
        }
        if (PLAN_LOCK_STATUS_CANCELLED.equalsIgnoreCase(lock.getLockStatus())
                || PLAN_LOCK_STATUS_CONSUMED.equalsIgnoreCase(lock.getLockStatus())
                || PLAN_LOCK_STATUS_RELEASED.equalsIgnoreCase(lock.getLockStatus())) {
            return false;
        }
        return calculateWipLockRemainingQty(lock).compareTo(BigDecimal.ZERO) > 0;
    }

    private HcPlanOrderInventoryLockDO resolveActiveSourceWipLock(Long planOperationId,
                                                                  String sourceTable,
                                                                  Long sourceId) {
        if (planOperationId == null || StrUtil.isBlank(sourceTable) || sourceId == null) {
            return null;
        }
        return hcPlanOrderInventoryLockMapper.selectListByPlanOperationId(planOperationId).stream()
                .filter(this::isActivePlanWipLock)
                .filter(lock -> StrUtil.equalsIgnoreCase(sourceTable, lock.getSourceTable()))
                .filter(lock -> Objects.equals(sourceId, lock.getSourceId()))
                .findFirst()
                .orElse(null);
    }

    private HcPlanOrderInventoryLockDO ensureSourceWipLockedForOperation(HcPlanOrderDO planOrder,
                                                                         HcPlanOrderOperationDO operation,
                                                                         String sourceType,
                                                                         String sourceTable,
                                                                         Long sourceId,
                                                                         BigDecimal lockQty,
                                                                         String operatorName,
                                                                         String remark) {
        if (planOrder == null || operation == null || StrUtil.isBlank(sourceType)
                || StrUtil.isBlank(sourceTable) || sourceId == null) {
            return null;
        }
        BigDecimal normalizedLockQty = zeroIfNull(lockQty);
        if (normalizedLockQty.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        HcPlanOrderInventoryLockDO existed = resolveActiveSourceWipLock(operation.getId(), sourceTable, sourceId);
        if (existed != null) {
            return existed;
        }
        HcInvStockDO stock = hcInvStockMapper.selectOneBySource(sourceType, sourceTable, sourceId);
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            return null;
        }
        HcPlanOrderInventoryLockDO lock = HcPlanOrderInventoryLockDO.builder()
                .tenantId(stock.getTenantId() == null ? planOrder.getTenantId() : stock.getTenantId())
                .planId(planOrder.getId())
                .targetPlanNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .targetOpCode(operation.getOpCode())
                .targetOpName(operation.getOpName())
                .lockType(PLAN_LOCK_TYPE_WIP)
                .stockId(stock.getId())
                .stockType(stock.getStockType())
                .sourceType(firstNotBlank(stock.getSourceType(), sourceType))
                .sourceTable(firstNotBlank(stock.getSourceTable(), sourceTable))
                .sourceId(stock.getSourceId() == null ? sourceId : stock.getSourceId())
                .sourcePlanId(stock.getSourcePlanId())
                .sourcePlanNo(stock.getSourcePlanNo())
                .sourcePlanOperationId(stock.getSourcePlanOperationId())
                .sourceBatchNo(firstNotBlank(stock.getSourceBatchNo(), stock.getBatchNo()))
                .opSeq(stock.getOpSeq())
                .opCode(stock.getOpCode())
                .opName(stock.getOpName())
                .segmentCode(stock.getSegmentCode())
                .segmentName(stock.getSegmentName())
                .lotNo(stock.getBatchNo())
                .batchNo(stock.getBatchNo())
                .materialId(stock.getMaterialId())
                .materialCode(stock.getMaterialCode())
                .materialName(stock.getMaterialName())
                .modelNo(stock.getModelNo())
                .recipeCode(stock.getRecipeCode())
                .sizeSpec(stock.getSpecSize())
                .productionDate(stock.getProductionDate())
                .locationCode(stock.getLocationCode())
                .locationName(stock.getLocationName())
                .availableQty(zeroIfNull(stock.getAvailableQty()))
                .lockQty(normalizedLockQty)
                .consumedQty(BigDecimal.ZERO)
                .releasedQty(BigDecimal.ZERO)
                .remainingQty(normalizedLockQty)
                .uom(firstNotBlank(stock.getUom(), operation.getUom(), operation.getUnitCode(), operation.getUnitName(), "片"))
                .lockStatus(PLAN_LOCK_STATUS_ACTIVE)
                .remark(firstNotBlank(remark, "工序入站自动锁定中间品"))
                .build();
        hcPlanOrderInventoryLockMapper.insert(lock);
        HcInvStockDO lockedStock = hcInvStockService.lockPlanWip(lock, normalizedLockQty, LocalDateTime.now(),
                SecurityFrameworkUtils.getLoginUserId(),
                firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                firstNotBlank(remark, "工序入站自动锁定中间品") + "；目标计划：" + firstNotBlank(planOrder.getPlanNo(), "-")
                        + "；目标工序：" + firstNotBlank(operation.getOpName(), operation.getOpCode(), "-"));
        lock.setLockTxnNo(lockedStock.getLastTxnNo());
        lock.setLockStatus(PLAN_LOCK_STATUS_ACTIVE);
        return lock;
    }

    private HcInvStockDO consumeSourceWipLock(HcPlanOrderInventoryLockDO sourceLock,
                                              BigDecimal consumeQty,
                                              String refDocType,
                                              Long refDocId,
                                              String refDocNo,
                                              LocalDateTime consumeTime,
                                              String operatorName,
                                              String remark) {
        if (sourceLock == null) {
            return null;
        }
        return hcInvStockService.consumePlanLockedWip(
                sourceLock,
                zeroIfNull(consumeQty),
                refDocType,
                refDocId,
                refDocNo,
                consumeTime,
                SecurityFrameworkUtils.getLoginUserId(),
                firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                firstNotBlank(remark, "工序确认消耗计划锁定中间品"));
    }

    private BigDecimal calculateWipLockRemainingQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal remainingQty = zeroIfNull(lock.getLockQty())
                .subtract(zeroIfNull(lock.getConsumedQty()))
                .subtract(zeroIfNull(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingQty;
    }

    private String resolvePackagingFqcStatus(String productionBatchNo) {
        QmsFqcSubmissionDetailDO detail = qmsCutRoundFqcService.getPackagingSubmissionDetail(productionBatchNo);
        return detail == null || StrUtil.isBlank(detail.getRowJudgment()) ? "PENDING" : detail.getRowJudgment();
    }

    private void validatePackagingFqcRelease(String productionBatchNo) {
        QmsFqcSubmissionDetailDO detail = qmsCutRoundFqcService.getPackagingSubmissionDetail(productionBatchNo);
        if (detail == null || StrUtil.isBlank(detail.getRowJudgment())) {
            throw invalidParamException("该裁切片号未完成裁切成品检验，不能包装");
        }
        if (!"OK".equalsIgnoreCase(detail.getRowJudgment())) {
            throw invalidParamException("该裁切片号裁切成品检验结果不是OK，不能包装");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime normalizeReportDateTime(LocalDateTime value) {
        if (value == null || value.getYear() < 2000) {
            return null;
        }
        return value;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
