package cn.iocoder.yudao.module.mes.service.workflow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.process.BpmProcessDefinitionRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsExceptionGroupTaskMemberDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionGroupTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsExceptionGroupTaskMemberMapper;
import cn.iocoder.yudao.module.mes.service.qms.QmsExceptionEventService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QmsExceptionWorkflowTaskProvider implements MesWorkflowBusinessTaskProvider {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int FETCH_SIZE = 5000;
    private static final String PROCESS_KEY = "qms_exception_event";
    private static final String FORM_VIEW_PATH = "/mes/quality/abnormal/exception";
    private static final String GROUP_TASK_TYPE_ROOT_CAUSE = "ROOT_CAUSE_PREVENTIVE";

    @Resource
    private QmsExceptionEventService qmsExceptionEventService;
    @Resource
    private QmsExceptionEventMapper qmsExceptionEventMapper;
    @Resource
    private QmsExceptionGroupTaskMapper qmsExceptionGroupTaskMapper;
    @Resource
    private QmsExceptionGroupTaskMemberMapper qmsExceptionGroupTaskMemberMapper;

    @Override
    public List<BpmTaskRespVO> getTodoTasks(Long userId, BpmTaskPageReqVO pageReqVO) {
        if (!matchesProcessDefinition(pageReqVO)) {
            return List.of();
        }
        QmsExceptionEventPageReqVO reqVO = new QmsExceptionEventPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(FETCH_SIZE);
        reqVO.setTabType("todo");
        reqVO.setDiscoverTime(pageReqVO.getCreateTime());
        PageResult<QmsExceptionEventRespVO> pageResult = qmsExceptionEventService.getExceptionEventPage(reqVO);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return List.of();
        }
        return pageResult.getList().stream()
                .filter(event -> Boolean.TRUE.equals(event.getCurrentUserTaskTodo())
                        || (Boolean.TRUE.equals(event.getCanHandle()) && isMainFlowTodoStatus(event.getStatus())))
                .filter(event -> matchesStatus(pageReqVO, false))
                .filter(event -> matchesKeyword(pageReqVO, event))
                .map(event -> buildTodoRow(userId, event))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<BpmTaskRespVO> getDoneTasks(Long userId, BpmTaskPageReqVO pageReqVO) {
        if (!matchesProcessDefinition(pageReqVO) || !matchesStatus(pageReqVO, true)) {
            return List.of();
        }
        List<QmsExceptionGroupTaskMemberDO> members =
                qmsExceptionGroupTaskMemberMapper.selectHandledListByActualHandlerUserId(userId);
        if (CollUtil.isEmpty(members)) {
            return List.of();
        }
        Set<Long> taskIds = members.stream()
                .map(QmsExceptionGroupTaskMemberDO::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(taskIds)) {
            return List.of();
        }
        Map<Long, QmsExceptionGroupTaskDO> taskMap = qmsExceptionGroupTaskMapper.selectListByIds(taskIds)
                .stream()
                .collect(Collectors.toMap(QmsExceptionGroupTaskDO::getId, task -> task, (left, right) -> left));
        Set<Long> exceptionIds = taskMap.values().stream()
                .map(QmsExceptionGroupTaskDO::getExceptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QmsExceptionEventDO> eventMap = CollUtil.isEmpty(exceptionIds) ? Map.of()
                : qmsExceptionEventMapper.selectBatchIds(exceptionIds).stream()
                .collect(Collectors.toMap(QmsExceptionEventDO::getId, event -> event, (left, right) -> left));
        return members.stream()
                .map(member -> buildDoneRow(pageReqVO, eventMap.get(member.getExceptionId()),
                        taskMap.get(member.getTaskId()), member))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(row -> row.getProcessInstance().getBusinessKey(), row -> row,
                        this::preferNewerRow))
                .values()
                .stream()
                .sorted(Comparator.comparing(this::resolveRelationTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private BpmTaskRespVO buildTodoRow(Long userId, QmsExceptionEventRespVO event) {
        String processInstanceId = StrUtil.blankToDefault(event.getProcessInstanceId(), "mes-exception-" + event.getId());
        BpmTaskRespVO row = new BpmTaskRespVO();
        boolean groupTaskTodo = Boolean.TRUE.equals(event.getCurrentUserTaskTodo());
        row.setId((groupTaskTodo ? "mes-exception-group-task-" : "mes-exception-handle-") + event.getId());
        row.setName(firstNotBlank(event.getCurrentUserTaskTodoLabel(), event.getListActionName(),
                event.getCurrentNodeName(), "异常事件办理"));
        row.setCreateTime(firstNonNull(event.getCreateTime(), event.getDiscoverTime()));
        row.setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        row.setAssignee(userId);
        row.setAssigneeUser(buildUser(userId, null));
        row.setTaskDefinitionKey(groupTaskTodo
                ? resolveTaskDefinitionKey(event.getCurrentUserTaskTodoType())
                : resolveTaskDefinitionKeyByStatus(event.getStatus()));
        row.setProcessInstanceId(processInstanceId);
        row.setProcessInstance(buildProcessInstance(event, processInstanceId, buildVariables(event, false)));
        return row;
    }

    private BpmTaskRespVO buildDoneRow(BpmTaskPageReqVO pageReqVO, QmsExceptionEventDO event,
                                       QmsExceptionGroupTaskDO task, QmsExceptionGroupTaskMemberDO member) {
        if (event == null || task == null || member == null
                || !matchesCreateTime(pageReqVO, firstNonNull(member.getConfirmTime(), member.getActualFinishTime()))
                || !matchesKeyword(pageReqVO, event, task, member)) {
            return null;
        }
        String processInstanceId = StrUtil.blankToDefault(event.getProcessInstanceId(), "mes-exception-" + event.getId());
        BpmTaskRespVO row = new BpmTaskRespVO();
        row.setId("mes-exception-group-member-" + member.getId());
        row.setName(resolveDoneLabel(task));
        row.setCreateTime(firstNonNull(task.getDispatchTime(), event.getCreateTime(), event.getDiscoverTime()));
        row.setEndTime(firstNonNull(member.getConfirmTime(), member.getActualFinishTime()));
        row.setStatus(BpmTaskStatusEnum.APPROVE.getStatus());
        row.setReason(member.getConfirmRemark());
        row.setAssignee(member.getActualHandlerUserId());
        row.setAssigneeUser(buildUser(member.getActualHandlerUserId(), member.getActualHandlerUserName()));
        row.setTaskDefinitionKey(resolveTaskDefinitionKey(task.getTaskType()));
        row.setProcessInstanceId(processInstanceId);
        row.setProcessInstance(buildProcessInstance(event, processInstanceId,
                buildVariables(event, task, member, true)));
        return row;
    }

    private BpmTaskRespVO.ProcessInstance buildProcessInstance(QmsExceptionEventRespVO event, String processInstanceId,
                                                              Map<String, Object> variables) {
        BpmTaskRespVO.ProcessInstance instance = new BpmTaskRespVO.ProcessInstance();
        instance.setId(processInstanceId);
        instance.setName("异常事件提报流程");
        instance.setCreateTime(event.getCreateTime());
        instance.setBusinessKey(String.valueOf(event.getId()));
        instance.setFormVariables(variables);
        instance.setProcessDefinition(buildProcessDefinition());
        instance.setStartUser(buildUser(event.getDiscovererId(), event.getDiscovererName()));
        return instance;
    }

    private BpmTaskRespVO.ProcessInstance buildProcessInstance(QmsExceptionEventDO event, String processInstanceId,
                                                              Map<String, Object> variables) {
        BpmTaskRespVO.ProcessInstance instance = new BpmTaskRespVO.ProcessInstance();
        instance.setId(processInstanceId);
        instance.setName("异常事件提报流程");
        instance.setCreateTime(event.getCreateTime());
        instance.setBusinessKey(String.valueOf(event.getId()));
        instance.setFormVariables(variables);
        instance.setProcessDefinition(buildProcessDefinition());
        instance.setStartUser(buildUser(event.getDiscovererId(), event.getDiscovererName()));
        return instance;
    }

    private BpmProcessDefinitionRespVO buildProcessDefinition() {
        BpmProcessDefinitionRespVO definition = new BpmProcessDefinitionRespVO();
        definition.setName("异常事件提报流程");
        definition.setKey(PROCESS_KEY);
        definition.setFormCustomViewPath(FORM_VIEW_PATH);
        return definition;
    }

    private Map<String, Object> buildVariables(QmsExceptionEventRespVO event, boolean done) {
        return Map.ofEntries(
                Map.entry("mesWorkflowBusinessType", "EXCEPTION"),
                Map.entry("mesWorkflowBusinessTaskType", Boolean.TRUE.equals(event.getCurrentUserTaskTodo())
                        ? "EXCEPTION_GROUP_TASK" : "EXCEPTION_HANDLE"),
                Map.entry("mesWorkflowBusinessDone", done),
                Map.entry("exceptionEventId", event.getId()),
                Map.entry("exceptionNo", blank(event.getExceptionNo())),
                Map.entry("exceptionType", blank(event.getExceptionType())),
                Map.entry("exceptionLevel", blank(event.getExceptionLevel())),
                Map.entry("discoverTime", format(event.getDiscoverTime())),
                Map.entry("discoverDeptName", blank(event.getDiscoverDeptName())),
                Map.entry("description", blank(event.getDescription())),
                Map.entry("currentNodeName", blank(event.getCurrentNodeName())),
                Map.entry("currentUserTaskTodo", Boolean.TRUE.equals(event.getCurrentUserTaskTodo())),
                Map.entry("currentUserTaskTodoLabel", blank(event.getCurrentUserTaskTodoLabel())),
                Map.entry("currentUserTaskDone", Boolean.TRUE.equals(event.getCurrentUserTaskDone())),
                Map.entry("currentUserTaskDoneLabel", blank(event.getCurrentUserTaskDoneLabel()))
        );
    }

    private Map<String, Object> buildVariables(QmsExceptionEventDO event, QmsExceptionGroupTaskDO task,
                                               QmsExceptionGroupTaskMemberDO member, boolean done) {
        return Map.ofEntries(
                Map.entry("mesWorkflowBusinessType", "EXCEPTION"),
                Map.entry("mesWorkflowBusinessTaskType", "EXCEPTION_GROUP_TASK"),
                Map.entry("mesWorkflowBusinessTaskId", task.getId()),
                Map.entry("mesWorkflowBusinessMemberId", member.getId()),
                Map.entry("mesWorkflowBusinessDone", done),
                Map.entry("exceptionEventId", event.getId()),
                Map.entry("exceptionNo", blank(event.getExceptionNo())),
                Map.entry("exceptionType", blank(event.getExceptionType())),
                Map.entry("exceptionLevel", blank(event.getExceptionLevel())),
                Map.entry("discoverTime", format(event.getDiscoverTime())),
                Map.entry("discoverDeptName", blank(event.getDiscoverDeptName())),
                Map.entry("description", blank(event.getDescription())),
                Map.entry("currentNodeName", blank(event.getCurrentNodeName())),
                Map.entry("currentUserTaskDone", true),
                Map.entry("currentUserTaskDoneLabel", resolveDoneLabel(task))
        );
    }

    private boolean matchesProcessDefinition(BpmTaskPageReqVO pageReqVO) {
        return StrUtil.isBlank(pageReqVO.getProcessDefinitionKey())
                || PROCESS_KEY.equals(pageReqVO.getProcessDefinitionKey());
    }

    private boolean matchesStatus(BpmTaskPageReqVO pageReqVO, boolean done) {
        Integer status = pageReqVO.getStatus();
        if (status == null) {
            return true;
        }
        return Objects.equals(status, done ? BpmTaskStatusEnum.APPROVE.getStatus() : BpmTaskStatusEnum.RUNNING.getStatus());
    }

    private boolean matchesCreateTime(BpmTaskPageReqVO pageReqVO, LocalDateTime value) {
        LocalDateTime[] range = pageReqVO.getCreateTime();
        if (range == null || range.length < 2 || value == null) {
            return true;
        }
        return !value.isBefore(range[0]) && !value.isAfter(range[1]);
    }

    private boolean matchesKeyword(BpmTaskPageReqVO pageReqVO, QmsExceptionEventDO event,
                                   QmsExceptionGroupTaskDO task, QmsExceptionGroupTaskMemberDO member) {
        String keyword = StrUtil.trimToEmpty(pageReqVO.getName());
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(event.getExceptionNo(), keyword)
                || StrUtil.containsIgnoreCase("异常事件提报流程", keyword)
                || StrUtil.containsIgnoreCase(event.getCurrentNodeName(), keyword)
                || StrUtil.containsIgnoreCase(event.getDescription(), keyword)
                || StrUtil.containsIgnoreCase(task.getGroupName(), keyword)
                || StrUtil.containsIgnoreCase(task.getTaskType(), keyword)
                || StrUtil.containsIgnoreCase(member.getUserName(), keyword)
                || StrUtil.containsIgnoreCase(member.getDelegateUserName(), keyword)
                || StrUtil.containsIgnoreCase(member.getActualHandlerUserName(), keyword);
    }

    private boolean matchesKeyword(BpmTaskPageReqVO pageReqVO, QmsExceptionEventRespVO event) {
        String keyword = StrUtil.trimToEmpty(pageReqVO.getName());
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(event.getExceptionNo(), keyword)
                || StrUtil.containsIgnoreCase("异常事件提报流程", keyword)
                || StrUtil.containsIgnoreCase(event.getCurrentNodeName(), keyword)
                || StrUtil.containsIgnoreCase(event.getCurrentUserTaskTodoLabel(), keyword)
                || StrUtil.containsIgnoreCase(event.getDescription(), keyword)
                || StrUtil.containsIgnoreCase(event.getDiscoverDeptName(), keyword);
    }

    private BpmTaskRespVO preferNewerRow(BpmTaskRespVO left, BpmTaskRespVO right) {
        LocalDateTime leftTime = resolveRelationTime(left);
        LocalDateTime rightTime = resolveRelationTime(right);
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return leftTime.isAfter(rightTime) ? left : right;
    }

    private LocalDateTime resolveRelationTime(BpmTaskRespVO row) {
        return row.getEndTime() != null ? row.getEndTime() : row.getCreateTime();
    }

    private String resolveTaskDefinitionKey(String taskType) {
        return GROUP_TASK_TYPE_ROOT_CAUSE.equals(taskType) ? "root_cause_member_sign" : "containment_member_sign";
    }

    private String resolveTaskDefinitionKeyByStatus(String status) {
        if ("CONFIRMING".equals(status)) {
            return "quality_confirm";
        }
        if ("CONTAINMENT".equals(status)) {
            return "containment_owner_handle";
        }
        if ("CONTAINMENT_SIGN".equals(status)) {
            return "containment_member_sign";
        }
        if ("RESPONSIBILITY_CONFIRM".equals(status)) {
            return "responsibility_confirm";
        }
        if ("ROOT_CAUSE".equals(status)) {
            return "root_cause_owner_handle";
        }
        if ("ROOT_CAUSE_SIGN".equals(status)) {
            return "root_cause_member_sign";
        }
        if ("RESULT_UPLOADER_ASSIGN".equals(status)) {
            return "result_uploader_assign";
        }
        if ("RESULT_UPLOAD".equals(status)) {
            return "result_upload";
        }
        if ("QA_CLOSURE".equals(status) || "VERIFYING".equals(status)) {
            return "qa_closure";
        }
        return "quality_confirm";
    }

    private boolean isMainFlowTodoStatus(String status) {
        return !"CONTAINMENT_SIGN".equals(status) && !"ROOT_CAUSE_SIGN".equals(status);
    }

    private String resolveDoneLabel(QmsExceptionGroupTaskDO task) {
        return GROUP_TASK_TYPE_ROOT_CAUSE.equals(task.getTaskType()) ? "已提交根因纠正" : "已处理围堵措施";
    }

    private UserSimpleBaseVO buildUser(Long id, String name) {
        if (id == null && StrUtil.isBlank(name)) {
            return null;
        }
        UserSimpleBaseVO user = new UserSimpleBaseVO();
        user.setId(id);
        user.setNickname(StrUtil.blankToDefault(name, id == null ? "" : String.valueOf(id)));
        return user;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
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
            return "";
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String blank(String value) {
        return StrUtil.blankToDefault(value, "");
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATETIME_FORMATTER);
    }

}
