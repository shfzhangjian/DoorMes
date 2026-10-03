package cn.iocoder.yudao.module.mes.service.workflow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.process.BpmProcessDefinitionRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcMrbReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcMrbReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
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
public class QmsNcWorkflowTaskProvider implements MesWorkflowBusinessTaskProvider {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> TERMINAL_STATUSES = Set.of("CLOSED", "CANCELLED");
    private static final String STATUS_MRB_REVIEW = "MRB_REVIEW";
    private static final String NODE_MRB_REVIEW = "MRB_REVIEW";
    private static final String RAW_MATERIAL = "RAW_MATERIAL";

    @Resource
    private QmsNcMrbReviewMapper qmsNcMrbReviewMapper;
    @Resource
    private QmsNcRecordMapper qmsNcRecordMapper;

    @Override
    public List<BpmTaskRespVO> getTodoTasks(Long userId, BpmTaskPageReqVO pageReqVO) {
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectPendingListByHandlerUserId(userId);
        return buildRows(userId, pageReqVO, reviews, false);
    }

    @Override
    public List<BpmTaskRespVO> getDoneTasks(Long userId, BpmTaskPageReqVO pageReqVO) {
        List<QmsNcMrbReviewDO> reviews = qmsNcMrbReviewMapper.selectHandledListByActualHandlerUserId(userId);
        return buildRows(userId, pageReqVO, reviews, true);
    }

