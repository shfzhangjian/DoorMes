package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentVersionUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFilePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFileSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmDocumentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmOnboardingApplyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmOnboardingApplyLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmOnboardingApplySignDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmDocumentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmOnboardingApplyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmOnboardingApplyLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmOnboardingApplySignMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierFileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSurveyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSurveyReviewMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.flowable.engine.repository.Model;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_DOCUMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_DOCUMENT_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ATTACHMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ATTACHMENT_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_APPLY_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_BPM_TASK_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_ENTRY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_REVIEW_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_SIGN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_ONBOARDING_APPLY_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SURVEY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SURVEY_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmServiceImpl implements SrmService {

    private static final String BIZ_SUPPLIER_FILE = "SUPPLIER_FILE";
    private static final String BIZ_ONBOARDING_APPLY = "ONBOARDING_APPLY";
    private static final String BIZ_SURVEY = "SURVEY";
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
    private static final String SIGN_STATUS_PENDING = "PENDING";
    private static final String SIGN_STATUS_COMPLETED = "COMPLETED";
    private static final String TRANSFER_ACTION_GENERAL_MANAGER = "GENERAL_MANAGER";
    private static final String TRANSFER_ACTION_ASSIGN_ENTRY = "ASSIGN_ENTRY";
    private static final String TRANSFER_ACTION_ARCHIVE = "ARCHIVE";
    private static final String BPM_PROCESS_KEY_ONBOARDING_APPLY = "srm_onboarding_apply";
    private static final String BPM_MODEL_ID_ONBOARDING_APPLY = "srm-onboarding-apply-model";
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
    private static final String PURCHASE_IMPORT_PERMISSION = "mes:srm-onboarding-apply:purchase-import";
    private static final String GENERAL_MANAGER_PERMISSION = "mes:srm-onboarding-apply:general-manager";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1_009_003_002;
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String SRM_SUPER_ADMIN_ROLE = "srm_supplier_super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String ONBOARDING_APPLY_NO_PREFIX = "XWLDR-";
    private static final DateTimeFormatter ONBOARDING_APPLY_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyyMMdd");
    private static final int ONBOARDING_APPLY_NO_DAILY_SEQUENCE_LIMIT = 999;
    private static final String FILE_STATUS_VALID = "VALID";
    private static final String FILE_STATUS_WARNING = "WARNING";
    private static final String FILE_STATUS_EXPIRED = "EXPIRED";
    private static final int DEFAULT_WARNING_DAYS = 31;
    private static final String FILE_TYPE_SYSTEM_CERT = "SYSTEM_CERT";
    private static final String FILE_TYPE_COMPLIANCE_AGREEMENT = "COMPLIANCE_AGREEMENT";
    private static final String FILE_TYPE_ENVIRONMENT_CERT = "ENVIRONMENT_CERT";
    private static final String EXPIRY_RULE_MANUAL = "MANUAL";
    private static final String EXPIRY_RULE_AUTO_12M_MINUS_1D = "AUTO_12M_MINUS_1D";
    private static final String EXPIRY_RULE_AUTO_60M_MINUS_1D = "AUTO_60M_MINUS_1D";
    private static final Set<String> ENVIRONMENT_FILE_NAMES_12_MONTH = Set.of(
            "卤素", "ROHS", "REACH");
    private static final Set<String> ENVIRONMENT_FILE_NAMES_60_MONTH = Set.of(
            "MSDS", "MSDS/TDS");
    private static final int INITIAL_VERSION = 0;
    private static final int INITIAL_ATTACHMENT_VERSION = 1;
    private static final int ATTACHMENT_FILE_TYPE_MAX_LENGTH = 64;
    private static final String ATTACHMENT_CATEGORY_OTHER = "OTHER";
    private static final String ATTACHMENT_FIRST_UPLOAD_DESCRIPTION = "首次上传";
    private static final String SURVEY_NO_PREFIX = "GYXX-";
    private static final DateTimeFormatter SURVEY_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyyMMdd");
    private static final int SURVEY_NO_DAILY_SEQUENCE_LIMIT = 999;

    @Resource
    private SrmAttachmentMapper attachmentMapper;
    @Resource
    private SrmDocumentMapper documentMapper;
    @Resource
    private SrmSupplierFileMapper supplierFileMapper;
    @Resource
    private SrmOnboardingApplyMapper onboardingApplyMapper;
    @Resource
    private SrmOnboardingApplyLogMapper onboardingApplyLogMapper;
    @Resource
    private SrmOnboardingApplySignMapper onboardingApplySignMapper;
    @Resource
    private SrmSurveyMapper surveyMapper;
    @Resource
    private SrmSurveyReviewMapper surveyReviewMapper;
    @Resource
    private SrmSupplierCandidateService supplierCandidateService;
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
    public Long createAttachment(SrmAttachmentSaveReqVO reqVO) {
        SrmAttachmentDO attachment = BeanUtils.toBean(reqVO, SrmAttachmentDO.class);
        attachment.setFileType(normalizeAttachmentFileType(reqVO.getFileName(), reqVO.getFileType()));
        initializeAttachmentVersion(attachment, reqVO.getAttachmentCategory());
        attachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long updateAttachmentVersion(SrmAttachmentVersionUpdateReqVO reqVO) {
        SrmAttachmentDO source = attachmentMapper.selectById(reqVO.getSourceAttachmentId());
        if (source == null) {
            throw exception(SRM_ATTACHMENT_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(source.getLatestVersion())) {
            throw exception(SRM_ATTACHMENT_VERSION_CONFLICT);
        }
        String versionGroupNo = ensureAttachmentVersionGroup(source);
        if (attachmentMapper.updateLatestVersion(source.getId(), true, false) == 0) {
            throw exception(SRM_ATTACHMENT_VERSION_CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        SrmAttachmentDO attachment = new SrmAttachmentDO();
        attachment.setBizType(source.getBizType());
        attachment.setBizId(source.getBizId());
        attachment.setAttachmentCategory(reqVO.getAttachmentCategory());
        attachment.setFileName(reqVO.getFileName());
        attachment.setFileUrl(reqVO.getFileUrl());
        attachment.setFileType(normalizeAttachmentFileType(reqVO.getFileName(), reqVO.getFileType()));
        attachment.setFileSize(reqVO.getFileSize());
        attachment.setVersionGroupNo(versionGroupNo);
        attachment.setVersionNo(defaultInteger(source.getVersionNo(), INITIAL_ATTACHMENT_VERSION) + 1);
        attachment.setPreviousAttachmentId(source.getId());
        attachment.setLatestVersion(true);
        attachment.setUploadTime(now);
        attachment.setVersionTime(now);
        attachment.setUpdateDescription(StrUtil.trim(reqVO.getUpdateDescription()));
        fillAttachmentUploaderDefaults(attachment);
        attachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long id) {
        SrmAttachmentDO attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw exception(SRM_ATTACHMENT_NOT_EXISTS);
        }
        attachmentMapper.deleteById(id);
        if (!Boolean.TRUE.equals(attachment.getLatestVersion())
                || StrUtil.isBlank(attachment.getVersionGroupNo())) {
            return;
        }
        List<SrmAttachmentDO> histories = attachmentMapper.selectByVersionGroup(attachment.getVersionGroupNo());
        if (!histories.isEmpty()) {
            attachmentMapper.updateLatestVersion(histories.get(0).getId(), false, true);
        }
    }

    @Override
    public List<SrmAttachmentDO> getAttachmentList(String bizType, Long bizId, boolean includeHistory) {
        return attachmentMapper.selectByBiz(bizType, bizId, includeHistory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplierFile(SrmSupplierFileSaveReqVO reqVO) {
        SrmSupplierFileDO supplierFile = BeanUtils.toBean(reqVO, SrmSupplierFileDO.class);
        fillSupplierFileDefaults(supplierFile);
        supplierFileMapper.insert(supplierFile);
        syncPrimaryAttachment(BIZ_SUPPLIER_FILE, supplierFile.getId(), supplierFile.getAttachmentName(),
                supplierFile.getAttachmentUrl(), supplierFile.getFileType());
        return supplierFile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplierFile(SrmSupplierFileSaveReqVO reqVO) {
        validateSupplierFileExists(reqVO.getId());
        SrmSupplierFileDO updateObj = BeanUtils.toBean(reqVO, SrmSupplierFileDO.class);
        fillSupplierFileDefaults(updateObj);
        if (supplierFileMapper.updateById(updateObj) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
        syncPrimaryAttachment(BIZ_SUPPLIER_FILE, updateObj.getId(), updateObj.getAttachmentName(),
                updateObj.getAttachmentUrl(), updateObj.getFileType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierFile(Long id) {
        validateSupplierFileExists(id);
        attachmentMapper.deleteByBiz(BIZ_SUPPLIER_FILE, id);
        supplierFileMapper.deleteById(id);
    }

    @Override
    public SrmSupplierFileDO getSupplierFile(Long id) {
        return supplierFileMapper.selectById(id);
    }

    @Override
    public PageResult<SrmSupplierFileDO> getSupplierFilePage(SrmSupplierFilePageReqVO reqVO) {
        return supplierFileMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOnboardingApply(SrmOnboardingApplySaveReqVO reqVO) {
        String applyNo = generateOnboardingApplyNo();
        validateApplyNoUnique(null, applyNo);
        SrmOnboardingApplyDO apply = new SrmOnboardingApplyDO();
        copyOnboardingApplyEditableFields(reqVO, apply);
        apply.setApplyNo(applyNo);
        apply.setStatus(STATUS_DRAFT);
        apply.setCurrentNodeName(onboardingApplyStatusText(STATUS_DRAFT));
        apply.setApplyTime(defaultLocalDateTime(reqVO.getApplyTime()));
        fillOnboardingApplyInitiatorDefaults(apply, null);
        apply.setVersion(INITIAL_VERSION);
        onboardingApplyMapper.insert(apply);
        writeOnboardingApplyLog(apply.getId(), "CREATE", null, STATUS_DRAFT, "创建导入申请", null);
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOnboardingApply(SrmOnboardingApplySaveReqVO reqVO) {
        SrmOnboardingApplyDO old = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyMaintainer(old);
        assertOnboardingApplyStatus(old, STATUS_DRAFT);
        copyOnboardingApplyEditableFields(reqVO, old);
        old.setVersion(reqVO.getVersion());
        updateOnboardingApplyByIdChecked(old);
        writeOnboardingApplyLog(old.getId(), "UPDATE", STATUS_DRAFT, STATUS_DRAFT, "更新导入申请", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOnboardingApply(Long id) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(id);
        assertOnboardingApplyMaintainer(apply);
        assertOnboardingApplyStatus(apply, STATUS_DRAFT);
        attachmentMapper.deleteByBiz(BIZ_ONBOARDING_APPLY, id);
        onboardingApplyMapper.deleteById(id);
    }

    @Override
    public SrmOnboardingApplyRespVO getOnboardingApply(Long id) {
        return buildOnboardingApplyResp(validateOnboardingApplyExists(id), true);
    }

    @Override
    public PageResult<SrmOnboardingApplyRespVO> getOnboardingApplyPage(SrmOnboardingApplyPageReqVO reqVO) {
        PageResult<SrmOnboardingApplyDO> page = onboardingApplyMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream()
                .map(item -> buildOnboardingApplyResp(item, false)).toList(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOnboardingApply(SrmOnboardingApplyActionReqVO.Submit reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyMaintainer(apply);
        assertOnboardingApplyStatus(apply, STATUS_DRAFT);
        List<Long> purchaseImportUserIds = resolveOnboardingApplyPermissionUserIds(PURCHASE_IMPORT_PERMISSION);
        String processInstanceId = startOnboardingApplyBpmProcess(apply);
        apply.setProcessInstanceId(processInstanceId);
        approveOnboardingApplyBpmTask(apply, BPM_NODE_START, apply.getApplicantId(), "提交导入申请",
                Map.of(), Map.of("purchaseImportUserIds", purchaseImportUserIds,
                        BPM_VARIABLE_COLL_USER_LIST, purchaseImportUserIds));
        updateOnboardingApplyStatus(apply, STATUS_PURCHASE_INTAKE, "提交导入申请，进入采购部门办理",
                Map.of("purchaseImportUserIds", purchaseImportUserIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseIntakeOnboardingApply(SrmOnboardingApplyActionReqVO.PurchaseIntake reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_PURCHASE_INTAKE);
        UserSnapshot operator = currentOnboardingApplyUser();
        if (findOnboardingApplyRunningTask(apply.getProcessInstanceId(), BPM_NODE_PURCHASE_INTAKE, operator.id()) == null) {
            throw exception(SRM_ONBOARDING_APPLY_BPM_TASK_MISSING);
        }
        UserSnapshot useDeptReviewer = resolveOnboardingApplyUser(reqVO.getUseDeptReviewerUserId(),
                reqVO.getUseDeptReviewerUserName());
        apply.setPurchaseHandlerUserId(operator.id());
        apply.setPurchaseHandlerUserName(operator.name());
        apply.setPurchaseReviewerUserId(operator.id());
        apply.setPurchaseReviewerUserName(operator.name());
        apply.setPurchaseIntakeOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setPurchaseIntakeTime(LocalDateTime.now());
        apply.setUseDeptReviewerUserId(useDeptReviewer.id());
        apply.setUseDeptReviewerUserName(useDeptReviewer.name());

        List<SrmOnboardingApplyActionReqVO.SignUser> signUsers = new ArrayList<>();
        SrmOnboardingApplyActionReqVO.SignUser useDeptSignUser = new SrmOnboardingApplyActionReqVO.SignUser();
        useDeptSignUser.setDeptCode("USE_DEPT");
        useDeptSignUser.setDeptName("使用部门负责人");
        useDeptSignUser.setUserId(useDeptReviewer.id());
        useDeptSignUser.setUserName(useDeptReviewer.name());
        signUsers.add(useDeptSignUser);
        if (CollUtil.isNotEmpty(reqVO.getSignUsers())) {
            signUsers.addAll(reqVO.getSignUsers());
        }
        List<SrmOnboardingApplySignDO> signs = buildOnboardingApplySignRows(apply.getId(), signUsers);
        if (CollUtil.isEmpty(signs)) {
            throw exception(SRM_ONBOARDING_APPLY_SIGN_REQUIRED);
        }
        onboardingApplySignMapper.deleteByApplyId(apply.getId());
        onboardingApplySignMapper.insertBatch(signs);
        List<Long> signUserIds = signs.stream().map(SrmOnboardingApplySignDO::getUserId).distinct().toList();
        Map<String, List<Long>> deptSignAssignees = nextAssignees(BPM_NODE_DEPT_SIGN, signUserIds);
        Map<String, Object> bpmVariables = new HashMap<>();
        bpmVariables.put("signUserIds", signUserIds);
        bpmVariables.put(BPM_VARIABLE_COLL_USER_LIST, signUserIds);
        bpmVariables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, deptSignAssignees);
        approveOnboardingApplyBpmTask(apply, BPM_NODE_PURCHASE_INTAKE, operator.id(), "采购部门办理",
                deptSignAssignees, bpmVariables);
        updateOnboardingApplyStatus(apply, STATUS_DEPT_SIGN, "采购部门办理完成，进入部门会签",
                Map.of("signUserIds", signUserIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signOnboardingApply(SrmOnboardingApplyActionReqVO.Sign reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_DEPT_SIGN);
        Long userId = currentOnboardingApplyUser().id();
        List<SrmOnboardingApplySignDO> pendingSigns = onboardingApplySignMapper.selectPendingListByApplyIdAndUserId(
                apply.getId(), userId);
        if (CollUtil.isEmpty(pendingSigns)) {
            throw exception(SRM_ONBOARDING_APPLY_OPERATOR_ONLY);
        }
        if (reqVO.getSignId() != null
                && pendingSigns.stream().noneMatch(sign -> Objects.equals(sign.getId(), reqVO.getSignId()))) {
            throw exception(SRM_ONBOARDING_APPLY_OPERATOR_ONLY);
        }
        String result = normalizeReviewResult(reqVO.getResult());
        LocalDateTime now = LocalDateTime.now();
        for (SrmOnboardingApplySignDO sign : pendingSigns) {
            sign.setSignStatus(SIGN_STATUS_COMPLETED);
            sign.setSignResult(result);
            sign.setSignOpinion(StrUtil.trim(reqVO.getOpinion()));
            sign.setSignTime(now);
            onboardingApplySignMapper.updateById(sign);
        }
        approveOnboardingApplyBpmTask(apply, BPM_NODE_DEPT_SIGN, userId, "部门会签",
                nextAssignees(BPM_NODE_PURCHASE_TRANSFER, List.of(apply.getPurchaseHandlerUserId())));
        boolean allCompleted = onboardingApplySignMapper.selectListByApplyId(apply.getId()).stream()
                .allMatch(sign -> SIGN_STATUS_COMPLETED.equals(sign.getSignStatus()));
        if (allCompleted) {
            updateOnboardingApplyStatus(apply, STATUS_PURCHASE_TRANSFER, "部门会签完成，返回采购部转办", null);
        } else {
            writeOnboardingApplyLog(apply.getId(), "DEPT_SIGN", STATUS_DEPT_SIGN, STATUS_DEPT_SIGN,
                    "部门会签", Map.of("result", result));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseTransferOnboardingApply(SrmOnboardingApplyActionReqVO.PurchaseTransfer reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_PURCHASE_TRANSFER);
        assertOnboardingApplyCurrentOperator(apply.getPurchaseHandlerUserId());
        String transferAction = StrUtil.trim(reqVO.getTransferAction());
        apply.setPurchaseResult(REVIEW_PASS);
        apply.setPurchaseOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setPurchaseHandleTime(LocalDateTime.now());
        if (TRANSFER_ACTION_GENERAL_MANAGER.equals(transferAction)) {
            UserSnapshot generalManager = reqVO.getGeneralManagerUserId() == null
                    ? resolveFirstOnboardingApplyPermissionUser(GENERAL_MANAGER_PERMISSION)
                    : resolveOnboardingApplyUser(reqVO.getGeneralManagerUserId(), reqVO.getGeneralManagerUserName());
            apply.setGeneralManagerUserId(generalManager.id());
            apply.setGeneralManagerUserName(generalManager.name());
            approveOnboardingApplyBpmTask(apply, BPM_NODE_PURCHASE_TRANSFER, apply.getPurchaseHandlerUserId(),
                    "采购部转办总经理审核",
                    nextAssignees(BPM_NODE_GENERAL_MANAGER_REVIEW, List.of(generalManager.id())),
                    Map.of("transferAction", transferAction, "generalManagerUserId", generalManager.id()));
            updateOnboardingApplyStatus(apply, STATUS_GENERAL_MANAGER_REVIEW, "采购部转办，进入总经理审核",
                    Map.of("generalManagerUserId", generalManager.id()));
            return;
        }
        if (TRANSFER_ACTION_ASSIGN_ENTRY.equals(transferAction)) {
            UserSnapshot materialEntryUser = resolveOnboardingApplyUser(reqVO.getMaterialEntryUserId(),
                    reqVO.getMaterialEntryUserName());
            UserSnapshot supplierRosterEntryUser = resolveOnboardingApplyUser(reqVO.getSupplierRosterEntryUserId(),
                    reqVO.getSupplierRosterEntryUserName());
            apply.setMaterialEntryUserId(materialEntryUser.id());
            apply.setMaterialEntryUserName(materialEntryUser.name());
            apply.setMaterialEntryRequiredDate(null);
            apply.setMaterialEntryCode(null);
            apply.setMaterialEntryOpinion(null);
            apply.setMaterialEntryHandleTime(null);
            apply.setMaterialCode(null);
            apply.setMaterialCodeCreated(false);
            apply.setMaterialCodeCompleteDate(null);
            apply.setSupplierRosterEntryUserId(supplierRosterEntryUser.id());
            apply.setSupplierRosterEntryUserName(supplierRosterEntryUser.name());
            apply.setSupplierRosterEntryRequiredDate(null);
            apply.setSupplierRosterEntryCode(null);
            apply.setSupplierRosterEntryOpinion(null);
            apply.setSupplierRosterEntryHandleTime(null);
            apply.setSupplierCode(null);
            apply.setSupplierRosterCreated(false);
            apply.setSupplierRosterCompleteDate(null);
            List<Long> entryUserIds = List.of(materialEntryUser.id(), supplierRosterEntryUser.id());
            Map<String, List<Long>> entryAssignees = nextAssignees(BPM_NODE_ENTRY_PROCESS, entryUserIds);
            Map<String, Object> bpmVariables = new HashMap<>();
            bpmVariables.put("transferAction", transferAction);
            bpmVariables.put("entryUserIds", entryUserIds.stream().distinct().toList());
            bpmVariables.put(BPM_VARIABLE_COLL_USER_LIST, entryUserIds.stream().distinct().toList());
            bpmVariables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, entryAssignees);
            approveOnboardingApplyBpmTask(apply, BPM_NODE_PURCHASE_TRANSFER, apply.getPurchaseHandlerUserId(),
                    "采购部交办录入", entryAssignees, bpmVariables);
            updateOnboardingApplyStatus(apply, STATUS_ENTRY_PROCESSING, "采购部交办物料编码和供方清单录入",
                    Map.of("entryUserIds", entryUserIds.stream().distinct().toList()));
            return;
        }
        if (TRANSFER_ACTION_ARCHIVE.equals(transferAction)) {
            if (!isOnboardingApplyEntryCompleted(apply)) {
                throw exception(SRM_ONBOARDING_APPLY_ENTRY_REQUIRED);
            }
            approveOnboardingApplyBpmTask(apply, BPM_NODE_PURCHASE_TRANSFER, apply.getPurchaseHandlerUserId(),
                    "采购部确认归档", null, Map.of("transferAction", transferAction));
            updateOnboardingApplyStatus(apply, STATUS_ARCHIVED, "采购部确认完成归档", null);
            return;
        }
        throw exception(SRM_ONBOARDING_APPLY_STATUS_INVALID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void entryOnboardingApply(SrmOnboardingApplyActionReqVO.Entry reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_ENTRY_PROCESSING);
        Long userId = currentOnboardingApplyUser().id();
        Task task = findOnboardingApplyRunningTask(apply.getProcessInstanceId(), BPM_NODE_ENTRY_PROCESS, userId);
        if (task == null) {
            throw exception(SRM_ONBOARDING_APPLY_BPM_TASK_MISSING);
        }
        boolean handled = false;
        LocalDateTime now = LocalDateTime.now();
        if (Objects.equals(apply.getMaterialEntryUserId(), userId)) {
            if (StrUtil.isBlank(reqVO.getMaterialEntryCode())) {
                throw exception(SRM_ONBOARDING_APPLY_ENTRY_REQUIRED);
            }
            apply.setMaterialEntryCode(StrUtil.trim(reqVO.getMaterialEntryCode()));
            apply.setMaterialEntryOpinion(StrUtil.trim(reqVO.getMaterialEntryOpinion()));
            apply.setMaterialEntryHandleTime(now);
            apply.setMaterialCode(StrUtil.trim(reqVO.getMaterialEntryCode()));
            apply.setMaterialCodeCreated(true);
            apply.setMaterialCodeCompleteDate(LocalDate.now());
            handled = true;
        }
        if (Objects.equals(apply.getSupplierRosterEntryUserId(), userId)) {
            if (StrUtil.isBlank(reqVO.getSupplierRosterEntryCode())) {
                throw exception(SRM_ONBOARDING_APPLY_ENTRY_REQUIRED);
            }
            apply.setSupplierRosterEntryCode(StrUtil.trim(reqVO.getSupplierRosterEntryCode()));
            apply.setSupplierRosterEntryOpinion(StrUtil.trim(reqVO.getSupplierRosterEntryOpinion()));
            apply.setSupplierRosterEntryHandleTime(now);
            apply.setSupplierCode(StrUtil.trim(reqVO.getSupplierRosterEntryCode()));
            apply.setSupplierRosterCreated(true);
            apply.setSupplierRosterCompleteDate(LocalDate.now());
            handled = true;
        }
        if (!handled) {
            throw exception(SRM_ONBOARDING_APPLY_OPERATOR_ONLY);
        }
        approveOnboardingApplyBpmTask(apply, BPM_NODE_ENTRY_PROCESS, userId, "交办录入",
                nextAssignees(BPM_NODE_PURCHASE_TRANSFER, List.of(apply.getPurchaseHandlerUserId())));
        if (isOnboardingApplyEntryCompleted(apply)
                && !hasRunningOnboardingApplyTask(apply.getProcessInstanceId(), BPM_NODE_ENTRY_PROCESS)) {
            updateOnboardingApplyStatus(apply, STATUS_PURCHASE_TRANSFER, "交办录入完成，返回采购部转办", null);
        } else {
            updateOnboardingApplyByIdChecked(apply);
            writeOnboardingApplyLog(apply.getId(), "ENTRY_PROCESS", STATUS_ENTRY_PROCESSING,
                    STATUS_ENTRY_PROCESSING, "交办录入", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useDeptReviewOnboardingApply(SrmOnboardingApplyActionReqVO.Review reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_USE_DEPT_REVIEW);
        assertOnboardingApplyCurrentOperator(apply.getUseDeptReviewerUserId());
        apply.setUseDeptResult(normalizeReviewResult(reqVO.getResult()));
        apply.setUseDeptOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setUseDeptHandleTime(LocalDateTime.now());
        finishOnboardingApplyReview(apply, BPM_NODE_USE_DEPT_REVIEW, apply.getUseDeptReviewerUserId(),
                "使用部门负责人审核", STATUS_QUALITY_REVIEW, BPM_NODE_QUALITY_REVIEW,
                apply.getQualityReviewerUserId(), "使用部门负责人审核完成，进入品质部审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void qualityReviewOnboardingApply(SrmOnboardingApplyActionReqVO.Review reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_QUALITY_REVIEW);
        assertOnboardingApplyCurrentOperator(apply.getQualityReviewerUserId());
        apply.setQualityResult(normalizeReviewResult(reqVO.getResult()));
        apply.setQualityOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setQualityHandleTime(LocalDateTime.now());
        finishOnboardingApplyReview(apply, BPM_NODE_QUALITY_REVIEW, apply.getQualityReviewerUserId(),
                "品质部审批", STATUS_TECH_REVIEW, BPM_NODE_TECH_REVIEW,
                apply.getTechReviewerUserId(), "品质部审批完成，进入技术研发部审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void techReviewOnboardingApply(SrmOnboardingApplyActionReqVO.Review reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_TECH_REVIEW);
        assertOnboardingApplyCurrentOperator(apply.getTechReviewerUserId());
        apply.setTechResult(normalizeReviewResult(reqVO.getResult()));
        apply.setTechOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setTechHandleTime(LocalDateTime.now());
        finishOnboardingApplyReview(apply, BPM_NODE_TECH_REVIEW, apply.getTechReviewerUserId(),
                "技术研发部审批", STATUS_PURCHASE_REVIEW, BPM_NODE_PURCHASE_REVIEW,
                apply.getPurchaseReviewerUserId(), "技术研发部审批完成，进入采购部审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purchaseReviewOnboardingApply(SrmOnboardingApplyActionReqVO.Review reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_PURCHASE_REVIEW);
        assertOnboardingApplyCurrentOperator(apply.getPurchaseReviewerUserId());
        apply.setPurchaseResult(normalizeReviewResult(reqVO.getResult()));
        apply.setPurchaseOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setPurchaseHandleTime(LocalDateTime.now());
        finishOnboardingApplyReview(apply, BPM_NODE_PURCHASE_REVIEW, apply.getPurchaseReviewerUserId(),
                "采购部审批", STATUS_GENERAL_MANAGER_REVIEW, BPM_NODE_GENERAL_MANAGER_REVIEW,
                apply.getGeneralManagerUserId(), "采购部审批完成，进入总经理审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generalManagerReviewOnboardingApply(SrmOnboardingApplyActionReqVO.Review reqVO) {
        SrmOnboardingApplyDO apply = validateOnboardingApplyExists(reqVO.getId());
        assertOnboardingApplyStatus(apply, STATUS_GENERAL_MANAGER_REVIEW);
        assertOnboardingApplyCurrentOperator(apply.getGeneralManagerUserId());
        apply.setGeneralManagerResult(normalizeReviewResult(reqVO.getResult()));
        apply.setGeneralManagerOpinion(StrUtil.trim(reqVO.getOpinion()));
        apply.setGeneralManagerHandleTime(LocalDateTime.now());
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        nextAssignees.putAll(nextAssignees(BPM_NODE_PURCHASE_TRANSFER, List.of(apply.getPurchaseHandlerUserId())));
        // 兼容历史部署/运行时缓存仍把总经理审核错误指向“交办办理”的实例，避免提交时被下一节点审批人校验拦截。
        nextAssignees.putAll(nextAssignees(BPM_NODE_ENTRY_PROCESS, List.of(apply.getPurchaseHandlerUserId())));
        approveOnboardingApplyBpmTask(apply, BPM_NODE_GENERAL_MANAGER_REVIEW, apply.getGeneralManagerUserId(),
                "总经理审核", nextAssignees, buildOnboardingApplyGeneralManagerReturnVariables(apply, nextAssignees));
        moveOnboardingApplyEntryTaskBackToPurchaseTransferIfNeeded(apply);
        updateOnboardingApplyStatus(apply, STATUS_PURCHASE_TRANSFER, "总经理审核完成，返回采购部转办",
                Map.of("result", apply.getGeneralManagerResult()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSurvey(SrmSurveySaveReqVO reqVO) {
        String surveyNo = generateSurveyNo();
        validateSurveyNoUnique(null, surveyNo);
        SrmSupplierCandidateService.SupplierResolution supplier = supplierCandidateService.resolveSurveySupplier(
                reqVO.getUnregisteredSupplier(), reqVO.getSupplierId(), reqVO.getSupplierName(),
                reqVO.getConfirmInitializePending(), surveyNo);
        SrmSurveyDO survey = BeanUtils.toBean(reqVO, SrmSurveyDO.class);
        survey.setSurveyNo(surveyNo);
        applySurveySupplier(survey, supplier);
        survey.setStatus(STATUS_DRAFT);
        survey.setApplyTime(LocalDateTime.now());
        fillSurveyInitiatorDefaults(survey, null);
        survey.setVersion(INITIAL_VERSION);
        surveyMapper.insert(survey);
        supplierCandidateService.bindSupplierSourceToSurvey(survey.getSupplierId(), survey.getId());
        replaceSurveyReviews(survey.getId(), reqVO.getReviews());
        return survey.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSurvey(SrmSurveySaveReqVO reqVO) {
        SrmSurveyDO old = validateSurveyExists(reqVO.getId());
        String surveyNo = StrUtil.isBlank(old.getSurveyNo()) ? generateSurveyNo() : old.getSurveyNo();
        SrmSupplierCandidateService.SupplierResolution supplier = supplierCandidateService.resolveSurveySupplier(
                reqVO.getUnregisteredSupplier(), reqVO.getSupplierId(), reqVO.getSupplierName(),
                reqVO.getConfirmInitializePending(), surveyNo);
        SrmSurveyDO updateObj = BeanUtils.toBean(reqVO, SrmSurveyDO.class);
        updateObj.setSurveyNo(surveyNo);
        applySurveySupplier(updateObj, supplier);
        updateObj.setStatus(defaultString(old.getStatus(), STATUS_DRAFT));
        updateObj.setApplyTime(old.getApplyTime());
        fillSurveyInitiatorDefaults(updateObj, old);
        if (surveyMapper.updateById(updateObj) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
        supplierCandidateService.bindSupplierSourceToSurvey(updateObj.getSupplierId(), updateObj.getId());
        if (reqVO.getReviews() != null) {
            replaceSurveyReviews(reqVO.getId(), reqVO.getReviews());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSurvey(Long id) {
        validateSurveyExists(id);
        surveyReviewMapper.deleteBySurveyId(id);
        attachmentMapper.deleteByBiz(BIZ_SURVEY, id);
        surveyMapper.deleteById(id);
    }

    @Override
    public SrmSurveyDO getSurvey(Long id) {
        return surveyMapper.selectById(id);
    }

    @Override
    public PageResult<SrmSurveyDO> getSurveyPage(SrmSurveyPageReqVO reqVO) {
        return surveyMapper.selectPage(reqVO);
    }

    @Override
    public List<SrmSurveyReviewDO> getSurveyReviewList(Long surveyId) {
        return surveyReviewMapper.selectBySurveyId(surveyId);
    }

    @Override
    public Long createDocument(SrmDocumentSaveReqVO reqVO) {
        validateDocumentNoUnique(null, reqVO.getBizType(), reqVO.getDocNo());
        SrmDocumentDO document = BeanUtils.toBean(reqVO, SrmDocumentDO.class);
        fillDocumentDefaults(document, null);
        document.setVersion(INITIAL_VERSION);
        documentMapper.insert(document);
        return document.getId();
    }

    @Override
    public void updateDocument(SrmDocumentSaveReqVO reqVO) {
        SrmDocumentDO old = validateDocumentExists(reqVO.getId());
        validateDocumentNoUnique(reqVO.getId(), reqVO.getBizType(), reqVO.getDocNo());
        SrmDocumentDO updateObj = BeanUtils.toBean(reqVO, SrmDocumentDO.class);
        fillDocumentDefaults(updateObj, old);
        if (documentMapper.updateById(updateObj) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        SrmDocumentDO document = validateDocumentExists(id);
        attachmentMapper.deleteByBiz(document.getBizType(), id);
        documentMapper.deleteById(id);
    }

    @Override
    public SrmDocumentDO getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public PageResult<SrmDocumentDO> getDocumentPage(SrmDocumentPageReqVO reqVO) {
        return documentMapper.selectPage(reqVO);
    }

    private void validateSupplierFileExists(Long id) {
        if (supplierFileMapper.selectById(id) == null) {
            throw exception(SRM_SUPPLIER_FILE_NOT_EXISTS);
        }
    }

    private SrmOnboardingApplyDO validateOnboardingApplyExists(Long id) {
        SrmOnboardingApplyDO apply = onboardingApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(SRM_ONBOARDING_APPLY_NOT_EXISTS);
        }
        return apply;
    }

    private SrmSurveyDO validateSurveyExists(Long id) {
        SrmSurveyDO survey = surveyMapper.selectById(id);
        if (survey == null) {
            throw exception(SRM_SURVEY_NOT_EXISTS);
        }
        return survey;
    }

    private SrmDocumentDO validateDocumentExists(Long id) {
        SrmDocumentDO document = documentMapper.selectById(id);
        if (document == null) {
            throw exception(SRM_DOCUMENT_NOT_EXISTS);
        }
        return document;
    }

    private void validateApplyNoUnique(Long id, String applyNo) {
        if (StrUtil.isBlank(applyNo)) {
            return;
        }
        SrmOnboardingApplyDO apply = onboardingApplyMapper.selectByApplyNo(applyNo);
        if (apply != null && !apply.getId().equals(id)) {
            throw exception(SRM_ONBOARDING_APPLY_APPLY_NO_EXISTS);
        }
    }

    private String generateOnboardingApplyNo() {
        String dailyPrefix = ONBOARDING_APPLY_NO_PREFIX + LocalDate.now().format(
                ONBOARDING_APPLY_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= ONBOARDING_APPLY_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String applyNo = dailyPrefix + String.format(Locale.ROOT, "%03d", sequence);
            if (onboardingApplyMapper.selectByApplyNo(applyNo) == null) {
                return applyNo;
            }
        }
        throw exception(SRM_ONBOARDING_APPLY_APPLY_NO_EXISTS);
    }

    private void copyOnboardingApplyEditableFields(SrmOnboardingApplySaveReqVO reqVO,
            SrmOnboardingApplyDO apply) {
        apply.setSupplierId(reqVO.getSupplierId());
        apply.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        apply.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        apply.setMaterialName(StrUtil.trim(reqVO.getMaterialName()));
        apply.setMaterialCode(StrUtil.trim(reqVO.getMaterialCode()));
        apply.setMaterialModel(StrUtil.trim(reqVO.getMaterialModel()));
        apply.setApplicableProduct(StrUtil.trim(reqVO.getApplicableProduct()));
        apply.setImportType(StrUtil.blankToDefault(StrUtil.trim(reqVO.getImportType()), "NEW"));
        apply.setReplacedMaterialCode(StrUtil.trim(reqVO.getReplacedMaterialCode()));
        apply.setReplacedMaterialName(StrUtil.trim(reqVO.getReplacedMaterialName()));
        apply.setCustomerMaterialCode(StrUtil.trim(reqVO.getCustomerMaterialCode()));
        apply.setCustomerMaterialName(StrUtil.trim(reqVO.getCustomerMaterialName()));
        apply.setApplyReason(StrUtil.trim(reqVO.getApplyReason()));
        apply.setSupplierAdvantageDesc(StrUtil.trim(reqVO.getSupplierAdvantageDesc()));
        apply.setSupplementDesc(StrUtil.trim(reqVO.getSupplementDesc()));
        apply.setMaterialCodeCreated(Boolean.TRUE.equals(reqVO.getMaterialCodeCreated()));
        apply.setMaterialCodeCompleteDate(reqVO.getMaterialCodeCompleteDate());
        apply.setSupplierRosterCreated(Boolean.TRUE.equals(reqVO.getSupplierRosterCreated()));
        apply.setSupplierRosterCompleteDate(reqVO.getSupplierRosterCompleteDate());
        apply.setSpecReq(StrUtil.trim(reqVO.getSpecReq()));
        apply.setNatureRequirement(StrUtil.trim(reqVO.getNatureRequirement()));
        apply.setCertRequirement(StrUtil.trim(reqVO.getCertRequirement()));
        apply.setUseDeptReviewerUserId(reqVO.getUseDeptReviewerUserId());
        apply.setUseDeptReviewerUserName(resolveOnboardingApplyUserName(reqVO.getUseDeptReviewerUserId(),
                reqVO.getUseDeptReviewerUserName()));
        apply.setQualityReviewerUserId(reqVO.getQualityReviewerUserId());
        apply.setQualityReviewerUserName(resolveOnboardingApplyUserName(reqVO.getQualityReviewerUserId(),
                reqVO.getQualityReviewerUserName()));
        apply.setTechReviewerUserId(reqVO.getTechReviewerUserId());
        apply.setTechReviewerUserName(resolveOnboardingApplyUserName(reqVO.getTechReviewerUserId(),
                reqVO.getTechReviewerUserName()));
        apply.setPurchaseReviewerUserId(reqVO.getPurchaseReviewerUserId());
        apply.setPurchaseReviewerUserName(resolveOnboardingApplyUserName(reqVO.getPurchaseReviewerUserId(),
                reqVO.getPurchaseReviewerUserName()));
        apply.setGeneralManagerUserId(reqVO.getGeneralManagerUserId());
        apply.setGeneralManagerUserName(resolveOnboardingApplyUserName(reqVO.getGeneralManagerUserId(),
                reqVO.getGeneralManagerUserName()));
        apply.setApplyDept(StrUtil.trim(reqVO.getApplyDept()));
        apply.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private SrmOnboardingApplyRespVO buildOnboardingApplyResp(SrmOnboardingApplyDO apply, boolean includeLogs) {
        SrmOnboardingApplyRespVO respVO = BeanUtils.toBean(apply, SrmOnboardingApplyRespVO.class);
        Long userId = currentOnboardingApplyUser().id();
        boolean maintainer = isOnboardingApplyMaintainer(apply, userId);
        SrmOnboardingApplySignDO currentSign = onboardingApplySignMapper.selectByApplyIdAndUserId(apply.getId(),
                userId);
        respVO.setCanEdit(maintainer && STATUS_DRAFT.equals(apply.getStatus()));
        respVO.setCanSubmit(maintainer && STATUS_DRAFT.equals(apply.getStatus()));
        respVO.setCanPurchaseIntake(STATUS_PURCHASE_INTAKE.equals(apply.getStatus())
                && findOnboardingApplyRunningTask(apply.getProcessInstanceId(), BPM_NODE_PURCHASE_INTAKE,
                userId) != null);
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
        respVO.setCanArchive(Boolean.TRUE.equals(respVO.getCanPurchaseTransfer())
                && isOnboardingApplyEntryCompleted(apply));
        if (includeLogs) {
            respVO.setSigns(onboardingApplySignMapper.selectListByApplyId(apply.getId()).stream()
                    .map(item -> BeanUtils.toBean(item, SrmOnboardingApplyRespVO.Sign.class)).toList());
            respVO.setLogs(onboardingApplyLogMapper.selectListByApplyId(apply.getId()).stream()
                    .map(this::buildOnboardingApplyLogResp).toList());
        }
        return respVO;
    }

    private SrmOnboardingApplyRespVO.Log buildOnboardingApplyLogResp(SrmOnboardingApplyLogDO log) {
        SrmOnboardingApplyRespVO.Log respVO = BeanUtils.toBean(log, SrmOnboardingApplyRespVO.Log.class);
        respVO.setActionName(onboardingApplyActionText(log.getAction()));
        return respVO;
    }

    private void finishOnboardingApplyReview(SrmOnboardingApplyDO apply, String taskKey, Long assigneeUserId,
            String reason, String nextStatus, String nextTaskKey, Long nextAssigneeUserId, String passDescription) {
        String result = onboardingApplyReviewResultByTaskKey(apply, taskKey);
        if (REVIEW_REJECT.equals(result)) {
            rejectOnboardingApplyBpmTask(apply, taskKey, assigneeUserId, reason + "不通过");
            updateOnboardingApplyStatus(apply, STATUS_REJECTED, reason + "不通过",
                    Map.of("result", result));
            return;
        }
        approveOnboardingApplyBpmTask(apply, taskKey, assigneeUserId, reason,
                nextTaskKey == null ? null : nextAssignees(nextTaskKey, List.of(nextAssigneeUserId)));
        updateOnboardingApplyStatus(apply, nextStatus, passDescription, Map.of("result", result));
    }

    private String onboardingApplyReviewResultByTaskKey(SrmOnboardingApplyDO apply, String taskKey) {
        return switch (taskKey) {
            case BPM_NODE_USE_DEPT_REVIEW -> apply.getUseDeptResult();
            case BPM_NODE_QUALITY_REVIEW -> apply.getQualityResult();
            case BPM_NODE_TECH_REVIEW -> apply.getTechResult();
            case BPM_NODE_PURCHASE_REVIEW -> apply.getPurchaseResult();
            case BPM_NODE_GENERAL_MANAGER_REVIEW -> apply.getGeneralManagerResult();
            default -> REVIEW_PASS;
        };
    }

    private String startOnboardingApplyBpmProcess(SrmOnboardingApplyDO apply) {
        ensureOnboardingApplyBpmModelDeployed();
        Map<String, Object> variables = buildOnboardingApplyBpmVariables(apply);
        variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, new HashMap<String, List<Long>>());
        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY_ONBOARDING_APPLY)
                .setBusinessKey(String.valueOf(apply.getId()))
                .setVariables(variables)
                .setStartUserSelectAssignees(new HashMap<>());
        try {
            return bpmProcessInstanceApi.createProcessInstance(apply.getApplicantId(), createReq);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployOnboardingApplyBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(apply.getApplicantId(), createReq);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(SRM_ONBOARDING_APPLY_BPM_NOT_PUBLISHED);
                }
                throw retryEx;
            }
        }
    }

    private void ensureOnboardingApplyBpmModelDeployed() {
        Model model = bpmModelService.getModel(BPM_MODEL_ID_ONBOARDING_APPLY);
        if (model != null && StrUtil.isNotBlank(model.getDeploymentId())) {
            return;
        }
        deployOnboardingApplyBpmModel();
    }

    private void deployOnboardingApplyBpmModel() {
        try {
            bpmModelService.deployModel(resolveOnboardingApplyBpmDeployUserId(), BPM_MODEL_ID_ONBOARDING_APPLY);
        } catch (ServiceException ex) {
            throw exception(SRM_ONBOARDING_APPLY_BPM_NOT_PUBLISHED);
        }
    }

    private Long resolveOnboardingApplyBpmDeployUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Model model = bpmModelService.getModel(BPM_MODEL_ID_ONBOARDING_APPLY);
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

    private void approveOnboardingApplyBpmTask(SrmOnboardingApplyDO apply, String taskKey, Long assigneeUserId,
            String reason, Map<String, List<Long>> nextAssignees) {
        approveOnboardingApplyBpmTask(apply, taskKey, assigneeUserId, reason, nextAssignees, Map.of());
    }

    private void approveOnboardingApplyBpmTask(SrmOnboardingApplyDO apply, String taskKey, Long assigneeUserId,
            String reason, Map<String, List<Long>> nextAssignees, Map<String, Object> extraVariables) {
        Task task = findOnboardingApplyRunningTask(apply.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_ONBOARDING_APPLY_BPM_TASK_MISSING);
        }
        Map<String, Object> variables = buildOnboardingApplyBpmVariables(apply);
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

    private Map<String, Object> buildOnboardingApplyGeneralManagerReturnVariables(SrmOnboardingApplyDO apply,
            Map<String, List<Long>> nextAssignees) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(BPM_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, nextAssignees);
        variables.put(BPM_VARIABLE_COLL_USER_LIST, List.of(apply.getPurchaseHandlerUserId()));
        return variables;
    }

    private void moveOnboardingApplyEntryTaskBackToPurchaseTransferIfNeeded(SrmOnboardingApplyDO apply) {
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

    private void rejectOnboardingApplyBpmTask(SrmOnboardingApplyDO apply, String taskKey, Long assigneeUserId,
            String reason) {
        Task task = findOnboardingApplyRunningTask(apply.getProcessInstanceId(), taskKey, assigneeUserId);
        if (task == null) {
            throw exception(SRM_ONBOARDING_APPLY_BPM_TASK_MISSING);
        }
        BpmTaskRejectReqVO rejectReq = new BpmTaskRejectReqVO()
                .setId(task.getId())
                .setReason(reason);
        bpmTaskService.rejectTask(assigneeUserId, rejectReq);
    }

    private Task findOnboardingApplyRunningTask(String processInstanceId, String taskKey, Long assigneeUserId) {
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

    private boolean hasRunningOnboardingApplyTask(String processInstanceId, String taskKey) {
        if (StrUtil.isBlank(processInstanceId)) {
            return false;
        }
        return CollUtil.isNotEmpty(
                bpmTaskService.getRunningTaskListByProcessInstanceId(processInstanceId, true, taskKey));
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

    private Map<String, Object> buildOnboardingApplyBpmVariables(SrmOnboardingApplyDO apply) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("onboardingApplyId", apply.getId());
        variables.put("businessKey", apply.getId());
        variables.put("applyNo", apply.getApplyNo());
        variables.put("supplierName", apply.getSupplierName());
        variables.put("materialName", apply.getMaterialName());
        variables.put("materialModel", apply.getMaterialModel());
        variables.put("applicableProduct", apply.getApplicableProduct());
        variables.put("importType", apply.getImportType());
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
        List<Long> signUserIds = onboardingApplySignMapper.selectListByApplyId(apply.getId()).stream()
                .map(SrmOnboardingApplySignDO::getUserId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isNotEmpty(signUserIds)) {
            variables.put("signUserIds", signUserIds);
            variables.put(BPM_VARIABLE_COLL_USER_LIST, signUserIds);
        }
        return variables;
    }

    private void updateOnboardingApplyStatus(SrmOnboardingApplyDO apply, String toStatus, String description,
            Object detail) {
        String fromStatus = apply.getStatus();
        apply.setStatus(toStatus);
        apply.setCurrentNodeName(onboardingApplyStatusText(toStatus));
        updateOnboardingApplyByIdChecked(apply);
        writeOnboardingApplyLog(apply.getId(), onboardingApplyActionByStatus(toStatus), fromStatus, toStatus,
                description, detail);
    }

    private void updateOnboardingApplyByIdChecked(SrmOnboardingApplyDO apply) {
        if (onboardingApplyMapper.updateById(apply) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
    }

    private void validateOnboardingApplyReviewers(SrmOnboardingApplyDO apply) {
        if (apply.getUseDeptReviewerUserId() == null || apply.getQualityReviewerUserId() == null
                || apply.getTechReviewerUserId() == null || apply.getPurchaseReviewerUserId() == null
                || apply.getGeneralManagerUserId() == null) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED);
        }
        adminUserApi.validateUserList(Set.of(apply.getUseDeptReviewerUserId(), apply.getQualityReviewerUserId(),
                apply.getTechReviewerUserId(), apply.getPurchaseReviewerUserId(), apply.getGeneralManagerUserId()));
    }

    private List<SrmOnboardingApplySignDO> buildOnboardingApplySignRows(Long applyId,
            List<SrmOnboardingApplyActionReqVO.SignUser> signUsers) {
        if (CollUtil.isEmpty(signUsers)) {
            return List.of();
        }
        Map<Long, SrmOnboardingApplySignDO> unique = new LinkedHashMap<>();
        for (SrmOnboardingApplyActionReqVO.SignUser signUser : signUsers) {
            if (signUser.getUserId() == null || StrUtil.isBlank(signUser.getDeptName())) {
                continue;
            }
            UserSnapshot user = resolveOnboardingApplyUser(signUser.getUserId(), signUser.getUserName());
            String deptCode = StrUtil.trim(signUser.getDeptCode());
            String deptName = StrUtil.trim(signUser.getDeptName());
            unique.compute(user.id(), (ignored, existing) -> {
                if (existing == null) {
                    SrmOnboardingApplySignDO sign = new SrmOnboardingApplySignDO();
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

    private List<Long> resolveOnboardingApplyPermissionUserIds(String permission) {
        Set<Long> permissionUserIds = permissionApi.getUserIdListByPermissions(List.of(permission));
        if (CollUtil.isEmpty(permissionUserIds)) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED);
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserList(permissionUserIds);
        if (CollUtil.isEmpty(users)) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED);
        }
        List<Long> userIds = users.stream().map(AdminUserRespDTO::getId).filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(userIds)) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED);
        }
        return userIds;
    }

    private UserSnapshot resolveFirstOnboardingApplyPermissionUser(String permission) {
        List<Long> userIds = resolveOnboardingApplyPermissionUserIds(permission);
        return resolveOnboardingApplyUser(userIds.get(0), null);
    }

    private UserSnapshot resolveOnboardingApplyUser(Long userId, String fallbackName) {
        if (userId == null) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEWER_REQUIRED);
        }
        return new UserSnapshot(userId, StrUtil.blankToDefault(user.getNickname(),
                StrUtil.blankToDefault(user.getUsername(), StrUtil.trim(fallbackName))));
    }

    private boolean isOnboardingApplyEntryCompleted(SrmOnboardingApplyDO apply) {
        return Boolean.TRUE.equals(apply.getMaterialCodeCreated())
                && Boolean.TRUE.equals(apply.getSupplierRosterCreated())
                && StrUtil.isNotBlank(apply.getMaterialEntryCode())
                && StrUtil.isNotBlank(apply.getSupplierRosterEntryCode());
    }

    private String normalizeReviewResult(String result) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(result), REVIEW_PASS);
        if (!REVIEW_PASS.equals(normalized) && !REVIEW_REJECT.equals(normalized)) {
            throw exception(SRM_ONBOARDING_APPLY_REVIEW_RESULT_INVALID);
        }
        return normalized;
    }

    private void assertOnboardingApplyStatus(SrmOnboardingApplyDO apply, String expectedStatus) {
        if (!expectedStatus.equals(apply.getStatus())) {
            throw exception(SRM_ONBOARDING_APPLY_STATUS_INVALID);
        }
    }

    private void assertOnboardingApplyMaintainer(SrmOnboardingApplyDO apply) {
        Long userId = currentOnboardingApplyUser().id();
        if (!isOnboardingApplyMaintainer(apply, userId)) {
            throw exception(SRM_ONBOARDING_APPLY_OPERATOR_ONLY);
        }
    }

    private boolean isOnboardingApplyMaintainer(SrmOnboardingApplyDO apply, Long userId) {
        return Objects.equals(apply.getApplicantId(), userId) || isOnboardingApplyAdmin(userId);
    }

    private void assertOnboardingApplyCurrentOperator(Long expectedUserId) {
        Long userId = currentOnboardingApplyUser().id();
        if (!Objects.equals(expectedUserId, userId)) {
            throw exception(SRM_ONBOARDING_APPLY_OPERATOR_ONLY);
        }
    }

    private boolean isOnboardingApplyAdmin(Long userId) {
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

    private String resolveOnboardingApplyUserName(Long userId, String fallbackName) {
        if (userId == null) {
            return StrUtil.trim(fallbackName);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            return StrUtil.trim(fallbackName);
        }
        return StrUtil.blankToDefault(user.getNickname(), StrUtil.blankToDefault(user.getUsername(), fallbackName));
    }

    private UserSnapshot currentOnboardingApplyUser() {
        Initiator initiator = getCurrentInitiator();
        return new UserSnapshot(initiator.userId(), defaultString(initiator.userName(), "系统用户"));
    }

    private void writeOnboardingApplyLog(Long applyId, String action, String fromStatus, String toStatus,
            String description, Object detail) {
        UserSnapshot user = currentOnboardingApplyUser();
        SrmOnboardingApplyLogDO log = new SrmOnboardingApplyLogDO();
        log.setApplyId(applyId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setActionDescription(description);
        log.setDetailJson(detail == null ? null : JsonUtils.toJsonString(detail));
        onboardingApplyLogMapper.insert(log);
    }

    private String onboardingApplyStatusText(String status) {
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

    private String onboardingApplyActionByStatus(String status) {
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

    private String onboardingApplyActionText(String action) {
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

    private void validateSurveyNoUnique(Long id, String surveyNo) {
        if (StrUtil.isBlank(surveyNo)) {
            return;
        }
        SrmSurveyDO survey = surveyMapper.selectBySurveyNo(surveyNo);
        if (survey != null && !survey.getId().equals(id)) {
            throw exception(SRM_SURVEY_NO_EXISTS);
        }
    }

    private String generateSurveyNo() {
        String dailyPrefix = SURVEY_NO_PREFIX + LocalDate.now().format(SURVEY_NO_DATE_FORMATTER);
        for (int sequence = 1; sequence <= SURVEY_NO_DAILY_SEQUENCE_LIMIT; sequence++) {
            String surveyNo = dailyPrefix + String.format(Locale.ROOT, "%03d", sequence);
            if (surveyMapper.selectBySurveyNo(surveyNo) == null) {
                return surveyNo;
            }
        }
        throw exception(SRM_SURVEY_NO_EXISTS);
    }

    private void validateDocumentNoUnique(Long id, String bizType, String docNo) {
        SrmDocumentDO document = documentMapper.selectByBizTypeAndDocNo(bizType, docNo);
        if (document != null && !document.getId().equals(id)) {
            throw exception(SRM_DOCUMENT_NO_EXISTS);
        }
    }

    private void applySurveySupplier(SrmSurveyDO survey,
            SrmSupplierCandidateService.SupplierResolution supplier) {
        survey.setSupplierId(supplier.supplierId());
        survey.setSupplierCode(supplier.supplierCode());
        survey.setSupplierName(supplier.supplierName());
        survey.setSupplierSourceType(supplier.sourceType());
        survey.setUnregisteredSupplier(supplier.unregisteredSupplier());
    }

    private void replaceSurveyReviews(Long surveyId, List<SrmSurveySaveReqVO.Review> reviews) {
        surveyReviewMapper.deleteBySurveyId(surveyId);
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        List<SrmSurveyReviewDO> reviewList = BeanUtils.toBean(reviews, SrmSurveyReviewDO.class);
        for (int index = 0; index < reviewList.size(); index++) {
            SrmSurveyReviewDO review = reviewList.get(index);
            review.setId(null);
            review.setSurveyId(surveyId);
            review.setSort(review.getSort() == null ? index + 1 : review.getSort());
            surveyReviewMapper.insert(review);
        }
    }

    private void fillSupplierFileDefaults(SrmSupplierFileDO supplierFile) {
        applySupplierFilePayloadFallback(supplierFile);
        normalizeSupplierFileFields(supplierFile);
        applySupplierFileExpiryRule(supplierFile);
        supplierFile.setWarningDays(defaultInteger(supplierFile.getWarningDays(), DEFAULT_WARNING_DAYS));
        supplierFile.setFileStatus(resolveSupplierFileStatus(supplierFile));
    }

    private void applySupplierFilePayloadFallback(SrmSupplierFileDO supplierFile) {
        JSONObject payload = parseSupplierFilePayload(supplierFile.getPayloadJson());
        supplierFile.setProvidedProduct(firstNonBlank(
                supplierFile.getProvidedProduct(),
                firstPayloadText(payload, "providedProduct", "provided_product", "供应产品", "供应商产品")));
        supplierFile.setProductModel(firstNonBlank(
                supplierFile.getProductModel(),
                firstPayloadText(payload, "productModel", "product_model", "产品型号")));
        supplierFile.setInspectionAgency(firstNonBlank(
                supplierFile.getInspectionAgency(),
                firstPayloadText(payload, "inspectionAgency", "inspection_agency", "检测机构")));
        supplierFile.setReportCode(firstNonBlank(
                supplierFile.getReportCode(),
                firstPayloadText(payload, "reportCode", "report_code", "报告编码", "报告编号")));
        supplierFile.setStandardCompliant(firstNonBlank(
                supplierFile.getStandardCompliant(),
                firstPayloadText(payload, "standardCompliant", "standard_compliant", "是否符合标准", "是否符合")));
    }

    private JSONObject parseSupplierFilePayload(String payloadJson) {
        if (StrUtil.isBlank(payloadJson)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(payloadJson);
        } catch (RuntimeException ignored) {
            return new JSONObject();
        }
    }

    private static String firstNonBlank(String current, String fallback) {
        if (StrUtil.isNotBlank(current)) {
            return current;
        }
        String text = StrUtil.trim(fallback);
        return StrUtil.isBlank(text) ? null : text;
    }

    private static String firstPayloadText(JSONObject payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = StrUtil.trim(payload.getStr(key));
            if (StrUtil.isNotBlank(value) && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private void normalizeSupplierFileFields(SrmSupplierFileDO supplierFile) {
        supplierFile.setSupplierCode(StrUtil.trim(supplierFile.getSupplierCode()));
        supplierFile.setSupplierName(StrUtil.trim(supplierFile.getSupplierName()));
        supplierFile.setFileType(StrUtil.trim(supplierFile.getFileType()));
        supplierFile.setFileName(StrUtil.trim(supplierFile.getFileName()));
        supplierFile.setProvidedProduct(StrUtil.trim(supplierFile.getProvidedProduct()));
        supplierFile.setProductModel(StrUtil.trim(supplierFile.getProductModel()));
        supplierFile.setInspectionAgency(StrUtil.trim(supplierFile.getInspectionAgency()));
        supplierFile.setReportCode(StrUtil.trim(supplierFile.getReportCode()));
        supplierFile.setStandardCompliant(normalizeStandardCompliant(supplierFile.getStandardCompliant()));
        supplierFile.setExpiryRule(StrUtil.trim(supplierFile.getExpiryRule()));
        if (FILE_TYPE_COMPLIANCE_AGREEMENT.equals(supplierFile.getFileType())) {
            supplierFile.setProductModel(null);
            supplierFile.setInspectionAgency(null);
            supplierFile.setReportCode(null);
            supplierFile.setStandardCompliant(null);
        } else if (FILE_TYPE_SYSTEM_CERT.equals(supplierFile.getFileType())) {
            supplierFile.setProductModel(null);
            supplierFile.setInspectionAgency(null);
            supplierFile.setStandardCompliant(null);
        }
    }

    private static String normalizeStandardCompliant(String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String upperText = text.toUpperCase(Locale.ROOT);
        if (Set.of("Y", "YES", "TRUE", "1").contains(upperText)
                || Set.of("是", "符合", "合格").contains(text)) {
            return "Y";
        }
        if (Set.of("N", "NO", "FALSE", "0").contains(upperText)
                || Set.of("否", "不符合", "不合格").contains(text)) {
            return "N";
        }
        return text;
    }

    private void applySupplierFileExpiryRule(SrmSupplierFileDO supplierFile) {
        SupplierFileExpiryRule expiryRule = resolveSupplierFileExpiryRule(supplierFile);
        supplierFile.setExpiryRule(expiryRule.code());
        supplierFile.setValidityMonths(expiryRule.months());
        if (expiryRule.months() == null) {
            return;
        }
        LocalDate effectDate = supplierFile.getEffectDate();
        supplierFile.setExpiryDate(effectDate == null
                ? null
                : effectDate.plusMonths(expiryRule.months()).minusDays(1));
    }

    private SupplierFileExpiryRule resolveSupplierFileExpiryRule(SrmSupplierFileDO supplierFile) {
        String fileType = supplierFile.getFileType();
        String fileName = supplierFile.getFileName();
        if (FILE_TYPE_COMPLIANCE_AGREEMENT.equals(fileType)) {
            return new SupplierFileExpiryRule(EXPIRY_RULE_AUTO_12M_MINUS_1D, 12);
        }
        if (FILE_TYPE_ENVIRONMENT_CERT.equals(fileType)) {
            if (ENVIRONMENT_FILE_NAMES_12_MONTH.contains(fileName)) {
                return new SupplierFileExpiryRule(EXPIRY_RULE_AUTO_12M_MINUS_1D, 12);
            }
            if (ENVIRONMENT_FILE_NAMES_60_MONTH.contains(fileName)) {
                return new SupplierFileExpiryRule(EXPIRY_RULE_AUTO_60M_MINUS_1D, 60);
            }
        }
        return new SupplierFileExpiryRule(EXPIRY_RULE_MANUAL, null);
    }

    private record SupplierFileExpiryRule(String code, Integer months) {
    }

    private void fillDocumentDefaults(SrmDocumentDO document, SrmDocumentDO old) {
        document.setStatus(old == null ? STATUS_DRAFT : defaultString(old.getStatus(), STATUS_DRAFT));
        document.setApplyTime(defaultLocalDateTime(document.getApplyTime()));
        fillDocumentInitiatorDefaults(document, old);
    }

    private void fillOnboardingApplyInitiatorDefaults(SrmOnboardingApplyDO apply, SrmOnboardingApplyDO old) {
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

    private void fillSurveyInitiatorDefaults(SrmSurveyDO survey, SrmSurveyDO old) {
        Initiator initiator = getCurrentInitiator();
        survey.setApplicantId(old == null
                ? defaultLong(initiator.userId(), survey.getApplicantId())
                : defaultLong(old.getApplicantId(), defaultLong(initiator.userId(), survey.getApplicantId())));
        survey.setApplicantName(old == null
                ? defaultString(initiator.userName(), survey.getApplicantName())
                : defaultString(old.getApplicantName(), defaultString(initiator.userName(), survey.getApplicantName())));
    }

    private void fillDocumentInitiatorDefaults(SrmDocumentDO document, SrmDocumentDO old) {
        Initiator initiator = getCurrentInitiator();
        document.setApplicantId(old == null
                ? defaultLong(initiator.userId(), document.getApplicantId())
                : defaultLong(old.getApplicantId(), defaultLong(initiator.userId(), document.getApplicantId())));
        document.setApplicantName(old == null
                ? defaultString(initiator.userName(), document.getApplicantName())
                : defaultString(old.getApplicantName(), defaultString(initiator.userName(), document.getApplicantName())));
        document.setApplyDept(old == null
                ? defaultString(initiator.deptName(), document.getApplyDept())
                : defaultString(old.getApplyDept(), defaultString(initiator.deptName(), document.getApplyDept())));
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

    private record Initiator(Long userId, String userName, String deptName) {
    }

    private record UserSnapshot(Long id, String name) {
    }

    private String resolveSupplierFileStatus(SrmSupplierFileDO supplierFile) {
        if (supplierFile.getExpiryDate() == null) {
            return FILE_STATUS_VALID;
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), supplierFile.getExpiryDate());
        if (daysLeft < 0) {
            return FILE_STATUS_EXPIRED;
        }
        if (daysLeft <= supplierFile.getWarningDays()) {
            return FILE_STATUS_WARNING;
        }
        return FILE_STATUS_VALID;
    }

    private void syncPrimaryAttachment(String bizType, Long bizId, String fileName, String fileUrl, String fileType) {
        if (StrUtil.isBlank(fileName) || StrUtil.isBlank(fileUrl)) {
            return;
        }
        attachmentMapper.deleteByBiz(bizType, bizId);
        SrmAttachmentDO attachment = new SrmAttachmentDO();
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setFileName(fileName);
        attachment.setFileUrl(fileUrl);
        attachment.setFileType(normalizeAttachmentFileType(fileName, fileType));
        initializeAttachmentVersion(attachment, ATTACHMENT_CATEGORY_OTHER);
        attachmentMapper.insert(attachment);
    }

    private void initializeAttachmentVersion(SrmAttachmentDO attachment, String attachmentCategory) {
        LocalDateTime now = LocalDateTime.now();
        attachment.setAttachmentCategory(defaultString(attachmentCategory, ATTACHMENT_CATEGORY_OTHER));
        attachment.setVersionGroupNo(buildAttachmentVersionGroupNo());
        attachment.setVersionNo(INITIAL_ATTACHMENT_VERSION);
        attachment.setPreviousAttachmentId(null);
        attachment.setLatestVersion(true);
        attachment.setUploadTime(now);
        attachment.setVersionTime(now);
        attachment.setUpdateDescription(ATTACHMENT_FIRST_UPLOAD_DESCRIPTION);
        fillAttachmentUploaderDefaults(attachment);
    }

    private String ensureAttachmentVersionGroup(SrmAttachmentDO attachment) {
        if (StrUtil.isNotBlank(attachment.getVersionGroupNo())) {
            return attachment.getVersionGroupNo();
        }
        String versionGroupNo = buildAttachmentVersionGroupNo();
        SrmAttachmentDO updateObj = new SrmAttachmentDO();
        updateObj.setId(attachment.getId());
        updateObj.setVersionGroupNo(versionGroupNo);
        updateObj.setVersionNo(defaultInteger(attachment.getVersionNo(), INITIAL_ATTACHMENT_VERSION));
        updateObj.setVersionTime(defaultLocalDateTime(attachment.getVersionTime()));
        updateObj.setUpdateDescription(defaultString(
                attachment.getUpdateDescription(), ATTACHMENT_FIRST_UPLOAD_DESCRIPTION));
        attachmentMapper.updateById(updateObj);
        attachment.setVersionGroupNo(versionGroupNo);
        return versionGroupNo;
    }

    private String buildAttachmentVersionGroupNo() {
        return "ATT-" + UUID.randomUUID().toString().replace("-", "")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeAttachmentFileType(String fileName, String fileType) {
        String normalized = fileType;
        if (StrUtil.isNotBlank(fileName)) {
            int extensionIndex = fileName.lastIndexOf('.');
            if (extensionIndex >= 0 && extensionIndex < fileName.length() - 1) {
                normalized = fileName.substring(extensionIndex + 1);
            }
        }
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        normalized = StrUtil.trim(normalized).toLowerCase(Locale.ROOT);
        return normalized.substring(0, Math.min(normalized.length(), ATTACHMENT_FILE_TYPE_MAX_LENGTH));
    }

    private void fillAttachmentUploaderDefaults(SrmAttachmentDO attachment) {
        Initiator initiator = getCurrentInitiator();
        if (attachment.getUploadUserId() == null) {
            attachment.setUploadUserId(initiator.userId());
        }
        if (StrUtil.isBlank(attachment.getUploadUserName())) {
            attachment.setUploadUserName(initiator.userName());
        }
    }

    private static String defaultString(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    private static Integer defaultInteger(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static Long defaultLong(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static LocalDateTime defaultLocalDateTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }

}
