package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmTrialValidationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmTrialValidationLogDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleEvaluationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmTrialValidationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmTrialValidationMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_EVALUATION_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_BPM_TASK_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_REVIEWER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_TRIAL_VALIDATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmTrialValidationServiceImpl implements SrmTrialValidationService {

    private static final String STATUS_NOTICE_SENT = "NOTICE_SENT";
    private static final String STATUS_TRIAL_EXECUTION = "TRIAL_EXECUTION";
    private static final String STATUS_PRODUCTION_COMPLETE = "PRODUCTION_COMPLETE";
    private static final String STATUS_ARCHIVE_CONFIRM = "ARCHIVE_CONFIRM";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String BPM_PROCESS_KEY = "srm_trial_validation";
    private static final String BPM_MODEL_ID = "srm-trial-validation-model";
    private static final String BPM_NODE_START = "StartUserNode";
    private static final String BPM_NODE_TRIAL_EXECUTION = "trial_execution";
    private static final String BPM_NODE_PRODUCTION_COMPLETE = "production_complete";
    private static final String BPM_NODE_ARCHIVE_CONFIRM = "archive_confirm";
    private static final String BPM_VARIABLE_COLL_USER_LIST = "coll_userList";
    private static final String BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES = "PROCESS_APPROVE_USER_SELECT_ASSIGNEES";
    private static final String TRIAL_EXECUTION_PERMISSION = "mes:srm-certification-trial-validation:execute";
    private static final String PRODUCTION_COMPLETE_PERMISSION = "mes:srm-certification-trial-validation:complete";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String SRM_SUPER_ADMIN_ROLE = "srm_supplier_super_admin";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String TRIAL_NO_PREFIX = "SCYZ-";
    private static final DateTimeFormatter TRIAL_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int TRIAL_NO_DAILY_SEQUENCE_LIMIT = 999;
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1_009_003_002;

    @Resource
    private SrmTrialValidationMapper trialValidationMapper;
    @Resource
    private SrmTrialValidationLogMapper logMapper;
    @Resource
    private SrmSampleEvaluationMapper sampleEvaluationMapper;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long issueFromSampleEvaluation(Long sampleEvaluationId) {
        SrmSampleEvaluationDO evaluation = validateSampleEvaluation(sampleEvaluationId);
        assertSampleEvaluationMaintainer(evaluation);
        validateSampleEvaluationTrialFields(evaluation);
        if (trialValidationMapper.selectBySourceSampleEvaluationId(evaluation.getId()) != null) {
            throw exception(SRM_TRIAL_VALIDATION_DUPLICATE);
        }

        UserSnapshot user = currentUser();
        SrmTrialValidationDO trial = new SrmTrialValidationDO();
        trial.setTrialNo(generateTrialNo());
        trial.setSourceSampleEvaluationId(evaluation.getId());
        trial.setSourceSampleEvaluationNo(evaluation.getEvaluationNo());
        trial.setSupplierId(evaluation.getSupplierId());
        trial.setSupplierCode(evaluation.getSupplierCode());
        trial.setSupplierName(evaluation.getSupplierName());
        trial.setMaterialCode(null);
        trial.setMaterialName(evaluation.getMaterialName());
        trial.setMaterialModel(evaluation.getMaterialModel());
        trial.setQuantity(evaluation.getSampleQty());
        trial.setStatus(STATUS_NOTICE_SENT);
        trial.setCurrentNodeName(statusText(STATUS_NOTICE_SENT));
        trial.setInitiatorUserId(user.id());
        trial.setInitiatorUserName(user.name());
        trial.setNoticeTime(LocalDateTime.now());
        trial.setVersion(0);
        trialValidationMapper.insert(trial);
        writeLog(trial.getId(), "CREATE", null, STATUS_NOTICE_SENT, "从样品评价下达试生产通知",
                Map.of("sourceSampleEvaluationId", evaluation.getId(),
                        "sourceSampleEvaluationNo", evaluation.getEvaluationNo()));

        List<Long> trialExecutionUserIds = resolvePermissionUserIds(TRIAL_EXECUTION_PERMISSION,
                SRM_TRIAL_VALIDATION_REVIEWER_REQUIRED);
        String processInstanceId = startBpmProcess(trial, trialExecutionUserIds);
        trial.setProcessInstanceId(processInstanceId);
        updateByIdChecked(trial);
        approveBpmTask(trial, BPM_NODE_START, trial.getInitiatorUserId(), "下达试生产通知",
                nextAssignees(BPM_NODE_TRIAL_EXECUTION, trialExecutionUserIds),
                Map.of("trialExecutionUserIds", trialExecutionUserIds,
                        BPM_VARIABLE_COLL_USER_LIST, trialExecutionUserIds));
        updateStatus(trial, STATUS_TRIAL_EXECUTION, "下达试生产通知，进入试生产执行",
                Map.of("trialExecutionUserIds", trialExecutionUserIds));
        return trial.getId();
    }

    @Override
    public SrmTrialValidationRespVO getTrialValidation(Long id) {
        return buildResp(validateTrialValidation(id), true);
    }

    @Override
    public PageResult<SrmTrialValidationRespVO> getTrialValidationPage(SrmTrialValidationPageReqVO reqVO) {
        PageResult<SrmTrialValidationDO> page = trialValidationMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(),
                page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trialExecute(SrmTrialValidationActionReqVO.TrialExecute reqVO) {
        SrmTrialValidationDO trial = validateTrialValidation(reqVO.getId());
        assertStatus(trial, STATUS_TRIAL_EXECUTION);
        UserSnapshot user = currentUser();
        Task task = findRunningTask(trial.getProcessInstanceId(), BPM_NODE_TRIAL_EXECUTION, user.id());
        if (task == null) {
            throw exception(SRM_TRIAL_VALIDATION_BPM_TASK_MISSING);
        }
        applyTrialResult(trial, reqVO.getMaterialBatchNo(), reqVO.getQuantity());
        trial.setTrialExecutionUserId(user.id());
        trial.setTrialExecutionUserName(user.name());
        trial.setTrialExecutionTime(LocalDateTime.now());
        trial.setTrialExecutionOpinion(StrUtil.trim(reqVO.getOpinion()));

        List<Long> productionCompleteUserIds = resolvePermissionUserIds(PRODUCTION_COMPLETE_PERMISSION,
                SRM_TRIAL_VALIDATION_REVIEWER_REQUIRED);
        approveBpmTask(trial, BPM_NODE_TRIAL_EXECUTION, user.id(), "试生产执行",
                nextAssignees(BPM_NODE_PRODUCTION_COMPLETE, productionCompleteUserIds),
                Map.of("productionCompleteUserIds", productionCompleteUserIds,
                        BPM_VARIABLE_COLL_USER_LIST, productionCompleteUserIds));
        updateStatus(trial, STATUS_PRODUCTION_COMPLETE, "试生产执行完成，进入完成生产",
                Map.of("productionCompleteUserIds", productionCompleteUserIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeProduction(SrmTrialValidationActionReqVO.ProductionComplete reqVO) {
        SrmTrialValidationDO trial = validateTrialValidation(reqVO.getId());
        assertStatus(trial, STATUS_PRODUCTION_COMPLETE);
        UserSnapshot user = currentUser();
        Task task = findRunningTask(trial.getProcessInstanceId(), BPM_NODE_PRODUCTION_COMPLETE, user.id());
        if (task == null) {
            throw exception(SRM_TRIAL_VALIDATION_BPM_TASK_MISSING);
        }
        applyTrialResult(trial, reqVO.getMaterialBatchNo(), reqVO.getQuantity());
        trial.setProductionCompleteUserId(user.id());
        trial.setProductionCompleteUserName(user.name());
        trial.setProductionCompleteTime(LocalDateTime.now());
        trial.setProductionCompleteOpinion(StrUtil.trim(reqVO.getOpinion()));

        approveBpmTask(trial, BPM_NODE_PRODUCTION_COMPLETE, user.id(), "完成生产",
                nextAssignees(BPM_NODE_ARCHIVE_CONFIRM, List.of(trial.getInitiatorUserId())),
                Map.of("archiveUserIds", List.of(trial.getInitiatorUserId())));
        updateStatus(trial, STATUS_ARCHIVE_CONFIRM, "完成生产，返回发起人确认归档", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveConfirm(SrmTrialValidationActionReqVO.ArchiveConfirm reqVO) {
        SrmTrialValidationDO trial = validateTrialValidation(reqVO.getId());
        assertStatus(trial, STATUS_ARCHIVE_CONFIRM);
        UserSnapshot user = currentUser();
        if (!Objects.equals(trial.getInitiatorUserId(), user.id())) {
            throw exception(SRM_TRIAL_VALIDATION_OPERATOR_ONLY);
        }
        Task task = findRunningTask(trial.getProcessInstanceId(), BPM_NODE_ARCHIVE_CONFIRM, user.id());
        if (task == null) {
            throw exception(SRM_TRIAL_VALIDATION_BPM_TASK_MISSING);
        }
        trial.setArchiveUserId(user.id());
        trial.setArchiveUserName(user.name());
        trial.setArchiveTime(LocalDateTime.now());
        trial.setArchiveOpinion(StrUtil.trim(reqVO.getOpinion()));
        approveBpmTask(trial, BPM_NODE_ARCHIVE_CONFIRM, user.id(), "确认归档", null, Map.of());
        updateStatus(trial, STATUS_ARCHIVED, "发起人确认归档完成", null);
    }

    private void applyTrialResult(SrmTrialValidationDO trial, String materialBatchNo, BigDecimal quantity) {
        trial.setMaterialBatchNo(StrUtil.trim(materialBatchNo));
        trial.setQuantity(quantity);
    }

    private String startBpmProcess(SrmTrialValidationDO trial, List<Long> trialExecutionUserIds) {
        Map<String, Object> variables = buildBpmVariables(trial);
        variables.put("trialExecutionUserIds", trialExecutionUserIds);
        variables.put(BPM_VARIABLE_COLL_USER_LIST, trialExecutionUserIds);
        Map<String, List<Long>> startAssignees = new HashMap<>();
        startAssignees.put(BPM_NODE_TRIAL_EXECUTION, trialExecutionUserIds);
        startAssignees.put(BPM_NODE_ARCHIVE_CONFIRM, List.of(trial.getInitiatorUserId()));
        variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, startAssignees);
        BpmProcessInstanceCreateReqDTO request = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY)
                .setBusinessKey(String.valueOf(trial.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(startAssignees);
        try {
            return bpmProcessInstanceApi.createProcessInstance(trial.getInitiatorUserId(), request);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(trial.getInitiatorUserId(), request);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(SRM_TRIAL_VALIDATION_BPM_NOT_PUBLISHED);
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
            throw exception(SRM_TRIAL_VALIDATION_BPM_NOT_PUBLISHED);
        }
    }

    private void approveBpmTask(SrmTrialValidationDO trial, String taskKey, Long assigneeUserId, String reason,
                                Map<String, List<Long>> nextAssignees, Map<String, Object> extraVariables) {
        if (StrUtil.isBlank(trial.getProcessInstanceId())) {
            throw exception(SRM_TRIAL_VALIDATION_BPM_TASK_MISSING);
        }
        Task task = findRunningTask(trial.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_TRIAL_VALIDATION_BPM_TASK_MISSING);
        }
        Map<String, Object> variables = buildBpmVariables(trial);
        variables.putAll(extraVariables);
        if (CollUtil.isNotEmpty(nextAssignees)) {
            variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, nextAssignees);
        }
        BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                .setId(task.getId())
                .setReason(reason)
                .setVariables(variables);
        if (CollUtil.isNotEmpty(nextAssignees)) {
            reqVO.setNextAssignees(nextAssignees);
        }
        bpmTaskService.approveTask(assigneeUserId, reqVO);
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

    private Map<String, Object> buildBpmVariables(SrmTrialValidationDO trial) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("trialValidationId", trial.getId());
        variables.put("businessKey", trial.getId());
        variables.put("trialNo", trial.getTrialNo());
        variables.put("sourceSampleEvaluationId", trial.getSourceSampleEvaluationId());
        variables.put("sourceSampleEvaluationNo", trial.getSourceSampleEvaluationNo());
        variables.put("supplierId", trial.getSupplierId());
        variables.put("supplierCode", trial.getSupplierCode());
        variables.put("supplierName", trial.getSupplierName());
        variables.put("materialId", trial.getMaterialId());
        variables.put("materialCode", trial.getMaterialCode());
        variables.put("materialName", trial.getMaterialName());
        variables.put("materialModel", trial.getMaterialModel());
        variables.put("materialBatchNo", trial.getMaterialBatchNo());
        variables.put("quantity", trial.getQuantity());
        variables.put("initiatorId", trial.getInitiatorUserId());
        variables.put("initiatorUserName", trial.getInitiatorUserName());
        return variables;
    }

    private SrmTrialValidationRespVO buildResp(SrmTrialValidationDO trial, boolean includeDetails) {
        SrmTrialValidationRespVO respVO = BeanUtils.toBean(trial, SrmTrialValidationRespVO.class);
        Long userId = currentUser().id();
        respVO.setCanTrialExecute(STATUS_TRIAL_EXECUTION.equals(trial.getStatus())
                && findRunningTask(trial.getProcessInstanceId(), BPM_NODE_TRIAL_EXECUTION, userId) != null);
        respVO.setCanCompleteProduction(STATUS_PRODUCTION_COMPLETE.equals(trial.getStatus())
                && findRunningTask(trial.getProcessInstanceId(), BPM_NODE_PRODUCTION_COMPLETE, userId) != null);
        respVO.setCanArchiveConfirm(STATUS_ARCHIVE_CONFIRM.equals(trial.getStatus())
                && Objects.equals(trial.getInitiatorUserId(), userId)
                && findRunningTask(trial.getProcessInstanceId(), BPM_NODE_ARCHIVE_CONFIRM, userId) != null);
        if (includeDetails) {
            respVO.setLogs(logMapper.selectListByTrialId(trial.getId()).stream().map(this::buildLogResp).toList());
        }
        return respVO;
    }

    private SrmTrialValidationRespVO.Log buildLogResp(SrmTrialValidationLogDO log) {
        SrmTrialValidationRespVO.Log respVO = BeanUtils.toBean(log, SrmTrialValidationRespVO.Log.class);
        respVO.setActionName(actionText(log.getAction()));
        return respVO;
    }

    private void updateStatus(SrmTrialValidationDO trial, String toStatus, String description, Object detail) {
        String fromStatus = trial.getStatus();
        trial.setStatus(toStatus);
        trial.setCurrentNodeName(statusText(toStatus));
        updateByIdChecked(trial);
        writeLog(trial.getId(), resolveActionByStatus(toStatus), fromStatus, toStatus, description, detail);
    }

    private void updateByIdChecked(SrmTrialValidationDO trial) {
        if (trialValidationMapper.updateById(trial) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    private SrmTrialValidationDO validateTrialValidation(Long id) {
        SrmTrialValidationDO trial = id == null ? null : trialValidationMapper.selectById(id);
        if (trial == null) {
            throw exception(SRM_TRIAL_VALIDATION_NOT_EXISTS);
        }
        return trial;
    }

    private SrmSampleEvaluationDO validateSampleEvaluation(Long id) {
        SrmSampleEvaluationDO evaluation = id == null ? null : sampleEvaluationMapper.selectById(id);
        if (evaluation == null) {
            throw exception(SRM_SAMPLE_EVALUATION_NOT_EXISTS);
        }
        return evaluation;
    }

    private void validateSampleEvaluationTrialFields(SrmSampleEvaluationDO evaluation) {
        if (StrUtil.isBlank(evaluation.getSupplierCode())
                || StrUtil.isBlank(evaluation.getSupplierName())
                || StrUtil.isBlank(evaluation.getMaterialName())
                || evaluation.getSampleQty() == null
                || evaluation.getSampleQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(SRM_TRIAL_VALIDATION_SOURCE_REQUIRED);
        }
    }

    private void assertSampleEvaluationMaintainer(SrmSampleEvaluationDO evaluation) {
        Long userId = currentUser().id();
        if (!Objects.equals(evaluation.getInitiatorUserId(), userId) && !isSrmAdmin(userId)) {
            throw exception(SRM_SAMPLE_EVALUATION_OPERATOR_ONLY);
        }
    }

    private void assertStatus(SrmTrialValidationDO trial, String expectedStatus) {
        if (!expectedStatus.equals(trial.getStatus())) {
            throw exception(SRM_TRIAL_VALIDATION_STATUS_INVALID);
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

    private List<Long> resolvePermissionUserIds(String permission, ErrorCode errorCode) {
        Set<Long> permissionUserIds = permissionApi.getUserIdListByPermissions(List.of(permission));
        if (CollUtil.isEmpty(permissionUserIds)) {
            throw exception(errorCode);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(permissionUserIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(errorCode);
        }
        List<Long> userIds = users.stream().map(AdminUserRespDTO::getId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(userIds)) {
            throw exception(errorCode);
        }
        return userIds;
    }

    private String generateTrialNo() {
        String base = TRIAL_NO_PREFIX + LocalDate.now().format(TRIAL_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= TRIAL_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String trialNo = base + String.format("%03d", sequence);
            if (trialValidationMapper.selectByTrialNo(trialNo) == null) {
                return trialNo;
            }
        }
        throw exception(SRM_TRIAL_VALIDATION_NO_EXISTS);
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

    private void writeLog(Long trialId, String action, String fromStatus, String toStatus,
                          String description, Object detail) {
        UserSnapshot user = currentUser();
        SrmTrialValidationLogDO log = new SrmTrialValidationLogDO();
        log.setTrialId(trialId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setActionDescription(description);
        log.setDetailJson(detail == null ? null : JsonUtils.toJsonString(detail));
        logMapper.insert(log);
    }

    private String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_NOTICE_SENT)) {
            case STATUS_TRIAL_EXECUTION -> "试生产执行";
            case STATUS_PRODUCTION_COMPLETE -> "完成生产";
            case STATUS_ARCHIVE_CONFIRM -> "确认归档";
            case STATUS_ARCHIVED -> "已归档";
            default -> "发起试生产通知";
        };
    }

    private String resolveActionByStatus(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_NOTICE_SENT)) {
            case STATUS_TRIAL_EXECUTION -> "ISSUE_NOTICE";
            case STATUS_PRODUCTION_COMPLETE -> "TRIAL_EXECUTE";
            case STATUS_ARCHIVE_CONFIRM -> "PRODUCTION_COMPLETE";
            case STATUS_ARCHIVED -> "ARCHIVE";
            default -> "UPDATE";
        };
    }

    private String actionText(String action) {
        return switch (StrUtil.blankToDefault(action, "")) {
            case "CREATE" -> "创建";
            case "UPDATE" -> "更新";
            case "ISSUE_NOTICE" -> "下达试生产通知";
            case "TRIAL_EXECUTE" -> "试生产执行";
            case "PRODUCTION_COMPLETE" -> "完成生产";
            case "ARCHIVE" -> "确认归档";
            default -> action;
        };
    }

    private record UserSnapshot(Long id, String name) {
    }

}
