package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsException8dTeamMemberReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventGenerate8dReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventLinkNcrReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskBatchSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberConfirmRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionRelationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionTeamMemberReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRelationReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dFlowLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dTeamMemberDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionFlowLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskConfirmLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskMemberDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskReplyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionTeamMemberDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRelationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dFlowLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.Qms8dTeamMemberMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionFlowLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionGroupTaskConfirmLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionGroupTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionGroupTaskMemberMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionGroupTaskReplyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionTeamMemberMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRelationMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsExceptionEventServiceImpl implements QmsExceptionEventService {

    private static final ErrorCode QMS_EXCEPTION_EVENT_NOT_EXISTS = new ErrorCode(1008100130, "异常事件不存在");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CLOSED = new ErrorCode(1008100131, "异常事件已关闭或已取消，不能继续流转");
    private static final ErrorCode QMS_EXCEPTION_EVENT_RETURN_OPINION_REQUIRED = new ErrorCode(1008100132, "退回意见不能为空");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CLOSE_NCR_REQUIRED = new ErrorCode(1008100133, "产品质量相关异常关闭前必须关联 NCR");
    private static final ErrorCode QMS_EXCEPTION_EVENT_NCR_NOT_EXISTS = new ErrorCode(1008100134, "关联的 NCR 不存在");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CLOSE_EFFECT_REQUIRED = new ErrorCode(1008100135, "关闭异常必须填写效果确认");
    private static final ErrorCode QMS_EXCEPTION_EVENT_BPM_NOT_PUBLISHED =
            new ErrorCode(1008100136, "异常事件提报流程未发布，请先在 SIMPLE 设计器发布流程 qms_exception_event");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_NOT_EXISTS =
            new ErrorCode(1008100137, "异常事件临时小组任务不存在");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION =
            new ErrorCode(1008100138, "当前用户无权处理该临时小组任务");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID =
            new ErrorCode(1008100139, "当前临时小组任务状态不允许该操作");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_ASSIGNEE_REQUIRED =
            new ErrorCode(1008100140, "临时小组任务必须至少指定小组成员或根因分析人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_RETURN_OPINION_REQUIRED =
            new ErrorCode(1008100141, "退回临时小组任务必须填写原因");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_ASSIGNMENT_REQUIRED =
            new ErrorCode(1008100142, "调查小组任务必须选择临时小组部门，并为每个部门指定一个办理人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_ROOT_TASK_OWNER_REQUIRED =
            new ErrorCode(1008100143, "根因分析与纠正预防任务必须指定一个执行人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED =
            new ErrorCode(1008100144, "任务提交内容不能为空");
    private static final ErrorCode QMS_EXCEPTION_EVENT_NCR_LINK_LOCKED =
            new ErrorCode(1008100145, "关联 NCR 只能在异常事件新建时选择，保存后不允许修改");
    private static final ErrorCode QMS_EXCEPTION_EVENT_MISREPORT_STATUS_INVALID =
            new ErrorCode(1008100146, "仅确认步骤可选择误报并直接关闭异常事件");
    private static final ErrorCode QMS_EXCEPTION_EVENT_NCR_ALREADY_LINKED =
            new ErrorCode(1008100148, "该异常处置单已挂接异常事件，不能重复关联");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_DELEGATE_REQUIRED =
            new ErrorCode(1008100149, "请选择被委托代办人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_GROUP_TASK_DELEGATE_SELF_INVALID =
            new ErrorCode(1008100150, "被委托代办人不能是本人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CONFIRM_TYPE_REQUIRED =
            new ErrorCode(1008100301, "请填写确认步骤中的异常类别");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CONFIRM_LEVEL_REQUIRED =
            new ErrorCode(1008100302, "请填写确认步骤中的异常等级");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CONFIRM_RELATED_PRODUCT_REQUIRED =
            new ErrorCode(1008100303, "请填写确认步骤中的关联产品");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CONTAINMENT_OWNER_REQUIRED =
            new ErrorCode(1008100304, "请指定临时小组负责人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CONTAINMENT_MEMBER_REQUIRED =
            new ErrorCode(1008100305, "请指定临时小组会签成员");
    private static final ErrorCode QMS_EXCEPTION_EVENT_RESPONSIBILITY_OWNER_REQUIRED =
            new ErrorCode(1008100306, "请指定责任部门负责人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_ROOT_CAUSE_MEMBER_REQUIRED =
            new ErrorCode(1008100307, "请选择根因分析纠正预防会签人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_RESULT_UPLOADER_REQUIRED =
            new ErrorCode(1008100308, "请指定执行结果上传人");
    private static final ErrorCode QMS_EXCEPTION_EVENT_RESULT_REQUIRED =
            new ErrorCode(1008100309, "请填写纠正预防措施执行成果");
    private static final ErrorCode QMS_EXCEPTION_EVENT_CONFIRM_DEADLINE_REQUIRED =
            new ErrorCode(1008100310, "请填写确认步骤中的处理期限");
    private static final ErrorCode QMS_EXCEPTION_EVENT_WITHDRAW_NOT_AVAILABLE =
            new ErrorCode(1008100311, "当前节点不可撤回：仅允许上一办理人在下一节点未办理前撤回");
    private static final ErrorCode QMS_EXCEPTION_EVENT_WITHDRAW_TARGET_INVALID =
            new ErrorCode(1008100312, "撤回目标节点无效，请刷新后重试");

    private static final String STATUS_CONFIRMING = "CONFIRMING";
    private static final String STATUS_CONTAINMENT = "CONTAINMENT";
    private static final String STATUS_CONTAINMENT_SIGN = "CONTAINMENT_SIGN";
    private static final String STATUS_RESPONSIBILITY = "RESPONSIBILITY_CONFIRM";
    private static final String STATUS_ROOT_CAUSE = "ROOT_CAUSE";
    private static final String STATUS_ROOT_CAUSE_SIGN = "ROOT_CAUSE_SIGN";
    private static final String STATUS_RESULT_UPLOADER_ASSIGN = "RESULT_UPLOADER_ASSIGN";
    private static final String STATUS_RESULT_UPLOAD = "RESULT_UPLOAD";
    private static final String STATUS_QA_CLOSURE = "QA_CLOSURE";
    private static final String STATUS_VERIFYING = "VERIFYING";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String NODE_CONFIRMING = "QUALITY_CONFIRM";
    private static final String NODE_CONTAINMENT = "CONTAINMENT";
    private static final String NODE_CONTAINMENT_SIGN = "CONTAINMENT_SIGN";
    private static final String NODE_RESPONSIBILITY = "RESPONSIBILITY_CONFIRM";
    private static final String NODE_ROOT_CAUSE = "ROOT_CAUSE";
    private static final String NODE_ROOT_CAUSE_SIGN = "ROOT_CAUSE_SIGN";
    private static final String NODE_RESULT_UPLOADER_ASSIGN = "RESULT_UPLOADER_ASSIGN";
    private static final String NODE_RESULT_UPLOAD = "RESULT_UPLOAD";
    private static final String NODE_QA_CLOSURE = "QA_CLOSURE";
    private static final String NODE_VERIFYING = "VERIFYING";
    private static final String NODE_RETURNED = "RETURNED";
    private static final String NODE_CLOSED = "CLOSED";

    private static final String GROUP_TASK_STATUS_DISPATCHED = "DISPATCHED";
    private static final String GROUP_TASK_STATUS_SUBMITTED = "SUBMITTED";
    private static final String GROUP_TASK_STATUS_SIGNED = "SIGNED";
    private static final String GROUP_TASK_STATUS_ACCEPTED = "ACCEPTED";
    private static final String GROUP_TASK_STATUS_RETURNED = "RETURNED";
    private static final String GROUP_TASK_ACTION_ACCEPT = "ACCEPT";
    private static final String GROUP_TASK_ACTION_RETURN = "RETURN";
    private static final String GROUP_TASK_TYPE_INVESTIGATION = "INVESTIGATION_GROUP";
    private static final String GROUP_TASK_TYPE_ROOT_CAUSE = "ROOT_CAUSE_PREVENTIVE";
    private static final String GROUP_MEMBER_CONFIRM_PENDING = "PENDING";
    private static final String GROUP_MEMBER_CONFIRM_SUBMITTED = "SUBMITTED";
    private static final String GROUP_MEMBER_CONFIRM_CONFIRMED = "CONFIRMED";
    private static final String GROUP_MEMBER_CONFIRM_DISAGREED = "DISAGREED";
    private static final String GROUP_MEMBER_CONFIRM_EXPIRED = "EXPIRED";
    private static final String GROUP_MEMBER_ACTION_CONFIRM = "CONFIRM";
    private static final String GROUP_MEMBER_ACTION_DISAGREE = "DISAGREE";
    private static final String ACTION_MISREPORT_CLOSE = "MISREPORT_CLOSE";
    private static final String RELATION_TYPE_NCR = "NCR";
    private static final String RELATION_TYPE_RAW_MATERIAL_NCR = "RAW_MATERIAL_NCR";
    private static final String RELATION_TYPE_EXCEPTION = "EXCEPTION";

    private static final String BPM_EXCEPTION_PROCESS_KEY = "qms_exception_event";
    private static final String BPM_EXCEPTION_MODEL_ID = "qms-exception-event-model";
    private static final String BPM_NODE_START_USER = "StartUserNode";
    private static final String BPM_NODE_QUALITY_CONFIRM = "quality_confirm";
    private static final String BPM_NODE_CONTAINMENT_OWNER_HANDLE = "containment_owner_handle";
    private static final String BPM_NODE_CONTAINMENT_OWNER_HANDLE_LEGACY = "containment";
    private static final String BPM_NODE_CONTAINMENT_MEMBER_SIGN = "containment_member_sign";
    private static final String BPM_NODE_RESPONSIBILITY_CONFIRM = "responsibility_confirm";
    private static final String BPM_NODE_ROOT_CAUSE_OWNER_HANDLE = "root_cause_owner_handle";
    private static final String BPM_NODE_ROOT_CAUSE_OWNER_HANDLE_LEGACY = "root_cause";
    private static final String BPM_NODE_ROOT_CAUSE_MEMBER_SIGN = "root_cause_member_sign";
    private static final String BPM_NODE_RESULT_UPLOADER_ASSIGN = "result_uploader_assign";
    private static final String BPM_NODE_RESULT_UPLOAD = "result_upload";
    private static final String BPM_NODE_QA_CLOSURE = "qa_closure";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1009003002;
    private static final Long BPM_EXCEPTION_BUILT_IN_MANAGER_USER_ID = 144L;
    private static final String ATTACHMENT_DESCRIPTION = "ATTACHMENT_DESCRIPTION";
    private static final String ATTACHMENT_CONFIRM_RELATED = "ATTACHMENT_CONFIRM_RELATED";
    private static final String ATTACHMENT_CONTAINMENT = "ATTACHMENT_CONTAINMENT";
    private static final String ATTACHMENT_ROOT_CAUSE = "ATTACHMENT_ROOT_CAUSE";
    private static final String ATTACHMENT_RESULT = "ATTACHMENT_RESULT";
    private static final String ATTACHMENT_EFFECT = "ATTACHMENT_EFFECT";
    private static final Set<String> ATTACHMENT_RELATION_TYPES = Set.of(
            ATTACHMENT_DESCRIPTION, ATTACHMENT_CONFIRM_RELATED, ATTACHMENT_CONTAINMENT,
            ATTACHMENT_ROOT_CAUSE, ATTACHMENT_RESULT, ATTACHMENT_EFFECT);

    @Resource
    private QmsExceptionEventMapper qmsExceptionEventMapper;
    @Resource
    private QmsExceptionTeamMemberMapper qmsExceptionTeamMemberMapper;
    @Resource
    private QmsExceptionRelationMapper qmsExceptionRelationMapper;
    @Resource
    private QmsExceptionFlowLogMapper qmsExceptionFlowLogMapper;
    @Resource
    private QmsExceptionGroupTaskMapper qmsExceptionGroupTaskMapper;
    @Resource
    private QmsExceptionGroupTaskMemberMapper qmsExceptionGroupTaskMemberMapper;
    @Resource
    private QmsExceptionGroupTaskReplyMapper qmsExceptionGroupTaskReplyMapper;
    @Resource
    private QmsExceptionGroupTaskConfirmLogMapper qmsExceptionGroupTaskConfirmLogMapper;
    @Resource
    private Qms8dReportMapper qms8dReportMapper;
    @Resource
    private Qms8dTeamMemberMapper qms8dTeamMemberMapper;
    @Resource
    private Qms8dRelationMapper qms8dRelationMapper;
    @Resource
    private Qms8dFlowLogMapper qms8dFlowLogMapper;
    @Resource
    private QmsNcRecordMapper qmsNcRecordMapper;
    @Resource
    private QmsNcRelationMapper qmsNcRelationMapper;
    @Resource
    private QmsNcRecordService qmsNcRecordService;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private BpmModelService bpmModelService;
    @Resource
    private BpmProcessInstanceCopyService bpmProcessInstanceCopyService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    private volatile boolean exceptionBpmModelPublishedAfterStartup;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private RuntimeService runtimeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<QmsExceptionEventRespVO> getExceptionEventPage(QmsExceptionEventPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Set<Long> participatedIds = buildParticipatedExceptionIds(loginUserId);
        Set<Long> pendingTaskExceptionIds = buildPendingGroupTaskExceptionIds(loginUserId);
        Set<Long> containmentOverdueExceptionIds = buildContainmentOverdueExceptionIds(loginUserId);
        PageResult<QmsExceptionEventDO> pageResult = qmsExceptionEventMapper.selectPage(
                pageReqVO, loginUserId, participatedIds, pendingTaskExceptionIds, containmentOverdueExceptionIds);
        pageResult.getList().replaceAll(this::advanceAcceptedGroupTaskProgressIfNeeded);
        PageResult<QmsExceptionEventRespVO> respPage = BeanUtils.toBean(pageResult, QmsExceptionEventRespVO.class);
        fillCurrentUserTaskTodo(respPage.getList(), loginUserId);
        respPage.getList().forEach(respVO -> fillCurrentUserListState(
                respVO, loginUserId, participatedIds, pendingTaskExceptionIds));
        return respPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsExceptionEventRespVO getExceptionEvent(Long id) {
        QmsExceptionEventDO event = advanceAcceptedGroupTaskProgressIfNeeded(validateExceptionExists(id));
        return buildEventResp(event, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsExceptionEventRespVO createExceptionEvent(QmsExceptionEventCreateReqVO createReqVO) {
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = currentUserName();
        QmsExceptionEventDO event = BeanUtils.toBean(createReqVO, QmsExceptionEventDO.class);
        event.setExceptionNo(StrUtil.blankToDefault(createReqVO.getExceptionNo(), generateExceptionNo(now)));
        event.setStatus(STATUS_CONFIRMING);
        event.setCurrentNodeCode(NODE_CONFIRMING);
        event.setCurrentNodeName("确认");
        event.setCurrentHandlerUserId(null);
        event.setCurrentHandlerUserName(null);
        event.setDiscovererId(createReqVO.getDiscovererId() != null ? createReqVO.getDiscovererId() : loginUserId);
        event.setDiscovererCode(defaultCode(createReqVO.getDiscovererCode(), event.getDiscovererId()));
        event.setDiscovererName(firstNotBlank(createReqVO.getDiscovererName(), loginUserName));
        event.setDiscoverDeptId(createReqVO.getDiscoverDeptId() != null ? createReqVO.getDiscoverDeptId() : SecurityFrameworkUtils.getLoginUserDeptId());
        event.setDiscoverDeptCode(defaultCode(createReqVO.getDiscoverDeptCode(), event.getDiscoverDeptId()));
        event.setDiscoverDeptName(firstNotBlank(createReqVO.getDiscoverDeptName(), "当前部门"));
        event.setDiscoverTime(createReqVO.getDiscoverTime() != null ? createReqVO.getDiscoverTime() : now);
        event.setConfirmerId(null);
        event.setConfirmerCode(null);
        event.setConfirmerName(null);
        event.setConfirmDeptId(createReqVO.getConfirmDeptId());
        event.setConfirmDeptCode(defaultCode(createReqVO.getConfirmDeptCode(), event.getConfirmDeptId()));
        event.setConfirmDeptName(createReqVO.getConfirmDeptName());
        event.setIsRelatedProduct(createReqVO.getIsRelatedProduct());
        fillEventCodes(event);
        qmsExceptionEventMapper.insert(event);
        String processInstanceId = startExceptionBpmProcess(event);
        QmsExceptionEventDO processUpdate = new QmsExceptionEventDO();
        processUpdate.setId(event.getId());
        processUpdate.setProcessInstanceId(processInstanceId);
        qmsExceptionEventMapper.updateById(processUpdate);
        event.setProcessInstanceId(processInstanceId);
        approveRunningExceptionBpmTasks(event, BPM_NODE_START_USER, "异常事件提报同步发起登记",
                null);
        syncCurrentHandlerFromRunningBpmTask(event, BPM_NODE_QUALITY_CONFIRM, NODE_CONFIRMING, "确认");
        event = qmsExceptionEventMapper.selectById(event.getId());
        saveTeamMembers(event.getId(), createReqVO.getTeamMembers());
        saveAttachmentRelations(event.getId(), createReqVO.getRelations());
        saveInitialGroupTasks(event, createReqVO.getGroupTasks(), now);
        writeFlowLog(null, event, "REPORT", "提报", null, snapshot("description", event.getDescription()));
        if (StrUtil.isNotBlank(createReqVO.getRelatedNcrNo())) {
            QmsExceptionEventLinkNcrReqVO linkReqVO = new QmsExceptionEventLinkNcrReqVO();
            linkReqVO.setId(event.getId());
            linkReqVO.setNcrNo(createReqVO.getRelatedNcrNo());
            linkReqVO.setNcrType(Boolean.TRUE.equals(createReqVO.getIsRelatedProduct())
                    ? RELATION_TYPE_NCR : RELATION_TYPE_RAW_MATERIAL_NCR);
            linkReqVO.setRemark("提报时关联 NCR");
            linkNcrInternal(linkReqVO, true, true);
        }
        return buildEventResp(qmsExceptionEventMapper.selectById(event.getId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleExceptionEvent(QmsExceptionEventHandleReqVO handleReqVO) {
        QmsExceptionEventDO before = validateExceptionCanFlow(handleReqVO.getId());
        String actionCode = StrUtil.blankToDefault(handleReqVO.getActionCode(), "HANDLE").toUpperCase();
        if (STATUS_CONFIRMING.equals(before.getStatus())) {
            validateConfirmStepClassification(handleReqVO);
            if (!ACTION_MISREPORT_CLOSE.equals(actionCode)) {
                validateConfirmStepContainmentPlan(handleReqVO);
            }
        }
        if (ACTION_MISREPORT_CLOSE.equals(actionCode)) {
            closeMisreportExceptionEvent(before, handleReqVO);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        QmsExceptionEventDO updateObj = BeanUtils.toBean(handleReqVO, QmsExceptionEventDO.class);
        updateObj.setId(before.getId());
        preserveCreateOnlyFields(before, updateObj);
        preserveConfirmStepFieldsOutsideConfirming(before, updateObj);
        fillEventCodes(updateObj);
        applyNextNode(before, updateObj, handleReqVO, now);
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO taskEvent = qmsExceptionEventMapper.selectById(before.getId());
        saveInitialGroupTasks(taskEvent, handleReqVO.getGroupTasks(), now);
        if (handleReqVO.getTeamMembers() != null) {
            saveTeamMembers(before.getId(), handleReqVO.getTeamMembers());
        }
        saveAttachmentRelations(before.getId(), handleReqVO.getRelations());
        syncConfirmStepNcrRelation(before, handleReqVO, true);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        createResultUploadCopiesIfNeeded(before, after, handleReqVO);
        syncBpmToBusinessStatus(after, firstNotBlank(handleReqVO.getOpinion(), "异常事件办理"));
        writeFlowLog(before, after, "HANDLE", "办理", handleReqVO.getOpinion(),
                snapshot("actionCode", actionCode));
    }

    private void closeMisreportExceptionEvent(QmsExceptionEventDO before, QmsExceptionEventHandleReqVO handleReqVO) {
        if (!STATUS_CONFIRMING.equals(before.getStatus())) {
            throw exception(QMS_EXCEPTION_EVENT_MISREPORT_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        QmsExceptionEventDO updateObj = BeanUtils.toBean(handleReqVO, QmsExceptionEventDO.class);
        updateObj.setId(before.getId());
        preserveCreateOnlyFields(before, updateObj);
        preserveConfirmStepFieldsOutsideConfirming(before, updateObj);
        fillEventCodes(updateObj);
        updateObj.setStatus(STATUS_CLOSED);
        updateObj.setCurrentNodeCode(NODE_CLOSED);
        updateObj.setCurrentNodeName("流程结束(误报归档)");
        updateObj.setConfirmTime(firstNonNull(updateObj.getConfirmTime(), now));
        updateObj.setConfirmerId(firstNonNull(updateObj.getConfirmerId(), SecurityFrameworkUtils.getLoginUserId()));
        updateObj.setConfirmerCode(defaultCode(updateObj.getConfirmerCode(), updateObj.getConfirmerId()));
        updateObj.setConfirmerName(firstNotBlank(updateObj.getConfirmerName(), currentUserName()));
        updateObj.setConfirmDeptId(firstNonNull(updateObj.getConfirmDeptId(), before.getConfirmDeptId()));
        updateObj.setConfirmDeptCode(defaultCode(updateObj.getConfirmDeptCode(), updateObj.getConfirmDeptId()));
        updateObj.setConfirmDeptName(firstNotBlank(updateObj.getConfirmDeptName(), before.getConfirmDeptName()));
        updateObj.setEffectConfirm("确认误报，异常事件直接关闭归档");
        updateObj.setQaConfirmValid(true);
        updateObj.setQaConfirmerId(updateObj.getConfirmerId());
        updateObj.setQaConfirmerName(updateObj.getConfirmerName());
        updateObj.setFinishTime(now);
        updateObj.setCloseUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setCloseUserName(currentUserName());
        updateObj.setCurrentHandlerUserId(null);
        updateObj.setCurrentHandlerUserName(null);
        qmsExceptionEventMapper.updateById(updateObj);
        saveAttachmentRelations(before.getId(), handleReqVO.getRelations());
        syncConfirmStepNcrRelation(before, handleReqVO, false);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        syncBpmToBusinessStatus(after, "确认误报关闭异常事件");
        writeFlowLog(before, after, ACTION_MISREPORT_CLOSE, "误报关闭",
                firstNotBlank(handleReqVO.getOpinion(), "确认误报，直接关闭"),
                snapshot("actionCode", ACTION_MISREPORT_CLOSE));
    }

    private void preserveCreateOnlyFields(QmsExceptionEventDO before, QmsExceptionEventDO updateObj) {
        updateObj.setExceptionNo(before.getExceptionNo());
        updateObj.setSourceType(before.getSourceType());
        updateObj.setSourceId(before.getSourceId());
        updateObj.setSourceNo(before.getSourceNo());
    }

    private void preserveConfirmStepFieldsOutsideConfirming(QmsExceptionEventDO before, QmsExceptionEventDO updateObj) {
        if (STATUS_CONFIRMING.equals(before.getStatus())) {
            return;
        }
        updateObj.setExceptionType(before.getExceptionType());
        updateObj.setExceptionLevel(before.getExceptionLevel());
        updateObj.setIsRelatedProduct(before.getIsRelatedProduct());
        updateObj.setRelatedNcrNo(before.getRelatedNcrNo());
    }

    private void validateConfirmStepClassification(QmsExceptionEventHandleReqVO handleReqVO) {
        if (StrUtil.isBlank(handleReqVO.getExceptionType())) {
            throw exception(QMS_EXCEPTION_EVENT_CONFIRM_TYPE_REQUIRED);
        }
        if (StrUtil.isBlank(handleReqVO.getExceptionLevel())) {
            throw exception(QMS_EXCEPTION_EVENT_CONFIRM_LEVEL_REQUIRED);
        }
        if (handleReqVO.getIsRelatedProduct() == null) {
            throw exception(QMS_EXCEPTION_EVENT_CONFIRM_RELATED_PRODUCT_REQUIRED);
        }
    }

    private void validateConfirmStepContainmentPlan(QmsExceptionEventHandleReqVO handleReqVO) {
        if (handleReqVO.getContainmentDeadline() == null) {
            throw exception(QMS_EXCEPTION_EVENT_CONFIRM_DEADLINE_REQUIRED);
        }
    }

    private void syncConfirmStepNcrRelation(QmsExceptionEventDO before, QmsExceptionEventHandleReqVO handleReqVO,
                                            boolean autoCreateProductNcr) {
        if (!STATUS_CONFIRMING.equals(before.getStatus())) {
            return;
        }
        QmsExceptionEventDO event = qmsExceptionEventMapper.selectById(before.getId());
        if (StrUtil.isNotBlank(handleReqVO.getRelatedNcrNo())) {
            if (qmsExceptionRelationMapper.selectByObjectNo(
                    before.getId(), RELATION_TYPE_NCR, handleReqVO.getRelatedNcrNo()) != null
                    || qmsExceptionRelationMapper.selectByObjectNo(
                    before.getId(), RELATION_TYPE_RAW_MATERIAL_NCR, handleReqVO.getRelatedNcrNo()) != null) {
                return;
            }
            QmsExceptionEventLinkNcrReqVO linkReqVO = new QmsExceptionEventLinkNcrReqVO();
            linkReqVO.setId(before.getId());
            linkReqVO.setNcrNo(handleReqVO.getRelatedNcrNo());
            linkReqVO.setNcrType(Boolean.TRUE.equals(handleReqVO.getIsRelatedProduct())
                    ? RELATION_TYPE_NCR : RELATION_TYPE_RAW_MATERIAL_NCR);
            linkReqVO.setRemark("确认时关联处置单");
            linkNcrInternal(linkReqVO, true, true);
            return;
        }
        if (autoCreateProductNcr) {
            autoCreateAndLinkNcrIfNecessary(event);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnExceptionEvent(QmsExceptionEventReturnReqVO returnReqVO) {
        if (StrUtil.isBlank(returnReqVO.getOpinion())) {
            throw exception(QMS_EXCEPTION_EVENT_RETURN_OPINION_REQUIRED);
        }
        QmsExceptionEventDO before = validateExceptionCanFlow(returnReqVO.getId());
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        String targetNodeCode = StrUtil.blankToDefault(returnReqVO.getTargetNodeCode(), NODE_RETURNED);
        LocalDateTime now = LocalDateTime.now();
        if (NODE_CONTAINMENT.equals(targetNodeCode)) {
            updateObj.setStatus(STATUS_CONTAINMENT);
            updateObj.setCurrentNodeCode(NODE_CONTAINMENT);
            updateObj.setCurrentNodeName("围堵措施办理");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getContainmentOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getContainmentOwnerName(), before.getCurrentHandlerUserName()));
            resetGroupTasksForReturn(before.getId(), GROUP_TASK_TYPE_INVESTIGATION, returnReqVO.getOpinion(), now);
        } else if (NODE_CONTAINMENT_SIGN.equals(targetNodeCode)) {
            updateObj.setStatus(STATUS_CONTAINMENT_SIGN);
            updateObj.setCurrentNodeCode(NODE_CONTAINMENT_SIGN);
            updateObj.setCurrentNodeName("临时小组围堵措施会签");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getContainmentOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getContainmentOwnerName(), before.getCurrentHandlerUserName()));
            resetGroupTasksForReturn(before.getId(), GROUP_TASK_TYPE_INVESTIGATION, returnReqVO.getOpinion(), now);
        } else if (NODE_ROOT_CAUSE.equals(targetNodeCode)) {
            updateObj.setStatus(STATUS_ROOT_CAUSE);
            updateObj.setCurrentNodeCode(NODE_ROOT_CAUSE);
            updateObj.setCurrentNodeName("根因分析和纠正预防措施办理");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getActionOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getActionOwnerName(), before.getCurrentHandlerUserName()));
            resetGroupTasksForReturn(before.getId(), GROUP_TASK_TYPE_ROOT_CAUSE, returnReqVO.getOpinion(), now);
        } else if (NODE_ROOT_CAUSE_SIGN.equals(targetNodeCode)) {
            updateObj.setStatus(STATUS_ROOT_CAUSE_SIGN);
            updateObj.setCurrentNodeCode(NODE_ROOT_CAUSE_SIGN);
            updateObj.setCurrentNodeName("根因分析纠正预防会签");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getActionOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getActionOwnerName(), before.getCurrentHandlerUserName()));
            resetGroupTasksForReturn(before.getId(), GROUP_TASK_TYPE_ROOT_CAUSE, returnReqVO.getOpinion(), now);
        } else if (NODE_RESULT_UPLOAD.equals(targetNodeCode)) {
            updateObj.setStatus(STATUS_RESULT_UPLOAD);
            updateObj.setCurrentNodeCode(NODE_RESULT_UPLOAD);
            updateObj.setCurrentNodeName("执行结果上传");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getResultUploaderId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getResultUploaderName(), before.getCurrentHandlerUserName()));
        } else {
            updateObj.setStatus(STATUS_RETURNED);
            updateObj.setCurrentNodeCode(targetNodeCode);
            updateObj.setCurrentNodeName(StrUtil.blankToDefault(returnReqVO.getTargetNodeName(), "退回补充"));
            updateObj.setCurrentHandlerUserId(before.getDiscovererId());
            updateObj.setCurrentHandlerUserName(before.getDiscovererName());
        }
        returnRunningExceptionBpmTask(before, resolveReturnBpmTargetTaskDefinitionKey(targetNodeCode),
                returnReqVO.getOpinion());
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, "RETURN", "退回", returnReqVO.getOpinion(),
                snapshot("targetNodeCode", updateObj.getCurrentNodeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawExceptionEvent(QmsExceptionEventWithdrawReqVO withdrawReqVO) {
        QmsExceptionEventDO before = validateExceptionCanFlow(withdrawReqVO.getId());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        HistoricTaskInstance withdrawTask = findWithdrawableHistoricTask(before, loginUserId);
        if (withdrawTask == null) {
            throw exception(QMS_EXCEPTION_EVENT_WITHDRAW_NOT_AVAILABLE);
        }
        disableStartUserNodeAutoApproveWhenWithdrawToReturned(before, withdrawTask);
        bpmTaskService.withdrawTask(loginUserId, withdrawTask.getId());

        List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                before.getProcessInstanceId(), true, null);
        if (CollUtil.isEmpty(runningTasks)) {
            throw exception(QMS_EXCEPTION_EVENT_WITHDRAW_TARGET_INVALID);
        }
        Task targetTask = runningTasks.get(0);
        String targetStatus = resolveWithdrawStatusByBpmTaskKey(targetTask.getTaskDefinitionKey());
        if (StrUtil.isBlank(targetStatus)) {
            throw exception(QMS_EXCEPTION_EVENT_WITHDRAW_TARGET_INVALID);
        }

        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        applyWithdrawNode(before, updateObj, targetStatus, targetTask);
        resetGroupTasksAfterWithdraw(before.getId(), targetStatus, withdrawReqVO.getReason());
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, "WITHDRAW", "撤回修改", withdrawReqVO.getReason(),
                snapshot("withdrawTaskId", withdrawTask.getId(),
                        "withdrawTaskName", withdrawTask.getName(),
                        "targetTaskKey", targetTask.getTaskDefinitionKey(),
                        "targetTaskName", targetTask.getName(),
                        "targetStatus", targetStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeExceptionEvent(QmsExceptionEventCloseReqVO closeReqVO) {
        if (StrUtil.isBlank(closeReqVO.getEffectConfirm())) {
            throw exception(QMS_EXCEPTION_EVENT_CLOSE_EFFECT_REQUIRED);
        }
        QmsExceptionEventDO before = validateExceptionCanFlow(closeReqVO.getId());
        if (Boolean.TRUE.equals(before.getIsRelatedProduct())
                && qmsExceptionRelationMapper.selectCountByType(before.getId(), "NCR") <= 0) {
            throw exception(QMS_EXCEPTION_EVENT_CLOSE_NCR_REQUIRED);
        }
        LocalDateTime finishTime = closeReqVO.getFinishTime() != null ? closeReqVO.getFinishTime() : LocalDateTime.now();
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_CLOSED);
        updateObj.setCurrentNodeCode(NODE_CLOSED);
        updateObj.setCurrentNodeName("流程结束(归档)");
        updateObj.setEffectConfirm(closeReqVO.getEffectConfirm());
        updateObj.setQaConfirmValid(Boolean.TRUE.equals(closeReqVO.getQaConfirmValid()));
        updateObj.setQaConfirmerId(closeReqVO.getQaConfirmerId() != null ? closeReqVO.getQaConfirmerId() : SecurityFrameworkUtils.getLoginUserId());
        updateObj.setQaConfirmerName(firstNotBlank(closeReqVO.getQaConfirmerName(), currentUserName()));
        updateObj.setFinishTime(finishTime);
        updateObj.setCloseUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setCloseUserName(currentUserName());
        qmsExceptionEventMapper.updateById(updateObj);
        saveAttachmentRelations(before.getId(), closeReqVO.getRelations());
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        syncBpmToBusinessStatus(after, "异常事件关闭");
        writeFlowLog(before, after, "CLOSE", "关闭", firstNotBlank(closeReqVO.getOpinion(), closeReqVO.getEffectConfirm()),
                snapshot("effectConfirm", closeReqVO.getEffectConfirm()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGroupTasks(QmsExceptionGroupTaskBatchSaveReqVO saveReqVO) {
        QmsExceptionEventDO before = validateExceptionCanFlow(saveReqVO.getExceptionId());
        validateCurrentHandlerOrDispatcher(before, null);
        LocalDateTime now = LocalDateTime.now();
        List<QmsExceptionGroupTaskReqVO> reqTasks = saveReqVO.getGroupTasks() == null
                ? List.of() : saveReqVO.getGroupTasks();
        Set<String> incomingTypes = new HashSet<>();
        for (QmsExceptionGroupTaskReqVO reqTask : reqTasks) {
            if (!incomingTypes.add(normalizeTaskType(reqTask.getTaskType()))) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_ASSIGNMENT_REQUIRED);
            }
        }
        Set<Long> incomingIds = reqTasks.stream()
                .map(QmsExceptionGroupTaskReqVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        qmsExceptionGroupTaskMapper.selectListByExceptionId(before.getId()).forEach(existing -> {
            if (incomingTypes.contains(normalizeTaskType(existing.getTaskType()))
                    && !incomingIds.contains(existing.getId())
                    && !statusIn(existing.getTaskStatus(), GROUP_TASK_STATUS_SUBMITTED,
                    GROUP_TASK_STATUS_SIGNED, GROUP_TASK_STATUS_ACCEPTED)) {
                qmsExceptionGroupTaskMemberMapper.deleteByTaskId(existing.getId());
                qmsExceptionGroupTaskReplyMapper.deleteByTaskId(existing.getId());
                qmsExceptionGroupTaskConfirmLogMapper.deleteByTaskId(existing.getId());
                qmsExceptionGroupTaskMapper.deleteById(existing.getId());
            }
        });
        for (int i = 0; i < reqTasks.size(); i++) {
            saveGroupTask(before, reqTasks.get(i), i + 1, now);
        }
        refreshContainmentDeadlineFromGroupTasks(before.getId());
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, "DISPATCH_GROUP_TASK", "下达临时小组任务",
                firstNotBlank(saveReqVO.getOpinion(), "下达围堵与根因任务"),
                snapshot("taskCount", reqTasks.size()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitGroupTask(QmsExceptionGroupTaskSubmitReqVO submitReqVO) {
        QmsExceptionGroupTaskDO beforeTask = validateGroupTaskExists(submitReqVO.getId());
        QmsExceptionEventDO event = validateExceptionCanFlow(beforeTask.getExceptionId());
        validateGroupTaskActiveForCurrentNode(event, beforeTask);
        if (!statusIn(beforeTask.getTaskStatus(), GROUP_TASK_STATUS_DISPATCHED, GROUP_TASK_STATUS_RETURNED)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
        if (!isGroupTaskAssignee(beforeTask, SecurityFrameworkUtils.getLoginUserId())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION);
        }
        LocalDateTime now = LocalDateTime.now();
        QmsExceptionGroupTaskDO updateObj = new QmsExceptionGroupTaskDO();
        updateObj.setId(beforeTask.getId());
        updateObj.setTaskStatus(GROUP_TASK_STATUS_SUBMITTED);
        updateObj.setActualFinishTime(firstNonNull(submitReqVO.getActualFinishTime(), now));
        updateObj.setActionDescription(submitReqVO.getActionDescription());
        updateObj.setRootCauseCategory(submitReqVO.getRootCauseCategory());
        updateObj.setRootCause(submitReqVO.getRootCause());
        updateObj.setPreventiveAction(submitReqVO.getPreventiveAction());
        updateObj.setAttachmentUrls(cleanStringList(submitReqVO.getAttachmentUrls()));
        updateObj.setSubmitterUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setSubmitterUserName(currentUserName());
        updateObj.setSubmitTime(now);
        updateObj.setReviewOpinion("");
        updateObj.setReplyCount(firstNonNull(beforeTask.getReplyCount(), 0) + 1);
        validateTaskSubmitContent(beforeTask, submitReqVO);
        saveGroupTaskReply(beforeTask, submitReqVO, updateObj.getReplyCount(), now);
        qmsExceptionGroupTaskMapper.updateById(updateObj);
        resetGroupTaskMemberConfirmStatus(beforeTask.getId());
        writeFlowLog(event, event, "GROUP_TASK_SUBMIT", "小组任务提交",
                firstNotBlank(submitReqVO.getOpinion(), submitReqVO.getActionDescription(), "小组任务提交"),
                snapshot("taskId", beforeTask.getId(), "groupName", beforeTask.getGroupName(),
                        "submitter", currentUserName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsExceptionGroupTaskMemberConfirmRespVO confirmGroupTaskMember(
            QmsExceptionGroupTaskMemberConfirmReqVO confirmReqVO) {
        QmsExceptionGroupTaskDO task = validateGroupTaskExists(confirmReqVO.getTaskId());
        QmsExceptionEventDO event = validateExceptionCanFlow(task.getExceptionId());
        validateGroupTaskActiveForCurrentNode(event, task);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        QmsExceptionGroupTaskMemberDO member = selectCurrentHandlerGroupTaskMember(
                task.getId(), confirmReqVO.getMemberId(), loginUserId);
        if (member == null) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION);
        }
        if (!statusIn(task.getTaskStatus(), GROUP_TASK_STATUS_DISPATCHED,
                GROUP_TASK_STATUS_RETURNED, GROUP_TASK_STATUS_SUBMITTED)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
        if (!isPendingMemberConfirm(member)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION);
        }
        LocalDateTime now = LocalDateTime.now();
        String actionCode = StrUtil.blankToDefault(confirmReqVO.getActionCode(), GROUP_MEMBER_ACTION_CONFIRM).toUpperCase();
        if (!statusIn(actionCode, GROUP_MEMBER_ACTION_CONFIRM, GROUP_MEMBER_ACTION_DISAGREE)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
        if (GROUP_MEMBER_ACTION_DISAGREE.equals(actionCode) && StrUtil.isBlank(confirmReqVO.getRemark())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_RETURN_OPINION_REQUIRED);
        }
        if (GROUP_MEMBER_ACTION_CONFIRM.equals(actionCode)) {
            validateMemberSubmitContent(task, confirmReqVO);
        }
        String taskStatusBefore = task.getTaskStatus();
        boolean disagreed = GROUP_MEMBER_ACTION_DISAGREE.equals(actionCode);
        QmsExceptionGroupTaskMemberDO updateObj = new QmsExceptionGroupTaskMemberDO();
        updateObj.setId(member.getId());
        updateObj.setActualFinishTime(firstNonNull(confirmReqVO.getActualFinishTime(), now));
        updateObj.setActionDescription(firstNotBlank(confirmReqVO.getActionDescription(), confirmReqVO.getRemark()));
        updateObj.setRootCauseCategory(confirmReqVO.getRootCauseCategory());
        updateObj.setRootCause(confirmReqVO.getRootCause());
        updateObj.setPreventiveAction(confirmReqVO.getPreventiveAction());
        updateObj.setAttachmentUrls(cleanStringList(confirmReqVO.getAttachmentUrls()));
        updateObj.setActualHandlerUserId(loginUserId);
        updateObj.setActualHandlerUserName(currentUserName());
        updateObj.setConfirmStatus(disagreed ? GROUP_MEMBER_CONFIRM_DISAGREED : GROUP_MEMBER_CONFIRM_CONFIRMED);
        updateObj.setOverdueFlag(false);
        updateObj.setConfirmRemark(confirmReqVO.getRemark());
        updateObj.setConfirmTime(now);
        qmsExceptionGroupTaskMemberMapper.updateById(updateObj);
        member.setActualHandlerUserId(loginUserId);
        member.setActualHandlerUserName(currentUserName());

        String taskStatusAfter = taskStatusBefore;
        if (disagreed) {
            QmsExceptionGroupTaskDO returnTask = buildTaskReviewUpdate(
                    task, GROUP_TASK_STATUS_RETURNED, now,
                    firstNotBlank(confirmReqVO.getRemark(), "成员不同意，退回填写人重写"));
            qmsExceptionGroupTaskMapper.updateById(returnTask);
            reviewLatestTaskReply(task.getId(), returnTask);
            taskStatusAfter = GROUP_TASK_STATUS_RETURNED;
        } else if (allTaskMembersConfirmed(task.getId())) {
            QmsExceptionGroupTaskDO acceptedTask = buildMemberSubmitTaskUpdate(task, member, confirmReqVO,
                    GROUP_TASK_STATUS_ACCEPTED, now);
            acceptedTask.setReviewerUserId(SecurityFrameworkUtils.getLoginUserId());
            acceptedTask.setReviewerUserName(currentUserName());
            acceptedTask.setReviewTime(now);
            acceptedTask.setReviewOpinion(firstNotBlank(confirmReqVO.getRemark(), "全员办理完成"));
            qmsExceptionGroupTaskMapper.updateById(acceptedTask);
            saveGroupTaskMemberReply(task, member, confirmReqVO, acceptedTask.getReplyCount(), GROUP_TASK_STATUS_ACCEPTED, now);
            taskStatusAfter = GROUP_TASK_STATUS_ACCEPTED;
        }
        if (!disagreed && !GROUP_TASK_STATUS_ACCEPTED.equals(taskStatusAfter)) {
            QmsExceptionGroupTaskDO submittedTask = buildMemberSubmitTaskUpdate(task, member, confirmReqVO,
                    GROUP_TASK_STATUS_SUBMITTED, now);
            qmsExceptionGroupTaskMapper.updateById(submittedTask);
            saveGroupTaskMemberReply(task, member, confirmReqVO, submittedTask.getReplyCount(), GROUP_TASK_STATUS_SUBMITTED, now);
            taskStatusAfter = GROUP_TASK_STATUS_SUBMITTED;
        }
        saveGroupTaskConfirmLog(event, task, member, actionCode, updateObj.getConfirmStatus(),
                confirmReqVO.getRemark(), taskStatusBefore, taskStatusAfter, now);
        refreshContainmentDeadlineFromGroupTasks(event.getId());
        if (GROUP_TASK_STATUS_ACCEPTED.equals(taskStatusAfter)) {
            aggregateAcceptedGroupTasksToEvent(event.getId());
            tryAdvanceEventAfterTaskAccepted(event, task, now);
        } else {
            Long bpmAssigneeUserId = firstNonNull(member.getUserId(), loginUserId);
            approveCurrentMemberBpmTask(event, task, bpmAssigneeUserId,
                    firstNotBlank(confirmReqVO.getRemark(),
                            buildMemberHandleReason(member, loginUserId,
                                    disagreed ? "成员不同意任务" : "成员提交办理内容")));
        }
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(event.getId());
        writeFlowLog(event, after, disagreed ? "GROUP_TASK_MEMBER_DISAGREE" : "GROUP_TASK_MEMBER_CONFIRM",
                disagreed ? "成员不同意任务" : "成员提交任务",
                firstNotBlank(confirmReqVO.getRemark(), disagreed ? "不同意，退回填写人重写" : "成员提交办理内容"),
                snapshot("taskId", task.getId(), "groupName", task.getGroupName(),
                        "member", buildMemberActualHandlerDisplay(member, loginUserId),
                        "assignedMember", member.getUserName(),
                        "delegateUserId", member.getDelegateUserId(),
                        "taskStatusAfter", taskStatusAfter));

        QmsExceptionGroupTaskMemberConfirmRespVO respVO = new QmsExceptionGroupTaskMemberConfirmRespVO();
        respVO.setExceptionId(event.getId());
        respVO.setExceptionNo(event.getExceptionNo());
        respVO.setTaskId(task.getId());
        respVO.setConfirmStatus(updateObj.getConfirmStatus());
        respVO.setOverdue(false);
        respVO.setMessage(disagreed ? "已退回填写人重写" : "已提交办理内容");
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateGroupTaskMember(QmsExceptionGroupTaskMemberDelegateReqVO delegateReqVO) {
        QmsExceptionGroupTaskDO task = validateGroupTaskExists(delegateReqVO.getTaskId());
        QmsExceptionEventDO event = validateExceptionCanFlow(task.getExceptionId());
        validateGroupTaskActiveForCurrentNode(event, task);
        if (!statusIn(task.getTaskStatus(), GROUP_TASK_STATUS_DISPATCHED,
                GROUP_TASK_STATUS_RETURNED, GROUP_TASK_STATUS_SUBMITTED)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        QmsExceptionGroupTaskMemberDO member = qmsExceptionGroupTaskMemberMapper.selectById(delegateReqVO.getMemberId());
        if (member == null || !Objects.equals(member.getTaskId(), task.getId())
                || !Objects.equals(member.getUserId(), loginUserId)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION);
        }
        if (!isPendingMemberConfirm(member)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION);
        }
        Long delegateUserId = delegateReqVO.getDelegateUserId();
        if (delegateUserId == null) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_DELEGATE_REQUIRED);
        }
        if (Objects.equals(delegateUserId, member.getUserId())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_DELEGATE_SELF_INVALID);
        }
        AdminUserRespDTO delegateUser = adminUserApi.getUser(delegateUserId);
        String delegateUserName = firstNotBlank(delegateReqVO.getDelegateUserName(),
                delegateUser == null ? null : delegateUser.getNickname(), String.valueOf(delegateUserId));
        LocalDateTime now = LocalDateTime.now();
        QmsExceptionGroupTaskMemberDO updateObj = new QmsExceptionGroupTaskMemberDO();
        updateObj.setId(member.getId());
        updateObj.setDelegateUserId(delegateUserId);
        updateObj.setDelegateUserName(delegateUserName);
        updateObj.setDelegateTime(now);
        qmsExceptionGroupTaskMemberMapper.updateById(updateObj);
        writeFlowLog(event, event, "GROUP_TASK_MEMBER_DELEGATE", "会签办理委托",
                currentUserName() + "委托" + delegateUserName + "代办",
                snapshot("taskId", task.getId(), "taskType", normalizeTaskType(task.getTaskType()),
                        "memberId", member.getId(), "member", member.getUserName(),
                        "delegateUserId", delegateUserId, "delegateUserName", delegateUserName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewGroupTask(QmsExceptionGroupTaskReviewReqVO reviewReqVO) {
        QmsExceptionGroupTaskDO beforeTask = validateGroupTaskExists(reviewReqVO.getId());
        QmsExceptionEventDO before = validateExceptionCanFlow(beforeTask.getExceptionId());
        validateCurrentHandlerOrDispatcher(before, beforeTask);
        if (!GROUP_TASK_STATUS_SIGNED.equals(beforeTask.getTaskStatus())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
        String actionCode = StrUtil.blankToDefault(reviewReqVO.getActionCode(), "").toUpperCase();
        if (GROUP_TASK_ACTION_RETURN.equals(actionCode) && StrUtil.isBlank(reviewReqVO.getOpinion())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_RETURN_OPINION_REQUIRED);
        }
        if (!statusIn(actionCode, GROUP_TASK_ACTION_ACCEPT, GROUP_TASK_ACTION_RETURN)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
        boolean accepted = GROUP_TASK_ACTION_ACCEPT.equals(actionCode);
        QmsExceptionGroupTaskDO updateObj = new QmsExceptionGroupTaskDO();
        updateObj.setId(beforeTask.getId());
        updateObj.setTaskStatus(accepted ? GROUP_TASK_STATUS_ACCEPTED : GROUP_TASK_STATUS_RETURNED);
        updateObj.setReviewerUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setReviewerUserName(currentUserName());
        updateObj.setReviewTime(LocalDateTime.now());
        updateObj.setReviewOpinion(reviewReqVO.getOpinion());
        qmsExceptionGroupTaskMapper.updateById(updateObj);
        reviewLatestTaskReply(beforeTask.getId(), updateObj);
        if (!accepted) {
            resetGroupTaskMemberConfirmStatus(beforeTask.getId());
        }
        if (accepted) {
            aggregateAcceptedGroupTasksToEvent(before.getId());
        }
        refreshContainmentDeadlineFromGroupTasks(before.getId());
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, accepted ? "GROUP_TASK_ACCEPT" : "GROUP_TASK_RETURN",
                accepted ? "接收小组任务" : "退回小组任务",
                firstNotBlank(reviewReqVO.getOpinion(), accepted ? "接收完成" : "退回重做"),
                snapshot("taskId", beforeTask.getId(), "groupName", beforeTask.getGroupName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkNcr(QmsExceptionEventLinkNcrReqVO linkReqVO) {
        linkNcrInternal(linkReqVO, false);
    }

    private void linkNcrInternal(QmsExceptionEventLinkNcrReqVO linkReqVO, boolean allowSavedException) {
        linkNcrInternal(linkReqVO, allowSavedException, false);
    }

    private void linkNcrInternal(QmsExceptionEventLinkNcrReqVO linkReqVO, boolean allowSavedException,
                                 boolean validateSelectableNcr) {
        QmsExceptionEventDO before = validateExceptionExists(linkReqVO.getId());
        if (!allowSavedException) {
            throw exception(QMS_EXCEPTION_EVENT_NCR_LINK_LOCKED);
        }
        QmsNcRecordDO ncr = resolveNcr(linkReqVO);
        if (validateSelectableNcr) {
            validateNcrSelectableForException(ncr);
        }
        String relationType = normalizeNcrRelationType(linkReqVO.getNcrType(), ncr);
        QmsExceptionRelationDO existingRelation = qmsExceptionRelationMapper.selectByObjectNo(
                before.getId(), relationType, ncr.getNcNo());
        if (existingRelation == null) {
            boolean firstNcr = qmsExceptionRelationMapper.selectCountByType(before.getId(), RELATION_TYPE_NCR) == 0
                    && qmsExceptionRelationMapper.selectCountByType(before.getId(), RELATION_TYPE_RAW_MATERIAL_NCR) == 0;
            qmsExceptionRelationMapper.insert(QmsExceptionRelationDO.builder()
                    .exceptionId(before.getId())
                    .relationType(relationType)
                    .relatedObjectId(ncr.getId())
                    .relatedObjectNo(ncr.getNcNo())
                    .relatedObjectName(firstNotBlank(ncr.getLotNo(), ncr.getDefectCode(), ncr.getRemark()))
                    .relationStatus(ncr.getMrbDecision())
                    .primaryFlag(firstNcr)
                    .relationTime(LocalDateTime.now())
                    .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                    .relationUserName(currentUserName())
                    .remark(linkReqVO.getRemark())
                    .build());
        }
        upsertNcrExceptionRelation(ncr, before, linkReqVO.getRemark());
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        updateObj.setRelatedNcrNo(ncr.getNcNo());
        updateObj.setIsRelatedProduct(RELATION_TYPE_NCR.equals(relationType));
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, "LINK_NCR",
                RELATION_TYPE_RAW_MATERIAL_NCR.equals(relationType) ? "关联原物料处置单" : "关联NCR",
                linkReqVO.getRemark(),
                snapshot("ncrNo", ncr.getNcNo(), "relationType", relationType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Qms8dReportRespVO generate8d(QmsExceptionEventGenerate8dReqVO generateReqVO) {
        QmsExceptionEventDO before = validateExceptionExists(generateReqVO.getId());
        QmsExceptionRelationDO primary8dRelation = qmsExceptionRelationMapper.selectPrimaryRelation(before.getId(), "EIGHT_D");
        if (primary8dRelation != null) {
            Qms8dReportDO report = primary8dRelation.getRelatedObjectId() != null
                    ? qms8dReportMapper.selectById(primary8dRelation.getRelatedObjectId())
                    : qms8dReportMapper.selectByReportNo(primary8dRelation.getRelatedObjectNo());
            if (report != null) {
                return build8dResp(report);
            }
        }
        LocalDate now = LocalDate.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = currentUserName();
        Qms8dReportDO report = Qms8dReportDO.builder()
                .reportNo(generate8dNo())
                .sourceType("EXCEPTION")
                .sourceId(before.getId())
                .sourceNo(before.getExceptionNo())
                .issueDate(now)
                .targetDate(generateReqVO.getTargetDate() != null ? generateReqVO.getTargetDate() : now.plusDays(30))
                .currentStep("D1_D2")
                .status("APPROVING")
                .currentNodeCode("D1_D2_DEFINE")
                .currentNodeName("D1-D2团队与问题定义")
                .currentHandlerUserId(loginUserId)
                .currentHandlerUserName(loginUserName)
                .initiatorUserId(loginUserId)
                .initiatorUserName(loginUserName)
                .initiatorDeptId(SecurityFrameworkUtils.getLoginUserDeptId())
                .initiatorDeptName("当前部门")
                .problemDesc(before.getDescription())
                .containmentAction(before.getContainmentAction())
                .rootCauseCategory(before.getRootCauseCategory())
                .rootCauseAnalysis(before.getRootCause())
                .correctiveAction(before.getPreventiveAction())
                .actionOwnerId(before.getActionOwnerId())
                .actionOwnerName(before.getActionOwnerName())
                .updateSop(false)
                .updateFmea(false)
                .updateControlPlan(false)
                .build();
        qms8dReportMapper.insert(report);
        save8dTeamMembers(report.getId(), generateReqVO.getTeamMembers());
        qms8dRelationMapper.insert(Qms8dRelationDO.builder()
                .reportId(report.getId())
                .reportNo(report.getReportNo())
                .relationType("EXCEPTION")
                .relatedObjectId(before.getId())
                .relatedObjectNo(before.getExceptionNo())
                .relatedObjectName(before.getDescription())
                .relationStatus(before.getStatus())
                .primaryFlag(true)
                .relationTime(LocalDateTime.now())
                .relationUserId(loginUserId)
                .relationUserName(loginUserName)
                .remark(generateReqVO.getRemark())
                .build());
        write8dFlowLog(report, generateReqVO.getRemark(),
                snapshot("reportNo", report.getReportNo(), "sourceType", report.getSourceType(),
                        "sourceNo", report.getSourceNo(), "targetDate", report.getTargetDate()));
        qmsExceptionRelationMapper.insert(QmsExceptionRelationDO.builder()
                .exceptionId(before.getId())
                .relationType("EIGHT_D")
                .relatedObjectId(report.getId())
                .relatedObjectNo(report.getReportNo())
                .relatedObjectName(before.getDescription())
                .relationStatus(report.getStatus())
                .primaryFlag(true)
                .relationTime(LocalDateTime.now())
                .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                .relationUserName(currentUserName())
                .remark(generateReqVO.getRemark())
                .build());
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        updateObj.setRelated8dNo(report.getReportNo());
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, "GENERATE_8D", "生成8D", generateReqVO.getRemark(),
                snapshot("reportNo", report.getReportNo()));
        return build8dResp(report);
    }

    @Override
    public List<QmsExceptionEventRespVO.FlowLog> getFlowLogList(Long exceptionId) {
        validateExceptionExists(exceptionId);
        return BeanUtils.toBean(qmsExceptionFlowLogMapper.selectListByExceptionId(exceptionId),
                QmsExceptionEventRespVO.FlowLog.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBpmProcessStatus(String businessKey, String processInstanceId, Integer bpmStatus, String reason) {
        if (!Objects.equals(bpmStatus, BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            return;
        }
        QmsExceptionEventDO before = resolveExceptionByBpmBusiness(businessKey, processInstanceId);
        if (before == null || STATUS_CANCELLED.equals(before.getStatus())) {
            return;
        }
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        updateObj.setStatus(STATUS_CANCELLED);
        updateObj.setCurrentNodeCode(STATUS_CANCELLED);
        updateObj.setCurrentNodeName("流程已取消");
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, "BPM_CANCEL", "流程取消", reason,
                snapshot("processInstanceId", processInstanceId, "bpmStatus", bpmStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncConfirmTaskTransfer(String businessKey, String processInstanceId, String taskId, Long fromUserId,
                                        Long toUserId, String reason) {
        QmsExceptionEventDO before = resolveExceptionByBpmBusiness(businessKey, processInstanceId);
        if (before == null || !STATUS_CONFIRMING.equals(before.getStatus())
                || !NODE_CONFIRMING.equals(before.getCurrentNodeCode()) || toUserId == null) {
            return;
        }
        AdminUserRespDTO toUser = adminUserApi.getUser(toUserId);
        String toUserName = toUser == null ? String.valueOf(toUserId)
                : firstNotBlank(toUser.getNickname(), toUser.getUsername(), String.valueOf(toUserId));

        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        updateObj.setCurrentHandlerUserId(toUserId);
        updateObj.setCurrentHandlerUserName(toUserName);
        updateObj.setConfirmerId(toUserId);
        updateObj.setConfirmerCode(toUser == null ? defaultCode(null, toUserId)
                : firstNotBlank(toUser.getUsername(), defaultCode(null, toUserId)));
        updateObj.setConfirmerName(toUserName);
        Long deptId = toUser == null ? null : toUser.getDeptId();
        if (deptId != null) {
            DeptRespDTO dept = deptApi.getDept(deptId);
            updateObj.setConfirmDeptId(deptId);
            updateObj.setConfirmDeptCode(defaultCode(null, deptId));
            updateObj.setConfirmDeptName(dept == null ? String.valueOf(deptId) : dept.getName());
        }
        qmsExceptionEventMapper.updateById(updateObj);

        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        String fromUserName = resolveUserName(fromUserId);
        writeFlowLog(before, after, "TRANSFER", "转办",
                firstNotBlank(reason, firstNotBlank(fromUserName, "原确认人") + "转办给" + toUserName),
                snapshot("taskId", taskId, "processInstanceId", processInstanceId,
                        "fromUserId", fromUserId, "fromUserName", fromUserName,
                        "toUserId", toUserId, "toUserName", toUserName));
    }

    private String startExceptionBpmProcess(QmsExceptionEventDO event) {
        if (StrUtil.isNotBlank(event.getProcessInstanceId())) {
            return event.getProcessInstanceId();
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Map<String, Object> variables = buildExceptionBpmVariables(event);
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_EXCEPTION_PROCESS_KEY)
                .setBusinessKey(String.valueOf(event.getId()))
                .setVariables(variables);
        return createExceptionProcessInstance(loginUserId, reqDTO);
    }

    private String createExceptionProcessInstance(Long loginUserId, BpmProcessInstanceCreateReqDTO reqDTO) {
        deployBuiltInExceptionBpmModelOnce();
        try {
            return bpmProcessInstanceApi.createProcessInstance(loginUserId, reqDTO);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                throw ex;
            }
            deployBuiltInExceptionBpmModel();
            try {
                return bpmProcessInstanceApi.createProcessInstance(loginUserId, reqDTO);
            } catch (ServiceException retryEx) {
                if (Objects.equals(retryEx.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) {
                    throw exception(QMS_EXCEPTION_EVENT_BPM_NOT_PUBLISHED);
                }
                throw retryEx;
            }
        }
    }

    private void deployBuiltInExceptionBpmModelOnce() {
        if (exceptionBpmModelPublishedAfterStartup) {
            return;
        }
        synchronized (this) {
            if (exceptionBpmModelPublishedAfterStartup) {
                return;
            }
            deployBuiltInExceptionBpmModel();
            exceptionBpmModelPublishedAfterStartup = true;
        }
    }

    private void deployBuiltInExceptionBpmModel() {
        bpmModelService.deployModel(BPM_EXCEPTION_BUILT_IN_MANAGER_USER_ID, BPM_EXCEPTION_MODEL_ID);
    }

    private QmsExceptionEventDO resolveExceptionByBpmBusiness(String businessKey, String processInstanceId) {
        Long exceptionId = parseLongOrNull(businessKey);
        if (exceptionId != null) {
            QmsExceptionEventDO event = qmsExceptionEventMapper.selectById(exceptionId);
            if (event != null) {
                return event;
            }
        }
        if (StrUtil.isBlank(processInstanceId)) {
            return null;
        }
        return qmsExceptionEventMapper.selectByProcessInstanceId(processInstanceId);
    }

    private void syncBpmToBusinessStatus(QmsExceptionEventDO event, String reason) {
        if (event == null || StrUtil.isBlank(event.getProcessInstanceId())) {
            return;
        }
        String status = event.getStatus();
        if (statusIn(status, STATUS_CONFIRMING, STATUS_CONTAINMENT, STATUS_CONTAINMENT_SIGN,
                STATUS_RESPONSIBILITY, STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN,
                STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_START_USER, reason, null);
            if (STATUS_CONFIRMING.equals(status)) {
                syncCurrentHandlerFromRunningBpmTask(event, BPM_NODE_QUALITY_CONFIRM, NODE_CONFIRMING, "确认");
            }
        }
        if (statusIn(status, STATUS_CONTAINMENT, STATUS_CONTAINMENT_SIGN,
                STATUS_RESPONSIBILITY, STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN,
                STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_QUALITY_CONFIRM, reason,
                    nextAssignees(BPM_NODE_CONTAINMENT_OWNER_HANDLE, buildContainmentOwnerBpmAssigneeUserIds(event)));
        }
        if (statusIn(status, STATUS_CONTAINMENT_SIGN, STATUS_RESPONSIBILITY, STATUS_ROOT_CAUSE,
                STATUS_ROOT_CAUSE_SIGN, STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_CONTAINMENT_OWNER_HANDLE, reason,
                    containmentNextAssignees(event));
        }
        if (statusIn(status, STATUS_RESPONSIBILITY, STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN,
                STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_CONTAINMENT_MEMBER_SIGN, reason,
                    nextAssignees(BPM_NODE_RESPONSIBILITY_CONFIRM, buildResponsibilityConfirmBpmAssigneeUserIds(event)));
        }
        if (statusIn(status, STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN,
                STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_RESPONSIBILITY_CONFIRM, reason,
                    nextAssignees(BPM_NODE_ROOT_CAUSE_OWNER_HANDLE, buildRootCauseOwnerBpmAssigneeUserIds(event)));
        }
        if (statusIn(status, STATUS_ROOT_CAUSE_SIGN, STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_ROOT_CAUSE_OWNER_HANDLE, reason,
                    rootCauseNextAssignees(event));
        }
        if (statusIn(status, STATUS_RESULT_UPLOADER_ASSIGN, STATUS_RESULT_UPLOAD,
                STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_ROOT_CAUSE_MEMBER_SIGN, reason,
                    nextAssignees(BPM_NODE_RESULT_UPLOADER_ASSIGN, buildResultUploaderAssignBpmAssigneeUserIds(event)));
        }
        if (statusIn(status, STATUS_RESULT_UPLOAD, STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_RESULT_UPLOADER_ASSIGN, reason,
                    nextAssignees(BPM_NODE_RESULT_UPLOAD, buildResultUploadBpmAssigneeUserIds(event)));
        }
        if (statusIn(status, STATUS_QA_CLOSURE, STATUS_VERIFYING, STATUS_CLOSED)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_RESULT_UPLOAD, reason,
                    nextAssignees(BPM_NODE_QA_CLOSURE, buildQaClosureBpmAssigneeUserIds(event)));
        }
        if (STATUS_CLOSED.equals(status)) {
            approveRunningExceptionBpmTasks(event, BPM_NODE_QA_CLOSURE, reason, null);
        }
    }

    private void returnRunningExceptionBpmTask(QmsExceptionEventDO event, String targetTaskDefinitionKey, String reason) {
        if (event == null || StrUtil.isBlank(event.getProcessInstanceId())
                || StrUtil.isBlank(targetTaskDefinitionKey)) {
            return;
        }
        List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                event.getProcessInstanceId(), true, null);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        Task currentTask = selectCurrentUserRunningTask(tasks);
        if (currentTask == null || targetTaskDefinitionKey.equals(currentTask.getTaskDefinitionKey())) {
            return;
        }
        Long assigneeUserId = parseTaskAssignee(currentTask);
        if (assigneeUserId == null) {
            return;
        }
        BpmTaskReturnReqVO reqVO = new BpmTaskReturnReqVO()
                .setId(currentTask.getId())
                .setTargetTaskDefinitionKey(targetTaskDefinitionKey)
                .setReason(reason);
        bpmTaskService.returnTask(assigneeUserId, reqVO);
    }

    private void disableStartUserNodeAutoApproveWhenWithdrawToReturned(QmsExceptionEventDO event,
                                                                       HistoricTaskInstance withdrawTask) {
        if (event == null || withdrawTask == null || StrUtil.isBlank(event.getProcessInstanceId())
                || !BPM_NODE_START_USER.equals(withdrawTask.getTaskDefinitionKey())) {
            return;
        }
        runtimeService.setVariable(event.getProcessInstanceId(),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_SKIP_START_USER_NODE, "false");
    }

    private String resolveWithdrawStatusByBpmTaskKey(String taskDefinitionKey) {
        if (BPM_NODE_START_USER.equals(taskDefinitionKey)) {
            return STATUS_RETURNED;
        }
        if (BPM_NODE_QUALITY_CONFIRM.equals(taskDefinitionKey)) {
            return STATUS_CONFIRMING;
        }
        if (statusIn(taskDefinitionKey, BPM_NODE_CONTAINMENT_OWNER_HANDLE,
                BPM_NODE_CONTAINMENT_OWNER_HANDLE_LEGACY)) {
            return STATUS_CONTAINMENT;
        }
        if (BPM_NODE_CONTAINMENT_MEMBER_SIGN.equals(taskDefinitionKey)) {
            return STATUS_CONTAINMENT_SIGN;
        }
        if (BPM_NODE_RESPONSIBILITY_CONFIRM.equals(taskDefinitionKey)) {
            return STATUS_RESPONSIBILITY;
        }
        if (statusIn(taskDefinitionKey, BPM_NODE_ROOT_CAUSE_OWNER_HANDLE,
                BPM_NODE_ROOT_CAUSE_OWNER_HANDLE_LEGACY)) {
            return STATUS_ROOT_CAUSE;
        }
        if (BPM_NODE_ROOT_CAUSE_MEMBER_SIGN.equals(taskDefinitionKey)) {
            return STATUS_ROOT_CAUSE_SIGN;
        }
        if (BPM_NODE_RESULT_UPLOADER_ASSIGN.equals(taskDefinitionKey)) {
            return STATUS_RESULT_UPLOADER_ASSIGN;
        }
        if (BPM_NODE_RESULT_UPLOAD.equals(taskDefinitionKey)) {
            return STATUS_RESULT_UPLOAD;
        }
        if (BPM_NODE_QA_CLOSURE.equals(taskDefinitionKey)) {
            return STATUS_QA_CLOSURE;
        }
        return null;
    }

    private void applyWithdrawNode(QmsExceptionEventDO before, QmsExceptionEventDO updateObj,
                                   String targetStatus, Task targetTask) {
        updateObj.setStatus(targetStatus);
        Long taskAssigneeUserId = parseTaskAssignee(targetTask);
        String taskAssigneeUserName = resolveUserName(taskAssigneeUserId);
        if (STATUS_RETURNED.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_RETURNED);
            updateObj.setCurrentNodeName("退回补充");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getDiscovererId(), taskAssigneeUserId));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getDiscovererName(), taskAssigneeUserName));
            return;
        }
        if (STATUS_CONFIRMING.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_CONFIRMING);
            updateObj.setCurrentNodeName("确认");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getConfirmerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getConfirmerName()));
            return;
        }
        if (STATUS_CONTAINMENT.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_CONTAINMENT);
            updateObj.setCurrentNodeName("围堵措施办理");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getContainmentOwnerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getContainmentOwnerName()));
            return;
        }
        if (STATUS_CONTAINMENT_SIGN.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_CONTAINMENT_SIGN);
            updateObj.setCurrentNodeName("临时小组围堵措施会签");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getContainmentOwnerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getContainmentOwnerName()));
            return;
        }
        if (STATUS_RESPONSIBILITY.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_RESPONSIBILITY);
            updateObj.setCurrentNodeName("责任部门确认");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getContainmentOwnerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getContainmentOwnerName()));
            return;
        }
        if (STATUS_ROOT_CAUSE.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_ROOT_CAUSE);
            updateObj.setCurrentNodeName("根因分析和纠正预防措施办理");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getActionOwnerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getActionOwnerName()));
            return;
        }
        if (STATUS_ROOT_CAUSE_SIGN.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_ROOT_CAUSE_SIGN);
            updateObj.setCurrentNodeName("根因分析纠正预防会签");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getActionOwnerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getActionOwnerName()));
            return;
        }
        if (STATUS_RESULT_UPLOADER_ASSIGN.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_RESULT_UPLOADER_ASSIGN);
            updateObj.setCurrentNodeName("指定执行结果上传人");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getActionOwnerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getActionOwnerName()));
            return;
        }
        if (STATUS_RESULT_UPLOAD.equals(targetStatus)) {
            updateObj.setCurrentNodeCode(NODE_RESULT_UPLOAD);
            updateObj.setCurrentNodeName("执行结果上传");
            updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getResultUploaderId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getResultUploaderName()));
            return;
        }
        updateObj.setCurrentNodeCode(NODE_QA_CLOSURE);
        updateObj.setCurrentNodeName("品质部效果确认关闭");
        updateObj.setCurrentHandlerUserId(firstNonNull(taskAssigneeUserId, before.getQaConfirmerId(),
                before.getConfirmerId()));
        updateObj.setCurrentHandlerUserName(firstNotBlank(taskAssigneeUserName, before.getQaConfirmerName(),
                before.getConfirmerName()));
    }

    private void resetGroupTasksAfterWithdraw(Long exceptionId, String targetStatus, String reason) {
        LocalDateTime now = LocalDateTime.now();
        if (statusIn(targetStatus, STATUS_RETURNED, STATUS_CONFIRMING, STATUS_CONTAINMENT,
                STATUS_CONTAINMENT_SIGN)) {
            resetGroupTasksForReturn(exceptionId, GROUP_TASK_TYPE_INVESTIGATION, reason, now);
        }
        if (statusIn(targetStatus, STATUS_RETURNED, STATUS_CONFIRMING, STATUS_CONTAINMENT,
                STATUS_CONTAINMENT_SIGN, STATUS_RESPONSIBILITY, STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN)) {
            resetGroupTasksForReturn(exceptionId, GROUP_TASK_TYPE_ROOT_CAUSE, reason, now);
        }
    }

    private Task selectCurrentUserRunningTask(List<Task> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return null;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId != null) {
            for (Task task : tasks) {
                if (Objects.equals(parseTaskAssignee(task), loginUserId)) {
                    return task;
                }
            }
        }
        return tasks.get(0);
    }

    private String resolveReturnBpmTargetTaskDefinitionKey(String targetNodeCode) {
        if (NODE_CONTAINMENT.equals(targetNodeCode)) {
            return BPM_NODE_CONTAINMENT_OWNER_HANDLE;
        }
        if (NODE_CONTAINMENT_SIGN.equals(targetNodeCode)) {
            return BPM_NODE_CONTAINMENT_MEMBER_SIGN;
        }
        if (NODE_ROOT_CAUSE.equals(targetNodeCode)) {
            return BPM_NODE_ROOT_CAUSE_OWNER_HANDLE;
        }
        if (NODE_ROOT_CAUSE_SIGN.equals(targetNodeCode)) {
            return BPM_NODE_ROOT_CAUSE_MEMBER_SIGN;
        }
        if (NODE_RESULT_UPLOAD.equals(targetNodeCode)) {
            return BPM_NODE_RESULT_UPLOAD;
        }
        if (NODE_CONFIRMING.equals(targetNodeCode)) {
            return BPM_NODE_QUALITY_CONFIRM;
        }
        return BPM_NODE_START_USER;
    }

    private void approveRunningExceptionBpmTasks(QmsExceptionEventDO event, String taskDefinitionKey, String reason,
                                                 Map<String, List<Long>> nextAssignees) {
        List<Task> tasks = getRunningTasksByDefinitionKeyWithAliases(event.getProcessInstanceId(), taskDefinitionKey);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        Task task = selectCurrentUserRunningTask(tasks);
        Long assigneeUserId = parseTaskAssignee(task);
        if (assigneeUserId == null) {
            return;
        }
        BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                .setId(task.getId())
                .setReason(reason)
                .setVariables(buildExceptionBpmVariables(event));
        if (nextAssignees != null && !nextAssignees.isEmpty()) {
            reqVO.setNextAssignees(nextAssignees);
        }
        bpmTaskService.approveTask(assigneeUserId, reqVO);
    }

    private void approveCurrentMemberBpmTask(QmsExceptionEventDO event, QmsExceptionGroupTaskDO groupTask,
                                             Long assigneeUserId, String reason) {
        if (event == null || groupTask == null || assigneeUserId == null
                || StrUtil.isBlank(event.getProcessInstanceId())) {
            return;
        }
        String taskDefinitionKey = resolveGroupTaskBpmNode(groupTask.getTaskType());
        if (StrUtil.isBlank(taskDefinitionKey)) {
            return;
        }
        List<Task> tasks = getRunningTasksByDefinitionKeyWithAliases(event.getProcessInstanceId(), taskDefinitionKey);
        if (CollUtil.isEmpty(tasks) && isRootCauseGroupTask(groupTask)) {
            tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                    event.getProcessInstanceId(), true, BPM_NODE_QA_CLOSURE);
        }
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (Task runningTask : tasks) {
            if (!Objects.equals(parseTaskAssignee(runningTask), assigneeUserId)) {
                continue;
            }
            BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                    .setId(runningTask.getId())
                    .setReason(reason)
                    .setVariables(buildExceptionBpmVariables(event));
            Map<String, List<Long>> nextAssignees = BPM_NODE_QA_CLOSURE.equals(runningTask.getTaskDefinitionKey())
                    ? null : groupTaskApproveNextAssignees(event, groupTask);
            if (CollUtil.isNotEmpty(nextAssignees)) {
                reqVO.setNextAssignees(nextAssignees);
            }
            bpmTaskService.approveTask(assigneeUserId, reqVO);
        }
    }

    private Map<String, List<Long>> groupTaskApproveNextAssignees(QmsExceptionEventDO event,
                                                                  QmsExceptionGroupTaskDO groupTask) {
        if (event == null || groupTask == null) {
            return null;
        }
        String taskType = normalizeTaskType(groupTask.getTaskType());
        if (GROUP_TASK_TYPE_INVESTIGATION.equals(taskType)) {
            return nextAssignees(BPM_NODE_RESPONSIBILITY_CONFIRM,
                    buildResponsibilityConfirmBpmAssigneeUserIds(event));
        }
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            return nextAssignees(BPM_NODE_RESULT_UPLOADER_ASSIGN,
                    buildResultUploaderAssignBpmAssigneeUserIds(event));
        }
        return null;
    }

    private String resolveGroupTaskBpmNode(String taskType) {
        String normalizedTaskType = normalizeTaskType(taskType);
        if (GROUP_TASK_TYPE_INVESTIGATION.equals(normalizedTaskType)) {
            return BPM_NODE_CONTAINMENT_MEMBER_SIGN;
        }
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(normalizedTaskType)) {
            return BPM_NODE_ROOT_CAUSE_MEMBER_SIGN;
        }
        return null;
    }

    private boolean isRootCauseGroupTask(QmsExceptionGroupTaskDO groupTask) {
        return groupTask != null && GROUP_TASK_TYPE_ROOT_CAUSE.equals(normalizeTaskType(groupTask.getTaskType()));
    }

    private Map<String, Object> buildExceptionBpmVariables(QmsExceptionEventDO event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("exceptionEventId", event.getId());
        variables.put("exceptionNo", event.getExceptionNo());
        variables.put("exceptionType", event.getExceptionType());
        variables.put("exceptionLevel", event.getExceptionLevel());
        variables.put("status", event.getStatus());
        variables.put("currentHandlerUserId", event.getCurrentHandlerUserId());
        variables.put("containmentOwnerUserIds", buildContainmentOwnerBpmAssigneeUserIds(event));
        variables.put("containmentSignUserIds", buildContainmentSignBpmAssigneeUserIds(event));
        variables.put("responsibilityConfirmUserIds", buildResponsibilityConfirmBpmAssigneeUserIds(event));
        variables.put("rootCauseOwnerUserIds", buildRootCauseOwnerBpmAssigneeUserIds(event));
        variables.put("rootCauseSignUserIds", buildRootCauseSignBpmAssigneeUserIds(event));
        variables.put("resultUploaderAssignUserIds", buildResultUploaderAssignBpmAssigneeUserIds(event));
        variables.put("resultUploadUserIds", buildResultUploadBpmAssigneeUserIds(event));
        variables.put("qaClosureUserIds", buildQaClosureBpmAssigneeUserIds(event));
        return variables;
    }

    private Map<String, List<Long>> nextAssignees(String taskDefinitionKey, List<Long> userIds) {
        List<Long> assigneeUserIds = distinctUserIds(userIds);
        if (CollUtil.isEmpty(assigneeUserIds)) {
            return null;
        }
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        for (String key : taskDefinitionKeysWithAliases(taskDefinitionKey)) {
            nextAssignees.put(key, assigneeUserIds);
        }
        return nextAssignees;
    }

    private Map<String, List<Long>> containmentNextAssignees(QmsExceptionEventDO event) {
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        mergeNextAssignees(nextAssignees, BPM_NODE_CONTAINMENT_MEMBER_SIGN,
                buildContainmentSignBpmAssigneeUserIds(event));
        mergeNextAssignees(nextAssignees, BPM_NODE_RESPONSIBILITY_CONFIRM,
                buildResponsibilityConfirmBpmAssigneeUserIds(event));
        return nextAssignees.isEmpty() ? null : nextAssignees;
    }

    private Map<String, List<Long>> rootCauseNextAssignees(QmsExceptionEventDO event) {
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        List<Long> rootCauseSignUserIds = buildRootCauseSignBpmAssigneeUserIds(event);
        mergeNextAssignees(nextAssignees, BPM_NODE_ROOT_CAUSE_MEMBER_SIGN, rootCauseSignUserIds);
        mergeNextAssignees(nextAssignees, BPM_NODE_QA_CLOSURE,
                buildResultUploaderAssignBpmAssigneeUserIds(event));
        return nextAssignees.isEmpty() ? null : nextAssignees;
    }

    private void mergeNextAssignees(Map<String, List<Long>> target, String taskDefinitionKey, List<Long> userIds) {
        Map<String, List<Long>> source = nextAssignees(taskDefinitionKey, userIds);
        if (CollUtil.isEmpty(source)) {
            return;
        }
        target.putAll(source);
    }

    private List<Long> buildContainmentOwnerBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null) {
            return List.of();
        }
        return singleUserId(firstNonNull(event.getContainmentOwnerId(), event.getCurrentHandlerUserId()));
    }

    private List<Long> buildContainmentSignBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null || event.getId() == null) {
            return List.of();
        }
        List<QmsExceptionGroupTaskDO> investigationTasks = qmsExceptionGroupTaskMapper
                .selectListByExceptionId(event.getId()).stream()
                .filter(task -> GROUP_TASK_TYPE_INVESTIGATION.equals(normalizeTaskType(task.getTaskType())))
                .filter(task -> !GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(investigationTasks)) {
            return currentHandlerUserIds(event);
        }
        List<Long> userIds = new ArrayList<>();
        for (QmsExceptionGroupTaskDO task : investigationTasks) {
            qmsExceptionGroupTaskMemberMapper.selectListByTaskId(task.getId()).stream()
                    .filter(this::isPendingMemberConfirm)
                    .map(QmsExceptionGroupTaskMemberDO::getUserId)
                    .forEach(userIds::add);
        }
        List<Long> assigneeUserIds = distinctUserIds(userIds);
        return CollUtil.isEmpty(assigneeUserIds) ? currentHandlerUserIds(event) : assigneeUserIds;
    }

    private List<Long> buildResponsibilityConfirmBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null) {
            return List.of();
        }
        List<Long> userIds = singleUserId(firstNonNull(event.getContainmentOwnerId(), event.getCurrentHandlerUserId()));
        return CollUtil.isNotEmpty(userIds) ? userIds
                : groupTaskExecutorUserIds(event.getId(), GROUP_TASK_TYPE_INVESTIGATION);
    }

    private List<Long> buildRootCauseOwnerBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null) {
            return List.of();
        }
        return singleUserId(firstNonNull(event.getActionOwnerId(), event.getCurrentHandlerUserId()));
    }

    private List<Long> buildRootCauseSignBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null || event.getId() == null) {
            return List.of();
        }
        List<QmsExceptionGroupTaskDO> rootCauseTasks = qmsExceptionGroupTaskMapper.selectListByExceptionId(event.getId()).stream()
                .filter(task -> GROUP_TASK_TYPE_ROOT_CAUSE.equals(normalizeTaskType(task.getTaskType())))
                .filter(task -> !GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus()))
                .collect(Collectors.toList());
        List<Long> userIds = new ArrayList<>();
        for (QmsExceptionGroupTaskDO task : rootCauseTasks) {
            qmsExceptionGroupTaskMemberMapper.selectListByTaskId(task.getId()).stream()
                    .filter(this::isPendingMemberConfirm)
                    .map(QmsExceptionGroupTaskMemberDO::getUserId)
                    .forEach(userIds::add);
        }
        List<Long> assigneeUserIds = distinctUserIds(userIds);
        return CollUtil.isEmpty(assigneeUserIds) ? currentHandlerUserIds(event) : assigneeUserIds;
    }

    private List<Long> buildResultUploaderAssignBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null) {
            return List.of();
        }
        List<Long> userIds = singleUserId(firstNonNull(event.getActionOwnerId(), event.getCurrentHandlerUserId()));
        return CollUtil.isNotEmpty(userIds) ? userIds
                : groupTaskExecutorUserIds(event.getId(), GROUP_TASK_TYPE_ROOT_CAUSE);
    }

    private List<Long> groupTaskExecutorUserIds(Long exceptionId, String taskType) {
        if (exceptionId == null || StrUtil.isBlank(taskType)) {
            return List.of();
        }
        return distinctUserIds(qmsExceptionGroupTaskMapper.selectListByExceptionId(exceptionId).stream()
                .filter(task -> taskType.equals(normalizeTaskType(task.getTaskType())))
                .map(QmsExceptionGroupTaskDO::getExecutorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private List<Long> buildResultUploadBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null) {
            return List.of();
        }
        return singleUserId(firstNonNull(event.getResultUploaderId(), event.getCurrentHandlerUserId()));
    }

    private List<Long> buildQaClosureBpmAssigneeUserIds(QmsExceptionEventDO event) {
        if (event == null) {
            return List.of();
        }
        return singleUserId(firstNonNull(event.getQaConfirmerId(), event.getConfirmerId(),
                event.getCurrentHandlerUserId()));
    }

    private List<Long> currentHandlerUserIds(QmsExceptionEventDO event) {
        return event == null || event.getCurrentHandlerUserId() == null
                ? List.of() : List.of(event.getCurrentHandlerUserId());
    }

    private List<Long> distinctUserIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> singleUserId(Long userId) {
        return userId == null ? List.of() : List.of(userId);
    }

    private Long parseTaskAssignee(Task task) {
        return task == null ? null : parseLongOrNull(task.getAssignee());
    }

    private void syncCurrentHandlerFromRunningBpmTask(QmsExceptionEventDO event, String taskDefinitionKey,
                                                      String nodeCode, String nodeName) {
        if (event == null || event.getId() == null || StrUtil.isBlank(event.getProcessInstanceId())) {
            return;
        }
        List<Task> tasks = getRunningTasksByDefinitionKeyWithAliases(event.getProcessInstanceId(), taskDefinitionKey);
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        Long assigneeUserId = parseTaskAssignee(tasks.get(0));
        if (assigneeUserId == null) {
            return;
        }
        AdminUserRespDTO user = adminUserApi.getUser(assigneeUserId);
        String handlerUserName = user == null
                ? String.valueOf(assigneeUserId)
                : firstNotBlank(user.getNickname(), String.valueOf(assigneeUserId));

        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(event.getId());
        updateObj.setCurrentNodeCode(nodeCode);
        updateObj.setCurrentNodeName(nodeName);
        updateObj.setCurrentHandlerUserId(assigneeUserId);
        updateObj.setCurrentHandlerUserName(handlerUserName);
        if (BPM_NODE_QUALITY_CONFIRM.equals(taskDefinitionKey)) {
            updateObj.setConfirmerId(assigneeUserId);
            updateObj.setConfirmerCode(defaultCode(null, assigneeUserId));
            updateObj.setConfirmerName(handlerUserName);
            Long deptId = user == null ? null : user.getDeptId();
            if (deptId != null) {
                DeptRespDTO dept = deptApi.getDept(deptId);
                updateObj.setConfirmDeptId(deptId);
                updateObj.setConfirmDeptCode(defaultCode(null, deptId));
                updateObj.setConfirmDeptName(dept == null ? String.valueOf(deptId) : dept.getName());
            }
        }
        qmsExceptionEventMapper.updateById(updateObj);
    }

    private Long parseLongOrNull(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean statusIn(String status, String... candidates) {
        for (String candidate : candidates) {
            if (Objects.equals(status, candidate)) {
                return true;
            }
        }
        return false;
    }

    private void applyNextNode(QmsExceptionEventDO before, QmsExceptionEventDO updateObj,
                               QmsExceptionEventHandleReqVO handleReqVO, LocalDateTime now) {
        String currentStatus = before.getStatus();
        if (STATUS_RETURNED.equals(currentStatus)) {
            updateObj.setStatus(STATUS_CONFIRMING);
            updateObj.setCurrentNodeCode(NODE_CONFIRMING);
            updateObj.setCurrentNodeName("确认");
            updateObj.setCurrentHandlerUserId(null);
            updateObj.setCurrentHandlerUserName(null);
            return;
        }
        if (STATUS_CONFIRMING.equals(currentStatus)) {
            if (handleReqVO.getNextHandlerUserId() == null) {
                throw exception(QMS_EXCEPTION_EVENT_CONTAINMENT_OWNER_REQUIRED);
            }
            updateObj.setStatus(STATUS_CONTAINMENT);
            updateObj.setCurrentNodeCode(NODE_CONTAINMENT);
            updateObj.setCurrentNodeName("围堵措施办理");
            updateObj.setConfirmTime(now);
            updateObj.setConfirmerId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setConfirmerCode(defaultCode(null, SecurityFrameworkUtils.getLoginUserId()));
            updateObj.setConfirmerName(currentUserName());
            updateObj.setConfirmDeptCode(defaultCode(updateObj.getConfirmDeptCode(), updateObj.getConfirmDeptId()));
            updateObj.setContainmentOwnerId(handleReqVO.getNextHandlerUserId());
            updateObj.setContainmentOwnerName(firstNotBlank(handleReqVO.getNextHandlerUserName(),
                    resolveUserName(handleReqVO.getNextHandlerUserId()), String.valueOf(handleReqVO.getNextHandlerUserId())));
            updateObj.setQaConfirmerId(SecurityFrameworkUtils.getLoginUserId());
            updateObj.setQaConfirmerName(currentUserName());
            updateObj.setContainmentDeadline(firstNonNull(updateObj.getContainmentDeadline(),
                    before.getContainmentDeadline()));
            updateObj.setCurrentHandlerUserId(updateObj.getContainmentOwnerId());
            updateObj.setCurrentHandlerUserName(updateObj.getContainmentOwnerName());
            return;
        } else if (STATUS_CONTAINMENT.equals(currentStatus)) {
            validateGroupTasksReady(handleReqVO.getGroupTasks(), GROUP_TASK_TYPE_INVESTIGATION,
                    QMS_EXCEPTION_EVENT_CONTAINMENT_MEMBER_REQUIRED);
            if (StrUtil.isBlank(updateObj.getContainmentAction())) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED);
            }
            updateObj.setStatus(STATUS_CONTAINMENT_SIGN);
            updateObj.setCurrentNodeCode(NODE_CONTAINMENT_SIGN);
            updateObj.setCurrentNodeName("临时小组围堵措施会签");
            updateObj.setContainmentFinishTime(now);
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getContainmentOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getContainmentOwnerName(), before.getCurrentHandlerUserName()));
            return;
        } else if (STATUS_RESPONSIBILITY.equals(currentStatus)) {
            if (handleReqVO.getNextHandlerUserId() == null) {
                throw exception(QMS_EXCEPTION_EVENT_RESPONSIBILITY_OWNER_REQUIRED);
            }
            updateObj.setStatus(STATUS_ROOT_CAUSE);
            updateObj.setCurrentNodeCode(NODE_ROOT_CAUSE);
            updateObj.setCurrentNodeName("根因分析和纠正预防措施办理");
            updateObj.setActionOwnerId(handleReqVO.getNextHandlerUserId());
            updateObj.setActionOwnerName(firstNotBlank(handleReqVO.getNextHandlerUserName(),
                    resolveUserName(handleReqVO.getNextHandlerUserId()), String.valueOf(handleReqVO.getNextHandlerUserId())));
            updateObj.setCurrentHandlerUserId(updateObj.getActionOwnerId());
            updateObj.setCurrentHandlerUserName(updateObj.getActionOwnerName());
            return;
        } else if (STATUS_ROOT_CAUSE.equals(currentStatus)) {
            validateGroupTasksReady(handleReqVO.getGroupTasks(), GROUP_TASK_TYPE_ROOT_CAUSE,
                    QMS_EXCEPTION_EVENT_ROOT_CAUSE_MEMBER_REQUIRED);
            if (StrUtil.isBlank(updateObj.getRootCause()) && StrUtil.isBlank(updateObj.getPreventiveAction())) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED);
            }
            updateObj.setStatus(STATUS_ROOT_CAUSE_SIGN);
            updateObj.setCurrentNodeCode(NODE_ROOT_CAUSE_SIGN);
            updateObj.setCurrentNodeName("根因分析纠正预防会签");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getActionOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getActionOwnerName(), before.getCurrentHandlerUserName()));
            return;
        } else if (STATUS_RESULT_UPLOADER_ASSIGN.equals(currentStatus)) {
            if (handleReqVO.getNextHandlerUserId() == null) {
                throw exception(QMS_EXCEPTION_EVENT_RESULT_UPLOADER_REQUIRED);
            }
            updateObj.setStatus(STATUS_RESULT_UPLOAD);
            updateObj.setCurrentNodeCode(NODE_RESULT_UPLOAD);
            updateObj.setCurrentNodeName("执行结果上传");
            updateObj.setResultUploaderId(handleReqVO.getNextHandlerUserId());
            updateObj.setResultUploaderName(firstNotBlank(handleReqVO.getNextHandlerUserName(),
                    resolveUserName(handleReqVO.getNextHandlerUserId()), String.valueOf(handleReqVO.getNextHandlerUserId())));
            updateObj.setCurrentHandlerUserId(updateObj.getResultUploaderId());
            updateObj.setCurrentHandlerUserName(updateObj.getResultUploaderName());
            return;
        } else if (STATUS_RESULT_UPLOAD.equals(currentStatus)) {
            if (StrUtil.isBlank(updateObj.getCorrectivePreventiveResult())) {
                throw exception(QMS_EXCEPTION_EVENT_RESULT_REQUIRED);
            }
            updateObj.setResultUploadTime(firstNonNull(updateObj.getResultUploadTime(), now));
            updateObj.setStatus(STATUS_QA_CLOSURE);
            updateObj.setCurrentNodeCode(NODE_QA_CLOSURE);
            updateObj.setCurrentNodeName("品质部门闭环确认");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getQaConfirmerId(), before.getConfirmerId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getQaConfirmerName(), before.getConfirmerName()));
            updateObj.setCopyUserIds(joinLongIds(handleReqVO.getCopyToUserIds()));
            updateObj.setCopyUserNames(joinNames(handleReqVO.getCopyToUserNames()));
            return;
        } else if (STATUS_VERIFYING.equals(currentStatus) || STATUS_QA_CLOSURE.equals(currentStatus)) {
            updateObj.setStatus(STATUS_QA_CLOSURE);
            updateObj.setCurrentNodeCode(NODE_QA_CLOSURE);
            updateObj.setCurrentNodeName("品质部门闭环确认");
        }
        Long fallbackHandlerUserId = STATUS_CONTAINMENT.equals(currentStatus)
                ? firstNonNull(before.getConfirmerId(), before.getCurrentHandlerUserId())
                : before.getCurrentHandlerUserId();
        String fallbackHandlerUserName = STATUS_CONTAINMENT.equals(currentStatus)
                ? firstNotBlank(before.getConfirmerName(), before.getCurrentHandlerUserName(), currentUserName())
                : before.getCurrentHandlerUserName();
        updateObj.setCurrentHandlerUserId(handleReqVO.getNextHandlerUserId() != null
                ? handleReqVO.getNextHandlerUserId() : fallbackHandlerUserId);
        updateObj.setCurrentHandlerUserName(firstNotBlank(handleReqVO.getNextHandlerUserName(),
                fallbackHandlerUserName, currentUserName()));
    }

    private QmsExceptionEventDO validateExceptionExists(Long id) {
        QmsExceptionEventDO event = qmsExceptionEventMapper.selectById(id);
        if (event == null) {
            throw exception(QMS_EXCEPTION_EVENT_NOT_EXISTS);
        }
        return event;
    }

    private QmsExceptionEventDO validateExceptionCanFlow(Long id) {
        QmsExceptionEventDO event = validateExceptionExists(id);
        if (STATUS_CLOSED.equals(event.getStatus()) || STATUS_CANCELLED.equals(event.getStatus())) {
            throw exception(QMS_EXCEPTION_EVENT_CLOSED);
        }
        return event;
    }

    private QmsExceptionGroupTaskDO validateGroupTaskExists(Long id) {
        QmsExceptionGroupTaskDO task = qmsExceptionGroupTaskMapper.selectById(id);
        if (task == null) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NOT_EXISTS);
        }
        return task;
    }

    private QmsExceptionGroupTaskMemberDO selectCurrentHandlerGroupTaskMember(Long taskId, Long memberId,
                                                                              Long loginUserId) {
        if (taskId == null || loginUserId == null) {
            return null;
        }
        if (memberId != null) {
            QmsExceptionGroupTaskMemberDO member = qmsExceptionGroupTaskMemberMapper.selectById(memberId);
            if (member == null || !Objects.equals(member.getTaskId(), taskId)
                    || !isGroupTaskMemberHandler(member, loginUserId)) {
                return null;
            }
            return member;
        }
        return qmsExceptionGroupTaskMemberMapper.selectByTaskIdAndUserId(taskId, loginUserId);
    }

    private void validateCurrentHandlerOrDispatcher(QmsExceptionEventDO event, QmsExceptionGroupTaskDO task) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        boolean currentHandler = loginUserId != null && Objects.equals(event.getCurrentHandlerUserId(), loginUserId);
        boolean dispatcher = loginUserId != null && (task == null
                ? qmsExceptionGroupTaskMapper.selectListByExceptionId(event.getId()).stream()
                        .anyMatch(groupTask -> Objects.equals(groupTask.getDispatcherUserId(), loginUserId))
                : Objects.equals(task.getDispatcherUserId(), loginUserId));
        if (!currentHandler && !dispatcher) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NO_PERMISSION);
        }
    }

    private void saveInitialGroupTasks(QmsExceptionEventDO event, List<QmsExceptionGroupTaskReqVO> groupTasks,
                                       LocalDateTime now) {
        if (CollUtil.isEmpty(groupTasks)) {
            return;
        }
        for (int i = 0; i < groupTasks.size(); i++) {
            saveGroupTask(event, groupTasks.get(i), i + 1, now);
        }
        refreshContainmentDeadlineFromGroupTasks(event.getId());
    }

    private void saveGroupTask(QmsExceptionEventDO event, QmsExceptionGroupTaskReqVO taskReqVO,
                               int sortNo, LocalDateTime now) {
        QmsExceptionGroupTaskDO existing = taskReqVO.getId() == null
                ? null : validateGroupTaskExists(taskReqVO.getId());
        if (existing != null && !Objects.equals(existing.getExceptionId(), event.getId())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_NOT_EXISTS);
        }
        if (existing != null && statusIn(existing.getTaskStatus(), GROUP_TASK_STATUS_SUBMITTED,
                GROUP_TASK_STATUS_SIGNED, GROUP_TASK_STATUS_ACCEPTED)) {
            return;
        }
        validateTaskAssignment(event, taskReqVO);
        String taskType = normalizeTaskType(taskReqVO.getTaskType());
        QmsExceptionGroupTaskDO task = BeanUtils.toBean(taskReqVO, QmsExceptionGroupTaskDO.class);
        task.setExceptionId(event.getId());
        task.setExceptionNo(event.getExceptionNo());
        task.setTaskType(taskType);
        task.setGroupName(firstNotBlank(taskReqVO.getGroupName(), defaultTaskName(taskType, sortNo)));
        task.setDispatcherUserId(SecurityFrameworkUtils.getLoginUserId());
        task.setDispatcherUserName(currentUserName());
        task.setExecutorUserId(resolveReqTaskExecutorId(taskReqVO));
        task.setExecutorUserName(resolveReqTaskExecutorName(taskReqVO));
        if (task.getExecutorUserId() == null && GROUP_TASK_TYPE_INVESTIGATION.equals(taskType)) {
            task.setExecutorUserId(firstNonNull(event.getContainmentOwnerId(), event.getCurrentHandlerUserId()));
            task.setExecutorUserName(firstNotBlank(event.getContainmentOwnerName(), event.getCurrentHandlerUserName()));
        }
        if (task.getExecutorUserId() == null && GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            task.setExecutorUserId(firstNonNull(event.getActionOwnerId(), event.getCurrentHandlerUserId()));
            task.setExecutorUserName(firstNotBlank(event.getActionOwnerName(), event.getCurrentHandlerUserName()));
        }
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            task.setRootCauseOwnerId(task.getExecutorUserId());
            task.setRootCauseOwnerName(task.getExecutorUserName());
        }
        task.setDispatchTime(now);
        task.setReplyCount(existing == null ? 0 : firstNonNull(existing.getReplyCount(), 0));
        task.setAttachmentUrls(cleanStringList(taskReqVO.getAttachmentUrls()));
        task.setTaskStatus(existing != null
                && statusIn(existing.getTaskStatus(), GROUP_TASK_STATUS_SUBMITTED,
                GROUP_TASK_STATUS_SIGNED, GROUP_TASK_STATUS_ACCEPTED)
                ? existing.getTaskStatus() : GROUP_TASK_STATUS_DISPATCHED);
        if (existing == null) {
            qmsExceptionGroupTaskMapper.insert(task);
        } else {
            task.setId(existing.getId());
            qmsExceptionGroupTaskMapper.updateById(task);
        }
        saveGroupTaskMembers(event.getId(), task.getId(), taskReqVO.getMembers());
    }

    private void validateTaskAssignment(QmsExceptionEventDO event, QmsExceptionGroupTaskReqVO taskReqVO) {
        String taskType = normalizeTaskType(taskReqVO.getTaskType());
        validateTaskConfirmers(taskReqVO.getMembers());
        boolean hasMembers = CollUtil.isNotEmpty(taskReqVO.getMembers()) && taskReqVO.getMembers().stream()
                .anyMatch(member -> member.getUserId() != null || StrUtil.isNotBlank(member.getUserName()));
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            if (!hasMembers && STATUS_ROOT_CAUSE.equals(event.getStatus())) {
                return;
            }
            if (!hasMembers) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_ASSIGNEE_REQUIRED);
            }
            return;
        }
        if (!hasMembers) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_ASSIGNEE_REQUIRED);
        }
    }

    private void validateTaskConfirmers(List<QmsExceptionGroupTaskMemberReqVO> members) {
        if (CollUtil.isEmpty(members)) {
            return;
        }
        Set<String> userKeys = new HashSet<>();
        for (QmsExceptionGroupTaskMemberReqVO member : members) {
            boolean hasUser = member.getUserId() != null || StrUtil.isNotBlank(member.getUserName());
            if (!hasUser) {
                continue;
            }
            String userKey = member.getUserId() != null
                    ? String.valueOf(member.getUserId()) : member.getUserName();
            if (!userKeys.add(userKey)) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_ASSIGNEE_REQUIRED);
            }
        }
    }

    private void validateTaskSubmitContent(QmsExceptionGroupTaskDO task, QmsExceptionGroupTaskSubmitReqVO submitReqVO) {
        String taskType = normalizeTaskType(task.getTaskType());
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            if (StrUtil.isBlank(submitReqVO.getRootCause()) && StrUtil.isBlank(submitReqVO.getPreventiveAction())) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED);
            }
            return;
        }
        if (StrUtil.isBlank(submitReqVO.getActionDescription())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED);
        }
    }

    private void validateMemberSubmitContent(QmsExceptionGroupTaskDO task, QmsExceptionGroupTaskMemberConfirmReqVO confirmReqVO) {
        String taskType = normalizeTaskType(task.getTaskType());
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            if (StrUtil.isBlank(confirmReqVO.getActionDescription()) && StrUtil.isBlank(confirmReqVO.getRemark())) {
                throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED);
            }
            return;
        }
        if (StrUtil.isBlank(confirmReqVO.getActionDescription())) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_SUBMIT_CONTENT_REQUIRED);
        }
    }

    private QmsExceptionGroupTaskDO buildMemberSubmitTaskUpdate(QmsExceptionGroupTaskDO task,
                                                                QmsExceptionGroupTaskMemberDO member,
                                                                QmsExceptionGroupTaskMemberConfirmReqVO confirmReqVO,
                                                                String taskStatus, LocalDateTime now) {
        QmsExceptionGroupTaskDO updateObj = new QmsExceptionGroupTaskDO();
        updateObj.setId(task.getId());
        updateObj.setTaskStatus(taskStatus);
        updateObj.setActualFinishTime(firstNonNull(confirmReqVO.getActualFinishTime(), now));
        updateObj.setActionDescription(firstNotBlank(confirmReqVO.getActionDescription(), confirmReqVO.getRemark()));
        updateObj.setRootCauseCategory(confirmReqVO.getRootCauseCategory());
        updateObj.setRootCause(confirmReqVO.getRootCause());
        updateObj.setPreventiveAction(confirmReqVO.getPreventiveAction());
        updateObj.setAttachmentUrls(cleanStringList(confirmReqVO.getAttachmentUrls()));
        updateObj.setSubmitterUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setSubmitterUserName(buildMemberActualHandlerDisplay(member, SecurityFrameworkUtils.getLoginUserId()));
        updateObj.setSubmitTime(now);
        updateObj.setReplyCount(firstNonNull(task.getReplyCount(), 0) + 1);
        return updateObj;
    }

    private void saveGroupTaskReply(QmsExceptionGroupTaskDO task, QmsExceptionGroupTaskSubmitReqVO submitReqVO,
                                    Integer replyNo, LocalDateTime now) {
        qmsExceptionGroupTaskReplyMapper.insert(QmsExceptionGroupTaskReplyDO.builder()
                .exceptionId(task.getExceptionId())
                .taskId(task.getId())
                .taskType(normalizeTaskType(task.getTaskType()))
                .replyNo(replyNo)
                .replyStatus(GROUP_TASK_STATUS_SUBMITTED)
                .actualFinishTime(firstNonNull(submitReqVO.getActualFinishTime(), now))
                .actionDescription(submitReqVO.getActionDescription())
                .rootCauseCategory(submitReqVO.getRootCauseCategory())
                .rootCause(submitReqVO.getRootCause())
                .preventiveAction(submitReqVO.getPreventiveAction())
                .attachmentUrls(cleanStringList(submitReqVO.getAttachmentUrls()))
                .submitterUserId(SecurityFrameworkUtils.getLoginUserId())
                .submitterUserName(currentUserName())
                .submitTime(now)
                .remark(submitReqVO.getOpinion())
                .build());
    }

    private void saveGroupTaskMemberReply(QmsExceptionGroupTaskDO task,
                                          QmsExceptionGroupTaskMemberDO member,
                                          QmsExceptionGroupTaskMemberConfirmReqVO confirmReqVO,
                                          Integer replyNo, String replyStatus, LocalDateTime now) {
        qmsExceptionGroupTaskReplyMapper.insert(QmsExceptionGroupTaskReplyDO.builder()
                .exceptionId(task.getExceptionId())
                .taskId(task.getId())
                .taskType(normalizeTaskType(task.getTaskType()))
                .replyNo(replyNo)
                .replyStatus(replyStatus)
                .memberId(member == null ? null : member.getId())
                .memberUserId(member == null ? null : member.getUserId())
                .memberUserName(member == null ? null : member.getUserName())
                .actualFinishTime(firstNonNull(confirmReqVO.getActualFinishTime(), now))
                .actionDescription(firstNotBlank(confirmReqVO.getActionDescription(), confirmReqVO.getRemark()))
                .rootCauseCategory(confirmReqVO.getRootCauseCategory())
                .rootCause(confirmReqVO.getRootCause())
                .preventiveAction(confirmReqVO.getPreventiveAction())
                .attachmentUrls(cleanStringList(confirmReqVO.getAttachmentUrls()))
                .submitterUserId(SecurityFrameworkUtils.getLoginUserId())
                .submitterUserName(currentUserName())
                .submitTime(now)
                .remark(confirmReqVO.getRemark())
                .build());
    }

    private void reviewLatestTaskReply(Long taskId, QmsExceptionGroupTaskDO taskReview) {
        QmsExceptionGroupTaskReplyDO latestReply = qmsExceptionGroupTaskReplyMapper.selectLatestByTaskId(taskId);
        if (latestReply == null) {
            return;
        }
        QmsExceptionGroupTaskReplyDO updateReply = new QmsExceptionGroupTaskReplyDO();
        updateReply.setId(latestReply.getId());
        updateReply.setReplyStatus(taskReview.getTaskStatus());
        updateReply.setReviewerUserId(taskReview.getReviewerUserId());
        updateReply.setReviewerUserName(taskReview.getReviewerUserName());
        updateReply.setReviewTime(taskReview.getReviewTime());
        updateReply.setReviewOpinion(taskReview.getReviewOpinion());
        qmsExceptionGroupTaskReplyMapper.updateById(updateReply);
    }

    private String normalizeTaskType(String taskType) {
        return GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)
                ? GROUP_TASK_TYPE_ROOT_CAUSE : GROUP_TASK_TYPE_INVESTIGATION;
    }

    private String defaultTaskName(String taskType, int sortNo) {
        return GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)
                ? "责任部门" : "临时小组";
    }

    private Long resolveReqTaskExecutorId(QmsExceptionGroupTaskReqVO taskReqVO) {
        if (taskReqVO == null) {
            return null;
        }
        return firstNonNull(taskReqVO.getExecutorUserId(), taskReqVO.getRootCauseOwnerId());
    }

    private String resolveReqTaskExecutorName(QmsExceptionGroupTaskReqVO taskReqVO) {
        if (taskReqVO == null) {
            return null;
        }
        return firstNotBlank(taskReqVO.getExecutorUserName(), taskReqVO.getRootCauseOwnerName());
    }

    private void saveGroupTaskMembers(Long exceptionId, Long taskId, List<QmsExceptionGroupTaskMemberReqVO> members) {
        qmsExceptionGroupTaskMemberMapper.deleteByTaskId(taskId);
        if (CollUtil.isEmpty(members)) {
            return;
        }
        List<QmsExceptionGroupTaskMemberDO> memberDOs = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            QmsExceptionGroupTaskMemberReqVO member = members.get(i);
            if (member.getUserId() == null && StrUtil.isBlank(member.getUserName())) {
                continue;
            }
            memberDOs.add(QmsExceptionGroupTaskMemberDO.builder()
                    .exceptionId(exceptionId)
                    .taskId(taskId)
                    .deptId(member.getDeptId())
                    .deptName(firstNotBlank(member.getDeptName(), "未指定部门"))
                    .userId(member.getUserId())
                    .userName(firstNotBlank(member.getUserName(), "未指定成员"))
                    .memberRole(firstNotBlank(member.getMemberRole(), "MEMBER"))
                    .sortNo(member.getSortNo() != null ? member.getSortNo() : i + 1)
                    .confirmStatus(GROUP_MEMBER_CONFIRM_PENDING)
                    .overdueFlag(false)
                    .build());
        }
        if (CollUtil.isNotEmpty(memberDOs)) {
            qmsExceptionGroupTaskMemberMapper.insertBatch(memberDOs);
        }
    }

    private void markSubmitterGroupTaskMember(Long taskId, Long userId, LocalDateTime now) {
        QmsExceptionGroupTaskMemberDO member =
                qmsExceptionGroupTaskMemberMapper.selectByTaskIdAndUserId(taskId, userId);
        if (member == null) {
            return;
        }
        QmsExceptionGroupTaskMemberDO updateObj = new QmsExceptionGroupTaskMemberDO();
        updateObj.setId(member.getId());
        updateObj.setConfirmStatus(GROUP_MEMBER_CONFIRM_SUBMITTED);
        updateObj.setConfirmTime(now);
        updateObj.setActualHandlerUserId(userId);
        updateObj.setActualHandlerUserName(currentUserName());
        updateObj.setConfirmRemark(buildMemberHandleReason(member, userId, "本人提交小组措施"));
        updateObj.setOverdueFlag(false);
        qmsExceptionGroupTaskMemberMapper.updateById(updateObj);
    }

    private void resetGroupTaskMemberConfirmStatus(Long taskId) {
        qmsExceptionGroupTaskMemberMapper.resetConfirmStatusByTaskId(taskId, GROUP_MEMBER_CONFIRM_PENDING);
    }

    private void resetGroupTasksForReturn(Long exceptionId, String taskType, String opinion, LocalDateTime now) {
        qmsExceptionGroupTaskMapper.selectListByExceptionId(exceptionId).stream()
                .filter(task -> taskType.equals(normalizeTaskType(task.getTaskType())))
                .forEach(task -> {
                    QmsExceptionGroupTaskDO updateObj = new QmsExceptionGroupTaskDO();
                    updateObj.setId(task.getId());
                    updateObj.setTaskStatus(GROUP_TASK_STATUS_RETURNED);
                    updateObj.setReviewerUserId(SecurityFrameworkUtils.getLoginUserId());
                    updateObj.setReviewerUserName(currentUserName());
                    updateObj.setReviewTime(now);
                    updateObj.setReviewOpinion(firstNotBlank(opinion, "退回重新会签"));
                    qmsExceptionGroupTaskMapper.updateById(updateObj);
                    reviewLatestTaskReply(task.getId(), updateObj);
                    resetGroupTaskMemberConfirmStatus(task.getId());
                });
    }

    private void validateGroupTaskActiveForCurrentNode(QmsExceptionEventDO event, QmsExceptionGroupTaskDO task) {
        if (!isGroupTaskActiveForEvent(event, task)) {
            throw exception(QMS_EXCEPTION_EVENT_GROUP_TASK_STATUS_INVALID);
        }
    }

    private boolean isGroupTaskActiveForEvent(QmsExceptionEventDO event, QmsExceptionGroupTaskDO task) {
        if (event == null || task == null) {
            return false;
        }
        String taskType = normalizeTaskType(task.getTaskType());
        if (GROUP_TASK_TYPE_INVESTIGATION.equals(taskType)) {
            return statusIn(event.getStatus(), STATUS_CONTAINMENT, STATUS_CONTAINMENT_SIGN);
        }
        if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)) {
            return statusIn(event.getStatus(), STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN);
        }
        return false;
    }

    private boolean allTaskMembersConfirmed(Long taskId) {
        List<QmsExceptionGroupTaskMemberDO> members = qmsExceptionGroupTaskMemberMapper.selectListByTaskId(taskId);
        return CollUtil.isNotEmpty(members) && members.stream().allMatch(member ->
                GROUP_MEMBER_CONFIRM_CONFIRMED.equals(firstNotBlank(
                        member.getConfirmStatus(), GROUP_MEMBER_CONFIRM_PENDING)));
    }

    private QmsExceptionGroupTaskDO buildTaskReviewUpdate(QmsExceptionGroupTaskDO task, String taskStatus,
                                                          LocalDateTime now, String opinion) {
        QmsExceptionGroupTaskDO updateObj = new QmsExceptionGroupTaskDO();
        updateObj.setId(task.getId());
        updateObj.setTaskStatus(taskStatus);
        updateObj.setReviewerUserId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setReviewerUserName(currentUserName());
        updateObj.setReviewTime(now);
        updateObj.setReviewOpinion(opinion);
        return updateObj;
    }

    private void saveGroupTaskConfirmLog(QmsExceptionEventDO event, QmsExceptionGroupTaskDO task,
                                         QmsExceptionGroupTaskMemberDO member, String actionCode,
                                         String confirmStatus, String opinion,
                                         String taskStatusBefore, String taskStatusAfter, LocalDateTime now) {
        QmsExceptionGroupTaskReplyDO latestReply = qmsExceptionGroupTaskReplyMapper.selectLatestByTaskId(task.getId());
        qmsExceptionGroupTaskConfirmLogMapper.insert(QmsExceptionGroupTaskConfirmLogDO.builder()
                .exceptionId(event.getId())
                .exceptionNo(event.getExceptionNo())
                .taskId(task.getId())
                .taskType(normalizeTaskType(task.getTaskType()))
                .replyId(latestReply == null ? null : latestReply.getId())
                .replyNo(latestReply == null ? null : latestReply.getReplyNo())
                .memberId(member.getId())
                .userId(firstNonNull(member.getActualHandlerUserId(), member.getUserId()))
                .userName(firstNotBlank(member.getActualHandlerUserName(), member.getUserName()))
                .confirmAction(actionCode)
                .confirmStatus(confirmStatus)
                .confirmOpinion(opinion)
                .confirmTime(now)
                .taskStatusBefore(taskStatusBefore)
                .taskStatusAfter(taskStatusAfter)
                .tenantId(task.getTenantId())
                .build());
    }

    private boolean isGroupTaskAssignee(QmsExceptionGroupTaskDO task, Long loginUserId) {
        if (loginUserId == null) {
            return false;
        }
        return Objects.equals(effectiveTaskExecutorId(task), loginUserId);
    }

    private Long effectiveTaskExecutorId(QmsExceptionGroupTaskDO task) {
        if (task == null) {
            return null;
        }
        return firstNonNull(task.getExecutorUserId(), task.getRootCauseOwnerId());
    }

    private void refreshContainmentDeadlineFromGroupTasks(Long exceptionId) {
        LocalDateTime deadline = qmsExceptionGroupTaskMapper.selectListByExceptionId(exceptionId).stream()
                .filter(task -> GROUP_TASK_TYPE_INVESTIGATION.equals(normalizeTaskType(task.getTaskType())))
                .filter(task -> !GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus()))
                .map(QmsExceptionGroupTaskDO::getContainmentDeadline)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (deadline == null) {
            return;
        }
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(exceptionId);
        updateObj.setContainmentDeadline(deadline);
        qmsExceptionEventMapper.updateById(updateObj);
    }

    private void aggregateAcceptedGroupTasksToEvent(Long exceptionId) {
        List<QmsExceptionGroupTaskDO> acceptedTasks = qmsExceptionGroupTaskMapper.selectListByExceptionId(exceptionId)
                .stream()
                .filter(task -> GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(acceptedTasks)) {
            return;
        }
        List<QmsExceptionGroupTaskDO> investigationTasks = acceptedTasks.stream()
                .filter(task -> GROUP_TASK_TYPE_INVESTIGATION.equals(normalizeTaskType(task.getTaskType())))
                .collect(Collectors.toList());
        List<QmsExceptionGroupTaskDO> rootCauseTasks = acceptedTasks.stream()
                .filter(task -> GROUP_TASK_TYPE_ROOT_CAUSE.equals(normalizeTaskType(task.getTaskType())))
                .collect(Collectors.toList());
        Set<Long> acceptedTaskIds = acceptedTasks.stream()
                .map(QmsExceptionGroupTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<QmsExceptionGroupTaskMemberDO> acceptedMembers =
                qmsExceptionGroupTaskMemberMapper.selectListByTaskIds(acceptedTaskIds).stream()
                        .filter(member -> GROUP_MEMBER_CONFIRM_CONFIRMED.equals(member.getConfirmStatus()))
                        .collect(Collectors.toList());
        Set<Long> investigationTaskIds = investigationTasks.stream()
                .map(QmsExceptionGroupTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> rootCauseTaskIds = rootCauseTasks.stream()
                .map(QmsExceptionGroupTaskDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(exceptionId);
        String containmentAction = joinMemberText(acceptedMembers, investigationTaskIds, "ACTION");
        if (StrUtil.isBlank(containmentAction)) {
            containmentAction = joinTaskText(investigationTasks, true);
        }
        if (StrUtil.isNotBlank(containmentAction)) {
            updateObj.setContainmentAction(containmentAction);
        }
        investigationTasks.stream()
                .map(QmsExceptionGroupTaskDO::getActualFinishTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .ifPresent(updateObj::setContainmentFinishTime);
        acceptedMembers.stream()
                .filter(member -> rootCauseTaskIds.contains(member.getTaskId()))
                .map(QmsExceptionGroupTaskMemberDO::getRootCauseCategory)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .ifPresent(updateObj::setRootCauseCategory);
        String rootCause = joinMemberText(acceptedMembers, rootCauseTaskIds, "ROOT_CAUSE");
        if (StrUtil.isBlank(rootCause)) {
            rootCause = joinTaskText(rootCauseTasks, false);
        }
        if (StrUtil.isNotBlank(rootCause)) {
            updateObj.setRootCause(rootCause);
        }
        String preventiveAction = joinMemberText(acceptedMembers, rootCauseTaskIds, "PREVENTIVE");
        if (StrUtil.isBlank(preventiveAction)) {
            preventiveAction = rootCauseTasks.stream()
                    .map(QmsExceptionGroupTaskDO::getPreventiveAction)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("\n"));
        }
        if (StrUtil.isNotBlank(preventiveAction)) {
            updateObj.setPreventiveAction(preventiveAction);
        }
        investigationTasks.stream()
                .filter(task -> task.getContainmentDeptId() != null)
                .findFirst()
                .ifPresent(task -> {
                    updateObj.setActionDeptId(task.getContainmentDeptId());
                    updateObj.setActionDeptName(task.getContainmentDeptName());
                });
        rootCauseTasks.stream()
                .filter(task -> task.getRootCauseOwnerId() != null)
                .findFirst()
                .ifPresent(task -> {
                    updateObj.setActionOwnerId(task.getRootCauseOwnerId());
                    updateObj.setActionOwnerName(task.getRootCauseOwnerName());
                });
        qmsExceptionEventMapper.updateById(updateObj);
    }

    private void tryAdvanceEventAfterTaskAccepted(QmsExceptionEventDO before, QmsExceptionGroupTaskDO acceptedTask,
                                                  LocalDateTime now) {
        if (before == null || acceptedTask == null) {
            return;
        }
        String taskType = normalizeTaskType(acceptedTask.getTaskType());
        if (!allTasksOfTypeAccepted(before.getId(), taskType)) {
            return;
        }
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        String actionCode;
        String actionName;
        if (GROUP_TASK_TYPE_INVESTIGATION.equals(taskType)
                && statusIn(before.getStatus(), STATUS_CONTAINMENT, STATUS_CONTAINMENT_SIGN)) {
            updateObj.setStatus(STATUS_RESPONSIBILITY);
            updateObj.setCurrentNodeCode(NODE_RESPONSIBILITY);
            updateObj.setCurrentNodeName("确认责任部门");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getContainmentOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getContainmentOwnerName(), before.getCurrentHandlerUserName()));
            updateObj.setContainmentFinishTime(now);
            actionCode = "CONTAINMENT_ALL_DONE";
            actionName = "围堵措施会签完成";
        } else if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)
                && statusIn(before.getStatus(), STATUS_ROOT_CAUSE, STATUS_ROOT_CAUSE_SIGN)) {
            updateObj.setStatus(STATUS_RESULT_UPLOADER_ASSIGN);
            updateObj.setCurrentNodeCode(NODE_RESULT_UPLOADER_ASSIGN);
            updateObj.setCurrentNodeName("指定执行结果上传人");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getActionOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getActionOwnerName(), before.getCurrentHandlerUserName()));
            actionCode = "ROOT_CAUSE_ALL_DONE";
            actionName = "根因纠正会签完成";
        } else {
            return;
        }
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        syncBpmToBusinessStatus(after, actionName);
        writeFlowLog(before, after, actionCode, actionName, actionName,
                snapshot("taskType", taskType, "taskId", acceptedTask.getId()));
    }

    private QmsExceptionEventDO advanceAcceptedGroupTaskProgressIfNeeded(QmsExceptionEventDO before) {
        if (before == null || before.getId() == null) {
            return before;
        }
        String taskType;
        if (STATUS_CONTAINMENT_SIGN.equals(before.getStatus())) {
            taskType = GROUP_TASK_TYPE_INVESTIGATION;
        } else if (STATUS_ROOT_CAUSE_SIGN.equals(before.getStatus())) {
            taskType = GROUP_TASK_TYPE_ROOT_CAUSE;
        } else {
            return before;
        }
        QmsExceptionGroupTaskDO latestTask = latestGroupTaskOfType(before.getId(), taskType);
        if (latestTask == null || !GROUP_TASK_STATUS_ACCEPTED.equals(latestTask.getTaskStatus())) {
            return before;
        }
        QmsExceptionEventDO updateObj = new QmsExceptionEventDO();
        updateObj.setId(before.getId());
        String actionCode;
        String actionName;
        if (GROUP_TASK_TYPE_INVESTIGATION.equals(taskType)
                && STATUS_CONTAINMENT_SIGN.equals(before.getStatus())) {
            updateObj.setStatus(STATUS_RESPONSIBILITY);
            updateObj.setCurrentNodeCode(NODE_RESPONSIBILITY);
            updateObj.setCurrentNodeName("确认责任部门");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getContainmentOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getContainmentOwnerName(), before.getCurrentHandlerUserName()));
            updateObj.setContainmentFinishTime(firstNonNull(before.getContainmentFinishTime(), LocalDateTime.now()));
            actionCode = "CONTAINMENT_ALL_DONE_AUTO";
            actionName = "围堵措施会签完成";
        } else if (GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType)
                && STATUS_ROOT_CAUSE_SIGN.equals(before.getStatus())) {
            updateObj.setStatus(STATUS_RESULT_UPLOADER_ASSIGN);
            updateObj.setCurrentNodeCode(NODE_RESULT_UPLOADER_ASSIGN);
            updateObj.setCurrentNodeName("指定执行结果上传人");
            updateObj.setCurrentHandlerUserId(firstNonNull(before.getActionOwnerId(), before.getCurrentHandlerUserId()));
            updateObj.setCurrentHandlerUserName(firstNotBlank(before.getActionOwnerName(), before.getCurrentHandlerUserName()));
            actionCode = "ROOT_CAUSE_ALL_DONE_AUTO";
            actionName = "根因纠正会签完成";
        } else {
            return before;
        }
        qmsExceptionEventMapper.updateById(updateObj);
        QmsExceptionEventDO after = qmsExceptionEventMapper.selectById(before.getId());
        writeFlowLog(before, after, actionCode, actionName, "检测到最新小组任务已完成，自动推进业务节点",
                snapshot("taskType", taskType, "taskId", latestTask.getId(), "taskStatus", latestTask.getTaskStatus()));
        return after;
    }

    private boolean allTasksOfTypeAccepted(Long exceptionId, String taskType) {
        QmsExceptionGroupTaskDO latestTask = latestGroupTaskOfType(exceptionId, taskType);
        return latestTask != null && GROUP_TASK_STATUS_ACCEPTED.equals(latestTask.getTaskStatus());
    }

    private QmsExceptionGroupTaskDO latestGroupTaskOfType(Long exceptionId, String taskType) {
        if (exceptionId == null || StrUtil.isBlank(taskType)) {
            return null;
        }
        return qmsExceptionGroupTaskMapper.selectListByExceptionId(exceptionId).stream()
                .filter(task -> taskType.equals(normalizeTaskType(task.getTaskType())))
                .max(this::compareGroupTaskVersion)
                .orElse(null);
    }

    private int compareGroupTaskVersion(QmsExceptionGroupTaskDO left, QmsExceptionGroupTaskDO right) {
        LocalDateTime leftTime = left == null ? null : left.getDispatchTime();
        LocalDateTime rightTime = right == null ? null : right.getDispatchTime();
        int timeCompare = Comparator.nullsFirst(LocalDateTime::compareTo).compare(leftTime, rightTime);
        if (timeCompare != 0) {
            return timeCompare;
        }
        Long leftId = left == null ? null : left.getId();
        Long rightId = right == null ? null : right.getId();
        return Comparator.nullsFirst(Long::compareTo).compare(leftId, rightId);
    }

    private String joinTaskText(List<QmsExceptionGroupTaskDO> tasks, boolean containment) {
        return tasks.stream()
                .map(task -> containment
                        ? firstNotBlank(task.getActionDescription(), task.getContainmentSuggestion())
                        : task.getRootCause())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private String joinMemberText(List<QmsExceptionGroupTaskMemberDO> members, Set<Long> taskIds, String field) {
        return members.stream()
                .filter(member -> taskIds.contains(member.getTaskId()))
                .map(member -> {
                    if ("ROOT_CAUSE".equals(field)) {
                        return member.getRootCause();
                    }
                    if ("PREVENTIVE".equals(field)) {
                        return member.getPreventiveAction();
                    }
                    return member.getActionDescription();
                })
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private List<String> cleanStringList(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }
        return values.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private QmsNcRecordDO resolveNcr(QmsExceptionEventLinkNcrReqVO linkReqVO) {
        QmsNcRecordDO ncr = linkReqVO.getNcrId() != null ? qmsNcRecordMapper.selectById(linkReqVO.getNcrId()) : null;
        if (ncr == null && StrUtil.isNotBlank(linkReqVO.getNcrNo())) {
            ncr = qmsNcRecordMapper.selectFirstOne(QmsNcRecordDO::getNcNo, linkReqVO.getNcrNo());
        }
        if (ncr == null) {
            throw exception(QMS_EXCEPTION_EVENT_NCR_NOT_EXISTS);
        }
        return ncr;
    }

    private String normalizeNcrRelationType(String relationType, QmsNcRecordDO ncr) {
        if (RELATION_TYPE_RAW_MATERIAL_NCR.equalsIgnoreCase(StrUtil.blankToDefault(relationType, ""))) {
            return RELATION_TYPE_RAW_MATERIAL_NCR;
        }
        if (RELATION_TYPE_NCR.equalsIgnoreCase(StrUtil.blankToDefault(relationType, ""))) {
            return RELATION_TYPE_NCR;
        }
        return "RAW_MATERIAL".equalsIgnoreCase(StrUtil.blankToDefault(ncr.getSourceType(), ""))
                ? RELATION_TYPE_RAW_MATERIAL_NCR : RELATION_TYPE_NCR;
    }

    private void upsertNcrExceptionRelation(QmsNcRecordDO ncr, QmsExceptionEventDO exceptionEvent, String remark) {
        QmsNcRelationDO existingRelation = qmsNcRelationMapper.selectByObjectNo(
                ncr.getId(), RELATION_TYPE_EXCEPTION, exceptionEvent.getExceptionNo());
        if (existingRelation == null) {
            boolean firstException = qmsNcRelationMapper.selectPrimaryRelation(ncr.getId(), RELATION_TYPE_EXCEPTION) == null;
            qmsNcRelationMapper.insert(QmsNcRelationDO.builder()
                    .ncRecordId(ncr.getId())
                    .ncNo(ncr.getNcNo())
                    .relationType(RELATION_TYPE_EXCEPTION)
                    .relatedObjectId(exceptionEvent.getId())
                    .relatedObjectNo(exceptionEvent.getExceptionNo())
                    .relatedObjectName(firstNotBlank(exceptionEvent.getDescription(), exceptionEvent.getExceptionNo()))
                    .relationStatus(exceptionEvent.getStatus())
                    .primaryFlag(firstException)
                    .relationTime(LocalDateTime.now())
                    .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                    .relationUserName(currentUserName())
                    .remark(firstNotBlank(remark, "异常事件挂接处置单"))
                    .build());
        }
        QmsNcRecordDO updateObj = new QmsNcRecordDO();
        updateObj.setId(ncr.getId());
        updateObj.setRelatedExceptionNo(exceptionEvent.getExceptionNo());
        updateObj.setRelatedExceptionId(exceptionEvent.getId());
        qmsNcRecordMapper.updateById(updateObj);
    }

    private void validateNcrSelectableForException(QmsNcRecordDO ncr) {
        if (ncr.getRelatedExceptionId() != null || StrUtil.isNotBlank(ncr.getRelatedExceptionNo())
                || qmsNcRelationMapper.selectPrimaryRelation(ncr.getId(), RELATION_TYPE_EXCEPTION) != null) {
            throw exception(QMS_EXCEPTION_EVENT_NCR_ALREADY_LINKED);
        }
    }

    private void saveTeamMembers(Long exceptionId, List<QmsExceptionTeamMemberReqVO> teamMembers) {
        qmsExceptionTeamMemberMapper.delete(QmsExceptionTeamMemberDO::getExceptionId, exceptionId);
        if (CollUtil.isEmpty(teamMembers)) {
            return;
        }
        List<QmsExceptionTeamMemberDO> memberDOs = teamMembers.stream()
                .filter(member -> StrUtil.isNotBlank(member.getUserName()) || member.getUserId() != null)
                .map(member -> QmsExceptionTeamMemberDO.builder()
                        .exceptionId(exceptionId)
                        .deptId(member.getDeptId())
                        .deptName(firstNotBlank(member.getDeptName(), "未指定部门"))
                        .userId(member.getUserId())
                        .userName(firstNotBlank(member.getUserName(), "未指定成员"))
                        .memberRole(firstNotBlank(member.getMemberRole(), "MEMBER"))
                        .joinDate(member.getJoinDate())
                        .signStatus(member.getSignStatus())
                        .build())
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(memberDOs)) {
            qmsExceptionTeamMemberMapper.insertBatch(memberDOs);
        }
    }

    private void saveAttachmentRelations(Long exceptionId, List<QmsExceptionRelationReqVO> relations) {
        if (relations == null) {
            return;
        }
        qmsExceptionRelationMapper.deleteByExceptionIdAndRelationTypes(exceptionId, ATTACHMENT_RELATION_TYPES);
        if (CollUtil.isEmpty(relations)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<QmsExceptionRelationDO> attachmentRelations = relations.stream()
                .filter(relation -> ATTACHMENT_RELATION_TYPES.contains(relation.getRelationType()))
                .filter(relation -> StrUtil.isNotBlank(firstNotBlank(relation.getRemark(), relation.getRelatedObjectNo())))
                .map(relation -> {
                    String url = firstNotBlank(relation.getRemark(), relation.getRelatedObjectNo());
                    String attachmentName = firstNotBlank(
                            relation.getRelatedObjectName(), extractAttachmentName(url), relation.getRelatedObjectNo(), "附件");
                    return QmsExceptionRelationDO.builder()
                            .exceptionId(exceptionId)
                            .relationType(relation.getRelationType())
                            .relatedObjectId(relation.getRelatedObjectId())
                            .relatedObjectNo(buildAttachmentObjectNo(url, attachmentName))
                            .relatedObjectName(attachmentName)
                            .relationStatus(firstNotBlank(relation.getRelationStatus(), "ACTIVE"))
                            .primaryFlag(false)
                            .relationTime(now)
                            .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                            .relationUserName(currentUserName())
                            .remark(url)
                            .build();
                })
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(attachmentRelations)) {
            qmsExceptionRelationMapper.insertBatch(attachmentRelations);
        }
    }

    private String buildAttachmentObjectNo(String url, String attachmentName) {
        String objectNo = firstNotBlank(attachmentName, extractAttachmentName(url), "附件");
        if (objectNo.length() <= 64) {
            return objectNo;
        }
        String suffix = "-" + Integer.toUnsignedString(url.hashCode(), 36);
        return StrUtil.sub(objectNo, 0, 64 - suffix.length()) + suffix;
    }

    private String extractAttachmentName(String url) {
        String cleanUrl = StrUtil.subBefore(firstNotBlank(url), "?", false);
        int slashIndex = cleanUrl.lastIndexOf('/');
        return slashIndex >= 0 ? cleanUrl.substring(slashIndex + 1) : cleanUrl;
    }

    private void autoCreateAndLinkNcrIfNecessary(QmsExceptionEventDO event) {
        if (!Boolean.TRUE.equals(event.getIsRelatedProduct())
                || StrUtil.isNotBlank(event.getRelatedNcrNo())
                || qmsExceptionRelationMapper.selectCountByType(event.getId(), "NCR") > 0) {
            return;
        }
        QmsNcRecordSaveReqVO ncrReqVO = new QmsNcRecordSaveReqVO();
        ncrReqVO.setSourceType("FINISHED_PRODUCT");
        ncrReqVO.setSourceTypeName("成品");
        ncrReqVO.setSourceBizType("EXCEPTION");
        ncrReqVO.setSourceBizTypeName("异常事件");
        ncrReqVO.setSourceId(event.getId());
        ncrReqVO.setSourceNo(event.getExceptionNo());
        ncrReqVO.setHappenTime(firstNonNull(event.getDiscoverTime(), LocalDateTime.now()));
        ncrReqVO.setNcLevel(event.getExceptionLevel());
        ncrReqVO.setNcDescription(firstNotBlank(event.getDescription(), event.getInitialImpact()));
        ncrReqVO.setRelatedExceptionNo(event.getExceptionNo());
        ncrReqVO.setApplicantUserId(firstNonNull(event.getDiscovererId(), SecurityFrameworkUtils.getLoginUserId()));
        ncrReqVO.setApplicantUserName(firstNotBlank(event.getDiscovererName(), currentUserName()));
        ncrReqVO.setApplicantDeptId(firstNonNull(event.getDiscoverDeptId(), SecurityFrameworkUtils.getLoginUserDeptId()));
        ncrReqVO.setApplicantDeptName(firstNotBlank(event.getDiscoverDeptName(), "当前部门"));
        ncrReqVO.setCurrentHandlerUserId(firstNonNull(event.getCurrentHandlerUserId(), SecurityFrameworkUtils.getLoginUserId()));
        ncrReqVO.setCurrentHandlerUserName(firstNotBlank(event.getCurrentHandlerUserName(), currentUserName()));
        ncrReqVO.setRemark("产品相关异常事件自动生成 NCR");

        QmsNcRelationReqVO relationReqVO = new QmsNcRelationReqVO();
        relationReqVO.setRelationType("EXCEPTION");
        relationReqVO.setRelatedObjectId(event.getId());
        relationReqVO.setRelatedObjectNo(event.getExceptionNo());
        relationReqVO.setRelatedObjectName(firstNotBlank(event.getDescription(), event.getExceptionNo()));
        relationReqVO.setRelationStatus(event.getStatus());
        relationReqVO.setPrimaryFlag(true);
        relationReqVO.setRemark("由异常事件自动生成 NCR");
        ncrReqVO.setRelations(List.of(relationReqVO));

        Long ncrId = qmsNcRecordService.createNcRecord(ncrReqVO);
        QmsNcRecordDO ncr = qmsNcRecordMapper.selectById(ncrId);
        QmsExceptionEventLinkNcrReqVO linkReqVO = new QmsExceptionEventLinkNcrReqVO();
        linkReqVO.setId(event.getId());
        linkReqVO.setNcrId(ncrId);
        linkReqVO.setNcrNo(ncr == null ? null : ncr.getNcNo());
        linkReqVO.setNcrType(RELATION_TYPE_NCR);
        linkReqVO.setRemark("产品相关异常事件自动生成并关联 NCR");
        linkNcrInternal(linkReqVO, true, false);
    }

    private void save8dTeamMembers(Long reportId, List<QmsException8dTeamMemberReqVO> teamMembers) {
        if (CollUtil.isEmpty(teamMembers)) {
            qms8dTeamMemberMapper.insert(Qms8dTeamMemberDO.builder()
                    .reportId(reportId)
                    .memberRole("LEADER")
                    .deptName("品质部")
                    .userId(SecurityFrameworkUtils.getLoginUserId())
                    .userName(currentUserName())
                    .build());
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

    private QmsExceptionEventRespVO buildEventResp(QmsExceptionEventDO event, boolean detail) {
        QmsExceptionEventRespVO respVO = BeanUtils.toBean(event, QmsExceptionEventRespVO.class);
        if (!detail) {
            return respVO;
        }
        respVO.setTeamMembers(BeanUtils.toBean(qmsExceptionTeamMemberMapper.selectListByExceptionId(event.getId()),
                QmsExceptionEventRespVO.TeamMember.class));
        List<QmsExceptionGroupTaskDO> groupTasks = qmsExceptionGroupTaskMapper.selectListByExceptionId(event.getId());
        List<Long> groupTaskIds = groupTasks.stream().map(QmsExceptionGroupTaskDO::getId).collect(Collectors.toList());
        Map<Long, List<QmsExceptionGroupTaskMemberDO>> memberMap =
                qmsExceptionGroupTaskMemberMapper.selectListByTaskIds(groupTaskIds).stream()
                        .collect(Collectors.groupingBy(QmsExceptionGroupTaskMemberDO::getTaskId));
        Map<Long, List<QmsExceptionGroupTaskReplyDO>> replyMap =
                qmsExceptionGroupTaskReplyMapper.selectListByTaskIds(groupTaskIds).stream()
                        .collect(Collectors.groupingBy(QmsExceptionGroupTaskReplyDO::getTaskId));
        Map<Long, List<QmsExceptionGroupTaskConfirmLogDO>> confirmLogMap =
                qmsExceptionGroupTaskConfirmLogMapper.selectListByTaskIds(groupTaskIds).stream()
                        .collect(Collectors.groupingBy(QmsExceptionGroupTaskConfirmLogDO::getTaskId));
        List<QmsExceptionEventRespVO.GroupTask> groupTaskVOs =
                BeanUtils.toBean(groupTasks, QmsExceptionEventRespVO.GroupTask.class);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        groupTaskVOs.forEach(task -> {
            List<QmsExceptionGroupTaskMemberDO> taskMembers = memberMap.getOrDefault(task.getId(), List.of());
            List<QmsExceptionGroupTaskReplyDO> taskReplies = replyMap.getOrDefault(task.getId(), List.of());
            List<QmsExceptionGroupTaskConfirmLogDO> taskConfirmLogs = filterCurrentGroupTaskConfirmLogs(
                    confirmLogMap.getOrDefault(task.getId(), List.of()), taskMembers, taskReplies);
            task.setMembers(BeanUtils.toBean(taskMembers, QmsExceptionEventRespVO.GroupTaskMember.class));
            task.setReplies(BeanUtils.toBean(taskReplies, QmsExceptionEventRespVO.GroupTaskReply.class));
            task.setConfirmLogs(BeanUtils.toBean(taskConfirmLogs, QmsExceptionEventRespVO.GroupTaskConfirmLog.class));
            QmsExceptionGroupTaskDO taskDO = groupTasks.stream()
                    .filter(item -> Objects.equals(item.getId(), task.getId()))
                    .findFirst()
                    .orElse(null);
            if (taskDO != null) {
                task.setCurrentUserCanSubmit(canCurrentUserSubmitGroupTask(
                        taskDO, loginUserId, taskMembers));
                task.setCurrentUserCanConfirm(canCurrentUserConfirmGroupTask(
                        taskDO, loginUserId, taskMembers));
                task.setCurrentUserCanReview(canCurrentUserReviewGroupTask(event, taskDO, loginUserId));
            }
        });
        respVO.setGroupTasks(groupTaskVOs);
        List<QmsExceptionGroupTaskMemberDO> currentUserMembers = memberMap.values().stream()
                .flatMap(List::stream)
                .filter(member -> isGroupTaskMemberHandler(member, loginUserId))
                .collect(Collectors.toList());
        Set<Long> memberTaskIds = currentUserMembers.stream()
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> pendingConfirmTaskIds = currentUserMembers.stream()
                .filter(this::isPendingMemberConfirm)
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> doneMemberTaskIds = currentUserMembers.stream()
                .filter(this::isDoneMemberConfirm)
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        fillCurrentUserTaskTodo(respVO, loginUserId, groupTasks,
                memberTaskIds, pendingConfirmTaskIds, doneMemberTaskIds);
        fillCurrentUserListState(respVO, loginUserId, buildParticipatedExceptionIds(loginUserId),
                buildPendingGroupTaskExceptionIds(loginUserId));
        respVO.setRelations(BeanUtils.toBean(qmsExceptionRelationMapper.selectListByExceptionId(event.getId()),
                QmsExceptionEventRespVO.Relation.class));
        respVO.setFlowLogs(getFlowLogList(event.getId()));
        return respVO;
    }

    private List<QmsExceptionGroupTaskConfirmLogDO> filterCurrentGroupTaskConfirmLogs(
            List<QmsExceptionGroupTaskConfirmLogDO> logs,
            List<QmsExceptionGroupTaskMemberDO> members,
            List<QmsExceptionGroupTaskReplyDO> replies) {
        if (CollUtil.isEmpty(logs)) {
            return List.of();
        }
        Set<Long> memberIds = members.stream()
                .map(QmsExceptionGroupTaskMemberDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> memberUserIds = members.stream()
                .map(QmsExceptionGroupTaskMemberDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> replyIds = replies.stream()
                .map(QmsExceptionGroupTaskReplyDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, QmsExceptionGroupTaskConfirmLogDO> latestLogMap = new LinkedHashMap<>();
        logs.stream()
                .filter(log -> isCurrentGroupTaskConfirmLog(log, memberIds, memberUserIds, replyIds))
                .forEach(log -> {
                    String key = buildGroupTaskConfirmLogDedupeKey(log);
                    QmsExceptionGroupTaskConfirmLogDO existed = latestLogMap.get(key);
                    if (existed == null || isNewerGroupTaskConfirmLog(log, existed)) {
                        latestLogMap.put(key, log);
                    }
                });
        return latestLogMap.values().stream()
                .sorted(Comparator
                        .comparing(QmsExceptionGroupTaskConfirmLogDO::getConfirmTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(QmsExceptionGroupTaskConfirmLogDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private boolean isCurrentGroupTaskConfirmLog(QmsExceptionGroupTaskConfirmLogDO log, Set<Long> memberIds,
                                                 Set<Long> memberUserIds, Set<Long> replyIds) {
        if (log.getMemberId() != null) {
            return memberIds.contains(log.getMemberId());
        }
        if (log.getReplyId() != null) {
            return replyIds.contains(log.getReplyId());
        }
        return log.getUserId() != null && memberUserIds.contains(log.getUserId());
    }

    private String buildGroupTaskConfirmLogDedupeKey(QmsExceptionGroupTaskConfirmLogDO log) {
        String memberKey = log.getMemberId() != null
                ? "member-" + log.getMemberId()
                : "user-" + log.getUserId();
        String replyKey = log.getReplyId() != null
                ? "reply-" + log.getReplyId()
                : "no-reply";
        return replyKey + ":" + memberKey + ":" + StrUtil.blankToDefault(log.getConfirmAction(), "no-action");
    }

    private boolean isNewerGroupTaskConfirmLog(QmsExceptionGroupTaskConfirmLogDO current,
                                               QmsExceptionGroupTaskConfirmLogDO existed) {
        if (!Objects.equals(current.getConfirmTime(), existed.getConfirmTime())) {
            if (current.getConfirmTime() == null) {
                return false;
            }
            if (existed.getConfirmTime() == null) {
                return true;
            }
            return current.getConfirmTime().isAfter(existed.getConfirmTime());
        }
        return Objects.compare(current.getId(), existed.getId(), Comparator.nullsFirst(Long::compareTo)) > 0;
    }

    private Qms8dReportRespVO build8dResp(Qms8dReportDO report) {
        Qms8dReportRespVO respVO = BeanUtils.toBean(report, Qms8dReportRespVO.class);
        respVO.setTeamMembers(BeanUtils.toBean(qms8dTeamMemberMapper.selectListByReportId(report.getId()),
                Qms8dReportRespVO.TeamMember.class));
        return respVO;
    }

    private void writeFlowLog(QmsExceptionEventDO before, QmsExceptionEventDO after, String actionCode,
                              String actionName, String opinion, Map<String, Object> snapshot) {
        qmsExceptionFlowLogMapper.insert(QmsExceptionFlowLogDO.builder()
                .exceptionId(after.getId())
                .exceptionNo(after.getExceptionNo())
                .actionCode(actionCode)
                .actionName(actionName)
                .fromStatus(before == null ? null : before.getStatus())
                .toStatus(after.getStatus())
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

    private void write8dFlowLog(Qms8dReportDO report, String opinion, Map<String, Object> snapshot) {
        qms8dFlowLogMapper.insert(Qms8dFlowLogDO.builder()
                .reportId(report.getId())
                .reportNo(report.getReportNo())
                .actionCode("CREATE")
                .actionName("立案")
                .toStatus(report.getStatus())
                .toStep(report.getCurrentStep())
                .toNodeCode(report.getCurrentNodeCode())
                .toNodeName(report.getCurrentNodeName())
                .opinion(opinion)
                .handlerUserId(SecurityFrameworkUtils.getLoginUserId())
                .handlerUserName(currentUserName())
                .handleTime(LocalDateTime.now())
                .businessSnapshot(snapshot)
                .build());
    }

    private void fillCurrentUserListState(QmsExceptionEventRespVO respVO, Long loginUserId,
                                          Set<Long> participatedIds, Set<Long> pendingTaskExceptionIds) {
        if (respVO == null) {
            return;
        }
        boolean minePending = isCurrentUserPendingEvent(respVO, loginUserId, pendingTaskExceptionIds);
        boolean mineDiscovered = loginUserId != null && Objects.equals(respVO.getDiscovererId(), loginUserId);
        boolean mineParticipated = respVO.getId() != null && participatedIds != null
                && participatedIds.contains(respVO.getId());
        respVO.setMinePending(minePending);
        respVO.setMineDiscovered(mineDiscovered);
        respVO.setMineParticipated(mineParticipated);
        respVO.setCanHandle(minePending);
        respVO.setCanWithdraw(canWithdrawCurrentUser(respVO.getId(), loginUserId));
        respVO.setListActionCode(minePending ? resolveListActionCode(respVO) : "VIEW");
        respVO.setListActionName(minePending ? resolveListActionName(respVO) : "详情");
    }

    private boolean canWithdrawCurrentUser(Long exceptionId, Long loginUserId) {
        if (exceptionId == null || loginUserId == null) {
            return false;
        }
        QmsExceptionEventDO event = qmsExceptionEventMapper.selectById(exceptionId);
        return findWithdrawableHistoricTask(event, loginUserId) != null;
    }

    private HistoricTaskInstance findWithdrawableHistoricTask(QmsExceptionEventDO event, Long loginUserId) {
        if (event == null || loginUserId == null || StrUtil.isBlank(event.getProcessInstanceId())
                || isTerminalExceptionStatus(event.getStatus())) {
            return null;
        }
        if (!Objects.equals(bpmProcessInstanceApi.getProcessInstanceStatus(event.getProcessInstanceId()),
                BpmProcessInstanceStatusEnum.RUNNING.getStatus())) {
            return null;
        }
        List<Task> runningTasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                event.getProcessInstanceId(), true, null);
        if (CollUtil.isEmpty(runningTasks)) {
            return null;
        }
        List<HistoricTaskInstance> historicTasks = bpmTaskService.getTaskListByProcessInstanceId(
                event.getProcessInstanceId(), false);
        if (CollUtil.isEmpty(historicTasks)) {
            return null;
        }
        List<HistoricTaskInstance> userFinishedTasks = historicTasks.stream()
                .filter(task -> task.getEndTime() != null
                        && Objects.equals(task.getAssignee(), String.valueOf(loginUserId)))
                .sorted(Comparator.comparing(HistoricTaskInstance::getEndTime).reversed())
                .collect(Collectors.toList());
        for (HistoricTaskInstance task : userFinishedTasks) {
            if (hasNonCancelledFinishedTaskAfter(historicTasks, task)) {
                continue;
            }
            boolean nextTaskStillPending = runningTasks.stream()
                    .anyMatch(runningTask -> runningTask.getCreateTime() != null
                            && !runningTask.getCreateTime().before(task.getEndTime()));
            if (nextTaskStillPending) {
                return task;
            }
        }
        return null;
    }

    private boolean hasNonCancelledFinishedTaskAfter(List<HistoricTaskInstance> tasks,
                                                     HistoricTaskInstance baseTask) {
        if (CollUtil.isEmpty(tasks) || baseTask == null || baseTask.getEndTime() == null) {
            return false;
        }
        return tasks.stream()
                .anyMatch(task -> task.getEndTime() != null
                        && task.getEndTime().after(baseTask.getEndTime())
                        && !isHistoricTaskCancelled(task));
    }

    private boolean isHistoricTaskCancelled(HistoricTaskInstance task) {
        Object value = task == null || task.getTaskLocalVariables() == null ? null
                : task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
        Integer status = null;
        if (value instanceof Number number) {
            status = number.intValue();
        } else if (value instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                status = Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                status = null;
            }
        }
        return BpmTaskStatusEnum.isCancelStatus(status);
    }

    private boolean isCurrentUserPendingEvent(QmsExceptionEventRespVO respVO, Long loginUserId,
                                              Set<Long> pendingTaskExceptionIds) {
        if (loginUserId == null || respVO.getId() == null || isTerminalExceptionStatus(respVO.getStatus())) {
            return false;
        }
        if (Objects.equals(respVO.getCurrentHandlerUserId(), loginUserId)) {
            return true;
        }
        if (hasCurrentUserRunningBpmTask(respVO, loginUserId)) {
            return true;
        }
        if (Boolean.TRUE.equals(respVO.getCurrentUserTaskTodo())) {
            return true;
        }
        return pendingTaskExceptionIds != null && pendingTaskExceptionIds.contains(respVO.getId());
    }

    private boolean hasCurrentUserRunningBpmTask(QmsExceptionEventRespVO respVO, Long loginUserId) {
        if (respVO == null || loginUserId == null || StrUtil.isBlank(respVO.getProcessInstanceId())) {
            return false;
        }
        String taskDefinitionKey = resolveCurrentBpmTaskDefinitionKey(respVO.getStatus(), respVO.getCurrentNodeCode());
        if (StrUtil.isBlank(taskDefinitionKey)) {
            return false;
        }
        List<Task> tasks = getRunningTasksByDefinitionKeyWithAliases(respVO.getProcessInstanceId(), taskDefinitionKey);
        if (CollUtil.isEmpty(tasks)) {
            return false;
        }
        return tasks.stream().anyMatch(task -> Objects.equals(parseTaskAssignee(task), loginUserId));
    }

    private String resolveCurrentBpmTaskDefinitionKey(String status, String currentNodeCode) {
        if (STATUS_CONFIRMING.equals(status) || NODE_CONFIRMING.equals(currentNodeCode)) {
            return BPM_NODE_QUALITY_CONFIRM;
        }
        if (STATUS_RETURNED.equals(status) || NODE_RETURNED.equals(currentNodeCode)) {
            return BPM_NODE_START_USER;
        }
        if (STATUS_CONTAINMENT.equals(status) || NODE_CONTAINMENT.equals(currentNodeCode)) {
            return BPM_NODE_CONTAINMENT_OWNER_HANDLE;
        }
        if (STATUS_CONTAINMENT_SIGN.equals(status) || NODE_CONTAINMENT_SIGN.equals(currentNodeCode)) {
            return BPM_NODE_CONTAINMENT_MEMBER_SIGN;
        }
        if (STATUS_RESPONSIBILITY.equals(status) || NODE_RESPONSIBILITY.equals(currentNodeCode)) {
            return BPM_NODE_RESPONSIBILITY_CONFIRM;
        }
        if (STATUS_ROOT_CAUSE.equals(status) || NODE_ROOT_CAUSE.equals(currentNodeCode)) {
            return BPM_NODE_ROOT_CAUSE_OWNER_HANDLE;
        }
        if (STATUS_ROOT_CAUSE_SIGN.equals(status) || NODE_ROOT_CAUSE_SIGN.equals(currentNodeCode)) {
            return BPM_NODE_ROOT_CAUSE_MEMBER_SIGN;
        }
        if (STATUS_RESULT_UPLOADER_ASSIGN.equals(status) || NODE_RESULT_UPLOADER_ASSIGN.equals(currentNodeCode)) {
            return BPM_NODE_RESULT_UPLOADER_ASSIGN;
        }
        if (STATUS_RESULT_UPLOAD.equals(status) || NODE_RESULT_UPLOAD.equals(currentNodeCode)) {
            return BPM_NODE_RESULT_UPLOAD;
        }
        if (STATUS_QA_CLOSURE.equals(status) || STATUS_VERIFYING.equals(status)
                || NODE_QA_CLOSURE.equals(currentNodeCode)) {
            return BPM_NODE_QA_CLOSURE;
        }
        return null;
    }

    private List<Task> getRunningTasksByDefinitionKeyWithAliases(String processInstanceId, String taskDefinitionKey) {
        if (StrUtil.isBlank(processInstanceId) || StrUtil.isBlank(taskDefinitionKey)) {
            return List.of();
        }
        for (String key : taskDefinitionKeysWithAliases(taskDefinitionKey)) {
            List<Task> tasks = bpmTaskService.getRunningTaskListByProcessInstanceId(
                    processInstanceId, true, key);
            if (CollUtil.isNotEmpty(tasks)) {
                return tasks;
            }
        }
        return List.of();
    }

    private List<String> taskDefinitionKeysWithAliases(String taskDefinitionKey) {
        if (StrUtil.isBlank(taskDefinitionKey)) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        keys.add(taskDefinitionKey);
        if (BPM_NODE_CONTAINMENT_OWNER_HANDLE.equals(taskDefinitionKey)) {
            keys.add(BPM_NODE_CONTAINMENT_OWNER_HANDLE_LEGACY);
        } else if (BPM_NODE_CONTAINMENT_OWNER_HANDLE_LEGACY.equals(taskDefinitionKey)) {
            keys.add(BPM_NODE_CONTAINMENT_OWNER_HANDLE);
        } else if (BPM_NODE_ROOT_CAUSE_OWNER_HANDLE.equals(taskDefinitionKey)) {
            keys.add(BPM_NODE_ROOT_CAUSE_OWNER_HANDLE_LEGACY);
        } else if (BPM_NODE_ROOT_CAUSE_OWNER_HANDLE_LEGACY.equals(taskDefinitionKey)) {
            keys.add(BPM_NODE_ROOT_CAUSE_OWNER_HANDLE);
        }
        return keys.stream().distinct().collect(Collectors.toList());
    }

    private boolean isTerminalExceptionStatus(String status) {
        return STATUS_CLOSED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    private String resolveListActionCode(QmsExceptionEventRespVO respVO) {
        if (Boolean.TRUE.equals(respVO.getCurrentUserTaskTodo())) {
            return "GROUP_TASK";
        }
        String status = respVO.getStatus();
        if (STATUS_CONFIRMING.equals(status)) {
            return "QUALITY_CONFIRM";
        }
        if (STATUS_RETURNED.equals(status)) {
            return "SUPPLEMENT";
        }
        if (STATUS_CONTAINMENT.equals(status)) {
            return "CONTAINMENT";
        }
        if (STATUS_CONTAINMENT_SIGN.equals(status)) {
            return "CONTAINMENT_SIGN";
        }
        if (STATUS_RESPONSIBILITY.equals(status)) {
            return "RESPONSIBILITY_CONFIRM";
        }
        if (STATUS_ROOT_CAUSE.equals(status)) {
            return "ROOT_CAUSE";
        }
        if (STATUS_ROOT_CAUSE_SIGN.equals(status)) {
            return "ROOT_CAUSE_SIGN";
        }
        if (STATUS_RESULT_UPLOADER_ASSIGN.equals(status)) {
            return "RESULT_UPLOADER_ASSIGN";
        }
        if (STATUS_RESULT_UPLOAD.equals(status)) {
            return "RESULT_UPLOAD";
        }
        if (STATUS_QA_CLOSURE.equals(status) || STATUS_VERIFYING.equals(status)) {
            return "CLOSE";
        }
        return "HANDLE";
    }

    private String resolveListActionName(QmsExceptionEventRespVO respVO) {
        if (Boolean.TRUE.equals(respVO.getCurrentUserTaskTodo())) {
            return firstNotBlank(respVO.getCurrentUserTaskTodoLabel(), "处理子任务");
        }
        String status = respVO.getStatus();
        if (STATUS_CONFIRMING.equals(status)) {
            return "确认";
        }
        if (STATUS_RETURNED.equals(status)) {
            return "补充提交";
        }
        if (STATUS_CONTAINMENT.equals(status)) {
            return "围堵办理";
        }
        if (STATUS_CONTAINMENT_SIGN.equals(status)) {
            return "围堵会签";
        }
        if (STATUS_RESPONSIBILITY.equals(status)) {
            return "指定责任负责人";
        }
        if (STATUS_ROOT_CAUSE.equals(status)) {
            return "根因纠正办理";
        }
        if (STATUS_ROOT_CAUSE_SIGN.equals(status)) {
            return "根因纠正会签";
        }
        if (STATUS_RESULT_UPLOADER_ASSIGN.equals(status)) {
            return "指定上传人";
        }
        if (STATUS_RESULT_UPLOAD.equals(status)) {
            return "上传执行结果";
        }
        if (STATUS_QA_CLOSURE.equals(status) || STATUS_VERIFYING.equals(status)) {
            return "关闭";
        }
        return "办理";
    }

    private void fillCurrentUserTaskTodo(List<QmsExceptionEventRespVO> eventList, Long loginUserId) {
        if (CollUtil.isEmpty(eventList) || loginUserId == null) {
            return;
        }
        Set<Long> exceptionIds = eventList.stream()
                .map(QmsExceptionEventRespVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(exceptionIds)) {
            return;
        }
        List<QmsExceptionGroupTaskMemberDO> currentUserMembers =
                qmsExceptionGroupTaskMemberMapper.selectListByUserId(loginUserId);
        Set<Long> memberTaskIds = currentUserMembers.stream()
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> pendingConfirmTaskIds = currentUserMembers.stream()
                .filter(this::isPendingMemberConfirm)
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> doneMemberTaskIds = currentUserMembers.stream()
                .filter(this::isDoneMemberConfirm)
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, List<QmsExceptionGroupTaskDO>> taskMap = qmsExceptionGroupTaskMapper
                .selectListByExceptionIds(exceptionIds).stream()
                .filter(task -> isCurrentUserPendingGroupTask(
                        task, loginUserId, pendingConfirmTaskIds, pendingConfirmTaskIds)
                        || isCurrentUserDoneGroupTask(task, loginUserId, memberTaskIds, doneMemberTaskIds))
                .collect(Collectors.groupingBy(QmsExceptionGroupTaskDO::getExceptionId));
        eventList.forEach(event -> fillCurrentUserTaskTodo(
                event, loginUserId, taskMap.getOrDefault(event.getId(), List.of()),
                memberTaskIds, pendingConfirmTaskIds, doneMemberTaskIds));
    }

    private void fillCurrentUserTaskTodo(QmsExceptionEventRespVO event, Long loginUserId,
                                         List<QmsExceptionGroupTaskDO> tasks, Set<Long> memberTaskIds,
                                         Set<Long> pendingConfirmTaskIds, Set<Long> doneMemberTaskIds) {
        List<QmsExceptionGroupTaskDO> activeTasks = tasks.stream()
                .filter(task -> isGroupTaskActiveForEvent(
                        BeanUtils.toBean(event, QmsExceptionEventDO.class), task))
                .collect(Collectors.toList());
        List<QmsExceptionGroupTaskDO> pendingTasks = activeTasks.stream()
                .filter(task -> isCurrentUserPendingGroupTask(
                        task, loginUserId, pendingConfirmTaskIds, pendingConfirmTaskIds))
                .collect(Collectors.toList());
        List<QmsExceptionGroupTaskDO> doneTasks = activeTasks.stream()
                .filter(task -> isCurrentUserDoneGroupTask(task, loginUserId, memberTaskIds, doneMemberTaskIds))
                .collect(Collectors.toList());
        boolean hasInvestigation = pendingTasks.stream()
                .anyMatch(task -> GROUP_TASK_TYPE_INVESTIGATION.equals(normalizeTaskType(task.getTaskType())));
        boolean hasRootCause = pendingTasks.stream()
                .anyMatch(task -> GROUP_TASK_TYPE_ROOT_CAUSE.equals(normalizeTaskType(task.getTaskType())));
        boolean hasReview = pendingTasks.stream()
                .anyMatch(task -> isCurrentUserReviewPendingGroupTask(task, loginUserId));
        boolean hasSubmit = pendingTasks.stream()
                .anyMatch(task -> isCurrentUserSubmitPendingGroupTask(task, loginUserId, pendingConfirmTaskIds));
        boolean hasConfirm = pendingTasks.stream()
                .anyMatch(task -> isCurrentUserConfirmPendingGroupTask(task, loginUserId, pendingConfirmTaskIds));
        event.setCurrentUserTaskTodo(CollUtil.isNotEmpty(pendingTasks));
        event.setCurrentUserTaskTodoCount(pendingTasks.size());
        event.setCurrentUserTaskTodoType(hasInvestigation && hasRootCause ? "MIXED"
                : hasRootCause ? GROUP_TASK_TYPE_ROOT_CAUSE
                : hasInvestigation ? GROUP_TASK_TYPE_INVESTIGATION : null);
        event.setCurrentUserTaskTodoLabel(resolveCurrentUserTaskTodoLabel(
                hasInvestigation, hasRootCause, hasReview, hasSubmit, hasConfirm));
        boolean doneHasInvestigation = doneTasks.stream()
                .anyMatch(task -> GROUP_TASK_TYPE_INVESTIGATION.equals(normalizeTaskType(task.getTaskType())));
        boolean doneHasRootCause = doneTasks.stream()
                .anyMatch(task -> GROUP_TASK_TYPE_ROOT_CAUSE.equals(normalizeTaskType(task.getTaskType())));
        event.setCurrentUserTaskDone(CollUtil.isNotEmpty(doneTasks));
        event.setCurrentUserTaskDoneCount(doneTasks.size());
        event.setCurrentUserTaskDoneType(doneHasInvestigation && doneHasRootCause ? "MIXED"
                : doneHasRootCause ? GROUP_TASK_TYPE_ROOT_CAUSE
                : doneHasInvestigation ? GROUP_TASK_TYPE_INVESTIGATION : null);
        event.setCurrentUserTaskDoneLabel(doneHasInvestigation && doneHasRootCause ? "已处理子任务"
                : doneHasRootCause ? "已提交根因纠正"
                : doneHasInvestigation ? "已处理围堵措施" : null);
    }

    private boolean isCurrentUserPendingGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId,
                                                  Set<Long> memberTaskIds, Set<Long> pendingConfirmTaskIds) {
        return isCurrentUserSubmitPendingGroupTask(task, loginUserId, memberTaskIds)
                || isCurrentUserConfirmPendingGroupTask(task, loginUserId, pendingConfirmTaskIds)
                || isCurrentUserReviewPendingGroupTask(task, loginUserId);
    }

    private boolean isCurrentUserSubmitPendingGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId,
                                                        Set<Long> pendingMemberTaskIds) {
        return task != null
                && loginUserId != null
                && statusIn(task.getTaskStatus(), GROUP_TASK_STATUS_DISPATCHED,
                GROUP_TASK_STATUS_RETURNED, GROUP_TASK_STATUS_SUBMITTED)
                && pendingMemberTaskIds.contains(task.getId());
    }

    private boolean isCurrentUserReviewPendingGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId) {
        return task != null
                && loginUserId != null
                && GROUP_TASK_STATUS_SIGNED.equals(task.getTaskStatus())
                && Objects.equals(task.getDispatcherUserId(), loginUserId);
    }

    private boolean isCurrentUserConfirmPendingGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId,
                                                         Set<Long> pendingConfirmTaskIds) {
        return task != null
                && loginUserId != null
                && GROUP_TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())
                && pendingConfirmTaskIds.contains(task.getId());
    }

    private boolean isCurrentUserDoneGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId,
                                               Set<Long> memberTaskIds,
                                               Set<Long> doneMemberTaskIds) {
        return task != null
                && (doneMemberTaskIds.contains(task.getId())
                        || (GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus())
                                && memberTaskIds.contains(task.getId()))
                        || (Objects.equals(effectiveTaskExecutorId(task), loginUserId)
                                && statusIn(task.getTaskStatus(),
                                GROUP_TASK_STATUS_SUBMITTED, GROUP_TASK_STATUS_SIGNED,
                                GROUP_TASK_STATUS_ACCEPTED))
                        || (Objects.equals(task.getDispatcherUserId(), loginUserId)
                                && GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus())));
    }

    private String resolveCurrentUserTaskTodoLabel(boolean hasInvestigation, boolean hasRootCause,
                                                   boolean hasReview, boolean hasSubmit, boolean hasConfirm) {
        if (hasReview && hasSubmit) {
            return "复核并处理子任务";
        }
        if (hasReview) {
            return hasInvestigation && hasRootCause ? "复核子任务"
                    : hasRootCause ? "复核根因纠正"
                    : hasInvestigation ? "复核围堵措施" : "复核子任务";
        }
        if (hasConfirm) {
            return hasInvestigation && hasRootCause ? "确认子任务"
                    : hasRootCause ? "确认根因纠正"
                    : hasInvestigation ? "确认小组提交" : "确认子任务";
        }
        return hasInvestigation && hasRootCause ? "处理子任务"
                : hasRootCause ? "提交根因纠正"
                : hasInvestigation ? "上传围堵措施" : null;
    }

    private boolean isPendingMemberConfirm(QmsExceptionGroupTaskMemberDO member) {
        return GROUP_MEMBER_CONFIRM_PENDING.equals(firstNotBlank(member.getConfirmStatus(), GROUP_MEMBER_CONFIRM_PENDING));
    }

    private boolean isDoneMemberConfirm(QmsExceptionGroupTaskMemberDO member) {
        return statusIn(firstNotBlank(member.getConfirmStatus(), GROUP_MEMBER_CONFIRM_PENDING),
                GROUP_MEMBER_CONFIRM_SUBMITTED, GROUP_MEMBER_CONFIRM_CONFIRMED,
                GROUP_MEMBER_CONFIRM_DISAGREED, GROUP_MEMBER_CONFIRM_EXPIRED);
    }

    private boolean isGroupTaskMemberHandler(QmsExceptionGroupTaskMemberDO member, Long userId) {
        return member != null && userId != null
                && (Objects.equals(member.getUserId(), userId)
                || Objects.equals(member.getDelegateUserId(), userId));
    }

    private boolean canCurrentUserSubmitGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId,
                                                  List<QmsExceptionGroupTaskMemberDO> members) {
        if (task == null || loginUserId == null
                || !statusIn(task.getTaskStatus(), GROUP_TASK_STATUS_DISPATCHED,
                GROUP_TASK_STATUS_RETURNED, GROUP_TASK_STATUS_SUBMITTED)) {
            return false;
        }
        return members.stream().anyMatch(member -> isGroupTaskMemberHandler(member, loginUserId)
                && isPendingMemberConfirm(member));
    }

    private boolean canCurrentUserConfirmGroupTask(QmsExceptionGroupTaskDO task, Long loginUserId,
                                                   List<QmsExceptionGroupTaskMemberDO> members) {
        return false;
    }

    private boolean canCurrentUserReviewGroupTask(QmsExceptionEventDO event, QmsExceptionGroupTaskDO task,
                                                  Long loginUserId) {
        return task != null
                && loginUserId != null
                && GROUP_TASK_STATUS_SIGNED.equals(task.getTaskStatus())
                && (Objects.equals(task.getDispatcherUserId(), loginUserId)
                || Objects.equals(event.getCurrentHandlerUserId(), loginUserId));
    }

    private Set<Long> buildParticipatedExceptionIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        qmsExceptionFlowLogMapper.selectListByHandlerUserId(loginUserId)
                .forEach(log -> ids.add(log.getExceptionId()));
        qmsExceptionTeamMemberMapper.selectListByUserId(loginUserId)
                .forEach(member -> ids.add(member.getExceptionId()));
        qmsExceptionGroupTaskMemberMapper.selectListByUserId(loginUserId)
                .forEach(member -> ids.add(member.getExceptionId()));
        qmsExceptionGroupTaskMapper.selectListByRootCauseOwnerId(loginUserId)
                .forEach(task -> ids.add(task.getExceptionId()));
        qmsExceptionGroupTaskMapper.selectListByExecutorUserId(loginUserId)
                .forEach(task -> ids.add(task.getExceptionId()));
        qmsExceptionGroupTaskMapper.selectListByDispatcherUserId(loginUserId)
                .forEach(task -> ids.add(task.getExceptionId()));
        return ids;
    }

    private Set<Long> buildPendingGroupTaskExceptionIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        List<QmsExceptionGroupTaskMemberDO> currentUserMembers =
                qmsExceptionGroupTaskMemberMapper.selectListByUserId(loginUserId);
        Set<Long> taskIds = currentUserMembers.stream()
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> pendingConfirmTaskIds = currentUserMembers.stream()
                .filter(this::isPendingMemberConfirm)
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<QmsExceptionGroupTaskDO> tasks = new ArrayList<>(qmsExceptionGroupTaskMapper.selectListByIds(taskIds));
        Set<Long> exceptionIds = tasks.stream()
                .map(QmsExceptionGroupTaskDO::getExceptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QmsExceptionEventDO> eventMap = CollUtil.isEmpty(exceptionIds) ? Map.of()
                : qmsExceptionEventMapper.selectBatchIds(exceptionIds).stream()
                .collect(Collectors.toMap(QmsExceptionEventDO::getId, event -> event, (left, right) -> left));
        tasks.stream()
                .filter(task -> isGroupTaskActiveForEvent(eventMap.get(task.getExceptionId()), task))
                .filter(task -> isCurrentUserPendingGroupTask(task, loginUserId, pendingConfirmTaskIds, pendingConfirmTaskIds))
                .forEach(task -> ids.add(task.getExceptionId()));
        return ids;
    }

    private Set<Long> buildContainmentOverdueExceptionIds(Long loginUserId) {
        Set<Long> ids = new HashSet<>();
        if (loginUserId == null) {
            return ids;
        }
        List<QmsExceptionGroupTaskMemberDO> currentUserMembers =
                qmsExceptionGroupTaskMemberMapper.selectListByUserId(loginUserId);
        if (CollUtil.isEmpty(currentUserMembers)) {
            return ids;
        }
        currentUserMembers.stream()
                .filter(member -> Boolean.TRUE.equals(member.getOverdueFlag())
                        || GROUP_MEMBER_CONFIRM_EXPIRED.equals(member.getConfirmStatus()))
                .forEach(member -> ids.add(member.getExceptionId()));
        Set<Long> taskIds = currentUserMembers.stream()
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(taskIds)) {
            return ids;
        }
        LocalDateTime now = LocalDateTime.now();
        qmsExceptionGroupTaskMapper.selectListByIds(taskIds).stream()
                .filter(task -> GROUP_TASK_TYPE_INVESTIGATION.equals(normalizeTaskType(task.getTaskType())))
                .filter(task -> !GROUP_TASK_STATUS_ACCEPTED.equals(task.getTaskStatus()))
                .filter(task -> task.getContainmentDeadline() != null
                        && now.isAfter(task.getContainmentDeadline()))
                .forEach(task -> ids.add(task.getExceptionId()));
        return ids;
    }

    private String generateExceptionNo(LocalDateTime now) {
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 1; i <= 999; i++) {
            String no = String.format("YC-%s-%03d", datePart, i);
            if (qmsExceptionEventMapper.selectByExceptionNo(no) == null) {
                return no;
            }
        }
        return String.format("YC-%s-%s", datePart, System.nanoTime());
    }

    private String generate8dNo() {
        return String.format("8D-%s-%06d", LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")),
                System.currentTimeMillis() % 1_000_000);
    }

    private Map<String, Object> snapshot(Object... keyValues) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            snapshot.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return snapshot;
    }

    private void createResultUploadCopiesIfNeeded(QmsExceptionEventDO before, QmsExceptionEventDO after,
                                                  QmsExceptionEventHandleReqVO handleReqVO) {
        if (before == null || after == null || handleReqVO == null
                || !STATUS_RESULT_UPLOAD.equals(before.getStatus())
                || !STATUS_QA_CLOSURE.equals(after.getStatus())
                || StrUtil.isBlank(after.getProcessInstanceId())
                || CollUtil.isEmpty(handleReqVO.getCopyToUserIds())) {
            return;
        }
        List<Long> copyUserIds = distinctUserIds(handleReqVO.getCopyToUserIds()).stream()
                .filter(userId -> !Objects.equals(userId, after.getQaConfirmerId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(copyUserIds)) {
            return;
        }
        String reason = firstNotBlank(handleReqVO.getOpinion(), "执行结果上传完成，抄送查看");
        bpmProcessInstanceCopyService.createProcessInstanceCopy(copyUserIds, reason,
                after.getProcessInstanceId(), BPM_NODE_RESULT_UPLOAD, "执行结果上传", null);
        writeFlowLog(before, after, "COPY", "执行结果抄送", reason,
                snapshot("copyUserIds", joinLongIds(copyUserIds),
                        "copyUserNames", joinNames(handleReqVO.getCopyToUserNames())));
    }

    private void validateGroupTasksReady(List<QmsExceptionGroupTaskReqVO> tasks, String taskType,
                                         ErrorCode errorCode) {
        if (CollUtil.isEmpty(tasks)) {
            throw exception(errorCode);
        }
        boolean ready = tasks.stream()
                .filter(task -> taskType.equals(normalizeTaskType(task.getTaskType())))
                .anyMatch(task -> CollUtil.isNotEmpty(task.getMembers())
                        && task.getMembers().stream()
                        .anyMatch(member -> member.getUserId() != null || StrUtil.isNotBlank(member.getUserName())));
        if (!ready) {
            throw exception(errorCode);
        }
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user == null ? null : firstNotBlank(user.getNickname(), user.getUsername());
    }

    private String joinLongIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return null;
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String joinNames(List<String> names) {
        if (CollUtil.isEmpty(names)) {
            return null;
        }
        return names.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String currentUserName() {
        return StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private String buildMemberHandleReason(QmsExceptionGroupTaskMemberDO member, Long handlerUserId,
                                           String defaultReason) {
        if (member != null && handlerUserId != null && member.getUserId() != null
                && !Objects.equals(member.getUserId(), handlerUserId)) {
            return currentUserName() + "代" + firstNotBlank(member.getUserName(), "原办理人") + "办理";
        }
        return defaultReason;
    }

    private String buildMemberActualHandlerDisplay(QmsExceptionGroupTaskMemberDO member, Long handlerUserId) {
        if (member != null && handlerUserId != null && member.getUserId() != null
                && !Objects.equals(member.getUserId(), handlerUserId)) {
            return currentUserName() + "代" + firstNotBlank(member.getUserName(), "原办理人") + "办理";
        }
        return currentUserName();
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String defaultCode(String code, Long id) {
        return StrUtil.isNotBlank(code) ? code : id == null ? null : String.valueOf(id);
    }

    private void fillEventCodes(QmsExceptionEventDO event) {
        if (event == null) {
            return;
        }
        event.setDiscoverDeptCode(defaultCode(event.getDiscoverDeptCode(), event.getDiscoverDeptId()));
        event.setDiscovererCode(defaultCode(event.getDiscovererCode(), event.getDiscovererId()));
        event.setConfirmDeptCode(defaultCode(event.getConfirmDeptCode(), event.getConfirmDeptId()));
        event.setConfirmerCode(defaultCode(event.getConfirmerCode(), event.getConfirmerId()));
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
}
