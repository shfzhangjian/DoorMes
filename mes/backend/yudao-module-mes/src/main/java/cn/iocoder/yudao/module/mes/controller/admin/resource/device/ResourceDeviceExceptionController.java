package cn.iocoder.yudao.module.mes.controller.admin.resource.device;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionProcessReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionDO;
import cn.iocoder.yudao.module.mes.service.resource.device.ResourceDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备异常台账")
@RestController
@RequestMapping("/mes/resource/device/exception")
@Validated
public class ResourceDeviceExceptionController {

    private static final String STATUS_REPORTED = "REPORTED";
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_PENDING_ARCHIVE = "PENDING_ARCHIVE";
    private static final String STATUS_CLOSED = "CLOSED";

    @Resource
    private ResourceDeviceService resourceDeviceService;

    @PostMapping("/create")
    @Operation(summary = "创建设备异常")
    public CommonResult<Long> createException(@Valid @RequestBody ResourceDeviceExceptionSaveReqVO reqVO) {
        return success(resourceDeviceService.createException(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备异常")
    public CommonResult<Boolean> updateException(
            @Validated({Default.class, ResourceDeviceExceptionSaveReqVO.Update.class})
            @RequestBody ResourceDeviceExceptionSaveReqVO reqVO) {
        resourceDeviceService.updateException(reqVO);
        return success(true);
    }

    @PostMapping("/process")
    @Operation(summary = "办理设备异常流程")
    public CommonResult<Boolean> processException(@Valid @RequestBody ResourceDeviceExceptionProcessReqVO reqVO) {
        resourceDeviceService.processException(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备异常")
    public CommonResult<Boolean> deleteException(@RequestParam("id") Long id) {
        resourceDeviceService.deleteException(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备异常")
    public CommonResult<ResourceDeviceExceptionRespVO> getException(@RequestParam("id") Long id) {
        return success(buildResp(resourceDeviceService.getException(id), SecurityFrameworkUtils.getLoginUserId()));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备异常分页")
    public CommonResult<PageResult<ResourceDeviceExceptionRespVO>> getExceptionPage(
            @Valid ResourceDeviceExceptionPageReqVO reqVO) {
        reqVO.setCurrentUserId(SecurityFrameworkUtils.getLoginUserId());
        PageResult<ResourceDeviceExceptionDO> page = resourceDeviceService.getExceptionPage(reqVO);
        PageResult<ResourceDeviceExceptionRespVO> resp = BeanUtils.toBean(page, ResourceDeviceExceptionRespVO.class);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        resp.getList().forEach(item -> fillWorkflowState(item, loginUserId));
        return success(resp);
    }

    @GetMapping("/part/list")
    @Operation(summary = "获得设备异常消耗备件")
    public CommonResult<List<ResourceDeviceExceptionRespVO.RepairPart>> getExceptionPartList(
            @RequestParam("exceptionId") Long exceptionId) {
        return success(BeanUtils.toBean(resourceDeviceService.getExceptionPartList(exceptionId),
                ResourceDeviceExceptionRespVO.RepairPart.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备异常 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExceptionExcel(@Valid ResourceDeviceExceptionPageReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ResourceDeviceExceptionDO> list = resourceDeviceService.getExceptionPage(reqVO).getList();
        ExcelUtils.write(response, "设备异常台账.xls", "数据", ResourceDeviceExceptionRespVO.class,
                BeanUtils.toBean(list, ResourceDeviceExceptionRespVO.class));
    }

    private ResourceDeviceExceptionRespVO buildResp(ResourceDeviceExceptionDO exception, Long loginUserId) {
        ResourceDeviceExceptionRespVO respVO = BeanUtils.toBean(exception, ResourceDeviceExceptionRespVO.class);
        if (respVO == null || exception == null) {
            return respVO;
        }
        fillWorkflowState(respVO, loginUserId);
        respVO.setParts(BeanUtils.toBean(resourceDeviceService.getExceptionPartList(exception.getId()),
                ResourceDeviceExceptionRespVO.RepairPart.class));
        respVO.setFlowLogs(buildFlowLogs(exception));
        return respVO;
    }

    private void fillWorkflowState(ResourceDeviceExceptionRespVO respVO, Long loginUserId) {
        if (respVO == null) {
            return;
        }
        respVO.setOrderNo(respVO.getExceptionNo());
        respVO.setProcessInstanceId("mes-device-exception-" + respVO.getId());
        respVO.setCurrentNodeCode(resolveNodeCode(respVO.getStatus()));
        respVO.setCurrentNodeName(resolveNodeName(respVO.getStatus()));
        fillCurrentHandler(respVO);
        boolean canHandle = canHandle(respVO, loginUserId);
        respVO.setCanHandle(canHandle);
        respVO.setCurrentUserTaskTodo(canHandle);
        respVO.setCurrentUserTaskTodoCount(canHandle ? 1 : 0);
        respVO.setCurrentUserTaskTodoType(resolveActionCode(respVO.getStatus()));
        respVO.setCurrentUserTaskTodoLabel(canHandle ? resolveActionName(respVO.getStatus()) : "");
        respVO.setCurrentUserTaskDone(false);
        respVO.setCurrentUserTaskDoneCount(0);
        respVO.setCurrentUserTaskDoneType("");
        respVO.setCurrentUserTaskDoneLabel("");
        respVO.setListActionCode(canHandle ? resolveActionCode(respVO.getStatus()) : "VIEW");
        respVO.setListActionName(canHandle ? resolveActionName(respVO.getStatus()) : "详情");
    }

    private void fillCurrentHandler(ResourceDeviceExceptionRespVO respVO) {
        if (STATUS_DISPATCHED.equals(respVO.getStatus())) {
            respVO.setCurrentHandlerUserId(respVO.getAssigneeId());
            respVO.setCurrentHandlerUserName(respVO.getAssignee());
            return;
        }
        if (STATUS_PENDING_CONFIRM.equals(respVO.getStatus())) {
            respVO.setCurrentHandlerUserId(respVO.getReporterId());
            respVO.setCurrentHandlerUserName(respVO.getReporter());
            return;
        }
        if (STATUS_PENDING_ARCHIVE.equals(respVO.getStatus())) {
            respVO.setCurrentHandlerUserId(respVO.getDispatcherId());
            respVO.setCurrentHandlerUserName(respVO.getDispatcher());
        }
    }

    private boolean canHandle(ResourceDeviceExceptionRespVO respVO, Long loginUserId) {
        if (respVO == null || STATUS_CLOSED.equals(respVO.getStatus())) {
            return false;
        }
        if (STATUS_REPORTED.equals(respVO.getStatus())) {
            return true;
        }
        if (loginUserId == null) {
            return false;
        }
        if (STATUS_DISPATCHED.equals(respVO.getStatus())) {
            return Objects.equals(respVO.getAssigneeId(), loginUserId);
        }
        if (STATUS_PENDING_CONFIRM.equals(respVO.getStatus())) {
            return Objects.equals(respVO.getReporterId(), loginUserId);
        }
        if (STATUS_PENDING_ARCHIVE.equals(respVO.getStatus())) {
            return Objects.equals(respVO.getDispatcherId(), loginUserId);
        }
        return false;
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
        if (STATUS_CLOSED.equals(status)) {
            return "archived";
        }
        return "report";
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
        if (STATUS_CLOSED.equals(status)) {
            return "已关闭归档";
        }
        return "发起异常";
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

    private List<ResourceDeviceExceptionRespVO.FlowLog> buildFlowLogs(ResourceDeviceExceptionDO exception) {
        List<ResourceDeviceExceptionRespVO.FlowLog> logs = new ArrayList<>();
        addFlowLog(logs, "CREATE", "发起异常", null, STATUS_REPORTED, null,
                "响应分派", exception.getFaultDesc(), exception.getReporterId(), exception.getReporter(),
                exception.getReportTime());
        addFlowLog(logs, "RESPOND", "响应分派", STATUS_REPORTED,
                "NEED_REPAIR".equals(exception.getResponseResult()) ? STATUS_DISPATCHED : STATUS_PENDING_CONFIRM,
                "响应分派", "NEED_REPAIR".equals(exception.getResponseResult()) ? "维修执行" : "完成确认",
                exception.getResponseRemark(), exception.getDispatcherId(), exception.getDispatcher(),
                exception.getDispatchTime());
        addFlowLog(logs, "REPAIR", "维修执行", STATUS_DISPATCHED, STATUS_PENDING_CONFIRM,
                "维修执行", "完成确认", exception.getRepairAction(), exception.getAssigneeId(),
                exception.getAssignee(), exception.getRepairTime());
        addFlowLog(logs, "CONFIRM", "完成确认", STATUS_PENDING_CONFIRM, STATUS_PENDING_ARCHIVE,
                "完成确认", "关闭归档", exception.getConfirmRemark(), exception.getConfirmerId(),
                exception.getConfirmer(), exception.getConfirmTime());
        addFlowLog(logs, "ARCHIVE", "关闭归档", STATUS_PENDING_ARCHIVE, STATUS_CLOSED,
                "关闭归档", "已关闭归档", exception.getArchiveReason(), exception.getArchiverId(),
                exception.getArchiver(), exception.getArchiveTime());
        return logs;
    }

    private void addFlowLog(List<ResourceDeviceExceptionRespVO.FlowLog> logs, String actionCode, String actionName,
                            String fromStatus, String toStatus, String fromNodeName, String toNodeName,
                            String opinion, Long handlerUserId, String handlerUserName, LocalDateTime handleTime) {
        if (handleTime == null) {
            return;
        }
        ResourceDeviceExceptionRespVO.FlowLog log = new ResourceDeviceExceptionRespVO.FlowLog();
        log.setActionCode(actionCode);
        log.setActionName(actionName);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setFromNodeCode(resolveNodeCode(fromStatus));
        log.setFromNodeName(fromNodeName);
        log.setToNodeCode(resolveNodeCode(toStatus));
        log.setToNodeName(toNodeName);
        log.setOpinion(opinion);
        log.setHandlerUserId(handlerUserId);
        log.setHandlerUserName(handlerUserName);
        log.setHandleTime(handleTime);
        logs.add(log);
    }

}
