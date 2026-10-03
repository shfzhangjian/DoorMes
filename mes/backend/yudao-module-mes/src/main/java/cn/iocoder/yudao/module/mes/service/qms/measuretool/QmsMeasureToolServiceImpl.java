package cn.iocoder.yudao.module.mes.service.qms.measuretool;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.dict.DictDataService;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolApplySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationDueHintRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationMonthlySummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskBatchConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCandidateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategoryListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolLedgerStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMaintainCalibrationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMaintainMsaReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolMsaRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolStatusRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolApplyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCategoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolMsaRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolStatusRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolApplyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolCalibrationRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolCalibrationTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolCategoryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolMsaRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.measuretool.QmsMeasureToolStatusRecordMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_APPLY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_APPLY_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_CATEGORY_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_CATEGORY_HIERARCHY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_CATEGORY_IN_USE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_LEDGER_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_LEDGER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_MSA_NOT_ENABLED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_RECORD_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_TASK_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_MEASURE_TOOL_VERSION_CONFLICT;

@Service
@Validated
public class QmsMeasureToolServiceImpl implements QmsMeasureToolService {

    private static final int INITIAL_VERSION = 0;
    private static final int DEFAULT_SORT = 0;
    private static final int DEFAULT_CYCLE_MONTHS = 12;
    private static final int DEFAULT_WARNING_DAYS = 30;
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String DICT_TYPE_CALIBRATION_METHOD = "mes_qms_measure_tool_calibration_method";

    private static final String STATUS_IN_USE = "IN_USE";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final String STATUS_SCRAPPED = "SCRAPPED";
    private static final String WARNING_NORMAL = "NORMAL";
    private static final String WARNING_DUE_SOON = "DUE_SOON";
    private static final String WARNING_OVERDUE = "OVERDUE";
    private static final String WARNING_NOT_CALIBRATED = "NOT_CALIBRATED";
    private static final String WARNING_NOT_REQUIRED = "NOT_REQUIRED";
    private static final String APPLY_DRAFT = "DRAFT";
    private static final String APPLY_APPROVING = "APPROVING";
    private static final String APPLY_APPROVED = "APPROVED";
    private static final String APPLY_REJECTED = "REJECTED";
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String TASK_CANCELLED = "CANCELLED";
    private static final String TASK_OVERDUE = "OVERDUE";
    private static final String TASK_SOURCE_AUTO = "AUTO";
    private static final String TASK_SOURCE_MONTHLY = "MONTHLY";
    private static final String TASK_SOURCE_MANUAL = "MANUAL";
    private static final String RESULT_UNQUALIFIED = "UNQUALIFIED";
    private static final String SOURCE_TASK = "TASK";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_SNAPSHOT = "SNAPSHOT";
    private static final String DICT_TYPE_CALIBRATION_ORG = "mes_qms_measure_tool_calibration_org";
    private static final String INTERNAL_MEASURE_TOOL_ADMIN_ROLE = "internal_measure_tool_admin";

