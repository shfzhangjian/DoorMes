package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskItemSelectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRejectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRecheckHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetCellValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDefectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetCellValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsProductEventRecheckDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsProductEventRecheckGroupDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetCellValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleDefectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSheetCellValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSubmissionDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsProductEventRecheckDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsProductEventRecheckGroupMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
public class QmsProductAbnormalEventServiceImpl implements QmsProductAbnormalEventService {
    @jakarta.annotation.Resource
    @org.springframework.context.annotation.Lazy
    private cn.iocoder.yudao.module.mes.service.hc.processreport.HcFinishedPackagingService coaFreezePackagingService;


    private static final String SOURCE_TYPE_FAI = "FAI";
    private static final String SOURCE_TYPE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String SOURCE_TYPE_CUT_ROUND_FQC = "CUT_ROUND_FQC";
    private static final String SOURCE_TYPE_FG_SHIPPING_FQC = "FG_SHIPPING_FQC";
    private static final String SOURCE_TYPE_IQC = "IQC";
    private static final String SOURCE_TYPE_OQC = "OQC";
    private static final String PROCESS_CATEGORY_FINAL_PRODUCTS = "FINAL_PRODUCTS";

    private static final String JUDGMENT_NG = "NG";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String NCR_STATUS_ALL = "ALL";
    private static final String NCR_STATUS_GENERATED = "GENERATED";
    private static final String NCR_STATUS_PENDING = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final String RECHECK_STATUS_ALL = "ALL";
    private static final String RECHECK_STATUS_NONE = "NONE";
    private static final String RECHECK_STATUS_RECHECKING = "RECHECKING";
    private static final String RECHECK_STATUS_OK = "RECHECK_OK";
    private static final String RECHECK_STATUS_NG = "RECHECK_NG";

    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsFaiItemMapper qmsFaiItemMapper;
    @Resource
    private QmsFaiSampleMapper qmsFaiSampleMapper;
    @Resource
    private QmsFaiSheetCellValueMapper qmsFaiSheetCellValueMapper;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Resource
    private QmsFqcItemMapper qmsFqcItemMapper;
    @Resource
    private QmsFqcSampleMapper qmsFqcSampleMapper;
    @Resource
    private QmsFqcSampleDefectMapper qmsFqcSampleDefectMapper;
    @Resource
    private QmsFqcSheetCellValueMapper qmsFqcSheetCellValueMapper;
    @Resource
    private QmsFqcSubmissionDetailMapper qmsFqcSubmissionDetailMapper;
    @Resource
    private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;
    @Resource
    private QmsIqcOrderMapper qmsIqcOrderMapper;
    @Resource
    private QmsIqcItemMapper qmsIqcItemMapper;
    @Resource
    private QmsIqcSampleMapper qmsIqcSampleMapper;
    @Resource
    private QmsOqcOrderMapper qmsOqcOrderMapper;
    @Resource
    private QmsOqcItemMapper qmsOqcItemMapper;
    @Resource
    private QmsOqcSampleMapper qmsOqcSampleMapper;
    @Resource
    private QmsNcRecordMapper qmsNcRecordMapper;
    @Resource
    private QmsNcRelationMapper qmsNcRelationMapper;
    @Resource
    private QmsProductEventRecheckGroupMapper qmsProductEventRecheckGroupMapper;
    @Resource
    private QmsProductEventRecheckDetailMapper qmsProductEventRecheckDetailMapper;
    @Resource
    private QmsDefectCodeMapper qmsDefectCodeMapper;
    @Resource
    private QmsNoGeneratorService qmsNoGeneratorService;

    @Resource
    private QmsFaiService qmsFaiService;
    @Resource
    private QmsCutRoundFqcService qmsCutRoundFqcService;
    @Resource
    private QmsFgShippingFqcService qmsFgShippingFqcService;
    @Resource
    private QmsIqcService qmsIqcService;
    @Resource
    private QmsOqcService qmsOqcService;

    @Override
    public PageResult<QmsProductAbnormalEventRespVO> getPage(QmsProductAbnormalEventPageReqVO reqVO) {
        List<QmsProductAbnormalEventRespVO> rows = new ArrayList<>();
        if (includesSource(reqVO, SOURCE_TYPE_FAI)) {
            rows.addAll(listFaiRows(reqVO, false));
        }
        if (includesSource(reqVO, SOURCE_TYPE_GLUE_BOARD_FAI)) {
            rows.addAll(listFaiRows(reqVO, true));
        }
        if (includesSource(reqVO, SOURCE_TYPE_CUT_ROUND_FQC)) {
            rows.addAll(listFqcRows(reqVO, SOURCE_TYPE_CUT_ROUND_FQC));
        }
        if (includesSource(reqVO, SOURCE_TYPE_FG_SHIPPING_FQC)) {
            rows.addAll(listFqcRows(reqVO, SOURCE_TYPE_FG_SHIPPING_FQC));
        }
        if (includesSource(reqVO, SOURCE_TYPE_OQC)) {
            rows.addAll(listOqcRows(reqVO));
        }

        rows = mergeRecheckRows(reqVO, rows);
        rows = rows.stream()
                .filter(this::matchesProductAbnormalScope)
                .collect(Collectors.toList());
        fillNcrStatus(rows);
        rows = rows.stream()
                .filter(row -> matchesNcrStatus(row, reqVO))
                .filter(row -> matchesRecheckStatus(row, reqVO))
                .collect(Collectors.toList());
        rows.sort(this::compareByInspectionTimeDesc);
        int total = rows.size();
        List<QmsProductAbnormalEventRespVO> pagedRows = page(rows, reqVO);
        fillAbnormalSummary(pagedRows);
        return new PageResult<>(pagedRows, (long) total);
    }

