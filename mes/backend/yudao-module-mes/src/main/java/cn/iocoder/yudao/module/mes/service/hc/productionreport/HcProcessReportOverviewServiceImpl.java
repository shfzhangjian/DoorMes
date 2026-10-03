package cn.iocoder.yudao.module.mes.service.hc.productionreport;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import java.time.format.DateTimeFormatter;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionreport.vo.HcProcessReportOverviewDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slittingpress.HcSlittingPressProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.wetproductionrecord.HcWetProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSecondDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundProductionRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slittingpress.HcSlittingPressProductionLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.wetproductionrecord.HcWetProductionRecordMapper;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcAdhesiveProductionRecordService;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcCutRoundConsoleService;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcFormulaProductionRecordService;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcGrindingProductionRecordLedgerService;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcSlittingPressProductionRecordService;
import cn.iocoder.yudao.module.mes.service.hc.wetproductionrecord.HcWetProductionRecordService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPLANORDER_NOT_EXISTS;

/**
 * 工序报工综合报表只读聚合服务。
 *
 * <p>仅使用普通 SELECT；不调用任何会初始化中间品、同步 FAI 或补建报工的接口。</p>
 */
@Service
public class HcProcessReportOverviewServiceImpl implements HcProcessReportOverviewService {

    @Resource
    private HcPlanOrderMapper planOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper operationMapper;
    @Resource
    private HcProcessReportMapper processReportMapper;
    @Resource
    private HcGrindingReportMapper grindingReportMapper;
    @Resource
    private HcAdhesiveReportMapper adhesiveReportMapper;
    @Resource
    private HcAdhesive2ReportMapper adhesive2ReportMapper;
    @Resource
    private HcPressSlotReportMapper pressSlotReportMapper;
    @Resource
    private HcCutRoundReportMapper cutRoundReportMapper;
    @Resource
    private HcPackReportMapper packReportMapper;
    @Resource
    private HcGrindingProductionLedgerMapper grindingProductionLedgerMapper;
    @Resource
    private HcGrindingFirstDetailMapper grindingFirstDetailMapper;
    @Resource
    private HcGrindingSecondDetailMapper grindingSecondDetailMapper;
    @Resource
    private HcSlittingSliceRecordMapper slittingSliceRecordMapper;
    @Resource
    private HcWetProductionRecordMapper wetProductionRecordMapper;
    @Resource
    private HcCutRoundProductionRecordMapper cutRoundProductionRecordMapper;
    @Resource
    private HcSlittingPressProductionLedgerMapper slittingPressProductionLedgerMapper;
    @Resource
    private HcStationRecordMapper stationRecordMapper;
    @Resource
    private HcStationRecordItemMapper stationRecordItemMapper;
    @Resource
    private HcProcessFormRecordMapper processFormRecordMapper;
    @Resource
    private HcProcessFormRecordItemMapper processFormRecordItemMapper;
    /**
     * 直接复用各工序生产记录表的只读聚合服务，保证综合报表与各工序生产记录页面口径一致。
     */
    @Resource
    private HcFormulaProductionRecordService formulaProductionRecordService;
    @Resource
    private HcWetProductionRecordService wetProductionRecordService;
    @Resource
    private HcGrindingProductionRecordLedgerService grindingProductionRecordLedgerService;
    @Resource
    private HcAdhesiveProductionRecordService adhesiveProductionRecordService;
    @Resource
    private HcSlittingPressProductionRecordService slittingPressProductionRecordService;
    @Resource
    private HcCutRoundConsoleService cutRoundConsoleService;

    @Override
    public HcProcessReportOverviewDetailRespVO getDetail(Long planId) {
        HcPlanOrderDO plan = planId == null ? null : planOrderMapper.selectById(planId);
        if (plan == null) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
        List<HcPlanOrderOperationDO> operations = operationMapper.selectList(new LambdaQueryWrapperX<HcPlanOrderOperationDO>()
                .eq(HcPlanOrderOperationDO::getPlanId, planId)
                .eq(HcPlanOrderOperationDO::getDeleted, false)
                .orderByAsc(HcPlanOrderOperationDO::getOpSeq)
                .orderByAsc(HcPlanOrderOperationDO::getSort)
                .orderByAsc(HcPlanOrderOperationDO::getId));

        List<HcProcessReportDO> reports = processReportMapper.selectList(new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getPlanId, planId)
                .eq(HcProcessReportDO::getDeleted, false)
                .orderByAsc(HcProcessReportDO::getPlanOperationId)
                .orderByAsc(HcProcessReportDO::getStartTime)
                .orderByAsc(HcProcessReportDO::getId));
        List<HcProcessReportOverviewDetailRespVO.ReportRecord> reportRows = new ArrayList<>(
                reports.stream().map(this::toReportRecord).toList());
        reportRows.addAll(selectSpecializedReportRows(plan, operations, reports));
        List<HcProcessFormRecordDO> processForms = processFormRecordMapper.selectList(new LambdaQueryWrapperX<HcProcessFormRecordDO>()
                .eq(HcProcessFormRecordDO::getPlanId, planId)
                .eq(HcProcessFormRecordDO::getDeleted, false)
                .orderByAsc(HcProcessFormRecordDO::getPlanOperationId)
                .orderByAsc(HcProcessFormRecordDO::getRecordDate)
                .orderByAsc(HcProcessFormRecordDO::getId)).stream()
                .filter(this::includeProcessForm)
                .toList();
        List<HcStationRecordDO> stationForms = selectStationForms(planId);

