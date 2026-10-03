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
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSupplierPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSupplierRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleRequestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleRequestLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmTrialValidationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleRequestLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSampleRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmTrialValidationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_BPM_TASK_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_REVIEWER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SAMPLE_REQUEST_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmSampleRequestServiceImpl implements SrmSampleRequestService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PROJECT_REVIEW = "PROJECT_REVIEW";
    private static final String STATUS_PURCHASE_REVIEW = "PURCHASE_REVIEW";
    private static final String STATUS_INITIATOR_CONFIRM = "INITIATOR_CONFIRM";
    private static final String STATUS_FINAL_APPROVAL = "FINAL_APPROVAL";
    private static final String STATUS_ARCHIVE_CONFIRM = "ARCHIVE_CONFIRM";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String SUPPLIER_STATUS_QUALIFIED = "QUALIFIED";
    private static final String BPM_PROCESS_KEY = "srm_sample_request";
    private static final String BPM_MODEL_ID = "srm-sample-request-model";
    private static final String BPM_NODE_START = "StartUserNode";
    private static final String BPM_NODE_PROJECT_REVIEW = "project_review";
    private static final String BPM_NODE_PURCHASE_REVIEW = "purchase_review";
    private static final String BPM_NODE_INITIATOR_DECISION = "initiator_decision";
    private static final String BPM_NODE_FINAL_APPROVAL = "final_approval";
    private static final String BPM_NODE_ARCHIVE_CONFIRM = "archive_confirm";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1_009_003_002;
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String SRM_SUPER_ADMIN_ROLE = "srm_supplier_super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String REQUEST_NO_PREFIX = "SRM-SR-";
    private static final DateTimeFormatter REQUEST_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int REQUEST_NO_DAILY_SEQUENCE_LIMIT = 999;
    private static final String DIRECT_ARCHIVE_FORM_TEXT = "直接归档表单，具体内容见附件";

    @Resource
    private SrmSampleRequestMapper sampleRequestMapper;
    @Resource
    private SrmSampleRequestLogMapper logMapper;
    @Resource
    private MesSupplierMapper supplierMapper;
    @Resource
    private SrmTrialValidationMapper trialValidationMapper;
    @Resource
    private SrmSupplierScopeService supplierScopeService;
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
    public Long createSampleRequest(SrmSampleRequestSaveReqVO reqVO) {
        String requestNo = generateRequestNo();
        validateRequestNoUnique(null, requestNo);
        UserSnapshot user = currentUser();
        SrmSampleRequestDO request = new SrmSampleRequestDO();
        copyEditableFields(reqVO, request);
        request.setRequestNo(requestNo);
        boolean directArchive = Boolean.TRUE.equals(reqVO.getDirectArchive());
        request.setStatus(directArchive ? STATUS_ARCHIVED : STATUS_DRAFT);
        request.setCurrentNodeName(statusText(request.getStatus()));
        request.setApplicantId(user.id());
        request.setApplicantName(user.name());
        request.setApplyDate(request.getApplyDate() == null ? LocalDate.now() : request.getApplyDate());
        if (directArchive) {
            request.setArchiveOpinion(DIRECT_ARCHIVE_FORM_TEXT);
            request.setArchiveTime(LocalDateTime.now());
        }
        request.setVersion(0);
        sampleRequestMapper.insert(request);
        writeLog(request.getId(), directArchive ? "DIRECT_ARCHIVE_FORM" : "CREATE", null, request.getStatus(),
                directArchive ? DIRECT_ARCHIVE_FORM_TEXT : "创建样品需求单", null);
        return request.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSampleRequest(SrmSampleRequestSaveReqVO reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertMaintainer(request);
        assertStatus(request, STATUS_DRAFT);
        copyEditableFields(reqVO, request);
        request.setVersion(reqVO.getVersion());
        updateByIdChecked(request);
        writeLog(request.getId(), "UPDATE", STATUS_DRAFT, STATUS_DRAFT, "更新样品需求单", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSampleRequest(Long id) {
        SrmSampleRequestDO request = validateSampleRequest(id);
        assertMaintainer(request);
        assertStatus(request, STATUS_DRAFT);
        sampleRequestMapper.deleteById(id);
    }

    @Override
    public SrmSampleRequestRespVO getSampleRequest(Long id) {
        return buildResp(validateSampleRequest(id), true);
    }

    @Override
    public PageResult<SrmSampleRequestRespVO> getSampleRequestPage(SrmSampleRequestPageReqVO reqVO) {
        PageResult<SrmSampleRequestDO> page = sampleRequestMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(),
                page.getTotal());
    }

    @Override
    public PageResult<SrmSampleRequestSupplierRespVO> getSupplierPage(SrmSampleRequestSupplierPageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        LambdaQueryWrapper<MesSupplierDO> wrapper = new LambdaQueryWrapperX<MesSupplierDO>()
                .eq(MesSupplierDO::getStatus, SUPPLIER_STATUS_QUALIFIED)
                .likeIfPresent(MesSupplierDO::getSupplierCode, StrUtil.trimToNull(reqVO.getSupplierCode()))
                .likeIfPresent(MesSupplierDO::getSupplierName, StrUtil.trimToNull(reqVO.getSupplierName()))
                .orderByAsc(MesSupplierDO::getSupplierCode)
                .orderByAsc(MesSupplierDO::getSupplierName)
                .orderByDesc(MesSupplierDO::getId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(query -> query.like(MesSupplierDO::getSupplierCode, keyword)
                    .or()
                    .like(MesSupplierDO::getSupplierName, keyword));
        }
        Collection<Long> scopeIds = supplierScopeService.getCurrentAccessibleScopeIds();
        if (scopeIds != null) {
            wrapper.in(MesSupplierDO::getScopeId, CollUtil.isEmpty(scopeIds) ? List.of(-1L) : scopeIds);
        }

        LinkedHashMap<String, SrmSampleRequestSupplierRespVO> uniqueSuppliers = new LinkedHashMap<>();
        supplierMapper.selectList(wrapper).forEach(supplier -> {
            String supplierCode = StrUtil.trimToEmpty(supplier.getSupplierCode());
            String supplierName = StrUtil.trimToEmpty(supplier.getSupplierName());
            if (StrUtil.isBlank(supplierCode) && StrUtil.isBlank(supplierName)) {
                return;
            }
            String key = supplierCode + "\u0001" + supplierName;
            uniqueSuppliers.computeIfAbsent(key, ignored -> {
                SrmSampleRequestSupplierRespVO respVO = new SrmSampleRequestSupplierRespVO();
                respVO.setSupplierKey(key);
                respVO.setSupplierId(supplier.getId());
                respVO.setSupplierCode(supplierCode);
                respVO.setSupplierName(supplierName);
                return respVO;
            });
        });
        List<SrmSampleRequestSupplierRespVO> rows = new ArrayList<>(uniqueSuppliers.values());
        int pageNo = Math.max(reqVO.getPageNo(), 1);
        int pageSize = Math.max(reqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(fromIndex >= toIndex ? List.of() : rows.subList(fromIndex, toIndex),
                (long) rows.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(SrmSampleRequestActionReqVO.Submit reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertMaintainer(request);
        assertStatus(request, STATUS_DRAFT);
        validateRequiredReviewers(request);
        String processInstanceId = startBpmProcess(request);
        request.setProcessInstanceId(processInstanceId);
        approveBpmTask(request, BPM_NODE_START, request.getApplicantId(), "提交样品需求单",
                nextAssignees(BPM_NODE_PROJECT_REVIEW, List.of(request.getProjectLeaderUserId())));
        updateStatus(request, STATUS_PROJECT_REVIEW, "提交样品需求单，进入项目负责人审核", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void projectReview(SrmSampleRequestActionReqVO.Review reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertStatus(request, STATUS_PROJECT_REVIEW);
        assertCurrentOperator(request.getProjectLeaderUserId());
        Long purchaseReviewerUserId = reqVO.getNextPurchaseOwnerUserId() == null
                ? request.getPurchaseOwnerUserId()
                : reqVO.getNextPurchaseOwnerUserId();
        if (purchaseReviewerUserId == null) {
            throw exception(SRM_SAMPLE_REQUEST_REVIEWER_REQUIRED);
        }
        UserSnapshot purchaseReviewer = resolveUser(purchaseReviewerUserId, reqVO.getNextPurchaseOwnerUserName());
        request.setPurchaseOwnerUserId(purchaseReviewer.id());
        if (StrUtil.isBlank(request.getPurchaseOwnerUserName())) {
            request.setPurchaseOwnerUserName(purchaseReviewer.name());
        }
        request.setProjectLeaderOpinion(StrUtil.trim(reqVO.getOpinion()));
        request.setProjectLeaderHandleTime(LocalDateTime.now());
        approveBpmTask(request, BPM_NODE_PROJECT_REVIEW, request.getProjectLeaderUserId(), "项目负责人审核",
                nextAssignees(BPM_NODE_PURCHASE_REVIEW, List.of(purchaseReviewer.id())));
        updateStatus(request, STATUS_PURCHASE_REVIEW, "项目负责人审核完成，进入采购负责人审核", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseReview(SrmSampleRequestActionReqVO.Review reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertStatus(request, STATUS_PURCHASE_REVIEW);
        assertCurrentOperator(request.getPurchaseOwnerUserId());
        request.setPurchaseDifficulty(StrUtil.trim(reqVO.getPurchaseDifficulty()));
        request.setPurchaseOwnerOpinion(StrUtil.trim(reqVO.getOpinion()));
        request.setPurchaseOwnerHandleTime(LocalDateTime.now());
        approveBpmTask(request, BPM_NODE_PURCHASE_REVIEW, request.getPurchaseOwnerUserId(), "采购负责人审核",
                nextAssignees(BPM_NODE_INITIATOR_DECISION, List.of(request.getApplicantId())));
        updateStatus(request, STATUS_INITIATOR_CONFIRM, "采购负责人审核完成，返回发起人确认", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initiatorDecision(SrmSampleRequestActionReqVO.InitiatorDecision reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertMaintainer(request);
        assertStatus(request, STATUS_INITIATOR_CONFIRM);
        boolean directArchive = Boolean.TRUE.equals(reqVO.getDirectArchive());
        if (directArchive) {
            request.setArchiveOpinion(StrUtil.trim(reqVO.getOpinion()));
            request.setArchiveTime(LocalDateTime.now());
            approveBpmTask(request, BPM_NODE_INITIATOR_DECISION, request.getApplicantId(), "发起人确认归档",
                    null, Map.of("needFinalApproval", false));
            updateStatus(request, STATUS_ARCHIVED, "发起人确认直接归档", null);
            return;
        }
        if (reqVO.getFinalApproverUserId() == null) {
            throw exception(SRM_SAMPLE_REQUEST_REVIEWER_REQUIRED);
        }
        UserSnapshot approver = resolveUser(reqVO.getFinalApproverUserId(), reqVO.getFinalApproverUserName());
        request.setFinalApproverUserId(approver.id());
        request.setFinalApproverUserName(approver.name());
        request.setArchiveOpinion(StrUtil.trim(reqVO.getOpinion()));
        approveBpmTask(request, BPM_NODE_INITIATOR_DECISION, request.getApplicantId(), "发起人提交最终批准",
                nextAssignees(BPM_NODE_FINAL_APPROVAL, List.of(approver.id())),
                Map.of("needFinalApproval", true, "finalApproverUserId", approver.id()));
        updateStatus(request, STATUS_FINAL_APPROVAL, "发起人选择最终批准人", Map.of("finalApproverUserId", approver.id()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalApprove(SrmSampleRequestActionReqVO.Review reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertStatus(request, STATUS_FINAL_APPROVAL);
        assertCurrentOperator(request.getFinalApproverUserId());
        request.setFinalApproverOpinion(StrUtil.trim(reqVO.getOpinion()));
        request.setFinalApproverHandleTime(LocalDateTime.now());
        approveBpmTask(request, BPM_NODE_FINAL_APPROVAL, request.getFinalApproverUserId(), "最终批准人审核",
                nextAssignees(BPM_NODE_ARCHIVE_CONFIRM, List.of(request.getApplicantId())));
        updateStatus(request, STATUS_ARCHIVE_CONFIRM, "最终批准人审核完成，返回发起人归档", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveConfirm(SrmSampleRequestActionReqVO.ArchiveConfirm reqVO) {
        SrmSampleRequestDO request = validateSampleRequest(reqVO.getId());
        assertMaintainer(request);
        assertStatus(request, STATUS_ARCHIVE_CONFIRM);
        request.setArchiveOpinion(StrUtil.trim(reqVO.getOpinion()));
        request.setArchiveTime(LocalDateTime.now());
        approveBpmTask(request, BPM_NODE_ARCHIVE_CONFIRM, request.getApplicantId(), "发起人确认归档", null);
        updateStatus(request, STATUS_ARCHIVED, "发起人确认归档完成", null);
    }

    private void copyEditableFields(SrmSampleRequestSaveReqVO reqVO, SrmSampleRequestDO request) {
        boolean directArchive = Boolean.TRUE.equals(reqVO.getDirectArchive());
        request.setMaterialName(StrUtil.trim(reqVO.getMaterialName()));
        request.setMaterialModel(StrUtil.trim(reqVO.getMaterialModel()));
        request.setApplyType(StrUtil.blankToDefault(StrUtil.trim(reqVO.getApplyType()), "NORMAL"));
        request.setApplyDept(StrUtil.trim(reqVO.getApplyDept()));
        request.setUsedProduct(StrUtil.trim(reqVO.getUsedProduct()));
        request.setRequireQty(reqVO.getRequireQty());
        request.setApplyDate(reqVO.getApplyDate());
        request.setRequireDate(reqVO.getRequireDate());
        request.setSpecifiedSupplierType(StrUtil.blankToDefault(StrUtil.trim(reqVO.getSpecifiedSupplierType()), "NONE"));
        request.setSupplierId(reqVO.getSupplierId());
        request.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        request.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        request.setTechnicalRequirement(StrUtil.trim(reqVO.getTechnicalRequirement()));
        request.setRdSampleNecessity(StrUtil.trim(reqVO.getRdSampleNecessity()));
        request.setOaApprovalUrl(StrUtil.trim(reqVO.getOaApprovalUrl()));
        if (directArchive) {
            request.setProjectLeaderUserId(null);
            request.setProjectLeaderUserName(StrUtil.trim(reqVO.getProjectLeaderUserName()));
            request.setPurchaseOwnerUserId(null);
            request.setPurchaseOwnerUserName(StrUtil.trim(reqVO.getPurchaseOwnerUserName()));
            request.setFinalApproverUserId(null);
            request.setFinalApproverUserName(StrUtil.trim(reqVO.getFinalApproverUserName()));
        } else {
            request.setProjectLeaderUserId(reqVO.getProjectLeaderUserId());
            request.setProjectLeaderUserName(resolveUserName(reqVO.getProjectLeaderUserId(), reqVO.getProjectLeaderUserName()));
            request.setPurchaseOwnerUserId(reqVO.getPurchaseOwnerUserId());
            request.setPurchaseOwnerUserName(resolveUserName(reqVO.getPurchaseOwnerUserId(), reqVO.getPurchaseOwnerUserName()));
            request.setFinalApproverUserId(reqVO.getFinalApproverUserId());
            request.setFinalApproverUserName(resolveUserName(reqVO.getFinalApproverUserId(), reqVO.getFinalApproverUserName()));
        }
        request.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private SrmSampleRequestRespVO buildResp(SrmSampleRequestDO request, boolean includeLogs) {
        SrmSampleRequestRespVO respVO = BeanUtils.toBean(request, SrmSampleRequestRespVO.class);
        Long userId = currentUser().id();
        boolean maintainer = isMaintainer(request, userId);
        respVO.setCanEdit(maintainer && STATUS_DRAFT.equals(request.getStatus()));
        respVO.setCanSubmit(maintainer && STATUS_DRAFT.equals(request.getStatus()));
        respVO.setCanProjectReview(Objects.equals(request.getProjectLeaderUserId(), userId)
                && STATUS_PROJECT_REVIEW.equals(request.getStatus()));
        respVO.setCanPurchaseReview(Objects.equals(request.getPurchaseOwnerUserId(), userId)
                && STATUS_PURCHASE_REVIEW.equals(request.getStatus()));
        respVO.setCanInitiatorDecision(maintainer && STATUS_INITIATOR_CONFIRM.equals(request.getStatus()));
        respVO.setCanFinalApprove(Objects.equals(request.getFinalApproverUserId(), userId)
                && STATUS_FINAL_APPROVAL.equals(request.getStatus()));
        respVO.setCanArchiveConfirm(maintainer && STATUS_ARCHIVE_CONFIRM.equals(request.getStatus()));
        if (includeLogs) {
            respVO.setLogs(logMapper.selectListByRequestId(request.getId()).stream().map(this::buildLogResp).toList());
            respVO.setTrialValidations(trialValidationMapper.selectListBySampleRequestId(request.getId()).stream()
                    .map(this::buildTrialValidationResp).toList());
        }
        return respVO;
    }

    private SrmSampleRequestRespVO.TrialValidation buildTrialValidationResp(SrmTrialValidationDO trialValidation) {
        return BeanUtils.toBean(trialValidation, SrmSampleRequestRespVO.TrialValidation.class);
    }

    private SrmSampleRequestRespVO.Log buildLogResp(SrmSampleRequestLogDO log) {
        SrmSampleRequestRespVO.Log respVO = BeanUtils.toBean(log, SrmSampleRequestRespVO.Log.class);
        respVO.setActionName(actionText(log.getAction()));
        return respVO;
    }

    private String startBpmProcess(SrmSampleRequestDO request) {
        Map<String, Object> variables = buildBpmVariables(request);
        variables.put("needFinalApproval", false);
        Map<String, List<Long>> startAssignees = new HashMap<>();
        startAssignees.put(BPM_NODE_PROJECT_REVIEW, List.of(request.getProjectLeaderUserId()));
        variables.put("PROCESS_APPROVE_USER_SELECT_ASSIGNEES", new HashMap<String, List<Long>>());
        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY)
                .setBusinessKey(String.valueOf(request.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(startAssignees);
        try {
            return bpmProcessInstanceApi.createProcessInstance(request.getApplicantId(), createReq);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(request.getApplicantId(), createReq);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(SRM_SAMPLE_REQUEST_BPM_NOT_PUBLISHED);
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
            throw exception(SRM_SAMPLE_REQUEST_BPM_NOT_PUBLISHED);
        }
    }

    private void approveBpmTask(SrmSampleRequestDO request, String taskKey, Long assigneeUserId,
                                String reason, Map<String, List<Long>> nextAssignees) {
        approveBpmTask(request, taskKey, assigneeUserId, reason, nextAssignees, Map.of());
    }

    private void approveBpmTask(SrmSampleRequestDO request, String taskKey, Long assigneeUserId,
                                String reason, Map<String, List<Long>> nextAssignees,
                                Map<String, Object> extraVariables) {
        if (StrUtil.isBlank(request.getProcessInstanceId())) {
            throw exception(SRM_SAMPLE_REQUEST_BPM_TASK_MISSING);
        }
        Task task = findRunningTask(request.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_SAMPLE_REQUEST_BPM_TASK_MISSING);
        }
        Map<String, Object> variables = buildBpmVariables(request);
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

    private Map<String, Object> buildBpmVariables(SrmSampleRequestDO request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("sampleRequestId", request.getId());
        variables.put("businessKey", request.getId());
        variables.put("requestNo", request.getRequestNo());
        variables.put("materialName", request.getMaterialName());
        variables.put("materialModel", request.getMaterialModel());
        variables.put("supplierName", request.getSupplierName());
        variables.put("applyDept", request.getApplyDept());
        variables.put("usedProduct", request.getUsedProduct());
        variables.put("projectLeaderUserId", request.getProjectLeaderUserId());
        variables.put("purchaseOwnerUserId", request.getPurchaseOwnerUserId());
        variables.put("finalApproverUserId", request.getFinalApproverUserId());
        variables.put("initiatorId", request.getApplicantId());
        return variables;
    }

    private void updateStatus(SrmSampleRequestDO request, String toStatus, String description, Object detail) {
        String fromStatus = request.getStatus();
        request.setStatus(toStatus);
        request.setCurrentNodeName(statusText(toStatus));
        updateByIdChecked(request);
        writeLog(request.getId(), resolveActionByStatus(toStatus), fromStatus, toStatus, description, detail);
    }

    private void updateByIdChecked(SrmSampleRequestDO request) {
        if (sampleRequestMapper.updateById(request) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    private void validateRequiredReviewers(SrmSampleRequestDO request) {
        if (request.getProjectLeaderUserId() == null || StrUtil.isBlank(request.getPurchaseOwnerUserName())) {
            throw exception(SRM_SAMPLE_REQUEST_REVIEWER_REQUIRED);
        }
        List<Long> reviewerUserIds = new ArrayList<>();
        reviewerUserIds.add(request.getProjectLeaderUserId());
        if (request.getPurchaseOwnerUserId() != null) {
            reviewerUserIds.add(request.getPurchaseOwnerUserId());
        }
        adminUserApi.validateUserList(reviewerUserIds);
    }

    private String generateRequestNo() {
        String base = REQUEST_NO_PREFIX + LocalDate.now().format(REQUEST_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= REQUEST_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String requestNo = base + String.format("%03d", sequence);
            if (sampleRequestMapper.selectByRequestNo(requestNo) == null) {
                return requestNo;
            }
        }
        throw exception(SRM_SAMPLE_REQUEST_NO_EXISTS);
    }

    private void validateRequestNoUnique(Long id, String requestNo) {
        if (StrUtil.isBlank(requestNo)) {
            return;
        }
        SrmSampleRequestDO sameNo = sampleRequestMapper.selectByRequestNo(StrUtil.trim(requestNo));
        if (sameNo != null && !Objects.equals(sameNo.getId(), id)) {
            throw exception(SRM_SAMPLE_REQUEST_NO_EXISTS);
        }
    }

    private SrmSampleRequestDO validateSampleRequest(Long id) {
        SrmSampleRequestDO request = id == null ? null : sampleRequestMapper.selectById(id);
        if (request == null) {
            throw exception(SRM_SAMPLE_REQUEST_NOT_EXISTS);
        }
        return request;
    }

    private void assertStatus(SrmSampleRequestDO request, String expectedStatus) {
        if (!expectedStatus.equals(request.getStatus())) {
            throw exception(SRM_SAMPLE_REQUEST_STATUS_INVALID);
        }
    }

    private void assertMaintainer(SrmSampleRequestDO request) {
        Long userId = currentUser().id();
        if (!isMaintainer(request, userId)) {
            throw exception(SRM_SAMPLE_REQUEST_OPERATOR_ONLY);
        }
    }

    private boolean isMaintainer(SrmSampleRequestDO request, Long userId) {
        return Objects.equals(request.getApplicantId(), userId) || isSampleRequestAdmin(userId);
    }

    private void assertCurrentOperator(Long expectedUserId) {
        Long userId = currentUser().id();
        if (!Objects.equals(expectedUserId, userId)) {
            throw exception(SRM_SAMPLE_REQUEST_OPERATOR_ONLY);
        }
    }

    private boolean isSampleRequestAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, SRM_SUPER_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private String resolveUserName(Long userId, String fallbackName) {
        if (userId == null) {
            return StrUtil.trim(fallbackName);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return userName(user, fallbackName);
    }

    private UserSnapshot resolveUser(Long userId, String fallbackName) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(SRM_SAMPLE_REQUEST_REVIEWER_REQUIRED);
        }
        return new UserSnapshot(userId, userName(user, fallbackName));
    }

    private String userName(AdminUserRespDTO user, String fallbackName) {
        if (user == null) {
            return StrUtil.trim(fallbackName);
        }
        return StrUtil.blankToDefault(user.getNickname(), StrUtil.blankToDefault(user.getUsername(), fallbackName));
    }

    private void writeLog(Long requestId, String action, String fromStatus, String toStatus,
                          String description, Object detail) {
        UserSnapshot user = currentUser();
        SrmSampleRequestLogDO log = new SrmSampleRequestLogDO();
        log.setRequestId(requestId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setActionDescription(description);
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

    private String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_DRAFT)) {
            case STATUS_PROJECT_REVIEW -> "项目负责人审核";
            case STATUS_PURCHASE_REVIEW -> "采购负责人审核";
            case STATUS_INITIATOR_CONFIRM -> "发起人确认";
            case STATUS_FINAL_APPROVAL -> "最终批准人审核";
            case STATUS_ARCHIVE_CONFIRM -> "发起人归档确认";
            case STATUS_ARCHIVED -> "已归档";
            default -> "草稿";
        };
    }

    private String resolveActionByStatus(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_DRAFT)) {
            case STATUS_PROJECT_REVIEW -> "SUBMIT";
            case STATUS_PURCHASE_REVIEW -> "PROJECT_REVIEW";
            case STATUS_INITIATOR_CONFIRM -> "PURCHASE_REVIEW";
            case STATUS_FINAL_APPROVAL -> "INITIATOR_SEND_APPROVAL";
            case STATUS_ARCHIVE_CONFIRM -> "FINAL_APPROVE";
            case STATUS_ARCHIVED -> "ARCHIVE";
            default -> "UPDATE";
        };
    }

    private String actionText(String action) {
        return switch (StrUtil.blankToDefault(action, "")) {
            case "CREATE" -> "创建";
            case "DIRECT_ARCHIVE_FORM" -> "直接归档表单";
            case "UPDATE" -> "更新";
            case "SUBMIT" -> "提交";
            case "PROJECT_REVIEW" -> "项目负责人审核";
            case "PURCHASE_REVIEW" -> "采购负责人审核";
            case "INITIATOR_SEND_APPROVAL" -> "发起人送审";
            case "FINAL_APPROVE" -> "最终批准人审核";
            case "ARCHIVE" -> "归档";
            default -> action;
        };
    }

    private record UserSnapshot(Long id, String name) {
    }

}