    @Override
    public QmsProductAbnormalEventDetailRespVO getDetail(String sourceType, Long inspectionId) {
        String normalizedSourceType = normalizeSourceType(sourceType);
        if (inspectionId == null || StrUtil.isBlank(normalizedSourceType)) {
            return null;
        }
        QmsProductAbnormalEventDetailRespVO detail = null;
        if (SOURCE_TYPE_FAI.equals(normalizedSourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(normalizedSourceType)) {
            detail = buildFaiDetail(normalizedSourceType, qmsFaiService.getFaiResp(inspectionId));
        } else if (SOURCE_TYPE_CUT_ROUND_FQC.equals(normalizedSourceType)) {
            detail = buildCutRoundFqcDetail(qmsCutRoundFqcService.getLight(inspectionId));
        } else if (SOURCE_TYPE_FG_SHIPPING_FQC.equals(normalizedSourceType)) {
            detail = buildFgShippingFqcDetail(qmsFgShippingFqcService.get(inspectionId));
        } else if (SOURCE_TYPE_IQC.equals(normalizedSourceType)) {
            detail = buildIqcDetail(qmsIqcService.getIqcResp(inspectionId));
        } else if (SOURCE_TYPE_OQC.equals(normalizedSourceType)) {
            detail = buildOqcDetail(qmsOqcService.getOqcResp(inspectionId));
        }
        if (detail == null) {
            return null;
        }
        applyDetailRecheckState(detail);
        fillDetailNcrStatus(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsProductAbnormalEventRejectRespVO rejectRecheck(QmsProductAbnormalEventRejectReqVO reqVO) {
        String sourceType = normalizeSourceType(reqVO.getSourceType());
        if (!isSupportedSourceType(sourceType)) {
            throw invalidParamException("检验来源类型不支持");
        }
        QmsProductAbnormalEventRespVO currentRow = buildSourceRow(sourceType, reqVO.getInspectionId());
        if (currentRow == null) {
            throw invalidParamException("检验单不存在");
        }
        if (!isAbnormalInspection(sourceType, reqVO.getInspectionId())) {
            throw invalidParamException("只能对异常或不合格检验单发起驳回复检");
        }
        if (hasRejectNextInspection(sourceType, reqVO.getInspectionId())) {
            throw invalidParamException("当前检验单已发起过驳回复检，请打开已生成的复检单继续处理");
        }
        RecheckSelection selection = SOURCE_TYPE_FAI.equals(sourceType)
                ? RecheckSelection.pendingStandardSelection()
                : buildRecheckSelection(reqVO.getSelectedItemIds(), reqVO.getSelectedScopes());

        QmsProductEventRecheckDetailDO currentDetail =
                qmsProductEventRecheckDetailMapper.selectByInspection(sourceType, reqVO.getInspectionId());
        QmsProductEventRecheckGroupDO group;
        Integer nextRoundNo;
        Long rootInspectionId;
        String rootInspectionNo;
        if (currentDetail == null) {
            group = createRecheckGroup(sourceType, currentRow);
            currentDetail = createRootRecheckDetail(group, currentRow);
            nextRoundNo = 1;
            rootInspectionId = currentRow.getInspectionId();
            rootInspectionNo = currentRow.getInspectionNo();
        } else {
            group = qmsProductEventRecheckGroupMapper.selectById(currentDetail.getGroupId());
            if (group == null) {
                throw invalidParamException("驳回复检链路不存在，请刷新后重试");
            }
            if (!Objects.equals(group.getLatestInspectionId(), reqVO.getInspectionId())) {
                throw invalidParamException("只能在最新复检单上继续发起驳回复检");
            }
            nextRoundNo = Objects.requireNonNullElse(group.getLatestRoundNo(), currentDetail.getRoundNo()) + 1;
            rootInspectionId = group.getRootInspectionId();
            rootInspectionNo = group.getRootInspectionNo();
        }

        String rejectReason = reqVO.getRejectReason().trim();
        Long rejectUserId = SecurityFrameworkUtils.getLoginUserId();
        String rejectUserName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        LocalDateTime rejectTime = LocalDateTime.now();

        CopyInspectionResult copyResult = copyInspectionForRecheck(sourceType, reqVO.getInspectionId(), nextRoundNo,
                rootInspectionId, rootInspectionNo, selection);
        markCurrentInspectionRejected(sourceType, reqVO.getInspectionId(), copyResult.newInspectionId,
                copyResult.newInspectionNo, rejectReason, rejectUserId, rejectUserName, rejectTime);
        markNewInspectionRecheck(sourceType, copyResult.newInspectionId, group.getId(), nextRoundNo,
                reqVO.getInspectionId(), currentRow.getInspectionNo(), rootInspectionId, rootInspectionNo);

        QmsProductEventRecheckDetailDO detail = QmsProductEventRecheckDetailDO.builder()
                .groupId(group.getId())
                .sourceType(sourceType)
                .roundNo(nextRoundNo)
                .inspectionId(copyResult.newInspectionId)
                .inspectionNo(copyResult.newInspectionNo)
                .prevInspectionId(reqVO.getInspectionId())
                .prevInspectionNo(currentRow.getInspectionNo())
                .inspectionStatus(STATUS_PENDING)
                .inspectionJudgment(JUDGMENT_PENDING)
                .rejectReason(rejectReason)
                .rejectUserId(rejectUserId)
                .rejectUserName(rejectUserName)
                .rejectTime(rejectTime)
                .tenantId(copyResult.tenantId)
                .build();
        qmsProductEventRecheckDetailMapper.insert(detail);

        QmsProductEventRecheckGroupDO updateGroup = new QmsProductEventRecheckGroupDO();
        updateGroup.setId(group.getId());
        updateGroup.setLatestInspectionId(copyResult.newInspectionId);
        updateGroup.setLatestInspectionNo(copyResult.newInspectionNo);
        updateGroup.setLatestRoundNo(nextRoundNo);
        updateGroup.setLatestStatus(STATUS_PENDING);
        updateGroup.setLatestJudgment(JUDGMENT_PENDING);
        updateGroup.setChainStatus(RECHECK_STATUS_RECHECKING);
        updateGroup.setTotalRecheckCount(nextRoundNo);
        updateGroup.setLatestResultTime(null);
        updateGroup.setLastRejectReason(rejectReason);
        updateGroup.setLastRejectUserId(rejectUserId);
        updateGroup.setLastRejectUserName(rejectUserName);
        updateGroup.setLastRejectTime(rejectTime);
        qmsProductEventRecheckGroupMapper.updateById(updateGroup);
        updateRootRecheckResult(sourceType, rootInspectionId, JUDGMENT_PENDING, null);

        QmsProductAbnormalEventRejectRespVO respVO = new QmsProductAbnormalEventRejectRespVO();
        respVO.setSourceType(sourceType);
        respVO.setGroupId(group.getId());
        respVO.setRootInspectionId(rootInspectionId);
        respVO.setRootInspectionNo(rootInspectionNo);
        respVO.setNewInspectionId(copyResult.newInspectionId);
        respVO.setNewInspectionNo(copyResult.newInspectionNo);
        respVO.setRecheckRoundNo(nextRoundNo);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsProductAbnormalEventTaskRecheckResult createTaskRecheck(
            String rawSourceType, Long inspectionId, Collection<Long> selectedItemIds,
            String rawRejectReason, Long dispatchTaskId) {
        String sourceType = normalizeSourceType(rawSourceType);
        if (!isSupportedSourceType(sourceType)) {
            throw invalidParamException("检验来源类型不支持");
        }
        QmsProductAbnormalEventRespVO currentRow = buildSourceRow(sourceType, inspectionId);
        if (currentRow == null) {
            throw invalidParamException("检验单不存在");
        }
        if (!isAbnormalInspection(sourceType, inspectionId)) {
            throw invalidParamException("只能对异常或不合格检验单发起驳回复检");
        }
        if (hasRejectNextInspection(sourceType, inspectionId)) {
            throw invalidParamException("当前检验单已发起过驳回复检，请打开已生成的复检任务继续处理");
        }
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            throw invalidParamException("至少选择一个复检项目");
        }

        QmsProductEventRecheckDetailDO currentDetail =
                qmsProductEventRecheckDetailMapper.selectByInspection(sourceType, inspectionId);
        QmsProductEventRecheckGroupDO group;
        Integer nextRoundNo;
        Long rootInspectionId;
        String rootInspectionNo;
        Long rootDetailId;
        Integer rootRoundNo;
        if (currentDetail == null) {
            group = createRecheckGroup(sourceType, currentRow, 1);
            currentDetail = createRootRecheckDetail(group, currentRow, 1);
            nextRoundNo = 2;
            rootInspectionId = currentRow.getInspectionId();
            rootInspectionNo = currentRow.getInspectionNo();
            rootDetailId = currentDetail.getId();
            rootRoundNo = currentDetail.getRoundNo();
        } else {
            group = qmsProductEventRecheckGroupMapper.selectById(currentDetail.getGroupId());
            if (group == null) {
                throw invalidParamException("驳回复检链路不存在，请刷新后重试");
            }
            if (!Objects.equals(group.getLatestInspectionId(), inspectionId)) {
                throw invalidParamException("只能在最新复检单上继续发起驳回复检");
            }
            if (group.getDispatchTaskId() != null
                    && !Objects.equals(group.getDispatchTaskId(), dispatchTaskId)) {
                throw invalidParamException("当前复检链已纳入其他质量任务，请从原任务继续办理");
            }
            nextRoundNo = Objects.requireNonNullElse(group.getLatestRoundNo(), currentDetail.getRoundNo()) + 1;
            rootInspectionId = group.getRootInspectionId();
            rootInspectionNo = group.getRootInspectionNo();
            QmsProductEventRecheckDetailDO rootDetail = qmsProductEventRecheckDetailMapper
                    .selectListByGroupId(group.getId()).stream().findFirst().orElse(null);
            rootDetailId = rootDetail == null ? null : rootDetail.getId();
            rootRoundNo = rootDetail == null ? currentDetail.getRoundNo() : rootDetail.getRoundNo();
        }
        int taskRoundNo = Objects.equals(rootRoundNo, 0) ? nextRoundNo + 1 : nextRoundNo;

        String rejectReason = StrUtil.blankToDefault(rawRejectReason, "").trim();
        if (StrUtil.isBlank(rejectReason)) {
            throw invalidParamException("驳回说明不能为空");
        }
        Long rejectUserId = SecurityFrameworkUtils.getLoginUserId();
        String rejectUserName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        LocalDateTime rejectTime = LocalDateTime.now();
        Set<Long> selected = new HashSet<>(selectedItemIds);

        CopyInspectionResult copyResult = copyInspectionForRecheck(sourceType, inspectionId, nextRoundNo,
                rootInspectionId, rootInspectionNo, RecheckSelection.items(selected));
        markCurrentInspectionRejected(sourceType, inspectionId, copyResult.newInspectionId,
                copyResult.newInspectionNo, rejectReason, rejectUserId, rejectUserName, rejectTime);
        markNewInspectionRecheck(sourceType, copyResult.newInspectionId, group.getId(), nextRoundNo,
                inspectionId, currentRow.getInspectionNo(), rootInspectionId, rootInspectionNo);

        QmsProductEventRecheckDetailDO newDetail = QmsProductEventRecheckDetailDO.builder()
                .groupId(group.getId())
                .sourceType(sourceType)
                .roundNo(nextRoundNo)
                .inspectionId(copyResult.newInspectionId)
                .inspectionNo(copyResult.newInspectionNo)
                .prevInspectionId(inspectionId)
                .prevInspectionNo(currentRow.getInspectionNo())
                .inspectionStatus(STATUS_PENDING)
                .inspectionJudgment(JUDGMENT_PENDING)
                .rejectReason(rejectReason)
                .rejectUserId(rejectUserId)
                .rejectUserName(rejectUserName)
                .rejectTime(rejectTime)
                .tenantId(copyResult.tenantId)
                .build();
        qmsProductEventRecheckDetailMapper.insert(newDetail);

        int recheckCount = Math.max(0,
                qmsProductEventRecheckDetailMapper.selectListByGroupId(group.getId()).size() - 1);
        QmsProductEventRecheckGroupDO updateGroup = new QmsProductEventRecheckGroupDO();
        updateGroup.setId(group.getId());
        updateGroup.setLatestInspectionId(copyResult.newInspectionId);
        updateGroup.setLatestInspectionNo(copyResult.newInspectionNo);
        updateGroup.setLatestRoundNo(nextRoundNo);
        updateGroup.setLatestStatus(STATUS_PENDING);
        updateGroup.setLatestJudgment(JUDGMENT_PENDING);
        updateGroup.setChainStatus(RECHECK_STATUS_RECHECKING);
        updateGroup.setTotalRecheckCount(recheckCount);
        updateGroup.setLatestResultTime(null);
        updateGroup.setLastRejectReason(rejectReason);
        updateGroup.setLastRejectUserId(rejectUserId);
        updateGroup.setLastRejectUserName(rejectUserName);
        updateGroup.setLastRejectTime(rejectTime);
        if (dispatchTaskId != null) {
            updateGroup.setDispatchTaskId(dispatchTaskId);
        }
        qmsProductEventRecheckGroupMapper.updateById(updateGroup);
        updateRootRecheckResult(sourceType, rootInspectionId, JUDGMENT_PENDING, null);

        return new QmsProductAbnormalEventTaskRecheckResult(sourceType, group.getId(), rootInspectionId,
                rootInspectionNo, rootDetailId, inspectionId, currentRow.getInspectionNo(), currentDetail.getId(),
                copyResult.newInspectionId, copyResult.newInspectionNo, newDetail.getId(), nextRoundNo, taskRoundNo,
                copyResult.tenantId, copyResult.itemIdMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDispatchTask(Long groupId, Long dispatchTaskId, Long rootDetailId, Long rootRoundId,
                                 Long sourceDetailId, Long sourceRoundId,
                                 Long newDetailId, Long newRoundId) {
        QmsProductEventRecheckGroupDO group = qmsProductEventRecheckGroupMapper.selectById(groupId);
        if (group == null) {
            throw invalidParamException("驳回复检链路不存在");
        }
        if (group.getDispatchTaskId() != null && !Objects.equals(group.getDispatchTaskId(), dispatchTaskId)) {
            throw invalidParamException("驳回复检链已关联其他质量任务");
        }
        QmsProductEventRecheckGroupDO groupUpdate = new QmsProductEventRecheckGroupDO();
        groupUpdate.setId(groupId);
        groupUpdate.setDispatchTaskId(dispatchTaskId);
        qmsProductEventRecheckGroupMapper.updateById(groupUpdate);
        bindDispatchRound(rootDetailId, rootRoundId);
        bindDispatchRound(sourceDetailId, sourceRoundId);
        bindDispatchRound(newDetailId, newRoundId);
    }

    private void bindDispatchRound(Long detailId, Long roundId) {
        if (detailId == null || roundId == null) {
            return;
        }
        QmsProductEventRecheckDetailDO update = new QmsProductEventRecheckDetailDO();
        update.setId(detailId);
        update.setDispatchTaskRoundId(roundId);
        qmsProductEventRecheckDetailMapper.updateById(update);
    }

    @Override
    public QmsProductAbnormalEventRecheckHistoryRespVO getRecheckHistory(String sourceType, Long inspectionId) {
        String normalizedSourceType = normalizeSourceType(sourceType);
        QmsProductEventRecheckDetailDO detail =
                qmsProductEventRecheckDetailMapper.selectByInspection(normalizedSourceType, inspectionId);
        if (detail == null) {
            return null;
        }
        QmsProductEventRecheckGroupDO group = qmsProductEventRecheckGroupMapper.selectById(detail.getGroupId());
        if (group == null) {
            return null;
        }
        refreshGroupDisplayState(group);
        List<QmsProductEventRecheckDetailDO> details =
                qmsProductEventRecheckDetailMapper.selectListByGroupId(group.getId());
        QmsProductAbnormalEventRecheckHistoryRespVO respVO = new QmsProductAbnormalEventRecheckHistoryRespVO();
        respVO.setGroupId(group.getId());
        respVO.setDispatchTaskId(group.getDispatchTaskId());
        respVO.setSourceType(group.getSourceType());
        respVO.setRootInspectionId(group.getRootInspectionId());
        respVO.setRootInspectionNo(group.getRootInspectionNo());
        respVO.setLatestInspectionId(group.getLatestInspectionId());
        respVO.setLatestInspectionNo(group.getLatestInspectionNo());
        respVO.setChainStatus(group.getChainStatus());
        respVO.setTotalRecheckCount(group.getTotalRecheckCount());
        respVO.setDetails(details.stream().map(item -> {
            QmsProductAbnormalEventRecheckHistoryRespVO.Detail row =
                    new QmsProductAbnormalEventRecheckHistoryRespVO.Detail();
            row.setRoundNo(item.getRoundNo());
            row.setDispatchTaskRoundId(item.getDispatchTaskRoundId());
            row.setInspectionId(item.getInspectionId());
            row.setInspectionNo(item.getInspectionNo());
            row.setPrevInspectionId(item.getPrevInspectionId());
            row.setPrevInspectionNo(item.getPrevInspectionNo());
            row.setInspectionStatus(item.getInspectionStatus());
            row.setInspectionJudgment(item.getInspectionJudgment());
            row.setRejectReason(item.getRejectReason());
            row.setRejectUserId(item.getRejectUserId());
            row.setRejectUserName(item.getRejectUserName());
            row.setRejectTime(item.getRejectTime());
            row.setResultTime(item.getResultTime());
            return row;
        }).collect(Collectors.toList()));
        return respVO;
    }

    private List<QmsProductAbnormalEventRespVO> listFaiRows(QmsProductAbnormalEventPageReqVO reqVO,
                                                            boolean glueBoardFai) {
        LambdaQueryWrapperX<QmsFaiOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsFaiOrderDO::getDeleted, false)
                .and(query -> query.eq(QmsFaiOrderDO::getJudgment, JUDGMENT_NG)
                .or()
                .gt(QmsFaiOrderDO::getAbnormalItemCount, 0)
                .or()
                .eq(QmsFaiOrderDO::getStatus, STATUS_REJECTED));
        if (glueBoardFai) {
            wrapper.eq(QmsFaiOrderDO::getSourceModule, SOURCE_TYPE_GLUE_BOARD_FAI);
        } else {
            wrapper.and(query -> query.isNull(QmsFaiOrderDO::getSourceModule)
                    .or()
                    .ne(QmsFaiOrderDO::getSourceModule, SOURCE_TYPE_GLUE_BOARD_FAI));
        }
        return qmsFaiOrderMapper.selectList(wrapper).stream()
                .map(order -> buildFaiRow(order, glueBoardFai))
                .filter(row -> matchesRequest(row, reqVO))
                .collect(Collectors.toList());
    }

    private List<QmsProductAbnormalEventRespVO> listFqcRows(QmsProductAbnormalEventPageReqVO reqVO,
                                                            String sourceType) {
        LambdaQueryWrapperX<QmsFqcOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsFqcOrderDO::getDeleted, false)
                .eq(QmsFqcOrderDO::getSourceModule, sourceType)
                .and(query -> query.eq(QmsFqcOrderDO::getJudgment, JUDGMENT_NG)
                .or()
                .gt(QmsFqcOrderDO::getAbnormalItemCount, 0)
                .or()
                .gt(QmsFqcOrderDO::getNgQty, 0)
                .or()
                .eq(QmsFqcOrderDO::getStatus, STATUS_REJECTED));
        return qmsFqcOrderMapper.selectList(wrapper).stream()
                .map(order -> buildFqcRow(order, sourceType))
                .filter(row -> matchesRequest(row, reqVO))
                .collect(Collectors.toList());
    }

    private List<QmsProductAbnormalEventRespVO> listOqcRows(QmsProductAbnormalEventPageReqVO reqVO) {
        LambdaQueryWrapperX<QmsOqcOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsOqcOrderDO::getDeleted, false)
                .and(query -> query.eq(QmsOqcOrderDO::getJudgment, JUDGMENT_NG)
                .or()
                .gt(QmsOqcOrderDO::getAbnormalItemCount, 0)
                .or()
                .eq(QmsOqcOrderDO::getStatus, STATUS_REJECTED));
        return qmsOqcOrderMapper.selectList(wrapper).stream()
                .map(this::buildOqcRow)
                .filter(row -> matchesRequest(row, reqVO))
                .collect(Collectors.toList());
    }

    private List<QmsProductAbnormalEventRespVO> mergeRecheckRows(QmsProductAbnormalEventPageReqVO reqVO,
                                                                  List<QmsProductAbnormalEventRespVO> rawRows) {
        List<String> sourceTypes = includedSourceTypes(reqVO);
        if (sourceTypes.isEmpty()) {
            return rawRows;
        }
        Set<String> chainedKeys = new HashSet<>();
        List<QmsProductAbnormalEventRespVO> recheckRows = new ArrayList<>();
        for (QmsProductEventRecheckGroupDO group : qmsProductEventRecheckGroupMapper.selectListBySourceTypes(sourceTypes)) {
            List<QmsProductEventRecheckDetailDO> details =
                    qmsProductEventRecheckDetailMapper.selectListByGroupId(group.getId());
            for (QmsProductEventRecheckDetailDO detail : details) {
                chainedKeys.add(eventKey(detail.getSourceType(), detail.getInspectionId()));
            }
            QmsProductAbnormalEventRespVO row = buildRecheckDisplayRow(group, details);
            if (row != null && matchesRequest(row, reqVO)) {
                recheckRows.add(row);
            }
        }
        List<QmsProductAbnormalEventRespVO> result = rawRows.stream()
                .filter(row -> !chainedKeys.contains(eventKey(row.getSourceType(), row.getInspectionId())))
                .peek(this::applyDefaultRecheckState)
                .collect(Collectors.toCollection(ArrayList::new));
        result.addAll(recheckRows);
        return result;
    }

    private QmsProductAbnormalEventRespVO buildRecheckDisplayRow(QmsProductEventRecheckGroupDO group,
                                                                 List<QmsProductEventRecheckDetailDO> details) {
        if (group == null || details == null || details.isEmpty()) {
            return null;
        }
        refreshGroupDisplayState(group);
        QmsProductAbnormalEventRespVO rootRow = buildSourceRow(group.getSourceType(), group.getRootInspectionId());
        QmsProductAbnormalEventRespVO latestRow = buildSourceRow(group.getSourceType(), group.getLatestInspectionId());
        if (rootRow == null || latestRow == null) {
            return null;
        }
        QmsProductAbnormalEventRespVO row = rootRow;
        QmsProductEventRecheckDetailDO latestDetail = details.get(details.size() - 1);
        row.setJudgment(latestRow.getJudgment());
        row.setStatus(latestRow.getStatus());
        row.setRecheckGroupId(group.getId());
        row.setDispatchTaskId(group.getDispatchTaskId());
        row.setRecheckStatus(group.getChainStatus());
        row.setRecheckStatusName(recheckStatusName(group.getChainStatus()));
        row.setRecheckCount(group.getTotalRecheckCount());
        row.setRecheckRoundNo(latestDetail.getRoundNo());
        row.setRecheckRootInspectionId(group.getRootInspectionId());
        row.setRecheckRootInspectionNo(group.getRootInspectionNo());
        row.setRecheckPrevInspectionId(latestDetail.getPrevInspectionId());
        row.setRecheckPrevInspectionNo(latestDetail.getPrevInspectionNo());
        row.setRecheckLatestInspectionId(group.getLatestInspectionId());
        row.setRecheckLatestInspectionNo(group.getLatestInspectionNo());
        row.setRejectNextInspectionId(firstNotNull(row.getRejectNextInspectionId(), group.getLatestInspectionId()));
        row.setRejectNextInspectionNo(firstNotBlank(row.getRejectNextInspectionNo(), group.getLatestInspectionNo()));
        row.setRecheckResult(group.getLatestJudgment());
        row.setRejectReason(group.getLastRejectReason());
        row.setRejectUserName(group.getLastRejectUserName());
        row.setRejectTime(group.getLastRejectTime());
        row.setCanRejectRecheck(RECHECK_STATUS_NG.equals(group.getChainStatus()));
        row.setCanGenerateNcr(RECHECK_STATUS_NG.equals(group.getChainStatus()));
        if (RECHECK_STATUS_RECHECKING.equals(group.getChainStatus())) {
            row.setAbnormalSummary("驳回复检中，待完成复检判定");
        } else if (RECHECK_STATUS_OK.equals(group.getChainStatus())) {
            row.setAbnormalSummary("原单不合格，复检结果 OK");
        }
        return row;
    }

    private void refreshGroupDisplayState(QmsProductEventRecheckGroupDO group) {
        QmsProductAbnormalEventRespVO latestRow = buildSourceRow(group.getSourceType(), group.getLatestInspectionId());
        if (latestRow == null) {
            return;
        }
        String chainStatus = resolveRecheckStatus(group.getSourceType(), latestRow);
        LocalDateTime resultTime = STATUS_COMPLETED.equals(latestRow.getStatus()) ? latestRow.getInspectionTime() : null;
        QmsProductEventRecheckDetailDO latestDetail =
                qmsProductEventRecheckDetailMapper.selectLatestByGroupId(group.getId());
        if (latestDetail != null) {
            QmsProductEventRecheckDetailDO updateDetail = new QmsProductEventRecheckDetailDO();
            updateDetail.setId(latestDetail.getId());
            updateDetail.setInspectionStatus(latestRow.getStatus());
            updateDetail.setInspectionJudgment(latestRow.getJudgment());
            updateDetail.setResultTime(resultTime);
            qmsProductEventRecheckDetailMapper.updateById(updateDetail);
        }
        group.setLatestStatus(latestRow.getStatus());
        group.setLatestJudgment(latestRow.getJudgment());
        group.setChainStatus(chainStatus);
        group.setLatestResultTime(resultTime);
        QmsProductEventRecheckGroupDO updateGroup = new QmsProductEventRecheckGroupDO();
        updateGroup.setId(group.getId());
        updateGroup.setLatestStatus(group.getLatestStatus());
        updateGroup.setLatestJudgment(group.getLatestJudgment());
        updateGroup.setChainStatus(group.getChainStatus());
        updateGroup.setLatestResultTime(group.getLatestResultTime());
        qmsProductEventRecheckGroupMapper.updateById(updateGroup);
        updateRootRecheckResult(group.getSourceType(), group.getRootInspectionId(), latestRow.getJudgment(), resultTime);
    }

    private String resolveRecheckStatus(String sourceType, QmsProductAbnormalEventRespVO latestRow) {
        if (latestRow == null) {
            return RECHECK_STATUS_RECHECKING;
        }
        if (JUDGMENT_OK.equalsIgnoreCase(StrUtil.blankToDefault(latestRow.getJudgment(), ""))) {
            return RECHECK_STATUS_OK;
        }
        if (JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(latestRow.getJudgment(), ""))
                || STATUS_REJECTED.equalsIgnoreCase(StrUtil.blankToDefault(latestRow.getStatus(), ""))
                || isAbnormalInspection(sourceType, latestRow.getInspectionId())) {
            return RECHECK_STATUS_NG;
        }
        return RECHECK_STATUS_RECHECKING;
    }

    private void applyDefaultRecheckState(QmsProductAbnormalEventRespVO row) {
        row.setRecheckStatus(RECHECK_STATUS_NONE);
        row.setRecheckStatusName(recheckStatusName(RECHECK_STATUS_NONE));
        row.setCanRejectRecheck(isAbnormalInspection(row.getSourceType(), row.getInspectionId()));
        row.setCanGenerateNcr(Boolean.TRUE);
    }

    private void applyDetailRecheckState(QmsProductAbnormalEventDetailRespVO detail) {
        if (detail == null) {
            return;
        }
        QmsProductEventRecheckDetailDO chainDetail =
                qmsProductEventRecheckDetailMapper.selectByInspection(detail.getSourceType(), detail.getInspectionId());
        if (chainDetail == null) {
            detail.setRecheckStatus(RECHECK_STATUS_NONE);
            detail.setRecheckStatusName(recheckStatusName(RECHECK_STATUS_NONE));
            detail.setCanRejectRecheck(isAbnormalInspection(detail.getSourceType(), detail.getInspectionId()));
            detail.setCanGenerateNcr(Boolean.TRUE);
            return;
        }
        QmsProductEventRecheckGroupDO group =
                qmsProductEventRecheckGroupMapper.selectById(chainDetail.getGroupId());
        if (group == null) {
            detail.setRecheckStatus(RECHECK_STATUS_NONE);
            detail.setRecheckStatusName(recheckStatusName(RECHECK_STATUS_NONE));
            detail.setCanRejectRecheck(Boolean.FALSE);
            detail.setCanGenerateNcr(Boolean.FALSE);
            return;
        }
        refreshGroupDisplayState(group);
        boolean latestNg = RECHECK_STATUS_NG.equals(group.getChainStatus())
                && Objects.equals(group.getLatestInspectionId(), detail.getInspectionId());
        detail.setRecheckGroupId(group.getId());
        detail.setRecheckStatus(group.getChainStatus());
        detail.setRecheckStatusName(recheckStatusName(group.getChainStatus()));
        detail.setRecheckCount(group.getTotalRecheckCount());
        detail.setRecheckRoundNo(chainDetail.getRoundNo());
        detail.setRecheckRootInspectionId(group.getRootInspectionId());
        detail.setRecheckRootInspectionNo(group.getRootInspectionNo());
        detail.setRecheckPrevInspectionId(chainDetail.getPrevInspectionId());
        detail.setRecheckPrevInspectionNo(chainDetail.getPrevInspectionNo());
        detail.setRecheckLatestInspectionId(group.getLatestInspectionId());
        detail.setRecheckLatestInspectionNo(group.getLatestInspectionNo());
        detail.setRecheckResult(group.getLatestJudgment());
        detail.setRejectReason(group.getLastRejectReason());
        detail.setRejectUserName(group.getLastRejectUserName());
        detail.setRejectTime(group.getLastRejectTime());
        detail.setCanRejectRecheck(latestNg);
        detail.setCanGenerateNcr(latestNg);
    }

    private QmsProductAbnormalEventRespVO buildSourceRow(String sourceType, Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(inspectionId);
            return order == null ? null : buildFaiRow(order, SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType));
        }
        if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(inspectionId);
            return order == null ? null : buildFqcRow(order, sourceType);
        }
        if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO order = qmsIqcOrderMapper.selectById(inspectionId);
            return order == null ? null : buildIqcRow(order);
        }
        if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO order = qmsOqcOrderMapper.selectById(inspectionId);
            return order == null ? null : buildOqcRow(order);
        }
        return null;
    }

    private List<String> includedSourceTypes(QmsProductAbnormalEventPageReqVO reqVO) {
        List<String> all = List.of(SOURCE_TYPE_FAI, SOURCE_TYPE_GLUE_BOARD_FAI, SOURCE_TYPE_CUT_ROUND_FQC,
                SOURCE_TYPE_FG_SHIPPING_FQC, SOURCE_TYPE_OQC);
        String requested = normalizeSourceType(reqVO.getSourceType());
        if (StrUtil.isBlank(requested)) {
            return all;
        }
        if (SOURCE_TYPE_IQC.equals(requested)) {
            return List.of(SOURCE_TYPE_IQC);
        }
        return all.contains(requested) ? List.of(requested) : Collections.emptyList();
    }

    private String eventKey(String sourceType, Long inspectionId) {
        return normalizeSourceType(sourceType) + ":" + inspectionId;
    }

    private QmsProductAbnormalEventRespVO buildFaiRow(QmsFaiOrderDO order, boolean glueBoardFai) {
        QmsProductAbnormalEventRespVO row = new QmsProductAbnormalEventRespVO();
        row.setSourceType(glueBoardFai ? SOURCE_TYPE_GLUE_BOARD_FAI : SOURCE_TYPE_FAI);
        row.setInspectionType(glueBoardFai ? "胶板检验" : "首件检验");
        row.setInspectionId(order.getId());
        row.setStandardId(order.getStandardId());
        row.setEventKey(row.getSourceType() + ":" + order.getId());
        row.setInspectionNo(order.getFaiNo());
        row.setOperationName(firstNotBlank(order.getOperationName(), order.getSourceOperationName(), order.getProcessCategory()));
        row.setProcessCategory(order.getProcessCategory());
        row.setProductModel(glueBoardFai
                ? firstNotBlank(order.getGlueBoardModel(), order.getProductModel(), order.getMaterialName(), order.getSpecification())
                : firstNotBlank(order.getProductModel(), order.getMaterialName(), order.getSpecification()));
        row.setSpecification(order.getSpecification());
        row.setProductBatchNo(glueBoardFai
                ? firstNotBlank(order.getGluePlateBatchNo(), order.getProductBatchNo())
                : order.getProductBatchNo());
        row.setInspectionQty(order.getInspectionQty());
        row.setUnqualifiedQty(bigDecimal(order.getAbnormalItemCount()));
        row.setInspectionTime(firstNotNull(order.getInspectionTime(), order.getQaTime(),
                order.getSubmissionTime(), order.getUpdateTime(), order.getCreateTime()));
        row.setJudgment(order.getJudgment());
        row.setStatus(order.getStatus());
        row.setRejectNextInspectionId(order.getRejectNextInspectionId());
        row.setRejectNextInspectionNo(order.getRejectNextInspectionNo());
        return row;
    }

    private QmsProductAbnormalEventRespVO buildFqcRow(QmsFqcOrderDO order, String sourceType) {
        QmsProductAbnormalEventRespVO row = new QmsProductAbnormalEventRespVO();
        row.setSourceType(sourceType);
        row.setInspectionType(SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) ? "裁切成品检验" : "发货成品检验");
        row.setInspectionId(order.getId());
        row.setStandardId(order.getStandardId());
        row.setEventKey(row.getSourceType() + ":" + order.getId());
        row.setInspectionNo(order.getFqcNo());
        row.setOperationName(SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType)
                ? firstNotBlank(order.getOperationName(), order.getSourceOperationName(), "裁切")
                : firstNotBlank(order.getOperationName(), order.getSourceOperationName(), "发货"));
        row.setProcessCategory(PROCESS_CATEGORY_FINAL_PRODUCTS);
        row.setProductModel(firstNotBlank(order.getProductModel(), order.getMaterialName(), order.getSpecification()));
        row.setSpecification(order.getSpecification());
        row.setProductBatchNo(firstNotBlank(order.getProductBatchNo(), order.getBatchNo()));
        row.setInspectionQty(firstNotNull(order.getProduceQty(),
                order.getSampleQty() == null ? null : BigDecimal.valueOf(order.getSampleQty())));
        row.setUnqualifiedQty(bigDecimal(firstPositiveInteger(order.getNgQty(), order.getAbnormalItemCount())));
        row.setInspectionTime(firstNotNull(order.getInspectionTime(), order.getQaTime(),
                order.getSubmissionTime(), order.getUpdateTime(), order.getCreateTime()));
        row.setJudgment(order.getJudgment());
        row.setStatus(order.getStatus());
        row.setRejectNextInspectionId(order.getRejectNextInspectionId());
        row.setRejectNextInspectionNo(order.getRejectNextInspectionNo());
        return row;
    }

    private QmsProductAbnormalEventRespVO buildIqcRow(QmsIqcOrderDO order) {
        QmsProductAbnormalEventRespVO row = new QmsProductAbnormalEventRespVO();
        row.setSourceType(SOURCE_TYPE_IQC);
        row.setInspectionType("进料检验(IQC)");
        row.setInspectionId(order.getId());
        row.setStandardId(order.getStandardId());
        row.setEventKey(row.getSourceType() + ":" + order.getId());
        row.setInspectionNo(order.getIqcNo());
        row.setOperationName("进料检验");
        row.setProductModel(firstNotBlank(order.getProductModelCode(), order.getModelNo(), order.getMaterialName()));
        row.setSpecification(firstNotBlank(order.getSpecification(), order.getModelNo()));
        row.setProductBatchNo(order.getBatchNo());
        row.setInspectionQty(order.getReceiveQty());
        row.setUnqualifiedQty(JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(order.getJudgment(), ""))
                ? order.getReceiveQty() : null);
        row.setInspectionTime(firstNotNull(order.getInspectionTime(), order.getQaTime(),
                order.getInspectionApplyTime(), order.getUpdateTime(), order.getCreateTime()));
        row.setJudgment(order.getJudgment());
        row.setStatus(order.getStatus());
        row.setRejectNextInspectionId(order.getRejectNextInspectionId());
        row.setRejectNextInspectionNo(order.getRejectNextInspectionNo());
        return row;
    }

    private QmsProductAbnormalEventDetailRespVO buildIqcDetail(QmsIqcRespVO record) {
        if (record == null) {
            return null;
        }
        QmsProductAbnormalEventDetailRespVO detail = new QmsProductAbnormalEventDetailRespVO();
        detail.setSourceType(SOURCE_TYPE_IQC);
        detail.setInspectionType("进料检验(IQC)");
        detail.setInspectionId(record.getId());
        detail.setStandardId(record.getStandardId());
        detail.setEventKey(SOURCE_TYPE_IQC + ":" + record.getId());
        detail.setInspectionNo(record.getIqcNo());
        detail.setSourceReportNo(record.getReceiptNo());
        detail.setOperationName("进料检验");
        detail.setMaterialCode(record.getMaterialCode());
        detail.setMaterialName(record.getMaterialName());
        detail.setSpecification(firstNotBlank(record.getSpecification(), record.getModelNo()));
        detail.setProductModel(firstNotBlank(record.getProductModelCode(), record.getProductModelName(),
                record.getModelNo(), record.getMaterialName()));
        detail.setProductBatchNo(record.getBatchNo());
        detail.setInspectionQty(record.getReceiveQty());
        detail.setSampleQty(record.getItems() == null ? null : record.getItems().stream()
                .map(QmsIqcRespVO.IqcItem::getSampleSize)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        detail.setInspectionTime(firstNotNull(record.getInspectionTime(), record.getQaTime(),
                record.getInspectionApplyTime(), record.getCreateTime()));
        detail.setSubmissionTime(record.getInspectionApplyTime());
        detail.setInspectorName(firstNotBlank(record.getInspectorName(), record.getQaInspectorName()));
        detail.setSubmitterName(record.getReceiverName());
        detail.setJudgment(record.getJudgment());
        detail.setStatus(record.getStatus());
        detail.setRemark(record.getRemark());
        detail.setCreateTime(record.getCreateTime());

        List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows = new ArrayList<>();
        if (record.getItems() != null) {
            for (QmsIqcRespVO.IqcItem item : record.getItems()) {
                QmsProductAbnormalEventDetailRespVO.DetailItem row =
                        new QmsProductAbnormalEventDetailRespVO.DetailItem();
                row.setRowNo(rows.size() + 1);
                row.setSectionName("IQC检验项");
                row.setInspectionItem(item.getInspectionItem());
                row.setRecheckItemFlag(hasIqcRecheckFlag(item));
                row.setItemType(item.getItemType());
                row.setStandardDesc(firstNotBlank(item.getStandardDesc(), item.getRuleDescription()));
                row.setSampleSize(item.getSampleSize());
                row.setMeasuredValue(buildIqcMeasuredValue(item));
                row.setUnit(item.getUnit());
                row.setResult(item.getItemResult());
                QmsIqcRespVO.IqcSample abnormalSample = firstIqcAbnormalSample(item);
                if (abnormalSample != null) {
                    row.setDefectCode(abnormalSample.getDefectCode());
                    row.setDefectName(abnormalSample.getDefectName());
                }
                row.setAbnormalDesc(isAbnormalResult(row.getResult()) ? "检验项不合格" : null);
                row.setInspectorName(detail.getInspectorName());
                row.setInspectionTime(detail.getInspectionTime());
                rows.add(row);
            }
        }
        detail.setDetails(rows);
        applySummary(detail, buildIqcSummary(record.getId(), record.getJudgment()));
        return detail;
    }

    private QmsProductAbnormalEventRespVO buildOqcRow(QmsOqcOrderDO order) {
        QmsProductAbnormalEventRespVO row = new QmsProductAbnormalEventRespVO();
        row.setSourceType(SOURCE_TYPE_OQC);
        row.setInspectionType("出货检验(OQC)");
        row.setInspectionId(order.getId());
        row.setStandardId(order.getStandardId());
        row.setEventKey(row.getSourceType() + ":" + order.getId());
        row.setInspectionNo(order.getOqcNo());
        row.setOperationName("出货");
        row.setProcessCategory(PROCESS_CATEGORY_FINAL_PRODUCTS);
        row.setProductModel(firstNotBlank(order.getModelCode(), order.getMaterialName(), order.getSpecification()));
        row.setSpecification(order.getSpecification());
        row.setProductBatchNo(firstNotBlank(order.getBatchNo(), order.getCustomerBatchNo()));
        row.setInspectionQty(order.getShippingQty());
        row.setUnqualifiedQty(bigDecimal(order.getAbnormalItemCount()));
        row.setInspectionTime(firstNotNull(order.getInspectionTime(), order.getQaTime(),
                order.getUpdateTime(), order.getCreateTime()));
        row.setJudgment(order.getJudgment());
        row.setStatus(order.getStatus());
        row.setRejectNextInspectionId(order.getRejectNextInspectionId());
        row.setRejectNextInspectionNo(order.getRejectNextInspectionNo());
        return row;
    }

    private QmsProductAbnormalEventDetailRespVO buildFaiDetail(String sourceType, QmsFaiRespVO record) {
        if (record == null) {
            return null;
        }
        boolean glueBoardFai = SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType);
        QmsProductAbnormalEventDetailRespVO detail = new QmsProductAbnormalEventDetailRespVO();
        detail.setSourceType(sourceType);
        detail.setInspectionType(glueBoardFai ? "胶板检验" : "首件检验");
        detail.setInspectionId(record.getId());
        detail.setStandardId(record.getStandardId());
        detail.setEventKey(sourceType + ":" + record.getId());
        detail.setInspectionNo(record.getFaiNo());
        detail.setSourceReportNo(record.getSourceReportNo());
        detail.setWorkOrderNo(record.getWorkOrderNo());
        detail.setOperationName(firstNotBlank(record.getOperationName(), record.getSourceOperationName(), record.getProcessCategory()));
        detail.setMaterialCode(glueBoardFai ? firstNotBlank(record.getGlueBoardMaterialCode(), record.getMaterialCode()) : record.getMaterialCode());
        detail.setMaterialName(record.getMaterialName());
        detail.setSpecification(record.getSpecification());
        detail.setProductModel(glueBoardFai
                ? firstNotBlank(record.getGlueBoardModel(), record.getProductModel(), record.getMaterialName(), record.getSpecification())
                : firstNotBlank(record.getProductModel(), record.getMaterialName(), record.getSpecification()));
        detail.setProductBatchNo(glueBoardFai
                ? firstNotBlank(record.getGluePlateBatchNo(), record.getProductBatchNo())
                : record.getProductBatchNo());
        detail.setInspectionQty(record.getInspectionQty());
        detail.setInspectionTime(firstNotNull(record.getInspectionTime(), record.getQaTime(), record.getSubmissionTime(), record.getCreateTime()));
        detail.setSubmissionTime(record.getSubmissionTime());
        detail.setInspectorName(firstNotBlank(record.getQaInspectorName(), record.getOperatorName()));
        detail.setSubmitterName(record.getSubmitterName());
        detail.setJudgment(record.getJudgment());
        detail.setStatus(record.getStatus());
        detail.setRemark(record.getRemark());
        detail.setCreateTime(record.getCreateTime());

        List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows = new ArrayList<>();
        if (record.getItems() != null) {
            for (QmsFaiRespVO.FaiItem item : record.getItems()) {
                QmsProductAbnormalEventDetailRespVO.DetailItem row = new QmsProductAbnormalEventDetailRespVO.DetailItem();
                row.setRowNo(rows.size() + 1);
                row.setSectionName(firstNotBlank(item.getSheetSectionName(), item.getStepName()));
                row.setInspectionItem(firstNotBlank(item.getSheetMetricName(), item.getInspectionItem()));
                row.setRecheckItemFlag(hasFaiRecheckFlag(item));
                row.setItemType(item.getItemType());
                row.setStandardDesc(firstNotBlank(item.getStandardDesc(), item.getRuleDescription()));
                row.setSampleSize(firstNotNull(item.getSampleSize(), item.getRequiredSampleCount()));
                row.setMeasuredValue(formatStats(firstNotNull(item.getQaMin(), item.getOperatorMin(), item.getCalculatedMin()),
                        firstNotNull(item.getQaMax(), item.getOperatorMax(), item.getCalculatedMax()),
                        firstNotNull(item.getQaAvg(), item.getOperatorAvg(), item.getCalculatedAvg())));
                row.setUnit(item.getUnit());
                row.setResult(firstNotBlank(item.getQaResult(), item.getOperatorResult(), item.getInputStatus()));
                row.setAbnormalDesc(isAbnormalResult(row.getResult()) ? "检验项不合格" : null);
                row.setInspectorName(firstNotBlank(item.getQaInspectorName(), item.getOperatorName(), detail.getInspectorName()));
                row.setInspectionTime(firstNotNull(item.getQaTime(), item.getOperatorTime(), detail.getInspectionTime()));
                rows.add(row);
            }
        }
        detail.setDetails(rows);
        applySummary(detail, buildFaiSummary(record.getId(), record.getJudgment()));
        return detail;
    }

    private List<QmsProductAbnormalEventDetailRespVO.AbnormalItem> buildCutRoundAbnormalItems(
            Long fqcId,
            List<QmsCutRoundFqcRespVO.SubmissionDetail> submissionDetails) {
        Map<Long, String> batchNoByDetailId = new HashMap<>();
        if (submissionDetails != null) {
            for (QmsCutRoundFqcRespVO.SubmissionDetail detail : submissionDetails) {
                batchNoByDetailId.put(detail.getId(), detail.getProductionBatchNo());
            }
        }

        Map<String, QmsProductAbnormalEventDetailRespVO.AbnormalItem> uniqueItems = new LinkedHashMap<>();
        Set<String> rowLevelAbnormalTargets = new HashSet<>();
        if (submissionDetails != null) {
            for (QmsCutRoundFqcRespVO.SubmissionDetail detail : submissionDetails) {
                String targetNo = StrUtil.trim(detail.getProductionBatchNo());
                if (!JUDGMENT_NG.equals(detail.getRowJudgment()) || StrUtil.isBlank(targetNo)) {
                    continue;
                }
                QmsProductAbnormalEventDetailRespVO.AbnormalItem abnormalItem =
                        new QmsProductAbnormalEventDetailRespVO.AbnormalItem();
                abnormalItem.setTargetNo(targetNo);
                abnormalItem.setInspectionItem(firstNotBlank(detail.getNgReason(), detail.getDefectName(),
                        detail.getDefectCode(), "片级判定NG"));
                rowLevelAbnormalTargets.add(targetNo);
                uniqueItems.putIfAbsent(targetNo + "\u0000" + abnormalItem.getInspectionItem(),
                        abnormalItem);
            }
        }
        for (QmsFqcItemDO item : qmsFqcItemMapper.selectListByFqcId(fqcId)) {
            if (!isFqcItemAbnormal(item)) {
                continue;
            }
            String targetNo = StrUtil.trim(firstNotBlank(item.getProductionBatchNo(),
                    batchNoByDetailId.get(item.getSubmissionDetailId())));
            if (rowLevelAbnormalTargets.contains(targetNo)) {
                continue;
            }
            String inspectionItem = firstNotBlank(item.getSheetMetricName(), item.getInspectionItem(), item.getMetricCode());
            if (StrUtil.isBlank(targetNo) || StrUtil.isBlank(inspectionItem)) {
                continue;
            }
            QmsProductAbnormalEventDetailRespVO.AbnormalItem abnormalItem =
                    new QmsProductAbnormalEventDetailRespVO.AbnormalItem();
            abnormalItem.setTargetNo(targetNo);
            abnormalItem.setInspectionItem(inspectionItem);
            uniqueItems.putIfAbsent(targetNo + "\u0000" + inspectionItem, abnormalItem);
        }
        return new ArrayList<>(uniqueItems.values());
    }

    private QmsProductAbnormalEventDetailRespVO buildCutRoundFqcDetail(QmsCutRoundFqcRespVO record) {
        if (record == null) {
            return null;
        }
        QmsProductAbnormalEventDetailRespVO detail = fillFqcHeader(record.getId(), record.getFqcNo(),
                SOURCE_TYPE_CUT_ROUND_FQC, "裁切成品检验", record.getSourceReportNo(), record.getWorkOrderNo(),
                firstNotBlank(record.getOperationName(), record.getSourceOperationName(), "裁切"),
                record.getMaterialCode(), record.getMaterialName(), record.getSpecification(),
                firstNotBlank(record.getProductModel(), record.getMaterialName(), record.getSpecification()),
                firstNotBlank(record.getProductBatchNo(), record.getBatchNo()), null, record.getProduceQty(),
                record.getSampleQty(), record.getInspectionTime(), record.getSubmissionTime(), record.getInspectorName(),
                record.getSubmitterName(), record.getJudgment(), record.getStatus(), record.getRemark(), record.getCreateTime());
        detail.setStandardId(record.getStandardId());

        List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows = new ArrayList<>();
        if (record.getSubmissionDetails() != null) {
            for (QmsCutRoundFqcRespVO.SubmissionDetail submissionDetail : record.getSubmissionDetails()) {
                addCutRoundSubmissionDetail(rows, submissionDetail);
                addFqcItemRows(rows, submissionDetail.getItems(), detail.getInspectorName(), detail.getInspectionTime());
            }
        } else {
            addFqcItemRows(rows, record.getItems(), detail.getInspectorName(), detail.getInspectionTime());
        }
        detail.setDetails(rows);
        List<QmsProductAbnormalEventDetailRespVO.AbnormalItem> abnormalItems =
                buildCutRoundAbnormalItems(record.getId(), record.getSubmissionDetails());
        detail.setAbnormalItems(abnormalItems);
        applySummary(detail, buildCutRoundFqcSummary(record.getId(), record.getJudgment()));
        return detail;
    }

    private QmsProductAbnormalEventDetailRespVO buildFgShippingFqcDetail(QmsFgShippingFqcRespVO record) {
        if (record == null) {
            return null;
        }
        QmsProductAbnormalEventDetailRespVO detail = fillFqcHeader(record.getId(), record.getFqcNo(),
                SOURCE_TYPE_FG_SHIPPING_FQC, "发货成品检验", firstNotBlank(record.getShippingNoticeNo(), record.getSourceReportNo()),
                record.getWorkOrderNo(), "发货", record.getMaterialCode(), record.getMaterialName(), record.getSpecification(),
                firstNotBlank(record.getProductModel(), record.getMaterialName(), record.getSpecification()),
                firstNotBlank(record.getProductBatchNo(), record.getBatchNo()), record.getCustomerName(), record.getProduceQty(),
                record.getSampleQty(), record.getInspectionTime(), record.getSubmissionTime(), record.getInspectorName(),
                record.getSubmitterName(), record.getJudgment(), record.getStatus(), record.getRemark(), record.getCreateTime());
        detail.setStandardId(record.getStandardId());

        List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows = new ArrayList<>();
        if (record.getShippingDetails() != null) {
            for (QmsFgShippingFqcRespVO.ShippingDetail shippingDetail : record.getShippingDetails()) {
                addFgShippingDetail(rows, shippingDetail);
                addFqcItemRows(rows, shippingDetail.getItems(), detail.getInspectorName(), detail.getInspectionTime());
            }
        } else {
            addFqcItemRows(rows, record.getItems(), detail.getInspectorName(), detail.getInspectionTime());
        }
        detail.setDetails(rows);
        applySummary(detail, buildFgShippingFqcSummary(record.getId(), record.getJudgment()));
        return detail;
    }

    private QmsProductAbnormalEventDetailRespVO buildOqcDetail(QmsOqcRespVO record) {
        if (record == null) {
            return null;
        }
        QmsProductAbnormalEventDetailRespVO detail = new QmsProductAbnormalEventDetailRespVO();
        detail.setSourceType(SOURCE_TYPE_OQC);
        detail.setInspectionType("出货检验(OQC)");
        detail.setInspectionId(record.getId());
        detail.setStandardId(record.getStandardId());
        detail.setEventKey(SOURCE_TYPE_OQC + ":" + record.getId());
        detail.setInspectionNo(record.getOqcNo());
        detail.setSourceReportNo(firstNotBlank(record.getShippingNo(), record.getNoticeNo()));
        detail.setOperationName("出货");
        detail.setMaterialCode(record.getMaterialCode());
        detail.setMaterialName(record.getMaterialName());
        detail.setSpecification(record.getSpecification());
        detail.setProductModel(firstNotBlank(record.getModelCode(), record.getMaterialName(), record.getSpecification()));
        detail.setProductBatchNo(firstNotBlank(record.getBatchNo(), record.getCustomerBatchNo()));
        detail.setCustomerName(record.getCustomerName());
        detail.setInspectionQty(record.getShippingQty());
        detail.setSampleQty(record.getSampleQty());
        detail.setInspectionTime(firstNotNull(record.getInspectionTime(), record.getQaTime(), record.getCreateTime()));
        detail.setInspectorName(firstNotBlank(record.getInspectorName(), record.getQaInspectorName()));
        detail.setJudgment(record.getJudgment());
        detail.setStatus(record.getStatus());
        detail.setRemark(record.getRemark());
        detail.setCreateTime(record.getCreateTime());

        List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows = new ArrayList<>();
        if (record.getItems() != null) {
            for (QmsOqcRespVO.OqcItem item : record.getItems()) {
                QmsProductAbnormalEventDetailRespVO.DetailItem row = new QmsProductAbnormalEventDetailRespVO.DetailItem();
                row.setRowNo(rows.size() + 1);
                row.setSectionName(item.getCategory());
                row.setInspectionItem(item.getInspectionItem());
                row.setRecheckItemFlag(hasOqcRecheckFlag(item));
                row.setItemType(item.getItemType());
                row.setStandardDesc(firstNotBlank(item.getStandardDesc(), item.getRuleDescription()));
                row.setSampleSize(firstNotNull(item.getSampleSize(), item.getRequiredSampleCount()));
                row.setMeasuredValue(formatStats(firstNotNull(item.getMinValue(), item.getOperatorMin(), item.getCalculatedMin()),
                        firstNotNull(item.getMaxValue(), item.getOperatorMax(), item.getCalculatedMax()),
                        firstNotNull(item.getAverageValue(), item.getOperatorAvg(), item.getCalculatedAvg())));
                row.setUnit(item.getUnit());
                row.setResult(firstNotBlank(item.getItemResult(), item.getQaResult(), item.getOperatorResult(), item.getInputStatus()));
                row.setAbnormalDesc(isAbnormalResult(row.getResult()) ? "检验项不合格" : null);
                row.setInspectorName(detail.getInspectorName());
                row.setInspectionTime(detail.getInspectionTime());
                rows.add(row);
            }
        }
        detail.setDetails(rows);
        applySummary(detail, buildOqcSummary(record.getId(), record.getJudgment()));
        return detail;
    }

    private QmsProductAbnormalEventDetailRespVO fillFqcHeader(Long id, String fqcNo, String sourceType,
                                                              String inspectionType, String sourceReportNo,
                                                              String workOrderNo, String operationName,
                                                              String materialCode, String materialName,
                                                              String specification, String productModel,
                                                              String productBatchNo, String customerName,
                                                              BigDecimal inspectionQty, Integer sampleQty,
                                                              LocalDateTime inspectionTime, LocalDateTime submissionTime,
                                                              String inspectorName, String submitterName,
                                                              String judgment, String status, String remark,
                                                              LocalDateTime createTime) {
        QmsProductAbnormalEventDetailRespVO detail = new QmsProductAbnormalEventDetailRespVO();
        detail.setSourceType(sourceType);
        detail.setInspectionType(inspectionType);
        detail.setInspectionId(id);
        detail.setEventKey(sourceType + ":" + id);
        detail.setInspectionNo(fqcNo);
        detail.setSourceReportNo(sourceReportNo);
        detail.setWorkOrderNo(workOrderNo);
        detail.setOperationName(operationName);
        detail.setMaterialCode(materialCode);
        detail.setMaterialName(materialName);
        detail.setSpecification(specification);
        detail.setProductModel(productModel);
        detail.setProductBatchNo(productBatchNo);
        detail.setCustomerName(customerName);
        detail.setInspectionQty(inspectionQty);
        detail.setSampleQty(sampleQty);
        detail.setInspectionTime(firstNotNull(inspectionTime, submissionTime, createTime));
        detail.setSubmissionTime(submissionTime);
        detail.setInspectorName(inspectorName);
        detail.setSubmitterName(submitterName);
        detail.setJudgment(judgment);
        detail.setStatus(status);
        detail.setRemark(remark);
        detail.setCreateTime(createTime);
        return detail;
    }

    private void addCutRoundSubmissionDetail(List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows,
                                             QmsCutRoundFqcRespVO.SubmissionDetail detail) {
        QmsProductAbnormalEventDetailRespVO.DetailItem row = new QmsProductAbnormalEventDetailRespVO.DetailItem();
        row.setRowNo(rows.size() + 1);
        row.setSectionName("送检明细");
        row.setInspectionItem(firstNotBlank(detail.getProductionBatchNo(), detail.getParentProductionBatchNo(), detail.getMaterialName()));
        row.setRecheckDetailFlag(detail.getRecheckDetailFlag());
        row.setStandardDesc(firstNotBlank(detail.getModelCode(), detail.getSizeRule()));
        row.setResult(detail.getRowJudgment());
        row.setDefectCode(detail.getDefectCode());
        row.setDefectName(detail.getDefectName());
        row.setAbnormalDesc(detail.getNgReason());
        row.setInspectorName(detail.getInspectorName());
        row.setInspectionTime(detail.getInspectionTime());
        row.setRemark(detail.getRemark());
        rows.add(row);
    }

    private void addFgShippingDetail(List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows,
                                     QmsFgShippingFqcRespVO.ShippingDetail detail) {
        QmsProductAbnormalEventDetailRespVO.DetailItem row = new QmsProductAbnormalEventDetailRespVO.DetailItem();
        row.setRowNo(rows.size() + 1);
        row.setSectionName("发货明细");
        row.setInspectionItem(firstNotBlank(detail.getActualSliceBatchNo(), detail.getSliceBatchNo(), detail.getStockNo()));
        row.setRecheckDetailFlag(detail.getRecheckDetailFlag());
        row.setStandardDesc(firstNotBlank(detail.getModelCode(), detail.getProductSize(), detail.getMaterialName()));
        row.setSampleSize(detail.getShippingQty());
        row.setResult(detail.getRowJudgment());
        row.setDefectCode(detail.getDefectCode());
        row.setDefectName(detail.getDefectName());
        row.setAbnormalDesc(firstNotBlank(detail.getNgReason(), detail.getMismatchReason()));
        row.setInspectorName(detail.getInspectorName());
        row.setInspectionTime(detail.getInspectionTime());
        row.setRemark(detail.getRemark());
        rows.add(row);
    }

    private void addFqcItemRows(List<QmsProductAbnormalEventDetailRespVO.DetailItem> rows,
                                List<QmsFqcRespVO.FqcItem> items,
                                String inspectorName,
                                LocalDateTime inspectionTime) {
        if (items == null) {
            return;
        }
        for (QmsFqcRespVO.FqcItem item : items) {
            QmsProductAbnormalEventDetailRespVO.DetailItem row = new QmsProductAbnormalEventDetailRespVO.DetailItem();
            row.setRowNo(rows.size() + 1);
            row.setSectionName(firstNotBlank(item.getSheetSectionName(), item.getStepName(), item.getCategory()));
            row.setInspectionItem(firstNotBlank(item.getSheetMetricName(), item.getInspectionItem()));
            row.setRecheckItemFlag(hasFqcRecheckFlag(item));
            row.setItemType(item.getItemType());
            row.setStandardDesc(firstNotBlank(item.getStandardDesc(), item.getRuleDescription()));
            row.setSampleSize(firstNotNull(item.getSampleSize(), item.getRequiredSampleCount()));
            row.setMeasuredValue(formatStats(firstNotNull(item.getMinValue(), item.getQaMin(), item.getOperatorMin(), item.getCalculatedMin()),
                    firstNotNull(item.getMaxValue(), item.getQaMax(), item.getOperatorMax(), item.getCalculatedMax()),
                    firstNotNull(item.getAverageValue(), item.getQaAvg(), item.getOperatorAvg(), item.getCalculatedAvg())));
            row.setUnit(item.getUnit());
            row.setResult(firstNotBlank(item.getItemResult(), item.getQaResult(), item.getOperatorResult(), item.getInputStatus()));
            row.setAbnormalDesc(isAbnormalResult(row.getResult()) ? "检验项不合格" : null);
            row.setInspectorName(firstNotBlank(item.getOperatorName(), inspectorName));
            row.setInspectionTime(firstNotNull(item.getOperatorTime(), inspectionTime));
            rows.add(row);
        }
    }

    private boolean hasFaiRecheckFlag(QmsFaiRespVO.FaiItem item) {
        return item != null && (Boolean.TRUE.equals(item.getRecheckItemFlag())
                || item.getSamples() != null && item.getSamples().stream()
                .anyMatch(sample -> Boolean.TRUE.equals(sample.getRecheckItemFlag())));
    }

    private boolean hasFqcRecheckFlag(QmsFqcRespVO.FqcItem item) {
        return item != null && (Boolean.TRUE.equals(item.getRecheckItemFlag())
                || item.getSamples() != null && item.getSamples().stream()
                .anyMatch(sample -> Boolean.TRUE.equals(sample.getRecheckItemFlag())));
    }

    private boolean hasIqcRecheckFlag(QmsIqcRespVO.IqcItem item) {
        return item != null && (Boolean.TRUE.equals(item.getRecheckItemFlag())
                || item.getSamples() != null && item.getSamples().stream()
                .anyMatch(sample -> Boolean.TRUE.equals(sample.getRecheckItemFlag())));
    }

    private boolean hasOqcRecheckFlag(QmsOqcRespVO.OqcItem item) {
        return item != null && (Boolean.TRUE.equals(item.getRecheckItemFlag())
                || item.getSamples() != null && item.getSamples().stream()
                .anyMatch(sample -> Boolean.TRUE.equals(sample.getRecheckItemFlag())));
    }

    private String buildIqcMeasuredValue(QmsIqcRespVO.IqcItem item) {
        if (item == null) {
            return null;
        }
        String stats = formatStats(item.getMinValue(), item.getMaxValue(), item.getAverageValue());
        if (StrUtil.isNotBlank(stats)) {
            return stats;
        }
        if (item.getSamples() == null) {
            return null;
        }
        return item.getSamples().stream()
                .sorted(Comparator.comparing(QmsIqcRespVO.IqcSample::getSampleSeq,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::buildIqcSampleValue)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .limit(8)
                .collect(Collectors.joining("、"));
    }

    private String buildIqcSampleValue(QmsIqcRespVO.IqcSample sample) {
        if (sample == null) {
            return null;
        }
        String value = firstNotBlank(sample.getRawValuesJson(), decimalText(sample.getMeasuredValue()),
                decimalText(sample.getResultValue()), sample.getQualitativeValue());
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return sample.getSampleSeq() == null ? value : "第" + sample.getSampleSeq() + "组：" + value;
    }

    private QmsIqcRespVO.IqcSample firstIqcAbnormalSample(QmsIqcRespVO.IqcItem item) {
        if (item == null || item.getSamples() == null) {
            return null;
        }
        return item.getSamples().stream()
                .filter(sample -> isAbnormalResult(sample.getSampleResult())
                        || isAbnormalResult(sample.getQualitativeValue())
                        || StrUtil.isNotBlank(sample.getDefectCode())
                        || StrUtil.isNotBlank(sample.getDefectName()))
                .findFirst()
                .orElse(null);
    }

    private void fillDetailNcrStatus(QmsProductAbnormalEventDetailRespVO detail) {
        QmsProductAbnormalEventRespVO row = new QmsProductAbnormalEventRespVO();
        row.setSourceType(detail.getSourceType());
        row.setInspectionId(detail.getInspectionId());
        QmsNcRecordDO existing = findExistingNcr(row);
        boolean generated = existing != null && existing.getId() != null;
        detail.setNcrGenerated(generated);
        detail.setNcrStatus(generated ? NCR_STATUS_GENERATED : NCR_STATUS_PENDING);
        if (generated) {
            detail.setNcrId(existing.getId());
            detail.setNcrNo(existing.getNcNo());
        }
    }

    private void fillNcrStatus(List<QmsProductAbnormalEventRespVO> rows) {
        for (QmsProductAbnormalEventRespVO row : rows) {
            QmsNcRecordDO existing = findExistingNcr(row);
            boolean generated = existing != null && existing.getId() != null;
            row.setNcrGenerated(generated);
            row.setNcrStatus(generated ? NCR_STATUS_GENERATED : NCR_STATUS_PENDING);
            if (generated) {
                row.setNcrId(existing.getId());
                row.setNcrNo(existing.getNcNo());
            }
        }
    }

    private QmsNcRecordDO findExistingNcr(QmsProductAbnormalEventRespVO row) {
        if (row == null || row.getInspectionId() == null || StrUtil.isBlank(row.getSourceType())) {
            return null;
        }
        String sourceBizType = normalizeSourceType(row.getSourceType());
        QmsNcRelationDO relation = qmsNcRelationMapper.selectPrimaryByRelatedObject(sourceBizType,
                row.getInspectionId());
        if (relation != null && relation.getNcRecordId() != null) {
            QmsNcRecordDO existing = qmsNcRecordMapper.selectLightById(relation.getNcRecordId());
            if (existing != null && !"CANCELLED".equals(existing.getStatus())) {
                return existing;
            }
        }
        return qmsNcRecordMapper.selectNonCancelledLightBySourceBiz(sourceBizType, row.getInspectionId());
    }

    private QmsProductEventRecheckGroupDO createRecheckGroup(String sourceType, QmsProductAbnormalEventRespVO row) {
        return createRecheckGroup(sourceType, row, 0);
    }

    private QmsProductEventRecheckGroupDO createRecheckGroup(String sourceType,
                                                              QmsProductAbnormalEventRespVO row,
                                                              int rootRoundNo) {
        QmsProductEventRecheckGroupDO group = QmsProductEventRecheckGroupDO.builder()
                .sourceType(sourceType)
                .rootInspectionId(row.getInspectionId())
                .rootInspectionNo(row.getInspectionNo())
                .latestInspectionId(row.getInspectionId())
                .latestInspectionNo(row.getInspectionNo())
                .latestRoundNo(rootRoundNo)
                .latestStatus(row.getStatus())
                .latestJudgment(row.getJudgment())
                .chainStatus(RECHECK_STATUS_NG)
                .totalRecheckCount(0)
                .latestResultTime(row.getInspectionTime())
                .tenantId(getInspectionTenantId(sourceType, row.getInspectionId()))
                .build();
        qmsProductEventRecheckGroupMapper.insert(group);
        markNewInspectionRecheck(sourceType, row.getInspectionId(), group.getId(), rootRoundNo,
                null, null, row.getInspectionId(), row.getInspectionNo());
        return group;
    }

    private QmsProductEventRecheckDetailDO createRootRecheckDetail(QmsProductEventRecheckGroupDO group,
                                                                   QmsProductAbnormalEventRespVO row) {
        return createRootRecheckDetail(group, row, 0);
    }

    private QmsProductEventRecheckDetailDO createRootRecheckDetail(QmsProductEventRecheckGroupDO group,
                                                                   QmsProductAbnormalEventRespVO row,
                                                                   int rootRoundNo) {
        QmsProductEventRecheckDetailDO detail = QmsProductEventRecheckDetailDO.builder()
                .groupId(group.getId())
                .sourceType(group.getSourceType())
                .roundNo(rootRoundNo)
                .inspectionId(row.getInspectionId())
                .inspectionNo(row.getInspectionNo())
                .inspectionStatus(row.getStatus())
                .inspectionJudgment(row.getJudgment())
                .resultTime(row.getInspectionTime())
                .tenantId(group.getTenantId())
                .build();
        qmsProductEventRecheckDetailMapper.insert(detail);
        return detail;
    }

    private CopyInspectionResult copyInspectionForRecheck(String sourceType, Long inspectionId, Integer roundNo,
                                                          Long rootInspectionId, String rootInspectionNo) {
        return copyInspectionForRecheck(sourceType, inspectionId, roundNo, rootInspectionId, rootInspectionNo,
                RecheckSelection.all());
    }

    private CopyInspectionResult copyInspectionForRecheck(String sourceType, Long inspectionId, Integer roundNo,
                                                          Long rootInspectionId, String rootInspectionNo,
                                                          RecheckSelection selection) {
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            return copyFaiForRecheck(sourceType, inspectionId, roundNo, rootInspectionId, rootInspectionNo,
                    selection);
        }
        if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            return copyFqcForRecheck(sourceType, inspectionId, roundNo, rootInspectionId, rootInspectionNo,
                    selection);
        }
        if (SOURCE_TYPE_IQC.equals(sourceType)) {
            return copyIqcForRecheck(inspectionId, roundNo, rootInspectionId, rootInspectionNo, selection);
        }
        if (SOURCE_TYPE_OQC.equals(sourceType)) {
            return copyOqcForRecheck(inspectionId, roundNo, rootInspectionId, rootInspectionNo, selection);
        }
        throw invalidParamException("检验来源类型不支持");
    }

    private CopyInspectionResult copyFaiForRecheck(String sourceType, Long inspectionId, Integer roundNo,
                                                   Long rootInspectionId, String rootInspectionNo,
                                                   RecheckSelection selection) {
        QmsFaiOrderDO source = qmsFaiOrderMapper.selectById(inspectionId);
        if (source == null) {
            throw invalidParamException("首件检验单不存在");
        }
        QmsFaiOrderDO target = BeanUtils.toBean(source, QmsFaiOrderDO.class);
        resetCopiedBase(target);
        target.setFaiNo(qmsNoGeneratorService.generateNo(SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)
                ? SOURCE_TYPE_GLUE_BOARD_FAI : SOURCE_TYPE_FAI));
        resetFaiOrderForRecheck(target, roundNo, rootInspectionId, rootInspectionNo);
        boolean pendingStandardItemSelection = selection.itemIds() != null && selection.itemIds().isEmpty();
        if (pendingStandardItemSelection) {
            // FAI 复检的具体项目应在复检单选择标准后确定，不能沿用原检项目。
            target.setRequiredItemCount(0);
            target.setCompletedItemCount(0);
            target.setAbnormalItemCount(0);
            target.setEntryProgress(0);
        }
        qmsFaiOrderMapper.insert(target);
        coaFreezePackagingService.syncCoaFreezeForFai(target.getId());

        if (pendingStandardItemSelection) {
            return new CopyInspectionResult(target.getId(), target.getFaiNo(), target.getTenantId(), Map.of());
        }

        List<QmsFaiItemDO> sourceItems = qmsFaiItemMapper.selectListByFaiId(source.getId());
        Set<Long> selected = resolveSelectedItemIds(sourceItems.stream().map(QmsFaiItemDO::getId).toList(),
                selection.itemIds());
        Map<Long, Long> itemIdMap = new HashMap<>();
        for (QmsFaiItemDO sourceItem : sourceItems) {
            QmsFaiItemDO targetItem = BeanUtils.toBean(sourceItem, QmsFaiItemDO.class);
            resetCopiedBase(targetItem);
            targetItem.setFaiId(target.getId());
            targetItem.setFaiNo(target.getFaiNo());
            targetItem.setRecheckItemFlag(selected.contains(sourceItem.getId()));
            if (selected.contains(sourceItem.getId())) {
                resetFaiItemValue(targetItem);
            }
            qmsFaiItemMapper.insert(targetItem);
            itemIdMap.put(sourceItem.getId(), targetItem.getId());
        }

        Map<Long, Long> cellIdMap = new HashMap<>();
        for (QmsFaiSheetCellValueDO sourceCell : qmsFaiSheetCellValueMapper.selectListByFaiId(source.getId())) {
            QmsFaiSheetCellValueDO targetCell = BeanUtils.toBean(sourceCell, QmsFaiSheetCellValueDO.class);
            resetCopiedBase(targetCell);
            targetCell.setFaiId(target.getId());
            targetCell.setFaiItemId(itemIdMap.get(sourceCell.getFaiItemId()));
            if (selected.contains(sourceCell.getFaiItemId())) {
                resetFaiCellValue(targetCell);
            }
            qmsFaiSheetCellValueMapper.insert(targetCell);
            cellIdMap.put(sourceCell.getId(), targetCell.getId());
        }

        for (QmsFaiSampleDO sourceSample : qmsFaiSampleMapper.selectListByFaiId(source.getId())) {
            QmsFaiSampleDO targetSample = BeanUtils.toBean(sourceSample, QmsFaiSampleDO.class);
            resetCopiedBase(targetSample);
            targetSample.setFaiId(target.getId());
            targetSample.setFaiNo(target.getFaiNo());
            targetSample.setFaiItemId(itemIdMap.get(sourceSample.getFaiItemId()));
            targetSample.setSheetCellId(cellIdMap.get(sourceSample.getSheetCellId()));
            boolean resetSample = shouldResetFaiSample(sourceSample, selected, selection);
            targetSample.setRecheckItemFlag(resetSample);
            if (resetSample) {
                resetFaiSampleValue(targetSample);
            }
            qmsFaiSampleMapper.insert(targetSample);
        }
        return new CopyInspectionResult(target.getId(), target.getFaiNo(), target.getTenantId(), itemIdMap);
    }

    private CopyInspectionResult copyFqcForRecheck(String sourceType, Long inspectionId, Integer roundNo,
                                                   Long rootInspectionId, String rootInspectionNo,
                                                   RecheckSelection selection) {
        QmsFqcOrderDO source = qmsFqcOrderMapper.selectById(inspectionId);
        if (source == null) {
            throw invalidParamException("FQC检验单不存在");
        }
        QmsFqcOrderDO target = BeanUtils.toBean(source, QmsFqcOrderDO.class);
        resetCopiedBase(target);
        target.setFqcNo(qmsNoGeneratorService.generateNo("FQC"));
        resetFqcOrderForRecheck(target, roundNo, rootInspectionId, rootInspectionNo);
        qmsFqcOrderMapper.insert(target);

        List<QmsFqcItemDO> sourceItems = qmsFqcItemMapper.selectListByFqcId(source.getId());
        Set<Long> selected = resolveSelectedItemIds(sourceItems.stream().map(QmsFqcItemDO::getId).toList(),
                selection.itemIds());
        List<QmsFqcSampleDO> sourceSamples = qmsFqcSampleMapper.selectListByFqcId(source.getId());
        Set<Long> selectedDetailIds = sourceSamples.stream()
                .filter(sample -> shouldResetFqcSample(sample, selected, selection))
                .map(QmsFqcSampleDO::getSubmissionDetailId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Long> detailIdMap = new HashMap<>();
        if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType)) {
            for (QmsFqcSubmissionDetailDO sourceDetail : qmsFqcSubmissionDetailMapper.selectListByFqcId(source.getId())) {
                QmsFqcSubmissionDetailDO targetDetail = BeanUtils.toBean(sourceDetail, QmsFqcSubmissionDetailDO.class);
                resetCopiedBase(targetDetail);
                targetDetail.setFqcId(target.getId());
                targetDetail.setFqcNo(target.getFqcNo());
                targetDetail.setRecheckDetailFlag(selectedDetailIds.contains(sourceDetail.getId()));
                if (selectedDetailIds.contains(sourceDetail.getId())) {
                    resetFqcSubmissionDetailValue(targetDetail);
                }
                qmsFqcSubmissionDetailMapper.insert(targetDetail);
                detailIdMap.put(sourceDetail.getId(), targetDetail.getId());
            }
        } else {
            for (QmsFqcShippingDetailDO sourceDetail : qmsFqcShippingDetailMapper.selectListByFqcId(source.getId())) {
                QmsFqcShippingDetailDO targetDetail = BeanUtils.toBean(sourceDetail, QmsFqcShippingDetailDO.class);
                resetCopiedBase(targetDetail);
                targetDetail.setFqcId(target.getId());
                targetDetail.setFqcNo(target.getFqcNo());
                targetDetail.setRecheckDetailFlag(selectedDetailIds.contains(sourceDetail.getId()));
                if (selectedDetailIds.contains(sourceDetail.getId())) {
                    resetFqcShippingDetailValue(targetDetail);
                }
                qmsFqcShippingDetailMapper.insert(targetDetail);
                detailIdMap.put(sourceDetail.getId(), targetDetail.getId());
            }
        }

        Map<Long, Long> itemIdMap = new HashMap<>();
        for (QmsFqcItemDO sourceItem : sourceItems) {
            QmsFqcItemDO targetItem = BeanUtils.toBean(sourceItem, QmsFqcItemDO.class);
            resetCopiedBase(targetItem);
            targetItem.setFqcId(target.getId());
            targetItem.setFqcNo(target.getFqcNo());
            targetItem.setSubmissionDetailId(detailIdMap.get(sourceItem.getSubmissionDetailId()));
            targetItem.setRecheckItemFlag(selected.contains(sourceItem.getId()));
            if (selected.contains(sourceItem.getId())) {
                resetFqcItemValue(targetItem);
            }
            qmsFqcItemMapper.insert(targetItem);
            itemIdMap.put(sourceItem.getId(), targetItem.getId());
        }

        Map<Long, Long> cellIdMap = new HashMap<>();
        for (QmsFqcSheetCellValueDO sourceCell : qmsFqcSheetCellValueMapper.selectListByFqcId(source.getId())) {
            QmsFqcSheetCellValueDO targetCell = BeanUtils.toBean(sourceCell, QmsFqcSheetCellValueDO.class);
            resetCopiedBase(targetCell);
            targetCell.setFqcId(target.getId());
            targetCell.setFqcItemId(itemIdMap.get(sourceCell.getFqcItemId()));
            if (selected.contains(sourceCell.getFqcItemId())) {
                resetFqcCellValue(targetCell);
            }
            qmsFqcSheetCellValueMapper.insert(targetCell);
            cellIdMap.put(sourceCell.getId(), targetCell.getId());
        }

        for (QmsFqcSampleDO sourceSample : sourceSamples) {
            QmsFqcSampleDO targetSample = BeanUtils.toBean(sourceSample, QmsFqcSampleDO.class);
            resetCopiedBase(targetSample);
            targetSample.setFqcId(target.getId());
            targetSample.setFqcNo(target.getFqcNo());
            targetSample.setFqcItemId(itemIdMap.get(sourceSample.getFqcItemId()));
            targetSample.setSubmissionDetailId(detailIdMap.get(sourceSample.getSubmissionDetailId()));
            targetSample.setSheetCellId(cellIdMap.get(sourceSample.getSheetCellId()));
            boolean resetSample = shouldResetFqcSample(sourceSample, selected, selection);
            targetSample.setRecheckItemFlag(resetSample);
            if (resetSample) {
                resetFqcSampleValue(targetSample);
            }
            qmsFqcSampleMapper.insert(targetSample);
        }
        return new CopyInspectionResult(target.getId(), target.getFqcNo(), target.getTenantId(), itemIdMap);
    }

    private CopyInspectionResult copyIqcForRecheck(Long inspectionId, Integer roundNo,
                                                   Long rootInspectionId, String rootInspectionNo,
                                                   RecheckSelection selection) {
        QmsIqcOrderDO source = qmsIqcOrderMapper.selectById(inspectionId);
        if (source == null) {
            throw invalidParamException("IQC检验单不存在");
        }
        QmsIqcOrderDO target = BeanUtils.toBean(source, QmsIqcOrderDO.class);
        resetCopiedBase(target);
        target.setIqcNo(qmsNoGeneratorService.generateNo(SOURCE_TYPE_IQC));
        resetIqcOrderForRecheck(target, roundNo, rootInspectionId, rootInspectionNo);
        qmsIqcOrderMapper.insert(target);

        List<QmsIqcItemDO> sourceItems = qmsIqcItemMapper.selectListByIqcId(source.getId());
        Set<Long> selected = resolveSelectedItemIds(sourceItems.stream().map(QmsIqcItemDO::getId).toList(),
                selection.itemIds());
        Map<Long, Long> itemIdMap = new HashMap<>();
        for (QmsIqcItemDO sourceItem : sourceItems) {
            QmsIqcItemDO targetItem = BeanUtils.toBean(sourceItem, QmsIqcItemDO.class);
            resetCopiedBase(targetItem);
            targetItem.setIqcId(target.getId());
            targetItem.setIqcNo(target.getIqcNo());
            targetItem.setRecheckItemFlag(selected.contains(sourceItem.getId()));
            if (selected.contains(sourceItem.getId())) {
                resetIqcItemValue(targetItem);
            }
            qmsIqcItemMapper.insert(targetItem);
            itemIdMap.put(sourceItem.getId(), targetItem.getId());
        }

        for (QmsIqcSampleDO sourceSample : qmsIqcSampleMapper.selectListByIqcId(source.getId())) {
            QmsIqcSampleDO targetSample = BeanUtils.toBean(sourceSample, QmsIqcSampleDO.class);
            resetCopiedBase(targetSample);
            targetSample.setIqcId(target.getId());
            targetSample.setIqcNo(target.getIqcNo());
            targetSample.setIqcItemId(itemIdMap.get(sourceSample.getIqcItemId()));
            boolean resetSample = selected.contains(sourceSample.getIqcItemId());
            targetSample.setRecheckItemFlag(resetSample);
            if (resetSample) {
                resetIqcSampleValue(targetSample);
            }
            qmsIqcSampleMapper.insert(targetSample);
        }
        return new CopyInspectionResult(target.getId(), target.getIqcNo(), target.getTenantId(), itemIdMap);
    }

    private CopyInspectionResult copyOqcForRecheck(Long inspectionId, Integer roundNo,
                                                   Long rootInspectionId, String rootInspectionNo,
                                                   RecheckSelection selection) {
        QmsOqcOrderDO source = qmsOqcOrderMapper.selectById(inspectionId);
        if (source == null) {
            throw invalidParamException("OQC检验单不存在");
        }
        QmsOqcOrderDO target = BeanUtils.toBean(source, QmsOqcOrderDO.class);
        resetCopiedBase(target);
        target.setOqcNo(qmsNoGeneratorService.generateNo("OQC"));
        resetOqcOrderForRecheck(target, roundNo, rootInspectionId, rootInspectionNo);
        qmsOqcOrderMapper.insert(target);

        List<QmsOqcItemDO> sourceItems = qmsOqcItemMapper.selectListByOqcId(source.getId());
        Set<Long> selected = resolveSelectedItemIds(sourceItems.stream().map(QmsOqcItemDO::getId).toList(),
                selection.itemIds());
        Map<Long, Long> itemIdMap = new HashMap<>();
        for (QmsOqcItemDO sourceItem : sourceItems) {
            QmsOqcItemDO targetItem = BeanUtils.toBean(sourceItem, QmsOqcItemDO.class);
            resetCopiedBase(targetItem);
            targetItem.setOqcId(target.getId());
            targetItem.setOqcNo(target.getOqcNo());
            targetItem.setRecheckItemFlag(selected.contains(sourceItem.getId()));
            if (selected.contains(sourceItem.getId())) {
                resetOqcItemValue(targetItem);
            }
            qmsOqcItemMapper.insert(targetItem);
            itemIdMap.put(sourceItem.getId(), targetItem.getId());
        }

        for (QmsOqcSampleDO sourceSample : qmsOqcSampleMapper.selectListByOqcId(source.getId())) {
            QmsOqcSampleDO targetSample = BeanUtils.toBean(sourceSample, QmsOqcSampleDO.class);
            resetCopiedBase(targetSample);
            targetSample.setOqcId(target.getId());
            targetSample.setOqcNo(target.getOqcNo());
            targetSample.setOqcItemId(itemIdMap.get(sourceSample.getOqcItemId()));
            boolean resetSample = shouldResetOqcSample(sourceSample, selected, selection);
            targetSample.setRecheckItemFlag(resetSample);
            if (resetSample) {
                resetOqcSampleValue(targetSample);
            }
            qmsOqcSampleMapper.insert(targetSample);
        }
        return new CopyInspectionResult(target.getId(), target.getOqcNo(), target.getTenantId(), itemIdMap);
    }

    private Set<Long> resolveSelectedItemIds(List<Long> availableItemIds, Set<Long> requestedItemIds) {
        Set<Long> available = new HashSet<>(availableItemIds);
        if (requestedItemIds == null) {
            return available;
        }
        if (requestedItemIds.isEmpty() || !available.containsAll(requestedItemIds)) {
            throw invalidParamException("选择的复检项目无效或不属于当前检验单");
        }
        return new HashSet<>(requestedItemIds);
    }

    private RecheckSelection buildRecheckSelection(Collection<Long> selectedItemIds,
                                                   Collection<QmsDispatchTaskItemSelectionReqVO> scopes) {
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            throw invalidParamException("至少选择一个复检项目或片号");
        }
        Set<Long> items = selectedItemIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (items.isEmpty() || items.size() != selectedItemIds.size()) {
            throw invalidParamException("选择的复检项目无效或不属于当前检验单");
        }
        Map<Long, Set<String>> positionsByItemId = new HashMap<>();
        Map<Long, Set<String>> piecesByItemId = new HashMap<>();
        if (scopes != null) {
            for (QmsDispatchTaskItemSelectionReqVO scope : scopes) {
                if (scope == null || scope.getItemId() == null || !items.contains(scope.getItemId())) {
                    throw invalidParamException("选择的复检范围无效");
                }
                String scopeType = StrUtil.blankToDefault(scope.getScopeType(), "ITEM").trim().toUpperCase();
                if ("ITEM".equals(scopeType)) {
                    positionsByItemId.remove(scope.getItemId());
                    piecesByItemId.remove(scope.getItemId());
                    continue;
                }
                Set<String> values = normalizeScopeValues(scope.getPositions());
                if ("POSITION".equals(scopeType)) {
                    positionsByItemId.computeIfAbsent(scope.getItemId(), ignored -> new HashSet<>()).addAll(values);
                } else if ("PIECE".equals(scopeType)) {
                    piecesByItemId.computeIfAbsent(scope.getItemId(), ignored -> new HashSet<>()).addAll(values);
                } else {
                    throw invalidParamException("选择的复检范围类型不支持");
                }
            }
        }
        return new RecheckSelection(items, positionsByItemId, piecesByItemId);
    }

    private Set<String> normalizeScopeValues(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            throw invalidParamException("选择的复检范围不能为空");
        }
        Set<String> result = values.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            throw invalidParamException("选择的复检范围不能为空");
        }
        return result;
    }

    private boolean shouldResetFaiSample(QmsFaiSampleDO sample, Set<Long> selected, RecheckSelection selection) {
        if (!selected.contains(sample.getFaiItemId())) {
            return false;
        }
        Set<String> positions = selection.positionsByItemId().get(sample.getFaiItemId());
        return positions == null || positions.isEmpty() || positions.contains(faiPositionKey(sample));
    }

    private boolean shouldResetFqcSample(QmsFqcSampleDO sample, Set<Long> selected, RecheckSelection selection) {
        if (!selected.contains(sample.getFqcItemId())) {
            return false;
        }
        Set<String> pieces = selection.piecesByItemId().get(sample.getFqcItemId());
        return pieces == null || pieces.isEmpty() || pieces.contains(fqcPieceKey(sample));
    }

    private boolean shouldResetOqcSample(QmsOqcSampleDO sample, Set<Long> selected, RecheckSelection selection) {
        if (!selected.contains(sample.getOqcItemId())) {
            return false;
        }
        Set<String> pieces = selection.piecesByItemId().get(sample.getOqcItemId());
        return pieces == null || pieces.isEmpty() || pieces.contains(oqcPieceKey(sample));
    }

    private String faiPositionKey(QmsFaiSampleDO sample) {
        return StrUtil.blankToDefault(StrUtil.trim(sample.getSamplePosition()),
                "位置" + Objects.requireNonNullElse(sample.getSampleSeq(), 1));
    }

    private String fqcPieceKey(QmsFqcSampleDO sample) {
        return firstNotBlank(sample.getProductionBatchNo(), sample.getParentProductionBatchNo(),
                sample.getSliceSeqNo() == null ? null : "第" + sample.getSliceSeqNo() + "片",
                sample.getSampleSeq() == null ? null : "样本" + sample.getSampleSeq(), "未指定片号");
    }

    private String oqcPieceKey(QmsOqcSampleDO sample) {
        return firstNotBlank(sample.getSamplePosition(),
                sample.getSampleSeq() == null ? null : "样本" + sample.getSampleSeq(), "未指定片号");
    }

    private boolean matchesNcrStatus(QmsProductAbnormalEventRespVO row, QmsProductAbnormalEventPageReqVO reqVO) {
        String requested = StrUtil.blankToDefault(reqVO.getNcrStatus(), NCR_STATUS_ALL).trim().toUpperCase();
        if (StrUtil.isBlank(requested) || NCR_STATUS_ALL.equals(requested)) {
            return true;
        }
        return requested.equals(row.getNcrStatus());
    }

    private boolean matchesProductAbnormalScope(QmsProductAbnormalEventRespVO row) {
        if (row == null) {
            return false;
        }
        if (!JUDGMENT_PENDING.equalsIgnoreCase(StrUtil.blankToDefault(row.getJudgment(), ""))) {
            return true;
        }
        return RECHECK_STATUS_RECHECKING.equalsIgnoreCase(StrUtil.blankToDefault(row.getRecheckStatus(), ""));
    }

    private boolean matchesRecheckStatus(QmsProductAbnormalEventRespVO row, QmsProductAbnormalEventPageReqVO reqVO) {
        String requested = StrUtil.blankToDefault(reqVO.getRecheckStatus(), RECHECK_STATUS_ALL).trim().toUpperCase();
        if (StrUtil.isBlank(requested) || RECHECK_STATUS_ALL.equals(requested)) {
            return true;
        }
        return requested.equals(row.getRecheckStatus());
    }

    private void resetCopiedBase(BaseDO target) {
        target.setDeleted(false);
        target.clean();
        if (target instanceof QmsFaiOrderDO item) {
            item.setId(null);
        } else if (target instanceof QmsFaiItemDO item) {
            item.setId(null);
        } else if (target instanceof QmsFaiSampleDO item) {
            item.setId(null);
        } else if (target instanceof QmsFaiSheetCellValueDO item) {
            item.setId(null);
        } else if (target instanceof QmsFqcOrderDO item) {
            item.setId(null);
        } else if (target instanceof QmsFqcItemDO item) {
            item.setId(null);
        } else if (target instanceof QmsFqcSampleDO item) {
            item.setId(null);
        } else if (target instanceof QmsFqcSheetCellValueDO item) {
            item.setId(null);
        } else if (target instanceof QmsFqcSubmissionDetailDO item) {
            item.setId(null);
        } else if (target instanceof QmsFqcShippingDetailDO item) {
            item.setId(null);
        } else if (target instanceof QmsIqcOrderDO item) {
            item.setId(null);
        } else if (target instanceof QmsIqcItemDO item) {
            item.setId(null);
        } else if (target instanceof QmsIqcSampleDO item) {
            item.setId(null);
        } else if (target instanceof QmsOqcOrderDO item) {
            item.setId(null);
        } else if (target instanceof QmsOqcItemDO item) {
            item.setId(null);
        } else if (target instanceof QmsOqcSampleDO item) {
            item.setId(null);
        }
    }

    private void resetFaiOrderForRecheck(QmsFaiOrderDO target, Integer roundNo,
                                         Long rootInspectionId, String rootInspectionNo) {
        target.setStatus(STATUS_PENDING);
        target.setJudgment(JUDGMENT_PENDING);
        target.setOperatorId(null);
        target.setOperatorName(null);
        target.setOperatorTime(null);
        target.setQaInspectorId(null);
        target.setQaInspectorName(null);
        target.setQaTime(null);
        target.setInspectionTime(null);
        target.setReleaseResult(null);
        target.setReleaseTime(null);
        target.setRetentionStatus(null);
        target.setRetentionConfirmTime(null);
        target.setRetentionConfirmUserId(null);
        target.setRetentionConfirmUserName(null);
        target.setAuditNotifyTime(null);
        target.setEntryProgress(0);
        target.setCompletedItemCount(0);
        target.setAbnormalItemCount(0);
        target.setLastSaveTime(null);
        target.setLastCalculateTime(null);
        target.setLastImportBatchNo(null);
        target.setSheetLocked(false);
        target.setReturnCount(0);
        target.setLastReturnReason(null);
        target.setRejectFlag(false);
        target.setRecheckFlag(true);
        target.setRecheckRoundNo(roundNo);
        target.setRejectRootInspectionId(rootInspectionId);
        target.setRejectRootInspectionNo(rootInspectionNo);
        target.setRejectRecheckResult(JUDGMENT_PENDING);
        target.setRejectRecheckTime(null);
        target.setRejectReason(null);
        target.setRejectTime(null);
        target.setRejectUserId(null);
        target.setRejectUserName(null);
        target.setRejectPrevInspectionId(null);
        target.setRejectPrevInspectionNo(null);
        target.setRejectNextInspectionId(null);
        target.setRejectNextInspectionNo(null);
    }

    private void resetFqcOrderForRecheck(QmsFqcOrderDO target, Integer roundNo,
                                         Long rootInspectionId, String rootInspectionNo) {
        target.setStatus(STATUS_PENDING);
        target.setJudgment(JUDGMENT_PENDING);
        target.setInspectorId(null);
        target.setInspectorName(null);
        target.setInspectionTime(null);
        target.setQaInspectorId(null);
        target.setQaInspectorName(null);
        target.setQaTime(null);
        target.setReleaseResult(null);
        target.setReleaseTime(null);
        target.setAuditNotifyTime(null);
        target.setRelatedNcrNo(null);
        target.setNcrStatus(null);
        target.setEntryProgress(0);
        target.setCompletedItemCount(0);
        target.setAbnormalItemCount(0);
        target.setOkQty(0);
        target.setNgQty(0);
        target.setLastSaveTime(null);
        target.setLastCalculateTime(null);
        target.setLastImportBatchNo(null);
        target.setSheetLocked(false);
        target.setReturnCount(0);
        target.setLastReturnReason(null);
        target.setRejectFlag(false);
        target.setRecheckFlag(true);
        target.setRecheckRoundNo(roundNo);
        target.setRejectRootInspectionId(rootInspectionId);
        target.setRejectRootInspectionNo(rootInspectionNo);
        target.setRejectRecheckResult(JUDGMENT_PENDING);
        target.setRejectRecheckTime(null);
        target.setRejectReason(null);
        target.setRejectTime(null);
        target.setRejectUserId(null);
        target.setRejectUserName(null);
        target.setRejectPrevInspectionId(null);
        target.setRejectPrevInspectionNo(null);
        target.setRejectNextInspectionId(null);
        target.setRejectNextInspectionNo(null);
    }

    private void resetIqcOrderForRecheck(QmsIqcOrderDO target, Integer roundNo,
                                         Long rootInspectionId, String rootInspectionNo) {
        target.setStatus(STATUS_PENDING);
        target.setJudgment(JUDGMENT_PENDING);
        target.setInspectorId(null);
        target.setInspectorName(null);
        target.setInspectionTime(null);
        target.setQaInspectorId(null);
        target.setQaInspectorName(null);
        target.setQaTime(null);
        target.setAuditNotifyTime(null);
        target.setDisposalType(null);
        target.setReturnCount(0);
        target.setLastReturnReason(null);
        target.setRejectFlag(false);
        target.setRecheckFlag(true);
        target.setRecheckRoundNo(roundNo);
        target.setRejectRootInspectionId(rootInspectionId);
        target.setRejectRootInspectionNo(rootInspectionNo);
        target.setRejectRecheckResult(JUDGMENT_PENDING);
        target.setRejectRecheckTime(null);
        target.setRejectReason(null);
        target.setRejectTime(null);
        target.setRejectUserId(null);
        target.setRejectUserName(null);
        target.setRejectPrevInspectionId(null);
        target.setRejectPrevInspectionNo(null);
        target.setRejectNextInspectionId(null);
        target.setRejectNextInspectionNo(null);
    }

    private void resetOqcOrderForRecheck(QmsOqcOrderDO target, Integer roundNo,
                                         Long rootInspectionId, String rootInspectionNo) {
        target.setStatus(STATUS_PENDING);
        target.setJudgment(JUDGMENT_PENDING);
        target.setInspectorId(null);
        target.setInspectorName(null);
        target.setInspectionTime(null);
        target.setQaInspectorId(null);
        target.setQaInspectorName(null);
        target.setQaTime(null);
        target.setReleaseResult(null);
        target.setReleaseTime(null);
        target.setAuditNotifyTime(null);
        target.setRelatedNcrNo(null);
        target.setNcrStatus(null);
        target.setEntryProgress(0);
        target.setCompletedItemCount(0);
        target.setAbnormalItemCount(0);
        target.setLastSaveTime(null);
        target.setLastCalculateTime(null);
        target.setSheetLocked(false);
        target.setRejectFlag(false);
        target.setRecheckFlag(true);
        target.setRecheckRoundNo(roundNo);
        target.setRejectRootInspectionId(rootInspectionId);
        target.setRejectRootInspectionNo(rootInspectionNo);
        target.setRejectRecheckResult(JUDGMENT_PENDING);
        target.setRejectRecheckTime(null);
        target.setRejectReason(null);
        target.setRejectTime(null);
        target.setRejectUserId(null);
        target.setRejectUserName(null);
        target.setRejectPrevInspectionId(null);
        target.setRejectPrevInspectionNo(null);
        target.setRejectNextInspectionId(null);
        target.setRejectNextInspectionNo(null);
    }

    private void resetFaiItemValue(QmsFaiItemDO item) {
        item.setOperatorMax(null);
        item.setOperatorMin(null);
        item.setOperatorAvg(null);
        item.setOperatorResult(JUDGMENT_PENDING);
        item.setOperatorId(null);
        item.setOperatorName(null);
        item.setOperatorTime(null);
        item.setQaMax(null);
        item.setQaMin(null);
        item.setQaAvg(null);
        item.setQaResult(JUDGMENT_PENDING);
        item.setQaInspectorId(null);
        item.setQaInspectorName(null);
        item.setQaTime(null);
        item.setCalculatedAvg(null);
        item.setCalculatedStd(null);
        item.setCalculatedMin(null);
        item.setCalculatedMax(null);
        item.setCellCompletedCount(0);
        item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0);
        item.setInputStatus(JUDGMENT_PENDING);
        item.setAttachmentUrls(null);
    }

    private void resetFqcItemValue(QmsFqcItemDO item) {
        item.setMaxValue(null);
        item.setMinValue(null);
        item.setAverageValue(null);
        item.setItemResult(JUDGMENT_PENDING);
        item.setOperatorMax(null);
        item.setOperatorMin(null);
        item.setOperatorAvg(null);
        item.setOperatorResult(JUDGMENT_PENDING);
        item.setOperatorId(null);
        item.setOperatorName(null);
        item.setOperatorTime(null);
        item.setQaMax(null);
        item.setQaMin(null);
        item.setQaAvg(null);
        item.setQaResult(JUDGMENT_PENDING);
        item.setQaInspectorId(null);
        item.setQaInspectorName(null);
        item.setQaTime(null);
        item.setCalculatedAvg(null);
        item.setCalculatedStd(null);
        item.setCalculatedMin(null);
        item.setCalculatedMax(null);
        item.setCellCompletedCount(0);
        item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0);
        item.setInputStatus(JUDGMENT_PENDING);
    }

    private void resetIqcItemValue(QmsIqcItemDO item) {
        item.setMaxValue(null);
        item.setMinValue(null);
        item.setAverageValue(null);
        item.setItemResult(JUDGMENT_PENDING);
        item.setAttachmentUrls(null);
    }

    private void resetOqcItemValue(QmsOqcItemDO item) {
        item.setMaxValue(null);
        item.setMinValue(null);
        item.setAverageValue(null);
        item.setItemResult(JUDGMENT_PENDING);
        item.setOperatorMax(null);
        item.setOperatorMin(null);
        item.setOperatorAvg(null);
        item.setOperatorResult(JUDGMENT_PENDING);
        item.setOperatorId(null);
        item.setOperatorName(null);
        item.setOperatorTime(null);
        item.setQaMax(null);
        item.setQaMin(null);
        item.setQaAvg(null);
        item.setQaResult(JUDGMENT_PENDING);
        item.setQaInspectorId(null);
        item.setQaInspectorName(null);
        item.setQaTime(null);
        item.setCalculatedAvg(null);
        item.setCalculatedStd(null);
        item.setCalculatedMin(null);
        item.setCalculatedMax(null);
        item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0);
        item.setInputStatus(JUDGMENT_PENDING);
    }

    private void resetFaiCellValue(QmsFaiSheetCellValueDO cell) {
        cell.setRawValue(null);
        cell.setNumericValue(null);
        cell.setTextValue(null);
        cell.setValueSource(null);
        cell.setCellStatus(JUDGMENT_PENDING);
        cell.setJudgmentResult(JUDGMENT_PENDING);
        cell.setImportBatchNo(null);
        cell.setInputUserId(null);
        cell.setInputUserName(null);
        cell.setInputTime(null);
    }

    private void resetFqcCellValue(QmsFqcSheetCellValueDO cell) {
        cell.setRawValue(null);
        cell.setNumericValue(null);
        cell.setTextValue(null);
        cell.setValueSource(null);
        cell.setCellStatus(JUDGMENT_PENDING);
        cell.setJudgmentResult(JUDGMENT_PENDING);
        cell.setImportBatchNo(null);
        cell.setInputUserId(null);
        cell.setInputUserName(null);
        cell.setInputTime(null);
    }

    private void resetFaiSampleValue(QmsFaiSampleDO sample) {
        sample.setRawValuesJson(null);
        sample.setResultValue(null);
        sample.setDensityValue(null);
        sample.setCompressionRate(null);
        sample.setCompressionElasticityRate(null);
        sample.setMeasuredValue(null);
        sample.setQualitativeValue(null);
        sample.setSampleResult(JUDGMENT_PENDING);
        sample.setImportBatchNo(null);
        sample.setValueSource(null);
        sample.setInputTime(null);
        sample.setDefectCode(null);
        sample.setDefectName(null);
        sample.setRemark(null);
    }

    private void resetFqcSampleValue(QmsFqcSampleDO sample) {
        sample.setRawValuesJson(null);
        sample.setResultValue(null);
        sample.setDensityValue(null);
        sample.setCompressionRate(null);
        sample.setCompressionElasticityRate(null);
        sample.setMeasuredValue(null);
        sample.setQualitativeValue(null);
        sample.setSampleResult(JUDGMENT_PENDING);
        sample.setImportBatchNo(null);
        sample.setValueSource(null);
        sample.setInputTime(null);
        sample.setDefectCode(null);
        sample.setDefectName(null);
        sample.setRemark(null);
    }

    private void resetIqcSampleValue(QmsIqcSampleDO sample) {
        sample.setRawValuesJson(null);
        sample.setResultValue(null);
        sample.setMeasuredValue(null);
        sample.setQualitativeValue(null);
        sample.setDateValue(null);
        sample.setEvaluationDate(null);
        sample.setSampleResult(JUDGMENT_PENDING);
        sample.setDefectCode(null);
        sample.setDefectName(null);
        sample.setRemark(null);
    }

    private void resetOqcSampleValue(QmsOqcSampleDO sample) {
        sample.setRawValuesJson(null);
        sample.setResultValue(null);
        sample.setMeasuredValue(null);
        sample.setQualitativeValue(null);
        sample.setSampleResult(JUDGMENT_PENDING);
        sample.setValueSource(null);
        sample.setInputTime(null);
        sample.setRemark(null);
    }

    private void resetFqcSubmissionDetailValue(QmsFqcSubmissionDetailDO detail) {
        detail.setRowJudgment(JUDGMENT_PENDING);
        detail.setDefectCode(null);
        detail.setDefectName(null);
        detail.setNgReason(null);
        detail.setInspectorId(null);
        detail.setInspectorName(null);
        detail.setInspectionTime(null);
        detail.setRemark(null);
    }

    private void resetFqcShippingDetailValue(QmsFqcShippingDetailDO detail) {
        detail.setRowJudgment(JUDGMENT_PENDING);
        detail.setDefectCode(null);
        detail.setDefectName(null);
        detail.setNgReason(null);
        detail.setMismatchReason(null);
        detail.setInspectorId(null);
        detail.setInspectorName(null);
        detail.setInspectionTime(null);
        detail.setRemark(null);
    }

    private void markCurrentInspectionRejected(String sourceType, Long inspectionId, Long nextInspectionId,
                                               String nextInspectionNo, String rejectReason, Long rejectUserId,
                                               String rejectUserName, LocalDateTime rejectTime) {
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO update = new QmsFaiOrderDO();
            update.setId(inspectionId);
            update.setStatus(STATUS_REJECTED);
            update.setRejectFlag(true);
            update.setRejectNextInspectionId(nextInspectionId);
            update.setRejectNextInspectionNo(nextInspectionNo);
            update.setRejectReason(rejectReason);
            update.setRejectTime(rejectTime);
            update.setRejectUserId(rejectUserId);
            update.setRejectUserName(rejectUserName);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsFaiOrderMapper.updateById(update);
        coaFreezePackagingService.syncCoaFreezeForFai(update.getId());
        } else if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO update = new QmsFqcOrderDO();
            update.setId(inspectionId);
            update.setStatus(STATUS_REJECTED);
            update.setRejectFlag(true);
            update.setRejectNextInspectionId(nextInspectionId);
            update.setRejectNextInspectionNo(nextInspectionNo);
            update.setRejectReason(rejectReason);
            update.setRejectTime(rejectTime);
            update.setRejectUserId(rejectUserId);
            update.setRejectUserName(rejectUserName);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsFqcOrderMapper.updateById(update);
        } else if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO update = new QmsIqcOrderDO();
            update.setId(inspectionId);
            update.setStatus(STATUS_REJECTED);
            update.setRejectFlag(true);
            update.setRejectNextInspectionId(nextInspectionId);
            update.setRejectNextInspectionNo(nextInspectionNo);
            update.setRejectReason(rejectReason);
            update.setRejectTime(rejectTime);
            update.setRejectUserId(rejectUserId);
            update.setRejectUserName(rejectUserName);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsIqcOrderMapper.updateById(update);
        } else if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO update = new QmsOqcOrderDO();
            update.setId(inspectionId);
            update.setStatus(STATUS_REJECTED);
            update.setRejectFlag(true);
            update.setRejectNextInspectionId(nextInspectionId);
            update.setRejectNextInspectionNo(nextInspectionNo);
            update.setRejectReason(rejectReason);
            update.setRejectTime(rejectTime);
            update.setRejectUserId(rejectUserId);
            update.setRejectUserName(rejectUserName);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsOqcOrderMapper.updateById(update);
        }
    }

    private void markNewInspectionRecheck(String sourceType, Long inspectionId, Long groupId, Integer roundNo,
                                          Long prevInspectionId, String prevInspectionNo,
                                          Long rootInspectionId, String rootInspectionNo) {
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO update = new QmsFaiOrderDO();
            update.setId(inspectionId);
            update.setRecheckGroupId(groupId);
            update.setRecheckRoundNo(roundNo);
            update.setRejectPrevInspectionId(prevInspectionId);
            update.setRejectPrevInspectionNo(prevInspectionNo);
            update.setRejectRootInspectionId(rootInspectionId);
            update.setRejectRootInspectionNo(rootInspectionNo);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsFaiOrderMapper.updateById(update);
        coaFreezePackagingService.syncCoaFreezeForFai(update.getId());
        } else if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO update = new QmsFqcOrderDO();
            update.setId(inspectionId);
            update.setRecheckGroupId(groupId);
            update.setRecheckRoundNo(roundNo);
            update.setRejectPrevInspectionId(prevInspectionId);
            update.setRejectPrevInspectionNo(prevInspectionNo);
            update.setRejectRootInspectionId(rootInspectionId);
            update.setRejectRootInspectionNo(rootInspectionNo);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsFqcOrderMapper.updateById(update);
        } else if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO update = new QmsIqcOrderDO();
            update.setId(inspectionId);
            update.setRecheckGroupId(groupId);
            update.setRecheckRoundNo(roundNo);
            update.setRejectPrevInspectionId(prevInspectionId);
            update.setRejectPrevInspectionNo(prevInspectionNo);
            update.setRejectRootInspectionId(rootInspectionId);
            update.setRejectRootInspectionNo(rootInspectionNo);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsIqcOrderMapper.updateById(update);
        } else if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO update = new QmsOqcOrderDO();
            update.setId(inspectionId);
            update.setRecheckGroupId(groupId);
            update.setRecheckRoundNo(roundNo);
            update.setRejectPrevInspectionId(prevInspectionId);
            update.setRejectPrevInspectionNo(prevInspectionNo);
            update.setRejectRootInspectionId(rootInspectionId);
            update.setRejectRootInspectionNo(rootInspectionNo);
            update.setRejectRecheckResult(JUDGMENT_PENDING);
            qmsOqcOrderMapper.updateById(update);
        }
    }

    private void updateRootRecheckResult(String sourceType, Long rootInspectionId, String result,
                                         LocalDateTime resultTime) {
        if (rootInspectionId == null) {
            return;
        }
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO update = new QmsFaiOrderDO();
            update.setId(rootInspectionId);
            update.setRejectRecheckResult(StrUtil.blankToDefault(result, JUDGMENT_PENDING));
            update.setRejectRecheckTime(resultTime);
            qmsFaiOrderMapper.updateById(update);
        coaFreezePackagingService.syncCoaFreezeForFai(update.getId());
        } else if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO update = new QmsFqcOrderDO();
            update.setId(rootInspectionId);
            update.setRejectRecheckResult(StrUtil.blankToDefault(result, JUDGMENT_PENDING));
            update.setRejectRecheckTime(resultTime);
            qmsFqcOrderMapper.updateById(update);
        } else if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO update = new QmsIqcOrderDO();
            update.setId(rootInspectionId);
            update.setRejectRecheckResult(StrUtil.blankToDefault(result, JUDGMENT_PENDING));
            update.setRejectRecheckTime(resultTime);
            qmsIqcOrderMapper.updateById(update);
        } else if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO update = new QmsOqcOrderDO();
            update.setId(rootInspectionId);
            update.setRejectRecheckResult(StrUtil.blankToDefault(result, JUDGMENT_PENDING));
            update.setRejectRecheckTime(resultTime);
            qmsOqcOrderMapper.updateById(update);
        }
    }

    private boolean hasRejectNextInspection(String sourceType, Long inspectionId) {
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(inspectionId);
            return order != null && order.getRejectNextInspectionId() != null;
        }
        if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(inspectionId);
            return order != null && order.getRejectNextInspectionId() != null;
        }
        if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO order = qmsIqcOrderMapper.selectById(inspectionId);
            return order != null && order.getRejectNextInspectionId() != null;
        }
        if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO order = qmsOqcOrderMapper.selectById(inspectionId);
            return order != null && order.getRejectNextInspectionId() != null;
        }
        return false;
    }

    private boolean isAbnormalInspection(String sourceType, Long inspectionId) {
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(inspectionId);
            return order != null && (JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(order.getJudgment(), ""))
                    || STATUS_REJECTED.equalsIgnoreCase(StrUtil.blankToDefault(order.getStatus(), ""))
                    || (order.getAbnormalItemCount() != null && order.getAbnormalItemCount() > 0));
        }
        if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(inspectionId);
            return order != null && (JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(order.getJudgment(), ""))
                    || STATUS_REJECTED.equalsIgnoreCase(StrUtil.blankToDefault(order.getStatus(), ""))
                    || (order.getAbnormalItemCount() != null && order.getAbnormalItemCount() > 0)
                    || (order.getNgQty() != null && order.getNgQty() > 0));
        }
        if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO order = qmsIqcOrderMapper.selectById(inspectionId);
            return order != null && (JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(order.getJudgment(), ""))
                    || STATUS_REJECTED.equalsIgnoreCase(StrUtil.blankToDefault(order.getStatus(), "")));
        }
        if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO order = qmsOqcOrderMapper.selectById(inspectionId);
            return order != null && (JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(order.getJudgment(), ""))
                    || STATUS_REJECTED.equalsIgnoreCase(StrUtil.blankToDefault(order.getStatus(), ""))
                    || (order.getAbnormalItemCount() != null && order.getAbnormalItemCount() > 0));
        }
        return false;
    }

    private Long getInspectionTenantId(String sourceType, Long inspectionId) {
        if (SOURCE_TYPE_FAI.equals(sourceType) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)) {
            QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(inspectionId);
            return order == null ? null : order.getTenantId();
        }
        if (SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType) || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)) {
            QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(inspectionId);
            return order == null ? null : order.getTenantId();
        }
        if (SOURCE_TYPE_IQC.equals(sourceType)) {
            QmsIqcOrderDO order = qmsIqcOrderMapper.selectById(inspectionId);
            return order == null ? null : order.getTenantId();
        }
        if (SOURCE_TYPE_OQC.equals(sourceType)) {
            QmsOqcOrderDO order = qmsOqcOrderMapper.selectById(inspectionId);
            return order == null ? null : order.getTenantId();
        }
        return null;
    }

    private boolean isSupportedSourceType(String sourceType) {
        return SOURCE_TYPE_FAI.equals(sourceType)
                || SOURCE_TYPE_GLUE_BOARD_FAI.equals(sourceType)
                || SOURCE_TYPE_CUT_ROUND_FQC.equals(sourceType)
                || SOURCE_TYPE_FG_SHIPPING_FQC.equals(sourceType)
                || SOURCE_TYPE_IQC.equals(sourceType)
                || SOURCE_TYPE_OQC.equals(sourceType);
    }

    private String recheckStatusName(String status) {
        return switch (StrUtil.blankToDefault(status, RECHECK_STATUS_NONE)) {
            case RECHECK_STATUS_RECHECKING -> "复检中";
            case RECHECK_STATUS_OK -> "复检OK";
            case RECHECK_STATUS_NG -> "复检NG";
            default -> "未驳回";
        };
    }

    private void fillAbnormalSummary(List<QmsProductAbnormalEventRespVO> rows) {
        for (QmsProductAbnormalEventRespVO row : rows) {
            if (StrUtil.isNotBlank(row.getAbnormalSummary())
                    && RECHECK_STATUS_RECHECKING.equals(row.getRecheckStatus())) {
                continue;
            }
            SummaryResult summary = null;
            if (SOURCE_TYPE_FAI.equals(row.getSourceType()) || SOURCE_TYPE_GLUE_BOARD_FAI.equals(row.getSourceType())) {
                summary = buildFaiSummary(row.getInspectionId(), row.getJudgment());
            } else if (SOURCE_TYPE_CUT_ROUND_FQC.equals(row.getSourceType())) {
                summary = buildCutRoundFqcSummary(row.getInspectionId(), row.getJudgment());
            } else if (SOURCE_TYPE_FG_SHIPPING_FQC.equals(row.getSourceType())) {
                summary = buildFgShippingFqcSummary(row.getInspectionId(), row.getJudgment());
            } else if (SOURCE_TYPE_IQC.equals(row.getSourceType())) {
                summary = buildIqcSummary(row.getInspectionId(), row.getJudgment());
            } else if (SOURCE_TYPE_OQC.equals(row.getSourceType())) {
                summary = buildOqcSummary(row.getInspectionId(), row.getJudgment());
            }
            if (summary != null) {
                row.setAbnormalSummary(summary.summary());
                if (summary.unqualifiedQty() != null) {
                    row.setUnqualifiedQty(summary.unqualifiedQty());
                }
            }
        }
    }

    private SummaryResult buildFaiSummary(Long faiId, String judgment) {
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(faiId);
        Map<Long, QmsFaiItemDO> itemById = items.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QmsFaiItemDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        LinkedHashMap<String, Integer> entries = new LinkedHashMap<>();
        Set<Long> summarizedItemIds = new HashSet<>();
        Map<String, String> defectPathCache = new HashMap<>();

        for (QmsFaiSampleDO sample : qmsFaiSampleMapper.selectListByFaiId(faiId)) {
            if (!isFaiSampleAbnormal(sample)) {
                continue;
            }
            QmsFaiItemDO item = itemById.get(sample.getFaiItemId());
            String defectPath = resolveDefectPath(null, sample.getDefectCode(), defectPathCache);
            addSummaryEntry(entries, StrUtil.isNotBlank(defectPath)
                    ? defectPath : buildFallbackLabel(item, sample.getDefectName()), 1);
            if (sample.getFaiItemId() != null) {
                summarizedItemIds.add(sample.getFaiItemId());
            }
        }

        for (QmsFaiItemDO item : items) {
            if (!isFaiItemAbnormal(item) || summarizedItemIds.contains(item.getId())) {
                continue;
            }
            addSummaryEntry(entries, buildFallbackLabel(item, null), abnormalQuantity(item.getAbnormalSampleCount()));
        }
        return buildSummaryResult(entries, judgment, fallbackAbnormalCount(items.stream()
                .map(QmsFaiItemDO::getAbnormalSampleCount)
                .collect(Collectors.toList())));
    }

    private SummaryResult buildCutRoundFqcSummary(Long fqcId, String judgment) {
        LinkedHashMap<String, LinkedHashSet<String>> pieceGroups = new LinkedHashMap<>();
        Set<String> uniquePieceNos = new LinkedHashSet<>();
        for (QmsFqcSubmissionDetailDO detail : qmsFqcSubmissionDetailMapper.selectListByFqcId(fqcId)) {
            if (!isAbnormalResult(detail.getRowJudgment())) {
                continue;
            }
            String pieceNo = StrUtil.trim(detail.getProductionBatchNo());
            String defectName = normalizeDefectDisplayName(detail.getDefectName(), detail.getNgReason(),
                    detail.getProductionBatchNo());
            if (StrUtil.isBlank(pieceNo) || StrUtil.isBlank(defectName)) {
                continue;
            }
            uniquePieceNos.add(pieceNo);
            pieceGroups.computeIfAbsent(defectName, key -> new LinkedHashSet<>()).add(extractPieceSerial(pieceNo));
        }
        if (!pieceGroups.isEmpty()) {
            return new SummaryResult(formatPieceDefectSummary(pieceGroups), BigDecimal.valueOf(uniquePieceNos.size()));
        }

        LinkedHashMap<String, Integer> entries = new LinkedHashMap<>();
        Map<String, String> defectPathCache = new HashMap<>();
        for (QmsFqcSubmissionDetailDO detail : qmsFqcSubmissionDetailMapper.selectListByFqcId(fqcId)) {
            if (!isAbnormalResult(detail.getRowJudgment())) {
                continue;
            }
            String defectPath = resolveDefectPath(null, detail.getDefectCode(), defectPathCache);
            addSummaryEntry(entries, StrUtil.isNotBlank(defectPath)
                    ? defectPath : buildFallbackLabel(
                    firstNotBlank(detail.getDefectName(), detail.getNgReason(), detail.getProductionBatchNo()),
                    firstNotBlank(detail.getModelCode(), detail.getSizeRule()),
                    detail.getProductionBatchNo()), 1);
        }
        addFqcItemSummary(entries, fqcId, defectPathCache);
        return buildSummaryResult(entries, judgment, fallbackFqcAbnormalCount(fqcId));
    }

    private SummaryResult buildFgShippingFqcSummary(Long fqcId, String judgment) {
        LinkedHashMap<String, LinkedHashSet<String>> pieceGroups = new LinkedHashMap<>();
        Set<String> uniquePieceNos = new LinkedHashSet<>();
        for (QmsFqcShippingDetailDO detail : qmsFqcShippingDetailMapper.selectListByFqcId(fqcId)) {
            if (!isAbnormalResult(detail.getRowJudgment())) {
                continue;
            }
            String pieceNo = firstNotBlank(detail.getActualSliceBatchNo(), detail.getSliceBatchNo(), detail.getStockNo());
            String defectName = normalizeDefectDisplayName(detail.getDefectName(),
                    firstNotBlank(detail.getNgReason(), detail.getMismatchReason()), pieceNo);
            if (StrUtil.isBlank(pieceNo) || StrUtil.isBlank(defectName)) {
                continue;
            }
            uniquePieceNos.add(pieceNo);
            pieceGroups.computeIfAbsent(defectName, key -> new LinkedHashSet<>()).add(extractPieceSerial(pieceNo));
        }
        if (!pieceGroups.isEmpty()) {
            return new SummaryResult(formatPieceDefectSummary(pieceGroups), BigDecimal.valueOf(uniquePieceNos.size()));
        }

        LinkedHashMap<String, Integer> entries = new LinkedHashMap<>();
        Map<String, String> defectPathCache = new HashMap<>();
        for (QmsFqcShippingDetailDO detail : qmsFqcShippingDetailMapper.selectListByFqcId(fqcId)) {
            if (!isAbnormalResult(detail.getRowJudgment())) {
                continue;
            }
            String defectPath = resolveDefectPath(null, detail.getDefectCode(), defectPathCache);
            addSummaryEntry(entries, StrUtil.isNotBlank(defectPath)
                    ? defectPath : buildFallbackLabel(
                    firstNotBlank(detail.getDefectName(), detail.getNgReason(), detail.getActualSliceBatchNo(),
                            detail.getSliceBatchNo(), detail.getStockNo()),
                    firstNotBlank(detail.getModelCode(), detail.getProductSize(), detail.getMaterialName()),
                    detail.getStockNo()), abnormalQuantity(detail.getShippingQty()));
        }
        addFqcItemSummary(entries, fqcId, defectPathCache);
        return buildSummaryResult(entries, judgment, fallbackFqcAbnormalCount(fqcId));
    }

    private String formatPieceDefectSummary(LinkedHashMap<String, LinkedHashSet<String>> pieceGroups) {
        return pieceGroups.entrySet().stream()
                .map(entry -> entry.getKey() + "：" + entry.getValue().size() + "片，"
                        + String.join("、", entry.getValue()))
                .collect(Collectors.joining("；"));
    }

    private String normalizeDefectDisplayName(String defectName, String abnormalDesc, String fallback) {
        String value = firstNotBlank(defectName, abnormalDesc, fallback);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        value = StrUtil.trim(value);
        int defectMarkerIndex = Math.max(value.lastIndexOf("缺陷："), value.lastIndexOf("缺陷:"));
        if (defectMarkerIndex >= 0) {
            value = value.substring(defectMarkerIndex + 3);
        }
        value = splitBeforeAny(value, "；", ";", "，", ",", "。");
        value = value.replaceFirst("^[A-Za-z0-9_-]+\\s*[-:：]\\s*", "");
        return StrUtil.trim(value);
    }

    private String splitBeforeAny(String value, String... separators) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        int splitIndex = -1;
        for (String separator : separators) {
            int index = value.indexOf(separator);
            if (index >= 0 && (splitIndex < 0 || index < splitIndex)) {
                splitIndex = index;
            }
        }
        return splitIndex >= 0 ? value.substring(0, splitIndex) : value;
    }

    private String extractPieceSerial(String pieceNo) {
        String value = StrUtil.trim(pieceNo);
        if (StrUtil.isBlank(value)) {
            return "";
        }
        int end = value.length() - 1;
        while (end >= 0 && !Character.isDigit(value.charAt(end))) {
            end--;
        }
        if (end < 0) {
            return value;
        }
        int start = end;
        while (start >= 0 && Character.isDigit(value.charAt(start))) {
            start--;
        }
        return value.substring(start + 1, end + 1);
    }

    private SummaryResult buildIqcSummary(Long iqcId, String judgment) {
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(iqcId);
        Map<Long, QmsIqcItemDO> itemById = items.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QmsIqcItemDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        LinkedHashMap<String, Integer> entries = new LinkedHashMap<>();
        Set<Long> summarizedItemIds = new HashSet<>();
        Map<String, String> defectPathCache = new HashMap<>();

        for (QmsIqcSampleDO sample : qmsIqcSampleMapper.selectListByIqcId(iqcId)) {
            if (!isIqcSampleAbnormal(sample)) {
                continue;
            }
            QmsIqcItemDO item = itemById.get(sample.getIqcItemId());
            String defectPath = resolveDefectPath(null, sample.getDefectCode(), defectPathCache);
            addSummaryEntry(entries, StrUtil.isNotBlank(defectPath)
                    ? defectPath : buildFallbackLabel(item, sample.getDefectName()), 1);
            if (sample.getIqcItemId() != null) {
                summarizedItemIds.add(sample.getIqcItemId());
            }
        }

        for (QmsIqcItemDO item : items) {
            if (!isIqcItemAbnormal(item) || summarizedItemIds.contains(item.getId())) {
                continue;
            }
            addSummaryEntry(entries, buildFallbackLabel(item, null), 1);
        }
        return buildSummaryResult(entries, judgment, fallbackAbnormalCount(items.stream()
                .map(item -> isIqcItemAbnormal(item) ? Integer.valueOf(1) : null)
                .collect(Collectors.toList())));
    }

    private SummaryResult buildOqcSummary(Long oqcId, String judgment) {
        List<QmsOqcItemDO> items = qmsOqcItemMapper.selectListByOqcId(oqcId);
        Map<Long, QmsOqcItemDO> itemById = items.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QmsOqcItemDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        LinkedHashMap<String, Integer> entries = new LinkedHashMap<>();
        Set<Long> summarizedItemIds = new HashSet<>();
        for (QmsOqcSampleDO sample : qmsOqcSampleMapper.selectListByOqcId(oqcId)) {
            if (!isOqcSampleAbnormal(sample)) {
                continue;
            }
            QmsOqcItemDO item = itemById.get(sample.getOqcItemId());
            addSummaryEntry(entries, buildFallbackLabel(item, null), 1);
            if (sample.getOqcItemId() != null) {
                summarizedItemIds.add(sample.getOqcItemId());
            }
        }
        for (QmsOqcItemDO item : items) {
            if (!isOqcItemAbnormal(item) || summarizedItemIds.contains(item.getId())) {
                continue;
            }
            addSummaryEntry(entries, buildFallbackLabel(item, null), abnormalQuantity(item.getAbnormalSampleCount()));
        }
        return buildSummaryResult(entries, judgment, fallbackAbnormalCount(items.stream()
                .map(QmsOqcItemDO::getAbnormalSampleCount)
                .collect(Collectors.toList())));
    }

    private void addFqcItemSummary(LinkedHashMap<String, Integer> entries, Long fqcId,
                                   Map<String, String> defectPathCache) {
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(fqcId);
        Map<Long, QmsFqcItemDO> itemById = items.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QmsFqcItemDO::getId, item -> item, (left, right) -> left,
                        LinkedHashMap::new));
        Set<Long> summarizedItemIds = new HashSet<>();
        Set<String> defectSampleKeys = new HashSet<>();

        for (QmsFqcSampleDefectDO defect : qmsFqcSampleDefectMapper.selectListByFqcId(fqcId)) {
            QmsFqcItemDO item = itemById.get(defect.getFqcItemId());
            String defectPath = resolveDefectPath(defect.getDefectCodeId(), defect.getDefectCode(), defectPathCache);
            addSummaryEntry(entries, StrUtil.isNotBlank(defectPath)
                    ? defectPath : buildFallbackLabel(item, defect.getDefectName()), 1);
            if (defect.getFqcItemId() != null) {
                summarizedItemIds.add(defect.getFqcItemId());
            }
            defectSampleKeys.add(sampleKey(defect.getFqcItemId(), defect.getSampleId(), defect.getSampleSeq()));
        }

        for (QmsFqcSampleDO sample : qmsFqcSampleMapper.selectListByFqcId(fqcId)) {
            if (!isFqcSampleAbnormal(sample)
                    || defectSampleKeys.contains(sampleKey(sample.getFqcItemId(), sample.getId(), sample.getSampleSeq()))) {
                continue;
            }
            QmsFqcItemDO item = itemById.get(sample.getFqcItemId());
            String defectPath = resolveDefectPath(null, sample.getDefectCode(), defectPathCache);
            addSummaryEntry(entries, StrUtil.isNotBlank(defectPath)
                    ? defectPath : buildFallbackLabel(item, sample.getDefectName()), 1);
            if (sample.getFqcItemId() != null) {
                summarizedItemIds.add(sample.getFqcItemId());
            }
        }

        for (QmsFqcItemDO item : items) {
            if (!isFqcItemAbnormal(item) || summarizedItemIds.contains(item.getId())) {
                continue;
            }
            addSummaryEntry(entries, buildFallbackLabel(item, null), abnormalQuantity(item.getAbnormalSampleCount()));
        }
    }

    private SummaryResult buildSummaryResult(LinkedHashMap<String, Integer> entries, String judgment,
                                             Integer fallbackCount) {
        if (entries.isEmpty()) {
            BigDecimal fallbackQty = fallbackCount != null && fallbackCount > 0 ? BigDecimal.valueOf(fallbackCount) : null;
            if (fallbackCount != null && fallbackCount > 0) {
                return new SummaryResult("存在 " + fallbackCount + " 项检验异常", fallbackQty);
            }
            return new SummaryResult(
                    JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(judgment, ""))
                            ? "单据判定不合格" : "存在异常记录",
                    fallbackQty);
        }
        int total = entries.values().stream().mapToInt(Integer::intValue).sum();
        String summary = entries.entrySet().stream()
                .map(entry -> entry.getKey() + " 不合格数量 " + entry.getValue())
                .collect(Collectors.joining("；"));
        return new SummaryResult(summary, BigDecimal.valueOf(total));
    }

    private void applySummary(QmsProductAbnormalEventDetailRespVO detail, SummaryResult summary) {
        if (detail == null || summary == null) {
            return;
        }
        detail.setAbnormalSummary(summary.summary());
        detail.setUnqualifiedQty(summary.unqualifiedQty());
    }

    private void addSummaryEntry(LinkedHashMap<String, Integer> entries, String label, int qty) {
        if (StrUtil.isBlank(label) || qty <= 0) {
            return;
        }
        entries.merge(label.trim(), qty, Integer::sum);
    }

    private String resolveDefectPath(Long defectCodeId, String defectCode, Map<String, String> defectPathCache) {
        String cacheKey = defectCodeId != null ? "ID:" + defectCodeId : "CODE:" + StrUtil.blankToDefault(defectCode, "");
        if (defectPathCache.containsKey(cacheKey)) {
            return defectPathCache.get(cacheKey);
        }
        QmsDefectCodeDO defect = defectCodeId == null ? null : qmsDefectCodeMapper.selectById(defectCodeId);
        if (defect == null && StrUtil.isNotBlank(defectCode)) {
            defect = qmsDefectCodeMapper.selectOne(new LambdaQueryWrapperX<QmsDefectCodeDO>()
                    .eq(QmsDefectCodeDO::getCode, defectCode.trim())
                    .last("LIMIT 1"));
        }
        String path = buildDefectPath(defect);
        defectPathCache.put(cacheKey, path);
        return path;
    }

    private String buildDefectPath(QmsDefectCodeDO defect) {
        if (defect == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Set<Long> visitedIds = new HashSet<>();
        QmsDefectCodeDO current = defect;
        while (current != null && current.getId() != null && visitedIds.add(current.getId())) {
            if (StrUtil.isNotBlank(current.getName())) {
                names.add(current.getName().trim());
            }
            Long parentId = current.getParentId();
            if (parentId == null || parentId <= 0) {
                break;
            }
            current = qmsDefectCodeMapper.selectById(parentId);
        }
        Collections.reverse(names);
        return names.isEmpty() ? null : String.join("-", names);
    }

    private String buildFallbackLabel(QmsFaiItemDO item, String fallback) {
        return item == null ? fallback : buildFallbackLabel(
                firstNotBlank(item.getSheetMetricName(), item.getInspectionItem(), item.getMetricCode()),
                firstNotBlank(item.getStandardDesc(), item.getRuleDescription()),
                fallback);
    }

    private String buildFallbackLabel(QmsFqcItemDO item, String fallback) {
        return item == null ? fallback : buildFallbackLabel(
                firstNotBlank(item.getSheetMetricName(), item.getInspectionItem(), item.getMetricCode()),
                firstNotBlank(item.getStandardDesc(), item.getRuleDescription()),
                fallback);
    }

    private String buildFallbackLabel(QmsIqcItemDO item, String fallback) {
        return item == null ? fallback : buildFallbackLabel(
                item.getInspectionItem(),
                firstNotBlank(item.getStandardDesc(), item.getRuleDescription()),
                fallback);
    }

    private String buildFallbackLabel(QmsOqcItemDO item, String fallback) {
        return item == null ? fallback : buildFallbackLabel(
                firstNotBlank(item.getInspectionItem(), item.getCategory()),
                firstNotBlank(item.getStandardDesc(), item.getRuleDescription()),
                fallback);
    }

    private String buildFallbackLabel(String inspectionItem, String standardDesc, String fallback) {
        String item = StrUtil.blankToDefault(inspectionItem, "").trim();
        String standard = StrUtil.blankToDefault(standardDesc, "").trim();
        if (StrUtil.isNotBlank(item) && StrUtil.isNotBlank(standard)) {
            return item + " " + standard;
        }
        return firstNotBlank(item, standard, fallback);
    }

    private int abnormalQuantity(Integer qty) {
        return qty != null && qty > 0 ? qty : 1;
    }

    private Integer fallbackAbnormalCount(List<Integer> counts) {
        int total = counts.stream()
                .filter(Objects::nonNull)
                .filter(count -> count > 0)
                .mapToInt(Integer::intValue)
                .sum();
        return total > 0 ? total : null;
    }

    private Integer fallbackFqcAbnormalCount(Long fqcId) {
        return fallbackAbnormalCount(qmsFqcItemMapper.selectListByFqcId(fqcId).stream()
                .map(QmsFqcItemDO::getAbnormalSampleCount)
                .collect(Collectors.toList()));
    }

    private String sampleKey(Long itemId, Long sampleId, Integer sampleSeq) {
        return String.valueOf(itemId) + "#" + String.valueOf(sampleId) + "#" + String.valueOf(sampleSeq);
    }

    private boolean isFaiItemAbnormal(QmsFaiItemDO item) {
        return item != null && (isAbnormalResult(item.getQaResult())
                || isAbnormalResult(item.getOperatorResult())
                || isAbnormalResult(item.getInputStatus())
                || (item.getAbnormalSampleCount() != null && item.getAbnormalSampleCount() > 0));
    }

    private boolean isFqcItemAbnormal(QmsFqcItemDO item) {
        return item != null && (isAbnormalResult(item.getItemResult())
                || isAbnormalResult(item.getQaResult())
                || isAbnormalResult(item.getOperatorResult())
                || isAbnormalResult(item.getInputStatus())
                || (item.getAbnormalSampleCount() != null && item.getAbnormalSampleCount() > 0));
    }

    private boolean isIqcItemAbnormal(QmsIqcItemDO item) {
        return item != null && isAbnormalResult(item.getItemResult());
    }

    private boolean isOqcItemAbnormal(QmsOqcItemDO item) {
        return item != null && (isAbnormalResult(item.getItemResult())
                || isAbnormalResult(item.getQaResult())
                || isAbnormalResult(item.getOperatorResult())
                || isAbnormalResult(item.getInputStatus())
                || (item.getAbnormalSampleCount() != null && item.getAbnormalSampleCount() > 0));
    }

    private boolean isFaiSampleAbnormal(QmsFaiSampleDO sample) {
        return sample != null && (isAbnormalResult(sample.getSampleResult())
                || isAbnormalResult(sample.getQualitativeValue())
                || StrUtil.isNotBlank(sample.getDefectCode())
                || StrUtil.isNotBlank(sample.getDefectName()));
    }

    private boolean isFqcSampleAbnormal(QmsFqcSampleDO sample) {
        return sample != null && (isAbnormalResult(sample.getSampleResult())
                || isAbnormalResult(sample.getQualitativeValue())
                || StrUtil.isNotBlank(sample.getDefectCode())
                || StrUtil.isNotBlank(sample.getDefectName()));
    }

    private boolean isIqcSampleAbnormal(QmsIqcSampleDO sample) {
        return sample != null && (isAbnormalResult(sample.getSampleResult())
                || isAbnormalResult(sample.getQualitativeValue())
                || StrUtil.isNotBlank(sample.getDefectCode())
                || StrUtil.isNotBlank(sample.getDefectName()));
    }

    private boolean isOqcSampleAbnormal(QmsOqcSampleDO sample) {
        return sample != null && (isAbnormalResult(sample.getSampleResult())
                || isAbnormalResult(sample.getQualitativeValue()));
    }

    private boolean isAbnormalResult(String result) {
        return JUDGMENT_NG.equalsIgnoreCase(StrUtil.blankToDefault(result, ""))
                || INPUT_STATUS_ABNORMAL.equalsIgnoreCase(StrUtil.blankToDefault(result, ""));
    }

    private boolean includesSource(QmsProductAbnormalEventPageReqVO reqVO, String sourceType) {
        String requested = normalizeSourceType(reqVO.getSourceType());
        return StrUtil.isBlank(requested) || sourceType.equals(requested);
    }

    private String normalizeSourceType(String sourceType) {
        return StrUtil.blankToDefault(sourceType, "").trim().toUpperCase();
    }

    private boolean matchesRequest(QmsProductAbnormalEventRespVO row, QmsProductAbnormalEventPageReqVO reqVO) {
        return matchesInspectionNo(row, reqVO.getInspectionNo())
                && containsIfPresent(row.getOperationName(), reqVO.getOperationName())
                && matchesProcessCategory(row, reqVO.getProcessCategory())
                && containsIfPresent(row.getProductModel(), reqVO.getProductModel())
                && containsIfPresent(row.getProductBatchNo(), reqVO.getProductBatchNo())
                && matchesEventTimeRange(row, reqVO.getInspectionTime());
    }

    private boolean matchesProcessCategory(QmsProductAbnormalEventRespVO row, String processCategory) {
        if (StrUtil.isBlank(processCategory)) {
            return true;
        }
        List<String> processCategories = new ArrayList<>();
        List<String> operationCodes = new ArrayList<>();
        List<String> operationNames = new ArrayList<>();
        qmsFaiOrderMapper.appendFaiLedgerProcessAliases(processCategory, processCategories, operationCodes,
                operationNames);
        return equalsAnyIgnoreCase(row.getProcessCategory(), processCategories)
                || equalsAnyIgnoreCase(row.getOperationName(), processCategories)
                || equalsAnyIgnoreCase(row.getOperationName(), operationCodes)
                || containsAnyIgnoreCase(row.getOperationName(), operationNames);
    }

    private boolean equalsAnyIgnoreCase(String value, Collection<String> candidates) {
        if (StrUtil.isBlank(value) || candidates == null || candidates.isEmpty()) {
            return false;
        }
        return candidates.stream().anyMatch(candidate -> StrUtil.equalsIgnoreCase(value.trim(), candidate));
    }

    private boolean containsAnyIgnoreCase(String value, Collection<String> candidates) {
        if (StrUtil.isBlank(value) || candidates == null || candidates.isEmpty()) {
            return false;
        }
        String text = value.trim();
        return candidates.stream()
                .filter(StrUtil::isNotBlank)
                .anyMatch(candidate -> StrUtil.containsIgnoreCase(text, candidate.trim()));
    }

    private boolean matchesInspectionNo(QmsProductAbnormalEventRespVO row, String keyword) {
        return containsIfPresent(row.getInspectionNo(), keyword)
                || containsIfPresent(row.getRejectNextInspectionNo(), keyword)
                || containsIfPresent(row.getRecheckRootInspectionNo(), keyword)
                || containsIfPresent(row.getRecheckPrevInspectionNo(), keyword)
                || containsIfPresent(row.getRecheckLatestInspectionNo(), keyword);
    }

    private boolean matchesEventTimeRange(QmsProductAbnormalEventRespVO row, LocalDateTime[] range) {
        if (range == null || range.length == 0) {
            return true;
        }
        return matchesTimeRange(row.getInspectionTime(), range) || matchesTimeRange(row.getRejectTime(), range);
    }

    private boolean containsIfPresent(String value, String keyword) {
        return StrUtil.isBlank(keyword) || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(value, ""), keyword.trim());
    }

    private boolean matchesTimeRange(LocalDateTime value, LocalDateTime[] range) {
        if (range == null || range.length == 0) {
            return true;
        }
        if (value == null) {
            return false;
        }
        LocalDateTime start = range.length > 0 ? range[0] : null;
        LocalDateTime end = range.length > 1 ? range[1] : null;
        return (start == null || !value.isBefore(start)) && (end == null || !value.isAfter(end));
    }

    private int compareByInspectionTimeDesc(QmsProductAbnormalEventRespVO left, QmsProductAbnormalEventRespVO right) {
        int timeCompare = Comparator.nullsLast(LocalDateTime::compareTo)
                .compare(eventSortTime(right), eventSortTime(left));
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Comparator.nullsLast(Long::compareTo)
                .compare(right.getInspectionId(), left.getInspectionId());
    }

    private LocalDateTime eventSortTime(QmsProductAbnormalEventRespVO row) {
        return firstNotNull(row.getRejectTime(), row.getInspectionTime());
    }

    private List<QmsProductAbnormalEventRespVO> page(List<QmsProductAbnormalEventRespVO> rows,
                                                     QmsProductAbnormalEventPageReqVO reqVO) {
        Integer pageSize = reqVO.getPageSize();
        if (pageSize != null && pageSize < 0) {
            return rows;
        }
        int safePageNo = Math.max(1, Objects.requireNonNullElse(reqVO.getPageNo(), 1));
        int safePageSize = Math.max(1, Objects.requireNonNullElse(pageSize, 10));
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new ArrayList<>(rows.subList(fromIndex, toIndex));
    }

    @SafeVarargs
    private <T> T firstNotNull(T... values) {
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

    private Integer firstPositiveInteger(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal bigDecimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String formatStats(BigDecimal min, BigDecimal max, BigDecimal avg) {
        List<String> parts = new ArrayList<>();
        if (min != null) {
            parts.add("最小 " + min.stripTrailingZeros().toPlainString());
        }
        if (max != null) {
            parts.add("最大 " + max.stripTrailingZeros().toPlainString());
        }
        if (avg != null) {
            parts.add("平均 " + avg.stripTrailingZeros().toPlainString());
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private record SummaryResult(String summary, BigDecimal unqualifiedQty) {
    }

    private record CopyInspectionResult(Long newInspectionId, String newInspectionNo, Long tenantId,
                                        Map<Long, Long> itemIdMap) {
    }

    private record RecheckSelection(Set<Long> itemIds,
                                    Map<Long, Set<String>> positionsByItemId,
                                    Map<Long, Set<String>> piecesByItemId) {

        static RecheckSelection all() {
            return new RecheckSelection(null, Map.of(), Map.of());
        }

        static RecheckSelection items(Set<Long> itemIds) {
            return new RecheckSelection(itemIds, Map.of(), Map.of());
        }

        static RecheckSelection pendingStandardSelection() {
            return new RecheckSelection(Collections.emptySet(), Map.of(), Map.of());
        }
    }
}