    @Resource
    private QmsMeasureToolCategoryMapper categoryMapper;
    @Resource
    private QmsMeasureToolLedgerMapper ledgerMapper;
    @Resource
    private QmsMeasureToolApplyMapper applyMapper;
    @Resource
    private QmsMeasureToolCalibrationTaskMapper taskMapper;
    @Resource
    private QmsMeasureToolCalibrationRecordMapper recordMapper;
    @Resource
    private QmsMeasureToolMsaRecordMapper msaRecordMapper;
    @Resource
    private QmsMeasureToolStatusRecordMapper statusRecordMapper;
    @Resource
    private DictDataService dictDataService;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public Long createCategory(QmsMeasureToolCategorySaveReqVO reqVO) {
        validateCategoryCodeUnique(null, reqVO.getCategoryCode());
        Long parentId = validateCategoryParent(null, reqVO.getParentId());
        QmsMeasureToolCategoryDO category = BeanUtils.toBean(reqVO, QmsMeasureToolCategoryDO.class);
        category.setParentId(parentId);
        category.setStatus(defaultInt(reqVO.getStatus(), 1));
        category.setSort(defaultInt(reqVO.getSort(), DEFAULT_SORT));
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(QmsMeasureToolCategorySaveReqVO reqVO) {
        QmsMeasureToolCategoryDO existed = validateCategoryExists(reqVO.getId());
        validateCategoryCodeUnique(reqVO.getId(), reqVO.getCategoryCode());
        Long parentId = validateCategoryParent(reqVO.getId(), reqVO.getParentId());
        if (parentId > 0 && categoryMapper.selectCountByParentId(existed.getId()) > 0) {
            throw exception(QMS_MEASURE_TOOL_CATEGORY_HIERARCHY_INVALID);
        }
        QmsMeasureToolCategoryDO updateObj = BeanUtils.toBean(reqVO, QmsMeasureToolCategoryDO.class);
        updateObj.setParentId(parentId);
        updateObj.setStatus(defaultInt(reqVO.getStatus(), 1));
        updateObj.setSort(defaultInt(reqVO.getSort(), DEFAULT_SORT));
        if (categoryMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    public void deleteCategory(Long id) {
        validateCategoryExists(id);
        if (categoryMapper.selectCountByParentId(id) > 0 || ledgerMapper.selectCountByCategoryId(id) > 0) {
            throw exception(QMS_MEASURE_TOOL_CATEGORY_IN_USE);
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public QmsMeasureToolCategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<QmsMeasureToolCategoryDO> getCategoryList(QmsMeasureToolCategoryListReqVO reqVO) {
        return categoryMapper.selectList(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLedger(QmsMeasureToolLedgerSaveReqVO reqVO) {
        QmsMeasureToolLedgerDO ledger = BeanUtils.toBean(reqVO, QmsMeasureToolLedgerDO.class);
        fillLedgerDefaultsAndCategory(ledger);
        if (!hasInternalMeasureToolAdminRole()) {
            ledger.setExternalOpen(1);
        }
        ensureCalibrationOrgDictData(ledger.getCalibrationOrg());
        if (isBlank(ledger.getToolCode())) {
            ledger.setToolCode(generateNo("MT"));
        }
        validateToolCodeUnique(null, ledger.getToolCode());
        ledger.setVersion(INITIAL_VERSION);
        ledgerMapper.insert(ledger);
        return ledger.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLedger(QmsMeasureToolLedgerSaveReqVO reqVO) {
        QmsMeasureToolLedgerDO existed = validateLedgerAccessible(reqVO.getId());
        QmsMeasureToolLedgerDO updateObj = BeanUtils.toBean(reqVO, QmsMeasureToolLedgerDO.class);
        preserveLatestSnapshots(updateObj, existed);
        fillLedgerDefaultsAndCategory(updateObj);
        if (!hasInternalMeasureToolAdminRole() || reqVO.getExternalOpen() == null) {
            updateObj.setExternalOpen(existed.getExternalOpen());
        }
        ensureCalibrationOrgDictData(updateObj.getCalibrationOrg());
        validateToolCodeUnique(reqVO.getId(), updateObj.getToolCode());
        if (ledgerMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLedgerStatus(QmsMeasureToolLedgerStatusUpdateReqVO reqVO) {
        QmsMeasureToolLedgerDO existed = validateLedgerAccessible(reqVO.getId());
        if (!Objects.equals(existed.getVersion(), reqVO.getVersion())) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
        QmsMeasureToolLedgerDO updateObj = new QmsMeasureToolLedgerDO();
        updateObj.setId(existed.getId());
        updateObj.setVersion(reqVO.getVersion());
        updateObj.setStatus(reqVO.getStatus());
        if (ledgerMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
        QmsMeasureToolStatusRecordDO record = new QmsMeasureToolStatusRecordDO();
        record.setLedgerId(existed.getId());
        record.setPreviousStatus(existed.getStatus());
        record.setStatus(reqVO.getStatus());
        record.setHandler(trimToNull(reqVO.getHandler()));
        record.setHandleTime(reqVO.getHandleTime());
        record.setOaProcessNo(trimToNull(reqVO.getOaProcessNo()));
        record.setHandleRemark(trimToNull(reqVO.getHandleRemark()));
        record.setAttachments(trimToNull(reqVO.getAttachments()));
        statusRecordMapper.insert(record);
    }

    @Override
    public PageResult<QmsMeasureToolStatusRecordDO> getStatusRecordPage(QmsMeasureToolStatusRecordPageReqVO reqVO) {
        validateLedgerAccessible(reqVO.getLedgerId());
        return statusRecordMapper.selectPage(reqVO);
    }

    @Override
    public void deleteLedger(Long id) {
        validateLedgerAccessible(id);
        ledgerMapper.deleteById(id);
    }

    @Override
    public QmsMeasureToolLedgerDO getLedger(Long id) {
        boolean internalAdmin = hasInternalMeasureToolAdminRole();
        QmsMeasureToolLedgerDO ledger = validateLedgerAccessible(id);
        enrichLedgerRuntimeState(ledger);
        concealExternalOpen(ledger, internalAdmin);
        return ledger;
    }

    @Override
    public PageResult<QmsMeasureToolLedgerDO> getLedgerPage(QmsMeasureToolLedgerPageReqVO reqVO) {
        boolean internalAdmin = hasInternalMeasureToolAdminRole();
        if (!internalAdmin) {
            reqVO.setExternalOpen(1);
        }
        PageResult<QmsMeasureToolLedgerDO> pageResult = ledgerMapper.selectPage(reqVO);
        pageResult.getList().forEach(ledger -> {
            enrichLedgerRuntimeState(ledger);
            concealExternalOpen(ledger, internalAdmin);
        });
        return pageResult;
    }

    @Override
    public QmsMeasureToolLedgerSelectOptionRespVO getLedgerSelectOptions() {
        boolean internalAdmin = hasInternalMeasureToolAdminRole();
        Set<String> personnelNames = new HashSet<>();
        Set<String> usingDepartments = new HashSet<>();
        Set<String> calibrationOrgs = new HashSet<>();
        ledgerMapper.selectList().stream()
                .filter(ledger -> internalAdmin || isExternalOpen(ledger))
                .forEach(ledger -> {
            addOptionValue(personnelNames, ledger.getMaintainerName());
            addOptionValue(personnelNames, ledger.getResponsiblePerson());
            addOptionValue(personnelNames, ledger.getCalibrator());
            addOptionValue(personnelNames, ledger.getMsaAnalyst());
            addOptionValue(usingDepartments, ledger.getUsingDepartment());
            addOptionValue(calibrationOrgs, ledger.getCalibrationOrg());
        });
        recordMapper.selectList().forEach(record -> {
            addOptionValue(personnelNames, record.getCalibrator());
            addOptionValue(usingDepartments, record.getUsingDepartment());
            addOptionValue(calibrationOrgs, record.getCalibrationOrg());
        });
        msaRecordMapper.selectList().forEach(record -> {
            addOptionValue(personnelNames, record.getAnalyst());
            addOptionValue(personnelNames, record.getMaintainerName());
            addOptionValue(usingDepartments, record.getUsingDepartment());
        });
        QmsMeasureToolLedgerSelectOptionRespVO respVO = new QmsMeasureToolLedgerSelectOptionRespVO();
        respVO.setPersonnelNames(sortOptionValues(personnelNames));
        respVO.setUsingDepartments(sortOptionValues(usingDepartments));
        respVO.setCalibrationOrgs(sortOptionValues(calibrationOrgs));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importLedgerExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
        List<QmsMeasureToolLedgerImportExcelVO> rows = ExcelUtils.read(file, QmsMeasureToolLedgerImportExcelVO.class);
        boolean internalAdmin = hasInternalMeasureToolAdminRole();
        int importedCount = 0;
        for (int index = 0; index < rows.size(); index++) {
            QmsMeasureToolLedgerImportExcelVO row = rows.get(index);
            if (isBlank(row.getToolName())) {
                if (isBlank(row.getToolCode()) && isBlank(row.getCategoryName()) && isBlank(row.getAreaName())) {
                    continue;
                }
                throw new IllegalArgumentException("第" + (index + 2) + "行设备名称不能为空");
            }
            QmsMeasureToolLedgerDO ledger = BeanUtils.toBean(row, QmsMeasureToolLedgerDO.class);
            ledger.setCategoryId(resolveOrCreateCategoryId(
                    defaultString(row.getPositionName(), row.getStorageLocation()),
                    defaultString(row.getAreaName(), row.getCategoryName()),
                    index + 2));
            ledger.setStatus(normalizeToolStatus(row.getStatus()));
            ledger.setCalibrationType(normalizeCalibrationType(row.getCalibrationType()));
            ledger.setCalibrationResult(normalizeCalibrationResult(row.getCalibrationResult()));
            ledger.setMsaEnabled(normalizeMsaEnabled(row.getMsaEnabled()));
            ledger.setMsaResult(normalizeMsaResult(row.getMsaResult()));
            // 下次日期统一由“上次日期 + 周期”驱动，导入表中的旧下次日期不作为主数据来源。
            ledger.setNextCalibrationDate(null);
            ledger.setNextMsaDate(null);
            fillLedgerDefaultsAndCategory(ledger);
            ensureCalibrationOrgDictData(ledger.getCalibrationOrg());
            if (isBlank(ledger.getToolCode())) {
                ledger.setToolCode(generateNo("MT"));
            }
            QmsMeasureToolLedgerDO existed = ledgerMapper.selectByToolCode(ledger.getToolCode());
            if (existed == null) {
                if (!internalAdmin) {
                    ledger.setExternalOpen(1);
                }
                ledger.setVersion(INITIAL_VERSION);
                ledgerMapper.insert(ledger);
            } else {
                if (!internalAdmin && !isExternalOpen(existed)) {
                    throw exception(QMS_MEASURE_TOOL_LEDGER_NOT_EXISTS);
                }
                ledger.setId(existed.getId());
                ledger.setVersion(existed.getVersion());
                ledger.setExternalOpen(existed.getExternalOpen());
                if (ledgerMapper.updateById(ledger) == 0) {
                    throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
                }
            }
            importedCount++;
        }
        if (importedCount == 0) {
            throw new IllegalArgumentException("导入文件没有有效的量检具台账数据");
        }
        return importedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainCalibration(QmsMeasureToolMaintainCalibrationReqVO reqVO) {
        QmsMeasureToolLedgerDO ledger = validateLedgerAccessible(reqVO.getLedgerId());
        archiveCalibrationSnapshotIfPresent(ledger);
        LocalDate nextCalibrationDate = resolveNextCalibrationDate(reqVO.getCalibrationDate(), null,
                ledger.getCalibrationCycleMonths(), null);
        QmsMeasureToolLedgerDO updateObj = new QmsMeasureToolLedgerDO();
        updateObj.setId(ledger.getId());
        updateObj.setCalibrationType(trimToNull(reqVO.getCalibrationType()));
        String calibrationMethod = normalizeCalibrationMethod(reqVO.getCalibrationMethod());
        updateObj.setCalibrationMethod(calibrationMethod);
        String calibrationOrg = trimToNull(reqVO.getCalibrationOrg());
        ensureCalibrationOrgDictData(calibrationOrg);
        updateObj.setCalibrationOrg(calibrationOrg);
        updateObj.setCalibrator(trimToNull(reqVO.getCalibrator()));
        updateObj.setCalibrationResult(reqVO.getCalibrationResult());
        updateObj.setCertificateNo(trimToNull(reqVO.getCertificateNo()));
        updateObj.setCalibrationReport(trimToNull(reqVO.getCalibrationReport()));
        updateObj.setLastCalibrationDate(reqVO.getCalibrationDate());
        updateObj.setNextCalibrationDate(nextCalibrationDate);
        updateObj.setCalibrationStatus(resolveWarningStatus(nextCalibrationDate, ledger.getWarningDays()));
        if (!STATUS_SCRAPPED.equals(ledger.getStatus())) {
            updateObj.setStatus(RESULT_UNQUALIFIED.equals(reqVO.getCalibrationResult()) ? STATUS_STOPPED : STATUS_IN_USE);
        }
        if (ledgerMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void maintainMsa(QmsMeasureToolMaintainMsaReqVO reqVO) {
        QmsMeasureToolLedgerDO ledger = validateLedgerAccessible(reqVO.getLedgerId());
        if (!isMsaEnabled(ledger)) {
            throw exception(QMS_MEASURE_TOOL_MSA_NOT_ENABLED);
        }
        archiveMsaSnapshotIfPresent(ledger);
        LocalDate nextMsaDate = resolveNextCalibrationDate(reqVO.getMsaDate(), null,
                ledger.getMsaCycleMonths(), null);
        QmsMeasureToolLedgerDO updateObj = new QmsMeasureToolLedgerDO();
        updateObj.setId(ledger.getId());
        updateObj.setLastMsaDate(reqVO.getMsaDate());
        updateObj.setNextMsaDate(nextMsaDate);
        updateObj.setMsaResult(reqVO.getMsaResult());
        updateObj.setMsaReport(trimToNull(reqVO.getMsaReport()));
        updateObj.setMsaAnalyst(trimToNull(reqVO.getAnalyst()));
        updateObj.setMsaStatus(resolveMsaWarningStatus(nextMsaDate, ledger.getMsaWarningDays(), true));
        if (ledgerMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    public PageResult<QmsMeasureToolMsaRecordDO> getMsaRecordPage(QmsMeasureToolMsaRecordPageReqVO reqVO) {
        return msaRecordMapper.selectPage(reqVO);
    }

    @Override
    public Long createApply(QmsMeasureToolApplySaveReqVO reqVO) {
        QmsMeasureToolApplyDO apply = BeanUtils.toBean(reqVO, QmsMeasureToolApplyDO.class);
        fillApplyDefaultsAndCategory(apply);
        apply.setApplyNo(generateNo("MTA"));
        apply.setStatus(APPLY_DRAFT);
        apply.setVersion(INITIAL_VERSION);
        applyMapper.insert(apply);
        return apply.getId();
    }

    @Override
    public void updateApply(QmsMeasureToolApplySaveReqVO reqVO) {
        QmsMeasureToolApplyDO existed = validateApplyExists(reqVO.getId());
        if (!Set.of(APPLY_DRAFT, APPLY_REJECTED).contains(existed.getStatus())) {
            throw exception(QMS_MEASURE_TOOL_APPLY_STATUS_INVALID);
        }
        QmsMeasureToolApplyDO updateObj = BeanUtils.toBean(reqVO, QmsMeasureToolApplyDO.class);
        fillApplyDefaultsAndCategory(updateObj);
        updateObj.setApplyNo(existed.getApplyNo());
        updateObj.setStatus(existed.getStatus());
        if (applyMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    public void deleteApply(Long id) {
        QmsMeasureToolApplyDO apply = validateApplyExists(id);
        if (APPLY_APPROVED.equals(apply.getStatus())) {
            throw exception(QMS_MEASURE_TOOL_APPLY_STATUS_INVALID);
        }
        applyMapper.deleteById(id);
    }

    @Override
    public void submitApply(QmsMeasureToolApplyActionReqVO reqVO) {
        QmsMeasureToolApplyDO apply = validateApplyExists(reqVO.getId());
        if (!Set.of(APPLY_DRAFT, APPLY_REJECTED).contains(apply.getStatus())) {
            throw exception(QMS_MEASURE_TOOL_APPLY_STATUS_INVALID);
        }
        QmsMeasureToolApplyDO updateObj = new QmsMeasureToolApplyDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(APPLY_APPROVING);
        updateObj.setApprovalOpinion(trimToNull(reqVO.getOpinion()));
        if (applyMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveApply(QmsMeasureToolApplyActionReqVO reqVO) {
        QmsMeasureToolApplyDO apply = validateApplyExists(reqVO.getId());
        if (!APPLY_APPROVING.equals(apply.getStatus())) {
            throw exception(QMS_MEASURE_TOOL_APPLY_STATUS_INVALID);
        }
        QmsMeasureToolLedgerDO ledger = BeanUtils.toBean(apply, QmsMeasureToolLedgerDO.class);
        ledger.setId(null);
        ledger.setToolCode(generateNo("MT"));
        ledger.setLastCalibrationDate(null);
        ledger.setNextCalibrationDate(resolveNextCalibrationDate(null, apply.getPurchaseDate(),
                apply.getCalibrationCycleMonths(), null));
        ledger.setStatus(STATUS_IN_USE);
        ledger.setCalibrationStatus(resolveWarningStatus(ledger.getNextCalibrationDate(), ledger.getWarningDays()));
        ledger.setVersion(INITIAL_VERSION);
        validateToolCodeUnique(null, ledger.getToolCode());
        ledgerMapper.insert(ledger);

        QmsMeasureToolApplyDO updateObj = new QmsMeasureToolApplyDO();
        updateObj.setId(apply.getId());
        updateObj.setStatus(APPLY_APPROVED);
        updateObj.setApprovalOpinion(trimToNull(reqVO.getOpinion()));
        updateObj.setApprovedBy(trimToNull(reqVO.getOperatorName()));
        updateObj.setApprovedTime(LocalDateTime.now());
        updateObj.setLedgerId(ledger.getId());
        updateObj.setAssignedToolCode(ledger.getToolCode());
        if (applyMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
        return ledger.getId();
    }

    @Override
    public void rejectApply(QmsMeasureToolApplyActionReqVO reqVO) {
        QmsMeasureToolApplyDO apply = validateApplyExists(reqVO.getId());
        if (!APPLY_APPROVING.equals(apply.getStatus())) {
            throw exception(QMS_MEASURE_TOOL_APPLY_STATUS_INVALID);
        }
        QmsMeasureToolApplyDO updateObj = new QmsMeasureToolApplyDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(APPLY_REJECTED);
        updateObj.setApprovalOpinion(trimToNull(reqVO.getOpinion()));
        updateObj.setApprovedBy(trimToNull(reqVO.getOperatorName()));
        updateObj.setApprovedTime(LocalDateTime.now());
        if (applyMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    public QmsMeasureToolApplyDO getApply(Long id) {
        return applyMapper.selectById(id);
    }

    @Override
    public PageResult<QmsMeasureToolApplyDO> getApplyPage(QmsMeasureToolApplyPageReqVO reqVO) {
        return applyMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateCalibrationTasks(QmsMeasureToolCalibrationTaskGenerateReqVO reqVO) {
        if (reqVO.getLedgerIds() != null && !reqVO.getLedgerIds().isEmpty()) {
            return generateMonthlyCalibrationTasks(reqVO);
        }

        LocalDate today = LocalDate.now();
        int queryDays = reqVO.getWarningDays() == null ? 365 : reqVO.getWarningDays();
        List<QmsMeasureToolLedgerDO> candidates = ledgerMapper.selectWarningCandidates(today.plusDays(queryDays));
        int created = 0;
        for (QmsMeasureToolLedgerDO ledger : candidates) {
            int warningDays = reqVO.getWarningDays() == null
                    ? defaultInt(ledger.getWarningDays(), DEFAULT_WARNING_DAYS)
                    : reqVO.getWarningDays();
            LocalDate dueDate = ledger.getNextCalibrationDate();
            if (dueDate == null || dueDate.isAfter(today.plusDays(warningDays))) {
                continue;
            }
            if (taskMapper.selectOpenTask(ledger.getId(), dueDate) != null) {
                updateLedgerWarningStatus(ledger);
                continue;
            }
            QmsMeasureToolCalibrationTaskDO task = new QmsMeasureToolCalibrationTaskDO();
            task.setTaskNo(generateNo("MTC"));
            task.setLedgerId(ledger.getId());
            task.setToolCode(ledger.getToolCode());
            task.setToolName(ledger.getToolName());
            task.setCategoryId(ledger.getCategoryId());
            task.setCategoryName(ledger.getCategoryName());
            task.setUsingDepartment(ledger.getUsingDepartment());
            task.setKeeperName(ledger.getKeeperName());
            task.setDueDate(dueDate);
            task.setWarningDays(warningDays);
            task.setWarningStatus(resolveWarningStatus(dueDate, warningDays));
            task.setTaskStatus(WARNING_OVERDUE.equals(task.getWarningStatus()) ? TASK_OVERDUE : TASK_PENDING);
            task.setSourceType(TASK_SOURCE_AUTO);
            task.setGeneratedTime(LocalDateTime.now());
            task.setVersion(INITIAL_VERSION);
            taskMapper.insert(task);
            updateLedgerWarningStatus(ledger);
            created++;
        }
        return created;
    }

    @Override
    public List<QmsMeasureToolCalibrationTaskCandidateRespVO> getCalibrationTaskCandidates(
            QmsMeasureToolCalibrationTaskCandidateReqVO reqVO) {
        YearMonth targetMonth = resolveTargetMonth(reqVO.getMonth());
        return buildMonthlyTaskCandidates(targetMonth, reqVO.getCategoryId(),
                Boolean.TRUE.equals(reqVO.getIncludeNonMonthDue()));
    }

    @Override
    public QmsMeasureToolCalibrationDueHintRespVO getCalibrationDueHint(QmsMeasureToolCalibrationTaskCandidateReqVO reqVO) {
        YearMonth targetMonth = resolveTargetMonth(reqVO.getMonth());
        List<QmsMeasureToolCalibrationTaskCandidateRespVO> candidates =
                buildMonthlyTaskCandidates(targetMonth, reqVO.getCategoryId(), false);
        long overdueCount = candidates.stream().filter(item -> Boolean.TRUE.equals(item.getOverdue())).count();
        long dueSoonCount = candidates.stream()
                .filter(item -> Boolean.TRUE.equals(item.getDueInSelectedMonth())
                        && !Boolean.TRUE.equals(item.getOverdue()))
                .count();
        return new QmsMeasureToolCalibrationDueHintRespVO(targetMonth.toString(), dueSoonCount, overdueCount,
                dueSoonCount + overdueCount);
    }

    private int generateMonthlyCalibrationTasks(QmsMeasureToolCalibrationTaskGenerateReqVO reqVO) {
        YearMonth targetMonth = resolveTargetMonth(reqVO.getMonth());
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        Set<Long> selectedLedgerIds = new HashSet<>(reqVO.getLedgerIds().stream()
                .filter(Objects::nonNull)
                .toList());
        if (selectedLedgerIds.isEmpty()) {
            return 0;
        }

        int created = 0;
        for (QmsMeasureToolLedgerDO ledger : ledgerMapper.selectCalibrationTaskCandidates(null)) {
            if (!selectedLedgerIds.contains(ledger.getId())
                    || taskMapper.selectNonCancelledTaskInMonth(ledger.getId(), monthStart, monthEnd) != null) {
                continue;
            }
            int warningDays = reqVO.getWarningDays() == null
                    ? defaultInt(ledger.getWarningDays(), DEFAULT_WARNING_DAYS)
                    : reqVO.getWarningDays();
            LocalDate taskDueDate = resolveMonthlyTaskDueDate(targetMonth, ledger.getNextCalibrationDate());
            QmsMeasureToolCalibrationTaskDO task = new QmsMeasureToolCalibrationTaskDO();
            task.setTaskNo(generateNo("MTC"));
            task.setLedgerId(ledger.getId());
            task.setToolCode(ledger.getToolCode());
            task.setToolName(ledger.getToolName());
            task.setCategoryId(ledger.getCategoryId());
            task.setCategoryName(ledger.getCategoryName());
            task.setUsingDepartment(ledger.getUsingDepartment());
            task.setKeeperName(ledger.getKeeperName());
            task.setDueDate(taskDueDate);
            task.setWarningDays(warningDays);
            task.setWarningStatus(resolveWarningStatus(ledger.getNextCalibrationDate(), warningDays));
            task.setTaskStatus(TASK_PENDING);
            task.setSourceType(TASK_SOURCE_MONTHLY);
            task.setGeneratedTime(LocalDateTime.now());
            task.setRemark(buildMonthlyTaskRemark(targetMonth, ledger.getNextCalibrationDate(), taskDueDate));
            task.setVersion(INITIAL_VERSION);
            taskMapper.insert(task);
            updateLedgerWarningStatus(ledger);
            created++;
        }
        return created;
    }

    private List<QmsMeasureToolCalibrationTaskCandidateRespVO> buildMonthlyTaskCandidates(YearMonth targetMonth,
                                                                                          Long categoryId,
                                                                                          boolean includeNonMonthDue) {
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        return ledgerMapper.selectCalibrationTaskCandidates(categoryId).stream()
                .filter(ledger -> shouldShowMonthlyTaskCandidate(ledger.getNextCalibrationDate(), targetMonth,
                        includeNonMonthDue))
                .filter(ledger -> taskMapper.selectNonCancelledTaskInMonth(ledger.getId(), monthStart, monthEnd) == null)
                .map(ledger -> buildMonthlyTaskCandidateResp(targetMonth, ledger))
                .toList();
    }

    private QmsMeasureToolCalibrationTaskCandidateRespVO buildMonthlyTaskCandidateResp(YearMonth targetMonth,
                                                                                       QmsMeasureToolLedgerDO ledger) {
        LocalDate nextCalibrationDate = ledger.getNextCalibrationDate();
        QmsMeasureToolCalibrationTaskCandidateRespVO respVO = new QmsMeasureToolCalibrationTaskCandidateRespVO();
        respVO.setLedgerId(ledger.getId());
        respVO.setToolCode(ledger.getToolCode());
        respVO.setToolName(ledger.getToolName());
        respVO.setCategoryId(ledger.getCategoryId());
        respVO.setCategoryName(ledger.getCategoryName());
        respVO.setUsingDepartment(ledger.getUsingDepartment());
        respVO.setKeeperName(ledger.getKeeperName());
        respVO.setLastCalibrationDate(ledger.getLastCalibrationDate());
        respVO.setNextCalibrationDate(nextCalibrationDate);
        respVO.setTaskDueDate(resolveMonthlyTaskDueDate(targetMonth, nextCalibrationDate));
        respVO.setDueInSelectedMonth(isDateInMonth(nextCalibrationDate, targetMonth));
        respVO.setOverdue(isOverdue(nextCalibrationDate));
        respVO.setWarningStatus(resolveWarningStatus(nextCalibrationDate, ledger.getWarningDays()));
        return respVO;
    }

    private boolean shouldShowMonthlyTaskCandidate(LocalDate nextCalibrationDate, YearMonth targetMonth,
                                                   boolean includeNonMonthDue) {
        if (nextCalibrationDate == null) {
            return false;
        }
        return includeNonMonthDue || isDateInMonth(nextCalibrationDate, targetMonth) || isOverdue(nextCalibrationDate);
    }

    private LocalDate resolveMonthlyTaskDueDate(YearMonth targetMonth, LocalDate nextCalibrationDate) {
        LocalDate today = LocalDate.now();
        if (nextCalibrationDate != null && isDateInMonth(nextCalibrationDate, targetMonth)
                && !nextCalibrationDate.isBefore(today)) {
            return nextCalibrationDate;
        }
        if (YearMonth.from(today).equals(targetMonth)) {
            return today;
        }
        return targetMonth.atEndOfMonth();
    }

    private String buildMonthlyTaskRemark(YearMonth targetMonth, LocalDate nextCalibrationDate, LocalDate taskDueDate) {
        if (Objects.equals(nextCalibrationDate, taskDueDate)) {
            return null;
        }
        return "手工纳入 " + targetMonth + " 月度计量；台账原下次校准日期："
                + (nextCalibrationDate == null ? "-" : nextCalibrationDate);
    }

    @Override
    public void cancelCalibrationTask(QmsMeasureToolCalibrationTaskCancelReqVO reqVO) {
        QmsMeasureToolCalibrationTaskDO task = validateTaskExists(reqVO.getId());
        if (TASK_COMPLETED.equals(task.getTaskStatus()) || TASK_CANCELLED.equals(task.getTaskStatus())) {
            throw exception(QMS_MEASURE_TOOL_TASK_STATUS_INVALID);
        }
        QmsMeasureToolCalibrationTaskDO updateObj = new QmsMeasureToolCalibrationTaskDO();
        updateObj.setId(task.getId());
        updateObj.setTaskStatus(TASK_CANCELLED);
        updateObj.setRemark(trimToNull(reqVO.getReason()));
        if (taskMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    @Override
    public QmsMeasureToolCalibrationTaskDO getCalibrationTask(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public PageResult<QmsMeasureToolCalibrationTaskDO> getCalibrationTaskPage(QmsMeasureToolCalibrationTaskPageReqVO reqVO) {
        refreshOverdueTasks();
        return taskMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCalibrationRecord(QmsMeasureToolCalibrationRecordSaveReqVO reqVO) {
        QmsMeasureToolLedgerDO ledger = validateLedgerAccessible(reqVO.getLedgerId());
        QmsMeasureToolCalibrationTaskDO task = null;
        if (reqVO.getTaskId() != null) {
            task = validateTaskExists(reqVO.getTaskId());
            if (!Objects.equals(task.getLedgerId(), ledger.getId())
                    || TASK_COMPLETED.equals(task.getTaskStatus())
                    || TASK_CANCELLED.equals(task.getTaskStatus())
                    || (TASK_IN_PROGRESS.equals(task.getTaskStatus()) && task.getRecordId() != null)) {
                throw exception(QMS_MEASURE_TOOL_TASK_STATUS_INVALID);
            }
        }

        QmsMeasureToolCalibrationRecordDO record = BeanUtils.toBean(reqVO, QmsMeasureToolCalibrationRecordDO.class);
        record.setRecordNo(generateNo("MTR"));
        record.setSourceType(task == null ? defaultString(reqVO.getSourceType(), SOURCE_MANUAL) : SOURCE_TASK);
        fillRecordLedgerSnapshot(record, ledger);
        record.setCalibrationMethod(normalizeCalibrationMethod(record.getCalibrationMethod()));
        ensureCalibrationOrgDictData(record.getCalibrationOrg());
        record.setNextCalibrationDate(resolveNextCalibrationDate(reqVO.getCalibrationDate(), null,
                ledger.getCalibrationCycleMonths(), null));
        record.setValidUntil(reqVO.getValidUntil() == null ? record.getNextCalibrationDate() : reqVO.getValidUntil());
        record.setVersion(INITIAL_VERSION);
        recordMapper.insert(record);

        if (task != null) {
            submitTaskRecordForConfirm(task, record);
        } else {
            applyRecordToLedger(record, ledger);
            if (SOURCE_MANUAL.equals(record.getSourceType())) {
                createCompletedManualTaskByRecord(record, ledger);
            }
        }
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalibrationRecord(QmsMeasureToolCalibrationRecordSaveReqVO reqVO) {
        QmsMeasureToolCalibrationRecordDO existed = validateRecordExists(reqVO.getId());
        validateLedgerAccessible(existed.getLedgerId());
        QmsMeasureToolLedgerDO ledger = validateLedgerAccessible(reqVO.getLedgerId());
        QmsMeasureToolCalibrationRecordDO updateObj = BeanUtils.toBean(reqVO, QmsMeasureToolCalibrationRecordDO.class);
        updateObj.setRecordNo(existed.getRecordNo());
        updateObj.setTaskId(reqVO.getTaskId() == null ? existed.getTaskId() : reqVO.getTaskId());
        QmsMeasureToolCalibrationTaskDO task = updateObj.getTaskId() == null
                ? null
                : taskMapper.selectById(updateObj.getTaskId());
        updateObj.setSourceType(task == null
                ? defaultString(reqVO.getSourceType(), existed.getSourceType())
                : (TASK_SOURCE_MANUAL.equals(task.getSourceType()) ? SOURCE_MANUAL : SOURCE_TASK));
        fillRecordLedgerSnapshot(updateObj, ledger);
        updateObj.setCalibrationMethod(normalizeCalibrationMethod(updateObj.getCalibrationMethod()));
        ensureCalibrationOrgDictData(updateObj.getCalibrationOrg());
        updateObj.setNextCalibrationDate(resolveNextCalibrationDate(reqVO.getCalibrationDate(), null,
                ledger.getCalibrationCycleMonths(), null));
        updateObj.setValidUntil(reqVO.getValidUntil() == null ? updateObj.getNextCalibrationDate() : reqVO.getValidUntil());
        if (recordMapper.updateById(updateObj) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }

        if (task != null && TASK_IN_PROGRESS.equals(task.getTaskStatus())) {
            if (!Objects.equals(task.getLedgerId(), updateObj.getLedgerId())) {
                throw exception(QMS_MEASURE_TOOL_TASK_STATUS_INVALID);
            }
            updateTaskRecordForConfirm(task, updateObj);
            return;
        }
        if (task != null && TASK_SOURCE_MANUAL.equals(task.getSourceType())) {
            updateCompletedManualTaskByRecord(task, updateObj, ledger);
        } else if (task != null && TASK_COMPLETED.equals(task.getTaskStatus())) {
            updateCompletedTaskByRecord(task, updateObj);
        }

        recalculateLedgerFromLatestRecord(existed.getLedgerId());
        if (!Objects.equals(existed.getLedgerId(), updateObj.getLedgerId())) {
            recalculateLedgerFromLatestRecord(updateObj.getLedgerId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchConfirmCalibrationTasks(QmsMeasureToolCalibrationTaskBatchConfirmReqVO reqVO) {
        int confirmedCount = 0;
        Set<Long> taskIds = new HashSet<>(reqVO.getIds());
        for (Long taskId : taskIds) {
            QmsMeasureToolCalibrationTaskDO task = validateTaskExists(taskId);
            if (!TASK_IN_PROGRESS.equals(task.getTaskStatus()) || task.getRecordId() == null) {
                throw exception(QMS_MEASURE_TOOL_TASK_STATUS_INVALID);
            }
            QmsMeasureToolCalibrationRecordDO record = validateRecordExists(task.getRecordId());
        QmsMeasureToolLedgerDO ledger = validateLedgerAccessible(record.getLedgerId());
            if (!Objects.equals(task.getLedgerId(), record.getLedgerId())) {
                throw exception(QMS_MEASURE_TOOL_TASK_STATUS_INVALID);
            }
            applyRecordToLedger(record, ledger);
            completeTaskByRecord(task, record);
            confirmedCount++;
        }
        return confirmedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCalibrationRecord(Long id) {
        QmsMeasureToolCalibrationRecordDO record = validateRecordExists(id);
        recordMapper.deleteById(id);
        if (record.getTaskId() != null) {
            QmsMeasureToolCalibrationTaskDO task = taskMapper.selectById(record.getTaskId());
            if (task != null && Objects.equals(task.getRecordId(), record.getId())) {
                if (TASK_SOURCE_MANUAL.equals(task.getSourceType())) {
                    taskMapper.deleteById(task.getId());
                } else {
                    taskMapper.update(null, new LambdaUpdateWrapper<QmsMeasureToolCalibrationTaskDO>()
                            .eq(QmsMeasureToolCalibrationTaskDO::getId, task.getId())
                            .set(QmsMeasureToolCalibrationTaskDO::getTaskStatus, resolveTaskStatus(task.getDueDate()))
                            .set(QmsMeasureToolCalibrationTaskDO::getCompletedTime, null)
                            .set(QmsMeasureToolCalibrationTaskDO::getRecordId, null)
                            .set(QmsMeasureToolCalibrationTaskDO::getHandlerName, null));
                }
            }
        }
        recalculateLedgerFromLatestRecord(record.getLedgerId());
    }

    @Override
    public QmsMeasureToolCalibrationRecordDO getCalibrationRecord(Long id) {
        return recordMapper.selectById(id);
    }

    @Override
    public PageResult<QmsMeasureToolCalibrationRecordDO> getCalibrationRecordPage(QmsMeasureToolCalibrationRecordPageReqVO reqVO) {
        return recordMapper.selectPage(reqVO);
    }

    @Override
    public List<QmsMeasureToolCalibrationMonthlySummaryRespVO> getCalibrationMonthlySummary(QmsMeasureToolCalibrationMonthlySummaryReqVO reqVO) {
        LocalDate[] range = reqVO.getCalibrationDate();
        LocalDate startDate = range != null && range.length > 0 && range[0] != null
                ? range[0]
                : LocalDate.now().withDayOfYear(1);
        LocalDate endDate = range != null && range.length > 1 && range[1] != null
                ? range[1]
                : LocalDate.now();

        List<QmsMeasureToolCalibrationRecordDO> records = recordMapper.selectSummaryList(reqVO, startDate, endDate);
        List<QmsMeasureToolCalibrationTaskDO> tasks = taskMapper.selectTasksByMonth(startDate, endDate,
                reqVO.getCategoryId(), reqVO.getUsingDepartment());

        Map<SummaryKey, SummaryAccumulator> summaryMap = new LinkedHashMap<>();
        tasks.forEach(task -> summaryMap.computeIfAbsent(SummaryKey.of(task.getDueDate(), task.getCategoryId(),
                        task.getCategoryName(), task.getUsingDepartment()), SummaryAccumulator::new)
                .taskCount++);
        records.forEach(record -> {
            SummaryAccumulator acc = summaryMap.computeIfAbsent(SummaryKey.of(record.getCalibrationDate(),
                    record.getCategoryId(), record.getCategoryName(), record.getUsingDepartment()), SummaryAccumulator::new);
            acc.recordCount++;
            if ("QUALIFIED".equals(record.getCalibrationResult())) {
                acc.qualifiedCount++;
            } else if (RESULT_UNQUALIFIED.equals(record.getCalibrationResult())) {
                acc.unqualifiedCount++;
            } else if ("LIMITED".equals(record.getCalibrationResult())) {
                acc.limitedCount++;
            }
            if (record.getTaskId() != null) {
                QmsMeasureToolCalibrationTaskDO task = taskMapper.selectById(record.getTaskId());
                if (task != null && record.getCalibrationDate() != null && task.getDueDate() != null
                        && record.getCalibrationDate().isAfter(task.getDueDate())) {
                    acc.overdueCompletedCount++;
                }
            }
        });

        return summaryMap.values().stream()
                .sorted(Comparator.comparing((SummaryAccumulator acc) -> acc.key.month)
                        .thenComparing(acc -> defaultString(acc.key.categoryName, ""))
                        .thenComparing(acc -> defaultString(acc.key.usingDepartment, "")))
                .map(SummaryAccumulator::toResp)
                .toList();
    }

    private YearMonth resolveTargetMonth(String month) {
        return isBlank(month) ? YearMonth.now() : YearMonth.parse(month);
    }

    private boolean isDateInMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private boolean isOverdue(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    private void validateCategoryCodeUnique(Long id, String categoryCode) {
        QmsMeasureToolCategoryDO existed = categoryMapper.selectByCode(categoryCode);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw exception(QMS_MEASURE_TOOL_CATEGORY_CODE_EXISTS);
        }
    }

    private QmsMeasureToolCategoryDO validateCategoryExists(Long id) {
        QmsMeasureToolCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(QMS_MEASURE_TOOL_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    /** 量检具分类仅支持“位置 -> 区域”两级，避免把区域再挂到区域下。 */
    private Long validateCategoryParent(Long categoryId, Long parentId) {
        long normalizedParentId = defaultLong(parentId);
        if (normalizedParentId == 0) {
            return 0L;
        }
        if (Objects.equals(categoryId, normalizedParentId)) {
            throw exception(QMS_MEASURE_TOOL_CATEGORY_HIERARCHY_INVALID);
        }
        QmsMeasureToolCategoryDO parent = validateCategoryExists(normalizedParentId);
        if (defaultLong(parent.getParentId()) > 0) {
            throw exception(QMS_MEASURE_TOOL_CATEGORY_HIERARCHY_INVALID);
        }
        return normalizedParentId;
    }

    private QmsMeasureToolLedgerDO validateLedgerExists(Long id) {
        QmsMeasureToolLedgerDO ledger = ledgerMapper.selectById(id);
        if (ledger == null) {
            throw exception(QMS_MEASURE_TOOL_LEDGER_NOT_EXISTS);
        }
        return ledger;
    }

    /** 普通用户只可访问已对外开放的量检具；未开放记录按不存在处理，避免泄露其存在性。 */
    private QmsMeasureToolLedgerDO validateLedgerAccessible(Long id) {
        QmsMeasureToolLedgerDO ledger = validateLedgerExists(id);
        if (!hasInternalMeasureToolAdminRole() && !isExternalOpen(ledger)) {
            throw exception(QMS_MEASURE_TOOL_LEDGER_NOT_EXISTS);
        }
        return ledger;
    }

    private boolean hasInternalMeasureToolAdminRole() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return SecurityFrameworkUtils.skipPermissionCheck()
                || (loginUserId != null && (permissionApi.getSuperAdminUserIdList().contains(loginUserId)
                || permissionApi.hasAnyRoles(loginUserId, INTERNAL_MEASURE_TOOL_ADMIN_ROLE)));
    }

    private boolean isExternalOpen(QmsMeasureToolLedgerDO ledger) {
        return ledger != null && Integer.valueOf(1).equals(ledger.getExternalOpen());
    }

    private void concealExternalOpen(QmsMeasureToolLedgerDO ledger, boolean internalAdmin) {
        if (ledger != null && !internalAdmin) {
            ledger.setExternalOpen(null);
        }
    }

    private QmsMeasureToolApplyDO validateApplyExists(Long id) {
        QmsMeasureToolApplyDO apply = applyMapper.selectById(id);
        if (apply == null) {
            throw exception(QMS_MEASURE_TOOL_APPLY_NOT_EXISTS);
        }
        return apply;
    }

    private QmsMeasureToolCalibrationTaskDO validateTaskExists(Long id) {
        QmsMeasureToolCalibrationTaskDO task = taskMapper.selectById(id);
        if (task == null) {
            throw exception(QMS_MEASURE_TOOL_TASK_NOT_EXISTS);
        }
        return task;
    }

    private QmsMeasureToolCalibrationRecordDO validateRecordExists(Long id) {
        QmsMeasureToolCalibrationRecordDO record = recordMapper.selectById(id);
        if (record == null) {
            throw exception(QMS_MEASURE_TOOL_RECORD_NOT_EXISTS);
        }
        return record;
    }

    private void validateToolCodeUnique(Long id, String toolCode) {
        QmsMeasureToolLedgerDO existed = ledgerMapper.selectByToolCode(toolCode);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw exception(QMS_MEASURE_TOOL_LEDGER_CODE_EXISTS);
        }
    }

    private void ensureCalibrationOrgDictData(String rawCalibrationOrg) {
        ensureMeasureToolDictData(DICT_TYPE_CALIBRATION_ORG, rawCalibrationOrg, "量检具校准维护自动补充");
    }

    private void ensureMeasureToolDictData(String dictType, String rawValue, String remark) {
        String value = trimToNull(rawValue);
        if (value == null || dictDataService.getDictData(dictType, value) != null) {
            return;
        }
        DictDataSaveReqVO reqVO = new DictDataSaveReqVO();
        reqVO.setDictType(dictType);
        reqVO.setLabel(value);
        reqVO.setValue(value);
        reqVO.setSort((int) dictDataService.getDictDataCountByDictType(dictType) + 1);
        reqVO.setStatus(0);
        reqVO.setColorType("default");
        reqVO.setRemark(remark);
        dictDataService.createDictData(reqVO);
    }

    private void fillLedgerDefaultsAndCategory(QmsMeasureToolLedgerDO ledger) {
        ledger.setToolCode(trimToNull(ledger.getToolCode()));
        ledger.setBodyNo(trimToNull(ledger.getBodyNo()));
        ledger.setCalibrationMethod(normalizeCalibrationMethod(ledger.getCalibrationMethod()));
        ledger.setCalibrationOrg(trimToNull(ledger.getCalibrationOrg()));
        ledger.setCalibrator(trimToNull(ledger.getCalibrator()));
        ledger.setUsingDepartment(trimToNull(ledger.getUsingDepartment()));
        ledger.setResponsiblePerson(trimToNull(ledger.getResponsiblePerson()));
        ledger.setCalibrationCycleMonths(defaultInt(ledger.getCalibrationCycleMonths(), DEFAULT_CYCLE_MONTHS));
        ledger.setWarningDays(defaultInt(ledger.getWarningDays(), DEFAULT_WARNING_DAYS));
        ledger.setStatus(defaultString(ledger.getStatus(), STATUS_IN_USE));
        ledger.setMaintainerName(trimToNull(ledger.getMaintainerName()));
        ledger.setKeeperName(ledger.getMaintainerName());
        ledger.setExternalOpen(defaultInt(ledger.getExternalOpen(), 1));
        fillLedgerLocationAndAreaSnapshot(ledger);
        ledger.setNextCalibrationDate(resolveNextCalibrationDate(ledger.getLastCalibrationDate(), ledger.getPurchaseDate(),
                ledger.getCalibrationCycleMonths(), null));
        ledger.setCalibrationStatus(resolveWarningStatus(ledger.getNextCalibrationDate(), ledger.getWarningDays()));
        ledger.setMsaEnabled(defaultInt(ledger.getMsaEnabled(), 0));
        if (isMsaEnabled(ledger)) {
            ledger.setMsaCycleMonths(defaultInt(ledger.getMsaCycleMonths(), DEFAULT_CYCLE_MONTHS));
            ledger.setMsaWarningDays(defaultInt(ledger.getMsaWarningDays(), DEFAULT_WARNING_DAYS));
            ledger.setNextMsaDate(resolveNextCalibrationDate(ledger.getLastMsaDate(), ledger.getPurchaseDate(),
                    ledger.getMsaCycleMonths(), null));
            ledger.setMsaStatus(resolveMsaWarningStatus(ledger.getNextMsaDate(), ledger.getMsaWarningDays(), true));
        } else {
            ledger.setMsaCycleMonths(null);
            ledger.setMsaWarningDays(null);
            ledger.setLastMsaDate(null);
            ledger.setNextMsaDate(null);
            ledger.setMsaStatus(WARNING_NOT_REQUIRED);
            ledger.setMsaResult(null);
            ledger.setMsaReport(null);
            ledger.setMsaAnalyst(null);
        }
    }

    private String normalizeCalibrationMethod(String rawCalibrationMethod) {
        String method = trimToNull(rawCalibrationMethod);
        if (method != null) {
            dictDataService.validateDictDataList(DICT_TYPE_CALIBRATION_METHOD, List.of(method));
        }
        return method;
    }

    private void addOptionValue(Set<String> values, String rawValue) {
        String value = trimToNull(rawValue);
        if (value != null) {
            values.add(value);
        }
    }

    private List<String> sortOptionValues(Set<String> values) {
        return values.stream().sorted().toList();
    }

    /** 主数据编辑不能绕过“维护校准/维护MSA”而改写当前结果。 */
    private void preserveLatestSnapshots(QmsMeasureToolLedgerDO updateObj, QmsMeasureToolLedgerDO existed) {
        updateObj.setCalibrationType(existed.getCalibrationType());
        updateObj.setCalibrationMethod(existed.getCalibrationMethod());
        updateObj.setCalibrationOrg(existed.getCalibrationOrg());
        updateObj.setCalibrator(existed.getCalibrator());
        updateObj.setCalibrationResult(existed.getCalibrationResult());
        updateObj.setCertificateNo(existed.getCertificateNo());
        updateObj.setCalibrationReport(existed.getCalibrationReport());
        updateObj.setLastCalibrationDate(existed.getLastCalibrationDate());
        updateObj.setNextCalibrationDate(existed.getNextCalibrationDate());
        updateObj.setMsaResult(existed.getMsaResult());
        updateObj.setMsaReport(existed.getMsaReport());
        updateObj.setMsaAnalyst(existed.getMsaAnalyst());
        updateObj.setLastMsaDate(existed.getLastMsaDate());
        updateObj.setNextMsaDate(existed.getNextMsaDate());
    }

    private void enrichLedgerRuntimeState(QmsMeasureToolLedgerDO ledger) {
        if (ledger == null) {
            return;
        }
        String calibrationStatus = resolveWarningStatus(ledger.getNextCalibrationDate(), ledger.getWarningDays());
        ledger.setCalibrationStatus(calibrationStatus);
        ledger.setCalibrationMissedCount(resolveMissedCount(ledger.getNextCalibrationDate(), ledger.getCalibrationCycleMonths()));
        ledger.setRecentCalibrationMissedDate(resolveRecentMissedDate(ledger.getNextCalibrationDate(),
                ledger.getCalibrationCycleMonths()));
        ledger.setMsaStatus(resolveMsaWarningStatus(ledger.getNextMsaDate(), ledger.getMsaWarningDays(), isMsaEnabled(ledger)));
        ledger.setMsaMissedCount(isMsaEnabled(ledger)
                ? resolveMissedCount(ledger.getNextMsaDate(), ledger.getMsaCycleMonths()) : 0);
        ledger.setRecentMsaMissedDate(isMsaEnabled(ledger)
                ? resolveRecentMissedDate(ledger.getNextMsaDate(), ledger.getMsaCycleMonths()) : null);
        ledger.setCalibrationOverdue(WARNING_OVERDUE.equals(calibrationStatus));
        ledger.setDisplayStatus(ledger.getStatus());
    }

    private boolean isMsaEnabled(QmsMeasureToolLedgerDO ledger) {
        return ledger != null && Integer.valueOf(1).equals(ledger.getMsaEnabled());
    }

    private void fillApplyDefaultsAndCategory(QmsMeasureToolApplyDO apply) {
        apply.setCalibrationCycleMonths(defaultInt(apply.getCalibrationCycleMonths(), DEFAULT_CYCLE_MONTHS));
        apply.setWarningDays(defaultInt(apply.getWarningDays(), DEFAULT_WARNING_DAYS));
        fillCategorySnapshot(apply.getCategoryId(), apply::setCategoryName);
    }

    private void fillRecordLedgerSnapshot(QmsMeasureToolCalibrationRecordDO record, QmsMeasureToolLedgerDO ledger) {
        record.setLedgerId(ledger.getId());
        record.setToolCode(ledger.getToolCode());
        record.setToolName(ledger.getToolName());
        record.setCategoryId(ledger.getCategoryId());
        record.setCategoryName(ledger.getCategoryName());
        record.setUsingDepartment(ledger.getUsingDepartment());
        record.setKeeperName(ledger.getKeeperName());
    }

    private void fillCategorySnapshot(Long categoryId, java.util.function.Consumer<String> categoryNameSetter) {
        if (categoryId == null) {
            categoryNameSetter.accept(null);
            return;
        }
        QmsMeasureToolCategoryDO category = validateCategoryExists(categoryId);
        categoryNameSetter.accept(category.getCategoryName());
    }

    /**
     * 位置和区域采用现有两列反范式保存：storageLocation 保存一级位置名称，
     * categoryId/categoryName 保存二级区域。后端以区域父节点为准，避免客户端伪造位置文本。
     */
    private void fillLedgerLocationAndAreaSnapshot(QmsMeasureToolLedgerDO ledger) {
        ledger.setStorageLocation(trimToNull(ledger.getStorageLocation()));
        if (ledger.getCategoryId() == null) {
            ledger.setCategoryName(null);
            return;
        }
        QmsMeasureToolCategoryDO area = validateCategoryExists(ledger.getCategoryId());
        ledger.setCategoryName(area.getCategoryName());
        if (defaultLong(area.getParentId()) == 0) {
            return; // 兼容未迁移的旧平铺类别，编辑时保留原位置文本。
        }
        QmsMeasureToolCategoryDO location = validateCategoryExists(area.getParentId());
        ledger.setStorageLocation(location.getCategoryName());
    }

    private Long resolveOrCreateCategoryId(String storageLocation, String areaName, int rowNo) {
        String normalizedAreaName = trimToNull(areaName);
        if (normalizedAreaName == null) {
            throw new IllegalArgumentException("第" + rowNo + "行区域不能为空");
        }
        String normalizedLocation = trimToNull(storageLocation);
        if (normalizedLocation == null) {
            // 兼容旧模板：未提供位置时沿用原平铺类别。
            QmsMeasureToolCategoryDO existed = categoryMapper.selectByName(normalizedAreaName);
            if (existed != null) {
                return existed.getId();
            }
            return createCategory(0L, normalizedAreaName);
        }
        QmsMeasureToolCategoryDO location = categoryMapper.selectByNameAndParentId(normalizedLocation, 0L);
        if (location == null) {
            location = new QmsMeasureToolCategoryDO();
            location.setId(createCategory(0L, normalizedLocation));
        }
        QmsMeasureToolCategoryDO area = categoryMapper.selectByNameAndParentId(normalizedAreaName, location.getId());
        return area != null ? area.getId() : createCategory(location.getId(), normalizedAreaName);
    }

    private Long createCategory(Long parentId, String categoryName) {
        QmsMeasureToolCategoryDO category = new QmsMeasureToolCategoryDO();
        category.setParentId(parentId);
        category.setCategoryCode(generateNo("MTC"));
        category.setCategoryName(categoryName);
        category.setStatus(1);
        category.setSort(DEFAULT_SORT);
        categoryMapper.insert(category);
        return category.getId();
    }

    private String normalizeToolStatus(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return STATUS_IN_USE;
        }
        return switch (value.toUpperCase()) {
            case "在用", "正在使用", "IN_USE" -> STATUS_IN_USE;
            case "闲置", "IDLE" -> "IDLE";
            case "校准中", "CALIBRATING" -> "CALIBRATING";
            case "维修", "维修中", "REPAIRING" -> "REPAIRING";
            case "停用", "STOPPED" -> STATUS_STOPPED;
            case "报废", "SCRAPPED" -> STATUS_SCRAPPED;
            // 过期是按校准/MSA日期推导的展示状态，不能作为人工台账状态写入。
            case "过期", "EXPIRED" -> STATUS_IN_USE;
            default -> throw new IllegalArgumentException("量检具状态不合法：" + rawValue);
        };
    }

    private String normalizeCalibrationType(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase()) {
            case "内校", "INTERNAL" -> "INTERNAL";
            case "外校", "EXTERNAL" -> "EXTERNAL";
            default -> throw new IllegalArgumentException("校准类型不合法：" + rawValue);
        };
    }

    private String normalizeCalibrationResult(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase()) {
            case "合格", "QUALIFIED" -> "QUALIFIED";
            case "不合格", "UNQUALIFIED" -> "UNQUALIFIED";
            case "限用", "LIMITED" -> "LIMITED";
            default -> throw new IllegalArgumentException("校准结果不合法：" + rawValue);
        };
    }

    private Integer normalizeMsaEnabled(String rawValue) {
        String value = trimToNull(rawValue);
        return value != null && Set.of("1", "是", "Y", "YES", "纳入", "TRUE").contains(value.toUpperCase()) ? 1 : 0;
    }

    private String normalizeMsaResult(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase()) {
            case "合格", "QUALIFIED" -> "QUALIFIED";
            case "不合格", "UNQUALIFIED" -> "UNQUALIFIED";
            default -> throw new IllegalArgumentException("MSA分析结果不合法：" + rawValue);
        };
    }

    private LocalDate resolveNextCalibrationDate(LocalDate baseDate, LocalDate purchaseDate,
                                                  Integer cycleMonths, LocalDate explicitNextDate) {
        if (explicitNextDate != null) {
            return explicitNextDate;
        }
        LocalDate effectiveBaseDate = baseDate != null ? baseDate : purchaseDate;
        if (effectiveBaseDate == null) {
            return null;
        }
        return effectiveBaseDate.plusMonths(defaultInt(cycleMonths, DEFAULT_CYCLE_MONTHS));
    }

    private String resolveWarningStatus(LocalDate nextCalibrationDate, Integer warningDays) {
        if (nextCalibrationDate == null) {
            return WARNING_NOT_CALIBRATED;
        }
        LocalDate today = LocalDate.now();
        if (nextCalibrationDate.isBefore(today)) {
            return WARNING_OVERDUE;
        }
        if (!nextCalibrationDate.isAfter(today.plusDays(defaultInt(warningDays, DEFAULT_WARNING_DAYS)))) {
            return WARNING_DUE_SOON;
        }
        return WARNING_NORMAL;
    }

    private String resolveMsaWarningStatus(LocalDate nextMsaDate, Integer warningDays, boolean msaEnabled) {
        return msaEnabled ? resolveWarningStatus(nextMsaDate, warningDays) : WARNING_NOT_REQUIRED;
    }

    private int resolveMissedCount(LocalDate nextDate, Integer cycleMonths) {
        LocalDate today = LocalDate.now();
        if (nextDate == null || !nextDate.isBefore(today)) {
            return 0;
        }
        int cycle = defaultInt(cycleMonths, DEFAULT_CYCLE_MONTHS);
        int crossedMonths = (today.getYear() - nextDate.getYear()) * 12
                + today.getMonthValue() - nextDate.getMonthValue();
        int missedCount = Math.max(1, crossedMonths / cycle + 1);
        while (missedCount > 1 && nextDate.plusMonths((long) (missedCount - 1) * cycle).isAfter(today)) {
            missedCount--;
        }
        return missedCount;
    }

    private LocalDate resolveRecentMissedDate(LocalDate nextDate, Integer cycleMonths) {
        int missedCount = resolveMissedCount(nextDate, cycleMonths);
        return missedCount == 0 ? null : nextDate.plusMonths((long) (missedCount - 1) * defaultInt(cycleMonths, DEFAULT_CYCLE_MONTHS));
    }

    private void archiveCalibrationSnapshotIfPresent(QmsMeasureToolLedgerDO ledger) {
        if (ledger.getLastCalibrationDate() == null || isBlank(ledger.getCalibrationResult())) {
            return;
        }
        QmsMeasureToolCalibrationRecordDO record = new QmsMeasureToolCalibrationRecordDO();
        fillRecordLedgerSnapshot(record, ledger);
        record.setRecordNo(generateNo("MTR"));
        record.setCalibrationDate(ledger.getLastCalibrationDate());
        record.setCalibrationType(ledger.getCalibrationType());
        record.setCalibrationMethod(ledger.getCalibrationMethod());
        record.setCalibrationOrg(ledger.getCalibrationOrg());
        record.setCalibrator(ledger.getCalibrator());
        record.setCalibrationResult(ledger.getCalibrationResult());
        record.setCertificateNo(ledger.getCertificateNo());
        record.setCertificateAttachment(ledger.getCalibrationReport());
        record.setValidUntil(ledger.getNextCalibrationDate());
        record.setNextCalibrationDate(ledger.getNextCalibrationDate());
        record.setSourceType(SOURCE_SNAPSHOT);
        record.setMissedCount(resolveMissedCount(ledger.getNextCalibrationDate(), ledger.getCalibrationCycleMonths()));
        record.setOverdueFlag(record.getMissedCount() > 0 ? 1 : 0);
        record.setOverdueDueDate(resolveRecentMissedDate(ledger.getNextCalibrationDate(), ledger.getCalibrationCycleMonths()));
        record.setVersion(INITIAL_VERSION);
        recordMapper.insert(record);
    }

    private void archiveMsaSnapshotIfPresent(QmsMeasureToolLedgerDO ledger) {
        if (ledger.getLastMsaDate() == null) {
            return;
        }
        QmsMeasureToolMsaRecordDO record = new QmsMeasureToolMsaRecordDO();
        record.setRecordNo(generateNo("MSAR"));
        record.setLedgerId(ledger.getId());
        record.setToolCode(ledger.getToolCode());
        record.setToolName(ledger.getToolName());
        record.setCategoryId(ledger.getCategoryId());
        record.setCategoryName(ledger.getCategoryName());
        record.setUsingDepartment(ledger.getUsingDepartment());
        record.setMaintainerName(ledger.getMaintainerName());
        record.setMsaDate(ledger.getLastMsaDate());
        record.setNextMsaDate(ledger.getNextMsaDate());
        record.setMsaResult(ledger.getMsaResult());
        record.setMsaReport(ledger.getMsaReport());
        record.setAnalyst(ledger.getMsaAnalyst());
        record.setMissedCount(resolveMissedCount(ledger.getNextMsaDate(), ledger.getMsaCycleMonths()));
        record.setOverdueFlag(record.getMissedCount() > 0 ? 1 : 0);
        record.setOverdueDueDate(resolveRecentMissedDate(ledger.getNextMsaDate(), ledger.getMsaCycleMonths()));
        record.setSourceType(SOURCE_SNAPSHOT);
        record.setVersion(INITIAL_VERSION);
        msaRecordMapper.insert(record);
    }

    private String resolveTaskStatus(LocalDate dueDate) {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) ? TASK_OVERDUE : TASK_PENDING;
    }

    private void updateLedgerWarningStatus(QmsMeasureToolLedgerDO ledger) {
        QmsMeasureToolLedgerDO updateLedger = new QmsMeasureToolLedgerDO();
        updateLedger.setId(ledger.getId());
        updateLedger.setCalibrationStatus(resolveWarningStatus(ledger.getNextCalibrationDate(), ledger.getWarningDays()));
        ledgerMapper.updateById(updateLedger);
    }

    private void applyRecordToLedger(QmsMeasureToolCalibrationRecordDO record, QmsMeasureToolLedgerDO ledger) {
        QmsMeasureToolLedgerDO updateLedger = new QmsMeasureToolLedgerDO();
        updateLedger.setId(ledger.getId());
        updateLedger.setLastCalibrationDate(record.getCalibrationDate());
        updateLedger.setNextCalibrationDate(record.getNextCalibrationDate());
        updateLedger.setCalibrationType(record.getCalibrationType());
        updateLedger.setCalibrationMethod(record.getCalibrationMethod());
        updateLedger.setCalibrationOrg(record.getCalibrationOrg());
        updateLedger.setCalibrator(record.getCalibrator());
        updateLedger.setCalibrationResult(record.getCalibrationResult());
        updateLedger.setCertificateNo(record.getCertificateNo());
        updateLedger.setCalibrationReport(record.getCertificateAttachment());
        updateLedger.setCalibrationStatus(resolveWarningStatus(record.getNextCalibrationDate(), ledger.getWarningDays()));
        if (!STATUS_SCRAPPED.equals(ledger.getStatus())) {
            updateLedger.setStatus(RESULT_UNQUALIFIED.equals(record.getCalibrationResult()) ? STATUS_STOPPED : STATUS_IN_USE);
        }
        if (ledgerMapper.updateById(updateLedger) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    private void createCompletedManualTaskByRecord(QmsMeasureToolCalibrationRecordDO record,
                                                   QmsMeasureToolLedgerDO ledger) {
        QmsMeasureToolCalibrationTaskDO task = new QmsMeasureToolCalibrationTaskDO();
        task.setTaskNo(generateNo("MTC"));
        fillTaskLedgerSnapshot(task, ledger);
        task.setDueDate(record.getCalibrationDate());
        task.setWarningDays(defaultInt(ledger.getWarningDays(), DEFAULT_WARNING_DAYS));
        task.setWarningStatus(resolveWarningStatus(record.getNextCalibrationDate(), task.getWarningDays()));
        task.setTaskStatus(TASK_COMPLETED);
        task.setSourceType(TASK_SOURCE_MANUAL);
        task.setGeneratedTime(LocalDateTime.now());
        task.setCompletedTime(LocalDateTime.now());
        task.setRecordId(record.getId());
        task.setHandlerName(trimToNull(record.getCalibrator()));
        task.setRemark("人工新增检验记录");
        task.setVersion(INITIAL_VERSION);
        taskMapper.insert(task);
        if (recordMapper.update(null, new LambdaUpdateWrapper<QmsMeasureToolCalibrationRecordDO>()
                .eq(QmsMeasureToolCalibrationRecordDO::getId, record.getId())
                .set(QmsMeasureToolCalibrationRecordDO::getTaskId, task.getId())) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
        record.setTaskId(task.getId());
    }

    private void updateCompletedManualTaskByRecord(QmsMeasureToolCalibrationTaskDO task,
                                                   QmsMeasureToolCalibrationRecordDO record,
                                                   QmsMeasureToolLedgerDO ledger) {
        QmsMeasureToolCalibrationTaskDO updateTask = new QmsMeasureToolCalibrationTaskDO();
        updateTask.setId(task.getId());
        fillTaskLedgerSnapshot(updateTask, ledger);
        updateTask.setDueDate(record.getCalibrationDate());
        updateTask.setWarningDays(defaultInt(ledger.getWarningDays(), DEFAULT_WARNING_DAYS));
        updateTask.setWarningStatus(resolveWarningStatus(record.getNextCalibrationDate(), updateTask.getWarningDays()));
        updateTask.setTaskStatus(TASK_COMPLETED);
        updateTask.setCompletedTime(task.getCompletedTime());
        updateTask.setRecordId(record.getId());
        updateTask.setHandlerName(trimToNull(record.getCalibrator()));
        if (taskMapper.updateById(updateTask) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    private void updateCompletedTaskByRecord(QmsMeasureToolCalibrationTaskDO task,
                                             QmsMeasureToolCalibrationRecordDO record) {
        QmsMeasureToolCalibrationTaskDO updateTask = new QmsMeasureToolCalibrationTaskDO();
        updateTask.setId(task.getId());
        updateTask.setWarningStatus(resolveWarningStatus(record.getNextCalibrationDate(), task.getWarningDays()));
        updateTask.setRecordId(record.getId());
        updateTask.setHandlerName(trimToNull(record.getCalibrator()));
        if (taskMapper.updateById(updateTask) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    private void fillTaskLedgerSnapshot(QmsMeasureToolCalibrationTaskDO task, QmsMeasureToolLedgerDO ledger) {
        task.setLedgerId(ledger.getId());
        task.setToolCode(ledger.getToolCode());
        task.setToolName(ledger.getToolName());
        task.setCategoryId(ledger.getCategoryId());
        task.setCategoryName(ledger.getCategoryName());
        task.setUsingDepartment(ledger.getUsingDepartment());
        task.setKeeperName(ledger.getKeeperName());
    }

    private void submitTaskRecordForConfirm(QmsMeasureToolCalibrationTaskDO task,
                                            QmsMeasureToolCalibrationRecordDO record) {
        QmsMeasureToolCalibrationTaskDO updateTask = new QmsMeasureToolCalibrationTaskDO();
        updateTask.setId(task.getId());
        updateTask.setTaskStatus(TASK_IN_PROGRESS);
        updateTask.setRecordId(record.getId());
        updateTask.setHandlerName(trimToNull(record.getCalibrator()));
        updateTask.setCompletedTime(null);
        if (taskMapper.updateById(updateTask) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    private void updateTaskRecordForConfirm(QmsMeasureToolCalibrationTaskDO task,
                                            QmsMeasureToolCalibrationRecordDO record) {
        QmsMeasureToolCalibrationTaskDO updateTask = new QmsMeasureToolCalibrationTaskDO();
        updateTask.setId(task.getId());
        updateTask.setRecordId(record.getId());
        updateTask.setHandlerName(trimToNull(record.getCalibrator()));
        updateTask.setCompletedTime(null);
        if (taskMapper.updateById(updateTask) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    private void completeTaskByRecord(QmsMeasureToolCalibrationTaskDO task, QmsMeasureToolCalibrationRecordDO record) {
        QmsMeasureToolCalibrationTaskDO updateTask = new QmsMeasureToolCalibrationTaskDO();
        updateTask.setId(task.getId());
        updateTask.setTaskStatus(TASK_COMPLETED);
        updateTask.setWarningStatus(resolveWarningStatus(record.getNextCalibrationDate(), task.getWarningDays()));
        updateTask.setCompletedTime(LocalDateTime.now());
        updateTask.setRecordId(record.getId());
        updateTask.setHandlerName(trimToNull(record.getCalibrator()));
        if (taskMapper.updateById(updateTask) == 0) {
            throw exception(QMS_MEASURE_TOOL_VERSION_CONFLICT);
        }
    }

    private void recalculateLedgerFromLatestRecord(Long ledgerId) {
        QmsMeasureToolLedgerDO ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null) {
            return;
        }
        QmsMeasureToolCalibrationRecordDO latest = recordMapper.selectLatestByLedgerId(ledgerId);
        if (latest == null) {
            LocalDate nextCalibrationDate = resolveNextCalibrationDate(null, ledger.getPurchaseDate(),
                    ledger.getCalibrationCycleMonths(), null);
            ledgerMapper.update(null, new LambdaUpdateWrapper<QmsMeasureToolLedgerDO>()
                    .eq(QmsMeasureToolLedgerDO::getId, ledgerId)
                    .set(QmsMeasureToolLedgerDO::getLastCalibrationDate, null)
                    .set(QmsMeasureToolLedgerDO::getNextCalibrationDate, nextCalibrationDate)
                    .set(QmsMeasureToolLedgerDO::getCalibrationStatus,
                            resolveWarningStatus(nextCalibrationDate, ledger.getWarningDays())));
            return;
        }
        applyRecordToLedger(latest, ledger);
    }

    private void refreshOverdueTasks() {
        QmsMeasureToolCalibrationTaskPageReqVO reqVO = new QmsMeasureToolCalibrationTaskPageReqVO();
        reqVO.setTaskStatus(TASK_PENDING);
        reqVO.setPageNo(1);
        reqVO.setPageSize(100);
        PageResult<QmsMeasureToolCalibrationTaskDO> pending = taskMapper.selectPage(reqVO);
        pending.getList().stream()
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now()))
                .forEach(task -> {
                    QmsMeasureToolCalibrationTaskDO updateObj = new QmsMeasureToolCalibrationTaskDO();
                    updateObj.setId(task.getId());
                    updateObj.setTaskStatus(TASK_OVERDUE);
                    updateObj.setWarningStatus(WARNING_OVERDUE);
                    taskMapper.updateById(updateObj);
                });
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_DATE_FORMATTER)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private static int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private record SummaryKey(String month, Long categoryId, String categoryName, String usingDepartment) {
        static SummaryKey of(LocalDate date, Long categoryId, String categoryName, String usingDepartment) {
            String month = date == null ? "" : YearMonth.from(date).toString();
            return new SummaryKey(month, categoryId, categoryName, usingDepartment);
        }
    }

    private static final class SummaryAccumulator {
        private final SummaryKey key;
        private long taskCount;
        private long recordCount;
        private long qualifiedCount;
        private long unqualifiedCount;
        private long limitedCount;
        private long overdueCompletedCount;

        private SummaryAccumulator(SummaryKey key) {
            this.key = key;
        }

        private QmsMeasureToolCalibrationMonthlySummaryRespVO toResp() {
            BigDecimal completionRate = taskCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(recordCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(taskCount), 2, RoundingMode.HALF_UP);
            return new QmsMeasureToolCalibrationMonthlySummaryRespVO(key.month(), key.categoryId(), key.categoryName(),
                    key.usingDepartment(), taskCount, recordCount, qualifiedCount, unqualifiedCount, limitedCount,
                    overdueCompletedCount, completionRate);
        }
    }

}
