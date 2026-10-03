package cn.iocoder.yudao.module.mes.service.hc.sliceadjust.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.AuditQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.AuditRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.CandidateQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.CandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.ColumnAffectedVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.RenameReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.SwapReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.SwapRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.TraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo.HcSliceAdjustVO.TraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.sliceadjust.HcSliceAdjustRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.sliceadjust.HcSliceAdjustMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.sliceadjust.HcSliceAdjustTraceRow;
import cn.iocoder.yudao.module.mes.service.hc.sliceadjust.HcSliceAdjustService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcSliceAdjustServiceImpl implements HcSliceAdjustService {

    private static final String ADJUST_TYPE_FULL_PROCESS_SLICE_SWAP = "FULL_PROCESS_SLICE_SWAP";

    private static final String ADJUST_TYPE_FULL_PROCESS_SLICE_RENAME = "FULL_PROCESS_SLICE_RENAME";

    private static final List<SwapColumn> SWAP_COLUMNS = List.of(
            new SwapColumn("mes_sfc_slitting_slice_record", "slice_serial_no", "分切片号"),
            new SwapColumn("mes_sfc_slitting_slice_record", "source_batch_no", "分切来源批号"),
            new SwapColumn("mes_sfc_slitting_slice_record", "source_production_batch_no", "分切来源生产批号"),
            new SwapColumn("mes_sfc_press_slot_report", "production_batch_no", "压槽报工片号"),
            new SwapColumn("mes_sfc_press_slot_report", "parent_production_batch_no", "压槽上游片号"),
            new SwapColumn("mes_sfc_press_slot_report", "source_batch_no", "压槽来源批号"),
            new SwapColumn("mes_sfc_press_slot_report", "source_production_batch_no", "压槽来源生产批号"),
            new SwapColumn("mes_sfc_press_slot_intermediate_record", "batch_no", "压槽中间品批号"),
            new SwapColumn("mes_sfc_press_slot_intermediate_record", "first_sample_slice_no", "压槽首检片号"),
            new SwapColumn("mes_sfc_press_slot_intermediate_record", "front_slice_no", "压槽前片号"),
            new SwapColumn("mes_sfc_press_slot_intermediate_record", "middle_slice_no", "压槽中片号"),
            new SwapColumn("mes_sfc_press_slot_intermediate_record", "end_slice_no", "压槽后片号"),
            new SwapColumn("mes_sfc_press_slot_intermediate_detail", "slice_batch_no", "压槽中间品明细片号"),
            new SwapColumn("mes_sfc_adhesive_report", "production_batch_no", "粘胶报工片号"),
            new SwapColumn("mes_sfc_adhesive_report", "parent_production_batch_no", "粘胶上游片号"),
            new SwapColumn("mes_sfc_adhesive_report", "source_batch_no", "粘胶来源批号"),
            new SwapColumn("mes_sfc_adhesive_report", "source_production_batch_no", "粘胶来源生产批号"),
            new SwapColumn("mes_sfc_adhesive_intermediate_record", "batch_no", "粘胶中间品批号"),
            new SwapColumn("mes_sfc_adhesive2_report", "production_batch_no", "粘胶2报工片号"),
            new SwapColumn("mes_sfc_adhesive2_report", "parent_production_batch_no", "粘胶2上游片号"),
            new SwapColumn("mes_sfc_adhesive2_report", "source_batch_no", "粘胶2来源批号"),
            new SwapColumn("mes_sfc_adhesive2_report", "source_production_batch_no", "粘胶2来源生产批号"),
            new SwapColumn("mes_sfc_adhesive2_intermediate_record", "batch_no", "粘胶2中间品批号"),
            new SwapColumn("mes_sfc_adhesive2_intermediate_record", "front_slice_no", "粘胶2前片号"),
            new SwapColumn("mes_sfc_adhesive2_intermediate_record", "middle_slice_no", "粘胶2中片号"),
            new SwapColumn("mes_sfc_adhesive2_intermediate_record", "end_slice_no", "粘胶2后片号"),
            new SwapColumn("mes_sfc_adhesive2_intermediate_detail", "slice_batch_no", "粘胶2中间品明细片号"),
            new SwapColumn("mes_sfc_cut_round_report", "production_batch_no", "裁切报工片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_sfc_cut_round_report", "parent_production_batch_no", "裁切上游片号"),
            new SwapColumn("mes_sfc_cut_round_report", "source_batch_no", "裁切来源批号"),
            new SwapColumn("mes_sfc_cut_round_report", "source_production_batch_no", "裁切来源生产批号"),
            new SwapColumn("mes_sfc_cut_round_inspection_detail", "production_batch_no", "裁切检验片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_sfc_cut_round_inspection_detail", "parent_production_batch_no", "裁切检验上游片号"),
            new SwapColumn("mes_qms_fqc_submission_detail", "production_batch_no", "FQC送检片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_qms_fqc_submission_detail", "parent_production_batch_no", "FQC送检上游片号"),
            new SwapColumn("mes_qms_fqc_item", "production_batch_no", "FQC检验项片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_qms_fqc_item", "parent_production_batch_no", "FQC检验项上游片号"),
            new SwapColumn("mes_qms_fqc_sample", "production_batch_no", "FQC样本片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_qms_fqc_sample", "parent_production_batch_no", "FQC样本上游片号"),
            new SwapColumn("mes_qms_fqc_scan_record", "matched_production_batch_no", "FQC扫描匹配片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_qms_fqc_order", "product_batch_no", "FQC单产品批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_qms_fqc_order", "batch_no", "FQC单批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_sfc_inner_pack_unit_item", "slice_batch_no", "内包装片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_sfc_inner_pack_unit_item", "production_batch_no", "内包装生产批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_finished_stock", "slice_batch_no", "成品库存片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_finished_stock", "batch_no", "成品库存批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_stock", "batch_no", "实时库存批号(裁切前)",
                    SliceNoMode.PRE_CUT_BASE, "AND COALESCE(op_seq, 0) < 40"),
            new SwapColumn("mes_inv_stock", "batch_no", "实时库存批号(裁切及以后)",
                    SliceNoMode.CUT_OR_AFTER, "AND COALESCE(op_seq, 0) >= 40"),
            new SwapColumn("mes_inv_stock", "source_batch_no", "实时库存来源批号(裁切前)",
                    SliceNoMode.PRE_CUT_BASE, "AND COALESCE(op_seq, 0) < 40"),
            new SwapColumn("mes_inv_stock", "source_batch_no", "实时库存来源批号(裁切及以后)",
                    SliceNoMode.CUT_OR_AFTER, "AND COALESCE(op_seq, 0) >= 40"),
            new SwapColumn("mes_inv_stock", "source_parent_batch_no", "实时库存来源上游批号"),
            new SwapColumn("mes_inv_fg_shipping_notice_item", "slice_batch_no", "发货通知片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_shipping_notice_item", "actual_slice_batch_no", "发货通知实际片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_shipping_notice_item", "batch_no", "发货通知批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_shipping_notice_item", "package_slice_no", "发货通知包装片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_shipping_pick_item", "slice_batch_no", "发货拣配片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_shipping_pick_item", "actual_slice_batch_no", "发货拣配实际片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_shipping_pick_item", "batch_no", "发货拣配批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_outbound_box_item", "slice_batch_no", "出库装箱片号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_inv_fg_outbound_box_item", "batch_no", "出库装箱批号", SliceNoMode.CUT_OR_AFTER),
            new SwapColumn("mes_sfc_operation_report", "batch_no", "通用报工批号"),
            new SwapColumn("mes_sfc_operation_report", "production_batch_no", "通用报工生产批号(裁切前)",
                    SliceNoMode.PRE_CUT_BASE, "AND COALESCE(operation_seq, 0) < 40"),
            new SwapColumn("mes_sfc_operation_report", "production_batch_no", "通用报工生产批号(裁切及以后)",
                    SliceNoMode.CUT_OR_AFTER, "AND COALESCE(operation_seq, 0) >= 40"),
            new SwapColumn("mes_sfc_operation_report", "parent_production_batch_no", "通用报工上游生产批号"),
            new SwapColumn("mes_sfc_operation_report", "parent_batch_no", "通用报工上游批号"),
            new SwapColumn("mes_lot_instance", "production_batch_no", "批次实例生产批号(裁切前)",
                    SliceNoMode.PRE_CUT_BASE, "AND COALESCE(operation_seq, 0) < 40"),
            new SwapColumn("mes_lot_instance", "production_batch_no", "批次实例生产批号(裁切及以后)",
                    SliceNoMode.CUT_OR_AFTER, "AND COALESCE(operation_seq, 0) >= 40"),
            new SwapColumn("mes_lot_instance", "parent_production_batch_no", "批次实例上游生产批号"),
            new SwapColumn("mes_sfc_wet_report_abnormal_position", "production_batch_no", "湿法异常位置片号")
    );

    @Resource
    private HcSliceAdjustMapper sliceAdjustMapper;

    @Override
    public List<CandidateRespVO> getCandidateList(CandidateQueryReqVO reqVO) {
        String segmentBatchNo = normalizeSegmentBatchNo(reqVO == null ? null : reqVO.getSegmentBatchNo());
        String keyword = StrUtil.trimToNull(reqVO == null ? null : reqVO.getKeyword());
        return aggregateCandidates(sliceAdjustMapper.selectTraceRows(segmentBatchNo, keyword))
                .stream()
                .filter(this::canSelectCandidate)
                .toList();
    }

    @Override
    public List<TraceRespVO> getTraceList(TraceQueryReqVO reqVO) {
        String segmentBatchNo = normalizeSegmentBatchNo(reqVO == null ? null : reqVO.getSegmentBatchNo());
        String leftSliceNo = StrUtil.trimToNull(reqVO == null ? null : reqVO.getLeftSliceNo());
        String rightSliceNo = StrUtil.trimToNull(reqVO == null ? null : reqVO.getRightSliceNo());
        if (leftSliceNo == null || rightSliceNo == null) {
            return List.of();
        }
        return sliceAdjustMapper.selectTraceRowsBySliceNos(segmentBatchNo, leftSliceNo, rightSliceNo)
                .stream()
                .map(row -> toTraceResp(row, leftSliceNo, rightSliceNo))
                .sorted(Comparator
                        .comparing(TraceRespVO::getSide, Comparator.nullsLast(String::compareTo))
                        .thenComparing(item -> item.getStageSort() == null ? 999 : item.getStageSort())
                        .thenComparing(TraceRespVO::getReportTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    @Override
    public List<AuditRecordRespVO> getAuditRecordList(AuditQueryReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO == null ? null : reqVO.getKeyword());
        LambdaQueryWrapperX<HcSliceAdjustRecordDO> query = new LambdaQueryWrapperX<>();
        if (keyword != null) {
            String normalizedSegmentBatchNo = normalizeSegmentBatchNo(keyword);
            query.and(wrapper -> wrapper.like(HcSliceAdjustRecordDO::getAdjustNo, keyword)
                    .or().like(HcSliceAdjustRecordDO::getSegmentBatchNo, normalizedSegmentBatchNo)
                    .or().like(HcSliceAdjustRecordDO::getLeftSliceNo, keyword)
                    .or().like(HcSliceAdjustRecordDO::getRightSliceNo, keyword)
                    .or().like(HcSliceAdjustRecordDO::getOperatorName, keyword));
        }
        query.orderByDesc(HcSliceAdjustRecordDO::getAdjustTime)
                .orderByDesc(HcSliceAdjustRecordDO::getId)
                .last("LIMIT 100");
        return sliceAdjustMapper.selectList(query).stream()
                .map(this::toAuditRecordResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwapRespVO swapSliceNo(SwapReqVO reqVO) {
        String leftSliceNo = StrUtil.trimToNull(reqVO.getLeftSliceNo());
        String rightSliceNo = StrUtil.trimToNull(reqVO.getRightSliceNo());
        if (StrUtil.equalsIgnoreCase(leftSliceNo, rightSliceNo)) {
            throw invalidParamException("两个片号不能相同");
        }
        String reason = StrUtil.trimToNull(reqVO.getReason());
        if (StrUtil.isBlank(reason)) {
            throw invalidParamException("调账原因不能为空");
        }

        List<CandidateRespVO> allCandidates = aggregateCandidates(
                sliceAdjustMapper.selectTraceRowsBySliceNos(null, leftSliceNo, rightSliceNo));
        CandidatePair pair = resolveCandidatePair(allCandidates, leftSliceNo, rightSliceNo,
                normalizeSegmentBatchNo(reqVO.getSegmentBatchNo()));
        String segmentBatchNo = pair.segmentBatchNo();

        List<HcSliceAdjustTraceRow> beforeRows = sliceAdjustMapper
                .selectTraceRowsBySliceNos(segmentBatchNo, leftSliceNo, rightSliceNo);
        Map<String, Object> beforeSnapshot = buildSnapshot(segmentBatchNo, leftSliceNo, rightSliceNo, beforeRows);

        List<ColumnAffectedVO> affectedColumns = new ArrayList<>();
        int affectedRows = 0;
        int index = 0;
        SliceNoParts leftParts = parseSliceNo(leftSliceNo);
        SliceNoParts rightParts = parseSliceNo(rightSliceNo);
        for (SwapColumn column : SWAP_COLUMNS) {
            int businessAffectedRows = swapColumnValues(column, leftParts, rightParts, index++);
            if (businessAffectedRows > 0) {
                affectedRows += businessAffectedRows;
                affectedColumns.add(buildAffectedColumn(column, businessAffectedRows));
            }
        }
        if (affectedRows == 0) {
            throw invalidParamException("未找到需要调换的片号关联记录，请确认片号是否正确");
        }

        List<HcSliceAdjustTraceRow> afterRows = sliceAdjustMapper
                .selectTraceRowsBySliceNos(segmentBatchNo, leftSliceNo, rightSliceNo);
        Map<String, Object> afterSnapshot = buildSnapshot(segmentBatchNo, leftSliceNo, rightSliceNo, afterRows);

        LocalDateTime now = LocalDateTime.now();
        String adjustNo = nextNo("SLA");
        String operatorName = firstNotBlank(StrUtil.trimToNull(reqVO.getOperatorName()), currentUserName());
        sliceAdjustMapper.insert(HcSliceAdjustRecordDO.builder()
                .tenantId(currentTenantId())
                .adjustNo(adjustNo)
                .adjustType(ADJUST_TYPE_FULL_PROCESS_SLICE_SWAP)
                .segmentBatchNo(segmentBatchNo)
                .leftSliceNo(leftSliceNo)
                .rightSliceNo(rightSliceNo)
                .leftLastProcessCode(pair.left().getLastProcessCode())
                .leftLastProcessName(pair.left().getLastProcessName())
                .rightLastProcessCode(pair.right().getLastProcessCode())
                .rightLastProcessName(pair.right().getLastProcessName())
                .operatorName(operatorName)
                .adjustTime(now)
                .adjustReason(reason)
                .affectedRows(affectedRows)
                .affectedColumnsJson(JsonUtils.toJsonString(affectedColumns))
                .beforeSnapshotJson(JsonUtils.toJsonString(beforeSnapshot))
                .afterSnapshotJson(JsonUtils.toJsonString(afterSnapshot))
                .build());

        SwapRespVO respVO = new SwapRespVO();
        respVO.setAdjustNo(adjustNo);
        respVO.setSegmentBatchNo(segmentBatchNo);
        respVO.setLeftSliceNo(leftSliceNo);
        respVO.setRightSliceNo(rightSliceNo);
        respVO.setAffectedRows(affectedRows);
        respVO.setAffectedColumns(affectedColumns);
        respVO.setMessage("片号全流程关联数据已调换");
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwapRespVO renameSliceNo(RenameReqVO reqVO) {
        String sourceSliceNo = StrUtil.trimToNull(reqVO.getSourceSliceNo());
        String targetSliceNo = StrUtil.trimToNull(reqVO.getTargetSliceNo());
        if (StrUtil.equalsIgnoreCase(sourceSliceNo, targetSliceNo)) {
            throw invalidParamException("原片号和新片号不能相同");
        }
        String reason = StrUtil.trimToNull(reqVO.getReason());
        if (StrUtil.isBlank(reason)) {
            throw invalidParamException("调账原因不能为空");
        }

        SliceNoParts sourceParts = parseSliceNo(sourceSliceNo);
        SliceNoParts targetParts = parseSliceNo(targetSliceNo);
        List<String> targetCheckNos = renameTargetCheckNos(sourceParts, targetParts);
        List<CandidateRespVO> allCandidates = loadRenameCandidates(sourceSliceNo, targetCheckNos);
        CandidateRespVO sourceCandidate = resolveSingleCandidate(allCandidates, sourceSliceNo,
                normalizeSegmentBatchNo(reqVO.getSegmentBatchNo()));
        String segmentBatchNo = normalizeSegmentBatchNo(sourceCandidate.getSegmentBatchNo());
        ensureRenameTargetNotExists(allCandidates, sourceSliceNo, targetCheckNos);

        String afterTraceTargetSliceNo = targetCheckNos.get(0);
        List<HcSliceAdjustTraceRow> beforeRows = sliceAdjustMapper
                .selectTraceRowsBySliceNos(segmentBatchNo, sourceSliceNo, afterTraceTargetSliceNo);
        Map<String, Object> beforeSnapshot = buildSnapshot(segmentBatchNo, sourceSliceNo, targetSliceNo, beforeRows);

        List<ColumnAffectedVO> affectedColumns = new ArrayList<>();
        int affectedRows = 0;
        for (SwapColumn column : SWAP_COLUMNS) {
            int businessAffectedRows = renameColumnValues(column, sourceParts, targetParts);
            if (businessAffectedRows > 0) {
                affectedRows += businessAffectedRows;
                affectedColumns.add(buildAffectedColumn(column, businessAffectedRows));
            }
        }
        if (affectedRows == 0) {
            throw invalidParamException("未找到需要修改的片号关联记录，请确认片号是否正确");
        }

        List<HcSliceAdjustTraceRow> afterRows = sliceAdjustMapper
                .selectTraceRowsBySliceNos(segmentBatchNo, sourceSliceNo, afterTraceTargetSliceNo);
        Map<String, Object> afterSnapshot = buildSnapshot(segmentBatchNo, sourceSliceNo, targetSliceNo, afterRows);

        LocalDateTime now = LocalDateTime.now();
        String adjustNo = nextNo("SLR");
        String operatorName = firstNotBlank(StrUtil.trimToNull(reqVO.getOperatorName()), currentUserName());
        sliceAdjustMapper.insert(HcSliceAdjustRecordDO.builder()
                .tenantId(currentTenantId())
                .adjustNo(adjustNo)
                .adjustType(ADJUST_TYPE_FULL_PROCESS_SLICE_RENAME)
                .segmentBatchNo(segmentBatchNo)
                .leftSliceNo(sourceSliceNo)
                .rightSliceNo(targetSliceNo)
                .leftLastProcessCode(sourceCandidate.getLastProcessCode())
                .leftLastProcessName(sourceCandidate.getLastProcessName())
                .rightLastProcessCode(null)
                .rightLastProcessName(null)
                .operatorName(operatorName)
                .adjustTime(now)
                .adjustReason(reason)
                .affectedRows(affectedRows)
                .affectedColumnsJson(JsonUtils.toJsonString(affectedColumns))
                .beforeSnapshotJson(JsonUtils.toJsonString(beforeSnapshot))
                .afterSnapshotJson(JsonUtils.toJsonString(afterSnapshot))
                .build());

        SwapRespVO respVO = new SwapRespVO();
        respVO.setAdjustNo(adjustNo);
        respVO.setSegmentBatchNo(segmentBatchNo);
        respVO.setLeftSliceNo(sourceSliceNo);
        respVO.setRightSliceNo(targetSliceNo);
        respVO.setAffectedRows(affectedRows);
        respVO.setAffectedColumns(affectedColumns);
        respVO.setMessage("片号全流程关联数据已修改");
        return respVO;
    }

    private List<CandidateRespVO> aggregateCandidates(List<HcSliceAdjustTraceRow> rows) {
        Map<String, CandidateAccumulator> map = new LinkedHashMap<>();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        for (HcSliceAdjustTraceRow row : rows) {
            String sliceNo = StrUtil.trimToNull(row.getSliceNo());
            if (sliceNo == null) {
                continue;
            }
            String segmentBatchNo = StrUtil.trimToEmpty(normalizeSegmentBatchNo(row.getSegmentBatchNo()));
            String key = sliceNo + "\n" + segmentBatchNo;
            CandidateAccumulator accumulator = map.computeIfAbsent(key, item -> new CandidateAccumulator(sliceNo, segmentBatchNo));
            accumulator.relatedCount++;
            if (shouldUseAsLast(row, accumulator.lastRow)) {
                accumulator.lastRow = row;
            }
        }
        return map.values().stream()
                .map(this::toCandidateResp)
                .sorted(Comparator
                        .comparing(CandidateRespVO::getSegmentBatchNo, Comparator.nullsLast(String::compareTo)).reversed()
                        .thenComparing(CandidateRespVO::getSliceNo, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private CandidateRespVO toCandidateResp(CandidateAccumulator accumulator) {
        HcSliceAdjustTraceRow row = accumulator.lastRow;
        CandidateRespVO respVO = new CandidateRespVO();
        respVO.setSliceNo(accumulator.sliceNo);
        respVO.setSegmentBatchNo(StrUtil.isBlank(accumulator.segmentBatchNo) ? null : accumulator.segmentBatchNo);
        respVO.setLastProcessCode(firstNotBlank(row.getProcessCode(), row.getOperationCode()));
        respVO.setLastProcessName(firstNotBlank(row.getProcessName(), row.getOperationName()));
        respVO.setPlanNo(row.getPlanNo());
        respVO.setMaterialCode(row.getMaterialCode());
        respVO.setMaterialName(row.getMaterialName());
        respVO.setModelCode(row.getModelCode());
        respVO.setStatusText(row.getStatusText());
        respVO.setResultText(row.getResultText());
        respVO.setRelatedCount(accumulator.relatedCount);
        respVO.setLastReportTime(row.getReportTime());
        return respVO;
    }

    private boolean canSelectCandidate(CandidateRespVO candidate) {
        return candidate != null && !isExcludedCandidateProcess(candidate.getLastProcessCode(), candidate.getLastProcessName());
    }

    private boolean isExcludedCandidateProcess(String processCode, String processName) {
        String code = StrUtil.trimToEmpty(processCode).toUpperCase();
        String name = StrUtil.trimToEmpty(processName);
        String upperName = name.toUpperCase();
        if (code.contains("FQC") || code.contains("INSPECTION")
                || upperName.contains("FQC") || upperName.contains("INSPECTION")) {
            return true;
        }
        if (name.contains("检验") || name.contains("送检")) {
            return true;
        }
        if (code.contains("WET") || name.contains("湿法")) {
            return true;
        }
        return code.contains("GRIND")
                || name.contains("磨皮")
                || name.contains("粗磨")
                || name.contains("精磨")
                || name.contains("研磨")
                || name.contains("磨削")
                || name.contains("磨边");
    }

    private TraceRespVO toTraceResp(HcSliceAdjustTraceRow row, String leftSliceNo, String rightSliceNo) {
        TraceRespVO respVO = new TraceRespVO();
        String sliceNo = StrUtil.trimToNull(row.getSliceNo());
        respVO.setSide(equalsText(sliceNo, leftSliceNo) ? "left" : equalsText(sliceNo, rightSliceNo) ? "right" : null);
        respVO.setSliceNo(sliceNo);
        respVO.setSegmentBatchNo(normalizeSegmentBatchNo(row.getSegmentBatchNo()));
        respVO.setProcessCode(firstNotBlank(row.getProcessCode(), row.getOperationCode()));
        respVO.setProcessName(firstNotBlank(row.getProcessName(), row.getOperationName()));
        respVO.setStageSort(row.getStageSort());
        respVO.setSourceTable(row.getSourceTable());
        respVO.setSourceId(row.getSourceId());
        respVO.setStatusText(row.getStatusText());
        respVO.setResultText(row.getResultText());
        respVO.setReportTime(row.getReportTime());
        return respVO;
    }

    private AuditRecordRespVO toAuditRecordResp(HcSliceAdjustRecordDO record) {
        AuditRecordRespVO respVO = new AuditRecordRespVO();
        respVO.setId(record.getId());
        respVO.setAdjustNo(record.getAdjustNo());
        respVO.setAdjustType(record.getAdjustType());
        respVO.setSegmentBatchNo(normalizeSegmentBatchNo(record.getSegmentBatchNo()));
        respVO.setLeftSliceNo(record.getLeftSliceNo());
        respVO.setRightSliceNo(record.getRightSliceNo());
        respVO.setLeftLastProcessName(record.getLeftLastProcessName());
        respVO.setRightLastProcessName(record.getRightLastProcessName());
        respVO.setOperatorName(record.getOperatorName());
        respVO.setAdjustTime(record.getAdjustTime());
        respVO.setAdjustReason(record.getAdjustReason());
        respVO.setAffectedRows(record.getAffectedRows());
        return respVO;
    }

    private boolean shouldUseAsLast(HcSliceAdjustTraceRow row, HcSliceAdjustTraceRow current) {
        if (current == null) {
            return true;
        }
        int rowSort = row.getStageSort() == null ? 0 : row.getStageSort();
        int currentSort = current.getStageSort() == null ? 0 : current.getStageSort();
        if (rowSort != currentSort) {
            return rowSort > currentSort;
        }
        LocalDateTime rowTime = row.getReportTime();
        LocalDateTime currentTime = current.getReportTime();
        return rowTime != null && (currentTime == null || rowTime.isAfter(currentTime));
    }

    private CandidatePair resolveCandidatePair(List<CandidateRespVO> candidates, String leftSliceNo,
                                                String rightSliceNo, String requestedSegmentBatchNo) {
        if (candidates == null || candidates.isEmpty()) {
            throw invalidParamException("未找到两个片号的报工记录");
        }
        if (StrUtil.isNotBlank(requestedSegmentBatchNo)) {
            String normalizedRequestedSegmentBatchNo = normalizeSegmentBatchNo(requestedSegmentBatchNo);
            CandidateRespVO left = findCandidate(candidates, leftSliceNo, normalizedRequestedSegmentBatchNo);
            CandidateRespVO right = findCandidate(candidates, rightSliceNo, normalizedRequestedSegmentBatchNo);
            if (left == null || right == null) {
                throw invalidParamException("两个片号不在同一分段下，请重新选择");
            }
            return new CandidatePair(normalizedRequestedSegmentBatchNo, left, right);
        }

        Set<String> leftSegments = segmentsOf(candidates, leftSliceNo);
        Set<String> rightSegments = segmentsOf(candidates, rightSliceNo);
        if (leftSegments.isEmpty()) {
            throw invalidParamException("未找到左侧片号报工记录：" + leftSliceNo);
        }
        if (rightSegments.isEmpty()) {
            throw invalidParamException("未找到右侧片号报工记录：" + rightSliceNo);
        }
        leftSegments.retainAll(rightSegments);
        if (leftSegments.isEmpty()) {
            throw invalidParamException("两个片号不在同一分段下，不能调账");
        }
        if (leftSegments.size() > 1) {
            throw invalidParamException("两个片号存在多个共同分段，请先在页面指定分段后再调账");
        }
        String segmentBatchNo = leftSegments.iterator().next();
        CandidateRespVO left = findCandidate(candidates, leftSliceNo, segmentBatchNo);
        CandidateRespVO right = findCandidate(candidates, rightSliceNo, segmentBatchNo);
        return new CandidatePair(segmentBatchNo, left, right);
    }

    private CandidateRespVO resolveSingleCandidate(List<CandidateRespVO> candidates, String sliceNo,
                                                   String requestedSegmentBatchNo) {
        if (candidates == null || candidates.isEmpty()) {
            throw invalidParamException("未找到片号报工记录：" + sliceNo);
        }
        if (StrUtil.isNotBlank(requestedSegmentBatchNo)) {
            String normalizedRequestedSegmentBatchNo = normalizeSegmentBatchNo(requestedSegmentBatchNo);
            CandidateRespVO candidate = findCandidate(candidates, sliceNo, normalizedRequestedSegmentBatchNo);
            if (candidate == null) {
                throw invalidParamException("未找到片号在指定分段下的报工记录：" + sliceNo);
            }
            return candidate;
        }

        Set<String> segments = segmentsOf(candidates, sliceNo);
        if (segments.isEmpty()) {
            throw invalidParamException("未找到片号报工记录：" + sliceNo);
        }
        if (segments.size() > 1) {
            throw invalidParamException("片号存在多个分段，请先在页面指定分段后再修改");
        }
        return findCandidate(candidates, sliceNo, segments.iterator().next());
    }

    private List<CandidateRespVO> loadRenameCandidates(String sourceSliceNo, List<String> targetCheckNos) {
        List<HcSliceAdjustTraceRow> rows = new ArrayList<>();
        for (String targetCheckNo : targetCheckNos) {
            rows.addAll(sliceAdjustMapper.selectTraceRowsBySliceNos(null, sourceSliceNo, targetCheckNo));
        }
        return aggregateCandidates(rows);
    }

    private void ensureRenameTargetNotExists(List<CandidateRespVO> candidates, String sourceSliceNo,
                                             List<String> targetCheckNos) {
        for (String targetCheckNo : targetCheckNos) {
            if (equalsText(targetCheckNo, sourceSliceNo)) {
                continue;
            }
            if (!segmentsOf(candidates, targetCheckNo).isEmpty()) {
                throw invalidParamException("新片号已存在，不能直接修改为已有片号：" + targetCheckNo);
            }
        }
    }

    private Set<String> segmentsOf(List<CandidateRespVO> candidates, String sliceNo) {
        Set<String> segments = new LinkedHashSet<>();
        for (CandidateRespVO candidate : candidates) {
            if (equalsText(candidate.getSliceNo(), sliceNo)) {
                segments.add(StrUtil.trimToEmpty(normalizeSegmentBatchNo(candidate.getSegmentBatchNo())));
            }
        }
        return segments;
    }

    private CandidateRespVO findCandidate(List<CandidateRespVO> candidates, String sliceNo, String segmentBatchNo) {
        String normalizedSliceNo = StrUtil.trimToEmpty(sliceNo);
        String normalizedSegmentBatchNo = StrUtil.trimToEmpty(normalizeSegmentBatchNo(segmentBatchNo));
        return candidates.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(StrUtil.trimToEmpty(item.getSliceNo()), normalizedSliceNo)
                        && Objects.equals(StrUtil.trimToEmpty(normalizeSegmentBatchNo(item.getSegmentBatchNo())),
                        normalizedSegmentBatchNo))
                .findFirst()
                .orElse(null);
    }

    private String normalizeSegmentBatchNo(String segmentBatchNo) {
        String text = StrUtil.trimToNull(segmentBatchNo);
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?i)-J\\d+$", "");
    }

    private Map<String, Object> buildSnapshot(String segmentBatchNo, String leftSliceNo, String rightSliceNo,
                                              List<HcSliceAdjustTraceRow> rows) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("segmentBatchNo", segmentBatchNo);
        snapshot.put("leftSliceNo", leftSliceNo);
        snapshot.put("rightSliceNo", rightSliceNo);
        snapshot.put("leftRows", rowsOf(rows, leftSliceNo));
        snapshot.put("rightRows", rowsOf(rows, rightSliceNo));
        return snapshot;
    }

    private List<Map<String, Object>> rowsOf(List<HcSliceAdjustTraceRow> rows, String sliceNo) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (HcSliceAdjustTraceRow row : rows) {
            if (!equalsText(row.getSliceNo(), sliceNo)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sourceTable", row.getSourceTable());
            item.put("sourceId", row.getSourceId());
            item.put("processCode", row.getProcessCode());
            item.put("processName", row.getProcessName());
            item.put("planNo", row.getPlanNo());
            item.put("materialCode", row.getMaterialCode());
            item.put("materialName", row.getMaterialName());
            item.put("modelCode", row.getModelCode());
            item.put("statusText", row.getStatusText());
            item.put("resultText", row.getResultText());
            item.put("reportTime", row.getReportTime());
            list.add(item);
        }
        return list;
    }

    private ColumnAffectedVO buildAffectedColumn(SwapColumn column, int affectedRows) {
        ColumnAffectedVO vo = new ColumnAffectedVO();
        vo.setTableName(column.tableName());
        vo.setColumnName(column.columnName());
        vo.setColumnComment(column.columnComment());
        vo.setAffectedRows(affectedRows);
        return vo;
    }

    private int swapColumnValues(SwapColumn column, SliceNoParts leftParts, SliceNoParts rightParts, int columnIndex) {
        Map<String, String> mappings = buildColumnSwapMap(column, leftParts, rightParts);
        List<TempMove> tempMoves = new ArrayList<>();
        int tempIndex = 0;
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String tempValue = "__SA_TMP_" + System.nanoTime() + "_" + columnIndex + "_" + tempIndex++;
            int rows = sliceAdjustMapper.updateColumnValue(column.tableName(), column.columnName(), column.extraCondition(),
                    entry.getKey(), tempValue);
            if (rows > 0) {
                tempMoves.add(new TempMove(tempValue, entry.getValue(), rows));
            }
        }
        int affectedRows = 0;
        for (TempMove move : tempMoves) {
            int restoreRows = sliceAdjustMapper.updateColumnValue(column.tableName(), column.columnName(), column.extraCondition(),
                    move.tempValue(), move.targetValue());
            if (restoreRows != move.rows()) {
                throw invalidParamException("片号调账临时交换异常，请回滚事务后人工核查："
                        + column.tableName() + "." + column.columnName());
            }
            affectedRows += restoreRows;
        }
        return affectedRows;
    }

    private int renameColumnValues(SwapColumn column, SliceNoParts sourceParts, SliceNoParts targetParts) {
        Map<String, String> mappings = buildColumnRenameMap(column, sourceParts, targetParts);
        int affectedRows = 0;
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            affectedRows += sliceAdjustMapper.updateColumnValue(column.tableName(), column.columnName(),
                    column.extraCondition(), entry.getKey(), entry.getValue());
        }
        return affectedRows;
    }

    private Map<String, String> buildColumnSwapMap(SwapColumn column, SliceNoParts leftParts,
                                                    SliceNoParts rightParts) {
        Map<String, String> mappings = new LinkedHashMap<>();
        String leftTarget = targetSliceNo(column.sliceNoMode(), rightParts, leftParts);
        String rightTarget = targetSliceNo(column.sliceNoMode(), leftParts, rightParts);
        addSliceMapping(mappings, leftParts.fullNo(), leftTarget);
        addSliceMapping(mappings, leftParts.baseNo(), leftTarget);
        addSliceMapping(mappings, rightParts.fullNo(), rightTarget);
        addSliceMapping(mappings, rightParts.baseNo(), rightTarget);
        return mappings;
    }

    private Map<String, String> buildColumnRenameMap(SwapColumn column, SliceNoParts sourceParts,
                                                     SliceNoParts targetParts) {
        Map<String, String> mappings = new LinkedHashMap<>();
        String target = targetRenameSliceNo(column.sliceNoMode(), sourceParts, targetParts);
        addSliceMapping(mappings, sourceParts.fullNo(), target);
        addSliceMapping(mappings, sourceParts.baseNo(), target);
        return mappings;
    }

    private void addSliceMapping(Map<String, String> mappings, String oldValue, String newValue) {
        String oldText = StrUtil.trimToNull(oldValue);
        String newText = StrUtil.trimToNull(newValue);
        if (oldText == null || newText == null || equalsText(oldText, newText)) {
            return;
        }
        String existing = mappings.get(oldText);
        if (existing != null && !equalsText(existing, newText)) {
            throw invalidParamException("片号调账规则冲突，请不要选择同一基础片号的不同后缀片号互换：" + oldText);
        }
        mappings.put(oldText, newText);
    }

    private String targetSliceNo(SliceNoMode mode, SliceNoParts targetSide, SliceNoParts sourceSide) {
        if (mode == SliceNoMode.PRE_CUT_BASE) {
            return targetSide.baseNo();
        }
        return sourceSide.suffix() == null ? targetSide.baseNo() : targetSide.baseNo() + sourceSide.suffix();
    }

    private String targetRenameSliceNo(SliceNoMode mode, SliceNoParts sourceParts, SliceNoParts targetParts) {
        if (mode == SliceNoMode.PRE_CUT_BASE) {
            return targetParts.baseNo();
        }
        if (targetParts.suffix() != null) {
            return targetParts.fullNo();
        }
        return sourceParts.suffix() == null ? targetParts.baseNo() : targetParts.baseNo() + sourceParts.suffix();
    }

    private List<String> renameTargetCheckNos(SliceNoParts sourceParts, SliceNoParts targetParts) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String cutOrAfterTarget = targetRenameSliceNo(SliceNoMode.CUT_OR_AFTER, sourceParts, targetParts);
        values.add(cutOrAfterTarget);
        values.add(targetParts.fullNo());
        values.add(targetParts.baseNo());
        return values.stream()
                .map(StrUtil::trimToNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private SliceNoParts parseSliceNo(String sliceNo) {
        String fullNo = StrUtil.trimToEmpty(sliceNo);
        if (fullNo.length() > 1) {
            char lastChar = fullNo.charAt(fullNo.length() - 1);
            char suffix = Character.toUpperCase(lastChar);
            if ((suffix == 'A' || suffix == 'B') && Character.isDigit(fullNo.charAt(fullNo.length() - 2))) {
                return new SliceNoParts(fullNo, fullNo.substring(0, fullNo.length() - 1), String.valueOf(suffix));
            }
        }
        return new SliceNoParts(fullNo, fullNo, null);
    }

    private String nextNo(String prefix) {
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String currentUserName() {
        return firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String text = StrUtil.trimToNull(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private boolean equalsText(String left, String right) {
        return StrUtil.equalsIgnoreCase(StrUtil.trimToEmpty(left), StrUtil.trimToEmpty(right));
    }

    private enum SliceNoMode {
        PRE_CUT_BASE,
        CUT_OR_AFTER
    }

    private record SwapColumn(String tableName, String columnName, String columnComment, SliceNoMode sliceNoMode,
                              String extraCondition) {

        private SwapColumn {
            extraCondition = StrUtil.trimToEmpty(extraCondition);
        }

        private SwapColumn(String tableName, String columnName, String columnComment) {
            this(tableName, columnName, columnComment, SliceNoMode.PRE_CUT_BASE, "");
        }

        private SwapColumn(String tableName, String columnName, String columnComment, SliceNoMode sliceNoMode) {
            this(tableName, columnName, columnComment, sliceNoMode, "");
        }
    }

    private record SliceNoParts(String fullNo, String baseNo, String suffix) {
    }

    private record TempMove(String tempValue, String targetValue, int rows) {
    }

    private record CandidatePair(String segmentBatchNo, CandidateRespVO left, CandidateRespVO right) {
    }

    private static final class CandidateAccumulator {

        private final String sliceNo;
        private final String segmentBatchNo;
        private int relatedCount;
        private HcSliceAdjustTraceRow lastRow;

        private CandidateAccumulator(String sliceNo, String segmentBatchNo) {
            this.sliceNo = sliceNo;
            this.segmentBatchNo = segmentBatchNo;
        }
    }
}
