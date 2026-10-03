package cn.iocoder.yudao.module.mes.service.hc.equipmentconsumable;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcEquipmentConsumableServiceImpl implements HcEquipmentConsumableService {

    private static final String DEFAULT_PROCESS_CODE = "ROUGH_GRINDING";
    private static final String DEFAULT_PROCESS_NAME = "磨皮";
    private static final String EVENT_ADJUST = "ADJUST";
    private static final String EVENT_REPLACE = "REPLACE";
    private static final String TYPE_GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final String TYPE_SANDPAPER = "SANDPAPER";
    private static final String STATUS_IN_USE = "IN_USE";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final String IMPORT_INIT_REASON = "Excel导入初始化";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcEquipmentMapper hcEquipmentMapper;

    @Resource
    private HcEquipmentConsumableStateMapper hcEquipmentConsumableStateMapper;

    @Resource
    private HcEquipmentConsumableEventMapper hcEquipmentConsumableEventMapper;

    @Override
    public PageResult<HcEquipmentConsumableStateDO> getStatePage(HcEquipmentConsumableStatePageReqVO reqVO) {
        return hcEquipmentConsumableStateMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<HcEquipmentConsumableEventDO> getEventPage(HcEquipmentConsumableEventPageReqVO reqVO) {
        return hcEquipmentConsumableEventMapper.selectPage(reqVO);
    }

    @Override
    public HcEquipmentConsumableStateDO getState(Long id) {
        return hcEquipmentConsumableStateMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adjust(HcEquipmentConsumableAdjustReqVO reqVO) {
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("设备不存在");
        }
        String processCode = StrUtil.blankToDefault(reqVO.getProcessCode(), DEFAULT_PROCESS_CODE);
        String processName = StrUtil.blankToDefault(reqVO.getProcessName(), DEFAULT_PROCESS_NAME);
        LocalDateTime eventTime = reqVO.getEventTime() == null ? LocalDateTime.now() : reqVO.getEventTime();
        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");

        HcEquipmentConsumableStateDO state = reqVO.getId() == null ? null : hcEquipmentConsumableStateMapper.selectById(reqVO.getId());
        if (state == null) {
            state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(equipment.getId(), processCode, reqVO.getConsumableType());
        }
        boolean newState = state == null;
        String beforeBatchNo = state == null ? null : state.getBatchNo();
        Integer beforeUseCount = state == null ? null : state.getUseCount();
        BigDecimal beforeUsedLength = state == null ? null : state.getUsedLength();
        if (newState) {
            state = HcEquipmentConsumableStateDO.builder()
                    .equipmentId(equipment.getId())
                    .equipmentCode(equipment.getEquipmentCode())
                    .equipmentName(equipment.getEquipmentName())
                    .tenantId(equipment.getTenantId())
                    .build();
        }
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(equipment.getEquipmentCode());
        state.setEquipmentName(equipment.getEquipmentName());
        state.setWorkCenterId(firstNonNull(reqVO.getWorkCenterId(), equipment.getWorkCenterId()));
        state.setWorkCenterCode(firstNotBlank(reqVO.getWorkCenterCode(), equipment.getWorkCenterCode()));
        state.setWorkCenterName(firstNotBlank(reqVO.getWorkCenterName(), equipment.getWorkCenterName()));
        state.setProcessCode(processCode);
        state.setProcessName(processName);
        // 未传操作类型的旧调用方保持既有“更换”语义；当前台账页面会明确传递 REPLACE 或 ADJUST。
        String eventType = StrUtil.blankToDefault(reqVO.getEventType(), EVENT_REPLACE);
        if (!EVENT_REPLACE.equals(eventType) && !EVENT_ADJUST.equals(eventType)) {
            throw invalidParamException("耗材操作类型仅支持更换或调整");
        }
        boolean replaceRequested = EVENT_REPLACE.equals(eventType);
        state.setConsumableType(reqVO.getConsumableType());
        state.setBatchNo(reqVO.getBatchNo());
        // 同批号也允许发生真实更换；只有“更换”才重置累计寿命和更换基准时间。
        if (newState || replaceRequested) {
            state.setLastReplaceTime(eventTime);
            state.setLastReplacePlanNo(reqVO.getLastReplacePlanNo());
            state.setLastReplaceReason(reqVO.getReplaceReason());
            state.setUseCount(0);
            state.setUsedLength(BigDecimal.ZERO);
        } else {
            // 调整用于修正台账，不得把它误记为一次更换或重置累计寿命。
            state.setUseCount(reqVO.getUseCount() == null ? firstNonNull(state.getUseCount(), 0) : reqVO.getUseCount());
            state.setUsedLength(reqVO.getUsedLength() == null ? firstNonNull(state.getUsedLength(), BigDecimal.ZERO) : reqVO.getUsedLength());
        }
        state.setLimitCount(reqVO.getLimitCount());
        state.setLimitLength(reqVO.getLimitLength());
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), STATUS_IN_USE));
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark());
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
                .eventType(eventType)
                .planNo(reqVO.getLastReplacePlanNo())
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(EVENT_REPLACE.equals(eventType) ? null : diff(state.getUseCount(), beforeUseCount))
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(state.getUsedLength().subtract(beforeUsedLength == null ? BigDecimal.ZERO : beforeUsedLength))
                .replaceReason(reqVO.getReplaceReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark())
                .tenantId(state.getTenantId())
                .build();
        hcEquipmentConsumableEventMapper.insert(event);
        return state.getId();
    }

    @Override
    public List<HcEquipmentConsumableStateImportExcelVO> buildStateExportList(HcEquipmentConsumableStatePageReqVO reqVO) {
        return hcEquipmentConsumableStateMapper.selectList(reqVO).stream()
                .map(this::toStateImportExcel)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcEquipmentConsumableStateImportRespVO importStateExcel(MultipartFile file, Boolean confirmClear)
            throws IOException {
        if (!Boolean.TRUE.equals(confirmClear)) {
            throw invalidParamException("导入初始化前必须确认清空当前砂纸/导布寿命状态数据");
        }
        HcEquipmentConsumableStateImportRespVO respVO = new HcEquipmentConsumableStateImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }

        List<HcEquipmentConsumableStateImportExcelVO> excelRows =
                ExcelUtils.read(file, HcEquipmentConsumableStateImportExcelVO.class);
        List<ConsumableStateImportRow> rows = new ArrayList<>();
        Set<String> importKeys = new HashSet<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcEquipmentConsumableStateImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            ConsumableStateImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
            if (row == null) {
                continue;
            }
            String importKey = row.equipment.getId() + "|" + row.processCode + "|" + row.consumableType;
            if (!importKeys.add(importKey)) {
                addImportFailure(respVO, String.format("第%d行：同一设备、工序、耗材类型在导入文件内重复：%s", rowNo, importKey));
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
        respVO.setClearedEventCount(hcEquipmentConsumableEventMapper.physicalDeleteByTenantId(tenantId));
        respVO.setClearedStateCount(hcEquipmentConsumableStateMapper.physicalDeleteByTenantId(tenantId));
        for (ConsumableStateImportRow row : rows) {
            HcEquipmentConsumableStateDO state = buildImportedState(row, tenantId);
            hcEquipmentConsumableStateMapper.insert(state);
            hcEquipmentConsumableEventMapper.insert(buildImportedInitEvent(state, row, tenantId));
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        respVO.getMessages().add(String.format(
                "导入初始化完成：物理删除当前状态 %d 条、流水 %d 条，初始化状态 %d 条",
                respVO.getClearedStateCount(), respVO.getClearedEventCount(), respVO.getSuccessCount()));
        return respVO;
    }

    private HcEquipmentConsumableStateImportExcelVO toStateImportExcel(HcEquipmentConsumableStateDO state) {
        HcEquipmentConsumableStateImportExcelVO excelVO = new HcEquipmentConsumableStateImportExcelVO();
        excelVO.setEquipmentId(state.getEquipmentId());
        excelVO.setEquipmentCode(state.getEquipmentCode());
        excelVO.setEquipmentName(state.getEquipmentName());
        excelVO.setWorkCenterId(state.getWorkCenterId());
        excelVO.setWorkCenterCode(state.getWorkCenterCode());
        excelVO.setWorkCenterName(state.getWorkCenterName());
        excelVO.setProcessCode(state.getProcessCode());
        excelVO.setProcessName(state.getProcessName());
        excelVO.setConsumableType(state.getConsumableType());
        excelVO.setConsumableTypeName(consumableTypeName(state.getConsumableType()));
        excelVO.setBatchNo(state.getBatchNo());
        excelVO.setUsedLength(state.getUsedLength());
        excelVO.setUseCount(state.getUseCount());
        excelVO.setLimitLength(state.getLimitLength());
        excelVO.setLimitCount(state.getLimitCount());
        excelVO.setWarningFlag(state.getWarningFlag());
        excelVO.setWarningFlagName(Integer.valueOf(1).equals(state.getWarningFlag()) ? "预警" : "正常");
        excelVO.setStatus(state.getStatus());
        excelVO.setStatusName(statusName(state.getStatus()));
        excelVO.setLastReplaceTime(formatDateTime(state.getLastReplaceTime()));
        excelVO.setLastReplacePlanNo(state.getLastReplacePlanNo());
        excelVO.setLastReplaceReason(state.getLastReplaceReason());
        excelVO.setLastOperatorId(state.getLastOperatorId());
        excelVO.setLastOperatorName(state.getLastOperatorName());
        excelVO.setLastEventTime(formatDateTime(state.getLastEventTime()));
        excelVO.setRemark(state.getRemark());
        return excelVO;
    }

    private ConsumableStateImportRow normalizeImportRow(HcEquipmentConsumableStateImportExcelVO excelRow,
                                                       int rowNo,
                                                       HcEquipmentConsumableStateImportRespVO respVO) {
        HcEquipmentDO equipment = resolveImportEquipment(excelRow, rowNo, respVO);
        String consumableType = normalizeConsumableType(excelRow.getConsumableType(), excelRow.getConsumableTypeName());
        if (StrUtil.isBlank(consumableType)) {
            addImportFailure(respVO, String.format("第%d行：耗材类型不能为空，仅支持砂纸或导布", rowNo));
        }
        String batchNo = trimToNull(excelRow.getBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            addImportFailure(respVO, String.format("第%d行：当前批号不能为空", rowNo));
        }
        LocalDateTime lastReplaceTime = parseDateTime(excelRow.getLastReplaceTime(), rowNo, "上次更换时间", respVO);
        LocalDateTime lastEventTime = parseDateTime(excelRow.getLastEventTime(), rowNo, "最后操作时间", respVO);
        if (equipment == null || StrUtil.isBlank(consumableType) || StrUtil.isBlank(batchNo)) {
            return null;
        }
        ConsumableStateImportRow row = new ConsumableStateImportRow();
        row.rowNo = rowNo;
        row.equipment = equipment;
        row.processCode = StrUtil.blankToDefault(trimToNull(excelRow.getProcessCode()), DEFAULT_PROCESS_CODE);
        row.processName = StrUtil.blankToDefault(trimToNull(excelRow.getProcessName()), DEFAULT_PROCESS_NAME);
        row.consumableType = consumableType;
        row.batchNo = batchNo;
        row.usedLength = excelRow.getUsedLength() == null ? BigDecimal.ZERO : excelRow.getUsedLength();
        row.useCount = excelRow.getUseCount() == null ? 0 : excelRow.getUseCount();
        row.limitLength = excelRow.getLimitLength();
        row.limitCount = excelRow.getLimitCount();
        row.status = normalizeStatus(excelRow.getStatus(), excelRow.getStatusName());
        row.lastReplaceTime = firstNonNull(lastReplaceTime, lastEventTime, LocalDateTime.now());
        row.lastEventTime = firstNonNull(lastEventTime, lastReplaceTime, LocalDateTime.now());
        row.lastReplacePlanNo = trimToNull(excelRow.getLastReplacePlanNo());
        row.lastReplaceReason = trimToNull(excelRow.getLastReplaceReason());
        row.lastOperatorId = firstNonNull(excelRow.getLastOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        row.lastOperatorName = firstNotBlank(excelRow.getLastOperatorName(),
                SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        row.remark = trimToNull(excelRow.getRemark());
        row.workCenterId = firstNonNull(excelRow.getWorkCenterId(), equipment.getWorkCenterId());
        row.workCenterCode = firstNotBlank(excelRow.getWorkCenterCode(), equipment.getWorkCenterCode());
        row.workCenterName = firstNotBlank(excelRow.getWorkCenterName(), equipment.getWorkCenterName());
        if (row.usedLength.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：累计米数不能为负数", rowNo));
            return null;
        }
        if (row.useCount < 0) {
            addImportFailure(respVO, String.format("第%d行：累计次数不能为负数", rowNo));
            return null;
        }
        return row;
    }

    private HcEquipmentDO resolveImportEquipment(HcEquipmentConsumableStateImportExcelVO excelRow,
                                                 int rowNo,
                                                 HcEquipmentConsumableStateImportRespVO respVO) {
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

    private HcEquipmentConsumableStateDO buildImportedState(ConsumableStateImportRow row, Long tenantId) {
        HcEquipmentConsumableStateDO state = HcEquipmentConsumableStateDO.builder()
                .equipmentId(row.equipment.getId())
                .equipmentCode(row.equipment.getEquipmentCode())
                .equipmentName(row.equipment.getEquipmentName())
                .workCenterId(row.workCenterId)
                .workCenterCode(row.workCenterCode)
                .workCenterName(row.workCenterName)
                .processCode(row.processCode)
                .processName(row.processName)
                .consumableType(row.consumableType)
                .batchNo(row.batchNo)
                .lastReplaceTime(row.lastReplaceTime)
                .lastReplacePlanNo(row.lastReplacePlanNo)
                .lastReplaceReason(firstNotBlank(row.lastReplaceReason, IMPORT_INIT_REASON))
                .useCount(row.useCount)
                .usedLength(row.usedLength)
                .limitCount(row.limitCount)
                .limitLength(row.limitLength)
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

    private HcEquipmentConsumableEventDO buildImportedInitEvent(HcEquipmentConsumableStateDO state,
                                                               ConsumableStateImportRow row,
                                                               Long tenantId) {
        return HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(state.getProcessCode())
                .processName(state.getProcessName())
                .consumableType(state.getConsumableType())
                .eventType(EVENT_ADJUST)
                .planNo(row.lastReplacePlanNo)
                .afterBatchNo(state.getBatchNo())
                .afterUseCount(state.getUseCount())
                .changeUseCount(state.getUseCount())
                .afterUsedLength(state.getUsedLength())
                .changeLength(state.getUsedLength())
                .replaceReason(firstNotBlank(row.lastReplaceReason, IMPORT_INIT_REASON))
                .operatorId(row.lastOperatorId)
                .operatorName(row.lastOperatorName)
                .eventTime(row.lastEventTime)
                .remark(firstNotBlank(row.remark, IMPORT_INIT_REASON))
                .tenantId(tenantId)
                .build();
    }

    private boolean isBlankImportRow(HcEquipmentConsumableStateImportExcelVO row) {
        if (row == null) {
            return true;
        }
        return row.getEquipmentId() == null
                && StrUtil.isBlank(row.getEquipmentCode())
                && StrUtil.isBlank(row.getConsumableType())
                && StrUtil.isBlank(row.getConsumableTypeName())
                && StrUtil.isBlank(row.getBatchNo())
                && row.getUsedLength() == null
                && row.getUseCount() == null;
    }

    private String normalizeConsumableType(String typeCode, String typeName) {
        String text = firstNotBlank(typeCode, typeName);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String upper = text.trim().toUpperCase();
        if (TYPE_SANDPAPER.equals(upper) || "砂纸".equals(text.trim())) {
            return TYPE_SANDPAPER;
        }
        if (TYPE_GUIDE_CLOTH.equals(upper) || "导布".equals(text.trim()) || "GUIDECLOTH".equals(upper)) {
            return TYPE_GUIDE_CLOTH;
        }
        return null;
    }

    private String normalizeStatus(String statusCode, String statusName) {
        String text = firstNotBlank(statusCode, statusName);
        if (StrUtil.isBlank(text)) {
            return STATUS_IN_USE;
        }
        String upper = text.trim().toUpperCase();
        if ("ACTIVE".equals(upper)) {
            return "ACTIVE";
        }
        if (STATUS_STOPPED.equals(upper) || "停用".equals(text.trim())) {
            return STATUS_STOPPED;
        }
        return STATUS_IN_USE;
    }

    private LocalDateTime parseDateTime(String value, int rowNo, String fieldName,
                                        HcEquipmentConsumableStateImportRespVO respVO) {
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

    private String consumableTypeName(String value) {
        if (TYPE_SANDPAPER.equals(value)) {
            return "砂纸";
        }
        if (TYPE_GUIDE_CLOTH.equals(value)) {
            return "导布";
        }
        return value;
    }

    private String statusName(String value) {
        if (STATUS_STOPPED.equals(value)) {
            return "停用";
        }
        return "使用中";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private void addImportFailure(HcEquipmentConsumableStateImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String trimToNull(String value) {
        String text = StrUtil.trim(value);
        return StrUtil.isBlank(text) ? null : text;
    }

    private Integer calcWarningFlag(HcEquipmentConsumableStateDO state) {
        boolean lengthWarn = state.getLimitLength() != null
                && state.getUsedLength() != null
                && state.getUsedLength().compareTo(state.getLimitLength()) >= 0;
        boolean countWarn = state.getLimitCount() != null
                && state.getUseCount() != null
                && state.getUseCount() >= state.getLimitCount();
        return lengthWarn || countWarn ? 1 : 0;
    }

    private Integer diff(Integer after, Integer before) {
        if (after == null && before == null) {
            return null;
        }
        return (after == null ? 0 : after) - (before == null ? 0 : before);
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

    private static class ConsumableStateImportRow {
        private int rowNo;
        private HcEquipmentDO equipment;
        private Long workCenterId;
        private String workCenterCode;
        private String workCenterName;
        private String processCode;
        private String processName;
        private String consumableType;
        private String batchNo;
        private BigDecimal usedLength;
        private Integer useCount;
        private BigDecimal limitLength;
        private Integer limitCount;
        private String status;
        private LocalDateTime lastReplaceTime;
        private String lastReplacePlanNo;
        private String lastReplaceReason;
        private Long lastOperatorId;
        private String lastOperatorName;
        private LocalDateTime lastEventTime;
        private String remark;
    }
}
