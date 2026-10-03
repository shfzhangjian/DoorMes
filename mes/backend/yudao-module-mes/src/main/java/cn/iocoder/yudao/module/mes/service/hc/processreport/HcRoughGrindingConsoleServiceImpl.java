package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleBoardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleBoardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleAllocationModeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleDailyCheckSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstAllocationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstAllocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstAllocationQuantityReviseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductSegmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleOriginalMotherTimeStampReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleReportTimeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSegmentTimingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSegmentTimingStampReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSourceBalanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughFaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughSecondSegmentInspectionApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughSecondSegmentInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcStatisticsDataReviseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportAbnormalPositionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportAbnormalPositionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcWetReportAbnormalPositionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingAllocationModeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingMiddleProductDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingMiddleProductRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSegmentTimingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSourceBalanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcWetReportAbnormalPositionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingAllocationModeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingMiddleProductDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingMiddleProductRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSecondDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSegmentTimingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSourceBalanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.qtimeconfig.HcQtimeEvaluationService;
import cn.iocoder.yudao.module.mes.service.hc.stationform.HcStationFormService;
import cn.iocoder.yudao.module.mes.service.qms.QmsFaiService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Slf4j
public class HcRoughGrindingConsoleServiceImpl implements HcRoughGrindingConsoleService {

    @Resource
    private HcGrindingConsumptionService grindingConsumptionService;

    private static final String PROCESS_CODE = "ROUGH_GRINDING";
    private static final String PROCESS_NAME = "磨皮";
    private static final String SOURCE_MENU_CODE_ROUGH = "ROUGH_GRINDING_REPORT";
    private static final String SOURCE_MENU_CODE_ROUGH_SECOND_SEGMENT = "ROUGH_GRINDING_SECOND_SEGMENT";
    private static final String REPORT_TYPE_START = "START";
    private static final String FAI_STANDARD_MATCH_PRODUCT_MODEL_PROCESS = "PRODUCT_MODEL_PROCESS";
    private static final String FAI_STATUS_PENDING = "PENDING";
    private static final String FAI_STATUS_COMPLETED = "COMPLETED";
    private static final String FAI_STATUS_REJECTED = "REJECTED";
    private static final String FAI_STATUS_CANCELED = "CANCELED";
    private static final String FAI_JUDGMENT_PENDING = "PENDING";
    private static final String FAI_JUDGMENT_OK = "OK";
    private static final String FAI_JUDGMENT_NG = "NG";
    private static final String FAI_SUBMISSION_TYPE_MASS_SHIPMENT = "MASS_SHIPMENT";
    private static final String FAI_SAMPLE_TYPE_SECOND_SEGMENT = "SECOND_SEGMENT_SAMPLE";
    private static final String FAI_SAMPLE_TYPE_SECOND_SEGMENT_NAME = "二磨分段留样";
    private static final String FAI_TRIGGER_NEW_ORDER = "NEW_ORDER";
    private static final String FAI_TRIGGER_REWORK_RECHECK = "REWORK_RECHECK";
    private static final String FORM_STARTUP = "ROUGH_STARTUP_CHECK";
    private static final String FORM_CLEANING = "ROUGH_CLEANING_CHECK";
    private static final String FORM_PROCESS_CHECK = "ROUGH_PROCESS_CHECK";
    private static final String FORM_MIDDLE_PRODUCT = "ROUGH_MIDDLE_PRODUCT_RECORD";
    private static final List<String> DAILY_FORM_CODES = List.of(FORM_STARTUP, FORM_CLEANING);
    private static final String RECORD_SCOPE_DAILY = "EQUIPMENT_DAILY";
    private static final String RECORD_SCOPE_PROCESS_DETAIL = "PROCESS_DETAIL";
    private static final String BIZ_GRINDING_FIRST = "GRINDING_FIRST";
    private static final String BIZ_GRINDING_SECOND = "GRINDING_SECOND";
    private static final String PASS_TYPE_FIRST = "FIRST";
    private static final String PASS_TYPE_SECOND = "SECOND";
    private static final String ALLOCATION_MODE_FIRST_ALLOCATED = "FIRST_ALLOCATED";
    private static final String ALLOCATION_SEGMENT_NONE = "NONE";
    private static final String SOURCE_BALANCE_TYPE_WET_OUTPUT = "WET_OUTPUT";
    private static final List<String> FIRST_ALLOCATION_SEGMENT_MARKS = List.of("P", "Q", "R", "S", ALLOCATION_SEGMENT_NONE);
    private static final String PRODUCTION_RECORD_ROLE_FIRST_ORIGINAL = "FIRST_ORIGINAL";
    private static final String PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION = "FIRST_ALLOCATION";
    private static final String PRODUCTION_RECORD_ROLE_SECOND = "SECOND";
    private static final String SEGMENT_TIMING_ACTION_START = "START";
    private static final String SEGMENT_TIMING_ACTION_END = "END";
    private static final String MOTHER_BATCH_TIMING_MARK = "MOTHER";
    private static final List<String> GRINDING_SEGMENT_MARKS = List.of("P", "Q", "R", "S");
    private static final List<String> SECOND_TIMING_SEGMENT_MARKS = List.of("P", "Q", "R", "S", ALLOCATION_SEGMENT_NONE);
    private static final BigDecimal RANGE_EPS = new BigDecimal("0.0001");
    private static final String DOC_STATUS_RECORDED = "RECORDED";
    private static final String DOC_STATUS_CONFIRMED = "CONFIRMED";
    private static final int MIDDLE_PRODUCT_DEFAULT_ROW_COUNT = 50;
    private static final String CONSUMABLE_SANDPAPER = "SANDPAPER";
    private static final String CONSUMABLE_GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final int DEFAULT_SANDPAPER_LIMIT_DAYS = 15;
    private static final int DEFAULT_GUIDE_CLOTH_LIMIT_COUNT = 200;
    private static final int GUIDE_CLOTH_USE_INCREMENT = 2;
    private static final BigDecimal DEFAULT_SANDPAPER_LIMIT_LENGTH = BigDecimal.valueOf(500);
    private static final int CONSUMABLE_WARNING_PERCENT = 90;
    private static final String CONSUMABLE_EVENT_USE = "USE";
    private static final String CONSUMABLE_EVENT_REPLACE = "REPLACE";
    private static final String OP_STATUS_RUNNING = "RUNNING";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String OP_STATUS_CANCELLED = "CANCELLED";
    private static final String STOCK_POST_STATUS_POSTED = "POSTED";
    private static final String STOCK_POST_STATUS_INTERNAL_FLOW = "INTERNAL_FLOW";
    private static final String PLAN_LOCK_TYPE_WIP = "WIP";
    private static final String PLAN_LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String PLAN_LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String PLAN_LOCK_STATUS_RELEASED = "RELEASED";
    private static final String SOURCE_TYPE_WET = "WET";
    private static final String REF_DOC_TYPE_ROUGH_FIRST = "ROUGH_GRINDING_FIRST";
    private static final String REF_DOC_TYPE_ROUGH_SECOND = "ROUGH_GRINDING_SECOND";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private HcProcessReportService hcProcessReportService;
    @Resource
    private HcProcessReportMapper hcProcessReportMapper;
    @Resource
    private HcQtimeEvaluationService hcQtimeEvaluationService;
    @Resource
    private HcWetReportAbnormalPositionMapper hcWetReportAbnormalPositionMapper;
    @Resource
    private QmsFaiService qmsFaiService;
    @Resource
    private cn.iocoder.yudao.module.mes.service.qms.QmsNcPickQualificationService pickQualificationService;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcInvStockService hcInvStockService;
    @Resource
    private HcInvTxnLogMapper hcInvTxnLogMapper;
    @Resource
    private HcEquipmentMapper hcEquipmentMapper;
    @Resource
    private HcStationFormMapper hcStationFormMapper;
    @Resource
    private HcStationFormItemMapper hcStationFormItemMapper;
    @Resource
    private HcStationFormService hcStationFormService;
    @Resource
    private HcProcessFormRecordMapper hcProcessFormRecordMapper;
    @Resource
    private HcProcessFormRecordItemMapper hcProcessFormRecordItemMapper;
    @Resource
    private HcStationRecordMapper hcStationRecordMapper;
    @Resource
    private HcStationRecordItemMapper hcStationRecordItemMapper;
    @Resource
    private HcEquipmentConsumableStateMapper hcEquipmentConsumableStateMapper;
    @Resource
    private HcEquipmentConsumableRuleMapper hcEquipmentConsumableRuleMapper;
    @Resource
    private HcEquipmentConsumableEventMapper hcEquipmentConsumableEventMapper;
    @Resource
    private HcGrindingSourceBalanceMapper hcGrindingSourceBalanceMapper;
    @Resource
    private HcGrindingAllocationModeMapper hcGrindingAllocationModeMapper;
    @Resource
    private HcGrindingFirstAllocationMapper hcGrindingFirstAllocationMapper;
    @Resource
    private HcGrindingFirstDetailMapper hcGrindingFirstDetailMapper;
    @Resource
    private HcGrindingSecondDetailMapper hcGrindingSecondDetailMapper;
    @Resource
    private HcGrindingSegmentTimingMapper hcGrindingSegmentTimingMapper;
    @Resource
    private HcGrindingMiddleProductRecordMapper hcGrindingMiddleProductRecordMapper;
    @Resource
    private HcGrindingMiddleProductDetailMapper hcGrindingMiddleProductDetailMapper;
    @Resource
    private HcGrindingProductionRecordMapper hcGrindingProductionRecordMapper;
    @Resource
    private HcGrindingProductionLedgerMapper hcGrindingProductionLedgerMapper;
    @Resource
    private HcGrindingProductionRecordLedgerService hcGrindingProductionRecordLedgerService;

