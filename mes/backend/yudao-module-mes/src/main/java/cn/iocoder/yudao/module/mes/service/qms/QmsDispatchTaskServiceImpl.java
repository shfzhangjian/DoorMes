package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCandidateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskDispatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskProcessSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRoundRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSampleResultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskResultItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskResultSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskWizardCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskRoundDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDispatchTaskSampleResultDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDispatchTaskLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDispatchTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDispatchTaskItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDispatchTaskRoundMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDispatchTaskSampleResultMapper;
import cn.iocoder.yudao.module.mes.service.qms.task.QmsTaskExecutionAdapter;
import cn.iocoder.yudao.module.mes.service.qms.task.QmsTaskExecutionSnapshot;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.flowable.engine.TaskService;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_EXECUTION_NOT_SUBMITTED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_EXECUTOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_BPM_NOT_PUBLISHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_TASK_CANCELLED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_TYPE_NOT_SUPPORTED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_RESULT_NOT_EDITABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_REQUEST_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_NATIVE_ENTRY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_DISPATCH_PROCESS_NOT_ACTIONABLE;

@Service
@Validated
public class QmsDispatchTaskServiceImpl implements QmsDispatchTaskService {

    private static final String BPM_PROCESS_KEY = "qms_quality_dispatch_task";
    private static final String BPM_MODEL_ID = "qms-quality-dispatch-task-model";
    private static final Integer BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE = 1009003002;
    private static final Long BPM_MANAGER_USER_ID = 144L;
    private static final String TRIGGER_PRODUCT_ABNORMAL_EVENT = "PRODUCT_ABNORMAL_EVENT";
    private static final String EXECUTION_MODE_NATIVE = "NATIVE";
    private static final String EXECUTION_MODE_TASK_OWNED = "TASK_OWNED";

