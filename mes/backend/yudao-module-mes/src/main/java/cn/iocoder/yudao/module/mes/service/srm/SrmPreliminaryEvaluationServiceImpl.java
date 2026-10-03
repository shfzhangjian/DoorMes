package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationCcDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryProjectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryProjectScorerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryEvaluationCcMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryEvaluationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryEvaluationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryEvaluationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryProjectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryProjectScorerMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_BPM_TASK_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_GM_ROLE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_SCORER_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_SCORER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_SCORE_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_SCORE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_SCORE_LINE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_EVALUATION_TOTAL_SCORE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_PROJECT_NOT_EXISTS;

@Service
@Validated
public class SrmPreliminaryEvaluationServiceImpl implements SrmPreliminaryEvaluationService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SCORING = "SCORING";
    private static final String STATUS_PENDING_CALCULATION = "PENDING_CALCULATION";
    private static final String STATUS_PENDING_DECISION = "PENDING_DECISION";
    private static final String STATUS_GM_REVIEW = "GM_REVIEW";
    private static final String STATUS_PENDING_PUBLISH = "PENDING_PUBLISH";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String SCORE_PENDING = "PENDING";
    private static final String SCORE_COMPLETED = "COMPLETED";
    private static final String DECISION_QUALIFIED = "QUALIFIED";
    private static final String DECISION_UNQUALIFIED = "UNQUALIFIED";
    private static final String GENERAL_MANAGER_ROLE = "srm_general_manager";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String BPM_PROCESS_KEY = "srm_preliminary_evaluation";
    private static final String BPM_MODEL_ID = "srm-preliminary-evaluation-model";
    private static final String BPM_NODE_START = "StartUserNode";
    private static final String BPM_NODE_SCORING = "scoring";
    private static final String BPM_NODE_DECISION = "initiator_decision";
    private static final String BPM_NODE_GM = "general_manager_review";
    private static final String BPM_NODE_PUBLISH = "publish";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1_009_003_002;
    @Resource
    private SrmPreliminaryEvaluationMapper evaluationMapper;
    @Resource
    private SrmPreliminaryEvaluationItemMapper evaluationItemMapper;
    @Resource
    private SrmPreliminaryEvaluationCcMapper ccMapper;
    @Resource
    private SrmPreliminaryEvaluationLogMapper logMapper;
    @Resource
    private SrmEvaluationTemplateMapper templateMapper;
    @Resource
    private SrmEvaluationTemplateVersionMapper templateVersionMapper;
    @Resource
    private SrmEvaluationTemplateItemMapper templateItemMapper;
    @Resource
    private SrmPreliminaryProjectMapper projectMapper;
    @Resource
    private SrmPreliminaryProjectScorerMapper projectScorerMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private BpmModelService bpmModelService;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private BpmProcessInstanceCopyService bpmProcessInstanceCopyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvaluation(SrmPreliminaryEvaluationSaveReqVO reqVO) {
        if (evaluationMapper.selectByEvaluationNo(reqVO.getEvaluationNo()) != null) {
            throw exception(SRM_PRELIMINARY_EVALUATION_NO_EXISTS);
        }
        TemplateSnapshot template = validatePublishedTemplate(reqVO.getTemplateVersionId());
        UserSnapshot user = currentUser();
        SrmPreliminaryEvaluationDO evaluation = new SrmPreliminaryEvaluationDO();
        copyEditableFields(reqVO, evaluation);
        applyProjectSnapshot(reqVO.getProjectId(), evaluation);
        applyTemplateSnapshot(evaluation, template);
        applyEditableScoreLine(reqVO, evaluation);
        evaluation.setStatus(STATUS_DRAFT);
        evaluation.setInitiatorId(user.id());
        evaluation.setInitiatorName(user.name());
        evaluation.setVetoTriggered(Boolean.FALSE);
        evaluation.setVersion(0);
        evaluationMapper.insert(evaluation);
        createEvaluationItems(evaluation, template.items(),
                buildScorerContext(reqVO.getItems(), template.items(), evaluation.getProjectId(),
                        template.version().getId()));
        writeLog(evaluation.getId(), "CREATE", null, STATUS_DRAFT, "创建初评单并引用模板快照",
                Map.of("projectId", evaluation.getProjectId(), "templateVersionId", template.version().getId()));
        return evaluation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvaluation(SrmPreliminaryEvaluationSaveReqVO reqVO) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(reqVO.getId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        SrmPreliminaryEvaluationDO sameNo = evaluationMapper.selectByEvaluationNo(reqVO.getEvaluationNo());
        if (sameNo != null && !Objects.equals(sameNo.getId(), evaluation.getId())) {
            throw exception(SRM_PRELIMINARY_EVALUATION_NO_EXISTS);
        }
        Long oldTemplateVersionId = evaluation.getTemplateVersionId();
        Long oldProjectId = evaluation.getProjectId();
        copyEditableFields(reqVO, evaluation);
        applyProjectSnapshot(reqVO.getProjectId(), evaluation);
        if (!Objects.equals(oldTemplateVersionId, reqVO.getTemplateVersionId())) {
            TemplateSnapshot template = validatePublishedTemplate(reqVO.getTemplateVersionId());
            applyTemplateSnapshot(evaluation, template);
            evaluationItemMapper.deleteByEvaluationId(evaluation.getId());
            createEvaluationItems(evaluation, template.items(),
                    buildScorerContext(reqVO.getItems(), template.items(), evaluation.getProjectId(),
                            template.version().getId()));
        } else if (!Objects.equals(oldProjectId, evaluation.getProjectId())) {
            TemplateSnapshot template = validatePublishedTemplate(evaluation.getTemplateVersionId());
            evaluationItemMapper.deleteByEvaluationId(evaluation.getId());
            createEvaluationItems(evaluation, template.items(),
                    buildScorerContext(reqVO.getItems(), template.items(), evaluation.getProjectId(),
                            template.version().getId()));
        } else {
            applyDraftItemScorers(evaluation, reqVO.getItems());
        }
        applyEditableScoreLine(reqVO, evaluation);
        evaluationMapper.updateById(evaluation);
        writeLog(evaluation.getId(), "UPDATE", STATUS_DRAFT, STATUS_DRAFT, "更新初评单基本信息",
                Map.of("projectId", evaluation.getProjectId(), "templateVersionId", evaluation.getTemplateVersionId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvaluation(Long id) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        evaluationItemMapper.deleteByEvaluationId(id);
        ccMapper.deleteByEvaluationId(id);
        evaluationMapper.deleteById(id);
    }

    @Override
    public SrmPreliminaryEvaluationRespVO getEvaluation(Long id) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(id);
        return buildResp(evaluation, true);
    }

    @Override
    public PageResult<SrmPreliminaryEvaluationRespVO> getEvaluationPage(SrmPreliminaryEvaluationPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getTodoOnly())) {
            Long userId = currentUser().id();
            Set<Long> ids = evaluationItemMapper.selectListByScorerId(userId, SCORE_PENDING).stream()
                    .map(SrmPreliminaryEvaluationItemDO::getEvaluationId).collect(Collectors.toSet());
            reqVO.setVisibleEvaluationIds(ids);
            if (StrUtil.isBlank(reqVO.getStatus())) {
                reqVO.setStatus(STATUS_SCORING);
            }
        }
        PageResult<SrmPreliminaryEvaluationDO> page = evaluationMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignScorers(SrmPreliminaryEvaluationActionReqVO.AssignScorer reqVO) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        List<SrmPreliminaryEvaluationItemDO> items = evaluationItemMapper.selectListByEvaluationId(evaluation.getId());
        Map<Long, SrmPreliminaryEvaluationItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(SrmPreliminaryEvaluationItemDO::getId, item -> item));
        Set<Long> userIds = reqVO.getAssignments().stream()
                .map(SrmPreliminaryEvaluationActionReqVO.Assignment::getScorerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        reqVO.getAssignments().stream()
                .map(SrmPreliminaryEvaluationActionReqVO.Assignment::getScorerCandidateUserIds)
                .filter(CollUtil::isNotEmpty)
                .forEach(ids -> ids.stream().filter(Objects::nonNull).forEach(userIds::add));
        adminUserApi.validateUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = userMap.values().stream().map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Map.of() : deptApi.getDeptMap(deptIds);
        for (SrmPreliminaryEvaluationActionReqVO.Assignment assignment : reqVO.getAssignments()) {
            SrmPreliminaryEvaluationItemDO item = itemMap.get(assignment.getItemId());
            if (item == null) {
                throw exception(SRM_PRELIMINARY_EVALUATION_NOT_EXISTS);
            }
            AdminUserRespDTO user = userMap.get(assignment.getScorerUserId());
            String scorerName = userName(user);
            String deptName = resolveDeptName(user, deptMap);
            List<Long> candidateUserIds = mergeUserIds(parseLongCsv(item.getScorerCandidateUserIds()),
                    assignment.getScorerCandidateUserIds(), assignment.getScorerUserId());
            String candidateUserNames = resolveUserNames(candidateUserIds, userMap);
            item.setScorerUserId(assignment.getScorerUserId());
            item.setScorerUserName(scorerName);
            item.setScorerCandidateUserIds(joinIds(candidateUserIds));
            item.setScorerCandidateUserNames(candidateUserNames);
            if (StrUtil.isNotBlank(deptName)) {
                item.setDefaultDeptNamesSnapshot(deptName);
            }
            item.setScoreStatus(SCORE_PENDING);
            item.setActualScore(null);
            item.setScoringDescription(null);
            item.setActualScoreTime(null);
            evaluationItemMapper.updateById(item);
            saveDefaultScorer(evaluation, item, assignment.getScorerUserId(), scorerName, deptName, candidateUserIds,
                    candidateUserNames);
        }
        writeLog(evaluation.getId(), "ASSIGN_SCORER", STATUS_DRAFT, STATUS_DRAFT, "维护指标评分人",
                Map.of("scorerUserIds", userIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendForScoring(Long id) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        List<SrmPreliminaryEvaluationItemDO> items = evaluationItemMapper.selectListByEvaluationId(id);
        if (CollUtil.isEmpty(items) || items.stream().anyMatch(item -> item.getScorerUserId() == null)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_SCORER_REQUIRED);
        }
        List<Long> scorerUserIds = items.stream().map(SrmPreliminaryEvaluationItemDO::getScorerUserId)
                .filter(Objects::nonNull).distinct().toList();
        adminUserApi.validateUserList(scorerUserIds);
        String processInstanceId = startBpmProcess(evaluation, scorerUserIds);
        evaluation.setProcessInstanceId(processInstanceId);
        evaluation.setStatus(STATUS_SCORING);
        evaluation.setSendTime(LocalDateTime.now());
        evaluationMapper.updateById(evaluation);
        approveBpmTask(evaluation, BPM_NODE_START, evaluation.getInitiatorId(), "确认发送评分",
                nextAssignees(BPM_NODE_SCORING, scorerUserIds));
        writeLog(evaluation.getId(), "SEND", STATUS_DRAFT, STATUS_SCORING, "确认发送，进入分人评分",
                Map.of("scorerUserIds", scorerUserIds, "processInstanceId", processInstanceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScore(SrmPreliminaryEvaluationActionReqVO.Score reqVO) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertStatus(evaluation, STATUS_SCORING);
        Long userId = currentUser().id();
        List<SrmPreliminaryEvaluationItemDO> allItems = evaluationItemMapper.selectListByEvaluationId(evaluation.getId());
        List<SrmPreliminaryEvaluationItemDO> myItems = allItems.stream()
                .filter(item -> Objects.equals(item.getScorerUserId(), userId)).toList();
        if (CollUtil.isEmpty(myItems)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_SCORER_ONLY);
        }
        Map<Long, SrmPreliminaryEvaluationItemDO> myItemMap = myItems.stream()
                .collect(Collectors.toMap(SrmPreliminaryEvaluationItemDO::getId, item -> item));
        Set<Long> submittedIds = reqVO.getItems().stream()
                .map(SrmPreliminaryEvaluationActionReqVO.ScoreItem::getItemId).collect(Collectors.toSet());
        Set<Long> requiredIds = myItems.stream().map(SrmPreliminaryEvaluationItemDO::getId).collect(Collectors.toSet());
        if (!submittedIds.equals(requiredIds)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_SCORE_INCOMPLETE);
        }
        LocalDateTime now = LocalDateTime.now();
        for (SrmPreliminaryEvaluationActionReqVO.ScoreItem scoreItem : reqVO.getItems()) {
            SrmPreliminaryEvaluationItemDO item = myItemMap.get(scoreItem.getItemId());
            if (item == null) {
                throw exception(SRM_PRELIMINARY_EVALUATION_SCORER_ONLY);
            }
            if (scoreItem.getActualScore().compareTo(item.getMaxScoreSnapshot()) > 0) {
                throw exception(SRM_PRELIMINARY_EVALUATION_SCORE_INVALID, item.getIndicatorNameSnapshot());
            }
            item.setActualScore(scoreItem.getActualScore());
            item.setScoringDescription(StrUtil.trim(scoreItem.getScoringDescription()));
            item.setActualScoreTime(now);
            item.setScoreStatus(SCORE_COMPLETED);
            evaluationItemMapper.updateById(item);
        }
        List<SrmPreliminaryEvaluationItemDO> refreshed = evaluationItemMapper.selectListByEvaluationId(evaluation.getId());
        boolean allScored = refreshed.stream().allMatch(item -> SCORE_COMPLETED.equals(item.getScoreStatus()));
        if (allScored) {
            evaluation.setStatus(STATUS_PENDING_CALCULATION);
            evaluation.setAllScoredTime(now);
            evaluationMapper.updateById(evaluation);
        }
        approveBpmTask(evaluation, BPM_NODE_SCORING, userId, "提交实际得分",
                nextAssignees(BPM_NODE_DECISION, List.of(evaluation.getInitiatorId())));
        if (allScored) {
            writeLog(evaluation.getId(), "ALL_SCORED", STATUS_SCORING, STATUS_PENDING_CALCULATION,
                    "全部评分人已完成，推送发起人计算得分", null);
        } else {
            writeLog(evaluation.getId(), "SCORE", STATUS_SCORING, STATUS_SCORING, "评分人完成本人评分", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateScore(Long id) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_PENDING_CALCULATION);
        List<SrmPreliminaryEvaluationItemDO> items = evaluationItemMapper.selectListByEvaluationId(id);
        if (items.stream().anyMatch(item -> !SCORE_COMPLETED.equals(item.getScoreStatus()) || item.getActualScore() == null)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_SCORE_INCOMPLETE);
        }
        BigDecimal total = items.stream().map(SrmPreliminaryEvaluationItemDO::getActualScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, List<SrmPreliminaryEvaluationItemDO>> groups = items.stream()
                .collect(Collectors.groupingBy(SrmPreliminaryEvaluationItemDO::getGroupCodeSnapshot,
                        LinkedHashMap::new, Collectors.toList()));
        List<String> vetoReasons = new ArrayList<>();
        for (List<SrmPreliminaryEvaluationItemDO> groupItems : groups.values()) {
            SrmPreliminaryEvaluationItemDO first = groupItems.get(0);
            if (StrUtil.isBlank(first.getVetoOperatorSnapshot()) || first.getVetoScoreSnapshot() == null) {
                continue;
            }
            BigDecimal groupScore = groupItems.stream().map(SrmPreliminaryEvaluationItemDO::getActualScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (matchesVeto(groupScore, first.getVetoOperatorSnapshot(), first.getVetoScoreSnapshot())) {
                vetoReasons.add(first.getGroupNameSnapshot() + "得分" + decimalText(groupScore)
                        + "触发否决线" + operatorText(first.getVetoOperatorSnapshot())
                        + decimalText(first.getVetoScoreSnapshot()));
            }
        }
        boolean vetoTriggered = !vetoReasons.isEmpty();
        String autoDecision = vetoTriggered || total.compareTo(evaluation.getQualificationScoreSnapshot()) < 0
                ? DECISION_UNQUALIFIED : DECISION_QUALIFIED;
        evaluation.setTotalScore(total);
        evaluation.setVetoTriggered(vetoTriggered);
        evaluation.setVetoDescription(String.join("；", vetoReasons));
        evaluation.setAutoDecision(autoDecision);
        evaluation.setCalculatedTime(LocalDateTime.now());
        evaluation.setStatus(STATUS_PENDING_DECISION);
        evaluationMapper.updateById(evaluation);
        writeLog(evaluation.getId(), "CALCULATE", STATUS_PENDING_CALCULATION, STATUS_PENDING_DECISION,
                "计算得分：" + decimalText(total) + "，自动判定：" + autoDecision,
                Map.of("totalScore", total, "vetoTriggered", vetoTriggered, "vetoReasons", vetoReasons));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDecision(SrmPreliminaryEvaluationActionReqVO.Decision reqVO) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_PENDING_DECISION);
        if (!(DECISION_QUALIFIED.equals(reqVO.getFinalDecision())
                || DECISION_UNQUALIFIED.equals(reqVO.getFinalDecision()))) {
            throw exception(SRM_PRELIMINARY_EVALUATION_STATUS_INVALID);
        }
        validateFinalTotalScore(reqVO.getTotalScore(), evaluation);
        evaluation.setTotalScore(reqVO.getTotalScore());
        evaluation.setFinalDecision(reqVO.getFinalDecision());
        evaluation.setFinalDescription(StrUtil.trim(reqVO.getFinalDescription()));
        boolean needGeneralManager = reqVO.getGeneralManagerUserId() != null;
        Map<String, Object> variables = buildBpmVariables(evaluation);
        variables.put("needGeneralManager", needGeneralManager);
        bpmProcessInstanceApi.updateProcessInstanceVariables(evaluation.getProcessInstanceId(), variables);
        if (needGeneralManager) {
            if (!permissionApi.hasAnyRoles(reqVO.getGeneralManagerUserId(), GENERAL_MANAGER_ROLE)) {
                throw exception(SRM_PRELIMINARY_EVALUATION_GM_ROLE_REQUIRED);
            }
            AdminUserRespDTO gm = adminUserApi.getUser(reqVO.getGeneralManagerUserId());
            evaluation.setGeneralManagerUserId(reqVO.getGeneralManagerUserId());
            evaluation.setGeneralManagerUserName(gm == null ? null : gm.getNickname());
            evaluation.setStatus(STATUS_GM_REVIEW);
            evaluationMapper.updateById(evaluation);
            approveBpmTask(evaluation, BPM_NODE_DECISION, evaluation.getInitiatorId(), "提交最终判定并送总经理",
                    nextAssignees(BPM_NODE_GM, List.of(reqVO.getGeneralManagerUserId())));
            writeLog(evaluation.getId(), "SEND_GM", STATUS_PENDING_DECISION, STATUS_GM_REVIEW,
                    "最终判定已送总经理办理，最终得分：" + decimalText(evaluation.getTotalScore()),
                    Map.of("generalManagerUserId", reqVO.getGeneralManagerUserId(), "totalScore", evaluation.getTotalScore()));
        } else {
            evaluation.setStatus(STATUS_PENDING_PUBLISH);
            evaluationMapper.updateById(evaluation);
            approveBpmTask(evaluation, BPM_NODE_DECISION, evaluation.getInitiatorId(), "提交最终判定，直接进入发布",
                    nextAssignees(BPM_NODE_PUBLISH, List.of(evaluation.getInitiatorId())));
            writeLog(evaluation.getId(), "DECISION", STATUS_PENDING_DECISION, STATUS_PENDING_PUBLISH,
                    "最终判定已完成，最终得分：" + decimalText(evaluation.getTotalScore()) + "，待发布",
                    Map.of("totalScore", evaluation.getTotalScore()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitGeneralManagerOpinion(SrmPreliminaryEvaluationActionReqVO.GeneralManagerOpinion reqVO) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertStatus(evaluation, STATUS_GM_REVIEW);
        Long userId = currentUser().id();
        if (!Objects.equals(evaluation.getGeneralManagerUserId(), userId)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_OPERATOR_ONLY);
        }
        evaluation.setGeneralManagerOpinion(StrUtil.trim(reqVO.getOpinion()));
        evaluation.setGeneralManagerHandleTime(LocalDateTime.now());
        evaluation.setStatus(STATUS_PENDING_PUBLISH);
        evaluationMapper.updateById(evaluation);
        approveBpmTask(evaluation, BPM_NODE_GM, userId, "填写总经理意见",
                nextAssignees(BPM_NODE_PUBLISH, List.of(evaluation.getInitiatorId())));
        writeLog(evaluation.getId(), "GM_OPINION", STATUS_GM_REVIEW, STATUS_PENDING_PUBLISH,
                "总经理已办理，返回发起人发布", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(SrmPreliminaryEvaluationActionReqVO.Publish reqVO) {
        SrmPreliminaryEvaluationDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_PENDING_PUBLISH);
        List<Long> ccUserIds = reqVO.getCcUserIds() == null ? List.of()
                : reqVO.getCcUserIds().stream().filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isNotEmpty(ccUserIds)) {
            adminUserApi.validateUserList(ccUserIds);
        }
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(ccUserIds);
        ccMapper.deleteByEvaluationId(evaluation.getId());
        for (Long ccUserId : ccUserIds) {
            SrmPreliminaryEvaluationCcDO cc = new SrmPreliminaryEvaluationCcDO();
            cc.setEvaluationId(evaluation.getId());
            cc.setUserId(ccUserId);
            AdminUserRespDTO user = users.get(ccUserId);
            cc.setUserName(user == null ? String.valueOf(ccUserId) : user.getNickname());
            ccMapper.insert(cc);
        }
        UserSnapshot publisher = currentUser();
        evaluation.setPublisherId(publisher.id());
        evaluation.setPublisherName(publisher.name());
        evaluation.setPublishTime(LocalDateTime.now());
        evaluation.setStatus(STATUS_PUBLISHED);
        evaluationMapper.updateById(evaluation);

        Task publishTask = findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_PUBLISH, evaluation.getInitiatorId());
        if (publishTask != null && CollUtil.isNotEmpty(ccUserIds)) {
            bpmProcessInstanceCopyService.createProcessInstanceCopy(ccUserIds, "抄送已发布的供应商选择初评表",
                    evaluation.getProcessInstanceId(), BPM_NODE_PUBLISH, "发布评估表", publishTask.getId());
        }
        approveBpmTask(evaluation, BPM_NODE_PUBLISH, evaluation.getInitiatorId(), "发布评估表", null);
        writeLog(evaluation.getId(), "PUBLISH", STATUS_PENDING_PUBLISH, STATUS_PUBLISHED,
                "发布评估表并抄送 " + ccUserIds.size() + " 人", Map.of("ccUserIds", ccUserIds));
    }

    private SrmPreliminaryEvaluationRespVO buildResp(SrmPreliminaryEvaluationDO evaluation, boolean includeDetail) {
        Long userId = currentUser().id();
        boolean admin = isEvaluationAdmin(userId);
        boolean initiator = Objects.equals(evaluation.getInitiatorId(), userId);
        boolean generalManager = Objects.equals(evaluation.getGeneralManagerUserId(), userId);
        boolean copied = STATUS_PUBLISHED.equals(evaluation.getStatus())
                && ccMapper.selectByEvaluationIdAndUserId(evaluation.getId(), userId) != null;
        boolean fullVisible = admin || initiator || generalManager || copied;
        SrmPreliminaryEvaluationRespVO resp = BeanUtils.toBean(evaluation, SrmPreliminaryEvaluationRespVO.class);
        resp.setViewerScope(admin ? "ADMIN" : initiator ? "INITIATOR" : generalManager ? "GENERAL_MANAGER"
                : copied ? "CC" : "OTHER");
        resp.setCanMaintain(STATUS_DRAFT.equals(evaluation.getStatus()) && (admin || initiator));
        resp.setCanCalculate(STATUS_PENDING_CALCULATION.equals(evaluation.getStatus()) && (admin || initiator));
        resp.setCanHandleGeneralManager(STATUS_GM_REVIEW.equals(evaluation.getStatus()) && generalManager);
        resp.setCanPublish(STATUS_PENDING_PUBLISH.equals(evaluation.getStatus()) && (admin || initiator));
        applyHeaderMask(resp, fullVisible);
        if (!includeDetail) {
            resp.setCanScore(evaluationItemMapper.selectListByEvaluationId(evaluation.getId()).stream()
                    .anyMatch(item -> Objects.equals(item.getScorerUserId(), userId)
                            && SCORE_PENDING.equals(item.getScoreStatus()) && STATUS_SCORING.equals(evaluation.getStatus())));
            return resp;
        }
        List<SrmPreliminaryEvaluationItemDO> itemDOs = evaluationItemMapper.selectListByEvaluationId(evaluation.getId());
        resp.setCanScore(STATUS_SCORING.equals(evaluation.getStatus()) && itemDOs.stream()
                .anyMatch(item -> Objects.equals(item.getScorerUserId(), userId) && SCORE_PENDING.equals(item.getScoreStatus())));
        List<SrmPreliminaryEvaluationRespVO.Item> items = new ArrayList<>();
        for (SrmPreliminaryEvaluationItemDO itemDO : itemDOs) {
            SrmPreliminaryEvaluationRespVO.Item item = BeanUtils.toBean(itemDO, SrmPreliminaryEvaluationRespVO.Item.class);
            boolean ownItem = Objects.equals(itemDO.getScorerUserId(), userId);
            item.setCurrentUserItem(ownItem);
            applyItemMask(item, fullVisible || ownItem);
            items.add(item);
        }
        resp.setItems(items);
        resp.setCcUsers(BeanUtils.toBean(ccMapper.selectListByEvaluationId(evaluation.getId()),
                SrmPreliminaryEvaluationRespVO.Cc.class));
        List<SrmPreliminaryEvaluationRespVO.Log> logs = BeanUtils.toBean(
                logMapper.selectListByEvaluationId(evaluation.getId()), SrmPreliminaryEvaluationRespVO.Log.class);
        if (!fullVisible) {
            for (SrmPreliminaryEvaluationRespVO.Log log : logs) {
                if (("SCORE".equals(log.getAction()) || "ALL_SCORED".equals(log.getAction()))
                        && !Objects.equals(log.getOperatorId(), userId)) {
                    log.setOperatorId(null);
                    log.setOperatorName("*");
                }
            }
        }
        resp.setLogs(logs);
        return resp;
    }

    private void applyHeaderMask(SrmPreliminaryEvaluationRespVO resp, boolean visible) {
        if (visible) {
            resp.setTotalScoreDisplay(decimalText(resp.getTotalScore()));
            resp.setGeneralManagerOpinionDisplay(resp.getGeneralManagerOpinion());
            return;
        }
        resp.setTotalScore(null);
        resp.setTotalScoreDisplay("*");
        resp.setGeneralManagerOpinion(null);
        resp.setGeneralManagerOpinionDisplay("*");
    }

    private void applyItemMask(SrmPreliminaryEvaluationRespVO.Item item, boolean visible) {
        if (visible) {
            item.setScorerUserNameDisplay(item.getScorerUserName());
            item.setActualScoreDisplay(decimalText(item.getActualScore()));
            item.setScoringDescriptionDisplay(item.getScoringDescription());
            return;
        }
        item.setScorerUserId(null);
        item.setScorerUserName(null);
        item.setScorerCandidateUserIds(null);
        item.setScorerCandidateUserNames("*");
        item.setScorerUserNameDisplay("*");
        item.setActualScore(null);
        item.setActualScoreDisplay("*");
        item.setScoringDescription(null);
        item.setScoringDescriptionDisplay("*");
        item.setActualScoreTime(null);
    }

    private TemplateSnapshot validatePublishedTemplate(Long versionId) {
        SrmEvaluationTemplateVersionDO version = versionId == null ? null : templateVersionMapper.selectById(versionId);
        if (version == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS);
        }
        if (!"PUBLISHED".equals(version.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        SrmEvaluationTemplateDO template = templateMapper.selectById(version.getTemplateId());
        if (template == null || !"ENABLED".equals(template.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        List<SrmEvaluationTemplateItemDO> items = templateItemMapper.selectListByVersionId(versionId);
        if (CollUtil.isEmpty(items)) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        return new TemplateSnapshot(template, version, items);
    }

    private void createEvaluationItems(SrmPreliminaryEvaluationDO evaluation, List<SrmEvaluationTemplateItemDO> templateItems,
                                       ScorerContext scorerContext) {
        for (SrmEvaluationTemplateItemDO templateItem : templateItems) {
            SrmPreliminaryEvaluationItemDO item = new SrmPreliminaryEvaluationItemDO();
            item.setEvaluationId(evaluation.getId());
            item.setTemplateItemId(templateItem.getId());
            item.setGroupCodeSnapshot(templateItem.getGroupCode());
            item.setGroupNameSnapshot(templateItem.getGroupName());
            item.setGroupSort(templateItem.getGroupSort());
            item.setGroupMaxScoreSnapshot(templateItem.getGroupMaxScore());
            item.setVetoOperatorSnapshot(templateItem.getVetoOperator());
            item.setVetoScoreSnapshot(templateItem.getVetoScore());
            item.setIndicatorCodeSnapshot(templateItem.getIndicatorCode());
            item.setIndicatorNameSnapshot(templateItem.getIndicatorName());
            item.setIndicatorSort(templateItem.getIndicatorSort());
            item.setScoringRuleSnapshot(templateItem.getScoringRule());
            item.setMaxScoreSnapshot(templateItem.getMaxScore());
            SrmPreliminaryProjectScorerDO projectScorer = scorerContext.projectScorerByTemplateItemId()
                    .get(templateItem.getId());
            item.setDefaultDeptNamesSnapshot(projectScorer != null && StrUtil.isNotBlank(projectScorer.getDefaultDeptNames())
                    ? projectScorer.getDefaultDeptNames() : templateItem.getDefaultDeptNames());
            item.setAttachmentRequiredSnapshot(Boolean.TRUE.equals(templateItem.getAttachmentRequired()));
            SrmPreliminaryEvaluationSaveReqVO.Item requestedItem = scorerContext.requestedByTemplateItemId()
                    .get(templateItem.getId());
            Long selectedUserId = positiveUserId(requestedItem == null ? null : requestedItem.getScorerUserId());
            if (selectedUserId == null && projectScorer != null) {
                selectedUserId = positiveUserId(projectScorer.getScorerUserId());
            }
            if (selectedUserId == null) {
                selectedUserId = positiveUserId(templateItem.getDefaultScorerUserId());
            }
            List<Long> baseCandidateUserIds = projectScorer == null
                    ? parseLongCsv(templateItem.getDefaultScorerUserIds())
                    : parseLongCsv(projectScorer.getScorerCandidateUserIds());
            List<Long> candidateUserIds = mergeUserIds(baseCandidateUserIds,
                    requestedItem == null ? null : parseLongCsv(requestedItem.getScorerCandidateUserIds()),
                    selectedUserId);
            applyScorerFields(item, selectedUserId, candidateUserIds, scorerContext);
            item.setScoreStatus(SCORE_PENDING);
            evaluationItemMapper.insert(item);
            if (item.getScorerUserId() != null) {
                saveDefaultScorer(evaluation, item, item.getScorerUserId(), item.getScorerUserName(),
                        item.getDefaultDeptNamesSnapshot(), candidateUserIds, item.getScorerCandidateUserNames());
            }
        }
    }

    private void applyTemplateSnapshot(SrmPreliminaryEvaluationDO evaluation, TemplateSnapshot template) {
        evaluation.setTemplateId(template.template().getId());
        evaluation.setTemplateVersionId(template.version().getId());
        evaluation.setTemplateCodeSnapshot(template.template().getTemplateCode());
        evaluation.setTemplateNameSnapshot(template.template().getTemplateName());
        evaluation.setTemplateVersionSnapshot(template.version().getVersionNo());
        evaluation.setTotalScoreBaseline(template.version().getTotalScore());
        evaluation.setQualificationScoreSnapshot(template.version().getQualificationScore());
    }

    private void applyProjectSnapshot(Long projectId, SrmPreliminaryEvaluationDO evaluation) {
        SrmPreliminaryProjectDO project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null || !STATUS_ENABLED.equals(project.getStatus())) {
            throw exception(SRM_PRELIMINARY_PROJECT_NOT_EXISTS);
        }
        evaluation.setProjectId(project.getId());
        evaluation.setProjectCode(project.getProjectCode());
        evaluation.setProjectName(project.getProjectName());
    }

    private void applyEditableScoreLine(SrmPreliminaryEvaluationSaveReqVO reqVO, SrmPreliminaryEvaluationDO evaluation) {
        if (reqVO.getTotalScoreBaseline() != null) {
            evaluation.setTotalScoreBaseline(reqVO.getTotalScoreBaseline());
        }
        if (reqVO.getQualificationScoreSnapshot() != null) {
            evaluation.setQualificationScoreSnapshot(reqVO.getQualificationScoreSnapshot());
        }
        if (evaluation.getTotalScoreBaseline() != null && evaluation.getQualificationScoreSnapshot() != null
                && evaluation.getQualificationScoreSnapshot().compareTo(evaluation.getTotalScoreBaseline()) > 0) {
            throw exception(SRM_PRELIMINARY_EVALUATION_SCORE_LINE_INVALID);
        }
    }

    private void validateFinalTotalScore(BigDecimal totalScore, SrmPreliminaryEvaluationDO evaluation) {
        if (totalScore == null || totalScore.compareTo(BigDecimal.ZERO) < 0
                || (evaluation.getTotalScoreBaseline() != null
                && totalScore.compareTo(evaluation.getTotalScoreBaseline()) > 0)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_TOTAL_SCORE_INVALID);
        }
    }

    private String resolveDeptName(AdminUserRespDTO user, Map<Long, DeptRespDTO> deptMap) {
        if (user == null || user.getDeptId() == null) {
            return null;
        }
        DeptRespDTO dept = deptMap.get(user.getDeptId());
        return dept == null ? null : dept.getName();
    }

    private void applyDraftItemScorers(SrmPreliminaryEvaluationDO evaluation,
                                       List<SrmPreliminaryEvaluationSaveReqVO.Item> reqItems) {
        if (CollUtil.isEmpty(reqItems)) {
            return;
        }
        List<SrmPreliminaryEvaluationItemDO> items = evaluationItemMapper.selectListByEvaluationId(evaluation.getId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, SrmPreliminaryEvaluationItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(SrmPreliminaryEvaluationItemDO::getId, item -> item));
        ScorerContext scorerContext = buildScorerContext(reqItems, List.of(), evaluation.getProjectId(),
                evaluation.getTemplateVersionId());
        for (SrmPreliminaryEvaluationSaveReqVO.Item reqItem : reqItems) {
            SrmPreliminaryEvaluationItemDO item = itemMap.get(reqItem.getId());
            if (item == null) {
                continue;
            }
            Long selectedUserId = positiveUserId(reqItem.getScorerUserId());
            List<Long> candidateUserIds = mergeUserIds(parseLongCsv(item.getScorerCandidateUserIds()),
                    parseLongCsv(reqItem.getScorerCandidateUserIds()), selectedUserId);
            applyScorerFields(item, selectedUserId, candidateUserIds, scorerContext);
            item.setScoreStatus(SCORE_PENDING);
            item.setActualScore(null);
            item.setScoringDescription(null);
            item.setActualScoreTime(null);
            evaluationItemMapper.updateById(item);
            if (item.getScorerUserId() != null) {
                saveDefaultScorer(evaluation, item, item.getScorerUserId(), item.getScorerUserName(),
                        item.getDefaultDeptNamesSnapshot(), candidateUserIds, item.getScorerCandidateUserNames());
            }
        }
    }

    private void applyScorerFields(SrmPreliminaryEvaluationItemDO item, Long selectedUserId,
                                   List<Long> candidateUserIds, ScorerContext scorerContext) {
        List<Long> validCandidateUserIds = candidateUserIds.stream()
                .filter(id -> scorerContext.userMap().containsKey(id))
                .toList();
        Long validSelectedUserId = selectedUserId != null && scorerContext.userMap().containsKey(selectedUserId)
                ? selectedUserId
                : (CollUtil.isEmpty(validCandidateUserIds) ? null : validCandidateUserIds.get(0));
        AdminUserRespDTO selectedUser = validSelectedUserId == null ? null : scorerContext.userMap().get(validSelectedUserId);
        String scorerName = userName(selectedUser);
        String deptName = resolveDeptName(selectedUser, scorerContext.deptMap());
        item.setScorerCandidateUserIds(joinIds(validCandidateUserIds));
        item.setScorerCandidateUserNames(resolveUserNames(validCandidateUserIds, scorerContext.userMap()));
        item.setScorerUserId(validSelectedUserId);
        item.setScorerUserName(scorerName);
        if (StrUtil.isNotBlank(deptName)) {
            item.setDefaultDeptNamesSnapshot(deptName);
        }
    }

    private void updateTemplateDefaultScorer(SrmPreliminaryEvaluationItemDO item, Long scorerUserId,
                                             String scorerUserName, String deptName, List<Long> candidateUserIds,
                                             String candidateUserNames) {
        if (item.getTemplateItemId() == null) {
            return;
        }
        SrmEvaluationTemplateItemDO templateItem = new SrmEvaluationTemplateItemDO();
        templateItem.setId(item.getTemplateItemId());
        templateItem.setDefaultScorerUserId(scorerUserId);
        templateItem.setDefaultScorerUserName(scorerUserName);
        templateItem.setDefaultScorerUserIds(joinIds(candidateUserIds));
        templateItem.setDefaultScorerUserNames(candidateUserNames);
        if (StrUtil.isNotBlank(deptName)) {
            templateItem.setDefaultDeptNames(deptName);
        }
        templateItemMapper.updateById(templateItem);
    }

    private void saveDefaultScorer(SrmPreliminaryEvaluationDO evaluation, SrmPreliminaryEvaluationItemDO item,
                                   Long scorerUserId, String scorerUserName, String deptName,
                                   List<Long> candidateUserIds, String candidateUserNames) {
        if (evaluation.getProjectId() != null) {
            updateProjectDefaultScorer(evaluation, item, scorerUserId, scorerUserName, deptName, candidateUserIds,
                    candidateUserNames);
            return;
        }
        updateTemplateDefaultScorer(item, scorerUserId, scorerUserName, deptName, candidateUserIds, candidateUserNames);
    }

    private void updateProjectDefaultScorer(SrmPreliminaryEvaluationDO evaluation, SrmPreliminaryEvaluationItemDO item,
                                            Long scorerUserId, String scorerUserName, String deptName,
                                            List<Long> candidateUserIds, String candidateUserNames) {
        if (item.getTemplateItemId() == null) {
            return;
        }
        SrmPreliminaryProjectScorerDO config = projectScorerMapper
                .selectByProjectAndTemplateItem(evaluation.getProjectId(), item.getTemplateItemId());
        boolean create = config == null;
        if (create) {
            config = new SrmPreliminaryProjectScorerDO();
            config.setProjectId(evaluation.getProjectId());
            config.setTemplateId(evaluation.getTemplateId());
            config.setTemplateVersionId(evaluation.getTemplateVersionId());
            config.setTemplateItemId(item.getTemplateItemId());
        }
        config.setGroupCodeSnapshot(item.getGroupCodeSnapshot());
        config.setGroupNameSnapshot(item.getGroupNameSnapshot());
        config.setGroupSort(item.getGroupSort());
        config.setIndicatorCodeSnapshot(item.getIndicatorCodeSnapshot());
        config.setIndicatorNameSnapshot(item.getIndicatorNameSnapshot());
        config.setIndicatorSort(item.getIndicatorSort());
        config.setDefaultDeptNames(StrUtil.blankToDefault(deptName, item.getDefaultDeptNamesSnapshot()));
        config.setScorerCandidateUserIds(joinIds(candidateUserIds));
        config.setScorerCandidateUserNames(candidateUserNames);
        config.setScorerUserId(scorerUserId);
        config.setScorerUserName(scorerUserName);
        if (create) {
            projectScorerMapper.insert(config);
        } else {
            projectScorerMapper.updateById(config);
        }
    }

    private List<Long> parseLongCsv(String text) {
        List<Long> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }
        for (String part : text.split("[,，;；\\s]+")) {
            if (StrUtil.isBlank(part)) {
                continue;
            }
            Long value;
            try {
                value = Long.valueOf(part.trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (positiveUserId(value) != null && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> splitNameList(String text) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }
        for (String part : text.split("[,，;；、\\n\\r]+")) {
            String value = StrUtil.trim(part);
            if (StrUtil.isNotBlank(value) && !"-".equals(value) && !"0".equals(value) && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<Long> mergeUserIds(List<Long> originalUserIds, List<Long> appendedUserIds, Long selectedUserId) {
        List<Long> result = new ArrayList<>();
        appendUserIds(result, originalUserIds);
        appendUserIds(result, appendedUserIds);
        appendUserId(result, selectedUserId);
        return result;
    }

    private void appendUserIds(List<Long> target, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        for (Long userId : userIds) {
            appendUserId(target, userId);
        }
    }

    private void appendUserId(List<Long> target, Long userId) {
        if (positiveUserId(userId) != null && !target.contains(userId)) {
            target.add(userId);
        }
    }

    private String joinIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private String joinNames(List<String> names) {
        if (CollUtil.isEmpty(names)) {
            return null;
        }
        return String.join("、", names);
    }

    private Long positiveUserId(Long userId) {
        return userId != null && userId > 0 ? userId : null;
    }

    private String userName(AdminUserRespDTO user) {
        return user == null ? null : StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }

    private ScorerContext buildScorerContext(List<SrmPreliminaryEvaluationSaveReqVO.Item> reqItems,
                                             List<SrmEvaluationTemplateItemDO> templateItems,
                                             Long projectId,
                                             Long templateVersionId) {
        Map<Long, SrmPreliminaryEvaluationSaveReqVO.Item> requestedByTemplateItemId = new HashMap<>();
        Map<Long, SrmPreliminaryProjectScorerDO> projectScorerByTemplateItemId =
                loadProjectScorerMap(projectId, templateVersionId);
        Set<Long> userIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(templateItems)) {
            for (SrmEvaluationTemplateItemDO templateItem : templateItems) {
                parseLongCsv(templateItem.getDefaultScorerUserIds()).forEach(userIds::add);
                Long defaultScorerUserId = positiveUserId(templateItem.getDefaultScorerUserId());
                if (defaultScorerUserId != null) {
                    userIds.add(defaultScorerUserId);
                }
            }
        }
        if (!projectScorerByTemplateItemId.isEmpty()) {
            for (SrmPreliminaryProjectScorerDO projectScorer : projectScorerByTemplateItemId.values()) {
                parseLongCsv(projectScorer.getScorerCandidateUserIds()).forEach(userIds::add);
                Long scorerUserId = positiveUserId(projectScorer.getScorerUserId());
                if (scorerUserId != null) {
                    userIds.add(scorerUserId);
                }
            }
        }
        if (CollUtil.isNotEmpty(reqItems)) {
            for (SrmPreliminaryEvaluationSaveReqVO.Item reqItem : reqItems) {
                if (reqItem.getTemplateItemId() != null) {
                    requestedByTemplateItemId.put(reqItem.getTemplateItemId(), reqItem);
                }
                parseLongCsv(reqItem.getScorerCandidateUserIds()).forEach(userIds::add);
                Long scorerUserId = positiveUserId(reqItem.getScorerUserId());
                if (scorerUserId != null) {
                    userIds.add(scorerUserId);
                }
            }
        }
        if (CollUtil.isEmpty(userIds)) {
            return new ScorerContext(requestedByTemplateItemId, projectScorerByTemplateItemId, Map.of(), Map.of());
        }
        adminUserApi.validateUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = userMap.values().stream().map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Map.of() : deptApi.getDeptMap(deptIds);
        return new ScorerContext(requestedByTemplateItemId, projectScorerByTemplateItemId, userMap, deptMap);
    }

    private Map<Long, SrmPreliminaryProjectScorerDO> loadProjectScorerMap(Long projectId, Long templateVersionId) {
        if (projectId == null || templateVersionId == null) {
            return Map.of();
        }
        List<SrmPreliminaryProjectScorerDO> configs = projectScorerMapper
                .selectListByProjectAndTemplateVersion(projectId, templateVersionId);
        if (CollUtil.isEmpty(configs)) {
            return Map.of();
        }
        return configs.stream().collect(Collectors.toMap(SrmPreliminaryProjectScorerDO::getTemplateItemId,
                item -> item, (left, right) -> right));
    }

    private String resolveUserNames(List<Long> userIds, Map<Long, AdminUserRespDTO> userMap) {
        if (CollUtil.isEmpty(userIds)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = userMap.get(userId);
            names.add(user == null ? String.valueOf(userId) : StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
        }
        return joinNames(names);
    }

    private void copyEditableFields(SrmPreliminaryEvaluationSaveReqVO reqVO, SrmPreliminaryEvaluationDO evaluation) {
        evaluation.setEvaluationNo(StrUtil.trim(reqVO.getEvaluationNo()));
        evaluation.setSupplierId(reqVO.getSupplierId());
        evaluation.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        evaluation.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        evaluation.setSupplierSourceType("REGISTERED");
        evaluation.setTemplateVersionId(reqVO.getTemplateVersionId());
        applyEditableScoreLine(reqVO, evaluation);
        evaluation.setRemark(reqVO.getRemark());
    }

    private String startBpmProcess(SrmPreliminaryEvaluationDO evaluation, List<Long> scorerUserIds) {
        Map<String, Object> variables = buildBpmVariables(evaluation);
        variables.put("scoreUserIds", scorerUserIds);
        variables.put("needGeneralManager", false);
        Map<String, List<Long>> startAssignees = new HashMap<>();
        startAssignees.put(BPM_NODE_SCORING, scorerUserIds);
        startAssignees.put(BPM_NODE_DECISION, List.of(evaluation.getInitiatorId()));
        startAssignees.put(BPM_NODE_PUBLISH, List.of(evaluation.getInitiatorId()));
        variables.put("PROCESS_APPROVE_USER_SELECT_ASSIGNEES", startAssignees);
        BpmProcessInstanceCreateReqDTO request = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY)
                .setBusinessKey(String.valueOf(evaluation.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(startAssignees);
        try {
            return bpmProcessInstanceApi.createProcessInstance(evaluation.getInitiatorId(), request);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(evaluation.getInitiatorId(), request);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(SRM_PRELIMINARY_EVALUATION_BPM_NOT_PUBLISHED);
                }
                throw retryEx;
            }
        }
    }

    private void deployBpmModel() {
        try {
            Long deployUserId = SecurityFrameworkUtils.getLoginUserId();
            bpmModelService.deployModel(deployUserId == null ? 1L : deployUserId, BPM_MODEL_ID);
        } catch (ServiceException ex) {
            throw exception(SRM_PRELIMINARY_EVALUATION_BPM_NOT_PUBLISHED);
        }
    }

    private void approveBpmTask(SrmPreliminaryEvaluationDO evaluation, String taskKey, Long assigneeUserId,
                                String reason, Map<String, List<Long>> nextAssignees) {
        if (StrUtil.isBlank(evaluation.getProcessInstanceId())) {
            throw exception(SRM_PRELIMINARY_EVALUATION_BPM_TASK_MISSING);
        }
        Task task = findRunningTask(evaluation.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_PRELIMINARY_EVALUATION_BPM_TASK_MISSING);
        }
        BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                .setId(task.getId())
                .setReason(reason)
                .setVariables(buildBpmVariables(evaluation));
        if (CollUtil.isNotEmpty(nextAssignees)) {
            reqVO.setNextAssignees(nextAssignees);
        }
        bpmTaskService.approveTask(assigneeUserId, reqVO);
    }

    private Task findRunningTask(String processInstanceId, String taskKey, Long assigneeUserId) {
        if (StrUtil.isBlank(processInstanceId)) {
            return null;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(processInstanceId, true, taskKey);
        if (CollUtil.isEmpty(tasks)) {
            return null;
        }
        return tasks.stream().filter(task -> Objects.equals(parseTaskAssignee(task), assigneeUserId))
                .findFirst().orElse(null);
    }

    private Long parseTaskAssignee(Task task) {
        if (task == null || StrUtil.isBlank(task.getAssignee())) {
            return null;
        }
        try {
            return Long.valueOf(task.getAssignee());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, List<Long>> nextAssignees(String taskKey, List<Long> userIds) {
        return Map.of(taskKey, userIds.stream().filter(Objects::nonNull).distinct().toList());
    }

    private Map<String, Object> buildBpmVariables(SrmPreliminaryEvaluationDO evaluation) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("evaluationId", evaluation.getId());
        variables.put("evaluationNo", evaluation.getEvaluationNo());
        variables.put("supplierCode", evaluation.getSupplierCode());
        variables.put("supplierName", evaluation.getSupplierName());
        variables.put("projectId", evaluation.getProjectId());
        variables.put("projectCode", evaluation.getProjectCode());
        variables.put("projectName", evaluation.getProjectName());
        variables.put("templateName", evaluation.getTemplateNameSnapshot());
        variables.put("templateVersion", evaluation.getTemplateVersionSnapshot());
        variables.put("initiatorId", evaluation.getInitiatorId());
        if (evaluation.getTotalScore() != null) {
            variables.put("totalScore", evaluation.getTotalScore());
        }
        if (StrUtil.isNotBlank(evaluation.getFinalDecision())) {
            variables.put("finalDecision", evaluation.getFinalDecision());
        }
        return variables;
    }

    private boolean matchesVeto(BigDecimal value, String operator, BigDecimal threshold) {
        int compare = value.compareTo(threshold);
        return switch (operator) {
            case "LE" -> compare <= 0;
            case "LT" -> compare < 0;
            case "EQ" -> compare == 0;
            case "GE" -> compare >= 0;
            case "GT" -> compare > 0;
            default -> false;
        };
    }

    private String operatorText(String operator) {
        return switch (operator) {
            case "LE" -> "<=";
            case "LT" -> "<";
            case "EQ" -> "=";
            case "GE" -> ">=";
            case "GT" -> ">";
            default -> operator;
        };
    }

    private boolean isEvaluationAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, EVALUATION_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private void assertMaintainer(SrmPreliminaryEvaluationDO evaluation) {
        Long userId = currentUser().id();
        if (!Objects.equals(evaluation.getInitiatorId(), userId) && !isEvaluationAdmin(userId)) {
            throw exception(SRM_PRELIMINARY_EVALUATION_OPERATOR_ONLY);
        }
    }

    private void assertStatus(SrmPreliminaryEvaluationDO evaluation, String expected) {
        if (!expected.equals(evaluation.getStatus())) {
            throw exception(SRM_PRELIMINARY_EVALUATION_STATUS_INVALID);
        }
    }

    private SrmPreliminaryEvaluationDO validateEvaluation(Long id) {
        SrmPreliminaryEvaluationDO evaluation = id == null ? null : evaluationMapper.selectById(id);
        if (evaluation == null) {
            throw exception(SRM_PRELIMINARY_EVALUATION_NOT_EXISTS);
        }
        return evaluation;
    }

    private void writeLog(Long evaluationId, String action, String fromStatus, String toStatus,
                          String description, Object detail) {
        UserSnapshot user = currentUser();
        SrmPreliminaryEvaluationLogDO log = new SrmPreliminaryEvaluationLogDO();
        log.setEvaluationId(evaluationId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionDescription(description);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setDetailJson(detail == null ? null : JsonUtils.toJsonString(detail));
        logMapper.insert(log);
    }

    private UserSnapshot currentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        if (userId != null && StrUtil.isBlank(userName)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            userName = user == null ? null : user.getNickname();
        }
        return new UserSnapshot(userId, StrUtil.blankToDefault(userName, "系统用户"));
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private record UserSnapshot(Long id, String name) {
    }

    private record ScorerContext(Map<Long, SrmPreliminaryEvaluationSaveReqVO.Item> requestedByTemplateItemId,
                                 Map<Long, SrmPreliminaryProjectScorerDO> projectScorerByTemplateItemId,
                                 Map<Long, AdminUserRespDTO> userMap,
                                 Map<Long, DeptRespDTO> deptMap) {
    }

    private record TemplateSnapshot(SrmEvaluationTemplateDO template,
                                    SrmEvaluationTemplateVersionDO version,
                                    List<SrmEvaluationTemplateItemDO> items) {
    }

}
