package cn.iocoder.yudao.module.mes.service.resource.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategoryListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionProcessReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceExceptionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCandidateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintCurrentMonthAppendReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintPlanWeekSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo.ResourceDeviceMaintStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceCategoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceExceptionPartDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintOrderPartDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceMaintStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDeviceParamDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.resource.device.ResourceDevicePartDO;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceCategoryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceExceptionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceExceptionPartMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintOrderItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintOrderPartMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceMaintStandardMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDeviceParamMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.resource.device.ResourceDevicePartMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_CATEGORY_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_EXCEPTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_LEDGER_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_LEDGER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_MAINT_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_MAINT_ORDER_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_MAINT_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_MAINT_RECORD_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_MAINT_STANDARD_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_MAINT_STANDARD_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.RESOURCE_DEVICE_VERSION_CONFLICT;

@Service
@Validated
public class ResourceDeviceServiceImpl implements ResourceDeviceService {

    private static final int INITIAL_VERSION = 0;
    private static final int DEFAULT_SORT = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int LEDGER_STATUS_NORMAL = 1;
    private static final String MAINT_STATUS_NORMAL = "NORMAL";
    private static final String ORDER_WAIT_EXECUTE = "WAIT_EXECUTE";
    private static final String ORDER_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String ORDER_DONE = "DONE";
    private static final String ORDER_ABNORMAL = "ABNORMAL";
    private static final String ORDER_CANCELLED = "CANCELLED";
    private static final String ORDER_OVERDUE = "OVERDUE";
    private static final String PLAN_EXEC_PENDING = "PENDING";
    private static final String ITEM_NORMAL = "NORMAL";
    private static final String ITEM_PENDING = "PENDING";
    private static final String ITEM_ABNORMAL = "ABNORMAL";
    private static final String PLAN_STATUS_DRAFT = "DRAFT";
    private static final String PLAN_STATUS_PUBLISHED = "PUBLISHED";
    private static final String EXCEPTION_REPORTED = "REPORTED";
    private static final String EXCEPTION_DISPATCHED = "DISPATCHED";
    private static final String EXCEPTION_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String EXCEPTION_PENDING_ARCHIVE = "PENDING_ARCHIVE";
    private static final String EXCEPTION_CLOSED = "CLOSED";
    private static final String EXCEPTION_ACTION_RESPOND = "RESPOND";
    private static final String EXCEPTION_ACTION_REPAIR = "REPAIR";
    private static final String EXCEPTION_ACTION_CONFIRM = "CONFIRM";
    private static final String EXCEPTION_ACTION_ARCHIVE = "ARCHIVE";
    private static final String EXCEPTION_RESPONSE_HANDLED = "HANDLED";
    private static final String EXCEPTION_RESPONSE_NEED_REPAIR = "NEED_REPAIR";
    private static final String EXCEPTION_REPAIR_DONE = "DONE";
    private static final LocalDate MIN_VALID_BUSINESS_DATE = LocalDate.of(2000, 1, 1);
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private ResourceDeviceCategoryMapper categoryMapper;
    @Resource
    private ResourceDeviceLedgerMapper ledgerMapper;
    @Resource
    private ResourceDevicePartMapper partMapper;
    @Resource
    private ResourceDeviceParamMapper paramMapper;
    @Resource
    private ResourceDeviceMaintStandardMapper standardMapper;
    @Resource
    private ResourceDeviceMaintStandardItemMapper standardItemMapper;
    @Resource
    private ResourceDeviceMaintPlanMapper maintPlanMapper;
    @Resource
    private ResourceDeviceMaintOrderMapper orderMapper;
    @Resource
    private ResourceDeviceMaintOrderItemMapper orderItemMapper;
    @Resource
    private ResourceDeviceMaintOrderPartMapper orderPartMapper;
    @Resource
    private ResourceDeviceMaintRecordMapper recordMapper;
    @Resource
    private ResourceDeviceExceptionMapper exceptionMapper;
    @Resource
    private ResourceDeviceExceptionPartMapper exceptionPartMapper;

