package cn.iocoder.yudao.module.mes.service.hc.pressslotspare;

import cn.iocoder.yudao.module.mes.service.hc.processreport.HcPressSlotRndPadType;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSparePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotSpareRecordMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcPressSlotSpareServiceImpl implements HcPressSlotSpareService {

    private static final String SPARE_PRESS_ROLLER = "PRESS_ROLLER";
    private static final String SPARE_BEARING = "BEARING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final String EVENT_CONFIG = "CONFIG";
    private static final String EVENT_RND_MANUAL_USE = "RND_MANUAL_USE";
    private static final String RECORD_SOURCE_RND_MANUAL = "RND_MANUAL";
    private static final String BIZ_TYPE_RND_MANUAL = "PRESS_SLOT_RND_MANUAL";
    private static final String PROD_TYPE_RND_TRIAL = "RND_TRIAL";
    private static final String PRESS_SLOT_OPERATION_CODE = "PRESS_SLOT";
    private static final String PRESS_SLOT_OPERATION_NAME = "压槽";
    private static final String IMPORT_INIT_REASON = "Excel导入初始化";
    private static final int ROLLER_LIMIT_DAYS = 60;
    private static final int ROLLER_LIMIT_COUNT = 2000;
    private static final int BEARING_LIMIT_DAYS = 150;
    private static final int BEARING_LIMIT_COUNT = 5000;
    private static final int MIN_VALID_BUSINESS_YEAR = 2000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcEquipmentMapper hcEquipmentMapper;

    @Resource
    private HcPressSlotSpareMapper hcPressSlotSpareMapper;

    @Resource
    private HcPressSlotSpareRecordMapper hcPressSlotSpareRecordMapper;

    @Override
    public PageResult<HcPressSlotSpareDO> getPage(HcPressSlotSparePageReqVO reqVO) {
        return hcPressSlotSpareMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<HcPressSlotSpareRecordDO> getRecordPage(HcPressSlotSpareRecordPageReqVO reqVO) {
        return hcPressSlotSpareRecordMapper.selectPage(reqVO);
    }

    @Override
    public HcPressSlotSpareDO get(Long id) {
        return hcPressSlotSpareMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(HcPressSlotSpareSaveReqVO reqVO) {
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("设备不存在");
        }
        String type = normalizeSpareType(reqVO.getSpareType());
        HcPressSlotSpareDO existed = reqVO.getId() == null ? null : hcPressSlotSpareMapper.selectById(reqVO.getId());
        if (existed == null) {
            existed = hcPressSlotSpareMapper.selectOneByEquipmentAndType(equipment.getId(), type);
        }
        boolean isNew = existed == null;
        String beforeMaterialCode = existed == null ? null : existed.getMaterialCode();
        String beforeBatchNo = existed == null ? null : existed.getBatchNo();
        Integer beforeUseCount = existed == null ? null : existed.getUseCount();
        BigDecimal beforeAvailableQuantity = existed == null ? BigDecimal.ZERO : zeroIfNull(existed.getAvailableQuantity());

        LocalDateTime eventTime = LocalDateTime.now();
        LocalDateTime lastReplaceTime = resolveBusinessDateTime(reqVO.getLastReplaceTime(),
                isNew ? null : existed.getLastReplaceTime(), eventTime);
        LocalDateTime lastCleanTime = resolveBusinessDateTime(reqVO.getLastCleanTime(),
                isNew ? null : existed.getLastCleanTime(), null);
        Long operatorId = reqVO.getOperatorId() != null ? reqVO.getOperatorId() : SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        HcPressSlotSpareDO state = isNew ? new HcPressSlotSpareDO() : existed;
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(equipment.getEquipmentCode());
        state.setEquipmentName(equipment.getEquipmentName());
        state.setWorkCenterId(firstNonNull(reqVO.getWorkCenterId(), equipment.getWorkCenterId()));
        state.setWorkCenterCode(firstNotBlank(reqVO.getWorkCenterCode(), equipment.getWorkCenterCode()));
        state.setWorkCenterName(firstNotBlank(reqVO.getWorkCenterName(), equipment.getWorkCenterName()));
        state.setSpareType(type);
        state.setMaterialCode(reqVO.getMaterialCode());
        state.setMaterialName(reqVO.getMaterialName());
        state.setBatchNo(reqVO.getBatchNo());
        state.setOnlineQuantity(firstNonNull(reqVO.getOnlineQuantity(), BigDecimal.ONE));
        state.setAvailableQuantity(firstNonNull(reqVO.getAvailableQuantity(), SPARE_PRESS_ROLLER.equals(type) ? BigDecimal.ONE : BigDecimal.ZERO));
        state.setLastReplaceTime(lastReplaceTime);
        state.setLastReplacePlanNo(null);
        state.setLastReplaceReason(reqVO.getReplaceReason());
        state.setLastCleanTime(lastCleanTime);
        state.setLastCleanRemark(reqVO.getLastCleanRemark());
        state.setUseCount(reqVO.getUseCount() == null ? 0 : reqVO.getUseCount());
        state.setLimitCount(reqVO.getLimitCount() == null ? defaultLimitCount(type) : reqVO.getLimitCount());
        state.setLimitDays(reqVO.getLimitDays() == null ? defaultLimitDays(type) : reqVO.getLimitDays());
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ACTIVE));
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark());
        state.setTenantId(equipment.getTenantId());
        if (isNew) {
            hcPressSlotSpareMapper.insert(state);
        } else {
            int updated = hcPressSlotSpareMapper.updateCurrentStateById(state);
            if (updated == 0) {
                throw invalidParamException("压槽备件不存在或已被删除");
            }
        }

        BigDecimal afterAvailableQuantity = zeroIfNull(state.getAvailableQuantity());
        hcPressSlotSpareRecordMapper.insert(HcPressSlotSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(type)
                .eventType(EVENT_CONFIG)
                .beforeMaterialCode(beforeMaterialCode)
                .beforeBatchNo(beforeBatchNo)
                .afterMaterialCode(state.getMaterialCode())
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(null)
                .beforeAvailableQuantity(beforeAvailableQuantity)
                .afterAvailableQuantity(afterAvailableQuantity)
                .changeQuantity(afterAvailableQuantity.subtract(beforeAvailableQuantity))
                .onlineQuantity(state.getOnlineQuantity())
                .offlineQuantity(BigDecimal.ZERO)
                .finalUseCount(state.getUseCount())
                .replaceReason(reqVO.getReplaceReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark())
                .tenantId(state.getTenantId())
                .build());
        return state.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveRndConsume(HcPressSlotSpareRndConsumeSaveReqVO reqVO) {
        if (reqVO.getPressSlotOutputPcs() > reqVO.getPressSlotInputPcs()) {
            throw invalidParamException("压槽产出片数不能大于投入片数");
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("压槽设备不存在");
        }
        String padType = HcPressSlotRndPadType.forEquipment(equipment.getApplicablePadType(), equipment.getEquipmentName());
        if (padType == null) {
            throw invalidParamException("请核对压槽设备适用垫型：必须为白垫或黑垫，且不能与设备名称冲突");
        }
        HcPressSlotSpareDO roller = hcPressSlotSpareMapper.selectOneByEquipmentAndType(equipment.getId(), SPARE_PRESS_ROLLER);
        HcPressSlotSpareDO bearing = hcPressSlotSpareMapper.selectOneByEquipmentAndType(equipment.getId(), SPARE_BEARING);
        if (roller == null || bearing == null) {
            throw invalidParamException("该设备必须先完成压槽辊和轴承的当前状态配置后，才能登记研发样品消耗");
        }
        LocalDateTime eventTime = reqVO.getConsumeTime();
        if (eventTime.isAfter(LocalDateTime.now())) {
            throw invalidParamException("研发消耗时间不能晚于当前时间");
        }
        validateRndConsumeState(roller, eventTime);
        validateRndConsumeState(bearing, eventTime);

        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        String recordGroupNo = "RND-" + UUID.randomUUID();
        updateRndConsumeStateAndRecord(roller, reqVO, eventTime, operatorId, operatorName, recordGroupNo, padType);
        updateRndConsumeStateAndRecord(bearing, reqVO, eventTime, operatorId, operatorName, recordGroupNo, padType);
        return recordGroupNo;
    }

    @Override
    public List<HcPressSlotSpareImportExcelVO> buildExportList(HcPressSlotSparePageReqVO reqVO) {
        return hcPressSlotSpareMapper.selectList(reqVO).stream()
                .map(this::toImportExcel)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcPressSlotSpareImportRespVO importExcel(MultipartFile file, Boolean confirmClear) throws IOException {
        if (!Boolean.TRUE.equals(confirmClear)) {
            throw invalidParamException("导入初始化前必须确认清空当前压槽辊/轴承状态数据");
        }
        HcPressSlotSpareImportRespVO respVO = new HcPressSlotSpareImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }

        List<HcPressSlotSpareImportExcelVO> excelRows = ExcelUtils.read(file, HcPressSlotSpareImportExcelVO.class);
        List<PressSlotSpareImportRow> rows = new ArrayList<>();
        Set<String> importKeys = new HashSet<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcPressSlotSpareImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            PressSlotSpareImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
            if (row == null) {
                continue;
            }
            String importKey = row.equipment.getId() + "|" + row.spareType;
            if (!importKeys.add(importKey)) {
                addImportFailure(respVO, String.format("第%d行：同一设备、备件类型在导入文件内重复：%s", rowNo, importKey));
                continue;
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
        respVO.setClearedRecordCount(hcPressSlotSpareRecordMapper.physicalDeleteByTenantId(tenantId));
        respVO.setClearedStateCount(hcPressSlotSpareMapper.physicalDeleteByTenantId(tenantId));
        for (PressSlotSpareImportRow row : rows) {
            HcPressSlotSpareDO state = buildImportedState(row, tenantId);
            hcPressSlotSpareMapper.insert(state);
            hcPressSlotSpareRecordMapper.insert(buildImportedInitRecord(state, row, tenantId));
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        respVO.getMessages().add(String.format(
                "导入初始化完成：物理删除当前状态 %d 条、流水 %d 条，初始化状态 %d 条",
                respVO.getClearedStateCount(), respVO.getClearedRecordCount(), respVO.getSuccessCount()));
        return respVO;
    }

    private HcPressSlotSpareImportExcelVO toImportExcel(HcPressSlotSpareDO state) {
        HcPressSlotSpareImportExcelVO excelVO = new HcPressSlotSpareImportExcelVO();
        excelVO.setEquipmentId(state.getEquipmentId());
        excelVO.setEquipmentCode(state.getEquipmentCode());
        excelVO.setEquipmentName(state.getEquipmentName());
        excelVO.setWorkCenterId(state.getWorkCenterId());
        excelVO.setWorkCenterCode(state.getWorkCenterCode());
        excelVO.setWorkCenterName(state.getWorkCenterName());
        excelVO.setSpareType(state.getSpareType());
        excelVO.setSpareTypeName(spareTypeName(state.getSpareType()));
        excelVO.setMaterialCode(state.getMaterialCode());
        excelVO.setMaterialName(state.getMaterialName());
        excelVO.setBatchNo(state.getBatchNo());
        excelVO.setOnlineQuantity(state.getOnlineQuantity());
        excelVO.setAvailableQuantity(state.getAvailableQuantity());
        excelVO.setUseCount(state.getUseCount());
        excelVO.setLimitCount(state.getLimitCount());
        excelVO.setLimitDays(state.getLimitDays());
        excelVO.setWarningFlag(state.getWarningFlag());
        excelVO.setWarningFlagName(Integer.valueOf(1).equals(state.getWarningFlag()) ? "预警" : "正常");
        excelVO.setStatus(state.getStatus());
        excelVO.setStatusName(statusName(state.getStatus()));
        excelVO.setLastReplaceTime(formatDateTime(state.getLastReplaceTime()));
        excelVO.setLastReplacePlanNo(state.getLastReplacePlanNo());
        excelVO.setLastReplaceReason(state.getLastReplaceReason());
        excelVO.setLastCleanTime(formatDateTime(state.getLastCleanTime()));
        excelVO.setLastCleanRemark(state.getLastCleanRemark());
        excelVO.setLastOperatorId(state.getLastOperatorId());
        excelVO.setLastOperatorName(state.getLastOperatorName());
        excelVO.setLastEventTime(formatDateTime(state.getLastEventTime()));
        excelVO.setRemark(state.getRemark());
        return excelVO;
    }

    private void validateRndConsumeState(HcPressSlotSpareDO state, LocalDateTime eventTime) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(StrUtil.trimToEmpty(state.getStatus()))) {
            throw invalidParamException(String.format("%s（%s）当前为停用状态，不能登记研发样品消耗",
                    spareTypeName(state.getSpareType()), state.getBatchNo()));
        }
        if (state.getLastEventTime() != null && eventTime.isBefore(state.getLastEventTime())) {
            throw invalidParamException(String.format("研发消耗时间不能早于%s（%s）的最后操作时间 %s",
                    spareTypeName(state.getSpareType()), state.getBatchNo(), formatDateTime(state.getLastEventTime())));
        }
    }

    private void updateRndConsumeStateAndRecord(HcPressSlotSpareDO state,
                                                HcPressSlotSpareRndConsumeSaveReqVO reqVO,
                                                LocalDateTime eventTime,
                                                Long operatorId,
                                                String operatorName,
                                                String recordGroupNo, String padType) {
        int beforeUseCount = state.getUseCount() == null ? 0 : state.getUseCount();
        if (reqVO.getPressSlotInputPcs() > Integer.MAX_VALUE - beforeUseCount) {
            throw invalidParamException("研发样品投入片数超出备件累计片数的可记录范围");
        }
        int afterUseCount = beforeUseCount + reqVO.getPressSlotInputPcs();
        state.setUseCount(afterUseCount);
        state.setWarningFlag(calcWarningFlag(state));
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        int updated = hcPressSlotSpareMapper.updateCurrentStateById(state);
        if (updated == 0) {
            throw invalidParamException("压槽备件不存在、已删除或不属于当前租户");
        }

        hcPressSlotSpareRecordMapper.insert(HcPressSlotSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(state.getSpareType())
                .eventType(EVENT_RND_MANUAL_USE)
                .recordSource(RECORD_SOURCE_RND_MANUAL)
                .productionBatchNo(reqVO.getProductionBatchNo().trim())
                .prodType(PROD_TYPE_RND_TRIAL)
                .modelCode(reqVO.getModelCode().trim())
                .padType(padType)
                .operationCode(PRESS_SLOT_OPERATION_CODE)
                .operationName(PRESS_SLOT_OPERATION_NAME)
                .bizType(BIZ_TYPE_RND_MANUAL)
                .recordGroupNo(recordGroupNo)
                .beforeMaterialCode(state.getMaterialCode())
                .beforeBatchNo(state.getBatchNo())
                .afterMaterialCode(state.getMaterialCode())
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(afterUseCount)
                .changeUseCount(reqVO.getPressSlotInputPcs())
                .pressSlotInputPcs(BigDecimal.valueOf(reqVO.getPressSlotInputPcs()))
                .pressSlotOutputPcs(BigDecimal.valueOf(reqVO.getPressSlotOutputPcs()))
                .beforeAvailableQuantity(zeroIfNull(state.getAvailableQuantity()))
                .afterAvailableQuantity(zeroIfNull(state.getAvailableQuantity()))
                .changeQuantity(BigDecimal.ZERO)
                .onlineQuantity(state.getOnlineQuantity())
                .offlineQuantity(BigDecimal.ZERO)
                .finalUseCount(afterUseCount)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark().trim())
                .tenantId(state.getTenantId())
                .build());
    }

    private PressSlotSpareImportRow normalizeImportRow(HcPressSlotSpareImportExcelVO excelRow,
                                                       int rowNo,
                                                       HcPressSlotSpareImportRespVO respVO) {
        HcEquipmentDO equipment = resolveImportEquipment(excelRow, rowNo, respVO);
        String spareType = normalizeImportSpareType(excelRow.getSpareType(), excelRow.getSpareTypeName());
        if (StrUtil.isBlank(spareType)) {
            addImportFailure(respVO, String.format("第%d行：备件类型不能为空，仅支持压槽辊或轴承", rowNo));
        }
        String materialCode = trimToNull(excelRow.getMaterialCode());
        if (SPARE_BEARING.equals(spareType) && StrUtil.isBlank(materialCode)) {
            addImportFailure(respVO, String.format("第%d行：轴承必须填写料号", rowNo));
        }
        String batchNo = trimToNull(excelRow.getBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            addImportFailure(respVO, String.format("第%d行：批号/编码不能为空", rowNo));
        }
        LocalDateTime lastReplaceTime = parseDateTime(excelRow.getLastReplaceTime(), rowNo, "上次更换时间", respVO);
        LocalDateTime lastCleanTime = parseDateTime(excelRow.getLastCleanTime(), rowNo, "上次清洗时间", respVO);
        LocalDateTime lastEventTime = parseDateTime(excelRow.getLastEventTime(), rowNo, "最后操作时间", respVO);
        if (equipment == null || StrUtil.isBlank(spareType) || StrUtil.isBlank(batchNo)
                || (SPARE_BEARING.equals(spareType) && StrUtil.isBlank(materialCode))) {
            return null;
        }

        PressSlotSpareImportRow row = new PressSlotSpareImportRow();
        row.equipment = equipment;
        row.workCenterId = firstNonNull(excelRow.getWorkCenterId(), equipment.getWorkCenterId());
        row.workCenterCode = firstNotBlank(excelRow.getWorkCenterCode(), equipment.getWorkCenterCode());
        row.workCenterName = firstNotBlank(excelRow.getWorkCenterName(), equipment.getWorkCenterName());
        row.spareType = spareType;
        row.materialCode = materialCode;
        row.materialName = trimToNull(excelRow.getMaterialName());
        row.batchNo = batchNo;
        row.onlineQuantity = firstNonNull(excelRow.getOnlineQuantity(), BigDecimal.ONE);
        row.availableQuantity = firstNonNull(excelRow.getAvailableQuantity(),
                SPARE_PRESS_ROLLER.equals(spareType) ? BigDecimal.ONE : BigDecimal.ZERO);
        row.useCount = excelRow.getUseCount() == null ? 0 : excelRow.getUseCount();
        row.limitCount = excelRow.getLimitCount() == null ? defaultLimitCount(spareType) : excelRow.getLimitCount();
        row.limitDays = excelRow.getLimitDays() == null ? defaultLimitDays(spareType) : excelRow.getLimitDays();
        row.status = normalizeStatus(excelRow.getStatus(), excelRow.getStatusName());
        row.lastReplaceTime = firstNonNull(lastReplaceTime, lastEventTime, LocalDateTime.now());
        row.lastCleanTime = lastCleanTime;
        row.lastReplacePlanNo = trimToNull(excelRow.getLastReplacePlanNo());
        row.lastReplaceReason = trimToNull(excelRow.getLastReplaceReason());
        row.lastCleanRemark = trimToNull(excelRow.getLastCleanRemark());
        row.lastOperatorId = firstNonNull(excelRow.getLastOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        row.lastOperatorName = firstNotBlank(excelRow.getLastOperatorName(),
                SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        row.lastEventTime = firstNonNull(lastEventTime, lastReplaceTime, LocalDateTime.now());
        row.remark = trimToNull(excelRow.getRemark());
        if (row.onlineQuantity.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：在线数量不能为负数", rowNo));
            return null;
        }
        if (row.availableQuantity.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：可用量不能为负数", rowNo));
            return null;
        }
        if (row.useCount < 0) {
            addImportFailure(respVO, String.format("第%d行：累计片数不能为负数", rowNo));
            return null;
        }
        if (row.limitCount < 0 || row.limitDays < 0) {
            addImportFailure(respVO, String.format("第%d行：上限值不能为负数", rowNo));
            return null;
        }
        return row;
    }

    private HcEquipmentDO resolveImportEquipment(HcPressSlotSpareImportExcelVO excelRow,
                                                 int rowNo,
                                                 HcPressSlotSpareImportRespVO respVO) {
        HcEquipmentDO equipment = null;
        if (excelRow.getEquipmentId() != null) {
            equipment = hcEquipmentMapper.selectById(excelRow.getEquipmentId());
        }
        String equipmentCode = trimToNull(excelRow.getEquipmentCode());
        if (equipment == null && StrUtil.isNotBlank(equipmentCode)) {
            equipment = hcEquipmentMapper.selectOne(new LambdaQueryWrapperX<HcEquipmentDO>()
                    .eq(HcEquipmentDO::getEquipmentCode, equipmentCode)
                    .eq(HcEquipmentDO::getDeleted, false)
                    .last("LIMIT 1"));
        }
        if (equipment == null) {
            addImportFailure(respVO, String.format("第%d行：设备不存在，请填写有效设备ID或设备编码", rowNo));
        }
        return equipment;
    }

    private HcPressSlotSpareDO buildImportedState(PressSlotSpareImportRow row, Long tenantId) {
        HcPressSlotSpareDO state = HcPressSlotSpareDO.builder()
                .equipmentId(row.equipment.getId())
                .equipmentCode(row.equipment.getEquipmentCode())
                .equipmentName(row.equipment.getEquipmentName())
                .workCenterId(row.workCenterId)
                .workCenterCode(row.workCenterCode)
                .workCenterName(row.workCenterName)
                .spareType(row.spareType)
                .materialCode(row.materialCode)
                .materialName(row.materialName)
                .batchNo(row.batchNo)
                .onlineQuantity(row.onlineQuantity)
                .availableQuantity(row.availableQuantity)
                .lastReplaceTime(row.lastReplaceTime)
                .lastReplacePlanNo(row.lastReplacePlanNo)
                .lastReplaceReason(firstNotBlank(row.lastReplaceReason, IMPORT_INIT_REASON))
                .lastCleanTime(row.lastCleanTime)
                .lastCleanRemark(row.lastCleanRemark)
                .useCount(row.useCount)
                .limitCount(row.limitCount)
                .limitDays(row.limitDays)
                .status(row.status)
                .lastOperatorId(row.lastOperatorId)
                .lastOperatorName(row.lastOperatorName)
                .lastEventTime(row.lastEventTime)
                .remark(row.remark)
                .tenantId(tenantId)
                .build();
        state.setWarningFlag(calcWarningFlag(state));
        return state;
    }

    private HcPressSlotSpareRecordDO buildImportedInitRecord(HcPressSlotSpareDO state,
                                                             PressSlotSpareImportRow row,
                                                             Long tenantId) {
        return HcPressSlotSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(state.getSpareType())
                .eventType(EVENT_CONFIG)
                .planNo(row.lastReplacePlanNo)
                .afterMaterialCode(state.getMaterialCode())
                .afterBatchNo(state.getBatchNo())
                .afterUseCount(state.getUseCount())
                .changeUseCount(state.getUseCount())
                .afterAvailableQuantity(state.getAvailableQuantity())
                .changeQuantity(state.getAvailableQuantity())
                .onlineQuantity(state.getOnlineQuantity())
                .offlineQuantity(BigDecimal.ZERO)
                .finalUseCount(state.getUseCount())
                .replaceReason(firstNotBlank(row.lastReplaceReason, IMPORT_INIT_REASON))
                .operatorId(row.lastOperatorId)
                .operatorName(row.lastOperatorName)
                .eventTime(row.lastEventTime)
                .remark(firstNotBlank(row.remark, IMPORT_INIT_REASON))
                .tenantId(tenantId)
                .build();
    }

    private boolean isBlankImportRow(HcPressSlotSpareImportExcelVO row) {
        if (row == null) {
            return true;
        }
        return row.getEquipmentId() == null
                && StrUtil.isBlank(row.getEquipmentCode())
                && StrUtil.isBlank(row.getSpareType())
                && StrUtil.isBlank(row.getSpareTypeName())
                && StrUtil.isBlank(row.getMaterialCode())
                && StrUtil.isBlank(row.getBatchNo())
                && row.getAvailableQuantity() == null
                && row.getUseCount() == null;
    }

    private String normalizeImportSpareType(String typeCode, String typeName) {
        String text = firstNotBlank(typeCode, typeName);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String trimmed = text.trim();
        String upper = trimmed.toUpperCase();
        if (SPARE_PRESS_ROLLER.equals(upper) || "ROLLER".equals(upper)
                || "压槽辊".equals(trimmed) || "压辊".equals(trimmed)) {
            return SPARE_PRESS_ROLLER;
        }
        if (SPARE_BEARING.equals(upper) || "轴承".equals(trimmed)) {
            return SPARE_BEARING;
        }
        return null;
    }

    private String normalizeStatus(String statusCode, String statusName) {
        String text = firstNotBlank(statusCode, statusName);
        if (StrUtil.isBlank(text)) {
            return STATUS_ACTIVE;
        }
        String trimmed = text.trim();
        String upper = trimmed.toUpperCase();
        if (STATUS_STOPPED.equals(upper) || "停用".equals(trimmed) || "未使用".equals(trimmed)
                || "未使用（停用）".equals(trimmed)) {
            return STATUS_STOPPED;
        }
        return STATUS_ACTIVE;
    }

    private LocalDateTime parseDateTime(String value, int rowNo, String fieldName,
                                        HcPressSlotSpareImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace('/', '-').replace('T', ' ');
        try {
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            if (normalized.length() == 16) {
                return LocalDateTime.parse(normalized, DATE_TIME_MINUTE_FORMATTER);
            }
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd HH:mm:ss", rowNo, fieldName));
            return null;
        }
    }

    private String spareTypeName(String value) {
        if (SPARE_PRESS_ROLLER.equals(value)) {
            return "压槽辊";
        }
        if (SPARE_BEARING.equals(value)) {
            return "轴承";
        }
        return value;
    }

    private String statusName(String value) {
        if (STATUS_STOPPED.equals(value)) {
            return "未使用（停用）";
        }
        return "使用中";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private void addImportFailure(HcPressSlotSpareImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String trimToNull(String value) {
        String text = StrUtil.trim(value);
        return StrUtil.isBlank(text) ? null : text;
    }

    private String normalizeSpareType(String spareType) {
        String type = StrUtil.trimToEmpty(spareType).toUpperCase();
        if (SPARE_PRESS_ROLLER.equals(type) || "ROLLER".equals(type) || "压辊".equals(spareType)) {
            return SPARE_PRESS_ROLLER;
        }
        if (SPARE_BEARING.equals(type) || "轴承".equals(spareType)) {
            return SPARE_BEARING;
        }
        throw invalidParamException("压槽备件类型只允许压辊或轴承");
    }

    private int defaultLimitCount(String type) {
        return SPARE_PRESS_ROLLER.equals(type) ? ROLLER_LIMIT_COUNT : BEARING_LIMIT_COUNT;
    }

    private int defaultLimitDays(String type) {
        return SPARE_PRESS_ROLLER.equals(type) ? ROLLER_LIMIT_DAYS : BEARING_LIMIT_DAYS;
    }

    private Integer calcWarningFlag(HcPressSlotSpareDO state) {
        int useCount = state.getUseCount() == null ? 0 : state.getUseCount();
        int limitCount = state.getLimitCount() == null ? 0 : state.getLimitCount();
        return limitCount > 0 && useCount * 100 >= limitCount * 95 ? 1 : 0;
    }

    private LocalDateTime resolveBusinessDateTime(LocalDateTime requestedTime, LocalDateTime existedTime,
                                                 LocalDateTime fallbackTime) {
        if (isValidBusinessDateTime(requestedTime)) {
            return requestedTime;
        }
        if (isValidBusinessDateTime(existedTime)) {
            return existedTime;
        }
        return fallbackTime;
    }

    private boolean isValidBusinessDateTime(LocalDateTime value) {
        return value != null && value.getYear() >= MIN_VALID_BUSINESS_YEAR;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static class PressSlotSpareImportRow {
        private HcEquipmentDO equipment;
        private Long workCenterId;
        private String workCenterCode;
        private String workCenterName;
        private String spareType;
        private String materialCode;
        private String materialName;
        private String batchNo;
        private BigDecimal onlineQuantity;
        private BigDecimal availableQuantity;
        private Integer useCount;
        private Integer limitCount;
        private Integer limitDays;
        private String status;
        private LocalDateTime lastReplaceTime;
        private String lastReplacePlanNo;
        private String lastReplaceReason;
        private LocalDateTime lastCleanTime;
        private String lastCleanRemark;
        private Long lastOperatorId;
        private String lastOperatorName;
        private LocalDateTime lastEventTime;
        private String remark;
    }
}
