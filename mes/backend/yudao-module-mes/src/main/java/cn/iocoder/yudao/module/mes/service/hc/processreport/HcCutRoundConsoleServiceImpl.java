package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveCheckItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSegmentCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundInspectionTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundInspectionTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcVisualAbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundCheckDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotChangeoverInspectionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundCheckDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotChangeoverInspectionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.service.hc.equipment.HcEquipmentService;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.dto.HcWipOutputPostReq;
import cn.iocoder.yudao.module.mes.service.qms.QmsCutRoundFqcService;
import cn.iocoder.yudao.module.mes.service.qms.QmsFqcService;
import cn.iocoder.yudao.module.mes.service.hc.qtimeconfig.HcQtimeEvaluationService;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionServiceImpl;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionKeyUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcCutRoundConsoleServiceImpl implements HcCutRoundConsoleService {

    private static final String PROCESS_CODE = "CUT_ROUND";
    private static final String SOURCE_MENU_CODE = "CUT_ROUND_REPORT";
    private static final String SOURCE_MENU_CODE_SEGMENT_COMPLETE = "CUT_ROUND_SEGMENT_COMPLETE";
    private static final String FORM_TYPE_STARTUP_CHECK = "STARTUP_CHECK";
    private static final String FORM_TYPE_CLEANING_CHECK = "CLEANING_CHECK";
    private static final String FORM_TYPE_PRODUCTION_CHECK = "PRODUCTION_CHECK";
    private static final String CUT_ROUND_PROCESS_FORM_RECORD_PREFIX = "CUT_ROUND_PRODUCTION_CHECK_";
    private static final String PLAN_LOCK_TYPE_WIP = "WIP";
    private static final String PLAN_LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String PLAN_LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String PLAN_LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String PLAN_LOCK_STATUS_RELEASED = "RELEASED";
    private static final String STOCK_POST_STATUS_POSTED = "POSTED";
    private static final String SOURCE_TYPE_ADHESIVE2 = "ADHESIVE2";
    private static final String SOURCE_TYPE_CUT_ROUND = "CUT_ROUND";
    private static final String SOURCE_TABLE_ADHESIVE2_REPORT = "mes_sfc_adhesive2_report";
    private static final String SOURCE_TABLE_CUT_ROUND_REPORT = "mes_sfc_cut_round_report";
    private static final String SOURCE_MENU_CODE_ADHESIVE2 = "ADHESIVE2_REPORT";
    private static final String SOURCE_MENU_CODE_ADHESIVE2_SEGMENT_COMPLETE = "ADHESIVE2_SEGMENT_COMPLETE";
    private static final String SOURCE_MENU_CODE_PRESS_SLOT = "PRESS_SLOT_REPORT";
    private static final String SOURCE_MENU_CODE_SLITTING = "SLITTING_REPORT";
    private static final String PROCESS_CODE_ADHESIVE2 = "ADHESIVE2";
    private static final String FAI_STATUS_COMPLETED = "COMPLETED";
    private static final String FAI_STATUS_CANCELED = "CANCELED";
    private static final String FAI_JUDGMENT_OK = "OK";
    private static final String PRODUCT_QUALITY_ABNORMAL = "QUALITY_ABNORMAL";
    private static final String QUALITY_STATUS_NORMAL = "NORMAL";
    private static final String CHANGEOVER_STATUS_RECORDED = "RECORDED";
    private static final String CHANGEOVER_STATUS_CONFIRMED = "CONFIRMED";
    private static final String OP_STATUS_RELEASED = "RELEASED";
    private static final String OP_STATUS_RUNNING = "RUNNING";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String OP_STATUS_PAUSED = "PAUSED";
    private static final String OP_STATUS_CANCELLED = "CANCELLED";
    private static final String TASK_STATUS_ALL = "ALL";
    private static final String TASK_STATUS_UNFINISHED = "UNFINISHED";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_STATUS_COMPLETED = "COMPLETED";
    private static final String TASK_STATUS_PAUSED = "PAUSED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String UPSTREAM_ADHESIVE2_OP_CODE = "WC-ADH2";
    private static final String REPORT_TYPE_START = "START";
    private static final String REPORT_TYPE_EQUIPMENT_SWITCH = "EQUIPMENT_SWITCH";
    private static final String REPORT_TYPE_END = "END";
    private static final String PAD_TYPE_WHITE = "WHITE_PAD";
    private static final String PAD_TYPE_BLACK = "BLACK_PAD";
    private static final String PAD_TYPE_COMMON = "COMMON";
    private static final String REPORT_TYPE_SEGMENT_COMPLETE = "SEGMENT_COMPLETE";
    private static final String CONSUMABLE_BLADE = "CUTTING_BLADE";
    private static final String CONSUMABLE_FELT = "CUTTING_FELT";
    private static final String EVENT_CONFIG = "CONFIG";
    private static final String EVENT_REPLACE = "REPLACE";
    private static final String EVENT_USE = "USE";
    private static final String EVENT_RND_MANUAL_USE = "RND_MANUAL_USE";
    private static final String RECORD_SOURCE_RND_MANUAL = "RND_MANUAL";
    private static final String DISPLAY_SOURCE_REPORT = "量产报工";
    private static final String DISPLAY_SOURCE_RND_MANUAL = "研发手工备件登记";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final List<String> VISUAL_CATEGORY_NAMES = List.of(
            "黑点", "蓝点", "黄点", "红点", "针孔", "条纹", "褶皱", "波浪纹", "其他");
    private static final String FQC_STATUS_PENDING = "PENDING";
    private static final String FQC_STATUS_COMPLETED = "COMPLETED";
    private static final String FQC_STATUS_REJECTED = "REJECTED";
    private static final String FQC_JUDGMENT_PENDING = "PENDING";
    private static final String FQC_JUDGMENT_OK = "OK";
    private static final String FQC_JUDGMENT_NG = "NG";
    private static final String INSPECTION_STATUS_INSPECTING = "INSPECTING";
    private static final String INSPECTION_TASK_STATUS_INSPECTING = "INSPECTING";
    /** 仅作过程风险追溯，不能替代 FQC/COA 的最终质量结论。 */
    private static final String QUALITY_RISK_NONE = "NONE";
    private static final String QUALITY_RISK_ADHESIVE2_NG = "ADHESIVE2_NG";
    private static final String QUALITY_RISK_CUT_ROUND_NG = "CUT_ROUND_NG";
    private static final String QUALITY_RISK_BOTH_NG = "BOTH_NG";
    private static final double CONSUMABLE_WARNING_RATIO = 0.9D;

    @Resource
    private HcUpstreamSampleLockService upstreamSampleLockService;

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;
    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcProcessReportMapper hcProcessReportMapper;
    @Resource
    private HcProcessFormRecordMapper hcProcessFormRecordMapper;
    @Resource
    private HcProcessFormRecordItemMapper hcProcessFormRecordItemMapper;
    @Resource
    private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private HcQtimeEvaluationService hcQtimeEvaluationService;
    @Resource
    private HcProductionRecordRevisionService productionRecordRevisionService;
    @Resource
    private HcProductionRecordPadTypeResolver productionRecordPadTypeResolver;
    @Resource
    private HcCutRoundCheckDetailMapper hcCutRoundCheckDetailMapper;
    @Resource
    private HcCutRoundInspectionTaskMapper hcCutRoundInspectionTaskMapper;
    @Resource
    private HcCutRoundInspectionDetailMapper hcCutRoundInspectionDetailMapper;
    @Resource
    private HcCutRoundSpareMapper hcCutRoundSpareMapper;
    @Resource
    private HcCutRoundSpareRecordMapper hcCutRoundSpareRecordMapper;
    @Resource
    private HcCutRoundBladeConsumptionService bladeConsumptionService;
    @Resource
    private HcStationFormMapper hcStationFormMapper;
    @Resource
    private HcStationFormItemMapper hcStationFormItemMapper;
    @Resource
    private HcProductModelMapper hcProductModelMapper;
    @Resource
    private HcStationRecordMapper hcStationRecordMapper;
    @Resource
    private HcStationRecordItemMapper hcStationRecordItemMapper;
    @Resource
    private HcEquipmentConsumableStateMapper hcEquipmentConsumableStateMapper;
    @Resource
    private HcEquipmentConsumableEventMapper hcEquipmentConsumableEventMapper;
    @Resource
    private HcAdhesiveGlueBoardStockMapper hcAdhesiveGlueBoardStockMapper;
    @Resource
    private HcPressSlotChangeoverInspectionMapper hcPressSlotChangeoverInspectionMapper;
    @Resource
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Resource
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Resource
    private HcEquipmentService hcEquipmentService;
    @Resource
    private HcInvStockService hcInvStockService;
    @Resource
    private QmsFqcService qmsFqcService;
    @Resource
    private QmsCutRoundFqcService qmsCutRoundFqcService;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;

    @Override
    public List<HcAdhesiveReportTaskRespVO> getTaskList(HcAdhesiveReportTaskPageReqVO reqVO) {
        String taskStatus = normalizeCutRoundTaskStatus(reqVO.getTaskStatus());
        List<HcAdhesiveReportTaskRespVO> tasks = hcProcessReportMapper.selectCutRoundTaskList(
                TASK_STATUS_ALL, reqVO.getTaskKeyword(), reqVO.getProductKeyword(),
                reqVO.getMotherMaterialKeyword(), reqVO.getMotherModelKeyword(), reqVO.getProductionDate());
        List<HcAdhesiveReportTaskRespVO> segmentRows = buildCutRoundTaskSegmentRows(tasks);
        HcEquipmentDO equipment = resolveCutRoundTaskListEquipment(reqVO.getEquipmentId());
        return filterCutRoundTaskRowsByStatus(filterCutRoundTaskRowsByEquipment(segmentRows, equipment), taskStatus);
    }

    private HcEquipmentDO resolveCutRoundTaskListEquipment(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        HcEquipmentDO equipment = hcEquipmentService.getHcEquipment(equipmentId);
        validateCutRoundEquipmentBase(equipment, null);
        return equipment;
    }

    private List<HcAdhesiveReportTaskRespVO> filterCutRoundTaskRowsByEquipment(
            List<HcAdhesiveReportTaskRespVO> rows, HcEquipmentDO equipment) {
        if (equipment == null || rows == null || rows.isEmpty()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        String equipmentPadType = normalizeCutRoundPadType(equipment.getApplicablePadType());
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> isCutRoundTaskPadTypeMatched(row, equipmentPadType))
                .filter(row -> isCutRoundTaskEquipmentMatched(row, equipment.getId()))
                .toList();
    }

    private boolean isCutRoundTaskPadTypeMatched(HcAdhesiveReportTaskRespVO row, String equipmentPadType) {
        if (PAD_TYPE_COMMON.equals(equipmentPadType)) {
            return true;
        }
        String taskPadType = normalizeCutRoundPadType(row.getCategoryCode());
        if (StrUtil.isBlank(taskPadType)) {
            taskPadType = resolveCutRoundMaterialType(normalizeCutRoundModelCode(row.getModelCode()));
        }
        return equipmentPadType.equals(taskPadType);
    }

    private boolean isCutRoundTaskEquipmentMatched(HcAdhesiveReportTaskRespVO row, Long equipmentId) {
        String rowStatus = normalizeCutRoundTaskStatus(row.getStatus());
        if (TASK_STATUS_PENDING.equals(rowStatus) && row.getEquipmentId() == null) {
            return true;
        }
        return Objects.equals(equipmentId, row.getEquipmentId());
    }

    private String normalizeCutRoundTaskStatus(String taskStatus) {
        String status = StrUtil.trimToEmpty(taskStatus).toUpperCase();
        if (StrUtil.isBlank(status) || TASK_STATUS_ALL.equals(status)) {
            return TASK_STATUS_ALL;
        }
        if (TASK_STATUS_UNFINISHED.equals(status)) {
            return TASK_STATUS_UNFINISHED;
        }
        if (TASK_STATUS_PENDING.equals(status) || OP_STATUS_RELEASED.equals(status)) {
            return TASK_STATUS_PENDING;
        }
        if (TASK_STATUS_IN_PROGRESS.equals(status) || OP_STATUS_RUNNING.equals(status) || "RUNNING".equals(status) || "PROCESSING".equals(status)) {
            return TASK_STATUS_IN_PROGRESS;
        }
        if (TASK_STATUS_COMPLETED.equals(status) || OP_STATUS_FINISHED.equals(status) || "FINISHED".equals(status) || "DONE".equals(status)) {
            return TASK_STATUS_COMPLETED;
        }
        if (TASK_STATUS_PAUSED.equals(status) || OP_STATUS_PAUSED.equals(status)) {
            return TASK_STATUS_PAUSED;
        }
        if (TASK_STATUS_CANCELLED.equals(status) || OP_STATUS_CANCELLED.equals(status) || "CANCELED".equals(status)) {
            return TASK_STATUS_CANCELLED;
        }
        return TASK_STATUS_ALL;
    }

    private List<HcAdhesiveReportTaskRespVO> filterCutRoundTaskRowsByStatus(List<HcAdhesiveReportTaskRespVO> rows,
                                                                            String taskStatus) {
        if (rows == null || rows.isEmpty() || TASK_STATUS_ALL.equals(taskStatus)) {
            return rows == null ? Collections.emptyList() : rows;
        }
        return rows.stream()
                .filter(row -> matchesCutRoundTaskStatus(row, taskStatus))
                .toList();
    }

    private boolean matchesCutRoundTaskStatus(HcAdhesiveReportTaskRespVO row, String taskStatus) {
        String rowStatus = normalizeCutRoundTaskStatus(row == null ? null : row.getStatus());
        if (TASK_STATUS_UNFINISHED.equals(taskStatus)) {
            return !List.of(TASK_STATUS_COMPLETED, TASK_STATUS_CANCELLED).contains(rowStatus);
        }
        return taskStatus.equals(rowStatus);
    }

    private List<HcAdhesiveReportTaskRespVO> buildCutRoundTaskSegmentRows(List<HcAdhesiveReportTaskRespVO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<HcCutRoundReportDO>> reportsByOperationId = new LinkedHashMap<>();
        for (HcAdhesiveReportTaskRespVO task : tasks) {
            Long planOperationId = task.getPlanOperationId();
            if (planOperationId == null) {
                continue;
            }
            List<HcCutRoundReportDO> reports = reportsByOperationId.computeIfAbsent(
                    planOperationId, hcCutRoundReportMapper::selectListByPlanOperationId);
            applyCutRoundSegmentTaskStatus(task, reports);
        }
        return tasks;
    }

    private void applyCutRoundSegmentTaskStatus(HcAdhesiveReportTaskRespVO row,
                                                List<HcCutRoundReportDO> cutRoundReports) {
        if (row == null) {
            return;
        }
        String rowStatus = normalizeCutRoundTaskStatus(row.getStatus());
        if (TASK_STATUS_COMPLETED.equals(rowStatus)
                || TASK_STATUS_PAUSED.equals(rowStatus)
                || TASK_STATUS_CANCELLED.equals(rowStatus)) {
            return;
        }
        String segmentBatchNo = normalizeCutRoundMotherSegmentBatchNo(firstNotBlank(row.getSourceBatchNo(),
                row.getSourceProductionBatchNo(), row.getParentProductionBatchNo(),
                row.getProductionBatchNo(), row.getBatchNo()));
        if (StrUtil.isBlank(segmentBatchNo)) {
            return;
        }
        List<HcCutRoundReportDO> segmentReports = filterCutRoundReportsByMotherBatchNo(
                cutRoundReports == null ? Collections.emptyList() : cutRoundReports, segmentBatchNo);
        row.setAvailableSourceLength(BigDecimal.valueOf(calculateCutRoundUnconfirmedSourceCount(row, segmentReports)));
        if (segmentReports.isEmpty()) {
            return;
        }
        boolean allSubmitted = segmentReports.stream()
                .allMatch(report -> "SUBMITTED".equalsIgnoreCase(StrUtil.blankToDefault(report.getReportStatus(), "")));
        if (!allSubmitted) {
            row.setStatus(TASK_STATUS_IN_PROGRESS);
            return;
        }
        HcCutRoundReportDO latestReport = segmentReports.get(0);
        row.setAvailableSourceLength(BigDecimal.ZERO);
        row.setStatus(TASK_STATUS_COMPLETED);
        row.setEndTime(latestReport.getEndTime());
        row.setRecorderName(firstNotBlank(latestReport.getRecorderName(), row.getRecorderName()));
        row.setRecorderTime(latestReport.getRecorderTime() == null ? row.getRecorderTime() : latestReport.getRecorderTime());
        row.setConfirmerName(firstNotBlank(latestReport.getConfirmerName(), row.getConfirmerName()));
        row.setConfirmerTime(latestReport.getConfirmerTime() == null ? row.getConfirmerTime() : latestReport.getConfirmerTime());
    }

    private int calculateCutRoundUnconfirmedSourceCount(HcAdhesiveReportTaskRespVO row,
                                                        List<HcCutRoundReportDO> segmentReports) {
        int sourceCount = row.getConfirmedSourceCount() == null ? 0 : row.getConfirmedSourceCount();
        if (sourceCount <= 0) {
            sourceCount = Math.max(zeroIfNull(row.getAvailableSourceLength()).intValue(), 0);
        }
        long confirmedCount = (segmentReports == null ? Collections.<HcCutRoundReportDO>emptyList() : segmentReports)
                .stream()
                .filter(Objects::nonNull)
                .filter(report -> isCutRoundReportConfirmedOrSubmitted(report.getReportStatus()))
                .map(this::buildCutRoundReportSourceKey)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .count();
        return Math.max(sourceCount - (int) Math.min(confirmedCount, Integer.MAX_VALUE), 0);
    }

    private boolean isCutRoundReportConfirmedOrSubmitted(String reportStatus) {
        return isConfirmedOrSubmittedStatus(reportStatus);
    }

    private boolean isConfirmedOrSubmittedStatus(String reportStatus) {
        String status = StrUtil.blankToDefault(reportStatus, "").toUpperCase();
        return "CONFIRMED".equals(status) || "SUBMITTED".equals(status);
    }

    private String buildCutRoundReportSourceKey(HcCutRoundReportDO report) {
        if (report == null) {
            return "";
        }
        if (report.getSourceAdhesive2ReportId() != null) {
            return "ADHESIVE2:" + report.getSourceAdhesive2ReportId();
        }
        return firstNotBlank(
                stripCutRoundSizeSuffix(report.getSourceProductionBatchNo()),
                stripCutRoundSizeSuffix(report.getProductionBatchNo()),
                stripCutRoundSizeSuffix(report.getSourceBatchNo()),
                stripCutRoundSizeSuffix(report.getParentProductionBatchNo()));
    }

    private HcEquipmentDO validateCutRoundEquipment(HcPlanOrderDO planOrder,
                                                    HcPlanOrderOperationDO operation,
                                                    Long equipmentId) {
        HcEquipmentDO equipment = hcEquipmentService.getHcEquipment(equipmentId);
        validateCutRoundEquipmentBase(equipment, operation);
        String planPadType = resolveCutRoundPlanPadType(planOrder);
        String equipmentPadType = normalizeCutRoundPadType(equipment.getApplicablePadType());
        if (!PAD_TYPE_COMMON.equals(equipmentPadType) && !equipmentPadType.equals(planPadType)) {
            throw invalidParamException(String.format("型号 %s 属于%s，不能使用%s设备 %s / %s",
                    firstNotBlank(planOrder.getModelCode(), planOrder.getModelName(), "-"),
                    resolveCutRoundPadTypeName(planPadType), resolveCutRoundPadTypeName(equipmentPadType),
                    firstNotBlank(equipment.getEquipmentCode(), "-"),
                    firstNotBlank(equipment.getEquipmentName(), "-")));
        }
        return equipment;
    }

    private void validateCutRoundEquipmentBase(HcEquipmentDO equipment, HcPlanOrderOperationDO operation) {
        if (equipment == null) {
            throw invalidParamException("所选裁切设备不存在");
        }
        if (!Integer.valueOf(0).equals(equipment.getStatus())) {
            throw invalidParamException(String.format("设备 %s / %s 已停用，不能用于裁切",
                    firstNotBlank(equipment.getEquipmentCode(), "-"),
                    firstNotBlank(equipment.getEquipmentName(), "-")));
        }
        if (operation != null && operation.getWorkCenterId() != null && equipment.getWorkCenterId() != null
                && !Objects.equals(operation.getWorkCenterId(), equipment.getWorkCenterId())) {
            throw invalidParamException(String.format("设备 %s / %s 不属于当前裁切工作中心",
                    firstNotBlank(equipment.getEquipmentCode(), "-"),
                    firstNotBlank(equipment.getEquipmentName(), "-")));
        }
        String equipmentPadType = normalizeCutRoundPadType(equipment.getApplicablePadType());
        if (!List.of(PAD_TYPE_WHITE, PAD_TYPE_BLACK, PAD_TYPE_COMMON).contains(equipmentPadType)) {
            throw invalidParamException(String.format("设备 %s / %s 未维护有效适用垫型，请先维护设备台账",
                    firstNotBlank(equipment.getEquipmentCode(), "-"),
                    firstNotBlank(equipment.getEquipmentName(), "-")));
        }
    }

    private String resolveCutRoundPlanPadType(HcPlanOrderDO planOrder) {
        String categoryCode = normalizeCutRoundPadType(planOrder == null ? null : planOrder.getCategoryCode());
        if (List.of(PAD_TYPE_WHITE, PAD_TYPE_BLACK).contains(categoryCode)) {
            return categoryCode;
        }
        String modelCode = normalizeCutRoundModelCode(planOrder == null ? null
                : firstNotBlank(planOrder.getModelCode(), planOrder.getModelName()));
        String resolved = resolveCutRoundMaterialType(modelCode);
        if (!List.of(PAD_TYPE_WHITE, PAD_TYPE_BLACK).contains(resolved)) {
            throw invalidParamException(String.format("计划 %s 未维护有效垫型，不能分配裁切设备",
                    planOrder == null ? "-" : firstNotBlank(planOrder.getPlanNo(), "-")));
        }
        return resolved;
    }

    private String normalizeCutRoundPadType(String padType) {
        return StrUtil.trimToEmpty(padType).toUpperCase();
    }

    private String resolveCutRoundPadTypeName(String padType) {
        return switch (normalizeCutRoundPadType(padType)) {
            case PAD_TYPE_WHITE -> "白垫";
            case PAD_TYPE_BLACK -> "黑垫";
            case PAD_TYPE_COMMON -> "通用";
            default -> "未配置垫型";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(HcAdhesiveReportStartReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectByIdForUpdate(reqVO.getPlanId());
        if (planOrder == null || !"RELEASED".equals(planOrder.getPlanStatus())) {
            throw invalidParamException("只有已下发计划允许开工，计划可能已撤回，请刷新任务");
        }
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        if (OP_STATUS_FINISHED.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前裁切工序已完工，不能再开工");
        }
        if (OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前裁切工序已开工");
        }
        LocalDate reportDate = reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = normalizeReportDateTime(reqVO.getStartTime(), reportDate);
        LocalDateTime recorderTime = normalizeReportDateTime(reqVO.getRecorderTime(), reportDate);
        if (startTime == null) {
            startTime = now;
        }
        if (recorderTime == null) {
            recorderTime = now;
        }
        HcEquipmentDO equipment = validateCutRoundEquipment(planOrder, operation, reqVO.getEquipmentId());
        String operatorName = firstNotBlank(reqVO.getRecorderName(),
                SecurityFrameworkUtils.getLoginUserNickname(), "系统");
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
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .materialId(planOrder.getMaterialId())
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .motherMaterialId(planOrder.getMotherMaterialId())
                .motherMaterialCode(firstNotBlank(operation.getMotherMaterialCode(), planOrder.getMotherMaterialCode()))
                .motherMaterialName(firstNotBlank(operation.getMotherMaterialName(), planOrder.getMotherMaterialName()))
                .motherModelId(planOrder.getMotherModelId())
                .motherModelCode(firstNotBlank(operation.getMotherModelCode(), planOrder.getMotherModelCode()))
                .motherModelName(firstNotBlank(operation.getMotherModelName(), planOrder.getMotherModelName()))
                .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                .productionBatchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo()))
                .parentProductionBatchNo(firstNotBlank(operation.getParentProductionBatchNo(), planOrder.getParentProductionBatchNo(), planOrder.getProductionBatchNo()))
                .reportDate(reportDate)
                .startTime(startTime)
                .goodQty(BigDecimal.ZERO)
                .scrapQty(BigDecimal.ZERO)
                .recorderName(operatorName)
                .recorderTime(recorderTime)
                .reportUom(firstNotBlank(operation.getUom(), operation.getUnitCode(), operation.getUnitName(), "片"))
                .reportType(REPORT_TYPE_START)
                .sourceMenuCode(SOURCE_MENU_CODE)
                .build();
        hcProcessReportMapper.insert(reportDO);
        HcPlanOrderOperationDO updateObj = new HcPlanOrderOperationDO();
        updateObj.setId(operation.getId());
        updateObj.setOperationStatus(OP_STATUS_RUNNING);
        updateObj.setEquipmentId(equipment.getId());
        updateObj.setEquipmentCode(equipment.getEquipmentCode());
        updateObj.setEquipmentName(equipment.getEquipmentName());
        updateObj.setStatusOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setStatusOperatorName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "system"));
        updateObj.setStatusOperateTime(now);
        hcPlanOrderOperationMapper.updateById(updateObj);
        // 与粘胶1一致：同一裁切设备可挂接多个生产中计划，设备台账展示最后一次开工计划。
        hcEquipmentService.occupyEquipment(equipment.getId(), planOrder.getPlanNo(), operation.getOpCode(),
                operation.getOpName(), startTime, operatorName, recorderTime);
        return reportDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchEquipment(HcAdhesiveReportSwitchEquipmentReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        if (!OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前裁切工序未开工，不能切换设备");
        }
        HcProcessReportDO latestReport = getLatestProcessReport(operation.getId());
        if (latestReport == null) {
            throw invalidParamException("当前裁切工序未找到有效开工记录，不能切换设备");
        }
        Long oldEquipmentId = firstNotNull(latestReport.getEquipmentId(), operation.getEquipmentId());
        String oldEquipmentCode = firstNotBlank(latestReport.getEquipmentCode(), operation.getEquipmentCode());
        String oldEquipmentName = firstNotBlank(latestReport.getEquipmentName(), operation.getEquipmentName());
        HcEquipmentDO newEquipment = validateCutRoundEquipment(planOrder, operation, reqVO.getEquipmentId());
        Long newEquipmentId = newEquipment.getId();
        String newEquipmentCode = newEquipment.getEquipmentCode();
        String newEquipmentName = newEquipment.getEquipmentName();

        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        LocalDateTime recordTime = LocalDateTime.now();
        LocalDateTime startTime = latestReport.getStartTime() == null ? recordTime : latestReport.getStartTime();
        if (!Objects.equals(oldEquipmentId, newEquipmentId)) {
            // 同机多计划模式下，新计划可覆盖设备台账中的当前展示计划。
            hcEquipmentService.occupyEquipment(newEquipmentId, planOrder.getPlanNo(), operation.getOpCode(),
                    operation.getOpName(), startTime, operatorName, recordTime);
            // 旧设备可能仍被其他计划使用，只在仍展示本计划时才清空。
            releaseCutRoundEquipmentIfOwnedByPlan(oldEquipmentId, planOrder.getPlanNo(), operatorName, recordTime);
        }

        if (!Objects.equals(oldEquipmentId, newEquipmentId)) {
            HcProcessReportDO switchReport = HcProcessReportDO.builder()
                    .tenantId(planOrder.getTenantId())
                    .planId(planOrder.getId())
                    .planNo(planOrder.getPlanNo())
                    .planOperationId(operation.getId())
                    .operationStatus(OP_STATUS_RUNNING)
                    .operationSeq(operation.getOpSeq())
                    .operationCode(operation.getOpCode())
                    .operationName(operation.getOpName())
                    .workCenterId(operation.getWorkCenterId())
                    .workCenterCode(operation.getWorkCenterCode())
                    .workCenterName(operation.getWorkCenterName())
                    .equipmentId(newEquipmentId)
                    .equipmentCode(newEquipmentCode)
                    .equipmentName(newEquipmentName)
                    .materialId(planOrder.getMaterialId())
                    .materialCode(planOrder.getMaterialCode())
                    .materialName(planOrder.getMaterialName())
                    .motherMaterialId(planOrder.getMotherMaterialId())
                    .motherMaterialCode(firstNotBlank(operation.getMotherMaterialCode(), planOrder.getMotherMaterialCode()))
                    .motherMaterialName(firstNotBlank(operation.getMotherMaterialName(), planOrder.getMotherMaterialName()))
                    .motherModelId(planOrder.getMotherModelId())
                    .motherModelCode(firstNotBlank(operation.getMotherModelCode(), planOrder.getMotherModelCode()))
                    .motherModelName(firstNotBlank(operation.getMotherModelName(), planOrder.getMotherModelName()))
                    .batchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo(), planOrder.getBatchNo()))
                    .productionBatchNo(firstNotBlank(operation.getProductionBatchNo(), planOrder.getProductionBatchNo()))
                    .parentProductionBatchNo(firstNotBlank(operation.getParentProductionBatchNo(),
                            planOrder.getParentProductionBatchNo(), planOrder.getProductionBatchNo()))
                    .reportDate(latestReport.getReportDate() == null ? LocalDate.now() : latestReport.getReportDate())
                    .startTime(startTime)
                    .goodQty(BigDecimal.ZERO)
                    .scrapQty(BigDecimal.ZERO)
                    .recorderName(operatorName)
                    .recorderTime(recordTime)
                    .reportUom(firstNotBlank(operation.getUom(), operation.getUnitCode(), operation.getUnitName(), "片"))
                    .reportType(REPORT_TYPE_EQUIPMENT_SWITCH)
                    .sourceMenuCode(SOURCE_MENU_CODE)
                    .remark(firstNotBlank(reqVO.getSwitchReason(), "裁切工单切换机台"))
                    .extraJson(buildCutRoundEquipmentSwitchExtra(oldEquipmentId, oldEquipmentCode,
                            oldEquipmentName, newEquipment, reqVO.getSwitchReason(), operatorName, recordTime))
                    .build();
            hcProcessReportMapper.insert(switchReport);
        }

        HcPlanOrderOperationDO updateOperation = new HcPlanOrderOperationDO();
        updateOperation.setId(operation.getId());
        updateOperation.setEquipmentId(newEquipmentId);
        updateOperation.setEquipmentCode(newEquipmentCode);
        updateOperation.setEquipmentName(newEquipmentName);
        updateOperation.setStatusOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateOperation.setStatusOperatorName(operatorName);
        updateOperation.setStatusOperateTime(recordTime);
        hcPlanOrderOperationMapper.updateById(updateOperation);
    }

    private void releaseCutRoundEquipmentIfOwnedByPlan(Long equipmentId, String planNo,
                                                        String operatorName, LocalDateTime recordTime) {
        if (equipmentId == null) {
            return;
        }
        HcEquipmentDO oldEquipment = hcEquipmentService.getHcEquipment(equipmentId);
        if (oldEquipment != null && Objects.equals(StrUtil.trimToEmpty(oldEquipment.getCurrentPlanNo()),
                StrUtil.trimToEmpty(planNo))) {
            hcEquipmentService.clearEquipmentBinding(equipmentId, "IDLE", operatorName, recordTime);
        }
    }

    private String buildCutRoundEquipmentSwitchExtra(Long oldEquipmentId, String oldEquipmentCode,
                                                     String oldEquipmentName, HcEquipmentDO newEquipment,
                                                     String switchReason, String operatorName,
                                                     LocalDateTime recordTime) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("oldEquipmentId", oldEquipmentId);
        extra.put("oldEquipmentCode", oldEquipmentCode);
        extra.put("oldEquipmentName", oldEquipmentName);
        extra.put("newEquipmentId", newEquipment.getId());
        extra.put("newEquipmentCode", newEquipment.getEquipmentCode());
        extra.put("newEquipmentName", newEquipment.getEquipmentName());
        extra.put("switchReason", StrUtil.trimToEmpty(switchReason));
        extra.put("operatorName", operatorName);
        extra.put("switchTime", recordTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return JsonUtils.toJsonString(extra);
    }

    @Override
    public List<HcWetPassWorkRespVO> getPassWorkList(Long planId, Long planOperationId,
                                                     Long requestEquipmentId, LocalDate requestRecordDate) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(planId);
        HcPlanOrderOperationDO operation = validateOperation(planOrder, planOperationId);
        Long equipmentId = requestEquipmentId == null ? operation.getEquipmentId() : requestEquipmentId;
        LocalDate recordDate = requestRecordDate == null ? LocalDate.now() : requestRecordDate;
        List<HcStationFormDO> forms = hcStationFormMapper.selectEnabledByProcess(PROCESS_CODE).stream()
                .filter(this::isCutRoundDailyStationForm)
                .toList();
        List<HcWetPassWorkRespVO> rows = new ArrayList<>();
        for (HcStationFormDO form : forms) {
            HcStationRecordDO record = hcStationRecordMapper.selectOneByEquipmentDaily(equipmentId, recordDate, form.getFormCode());
            rows.add(buildPassWorkResp(form, record));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePassWork(HcWetPassWorkSaveReqVO reqVO) {
        return upsertPassWork(reqVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmPassWork(HcWetPassWorkSaveReqVO reqVO) {
        return upsertPassWork(reqVO, true);
    }

    @Override
    public List<HcAdhesiveCheckItemRespVO> getCheckTemplate(String modelCode) {
        HcStationFormDO form = requireCutRoundProductionCheckForm(modelCode);
        return hcStationFormItemMapper.selectByFormId(form.getId()).stream()
                .map(this::buildCutRoundCheckTemplateResp)
                .toList();
    }

    @Override
    public HcAdhesiveSourceRespVO scanSource(String batchNo) {
        String normalized = StrUtil.trimToEmpty(batchNo);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("请扫描或输入粘胶2已确认片号");
        }
        HcAdhesive2ReportDO source = findCutRoundSourceByProductionBatchNo(normalized);
        if (source == null) {
            String sourceBatchNo = stripCutRoundSizeSuffix(normalized);
            if (StrUtil.isNotBlank(sourceBatchNo) && !sourceBatchNo.equalsIgnoreCase(normalized)) {
                source = findCutRoundSourceByProductionBatchNo(sourceBatchNo);
                if (source != null && hasAdhesive2TailSelection(source)) {
                    throw invalidParamException("请扫描粘胶2已选择尾号的流转单片号");
                }
            }
        }
        if (source == null) {
            throw invalidParamException("未找到可进入裁切的粘胶2已确认片或NG片号");
        }
        if (isCutRoundSourceFromPressSlotProcessCheck(source)) {
            throw invalidParamException("压槽过程加检片不进入粘胶2/裁切工序");
        }
        if (isAdhesive2SourceUnderCoaInspection(source)) {
            throw invalidParamException("该粘胶2片号已进行COA送检，暂不能进入裁切报工");
        }
        return buildSourceResp(source);
    }

    /**
     * 裁切来源优先使用已确认的粘胶2片；若不存在，则允许粘胶2自身NG片由裁切扫码确认。
     * 普通草稿片仍不能绕过粘胶2扫码确认。
     */
    private HcAdhesive2ReportDO findCutRoundSourceByProductionBatchNo(String productionBatchNo) {
        HcAdhesive2ReportDO confirmedSource = hcAdhesive2ReportMapper
                .selectConfirmedByProductionBatchNo(productionBatchNo);
        if (confirmedSource != null) {
            return confirmedSource;
        }
        HcAdhesive2ReportDO abnormalSource = hcAdhesive2ReportMapper
                .selectAbnormalByProductionBatchNo(productionBatchNo);
        return isAdhesive2SourceNg(abnormalSource) ? abnormalSource : null;
    }

    @Override
    public List<HcAdhesiveSourceRespVO> getSourceList(Long planId, Long planOperationId, String sourceBatchNo) {
        if (planId == null || planOperationId == null) {
            return Collections.emptyList();
        }
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(planId);
        validateOperation(planOrder, planOperationId);
        String targetSegmentBatchNo = normalizeCutRoundMotherSegmentBatchNo(sourceBatchNo);
        Map<String, HcAdhesiveSourceRespVO> sourceMap = new LinkedHashMap<>();
        List<HcAdhesive2ReportDO> confirmedAdhesive2Sources = hcAdhesive2ReportMapper.selectConfirmedListByPlanId(planId);
        List<HcAdhesive2ReportDO> abnormalAdhesive2Sources = hcAdhesive2ReportMapper.selectAbnormalListByPlanId(planId);
        Set<Long> coaInspectionSourcePressSlotReportIds = loadActiveAdhesive2CoaInspectionSourcePressSlotReportIds(
                confirmedAdhesive2Sources, abnormalAdhesive2Sources);
        Set<Long> coaInspectionSourceAdhesive2ReportIds = loadActiveAdhesive2PostConfirmCoaInspectionSourceReportIds(
                confirmedAdhesive2Sources, abnormalAdhesive2Sources);
        confirmedAdhesive2Sources.stream()
                .filter(source -> !isCutRoundSourceFromPressSlotProcessCheck(source))
                .filter(source -> !isAdhesive2SourceUnderCoaInspection(source,
                        coaInspectionSourcePressSlotReportIds, coaInspectionSourceAdhesive2ReportIds))
                .map(this::buildSourceResp)
                .filter(source -> matchesCutRoundSourceSegment(source, targetSegmentBatchNo))
                .forEach(source -> putCutRoundSource(sourceMap, source, true));
        abnormalAdhesive2Sources.stream()
                .filter(source -> !isCutRoundSourceFromPressSlotProcessCheck(source))
                .filter(source -> !isAdhesive2SourceUnderCoaInspection(source,
                        coaInspectionSourcePressSlotReportIds, coaInspectionSourceAdhesive2ReportIds))
                .map(this::buildSourceResp)
                .filter(source -> matchesCutRoundSourceSegment(source, targetSegmentBatchNo))
                .forEach(source -> putCutRoundSource(sourceMap, source, true));
        hcPressSlotReportMapper.selectAbnormalListByPlanId(planId).stream()
                .filter(source -> !isPressSlotProcessCheckReport(source))
                .map(this::buildCutRoundPressSlotAbnormalSourceResp)
                .filter(source -> matchesCutRoundSourceSegment(source, targetSegmentBatchNo))
                .forEach(source -> putCutRoundSource(sourceMap, source, false));
        hcSlittingSliceRecordMapper.selectAbnormalListByPlanId(planId).stream()
                .map(this::buildCutRoundSlittingAbnormalSourceResp)
                .filter(source -> matchesCutRoundSourceSegment(source, targetSegmentBatchNo))
                .forEach(source -> putCutRoundSource(sourceMap, source, false));
        return new ArrayList<>(sourceMap.values());
    }

    @Override
    public List<HcAdhesiveReportRespVO> getReportList(Long planOperationId) {
        return getReportList(planOperationId, null);
    }

    @Override
    public List<HcAdhesiveReportRespVO> getReportList(Long planOperationId, LocalDate scanConfirmDate) {
        if (planOperationId == null) {
            return Collections.emptyList();
        }
        LocalDateTime scanConfirmStart = scanConfirmDate == null ? null : scanConfirmDate.atStartOfDay();
        LocalDateTime scanConfirmEndExclusive = scanConfirmStart == null ? null : scanConfirmStart.plusDays(1);
        List<HcAdhesiveReportRespVO> result = new ArrayList<>();
        for (HcCutRoundReportDO report : hcCutRoundReportMapper.selectListByPlanOperationId(
                planOperationId, scanConfirmStart, scanConfirmEndExclusive)) {
            result.add(buildReportRespWithCheckItems(report));
        }
        return result;
    }

    @Override
    public HcAdhesiveReportRespVO getReportByBatchNo(String batchNo, String planNo) {
        String normalized = StrUtil.trimToEmpty(batchNo);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(normalized);
        String sourceBatchNo = stripCutRoundSizeSuffix(normalized);
        if (StrUtil.isNotBlank(sourceBatchNo) && !sourceBatchNo.equalsIgnoreCase(normalized)) {
            candidates.add(sourceBatchNo);
        }
        String targetPlanNo = StrUtil.trimToEmpty(planNo);
        for (String candidate : candidates) {
            for (HcCutRoundReportDO report : hcCutRoundReportMapper.selectListByBatchNo(candidate)) {
                if (StrUtil.isBlank(targetPlanNo) || targetPlanNo.equalsIgnoreCase(StrUtil.trimToEmpty(report.getPlanNo()))) {
                    return buildReportRespWithCheckItems(report);
                }
            }
        }
        return null;
    }

    @Override
    public PageResult<HcCutRoundProductionRecordRespVO> getProductionRecordPage(
            HcCutRoundProductionRecordPageReqVO reqVO) {
        List<HcCutRoundProductionRecordRespVO> rows = getProductionRecordList(reqVO);
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        if (pageSize <= 0) {
            return new PageResult<>(rows, (long) rows.size());
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    @Override
    public List<HcCutRoundProductionRecordRespVO> getProductionRecordList(
            HcCutRoundProductionRecordPageReqVO reqVO) {
        String padTypeFilter = productionRecordPadTypeResolver.normalizeFilter(reqVO.getPadType());
        if (StrUtil.isNotBlank(reqVO.getPadType()) && padTypeFilter == null) {
            throw invalidParamException("类型筛选仅支持黑垫、白垫或未归类");
        }
        List<HcCutRoundReportDO> reports = hcCutRoundReportMapper.selectProductionRecordList(reqVO);
        List<RndManualProductionRecord> rndManualRecords = selectRndManualProductionRecords(reqVO);
        String targetCutSizeMm = normalizeCutSizeMm(reqVO.getCutSizeMm());
        Map<String, List<String>> bladeReplaceReasonMap = buildBladeReplaceReasonMap(reports, reqVO);
        List<String> modelCodes = new ArrayList<>();
        reports.forEach(report -> modelCodes.add(resolveProductionRecordModelCode(report, parseExtra(report.getExtraJson()))));
        rndManualRecords.forEach(record -> modelCodes.add(record.modelCode));
        Map<String, String> modelPadTypes = productionRecordPadTypeResolver.resolveByModelCodes(modelCodes);
        Map<ProductionRecordPadKey, ProductionRecordAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (HcCutRoundReportDO report : reports) {
            Map<String, Object> extra = parseExtra(report.getExtraJson());
            String cutSizeMm = resolveCutSizeMm(report, extra);
            if (StrUtil.isNotBlank(targetCutSizeMm) && !targetCutSizeMm.equals(cutSizeMm)) {
                continue;
            }
            ProductionRecordKey key = new ProductionRecordKey(
                    resolveProductionRecordDate(report),
                    resolveProductionRecordModelCode(report, extra),
                    resolveProductionRecordBatchNo(report),
                    cutSizeMm);
            String padType = modelPadTypes.get(StrUtil.trimToEmpty(key.modelCode()));
            ProductionRecordPadKey padKey = new ProductionRecordPadKey(key, padType);
            accumulatorMap.computeIfAbsent(padKey, ignored -> new ProductionRecordAccumulator(key, padType, key))
                    .accept(report, extra);
        }
        for (RndManualProductionRecord record : rndManualRecords) {
            if (StrUtil.isNotBlank(targetCutSizeMm) && !targetCutSizeMm.equals(record.cutSizeMm)) {
                continue;
            }
            ProductionRecordKey key = new ProductionRecordKey(
                    record.reportDate, record.modelCode, record.productionBatchNo, record.cutSizeMm);
            String modelPadType = modelPadTypes.get(StrUtil.trimToEmpty(record.modelCode));
            String padType = firstNotBlank(record.padType, modelPadType);
            padType = productionRecordPadTypeResolver.normalizePadType(padType);
            ProductionRecordPadKey padKey = new ProductionRecordPadKey(key, padType);
            // 原型号分类的聚合行保留修订 ID；研发明确选择不同垫型时使用独立 ID。
            Object displayKey = Objects.equals(padType, modelPadType) ? key : padKey;
            accumulatorMap.computeIfAbsent(padKey, ignored -> new ProductionRecordAccumulator(key, padKey.padType(), displayKey))
                    .acceptRndManual(record);
        }
        List<HcCutRoundProductionRecordRespVO> result = accumulatorMap.values().stream()
                .peek(item -> item.fillBladeReplaceReasons(bladeReplaceReasonMap))
                .map(ProductionRecordAccumulator::toRespVO)
                .sorted(Comparator
                        .comparing((HcCutRoundProductionRecordRespVO item) -> firstNotNull(
                                        item.getRecordTime(),
                                        item.getReportDate() == null ? null : item.getReportDate().atStartOfDay()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcCutRoundProductionRecordRespVO::getReportDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcCutRoundProductionRecordRespVO::getModelCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(HcCutRoundProductionRecordRespVO::getProductionBatchNo,
                                Comparator.nullsLast(String::compareTo))
                .thenComparing(HcCutRoundProductionRecordRespVO::getCutSizeMm,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
        return productionRecordRevisionService.applyRevisions(
                HcProductionRecordRevisionServiceImpl.MODULE_CUT_ROUND, result).stream()
                .filter(row -> productionRecordPadTypeResolver.matchesFilter(padTypeFilter, row.getPadType()))
                .toList();
    }

    private List<RndManualProductionRecord> selectRndManualProductionRecords(
            HcCutRoundProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcCutRoundSpareRecordDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HcCutRoundSpareRecordDO::getDeleted, false)
                .eq(HcCutRoundSpareRecordDO::getEventType, EVENT_RND_MANUAL_USE)
                .eq(HcCutRoundSpareRecordDO::getRecordSource, RECORD_SOURCE_RND_MANUAL)
                .likeIfPresent(HcCutRoundSpareRecordDO::getModelCode, StrUtil.trimToNull(reqVO.getModelCode()))
                .likeIfPresent(HcCutRoundSpareRecordDO::getProductionBatchNo,
                        StrUtil.trimToNull(reqVO.getProductionBatchNo()))
                .likeIfPresent(HcCutRoundSpareRecordDO::getOperatorName, StrUtil.trimToNull(reqVO.getRecorderName()))
                .orderByAsc(HcCutRoundSpareRecordDO::getEventTime)
                .orderByAsc(HcCutRoundSpareRecordDO::getId);
        if (reqVO.getReportDateStart() != null) {
            wrapper.ge(HcCutRoundSpareRecordDO::getEventTime, reqVO.getReportDateStart().atStartOfDay());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.lt(HcCutRoundSpareRecordDO::getEventTime,
                    reqVO.getReportDateEnd().plusDays(1).atStartOfDay());
        }
        Map<String, RndManualProductionRecord> groupedRecords = new LinkedHashMap<>();
        for (HcCutRoundSpareRecordDO record : hcCutRoundSpareRecordMapper.selectList(wrapper)) {
            if (record.getEventTime() == null || StrUtil.isBlank(record.getProductionBatchNo())) {
                continue;
            }
            String groupKey = record.getEventTime().toLocalDate() + "|"
                    + firstNotBlank(record.getRecordGroupNo(), "RND-LEGACY-" + record.getId());
            groupedRecords.computeIfAbsent(groupKey, ignored -> new RndManualProductionRecord(record))
                    .accept(record);
        }
        return new ArrayList<>(groupedRecords.values());
    }

    @Override
    public List<HcCutRoundInspectionTaskRespVO> getInspectionTaskList(Long planOperationId) {
        List<HcCutRoundInspectionTaskDO> tasks = hcCutRoundInspectionTaskMapper.selectListByPlanOperationId(planOperationId);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> taskIds = tasks.stream().map(HcCutRoundInspectionTaskDO::getId).toList();
        Map<Long, List<HcCutRoundInspectionDetailDO>> detailMap = new LinkedHashMap<>();
        for (HcCutRoundInspectionDetailDO detail : hcCutRoundInspectionDetailMapper.selectListByTaskIds(taskIds)) {
            detailMap.computeIfAbsent(detail.getTaskId(), key -> new ArrayList<>()).add(detail);
        }
        return tasks.stream()
                .map(task -> buildInspectionTaskResp(task, detailMap.getOrDefault(task.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInspectionTask(HcCutRoundInspectionTaskSaveReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        List<Long> reportIds = reqVO.getReportIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (reportIds.isEmpty()) {
            throw invalidParamException("报检裁切片不能为空");
        }
        List<HcCutRoundReportDO> reports = hcCutRoundReportMapper.selectListByIds(reportIds);
        if (reports.size() != reportIds.size()) {
            throw invalidParamException("存在无效的裁切片记录");
        }
        Map<Long, HcCutRoundReportDO> reportMap = new LinkedHashMap<>();
        for (HcCutRoundReportDO report : reports) {
            reportMap.put(report.getId(), report);
            if (!Objects.equals(report.getPlanOperationId(), operation.getId())) {
                throw invalidParamException("报检裁切片不属于当前裁切工单");
            }
            String reportStatus = StrUtil.blankToDefault(report.getReportStatus(), "");
            if (!"CONFIRMED".equalsIgnoreCase(reportStatus) && !"SUBMITTED".equalsIgnoreCase(reportStatus)) {
                throw invalidParamException("存在未扫码确认的裁切片，不能提交检验");
            }
            if (report.getInspectionTaskId() != null
                    || INSPECTION_STATUS_INSPECTING.equalsIgnoreCase(StrUtil.blankToDefault(report.getInspectionStatus(), ""))) {
                throw invalidParamException("存在已提交检验任务的裁切片，请移除后再提交");
            }
        }
        validateUpstreamSampleUnlocked(reports);
        validateInspectionReportsInSameSegment(reports);
        LocalDateTime now = LocalDateTime.now();
        String taskNo = buildInspectionTaskNo(operation, now);
        HcCutRoundInspectionTaskDO task = HcCutRoundInspectionTaskDO.builder()
                .tenantId(planOrder.getTenantId())
                .taskNo(taskNo)
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .reportProcess(firstNotBlank(reqVO.getReportProcess(), operation.getOpName(), "裁切"))
                .receiveLocation(firstNotBlank(reqVO.getReceiveLocation(), "检验室"))
                .reportDate(reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate())
                .reportTime(normalizeValidLocalDateTime(reqVO.getReportTime(), now))
                .reporterName(firstNotBlank(reqVO.getReporterName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .receiverName(reqVO.getReceiverName())
                .priorityLevel(firstNotBlank(reqVO.getPriorityLevel(), "NORMAL"))
                .expectedFinishDate(reqVO.getExpectedFinishDate())
                .taskStatus(INSPECTION_TASK_STATUS_INSPECTING)
                .detailCount(reportIds.size())
                .remark(reqVO.getRemark())
                .build();
        hcCutRoundInspectionTaskMapper.insert(task);
        int seqNo = 1;
        List<HcCutRoundInspectionDetailDO> createdDetails = new ArrayList<>();
        for (Long reportId : reportIds) {
            HcCutRoundReportDO report = reportMap.get(reportId);
            String sizeRule = firstNotBlank(extraString(parseExtra(report.getExtraJson()), "actualSizeRule"), "");
            CutRoundQualityRisk qualityRisk = resolveQualityRisk(report);
            HcCutRoundInspectionDetailDO detail = HcCutRoundInspectionDetailDO.builder()
                    .tenantId(report.getTenantId() == null ? planOrder.getTenantId() : report.getTenantId())
                    .taskId(task.getId())
                    .cutRoundReportId(report.getId())
                    .seqNo(seqNo++)
                    .parentProductionBatchNo(report.getParentProductionBatchNo())
                    .materialCode(report.getMaterialCode())
                    .materialName(report.getMaterialName())
                    .modelCode(report.getModelCode())
                    .sizeRule(sizeRule)
                    .productionBatchNo(report.getProductionBatchNo())
                    .qualityRiskFlag(qualityRisk.flag())
                    .qualityRiskSnapshotJson(qualityRisk.snapshotJson())
                    .build();
            hcCutRoundInspectionDetailMapper.insert(detail);
            createdDetails.add(detail);
            HcCutRoundReportDO update = new HcCutRoundReportDO();
            update.setId(report.getId());
            update.setInspectionTaskId(task.getId());
            update.setInspectionTaskNo(taskNo);
            update.setInspectionStatus(INSPECTION_STATUS_INSPECTING);
            update.setInspectionResult(null);
            update.setInspectorName(null);
            update.setInspectionTime(null);
            update.setInspectionRemark("裁切成品检验已生成，等待片级检验");
            hcCutRoundReportMapper.updateById(update);
        }
        qmsCutRoundFqcService.createFromCutRoundInspectionTask(planOrder, operation, task, createdDetails, reportMap);
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveReport(HcAdhesiveReportSaveReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        if (!OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            throw invalidParamException(OP_STATUS_FINISHED.equals(operation.getOperationStatus())
                    ? "当前工单此工序已完工，不能再报工！" : "当前工单此工序未开工，请先执行开工确认");
        }
        HcAdhesive2ReportDO source = hcAdhesive2ReportMapper.selectById(reqVO.getSourceGrindingSecondDetailId());
        if (source == null || Boolean.TRUE.equals(source.getDeleted())) {
            throw invalidParamException("来源粘胶2片号不存在");
        }
        if (!isConfirmedOrSubmittedStatus(source.getReportStatus()) && !isAdhesive2SourceNg(source)) {
            throw invalidParamException("来源粘胶2片号未扫码确认，不能裁切报工");
        }
        if (isCutRoundSourceFromPressSlotProcessCheck(source)) {
            throw invalidParamException("压槽过程加检片不进入粘胶2/裁切工序");
        }
        HcCutRoundReportDO existed = reqVO.getId() == null ? null : hcCutRoundReportMapper.selectById(reqVO.getId());
        if (existed == null && !hcCutRoundReportMapper.selectListBySourceAdhesive2ReportId(source.getId()).isEmpty()) {
            throw invalidParamException("来源粘胶2片号已存在裁切报工记录，不能重复报工");
        }
        if (existed != null && !"DRAFT".equalsIgnoreCase(StrUtil.blankToDefault(existed.getReportStatus(), "DRAFT"))) {
            throw invalidParamException("当前裁切报工记录已提交或确认，不能修改");
        }
        LocalDate reportDate = reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = normalizeReportDateTime(reqVO.getStartTime(), reportDate);
        LocalDateTime endTime = normalizeReportDateTime(reqVO.getEndTime(), reportDate);
        LocalDateTime recorderTime = normalizeReportDateTime(reqVO.getRecorderTime(), reportDate);
        if (startTime == null) {
            startTime = now;
        }
        if (endTime == null) {
            endTime = now;
        }
        if (recorderTime == null) {
            recorderTime = now;
        }
        BigDecimal inputQty = BigDecimal.ONE;
        HcPlanOrderInventoryLockDO sourceLock = ensureSourceWipLockedForOperation(planOrder, operation,
                SOURCE_TYPE_ADHESIVE2, SOURCE_TABLE_ADHESIVE2_REPORT, source.getId(),
                inputQty, firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                "裁切入站自动锁定粘胶2中间品");
        CutRoundQualityRisk qualityRisk = buildQualityRisk(source, reqVO.getSelfCheck(), reqVO.getDefectCode(),
                reqVO.getRemark(), reqVO.getExtraJson());
        HcCutRoundReportDO report = HcCutRoundReportDO.builder()
                .id(existed == null ? null : existed.getId())
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .sourceAdhesive2ReportId(source.getId())
                .sourceStockId(sourceLock == null ? null : sourceLock.getStockId())
                .sourcePlanLockId(sourceLock == null ? null : sourceLock.getId())
                .sourceStockBatchNo(sourceLock == null ? null : firstNotBlank(sourceLock.getSourceBatchNo(), sourceLock.getBatchNo()))
                .sourceLockQty(sourceLock == null ? null : inputQty)
                .sourcePressSlotReportId(source.getSourcePressSlotReportId())
                .sourceSlittingSliceId(source.getSourceSlittingSliceId())
                .sourceBatchNo(firstNotBlank(reqVO.getSourceBatchNo(), source.getSourceBatchNo()))
                .sourceProductionBatchNo(firstNotBlank(reqVO.getSourceProductionBatchNo(), source.getProductionBatchNo()))
                .productionBatchNo(firstNotBlank(reqVO.getProductionBatchNo(), source.getProductionBatchNo()))
                .parentProductionBatchNo(firstNotBlank(reqVO.getParentProductionBatchNo(), source.getParentProductionBatchNo()))
                .materialCode(firstNotBlank(reqVO.getMaterialCode(), planOrder.getMaterialCode()))
                .materialName(firstNotBlank(reqVO.getMaterialName(), planOrder.getMaterialName()))
                .modelCode(firstNotBlank(reqVO.getModelCode(), planOrder.getModelCode(), planOrder.getModelName()))
                .reportDate(reportDate)
                .startTime(startTime)
                .endTime(endTime)
                .inputLength(inputQty)
                .outputLength(BigDecimal.ONE)
                .selfCheck(firstNotBlank(reqVO.getSelfCheck(), "OK"))
                .defectCode(reqVO.getDefectCode())
                .qualityRiskFlag(qualityRisk.flag())
                .qualityRiskSnapshotJson(qualityRisk.snapshotJson())
                .reportStatus(existed == null ? "DRAFT" : StrUtil.blankToDefault(existed.getReportStatus(), "DRAFT"))
                .recorderName(firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(recorderTime)
                .remark(reqVO.getRemark())
                .extraJson(reqVO.getExtraJson())
                .build();
        if (existed == null) {
            hcCutRoundReportMapper.insert(report);
        } else {
            hcCutRoundReportMapper.updateById(report);
            hcCutRoundCheckDetailMapper.deleteByReportId(report.getId());
        }
        saveCheckDetails(reqVO, planOrder, operation, report.getId());
        syncCutRoundProductionCheckProcessFormRecord(report, planOrder, operation);
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAndConfirmReport(HcAdhesiveReportSaveConfirmReqVO reqVO) {
        Long reportId = saveReport(reqVO);
        HcAdhesiveReportConfirmReqVO confirmReqVO = new HcAdhesiveReportConfirmReqVO();
        confirmReqVO.setId(reportId);
        confirmReqVO.setScannedBatchNo(firstNotBlank(reqVO.getScannedBatchNo(),
                reqVO.getProductionBatchNo(), reqVO.getSourceProductionBatchNo()));
        confirmReqVO.setActualSizeRule(reqVO.getActualSizeRule());
        confirmReqVO.setConfirmerName(reqVO.getConfirmerName());
        confirmReqVO.setConfirmerTime(reqVO.getConfirmerTime());
        return confirmReport(confirmReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmReport(HcAdhesiveReportConfirmReqVO reqVO) {
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(reqVO.getId());
        if (report == null || Boolean.TRUE.equals(report.getDeleted())) {
            throw invalidParamException("裁切报工记录不存在");
        }
        if ("SUBMITTED".equalsIgnoreCase(StrUtil.blankToDefault(report.getReportStatus(), "DRAFT"))) {
            throw invalidParamException("裁切报工记录已过站提交，不能重复扫码确认");
        }
        HcAdhesive2ReportDO source = report.getSourceAdhesive2ReportId() == null ? null
                : hcAdhesive2ReportMapper.selectById(report.getSourceAdhesive2ReportId());
        String inheritedActualSizeRule = source == null ? "" : normalizeCutRoundActualSizeRule(source.getActualSizeRule());
        String requestedActualSizeRule = normalizeCutRoundActualSizeRule(reqVO.getActualSizeRule());
        if (StrUtil.isNotBlank(inheritedActualSizeRule) && StrUtil.isNotBlank(requestedActualSizeRule)
                && !Objects.equals(inheritedActualSizeRule, requestedActualSizeRule)) {
            throw invalidParamException("裁切实际尺寸必须继承粘胶2已选择的尾号尺寸");
        }
        String actualSizeRule = firstNotBlank(inheritedActualSizeRule, requestedActualSizeRule);
        if (StrUtil.isBlank(actualSizeRule)) {
            throw invalidParamException("缺少粘胶2尾号尺寸；历史片号请先选择实际尺寸");
        }
        String finalProductionBatchNo = StrUtil.isNotBlank(inheritedActualSizeRule)
                ? StrUtil.trimToEmpty(report.getProductionBatchNo())
                : buildCutRoundProductionBatchNo(firstNotBlank(report.getProductionBatchNo(), report.getSourceProductionBatchNo()), actualSizeRule);
        String scannedBatchNo = StrUtil.isNotBlank(inheritedActualSizeRule)
                ? StrUtil.trimToEmpty(reqVO.getScannedBatchNo())
                : buildCutRoundProductionBatchNo(reqVO.getScannedBatchNo(), actualSizeRule);
        if (StrUtil.isBlank(finalProductionBatchNo) || !Objects.equals(scannedBatchNo, finalProductionBatchNo)) {
            throw invalidParamException("扫码片号与裁切片号不一致");
        }
        HcPlanOrderOperationDO operation = getOperation(report.getPlanOperationId());
        Map<String, Integer> useCounts = increaseConsumablesOnConfirm(report, operation, reqVO.getConfirmerName());
        HcCutRoundReportDO update = new HcCutRoundReportDO();
        update.setId(report.getId());
        update.setProductionBatchNo(finalProductionBatchNo);
        update.setReportStatus("CONFIRMED");
        update.setConfirmerName(firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        LocalDateTime confirmerTime = normalizeReportDateTime(reqVO.getConfirmerTime(), report.getReportDate());
        update.setConfirmerTime(confirmerTime == null ? LocalDateTime.now() : confirmerTime);
        update.setBladeUseCount(useCounts.get(CONSUMABLE_BLADE));
        update.setFeltUseCount(useCounts.get(CONSUMABLE_FELT));
        Map<String, Object> extra = new LinkedHashMap<>(parseExtra(report.getExtraJson()));
        extra.put("actualSizeRule", actualSizeRule);
        extra.put("actualSizeSuffix", getCutRoundSizeSuffix(actualSizeRule));
        update.setExtraJson(JsonUtils.toJsonString(extra));
        hcCutRoundReportMapper.updateById(update);
        HcCutRoundReportDO confirmedReport = hcCutRoundReportMapper.selectById(report.getId());
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(report.getPlanId());
        syncCutRoundProductionCheckProcessFormRecord(confirmedReport == null ? report : confirmedReport, planOrder, operation);
        consumeCutRoundSourceWipIfPresent(planOrder, operation, report, finalProductionBatchNo,
                update.getConfirmerTime(), update.getConfirmerName());
        postCutRoundOutputWipIfNeeded(planOrder, operation, report, finalProductionBatchNo,
                update.getConfirmerTime(), update.getConfirmerName());
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReportPrinted(HcAdhesiveReportPrintReqVO reqVO) {
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(reqVO.getId());
        if (report == null || Boolean.TRUE.equals(report.getDeleted())) {
            throw invalidParamException("裁切报工记录不存在");
        }
        Map<String, Object> extra = new LinkedHashMap<>(parseExtra(report.getExtraJson()));
        extra.put("printStatus", firstNotBlank(reqVO.getPrintStatus(), "已打印"));
        extra.put("printTime", (reqVO.getPrintTime() == null ? LocalDateTime.now() : reqVO.getPrintTime())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        extra.put("printCount", reqVO.getPrintCount() == null ? 1 : Math.max(reqVO.getPrintCount(), 1));
        HcCutRoundReportDO update = new HcCutRoundReportDO();
        update.setId(report.getId());
        update.setExtraJson(JsonUtils.toJsonString(extra));
        hcCutRoundReportMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcAdhesiveReportRespVO correctReportAbnormalCategory(HcVisualAbnormalCategoryCorrectReqVO reqVO) {
        String category = StrUtil.trimToEmpty(reqVO.getCategory());
        String reason = StrUtil.trimToEmpty(reqVO.getReason());
        validateVisualAbnormalCategory(category, reason);
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectByIdForUpdate(reqVO.getId());
        if (report == null || Boolean.TRUE.equals(report.getDeleted())) {
            throw invalidParamException("裁切报工记录不存在");
        }
        Map<String, Object> extra = new LinkedHashMap<>(parseExtra(report.getExtraJson()));
        List<String> beforeCategories = new ArrayList<>(resolveActiveVisualCategories(extra));
        String currentDefectCode = StrUtil.trimToEmpty(report.getDefectCode());
        if (VISUAL_CATEGORY_NAMES.contains(currentDefectCode) && !beforeCategories.contains(currentDefectCode)) {
            beforeCategories.add(currentDefectCode);
        }
        if (!isNgValue(report.getSelfCheck()) && StrUtil.isBlank(currentDefectCode) && beforeCategories.isEmpty()) {
            throw invalidParamException("当前裁切报工记录没有外观异常类别，不能修正");
        }

        String correctedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Long correctedById = SecurityFrameworkUtils.getLoginUserId();
        String correctedByName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        applyVisualAbnormalCategoryCorrectionExtra(extra, beforeCategories, category, reason,
                correctedAt, correctedById, correctedByName);

        HcCutRoundReportDO update = new HcCutRoundReportDO();
        update.setId(report.getId());
        update.setSelfCheck("NG");
        update.setDefectCode(category);
        update.setExtraJson(JsonUtils.toJsonString(extra));
        update.setRemark(buildVisualAbnormalCategoryCorrectionRemark(
                report.getRemark(), beforeCategories, category, correctedAt, correctedByName));
        update.setQualityRiskSnapshotJson(correctVisualCategoryText(
                report.getQualityRiskSnapshotJson(), beforeCategories, category));
        hcCutRoundReportMapper.updateById(update);
        return buildReportResp(hcCutRoundReportMapper.selectById(report.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(HcAdhesiveReportSubmitReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        if (!OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前裁切工序未开工或已完工，不能工单完工");
        }
        validateUpstreamAdhesive2FinishedBeforeSubmit(planOrder, operation);
        List<HcCutRoundReportDO> reports = hcCutRoundReportMapper.selectListByPlanOperationId(operation.getId());
        if (reports.isEmpty()) {
            throw invalidParamException("请先至少保存一条裁切报工记录");
        }
        validateUpstreamSampleUnlocked(reports);
        boolean hasUnconfirmed = reports.stream()
                .anyMatch(report -> !"CONFIRMED".equalsIgnoreCase(report.getReportStatus())
                        && !"SUBMITTED".equalsIgnoreCase(report.getReportStatus()));
        if (hasUnconfirmed) {
            throw invalidParamException("存在未扫码确认的裁切报工记录，不能工单完工");
        }
        boolean hasUncompletedSegment = reports.stream()
                .anyMatch(report -> !"SUBMITTED".equalsIgnoreCase(report.getReportStatus()));
        if (hasUncompletedSegment) {
            throw invalidParamException("存在尚未点击本段完工的裁切分段，不能工单完工");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate reportDate = reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate();
        LocalDateTime endTime = normalizeReportDateTime(reqVO.getEndTime(), reportDate);
        if (endTime == null) {
            endTime = now;
        }
        LocalDateTime startTime = normalizeReportDateTime(reports.get(reports.size() - 1).getStartTime(), reportDate);
        if (startTime == null) {
            startTime = now;
        }
        HcProcessReportDO endReport = HcProcessReportDO.builder()
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
                .materialCode(planOrder.getMaterialCode())
                .materialName(planOrder.getMaterialName())
                .reportDate(reportDate)
                .startTime(startTime)
                .endTime(endTime)
                .goodQty(BigDecimal.valueOf(reports.size()))
                .scrapQty(BigDecimal.ZERO)
                .recorderName(firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(now)
                .confirmerName(firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .confirmerTime(now)
                .reportUom("片")
                .reportType(REPORT_TYPE_END)
                .sourceMenuCode(SOURCE_MENU_CODE)
                .remark(reqVO.getRemark())
                .build();
        hcProcessReportMapper.insert(endReport);
        HcPlanOrderOperationDO updateOperation = new HcPlanOrderOperationDO();
        updateOperation.setId(operation.getId());
        updateOperation.setOperationStatus(OP_STATUS_FINISHED);
        updateOperation.setStatusOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateOperation.setStatusOperatorName(firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateOperation.setStatusOperateTime(now);
        hcPlanOrderOperationMapper.updateById(updateOperation);
        // 与粘胶1一致：同机可挂接多个生产中计划，工单完工不清空设备实时展示绑定。
        for (HcCutRoundReportDO report : reports) {
            if (!"SUBMITTED".equalsIgnoreCase(report.getReportStatus())) {
                HcCutRoundReportDO update = new HcCutRoundReportDO();
                update.setId(report.getId());
                update.setReportStatus("SUBMITTED");
                hcCutRoundReportMapper.updateById(update);
            }
        }
        return endReport.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long completeSegment(HcAdhesiveSegmentCompleteReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        String segmentBatchNo = normalizeCutRoundMotherSegmentBatchNo(reqVO.getSourceProductionBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            throw invalidParamException("请选择当前裁切分段后再本段完工");
        }
        List<HcCutRoundReportDO> segmentReports = filterCutRoundReportsByMotherBatchNo(
                hcCutRoundReportMapper.selectListByPlanOperationId(operation.getId()), segmentBatchNo);
        if (segmentReports.isEmpty()) {
            throw invalidParamException("当前分段暂无裁切报工记录，不能本段完工");
        }
        validateUpstreamSampleUnlocked(segmentReports);
        boolean hasUnconfirmed = segmentReports.stream()
                .anyMatch(report -> {
                    String status = StrUtil.blankToDefault(report.getReportStatus(), "DRAFT");
                    return !"CONFIRMED".equalsIgnoreCase(status) && !"SUBMITTED".equalsIgnoreCase(status);
                });
        if (hasUnconfirmed) {
            throw invalidParamException("当前分段存在未扫码确认的裁切报工记录，不能本段完工");
        }
        boolean allSubmitted = segmentReports.stream()
                .allMatch(report -> "SUBMITTED".equalsIgnoreCase(StrUtil.blankToDefault(report.getReportStatus(), "")));
        if (allSubmitted) {
            throw invalidParamException("当前裁切分段已完成");
        }
        LocalDate reportDate = reqVO.getReportDate() == null ? LocalDate.now() : reqVO.getReportDate();
        LocalDateTime confirmerTime = normalizeReportDateTime(reqVO.getConfirmerTime(), reportDate);
        if (confirmerTime == null) {
            confirmerTime = LocalDateTime.now();
        }
        String confirmerName = firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        HcCutRoundReportDO firstReport = segmentReports.get(0);
        BigDecimal totalOutputQty = segmentReports.stream().map(HcCutRoundReportDO::getOutputLength)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime segmentStartTime = segmentReports.stream().map(HcCutRoundReportDO::getStartTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(confirmerTime);
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("segmentComplete", true);
        extra.put("qtimeScope", "BATCH");
        extra.put("reportCount", segmentReports.size());
        HcProcessReportDO segmentCompleteReport = HcProcessReportDO.builder()
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationStatus(OP_STATUS_FINISHED)
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
                .materialCode(firstNotBlank(firstReport.getMaterialCode(), planOrder.getMaterialCode()))
                .materialName(firstNotBlank(firstReport.getMaterialName(), planOrder.getMaterialName()))
                .motherMaterialId(planOrder.getMotherMaterialId())
                .motherMaterialCode(firstNotBlank(operation.getMotherMaterialCode(), planOrder.getMotherMaterialCode()))
                .motherMaterialName(firstNotBlank(operation.getMotherMaterialName(), planOrder.getMotherMaterialName()))
                .motherModelId(planOrder.getMotherModelId())
                .motherModelCode(firstNotBlank(operation.getMotherModelCode(), planOrder.getMotherModelCode()))
                .motherModelName(firstNotBlank(operation.getMotherModelName(), planOrder.getMotherModelName()))
                .batchNo(segmentBatchNo)
                .productionBatchNo(segmentBatchNo)
                .parentProductionBatchNo(segmentBatchNo)
                .parentBatchNo(segmentBatchNo)
                .sampleCode(operation.getSampleCode())
                .reportDate(reportDate)
                .startTime(segmentStartTime)
                .endTime(confirmerTime)
                .goodQty(totalOutputQty)
                .scrapQty(BigDecimal.ZERO)
                .recorderName(confirmerName)
                .recorderTime(confirmerTime)
                .confirmerName(confirmerName)
                .confirmerTime(confirmerTime)
                .reportUom(firstNotBlank(operation.getUom(), operation.getUnitCode(), operation.getUnitName(),
                        planOrder.getTargetUom(), "片"))
                .reportMinutes(0)
                .reportType(REPORT_TYPE_SEGMENT_COMPLETE)
                .sourceMenuCode(SOURCE_MENU_CODE_SEGMENT_COMPLETE)
                .extraJson(JsonUtils.toJsonString(extra))
                .remark("裁切本段完工")
                .build();
        hcProcessReportMapper.insert(segmentCompleteReport);
        for (HcCutRoundReportDO report : segmentReports) {
            HcCutRoundReportDO update = new HcCutRoundReportDO();
            update.setId(report.getId());
            update.setReportStatus("SUBMITTED");
            update.setConfirmerName(confirmerName);
            update.setConfirmerTime(confirmerTime);
            hcCutRoundReportMapper.updateById(update);
        }
        return segmentCompleteReport.getId();
    }

    @Override
    public List<HcPressSlotConsumableRespVO> getConsumableStatus(Long planOperationId, Long equipmentId) {
        HcPlanOrderOperationDO operation = getOperation(planOperationId);
        Long effectiveEquipmentId = equipmentId != null ? equipmentId : operation.getEquipmentId();
        return List.of(CONSUMABLE_BLADE, CONSUMABLE_FELT).stream()
                .map(type -> buildConsumableResp(operation, effectiveEquipmentId,
                        effectiveEquipmentId == null ? null
                                : hcCutRoundSpareMapper.selectOneByEquipmentAndType(effectiveEquipmentId, type),
                        type))
                .toList();
    }

    @Override
    public HcAdhesiveGlueBoardStockRespVO getConsumableStockByBatch(String consumableType, String batchNo) {
        throw invalidParamException("刀片请在更换窗口选择边库领用记录；毛毡请在裁切备件管理维护后更换");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long replaceConsumable(HcPressSlotConsumableReplaceReqVO reqVO) {
        HcPlanOrderOperationDO operation = getOperation(reqVO.getPlanOperationId());
        String type = normalizeConsumableType(reqVO.getConsumableType());
        Long equipmentId = reqVO.getEquipmentId() != null ? reqVO.getEquipmentId() : operation.getEquipmentId();
        if (equipmentId == null) {
            throw invalidParamException("当前裁切工序未绑定设备，不能更换" + typeName(type));
        }
        if (CONSUMABLE_BLADE.equals(type)) {
            return bladeConsumptionService.replace(reqVO, operation, equipmentId);
        }
        BigDecimal quantity = reqVO.getReplaceQuantity() == null ? BigDecimal.ONE : reqVO.getReplaceQuantity();
        validatePositive(quantity, "本次更换数量");
        int initialUseCount = resolveCutRoundInitialUseCount(reqVO.getInitialUseCount());
        LocalDateTime eventTime = firstNotNullDateTime(reqVO.getReplaceTime(), LocalDateTime.now());
        HcCutRoundSpareDO existed = hcCutRoundSpareMapper.selectOneByEquipmentAndType(equipmentId, type);
        if (existed == null || Boolean.TRUE.equals(existed.getDeleted())) {
            throw invalidParamException("请先在裁切备件管理维护" + typeName(type) + "基础信息");
        }
        int beforeUseCount = existed.getUseCount() == null ? 0 : existed.getUseCount();
        HcCutRoundSpareDO state = HcCutRoundSpareDO.builder()
                .id(existed.getId())
                .tenantId(operation.getTenantId())
                .equipmentId(equipmentId)
                .equipmentCode(firstNotBlank(reqVO.getEquipmentCode(), operation.getEquipmentCode(), existed.getEquipmentCode()))
                .equipmentName(firstNotBlank(reqVO.getEquipmentName(), operation.getEquipmentName(), existed.getEquipmentName()))
                .workCenterId(operation.getWorkCenterId())
                .workCenterCode(operation.getWorkCenterCode())
                .workCenterName(operation.getWorkCenterName())
                .spareType(type)
                .materialCode(null)
                .materialName(null)
                .batchNo(null)
                .onlineQuantity(BigDecimal.ONE)
                .availableQuantity(BigDecimal.ZERO)
                .lastReplaceTime(eventTime)
                .lastReplacePlanNo(reqVO.getPlanNo())
                .lastReplaceReason(reqVO.getReplaceReason())
                .useCount(initialUseCount)
                .limitCount(resolveLimitCount(type, existed))
                .limitDays(resolveLimitDays(type, existed))
                .warningFlag(0)
                .status(STATUS_ACTIVE)
                .lastOperatorId(reqVO.getOperatorId() == null ? SecurityFrameworkUtils.getLoginUserId() : reqVO.getOperatorId())
                .lastOperatorName(firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .lastEventTime(eventTime)
                .remark(reqVO.getReplaceReason())
                .build();
        state.setWarningFlag(isConsumableNeedReminder(state) ? 1 : 0);
        hcCutRoundSpareMapper.updateById(state);
        Long stateId = existed.getId();
        hcCutRoundSpareRecordMapper.insert(HcCutRoundSpareRecordDO.builder()
                .tenantId(operation.getTenantId())
                .spareId(stateId)
                .equipmentId(equipmentId)
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(type)
                .eventType(EVENT_REPLACE)
                .planId(operation.getPlanId())
                .planNo(reqVO.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(initialUseCount)
                .changeUseCount(initialUseCount - beforeUseCount)
                .onlineQuantity(BigDecimal.ONE)
                .offlineQuantity(zeroIfNull(existed.getOnlineQuantity()))
                .finalUseCount(initialUseCount)
                .operatorId(state.getLastOperatorId())
                .operatorName(state.getLastOperatorName())
                .eventTime(eventTime)
                .replaceReason(reqVO.getReplaceReason())
                .remark(reqVO.getReplaceReason())
                .build());
        return stateId;
    }

    @Override
    public HcPressSlotChangeoverInspectionRespVO getLatestChangeover(Long planOperationId) {
        return hcPressSlotChangeoverInspectionMapper.selectListByPlanOperation(planOperationId, null).stream()
                .max(Comparator.comparing(this::resolveChangeoverEffectiveTime)
                        .thenComparing(record -> record.getId() == null ? 0L : record.getId()))
                .map(this::buildChangeoverResp)
                .orElse(null);
    }

    @Override
    public List<HcPressSlotChangeoverInspectionRespVO> getChangeoverList(Long planOperationId, String motherSegmentBatchNo) {
        return hcPressSlotChangeoverInspectionMapper.selectListByPlanOperation(planOperationId, motherSegmentBatchNo).stream()
                .map(this::buildChangeoverResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveChangeover(HcPressSlotChangeoverInspectionSaveReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        LocalDateTime now = LocalDateTime.now();
        HcPressSlotChangeoverInspectionDO existed = reqVO.getId() == null ? null
                : hcPressSlotChangeoverInspectionMapper.selectById(reqVO.getId());
        if (isChangeoverConfirmed(existed)) {
            throw invalidParamException("工艺参数点检已确认，不能修改");
        }
        boolean confirm = CHANGEOVER_STATUS_CONFIRMED.equalsIgnoreCase(reqVO.getInspectionStatus());
        LocalDateTime recordTime = normalizeValidLocalDateTime(reqVO.getRecordTime(), now);
        LocalDateTime submitTime = now;
        String recorderName = firstNotBlank(reqVO.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        String confirmerName = confirm ? firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), recorderName) : null;
        LocalDateTime confirmerTime = confirm ? normalizeValidLocalDateTime(reqVO.getConfirmerTime(), now) : null;
        String inspectionStatus = confirm ? CHANGEOVER_STATUS_CONFIRMED
                : firstNotBlank(reqVO.getInspectionStatus(), CHANGEOVER_STATUS_RECORDED);
        String extraJson = mergeChangeoverExtra(reqVO.getExtraJson(), recorderName, confirmerName, confirmerTime);
        String detailItemsJson = StrUtil.isNotBlank(reqVO.getDetailItemsJson())
                ? reqVO.getDetailItemsJson()
                : JsonUtils.toJsonString(reqVO.getCheckItems() == null ? Collections.emptyList() : reqVO.getCheckItems());
        HcPressSlotChangeoverInspectionDO record = HcPressSlotChangeoverInspectionDO.builder()
                .id(reqVO.getId())
                .tenantId(planOrder.getTenantId())
                .planId(planOrder.getId())
                .planNo(planOrder.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .sourceSlittingSliceId(reqVO.getSourceSlittingSliceId())
                .pressSlotSliceNo(reqVO.getPressSlotSliceNo())
                .motherSegmentBatchNo(reqVO.getMotherSegmentBatchNo())
                .productionModelCode(reqVO.getProductionModelCode())
                .productionMaterialCode(reqVO.getProductionMaterialCode())
                .currentPlanNo(reqVO.getCurrentPlanNo())
                .inspectionStatus(inspectionStatus)
                .submitTime(submitTime)
                .headerDataJson(reqVO.getHeaderDataJson())
                .detailItemsJson(detailItemsJson)
                .recorderName(recorderName)
                .recordTime(recordTime)
                .remark(reqVO.getRemark())
                .extraJson(extraJson)
                .build();
        if (record.getId() == null) {
            hcPressSlotChangeoverInspectionMapper.insert(record);
        } else {
            hcPressSlotChangeoverInspectionMapper.updateById(record);
        }
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChangeover(Long planOperationId, Long recordId) {
        if (planOperationId == null || recordId == null) {
            throw invalidParamException("请选择要删除的工艺参数点检记录");
        }
        HcPressSlotChangeoverInspectionDO record = hcPressSlotChangeoverInspectionMapper.selectById(recordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw invalidParamException("工艺参数点检记录不存在或已删除");
        }
        if (!Objects.equals(record.getPlanOperationId(), planOperationId)) {
            throw invalidParamException("工艺参数点检记录不属于当前裁切工单");
        }
        hcPressSlotChangeoverInspectionMapper.deleteById(recordId);
    }

    private boolean isChangeoverConfirmed(HcPressSlotChangeoverInspectionDO record) {
        if (record == null) {
            return false;
        }
        if (CHANGEOVER_STATUS_CONFIRMED.equalsIgnoreCase(record.getInspectionStatus())) {
            return true;
        }
        Map<String, Object> extra = parseExtra(record.getExtraJson());
        return CHANGEOVER_STATUS_CONFIRMED.equalsIgnoreCase(extraString(extra, "recordStatus"))
                || CHANGEOVER_STATUS_CONFIRMED.equalsIgnoreCase(extraString(extra, "docStatus"));
    }

    private HcPlanOrderOperationDO getOperation(Long planOperationId) {
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(planOperationId);
        if (operation == null) {
            throw invalidParamException("计划工序不存在");
        }
        return validateOperation(hcPlanOrderMapper.selectById(operation.getPlanId()), planOperationId);
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
        if (!"OP-CUT-ROUND".equalsIgnoreCase(opCode) && !"OP-CUT".equalsIgnoreCase(opCode)
                && !opName.contains("裁切") && !opName.contains("裁圆")) {
            throw invalidParamException("当前仅允许裁切工序使用该功能");
        }
        return operation;
    }

    private void validateUpstreamAdhesive2FinishedBeforeSubmit(HcPlanOrderDO planOrder, HcPlanOrderOperationDO operation) {
        if (planOrder == null || operation == null || operation.getOpSeq() == null) {
            return;
        }
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(planOrder.getId());
        boolean hasUnfinishedAdhesive2 = operations.stream()
                .filter(item -> item.getOpSeq() != null && item.getOpSeq() < operation.getOpSeq())
                .filter(item -> UPSTREAM_ADHESIVE2_OP_CODE.equalsIgnoreCase(StrUtil.trimToEmpty(item.getOpCode()))
                        || StrUtil.trimToEmpty(item.getOpName()).contains("粘胶2"))
                .anyMatch(item -> !OP_STATUS_FINISHED.equalsIgnoreCase(StrUtil.trimToEmpty(item.getOperationStatus())));
        if (hasUnfinishedAdhesive2) {
            throw invalidParamException("上游粘胶2工序未完工，不能裁切工单完工。请先完成全部粘胶2报工后再完工裁切");
        }
    }

    private Long upsertPassWork(HcWetPassWorkSaveReqVO reqVO, boolean confirm) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        HcStationFormDO form = hcStationFormMapper.selectEnabledByCode(reqVO.getFormCode());
        if (form == null || !PROCESS_CODE.equals(form.getProcessCode())) {
            throw invalidParamException("裁切工作准备表单不存在或未启用");
        }
        LocalDate recordDate = reqVO.getRecordDate() == null ? LocalDate.now() : reqVO.getRecordDate();
        Long equipmentId = reqVO.getEquipmentId() == null ? operation.getEquipmentId() : reqVO.getEquipmentId();
        HcStationRecordDO record = reqVO.getRecordId() == null ? null : hcStationRecordMapper.selectById(reqVO.getRecordId());
        if (record == null) {
            record = hcStationRecordMapper.selectOneByEquipmentDaily(equipmentId, recordDate, form.getFormCode());
        }
        if (record == null) {
            record = hcStationRecordMapper.selectLegacyPlanRecordForDailyFallback(reqVO.getPlanOperationId(), form.getFormCode());
        }
        LocalDateTime now = LocalDateTime.now();
        String currentNickname = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        LocalDateTime existingRecordTime = normalizePassWorkDateTime(record == null ? null : record.getRecordTime(), null);
        LocalDateTime recorderTime = confirm
                ? normalizePassWorkDateTime(reqVO.getRecorderTime(), existingRecordTime == null ? now : existingRecordTime)
                : now;
        LocalDateTime confirmerTime = confirm ? now : normalizePassWorkDateTime(reqVO.getConfirmerTime(), null);
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
                .recordUserName(firstNotBlank(reqVO.getRecorder(), record == null ? null : record.getRecordUserName(), currentNickname))
                .recordTime(recorderTime)
                .confirmUserName(confirm ? firstNotBlank(reqVO.getConfirmer(), currentNickname) : reqVO.getConfirmer())
                .confirmTime(confirmerTime)
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
            for (HcWetPassWorkItemReqVO item : reqVO.getDetails()) {
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

    private HcWetPassWorkRespVO buildPassWorkResp(HcStationFormDO form, HcStationRecordDO record) {
        HcWetPassWorkRespVO resp = new HcWetPassWorkRespVO();
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
        resp.setRecorderTime(record == null ? null : formatPassWorkDateTime(record.getRecordTime()));
        resp.setConfirmer(record == null ? null : record.getConfirmUserName());
        resp.setConfirmerTime(record == null ? null : formatPassWorkDateTime(record.getConfirmTime()));
        resp.setPresetHeaderDataJson(form.getPresetHeaderDataJson());
        resp.setHeaderDataJson(record == null ? form.getPresetHeaderDataJson() : firstNotBlank(record.getHeaderDataJson(), form.getPresetHeaderDataJson()));
        resp.setSchemaJson(form.getSchemaJson());
        resp.setPresetDetails(hcStationFormItemMapper.selectByFormId(form.getId()).stream().map(this::buildPresetItem).toList());
        resp.setDetails(record == null ? resp.getPresetDetails()
                : hcStationRecordItemMapper.selectByRecordIds(List.of(record.getId())).stream().map(this::buildRecordItem).toList());
        return resp;
    }

    private HcProcessReportDO getLatestProcessReport(Long planOperationId) {
        if (planOperationId == null) {
            return null;
        }
        return hcProcessReportMapper.selectOne(new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getPlanOperationId, planOperationId)
                .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE)
                .eq(HcProcessReportDO::getDeleted, false)
                .orderByDesc(HcProcessReportDO::getId)
                .last("LIMIT 1"));
    }

    private HcWetPassWorkItemRespVO buildPresetItem(HcStationFormItemDO item) {
        HcWetPassWorkItemRespVO resp = new HcWetPassWorkItemRespVO();
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

    private HcWetPassWorkItemRespVO buildRecordItem(HcStationRecordItemDO item) {
        HcWetPassWorkItemRespVO resp = new HcWetPassWorkItemRespVO();
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

    private HcAdhesiveSourceRespVO buildSourceResp(HcAdhesive2ReportDO source) {
        boolean coaFlag = isCutRoundCoaFlag(source);
        LocalDateTime cutRoundStartTime = hcCutRoundReportMapper.selectListBySourceAdhesive2ReportId(source.getId()).stream()
                .map(HcCutRoundReportDO::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return HcAdhesiveSourceRespVO.builder()
                .grindingSecondDetailId(source.getId())
                .grindingPlanId(source.getPlanId())
                .grindingPlanNo(source.getPlanNo())
                .grindingPlanOperationId(source.getPlanOperationId())
                .rowUid(String.valueOf(source.getId()))
                .motherBatchNo(firstNotBlank(source.getSourceBatchNo(), source.getParentProductionBatchNo()))
                .productionBatchNo(source.getProductionBatchNo())
                .actualSizeRule(source.getActualSizeRule())
                .actualSizeSuffix(source.getActualSizeSuffix())
                .parentProductionBatchNo(source.getParentProductionBatchNo())
                .modelCode(source.getModelCode())
                .segmentMark(resolveSegmentMark(source.getSourceBatchNo()))
                .processLength(firstNonNullDecimal(source.getOutputLength(), source.getInputLength(), BigDecimal.ONE))
                .outputLength(firstNonNullDecimal(source.getOutputLength(), source.getInputLength(), BigDecimal.ONE))
                .selfCheck(source.getSelfCheck())
                .defectCode(source.getDefectCode())
                .operationName(firstNotBlank(source.getOperationName(), "粘胶2"))
                .processStage("ADHESIVE2")
                .productQualityStatus(firstNotBlank(source.getProductQualityStatus(),
                        isAdhesive2SourceNg(source) ? PRODUCT_QUALITY_ABNORMAL : QUALITY_STATUS_NORMAL))
                .qualityLockReason(source.getQualityLockReason())
                .sourceMenuCode(SOURCE_MENU_CODE_ADHESIVE2)
                .coaFlag(coaFlag)
                .extraJson(buildCutRoundSourceExtraJson(source, coaFlag))
                .confirmStatus(source.getReportStatus())
                .confirmedBatchNo(source.getProductionBatchNo())
                .confirmTime(source.getConfirmerTime())
                .downstreamStatus(resolveCutRoundDownstreamStatus(source.getId()))
                .qtime(hcQtimeEvaluationService.evaluateAdhesive2ToCutRound(
                        resolveAdhesive2SegmentCompleteTime(source), cutRoundStartTime,
                        firstNotBlank(source.getModelCode(), source.getMaterialCode(), source.getProductionBatchNo())))
                .build();
    }

    private LocalDateTime resolveAdhesive2SegmentCompleteTime(HcAdhesive2ReportDO source) {
        if (source == null) {
            return null;
        }
        String segmentBatchNo = normalizeCutRoundMotherSegmentBatchNo(firstNotBlank(
                source.getSourceBatchNo(), source.getParentProductionBatchNo(),
                source.getSourceProductionBatchNo(), source.getProductionBatchNo()));
        if (source.getPlanOperationId() == null || StrUtil.isBlank(segmentBatchNo)) {
            return source.getEndTime();
        }
        HcProcessReportDO batchComplete = hcProcessReportMapper.selectOne(
                new LambdaQueryWrapperX<HcProcessReportDO>()
                        .eq(HcProcessReportDO::getPlanOperationId, source.getPlanOperationId())
                        .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE_ADHESIVE2_SEGMENT_COMPLETE)
                        .eq(HcProcessReportDO::getBatchNo, segmentBatchNo)
                        .eq(HcProcessReportDO::getDeleted, false)
                        .orderByDesc(HcProcessReportDO::getEndTime)
                        .orderByDesc(HcProcessReportDO::getId)
                        .last("LIMIT 1"));
        return batchComplete == null || batchComplete.getEndTime() == null
                ? source.getEndTime() : batchComplete.getEndTime();
    }

    /**
     * 通过来源粘胶2报工ID判断裁切扫码报工状态，供扫码页明确展示并避免把“已保存”和“已确认”混为一谈。
     */
    private String resolveCutRoundDownstreamStatus(Long adhesive2ReportId) {
        List<HcCutRoundReportDO> reports = hcCutRoundReportMapper
                .selectListBySourceAdhesive2ReportId(adhesive2ReportId);
        if (reports.isEmpty()) {
            return "未裁切报工";
        }
        return reports.stream().anyMatch(report -> isConfirmedOrSubmittedStatus(report.getReportStatus()))
                ? "裁切已扫码报工" : "裁切已报工待扫码确认";
    }

    private boolean isAdhesive2SourceUnderCoaInspection(HcAdhesive2ReportDO source) {
        return isAdhesive2SourceUnderCoaInspection(source,
                loadActiveAdhesive2CoaInspectionSourcePressSlotReportIds(List.of(source)),
                loadActiveAdhesive2PostConfirmCoaInspectionSourceReportIds(List.of(source)));
    }

    private boolean isAdhesive2SourceUnderCoaInspection(HcAdhesive2ReportDO source,
                                                         Set<Long> coaInspectionSourcePressSlotReportIds,
                                                         Set<Long> coaInspectionSourceAdhesive2ReportIds) {
        if (source == null) {
            return false;
        }
        return (source.getSourcePressSlotReportId() != null
                && coaInspectionSourcePressSlotReportIds.contains(source.getSourcePressSlotReportId()))
                || (source.getId() != null
                && coaInspectionSourceAdhesive2ReportIds.contains(source.getId()));
    }

    @SafeVarargs
    private final Set<Long> loadActiveAdhesive2CoaInspectionSourcePressSlotReportIds(
            List<HcAdhesive2ReportDO>... sourceGroups) {
        Set<Long> sourcePressSlotReportIds = new LinkedHashSet<>();
        for (List<HcAdhesive2ReportDO> sourceGroup : sourceGroups) {
            if (sourceGroup == null) {
                continue;
            }
            sourceGroup.stream()
                    .map(HcAdhesive2ReportDO::getSourcePressSlotReportId)
                    .filter(Objects::nonNull)
                    .forEach(sourcePressSlotReportIds::add);
        }
        if (sourcePressSlotReportIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (QmsFaiOrderDO faiOrder : qmsFaiOrderMapper.selectListBySourceReportIds(
                sourcePressSlotReportIds, SOURCE_MENU_CODE_ADHESIVE2, PROCESS_CODE_ADHESIVE2)) {
            if (faiOrder != null
                    && faiOrder.getSourceReportId() != null
                    && StrUtil.containsIgnoreCase(faiOrder.getSourceReportNo(), "-COA-")
                    && !StrUtil.containsIgnoreCase(faiOrder.getSourceReportNo(), "-COA-POST-")
                    && isAdhesive2CoaFaiBlockingCutRoundSource(faiOrder)) {
                result.add(faiOrder.getSourceReportId());
            }
        }
        return result;
    }

    /**
     * 新 COA 以粘胶2成品报工为来源；与历史“压槽来源 COA”分别按来源表主键拦截，避免主键值碰撞误拦截。
     */
    @SafeVarargs
    private final Set<Long> loadActiveAdhesive2PostConfirmCoaInspectionSourceReportIds(
            List<HcAdhesive2ReportDO>... sourceGroups) {
        Set<Long> adhesive2ReportIds = new LinkedHashSet<>();
        for (List<HcAdhesive2ReportDO> sourceGroup : sourceGroups) {
            if (sourceGroup == null) {
                continue;
            }
            sourceGroup.stream().map(HcAdhesive2ReportDO::getId)
                    .filter(Objects::nonNull)
                    .forEach(adhesive2ReportIds::add);
        }
        if (adhesive2ReportIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (QmsFaiOrderDO faiOrder : qmsFaiOrderMapper.selectListBySourceReportIds(
                adhesive2ReportIds, SOURCE_MENU_CODE_ADHESIVE2, PROCESS_CODE_ADHESIVE2)) {
            if (faiOrder != null
                    && faiOrder.getSourceReportId() != null
                    && StrUtil.containsIgnoreCase(faiOrder.getSourceReportNo(), "-COA-POST-")
                    && isAdhesive2CoaFaiBlockingCutRoundSource(faiOrder)) {
                result.add(faiOrder.getSourceReportId());
            }
        }
        return result;
    }

    private boolean isAdhesive2CoaFaiBlockingCutRoundSource(QmsFaiOrderDO faiOrder) {
        if (faiOrder == null) {
            return false;
        }
        String status = StrUtil.trimToEmpty(faiOrder.getStatus());
        String judgment = StrUtil.trimToEmpty(faiOrder.getJudgment());
        if (FAI_STATUS_CANCELED.equalsIgnoreCase(status)) {
            return false;
        }
        return !(FAI_STATUS_COMPLETED.equalsIgnoreCase(status)
                && FAI_JUDGMENT_OK.equalsIgnoreCase(judgment));
    }

    private HcAdhesiveSourceRespVO buildCutRoundPressSlotAbnormalSourceResp(HcPressSlotReportDO source) {
        String motherBatchNo = firstNotBlank(source.getParentProductionBatchNo(),
                source.getSourceProductionBatchNo(), source.getSourceBatchNo(), source.getProductionBatchNo());
        String reason = firstNotBlank(source.getDefectCode(), source.getRemark(), "压槽自检NG");
        return HcAdhesiveSourceRespVO.builder()
                .grindingSecondDetailId(source.getId())
                .grindingPlanId(source.getPlanId())
                .grindingPlanNo(source.getPlanNo())
                .grindingPlanOperationId(source.getPlanOperationId())
                .rowUid("PRESS_SLOT-" + source.getId())
                .motherBatchNo(motherBatchNo)
                .productionBatchNo(source.getProductionBatchNo())
                .parentProductionBatchNo(motherBatchNo)
                .modelCode(source.getModelCode())
                .segmentMark(resolveSegmentMark(firstNotBlank(motherBatchNo, source.getProductionBatchNo())))
                .processLength(firstNonNullDecimal(source.getOutputLength(), source.getInputLength(), BigDecimal.ONE))
                .outputLength(firstNonNullDecimal(source.getOutputLength(), source.getInputLength(), BigDecimal.ONE))
                .selfCheck(firstNotBlank(source.getSelfCheck(), "NG"))
                .defectCode(source.getDefectCode())
                .operationName(firstNotBlank(source.getOperationName(), "压槽"))
                .processStage("PRESS_SLOT")
                .productQualityStatus(PRODUCT_QUALITY_ABNORMAL)
                .qualityLockReason(reason)
                .sourceMenuCode(SOURCE_MENU_CODE_PRESS_SLOT)
                .coaFlag(isTruthy(parseExtra(source.getExtraJson()).get("coaFlag")))
                .extraJson(buildCutRoundAbnormalExtraJson("压槽", reason, source.getExtraJson(), source.getSelfCheck()))
                .confirmStatus(firstNotBlank(source.getReportStatus(), "NG"))
                .confirmedBatchNo(source.getProductionBatchNo())
                .confirmTime(source.getConfirmerTime())
                .downstreamStatus("ABNORMAL")
                .build();
    }

    private HcAdhesiveSourceRespVO buildCutRoundSlittingAbnormalSourceResp(HcSlittingSliceRecordDO slice) {
        String motherBatchNo = firstNotBlank(slice.getSourceProductionBatchNo(), slice.getSourceBatchNo(), slice.getSliceSerialNo());
        String reason = firstNotBlank(slice.getRemark(), "分切目视自检NG");
        return HcAdhesiveSourceRespVO.builder()
                .grindingSecondDetailId(slice.getId())
                .grindingPlanId(slice.getPlanId())
                .grindingPlanNo(slice.getPlanNo())
                .grindingPlanOperationId(slice.getPlanOperationId())
                .rowUid("SLITTING-" + slice.getId())
                .motherBatchNo(motherBatchNo)
                .productionBatchNo(slice.getSliceSerialNo())
                .parentProductionBatchNo(motherBatchNo)
                .segmentMark(firstNotBlank(slice.getSizeName(), resolveSegmentMark(firstNotBlank(motherBatchNo, slice.getSliceSerialNo()))))
                .processLength(firstNonNullDecimal(slice.getSliceLength(), BigDecimal.ONE))
                .outputLength(firstNonNullDecimal(slice.getSliceLength(), BigDecimal.ONE))
                .selfCheck(firstNotBlank(slice.getSelfCheck(), "NG"))
                .defectCode(null)
                .operationName(firstNotBlank(slice.getOperationName(), "分切"))
                .processStage("SLITTING")
                .productQualityStatus(PRODUCT_QUALITY_ABNORMAL)
                .qualityLockReason(reason)
                .sourceMenuCode(SOURCE_MENU_CODE_SLITTING)
                .extraJson(buildCutRoundAbnormalExtraJson("分切", reason, null, slice.getSelfCheck()))
                .confirmStatus(firstNotBlank(slice.getScanStatus(), "NG"))
                .confirmedBatchNo(slice.getSliceSerialNo())
                .confirmTime(slice.getScanTime())
                .downstreamStatus("ABNORMAL")
                .build();
    }

    private void putCutRoundSource(Map<String, HcAdhesiveSourceRespVO> sourceMap,
                                   HcAdhesiveSourceRespVO source,
                                   boolean overwrite) {
        String key = firstNotBlank(source.getProductionBatchNo(), source.getConfirmedBatchNo(), source.getRowUid());
        if (StrUtil.isBlank(key)) {
            return;
        }
        if (overwrite) {
            sourceMap.put(key, source);
            return;
        }
        HcAdhesiveSourceRespVO existing = sourceMap.get(key);
        if (existing == null || (isCutRoundSourceAbnormal(source) && !isCutRoundSourceAbnormal(existing))) {
            sourceMap.put(key, source);
        }
    }

    private boolean matchesCutRoundSourceSegment(HcAdhesiveSourceRespVO source, String targetSegmentBatchNo) {
        if (StrUtil.isBlank(targetSegmentBatchNo)) {
            return true;
        }
        if (source == null) {
            return false;
        }
        return StrUtil.equalsIgnoreCase(normalizeCutRoundMotherSegmentBatchNo(source.getMotherBatchNo()), targetSegmentBatchNo)
                || StrUtil.equalsIgnoreCase(normalizeCutRoundMotherSegmentBatchNo(source.getParentProductionBatchNo()), targetSegmentBatchNo)
                || StrUtil.equalsIgnoreCase(normalizeCutRoundMotherSegmentBatchNo(source.getProductionBatchNo()), targetSegmentBatchNo)
                || StrUtil.equalsIgnoreCase(normalizeCutRoundMotherSegmentBatchNo(source.getConfirmedBatchNo()), targetSegmentBatchNo);
    }

    private boolean isCutRoundSourceAbnormal(HcAdhesiveSourceRespVO source) {
        return source != null
                && (PRODUCT_QUALITY_ABNORMAL.equalsIgnoreCase(StrUtil.blankToDefault(source.getProductQualityStatus(), ""))
                || isNgValue(source.getSelfCheck())
                || StrUtil.isNotBlank(source.getDefectCode())
                || StrUtil.isNotBlank(source.getQualityLockReason()));
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
            if (calculateWipLockRemainingQty(existed).compareTo(normalizedLockQty) < 0) {
                throw invalidParamException("来源中间品锁定余量不足，剩余 " + calculateWipLockRemainingQty(existed)
                        + "，本次需 " + normalizedLockQty);
            }
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
                .thickness(stock.getThickness())
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
                .uom(firstNotBlank(stock.getUom(), operation.getUom(), operation.getUnitCode()))
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
        BigDecimal normalizedConsumeQty = zeroIfNull(consumeQty);
        if (normalizedConsumeQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("工序报工投入数量必须大于0，才能消耗中间边库");
        }
        return hcInvStockService.consumePlanLockedWip(
                sourceLock,
                normalizedConsumeQty,
                refDocType,
                refDocId,
                refDocNo,
                consumeTime,
                SecurityFrameworkUtils.getLoginUserId(),
                firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                firstNotBlank(remark, "工序确认消耗计划锁定中间品"));
    }

    private void consumeCutRoundSourceWipIfPresent(HcPlanOrderDO planOrder,
                                                   HcPlanOrderOperationDO operation,
                                                   HcCutRoundReportDO report,
                                                   String finalProductionBatchNo,
                                                   LocalDateTime consumeTime,
                                                   String operatorName) {
        if (report == null || report.getId() == null || report.getSourceAdhesive2ReportId() == null) {
            return;
        }
        if (StrUtil.isNotBlank(report.getSourceConsumeTxnNo())) {
            return;
        }
        BigDecimal consumeQty = firstNonNullDecimal(report.getInputLength(), BigDecimal.ONE);
        HcPlanOrderInventoryLockDO sourceLock = report.getSourcePlanLockId() == null
                ? null
                : hcPlanOrderInventoryLockMapper.selectById(report.getSourcePlanLockId());
        if (!isActivePlanWipLock(sourceLock)) {
            sourceLock = resolveActiveSourceWipLock(report.getPlanOperationId(),
                    SOURCE_TABLE_ADHESIVE2_REPORT, report.getSourceAdhesive2ReportId());
        }
        if (sourceLock == null) {
            sourceLock = ensureSourceWipLockedForOperation(planOrder, operation,
                    SOURCE_TYPE_ADHESIVE2, SOURCE_TABLE_ADHESIVE2_REPORT, report.getSourceAdhesive2ReportId(),
                    consumeQty, operatorName, "裁切确认补锁粘胶2中间品");
        }
        HcInvStockDO consumedStock = consumeSourceWipLock(sourceLock, consumeQty,
                SOURCE_TYPE_CUT_ROUND, report.getId(), firstNotBlank(finalProductionBatchNo, report.getProductionBatchNo()),
                consumeTime, operatorName,
                "裁切确认消耗粘胶2中间品；来源片号：" + firstNotBlank(report.getSourceProductionBatchNo(), report.getSourceBatchNo(), "-"));
        if (sourceLock == null || consumedStock == null) {
            return;
        }
        HcCutRoundReportDO consumeUpdate = new HcCutRoundReportDO();
        consumeUpdate.setId(report.getId());
        consumeUpdate.setSourceStockId(sourceLock.getStockId());
        consumeUpdate.setSourcePlanLockId(sourceLock.getId());
        consumeUpdate.setSourceStockBatchNo(firstNotBlank(sourceLock.getSourceBatchNo(), sourceLock.getBatchNo()));
        consumeUpdate.setSourceLockQty(consumeQty);
        consumeUpdate.setSourceConsumeQty(consumeQty);
        consumeUpdate.setSourceConsumeTxnNo(consumedStock.getLastTxnNo());
        consumeUpdate.setSourceConsumeTime(consumedStock.getLastTxnTime());
        hcCutRoundReportMapper.updateById(consumeUpdate);
        report.setSourceStockId(sourceLock.getStockId());
        report.setSourcePlanLockId(sourceLock.getId());
        report.setSourceStockBatchNo(consumeUpdate.getSourceStockBatchNo());
        report.setSourceLockQty(consumeQty);
        report.setSourceConsumeQty(consumeQty);
        report.setSourceConsumeTxnNo(consumedStock.getLastTxnNo());
        report.setSourceConsumeTime(consumedStock.getLastTxnTime());
    }

    private void postCutRoundOutputWipIfNeeded(HcPlanOrderDO planOrder,
                                               HcPlanOrderOperationDO operation,
                                               HcCutRoundReportDO report,
                                               String finalProductionBatchNo,
                                               LocalDateTime postTime,
                                               String operatorName) {
        if (report == null || report.getId() == null || report.getOutputStockId() != null) {
            return;
        }
        if (planOrder == null || operation == null) {
            return;
        }
        BigDecimal outputQty = firstNonNullDecimal(report.getOutputLength(), BigDecimal.ONE);
        if (outputQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String batchNo = firstNotBlank(finalProductionBatchNo, report.getProductionBatchNo(), report.getSourceProductionBatchNo());
        HcInvStockDO stock = hcInvStockService.postProcessOutputWip(HcWipOutputPostReq.builder()
                .plan(planOrder)
                .operation(operation)
                .sourceType(SOURCE_TYPE_CUT_ROUND)
                .sourceTable(SOURCE_TABLE_CUT_ROUND_REPORT)
                .sourceId(report.getId())
                .sourceReportId(report.getId())
                .batchNo(batchNo)
                .parentBatchNo(firstNotBlank(report.getParentProductionBatchNo(), report.getSourceBatchNo()))
                .outputQty(outputQty)
                .uom("pcs")
                .modelNo(firstNotBlank(report.getModelCode(), planOrder.getModelCode(), planOrder.getModelName()))
                .businessRemark("裁切扫码确认产出入中间边库；来源计划：" + firstNotBlank(planOrder.getPlanNo(), "-")
                        + "；来源批号：" + firstNotBlank(report.getSourceProductionBatchNo(), report.getSourceBatchNo(), "-"))
                .txnRemark("裁切扫码确认自动入中间边库")
                .postTime(postTime)
                .operatorId(SecurityFrameworkUtils.getLoginUserId())
                .operatorName(firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .build());
        HcCutRoundReportDO stockUpdate = new HcCutRoundReportDO();
        stockUpdate.setId(report.getId());
        stockUpdate.setOutputStockId(stock.getId());
        stockUpdate.setOutputStockPostStatus(STOCK_POST_STATUS_POSTED);
        stockUpdate.setOutputStockPostTime(stock.getLastTxnTime());
        stockUpdate.setOutputStockPostMessage("裁切扫码确认自动入中间边库");
        hcCutRoundReportMapper.updateById(stockUpdate);
        report.setProductionBatchNo(batchNo);
        report.setOutputStockId(stock.getId());
        report.setOutputStockPostStatus(STOCK_POST_STATUS_POSTED);
        report.setOutputStockPostTime(stock.getLastTxnTime());
        report.setOutputStockPostMessage(stockUpdate.getOutputStockPostMessage());
    }

    private boolean isCutRoundCoaFlag(HcAdhesive2ReportDO source) {
        if (source == null) {
            return false;
        }
        if (isTruthy(parseExtra(source.getExtraJson()).get("coaFlag"))) {
            return true;
        }
        if (source.getSourcePressSlotReportId() == null) {
            return false;
        }
        HcPressSlotReportDO pressSlotReport = hcPressSlotReportMapper.selectById(source.getSourcePressSlotReportId());
        return pressSlotReport != null && isTruthy(parseExtra(pressSlotReport.getExtraJson()).get("coaFlag"));
    }

    private boolean isCutRoundSourceFromPressSlotProcessCheck(HcAdhesive2ReportDO source) {
        if (source == null || source.getSourcePressSlotReportId() == null) {
            return false;
        }
        HcPressSlotReportDO pressSlotReport = hcPressSlotReportMapper.selectById(source.getSourcePressSlotReportId());
        return isPressSlotProcessCheckReport(pressSlotReport);
    }

    private boolean isPressSlotProcessCheckReport(HcPressSlotReportDO source) {
        if (source == null) {
            return false;
        }
        Map<String, Object> extra = parseExtra(source.getExtraJson());
        return "PROCESS_CHECK".equalsIgnoreCase(StrUtil.trimToEmpty(extraString(extra, "reportType")))
                || "PROCESS_CHECK".equalsIgnoreCase(StrUtil.trimToEmpty(extraString(extra, "inspectionScene")))
                || "PROCESS_CHECK".equalsIgnoreCase(StrUtil.trimToEmpty(extraString(extra, "inspectionSampleCategory")));
    }

    private String buildCutRoundSourceExtraJson(HcAdhesive2ReportDO source, boolean coaFlag) {
        Map<String, Object> extra = new LinkedHashMap<>(parseExtra(source == null ? null : source.getExtraJson()));
        if (coaFlag) {
            extra.put("coaFlag", true);
            extra.putIfAbsent("coaFlagName", "成品COA专用");
        }
        if (source != null && isAdhesive2SourceNg(source)) {
            String attributedProcessName = firstNotBlank(
                    extraString(extra, "sourceNgProcessName"),
                    extraString(extra, "ngAttributionProcessName"),
                    source.getOperationName(), "粘胶2");
            String reason = firstNotBlank(
                    extraString(extra, "sourceNgReason"),
                    extraString(extra, "ngAttributionLabel"),
                    source.getQualityLockReason(), source.getDefectCode(), "粘胶2自检NG");
            extra.put("sourceNgProcessName", attributedProcessName);
            extra.put("sourceNgReason", reason);
            extra.put("visualInspectionResult", firstNotBlank(source.getSelfCheck(), "NG"));
        }
        if (extra.isEmpty()) {
            return source == null ? null : source.getExtraJson();
        }
        return JsonUtils.toJsonString(extra);
    }

    private String buildCutRoundAbnormalExtraJson(String processName, String reason, String extraJson, String result) {
        Map<String, Object> extra = new LinkedHashMap<>(parseExtra(extraJson));
        extra.put("sourceNgProcessName", processName);
        extra.put("sourceNgReason", reason);
        extra.put("visualInspectionResult", firstNotBlank(result, "NG"));
        return JsonUtils.toJsonString(extra);
    }

    private boolean isAdhesive2SourceNg(HcAdhesive2ReportDO source) {
        if (source == null) {
            return true;
        }
        if (isNgValue(source.getSelfCheck())
                || isNgValue(source.getDefectCode())
                || isNgValue(source.getProductQualityStatus())
                || isNgValue(source.getQualityLockReason())) {
            return true;
        }
        Map<String, Object> extra = parseExtra(source.getExtraJson());
        return isNgValue(extra.get("selfCheck"))
                || isNgValue(extra.get("visualInspectionResult"))
                || isNgValue(extra.get("inspectionResult"))
                || hasNgVisualItem(extra.get("visualItems"))
                || hasNgVisualItem(extra.get("visualInspectionItems"));
    }

    private boolean hasAdhesive2TailSelection(HcAdhesive2ReportDO source) {
        return source != null && StrUtil.isNotBlank(source.getActualSizeRule())
                && StrUtil.isNotBlank(source.getActualSizeSuffix());
    }

    private CutRoundQualityRisk resolveQualityRisk(HcCutRoundReportDO report) {
        if (report == null) {
            return new CutRoundQualityRisk(QUALITY_RISK_NONE, null);
        }
        String riskFlag = StrUtil.trimToEmpty(report.getQualityRiskFlag()).toUpperCase();
        if (isSupportedQualityRiskFlag(riskFlag) && StrUtil.isNotBlank(report.getQualityRiskSnapshotJson())) {
            return new CutRoundQualityRisk(riskFlag, report.getQualityRiskSnapshotJson());
        }
        HcAdhesive2ReportDO source = report.getSourceAdhesive2ReportId() == null ? null
                : hcAdhesive2ReportMapper.selectById(report.getSourceAdhesive2ReportId());
        return buildQualityRisk(source, report.getSelfCheck(), report.getDefectCode(), report.getRemark(),
                report.getExtraJson());
    }

    private CutRoundQualityRisk buildQualityRisk(HcAdhesive2ReportDO source, String cutRoundSelfCheck,
                                                 String cutRoundDefectCode, String cutRoundRemark,
                                                 String cutRoundExtraJson) {
        boolean sourceNg = source != null && isAdhesive2SourceNg(source);
        boolean cutRoundNg = isNgValue(cutRoundSelfCheck);
        String riskFlag = sourceNg && cutRoundNg ? QUALITY_RISK_BOTH_NG
                : sourceNg ? QUALITY_RISK_ADHESIVE2_NG
                : cutRoundNg ? QUALITY_RISK_CUT_ROUND_NG : QUALITY_RISK_NONE;
        Map<String, Object> sourceExtra = parseExtra(source == null ? null : source.getExtraJson());
        Map<String, Object> cutRoundExtra = parseExtra(cutRoundExtraJson);
        List<Map<String, String>> sourceDefectItems = sourceNg
                ? buildNgDefectItems(sourceExtra, source == null ? null : source.getDefectCode(),
                firstNotBlank(source == null ? null : source.getQualityLockReason(), "粘胶2自检NG"))
                : Collections.emptyList();
        List<Map<String, String>> cutRoundDefectItems = cutRoundNg
                ? buildNgDefectItems(cutRoundExtra, cutRoundDefectCode,
                firstNotBlank(cutRoundRemark, "裁切自检NG"))
                : Collections.emptyList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("riskFlag", riskFlag);
        snapshot.put("sourceNg", sourceNg);
        snapshot.put("sourceProcessCode", sourceNg ? PROCESS_CODE_ADHESIVE2 : null);
        snapshot.put("sourceProcessName", sourceNg ? "粘胶2" : null);
        snapshot.put("sourceReason", sourceNg ? firstNotBlank(
                extraString(sourceExtra, "sourceNgReason"),
                extraString(sourceExtra, "ngAttributionLabel"),
                source == null ? null : source.getQualityLockReason(),
                source == null ? null : source.getDefectCode(), "粘胶2自检NG") : null);
        snapshot.put("sourceSelfCheck", source == null ? null : source.getSelfCheck());
        snapshot.put("sourceDefectCode", source == null ? null : source.getDefectCode());
        snapshot.put("sourceQualityLockReason", source == null ? null : source.getQualityLockReason());
        snapshot.put("sourceDefectItems", sourceDefectItems);
        snapshot.put("sourceNgReviewResult", sourceNg ? firstNotBlank(
                extraString(cutRoundExtra, "sourceNgReviewResult"), cutRoundNg ? "NG" : "OK") : null);
        snapshot.put("sourceNgReviewTime", sourceNg ? extraString(cutRoundExtra, "sourceNgReviewTime") : null);
        snapshot.put("cutRoundNg", cutRoundNg);
        snapshot.put("cutRoundProcessCode", cutRoundNg ? "CUT_ROUND" : null);
        snapshot.put("cutRoundProcessName", cutRoundNg ? "裁切" : null);
        snapshot.put("cutRoundReason", cutRoundNg ? firstNotBlank(cutRoundDefectCode, cutRoundRemark, "裁切自检NG") : null);
        snapshot.put("cutRoundSelfCheck", cutRoundSelfCheck);
        snapshot.put("cutRoundDefectCode", cutRoundDefectCode);
        snapshot.put("cutRoundDefectItems", cutRoundDefectItems);
        snapshot.put("cutRoundNgSummary", joinNgDefectItemNames(cutRoundDefectItems));
        return new CutRoundQualityRisk(riskFlag, JsonUtils.toJsonString(snapshot));
    }

    /**
     * 统一从外观检验项目中提取 NG 项。裁切保存时用当前裁切外观项目；粘胶2来源快照则用其原始外观项目。
     * 只有没有结构化外观项目时，才退回使用单据上的缺陷码或异常说明。
     */
    private List<Map<String, String>> buildNgDefectItems(Map<String, Object> extra, String fallbackDefectCode,
                                                           String fallbackReason) {
        Object visualItems = firstNonNullObject(extra.get("visualItems"), extra.get("visualInspectionItems"),
                extra.get("sourceNgVisualItems"));
        List<Map<String, String>> result = new ArrayList<>();
        if (visualItems instanceof List<?> rows) {
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> item)) {
                    continue;
                }
                String itemName = firstNotBlank(mapValue(item, "itemName"), mapValue(item, "name"),
                        mapValue(item, "defectName"), mapValue(item, "checkItem"), mapValue(item, "label"));
                String resultValue = firstNotBlank(mapValue(item, "result"), mapValue(item, "checkResult"),
                        mapValue(item, "status"), mapValue(item, "ok"));
                if (!isNgValue(resultValue)) {
                    continue;
                }
                String defectCode = firstNotBlank(mapValue(item, "defectCode"), mapValue(item, "inheritedDefectCode"),
                        fallbackDefectCode, itemName);
                String defectName = firstNotBlank(itemName, defectCode, "外观缺陷");
                String remark = firstNotBlank(mapValue(item, "remark"), mapValue(item, "description"), fallbackReason);
                addNgDefectItem(result, defectCode, defectName, remark);
            }
        }
        if (result.isEmpty() && StrUtil.isNotBlank(firstNotBlank(fallbackDefectCode, fallbackReason))) {
            String defectCode = firstNotBlank(fallbackDefectCode, fallbackReason, "外观缺陷");
            addNgDefectItem(result, defectCode, firstNotBlank(fallbackReason, defectCode), fallbackReason);
        }
        return result;
    }

    private void validateVisualAbnormalCategory(String category, String reason) {
        if (!VISUAL_CATEGORY_NAMES.contains(category)) {
            throw invalidParamException("异常类别只能选择黑点、蓝点、黄点、红点、针孔、条纹、褶皱、波浪纹或其他");
        }
        if (StrUtil.isBlank(reason)) {
            throw invalidParamException("修正原因不能为空");
        }
    }

    private List<String> resolveActiveVisualCategories(Object source) {
        return normalizeVisualItemRows(source).stream()
                .filter(item -> isNgValue(item.get("result")) || StrUtil.isNotBlank(toStringValue(item.get("remark"))))
                .map(item -> StrUtil.trimToEmpty(toStringValue(item.get("itemName"))))
                .filter(VISUAL_CATEGORY_NAMES::contains)
                .distinct()
                .toList();
    }

    private List<Map<String, Object>> buildCorrectedVisualItems(Object source, String category) {
        Map<String, Map<String, Object>> existedMap = new LinkedHashMap<>();
        for (Map<String, Object> row : normalizeVisualItemRows(source)) {
            String itemName = StrUtil.trimToEmpty(toStringValue(row.get("itemName")));
            if (VISUAL_CATEGORY_NAMES.contains(itemName) && !existedMap.containsKey(itemName)) {
                existedMap.put(itemName, row);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String itemName : VISUAL_CATEGORY_NAMES) {
            Map<String, Object> existed = existedMap.get(itemName);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemName", itemName);
            item.put("remark", itemName.equals(category) && existed != null
                    ? StrUtil.trimToEmpty(toStringValue(existed.get("remark"))) : "");
            item.put("result", itemName.equals(category) ? "NG" : "OK");
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> normalizeVisualItemRows(Object source) {
        Object rowsSource = source;
        if (rowsSource instanceof String text && StrUtil.isNotBlank(text)) {
            List<Map<String, Object>> parsedList = JsonUtils.parseObjectQuietly(text,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (parsedList != null) {
                rowsSource = parsedList;
            } else {
                Map<String, Object> parsedMap = JsonUtils.parseObjectQuietly(text,
                        new TypeReference<Map<String, Object>>() {});
                rowsSource = parsedMap == null ? null : firstNonNullObject(parsedMap.get("visualItems"),
                        parsedMap.get("visualInspectionItems"), parsedMap.get("items"), parsedMap.get("details"),
                        parsedMap.get("list"));
            }
        } else if (rowsSource instanceof Map<?, ?> map) {
            rowsSource = firstNonNullObject(map.get("visualItems"), map.get("visualInspectionItems"),
                    map.get("items"), map.get("details"), map.get("list"));
        }
        if (!(rowsSource instanceof List<?> rows)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> raw)) {
                continue;
            }
            String itemName = firstNotBlank(toStringValue(raw.get("itemName")), toStringValue(raw.get("name")),
                    toStringValue(raw.get("defectName")), toStringValue(raw.get("checkItem")),
                    toStringValue(raw.get("label")));
            if (StrUtil.isBlank(itemName)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemName", itemName);
            item.put("remark", firstNotBlank(toStringValue(raw.get("remark")), toStringValue(raw.get("value")),
                    toStringValue(raw.get("actualValue")), toStringValue(raw.get("description")),
                    toStringValue(raw.get("text"))));
            item.put("result", firstNotBlank(toStringValue(raw.get("result")), toStringValue(raw.get("checkResult")),
                    toStringValue(raw.get("status")), toStringValue(raw.get("ok"))));
            result.add(item);
        }
        return result;
    }

    private void applyVisualAbnormalCategoryCorrectionExtra(Map<String, Object> extra, List<String> beforeCategories,
                                                           String category, String reason, String correctedAt,
                                                           Long correctedById, String correctedByName) {
        Map<String, Object> correction = new LinkedHashMap<>();
        correction.put("beforeCategories", beforeCategories == null ? Collections.emptyList() : beforeCategories);
        correction.put("afterCategory", category);
        correction.put("reason", reason);
        correction.put("correctedAt", correctedAt);
        correction.put("correctedById", correctedById);
        correction.put("correctedByName", correctedByName);

        List<Object> history = new ArrayList<>();
        Object rawHistory = extra.get("abnormalCategoryCorrectionHistory");
        if (rawHistory instanceof List<?> rawList) {
            history.addAll(rawList);
        } else if (rawHistory != null) {
            history.add(rawHistory);
        }
        history.add(correction);

        extra.put("visualItems", buildCorrectedVisualItems(extra, category));
        extra.put("visualInspectionResult", "NG");
        extra.put("selfCheck", "NG");
        extra.put("abnormalCategoryCorrected", true);
        extra.put("correctedVisualCategory", category);
        extra.put("abnormalCategoryCorrectionReason", reason);
        extra.put("abnormalCategoryCorrectionTime", correctedAt);
        extra.put("abnormalCategoryCorrectionUserId", correctedById);
        extra.put("abnormalCategoryCorrectionUserName", correctedByName);
        extra.put("abnormalCategoryCorrectionHistory", history);
    }

    private String buildVisualAbnormalCategoryCorrectionRemark(String originalRemark, List<String> beforeCategories,
                                                               String category, String correctedAt,
                                                               String correctedByName) {
        String correctedRemark = correctVisualCategoryText(originalRemark, beforeCategories, category);
        String auditRemark = "异常类别已修正，时间：" + correctedAt + "，修正人：" + correctedByName + "，原因已记录";
        if (StrUtil.isBlank(correctedRemark)) {
            return auditRemark;
        }
        if (correctedRemark.contains(auditRemark)) {
            return correctedRemark;
        }
        return correctedRemark + "；" + auditRemark;
    }

    private String correctVisualCategoryText(String source, List<String> beforeCategories, String category) {
        String text = StrUtil.trimToEmpty(source);
        if (StrUtil.isBlank(text)) {
            return source;
        }
        List<String> effectiveBeforeCategories = new ArrayList<>(beforeCategories == null
                ? Collections.emptyList() : beforeCategories);
        if (effectiveBeforeCategories.isEmpty()) {
            for (String itemName : VISUAL_CATEGORY_NAMES) {
                if (!StrUtil.equals(itemName, category) && text.contains(itemName)) {
                    effectiveBeforeCategories.add(itemName);
                }
            }
        }
        String corrected = text;
        for (String beforeCategory : effectiveBeforeCategories) {
            if (!StrUtil.equals(beforeCategory, category) && StrUtil.isNotBlank(beforeCategory)) {
                corrected = corrected.replace(beforeCategory, category);
            }
        }
        return corrected;
    }

    private Object firstNonNullObject(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String mapValue(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private void addNgDefectItem(List<Map<String, String>> items, String defectCode, String defectName, String remark) {
        String key = firstNotBlank(defectCode, defectName, remark);
        if (items.stream().anyMatch(item -> key.equals(firstNotBlank(item.get("defectCode"), item.get("defectName"), item.get("remark"))))) {
            return;
        }
        Map<String, String> item = new LinkedHashMap<>();
        item.put("defectCode", defectCode);
        item.put("defectName", defectName);
        item.put("remark", remark);
        items.add(item);
    }

    private String joinNgDefectItemNames(List<Map<String, String>> items) {
        return items.stream()
                .map(item -> firstNotBlank(item.get("defectName"), item.get("defectCode"), item.get("remark")))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("；"));
    }

    private boolean isSupportedQualityRiskFlag(String riskFlag) {
        return QUALITY_RISK_NONE.equals(riskFlag)
                || QUALITY_RISK_ADHESIVE2_NG.equals(riskFlag)
                || QUALITY_RISK_CUT_ROUND_NG.equals(riskFlag)
                || QUALITY_RISK_BOTH_NG.equals(riskFlag);
    }

    private boolean hasNgVisualItem(Object value) {
        if (!(value instanceof List<?> rows)) {
            return false;
        }
        return rows.stream().anyMatch(row -> {
            if (!(row instanceof Map<?, ?> item)) {
                return false;
            }
            return isNgValue(item.get("result"))
                    || isNgValue(item.get("checkResult"))
                    || isNgValue(item.get("status"))
                    || isNgValue(item.get("ok"));
        });
    }

    private boolean isNgValue(Object value) {
        String text = StrUtil.trimToEmpty(value == null ? null : String.valueOf(value)).toUpperCase();
        return List.of("NG", "N", "FALSE", "ABNORMAL", "FAIL", "FAILED", "不合格", "异常").contains(text)
                || text.startsWith("NG")
                || text.contains("_NG")
                || text.contains("NG_")
                || text.contains(" NG")
                || text.contains("NG ")
                || text.contains("NG片")
                || text.contains("NG异常");
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = StrUtil.trimToEmpty(value == null ? null : String.valueOf(value));
        return "Y".equalsIgnoreCase(text) || "TRUE".equalsIgnoreCase(text) || "1".equals(text);
    }

    private HcAdhesiveReportRespVO buildReportRespWithCheckItems(HcCutRoundReportDO report) {
        HcAdhesiveReportRespVO resp = buildReportResp(report);
        resp.setCheckItems(hcCutRoundCheckDetailMapper.selectListByReportId(report.getId()).stream()
                .map(this::buildCheckItemResp)
                .toList());
        return resp;
    }

    private void validateUpstreamSampleUnlocked(List<HcCutRoundReportDO> reports) {
        for (HcCutRoundReportDO report : reports) {
            String reason = upstreamSampleLockService.getCutRoundLockReason(report);
            if (StrUtil.isNotBlank(reason)) {
                throw invalidParamException("裁切片 " + report.getProductionBatchNo() + "：" + reason);
            }
        }
    }

    private HcAdhesiveReportRespVO buildReportResp(HcCutRoundReportDO report) {
        HcAdhesiveReportRespVO resp = new HcAdhesiveReportRespVO();
        resp.setUpstreamSampleLockReason(upstreamSampleLockService.getCutRoundLockReason(report));
        resp.setId(report.getId());
        resp.setPlanId(report.getPlanId());
        resp.setPlanNo(report.getPlanNo());
        resp.setPlanOperationId(report.getPlanOperationId());
        resp.setSourceGrindingSecondDetailId(report.getSourceAdhesive2ReportId());
        resp.setSourceType("粘胶2片号");
        resp.setSourceBatchNo(report.getSourceBatchNo());
        resp.setSourceProductionBatchNo(report.getSourceProductionBatchNo());
        resp.setProductionBatchNo(report.getProductionBatchNo());
        resp.setParentProductionBatchNo(report.getParentProductionBatchNo());
        resp.setMaterialCode(report.getMaterialCode());
        resp.setMaterialName(report.getMaterialName());
        resp.setModelCode(report.getModelCode());
        resp.setReportDate(report.getReportDate());
        resp.setStartTime(report.getStartTime());
        resp.setEndTime(report.getEndTime());
        resp.setInputLength(report.getInputLength());
        resp.setOutputLength(report.getOutputLength());
        resp.setGlueBoardMaterialCode(report.getBladeMaterialCode());
        resp.setGlueBoardBatchNo(report.getBladeBatchNo());
        resp.setSelfCheck(report.getSelfCheck());
        resp.setDefectCode(report.getDefectCode());
        resp.setQualityRiskFlag(report.getQualityRiskFlag());
        resp.setQualityRiskSnapshotJson(report.getQualityRiskSnapshotJson());
        resp.setReportStatus(report.getReportStatus());
        applyFqcSnapshotToReportResp(resp, report.getId());
        resp.setInspectionTaskId(report.getInspectionTaskId());
        resp.setInspectionTaskNo(report.getInspectionTaskNo());
        resp.setInspectionStatus(report.getInspectionStatus());
        resp.setInspectionResult(report.getInspectionResult());
        resp.setInspectorName(report.getInspectorName());
        resp.setInspectionTime(report.getInspectionTime());
        resp.setInspectionRemark(report.getInspectionRemark());
        resp.setRecorderName(report.getRecorderName());
        resp.setRecorderTime(report.getRecorderTime());
        resp.setConfirmerName(report.getConfirmerName());
        resp.setConfirmerTime(report.getConfirmerTime());
        resp.setRemark(report.getRemark());
        resp.setExtraJson(report.getExtraJson());
        return resp;
    }

    private HcCutRoundInspectionTaskRespVO buildInspectionTaskResp(HcCutRoundInspectionTaskDO task,
                                                                    List<HcCutRoundInspectionDetailDO> details) {
        HcCutRoundInspectionTaskRespVO resp = new HcCutRoundInspectionTaskRespVO();
        resp.setId(task.getId());
        resp.setTaskNo(task.getTaskNo());
        resp.setPlanId(task.getPlanId());
        resp.setPlanNo(task.getPlanNo());
        resp.setPlanOperationId(task.getPlanOperationId());
        resp.setOperationCode(task.getOperationCode());
        resp.setOperationName(task.getOperationName());
        resp.setReportProcess(task.getReportProcess());
        resp.setReceiveLocation(task.getReceiveLocation());
        resp.setReportDate(task.getReportDate());
        resp.setReportTime(task.getReportTime());
        resp.setReporterName(task.getReporterName());
        resp.setReceiverName(task.getReceiverName());
        resp.setPriorityLevel(task.getPriorityLevel());
        resp.setExpectedFinishDate(task.getExpectedFinishDate());
        resp.setTaskStatus(task.getTaskStatus());
        HcCutRoundInspectionDetailDO fqcDetail = findFirstFqcDetail(details);
        if (fqcDetail != null) {
            resp.setFqcOrderId(fqcDetail.getFqcOrderId());
            resp.setFqcNo(fqcDetail.getFqcNo());
            resp.setFqcStatus(fqcDetail.getFqcStatus());
            resp.setFqcJudgment(fqcDetail.getFqcJudgment());
        }
        resp.setDetailCount(task.getDetailCount());
        resp.setRemark(task.getRemark());
        resp.setDetails(details.stream().map(this::buildInspectionDetailResp).toList());
        return resp;
    }

    private HcCutRoundInspectionTaskRespVO.Detail buildInspectionDetailResp(HcCutRoundInspectionDetailDO detail) {
        HcCutRoundInspectionTaskRespVO.Detail resp = new HcCutRoundInspectionTaskRespVO.Detail();
        resp.setId(detail.getId());
        resp.setTaskId(detail.getTaskId());
        resp.setCutRoundReportId(detail.getCutRoundReportId());
        resp.setSeqNo(detail.getSeqNo());
        resp.setParentProductionBatchNo(detail.getParentProductionBatchNo());
        resp.setMaterialCode(detail.getMaterialCode());
        resp.setMaterialName(detail.getMaterialName());
        resp.setModelCode(detail.getModelCode());
        resp.setSizeRule(detail.getSizeRule());
        resp.setProductionBatchNo(detail.getProductionBatchNo());
        resp.setQualityRiskFlag(detail.getQualityRiskFlag());
        resp.setQualityRiskSnapshotJson(detail.getQualityRiskSnapshotJson());
        resp.setFqcOrderId(detail.getFqcOrderId());
        resp.setFqcNo(detail.getFqcNo());
        resp.setFqcStatus(detail.getFqcStatus());
        resp.setFqcJudgment(detail.getFqcJudgment());
        resp.setInspectionResult(detail.getInspectionResult());
        resp.setInspectorName(detail.getInspectorName());
        resp.setInspectionTime(detail.getInspectionTime());
        resp.setRemark(detail.getRemark());
        return resp;
    }

    private void applyFqcSnapshotToReportResp(HcAdhesiveReportRespVO resp, Long reportId) {
        HcCutRoundInspectionDetailDO fqcDetail = findFirstFqcDetail(hcCutRoundInspectionDetailMapper.selectListByCutRoundReportId(reportId));
        if (fqcDetail == null) {
            return;
        }
        resp.setFqcOrderId(fqcDetail.getFqcOrderId());
        resp.setFqcNo(fqcDetail.getFqcNo());
        resp.setFqcStatus(fqcDetail.getFqcStatus());
        resp.setFqcJudgment(fqcDetail.getFqcJudgment());
    }

    private HcCutRoundInspectionDetailDO findFirstFqcDetail(List<HcCutRoundInspectionDetailDO> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        return details.stream()
                .filter(detail -> detail.getFqcOrderId() != null || StrUtil.isNotBlank(detail.getFqcNo()))
                .findFirst()
                .orElse(null);
    }

    private QmsFqcOrderDO createOrUpdateCutRoundFqc(HcPlanOrderDO planOrder, HcPlanOrderOperationDO operation,
                                                     HcCutRoundInspectionTaskDO task,
                                                     HcCutRoundInspectionDetailDO detail,
                                                     HcCutRoundReportDO report) {
        QmsFqcSaveReqVO reqVO = buildCutRoundFqcReq(planOrder, operation, task, detail, report);
        QmsFqcOrderDO existed = qmsFqcOrderMapper.selectLatestBySource(SOURCE_TYPE_CUT_ROUND, report.getId());
        if (existed == null) {
            Long fqcId = qmsFqcService.createFqc(reqVO);
            return qmsFqcOrderMapper.selectById(fqcId);
        }
        QmsFqcOrderDO update = new QmsFqcOrderDO();
        update.setId(existed.getId());
        update.setReportNo(reqVO.getReportNo());
        update.setWorkOrderNo(reqVO.getWorkOrderNo());
        update.setSourceReportId(reqVO.getSourceReportId());
        update.setSourceReportNo(reqVO.getSourceReportNo());
        update.setSourceModule(reqVO.getSourceModule());
        update.setSourceOperationCode(reqVO.getSourceOperationCode());
        update.setSourceOperationName(reqVO.getSourceOperationName());
        update.setPlanOrderId(reqVO.getPlanOrderId());
        update.setOperationCode(reqVO.getOperationCode());
        update.setOperationName(reqVO.getOperationName());
        update.setMachineId(reqVO.getMachineId());
        update.setMachineCode(reqVO.getMachineCode());
        update.setMachineName(reqVO.getMachineName());
        update.setMaterialId(reqVO.getMaterialId());
        update.setMaterialCode(reqVO.getMaterialCode());
        update.setMaterialName(reqVO.getMaterialName());
        update.setSpecification(reqVO.getSpecification());
        update.setProductModel(reqVO.getProductModel());
        update.setProductBatchNo(reqVO.getProductBatchNo());
        update.setBatchNo(reqVO.getBatchNo());
        update.setProduceQty(reqVO.getProduceQty());
        update.setUnitCode(reqVO.getUnitCode());
        update.setUnitName(reqVO.getUnitName());
        update.setSampleQty(reqVO.getSampleQty());
        update.setInspectionCategory(reqVO.getInspectionCategory());
        update.setSubmissionType(reqVO.getSubmissionType());
        update.setSubmissionTime(reqVO.getSubmissionTime());
        update.setSubmitterName(reqVO.getSubmitterName());
        update.setRemark(reqVO.getRemark());
        qmsFqcOrderMapper.updateById(update);
        return qmsFqcOrderMapper.selectById(existed.getId());
    }

    private QmsFqcSaveReqVO buildCutRoundFqcReq(HcPlanOrderDO planOrder, HcPlanOrderOperationDO operation,
                                                HcCutRoundInspectionTaskDO task,
                                                HcCutRoundInspectionDetailDO detail,
                                                HcCutRoundReportDO report) {
        QmsFqcSaveReqVO reqVO = new QmsFqcSaveReqVO();
        String productionBatchNo = firstNotBlank(report.getProductionBatchNo(), report.getSourceProductionBatchNo(),
                report.getSourceBatchNo(), String.valueOf(report.getId()));
        String materialCode = firstNotBlank(report.getMaterialCode(), planOrder.getMaterialCode());
        String materialName = firstNotBlank(report.getMaterialName(), planOrder.getMaterialName());
        String modelCode = firstNotBlank(report.getModelCode(), planOrder.getModelCode(), planOrder.getModelName());
        reqVO.setReportNo(task.getTaskNo() + "-" + String.format("%03d", detail.getSeqNo() == null ? 1 : detail.getSeqNo()));
        reqVO.setWorkOrderNo(firstNotBlank(planOrder.getPlanNo(), task.getPlanNo(), task.getTaskNo()));
        reqVO.setSourceReportId(report.getId());
        reqVO.setSourceReportNo(productionBatchNo);
        reqVO.setSourceModule(SOURCE_TYPE_CUT_ROUND);
        reqVO.setSourceOperationCode(operation.getOpCode());
        reqVO.setSourceOperationName(operation.getOpName());
        reqVO.setPlanOrderId(planOrder.getId());
        reqVO.setOperationCode(operation.getOpCode());
        reqVO.setOperationName(operation.getOpName());
        reqVO.setMachineId(operation.getEquipmentId());
        reqVO.setMachineCode(operation.getEquipmentCode());
        reqVO.setMachineName(operation.getEquipmentName());
        reqVO.setMaterialId(planOrder.getMaterialId());
        reqVO.setMaterialCode(materialCode);
        reqVO.setMaterialName(materialName);
        reqVO.setSpecification(firstNotBlank(detail.getSizeRule(), planOrder.getSizeSpec(), planOrder.getSizeName()));
        reqVO.setProductModel(modelCode);
        reqVO.setProductBatchNo(productionBatchNo);
        reqVO.setBatchNo(productionBatchNo);
        reqVO.setProduceQty(BigDecimal.ONE);
        reqVO.setUnitCode(firstNotBlank(planOrder.getTargetUnitCode(), operation.getUnitCode(), "PCS"));
        reqVO.setUnitName(firstNotBlank(planOrder.getTargetUnitName(), operation.getUnitName(), operation.getUom(), "片"));
        reqVO.setSampleQty(1);
        reqVO.setInspectionCategory("FINAL");
        reqVO.setSubmissionType(resolveFqcSubmissionType(planOrder));
        reqVO.setSubmissionTime(normalizeValidLocalDateTime(task.getReportTime(),
                normalizeValidLocalDateTime(task.getCreateTime(), LocalDateTime.now())));
        reqVO.setSubmitterName(firstNotBlank(task.getReporterName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        reqVO.setStatus(FQC_STATUS_PENDING);
        reqVO.setJudgment(FQC_JUDGMENT_PENDING);
        reqVO.setRemark("来源裁切报检单：" + task.getTaskNo() + "；片号：" + productionBatchNo);
        return reqVO;
    }

    private String resolveFqcSubmissionType(HcPlanOrderDO planOrder) {
        String text = (StrUtil.blankToDefault(planOrder.getPlanMode(), "") + " "
                + StrUtil.blankToDefault(planOrder.getSourceType(), "") + " "
                + StrUtil.blankToDefault(planOrder.getProdType(), "") + " "
                + StrUtil.blankToDefault(planOrder.getProdTypeName(), "")).toUpperCase();
        return text.contains("RD") || text.contains("RND") || text.contains("研发") ? "RND" : "MASS_SHIPMENT";
    }

    private void applyFqcSnapshotToInspectionDetail(HcCutRoundInspectionDetailDO detail, QmsFqcOrderDO fqcOrder) {
        HcCutRoundInspectionDetailDO update = new HcCutRoundInspectionDetailDO();
        update.setId(detail.getId());
        update.setFqcOrderId(fqcOrder.getId());
        update.setFqcNo(fqcOrder.getFqcNo());
        update.setFqcStatus(fqcOrder.getStatus());
        update.setFqcJudgment(fqcOrder.getJudgment());
        if (FQC_STATUS_COMPLETED.equals(fqcOrder.getStatus())) {
            update.setInspectionResult(FQC_JUDGMENT_OK);
            update.setInspectorName(firstNotBlank(fqcOrder.getInspectorName(), fqcOrder.getQaInspectorName()));
            update.setInspectionTime(fqcOrder.getInspectionTime() == null ? fqcOrder.getQaTime() : fqcOrder.getInspectionTime());
        } else if (FQC_STATUS_REJECTED.equals(fqcOrder.getStatus())) {
            update.setInspectionResult(FQC_JUDGMENT_NG);
            update.setInspectorName(firstNotBlank(fqcOrder.getInspectorName(), fqcOrder.getQaInspectorName()));
            update.setInspectionTime(fqcOrder.getInspectionTime() == null ? fqcOrder.getQaTime() : fqcOrder.getInspectionTime());
        }
        hcCutRoundInspectionDetailMapper.updateById(update);
    }

    private String resolveInspectionResultFromFqc(QmsFqcOrderDO fqcOrder) {
        if (FQC_STATUS_COMPLETED.equals(fqcOrder.getStatus()) && FQC_JUDGMENT_OK.equals(fqcOrder.getJudgment())) {
            return FQC_JUDGMENT_OK;
        }
        if (FQC_STATUS_REJECTED.equals(fqcOrder.getStatus()) || FQC_JUDGMENT_NG.equals(fqcOrder.getJudgment())) {
            return FQC_JUDGMENT_NG;
        }
        return null;
    }

    private String buildInspectionTaskNo(HcPlanOrderOperationDO operation, LocalDateTime now) {
        return "CUTQI" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "-" + (operation.getId() == null ? "0" : operation.getId());
    }

    private void validateInspectionReportsInSameSegment(List<HcCutRoundReportDO> reports) {
        List<String> segmentBatchNos = reports.stream()
                .map(this::resolveInspectionSegmentBatchNo)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (segmentBatchNos.size() > 1) {
            throw invalidParamException("不同分段批次的裁切片需分开送检，请按 Q/R/S 段分别提交检验");
        }
    }

    private String resolveInspectionSegmentBatchNo(HcCutRoundReportDO report) {
        if (report == null) {
            return "";
        }
        String fallback = "";
        String[] batchNos = {
                report.getParentProductionBatchNo(),
                report.getSourceBatchNo(),
                report.getSourceProductionBatchNo(),
                report.getProductionBatchNo()
        };
        for (String batchNo : batchNos) {
            String normalized = normalizeInspectionSegmentBatchNo(batchNo);
            if (StrUtil.isBlank(normalized)) {
                continue;
            }
            if (isInspectionSegmentBatchNo(normalized)) {
                return normalized;
            }
            if (StrUtil.isBlank(fallback)) {
                fallback = normalized;
            }
        }
        return fallback;
    }

    private String normalizeInspectionSegmentBatchNo(String batchNo) {
        String normalized = StrUtil.trimToEmpty(batchNo)
                .replaceAll("(?i)-(?:J|S)\\d+$", "")
                .toUpperCase();
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        return normalized.replaceFirst("(?i)^(.+[PQRS])\\d{3}[A-Z]?$", "$1");
    }

    private boolean isInspectionSegmentBatchNo(String batchNo) {
        return StrUtil.isNotBlank(batchNo) && batchNo.matches("(?i)^.*[PQRS]$");
    }

    private HcAdhesiveCheckItemRespVO buildCutRoundCheckTemplateResp(HcStationFormItemDO item) {
        HcAdhesiveCheckItemRespVO resp = new HcAdhesiveCheckItemRespVO();
        resp.setId(item.getId());
        resp.setItemCategory(item.getItemCategory());
        resp.setItemName(item.getItemName());
        resp.setStandardValue(item.getStandardText());
        resp.setCheckResult(firstNotBlank(item.getDefaultResult(), "OK"));
        resp.setValueMode(firstNotBlank(item.getValueMode(), "TEXT"));
        resp.setDualLabel1(item.getDualLabel1());
        resp.setDualLabel2(item.getDualLabel2());
        resp.setRequiredFlag(Boolean.TRUE.equals(item.getRequiredFlag()));
        resp.setSortNo(item.getItemSeq());
        return resp;
    }

    private void syncCutRoundProductionCheckProcessFormRecord(HcCutRoundReportDO report,
                                                              HcPlanOrderDO planOrder,
                                                              HcPlanOrderOperationDO operation) {
        if (report == null || report.getId() == null || planOrder == null || operation == null) {
            return;
        }
        HcStationFormDO form = requireCutRoundProductionCheckForm(firstNotBlank(
                report.getModelCode(), planOrder.getModelCode(), planOrder.getModelName()));
        List<HcCutRoundCheckDetailDO> details = hcCutRoundCheckDetailMapper.selectListByReportId(report.getId());
        if (details.isEmpty()) {
            return;
        }
        String productionBatchNo = firstNotBlank(report.getProductionBatchNo(), report.getSourceProductionBatchNo(),
                report.getSourceBatchNo(), report.getParentProductionBatchNo());
        String modelCode = firstNotBlank(report.getModelCode(), planOrder.getModelCode(), planOrder.getModelName());
        String templateName = resolveCutRoundStationDisplayName(form);
        Map<String, Object> headerData = new LinkedHashMap<>();
        headerData.put("productionDate", report.getReportDate());
        headerData.put("recordDate", report.getReportDate());
        headerData.put("planNo", planOrder.getPlanNo());
        headerData.put("batchNo", productionBatchNo);
        headerData.put("productionBatchNo", productionBatchNo);
        headerData.put("sourceProductionBatchNo", report.getSourceProductionBatchNo());
        headerData.put("parentProductionBatchNo", report.getParentProductionBatchNo());
        headerData.put("modelCode", modelCode);
        headerData.put("modelName", firstNotBlank(planOrder.getModelName(), modelCode));
        headerData.put("materialCode", firstNotBlank(report.getMaterialCode(), planOrder.getMaterialCode()));
        headerData.put("materialName", firstNotBlank(report.getMaterialName(), planOrder.getMaterialName()));
        headerData.put("equipmentCode", operation.getEquipmentCode());
        headerData.put("equipmentName", operation.getEquipmentName());
        headerData.put("startTime", report.getStartTime());
        headerData.put("endTime", report.getEndTime());
        headerData.put("recorderName", report.getRecorderName());
        headerData.put("recorderTime", report.getRecorderTime());
        headerData.put("confirmerName", report.getConfirmerName());
        headerData.put("confirmerTime", report.getConfirmerTime());
        headerData.put("sourceExcel", toStringValue(parseCutRoundStationFormSchema(form).get("sourceExcel")));

        Map<String, Object> contextData = new LinkedHashMap<>();
        contextData.put("sourceType", SOURCE_TYPE_CUT_ROUND);
        contextData.put("sourceTable", SOURCE_TABLE_CUT_ROUND_REPORT);
        contextData.put("sourceReportId", report.getId());
        contextData.put("sourceMenuCode", SOURCE_MENU_CODE);
        contextData.put("renderMode", "CUT_ROUND_PRODUCTION_CHECK");
        contextData.put("templateFormCode", form.getFormCode());
        contextData.put("templateFormName", form.getFormName());

        String recordNo = CUT_ROUND_PROCESS_FORM_RECORD_PREFIX + report.getId();
        HcProcessFormRecordDO existing = hcProcessFormRecordMapper.selectOneByRecordNo(recordNo);
        HcProcessFormRecordDO record = existing == null ? new HcProcessFormRecordDO() : existing;
        record.setRecordNo(recordNo);
        record.setTemplateId(form.getId());
        record.setVersionId(form.getId());
        record.setTemplateCode(form.getFormCode());
        record.setTemplateName(templateName);
        record.setProcessCode(PROCESS_CODE);
        record.setProcessName(firstNotBlank(operation.getOpName(), "裁切"));
        record.setModelCode(modelCode);
        record.setModelName(firstNotBlank(planOrder.getModelName(), modelCode));
        record.setFormType(FORM_TYPE_PRODUCTION_CHECK);
        record.setFormTypeName("工艺点检");
        record.setRecordDate(report.getReportDate() == null ? LocalDate.now() : report.getReportDate());
        record.setPlanId(planOrder.getId());
        record.setPlanNo(planOrder.getPlanNo());
        record.setPlanOperationId(operation.getId());
        record.setBatchNo(productionBatchNo);
        record.setEquipmentId(operation.getEquipmentId());
        record.setEquipmentCode(operation.getEquipmentCode());
        record.setEquipmentName(operation.getEquipmentName());
        record.setRecordStatus(firstNotBlank(report.getReportStatus(), "DRAFT"));
        record.setResultStatus(resolveCutRoundProductionCheckResult(details, report));
        record.setFillUserId(SecurityFrameworkUtils.getLoginUserId());
        record.setFillUserName(firstNotBlank(report.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        record.setFillTime(firstNotNullDateTime(report.getRecorderTime(), report.getCreateTime(), LocalDateTime.now()));
        record.setConfirmUserName(report.getConfirmerName());
        record.setConfirmTime(report.getConfirmerTime());
        record.setHeaderDataJson(JsonUtils.toJsonString(headerData));
        record.setContextJson(JsonUtils.toJsonString(contextData));
        record.setRemark(report.getRemark());
        record.setTenantId(planOrder.getTenantId());

        if (record.getId() == null) {
            hcProcessFormRecordMapper.insert(record);
        } else {
            hcProcessFormRecordMapper.updateById(record);
            hcProcessFormRecordItemMapper.deleteByRecordId(record.getId());
        }
        List<HcProcessFormRecordItemDO> items = buildCutRoundProductionCheckProcessFormItems(record, details, form);
        if (!items.isEmpty()) {
            hcProcessFormRecordItemMapper.insertBatch(items);
        }
    }

    private List<HcProcessFormRecordItemDO> buildCutRoundProductionCheckProcessFormItems(HcProcessFormRecordDO record,
                                                                                         List<HcCutRoundCheckDetailDO> details,
                                                                                         HcStationFormDO form) {
        Map<Integer, HcStationFormItemDO> templateItems = new LinkedHashMap<>();
        hcStationFormItemMapper.selectByFormId(form.getId()).forEach(item -> templateItems.put(item.getItemSeq(), item));
        List<HcProcessFormRecordItemDO> items = new ArrayList<>();
        for (HcCutRoundCheckDetailDO detail : details) {
            Integer seq = detail.getSortNo() == null ? items.size() + 1 : detail.getSortNo();
            HcStationFormItemDO templateItem = templateItems.get(seq);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sourceDetailId", detail.getId());
            row.put("itemCategory", detail.getItemCategory());
            row.put("itemName", detail.getItemName());
            row.put("standardValue", detail.getStandardValue());
            row.put("actualValue", detail.getActualValue());
            row.put("checkResult", detail.getCheckResult());
            row.put("abnormalRemark", detail.getAbnormalRemark());
            HcProcessFormRecordItemDO item = new HcProcessFormRecordItemDO();
            item.setRecordId(record.getId());
            item.setTemplateItemId(templateItem == null ? null : templateItem.getId());
            item.setItemSeq(seq);
            item.setFieldKey("cut_round_check_" + seq);
            item.setFieldLabel(firstNotBlank(detail.getItemName(), templateItem == null ? null : templateItem.getItemName()));
            item.setItemCategory(firstNotBlank(detail.getItemCategory(), templateItem == null ? null : templateItem.getItemCategory()));
            item.setStepNode(templateItem == null ? null : templateItem.getStepNode());
            item.setStandardText(firstNotBlank(detail.getStandardValue(), templateItem == null ? null : templateItem.getStandardText()));
            item.setValueMode(templateItem == null ? "TEXT" : firstNotBlank(templateItem.getValueMode(), "TEXT"));
            item.setActualValue(detail.getActualValue());
            item.setResultFlag(firstNotBlank(detail.getCheckResult(), "OK"));
            item.setAbnormalRemark(detail.getAbnormalRemark());
            item.setSourceRowJson(JsonUtils.toJsonString(row));
            item.setTenantId(record.getTenantId());
            items.add(item);
        }
        return items;
    }

    private String resolveCutRoundProductionCheckResult(List<HcCutRoundCheckDetailDO> details,
                                                        HcCutRoundReportDO report) {
        if (details.stream().anyMatch(item -> "NG".equalsIgnoreCase(StrUtil.blankToDefault(item.getCheckResult(), "")))) {
            return "NG";
        }
        return firstNotBlank(report.getSelfCheck(), "OK");
    }

    private HcStationFormDO requireCutRoundProductionCheckForm(String modelCode) {
        HcStationFormDO form = resolveCutRoundProductionCheckForm(modelCode);
        if (form == null) {
            throw invalidParamException("裁切生产点检表模板配置没有找到配置，请联系管理员！");
        }
        return form;
    }

    private HcStationFormDO resolveCutRoundProductionCheckForm(String modelCode) {
        String normalizedModel = normalizeCutRoundModelCode(modelCode);
        String materialType = resolveCutRoundMaterialType(normalizedModel);
        List<HcStationFormDO> forms = hcStationFormMapper.selectEnabledByProcess(PROCESS_CODE).stream()
                .filter(form -> FORM_TYPE_PRODUCTION_CHECK.equalsIgnoreCase(resolveCutRoundStationProcessFormType(form)))
                .filter(form -> getCutRoundStationFormModelMatchScore(form, normalizedModel, materialType) >= 0)
                .sorted(Comparator.comparingInt((HcStationFormDO form) ->
                                getCutRoundStationFormModelMatchScore(form, normalizedModel, materialType))
                        .thenComparing(form -> form.getSortNo() == null ? Integer.MAX_VALUE : form.getSortNo())
                        .thenComparing(form -> form.getId() == null ? Long.MAX_VALUE : form.getId()))
                .toList();
        return forms.isEmpty() ? null : forms.get(0);
    }

    private boolean isCutRoundDailyStationForm(HcStationFormDO form) {
        String formType = resolveCutRoundStationProcessFormType(form);
        return FORM_TYPE_STARTUP_CHECK.equalsIgnoreCase(formType) || FORM_TYPE_CLEANING_CHECK.equalsIgnoreCase(formType);
    }

    private String resolveCutRoundStationProcessFormType(HcStationFormDO form) {
        Map<String, Object> schema = parseCutRoundStationFormSchema(form);
        String explicitType = firstNotBlank(toStringValue(schema.get("cutRoundFormType")),
                toStringValue(schema.get("processFormType")),
                toStringValue(schema.get("formType")),
                toStringValue(schema.get("type")));
        if (StrUtil.isNotBlank(explicitType)) {
            return explicitType.trim().toUpperCase();
        }
        String text = (StrUtil.blankToDefault(form.getFormCode(), "") + " " + StrUtil.blankToDefault(form.getFormName(), "")).toUpperCase();
        if (text.contains("CLEAN") || text.contains("清洁")) {
            return FORM_TYPE_CLEANING_CHECK;
        }
        if (text.contains("STARTUP") || text.contains("开机")) {
            return FORM_TYPE_STARTUP_CHECK;
        }
        if (text.contains("PRODUCTION_CHECK") || text.contains("PROCESS_CHECK") || text.contains("生产点检") || text.contains("裁切点检")) {
            return FORM_TYPE_PRODUCTION_CHECK;
        }
        return "";
    }

    private int getCutRoundStationFormModelMatchScore(HcStationFormDO form, String normalizedModel,
                                                      String materialType) {
        Map<String, Object> schema = parseCutRoundStationFormSchema(form);
        String schemaModelCode = normalizeCutRoundModelCode(firstNotBlank(
                toStringValue(schema.get("modelCode")),
                toStringValue(schema.get("model"))));
        if (jsonStringContains(schema.get("modelCodes"), normalizedModel)
                || (StrUtil.isNotBlank(normalizedModel) && normalizedModel.equalsIgnoreCase(schemaModelCode))) {
            return 0;
        }
        String schemaModelPrefix = normalizeCutRoundModelCode(firstNotBlank(
                toStringValue(schema.get("modelCodePrefix")),
                toStringValue(schema.get("modelPrefix"))));
        if (StrUtil.isNotBlank(normalizedModel)
                && (jsonPrefixMatches(schema.get("modelCodePrefixes"), normalizedModel)
                || (StrUtil.isNotBlank(schemaModelPrefix) && normalizedModel.startsWith(schemaModelPrefix)))) {
            return 10;
        }
        if (StrUtil.isNotBlank(materialType)
                && (jsonStringContains(schema.get("materialTypes"), materialType)
                || materialType.equalsIgnoreCase(toStringValue(schema.get("materialType"))))) {
            return 20;
        }
        String modelScope = toStringValue(schema.get("modelScope"));
        if ("COMMON".equalsIgnoreCase(schemaModelCode) || "ALL".equalsIgnoreCase(schemaModelCode)
                || "COMMON".equalsIgnoreCase(modelScope) || "ALL".equalsIgnoreCase(modelScope)) {
            return 90;
        }
        return -1;
    }

    private Map<String, Object> parseCutRoundStationFormSchema(HcStationFormDO form) {
        if (form == null || StrUtil.isBlank(form.getSchemaJson())) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> schema = JsonUtils.parseObject(form.getSchemaJson(), new TypeReference<Map<String, Object>>() {
            });
            return schema == null ? Collections.emptyMap() : schema;
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private String resolveCutRoundStationDisplayName(HcStationFormDO form) {
        String displayName = toStringValue(parseCutRoundStationFormSchema(form).get("displayName"));
        return firstNotBlank(displayName, stripCutRoundTemplateDisplayName(form.getFormName()));
    }

    private String stripCutRoundTemplateDisplayName(String formName) {
        String name = StrUtil.trimToEmpty(formName);
        if (StrUtil.isBlank(name)) {
            return "裁切生产点检表";
        }
        return name.replaceAll("^HCR-?\\d+-\\d+-\\d+\\s*", "")
                .replaceAll("^\\d+-\\d+-\\d+\\s*", "")
                .replaceAll("^CMP(黑垫|软垫|白垫)(（[^）]+）|\\([^)]*\\))?", "CMP")
                .trim();
    }

    private String normalizeCutRoundModelCode(String modelCode) {
        return StrUtil.trimToEmpty(modelCode).toUpperCase();
    }

    private String resolveCutRoundMaterialType(String normalizedModel) {
        if (StrUtil.isBlank(normalizedModel)) {
            return "";
        }
        HcProductModelDO productModel = hcProductModelMapper.selectByModelCode(normalizedModel);
        String categoryCode = StrUtil.trimToEmpty(productModel == null ? null : productModel.getCategoryCode())
                .toUpperCase();
        if ("BLACK_PAD".equals(categoryCode) || "WHITE_PAD".equals(categoryCode)) {
            return categoryCode;
        }
        return inferCutRoundMaterialType(normalizedModel);
    }

    private String inferCutRoundMaterialType(String normalizedModel) {
        String model = StrUtil.trimToEmpty(normalizedModel).toUpperCase();
        if (model.startsWith("HCR") || model.contains("BLACK") || model.contains("黑")) {
            return "BLACK_PAD";
        }
        if (model.startsWith("W") || model.contains("WHITE") || model.contains("SOFT") || model.contains("白") || model.contains("软")) {
            return "WHITE_PAD";
        }
        return "";
    }

    private boolean jsonPrefixMatches(Object value, String normalizedModel) {
        if (StrUtil.isBlank(normalizedModel)) {
            return false;
        }
        return toStringList(value).stream()
                .map(this::normalizeCutRoundModelCode)
                .filter(StrUtil::isNotBlank)
                .anyMatch(normalizedModel::startsWith);
    }

    private boolean jsonStringContains(Object value, String expected) {
        if (StrUtil.isBlank(expected)) {
            return false;
        }
        return toStringList(value).stream()
                .anyMatch(item -> expected.equalsIgnoreCase(StrUtil.trimToEmpty(item)));
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> rows = new ArrayList<>();
            iterable.forEach(item -> {
                String text = toStringValue(item);
                if (StrUtil.isNotBlank(text)) {
                    rows.add(text);
                }
            });
            return rows;
        }
        String text = toStringValue(value);
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        if (text.contains(",")) {
            List<String> rows = new ArrayList<>();
            for (String item : text.split(",")) {
                if (StrUtil.isNotBlank(item)) {
                    rows.add(item.trim());
                }
            }
            return rows;
        }
        return List.of(text);
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private LocalDateTime firstNotNullDateTime(LocalDateTime... values) {
        if (values == null) {
            return null;
        }
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void saveCheckDetails(HcAdhesiveReportSaveReqVO reqVO, HcPlanOrderDO planOrder,
                                  HcPlanOrderOperationDO operation, Long reportId) {
        if (reqVO.getCheckItems() == null || reqVO.getCheckItems().isEmpty()) {
            return;
        }
        for (HcAdhesiveCheckItemReqVO item : reqVO.getCheckItems()) {
            hcCutRoundCheckDetailMapper.insert(HcCutRoundCheckDetailDO.builder()
                    .tenantId(planOrder.getTenantId())
                    .cutRoundReportId(reportId)
                    .planId(planOrder.getId())
                    .planNo(planOrder.getPlanNo())
                    .planOperationId(operation.getId())
                    .itemCategory(item.getItemCategory())
                    .itemName(item.getItemName())
                    .standardValue(item.getStandardValue())
                    .actualValue(item.getActualValue())
                    .checkResult(firstNotBlank(item.getCheckResult(), "OK"))
                    .abnormalRemark(item.getAbnormalRemark())
                    .sortNo(item.getSortNo())
                    .build());
        }
    }

    private HcAdhesiveCheckItemRespVO buildCheckItemResp(HcCutRoundCheckDetailDO item) {
        HcAdhesiveCheckItemRespVO resp = new HcAdhesiveCheckItemRespVO();
        resp.setId(item.getId());
        resp.setItemCategory(item.getItemCategory());
        resp.setItemName(item.getItemName());
        resp.setStandardValue(item.getStandardValue());
        resp.setActualValue(item.getActualValue());
        resp.setCheckResult(item.getCheckResult());
        resp.setAbnormalRemark(item.getAbnormalRemark());
        resp.setSortNo(item.getSortNo());
        return resp;
    }

    private Map<String, Integer> increaseConsumablesOnConfirm(HcCutRoundReportDO report,
                                                              HcPlanOrderOperationDO operation,
                                                              String operatorName) {
        validateConsumablesReady(operation);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String type : List.of(CONSUMABLE_BLADE, CONSUMABLE_FELT)) {
            HcCutRoundSpareDO state = hcCutRoundSpareMapper.selectForUpdate(operation.getEquipmentId(), type);
            int before = state.getUseCount() == null ? 0 : state.getUseCount();
            int after = before + 1;
            state.setUseCount(after);
            state.setWarningFlag(isConsumableNeedReminder(state) ? 1 : 0);
            state.setLastOperatorId(SecurityFrameworkUtils.getLoginUserId());
            state.setLastOperatorName(firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
            state.setLastEventTime(LocalDateTime.now());
            hcCutRoundSpareMapper.updateById(state);
            hcCutRoundSpareRecordMapper.insert(HcCutRoundSpareRecordDO.builder()
                    .tenantId(operation.getTenantId())
                    .spareId(state.getId())
                    .equipmentId(state.getEquipmentId())
                    .equipmentCode(state.getEquipmentCode())
                    .equipmentName(state.getEquipmentName())
                    .workCenterId(state.getWorkCenterId())
                    .workCenterCode(state.getWorkCenterCode())
                    .workCenterName(state.getWorkCenterName())
                    .spareType(type)
                    .eventType(EVENT_USE)
                    .planId(report.getPlanId())
                    .planNo(report.getPlanNo())
                    .planOperationId(operation.getId())
                    .operationCode(operation.getOpCode())
                    .operationName(operation.getOpName())
                    .bizType(SOURCE_MENU_CODE)
                    .bizId(report.getId())
                    .beforeUseCount(before)
                    .afterUseCount(after)
                    .changeUseCount(1)
                    .onlineQuantity(state.getOnlineQuantity())
                    .offlineQuantity(BigDecimal.ZERO)
                    .finalUseCount(after)
                    .operatorId(SecurityFrameworkUtils.getLoginUserId())
                    .operatorName(firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                    .eventTime(LocalDateTime.now())
                    .remark("裁切扫码确认累计")
                    .build());
            counts.put(type, after);
        }
        return counts;
    }

    private void validateConsumablesReady(HcPlanOrderOperationDO operation) {
        if (operation.getEquipmentId() == null) {
            throw invalidParamException("当前裁切工序未绑定设备，不能裁切报工");
        }
        for (String type : List.of(CONSUMABLE_BLADE, CONSUMABLE_FELT)) {
            HcCutRoundSpareDO state = hcCutRoundSpareMapper.selectOneByEquipmentAndType(operation.getEquipmentId(), type);
            if (state == null) {
                throw invalidParamException(typeName(type) + "未挂接，请先在裁切备件管理维护");
            }
            String configIssue = getConsumableConfigIssue(type, state);
            if (StrUtil.isNotBlank(configIssue)) {
                throw invalidParamException(configIssue);
            }
            if (isConsumableNeedReplace(state)) {
                throw invalidParamException(typeName(type) + "寿命已达上限，请先更换或复位");
            }
        }
    }

    private HcPressSlotConsumableRespVO buildConsumableResp(HcPlanOrderOperationDO operation, Long equipmentId,
                                                            HcCutRoundSpareDO state, String type) {
        HcPressSlotConsumableRespVO resp = new HcPressSlotConsumableRespVO();
        resp.setEquipmentId(equipmentId);
        resp.setEquipmentCode(operation.getEquipmentCode());
        resp.setEquipmentName(operation.getEquipmentName());
        resp.setWorkCenterId(operation.getWorkCenterId());
        resp.setWorkCenterCode(operation.getWorkCenterCode());
        resp.setWorkCenterName(operation.getWorkCenterName());
        resp.setProcessCode(PROCESS_CODE);
        resp.setProcessName(firstNotBlank(operation.getOpName(), "裁切"));
        resp.setConsumableType(type);
        resp.setConsumableTypeName(typeName(type));
        if (state == null) {
            resp.setUseCount(0);
            resp.setWarningFlag(1);
            resp.setStatus("EMPTY");
            resp.setMessage(typeName(type) + "未挂接，请先在裁切备件管理维护");
            return resp;
        }
        resp.setStateId(state.getId());
        resp.setEquipmentId(state.getEquipmentId());
        resp.setEquipmentCode(state.getEquipmentCode());
        resp.setEquipmentName(state.getEquipmentName());
        resp.setOnlineQuantity(zeroIfNull(state.getOnlineQuantity()));
        resp.setAvailableCount(zeroIfNull(state.getAvailableQuantity()));
        resp.setUseCount(state.getUseCount() == null ? 0 : state.getUseCount());
        resp.setLimitCount(resolveLimitCount(type, state));
        int limitDays = resolveLimitDays(type, state);
        resp.setLimitDays(limitDays);
        resp.setUseDays(limitDays > 0 ? calculateUseDays(state.getLastReplaceTime()) : 0);
        resp.setLastOperatorId(state.getLastOperatorId());
        resp.setLastOperatorName(state.getLastOperatorName());
        resp.setLastReplaceTime(state.getLastReplaceTime());
        resp.setLastEventTime(state.getLastEventTime());
        String configIssue = getConsumableConfigIssue(type, state);
        if (StrUtil.isNotBlank(configIssue)) {
            resp.setWarningFlag(1);
            resp.setStatus("CONFIG_MISSING");
            resp.setMessage(configIssue);
            return resp;
        }
        boolean needReplace = isConsumableNeedReplace(state);
        boolean warning = isConsumableNeedReminder(state);
        resp.setWarningFlag(warning ? 1 : 0);
        resp.setStatus(needReplace ? "NEED_REPLACE" : warning ? "WARNING" : firstNotBlank(state.getStatus(), STATUS_ACTIVE));
        resp.setMessage(needReplace ? typeName(type) + "寿命已达上限，请先更换或复位"
                : warning ? buildConsumableReminderMessage(type, state)
                : typeName(type) + "状态正常");
        return resp;
    }

    private String getConsumableConfigIssue(String type, HcCutRoundSpareDO state) {
        if (state == null) {
            return typeName(type) + "未挂接，请先在裁切备件管理维护";
        }
        int limitCount = resolveLimitCount(type, state);
        if (limitCount <= 0) {
            return typeName(type) + (CONSUMABLE_BLADE.equals(type) ? "次数" : "片数")
                    + "寿命上限未配置，请先在裁切备件管理维护";
        }
        if (CONSUMABLE_FELT.equals(type) && resolveLimitDays(type, state) <= 0) {
            return "毛毡天数寿命上限未配置，请先在裁切备件管理维护";
        }
        return null;
    }

    private boolean isConsumableNeedReplace(HcCutRoundSpareDO state) {
        if (state == null) {
            return true;
        }
        String type = normalizeConsumableType(state.getSpareType());
        int count = state.getUseCount() == null ? 0 : state.getUseCount();
        int limitCount = resolveLimitCount(type, state);
        int limitDays = resolveLimitDays(type, state);
        int useDays = calculateUseDays(state.getLastReplaceTime());
        return (limitCount > 0 && count >= limitCount) || (limitDays > 0 && useDays >= limitDays);
    }

    private boolean isConsumableNeedReminder(HcCutRoundSpareDO state) {
        if (state == null) {
            return true;
        }
        if (isConsumableNeedReplace(state)) {
            return true;
        }
        String type = normalizeConsumableType(state.getSpareType());
        int count = state.getUseCount() == null ? 0 : state.getUseCount();
        int limitCount = resolveLimitCount(type, state);
        int limitDays = resolveLimitDays(type, state);
        int useDays = calculateUseDays(state.getLastReplaceTime());
        return isValueReachWarning(count, limitCount) || isValueReachWarning(useDays, limitDays);
    }

    private boolean isValueReachWarning(int used, int limit) {
        if (limit <= 0) {
            return false;
        }
        return used >= Math.max(1, (int) Math.ceil(limit * CONSUMABLE_WARNING_RATIO));
    }

    private String buildConsumableReminderMessage(String type, HcCutRoundSpareDO state) {
        int count = state.getUseCount() == null ? 0 : state.getUseCount();
        int limitCount = resolveLimitCount(type, state);
        int limitDays = resolveLimitDays(type, state);
        int useDays = calculateUseDays(state.getLastReplaceTime());
        String countText = limitCount > 0 ? count + "/" + limitCount + "片" : "";
        String dayText = limitDays > 0 ? useDays + "/" + limitDays + "天" : "";
        String detail = StrUtil.isNotBlank(countText) && StrUtil.isNotBlank(dayText)
                ? countText + "、" + dayText : firstNotBlank(countText, dayText);
        return typeName(type) + "寿命已达到90%提醒" + (StrUtil.isNotBlank(detail) ? "（" + detail + "）" : "") + "，请及时更换或复位";
    }

    private String normalizeConsumableType(String consumableType) {
        String type = StrUtil.trimToEmpty(consumableType).toUpperCase();
        if (CONSUMABLE_BLADE.equals(type) || "BLADE".equals(type) || "刀片".equals(consumableType)) {
            return CONSUMABLE_BLADE;
        }
        if (CONSUMABLE_FELT.equals(type) || "FELT".equals(type) || "毛毡".equals(consumableType)) {
            return CONSUMABLE_FELT;
        }
        throw invalidParamException("裁切耗材类型只允许刀片或毛毡");
    }

    private void validateStock(HcAdhesiveGlueBoardStockDO stock, String type, BigDecimal quantity) {
        if (!type.equalsIgnoreCase(StrUtil.blankToDefault(stock.getAccessoryCategory(), ""))) {
            throw invalidParamException("该辅料批次分类不是" + typeName(type));
        }
        BigDecimal availableCount = zeroIfNull(stock.getAvailableCount());
        if (availableCount.compareTo(quantity) < 0) {
            throw invalidParamException(typeName(type) + "边库可用数量不足，当前可用" + availableCount + "个");
        }
    }

    private void deductStock(HcAdhesiveGlueBoardStockDO stock, BigDecimal quantity) {
        HcAdhesiveGlueBoardStockDO update = new HcAdhesiveGlueBoardStockDO();
        update.setId(stock.getId());
        update.setAvailableCount(zeroIfNull(stock.getAvailableCount()).subtract(quantity));
        update.setUsedCount(zeroIfNull(stock.getUsedCount()).add(quantity));
        hcAdhesiveGlueBoardStockMapper.updateById(update);
    }

    private HcAdhesiveGlueBoardStockRespVO buildStockResp(HcAdhesiveGlueBoardStockDO stock) {
        HcAdhesiveGlueBoardStockRespVO resp = new HcAdhesiveGlueBoardStockRespVO();
        resp.setId(stock.getId());
        resp.setAccessoryCategory(stock.getAccessoryCategory());
        resp.setAccessoryCategoryName(stock.getAccessoryCategoryName());
        resp.setGlueBoardMaterialCode(stock.getGlueBoardMaterialCode());
        resp.setGlueBoardMaterialName(stock.getGlueBoardMaterialName());
        resp.setGlueBoardBatchNo(stock.getGlueBoardBatchNo());
        resp.setAvailableCount(stock.getAvailableCount());
        resp.setStockStatus(stock.getStockStatus());
        resp.setQualityStatus(stock.getQualityStatus());
        return resp;
    }

    private HcPressSlotChangeoverInspectionRespVO buildChangeoverResp(HcPressSlotChangeoverInspectionDO record) {
        HcPressSlotChangeoverInspectionRespVO resp = new HcPressSlotChangeoverInspectionRespVO();
        resp.setId(record.getId());
        resp.setPlanId(record.getPlanId());
        resp.setPlanNo(record.getPlanNo());
        resp.setPlanOperationId(record.getPlanOperationId());
        resp.setSourceSlittingSliceId(record.getSourceSlittingSliceId());
        resp.setPressSlotSliceNo(record.getPressSlotSliceNo());
        resp.setMotherSegmentBatchNo(record.getMotherSegmentBatchNo());
        resp.setProductionModelCode(record.getProductionModelCode());
        resp.setProductionMaterialCode(record.getProductionMaterialCode());
        resp.setCurrentPlanNo(record.getCurrentPlanNo());
        resp.setInspectionStatus(firstNotBlank(record.getInspectionStatus(), "RECORDED"));
        LocalDateTime submitTime = normalizeValidLocalDateTime(record.getSubmitTime(),
                normalizeValidLocalDateTime(record.getRecordTime(), record.getUpdateTime()));
        LocalDateTime recordTime = normalizeValidLocalDateTime(record.getRecordTime(), submitTime);
        resp.setSubmitTime(submitTime);
        resp.setFeedbackTime(record.getFeedbackTime());
        resp.setFeedbackResult(record.getFeedbackResult());
        resp.setFeedbackRemark(record.getFeedbackRemark());
        resp.setHeaderDataJson(record.getHeaderDataJson());
        resp.setDetailItemsJson(record.getDetailItemsJson());
        resp.setRecorderName(record.getRecorderName());
        resp.setRecordTime(recordTime);
        resp.setRemark(record.getRemark());
        resp.setExtraJson(record.getExtraJson());
        Map<String, Object> extra = parseExtra(record.getExtraJson());
        boolean confirmed = isChangeoverConfirmed(record);
        resp.setConfirmerName(confirmed ? firstNotBlank(extraString(extra, "confirmerName"), "") : "");
        resp.setConfirmerTime(confirmed ? parseExtraDateTime(extra.get("confirmerTime"), null) : null);
        resp.setCheckItems(parseChangeoverCheckItems(record.getDetailItemsJson()));
        return resp;
    }

    private LocalDateTime resolveChangeoverEffectiveTime(HcPressSlotChangeoverInspectionDO record) {
        return normalizeValidLocalDateTime(record.getRecordTime(),
                normalizeValidLocalDateTime(record.getSubmitTime(),
                        normalizeValidLocalDateTime(record.getUpdateTime(), record.getCreateTime())));
    }

    private List<HcAdhesiveCheckItemRespVO> parseChangeoverCheckItems(String detailItemsJson) {
        if (StrUtil.isBlank(detailItemsJson)) {
            return Collections.emptyList();
        }
        List<HcAdhesiveCheckItemRespVO> checkItems = JsonUtils.parseObjectQuietly(detailItemsJson,
                new TypeReference<List<HcAdhesiveCheckItemRespVO>>() {});
        return checkItems == null ? Collections.emptyList() : checkItems;
    }

    private String mergeChangeoverExtra(String extraJson, String recorderName, String confirmerName, LocalDateTime confirmerTime) {
        Map<String, Object> extra = new LinkedHashMap<>(parseExtra(extraJson));
        extra.put("recorderName", recorderName);
        if (StrUtil.isNotBlank(confirmerName) && confirmerTime != null) {
            extra.put("confirmerName", confirmerName);
            extra.put("confirmerTime", confirmerTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            extra.put("docStatus", CHANGEOVER_STATUS_CONFIRMED);
            extra.put("recordStatus", CHANGEOVER_STATUS_CONFIRMED);
        }
        return JsonUtils.toJsonString(extra);
    }

    private String extraString(Map<String, Object> extra, String key) {
        Object value = extra.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime parseExtraDateTime(Object value, LocalDateTime fallback) {
        if (value instanceof LocalDateTime localDateTime) {
            return normalizeValidLocalDateTime(localDateTime, fallback);
        }
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return fallback;
        }
        try {
            return normalizeValidLocalDateTime(LocalDateTime.parse(String.valueOf(value),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private LocalDateTime normalizeValidLocalDateTime(LocalDateTime value, LocalDateTime fallback) {
        if (value == null || value.getYear() < 2000) {
            return fallback == null ? LocalDateTime.now() : fallback;
        }
        return value;
    }

    private LocalDateTime normalizeReportDateTime(LocalDateTime value, LocalDate reportDate) {
        if (value == null || value.getYear() < 2000) {
            return null;
        }
        return value;
    }

    private int resolveCutRoundInitialUseCount(Integer initialUseCount) {
        int value = initialUseCount == null ? 0 : initialUseCount;
        if (value < 0) {
            throw invalidParamException("初始化片数不能小于0");
        }
        return value;
    }

    private int resolveLimitCount(String type, HcCutRoundSpareDO state) {
        return normalizeLimitCount(type, state != null ? state.getLimitCount() : null);
    }

    private int resolveLimitCount(String type, HcAdhesiveGlueBoardStockDO stock) {
        return normalizeLimitCount(type, stock != null && stock.getLifetimeLimitCount() != null
                && stock.getLifetimeLimitCount().compareTo(BigDecimal.ZERO) > 0 ? stock.getLifetimeLimitCount().intValue() : null);
    }

    private int normalizeLimitCount(String type, Integer configuredLimitCount) {
        return configuredLimitCount != null && configuredLimitCount > 0 ? configuredLimitCount : 0;
    }

    private int resolveLimitDays(String type, HcCutRoundSpareDO state) {
        if (!CONSUMABLE_FELT.equals(type)) {
            return 0;
        }
        return state != null && state.getLimitDays() != null && state.getLimitDays() > 0 ? state.getLimitDays() : 0;
    }

    private int calculateUseDays(LocalDateTime baseTime) {
        if (baseTime == null) {
            return 0;
        }
        return (int) Math.max(ChronoUnit.DAYS.between(baseTime.toLocalDate(), LocalDate.now()), 0);
    }

    private String typeName(String type) {
        return CONSUMABLE_BLADE.equals(type) ? "刀片" : "毛毡";
    }

    private Map<String, List<String>> buildBladeReplaceReasonMap(List<HcCutRoundReportDO> reports,
            HcCutRoundProductionRecordPageReqVO reqVO) {
        List<Long> operationIds = reports.stream()
                .map(HcCutRoundReportDO::getPlanOperationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (operationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDateTime eventTimeStart = reqVO.getReportDateStart() == null
                ? null : reqVO.getReportDateStart().atStartOfDay();
        LocalDateTime eventTimeEnd = reqVO.getReportDateEnd() == null
                ? null : reqVO.getReportDateEnd().plusDays(1).atStartOfDay();
        List<HcCutRoundSpareRecordDO> replaceRecords = hcCutRoundSpareRecordMapper.selectList(
                new LambdaQueryWrapperX<HcCutRoundSpareRecordDO>()
                        .eq(HcCutRoundSpareRecordDO::getDeleted, false)
                        .in(HcCutRoundSpareRecordDO::getPlanOperationId, operationIds)
                        .eq(HcCutRoundSpareRecordDO::getSpareType, CONSUMABLE_BLADE)
                        .eq(HcCutRoundSpareRecordDO::getEventType, EVENT_REPLACE)
                        .geIfPresent(HcCutRoundSpareRecordDO::getEventTime, eventTimeStart)
                        .leIfPresent(HcCutRoundSpareRecordDO::getEventTime, eventTimeEnd)
                        .orderByAsc(HcCutRoundSpareRecordDO::getEventTime)
                        .orderByAsc(HcCutRoundSpareRecordDO::getId));
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (HcCutRoundSpareRecordDO record : replaceRecords) {
            if (record.getPlanOperationId() == null || record.getEventTime() == null
                    || StrUtil.isBlank(record.getReplaceReason())) {
                continue;
            }
            String key = productionRecordReasonKey(record.getPlanOperationId(), record.getEventTime().toLocalDate());
            List<String> reasons = result.computeIfAbsent(key, ignored -> new ArrayList<>());
            if (!reasons.contains(record.getReplaceReason())) {
                reasons.add(record.getReplaceReason());
            }
        }
        return result;
    }

    private LocalDate resolveProductionRecordDate(HcCutRoundReportDO report) {
        if (report.getReportDate() != null) {
            return report.getReportDate();
        }
        LocalDateTime fallbackTime = firstNotNull(report.getConfirmerTime(), report.getEndTime(), report.getRecorderTime());
        return fallbackTime == null ? null : fallbackTime.toLocalDate();
    }

    private String resolveProductionRecordModelCode(HcCutRoundReportDO report, Map<String, Object> extra) {
        return firstNotBlank(
                extraString(extra, "runtimeModelCode"),
                extraString(extra, "plannedModelCode"),
                report.getModelCode());
    }

    private String resolveProductionRecordBatchNo(HcCutRoundReportDO report) {
        return firstNotBlank(
                report.getParentProductionBatchNo(),
                stripCutRoundSizeSuffix(report.getSourceBatchNo()),
                stripCutRoundSizeSuffix(report.getSourceProductionBatchNo()),
                stripCutRoundSizeSuffix(report.getProductionBatchNo()));
    }

    private String resolveCutSizeMm(HcCutRoundReportDO report, Map<String, Object> extra) {
        return normalizeCutSizeMm(firstNotBlank(
                extraString(extra, "actualSizeRule"),
                extraString(extra, "actualSize"),
                report.getProductionBatchNo()));
    }

    private String normalizeCutSizeMm(String value) {
        String normalized = normalizeCutRoundActualSizeRule(value);
        if (StrUtil.isNotBlank(normalized)) {
            return normalized.replace("mm", "");
        }
        String text = StrUtil.trimToEmpty(value)
                .replaceAll("(?i)-J\\d+$", "")
                .replaceAll("(?i)-S\\d+$", "")
                .toUpperCase();
        if (text.matches(".*[PQRS]\\d{3}A$")) {
            return "775";
        }
        if (text.matches(".*[PQRS]\\d{3}B$")) {
            return "740";
        }
        return "";
    }

    private BigDecimal resolveProductionRecordQty(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private Integer calculateProductionRecordFeltUseDays(HcCutRoundReportDO report, Map<String, Object> extra) {
        LocalDateTime lastReplaceTime = parseExtraDateTime(extra.get("feltLastReplaceTime"), null);
        if (lastReplaceTime == null) {
            return null;
        }
        LocalDateTime reportTime = firstNotNull(
                report.getConfirmerTime(),
                report.getEndTime(),
                report.getRecorderTime(),
                report.getReportDate() == null ? null : report.getReportDate().atStartOfDay());
        if (reportTime == null) {
            return null;
        }
        return (int) Math.max(ChronoUnit.DAYS.between(lastReplaceTime.toLocalDate(), reportTime.toLocalDate()), 0);
    }

    private Integer maxInteger(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null ? candidate : Math.max(current, candidate);
    }

    private void addIfNotBlank(Set<String> target, String value) {
        String text = StrUtil.trimToEmpty(value);
        if (StrUtil.isNotBlank(text)) {
            target.add(text);
        }
    }

    private String joinDistinct(Set<String> values) {
        return values.isEmpty() ? "" : String.join("；", values);
    }

    private String productionRecordReasonKey(Long planOperationId, LocalDate reportDate) {
        return planOperationId + "|" + (reportDate == null ? "" : reportDate);
    }

    private String resolveSegmentMark(String batchNo) {
        if (StrUtil.isBlank(batchNo)) {
            return "";
        }
        for (String mark : List.of("P", "Q", "R", "S")) {
            if (batchNo.contains(mark)) {
                return mark;
            }
        }
        return "";
    }

    private String normalizeCutRoundActualSizeRule(String value) {
        String text = StrUtil.trimToEmpty(value).toUpperCase();
        if (text.contains("740") || "B".equals(text)) {
            return "740mm";
        }
        if (text.contains("775") || "A".equals(text)) {
            return "775mm";
        }
        return "";
    }

    private String getCutRoundSizeSuffix(String actualSizeRule) {
        String normalized = normalizeCutRoundActualSizeRule(actualSizeRule);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        return "740mm".equals(normalized) ? "B" : "A";
    }

    private String stripCutRoundSizeSuffix(String batchNo) {
        String base = StrUtil.trimToEmpty(batchNo)
                .replaceAll("(?i)-J\\d+$", "")
                .replaceAll("(?i)-S\\d+$", "")
                .toUpperCase();
        return base.replaceFirst("(?i)([PQRS]\\d{3})[AB]$", "$1");
    }

    private String buildCutRoundProductionBatchNo(String batchNo, String actualSizeRule) {
        String base = stripCutRoundSizeSuffix(batchNo);
        String suffix = getCutRoundSizeSuffix(actualSizeRule);
        if (StrUtil.isBlank(base)) {
            return "";
        }
        if (StrUtil.isNotBlank(suffix) && base.matches("(?i)^[A-Z]\\d{2}[A-Z]\\d{3}[A-Z][PQRS]\\d{3}$")) {
            return base + suffix;
        }
        return base;
    }

    private String normalizeCutRoundMotherSegmentBatchNo(String batchNo) {
        String base = stripCutRoundSizeSuffix(batchNo);
        if (StrUtil.isBlank(base)) {
            return "";
        }
        return base.replaceFirst("(?i)^(.+[PQRS])\\d{3}$", "$1");
    }

    private String resolveCutRoundMotherSegmentBatchNo(HcCutRoundReportDO report) {
        if (report == null) {
            return "";
        }
        return firstNotBlank(
                normalizeCutRoundMotherSegmentBatchNo(report.getSourceBatchNo()),
                normalizeCutRoundMotherSegmentBatchNo(report.getParentProductionBatchNo()),
                normalizeCutRoundMotherSegmentBatchNo(report.getSourceProductionBatchNo()),
                normalizeCutRoundMotherSegmentBatchNo(report.getProductionBatchNo()));
    }

    private List<HcCutRoundReportDO> filterCutRoundReportsByMotherBatchNo(List<HcCutRoundReportDO> reports,
                                                                          String motherBatchNo) {
        String normalizedMotherBatchNo = normalizeCutRoundMotherSegmentBatchNo(motherBatchNo);
        if (StrUtil.isBlank(normalizedMotherBatchNo)) {
            return reports == null ? Collections.emptyList() : reports;
        }
        return (reports == null ? Collections.<HcCutRoundReportDO>emptyList() : reports)
                .stream()
                .filter(report -> StrUtil.equalsIgnoreCase(resolveCutRoundMotherSegmentBatchNo(report), normalizedMotherBatchNo))
                .toList();
    }

    private LocalDateTime normalizePassWorkDateTime(LocalDateTime value, LocalDateTime fallback) {
        if (value == null || value.getYear() < 2000) {
            return fallback;
        }
        return value;
    }

    private String formatPassWorkDateTime(LocalDateTime value) {
        LocalDateTime normalized = normalizePassWorkDateTime(value, null);
        return normalized == null ? null : normalized.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Map<String, Object> parseExtra(String extraJson) {
        if (StrUtil.isBlank(extraJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = JsonUtils.parseObject(extraJson, new TypeReference<>() {});
        return map == null ? Collections.emptyMap() : map;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calculateWipLockRemainingQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal remainingQty = zeroIfNull(lock.getLockQty())
                .subtract(zeroIfNull(lock.getConsumedQty()))
                .subtract(zeroIfNull(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingQty;
    }

    private BigDecimal firstNonNullDecimal(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void validatePositive(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException(label + "必须大于0");
        }
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private record ProductionRecordKey(LocalDate reportDate, String modelCode, String productionBatchNo,
                                       String cutSizeMm) {
    }

    private record ProductionRecordPadKey(ProductionRecordKey key, String padType) {
    }

    private class ProductionRecordAccumulator {

        private final ProductionRecordKey key;
        private final String padType;
        private final Object displayKey;
        private BigDecimal inputQty = BigDecimal.ZERO;
        private BigDecimal outputQty = BigDecimal.ZERO;
        private final Set<String> bladeBatchNos = new LinkedHashSet<>();
        private final Set<String> bladeModels = new LinkedHashSet<>();
        private Integer bladeUseCount;
        private final Set<String> feltBatchNos = new LinkedHashSet<>();
        private final Set<String> feltModels = new LinkedHashSet<>();
        private Integer feltUseCount;
        private Integer feltUseDays;
        private LocalDateTime recordTime;
        private final Set<Long> planOperationIds = new LinkedHashSet<>();
        private final Set<String> bladeReplaceReasons = new LinkedHashSet<>();
        private final Set<String> recorderNames = new LinkedHashSet<>();
        private final Set<String> remarks = new LinkedHashSet<>();
        private final Set<String> recordSources = new LinkedHashSet<>();

        ProductionRecordAccumulator(ProductionRecordKey key, String padType, Object displayKey) {
            this.key = key;
            this.padType = padType;
            this.displayKey = displayKey;
        }

        void accept(HcCutRoundReportDO report, Map<String, Object> extra) {
            recordSources.add(DISPLAY_SOURCE_REPORT);
            inputQty = inputQty.add(resolveProductionRecordQty(report.getInputLength()));
            outputQty = outputQty.add(resolveProductionRecordQty(report.getOutputLength()));
            addIfNotBlank(bladeModels, report.getBladeMaterialCode());
            addIfNotBlank(bladeBatchNos, report.getBladeBatchNo());
            bladeUseCount = maxInteger(bladeUseCount, report.getBladeUseCount());
            addIfNotBlank(feltModels, report.getFeltMaterialCode());
            addIfNotBlank(feltBatchNos, report.getFeltBatchNo());
            feltUseCount = maxInteger(feltUseCount, report.getFeltUseCount());
            feltUseDays = maxInteger(feltUseDays, calculateProductionRecordFeltUseDays(report, extra));
            if (report.getPlanOperationId() != null) {
                planOperationIds.add(report.getPlanOperationId());
            }
            addIfNotBlank(recorderNames, report.getRecorderName());
            addIfNotBlank(remarks, report.getRemark());
            LocalDateTime currentRecordTime = firstNotNull(report.getEndTime(), report.getConfirmerTime(),
                    report.getRecorderTime());
            if (currentRecordTime != null && (recordTime == null || currentRecordTime.isAfter(recordTime))) {
                recordTime = currentRecordTime;
            }
        }

        void acceptRndManual(RndManualProductionRecord record) {
            recordSources.add(DISPLAY_SOURCE_RND_MANUAL);
            inputQty = inputQty.add(record.cutInputPcs);
            outputQty = outputQty.add(record.cutOutputPcs);
            bladeUseCount = maxInteger(bladeUseCount, record.bladeUseCount);
            feltUseCount = maxInteger(feltUseCount, record.feltUseCount);
            feltUseDays = maxInteger(feltUseDays, record.feltUseDays);
            addIfNotBlank(recorderNames, record.operatorName);
            addIfNotBlank(remarks, record.remark == null ? null : DISPLAY_SOURCE_RND_MANUAL + "：" + record.remark);
            if (record.eventTime != null && (recordTime == null || record.eventTime.isAfter(recordTime))) {
                recordTime = record.eventTime;
            }
        }

        void fillBladeReplaceReasons(Map<String, List<String>> reasonMap) {
            for (Long planOperationId : planOperationIds) {
                List<String> reasons = reasonMap.get(productionRecordReasonKey(planOperationId, key.reportDate()));
                if (reasons == null) {
                    continue;
                }
                reasons.forEach(reason -> addIfNotBlank(bladeReplaceReasons, reason));
            }
        }

        HcCutRoundProductionRecordRespVO toRespVO() {
            HcCutRoundProductionRecordRespVO respVO = new HcCutRoundProductionRecordRespVO();
            respVO.setId(HcProductionRecordRevisionKeyUtils.generateDisplayId(
                    HcProductionRecordRevisionServiceImpl.MODULE_CUT_ROUND, displayKey));
            respVO.setPadType(padType);
            respVO.setReportDate(key.reportDate());
            respVO.setModelCode(key.modelCode());
            respVO.setProductionBatchNo(key.productionBatchNo());
            respVO.setCutSizeMm(key.cutSizeMm());
            respVO.setInputQty(inputQty);
            respVO.setOutputQty(outputQty);
            respVO.setBladeModel(joinDistinct(bladeModels));
            respVO.setBladeBatchNo(joinDistinct(bladeBatchNos));
            respVO.setBladeUseCount(bladeUseCount);
            respVO.setFeltModel(joinDistinct(feltModels));
            respVO.setFeltBatchNo(joinDistinct(feltBatchNos));
            respVO.setFeltUseCount(feltUseCount);
            respVO.setFeltUseDays(feltUseDays);
            respVO.setBladeReplaceReason(joinDistinct(bladeReplaceReasons));
            respVO.setRecorderName(joinDistinct(recorderNames));
            respVO.setRecordSource(joinDistinct(recordSources));
            respVO.setRecordTime(recordTime);
            respVO.setRemark(joinDistinct(remarks));
            return respVO;
        }
    }

    private class RndManualProductionRecord {

        private final LocalDate reportDate;
        private final String modelCode;
        private final String padType;
        private final String productionBatchNo;
        private final String cutSizeMm;
        private BigDecimal cutInputPcs = BigDecimal.ZERO;
        private BigDecimal cutOutputPcs = BigDecimal.ZERO;
        private Integer bladeUseCount;
        private Integer feltUseCount;
        private Integer feltUseDays;
        private LocalDateTime eventTime;
        private String operatorName;
        private String remark;

        RndManualProductionRecord(HcCutRoundSpareRecordDO record) {
            reportDate = record.getEventTime().toLocalDate();
            modelCode = record.getModelCode();
            padType = productionRecordPadTypeResolver.normalizePadType(record.getPadType());
            productionBatchNo = record.getProductionBatchNo();
            cutSizeMm = normalizeCutSizeMm(record.getCutSizeMm());
        }

        void accept(HcCutRoundSpareRecordDO record) {
            if (cutInputPcs.compareTo(BigDecimal.ZERO) == 0 && record.getCutInputPcs() != null) {
                cutInputPcs = record.getCutInputPcs();
            }
            if (cutOutputPcs.compareTo(BigDecimal.ZERO) == 0 && record.getCutOutputPcs() != null) {
                cutOutputPcs = record.getCutOutputPcs();
            }
            if (CONSUMABLE_BLADE.equalsIgnoreCase(StrUtil.trimToEmpty(record.getSpareType()))) {
                bladeUseCount = maxInteger(bladeUseCount, record.getAfterUseCount());
            }
            if (CONSUMABLE_FELT.equalsIgnoreCase(StrUtil.trimToEmpty(record.getSpareType()))) {
                feltUseCount = maxInteger(feltUseCount, record.getAfterUseCount());
                feltUseDays = maxInteger(feltUseDays, record.getFeltUseDays());
            }
            if (record.getEventTime() != null && (eventTime == null || record.getEventTime().isAfter(eventTime))) {
                eventTime = record.getEventTime();
            }
            operatorName = firstNotBlank(operatorName, record.getOperatorName());
            remark = firstNotBlank(remark, record.getRemark());
        }
    }

    private static final class CutRoundQualityRisk {

        private final String flag;
        private final String snapshotJson;

        private CutRoundQualityRisk(String flag, String snapshotJson) {
            this.flag = flag;
            this.snapshotJson = snapshotJson;
        }

        private String flag() {
            return flag;
        }

        private String snapshotJson() {
            return snapshotJson;
        }
    }
}
