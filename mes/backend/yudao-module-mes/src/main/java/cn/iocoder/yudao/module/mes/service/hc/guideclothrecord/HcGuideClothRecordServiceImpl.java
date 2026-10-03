package cn.iocoder.yudao.module.mes.service.hc.guideclothrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo.HcGuideClothRuntimeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.guideclothrecord.HcGuideClothRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcProductionRecordPadTypeResolver;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineContext;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineResolverService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcGuideClothRecordServiceImpl implements HcGuideClothRecordService {

    private static final int CURRENT_FLAG_CURRENT = 0;
    private static final int CURRENT_FLAG_HISTORY = 1;
    private static final String LINE_CODE_WHITE = "WHITE";
    private static final String LINE_CODE_BLACK = "BLACK";
    private static final String PROCESS_CODE_WET = "WET";
    private static final String PROCESS_NAME_WET = "湿法";
    private static final String CONSUMABLE_TYPE_GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final String CONSUMABLE_TYPE_PET = "PET";
    private static final String LEDGER_USAGE_STATUS_ACTIVE = "ACTIVE";
    private static final String EVENT_TYPE_USE = "USE";
    private static final String EVENT_TYPE_REPLACE = "REPLACE";
    private static final String EVENT_TYPE_RND_MANUAL_USE = "RND_MANUAL_USE";
    private static final String RECORD_SOURCE_RND_MANUAL = "RND_MANUAL";
    private static final String BIZ_TYPE_RND_MANUAL = "WET_GUIDE_CLOTH_RND_MANUAL";
    private static final String STATUS_IN_USE = "IN_USE";
    private static final BigDecimal DEFAULT_GUIDE_CLOTH_LIMIT_LENGTH = new BigDecimal("300.000");
    private static final int GUIDE_CLOTH_MAX_USE_COUNT = 28;
    private static final int GUIDE_CLOTH_MONTH_REMIND_BEFORE_DAYS = 3;
    private static final int MIN_VALID_BUSINESS_YEAR = 2000;
    private static final String IMPORT_INIT_MESSAGE = "Excel导入初始化";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcGuideClothRecordMapper hcGuideClothRecordMapper;

    @Resource
    private HcEquipmentMapper hcEquipmentMapper;

    @Resource
    private HcEquipmentConsumableStateMapper hcEquipmentConsumableStateMapper;

    @Resource
    private HcEquipmentConsumableEventMapper hcEquipmentConsumableEventMapper;

    @Resource
    private HcToolingConsumableLedgerMapper hcToolingConsumableLedgerMapper;

    @Resource
    private HcProductionLineResolverService hcProductionLineResolverService;

    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HcGuideClothRecordSaveReqVO reqVO) {
        validateReplaceTime(reqVO.getReplaceTime());
        HcGuideClothRecordDO entity = BeanUtils.toBean(reqVO, HcGuideClothRecordDO.class);
        hcGuideClothRecordMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HcGuideClothRecordSaveReqVO reqVO) {
        HcGuideClothRecordDO existing = hcGuideClothRecordMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw invalidParamException("导布更换记录不存在");
        }
        validateReplaceTime(reqVO.getReplaceTime());
        HcGuideClothRecordDO updateObj = BeanUtils.toBean(reqVO, HcGuideClothRecordDO.class);
        // 产线与自动绑定的湿法设备是导布寿命流水的归属，不允许通过编辑历史记录变更。
        updateObj.setLineName(existing.getLineName());
        updateObj.setLineCode(existing.getLineCode());
        updateObj.setEquipmentId(existing.getEquipmentId());
        hcGuideClothRecordMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        validateExists(id);
        hcGuideClothRecordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        hcGuideClothRecordMapper.deleteByIds(ids);
    }

    @Override
    public HcGuideClothRecordDO get(Long id) {
        return hcGuideClothRecordMapper.selectById(id);
    }

    @Override
    public List<HcGuideClothRecordDO> getList(HcGuideClothRecordPageReqVO reqVO) {
        return hcGuideClothRecordMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcGuideClothRecordDO> getPage(HcGuideClothRecordPageReqVO reqVO) {
        return hcGuideClothRecordMapper.selectPage(reqVO);
    }

    @Override
    public List<HcGuideClothRecordImportExcelVO> buildExportList(HcGuideClothRecordPageReqVO reqVO) {
        return hcGuideClothRecordMapper.selectList(reqVO).stream()
                .map(this::toImportExcel)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcGuideClothRecordImportRespVO importExcel(MultipartFile file, Boolean confirmClear) throws IOException {
        if (!Boolean.TRUE.equals(confirmClear)) {
            throw invalidParamException("导入初始化前必须确认清空当前导布更换记录数据");
        }
        HcGuideClothRecordImportRespVO respVO = new HcGuideClothRecordImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }

        List<HcGuideClothRecordImportExcelVO> excelRows = ExcelUtils.read(file, HcGuideClothRecordImportExcelVO.class);
        List<GuideClothImportRow> rows = new ArrayList<>();
        Map<String, Integer> currentLineRows = new HashMap<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcGuideClothRecordImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            int failureCountBefore = respVO.getFailures().size();
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            GuideClothImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
            if (row == null || respVO.getFailures().size() > failureCountBefore) {
                continue;
            }
            if (CURRENT_FLAG_CURRENT == row.currentFlag) {
                Integer previousRowNo = currentLineRows.putIfAbsent(row.lineCode, rowNo);
                if (previousRowNo != null) {
                    addImportFailure(respVO, String.format("第%d行：产线编号 %s 存在多个当前记录，已在第%d行出现",
                            rowNo, row.lineCode, previousRowNo));
                    continue;
                }
            }
            rows.add(row);
        }
        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未清空也未写入任何数据");
            return respVO;
        }
        if (rows.isEmpty()) {
            addImportFailure(respVO, "导入文件没有有效数据行");
            return respVO;
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        respVO.setClearedCount(hcGuideClothRecordMapper.physicalDeleteByTenantId(tenantId));
        for (GuideClothImportRow row : rows) {
            HcGuideClothRecordDO record = HcGuideClothRecordDO.builder()
                    .tenantId(tenantId)
                    .lineName(row.lineName)
                    .lineCode(row.lineCode)
                    .replaceTime(row.replaceTime)
                    .replacePlanNo(row.replacePlanNo)
                    .petBatchNo(row.petBatchNo)
                    .guideClothBatchNo(row.guideClothBatchNo)
                    .petModel(row.petModel)
                    .replaceReason(row.replaceReason)
                    .useCount(row.useCount)
                    .currentFlag(row.currentFlag)
                    .remark(row.remark)
                    .build();
            hcGuideClothRecordMapper.insert(record);
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        respVO.getMessages().add(String.format("导入初始化完成：物理删除导布更换记录 %d 条，初始化记录 %d 条",
                respVO.getClearedCount(), respVO.getSuccessCount()));
        return respVO;
    }

    private HcGuideClothRecordImportExcelVO toImportExcel(HcGuideClothRecordDO record) {
        HcGuideClothRecordImportExcelVO excelVO = new HcGuideClothRecordImportExcelVO();
        excelVO.setLineName(record.getLineName());
        excelVO.setLineCode(record.getLineCode());
        excelVO.setReplaceTime(formatDateTime(record.getReplaceTime()));
        excelVO.setReplacePlanNo(record.getReplacePlanNo());
        excelVO.setPetBatchNo(record.getPetBatchNo());
        excelVO.setGuideClothBatchNo(record.getGuideClothBatchNo());
        excelVO.setPetModel(record.getPetModel());
        excelVO.setReplaceReason(record.getReplaceReason());
        excelVO.setUseCount(record.getUseCount());
        excelVO.setCurrentFlag(record.getCurrentFlag());
        excelVO.setCurrentFlagName(currentFlagName(record.getCurrentFlag()));
        excelVO.setRemark(record.getRemark());
        return excelVO;
    }

    private GuideClothImportRow normalizeImportRow(HcGuideClothRecordImportExcelVO excelRow,
                                                   int rowNo,
                                                   HcGuideClothRecordImportRespVO respVO) {
        String lineName = trimToNull(excelRow.getLineName());
        String lineCode = trimToNull(excelRow.getLineCode());
        if (isBlank(lineName)) {
            addImportFailure(respVO, String.format("第%d行：产线名不能为空", rowNo));
        }
        if (isBlank(lineCode)) {
            addImportFailure(respVO, String.format("第%d行：产线编号不能为空", rowNo));
        }
        Integer useCount = excelRow.getUseCount();
        if (useCount == null) {
            addImportFailure(respVO, String.format("第%d行：累计使用次数不能为空", rowNo));
        } else if (useCount < 0) {
            addImportFailure(respVO, String.format("第%d行：累计使用次数不能为负数", rowNo));
        }
        Integer currentFlag = normalizeCurrentFlag(excelRow.getCurrentFlag(), excelRow.getCurrentFlagName());
        if (currentFlag == null) {
            addImportFailure(respVO, String.format("第%d行：当前标记不能为空，仅支持 0/当前 或 1/历史", rowNo));
        }
        LocalDateTime replaceTime = parseDateTime(excelRow.getReplaceTime(), rowNo, "上次更换时间", respVO);
        if (!isValidBusinessTime(replaceTime)) {
            addImportFailure(respVO, String.format("第%d行：上次更换时间不能为空或早于2000年", rowNo));
        }
        if (isBlank(lineName) || isBlank(lineCode) || useCount == null || useCount < 0
                || currentFlag == null || !isValidBusinessTime(replaceTime)) {
            return null;
        }

        GuideClothImportRow row = new GuideClothImportRow();
        row.rowNo = rowNo;
        row.lineName = lineName;
        row.lineCode = lineCode;
        row.replaceTime = replaceTime;
        row.replacePlanNo = trimToNull(excelRow.getReplacePlanNo());
        row.petBatchNo = trimToNull(excelRow.getPetBatchNo());
        row.guideClothBatchNo = trimToNull(excelRow.getGuideClothBatchNo());
        row.petModel = trimToNull(excelRow.getPetModel());
        row.replaceReason = defaultText(trimToNull(excelRow.getReplaceReason()), IMPORT_INIT_MESSAGE);
        row.useCount = useCount;
        row.currentFlag = currentFlag;
        row.remark = trimToNull(excelRow.getRemark());
        return row;
    }

    private boolean isBlankImportRow(HcGuideClothRecordImportExcelVO row) {
        if (row == null) {
            return true;
        }
        return isBlank(row.getLineName())
                && isBlank(row.getLineCode())
                && isBlank(row.getReplacePlanNo())
                && isBlank(row.getPetBatchNo())
                && isBlank(row.getGuideClothBatchNo())
                && isBlank(row.getPetModel())
                && row.getUseCount() == null
                && row.getCurrentFlag() == null
                && isBlank(row.getCurrentFlagName());
    }

    private Integer normalizeCurrentFlag(Integer currentFlag, String currentFlagName) {
        if (currentFlag != null) {
            return currentFlag == CURRENT_FLAG_CURRENT || currentFlag == CURRENT_FLAG_HISTORY ? currentFlag : null;
        }
        String text = trimToNull(currentFlagName);
        if (text == null) {
            return null;
        }
        if ("当前".equals(text) || "current".equalsIgnoreCase(text)) {
            return CURRENT_FLAG_CURRENT;
        }
        if ("历史".equals(text) || "history".equalsIgnoreCase(text)) {
            return CURRENT_FLAG_HISTORY;
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value, int rowNo, String fieldName,
                                        HcGuideClothRecordImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace('/', '-').replace('T', ' ');
        List<DateTimeFormatter> dateTimeFormatters = List.of(
                DATE_TIME_FORMATTER,
                DATE_TIME_MINUTE_FORMATTER,
                DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"),
                DateTimeFormatter.ofPattern("yyyy-M-d H:m"));
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d")).atStartOfDay();
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd HH:mm:ss", rowNo, fieldName));
            return null;
        }
    }

    private String currentFlagName(Integer currentFlag) {
        if (currentFlag == null) {
            return null;
        }
        return CURRENT_FLAG_CURRENT == currentFlag ? "当前" : "历史";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private void addImportFailure(HcGuideClothRecordImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String trimToNull(String value) {
        String text = value == null ? null : value.trim();
        return isBlank(text) ? null : text;
    }

    @Override
    public HcGuideClothRuntimeRespVO getRuntimeByMotherModelCode(String motherModelCode) {
        return getRuntimeByMotherModelCode(motherModelCode, null);
    }

    @Override
    public HcGuideClothRuntimeRespVO getRuntimeByMotherModelCode(String motherModelCode, Long equipmentId) {
        HcProductionLineContext lineInfo = resolveGuideClothLineContext(motherModelCode);
        HcGuideClothRecordDO current = selectCurrentByLineContext(lineInfo);
        HcGuideClothRuntimeRespVO respVO = new HcGuideClothRuntimeRespVO();
        respVO.setLineCode(lineInfo.getLineCode());
        respVO.setLineName(lineInfo.getLineName());
        HcEquipmentConsumableStateDO state = equipmentId == null
                ? null
                : hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(equipmentId, PROCESS_CODE_WET, CONSUMABLE_TYPE_GUIDE_CLOTH);
        applyConsumableRuntime(respVO, state);
        if (current == null) {
            if (respVO.getCurrentUseCount() == null) {
                respVO.setCurrentUseCount(0);
            }
            if (respVO.getNextUseCount() == null) {
                respVO.setNextUseCount(1);
            }
            applyGuideClothCountRuntime(respVO);
            return respVO;
        }
        if (respVO.getCurrentUseCount() == null) {
            respVO.setCurrentUseCount(defaultCount(current.getUseCount()));
        }
        if (respVO.getNextUseCount() == null) {
            respVO.setNextUseCount(defaultCount(respVO.getCurrentUseCount()) + 1);
        }
        if (!isValidBusinessTime(respVO.getReplaceTime()) && isValidBusinessTime(current.getReplaceTime())) {
            respVO.setReplaceTime(current.getReplaceTime());
        }
        respVO.setReplacePlanNo(firstNotBlank(respVO.getReplacePlanNo(), current.getReplacePlanNo()));
        respVO.setPetBatchNo(current.getPetBatchNo());
        respVO.setGuideClothBatchNo(firstNotBlank(respVO.getGuideClothBatchNo(), current.getGuideClothBatchNo()));
        respVO.setPetModel(current.getPetModel());
        respVO.setReplaceReason(firstNotBlank(respVO.getReplaceReason(), current.getReplaceReason()));
        applyGuideClothCountRuntime(respVO);
        return respVO;
    }

    @Override
    public HcGuideClothRuntimeRespVO getRndRuntime(Long guideClothRecordId) {
        HcGuideClothRecordDO guideClothRecord = validateCurrentGuideClothRecord(guideClothRecordId);
        HcEquipmentDO equipment = resolveBoundWetEquipment(guideClothRecord);
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(
                equipment.getId(), PROCESS_CODE_WET, CONSUMABLE_TYPE_GUIDE_CLOTH);
        if (state == null) {
            throw invalidParamException("该设备尚未配置湿法导布当前状态，请先完成湿法报工或导布寿命初始化");
        }
        HcGuideClothRuntimeRespVO respVO = new HcGuideClothRuntimeRespVO();
        respVO.setLineCode(guideClothRecord.getLineCode());
        respVO.setLineName(guideClothRecord.getLineName());
        applyConsumableRuntime(respVO, state);
        if (respVO.getCurrentUseCount() == null) {
            respVO.setCurrentUseCount(defaultCount(guideClothRecord.getUseCount()));
        }
        if (respVO.getNextUseCount() == null) {
            respVO.setNextUseCount(defaultCount(respVO.getCurrentUseCount()) + 1);
        }
        if (!isValidBusinessTime(respVO.getReplaceTime()) && isValidBusinessTime(guideClothRecord.getReplaceTime())) {
            respVO.setReplaceTime(guideClothRecord.getReplaceTime());
        }
        respVO.setReplacePlanNo(firstNotBlank(respVO.getReplacePlanNo(), guideClothRecord.getReplacePlanNo()));
        respVO.setPetBatchNo(guideClothRecord.getPetBatchNo());
        respVO.setGuideClothBatchNo(firstNotBlank(respVO.getGuideClothBatchNo(), guideClothRecord.getGuideClothBatchNo()));
        respVO.setPetModel(guideClothRecord.getPetModel());
        respVO.setReplaceReason(firstNotBlank(respVO.getReplaceReason(), guideClothRecord.getReplaceReason()));
        applyGuideClothCountRuntime(respVO);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer handleWetReport(String motherModelCode, String planNo, String petBatchNo, String guideClothBatchNo,
                                   String petModel, String replaceReason, Integer submittedUseCount, boolean changed,
                                   String remark, Long tenantId, LocalDateTime replaceTime) {
        return handleWetReport(motherModelCode, planNo, petBatchNo, guideClothBatchNo, petModel, replaceReason,
                submittedUseCount, changed, remark, tenantId, replaceTime, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer handleWetReport(String motherModelCode, String planNo, String petBatchNo, String guideClothBatchNo,
                                   String petModel, String replaceReason, Integer submittedUseCount, boolean changed,
                                   String remark, Long tenantId, LocalDateTime replaceTime, Long equipmentId,
                                   String equipmentCode, String equipmentName, Long workCenterId, String workCenterCode,
                                   String workCenterName, Long planId, Long planOperationId, String operationCode,
                                   String operationName, BigDecimal receiveLength, Long reportId, Long operatorId,
                                   String operatorName) {
        HcProductionLineContext lineInfo = resolveGuideClothLineContext(motherModelCode);
        validateResolvedGuideClothLine(lineInfo, motherModelCode);
        HcGuideClothRecordDO current = selectCurrentByLineContext(lineInfo);
        LocalDateTime effectiveTime = replaceTime == null ? LocalDateTime.now() : replaceTime;
        Integer effectiveUseCount;
        Long guideClothRecordId;
        if (changed) {
            if (current != null) {
                HcGuideClothRecordDO historyObj = new HcGuideClothRecordDO();
                historyObj.setId(current.getId());
                historyObj.setCurrentFlag(CURRENT_FLAG_HISTORY);
                hcGuideClothRecordMapper.updateById(historyObj);
            }
            HcGuideClothRecordDO insertObj = HcGuideClothRecordDO.builder()
                    .tenantId(tenantId)
                    .lineName(lineInfo.getLineName())
                    .lineCode(lineInfo.getLineCode())
                    .equipmentId(equipmentId)
                    .replaceTime(effectiveTime)
                    .replacePlanNo(nullToEmpty(planNo))
                    .petBatchNo(nullToEmpty(petBatchNo))
                    .guideClothBatchNo(nullToEmpty(guideClothBatchNo))
                    .petModel(nullToEmpty(petModel))
                    .replaceReason(nullToEmpty(replaceReason))
                    .useCount(1)
                    .currentFlag(CURRENT_FLAG_CURRENT)
                    .remark(nullToEmpty(remark))
                    .build();
            hcGuideClothRecordMapper.insert(insertObj);
            effectiveUseCount = 1;
            guideClothRecordId = insertObj.getId();
        } else {
            effectiveUseCount = submittedUseCount != null
                    ? submittedUseCount
                    : (current == null ? 1 : defaultCount(current.getUseCount()) + 1);
            if (current == null) {
                HcEquipmentConsumableStateDO initializationState = requireWetGuideClothInitializationState(
                        equipmentId, guideClothBatchNo);
                HcGuideClothRecordDO insertObj = HcGuideClothRecordDO.builder()
                        .tenantId(tenantId)
                        .lineName(lineInfo.getLineName())
                        .lineCode(lineInfo.getLineCode())
                        .equipmentId(equipmentId)
                        .replaceTime(initializationState.getLastReplaceTime())
                        .replacePlanNo(nullToEmpty(initializationState.getLastReplacePlanNo()))
                        .petBatchNo(nullToEmpty(petBatchNo))
                        .guideClothBatchNo(firstNotBlank(initializationState.getBatchNo(), guideClothBatchNo, ""))
                        .petModel(nullToEmpty(petModel))
                        .replaceReason(nullToEmpty(initializationState.getLastReplaceReason()))
                        .useCount(effectiveUseCount)
                        .currentFlag(CURRENT_FLAG_CURRENT)
                        .remark(nullToEmpty(remark))
                        .build();
                hcGuideClothRecordMapper.insert(insertObj);
                guideClothRecordId = insertObj.getId();
            } else {
                bindGuideClothRecordEquipment(current, equipmentId);
                HcGuideClothRecordDO updateObj = new HcGuideClothRecordDO();
                updateObj.setId(current.getId());
                updateObj.setUseCount(effectiveUseCount);
                if (isBlank(current.getRemark()) && !isBlank(remark)) {
                    updateObj.setRemark(remark);
                }
                hcGuideClothRecordMapper.updateById(updateObj);
                guideClothRecordId = current.getId();
            }
        }
        handleWetConsumableState(guideClothRecordId, equipmentId, equipmentCode, equipmentName, workCenterId, workCenterCode,
                workCenterName, tenantId, planId, planNo, planOperationId, operationCode, operationName,
                guideClothBatchNo, petModel, petBatchNo, replaceReason, effectiveUseCount, changed, receiveLength, reportId,
                operatorId, operatorName, effectiveTime, remark);
        return effectiveUseCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveRndConsume(HcGuideClothRndConsumeSaveReqVO reqVO) {
        boolean guideClothChanged = Boolean.TRUE.equals(reqVO.getGuideClothChanged());
        HcToolingConsumableLedgerDO petLedger = validateRndConsumableLedger(
                reqVO.getPetLedgerId(), CONSUMABLE_TYPE_PET, "PET");
        reqVO.setPetBatchNo(petLedger.getBatchNo());
        reqVO.setPetModel(petLedger.getModel());
        if (guideClothChanged) {
            HcToolingConsumableLedgerDO guideClothLedger = validateRndConsumableLedger(
                    reqVO.getGuideClothLedgerId(), CONSUMABLE_TYPE_GUIDE_CLOTH, "导布");
            reqVO.setGuideClothNewBatchNo(guideClothLedger.getBatchNo());
        }
        HcGuideClothRecordDO guideClothRecord = validateCurrentGuideClothRecord(reqVO.getGuideClothRecordId());
        validateRndGuideClothPadType(guideClothRecord);
        HcEquipmentDO equipment = resolveBoundWetEquipment(guideClothRecord);
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(
                equipment.getId(), PROCESS_CODE_WET, CONSUMABLE_TYPE_GUIDE_CLOTH);
        if (state == null) {
            throw invalidParamException("该设备尚未配置湿法导布当前状态，请先完成湿法报工或导布寿命初始化");
        }
        LocalDateTime eventTime = reqVO.getConsumeTime();
        String newGuideClothBatchNo = trimToNull(reqVO.getGuideClothNewBatchNo());
        String guideClothReplaceReason = trimToNull(reqVO.getGuideClothReplaceReason());
        if (guideClothChanged) {
            validateRndConsumeEventTime(state, eventTime);
            if (newGuideClothBatchNo == null) {
                throw invalidParamException("选择更换导布后必须填写新导布批号");
            }
            if (guideClothReplaceReason == null) {
                throw invalidParamException("选择更换导布后必须填写更换原因");
            }
        } else {
            fillMissingWetGuideClothReplaceBaseline(guideClothRecord, state);
            validateRndConsumeState(state, eventTime);
        }

        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = defaultCount(state.getUseCount());
        BigDecimal beforeUsedLength = zeroIfNull(state.getUsedLength());
        BigDecimal outputLength = zeroIfNull(reqVO.getWetOutputMeter());
        Integer afterUseCount = guideClothChanged ? 1 : beforeUseCount + 1;
        BigDecimal afterUsedLength = guideClothChanged ? outputLength : beforeUsedLength.add(outputLength);
        BigDecimal limitLength = firstNonNull(state.getLimitLength(), DEFAULT_GUIDE_CLOTH_LIMIT_LENGTH);
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        String recordGroupNo = "RND-" + UUID.randomUUID();

        if (guideClothChanged) {
            HcGuideClothRecordDO historyObj = new HcGuideClothRecordDO();
            historyObj.setId(guideClothRecord.getId());
            historyObj.setCurrentFlag(CURRENT_FLAG_HISTORY);
            hcGuideClothRecordMapper.updateById(historyObj);
            HcGuideClothRecordDO newCurrentRecord = HcGuideClothRecordDO.builder()
                    .tenantId(firstNonNull(guideClothRecord.getTenantId(), state.getTenantId(), equipment.getTenantId()))
                    .lineName(guideClothRecord.getLineName())
                    .lineCode(guideClothRecord.getLineCode())
                    .equipmentId(equipment.getId())
                    .replaceTime(eventTime)
                    .replacePlanNo("")
                    .petBatchNo(reqVO.getPetBatchNo().trim())
                    .guideClothBatchNo(newGuideClothBatchNo)
                    .petModel(reqVO.getPetModel().trim())
                    .replaceReason(guideClothReplaceReason)
                    .useCount(afterUseCount)
                    .currentFlag(CURRENT_FLAG_CURRENT)
                    .remark(reqVO.getRemark().trim())
                    .build();
            hcGuideClothRecordMapper.insert(newCurrentRecord);
            guideClothRecord = newCurrentRecord;
        }

        state.setBatchNo(guideClothChanged ? newGuideClothBatchNo : state.getBatchNo());
        state.setLastReplaceTime(guideClothChanged ? eventTime : state.getLastReplaceTime());
        state.setLastReplacePlanNo(guideClothChanged ? "" : state.getLastReplacePlanNo());
        state.setLastReplaceReason(guideClothChanged ? guideClothReplaceReason : state.getLastReplaceReason());
        state.setUseCount(afterUseCount);
        state.setUsedLength(afterUsedLength);
        state.setLimitLength(limitLength);
        state.setWarningFlag(calcWarningFlag(afterUsedLength, limitLength));
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark().trim());
        hcEquipmentConsumableStateMapper.updateById(state);

        if (!guideClothChanged) {
            HcGuideClothRecordDO recordUpdate = new HcGuideClothRecordDO();
            recordUpdate.setId(guideClothRecord.getId());
            recordUpdate.setUseCount(afterUseCount);
            hcGuideClothRecordMapper.updateById(recordUpdate);
        } else {
            hcEquipmentConsumableEventMapper.insert(buildRndConsumeEvent(reqVO, state, guideClothRecord.getId(),
                    recordGroupNo, EVENT_TYPE_REPLACE, beforeBatchNo, state.getBatchNo(), beforeUseCount, 0,
                    beforeUsedLength, BigDecimal.ZERO, null, BigDecimal.ZERO, guideClothReplaceReason,
                    operatorId, operatorName, eventTime));
        }

        hcEquipmentConsumableEventMapper.insert(buildRndConsumeEvent(reqVO, state, guideClothRecord.getId(),
                recordGroupNo, EVENT_TYPE_RND_MANUAL_USE, guideClothChanged ? state.getBatchNo() : beforeBatchNo,
                state.getBatchNo(), guideClothChanged ? 0 : beforeUseCount, afterUseCount,
                guideClothChanged ? BigDecimal.ZERO : beforeUsedLength, afterUsedLength, 1, outputLength,
                guideClothChanged ? guideClothReplaceReason : "研发样品导布消耗", operatorId, operatorName, eventTime));
        return recordGroupNo;
    }

    /**
     * 研发消耗只能使用湿法工序仍在用的耗材领用台账，不能通过修改请求体绕过前端选择器。
     */
    private HcToolingConsumableLedgerDO validateRndConsumableLedger(Long ledgerId, String consumableType,
                                                                      String consumableName) {
        if (ledgerId == null) {
            throw invalidParamException("请选择湿法" + consumableName + "耗材领用台账");
        }
        HcToolingConsumableLedgerDO ledger = hcToolingConsumableLedgerMapper.selectByIdForUpdate(ledgerId);
        if (ledger == null) {
            throw invalidParamException("所选湿法" + consumableName + "耗材领用台账不存在或已删除");
        }
        if (!PROCESS_CODE_WET.equalsIgnoreCase(nullToEmpty(ledger.getProcessCode()))
                || !consumableType.equalsIgnoreCase(nullToEmpty(ledger.getConsumableType()))) {
            throw invalidParamException("请选择湿法" + consumableName + "耗材领用台账中的批次");
        }
        if (!LEDGER_USAGE_STATUS_ACTIVE.equalsIgnoreCase(nullToEmpty(ledger.getUsageStatus()))) {
            throw invalidParamException("所选湿法" + consumableName + "耗材领用台账已标记完成，无法选择");
        }
        if (trimToNull(ledger.getBatchNo()) == null) {
            throw invalidParamException("所选湿法" + consumableName + "耗材领用台账缺少批号");
        }
        if (CONSUMABLE_TYPE_PET.equals(consumableType) && trimToNull(ledger.getModel()) == null) {
            throw invalidParamException("所选湿法PET耗材领用台账缺少型号");
        }
        return ledger;
    }

    private HcEquipmentConsumableEventDO buildRndConsumeEvent(HcGuideClothRndConsumeSaveReqVO reqVO,
                                                                HcEquipmentConsumableStateDO state,
                                                                Long guideClothRecordId,
                                                                String recordGroupNo,
                                                                String eventType,
                                                                String beforeBatchNo,
                                                                String afterBatchNo,
                                                                Integer beforeUseCount,
                                                                Integer afterUseCount,
                                                                BigDecimal beforeUsedLength,
                                                                BigDecimal afterUsedLength,
                                                                Integer changeUseCount,
                                                                BigDecimal changeLength,
                                                                String replaceReason,
                                                                Long operatorId,
                                                                String operatorName,
                                                                LocalDateTime eventTime) {
        boolean rndManualUse = EVENT_TYPE_RND_MANUAL_USE.equals(eventType);
        return HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(PROCESS_CODE_WET)
                .processName(PROCESS_NAME_WET)
                .consumableType(CONSUMABLE_TYPE_GUIDE_CLOTH)
                .eventType(eventType)
                .recordSource(RECORD_SOURCE_RND_MANUAL)
                .guideClothRecordId(guideClothRecordId)
                .bizType(BIZ_TYPE_RND_MANUAL)
                .recordGroupNo(recordGroupNo)
                .productModelCode(reqVO.getProductModelCode().trim())
                .productMaterialCode(reqVO.getProductMaterialCode().trim())
                .productBatchNo(reqVO.getProductBatchNo().trim())
                .petModel(reqVO.getPetModel().trim())
                .petBatchNo(reqVO.getPetBatchNo().trim())
                .wetInputKg(rndManualUse ? reqVO.getWetInputKg() : null)
                .wetOutputMeter(rndManualUse ? zeroIfNull(reqVO.getWetOutputMeter()) : null)
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(afterBatchNo)
                .beforeUseCount(beforeUseCount)
                .afterUseCount(afterUseCount)
                .changeUseCount(changeUseCount)
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(afterUsedLength)
                .changeLength(changeLength)
                .replaceReason(replaceReason)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark().trim())
                .tenantId(state.getTenantId())
                .build();
    }

    private HcGuideClothRecordDO validateCurrentGuideClothRecord(Long guideClothRecordId) {
        HcGuideClothRecordDO guideClothRecord = hcGuideClothRecordMapper.selectById(guideClothRecordId);
        if (guideClothRecord == null || !Integer.valueOf(CURRENT_FLAG_CURRENT).equals(guideClothRecord.getCurrentFlag())) {
            throw invalidParamException("请选择有效的当前导布记录进行研发消耗登记");
        }
        return guideClothRecord;
    }

    private void validateRndGuideClothPadType(HcGuideClothRecordDO guideClothRecord) {
        String padType = padTypeResolver.resolveByGuideClothLineCode(guideClothRecord.getLineCode());
        if (padType == null) {
            throw invalidParamException("当前导布记录未归属黑垫线或白垫线，禁止登记研发消耗");
        }
    }

    private void bindGuideClothRecordEquipment(HcGuideClothRecordDO guideClothRecord, Long equipmentId) {
        if (guideClothRecord == null || equipmentId == null) {
            return;
        }
        if (guideClothRecord.getEquipmentId() != null
                && !guideClothRecord.getEquipmentId().equals(equipmentId)) {
            throw invalidParamException("当前导布记录已绑定其他湿法设备，不能跨设备累计导布寿命");
        }
        if (guideClothRecord.getEquipmentId() == null) {
            HcGuideClothRecordDO updateObj = new HcGuideClothRecordDO();
            updateObj.setId(guideClothRecord.getId());
            updateObj.setEquipmentId(equipmentId);
            hcGuideClothRecordMapper.updateById(updateObj);
            guideClothRecord.setEquipmentId(equipmentId);
        }
    }

    private HcEquipmentDO resolveBoundWetEquipment(HcGuideClothRecordDO guideClothRecord) {
        Long equipmentId = guideClothRecord.getEquipmentId();
        if (equipmentId == null) {
            HcEquipmentConsumableEventDO latestEvent = hcEquipmentConsumableEventMapper
                    .selectLatestByGuideClothRecordId(guideClothRecord.getId(), PROCESS_CODE_WET,
                            CONSUMABLE_TYPE_GUIDE_CLOTH);
            equipmentId = latestEvent == null ? null : latestEvent.getEquipmentId();
        }
        if (equipmentId == null) {
            List<HcEquipmentConsumableStateDO> matchingStates = hcEquipmentConsumableStateMapper
                    .selectListByBatchNoAndType(guideClothRecord.getGuideClothBatchNo(), PROCESS_CODE_WET,
                            CONSUMABLE_TYPE_GUIDE_CLOTH);
            if (matchingStates.size() == 1) {
                equipmentId = matchingStates.get(0).getEquipmentId();
            }
        }
        if (equipmentId == null) {
            throw invalidParamException("当前导布记录未绑定唯一的湿法设备，请先完成湿法报工或执行导布设备绑定迁移");
        }
        bindGuideClothRecordEquipment(guideClothRecord, equipmentId);
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw invalidParamException("当前导布记录绑定的湿法设备不存在");
        }
        return equipment;
    }

    private void fillMissingWetGuideClothReplaceBaseline(HcGuideClothRecordDO guideClothRecord,
                                                          HcEquipmentConsumableStateDO state) {
        if (isValidBusinessTime(state.getLastReplaceTime()) || !isValidBusinessTime(guideClothRecord.getReplaceTime())) {
            return;
        }
        String guideClothBatchNo = trimToNull(guideClothRecord.getGuideClothBatchNo());
        String stateBatchNo = trimToNull(state.getBatchNo());
        if (guideClothBatchNo != null && stateBatchNo != null && !guideClothBatchNo.equals(stateBatchNo)) {
            throw invalidParamException("当前导布记录与湿法设备的导布批号不一致，请先确认导布更换记录");
        }
        state.setBatchNo(firstNotBlank(stateBatchNo, guideClothBatchNo));
        state.setLastReplaceTime(guideClothRecord.getReplaceTime());
        state.setLastReplacePlanNo(firstNotBlank(state.getLastReplacePlanNo(), guideClothRecord.getReplacePlanNo()));
        state.setLastReplaceReason(firstNotBlank(state.getLastReplaceReason(), guideClothRecord.getReplaceReason()));
        hcEquipmentConsumableStateMapper.updateById(state);
    }

    private void applyGuideClothCountRuntime(HcGuideClothRuntimeRespVO respVO) {
        LocalDateTime replaceTime = isValidBusinessTime(respVO.getReplaceTime())
                ? respVO.getReplaceTime() : null;
        respVO.setReplaceTime(replaceTime);
        int nextUseCount = defaultCount(respVO.getNextUseCount());
        if (nextUseCount >= GUIDE_CLOTH_MAX_USE_COUNT) {
            respVO.setWarningFlag(1);
            respVO.setWarningText("导布累计次数将达到 " + nextUseCount + "/"
                    + GUIDE_CLOTH_MAX_USE_COUNT + "，请更换导布");
            return;
        }
        if (replaceTime == null) {
            respVO.setWarningFlag(1);
            respVO.setWarningText("未记录有效的导布更换时间，请先登记导布更换");
            return;
        }
        if (isMonthlyReplacementReminderDue(replaceTime, LocalDate.now())) {
            respVO.setWarningFlag(1);
            respVO.setWarningText("导布每月需更换一次，上次更换时间："
                    + formatReplaceTime(replaceTime) + "，下次更换日期："
                    + formatReplaceDate(nextMonthlyReplaceDate(replaceTime)) + "，请更换导布");
            return;
        }
        respVO.setWarningFlag(0);
        respVO.setWarningText("导布正常，预计累计使用 " + nextUseCount + "/"
                + GUIDE_CLOTH_MAX_USE_COUNT + " 次");
    }

    private boolean isMonthlyReplacementReminderDue(LocalDateTime replaceTime, LocalDate effectiveDate) {
        if (!isValidBusinessTime(replaceTime)) {
            return true;
        }
        LocalDate reminderStartDate = nextMonthlyReplaceDate(replaceTime)
                .minusDays(GUIDE_CLOTH_MONTH_REMIND_BEFORE_DAYS);
        LocalDate checkedDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
        return !checkedDate.isBefore(reminderStartDate);
    }

    private LocalDate nextMonthlyReplaceDate(LocalDateTime replaceTime) {
        return isValidBusinessTime(replaceTime) ? replaceTime.toLocalDate().plusMonths(1) : null;
    }

    private String formatReplaceTime(LocalDateTime replaceTime) {
        return isValidBusinessTime(replaceTime) ? replaceTime.format(DATE_TIME_FORMATTER) : "未记录";
    }

    private String formatReplaceDate(LocalDate replaceDate) {
        return replaceDate == null ? "未记录" : replaceDate.toString();
    }

    private void applyConsumableRuntime(HcGuideClothRuntimeRespVO respVO, HcEquipmentConsumableStateDO state) {
        BigDecimal currentUsedLength = state == null ? BigDecimal.ZERO : zeroIfNull(state.getUsedLength());
        BigDecimal limitLength = state == null ? DEFAULT_GUIDE_CLOTH_LIMIT_LENGTH
                : firstNonNull(state.getLimitLength(), DEFAULT_GUIDE_CLOTH_LIMIT_LENGTH);
        respVO.setConsumableStateId(state == null ? null : state.getId());
        respVO.setGuideClothBatchNo(state == null ? null : state.getBatchNo());
        respVO.setCurrentUsedLength(currentUsedLength);
        respVO.setNextUsedLength(currentUsedLength);
        respVO.setLimitLength(limitLength);
        respVO.setWarningFlag(state == null ? calcWarningFlag(currentUsedLength, limitLength) : calcWarningFlag(state));
        respVO.setWarningText(buildWarningText(respVO.getWarningFlag(), currentUsedLength, limitLength));
        if (state != null) {
            int currentUseCount = defaultCount(state.getUseCount());
            respVO.setCurrentUseCount(currentUseCount);
            respVO.setNextUseCount(currentUseCount + 1);
            respVO.setReplaceTime(isValidBusinessTime(state.getLastReplaceTime())
                    ? state.getLastReplaceTime() : null);
            respVO.setReplacePlanNo(firstNotBlank(respVO.getReplacePlanNo(), state.getLastReplacePlanNo()));
            respVO.setReplaceReason(firstNotBlank(respVO.getReplaceReason(), state.getLastReplaceReason()));
        }
    }

    private void handleWetConsumableState(Long guideClothRecordId, Long equipmentId, String equipmentCode, String equipmentName,
                                          Long workCenterId, String workCenterCode, String workCenterName,
                                          Long tenantId, Long planId, String planNo, Long planOperationId,
                                          String operationCode, String operationName, String guideClothBatchNo,
                                          String petModel, String petBatchNo, String replaceReason,
                                          Integer effectiveUseCount, boolean changed,
                                          BigDecimal receiveLength, Long reportId, Long operatorId,
                                          String operatorName, LocalDateTime eventTime, String remark) {
        if (equipmentId == null) {
            return;
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return;
        }
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(
                equipmentId, PROCESS_CODE_WET, CONSUMABLE_TYPE_GUIDE_CLOTH);
        boolean newState = state == null;
        String beforeBatchNo = state == null ? null : state.getBatchNo();
        Integer beforeUseCount = state == null ? null : state.getUseCount();
        BigDecimal beforeUsedLength = state == null ? null : state.getUsedLength();
        if (newState) {
            state = HcEquipmentConsumableStateDO.builder()
                    .equipmentId(equipment.getId())
                    .equipmentCode(equipment.getEquipmentCode())
                    .equipmentName(equipment.getEquipmentName())
                    .tenantId(firstNonNull(tenantId, equipment.getTenantId()))
                    .build();
        }

        BigDecimal usedChangeLength = changed ? BigDecimal.ZERO : zeroIfNull(receiveLength);
        BigDecimal afterUsedLength = changed ? BigDecimal.ZERO : zeroIfNull(beforeUsedLength).add(usedChangeLength);
        BigDecimal limitLength = firstNonNull(state.getLimitLength(), DEFAULT_GUIDE_CLOTH_LIMIT_LENGTH);
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(firstNotBlank(equipmentCode, equipment.getEquipmentCode()));
        state.setEquipmentName(firstNotBlank(equipmentName, equipment.getEquipmentName()));
        state.setWorkCenterId(firstNonNull(workCenterId, equipment.getWorkCenterId()));
        state.setWorkCenterCode(firstNotBlank(workCenterCode, equipment.getWorkCenterCode()));
        state.setWorkCenterName(firstNotBlank(workCenterName, equipment.getWorkCenterName()));
        state.setProcessCode(PROCESS_CODE_WET);
        state.setProcessName(firstNotBlank(operationName, PROCESS_NAME_WET));
        state.setConsumableType(CONSUMABLE_TYPE_GUIDE_CLOTH);
        state.setBatchNo(firstNotBlank(guideClothBatchNo, state.getBatchNo()));
        state.setLastReplaceTime(changed ? eventTime : state.getLastReplaceTime());
        state.setLastReplacePlanNo(changed ? nullToEmpty(planNo) : state.getLastReplacePlanNo());
        state.setLastReplaceReason(changed ? nullToEmpty(replaceReason) : state.getLastReplaceReason());
        state.setUseCount(effectiveUseCount);
        state.setUsedLength(afterUsedLength);
        state.setLimitLength(limitLength);
        state.setWarningFlag(calcWarningFlag(afterUsedLength, limitLength));
        state.setStatus(STATUS_IN_USE);
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(firstNotBlank(operatorName, "系统"));
        state.setLastEventTime(eventTime);
        state.setRemark(remark);
        state.setTenantId(firstNonNull(state.getTenantId(), tenantId, equipment.getTenantId()));
        if (newState) {
            hcEquipmentConsumableStateMapper.insert(state);
        } else {
            hcEquipmentConsumableStateMapper.updateById(state);
        }

        HcEquipmentConsumableEventDO event = HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(state.getProcessCode())
                .processName(state.getProcessName())
                .consumableType(state.getConsumableType())
                .eventType(changed ? EVENT_TYPE_REPLACE : EVENT_TYPE_USE)
                .guideClothRecordId(guideClothRecordId)
                .petModel(nullToEmpty(petModel))
                .petBatchNo(nullToEmpty(petBatchNo))
                .planId(planId)
                .planNo(planNo)
                .planOperationId(planOperationId)
                .operationCode(operationCode)
                .operationName(operationName)
                .bizType("WET_REPORT")
                .bizId(reportId)
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(changed ? null : diff(state.getUseCount(), beforeUseCount))
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(usedChangeLength)
                .replaceReason(replaceReason)
                .operatorId(operatorId)
                .operatorName(firstNotBlank(operatorName, "系统"))
                .eventTime(eventTime)
                .remark(remark)
                .tenantId(state.getTenantId())
                .build();
        hcEquipmentConsumableEventMapper.insert(event);
    }

    private void validateRndConsumeState(HcEquipmentConsumableStateDO state, LocalDateTime eventTime) {
        validateRndConsumeEventTime(state, eventTime);
        if (!isValidBusinessTime(state.getLastReplaceTime())) {
            throw invalidParamException("未记录有效的导布更换时间，请先登记导布更换");
        }
        if (isMonthlyReplacementReminderDue(state.getLastReplaceTime(), eventTime.toLocalDate())) {
            throw invalidParamException("导布已到每月更换提醒期，请先更换导布后再登记研发消耗");
        }
        if (defaultCount(state.getUseCount()) + 1 >= GUIDE_CLOTH_MAX_USE_COUNT) {
            throw invalidParamException("导布累计使用次数将达到 " + GUIDE_CLOTH_MAX_USE_COUNT + " 次，请先更换导布");
        }
    }

    private void validateRndConsumeEventTime(HcEquipmentConsumableStateDO state, LocalDateTime eventTime) {
        if (!STATUS_IN_USE.equalsIgnoreCase(state.getStatus())) {
            throw invalidParamException("该湿法导布当前状态不可用，请先完成导布更换或状态配置");
        }
        if (!isValidBusinessTime(eventTime)) {
            throw invalidParamException("研发消耗时间不能为空或早于2000年");
        }
        if (eventTime.isAfter(LocalDateTime.now())) {
            throw invalidParamException("研发消耗时间不能晚于当前时间");
        }
        if (isValidBusinessTime(state.getLastEventTime()) && eventTime.isBefore(state.getLastEventTime())) {
            throw invalidParamException("研发消耗时间不能早于该设备最近一次导布流水时间");
        }
    }

    private void validateExists(Long id) {
        if (id == null || hcGuideClothRecordMapper.selectById(id) == null) {
            throw invalidParamException("导布更换记录不存在");
        }
    }

    private int defaultCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private String defaultText(String text, String defaultValue) {
        return isBlank(text) ? defaultValue : text;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer calcWarningFlag(HcEquipmentConsumableStateDO state) {
        if (state == null) {
            return 0;
        }
        return calcWarningFlag(state.getUsedLength(), state.getLimitLength());
    }

    private Integer calcWarningFlag(BigDecimal usedLength, BigDecimal limitLength) {
        return limitLength != null
                && usedLength != null
                && usedLength.compareTo(limitLength) >= 0 ? 1 : 0;
    }

    private String buildWarningText(Integer warningFlag, BigDecimal usedLength, BigDecimal limitLength) {
        if (warningFlag != null && warningFlag == 1) {
            return "导布累计使用长度已达到或超过 " + limitLength.stripTrailingZeros().toPlainString() + "m，请更换导布";
        }
        return "正常";
    }

    private Integer diff(Integer after, Integer before) {
        if (after == null && before == null) {
            return null;
        }
        return (after == null ? 0 : after) - (before == null ? 0 : before);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
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
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private HcGuideClothRecordDO selectCurrentByLineContext(HcProductionLineContext lineInfo) {
        List<String> lineCodes = new ArrayList<>();
        if (lineInfo != null) {
            lineCodes.add(lineInfo.getLineCode());
            lineCodes.add(lineInfo.getLineShortCode());
        }
        return hcGuideClothRecordMapper.selectCurrentByLineCodes(lineCodes);
    }

    /**
     * 导布业务产线优先以产品型号主数据的垫型分类为准，型号前缀只兼容历史数据。
     */
    private HcProductionLineContext resolveGuideClothLineContext(String motherModelCode) {
        String padType = padTypeResolver.resolveByModelCode(motherModelCode);
        if (HcProductionRecordPadTypeResolver.BLACK_PAD.equals(padType)) {
            return HcProductionLineContext.builder()
                    .lineCode(LINE_CODE_BLACK)
                    .lineName("黑垫线")
                    .lineShortCode("B")
                    .batchLineCode("B")
                    .build();
        }
        if (HcProductionRecordPadTypeResolver.WHITE_PAD.equals(padType)) {
            return HcProductionLineContext.builder()
                    .lineCode(LINE_CODE_WHITE)
                    .lineName("白垫线")
                    .lineShortCode("W")
                    .batchLineCode("A")
                    .build();
        }
        return hcProductionLineResolverService.resolveByMotherModelCode(motherModelCode);
    }

    private void validateResolvedGuideClothLine(HcProductionLineContext lineInfo, String motherModelCode) {
        String lineCode = lineInfo == null ? null : trimToNull(lineInfo.getLineCode());
        if (!LINE_CODE_WHITE.equalsIgnoreCase(nullToEmpty(lineCode))
                && !LINE_CODE_BLACK.equalsIgnoreCase(nullToEmpty(lineCode))) {
            throw invalidParamException("产品型号 " + defaultText(trimToNull(motherModelCode), "-")
                    + " 未维护有效的黑垫/白垫分类，禁止生成未知产线导布记录");
        }
    }

    /**
     * 未发生物理更换时，首次导布当前记录只能继承同设备耗材状态的真实更换基线。
     * 禁止把本次湿法完工时间当成导布更换时间。
     */
    private HcEquipmentConsumableStateDO requireWetGuideClothInitializationState(Long equipmentId,
                                                                                   String guideClothBatchNo) {
        if (equipmentId == null) {
            throw invalidParamException("当前湿法任务未绑定设备，无法取得导布真实更换基线");
        }
        HcEquipmentConsumableStateDO state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(
                equipmentId, PROCESS_CODE_WET, CONSUMABLE_TYPE_GUIDE_CLOTH);
        if (state == null || !isValidBusinessTime(state.getLastReplaceTime())) {
            throw invalidParamException("当前设备未记录有效的导布更换时间，请先初始化导布寿命；"
                    + "系统不会使用湿法完工时间代替更换时间");
        }
        String stateBatchNo = trimToNull(state.getBatchNo());
        String submittedBatchNo = trimToNull(guideClothBatchNo);
        if (stateBatchNo == null) {
            throw invalidParamException("当前设备导布状态缺少批号，请先初始化导布寿命");
        }
        if (submittedBatchNo != null && !stateBatchNo.equalsIgnoreCase(submittedBatchNo)) {
            throw invalidParamException("报工导布批号 " + submittedBatchNo + " 与设备当前导布批号 "
                    + stateBatchNo + " 不一致，禁止自动初始化导布记录");
        }
        return state;
    }

    private void validateReplaceTime(LocalDateTime replaceTime) {
        if (!isValidBusinessTime(replaceTime)) {
            throw invalidParamException("上次更换时间不能为空或无效，请重新选择");
        }
    }

    private boolean isValidBusinessTime(LocalDateTime value) {
        return value != null && value.getYear() >= MIN_VALID_BUSINESS_YEAR;
    }

    private static class GuideClothImportRow {
        private int rowNo;
        private String lineName;
        private String lineCode;
        private LocalDateTime replaceTime;
        private String replacePlanNo;
        private String petBatchNo;
        private String guideClothBatchNo;
        private String petModel;
        private String replaceReason;
        private Integer useCount;
        private Integer currentFlag;
        private String remark;
    }
}
