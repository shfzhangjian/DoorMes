package cn.iocoder.yudao.module.mes.service.workflow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.convert.task.BpmTaskConvert;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MesWorkflowTaskServiceImpl implements MesWorkflowTaskService {

    private static final int MIN_FETCH_SIZE = 1000;
    private static final int MAX_FETCH_SIZE = 5000;

    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private List<MesWorkflowBusinessTaskProvider> businessTaskProviders;

    @Override
    public PageResult<BpmTaskRespVO> getTodoPage(BpmTaskPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        int fetchSize = resolveFetchSize(pageReqVO);
        PageResult<Task> bpmPage = bpmTaskService.getTaskTodoPage(loginUserId, copyPageReq(pageReqVO, fetchSize));
        List<BpmTaskRespVO> bpmRows = buildTodoRows(bpmPage);
        bpmRows.forEach(row -> row.setStatus(BpmTaskStatusEnum.RUNNING.getStatus()));
        List<BpmTaskRespVO> businessRows = businessTaskProviders.stream()
                .flatMap(provider -> provider.getTodoTasks(loginUserId, pageReqVO).stream())
                .collect(Collectors.toList());
        return mergeAndPage(pageReqVO, businessRows, bpmRows, overflowTotal(bpmPage, bpmRows));
    }

    @Override
    public PageResult<BpmTaskRespVO> getDonePage(BpmTaskPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        int fetchSize = resolveFetchSize(pageReqVO);
        PageResult<HistoricTaskInstance> bpmPage = bpmTaskService.getTaskDonePage(loginUserId, copyPageReq(pageReqVO, fetchSize));
        List<BpmTaskRespVO> bpmRows = buildDoneRows(bpmPage);
        List<BpmTaskRespVO> businessRows = businessTaskProviders.stream()
                .flatMap(provider -> provider.getDoneTasks(loginUserId, pageReqVO).stream())
                .collect(Collectors.toList());
        return mergeAndPage(pageReqVO, businessRows, bpmRows, overflowTotal(bpmPage, bpmRows));
    }

    private List<BpmTaskRespVO> buildTodoRows(PageResult<Task> pageResult) {
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return List.of();
        }
        Set<String> processInstanceIds = pageResult.getList().stream()
                .map(Task::getProcessInstanceId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, ProcessInstance> processInstanceMap = processInstanceService.getProcessInstanceMap(processInstanceIds);
        Set<Long> userIds = processInstanceMap.values().stream()
                .map(instance -> NumberUtils.parseLong(instance.getStartUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        pageResult.getList().stream()
                .map(task -> NumberUtils.parseLong(task.getAssignee()))
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<String, BpmProcessDefinitionInfoDO> processDefinitionInfoMap = processDefinitionService
                .getProcessDefinitionInfoMap(pageResult.getList().stream()
                        .map(Task::getProcessDefinitionId)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toSet()));
        PageResult<BpmTaskRespVO> voPage = BpmTaskConvert.INSTANCE.buildTodoTaskPage(
                pageResult, processInstanceMap, userMap, processDefinitionInfoMap);
        Map<String, Task> taskMap = pageResult.getList().stream()
                .collect(Collectors.toMap(Task::getId, task -> task, (left, right) -> left));
        voPage.getList().forEach(row -> fillTaskAssignee(row, taskMap.get(row.getId()), userMap));
        return voPage.getList();
    }

    private List<BpmTaskRespVO> buildDoneRows(PageResult<HistoricTaskInstance> pageResult) {
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return List.of();
        }
        Set<String> processInstanceIds = pageResult.getList().stream()
                .map(HistoricTaskInstance::getProcessInstanceId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, HistoricProcessInstance> processInstanceMap =
                processInstanceService.getHistoricProcessInstanceMap(processInstanceIds);
        Set<Long> userIds = processInstanceMap.values().stream()
                .map(instance -> NumberUtils.parseLong(instance.getStartUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        pageResult.getList().stream()
                .map(task -> NumberUtils.parseLong(task.getAssignee()))
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(userMap.values().stream()
                .map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<String, BpmProcessDefinitionInfoDO> processDefinitionInfoMap = processDefinitionService
                .getProcessDefinitionInfoMap(pageResult.getList().stream()
                        .map(HistoricTaskInstance::getProcessDefinitionId)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toSet()));
        return BpmTaskConvert.INSTANCE.buildTaskPage(
                pageResult, processInstanceMap, userMap, deptMap, processDefinitionInfoMap).getList();
    }

    private void fillTaskAssignee(BpmTaskRespVO row, Task task, Map<Long, AdminUserRespDTO> userMap) {
        if (row == null || task == null || StrUtil.isBlank(task.getAssignee())) {
            return;
        }
        AdminUserRespDTO user = userMap.get(NumberUtils.parseLong(task.getAssignee()));
        if (user == null) {
            return;
        }
        row.setAssignee(user.getId());
        row.setAssigneeUser(cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(
                user, cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO.class));
    }

    private PageResult<BpmTaskRespVO> mergeAndPage(BpmTaskPageReqVO pageReqVO, List<BpmTaskRespVO> businessRows,
                                                   List<BpmTaskRespVO> bpmRows, long overflowTotal) {
        Map<String, BpmTaskRespVO> rowMap = new LinkedHashMap<>();
        addRows(rowMap, businessRows);
        addRows(rowMap, bpmRows);
        List<BpmTaskRespVO> rows = new ArrayList<>(rowMap.values());
        rows.sort(Comparator.comparing(this::resolveRelationTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(row -> StrUtil.nullToDefault(row.getId(), ""), Comparator.reverseOrder()));
        int pageNo = Math.max(pageReqVO.getPageNo(), 1);
        int pageSize = Math.max(pageReqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), rows.size() + overflowTotal);
    }

    private void addRows(Map<String, BpmTaskRespVO> rowMap, Collection<BpmTaskRespVO> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        rows.stream()
                .filter(Objects::nonNull)
                .forEach(row -> rowMap.putIfAbsent(buildDedupeKey(row), row));
    }

    private String buildDedupeKey(BpmTaskRespVO row) {
        Map<String, Object> variables = row.getProcessInstance() == null
                || row.getProcessInstance().getFormVariables() == null
                ? Map.of() : row.getProcessInstance().getFormVariables();
        String businessType = stringValue(variables.get("mesWorkflowBusinessType"));
        String businessKey = row.getProcessInstance() == null ? "" : row.getProcessInstance().getBusinessKey();
        String processKey = row.getProcessInstance() == null || row.getProcessInstance().getProcessDefinition() == null
                ? "" : row.getProcessInstance().getProcessDefinition().getKey();
        if (isKnownMesProcess(processKey, variables) && StrUtil.isNotBlank(row.getProcessInstanceId())) {
            return "mes-process:" + row.getProcessInstanceId();
        }
        if (StrUtil.isNotBlank(businessType) && StrUtil.isNotBlank(businessKey)) {
            return "mes-business:" + businessType + ":" + businessKey;
        }
        return "bpm-task:" + StrUtil.nullToDefault(row.getId(), "");
    }

    private boolean isKnownMesProcess(String processKey, Map<String, Object> variables) {
        return StrUtil.equalsAny(processKey, "qms_exception_event", "qms_ncr_disposition",
                "qms_raw_material_ncr_disposition", "resource_device_exception")
                || variables.containsKey("exceptionNo")
                || variables.containsKey("exceptionEventId")
                || variables.containsKey("deviceExceptionId")
                || variables.containsKey("ncNo")
                || variables.containsKey("ncRecordId");
    }

    private LocalDateTime resolveRelationTime(BpmTaskRespVO row) {
        if (row.getEndTime() != null) {
            return row.getEndTime();
        }
        if (row.getCreateTime() != null) {
            return row.getCreateTime();
        }
        return row.getProcessInstance() == null ? null : row.getProcessInstance().getCreateTime();
    }

    private long overflowTotal(PageResult<?> pageResult, List<?> fetchedRows) {
        if (pageResult == null) {
            return 0;
        }
        return Math.max(0L, pageResult.getTotal() - (fetchedRows == null ? 0 : fetchedRows.size()));
    }

    private int resolveFetchSize(BpmTaskPageReqVO pageReqVO) {
        int pageNo = Math.max(pageReqVO.getPageNo(), 1);
        int pageSize = Math.max(pageReqVO.getPageSize(), 1);
        return Math.min(MAX_FETCH_SIZE, Math.max(MIN_FETCH_SIZE, pageNo * pageSize + MIN_FETCH_SIZE));
    }

    private BpmTaskPageReqVO copyPageReq(BpmTaskPageReqVO source, int pageSize) {
        BpmTaskPageReqVO target = new BpmTaskPageReqVO();
        target.setPageNo(1);
        target.setPageSize(pageSize);
        target.setName(source.getName());
        target.setCategory(source.getCategory());
        target.setProcessDefinitionKey(source.getProcessDefinitionKey());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        return target;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

}