    @Override
    public PageResult<HcGrindingProductionRecordRespVO> getProductionRecordPage(
            HcGrindingProductionRecordPageReqVO reqVO) {
        List<HcGrindingProductionRecordRespVO> rows = getProductionRecordList(reqVO);
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
    public List<HcGrindingProductionRecordRespVO> getProductionRecordList(
            HcGrindingProductionRecordPageReqVO reqVO) {
        return hcGrindingProductionRecordMapper.selectProductionRecordList(reqVO);
    }

    @Override
    public HcRoughConsoleBoardRespVO getBoard(HcRoughConsoleBoardReqVO reqVO) {
        LocalDate recordDate = reqVO.getRecordDate() == null ? LocalDate.now() : reqVO.getRecordDate();
        HcPlanOrderOperationDO operation = reqVO.getPlanOperationId() == null ? null : hcPlanOrderOperationMapper.selectById(reqVO.getPlanOperationId());
        HcPlanOrderDO plan = null;
        if (reqVO.getPlanOperationId() != null && operation == null) {
            throw invalidParamException("计划工序不存在");
        }
        if (operation != null) {
            plan = hcPlanOrderMapper.selectById(operation.getPlanId());
            if (plan == null) {
                throw invalidParamException("生产计划不存在");
            }
        }
        Long equipmentId = firstNonNull(reqVO.getEquipmentId(), operation == null ? null : operation.getEquipmentId());

        List<HcGrindingFirstDetailDO> firstDetails = operation == null ? List.of() : hcGrindingFirstDetailMapper.selectListByPlanOperationId(operation.getId());
        List<HcGrindingSecondDetailDO> secondDetails = operation == null ? List.of() : hcGrindingSecondDetailMapper.selectListByPlanOperationId(operation.getId());
        refreshSecondSegmentInspectionSummaries(secondDetails);
        String allocationMotherBatchNo = operation == null ? null : resolveTimingMotherBatchNo(plan, operation);
        HcGrindingAllocationModeDO allocationMode = operation == null || StrUtil.isBlank(allocationMotherBatchNo) ? null
                : hcGrindingAllocationModeMapper.selectByMotherBatch(operation.getId(), allocationMotherBatchNo);
        List<HcGrindingFirstAllocationDO> firstAllocations = allocationMode == null ? List.of()
                : hcGrindingFirstAllocationMapper.selectListByModeId(allocationMode.getId());
        var allocatedFirstDetailIds = firstAllocations.stream()
                .map(HcGrindingFirstAllocationDO::getFirstDetailId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        HcRoughConsoleBoardRespVO respVO = new HcRoughConsoleBoardRespVO();
        HcRoughReportTaskRespVO task = operation == null ? buildEquipmentOnlyTask(equipmentId) : buildTask(plan, operation, equipmentId);
        respVO.setTask(task);
        respVO.setDailyChecks(getDailyCheckList(equipmentId, recordDate));
        respVO.setConsumables(equipmentId == null ? List.of() : hcEquipmentConsumableStateMapper.selectListByEquipment(equipmentId, PROCESS_CODE).stream()
                .map(this::toConsumableResp)
                .toList());
        respVO.setSourceBalances(hcGrindingSourceBalanceMapper.selectAvailableList(null).stream()
                .map(this::toSourceBalanceResp)
                .toList());
        // 新磨皮报工统一按一磨分段处理；未落库时仅表示尚未发生首个业务动作。
        String effectiveAllocationMode = allocationMode == null ? ALLOCATION_MODE_FIRST_ALLOCATED : allocationMode.getAllocationMode();
        respVO.setAllocationMode(effectiveAllocationMode);
        respVO.setFirstAllocationMode(effectiveAllocationMode);
        respVO.setFirstAllocationModeLocked(allocationMode != null);
        respVO.setFirstAllocations(firstAllocations.stream().map(this::toFirstAllocationResp).toList());
        respVO.setFirstReports(firstDetails.stream()
                .filter(detail -> !allocatedFirstDetailIds.contains(detail.getId()))
                .map(this::toFirstReportResp).toList());
        respVO.setSecondReports(secondDetails.stream().map(this::toSecondReportResp).toList());
        String timingMotherBatchNo = operation == null ? null : resolveTimingMotherBatchNo(plan, operation);
        List<HcRoughConsoleSegmentTimingRespVO> firstSegmentTimings =
                operation == null || StrUtil.isBlank(timingMotherBatchNo) ? List.of()
                        : hcGrindingSegmentTimingMapper.selectListByPlanOperationId(operation.getId(), timingMotherBatchNo, PASS_TYPE_FIRST)
                                .stream()
                                .filter(timing -> !MOTHER_BATCH_TIMING_MARK.equals(timing.getSegmentMark()))
                                .map(this::toSegmentTimingResp).toList();
        fillFirstSegmentTimingQtime(firstSegmentTimings, task);
        respVO.setFirstSegmentTimings(firstSegmentTimings);
        List<HcRoughConsoleSegmentTimingRespVO> secondSegmentTimings =
                operation == null || StrUtil.isBlank(timingMotherBatchNo) ? List.of()
                        : hcGrindingSegmentTimingMapper.selectListByPlanOperationId(operation.getId(), timingMotherBatchNo, PASS_TYPE_SECOND)
                                .stream().map(this::toSegmentTimingResp).toList();
        fillSecondSegmentTimingQtime(secondSegmentTimings, task, firstAllocations, secondDetails);
        respVO.setSecondSegmentTimings(secondSegmentTimings);
        respVO.setFirstProcessLength(sum(firstDetails, HcGrindingFirstDetailDO::getProcessLength));
        respVO.setFirstLossLength(sum(firstDetails, HcGrindingFirstDetailDO::getLossLength));
        respVO.setFirstOutputLength(sum(firstDetails, HcGrindingFirstDetailDO::getOutputLength));
        respVO.setFirstNapSampleLength(sum(firstDetails, HcGrindingFirstDetailDO::getNapSampleLength));
        respVO.setSecondProcessLength(sum(secondDetails, HcGrindingSecondDetailDO::getProcessLength));
        respVO.setSecondLossLength(sum(secondDetails, HcGrindingSecondDetailDO::getLossLength));
        respVO.setSecondOutputLength(sum(secondDetails, HcGrindingSecondDetailDO::getOutputLength));
        respVO.setSecondNapSampleLength(sum(secondDetails, HcGrindingSecondDetailDO::getNapSampleLength));
        respVO.setSecondResearchConsumptionLength(sum(secondDetails, HcGrindingSecondDetailDO::getResearchConsumptionLength));
        BigDecimal secondSourceLength = respVO.getFirstOutputLength().compareTo(RANGE_EPS) > 0
                ? respVO.getFirstOutputLength()
                : respVO.getFirstProcessLength();
        respVO.setPendingSecondLength(nonNegative(secondSourceLength.subtract(respVO.getSecondProcessLength())));
        respVO.setDailyStartupDone(isDailyDone(respVO.getDailyChecks(), FORM_STARTUP));
        respVO.setDailyCleaningDone(isDailyDone(respVO.getDailyChecks(), FORM_CLEANING));
        return respVO;
    }

    private void fillFirstSegmentTimingQtime(List<HcRoughConsoleSegmentTimingRespVO> timings, HcRoughReportTaskRespVO task) {
        if (timings == null || timings.isEmpty() || task == null) {
            return;
        }
        for (HcRoughConsoleSegmentTimingRespVO timing : timings) {
            timing.setQtime(hcQtimeEvaluationService.evaluateWetToGrinding(
                    task.getPreviousEndTime(),
                    timing.getStartTime(),
                    task.getModelCode(),
                    task.getMotherModelCode(),
                    task.getProductionBatchNo(),
                    task.getParentProductionBatchNo(),
                    task.getBatchNo(),
                    timing.getSegmentBatchNo(),
                    timing.getSegmentMark()));
        }
    }

    /**
     * 二磨 QTIME 必须以对应一磨加工单元的完工时间为起点，不能复用湿法到一磨的规则。
     */
    private void fillSecondSegmentTimingQtime(List<HcRoughConsoleSegmentTimingRespVO> timings,
                                              HcRoughReportTaskRespVO task,
                                              List<HcGrindingFirstAllocationDO> firstAllocations,
                                              List<HcGrindingSecondDetailDO> secondDetails) {
        if (timings == null || timings.isEmpty() || task == null) {
            return;
        }
        Map<Long, HcGrindingFirstAllocationDO> firstAllocationById = firstAllocations.stream()
                .filter(allocation -> allocation.getId() != null)
                .collect(Collectors.toMap(HcGrindingFirstAllocationDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, HcGrindingSecondDetailDO> secondDetailById = secondDetails.stream()
                .filter(detail -> detail.getId() != null)
                .collect(Collectors.toMap(HcGrindingSecondDetailDO::getId, Function.identity(), (left, right) -> left));
        for (HcRoughConsoleSegmentTimingRespVO timing : timings) {
            HcGrindingSecondDetailDO secondDetail = timing.getSecondDetailId() == null
                    ? null : secondDetailById.get(timing.getSecondDetailId());
            HcGrindingFirstAllocationDO firstAllocation = resolveFirstAllocationForSecondTiming(
                    timing, secondDetail, firstAllocationById, firstAllocations);
            LocalDateTime secondStartTime = timing.getStartTime() == null && secondDetail != null
                    ? secondDetail.getStartTime() : timing.getStartTime();
            timing.setQtime(hcQtimeEvaluationService.evaluateFirstGrindingToSecondGrinding(
                    firstAllocation == null ? null : firstAllocation.getEndTime(),
                    secondStartTime,
                    task.getModelCode(),
                    task.getMotherModelCode(),
                    task.getProductionBatchNo(),
                    task.getParentProductionBatchNo(),
                    task.getBatchNo(),
                    timing.getSegmentBatchNo(),
                    timing.getSegmentMark()));
        }
    }

    private HcGrindingFirstAllocationDO resolveFirstAllocationForSecondTiming(
            HcRoughConsoleSegmentTimingRespVO timing,
            HcGrindingSecondDetailDO secondDetail,
            Map<Long, HcGrindingFirstAllocationDO> firstAllocationById,
            List<HcGrindingFirstAllocationDO> firstAllocations) {
        if (secondDetail != null && secondDetail.getFirstAllocationId() != null) {
            HcGrindingFirstAllocationDO allocation = firstAllocationById.get(secondDetail.getFirstAllocationId());
            if (allocation != null) {
                return allocation;
            }
        }
        String segmentMark = normalizeSecondTimingSegmentMark(timing.getSegmentMark());
        if (segmentMark == null) {
            return null;
        }
        List<HcGrindingFirstAllocationDO> matchedAllocations = firstAllocations.stream()
                .filter(allocation -> segmentMark.equals(normalizeSecondTimingSegmentMark(allocation.getSegmentMark())))
                .toList();
        return matchedAllocations.size() == 1 ? matchedAllocations.get(0) : null;
    }

    @Override
    public HcRoughFaiRespVO getFaiSummary(Long planId, Long planOperationId) {
        HcPlanOrderDO plan = validatePlan(planId);
        HcPlanOrderOperationDO operation = validateOperation(plan, planOperationId);
        HcProcessReportDO latestStartReport = getLatestStartReport(operation.getId());
        QmsFaiOrderDO latestFai = qmsFaiOrderMapper.selectLatestBySourceReportNo(buildRoughTaskNo(plan, operation),
                SOURCE_MENU_CODE_ROUGH);
        if (latestFai == null && latestStartReport != null && latestStartReport.getFaiId() != null) {
            latestFai = qmsFaiOrderMapper.selectById(latestStartReport.getFaiId());
        }
        if (latestFai != null) {
            syncRoughReportFaiSummary(latestStartReport, latestFai);
            return buildRoughFaiResp(latestFai, latestStartReport);
        }
        return buildRoughFaiResp(null, latestStartReport);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcRoughFaiRespVO applyFai(HcRoughFaiApplyReqVO reqVO) {
        HcPlanOrderDO plan = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, reqVO.getPlanOperationId());
        assertOperationRunning(operation);
        HcProcessReportDO latestStartReport = getLatestStartReport(operation.getId());
        if (latestStartReport == null) {
            throw invalidParamException("当前工序未找到开工记录，不能提交首检申请");
        }
        if (latestStartReport.getEquipmentId() == null && StrUtil.isBlank(latestStartReport.getEquipmentCode())) {
            throw invalidParamException("请先选择并锁定磨皮机台");
        }
        String standardMatchMode = FAI_STANDARD_MATCH_PRODUCT_MODEL_PROCESS;
        String motherMaterialCode = firstNotBlank(latestStartReport.getMotherMaterialCode(),
                operation.getMotherMaterialCode(), plan.getMotherMaterialCode(),
                latestStartReport.getMaterialCode(), plan.getMaterialCode());
        String operationCode = firstNotBlank(latestStartReport.getOperationCode(), operation.getOpCode());
        String operationName = firstNotBlank(latestStartReport.getOperationName(), operation.getOpName());
        String productModel = resolveRoughFaiProductModel(latestStartReport, operation, plan);
        if (StrUtil.isBlank(productModel)) {
            throw invalidParamException("当前磨皮任务缺少产品型号，无法按产品型号与工段匹配 FAI 检验标准");
        }
        if (StrUtil.isBlank(operationCode) && StrUtil.isBlank(operationName)) {
            throw invalidParamException("当前任务缺少工段信息，无法按产品型号与工段匹配 FAI 检验标准");
        }

        String taskNo = buildRoughTaskNo(plan, operation);
        QmsFaiOrderDO latestFai = qmsFaiOrderMapper.selectLatestBySourceReportNo(taskNo, SOURCE_MENU_CODE_ROUGH);
        if (latestFai == null && latestStartReport.getFaiId() != null) {
            latestFai = qmsFaiOrderMapper.selectById(latestStartReport.getFaiId());
        }
        if (latestFai != null && !isFaiReapplyAllowed(latestFai)) {
            syncRoughReportFaiSummary(latestStartReport, latestFai);
            if (FAI_STATUS_COMPLETED.equals(latestFai.getStatus())) {
                throw invalidParamException("当前任务首检已完成，不能重复提交首检申请");
            }
            throw invalidParamException("当前任务已有未完成首检单，请先处理后再提交");
        }
        String triggerReason = firstNotBlank(reqVO.getTriggerReason(),
                latestFai != null && isFaiReapplyAllowed(latestFai) ? FAI_TRIGGER_REWORK_RECHECK : FAI_TRIGGER_NEW_ORDER);
        if (latestFai != null && isFaiReapplyAllowed(latestFai) && !FAI_TRIGGER_REWORK_RECHECK.equals(triggerReason)) {
            triggerReason = FAI_TRIGGER_REWORK_RECHECK;
        }

        String productBatchNo = firstNotBlank(latestStartReport.getProductionBatchNo(), latestStartReport.getBatchNo(),
                operation.getProductionBatchNo(), plan.getProductionBatchNo(), operation.getBatchNo(), plan.getBatchNo());
        if (StrUtil.isBlank(productBatchNo)) {
            throw invalidParamException("请先确认批次号，批次号将写入首件检验的产品批次");
        }

        QmsFaiSaveReqVO faiReqVO = new QmsFaiSaveReqVO();
        faiReqVO.setWorkOrderNo(plan.getPlanNo());
        faiReqVO.setSourceReportId(latestStartReport.getId());
        faiReqVO.setSourceReportNo(taskNo);
        faiReqVO.setSourceModule(SOURCE_MENU_CODE_ROUGH);
        faiReqVO.setSourceOperationCode(operationCode);
        faiReqVO.setSourceOperationName(operationName);
        faiReqVO.setPlanOrderId(plan.getId());
        faiReqVO.setStandardMatchMode(standardMatchMode);
        faiReqVO.setOperationCode(operationCode);
        faiReqVO.setOperationName(operationName);
        faiReqVO.setMachineId(latestStartReport.getEquipmentId());
        faiReqVO.setMachineCode(firstNotBlank(latestStartReport.getEquipmentCode(), operation.getEquipmentCode()));
        faiReqVO.setMachineName(firstNotBlank(latestStartReport.getEquipmentName(), operation.getEquipmentName()));
        faiReqVO.setMaterialId(firstNonNull(latestStartReport.getMotherMaterialId(), operation.getMotherMaterialId(), plan.getMotherMaterialId(),
                latestStartReport.getMaterialId(), plan.getMaterialId()));
        faiReqVO.setMaterialCode(motherMaterialCode);
        faiReqVO.setMaterialName(firstNotBlank(latestStartReport.getMotherMaterialName(), operation.getMotherMaterialName(),
                plan.getMotherMaterialName(), latestStartReport.getMaterialName(), plan.getMaterialName()));
        faiReqVO.setSpecification(plan.getSizeName());
        faiReqVO.setProductModel(productModel);
        faiReqVO.setProductBatchNo(productBatchNo);
        faiReqVO.setProcessCategory(PROCESS_CODE);
        faiReqVO.setSubmissionType(FAI_SUBMISSION_TYPE_MASS_SHIPMENT);
        faiReqVO.setTriggerReason(triggerReason);
        faiReqVO.setStatus(FAI_STATUS_PENDING);
        faiReqVO.setJudgment(FAI_JUDGMENT_PENDING);
        faiReqVO.setSubmissionTime(LocalDateTime.now());
        faiReqVO.setSubmitterName(firstNotBlank(reqVO.getSubmitterName(), SecurityFrameworkUtils.getLoginUserNickname(),
                latestStartReport.getRecorderName(), "系统"));
        faiReqVO.setRemark(firstNotBlank(reqVO.getRemark(), "磨皮操作看板首检申请"));
        Long faiId = qmsFaiService.createFaiForInspectionPush(faiReqVO);
        QmsFaiOrderDO createdFai = qmsFaiOrderMapper.selectById(faiId);
        syncRoughReportFaiSummary(latestStartReport, createdFai);
        return buildRoughFaiResp(createdFai, latestStartReport);
    }

    @Override
    public HcRoughSecondSegmentInspectionRespVO getSecondSegmentInspectionSummary(Long secondDetailId) {
        HcGrindingSecondDetailDO detail = validateSecondDetail(secondDetailId);
        QmsFaiOrderDO latestFai = selectLatestSecondSegmentFai(detail.getId());
        if (latestFai != null) {
            syncSecondSegmentInspectionSummary(detail, latestFai);
        }
        return buildSecondSegmentInspectionResp(detail, latestFai);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcRoughSecondSegmentInspectionRespVO applySecondSegmentInspection(HcRoughSecondSegmentInspectionApplyReqVO reqVO) {
        HcGrindingSecondDetailDO detail = validateSecondDetail(reqVO.getSecondDetailId());
        if (StrUtil.isBlank(detail.getProductionBatchNo())) {
            throw invalidParamException("二次磨皮分段生产批次号不能为空，不能提交留样送检");
        }
        QmsFaiOrderDO latestFai = selectLatestSecondSegmentFai(detail.getId());
        if (latestFai != null && !isFaiReapplyAllowed(latestFai)) {
            syncSecondSegmentInspectionSummary(detail, latestFai);
            return buildSecondSegmentInspectionResp(detail, latestFai);
        }
        BigDecimal sampleLength = zero(reqVO.getSampleLength());
        if (sampleLength.compareTo(BigDecimal.ZERO) <= 0) {
            sampleLength = zero(detail.getNapSampleLength());
        }
        if (sampleLength.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("请先在二次磨皮报工中填写 NAP留样米数(m)");
        }
        BigDecimal maxSampleLength = nonNegative(zero(detail.getProcessLength()).subtract(zero(detail.getLossLength())));
        if (maxSampleLength.compareTo(BigDecimal.ZERO) > 0 && sampleLength.compareTo(maxSampleLength) > 0) {
            throw invalidParamException("留样送检米数不能大于本段可留样米数 " + toPlain(maxSampleLength));
        }
        syncSecondSegmentSampleLength(detail, sampleLength);

        HcPlanOrderDO plan = validatePlan(detail.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, detail.getPlanOperationId());
        HcEquipmentDO equipment = detail.getEquipmentId() == null ? null : hcEquipmentMapper.selectById(detail.getEquipmentId());
        String operationCode = firstNotBlank(operation.getOpCode(), detail.getWorkCenterCode(), PROCESS_CODE);
        String operationName = firstNotBlank(operation.getOpName(), detail.getWorkCenterName(), PROCESS_NAME);
        String machineCode = firstNotBlank(detail.getEquipmentCode(), equipment == null ? null : equipment.getEquipmentCode(),
                operation.getEquipmentCode());
        if (StrUtil.isBlank(machineCode)) {
            throw invalidParamException("二次磨皮明细缺少机台编号，不能提交留样送检");
        }
        String motherMaterialCode = firstNotBlank(operation.getMotherMaterialCode(), plan.getMotherMaterialCode(),
                plan.getMaterialCode());
        String productModel = resolveRoughFaiProductModel(null, operation, plan);
        if (StrUtil.isBlank(productModel)) {
            throw invalidParamException("当前磨皮任务缺少产品型号，无法按产品型号与工段匹配 FAI 检验标准");
        }
        String motherBatchNo = firstNotBlank(detail.getMotherBatchNo(), detail.getParentProductionBatchNo(),
                detail.getSourceProductionBatchNo());
        String sourceReportNo = buildSecondSegmentInspectionSourceNo(detail, plan, operation);

        QmsFaiSaveReqVO faiReqVO = new QmsFaiSaveReqVO();
        faiReqVO.setWorkOrderNo(plan.getPlanNo());
        faiReqVO.setSourceReportId(detail.getId());
        faiReqVO.setSourceReportNo(sourceReportNo);
        faiReqVO.setSourceModule(SOURCE_MENU_CODE_ROUGH_SECOND_SEGMENT);
        faiReqVO.setSourceOperationCode(operationCode);
        faiReqVO.setSourceOperationName(operationName);
        faiReqVO.setPlanOrderId(plan.getId());
        faiReqVO.setStandardMatchMode(FAI_STANDARD_MATCH_PRODUCT_MODEL_PROCESS);
        faiReqVO.setOperationCode(operationCode);
        faiReqVO.setOperationName(operationName);
        faiReqVO.setMachineId(firstNonNull(detail.getEquipmentId(), operation.getEquipmentId()));
        faiReqVO.setMachineCode(machineCode);
        faiReqVO.setMachineName(firstNotBlank(detail.getEquipmentName(), equipment == null ? null : equipment.getEquipmentName(),
                operation.getEquipmentName()));
        faiReqVO.setMaterialId(firstNonNull(operation.getMotherMaterialId(), plan.getMotherMaterialId(), plan.getMaterialId()));
        faiReqVO.setMaterialCode(motherMaterialCode);
        faiReqVO.setMaterialName(firstNotBlank(operation.getMotherMaterialName(), plan.getMotherMaterialName(),
                plan.getMaterialName()));
        faiReqVO.setSpecification(plan.getSizeName());
        faiReqVO.setProductModel(productModel);
        faiReqVO.setProductBatchNo(detail.getProductionBatchNo());
        faiReqVO.setProcessCategory(PROCESS_CODE);
        faiReqVO.setSubmissionType(FAI_SUBMISSION_TYPE_MASS_SHIPMENT);
        faiReqVO.setWetSampleType(FAI_SAMPLE_TYPE_SECOND_SEGMENT);
        faiReqVO.setSampleLength(sampleLength);
        faiReqVO.setTriggerReason(latestFai != null && isFaiReapplyAllowed(latestFai)
                ? FAI_TRIGGER_REWORK_RECHECK : FAI_TRIGGER_NEW_ORDER);
        faiReqVO.setStatus(FAI_STATUS_PENDING);
        faiReqVO.setJudgment(FAI_JUDGMENT_PENDING);
        faiReqVO.setSubmissionTime(LocalDateTime.now());
        faiReqVO.setSubmitterName(firstNotBlank(reqVO.getSubmitterName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        faiReqVO.setRemark(buildSecondSegmentInspectionRemark(detail, motherBatchNo, reqVO.getRemark()));
        Long faiId = qmsFaiService.createFaiForInspectionPush(faiReqVO);
        QmsFaiOrderDO createdFai = qmsFaiOrderMapper.selectById(faiId);
        syncSecondSegmentInspectionSummary(detail, createdFai);
        return buildSecondSegmentInspectionResp(detail, createdFai);
    }

    @Override
    public List<HcWetReportAbnormalPositionRespVO> getAbnormalPositionsByMotherBatchNo(String motherBatchNo) {
        String batchNo = StrUtil.trim(motherBatchNo);
        if (StrUtil.isBlank(batchNo)) {
            return List.of();
        }
        return hcWetReportAbnormalPositionMapper.selectListByRelatedMotherBatchNo(batchNo)
                .stream()
                .map(this::buildAbnormalPositionResp)
                .toList();
    }

    @Override
    public List<HcEquipmentSelectOptionRespVO> getEquipmentOptions(Long planOperationId, Long workCenterId) {
        HcPlanOrderOperationDO operation = planOperationId == null ? null : hcPlanOrderOperationMapper.selectById(planOperationId);
        List<HcEquipmentDO> equipments = new ArrayList<>();
        if (operation != null && operation.getEquipmentId() != null) {
            HcEquipmentDO equipment = hcEquipmentMapper.selectById(operation.getEquipmentId());
            if (isEnabledEquipment(equipment)) {
                equipments.add(equipment);
            }
        }
        Long targetWorkCenterId = operation != null && operation.getWorkCenterId() != null ? operation.getWorkCenterId() : workCenterId;
        if (equipments.isEmpty() && targetWorkCenterId != null) {
            equipments = hcEquipmentMapper.selectList(new LambdaQueryWrapperX<HcEquipmentDO>()
                    .eq(HcEquipmentDO::getWorkCenterId, targetWorkCenterId)
                    .eq(HcEquipmentDO::getStatus, 0)
                    .orderByAsc(HcEquipmentDO::getId));
        }
        if (equipments.isEmpty() && operation == null && workCenterId == null) {
            equipments = hcEquipmentMapper.selectList(new LambdaQueryWrapperX<HcEquipmentDO>()
                    .eq(HcEquipmentDO::getStatus, 0)
                    .and(wrapper -> wrapper.like(HcEquipmentDO::getWorkCenterName, PROCESS_NAME)
                            .or().like(HcEquipmentDO::getEquipmentName, PROCESS_NAME)
                            .or().like(HcEquipmentDO::getWorkCenterCode, "GRIND")
                            .or().like(HcEquipmentDO::getEquipmentCode, "GRIND"))
                    .orderByAsc(HcEquipmentDO::getId));
        }
        return equipments.stream().map(this::toEquipmentOption).toList();
    }

    @Override
    public List<HcWetPassWorkRespVO> getDailyCheckList(Long equipmentId, LocalDate recordDate) {
        LocalDate bizDate = recordDate == null ? LocalDate.now() : recordDate;
        List<HcStationFormDO> forms = hcStationFormMapper.selectEnabledByProcess(PROCESS_CODE).stream()
                .filter(form -> DAILY_FORM_CODES.contains(form.getFormCode()))
                .sorted(Comparator.comparing(HcStationFormDO::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(HcStationFormDO::getId))
                .toList();
        if (forms.isEmpty()) {
            return List.of();
        }
        List<Long> formIds = forms.stream().map(HcStationFormDO::getId).toList();
        Map<Long, List<HcStationFormItemDO>> templateItemMap = hcStationFormItemMapper.selectByFormIds(formIds).stream()
                .collect(Collectors.groupingBy(HcStationFormItemDO::getFormId, LinkedHashMap::new, Collectors.toList()));
        Map<String, HcStationRecordDO> recordMap = new LinkedHashMap<>();
        if (equipmentId != null) {
            for (HcStationRecordDO record : hcStationRecordMapper.selectListByEquipmentDaily(equipmentId, bizDate, DAILY_FORM_CODES)) {
                recordMap.putIfAbsent(record.getFormCode(), record);
            }
        }
        Collection<Long> recordIds = recordMap.values().stream().map(HcStationRecordDO::getId).toList();
        Map<Long, List<HcStationRecordItemDO>> recordItemMap = hcStationRecordItemMapper.selectByRecordIds(recordIds).stream()
                .collect(Collectors.groupingBy(HcStationRecordItemDO::getRecordId, LinkedHashMap::new, Collectors.toList()));

        List<HcWetPassWorkRespVO> result = new ArrayList<>();
        for (HcStationFormDO form : forms) {
            HcStationRecordDO record = recordMap.get(form.getFormCode());
            HcWetPassWorkRespVO respVO = new HcWetPassWorkRespVO();
            respVO.setRecordId(record == null ? null : record.getId());
            respVO.setFormId(form.getId());
            respVO.setFormCode(form.getFormCode());
            respVO.setId(form.getFormCode());
            respVO.setName(form.getFormName());
            respVO.setTiming(form.getTriggerTimingName());
            respVO.setStatus(record == null ? "PENDING" : record.getDocStatus());
            respVO.setResult(record == null ? "未填写" : record.getResultStatus());
            respVO.setInspectionResult(record == null ? null : record.getInspectionResult());
            respVO.setCanFill(record == null || !DOC_STATUS_CONFIRMED.equals(record.getDocStatus()));
            respVO.setCanConfirm(record != null && DOC_STATUS_RECORDED.equals(record.getDocStatus()));
            respVO.setCanView(true);
            LocalDate effectiveRecordDate = record == null || record.getRecordDate() == null ? bizDate : record.getRecordDate();
            respVO.setRecordDate(effectiveRecordDate.toString());
            respVO.setRecorder(record == null ? null : record.getRecordUserName());
            respVO.setRecorderTime(format(record == null ? null : record.getRecordTime(), effectiveRecordDate));
            respVO.setConfirmer(record == null ? null : record.getConfirmUserName());
            respVO.setConfirmerTime(format(record == null ? null : record.getConfirmTime(), effectiveRecordDate));
            respVO.setFormRemark(record == null ? null : record.getFormRemark());
            respVO.setConfirmRemark(record == null ? null : record.getConfirmRemark());
            respVO.setPresetHeaderDataJson(form.getPresetHeaderDataJson());
            respVO.setHeaderDataJson(record == null ? form.getPresetHeaderDataJson() : record.getHeaderDataJson());
            respVO.setSchemaJson(form.getSchemaJson());
            List<HcWetPassWorkItemRespVO> presetDetails = buildWorkDetails(null, templateItemMap.get(form.getId()));
            respVO.setPresetDetails(presetDetails);
            respVO.setDetails(buildWorkDetails(recordItemMap.get(record == null ? null : record.getId()), templateItemMap.get(form.getId())));
            result.add(respVO);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDailyCheck(HcRoughConsoleDailyCheckSaveReqVO reqVO) {
        return saveDailyCheckInternal(reqVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmDailyCheck(HcRoughConsoleDailyCheckSaveReqVO reqVO) {
        return saveDailyCheckInternal(reqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long replaceConsumable(HcRoughConsoleConsumableReplaceReqVO reqVO) {
        var consumptionBooking = grindingConsumptionService.begin("REPLACE", reqVO.getConsumption(), reqVO);
        if (consumptionBooking != null && consumptionBooking.getResultId() != null) return consumptionBooking.getResultId();
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("设备不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventTime = reqVO.getReplaceTime() == null ? now : reqVO.getReplaceTime();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        HcEquipmentConsumableStateDO state = reqVO.getStateId() == null ? null : hcEquipmentConsumableStateMapper.selectById(reqVO.getStateId());
        if (state == null) {
            state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(reqVO.getEquipmentId(), PROCESS_CODE, reqVO.getConsumableType());
        }
        boolean newState = state == null;
        String beforeBatchNo = state == null ? null : state.getBatchNo();
        Integer beforeCount = state == null ? null : state.getUseCount();
        BigDecimal beforeLength = state == null ? null : state.getUsedLength();
        if (newState) {
            state = HcEquipmentConsumableStateDO.builder()
                    .equipmentId(equipment.getId())
                    .equipmentCode(equipment.getEquipmentCode())
                    .equipmentName(equipment.getEquipmentName())
                    .workCenterId(equipment.getWorkCenterId())
                    .workCenterCode(equipment.getWorkCenterCode())
                    .workCenterName(equipment.getWorkCenterName())
                    .processCode(PROCESS_CODE)
                    .processName(PROCESS_NAME)
                    .consumableType(reqVO.getConsumableType())
                    .tenantId(equipment.getTenantId())
                    .build();
        }
        ConsumableRuleConfig ruleConfig = resolveConsumableRule(equipment, reqVO.getConsumableType());
        state.setBatchNo(reqVO.getBatchNo());
        state.setLastReplaceTime(eventTime);
        state.setLastReplacePlanNo(reqVO.getPlanNo());
        state.setLastReplaceReason(reqVO.getReplaceReason());
        state.setUseCount(reqVO.getInitialUseCount() == null ? 0 : reqVO.getInitialUseCount());
        state.setUsedLength(reqVO.getInitialUsedLength() == null ? BigDecimal.ZERO : reqVO.getInitialUsedLength());
        if (CONSUMABLE_SANDPAPER.equals(reqVO.getConsumableType())) {
            state.setLimitCount(null);
            state.setLimitLength(DEFAULT_SANDPAPER_LIMIT_LENGTH);
        } else {
            state.setLimitCount(firstNonNull(reqVO.getLimitCount(), ruleConfig.limitCount));
            state.setLimitLength(firstNonNull(reqVO.getLimitLength(), ruleConfig.limitLength));
        }
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus("IN_USE");
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        if (newState) {
            hcEquipmentConsumableStateMapper.insert(state);
        } else {
            hcEquipmentConsumableStateMapper.updateById(state);
        }
        Long replacementEventId = insertConsumableEvent(state, reqVO.getPlanId(), reqVO.getPlanNo(), reqVO.getPlanOperationId(),
                reqVO.getOperationCode(), reqVO.getOperationName(), null, null, "REPLACE", beforeBatchNo,
                reqVO.getBatchNo(), beforeCount, state.getUseCount(), beforeLength, state.getUsedLength(),
                BigDecimal.ZERO, reqVO.getReplaceReason(), operatorId, operatorName, eventTime, equipment.getTenantId());
        grindingConsumptionService.finish(consumptionBooking, replacementEventId,
                state.getId(), reqVO.getConsumption(), CONSUMABLE_SANDPAPER.equals(reqVO.getConsumableType()), reqVO.getBatchNo(),
                CONSUMABLE_GUIDE_CLOTH.equals(reqVO.getConsumableType()), reqVO.getBatchNo(), eventTime, reqVO.getPlanNo(), null);
        return state.getId();
    }

    @Override
    public HcRoughConsoleSourceBalanceRespVO scanSource(String batchNo) {
        if (StrUtil.isBlank(batchNo)) {
            throw invalidParamException("请扫描或输入来源母批号");
        }
        HcGrindingSourceBalanceDO balance = hcGrindingSourceBalanceMapper.selectByBatchNo(batchNo.trim());
        if (balance == null) {
            throw invalidParamException("未找到可加工母料来源");
        }
        return toSourceBalanceResp(balance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long stampSegmentTiming(HcRoughConsoleSegmentTimingStampReqVO reqVO) {
        HcPlanOrderDO plan = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, reqVO.getPlanOperationId());
        assertOperationRunning(operation);
        String passType = normalizeTimingPassType(reqVO.getPassType());
        String segmentMark = normalizeTimingSegmentMark(reqVO.getSegmentMark(), passType);
        String action = StrUtil.trimToEmpty(reqVO.getAction()).toUpperCase();
        String motherBatchNo = resolveTimingMotherBatchNo(plan, operation);
        if (StrUtil.isBlank(motherBatchNo)) {
            throw invalidParamException("当前计划未带出母批批号，不能记录开工完工时间");
        }
        HcGrindingAllocationModeDO allocationMode = PASS_TYPE_FIRST.equals(passType)
                ? ensureFirstAllocatedMode(plan, operation, motherBatchNo) : null;
        if (PASS_TYPE_FIRST.equals(passType)) {
            if (!ALLOCATION_MODE_FIRST_ALLOCATED.equals(allocationMode.getAllocationMode())) {
                throw invalidParamException("当前母批存在历史原有母批报工，不能再记录一磨分段时间");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        if (SEGMENT_TIMING_ACTION_START.equals(action)) {
            HcGrindingSegmentTimingDO existed = hcGrindingSegmentTimingMapper.selectBySegment(
                    operation.getId(), motherBatchNo, passType, segmentMark);
            if (existed != null && existed.getStartTime() != null) {
                throw invalidParamException(getTimingDisplayName(passType, segmentMark) + "已记录开工时间，不能重复点击");
            }
            if (existed != null) {
                int updated = hcGrindingSegmentTimingMapper.markStartOnce(existed.getId(), now, operatorId, operatorName,
                        plan.getTenantId());
                if (updated <= 0) {
                    throw invalidParamException(getTimingDisplayName(passType, segmentMark) + "已记录开工时间，不能重复点击");
                }
                if (PASS_TYPE_SECOND.equals(passType)) {
                    existed.setStartTime(now);
                    backfillSecondReportStartTime(existed, now);
                }
                return existed.getId();
            }
            HcGrindingSegmentTimingDO timing = buildSegmentTiming(plan, operation, motherBatchNo, passType,
                    segmentMark, null, null);
            timing.setStartTime(now);
            timing.setStartOperatorId(operatorId);
            timing.setStartOperatorName(operatorName);
            try {
                hcGrindingSegmentTimingMapper.insert(timing);
            } catch (DuplicateKeyException exception) {
                throw invalidParamException(getTimingDisplayName(passType, segmentMark) + "已记录开工时间，不能重复点击");
            }
            if (PASS_TYPE_SECOND.equals(passType)) {
                backfillSecondReportStartTime(timing, now);
            }
            return timing.getId();
        }
        if (!SEGMENT_TIMING_ACTION_END.equals(action)) {
            throw invalidParamException("时间动作只能是 START 或 END");
        }
        HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(
                operation.getId(), motherBatchNo, passType, segmentMark);
        if (timing == null || timing.getStartTime() == null) {
            throw invalidParamException(getTimingDisplayName(passType, segmentMark) + "尚未开工，不能记录完工时间");
        }
        if (timing.getEndTime() != null) {
            throw invalidParamException(getTimingDisplayName(passType, segmentMark) + "已记录完工时间，不能重复点击");
        }
        int updated = hcGrindingSegmentTimingMapper.markEndOnce(timing.getId(), now, operatorId, operatorName,
                plan.getTenantId());
        if (updated <= 0) {
            throw invalidParamException(getTimingDisplayName(passType, segmentMark) + "已记录完工时间，不能重复点击");
        }
        if (PASS_TYPE_SECOND.equals(passType)) {
            backfillSecondReportEndTime(timing, now);
        }
        return timing.getId();
    }

    /**
     * 原有母批开工、完工入口仅保留接口兼容，新的磨皮报工必须使用分段时间事实。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long stampOriginalMotherBatchTime(HcRoughConsoleOriginalMotherTimeStampReqVO reqVO) {
        throw invalidParamException("磨皮一磨已统一使用P/Q/R/S/不分段报工，请记录对应分段开工完工时间");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFirstReport(HcRoughConsoleFirstReportSaveReqVO reqVO) {
        throw invalidParamException("磨皮一磨已统一使用P/Q/R/S/不分段加工单元，请通过一磨分段报工保存");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFirstAllocationMode(HcRoughConsoleAllocationModeSaveReqVO reqVO) {
        HcPlanOrderDO plan = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, reqVO.getPlanOperationId());
        assertOperationRunning(operation);
        String motherBatchNo = resolveAllocationMotherBatchNo(plan, operation, reqVO.getMotherBatchNo());
        normalizeAllocationMode(reqVO.getAllocationMode());
        return ensureFirstAllocatedMode(plan, operation, motherBatchNo).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFirstAllocation(HcRoughConsoleFirstAllocationSaveReqVO reqVO) {
        var consumptionBooking = grindingConsumptionService.begin("FIRST", reqVO.getConsumption(), reqVO);
        if (consumptionBooking != null && consumptionBooking.getResultId() != null) return consumptionBooking.getResultId();
        HcPlanOrderDO plan = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, reqVO.getPlanOperationId());
        assertOperationRunning(operation);
        HcEquipmentDO equipment = validateEquipment(reqVO.getEquipmentId(), operation);
        String motherBatchNo = resolveAllocationMotherBatchNo(plan, operation, reqVO.getMotherBatchNo());
        String segmentMark = normalizeFirstAllocationSegmentMark(reqVO.getSegmentMark());
        requireProductionRecordStartOperator(plan, operation, PASS_TYPE_FIRST, segmentMark);
        HcGrindingAllocationModeDO mode = ensureFirstAllocatedMode(plan, operation, motherBatchNo);
        if (!Objects.equals(mode.getPlanId(), plan.getId())) {
            throw invalidParamException("一磨模式锁与当前生产计划不一致");
        }
        ProcessCheckSnapshot processCheck = resolveConfirmedProcessCheckSnapshot(
                reqVO.getProcessFormRecordId(), plan, operation, PASS_TYPE_FIRST, segmentMark);
        if (!ALLOCATION_SEGMENT_NONE.equals(segmentMark)
                && hcGrindingFirstAllocationMapper.selectBySegment(mode.getId(), segmentMark) != null) {
            throw invalidParamException(firstAllocationDisplayName(segmentMark) + "已完成一磨报工，不能重复保存");
        }
        List<HcGrindingFirstAllocationDO> existingAllocations = hcGrindingFirstAllocationMapper.selectListByModeId(mode.getId());

        HcGrindingSourceBalanceDO balance = hcGrindingSourceBalanceMapper.selectByBatchNoForUpdate(motherBatchNo);
        if (balance == null) {
            throw invalidParamException("未找到当前母批的一磨来源余额");
        }
        BigDecimal confirmedLength = zero(reqVO.getConfirmedLength());
        if (confirmedLength.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("一磨确认加工米数必须大于0");
        }
        BigDecimal availableBefore = zero(balance.getAvailableLength());
        if (confirmedLength.subtract(availableBefore).compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("一磨确认加工米数不能超出当前来源余额");
        }
        BigDecimal requestedStart = zero(reqVO.getStartPosition());
        if (requestedStart.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException("一磨加工起位置不能小于0m");
        }
        BigDecimal requestedEnd = requestedStart.add(confirmedLength);
        BigDecimal totalLength = zero(balance.getTotalLength());
        if (totalLength.compareTo(BigDecimal.ZERO) <= 0) {
            totalLength = zero(mode.getAvailableLengthAtLock());
        }
        if (totalLength.compareTo(BigDecimal.ZERO) <= 0) {
            totalLength = availableBefore.add(zero(balance.getUsedFirstLength()));
        }
        if (requestedEnd.subtract(totalLength).compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("一磨加工区间 " + requestedStart.stripTrailingZeros().toPlainString() + "-"
                    + requestedEnd.stripTrailingZeros().toPlainString() + "m 超出母批总长度 "
                    + totalLength.stripTrailingZeros().toPlainString() + "m");
        }
        for (HcGrindingFirstAllocationDO item : existingAllocations) {
            BigDecimal existingStart = zero(item.getStartPosition());
            BigDecimal existingEnd = existingStart.add(zero(item.getConfirmedLength()));
            boolean overlapped = requestedStart.compareTo(existingEnd.subtract(RANGE_EPS)) < 0
                    && requestedEnd.compareTo(existingStart.add(RANGE_EPS)) > 0;
            if (overlapped) {
                throw invalidParamException("一磨加工区间 " + requestedStart.stripTrailingZeros().toPlainString() + "-"
                        + requestedEnd.stripTrailingZeros().toPlainString() + "m 与已报 "
                        + firstAllocationDisplayName(item.getSegmentMark()) + "区间 "
                        + existingStart.stripTrailingZeros().toPlainString() + "-"
                        + existingEnd.stripTrailingZeros().toPlainString() + "m 重叠");
            }
        }

        AllocationReportTime reportTime = resolveFirstAllocationReportTime(operation, motherBatchNo, segmentMark);
        LocalDateTime reportStartTime = reportTime.startTime();
        LocalDateTime reportEndTime = reportTime.endTime();
        validateReportTimeRange(reportStartTime, reportEndTime, "一次磨皮" + firstAllocationDisplayName(segmentMark));
        LocalDate reportDate = effectiveReportDate(null, reportStartTime);
        BigDecimal availableAfter = nonNegative(availableBefore.subtract(confirmedLength));

        ConsumableReportResult sandpaperResult = increaseReportConsumable(equipment, plan, operation, CONSUMABLE_SANDPAPER,
                reqVO.getCurrentSandpaperBatchNo(), reqVO.getSandpaperBatchNo(), reqVO.getSandpaperReplaceReason(),
                reqVO.getSandpaperChanged(), confirmedLength, PASS_TYPE_FIRST, null, reportEndTime,
                reqVO.getOperatorId(), reqVO.getOperatorName());
        ConsumableReportResult guideClothResult = increaseReportConsumable(equipment, plan, operation, CONSUMABLE_GUIDE_CLOTH,
                reqVO.getCurrentGuideClothBatchNo(), reqVO.getGuideClothBatchNo(), reqVO.getGuideClothReplaceReason(),
                reqVO.getGuideClothChanged(), confirmedLength, PASS_TYPE_FIRST, null, reportEndTime,
                reqVO.getOperatorId(), reqVO.getOperatorName());
        HcEquipmentConsumableStateDO sandpaper = sandpaperResult.state();
        HcEquipmentConsumableStateDO guideCloth = guideClothResult.state();

        HcGrindingFirstDetailDO firstDetail = buildFirstAllocationDetail(reqVO, plan, operation, equipment, balance,
                requestedStart, confirmedLength, availableBefore, availableAfter, reportDate, reportStartTime,
                reportEndTime, sandpaper, guideCloth);
        hcGrindingFirstDetailMapper.insert(firstDetail);
        Long checkRecordId = upsertProcessCheckStationRecord(null, BIZ_GRINDING_FIRST, firstDetail.getId(), PASS_TYPE_FIRST,
                buildProcessCheckHeaderJson(PASS_TYPE_FIRST, buildFirstAllocationBatchNo(motherBatchNo, segmentMark),
                        confirmedLength, processCheck.headerDataJson(), processCheck.recordId()),
                processCheck.details(), processCheck.form(), processCheck.recordId(),
                plan, operation, equipment, reqVO.getOperatorName(), reportDate);
        if (checkRecordId != null) {
            firstDetail.setCheckRecordId(checkRecordId);
            hcGrindingFirstDetailMapper.updateById(firstDetail);
        }

        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        HcGrindingFirstAllocationDO allocation = HcGrindingFirstAllocationDO.builder()
                .allocationModeId(mode.getId())
                .firstDetailId(firstDetail.getId())
                .planId(plan.getId())
                .planNo(plan.getPlanNo())
                .planOperationId(operation.getId())
                .motherBatchNo(motherBatchNo)
                .segmentMark(segmentMark)
                .productionBatchNo(buildFirstAllocationBatchNo(motherBatchNo, segmentMark))
                .startPosition(requestedStart)
                .confirmedLength(confirmedLength)
                .startTime(reportStartTime)
                .endTime(reportEndTime)
                .sandpaperStateId(sandpaper == null ? null : sandpaper.getId())
                .sandpaperLife(sandpaper == null ? null : sandpaper.getUsedLength())
                .sandpaperBatchNo(firstNotBlank(sandpaper == null ? null : sandpaper.getBatchNo(),
                        firstDetail.getCurrentSandpaperBatchNo(), firstDetail.getSandpaperBatchNo()))
                .guideClothStateId(guideCloth == null ? null : guideCloth.getId())
                .guideClothLife(guideCloth == null ? null : guideCloth.getUseCount())
                .guideClothBatchNo(firstNotBlank(guideCloth == null ? null : guideCloth.getBatchNo(),
                        firstDetail.getCurrentGuideClothBatchNo()))
                .checkRecordId(checkRecordId)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .detailStatus("SUBMITTED")
                .remark(reqVO.getRemark())
                .tenantId(plan.getTenantId())
                .build();
        try {
            hcGrindingFirstAllocationMapper.insert(allocation);
        } catch (DuplicateKeyException exception) {
            throw invalidParamException(firstAllocationDisplayName(segmentMark) + "已完成一磨报工，不能重复保存");
        }
        bindFirstAllocationSegmentTiming(firstDetail, plan, operation, motherBatchNo, segmentMark);
        backfillConsumableEventBizId(sandpaper, firstDetail.getId(), PASS_TYPE_FIRST);
        backfillConsumableEventBizId(guideCloth, firstDetail.getId(), PASS_TYPE_FIRST);
        balance.setUsedFirstLength(zero(balance.getUsedFirstLength()).add(confirmedLength));
        balance.setAvailableLength(availableAfter);
        balance.setLastReportTime(reportEndTime);
        balance.setStatus(availableAfter.compareTo(RANGE_EPS) > 0 ? "AVAILABLE" : "USED_UP");
        hcGrindingSourceBalanceMapper.updateById(balance);
        syncFirstAllocationProductionRecord(plan, operation, firstDetail, allocation, sandpaper,
                sandpaperResult.sandpaperSegments(), guideCloth, reqVO);
        grindingConsumptionService.finish(consumptionBooking, firstDetail.getId(), allocation.getId(), reqVO.getConsumption(),
                Boolean.TRUE.equals(reqVO.getSandpaperChanged()), reqVO.getSandpaperBatchNo(),
                Boolean.TRUE.equals(reqVO.getGuideClothChanged()), reqVO.getGuideClothBatchNo(), reportEndTime,
                plan.getPlanNo(), allocation.getProductionBatchNo());
        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviseFirstAllocationQuantity(HcRoughConsoleFirstAllocationQuantityReviseReqVO reqVO) {
        BigDecimal newLength = zero(reqVO.getConfirmedLength());
        if (newLength.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("修正后确认加工米数必须大于0");
        }
        String reason = StrUtil.trim(reqVO.getReason());
        if (StrUtil.isBlank(reason)) {
            throw invalidParamException("修正原因不能为空");
        }
        HcGrindingFirstAllocationDO allocation = hcGrindingFirstAllocationMapper.selectByIdForUpdate(
                reqVO.getFirstAllocationId());
        if (allocation == null || Boolean.TRUE.equals(allocation.getDeleted())) {
            throw invalidParamException("一磨加工单元不存在");
        }
        HcGrindingSecondDetailDO secondDetail = hcGrindingSecondDetailMapper.selectByFirstAllocationId(allocation.getId());
        if (secondDetail != null && !Boolean.TRUE.equals(secondDetail.getDeleted())) {
            throw invalidParamException("该一磨加工单元已存在对应二磨报工，不能修正一磨米数");
        }
        HcGrindingFirstDetailDO firstDetail = hcGrindingFirstDetailMapper.selectById(allocation.getFirstDetailId());
        if (firstDetail == null || Boolean.TRUE.equals(firstDetail.getDeleted())) {
            throw invalidParamException("一磨加工单元对应的报工明细不存在");
        }
        HcPlanOrderDO plan = validatePlan(allocation.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, allocation.getPlanOperationId());
        HcGrindingAllocationModeDO mode = hcGrindingAllocationModeMapper.selectByMotherBatchForUpdate(
                allocation.getPlanOperationId(), allocation.getMotherBatchNo());
        if (mode == null || !Objects.equals(mode.getId(), allocation.getAllocationModeId())
                || !ALLOCATION_MODE_FIRST_ALLOCATED.equals(mode.getAllocationMode())) {
            throw invalidParamException("一磨加工单元模式锁不存在或状态不一致");
        }
        HcGrindingSourceBalanceDO balance = hcGrindingSourceBalanceMapper.selectByBatchNoForUpdate(
                allocation.getMotherBatchNo());
        if (balance == null) {
            throw invalidParamException("未找到当前母批的一磨来源余额");
        }

        BigDecimal oldLength = zero(allocation.getConfirmedLength());
        BigDecimal delta = newLength.subtract(oldLength);
        if (isZeroQuantityDelta(delta)) {
            return allocation.getId();
        }
        BigDecimal startPosition = zero(allocation.getStartPosition());
        BigDecimal requestedEnd = startPosition.add(newLength);
        BigDecimal totalLength = firstNonNull(zero(balance.getTotalLength()), zero(mode.getAvailableLengthAtLock()));
        if (totalLength.compareTo(BigDecimal.ZERO) <= 0) {
            totalLength = zero(balance.getAvailableLength()).add(zero(balance.getUsedFirstLength()));
        }
        List<HcGrindingFirstAllocationDO> allocations = hcGrindingFirstAllocationMapper.selectListByModeId(mode.getId());
        List<HcGrindingFirstAllocationDO> orderedAllocations = allocations.stream()
                .sorted(Comparator
                        .comparing((HcGrindingFirstAllocationDO item) -> zero(item.getStartPosition()))
                        .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()))
                .toList();
        BigDecimal usedAfter = BigDecimal.ZERO;
        BigDecimal usedBeforeCurrent = BigDecimal.ZERO;
        boolean beforeCurrent = true;
        for (HcGrindingFirstAllocationDO item : orderedAllocations) {
            boolean currentItem = Objects.equals(item.getId(), allocation.getId());
            BigDecimal itemLength = currentItem ? newLength : zero(item.getConfirmedLength());
            BigDecimal itemStart = zero(item.getStartPosition());
            BigDecimal itemEnd = itemStart.add(itemLength);
            if (!currentItem) {
                boolean overlapped = startPosition.compareTo(itemEnd.subtract(RANGE_EPS)) < 0
                        && requestedEnd.compareTo(itemStart.add(RANGE_EPS)) > 0;
                if (overlapped) {
                    throw invalidParamException("一磨加工区间 " + rangeText(startPosition, requestedEnd)
                            + " 与已报 " + firstAllocationDisplayName(item.getSegmentMark()) + "区间 "
                            + rangeText(itemStart, itemEnd) + " 重叠");
                }
            }
            if (beforeCurrent && !currentItem) {
                usedBeforeCurrent = usedBeforeCurrent.add(itemLength);
            }
            if (currentItem) {
                beforeCurrent = false;
            }
            usedAfter = usedAfter.add(itemLength);
        }
        if (requestedEnd.subtract(totalLength).compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("一磨加工区间 " + rangeText(startPosition, requestedEnd)
                    + " 超出母批总长度 " + toPlain(totalLength) + "m");
        }
        if (usedAfter.subtract(totalLength).compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("一磨累计加工米数不能超出母批总长度");
        }

        String revisionRemark = buildQuantityRevisionRemark("一磨" + firstAllocationDisplayName(allocation.getSegmentMark())
                + "确认加工米数", oldLength, newLength, reason);
        HcEquipmentConsumableEventDO sandpaperEvent = reviseConsumableUsageTotal(BIZ_GRINDING_FIRST,
                firstDetail.getId(), CONSUMABLE_SANDPAPER, newLength, revisionRemark);
        reviseConsumableUsageTotal(BIZ_GRINDING_FIRST, firstDetail.getId(), CONSUMABLE_GUIDE_CLOTH,
                newLength, revisionRemark);
        BigDecimal sandpaperLife = sandpaperEvent == null
                ? (allocation.getSandpaperLife() == null ? null : zero(allocation.getSandpaperLife()).add(delta))
                : sandpaperEvent.getAfterUsedLength();
        BigDecimal reservedLength = zero(balance.getReservedLength());
        BigDecimal availableBefore = nonNegative(totalLength.subtract(usedBeforeCurrent).subtract(reservedLength));
        BigDecimal availableAfter = nonNegative(availableBefore.subtract(newLength));

        HcGrindingFirstAllocationDO updateAllocation = HcGrindingFirstAllocationDO.builder()
                .id(allocation.getId())
                .confirmedLength(newLength)
                .sandpaperLife(sandpaperLife)
                .remark(appendQuantityRevisionRemark(allocation.getRemark(), revisionRemark))
                .build();
        hcGrindingFirstAllocationMapper.updateById(updateAllocation);

        HcGrindingFirstDetailDO updateDetail = HcGrindingFirstDetailDO.builder()
                .id(firstDetail.getId())
                .remainLength(availableBefore)
                .processLength(newLength)
                .outputLength(newLength)
                .availableBefore(availableBefore)
                .availableAfter(availableAfter)
                .sandpaperLife(sandpaperLife)
                .remark(appendQuantityRevisionRemark(firstDetail.getRemark(), revisionRemark))
                .build();
        hcGrindingFirstDetailMapper.updateById(updateDetail);
        firstDetail.setProcessLength(newLength);
        firstDetail.setOutputLength(newLength);
        firstDetail.setAvailableBefore(availableBefore);
        firstDetail.setAvailableAfter(availableAfter);
        firstDetail.setSandpaperLife(sandpaperLife);
        firstDetail.setRemark(updateDetail.getRemark());
        allocation.setConfirmedLength(newLength);
        allocation.setSandpaperLife(sandpaperLife);
        allocation.setRemark(updateAllocation.getRemark());

        updateFirstAllocationSourceBalance(balance, usedAfter, totalLength, revisionRemark);
        reviseFirstAllocationModeSnapshot(mode, totalLength);
        refreshLaterFirstAllocationAvailabilitySnapshots(orderedAllocations, allocation.getId(), newLength,
                totalLength, reservedLength, revisionRemark);
        reviseFirstAllocationProcessCheckLength(firstDetail, allocation, newLength, revisionRemark);
        syncFirstAllocationProductionRecordQuantity(firstDetail, allocation, newLength, sandpaperLife, revisionRemark);
        shiftLaterFirstAllocationSandpaperSnapshots(mode.getId(), allocation, sandpaperEvent, delta, revisionRemark);

        log.info("[一磨分段报工米数修正] allocationId={}, firstDetailId={}, batchNo={}, beforeLength={}, afterLength={}, delta={}",
                allocation.getId(), firstDetail.getId(), allocation.getProductionBatchNo(), oldLength, newLength, delta);
        return allocation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviseStatisticsData(HcStatisticsDataReviseReqVO reqVO) {
        String role = StrUtil.trimToEmpty(reqVO.getRecordRole()).toUpperCase();
        if (PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION.equals(role)) {
            BigDecimal confirmedLength = firstNonNull(reqVO.getConfirmedLength(), reqVO.getProcessLength(), reqVO.getOutputLength());
            HcRoughConsoleFirstAllocationQuantityReviseReqVO quantityReqVO =
                    new HcRoughConsoleFirstAllocationQuantityReviseReqVO();
            quantityReqVO.setFirstAllocationId(firstNonNull(reqVO.getFirstAllocationId(), reqVO.getId()));
            quantityReqVO.setConfirmedLength(confirmedLength);
            quantityReqVO.setReason(reqVO.getReason());
            return reviseFirstAllocationQuantity(quantityReqVO);
        }
        if (PRODUCTION_RECORD_ROLE_SECOND.equals(role)) {
            return reviseSecondStatisticsData(reqVO);
        }
        return reviseFirstOriginalStatisticsData(reqVO);
    }

    private Long reviseFirstOriginalStatisticsData(HcStatisticsDataReviseReqVO reqVO) {
        String reason = requireStatisticsRevisionReason(reqVO.getReason());
        if (reqVO.getId() == null) {
            throw invalidParamException("磨皮一磨报工记录不能为空");
        }
        HcGrindingFirstDetailDO detail = hcGrindingFirstDetailMapper.selectOne(
                new LambdaQueryWrapperX<HcGrindingFirstDetailDO>()
                        .eq(HcGrindingFirstDetailDO::getId, reqVO.getId())
                        .eq(HcGrindingFirstDetailDO::getDeleted, false)
                        .last("FOR UPDATE"));
        if (detail == null) {
            throw invalidParamException("磨皮一磨报工记录不存在");
        }
        BigDecimal nextOutputLength = statisticsValueOrCurrent(
                firstNonNull(reqVO.getOutputLength(), reqVO.getProcessLength()), detail.getOutputLength());
        BigDecimal nextProcessLength = statisticsValueOrCurrent(reqVO.getProcessLength(), nextOutputLength);
        BigDecimal nextLossLength = statisticsValueOrCurrent(reqVO.getLossLength(), detail.getLossLength());
        requireNonNegative(nextProcessLength, "磨皮一磨统计投入米数");
        requireNonNegative(nextLossLength, "磨皮一磨统计固定损耗");
        requireNonNegative(nextOutputLength, "磨皮一磨统计产出米数");

        String revisionRemark = buildStatisticsRevisionRemark("磨皮一磨统计数据",
                Map.of(
                        "processLength", zero(detail.getProcessLength()),
                        "lossLength", zero(detail.getLossLength()),
                        "outputLength", zero(detail.getOutputLength())),
                Map.of(
                        "processLength", nextProcessLength,
                        "lossLength", nextLossLength,
                        "outputLength", nextOutputLength),
                reason);
        HcGrindingFirstDetailDO update = HcGrindingFirstDetailDO.builder()
                .id(detail.getId())
                .processLength(nextProcessLength)
                .lossLength(nextLossLength)
                .outputLength(nextOutputLength)
                .remark(appendQuantityRevisionRemark(detail.getRemark(), revisionRemark))
                .build();
        hcGrindingFirstDetailMapper.updateById(update);
        return detail.getId();
    }

    private Long reviseSecondStatisticsData(HcStatisticsDataReviseReqVO reqVO) {
        String reason = requireStatisticsRevisionReason(reqVO.getReason());
        if (reqVO.getId() == null) {
            throw invalidParamException("磨皮二磨报工记录不能为空");
        }
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectOne(
                new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                        .eq(HcGrindingSecondDetailDO::getId, reqVO.getId())
                        .eq(HcGrindingSecondDetailDO::getDeleted, false)
                        .last("FOR UPDATE"));
        if (detail == null) {
            throw invalidParamException("磨皮二磨报工记录不存在");
        }
        BigDecimal nextProcessLength = statisticsValueOrCurrent(
                firstNonNull(reqVO.getProcessLength(), reqVO.getOutputLength()), detail.getProcessLength());
        BigDecimal nextLossLength = statisticsValueOrCurrent(reqVO.getLossLength(), detail.getLossLength());
        requireNonNegative(nextProcessLength, "磨皮二磨统计报工米数");
        requireNonNegative(nextLossLength, "磨皮二磨统计固定损耗");
        BigDecimal nextOutputLength = calculateSecondOutputLength(nextProcessLength, nextLossLength,
                detail.getNapSampleLength(), detail.getResearchConsumptionLength(), sumPersistedSecondAbnormalLength(detail.getId()));
        requireNonNegative(nextOutputLength, "磨皮二磨统计产出米数");

        String revisionRemark = buildStatisticsRevisionRemark("磨皮二磨统计数据",
                Map.of(
                        "processLength", zero(detail.getProcessLength()),
                        "lossLength", zero(detail.getLossLength()),
                        "outputLength", zero(detail.getOutputLength())),
                Map.of(
                        "processLength", nextProcessLength,
                        "lossLength", nextLossLength,
                        "outputLength", nextOutputLength),
                reason);
        HcGrindingSecondDetailDO update = HcGrindingSecondDetailDO.builder()
                .id(detail.getId())
                .processLength(nextProcessLength)
                .lossLength(nextLossLength)
                .outputLength(nextOutputLength)
                .remark(appendQuantityRevisionRemark(detail.getRemark(), revisionRemark))
                .build();
        hcGrindingSecondDetailMapper.updateById(update);
        return detail.getId();
    }

    private String requireStatisticsRevisionReason(String reason) {
        String normalized = StrUtil.trim(reason);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("修订原因不能为空");
        }
        return normalized;
    }

    private BigDecimal statisticsValueOrCurrent(BigDecimal requested, BigDecimal current) {
        return requested == null ? zero(current) : requested;
    }

    private String buildStatisticsRevisionRemark(String label,
                                                 Map<String, BigDecimal> before,
                                                 Map<String, BigDecimal> after,
                                                 String reason) {
        return label + "由[" + formatStatisticsRevisionFields(before) + "]调整为["
                + formatStatisticsRevisionFields(after) + "]；原因：" + StrUtil.trim(reason)
                + "；时间：" + LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    private String formatStatisticsRevisionFields(Map<String, BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + toPlain(entry.getValue()))
                .collect(Collectors.joining(","));
    }

    private void refreshLaterFirstAllocationAvailabilitySnapshots(List<HcGrindingFirstAllocationDO> orderedAllocations,
                                                                  Long currentAllocationId,
                                                                  BigDecimal currentLength,
                                                                  BigDecimal totalLength,
                                                                  BigDecimal reservedLength,
                                                                  String revisionRemark) {
        if (orderedAllocations == null || orderedAllocations.isEmpty() || currentAllocationId == null) {
            return;
        }
        BigDecimal usedBefore = BigDecimal.ZERO;
        boolean afterCurrent = false;
        for (HcGrindingFirstAllocationDO item : orderedAllocations) {
            boolean currentItem = Objects.equals(item.getId(), currentAllocationId);
            BigDecimal itemLength = currentItem ? currentLength : zero(item.getConfirmedLength());
            BigDecimal availableBefore = nonNegative(zero(totalLength).subtract(usedBefore).subtract(zero(reservedLength)));
            BigDecimal availableAfter = nonNegative(availableBefore.subtract(itemLength));
            if (afterCurrent && item.getFirstDetailId() != null) {
                HcGrindingFirstDetailDO detail = hcGrindingFirstDetailMapper.selectById(item.getFirstDetailId());
                if (detail == null || Boolean.TRUE.equals(detail.getDeleted())) {
                    usedBefore = usedBefore.add(itemLength);
                    continue;
                }
                HcGrindingFirstDetailDO updateDetail = HcGrindingFirstDetailDO.builder()
                        .id(item.getFirstDetailId())
                        .remainLength(availableBefore)
                        .availableBefore(availableBefore)
                        .availableAfter(availableAfter)
                        .remark(appendQuantityRevisionRemark(detail.getRemark(), revisionRemark))
                        .build();
                hcGrindingFirstDetailMapper.updateById(updateDetail);
            }
            usedBefore = usedBefore.add(itemLength);
            if (currentItem) {
                afterCurrent = true;
            }
        }
    }

    private void updateFirstAllocationSourceBalance(HcGrindingSourceBalanceDO balance,
                                                    BigDecimal usedAfter,
                                                    BigDecimal totalLength,
                                                    String revisionRemark) {
        BigDecimal nextAvailableLength = totalLength.subtract(zero(usedAfter)).subtract(zero(balance.getReservedLength()));
        if (nextAvailableLength.compareTo(BigDecimal.ZERO) < 0) {
            nextAvailableLength = BigDecimal.ZERO;
        }
        HcGrindingSourceBalanceDO update = HcGrindingSourceBalanceDO.builder()
                .id(balance.getId())
                .usedFirstLength(usedAfter)
                .availableLength(nextAvailableLength)
                .lastReportTime(LocalDateTime.now())
                .status(nextAvailableLength.compareTo(BigDecimal.ZERO) > 0 ? "AVAILABLE" : "USED_UP")
                .remark(appendQuantityRevisionRemark(balance.getRemark(), revisionRemark))
                .build();
        hcGrindingSourceBalanceMapper.updateById(update);
    }

    private void reviseFirstAllocationModeSnapshot(HcGrindingAllocationModeDO mode, BigDecimal totalLength) {
        if (mode == null || totalLength == null || totalLength.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (zero(mode.getAvailableLengthAtLock()).compareTo(totalLength) >= 0) {
            return;
        }
        HcGrindingAllocationModeDO update = HcGrindingAllocationModeDO.builder()
                .id(mode.getId())
                .availableLengthAtLock(totalLength)
                .build();
        hcGrindingAllocationModeMapper.updateById(update);
    }

    private void reviseFirstAllocationProcessCheckLength(HcGrindingFirstDetailDO firstDetail,
                                                         HcGrindingFirstAllocationDO allocation,
                                                         BigDecimal newLength,
                                                         String revisionRemark) {
        HcStationRecordDO record = allocation.getCheckRecordId() == null ? null
                : hcStationRecordMapper.selectById(allocation.getCheckRecordId());
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            record = hcStationRecordMapper.selectOneByBiz(RECORD_SCOPE_PROCESS_DETAIL, BIZ_GRINDING_FIRST,
                    firstDetail.getId(), FORM_PROCESS_CHECK);
        }
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            return;
        }
        Map<String, Object> header = StrUtil.isBlank(record.getHeaderDataJson())
                ? new LinkedHashMap<>()
                : JsonUtils.parseObjectQuietly(record.getHeaderDataJson(), new TypeReference<Map<String, Object>>() {});
        if (header == null) {
            header = new LinkedHashMap<>();
        }
        header.put("processLength", toPlain(newLength));
        HcStationRecordDO update = new HcStationRecordDO();
        update.setId(record.getId());
        update.setHeaderDataJson(JsonUtils.toJsonString(header));
        update.setFormRemark(appendQuantityRevisionRemark(record.getFormRemark(), revisionRemark));
        hcStationRecordMapper.updateById(update);
    }

    private void syncFirstAllocationProductionRecordQuantity(HcGrindingFirstDetailDO firstDetail,
                                                             HcGrindingFirstAllocationDO allocation,
                                                             BigDecimal newLength,
                                                             BigDecimal sandpaperLife,
                                                             String revisionRemark) {
        List<HcGrindingProductionRecordDO> records = hcGrindingProductionLedgerMapper.selectListBySource(
                PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION, PASS_TYPE_FIRST, firstDetail.getId());
        if (records == null || records.isEmpty()) {
            return;
        }
        int maxSegmentNo = records.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getDeleted()))
                .map(HcGrindingProductionRecordDO::getSourceSegmentNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(1);
        for (HcGrindingProductionRecordDO record : records) {
            if (Boolean.TRUE.equals(record.getDeleted())) {
                continue;
            }
            HcGrindingProductionRecordDO update = HcGrindingProductionRecordDO.builder()
                    .id(record.getId())
                    .inputLength(newLength)
                    .outputLength(newLength)
                    .remark(appendQuantityRevisionRemark(record.getRemark(), revisionRemark))
                    .build();
            if (sandpaperLife != null
                    && (record.getSourceSegmentNo() == null || Objects.equals(record.getSourceSegmentNo(), maxSegmentNo))) {
                update.setSandpaperLife(sandpaperLife);
            }
            if (allocation.getProductionBatchNo() != null) {
                update.setBatchNo(allocation.getProductionBatchNo());
            }
            hcGrindingProductionLedgerMapper.updateById(update);
        }
    }

    private void shiftLaterFirstAllocationSandpaperSnapshots(Long allocationModeId,
                                                             HcGrindingFirstAllocationDO anchorAllocation,
                                                             HcEquipmentConsumableEventDO sandpaperEvent,
                                                             BigDecimal delta,
                                                             String revisionRemark) {
        if (allocationModeId == null || anchorAllocation == null || sandpaperEvent == null
                || sandpaperEvent.getStateId() == null || isZeroQuantityDelta(delta)) {
            return;
        }
        List<HcGrindingFirstAllocationDO> laterAllocations = hcGrindingFirstAllocationMapper.selectListByModeId(allocationModeId)
                .stream()
                .filter(item -> item.getId() != null && anchorAllocation.getId() != null
                        && item.getId() > anchorAllocation.getId())
                .filter(item -> Objects.equals(item.getSandpaperStateId(), sandpaperEvent.getStateId()))
                .toList();
        for (HcGrindingFirstAllocationDO item : laterAllocations) {
            BigDecimal nextSandpaperLife = item.getSandpaperLife() == null ? null : zero(item.getSandpaperLife()).add(delta);
            if (nextSandpaperLife != null) {
                requireNonNegative(nextSandpaperLife, "后续一磨加工单元砂纸寿命");
                HcGrindingFirstAllocationDO updateAllocation = HcGrindingFirstAllocationDO.builder()
                        .id(item.getId())
                        .sandpaperLife(nextSandpaperLife)
                        .build();
                hcGrindingFirstAllocationMapper.updateById(updateAllocation);
            }
            HcGrindingFirstDetailDO detail = item.getFirstDetailId() == null ? null
                    : hcGrindingFirstDetailMapper.selectById(item.getFirstDetailId());
            if (detail != null && !Boolean.TRUE.equals(detail.getDeleted()) && detail.getSandpaperLife() != null) {
                BigDecimal nextDetailSandpaperLife = zero(detail.getSandpaperLife()).add(delta);
                requireNonNegative(nextDetailSandpaperLife, "后续一磨明细砂纸寿命");
                HcGrindingFirstDetailDO updateDetail = HcGrindingFirstDetailDO.builder()
                        .id(detail.getId())
                        .sandpaperLife(nextDetailSandpaperLife)
                        .build();
                hcGrindingFirstDetailMapper.updateById(updateDetail);
                shiftFirstAllocationProductionRecordSandpaperLife(detail.getId(), delta, revisionRemark);
            }
        }
    }

    private void shiftFirstAllocationProductionRecordSandpaperLife(Long firstDetailId,
                                                                   BigDecimal delta,
                                                                   String revisionRemark) {
        List<HcGrindingProductionRecordDO> records = hcGrindingProductionLedgerMapper.selectListBySource(
                PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION, PASS_TYPE_FIRST, firstDetailId);
        for (HcGrindingProductionRecordDO record : records) {
            if (Boolean.TRUE.equals(record.getDeleted()) || record.getSandpaperLife() == null) {
                continue;
            }
            BigDecimal nextSandpaperLife = zero(record.getSandpaperLife()).add(delta);
            requireNonNegative(nextSandpaperLife, "后续一磨生产记录砂纸寿命");
            HcGrindingProductionRecordDO update = HcGrindingProductionRecordDO.builder()
                    .id(record.getId())
                    .sandpaperLife(nextSandpaperLife)
                    .remark(appendQuantityRevisionRemark(record.getRemark(), revisionRemark))
                    .build();
            hcGrindingProductionLedgerMapper.updateById(update);
        }
    }

    private HcEquipmentConsumableEventDO reviseConsumableUsageTotal(String bizType,
                                                                    Long bizId,
                                                                    String consumableType,
                                                                    BigDecimal newTotalLength,
                                                                    String revisionRemark) {
        if (StrUtil.isBlank(bizType) || bizId == null || StrUtil.isBlank(consumableType)) {
            return null;
        }
        List<HcEquipmentConsumableEventDO> usageEvents = hcEquipmentConsumableEventMapper.selectList(
                new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                        .eq(HcEquipmentConsumableEventDO::getBizType, bizType)
                        .eq(HcEquipmentConsumableEventDO::getBizId, bizId)
                        .eq(HcEquipmentConsumableEventDO::getConsumableType, consumableType)
                        .eq(HcEquipmentConsumableEventDO::getEventType, CONSUMABLE_EVENT_USE)
                        .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                        .orderByAsc(HcEquipmentConsumableEventDO::getEventTime)
                        .orderByAsc(HcEquipmentConsumableEventDO::getId));
        if (usageEvents.isEmpty()) {
            return null;
        }
        BigDecimal oldTotalLength = usageEvents.stream()
                .map(HcEquipmentConsumableEventDO::getChangeLength)
                .map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delta = zero(newTotalLength).subtract(oldTotalLength);
        if (isZeroQuantityDelta(delta)) {
            return usageEvents.get(usageEvents.size() - 1);
        }
        HcEquipmentConsumableEventDO targetEvent = usageEvents.get(usageEvents.size() - 1);
        BigDecimal nextChangeLength = zero(targetEvent.getChangeLength()).add(delta);
        BigDecimal nextAfterUsedLength = zero(targetEvent.getAfterUsedLength()).add(delta);
        requireNonNegative(nextChangeLength, "耗材使用事件修正后用量");
        requireNonNegative(nextAfterUsedLength, "耗材使用事件修正后累计量");
        HcEquipmentConsumableEventDO updateEvent = HcEquipmentConsumableEventDO.builder()
                .id(targetEvent.getId())
                .changeLength(nextChangeLength)
                .afterUsedLength(nextAfterUsedLength)
                .remark(appendQuantityRevisionRemark(targetEvent.getRemark(), revisionRemark))
                .build();
        hcEquipmentConsumableEventMapper.updateById(updateEvent);
        targetEvent.setChangeLength(nextChangeLength);
        targetEvent.setAfterUsedLength(nextAfterUsedLength);
        shiftLaterConsumableLength(targetEvent, delta, revisionRemark);
        return targetEvent;
    }

    private void shiftLaterConsumableLength(HcEquipmentConsumableEventDO anchorEvent,
                                            BigDecimal delta,
                                            String revisionRemark) {
        if (anchorEvent == null || anchorEvent.getStateId() == null
                || anchorEvent.getEventTime() == null || anchorEvent.getId() == null
                || isZeroQuantityDelta(delta)) {
            return;
        }
        boolean replacedAfterAnchor = false;
        List<HcEquipmentConsumableEventDO> laterEvents = hcEquipmentConsumableEventMapper.selectList(
                new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                        .eq(HcEquipmentConsumableEventDO::getStateId, anchorEvent.getStateId())
                        .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                        .and(wrapper -> wrapper
                                .gt(HcEquipmentConsumableEventDO::getEventTime, anchorEvent.getEventTime())
                                .or(nested -> nested
                                        .eq(HcEquipmentConsumableEventDO::getEventTime, anchorEvent.getEventTime())
                                        .gt(HcEquipmentConsumableEventDO::getId, anchorEvent.getId())))
                        .orderByAsc(HcEquipmentConsumableEventDO::getEventTime)
                        .orderByAsc(HcEquipmentConsumableEventDO::getId));
        for (HcEquipmentConsumableEventDO event : laterEvents) {
            BigDecimal nextBeforeUsed = event.getBeforeUsedLength() == null ? null : event.getBeforeUsedLength().add(delta);
            BigDecimal nextAfterUsed = event.getAfterUsedLength() == null ? null : event.getAfterUsedLength().add(delta);
            if (nextBeforeUsed != null) {
                requireNonNegative(nextBeforeUsed, "后续耗材事件修正后使用前累计量");
            }
            if (CONSUMABLE_EVENT_REPLACE.equalsIgnoreCase(event.getEventType())) {
                nextAfterUsed = event.getAfterUsedLength();
                replacedAfterAnchor = true;
            } else if (nextAfterUsed != null) {
                requireNonNegative(nextAfterUsed, "后续耗材事件修正后使用后累计量");
            }
            HcEquipmentConsumableEventDO update = HcEquipmentConsumableEventDO.builder()
                    .id(event.getId())
                    .beforeUsedLength(nextBeforeUsed)
                    .afterUsedLength(nextAfterUsed)
                    .remark(appendQuantityRevisionRemark(event.getRemark(), revisionRemark))
                    .build();
            hcEquipmentConsumableEventMapper.updateById(update);
            if (replacedAfterAnchor) {
                break;
            }
        }
        if (replacedAfterAnchor) {
            return;
        }
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectById(anchorEvent.getStateId());
        if (state == null || Boolean.TRUE.equals(state.getDeleted())) {
            return;
        }
        BigDecimal nextUsedLength = zero(state.getUsedLength()).add(delta);
        requireNonNegative(nextUsedLength, "耗材状态修正后累计米数");
        state.setUsedLength(nextUsedLength);
        HcEquipmentConsumableStateDO updateState = HcEquipmentConsumableStateDO.builder()
                .id(state.getId())
                .usedLength(nextUsedLength)
                .warningFlag(calcWarningFlag(state))
                .remark(appendQuantityRevisionRemark(state.getRemark(), revisionRemark))
                .build();
        hcEquipmentConsumableStateMapper.updateById(updateState);
    }

    private String buildQuantityRevisionRemark(String label, BigDecimal oldQty, BigDecimal newQty, String reason) {
        return label + "由" + toPlain(oldQty) + "m调整为" + toPlain(newQty) + "m；原因：" + StrUtil.trim(reason);
    }

    private String appendQuantityRevisionRemark(String currentRemark, String revisionRemark) {
        String remark = StrUtil.trim(currentRemark);
        String revision = StrUtil.trim(revisionRemark);
        if (StrUtil.isBlank(revision)) {
            return remark;
        }
        return StrUtil.isBlank(remark) ? "数据修正：" + revision : remark + "；数据修正：" + revision;
    }

    private boolean isZeroQuantityDelta(BigDecimal delta) {
        return delta == null || delta.abs().compareTo(new BigDecimal("0.0001")) <= 0;
    }

    private BigDecimal maxDecimal(BigDecimal left, BigDecimal right) {
        return zero(left).compareTo(zero(right)) >= 0 ? zero(left) : zero(right);
    }

    private void requireNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException(label + "不能为负数");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFirstAllocation(Long id) {
        if (id == null) {
            throw invalidParamException("一磨加工单元不能为空");
        }
        HcGrindingFirstAllocationDO allocation = hcGrindingFirstAllocationMapper.selectByIdForUpdate(id);
        if (allocation == null || Boolean.TRUE.equals(allocation.getDeleted())) {
            throw invalidParamException("一磨加工单元不存在");
        }
        HcGrindingSecondDetailDO secondDetail = hcGrindingSecondDetailMapper.selectByFirstAllocationId(id);
        if (secondDetail != null) {
            throw invalidParamException("该一磨加工单元已存在对应二磨报工，请先删除二磨报工");
        }
        List<HcGrindingFirstAllocationDO> allocations = hcGrindingFirstAllocationMapper.selectListByModeId(
                allocation.getAllocationModeId());
        boolean hasLaterAllocation = allocations.stream()
                .anyMatch(item -> !Objects.equals(item.getId(), id)
                        && item.getId() != null && item.getId() > id);
        if (hasLaterAllocation) {
            throw invalidParamException("只能按报工顺序倒序删除一磨加工单元");
        }
        HcGrindingFirstDetailDO firstDetail = hcGrindingFirstDetailMapper.selectById(allocation.getFirstDetailId());
        if (firstDetail == null || Boolean.TRUE.equals(firstDetail.getDeleted())) {
            throw invalidParamException("一磨加工单元对应的报工明细不存在");
        }
        HcPlanOrderDO plan = validatePlan(allocation.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, allocation.getPlanOperationId());
        hcGrindingProductionRecordLedgerService.deleteRecordBySource(PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION,
                PASS_TYPE_FIRST, firstDetail.getId());
        hcGrindingSourceBalanceMapper.selectByBatchNoForUpdate(allocation.getMotherBatchNo());
        rollbackFirstSourceBalance(firstDetail);
        rollbackReportConsumables(PASS_TYPE_FIRST, firstDetail.getId(), plan, operation);
        deleteProcessCheckRecord(allocation.getCheckRecordId(), BIZ_GRINDING_FIRST, firstDetail.getId());
        hcGrindingSegmentTimingMapper.clearFirstDetailId(firstDetail.getId());
        hcGrindingFirstAllocationMapper.physicalDeleteById(allocation.getId());
        hcGrindingFirstDetailMapper.physicalDeleteById(firstDetail.getId());
    }

    private String requireMotherBatchNo(String motherBatchNo) {
        String normalized = StrUtil.trim(motherBatchNo);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("母批号不能为空");
        }
        return normalized;
    }

    private String resolveAllocationMotherBatchNo(HcPlanOrderDO plan,
                                                  HcPlanOrderOperationDO operation,
                                                  String requestMotherBatchNo) {
        String requested = requireMotherBatchNo(requestMotherBatchNo);
        String authoritative = StrUtil.trim(resolveTimingMotherBatchNo(plan, operation));
        if (StrUtil.isNotBlank(authoritative) && !StrUtil.equals(authoritative, requested)) {
            throw invalidParamException("请求母批号与当前磨皮工序母批号不一致");
        }
        return firstNotBlank(authoritative, requested);
    }

    private String normalizeAllocationMode(String allocationMode) {
        String normalized = StrUtil.trimToEmpty(allocationMode).toUpperCase();
        if (!ALLOCATION_MODE_FIRST_ALLOCATED.equals(normalized)) {
            throw invalidParamException("磨皮一磨已统一使用 P/Q/R/S/不分段，不再支持 ORIGINAL 原有母批模式");
        }
        return normalized;
    }

    /**
     * 在首次一磨业务动作时建立内部模式记录，避免扫码/查看看板即写库。
     */
    private HcGrindingAllocationModeDO ensureFirstAllocatedMode(HcPlanOrderDO plan,
                                                                 HcPlanOrderOperationDO operation,
                                                                 String motherBatchNo) {
        HcGrindingAllocationModeDO existed = hcGrindingAllocationModeMapper.selectByMotherBatchForUpdate(
                operation.getId(), motherBatchNo);
        if (existed != null) {
            if (!ALLOCATION_MODE_FIRST_ALLOCATED.equals(existed.getAllocationMode())) {
                throw invalidParamException("当前母批存在历史原有母批报工，不能再追加P/Q/R/S/不分段报工");
            }
            if (!Objects.equals(existed.getPlanId(), plan.getId())) {
                throw invalidParamException("一磨分段记录与当前生产计划不一致");
            }
            return existed;
        }

        assertFirstAllocatedModeCanLock(operation, motherBatchNo);
        HcGrindingSourceBalanceDO balance = hcGrindingSourceBalanceMapper.selectByBatchNoForUpdate(motherBatchNo);
        if (balance == null) {
            balance = initializeFirstAllocationSourceBalance(plan, operation, motherBatchNo);
        }
        if (balance == null || zero(balance.getAvailableLength()).compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("当前母批没有可供一磨分段确认的来源余额");
        }

        Long operatorId = firstNonNull(SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        HcGrindingAllocationModeDO mode = HcGrindingAllocationModeDO.builder()
                .planId(plan.getId())
                .planNo(plan.getPlanNo())
                .planOperationId(operation.getId())
                .motherBatchNo(motherBatchNo)
                .allocationMode(ALLOCATION_MODE_FIRST_ALLOCATED)
                .availableLengthAtLock(balance.getAvailableLength())
                .lockedTime(LocalDateTime.now())
                .lockedOperatorId(operatorId)
                .lockedOperatorName(operatorName)
                .tenantId(plan.getTenantId())
                .build();
        try {
            hcGrindingAllocationModeMapper.insert(mode);
            return mode;
        } catch (DuplicateKeyException exception) {
            HcGrindingAllocationModeDO concurrent = hcGrindingAllocationModeMapper.selectByMotherBatchForUpdate(
                    operation.getId(), motherBatchNo);
            if (concurrent != null && ALLOCATION_MODE_FIRST_ALLOCATED.equals(concurrent.getAllocationMode())) {
                return concurrent;
            }
            throw invalidParamException("当前母批的一磨分段记录已由其他操作建立，请刷新后重试");
        }
    }

    private String normalizeFirstAllocationSegmentMark(String segmentMark) {
        String normalized = StrUtil.trimToEmpty(segmentMark).toUpperCase();
        if (!FIRST_ALLOCATION_SEGMENT_MARKS.contains(normalized)) {
            throw invalidParamException("一磨加工单元只能是 P、Q、R、S 或 NONE（不分段）");
        }
        return normalized;
    }

    private String firstAllocationDisplayName(String segmentMark) {
        return ALLOCATION_SEGMENT_NONE.equals(segmentMark) ? "不分段" : segmentMark + "段";
    }

    private String buildFirstAllocationBatchNo(String motherBatchNo, String segmentMark) {
        return ALLOCATION_SEGMENT_NONE.equals(segmentMark)
                ? motherBatchNo : StrUtil.trimToEmpty(motherBatchNo) + segmentMark;
    }

    private void assertFirstAllocatedModeCanLock(HcPlanOrderOperationDO operation, String motherBatchNo) {
        boolean hasFirstReport = hcGrindingFirstDetailMapper.selectListByPlanOperationId(operation.getId()).stream()
                .anyMatch(detail -> StrUtil.equals(motherBatchNo,
                        firstNotBlank(detail.getMotherBatchNo(), detail.getSourceProductionBatchNo())));
        boolean hasSecondReport = hcGrindingSecondDetailMapper.selectListByPlanOperationId(operation.getId()).stream()
                .anyMatch(detail -> StrUtil.equals(motherBatchNo,
                        firstNotBlank(detail.getMotherBatchNo(), detail.getParentProductionBatchNo(),
                                detail.getSourceProductionBatchNo())));
        if (hasFirstReport || hasSecondReport) {
            throw invalidParamException("当前母批已经产生原模式报工，不能再切换为一磨前置分配模式");
        }
    }

    /**
     * 兼容原磨皮逻辑：历史计划只按“前序合格产出 - 已磨皮数量”计算当前可加工数量，
     * 未必已经生成独立来源余额。首次锁定一磨前置分配模式时，以同一剩余量初始化余额台账。
     */
    private HcGrindingSourceBalanceDO initializeFirstAllocationSourceBalance(HcPlanOrderDO plan,
                                                                              HcPlanOrderOperationDO operation,
                                                                              String motherBatchNo) {
        HcRoughReportTaskRespVO task = buildTask(plan, operation, operation.getEquipmentId());
        BigDecimal availableLength = firstNonNull(task.getRemainingLength(), task.getPreviousGoodQty(), task.getMotherLength());
        if (zero(availableLength).compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        HcPlanOrderOperationDO previousOperation = hcPlanOrderOperationMapper.selectListByPlanId(plan.getId()).stream()
                .filter(item -> item.getOpSeq() != null && operation.getOpSeq() != null
                        && item.getOpSeq() < operation.getOpSeq())
                .max(Comparator.comparing(HcPlanOrderOperationDO::getOpSeq))
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        HcGrindingSourceBalanceDO initialized = HcGrindingSourceBalanceDO.builder()
                .balanceKey(SOURCE_BALANCE_TYPE_WET_OUTPUT + ":" + plan.getPlanNo() + ":" + motherBatchNo)
                .sourceType(SOURCE_BALANCE_TYPE_WET_OUTPUT)
                .sourcePlanId(plan.getId())
                .sourcePlanNo(plan.getPlanNo())
                .sourcePlanOperationId(previousOperation == null ? null : previousOperation.getId())
                .sourceBatchNo(motherBatchNo)
                .sourceProductionBatchNo(motherBatchNo)
                .materialCode(firstNotBlank(operation.getMotherMaterialCode(), plan.getMotherMaterialCode(),
                        plan.getMaterialCode()))
                .materialName(firstNotBlank(operation.getMotherMaterialName(), plan.getMotherMaterialName(),
                        plan.getMaterialName()))
                .modelCode(firstNotBlank(operation.getMotherModelCode(), plan.getMotherModelCode(), plan.getModelCode()))
                .modelName(firstNotBlank(operation.getMotherModelName(), plan.getMotherModelName(), plan.getModelName()))
                .totalLength(availableLength)
                .usedFirstLength(BigDecimal.ZERO)
                .usedSecondLength(BigDecimal.ZERO)
                .reservedLength(BigDecimal.ZERO)
                .availableLength(availableLength)
                .lastReportTime(firstNonNull(task.getPreviousEndTime(), now))
                .status("AVAILABLE")
                .remark("一磨前置分配按原当前可加工数量初始化")
                .tenantId(plan.getTenantId())
                .build();
        try {
            hcGrindingSourceBalanceMapper.insert(initialized);
            return initialized;
        } catch (DuplicateKeyException exception) {
            HcGrindingSourceBalanceDO concurrent = hcGrindingSourceBalanceMapper.selectByBatchNoForUpdate(motherBatchNo);
            if (concurrent != null) {
                return concurrent;
            }
            throw exception;
        }
    }

    private AllocationReportTime resolveFirstAllocationReportTime(HcPlanOrderOperationDO operation,
                                                                   String motherBatchNo,
                                                                   String segmentMark) {
        LocalDateTime now = LocalDateTime.now();
        HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(
                operation.getId(), motherBatchNo, PASS_TYPE_FIRST, segmentMark);
        // 一磨分段开工、完工为可选现场打点；未打点时以本次服务端保存时刻生成报工时间，不能再回退到计划或旧报工时间。
        LocalDateTime startTime = timing == null ? null : timing.getStartTime();
        LocalDateTime endTime = timing == null ? null : timing.getEndTime();
        if (startTime == null && endTime != null) {
            startTime = endTime;
        }
        if (startTime == null) {
            startTime = now;
        }
        if (endTime == null) {
            endTime = now;
        }
        if (endTime.isBefore(startTime)) {
            endTime = startTime;
        }
        return new AllocationReportTime(startTime, endTime);
    }

    private HcGrindingFirstDetailDO buildFirstAllocationDetail(HcRoughConsoleFirstAllocationSaveReqVO reqVO,
                                                                HcPlanOrderDO plan,
                                                                HcPlanOrderOperationDO operation,
                                                                HcEquipmentDO equipment,
                                                                HcGrindingSourceBalanceDO balance,
                                                                BigDecimal startPosition,
                                                                BigDecimal confirmedLength,
                                                                BigDecimal availableBefore,
                                                                BigDecimal availableAfter,
                                                                LocalDate reportDate,
                                                                LocalDateTime startTime,
                                                                LocalDateTime endTime,
                                                                HcEquipmentConsumableStateDO sandpaper,
                                                                HcEquipmentConsumableStateDO guideCloth) {
        Integer sandpaperLifeDays = sandpaper == null ? null
                : calculateSandpaperLifeDays(sandpaper.getLastReplaceTime(), endTime);
        return HcGrindingFirstDetailDO.builder()
                .planId(plan.getId())
                .planNo(plan.getPlanNo())
                .planOperationId(operation.getId())
                .rowUid("FIRST-ALLOC-" + UUID.randomUUID())
                .sourceType(firstNotBlank(balance.getSourceType(), "PREVIOUS"))
                .motherBatchNo(requireMotherBatchNo(reqVO.getMotherBatchNo()))
                .sourcePlanNo(balance.getSourcePlanNo())
                .sourcePlanId(balance.getSourcePlanId())
                .sourcePlanOperationId(balance.getSourcePlanOperationId())
                .sourceProductionBatchNo(firstNotBlank(balance.getSourceProductionBatchNo(), balance.getSourceBatchNo()))
                .remainStartMeter(startPosition)
                .remainLength(availableBefore)
                .processLength(confirmedLength)
                .lossLength(BigDecimal.ZERO)
                .outputLength(confirmedLength)
                .napSampleLength(BigDecimal.ZERO)
                .grindingPass("1")
                .startTime(startTime)
                .endTime(endTime)
                .reportDate(reportDate)
                .sandpaperLife(sandpaper == null ? null : sandpaper.getUsedLength())
                .sandpaperLifeDays(sandpaperLifeDays == null ? null : BigDecimal.valueOf(sandpaperLifeDays))
                .sandpaperBatchNo(firstNotBlank(reqVO.getSandpaperBatchNo(), sandpaper == null ? null : sandpaper.getBatchNo()))
                .currentSandpaperBatchNo(firstNotBlank(reqVO.getCurrentSandpaperBatchNo(), reqVO.getSandpaperBatchNo(),
                        sandpaper == null ? null : sandpaper.getBatchNo()))
                .currentGuideClothBatchNo(firstNotBlank(reqVO.getCurrentGuideClothBatchNo(), reqVO.getGuideClothBatchNo(),
                        guideCloth == null ? null : guideCloth.getBatchNo()))
                .selfCheck("OK")
                .rowStatus("已报工")
                .remark(reqVO.getRemark())
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .workCenterId(equipment.getWorkCenterId())
                .workCenterCode(equipment.getWorkCenterCode())
                .workCenterName(equipment.getWorkCenterName())
                .availableBefore(availableBefore)
                .availableAfter(availableAfter)
                .sandpaperStateId(sandpaper == null ? null : sandpaper.getId())
                .guideClothStateId(guideCloth == null ? null : guideCloth.getId())
                .detailStatus("SUBMITTED")
                .tenantId(plan.getTenantId())
                .build();
    }

    private void bindFirstAllocationSegmentTiming(HcGrindingFirstDetailDO firstDetail,
                                                   HcPlanOrderDO plan,
                                                   HcPlanOrderOperationDO operation,
                                                   String motherBatchNo,
                                                   String segmentMark) {
        if (ALLOCATION_SEGMENT_NONE.equals(segmentMark)) {
            return;
        }
        HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(operation.getId(),
                motherBatchNo, PASS_TYPE_FIRST, segmentMark);
        if (timing == null) {
            timing = buildSegmentTiming(plan, operation, motherBatchNo, PASS_TYPE_FIRST, segmentMark,
                    firstDetail.getId(), null);
            hcGrindingSegmentTimingMapper.insert(timing);
            return;
        }
        timing.setSegmentBatchNo(buildFirstAllocationBatchNo(motherBatchNo, segmentMark));
        timing.setFirstDetailId(firstDetail.getId());
        hcGrindingSegmentTimingMapper.updateById(timing);
    }

    private void inheritSecondReportFromFirstAllocation(HcRoughConsoleSecondReportSaveReqVO reqVO,
                                                        HcPlanOrderDO plan,
                                                        HcPlanOrderOperationDO operation,
                                                        HcGrindingAllocationModeDO mode) {
        if (reqVO.getFirstAllocationId() == null) {
            throw invalidParamException("一磨前置分配模式下，二磨必须选择对应的一磨加工单元");
        }
        HcGrindingFirstAllocationDO allocation = hcGrindingFirstAllocationMapper.selectByIdForUpdate(
                reqVO.getFirstAllocationId());
        if (allocation == null || Boolean.TRUE.equals(allocation.getDeleted())
                || !Objects.equals(allocation.getAllocationModeId(), mode.getId())
                || !Objects.equals(allocation.getPlanId(), plan.getId())
                || !Objects.equals(allocation.getPlanOperationId(), operation.getId())
                || !StrUtil.equals(allocation.getMotherBatchNo(), requireMotherBatchNo(reqVO.getMotherBatchNo()))) {
            throw invalidParamException("所选一磨加工单元不属于当前母批或当前计划");
        }
        HcGrindingFirstDetailDO firstDetail = hcGrindingFirstDetailMapper.selectById(allocation.getFirstDetailId());
        if (firstDetail == null || Boolean.TRUE.equals(firstDetail.getDeleted())) {
            throw invalidParamException("所选一磨加工单元对应的一磨报工不存在");
        }
        HcGrindingSecondDetailDO existing = hcGrindingSecondDetailMapper.selectByFirstAllocationId(allocation.getId());
        if (existing != null && !Objects.equals(existing.getId(), reqVO.getId())) {
            throw invalidParamException(firstAllocationDisplayName(allocation.getSegmentMark()) + "已完成二磨报工，不能重复保存");
        }
        if (reqVO.getId() != null) {
            HcGrindingSecondDetailDO current = hcGrindingSecondDetailMapper.selectById(reqVO.getId());
            if (current != null && (!Objects.equals(current.getFirstAllocationId(), allocation.getId())
                    || !Objects.equals(current.getPlanOperationId(), operation.getId()))) {
                throw invalidParamException("不能将已有二磨报工改绑到其他一磨加工单元");
            }
        }
        assertInheritedValue(reqVO.getFirstDetailId(), allocation.getFirstDetailId(), "一磨报工ID");
        assertInheritedText(reqVO.getSegmentMark(), allocation.getSegmentMark(), "段号");
        assertInheritedText(reqVO.getProductionBatchNo(), allocation.getProductionBatchNo(), "生产批号");
        assertInheritedDecimal(reqVO.getStartPosition(), allocation.getStartPosition(), "起米位置");
        assertInheritedDecimal(reqVO.getProcessLength(), allocation.getConfirmedLength(), "投入米数");
        BigDecimal allocatedLength = zero(allocation.getConfirmedLength());
        // 先按服务端统一公式重算，再校验一磨分配的米数守恒，不信任客户端产出。
        BigDecimal abnormalLength = validateAndSumSecondAbnormalLength(reqVO.getAbnormalPositions());
        reqVO.setOutputLength(calculateSecondOutputLength(allocatedLength, reqVO.getLossLength(),
                reqVO.getNapSampleLength(), reqVO.getResearchConsumptionLength(), abnormalLength));

        BigDecimal distributedLength = zero(reqVO.getLossLength())
                .add(zero(reqVO.getNapSampleLength()))
                .add(zero(reqVO.getResearchConsumptionLength()))
                .add(abnormalLength)
                .add(zero(reqVO.getOutputLength()));
        if (distributedLength.subtract(allocatedLength).abs().compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("二磨固定损耗、NAP留样、研发消耗、异常与产出米数合计必须等于一磨确认加工米数"
                    + allocatedLength.stripTrailingZeros().toPlainString() + "m");
        }
        reqVO.setFirstDetailId(allocation.getFirstDetailId());
        reqVO.setFirstAllocationId(allocation.getId());
        reqVO.setMotherBatchNo(allocation.getMotherBatchNo());
        reqVO.setSegmentMark(allocation.getSegmentMark());
        reqVO.setProductionBatchNo(allocation.getProductionBatchNo());
        reqVO.setParentProductionBatchNo(allocation.getMotherBatchNo());
        reqVO.setSourceProductionBatchNo(allocation.getProductionBatchNo());
        reqVO.setStartPosition(allocation.getStartPosition());
        reqVO.setProcessLength(allocation.getConfirmedLength());
    }

    private void assertInheritedValue(Long requestValue, Long inheritedValue, String fieldName) {
        if (requestValue != null && !Objects.equals(requestValue, inheritedValue)) {
            throw invalidParamException("二磨" + fieldName + "必须继承一磨加工单元");
        }
    }

    private void assertInheritedText(String requestValue, String inheritedValue, String fieldName) {
        if (StrUtil.isNotBlank(requestValue) && !StrUtil.equalsIgnoreCase(StrUtil.trim(requestValue), inheritedValue)) {
            throw invalidParamException("二磨" + fieldName + "必须继承一磨加工单元");
        }
    }

    private void assertInheritedDecimal(BigDecimal requestValue, BigDecimal inheritedValue, String fieldName) {
        if (requestValue != null && requestValue.subtract(zero(inheritedValue)).abs().compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("二磨" + fieldName + "必须继承一磨加工单元");
        }
    }

    private record AllocationReportTime(LocalDateTime startTime, LocalDateTime endTime) {
    }

    private void saveFirstAbnormalPositions(HcRoughConsoleFirstReportSaveReqVO reqVO,
                                            HcPlanOrderDO plan,
                                            HcPlanOrderOperationDO operation,
                                            HcGrindingFirstDetailDO detail) {
        hcWetReportAbnormalPositionMapper.delete(new LambdaQueryWrapperX<HcWetReportAbnormalPositionDO>()
                .eq(HcWetReportAbnormalPositionDO::getSourceMenuCode, SOURCE_MENU_CODE_ROUGH)
                .eq(HcWetReportAbnormalPositionDO::getProcessStage, "FIRST_GRINDING")
                .eq(HcWetReportAbnormalPositionDO::getSourceDetailId, detail.getId()));
        List<HcWetReportAbnormalPositionSaveReqVO> positions = reqVO.getAbnormalPositions();
        if (positions == null || positions.isEmpty()) {
            return;
        }
        List<HcWetReportAbnormalPositionDO> rows = new ArrayList<>();
        int sortIndex = 0;
        for (HcWetReportAbnormalPositionSaveReqVO position : positions) {
            String positionText = StrUtil.trim(position.getPositionText());
            BigDecimal abnormalLength = position.getAbnormalLength();
            String remark = StrUtil.trim(position.getRemark());
            if (StrUtil.isBlank(positionText) && abnormalLength == null && StrUtil.isBlank(remark)) {
                continue;
            }
            if (StrUtil.isBlank(positionText)) {
                throw invalidParamException("异常位置不能为空");
            }
            if (abnormalLength != null && abnormalLength.compareTo(BigDecimal.ZERO) < 0) {
                throw invalidParamException("异常位置米数不能为负数");
            }
            sortIndex++;
            rows.add(HcWetReportAbnormalPositionDO.builder()
                    .operationReportId(null)
                    .planId(plan.getId())
                    .planOperationId(operation.getId())
                    .planNo(plan.getPlanNo())
                    .sourceMenuCode(SOURCE_MENU_CODE_ROUGH)
                    .processStage("FIRST_GRINDING")
                    .operationCode(operation.getOpCode())
                    .operationName(operation.getOpName())
                    .batchNo(firstNotBlank(detail.getMotherBatchNo(), reqVO.getMotherBatchNo()))
                    .productionBatchNo(firstNotBlank(detail.getSourceProductionBatchNo(), reqVO.getSourceProductionBatchNo(),
                            operation.getProductionBatchNo(), plan.getProductionBatchNo()))
                    .sourcePlanNo(firstNotBlank(detail.getSourcePlanNo(), reqVO.getSourcePlanNo()))
                    .sourceRowUid(detail.getRowUid())
                    .sourceDetailId(detail.getId())
                    .positionText(positionText)
                    .abnormalLength(abnormalLength)
                    .remark(remark)
                    .sortOrder(position.getSortOrder() == null ? sortIndex : position.getSortOrder())
                    .tenantId(plan.getTenantId())
                    .build());
        }
        if (!rows.isEmpty()) {
            hcWetReportAbnormalPositionMapper.insertBatch(rows);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSecondReport(HcRoughConsoleSecondReportSaveReqVO reqVO) {
        var consumptionBooking = grindingConsumptionService.begin("SECOND", reqVO.getConsumption(), reqVO);
        if (consumptionBooking != null && consumptionBooking.getResultId() != null) return consumptionBooking.getResultId();
        HcPlanOrderDO plan = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, reqVO.getPlanOperationId());
        assertOperationRunning(operation);
        HcEquipmentDO equipment = validateEquipment(reqVO.getEquipmentId(), operation);
        String requestedMotherBatchNo = requireMotherBatchNo(reqVO.getMotherBatchNo());
        String motherBatchNo = firstNotBlank(resolveTimingMotherBatchNo(plan, operation), requestedMotherBatchNo);
        HcGrindingAllocationModeDO allocationMode = hcGrindingAllocationModeMapper.selectByMotherBatch(
                operation.getId(), motherBatchNo);
        if (allocationMode == null) {
            throw invalidParamException("请先完成对应P/Q/R/S/不分段的一磨报工");
        }
        if (!ALLOCATION_MODE_FIRST_ALLOCATED.equals(allocationMode.getAllocationMode())) {
            throw invalidParamException("当前母批存在历史原有母批报工，不能再追加分段二磨报工");
        }
        resolveAllocationMotherBatchNo(plan, operation, requestedMotherBatchNo);
        inheritSecondReportFromFirstAllocation(reqVO, plan, operation, allocationMode);
        requireProductionRecordStartOperator(plan, operation, PASS_TYPE_SECOND, reqVO.getSegmentMark());
        assertSecondReportWithinFirstGrindingRange(reqVO);
        assertSecondReportRangeNotOverlap(reqVO);
        ProcessCheckSnapshot processCheck = resolveConfirmedProcessCheckSnapshot(
                reqVO.getProcessFormRecordId(), plan, operation, PASS_TYPE_SECOND,
                firstNotBlank(reqVO.getSegmentMark(), ALLOCATION_SEGMENT_NONE));

        LocalDateTime reportStartTime = normalizeOptionalReportDateTime(reqVO.getStartTime(), null);
        LocalDateTime reportEndTime = normalizeOptionalReportDateTime(reqVO.getEndTime(), null);
        validateReportTimeRange(reportStartTime, reportEndTime, "第二次磨皮");
        BigDecimal consumeLength = zero(reqVO.getProcessLength()).subtract(zero(reqVO.getLossLength()));
        ConsumableReportResult sandpaperResult = increaseReportConsumable(equipment, plan, operation, CONSUMABLE_SANDPAPER,
                reqVO.getCurrentSandpaperBatchNo(), reqVO.getSandpaperBatchNo(), reqVO.getSandpaperReplaceReason(),
                reqVO.getSandpaperChanged(), consumeLength, "SECOND", null, reportEndTime, reqVO.getOperatorId(), reqVO.getOperatorName());
        ConsumableReportResult guideClothResult = increaseReportConsumable(equipment, plan, operation, CONSUMABLE_GUIDE_CLOTH,
                reqVO.getCurrentGuideClothBatchNo(), reqVO.getGuideClothBatchNo(), reqVO.getGuideClothReplaceReason(),
                reqVO.getGuideClothChanged(), consumeLength, "SECOND", null, reportEndTime, reqVO.getOperatorId(), reqVO.getOperatorName());
        HcEquipmentConsumableStateDO sandpaper = sandpaperResult.state();
        HcEquipmentConsumableStateDO guideCloth = guideClothResult.state();

        HcGrindingSecondDetailDO detail = reqVO.getId() == null ? null : hcGrindingSecondDetailMapper.selectById(reqVO.getId());
        boolean newDetail = detail == null;
        if (newDetail) {
            detail = HcGrindingSecondDetailDO.builder()
                    .tenantId(plan.getTenantId())
                    .rowUid("SECOND-" + UUID.randomUUID())
                    .build();
        }
        fillSecondDetail(detail, reqVO, plan, operation, equipment, sandpaper, guideCloth);
        if (newDetail) {
            hcGrindingSecondDetailMapper.insert(detail);
        } else {
            hcGrindingSecondDetailMapper.updateById(detail);
        }
        Long checkRecordId = upsertProcessCheckStationRecord(detail.getCheckRecordId(), BIZ_GRINDING_SECOND, detail.getId(), "SECOND",
                buildProcessCheckHeaderJson("SECOND", firstNotBlank(detail.getProductionBatchNo(), detail.getMotherBatchNo()),
                        detail.getProcessLength(), processCheck.headerDataJson(), processCheck.recordId()),
                processCheck.details(), processCheck.form(), processCheck.recordId(),
                plan, operation, equipment, reqVO.getOperatorName(), detail.getReportDate());
        if (checkRecordId != null && !Objects.equals(detail.getCheckRecordId(), checkRecordId)) {
            detail.setCheckRecordId(checkRecordId);
            hcGrindingSecondDetailMapper.updateById(detail);
        }
        bindSecondSegmentTiming(detail, plan, operation);
        saveMiddleProductRecord(null, BIZ_GRINDING_SECOND, detail.getId(), "SECOND",
                reqVO.getMiddleProductHeaderDataJson(), reqVO.getMiddleProductDetails(), plan, operation, equipment,
                reqVO.getOperatorName(), detail.getReportDate(), firstNotBlank(detail.getProductionBatchNo(), detail.getMotherBatchNo()),
                detail.getOutputLength());
        saveSecondAbnormalPositions(reqVO, plan, operation, detail);
        backfillConsumableEventBizId(sandpaper, detail.getId(), "SECOND");
        backfillConsumableEventBizId(guideCloth, detail.getId(), "SECOND");
        syncSecondProductionRecord(plan, operation, detail, sandpaper, sandpaperResult.sandpaperSegments(), guideCloth, reqVO);
        grindingConsumptionService.finish(consumptionBooking, detail.getId(), detail.getId(), reqVO.getConsumption(),
                Boolean.TRUE.equals(reqVO.getSandpaperChanged()), reqVO.getSandpaperBatchNo(),
                Boolean.TRUE.equals(reqVO.getGuideClothChanged()), reqVO.getGuideClothBatchNo(), reportEndTime,
                plan.getPlanNo(), detail.getProductionBatchNo());
        return detail.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long updateReportTime(HcRoughConsoleReportTimeUpdateReqVO reqVO) {
        String passType = StrUtil.blankToDefault(reqVO.getPassType(), "").trim().toUpperCase();
        log.info("[磨皮报工时间修改] request id={}, passType={}, reportDate={}, startTime={}, endTime={}",
                reqVO.getId(), passType, reqVO.getReportDate(), reqVO.getStartTime(), reqVO.getEndTime());
        LocalDate requestReportDate = parseReportDateText(reqVO.getReportDate());
        if ("FIRST".equals(passType) || "1".equals(passType)) {
            HcGrindingFirstDetailDO detail = hcGrindingFirstDetailMapper.selectById(reqVO.getId());
            if (detail == null) {
                throw invalidParamException("第一次磨皮报工记录不存在");
            }
            LocalDateTime startTime = normalizeOptionalReportDateTime(reqVO.getStartTime(), detail.getStartTime());
            LocalDateTime endTime = normalizeOptionalReportDateTime(reqVO.getEndTime(), detail.getEndTime());
            LocalDateTime effectiveStartTime = startTime == null ? detail.getStartTime() : startTime;
            LocalDateTime effectiveEndTime = endTime == null ? detail.getEndTime() : endTime;
            validateReportTimeRange(effectiveStartTime, effectiveEndTime, "第一次磨皮报工");
            LocalDate reportDate = effectiveReportDate(requestReportDate, effectiveStartTime);
            int updated = hcGrindingFirstDetailMapper.updateReportTime(detail.getId(), reportDate, effectiveStartTime, effectiveEndTime);
            log.info("[磨皮报工时间修改] first updated={}, id={}, beforeStart={}, beforeEnd={}, afterStart={}, afterEnd={}",
                    updated, detail.getId(), detail.getStartTime(), detail.getEndTime(), effectiveStartTime, effectiveEndTime);
            if (updated <= 0) {
                throw invalidParamException("第一次磨皮报工时间保存失败");
            }
            HcGrindingFirstAllocationDO allocation = hcGrindingFirstAllocationMapper.selectByFirstDetailId(detail.getId());
            if (allocation == null) {
                hcGrindingProductionRecordLedgerService.syncAutoRecordTime(PASS_TYPE_FIRST, detail.getId(), effectiveEndTime);
            } else {
                allocation.setStartTime(effectiveStartTime);
                allocation.setEndTime(effectiveEndTime);
                hcGrindingFirstAllocationMapper.updateById(allocation);
                if (!ALLOCATION_SEGMENT_NONE.equals(allocation.getSegmentMark())) {
                    HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(
                            allocation.getPlanOperationId(), allocation.getMotherBatchNo(), PASS_TYPE_FIRST,
                            allocation.getSegmentMark());
                    if (timing == null) {
                        throw invalidParamException("一磨分段开工完工记录不存在，不能修订报工时间");
                    }
                    timing.setStartTime(effectiveStartTime);
                    timing.setEndTime(effectiveEndTime);
                    hcGrindingSegmentTimingMapper.updateById(timing);
                }
                hcGrindingProductionRecordLedgerService.syncAutoRecordTime(PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION,
                        PASS_TYPE_FIRST, detail.getId(), effectiveEndTime);
            }
            return detail.getId();
        }
        if ("SECOND".equals(passType) || "2".equals(passType)) {
            HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(reqVO.getId());
            if (detail == null) {
                throw invalidParamException("第二次磨皮报工记录不存在");
            }
            LocalDateTime startTime = normalizeOptionalReportDateTime(reqVO.getStartTime(), detail.getStartTime());
            LocalDateTime endTime = normalizeOptionalReportDateTime(reqVO.getEndTime(), detail.getEndTime());
            LocalDateTime effectiveStartTime = startTime == null ? detail.getStartTime() : startTime;
            LocalDateTime effectiveEndTime = endTime == null ? detail.getEndTime() : endTime;
            validateReportTimeRange(effectiveStartTime, effectiveEndTime, "第二次磨皮报工");
            LocalDate reportDate = effectiveReportDate(requestReportDate, effectiveStartTime);
            int updated = hcGrindingSecondDetailMapper.updateReportTime(detail.getId(), reportDate, effectiveStartTime, effectiveEndTime);
            log.info("[磨皮报工时间修改] second updated={}, id={}, beforeStart={}, beforeEnd={}, afterStart={}, afterEnd={}",
                    updated, detail.getId(), detail.getStartTime(), detail.getEndTime(), effectiveStartTime, effectiveEndTime);
            if (updated <= 0) {
                throw invalidParamException("第二次磨皮报工时间保存失败");
            }
            syncSecondSegmentTimingReportTime(detail, effectiveStartTime, effectiveEndTime);
            hcGrindingProductionRecordLedgerService.syncAutoRecordTime(PASS_TYPE_SECOND, detail.getId(), effectiveEndTime);
            return detail.getId();
        }
        throw invalidParamException("磨皮次数不正确");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFirstReport(Long id) {
        if (id == null) {
            throw invalidParamException("第一次磨皮报工记录不能为空");
        }
        HcGrindingFirstDetailDO detail = hcGrindingFirstDetailMapper.selectById(id);
        if (detail == null || Boolean.TRUE.equals(detail.getDeleted())) {
            throw invalidParamException("第一次磨皮报工记录不存在");
        }
        if (hcGrindingFirstAllocationMapper.selectByFirstDetailId(detail.getId()) != null) {
            throw invalidParamException("该记录属于一磨前置分配加工单元，请使用加工单元删除入口");
        }
        List<HcGrindingSecondDetailDO> secondDetails = hcGrindingSecondDetailMapper.selectListByPlanOperationId(detail.getPlanOperationId());
        if (!secondDetails.isEmpty()) {
            throw invalidParamException("当前工单已存在第二次磨皮报工，请先删除全部第二次磨皮报工后，再删除第一次磨皮报工");
        }
        HcPlanOrderDO plan = validatePlan(detail.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, detail.getPlanOperationId());
        hcGrindingProductionRecordLedgerService.deleteRecordBySource("FIRST", detail.getId());
        rollbackFirstSourceBalance(detail);
        rollbackReportConsumables("FIRST", detail.getId(), plan, operation);
        deleteProcessCheckRecord(detail.getCheckRecordId(), BIZ_GRINDING_FIRST, detail.getId());
        hcWetReportAbnormalPositionMapper.physicalDeleteBySource(SOURCE_MENU_CODE_ROUGH, "FIRST_GRINDING", detail.getId());
        hcGrindingFirstDetailMapper.physicalDeleteById(detail.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSecondReport(Long id) {
        if (id == null) {
            throw invalidParamException("第二次磨皮报工记录不能为空");
        }
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(id);
        if (detail == null || Boolean.TRUE.equals(detail.getDeleted())) {
            throw invalidParamException("第二次磨皮报工记录不存在");
        }
        assertSecondReportCanDelete(detail);
        HcPlanOrderDO plan = validatePlan(detail.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, detail.getPlanOperationId());
        hcGrindingProductionRecordLedgerService.deleteRecordBySource("SECOND", detail.getId());
        rollbackReportConsumables("SECOND", detail.getId(), plan, operation);
        deleteProcessCheckRecord(detail.getCheckRecordId(), BIZ_GRINDING_SECOND, detail.getId());
        deleteMiddleProductRecord(BIZ_GRINDING_SECOND, detail.getId());
        hcWetReportAbnormalPositionMapper.physicalDeleteBySource(SOURCE_MENU_CODE_ROUGH, "SECOND_GRINDING", detail.getId());
        hcGrindingSecondDetailMapper.physicalDeleteById(detail.getId());
    }

    private void assertSecondReportCanDelete(HcGrindingSecondDetailDO detail) {
        if (DOC_STATUS_CONFIRMED.equals(detail.getConfirmStatus()) || "已确认".equals(detail.getConfirmStatus())
                || detail.getConfirmTime() != null) {
            throw invalidParamException("第二次磨皮已扫码确认，不能删除");
        }
        if (STOCK_POST_STATUS_POSTED.equals(detail.getStockPostStatus())
                || STOCK_POST_STATUS_INTERNAL_FLOW.equals(detail.getStockPostStatus())
                || detail.getStockId() != null) {
            throw invalidParamException("第二次磨皮已同步中间边库，不能删除");
        }
        if (detail.getInspectionId() != null) {
            throw invalidParamException("第二次磨皮已发起分段送检，不能删除");
        }
    }

    private void rollbackFirstSourceBalance(HcGrindingFirstDetailDO detail) {
        String batchNo = firstNotBlank(detail.getMotherBatchNo(), detail.getSourceProductionBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            return;
        }
        HcGrindingSourceBalanceDO balance = hcGrindingSourceBalanceMapper.selectByBatchNo(batchNo);
        if (balance == null) {
            return;
        }
        BigDecimal rollbackLength = zero(detail.getProcessLength());
        balance.setUsedFirstLength(nonNegative(zero(balance.getUsedFirstLength()).subtract(rollbackLength)));
        BigDecimal availableLength = zero(balance.getAvailableLength()).add(rollbackLength);
        if (balance.getTotalLength() != null && balance.getTotalLength().compareTo(BigDecimal.ZERO) > 0
                && availableLength.compareTo(balance.getTotalLength()) > 0) {
            availableLength = balance.getTotalLength();
        }
        balance.setAvailableLength(nonNegative(availableLength));
        balance.setStatus(balance.getAvailableLength().compareTo(BigDecimal.ZERO) > 0 ? "AVAILABLE" : "USED_UP");
        balance.setLastReportTime(LocalDateTime.now());
        hcGrindingSourceBalanceMapper.updateById(balance);
    }

    private void deleteProcessCheckRecord(Long recordId, String bizType, Long bizId) {
        HcStationRecordDO record = recordId == null ? null : hcStationRecordMapper.selectById(recordId);
        if ((record == null || Boolean.TRUE.equals(record.getDeleted())) && bizType != null && bizId != null) {
            record = hcStationRecordMapper.selectOneByBiz(RECORD_SCOPE_PROCESS_DETAIL, bizType, bizId, FORM_PROCESS_CHECK);
        }
        if (record == null) {
            return;
        }
        hcStationRecordItemMapper.physicalDeleteByRecordId(record.getId());
        hcStationRecordMapper.physicalDeleteById(record.getId());
    }

    private void deleteMiddleProductRecord(String bizType, Long bizId) {
        HcGrindingMiddleProductRecordDO record = hcGrindingMiddleProductRecordMapper.selectByBiz(bizType, bizId);
        if (record == null) {
            return;
        }
        hcGrindingMiddleProductDetailMapper.physicalDeleteByRecordId(record.getId());
        hcGrindingMiddleProductRecordMapper.physicalDeleteById(record.getId());
    }

    private void rollbackReportConsumables(String grindingStage, Long detailId, HcPlanOrderDO plan, HcPlanOrderOperationDO operation) {
        grindingConsumptionService.cancel(grindingStage, detailId);
        String bizType = "GRINDING_" + grindingStage;
        List<HcEquipmentConsumableEventDO> events = hcEquipmentConsumableEventMapper.selectListByBiz(bizType, detailId).stream()
                .filter(event -> event.getStateId() != null && List.of("USE", "REPLACE").contains(event.getEventType()))
                .toList();
        if (events.isEmpty()) {
            return;
        }
        Map<Long, List<HcEquipmentConsumableEventDO>> eventsByState = events.stream()
                .collect(Collectors.groupingBy(HcEquipmentConsumableEventDO::getStateId, LinkedHashMap::new, Collectors.toList()));
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        LocalDateTime rollbackTime = LocalDateTime.now();
        for (List<HcEquipmentConsumableEventDO> stateEvents : eventsByState.values()) {
            HcEquipmentConsumableEventDO firstEvent = stateEvents.get(0);
            HcEquipmentConsumableEventDO lastEvent = stateEvents.get(stateEvents.size() - 1);
            HcEquipmentConsumableEventDO laterEvent = hcEquipmentConsumableEventMapper.selectLaterUsageEvent(
                    lastEvent.getStateId(), lastEvent.getEventTime(), lastEvent.getId());
            if (laterEvent != null) {
                throw invalidParamException("该报工之后已发生耗材使用或更换记录，请先删除后续报工后再删除当前报工");
            }
            HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectById(lastEvent.getStateId());
            if (state == null || Boolean.TRUE.equals(state.getDeleted())) {
                continue;
            }
            String beforeBatchNo = state.getBatchNo();
            Integer beforeCount = state.getUseCount();
            BigDecimal beforeLength = state.getUsedLength();
            HcEquipmentConsumableEventDO previousReplace = hcEquipmentConsumableEventMapper.selectLatestBeforeByStateIdAndEventType(
                    state.getId(), "REPLACE", firstEvent.getEventTime(), firstEvent.getId());
            state.setBatchNo(firstNotBlank(firstEvent.getBeforeBatchNo(), "-"));
            state.setUseCount(firstEvent.getBeforeUseCount() == null ? 0 : firstEvent.getBeforeUseCount());
            state.setUsedLength(zero(firstEvent.getBeforeUsedLength()));
            state.setLastReplaceTime(previousReplace == null ? null : previousReplace.getEventTime());
            state.setLastReplacePlanNo(previousReplace == null ? null : previousReplace.getPlanNo());
            state.setLastReplaceReason(previousReplace == null ? null : previousReplace.getReplaceReason());
            state.setWarningFlag(calcWarningFlag(state));
            state.setStatus("IN_USE");
            state.setLastOperatorId(operatorId);
            state.setLastOperatorName(operatorName);
            state.setLastEventTime(rollbackTime);
            hcEquipmentConsumableStateMapper.updateById(state);
            insertConsumableEvent(state, plan.getId(), plan.getPlanNo(), operation.getId(), operation.getOpCode(), operation.getOpName(),
                    grindingStage, detailId, "DELETE_ROLLBACK", beforeBatchNo, state.getBatchNo(), beforeCount,
                    state.getUseCount(), beforeLength, state.getUsedLength(),
                    zero(state.getUsedLength()).subtract(zero(beforeLength)), "删除磨皮报工回滚耗材使用",
                    operatorId, operatorName, rollbackTime, plan.getTenantId());
        }
        hcEquipmentConsumableEventMapper.deleteUsageByBiz(bizType, detailId);
    }

    private BigDecimal validateAndSumSecondAbnormalLength(List<HcWetReportAbnormalPositionSaveReqVO> positions) {
        if (positions == null || positions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalLength = BigDecimal.ZERO;
        for (HcWetReportAbnormalPositionSaveReqVO position : positions) {
            String positionText = StrUtil.trim(position.getPositionText());
            BigDecimal abnormalLength = position.getAbnormalLength();
            String remark = StrUtil.trim(position.getRemark());
            if (StrUtil.isBlank(positionText) && abnormalLength == null && StrUtil.isBlank(remark)) {
                continue;
            }
            if (StrUtil.isBlank(positionText)) {
                throw invalidParamException("异常位置不能为空");
            }
            if (abnormalLength == null || abnormalLength.compareTo(BigDecimal.ZERO) <= 0) {
                throw invalidParamException("异常位置米数必须大于0");
            }
            totalLength = totalLength.add(abnormalLength);
        }
        return totalLength;
    }

    private BigDecimal sumPersistedSecondAbnormalLength(Long detailId) {
        if (detailId == null) {
            return BigDecimal.ZERO;
        }
        return hcWetReportAbnormalPositionMapper
                .selectListBySource(SOURCE_MENU_CODE_ROUGH, "SECOND_GRINDING", detailId)
                .stream()
                .map(HcWetReportAbnormalPositionDO::getAbnormalLength)
                .map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSecondOutputLength(BigDecimal processLength, BigDecimal lossLength,
                                                   BigDecimal napSampleLength, BigDecimal researchConsumptionLength,
                                                   BigDecimal abnormalLength) {
        requireNonNegative(zero(researchConsumptionLength), "研发消耗米数");
        BigDecimal outputLength = zero(processLength)
                .subtract(zero(lossLength))
                .subtract(zero(napSampleLength))
                .subtract(zero(researchConsumptionLength))
                .subtract(zero(abnormalLength));
        if (outputLength.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException("固定损耗、NAP留样、研发消耗和异常米数合计不能超过投入米数");
        }
        return outputLength;
    }

    private void saveSecondAbnormalPositions(HcRoughConsoleSecondReportSaveReqVO reqVO,
                                             HcPlanOrderDO plan,
                                             HcPlanOrderOperationDO operation,
                                             HcGrindingSecondDetailDO detail) {
        hcWetReportAbnormalPositionMapper.delete(new LambdaQueryWrapperX<HcWetReportAbnormalPositionDO>()
                .eq(HcWetReportAbnormalPositionDO::getSourceMenuCode, SOURCE_MENU_CODE_ROUGH)
                .eq(HcWetReportAbnormalPositionDO::getProcessStage, "SECOND_GRINDING")
                .eq(HcWetReportAbnormalPositionDO::getSourceDetailId, detail.getId()));
        List<HcWetReportAbnormalPositionSaveReqVO> positions = reqVO.getAbnormalPositions();
        if (positions == null || positions.isEmpty()) {
            return;
        }
        List<HcWetReportAbnormalPositionDO> rows = new ArrayList<>();
        int sortIndex = 0;
        for (HcWetReportAbnormalPositionSaveReqVO position : positions) {
            String positionText = StrUtil.trim(position.getPositionText());
            BigDecimal abnormalLength = position.getAbnormalLength();
            String remark = StrUtil.trim(position.getRemark());
            if (StrUtil.isBlank(positionText) && abnormalLength == null && StrUtil.isBlank(remark)) {
                continue;
            }
            if (StrUtil.isBlank(positionText)) {
                throw invalidParamException("异常位置不能为空");
            }
            if (abnormalLength == null || abnormalLength.compareTo(BigDecimal.ZERO) <= 0) {
                throw invalidParamException("异常位置米数必须大于0");
            }
            sortIndex++;
            rows.add(HcWetReportAbnormalPositionDO.builder()
                    .operationReportId(null)
                    .planId(plan.getId())
                    .planOperationId(operation.getId())
                    .planNo(plan.getPlanNo())
                    .sourceMenuCode(SOURCE_MENU_CODE_ROUGH)
                    .processStage("SECOND_GRINDING")
                    .operationCode(operation.getOpCode())
                    .operationName(operation.getOpName())
                    .batchNo(firstNotBlank(detail.getProductionBatchNo(), reqVO.getProductionBatchNo(),
                            detail.getMotherBatchNo(), reqVO.getMotherBatchNo()))
                    .productionBatchNo(firstNotBlank(detail.getProductionBatchNo(), reqVO.getProductionBatchNo(),
                            operation.getProductionBatchNo(), plan.getProductionBatchNo()))
                    .sourcePlanNo(firstNotBlank(reqVO.getPlanNo(), plan.getPlanNo()))
                    .sourceRowUid(detail.getRowUid())
                    .sourceDetailId(detail.getId())
                    .positionText(positionText)
                    .abnormalLength(abnormalLength)
                    .remark(remark)
                    .sortOrder(position.getSortOrder() == null ? sortIndex : position.getSortOrder())
                    .tenantId(plan.getTenantId())
                    .build());
        }
        if (!rows.isEmpty()) {
            hcWetReportAbnormalPositionMapper.insertBatch(rows);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmSecondReport(HcRoughConsoleSecondReportConfirmReqVO reqVO) {
        HcGrindingSecondDetailDO anchorDetail = hcGrindingSecondDetailMapper.selectById(reqVO.getId());
        if (anchorDetail == null || Boolean.TRUE.equals(anchorDetail.getDeleted())) {
            throw invalidParamException("第二次磨皮记录不存在");
        }
        String scannedBatchNo = StrUtil.trim(reqVO.getScannedBatchNo());
        HcGrindingSecondDetailDO targetDetail = resolveSecondReportScanTargetForUpdate(anchorDetail, scannedBatchNo);
        if (targetDetail == null) {
            throw invalidParamException("未在当前母批号的二次磨皮分段中找到扫码批次");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String confirmerName = firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        boolean forcePostWip = Boolean.TRUE.equals(reqVO.getForcePostWip());
        if ("CONFIRMED".equals(targetDetail.getConfirmStatus())) {
            if (forcePostWip && (!STOCK_POST_STATUS_POSTED.equals(targetDetail.getStockPostStatus()) || targetDetail.getStockId() == null)) {
                syncSecondGrindingWipStockAfterConfirm(targetDetail, LocalDateTime.now(), loginUserId, confirmerName,
                        "二磨已确认人工入中间边库", true);
                return targetDetail.getId();
            }
            if (!STOCK_POST_STATUS_POSTED.equals(targetDetail.getStockPostStatus()) && !STOCK_POST_STATUS_INTERNAL_FLOW.equals(targetDetail.getStockPostStatus())) {
                syncSecondGrindingWipStockAfterConfirm(targetDetail, LocalDateTime.now(), loginUserId, confirmerName,
                        "二磨已确认补同步库存状态", false);
                return targetDetail.getId();
            }
            throw invalidParamException("该二次磨皮分段已扫码确认");
        }
        LocalDateTime confirmTime = normalizeRequiredReportDateTime(reqVO.getConfirmerTime(), LocalDateTime.now());
        HcGrindingSecondDetailDO update = new HcGrindingSecondDetailDO();
        update.setId(targetDetail.getId());
        update.setConfirmStatus("CONFIRMED");
        update.setConfirmTime(confirmTime);
        update.setConfirmedBatchNo(scannedBatchNo);
        update.setConfirmOperatorId(loginUserId);
        update.setConfirmOperatorName(confirmerName);
        update.setDownstreamStatus("AVAILABLE");
        update.setDetailStatus("CONFIRMED");
        hcGrindingSecondDetailMapper.updateById(update);
        targetDetail.setConfirmStatus(update.getConfirmStatus());
        targetDetail.setConfirmTime(update.getConfirmTime());
        targetDetail.setConfirmedBatchNo(update.getConfirmedBatchNo());
        targetDetail.setConfirmOperatorId(update.getConfirmOperatorId());
        targetDetail.setConfirmOperatorName(update.getConfirmOperatorName());
        targetDetail.setDownstreamStatus(update.getDownstreamStatus());
        targetDetail.setDetailStatus(update.getDetailStatus());
        syncSecondGrindingWipStockAfterConfirm(targetDetail, confirmTime, loginUserId, confirmerName,
                forcePostWip ? "二磨确认人工入中间边库" : "二磨确认自动入库", forcePostWip);
        return targetDetail.getId();
    }

    private void syncSecondGrindingWipStockAfterConfirm(HcGrindingSecondDetailDO detail,
                                                       LocalDateTime postTime,
                                                       Long operatorId,
                                                       String operatorName,
                                                       String postMessage,
        boolean forcePostWip) {
        HcPlanOrderDO plan = validatePlan(detail.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, detail.getPlanOperationId());
        consumeWetWipLocksForSecondReport(plan, operation, detail, postTime, operatorId, operatorName);
        postSecondGrindingWipStock(detail, plan, operation, postTime, operatorId, operatorName, postMessage);
    }

    private void consumeWetWipLocksForSecondReport(HcPlanOrderDO plan,
                                                   HcPlanOrderOperationDO operation,
                                                   HcGrindingSecondDetailDO secondDetail,
                                                   LocalDateTime consumeTime,
                                                   Long operatorId,
                                                   String operatorName) {
        if (operation == null || operation.getId() == null || secondDetail == null) {
            return;
        }
        for (HcGrindingFirstDetailDO firstDetail : resolveFirstDetailsForSecondReport(operation.getId(), secondDetail)) {
            consumeWetWipLockForSecondReport(plan, operation, firstDetail, secondDetail, consumeTime, operatorId, operatorName);
        }
    }

    private List<HcGrindingFirstDetailDO> resolveFirstDetailsForSecondReport(Long planOperationId,
                                                                             HcGrindingSecondDetailDO secondDetail) {
        if (planOperationId == null || secondDetail == null) {
            return List.of();
        }
        if (secondDetail.getFirstDetailId() != null) {
            HcGrindingFirstDetailDO firstDetail = hcGrindingFirstDetailMapper.selectById(secondDetail.getFirstDetailId());
            if (firstDetail != null && !Boolean.TRUE.equals(firstDetail.getDeleted())) {
                return List.of(firstDetail);
            }
        }
        List<HcGrindingFirstDetailDO> firstDetails = hcGrindingFirstDetailMapper.selectListByPlanOperationId(planOperationId);
        String sourceRowUid = StrUtil.trim(secondDetail.getSourceRowUid());
        if (StrUtil.isNotBlank(sourceRowUid)) {
            List<HcGrindingFirstDetailDO> matchedByRowUid = firstDetails.stream()
                    .filter(firstDetail -> StrUtil.equals(sourceRowUid, StrUtil.trim(firstDetail.getRowUid())))
                    .toList();
            if (!matchedByRowUid.isEmpty()) {
                return matchedByRowUid;
            }
        }
        String motherBatchNo = firstNotBlank(secondDetail.getMotherBatchNo(), secondDetail.getParentProductionBatchNo());
        if (StrUtil.isBlank(motherBatchNo)) {
            return List.of();
        }
        return firstDetails.stream()
                .filter(firstDetail -> isSameBatch(motherBatchNo,
                        firstNotBlank(firstDetail.getMotherBatchNo(), firstDetail.getSourceProductionBatchNo())))
                .toList();
    }

    private void consumeWetWipLockForSecondReport(HcPlanOrderDO plan,
                                                  HcPlanOrderOperationDO operation,
                                                  HcGrindingFirstDetailDO firstDetail,
                                                  HcGrindingSecondDetailDO secondDetail,
                                                  LocalDateTime consumeTime,
                                                  Long operatorId,
                                                  String operatorName) {
        if (plan == null || operation == null || firstDetail == null || firstDetail.getId() == null
                || secondDetail == null || secondDetail.getId() == null) {
            return;
        }
        BigDecimal consumeQty = firstPositiveDecimal(secondDetail.getProcessLength(), secondDetail.getOutputLength());
        if (consumeQty.compareTo(BigDecimal.ZERO) <= 0
                || hasRoughWetWipConsumeTxn(firstDetail.getId(), secondDetail.getId())) {
            return;
        }
        List<HcPlanOrderInventoryLockDO> activeLocks = selectActiveWetWipLocks(operation.getId(),
                firstNotBlank(firstDetail.getMotherBatchNo(), firstDetail.getSourceProductionBatchNo(),
                        secondDetail.getMotherBatchNo(), secondDetail.getParentProductionBatchNo()));
        if (activeLocks.isEmpty()) {
            return;
        }
        BigDecimal totalRemainingQty = activeLocks.stream()
                .map(this::calculateWipLockRemainingQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRemainingQty.compareTo(consumeQty) < 0) {
            throw invalidParamException("湿法中间品锁定余量不足，剩余 " + totalRemainingQty
                    + "，本次二磨确认需消耗 " + consumeQty);
        }
        BigDecimal leftConsumeQty = consumeQty;
        LocalDateTime effectiveConsumeTime = consumeTime == null ? LocalDateTime.now() : consumeTime;
        for (HcPlanOrderInventoryLockDO lock : activeLocks) {
            if (leftConsumeQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal lockRemainingQty = calculateWipLockRemainingQty(lock);
            if (lockRemainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal currentConsumeQty = lockRemainingQty.min(leftConsumeQty);
            hcInvStockService.consumePlanLockedWip(
                    lock,
                    currentConsumeQty,
                    REF_DOC_TYPE_ROUGH_SECOND,
                    secondDetail.getId(),
                    firstNotBlank(secondDetail.getConfirmedBatchNo(), secondDetail.getProductionBatchNo(),
                            firstDetail.getMotherBatchNo(), firstDetail.getSourceProductionBatchNo(), plan.getPlanNo()),
                    effectiveConsumeTime,
                    firstNonNull(operatorId, SecurityFrameworkUtils.getLoginUserId()),
                    firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"),
                    "二磨扫码确认消耗湿法中间品；二磨批号："
                            + firstNotBlank(secondDetail.getConfirmedBatchNo(), secondDetail.getProductionBatchNo(), "-")
                            + "；一磨记录：" + firstNotBlank(firstDetail.getRowUid(), String.valueOf(firstDetail.getId()))
                            + "；来源批号：" + firstNotBlank(lock.getSourceBatchNo(), lock.getBatchNo(), lock.getLotNo(), "-")
                            + "；状态：已扫码确认");
            leftConsumeQty = leftConsumeQty.subtract(currentConsumeQty);
        }
    }

    private boolean hasRoughWetWipConsumeTxn(Long firstDetailId, Long secondDetailId) {
        if (secondDetailId != null && hcInvTxnLogMapper.selectOne(new LambdaQueryWrapperX<HcInvTxnLogDO>()
                .eq(HcInvTxnLogDO::getRefDocType, REF_DOC_TYPE_ROUGH_SECOND)
                .eq(HcInvTxnLogDO::getRefDocId, secondDetailId)
                .eq(HcInvTxnLogDO::getDeleted, false)
                .last("LIMIT 1")) != null) {
            return true;
        }
        return firstDetailId != null && hcInvTxnLogMapper.selectOne(new LambdaQueryWrapperX<HcInvTxnLogDO>()
                .eq(HcInvTxnLogDO::getRefDocType, REF_DOC_TYPE_ROUGH_FIRST)
                .eq(HcInvTxnLogDO::getRefDocId, firstDetailId)
                .eq(HcInvTxnLogDO::getDeleted, false)
                .last("LIMIT 1")) != null;
    }

    private BigDecimal firstPositiveDecimal(BigDecimal... values) {
        if (values == null) {
            return BigDecimal.ZERO;
        }
        for (BigDecimal value : values) {
            BigDecimal normalized = zero(value);
            if (normalized.compareTo(BigDecimal.ZERO) > 0) {
                return normalized;
            }
        }
        return BigDecimal.ZERO;
    }

    private List<HcPlanOrderInventoryLockDO> selectActiveWetWipLocks(Long planOperationId, String batchNo) {
        if (planOperationId == null) {
            return List.of();
        }
        return hcPlanOrderInventoryLockMapper.selectListByPlanOperationId(planOperationId).stream()
                .filter(this::isActivePlanWipLock)
                .filter(lock -> SOURCE_TYPE_WET.equalsIgnoreCase(lock.getSourceType()))
                .filter(lock -> StrUtil.isBlank(batchNo) || isSameBatch(batchNo,
                        firstNotBlank(lock.getSourceBatchNo(), lock.getBatchNo(), lock.getLotNo())))
                .toList();
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
        String status = firstNotBlank(lock.getLockStatus(), "ACTIVE");
        if (PLAN_LOCK_STATUS_CONSUMED.equalsIgnoreCase(status)
                || PLAN_LOCK_STATUS_CANCELLED.equalsIgnoreCase(status)
                || PLAN_LOCK_STATUS_RELEASED.equalsIgnoreCase(status)) {
            return false;
        }
        return calculateWipLockRemainingQty(lock).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateWipLockRemainingQty(HcPlanOrderInventoryLockDO lock) {
        if (lock == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal remainingQty = zero(lock.getLockQty())
                .subtract(zero(lock.getConsumedQty()))
                .subtract(zero(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingQty;
    }

    private boolean isSameBatch(String left, String right) {
        return StrUtil.isNotBlank(left) && StrUtil.isNotBlank(right)
                && StrUtil.equalsIgnoreCase(normalizeBatchNo(left), normalizeBatchNo(right));
    }

    private void postSecondGrindingWipStock(HcGrindingSecondDetailDO detail,
                                            HcPlanOrderDO plan,
                                            HcPlanOrderOperationDO operation,
                                            LocalDateTime postTime,
                                            Long operatorId,
                                            String operatorName,
                                            String postMessage) {
        HcInvStockDO stock = hcInvStockService.postGrindingSecondWip(detail, plan, operation, postTime, operatorId, operatorName);
        HcGrindingSecondDetailDO stockPostUpdate = new HcGrindingSecondDetailDO();
        stockPostUpdate.setId(detail.getId());
        stockPostUpdate.setStockId(stock.getId());
        stockPostUpdate.setStockPostStatus(STOCK_POST_STATUS_POSTED);
        stockPostUpdate.setStockPostTime(postTime == null ? LocalDateTime.now() : postTime);
        stockPostUpdate.setStockPostOperatorId(operatorId);
        stockPostUpdate.setStockPostOperatorName(firstNotBlank(operatorName, "系统"));
        stockPostUpdate.setStockPostMessage(postMessage);
        hcGrindingSecondDetailMapper.updateById(stockPostUpdate);
    }

    private void markSecondGrindingInternalFlow(HcGrindingSecondDetailDO detail,
                                                LocalDateTime operateTime,
                                                Long operatorId,
                                                String operatorName,
                                                String message) {
        HcGrindingSecondDetailDO stockPostUpdate = new HcGrindingSecondDetailDO();
        stockPostUpdate.setId(detail.getId());
        stockPostUpdate.setStockPostStatus(STOCK_POST_STATUS_INTERNAL_FLOW);
        stockPostUpdate.setStockPostTime(operateTime == null ? LocalDateTime.now() : operateTime);
        stockPostUpdate.setStockPostOperatorId(operatorId);
        stockPostUpdate.setStockPostOperatorName(firstNotBlank(operatorName, "系统"));
        stockPostUpdate.setStockPostMessage(message);
        hcGrindingSecondDetailMapper.updateById(stockPostUpdate);
    }

    private boolean hasNextPlanOperation(Long planId, HcPlanOrderOperationDO currentOperation) {
        if (planId == null || currentOperation == null || currentOperation.getId() == null) {
            return false;
        }
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(planId);
        boolean currentMatched = false;
        for (HcPlanOrderOperationDO operation : operations) {
            if (operation == null || OP_STATUS_CANCELLED.equalsIgnoreCase(firstNotBlank(operation.getOperationStatus(), ""))) {
                continue;
            }
            if (Objects.equals(operation.getId(), currentOperation.getId())) {
                currentMatched = true;
                continue;
            }
            if (currentMatched) {
                return true;
            }
        }
        return false;
    }

    private HcGrindingSecondDetailDO resolveSecondReportScanTargetForUpdate(HcGrindingSecondDetailDO anchorDetail, String scannedBatchNo) {
        if (StrUtil.isBlank(scannedBatchNo)) {
            throw invalidParamException("扫码批次号不能为空");
        }
        String motherBatchNo = StrUtil.trim(firstNotBlank(anchorDetail.getMotherBatchNo(), anchorDetail.getParentProductionBatchNo()));
        List<HcGrindingSecondDetailDO> candidates = hcGrindingSecondDetailMapper.selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                        .eq(HcGrindingSecondDetailDO::getPlanOperationId, anchorDetail.getPlanOperationId())
                        .eq(HcGrindingSecondDetailDO::getDeleted, false)
                        .orderByAsc(HcGrindingSecondDetailDO::getId)
                        .last("FOR UPDATE"))
                .stream()
                .filter(detail -> StrUtil.isBlank(motherBatchNo)
                        || StrUtil.equals(motherBatchNo, StrUtil.trim(firstNotBlank(detail.getMotherBatchNo(), detail.getParentProductionBatchNo()))))
                .filter(detail -> secondReportBatchMatches(detail, scannedBatchNo))
                .toList();
        HcGrindingSecondDetailDO target = candidates.stream()
                .filter(detail -> Objects.equals(detail.getId(), anchorDetail.getId()))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(detail -> !"CONFIRMED".equals(detail.getConfirmStatus()))
                        .findFirst()
                        .orElseGet(() -> candidates.stream().findFirst().orElse(null)));
        if (target != null && !"CONFIRMED".equals(target.getConfirmStatus())) {
            assertNoOtherConfirmedSecondReport(candidates, target.getId(), scannedBatchNo);
        }
        return target;
    }

    private boolean secondReportBatchMatches(HcGrindingSecondDetailDO detail, String scannedBatchNo) {
        String scanned = StrUtil.trim(scannedBatchNo);
        return StrUtil.equals(scanned, StrUtil.trim(detail.getProductionBatchNo()))
                || StrUtil.equals(scanned, StrUtil.trim(detail.getConfirmedBatchNo()));
    }

    private void assertNoOtherConfirmedSecondReport(List<HcGrindingSecondDetailDO> candidates,
                                                    Long targetDetailId,
                                                    String scannedBatchNo) {
        HcGrindingSecondDetailDO confirmed = candidates.stream()
                .filter(detail -> !Objects.equals(detail.getId(), targetDetailId))
                .filter(detail -> "CONFIRMED".equals(detail.getConfirmStatus()))
                .findFirst()
                .orElse(null);
        if (confirmed != null) {
            throw invalidParamException("批次 " + StrUtil.trim(scannedBatchNo)
                    + " 已由二次磨皮记录 " + confirmed.getId() + " 扫码确认，不能重复确认");
        }
    }

    @Override
    public Long markSecondReportPrinted(HcRoughConsoleSecondReportPrintReqVO reqVO) {
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(reqVO.getId());
        if (detail == null || Boolean.TRUE.equals(detail.getDeleted())) {
            throw invalidParamException("第二次磨皮记录不存在");
        }
        HcGrindingSecondDetailDO update = new HcGrindingSecondDetailDO();
        update.setId(detail.getId());
        update.setPrintStatus(firstNotBlank(reqVO.getPrintStatus(), "PRINTED"));
        update.setPrintCount(reqVO.getPrintCount() == null ? (detail.getPrintCount() == null ? 0 : detail.getPrintCount()) + 1 : reqVO.getPrintCount());
        update.setLastPrintTime(reqVO.getLastPrintTime() == null ? LocalDateTime.now() : reqVO.getLastPrintTime());
        hcGrindingSecondDetailMapper.updateById(update);
        return detail.getId();
    }

    @Override
    public HcRoughConsoleMiddleProductRecordRespVO getMiddleProductRecord(Long recordId) {
        return toMiddleProductRecordResp(validateBusinessMiddleProductRecord(recordId));
    }

    @Override
    public PageResult<HcStationRecordRespVO> getMiddleProductRecordPage(HcStationRecordPageReqVO reqVO) {
        PageResult<HcGrindingMiddleProductRecordDO> page = hcGrindingMiddleProductRecordMapper.selectPage(reqVO);
        return new PageResult<>(
                page.getList().stream().map(this::toMiddleProductRecordListResp).toList(),
                page.getTotal());
    }

    @Override
    public HcRoughConsoleMiddleProductRecordRespVO getMiddleProductRecordByStationRecord(Long stationRecordId) {
        return toMiddleProductRecordResp(validateBusinessMiddleProductRecordByStationRecord(stationRecordId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcRoughConsoleMiddleProductRecordRespVO getOrInitSegmentMiddleProductRecord(HcRoughConsoleMiddleProductSegmentReqVO reqVO) {
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(reqVO.getPlanOperationId());
        if (operation == null || Boolean.TRUE.equals(operation.getDeleted())) {
            throw invalidParamException("计划工序不存在");
        }
        HcPlanOrderDO plan = hcPlanOrderMapper.selectById(operation.getPlanId());
        if (plan == null || Boolean.TRUE.equals(plan.getDeleted())) {
            throw invalidParamException("生产计划不存在");
        }
        String passType = StrUtil.blankToDefault(reqVO.getPassType(), "SECOND");
        String segmentMark = normalizeSegmentMark(reqVO.getSegmentMark());
        HcGrindingMiddleProductRecordDO record = hcGrindingMiddleProductRecordMapper.selectBySegment(operation.getId(), passType, segmentMark);
        BigDecimal segmentTotalLength = calcSecondSegmentLength(operation.getId(), segmentMark);
        int generatedLength = calcMiddleProductGeneratedLength(segmentTotalLength);
        if (record == null) {
            String productionBatchNo = resolveSegmentProductionBatchNo(plan, operation, segmentMark);
            record = hcGrindingMiddleProductRecordMapper.selectByProductionBatchNo(operation.getId(), passType, productionBatchNo);
            if (record != null) {
                record.setSegmentMark(segmentMark);
                record.setSegmentName(segmentName(segmentMark));
                record.setPassName("SECOND".equals(passType) ? "二次磨皮-" + segmentName(segmentMark) : "一次磨皮");
                hcGrindingMiddleProductRecordMapper.updateById(record);
            }
        }
        if (record == null) {
            record = buildSegmentMiddleProductRecord(plan, operation, passType, segmentMark, segmentTotalLength, generatedLength);
            hcGrindingMiddleProductRecordMapper.insert(record);
            initMiddleProductDetails(record);
        } else {
            boolean needUpdate = false;
            List<HcGrindingMiddleProductDetailDO> details = hcGrindingMiddleProductDetailMapper.selectListByRecordId(record.getId());
            if (zero(record.getSegmentTotalLength()).compareTo(segmentTotalLength) != 0) {
                record.setSegmentTotalLength(segmentTotalLength);
                needUpdate = true;
            }
            if (shouldResetAutoMiddleProductDetails(record, details)) {
                record.setGeneratedLength(generatedLength);
                hcGrindingMiddleProductRecordMapper.updateById(record);
                hcGrindingMiddleProductDetailMapper.deleteByRecordId(record.getId());
                initMiddleProductDetails(record);
                return toMiddleProductRecordResp(record);
            }
            if (details.isEmpty() && !Objects.equals(record.getGeneratedLength(), generatedLength)) {
                record.setGeneratedLength(generatedLength);
                needUpdate = true;
            } else if (!details.isEmpty() && !Objects.equals(record.getGeneratedLength(), details.size())) {
                record.setGeneratedLength(details.size());
                needUpdate = true;
            }
            if (needUpdate) {
                hcGrindingMiddleProductRecordMapper.updateById(record);
            }
            if (details.isEmpty() && generatedLength > 0) {
                initMiddleProductDetails(record);
            }
        }
        return toMiddleProductRecordResp(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMiddleProductRecord(HcRoughConsoleMiddleProductRecordSaveReqVO reqVO) {
        HcGrindingMiddleProductRecordDO businessRecord = validateBusinessMiddleProductRecord(reqVO.getRecordId());
        saveBusinessMiddleProductRecord(businessRecord, reqVO.getHeaderDataJson(), reqVO.getDetails(), false);
        return businessRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmMiddleProductRecord(HcRoughConsoleMiddleProductRecordSaveReqVO reqVO) {
        HcGrindingMiddleProductRecordDO businessRecord = validateBusinessMiddleProductRecord(reqVO.getRecordId());
        saveBusinessMiddleProductRecord(businessRecord, reqVO.getHeaderDataJson(), reqVO.getDetails(), true);
        return businessRecord.getId();
    }

    private Map<String, Object> parseMiddleProductHeaderMap(String headerDataJson) {
        if (StrUtil.isBlank(headerDataJson)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> parsed = JsonUtils.parseObjectQuietly(headerDataJson, new TypeReference<Map<String, Object>>() {});
        return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startWorkOrder(HcRoughReportStartReqVO reqVO) {
        return hcProcessReportService.startRoughReport(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchWorkOrderEquipment(HcRoughReportSwitchEquipmentReqVO reqVO) {
        // 每日点检/清洁记录按日期+设备加载，不随计划迁移；这里只切换计划工序挂接设备。
        hcProcessReportService.switchRoughEquipment(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long completeWorkOrder(HcRoughReportSaveReqVO reqVO) {
        HcPlanOrderDO plan = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(plan, reqVO.getPlanOperationId());
        assertFirstAllocatedWorkOrderCanComplete(plan, operation, reqVO.getBatchNo());
        return hcProcessReportService.submitRoughReport(reqVO);
    }

    private void assertFirstAllocatedWorkOrderCanComplete(HcPlanOrderDO plan,
                                                          HcPlanOrderOperationDO operation,
                                                          String requestBatchNo) {
        String motherBatchNo = firstNotBlank(resolveTimingMotherBatchNo(plan, operation), requestBatchNo);
        if (StrUtil.isBlank(motherBatchNo)) {
            return;
        }
        HcGrindingAllocationModeDO mode = hcGrindingAllocationModeMapper.selectByMotherBatchForUpdate(
                operation.getId(), motherBatchNo);
        if (mode == null) {
            throw invalidParamException("尚未产生P/Q/R/S/不分段一磨报工，不能完工");
        }
        if (!ALLOCATION_MODE_FIRST_ALLOCATED.equals(mode.getAllocationMode())) {
            throw invalidParamException("当前母批存在历史原有母批报工，不能按分段规则完工");
        }
        List<HcGrindingFirstAllocationDO> allocations = hcGrindingFirstAllocationMapper.selectListByModeId(mode.getId());
        if (allocations.isEmpty()) {
            throw invalidParamException("一磨前置分配模式尚未产生任何加工单元，不能完工");
        }
        HcGrindingSourceBalanceDO balance = hcGrindingSourceBalanceMapper.selectByBatchNoForUpdate(motherBatchNo);
        boolean balanceUsedUp = balance != null && zero(balance.getAvailableLength()).compareTo(RANGE_EPS) <= 0;
        if (!balanceUsedUp) {
            BigDecimal remainingLength = balance == null ? null : balance.getAvailableLength();
            throw invalidParamException("一磨来源余额尚未闭合"
                    + (remainingLength == null ? "" : "，当前剩余"
                    + zero(remainingLength).stripTrailingZeros().toPlainString() + "m")
                    + "，请继续分段或以不分段收口后再完工");
        }
        Map<Long, Long> secondCountByAllocation = hcGrindingSecondDetailMapper
                .selectListByPlanOperationId(operation.getId()).stream()
                .filter(item -> item.getFirstAllocationId() != null)
                .collect(Collectors.groupingBy(HcGrindingSecondDetailDO::getFirstAllocationId, Collectors.counting()));
        List<String> incompleteSegments = allocations.stream()
                .filter(item -> secondCountByAllocation.getOrDefault(item.getId(), 0L) != 1L)
                .map(item -> firstAllocationDisplayName(item.getSegmentMark()))
                .toList();
        if (!incompleteSegments.isEmpty()) {
            throw invalidParamException("以下一磨加工单元尚未形成唯一对应二磨：" + String.join("、", incompleteSegments));
        }
    }

    private Long saveDailyCheckInternal(HcRoughConsoleDailyCheckSaveReqVO reqVO, boolean confirm) {
        LocalDate recordDate = reqVO.getRecordDate() == null ? LocalDate.now() : reqVO.getRecordDate();
        HcStationFormDO form = hcStationFormMapper.selectEnabledByCode(reqVO.getFormCode());
        if (form == null) {
            throw invalidParamException("磨皮每日点检/清洁模板不存在");
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("设备不存在");
        }
        HcStationRecordDO record = reqVO.getRecordId() == null ? null : hcStationRecordMapper.selectById(reqVO.getRecordId());
        if (record == null) {
            record = hcStationRecordMapper.selectOneByEquipmentDaily(reqVO.getEquipmentId(), recordDate, reqVO.getFormCode());
        }
        if (record == null) {
            record = hcStationRecordMapper.selectLegacyPlanRecordForDailyFallback(reqVO.getPlanOperationId(), reqVO.getFormCode());
        }
        boolean newRecord = record == null;
        LocalDateTime now = LocalDateTime.now();
        String currentNickname = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        if (newRecord) {
            record = HcStationRecordDO.builder()
                    .formId(form.getId())
                    .formCode(form.getFormCode())
                    .formName(form.getFormName())
                    .triggerTimingCode(form.getTriggerTimingCode())
                    .triggerTimingName(form.getTriggerTimingName())
                    .tenantId(equipment.getTenantId())
                    .build();
        }
        record.setRecordScope(RECORD_SCOPE_DAILY);
        record.setRecordDate(recordDate);
        // 磨皮开机/清洁点检按设备+日期归档，不随当前计划挂接。
        record.setPlanId(null);
        record.setPlanNo(null);
        record.setPlanOperationId(null);
        record.setOperationCode(null);
        record.setOperationName(PROCESS_NAME);
        record.setFormId(form.getId());
        record.setFormCode(form.getFormCode());
        record.setFormName(form.getFormName());
        record.setTriggerTimingCode(form.getTriggerTimingCode());
        record.setTriggerTimingName(form.getTriggerTimingName());
        record.setEquipmentId(equipment.getId());
        record.setEquipmentCode(equipment.getEquipmentCode());
        record.setEquipmentName(equipment.getEquipmentName());
        record.setWorkCenterId(firstNonNull(reqVO.getWorkCenterId(), equipment.getWorkCenterId()));
        record.setWorkCenterCode(firstNotBlank(reqVO.getWorkCenterCode(), equipment.getWorkCenterCode()));
        record.setWorkCenterName(firstNotBlank(reqVO.getWorkCenterName(), equipment.getWorkCenterName()));
        record.setTenantId(firstNonNull(record.getTenantId(), equipment.getTenantId()));
        record.setDocStatus(confirm ? DOC_STATUS_CONFIRMED : DOC_STATUS_RECORDED);
        record.setResultStatus(StrUtil.blankToDefault(reqVO.getResult(), "OK"));
        record.setInspectionResult(reqVO.getInspectionResult());
        record.setHeaderDataJson(reqVO.getHeaderDataJson());
        record.setFormRemark(reqVO.getFormRemark());
        record.setConfirmRemark(reqVO.getConfirmRemark());
        record.setRecordUserName(firstNotBlank(reqVO.getRecorder(), record.getRecordUserName(), currentNickname));
        LocalDateTime recordTimeFallback = normalizeDailyRecordTime(firstNonNull(record.getRecordTime(), now), recordDate, now);
        LocalDateTime recorderTime = reqVO.getRecorderTime();
        if (!confirm && recorderTime == null) {
            recorderTime = now;
        }
        record.setRecordTime(normalizeDailyRecordTime(recorderTime, recordDate, confirm ? recordTimeFallback : now));
        if (confirm) {
            record.setConfirmUserName(firstNotBlank(reqVO.getConfirmer(), currentNickname));
            record.setConfirmTime(normalizeDailyRecordTime(reqVO.getConfirmerTime(), recordDate, now));
        }
        if (newRecord) {
            hcStationRecordMapper.insert(record);
        } else {
            hcStationRecordMapper.updateById(record);
        }
        hcStationRecordItemMapper.deleteByRecordId(record.getId());
        if (reqVO.getDetails() != null && !reqVO.getDetails().isEmpty()) {
            Long recordId = record.getId();
            Long tenantId = record.getTenantId();
            List<HcStationRecordItemDO> items = reqVO.getDetails().stream()
                    .map(item -> toRecordItem(recordId, item, tenantId))
                    .toList();
            hcStationRecordItemMapper.insertBatch(items);
        }
        return record.getId();
    }

    private HcRoughReportTaskRespVO buildTask(HcPlanOrderDO plan, HcPlanOrderOperationDO operation, Long equipmentId) {
        HcRoughReportTaskPageReqVO reqVO = new HcRoughReportTaskPageReqVO();
        reqVO.setTaskStatus("ALL");
        reqVO.setTaskKeyword(plan.getPlanNo());
        return hcProcessReportService.getRoughTaskList(reqVO).stream()
                .filter(task -> operation.getId().equals(task.getPlanOperationId()))
                .findFirst()
                .map(task -> applyTaskEquipment(task, equipmentId))
                .orElseGet(() -> fallbackTask(plan, operation, equipmentId));
    }

    private HcRoughReportTaskRespVO applyTaskEquipment(HcRoughReportTaskRespVO task, Long equipmentId) {
        if (equipmentId == null) {
            return task;
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return task;
        }
        task.setEquipmentId(equipment.getId());
        task.setEquipmentCode(equipment.getEquipmentCode());
        task.setEquipmentName(equipment.getEquipmentName());
        task.setWorkCenterId(firstNonNull(task.getWorkCenterId(), equipment.getWorkCenterId()));
        return task;
    }

    private HcRoughReportTaskRespVO fallbackTask(HcPlanOrderDO plan, HcPlanOrderOperationDO operation, Long equipmentId) {
        HcEquipmentDO equipment = equipmentId == null ? null : hcEquipmentMapper.selectById(equipmentId);
        HcRoughReportTaskRespVO task = new HcRoughReportTaskRespVO();
        task.setId("ROUGH-" + operation.getId());
        task.setPlanId(plan.getId());
        task.setPlanOperationId(operation.getId());
        task.setPlanNo(plan.getPlanNo());
        task.setProduct(plan.getMaterialName());
        task.setMaterialCode(plan.getMaterialCode());
        task.setProductName(plan.getMaterialName());
        task.setMotherMaterialCode(operation.getMotherMaterialCode());
        task.setMotherMaterialName(operation.getMotherMaterialName());
        task.setModelCode(plan.getModelCode());
        task.setMotherModelCode(operation.getMotherModelCode());
        task.setProductionStartDate(plan.getProductionStartDate());
        task.setProductionEndDate(plan.getProductionEndDate());
        task.setBatchNo(operation.getBatchNo());
        task.setProductionBatchNo(operation.getProductionBatchNo());
        task.setParentProductionBatchNo(operation.getParentProductionBatchNo());
        task.setProcess(operation.getOpName());
        task.setWorkCenterId(operation.getWorkCenterId());
        task.setEquipmentId(equipment == null ? operation.getEquipmentId() : equipment.getId());
        task.setEquipmentCode(equipment == null ? operation.getEquipmentCode() : equipment.getEquipmentCode());
        task.setEquipmentName(equipment == null ? operation.getEquipmentName() : equipment.getEquipmentName());
        task.setPlanQty(operation.getRequiredQty());
        task.setUom(firstNotBlank(operation.getUom(), operation.getUnitName(), plan.getTargetUom()));
        task.setStatus(operation.getOperationStatus());
        task.setRequirements(operation.getInstructionText());
        return task;
    }

    private HcRoughReportTaskRespVO buildEquipmentOnlyTask(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return null;
        }
        HcRoughReportTaskRespVO task = new HcRoughReportTaskRespVO();
        task.setEquipmentId(equipment.getId());
        task.setEquipmentCode(equipment.getEquipmentCode());
        task.setEquipmentName(equipment.getEquipmentName());
        task.setWorkCenterId(equipment.getWorkCenterId());
        task.setProcess(PROCESS_NAME);
        return task;
    }

    private boolean isEnabledEquipment(HcEquipmentDO equipment) {
        return equipment != null && (equipment.getStatus() == null || equipment.getStatus() == 0);
    }

    private HcEquipmentSelectOptionRespVO toEquipmentOption(HcEquipmentDO equipment) {
        HcEquipmentSelectOptionRespVO option = new HcEquipmentSelectOptionRespVO();
        option.setValue(equipment.getId());
        option.setCode(equipment.getEquipmentCode());
        option.setName(equipment.getEquipmentName());
        option.setLabel(StrUtil.isAllNotBlank(equipment.getEquipmentCode(), equipment.getEquipmentName())
                ? equipment.getEquipmentCode() + " / " + equipment.getEquipmentName()
                : StrUtil.blankToDefault(equipment.getEquipmentCode(), equipment.getEquipmentName()));
        option.setWorkCenterId(equipment.getWorkCenterId());
        option.setWorkCenterCode(equipment.getWorkCenterCode());
        option.setWorkCenterName(equipment.getWorkCenterName());
        option.setStatus(equipment.getStatus());
        option.setWorkStatus(equipment.getWorkStatus());
        return option;
    }

    private List<HcWetPassWorkItemRespVO> buildWorkDetails(List<HcStationRecordItemDO> recordItems,
                                                           List<HcStationFormItemDO> templateItems) {
        Map<Integer, HcStationRecordItemDO> recordItemMap = new LinkedHashMap<>();
        if (recordItems != null) {
            for (HcStationRecordItemDO recordItem : recordItems) {
                recordItemMap.put(recordItem.getItemSeq(), recordItem);
            }
        }
        List<HcWetPassWorkItemRespVO> result = new ArrayList<>();
        if (templateItems == null || templateItems.isEmpty()) {
            for (HcStationRecordItemDO recordItem : recordItemMap.values()) {
                result.add(toWorkItemResp(recordItem));
            }
            return result;
        }
        for (HcStationFormItemDO templateItem : templateItems) {
            HcStationRecordItemDO recordItem = recordItemMap.get(templateItem.getItemSeq());
            HcWetPassWorkItemRespVO itemRespVO = new HcWetPassWorkItemRespVO();
            itemRespVO.setItemSeq(templateItem.getItemSeq());
            itemRespVO.setCategory(templateItem.getItemCategory());
            itemRespVO.setNode(templateItem.getStepNode());
            itemRespVO.setItem(templateItem.getItemName());
            itemRespVO.setStandard(templateItem.getStandardText());
            itemRespVO.setValueMode(templateItem.getValueMode());
            itemRespVO.setDualLabel1(templateItem.getDualLabel1());
            itemRespVO.setDualLabel2(templateItem.getDualLabel2());
            itemRespVO.setActualValue(recordItem == null ? null : recordItem.getActualValue());
            itemRespVO.setActualValue2(recordItem == null ? null : recordItem.getActualValue2());
            itemRespVO.setStatus(recordItem == null ? StrUtil.blankToDefault(templateItem.getDefaultResult(), "OK") : recordItem.getResultFlag());
            itemRespVO.setRemark(recordItem == null ? templateItem.getRemark() : recordItem.getAbnormalRemark());
            result.add(itemRespVO);
        }
        return result;
    }

    private HcWetPassWorkItemRespVO toWorkItemResp(HcStationRecordItemDO recordItem) {
        HcWetPassWorkItemRespVO itemRespVO = new HcWetPassWorkItemRespVO();
        itemRespVO.setItemSeq(recordItem.getItemSeq());
        itemRespVO.setCategory(recordItem.getItemCategory());
        itemRespVO.setNode(recordItem.getStepNode());
        itemRespVO.setItem(recordItem.getItemName());
        itemRespVO.setStandard(recordItem.getStandardText());
        itemRespVO.setValueMode(recordItem.getValueMode());
        itemRespVO.setDualLabel1(recordItem.getDualLabel1());
        itemRespVO.setDualLabel2(recordItem.getDualLabel2());
        itemRespVO.setActualValue(recordItem.getActualValue());
        itemRespVO.setActualValue2(recordItem.getActualValue2());
        itemRespVO.setStatus(recordItem.getResultFlag());
        itemRespVO.setRemark(recordItem.getAbnormalRemark());
        return itemRespVO;
    }

    private HcStationRecordItemDO toRecordItem(Long recordId, HcWetPassWorkItemReqVO item, Long tenantId) {
        return HcStationRecordItemDO.builder()
                .recordId(recordId)
                .itemSeq(item.getItemSeq())
                .itemCategory(item.getCategory())
                .stepNode(item.getNode())
                .itemName(StrUtil.blankToDefault(item.getItem(), "明细项" + firstNonNull(item.getItemSeq(), 0)))
                .standardText(item.getStandard())
                .valueMode(item.getValueMode())
                .dualLabel1(item.getDualLabel1())
                .dualLabel2(item.getDualLabel2())
                .actualValue(item.getActualValue())
                .actualValue2(item.getActualValue2())
                .resultFlag(StrUtil.blankToDefault(item.getStatus(), "OK"))
                .abnormalRemark(item.getRemark())
                .tenantId(tenantId)
                .build();
    }

    private Long upsertProcessCheckStationRecord(Long previousRecordId, String bizType, Long bizId, String passType,
                                                 String headerDataJson, List<HcWetPassWorkItemReqVO> details,
                                                 HcStationFormDO form, Long sourceProcessFormRecordId,
                                                 HcPlanOrderDO plan, HcPlanOrderOperationDO operation, HcEquipmentDO equipment,
                                                 String operatorName, LocalDate recordDate) {
        if (bizId == null || details == null || details.isEmpty()) {
            return previousRecordId;
        }
        if (form == null) {
            throw invalidParamException("未解析到正式发布的磨皮工艺参数点检表动态表单");
        }
        HcStationRecordDO record = previousRecordId == null ? null : hcStationRecordMapper.selectById(previousRecordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            record = hcStationRecordMapper.selectOneByBiz(RECORD_SCOPE_PROCESS_DETAIL, bizType, bizId, form.getFormCode());
        }
        boolean newRecord = record == null;
        LocalDateTime now = LocalDateTime.now();
        if (newRecord) {
            record = HcStationRecordDO.builder()
                    .formId(form.getId())
                    .formCode(form.getFormCode())
                    .formName(form.getFormName())
                    .tenantId(plan.getTenantId())
                    .build();
        }
        record.setRecordScope(RECORD_SCOPE_PROCESS_DETAIL);
        record.setRecordDate(firstNonNull(recordDate, LocalDate.now()));
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setPlanId(plan.getId());
        record.setPlanNo(plan.getPlanNo());
        record.setPlanOperationId(operation.getId());
        record.setOperationCode(operation.getOpCode());
        record.setOperationName(operation.getOpName());
        record.setFormId(form.getId());
        record.setFormCode(form.getFormCode());
        record.setFormName(form.getFormName());
        record.setTriggerTimingCode(form.getTriggerTimingCode());
        record.setTriggerTimingName(firstNotBlank(form.getTriggerTimingName(), "生产中"));
        record.setDocStatus(DOC_STATUS_RECORDED);
        record.setResultStatus(calcPassWorkResult(details));
        record.setInspectionResult(record.getResultStatus());
        record.setEquipmentId(equipment.getId());
        record.setEquipmentCode(equipment.getEquipmentCode());
        record.setEquipmentName(equipment.getEquipmentName());
        record.setWorkCenterId(equipment.getWorkCenterId());
        record.setWorkCenterCode(equipment.getWorkCenterCode());
        record.setWorkCenterName(equipment.getWorkCenterName());
        record.setRecordUserName(firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        record.setRecordTime(now);
        record.setHeaderDataJson(headerDataJson);
        record.setFormRemark("FIRST".equals(passType) ? "第一次磨皮工艺参数点检" : "第二次磨皮工艺参数点检");
        record.setSourceProcessFormRecordId(sourceProcessFormRecordId);
        record.setTenantId(firstNonNull(record.getTenantId(), plan.getTenantId()));
        if (newRecord) {
            hcStationRecordMapper.insert(record);
        } else {
            hcStationRecordMapper.updateById(record);
        }
        hcStationRecordItemMapper.deleteByRecordId(record.getId());
        Long recordId = record.getId();
        Long tenantId = record.getTenantId();
        hcStationRecordItemMapper.insertBatch(details.stream()
                .map(item -> toRecordItem(recordId, item, tenantId))
                .toList());
        return recordId;
    }

    /**
     * 服务端从已确认的动态表单记录重新取数，禁止以浏览器回传的点检明细替代真实记录。
     */
    private ProcessCheckSnapshot resolveConfirmedProcessCheckSnapshot(Long processFormRecordId,
                                                                       HcPlanOrderDO plan,
                                                                       HcPlanOrderOperationDO operation,
                                                                       String passType,
                                                                       String expectedSourceRowId) {
        if (processFormRecordId == null) {
            throw invalidParamException("必须引用已确认的" + passDisplayName(passType) + "工艺参数点检表");
        }
        HcProcessFormRecordDO record = hcProcessFormRecordMapper.selectById(processFormRecordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw invalidParamException("引用的工艺参数点检记录不存在或已删除");
        }
        if (!"CONFIRMED".equalsIgnoreCase(record.getRecordStatus())) {
            throw invalidParamException("引用的工艺参数点检表尚未确认，不能报工");
        }
        if (!PROCESS_CODE.equalsIgnoreCase(record.getProcessCode()) || !"PROCESS_PARAM".equalsIgnoreCase(record.getFormType())) {
            throw invalidParamException("引用记录不是磨皮工艺参数点检表");
        }
        if (!Objects.equals(record.getPlanId(), plan.getId())
                || !Objects.equals(record.getPlanOperationId(), operation.getId())) {
            throw invalidParamException("引用的工艺参数点检表与当前生产计划或工序不一致");
        }
        HcStationFormDO form = hcStationFormService.resolvePublishedHcStationForm(
                PROCESS_CODE, plan.getModelCode(), "PROCESS_PARAM", passType);
        if (form == null) {
            throw invalidParamException("未配置" + passDisplayName(passType) + "正式发布的工艺参数点检表");
        }
        if (!Objects.equals(record.getTemplateId(), form.getId())
                || !Objects.equals(record.getTemplateCode(), form.getFormCode())) {
            throw invalidParamException("引用的工艺参数点检表不是当前型号正式发布版本，请重新填写并确认");
        }
        Map<String, Object> context = StrUtil.isBlank(record.getContextJson())
                ? new LinkedHashMap<>()
                : JsonUtils.parseObjectQuietly(record.getContextJson(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> businessParams = new LinkedHashMap<>();
        if (context != null && context.get("businessParams") instanceof Map<?, ?> rawParams) {
            rawParams.forEach((key, value) -> businessParams.put(String.valueOf(key), value));
        }
        String recordPass = firstNotBlank(toStringValue(businessParams.get("grindingPass")),
                toStringValue(businessParams.get("passType")));
        if (!passType.equalsIgnoreCase(recordPass)) {
            throw invalidParamException("引用的工艺参数点检表与当前磨皮阶段不一致");
        }
        String sourceRowId = toStringValue(businessParams.get("sourceRowId"));
        if (StrUtil.isNotBlank(expectedSourceRowId) && !expectedSourceRowId.equalsIgnoreCase(StrUtil.trim(sourceRowId))) {
            throw invalidParamException("引用的工艺参数点检表与当前加工单元不一致");
        }
        List<HcWetPassWorkItemReqVO> details = hcProcessFormRecordItemMapper.selectListByRecordId(record.getId()).stream()
                .map(this::toProcessCheckWorkItem)
                .toList();
        if (details.isEmpty()) {
            throw invalidParamException("引用的工艺参数点检表没有可保存的明细项");
        }
        return new ProcessCheckSnapshot(record.getId(), form, record.getHeaderDataJson(), details);
    }

    private HcWetPassWorkItemReqVO toProcessCheckWorkItem(HcProcessFormRecordItemDO item) {
        HcWetPassWorkItemReqVO result = new HcWetPassWorkItemReqVO();
        result.setItemSeq(item.getItemSeq());
        result.setCategory(item.getItemCategory());
        result.setNode(item.getStepNode());
        result.setItem(firstNotBlank(item.getFieldLabel(), item.getFieldKey()));
        result.setStandard(item.getStandardText());
        result.setValueMode(item.getValueMode());
        result.setActualValue(firstNotBlank(item.getActualValue(), toPlain(item.getActualNumber())));
        result.setActualValue2(item.getActualValue2());
        result.setStatus(firstNotBlank(item.getResultFlag(), "OK"));
        result.setRemark(item.getAbnormalRemark());
        return result;
    }

    private String passDisplayName(String passType) {
        return PASS_TYPE_SECOND.equalsIgnoreCase(passType) ? "第二次磨皮" : "第一次磨皮";
    }

    private record ProcessCheckSnapshot(Long recordId, HcStationFormDO form,
                                        String headerDataJson, List<HcWetPassWorkItemReqVO> details) {
    }

    private String calcPassWorkResult(List<HcWetPassWorkItemReqVO> details) {
        boolean hasNg = details.stream().anyMatch(item -> "NG".equalsIgnoreCase(StrUtil.trim(item.getStatus())));
        return hasNg ? "NG" : "OK";
    }

    private List<HcWetPassWorkItemReqVO> buildProcessCheckDetails(HcRoughConsoleFirstReportSaveReqVO reqVO) {
        return List.of(
                processCheckItem(1, "生产环境", "", "环境温度", "20-28℃", null),
                processCheckItem(2, "工艺参数点检表", "", "气压", "0.45-0.60MPa", reqVO.getPressure()),
                processCheckItem(3, "工艺参数点检表", "", "线速", "8-12m/min", reqVO.getLineSpeed()),
                processCheckItem(4, "工艺参数点检表", "", "转速", "120-180rpm", reqVO.getRotationSpeed()),
                processCheckItem(5, "工艺参数点检表", "", "计米器", "与本次报工收卷米数一致", reqVO.getMeterCounter()),
                processCheckItem(6, "质量结果", "", "磨皮厚度", "按工艺卡", reqVO.getGrindingThickness()),
                processCheckItem(7, "质量结果", "", "磨皮后厚度", "按工艺卡", reqVO.getAfterGrindingThickness()));
    }

    private List<HcWetPassWorkItemReqVO> buildProcessCheckDetails(HcRoughConsoleSecondReportSaveReqVO reqVO) {
        List<HcWetPassWorkItemReqVO> details = new ArrayList<>(List.of(
                processCheckItem(1, "生产环境", "", "环境温度", "20-28℃", null),
                processCheckItem(2, "工艺参数点检表", "", "气压", "0.45-0.60MPa", reqVO.getPressure()),
                processCheckItem(3, "工艺参数点检表", "", "线速", "8-12m/min", reqVO.getLineSpeed()),
                processCheckItem(4, "工艺参数点检表", "", "转速", "120-180rpm", reqVO.getRotationSpeed()),
                processCheckItem(5, "工艺参数点检表", "", "计米器", "与本次报工收卷米数一致", reqVO.getMeterCounter()),
                processCheckItem(6, "质量结果", "", "磨皮厚度", "按工艺卡", reqVO.getGrindingThickness()),
                processCheckItem(7, "质量结果", "", "磨皮后厚度", "按工艺卡", reqVO.getAfterGrindingThickness())));
        details.add(processCheckItem(8, "磨皮后NAP层", "", "厚度", "按工艺卡", reqVO.getQualityThickness()));
        details.add(processCheckItem(9, "磨皮后NAP层", "", "磨后宽幅", "按工艺卡", reqVO.getQualityWidth()));
        details.add(processCheckItem(10, "磨皮后NAP层", "", "磨皮米数", "与本次报工收卷米数一致",
                reqVO.getGrindingMeters() == null ? null : toPlain(reqVO.getGrindingMeters())));
        return details;
    }

    private HcWetPassWorkItemReqVO processCheckItem(Integer seq, String category, String node, String itemName,
                                                    String standard, String actualValue) {
        HcWetPassWorkItemReqVO item = new HcWetPassWorkItemReqVO();
        item.setItemSeq(seq);
        item.setCategory(category);
        item.setNode(node);
        item.setItem(itemName);
        item.setStandard(standard);
        item.setValueMode("TEXT");
        item.setActualValue(actualValue);
        item.setStatus("OK");
        return item;
    }

    private String buildProcessCheckHeaderJson(String passType, String batchNo, BigDecimal processLength,
                                               String headerDataJson, Long processFormRecordId) {
        Map<String, Object> header = StrUtil.isBlank(headerDataJson)
                ? new LinkedHashMap<>()
                : JsonUtils.parseObjectQuietly(headerDataJson, new TypeReference<Map<String, Object>>() {});
        if (header == null) {
            header = new LinkedHashMap<>();
        }
        header.put("passType", passType);
        header.put("passName", "FIRST".equals(passType) ? "第一次磨皮" : "第二次磨皮");
        header.put("batchNo", batchNo);
        header.put("processLength", processLength == null ? null : toPlain(processLength));
        header.put("processFormRecordId", processFormRecordId);
        return JsonUtils.toJsonString(header);
    }

    private <T> List<T> firstNonEmpty(List<T> first, List<T> fallback) {
        return first == null || first.isEmpty() ? fallback : first;
    }

    private Long saveMiddleProductRecord(Long previousRecordId, String bizType, Long bizId, String passType,
                                         String headerDataJson, List<HcRoughConsoleMiddleProductItemReqVO> details,
                                         HcPlanOrderDO plan, HcPlanOrderOperationDO operation, HcEquipmentDO equipment,
                                         String operatorName, LocalDate reportDate, String batchNo, BigDecimal outputLength) {
        if (details == null || details.isEmpty()) {
            return previousRecordId;
        }
        Map<String, Object> header = parseMiddleProductHeaderMap(headerDataJson);
        String productionBatchNo = firstNotBlank(
                toStringValue(header.get("productionBatchNo")),
                batchNo,
                toStringValue(header.get("batchNo")));
        String segmentMark = normalizeSegmentMark(firstNotBlank(
                toStringValue(header.get("segmentMark")),
                inferSegmentMarkFromBatchNo(productionBatchNo)));
        HcGrindingMiddleProductRecordDO record = previousRecordId == null ? null : hcGrindingMiddleProductRecordMapper.selectById(previousRecordId);
        if (record == null) {
            record = hcGrindingMiddleProductRecordMapper.selectByBiz(bizType, bizId);
        }
        if (record == null && StrUtil.isNotBlank(segmentMark)) {
            record = hcGrindingMiddleProductRecordMapper.selectBySegment(operation.getId(), passType, segmentMark);
        }
        if (record == null) {
            record = hcGrindingMiddleProductRecordMapper.selectByProductionBatchNo(operation.getId(), passType, productionBatchNo);
        }
        boolean newRecord = record == null;
        LocalDateTime now = LocalDateTime.now();
        if (newRecord) {
            record = HcGrindingMiddleProductRecordDO.builder()
                    .formCode(FORM_MIDDLE_PRODUCT)
                    .formName("CMP软垫（W26P0100）磨皮中间品记录表")
                    .tenantId(plan.getTenantId())
                    .build();
        }
        record.setRecordDate(reportDate == null ? LocalDate.now() : reportDate);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setPlanId(plan.getId());
        record.setPlanNo(plan.getPlanNo());
        record.setPlanOperationId(operation.getId());
        record.setOperationCode(operation.getOpCode());
        record.setOperationName(operation.getOpName());
        record.setPassType(passType);
        String segmentName = segmentName(segmentMark);
        record.setPassName("FIRST".equals(passType) ? "一次磨皮"
                : StrUtil.isBlank(segmentMark) ? "二次磨皮" : "二次磨皮-" + segmentName);
        record.setSegmentMark(segmentMark);
        record.setSegmentName(segmentName);
        record.setSegmentTotalLength(zero(outputLength));
        record.setGeneratedLength(details.size());
        record.setDocStatus(DOC_STATUS_RECORDED);
        record.setResultStatus(calcMiddleProductResult(details));
        record.setEquipmentId(equipment.getId());
        record.setEquipmentCode(equipment.getEquipmentCode());
        record.setEquipmentName(equipment.getEquipmentName());
        record.setWorkCenterId(equipment.getWorkCenterId());
        record.setWorkCenterCode(equipment.getWorkCenterCode());
        record.setWorkCenterName(equipment.getWorkCenterName());
        record.setMotherModelCode(firstNotBlank(operation.getMotherModelCode(), plan.getMotherModelCode(), plan.getModelCode()));
        record.setMotherModelName(firstNotBlank(operation.getMotherModelName(), plan.getMotherModelName(), plan.getModelName()));
        record.setMaterialCode(plan.getMaterialCode());
        record.setMaterialName(plan.getMaterialName());
        record.setMotherBatchNo(batchNo);
        record.setProductionBatchNo(productionBatchNo);
        record.setRecorderId(SecurityFrameworkUtils.getLoginUserId());
        record.setRecorderName(firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        record.setRecorderTime(now);
        record.setHeaderDataJson(buildMiddleProductHeaderJson(passType, productionBatchNo, outputLength, header));
        record.setTenantId(firstNonNull(record.getTenantId(), plan.getTenantId()));
        if (newRecord) {
            hcGrindingMiddleProductRecordMapper.insert(record);
        } else {
            hcGrindingMiddleProductRecordMapper.updateById(record);
        }
        hcGrindingMiddleProductDetailMapper.deleteByRecordId(record.getId());
        insertMiddleProductDetails(record, details);
        return record.getId();
    }

    private String calcMiddleProductResult(List<HcRoughConsoleMiddleProductItemReqVO> details) {
        boolean hasNg = details.stream().anyMatch(item -> "NG".equalsIgnoreCase(StrUtil.trim(item.getResult())));
        return hasNg ? "NG" : "OK";
    }

    private String buildMiddleProductHeaderJson(String passType, String batchNo, BigDecimal outputLength) {
        return buildMiddleProductHeaderJson(passType, batchNo, outputLength, new LinkedHashMap<>());
    }

    private String buildMiddleProductHeaderJson(String passType, String batchNo, BigDecimal outputLength, Map<String, Object> header) {
        Map<String, Object> data = header == null ? new LinkedHashMap<>() : new LinkedHashMap<>(header);
        String segmentMark = normalizeSegmentMark(firstNotBlank(
                toStringValue(data.get("segmentMark")),
                inferSegmentMarkFromBatchNo(batchNo)));
        data.put("passType", StrUtil.blankToDefault(passType, ""));
        data.put("batchNo", StrUtil.blankToDefault(batchNo, ""));
        data.put("productionBatchNo", firstNotBlank(toStringValue(data.get("productionBatchNo")), batchNo));
        data.put("segmentMark", segmentMark);
        data.put("segmentName", segmentName(segmentMark));
        data.put("passName", "SECOND".equals(passType) && StrUtil.isNotBlank(segmentMark)
                ? "二次磨皮-" + segmentName(segmentMark)
                : "FIRST".equals(passType) ? "一次磨皮" : "二次磨皮");
        data.put("processLength", zero(outputLength).toPlainString());
        data.put("outputLength", zero(outputLength).toPlainString());
        return JsonUtils.toJsonString(data);
    }

    private HcGrindingMiddleProductRecordDO validateBusinessMiddleProductRecord(Long recordId) {
        HcGrindingMiddleProductRecordDO record = recordId == null ? null : hcGrindingMiddleProductRecordMapper.selectById(recordId);
        if (record != null && !Boolean.TRUE.equals(record.getDeleted()) && FORM_MIDDLE_PRODUCT.equals(record.getFormCode())) {
            return record;
        }
        return validateBusinessMiddleProductRecordByStationRecord(recordId);
    }

    private HcGrindingMiddleProductRecordDO validateBusinessMiddleProductRecordByStationRecord(Long stationRecordId) {
        HcStationRecordDO stationRecord = stationRecordId == null ? null : hcStationRecordMapper.selectById(stationRecordId);
        if (stationRecord == null || Boolean.TRUE.equals(stationRecord.getDeleted())
                || !FORM_MIDDLE_PRODUCT.equals(stationRecord.getFormCode())) {
            throw invalidParamException("磨皮中间品记录单不存在");
        }
        Map<String, Object> header = parseMiddleProductHeaderMap(stationRecord.getHeaderDataJson());
        HcGrindingMiddleProductRecordDO record = selectMiddleProductRecordByIdIfMatches(
                parseLong(toStringValue(header.get("middleProductRecordId"))), stationRecord);
        if (record == null && StrUtil.isNotBlank(stationRecord.getBizType()) && stationRecord.getBizId() != null) {
            record = hcGrindingMiddleProductRecordMapper.selectByBiz(stationRecord.getBizType(), stationRecord.getBizId());
        }
        if (record == null) {
            record = selectMiddleProductRecordByIdIfMatches(stationRecord.getBizId(), stationRecord);
        }
        String segmentMark = normalizeSegmentMark(firstNotBlank(
                toStringValue(header.get("segmentMark")),
                inferSegmentMarkFromBatchNo(firstNotBlank(
                        toStringValue(header.get("productionBatchNo")),
                        toStringValue(header.get("batchNo"))))));
        if (record == null && stationRecord.getPlanOperationId() != null && StrUtil.isNotBlank(segmentMark)) {
            String passType = firstNotBlank(toStringValue(header.get("passType")),
                    BIZ_GRINDING_SECOND.equals(stationRecord.getBizType()) ? "SECOND" : "FIRST");
            record = hcGrindingMiddleProductRecordMapper.selectBySegment(stationRecord.getPlanOperationId(), passType, segmentMark);
        }
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            throw invalidParamException("磨皮中间品业务记录不存在");
        }
        return record;
    }

    private HcGrindingMiddleProductRecordDO selectMiddleProductRecordByIdIfMatches(Long recordId, HcStationRecordDO stationRecord) {
        HcGrindingMiddleProductRecordDO record = recordId == null ? null : hcGrindingMiddleProductRecordMapper.selectById(recordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted()) || !FORM_MIDDLE_PRODUCT.equals(record.getFormCode())) {
            return null;
        }
        if (stationRecord != null && stationRecord.getPlanOperationId() != null && record.getPlanOperationId() != null
                && !Objects.equals(stationRecord.getPlanOperationId(), record.getPlanOperationId())) {
            return null;
        }
        return record;
    }

    private void saveBusinessMiddleProductRecord(HcGrindingMiddleProductRecordDO record, String headerDataJson,
                                                 List<HcRoughConsoleMiddleProductItemReqVO> details, boolean confirm) {
        if (details == null || details.isEmpty()) {
            throw invalidParamException("中间品记录单明细不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String currentNickname = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), record.getRecorderName(), "系统");
        record.setHeaderDataJson(firstNotBlank(headerDataJson, record.getHeaderDataJson()));
        Map<String, Object> header = parseMiddleProductHeaderMap(record.getHeaderDataJson());
        BigDecimal processLength = parseDecimal(firstNotBlank(
                toStringValue(header.get("processLength")),
                toStringValue(header.get("segmentTotalLength")),
                toStringValue(header.get("outputLength"))));
        if (processLength != null) {
            header.put("processLength", toPlain(processLength));
        }
        header.remove("generatedLength");
        BigDecimal widthMm = parseDecimal(firstNotBlank(toStringValue(header.get("widthMm")), toStringValue(header.get("width"))));
        if (widthMm != null) {
            record.setWidthMm(widthMm);
        }
        String productionBatchNo = firstNotBlank(toStringValue(header.get("productionBatchNo")), record.getProductionBatchNo(), toStringValue(header.get("batchNo")));
        String segmentMark = normalizeSegmentMark(firstNotBlank(
                toStringValue(header.get("segmentMark")),
                record.getSegmentMark(),
                inferSegmentMarkFromBatchNo(productionBatchNo)));
        String segmentName = segmentName(segmentMark);
        record.setProductionBatchNo(productionBatchNo);
        record.setSegmentMark(segmentMark);
        record.setSegmentName(segmentName);
        record.setPassName("SECOND".equals(record.getPassType()) && StrUtil.isNotBlank(segmentMark)
                ? "二次磨皮-" + segmentName
                : firstNotBlank(record.getPassName(), "SECOND".equals(record.getPassType()) ? "二次磨皮" : "一次磨皮"));
        header.put("productionBatchNo", productionBatchNo);
        header.put("segmentMark", segmentMark);
        header.put("segmentName", segmentName);
        header.put("passName", record.getPassName());
        record.setFormName("CMP软垫（W26P0100）磨皮中间品记录表");
        if (confirm) {
            String confirmerName = firstNotBlank(toStringValue(header.get("confirmer")), currentNickname, record.getConfirmerName(), "系统");
            LocalDateTime confirmerTime = parseHeaderDateTime(header.get("confirmerTime"), now);
            header.put("confirmer", confirmerName);
            header.put("confirmerTime", DATETIME_FORMATTER.format(confirmerTime));
            record.setConfirmerName(confirmerName);
            record.setConfirmerTime(confirmerTime);
        } else {
            record.setConfirmerName(firstNotBlank(toStringValue(header.get("confirmer")), record.getConfirmerName()));
        }
        record.setHeaderDataJson(JsonUtils.toJsonString(header));
        record.setResultStatus(calcMiddleProductResult(details));
        record.setDocStatus(confirm ? DOC_STATUS_CONFIRMED : DOC_STATUS_RECORDED);
        record.setGeneratedLength(details.size());
        record.setRecorderId(SecurityFrameworkUtils.getLoginUserId());
        record.setRecorderName(currentNickname);
        record.setRecorderTime(firstNonNull(record.getRecorderTime(), now));
        hcGrindingMiddleProductRecordMapper.updateById(record);
        hcGrindingMiddleProductDetailMapper.deleteByRecordId(record.getId());
        insertMiddleProductDetails(record, details);
    }

    private BigDecimal calcSecondSegmentLength(Long planOperationId, String segmentMark) {
        return sum(selectSecondSegmentDetails(planOperationId, segmentMark),
                detail -> firstNonNull(detail.getOutputLength(), detail.getProcessLength()));
    }

    private String resolveSegmentProductionBatchNo(HcPlanOrderDO plan, HcPlanOrderOperationDO operation, String segmentMark) {
        HcGrindingSecondDetailDO firstDetail = selectSecondSegmentDetails(operation.getId(), segmentMark).stream()
                .findFirst()
                .orElse(null);
        return firstDetail == null
                ? firstNotBlank(operation.getProductionBatchNo(), operation.getBatchNo(), plan.getProductionBatchNo())
                : firstNotBlank(firstDetail.getProductionBatchNo(), firstDetail.getConfirmedBatchNo(), firstDetail.getMotherBatchNo());
    }

    private List<HcGrindingSecondDetailDO> selectSecondSegmentDetails(Long planOperationId, String segmentMark) {
        String normalized = normalizeSegmentMark(segmentMark);
        return hcGrindingSecondDetailMapper.selectListByPlanOperationId(planOperationId).stream()
                .filter(detail -> Objects.equals(normalizeSegmentMark(detail.getSegmentMark()), normalized))
                .toList();
    }

    private HcGrindingMiddleProductRecordDO buildSegmentMiddleProductRecord(HcPlanOrderDO plan,
                                                                            HcPlanOrderOperationDO operation,
                                                                            String passType,
                                                                            String segmentMark,
                                                                            BigDecimal segmentTotalLength,
                                                                            int generatedLength) {
        List<HcGrindingSecondDetailDO> segmentDetails = selectSecondSegmentDetails(operation.getId(), segmentMark);
        HcGrindingSecondDetailDO firstDetail = segmentDetails.stream().findFirst().orElse(null);
        String segmentName = segmentName(segmentMark);
        String productionBatchNo = firstDetail == null ? firstNotBlank(operation.getProductionBatchNo(), operation.getBatchNo(), plan.getProductionBatchNo())
                : firstNotBlank(firstDetail.getProductionBatchNo(), firstDetail.getConfirmedBatchNo(), firstDetail.getMotherBatchNo());
        LocalDate recordDate = firstDetail == null ? LocalDate.now() : firstNonNull(firstDetail.getReportDate(), LocalDate.now());
        HcEquipmentDO equipment = firstDetail != null && firstDetail.getEquipmentId() != null
                ? hcEquipmentMapper.selectById(firstDetail.getEquipmentId())
                : operation.getEquipmentId() == null ? null : hcEquipmentMapper.selectById(operation.getEquipmentId());
        return HcGrindingMiddleProductRecordDO.builder()
                .planId(plan.getId())
                .planNo(plan.getPlanNo())
                .planOperationId(operation.getId())
                .operationCode(operation.getOpCode())
                .operationName(operation.getOpName())
                .formCode(FORM_MIDDLE_PRODUCT)
                .formName("CMP软垫（W26P0100）磨皮中间品记录表")
                .passType(passType)
                .passName("SECOND".equals(passType) ? "二次磨皮-" + segmentName : "一次磨皮")
                .bizType("SECOND".equals(passType) ? BIZ_GRINDING_SECOND : BIZ_GRINDING_FIRST)
                .segmentMark(segmentMark)
                .segmentName(segmentName)
                .segmentTotalLength(zero(segmentTotalLength))
                .generatedLength(generatedLength)
                .recordDate(recordDate)
                .docStatus("DRAFT")
                .resultStatus("OK")
                .equipmentId(equipment == null ? operation.getEquipmentId() : equipment.getId())
                .equipmentCode(equipment == null ? operation.getEquipmentCode() : equipment.getEquipmentCode())
                .equipmentName(equipment == null ? operation.getEquipmentName() : equipment.getEquipmentName())
                .workCenterId(equipment == null ? operation.getWorkCenterId() : firstNonNull(equipment.getWorkCenterId(), operation.getWorkCenterId()))
                .workCenterCode(equipment == null ? operation.getWorkCenterCode() : firstNotBlank(equipment.getWorkCenterCode(), operation.getWorkCenterCode()))
                .workCenterName(equipment == null ? operation.getWorkCenterName() : firstNotBlank(equipment.getWorkCenterName(), operation.getWorkCenterName()))
                .motherModelCode(firstNotBlank(operation.getMotherModelCode(), plan.getMotherModelCode(), plan.getModelCode()))
                .motherModelName(firstNotBlank(operation.getMotherModelName(), plan.getMotherModelName(), plan.getModelName()))
                .materialCode(plan.getMaterialCode())
                .materialName(plan.getMaterialName())
                .motherBatchNo(firstNotBlank(operation.getParentProductionBatchNo(), operation.getBatchNo(), plan.getBatchNo()))
                .productionBatchNo(productionBatchNo)
                .recorderId(SecurityFrameworkUtils.getLoginUserId())
                .recorderName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .recorderTime(LocalDateTime.now())
                .headerDataJson(buildMiddleProductHeaderJson(passType, productionBatchNo, segmentTotalLength))
                .tenantId(firstNonNull(operation.getTenantId(), plan.getTenantId()))
                .build();
    }

    private void initMiddleProductDetails(HcGrindingMiddleProductRecordDO record) {
        List<HcRoughConsoleMiddleProductItemReqVO> details = new ArrayList<>();
        BigDecimal remaining = zero(record.getSegmentTotalLength());
        int count = firstNonNull(record.getGeneratedLength(), 0);
        for (int index = 0; index < count; index += 1) {
            BigDecimal currentLength = remaining.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : remaining;
            BigDecimal lengthMeter = BigDecimal.valueOf(index + 1L);
            if (currentLength.compareTo(BigDecimal.ZERO) < 0) {
                currentLength = BigDecimal.ZERO;
            }
            remaining = remaining.subtract(currentLength);
            HcRoughConsoleMiddleProductItemReqVO item = new HcRoughConsoleMiddleProductItemReqVO();
            item.setSeq(index + 1);
            item.setRecordDate(record.getRecordDate());
            item.setModelCode(record.getMotherModelCode());
            item.setMaterialCode(record.getMaterialCode());
            item.setBatchNo(record.getProductionBatchNo());
            item.setInputLength(lengthMeter);
            item.setLengthMeter(lengthMeter);
            item.setOutputLength(lengthMeter);
            item.setGrindingPass(record.getPassName());
            item.setRecorderName(record.getRecorderName());
            details.add(item);
        }
        insertMiddleProductDetails(record, details);
    }

    private boolean shouldResetAutoMiddleProductDetails(HcGrindingMiddleProductRecordDO record,
                                                        List<HcGrindingMiddleProductDetailDO> details) {
        return record != null
                && StrUtil.equalsIgnoreCase(record.getDocStatus(), "DRAFT")
                && details != null
                && details.size() > MIDDLE_PRODUCT_DEFAULT_ROW_COUNT
                && details.stream().allMatch(detail -> StrUtil.isBlank(detail.getInnerThickness())
                        && StrUtil.isBlank(detail.getOuterThickness())
                        && StrUtil.isBlank(detail.getRemark()));
    }

    private void insertMiddleProductDetails(HcGrindingMiddleProductRecordDO record,
                                            List<HcRoughConsoleMiddleProductItemReqVO> details) {
        Long tenantId = firstNonNull(record.getTenantId(), 1L);
        for (int i = 0; i < details.size(); i += 1) {
            HcRoughConsoleMiddleProductItemReqVO item = details.get(i);
            hcGrindingMiddleProductDetailMapper.insert(toMiddleProductDetail(record, item, i + 1, tenantId));
        }
    }

    private HcStationRecordRespVO toMiddleProductRecordListResp(HcGrindingMiddleProductRecordDO record) {
        Map<String, Object> header = parseMiddleProductHeaderMap(record.getHeaderDataJson());
        header.put("recordSource", "ROUGH_MIDDLE_PRODUCT_BUSINESS");
        header.put("middleProductRecordId", record.getId());
        header.put("productionBatchNo", firstNotBlank(record.getProductionBatchNo(), toStringValue(header.get("productionBatchNo")), toStringValue(header.get("batchNo"))));
        header.put("batchNo", firstNotBlank(toStringValue(header.get("batchNo")), record.getProductionBatchNo(), record.getMotherBatchNo()));
        header.put("segmentMark", firstNotBlank(record.getSegmentMark(), toStringValue(header.get("segmentMark"))));
        header.put("segmentName", firstNotBlank(record.getSegmentName(), toStringValue(header.get("segmentName"))));
        header.put("passType", firstNotBlank(record.getPassType(), toStringValue(header.get("passType"))));
        header.put("passName", firstNotBlank(record.getPassName(), toStringValue(header.get("passName"))));
        header.put("processLength", firstNotBlank(
                toStringValue(header.get("processLength")),
                record.getSegmentTotalLength() == null ? null : toPlain(record.getSegmentTotalLength())));
        if (record.getWidthMm() != null) {
            header.put("widthMm", toPlain(record.getWidthMm()));
        }

        HcStationRecordRespVO respVO = new HcStationRecordRespVO();
        respVO.setId(record.getId());
        respVO.setPlanId(record.getPlanId());
        respVO.setPlanNo(record.getPlanNo());
        respVO.setBatchNo(firstNotBlank(record.getProductionBatchNo(), record.getMotherBatchNo()));
        respVO.setModelCode(record.getMotherModelCode());
        respVO.setModelName(record.getMotherModelName());
        respVO.setPlanOperationId(record.getPlanOperationId());
        respVO.setOperationCode(record.getOperationCode());
        respVO.setOperationName(record.getOperationName());
        respVO.setFormCode(record.getFormCode());
        respVO.setFormName(record.getFormName());
        respVO.setTriggerTimingCode(record.getPassType());
        respVO.setTriggerTimingName(record.getPassName());
        respVO.setDocStatus(record.getDocStatus());
        respVO.setResultStatus(record.getResultStatus());
        respVO.setInspectionResult(record.getResultStatus());
        respVO.setEquipmentId(record.getEquipmentId());
        respVO.setEquipmentCode(record.getEquipmentCode());
        respVO.setEquipmentName(record.getEquipmentName());
        respVO.setWorkCenterId(record.getWorkCenterId());
        respVO.setWorkCenterCode(record.getWorkCenterCode());
        respVO.setWorkCenterName(record.getWorkCenterName());
        respVO.setRecordScope(RECORD_SCOPE_PROCESS_DETAIL);
        respVO.setRecordDate(record.getRecordDate());
        respVO.setBizType(record.getBizType());
        respVO.setBizId(record.getBizId());
        respVO.setRecordUserName(record.getRecorderName());
        respVO.setRecordTime(record.getRecorderTime());
        respVO.setCreateUserName(firstNotBlank(record.getRecorderName(), record.getCreator()));
        respVO.setCreator(record.getCreator());
        respVO.setCreateTime(record.getCreateTime());
        respVO.setConfirmUserName(record.getConfirmerName());
        respVO.setConfirmTime(record.getConfirmerTime());
        respVO.setHeaderDataJson(JsonUtils.toJsonString(header));
        respVO.setFormRemark(record.getRemark());
        respVO.setUpdater(record.getUpdater());
        respVO.setUpdateTime(record.getUpdateTime());
        return respVO;
    }

    private HcGrindingMiddleProductDetailDO toMiddleProductDetail(HcGrindingMiddleProductRecordDO record,
                                                                  HcRoughConsoleMiddleProductItemReqVO item,
                                                                  int fallbackSeq,
                                                                  Long tenantId) {
        return HcGrindingMiddleProductDetailDO.builder()
                .recordId(record.getId())
                .seq(firstNonNull(item.getSeq(), fallbackSeq))
                .recordDate(firstNonNull(item.getRecordDate(), record.getRecordDate()))
                .modelCode(firstNotBlank(item.getModelCode(), record.getMotherModelCode()))
                .materialCode(firstNotBlank(item.getMaterialCode(), record.getMaterialCode()))
                .batchNo(firstNotBlank(item.getBatchNo(), record.getProductionBatchNo(), record.getMotherBatchNo()))
                .lengthMeter(firstNonNull(item.getLengthMeter(), parseDecimal(item.getLength()), item.getInputLength()))
                .innerThickness(firstNotBlank(item.getInnerThickness(), item.getThickness()))
                .outerThickness(firstNotBlank(item.getOuterThickness(), item.getWidth()))
                .inputLength(item.getInputLength())
                .outputLength(item.getOutputLength())
                .grindingPass(firstNotBlank(item.getGrindingPass(), record.getPassName()))
                .sandpaperLife(item.getSandpaperLife())
                .sandpaperBatchNo(item.getSandpaperBatchNo())
                .guideClothLife(item.getGuideClothLife())
                .guideClothBatchNo(item.getGuideClothBatchNo())
                .replaceReason(item.getReplaceReason())
                .recorderName(firstNotBlank(item.getRecorderName(), record.getRecorderName()))
                .remark(item.getRemark())
                .tenantId(tenantId)
                .build();
    }

    private HcRoughConsoleMiddleProductRecordRespVO toMiddleProductRecordResp(HcGrindingMiddleProductRecordDO record) {
        List<HcGrindingMiddleProductDetailDO> details = hcGrindingMiddleProductDetailMapper.selectListByRecordId(record.getId());
        Map<String, Object> header = parseMiddleProductHeaderMap(record.getHeaderDataJson());
        HcRoughConsoleMiddleProductRecordRespVO respVO = new HcRoughConsoleMiddleProductRecordRespVO();
        respVO.setRecordId(record.getId());
        respVO.setFormCode(record.getFormCode());
        respVO.setFormName(record.getFormName());
        respVO.setPassType(record.getPassType());
        respVO.setPassName(record.getPassName());
        respVO.setSegmentMark(record.getSegmentMark());
        respVO.setSegmentName(record.getSegmentName());
        respVO.setSegmentTotalLength(record.getSegmentTotalLength());
        respVO.setProcessLength(firstNonNull(
                parseDecimal(firstNotBlank(
                        toStringValue(header.get("processLength")),
                        toStringValue(header.get("segmentTotalLength")),
                        toStringValue(header.get("outputLength")))),
                record.getSegmentTotalLength()));
        respVO.setMaterialCode(record.getMaterialCode());
        respVO.setMaterialName(record.getMaterialName());
        respVO.setMotherModelCode(record.getMotherModelCode());
        respVO.setMotherModelName(record.getMotherModelName());
        respVO.setMotherBatchNo(record.getMotherBatchNo());
        respVO.setProductionBatchNo(record.getProductionBatchNo());
        respVO.setDocStatus(record.getDocStatus());
        respVO.setResultStatus(record.getResultStatus());
        respVO.setHeaderDataJson(record.getHeaderDataJson());
        respVO.setGeneratedLength(details.isEmpty() ? firstNonNull(record.getGeneratedLength(), 0) : details.size());
        respVO.setRecorder(record.getRecorderName());
        respVO.setRecordDate(record.getRecordDate());
        respVO.setRecorderTime(record.getRecorderTime());
        respVO.setWidthMm(record.getWidthMm());
        respVO.setConfirmer(record.getConfirmerName());
        respVO.setConfirmerTime(record.getConfirmerTime());
        respVO.setDetails(details.stream().map(this::toMiddleProductItemResp).toList());
        return respVO;
    }

    private HcRoughConsoleMiddleProductItemRespVO toMiddleProductItemResp(HcGrindingMiddleProductDetailDO item) {
        HcRoughConsoleMiddleProductItemRespVO respVO = new HcRoughConsoleMiddleProductItemRespVO();
        respVO.setSeq(item.getSeq());
        respVO.setRecordDate(item.getRecordDate());
        respVO.setModelCode(item.getModelCode());
        respVO.setMaterialCode(item.getMaterialCode());
        respVO.setBatchNo(item.getBatchNo());
        respVO.setLengthMeter(item.getLengthMeter());
        respVO.setInnerThickness(item.getInnerThickness());
        respVO.setOuterThickness(item.getOuterThickness());
        respVO.setInputLength(item.getInputLength());
        respVO.setOutputLength(item.getOutputLength());
        respVO.setGrindingPass(item.getGrindingPass());
        respVO.setSandpaperLife(item.getSandpaperLife());
        respVO.setSandpaperBatchNo(item.getSandpaperBatchNo());
        respVO.setGuideClothLife(item.getGuideClothLife());
        respVO.setGuideClothBatchNo(item.getGuideClothBatchNo());
        respVO.setReplaceReason(item.getReplaceReason());
        respVO.setRecorderName(item.getRecorderName());
        respVO.setRemark(item.getRemark());
        return respVO;
    }

    private HcEquipmentConsumableStateDO increaseConsumable(HcEquipmentDO equipment, HcPlanOrderDO plan,
                                                            HcPlanOrderOperationDO operation, String type,
                                                            String overrideBatchNo, BigDecimal changeLength,
                                                            String grindingStage, Long detailId, LocalDateTime eventTime,
                                                            Long operatorId, String operatorName) {
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(equipment.getId(), PROCESS_CODE, type);
        boolean newState = state == null;
        if (newState) {
            state = HcEquipmentConsumableStateDO.builder()
                    .equipmentId(equipment.getId())
                    .equipmentCode(equipment.getEquipmentCode())
                    .equipmentName(equipment.getEquipmentName())
                    .workCenterId(equipment.getWorkCenterId())
                    .workCenterCode(equipment.getWorkCenterCode())
                    .workCenterName(equipment.getWorkCenterName())
                    .processCode(PROCESS_CODE)
                    .processName(PROCESS_NAME)
                    .consumableType(type)
                    .batchNo(firstNotBlank(overrideBatchNo, "-"))
                    .lastReplaceTime(eventTime)
                    .useCount(0)
                    .usedLength(BigDecimal.ZERO)
                    .limitCount(null)
                    .limitLength(null)
                    .warningFlag(0)
                    .status("IN_USE")
                    .tenantId(equipment.getTenantId())
                    .build();
        }
        applyConsumableRule(state, equipment, type);
        String beforeBatchNo = state.getBatchNo();
        Integer beforeCount = state.getUseCount();
        BigDecimal beforeLength = state.getUsedLength();
        if (StrUtil.isNotBlank(overrideBatchNo)) {
            state.setBatchNo(overrideBatchNo);
        }
        Long effectiveOperatorId = firstNonNull(operatorId, SecurityFrameworkUtils.getLoginUserId());
        String effectiveOperatorName = firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        state.setUseCount((state.getUseCount() == null ? 0 : state.getUseCount())
                + consumableUseIncrement(state.getConsumableType()));
        state.setUsedLength(zero(state.getUsedLength()).add(zero(changeLength)));
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus("IN_USE");
        state.setLastOperatorId(effectiveOperatorId);
        state.setLastOperatorName(effectiveOperatorName);
        state.setLastEventTime(eventTime);
        if (newState) {
            hcEquipmentConsumableStateMapper.insert(state);
        } else {
            hcEquipmentConsumableStateMapper.updateById(state);
        }
        insertConsumableEvent(state, plan.getId(), plan.getPlanNo(), operation.getId(), operation.getOpCode(), operation.getOpName(),
                grindingStage, detailId, "USE", beforeBatchNo, state.getBatchNo(), beforeCount, state.getUseCount(),
                beforeLength, state.getUsedLength(), zero(changeLength), null, effectiveOperatorId,
                effectiveOperatorName, eventTime, plan.getTenantId());
        return state;
    }

    private ConsumableReportResult increaseReportConsumable(HcEquipmentDO equipment, HcPlanOrderDO plan,
                                                            HcPlanOrderOperationDO operation, String type,
                                                            String currentBatchNo, String targetBatchNo,
                                                            String replaceReason, Boolean physicalReplace,
                                                            BigDecimal changeLength,
                                                            String grindingStage, Long detailId,
                                                            LocalDateTime eventTime, Long operatorId,
                                                            String operatorName) {
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(equipment.getId(), PROCESS_CODE, type);
        boolean newState = state == null;
        if (newState) {
            state = HcEquipmentConsumableStateDO.builder()
                    .equipmentId(equipment.getId())
                    .equipmentCode(equipment.getEquipmentCode())
                    .equipmentName(equipment.getEquipmentName())
                    .workCenterId(equipment.getWorkCenterId())
                    .workCenterCode(equipment.getWorkCenterCode())
                    .workCenterName(equipment.getWorkCenterName())
                    .processCode(PROCESS_CODE)
                    .processName(PROCESS_NAME)
                    .consumableType(type)
                    .batchNo(firstNotBlank(normalizeBatchNo(currentBatchNo), normalizeBatchNo(targetBatchNo), "-"))
                    .lastReplaceTime(eventTime)
                    .useCount(0)
                    .usedLength(BigDecimal.ZERO)
                    .warningFlag(0)
                    .status("IN_USE")
                    .tenantId(equipment.getTenantId())
                    .build();
        }
        applyConsumableRule(state, equipment, type);
        if (newState) {
            hcEquipmentConsumableStateMapper.insert(state);
        }
        Long effectiveOperatorId = firstNonNull(operatorId, SecurityFrameworkUtils.getLoginUserId());
        String effectiveOperatorName = firstNotBlank(operatorName, SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        String targetBatch = normalizeBatchNo(targetBatchNo);
        String stateBatch = normalizeBatchNo(state.getBatchNo());
        String effectiveTargetBatch = firstNotBlank(targetBatch, stateBatch);
        boolean batchChanged = StrUtil.isNotBlank(targetBatch) && !Objects.equals(targetBatch, stateBatch);
        if (Boolean.FALSE.equals(physicalReplace) && batchChanged) {
            throw invalidParamException("未选择更换时，提交批号必须与设备当前批号一致");
        }
        if (Boolean.TRUE.equals(physicalReplace) && StrUtil.isBlank(effectiveTargetBatch)) {
            throw invalidParamException("选择更换后必须填写耗材批号");
        }
        // 新前端以“是否更换”为准，同批号物理换卷也会重置；旧调用方未传该字段时兼容原有的批号差异判断。
        boolean replaceRequested = !newState && (Boolean.TRUE.equals(physicalReplace)
                || (physicalReplace == null && batchChanged));
        if (!replaceRequested) {
            addConsumableUsage(state, plan, operation, grindingStage, detailId, zero(changeLength),
                    effectiveOperatorId, effectiveOperatorName, eventTime);
            return new ConsumableReportResult(state, CONSUMABLE_SANDPAPER.equals(type)
                    ? List.of(buildSandpaperUsageSnapshot(1, state)) : List.of());
        }
        if (CONSUMABLE_SANDPAPER.equals(type)) {
            // 一次、二次磨皮的同一笔报工可能跨越旧砂纸寿命上限：先消耗旧砂纸余量，
            // 再将溢出部分记入新砂纸，并保留两份不可变快照同步到生产记录。
            BigDecimal remainingLength = state.getLimitLength() == null
                    ? BigDecimal.ZERO
                    : nonNegative(state.getLimitLength().subtract(zero(state.getUsedLength())));
            BigDecimal oldBatchUseLength = zero(changeLength).min(remainingLength);
            if (oldBatchUseLength.compareTo(BigDecimal.ZERO) > 0) {
                addConsumableUsage(state, plan, operation, grindingStage, detailId, oldBatchUseLength,
                        effectiveOperatorId, effectiveOperatorName, eventTime);
            }
            SandpaperUsageSnapshot oldSandpaper = buildSandpaperUsageSnapshot(1, state);
            replaceConsumableBatchForReport(state, plan, operation, grindingStage, detailId, effectiveTargetBatch, replaceReason,
                    effectiveOperatorId, effectiveOperatorName, eventTime);
            BigDecimal newBatchUseLength = nonNegative(zero(changeLength).subtract(oldBatchUseLength));
            if (newBatchUseLength.compareTo(BigDecimal.ZERO) > 0) {
                addConsumableUsage(state, plan, operation, grindingStage, detailId, newBatchUseLength,
                        effectiveOperatorId, effectiveOperatorName, eventTime);
            }
            return new ConsumableReportResult(state, List.of(oldSandpaper, buildSandpaperUsageSnapshot(2, state)));
        }
        replaceConsumableBatchForReport(state, plan, operation, grindingStage, detailId, effectiveTargetBatch, replaceReason,
                effectiveOperatorId, effectiveOperatorName, eventTime);
        addConsumableUsage(state, plan, operation, grindingStage, detailId, zero(changeLength),
                effectiveOperatorId, effectiveOperatorName, eventTime);
        return new ConsumableReportResult(state, List.of());
    }

    private SandpaperUsageSnapshot buildSandpaperUsageSnapshot(int segmentNo, HcEquipmentConsumableStateDO state) {
        return new SandpaperUsageSnapshot(segmentNo, state.getBatchNo(), state.getUsedLength(), state.getLastReplaceTime());
    }

    private record ConsumableReportResult(HcEquipmentConsumableStateDO state,
                                          List<SandpaperUsageSnapshot> sandpaperSegments) {
    }

    private record SandpaperUsageSnapshot(Integer sourceSegmentNo, String batchNo,
                                          BigDecimal cumulativeLife, LocalDateTime lastReplaceTime) {
    }

    private void addConsumableUsage(HcEquipmentConsumableStateDO state, HcPlanOrderDO plan,
                                    HcPlanOrderOperationDO operation, String grindingStage, Long detailId,
                                    BigDecimal changeLength, Long operatorId, String operatorName,
                                    LocalDateTime eventTime) {
        String beforeBatchNo = state.getBatchNo();
        Integer beforeCount = state.getUseCount();
        BigDecimal beforeLength = state.getUsedLength();
        state.setUseCount((state.getUseCount() == null ? 0 : state.getUseCount())
                + consumableUseIncrement(state.getConsumableType()));
        state.setUsedLength(zero(state.getUsedLength()).add(zero(changeLength)));
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus("IN_USE");
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        hcEquipmentConsumableStateMapper.updateById(state);
        insertConsumableEvent(state, plan.getId(), plan.getPlanNo(), operation.getId(), operation.getOpCode(), operation.getOpName(),
                grindingStage, detailId, "USE", beforeBatchNo, state.getBatchNo(), beforeCount, state.getUseCount(),
                beforeLength, state.getUsedLength(), zero(changeLength), null, operatorId, operatorName, eventTime,
                plan.getTenantId());
    }

    private void replaceConsumableBatchForReport(HcEquipmentConsumableStateDO state, HcPlanOrderDO plan,
                                                 HcPlanOrderOperationDO operation, String grindingStage, Long detailId,
                                                 String targetBatchNo, String replaceReason, Long operatorId,
                                                 String operatorName, LocalDateTime eventTime) {
        String beforeBatchNo = state.getBatchNo();
        Integer beforeCount = state.getUseCount();
        BigDecimal beforeLength = state.getUsedLength();
        state.setBatchNo(targetBatchNo);
        state.setLastReplaceTime(eventTime);
        state.setLastReplacePlanNo(plan.getPlanNo());
        state.setLastReplaceReason(replaceReason);
        state.setUseCount(0);
        state.setUsedLength(BigDecimal.ZERO);
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus("IN_USE");
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        hcEquipmentConsumableStateMapper.updateById(state);
        insertConsumableEvent(state, plan.getId(), plan.getPlanNo(), operation.getId(), operation.getOpCode(), operation.getOpName(),
                grindingStage, detailId, "REPLACE", beforeBatchNo, state.getBatchNo(), beforeCount, state.getUseCount(),
                beforeLength, state.getUsedLength(), BigDecimal.ZERO, replaceReason, operatorId, operatorName, eventTime,
                plan.getTenantId());
    }

    private void backfillConsumableEventBizId(HcEquipmentConsumableStateDO state, Long detailId, String grindingStage) {
        if (state == null || detailId == null) {
            return;
        }
        List<HcEquipmentConsumableEventDO> events = hcEquipmentConsumableEventMapper.selectRecentByStateId(state.getId(), 5);
        for (HcEquipmentConsumableEventDO event : events) {
            if (List.of("USE", "REPLACE").contains(event.getEventType()) && grindingStage.equals(event.getGrindingStage())
                    && event.getGrindingDetailId() == null) {
                event.setGrindingDetailId(detailId);
                event.setBizType("GRINDING_" + grindingStage);
                event.setBizId(detailId);
                hcEquipmentConsumableEventMapper.updateById(event);
            }
        }
    }

    private Long insertConsumableEvent(HcEquipmentConsumableStateDO state, Long planId, String planNo, Long planOperationId,
                                       String operationCode, String operationName, String grindingStage, Long detailId,
                                       String eventType, String beforeBatchNo, String afterBatchNo, Integer beforeCount,
                                       Integer afterCount, BigDecimal beforeLength, BigDecimal afterLength,
                                       BigDecimal changeLength, String replaceReason, Long operatorId,
                                       String operatorName, LocalDateTime eventTime, Long tenantId) {
        HcEquipmentConsumableEventDO event = HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(PROCESS_CODE)
                .processName(PROCESS_NAME)
                .consumableType(state.getConsumableType())
                .eventType(eventType)
                .planId(planId)
                .planNo(planNo)
                .planOperationId(planOperationId)
                .operationCode(operationCode)
                .operationName(operationName)
                .grindingStage(grindingStage)
                .grindingDetailId(detailId)
                .bizType(detailId == null ? null : "GRINDING_" + grindingStage)
                .bizId(detailId)
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(afterBatchNo)
                .beforeUseCount(beforeCount)
                .afterUseCount(afterCount)
                .changeUseCount("USE".equals(eventType) ? countDelta(beforeCount, afterCount) : null)
                .beforeUsedLength(beforeLength)
                .afterUsedLength(afterLength)
                .changeLength(changeLength)
                .replaceReason(replaceReason)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .tenantId(tenantId)
                .build();
        hcEquipmentConsumableEventMapper.insert(event);
        return event.getId();
    }

    private void bindFirstSegmentTimings(HcGrindingFirstDetailDO detail,
                                         HcPlanOrderDO plan,
                                         HcPlanOrderOperationDO operation) {
        String motherBatchNo = resolveTimingMotherBatchNo(plan, operation);
        List<HcGrindingSegmentTimingDO> timings = hcGrindingSegmentTimingMapper.selectListByPlanOperationId(
                operation.getId(), motherBatchNo, PASS_TYPE_FIRST);
        Map<String, HcGrindingSegmentTimingDO> timingBySegment = timings.stream()
                .collect(Collectors.toMap(HcGrindingSegmentTimingDO::getSegmentMark, Function.identity(), (left, right) -> left));
        for (String segmentMark : GRINDING_SEGMENT_MARKS) {
            HcGrindingSegmentTimingDO timing = timingBySegment.get(segmentMark);
            if (timing == null) {
                hcGrindingSegmentTimingMapper.insert(buildSegmentTiming(plan, operation, motherBatchNo, PASS_TYPE_FIRST,
                        segmentMark, detail.getId(), null));
                continue;
            }
            timing.setSegmentBatchNo(buildFirstSegmentBatchNo(motherBatchNo, segmentMark));
            timing.setFirstDetailId(detail.getId());
            hcGrindingSegmentTimingMapper.updateById(timing);
        }
    }

    private void bindSecondSegmentTiming(HcGrindingSecondDetailDO detail,
                                         HcPlanOrderDO plan,
                                         HcPlanOrderOperationDO operation) {
        String motherBatchNo = resolveTimingMotherBatchNo(plan, operation);
        String segmentMark = normalizeSecondTimingSegmentMark(detail.getSegmentMark());
        if (segmentMark == null) {
            return;
        }
        HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(operation.getId(),
                motherBatchNo, PASS_TYPE_SECOND, segmentMark);
        if (timing == null) {
            hcGrindingSegmentTimingMapper.insert(buildSegmentTiming(plan, operation, motherBatchNo, PASS_TYPE_SECOND,
                    segmentMark, null, detail.getId()));
            return;
        }
        timing.setSecondDetailId(detail.getId());
        hcGrindingSegmentTimingMapper.updateById(timing);
        if (timing.getStartTime() != null) {
            backfillSecondReportStartTime(timing, timing.getStartTime());
        }
        if (timing.getEndTime() != null) {
            backfillSecondReportEndTime(timing, timing.getEndTime());
        }
    }

    /**
     * 二磨分段开工是该分段报工开始时间的权威来源；若分段报工已保存，则在首次开工打点时同步回填。
     */
    private void backfillSecondReportStartTime(HcGrindingSegmentTimingDO timing, LocalDateTime startTime) {
        if (timing.getSecondDetailId() == null) {
            return;
        }
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(timing.getSecondDetailId());
        if (detail == null || !Objects.equals(detail.getPlanOperationId(), timing.getPlanOperationId())) {
            return;
        }
        validateReportTimeRange(startTime, detail.getEndTime(), "第二次磨皮报工");
        LocalDate reportDate = effectiveReportDate(detail.getReportDate(), startTime);
        int updated = hcGrindingSecondDetailMapper.updateReportTime(detail.getId(), reportDate, startTime, detail.getEndTime());
        if (updated <= 0) {
            throw invalidParamException("第二次磨皮报工开始时间回填失败");
        }
    }

    /**
     * 二磨分段完工是该分段报工结束时间的权威来源；若分段报工已保存，则在首次完工打点时同步回填。
     */
    private void backfillSecondReportEndTime(HcGrindingSegmentTimingDO timing, LocalDateTime endTime) {
        if (timing.getSecondDetailId() == null) {
            return;
        }
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(timing.getSecondDetailId());
        if (detail == null || !Objects.equals(detail.getPlanOperationId(), timing.getPlanOperationId())) {
            return;
        }
        if (detail.getStartTime() == null) {
            log.warn("[磨皮分段完工] 二磨报工开始时间为空，跳过结束时间回填：timingId={}, detailId={}",
                    timing.getId(), detail.getId());
            return;
        }
        LocalDate reportDate = effectiveReportDate(detail.getReportDate(), detail.getStartTime());
        int updated = hcGrindingSecondDetailMapper.updateReportTime(detail.getId(), reportDate,
                detail.getStartTime(), endTime);
        if (updated <= 0) {
            throw invalidParamException("第二次磨皮报工结束时间回填失败");
        }
        hcGrindingProductionRecordLedgerService.syncAutoRecordTime(PASS_TYPE_SECOND, detail.getId(), endTime);
    }

    /** 将二磨报工时间修订同步回同一分段的时间事实，避免 QTIME 读取旧打点。 */
    private void syncSecondSegmentTimingReportTime(HcGrindingSecondDetailDO detail,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime) {
        String segmentMark = normalizeSecondTimingSegmentMark(detail.getSegmentMark());
        String motherBatchNo = firstNotBlank(detail.getMotherBatchNo(), detail.getParentProductionBatchNo());
        if (segmentMark == null || StrUtil.isBlank(motherBatchNo)) {
            return;
        }
        HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(detail.getPlanOperationId(),
                motherBatchNo, PASS_TYPE_SECOND, segmentMark);
        if (timing == null) {
            return;
        }
        timing.setSecondDetailId(detail.getId());
        timing.setStartTime(startTime);
        timing.setEndTime(endTime);
        hcGrindingSegmentTimingMapper.updateById(timing);
    }

    private HcGrindingSegmentTimingDO buildSegmentTiming(HcPlanOrderDO plan,
                                                          HcPlanOrderOperationDO operation,
                                                          String motherBatchNo,
                                                          String passType,
                                                          String segmentMark,
                                                          Long firstDetailId,
                                                          Long secondDetailId) {
        return HcGrindingSegmentTimingDO.builder()
                .planId(plan.getId())
                .planNo(plan.getPlanNo())
                .planOperationId(operation.getId())
                .motherBatchNo(motherBatchNo)
                .passType(passType)
                .segmentMark(segmentMark)
                .segmentBatchNo(PASS_TYPE_FIRST.equals(passType)
                        ? buildFirstSegmentBatchNo(motherBatchNo, segmentMark) : "")
                .firstDetailId(firstDetailId)
                .secondDetailId(secondDetailId)
                .tenantId(plan.getTenantId())
                .build();
    }

    private String buildFirstSegmentBatchNo(String motherBatchNo, String segmentMark) {
        String normalizedMark = normalizeTimingSegmentMark(segmentMark);
        return StrUtil.trimToEmpty(motherBatchNo) + normalizedMark;
    }

    private String resolveTimingMotherBatchNo(HcPlanOrderDO plan, HcPlanOrderOperationDO operation) {
        return firstNotBlank(operation.getProductionBatchNo(), operation.getBatchNo(), plan.getProductionBatchNo(), plan.getBatchNo());
    }

    private String normalizeTimingPassType(String passType) {
        String normalized = StrUtil.trimToEmpty(passType).toUpperCase();
        if (PASS_TYPE_FIRST.equals(normalized) || "1".equals(normalized)) {
            return PASS_TYPE_FIRST;
        }
        if (PASS_TYPE_SECOND.equals(normalized) || "2".equals(normalized)) {
            return PASS_TYPE_SECOND;
        }
        throw invalidParamException("磨皮次数只能是 FIRST 或 SECOND");
    }

    private String normalizeTimingSegmentMark(String segmentMark) {
        return normalizeTimingSegmentMark(segmentMark, PASS_TYPE_FIRST);
    }

    private String normalizeTimingSegmentMark(String segmentMark, String passType) {
        String normalized = StrUtil.trimToEmpty(segmentMark).toUpperCase();
        if (!SECOND_TIMING_SEGMENT_MARKS.contains(normalized)) {
            throw invalidParamException((PASS_TYPE_FIRST.equals(passType) ? "一次磨皮" : "二次磨皮")
                    + "时间记录对象只能是 P、Q、R、S 或 NONE（不分段）");
        }
        return normalized;
    }

    /** 二磨报工明细中的空分段以 NONE 作为独立时间事实键；历史异常值仍保持忽略。 */
    private String normalizeSecondTimingSegmentMark(String segmentMark) {
        String normalized = StrUtil.trimToEmpty(segmentMark).toUpperCase();
        if (StrUtil.isBlank(normalized) || ALLOCATION_SEGMENT_NONE.equals(normalized)) {
            return ALLOCATION_SEGMENT_NONE;
        }
        return GRINDING_SEGMENT_MARKS.contains(normalized) ? normalized : null;
    }

    private String getTimingDisplayName(String passType, String segmentMark) {
        String segmentName = ALLOCATION_SEGMENT_NONE.equals(segmentMark) ? "不分段" : segmentMark + "段";
        return (PASS_TYPE_FIRST.equals(passType) ? "一次磨皮" : "二次磨皮") + segmentName;
    }

    private void fillFirstDetail(HcGrindingFirstDetailDO detail, HcRoughConsoleFirstReportSaveReqVO reqVO,
                                 HcPlanOrderDO plan, HcPlanOrderOperationDO operation, HcEquipmentDO equipment,
                                 BigDecimal availableBefore, BigDecimal availableAfter,
                                 HcEquipmentConsumableStateDO sandpaper, HcEquipmentConsumableStateDO guideCloth) {
        detail.setPlanId(plan.getId());
        detail.setPlanNo(plan.getPlanNo());
        detail.setPlanOperationId(operation.getId());
        detail.setSourceType(firstNotBlank(reqVO.getSourceType(), "PREVIOUS"));
        detail.setMotherBatchNo(reqVO.getMotherBatchNo());
        detail.setSourcePlanNo(reqVO.getSourcePlanNo());
        detail.setSourcePlanId(reqVO.getSourcePlanId());
        detail.setSourcePlanOperationId(reqVO.getSourcePlanOperationId());
        detail.setSourceProductionBatchNo(reqVO.getSourceProductionBatchNo());
        detail.setRemainStartMeter(reqVO.getRemainStartMeter());
        detail.setRemainLength(reqVO.getRemainLength());
        detail.setProcessLength(zero(reqVO.getProcessLength()));
        detail.setLossLength(zero(reqVO.getLossLength()));
        detail.setOutputLength(zero(reqVO.getOutputLength()));
        detail.setNapSampleLength(zero(reqVO.getNapSampleLength()));
        detail.setGrindingPass("1");
        LocalDateTime startTime = normalizeOptionalReportDateTime(reqVO.getStartTime(), null);
        LocalDateTime endTime = normalizeOptionalReportDateTime(reqVO.getEndTime(), null);
        if (startTime == null || endTime == null) {
            throw invalidParamException("一次磨皮报工开始时间和结束时间不能为空");
        }
        LocalDate reportDate = effectiveReportDate(reqVO.getReportDate(), startTime);
        detail.setReportDate(reportDate);
        detail.setStartTime(startTime);
        detail.setEndTime(endTime);
        detail.setSandpaperBatchNo(firstNotBlank(reqVO.getSandpaperBatchNo(), sandpaper == null ? null : sandpaper.getBatchNo()));
        detail.setCurrentSandpaperBatchNo(firstNotBlank(reqVO.getCurrentSandpaperBatchNo(), reqVO.getSandpaperBatchNo(),
                sandpaper == null ? null : sandpaper.getBatchNo()));
        detail.setCurrentGuideClothBatchNo(firstNotBlank(reqVO.getCurrentGuideClothBatchNo(), reqVO.getGuideClothBatchNo(),
                guideCloth == null ? null : guideCloth.getBatchNo()));
        detail.setPressure(reqVO.getPressure());
        detail.setLineSpeed(reqVO.getLineSpeed());
        detail.setRotationSpeed(reqVO.getRotationSpeed());
        detail.setMeterCounter(reqVO.getMeterCounter());
        detail.setGrindingThickness(reqVO.getGrindingThickness());
        detail.setAfterGrindingThickness(reqVO.getAfterGrindingThickness());
        detail.setSelfCheck(StrUtil.blankToDefault(reqVO.getSelfCheck(), "OK"));
        detail.setDefectCode(reqVO.getDefectCode());
        detail.setRowStatus("已报工");
        detail.setRemark(reqVO.getRemark());
        detail.setEquipmentId(equipment.getId());
        detail.setEquipmentCode(equipment.getEquipmentCode());
        detail.setEquipmentName(equipment.getEquipmentName());
        detail.setWorkCenterId(equipment.getWorkCenterId());
        detail.setWorkCenterCode(equipment.getWorkCenterCode());
        detail.setWorkCenterName(equipment.getWorkCenterName());
        detail.setAvailableBefore(availableBefore);
        detail.setAvailableAfter(availableAfter);
        detail.setSandpaperStateId(sandpaper == null ? null : sandpaper.getId());
        detail.setGuideClothStateId(guideCloth == null ? null : guideCloth.getId());
        detail.setDetailStatus("SUBMITTED");
    }

    private void validateReportTimeRange(LocalDateTime startTime, LocalDateTime endTime, String reportName) {
        if (startTime == null || endTime == null) {
            throw invalidParamException(reportName + "报工开始时间和结束时间不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw invalidParamException(reportName + "结束时间不能早于开始时间");
        }
    }

    private void fillSecondDetail(HcGrindingSecondDetailDO detail, HcRoughConsoleSecondReportSaveReqVO reqVO,
                                  HcPlanOrderDO plan, HcPlanOrderOperationDO operation, HcEquipmentDO equipment,
                                  HcEquipmentConsumableStateDO sandpaper, HcEquipmentConsumableStateDO guideCloth) {
        detail.setFirstDetailId(reqVO.getFirstDetailId());
        detail.setFirstAllocationId(reqVO.getFirstAllocationId());
        detail.setPlanId(plan.getId());
        detail.setPlanNo(plan.getPlanNo());
        detail.setPlanOperationId(operation.getId());
        detail.setMotherBatchNo(reqVO.getMotherBatchNo());
        detail.setProductionBatchNo(reqVO.getProductionBatchNo());
        detail.setParentProductionBatchNo(reqVO.getParentProductionBatchNo());
        detail.setSourceProductionBatchNo(reqVO.getSourceProductionBatchNo());
        detail.setSegmentMark(reqVO.getSegmentMark());
        detail.setProcessLength(zero(reqVO.getProcessLength()));
        detail.setLossLength(zero(reqVO.getLossLength()));
        detail.setOutputLength(zero(reqVO.getOutputLength()));
        detail.setNapSampleLength(zero(reqVO.getNapSampleLength()));
        detail.setResearchConsumptionLength(zero(reqVO.getResearchConsumptionLength()));
        detail.setStartPosition(reqVO.getStartPosition());
        LocalDateTime startTime = normalizeOptionalReportDateTime(reqVO.getStartTime(), null);
        LocalDateTime endTime = normalizeOptionalReportDateTime(reqVO.getEndTime(), null);
        if (startTime == null || endTime == null) {
            throw invalidParamException("第二次磨皮报工开始时间和结束时间不能为空");
        }
        LocalDate reportDate = effectiveReportDate(reqVO.getReportDate(), startTime);
        detail.setReportDate(reportDate);
        detail.setStartTime(startTime);
        detail.setEndTime(endTime);
        detail.setSandpaperBatchNo(firstNotBlank(reqVO.getSandpaperBatchNo(), sandpaper == null ? null : sandpaper.getBatchNo()));
        detail.setCurrentSandpaperBatchNo(firstNotBlank(reqVO.getCurrentSandpaperBatchNo(), reqVO.getSandpaperBatchNo(),
                sandpaper == null ? null : sandpaper.getBatchNo()));
        detail.setCurrentGuideClothBatchNo(firstNotBlank(reqVO.getCurrentGuideClothBatchNo(), reqVO.getGuideClothBatchNo(),
                guideCloth == null ? null : guideCloth.getBatchNo()));
        detail.setPressure(reqVO.getPressure());
        detail.setLineSpeed(reqVO.getLineSpeed());
        detail.setRotationSpeed(reqVO.getRotationSpeed());
        detail.setMeterCounter(reqVO.getMeterCounter());
        detail.setGrindingThickness(reqVO.getGrindingThickness());
        detail.setAfterGrindingThickness(reqVO.getAfterGrindingThickness());
        detail.setQualityThickness(reqVO.getQualityThickness());
        detail.setQualityWidth(reqVO.getQualityWidth());
        detail.setGrindingMeters(reqVO.getGrindingMeters());
        detail.setSelfCheck(StrUtil.blankToDefault(reqVO.getSelfCheck(), "OK"));
        detail.setDefectCode(reqVO.getDefectCode());
        detail.setRowStatus("已报工");
        detail.setConfirmStatus(StrUtil.blankToDefault(detail.getConfirmStatus(), "UNCONFIRMED"));
        detail.setPrintStatus(StrUtil.blankToDefault(detail.getPrintStatus(), "UNPRINTED"));
        detail.setPrintCount(detail.getPrintCount() == null ? 0 : detail.getPrintCount());
        detail.setRemark(reqVO.getRemark());
        detail.setEquipmentId(equipment.getId());
        detail.setEquipmentCode(equipment.getEquipmentCode());
        detail.setEquipmentName(equipment.getEquipmentName());
        detail.setWorkCenterId(equipment.getWorkCenterId());
        detail.setWorkCenterCode(equipment.getWorkCenterCode());
        detail.setWorkCenterName(equipment.getWorkCenterName());
        detail.setAvailableBefore(reqVO.getAvailableBefore());
        detail.setAvailableAfter(reqVO.getAvailableAfter());
        detail.setSandpaperStateId(sandpaper == null ? null : sandpaper.getId());
        detail.setGuideClothStateId(guideCloth == null ? null : guideCloth.getId());
        detail.setDetailStatus("SUBMITTED");
    }

    /** 生产记录的签名取对应加工单元开工事实，不使用提交人或当前登录人兜底。 */
    private String requireProductionRecordStartOperator(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                                       String passType, String segmentMark) {
        String normalizedMark = PASS_TYPE_FIRST.equals(passType) && MOTHER_BATCH_TIMING_MARK.equals(segmentMark)
                ? MOTHER_BATCH_TIMING_MARK : normalizeTimingSegmentMark(
                        StrUtil.isBlank(segmentMark) ? ALLOCATION_SEGMENT_NONE : segmentMark, passType);
        String motherBatchNo = resolveTimingMotherBatchNo(plan, operation);
        if (StrUtil.isBlank(motherBatchNo)) {
            throw invalidParamException("当前计划缺少母批号，无法匹配磨皮开工人员");
        }
        HcGrindingSegmentTimingDO timing = hcGrindingSegmentTimingMapper.selectBySegment(
                operation.getId(), motherBatchNo, passType, normalizedMark);
        if (timing == null || timing.getStartTime() == null || StrUtil.isBlank(timing.getStartOperatorName())) {
            String displayName = MOTHER_BATCH_TIMING_MARK.equals(normalizedMark)
                    ? "一次磨皮母批" : getTimingDisplayName(passType, normalizedMark);
            throw invalidParamException(displayName + "缺少开工人员，请先完成对应加工单元的开工身份认证");
        }
        return timing.getStartOperatorName().trim();
    }

    private void syncFirstProductionRecord(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                            HcGrindingFirstDetailDO detail,
                                            HcEquipmentConsumableStateDO sandpaper,
                                            List<SandpaperUsageSnapshot> sandpaperSegments,
                                            HcEquipmentConsumableStateDO guideCloth,
                                            HcRoughConsoleFirstReportSaveReqVO reqVO) {
        hcGrindingProductionRecordLedgerService.syncAutoRecords(buildAutoProductionRecords(
                PASS_TYPE_FIRST, PRODUCTION_RECORD_ROLE_FIRST_ORIGINAL, PRODUCTION_RECORD_ROLE_FIRST_ORIGINAL,
                firstNotBlank(detail.getMotherBatchNo(), detail.getSourceProductionBatchNo()), null,
                detail.getRemainStartMeter(), null,
                detail.getId(), detail.getEquipmentId(), detail.getEquipmentCode(), detail.getEquipmentName(),
                firstNotBlank(detail.getMotherBatchNo(), detail.getSourceProductionBatchNo()),
                detail.getProcessLength(), detail.getOutputLength(),
                sandpaper, sandpaperSegments, detail.getSandpaperLife(), detail.getCurrentSandpaperBatchNo(), detail.getSandpaperBatchNo(),
                guideCloth == null ? null : guideCloth.getUseCount(),
                firstNotBlank(guideCloth == null ? null : guideCloth.getBatchNo(), detail.getCurrentGuideClothBatchNo()),
                reqVO.getSandpaperReplaceReason(), reqVO.getGuideClothReplaceReason(),
                requireProductionRecordStartOperator(plan, operation, PASS_TYPE_FIRST, MOTHER_BATCH_TIMING_MARK),
                detail.getEndTime(), detail.getRemark(), plan, operation));
    }

    private void syncFirstAllocationProductionRecord(HcPlanOrderDO plan,
                                                      HcPlanOrderOperationDO operation,
                                                      HcGrindingFirstDetailDO detail,
                                                      HcGrindingFirstAllocationDO allocation,
                                                      HcEquipmentConsumableStateDO sandpaper,
                                                      List<SandpaperUsageSnapshot> sandpaperSegments,
                                                      HcEquipmentConsumableStateDO guideCloth,
                                                      HcRoughConsoleFirstAllocationSaveReqVO reqVO) {
        hcGrindingProductionRecordLedgerService.syncAutoRecords(buildAutoProductionRecords(
                PASS_TYPE_FIRST, PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION, PRODUCTION_RECORD_ROLE_FIRST_ALLOCATION,
                allocation.getMotherBatchNo(), allocation.getSegmentMark(), allocation.getStartPosition(), allocation.getId(),
                detail.getId(), detail.getEquipmentId(), detail.getEquipmentCode(), detail.getEquipmentName(),
                allocation.getProductionBatchNo(), allocation.getConfirmedLength(), allocation.getConfirmedLength(),
                sandpaper, sandpaperSegments, detail.getSandpaperLife(), detail.getCurrentSandpaperBatchNo(),
                detail.getSandpaperBatchNo(), guideCloth == null ? null : guideCloth.getUseCount(),
                firstNotBlank(guideCloth == null ? null : guideCloth.getBatchNo(), detail.getCurrentGuideClothBatchNo()),
                reqVO.getSandpaperReplaceReason(), reqVO.getGuideClothReplaceReason(),
                requireProductionRecordStartOperator(plan, operation, PASS_TYPE_FIRST, allocation.getSegmentMark()),
                detail.getEndTime(), detail.getRemark(), plan, operation));
    }

    private void syncSecondProductionRecord(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                             HcGrindingSecondDetailDO detail,
                                             HcEquipmentConsumableStateDO sandpaper,
                                             List<SandpaperUsageSnapshot> sandpaperSegments,
                                             HcEquipmentConsumableStateDO guideCloth,
                                             HcRoughConsoleSecondReportSaveReqVO reqVO) {
        hcGrindingProductionRecordLedgerService.syncAutoRecords(buildAutoProductionRecords(
                PASS_TYPE_SECOND, PRODUCTION_RECORD_ROLE_SECOND, PRODUCTION_RECORD_ROLE_SECOND,
                detail.getMotherBatchNo(), detail.getSegmentMark(), detail.getStartPosition(), detail.getFirstAllocationId(),
                detail.getId(), detail.getEquipmentId(), detail.getEquipmentCode(), detail.getEquipmentName(),
                firstNotBlank(detail.getProductionBatchNo(), detail.getMotherBatchNo()),
                detail.getProcessLength(), detail.getOutputLength(),
                sandpaper, sandpaperSegments, detail.getSandpaperLife(), detail.getCurrentSandpaperBatchNo(), detail.getSandpaperBatchNo(),
                guideCloth == null ? null : guideCloth.getUseCount(),
                firstNotBlank(guideCloth == null ? null : guideCloth.getBatchNo(), detail.getCurrentGuideClothBatchNo()),
                reqVO.getSandpaperReplaceReason(), reqVO.getGuideClothReplaceReason(),
                requireProductionRecordStartOperator(plan, operation, PASS_TYPE_SECOND, detail.getSegmentMark()),
                detail.getEndTime(), detail.getRemark(), plan, operation));
    }

    private List<HcGrindingProductionRecordDO> buildAutoProductionRecords(String passType,
                                                                             String recordRole,
                                                                             String sourceBizType,
                                                                             String motherBatchNo,
                                                                             String segmentMark,
                                                                             BigDecimal startPosition,
                                                                             Long firstAllocationId,
                                                                             Long sourceDetailId,
                                                                             Long equipmentId, String equipmentCode, String equipmentName,
                                                                             String batchNo,
                                                                             BigDecimal inputLength, BigDecimal outputLength,
                                                                             HcEquipmentConsumableStateDO sandpaper,
                                                                             List<SandpaperUsageSnapshot> sandpaperSegments,
                                                                             BigDecimal detailSandpaperLife,
                                                                             String currentSandpaperBatchNo,
                                                                             String reportedSandpaperBatchNo,
                                                                             Integer guideClothLife,
                                                                             String guideClothBatchNo,
                                                                             String sandpaperReplaceReason,
                                                                             String guideClothReplaceReason,
                                                                             String recorderName, LocalDateTime recordTime,
                                                                             String remark, HcPlanOrderDO plan,
                                                                             HcPlanOrderOperationDO operation) {
        List<SandpaperUsageSnapshot> effectiveSegments = sandpaperSegments == null || sandpaperSegments.isEmpty()
                ? List.of(new SandpaperUsageSnapshot(1,
                        firstNotBlank(sandpaper == null ? null : sandpaper.getBatchNo(), currentSandpaperBatchNo,
                                reportedSandpaperBatchNo),
                        sandpaper == null ? detailSandpaperLife : sandpaper.getUsedLength(),
                        sandpaper == null ? null : sandpaper.getLastReplaceTime()))
                : sandpaperSegments;
        return effectiveSegments.stream().map(segment -> buildAutoProductionRecord(
                passType, recordRole, sourceBizType, motherBatchNo, segmentMark, startPosition, firstAllocationId,
                sourceDetailId, segment.sourceSegmentNo(), equipmentId, equipmentCode, equipmentName,
                batchNo, inputLength, outputLength,
                segment.cumulativeLife(), calculateSandpaperLifeDays(segment.lastReplaceTime(), recordTime), segment.batchNo(),
                guideClothLife, guideClothBatchNo, sandpaperReplaceReason, guideClothReplaceReason, recorderName,
                recordTime, remark, plan, operation)).toList();
    }

    private HcGrindingProductionRecordDO buildAutoProductionRecord(String passType,
                                                                      String recordRole,
                                                                      String sourceBizType,
                                                                      String motherBatchNo,
                                                                      String segmentMark,
                                                                      BigDecimal startPosition,
                                                                      Long firstAllocationId,
                                                                      Long sourceDetailId, Integer sourceSegmentNo,
                                                                      Long equipmentId, String equipmentCode, String equipmentName,
                                                                      String batchNo,
                                                                      BigDecimal inputLength, BigDecimal outputLength,
                                                                      BigDecimal detailSandpaperLife,
                                                                      Integer sandpaperLifeDays,
                                                                      String sandpaperBatchNo,
                                                                      Integer guideClothLife,
                                                                      String guideClothBatchNo,
                                                                      String sandpaperReplaceReason,
                                                                      String guideClothReplaceReason,
                                                                      String recorderName, LocalDateTime recordTime,
                                                                      String remark, HcPlanOrderDO plan,
                                                                      HcPlanOrderOperationDO operation) {
        return HcGrindingProductionRecordDO.builder()
                .reportDate(recordTime == null ? null : recordTime.toLocalDate())
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .equipmentName(equipmentName)
                .modelCode(firstNotBlank(plan.getMotherModelCode(), operation.getMotherModelCode(), plan.getModelCode()))
                .materialCode(firstNotBlank(plan.getMotherMaterialCode(), operation.getMotherMaterialCode(), plan.getMaterialCode()))
                .recordRole(recordRole)
                .sourceBizType(sourceBizType)
                .motherBatchNo(motherBatchNo)
                .batchNo(batchNo)
                .segmentMark(segmentMark)
                .startPosition(startPosition)
                .firstAllocationId(firstAllocationId)
                .inputLength(inputLength)
                .outputLength(outputLength)
                .passType(passType)
                .sandpaperLife(detailSandpaperLife)
                .sandpaperLifeDays(sandpaperLifeDays)
                .sandpaperBatchNo(sandpaperBatchNo)
                .guideClothLife(guideClothLife)
                .guideClothBatchNo(guideClothBatchNo)
                .replaceReason(buildProductionRecordReplaceReason(sandpaperReplaceReason, guideClothReplaceReason))
                .recorderName(recorderName)
                .recordTime(recordTime)
                .sourceDetailId(sourceDetailId)
                .sourceSegmentNo(sourceSegmentNo)
                .remark(remark)
                .tenantId(plan.getTenantId())
                .build();
    }

    private Integer calculateSandpaperLifeDays(LocalDateTime lastReplaceTime, LocalDateTime completionTime) {
        if (lastReplaceTime == null || completionTime == null
                || lastReplaceTime.getYear() < 2000 || completionTime.getYear() < 2000) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(lastReplaceTime.toLocalDate(), completionTime.toLocalDate());
        return days < 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, days + 1);
    }

    private String buildProductionRecordReplaceReason(String sandpaperReplaceReason, String guideClothReplaceReason) {
        List<String> reasons = new ArrayList<>();
        if (StrUtil.isNotBlank(sandpaperReplaceReason)) {
            reasons.add("砂纸：" + StrUtil.trim(sandpaperReplaceReason));
        }
        if (StrUtil.isNotBlank(guideClothReplaceReason)) {
            reasons.add("导布：" + StrUtil.trim(guideClothReplaceReason));
        }
        return reasons.isEmpty() ? null : StrUtil.join("；", reasons);
    }

    private HcProcessReportDO getLatestStartReport(Long planOperationId) {
        return hcProcessReportMapper.selectOne(new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getDeleted, false)
                .eq(HcProcessReportDO::getPlanOperationId, planOperationId)
                .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE_ROUGH)
                .eq(HcProcessReportDO::getReportType, REPORT_TYPE_START)
                .orderByDesc(HcProcessReportDO::getId)
                .last("LIMIT 1"));
    }

    private String buildRoughTaskNo(HcPlanOrderDO plan, HcPlanOrderOperationDO operation) {
        return firstNotBlank(plan.getPlanNo(), String.valueOf(plan.getId())) + "-"
                + String.format("%02d", operation.getOpSeq() == null ? 0 : operation.getOpSeq());
    }

    private void validateRoughFaiReleased(HcPlanOrderDO plan, HcPlanOrderOperationDO operation) {
        QmsFaiOrderDO latestFai = qmsFaiOrderMapper.selectLatestBySourceReportNo(buildRoughTaskNo(plan, operation),
                SOURCE_MENU_CODE_ROUGH);
        HcProcessReportDO latestStartReport = getLatestStartReport(operation.getId());
        if (latestFai == null && latestStartReport != null && latestStartReport.getFaiId() != null) {
            latestFai = qmsFaiOrderMapper.selectById(latestStartReport.getFaiId());
        }
        if (latestFai == null) {
            throw invalidParamException("首检未提交，不能提交磨皮完工");
        }
        syncRoughReportFaiSummary(latestStartReport, latestFai);
        if (pickQualificationService.isQualified(latestFai, latestFai.getProductBatchNo())) return;
        if (FAI_STATUS_REJECTED.equals(latestFai.getStatus())) {
            throw invalidParamException("首检已驳回，需调整后重新提交首检申请");
        }
        if (!FAI_STATUS_COMPLETED.equals(latestFai.getStatus()) || !FAI_JUDGMENT_OK.equals(latestFai.getJudgment())) {
            throw invalidParamException("首检未完成或未放行，不能提交磨皮完工");
        }
    }

    private boolean isFaiReapplyAllowed(QmsFaiOrderDO faiOrder) {
        return faiOrder != null && (FAI_STATUS_REJECTED.equals(faiOrder.getStatus()) || FAI_STATUS_CANCELED.equals(faiOrder.getStatus()));
    }

    private boolean isFaiReleased(QmsFaiOrderDO faiOrder) {
        return faiOrder != null && ((FAI_STATUS_COMPLETED.equals(faiOrder.getStatus()) && FAI_JUDGMENT_OK.equals(faiOrder.getJudgment()))
                || pickQualificationService.isQualified(faiOrder, faiOrder.getProductBatchNo()));
    }

    private void syncRoughReportFaiSummary(HcProcessReportDO roughReport, QmsFaiOrderDO faiOrder) {
        if (roughReport == null || faiOrder == null || faiOrder.getId() == null) {
            return;
        }
        HcProcessReportDO updateObj = new HcProcessReportDO();
        updateObj.setId(roughReport.getId());
        updateObj.setFaiId(faiOrder.getId());
        updateObj.setFaiNo(faiOrder.getFaiNo());
        updateObj.setFaiStatus(faiOrder.getStatus());
        updateObj.setFaiJudgment(faiOrder.getJudgment());
        updateObj.setFaiStandardId(faiOrder.getStandardId());
        updateObj.setFaiStandardNo(faiOrder.getStandardNo());
        updateObj.setFaiApplyTime(firstNotNullDateTime(roughReport.getFaiApplyTime(), faiOrder.getSubmissionTime(),
                faiOrder.getCreateTime(), LocalDateTime.now()));
        updateObj.setFaiReturnTime(firstNotNullDateTime(faiOrder.getUpdateTime(), LocalDateTime.now()));
        updateObj.setFaiRejectReason(faiOrder.getLastReturnReason());
        hcProcessReportMapper.updateById(updateObj);

        roughReport.setFaiId(updateObj.getFaiId());
        roughReport.setFaiNo(updateObj.getFaiNo());
        roughReport.setFaiStatus(updateObj.getFaiStatus());
        roughReport.setFaiJudgment(updateObj.getFaiJudgment());
        roughReport.setFaiStandardId(updateObj.getFaiStandardId());
        roughReport.setFaiStandardNo(updateObj.getFaiStandardNo());
        roughReport.setFaiApplyTime(updateObj.getFaiApplyTime());
        roughReport.setFaiReturnTime(updateObj.getFaiReturnTime());
        roughReport.setFaiRejectReason(updateObj.getFaiRejectReason());
    }

    private HcRoughFaiRespVO buildRoughFaiResp(QmsFaiOrderDO faiOrder, HcProcessReportDO roughReport) {
        HcRoughFaiRespVO respVO = new HcRoughFaiRespVO();
        if (faiOrder != null) {
            respVO.setFaiId(faiOrder.getId());
            respVO.setFaiNo(faiOrder.getFaiNo());
            respVO.setFaiStatus(faiOrder.getStatus());
            respVO.setFaiJudgment(faiOrder.getJudgment());
            respVO.setFaiStandardId(faiOrder.getStandardId());
            respVO.setFaiStandardNo(faiOrder.getStandardNo());
            respVO.setFaiStandardVersion(faiOrder.getStandardVersion());
            respVO.setFaiApplyTime(firstNotNullDateTime(roughReport == null ? null : roughReport.getFaiApplyTime(),
                    faiOrder.getSubmissionTime(), faiOrder.getCreateTime()));
            respVO.setFaiReturnTime(firstNotNullDateTime(faiOrder.getUpdateTime(),
                    roughReport == null ? null : roughReport.getFaiReturnTime()));
            respVO.setFaiRejectReason(firstNotBlank(faiOrder.getLastReturnReason(),
                    roughReport == null ? null : roughReport.getFaiRejectReason()));
            respVO.setAllowReportSubmit(isFaiReleased(faiOrder));
        } else if (roughReport != null) {
            respVO.setFaiId(roughReport.getFaiId());
            respVO.setFaiNo(roughReport.getFaiNo());
            respVO.setFaiStatus(roughReport.getFaiStatus());
            respVO.setFaiJudgment(roughReport.getFaiJudgment());
            respVO.setFaiStandardId(roughReport.getFaiStandardId());
            respVO.setFaiStandardNo(roughReport.getFaiStandardNo());
            respVO.setFaiApplyTime(roughReport.getFaiApplyTime());
            respVO.setFaiReturnTime(roughReport.getFaiReturnTime());
            respVO.setFaiRejectReason(roughReport.getFaiRejectReason());
            respVO.setAllowReportSubmit(FAI_STATUS_COMPLETED.equals(roughReport.getFaiStatus()) && FAI_JUDGMENT_OK.equals(roughReport.getFaiJudgment()));
        } else {
            respVO.setAllowReportSubmit(false);
        }
        respVO.setDisplayText(resolveRoughFaiDisplayText(respVO.getFaiStatus(), respVO.getFaiJudgment()));
        if (faiOrder != null && pickQualificationService.isQualified(faiOrder, faiOrder.getProductBatchNo())) {
            respVO.setFaiJudgment("OK");
            respVO.setDisplayText("合格（NCR挑选后，原始检验NG）");
        }
        return respVO;
    }

    private String resolveRoughFaiDisplayText(String status, String judgment) {
        if (FAI_STATUS_COMPLETED.equals(status) && FAI_JUDGMENT_OK.equals(judgment)) {
            return "已完成";
        }
        if (FAI_STATUS_REJECTED.equals(status)) {
            return "已驳回";
        }
        if (FAI_STATUS_CANCELED.equals(status)) {
            return "已取消";
        }
        if (FAI_STATUS_PENDING.equals(status)) {
            return "待检测";
        }
        return StrUtil.isBlank(status) ? "未提交" : status;
    }

    private HcGrindingSecondDetailDO validateSecondDetail(Long secondDetailId) {
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(secondDetailId);
        if (detail == null || Boolean.TRUE.equals(detail.getDeleted())) {
            throw invalidParamException("第二次磨皮记录不存在");
        }
        return detail;
    }

    private QmsFaiOrderDO selectLatestSecondSegmentFai(Long secondDetailId) {
        return qmsFaiOrderMapper.selectOne(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getSourceReportId, secondDetailId)
                .eq(QmsFaiOrderDO::getSourceModule, SOURCE_MENU_CODE_ROUGH_SECOND_SEGMENT)
                .eq(QmsFaiOrderDO::getDeleted, false)
                .orderByDesc(QmsFaiOrderDO::getId)
                .last("LIMIT 1"));
    }

    private void refreshSecondSegmentInspectionSummaries(List<HcGrindingSecondDetailDO> secondDetails) {
        if (secondDetails == null || secondDetails.isEmpty()) {
            return;
        }
        for (HcGrindingSecondDetailDO detail : secondDetails) {
            if (detail == null || detail.getId() == null) {
                continue;
            }
            QmsFaiOrderDO latestFai = selectLatestSecondSegmentFai(detail.getId());
            if (latestFai != null) {
                syncSecondSegmentInspectionSummary(detail, latestFai);
            }
        }
    }

    private String buildSecondSegmentInspectionSourceNo(HcGrindingSecondDetailDO detail, HcPlanOrderDO plan,
                                                        HcPlanOrderOperationDO operation) {
        return firstNotBlank(plan.getPlanNo(), String.valueOf(plan.getId())) + "-"
                + String.format("%02d", operation.getOpSeq() == null ? 0 : operation.getOpSeq())
                + "-S2-" + (StrUtil.isBlank(detail.getSegmentMark()) ? "NONE" : detail.getSegmentMark())
                + "-" + detail.getId();
    }

    private String buildSecondSegmentInspectionRemark(HcGrindingSecondDetailDO detail, String motherBatchNo, String userRemark) {
        String remark = "磨皮二次分段留样送检；加工母批号：" + firstNotBlank(motherBatchNo, "-")
                + "；受检生产批次号：" + firstNotBlank(detail.getProductionBatchNo(), "-")
                + "；分段：" + firstNotBlank(detail.getSegmentMark(), "不分段")
                + "；送检米数：" + toPlain(zero(detail.getNapSampleLength())) + "m"
                + "；样品类型：" + FAI_SAMPLE_TYPE_SECOND_SEGMENT_NAME;
        return StrUtil.isBlank(userRemark) ? remark : remark + "；备注：" + StrUtil.trim(userRemark);
    }

    private void syncSecondSegmentSampleLength(HcGrindingSecondDetailDO detail, BigDecimal sampleLength) {
        if (detail == null || detail.getId() == null) {
            return;
        }
        BigDecimal safeSampleLength = zero(sampleLength);
        BigDecimal nextOutputLength = calculateSecondOutputLength(detail.getProcessLength(), detail.getLossLength(),
                safeSampleLength, detail.getResearchConsumptionLength(), sumPersistedSecondAbnormalLength(detail.getId()));
        HcGrindingSecondDetailDO updateObj = new HcGrindingSecondDetailDO();
        updateObj.setId(detail.getId());
        updateObj.setNapSampleLength(safeSampleLength);
        updateObj.setOutputLength(nextOutputLength);
        hcGrindingSecondDetailMapper.updateById(updateObj);
        detail.setNapSampleLength(safeSampleLength);
        detail.setOutputLength(nextOutputLength);
    }

    private void syncSecondSegmentInspectionSummary(HcGrindingSecondDetailDO detail, QmsFaiOrderDO faiOrder) {
        if (detail == null || faiOrder == null || faiOrder.getId() == null) {
            return;
        }
        LocalDateTime returnTime = isFaiTerminalStatus(faiOrder.getStatus())
                ? firstNotNullDateTime(faiOrder.getUpdateTime(), LocalDateTime.now()) : null;
        LocalDateTime applyTime = firstNotNullDateTime(detail.getInspectionApplyTime(), faiOrder.getSubmissionTime(),
                faiOrder.getCreateTime(), LocalDateTime.now());
        if (Objects.equals(detail.getInspectionId(), faiOrder.getId())
                && Objects.equals(detail.getInspectionNo(), faiOrder.getFaiNo())
                && Objects.equals(detail.getInspectionStatus(), faiOrder.getStatus())
                && Objects.equals(detail.getInspectionResult(), faiOrder.getJudgment())
                && Objects.equals(detail.getInspectionApplyTime(), applyTime)
                && Objects.equals(detail.getInspectionReturnTime(), returnTime)
                && Objects.equals(detail.getInspectionRejectReason(), faiOrder.getLastReturnReason())) {
            return;
        }
        HcGrindingSecondDetailDO updateObj = new HcGrindingSecondDetailDO();
        updateObj.setId(detail.getId());
        updateObj.setInspectionId(faiOrder.getId());
        updateObj.setInspectionNo(faiOrder.getFaiNo());
        updateObj.setInspectionStatus(faiOrder.getStatus());
        updateObj.setInspectionResult(faiOrder.getJudgment());
        updateObj.setInspectionApplyTime(applyTime);
        updateObj.setInspectionReturnTime(returnTime);
        updateObj.setInspectionRejectReason(faiOrder.getLastReturnReason());
        hcGrindingSecondDetailMapper.updateById(updateObj);

        detail.setInspectionId(updateObj.getInspectionId());
        detail.setInspectionNo(updateObj.getInspectionNo());
        detail.setInspectionStatus(updateObj.getInspectionStatus());
        detail.setInspectionResult(updateObj.getInspectionResult());
        detail.setInspectionApplyTime(updateObj.getInspectionApplyTime());
        detail.setInspectionReturnTime(updateObj.getInspectionReturnTime());
        detail.setInspectionRejectReason(updateObj.getInspectionRejectReason());
    }

    private HcRoughSecondSegmentInspectionRespVO buildSecondSegmentInspectionResp(HcGrindingSecondDetailDO detail,
                                                                                  QmsFaiOrderDO faiOrder) {
        HcRoughSecondSegmentInspectionRespVO respVO = new HcRoughSecondSegmentInspectionRespVO();
        String motherBatchNo = firstNotBlank(detail.getMotherBatchNo(), detail.getParentProductionBatchNo(),
                detail.getSourceProductionBatchNo());
        respVO.setSecondDetailId(detail.getId());
        respVO.setSourceReportId(detail.getId());
        respVO.setSourceReportNo(faiOrder == null ? null : faiOrder.getSourceReportNo());
        respVO.setSourceModule(SOURCE_MENU_CODE_ROUGH_SECOND_SEGMENT);
        respVO.setProcessCategory(PROCESS_CODE);
        respVO.setSampleType(FAI_SAMPLE_TYPE_SECOND_SEGMENT);
        respVO.setSampleTypeName(FAI_SAMPLE_TYPE_SECOND_SEGMENT_NAME);
        respVO.setSampleLength(faiOrder == null ? detail.getNapSampleLength()
                : firstNonNull(faiOrder.getSampleLength(), detail.getNapSampleLength()));
        respVO.setMotherBatchNo(motherBatchNo);
        respVO.setParentBatchNo(motherBatchNo);
        respVO.setProductBatchNo(detail.getProductionBatchNo());
        respVO.setProductionBatchNo(detail.getProductionBatchNo());
        respVO.setSegmentMark(detail.getSegmentMark());
        if (faiOrder != null) {
            respVO.setInspectionId(faiOrder.getId());
            respVO.setInspectionNo(faiOrder.getFaiNo());
            respVO.setInspectionStatus(faiOrder.getStatus());
            respVO.setInspectionResult(faiOrder.getJudgment());
            respVO.setInspectionApplyTime(firstNotNullDateTime(detail.getInspectionApplyTime(), faiOrder.getSubmissionTime(),
                    faiOrder.getCreateTime()));
            respVO.setInspectionReturnTime(isFaiTerminalStatus(faiOrder.getStatus())
                    ? firstNotNullDateTime(faiOrder.getUpdateTime(), detail.getInspectionReturnTime()) : null);
            respVO.setInspectionRejectReason(firstNotBlank(faiOrder.getLastReturnReason(), detail.getInspectionRejectReason()));
            respVO.setReapplyAllowed(isFaiReapplyAllowed(faiOrder));
            respVO.setDisplayText(resolveSecondSegmentInspectionDisplayText(faiOrder.getStatus(), faiOrder.getJudgment()));
            if (pickQualificationService.isQualified(faiOrder, detail.getProductionBatchNo())) {
                respVO.setInspectionResult("OK");
                respVO.setDisplayText("合格（NCR挑选后，原始检验NG）");
            }
        } else {
            respVO.setInspectionId(detail.getInspectionId());
            respVO.setInspectionNo(detail.getInspectionNo());
            respVO.setInspectionStatus(detail.getInspectionStatus());
            respVO.setInspectionResult(detail.getInspectionResult());
            respVO.setInspectionApplyTime(detail.getInspectionApplyTime());
            respVO.setInspectionReturnTime(detail.getInspectionReturnTime());
            respVO.setInspectionRejectReason(detail.getInspectionRejectReason());
            respVO.setReapplyAllowed(detail.getInspectionId() == null || isFaiReapplyAllowedStatus(detail.getInspectionStatus()));
            respVO.setDisplayText(resolveSecondSegmentInspectionDisplayText(detail.getInspectionStatus(), detail.getInspectionResult()));
        }
        return respVO;
    }

    private boolean isFaiTerminalStatus(String status) {
        return FAI_STATUS_COMPLETED.equals(status) || FAI_STATUS_REJECTED.equals(status) || FAI_STATUS_CANCELED.equals(status);
    }

    private boolean isFaiReapplyAllowedStatus(String status) {
        return FAI_STATUS_REJECTED.equals(status) || FAI_STATUS_CANCELED.equals(status);
    }

    private String resolveSecondSegmentInspectionDisplayText(String status, String judgment) {
        if (StrUtil.isBlank(status)) {
            return "留样送检";
        }
        if (FAI_STATUS_COMPLETED.equals(status) && FAI_JUDGMENT_OK.equals(judgment)) {
            return "合格";
        }
        if (FAI_STATUS_REJECTED.equals(status) || FAI_STATUS_CANCELED.equals(status) || FAI_JUDGMENT_NG.equals(judgment)) {
            return "异常/可重提";
        }
        return "待检";
    }

    private LocalDateTime firstNotNullDateTime(LocalDateTime... values) {
        return firstNonNull(values);
    }

    private HcWetReportAbnormalPositionRespVO buildAbnormalPositionResp(HcWetReportAbnormalPositionDO data) {
        HcWetReportAbnormalPositionRespVO respVO = new HcWetReportAbnormalPositionRespVO();
        respVO.setId(data.getId());
        respVO.setOperationReportId(data.getOperationReportId());
        respVO.setPlanId(data.getPlanId());
        respVO.setPlanOperationId(data.getPlanOperationId());
        respVO.setPlanNo(data.getPlanNo());
        respVO.setSourceMenuCode(data.getSourceMenuCode());
        respVO.setProcessStage(data.getProcessStage());
        respVO.setOperationCode(data.getOperationCode());
        respVO.setOperationName(data.getOperationName());
        respVO.setBatchNo(data.getBatchNo());
        respVO.setProductionBatchNo(data.getProductionBatchNo());
        respVO.setSourcePlanNo(data.getSourcePlanNo());
        respVO.setSourceRowUid(data.getSourceRowUid());
        respVO.setSourceDetailId(data.getSourceDetailId());
        respVO.setPositionText(data.getPositionText());
        respVO.setAbnormalLength(data.getAbnormalLength());
        respVO.setRemark(data.getRemark());
        respVO.setSortOrder(data.getSortOrder());
        respVO.setCreateTime(data.getCreateTime());
        return respVO;
    }

    private String resolveRoughFaiProductModel(HcProcessReportDO report,
                                               HcPlanOrderOperationDO operation,
                                               HcPlanOrderDO plan) {
        return firstNotBlank(
                report == null ? null : report.getMotherModelName(),
                operation == null ? null : operation.getMotherModelName(),
                plan == null ? null : plan.getMotherModelName(),
                plan == null ? null : plan.getModelName(),
                report == null ? null : report.getMotherModelCode(),
                operation == null ? null : operation.getMotherModelCode(),
                plan == null ? null : plan.getMotherModelCode(),
                plan == null ? null : plan.getModelCode());
    }

    private HcPlanOrderDO validatePlan(Long planId) {
        HcPlanOrderDO plan = hcPlanOrderMapper.selectById(planId);
        if (plan == null) {
            throw invalidParamException("生产计划不存在");
        }
        return plan;
    }

    private HcPlanOrderOperationDO validateOperation(HcPlanOrderDO plan, Long planOperationId) {
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(planOperationId);
        if (operation == null || !plan.getId().equals(operation.getPlanId())) {
            throw invalidParamException("计划工序不存在");
        }
        return operation;
    }

    private void assertOperationRunning(HcPlanOrderOperationDO operation) {
        if (OP_STATUS_RUNNING.equals(operation.getOperationStatus())) {
            return;
        }
        if (OP_STATUS_FINISHED.equals(operation.getOperationStatus())) {
            throw invalidParamException("当前工单此工序已完工，不能再报工！");
        }
        throw invalidParamException("当前工单此工序未开工，请先执行开工确认！");
    }

    private HcEquipmentDO validateEquipment(Long equipmentId, HcPlanOrderOperationDO operation) {
        Long actualEquipmentId = equipmentId == null ? operation.getEquipmentId() : equipmentId;
        if (actualEquipmentId == null) {
            throw invalidParamException("请先选择磨皮设备");
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(actualEquipmentId);
        if (equipment == null) {
            throw invalidParamException("设备不存在");
        }
        return equipment;
    }

    private void assertDailyReady(Long equipmentId, LocalDate recordDate) {
        List<HcWetPassWorkRespVO> dailyChecks = getDailyCheckList(equipmentId, recordDate);
        if (!isDailyDone(dailyChecks, FORM_STARTUP)) {
            throw invalidParamException("今天尚未完成磨皮开机点检，不能报工");
        }
        if (!isDailyDone(dailyChecks, FORM_CLEANING)) {
            throw invalidParamException("今天尚未完成磨皮设备清洁点检，不能报工");
        }
    }

    private void assertSecondReportRangeNotOverlap(HcRoughConsoleSecondReportSaveReqVO reqVO) {
        BigDecimal start = zero(reqVO.getStartPosition());
        BigDecimal length = zero(reqVO.getProcessLength());
        if (length.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("第二次磨皮投入米数必须大于0");
        }
        BigDecimal end = start.add(length);
        String currentBatchKey = firstNotBlank(reqVO.getMotherBatchNo(), reqVO.getParentProductionBatchNo(), reqVO.getSourceProductionBatchNo());
        List<HcGrindingSecondDetailDO> existingDetails = hcGrindingSecondDetailMapper.selectListByPlanOperationId(reqVO.getPlanOperationId());
        for (HcGrindingSecondDetailDO existing : existingDetails) {
            if (Objects.equals(existing.getId(), reqVO.getId())) {
                continue;
            }
            String existingBatchKey = firstNotBlank(existing.getMotherBatchNo(), existing.getParentProductionBatchNo(), existing.getSourceProductionBatchNo());
            if (StrUtil.isAllNotBlank(currentBatchKey, existingBatchKey) && !StrUtil.equals(currentBatchKey, existingBatchKey)) {
                continue;
            }
            BigDecimal existingStart = zero(existing.getStartPosition());
            BigDecimal existingLength = zero(existing.getProcessLength());
            if (existingLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal existingEnd = existingStart.add(existingLength);
            if (isRangeOverlap(start, end, existingStart, existingEnd)) {
                throw invalidParamException("第二次磨皮区间" + rangeText(start, end)
                        + "与已报区间" + rangeText(existingStart, existingEnd) + "重叠，不能保存");
            }
        }
    }

    private void assertSecondReportWithinFirstGrindingRange(HcRoughConsoleSecondReportSaveReqVO reqVO) {
        BigDecimal start = zero(reqVO.getStartPosition());
        BigDecimal length = zero(reqVO.getProcessLength());
        if (length.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("第二次磨皮投入米数必须大于0");
        }
        BigDecimal end = start.add(length);
        if (reqVO.getFirstAllocationId() != null) {
            HcGrindingFirstAllocationDO allocation = hcGrindingFirstAllocationMapper.selectById(reqVO.getFirstAllocationId());
            if (allocation == null || Boolean.TRUE.equals(allocation.getDeleted())) {
                throw invalidParamException("所选一磨加工单元不存在或已删除");
            }
            BigDecimal allocationStart = zero(allocation.getStartPosition());
            BigDecimal allocationEnd = allocationStart.add(zero(allocation.getConfirmedLength()));
            if (start.compareTo(allocationStart.subtract(RANGE_EPS)) < 0
                    || end.compareTo(allocationEnd.add(RANGE_EPS)) > 0) {
                throw invalidParamException("第二次磨皮区间" + rangeText(start, end)
                        + "不能超出当前一磨加工区间" + rangeText(allocationStart, allocationEnd));
            }
            return;
        }
        List<HcGrindingFirstDetailDO> firstDetails = hcGrindingFirstDetailMapper.selectListByPlanOperationId(reqVO.getPlanOperationId());
        BigDecimal firstOutputLength = sum(firstDetails, HcGrindingFirstDetailDO::getOutputLength);
        if (firstOutputLength.compareTo(RANGE_EPS) <= 0) {
            firstOutputLength = sum(firstDetails, HcGrindingFirstDetailDO::getProcessLength);
        }
        if (firstOutputLength.compareTo(RANGE_EPS) <= 0) {
            throw invalidParamException("请先完成第一次磨皮报工后，再进行第二次磨皮报工");
        }
        if (end.subtract(firstOutputLength).compareTo(RANGE_EPS) > 0) {
            throw invalidParamException("第二次磨皮区间" + rangeText(start, end)
                    + "不能超出当前一磨产出米数" + zero(firstOutputLength).stripTrailingZeros().toPlainString() + "m");
        }
    }

    private boolean isRangeOverlap(BigDecimal start, BigDecimal end, BigDecimal existingStart, BigDecimal existingEnd) {
        return start.compareTo(existingEnd.subtract(RANGE_EPS)) < 0
                && end.compareTo(existingStart.add(RANGE_EPS)) > 0;
    }

    private String rangeText(BigDecimal start, BigDecimal end) {
        return zero(start).stripTrailingZeros().toPlainString() + "-" + zero(end).stripTrailingZeros().toPlainString() + "m";
    }

    private boolean isDailyDone(List<HcWetPassWorkRespVO> dailyChecks, String formCode) {
        if (dailyChecks == null) {
            return false;
        }
        return dailyChecks.stream()
                .filter(item -> formCode.equals(item.getFormCode()))
                .anyMatch(item -> DOC_STATUS_CONFIRMED.equals(item.getStatus()) || DOC_STATUS_RECORDED.equals(item.getStatus()));
    }

    private HcRoughConsoleConsumableRespVO toConsumableResp(HcEquipmentConsumableStateDO state) {
        if (CONSUMABLE_SANDPAPER.equals(state.getConsumableType())) {
            state.setLimitCount(null);
            state.setLimitLength(DEFAULT_SANDPAPER_LIMIT_LENGTH);
        } else if (CONSUMABLE_GUIDE_CLOTH.equals(state.getConsumableType()) && state.getLimitCount() == null) {
            state.setLimitCount(DEFAULT_GUIDE_CLOTH_LIMIT_COUNT);
        }
        HcRoughConsoleConsumableRespVO respVO = new HcRoughConsoleConsumableRespVO();
        respVO.setId(state.getId());
        respVO.setEquipmentId(state.getEquipmentId());
        respVO.setEquipmentCode(state.getEquipmentCode());
        respVO.setEquipmentName(state.getEquipmentName());
        respVO.setConsumableType(state.getConsumableType());
        respVO.setBatchNo(state.getBatchNo());
        respVO.setLastReplaceTime(state.getLastReplaceTime());
        respVO.setLastReplacePlanNo(state.getLastReplacePlanNo());
        respVO.setLastReplaceReason(state.getLastReplaceReason());
        respVO.setUseCount(state.getUseCount());
        respVO.setUsedLength(state.getUsedLength());
        respVO.setLimitCount(state.getLimitCount());
        respVO.setLimitLength(state.getLimitLength());
        respVO.setWarningFlag(calcWarningFlag(state));
        respVO.setStatus(state.getStatus());
        respVO.setLastOperatorName(state.getLastOperatorName());
        respVO.setLastEventTime(state.getLastEventTime());
        return respVO;
    }

    private HcRoughConsoleSourceBalanceRespVO toSourceBalanceResp(HcGrindingSourceBalanceDO balance) {
        HcRoughConsoleSourceBalanceRespVO respVO = new HcRoughConsoleSourceBalanceRespVO();
        respVO.setId(balance.getId());
        respVO.setBalanceKey(balance.getBalanceKey());
        respVO.setSourceType(balance.getSourceType());
        respVO.setSourcePlanId(balance.getSourcePlanId());
        respVO.setSourcePlanNo(balance.getSourcePlanNo());
        respVO.setSourcePlanOperationId(balance.getSourcePlanOperationId());
        respVO.setSourceBatchNo(balance.getSourceBatchNo());
        respVO.setSourceProductionBatchNo(balance.getSourceProductionBatchNo());
        respVO.setMaterialCode(balance.getMaterialCode());
        respVO.setMaterialName(balance.getMaterialName());
        respVO.setModelCode(balance.getModelCode());
        respVO.setModelName(balance.getModelName());
        respVO.setTotalLength(balance.getTotalLength());
        respVO.setUsedFirstLength(balance.getUsedFirstLength());
        respVO.setUsedSecondLength(balance.getUsedSecondLength());
        respVO.setReservedLength(balance.getReservedLength());
        respVO.setAvailableLength(balance.getAvailableLength());
        respVO.setLastReportTime(balance.getLastReportTime());
        respVO.setStatus(balance.getStatus());
        respVO.setRemark(balance.getRemark());
        return respVO;
    }

    private HcRoughConsoleFirstReportRespVO toFirstReportResp(HcGrindingFirstDetailDO detail) {
        HcRoughConsoleFirstReportRespVO respVO = new HcRoughConsoleFirstReportRespVO();
        respVO.setConsumption(grindingConsumptionService.get("FIRST", detail.getId()));
        respVO.setId(detail.getId());
        respVO.setGrindingReportId(detail.getGrindingReportId());
        respVO.setPlanId(detail.getPlanId());
        respVO.setPlanNo(detail.getPlanNo());
        respVO.setPlanOperationId(detail.getPlanOperationId());
        respVO.setRowUid(detail.getRowUid());
        respVO.setSourceType(detail.getSourceType());
        respVO.setMotherBatchNo(detail.getMotherBatchNo());
        respVO.setSourcePlanNo(detail.getSourcePlanNo());
        respVO.setSourceProductionBatchNo(detail.getSourceProductionBatchNo());
        respVO.setRemainStartMeter(detail.getRemainStartMeter());
        respVO.setRemainLength(detail.getRemainLength());
        respVO.setProcessLength(detail.getProcessLength());
        respVO.setLossLength(detail.getLossLength());
        respVO.setOutputLength(detail.getOutputLength());
        respVO.setNapSampleLength(detail.getNapSampleLength());
        respVO.setAvailableBefore(detail.getAvailableBefore());
        respVO.setAvailableAfter(detail.getAvailableAfter());
        respVO.setReportDate(detail.getReportDate());
        respVO.setStartTime(detail.getStartTime());
        respVO.setEndTime(detail.getEndTime());
        respVO.setSandpaperBatchNo(detail.getSandpaperBatchNo());
        respVO.setPressure(detail.getPressure());
        respVO.setLineSpeed(detail.getLineSpeed());
        respVO.setRotationSpeed(detail.getRotationSpeed());
        respVO.setMeterCounter(detail.getMeterCounter());
        respVO.setGrindingThickness(detail.getGrindingThickness());
        respVO.setAfterGrindingThickness(detail.getAfterGrindingThickness());
        respVO.setSelfCheck(detail.getSelfCheck());
        respVO.setDefectCode(detail.getDefectCode());
        respVO.setDetailStatus(detail.getDetailStatus());
        respVO.setRemark(detail.getRemark());
        respVO.setSegmentTimings(hcGrindingSegmentTimingMapper.selectListByFirstDetailId(detail.getId()).stream()
                .map(this::toSegmentTimingResp)
                .toList());
        return respVO;
    }

    private HcRoughConsoleFirstAllocationRespVO toFirstAllocationResp(HcGrindingFirstAllocationDO allocation) {
        HcRoughConsoleFirstAllocationRespVO respVO = new HcRoughConsoleFirstAllocationRespVO();
        respVO.setConsumption(grindingConsumptionService.get("FIRST", allocation.getFirstDetailId()));
        respVO.setId(allocation.getId());
        respVO.setFirstDetailId(allocation.getFirstDetailId());
        respVO.setSegmentMark(allocation.getSegmentMark());
        respVO.setProductionBatchNo(allocation.getProductionBatchNo());
        respVO.setStartPosition(allocation.getStartPosition());
        respVO.setConfirmedLength(allocation.getConfirmedLength());
        respVO.setStartTime(allocation.getStartTime());
        respVO.setEndTime(allocation.getEndTime());
        respVO.setSandpaperStateId(allocation.getSandpaperStateId());
        respVO.setSandpaperLife(allocation.getSandpaperLife());
        respVO.setSandpaperBatchNo(allocation.getSandpaperBatchNo());
        respVO.setGuideClothStateId(allocation.getGuideClothStateId());
        respVO.setGuideClothLife(allocation.getGuideClothLife());
        respVO.setGuideClothBatchNo(allocation.getGuideClothBatchNo());
        respVO.setCheckRecordId(allocation.getCheckRecordId());
        respVO.setOperatorId(allocation.getOperatorId());
        respVO.setOperatorName(allocation.getOperatorName());
        respVO.setDetailStatus(allocation.getDetailStatus());
        respVO.setRemark(allocation.getRemark());
        return respVO;
    }

    private HcRoughConsoleSegmentTimingRespVO toSegmentTimingResp(HcGrindingSegmentTimingDO timing) {
        HcRoughConsoleSegmentTimingRespVO respVO = new HcRoughConsoleSegmentTimingRespVO();
        respVO.setId(timing.getId());
        respVO.setPassType(timing.getPassType());
        respVO.setSegmentMark(timing.getSegmentMark());
        respVO.setSegmentBatchNo(timing.getSegmentBatchNo());
        respVO.setFirstDetailId(timing.getFirstDetailId());
        respVO.setSecondDetailId(timing.getSecondDetailId());
        respVO.setStartTime(timing.getStartTime());
        respVO.setEndTime(timing.getEndTime());
        respVO.setStartOperatorName(timing.getStartOperatorName());
        respVO.setEndOperatorName(timing.getEndOperatorName());
        return respVO;
    }

    private HcRoughConsoleSecondReportRespVO toSecondReportResp(HcGrindingSecondDetailDO detail) {
        HcRoughConsoleSecondReportRespVO respVO = new HcRoughConsoleSecondReportRespVO();
        respVO.setConsumption(grindingConsumptionService.get("SECOND", detail.getId()));
        respVO.setId(detail.getId());
        respVO.setGrindingReportId(detail.getGrindingReportId());
        respVO.setFirstDetailId(detail.getFirstDetailId());
        respVO.setFirstAllocationId(detail.getFirstAllocationId());
        respVO.setPlanId(detail.getPlanId());
        respVO.setPlanNo(detail.getPlanNo());
        respVO.setPlanOperationId(detail.getPlanOperationId());
        respVO.setRowUid(detail.getRowUid());
        respVO.setSourceRowUid(detail.getSourceRowUid());
        respVO.setMotherBatchNo(detail.getMotherBatchNo());
        respVO.setProductionBatchNo(detail.getProductionBatchNo());
        respVO.setParentProductionBatchNo(detail.getParentProductionBatchNo());
        respVO.setSourceProductionBatchNo(detail.getSourceProductionBatchNo());
        respVO.setSegmentMark(detail.getSegmentMark());
        respVO.setProcessLength(detail.getProcessLength());
        respVO.setLossLength(detail.getLossLength());
        respVO.setOutputLength(detail.getOutputLength());
        respVO.setNapSampleLength(detail.getNapSampleLength());
        respVO.setResearchConsumptionLength(zero(detail.getResearchConsumptionLength()));
        respVO.setStartPosition(detail.getStartPosition());
        respVO.setAvailableBefore(detail.getAvailableBefore());
        respVO.setAvailableAfter(detail.getAvailableAfter());
        respVO.setReportDate(detail.getReportDate());
        respVO.setStartTime(detail.getStartTime());
        respVO.setEndTime(detail.getEndTime());
        respVO.setSandpaperBatchNo(detail.getSandpaperBatchNo());
        respVO.setPressure(detail.getPressure());
        respVO.setLineSpeed(detail.getLineSpeed());
        respVO.setRotationSpeed(detail.getRotationSpeed());
        respVO.setMeterCounter(detail.getMeterCounter());
        respVO.setGrindingThickness(detail.getGrindingThickness());
        respVO.setAfterGrindingThickness(detail.getAfterGrindingThickness());
        respVO.setQualityThickness(detail.getQualityThickness());
        respVO.setQualityWidth(detail.getQualityWidth());
        respVO.setGrindingMeters(detail.getGrindingMeters());
        respVO.setSelfCheck(detail.getSelfCheck());
        respVO.setDefectCode(detail.getDefectCode());
        respVO.setPrintStatus(detail.getPrintStatus());
        respVO.setPrintCount(detail.getPrintCount());
        respVO.setLastPrintTime(detail.getLastPrintTime());
        respVO.setInspectionId(detail.getInspectionId());
        respVO.setInspectionNo(detail.getInspectionNo());
        respVO.setInspectionStatus(detail.getInspectionStatus());
        respVO.setInspectionResult(detail.getInspectionResult());
        respVO.setInspectionApplyTime(detail.getInspectionApplyTime());
        respVO.setInspectionReturnTime(detail.getInspectionReturnTime());
        respVO.setInspectionRejectReason(detail.getInspectionRejectReason());
        respVO.setConfirmStatus(detail.getConfirmStatus());
        respVO.setDownstreamStatus(detail.getDownstreamStatus());
        respVO.setDetailStatus(detail.getDetailStatus());
        respVO.setRemark(detail.getRemark());
        fillMiddleProductSummary(respVO, hcGrindingMiddleProductRecordMapper.selectByBiz(BIZ_GRINDING_SECOND, detail.getId()));
        return respVO;
    }

    private void fillMiddleProductSummary(HcRoughConsoleSecondReportRespVO respVO, HcGrindingMiddleProductRecordDO businessRecord) {
        if (businessRecord == null || Boolean.TRUE.equals(businessRecord.getDeleted())) {
            return;
        }
        respVO.setMiddleProductRecordId(businessRecord.getId());
        respVO.setMiddleProductStatus(businessRecord.getDocStatus());
        respVO.setMiddleProductRecorder(businessRecord.getRecorderName());
        respVO.setMiddleProductRecordTime(businessRecord.getRecorderTime());
        respVO.setMiddleProductGeneratedLength(firstNonNull(businessRecord.getGeneratedLength(), 0));
    }

    private void fillMiddleProductSummary(HcRoughConsoleFirstReportRespVO respVO, Long recordId) {
        if (recordId == null) {
            return;
        }
        HcGrindingMiddleProductRecordDO businessRecord = hcGrindingMiddleProductRecordMapper.selectById(recordId);
        if (businessRecord != null && !Boolean.TRUE.equals(businessRecord.getDeleted())) {
            respVO.setMiddleProductRecordId(businessRecord.getId());
            respVO.setMiddleProductStatus(businessRecord.getDocStatus());
            respVO.setMiddleProductRecorder(businessRecord.getRecorderName());
            respVO.setMiddleProductRecordTime(businessRecord.getRecorderTime());
            respVO.setMiddleProductGeneratedLength(firstNonNull(businessRecord.getGeneratedLength(), 0));
            return;
        }
        HcStationRecordDO record = hcStationRecordMapper.selectById(recordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            return;
        }
        respVO.setMiddleProductRecordId(record.getId());
        respVO.setMiddleProductStatus(record.getDocStatus());
        respVO.setMiddleProductRecorder(record.getRecordUserName());
        respVO.setMiddleProductRecordTime(record.getRecordTime());
        respVO.setMiddleProductGeneratedLength(countStationRecordItems(record.getId()));
    }

    private void fillMiddleProductSummary(HcRoughConsoleSecondReportRespVO respVO, Long recordId) {
        if (recordId == null) {
            return;
        }
        HcGrindingMiddleProductRecordDO businessRecord = hcGrindingMiddleProductRecordMapper.selectById(recordId);
        if (businessRecord != null && !Boolean.TRUE.equals(businessRecord.getDeleted())) {
            respVO.setMiddleProductRecordId(businessRecord.getId());
            respVO.setMiddleProductStatus(businessRecord.getDocStatus());
            respVO.setMiddleProductRecorder(businessRecord.getRecorderName());
            respVO.setMiddleProductRecordTime(businessRecord.getRecorderTime());
            respVO.setMiddleProductGeneratedLength(firstNonNull(businessRecord.getGeneratedLength(), 0));
            return;
        }
        HcStationRecordDO record = hcStationRecordMapper.selectById(recordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted())) {
            return;
        }
        respVO.setMiddleProductRecordId(record.getId());
        respVO.setMiddleProductStatus(record.getDocStatus());
        respVO.setMiddleProductRecorder(record.getRecordUserName());
        respVO.setMiddleProductRecordTime(record.getRecordTime());
        respVO.setMiddleProductGeneratedLength(countStationRecordItems(record.getId()));
    }

    private int countStationRecordItems(Long recordId) {
        if (recordId == null) {
            return 0;
        }
        return hcStationRecordItemMapper.selectByRecordIds(List.of(recordId)).size();
    }

    private int calcWarningFlag(HcEquipmentConsumableStateDO state) {
        boolean sandpaper = CONSUMABLE_SANDPAPER.equals(state.getConsumableType());
        boolean countWarn = !sandpaper && reachesConsumableWarningPercent(state.getUseCount(), state.getLimitCount());
        boolean lengthWarn = reachesConsumableWarningPercent(state.getUsedLength(), state.getLimitLength());
        boolean daysWarn = sandpaper && state.getLastReplaceTime() != null
                && reachesConsumableWarningPercent(
                        (int) ChronoUnit.DAYS.between(state.getLastReplaceTime().toLocalDate(), LocalDate.now()),
                        DEFAULT_SANDPAPER_LIMIT_DAYS);
        return countWarn || lengthWarn || daysWarn ? 1 : 0;
    }

    private int consumableUseIncrement(String consumableType) {
        return CONSUMABLE_GUIDE_CLOTH.equals(consumableType) ? GUIDE_CLOTH_USE_INCREMENT : 1;
    }

    private Integer countDelta(Integer beforeCount, Integer afterCount) {
        if (beforeCount == null || afterCount == null) {
            return null;
        }
        return afterCount - beforeCount;
    }

    private boolean reachesConsumableWarningPercent(Integer value, Integer limit) {
        return value != null && limit != null && limit > 0
                && value * 100L >= limit * (long) CONSUMABLE_WARNING_PERCENT;
    }

    private boolean reachesConsumableWarningPercent(BigDecimal value, BigDecimal limit) {
        return value != null && limit != null && limit.compareTo(BigDecimal.ZERO) > 0
                && value.multiply(BigDecimal.valueOf(100))
                        .compareTo(limit.multiply(BigDecimal.valueOf(CONSUMABLE_WARNING_PERCENT))) >= 0;
    }

    private void applyConsumableRule(HcEquipmentConsumableStateDO state, HcEquipmentDO equipment, String consumableType) {
        ConsumableRuleConfig ruleConfig = resolveConsumableRule(equipment, consumableType);
        state.setLimitCount(ruleConfig.limitCount);
        state.setLimitLength(ruleConfig.limitLength);
    }

    private ConsumableRuleConfig resolveConsumableRule(HcEquipmentDO equipment, String consumableType) {
        List<HcEquipmentConsumableRuleDO> rules = hcEquipmentConsumableRuleMapper.selectEnabledList(PROCESS_CODE, consumableType);
        HcEquipmentConsumableRuleDO matched = rules.stream()
                .filter(rule -> matchesConsumableRule(rule, equipment))
                .max(Comparator.comparingInt(rule -> consumableRulePriority(rule, equipment)))
                .orElse(null);
        if (CONSUMABLE_SANDPAPER.equals(consumableType)) {
            return new ConsumableRuleConfig(null, DEFAULT_SANDPAPER_LIMIT_LENGTH);
        }
        if (matched != null) {
            return new ConsumableRuleConfig(matched.getLimitCount(), matched.getLimitLength());
        }
        if (CONSUMABLE_GUIDE_CLOTH.equals(consumableType)) {
            return new ConsumableRuleConfig(DEFAULT_GUIDE_CLOTH_LIMIT_COUNT, null);
        }
        return new ConsumableRuleConfig(null, null);
    }

    private boolean matchesConsumableRule(HcEquipmentConsumableRuleDO rule, HcEquipmentDO equipment) {
        if (rule.getEquipmentId() != null) {
            return equipment != null && rule.getEquipmentId().equals(equipment.getId());
        }
        if (rule.getWorkCenterId() != null) {
            return equipment != null && rule.getWorkCenterId().equals(equipment.getWorkCenterId());
        }
        return true;
    }

    private int consumableRulePriority(HcEquipmentConsumableRuleDO rule, HcEquipmentDO equipment) {
        if (equipment != null && rule.getEquipmentId() != null && rule.getEquipmentId().equals(equipment.getId())) {
            return 30;
        }
        if (equipment != null && rule.getWorkCenterId() != null && rule.getWorkCenterId().equals(equipment.getWorkCenterId())) {
            return 20;
        }
        return 10;
    }

    private record ConsumableRuleConfig(Integer limitCount, BigDecimal limitLength) {
    }

    private LocalDate effectiveReportDate(LocalDate reportDate, LocalDateTime startTime) {
        if (reportDate != null) {
            return reportDate;
        }
        if (startTime != null && !isPlaceholderReportDateTime(startTime)) {
            return startTime.toLocalDate();
        }
        return LocalDate.now();
    }

    private LocalDateTime normalizeRequiredReportDateTime(LocalDateTime time, LocalDateTime fallback) {
        LocalDateTime normalized = normalizeOptionalReportDateTime(time, fallback);
        return normalized == null ? fallback : normalized;
    }

    private LocalDate parseReportDateText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return LocalDate.parse(StrUtil.trim(text));
        } catch (RuntimeException ex) {
            throw invalidParamException("报工日期格式不正确，请使用 yyyy-MM-dd");
        }
    }

    private LocalDateTime normalizeOptionalReportDateTime(String text, LocalDateTime fallback) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String value = StrUtil.trim(text);
        try {
            if (value.matches("^\\d{2}:\\d{2}(:\\d{2})?$")) {
                LocalTime time = LocalTime.parse(value.length() == 5 ? value + ":00" : value);
                return fallback == null ? null : normalizeOptionalReportDateTime(LocalDateTime.of(fallback.toLocalDate(), time), fallback);
            }
            String normalized = value.replace('T', ' ');
            if (normalized.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")) {
                normalized = normalized + ":00";
            }
            return normalizeOptionalReportDateTime(LocalDateTime.parse(normalized, DATETIME_FORMATTER), fallback);
        } catch (RuntimeException ex) {
            try {
                return normalizeOptionalReportDateTime(LocalDateTime.parse(value), fallback);
            } catch (RuntimeException ignored) {
                throw invalidParamException("报工时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    private LocalDateTime normalizeOptionalReportDateTime(LocalDateTime time, LocalDateTime fallback) {
        if (time == null) {
            return null;
        }
        return isPlaceholderReportDateTime(time) ? fallback : time;
    }

    private boolean isPlaceholderReportDateTime(LocalDateTime time) {
        return time != null && (time.getYear() <= 1971 || time.getYear() == 1900);
    }

    private <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> mapper) {
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return rows.stream().map(mapper).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private String normalizeSegmentMark(String segmentMark) {
        String value = StrUtil.trimToEmpty(segmentMark);
        if (StrUtil.isBlank(value) || "NONE".equalsIgnoreCase(value) || "不分段".equals(value) || "-".equals(value)) {
            return "";
        }
        String upper = value.toUpperCase();
        if (upper.startsWith("P")) {
            return "P";
        }
        if (upper.startsWith("Q")) {
            return "Q";
        }
        if (upper.startsWith("R")) {
            return "R";
        }
        if (upper.startsWith("S")) {
            return "S";
        }
        return upper;
    }

    private String inferSegmentMarkFromBatchNo(String batchNo) {
        String value = StrUtil.trimToEmpty(batchNo).toUpperCase();
        if (value.endsWith("P")) {
            return "P";
        }
        if (value.endsWith("Q")) {
            return "Q";
        }
        if (value.endsWith("R")) {
            return "R";
        }
        if (value.endsWith("S")) {
            return "S";
        }
        return null;
    }

    private String segmentName(String segmentMark) {
        String normalized = normalizeSegmentMark(segmentMark);
        return StrUtil.isBlank(normalized) ? "不分段" : normalized + "段";
    }

    private int calcMiddleProductGeneratedLength(BigDecimal totalLength) {
        BigDecimal length = zero(totalLength);
        if (length.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return MIDDLE_PRODUCT_DEFAULT_ROW_COUNT;
    }

    private LocalDateTime parseHeaderDateTime(Object value, LocalDateTime fallback) {
        String text = toStringValue(value);
        if (StrUtil.isBlank(text)) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(StrUtil.trim(text), DATETIME_FORMATTER);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(StrUtil.trim(text));
            } catch (Exception ex) {
                return fallback;
            }
        }
    }

    private String toPlain(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal parseDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(StrUtil.trim(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(StrUtil.trim(value)).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(StrUtil.trim(value)).longValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String value, LocalDate fallback) {
        if (StrUtil.isBlank(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(StrUtil.trim(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
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
                return value;
            }
        }
        return null;
    }

    private String normalizeBatchNo(String batchNo) {
        String normalized = StrUtil.trim(batchNo);
        return StrUtil.isBlank(normalized) || "-".equals(normalized) ? null : normalized;
    }

    private LocalDateTime normalizeDailyRecordTime(LocalDateTime time, LocalDate recordDate, LocalDateTime fallback) {
        LocalDateTime value = isPlaceholderDailyRecordTime(time, recordDate) ? fallback : time;
        if (value == null) {
            return null;
        }
        if (value.getYear() <= 1971 && recordDate != null) {
            return LocalDateTime.of(recordDate, value.toLocalTime());
        }
        if (isPlaceholderDailyRecordTime(value, recordDate)) {
            return fallback == value ? null : fallback;
        }
        return value;
    }

    private String format(LocalDateTime time, LocalDate recordDate) {
        LocalDateTime normalized = normalizeDailyRecordTime(time, recordDate, null);
        return normalized == null ? null : DATETIME_FORMATTER.format(normalized);
    }

    private boolean isPlaceholderDailyRecordTime(LocalDateTime time, LocalDate recordDate) {
        if (time == null) {
            return true;
        }
        if (time.getYear() <= 1971) {
            return true;
        }
        // 历史 TIME/零点占位经过 GMT+8 转换后会落成当天 08:00:00，不能再当成真实点检时间保存。
        return recordDate != null
                && recordDate.equals(time.toLocalDate())
                && LocalTime.of(8, 0).equals(time.toLocalTime());
    }
}