        Map<Long, List<HcProcessFormRecordItemDO>> processItems = loadProcessItems(processForms);
        Map<Long, List<HcStationRecordItemDO>> stationItems = loadStationItems(stationForms);
        Map<Long, HcProcessFormRecordDO> processFormsById = processForms.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HcProcessFormRecordDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<HcProcessReportOverviewDetailRespVO.ReportRecord>> reportsByOperation = reportRows.stream()
                .filter(item -> item.getPlanOperationId() != null)
                .collect(Collectors.groupingBy(HcProcessReportOverviewDetailRespVO.ReportRecord::getPlanOperationId,
                        LinkedHashMap::new, Collectors.toList()));
        List<HcProcessReportOverviewDetailRespVO.ReportRecord> productionRecordRows =
                selectProductionRecordRows(plan, operations, reports);
        Map<Long, List<HcProcessReportOverviewDetailRespVO.ReportRecord>> productionRecordsByOperation =
                productionRecordRows.stream()
                        .filter(item -> item.getPlanOperationId() != null)
                        .collect(Collectors.groupingBy(HcProcessReportOverviewDetailRespVO.ReportRecord::getPlanOperationId,
                                LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<HcProcessFormRecordDO>> processFormsByOperation = processForms.stream()
                .filter(item -> item.getPlanOperationId() != null)
                .collect(Collectors.groupingBy(HcProcessFormRecordDO::getPlanOperationId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<HcStationRecordDO>> stationFormsByOperation = stationForms.stream()
                .filter(item -> item.getPlanOperationId() != null)
                .collect(Collectors.groupingBy(HcStationRecordDO::getPlanOperationId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> operationIds = operations.stream().map(HcPlanOrderOperationDO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        HcProcessReportOverviewDetailRespVO response = new HcProcessReportOverviewDetailRespVO();
        response.setPlan(toPlanSummary(plan));
        List<HcProcessReportOverviewDetailRespVO.FormRecord> unassignedForms = new ArrayList<>();
        for (HcProcessFormRecordDO form : processForms) {
            if (form.getPlanOperationId() == null || !operationIds.contains(form.getPlanOperationId())) {
                unassignedForms.add(toProcessForm(form, processItems.getOrDefault(form.getId(), List.of())));
            }
        }
        for (HcStationRecordDO form : stationForms) {
            if ((form.getPlanOperationId() == null || !operationIds.contains(form.getPlanOperationId()))
                    && !hasProcessMirror(form, processFormsById)) {
                unassignedForms.add(toStationForm(form, stationItems.getOrDefault(form.getId(), List.of()), null));
            }
        }
        response.setUnassignedForms(unassignedForms);
        response.setUnassignedReports(reportRows.stream()
                .filter(report -> report.getPlanOperationId() == null
                        || !operationIds.contains(report.getPlanOperationId()))
                .toList());

        List<HcProcessReportOverviewDetailRespVO.OperationSummary> operationRows = new ArrayList<>();
        for (HcPlanOrderOperationDO operation : operations) {
            List<HcProcessReportOverviewDetailRespVO.ReportRecord> operationReports = reportsByOperation.getOrDefault(
                    operation.getId(), List.of());
            List<HcProcessFormRecordDO> operationProcessForms = processFormsByOperation.getOrDefault(operation.getId(), List.of());
            List<HcStationRecordDO> operationStationForms = stationFormsByOperation.getOrDefault(operation.getId(), List.of());
            List<HcProcessReportOverviewDetailRespVO.ReportRecord> summaryReports = selectSummaryReports(operation, operationReports);
            HcProcessReportOverviewDetailRespVO.OperationSummary row = toOperationSummary(operation, summaryReports);
            List<HcProcessReportOverviewDetailRespVO.FormRecord> formRows = mergeForms(
                    operationProcessForms, operationStationForms, processItems, stationItems);
            row.setForms(formRows);
            row.setFormCount(formRows.size());
            row.setConfirmedFormCount((int) formRows.stream().filter(this::isConfirmed).count());
            row.setAbnormalFormCount((int) formRows.stream().filter(this::isAbnormal).count());
            row.setMirroredFormCount((int) formRows.stream().filter(item -> item.getMirrorRecordId() != null).count());
            row.setReports(operationReports);
            List<HcProcessReportOverviewDetailRespVO.ReportRecord> productionRecords =
                    productionRecordsByOperation.getOrDefault(operation.getId(), List.of());
            row.setProductionRecords(productionRecords);
            row.setProductionRecordCount(productionRecords.size());
            operationRows.add(row);
        }
        response.setOperations(operationRows);
        return response;
    }

    private List<HcStationRecordDO> selectStationForms(Long planId) {
        // 只读取实际挂在计划上的工位记录。设备每日表单不能通过设备和日期范围补查进来。
        List<HcStationRecordDO> result = new ArrayList<>(stationRecordMapper.selectList(new LambdaQueryWrapperX<HcStationRecordDO>()
                .eq(HcStationRecordDO::getPlanId, planId)
                .eq(HcStationRecordDO::getDeleted, false)
                .orderByAsc(HcStationRecordDO::getPlanOperationId)
                .orderByAsc(HcStationRecordDO::getRecordDate)
                .orderByAsc(HcStationRecordDO::getId)).stream()
                .filter(this::includeStationForm)
                .toList());
        return result.stream().filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HcStationRecordDO::getId, Function.identity(), (left, right) -> {
                    if (left.getPlanOperationId() == null && right.getPlanOperationId() != null) {
                        return right;
                    }
                    return left;
                }, LinkedHashMap::new)).values().stream().toList();
    }

    private Map<Long, List<HcProcessFormRecordItemDO>> loadProcessItems(List<HcProcessFormRecordDO> forms) {
        List<Long> ids = forms.stream().map(HcProcessFormRecordDO::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return processFormRecordItemMapper.selectList(new LambdaQueryWrapperX<HcProcessFormRecordItemDO>()
                        .in(HcProcessFormRecordItemDO::getRecordId, ids)
                        .eq(HcProcessFormRecordItemDO::getDeleted, false)
                        .orderByAsc(HcProcessFormRecordItemDO::getRecordId)
                        .orderByAsc(HcProcessFormRecordItemDO::getItemSeq)
                        .orderByAsc(HcProcessFormRecordItemDO::getId))
                .stream().collect(Collectors.groupingBy(HcProcessFormRecordItemDO::getRecordId));
    }

    private Map<Long, List<HcStationRecordItemDO>> loadStationItems(List<HcStationRecordDO> forms) {
        List<Long> ids = forms.stream().map(HcStationRecordDO::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return stationRecordItemMapper.selectList(new LambdaQueryWrapperX<HcStationRecordItemDO>()
                        .in(HcStationRecordItemDO::getRecordId, ids)
                        .eq(HcStationRecordItemDO::getDeleted, false)
                        .orderByAsc(HcStationRecordItemDO::getRecordId)
                        .orderByAsc(HcStationRecordItemDO::getItemSeq)
                        .orderByAsc(HcStationRecordItemDO::getId))
                .stream().collect(Collectors.groupingBy(HcStationRecordItemDO::getRecordId));
    }

    private HcProcessReportOverviewDetailRespVO.PlanSummary toPlanSummary(HcPlanOrderDO plan) {
        HcProcessReportOverviewDetailRespVO.PlanSummary summary = new HcProcessReportOverviewDetailRespVO.PlanSummary();
        summary.setId(plan.getId());
        summary.setPlanNo(plan.getPlanNo());
        summary.setPlanDate(plan.getPlanDate());
        summary.setPlanStatus(plan.getPlanStatus());
        summary.setProductionStartDate(plan.getProductionStartDate());
        summary.setProductionEndDate(plan.getProductionEndDate());
        summary.setMaterialCode(plan.getMaterialCode());
        summary.setMaterialName(plan.getMaterialName());
        summary.setMotherMaterialCode(plan.getMotherMaterialCode());
        summary.setMotherMaterialName(plan.getMotherMaterialName());
        summary.setModelCode(plan.getModelCode());
        summary.setModelName(plan.getModelName());
        summary.setMotherModelCode(plan.getMotherModelCode());
        summary.setMotherModelName(plan.getMotherModelName());
        summary.setSizeSpec(firstNotBlank(plan.getSizeSpec(), plan.getSizeName()));
        summary.setTargetQty(plan.getTargetQty());
        summary.setTargetUom(firstNotBlank(plan.getTargetUom(), plan.getTargetUnitCode(), plan.getTargetUnitName()));
        summary.setBatchNo(firstNotBlank(plan.getBatchNo(), plan.getProductionBatchNo(), plan.getParentProductionBatchNo()));
        summary.setProductionBatchNo(plan.getProductionBatchNo());
        summary.setParentProductionBatchNo(plan.getParentProductionBatchNo());
        return summary;
    }

    private HcProcessReportOverviewDetailRespVO.OperationSummary toOperationSummary(
            HcPlanOrderOperationDO operation,
            List<HcProcessReportOverviewDetailRespVO.ReportRecord> reports) {
        HcProcessReportOverviewDetailRespVO.OperationSummary summary = new HcProcessReportOverviewDetailRespVO.OperationSummary();
        summary.setId(operation.getId());
        summary.setOpSeq(operation.getOpSeq());
        summary.setOpCode(operation.getOpCode());
        summary.setOpName(operation.getOpName());
        summary.setOperationStatus(operation.getOperationStatus());
        summary.setOperationStatusText(operationStatusText(operation.getOperationStatus()));
        summary.setWorkCenterCode(operation.getWorkCenterCode());
        summary.setWorkCenterName(operation.getWorkCenterName());
        summary.setEquipmentCode(operation.getEquipmentCode());
        summary.setEquipmentName(operation.getEquipmentName());
        summary.setRequiredQty(operation.getRequiredQty());
        summary.setUom(operation.getUom());
        summary.setFinishTime(operation.getFinishTime());
        summary.setFinishRemark(operation.getFinishRemark());
        summary.setReportCount(reports.size());
        summary.setStartReportCount((int) reports.stream().filter(item -> "START".equalsIgnoreCase(item.getReportType())).count());
        summary.setEndReportCount((int) reports.stream().filter(item -> "END".equalsIgnoreCase(item.getReportType())).count());
        summary.setReportedQty(sumReportRows(reports, HcProcessReportOverviewDetailRespVO.ReportRecord::getReportQty));
        summary.setGoodQty(sumReportRows(reports, HcProcessReportOverviewDetailRespVO.ReportRecord::getGoodQty));
        summary.setScrapQty(sumReportRows(reports, HcProcessReportOverviewDetailRespVO.ReportRecord::getScrapQty));
        HcProcessReportOverviewDetailRespVO.ReportRecord latest = reports.stream()
                .max(Comparator.comparing(HcProcessReportOverviewDetailRespVO.ReportRecord::getReportTime,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(HcProcessReportOverviewDetailRespVO.ReportRecord::getId,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        if (latest != null) {
            summary.setOutputPostStatus(latest.getOutputPostStatus());
            summary.setLatestReportTime(latest.getReportTime());
            summary.setLatestRecorderName(latest.getRecorderName());
            summary.setLatestConfirmerName(latest.getConfirmerName());
        }
        return summary;
    }

    private List<HcProcessReportOverviewDetailRespVO.FormRecord> mergeForms(
            List<HcProcessFormRecordDO> processForms,
            List<HcStationRecordDO> stationForms,
            Map<Long, List<HcProcessFormRecordItemDO>> processItems,
            Map<Long, List<HcStationRecordItemDO>> stationItems) {
        Map<Long, HcProcessFormRecordDO> processById = processForms.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HcProcessFormRecordDO::getId, Function.identity(), (left, right) -> left));
        List<HcProcessReportOverviewDetailRespVO.FormRecord> result = new ArrayList<>();
        for (HcProcessFormRecordDO form : processForms) {
            HcProcessReportOverviewDetailRespVO.FormRecord row = toProcessForm(form, processItems.getOrDefault(form.getId(), List.of()));
            HcStationRecordDO mirror = stationForms.stream()
                    .filter(item -> Objects.equals(item.getSourceProcessFormRecordId(), form.getId()))
                    .findFirst().orElse(null);
            if (mirror != null) {
                row.setMirrorRecordId(mirror.getId());
            }
            result.add(row);
        }
        for (HcStationRecordDO form : stationForms) {
            if (form.getSourceProcessFormRecordId() != null && processById.containsKey(form.getSourceProcessFormRecordId())) {
                continue;
            }
            result.add(toStationForm(form, stationItems.getOrDefault(form.getId(), List.of()), null));
        }
        return result.stream().sorted(Comparator.comparing(HcProcessReportOverviewDetailRespVO.FormRecord::getRecordDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HcProcessReportOverviewDetailRespVO.FormRecord::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    private HcProcessReportOverviewDetailRespVO.FormRecord toProcessForm(
            HcProcessFormRecordDO source, List<HcProcessFormRecordItemDO> items) {
        HcProcessReportOverviewDetailRespVO.FormRecord row = new HcProcessReportOverviewDetailRespVO.FormRecord();
        row.setSourceType("PROCESS_FORM");
        row.setId(source.getId());
        row.setSourceProcessFormRecordId(source.getId());
        row.setPlanOperationId(source.getPlanOperationId());
        row.setRecordScope(source.getPlanOperationId() == null ? null : "PLAN_OPERATION");
        row.setRecordNo(source.getRecordNo());
        row.setTemplateId(source.getTemplateId());
        row.setVersionId(source.getVersionId());
        row.setTemplateCode(source.getTemplateCode());
        row.setTemplateName(source.getTemplateName());
        row.setProcessCode(source.getProcessCode());
        row.setProcessName(source.getProcessName());
        row.setFormType(source.getFormType());
        row.setFormTypeName(source.getFormTypeName());
        row.setModelCode(source.getModelCode());
        row.setModelName(source.getModelName());
        row.setBatchNo(source.getBatchNo());
        row.setEquipmentCode(source.getEquipmentCode());
        row.setEquipmentName(source.getEquipmentName());
        row.setRecordStatus(source.getRecordStatus());
        row.setResultStatus(source.getResultStatus());
        row.setRecordDate(source.getRecordDate());
        row.setFillUserName(source.getFillUserName());
        row.setFillTime(source.getFillTime());
        row.setConfirmUserName(source.getConfirmUserName());
        row.setConfirmTime(source.getConfirmTime());
        row.setHeaderDataJson(source.getHeaderDataJson());
        row.setContextJson(source.getContextJson());
        row.setFormRemark(source.getRemark());
        row.setItems(items.stream().map(this::toProcessItem).toList());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.FormRecord toStationForm(
            HcStationRecordDO source, List<HcStationRecordItemDO> items, Long mirrorRecordId) {
        HcProcessReportOverviewDetailRespVO.FormRecord row = new HcProcessReportOverviewDetailRespVO.FormRecord();
        row.setSourceType("STATION_RECORD");
        row.setId(source.getId());
        row.setMirrorRecordId(mirrorRecordId);
        row.setSourceProcessFormRecordId(source.getSourceProcessFormRecordId());
        row.setPlanOperationId(source.getPlanOperationId());
        row.setBizType(source.getBizType());
        row.setBizId(source.getBizId());
        row.setTemplateId(source.getFormId());
        row.setTemplateCode(source.getFormCode());
        row.setTemplateName(source.getFormName());
        row.setProcessCode(source.getOperationCode());
        row.setProcessName(source.getOperationName());
        row.setRecordScope(source.getRecordScope());
        row.setTriggerTimingCode(source.getTriggerTimingCode());
        row.setTriggerTimingName(source.getTriggerTimingName());
        row.setEquipmentCode(source.getEquipmentCode());
        row.setEquipmentName(source.getEquipmentName());
        row.setDocStatus(source.getDocStatus());
        row.setResultStatus(source.getResultStatus());
        row.setInspectionResult(source.getInspectionResult());
        row.setRecordDate(source.getRecordDate());
        row.setRecordUserName(source.getRecordUserName());
        row.setRecordTime(source.getRecordTime());
        row.setConfirmUserName(source.getConfirmUserName());
        row.setConfirmTime(source.getConfirmTime());
        row.setHeaderDataJson(source.getHeaderDataJson());
        row.setFormRemark(source.getFormRemark());
        row.setConfirmRemark(source.getConfirmRemark());
        row.setItems(items.stream().map(this::toStationItem).toList());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.FormItem toProcessItem(HcProcessFormRecordItemDO source) {
        HcProcessReportOverviewDetailRespVO.FormItem row = new HcProcessReportOverviewDetailRespVO.FormItem();
        row.setId(source.getId());
        row.setRecordId(source.getRecordId());
        row.setTemplateItemId(source.getTemplateItemId());
        row.setItemSeq(source.getItemSeq());
        row.setFieldKey(source.getFieldKey());
        row.setFieldLabel(source.getFieldLabel());
        row.setItemCategory(source.getItemCategory());
        row.setStepNode(source.getStepNode());
        row.setStandardText(source.getStandardText());
        row.setUnit(source.getUnit());
        row.setValueMode(source.getValueMode());
        row.setControlType(source.getControlType());
        row.setActualValue(source.getActualValue());
        row.setActualValue2(source.getActualValue2());
        row.setActualNumber(source.getActualNumber());
        row.setActualTime(source.getActualTime());
        row.setResultFlag(source.getResultFlag());
        row.setAbnormalRemark(source.getAbnormalRemark());
        row.setSourceRowJson(source.getSourceRowJson());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.FormItem toStationItem(HcStationRecordItemDO source) {
        HcProcessReportOverviewDetailRespVO.FormItem row = new HcProcessReportOverviewDetailRespVO.FormItem();
        row.setId(source.getId());
        row.setRecordId(source.getRecordId());
        row.setItemSeq(source.getItemSeq());
        row.setItemName(source.getItemName());
        row.setItemCategory(source.getItemCategory());
        row.setStepNode(source.getStepNode());
        row.setStandardText(source.getStandardText());
        row.setValueMode(source.getValueMode());
        row.setDualLabel1(source.getDualLabel1());
        row.setDualLabel2(source.getDualLabel2());
        row.setActualValue(source.getActualValue());
        row.setActualValue2(source.getActualValue2());
        row.setResultFlag(source.getResultFlag());
        row.setAbnormalRemark(source.getAbnormalRemark());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toReportRecord(HcProcessReportDO source) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = new HcProcessReportOverviewDetailRespVO.ReportRecord();
        row.setId(source.getId());
        row.setPlanOperationId(source.getPlanOperationId());
        row.setSourceType("OPERATION_REPORT");
        row.setSourceTable("mes_sfc_operation_report");
        row.setReportType(source.getReportType());
        row.setSourceMenuCode(source.getSourceMenuCode());
        row.setOperationStatus(source.getOperationStatus());
        row.setBatchNo(source.getBatchNo());
        row.setProductionBatchNo(source.getProductionBatchNo());
        row.setParentProductionBatchNo(source.getParentProductionBatchNo());
        row.setFeedQty(source.getFeedQty());
        row.setGoodQty(source.getGoodQty());
        row.setScrapQty(source.getScrapQty());
        row.setReportQty(source.getGoodQty());
        row.setReportUom(source.getReportUom());
        row.setOutputQty(source.getGoodQty());
        row.setOutputPostStatus(source.getOutputStockPostStatus());
        row.setReportStatus(source.getOperationStatus());
        row.setFaiNo(source.getFaiNo());
        row.setFaiStatus(source.getFaiStatus());
        row.setFaiJudgment(source.getFaiJudgment());
        row.setExtraJson(source.getExtraJson());
        row.setRemark(source.getRemark());
        row.setRecorderName(source.getRecorderName());
        row.setConfirmerName(source.getConfirmerName());
        row.setStartTime(source.getStartTime());
        row.setEndTime(source.getEndTime());
        row.setReportTime(firstNotNull(source.getEndTime(), source.getStartTime(), source.getRecorderTime()));
        row.setDetailJson(JSONUtil.toJsonStr(source));
        return row;
    }

    /**
     * 生产记录页签使用各工序生产记录服务的最终聚合结果，而不是在综合报表中再次拼接原始报工。
     * 查询仍限定在当前计划的批号集合内，避免同型号、同日期的其他计划串入详情。
     */
    private List<HcProcessReportOverviewDetailRespVO.ReportRecord> selectProductionRecordRows(
            HcPlanOrderDO plan, List<HcPlanOrderOperationDO> operations, List<HcProcessReportDO> reports) {
        Set<String> batchNos = collectPlanBatchNos(plan, reports);
        if (batchNos.isEmpty()) {
            return List.of();
        }
        Map<String, Long> operationIds = operations.stream()
                .filter(item -> item.getId() != null)
                .filter(item -> resolveOperationStage(item) != null)
                .collect(Collectors.toMap(this::resolveOperationStage, HcPlanOrderOperationDO::getId,
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, HcProcessReportOverviewDetailRespVO.ReportRecord> result = new LinkedHashMap<>();
        for (String batchNo : batchNos) {
            HcFormulaProductionRecordPageReqVO formulaReq = new HcFormulaProductionRecordPageReqVO();
            formulaReq.setPageSize(PageParam.PAGE_SIZE_NONE);
            formulaReq.setReportDateStart(plan.getProductionStartDate());
            formulaReq.setReportDateEnd(plan.getProductionEndDate());
            formulaReq.setBatchNo(batchNo);
            formulaReq.setModelCode(plan.getModelCode());
            formulaReq.setMaterialCode(plan.getMaterialCode());
            for (HcFormulaProductionRecordRespVO source : formulaProductionRecordService.getList(formulaReq)) {
                if (matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getReportDate(),
                        source.getModelCode(), source.getMaterialCode())) {
                    putProductionRecord(result, toFormulaProductionRecord(source, operationIds.get("FORMULA")));
                }
            }

            HcWetProductionRecordPageReqVO wetReq = new HcWetProductionRecordPageReqVO();
            wetReq.setPageSize(PageParam.PAGE_SIZE_NONE);
            wetReq.setRecordDateStart(plan.getProductionStartDate());
            wetReq.setRecordDateEnd(plan.getProductionEndDate());
            wetReq.setBatchNo(batchNo);
            PageResult<HcWetProductionRecordRespVO> wetResult = wetProductionRecordService.getReportPage(wetReq);
            for (HcWetProductionRecordRespVO source : wetResult.getList()) {
                if (matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getRecordDate(),
                        source.getModelCode(), source.getMaterialCode())) {
                    putProductionRecord(result, toWetProductionRecord(source, operationIds.get("WET")));
                }
            }

            HcGrindingProductionRecordPageReqVO grindingReq = new HcGrindingProductionRecordPageReqVO();
            grindingReq.setPageSize(PageParam.PAGE_SIZE_NONE);
            grindingReq.setCompletionTimeStart(atStartOfDay(plan.getProductionStartDate()));
            grindingReq.setCompletionTimeEnd(atEndOfDay(plan.getProductionEndDate()));
            grindingReq.setBatchNo(batchNo);
            grindingReq.setModelCode(plan.getModelCode());
            grindingReq.setMaterialCode(plan.getMaterialCode());
            PageResult<HcGrindingProductionRecordDO> grindingResult =
                    grindingProductionRecordLedgerService.getPage(grindingReq);
            for (HcGrindingProductionRecordDO source : grindingResult.getList()) {
                LocalDate recordDate = source.getRecordTime() == null ? source.getReportDate() : source.getRecordTime().toLocalDate();
                if (matchesManualRecord(plan, batchNos, source.getBatchNo(), recordDate,
                        source.getModelCode(), source.getMaterialCode())
                        || matchesManualRecord(plan, batchNos, source.getMotherBatchNo(), recordDate,
                        source.getModelCode(), source.getMaterialCode())) {
                    putProductionRecord(result, toGrindingProductionRecord(source, operationIds.get("GRINDING")));
                }
            }

            HcAdhesiveProductionRecordPageReqVO adhesiveReq = new HcAdhesiveProductionRecordPageReqVO();
            adhesiveReq.setPageSize(PageParam.PAGE_SIZE_NONE);
            adhesiveReq.setReportDateStart(plan.getProductionStartDate());
            adhesiveReq.setReportDateEnd(plan.getProductionEndDate());
            adhesiveReq.setBatchNo(batchNo);
            adhesiveReq.setModelCode(plan.getModelCode());
            adhesiveReq.setMaterialCode(plan.getMaterialCode());
            for (HcAdhesiveProductionRecordRespVO source : adhesiveProductionRecordService.getAdhesive1List(adhesiveReq)) {
                if (matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getReportDate(),
                        source.getModelCode(), source.getMaterialCode())) {
                    putProductionRecord(result, toAdhesiveProductionRecord(source, operationIds.get("ADHESIVE1"),
                            "ADHESIVE1_PRODUCTION_RECORD"));
                }
            }
            for (HcAdhesiveProductionRecordRespVO source : adhesiveProductionRecordService.getAdhesive2List(adhesiveReq)) {
                if (matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getReportDate(),
                        source.getModelCode(), source.getMaterialCode())) {
                    putProductionRecord(result, toAdhesiveProductionRecord(source, operationIds.get("ADHESIVE2"),
                            "ADHESIVE2_PRODUCTION_RECORD"));
                }
            }

            HcSlittingPressProductionRecordPageReqVO slittingReq = new HcSlittingPressProductionRecordPageReqVO();
            slittingReq.setPageSize(PageParam.PAGE_SIZE_NONE);
            slittingReq.setReportDateStart(plan.getProductionStartDate());
            slittingReq.setReportDateEnd(plan.getProductionEndDate());
            slittingReq.setBatchNo(batchNo);
            slittingReq.setModelCode(plan.getModelCode());
            slittingReq.setMaterialCode(plan.getMaterialCode());
            for (HcSlittingPressProductionRecordRespVO source : slittingPressProductionRecordService.getList(slittingReq)) {
                if (!matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getReportDate(),
                        source.getModelCode(), source.getMaterialCode())) {
                    continue;
                }
                if (operationIds.get("SLITTING") != null
                        && (source.getSlittingInputM() != null || source.getSlittingOutputPcs() != null)) {
                    putProductionRecord(result, toSlittingPressProductionRecord(source, operationIds.get("SLITTING"), true));
                }
                if (operationIds.get("PRESS_SLOT") != null
                        && (source.getPressSlotActualInputPcs() != null || source.getPressSlotOutputPcs() != null)) {
                    putProductionRecord(result, toSlittingPressProductionRecord(source, operationIds.get("PRESS_SLOT"), false));
                }
            }

            HcCutRoundProductionRecordPageReqVO cutRoundReq = new HcCutRoundProductionRecordPageReqVO();
            cutRoundReq.setPageSize(PageParam.PAGE_SIZE_NONE);
            cutRoundReq.setReportDateStart(plan.getProductionStartDate());
            cutRoundReq.setReportDateEnd(plan.getProductionEndDate());
            cutRoundReq.setProductionBatchNo(batchNo);
            cutRoundReq.setModelCode(plan.getModelCode());
            for (HcCutRoundProductionRecordRespVO source : cutRoundConsoleService.getProductionRecordList(cutRoundReq)) {
                if (matchesManualRecord(plan, batchNos, source.getProductionBatchNo(), source.getReportDate(),
                        source.getModelCode(), null)) {
                    putProductionRecord(result, toCutRoundProductionRecord(source, operationIds.get("CUT_ROUND")));
                }
            }
        }
        return result.values().stream()
                .sorted(Comparator.comparing(HcProcessReportOverviewDetailRespVO.ReportRecord::getReportTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HcProcessReportOverviewDetailRespVO.ReportRecord::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void putProductionRecord(Map<String, HcProcessReportOverviewDetailRespVO.ReportRecord> target,
                                     HcProcessReportOverviewDetailRespVO.ReportRecord row) {
        if (row == null || row.getPlanOperationId() == null) {
            return;
        }
        String key = String.join("|", StrUtil.blankToDefault(row.getSourceType(), "UNKNOWN"),
                String.valueOf(row.getPlanOperationId()), String.valueOf(row.getId()),
                StrUtil.blankToDefault(row.getRecordRole(), ""), StrUtil.blankToDefault(row.getReportType(), ""));
        target.putIfAbsent(key, row);
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
    }

    private String statusOrRecorded(String status) {
        return StringUtils.hasText(status) ? status : "RECORDED";
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toFormulaProductionRecord(
            HcFormulaProductionRecordRespVO source, Long planOperationId) {
        if (source == null || planOperationId == null) {
            return null;
        }
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(source.getId(), planOperationId,
                "FORMULA_PRODUCTION_RECORD", "mes_sfc_operation_report", "END", "RECORDED", source.getBatchNo(),
                source.getBatchNo(), null, source.getInputWeight(), source.getOutputWeight(), source.getOutputWeight(),
                null, "kg", null, source.getRecordTime(), source.getRecorderName(), source.getRecordTime(), null,
                null, source.getRemark(), source);
        row.setSourceMenuCode("FORMULA_REPORT");
        row.setSourceBizType("FORMULA_REPORT");
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), source.getPadType(), source.getMaterialCode());
        row.setInputUom("kg");
        row.setOutputUom("kg");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toWetProductionRecord(
            HcWetProductionRecordRespVO source, Long planOperationId) {
        if (source == null || planOperationId == null) {
            return null;
        }
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(source.getId(), planOperationId,
                "WET_PRODUCTION_RECORD", "mes_hc_wet_production_record", "PRODUCTION_RECORD", statusOrRecorded(source.getStatus()),
                source.getBatchNo(), source.getBatchNo(), null, source.getInputKg(), source.getOutputMeter(),
                source.getOutputMeter(), null, "m", null, source.getRecordTime(), source.getRecorderName(),
                source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(), source.getRemark(), source);
        row.setSourceBizType(source.getDataSource());
        setProductionIdentity(row, source.getRecordDate(), source.getModelCode(), source.getPadType(), source.getMaterialCode());
        row.setInputUom("kg");
        row.setOutputUom("m");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toGrindingProductionRecord(
            HcGrindingProductionRecordRespVO source, Long planOperationId) {
        if (source == null || planOperationId == null) {
            return null;
        }
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(source.getId(), planOperationId,
                "GRINDING_PRODUCTION_RECORD", "mes_hc_grinding_production_record", source.getPassType(),
                source.getStatus(), source.getBatchNo(), source.getBatchNo(), source.getMotherBatchNo(),
                source.getInputLength(), source.getOutputLength(), source.getOutputLength(), null, "m", null,
                source.getRecordTime(), source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(),
                source.getRemark(), source);
        row.setRecordRole(source.getRecordRole());
        row.setSourceBizType(source.getSourceBizType());
        row.setSourceDetailId(source.getSourceDetailId());
        row.setSegmentMark(source.getSegmentMark());
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), source.getPadType(), source.getMaterialCode());
        row.setInputUom("m");
        row.setOutputUom("m");
        row.setStartPosition(source.getStartPosition());
        row.setInputLength(source.getInputLength());
        row.setOutputLength(source.getOutputLength());
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toAdhesiveProductionRecord(
            HcAdhesiveProductionRecordRespVO source, Long planOperationId, String sourceType) {
        if (source == null || planOperationId == null) {
            return null;
        }
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(source.getId(), planOperationId,
                sourceType, "mes_hc_adhesive_production_record", "PRODUCTION_RECORD", "RECORDED", source.getBatchNo(),
                source.getBatchNo(), null, source.getInputQty(), source.getOutputQty(), source.getOutputQty(), null,
                "m", null, source.getRecordTime(), source.getRecorderName(), source.getRecordTime(), null, null, source.getRemark(), source);
        row.setSourceBizType(source.getRecordSource());
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), source.getPadType(), source.getMaterialCode());
        row.setInputUom("ADHESIVE2_PRODUCTION_RECORD".equals(sourceType) ? "pcs" : "m");
        row.setOutputUom("ADHESIVE2_PRODUCTION_RECORD".equals(sourceType) ? "pcs" : "m");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toSlittingPressProductionRecord(
            HcSlittingPressProductionRecordRespVO source, Long planOperationId, boolean slitting) {
        if (source == null || planOperationId == null) {
            return null;
        }
        BigDecimal input = slitting ? source.getSlittingInputM() : source.getPressSlotActualInputPcs();
        BigDecimal output = slitting ? decimal(source.getSlittingOutputPcs()) : source.getPressSlotActualOutputPcs();
        BigDecimal good = slitting ? decimal(source.getSlittingOutputPcs()) : source.getPressSlotOutputPcs();
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(source.getId(), planOperationId,
                "SLITTING_PRESS_PRODUCTION_RECORD", "mes_hc_slitting_press_production_record",
                slitting ? "SLITTING" : "PRESS_SLOT", source.getStatus(), source.getBatchNo(), source.getBatchNo(),
                null, input, output, good, slitting ? decimal(source.getSlittingNgPcs()) : null, slitting ? "m" : "pcs",
                null, source.getRecordTime(), source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(),
                source.getRemark(), source);
        row.setSourceBizType(source.getSourceType());
        row.setRecordRole(slitting ? "SLITTING" : "PRESS_SLOT");
        row.setOutputQty(output);
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), source.getPadType(), source.getMaterialCode());
        row.setInputUom(slitting ? "m" : "pcs");
        row.setOutputUom("pcs");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toCutRoundProductionRecord(
            HcCutRoundProductionRecordRespVO source, Long planOperationId) {
        if (source == null || planOperationId == null) {
            return null;
        }
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(source.getId(), planOperationId,
                "CUT_ROUND_PRODUCTION_RECORD", "mes_hc_cut_round_production_record", "PRODUCTION_RECORD",
                source.getStatus(), source.getProductionBatchNo(), source.getProductionBatchNo(), null,
                source.getInputQty(), source.getOutputQty(), source.getOutputQty(), null, "pcs", null,
                source.getRecordTime(), source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(),
                source.getRemark(), source);
        row.setSourceBizType(source.getSourceType());
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), source.getPadType(), null);
        row.setInputUom("pcs");
        row.setOutputUom("pcs");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private List<HcProcessReportOverviewDetailRespVO.ReportRecord> selectSpecializedReportRows(
            HcPlanOrderDO plan, List<HcPlanOrderOperationDO> operations, List<HcProcessReportDO> reports) {
        Long planId = plan.getId();
        List<HcProcessReportOverviewDetailRespVO.ReportRecord> rows = new ArrayList<>();
        rows.addAll(selectManualProductionRows(plan, operations, reports));
        Map<Long, Long> grindingDetailOperationIds = new LinkedHashMap<>();
        for (HcPlanOrderOperationDO operation : operations) {
            if (!isGrindingOperation(operation)) {
                continue;
            }
            grindingFirstDetailMapper.selectListByPlanOperationId(operation.getId()).stream()
                    .map(HcGrindingFirstDetailDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(id -> grindingDetailOperationIds.put(id, operation.getId()));
            grindingSecondDetailMapper.selectListByPlanOperationId(operation.getId()).stream()
                    .map(HcGrindingSecondDetailDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(id -> grindingDetailOperationIds.put(id, operation.getId()));
        }
        if (!grindingDetailOperationIds.isEmpty()) {
            rows.addAll(grindingProductionLedgerMapper.selectByRecordIds(grindingDetailOperationIds.keySet()).stream()
                    .filter(source -> !Boolean.TRUE.equals(source.getDeleted()))
                    .map(source -> toGrindingProductionRecord(source,
                            grindingDetailOperationIds.get(source.getSourceDetailId())))
                    .filter(Objects::nonNull)
                    .toList());
        }
        rows.addAll(slittingSliceRecordMapper.selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                        .eq(HcSlittingSliceRecordDO::getPlanId, planId)
                        .eq(HcSlittingSliceRecordDO::getDeleted, false)
                        .orderByAsc(HcSlittingSliceRecordDO::getPlanOperationId)
                        .orderByAsc(HcSlittingSliceRecordDO::getSourceAdhesiveReportId)
                        .orderByAsc(HcSlittingSliceRecordDO::getSliceIndex)
                        .orderByAsc(HcSlittingSliceRecordDO::getId))
                .stream().map(this::toSlittingSliceRecord).toList());
        rows.addAll(grindingReportMapper.selectList(new LambdaQueryWrapperX<HcGrindingReportDO>()
                        .eq(HcGrindingReportDO::getPlanId, planId)
                        .eq(HcGrindingReportDO::getDeleted, false)
                        .orderByAsc(HcGrindingReportDO::getPlanOperationId)
                        .orderByAsc(HcGrindingReportDO::getStartTime)
                        .orderByAsc(HcGrindingReportDO::getId))
                .stream().map(this::toGrindingReport).toList());
        rows.addAll(adhesiveReportMapper.selectList(new LambdaQueryWrapperX<HcAdhesiveReportDO>()
                        .eq(HcAdhesiveReportDO::getPlanId, planId)
                        .eq(HcAdhesiveReportDO::getDeleted, false)
                        .orderByAsc(HcAdhesiveReportDO::getPlanOperationId)
                        .orderByAsc(HcAdhesiveReportDO::getStartTime)
                        .orderByAsc(HcAdhesiveReportDO::getId))
                .stream().map(this::toAdhesiveReport).toList());
        rows.addAll(adhesive2ReportMapper.selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                        .eq(HcAdhesive2ReportDO::getPlanId, planId)
                        .eq(HcAdhesive2ReportDO::getDeleted, false)
                        .orderByAsc(HcAdhesive2ReportDO::getPlanOperationId)
                        .orderByAsc(HcAdhesive2ReportDO::getStartTime)
                        .orderByAsc(HcAdhesive2ReportDO::getId))
                .stream().map(this::toAdhesive2Report).toList());
        rows.addAll(pressSlotReportMapper.selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                        .eq(HcPressSlotReportDO::getPlanId, planId)
                        .eq(HcPressSlotReportDO::getDeleted, false)
                        .orderByAsc(HcPressSlotReportDO::getPlanOperationId)
                        .orderByAsc(HcPressSlotReportDO::getStartTime)
                        .orderByAsc(HcPressSlotReportDO::getId))
                .stream().map(this::toPressSlotReport).toList());
        rows.addAll(cutRoundReportMapper.selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                        .eq(HcCutRoundReportDO::getPlanId, planId)
                        .eq(HcCutRoundReportDO::getDeleted, false)
                        .orderByAsc(HcCutRoundReportDO::getPlanOperationId)
                        .orderByAsc(HcCutRoundReportDO::getStartTime)
                        .orderByAsc(HcCutRoundReportDO::getId))
                .stream().map(this::toCutRoundReport).toList());
        rows.addAll(packReportMapper.selectList(new LambdaQueryWrapperX<HcPackReportDO>()
                        .eq(HcPackReportDO::getPlanId, planId)
                        .eq(HcPackReportDO::getDeleted, false)
                        .orderByAsc(HcPackReportDO::getPlanOperationId)
                        .orderByAsc(HcPackReportDO::getPackageDate)
                        .orderByAsc(HcPackReportDO::getId))
                .stream().map(this::toPackReport).toList());
        return rows;
    }

    /**
     * 生产记录台账没有统一的 plan_operation_id，按计划批号和工序语义补齐到综合报表。
     * 只有批号/日期能够与当前计划对应时才纳入，避免把其他计划的手工台账串进来。
     */
    private List<HcProcessReportOverviewDetailRespVO.ReportRecord> selectManualProductionRows(
            HcPlanOrderDO plan, List<HcPlanOrderOperationDO> operations, List<HcProcessReportDO> reports) {
        Set<String> batchNos = collectPlanBatchNos(plan, reports);
        Long grindingOperationId = findOperationId(operations, "GRINDING");
        Long wetOperationId = findOperationId(operations, "WET");
        Long slittingOperationId = findOperationId(operations, "SLITTING");
        Long pressSlotOperationId = findOperationId(operations, "PRESS_SLOT");
        Long cutRoundOperationId = findOperationId(operations, "CUT_ROUND");
        List<HcProcessReportOverviewDetailRespVO.ReportRecord> rows = new ArrayList<>();

        if (grindingOperationId != null) {
            selectManualGrindingProductionRecords(plan, batchNos).stream()
                    .filter(source -> matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getReportDate(),
                            source.getModelCode(), source.getMaterialCode()))
                    .map(source -> toGrindingProductionRecord(source, grindingOperationId))
                    .forEach(rows::add);
        }
        if (wetOperationId != null) {
            selectWetProductionRecords(plan, batchNos).stream()
                    .filter(source -> matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getRecordDate(),
                            source.getModelCode(), source.getMaterialCode()))
                    .map(source -> toWetProductionRecord(source, wetOperationId))
                    .forEach(rows::add);
        }
        if (cutRoundOperationId != null) {
            selectCutRoundProductionRecords(plan, batchNos).stream()
                    .filter(source -> matchesManualRecord(plan, batchNos, source.getProductionBatchNo(), source.getReportDate(),
                            source.getModelCode(), null))
                    .map(source -> toCutRoundProductionRecord(source, cutRoundOperationId))
                    .forEach(rows::add);
        }
        List<HcSlittingPressProductionRecordDO> slittingPressRecords = selectSlittingPressProductionRecords(plan, batchNos);
        for (HcSlittingPressProductionRecordDO source : slittingPressRecords) {
            if (!matchesManualRecord(plan, batchNos, source.getBatchNo(), source.getReportDate(),
                    source.getModelCode(), source.getMaterialCode())) {
                continue;
            }
            if (slittingOperationId != null
                    && (source.getSlittingInputM() != null || source.getSlittingOutputPcs() != null)) {
                rows.add(toSlittingPressProductionRecord(source, slittingOperationId, true));
            }
            if (pressSlotOperationId != null
                    && (source.getPressSlotInputPcs() != null || source.getPressSlotOutputPcs() != null)) {
                rows.add(toSlittingPressProductionRecord(source, pressSlotOperationId, false));
            }
        }
        return rows;
    }

    private List<HcGrindingProductionRecordDO> selectManualGrindingProductionRecords(HcPlanOrderDO plan,
                                                                                        Set<String> batchNos) {
        LambdaQueryWrapperX<HcGrindingProductionRecordDO> query = new LambdaQueryWrapperX<>();
        query.eq(HcGrindingProductionRecordDO::getDeleted, false)
                .isNull(HcGrindingProductionRecordDO::getSourceDetailId);
        if (!batchNos.isEmpty()) {
            query.in(HcGrindingProductionRecordDO::getBatchNo, batchNos);
        } else {
            query.geIfPresent(HcGrindingProductionRecordDO::getReportDate, plan.getProductionStartDate())
                    .leIfPresent(HcGrindingProductionRecordDO::getReportDate, plan.getProductionEndDate());
        }
        return grindingProductionLedgerMapper.selectList(query.orderByAsc(HcGrindingProductionRecordDO::getReportDate)
                .orderByAsc(HcGrindingProductionRecordDO::getId));
    }

    private List<HcWetProductionRecordDO> selectWetProductionRecords(HcPlanOrderDO plan, Set<String> batchNos) {
        LambdaQueryWrapperX<HcWetProductionRecordDO> query = new LambdaQueryWrapperX<HcWetProductionRecordDO>()
                .eq(HcWetProductionRecordDO::getDeleted, false);
        if (!batchNos.isEmpty()) {
            query.in(HcWetProductionRecordDO::getBatchNo, batchNos);
        } else {
            query.geIfPresent(HcWetProductionRecordDO::getRecordDate, plan.getProductionStartDate())
                    .leIfPresent(HcWetProductionRecordDO::getRecordDate, plan.getProductionEndDate());
        }
        return wetProductionRecordMapper.selectList(query.orderByAsc(HcWetProductionRecordDO::getRecordDate)
                .orderByAsc(HcWetProductionRecordDO::getId));
    }

    private List<HcCutRoundProductionRecordDO> selectCutRoundProductionRecords(HcPlanOrderDO plan,
                                                                                  Set<String> batchNos) {
        LambdaQueryWrapperX<HcCutRoundProductionRecordDO> query = new LambdaQueryWrapperX<HcCutRoundProductionRecordDO>()
                .eq(HcCutRoundProductionRecordDO::getDeleted, false);
        if (!batchNos.isEmpty()) {
            query.in(HcCutRoundProductionRecordDO::getProductionBatchNo, batchNos);
        } else {
            query.geIfPresent(HcCutRoundProductionRecordDO::getReportDate, plan.getProductionStartDate())
                    .leIfPresent(HcCutRoundProductionRecordDO::getReportDate, plan.getProductionEndDate());
        }
        return cutRoundProductionRecordMapper.selectList(query.orderByAsc(HcCutRoundProductionRecordDO::getReportDate)
                .orderByAsc(HcCutRoundProductionRecordDO::getId));
    }

    private List<HcSlittingPressProductionRecordDO> selectSlittingPressProductionRecords(HcPlanOrderDO plan,
                                                                                           Set<String> batchNos) {
        LambdaQueryWrapperX<HcSlittingPressProductionRecordDO> query =
                new LambdaQueryWrapperX<HcSlittingPressProductionRecordDO>()
                        .eq(HcSlittingPressProductionRecordDO::getDeleted, false);
        if (!batchNos.isEmpty()) {
            query.in(HcSlittingPressProductionRecordDO::getBatchNo, batchNos);
        } else {
            query.geIfPresent(HcSlittingPressProductionRecordDO::getReportDate, plan.getProductionStartDate())
                    .leIfPresent(HcSlittingPressProductionRecordDO::getReportDate, plan.getProductionEndDate());
        }
        return slittingPressProductionLedgerMapper.selectList(query.orderByAsc(HcSlittingPressProductionRecordDO::getReportDate)
                .orderByAsc(HcSlittingPressProductionRecordDO::getId));
    }

    private Set<String> collectPlanBatchNos(HcPlanOrderDO plan, List<HcProcessReportDO> reports) {
        Set<String> result = new java.util.LinkedHashSet<>();
        addBatch(result, plan.getPlanNo());
        addBatch(result, plan.getBatchNo());
        addBatch(result, plan.getProductionBatchNo());
        addBatch(result, plan.getParentProductionBatchNo());
        for (HcProcessReportDO report : reports) {
            addBatch(result, report.getFeedBatchNo());
            addBatch(result, report.getBatchNo());
            addBatch(result, report.getProductionBatchNo());
            addBatch(result, report.getParentProductionBatchNo());
            addBatch(result, report.getParentBatchNo());
        }
        return result;
    }

    private void addBatch(Set<String> target, String value) {
        if (StringUtils.hasText(value)) {
            target.add(value.trim());
        }
    }

    private boolean matchesManualRecord(HcPlanOrderDO plan, Set<String> batchNos, String recordBatchNo,
                                        LocalDate recordDate, String modelCode, String materialCode) {
        boolean batchMatched = StringUtils.hasText(recordBatchNo)
                && batchNos.stream().anyMatch(item -> item.equalsIgnoreCase(recordBatchNo.trim()));
        if (!batchNos.isEmpty() && !batchMatched) {
            return false;
        }
        if (StringUtils.hasText(modelCode)
                && !sameText(plan.getModelCode(), modelCode)
                && !sameText(plan.getMotherModelCode(), modelCode)) {
            return false;
        }
        if (StringUtils.hasText(materialCode)
                && !sameText(plan.getMaterialCode(), materialCode)
                && !sameText(plan.getMotherMaterialCode(), materialCode)) {
            return false;
        }
        if (recordDate == null) {
            return true;
        }
        return (plan.getProductionStartDate() == null || !recordDate.isBefore(plan.getProductionStartDate()))
                && (plan.getProductionEndDate() == null || !recordDate.isAfter(plan.getProductionEndDate()));
    }

    private boolean sameText(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right)
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private Long findOperationId(List<HcPlanOrderOperationDO> operations, String stageCode) {
        return operations.stream().filter(operation -> stageCode.equals(resolveOperationStage(operation)))
                .map(HcPlanOrderOperationDO::getId).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String resolveOperationStage(HcPlanOrderOperationDO operation) {
        String code = StrUtil.blankToDefault(operation.getOpCode(), "").toUpperCase();
        String name = StrUtil.blankToDefault(operation.getOpName(), "");
        if (code.contains("FORMULA") || code.contains("MIX") || name.contains("配料")) {
            return "FORMULA";
        }
        if (code.contains("WET") || name.contains("湿法")) {
            return "WET";
        }
        if (code.contains("ADH2") || code.contains("ADHESIVE2") || name.contains("粘胶2")) {
            return "ADHESIVE2";
        }
        if (code.contains("ADH1") || code.contains("ADHESIVE1") || name.contains("粘胶1")) {
            return "ADHESIVE1";
        }
        if (code.contains("SLIT") || name.contains("分切")) {
            return "SLITTING";
        }
        if (code.contains("PRESS") || code.contains("GROOVE") || name.contains("压槽")) {
            return "PRESS_SLOT";
        }
        if (code.contains("CUT") || name.contains("裁切") || name.contains("裁圆")) {
            return "CUT_ROUND";
        }
        if (code.contains("GRIND") || name.contains("磨皮")) {
            return "GRINDING";
        }
        return null;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toWetProductionRecord(
            HcWetProductionRecordDO source, Long planOperationId) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), planOperationId, "WET_PRODUCTION_RECORD", "mes_hc_wet_production_record",
                "PRODUCTION_RECORD", source.getStatus(), source.getBatchNo(), source.getBatchNo(), null,
                source.getInputKg(), source.getOutputMeter(), source.getOutputMeter(), null, "m", null, null,
                source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(),
                source.getRemark(), source);
        setProductionIdentity(row, source.getRecordDate(), source.getModelCode(), null, source.getMaterialCode());
        row.setInputUom("kg");
        row.setOutputUom("m");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toCutRoundProductionRecord(
            HcCutRoundProductionRecordDO source, Long planOperationId) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), planOperationId, "CUT_ROUND_PRODUCTION_RECORD", "mes_hc_cut_round_production_record",
                "PRODUCTION_RECORD", source.getStatus(), source.getProductionBatchNo(), source.getProductionBatchNo(), null,
                source.getInputQty(), source.getOutputQty(), source.getOutputQty(), null, "片", null, null,
                source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(),
                source.getRemark(), source);
        row.setSourceBizType(source.getSourceType());
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), null, null);
        row.setInputUom("pcs");
        row.setOutputUom("pcs");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toSlittingPressProductionRecord(
            HcSlittingPressProductionRecordDO source, Long planOperationId, boolean slitting) {
        BigDecimal input = slitting ? source.getSlittingInputM() : source.getPressSlotInputPcs();
        BigDecimal output = slitting ? decimal(source.getSlittingOutputPcs()) : source.getPressSlotOutputPcs();
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), planOperationId, "SLITTING_PRESS_PRODUCTION_RECORD",
                "mes_hc_slitting_press_production_record", "PRODUCTION_RECORD", source.getStatus(),
                source.getBatchNo(), source.getBatchNo(), null, input, output, output, null, "片", null, null,
                source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(), source.getConfirmTime(),
                source.getRemark(), source);
        row.setSourceBizType(source.getSourceType());
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), null, source.getMaterialCode());
        row.setInputUom(slitting ? "m" : "pcs");
        row.setOutputUom("pcs");
        row.setReportTime(source.getRecordTime());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toSlittingSliceRecord(
            HcSlittingSliceRecordDO source) {
        boolean confirmed = "CONFIRMED".equalsIgnoreCase(source.getScanStatus());
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "SLITTING_SLICE", "mes_sfc_slitting_slice_record",
                "SLICE", source.getScanStatus(), source.getSourceBatchNo(), source.getSourceProductionBatchNo(),
                null, source.getSourceLength(), source.getSliceLength(), confirmed ? source.getSliceLength() : null,
                null, "m", null, null, source.getScannerName(), source.getScanTime(), null, null,
                source.getRemark(), source);
        row.setOutputPostStatus(source.getOutputStockPostStatus());
        row.setStartPosition(source.getStartPosition());
        row.setEndPosition(source.getEndPosition());
        row.setOutputLength(source.getSliceLength());
        row.setSelfCheck(source.getSelfCheck());
        row.setReportQty(source.getSliceLength());
        row.setGoodQty(confirmed ? source.getSliceLength() : null);
        row.setOutputQty(confirmed ? source.getSliceLength() : null);
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toGrindingProductionRecord(
            HcGrindingProductionRecordDO source, Long planOperationId) {
        if (source == null || planOperationId == null) {
            return null;
        }
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), null, "GRINDING_PRODUCTION_RECORD", "mes_hc_grinding_production_record",
                source.getPassType(), source.getStatus(), source.getBatchNo(), source.getBatchNo(),
                source.getMotherBatchNo(), source.getInputLength(), source.getOutputLength(), source.getOutputLength(),
                null, "m", null, null, source.getRecorderName(), source.getRecordTime(), source.getConfirmerName(),
                source.getConfirmTime(), source.getRemark(), source);
        row.setRecordRole(source.getRecordRole());
        row.setSourceBizType(source.getSourceBizType());
        row.setSourceDetailId(source.getSourceDetailId());
        row.setSegmentMark(source.getSegmentMark());
        setProductionIdentity(row, source.getReportDate(), source.getModelCode(), source.getPadType(), source.getMaterialCode());
        row.setInputUom("m");
        row.setOutputUom("m");
        row.setPlanOperationId(planOperationId);
        row.setReportTime(source.getRecordTime());
        row.setReportQty(source.getOutputLength());
        row.setGoodQty(source.getOutputLength());
        row.setOutputQty(source.getOutputLength());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toGrindingReport(HcGrindingReportDO source) {
        BigDecimal scrap = add(source.getFirstLossLength(), source.getSecondLossLength());
        BigDecimal output = add(source.getFirstOutputLength(), source.getSecondOutputLength());
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "GRINDING_REPORT", "mes_sfc_grinding_report",
                source.getReportType(), source.getReportStatus(), source.getBatchNo(), source.getProductionBatchNo(),
                source.getParentProductionBatchNo(), source.getInputLength(), source.getReportQty(), source.getReportQty(),
                scrap, null, source.getStartTime(), source.getEndTime(), source.getRecorderName(), source.getRecorderTime(),
                source.getConfirmerName(), source.getConfirmerTime(), source.getRemark(), source);
        row.setOutputQty(output);
        row.setReportUom("m");
        row.setInputLength(source.getInputLength());
        row.setOutputLength(output);
        row.setLossLength(scrap);
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toAdhesiveReport(HcAdhesiveReportDO source) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "ADHESIVE1_REPORT", "mes_sfc_adhesive_report",
                source.getSourceType(), source.getReportStatus(), source.getSourceBatchNo(), source.getProductionBatchNo(),
                source.getParentProductionBatchNo(), source.getInputLength(), source.getOutputLength(), source.getOutputLength(),
                source.getLossLength(), "m", source.getStartTime(), source.getEndTime(), source.getRecorderName(), source.getRecorderTime(),
                source.getConfirmerName(), source.getConfirmerTime(), source.getRemark(), source);
        row.setOutputPostStatus(source.getOutputStockPostStatus());
        row.setInputLength(source.getInputLength());
        row.setStartPosition(source.getStartPosition());
        row.setEndPosition(source.getEndPosition());
        row.setOutputLength(source.getOutputLength());
        row.setLossLength(source.getLossLength());
        row.setSelfCheck(source.getSelfCheck());
        row.setDefectCode(source.getDefectCode());
        row.setProductQualityStatus(source.getProductQualityStatus());
        row.setQualityLockReason(source.getQualityLockReason());
        row.setFaiNo(source.getFaiNo());
        row.setFaiStatus(source.getFaiStatus());
        row.setFaiJudgment(source.getFaiJudgment());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toAdhesive2Report(HcAdhesive2ReportDO source) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "ADHESIVE2_REPORT", "mes_sfc_adhesive2_report",
                "DETAIL", source.getReportStatus(), source.getSourceBatchNo(), source.getProductionBatchNo(),
                source.getParentProductionBatchNo(), source.getInputLength(), source.getOutputLength(), source.getOutputLength(),
                source.getLossLength(), "m", source.getStartTime(), source.getEndTime(), source.getRecorderName(), source.getRecorderTime(),
                source.getConfirmerName(), source.getConfirmerTime(), source.getRemark(), source);
        row.setOutputPostStatus(source.getOutputStockPostStatus());
        row.setInputLength(source.getInputLength());
        row.setStartPosition(source.getStartPosition());
        row.setEndPosition(source.getEndPosition());
        row.setOutputLength(source.getOutputLength());
        row.setLossLength(source.getLossLength());
        row.setSelfCheck(source.getSelfCheck());
        row.setDefectCode(source.getDefectCode());
        row.setProductQualityStatus(source.getProductQualityStatus());
        row.setQualityLockReason(source.getQualityLockReason());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toPressSlotReport(HcPressSlotReportDO source) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "PRESS_SLOT_REPORT", "mes_sfc_press_slot_report",
                "DETAIL", source.getReportStatus(), source.getSourceBatchNo(), source.getProductionBatchNo(),
                source.getParentProductionBatchNo(), source.getInputLength(), source.getOutputLength(), source.getOutputLength(),
                source.getLossLength(), "m", source.getStartTime(), source.getEndTime(), source.getRecorderName(), source.getRecorderTime(),
                source.getConfirmerName(), source.getConfirmerTime(), source.getRemark(), source);
        row.setOutputPostStatus(source.getOutputStockPostStatus());
        row.setInputLength(source.getInputLength());
        row.setStartPosition(source.getStartPosition());
        row.setEndPosition(source.getEndPosition());
        row.setOutputLength(source.getOutputLength());
        row.setLossLength(source.getLossLength());
        row.setSelfCheck(source.getSelfCheck());
        row.setDefectCode(source.getDefectCode());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toCutRoundReport(HcCutRoundReportDO source) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "CUT_ROUND_REPORT", "mes_sfc_cut_round_report",
                "DETAIL", source.getReportStatus(), source.getSourceBatchNo(), source.getProductionBatchNo(),
                source.getParentProductionBatchNo(), source.getInputLength(), source.getOutputLength(), source.getOutputLength(),
                null, "m", source.getStartTime(), source.getEndTime(), source.getRecorderName(), source.getRecorderTime(),
                source.getConfirmerName(), source.getConfirmerTime(), source.getRemark(), source);
        row.setOutputPostStatus(source.getOutputStockPostStatus());
        row.setInputLength(source.getInputLength());
        row.setOutputLength(source.getOutputLength());
        row.setSelfCheck(source.getSelfCheck());
        row.setDefectCode(source.getDefectCode());
        row.setProductQualityStatus(source.getQualityRiskFlag());
        row.setInspectionStatus(source.getInspectionStatus());
        row.setInspectionResult(source.getInspectionResult());
        row.setInspectorName(source.getInspectorName());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord toPackReport(HcPackReportDO source) {
        BigDecimal qty = decimal(source.getInboundPieceCount(), source.getOuterPieceCount(), source.getInnerPieceCount());
        HcProcessReportOverviewDetailRespVO.ReportRecord row = baseReportRecord(
                source.getId(), source.getPlanOperationId(), "PACKAGING_REPORT", "mes_sfc_pack_report",
                "PACKAGING", source.getReportStatus(), source.getBatchNo(), source.getProductionBatchNo(), null,
                qty, qty, qty, null, "片", source.getRecorderTime(), source.getRecorderTime(), source.getRecorderName(), source.getRecorderTime(),
                source.getConfirmerName(), source.getConfirmerTime(), source.getRemark(), source);
        row.setInnerUnitCount(source.getInnerUnitCount());
        row.setInnerPieceCount(source.getInnerPieceCount());
        row.setOuterBoxCount(source.getOuterBoxCount());
        row.setOuterPieceCount(source.getOuterPieceCount());
        row.setInboundPieceCount(source.getInboundPieceCount());
        return row;
    }

    private HcProcessReportOverviewDetailRespVO.ReportRecord baseReportRecord(
            Long id, Long planOperationId, String sourceType, String sourceTable, String reportType, String reportStatus,
            String batchNo, String productionBatchNo, String parentProductionBatchNo, BigDecimal inputQty,
            BigDecimal reportQty, BigDecimal goodQty, BigDecimal scrapQty, String reportUom, LocalDateTime startTime,
            LocalDateTime endTime, String recorderName, LocalDateTime recorderTime, String confirmerName,
            LocalDateTime confirmerTime, String remark, Object source) {
        HcProcessReportOverviewDetailRespVO.ReportRecord row = new HcProcessReportOverviewDetailRespVO.ReportRecord();
        row.setId(id);
        row.setPlanOperationId(planOperationId);
        row.setSourceType(sourceType);
        row.setSourceTable(sourceTable);
        row.setReportType(reportType);
        row.setReportStatus(reportStatus);
        row.setOperationStatus(reportStatus);
        row.setBatchNo(batchNo);
        row.setProductionBatchNo(productionBatchNo);
        row.setParentProductionBatchNo(parentProductionBatchNo);
        row.setFeedQty(inputQty);
        row.setReportQty(reportQty);
        row.setGoodQty(goodQty);
        row.setScrapQty(scrapQty);
        row.setOutputQty(goodQty);
        row.setReportUom(reportUom);
        row.setRecorderName(recorderName);
        row.setConfirmerName(confirmerName);
        row.setStartTime(startTime);
        row.setEndTime(endTime);
        row.setReportTime(firstNotNull(endTime, startTime, recorderTime));
        row.setRemark(remark);
        row.setDetailJson(JSONUtil.toJsonStr(source));
        if (sourceType.endsWith("_PRODUCTION_RECORD")) {
            Map<String, Object> productionData = BeanUtil.beanToMap(source);
            productionData.replaceAll((key, value) -> {
                if (value instanceof LocalDateTime time) {
                    return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                return value instanceof LocalDate date ? date.toString() : value;
            });
            row.setProductionData(productionData);
        }
        return row;
    }

    private void setProductionIdentity(HcProcessReportOverviewDetailRespVO.ReportRecord row, LocalDate reportDate,
                                       String modelCode, String padType, String materialCode) {
        row.setReportDate(reportDate);
        row.setModelCode(modelCode);
        row.setPadType(padType);
        row.setMaterialCode(materialCode);
    }

    /**
     * 为每道工序选择唯一的统计事实。通用 START/END 行仍保留在明细中，不能与专用事实重复计量。
     */
    private List<HcProcessReportOverviewDetailRespVO.ReportRecord> selectSummaryReports(
            HcPlanOrderOperationDO operation,
            List<HcProcessReportOverviewDetailRespVO.ReportRecord> rows) {
        String operationText = (firstNotBlank(operation.getOpCode(), operation.getOpName()) + "").toUpperCase();
        List<String> preferredSources;
        if (operationText.contains("GRIND") || operationText.contains("磨皮")) {
            preferredSources = List.of("GRINDING_PRODUCTION_RECORD", "GRINDING_REPORT");
        } else if (operationText.contains("WET") || operationText.contains("湿法")) {
            preferredSources = List.of("WET_PRODUCTION_RECORD", "OPERATION_REPORT");
        } else if (operationText.contains("ADH1") || operationText.contains("ADHESIVE1") || operationText.contains("粘胶1")) {
            preferredSources = List.of("ADHESIVE1_REPORT");
        } else if (operationText.contains("ADH2") || operationText.contains("ADHESIVE2") || operationText.contains("粘胶2")) {
            preferredSources = List.of("ADHESIVE2_REPORT");
        } else if (operationText.contains("PRESS") || operationText.contains("GROOVE") || operationText.contains("压槽")) {
            preferredSources = List.of("SLITTING_PRESS_PRODUCTION_RECORD", "PRESS_SLOT_REPORT");
        } else if (operationText.contains("CUT") || operationText.contains("裁切")) {
            preferredSources = List.of("CUT_ROUND_PRODUCTION_RECORD", "CUT_ROUND_REPORT");
        } else if (operationText.contains("SLIT") || operationText.contains("分切")) {
            preferredSources = List.of("SLITTING_SLICE", "SLITTING_PRESS_PRODUCTION_RECORD");
        } else {
            preferredSources = List.of("OPERATION_REPORT");
        }
        for (String source : preferredSources) {
            List<HcProcessReportOverviewDetailRespVO.ReportRecord> selected = rows.stream()
                    .filter(item -> source.equals(item.getSourceType()))
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        return rows.stream().filter(item -> "OPERATION_REPORT".equals(item.getSourceType())).toList();
    }

    private boolean isGrindingOperation(HcPlanOrderOperationDO operation) {
        String value = firstNotBlank(operation.getOpCode(), operation.getOpName());
        return value != null && (value.toUpperCase().contains("GRIND") || value.contains("磨皮"));
    }

    private boolean includeProcessForm(HcProcessFormRecordDO form) {
        return form != null && !isDailyForm(null, firstNotBlank(form.getFormType(), form.getFormTypeName()),
                form.getTemplateCode(), firstNotBlank(form.getTemplateName(), form.getFormTypeName()));
    }

    private boolean includeStationForm(HcStationRecordDO form) {
        return form != null && !isDailyForm(form.getRecordScope(), form.getFormCode(), form.getFormCode(), form.getFormName());
    }

    /** 每日开机、清洁和保养不属于本报表的生产相关表单。 */
    private boolean isDailyForm(String recordScope, String formType, String formCode, String formName) {
        if ("EQUIPMENT_DAILY".equalsIgnoreCase(recordScope)) {
            return true;
        }
        String type = StrUtil.blankToDefault(formType, "").toUpperCase();
        if (Set.of("STARTUP_CHECK", "CLEANING_CHECK", "MAINTENANCE_CHECK").contains(type)) {
            return true;
        }
        String code = StrUtil.blankToDefault(formCode, "").toUpperCase();
        if (code.contains("STARTUP") || code.contains("CLEANING") || code.contains("MAINTENANCE")) {
            return true;
        }
        String text = (StrUtil.blankToDefault(formCode, "") + " " + StrUtil.blankToDefault(formName, ""));
        return text.contains("清洁") || text.contains("保养") || text.contains("开机点检");
    }

    private boolean hasProcessMirror(HcStationRecordDO station, Map<Long, HcProcessFormRecordDO> processFormsById) {
        return station.getSourceProcessFormRecordId() != null
                && processFormsById.containsKey(station.getSourceProcessFormRecordId());
    }

    private boolean isConfirmed(HcProcessReportOverviewDetailRespVO.FormRecord row) {
        return "CONFIRMED".equalsIgnoreCase(firstNotBlank(row.getRecordStatus(), row.getDocStatus()));
    }

    private boolean isAbnormal(HcProcessReportOverviewDetailRespVO.FormRecord row) {
        String result = firstNotBlank(row.getResultStatus(), row.getInspectionResult());
        return "NG".equalsIgnoreCase(result) || "FAIL".equalsIgnoreCase(result)
                || row.getItems().stream().anyMatch(item -> "NG".equalsIgnoreCase(item.getResultFlag())
                || "FAIL".equalsIgnoreCase(item.getResultFlag()));
    }

    private LocalDateTime reportTime(HcProcessReportDO report) {
        return firstNotNull(report.getEndTime(), report.getStartTime(), report.getRecorderTime(), report.getCreateTime());
    }

    private BigDecimal sumReportRows(List<HcProcessReportOverviewDetailRespVO.ReportRecord> reports,
                                     Function<HcProcessReportOverviewDetailRespVO.ReportRecord, BigDecimal> getter) {
        return reports.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal add(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                result = result.add(value);
            }
        }
        return result;
    }

    private BigDecimal decimal(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return BigDecimal.valueOf(value);
            }
        }
        return null;
    }

    private String operationStatusText(String status) {
        return switch (StrUtil.blankToDefault(status, "").toUpperCase()) {
            case "RELEASED" -> "待开工";
            case "RUNNING" -> "进行中";
            case "FINISHED", "COMPLETED" -> "已完成";
            case "PAUSED" -> "已暂停";
            case "CANCELLED", "CANCELED" -> "已取消";
            default -> StrUtil.blankToDefault(status, "未开始");
        };
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private LocalDateTime firstNotNull(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
