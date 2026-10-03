package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationAssignmentHistoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationProjectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationProjectUserDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationSignDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleRequestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationAssignmentHistoryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationProjectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationProjectUserMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationSignMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmTrialValidationMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_ASSIGNER_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_FINAL_APPROVER_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_ITEM_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_INITIATOR_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_INSPECTOR_OUT_OF_SCOPE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PRODUCTION_ATTACHMENT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_PROJECT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_SAVE_INITIATOR_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_SAVE_INITIATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_SIGN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_WITHDRAW_NOT_ALLOWED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmSampleEvaluationServiceImpl implements SrmSampleEvaluationService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_INSPECTION_REPORT = "INSPECTION_REPORT";
    private static final String STATUS_VALUE_CONFIRM = "VALUE_CONFIRM";
    private static final String STATUS_DEPT_SIGN = "DEPT_SIGN";
    private static final String STATUS_INITIATOR_CONFIRM = "INITIATOR_CONFIRM";
    private static final String STATUS_FINAL_APPROVAL = "FINAL_APPROVAL";
    private static final String STATUS_ARCHIVE_CONFIRM = "ARCHIVE_CONFIRM";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String SIGN_STATUS_PENDING = "PENDING";
    private static final String SIGN_STATUS_COMPLETED = "COMPLETED";
    private static final String ITEM_STATUS_PENDING = "PENDING";
    private static final String ITEM_STATUS_REPORTED = "REPORTED";
    private static final String BPM_PROCESS_KEY = "srm_sample_evaluation_project";
    private static final String BPM_MODEL_ID = "srm-sample-evaluation-project-model";
    private static final String BPM_NODE_START = "StartUserNode";
    private static final String BPM_NODE_INSPECTION_REPORT = "inspection_report";
    private static final String BPM_NODE_VALUE_CONFIRM = "value_confirm";
    private static final String BPM_NODE_DEPT_SIGN = "dept_sign";
    private static final String BPM_NODE_INITIATOR_CONFIRM = "initiator_confirm";
    private static final String BPM_NODE_FINAL_APPROVAL = "final_approval";
    private static final String BPM_NODE_ARCHIVE_CONFIRM = "archive_confirm";
    private static final String BPM_VARIABLE_COLL_USER_LIST = "coll_userList";
    private static final String BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES = "PROCESS_APPROVE_USER_SELECT_ASSIGNEES";
    private static final String QUALITY_REPORT_PERMISSION = "mes:srm-sample-evaluation:report";
    private static final String FINAL_APPROVAL_PERMISSION = "mes:srm-sample-evaluation:final-approve";
    private static final String BIZ_TYPE_SIGN_ATTACHMENT = "SRM_SAMPLE_EVALUATION_SIGN";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1_009_003_002;
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String SRM_SUPER_ADMIN_ROLE = "srm_supplier_super_admin";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String EVALUATION_NO_PREFIX = "YPPJB-";
    private static final DateTimeFormatter EVALUATION_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int EVALUATION_NO_DAILY_SEQUENCE_LIMIT = 999;
    private static final String PROJECT_STATUS_ENABLED = "ENABLED";

    @Resource
    private SrmSampleEvaluationMapper sampleEvaluationMapper;
    @Resource
    private SrmSampleEvaluationItemMapper itemMapper;
    @Resource
    private SrmSampleEvaluationSignMapper signMapper;
    @Resource
    private SrmSampleEvaluationProjectMapper projectMapper;
    @Resource
    private SrmSampleEvaluationProjectUserMapper projectUserMapper;
    @Resource
    private SrmSampleEvaluationAssignmentHistoryMapper assignmentHistoryMapper;
    @Resource
    private SrmSampleEvaluationLogMapper logMapper;
    @Resource
    private SrmSampleRequestMapper sampleRequestMapper;
    @Resource
    private SrmTrialValidationMapper trialValidationMapper;
    @Resource
    private SrmAttachmentMapper attachmentMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private BpmModelService bpmModelService;
    @Resource
    private BpmTaskService bpmTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSampleEvaluation(SrmSampleEvaluationSaveReqVO reqVO) {
        String evaluationNo = generateEvaluationNo();
        validateEvaluationNoUnique(null, evaluationNo);
        UserSnapshot user = currentUser();
        SrmSampleEvaluationDO evaluation = new SrmSampleEvaluationDO();
        SrmSampleEvaluationProjectDO project = copyEditableFields(reqVO, evaluation);
        fillFromSampleRequest(reqVO.getSampleRequestId(), evaluation, true);
        assertProjectSaveInitiator(project, user.id());
        evaluation.setEvaluationNo(evaluationNo);
        evaluation.setStatus(STATUS_DRAFT);
        evaluation.setCurrentNodeName(statusText(STATUS_DRAFT));
        evaluation.setInitiatorUserId(user.id());
        evaluation.setInitiatorUserName(user.name());
        evaluation.setEvaluationDate(evaluation.getEvaluationDate() == null ? LocalDate.now() : evaluation.getEvaluationDate());
        evaluation.setVersion(0);
        sampleEvaluationMapper.insert(evaluation);
        saveItems(evaluation.getId(), reqVO.getItems(), ITEM_STATUS_PENDING);
        if (evaluation.getSampleRequestId() != null) {
            sampleRequestMapper.incrementSampleEvaluationCount(evaluation.getSampleRequestId());
        }
        writeLog(evaluation.getId(), "CREATE", null, STATUS_DRAFT, "创建样品评价表", null);
        return evaluation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSampleEvaluation(SrmSampleEvaluationSaveReqVO reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        UserSnapshot user = currentUser();
        SrmSampleEvaluationProjectDO project = copyEditableFields(reqVO, evaluation);
        fillFromSampleRequest(reqVO.getSampleRequestId(), evaluation, false);
        assertProjectSaveInitiator(project, user.id());
        evaluation.setVersion(reqVO.getVersion());
        updateByIdChecked(evaluation);
        saveItems(evaluation.getId(), reqVO.getItems(), ITEM_STATUS_PENDING);
        writeLog(evaluation.getId(), "UPDATE", STATUS_DRAFT, STATUS_DRAFT, "更新样品评价表", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSampleEvaluation(Long id) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(id);
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        itemMapper.deleteByEvaluationId(id);
        signMapper.deleteByEvaluationId(id);
        sampleEvaluationMapper.deleteById(id);
    }

    @Override
    public SrmSampleEvaluationRespVO getSampleEvaluation(Long id) {
        return buildResp(validateSampleEvaluation(id), true);
    }

    @Override
    public PageResult<SrmSampleEvaluationRespVO> getSampleEvaluationPage(SrmSampleEvaluationPageReqVO reqVO) {
        PageResult<SrmSampleEvaluationDO> page = sampleEvaluationMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(),
                page.getTotal());
    }

    @Override
    public PageResult<SrmSampleRequestRespVO> getSelectableSampleRequestPage(SrmSampleEvaluationSampleRequestPageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        LambdaQueryWrapperX<SrmSampleRequestDO> query = new LambdaQueryWrapperX<SrmSampleRequestDO>()
                .likeIfPresent(SrmSampleRequestDO::getRequestNo, reqVO.getRequestNo())
                .likeIfPresent(SrmSampleRequestDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmSampleRequestDO::getSupplierName, reqVO.getSupplierName())
                .orderByDesc(SrmSampleRequestDO::getUpdateTime)
                .orderByDesc(SrmSampleRequestDO::getId);
        if (StrUtil.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like(SrmSampleRequestDO::getRequestNo, keyword)
                    .or()
                    .like(SrmSampleRequestDO::getMaterialName, keyword)
                    .or()
                    .like(SrmSampleRequestDO::getMaterialModel, keyword)
                    .or()
                    .like(SrmSampleRequestDO::getSupplierName, keyword));
        }
        PageResult<SrmSampleRequestDO> page = sampleRequestMapper.selectPage(reqVO, query);
        return new PageResult<>(page.getList().stream()
                .map(item -> BeanUtils.toBean(item, SrmSampleRequestRespVO.class)).toList(), page.getTotal());
    }

    @Override
    public List<SrmSampleEvaluationRespVO.Item> getLatestItemsBySampleRequestId(Long sampleRequestId) {
        if (sampleRequestId == null) {
            return List.of();
        }
        SrmSampleEvaluationDO latest = sampleEvaluationMapper.selectOne(new LambdaQueryWrapperX<SrmSampleEvaluationDO>()
                .eq(SrmSampleEvaluationDO::getSampleRequestId, sampleRequestId)
                .ne(SrmSampleEvaluationDO::getStatus, STATUS_DRAFT)
                .orderByDesc(SrmSampleEvaluationDO::getEvaluationDate)
                .orderByDesc(SrmSampleEvaluationDO::getId)
                .last("LIMIT 1"));
        if (latest == null) {
            return List.of();
        }
        return itemMapper.selectListByEvaluationId(latest.getId()).stream().map(this::buildItemResp).toList();
    }

    @Override
    public PageResult<SrmSampleEvaluationRespVO> getHistoryPage(Long sampleRequestId, SrmSampleEvaluationPageReqVO reqVO) {
        PageResult<SrmSampleEvaluationDO> page = sampleEvaluationMapper.selectPage(reqVO,
                new LambdaQueryWrapperX<SrmSampleEvaluationDO>()
                        .eq(SrmSampleEvaluationDO::getSampleRequestId, sampleRequestId)
                        .orderByDesc(SrmSampleEvaluationDO::getEvaluationDate)
                        .orderByDesc(SrmSampleEvaluationDO::getId));
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(), page.getTotal());
    }

    @Override
    public List<SrmSampleEvaluationProjectRespVO> getEnabledProjects() {
        return projectMapper.selectEnabledList().stream()
                .map(project -> buildProjectResp(project, false))
                .toList();
    }

    @Override
    public SrmSampleEvaluationProjectRespVO getProjectConfig(Long projectId) {
        return buildProjectResp(validateProject(projectId, null), true);
    }

    @Override
    public SrmSampleEvaluationProjectRespVO.AssignableInspectors getAssignableInspectors(Long projectId) {
        SrmSampleEvaluationProjectDO project = validateProject(projectId, null);
        CurrentUser currentUser = currentUserWithDept();
        assertProjectAssigner(project.getId(), currentUser.id());
        List<SrmSampleEvaluationProjectRespVO.AssignableInspector> users =
                resolveAssignableInspectors(project, currentUser);
        SrmSampleEvaluationAssignmentHistoryDO latest = assignmentHistoryMapper.selectLatest(
                project.getId(), currentUser.id(), currentUser.deptId());
        Long recommendedUserId = latest == null ? null : latest.getInspectorUserId();
        String recommendedUserName = latest == null ? null : latest.getInspectorUserName();
        if (recommendedUserId != null) {
            boolean stillAssignable = false;
            for (SrmSampleEvaluationProjectRespVO.AssignableInspector user : users) {
                boolean recommended = Objects.equals(user.getUserId(), recommendedUserId);
                user.setRecommended(recommended);
                stillAssignable = stillAssignable || recommended;
            }
            if (!stillAssignable) {
                recommendedUserId = null;
                recommendedUserName = null;
            }
        }
        SrmSampleEvaluationProjectRespVO.AssignableInspectors respVO =
                new SrmSampleEvaluationProjectRespVO.AssignableInspectors();
        respVO.setProjectId(project.getId());
        respVO.setProjectCode(project.getProjectCode());
        respVO.setProjectName(project.getProjectName());
        respVO.setRecommendedUserId(recommendedUserId);
        respVO.setRecommendedUserName(recommendedUserName);
        respVO.setUsers(users);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(SrmSampleEvaluationActionReqVO.Submit reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DRAFT);
        SrmSampleEvaluationProjectDO project = validateProject(evaluation.getProjectId(), evaluation.getProjectCode());
        CurrentUser currentUser = currentUserWithDept();
        assertProjectInitiator(project.getId(), currentUser.id());
        assertProjectAssigner(project.getId(), currentUser.id());
        SrmSampleEvaluationProjectRespVO.AssignableInspector inspector =
                validateAssignableInspector(project, currentUser, reqVO.getInspectorUserId());
        evaluation.setAssignedInspectorUserId(inspector.getUserId());
        evaluation.setAssignedInspectorUserName(inspector.getUserName());
        evaluation.setAssignedInspectorDeptId(inspector.getDeptId());
        evaluation.setAssignedInspectorDeptName(inspector.getDeptName());
        evaluation.setAssignedTime(LocalDateTime.now());
        evaluation.setConfirmUserId(evaluation.getInitiatorUserId());
        evaluation.setConfirmUserName(evaluation.getInitiatorUserName());
        String processInstanceId = startBpmProcess(evaluation);
        evaluation.setProcessInstanceId(processInstanceId);
        approveBpmTask(evaluation, BPM_NODE_START, evaluation.getInitiatorUserId(), "提交样品评价表",
                nextAssignees(BPM_NODE_INSPECTION_REPORT, List.of(inspector.getUserId())),
                Map.of("reportUserIds", List.of(inspector.getUserId()),
                        "assignedInspectorUserId", inspector.getUserId()));
        recordAssignmentHistory(evaluation, project, currentUser, inspector);
        updateStatus(evaluation, STATUS_INSPECTION_REPORT, "提交样品评价表，进入执行检测",
                Map.of("inspectorUserId", inspector.getUserId(), "inspectorUserName", inspector.getUserName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inspectionReport(SrmSampleEvaluationActionReqVO.InspectionReport reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertStatus(evaluation, STATUS_INSPECTION_REPORT);
        UserSnapshot user = currentUser();
        Long userId = user.id();
        if (evaluation.getAssignedInspectorUserId() != null
                && !Objects.equals(evaluation.getAssignedInspectorUserId(), userId)) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
        Task task = findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_INSPECTION_REPORT, userId);
        if (task == null) {
            throw exception(SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING);
        }
        validateInspectionItems(reqVO.getItems());
        UserSnapshot confirmUser = new UserSnapshot(evaluation.getInitiatorUserId(), evaluation.getInitiatorUserName());
        saveItems(evaluation.getId(), reqVO.getItems(), ITEM_STATUS_REPORTED);
        evaluation.setReporterUserId(userId);
        evaluation.setReporterUserName(user.name());
        evaluation.setReportTime(LocalDateTime.now());
        evaluation.setConfirmUserId(confirmUser.id());
        evaluation.setConfirmUserName(confirmUser.name());
        approveBpmTask(evaluation, BPM_NODE_INSPECTION_REPORT, userId, "执行检测",
                nextAssignees(BPM_NODE_VALUE_CONFIRM, List.of(confirmUser.id())),
                Map.of("confirmUserId", confirmUser.id(), "valueConfirmed", false));
        updateStatus(evaluation, STATUS_VALUE_CONFIRM, "执行检测完成，进入检测结果确认",
                Map.of("confirmUserId", confirmUser.id()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void valueConfirm(SrmSampleEvaluationActionReqVO.ValueConfirm reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertStatus(evaluation, STATUS_VALUE_CONFIRM);
        assertCurrentOperator(evaluation.getConfirmUserId());
        evaluation.setConfirmResult(Boolean.TRUE.equals(reqVO.getPassed()) ? "PASS" : "RETURN");
        evaluation.setConfirmOpinion(StrUtil.trim(reqVO.getOpinion()));
        evaluation.setConfirmTime(LocalDateTime.now());
        if (!Boolean.TRUE.equals(reqVO.getPassed())) {
            Long reporterUserId = firstLong(evaluation.getAssignedInspectorUserId(), evaluation.getReporterUserId());
            if (reporterUserId == null) {
                throw exception(SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED);
            }
            approveBpmTask(evaluation, BPM_NODE_VALUE_CONFIRM, evaluation.getConfirmUserId(), "检测结果确认退回",
                    nextAssignees(BPM_NODE_INSPECTION_REPORT, List.of(reporterUserId)),
                    Map.of("valueConfirmed", false, "reportUserIds", List.of(reporterUserId)));
            updateStatus(evaluation, STATUS_INSPECTION_REPORT, "检测结果确认退回执行检测", null);
            return;
        }
        List<SrmSampleEvaluationSignDO> signs = evaluation.getProjectId() == null
                ? buildSignRows(evaluation.getId(), reqVO.getSignUsers())
                : buildProjectSignRows(evaluation);
        if (CollUtil.isEmpty(signs)) {
            throw exception(SRM_SAMPLE_EVALUATION_SIGN_REQUIRED);
        }
        completeOwnSignRows(signs, evaluation.getConfirmUserId(), reqVO.getOpinion());
        signMapper.deleteByEvaluationId(evaluation.getId());
        signMapper.insertBatch(signs);
        List<Long> pendingSignUserIds = signs.stream()
                .filter(sign -> SIGN_STATUS_PENDING.equals(sign.getSignStatus()))
                .map(SrmSampleEvaluationSignDO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, Object> bpmVariables = new HashMap<>();
        bpmVariables.put("valueConfirmed", true);
        bpmVariables.put("hasPendingSigners", CollUtil.isNotEmpty(pendingSignUserIds));
        bpmVariables.put("signUserIds", pendingSignUserIds);
        bpmVariables.put(BPM_VARIABLE_COLL_USER_LIST, pendingSignUserIds);
        if (CollUtil.isEmpty(pendingSignUserIds)) {
            List<Long> finalApprovalUserIds = resolveFinalApprovalUserIds();
            approveBpmTask(evaluation, BPM_NODE_VALUE_CONFIRM, evaluation.getConfirmUserId(), "检测结果确认通过",
                    null,
                    appendFinalApprovalVariables(bpmVariables, finalApprovalUserIds));
            updateStatus(evaluation, STATUS_FINAL_APPROVAL, "检测结果确认通过，全部会签完成，进入最终批准",
                    Map.of("finalApprovalUserIds", finalApprovalUserIds), "VALUE_CONFIRM");
            return;
        }
        Map<String, List<Long>> deptSignAssignees = nextAssignees(BPM_NODE_DEPT_SIGN, pendingSignUserIds);
        bpmVariables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, deptSignAssignees);
        approveBpmTask(evaluation, BPM_NODE_VALUE_CONFIRM, evaluation.getConfirmUserId(), "检测结果确认通过",
                deptSignAssignees, bpmVariables);
        updateStatus(evaluation, STATUS_DEPT_SIGN, "检测结果确认通过，进入会签",
                Map.of("signUserIds", pendingSignUserIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sign(SrmSampleEvaluationActionReqVO.Sign reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertStatus(evaluation, STATUS_DEPT_SIGN);
        Long userId = currentUser().id();
        List<SrmSampleEvaluationSignDO> pendingSigns = signMapper.selectPendingListByEvaluationIdAndUserId(evaluation.getId(), userId);
        if (CollUtil.isEmpty(pendingSigns)) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
        if (reqVO.getSignId() != null
                && pendingSigns.stream().noneMatch(sign -> Objects.equals(sign.getId(), reqVO.getSignId()))) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
        if (pendingSigns.stream().anyMatch(this::requiresProductionAttachment)) {
            if (StrUtil.isBlank(reqVO.getOpinion())) {
                throw exception(SRM_SAMPLE_EVALUATION_PRODUCTION_ATTACHMENT_REQUIRED);
            }
            for (SrmSampleEvaluationSignDO sign : pendingSigns) {
                if (requiresProductionAttachment(sign) && CollUtil.isEmpty(attachmentMapper.selectByBiz(BIZ_TYPE_SIGN_ATTACHMENT, sign.getId(), false))) {
                    throw exception(SRM_SAMPLE_EVALUATION_PRODUCTION_ATTACHMENT_REQUIRED);
                }
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (SrmSampleEvaluationSignDO sign : pendingSigns) {
            sign.setSignStatus(SIGN_STATUS_COMPLETED);
            sign.setSignResult(StrUtil.trim(reqVO.getResult()));
            sign.setSignOpinion(StrUtil.trim(reqVO.getOpinion()));
            sign.setSignTime(now);
            signMapper.updateById(sign);
        }
        boolean allCompleted = signMapper.selectListByEvaluationId(evaluation.getId()).stream()
                .allMatch(sign -> SIGN_STATUS_COMPLETED.equals(sign.getSignStatus()));
        if (allCompleted) {
            List<Long> finalApprovalUserIds = resolveFinalApprovalUserIds();
            approveBpmTask(evaluation, BPM_NODE_DEPT_SIGN, userId, "会签", null,
                    finalApprovalVariables(finalApprovalUserIds));
            updateStatus(evaluation, STATUS_FINAL_APPROVAL, "会签完成，进入最终批准",
                    Map.of("finalApprovalUserIds", finalApprovalUserIds), "SIGN_COMPLETE");
        } else {
            approveBpmTask(evaluation, BPM_NODE_DEPT_SIGN, userId, "会签", null);
            writeLog(evaluation.getId(), "SIGN", STATUS_DEPT_SIGN, STATUS_DEPT_SIGN, "会签", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initiatorDecision(SrmSampleEvaluationActionReqVO.InitiatorDecision reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_INITIATOR_CONFIRM);
        evaluation.setInitiatorDecisionOpinion(StrUtil.trim(reqVO.getOpinion()));
        evaluation.setArchiveOpinion(StrUtil.trim(reqVO.getOpinion()));
        List<Long> finalApprovalUserIds = resolveFinalApprovalUserIds();
        approveBpmTask(evaluation, BPM_NODE_INITIATOR_CONFIRM, evaluation.getInitiatorUserId(), "发起人提交最终审批",
                null,
                Map.of("needFinalApproval", true,
                        "finalApprovalUserIds", finalApprovalUserIds,
                        BPM_VARIABLE_COLL_USER_LIST, finalApprovalUserIds));
        updateStatus(evaluation, STATUS_FINAL_APPROVAL, "发起人确认完成，进入最终批准",
                Map.of("finalApprovalUserIds", finalApprovalUserIds), "INITIATOR_SEND_APPROVAL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalApprove(SrmSampleEvaluationActionReqVO.FinalApprove reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertStatus(evaluation, STATUS_FINAL_APPROVAL);
        UserSnapshot user = currentUser();
        Task task = findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_FINAL_APPROVAL, user.id());
        if (task == null) {
            throw exception(SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING);
        }
        LocalDateTime now = LocalDateTime.now();
        evaluation.setFinalApproverUserId(user.id());
        evaluation.setFinalApproverUserName(user.name());
        evaluation.setFinalApproverOpinion(StrUtil.trim(reqVO.getOpinion()));
        evaluation.setFinalApproverHandleTime(now);
        approveBpmTask(evaluation, BPM_NODE_FINAL_APPROVAL, user.id(), "最终批准",
                nextAssignees(BPM_NODE_ARCHIVE_CONFIRM, List.of(evaluation.getInitiatorUserId())),
                Map.of("finalApproved", true, "finalApproverUserId", user.id()));
        updateStatus(evaluation, STATUS_ARCHIVE_CONFIRM, "最终批准完成，进入归档",
                Map.of("finalApproverUserId", user.id()), "FINAL_APPROVE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveConfirm(SrmSampleEvaluationActionReqVO.ArchiveConfirm reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertStatus(evaluation, STATUS_ARCHIVE_CONFIRM);
        UserSnapshot user = currentUser();
        if (!Objects.equals(evaluation.getInitiatorUserId(), user.id())) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
        Task task = findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_ARCHIVE_CONFIRM, user.id());
        if (task == null) {
            throw exception(SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING);
        }
        evaluation.setArchiveOpinion(StrUtil.trim(reqVO.getOpinion()));
        evaluation.setArchiveTime(LocalDateTime.now());
        approveBpmTask(evaluation, BPM_NODE_ARCHIVE_CONFIRM, user.id(), "完成归档", null);
        updateStatus(evaluation, STATUS_ARCHIVED, "归档完成", null, "ARCHIVE_CONFIRM");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawConfirm(SrmSampleEvaluationActionReqVO.WithdrawConfirm reqVO) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(reqVO.getId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_DEPT_SIGN);
        List<SrmSampleEvaluationSignDO> signs = signMapper.selectListByEvaluationId(evaluation.getId());
        if (CollUtil.isEmpty(signs) || signs.stream().anyMatch(sign -> SIGN_STATUS_COMPLETED.equals(sign.getSignStatus()))) {
            throw exception(SRM_SAMPLE_EVALUATION_WITHDRAW_NOT_ALLOWED);
        }
        HistoricTaskInstance valueConfirmTask = findLatestFinishedTask(evaluation.getProcessInstanceId(),
                BPM_NODE_VALUE_CONFIRM, evaluation.getInitiatorUserId());
        if (valueConfirmTask == null) {
            throw exception(SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING);
        }
        bpmTaskService.withdrawTask(evaluation.getInitiatorUserId(), valueConfirmTask.getId());
        signMapper.deleteByEvaluationId(evaluation.getId());
        updateStatus(evaluation, STATUS_VALUE_CONFIRM, "会签前撤回到检测结果确认",
                Map.of("opinion", StrUtil.nullToEmpty(StrUtil.trim(reqVO.getOpinion()))), "WITHDRAW_CONFIRM");
    }

    private SrmSampleEvaluationProjectDO copyEditableFields(SrmSampleEvaluationSaveReqVO reqVO, SrmSampleEvaluationDO evaluation) {
        evaluation.setSampleRequestId(reqVO.getSampleRequestId());
        evaluation.setSampleRequestNo(StrUtil.trim(reqVO.getSampleRequestNo()));
        SrmSampleEvaluationProjectDO project = validateProject(reqVO.getProjectId(), reqVO.getProjectCode());
        evaluation.setProjectId(project.getId());
        evaluation.setProjectCode(project.getProjectCode());
        evaluation.setProjectName(project.getProjectName());
        evaluation.setSupplierId(reqVO.getSupplierId());
        evaluation.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        evaluation.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        evaluation.setMaterialName(StrUtil.trim(reqVO.getMaterialName()));
        evaluation.setMaterialModel(StrUtil.trim(reqVO.getMaterialModel()));
        evaluation.setSampleQty(reqVO.getSampleQty());
        evaluation.setEvaluationDate(reqVO.getEvaluationDate());
        evaluation.setSampleSendCount(reqVO.getSampleSendCount());
        evaluation.setVerificationTypes(joinValues(reqVO.getVerificationTypes()));
        evaluation.setInspectionTypes(joinValues(reqVO.getInspectionTypes()));
        evaluation.setApplyDept(StrUtil.trim(reqVO.getApplyDept()));
        evaluation.setApplyDate(reqVO.getApplyDate());
        evaluation.setApprovedByUserId(reqVO.getApprovedByUserId());
        evaluation.setApprovedByName(resolveUserName(reqVO.getApprovedByUserId(), reqVO.getApprovedByName()));
        evaluation.setRemark(StrUtil.trim(reqVO.getRemark()));
        return project;
    }

    private void fillFromSampleRequest(Long sampleRequestId, SrmSampleEvaluationDO evaluation, boolean createMode) {
        if (sampleRequestId == null) {
            return;
        }
        SrmSampleRequestDO request = sampleRequestMapper.selectById(sampleRequestId);
        if (request == null) {
            throw exception(SRM_SAMPLE_REQUEST_NOT_EXISTS);
        }
        evaluation.setSampleRequestId(request.getId());
        evaluation.setSampleRequestNo(StrUtil.blankToDefault(evaluation.getSampleRequestNo(), request.getRequestNo()));
        evaluation.setSupplierId(evaluation.getSupplierId() == null ? request.getSupplierId() : evaluation.getSupplierId());
        evaluation.setSupplierCode(StrUtil.blankToDefault(evaluation.getSupplierCode(), request.getSupplierCode()));
        evaluation.setSupplierName(StrUtil.blankToDefault(evaluation.getSupplierName(), request.getSupplierName()));
        evaluation.setMaterialName(StrUtil.blankToDefault(evaluation.getMaterialName(), request.getMaterialName()));
        evaluation.setMaterialModel(StrUtil.blankToDefault(evaluation.getMaterialModel(), request.getMaterialModel()));
        evaluation.setSampleQty(evaluation.getSampleQty() == null ? request.getRequireQty() : evaluation.getSampleQty());
        evaluation.setApplyDept(StrUtil.blankToDefault(evaluation.getApplyDept(), request.getApplyDept()));
        evaluation.setApplyDate(evaluation.getApplyDate() == null ? request.getApplyDate() : evaluation.getApplyDate());
        evaluation.setApprovedByUserId(evaluation.getApprovedByUserId() == null ? request.getFinalApproverUserId() : evaluation.getApprovedByUserId());
        evaluation.setApprovedByName(StrUtil.blankToDefault(evaluation.getApprovedByName(), request.getFinalApproverUserName()));
        if (createMode && evaluation.getSampleSendCount() == null) {
            evaluation.setSampleSendCount((request.getSampleEvaluationCount() == null ? 0 : request.getSampleEvaluationCount()) + 1);
        }
    }

    private void saveItems(Long evaluationId, List<SrmSampleEvaluationSaveReqVO.Item> reqItems, String defaultStatus) {
        if (reqItems == null) {
            return;
        }
        List<Long> keepIds = reqItems.stream().map(SrmSampleEvaluationSaveReqVO.Item::getId)
                .filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(keepIds)) {
            itemMapper.deleteByEvaluationId(evaluationId);
        } else {
            itemMapper.delete(new LambdaQueryWrapperX<SrmSampleEvaluationItemDO>()
                    .eq(SrmSampleEvaluationItemDO::getEvaluationId, evaluationId)
                    .notIn(SrmSampleEvaluationItemDO::getId, keepIds));
        }
        int rowNo = 1;
        for (SrmSampleEvaluationSaveReqVO.Item reqItem : reqItems) {
            SrmSampleEvaluationItemDO item = BeanUtils.toBean(reqItem, SrmSampleEvaluationItemDO.class);
            item.setEvaluationId(evaluationId);
            item.setRowNo(item.getRowNo() == null ? rowNo : item.getRowNo());
            item.setItemName(StrUtil.trim(item.getItemName()));
            item.setTechnicalRequirement(StrUtil.trim(item.getTechnicalRequirement()));
            item.setTestData1(StrUtil.trim(item.getTestData1()));
            item.setTestData2(StrUtil.trim(item.getTestData2()));
            item.setTestData3(StrUtil.trim(item.getTestData3()));
            item.setTestData4(StrUtil.trim(item.getTestData4()));
            item.setTestData5(StrUtil.trim(item.getTestData5()));
            item.setItemJudgement(StrUtil.trim(item.getItemJudgement()));
            item.setItemStatus(StrUtil.blankToDefault(StrUtil.trim(item.getItemStatus()), defaultStatus));
            if (item.getId() == null) {
                itemMapper.insert(item);
            } else {
                itemMapper.updateById(item);
            }
            rowNo++;
        }
    }

    private void validateInspectionItems(List<SrmSampleEvaluationSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            throw exception(SRM_SAMPLE_EVALUATION_ITEM_REQUIRED);
        }
        boolean hasEffectiveItem = items.stream().anyMatch(item -> StrUtil.isNotBlank(item.getItemName())
                && (StrUtil.isNotBlank(item.getTestData1())
                || StrUtil.isNotBlank(item.getTestData2())
                || StrUtil.isNotBlank(item.getTestData3())
                || StrUtil.isNotBlank(item.getTestData4())
                || StrUtil.isNotBlank(item.getTestData5())));
        if (!hasEffectiveItem) {
            throw exception(SRM_SAMPLE_EVALUATION_ITEM_REQUIRED);
        }
    }

    private List<SrmSampleEvaluationSignDO> buildSignRows(Long evaluationId, List<SrmSampleEvaluationActionReqVO.SignUser> signUsers) {
        if (CollUtil.isEmpty(signUsers)) {
            return List.of();
        }
        Map<Long, SrmSampleEvaluationSignDO> unique = new LinkedHashMap<>();
        for (SrmSampleEvaluationActionReqVO.SignUser signUser : signUsers) {
            if (signUser.getUserId() == null || StrUtil.isBlank(signUser.getDeptName())) {
                continue;
            }
            UserSnapshot user = resolveUser(signUser.getUserId(), signUser.getUserName());
            unique.compute(user.id(), (ignored, existing) -> {
                if (existing == null) {
                    SrmSampleEvaluationSignDO sign = new SrmSampleEvaluationSignDO();
                    sign.setEvaluationId(evaluationId);
                    sign.setDeptCode(StrUtil.trim(signUser.getDeptCode()));
                    sign.setDeptName(StrUtil.trim(signUser.getDeptName()));
                    sign.setUserId(user.id());
                    sign.setUserName(user.name());
                    sign.setSignStatus(SIGN_STATUS_PENDING);
                    sign.setRequireAttachment(containsProductionDept(sign.getDeptName()));
                    return sign;
                }
                String deptName = StrUtil.trim(signUser.getDeptName());
                if (!StrUtil.contains(existing.getDeptName(), deptName)) {
                    existing.setDeptName(existing.getDeptName() + "、" + deptName);
                    existing.setRequireAttachment(Boolean.TRUE.equals(existing.getRequireAttachment())
                            || containsProductionDept(deptName));
                }
                return existing;
            });
        }
        return new ArrayList<>(unique.values());
    }

    private List<SrmSampleEvaluationSignDO> buildProjectSignRows(SrmSampleEvaluationDO evaluation) {
        List<SrmSampleEvaluationProjectUserDO> configuredUsers =
                projectUserMapper.selectListByProjectId(evaluation.getProjectId());
        if (CollUtil.isEmpty(configuredUsers)) {
            return List.of();
        }
        List<SrmSampleEvaluationSignDO> signs = new ArrayList<>();
        for (SrmSampleEvaluationProjectUserDO configuredUser : configuredUsers) {
            if (configuredUser.getUserId() == null) {
                continue;
            }
            UserSnapshot user = resolveUser(configuredUser.getUserId(), configuredUser.getUserName());
            SrmSampleEvaluationSignDO sign = new SrmSampleEvaluationSignDO();
            sign.setEvaluationId(evaluation.getId());
            sign.setDeptCode(StrUtil.trim(configuredUser.getDeptCode()));
            sign.setDeptName(StrUtil.trim(configuredUser.getDeptName()));
            sign.setUserId(user.id());
            sign.setUserName(user.name());
            sign.setSignStatus(SIGN_STATUS_PENDING);
            sign.setRequireAttachment(containsProductionDept(sign.getDeptName()));
            signs.add(sign);
        }
        return signs;
    }

    private void completeOwnSignRows(List<SrmSampleEvaluationSignDO> signs, Long confirmUserId, String opinion) {
        if (CollUtil.isEmpty(signs) || confirmUserId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        signs.stream()
                .filter(sign -> Objects.equals(sign.getUserId(), confirmUserId))
                .forEach(sign -> {
                    sign.setSignStatus(SIGN_STATUS_COMPLETED);
                    sign.setSignResult("PASS");
                    sign.setSignOpinion(StrUtil.trim(opinion));
                    sign.setSignTime(now);
                });
    }

    private SrmSampleEvaluationProjectRespVO buildProjectResp(SrmSampleEvaluationProjectDO project,
                                                              boolean includeUsers) {
        SrmSampleEvaluationProjectRespVO respVO =
                BeanUtils.toBean(project, SrmSampleEvaluationProjectRespVO.class);
        if (includeUsers) {
            respVO.setUsers(buildProjectUserRespList(project.getId()));
        }
        return respVO;
    }

    private List<SrmSampleEvaluationProjectRespVO.UserConfig> buildProjectUserRespList(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return projectUserMapper.selectListByProjectId(projectId).stream()
                .map(item -> BeanUtils.toBean(item, SrmSampleEvaluationProjectRespVO.UserConfig.class))
                .toList();
    }

    private SrmSampleEvaluationProjectDO validateProject(Long projectId, String projectCode) {
        if (projectId == null && StrUtil.isBlank(projectCode)) {
            throw exception(SRM_SAMPLE_EVALUATION_PROJECT_REQUIRED);
        }
        SrmSampleEvaluationProjectDO project = projectId != null
                ? projectMapper.selectById(projectId)
                : projectMapper.selectByProjectCode(StrUtil.trim(projectCode));
        if (project == null || !PROJECT_STATUS_ENABLED.equals(project.getStatus())) {
            throw exception(SRM_SAMPLE_EVALUATION_PROJECT_NOT_CONFIGURED);
        }
        return project;
    }

    private void assertProjectInitiator(Long projectId, Long userId) {
        if (projectUserMapper.selectInitiator(projectId, userId) == null) {
            throw exception(SRM_SAMPLE_EVALUATION_INITIATOR_NOT_CONFIGURED);
        }
    }

    private void assertProjectSaveInitiator(SrmSampleEvaluationProjectDO project, Long userId) {
        List<SrmSampleEvaluationProjectUserDO> initiators = projectUserMapper.selectListByProjectId(project.getId())
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getCanInitiate()))
                .toList();
        String projectName = StrUtil.blankToDefault(project.getProjectName(), project.getProjectCode());
        if (CollUtil.isEmpty(initiators)) {
            throw exception(SRM_SAMPLE_EVALUATION_SAVE_INITIATOR_NOT_CONFIGURED, projectName);
        }
        for (SrmSampleEvaluationProjectUserDO initiator : initiators) {
            if (Objects.equals(initiator.getUserId(), userId)) {
                return;
            }
        }
        throw exception(SRM_SAMPLE_EVALUATION_SAVE_INITIATOR_ONLY, projectName, resolveProjectUserNames(initiators));
    }

    private String resolveProjectUserNames(List<SrmSampleEvaluationProjectUserDO> users) {
        List<String> names = new ArrayList<>();
        for (SrmSampleEvaluationProjectUserDO user : users) {
            String name = StrUtil.blankToDefault(StrUtil.trim(user.getUserName()), String.valueOf(user.getUserId()));
            if (StrUtil.isNotBlank(name) && !names.contains(name)) {
                names.add(name);
            }
        }
        return CollUtil.isEmpty(names) ? "已配置" : String.join("、", names);
    }

    private void assertProjectAssigner(Long projectId, Long userId) {
        if (projectUserMapper.selectAssigner(projectId, userId) == null) {
            throw exception(SRM_SAMPLE_EVALUATION_ASSIGNER_NOT_CONFIGURED);
        }
    }

    private SrmSampleEvaluationProjectRespVO.AssignableInspector validateAssignableInspector(
            SrmSampleEvaluationProjectDO project, CurrentUser currentUser, Long inspectorUserId) {
        if (inspectorUserId == null) {
            throw exception(SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED);
        }
        return resolveAssignableInspectors(project, currentUser).stream()
                .filter(user -> Objects.equals(user.getUserId(), inspectorUserId))
                .findFirst()
                .orElseThrow(() -> exception(SRM_SAMPLE_EVALUATION_INSPECTOR_OUT_OF_SCOPE));
    }

    private List<SrmSampleEvaluationProjectRespVO.AssignableInspector> resolveAssignableInspectors(
            SrmSampleEvaluationProjectDO project, CurrentUser currentUser) {
        if (currentUser.id() == null) {
            return List.of();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserListBySubordinate(currentUser.id());
        if (CollUtil.isEmpty(users) && currentUser.deptId() != null) {
            users = adminUserApi.getUserListByDeptIds(List.of(currentUser.deptId()));
        }
        if (CollUtil.isEmpty(users)) {
            return List.of();
        }
        Map<Long, SrmSampleEvaluationProjectRespVO.AssignableInspector> uniqueUsers = new LinkedHashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null || Objects.equals(user.getId(), currentUser.id())
                    || !Objects.equals(user.getStatus(), 0)) {
                continue;
            }
            SrmSampleEvaluationProjectRespVO.AssignableInspector inspector =
                    new SrmSampleEvaluationProjectRespVO.AssignableInspector();
            inspector.setUserId(user.getId());
            inspector.setUserName(userName(user, null));
            inspector.setDeptId(user.getDeptId());
            inspector.setDeptName(resolveDeptName(user.getDeptId()));
            inspector.setRecommended(Boolean.FALSE);
            uniqueUsers.putIfAbsent(user.getId(), inspector);
        }
        SrmSampleEvaluationAssignmentHistoryDO latest = assignmentHistoryMapper.selectLatest(
                project.getId(), currentUser.id(), currentUser.deptId());
        Long recommendedUserId = latest == null ? null : latest.getInspectorUserId();
        return uniqueUsers.values().stream()
                .peek(user -> user.setRecommended(Objects.equals(user.getUserId(), recommendedUserId)))
                .sorted(Comparator.comparing((SrmSampleEvaluationProjectRespVO.AssignableInspector user) ->
                                StrUtil.blankToDefault(user.getDeptName(), ""))
                        .thenComparing(user -> StrUtil.blankToDefault(user.getUserName(), ""))
                        .thenComparing(user -> user.getUserId() == null ? 0L : user.getUserId()))
                .toList();
    }

    private void recordAssignmentHistory(SrmSampleEvaluationDO evaluation, SrmSampleEvaluationProjectDO project,
                                         CurrentUser currentUser,
                                         SrmSampleEvaluationProjectRespVO.AssignableInspector inspector) {
        SrmSampleEvaluationAssignmentHistoryDO history = new SrmSampleEvaluationAssignmentHistoryDO();
        history.setEvaluationId(evaluation.getId());
        history.setProjectId(project.getId());
        history.setProjectCode(project.getProjectCode());
        history.setProjectName(project.getProjectName());
        history.setInitiatorUserId(currentUser.id());
        history.setInitiatorUserName(currentUser.name());
        history.setInitiatorDeptId(currentUser.deptId());
        history.setInitiatorDeptName(currentUser.deptName());
        history.setInspectorUserId(inspector.getUserId());
        history.setInspectorUserName(inspector.getUserName());
        history.setInspectorDeptId(inspector.getDeptId());
        history.setInspectorDeptName(inspector.getDeptName());
        history.setAssignTime(LocalDateTime.now());
        assignmentHistoryMapper.insert(history);
    }

    private boolean isDeptSignWithdrawable(Long evaluationId) {
        List<SrmSampleEvaluationSignDO> signs = signMapper.selectListByEvaluationId(evaluationId);
        return CollUtil.isNotEmpty(signs)
                && signs.stream().noneMatch(sign -> SIGN_STATUS_COMPLETED.equals(sign.getSignStatus()));
    }

    private HistoricTaskInstance findLatestFinishedTask(String processInstanceId, String taskKey, Long assigneeUserId) {
        if (StrUtil.isBlank(processInstanceId) || StrUtil.isBlank(taskKey) || assigneeUserId == null) {
            return null;
        }
        List<HistoricTaskInstance> tasks = bpmTaskService.getTaskListByProcessInstanceId(processInstanceId, true);
        if (CollUtil.isEmpty(tasks)) {
            return null;
        }
        return tasks.stream()
                .filter(task -> task.getEndTime() != null)
                .filter(task -> taskKey.equals(task.getTaskDefinitionKey()))
                .filter(task -> Objects.equals(String.valueOf(assigneeUserId), task.getAssignee()))
                .max(Comparator.comparing(HistoricTaskInstance::getEndTime)
                        .thenComparing(HistoricTaskInstance::getId))
                .orElse(null);
    }

    private SrmSampleEvaluationRespVO buildResp(SrmSampleEvaluationDO evaluation, boolean includeDetails) {
        SrmSampleEvaluationRespVO respVO = BeanUtils.toBean(evaluation, SrmSampleEvaluationRespVO.class);
        respVO.setVerificationTypes(splitValues(evaluation.getVerificationTypes()));
        respVO.setInspectionTypes(splitValues(evaluation.getInspectionTypes()));
        Long userId = currentUser().id();
        boolean maintainer = isMaintainer(evaluation, userId);
        SrmSampleEvaluationSignDO currentSign = signMapper.selectByEvaluationIdAndUserId(evaluation.getId(), userId);
        respVO.setCanEdit(maintainer && STATUS_DRAFT.equals(evaluation.getStatus()));
        respVO.setCanSubmit(maintainer && STATUS_DRAFT.equals(evaluation.getStatus()));
        respVO.setCanInspectionReport(STATUS_INSPECTION_REPORT.equals(evaluation.getStatus())
                && findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_INSPECTION_REPORT, userId) != null);
        respVO.setCanValueConfirm(Objects.equals(evaluation.getConfirmUserId(), userId)
                && STATUS_VALUE_CONFIRM.equals(evaluation.getStatus()));
        respVO.setCanSign(currentSign != null && STATUS_DEPT_SIGN.equals(evaluation.getStatus()));
        respVO.setCurrentSignId(currentSign == null ? null : currentSign.getId());
        respVO.setCanWithdrawConfirm(maintainer && STATUS_DEPT_SIGN.equals(evaluation.getStatus())
                && isDeptSignWithdrawable(evaluation.getId()));
        respVO.setCanInitiatorDecision(maintainer && STATUS_INITIATOR_CONFIRM.equals(evaluation.getStatus()));
        respVO.setCanFinalApprove(STATUS_FINAL_APPROVAL.equals(evaluation.getStatus())
                && findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_FINAL_APPROVAL, userId) != null);
        respVO.setCanArchiveConfirm(Objects.equals(evaluation.getInitiatorUserId(), userId)
                && STATUS_ARCHIVE_CONFIRM.equals(evaluation.getStatus())
                && findRunningTask(evaluation.getProcessInstanceId(), BPM_NODE_ARCHIVE_CONFIRM, userId) != null);
        respVO.setCanIssueTrialValidation(maintainer
                && trialValidationMapper.selectBySourceSampleEvaluationId(evaluation.getId()) == null);
        if (includeDetails) {
            respVO.setItems(itemMapper.selectListByEvaluationId(evaluation.getId()).stream().map(this::buildItemResp).toList());
            respVO.setSigns(signMapper.selectListByEvaluationId(evaluation.getId()).stream().map(this::buildSignResp).toList());
            respVO.setProjectUsers(buildProjectUserRespList(evaluation.getProjectId()));
            respVO.setLogs(logMapper.selectListByEvaluationId(evaluation.getId()).stream().map(this::buildLogResp).toList());
        }
        return respVO;
    }

    private SrmSampleEvaluationRespVO.Item buildItemResp(SrmSampleEvaluationItemDO item) {
        return BeanUtils.toBean(item, SrmSampleEvaluationRespVO.Item.class);
    }

    private SrmSampleEvaluationRespVO.Sign buildSignResp(SrmSampleEvaluationSignDO sign) {
        return BeanUtils.toBean(sign, SrmSampleEvaluationRespVO.Sign.class);
    }

    private SrmSampleEvaluationRespVO.Log buildLogResp(SrmSampleEvaluationLogDO log) {
        SrmSampleEvaluationRespVO.Log respVO = BeanUtils.toBean(log, SrmSampleEvaluationRespVO.Log.class);
        respVO.setActionName(actionText(log.getAction()));
        return respVO;
    }

    private String startBpmProcess(SrmSampleEvaluationDO evaluation) {
        Map<String, Object> variables = buildBpmVariables(evaluation);
        variables.put("needFinalApproval", false);
        variables.put("valueConfirmed", false);
        Map<String, List<Long>> startAssignees = new HashMap<>();
        if (evaluation.getAssignedInspectorUserId() != null) {
            startAssignees.put(BPM_NODE_INSPECTION_REPORT, List.of(evaluation.getAssignedInspectorUserId()));
        }
        startAssignees.put(BPM_NODE_VALUE_CONFIRM, List.of(evaluation.getInitiatorUserId()));
        startAssignees.put(BPM_NODE_ARCHIVE_CONFIRM, List.of(evaluation.getInitiatorUserId()));
        variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, startAssignees);
        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY)
                .setBusinessKey(String.valueOf(evaluation.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(startAssignees);
        try {
            return bpmProcessInstanceApi.createProcessInstance(evaluation.getInitiatorUserId(), createReq);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(evaluation.getInitiatorUserId(), createReq);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(SRM_SAMPLE_EVALUATION_BPM_NOT_PUBLISHED);
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
            throw exception(SRM_SAMPLE_EVALUATION_BPM_NOT_PUBLISHED);
        }
    }

    private void approveBpmTask(SrmSampleEvaluationDO evaluation, String taskKey, Long assigneeUserId,
                                String reason, Map<String, List<Long>> nextAssignees) {
        approveBpmTask(evaluation, taskKey, assigneeUserId, reason, nextAssignees, Map.of());
    }

    private void approveBpmTask(SrmSampleEvaluationDO evaluation, String taskKey, Long assigneeUserId,
                                String reason, Map<String, List<Long>> nextAssignees,
                                Map<String, Object> extraVariables) {
        if (StrUtil.isBlank(evaluation.getProcessInstanceId())) {
            throw exception(SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING);
        }
        Task task = findRunningTask(evaluation.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_SAMPLE_EVALUATION_BPM_TASK_MISSING);
        }
        Map<String, Object> variables = buildBpmVariables(evaluation);
        variables.putAll(extraVariables);
        BpmTaskApproveReqVO approveReq = new BpmTaskApproveReqVO()
                .setId(task.getId())
                .setReason(reason)
                .setVariables(variables);
        if (CollUtil.isNotEmpty(nextAssignees)) {
            approveReq.setNextAssignees(nextAssignees);
        }
        bpmTaskService.approveTask(assigneeUserId, approveReq);
    }

    private Task findRunningTask(String processInstanceId, String taskKey, Long assigneeUserId) {
        if (StrUtil.isBlank(processInstanceId) || assigneeUserId == null) {
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

    private Map<String, Object> finalApprovalVariables(List<Long> finalApprovalUserIds) {
        return Map.of("needFinalApproval", true,
                "finalApprovalUserIds", finalApprovalUserIds,
                BPM_VARIABLE_COLL_USER_LIST, finalApprovalUserIds);
    }

    private Map<String, Object> appendFinalApprovalVariables(Map<String, Object> variables,
                                                             List<Long> finalApprovalUserIds) {
        variables.putAll(finalApprovalVariables(finalApprovalUserIds));
        return variables;
    }

    private Map<String, Object> buildBpmVariables(SrmSampleEvaluationDO evaluation) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("sampleEvaluationId", evaluation.getId());
        variables.put("businessKey", evaluation.getId());
        variables.put("evaluationNo", evaluation.getEvaluationNo());
        variables.put("sampleRequestNo", evaluation.getSampleRequestNo());
        variables.put("projectId", evaluation.getProjectId());
        variables.put("projectCode", evaluation.getProjectCode());
        variables.put("projectName", evaluation.getProjectName());
        variables.put("supplierCode", evaluation.getSupplierCode());
        variables.put("supplierName", evaluation.getSupplierName());
        variables.put("materialName", evaluation.getMaterialName());
        variables.put("materialModel", evaluation.getMaterialModel());
        variables.put("initiatorId", evaluation.getInitiatorUserId());
        variables.put("assignedInspectorUserId", evaluation.getAssignedInspectorUserId());
        if (evaluation.getAssignedInspectorUserId() != null) {
            variables.put("reportUserIds", List.of(evaluation.getAssignedInspectorUserId()));
        }
        if (evaluation.getReporterUserId() != null) {
            variables.put("reportUserIds", List.of(evaluation.getReporterUserId()));
        }
        variables.put("confirmUserId", evaluation.getConfirmUserId());
        List<Long> signUserIds = signMapper.selectListByEvaluationId(evaluation.getId()).stream()
                .map(SrmSampleEvaluationSignDO::getUserId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isNotEmpty(signUserIds)) {
            variables.put("signUserIds", signUserIds);
            variables.put(BPM_VARIABLE_COLL_USER_LIST, signUserIds);
        }
        return variables;
    }

    private List<Long> resolveQualityReportUserIds() {
        Set<Long> permissionUserIds = permissionApi.getUserIdListByPermissions(List.of(QUALITY_REPORT_PERMISSION));
        if (CollUtil.isEmpty(permissionUserIds)) {
            throw exception(SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(permissionUserIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED);
        }
        return users.stream().map(AdminUserRespDTO::getId).distinct().toList();
    }

    private List<Long> resolveFinalApprovalUserIds() {
        Set<Long> permissionUserIds = permissionApi.getUserIdListByPermissions(List.of(FINAL_APPROVAL_PERMISSION));
        if (CollUtil.isEmpty(permissionUserIds)) {
            throw exception(SRM_SAMPLE_EVALUATION_FINAL_APPROVER_NOT_CONFIGURED);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(permissionUserIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(SRM_SAMPLE_EVALUATION_FINAL_APPROVER_NOT_CONFIGURED);
        }
        List<Long> userIds = users.stream().map(AdminUserRespDTO::getId)
                .filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(userIds)) {
            throw exception(SRM_SAMPLE_EVALUATION_FINAL_APPROVER_NOT_CONFIGURED);
        }
        return userIds;
    }

    private void updateStatus(SrmSampleEvaluationDO evaluation, String toStatus, String description, Object detail) {
        updateStatus(evaluation, toStatus, description, detail, resolveActionByStatus(toStatus));
    }

    private void updateStatus(SrmSampleEvaluationDO evaluation, String toStatus, String description, Object detail,
                              String action) {
        String fromStatus = evaluation.getStatus();
        evaluation.setStatus(toStatus);
        evaluation.setCurrentNodeName(statusText(toStatus));
        updateByIdChecked(evaluation);
        writeLog(evaluation.getId(), action, fromStatus, toStatus, description, detail);
    }

    private void updateByIdChecked(SrmSampleEvaluationDO evaluation) {
        if (sampleEvaluationMapper.updateById(evaluation) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    private SrmSampleEvaluationDO validateSampleEvaluation(Long id) {
        SrmSampleEvaluationDO evaluation = id == null ? null : sampleEvaluationMapper.selectById(id);
        if (evaluation == null) {
            throw exception(SRM_SAMPLE_EVALUATION_NOT_EXISTS);
        }
        return evaluation;
    }

    private void validateEvaluationNoUnique(Long id, String evaluationNo) {
        if (StrUtil.isBlank(evaluationNo)) {
            return;
        }
        SrmSampleEvaluationDO sameNo = sampleEvaluationMapper.selectByEvaluationNo(StrUtil.trim(evaluationNo));
        if (sameNo != null && !Objects.equals(sameNo.getId(), id)) {
            throw exception(SRM_SAMPLE_EVALUATION_NO_EXISTS);
        }
    }

    private String generateEvaluationNo() {
        String base = EVALUATION_NO_PREFIX + LocalDate.now().format(EVALUATION_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= EVALUATION_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String evaluationNo = base + String.format("%03d", sequence);
            if (sampleEvaluationMapper.selectByEvaluationNo(evaluationNo) == null) {
                return evaluationNo;
            }
        }
        throw exception(SRM_SAMPLE_EVALUATION_NO_EXISTS);
    }

    private void assertStatus(SrmSampleEvaluationDO evaluation, String expectedStatus) {
        if (!expectedStatus.equals(evaluation.getStatus())) {
            throw exception(SRM_SAMPLE_EVALUATION_STATUS_INVALID);
        }
    }

    private void assertMaintainer(SrmSampleEvaluationDO evaluation) {
        Long userId = currentUser().id();
        if (!isMaintainer(evaluation, userId)) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
    }

    private boolean isMaintainer(SrmSampleEvaluationDO evaluation, Long userId) {
        return Objects.equals(evaluation.getInitiatorUserId(), userId) || isSrmAdmin(userId);
    }

    private void assertCurrentOperator(Long expectedUserId) {
        Long userId = currentUser().id();
        if (!Objects.equals(expectedUserId, userId)) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
    }

    private boolean isSrmAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, SRM_SUPER_ADMIN_ROLE, EVALUATION_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private UserSnapshot resolveUser(Long userId, String fallbackName) {
        if (userId == null) {
            throw exception(SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(SRM_SAMPLE_EVALUATION_REVIEWER_REQUIRED);
        }
        return new UserSnapshot(userId, userName(user, fallbackName));
    }

    private String resolveUserName(Long userId, String fallbackName) {
        if (userId == null) {
            return StrUtil.trim(fallbackName);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return userName(user, fallbackName);
    }

    private String userName(AdminUserRespDTO user, String fallbackName) {
        if (user == null) {
            return StrUtil.trim(fallbackName);
        }
        return StrUtil.blankToDefault(user.getNickname(), StrUtil.blankToDefault(user.getUsername(), fallbackName));
    }

    private CurrentUser currentUserWithDept() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        AdminUserRespDTO user = userId == null ? null : adminUserApi.getUser(userId);
        userName = StrUtil.blankToDefault(userName, userName(user, null));
        Long deptId = user == null ? null : user.getDeptId();
        return new CurrentUser(userId, StrUtil.blankToDefault(userName, "系统用户"), deptId, resolveDeptName(deptId));
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        DeptRespDTO dept = deptApi.getDept(deptId);
        return dept == null ? null : dept.getName();
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

    private void writeLog(Long evaluationId, String action, String fromStatus, String toStatus,
                          String description, Object detail) {
        UserSnapshot user = currentUser();
        SrmSampleEvaluationLogDO log = new SrmSampleEvaluationLogDO();
        log.setEvaluationId(evaluationId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setActionDescription(description);
        log.setDetailJson(detail == null ? null : JsonUtils.toJsonString(detail));
        logMapper.insert(log);
    }

    private boolean requiresProductionAttachment(SrmSampleEvaluationSignDO sign) {
        return Boolean.TRUE.equals(sign.getRequireAttachment()) || containsProductionDept(sign.getDeptName());
    }

    private boolean containsProductionDept(String deptName) {
        return StrUtil.contains(StrUtil.nullToDefault(deptName, ""), "生产");
    }

    private String joinValues(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return String.join(",", values.stream().filter(StrUtil::isNotBlank).map(StrUtil::trim).distinct().toList());
    }

    private Long firstLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<String> splitValues(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return StrUtil.split(value, ',').stream().map(StrUtil::trim).filter(StrUtil::isNotBlank).toList();
    }

    private String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_DRAFT)) {
            case STATUS_INSPECTION_REPORT -> "执行检测";
            case STATUS_VALUE_CONFIRM -> "检测结果确认";
            case STATUS_DEPT_SIGN -> "会签";
            case STATUS_INITIATOR_CONFIRM -> "发起人确认";
            case STATUS_FINAL_APPROVAL -> "最终批准";
            case STATUS_ARCHIVE_CONFIRM -> "归档";
            case STATUS_ARCHIVED -> "已归档";
            default -> "草稿";
        };
    }

    private String resolveActionByStatus(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_DRAFT)) {
            case STATUS_INSPECTION_REPORT -> "SUBMIT";
            case STATUS_VALUE_CONFIRM -> "INSPECTION_REPORT";
            case STATUS_DEPT_SIGN -> "VALUE_CONFIRM";
            case STATUS_INITIATOR_CONFIRM -> "SIGN_COMPLETE";
            case STATUS_FINAL_APPROVAL -> "INITIATOR_SEND_APPROVAL";
            case STATUS_ARCHIVE_CONFIRM -> "FINAL_APPROVE";
            case STATUS_ARCHIVED -> "ARCHIVE_CONFIRM";
            default -> "UPDATE";
        };
    }

    private String actionText(String action) {
        return switch (StrUtil.blankToDefault(action, "")) {
            case "CREATE" -> "创建";
            case "UPDATE" -> "更新";
            case "SUBMIT" -> "提交";
            case "INSPECTION_REPORT" -> "执行检测";
            case "VALUE_CONFIRM" -> "检测结果确认";
            case "WITHDRAW_CONFIRM" -> "会签前撤回";
            case "SIGN" -> "会签";
            case "SIGN_COMPLETE" -> "会签完成";
            case "INITIATOR_SEND_APPROVAL" -> "发起人送审";
            case "ARCHIVE_CONFIRM" -> "完成归档";
            case "FINAL_APPROVE" -> "最终批准";
            case "ARCHIVE" -> "归档";
            default -> action;
        };
    }

    private record UserSnapshot(Long id, String name) {
    }

    private record CurrentUser(Long id, String name, Long deptId, String deptName) {
    }

}
