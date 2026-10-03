package cn.iocoder.yudao.module.mes.service.workflow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.process.BpmProcessDefinitionRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceExceptionMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ResourceDeviceExceptionWorkflowTaskProvider implements MesWorkflowBusinessTaskProvider {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int FETCH_SIZE = 5000;
    private static final String PROCESS_KEY = "resource_device_exception";
    private static final String FORM_VIEW_PATH = "/mes/resource/device/fault-repair";
    private static final String STATUS_REPORTED = "REPORTED";
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_PENDING_ARCHIVE = "PENDING_ARCHIVE";
    private static final String STATUS_CLOSED = "CLOSED";

    @Resource
    private ResourceDeviceExceptionMapper exceptionMapper;

    @Override
    public List<BpmTaskRespVO> getTodoTasks(Long userId, BpmTaskPageReqVO pageReqVO) {
        if (!matchesProcessDefinition(pageReqVO) || !matchesStatus(pageReqVO, false)) {
            return List.of();
        }
        ResourceDeviceExceptionPageReqVO reqVO = new ResourceDeviceExceptionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(FETCH_SIZE);
        reqVO.setTabType("todo");
        reqVO.setCurrentUserId(userId);
        reqVO.setCreateTime(pageReqVO.getCreateTime());
        PageResult<ResourceDeviceExceptionDO> pageResult = exceptionMapper.selectPage(reqVO);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return List.of();
        }
        return pageResult.getList().stream()
                .filter(exception -> matchesKeyword(pageReqVO, exception))
                .map(exception -> buildTodoRow(userId, exception))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<BpmTaskRespVO> getDoneTasks(Long userId, BpmTaskPageReqVO pageReqVO) {
        if (!matchesProcessDefinition(pageReqVO) || !matchesStatus(pageReqVO, true)) {
            return List.of();
        }
        ResourceDeviceExceptionPageReqVO reqVO = new ResourceDeviceExceptionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(FETCH_SIZE);
        PageResult<ResourceDeviceExceptionDO> pageResult = exceptionMapper.selectPage(reqVO);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return List.of();
        }
        return pageResult.getList().stream()
                .map(exception -> buildDoneRow(userId, pageReqVO, exception))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BpmTaskRespVO::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private BpmTaskRespVO buildTodoRow(Long userId, ResourceDeviceExceptionDO exception) {
        BpmTaskRespVO row = new BpmTaskRespVO();
        row.setId("mes-device-exception-" + resolveActionCode(exception.getStatus()) + "-" + exception.getId());
        row.setName(resolveActionName(exception.getStatus()));
        row.setCreateTime(firstNonNull(resolveNodeTime(exception), exception.getReportTime(), exception.getCreateTime()));
        row.setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        row.setAssignee(resolveHandlerUserId(exception, userId));
        row.setAssigneeUser(buildUser(resolveHandlerUserId(exception, userId), resolveHandlerUserName(exception)));
        row.setTaskDefinitionKey(resolveNodeCode(exception.getStatus()));
        row.setProcessInstanceId(resolveProcessInstanceId(exception));
        row.setProcessInstance(buildProcessInstance(exception, buildVariables(exception, false)));
        return row;
    }

    private BpmTaskRespVO buildDoneRow(Long userId, BpmTaskPageReqVO pageReqVO, ResourceDeviceExceptionDO exception) {
        DoneStep doneStep = resolveDoneStep(userId, exception);
        if (doneStep == null || !matchesCreateTime(pageReqVO, doneStep.handleTime)
                || !matchesKeyword(pageReqVO, exception)) {
            return null;
        }
        BpmTaskRespVO row = new BpmTaskRespVO();
        row.setId("mes-device-exception-done-" + doneStep.actionCode + "-" + exception.getId());
        row.setName("已办理" + doneStep.actionName);
        row.setCreateTime(firstNonNull(exception.getReportTime(), exception.getCreateTime()));
        row.setEndTime(doneStep.handleTime);
        row.setStatus(BpmTaskStatusEnum.APPROVE.getStatus());
        row.setReason(doneStep.opinion);
        row.setAssignee(userId);
        row.setAssigneeUser(buildUser(userId, doneStep.handlerName));
        row.setTaskDefinitionKey(doneStep.nodeCode);
        row.setProcessInstanceId(resolveProcessInstanceId(exception));
        row.setProcessInstance(buildProcessInstance(exception, buildVariables(exception, true)));
        return row;
    }

    private BpmTaskRespVO.ProcessInstance buildProcessInstance(ResourceDeviceExceptionDO exception,
                                                              Map<String, Object> variables) {
        BpmTaskRespVO.ProcessInstance instance = new BpmTaskRespVO.ProcessInstance();
        instance.setId(resolveProcessInstanceId(exception));
        instance.setName("设备异常报修流程");
        instance.setCreateTime(firstNonNull(exception.getReportTime(), exception.getCreateTime()));
        instance.setBusinessKey(String.valueOf(exception.getId()));
        instance.setFormVariables(variables);
        instance.setProcessDefinition(buildProcessDefinition());
        instance.setStartUser(buildUser(exception.getReporterId(), exception.getReporter()));
        return instance;
    }

    private BpmProcessDefinitionRespVO buildProcessDefinition() {
        BpmProcessDefinitionRespVO definition = new BpmProcessDefinitionRespVO();
        definition.setName("设备异常报修流程");
        definition.setKey(PROCESS_KEY);
        definition.setFormCustomViewPath(FORM_VIEW_PATH);
        return definition;
    }

    private Map<String, Object> buildVariables(ResourceDeviceExceptionDO exception, boolean done) {
        return Map.ofEntries(
                Map.entry("mesWorkflowBusinessType", "DEVICE_EXCEPTION"),
                Map.entry("mesWorkflowBusinessTaskType", resolveActionCode(exception.getStatus())),
                Map.entry("mesWorkflowBusinessDone", done),
                Map.entry("deviceExceptionId", exception.getId()),
                Map.entry("deviceExceptionNo", blank(exception.getExceptionNo())),
                Map.entry("deviceCode", blank(exception.getDeviceCode())),
                Map.entry("deviceName", blank(exception.getDeviceName())),
                Map.entry("exceptionLevel", blank(exception.getExceptionLevel())),
                Map.entry("reportTime", format(exception.getReportTime())),
                Map.entry("faultDesc", blank(exception.getFaultDesc())),
                Map.entry("currentNodeName", resolveNodeName(exception.getStatus())),
                Map.entry("currentUserTaskTodo", !done && !STATUS_CLOSED.equals(exception.getStatus())),
                Map.entry("currentUserTaskTodoLabel", done ? "" : resolveActionName(exception.getStatus()))
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
        return Objects.equals(status, done ? BpmTaskStatusEnum.APPROVE.getStatus()
                : BpmTaskStatusEnum.RUNNING.getStatus());
    }

    private boolean matchesCreateTime(BpmTaskPageReqVO pageReqVO, LocalDateTime value) {
        LocalDateTime[] range = pageReqVO.getCreateTime();
        if (range == null || range.length < 2 || value == null) {
            return true;
        }
        return !value.isBefore(range[0]) && !value.isAfter(range[1]);
    }

    private boolean matchesKeyword(BpmTaskPageReqVO pageReqVO, ResourceDeviceExceptionDO exception) {
        String keyword = StrUtil.trimToEmpty(pageReqVO.getName());
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(exception.getExceptionNo(), keyword)
                || StrUtil.containsIgnoreCase("设备异常报修流程", keyword)
                || StrUtil.containsIgnoreCase(resolveNodeName(exception.getStatus()), keyword)
                || StrUtil.containsIgnoreCase(exception.getDeviceCode(), keyword)
                || StrUtil.containsIgnoreCase(exception.getDeviceName(), keyword)
                || StrUtil.containsIgnoreCase(exception.getFaultDesc(), keyword);
    }

    private DoneStep resolveDoneStep(Long userId, ResourceDeviceExceptionDO exception) {
        DoneStep latest = null;
        latest = preferLatest(latest, buildDoneStep(userId, exception.getDispatcherId(), exception.getDispatcher(),
                "RESPOND", "响应分派", "response_dispatch", exception.getDispatchTime(), exception.getResponseRemark()));
        latest = preferLatest(latest, buildDoneStep(userId, exception.getAssigneeId(), exception.getAssignee(),
                "REPAIR", "维修执行", "repair_execute", exception.getRepairTime(), exception.getRepairAction()));
        latest = preferLatest(latest, buildDoneStep(userId, exception.getConfirmerId(), exception.getConfirmer(),
                "CONFIRM", "完成确认", "finish_confirm", exception.getConfirmTime(), exception.getConfirmRemark()));
        latest = preferLatest(latest, buildDoneStep(userId, exception.getArchiverId(), exception.getArchiver(),
                "ARCHIVE", "关闭归档", "close_archive", exception.getArchiveTime(), exception.getArchiveReason()));
        return latest;
    }

    private DoneStep buildDoneStep(Long userId, Long handlerId, String handlerName, String actionCode,
                                   String actionName, String nodeCode, LocalDateTime handleTime, String opinion) {
        if (userId == null || handlerId == null || !Objects.equals(userId, handlerId) || handleTime == null) {
            return null;
        }
        return new DoneStep(actionCode, actionName, nodeCode, handleTime, handlerName, opinion);
    }

    private DoneStep preferLatest(DoneStep left, DoneStep right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.handleTime.isAfter(left.handleTime) ? right : left;
    }

    private Long resolveHandlerUserId(ResourceDeviceExceptionDO exception, Long fallbackUserId) {
        if (STATUS_DISPATCHED.equals(exception.getStatus())) {
            return exception.getAssigneeId();
        }
        if (STATUS_PENDING_CONFIRM.equals(exception.getStatus())) {
            return exception.getReporterId();
        }
        if (STATUS_PENDING_ARCHIVE.equals(exception.getStatus())) {
            return exception.getDispatcherId();
        }
        return fallbackUserId;
    }

    private String resolveHandlerUserName(ResourceDeviceExceptionDO exception) {
        if (STATUS_DISPATCHED.equals(exception.getStatus())) {
            return exception.getAssignee();
        }
        if (STATUS_PENDING_CONFIRM.equals(exception.getStatus())) {
            return exception.getReporter();
        }
        if (STATUS_PENDING_ARCHIVE.equals(exception.getStatus())) {
            return exception.getDispatcher();
        }
        return "";
    }

    private LocalDateTime resolveNodeTime(ResourceDeviceExceptionDO exception) {
        if (STATUS_DISPATCHED.equals(exception.getStatus())) {
            return exception.getDispatchTime();
        }
        if (STATUS_PENDING_CONFIRM.equals(exception.getStatus())) {
            return firstNonNull(exception.getRepairTime(), exception.getDispatchTime());
        }
        if (STATUS_PENDING_ARCHIVE.equals(exception.getStatus())) {
            return exception.getConfirmTime();
        }
        return exception.getReportTime();
    }

    private String resolveProcessInstanceId(ResourceDeviceExceptionDO exception) {
        return "mes-device-exception-" + exception.getId();
    }

    private String resolveNodeCode(String status) {
        if (STATUS_REPORTED.equals(status)) {
            return "response_dispatch";
        }
        if (STATUS_DISPATCHED.equals(status)) {
            return "repair_execute";
        }
        if (STATUS_PENDING_CONFIRM.equals(status)) {
            return "finish_confirm";
        }
        if (STATUS_PENDING_ARCHIVE.equals(status)) {
            return "close_archive";
        }
        return "archived";
    }

    private String resolveNodeName(String status) {
        if (STATUS_REPORTED.equals(status)) {
            return "响应分派";
        }
        if (STATUS_DISPATCHED.equals(status)) {
            return "维修执行";
        }
        if (STATUS_PENDING_CONFIRM.equals(status)) {
            return "完成确认";
        }
        if (STATUS_PENDING_ARCHIVE.equals(status)) {
            return "关闭归档";
        }
        return "已关闭归档";
    }

    private String resolveActionCode(String status) {
        if (STATUS_REPORTED.equals(status)) {
            return "RESPOND";
        }
        if (STATUS_DISPATCHED.equals(status)) {
            return "REPAIR";
        }
        if (STATUS_PENDING_CONFIRM.equals(status)) {
            return "CONFIRM";
        }
        if (STATUS_PENDING_ARCHIVE.equals(status)) {
            return "ARCHIVE";
        }
        return "VIEW";
    }

    private String resolveActionName(String status) {
        if (STATUS_REPORTED.equals(status)) {
            return "响应分派";
        }
        if (STATUS_DISPATCHED.equals(status)) {
            return "维修执行";
        }
        if (STATUS_PENDING_CONFIRM.equals(status)) {
            return "完成确认";
        }
        if (STATUS_PENDING_ARCHIVE.equals(status)) {
            return "关闭归档";
        }
        return "详情";
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

    private String blank(String value) {
        return StrUtil.blankToDefault(value, "");
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATETIME_FORMATTER);
    }

    private static final class DoneStep {
        private final String actionCode;
        private final String actionName;
        private final String nodeCode;
        private final LocalDateTime handleTime;
        private final String handlerName;
        private final String opinion;

        private DoneStep(String actionCode, String actionName, String nodeCode, LocalDateTime handleTime,
                         String handlerName, String opinion) {
            this.actionCode = actionCode;
            this.actionName = actionName;
            this.nodeCode = nodeCode;
            this.handleTime = handleTime;
            this.handlerName = handlerName;
            this.opinion = opinion;
        }
    }

}
