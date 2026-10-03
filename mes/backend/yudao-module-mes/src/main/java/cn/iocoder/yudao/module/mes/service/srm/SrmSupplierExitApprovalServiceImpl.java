package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierExitApprovalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierExitApprovalLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierExitApprovalSignDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierExitApprovalLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierExitApprovalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierExitApprovalSignMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Model;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_BPM_TASK_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_ENTRY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_MATERIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_QUALIFIED_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_REVIEW_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_SIGN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_EXIT_APPROVAL_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmSupplierExitApprovalServiceImpl implements SrmSupplierExitApprovalService {

    private static final String BIZ_SUPPLIER_EXIT_APPROVAL = "SUPPLIER_EXIT_APPROVAL";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PURCHASE_INTAKE = "PURCHASE_INTAKE";
    private static final String STATUS_DEPT_SIGN = "DEPT_SIGN";
    private static final String STATUS_PURCHASE_TRANSFER = "PURCHASE_TRANSFER";
    private static final String STATUS_ENTRY_PROCESSING = "ENTRY_PROCESSING";
    private static final String STATUS_USE_DEPT_REVIEW = "USE_DEPT_REVIEW";
    private static final String STATUS_QUALITY_REVIEW = "QUALITY_REVIEW";
    private static final String STATUS_TECH_REVIEW = "TECH_REVIEW";
    private static final String STATUS_PURCHASE_REVIEW = "PURCHASE_REVIEW";
    private static final String STATUS_GENERAL_MANAGER_REVIEW = "GENERAL_MANAGER_REVIEW";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String REVIEW_PASS = "PASS";
    private static final String REVIEW_REJECT = "REJECT";
    private static final String QUALIFIED_STATUS = "QUALIFIED";
    private static final String SIGN_STATUS_PENDING = "PENDING";
    private static final String SIGN_STATUS_COMPLETED = "COMPLETED";
    private static final String TRANSFER_ACTION_GENERAL_MANAGER = "GENERAL_MANAGER";
    private static final String TRANSFER_ACTION_ASSIGN_ENTRY = "ASSIGN_ENTRY";
    private static final String TRANSFER_ACTION_ARCHIVE = "ARCHIVE";
    private static final String BPM_PROCESS_KEY_SUPPLIER_EXIT_APPROVAL = "srm_supplier_exit_approval";
    private static final String BPM_MODEL_ID_SUPPLIER_EXIT_APPROVAL = "srm-supplier-exit-approval-model";
    private static final String BPM_NODE_START = "StartUserNode";
    private static final String BPM_NODE_PURCHASE_INTAKE = "purchase_intake";
    private static final String BPM_NODE_DEPT_SIGN = "dept_sign";
    private static final String BPM_NODE_PURCHASE_TRANSFER = "purchase_transfer";
    private static final String BPM_NODE_ENTRY_PROCESS = "entry_process";
    private static final String BPM_NODE_USE_DEPT_REVIEW = "use_dept_review";
    private static final String BPM_NODE_QUALITY_REVIEW = "quality_review";
    private static final String BPM_NODE_TECH_REVIEW = "tech_review";
    private static final String BPM_NODE_PURCHASE_REVIEW = "purchase_review";
    private static final String BPM_NODE_GENERAL_MANAGER_REVIEW = "general_manager_review";
    private static final String BPM_VARIABLE_COLL_USER_LIST = "coll_userList";
    private static final String BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES = "PROCESS_APPROVE_USER_SELECT_ASSIGNEES";
    private static final String PURCHASE_HANDLER_PERMISSION = "mes:srm-supplier-exit-approval:purchase-handler";
    private static final String GENERAL_MANAGER_PERMISSION = "mes:srm-supplier-exit-approval:general-manager";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1_009_003_002;
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String SRM_SUPER_ADMIN_ROLE = "srm_supplier_super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String SUPPLIER_EXIT_APPROVAL_NO_PREFIX = "WLTC-";
    private static final DateTimeFormatter SUPPLIER_EXIT_APPROVAL_NO_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SUPPLIER_EXIT_APPROVAL_NO_DAILY_SEQUENCE_LIMIT = 999;
    private static final int INITIAL_VERSION = 0;
    private static final String ENTRY_COMPLETED_MARK = "已完成";

    @Resource
    private SrmSupplierExitApprovalMapper supplierExitApprovalMapper;
    @Resource
    private SrmSupplierExitApprovalLogMapper supplierExitApprovalLogMapper;
    @Resource
    private SrmSupplierExitApprovalSignMapper supplierExitApprovalSignMapper;
    @Resource
    private SrmAttachmentMapper attachmentMapper;
    @Resource
    private MesSupplierMapper supplierMapper;
    @Resource
    private HcMaterialMapper materialMapper;
    @Resource
    private SrmSupplierScopeService supplierScopeService;
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
    @Resource
    private RuntimeService runtimeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplierExitApproval(SrmSupplierExitApprovalSaveReqVO reqVO) {
        String exitNo = generateSupplierExitApprovalNo();
        validateExitNoUnique(null, exitNo);
        SrmSupplierExitApprovalDO apply = new SrmSupplierExitApprovalDO();
        copyEditableFields(reqVO, apply);
        apply.setExitNo(exitNo);
        apply.setStatus(STATUS_DRAFT);
        apply.setCurrentNodeName(statusText(STATUS_DRAFT));
        apply.setApplyTime(defaultLocalDateTime(reqVO.getApplyTime()));
        fillInitiatorDefaults(apply, null);
        apply.setVersion(INITIAL_VERSION);
        supplierExitApprovalMapper.insert(apply);
        writeLog(apply.getId(), "CREATE", null, STATUS_DRAFT, "创建供方退出审批", null);
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierExitApproval(SrmSupplierExitApprovalSaveReqVO reqVO) {
        SrmSupplierExitApprovalDO old = validateExists(reqVO.getId());
        assertMaintainer(old);
        assertStatus(old, STATUS_DRAFT);
        copyEditableFields(reqVO, old);
        old.setVersion(reqVO.getVersion());
        updateByIdChecked(old);
        writeLog(old.getId(), "UPDATE", STATUS_DRAFT, STATUS_DRAFT, "更新供方退出审批", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierExitApproval(Long id) {
        SrmSupplierExitApprovalDO apply = validateExists(id);
        assertMaintainer(apply);
        assertStatus(apply, STATUS_DRAFT);
        attachmentMapper.deleteByBiz(BIZ_SUPPLIER_EXIT_APPROVAL, id);
        supplierExitApprovalSignMapper.deleteByApplyId(id);
        supplierExitApprovalMapper.deleteById(id);
    }

    @Override
    public SrmSupplierExitApprovalRespVO getSupplierExitApproval(Long id) {
        return buildResp(validateExists(id), true);
    }

    @Override
    public PageResult<SrmSupplierExitApprovalRespVO> getSupplierExitApprovalPage(
            SrmSupplierExitApprovalPageReqVO reqVO) {
        PageResult<SrmSupplierExitApprovalDO> page = supplierExitApprovalMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(),
                page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Submit reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertMaintainer(apply);
        assertStatus(apply, STATUS_DRAFT);
        List<Long> purchaseHandlerUserIds = resolvePermissionUserIds(PURCHASE_HANDLER_PERMISSION);
        String processInstanceId = startBpmProcess(apply);
        apply.setProcessInstanceId(processInstanceId);
        approveBpmTask(apply, BPM_NODE_START, apply.getApplicantId(), "提交供方退出审批",
                Map.of(), Map.of("purchaseHandlerUserIds", purchaseHandlerUserIds,
                        BPM_VARIABLE_COLL_USER_LIST, purchaseHandlerUserIds));
        updateStatus(apply, STATUS_PURCHASE_INTAKE, "提交供方退出审批，进入采购部门办理",
                Map.of("purchaseHandlerUserIds", purchaseHandlerUserIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseIntakeSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.PurchaseIntake reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_PURCHASE_INTAKE);
        UserSnapshot operator = currentUser();
        if (findRunningTask(apply.getProcessInstanceId(), BPM_NODE_PURCHASE_INTAKE, operator.id()) == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_BPM_TASK_MISSING);
        }
        UserSnapshot useDeptReviewer = resolveUser(reqVO.getUseDeptReviewerUserId(),
                reqVO.getUseDeptReviewerUserName());
        apply.setPurchaseHandlerUserId(operator.id());
        apply.setPurchaseHandlerUserName(operator.name());
        apply.setPurchaseReviewerUserId(operator.id());
        apply.setPurchaseReviewerUserName(operator.name());
        apply.setPurchaseIntakeOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setPurchaseIntakeTime(LocalDateTime.now());
        apply.setUseDeptReviewerUserId(useDeptReviewer.id());
        apply.setUseDeptReviewerUserName(useDeptReviewer.name());

        List<SrmSupplierExitApprovalActionReqVO.SignUser> signUsers = new ArrayList<>();
        SrmSupplierExitApprovalActionReqVO.SignUser useDeptSignUser =
                new SrmSupplierExitApprovalActionReqVO.SignUser();
        useDeptSignUser.setDeptCode("USE_DEPT");
        useDeptSignUser.setDeptName("使用部门负责人");
        useDeptSignUser.setUserId(useDeptReviewer.id());
        useDeptSignUser.setUserName(useDeptReviewer.name());
        signUsers.add(useDeptSignUser);
        if (CollUtil.isNotEmpty(reqVO.getSignUsers())) {
            signUsers.addAll(reqVO.getSignUsers());
        }
        List<SrmSupplierExitApprovalSignDO> signs = buildSignRows(apply.getId(), signUsers);
        if (CollUtil.isEmpty(signs)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_SIGN_REQUIRED);
        }
        supplierExitApprovalSignMapper.deleteByApplyId(apply.getId());
        supplierExitApprovalSignMapper.insertBatch(signs);
        List<Long> signUserIds = signs.stream().map(SrmSupplierExitApprovalSignDO::getUserId).distinct().toList();
        Map<String, List<Long>> deptSignAssignees = nextAssignees(BPM_NODE_DEPT_SIGN, signUserIds);
        Map<String, Object> bpmVariables = new HashMap<>();
        bpmVariables.put("signUserIds", signUserIds);
        bpmVariables.put(BPM_VARIABLE_COLL_USER_LIST, signUserIds);
        bpmVariables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, deptSignAssignees);
        approveBpmTask(apply, BPM_NODE_PURCHASE_INTAKE, operator.id(), "采购部门办理",
                deptSignAssignees, bpmVariables);
        updateStatus(apply, STATUS_DEPT_SIGN, "采购部门办理完成，进入部门会签",
                Map.of("signUserIds", signUserIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Sign reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_DEPT_SIGN);
        Long userId = currentUser().id();
        List<SrmSupplierExitApprovalSignDO> pendingSigns =
                supplierExitApprovalSignMapper.selectPendingListByApplyIdAndUserId(apply.getId(), userId);
        if (CollUtil.isEmpty(pendingSigns)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY);
        }
        if (reqVO.getSignId() != null
                && pendingSigns.stream().noneMatch(sign -> Objects.equals(sign.getId(), reqVO.getSignId()))) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY);
        }
        String result = normalizeReviewResult(reqVO.getResult());
        LocalDateTime now = LocalDateTime.now();
        for (SrmSupplierExitApprovalSignDO sign : pendingSigns) {
            sign.setSignStatus(SIGN_STATUS_COMPLETED);
            sign.setSignResult(result);
            sign.setSignOpinion(StrUtil.trim(reqVO.getOpinion()));
            sign.setSignTime(now);
            supplierExitApprovalSignMapper.updateById(sign);
        }
        approveBpmTask(apply, BPM_NODE_DEPT_SIGN, userId, "部门会签",
                nextAssignees(BPM_NODE_PURCHASE_TRANSFER, List.of(apply.getPurchaseHandlerUserId())));
        boolean allCompleted = supplierExitApprovalSignMapper.selectListByApplyId(apply.getId()).stream()
                .allMatch(sign -> SIGN_STATUS_COMPLETED.equals(sign.getSignStatus()));
        if (allCompleted) {
            updateStatus(apply, STATUS_PURCHASE_TRANSFER, "部门会签完成，返回采购部转办", null);
        } else {
            writeLog(apply.getId(), "DEPT_SIGN", STATUS_DEPT_SIGN, STATUS_DEPT_SIGN,
                    "部门会签", Map.of("result", result));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseTransferSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.PurchaseTransfer reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_PURCHASE_TRANSFER);
        assertCurrentOperator(apply.getPurchaseHandlerUserId());
        String transferAction = StrUtil.trim(reqVO.getTransferAction());
        apply.setPurchaseResult(REVIEW_PASS);
        apply.setPurchaseOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setPurchaseHandleTime(LocalDateTime.now());
        if (TRANSFER_ACTION_GENERAL_MANAGER.equals(transferAction)) {
            UserSnapshot generalManager = reqVO.getGeneralManagerUserId() == null
                    ? resolveFirstPermissionUser(GENERAL_MANAGER_PERMISSION)
                    : resolveUser(reqVO.getGeneralManagerUserId(), reqVO.getGeneralManagerUserName());
            apply.setGeneralManagerUserId(generalManager.id());
            apply.setGeneralManagerUserName(generalManager.name());
            approveBpmTask(apply, BPM_NODE_PURCHASE_TRANSFER, apply.getPurchaseHandlerUserId(),
                    "采购部转办总经理审核",
                    nextAssignees(BPM_NODE_GENERAL_MANAGER_REVIEW, List.of(generalManager.id())),
                    Map.of("transferAction", transferAction, "generalManagerUserId", generalManager.id()));
            updateStatus(apply, STATUS_GENERAL_MANAGER_REVIEW, "采购部转办，进入总经理审核",
                    Map.of("generalManagerUserId", generalManager.id()));
            return;
        }
        if (TRANSFER_ACTION_ASSIGN_ENTRY.equals(transferAction)) {
            UserSnapshot rosterRemoveUser = resolveUser(reqVO.getMaterialEntryUserId(),
                    reqVO.getMaterialEntryUserName());
            UserSnapshot inventoryAccountUser = resolveUser(reqVO.getSupplierRosterEntryUserId(),
                    reqVO.getSupplierRosterEntryUserName());
            apply.setMaterialEntryUserId(rosterRemoveUser.id());
            apply.setMaterialEntryUserName(rosterRemoveUser.name());
            apply.setMaterialEntryCode(null);
            apply.setMaterialEntryOpinion(null);
            apply.setMaterialEntryHandleTime(null);
            apply.setMaterialCodeCreated(false);
            apply.setMaterialCodeCompleteDate(null);
            apply.setSupplierRosterEntryUserId(inventoryAccountUser.id());
            apply.setSupplierRosterEntryUserName(inventoryAccountUser.name());
            apply.setSupplierRosterEntryCode(null);
            apply.setSupplierRosterEntryOpinion(null);
            apply.setSupplierRosterEntryHandleTime(null);
            apply.setSupplierRosterCreated(false);
            apply.setSupplierRosterCompleteDate(null);
            List<Long> entryUserIds = List.of(rosterRemoveUser.id(), inventoryAccountUser.id());
            Map<String, List<Long>> entryAssignees = nextAssignees(BPM_NODE_ENTRY_PROCESS, entryUserIds);
            Map<String, Object> bpmVariables = new HashMap<>();
            bpmVariables.put("transferAction", transferAction);
            bpmVariables.put("entryUserIds", entryUserIds.stream().distinct().toList());
            bpmVariables.put(BPM_VARIABLE_COLL_USER_LIST, entryUserIds.stream().distinct().toList());
            bpmVariables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, entryAssignees);
            approveBpmTask(apply, BPM_NODE_PURCHASE_TRANSFER, apply.getPurchaseHandlerUserId(),
                    "采购部交办退出闭环", entryAssignees, bpmVariables);
            updateStatus(apply, STATUS_ENTRY_PROCESSING, "采购部交办清单移除和库存/账务处理",
                    Map.of("entryUserIds", entryUserIds.stream().distinct().toList()));
            return;
        }
        if (TRANSFER_ACTION_ARCHIVE.equals(transferAction)) {
            if (!isEntryCompleted(apply)) {
                throw exception(SRM_SUPPLIER_EXIT_APPROVAL_ENTRY_REQUIRED);
            }
            approveBpmTask(apply, BPM_NODE_PURCHASE_TRANSFER, apply.getPurchaseHandlerUserId(),
                    "采购部确认归档", null, Map.of("transferAction", transferAction));
            updateStatus(apply, STATUS_ARCHIVED, "采购部确认完成归档", null);
            return;
        }
        throw exception(SRM_SUPPLIER_EXIT_APPROVAL_STATUS_INVALID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void entrySupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Entry reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_ENTRY_PROCESSING);
        Long userId = currentUser().id();
        Task task = findRunningTask(apply.getProcessInstanceId(), BPM_NODE_ENTRY_PROCESS, userId);
        if (task == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_BPM_TASK_MISSING);
        }
        boolean handled = false;
        LocalDateTime now = LocalDateTime.now();
        if (Objects.equals(apply.getMaterialEntryUserId(), userId)) {
            if (reqVO.getMaterialCodeCompleteDate() == null) {
                throw exception(SRM_SUPPLIER_EXIT_APPROVAL_ENTRY_REQUIRED);
            }
            apply.setMaterialCodeCompleteDate(reqVO.getMaterialCodeCompleteDate());
            apply.setMaterialEntryCode(ENTRY_COMPLETED_MARK);
            apply.setMaterialEntryOpinion(StrUtil.trim(reqVO.getMaterialEntryOpinion()));
            apply.setMaterialEntryHandleTime(now);
            apply.setMaterialCodeCreated(true);
            handled = true;
        }
        if (Objects.equals(apply.getSupplierRosterEntryUserId(), userId)) {
            if (reqVO.getSupplierRosterCompleteDate() == null) {
                throw exception(SRM_SUPPLIER_EXIT_APPROVAL_ENTRY_REQUIRED);
            }
            apply.setSupplierRosterCompleteDate(reqVO.getSupplierRosterCompleteDate());
            apply.setSupplierRosterEntryCode(ENTRY_COMPLETED_MARK);
            apply.setSupplierRosterEntryOpinion(StrUtil.trim(reqVO.getSupplierRosterEntryOpinion()));
            apply.setSupplierRosterEntryHandleTime(now);
            apply.setSupplierRosterCreated(true);
            handled = true;
        }
        if (!handled) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY);
        }
        approveBpmTask(apply, BPM_NODE_ENTRY_PROCESS, userId, "交办办理",
                nextAssignees(BPM_NODE_PURCHASE_TRANSFER, List.of(apply.getPurchaseHandlerUserId())));
        if (isEntryCompleted(apply) && !hasRunningTask(apply.getProcessInstanceId(), BPM_NODE_ENTRY_PROCESS)) {
            updateStatus(apply, STATUS_PURCHASE_TRANSFER, "交办办理完成，返回采购部转办", null);
        } else {
            updateByIdChecked(apply);
            writeLog(apply.getId(), "ENTRY_PROCESS", STATUS_ENTRY_PROCESSING, STATUS_ENTRY_PROCESSING,
                    "交办办理", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useDeptReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_USE_DEPT_REVIEW);
        assertCurrentOperator(apply.getUseDeptReviewerUserId());
        apply.setUseDeptResult(normalizeReviewResult(reqVO.getResult()));
        apply.setUseDeptOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setUseDeptHandleTime(LocalDateTime.now());
        finishReview(apply, BPM_NODE_USE_DEPT_REVIEW, apply.getUseDeptReviewerUserId(),
                "使用部门负责人审核", STATUS_QUALITY_REVIEW, BPM_NODE_QUALITY_REVIEW,
                apply.getQualityReviewerUserId(), "使用部门负责人审核完成，进入品质部审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void qualityReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_QUALITY_REVIEW);
        assertCurrentOperator(apply.getQualityReviewerUserId());
        apply.setQualityResult(normalizeReviewResult(reqVO.getResult()));
        apply.setQualityOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setQualityHandleTime(LocalDateTime.now());
        finishReview(apply, BPM_NODE_QUALITY_REVIEW, apply.getQualityReviewerUserId(),
                "品质部审批", STATUS_TECH_REVIEW, BPM_NODE_TECH_REVIEW,
                apply.getTechReviewerUserId(), "品质部审批完成，进入技术研发部审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void techReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_TECH_REVIEW);
        assertCurrentOperator(apply.getTechReviewerUserId());
        apply.setTechResult(normalizeReviewResult(reqVO.getResult()));
        apply.setTechOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setTechHandleTime(LocalDateTime.now());
        finishReview(apply, BPM_NODE_TECH_REVIEW, apply.getTechReviewerUserId(),
                "技术研发部审批", STATUS_PURCHASE_REVIEW, BPM_NODE_PURCHASE_REVIEW,
                apply.getPurchaseReviewerUserId(), "技术研发部审批完成，进入采购部审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_PURCHASE_REVIEW);
        assertCurrentOperator(apply.getPurchaseReviewerUserId());
        apply.setPurchaseResult(normalizeReviewResult(reqVO.getResult()));
        apply.setPurchaseOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setPurchaseHandleTime(LocalDateTime.now());
        finishReview(apply, BPM_NODE_PURCHASE_REVIEW, apply.getPurchaseReviewerUserId(),
                "采购部审批", STATUS_GENERAL_MANAGER_REVIEW, BPM_NODE_GENERAL_MANAGER_REVIEW,
                apply.getGeneralManagerUserId(), "采购部审批完成，进入总经理审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generalManagerReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        SrmSupplierExitApprovalDO apply = validateExists(reqVO.getId());
        assertStatus(apply, STATUS_GENERAL_MANAGER_REVIEW);
        assertCurrentOperator(apply.getGeneralManagerUserId());
        apply.setGeneralManagerResult(normalizeReviewResult(reqVO.getResult()));
        apply.setGeneralManagerOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setGeneralManagerHandleTime(LocalDateTime.now());
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        nextAssignees.putAll(nextAssignees(BPM_NODE_PURCHASE_TRANSFER, List.of(apply.getPurchaseHandlerUserId())));
        nextAssignees.putAll(nextAssignees(BPM_NODE_ENTRY_PROCESS, List.of(apply.getPurchaseHandlerUserId())));
        approveBpmTask(apply, BPM_NODE_GENERAL_MANAGER_REVIEW, apply.getGeneralManagerUserId(),
                "总经理审核", nextAssignees, buildGeneralManagerReturnVariables(apply, nextAssignees));
        moveEntryTaskBackToPurchaseTransferIfNeeded(apply);
        updateStatus(apply, STATUS_PURCHASE_TRANSFER, "总经理审核完成，返回采购部转办",
                Map.of("result", apply.getGeneralManagerResult()));
    }

    private SrmSupplierExitApprovalDO validateExists(Long id) {
        SrmSupplierExitApprovalDO apply = supplierExitApprovalMapper.selectById(id);
        if (apply == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_NOT_EXISTS);
        }
        return apply;
    }

    private void validateExitNoUnique(Long id, String exitNo) {
        if (StrUtil.isBlank(exitNo)) {
            return;
        }
        SrmSupplierExitApprovalDO apply = supplierExitApprovalMapper.selectByExitNo(exitNo);
        if (apply != null && !apply.getId().equals(id)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_NO_EXISTS);
        }
    }

    private String generateSupplierExitApprovalNo() {
        String dailyPrefix = SUPPLIER_EXIT_APPROVAL_NO_PREFIX + LocalDate.now()
                .format(SUPPLIER_EXIT_APPROVAL_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= SUPPLIER_EXIT_APPROVAL_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String exitNo = dailyPrefix + String.format(Locale.ROOT, "%03d", sequence);
            if (supplierExitApprovalMapper.selectByExitNo(exitNo) == null) {
                return exitNo;
            }
        }
        throw exception(SRM_SUPPLIER_EXIT_APPROVAL_NO_EXISTS);
    }

    private void copyEditableFields(SrmSupplierExitApprovalSaveReqVO reqVO, SrmSupplierExitApprovalDO apply) {
        copyQualifiedSupplierSnapshot(reqVO, apply);
        apply.setReasonQualityDeliveryService(Boolean.TRUE.equals(reqVO.getReasonQualityDeliveryService()));
        apply.setReasonSupplierInitiated(Boolean.TRUE.equals(reqVO.getReasonSupplierInitiated()));
        apply.setReasonBusinessAdjustment(Boolean.TRUE.equals(reqVO.getReasonBusinessAdjustment()));
        apply.setReasonOther(Boolean.TRUE.equals(reqVO.getReasonOther()));
        apply.setReasonOtherText(StrUtil.trim(reqVO.getReasonOtherText()));
        apply.setExitReasonDesc(StrUtil.trim(reqVO.getExitReasonDesc()));
        apply.setReplacementSupplierStatus(StrUtil.trim(reqVO.getReplacementSupplierStatus()));
        apply.setReplacementSupplierName(StrUtil.trim(reqVO.getReplacementSupplierName()));
        apply.setStockStatus(StrUtil.trim(reqVO.getStockStatus()));
        apply.setRemainingStockDesc(StrUtil.trim(reqVO.getRemainingStockDesc()));
        apply.setStockDisposalReturn(Boolean.TRUE.equals(reqVO.getStockDisposalReturn()));
        apply.setStockDisposalScrap(Boolean.TRUE.equals(reqVO.getStockDisposalScrap()));
        apply.setStockDisposalConsume(Boolean.TRUE.equals(reqVO.getStockDisposalConsume()));
        apply.setStockDisposalOther(Boolean.TRUE.equals(reqVO.getStockDisposalOther()));
        apply.setStockDisposalOtherText(StrUtil.trim(reqVO.getStockDisposalOtherText()));
        apply.setContractPaymentCleared(Boolean.TRUE.equals(reqVO.getContractPaymentCleared()));
        apply.setUnpaidAmount(reqVO.getUnpaidAmount());
        apply.setUninvoicedAmount(reqVO.getUninvoicedAmount());
        apply.setBusinessRiskImpact(StrUtil.trim(reqVO.getBusinessRiskImpact()));
        apply.setImpactDesc(StrUtil.trim(reqVO.getImpactDesc()));
        apply.setMaterialCodeCreated(Boolean.TRUE.equals(reqVO.getMaterialCodeCreated()));
        apply.setMaterialCodeCompleteDate(reqVO.getMaterialCodeCompleteDate());
        apply.setSupplierRosterCreated(Boolean.TRUE.equals(reqVO.getSupplierRosterCreated()));
        apply.setSupplierRosterCompleteDate(reqVO.getSupplierRosterCompleteDate());
        apply.setSpecReq(StrUtil.trim(reqVO.getSpecReq()));
        apply.setNatureRequirement(StrUtil.trim(reqVO.getNatureRequirement()));
        apply.setCertRequirement(StrUtil.trim(reqVO.getCertRequirement()));
        apply.setUseDeptReviewerUserId(reqVO.getUseDeptReviewerUserId());
        apply.setUseDeptReviewerUserName(resolveUserName(reqVO.getUseDeptReviewerUserId(),
                reqVO.getUseDeptReviewerUserName()));
        apply.setQualityReviewerUserId(reqVO.getQualityReviewerUserId());
        apply.setQualityReviewerUserName(resolveUserName(reqVO.getQualityReviewerUserId(),
                reqVO.getQualityReviewerUserName()));
        apply.setTechReviewerUserId(reqVO.getTechReviewerUserId());
        apply.setTechReviewerUserName(resolveUserName(reqVO.getTechReviewerUserId(),
                reqVO.getTechReviewerUserName()));
        apply.setPurchaseReviewerUserId(reqVO.getPurchaseReviewerUserId());
        apply.setPurchaseReviewerUserName(resolveUserName(reqVO.getPurchaseReviewerUserId(),
                reqVO.getPurchaseReviewerUserName()));
        apply.setGeneralManagerUserId(reqVO.getGeneralManagerUserId());
        apply.setGeneralManagerUserName(resolveUserName(reqVO.getGeneralManagerUserId(),
                reqVO.getGeneralManagerUserName()));
        apply.setApplyDept(StrUtil.trim(reqVO.getApplyDept()));
    }

    private void copyQualifiedSupplierSnapshot(SrmSupplierExitApprovalSaveReqVO reqVO,
            SrmSupplierExitApprovalDO apply) {
        MesSupplierDO supplier = findQualifiedSupplier(reqVO.getSupplierId(), reqVO.getSupplierCode());
        if (supplier == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_QUALIFIED_REQUIRED);
        }
        supplierScopeService.assertSupplierVisible(supplier);

        String materialCode = firstNotBlank(supplier.getMaterialCode(), reqVO.getMaterialCode());
        HcMaterialDO material = findMaterialByCode(materialCode);
        apply.setSupplierId(supplier.getId());
        apply.setSupplierCode(StrUtil.trim(supplier.getSupplierCode()));
        apply.setSupplierName(StrUtil.trim(supplier.getSupplierName()));
        apply.setMaterialCode(materialCode);
        apply.setMaterialName(firstNotBlank(material == null ? null : material.getMaterialName(),
                reqVO.getMaterialName(), supplier.getProvidedProduct(), supplier.getMainProducts(),
                supplier.getApplicableProduct()));
        apply.setMaterialModel(firstNotBlank(material == null ? null : material.getSpecModel(),
                material == null ? null : material.getModelCode(), supplier.getModel(),
                reqVO.getMaterialModel()));
        if (StrUtil.isBlank(apply.getMaterialCode()) || StrUtil.isBlank(apply.getMaterialName())) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_MATERIAL_REQUIRED);
        }
    }

    private MesSupplierDO findQualifiedSupplier(Long supplierId, String supplierCode) {
        if (supplierId != null) {
            MesSupplierDO supplier = supplierMapper.selectById(supplierId);
            return supplier != null && QUALIFIED_STATUS.equals(supplier.getStatus()) ? supplier : null;
        }
        if (StrUtil.isBlank(supplierCode)) {
            return null;
        }
        return supplierMapper.selectOne(new LambdaQueryWrapper<MesSupplierDO>()
                .eq(MesSupplierDO::getSupplierCode, StrUtil.trim(supplierCode))
                .eq(MesSupplierDO::getStatus, QUALIFIED_STATUS)
                .last("LIMIT 1"));
    }

    private HcMaterialDO findMaterialByCode(String materialCode) {
        if (StrUtil.isBlank(materialCode)) {
            return null;
        }
        return materialMapper.selectOne(new LambdaQueryWrapper<HcMaterialDO>()
                .eq(HcMaterialDO::getMaterialCode, StrUtil.trim(materialCode))
                .last("LIMIT 1"));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }

    private SrmSupplierExitApprovalRespVO buildResp(SrmSupplierExitApprovalDO apply, boolean includeLogs) {
        SrmSupplierExitApprovalRespVO respVO = BeanUtils.toBean(apply, SrmSupplierExitApprovalRespVO.class);
        Long userId = currentUser().id();
        boolean maintainer = isMaintainer(apply, userId);
        SrmSupplierExitApprovalSignDO currentSign =
                supplierExitApprovalSignMapper.selectByApplyIdAndUserId(apply.getId(), userId);
        respVO.setCanEdit(maintainer && STATUS_DRAFT.equals(apply.getStatus()));
        respVO.setCanSubmit(maintainer && STATUS_DRAFT.equals(apply.getStatus()));
        respVO.setCanPurchaseIntake(STATUS_PURCHASE_INTAKE.equals(apply.getStatus())
                && findRunningTask(apply.getProcessInstanceId(), BPM_NODE_PURCHASE_INTAKE, userId) != null);
        respVO.setCanUseDeptReview(Objects.equals(apply.getUseDeptReviewerUserId(), userId)
                && STATUS_USE_DEPT_REVIEW.equals(apply.getStatus()));
        respVO.setCanQualityReview(Objects.equals(apply.getQualityReviewerUserId(), userId)
                && STATUS_QUALITY_REVIEW.equals(apply.getStatus()));
        respVO.setCanTechReview(Objects.equals(apply.getTechReviewerUserId(), userId)
                && STATUS_TECH_REVIEW.equals(apply.getStatus()));
        respVO.setCanPurchaseReview(Objects.equals(apply.getPurchaseReviewerUserId(), userId)
                && STATUS_PURCHASE_REVIEW.equals(apply.getStatus()));
        respVO.setCanGeneralManagerReview(Objects.equals(apply.getGeneralManagerUserId(), userId)
                && STATUS_GENERAL_MANAGER_REVIEW.equals(apply.getStatus()));
        respVO.setCanSign(currentSign != null && STATUS_DEPT_SIGN.equals(apply.getStatus()));
        respVO.setCurrentSignId(currentSign == null ? null : currentSign.getId());
        respVO.setCanPurchaseTransfer(Objects.equals(apply.getPurchaseHandlerUserId(), userId)
                && STATUS_PURCHASE_TRANSFER.equals(apply.getStatus()));
        respVO.setCanMaterialEntry(Objects.equals(apply.getMaterialEntryUserId(), userId)
                && STATUS_ENTRY_PROCESSING.equals(apply.getStatus()));
        respVO.setCanSupplierRosterEntry(Objects.equals(apply.getSupplierRosterEntryUserId(), userId)
                && STATUS_ENTRY_PROCESSING.equals(apply.getStatus()));
        respVO.setCanArchive(Boolean.TRUE.equals(respVO.getCanPurchaseTransfer()) && isEntryCompleted(apply));
        if (includeLogs) {
            respVO.setSigns(supplierExitApprovalSignMapper.selectListByApplyId(apply.getId()).stream()
                    .map(item -> BeanUtils.toBean(item, SrmSupplierExitApprovalRespVO.Sign.class)).toList());
            respVO.setLogs(supplierExitApprovalLogMapper.selectListByApplyId(apply.getId()).stream()
                    .map(this::buildLogResp).toList());
        }
        return respVO;
    }

    private SrmSupplierExitApprovalRespVO.Log buildLogResp(SrmSupplierExitApprovalLogDO log) {
        SrmSupplierExitApprovalRespVO.Log respVO = BeanUtils.toBean(log, SrmSupplierExitApprovalRespVO.Log.class);
        respVO.setActionName(actionText(log.getAction()));
        return respVO;
    }

    private void finishReview(SrmSupplierExitApprovalDO apply, String taskKey, Long assigneeUserId,
            String reason, String nextStatus, String nextTaskKey, Long nextAssigneeUserId, String passDescription) {
        String result = reviewResultByTaskKey(apply, taskKey);
        if (REVIEW_REJECT.equals(result)) {
            rejectBpmTask(apply, taskKey, assigneeUserId, reason + "不通过");
            updateStatus(apply, STATUS_REJECTED, reason + "不通过", Map.of("result", result));
            return;
        }
        approveBpmTask(apply, taskKey, assigneeUserId, reason,
                nextTaskKey == null ? null : nextAssignees(nextTaskKey, List.of(nextAssigneeUserId)));
        updateStatus(apply, nextStatus, passDescription, Map.of("result", result));
    }

    private String reviewResultByTaskKey(SrmSupplierExitApprovalDO apply, String taskKey) {
        return switch (taskKey) {
            case BPM_NODE_USE_DEPT_REVIEW -> apply.getUseDeptResult();
            case BPM_NODE_QUALITY_REVIEW -> apply.getQualityResult();
            case BPM_NODE_TECH_REVIEW -> apply.getTechResult();
            case BPM_NODE_PURCHASE_REVIEW -> apply.getPurchaseResult();
            case BPM_NODE_GENERAL_MANAGER_REVIEW -> apply.getGeneralManagerResult();
            default -> REVIEW_PASS;
        };
    }

    private String startBpmProcess(SrmSupplierExitApprovalDO apply) {
        ensureBpmModelDeployed();
        Map<String, Object> variables = buildBpmVariables(apply);
        variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, new HashMap<String, List<Long>>());
        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY_SUPPLIER_EXIT_APPROVAL)
                .setBusinessKey(String.valueOf(apply.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(new HashMap<>());
        try {
            return bpmProcessInstanceApi.createProcessInstance(apply.getApplicantId(), createReq);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(apply.getApplicantId(), createReq);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(SRM_SUPPLIER_EXIT_APPROVAL_BPM_NOT_PUBLISHED);
                }
                throw retryEx;
            }
        }
    }

    private void ensureBpmModelDeployed() {
        Model model = bpmModelService.getModel(BPM_MODEL_ID_SUPPLIER_EXIT_APPROVAL);
        if (model != null && StrUtil.isNotBlank(model.getDeploymentId())) {
            return;
        }
        deployBpmModel();
    }

    private void deployBpmModel() {
        try {
            bpmModelService.deployModel(resolveBpmDeployUserId(), BPM_MODEL_ID_SUPPLIER_EXIT_APPROVAL);
        } catch (ServiceException ex) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_BPM_NOT_PUBLISHED);
        }
    }

    private Long resolveBpmDeployUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Model model = bpmModelService.getModel(BPM_MODEL_ID_SUPPLIER_EXIT_APPROVAL);
        if (model == null || StrUtil.isBlank(model.getMetaInfo())) {
            return loginUserId == null ? 1L : loginUserId;
        }
        Map<String, Object> metaInfo = JsonUtils.parseObject(model.getMetaInfo(), Map.class);
        Object managerUserIds = metaInfo == null ? null : metaInfo.get("managerUserIds");
        if (managerUserIds instanceof List<?> managerList && CollUtil.isNotEmpty(managerList)) {
            for (Object managerUserId : managerList) {
                Long parsedUserId = parseLongSafely(managerUserId);
                if (Objects.equals(parsedUserId, loginUserId)) {
                    return parsedUserId;
                }
            }
            Long firstManagerUserId = parseLongSafely(managerList.get(0));
            if (firstManagerUserId != null) {
                return firstManagerUserId;
            }
        }
        return loginUserId == null ? 1L : loginUserId;
    }

    private void approveBpmTask(SrmSupplierExitApprovalDO apply, String taskKey, Long assigneeUserId,
            String reason, Map<String, List<Long>> nextAssignees) {
        approveBpmTask(apply, taskKey, assigneeUserId, reason, nextAssignees, Map.of());
    }

    private void approveBpmTask(SrmSupplierExitApprovalDO apply, String taskKey, Long assigneeUserId,
            String reason, Map<String, List<Long>> nextAssignees, Map<String, Object> extraVariables) {
        Task task = findRunningTask(apply.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_BPM_TASK_MISSING);
        }
        Map<String, Object> variables = buildBpmVariables(apply);
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

    private Map<String, Object> buildGeneralManagerReturnVariables(SrmSupplierExitApprovalDO apply,
            Map<String, List<Long>> nextAssignees) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, nextAssignees);
        variables.put(BPM_VARIABLE_COLL_USER_LIST, List.of(apply.getPurchaseHandlerUserId()));
        return variables;
    }

    private void moveEntryTaskBackToPurchaseTransferIfNeeded(SrmSupplierExitApprovalDO apply) {
        if (StrUtil.isBlank(apply.getProcessInstanceId())) {
            return;
        }
        List<Task> entryTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(apply.getProcessInstanceId(),
                true, BPM_NODE_ENTRY_PROCESS);
        if (CollUtil.isEmpty(entryTasks)) {
            return;
        }
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(apply.getProcessInstanceId())
                .moveActivityIdsToSingleActivityId(List.of(BPM_NODE_ENTRY_PROCESS), BPM_NODE_PURCHASE_TRANSFER)
                .changeState();
    }

    private void rejectBpmTask(SrmSupplierExitApprovalDO apply, String taskKey, Long assigneeUserId, String reason) {
        Task task = findRunningTask(apply.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_BPM_TASK_MISSING);
        }
        BpmTaskRejectReqVO rejectReq = new BpmTaskRejectReqVO().setId(task.getId()).setReason(reason);
        bpmTaskService.rejectTask(assigneeUserId, rejectReq);
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

    private boolean hasRunningTask(String processInstanceId, String taskKey) {
        if (StrUtil.isBlank(processInstanceId)) {
            return false;
        }
        return CollUtil.isNotEmpty(bpmTaskService.getRunningTaskListByProcessInstanceId(processInstanceId,
                true, taskKey));
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

    private Map<String, Object> buildBpmVariables(SrmSupplierExitApprovalDO apply) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("supplierExitApprovalId", apply.getId());
        variables.put("businessKey", apply.getId());
        variables.put("exitNo", apply.getExitNo());
        variables.put("supplierName", apply.getSupplierName());
        variables.put("supplierCode", apply.getSupplierCode());
        variables.put("materialName", apply.getMaterialName());
        variables.put("materialCode", apply.getMaterialCode());
        variables.put("materialModel", apply.getMaterialModel());
        variables.put("applyDept", apply.getApplyDept());
        variables.put("initiatorId", apply.getApplicantId());
        variables.put("purchaseHandlerUserId", apply.getPurchaseHandlerUserId());
        variables.put("useDeptReviewerUserId", apply.getUseDeptReviewerUserId());
        variables.put("qualityReviewerUserId", apply.getQualityReviewerUserId());
        variables.put("techReviewerUserId", apply.getTechReviewerUserId());
        variables.put("purchaseReviewerUserId", apply.getPurchaseReviewerUserId());
        variables.put("generalManagerUserId", apply.getGeneralManagerUserId());
        variables.put("materialEntryUserId", apply.getMaterialEntryUserId());
        variables.put("supplierRosterEntryUserId", apply.getSupplierRosterEntryUserId());
        List<Long> signUserIds = supplierExitApprovalSignMapper.selectListByApplyId(apply.getId()).stream()
                .map(SrmSupplierExitApprovalSignDO::getUserId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isNotEmpty(signUserIds)) {
            variables.put("signUserIds", signUserIds);
            variables.put(BPM_VARIABLE_COLL_USER_LIST, signUserIds);
        }
        return variables;
    }

    private void updateStatus(SrmSupplierExitApprovalDO apply, String toStatus, String description, Object detail) {
        String fromStatus = apply.getStatus();
        apply.setStatus(toStatus);
        apply.setCurrentNodeName(statusText(toStatus));
        updateByIdChecked(apply);
        writeLog(apply.getId(), actionByStatus(toStatus), fromStatus, toStatus, description, detail);
    }

    private void updateByIdChecked(SrmSupplierExitApprovalDO apply) {
        if (supplierExitApprovalMapper.updateById(apply) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    private List<SrmSupplierExitApprovalSignDO> buildSignRows(Long applyId,
            List<SrmSupplierExitApprovalActionReqVO.SignUser> signUsers) {
        if (CollUtil.isEmpty(signUsers)) {
            return List.of();
        }
        Map<Long, SrmSupplierExitApprovalSignDO> unique = new LinkedHashMap<>();
        for (SrmSupplierExitApprovalActionReqVO.SignUser signUser : signUsers) {
            if (signUser.getUserId() == null || StrUtil.isBlank(signUser.getDeptName())) {
                continue;
            }
            UserSnapshot user = resolveUser(signUser.getUserId(), signUser.getUserName());
            String deptCode = StrUtil.trim(signUser.getDeptCode());
            String deptName = StrUtil.trim(signUser.getDeptName());
            unique.compute(user.id(), (ignored, existing) -> {
                if (existing == null) {
                    SrmSupplierExitApprovalSignDO sign = new SrmSupplierExitApprovalSignDO();
                    sign.setApplyId(applyId);
                    sign.setDeptCode(deptCode);
                    sign.setDeptName(deptName);
                    sign.setUserId(user.id());
                    sign.setUserName(user.name());
                    sign.setSignStatus(SIGN_STATUS_PENDING);
                    return sign;
                }
                if (!StrUtil.contains(existing.getDeptName(), deptName)) {
                    existing.setDeptName(existing.getDeptName() + "、" + deptName);
                }
                String existingDeptCode = StrUtil.blankToDefault(existing.getDeptCode(), "");
                if (StrUtil.isNotBlank(deptCode) && !StrUtil.contains(existingDeptCode, deptCode)) {
                    existing.setDeptCode(StrUtil.isBlank(existing.getDeptCode())
                            ? deptCode : existing.getDeptCode() + "," + deptCode);
                }
                return existing;
            });
        }
        return new ArrayList<>(unique.values());
    }

    private List<Long> resolvePermissionUserIds(String permission) {
        Set<Long> permissionUserIds = permissionApi.getUserIdListByPermissions(List.of(permission));
        if (CollUtil.isEmpty(permissionUserIds)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(permissionUserIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED);
        }
        List<Long> userIds = users.stream().map(AdminUserRespDTO::getId)
                .filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(userIds)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED);
        }
        return userIds;
    }

    private UserSnapshot resolveFirstPermissionUser(String permission) {
        List<Long> userIds = resolvePermissionUserIds(permission);
        return resolveUser(userIds.get(0), null);
    }

    private UserSnapshot resolveUser(Long userId, String fallbackName) {
        if (userId == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_REVIEWER_REQUIRED);
        }
        return new UserSnapshot(userId, StrUtil.blankToDefault(user.getNickname(),
                StrUtil.blankToDefault(user.getUsername(), StrUtil.trim(fallbackName))));
    }

    private boolean isEntryCompleted(SrmSupplierExitApprovalDO apply) {
        return Boolean.TRUE.equals(apply.getMaterialCodeCreated())
                && Boolean.TRUE.equals(apply.getSupplierRosterCreated())
                && apply.getMaterialCodeCompleteDate() != null
                && apply.getSupplierRosterCompleteDate() != null;
    }

    private String normalizeReviewResult(String result) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(result), REVIEW_PASS);
        if (!REVIEW_PASS.equals(normalized) && !REVIEW_REJECT.equals(normalized)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_REVIEW_RESULT_INVALID);
        }
        return normalized;
    }

    private void assertStatus(SrmSupplierExitApprovalDO apply, String expectedStatus) {
        if (!expectedStatus.equals(apply.getStatus())) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_STATUS_INVALID);
        }
    }

    private void assertMaintainer(SrmSupplierExitApprovalDO apply) {
        Long userId = currentUser().id();
        if (!isMaintainer(apply, userId)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY);
        }
    }

    private boolean isMaintainer(SrmSupplierExitApprovalDO apply, Long userId) {
        return Objects.equals(apply.getApplicantId(), userId) || isAdmin(userId);
    }

    private void assertCurrentOperator(Long expectedUserId) {
        Long userId = currentUser().id();
        if (!Objects.equals(expectedUserId, userId)) {
            throw exception(SRM_SUPPLIER_EXIT_APPROVAL_OPERATOR_ONLY);
        }
    }

    private boolean isAdmin(Long userId) {
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
        if (user == null) {
            return StrUtil.trim(fallbackName);
        }
        return StrUtil.blankToDefault(user.getNickname(), StrUtil.blankToDefault(user.getUsername(), fallbackName));
    }

    private UserSnapshot currentUser() {
        Initiator initiator = getCurrentInitiator();
        return new UserSnapshot(initiator.userId(), defaultString(initiator.userName(), "系统用户"));
    }

    private void writeLog(Long applyId, String action, String fromStatus, String toStatus,
            String description, Object detail) {
        UserSnapshot user = currentUser();
        SrmSupplierExitApprovalLogDO log = new SrmSupplierExitApprovalLogDO();
        log.setApplyId(applyId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setActionDescription(description);
        log.setDetailJson(detail == null ? null : JsonUtils.toJsonString(detail));
        supplierExitApprovalLogMapper.insert(log);
    }

    private String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_DRAFT)) {
            case STATUS_PURCHASE_INTAKE -> "采购部门办理";
            case STATUS_DEPT_SIGN -> "部门会签";
            case STATUS_PURCHASE_TRANSFER -> "采购部转办";
            case STATUS_ENTRY_PROCESSING -> "交办办理";
            case STATUS_USE_DEPT_REVIEW -> "使用部门负责人审核";
            case STATUS_QUALITY_REVIEW -> "品质部审批";
            case STATUS_TECH_REVIEW -> "技术研发部审批";
            case STATUS_PURCHASE_REVIEW -> "采购部审批";
            case STATUS_GENERAL_MANAGER_REVIEW -> "总经理审核";
            case STATUS_ARCHIVED -> "已归档";
            case STATUS_REJECTED -> "不通过";
            default -> "草稿";
        };
    }

    private String actionByStatus(String status) {
        return switch (StrUtil.blankToDefault(status, STATUS_DRAFT)) {
            case STATUS_PURCHASE_INTAKE -> "SUBMIT";
            case STATUS_DEPT_SIGN -> "PURCHASE_INTAKE";
            case STATUS_PURCHASE_TRANSFER -> "PURCHASE_TRANSFER";
            case STATUS_ENTRY_PROCESSING -> "ENTRY_PROCESS";
            case STATUS_USE_DEPT_REVIEW -> "SUBMIT";
            case STATUS_QUALITY_REVIEW -> "USE_DEPT_REVIEW";
            case STATUS_TECH_REVIEW -> "QUALITY_REVIEW";
            case STATUS_PURCHASE_REVIEW -> "TECH_REVIEW";
            case STATUS_GENERAL_MANAGER_REVIEW -> "PURCHASE_REVIEW";
            case STATUS_ARCHIVED -> "ARCHIVE";
            case STATUS_REJECTED -> "REJECT";
            default -> "UPDATE";
        };
    }

    private String actionText(String action) {
        return switch (StrUtil.blankToDefault(action, "")) {
            case "CREATE" -> "创建";
            case "ARCHIVE" -> "归档";
            case "DEPT_SIGN" -> "部门会签";
            case "ENTRY_PROCESS" -> "交办办理";
            case "UPDATE" -> "更新";
            case "SUBMIT" -> "提交";
            case "PURCHASE_INTAKE" -> "采购部门办理";
            case "PURCHASE_TRANSFER" -> "采购部转办";
            case "USE_DEPT_REVIEW" -> "使用部门负责人审核";
            case "QUALITY_REVIEW" -> "品质部审批";
            case "TECH_REVIEW" -> "技术研发部审批";
            case "PURCHASE_REVIEW" -> "采购部审批";
            case "GENERAL_MANAGER_REVIEW" -> "总经理审核";
            case "REJECT" -> "不通过";
            default -> action;
        };
    }

    private void fillInitiatorDefaults(SrmSupplierExitApprovalDO apply, SrmSupplierExitApprovalDO old) {
        Initiator initiator = getCurrentInitiator();
        apply.setApplicantId(old == null
                ? defaultLong(initiator.userId(), apply.getApplicantId())
                : defaultLong(old.getApplicantId(), defaultLong(initiator.userId(), apply.getApplicantId())));
        apply.setApplicantName(old == null
                ? defaultString(initiator.userName(), apply.getApplicantName())
                : defaultString(old.getApplicantName(), defaultString(initiator.userName(), apply.getApplicantName())));
        apply.setApplyDept(old == null
                ? defaultString(initiator.deptName(), apply.getApplyDept())
                : defaultString(old.getApplyDept(), defaultString(initiator.deptName(), apply.getApplyDept())));
    }

    private Initiator getCurrentInitiator() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        Long deptId = SecurityFrameworkUtils.getLoginUserDeptId();
        if (userId != null && (StrUtil.isBlank(userName) || deptId == null)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null) {
                userName = defaultString(userName, user.getNickname());
                deptId = deptId == null ? user.getDeptId() : deptId;
            }
        }
        String deptName = null;
        if (deptId != null) {
            DeptRespDTO dept = deptApi.getDept(deptId);
            deptName = dept == null ? null : dept.getName();
        }
        return new Initiator(userId, userName, deptName);
    }

    private Long parseLongSafely(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = StrUtil.trim(String.valueOf(value));
        if (StrUtil.isBlank(text) || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record Initiator(Long userId, String userName, String deptName) {
    }

    private record UserSnapshot(Long id, String name) {
    }

    private static String defaultString(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    private static Long defaultLong(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static LocalDateTime defaultLocalDateTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }

}