    @Override
    public Long createCategory(ResourceDeviceCategorySaveReqVO reqVO) {
        validateCategoryCodeUnique(null, reqVO.getCategoryCode());
        ResourceDeviceCategoryDO category = BeanUtils.toBean(reqVO, ResourceDeviceCategoryDO.class);
        category.setParentId(defaultLong(reqVO.getParentId()));
        category.setStatus(defaultInt(reqVO.getStatus(), STATUS_ENABLED));
        category.setSort(defaultInt(reqVO.getSort(), DEFAULT_SORT));
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(ResourceDeviceCategorySaveReqVO reqVO) {
        validateCategoryExists(reqVO.getId());
        validateCategoryCodeUnique(reqVO.getId(), reqVO.getCategoryCode());
        ResourceDeviceCategoryDO updateObj = BeanUtils.toBean(reqVO, ResourceDeviceCategoryDO.class);
        updateObj.setParentId(defaultLong(reqVO.getParentId()));
        updateObj.setStatus(defaultInt(reqVO.getStatus(), STATUS_ENABLED));
        updateObj.setSort(defaultInt(reqVO.getSort(), DEFAULT_SORT));
        if (categoryMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
    }

    @Override
    public void deleteCategory(Long id) {
        validateCategoryExists(id);
        categoryMapper.deleteById(id);
    }

    @Override
    public ResourceDeviceCategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<ResourceDeviceCategoryDO> getCategoryList(ResourceDeviceCategoryListReqVO reqVO) {
        return categoryMapper.selectList(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLedger(ResourceDeviceLedgerSaveReqVO reqVO) {
        validateDeviceCodeUnique(null, reqVO.getDeviceCode());
        ResourceDeviceLedgerDO ledger = BeanUtils.toBean(reqVO, ResourceDeviceLedgerDO.class);
        fillLedgerDefaults(ledger);
        ledger.setVersion(INITIAL_VERSION);
        ledgerMapper.insert(ledger);
        replaceDeviceParts(ledger.getId(), reqVO.getParts());
        replaceDeviceParams(ledger.getId(), reqVO.getParams());
        return ledger.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLedger(ResourceDeviceLedgerSaveReqVO reqVO) {
        validateLedgerExists(reqVO.getId());
        validateDeviceCodeUnique(reqVO.getId(), reqVO.getDeviceCode());
        ResourceDeviceLedgerDO updateObj = BeanUtils.toBean(reqVO, ResourceDeviceLedgerDO.class);
        fillLedgerDefaults(updateObj);
        if (ledgerMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        if (reqVO.getParts() != null) {
            replaceDeviceParts(reqVO.getId(), reqVO.getParts());
        }
        if (reqVO.getParams() != null) {
            replaceDeviceParams(reqVO.getId(), reqVO.getParams());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLedger(Long id) {
        validateLedgerExists(id);
        partMapper.deleteByDeviceId(id);
        paramMapper.deleteByDeviceId(id);
        ledgerMapper.deleteById(id);
    }

    @Override
    public ResourceDeviceLedgerDO getLedger(Long id) {
        return ledgerMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceDeviceLedgerDO> getLedgerPage(ResourceDeviceLedgerPageReqVO reqVO) {
        if (reqVO.getCategoryId() != null) {
            reqVO.setCategoryIds(resolveCategoryTreeIds(reqVO.getCategoryId()));
        }
        return ledgerMapper.selectPage(reqVO);
    }

    @Override
    public List<ResourceDevicePartDO> getPartList(Long deviceId) {
        return partMapper.selectByDeviceId(deviceId);
    }

    @Override
    public List<ResourceDeviceParamDO> getParamList(Long deviceId) {
        return paramMapper.selectByDeviceId(deviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStandard(ResourceDeviceMaintStandardSaveReqVO reqVO) {
        validateStandardCodeUnique(null, reqVO.getCode());
        ResourceDeviceMaintStandardDO standard = BeanUtils.toBean(reqVO, ResourceDeviceMaintStandardDO.class);
        fillStandardDefaults(standard);
        standard.setVersion(INITIAL_VERSION);
        standardMapper.insert(standard);
        replaceStandardItems(standard.getId(), reqVO.getItems());
        return standard.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStandard(ResourceDeviceMaintStandardSaveReqVO reqVO) {
        validateStandardExists(reqVO.getId());
        validateStandardCodeUnique(reqVO.getId(), reqVO.getCode());
        ResourceDeviceMaintStandardDO updateObj = BeanUtils.toBean(reqVO, ResourceDeviceMaintStandardDO.class);
        fillStandardDefaults(updateObj);
        if (standardMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        if (reqVO.getItems() != null) {
            replaceStandardItems(reqVO.getId(), reqVO.getItems());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStandard(Long id) {
        validateStandardExists(id);
        standardItemMapper.deleteByStandardId(id);
        standardMapper.deleteById(id);
    }

    @Override
    public ResourceDeviceMaintStandardDO getStandard(Long id) {
        return standardMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceDeviceMaintStandardDO> getStandardPage(ResourceDeviceMaintStandardPageReqVO reqVO) {
        return standardMapper.selectPage(reqVO);
    }

    @Override
    public List<ResourceDeviceMaintStandardItemDO> getStandardItemList(Long standardId) {
        return standardItemMapper.selectByStandardId(standardId);
    }

    @Override
    public Long createMaintPlan(ResourceDeviceMaintPlanSaveReqVO reqVO) {
        ResourceDeviceMaintPlanDO plan = BeanUtils.toBean(reqVO, ResourceDeviceMaintPlanDO.class);
        fillMaintPlanDefaults(plan);
        plan.setVersion(INITIAL_VERSION);
        maintPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    public void updateMaintPlan(ResourceDeviceMaintPlanSaveReqVO reqVO) {
        ResourceDeviceMaintPlanDO existed = validateMaintPlanExists(reqVO.getId());
        ResourceDeviceMaintPlanDO updateObj = BeanUtils.toBean(reqVO, ResourceDeviceMaintPlanDO.class);
        keepMaintPlanExecution(updateObj, existed);
        fillMaintPlanDefaults(updateObj);
        if (maintPlanMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
    }

    @Override
    public void deleteMaintPlan(Long id) {
        validateMaintPlanExists(id);
        maintPlanMapper.deleteById(id);
    }

    @Override
    public ResourceDeviceMaintPlanDO getMaintPlan(Long id) {
        return maintPlanMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceDeviceMaintPlanDO> getMaintPlanPage(ResourceDeviceMaintPlanPageReqVO reqVO) {
        return maintPlanMapper.selectPage(reqVO);
    }

    @Override
    public ResourceDeviceMaintPlanRespVO.Matrix getMaintPlanMatrix(Integer year, Long deviceId) {
        List<ResourceDeviceMaintPlanDO> rows = maintPlanMapper.selectByYear(year, deviceId);
        Set<Long> orderIds = new HashSet<>();
        Set<Long> standardItemIds = new HashSet<>();
        rows.forEach(plan -> {
            if (plan.getGeneratedOrderId() != null) {
                orderIds.add(plan.getGeneratedOrderId());
            }
            if (plan.getStandardItemId() != null) {
                standardItemIds.add(plan.getStandardItemId());
            }
        });
        Map<Long, ResourceDeviceMaintOrderDO> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMapper.selectBatchIds(orderIds).forEach(order -> orderMap.put(order.getId(), order));
        }
        Map<Long, ResourceDeviceMaintStandardItemDO> standardItemMap = new HashMap<>();
        if (!standardItemIds.isEmpty()) {
            standardItemMapper.selectBatchIds(standardItemIds).forEach(item -> standardItemMap.put(item.getId(), item));
        }

        Map<String, ResourceDeviceMaintPlanRespVO.MatrixRow> rowMap = new LinkedHashMap<>();
        Map<String, Map<String, ResourceDeviceMaintPlanRespVO.MatrixItem>> itemMap = new LinkedHashMap<>();
        Map<String, LocalDateTime> lastActualMap = new HashMap<>();
        for (ResourceDeviceMaintPlanDO plan : rows) {
            String key = matrixRowKey(plan);
            ResourceDeviceMaintPlanRespVO.MatrixRow row = rowMap.computeIfAbsent(key, ignored -> {
                ResourceDeviceMaintPlanRespVO.MatrixRow matrixRow = new ResourceDeviceMaintPlanRespVO.MatrixRow();
                matrixRow.setId(plan.getStandardId() == null ? plan.getDeviceId() : plan.getStandardId());
                matrixRow.setPlanYear(year);
                matrixRow.setDeviceId(plan.getDeviceId());
                matrixRow.setDeviceCode(plan.getDeviceCode());
                matrixRow.setDeviceName(plan.getDeviceName());
                matrixRow.setCategoryName(plan.getCategoryName());
                matrixRow.setStandardId(plan.getStandardId());
                matrixRow.setStandardCode(plan.getStandardCode());
                matrixRow.setStandardName(plan.getStandardName());
                matrixRow.setLastMaintDate("");
                matrixRow.setItems(new ArrayList<>());
                return matrixRow;
            });
            Map<String, ResourceDeviceMaintPlanRespVO.MatrixItem> groupItemMap =
                    itemMap.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
            String itemKey = matrixItemKey(plan);
            ResourceDeviceMaintStandardItemDO standardItem = plan.getStandardItemId() == null
                    ? null : standardItemMap.get(plan.getStandardItemId());
            ResourceDeviceMaintPlanRespVO.MatrixItem item = groupItemMap.computeIfAbsent(itemKey, ignored -> {
                ResourceDeviceMaintPlanRespVO.MatrixItem matrixItem = new ResourceDeviceMaintPlanRespVO.MatrixItem();
                matrixItem.setId(plan.getStandardItemId() == null ? plan.getId() : plan.getStandardItemId());
                matrixItem.setStandardItemId(plan.getStandardItemId());
                matrixItem.setItemGroup(firstNotBlank(plan.getItemGroup(), standardItem == null ? null : standardItem.getItemGroup()));
                matrixItem.setItemName(plan.getItemName());
                matrixItem.setMethod(plan.getMethod());
                matrixItem.setRequirement(plan.getRequirement());
                matrixItem.setFrequency(firstNotBlank(plan.getFrequency(), standardItem == null ? null : standardItem.getFrequency()));
                matrixItem.setMaintType(plan.getMaintType());
                matrixItem.setSort(standardItem == null ? DEFAULT_SORT : defaultInt(standardItem.getSort(), DEFAULT_SORT));
                matrixItem.setMonths(new LinkedHashMap<>());
                row.getItems().add(matrixItem);
                return matrixItem;
            });
            ResourceDeviceMaintOrderDO order = plan.getGeneratedOrderId() == null ? null : orderMap.get(plan.getGeneratedOrderId());
            item.getMonths().put(plan.getMonthNo(), buildMatrixCell(plan, order));
            LocalDateTime actualDate = plan.getActualDate() == null && order != null
                    ? order.getActualDate() : plan.getActualDate();
            if (actualDate != null) {
                LocalDateTime latest = lastActualMap.get(key);
                if (latest == null || actualDate.isAfter(latest)) {
                    lastActualMap.put(key, actualDate);
                }
            }
        }
        for (Map.Entry<String, ResourceDeviceMaintPlanRespVO.MatrixRow> entry : rowMap.entrySet()) {
            LocalDateTime lastActual = lastActualMap.get(entry.getKey());
            if (lastActual != null) {
                entry.getValue().setLastMaintDate(lastActual.toLocalDate().toString());
            }
            entry.getValue().getItems().sort(Comparator
                    .comparing((ResourceDeviceMaintPlanRespVO.MatrixItem item) -> defaultInt(item.getSort(), DEFAULT_SORT))
                    .thenComparing(item -> defaultString(item.getItemGroup(), ""))
                    .thenComparing(item -> defaultString(item.getItemName(), "")));
        }
        ResourceDeviceMaintPlanRespVO.Matrix matrix = new ResourceDeviceMaintPlanRespVO.Matrix();
        matrix.setList(new ArrayList<>(rowMap.values()));
        matrix.setTotal((long) rowMap.size());
        return matrix;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMaintPlanWeek(ResourceDeviceMaintPlanWeekSaveReqVO reqVO) {
        if (reqVO.getStandardItemId() == null) {
            throw invalidParamException("保养标准明细不能为空");
        }
        ResourceDeviceMaintPlanDO existed = maintPlanMapper.selectByItemMonth(
                reqVO.getPlanYear(), reqVO.getStandardItemId(), reqVO.getMonthNo());
        ResourceDeviceMaintPlanDO plan = BeanUtils.toBean(reqVO, ResourceDeviceMaintPlanDO.class);
        if (existed == null) {
            plan.setPublished(false);
            plan.setStatus(PLAN_STATUS_DRAFT);
            plan.setSourceRule(defaultString(plan.getSourceRule(), "年度计划手工维护"));
            plan.setRemark(defaultString(plan.getRemark(), "年度计划手工维护"));
            fillMaintPlanDefaults(plan);
            plan.setVersion(INITIAL_VERSION);
            maintPlanMapper.insert(plan);
            return plan.getId();
        }

        plan.setId(existed.getId());
        plan.setPublished(Boolean.TRUE.equals(existed.getPublished()));
        plan.setGeneratedOrderId(existed.getGeneratedOrderId());
        plan.setGeneratedTaskNo(existed.getGeneratedTaskNo());
        plan.setStatus(defaultString(existed.getStatus(), plan.getPublished() ? PLAN_STATUS_PUBLISHED : PLAN_STATUS_DRAFT));
        plan.setSourceRule(firstNotBlank(plan.getSourceRule(), existed.getSourceRule()));
        plan.setRemark(firstNotBlank(plan.getRemark(), existed.getRemark()));
        keepMaintPlanExecution(plan, existed);
        fillMaintPlanDefaults(plan);
        if (maintPlanMapper.updateById(plan) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        syncGeneratedOrderPlan(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyMaintPlanYear(ResourceDeviceMaintPlanGenerateReqVO reqVO) {
        Integer targetYear = reqVO.getTargetYear();
        Integer sourceYear = reqVO.getSourceYear() == null ? targetYear - 1 : reqVO.getSourceYear();
        List<ResourceDeviceMaintPlanDO> sourceRows = maintPlanMapper.selectByYear(sourceYear);
        if (sourceRows.isEmpty()) {
            throw invalidParamException("来源年份 {} 没有可复制的设备保养计划", sourceYear);
        }
        List<ResourceDeviceMaintPlanDO> targetRows = maintPlanMapper.selectByYear(targetYear);
        if (!targetRows.isEmpty() && !Boolean.TRUE.equals(reqVO.getOverwrite())) {
            throw invalidParamException("目标年份 {} 已存在设备保养计划，如需重建请勾选覆盖", targetYear);
        }
        if (!targetRows.isEmpty()) {
            maintPlanMapper.deleteByYear(targetYear);
        }
        int created = 0;
        for (ResourceDeviceMaintPlanDO source : sourceRows) {
            ResourceDeviceMaintPlanDO copy = BeanUtils.toBean(source, ResourceDeviceMaintPlanDO.class);
            copy.setId(null);
            copy.setPlanYear(targetYear);
            copy.setPublished(false);
            copy.setGeneratedOrderId(null);
            copy.setGeneratedTaskNo(null);
            copy.setExecuteStatus(PLAN_EXEC_PENDING);
            copy.setActualDate(null);
            copy.setExecutor(null);
            copy.setExecuteRemark(null);
            copy.setConfirmer(null);
            copy.setConfirmTime(null);
            copy.setStatus(PLAN_STATUS_DRAFT);
            copy.setVersion(INITIAL_VERSION);
            copy.setRemark(firstNotBlank(copy.getRemark(), "由 " + sourceYear + " 年计划复制"));
            fillMaintPlanDefaults(copy);
            maintPlanMapper.insert(copy);
            created++;
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateMaintOrdersFromPlan(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<ResourceDeviceMaintPlanDO> plans = maintPlanMapper.selectUnpublished(ids);
        int created = 0;
        for (ResourceDeviceMaintPlanDO plan : plans) {
            ResourceDeviceMaintOrderDO order = buildOrderFromPlan(plan);
            orderMapper.insert(order);
            ResourceDeviceMaintOrderItemDO item = ResourceDeviceMaintOrderItemDO.builder()
                    .taskId(order.getId())
                    .standardItemId(plan.getStandardItemId())
                    .itemName(displayMaintItemName(plan.getItemGroup(), plan.getItemName()))
                    .method(plan.getMethod())
                    .requirement(plan.getRequirement())
                    .result(ITEM_PENDING)
                    .sort(DEFAULT_SORT)
                    .build();
            orderItemMapper.insert(item);

            ResourceDeviceMaintPlanDO updateObj = new ResourceDeviceMaintPlanDO();
            updateObj.setId(plan.getId());
            updateObj.setPublished(true);
            updateObj.setGeneratedOrderId(order.getId());
            updateObj.setGeneratedTaskNo(order.getTaskNo());
            updateObj.setStatus(PLAN_STATUS_PUBLISHED);
            if (maintPlanMapper.updateById(updateObj) == 0) {
                throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
            }
            created++;
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceDeviceImportRespVO importMaintPlanExcel(List<ResourceDeviceMaintPlanImportExcelVO> rows,
                                                           Integer year,
                                                           Boolean overwrite) {
        ResourceDeviceImportRespVO respVO = new ResourceDeviceImportRespVO();
        if (rows == null || rows.isEmpty()) {
            respVO.addFailure("导入文件没有有效数据行");
            return respVO;
        }
        Integer targetYear = year == null ? rows.stream()
                .map(ResourceDeviceMaintPlanImportExcelVO::getPlanYear)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null) : year;
        if (targetYear == null) {
            respVO.addFailure("缺少计划年份");
            return respVO;
        }
        if (Boolean.TRUE.equals(overwrite)) {
            maintPlanMapper.deleteByYear(targetYear);
        }
        int rowNo = 1;
        for (ResourceDeviceMaintPlanImportExcelVO row : rows) {
            rowNo++;
            ResourceDeviceMaintPlanDO plan = BeanUtils.toBean(row, ResourceDeviceMaintPlanDO.class);
            normalizePlanItemGroup(plan);
            if (isBlank(plan.getDeviceCode()) && isBlank(plan.getDeviceName()) && isBlank(plan.getItemName())) {
                continue;
            }
            if (isBlank(plan.getDeviceCode()) || isBlank(plan.getItemName())
                    || plan.getMonthNo() == null || plan.getWeekNo() == null) {
                respVO.addFailure("第 " + rowNo + " 行缺少设备编号、保养项目、月份或周次");
                continue;
            }
            plan.setPlanYear(row.getPlanYear() == null ? targetYear : row.getPlanYear());
            fillDeviceSnapshotByCode(plan);
            fillMaintPlanDefaults(plan);
            plan.setVersion(INITIAL_VERSION);
            maintPlanMapper.insert(plan);
            respVO.addSuccess();
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executeMaintPlan(ResourceDeviceMaintPlanExecuteReqVO reqVO) {
        ResourceDeviceMaintPlanDO plan = validateMaintPlanExists(reqVO.getId());
        String executeStatus = resolvePlanExecuteStatus(plan, null);
        if (Set.of(ORDER_DONE, ORDER_ABNORMAL).contains(executeStatus)) {
            throw exception(RESOURCE_DEVICE_MAINT_ORDER_STATUS_INVALID);
        }
        ResourceDeviceMaintPlanDO updateObj = new ResourceDeviceMaintPlanDO();
        updateObj.setId(plan.getId());
        updateObj.setPublished(true);
        updateObj.setStatus(PLAN_STATUS_PUBLISHED);
        updateObj.setExecuteStatus(ORDER_WAIT_CONFIRM);
        updateObj.setExecutor(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setActualDate(LocalDateTime.now());
        updateObj.setExecuteRemark(trimToNull(reqVO.getExecuteRemark()));
        if (maintPlanMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmMaintPlans(ResourceDeviceMaintPlanConfirmReqVO reqVO) {
        int confirmed = 0;
        for (Long id : reqVO.getIds()) {
            ResourceDeviceMaintPlanDO plan = validateMaintPlanExists(id);
            if (!ORDER_WAIT_CONFIRM.equals(resolvePlanExecuteStatus(plan, null))) {
                continue;
            }
            ResourceDeviceMaintPlanDO updateObj = new ResourceDeviceMaintPlanDO();
            updateObj.setId(plan.getId());
            updateObj.setPublished(true);
            updateObj.setStatus(PLAN_STATUS_PUBLISHED);
            updateObj.setExecuteStatus(ORDER_DONE);
            updateObj.setConfirmer(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
            updateObj.setConfirmTime(LocalDateTime.now());
            if (plan.getActualDate() == null) {
                updateObj.setActualDate(LocalDateTime.now());
            }
            if (maintPlanMapper.updateById(updateObj) == 0) {
                throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
            }
            confirmed++;
        }
        return confirmed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMaintOrder(ResourceDeviceMaintOrderSaveReqVO reqVO) {
        ResourceDeviceMaintOrderDO order = BeanUtils.toBean(reqVO, ResourceDeviceMaintOrderDO.class);
        fillOrderDefaults(order);
        if (isBlank(order.getTaskNo())) {
            order.setTaskNo(generateNo("PMT"));
        }
        order.setVersion(INITIAL_VERSION);
        orderMapper.insert(order);
        if (reqVO.getItems() != null) {
            replaceOrderItems(order.getId(), reqVO.getItems());
        } else if (order.getStandardId() != null) {
            copyStandardItemsToOrder(order.getId(), order.getStandardId());
        }
        replaceOrderParts(order.getId(), reqVO.getParts());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaintOrder(ResourceDeviceMaintOrderSaveReqVO reqVO) {
        ResourceDeviceMaintOrderDO existed = validateMaintOrderExists(reqVO.getId());
        if (Set.of(ORDER_DONE, ORDER_ABNORMAL, ORDER_CANCELLED).contains(existed.getStatus())) {
            throw exception(RESOURCE_DEVICE_MAINT_ORDER_STATUS_INVALID);
        }
        ResourceDeviceMaintOrderDO updateObj = BeanUtils.toBean(reqVO, ResourceDeviceMaintOrderDO.class);
        fillOrderDefaults(updateObj);
        if (isBlank(updateObj.getTaskNo())) {
            updateObj.setTaskNo(existed.getTaskNo());
        }
        if (orderMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        if (reqVO.getItems() != null) {
            replaceOrderItems(reqVO.getId(), reqVO.getItems());
        }
        if (reqVO.getParts() != null) {
            replaceOrderParts(reqVO.getId(), reqVO.getParts());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaintOrder(Long id) {
        validateMaintOrderExists(id);
        orderItemMapper.deleteByTaskId(id);
        orderPartMapper.deleteByTaskId(id);
        orderMapper.deleteById(id);
    }

    @Override
    public ResourceDeviceMaintOrderDO getMaintOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceDeviceMaintOrderDO> getMaintOrderPage(ResourceDeviceMaintOrderPageReqVO reqVO) {
        refreshOverdueOrders();
        return orderMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executeMaintOrder(ResourceDeviceMaintOrderExecuteReqVO reqVO) {
        ResourceDeviceMaintOrderDO order = validateMaintOrderExists(reqVO.getId());
        if (Set.of(ORDER_DONE, ORDER_ABNORMAL, ORDER_CANCELLED).contains(order.getStatus())) {
            throw exception(RESOURCE_DEVICE_MAINT_ORDER_STATUS_INVALID);
        }
        if (reqVO.getItems() != null) {
            replaceOrderItems(reqVO.getId(), reqVO.getItems());
        }
        if (reqVO.getParts() != null) {
            replaceOrderParts(reqVO.getId(), reqVO.getParts());
        }

        ResourceDeviceMaintOrderDO updateObj = new ResourceDeviceMaintOrderDO();
        updateObj.setId(order.getId());
        updateObj.setExecutor(firstNotBlank(trimToNull(reqVO.getExecutor()),
                firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(),
                        firstNotBlank(order.getExecutor(), "系统"))));
        updateObj.setActualDate(reqVO.getActualDate() == null ? LocalDateTime.now() : reqVO.getActualDate());
        updateObj.setExecuteRemark(trimToNull(reqVO.getExecuteRemark()));
        updateObj.setPhotos(trimToNull(reqVO.getPhotos()));
        String resultStatus = resolveOrderResultStatus(reqVO.getStatus(), order.getId());
        if (reqVO.getItems() == null && ORDER_DONE.equals(resultStatus)) {
            orderItemMapper.updatePendingResultByTaskId(order.getId(), ITEM_NORMAL);
        }
        updateObj.setStatus(ORDER_WAIT_CONFIRM);
        if (orderMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        return order.getId();
    }

    @Override
    public List<ResourceDeviceMaintCandidateRespVO> getMaintCurrentMonthCandidates(ResourceDeviceMaintCandidateReqVO reqVO) {
        refreshOverdueOrders();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        List<Long> categoryIds = reqVO.getCategoryId() == null ? null : resolveCategoryTreeIds(reqVO.getCategoryId());
        List<ResourceDeviceLedgerDO> ledgers = ledgerMapper.selectEnabledList(categoryIds, reqVO.getDeviceName());
        List<ResourceDeviceMaintStandardDO> standards = standardMapper.selectEnabledList();
        boolean includeNormal = Boolean.TRUE.equals(reqVO.getIncludeNormal());
        List<ResourceDeviceMaintCandidateRespVO> result = new ArrayList<>();

        for (ResourceDeviceLedgerDO ledger : ledgers) {
            for (ResourceDeviceMaintStandardDO standard : standards) {
                if (!standardMatchesLedger(standard, ledger)) {
                    continue;
                }
                if (orderMapper.selectCurrentMonthOrder(ledger.getId(), standard.getId(), startDate, endDate) != null) {
                    continue;
                }
                ResourceDeviceMaintOrderDO latest = orderMapper.selectLatestByDeviceStandard(ledger.getId(), standard.getId());
                ResourceDeviceMaintCandidateRespVO candidate = buildMaintCandidate(ledger, standard, latest, startDate, endDate);
                if (includeNormal || Boolean.TRUE.equals(candidate.getOverdue())
                        || Boolean.TRUE.equals(candidate.getDueInCurrentMonth())) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int appendCurrentMonthMaintOrders(ResourceDeviceMaintCurrentMonthAppendReqVO reqVO) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        LocalDateTime planTime = LocalDateTime.now();
        int created = 0;
        Set<String> handledKeys = new HashSet<>();

        for (ResourceDeviceMaintCurrentMonthAppendReqVO.Candidate candidate : reqVO.getCandidates()) {
            String key = candidate.getDeviceId() + "#" + candidate.getStandardId();
            if (!handledKeys.add(key)) {
                continue;
            }
            if (orderMapper.selectCurrentMonthOrder(candidate.getDeviceId(), candidate.getStandardId(), startDate, endDate) != null) {
                continue;
            }
            ResourceDeviceLedgerDO ledger = validateLedgerExists(candidate.getDeviceId());
            ResourceDeviceMaintStandardDO standard = validateStandardExists(candidate.getStandardId());
            if (!standardMatchesLedger(standard, ledger)) {
                continue;
            }
            ResourceDeviceMaintOrderDO latest = orderMapper.selectLatestByDeviceStandard(candidate.getDeviceId(), candidate.getStandardId());
            LocalDate dueDate = resolveCandidateDueDate(latest, standard);
            LocalDate planDate = resolveCurrentMonthPlanDate(dueDate, startDate, endDate, today);
            if (isOpenMaintOrder(latest)) {
                ResourceDeviceMaintOrderDO updateObj = new ResourceDeviceMaintOrderDO();
                updateObj.setId(latest.getId());
                updateObj.setPlanDate(planDate);
                updateObj.setDueDate(dueDate);
                updateObj.setPlanTime(latest.getPlanTime() == null ? planTime : latest.getPlanTime());
                updateObj.setPlanPeriod(currentMonth.toString());
                if (!isBlank(reqVO.getExecutor())) {
                    updateObj.setExecutor(trimToNull(reqVO.getExecutor()));
                }
                updateObj.setRemark(defaultString(latest.getRemark(), "本月检验追加"));
                if (orderMapper.updateById(updateObj) == 0) {
                    throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
                }
                created++;
                continue;
            }
            ResourceDeviceMaintOrderDO order = new ResourceDeviceMaintOrderDO();
            order.setDeviceId(candidate.getDeviceId());
            order.setStandardId(candidate.getStandardId());
            order.setPlanDate(planDate);
            order.setDueDate(dueDate);
            order.setPlanTime(planTime);
            order.setPlanPeriod(currentMonth.toString());
            order.setExecutor(trimToNull(reqVO.getExecutor()));
            order.setStatus(ORDER_WAIT_EXECUTE);
            order.setRemark("本月检验追加");
            fillOrderDefaults(order);
            order.setTaskNo(generateNo("PMT"));
            order.setVersion(INITIAL_VERSION);
            orderMapper.insert(order);
            copyStandardItemsToOrder(order.getId(), order.getStandardId());
            created++;
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmMaintOrders(ResourceDeviceMaintConfirmReqVO reqVO) {
        int confirmed = 0;
        for (Long id : reqVO.getIds()) {
            ResourceDeviceMaintOrderDO order = validateMaintOrderExists(id);
            if (!ORDER_WAIT_CONFIRM.equals(order.getStatus())) {
                continue;
            }
            String resultStatus = resolveOrderResultStatus(null, order.getId());
            if (ORDER_DONE.equals(resultStatus)) {
                orderItemMapper.updatePendingResultByTaskId(order.getId(), ITEM_NORMAL);
            }
            ResourceDeviceMaintOrderDO updateObj = new ResourceDeviceMaintOrderDO();
            updateObj.setId(order.getId());
            updateObj.setStatus(resultStatus);
            updateObj.setConfirmer(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
            updateObj.setConfirmTime(LocalDateTime.now());
            if (order.getActualDate() == null) {
                updateObj.setActualDate(LocalDateTime.now());
            }
            if (orderMapper.updateById(updateObj) == 0) {
                throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
            }
            upsertMaintRecord(orderMapper.selectById(order.getId()));
            confirmed++;
        }
        return confirmed;
    }

    @Override
    public List<ResourceDeviceMaintOrderItemDO> getMaintOrderItemList(Long taskId) {
        return orderItemMapper.selectByTaskId(taskId);
    }

    @Override
    public List<ResourceDeviceMaintOrderPartDO> getMaintOrderPartList(Long taskId) {
        return orderPartMapper.selectByTaskId(taskId);
    }

    @Override
    public ResourceDeviceMaintRecordDO getMaintRecord(Long id) {
        return recordMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceDeviceMaintRecordDO> getMaintRecordPage(ResourceDeviceMaintRecordPageReqVO reqVO) {
        return recordMapper.selectPage(reqVO);
    }

    @Override
    public List<ResourceDeviceMaintRecordRespVO.MonthlySummary> getMaintRecordMonthlySummary(ResourceDeviceMaintRecordPageReqVO reqVO) {
        LocalDateTime[] range = reqVO.getActualTime();
        LocalDateTime startTime = range != null && range.length > 0 && range[0] != null
                ? range[0]
                : LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime endTime = range != null && range.length > 1 && range[1] != null
                ? range[1]
                : LocalDateTime.now();
        reqVO.setActualTime(new LocalDateTime[]{startTime, endTime});

        List<ResourceDeviceMaintOrderDO> orders = orderMapper.selectSummaryList(reqVO,
                startTime.toLocalDate(), endTime.toLocalDate());
        List<ResourceDeviceMaintRecordDO> records = recordMapper.selectSummaryList(reqVO);
        Map<MaintSummaryKey, MaintSummaryAccumulator> summaryMap = new LinkedHashMap<>();
        Map<Long, ResourceDeviceMaintOrderDO> orderMap = new LinkedHashMap<>();
        List<Long> recordOrderIds = records.stream()
                .map(ResourceDeviceMaintRecordDO::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!recordOrderIds.isEmpty()) {
            orderMapper.selectBatchIds(recordOrderIds).forEach(order -> orderMap.put(order.getId(), order));
        }

        orders.stream()
                .filter(order -> !ORDER_CANCELLED.equals(order.getStatus()))
                .forEach(order -> summaryMap.computeIfAbsent(MaintSummaryKey.of(order.getPlanDate(),
                        order.getCategoryId(), order.getCategoryName(), order.getMaintType()), MaintSummaryAccumulator::new)
                        .taskCount++);

        records.forEach(record -> {
            MaintSummaryAccumulator acc = summaryMap.computeIfAbsent(MaintSummaryKey.of(
                    record.getActualTime() == null ? null : record.getActualTime().toLocalDate(),
                    record.getCategoryId(), record.getCategoryName(), record.getMaintType()), MaintSummaryAccumulator::new);
            acc.recordCount++;
            if (ORDER_ABNORMAL.equals(record.getResultStatus())) {
                acc.abnormalCount++;
            } else if (ORDER_DONE.equals(record.getResultStatus())) {
                acc.normalCount++;
            }
            ResourceDeviceMaintOrderDO order = orderMap.get(record.getOrderId());
            if (order != null && order.getPlanDate() != null && record.getActualTime() != null
                    && record.getActualTime().toLocalDate().isAfter(defaultDate(order.getDueDate(), order.getPlanDate()))) {
                acc.overdueCompletedCount++;
            }
        });

        return summaryMap.values().stream()
                .sorted(Comparator.comparing((MaintSummaryAccumulator acc) -> acc.key.month())
                        .thenComparing(acc -> defaultString(acc.key.categoryName(), ""))
                        .thenComparing(acc -> defaultString(acc.key.maintType(), "")))
                .map(MaintSummaryAccumulator::toResp)
                .toList();
    }

    @Override
    public ResourceDeviceMaintOrderDO getMaintOrderByRecordId(Long recordId) {
        ResourceDeviceMaintRecordDO record = recordMapper.selectById(recordId);
        if (record == null) {
            throw exception(RESOURCE_DEVICE_MAINT_RECORD_NOT_EXISTS);
        }
        return orderMapper.selectById(record.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createException(ResourceDeviceExceptionSaveReqVO reqVO) {
        ResourceDeviceExceptionDO exception = BeanUtils.toBean(reqVO, ResourceDeviceExceptionDO.class);
        fillExceptionCreateDefaults(exception);
        exception.setVersion(INITIAL_VERSION);
        exceptionMapper.insert(exception);
        replaceExceptionParts(exception.getId(), reqVO.getParts());
        return exception.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateException(ResourceDeviceExceptionSaveReqVO reqVO) {
        ResourceDeviceExceptionDO existed = validateExceptionExists(reqVO.getId());
        validateExceptionEditable(existed);
        ResourceDeviceExceptionDO updateObj = BeanUtils.toBean(reqVO, ResourceDeviceExceptionDO.class);
        updateObj.setExceptionNo(defaultString(updateObj.getExceptionNo(), reqVO.getOrderNo()));
        fillExceptionUpdateDefaults(updateObj, existed);
        if (exceptionMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
        if (reqVO.getParts() != null) {
            replaceExceptionParts(reqVO.getId(), reqVO.getParts());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processException(ResourceDeviceExceptionProcessReqVO reqVO) {
        ResourceDeviceExceptionDO existed = validateExceptionExists(reqVO.getId());
        validateExceptionEditable(existed);
        String action = trimToNull(reqVO.getAction());
        if (EXCEPTION_ACTION_RESPOND.equals(action)) {
            processExceptionRespond(existed, reqVO);
            return;
        }
        if (EXCEPTION_ACTION_REPAIR.equals(action)) {
            processExceptionRepair(existed, reqVO);
            return;
        }
        if (EXCEPTION_ACTION_CONFIRM.equals(action)) {
            processExceptionConfirm(existed, reqVO);
            return;
        }
        if (EXCEPTION_ACTION_ARCHIVE.equals(action)) {
            processExceptionArchive(existed, reqVO);
            return;
        }
        throw invalidParamException("未知设备异常流程动作：{}", reqVO.getAction());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteException(Long id) {
        ResourceDeviceExceptionDO existed = validateExceptionExists(id);
        validateExceptionEditable(existed);
        exceptionPartMapper.deleteByExceptionId(id);
        exceptionMapper.deleteById(id);
    }

    @Override
    public ResourceDeviceExceptionDO getException(Long id) {
        return exceptionMapper.selectById(id);
    }

    @Override
    public PageResult<ResourceDeviceExceptionDO> getExceptionPage(ResourceDeviceExceptionPageReqVO reqVO) {
        return exceptionMapper.selectPage(reqVO);
    }

    @Override
    public List<ResourceDeviceExceptionPartDO> getExceptionPartList(Long exceptionId) {
        return exceptionPartMapper.selectByExceptionId(exceptionId);
    }

    private void processExceptionRespond(ResourceDeviceExceptionDO existed, ResourceDeviceExceptionProcessReqVO reqVO) {
        validateExceptionStatus(existed, EXCEPTION_REPORTED, "当前设备异常不在响应分派节点");
        String responseResult = defaultString(reqVO.getResponseResult(), EXCEPTION_RESPONSE_NEED_REPAIR);
        ResourceDeviceExceptionDO updateObj = new ResourceDeviceExceptionDO();
        updateObj.setId(existed.getId());
        updateObj.setDispatcherId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setDispatcher(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setDispatchTime(LocalDateTime.now());
        updateObj.setResponseResult(responseResult);
        updateObj.setResponseRemark(trimToNull(reqVO.getResponseRemark()));
        if (EXCEPTION_RESPONSE_HANDLED.equals(responseResult)) {
            updateObj.setStatus(EXCEPTION_PENDING_CONFIRM);
        } else if (EXCEPTION_RESPONSE_NEED_REPAIR.equals(responseResult)) {
            if (isBlank(reqVO.getAssignee())) {
                throw invalidParamException("请选择维修人");
            }
            updateObj.setAssigneeId(reqVO.getAssigneeId());
            updateObj.setAssignee(trimToNull(reqVO.getAssignee()));
            updateObj.setRepairPlanTime(reqVO.getRepairPlanTime());
            updateObj.setRepairPlanRemark(trimToNull(reqVO.getRepairPlanRemark()));
            updateObj.setPartChangeDesc(trimToNull(reqVO.getPartChangeDesc()));
            updateObj.setStatus(EXCEPTION_DISPATCHED);
        } else {
            throw invalidParamException("未知响应分派选项：{}", reqVO.getResponseResult());
        }
        updateExceptionById(updateObj);
    }

    private void processExceptionRepair(ResourceDeviceExceptionDO existed, ResourceDeviceExceptionProcessReqVO reqVO) {
        validateExceptionStatus(existed, EXCEPTION_DISPATCHED, "当前设备异常不在维修执行节点");
        validateExceptionNodeUser(existed.getAssigneeId(), "当前设备异常已分派给其他维修人");
        ResourceDeviceExceptionDO updateObj = new ResourceDeviceExceptionDO();
        updateObj.setId(existed.getId());
        updateObj.setRepairTime(reqVO.getRepairTime() == null ? LocalDateTime.now() : reqVO.getRepairTime());
        updateObj.setRepairStatus(defaultString(reqVO.getRepairStatus(), EXCEPTION_REPAIR_DONE));
        updateObj.setRepairAction(trimToNull(reqVO.getRepairAction()));
        updateObj.setUnfixReason(trimToNull(reqVO.getUnfixReason()));
        updateObj.setPartChangeDesc(trimToNull(reqVO.getPartChangeDesc()));
        updateObj.setStatus(EXCEPTION_PENDING_CONFIRM);
        updateExceptionById(updateObj);
        if (reqVO.getParts() != null) {
            replaceExceptionParts(existed.getId(), reqVO.getParts());
        }
    }

    private void processExceptionConfirm(ResourceDeviceExceptionDO existed, ResourceDeviceExceptionProcessReqVO reqVO) {
        validateExceptionStatus(existed, EXCEPTION_PENDING_CONFIRM, "当前设备异常不在完成确认节点");
        validateExceptionNodeUser(existed.getReporterId(), "当前设备异常需要由发起人完成确认");
        ResourceDeviceExceptionDO updateObj = new ResourceDeviceExceptionDO();
        updateObj.setId(existed.getId());
        updateObj.setConfirmerId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setConfirmer(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setConfirmTime(LocalDateTime.now());
        updateObj.setConfirmResult(defaultString(reqVO.getConfirmResult(), "CONFIRMED"));
        updateObj.setConfirmRemark(trimToNull(reqVO.getConfirmRemark()));
        updateObj.setStatus(EXCEPTION_PENDING_ARCHIVE);
        updateExceptionById(updateObj);
    }

    private void processExceptionArchive(ResourceDeviceExceptionDO existed, ResourceDeviceExceptionProcessReqVO reqVO) {
        validateExceptionStatus(existed, EXCEPTION_PENDING_ARCHIVE, "当前设备异常不在关闭归档节点");
        validateExceptionNodeUser(existed.getDispatcherId(), "当前设备异常需要由响应分派人关闭归档");
        ResourceDeviceExceptionDO updateObj = new ResourceDeviceExceptionDO();
        updateObj.setId(existed.getId());
        updateObj.setArchiverId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setArchiver(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setArchiveTime(LocalDateTime.now());
        updateObj.setArchiveReason(trimToNull(reqVO.getArchiveReason()));
        updateObj.setRootCause(trimToNull(reqVO.getRootCause()));
        updateObj.setPreventiveAction(trimToNull(reqVO.getPreventiveAction()));
        updateObj.setStatus(EXCEPTION_CLOSED);
        updateExceptionById(updateObj);
    }

    private void updateExceptionById(ResourceDeviceExceptionDO updateObj) {
        if (exceptionMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
    }

    private void validateExceptionStatus(ResourceDeviceExceptionDO exception, String expectedStatus, String message) {
        if (!expectedStatus.equals(exception.getStatus())) {
            throw invalidParamException(message);
        }
    }

    private void validateExceptionEditable(ResourceDeviceExceptionDO exception) {
        if (EXCEPTION_CLOSED.equals(exception.getStatus())) {
            throw invalidParamException("设备异常已关闭归档，不允许修改");
        }
    }

    private void validateExceptionNodeUser(Long expectedUserId, String message) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (expectedUserId != null && loginUserId != null && !Objects.equals(expectedUserId, loginUserId)) {
            throw invalidParamException(message);
        }
    }

    private void validateCategoryCodeUnique(Long id, String categoryCode) {
        ResourceDeviceCategoryDO existed = categoryMapper.selectByCode(categoryCode);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw exception(RESOURCE_DEVICE_CATEGORY_CODE_EXISTS);
        }
    }

    private ResourceDeviceCategoryDO validateCategoryExists(Long id) {
        ResourceDeviceCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(RESOURCE_DEVICE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private ResourceDeviceLedgerDO validateLedgerExists(Long id) {
        ResourceDeviceLedgerDO ledger = ledgerMapper.selectById(id);
        if (ledger == null) {
            throw exception(RESOURCE_DEVICE_LEDGER_NOT_EXISTS);
        }
        return ledger;
    }

    private void validateDeviceCodeUnique(Long id, String deviceCode) {
        ResourceDeviceLedgerDO existed = ledgerMapper.selectByDeviceCode(deviceCode);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw exception(RESOURCE_DEVICE_LEDGER_CODE_EXISTS);
        }
    }

    private ResourceDeviceMaintStandardDO validateStandardExists(Long id) {
        ResourceDeviceMaintStandardDO standard = standardMapper.selectById(id);
        if (standard == null) {
            throw exception(RESOURCE_DEVICE_MAINT_STANDARD_NOT_EXISTS);
        }
        return standard;
    }

    private void validateStandardCodeUnique(Long id, String code) {
        ResourceDeviceMaintStandardDO existed = standardMapper.selectByCode(code);
        if (existed != null && !Objects.equals(existed.getId(), id)) {
            throw exception(RESOURCE_DEVICE_MAINT_STANDARD_CODE_EXISTS);
        }
    }

    private ResourceDeviceMaintOrderDO validateMaintOrderExists(Long id) {
        ResourceDeviceMaintOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw exception(RESOURCE_DEVICE_MAINT_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private ResourceDeviceMaintPlanDO validateMaintPlanExists(Long id) {
        ResourceDeviceMaintPlanDO plan = maintPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(RESOURCE_DEVICE_MAINT_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    private ResourceDeviceExceptionDO validateExceptionExists(Long id) {
        ResourceDeviceExceptionDO exception = exceptionMapper.selectById(id);
        if (exception == null) {
            throw exception(RESOURCE_DEVICE_EXCEPTION_NOT_EXISTS);
        }
        return exception;
    }

    private String matrixRowKey(ResourceDeviceMaintPlanDO plan) {
        return defaultString(plan.getDeviceCode(), "") + "#"
                + defaultString(plan.getDeviceName(), "") + "#"
                + defaultString(plan.getStandardCode(), "") + "#"
                + defaultString(plan.getStandardName(), "");
    }

    private String matrixItemKey(ResourceDeviceMaintPlanDO plan) {
        if (plan.getStandardItemId() != null) {
            return "I#" + plan.getStandardItemId();
        }
        return "P#" + defaultString(plan.getItemGroup(), "") + "#"
                + defaultString(plan.getItemName(), "") + "#"
                + defaultString(plan.getMethod(), "") + "#"
                + defaultString(plan.getRequirement(), "") + "#"
                + defaultString(plan.getFrequency(), "");
    }

    private ResourceDeviceMaintPlanRespVO.CellItem buildMatrixCell(ResourceDeviceMaintPlanDO plan,
                                                                  ResourceDeviceMaintOrderDO order) {
        ResourceDeviceMaintPlanRespVO.CellItem cell = new ResourceDeviceMaintPlanRespVO.CellItem();
        cell.setId(plan.getId());
        cell.setMonthNo(plan.getMonthNo());
        cell.setWeekNo(plan.getWeekNo());
        cell.setWeekLabel(plan.getWeekNo() == null ? "" : "第" + plan.getWeekNo() + "周");
        cell.setPlanStartDate(plan.getPlanStartDate());
        cell.setPlanEndDate(plan.getPlanEndDate());
        cell.setPlanDate(plan.getPlanDate());
        cell.setGeneratedOrderId(plan.getGeneratedOrderId());
        cell.setGeneratedTaskNo(plan.getGeneratedTaskNo());
        LocalDateTime actualDate = plan.getActualDate() == null && order != null
                ? order.getActualDate() : plan.getActualDate();
        String executor = firstNotBlank(plan.getExecutor(), order == null ? null : order.getExecutor());
        String executeRemark = firstNotBlank(plan.getExecuteRemark(), order == null ? null : order.getExecuteRemark());
        String confirmer = firstNotBlank(plan.getConfirmer(), order == null ? null : order.getConfirmer());
        LocalDateTime confirmTime = plan.getConfirmTime() == null && order != null
                ? order.getConfirmTime() : plan.getConfirmTime();
        String executeStatus = resolvePlanExecuteStatus(plan, order);
        cell.setActualDate(actualDate);
        cell.setExecutor(executor);
        cell.setExecuteRemark(executeRemark);
        cell.setConfirmer(confirmer);
        cell.setConfirmTime(confirmTime);
        cell.setItemGroup(plan.getItemGroup());
        cell.setItemName(plan.getItemName());
        cell.setMaintType(plan.getMaintType());
        cell.setFrequency(plan.getFrequency());
        cell.setPublished(Boolean.TRUE.equals(plan.getPublished()));
        cell.setStatus(plan.getStatus());
        cell.setOrderStatus(order == null ? null : order.getStatus());
        cell.setExecuteStatus(executeStatus);
        boolean executed = actualDate != null || Set.of(ORDER_DONE, ORDER_ABNORMAL, ORDER_WAIT_CONFIRM).contains(executeStatus);
        boolean overdue = ORDER_OVERDUE.equals(executeStatus);
        cell.setExecuted(executed);
        cell.setOverdue(overdue);
        cell.setCellStatus(executeStatus);
        return cell;
    }

    private String resolvePlanExecuteStatus(ResourceDeviceMaintPlanDO plan, ResourceDeviceMaintOrderDO order) {
        String planStatus = trimToNull(plan.getExecuteStatus());
        if (isClosedOrSubmittedMaintStatus(planStatus)) {
            return planStatus;
        }
        String orderStatus = order == null ? null : trimToNull(order.getStatus());
        if (isClosedOrSubmittedMaintStatus(orderStatus)) {
            return orderStatus;
        }
        if (plan.getActualDate() != null || order != null && order.getActualDate() != null) {
            return ORDER_WAIT_CONFIRM;
        }
        if (ORDER_OVERDUE.equals(planStatus) || ORDER_OVERDUE.equals(orderStatus)) {
            return ORDER_OVERDUE;
        }
        if (plan.getPlanEndDate() != null && plan.getPlanEndDate().isBefore(LocalDate.now())) {
            return ORDER_OVERDUE;
        }
        return PLAN_EXEC_PENDING;
    }

    private boolean isClosedOrSubmittedMaintStatus(String status) {
        return ORDER_DONE.equals(status) || ORDER_ABNORMAL.equals(status) || ORDER_WAIT_CONFIRM.equals(status);
    }

    private void keepMaintPlanExecution(ResourceDeviceMaintPlanDO target, ResourceDeviceMaintPlanDO source) {
        target.setExecuteStatus(source.getExecuteStatus());
        target.setActualDate(source.getActualDate());
        target.setExecutor(source.getExecutor());
        target.setExecuteRemark(source.getExecuteRemark());
        target.setConfirmer(source.getConfirmer());
        target.setConfirmTime(source.getConfirmTime());
    }

    private void syncGeneratedOrderPlan(ResourceDeviceMaintPlanDO plan) {
        if (plan.getGeneratedOrderId() == null) {
            return;
        }
        ResourceDeviceMaintOrderDO order = orderMapper.selectById(plan.getGeneratedOrderId());
        if (order == null || ORDER_DONE.equals(order.getStatus()) || ORDER_ABNORMAL.equals(order.getStatus())
                || ORDER_CANCELLED.equals(order.getStatus())) {
            return;
        }
        ResourceDeviceMaintOrderDO updateObj = new ResourceDeviceMaintOrderDO();
        updateObj.setId(order.getId());
        updateObj.setPlanPeriod(plan.getPlanPeriod());
        updateObj.setPlanDate(plan.getPlanDate());
        updateObj.setDueDate(plan.getPlanEndDate());
        updateObj.setPlanTime(plan.getPlanDate() == null ? order.getPlanTime() : plan.getPlanDate().atStartOfDay());
        updateObj.setFrequency(plan.getFrequency());
        updateObj.setMaintType(plan.getMaintType());
        updateObj.setTaskDesc(displayMaintItemName(plan.getItemGroup(), plan.getItemName()));
        if (orderMapper.updateById(updateObj) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
    }

    private void fillMaintPlanDefaults(ResourceDeviceMaintPlanDO plan) {
        fillDeviceSnapshotByCode(plan);
        if (plan.getStandardId() != null) {
            ResourceDeviceMaintStandardDO standard = validateStandardExists(plan.getStandardId());
            plan.setStandardCode(defaultString(plan.getStandardCode(), standard.getCode()));
            plan.setStandardName(defaultString(plan.getStandardName(), standard.getName()));
            plan.setFrequency(defaultString(plan.getFrequency(), standard.getFrequency()));
            plan.setMaintType(defaultString(plan.getMaintType(), standard.getMaintType()));
        }
        if (plan.getStandardItemId() != null) {
            ResourceDeviceMaintStandardItemDO item = standardItemMapper.selectById(plan.getStandardItemId());
            if (item != null) {
                plan.setItemGroup(defaultString(plan.getItemGroup(), item.getItemGroup()));
                plan.setItemName(defaultString(plan.getItemName(), item.getItemName()));
                plan.setMethod(defaultString(plan.getMethod(), item.getMethod()));
                plan.setRequirement(defaultString(plan.getRequirement(), item.getRequirement()));
                plan.setFrequency(defaultString(plan.getFrequency(), item.getFrequency()));
            }
        }
        plan.setMaintType(defaultString(plan.getMaintType(), inferMaintType(plan.getFrequency())));
        normalizePlanItemGroup(plan);
        plan.setPublished(plan.getPublished() != null && plan.getPublished());
        plan.setStatus(defaultString(plan.getStatus(), plan.getPublished() ? PLAN_STATUS_PUBLISHED : PLAN_STATUS_DRAFT));
        plan.setExecuteStatus(defaultString(plan.getExecuteStatus(), PLAN_EXEC_PENDING));
        resolvePlanWeek(plan);
        plan.setPlanPeriod(String.format("%d-M%02d-W%d", plan.getPlanYear(), plan.getMonthNo(), plan.getWeekNo()));
        plan.setSourceRule(defaultString(plan.getSourceRule(), plan.getStandardCode()));
    }

    private void fillDeviceSnapshotByCode(ResourceDeviceMaintPlanDO plan) {
        ResourceDeviceLedgerDO ledger = null;
        if (plan.getDeviceId() != null) {
            ledger = ledgerMapper.selectById(plan.getDeviceId());
        }
        if (ledger == null && !isBlank(plan.getDeviceCode())) {
            ledger = ledgerMapper.selectByDeviceCode(plan.getDeviceCode());
        }
        if (ledger == null) {
            return;
        }
        plan.setDeviceId(ledger.getId());
        plan.setDeviceCode(defaultString(plan.getDeviceCode(), ledger.getDeviceCode()));
        plan.setDeviceName(defaultString(plan.getDeviceName(), ledger.getDeviceName()));
        plan.setCategoryId(ledger.getCategoryId());
        plan.setCategoryName(ledger.getCategoryName());
    }

    private void resolvePlanWeek(ResourceDeviceMaintPlanDO plan) {
        if (plan.getPlanYear() == null || plan.getMonthNo() == null || plan.getWeekNo() == null) {
            throw invalidParamException("计划年份、月份和周次不能为空");
        }
        if (plan.getMonthNo() < 1 || plan.getMonthNo() > 12 || plan.getWeekNo() < 1 || plan.getWeekNo() > 5) {
            throw invalidParamException("计划月份或周次超出范围");
        }
        YearMonth month = YearMonth.of(plan.getPlanYear(), plan.getMonthNo());
        LocalDate start = month.atDay(Math.min((plan.getWeekNo() - 1) * 7 + 1, month.lengthOfMonth()));
        LocalDate end = plan.getWeekNo() >= 5 ? month.atEndOfMonth()
                : month.atDay(Math.min(plan.getWeekNo() * 7, month.lengthOfMonth()));
        plan.setPlanStartDate(plan.getPlanStartDate() == null ? start : plan.getPlanStartDate());
        plan.setPlanEndDate(plan.getPlanEndDate() == null ? end : plan.getPlanEndDate());
        plan.setPlanDate(plan.getPlanDate() == null ? plan.getPlanStartDate() : plan.getPlanDate());
    }

    private ResourceDeviceMaintOrderDO buildOrderFromPlan(ResourceDeviceMaintPlanDO plan) {
        ResourceDeviceMaintOrderDO order = new ResourceDeviceMaintOrderDO();
        order.setTaskNo(generateNo("PMT"));
        order.setPlanPeriod(plan.getPlanPeriod());
        order.setDeviceId(plan.getDeviceId());
        order.setDeviceCode(plan.getDeviceCode());
        order.setDeviceName(plan.getDeviceName());
        order.setCategoryId(plan.getCategoryId());
        order.setCategoryName(plan.getCategoryName());
        order.setStandardId(plan.getStandardId());
        order.setStandardCode(plan.getStandardCode());
        order.setStandardName(plan.getStandardName());
        order.setFrequency(plan.getFrequency());
        order.setMaintType(plan.getMaintType());
        order.setTaskDesc(displayMaintItemName(plan.getItemGroup(), plan.getItemName()));
        order.setPlanDate(plan.getPlanDate());
        order.setDueDate(plan.getPlanEndDate());
        order.setPlanTime(plan.getPlanDate() == null ? LocalDateTime.now() : plan.getPlanDate().atStartOfDay());
        order.setStatus(ORDER_WAIT_EXECUTE);
        order.setRemark(firstNotBlank(plan.getRemark(), "由年度保养计划生成"));
        order.setVersion(INITIAL_VERSION);
        return order;
    }

    private void fillExceptionCreateDefaults(ResourceDeviceExceptionDO exception) {
        fillExceptionDeviceSnapshot(exception);
        exception.setExceptionNo(defaultString(exception.getExceptionNo(), generateNo("DEX")));
        exception.setExceptionLevel(defaultString(exception.getExceptionLevel(), "MAJOR"));
        exception.setReporterId(exception.getReporterId() == null ? SecurityFrameworkUtils.getLoginUserId() : exception.getReporterId());
        exception.setReporter(defaultString(exception.getReporter(),
                firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统")));
        exception.setReportTime(exception.getReportTime() == null ? LocalDateTime.now() : exception.getReportTime());
        exception.setStatus(EXCEPTION_REPORTED);
    }

    private void fillExceptionUpdateDefaults(ResourceDeviceExceptionDO target, ResourceDeviceExceptionDO source) {
        fillExceptionDeviceSnapshot(target);
        target.setExceptionNo(defaultString(target.getExceptionNo(), source.getExceptionNo()));
        target.setExceptionLevel(defaultString(target.getExceptionLevel(), source.getExceptionLevel()));
        target.setReporterId(source.getReporterId());
        target.setReporter(source.getReporter());
        target.setReportTime(source.getReportTime());
        target.setDispatcherId(source.getDispatcherId());
        target.setDispatcher(source.getDispatcher());
        target.setDispatchTime(source.getDispatchTime());
        target.setResponseResult(source.getResponseResult());
        target.setResponseRemark(source.getResponseRemark());
        target.setAssigneeId(source.getAssigneeId());
        target.setAssignee(source.getAssignee());
        target.setRepairPlanTime(source.getRepairPlanTime());
        target.setRepairPlanRemark(source.getRepairPlanRemark());
        target.setRepairTime(source.getRepairTime());
        target.setRepairStatus(source.getRepairStatus());
        target.setPartChangeDesc(source.getPartChangeDesc());
        target.setConfirmResult(source.getConfirmResult());
        target.setConfirmRemark(source.getConfirmRemark());
        target.setConfirmerId(source.getConfirmerId());
        target.setConfirmer(source.getConfirmer());
        target.setConfirmTime(source.getConfirmTime());
        target.setArchiverId(source.getArchiverId());
        target.setArchiver(source.getArchiver());
        target.setArchiveTime(source.getArchiveTime());
        target.setArchiveReason(source.getArchiveReason());
        target.setStatus(source.getStatus());
    }

    private void fillExceptionDeviceSnapshot(ResourceDeviceExceptionDO exception) {
        ResourceDeviceLedgerDO ledger = null;
        if (exception.getDeviceId() != null) {
            ledger = ledgerMapper.selectById(exception.getDeviceId());
        }
        if (ledger == null && !isBlank(exception.getDeviceCode())) {
            ledger = ledgerMapper.selectByDeviceCode(exception.getDeviceCode());
        }
        if (ledger != null) {
            exception.setDeviceId(ledger.getId());
            exception.setDeviceCode(defaultString(exception.getDeviceCode(), ledger.getDeviceCode()));
            exception.setDeviceName(defaultString(exception.getDeviceName(), ledger.getDeviceName()));
        }
    }

    private void fillLedgerDefaults(ResourceDeviceLedgerDO ledger) {
        fillLedgerCategorySnapshot(ledger);
        ledger.setStatus(defaultInt(ledger.getStatus(), LEDGER_STATUS_NORMAL));
        ledger.setMaintStatus(defaultString(ledger.getMaintStatus(), MAINT_STATUS_NORMAL));
    }

    private void fillStandardDefaults(ResourceDeviceMaintStandardDO standard) {
        fillCategorySnapshot(standard.getCategoryId(), standard::setCategoryName);
        standard.setStatus(defaultInt(standard.getStatus(), STATUS_ENABLED));
        standard.setMaintType(defaultString(standard.getMaintType(), "日常巡检"));
    }

    private void fillOrderDefaults(ResourceDeviceMaintOrderDO order) {
        if (order.getDeviceId() != null) {
            ResourceDeviceLedgerDO ledger = validateLedgerExists(order.getDeviceId());
            order.setDeviceCode(defaultString(order.getDeviceCode(), ledger.getDeviceCode()));
            order.setDeviceName(defaultString(order.getDeviceName(), ledger.getDeviceName()));
            order.setCategoryId(ledger.getCategoryId());
            order.setCategoryName(ledger.getCategoryName());
        }
        if (order.getStandardId() != null) {
            ResourceDeviceMaintStandardDO standard = validateStandardExists(order.getStandardId());
            order.setStandardCode(defaultString(order.getStandardCode(), standard.getCode()));
            order.setStandardName(defaultString(order.getStandardName(), standard.getName()));
            order.setFrequency(defaultString(order.getFrequency(), standard.getFrequency()));
            order.setMaintType(defaultString(order.getMaintType(), standard.getMaintType()));
            order.setTaskDesc(defaultString(order.getTaskDesc(), standard.getRemark()));
            if (order.getCategoryId() == null) {
                order.setCategoryId(standard.getCategoryId());
                order.setCategoryName(standard.getCategoryName());
            }
        }
        order.setPlanDate(order.getPlanDate() == null ? LocalDate.now() : order.getPlanDate());
        order.setDueDate(order.getDueDate() == null ? order.getPlanDate() : order.getDueDate());
        order.setPlanPeriod(defaultString(order.getPlanPeriod(), YearMonth.from(order.getPlanDate()).toString()));
        order.setStatus(defaultString(order.getStatus(), ORDER_WAIT_EXECUTE));
    }

    private boolean standardMatchesLedger(ResourceDeviceMaintStandardDO standard, ResourceDeviceLedgerDO ledger) {
        boolean categoryMatched = standard.getCategoryId() == null
                || Objects.equals(standard.getCategoryId(), ledger.getCategoryId());
        boolean deviceTypeMatched = isBlank(standard.getDeviceType())
                || isBlank(ledger.getDeviceType())
                || Objects.equals(standard.getDeviceType(), ledger.getDeviceType());
        return categoryMatched && deviceTypeMatched;
    }

    private ResourceDeviceMaintCandidateRespVO buildMaintCandidate(ResourceDeviceLedgerDO ledger,
                                                                  ResourceDeviceMaintStandardDO standard,
                                                                  ResourceDeviceMaintOrderDO latest,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate) {
        LocalDate taskDueDate = resolveCandidateDueDate(latest, standard);
        boolean overdue = taskDueDate != null && taskDueDate.isBefore(startDate);
        boolean dueInCurrentMonth = taskDueDate != null
                && !taskDueDate.isBefore(startDate)
                && !taskDueDate.isAfter(endDate);

        ResourceDeviceMaintCandidateRespVO respVO = new ResourceDeviceMaintCandidateRespVO();
        respVO.setDeviceId(ledger.getId());
        respVO.setDeviceCode(ledger.getDeviceCode());
        respVO.setDeviceName(ledger.getDeviceName());
        respVO.setCategoryId(ledger.getCategoryId());
        respVO.setCategoryName(ledger.getCategoryName());
        respVO.setStandardId(standard.getId());
        respVO.setStandardCode(standard.getCode());
        respVO.setStandardName(standard.getName());
        respVO.setFrequency(standard.getFrequency());
        respVO.setMaintType(standard.getMaintType());
        respVO.setLastPlanDate(latest == null ? null : latest.getPlanDate());
        respVO.setLastActualDate(latest == null || latest.getActualDate() == null ? null : latest.getActualDate().toLocalDate());
        respVO.setTaskDueDate(taskDueDate);
        respVO.setDueInCurrentMonth(dueInCurrentMonth);
        respVO.setOverdue(overdue);
        respVO.setWarningStatus(overdue ? ORDER_OVERDUE : dueInCurrentMonth ? "DUE_SOON" : MAINT_STATUS_NORMAL);
        return respVO;
    }

    private LocalDate resolveCandidateDueDate(ResourceDeviceMaintOrderDO latest, ResourceDeviceMaintStandardDO standard) {
        if (latest == null) {
            return LocalDate.now();
        }
        if (!Set.of(ORDER_DONE, ORDER_ABNORMAL).contains(latest.getStatus())) {
            return defaultDate(normalizeBusinessDate(latest.getDueDate()),
                    defaultDate(normalizeBusinessDate(latest.getPlanDate()), LocalDate.now()));
        }
        LocalDate baseDate = latest.getActualDate() == null ? latest.getPlanDate() : latest.getActualDate().toLocalDate();
        baseDate = normalizeBusinessDate(baseDate);
        if (baseDate == null) {
            return LocalDate.now();
        }
        return plusMaintFrequency(baseDate, standard.getFrequency());
    }

    private LocalDate resolveCurrentMonthPlanDate(LocalDate dueDate, LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (dueDate != null && !dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)) {
            return dueDate;
        }
        if (today.isBefore(startDate)) {
            return startDate;
        }
        if (today.isAfter(endDate)) {
            return endDate;
        }
        return today;
    }

    private boolean isOpenMaintOrder(ResourceDeviceMaintOrderDO order) {
        return order != null && !Set.of(ORDER_DONE, ORDER_ABNORMAL, ORDER_CANCELLED).contains(order.getStatus());
    }

    private LocalDate plusMaintFrequency(LocalDate baseDate, String frequency) {
        String value = defaultString(frequency, "MONTHLY").toUpperCase();
        if (value.contains("PRE_START") || value.contains("PRE_SHIFT")
                || value.contains("DAILY") || value.contains("每日") || value.contains("每天")) {
            return baseDate.plusDays(1);
        }
        if (value.contains("WEEKLY") || value.contains("每周") || value.contains("周")) {
            return baseDate.plusWeeks(1);
        }
        if (value.contains("QUARTER") || value.contains("季度")) {
            return baseDate.plusMonths(3);
        }
        if (value.contains("YEAR") || value.contains("年度") || value.contains("每年")) {
            return baseDate.plusYears(1);
        }
        return baseDate.plusMonths(1);
    }

    private String inferMaintType(String frequency) {
        String value = defaultString(frequency, "");
        if (value.contains("半年")) {
            return "半年保养";
        }
        if (value.contains("季度")) {
            return "季度保养";
        }
        if (value.contains("年")) {
            return "年度保养";
        }
        if (value.contains("周")) {
            return "周保养";
        }
        return "月度保养";
    }

    private void fillCategorySnapshot(Long categoryId, java.util.function.Consumer<String> setter) {
        if (categoryId == null) {
            setter.accept(null);
            return;
        }
        ResourceDeviceCategoryDO category = validateCategoryExists(categoryId);
        setter.accept(category.getCategoryName());
    }

    private void fillLedgerCategorySnapshot(ResourceDeviceLedgerDO ledger) {
        Long categoryId = ledger.getCategoryId();
        if (categoryId == null) {
            ledger.setCategoryName(null);
            ledger.setDeviceType(null);
            return;
        }
        ResourceDeviceCategoryDO category = validateCategoryExists(categoryId);
        ledger.setCategoryName(category.getCategoryName());
        if (category.getParentId() != null && category.getParentId() > 0) {
            ResourceDeviceCategoryDO parent = categoryMapper.selectById(category.getParentId());
            ledger.setDeviceType(parent == null ? category.getCategoryName() : parent.getCategoryName());
        } else {
            ledger.setDeviceType(category.getCategoryName());
        }
    }

    private void replaceDeviceParts(Long deviceId, List<ResourceDeviceLedgerSaveReqVO.DevicePart> parts) {
        partMapper.deleteByDeviceId(deviceId);
        if (parts == null || parts.isEmpty()) {
            return;
        }
        List<ResourceDevicePartDO> rows = parts.stream()
                .map(item -> {
                    ResourceDevicePartDO row = BeanUtils.toBean(item, ResourceDevicePartDO.class);
                    row.setId(null);
                    row.setDeviceId(deviceId);
                    row.setSort(defaultInt(row.getSort(), DEFAULT_SORT));
                    return row;
                })
                .toList();
        partMapper.insertBatch(rows);
    }

    private void replaceDeviceParams(Long deviceId, List<ResourceDeviceLedgerSaveReqVO.DeviceParam> params) {
        paramMapper.deleteByDeviceId(deviceId);
        if (params == null || params.isEmpty()) {
            return;
        }
        List<ResourceDeviceParamDO> rows = params.stream()
                .map(item -> {
                    ResourceDeviceParamDO row = BeanUtils.toBean(item, ResourceDeviceParamDO.class);
                    row.setId(null);
                    row.setDeviceId(deviceId);
                    row.setSort(defaultInt(row.getSort(), DEFAULT_SORT));
                    return row;
                })
                .toList();
        paramMapper.insertBatch(rows);
    }

    private void replaceStandardItems(Long standardId, List<ResourceDeviceMaintStandardSaveReqVO.StandardItem> items) {
        standardItemMapper.deleteByStandardId(standardId);
        if (items == null || items.isEmpty()) {
            return;
        }
        ResourceDeviceMaintStandardDO standard = standardMapper.selectById(standardId);
        List<ResourceDeviceMaintStandardItemDO> rows = items.stream()
                .map(item -> {
                    ResourceDeviceMaintStandardItemDO row = BeanUtils.toBean(item, ResourceDeviceMaintStandardItemDO.class);
                    row.setId(null);
                    row.setStandardId(standardId);
                    row.setFrequency(defaultString(row.getFrequency(), standard == null ? null : standard.getFrequency()));
                    row.setResultType(defaultString(row.getResultType(), "CHECK"));
                    row.setRequiredFlag(row.getRequiredFlag() == null || row.getRequiredFlag());
                    row.setSort(defaultInt(row.getSort(), DEFAULT_SORT));
                    return row;
                })
                .toList();
        standardItemMapper.insertBatch(rows);
    }

    private void copyStandardItemsToOrder(Long taskId, Long standardId) {
        List<ResourceDeviceMaintOrderItemDO> rows = standardItemMapper.selectByStandardId(standardId).stream()
                .map(item -> ResourceDeviceMaintOrderItemDO.builder()
                        .taskId(taskId)
                        .standardItemId(item.getId())
                        .itemName(displayMaintItemName(item.getItemGroup(), item.getItemName()))
                        .method(item.getMethod())
                        .requirement(item.getRequirement())
                        .result(ITEM_PENDING)
                        .sort(defaultInt(item.getSort(), DEFAULT_SORT))
                        .build())
                .toList();
        if (!rows.isEmpty()) {
            orderItemMapper.insertBatch(rows);
        }
    }

    private void replaceOrderItems(Long taskId, List<? extends Object> items) {
        orderItemMapper.deleteByTaskId(taskId);
        if (items == null || items.isEmpty()) {
            return;
        }
        List<ResourceDeviceMaintOrderItemDO> rows = new ArrayList<>();
        for (Object item : items) {
            ResourceDeviceMaintOrderItemDO row = BeanUtils.toBean(item, ResourceDeviceMaintOrderItemDO.class);
            row.setId(null);
            row.setTaskId(taskId);
            row.setResult(defaultString(row.getResult(), ITEM_PENDING));
            row.setSort(defaultInt(row.getSort(), DEFAULT_SORT));
            rows.add(row);
        }
        orderItemMapper.insertBatch(rows);
    }

    private void replaceOrderParts(Long taskId, List<? extends Object> parts) {
        orderPartMapper.deleteByTaskId(taskId);
        if (parts == null || parts.isEmpty()) {
            return;
        }
        List<ResourceDeviceMaintOrderPartDO> rows = new ArrayList<>();
        for (Object part : parts) {
            ResourceDeviceMaintOrderPartDO row = BeanUtils.toBean(part, ResourceDeviceMaintOrderPartDO.class);
            row.setId(null);
            row.setTaskId(taskId);
            row.setSort(defaultInt(row.getSort(), DEFAULT_SORT));
            rows.add(row);
        }
        orderPartMapper.insertBatch(rows);
    }

    private void replaceExceptionParts(Long exceptionId, List<? extends Object> parts) {
        exceptionPartMapper.deleteByExceptionId(exceptionId);
        if (parts == null || parts.isEmpty()) {
            return;
        }
        List<ResourceDeviceExceptionPartDO> rows = new ArrayList<>();
        for (Object part : parts) {
            ResourceDeviceExceptionPartDO row = BeanUtils.toBean(part, ResourceDeviceExceptionPartDO.class);
            row.setId(null);
            row.setExceptionId(exceptionId);
            row.setSort(defaultInt(row.getSort(), DEFAULT_SORT));
            rows.add(row);
        }
        exceptionPartMapper.insertBatch(rows);
    }

    private String resolveOrderResultStatus(String requestedStatus, Long taskId) {
        if (ORDER_ABNORMAL.equals(requestedStatus)) {
            return ORDER_ABNORMAL;
        }
        boolean hasAbnormal = orderItemMapper.selectByTaskId(taskId).stream()
                .anyMatch(item -> ITEM_ABNORMAL.equals(item.getResult()));
        return hasAbnormal ? ORDER_ABNORMAL : ORDER_DONE;
    }

    private void upsertMaintRecord(ResourceDeviceMaintOrderDO order) {
        ResourceDeviceMaintRecordDO existed = recordMapper.selectByOrderId(order.getId());
        ResourceDeviceMaintRecordDO record = new ResourceDeviceMaintRecordDO();
        record.setId(existed == null ? null : existed.getId());
        record.setRecordNo(existed == null ? generateNo("PMR") : existed.getRecordNo());
        record.setOrderId(order.getId());
        record.setTaskNo(order.getTaskNo());
        record.setDeviceId(order.getDeviceId());
        record.setDeviceCode(order.getDeviceCode());
        record.setDeviceName(order.getDeviceName());
        record.setCategoryId(order.getCategoryId());
        record.setCategoryName(order.getCategoryName());
        record.setStandardId(order.getStandardId());
        record.setStandardName(order.getStandardName());
        record.setMaintType(order.getMaintType());
        record.setFrequency(order.getFrequency());
        record.setExecutor(order.getExecutor());
        record.setDueDate(order.getDueDate());
        record.setPlanTime(order.getPlanTime());
        record.setActualTime(order.getActualDate());
        record.setConfirmer(order.getConfirmer());
        record.setConfirmTime(order.getConfirmTime());
        record.setPhotos(order.getPhotos());
        record.setResultStatus(order.getStatus());
        record.setExceptionId(order.getExceptionId());
        record.setExceptionNo(order.getExceptionNo());
        record.setRemark(firstNotBlank(order.getExecuteRemark(), order.getRemark()));
        if (existed == null) {
            recordMapper.insert(record);
        } else if (recordMapper.updateById(record) == 0) {
            throw exception(RESOURCE_DEVICE_VERSION_CONFLICT);
        }
    }

    private void refreshOverdueOrders() {
        List<ResourceDeviceMaintOrderDO> rows = orderMapper.selectDueOpenOrders(LocalDate.now());
        for (ResourceDeviceMaintOrderDO row : rows) {
            ResourceDeviceMaintOrderDO updateObj = new ResourceDeviceMaintOrderDO();
            updateObj.setId(row.getId());
            updateObj.setStatus("OVERDUE");
            orderMapper.updateById(updateObj);
        }
    }

    private List<Long> resolveCategoryTreeIds(Long categoryId) {
        List<ResourceDeviceCategoryDO> categories = categoryMapper.selectList(new ResourceDeviceCategoryListReqVO());
        List<Long> result = new ArrayList<>();
        collectCategoryIds(categoryId, categories, result);
        return result;
    }

    private void collectCategoryIds(Long id, List<ResourceDeviceCategoryDO> categories, List<Long> result) {
        result.add(id);
        categories.stream()
                .filter(item -> Objects.equals(item.getParentId(), id))
                .forEach(item -> collectCategoryIds(item.getId(), categories, result));
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_DATE_FORMATTER)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static void normalizePlanItemGroup(ResourceDeviceMaintPlanDO plan) {
        if (plan == null || !isBlank(plan.getItemName())) {
            return;
        }
        String group = trimToNull(plan.getItemGroup());
        if (group == null) {
            return;
        }
        String[] parts = group.split("\\s*/\\s*", 2);
        if (parts.length == 2) {
            plan.setItemGroup(trimToNull(parts[0]));
            plan.setItemName(trimToNull(parts[1]));
            return;
        }
        plan.setItemName(group);
    }

    private static String displayMaintItemName(String itemGroup, String itemName) {
        String group = trimToNull(itemGroup);
        String name = trimToNull(itemName);
        if (group == null) {
            return name;
        }
        if (name == null || group.equals(name)) {
            return group;
        }
        return group + " / " + name;
    }

    private static int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static LocalDate defaultDate(LocalDate value, LocalDate defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static LocalDate normalizeBusinessDate(LocalDate value) {
        return value == null || value.isBefore(MIN_VALID_BUSINESS_DATE) ? null : value;
    }

    private static String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private static String firstNotBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private record MaintSummaryKey(String month, Long categoryId, String categoryName, String maintType) {
        static MaintSummaryKey of(LocalDate date, Long categoryId, String categoryName, String maintType) {
            String month = date == null ? "" : YearMonth.from(date).toString();
            return new MaintSummaryKey(month, categoryId, categoryName, maintType);
        }
    }

    private static final class MaintSummaryAccumulator {
        private final MaintSummaryKey key;
        private long taskCount;
        private long recordCount;
        private long normalCount;
        private long abnormalCount;
        private long overdueCompletedCount;

        private MaintSummaryAccumulator(MaintSummaryKey key) {
            this.key = key;
        }

        private ResourceDeviceMaintRecordRespVO.MonthlySummary toResp() {
            BigDecimal completionRate = taskCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(recordCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(taskCount), 2, RoundingMode.HALF_UP);
            return new ResourceDeviceMaintRecordRespVO.MonthlySummary(key.month(), key.categoryId(), key.categoryName(),
                    key.maintType(), taskCount, recordCount, normalCount, abnormalCount, overdueCompletedCount,
                    completionRate);
        }
    }

}
