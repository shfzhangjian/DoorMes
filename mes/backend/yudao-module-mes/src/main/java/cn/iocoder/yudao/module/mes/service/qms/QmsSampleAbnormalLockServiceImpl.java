package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsSampleAbnormalLockRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsSampleAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsSampleAbnormalLockMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class QmsSampleAbnormalLockServiceImpl implements QmsSampleAbnormalLockService {

    private static final String INSPECTION_TYPE_FAI = "FAI";
    private static final String LOCK_STATUS_LOCKED = "LOCKED";
    private static final String LOCK_STATUS_RELEASED = "RELEASED";
    private static final String OBJECT_TYPE_MOTHER_ROLL = "MOTHER_ROLL";
    private static final String OBJECT_TYPE_SEGMENT = "SEGMENT";
    private static final String PROCESS_WET = "WET";
    private static final String PROCESS_ROUGH_GRINDING = "ROUGH_GRINDING";
    private static final String PROCESS_ADHESIVE1 = "ADHESIVE1";
    private static final String PROCESS_NAME_WET = "湿法";
    private static final String PROCESS_NAME_ROUGH_GRINDING = "磨皮";
    private static final String PROCESS_NAME_ADHESIVE1 = "粘胶1";
    private static final String SOURCE_MODULE_WET_REPORT = "WET_REPORT";
    private static final String SOURCE_MODULE_ROUGH_SECOND_SEGMENT = "ROUGH_GRINDING_SECOND_SEGMENT";
    private static final String SOURCE_MODULE_ADHESIVE_REPORT = "ADHESIVE_REPORT";
    private static final String RESULT_OK = "OK";
    private static final String RESULT_NG = "NG";
    private static final String TRIGGER_REWORK_RECHECK = "REWORK_RECHECK";
    private static final DateTimeFormatter LOCK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private QmsSampleAbnormalLockMapper sampleAbnormalLockMapper;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsNcPickQualificationService pickQualificationService;
    @Lazy
    @Resource
    private QmsFaiService qmsFaiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromFai(Long faiId) {
        if (faiId == null) {
            return;
        }
        QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(faiId);
        String result = normalizeTerminalResult(order == null ? null : order.getJudgment());
        FaiScope scope = resolveScope(order);
        if (order == null || result == null || scope == null) {
            return;
        }
        Long tenantId = order.getTenantId() == null ? currentTenantId() : order.getTenantId();
        QmsSampleAbnormalLockDO recheckLock =
                sampleAbnormalLockMapper.selectLatestByRecheckInspection(INSPECTION_TYPE_FAI, order.getId(), tenantId);
        if (recheckLock != null) {
            updateRecheckResult(recheckLock, order, scope, result);
            return;
        }

        QmsSampleAbnormalLockDO activeLock = sampleAbnormalLockMapper.selectLatestLocked(
                scope.getSourceProcessCode(), scope.getObjectType(), scope.getObjectNo(), tenantId);
        if (RESULT_OK.equals(result)) {
            if (activeLock != null && !Objects.equals(activeLock.getAbnormalInspectionId(), order.getId())) {
                updateRecheckResult(activeLock, order, scope, RESULT_OK);
            }
            return;
        }

        if (activeLock == null || Objects.equals(activeLock.getAbnormalInspectionId(), order.getId())) {
            QmsSampleAbnormalLockDO sameAbnormalLock = sampleAbnormalLockMapper.selectByAbnormalInspection(
                    order.getId(), scope.getObjectType(), scope.getObjectNo(), tenantId);
            upsertAbnormalLock(sameAbnormalLock, order, scope, tenantId);
            return;
        }
        updateRecheckResult(activeLock, order, scope, RESULT_NG);
    }

    @Override
    public PageResult<QmsSampleAbnormalLockRespVO> getPage(QmsSampleAbnormalLockPageReqVO pageReqVO) {
        PageResult<QmsSampleAbnormalLockDO> pageResult = sampleAbnormalLockMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, QmsSampleAbnormalLockRespVO.class);
    }

    @Override
    public QmsSampleAbnormalLockRespVO getActiveLock(String sourceProcessCode, String objectType, String objectNo, String qualificationObjectNo) {
        if (!StringUtils.hasText(sourceProcessCode) || !StringUtils.hasText(objectType)
                || !StringUtils.hasText(objectNo)) {
            return null;
        }
        String target = QmsNcPickQualificationService.normalize(qualificationObjectNo);
        String source = QmsNcPickQualificationService.normalize(objectNo);
        if (!target.equals(source) && !QmsNcPickQualificationService.isSegmentOf(source, target)) target = source;
        final String checkedObject = target;
        // 必须逐条检查：一个 NCR 改判不能掩盖同对象的其他异常或后续 NG 复检。
        return sampleAbnormalLockMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<QmsSampleAbnormalLockDO>()
                .eq(QmsSampleAbnormalLockDO::getTenantId, currentTenantId())
                .eq(QmsSampleAbnormalLockDO::getSourceProcessCode, sourceProcessCode)
                .eq(QmsSampleAbnormalLockDO::getObjectType, objectType)
                .eq(QmsSampleAbnormalLockDO::getObjectNo, objectNo)
                .eq(QmsSampleAbnormalLockDO::getLockStatus, LOCK_STATUS_LOCKED)
                .eq(QmsSampleAbnormalLockDO::getAbnormalResult, RESULT_NG)
                .isNotNull(QmsSampleAbnormalLockDO::getAbnormalFeedbackTime)
                .orderByDesc(QmsSampleAbnormalLockDO::getId)).stream()
                .filter(lock -> !pickQualificationService.isQualified(
                        RESULT_NG.equals(lock.getRecheckResult()) && lock.getRecheckInspectionId() != null
                                ? lock.getRecheckInspectionId() : lock.getAbnormalInspectionId(), checkedObject))
                .findFirst().map(lock -> BeanUtils.toBean(lock, QmsSampleAbnormalLockRespVO.class)).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsSampleAbnormalLockRespVO createRecheck(Long id) {
        QmsSampleAbnormalLockDO lock = sampleAbnormalLockMapper.selectById(id);
        if (lock == null) {
            throw new IllegalArgumentException("留样异常锁定记录不存在");
        }
        if (!LOCK_STATUS_LOCKED.equals(lock.getLockStatus())) {
            throw new IllegalArgumentException("当前留样异常已解锁，无需复检");
        }
        if (hasUnfinishedRecheck(lock)) {
            throw new IllegalArgumentException("当前复检单尚未反馈结果，请刷新后查看");
        }
        QmsFaiOrderDO abnormalOrder = qmsFaiOrderMapper.selectById(lock.getAbnormalInspectionId());
        if (abnormalOrder == null) {
            throw new IllegalArgumentException("原异常检验单不存在，无法创建复检单");
        }

        LocalDateTime now = LocalDateTime.now();
        QmsFaiSaveReqVO createReqVO = buildRecheckCreateReq(lock, abnormalOrder, now);
        Long recheckFaiId = qmsFaiService.createFai(createReqVO);
        QmsFaiOrderDO recheckOrder = qmsFaiOrderMapper.selectById(recheckFaiId);
        FaiScope scope = resolveScope(recheckOrder);
        if (scope == null) {
            scope = new FaiScope(lock.getSourceProcessCode(), lock.getSourceProcessName(),
                    lock.getObjectType(), lock.getObjectNo(), lock.getMotherBatchNo(), lock.getSegmentNo());
        }

        fillRecheckFields(lock, recheckOrder, scope, null);
        lock.setLockStatus(LOCK_STATUS_LOCKED);
        lock.setRecheckSubmitTime(firstNonNull(recheckOrder.getSubmissionTime(), now));
        lock.setRecheckFeedbackTime(null);
        lock.setRecheckCount((lock.getRecheckCount() == null ? 0 : lock.getRecheckCount()) + 1);
        lock.setLastRecheckApplyTime(now);
        sampleAbnormalLockMapper.updateById(lock);
        return BeanUtils.toBean(lock, QmsSampleAbnormalLockRespVO.class);
    }

    private void upsertAbnormalLock(QmsSampleAbnormalLockDO lock, QmsFaiOrderDO order,
                                    FaiScope scope, Long tenantId) {
        LocalDateTime feedbackTime = resolveFeedbackTime(order);
        boolean insert = lock == null;
        if (insert) {
            lock = new QmsSampleAbnormalLockDO();
            lock.setLockNo(generateLockNo(order));
            lock.setRecheckCount(0);
            lock.setTenantId(tenantId);
        }
        applyScope(lock, order, scope);
        lock.setLockStatus(LOCK_STATUS_LOCKED);
        lock.setAbnormalInspectionId(order.getId());
        lock.setAbnormalInspectionNo(order.getFaiNo());
        lock.setAbnormalInspectionType(INSPECTION_TYPE_FAI);
        lock.setAbnormalOperationCode(firstNotBlank(order.getOperationCode(), scope.getSourceProcessCode()));
        lock.setAbnormalOperationName(firstNotBlank(order.getOperationName(), scope.getSourceProcessName()));
        lock.setAbnormalObjectNo(scope.getObjectNo());
        lock.setAbnormalResult(RESULT_NG);
        lock.setAbnormalSubmitTime(order.getSubmissionTime());
        lock.setAbnormalFeedbackTime(feedbackTime);
        lock.setAbnormalSourceModule(order.getSourceModule());
        lock.setAbnormalSourceReportId(order.getSourceReportId());
        lock.setAbnormalSourceReportNo(order.getSourceReportNo());
        lock.setLockReason(buildLockReason(scope, feedbackTime));
        lock.setReleaseTime(null);
        lock.setReleaseReason(null);
        if (insert) {
            sampleAbnormalLockMapper.insert(lock);
        } else {
            sampleAbnormalLockMapper.updateById(lock);
        }
    }

    private void updateRecheckResult(QmsSampleAbnormalLockDO lock, QmsFaiOrderDO order,
                                     FaiScope scope, String result) {
        fillRecheckFields(lock, order, scope, result);
        lock.setRecheckFeedbackTime(resolveFeedbackTime(order));
        if (RESULT_OK.equals(result)) {
            lock.setLockStatus(LOCK_STATUS_RELEASED);
            lock.setReleaseTime(lock.getRecheckFeedbackTime());
            lock.setReleaseReason("复检检验单 " + order.getFaiNo() + " 判定OK，自动解锁");
        } else {
            lock.setLockStatus(LOCK_STATUS_LOCKED);
            lock.setReleaseTime(null);
            lock.setReleaseReason(null);
        }
        sampleAbnormalLockMapper.updateById(lock);
    }

    private void fillRecheckFields(QmsSampleAbnormalLockDO lock, QmsFaiOrderDO order,
                                   FaiScope scope, String result) {
        applyScope(lock, order, scope);
        lock.setRecheckInspectionId(order.getId());
        lock.setRecheckInspectionNo(order.getFaiNo());
        lock.setRecheckInspectionType(INSPECTION_TYPE_FAI);
        lock.setRecheckOperationCode(firstNotBlank(order.getOperationCode(), scope.getSourceProcessCode()));
        lock.setRecheckOperationName(firstNotBlank(order.getOperationName(), scope.getSourceProcessName()));
        lock.setRecheckObjectNo(scope.getObjectNo());
        lock.setRecheckResult(result);
        lock.setRecheckSubmitTime(order.getSubmissionTime());
        lock.setRecheckSourceModule(order.getSourceModule());
        lock.setRecheckSourceReportId(order.getSourceReportId());
        lock.setRecheckSourceReportNo(order.getSourceReportNo());
    }

    private void applyScope(QmsSampleAbnormalLockDO lock, QmsFaiOrderDO order, FaiScope scope) {
        lock.setObjectType(scope.getObjectType());
        lock.setObjectNo(scope.getObjectNo());
        lock.setSourceProcessCode(scope.getSourceProcessCode());
        lock.setSourceProcessName(scope.getSourceProcessName());
        lock.setPlanId(order.getPlanOrderId());
        lock.setPlanNo(order.getWorkOrderNo());
        lock.setOperationCode(order.getOperationCode());
        lock.setOperationName(order.getOperationName());
        lock.setMotherBatchNo(scope.getMotherBatchNo());
        lock.setSegmentNo(scope.getSegmentNo());
        lock.setProductionBatchNo(order.getProductBatchNo());
    }

    private QmsFaiSaveReqVO buildRecheckCreateReq(QmsSampleAbnormalLockDO lock, QmsFaiOrderDO abnormalOrder,
                                                  LocalDateTime now) {
        QmsFaiSaveReqVO reqVO = new QmsFaiSaveReqVO();
        reqVO.setWorkOrderNo(abnormalOrder.getWorkOrderNo());
        reqVO.setSourceReportId(abnormalOrder.getSourceReportId());
        reqVO.setSourceReportNo(abnormalOrder.getSourceReportNo());
        reqVO.setSourceModule(abnormalOrder.getSourceModule());
        reqVO.setSourceOperationCode(abnormalOrder.getSourceOperationCode());
        reqVO.setSourceOperationName(abnormalOrder.getSourceOperationName());
        reqVO.setPlanOrderId(abnormalOrder.getPlanOrderId());
        reqVO.setOperationCode(abnormalOrder.getOperationCode());
        reqVO.setOperationName(abnormalOrder.getOperationName());
        reqVO.setMachineId(abnormalOrder.getMachineId());
        reqVO.setMachineCode(abnormalOrder.getMachineCode());
        reqVO.setMachineName(abnormalOrder.getMachineName());
        reqVO.setMaterialId(abnormalOrder.getMaterialId());
        reqVO.setMaterialCode(abnormalOrder.getMaterialCode());
        reqVO.setMaterialName(abnormalOrder.getMaterialName());
        reqVO.setSpecification(abnormalOrder.getSpecification());
        reqVO.setProductModel(abnormalOrder.getProductModel());
        reqVO.setProductBatchNo(abnormalOrder.getProductBatchNo());
        reqVO.setGlueBoardStockId(abnormalOrder.getGlueBoardStockId());
        reqVO.setGlueBoardModel(abnormalOrder.getGlueBoardModel());
        reqVO.setGlueBoardMaterialCode(abnormalOrder.getGlueBoardMaterialCode());
        reqVO.setGluePlateBatchNo(abnormalOrder.getGluePlateBatchNo());
        reqVO.setSampleLength(abnormalOrder.getSampleLength());
        reqVO.setInspectionQty(abnormalOrder.getInspectionQty());
        reqVO.setProcessCategory(abnormalOrder.getProcessCategory());
        reqVO.setSubmissionType(abnormalOrder.getSubmissionType());
        reqVO.setWetSampleType(abnormalOrder.getWetSampleType());
        reqVO.setTriggerReason(TRIGGER_REWORK_RECHECK);
        reqVO.setStandardId(abnormalOrder.getStandardId());
        reqVO.setStandardNo(abnormalOrder.getStandardNo());
        reqVO.setStandardVersion(abnormalOrder.getStandardVersion());
        reqVO.setSubmissionTime(now);
        reqVO.setSubmitterName(abnormalOrder.getSubmitterName());
        reqVO.setInspectionTime(null);
        reqVO.setRemark(buildRecheckRemark(lock, abnormalOrder));
        reqVO.setSheetTemplateId(abnormalOrder.getSheetTemplateId());
        reqVO.setSheetTemplateCode(abnormalOrder.getSheetTemplateCode());
        reqVO.setSheetTemplateName(abnormalOrder.getSheetTemplateName());
        reqVO.setSheetTemplateVersion(abnormalOrder.getSheetTemplateVersion());
        reqVO.setEntryMode(abnormalOrder.getEntryMode());
        reqVO.setEntryLayout(abnormalOrder.getEntryLayout());
        reqVO.setSheetLocked(Boolean.FALSE);
        reqVO.setHistoricalBackfill(Boolean.FALSE);
        return reqVO;
    }

    private FaiScope resolveScope(QmsFaiOrderDO order) {
        if (order == null || !StringUtils.hasText(order.getProductBatchNo())) {
            return null;
        }
        if (SOURCE_MODULE_WET_REPORT.equals(order.getSourceModule()) || PROCESS_WET.equals(order.getProcessCategory())) {
            return new FaiScope(PROCESS_WET, PROCESS_NAME_WET, OBJECT_TYPE_MOTHER_ROLL,
                    order.getProductBatchNo(), order.getProductBatchNo(), null);
        }
        if (SOURCE_MODULE_ROUGH_SECOND_SEGMENT.equals(order.getSourceModule())) {
            return new FaiScope(PROCESS_ROUGH_GRINDING, PROCESS_NAME_ROUGH_GRINDING, OBJECT_TYPE_SEGMENT,
                    order.getProductBatchNo(), firstNotBlank(order.getGluePlateBatchNo(), order.getProductBatchNo()),
                    order.getProductBatchNo());
        }
        if (SOURCE_MODULE_ADHESIVE_REPORT.equals(order.getSourceModule()) || PROCESS_ADHESIVE1.equals(order.getProcessCategory())) {
            return new FaiScope(PROCESS_ADHESIVE1, PROCESS_NAME_ADHESIVE1, OBJECT_TYPE_SEGMENT,
                    order.getProductBatchNo(), firstNotBlank(order.getGluePlateBatchNo(), order.getProductBatchNo()),
                    order.getProductBatchNo());
        }
        return null;
    }

    private String normalizeTerminalResult(String judgment) {
        if (RESULT_OK.equals(judgment) || RESULT_NG.equals(judgment)) {
            return judgment;
        }
        return null;
    }

    private boolean hasUnfinishedRecheck(QmsSampleAbnormalLockDO lock) {
        return lock.getRecheckInspectionId() != null
                && !RESULT_OK.equals(lock.getRecheckResult())
                && !RESULT_NG.equals(lock.getRecheckResult());
    }

    private LocalDateTime resolveFeedbackTime(QmsFaiOrderDO order) {
        return firstNonNull(order.getQaTime(), order.getReleaseTime(), order.getInspectionTime(),
                order.getOperatorTime(), order.getUpdateTime(), LocalDateTime.now());
    }

    private String generateLockNo(QmsFaiOrderDO order) {
        return "QSAL-" + LOCK_NO_FORMATTER.format(LocalDateTime.now()) + "-" + order.getId();
    }

    private String buildLockReason(FaiScope scope, LocalDateTime feedbackTime) {
        String objectLabel = OBJECT_TYPE_MOTHER_ROLL.equals(scope.getObjectType()) ? "母卷" : "分段";
        return "当前" + objectLabel + " " + scope.getObjectNo() + " 因"
                + MESSAGE_TIME_FORMATTER.format(feedbackTime) + "，"
                + scope.getSourceProcessName() + " 留样送检NG异常，"
                + (PROCESS_WET.equals(scope.getSourceProcessCode()) || PROCESS_ROUGH_GRINDING.equals(scope.getSourceProcessCode())
                    ? "允许继续加工，裁切禁止提交检验、本段完工及工单完工，等待复检合格解锁。"
                    : "锁定不允许继续报工，等待复检确认后继续。");
    }

    private String buildRecheckRemark(QmsSampleAbnormalLockDO lock, QmsFaiOrderDO abnormalOrder) {
        String remark = "留样异常复检；原异常单：" + abnormalOrder.getFaiNo() + "；锁定编号：" + lock.getLockNo();
        if (StringUtils.hasText(abnormalOrder.getRemark())) {
            return remark + "；原备注：" + abnormalOrder.getRemark();
        }
        return remark;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
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
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static final class FaiScope {

        private final String sourceProcessCode;
        private final String sourceProcessName;
        private final String objectType;
        private final String objectNo;
        private final String motherBatchNo;
        private final String segmentNo;

        private FaiScope(String sourceProcessCode, String sourceProcessName, String objectType,
                         String objectNo, String motherBatchNo, String segmentNo) {
            this.sourceProcessCode = sourceProcessCode;
            this.sourceProcessName = sourceProcessName;
            this.objectType = objectType;
            this.objectNo = objectNo;
            this.motherBatchNo = motherBatchNo;
            this.segmentNo = segmentNo;
        }

        private String getSourceProcessCode() {
            return sourceProcessCode;
        }

        private String getSourceProcessName() {
            return sourceProcessName;
        }

        private String getObjectType() {
            return objectType;
        }

        private String getObjectNo() {
            return objectNo;
        }

        private String getMotherBatchNo() {
            return motherBatchNo;
        }

        private String getSegmentNo() {
            return segmentNo;
        }
    }

}