    private List<BpmTaskRespVO> buildRows(Long userId, BpmTaskPageReqVO pageReqVO,
                                          List<QmsNcMrbReviewDO> reviews, boolean done) {
        if (CollUtil.isEmpty(reviews)) {
            return List.of();
        }
        Set<Long> ncRecordIds = reviews.stream()
                .map(QmsNcMrbReviewDO::getNcRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(ncRecordIds)) {
            return List.of();
        }
        Map<Long, QmsNcRecordDO> recordMap = qmsNcRecordMapper.selectBatchIds(ncRecordIds)
                .stream()
                .collect(Collectors.toMap(QmsNcRecordDO::getId, record -> record, (left, right) -> left));
        return reviews.stream()
                .map(review -> buildRow(userId, pageReqVO, recordMap.get(review.getNcRecordId()), review, done))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(row -> row.getProcessInstance().getBusinessKey(), row -> row,
                        this::preferNewerRow))
                .values()
                .stream()
                .sorted(Comparator.comparing(this::resolveRelationTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private BpmTaskRespVO buildRow(Long userId, BpmTaskPageReqVO pageReqVO,
                                   QmsNcRecordDO record, QmsNcMrbReviewDO review, boolean done) {
        if (record == null || (!done && TERMINAL_STATUSES.contains(record.getStatus()))) {
            return null;
        }
        if (!done && !isMrbReviewActive(record)) {
            return null;
        }
        if (!matchesProcessDefinition(pageReqVO, record) || !matchesStatus(pageReqVO, done)
                || !matchesCreateTime(pageReqVO, done ? review.getHandleTime() : firstNonNull(review.getDelegateTime(),
                record.getUpdateTime(), record.getCreateTime()))
                || !matchesKeyword(pageReqVO, record, review)) {
            return null;
        }
        String processInstanceId = StrUtil.blankToDefault(record.getProcessInstanceId(), "mes-ncr-" + record.getId());
        BpmTaskRespVO row = new BpmTaskRespVO();
        row.setId("mes-ncr-mrb-review-" + review.getId());
        row.setName(StrUtil.blankToDefault(record.getCurrentNodeName(), "责任单位会签"));
        row.setCreateTime(firstNonNull(review.getDelegateTime(), record.getUpdateTime(), record.getCreateTime()));
        row.setEndTime(done ? review.getHandleTime() : null);
        row.setStatus(done ? BpmTaskStatusEnum.APPROVE.getStatus() : BpmTaskStatusEnum.RUNNING.getStatus());
        row.setReason(done ? firstNotBlank(review.getReviewOpinion(), review.getDispositionDetail()) : null);
        row.setAssignee(done ? review.getActualHandlerUserId() : userId);
        row.setAssigneeUser(buildUser(done ? review.getActualHandlerUserId() : userId,
                done ? review.getActualHandlerUserName() : resolveTodoUserName(userId, review)));
        row.setTaskDefinitionKey("mrb_review");
        row.setProcessInstanceId(processInstanceId);
        row.setProcessInstance(buildProcessInstance(record, processInstanceId, buildVariables(record, review, done)));
        return row;
    }

    private BpmTaskRespVO.ProcessInstance buildProcessInstance(QmsNcRecordDO record, String processInstanceId,
                                                              Map<String, Object> variables) {
        BpmTaskRespVO.ProcessInstance instance = new BpmTaskRespVO.ProcessInstance();
        instance.setId(processInstanceId);
        instance.setName(resolveProcessName(record));
        instance.setCreateTime(record.getCreateTime());
        instance.setBusinessKey(String.valueOf(record.getId()));
        instance.setFormVariables(variables);
        instance.setProcessDefinition(buildProcessDefinition(record));
        instance.setStartUser(buildUser(record.getApplicantUserId(), record.getApplicantUserName()));
        return instance;
    }

    private BpmProcessDefinitionRespVO buildProcessDefinition(QmsNcRecordDO record) {
        BpmProcessDefinitionRespVO definition = new BpmProcessDefinitionRespVO();
        definition.setName(resolveProcessName(record));
        if (RAW_MATERIAL.equals(record.getSourceType())) {
            definition.setKey("qms_raw_material_ncr_disposition");
            definition.setFormCustomViewPath("/mes/quality/abnormal/raw-material-ncr");
        } else {
            definition.setKey("qms_ncr_disposition");
            definition.setFormCustomViewPath("/mes/quality/abnormal/ncr");
        }
        return definition;
    }

    private Map<String, Object> buildVariables(QmsNcRecordDO record, QmsNcMrbReviewDO review, boolean done) {
        return Map.ofEntries(
                Map.entry("mesWorkflowBusinessType", "NCR"),
                Map.entry("mesWorkflowBusinessTaskType", "NCR_MRB_REVIEW"),
                Map.entry("mesWorkflowBusinessTaskId", review.getId()),
                Map.entry("mesWorkflowBusinessDone", done),
                Map.entry("ncRecordId", record.getId()),
                Map.entry("ncNo", blank(record.getNcNo())),
                Map.entry("sourceType", blank(record.getSourceType())),
                Map.entry("sourceTypeName", blank(firstNotBlank(record.getSourceTypeName(),
                        RAW_MATERIAL.equals(record.getSourceType()) ? "原材料" : "产品"))),
                Map.entry("happenTime", format(record.getHappenTime())),
                Map.entry("processName", blank(record.getProcessName())),
                Map.entry("lotNo", blank(record.getLotNo())),
                Map.entry("defectQty", record.getDefectQty() == null ? "" : record.getDefectQty().stripTrailingZeros().toPlainString()),
                Map.entry("ncLevel", blank(record.getNcLevel())),
                Map.entry("ncLevelName", blank(record.getNcLevelName())),
                Map.entry("ncDescription", blank(record.getNcDescription())),
                Map.entry("defectName", blank(record.getDefectName())),
                Map.entry("currentNodeName", blank(record.getCurrentNodeName())),
                Map.entry("currentUserTaskTodoLabel", "责任单位会签"),
                Map.entry("currentUserTaskDoneLabel", "已处理责任单位会签")
        );
    }

    private boolean isMrbReviewActive(QmsNcRecordDO record) {
        return record != null && (STATUS_MRB_REVIEW.equals(record.getStatus())
                || NODE_MRB_REVIEW.equals(record.getCurrentNodeCode()));
    }

    private boolean matchesProcessDefinition(BpmTaskPageReqVO pageReqVO, QmsNcRecordDO record) {
        String key = pageReqVO.getProcessDefinitionKey();
        return StrUtil.isBlank(key) || StrUtil.equals(key, buildProcessDefinition(record).getKey());
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

    private boolean matchesKeyword(BpmTaskPageReqVO pageReqVO, QmsNcRecordDO record, QmsNcMrbReviewDO review) {
        String keyword = StrUtil.trimToEmpty(pageReqVO.getName());
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(record.getNcNo(), keyword)
                || StrUtil.containsIgnoreCase(resolveProcessName(record), keyword)
                || StrUtil.containsIgnoreCase(record.getCurrentNodeName(), keyword)
                || StrUtil.containsIgnoreCase(record.getNcDescription(), keyword)
                || StrUtil.containsIgnoreCase(record.getDefectName(), keyword)
                || StrUtil.containsIgnoreCase(review.getDeptName(), keyword)
                || StrUtil.containsIgnoreCase(review.getHandlerUserName(), keyword)
                || StrUtil.containsIgnoreCase(review.getDelegateUserName(), keyword);
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

    private String resolveProcessName(QmsNcRecordDO record) {
        return RAW_MATERIAL.equals(record.getSourceType()) ? "原材料不合格处置流程" : "不合格品处置流程";
    }

    private String resolveTodoUserName(Long userId, QmsNcMrbReviewDO review) {
        if (Objects.equals(userId, review.getDelegateUserId())) {
            return review.getDelegateUserName();
        }
        return review.getHandlerUserName();
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