    @Resource
    private QmsDispatchTaskMapper taskMapper;
    @Resource
    private QmsDispatchTaskLogMapper logMapper;
    @Resource
    private QmsDispatchTaskItemMapper taskItemMapper;
    @Resource
    private QmsDispatchTaskRoundMapper taskRoundMapper;
    @Resource
    private QmsDispatchTaskSampleResultMapper sampleResultMapper;
    @Resource
    private QmsDispatchTaskInspectionService inspectionService;
    @Resource
    private QmsProductAbnormalEventService productAbnormalEventService;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private BpmModelService bpmModelService;
    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private TaskService bpmTaskService;
    @Resource
    private BpmTaskService workflowTaskService;
    @Resource
    private List<QmsTaskExecutionAdapter> executionAdapters;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromExecution(QmsDispatchTaskCreateReqVO reqVO) {
        String checkType = normalizeCheckType(reqVO.getCheckType());
        QmsDispatchTaskDO existing = taskMapper.selectByExecution(checkType, reqVO.getExecutionId());
        if (existing != null) {
            return existing.getId();
        }
        QmsTaskExecutionSnapshot snapshot = loadRequiredSnapshot(checkType, reqVO.getExecutionId());
        QmsDispatchTaskDO task = buildTask(snapshot);
        task.setTaskType("LEGACY");
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException ex) {
            existing = taskMapper.selectByExecution(checkType, reqVO.getExecutionId());
            if (existing != null) {
                return existing.getId();
            }
            throw ex;
        }
        QmsDispatchTaskDO numberUpdate = new QmsDispatchTaskDO();
        numberUpdate.setId(task.getId());
        numberUpdate.setTaskNo("QT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", task.getId()));
        taskMapper.updateById(numberUpdate);
        insertRound(task, null, null, snapshot.getExecutionId(), snapshot.getExecutionNo(), "EXECUTING", snapshot);
        writeLog(task.getId(), "CREATED", "关联原检验单并建立质量任务", null, "DRAFT",
                Map.of("checkType", checkType, "executionId", reqVO.getExecutionId()));
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndDispatch(QmsDispatchTaskWizardCreateReqVO reqVO) {
        validateWizardRequest(reqVO);
        if ("ADDITIONAL".equalsIgnoreCase(reqVO.getTaskType())) {
            return createTaskOwnedAndDispatch(reqVO);
        }
        boolean productEventRecheck = isProductEventRecheck(reqVO.getTriggerSource());
        QmsProductAbnormalEventTaskRecheckResult productResult = null;
        QmsDispatchTaskInspectionService.GeneratedInspection generated;
        if (productEventRecheck) {
            productResult = productAbnormalEventService.createTaskRecheck(reqVO.getInspectionScene(),
                    reqVO.getSourceExecutionId(), reqVO.getSelectedItemIds(), reqVO.getRejectReason(), null);
            generated = inspectionService.describeGeneratedInspection(reqVO.getCheckType(),
                    productResult.sourceInspectionId(), productResult.sourceInspectionNo(),
                    productResult.newInspectionId(), productResult.newInspectionNo(),
                    productResult.itemIdMap(), reqVO.getSelectedItemIds());
        } else {
            generated = inspectionService.generate(reqVO);
        }
        boolean recheck = "RECHECK".equals(reqVO.getTaskType().trim().toUpperCase(Locale.ROOT));
        QmsTaskExecutionSnapshot sourceSnapshot = recheck
                ? loadRequiredSnapshot(generated.checkType(), generated.sourceExecutionId()) : null;
        QmsTaskExecutionSnapshot snapshot = loadRequiredSnapshot(generated.checkType(), generated.executionId());
        QmsDispatchTaskDO task = buildTask(snapshot);
        task.setTaskType(reqVO.getTaskType().trim().toUpperCase(Locale.ROOT));
        task.setTriggerSource(StrUtil.blankToDefault(reqVO.getTriggerSource(), "QUALITY_TASK_CENTER"));
        task.setInspectionScene(reqVO.getInspectionScene());
        task.setSourceExecutionId(generated.sourceExecutionId());
        task.setSourceExecutionNo(generated.sourceExecutionNo());
        task.setExecutionId(generated.executionId());
        task.setExecutionNo(generated.executionNo());
        task.setExecutionRoute(generated.executionRoute());
        task.setReceiptNo(reqVO.getReceiptNo());
        task.setSupplierName(reqVO.getSupplierName());
        task.setArrivalDate(reqVO.getArrivalDate());
        task.setAssigneeUserId(reqVO.getAssigneeUserId());
        task.setAssigneeUserName(reqVO.getAssigneeUserName());
        task.setAssigneeDeptId(reqVO.getAssigneeDeptId());
        task.setAssigneeDeptName(reqVO.getAssigneeDeptName());
        task.setPriority(StrUtil.blankToDefault(reqVO.getPriority(), "NORMAL").toUpperCase(Locale.ROOT));
        task.setRequiredFinishTime(reqVO.getRequiredFinishTime());
        task.setTaskInstruction(reqVO.getTaskInstruction());
        task.setDispatchStatus("DISPATCHED");
        task.setDispatchTime(LocalDateTime.now());
        task.setProcessStatus("RUNNING");
        if (recheck) {
            int currentRoundNo = productResult == null ? 2 : productResult.taskRoundNo();
            task.setCurrentRoundNo(currentRoundNo);
            task.setRecheckCount(Math.max(1, currentRoundNo - 1));
            task.setRootExecutionId(productResult == null
                    ? generated.sourceExecutionId() : productResult.rootInspectionId());
            task.setRootExecutionNo(productResult == null
                    ? generated.sourceExecutionNo() : productResult.rootInspectionNo());
            if (productResult != null) {
                task.setRecheckGroupId(productResult.groupId());
            }
        }
        taskMapper.insert(task);
        String taskNo = "QT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", task.getId());
        QmsDispatchTaskDO numberUpdate = new QmsDispatchTaskDO();
        numberUpdate.setId(task.getId());
        numberUpdate.setTaskNo(taskNo);
        taskMapper.updateById(numberUpdate);
        insertTaskItems(task.getId(), task.getCurrentRoundNo(), generated.items());
        QmsDispatchTaskRoundDO rootRound = null;
        QmsDispatchTaskRoundDO sourceRound = null;
        if (recheck) {
            int sourceRoundNo = Math.max(1, task.getCurrentRoundNo() - 1);
            if (productResult != null && sourceRoundNo > 1
                    && !Objects.equals(productResult.rootInspectionId(), generated.sourceExecutionId())) {
                QmsTaskExecutionSnapshot rootSnapshot = loadRequiredSnapshot(generated.checkType(),
                        productResult.rootInspectionId());
                rootRound = insertRound(task, null, null, productResult.rootInspectionId(),
                        productResult.rootInspectionNo(), "RETURNED", rootSnapshot, 1);
            }
            sourceRound = insertRound(task, null, null, generated.sourceExecutionId(),
                    generated.sourceExecutionNo(), "RETURNED", sourceSnapshot, sourceRoundNo);
            if (rootRound == null) {
                rootRound = sourceRound;
            }
            sourceRound.setReturnReason(StrUtil.blankToDefault(reqVO.getRejectReason(), "创建复检任务"));
            sourceRound.setReturnedById(SecurityFrameworkUtils.getLoginUserId());
            sourceRound.setReturnedByName(StrUtil.blankToDefault(
                    SecurityFrameworkUtils.getLoginUserNickname(), "发起人"));
            sourceRound.setReturnedTime(LocalDateTime.now());
            if (productEventRecheck) {
                sourceRound.setInspectionStatus("REJECTED");
            }
            taskRoundMapper.updateById(sourceRound);
        }
        QmsDispatchTaskRoundDO currentRound = insertRound(task, generated.sourceExecutionId(),
                generated.sourceExecutionNo(), generated.executionId(), generated.executionNo(),
                "EXECUTING", snapshot);
        if (productResult != null) {
            bindRoundRecheckDetail(rootRound, productResult.rootDetailId());
            bindRoundRecheckDetail(sourceRound, productResult.sourceDetailId());
            bindRoundRecheckDetail(currentRound, productResult.newDetailId());
            productAbnormalEventService.bindDispatchTask(productResult.groupId(), task.getId(),
                    productResult.rootDetailId(), rootRound == null ? null : rootRound.getId(),
                    productResult.sourceDetailId(), sourceRound == null ? null : sourceRound.getId(),
                    productResult.newDetailId(), currentRound.getId());
        }

        String processInstanceId = startProcess(task.getId(), task.getExecutionId(), taskNo, reqVO);
        QmsDispatchTaskDO processUpdate = new QmsDispatchTaskDO();
        processUpdate.setId(task.getId());
        processUpdate.setProcessInstanceId(processInstanceId);
        processUpdate.setProcessStatus("RUNNING");
        taskMapper.updateById(processUpdate);
        writeLog(task.getId(), "CREATED", "向导生成" + generated.checkType() + "检验单"
                        + generated.executionNo(), null, "DISPATCHED",
                Map.of("taskType", task.getTaskType(), "executionId", generated.executionId(),
                        "selectedItemCount", generated.items().size()));
        writeLog(task.getId(), "PROCESS_STARTED", "任务已推送给" + reqVO.getAssigneeUserName(),
                "DISPATCHED", "RUNNING", Map.of("processInstanceId", processInstanceId));
        return task.getId();
    }

    private Long createTaskOwnedAndDispatch(QmsDispatchTaskWizardCreateReqVO reqVO) {
        QmsDispatchTaskInspectionService.TaskOwnedDefinition definition =
                inspectionService.buildTaskOwnedDefinition(reqVO);
        var standard = definition.standard();
        QmsDispatchTaskDO task = new QmsDispatchTaskDO();
        task.setTaskType("ADDITIONAL");
        task.setSourceMode("MANUAL_BATCH");
        task.setExecutionMode(EXECUTION_MODE_TASK_OWNED);
        task.setObjectMode(StrUtil.blankToDefault(reqVO.getObjectMode(), "BATCH").toUpperCase(Locale.ROOT));
        task.setSampleSelectionMode(StrUtil.blankToDefault(
                reqVO.getSampleSelectionMode(), "QUANTITY_ONLY").toUpperCase(Locale.ROOT));
        task.setRequiredSampleQty(reqVO.getRequiredSampleQty());
        task.setTriggerSource(StrUtil.blankToDefault(reqVO.getTriggerSource(), "QUALITY_TASK_CENTER"));
        task.setCheckType(normalizeCheckType(reqVO.getCheckType()));
        task.setObjectType(task.getObjectMode());
        task.setSourceType("QUALITY_TASK");
        task.setSourceNo("OQC".equals(task.getCheckType())
                ? reqVO.getWorkOrderNo() : reqVO.getBatchNo());
        task.setExecutionRoute("/mes/quality/task-center/detail");
        task.setCurrentRoundNo(1);
        task.setRecheckCount(0);
        task.setMaterialId(reqVO.getMaterialId());
        task.setMaterialCode(reqVO.getMaterialCode());
        task.setMaterialName(reqVO.getMaterialName());
        task.setMaterialSpec(reqVO.getSpecification());
        task.setProductModel(StrUtil.blankToDefault(reqVO.getProductModelCode(), reqVO.getProductModelName()));
        task.setOperationCode(reqVO.getOperationCode());
        task.setOperationName(reqVO.getOperationName());
        task.setLotNo(reqVO.getBatchNo());
        task.setReceiptNo(reqVO.getReceiptNo());
        task.setSupplierName(reqVO.getSupplierName());
        task.setArrivalDate(reqVO.getArrivalDate());
        task.setCheckQty(reqVO.getCheckQty());
        task.setUnit(reqVO.getUnit());
        task.setStandardId(standard.getId());
        task.setStandardNo(standard.getStandardNo());
        task.setStandardName(standard.getStandardName());
        task.setStandardVersion(standard.getVersion());
        task.setAssigneeUserId(reqVO.getAssigneeUserId());
        task.setAssigneeUserName(reqVO.getAssigneeUserName());
        task.setAssigneeDeptId(reqVO.getAssigneeDeptId());
        task.setAssigneeDeptName(reqVO.getAssigneeDeptName());
        task.setPriority(StrUtil.blankToDefault(reqVO.getPriority(), "NORMAL").toUpperCase(Locale.ROOT));
        task.setRequiredFinishTime(reqVO.getRequiredFinishTime());
        task.setTaskInstruction(reqVO.getTaskInstruction());
        task.setDispatchStatus("DISPATCHED");
        task.setDispatchTime(LocalDateTime.now());
        task.setProcessStatus("RUNNING");
        task.setSourceStatus("PENDING");
        task.setSourceJudgment("PENDING");
        task.setItemCount(definition.items().size());
        task.setAbnormalCount(0);
        task.setLastSyncTime(LocalDateTime.now());
        taskMapper.insert(task);

        String taskNo = "QT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", task.getId());
        String executionNo = taskNo + "-A01";
        QmsDispatchTaskDO numberUpdate = new QmsDispatchTaskDO();
        numberUpdate.setId(task.getId());
        numberUpdate.setTaskNo(taskNo);
        numberUpdate.setExecutionNo(executionNo);
        taskMapper.updateById(numberUpdate);
        task.setTaskNo(taskNo);
        task.setExecutionNo(executionNo);

        List<QmsDispatchTaskItemDO> taskItems = insertTaskItems(task.getId(), 1, definition.items());
        insertTaskOwnedSamples(task, taskItems, reqVO.getSamplePieceNos());
        insertTaskOwnedRound(task, "EXECUTING");

        String processInstanceId = startProcess(task.getId(), null, taskNo, reqVO);
        QmsDispatchTaskDO processUpdate = new QmsDispatchTaskDO();
        processUpdate.setId(task.getId());
        processUpdate.setProcessInstanceId(processInstanceId);
        taskMapper.updateById(processUpdate);
        writeLog(task.getId(), "CREATED", "向导建立任务中心加检记录", null, "DISPATCHED",
                Map.of("taskType", task.getTaskType(), "sampleCount", task.getRequiredSampleQty(),
                        "selectedItemCount", taskItems.size()));
        writeLog(task.getId(), "PROCESS_STARTED", "任务已推送给" + reqVO.getAssigneeUserName(),
                "DISPATCHED", "RUNNING", Map.of("processInstanceId", processInstanceId));
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(QmsDispatchTaskDispatchReqVO reqVO) {
        QmsDispatchTaskDO current = validateTaskExists(reqVO.getId());
        if ("CANCELLED".equals(current.getDispatchStatus())) {
            throw exception(QMS_DISPATCH_TASK_CANCELLED);
        }
        QmsDispatchTaskDO update = new QmsDispatchTaskDO();
        update.setId(current.getId());
        update.setAssigneeUserId(reqVO.getAssigneeUserId());
        update.setAssigneeUserName(reqVO.getAssigneeUserName());
        update.setAssigneeDeptId(reqVO.getAssigneeDeptId());
        update.setAssigneeDeptName(reqVO.getAssigneeDeptName());
        update.setPriority(StrUtil.blankToDefault(reqVO.getPriority(), "NORMAL").toUpperCase(Locale.ROOT));
        update.setRequiredFinishTime(reqVO.getRequiredFinishTime());
        update.setTaskInstruction(reqVO.getTaskInstruction());
        update.setDispatchStatus("DISPATCHED");
        update.setDispatchTime(LocalDateTime.now());
        if (!isTaskOwned(current)) {
            QmsTaskExecutionSnapshot snapshot = loadRequiredSnapshot(current.getCheckType(), current.getExecutionId());
            applyLiveResult(update, snapshot);
        }
        taskMapper.updateById(update);
        writeLog(current.getId(), "DISPATCHED", "质量任务已分派给" + reqVO.getAssigneeUserName(),
                current.getDispatchStatus(), "DISPATCHED", reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(QmsDispatchTaskCancelReqVO reqVO) {
        QmsDispatchTaskDO current = validateTaskExists(reqVO.getId());
        if ("CANCELLED".equals(current.getDispatchStatus())) {
            return;
        }
        QmsDispatchTaskDO update = new QmsDispatchTaskDO();
        update.setId(current.getId());
        update.setDispatchStatus("CANCELLED");
        update.setCancelReason(reqVO.getReason());
        taskMapper.updateById(update);
        writeLog(current.getId(), "CANCELLED", "取消任务：" + reqVO.getReason(),
                current.getDispatchStatus(), "CANCELLED", reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelForDeletedIqc(Long iqcId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        for (QmsDispatchTaskDO task : taskMapper.selectListByIqcId(iqcId)) {
            if ("CANCELLED".equals(task.getDispatchStatus())) {
                continue;
            }
            if ("RUNNING".equals(task.getProcessStatus())
                    && StrUtil.isNotBlank(task.getProcessInstanceId())
                    && loginUserId != null) {
                BpmProcessInstanceCancelReqVO cancelReqVO = new BpmProcessInstanceCancelReqVO();
                cancelReqVO.setId(task.getProcessInstanceId());
                cancelReqVO.setReason("关联 IQC 送检单已删除");
                bpmProcessInstanceService.cancelProcessInstanceByAdmin(loginUserId, cancelReqVO);
            }
            QmsDispatchTaskDO update = new QmsDispatchTaskDO();
            update.setId(task.getId());
            update.setDispatchStatus("CANCELLED");
            update.setCancelReason("关联 IQC 送检单已删除");
            taskMapper.updateById(update);
            writeLog(task.getId(), "CANCELLED", "关联 IQC 送检单已删除，系统取消任务",
                    task.getDispatchStatus(), "CANCELLED", Map.of("iqcId", iqcId));
        }
    }

    @Override
    public QmsDispatchTaskRespVO get(Long id) {
        return toResp(validateTaskExists(id));
    }

    @Override
    public QmsDispatchTaskDetailRespVO getDetail(Long id) {
        QmsDispatchTaskDO task = validateTaskExists(id);
        QmsDispatchTaskDetailRespVO detail = new QmsDispatchTaskDetailRespVO();
        detail.setTask(toResp(task));
        detail.setItems(taskItemMapper.selectListByTaskIdAndRoundNo(id, currentRoundNo(task)).stream().map(item -> {
            QmsDispatchTaskCandidateItemRespVO result = new QmsDispatchTaskCandidateItemRespVO();
            result.setId(item.getId());
            result.setSourceItemId(item.getSourceItemId());
            result.setExecutionItemId(item.getExecutionItemId());
            result.setStandardItemId(item.getStandardItemId());
            result.setPieceNo(item.getPieceNo());
            result.setInspectionItem(item.getInspectionItem());
            result.setStandardDesc(item.getStandardDesc());
            result.setUnit(item.getItemUnit());
            result.setItemType(item.getItemType());
            result.setInspectionMethod(item.getInspectionMethod());
            result.setTestTool(item.getTestTool());
            result.setSampleSize(item.getSampleSize());
            result.setMeasuredValue(item.getMeasuredValue());
            result.setQualitativeValue(item.getQualitativeValue());
            result.setResult(item.getResult());
            result.setCurrentResult(item.getResult());
            result.setSort(item.getSort());
            return result;
        }).toList());
        if (isTaskOwned(task)) {
            Map<Long, QmsDispatchTaskItemDO> itemMap = taskItemMapper
                    .selectListByTaskIdAndRoundNo(id, currentRoundNo(task)).stream()
                    .collect(Collectors.toMap(QmsDispatchTaskItemDO::getId, Function.identity()));
            detail.setSamples(sampleResultMapper.selectListByTaskIdAndRoundNo(id, currentRoundNo(task)).stream()
                    .map(sample -> toSampleResp(sample, itemMap.get(sample.getTaskItemId())))
                    .toList());
        } else {
            detail.setSamples(List.of());
        }
        List<QmsDispatchTaskRoundDO> rounds = taskRoundMapper.selectListByTaskId(id);
        if (rounds.isEmpty()) {
            detail.setRounds(List.of(toRoundResp(buildLegacyRoundSnapshot(task))));
        } else {
            detail.setRounds(BeanUtils.toBean(rounds, QmsDispatchTaskRoundRespVO.class));
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        org.flowable.task.api.Task activeTask = getActiveTask(task.getProcessInstanceId());
        detail.setActiveTaskKey(activeTask == null ? null : activeTask.getTaskDefinitionKey());
        detail.setActiveTaskName(activeTask == null ? null : activeTask.getName());
        detail.setActionable(activeTask != null && loginUserId != null
                && Objects.equals(activeTask.getAssignee(), String.valueOf(loginUserId)));
        detail.setEditable(isExecutionResultEditable(task, loginUserId));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveResult(QmsDispatchTaskResultSaveReqVO reqVO) {
        QmsDispatchTaskDO task = validateTaskExists(reqVO.getId());
        if (isTaskOwned(task)) {
            saveTaskOwnedResult(task, reqVO);
            return;
        }
        if (Set.of("FAI", "GLUE_BOARD_FAI").contains(normalizeCheckType(task.getCheckType()))) {
            throw exception(QMS_DISPATCH_NATIVE_ENTRY_REQUIRED, task.getCheckType());
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(task.getAssigneeUserId(), loginUserId)) {
            throw exception(QMS_DISPATCH_EXECUTOR_ONLY);
        }
        if (!isExecutionResultEditable(task, loginUserId)) {
            throw exception(QMS_DISPATCH_RESULT_NOT_EDITABLE);
        }
        List<QmsDispatchTaskItemDO> taskItems = taskItemMapper.selectListByTaskIdAndRoundNo(
                task.getId(), currentRoundNo(task));
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), task.getAssigneeUserName());
        inspectionService.saveResults(task.getCheckType(), task.getExecutionId(), loginUserId, operatorName,
                taskItems, reqVO.getItems());
        Map<Long, QmsDispatchTaskResultItemReqVO> requestMap = reqVO.getItems().stream()
                .collect(Collectors.toMap(QmsDispatchTaskResultItemReqVO::getTaskItemId, Function.identity()));
        for (QmsDispatchTaskItemDO taskItem : taskItems) {
            QmsDispatchTaskResultItemReqVO requestItem = requestMap.get(taskItem.getId());
            QmsDispatchTaskItemDO update = new QmsDispatchTaskItemDO();
            update.setId(taskItem.getId());
            update.setMeasuredValue(requestItem.getMeasuredValue());
            update.setQualitativeValue(requestItem.getQualitativeValue());
            update.setResult(requestItem.getResult().trim().toUpperCase(Locale.ROOT));
            taskItemMapper.updateById(update);
        }
        QmsTaskExecutionSnapshot snapshot = loadRequiredSnapshot(task.getCheckType(), task.getExecutionId());
        QmsDispatchTaskDO taskUpdate = new QmsDispatchTaskDO();
        taskUpdate.setId(task.getId());
        applyLiveResult(taskUpdate, snapshot);
        taskMapper.updateById(taskUpdate);
        writeLog(task.getId(), "RESULT_SAVED", "执行人保存检验结果", task.getProcessStatus(),
                task.getProcessStatus(), Map.of("itemCount", reqVO.getItems().size()));
    }

    private void saveTaskOwnedResult(QmsDispatchTaskDO task, QmsDispatchTaskResultSaveReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(task.getAssigneeUserId(), loginUserId)) {
            throw exception(QMS_DISPATCH_EXECUTOR_ONLY);
        }
        if (!isExecutionResultEditable(task, loginUserId)) {
            throw exception(QMS_DISPATCH_RESULT_NOT_EDITABLE);
        }
        int roundNo = currentRoundNo(task);
        List<QmsDispatchTaskItemDO> taskItems = taskItemMapper.selectListByTaskIdAndRoundNo(task.getId(), roundNo);
        List<QmsDispatchTaskSampleResultDO> samples =
                sampleResultMapper.selectListByTaskIdAndRoundNo(task.getId(), roundNo);
        Map<Long, QmsDispatchTaskSampleResultDO> sampleMap = samples.stream()
                .collect(Collectors.toMap(QmsDispatchTaskSampleResultDO::getId, Function.identity()));
        Set<Long> requestIds = reqVO.getItems().stream()
                .map(QmsDispatchTaskResultItemReqVO::getSampleResultId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (samples.isEmpty() || reqVO.getItems().size() != samples.size()
                || requestIds.size() != samples.size() || !sampleMap.keySet().equals(requestIds)) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "必须完整填写当前轮次全部样本检验项");
        }

        String operatorName = StrUtil.blankToDefault(
                SecurityFrameworkUtils.getLoginUserNickname(), task.getAssigneeUserName());
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, String> pieceNoBySampleSeq = new HashMap<>();
        for (QmsDispatchTaskResultItemReqVO request : reqVO.getItems()) {
            QmsDispatchTaskSampleResultDO sample = sampleMap.get(request.getSampleResultId());
            if (sample == null || !Objects.equals(sample.getTaskItemId(), request.getTaskItemId())) {
                throw exception(QMS_DISPATCH_ITEM_INVALID);
            }
            String result = normalizeTaskResult(request.getResult());
            String pieceNo = StrUtil.trim(request.getPieceNo());
            if (StrUtil.isBlank(pieceNo)) {
                pieceNo = null;
            }
            if ("SPECIFIED_PIECE".equalsIgnoreCase(task.getSampleSelectionMode())
                    && !Objects.equals(StrUtil.nullToEmpty(sample.getPieceNo()), StrUtil.nullToEmpty(pieceNo))) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID, "指定片号不可在执行时修改");
            }
            if ("EXECUTION_PIECE".equalsIgnoreCase(task.getObjectMode()) && StrUtil.isBlank(pieceNo)) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID,
                        "第" + sample.getSampleSeq() + "个样本必须填写实际片号");
            }
            String knownPieceNo = pieceNoBySampleSeq.putIfAbsent(sample.getSampleSeq(), pieceNo);
            if (knownPieceNo != null && !Objects.equals(knownPieceNo, pieceNo)) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID,
                        "同一样本序号的实际片号必须一致");
            }
            QmsDispatchTaskSampleResultDO update = new QmsDispatchTaskSampleResultDO();
            update.setId(sample.getId());
            update.setPieceNo(pieceNo);
            update.setMeasuredValue(request.getMeasuredValue());
            update.setQualitativeValue(request.getQualitativeValue());
            update.setResult(result);
            update.setInspectorId(loginUserId);
            update.setInspectorName(operatorName);
            update.setInspectionTime(now);
            sampleResultMapper.updateById(update);
            sample.setPieceNo(pieceNo);
            sample.setMeasuredValue(request.getMeasuredValue());
            sample.setQualitativeValue(request.getQualitativeValue());
            sample.setResult(result);
        }
        List<String> actualPieceNos = pieceNoBySampleSeq.values().stream()
                .filter(StrUtil::isNotBlank)
                .toList();
        if (new HashSet<>(actualPieceNos).size() != actualPieceNos.size()) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "不同样本不能使用相同片号");
        }

        Map<Long, List<QmsDispatchTaskSampleResultDO>> samplesByItem = samples.stream()
                .collect(Collectors.groupingBy(QmsDispatchTaskSampleResultDO::getTaskItemId));
        for (QmsDispatchTaskItemDO taskItem : taskItems) {
            List<QmsDispatchTaskSampleResultDO> itemSamples =
                    samplesByItem.getOrDefault(taskItem.getId(), List.of());
            String itemResult = itemSamples.stream().anyMatch(item -> "NG".equals(item.getResult())) ? "NG" : "OK";
            QmsDispatchTaskItemDO itemUpdate = new QmsDispatchTaskItemDO();
            itemUpdate.setId(taskItem.getId());
            itemUpdate.setResult(itemResult);
            taskItemMapper.updateById(itemUpdate);
        }
        String judgment = samples.stream().anyMatch(item -> "NG".equals(item.getResult())) ? "NG" : "OK";
        QmsDispatchTaskDO taskUpdate = new QmsDispatchTaskDO();
        taskUpdate.setId(task.getId());
        taskUpdate.setSourceStatus("WAITING_CONFIRM");
        taskUpdate.setSourceJudgment(judgment);
        taskUpdate.setSourceInspectorName(operatorName);
        taskUpdate.setSourceInspectionTime(now);
        taskUpdate.setAbnormalCount((int) samples.stream().filter(item -> "NG".equals(item.getResult())).count());
        taskUpdate.setLastSyncTime(now);
        taskMapper.updateById(taskUpdate);
        task.setSourceStatus("WAITING_CONFIRM");
        task.setSourceJudgment(judgment);
        task.setSourceInspectorName(operatorName);
        task.setSourceInspectionTime(now);
        writeLog(task.getId(), "RESULT_SAVED", "执行人保存加检样本结果", task.getProcessStatus(),
                task.getProcessStatus(), Map.of("sampleItemCount", samples.size(),
                        "sampleCount", Objects.requireNonNullElse(task.getRequiredSampleQty(), 0)));
    }

    private String normalizeTaskResult(String result) {
        String normalized = StrUtil.nullToEmpty(result).trim().toUpperCase(Locale.ROOT);
        if (!Set.of("OK", "NG").contains(normalized)) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "项目判定仅支持合格或不合格");
        }
        return normalized;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitProcessNode(QmsDispatchTaskProcessSubmitReqVO reqVO) {
        QmsDispatchTaskDO task = validateTaskExists(reqVO.getId());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        org.flowable.task.api.Task activeTask = getActiveTask(task.getProcessInstanceId());
        if (activeTask == null || loginUserId == null
                || !Objects.equals(activeTask.getAssignee(), String.valueOf(loginUserId))) {
            throw exception(QMS_DISPATCH_PROCESS_NOT_ACTIONABLE);
        }
        boolean taskOwned = isTaskOwned(task);
        QmsTaskExecutionSnapshot snapshot = taskOwned
                ? null : loadRequiredSnapshot(task.getCheckType(), task.getExecutionId());
        String activeTaskKey = activeTask.getTaskDefinitionKey();
        if ("task_execute".equals(activeTaskKey)) {
            if (taskOwned) {
                List<QmsDispatchTaskSampleResultDO> samples =
                        sampleResultMapper.selectListByTaskIdAndRoundNo(task.getId(), currentRoundNo(task));
                if (samples.isEmpty() || samples.stream()
                        .anyMatch(item -> !Set.of("OK", "NG").contains(StrUtil.nullToEmpty(item.getResult())))) {
                    throw exception(QMS_DISPATCH_EXECUTION_NOT_SUBMITTED);
                }
                if (!isExecutionSubmitted(task.getSourceStatus())) {
                    throw exception(QMS_DISPATCH_EXECUTION_NOT_SUBMITTED);
                }
            } else if (!isExecutionSubmitted(snapshot.getStatus())) {
                throw exception(QMS_DISPATCH_EXECUTION_NOT_SUBMITTED);
            }
        }

        QmsDispatchTaskRoundDO currentRound = ensureCurrentRound(task, snapshot);
        if ("task_execute".equals(activeTaskKey)) {
            if (taskOwned) {
                updateTaskOwnedRoundSnapshot(currentRound, "SUBMITTED", task);
            } else {
                updateRoundSnapshot(currentRound, "SUBMITTED", snapshot);
            }
        } else if ("task_confirm".equals(activeTaskKey) || "task_close".equals(activeTaskKey)) {
            currentRound.setRoundStatus("CONFIRMED");
            currentRound.setConfirmedById(loginUserId);
            currentRound.setConfirmedByName(StrUtil.blankToDefault(
                    SecurityFrameworkUtils.getLoginUserNickname(), "确认人"));
            currentRound.setConfirmedTime(LocalDateTime.now());
            if (taskOwned) {
                applyTaskOwnedRoundSnapshot(currentRound, task);
            } else {
                applyRoundSnapshot(currentRound, snapshot);
            }
            taskRoundMapper.updateById(currentRound);
        }
        if (!taskOwned) {
            QmsDispatchTaskDO update = new QmsDispatchTaskDO();
            update.setId(task.getId());
            applyLiveResult(update, snapshot);
            taskMapper.updateById(update);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("qualityTaskId", task.getId());
        variables.put("qualityTaskNo", task.getTaskNo());
        variables.put("checkType", task.getCheckType());
        if (task.getExecutionId() != null) {
            variables.put("executionId", task.getExecutionId());
        }
        if (StrUtil.isNotBlank(task.getExecutionNo())) {
            variables.put("executionNo", task.getExecutionNo());
        }
        BpmTaskApproveReqVO approveReqVO = new BpmTaskApproveReqVO()
                .setId(activeTask.getId())
                .setReason(StrUtil.blankToDefault(reqVO.getReason(), defaultProcessReason(activeTaskKey)))
                .setVariables(variables);
        workflowTaskService.approveTask(loginUserId, approveReqVO);
        if ("task_confirm".equals(activeTaskKey)) {
            autoCloseLegacyProcess(task, loginUserId, variables);
        }
        writeLog(task.getId(), "PROCESS_NODE_SUBMITTED", defaultProcessReason(activeTaskKey),
                activeTaskKey, activeTaskKey, Map.of("flowTaskId", activeTask.getId(),
                        "flowTaskName", StrUtil.nullToEmpty(activeTask.getName())));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnForRecheck(QmsDispatchTaskRecheckReqVO reqVO) {
        QmsDispatchTaskDO task = validateTaskExists(reqVO.getId());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        org.flowable.task.api.Task activeTask = getActiveTask(task.getProcessInstanceId());
        if (activeTask == null || loginUserId == null
                || !"task_confirm".equals(activeTask.getTaskDefinitionKey())
                || !Objects.equals(activeTask.getAssignee(), String.valueOf(loginUserId))) {
            throw exception(QMS_DISPATCH_PROCESS_NOT_ACTIONABLE);
        }
        if (isTaskOwned(task)) {
            returnTaskOwnedForRecheck(task, activeTask, loginUserId, reqVO);
            return;
        }
        int oldRoundNo = currentRoundNo(task);
        List<QmsDispatchTaskItemDO> currentItems = taskItemMapper.selectListByTaskIdAndRoundNo(task.getId(), oldRoundNo);
        List<Long> selectedExecutionItemIds = currentItems.stream()
                .map(QmsDispatchTaskItemDO::getExecutionItemId)
                .filter(Objects::nonNull)
                .toList();
        if (selectedExecutionItemIds.isEmpty()) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "当前轮次没有可复制的检验项目");
        }

        QmsTaskExecutionSnapshot oldSnapshot = loadRequiredSnapshot(task.getCheckType(), task.getExecutionId());
        QmsDispatchTaskRoundDO oldRound = ensureCurrentRound(task, oldSnapshot);
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "确认人");
        boolean productEventRecheck = isProductEventRecheck(task.getTriggerSource());
        QmsProductAbnormalEventTaskRecheckResult productResult = null;
        QmsDispatchTaskInspectionService.GeneratedInspection generated;
        if (productEventRecheck) {
            productResult = productAbnormalEventService.createTaskRecheck(task.getInspectionScene(),
                    task.getExecutionId(), selectedExecutionItemIds, reqVO.getReason(), task.getId());
            generated = inspectionService.describeGeneratedInspection(task.getCheckType(),
                    productResult.sourceInspectionId(), productResult.sourceInspectionNo(),
                    productResult.newInspectionId(), productResult.newInspectionNo(),
                    productResult.itemIdMap(), selectedExecutionItemIds);
        } else {
            inspectionService.markInspectionRejected(task.getCheckType(), task.getExecutionId());
            generated = inspectionService.cloneForTaskRecheck(
                    task.getCheckType(), task.getExecutionId(), selectedExecutionItemIds);
        }
        oldRound.setRoundStatus("RETURNED");
        oldRound.setReturnReason(reqVO.getReason().trim());
        oldRound.setReturnedById(loginUserId);
        oldRound.setReturnedByName(operatorName);
        oldRound.setReturnedTime(LocalDateTime.now());
        applyRoundSnapshot(oldRound, oldSnapshot);
        oldRound.setInspectionStatus("REJECTED");
        taskRoundMapper.updateById(oldRound);

        QmsTaskExecutionSnapshot newSnapshot = loadRequiredSnapshot(task.getCheckType(), generated.executionId());
        int nextRoundNo = productResult == null ? oldRoundNo + 1 : productResult.taskRoundNo();
        if (nextRoundNo != oldRoundNo + 1) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "复检轮次不连续，请刷新后重试");
        }
        insertTaskItems(task.getId(), nextRoundNo, generated.items());
        QmsDispatchTaskDO nextRoundTask = new QmsDispatchTaskDO();
        nextRoundTask.setId(task.getId());
        nextRoundTask.setExecutionId(generated.executionId());
        nextRoundTask.setExecutionNo(generated.executionNo());
        nextRoundTask.setExecutionRoute(generated.executionRoute());
        nextRoundTask.setCurrentRoundNo(nextRoundNo);
        nextRoundTask.setRecheckCount(Objects.requireNonNullElse(task.getRecheckCount(), 0) + 1);
        nextRoundTask.setDispatchStatus("DISPATCHED");
        nextRoundTask.setProcessStatus("RUNNING");
        applyLiveResult(nextRoundTask, newSnapshot);
        taskMapper.updateById(nextRoundTask);
        QmsDispatchTaskRoundDO newRound = insertRound(task, task.getExecutionId(), task.getExecutionNo(),
                generated.executionId(), generated.executionNo(), "EXECUTING", newSnapshot, nextRoundNo);
        if (productResult != null) {
            QmsDispatchTaskRoundDO rootRound = taskRoundMapper.selectByTaskIdAndRoundNo(task.getId(), 1);
            bindRoundRecheckDetail(oldRound, productResult.sourceDetailId());
            bindRoundRecheckDetail(newRound, productResult.newDetailId());
            productAbnormalEventService.bindDispatchTask(productResult.groupId(), task.getId(),
                    productResult.rootDetailId(), rootRound == null ? null : rootRound.getId(),
                    productResult.sourceDetailId(), oldRound.getId(),
                    productResult.newDetailId(), newRound.getId());
        }

        Map<String, Object> variables = buildProcessVariables(task, generated.executionId(), generated.executionNo());
        bpmTaskService.setVariables(activeTask.getId(), variables);
        workflowTaskService.returnTask(loginUserId, new BpmTaskReturnReqVO()
                .setId(activeTask.getId())
                .setTargetTaskDefinitionKey("task_execute")
                .setReason(reqVO.getReason().trim()));
        writeLog(task.getId(), "RETURN_FOR_RECHECK",
                "结果确认退回重检，生成第" + nextRoundNo + "轮（第" + (nextRoundNo - 1)
                        + "次复检）" + generated.executionNo(),
                "task_confirm", "task_execute", Map.of(
                        "roundNo", nextRoundNo,
                        "sourceExecutionId", task.getExecutionId(),
                        "executionId", generated.executionId(),
                        "reason", reqVO.getReason().trim(),
                        "sourceStatus", "REJECTED"));
    }

    private void returnTaskOwnedForRecheck(QmsDispatchTaskDO task, org.flowable.task.api.Task activeTask,
                                           Long loginUserId, QmsDispatchTaskRecheckReqVO reqVO) {
        int oldRoundNo = currentRoundNo(task);
        List<QmsDispatchTaskItemDO> oldItems =
                taskItemMapper.selectListByTaskIdAndRoundNo(task.getId(), oldRoundNo);
        List<QmsDispatchTaskSampleResultDO> oldSamples =
                sampleResultMapper.selectListByTaskIdAndRoundNo(task.getId(), oldRoundNo);
        if (oldItems.isEmpty() || oldSamples.isEmpty()) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "当前轮次没有可复制的任务检验数据");
        }
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "确认人");
        LocalDateTime now = LocalDateTime.now();
        QmsDispatchTaskRoundDO oldRound = ensureCurrentRound(task, null);
        oldRound.setRoundStatus("RETURNED");
        oldRound.setInspectionStatus("REJECTED");
        oldRound.setReturnReason(reqVO.getReason().trim());
        oldRound.setReturnedById(loginUserId);
        oldRound.setReturnedByName(operatorName);
        oldRound.setReturnedTime(now);
        oldRound.setJudgment(task.getSourceJudgment());
        oldRound.setInspectorName(task.getSourceInspectorName());
        oldRound.setInspectionTime(task.getSourceInspectionTime());
        taskRoundMapper.updateById(oldRound);

        int nextRoundNo = oldRoundNo + 1;
        Map<Long, Long> itemIdMap = new HashMap<>();
        for (QmsDispatchTaskItemDO oldItem : oldItems) {
            QmsDispatchTaskItemDO item = new QmsDispatchTaskItemDO();
            item.setTaskId(task.getId());
            item.setRoundNo(nextRoundNo);
            item.setSourceItemId(oldItem.getSourceItemId());
            item.setStandardItemId(oldItem.getStandardItemId());
            item.setPieceNo(oldItem.getPieceNo());
            item.setInspectionItem(oldItem.getInspectionItem());
            item.setStandardDesc(oldItem.getStandardDesc());
            item.setItemUnit(oldItem.getItemUnit());
            item.setItemType(oldItem.getItemType());
            item.setInspectionMethod(oldItem.getInspectionMethod());
            item.setTestTool(oldItem.getTestTool());
            item.setSampleSize(oldItem.getSampleSize());
            item.setResult("PENDING");
            item.setSort(oldItem.getSort());
            taskItemMapper.insert(item);
            itemIdMap.put(oldItem.getId(), item.getId());
        }
        for (QmsDispatchTaskSampleResultDO oldSample : oldSamples) {
            Long newTaskItemId = itemIdMap.get(oldSample.getTaskItemId());
            if (newTaskItemId == null) {
                throw exception(QMS_DISPATCH_ITEM_INVALID);
            }
            QmsDispatchTaskSampleResultDO sample = new QmsDispatchTaskSampleResultDO();
            sample.setTaskId(task.getId());
            sample.setTaskItemId(newTaskItemId);
            sample.setRoundNo(nextRoundNo);
            sample.setSampleSeq(oldSample.getSampleSeq());
            sample.setPieceNo("SPECIFIED_PIECE".equalsIgnoreCase(task.getSampleSelectionMode())
                    ? oldSample.getPieceNo() : null);
            sample.setResult("PENDING");
            sampleResultMapper.insert(sample);
        }

        String executionNo = task.getTaskNo() + "-R" + String.format("%02d", nextRoundNo - 1);
        QmsDispatchTaskDO update = new QmsDispatchTaskDO();
        update.setId(task.getId());
        update.setExecutionNo(executionNo);
        update.setCurrentRoundNo(nextRoundNo);
        update.setRecheckCount(Objects.requireNonNullElse(task.getRecheckCount(), 0) + 1);
        update.setDispatchStatus("DISPATCHED");
        update.setProcessStatus("RUNNING");
        update.setSourceStatus("PENDING");
        update.setSourceJudgment("PENDING");
        update.setSourceInspectorName(null);
        update.setSourceInspectionTime(null);
        update.setAbnormalCount(0);
        update.setLastSyncTime(now);
        taskMapper.updateById(update);
        taskMapper.clearInspectionOperator(task.getId());

        QmsDispatchTaskRoundDO newRound = new QmsDispatchTaskRoundDO();
        newRound.setTaskId(task.getId());
        newRound.setRoundNo(nextRoundNo);
        newRound.setCheckType(task.getCheckType());
        newRound.setInspectionScene(task.getInspectionScene());
        newRound.setSourceExecutionNo(task.getExecutionNo());
        newRound.setExecutionNo(executionNo);
        newRound.setRoundStatus("EXECUTING");
        newRound.setInspectionStatus("PENDING");
        newRound.setJudgment("PENDING");
        taskRoundMapper.insert(newRound);

        Map<String, Object> variables = buildProcessVariables(task, null, executionNo);
        bpmTaskService.setVariables(activeTask.getId(), variables);
        workflowTaskService.returnTask(loginUserId, new BpmTaskReturnReqVO()
                .setId(activeTask.getId())
                .setTargetTaskDefinitionKey("task_execute")
                .setReason(reqVO.getReason().trim()));
        writeLog(task.getId(), "RETURN_FOR_RECHECK",
                "结果确认退回任务加检第" + nextRoundNo + "轮（第" + (nextRoundNo - 1) + "次复检）",
                "task_confirm", "task_execute", Map.of("roundNo", nextRoundNo,
                        "executionNo", executionNo, "reason", reqVO.getReason().trim()));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBpmProcessStatus(String businessKey, String processInstanceId, Integer status, String reason) {
        QmsDispatchTaskDO task = null;
        if (StrUtil.isNotBlank(businessKey)) {
            try {
                task = taskMapper.selectById(Long.valueOf(businessKey));
            } catch (NumberFormatException ignored) {
                // 兼容早期流程未使用数字业务主键。
            }
        }
        if (task == null && StrUtil.isNotBlank(processInstanceId)) {
            task = taskMapper.selectByProcessInstanceId(processInstanceId);
        }
        if (task == null) {
            return;
        }
        BpmProcessInstanceStatusEnum processStatus = BpmProcessInstanceStatusEnum.valueOf(status);
        String afterProcessStatus = processStatus == null ? String.valueOf(status) : processStatus.name();
        String afterDispatchStatus = task.getDispatchStatus();
        boolean approved = BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(status);
        boolean taskOwned = isTaskOwned(task);
        if (approved) {
            Long operatorId = Objects.requireNonNullElse(SecurityFrameworkUtils.getLoginUserId(), task.getAssigneeUserId());
            String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(),
                    StrUtil.blankToDefault(task.getAssigneeUserName(), "流程系统"));
            if (!taskOwned) {
                inspectionService.completeInspection(task.getCheckType(), task.getExecutionId(), operatorId, operatorName);
            }
            afterDispatchStatus = "COMPLETED";
        } else if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(status)
                || BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(status)) {
            afterDispatchStatus = "CANCELLED";
        }
        QmsDispatchTaskDO update = new QmsDispatchTaskDO();
        update.setId(task.getId());
        update.setProcessInstanceId(processInstanceId);
        update.setProcessStatus(afterProcessStatus);
        update.setDispatchStatus(afterDispatchStatus);
        if (StrUtil.isNotBlank(reason)) update.setCancelReason(reason);
        if (taskOwned) {
            if (approved) {
                update.setSourceStatus("COMPLETED");
                update.setLastSyncTime(LocalDateTime.now());
                task.setSourceStatus("COMPLETED");
                QmsDispatchTaskRoundDO currentRound = ensureCurrentRound(task, null);
                updateTaskOwnedRoundSnapshot(currentRound, "CONFIRMED", task);
            }
        } else {
            try {
                QmsTaskExecutionSnapshot snapshot = loadRequiredSnapshot(task.getCheckType(), task.getExecutionId());
                applyLiveResult(update, snapshot);
                QmsDispatchTaskRoundDO currentRound = ensureCurrentRound(task, snapshot);
                updateRoundSnapshot(currentRound,
                        approved ? "CONFIRMED" : currentRound.getRoundStatus(), snapshot);
            } catch (RuntimeException ignored) {
                // 流程状态仍需落库，原生检验单快照允许稍后由查询刷新。
            }
        }
        taskMapper.updateById(update);
        writeLog(task.getId(), "PROCESS_" + afterProcessStatus,
                StrUtil.blankToDefault(reason, "流程状态更新为" + afterProcessStatus), task.getProcessStatus(),
                afterProcessStatus, Map.of("processInstanceId", StrUtil.nullToEmpty(processInstanceId),
                        "status", status));
    }

    @Override
    public PageResult<QmsDispatchTaskSourceRespVO> getSourcePage(QmsDispatchTaskSourcePageReqVO reqVO) {
        return inspectionService.getSourcePage(reqVO);
    }

    @Override
    public List<QmsDispatchTaskCandidateItemRespVO> getSourceItems(String checkType, Long executionId) {
        return inspectionService.getSourceItems(checkType, executionId);
    }

    @Override
    public List<QmsDispatchTaskCandidateItemRespVO> getSourceItemTree(String checkType, Long executionId) {
        return inspectionService.getSourceItemTree(checkType, executionId);
    }

    @Override
    public PageResult<QmsDispatchTaskStandardRespVO> getStandardPage(QmsDispatchTaskStandardPageReqVO reqVO) {
        return inspectionService.getStandardPage(reqVO);
    }

    @Override
    public List<QmsDispatchTaskCandidateItemRespVO> getStandardItems(Long standardId) {
        return inspectionService.getStandardItems(standardId);
    }

    @Override
    public PageResult<QmsDispatchTaskRespVO> getPage(QmsDispatchTaskPageReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        LambdaQueryWrapperX<QmsDispatchTaskDO> query = new LambdaQueryWrapperX<QmsDispatchTaskDO>()
                .likeIfPresent(QmsDispatchTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(QmsDispatchTaskDO::getCheckType, normalizeNullableCheckType(reqVO.getCheckType()))
                .eqIfPresent(QmsDispatchTaskDO::getDispatchStatus, reqVO.getDispatchStatus())
                .eqIfPresent(QmsDispatchTaskDO::getPriority, reqVO.getPriority());
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            query.and(wrapper -> wrapper.like(QmsDispatchTaskDO::getTaskNo, keyword)
                    .or().like(QmsDispatchTaskDO::getExecutionNo, keyword)
                    .or().like(QmsDispatchTaskDO::getLotNo, keyword)
                    .or().like(QmsDispatchTaskDO::getMaterialCode, keyword)
                    .or().like(QmsDispatchTaskDO::getMaterialName, keyword));
        }
        if (loginUserId != null && "MY_ASSIGNED".equalsIgnoreCase(reqVO.getScope())) {
            query.eq(QmsDispatchTaskDO::getAssigneeUserId, loginUserId);
        } else if (loginUserId != null && "MY_CREATED".equalsIgnoreCase(reqVO.getScope())) {
            query.eq(QmsDispatchTaskDO::getCreator, String.valueOf(loginUserId));
        }
        query.orderByDesc(QmsDispatchTaskDO::getId);
        PageResult<QmsDispatchTaskDO> page = taskMapper.selectPage(reqVO, query);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public List<QmsDispatchTaskLogRespVO> getLogs(Long taskId) {
        validateTaskExists(taskId);
        return BeanUtils.toBean(logMapper.selectListByTaskId(taskId), QmsDispatchTaskLogRespVO.class);
    }

    private QmsDispatchTaskDO buildTask(QmsTaskExecutionSnapshot snapshot) {
        QmsDispatchTaskDO task = new QmsDispatchTaskDO();
        task.setSourceMode("INSPECTION_RECORD");
        task.setExecutionMode(EXECUTION_MODE_NATIVE);
        task.setObjectMode("SOURCE_RECORD");
        task.setSampleSelectionMode("SOURCE_ITEMS");
        task.setCheckType(snapshot.getCheckType());
        task.setObjectType(snapshot.getObjectType());
        task.setSourceType(snapshot.getSourceType());
        task.setSourceId(snapshot.getSourceId());
        task.setSourceNo(snapshot.getSourceNo());
        task.setExecutionId(snapshot.getExecutionId());
        task.setExecutionNo(snapshot.getExecutionNo());
        task.setExecutionRoute(snapshot.getExecutionRoute());
        task.setMaterialId(snapshot.getMaterialId());
        task.setMaterialCode(snapshot.getMaterialCode());
        task.setMaterialName(snapshot.getMaterialName());
        task.setMaterialSpec(snapshot.getMaterialSpec());
        task.setProductModel(snapshot.getProductModel());
        task.setOperationCode(snapshot.getOperationCode());
        task.setOperationName(snapshot.getOperationName());
        task.setMachineId(snapshot.getMachineId());
        task.setMachineCode(snapshot.getMachineCode());
        task.setMachineName(snapshot.getMachineName());
        task.setLotNo(snapshot.getLotNo());
        task.setCheckQty(snapshot.getCheckQty());
        task.setUnit(snapshot.getUnit());
        task.setStandardId(snapshot.getStandardId());
        task.setStandardNo(snapshot.getStandardNo());
        task.setStandardName(snapshot.getStandardName());
        task.setStandardVersion(snapshot.getStandardVersion());
        task.setPriority("NORMAL");
        task.setDispatchStatus("DRAFT");
        task.setCurrentRoundNo(1);
        task.setRecheckCount(0);
        applyLiveResult(task, snapshot);
        return task;
    }

    private List<QmsDispatchTaskItemDO> insertTaskItems(Long taskId, Integer roundNo,
                                                        List<QmsDispatchTaskInspectionService.GeneratedItem> items) {
        List<QmsDispatchTaskItemDO> inserted = new ArrayList<>(items.size());
        for (QmsDispatchTaskInspectionService.GeneratedItem generated : items) {
            QmsDispatchTaskItemDO item = new QmsDispatchTaskItemDO();
            item.setTaskId(taskId);
            item.setRoundNo(roundNo);
            item.setSourceItemId(generated.sourceItemId());
            item.setExecutionItemId(generated.executionItemId());
            item.setStandardItemId(generated.standardItemId());
            item.setPieceNo(generated.pieceNo());
            item.setInspectionItem(generated.inspectionItem());
            item.setStandardDesc(generated.standardDesc());
            item.setItemUnit(generated.itemUnit());
            item.setItemType(generated.itemType());
            item.setInspectionMethod(generated.inspectionMethod());
            item.setTestTool(generated.testTool());
            item.setSampleSize(generated.sampleSize());
            item.setResult("PENDING");
            item.setSort(generated.sort());
            taskItemMapper.insert(item);
            inserted.add(item);
        }
        return inserted;
    }

    private void insertTaskOwnedSamples(QmsDispatchTaskDO task, List<QmsDispatchTaskItemDO> taskItems,
                                        List<String> samplePieceNos) {
        List<String> pieceNos = samplePieceNos == null ? List.of() : samplePieceNos;
        int sampleCount = Objects.requireNonNullElse(task.getRequiredSampleQty(), 0);
        for (QmsDispatchTaskItemDO item : taskItems) {
            for (int sampleSeq = 1; sampleSeq <= sampleCount; sampleSeq++) {
                QmsDispatchTaskSampleResultDO sample = new QmsDispatchTaskSampleResultDO();
                sample.setTaskId(task.getId());
                sample.setTaskItemId(item.getId());
                sample.setRoundNo(currentRoundNo(task));
                sample.setSampleSeq(sampleSeq);
                if (StrUtil.isNotBlank(item.getPieceNo())) {
                    sample.setPieceNo(item.getPieceNo());
                } else if (pieceNos.size() >= sampleSeq) {
                    sample.setPieceNo(pieceNos.get(sampleSeq - 1));
                }
                sample.setResult("PENDING");
                sampleResultMapper.insert(sample);
            }
        }
    }

    private QmsDispatchTaskRoundDO insertTaskOwnedRound(QmsDispatchTaskDO task, String roundStatus) {
        QmsDispatchTaskRoundDO round = buildLegacyRoundSnapshot(task);
        round.setRoundStatus(roundStatus);
        applyTaskOwnedRoundSnapshot(round, task);
        taskRoundMapper.insert(round);
        return round;
    }
    private QmsDispatchTaskRoundDO insertRound(QmsDispatchTaskDO task, Long sourceExecutionId,
                                               String sourceExecutionNo, Long executionId,
                                               String executionNo, String roundStatus,
                                               QmsTaskExecutionSnapshot snapshot) {
        return insertRound(task, sourceExecutionId, sourceExecutionNo, executionId, executionNo, roundStatus,
                snapshot, currentRoundNo(task));
    }


    private QmsDispatchTaskRoundDO insertRound(QmsDispatchTaskDO task, Long sourceExecutionId,
                                               String sourceExecutionNo, Long executionId,
                                               String executionNo, String roundStatus,
                                               QmsTaskExecutionSnapshot snapshot, Integer roundNo) {
        QmsDispatchTaskRoundDO round = new QmsDispatchTaskRoundDO();
        round.setTaskId(task.getId());
        round.setRoundNo(roundNo);
        round.setCheckType(task.getCheckType());
        round.setInspectionScene(task.getInspectionScene());
        round.setSourceExecutionId(sourceExecutionId);
        round.setSourceExecutionNo(sourceExecutionNo);
        round.setExecutionId(executionId);
        round.setExecutionNo(executionNo);
        round.setRoundStatus(roundStatus);
        applyRoundSnapshot(round, snapshot);
        taskRoundMapper.insert(round);
        return round;
    }

    private void bindRoundRecheckDetail(QmsDispatchTaskRoundDO round, Long recheckDetailId) {
        if (round == null || recheckDetailId == null) {
            return;
        }
        QmsDispatchTaskRoundDO update = new QmsDispatchTaskRoundDO();
        update.setId(round.getId());
        update.setRecheckDetailId(recheckDetailId);
        taskRoundMapper.updateById(update);
        round.setRecheckDetailId(recheckDetailId);
    }

    private String startProcess(Long taskId, Long executionId, String taskNo,
                                QmsDispatchTaskWizardCreateReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Map<String, Object> variables = new HashMap<>();
        variables.put("qualityTaskId", taskId);
        variables.put("qualityTaskNo", taskNo);
        variables.put("checkType", normalizeCheckType(reqVO.getCheckType()));
        if (executionId != null) {
            variables.put("executionId", executionId);
        }
        variables.put("priority", StrUtil.blankToDefault(reqVO.getPriority(), "NORMAL"));
        variables.put("requiredFinishTime", reqVO.getRequiredFinishTime());
        variables.put("taskInstruction", reqVO.getTaskInstruction());
        Map<String, List<Long>> assignees = Map.of("task_execute", List.of(reqVO.getAssigneeUserId()));
        BpmProcessInstanceCreateReqDTO request = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_KEY)
                .setBusinessKey(String.valueOf(taskId))
                .setVariables(variables)
                .setStartUserSelectAssignees(assignees);
        try {
            return bpmProcessInstanceApi.createProcessInstance(loginUserId, request);
        } catch (ServiceException ex) {
            if (!Objects.equals(ex.getCode(), BPM_PROCESS_DEFINITION_NOT_EXISTS_CODE)) throw ex;
            try {
                bpmModelService.deployModel(BPM_MANAGER_USER_ID, BPM_MODEL_ID);
                return bpmProcessInstanceApi.createProcessInstance(loginUserId, request);
            } catch (RuntimeException retryEx) {
                throw exception(QMS_DISPATCH_BPM_NOT_PUBLISHED);
            }
        }
    }

    private Map<String, Object> buildProcessVariables(QmsDispatchTaskDO task, Long executionId,
                                                      String executionNo) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("qualityTaskId", task.getId());
        variables.put("qualityTaskNo", task.getTaskNo());
        variables.put("checkType", task.getCheckType());
        if (executionId != null) {
            variables.put("executionId", executionId);
        }
        if (StrUtil.isNotBlank(executionNo)) {
            variables.put("executionNo", executionNo);
        }
        variables.put("recheckCount", Objects.requireNonNullElse(task.getRecheckCount(), 0) + 1);
        return variables;
    }

    private void autoCloseLegacyProcess(QmsDispatchTaskDO task, Long loginUserId,
                                        Map<String, Object> variables) {
        org.flowable.task.api.Task legacyCloseTask = getActiveTask(task.getProcessInstanceId());
        if (legacyCloseTask == null || !"task_close".equals(legacyCloseTask.getTaskDefinitionKey())
                || !Objects.equals(legacyCloseTask.getAssignee(), String.valueOf(loginUserId))) {
            return;
        }
        workflowTaskService.approveTask(loginUserId, new BpmTaskApproveReqVO()
                .setId(legacyCloseTask.getId())
                .setReason("兼容旧版流程：结果确认后自动关闭")
                .setVariables(variables));
        writeLog(task.getId(), "LEGACY_PROCESS_AUTO_CLOSED", "旧版关闭节点已自动办理",
                "task_confirm", "COMPLETED", Map.of("flowTaskId", legacyCloseTask.getId()));
    }

    private int currentRoundNo(QmsDispatchTaskDO task) {
        return Objects.requireNonNullElse(task.getCurrentRoundNo(), 1);
    }

    private QmsDispatchTaskRoundDO ensureCurrentRound(QmsDispatchTaskDO task,
                                                      QmsTaskExecutionSnapshot snapshot) {
        int roundNo = currentRoundNo(task);
        QmsDispatchTaskRoundDO round = taskRoundMapper.selectByTaskIdAndRoundNo(task.getId(), roundNo);
        if (round != null) {
            return round;
        }
        round = buildLegacyRoundSnapshot(task);
        if (isTaskOwned(task)) {
            applyTaskOwnedRoundSnapshot(round, task);
        } else {
            applyRoundSnapshot(round, snapshot);
        }
        taskRoundMapper.insert(round);
        return round;
    }

    private QmsDispatchTaskRoundDO buildLegacyRoundSnapshot(QmsDispatchTaskDO task) {
        QmsDispatchTaskRoundDO round = new QmsDispatchTaskRoundDO();
        round.setTaskId(task.getId());
        round.setRoundNo(currentRoundNo(task));
        round.setCheckType(task.getCheckType());
        round.setInspectionScene(task.getInspectionScene());
        round.setSourceExecutionId(task.getSourceExecutionId());
        round.setSourceExecutionNo(task.getSourceExecutionNo());
        round.setExecutionId(task.getExecutionId());
        round.setExecutionNo(task.getExecutionNo());
        round.setRoundStatus("COMPLETED".equals(task.getDispatchStatus()) ? "CONFIRMED" : "EXECUTING");
        round.setInspectionStatus(task.getSourceStatus());
        round.setJudgment(task.getSourceJudgment());
        round.setInspectorName(task.getSourceInspectorName());
        round.setInspectionTime(task.getSourceInspectionTime());
        return round;
    }

    private QmsDispatchTaskRoundRespVO toRoundResp(QmsDispatchTaskRoundDO round) {
        return BeanUtils.toBean(round, QmsDispatchTaskRoundRespVO.class);
    }

    private void updateRoundSnapshot(QmsDispatchTaskRoundDO round, String roundStatus,
                                     QmsTaskExecutionSnapshot snapshot) {
        round.setRoundStatus(roundStatus);
        applyRoundSnapshot(round, snapshot);
        taskRoundMapper.updateById(round);
    }

    private void applyRoundSnapshot(QmsDispatchTaskRoundDO round, QmsTaskExecutionSnapshot snapshot) {
        round.setInspectionStatus(snapshot.getStatus());
        round.setJudgment(snapshot.getJudgment());
        round.setInspectorName(snapshot.getInspectorName());
        round.setInspectionTime(snapshot.getInspectionTime());
    }

    private void updateTaskOwnedRoundSnapshot(QmsDispatchTaskRoundDO round, String roundStatus,
                                              QmsDispatchTaskDO task) {
        round.setRoundStatus(roundStatus);
        applyTaskOwnedRoundSnapshot(round, task);
        taskRoundMapper.updateById(round);
    }

    private void applyTaskOwnedRoundSnapshot(QmsDispatchTaskRoundDO round, QmsDispatchTaskDO task) {
        round.setInspectionStatus(task.getSourceStatus());
        round.setJudgment(task.getSourceJudgment());
        round.setInspectorName(task.getSourceInspectorName());
        round.setInspectionTime(task.getSourceInspectionTime());
    }

    private QmsDispatchTaskSampleResultRespVO toSampleResp(QmsDispatchTaskSampleResultDO sample,
                                                           QmsDispatchTaskItemDO item) {
        QmsDispatchTaskSampleResultRespVO result =
                BeanUtils.toBean(sample, QmsDispatchTaskSampleResultRespVO.class);
        if (item != null) {
            result.setInspectionItem(item.getInspectionItem());
            result.setStandardDesc(item.getStandardDesc());
            result.setUnit(item.getItemUnit());
            result.setItemType(item.getItemType());
            result.setInspectionMethod(item.getInspectionMethod());
            result.setTestTool(item.getTestTool());
        }
        return result;
    }

    private boolean isTaskOwned(QmsDispatchTaskDO task) {
        return EXECUTION_MODE_TASK_OWNED.equalsIgnoreCase(
                StrUtil.blankToDefault(task.getExecutionMode(), EXECUTION_MODE_NATIVE));
    }
    private QmsDispatchTaskRespVO toResp(QmsDispatchTaskDO task) {
        QmsDispatchTaskRespVO resp = BeanUtils.toBean(task, QmsDispatchTaskRespVO.class);
        if (isTaskOwned(task)) {
            resp.setEffectiveStatus(resolveEffectiveStatus(task, task.getSourceStatus()));
        } else {
            try {
                QmsTaskExecutionSnapshot snapshot = findAdapter(task.getCheckType()).load(task.getExecutionId());
                if (snapshot == null) {
                    resp.setEffectiveStatus("SOURCE_MISSING");
                } else {
                    resp.setSourceStatus(snapshot.getStatus());
                    resp.setExecutionRoute(snapshot.getExecutionRoute());
                    resp.setSourceJudgment(snapshot.getJudgment());
                    resp.setSourceInspectorName(snapshot.getInspectorName());
                    resp.setSourceInspectionTime(snapshot.getInspectionTime());
                    resp.setItemCount(snapshot.getItemCount());
                    resp.setAbnormalCount(snapshot.getAbnormalCount());
                    resp.setLastSyncTime(LocalDateTime.now());
                    resp.setEffectiveStatus(resolveEffectiveStatus(task, snapshot.getStatus()));
                }
            } catch (RuntimeException ex) {
                resp.setEffectiveStatus("SOURCE_MISSING");
            }
        }
        if (StrUtil.isNotBlank(task.getCreator())) {
            try {
                resp.setCreatorId(Long.valueOf(task.getCreator()));
            } catch (NumberFormatException ignored) {
                // 兼容历史非数字 creator。
            }
        }
        return resp;
    }

    private void applyLiveResult(QmsDispatchTaskDO target, QmsTaskExecutionSnapshot snapshot) {
        target.setSourceStatus(snapshot.getStatus());
        target.setSourceJudgment(snapshot.getJudgment());
        target.setSourceInspectorName(snapshot.getInspectorName());
        target.setSourceInspectionTime(snapshot.getInspectionTime());
        target.setItemCount(snapshot.getItemCount());
        target.setAbnormalCount(snapshot.getAbnormalCount());
        target.setLastSyncTime(LocalDateTime.now());
    }

    private String resolveEffectiveStatus(QmsDispatchTaskDO task, String sourceStatus) {
        if ("CANCELLED".equals(task.getDispatchStatus())) {
            return "CANCELLED";
        }
        if ("COMPLETED".equals(task.getDispatchStatus())) {
            return "COMPLETED";
        }
        if ("DRAFT".equals(task.getDispatchStatus())) {
            return "DRAFT";
        }
        String activeTaskKey = getActiveTaskKey(task.getProcessInstanceId());
        if ("task_confirm".equals(activeTaskKey) || "task_close".equals(activeTaskKey)) {
            return "WAITING_AUDIT";
        }
        if ("task_execute".equals(activeTaskKey)) {
            return "PENDING";
        }
        if (sourceStatus == null) {
            return "PENDING";
        }
        return switch (sourceStatus.toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "PENDING";
            case "INSPECTING", "REWORKING" -> "IN_PROGRESS";
            case "WAITING_CONFIRM", "WAITING_QA", "WAIT_QA" -> "WAITING_AUDIT";
            case "COMPLETED", "FINISHED", "REJECTED", "ABNORMAL" -> "COMPLETED";
            case "SUSPENDED" -> "SUSPENDED";
            case "CANCELED", "CANCELLED" -> "CANCELLED";
            default -> "IN_PROGRESS";
        };
    }

    private QmsTaskExecutionSnapshot loadRequiredSnapshot(String checkType, Long executionId) {
        QmsTaskExecutionSnapshot snapshot;
        try {
            snapshot = findAdapter(checkType).load(executionId);
        } catch (RuntimeException ex) {
            throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, checkType, executionId);
        }
        if (snapshot == null) {
            throw exception(QMS_DISPATCH_EXECUTION_NOT_EXISTS, checkType, executionId);
        }
        return snapshot;
    }

    private QmsTaskExecutionAdapter findAdapter(String checkType) {
        Map<String, QmsTaskExecutionAdapter> adapterMap = executionAdapters.stream()
                .collect(Collectors.toMap(QmsTaskExecutionAdapter::getCheckType, Function.identity()));
        QmsTaskExecutionAdapter adapter = adapterMap.get(normalizeCheckType(checkType));
        if (adapter == null) {
            throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
        }
        return adapter;
    }

    private QmsDispatchTaskDO validateTaskExists(Long id) {
        QmsDispatchTaskDO task = taskMapper.selectById(id);
        if (task == null) {
            throw exception(QMS_DISPATCH_TASK_NOT_EXISTS);
        }
        return task;
    }

    private boolean isExecutionResultEditable(QmsDispatchTaskDO task, Long loginUserId) {
        if (!Objects.equals(task.getAssigneeUserId(), loginUserId)
                || !"RUNNING".equals(task.getProcessStatus())
                || !"DISPATCHED".equals(task.getDispatchStatus())
                || StrUtil.isBlank(task.getProcessInstanceId())) {
            return false;
        }
        return bpmTaskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey("task_execute")
                .taskAssignee(String.valueOf(loginUserId))
                .active()
                .count() > 0;
    }

    private String getActiveTaskKey(String processInstanceId) {
        org.flowable.task.api.Task activeTask = getActiveTask(processInstanceId);
        return activeTask == null ? null : activeTask.getTaskDefinitionKey();
    }

    private org.flowable.task.api.Task getActiveTask(String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            return null;
        }
        return bpmTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .listPage(0, 1).stream()
                .findFirst()
                .orElse(null);
    }

    private boolean isExecutionSubmitted(String sourceStatus) {
        if (StrUtil.isBlank(sourceStatus)) {
            return false;
        }
        return Set.of("WAITING_QA", "WAITING_CONFIRM", "COMPLETED", "FINISHED", "REJECTED", "ABNORMAL")
                .contains(sourceStatus.trim().toUpperCase(Locale.ROOT));
    }

    private String defaultProcessReason(String activeTaskKey) {
        return switch (StrUtil.blankToDefault(activeTaskKey, "")) {
            case "task_execute" -> "检验记录填写完成并提交";
            case "task_confirm" -> "检验任务结果已确认";
            case "task_close" -> "质量检验任务关闭";
            default -> "质量检验任务流程办理";
        };
    }

    private void writeLog(Long taskId, String actionType, String actionDesc, String beforeStatus,
                          String afterStatus, Object details) {
        QmsDispatchTaskLogDO log = new QmsDispatchTaskLogDO();
        log.setTaskId(taskId);
        log.setActionType(actionType);
        log.setActionDesc(actionDesc);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        log.setOperatorName(StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        log.setActionTime(LocalDateTime.now());
        log.setDetails(details == null ? null : JsonUtils.toJsonString(details));
        logMapper.insert(log);
    }

    private String normalizeCheckType(String checkType) {
        if (StrUtil.isBlank(checkType)) {
            throw exception(QMS_DISPATCH_TYPE_NOT_SUPPORTED, checkType);
        }
        return checkType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableCheckType(String checkType) {
        return StrUtil.isBlank(checkType) ? null : checkType.trim().toUpperCase(Locale.ROOT);
    }

    private void validateWizardRequest(QmsDispatchTaskWizardCreateReqVO reqVO) {
        String taskType = reqVO.getTaskType().trim().toUpperCase(Locale.ROOT);
        if (isProductEventRecheck(reqVO.getTriggerSource()) && !"RECHECK".equals(taskType)) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "产品异常事件只能建立复检任务");
        }
        if ("RECHECK".equals(taskType)) {
            if (reqVO.getSourceExecutionId() == null) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID, "复检来源检验单不能为空");
            }
            if (isProductEventRecheck(reqVO.getTriggerSource())) {
                if (StrUtil.isBlank(reqVO.getInspectionScene())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "产品异常事件检验场景不能为空");
                }
                if (StrUtil.isBlank(reqVO.getRejectReason())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "驳回说明不能为空");
                }
            }
        } else if ("ADDITIONAL".equals(taskType)) {
            if (reqVO.getStandardId() == null) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID, "加检标准不能为空");
            }
            String checkType = normalizeCheckType(reqVO.getCheckType());
            if (!"IQC".equals(checkType)
                    && StrUtil.isBlank(reqVO.getProductModelCode())
                    && StrUtil.isBlank(reqVO.getProductModelName())) {
                throw exception(QMS_DISPATCH_REQUEST_INVALID, "产品型号不能为空");
            }
            if ("FAI".equals(checkType)) {
                if (StrUtil.isBlank(reqVO.getBatchNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "加检批次号不能为空");
                }
                if (StrUtil.isBlank(reqVO.getOperationName())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "过程首检加检工序不能为空");
                }
                reqVO.setCheckQty(Objects.requireNonNullElse(reqVO.getCheckQty(), BigDecimal.ONE));
                reqVO.setUnit(StrUtil.blankToDefault(reqVO.getUnit(), resolveInspectionUnit(reqVO.getOperationName())));
                reqVO.setRequiredSampleQty(1);
                reqVO.setObjectMode("BATCH");
            } else if ("GLUE_BOARD_FAI".equals(checkType)) {
                if (StrUtil.isBlank(reqVO.getBatchNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "胶板批号不能为空");
                }
                if (StrUtil.isBlank(reqVO.getMaterialCode())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "胶板料号不能为空");
                }
                if (StrUtil.isBlank(reqVO.getOperationCode()) && StrUtil.isBlank(reqVO.getOperationName())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "胶板检测工序不能为空");
                }
                if (reqVO.getCheckQty() == null || reqVO.getCheckQty().signum() <= 0) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "送检米数必须大于零");
                }
                reqVO.setUnit("m");
                reqVO.setRequiredSampleQty(1);
                reqVO.setObjectMode("BATCH");
            } else if ("FQC".equals(checkType)) {
                if (StrUtil.isBlank(reqVO.getBatchNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "母批次号不能为空");
                }
                if (reqVO.getCheckQty() == null || reqVO.getCheckQty().signum() <= 0) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "成品加检数量必须大于零");
                }
                reqVO.setUnit("片");
                reqVO.setRequiredSampleQty(Math.max(1, reqVO.getCheckQty().intValue()));
                reqVO.setObjectMode("EXECUTION_PIECE");
            } else if ("OQC".equals(checkType)) {
                if (StrUtil.isBlank(reqVO.getBatchNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "发货通知单不能为空");
                }
                if (StrUtil.isBlank(reqVO.getWorkOrderNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "FQC单号不能为空");
                }
                if (reqVO.getCheckQty() == null || reqVO.getCheckQty().signum() <= 0) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "出货加检数量必须大于零");
                }
                reqVO.setUnit("片");
                reqVO.setRequiredSampleQty(Math.max(1, reqVO.getCheckQty().intValue()));
                reqVO.setObjectMode("EXECUTION_PIECE");
            } else if ("IQC".equals(checkType)) {
                if (StrUtil.isBlank(reqVO.getReceiptNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "关联收料单不能为空");
                }
                if (StrUtil.isBlank(reqVO.getBatchNo())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "进料批次号不能为空");
                }
                if (StrUtil.isBlank(reqVO.getMaterialCode()) || StrUtil.isBlank(reqVO.getMaterialName())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "进料检验标准必须绑定物料编码和物料名称");
                }
                if (reqVO.getCheckQty() == null || reqVO.getCheckQty().signum() <= 0) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "到货数量必须大于零");
                }
                if (reqVO.getArrivalDate() == null) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "来料日期不能为空");
                }
                if (StrUtil.isBlank(reqVO.getSupplierName())) {
                    throw exception(QMS_DISPATCH_REQUEST_INVALID, "供应商不能为空");
                }
                reqVO.setUnit(StrUtil.blankToDefault(reqVO.getUnit(), "件"));
                reqVO.setRequiredSampleQty(1);
                reqVO.setObjectMode("BATCH");
            } else {
                throw exception(QMS_DISPATCH_REQUEST_INVALID,
                        "当前向导仅开放过程首检、成品检验、出货检验、胶板检验和进料检验");
            }
            if (!"IQC".equals(checkType)) {
                reqVO.setProductModelCode(StrUtil.blankToDefault(
                        reqVO.getProductModelCode(), reqVO.getProductModelName()));
            }
            reqVO.setSampleSelectionMode("QUANTITY_ONLY");
            reqVO.setSamplePieceNos(List.of());
        } else {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "建立方式仅支持复检或加检");
        }
        if (StrUtil.isNotBlank(reqVO.getPriority())
                && !Set.of("NORMAL", "URGENT").contains(reqVO.getPriority().trim().toUpperCase(Locale.ROOT))) {
            throw exception(QMS_DISPATCH_REQUEST_INVALID, "优先级仅支持普通或紧急");
        }
    }

    private boolean isProductEventRecheck(String triggerSource) {
        return TRIGGER_PRODUCT_ABNORMAL_EVENT.equalsIgnoreCase(StrUtil.nullToEmpty(triggerSource).trim());
    }

    private String resolveInspectionUnit(String operationName) {
        String name = StrUtil.trimToEmpty(operationName);
        if (name.contains("配料")) {
            return "kg";
        }
        if (List.of("湿法", "磨皮", "粘胶1", "粘胶2", "胶板").stream().anyMatch(name::contains)) {
            return "m";
        }
        return "片";
    }
}
