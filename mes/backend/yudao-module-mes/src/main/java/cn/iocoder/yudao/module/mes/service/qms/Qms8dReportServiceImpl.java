package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dActionItemDoneReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dActionItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportLinkSourceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dRelationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dTeamMemberReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dActionItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dFlowLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dTeamMemberDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dActionItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dFlowLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dTeamMemberMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class Qms8dReportServiceImpl implements Qms8dReportService {

    private static final ErrorCode QMS_8D_NOT_EXISTS = new ErrorCode(1008100150, "8D报告不存在");
    private static final ErrorCode QMS_8D_CLOSED = new ErrorCode(1008100151, "8D报告已关闭，不能继续流转");
    private static final ErrorCode QMS_8D_LEADER_REQUIRED = new ErrorCode(1008100152, "8D团队必须至少包含一名组长");
    private static final ErrorCode QMS_8D_RETURN_OPINION_REQUIRED = new ErrorCode(1008100153, "退回意见不能为空");
    private static final ErrorCode QMS_8D_CLOSE_STEP_INVALID = new ErrorCode(1008100154, "只有 D7-D8 阶段可以关闭 8D");
    private static final ErrorCode QMS_8D_CLOSE_VALIDATION_REQUIRED = new ErrorCode(1008100155, "关闭 8D 必须填写验证结果");
    private static final ErrorCode QMS_8D_STANDARDIZE_REQUIRED = new ErrorCode(1008100156, "勾选标准化更新时必须填写标准化说明");
    private static final ErrorCode QMS_8D_ACTION_ITEM_OPEN = new ErrorCode(1008100157, "存在未完成或未取消的 CAPA 行动项，不能关闭 8D");
    private static final ErrorCode QMS_8D_ACTION_ITEM_NOT_EXISTS = new ErrorCode(1008100158, "8D行动项不存在");
    private static final ErrorCode QMS_8D_ACTION_ITEM_PERMISSION = new ErrorCode(1008100159, "当前用户不是行动项责任人或 8D 组长，不能完成该行动项");
    private static final ErrorCode QMS_8D_STAGE_REQUIRED = new ErrorCode(1008100160, "当前阶段必填项未完成，不能推进 8D");
    private static final ErrorCode QMS_8D_SOURCE_NOT_EXISTS = new ErrorCode(1008100161, "关联来源对象不存在");

    private static final String STATUS_APPROVING = "APPROVING";
    private static final String STATUS_PASSED = "PASSED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String STEP_D1_D2 = "D1_D2";
    private static final String STEP_D3 = "D3";
    private static final String STEP_D4 = "D4";
    private static final String STEP_D5_D6 = "D5_D6";
    private static final String STEP_D7_D8 = "D7_D8";
    private static final String STEP_CLOSED = "CLOSED";
    private static final String ATTACHMENT_RELATION_TYPE = "ATTACHMENT";

    @Resource
    private Qms8dReportMapper qms8dReportMapper;
    @Resource
    private Qms8dTeamMemberMapper qms8dTeamMemberMapper;
    @Resource
    private Qms8dRelationMapper qms8dRelationMapper;
    @Resource
    private Qms8dActionItemMapper qms8dActionItemMapper;
    @Resource
    private Qms8dFlowLogMapper qms8dFlowLogMapper;
    @Resource
    private QmsExceptionEventMapper qmsExceptionEventMapper;
    @Resource
    private QmsNcRecordMapper qmsNcRecordMapper;

    @Override
    public PageResult<Qms8dReportRespVO> getReportPage(Qms8dReportPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Set<Long> participatedIds = buildParticipatedReportIds(loginUserId);
        PageResult<Qms8dReportDO> pageResult = qms8dReportMapper.selectPage(pageReqVO, loginUserId, participatedIds);
        return BeanUtils.toBean(pageResult, Qms8dReportRespVO.class);
    }

    @Override
    public Qms8dReportRespVO getReport(Long id) {
        return buildReportResp(validateReportExists(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(Qms8dReportSaveReqVO createReqVO) {
        Qms8dReportDO existing = resolveDuplicateSource(createReqVO.getSourceType(), createReqVO.getSourceId(), createReqVO.getSourceNo());
        if (existing != null) {
            return existing.getId();
        }
        validateLeader(createReqVO.getTeamMembers());
        LocalDate nowDate = LocalDate.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = currentUserName();
        Qms8dReportDO entity = BeanUtils.toBean(createReqVO, Qms8dReportDO.class);
        entity.setReportNo(StrUtil.blankToDefault(createReqVO.getReportNo(), generate8dNo()));
        entity.setIssueDate(createReqVO.getIssueDate() != null ? createReqVO.getIssueDate() : nowDate);
        entity.setTargetDate(createReqVO.getTargetDate() != null ? createReqVO.getTargetDate() : nowDate.plusDays(30));
        entity.setCurrentStep(STEP_D1_D2);
        entity.setStatus(STATUS_APPROVING);
        applyNode(entity, STEP_D1_D2);
        entity.setInitiatorUserId(loginUserId);
        entity.setInitiatorUserName(loginUserName);
        entity.setInitiatorDeptId(SecurityFrameworkUtils.getLoginUserDeptId());
        entity.setInitiatorDeptName("当前部门");
        entity.setCurrentHandlerUserId(firstNonNull(createReqVO.getCurrentHandlerUserId(), findLeaderUserId(createReqVO.getTeamMembers()), loginUserId));
        entity.setCurrentHandlerUserName(firstNotBlank(createReqVO.getCurrentHandlerUserName(), findLeaderUserName(createReqVO.getTeamMembers()), loginUserName));
        entity.setUpdateSop(Boolean.TRUE.equals(createReqVO.getUpdateSop()));
        entity.setUpdateFmea(Boolean.TRUE.equals(createReqVO.getUpdateFmea()));
        entity.setUpdateControlPlan(Boolean.TRUE.equals(createReqVO.getUpdateControlPlan()));
        qms8dReportMapper.insert(entity);
        saveTeamMembers(entity.getId(), createReqVO.getTeamMembers());
        saveActionItems(entity, createReqVO.getActionItems());
        linkPrimarySource(entity, createReqVO);
        saveAttachmentRelations(entity, createReqVO.getRelations());
        Qms8dReportDO after = qms8dReportMapper.selectById(entity.getId());
        writeFlowLog(null, after, "CREATE", "立案", null,
                snapshot("reportNo", after.getReportNo(), "sourceType", after.getSourceType(),
                        "sourceNo", after.getSourceNo(), "targetDate", after.getTargetDate()));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(Qms8dReportSaveReqVO updateReqVO) {
        Qms8dReportDO before = validateReportCanFlow(updateReqVO.getId());
        Qms8dReportDO updateObj = BeanUtils.toBean(updateReqVO, Qms8dReportDO.class);
        updateObj.setId(before.getId());
        updateObj.setStatus(null);
        updateObj.setCurrentStep(null);
        qms8dReportMapper.updateById(updateObj);
        Qms8dReportDO after = qms8dReportMapper.selectById(before.getId());
        saveTeamMembersIfPresent(after.getId(), updateReqVO.getTeamMembers());
        saveActionItems(after, updateReqVO.getActionItems());
        saveAttachmentRelations(after, updateReqVO.getRelations());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Qms8dReportHandleReqVO handleReqVO) {
        Qms8dReportDO before = validateReportCanFlow(handleReqVO.getId());
        Qms8dReportDO updateObj = BeanUtils.toBean(handleReqVO, Qms8dReportDO.class);
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_APPROVING);
        validateStageRequired(before.getCurrentStep(), updateObj, handleReqVO.getTeamMembers(), handleReqVO.getActionItems());
        String nextStep = nextStep(before.getCurrentStep());
        updateObj.setCurrentStep(nextStep);
        applyNode(updateObj, nextStep);
        updateObj.setCurrentHandlerUserId(firstNonNull(handleReqVO.getNextHandlerUserId(),
                handleReqVO.getCurrentHandlerUserId(), before.getCurrentHandlerUserId(), SecurityFrameworkUtils.getLoginUserId()));
        updateObj.setCurrentHandlerUserName(firstNotBlank(handleReqVO.getNextHandlerUserName(),
                handleReqVO.getCurrentHandlerUserName(), before.getCurrentHandlerUserName(), currentUserName()));
        qms8dReportMapper.updateById(updateObj);
        Qms8dReportDO after = qms8dReportMapper.selectById(before.getId());
        saveTeamMembersIfPresent(after.getId(), handleReqVO.getTeamMembers());
        saveActionItems(after, handleReqVO.getActionItems());
        saveAttachmentRelations(after, handleReqVO.getRelations());
        writeFlowLog(before, after, "HANDLE", "办理", handleReqVO.getOpinion(),
                snapshot("fromStep", before.getCurrentStep(), "toStep", after.getCurrentStep(),
                        "nextHandlerUserName", after.getCurrentHandlerUserName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnReport(Qms8dReportReturnReqVO returnReqVO) {
        if (StrUtil.isBlank(returnReqVO.getOpinion())) {
            throw exception(QMS_8D_RETURN_OPINION_REQUIRED);
        }
        Qms8dReportDO before = validateReportCanFlow(returnReqVO.getId());
        Qms8dReportDO updateObj = new Qms8dReportDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_REJECTED);
        updateObj.setCurrentStep(StrUtil.blankToDefault(returnReqVO.getTargetStep(), before.getCurrentStep()));
        applyNode(updateObj, updateObj.getCurrentStep());
        updateObj.setCurrentHandlerUserId(before.getInitiatorUserId());
        updateObj.setCurrentHandlerUserName(firstNotBlank(before.getInitiatorUserName(), currentUserName()));
        qms8dReportMapper.updateById(updateObj);
        Qms8dReportDO after = qms8dReportMapper.selectById(before.getId());
        writeFlowLog(before, after, "RETURN", "退回", returnReqVO.getOpinion(),
                snapshot("targetStep", after.getCurrentStep(), "returnUserName", currentUserName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeReport(Qms8dReportCloseReqVO closeReqVO) {
        Qms8dReportDO before = validateReportCanFlow(closeReqVO.getId());
        if (!STEP_D7_D8.equals(before.getCurrentStep())) {
            throw exception(QMS_8D_CLOSE_STEP_INVALID);
        }
        String validationResult = firstNotBlank(closeReqVO.getValidationResult(), before.getValidationResult());
        if (StrUtil.isBlank(validationResult)) {
            throw exception(QMS_8D_CLOSE_VALIDATION_REQUIRED);
        }
        String standardizeDesc = firstNotBlank(closeReqVO.getStandardizeDesc(), before.getStandardizeDesc());
        if ((Boolean.TRUE.equals(before.getUpdateSop())
                || Boolean.TRUE.equals(before.getUpdateFmea())
                || Boolean.TRUE.equals(before.getUpdateControlPlan()))
                && StrUtil.isBlank(standardizeDesc)) {
            throw exception(QMS_8D_STANDARDIZE_REQUIRED);
        }
        if (CollUtil.isNotEmpty(qms8dActionItemMapper.selectOpenListByReportId(before.getId()))) {
            throw exception(QMS_8D_ACTION_ITEM_OPEN);
        }
        LocalDateTime now = LocalDateTime.now();
        Qms8dReportDO updateObj = new Qms8dReportDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_PASSED);
        updateObj.setCurrentStep(STEP_CLOSED);
        applyNode(updateObj, STEP_CLOSED);
        updateObj.setValidationResult(validationResult);
        updateObj.setStandardizeDesc(standardizeDesc);
        updateObj.setValidationDate(before.getValidationDate() != null ? before.getValidationDate() : LocalDate.now());
        updateObj.setCloseTime(now);
        updateObj.setCloseUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setCloseUserName(currentUserName());
        updateObj.setCurrentHandlerUserId(null);
        updateObj.setCurrentHandlerUserName(null);
        qms8dReportMapper.updateById(updateObj);
        Qms8dReportDO after = qms8dReportMapper.selectById(before.getId());
        writeFlowLog(before, after, "CLOSE", "关闭", firstNotBlank(closeReqVO.getOpinion(), validationResult),
                snapshot("closeUserName", after.getCloseUserName(), "closeTime", after.getCloseTime(),
                        "validationResult", validationResult));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkSource(Qms8dReportLinkSourceReqVO linkReqVO) {
        Qms8dReportDO before = validateReportExists(linkReqVO.getId());
        SourceInfo sourceInfo = resolveSourceInfo(linkReqVO.getRelationType(), linkReqVO.getRelatedObjectId(),
                linkReqVO.getRelatedObjectNo(), linkReqVO.getRelatedObjectName(), linkReqVO.getRelationStatus());
        Qms8dRelationDO existing = qms8dRelationMapper.selectByObjectNo(before.getId(), linkReqVO.getRelationType(), sourceInfo.sourceNo);
        if (existing == null) {
            boolean firstRelation = qms8dRelationMapper.selectCountByReportId(before.getId()) == 0;
            qms8dRelationMapper.insert(Qms8dRelationDO.builder()
                    .reportId(before.getId())
                    .reportNo(before.getReportNo())
                    .relationType(linkReqVO.getRelationType())
                    .relatedObjectId(sourceInfo.sourceId)
                    .relatedObjectNo(sourceInfo.sourceNo)
                    .relatedObjectName(sourceInfo.sourceName)
                    .relationStatus(sourceInfo.sourceStatus)
                    .primaryFlag(firstRelation)
                    .relationTime(LocalDateTime.now())
                    .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                    .relationUserName(currentUserName())
                    .remark(linkReqVO.getRemark())
                    .build());
        }
        writeFlowLog(before, before, "LINK_SOURCE", "关联来源", linkReqVO.getRemark(),
                snapshot("relationType", linkReqVO.getRelationType(), "relatedObjectNo", sourceInfo.sourceNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void actionItemDone(Qms8dActionItemDoneReqVO doneReqVO) {
        Qms8dActionItemDO item = qms8dActionItemMapper.selectById(doneReqVO.getItemId());
        if (item == null) {
            throw exception(QMS_8D_ACTION_ITEM_NOT_EXISTS);
        }
        Qms8dReportDO report = validateReportCanFlow(item.getReportId());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!isLeader(report.getId(), loginUserId) && (loginUserId == null || !loginUserId.equals(item.getOwnerUserId()))) {
            throw exception(QMS_8D_ACTION_ITEM_PERMISSION);
        }
        Qms8dActionItemDO updateObj = new Qms8dActionItemDO();
        updateObj.setId(item.getId());
        updateObj.setItemStatus(StrUtil.blankToDefault(doneReqVO.getItemStatus(), "DONE"));
        updateObj.setFinishDesc(doneReqVO.getFinishDesc());
        updateObj.setVerificationResult(doneReqVO.getVerificationResult());
        updateObj.setActualFinishDate(LocalDate.now());
        qms8dActionItemMapper.updateById(updateObj);
        writeFlowLog(report, report, "ACTION_DONE", "行动项完成", doneReqVO.getFinishDesc(),
                snapshot("itemId", item.getId(), "ownerName", item.getOwnerUserName(),
                        "finishDate", updateObj.getActualFinishDate(),
                        "verificationResult", doneReqVO.getVerificationResult()));
    }

    @Override
    public List<Qms8dReportRespVO.FlowLog> getFlowLogList(Long reportId) {
        validateReportExists(reportId);
        return BeanUtils.toBean(qms8dFlowLogMapper.selectListByReportId(reportId), Qms8dReportRespVO.FlowLog.class);
    }

    private Qms8dReportDO validateReportExists(Long id) {
        Qms8dReportDO report = qms8dReportMapper.selectById(id);
        if (report == null) {
            throw exception(QMS_8D_NOT_EXISTS);
        }
        return report;
    }

    private Qms8dReportDO validateReportCanFlow(Long id) {
        Qms8dReportDO report = validateReportExists(id);
        if (STATUS_PASSED.equals(report.getStatus()) || STEP_CLOSED.equals(report.getCurrentStep())) {
            throw exception(QMS_8D_CLOSED);
        }
        return report;
    }

    private void validateLeader(List<Qms8dTeamMemberReqVO> teamMembers) {
        if (CollUtil.isEmpty(teamMembers) || teamMembers.stream().noneMatch(member -> "LEADER".equals(member.getMemberRole()))) {
            throw exception(QMS_8D_LEADER_REQUIRED);
        }
    }

    private void validateStageRequired(String currentStep, Qms8dReportDO updateObj,
                                       List<Qms8dTeamMemberReqVO> teamMembers,
                                       List<Qms8dActionItemReqVO> actionItems) {
        if (STEP_D1_D2.equals(currentStep)) {
            validateLeader(teamMembers);
            if (StrUtil.isBlank(updateObj.getProblemDesc())) {
                throw exception(QMS_8D_STAGE_REQUIRED);
            }
        } else if (STEP_D3.equals(currentStep)) {
            if (StrUtil.isBlank(updateObj.getContainmentAction())
                    || (updateObj.getContainmentOwnerId() == null && StrUtil.isBlank(updateObj.getContainmentOwnerName()))
                    || updateObj.getContainmentDate() == null) {
                throw exception(QMS_8D_STAGE_REQUIRED);
            }
        } else if (STEP_D4.equals(currentStep)) {
            if (StrUtil.isBlank(updateObj.getRootCauseCategory()) || StrUtil.isBlank(updateObj.getRootCauseAnalysis())) {
                throw exception(QMS_8D_STAGE_REQUIRED);
            }
        } else if (STEP_D5_D6.equals(currentStep)) {
            if (CollUtil.isEmpty(actionItems)
                    || actionItems.stream().noneMatch(item -> !"CANCELLED".equals(item.getItemStatus()))) {
                throw exception(QMS_8D_STAGE_REQUIRED);
            }
        }
    }

    private Qms8dReportDO resolveDuplicateSource(String sourceType, Long sourceId, String sourceNo) {
        if (StrUtil.isBlank(sourceType) || (sourceId == null && StrUtil.isBlank(sourceNo))) {
            return null;
        }
        Qms8dRelationDO relation = qms8dRelationMapper.selectPrimaryByObject(sourceType, sourceId, sourceNo);
        if (relation != null) {
            return qms8dReportMapper.selectById(relation.getReportId());
        }
        return qms8dReportMapper.selectBySource(sourceType, sourceId, sourceNo);
    }

    private void saveTeamMembersIfPresent(Long reportId, List<Qms8dTeamMemberReqVO> teamMembers) {
        if (teamMembers != null) {
            saveTeamMembers(reportId, teamMembers);
        }
    }

    private void saveTeamMembers(Long reportId, List<Qms8dTeamMemberReqVO> teamMembers) {
        qms8dTeamMemberMapper.delete(Qms8dTeamMemberDO::getReportId, reportId);
        if (CollUtil.isEmpty(teamMembers)) {
            return;
        }
        List<Qms8dTeamMemberDO> memberDOs = teamMembers.stream()
                .filter(member -> StrUtil.isNotBlank(member.getUserName()) || member.getUserId() != null)
                .map(member -> Qms8dTeamMemberDO.builder()
                        .reportId(reportId)
                        .memberRole(firstNotBlank(member.getMemberRole(), "MEMBER"))
                        .deptId(member.getDeptId())
                        .deptName(firstNotBlank(member.getDeptName(), "未指定部门"))
                        .userId(member.getUserId())
                        .userName(firstNotBlank(member.getUserName(), "未指定成员"))
                        .responsibility(member.getResponsibility())
                        .sort(member.getSort())
                        .build())
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(memberDOs)) {
            qms8dTeamMemberMapper.insertBatch(memberDOs);
        }
    }

    private void saveActionItems(Qms8dReportDO report, List<Qms8dActionItemReqVO> actionItems) {
        if (actionItems == null) {
            return;
        }
        qms8dActionItemMapper.delete(Qms8dActionItemDO::getReportId, report.getId());
        if (CollUtil.isEmpty(actionItems)) {
            return;
        }
        List<Qms8dActionItemDO> itemDOs = actionItems.stream()
                .filter(item -> StrUtil.isNotBlank(item.getActionDesc()))
                .map(item -> Qms8dActionItemDO.builder()
                        .reportId(report.getId())
                        .reportNo(report.getReportNo())
                        .actionType(firstNotBlank(item.getActionType(), "CORRECTIVE"))
                        .actionDesc(item.getActionDesc())
                        .rootCauseCategory(item.getRootCauseCategory())
                        .ownerUserId(item.getOwnerUserId())
                        .ownerUserName(firstNotBlank(item.getOwnerUserName(), "未指定责任人"))
                        .ownerDeptId(item.getOwnerDeptId())
                        .ownerDeptName(item.getOwnerDeptName())
                        .planFinishDate(item.getPlanFinishDate() != null ? item.getPlanFinishDate() : LocalDate.now())
                        .actualFinishDate(item.getActualFinishDate())
                        .itemStatus(firstNotBlank(item.getItemStatus(), "TODO"))
                        .finishDesc(item.getFinishDesc())
                        .verificationResult(item.getVerificationResult())
                        .sort(item.getSort())
                        .remark(item.getRemark())
                        .build())
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(itemDOs)) {
            qms8dActionItemMapper.insertBatch(itemDOs);
        }
    }

    private void saveAttachmentRelations(Qms8dReportDO report, List<Qms8dRelationReqVO> relations) {
        if (relations == null) {
            return;
        }
        qms8dRelationMapper.deleteByReportIdAndRelationType(report.getId(), ATTACHMENT_RELATION_TYPE);
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        List<Qms8dRelationDO> relationDOs = new ArrayList<>();
        for (Qms8dRelationReqVO relation : relations) {
            if (relation == null || !ATTACHMENT_RELATION_TYPE.equals(relation.getRelationType())) {
                continue;
            }
            String attachmentUrl = firstNotBlank(relation.getRemark(), relation.getRelatedObjectNo());
            if (StrUtil.isBlank(attachmentUrl)) {
                continue;
            }
            relationDOs.add(Qms8dRelationDO.builder()
                    .reportId(report.getId())
                    .reportNo(report.getReportNo())
                    .relationType(ATTACHMENT_RELATION_TYPE)
                    .relatedObjectId(relation.getRelatedObjectId())
                    .relatedObjectNo(attachmentUrl)
                    .relatedObjectName(firstNotBlank(relation.getRelatedObjectName(),
                            getAttachmentName(attachmentUrl, relationDOs.size() + 1)))
                    .relationStatus(firstNotBlank(relation.getRelationStatus(), "ACTIVE"))
                    .primaryFlag(false)
                    .relationTime(LocalDateTime.now())
                    .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                    .relationUserName(currentUserName())
                    .remark(attachmentUrl)
                    .build());
        }
        if (CollUtil.isNotEmpty(relationDOs)) {
            qms8dRelationMapper.insertBatch(relationDOs);
        }
    }

    private String getAttachmentName(String attachmentUrl, int index) {
        String cleanUrl = StrUtil.subBefore(attachmentUrl, "?", false);
        String fileName = StrUtil.subAfter(cleanUrl, "/", true);
        return StrUtil.blankToDefault(fileName, "附件" + index);
    }

    private void linkPrimarySource(Qms8dReportDO report, Qms8dReportSaveReqVO createReqVO) {
        SourceInfo sourceInfo = resolveSourceInfo(report.getSourceType(), report.getSourceId(), report.getSourceNo(),
                null, null);
        qms8dRelationMapper.insert(Qms8dRelationDO.builder()
                .reportId(report.getId())
                .reportNo(report.getReportNo())
                .relationType(report.getSourceType())
                .relatedObjectId(sourceInfo.sourceId)
                .relatedObjectNo(sourceInfo.sourceNo)
                .relatedObjectName(firstNotBlank(sourceInfo.sourceName, createReqVO.getProblemDesc()))
                .relationStatus(sourceInfo.sourceStatus)
                .primaryFlag(true)
                .relationTime(LocalDateTime.now())
                .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                .relationUserName(currentUserName())
                .remark("8D立案主来源")
                .build());
    }

    private SourceInfo resolveSourceInfo(String sourceType, Long sourceId, String sourceNo, String sourceName, String sourceStatus) {
        if ("EXCEPTION".equals(sourceType)) {
            QmsExceptionEventDO event = sourceId != null ? qmsExceptionEventMapper.selectById(sourceId) : null;
            if (event == null && StrUtil.isNotBlank(sourceNo)) {
                event = qmsExceptionEventMapper.selectByExceptionNo(sourceNo);
            }
            if (event == null) {
                throw exception(QMS_8D_SOURCE_NOT_EXISTS);
            }
            return new SourceInfo(event.getId(), event.getExceptionNo(), event.getDescription(), event.getStatus());
        }
        if ("NCR".equals(sourceType)) {
            QmsNcRecordDO ncr = sourceId != null ? qmsNcRecordMapper.selectById(sourceId) : null;
            if (ncr == null && StrUtil.isNotBlank(sourceNo)) {
                ncr = qmsNcRecordMapper.selectByNcNo(sourceNo);
            }
            if (ncr == null) {
                throw exception(QMS_8D_SOURCE_NOT_EXISTS);
            }
            return new SourceInfo(ncr.getId(), ncr.getNcNo(), firstNotBlank(ncr.getNcDescription(), ncr.getRemark()), ncr.getStatus());
        }
        return new SourceInfo(sourceId, sourceNo, sourceName, sourceStatus);
    }

    private Qms8dReportRespVO buildReportResp(Qms8dReportDO report, boolean detail) {
        Qms8dReportRespVO respVO = BeanUtils.toBean(report, Qms8dReportRespVO.class);
        if (!detail) {
            return respVO;
        }
        respVO.setTeamMembers(BeanUtils.toBean(qms8dTeamMemberMapper.selectListByReportId(report.getId()),
                Qms8dReportRespVO.TeamMember.class));
        respVO.setRelations(BeanUtils.toBean(qms8dRelationMapper.selectListByReportId(report.getId()),
                Qms8dReportRespVO.Relation.class));
        respVO.setActionItems(BeanUtils.toBean(qms8dActionItemMapper.selectListByReportId(report.getId()),
                Qms8dReportRespVO.ActionItem.class));
        respVO.setFlowLogs(getFlowLogList(report.getId()));
        return respVO;
    }

    private void writeFlowLog(Qms8dReportDO before, Qms8dReportDO after, String actionCode,
                              String actionName, String opinion, Map<String, Object> snapshot) {
        qms8dFlowLogMapper.insert(Qms8dFlowLogDO.builder()
                .reportId(after.getId())
                .reportNo(after.getReportNo())
                .actionCode(actionCode)
                .actionName(actionName)
                .fromStatus(before == null ? null : before.getStatus())
                .toStatus(after.getStatus())
                .fromStep(before == null ? null : before.getCurrentStep())
                .toStep(after.getCurrentStep())
                .fromNodeCode(before == null ? null : before.getCurrentNodeCode())
                .fromNodeName(before == null ? null : before.getCurrentNodeName())
                .toNodeCode(after.getCurrentNodeCode())
                .toNodeName(after.getCurrentNodeName())
                .opinion(opinion)
                .handlerUserId(SecurityFrameworkUtils.getLoginUserId())
                .handlerUserName(currentUserName())
                .handleTime(LocalDateTime.now())
                .businessSnapshot(snapshot)
                .build());
    }

    private Set<Long> buildParticipatedReportIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        qms8dFlowLogMapper.selectListByHandlerUserId(loginUserId)
                .forEach(log -> ids.add(log.getReportId()));
        qms8dTeamMemberMapper.selectListByUserId(loginUserId)
                .forEach(member -> ids.add(member.getReportId()));
        qms8dActionItemMapper.selectListByOwnerUserId(loginUserId)
                .forEach(item -> ids.add(item.getReportId()));
        return ids;
    }

    private void applyNode(Qms8dReportDO entity, String step) {
        if (STEP_D1_D2.equals(step)) {
            entity.setCurrentNodeCode("D1_D2_DEFINE");
            entity.setCurrentNodeName("D1-D2团队与问题定义");
        } else if (STEP_D3.equals(step)) {
            entity.setCurrentNodeCode("D3_CONTAINMENT");
            entity.setCurrentNodeName("D3临时围堵措施");
        } else if (STEP_D4.equals(step)) {
            entity.setCurrentNodeCode("D4_ROOT_CAUSE");
            entity.setCurrentNodeName("D4根本原因分析");
        } else if (STEP_D5_D6.equals(step)) {
            entity.setCurrentNodeCode("D5_D6_CAPA");
            entity.setCurrentNodeName("D5-D6永久对策执行");
        } else if (STEP_D7_D8.equals(step)) {
            entity.setCurrentNodeCode("D7_D8_STANDARDIZE");
            entity.setCurrentNodeName("D7-D8标准化与结案");
        } else if (STEP_CLOSED.equals(step)) {
            entity.setCurrentNodeCode("CLOSED");
            entity.setCurrentNodeName("流程结束(归档)");
        }
    }

    private String nextStep(String currentStep) {
        if (STEP_D1_D2.equals(currentStep)) {
            return STEP_D3;
        }
        if (STEP_D3.equals(currentStep)) {
            return STEP_D4;
        }
        if (STEP_D4.equals(currentStep)) {
            return STEP_D5_D6;
        }
        if (STEP_D5_D6.equals(currentStep)) {
            return STEP_D7_D8;
        }
        return STEP_D7_D8;
    }

    private boolean isLeader(Long reportId, Long userId) {
        if (userId == null) {
            return false;
        }
        return qms8dTeamMemberMapper.selectListByReportId(reportId).stream()
                .anyMatch(member -> "LEADER".equals(member.getMemberRole()) && userId.equals(member.getUserId()));
    }

    private Long findLeaderUserId(List<Qms8dTeamMemberReqVO> teamMembers) {
        if (CollUtil.isEmpty(teamMembers)) {
            return null;
        }
        return teamMembers.stream()
                .filter(member -> "LEADER".equals(member.getMemberRole()))
                .map(Qms8dTeamMemberReqVO::getUserId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    private String findLeaderUserName(List<Qms8dTeamMemberReqVO> teamMembers) {
        if (CollUtil.isEmpty(teamMembers)) {
            return null;
        }
        return teamMembers.stream()
                .filter(member -> "LEADER".equals(member.getMemberRole()))
                .map(Qms8dTeamMemberReqVO::getUserName)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private String generate8dNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        for (int i = 0; i < 5; i++) {
            String no = String.format("8D-%s-%06d", datePart, (System.currentTimeMillis() + i) % 1_000_000);
            if (qms8dReportMapper.selectByReportNo(no) == null) {
                return no;
            }
        }
        return "8D-" + datePart + "-" + System.nanoTime();
    }

    private Map<String, Object> snapshot(Object... keyValues) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            snapshot.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return snapshot;
    }

    private String currentUserName() {
        return StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static class SourceInfo {
        private final Long sourceId;
        private final String sourceNo;
        private final String sourceName;
        private final String sourceStatus;

        private SourceInfo(Long sourceId, String sourceNo, String sourceName, String sourceStatus) {
            this.sourceId = sourceId;
            this.sourceNo = sourceNo;
            this.sourceName = sourceName;
            this.sourceStatus = sourceStatus;
        }
    }
}
