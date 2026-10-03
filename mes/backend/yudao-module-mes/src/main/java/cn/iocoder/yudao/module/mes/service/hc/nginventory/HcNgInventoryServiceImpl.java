package cn.iocoder.yudao.module.mes.service.hc.nginventory;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationGridRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationStockReportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationStockReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationTreeRackRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationTreeWarehouseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUnfreezeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualOutboundReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPiecePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchQueryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgScrapReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgRackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgShelfReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgTransferReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgUnshelfReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgWarehouseSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgManualPieceImportExcelVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryLocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryRackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcLabelPrintLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgInventoryLocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgInventoryPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgInventoryRackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgInventoryWarehouseMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcUnqualifiedHistoryLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcLabelPrintLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.dto.HcWipOutputPostReq;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 分切、压槽不合格品上架、冻结及报废闭环。 */
@Service
@Validated
public class HcNgInventoryServiceImpl implements HcNgInventoryService {

    private static final String PAD_TYPE_BLACK = "BLACK_PAD";
    private static final String PAD_TYPE_WHITE = "WHITE_PAD";
    private static final String PURPOSE_SLITTING_NG = "SLITTING_NG";
    private static final String PURPOSE_PRESS_SLOT_NG = "PRESS_SLOT_NG";
    private static final String PURPOSE_FREEZE = "FREEZE";
    /** 货架容量模式下的内部定位记录标记；不再作为上架用途限制。 */
    private static final String PURPOSE_MIXED = "MIXED";
    private static final String STOCK_TYPE_WIP = "WIP";
    private static final String SOURCE_TYPE_SLITTING = "SLITTING";
    private static final String SOURCE_TYPE_PRESS_SLOT = "PRESS_SLOT";
    private static final String SOURCE_TYPE_MANUAL_HISTORY = "MANUAL_HISTORY";
    private static final String SOURCE_TYPE_NG = "NG_INVENTORY";
    private static final String SOURCE_TABLE_SLITTING = "mes_sfc_slitting_slice_record";
    private static final String SOURCE_TABLE_PRESS_SLOT = "mes_sfc_press_slot_report";
    private static final String SOURCE_TABLE_MANUAL_PIECE = "mes_inv_ng_manual_piece";
    private static final String SOURCE_TABLE_NG_PIECE = "mes_inv_ng_piece";
    private static final String LABEL_TYPE_NG_PIECE = "NG_PIECE";
    private static final String PROCESS_SLITTING = "SLITTING";
    private static final String PROCESS_PRESS_SLOT = "PRESS_SLOT";
    private static final String STATUS_WAIT_SHELF = "WAIT_SHELF";
    private static final String STATUS_WAIT_FREEZE_SHELF = "WAIT_FREEZE_SHELF";
    private static final String STATUS_STORED = "STORED";
    private static final String STATUS_FROZEN = "FROZEN";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_OUTBOUNDED = "OUTBOUNDED";
    private static final String STATUS_SCRAPPED = "SCRAPPED";
    private static final String STATUS_UNFROZEN_CLOSED = "UNFROZEN_CLOSED";
    private static final String ENTRY_REASON_NG_REPORT = "NG_REPORT";
    private static final String ENTRY_REASON_FREEZE_INSTRUCTION = "FREEZE_INSTRUCTION";
    private static final String ENTRY_REASON_HISTORY_BACKFILL = "HISTORY_BACKFILL";
    private static final String ENTRY_REASON_HISTORY_FREEZE_BACKFILL = "HISTORY_FREEZE_BACKFILL";
    private static final String MANUAL_STORAGE_NORMAL = "NORMAL";
    private static final String MANUAL_STORAGE_FREEZE = "FREEZE";
    private static final String MANUAL_RECORD_VOID = "VOID";
    private static final String MANUAL_RECORD_UNFROZEN_CLOSED = "UNFROZEN_CLOSED";
    private static final int MANUAL_PIECE_IMPORT_MAX_ROWS = 2000;
    private static final long MANUAL_PIECE_IMPORT_MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String QUALITY_STATUS_OK = "OK";
    private static final String QUALITY_STATUS_NG = "NG";
    private static final String BIZ_STATUS_NG_STORED = "NG_STORED";
    private static final String BIZ_STATUS_NG_FROZEN = "NG_FROZEN";
    private static final String BIZ_STATUS_OK_FROZEN = "FROZEN";
    private static final String BIZ_STATUS_NG_EMPTY = "NG_EMPTY";
    private static final String INSTRUCTION_FREEZE = "FREEZE_STOCK";
    private static final String INSTRUCTION_UNFREEZE = "UNFREEZE_STOCK";
    private static final BigDecimal PIECE_QTY = BigDecimal.ONE;
    private static final int MAX_DEFECT_SUMMARY_LENGTH = 1000;
    private static final int MAX_STOCK_BUSINESS_REMARK_LENGTH = 300;
    private static final Pattern WAREHOUSE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{0,31}$");
    // 货架编码是展示码第二段，允许使用纯数字（如 1、01），但不允许分隔符，避免破坏库位编码分段。
    private static final Pattern RACK_CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_]{0,31}$");
    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private HcNgInventoryPieceMapper hcNgInventoryPieceMapper;
    @Resource
    private HcNgManualPieceMapper hcNgManualPieceMapper;
    @Resource
    private HcNgInventoryLocationMapper hcNgInventoryLocationMapper;
    @Resource
    private HcNgInventoryWarehouseMapper hcNgInventoryWarehouseMapper;
    @Resource
    private HcNgInventoryRackMapper hcNgInventoryRackMapper;
    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcInvTxnLogMapper hcInvTxnLogMapper;
    @Resource
    private HcUnqualifiedHistoryLedgerMapper hcUnqualifiedHistoryLedgerMapper;
    @Resource
    private HcProductionInstructionMapper hcProductionInstructionMapper;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcProductModelMaterialMapper hcProductModelMaterialMapper;
    @Resource
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Resource
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Resource
    private HcLabelPrintLogMapper hcLabelPrintLogMapper;
    @Resource
    private HcInvStockService hcInvStockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerSlittingNgPiece(HcSlittingSliceRecordDO source, HcPlanOrderDO plan,
                                        HcPlanOrderOperationDO operation) {
        if (source == null || source.getId() == null || hcNgInventoryPieceMapper.selectBySource(SOURCE_TYPE_SLITTING, source.getId()) != null) {
            return;
        }
        Map<String, Object> defect = new LinkedHashMap<>();
        defect.put("selfCheck", source.getSelfCheck());
        defect.put("visualResult", source.getVisualResultJson());
        defect.put("remark", source.getRemark());
        insertNgPiece(HcNgInventoryPieceDO.builder()
                .sourceType(SOURCE_TYPE_SLITTING)
                .sourceTable(SOURCE_TABLE_SLITTING)
                .sourceId(source.getId())
                .sourceReportId(source.getSourceAdhesiveReportId())
                .sourcePlanId(source.getPlanId())
                .sourcePlanNo(source.getPlanNo())
                .sourcePlanOperationId(source.getPlanOperationId())
                .processType(PROCESS_SLITTING)
                .processName("分切")
                .pieceNo(firstNotBlank(source.getSliceSerialNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceBatchNo(firstNotBlank(source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceParentBatchNo(source.getSourceBatchNo())
                .materialId(plan == null ? null : plan.getMaterialId())
                .materialCode(plan == null ? null : plan.getMaterialCode())
                .materialName(plan == null ? null : plan.getMaterialName())
                .modelNo(firstNotBlank(plan == null ? null : plan.getModelCode(), plan == null ? null : plan.getModelName()))
                .padType(resolvePadType(plan))
                .pieceQty(PIECE_QTY)
                .entryReason(ENTRY_REASON_NG_REPORT)
                .qualityResult(QUALITY_STATUS_NG)
                .defectSummary(buildSlittingDefectSummary(source.getSelfCheck(), source.getVisualResultJson(), source.getRemark()))
                .defectDetailJson(JsonUtils.toJsonString(defect))
                .tenantId(firstNonNull(source.getTenantId(), plan == null ? null : plan.getTenantId()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlittingWaitShelfNgPiece(Long sourceId) {
        if (sourceId == null) {
            return;
        }
        HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectBySourceForUpdate(SOURCE_TYPE_SLITTING, sourceId);
        if (piece == null) {
            return;
        }
        if (!STATUS_WAIT_SHELF.equals(piece.getStatus()) || piece.getStockId() != null) {
            throw invalidParamException("该分切 NG 片已上架或处置，不能直接改为合格");
        }
        if (hcNgInventoryPieceMapper.deleteById(piece.getId()) <= 0) {
            throw invalidParamException("分切 NG 隔离记录状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerSlittingNgPieceAttributedByPressSlot(HcSlittingSliceRecordDO source,
                                                              HcPressSlotReportDO detectionReport,
                                                              HcPlanOrderDO plan,
                                                              HcPlanOrderOperationDO operation) {
        if (source == null || source.getId() == null
                || hcNgInventoryPieceMapper.selectBySource(SOURCE_TYPE_SLITTING, source.getId()) != null) {
            return;
        }
        Map<String, Object> defect = new LinkedHashMap<>();
        defect.put("selfCheck", source.getSelfCheck());
        defect.put("visualResult", source.getVisualResultJson());
        defect.put("remark", source.getRemark());
        defect.put("attributionType", "PRE_PROCESS_SELF_CHECK");
        defect.put("attributedProcessCode", PROCESS_SLITTING);
        defect.put("detectedProcessCode", PROCESS_PRESS_SLOT);
        defect.put("detectedProcessName", "压槽");
        defect.put("detectedReportId", detectionReport == null ? null : detectionReport.getId());
        defect.put("detectedSelfCheck", detectionReport == null ? null : detectionReport.getSelfCheck());
        defect.put("detectedDefectCode", detectionReport == null ? null : detectionReport.getDefectCode());
        defect.put("detectedRemark", detectionReport == null ? null : detectionReport.getRemark());
        defect.put("detectionExtra", detectionReport == null ? null : detectionReport.getExtraJson());
        insertNgPiece(HcNgInventoryPieceDO.builder()
                .sourceType(SOURCE_TYPE_SLITTING)
                .sourceTable(SOURCE_TABLE_SLITTING)
                .sourceId(source.getId())
                .sourceReportId(source.getSourceAdhesiveReportId())
                .sourcePlanId(source.getPlanId())
                .sourcePlanNo(source.getPlanNo())
                .sourcePlanOperationId(source.getPlanOperationId())
                .processType(PROCESS_SLITTING)
                .processName("分切")
                .pieceNo(firstNotBlank(source.getSliceSerialNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceBatchNo(firstNotBlank(source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceParentBatchNo(source.getSourceBatchNo())
                .materialId(plan == null ? null : plan.getMaterialId())
                .materialCode(plan == null ? null : plan.getMaterialCode())
                .materialName(plan == null ? null : plan.getMaterialName())
                .modelNo(firstNotBlank(plan == null ? null : plan.getModelCode(), plan == null ? null : plan.getModelName()))
                .padType(resolvePadType(plan))
                .pieceQty(PIECE_QTY)
                .entryReason(ENTRY_REASON_NG_REPORT)
                .qualityResult(QUALITY_STATUS_NG)
                .defectSummary(buildAttributedDefectSummary("分切", "压槽", detectionReport == null ? null : detectionReport.getSelfCheck(),
                        detectionReport == null ? null : detectionReport.getDefectCode(),
                        detectionReport == null ? null : detectionReport.getRemark()))
                .defectDetailJson(JsonUtils.toJsonString(defect))
                .tenantId(firstNonNull(source.getTenantId(), plan == null ? null : plan.getTenantId()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerPressSlotNgPiece(HcPressSlotReportDO source, HcPlanOrderDO plan,
                                         HcPlanOrderOperationDO operation) {
        if (source == null || source.getId() == null || hcNgInventoryPieceMapper.selectBySource(SOURCE_TYPE_PRESS_SLOT, source.getId()) != null) {
            return;
        }
        Map<String, Object> defect = new LinkedHashMap<>();
        defect.put("selfCheck", source.getSelfCheck());
        defect.put("defectCode", source.getDefectCode());
        defect.put("remark", source.getRemark());
        insertNgPiece(HcNgInventoryPieceDO.builder()
                .sourceType(SOURCE_TYPE_PRESS_SLOT)
                .sourceTable(SOURCE_TABLE_PRESS_SLOT)
                .sourceId(source.getId())
                .sourceReportId(source.getId())
                .sourcePlanId(source.getPlanId())
                .sourcePlanNo(source.getPlanNo())
                .sourcePlanOperationId(source.getPlanOperationId())
                .processType(PROCESS_PRESS_SLOT)
                .processName("压槽")
                .pieceNo(firstNotBlank(source.getProductionBatchNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceBatchNo(firstNotBlank(source.getProductionBatchNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceParentBatchNo(firstNotBlank(source.getParentProductionBatchNo(), source.getSourceBatchNo()))
                .materialId(plan == null ? null : plan.getMaterialId())
                .materialCode(firstNotBlank(source.getMaterialCode(), plan == null ? null : plan.getMaterialCode()))
                .materialName(firstNotBlank(source.getMaterialName(), plan == null ? null : plan.getMaterialName()))
                .modelNo(firstNotBlank(source.getModelCode(), plan == null ? null : plan.getModelCode(), plan == null ? null : plan.getModelName()))
                .padType(resolvePadType(plan))
                .pieceQty(PIECE_QTY)
                .entryReason(ENTRY_REASON_NG_REPORT)
                .qualityResult(QUALITY_STATUS_NG)
                .defectSummary(buildDefectSummary(source.getSelfCheck(), source.getDefectCode(), source.getRemark()))
                .defectDetailJson(JsonUtils.toJsonString(defect))
                .tenantId(firstNonNull(source.getTenantId(), plan == null ? null : plan.getTenantId()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerPressSlotNgPieceAttributedByAdhesive2(HcPressSlotReportDO source,
                                                               HcAdhesive2ReportDO detectionReport,
                                                               HcPlanOrderDO plan,
                                                               HcPlanOrderOperationDO operation) {
        if (source == null || source.getId() == null
                || hcNgInventoryPieceMapper.selectBySource(SOURCE_TYPE_PRESS_SLOT, source.getId()) != null) {
            return;
        }
        Map<String, Object> defect = new LinkedHashMap<>();
        defect.put("selfCheck", source.getSelfCheck());
        defect.put("defectCode", source.getDefectCode());
        defect.put("remark", source.getRemark());
        defect.put("attributionType", "PRE_PROCESS_SELF_CHECK");
        defect.put("attributedProcessCode", PROCESS_PRESS_SLOT);
        defect.put("detectedProcessCode", "ADHESIVE2");
        defect.put("detectedProcessName", "粘胶2");
        defect.put("detectedReportId", detectionReport == null ? null : detectionReport.getId());
        defect.put("detectedSelfCheck", detectionReport == null ? null : detectionReport.getSelfCheck());
        defect.put("detectedDefectCode", detectionReport == null ? null : detectionReport.getDefectCode());
        defect.put("detectedRemark", detectionReport == null ? null : detectionReport.getRemark());
        defect.put("detectionExtra", detectionReport == null ? null : detectionReport.getExtraJson());
        insertNgPiece(HcNgInventoryPieceDO.builder()
                .sourceType(SOURCE_TYPE_PRESS_SLOT)
                .sourceTable(SOURCE_TABLE_PRESS_SLOT)
                .sourceId(source.getId())
                .sourceReportId(source.getId())
                .sourcePlanId(source.getPlanId())
                .sourcePlanNo(source.getPlanNo())
                .sourcePlanOperationId(source.getPlanOperationId())
                .processType(PROCESS_PRESS_SLOT)
                .processName("压槽")
                .pieceNo(firstNotBlank(source.getProductionBatchNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceBatchNo(firstNotBlank(source.getProductionBatchNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceParentBatchNo(firstNotBlank(source.getParentProductionBatchNo(), source.getSourceBatchNo()))
                .materialId(plan == null ? null : plan.getMaterialId())
                .materialCode(firstNotBlank(source.getMaterialCode(), plan == null ? null : plan.getMaterialCode()))
                .materialName(firstNotBlank(source.getMaterialName(), plan == null ? null : plan.getMaterialName()))
                .modelNo(firstNotBlank(source.getModelCode(), plan == null ? null : plan.getModelCode(), plan == null ? null : plan.getModelName()))
                .padType(resolvePadType(plan))
                .pieceQty(PIECE_QTY)
                .entryReason(ENTRY_REASON_NG_REPORT)
                .qualityResult(QUALITY_STATUS_NG)
                .defectSummary(buildAttributedDefectSummary("压槽", "粘胶2", detectionReport == null ? null : detectionReport.getSelfCheck(),
                        detectionReport == null ? null : detectionReport.getDefectCode(),
                        detectionReport == null ? null : detectionReport.getRemark()))
                .defectDetailJson(JsonUtils.toJsonString(defect))
                .tenantId(firstNonNull(source.getTenantId(), plan == null ? null : plan.getTenantId()))
                .build());
    }

    @Override
    public HcProductionInstructionDO getEffectiveFreezeInstruction(Long planId, Long planOperationId) {
        HcProductionInstructionDO instruction = hcProductionInstructionMapper
                .selectLatestInventoryControlInstruction(planId, planOperationId);
        return instruction != null && INSTRUCTION_FREEZE.equals(instruction.getInstructionType()) ? instruction : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerSlittingFrozenPiece(HcSlittingSliceRecordDO source, HcPlanOrderDO plan,
                                            HcPlanOrderOperationDO operation, HcProductionInstructionDO instruction) {
        if (source == null || source.getId() == null || instruction == null || instruction.getId() == null
                || hcNgInventoryPieceMapper.selectBySource(SOURCE_TYPE_SLITTING, source.getId()) != null) {
            return;
        }
        boolean ng = isSlittingSourceNg(source);
        Map<String, Object> defect = new LinkedHashMap<>();
        defect.put("selfCheck", source.getSelfCheck());
        defect.put("visualResult", source.getVisualResultJson());
        defect.put("remark", source.getRemark());
        insertFrozenPiece(HcNgInventoryPieceDO.builder()
                .sourceType(SOURCE_TYPE_SLITTING)
                .sourceTable(SOURCE_TABLE_SLITTING)
                .sourceId(source.getId())
                .sourceReportId(source.getSourceAdhesiveReportId())
                .sourcePlanId(source.getPlanId())
                .sourcePlanNo(source.getPlanNo())
                .sourcePlanOperationId(source.getPlanOperationId())
                .processType(PROCESS_SLITTING)
                .processName("分切")
                .pieceNo(firstNotBlank(source.getSliceSerialNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceBatchNo(firstNotBlank(source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceParentBatchNo(source.getSourceBatchNo())
                .materialId(plan == null ? null : plan.getMaterialId())
                .materialCode(plan == null ? null : plan.getMaterialCode())
                .materialName(plan == null ? null : plan.getMaterialName())
                .modelNo(firstNotBlank(plan == null ? null : plan.getModelCode(), plan == null ? null : plan.getModelName()))
                .padType(resolvePadType(plan))
                .pieceQty(PIECE_QTY)
                .entryReason(ENTRY_REASON_FREEZE_INSTRUCTION)
                .qualityResult(ng ? QUALITY_STATUS_NG : QUALITY_STATUS_OK)
                .defectSummary(ng ? buildSlittingDefectSummary(source.getSelfCheck(), source.getVisualResultJson(), source.getRemark()) : null)
                .defectDetailJson(JsonUtils.toJsonString(defect))
                .freezeInstructionId(instruction.getId())
                .freezeInstructionNo(instruction.getInstructionNo())
                .freezeEffectiveTime(instruction.getIssuedTime())
                .tenantId(firstNonNull(source.getTenantId(), plan == null ? null : plan.getTenantId()))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerPressSlotFrozenPiece(HcPressSlotReportDO source, HcPlanOrderDO plan,
                                             HcPlanOrderOperationDO operation, HcProductionInstructionDO instruction) {
        if (source == null || source.getId() == null || instruction == null || instruction.getId() == null
                || hcNgInventoryPieceMapper.selectBySource(SOURCE_TYPE_PRESS_SLOT, source.getId()) != null) {
            return;
        }
        boolean ng = isPressSlotSourceNg(source);
        Map<String, Object> defect = new LinkedHashMap<>();
        defect.put("selfCheck", source.getSelfCheck());
        defect.put("defectCode", source.getDefectCode());
        defect.put("remark", source.getRemark());
        insertFrozenPiece(HcNgInventoryPieceDO.builder()
                .sourceType(SOURCE_TYPE_PRESS_SLOT)
                .sourceTable(SOURCE_TABLE_PRESS_SLOT)
                .sourceId(source.getId())
                .sourceReportId(source.getId())
                .sourcePlanId(source.getPlanId())
                .sourcePlanNo(source.getPlanNo())
                .sourcePlanOperationId(source.getPlanOperationId())
                .processType(PROCESS_PRESS_SLOT)
                .processName("压槽")
                .pieceNo(firstNotBlank(source.getProductionBatchNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceBatchNo(firstNotBlank(source.getProductionBatchNo(), source.getSourceProductionBatchNo(), source.getSourceBatchNo()))
                .sourceParentBatchNo(firstNotBlank(source.getParentProductionBatchNo(), source.getSourceBatchNo()))
                .materialId(plan == null ? null : plan.getMaterialId())
                .materialCode(firstNotBlank(source.getMaterialCode(), plan == null ? null : plan.getMaterialCode()))
                .materialName(firstNotBlank(source.getMaterialName(), plan == null ? null : plan.getMaterialName()))
                .modelNo(firstNotBlank(source.getModelCode(), plan == null ? null : plan.getModelCode(), plan == null ? null : plan.getModelName()))
                .padType(resolvePadType(plan))
                .pieceQty(PIECE_QTY)
                .entryReason(ENTRY_REASON_FREEZE_INSTRUCTION)
                .qualityResult(ng ? QUALITY_STATUS_NG : QUALITY_STATUS_OK)
                .defectSummary(ng ? buildDefectSummary(source.getSelfCheck(), source.getDefectCode(), source.getRemark()) : null)
                .defectDetailJson(JsonUtils.toJsonString(defect))
                .freezeInstructionId(instruction.getId())
                .freezeInstructionNo(instruction.getInstructionNo())
                .freezeEffectiveTime(instruction.getIssuedTime())
                .tenantId(firstNonNull(source.getTenantId(), plan == null ? null : plan.getTenantId()))
                .build());
    }

    @Override
    public boolean isNgPieceManaged(String sourceType, Long sourceId) {
        return sourceId != null && hcNgInventoryPieceMapper.selectBySource(sourceType, sourceId) != null;
    }

    @Override
    public PageResult<NgPieceRespVO> getWaitShelfPage(NgPiecePageReqVO reqVO) {
        reqVO.setStatus(STATUS_WAIT_SHELF);
        return toRespPage(hcNgInventoryPieceMapper.selectPage(reqVO));
    }

    @Override
    public PageResult<NgPieceRespVO> getWaitFreezeShelfPage(NgPiecePageReqVO reqVO) {
        reqVO.setStatus(STATUS_WAIT_FREEZE_SHELF);
        return toRespPage(hcNgInventoryPieceMapper.selectPage(reqVO));
    }

    @Override
    public PageResult<NgPieceSegmentRespVO> getWaitShelfSegmentPage(NgPiecePageReqVO reqVO) {
        return getWaitSegmentPage(reqVO, STATUS_WAIT_SHELF, false);
    }

    @Override
    public List<NgPieceRespVO> getWaitShelfSegmentPieceList(NgPiecePageReqVO reqVO) {
        return getWaitSegmentPieceList(reqVO, STATUS_WAIT_SHELF, false);
    }

    @Override
    public PageResult<NgPieceSegmentRespVO> getWaitFreezeShelfSegmentPage(NgPiecePageReqVO reqVO) {
        return getWaitSegmentPage(reqVO, STATUS_WAIT_FREEZE_SHELF, true);
    }

    @Override
    public List<NgPieceRespVO> getWaitFreezeShelfSegmentPieceList(NgPiecePageReqVO reqVO) {
        return getWaitSegmentPieceList(reqVO, STATUS_WAIT_FREEZE_SHELF, true);
    }

    @Override
    public PageResult<NgPieceRespVO> getPiecePage(NgPiecePageReqVO reqVO) {
        return toRespPage(hcNgInventoryPieceMapper.selectPage(reqVO));
    }

    @Override
    public PageResult<NgHistoryLedgerRespVO> getHistoryLedgerPage(NgHistoryLedgerPageReqVO reqVO) {
        PageResult<NgHistoryLedgerRespVO> pageResult = hcInvTxnLogMapper.selectNgHistoryLedgerPage(reqVO);
        pageResult.getList().forEach(item -> {
            item.setOperationName(resolveHistoryOperationName(item.getTxnType()));
            item.setTxnDirection(resolveHistoryTxnDirection(item.getTxnType(), item.getTxnQty()));
        });
        return pageResult;
    }

    @Override
    public PageResult<UnqualifiedHistoryLedgerRespVO> getUnqualifiedHistoryLedgerPage(
            UnqualifiedHistoryLedgerPageReqVO reqVO) {
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        long offset = (long) (pageNo - 1) * pageSize;
        List<UnqualifiedHistoryLedgerRespVO> rows = hcUnqualifiedHistoryLedgerMapper
                .selectPage(reqVO, offset, pageSize);
        long total = hcUnqualifiedHistoryLedgerMapper.count(reqVO);
        return new PageResult<>(rows, total);
    }

    @Override
    public PageResult<NgPieceSegmentRespVO> getInventorySegmentPage(NgPiecePageReqVO reqVO) {
        List<HcNgInventoryPieceDO> pieces = hcNgInventoryPieceMapper.selectInventoryListForGroup(reqVO);
        if (pieces.isEmpty()) {
            return new PageResult<>(List.of(), 0L);
        }
        Map<String, List<HcNgInventoryPieceDO>> groupMap = new LinkedHashMap<>();
        for (HcNgInventoryPieceDO piece : pieces) {
            String segmentBatchNo = resolveSegmentBatchNo(piece);
            String groupKey = buildWaitSegmentGroupKey(piece, segmentBatchNo, false);
            groupMap.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(piece);
        }
        List<NgPieceSegmentRespVO> groups = groupMap.values().stream()
                .map(group -> toWaitSegmentResp(group, false))
                .toList();
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        int start = Math.min((pageNo - 1) * pageSize, groups.size());
        int end = Math.min(start + pageSize, groups.size());
        return new PageResult<>(groups.subList(start, end), (long) groups.size());
    }

    @Override
    public List<NgPieceRespVO> getInventorySegmentPieceList(NgPiecePageReqVO reqVO) {
        String segmentBatchNo = StrUtil.trim(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            return List.of();
        }
        return toPieceRespList(hcNgInventoryPieceMapper.selectInventoryListForGroup(reqVO).stream()
                .filter(piece -> segmentBatchNo.equals(resolveSegmentBatchNo(piece)))
                .toList());
    }

    @Override
    public NgPieceLabelBatchQueryRespVO getPieceLabels(NgPieceLabelBatchQueryReqVO reqVO) {
        NgPieceLabelBatchQueryRespVO respVO = new NgPieceLabelBatchQueryRespVO();
        Set<Long> seenPieceIds = new HashSet<>();
        for (Long pieceId : reqVO.getPieceIds()) {
            if (!seenPieceIds.add(pieceId)) {
                respVO.getFailures().add("不合格品逐片ID " + pieceId + " 重复，已忽略");
                continue;
            }
            HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectById(pieceId);
            if (piece == null) {
                respVO.getFailures().add("未找到不合格品逐片记录：" + pieceId);
                continue;
            }
            if (!isPrintablePieceStatus(piece.getStatus())) {
                respVO.getFailures().add("片号 " + firstNotBlank(piece.getPieceNo(), String.valueOf(pieceId))
                        + " 当前状态不支持打印，请刷新后重试");
                continue;
            }
            respVO.getLabels().add(buildPieceLabel(piece));
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NgPieceLabelRespVO> markPieceLabelsPrinted(NgPieceLabelBatchPrintedReqVO reqVO) {
        List<NgPieceLabelRespVO> labels = new ArrayList<>();
        Set<Long> seenPieceIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (NgPieceLabelPrintedReqVO item : reqVO.getItems()) {
            if (!seenPieceIds.add(item.getPieceId())) {
                throw invalidParamException("同一不合格品逐片记录不能重复回写：" + item.getPieceId());
            }
            HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectByIdForUpdate(item.getPieceId());
            if (piece == null) {
                throw invalidParamException("不合格品逐片记录不存在或已删除：" + item.getPieceId());
            }
            if (!isPrintablePieceStatus(piece.getStatus())) {
                throw invalidParamException("片号 " + firstNotBlank(piece.getPieceNo(), String.valueOf(piece.getId()))
                        + " 当前状态不支持打印，请刷新后重试");
            }
            int printCount = normalizePrintCount(piece.getPrintCount());
            HcNgInventoryPieceDO update = new HcNgInventoryPieceDO();
            update.setId(piece.getId());
            update.setPrintCount(printCount + 1);
            update.setLastPrintTime(now);
            hcNgInventoryPieceMapper.updateById(update);
            piece.setPrintCount(printCount + 1);
            piece.setLastPrintTime(now);
            NgPieceLabelRespVO label = buildPieceLabel(piece);
            hcLabelPrintLogMapper.insert(HcLabelPrintLogDO.builder()
                    .tenantId(currentTenantId())
                    .labelNo(firstNotBlank(label.getPieceNo(), String.valueOf(label.getPieceId())))
                    .labelType(LABEL_TYPE_NG_PIECE)
                    .bizId(label.getPieceId())
                    .bizNo(firstNotBlank(label.getPieceNo(), String.valueOf(label.getPieceId())))
                    .planId(label.getPlanId())
                    .planNo(label.getPlanNo())
                    .planOperationId(label.getPlanOperationId())
                    .printCount(label.getPrintCount())
                    .printerName(StrUtil.trimToNull(item.getPrinterName()))
                    .printTime(now)
                    .labelContentJson(StrUtil.trimToNull(item.getLabelContentJson()))
                    .build());
            labels.add(label);
        }
        return labels;
    }

    private NgPieceLabelRespVO buildPieceLabel(HcNgInventoryPieceDO piece) {
        LocalDateTime workTime = piece.getCreateTime();
        String recorderName = piece.getCreator();
        String manuallyAddedMaterialCode = null;
        if (SOURCE_TYPE_SLITTING.equals(piece.getSourceType())) {
            HcSlittingSliceRecordDO source = hcSlittingSliceRecordMapper.selectById(piece.getSourceId());
            if (source != null) {
                workTime = source.getCreateTime() == null ? workTime : source.getCreateTime();
                recorderName = firstNotBlank(source.getCreator(), source.getScannerName(), recorderName);
            }
        } else if (SOURCE_TYPE_PRESS_SLOT.equals(piece.getSourceType())) {
            HcPressSlotReportDO source = hcPressSlotReportMapper.selectById(piece.getSourceId());
            if (source != null) {
                workTime = firstNonNull(source.getRecorderTime(), firstNonNull(source.getEndTime(),
                        firstNonNull(source.getCreateTime(), workTime)));
                recorderName = firstNotBlank(source.getRecorderName(), source.getConfirmerName(), source.getCreator(), recorderName);
            }
        } else if (SOURCE_TYPE_MANUAL_HISTORY.equals(piece.getSourceType())) {
            HcNgManualPieceDO source = hcNgManualPieceMapper.selectById(piece.getSourceId());
            if (source != null) {
                workTime = firstNonNull(source.getRecorderTime(), firstNonNull(source.getCreateTime(), workTime));
                recorderName = firstNotBlank(source.getRecorderName(), source.getCreator(), recorderName);
                manuallyAddedMaterialCode = firstNotBlank(source.getMaterialCode(), piece.getMaterialCode());
            }
        }
        NgPieceLabelRespVO label = new NgPieceLabelRespVO();
        label.setPieceId(piece.getId());
        label.setSourceType(piece.getSourceType());
        label.setSourceId(piece.getSourceId());
        label.setPlanId(piece.getSourcePlanId());
        label.setPlanOperationId(piece.getSourcePlanOperationId());
        label.setPlanNo(piece.getSourcePlanNo());
        label.setMaterialCode(resolvePieceLabelMaterialCode(piece.getModelNo(), manuallyAddedMaterialCode));
        label.setModelCode(piece.getModelNo());
        label.setSegmentBatchNo(resolveSegmentBatchNo(piece));
        label.setPieceNo(piece.getPieceNo());
        label.setProcessName(piece.getProcessName());
        label.setRecorderName(recorderName);
        label.setWorkTime(workTime);
        label.setRecordStatus(piece.getStatus());
        label.setPrintCount(normalizePrintCount(piece.getPrintCount()));
        label.setLastPrintTime(piece.getLastPrintTime());
        return label;
    }

    /**
     * 标签料号仅以物料分类中的型号关联为准；历史补录由人工填写时可明确覆盖该关联。
     */
    private String resolvePieceLabelMaterialCode(String modelCode, String manuallyAddedMaterialCode) {
        String manualMaterialCode = StrUtil.trimToNull(manuallyAddedMaterialCode);
        if (manualMaterialCode != null) {
            return manualMaterialCode;
        }
        String normalizedModelCode = StrUtil.trimToNull(modelCode);
        if (normalizedModelCode == null) {
            return null;
        }
        HcProductModelMaterialDO modelMaterial = hcProductModelMaterialMapper
                .selectFirstByModelCode(normalizedModelCode);
        return modelMaterial == null ? null : StrUtil.trimToNull(modelMaterial.getMaterialCode());
    }

    private boolean isPrintablePieceStatus(String status) {
        return List.of(STATUS_WAIT_SHELF, STATUS_WAIT_FREEZE_SHELF, STATUS_STORED, STATUS_FROZEN).contains(status);
    }

    private int normalizePrintCount(Integer printCount) {
        return printCount == null ? 0 : Math.max(printCount, 0);
    }

    private PageResult<NgPieceSegmentRespVO> getWaitSegmentPage(NgPiecePageReqVO reqVO, String status,
                                                                  boolean includeFreezeInstruction) {
        List<HcNgInventoryPieceDO> pieces = hcNgInventoryPieceMapper.selectWaitShelfListForGroup(reqVO, status);
        if (pieces.isEmpty()) {
            return new PageResult<>(List.of(), 0L);
        }
        Map<String, List<HcNgInventoryPieceDO>> groupMap = new LinkedHashMap<>();
        for (HcNgInventoryPieceDO piece : pieces) {
            String segmentBatchNo = resolveSegmentBatchNo(piece);
            String groupKey = buildWaitSegmentGroupKey(piece, segmentBatchNo, includeFreezeInstruction);
            groupMap.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(piece);
        }
        List<NgPieceSegmentRespVO> groups = groupMap.values().stream()
                .map(group -> toWaitSegmentResp(group, includeFreezeInstruction))
                .toList();
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        int start = Math.min((pageNo - 1) * pageSize, groups.size());
        int end = Math.min(start + pageSize, groups.size());
        return new PageResult<>(groups.subList(start, end), (long) groups.size());
    }

    private List<NgPieceRespVO> getWaitSegmentPieceList(NgPiecePageReqVO reqVO, String status,
                                                         boolean includeFreezeInstruction) {
        String segmentBatchNo = StrUtil.trim(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            return List.of();
        }
        return toPieceRespList(hcNgInventoryPieceMapper.selectWaitShelfListForGroup(reqVO, status).stream()
                .filter(piece -> segmentBatchNo.equals(resolveSegmentBatchNo(piece)))
                .filter(piece -> !includeFreezeInstruction
                        || Objects.equals(piece.getFreezeInstructionId(), reqVO.getFreezeInstructionId()))
                .toList());
    }

    private NgPieceSegmentRespVO toWaitSegmentResp(List<HcNgInventoryPieceDO> pieces,
                                                     boolean includeFreezeInstruction) {
        HcNgInventoryPieceDO first = pieces.get(0);
        NgPieceSegmentRespVO resp = new NgPieceSegmentRespVO();
        resp.setSegmentBatchNo(resolveSegmentBatchNo(first));
        resp.setProcessType(first.getProcessType());
        resp.setProcessName(first.getProcessName());
        resp.setSourcePlanId(first.getSourcePlanId());
        resp.setSourcePlanNo(first.getSourcePlanNo());
        resp.setSourcePlanOperationId(first.getSourcePlanOperationId());
        resp.setMaterialCode(first.getMaterialCode());
        resp.setMaterialName(first.getMaterialName());
        resp.setModelNo(first.getModelNo());
        resp.setTotalPieceCount(pieces.size());
        int okCount = (int) pieces.stream()
                .filter(piece -> QUALITY_STATUS_OK.equals(resolvePieceQualityStatus(piece)))
                .count();
        resp.setOkPieceCount(okCount);
        resp.setNgPieceCount(pieces.size() - okCount);
        resp.setSamplePieceNos(pieces.stream().map(HcNgInventoryPieceDO::getPieceNo)
                .filter(StrUtil::isNotBlank).distinct().limit(3).toList());
        if (includeFreezeInstruction) {
            resp.setFreezeInstructionId(first.getFreezeInstructionId());
            resp.setFreezeInstructionNo(first.getFreezeInstructionNo());
        }
        return resp;
    }

    private String buildWaitSegmentGroupKey(HcNgInventoryPieceDO piece, String segmentBatchNo,
                                            boolean includeFreezeInstruction) {
        return firstNotBlank(piece.getProcessType(), "-") + "|"
                + firstNotBlank(piece.getSourcePlanOperationId() == null ? null
                : String.valueOf(piece.getSourcePlanOperationId()), "-") + "|"
                + segmentBatchNo + (includeFreezeInstruction
                ? "|" + firstNotBlank(piece.getFreezeInstructionId() == null ? null
                : String.valueOf(piece.getFreezeInstructionId()), "-") : "");
    }

    private String resolveSegmentBatchNo(HcNgInventoryPieceDO piece) {
        return firstNotBlank(piece.getSourceParentBatchNo(), piece.getSourceBatchNo(), piece.getPieceNo(), "-");
    }

    @Override
    public List<NgLocationGridRespVO> getLocationGrid() {
        List<HcNgInventoryLocationDO> locations = getDisplayLocations();
        List<HcNgInventoryPieceDO> pieces = hcNgInventoryPieceMapper.selectActiveListByLocationKeys(
                locations.stream().map(HcNgInventoryLocationDO::getLocationKey).toList());
        Map<String, List<HcNgInventoryPieceDO>> pieceMap = new HashMap<>();
        for (HcNgInventoryPieceDO piece : pieces) {
            pieceMap.computeIfAbsent(piece.getCurrentLocationCode(), key -> new ArrayList<>()).add(piece);
        }
        return locations.stream().map(location -> toLocationGrid(location,
                pieceMap.getOrDefault(location.getLocationKey(), List.of()))).toList();
    }

    @Override
    public List<NgLocationStockReportRespVO> getLocationStockReport(NgLocationStockReportReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new NgLocationStockReportReqVO();
        }
        List<HcNgInventoryLocationDO> locations = getDisplayLocations();
        String padType = StrUtil.isBlank(reqVO.getPadType()) ? null : normalizePadType(reqVO.getPadType());
        String storagePurpose = StrUtil.isBlank(reqVO.getStoragePurpose())
                ? null : normalizeStoragePurpose(reqVO.getStoragePurpose());
        String modelNo = StrUtil.trimToNull(reqVO.getModelNo());
        String locationKeyword = StrUtil.trimToNull(reqVO.getLocationKeyword());
        Map<Long, HcNgInventoryWarehouseDO> warehouseMap = hcNgInventoryWarehouseMapper.selectListAll().stream()
                .collect(java.util.stream.Collectors.toMap(HcNgInventoryWarehouseDO::getId, Function.identity()));
        Map<String, HcNgInventoryLocationDO> locationMap = locations.stream()
                .collect(java.util.stream.Collectors.toMap(HcNgInventoryLocationDO::getLocationKey, Function.identity()));
        Map<String, NgLocationStockReportRespVO> resultMap = new LinkedHashMap<>();
        for (HcNgInventoryPieceDO piece : hcNgInventoryPieceMapper.selectActiveListByLocationKeys(locationMap.keySet())) {
            HcNgInventoryLocationDO location = locationMap.get(piece.getCurrentLocationCode());
            HcNgInventoryWarehouseDO warehouse = location == null ? null : warehouseMap.get(location.getWarehouseId());
            if (location == null || warehouse == null) {
                continue;
            }
            String currentPurpose = resolveLocationPurpose(location);
            if ((padType != null && !padType.equals(piece.getPadType()))
                    || (storagePurpose != null && !storagePurpose.equals(currentPurpose))
                    || (modelNo != null && !StrUtil.containsIgnoreCase(firstNotBlank(piece.getModelNo(), "-"), modelNo))
                    || (locationKeyword != null && !matchesLocationKeyword(location, warehouse, locationKeyword))) {
                continue;
            }
            String key = String.join("|", location.getLocationKey(), firstNotBlank(piece.getPadType(), "-"), firstNotBlank(piece.getModelNo(), "-"),
                    firstNotBlank(piece.getMaterialCode(), "-"), firstNotBlank(piece.getMaterialName(), "-"),
                    firstNotBlank(piece.getStatus(), "-"));
            NgLocationStockReportRespVO report = resultMap.computeIfAbsent(key,
                    ignored -> newLocationStockReport(location, warehouse, currentPurpose, piece));
            report.setQuantity(normalizeQty(report.getQuantity()).add(normalizeQty(piece.getPieceQty())));
            report.setPieceCount((report.getPieceCount() == null ? 0 : report.getPieceCount()) + 1);
        }
        List<NgLocationStockReportRespVO> result = new ArrayList<>(resultMap.values());
        result.sort(Comparator.comparing((NgLocationStockReportRespVO item) -> firstNotBlank(item.getWarehouseCode(), ""))
                .thenComparing(item -> item.getRackNo() == null ? 0 : item.getRackNo())
                .thenComparing(item -> item.getLocationNo() == null ? 0 : item.getLocationNo())
                .thenComparing(item -> firstNotBlank(item.getModelNo(), ""))
                .thenComparing(item -> firstNotBlank(item.getMaterialCode(), ""))
                .thenComparing(item -> firstNotBlank(item.getStockStatus(), "")));
        return result;
    }

    @Override
    public List<NgLocationTreeWarehouseRespVO> getLocationTree() {
        List<HcNgInventoryWarehouseDO> warehouses = hcNgInventoryWarehouseMapper.selectListAll().stream()
                .filter(w -> List.of("NG_SLITTING", "NG_PRESS_SLOT", "NG_FREEZE").contains(w.getWarehouseCode())).toList();
        if (warehouses.isEmpty()) {
            return List.of();
        }
        List<Long> warehouseIds = warehouses.stream().map(HcNgInventoryWarehouseDO::getId).toList();
        List<HcNgInventoryRackDO> racks = hcNgInventoryRackMapper.selectListByWarehouseIds(warehouseIds);
        Map<Long, List<HcNgInventoryRackDO>> rackMap = groupBy(racks, HcNgInventoryRackDO::getWarehouseId);
        List<HcNgInventoryLocationDO> locations = getDisplayLocations();
        Map<Long, List<HcNgInventoryLocationDO>> locationMap = groupBy(locations, HcNgInventoryLocationDO::getRackId);
        Map<String, List<HcNgInventoryPieceDO>> pieceMap = buildPieceMap(locations);

        List<NgLocationTreeWarehouseRespVO> result = new ArrayList<>();
        for (HcNgInventoryWarehouseDO warehouse : warehouses) {
            NgLocationTreeWarehouseRespVO warehouseResp = new NgLocationTreeWarehouseRespVO();
            warehouseResp.setId(warehouse.getId());
            warehouseResp.setWarehouseCode(warehouse.getWarehouseCode());
            warehouseResp.setWarehouseName(warehouse.getWarehouseName());
            warehouseResp.setPadType(warehouse.getPadType());
            warehouseResp.setStatus(warehouse.getStatus());
            warehouseResp.setSortNo(warehouse.getSortNo());
            warehouseResp.setRemark(warehouse.getRemark());
            List<NgLocationTreeRackRespVO> rackResps = new ArrayList<>();
            for (HcNgInventoryRackDO rack : rackMap.getOrDefault(warehouse.getId(), List.of())) {
                NgLocationTreeRackRespVO rackResp = new NgLocationTreeRackRespVO();
                rackResp.setId(rack.getId());
                rackResp.setWarehouseId(rack.getWarehouseId());
                rackResp.setRackNo(rack.getRackNo());
                rackResp.setRackCode(resolveRackCode(rack));
                rackResp.setRackName(rack.getRackName());
                rackResp.setStatus(rack.getStatus());
                rackResp.setSortNo(rack.getSortNo());
                rackResp.setRemark(rack.getRemark());
                List<NgLocationGridRespVO> locationResps = locationMap.getOrDefault(rack.getId(), List.of()).stream()
                        .map(location -> toLocationGrid(location,
                                pieceMap.getOrDefault(location.getLocationKey(), List.of())))
                        .peek(location -> location.setPadType(warehouse.getPadType()))
                        .toList();
                HcNgInventoryLocationDO storage = locationMap.getOrDefault(rack.getId(), List.of()).stream()
                        .findFirst().orElse(null);
                int capacityQty = storage == null || storage.getCapacityQty() == null ? 0 : storage.getCapacityQty();
                int occupiedQty = storage == null ? 0
                        : pieceMap.getOrDefault(storage.getLocationKey(), List.of()).stream()
                        .mapToInt(this::pieceQtyAsInt).sum();
                rackResp.setCapacityQty(capacityQty);
                rackResp.setOccupiedQty(occupiedQty);
                rackResp.setAvailableQty(Math.max(0, capacityQty - occupiedQty));
                rackResp.setStorageKey(storage == null ? null : storage.getLocationKey());
                rackResp.setLocations(locationResps);
                rackResps.add(rackResp);
            }
            warehouseResp.setRacks(rackResps);
            result.add(warehouseResp);
        }
        return result;
    }

    private NgLocationStockReportRespVO newLocationStockReport(HcNgInventoryLocationDO location,
                                                                 HcNgInventoryWarehouseDO warehouse,
                                                                 String storagePurpose,
                                                                 HcNgInventoryPieceDO piece) {
        NgLocationStockReportRespVO report = new NgLocationStockReportRespVO();
        report.setPadType(piece.getPadType());
        report.setWarehouseCode(warehouse.getWarehouseCode());
        report.setWarehouseName(warehouse.getWarehouseName());
        report.setRackNo(parseInteger(location.getRackNo()));
        report.setLocationNo(location.getLocationNo());
        report.setStoragePurpose(storagePurpose);
        report.setLocationCode(location.getLocationCode());
        report.setLocationName(firstNotBlank(location.getLocationName(), location.getLocationCode()));
        report.setModelNo(piece.getModelNo());
        report.setMaterialCode(piece.getMaterialCode());
        report.setMaterialName(piece.getMaterialName());
        report.setStockStatus(piece.getStatus());
        report.setQuantity(BigDecimal.ZERO);
        report.setPieceCount(0);
        return report;
    }

    private boolean matchesLocationKeyword(HcNgInventoryLocationDO location,
                                           HcNgInventoryWarehouseDO warehouse, String keyword) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return java.util.stream.Stream.of(location.getLocationCode(), location.getLocationName(),
                        warehouse.getWarehouseCode(), warehouse.getWarehouseName(), location.getRackNo(),
                        location.getLocationNo() == null ? null : "库位" + location.getLocationNo())
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(normalizedKeyword));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveWarehouse(NgWarehouseSaveReqVO reqVO) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWarehouse(Long id) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRack(NgRackSaveReqVO reqVO) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRack(Long id) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shelf(NgShelfReqVO reqVO) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unshelf(NgUnshelfReqVO reqVO) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualOutbound(NgManualOutboundReqVO reqVO) {
        List<HcNgInventoryPieceDO> pieces = lockPieces(reqVO.getPieceIds());
        List<HcNgInventoryLocationDO> locations = lockAllNgLocations();
        Map<String, HcNgInventoryLocationDO> locationMap = toLocationMap(locations);
        Map<String, Integer> occupied = buildOccupiedMap(locations);
        String reason = StrUtil.trim(reqVO.getReason());
        LocalDateTime now = LocalDateTime.now();
        Operator operator = currentOperator();
        for (HcNgInventoryPieceDO piece : pieces) {
            String status = piece.getStatus();
            if (!List.of(STATUS_WAIT_SHELF, STATUS_WAIT_FREEZE_SHELF, STATUS_STORED, STATUS_FROZEN)
                    .contains(status)) {
                throw invalidParamException("仅待上架、待冻结上架、已上架或冻结不合格品可以批量出库");
            }
            if (STATUS_WAIT_SHELF.equals(status) || STATUS_WAIT_FREEZE_SHELF.equals(status)) {
                recordPendingManualOutboundTxn(piece, now, operator, "不合格品未上架出库：" + reason);
            } else {
                boolean frozen = STATUS_FROZEN.equals(status);
                HcNgInventoryLocationDO sourceLocation = locationMap.get(piece.getCurrentLocationCode());
                if (sourceLocation == null) {
                    throw invalidParamException("不合格品当前库位不存在，不能出库");
                }
                removePieceFromStock(piece, frozen, now, operator, "NG_MANUAL_OUTBOUND", null,
                        (frozen ? "冻结不合格品出库：" : "不合格品出库：") + reason);
                occupied.compute(sourceLocation.getLocationKey(),
                        (key, value) -> Math.max(0, (value == null ? 0 : value) - pieceQtyAsInt(piece)));
            }
            HcNgInventoryPieceDO update = new HcNgInventoryPieceDO();
            update.setId(piece.getId());
            update.setStatus(STATUS_OUTBOUNDED);
            update.setStockId(null);
            update.setCurrentWarehouseCode(null);
            update.setCurrentWarehouseName(null);
            update.setCurrentLocationCode(null);
            update.setCurrentLocationName(null);
            hcNgInventoryPieceMapper.updateById(update);
            syncManualRecordStatus(piece, STATUS_OUTBOUNDED);
        }
        updateOccupiedLocations(locationMap, occupied);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(NgTransferReqVO reqVO) {
        throw invalidParamException("已采用分切库、压槽库、冻结库自动入库，不再支持仓库货架维护、上架、下架或移库");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scrap(NgScrapReqVO reqVO) {
        List<HcNgInventoryPieceDO> pieces = lockPieces(reqVO.getPieceIds());
        List<HcNgInventoryLocationDO> locations = lockAllNgLocations();
        Map<String, HcNgInventoryLocationDO> locationMap = toLocationMap(locations);
        Map<String, Integer> occupied = buildOccupiedMap(locations);
        LocalDateTime now = LocalDateTime.now();
        Operator operator = currentOperator();
        for (HcNgInventoryPieceDO piece : pieces) {
            if (!STATUS_STORED.equals(piece.getStatus())) {
                throw invalidParamException("冻结或未上架不合格品不能直接报废");
            }
            HcNgInventoryLocationDO sourceLocation = locationMap.get(piece.getCurrentLocationCode());
            if (sourceLocation == null) {
                throw invalidParamException("不合格品当前库位不存在，不能报废");
            }
            removePieceFromStock(piece, false, now, operator, "NG_SCRAP", null,
                    "不合格品报废：" + StrUtil.trim(reqVO.getScrapReason()));
            occupied.compute(sourceLocation.getLocationKey(), (key, value) -> Math.max(0, (value == null ? 0 : value) - pieceQtyAsInt(piece)));
            HcNgInventoryPieceDO update = new HcNgInventoryPieceDO();
            update.setId(piece.getId());
            update.setStatus(STATUS_SCRAPPED);
            // 报废后不再属于当前 NG 货架；历史操作时库位由 mes_inv_txn_log 保留。
            update.setStockId(null);
            update.setCurrentWarehouseCode(null);
            update.setCurrentWarehouseName(null);
            update.setCurrentLocationCode(null);
            update.setCurrentLocationName(null);
            update.setScrapReason(StrUtil.trim(reqVO.getScrapReason()));
            update.setScrappedBy(operator.id());
            update.setScrappedByName(operator.name());
            update.setScrappedTime(now);
            hcNgInventoryPieceMapper.updateById(update);
            syncManualRecordStatus(piece, STATUS_SCRAPPED);
        }
        updateOccupiedLocations(locationMap, occupied);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NgPieceRespVO createManualPiece(NgManualPieceCreateReqVO reqVO) {
        return createManualPieceInternal(reqVO, "MANUAL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NgPieceRespVO updateManualPiece(NgManualPieceUpdateReqVO reqVO) {
        HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectByIdForUpdate(reqVO.getPieceId());
        if (piece == null || !SOURCE_TYPE_MANUAL_HISTORY.equals(piece.getSourceType())) {
            throw invalidParamException("历史补录片不存在或已删除，请刷新后重试");
        }
        if (!List.of(STATUS_WAIT_SHELF, STATUS_WAIT_FREEZE_SHELF).contains(piece.getStatus())
                || piece.getStockId() != null) {
            throw invalidParamException("仅未上架的历史补录片可以编辑");
        }
        HcNgManualPieceDO manual = hcNgManualPieceMapper.selectByIdForUpdate(piece.getSourceId());
        if (manual == null) {
            throw invalidParamException("历史补录来源不存在，不能编辑");
        }
        normalizeAndValidateManualPieceUpdateReq(reqVO);
        ensureManualPieceNoAvailableForUpdate(reqVO.getPieceNo(), piece.getId(), manual.getId());

        String processName = manualProcessName(reqVO.getProcessType());
        String waitStatus = MANUAL_STORAGE_FREEZE.equals(reqVO.getStorageTarget())
                ? STATUS_WAIT_FREEZE_SHELF : STATUS_WAIT_SHELF;
        hcNgManualPieceMapper.update(null, new LambdaUpdateWrapper<HcNgManualPieceDO>()
                .eq(HcNgManualPieceDO::getId, manual.getId())
                .set(HcNgManualPieceDO::getSegmentBatchNo, reqVO.getSegmentBatchNo())
                .set(HcNgManualPieceDO::getPieceNo, reqVO.getPieceNo())
                .set(HcNgManualPieceDO::getProcessType, reqVO.getProcessType())
                .set(HcNgManualPieceDO::getProcessName, processName)
                .set(HcNgManualPieceDO::getSourceBatchNo, reqVO.getSourceBatchNo())
                .set(HcNgManualPieceDO::getMaterialCode, reqVO.getMaterialCode())
                .set(HcNgManualPieceDO::getModelNo, reqVO.getModelNo())
                .set(HcNgManualPieceDO::getPadType, reqVO.getPadType())
                .set(HcNgManualPieceDO::getStorageTarget, reqVO.getStorageTarget())
                .set(HcNgManualPieceDO::getDefectSummary, reqVO.getDefectSummary())
                .set(HcNgManualPieceDO::getRecordStatus, waitStatus));

        Map<String, Object> defectDetail = new LinkedHashMap<>();
        defectDetail.put("historyBackfill", true);
        defectDetail.put("backfillReason", manual.getBackfillReason());
        defectDetail.put("inputMode", manual.getInputMode());
        defectDetail.put("storageTarget", reqVO.getStorageTarget());
        String entryReason = MANUAL_STORAGE_FREEZE.equals(reqVO.getStorageTarget())
                ? ENTRY_REASON_HISTORY_FREEZE_BACKFILL : ENTRY_REASON_HISTORY_BACKFILL;
        String defectDetailJson = JsonUtils.toJsonString(defectDetail);
        hcNgInventoryPieceMapper.update(null, new LambdaUpdateWrapper<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getId, piece.getId())
                .set(HcNgInventoryPieceDO::getProcessType, reqVO.getProcessType())
                .set(HcNgInventoryPieceDO::getProcessName, processName)
                .set(HcNgInventoryPieceDO::getPieceNo, reqVO.getPieceNo())
                .set(HcNgInventoryPieceDO::getSourceBatchNo, reqVO.getSourceBatchNo())
                .set(HcNgInventoryPieceDO::getSourceParentBatchNo, reqVO.getSegmentBatchNo())
                .set(HcNgInventoryPieceDO::getMaterialCode, reqVO.getMaterialCode())
                .set(HcNgInventoryPieceDO::getModelNo, reqVO.getModelNo())
                .set(HcNgInventoryPieceDO::getPadType, reqVO.getPadType())
                .set(HcNgInventoryPieceDO::getEntryReason, entryReason)
                .set(HcNgInventoryPieceDO::getDefectSummary, reqVO.getDefectSummary())
                .set(HcNgInventoryPieceDO::getDefectDetailJson, defectDetailJson)
                .set(HcNgInventoryPieceDO::getStatus, waitStatus));

        piece.setProcessType(reqVO.getProcessType());
        piece.setProcessName(processName);
        piece.setPieceNo(reqVO.getPieceNo());
        piece.setSourceBatchNo(reqVO.getSourceBatchNo());
        piece.setSourceParentBatchNo(reqVO.getSegmentBatchNo());
        piece.setMaterialCode(reqVO.getMaterialCode());
        piece.setModelNo(reqVO.getModelNo());
        piece.setPadType(reqVO.getPadType());
        piece.setEntryReason(entryReason);
        piece.setDefectSummary(reqVO.getDefectSummary());
        piece.setDefectDetailJson(defectDetailJson);
        piece.setStatus(waitStatus);
        return toPieceResp(piece, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteManualPiece(NgManualPieceDeleteReqVO reqVO) {
        HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectByIdForUpdate(reqVO.getPieceId());
        if (piece == null || !SOURCE_TYPE_MANUAL_HISTORY.equals(piece.getSourceType())) {
            throw invalidParamException("历史补录片不存在或已删除，请刷新后重试");
        }
        if (!List.of(STATUS_WAIT_SHELF, STATUS_WAIT_FREEZE_SHELF).contains(piece.getStatus())
                || piece.getStockId() != null) {
            throw invalidParamException("仅未上架的历史补录片可以删除");
        }
        HcNgManualPieceDO manual = hcNgManualPieceMapper.selectByIdForUpdate(piece.getSourceId());
        if (manual == null) {
            throw invalidParamException("历史补录来源不存在，不能删除");
        }
        String deleteReason = trimRequired(reqVO.getDeleteReason(), "删除原因", 500);
        LocalDateTime now = LocalDateTime.now();
        Operator operator = currentOperator();
        HcNgManualPieceDO update = new HcNgManualPieceDO();
        update.setId(manual.getId());
        update.setRecordStatus(MANUAL_RECORD_VOID);
        update.setDeleteReason(deleteReason);
        update.setDeleteUserName(operator.name());
        update.setDeleteTime(now);
        hcNgManualPieceMapper.updateById(update);
        if (hcNgInventoryPieceMapper.deleteById(piece.getId()) != 1) {
            throw invalidParamException("历史补录片状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NgManualPieceImportRespVO importManualPieces(MultipartFile file) throws IOException {
        NgManualPieceImportRespVO result = new NgManualPieceImportRespVO();
        if (file == null || file.isEmpty()) {
            addManualImportFailure(result, "导入文件为空");
            return result;
        }
        String fileName = StrUtil.blankToDefault(file.getOriginalFilename(), "分切压槽不合格品历史片导入.xlsx");
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            addManualImportFailure(result, "仅支持导入 .xlsx/.xls 文件");
            return result;
        }
        if (file.getSize() > MANUAL_PIECE_IMPORT_MAX_FILE_SIZE) {
            addManualImportFailure(result, "导入文件不能超过5MB");
            return result;
        }
        List<HcNgManualPieceImportExcelVO> excelRows = ExcelUtils.read(file, HcNgManualPieceImportExcelVO.class);
        List<NgManualPieceCreateReqVO> requests = new ArrayList<>();
        Set<String> filePieceNos = new HashSet<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcNgManualPieceImportExcelVO row = excelRows.get(index);
            if (isBlankManualImportRow(row)) {
                result.setSkippedRows(result.getSkippedRows() + 1);
                continue;
            }
            result.setTotalRows(result.getTotalRows() + 1);
            int rowNo = index + 2;
            if (result.getTotalRows() > MANUAL_PIECE_IMPORT_MAX_ROWS) {
                addManualImportFailure(result, String.format("有效数据超过%d行，请拆分文件后导入", MANUAL_PIECE_IMPORT_MAX_ROWS));
                break;
            }
            NgManualPieceCreateReqVO reqVO = new NgManualPieceCreateReqVO();
            reqVO.setSegmentBatchNo(row.getSegmentBatchNo());
            reqVO.setPieceNo(row.getPieceNo());
            reqVO.setProcessType(row.getProcessType());
            reqVO.setSourceBatchNo(row.getSourceBatchNo());
            reqVO.setMaterialCode(row.getMaterialCode());
            reqVO.setModelNo(row.getModelNo());
            reqVO.setPadType(row.getPadType());
            reqVO.setDefectSummary(row.getDefectSummary());
            reqVO.setStorageTarget(row.getStorageTarget());
            reqVO.setBackfillReason(row.getBackfillReason());
            reqVO.setRemark(row.getRemark());
            try {
                normalizeAndValidateManualPieceReq(reqVO, false);
                if (!filePieceNos.add(reqVO.getPieceNo())) {
                    throw invalidParamException("同一文件内片号 " + reqVO.getPieceNo() + " 重复");
                }
                ensureManualPieceNoAvailable(reqVO.getPieceNo());
                requests.add(reqVO);
            } catch (RuntimeException ex) {
                addManualImportFailure(result, String.format("第%d行：%s", rowNo,
                        firstNotBlank(ex.getMessage(), "校验失败")));
            }
        }
        if (result.getTotalRows() == 0) {
            addManualImportFailure(result, "Excel未读取到有效历史片数据");
        }
        if (!result.getFailures().isEmpty()) {
            result.setFailureCount(result.getFailures().size());
            result.getMessages().add("导入校验未通过，未写入任何历史片数据");
            return result;
        }
        for (NgManualPieceCreateReqVO request : requests) {
            createManualPieceInternal(request, "EXCEL_IMPORT");
        }
        result.setSuccessCount(requests.size());
        result.getMessages().add(String.format("历史不合格品导入完成：成功 %d 行", requests.size()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeManualPiece(NgManualPieceUnfreezeReqVO reqVO) {
        HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectByIdForUpdate(reqVO.getPieceId());
        if (piece == null || !SOURCE_TYPE_MANUAL_HISTORY.equals(piece.getSourceType())) {
            throw invalidParamException("历史冻结片不存在或已关闭，请刷新后重试");
        }
        if (!STATUS_FROZEN.equals(piece.getStatus())) {
            throw invalidParamException("仅已上架的历史冻结片可以人工解除冻结");
        }
        HcNgManualPieceDO manual = hcNgManualPieceMapper.selectByIdForUpdate(piece.getSourceId());
        if (manual == null) {
            throw invalidParamException("历史冻结来源不存在，不能解除冻结");
        }
        String unfreezeReason = trimRequired(reqVO.getUnfreezeReason(), "解除冻结原因", 500);
        List<HcNgInventoryLocationDO> locations = lockAllNgLocations();
        Map<String, HcNgInventoryLocationDO> locationMap = toLocationMap(locations);
        HcNgInventoryLocationDO sourceLocation = locationMap.get(piece.getCurrentLocationCode());
        if (sourceLocation == null) {
            throw invalidParamException("历史冻结片所在货架不存在，不能解除冻结");
        }
        Map<String, Integer> occupied = buildOccupiedMap(locations);
        LocalDateTime now = LocalDateTime.now();
        Operator operator = currentOperator();
        removePieceFromStock(piece, true, now, operator, "NG_TRANSFER_OUT", null,
                "历史冻结人工解除并归库：" + unfreezeReason);
        occupied.compute(sourceLocation.getLocationKey(),
                (key, value) -> Math.max(0, (value == null ? 0 : value) - pieceQtyAsInt(piece)));
        HcNgInventoryLocationDO target = requireFixedLocation(piece, false);
        HcInvStockDO stock = addPieceToLocation(piece, target, false, now, operator,
                "NG_TRANSFER_IN", null, "历史冻结片解冻自动归库：" + unfreezeReason);
        bindPieceStock(piece, stock, target, false, now, operator);
        occupied.merge(target.getLocationKey(), pieceQtyAsInt(piece), Integer::sum);
        HcNgManualPieceDO manualUpdate = new HcNgManualPieceDO();
        manualUpdate.setId(manual.getId());
        manualUpdate.setRecordStatus(STATUS_STORED);
        manualUpdate.setStorageTarget(MANUAL_STORAGE_NORMAL);
        manualUpdate.setUnfreezeReason(unfreezeReason);
        manualUpdate.setUnfrozenBy(operator.id());
        manualUpdate.setUnfrozenByName(operator.name());
        manualUpdate.setUnfrozenTime(now);
        hcNgManualPieceMapper.updateById(manualUpdate);
        updateOccupiedLocations(locationMap, occupied);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoUnfreezeByInstruction(HcProductionInstructionDO instruction) {
        if (instruction == null || instruction.getId() == null || instruction.getPlanId() == null
                || instruction.getPlanOperationId() == null || !INSTRUCTION_UNFREEZE.equals(instruction.getInstructionType())) {
            return;
        }
        List<HcNgInventoryPieceDO> pieces = hcNgInventoryPieceMapper.selectFrozenByPlanOperationForUpdate(
                instruction.getPlanId(), instruction.getPlanOperationId());
        if (pieces.isEmpty()) {
            return;
        }
        List<HcNgInventoryLocationDO> locations = lockAllNgLocations();
        Map<String, HcNgInventoryLocationDO> locationMap = toLocationMap(locations);
        Map<String, Integer> occupied = buildOccupiedMap(locations);
        LocalDateTime now = LocalDateTime.now();
        Operator operator = currentOperator();
        for (HcNgInventoryPieceDO piece : pieces) {
            HcNgInventoryLocationDO sourceLocation = locationMap.get(piece.getCurrentLocationCode());
            if (sourceLocation == null) {
                throw invalidParamException("冻结库位不存在，不能执行解冻退回");
            }
            removePieceFromStock(piece, true, now, operator,
                    QUALITY_STATUS_NG.equals(piece.getQualityResult()) ? "NG_TRANSFER_OUT" : "PLAN_UNFREEZE",
                    instruction.getId(), "解冻指令自动退回");
            occupied.compute(sourceLocation.getLocationKey(), (key, value) -> Math.max(0, (value == null ? 0 : value) - pieceQtyAsInt(piece)));
            HcNgInventoryPieceDO update = new HcNgInventoryPieceDO();
            update.setId(piece.getId());
            update.setUnfreezeInstructionId(instruction.getId());
            if (QUALITY_STATUS_NG.equals(piece.getQualityResult())) {
                HcNgInventoryLocationDO target = requireFixedLocation(piece, false);
                HcInvStockDO stock = addPieceToLocation(piece, target, false, now, operator,
                        "NG_TRANSFER_IN", instruction.getId(), "解冻后自动归入" + target.getLocationName());
                update.setStatus(STATUS_STORED);
                update.setStockId(stock.getId());
                update.setCurrentWarehouseCode(target.getWarehouseCode());
                update.setCurrentWarehouseName(target.getWarehouseName());
                update.setCurrentLocationCode(target.getLocationKey());
                update.setCurrentLocationName(target.getLocationName());
                occupied.merge(target.getLocationKey(), pieceQtyAsInt(piece), Integer::sum);
                updateSourceReturnState(piece, null, now, "解冻后 NG 已自动归入" + target.getLocationName());
            } else {
                HcInvStockDO returnedStock = postFrozenQualifiedPieceToWip(piece, now, operator, instruction.getId());
                update.setStatus(STATUS_RETURNED);
                update.setStockId(returnedStock.getId());
                update.setCurrentWarehouseCode(returnedStock.getWarehouseCode());
                update.setCurrentWarehouseName(returnedStock.getWarehouseName());
                update.setCurrentLocationCode(returnedStock.getLocationCode());
                update.setCurrentLocationName(returnedStock.getLocationName());
                updateSourceReturnState(piece, returnedStock, now, "解冻后合格品已自动退回中间边库");
            }
            hcNgInventoryPieceMapper.updateById(update);
        }
        updateOccupiedLocations(locationMap, occupied);
    }

    private void insertNgPiece(HcNgInventoryPieceDO piece) {
        insertDirectPiece(piece, false);
    }

    private void insertFrozenPiece(HcNgInventoryPieceDO piece) {
        insertDirectPiece(piece, true);
    }

    /** 入库与来源报工共享事务；定位锁保证同库并发数量一致。 */
    private void insertDirectPiece(HcNgInventoryPieceDO piece, boolean frozen) {
        HcNgInventoryLocationDO target = requireFixedLocation(piece, frozen);
        // 锁后再次检查来源，避免重复报工产生重复库存。
        if (hcNgInventoryPieceMapper.selectBySourceForUpdate(piece.getSourceType(), piece.getSourceId()) != null) {
            return;
        }
        piece.setStatus(frozen ? STATUS_FROZEN : STATUS_STORED);
        insertPiece(piece);
        LocalDateTime now = LocalDateTime.now();
        Operator operator = currentOperator();
        HcInvStockDO stock = addPieceToLocation(piece, target, frozen, now, operator,
                frozen ? "FREEZE_SHELF" : "NG_SHELF", piece.getFreezeInstructionId(), "自动入库：" + target.getLocationName());
        bindPieceStock(piece, stock, target, frozen, now, operator);
        syncManualRecordStatus(piece, piece.getStatus());
        updateLocationOccupied(target, (target.getOccupiedQty() == null ? 0 : target.getOccupiedQty()) + pieceQtyAsInt(piece));
    }

    private HcNgInventoryLocationDO requireFixedLocation(HcNgInventoryPieceDO piece, boolean frozen) {
        String key;
        try {
            key = HcNgFixedWarehousePolicy.locationKey(piece.getProcessType(), frozen);
        } catch (IllegalArgumentException ex) {
            throw invalidParamException(ex.getMessage());
        }
        HcNgInventoryLocationDO target = requireNgLocation(
                hcNgInventoryLocationMapper.selectByLocationKeyForUpdate(key));
        validateLocationHierarchyEnabled(target);
        if (!Objects.equals(target.getWarehouseCode(), HcNgFixedWarehousePolicy.warehouseCode(piece.getProcessType(), frozen))) {
            throw invalidParamException("固定仓库配置不一致，请核对迁移结果");
        }
        return target;
    }

    private void bindPieceStock(HcNgInventoryPieceDO piece, HcInvStockDO stock,
                               HcNgInventoryLocationDO target, boolean frozen, LocalDateTime now, Operator operator) {
        piece.setStatus(frozen ? STATUS_FROZEN : STATUS_STORED);
        piece.setStockId(stock.getId());
        piece.setCurrentWarehouseCode(target.getWarehouseCode());
        piece.setCurrentWarehouseName(target.getWarehouseName());
        piece.setCurrentLocationCode(target.getLocationKey());
        piece.setCurrentLocationName(target.getLocationName());
        piece.setShelvedBy(operator.id());
        piece.setShelvedByName(operator.name());
        piece.setShelvedTime(now);
        hcNgInventoryPieceMapper.updateById(piece);
    }

    private void insertPiece(HcNgInventoryPieceDO piece) {
        piece.setPieceNo(firstNotBlank(piece.getPieceNo(), "NG-" + piece.getSourceType() + "-" + piece.getSourceId()));
        piece.setSourceBatchNo(firstNotBlank(piece.getSourceBatchNo(), piece.getPieceNo()));
        piece.setPieceQty(normalizeQty(piece.getPieceQty()));
        hcNgInventoryPieceMapper.insert(piece);
    }

    private NgPieceRespVO createManualPieceInternal(NgManualPieceCreateReqVO reqVO, String inputMode) {
        normalizeAndValidateManualPieceReq(reqVO, true);
        Operator operator = currentOperator();
        LocalDateTime now = LocalDateTime.now();
        HcNgManualPieceDO manual = HcNgManualPieceDO.builder()
                .tenantId(currentTenantId())
                .segmentBatchNo(reqVO.getSegmentBatchNo())
                .pieceNo(reqVO.getPieceNo())
                .processType(reqVO.getProcessType())
                .processName(manualProcessName(reqVO.getProcessType()))
                .sourceBatchNo(reqVO.getSourceBatchNo())
                .materialCode(reqVO.getMaterialCode())
                .modelNo(reqVO.getModelNo())
                .padType(reqVO.getPadType())
                .storageTarget(reqVO.getStorageTarget())
                .defectSummary(reqVO.getDefectSummary())
                .recordStatus(MANUAL_STORAGE_FREEZE.equals(reqVO.getStorageTarget())
                        ? STATUS_WAIT_FREEZE_SHELF : STATUS_WAIT_SHELF)
                .inputMode(inputMode)
                .backfillReason(reqVO.getBackfillReason())
                .recorderName(operator.name())
                .recorderTime(now)
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .build();
        hcNgManualPieceMapper.insert(manual);
        Map<String, Object> defectDetail = new LinkedHashMap<>();
        defectDetail.put("historyBackfill", true);
        defectDetail.put("backfillReason", manual.getBackfillReason());
        defectDetail.put("inputMode", inputMode);
        defectDetail.put("storageTarget", manual.getStorageTarget());
        HcNgInventoryPieceDO piece = HcNgInventoryPieceDO.builder()
                .tenantId(manual.getTenantId())
                .sourceType(SOURCE_TYPE_MANUAL_HISTORY)
                .sourceTable(SOURCE_TABLE_MANUAL_PIECE)
                .sourceId(manual.getId())
                .processType(manual.getProcessType())
                .processName(manual.getProcessName())
                .pieceNo(manual.getPieceNo())
                .sourceBatchNo(manual.getSourceBatchNo())
                .sourceParentBatchNo(manual.getSegmentBatchNo())
                .materialCode(manual.getMaterialCode())
                .modelNo(manual.getModelNo())
                .padType(manual.getPadType())
                .pieceQty(PIECE_QTY)
                .entryReason(MANUAL_STORAGE_FREEZE.equals(manual.getStorageTarget())
                        ? ENTRY_REASON_HISTORY_FREEZE_BACKFILL : ENTRY_REASON_HISTORY_BACKFILL)
                .qualityResult(QUALITY_STATUS_NG)
                .defectSummary(manual.getDefectSummary())
                .defectDetailJson(JsonUtils.toJsonString(defectDetail))
                .build();
        if (MANUAL_STORAGE_FREEZE.equals(manual.getStorageTarget())) {
            insertFrozenPiece(piece);
        } else {
            insertNgPiece(piece);
        }
        HcNgManualPieceDO manualUpdate = new HcNgManualPieceDO();
        manualUpdate.setId(manual.getId());
        manualUpdate.setNgPieceId(piece.getId());
        hcNgManualPieceMapper.updateById(manualUpdate);
        return toPieceResp(piece, Map.of());
    }

    private void normalizeAndValidateManualPieceReq(NgManualPieceCreateReqVO reqVO, boolean verifySystemDuplicate) {
        reqVO.setSegmentBatchNo(normalizeManualText(reqVO.getSegmentBatchNo(), "段批次", 100));
        reqVO.setPieceNo(normalizeManualText(reqVO.getPieceNo(), "片号", 100));
        reqVO.setProcessType(normalizeManualProcessType(reqVO.getProcessType()));
        reqVO.setSourceBatchNo(normalizeManualText(reqVO.getSourceBatchNo(), "来源批号", 100));
        reqVO.setMaterialCode(normalizeOptionalManualText(reqVO.getMaterialCode(), "料号", 64));
        reqVO.setModelNo(normalizeManualText(reqVO.getModelNo(), "型号", 64));
        reqVO.setPadType(normalizeManualPadType(reqVO.getPadType()));
        reqVO.setStorageTarget(normalizeManualStorageTarget(reqVO.getStorageTarget()));
        reqVO.setDefectSummary(trimRequired(reqVO.getDefectSummary(), "NG原因", MAX_DEFECT_SUMMARY_LENGTH));
        reqVO.setBackfillReason(trimRequired(reqVO.getBackfillReason(), "补录原因", 500));
        if (StrUtil.length(StrUtil.trimToEmpty(reqVO.getRemark())) > 500) {
            throw invalidParamException("备注不能超过500个字符");
        }
        if (verifySystemDuplicate) {
            ensureManualPieceNoAvailable(reqVO.getPieceNo());
        }
    }

    private void normalizeAndValidateManualPieceUpdateReq(NgManualPieceUpdateReqVO reqVO) {
        reqVO.setSegmentBatchNo(normalizeManualText(reqVO.getSegmentBatchNo(), "段批次", 100));
        reqVO.setPieceNo(normalizeManualText(reqVO.getPieceNo(), "片号", 100));
        reqVO.setProcessType(normalizeManualProcessType(reqVO.getProcessType()));
        reqVO.setSourceBatchNo(normalizeManualText(reqVO.getSourceBatchNo(), "来源批号", 100));
        reqVO.setMaterialCode(normalizeOptionalManualText(reqVO.getMaterialCode(), "料号", 64));
        reqVO.setModelNo(normalizeManualText(reqVO.getModelNo(), "型号", 64));
        reqVO.setPadType(normalizeManualPadType(reqVO.getPadType()));
        reqVO.setStorageTarget(normalizeManualStorageTarget(reqVO.getStorageTarget()));
        reqVO.setDefectSummary(trimRequired(reqVO.getDefectSummary(), "NG原因", MAX_DEFECT_SUMMARY_LENGTH));
    }

    private void ensureManualPieceNoAvailable(String pieceNo) {
        if (hcNgInventoryPieceMapper.existsNotDeletedByPieceNo(pieceNo)) {
            throw invalidParamException("片号 " + pieceNo + " 已存在不合格品台账，不能重复补录");
        }
        if (hcNgManualPieceMapper.existsActiveByPieceNo(pieceNo)) {
            throw invalidParamException("片号 " + pieceNo + " 已存在有效历史补录记录，不能重复补录");
        }
    }

    private void ensureManualPieceNoAvailableForUpdate(String pieceNo, Long pieceId, Long manualId) {
        if (hcNgInventoryPieceMapper.existsNotDeletedByPieceNoExcludeId(pieceNo, pieceId)) {
            throw invalidParamException("片号 " + pieceNo + " 已存在不合格品台账，不能重复");
        }
        if (hcNgManualPieceMapper.existsActiveByPieceNoExcludeId(pieceNo, manualId)) {
            throw invalidParamException("片号 " + pieceNo + " 已存在有效历史补录记录，不能重复");
        }
    }

    private boolean isBlankManualImportRow(HcNgManualPieceImportExcelVO row) {
        if (row == null) {
            return true;
        }
        // Excel 选填列的空单元格会解析为 null，不能使用不接受 null 元素的 List.of。
        return Arrays.asList(row.getSegmentBatchNo(), row.getPieceNo(), row.getProcessType(), row.getSourceBatchNo(),
                        row.getModelNo(), row.getPadType(), row.getDefectSummary(), row.getStorageTarget(),
                        row.getBackfillReason(), row.getRemark(), row.getMaterialCode())
                .stream().allMatch(StrUtil::isBlank);
    }

    private void addManualImportFailure(NgManualPieceImportRespVO result, String failure) {
        result.getFailures().add(failure);
    }

    private String normalizeManualText(String value, String fieldName, int maxLength) {
        String normalized = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException(fieldName + "不能为空");
        }
        if (StrUtil.length(normalized) > maxLength) {
            throw invalidParamException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private String normalizeOptionalManualText(String value, String fieldName, int maxLength) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (StrUtil.length(normalized) > maxLength) {
            throw invalidParamException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private String trimRequired(String value, String fieldName, int maxLength) {
        String normalized = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException(fieldName + "不能为空");
        }
        if (StrUtil.length(normalized) > maxLength) {
            throw invalidParamException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private String normalizeManualProcessType(String value) {
        String normalized = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (List.of("分切", PROCESS_SLITTING).contains(normalized)) {
            return PROCESS_SLITTING;
        }
        if (List.of("压槽", PROCESS_PRESS_SLOT).contains(normalized)) {
            return PROCESS_PRESS_SLOT;
        }
        throw invalidParamException("工序仅支持 分切 或 压槽");
    }

    private String manualProcessName(String processType) {
        return PROCESS_PRESS_SLOT.equals(processType) ? "压槽" : "分切";
    }

    private String normalizeManualPadType(String value) {
        String normalized = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (List.of("黑垫", PAD_TYPE_BLACK).contains(normalized)) {
            return PAD_TYPE_BLACK;
        }
        if (List.of("白垫", PAD_TYPE_WHITE).contains(normalized)) {
            return PAD_TYPE_WHITE;
        }
        throw invalidParamException("垫型仅支持 黑垫 或 白垫");
    }

    private String normalizeManualStorageTarget(String value) {
        String normalized = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (List.of("普通不合格品", "普通", MANUAL_STORAGE_NORMAL).contains(normalized)) {
            return MANUAL_STORAGE_NORMAL;
        }
        if (List.of("冻结品", "冻结", MANUAL_STORAGE_FREEZE).contains(normalized)) {
            return MANUAL_STORAGE_FREEZE;
        }
        throw invalidParamException("入库类型仅支持 普通不合格品 或 冻结品");
    }

    private void syncManualRecordStatus(HcNgInventoryPieceDO piece, String status) {
        if (!SOURCE_TYPE_MANUAL_HISTORY.equals(piece.getSourceType()) || piece.getSourceId() == null) {
            return;
        }
        HcNgManualPieceDO update = new HcNgManualPieceDO();
        update.setId(piece.getSourceId());
        update.setRecordStatus(status);
        hcNgManualPieceMapper.updateById(update);
    }

    private PageResult<NgPieceRespVO> toRespPage(PageResult<HcNgInventoryPieceDO> page) {
        return new PageResult<>(toPieceRespList(page.getList()), page.getTotal());
    }

    private String resolveHistoryOperationName(String txnType) {
        if (txnType == null) {
            return "库存操作";
        }
        return switch (txnType) {
            case "NG_SHELF" -> "不合格品入库";
            case "FREEZE_SHELF" -> "冻结入库";
            case "NG_UNSHELF" -> "不合格品下架";
            case "HISTORY_FREEZE_UNSHELF" -> "历史冻结下架";
            case "NG_TRANSFER_OUT" -> "移库出库";
            case "NG_TRANSFER_IN" -> "移库入库";
            case "NG_SCRAP" -> "报废出库";
            case "NG_MANUAL_OUTBOUND" -> "不合格品出库";
            case "NG_REWORK_PICK" -> "返工下架";
            case "NG_REWORK_OUT" -> "返工出库";
            case "HISTORY_UNFREEZE_CLOSE" -> "历史冻结解除并关闭";
            case "PLAN_UNFREEZE" -> "解冻退回";
            default -> txnType;
        };
    }

    private String resolveHistoryTxnDirection(String txnType, BigDecimal txnQty) {
        if (List.of("NG_SHELF", "FREEZE_SHELF", "NG_TRANSFER_IN").contains(txnType)) {
            return "IN";
        }
        if (txnQty != null && txnQty.compareTo(BigDecimal.ZERO) < 0) {
            return "OUT";
        }
        return "IN";
    }

    private List<NgPieceRespVO> toPieceRespList(List<HcNgInventoryPieceDO> pieces) {
        Map<String, HcNgInventoryLocationDO> locationMap = hcNgInventoryLocationMapper.selectListByLocationKeys(
                        pieces.stream().map(HcNgInventoryPieceDO::getCurrentLocationCode)
                                .filter(StrUtil::isNotBlank).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(HcNgInventoryLocationDO::getLocationKey, Function.identity()));
        return pieces.stream().map(piece -> toPieceResp(piece, locationMap)).toList();
    }

    private NgPieceRespVO toPieceResp(HcNgInventoryPieceDO piece,
                                      Map<String, HcNgInventoryLocationDO> locationMap) {
        NgPieceRespVO resp = new NgPieceRespVO();
        resp.setId(piece.getId());
        resp.setSourceType(piece.getSourceType());
        resp.setProcessType(piece.getProcessType());
        resp.setProcessName(piece.getProcessName());
        resp.setPieceNo(piece.getPieceNo());
        resp.setSourcePlanNo(piece.getSourcePlanNo());
        resp.setSourcePlanId(piece.getSourcePlanId());
        resp.setSourcePlanOperationId(piece.getSourcePlanOperationId());
        resp.setSourceBatchNo(piece.getSourceBatchNo());
        resp.setSourceParentBatchNo(piece.getSourceParentBatchNo());
        resp.setMaterialCode(piece.getMaterialCode());
        resp.setMaterialName(piece.getMaterialName());
        resp.setModelNo(piece.getModelNo());
        resp.setPadType(piece.getPadType());
        resp.setPieceQty(piece.getPieceQty());
        resp.setEntryReason(piece.getEntryReason());
        resp.setQualityResult(piece.getQualityResult());
        resp.setDefectSummary(piece.getDefectSummary());
        resp.setDefectDetailJson(piece.getDefectDetailJson());
        resp.setStatus(piece.getStatus());
        String currentLocationCode = piece.getCurrentLocationCode();
        // 待上架历史片尚未分配库位；Map.of() 不允许以 null 作为查询键。
        HcNgInventoryLocationDO currentLocation = StrUtil.isBlank(currentLocationCode)
                ? null : locationMap.get(currentLocationCode);
        resp.setCurrentWarehouseName(currentLocation == null ? piece.getCurrentWarehouseName() : currentLocation.getWarehouseName());
        resp.setCurrentLocationKey(currentLocation == null ? null : currentLocation.getLocationKey());
        resp.setCurrentLocationCode(currentLocation == null ? piece.getCurrentLocationCode() : currentLocation.getLocationCode());
        resp.setCurrentLocationName(currentLocation == null ? piece.getCurrentLocationName()
                : firstNotBlank(currentLocation.getLocationName(), currentLocation.getLocationCode()));
        resp.setOriginalLocationName(piece.getOriginalLocationName());
        resp.setFreezeInstructionId(piece.getFreezeInstructionId());
        resp.setFreezeInstructionNo(piece.getFreezeInstructionNo());
        resp.setFreezeEffectiveTime(piece.getFreezeEffectiveTime());
        resp.setUnfreezeInstructionId(piece.getUnfreezeInstructionId());
        resp.setScrapReason(piece.getScrapReason());
        resp.setShelvedTime(piece.getShelvedTime());
        resp.setScrappedTime(piece.getScrappedTime());
        resp.setPrintCount(normalizePrintCount(piece.getPrintCount()));
        resp.setLastPrintTime(piece.getLastPrintTime());
        return resp;
    }

    private NgLocationGridRespVO toLocationGrid(HcNgInventoryLocationDO location, List<HcNgInventoryPieceDO> pieces) {
        int capacity = location.getCapacityQty() == null ? 30 : location.getCapacityQty();
        int occupied = pieces.stream().mapToInt(this::pieceQtyAsInt).sum();
        NgLocationGridRespVO resp = new NgLocationGridRespVO();
        resp.setId(location.getId());
        resp.setLocationKey(location.getLocationKey());
        resp.setLocationCode(location.getLocationCode());
        resp.setLocationName(firstNotBlank(location.getLocationName(), location.getLocationCode()));
        resp.setWarehouseName(location.getWarehouseName());
        resp.setWarehouseCode(location.getWarehouseCode());
        resp.setWarehouseId(location.getWarehouseId());
        resp.setRackId(location.getRackId());
        resp.setRackNo(location.getRackNo());
        resp.setStoragePurpose(resolveLocationPurpose(location));
        resp.setLocationNo(location.getLocationNo());
        resp.setCapacityQty(capacity);
        resp.setOccupiedQty(occupied);
        resp.setAvailableQty(Math.max(0, capacity - occupied));
        resp.setStatus(location.getStatus());
        resp.setPieceNos(pieces.stream().map(HcNgInventoryPieceDO::getPieceNo).filter(StrUtil::isNotBlank).toList());
        return resp;
    }

    private List<HcNgInventoryPieceDO> lockPieces(Collection<Long> ids) {
        List<Long> normalizedIds = ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (normalizedIds.isEmpty()) {
            throw invalidParamException("请选择不合格品");
        }
        List<HcNgInventoryPieceDO> pieces = hcNgInventoryPieceMapper.selectListByIdsForUpdate(normalizedIds);
        if (pieces.size() != normalizedIds.size()) {
            throw invalidParamException("部分不合格品不存在或已被处理，请刷新后重试");
        }
        return pieces;
    }

    private HcNgInventoryLocationDO requireNgLocation(HcNgInventoryLocationDO location) {
        if (location == null || !"启用".equals(location.getStatus())) {
            throw invalidParamException("固定仓库不存在或未启用，请先完成初始化和迁移");
        }
        return requireNgLocationIdentity(location);
    }

    /**
     * 下架、移库需要识别已停用的来源库位，但不允许把它作为新的上架目标。
     */
    private HcNgInventoryLocationDO requireNgLocationIdentity(HcNgInventoryLocationDO location) {
        if (location == null) {
            throw invalidParamException("不合格品货架不存在");
        }
        if (StrUtil.isBlank(location.getLocationKey())) {
            throw invalidParamException("不合格品货架内部定位键不正确");
        }
        return location;
    }

    private List<HcNgInventoryLocationDO> requireNgLocations(List<HcNgInventoryLocationDO> locations) {
        if (locations == null || locations.isEmpty()) {
            throw invalidParamException("不合格品货架基础数据为空，请先维护仓库和货架");
        }
        List<HcNgInventoryLocationDO> sorted = locations.stream().map(this::requireNgLocationIdentity)
                .sorted(Comparator.comparing(HcNgInventoryLocationDO::getGridNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (sorted.stream().map(HcNgInventoryLocationDO::getLocationKey).distinct().count() != sorted.size()) {
            throw invalidParamException("不合格品货架内部定位键重复");
        }
        return sorted;
    }

    /**
     * 库位管理页面需要保留停用库位及其历史库存的可见性；停用仅限制后续上架目标，
     * 不能导致整张库位网格和仓库树无法加载。
     */
    private List<HcNgInventoryLocationDO> getDisplayLocations() {
        return hcNgInventoryLocationMapper.selectActiveList().stream()
                .filter(location -> HcNgFixedWarehousePolicy.isFixedLocation(location.getLocationKey()))
                .filter(location -> location.getWarehouseId() != null
                        && location.getRackId() != null
                        && location.getLocationNo() != null
                        && StrUtil.isNotBlank(location.getStoragePurpose())
                        && StrUtil.isNotBlank(location.getLocationKey()))
                .sorted(Comparator.comparing(HcNgInventoryLocationDO::getGridNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<HcNgInventoryLocationDO> lockAllNgLocations() {
        return requireNgLocations(hcNgInventoryLocationMapper.selectActiveListForUpdate());
    }

    private Map<String, HcNgInventoryLocationDO> toLocationMap(List<HcNgInventoryLocationDO> locations) {
        Map<String, HcNgInventoryLocationDO> result = new LinkedHashMap<>();
        locations.forEach(location -> result.put(location.getLocationKey(), location));
        return result;
    }

    private Map<String, Integer> buildOccupiedMap(List<HcNgInventoryLocationDO> locations) {
        // 调用方持有库位行锁，使用最新占用计数，避免 RR 快照读漏掉等待锁期间已提交的入库。
        Map<String, Integer> occupied = new LinkedHashMap<>();
        locations.forEach(location -> occupied.put(location.getLocationKey(),
                location.getOccupiedQty() == null ? 0 : location.getOccupiedQty()));
        return occupied;
    }

    private void validateShelfTarget(HcNgInventoryPieceDO piece, HcNgInventoryLocationDO target) {
        // 分切 NG、压槽 NG 与冻结品共享同一货架容量，仅保留垫型隔离。
        validatePiecePadType(piece, target);
    }

    private void validateFreezeShelfTarget(HcNgInventoryLocationDO target) {
        // 冻结状态由不合格品台账记录，不再占用独立的货架用途。
    }

    private HcNgInventoryLocationDO allocateLocation(List<HcNgInventoryLocationDO> locations, Map<String, Integer> occupied,
                                                       String padType, int qty) {
        for (HcNgInventoryLocationDO location : locations) {
            if (location.getWarehouseId() != null && !Objects.equals(padType, resolveLocationPadType(location))) {
                continue;
            }
            int used = occupied.getOrDefault(location.getLocationKey(), 0);
            int capacity = location.getCapacityQty() == null ? 30 : location.getCapacityQty();
            if (used + qty <= capacity) {
                return location;
            }
        }
        throw invalidParamException("目标货架容量不足，无法完成本次移库");
    }

    private HcNgInventoryLocationDO resolveUnfreezeLocation(HcNgInventoryPieceDO piece, List<HcNgInventoryLocationDO> locations,
                                                             Map<String, HcNgInventoryLocationDO> locationMap, Map<String, Integer> occupied) {
        HcNgInventoryLocationDO original = locationMap.get(piece.getOriginalLocationCode());
        int qty = pieceQtyAsInt(piece);
        int originalCapacity = original == null || original.getCapacityQty() == null ? 30 : original.getCapacityQty();
        if (original != null && occupied.getOrDefault(original.getLocationKey(), 0) + qty <= originalCapacity) {
            return original;
        }
        return allocateLocation(locations, occupied, piece.getPadType(), qty);
    }

    private HcInvStockDO addPieceToLocation(HcNgInventoryPieceDO piece, HcNgInventoryLocationDO location, boolean frozen,
                                            LocalDateTime now, Operator operator, String txnType, Long instructionId,
                                            String remark) {
        // 使用片号作为库存批次键，原来源批次仍保留 sourceBatchNo，避免归并后不同片的质量互相覆盖。
        HcInvStockDO stock = piece.getMaterialId() == null ? null : hcInvStockMapper.selectOneByStockKeyForUpdate(
                resolveLocationWarehouseCode(location), piece.getMaterialId(), piece.getPieceNo(), location.getLocationKey());
        if (stock != null && (normalizeQty(stock.getOnHandQty()).signum() != 0
                || !Objects.equals(stock.getSourceId(), piece.getId()))) {
            throw invalidParamException("该片固定库库存已存在或库存来源不一致，不能重复入库");
        }
        BigDecimal qty = normalizeQty(piece.getPieceQty());
        String qualityStatus = resolvePieceQualityStatus(piece);
        String businessStatus = frozen
                ? QUALITY_STATUS_NG.equals(qualityStatus) ? BIZ_STATUS_NG_FROZEN : BIZ_STATUS_OK_FROZEN
                : BIZ_STATUS_NG_STORED;
        if (stock == null) {
            String txnNo = buildTxnNo(txnType, piece.getId(), now);
            stock = HcInvStockDO.builder()
                    .tenantId(piece.getTenantId())
                    .stockType(STOCK_TYPE_WIP)
                    .sourceType(SOURCE_TYPE_NG)
                    .sourceTable(SOURCE_TABLE_NG_PIECE)
                    .sourceId(piece.getId())
                    .sourceReportId(piece.getSourceReportId())
                    .sourcePlanId(piece.getSourcePlanId())
                    .sourcePlanNo(piece.getSourcePlanNo())
                    .sourcePlanOperationId(piece.getSourcePlanOperationId())
                    .sourceBatchNo(piece.getSourceBatchNo())
                    .sourceParentBatchNo(piece.getSourceParentBatchNo())
                    .warehouseCode(resolveLocationWarehouseCode(location))
                    .warehouseName(resolveLocationWarehouseName(location))
                    .locationCode(location.getLocationKey())
                    .locationName(location.getLocationName())
                    .materialId(piece.getMaterialId())
                    .materialCode(piece.getMaterialCode())
                    .materialName(piece.getMaterialName())
                    .modelNo(piece.getModelNo())
                    .opCode(piece.getProcessType())
                    .opName(piece.getProcessName())
                    .segmentCode("NG")
                    .segmentName("不合格品")
                    .batchNo(piece.getPieceNo())
                    .productionDate(SOURCE_TYPE_MANUAL_HISTORY.equals(piece.getSourceType()) ? null : LocalDate.now())
                    .onHandQty(qty)
                    .availableQty(BigDecimal.ZERO)
                    .shareableQty(BigDecimal.ZERO)
                    .frozenQty(frozen ? qty : BigDecimal.ZERO)
                    .planLockedQty(BigDecimal.ZERO)
                    .qualityStatus(qualityStatus)
                    .bizStatus(businessStatus)
                    .businessRemark(buildStockBusinessRemark(resolveStockRemark(piece)))
                    .uom("pcs")
                    .lastTxnNo(txnNo)
                    .lastTxnTime(now)
                    .build();
            hcInvStockMapper.insert(stock);
            insertTxn(stock, qty, BigDecimal.ZERO, qty, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, frozen ? qty : BigDecimal.ZERO, txnType, now, instructionId, operator, remark);
            return stock;
        }
        BigDecimal beforeQty = normalizeQty(stock.getOnHandQty());
        BigDecimal beforeFrozen = normalizeQty(stock.getFrozenQty());
        BigDecimal afterQty = beforeQty.add(qty);
        BigDecimal afterFrozen = frozen ? beforeFrozen.add(qty) : beforeFrozen;
        String txnNo = buildTxnNo(txnType, piece.getId(), now);
        HcInvStockDO update = new HcInvStockDO();
        update.setId(stock.getId());
        update.setOnHandQty(afterQty);
        update.setAvailableQty(BigDecimal.ZERO);
        update.setShareableQty(BigDecimal.ZERO);
        update.setFrozenQty(afterFrozen);
        update.setPlanLockedQty(BigDecimal.ZERO);
        update.setQualityStatus(qualityStatus);
        update.setBizStatus(businessStatus);
        update.setLastTxnNo(txnNo);
        update.setLastTxnTime(now);
        hcInvStockMapper.updateById(update);
        stock.setOnHandQty(afterQty);
        stock.setFrozenQty(afterFrozen);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(now);
        insertTxn(stock, qty, beforeQty, afterQty, BigDecimal.ZERO, BigDecimal.ZERO,
                beforeFrozen, afterFrozen, txnType, now, instructionId, operator, remark);
        return stock;
    }

    private void removePieceFromStock(HcNgInventoryPieceDO piece, boolean frozen, LocalDateTime now,
                                      Operator operator, String txnType, Long instructionId, String remark) {
        if (piece.getStockId() == null) {
            throw invalidParamException("不合格品库存余额不存在，不能处理");
        }
        HcInvStockDO stock = hcInvStockMapper.selectByIdForUpdate(piece.getStockId());
        if (stock == null) {
            throw invalidParamException("不合格品库存余额不存在，不能处理");
        }
        BigDecimal qty = normalizeQty(piece.getPieceQty());
        BigDecimal beforeQty = normalizeQty(stock.getOnHandQty());
        BigDecimal beforeFrozen = normalizeQty(stock.getFrozenQty());
        if (beforeQty.compareTo(qty) < 0 || (frozen && beforeFrozen.compareTo(qty) < 0)) {
            throw invalidParamException("不合格品库存数量不足，不能处理");
        }
        BigDecimal afterQty = beforeQty.subtract(qty);
        BigDecimal afterFrozen = frozen ? beforeFrozen.subtract(qty) : beforeFrozen;
        String txnNo = buildTxnNo(txnType, piece.getId(), now);
        HcInvStockDO update = new HcInvStockDO();
        update.setId(stock.getId());
        update.setOnHandQty(afterQty);
        update.setAvailableQty(BigDecimal.ZERO);
        update.setShareableQty(BigDecimal.ZERO);
        update.setFrozenQty(afterFrozen);
        update.setPlanLockedQty(BigDecimal.ZERO);
        update.setBizStatus(afterQty.compareTo(BigDecimal.ZERO) == 0 ? BIZ_STATUS_NG_EMPTY
                : frozen ? QUALITY_STATUS_OK.equals(stock.getQualityStatus()) ? BIZ_STATUS_OK_FROZEN : BIZ_STATUS_NG_FROZEN
                : BIZ_STATUS_NG_STORED);
        update.setLastTxnNo(txnNo);
        update.setLastTxnTime(now);
        hcInvStockMapper.updateById(update);
        stock.setOnHandQty(afterQty);
        stock.setFrozenQty(afterFrozen);
        stock.setLastTxnNo(txnNo);
        stock.setLastTxnTime(now);
        insertTxn(stock, qty.negate(), beforeQty, afterQty, BigDecimal.ZERO, BigDecimal.ZERO,
                beforeFrozen, afterFrozen, txnType, now, instructionId, operator, remark);
    }

    /**
     * 未上架片尚未生成 {@code mes_inv_stock} 余额，出库时仍须写入可追溯的出库事实，
     * 但不得虚构货架库存或扣减货架占用。
     */
    private void recordPendingManualOutboundTxn(HcNgInventoryPieceDO piece, LocalDateTime now,
                                                Operator operator, String remark) {
        BigDecimal qty = normalizeQty(piece.getPieceQty());
        HcInvTxnLogDO txn = HcInvTxnLogDO.builder()
                .tenantId(piece.getTenantId())
                .stockType(STOCK_TYPE_WIP)
                .txnNo(buildTxnNo("NG_MANUAL_OUTBOUND", piece.getId(), now))
                .txnType("NG_MANUAL_OUTBOUND")
                .txnTime(now)
                .materialId(piece.getMaterialId())
                .materialCode(piece.getMaterialCode())
                .materialName(piece.getMaterialName())
                .modelNo(piece.getModelNo())
                .batchNo(piece.getPieceNo())
                .txnQty(qty.negate())
                .beforeQty(BigDecimal.ZERO)
                .afterQty(BigDecimal.ZERO)
                .beforeAvailableQty(BigDecimal.ZERO)
                .afterAvailableQty(BigDecimal.ZERO)
                .beforeFrozenQty(BigDecimal.ZERO)
                .afterFrozenQty(BigDecimal.ZERO)
                .beforePlanLockedQty(BigDecimal.ZERO)
                .afterPlanLockedQty(BigDecimal.ZERO)
                .uom("pcs")
                .refDocType(SOURCE_TYPE_NG)
                .sourceType(SOURCE_TYPE_NG)
                .sourceTable(SOURCE_TABLE_NG_PIECE)
                .sourceId(piece.getId())
                .sourceBatchNo(piece.getSourceBatchNo())
                .sourcePlanId(piece.getSourcePlanId())
                .sourcePlanNo(piece.getSourcePlanNo())
                .sourcePlanOperationId(piece.getSourcePlanOperationId())
                .creatorName(operator.name())
                .remark(remark)
                .build();
        hcInvTxnLogMapper.insert(txn);
    }

    private void insertTxn(HcInvStockDO stock, BigDecimal txnQty, BigDecimal beforeQty, BigDecimal afterQty,
                           BigDecimal beforeAvailable, BigDecimal afterAvailable, BigDecimal beforeFrozen,
                           BigDecimal afterFrozen, String txnType, LocalDateTime now, Long instructionId,
                           Operator operator, String remark) {
        HcInvTxnLogDO txn = HcInvTxnLogDO.builder()
                .tenantId(stock.getTenantId())
                .stockId(stock.getId())
                .stockType(STOCK_TYPE_WIP)
                .txnNo(firstNotBlank(stock.getLastTxnNo(), buildTxnNo(txnType, stock.getId(), now)))
                .txnType(txnType)
                .txnTime(now)
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .locationCode(stock.getLocationCode())
                .materialId(stock.getMaterialId())
                .materialCode(stock.getMaterialCode())
                .materialName(stock.getMaterialName())
                .modelNo(stock.getModelNo())
                .batchNo(stock.getBatchNo())
                .txnQty(txnQty)
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .beforeAvailableQty(beforeAvailable)
                .afterAvailableQty(afterAvailable)
                .beforeFrozenQty(beforeFrozen)
                .afterFrozenQty(afterFrozen)
                .beforePlanLockedQty(BigDecimal.ZERO)
                .afterPlanLockedQty(BigDecimal.ZERO)
                .uom("pcs")
                .refDocType(instructionId == null ? SOURCE_TYPE_NG : "PRODUCTION_INSTRUCTION")
                .refDocId(instructionId)
                .refDocNo(instructionId == null ? null : String.valueOf(instructionId))
                .sourceType(SOURCE_TYPE_NG)
                .sourceTable(SOURCE_TABLE_NG_PIECE)
                .sourceId(stock.getSourceId())
                .sourceBatchNo(stock.getSourceBatchNo())
                .sourcePlanId(stock.getSourcePlanId())
                .sourcePlanNo(stock.getSourcePlanNo())
                .sourcePlanOperationId(stock.getSourcePlanOperationId())
                .creatorName(operator.name())
                .remark(remark)
                .build();
        hcInvTxnLogMapper.insert(txn);
    }

    private HcInvStockDO postFrozenQualifiedPieceToWip(HcNgInventoryPieceDO piece, LocalDateTime now,
                                                        Operator operator, Long instructionId) {
        HcPlanOrderDO plan = hcPlanOrderMapper.selectById(piece.getSourcePlanId());
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(piece.getSourcePlanOperationId());
        if (plan == null || operation == null) {
            throw invalidParamException("冻结合格品缺少计划或工序信息，不能自动退回中间边库");
        }
        return hcInvStockService.postProcessOutputWip(HcWipOutputPostReq.builder()
                .plan(plan)
                .operation(operation)
                .sourceType(piece.getSourceType())
                .sourceTable(piece.getSourceTable())
                .sourceId(piece.getSourceId())
                .sourceReportId(piece.getSourceReportId())
                .batchNo(firstNotBlank(piece.getPieceNo(), piece.getSourceBatchNo()))
                .parentBatchNo(piece.getSourceParentBatchNo())
                .outputQty(normalizeQty(piece.getPieceQty()))
                .uom("pcs")
                .materialId(piece.getMaterialId())
                .materialCode(piece.getMaterialCode())
                .materialName(piece.getMaterialName())
                .modelNo(piece.getModelNo())
                .qualityStatus(QUALITY_STATUS_OK)
                .businessRemark("解冻指令自动退回中间边库；冻结指令：" + firstNotBlank(piece.getFreezeInstructionNo(), "-"))
                .txnRemark("解冻指令自动退回中间边库")
                .postTime(now)
                .operatorId(operator.id())
                .operatorName(operator.name())
                .build());
    }

    private void updateSourceReturnState(HcNgInventoryPieceDO piece, HcInvStockDO returnedStock,
                                         LocalDateTime now, String message) {
        boolean returnedToWip = returnedStock != null;
        if (SOURCE_TYPE_SLITTING.equals(piece.getSourceType())) {
            HcSlittingSliceRecordDO update = new HcSlittingSliceRecordDO();
            update.setId(piece.getSourceId());
            update.setOutputStockId(returnedToWip ? returnedStock.getId() : null);
            update.setOutputStockPostStatus(returnedToWip ? "POSTED" : "NG_STORED");
            update.setOutputStockPostTime(now);
            update.setOutputStockPostMessage(message);
            hcSlittingSliceRecordMapper.updateById(update);
            return;
        }
        if (SOURCE_TYPE_PRESS_SLOT.equals(piece.getSourceType())) {
            HcPressSlotReportDO update = new HcPressSlotReportDO();
            update.setId(piece.getSourceId());
            update.setOutputStockId(returnedToWip ? returnedStock.getId() : null);
            update.setOutputStockPostStatus(returnedToWip ? "POSTED" : "NG_STORED");
            update.setOutputStockPostTime(now);
            update.setOutputStockPostMessage(message);
            hcPressSlotReportMapper.updateById(update);
        }
    }

    private String resolvePieceQualityStatus(HcNgInventoryPieceDO piece) {
        return QUALITY_STATUS_OK.equals(piece.getQualityResult()) ? QUALITY_STATUS_OK : QUALITY_STATUS_NG;
    }

    private String resolveStockRemark(HcNgInventoryPieceDO piece) {
        if (QUALITY_STATUS_OK.equals(resolvePieceQualityStatus(piece))) {
            return "合格品冻结；指令：" + firstNotBlank(piece.getFreezeInstructionNo(), "-");
        }
        return piece.getDefectSummary();
    }

    private boolean isSlittingSourceNg(HcSlittingSliceRecordDO source) {
        return isNgResultText(source.getSelfCheck())
                || StrUtil.isNotBlank(extractSlittingNgItemNames(source.getVisualResultJson()));
    }

    private boolean isPressSlotSourceNg(HcPressSlotReportDO source) {
        if (isNgResultText(source.getSelfCheck())) {
            return true;
        }
        String defectCode = StrUtil.trimToNull(source.getDefectCode());
        return defectCode != null && !"OK".equalsIgnoreCase(defectCode);
    }

    private void ensureCapacity(HcNgInventoryLocationDO location, int occupied, int required) {
        int capacity = location.getCapacityQty() == null ? 30 : location.getCapacityQty();
        if (occupied + required > capacity) {
            throw invalidParamException("货架 " + location.getLocationName() + " 容量不足，最大 " + capacity + " 片");
        }
    }

    private void updateOccupiedLocations(Map<String, HcNgInventoryLocationDO> locations, Map<String, Integer> occupied) {
        locations.forEach((code, location) -> updateLocationOccupied(location, occupied.getOrDefault(code, 0)));
    }

    private void updateLocationOccupied(HcNgInventoryLocationDO location, int occupied) {
        HcNgInventoryLocationDO update = new HcNgInventoryLocationDO();
        update.setId(location.getId());
        update.setOccupiedQty(Math.max(0, occupied));
        hcNgInventoryLocationMapper.updateById(update);
    }

    /** 修改仓库编码后，按所属货架同步货架容量展示信息。 */
    private void syncWarehousePresentation(HcNgInventoryWarehouseDO warehouse, String oldWarehouseCode) {
        List<HcNgInventoryRackDO> racks = hcNgInventoryRackMapper.selectListByWarehouseIds(List.of(warehouse.getId()));
        Map<Long, HcNgInventoryRackDO> rackMap = racks.stream()
                .collect(java.util.stream.Collectors.toMap(HcNgInventoryRackDO::getId, Function.identity()));
        for (HcNgInventoryLocationDO location : hcNgInventoryLocationMapper.selectListByWarehouseId(warehouse.getId())) {
            HcNgInventoryRackDO rack = rackMap.get(location.getRackId());
            if (rack != null) {
                refreshRackStoragePresentation(location, warehouse, rack, location.getCapacityQty(), true);
            }
        }
        hcInvStockMapper.update(null, new LambdaUpdateWrapper<HcInvStockDO>()
                .eq(HcInvStockDO::getSourceType, SOURCE_TYPE_NG)
                .eq(HcInvStockDO::getWarehouseCode, oldWarehouseCode)
                .set(HcInvStockDO::getWarehouseCode, warehouse.getWarehouseCode())
                .set(HcInvStockDO::getWarehouseName, warehouse.getWarehouseName()));
    }

    /** 修改货架编号或货架编码后，同步隐藏存储定位记录，不改变其唯一键。 */
    private void syncRackPresentation(HcNgInventoryRackDO rack) {
        HcNgInventoryWarehouseDO warehouse = requireWarehouse(rack.getWarehouseId());
        for (HcNgInventoryLocationDO location : hcNgInventoryLocationMapper.selectListByRackId(rack.getId())) {
            refreshRackStoragePresentation(location, warehouse, rack, location.getCapacityQty(), true);
        }
    }

    /**
     * 将隐藏存储定位记录展示为货架；库存、二维码仍通过不可变 location_key 定位。
     */
    private void refreshRackStoragePresentation(HcNgInventoryLocationDO location,
                                                HcNgInventoryWarehouseDO warehouse,
                                                HcNgInventoryRackDO rack,
                                                Integer capacityQty,
                                                boolean overwriteLocationName) {
        String oldLocationCode = location.getLocationCode();
        String locationCode = buildRackStorageCode(warehouse, rack);
        location.setLocationCode(locationCode);
        if (overwriteLocationName || StrUtil.isBlank(location.getLocationName())
                || Objects.equals(StrUtil.trim(location.getLocationName()), StrUtil.trim(oldLocationCode))) {
            location.setLocationName(firstNotBlank(rack.getRackName(), locationCode));
        }
        location.setWarehouseCode(warehouse.getWarehouseCode());
        location.setWarehouseName(warehouse.getWarehouseName());
        location.setRackNo(String.valueOf(rack.getRackNo()));
        location.setLocationNo(0);
        location.setStoragePurpose(PURPOSE_MIXED);
        location.setCapacityQty(capacityQty);
        location.setStatus(rack.getStatus());
        location.setGridNo(buildRackGridNo(rack.getRackNo()));
        hcNgInventoryLocationMapper.updateById(location);
        syncLocationPresentation(location);
    }

    /**
     * 每个货架仅保留一条隐藏的库存定位记录，承载货架总容量、占用数及不可变扫码键。
     */
    private HcNgInventoryLocationDO ensureRackStorageLocation(HcNgInventoryRackDO rack,
                                                               HcNgInventoryWarehouseDO warehouse,
                                                               Integer capacityQty) {
        List<HcNgInventoryLocationDO> locations = hcNgInventoryLocationMapper.selectListByRackId(rack.getId());
        if (locations.size() > 1) {
            throw invalidParamException("货架存在历史库位，请先执行货架容量合并迁移后再维护");
        }
        if (locations.isEmpty()) {
            HcNgInventoryLocationDO location = new HcNgInventoryLocationDO();
            location.setTenantId(currentTenantId());
            location.setLocationKey(buildRackStorageKey());
            location.setWarehouseId(warehouse.getId());
            location.setRackId(rack.getId());
            location.setOccupiedQty(0);
            location.setLocationNo(0);
            location.setStoragePurpose(PURPOSE_MIXED);
            location.setCapacityQty(capacityQty);
            location.setStatus(rack.getStatus());
            location.setLocationCode(buildRackStorageCode(warehouse, rack));
            location.setLocationName(firstNotBlank(rack.getRackName(), location.getLocationCode()));
            location.setWarehouseCode(warehouse.getWarehouseCode());
            location.setWarehouseName(warehouse.getWarehouseName());
            location.setRackNo(String.valueOf(rack.getRackNo()));
            location.setGridNo(buildRackGridNo(rack.getRackNo()));
            hcNgInventoryLocationMapper.insert(location);
            return location;
        }
        HcNgInventoryLocationDO location = locations.get(0);
        int occupiedQty = activePieceQty(location);
        if (capacityQty < occupiedQty) {
            throw invalidParamException("货架容量不能小于当前已占用数量 " + occupiedQty);
        }
        refreshRackStoragePresentation(location, warehouse, rack, capacityQty, true);
        return location;
    }

    private HcNgInventoryLocationDO requireRackStorageLocation(HcNgInventoryRackDO rack) {
        List<HcNgInventoryLocationDO> locations = hcNgInventoryLocationMapper.selectListByRackId(rack.getId());
        if (locations.size() != 1) {
            throw invalidParamException("货架容量数据未完成合并，请先执行货架容量合并迁移");
        }
        return requireNgLocationIdentity(locations.get(0));
    }

    /** 同步库存余额和 NG 片台账中的展示快照；定位字段保持为不可变库位唯一键。 */
    private void syncLocationPresentation(HcNgInventoryLocationDO location) {
        String locationName = firstNotBlank(location.getLocationName(), location.getLocationCode());
        hcInvStockMapper.update(null, new LambdaUpdateWrapper<HcInvStockDO>()
                .eq(HcInvStockDO::getSourceType, SOURCE_TYPE_NG)
                .eq(HcInvStockDO::getLocationCode, location.getLocationKey())
                .set(HcInvStockDO::getWarehouseCode, location.getWarehouseCode())
                .set(HcInvStockDO::getWarehouseName, location.getWarehouseName())
                .set(HcInvStockDO::getLocationName, locationName));
        hcNgInventoryPieceMapper.update(null, new LambdaUpdateWrapper<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .eq(HcNgInventoryPieceDO::getCurrentLocationCode, location.getLocationKey())
                .set(HcNgInventoryPieceDO::getCurrentWarehouseCode, location.getWarehouseCode())
                .set(HcNgInventoryPieceDO::getCurrentWarehouseName, location.getWarehouseName())
                .set(HcNgInventoryPieceDO::getCurrentLocationName, locationName));
    }

    private String resolveLocationPurpose(HcNgInventoryLocationDO location) {
        return normalizeStoragePurpose(location.getStoragePurpose());
    }

    private void validateLocationHierarchyEnabled(HcNgInventoryLocationDO location) {
        if (location.getWarehouseId() == null || location.getRackId() == null || location.getLocationNo() == null) {
            throw invalidParamException("固定仓库配置不完整，请先完成初始化和迁移");
        }
        HcNgInventoryWarehouseDO warehouse = requireWarehouse(location.getWarehouseId());
        HcNgInventoryRackDO rack = requireRack(location.getRackId());
        if (!"启用".equals(warehouse.getStatus()) || !"启用".equals(rack.getStatus())) {
            throw invalidParamException("不合格品货架所属仓库或货架已停用");
        }
    }

    private String resolveLocationPadType(HcNgInventoryLocationDO location) {
        return requireWarehouse(location.getWarehouseId()).getPadType();
    }

    private String resolveLocationWarehouseCode(HcNgInventoryLocationDO location) {
        return location.getWarehouseCode();
    }

    private String resolveLocationWarehouseName(HcNgInventoryLocationDO location) {
        return location.getWarehouseName();
    }

    private void validatePiecePadType(HcNgInventoryPieceDO piece, HcNgInventoryLocationDO target) {
        String padType = normalizePadType(piece.getPadType());
        if (!Objects.equals(padType, resolveLocationPadType(target))) {
            throw invalidParamException("不合格品垫型与目标仓库不一致，请选择对应的黑垫或白垫仓库");
        }
    }

    private String resolvePadType(HcPlanOrderDO plan) {
        if (plan == null) {
            return null;
        }
        String categoryCode = StrUtil.trimToEmpty(plan.getCategoryCode()).toUpperCase(Locale.ROOT);
        return PAD_TYPE_BLACK.equals(categoryCode) || PAD_TYPE_WHITE.equals(categoryCode) ? categoryCode : null;
    }

    private String normalizePadType(String padType) {
        String normalized = StrUtil.trimToEmpty(padType).toUpperCase(Locale.ROOT);
        if (!PAD_TYPE_BLACK.equals(normalized) && !PAD_TYPE_WHITE.equals(normalized)) {
            throw invalidParamException("垫型仅支持 BLACK_PAD 或 WHITE_PAD");
        }
        return normalized;
    }

    private String buildWarehouseConflictMessage(String warehouseCode, String padType,
                                                 HcNgInventoryWarehouseDO conflictByCode,
                                                 HcNgInventoryWarehouseDO conflictByPadType) {
        List<String> reasons = new ArrayList<>();
        if (conflictByCode != null) {
            reasons.add("仓库编码“" + warehouseCode + "”已被“" + formatWarehouse(conflictByCode) + "”使用");
        }
        if (conflictByPadType != null) {
            reasons.add("垫型“" + padTypeLabel(padType) + "”已配置仓库“"
                    + formatWarehouse(conflictByPadType) + "”");
        }
        return String.join("；", reasons) + "，请在库位管理中编辑已有仓库，不要重复新建";
    }

    private String formatWarehouse(HcNgInventoryWarehouseDO warehouse) {
        return firstNotBlank(warehouse.getWarehouseName(), warehouse.getWarehouseCode(), "未命名仓库")
                + "（" + firstNotBlank(warehouse.getWarehouseCode(), "-") + "）";
    }

    private String padTypeLabel(String padType) {
        return PAD_TYPE_BLACK.equals(padType) ? "黑垫" : "白垫";
    }

    private String normalizeStoragePurpose(String purpose) {
        String normalized = StrUtil.trimToEmpty(purpose).toUpperCase(Locale.ROOT);
        if (!List.of(PURPOSE_SLITTING_NG, PURPOSE_PRESS_SLOT_NG, PURPOSE_FREEZE, PURPOSE_MIXED).contains(normalized)) {
            throw invalidParamException("存储标记仅支持 SLITTING_NG、PRESS_SLOT_NG、FREEZE 或 MIXED");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = firstNotBlank(StrUtil.trim(status), "启用");
        if (!"启用".equals(normalized) && !"停用".equals(normalized)) {
            throw invalidParamException("状态仅支持“启用”或“停用”");
        }
        return normalized;
    }

    private Integer defaultSortNo(Integer sortNo) {
        return sortNo == null ? 0 : sortNo;
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void validatePositiveNo(Integer value, String label) {
        if (value == null || value <= 0 || value > 999) {
            throw invalidParamException(label + "必须是 1 到 999 的整数");
        }
    }

    private void validateRackCapacity(Integer capacityQty) {
        if (capacityQty == null || capacityQty <= 0 || capacityQty > 99999) {
            throw invalidParamException("货架容量必须是 1 到 99999 的整数");
        }
    }

    private String buildLocationCode(HcNgInventoryWarehouseDO warehouse, HcNgInventoryRackDO rack,
                                     Integer locationNo) {
        return warehouse.getWarehouseCode() + "-" + resolveRackCode(rack) + "-" + locationNo;
    }

    private String buildRackStorageCode(HcNgInventoryWarehouseDO warehouse, HcNgInventoryRackDO rack) {
        return warehouse.getWarehouseCode() + "-" + resolveRackCode(rack);
    }

    private String resolveRackCode(HcNgInventoryRackDO rack) {
        return normalizeRackCode(rack.getRackCode(), rack.getRackNo());
    }

    private String normalizeRackCode(String rackCode, Integer rackNo) {
        String normalized = firstNotBlank(StrUtil.trimToNull(rackCode), defaultRackCode(rackNo))
                .toUpperCase(Locale.ROOT);
        if (!RACK_CODE_PATTERN.matcher(normalized).matches()) {
            throw invalidParamException("货架编码仅支持大写字母、数字和下划线，且必须以字母或数字开头");
        }
        return normalized;
    }

    private String defaultRackCode(Integer rackNo) {
        return "FQ" + (rackNo == null ? "" : rackNo);
    }

    private String buildLocationKey() {
        return "NGLOC-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String buildRackStorageKey() {
        return "NGRACK-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private int buildGridNo(Integer rackNo, Integer locationNo) {
        return rackNo * 1_000 + locationNo;
    }

    private int buildRackGridNo(Integer rackNo) {
        return (rackNo == null ? 0 : rackNo) * 1_000;
    }

    private HcNgInventoryWarehouseDO requireWarehouse(Long id) {
        HcNgInventoryWarehouseDO warehouse = id == null ? null : hcNgInventoryWarehouseMapper.selectById(id);
        if (warehouse == null) {
            throw invalidParamException("不合格品仓库不存在或已删除");
        }
        return warehouse;
    }

    private HcNgInventoryRackDO requireRack(Long id) {
        HcNgInventoryRackDO rack = id == null ? null : hcNgInventoryRackMapper.selectById(id);
        if (rack == null) {
            throw invalidParamException("不合格品货架不存在或已删除");
        }
        return rack;
    }

    private HcNgInventoryLocationDO requireLocation(Long id) {
        HcNgInventoryLocationDO location = id == null ? null : hcNgInventoryLocationMapper.selectById(id);
        if (location == null) {
            throw invalidParamException("不合格品库位不存在或已删除");
        }
        return location;
    }

    private boolean hasActiveOrFrozenReference(HcNgInventoryLocationDO location) {
        return !hcNgInventoryPieceMapper.selectActiveListByLocationKeys(List.of(location.getLocationKey())).isEmpty()
                || hcNgInventoryPieceMapper.existsActiveByOriginalLocationKey(location.getLocationKey());
    }

    private int activePieceQty(HcNgInventoryLocationDO location) {
        return hcNgInventoryPieceMapper.selectActiveListByLocationKeys(List.of(location.getLocationKey())).stream()
                .mapToInt(this::pieceQtyAsInt).sum();
    }

    private void ensureLocationsCanDelete(List<HcNgInventoryLocationDO> locations) {
        for (HcNgInventoryLocationDO location : locations) {
            if (hasActiveOrFrozenReference(location)) {
                throw invalidParamException("库位“" + location.getLocationName() + "”仍有库存或冻结退回引用，不能删除");
            }
        }
    }

    private Map<String, List<HcNgInventoryPieceDO>> buildPieceMap(List<HcNgInventoryLocationDO> locations) {
        Map<String, List<HcNgInventoryPieceDO>> result = new HashMap<>();
        List<String> locationKeys = locations.stream().map(HcNgInventoryLocationDO::getLocationKey).toList();
        for (HcNgInventoryPieceDO piece : hcNgInventoryPieceMapper.selectActiveListByLocationKeys(locationKeys)) {
            result.computeIfAbsent(piece.getCurrentLocationCode(), key -> new ArrayList<>()).add(piece);
        }
        return result;
    }

    private <T, K> Map<K, List<T>> groupBy(List<T> values, Function<T, K> keyFunction) {
        Map<K, List<T>> result = new LinkedHashMap<>();
        for (T value : values) {
            result.computeIfAbsent(keyFunction.apply(value), key -> new ArrayList<>()).add(value);
        }
        return result;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private int pieceQtyAsInt(HcNgInventoryPieceDO piece) {
        return normalizeQty(piece.getPieceQty()).intValueExact();
    }

    private BigDecimal normalizeQty(BigDecimal value) {
        return value == null ? PIECE_QTY : value;
    }

    private String buildDefectSummary(String first, String second, String remark) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addIfNotBlank(values, first);
        addIfNotBlank(values, second);
        addIfNotBlank(values, remark);
        return truncateText(values.isEmpty() ? "报工确认 NG" : String.join("；", values), MAX_DEFECT_SUMMARY_LENGTH);
    }

    private String buildSlittingDefectSummary(String selfCheck, String visualResultJson, String remark) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addIfNotBlank(values, selfCheck);
        String ngItemNames = extractSlittingNgItemNames(visualResultJson);
        if (StrUtil.isNotBlank(ngItemNames)) {
            values.add("目视NG：" + ngItemNames);
        } else if (isNgResultText(visualResultJson)) {
            values.add("目视检验NG");
        }
        addIfNotBlank(values, remark);
        return truncateText(values.isEmpty() ? "报工确认 NG" : String.join("；", values), MAX_DEFECT_SUMMARY_LENGTH);
    }

    private String buildAttributedDefectSummary(String attributedProcessName, String detectedProcessName,
                                                String selfCheck, String defectCode, String remark) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(detectedProcessName + "发现加工前" + attributedProcessName + "自检异常，归属" + attributedProcessName);
        addIfNotBlank(values, selfCheck);
        addIfNotBlank(values, defectCode);
        addIfNotBlank(values, remark);
        return truncateText(String.join("；", values), MAX_DEFECT_SUMMARY_LENGTH);
    }

    private String extractSlittingNgItemNames(String visualResultJson) {
        if (StrUtil.isBlank(visualResultJson) || !JsonUtils.isJson(visualResultJson)) {
            return null;
        }
        try {
            JsonNode items = JsonUtils.parseTree(visualResultJson);
            if (!items.isArray()) {
                return null;
            }
            LinkedHashSet<String> names = new LinkedHashSet<>();
            for (JsonNode item : items) {
                if (!isNgResultText(item.path("result").asText())) {
                    continue;
                }
                String itemName = StrUtil.trimToNull(item.path("itemName").asText());
                if (itemName != null) {
                    names.add(itemName);
                }
            }
            return names.isEmpty() ? null : String.join("、", names);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isNgResultText(String value) {
        String text = StrUtil.trimToEmpty(value).toUpperCase();
        return "NG".equals(text)
                || "ABNORMAL".equals(text)
                || "FAILED".equals(text)
                || "FAIL".equals(text)
                || "N".equals(text)
                || text.contains("不合格")
                || text.contains("异常");
    }

    private String buildStockBusinessRemark(String defectSummary) {
        return truncateText(StrUtil.blankToDefault(defectSummary, "不合格品"), MAX_STOCK_BUSINESS_REMARK_LENGTH);
    }

    private String truncateText(String value, int maxLength) {
        String text = StrUtil.trimToNull(value);
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private void addIfNotBlank(Collection<String> values, String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized != null) {
            values.add(normalized);
        }
    }

    private String buildTxnNo(String txnType, Long id, LocalDateTime now) {
        return "NG-" + txnType + "-" + now.format(TXN_TIME_FORMATTER) + "-" + id;
    }

    private Operator currentOperator() {
        return new Operator(SecurityFrameworkUtils.getLoginUserId(),
                firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record Operator(Long id, String name) {
    }
}
