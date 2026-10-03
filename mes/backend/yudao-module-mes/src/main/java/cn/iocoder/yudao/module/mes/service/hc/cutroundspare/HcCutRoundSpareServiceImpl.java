package cn.iocoder.yudao.module.mes.service.hc.cutroundspare;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSparePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareRecordMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcCutRoundSpareServiceImpl implements HcCutRoundSpareService {

    private static final String SPARE_BLADE = "CUTTING_BLADE";
    private static final String SPARE_FELT = "CUTTING_FELT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String EVENT_CONFIG = "CONFIG";
    private static final String EVENT_RND_MANUAL_USE = "RND_MANUAL_USE";
    private static final String RECORD_SOURCE_RND_MANUAL = "RND_MANUAL";
    private static final String BIZ_TYPE_RND_MANUAL = "CUT_ROUND_RND_MANUAL";
    private static final String CUT_ROUND_OPERATION_CODE = "CUT_ROUND";
    private static final String CUT_ROUND_OPERATION_NAME = "裁切";
    private static final int BLADE_LIMIT_COUNT = 5000;
    private static final int FELT_LIMIT_COUNT = 2000;
    private static final int FELT_LIMIT_DAYS = 90;
    private static final String IMPORT_INIT_REASON = "Excel导入初始化";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcEquipmentMapper hcEquipmentMapper;

    @Resource
    private HcCutRoundSpareMapper hcCutRoundSpareMapper;

    @Resource
    private HcCutRoundSpareRecordMapper hcCutRoundSpareRecordMapper;

    @Override
    public PageResult<HcCutRoundSpareDO> getPage(HcCutRoundSparePageReqVO reqVO) {
        return hcCutRoundSpareMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<HcCutRoundSpareRecordDO> getRecordPage(HcCutRoundSpareRecordPageReqVO reqVO) {
        return hcCutRoundSpareRecordMapper.selectPage(reqVO);
    }

    @Override
    public HcCutRoundSpareDO get(Long id) {
        return hcCutRoundSpareMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(HcCutRoundSpareSaveReqVO reqVO) {
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("设备不存在");
        }
        String type = normalizeSpareType(reqVO.getSpareType());
        HcCutRoundSpareDO existed = reqVO.getId() == null ? null : hcCutRoundSpareMapper.selectById(reqVO.getId());
        if (existed == null) {
            existed = hcCutRoundSpareMapper.selectForUpdate(equipment.getId(), type);
        }
        if (existed != null) {
            existed = hcCutRoundSpareMapper.selectForUpdate(existed.getEquipmentId(), existed.getSpareType());
            if (!Objects.equals(existed.getEquipmentId(), equipment.getId()) || !Objects.equals(existed.getSpareType(), type)) {
                throw invalidParamException("已有备件不能更换设备或类型，请维护原记录");
            }
        }
        boolean isNew = existed == null;
        boolean bladeCorrection = !isNew && SPARE_BLADE.equals(type) && Boolean.TRUE.equals(reqVO.getCorrectHistory());
        if (bladeCorrection && StrUtil.isBlank(reqVO.getReplaceReason())) {
            throw invalidParamException("历史纠错必须填写原因，实物更换请在裁切报工中登记并扣减库存");
        }
        if (!isNew && SPARE_BLADE.equals(type) && !bladeCorrection) {
            // 普通基础维护以锁定后的当前值为准，避免覆盖并发报工累计片数。
            reqVO.setUseCount(existed.getUseCount());
            reqVO.setLastReplaceTime(existed.getLastReplaceTime());
        }
        if (bladeCorrection && (reqVO.getUseCount() == null || reqVO.getUseCount() < 0
                || reqVO.getLastReplaceTime() == null || reqVO.getLastReplaceTime().getYear() < 2000)) {
            throw invalidParamException("历史纠错须填写非负累计片数和有效的更换时间");
        }
        Integer beforeUseCount = existed == null ? null : existed.getUseCount();

        LocalDateTime beforeReplaceTime = existed == null ? null : existed.getLastReplaceTime();
        LocalDateTime eventTime = LocalDateTime.now();
        Long operatorId = reqVO.getOperatorId() != null ? reqVO.getOperatorId() : SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        HcCutRoundSpareDO state = isNew ? new HcCutRoundSpareDO() : existed;
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(equipment.getEquipmentCode());
        state.setEquipmentName(equipment.getEquipmentName());
        state.setWorkCenterId(firstNonNull(reqVO.getWorkCenterId(), equipment.getWorkCenterId()));
        state.setWorkCenterCode(firstNotBlank(reqVO.getWorkCenterCode(), equipment.getWorkCenterCode()));
        state.setWorkCenterName(firstNotBlank(reqVO.getWorkCenterName(), equipment.getWorkCenterName()));
        state.setSpareType(type);
        if (isNew || !SPARE_BLADE.equals(type)) {
            state.setMaterialCode(null);
            state.setMaterialName(null);
            state.setBatchNo(null);
            state.setOnlineQuantity(BigDecimal.ONE);
        }
        state.setAvailableQuantity(BigDecimal.ZERO);
        state.setLastReplaceTime(!isNew && SPARE_BLADE.equals(type) && !bladeCorrection
                ? beforeReplaceTime : resolveLastReplaceTime(reqVO.getLastReplaceTime(),
                    isNew ? null : existed.getLastReplaceTime(), eventTime));
        if (isNew || !SPARE_BLADE.equals(type)) {
            state.setLastReplacePlanNo(null);
            state.setLastReplaceReason(reqVO.getReplaceReason());
        }
        state.setUseCount(reqVO.getUseCount() == null ? 0 : reqVO.getUseCount());
        state.setLimitCount(reqVO.getLimitCount() == null ? defaultLimitCount(type) : reqVO.getLimitCount());
        state.setLimitDays(resolveLimitDays(type, reqVO.getLimitDays()));
        state.setWarningFlag(calcWarningFlag(state));
        state.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ACTIVE));
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark());
        state.setTenantId(equipment.getTenantId());
        if (isNew) {
            hcCutRoundSpareMapper.insert(state);
        } else {
            hcCutRoundSpareMapper.updateById(state);
        }

        hcCutRoundSpareRecordMapper.insert(HcCutRoundSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(type)
                .eventType(bladeCorrection ? "ADJUST" : EVENT_CONFIG)
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(null)
                .onlineQuantity(state.getOnlineQuantity())
                .offlineQuantity(BigDecimal.ZERO)
                .finalUseCount(state.getUseCount())
                .replaceReason(reqVO.getReplaceReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(bladeCorrection ? "历史纠错（不发生实物更换）：上次更换时间 " + beforeReplaceTime
                        + " -> " + state.getLastReplaceTime() + "；原因：" + reqVO.getReplaceReason() : reqVO.getRemark())
                .tenantId(state.getTenantId())
                .build());
        return state.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveRndConsume(HcCutRoundSpareRndConsumeSaveReqVO reqVO) {
        if (!"BLACK_PAD".equals(reqVO.getPadType()) && !"WHITE_PAD".equals(reqVO.getPadType())) {
            throw invalidParamException("请选择黑垫或白垫");
        }
        if (reqVO.getCutOutputPcs() > reqVO.getCutInputPcs()) {
            throw invalidParamException("裁切产出片数不能大于投入片数");
        }
        String cutSizeMm = normalizeCutSizeMm(reqVO.getCutSizeMm());
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("裁切设备不存在");
        }
        HcCutRoundSpareDO blade = hcCutRoundSpareMapper.selectForUpdate(
                equipment.getId(), SPARE_BLADE);
        HcCutRoundSpareDO felt = hcCutRoundSpareMapper.selectForUpdate(
                equipment.getId(), SPARE_FELT);
        if (blade == null || felt == null) {
            throw invalidParamException("该设备必须先完成刀片和毛毡的当前状态配置后，才能登记研发样品消耗");
        }
        LocalDateTime eventTime = reqVO.getConsumeTime();
        if (eventTime.isAfter(LocalDateTime.now())) {
            throw invalidParamException("研发消耗时间不能晚于当前时间");
        }
        validateRndConsumeState(blade, eventTime);
        validateRndConsumeState(felt, eventTime);

        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        String recordGroupNo = "RND-" + UUID.randomUUID();
        updateRndConsumeStateAndRecord(blade, reqVO, cutSizeMm, eventTime, operatorId, operatorName,
                recordGroupNo, null);
        updateRndConsumeStateAndRecord(felt, reqVO, cutSizeMm, eventTime, operatorId, operatorName,
                recordGroupNo, calculateFeltUseDays(felt, eventTime));
        return recordGroupNo;
    }

    @Override
    public List<HcCutRoundSpareImportExcelVO> buildExportList(HcCutRoundSparePageReqVO reqVO) {
        return hcCutRoundSpareMapper.selectList(reqVO).stream()
                .map(this::toImportExcel)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcCutRoundSpareImportRespVO importStateExcel(MultipartFile file, Boolean confirmClear)
            throws IOException {
        if (!Boolean.TRUE.equals(confirmClear)) {
            throw invalidParamException("导入初始化前必须确认清空当前刀片/毛毡状态数据");
        }
        HcCutRoundSpareImportRespVO respVO = new HcCutRoundSpareImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }

        List<HcCutRoundSpareImportExcelVO> excelRows = ExcelUtils.read(file, HcCutRoundSpareImportExcelVO.class);
        List<CutRoundSpareImportRow> rows = new ArrayList<>();
        Set<String> importKeys = new HashSet<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcCutRoundSpareImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            CutRoundSpareImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
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
        hcCutRoundSpareMapper.lockAllStates();
        if (hcCutRoundSpareRecordMapper.hasInventoryConsumption()) {
            throw invalidParamException("已有刀片更换关联边库消耗，禁止清空初始化；请使用单条备件维护并保留流水");
        }
        respVO.setClearedRecordCount(hcCutRoundSpareRecordMapper.physicalDeleteByTenantId(tenantId));
        respVO.setClearedStateCount(hcCutRoundSpareMapper.physicalDeleteByTenantId(tenantId));
        for (CutRoundSpareImportRow row : rows) {
            HcCutRoundSpareDO state = buildImportedState(row, tenantId);
            hcCutRoundSpareMapper.insert(state);
            hcCutRoundSpareRecordMapper.insert(buildImportedInitRecord(state, row, tenantId));
            respVO.setSuccessCount(respVO.getSuccessCount() + 1);
        }
        respVO.getMessages().add(String.format(
                "导入初始化完成：物理删除当前状态 %d 条、流水 %d 条，初始化状态 %d 条",
                respVO.getClearedStateCount(), respVO.getClearedRecordCount(), respVO.getSuccessCount()));
        return respVO;
    }

    private HcCutRoundSpareImportExcelVO toImportExcel(HcCutRoundSpareDO state) {
        HcCutRoundSpareImportExcelVO excelVO = new HcCutRoundSpareImportExcelVO();
        excelVO.setEquipmentId(state.getEquipmentId());
        excelVO.setEquipmentCode(state.getEquipmentCode());
        excelVO.setEquipmentName(state.getEquipmentName());
        excelVO.setWorkCenterId(state.getWorkCenterId());
        excelVO.setWorkCenterCode(state.getWorkCenterCode());
        excelVO.setWorkCenterName(state.getWorkCenterName());
        excelVO.setSpareType(state.getSpareType());
        excelVO.setSpareTypeName(typeName(state.getSpareType()));
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
        excelVO.setLastOperatorId(state.getLastOperatorId());
        excelVO.setLastOperatorName(state.getLastOperatorName());
        excelVO.setLastEventTime(formatDateTime(state.getLastEventTime()));
        excelVO.setRemark(state.getRemark());
        return excelVO;
    }

    private void validateRndConsumeState(HcCutRoundSpareDO state, LocalDateTime eventTime) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(StrUtil.trimToEmpty(state.getStatus()))) {
            throw invalidParamException(String.format("%s当前为停用状态，不能登记研发样品消耗", typeName(state.getSpareType())));
        }
        if (state.getLastEventTime() != null && eventTime.isBefore(state.getLastEventTime())) {
            throw invalidParamException(String.format("研发消耗时间不能早于%s的最后操作时间 %s",
                    typeName(state.getSpareType()), formatDateTime(state.getLastEventTime())));
        }
    }

    private void updateRndConsumeStateAndRecord(HcCutRoundSpareDO state,
                                                HcCutRoundSpareRndConsumeSaveReqVO reqVO,
                                                String cutSizeMm,
                                                LocalDateTime eventTime,
                                                Long operatorId,
                                                String operatorName,
                                                String recordGroupNo,
                                                Integer feltUseDays) {
        int beforeUseCount = state.getUseCount() == null ? 0 : state.getUseCount();
        if (reqVO.getCutInputPcs() > Integer.MAX_VALUE - beforeUseCount) {
            throw invalidParamException("研发样品投入片数超出裁切备件累计片数的可记录范围");
        }
        int afterUseCount = beforeUseCount + reqVO.getCutInputPcs();
        state.setUseCount(afterUseCount);
        state.setWarningFlag(calcWarningFlag(state));
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        hcCutRoundSpareMapper.updateById(state);

        hcCutRoundSpareRecordMapper.insert(HcCutRoundSpareRecordDO.builder()
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
                .modelCode(reqVO.getModelCode().trim())
                .padType(reqVO.getPadType())
                .cutSizeMm(cutSizeMm)
                .operationCode(CUT_ROUND_OPERATION_CODE)
                .operationName(CUT_ROUND_OPERATION_NAME)
                .bizType(BIZ_TYPE_RND_MANUAL)
                .recordGroupNo(recordGroupNo)
                .beforeUseCount(beforeUseCount)
                .afterUseCount(afterUseCount)
                .changeUseCount(reqVO.getCutInputPcs())
                .cutInputPcs(BigDecimal.valueOf(reqVO.getCutInputPcs()))
                .cutOutputPcs(BigDecimal.valueOf(reqVO.getCutOutputPcs()))
                .feltUseDays(feltUseDays)
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

    private Integer calculateFeltUseDays(HcCutRoundSpareDO felt, LocalDateTime eventTime) {
        LocalDateTime lastReplaceTime = felt.getLastReplaceTime();
        if (!isValidBusinessTime(lastReplaceTime) || lastReplaceTime.isAfter(eventTime)) {
            return null;
        }
        return (int) Math.max(ChronoUnit.DAYS.between(lastReplaceTime.toLocalDate(), eventTime.toLocalDate()), 0);
    }

    private String normalizeCutSizeMm(String value) {
        String size = StrUtil.trimToEmpty(value).replace("mm", "");
        if ("775".equals(size) || "740".equals(size)) {
            return size;
        }
        throw invalidParamException("裁切尺寸只允许 775 或 740");
    }

    private CutRoundSpareImportRow normalizeImportRow(HcCutRoundSpareImportExcelVO excelRow,
                                                      int rowNo,
                                                      HcCutRoundSpareImportRespVO respVO) {
        HcEquipmentDO equipment = resolveImportEquipment(excelRow, rowNo, respVO);
        String spareType = normalizeImportSpareType(excelRow.getSpareType(), excelRow.getSpareTypeName());
        if (StrUtil.isBlank(spareType)) {
            addImportFailure(respVO, String.format("第%d行：备件类型不能为空，仅支持刀片或毛毡", rowNo));
        }
        LocalDateTime lastReplaceTime = parseDateTime(excelRow.getLastReplaceTime(), rowNo, "上次更换时间", respVO);
        LocalDateTime lastEventTime = parseDateTime(excelRow.getLastEventTime(), rowNo, "最后操作时间", respVO);
        if (equipment == null || StrUtil.isBlank(spareType)) {
            return null;
        }
        CutRoundSpareImportRow row = new CutRoundSpareImportRow();
        row.rowNo = rowNo;
        row.equipment = equipment;
        row.workCenterId = firstNonNull(excelRow.getWorkCenterId(), equipment.getWorkCenterId());
        row.workCenterCode = firstNotBlank(excelRow.getWorkCenterCode(), equipment.getWorkCenterCode());
        row.workCenterName = firstNotBlank(excelRow.getWorkCenterName(), equipment.getWorkCenterName());
        row.spareType = spareType;
        row.useCount = excelRow.getUseCount() == null ? 0 : excelRow.getUseCount();
        row.limitCount = excelRow.getLimitCount() == null ? defaultLimitCount(spareType) : excelRow.getLimitCount();
        Integer rawLimitDays = excelRow.getLimitDays();
        row.status = normalizeStatus(excelRow.getStatus(), excelRow.getStatusName());
        row.lastReplaceTime = firstNonNull(lastReplaceTime, lastEventTime, LocalDateTime.now());
        row.lastEventTime = firstNonNull(lastEventTime, lastReplaceTime, LocalDateTime.now());
        row.lastReplacePlanNo = trimToNull(excelRow.getLastReplacePlanNo());
        row.lastReplaceReason = trimToNull(excelRow.getLastReplaceReason());
        row.lastOperatorId = firstNonNull(excelRow.getLastOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        row.lastOperatorName = firstNotBlank(excelRow.getLastOperatorName(),
                SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        row.remark = trimToNull(excelRow.getRemark());
        if (row.useCount < 0) {
            addImportFailure(respVO, String.format("第%d行：累计片数不能为负数", rowNo));
            return null;
        }
        if (row.limitCount < 0) {
            addImportFailure(respVO, String.format("第%d行：片数上限不能为负数", rowNo));
            return null;
        }
        if (rawLimitDays != null && rawLimitDays < 0) {
            addImportFailure(respVO, String.format("第%d行：天数上限不能为负数", rowNo));
            return null;
        }
        row.limitDays = resolveLimitDays(spareType, rawLimitDays);
        return row;
    }

    private HcEquipmentDO resolveImportEquipment(HcCutRoundSpareImportExcelVO excelRow,
                                                 int rowNo,
                                                 HcCutRoundSpareImportRespVO respVO) {
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

    private HcCutRoundSpareDO buildImportedState(CutRoundSpareImportRow row, Long tenantId) {
        HcCutRoundSpareDO state = HcCutRoundSpareDO.builder()
                .equipmentId(row.equipment.getId())
                .equipmentCode(row.equipment.getEquipmentCode())
                .equipmentName(row.equipment.getEquipmentName())
                .workCenterId(row.workCenterId)
                .workCenterCode(row.workCenterCode)
                .workCenterName(row.workCenterName)
                .spareType(row.spareType)
                .materialCode(null)
                .materialName(null)
                .batchNo(null)
                .onlineQuantity(BigDecimal.ONE)
                .availableQuantity(BigDecimal.ZERO)
                .lastReplaceTime(row.lastReplaceTime)
                .lastReplacePlanNo(row.lastReplacePlanNo)
                .lastReplaceReason(firstNotBlank(row.lastReplaceReason, IMPORT_INIT_REASON))
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

    private HcCutRoundSpareRecordDO buildImportedInitRecord(HcCutRoundSpareDO state,
                                                            CutRoundSpareImportRow row,
                                                            Long tenantId) {
        return HcCutRoundSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(state.getSpareType())
                .eventType(EVENT_CONFIG)
                .afterUseCount(state.getUseCount())
                .changeUseCount(null)
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

    private boolean isBlankImportRow(HcCutRoundSpareImportExcelVO row) {
        if (row == null) {
            return true;
        }
        return row.getEquipmentId() == null
                && StrUtil.isBlank(row.getEquipmentCode())
                && StrUtil.isBlank(row.getSpareType())
                && StrUtil.isBlank(row.getSpareTypeName())
                && row.getUseCount() == null
                && row.getLimitCount() == null
                && row.getLimitDays() == null
                && StrUtil.isBlank(row.getLastReplaceTime())
                && StrUtil.isBlank(row.getLastEventTime());
    }

    private String normalizeSpareType(String spareType) {
        String type = StrUtil.trimToEmpty(spareType).toUpperCase();
        if (SPARE_BLADE.equals(type) || "BLADE".equals(type) || "刀片".equals(spareType)) {
            return SPARE_BLADE;
        }
        if (SPARE_FELT.equals(type) || "FELT".equals(type) || "毛毡".equals(spareType)) {
            return SPARE_FELT;
        }
        throw invalidParamException("裁切备件类型只允许刀片或毛毡");
    }

    private String normalizeImportSpareType(String typeCode, String typeName) {
        String text = firstNotBlank(typeCode, typeName);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String upper = text.trim().toUpperCase();
        if (SPARE_BLADE.equals(upper) || "BLADE".equals(upper) || "刀片".equals(text.trim())) {
            return SPARE_BLADE;
        }
        if (SPARE_FELT.equals(upper) || "FELT".equals(upper) || "毛毡".equals(text.trim())) {
            return SPARE_FELT;
        }
        return null;
    }

    private int defaultLimitCount(String type) {
        return SPARE_BLADE.equals(type) ? BLADE_LIMIT_COUNT : FELT_LIMIT_COUNT;
    }

    private int resolveLimitDays(String type, Integer configuredLimitDays) {
        if (SPARE_BLADE.equals(type)) {
            return 0;
        }
        return configuredLimitDays == null ? FELT_LIMIT_DAYS : configuredLimitDays;
    }

    private String typeName(String type) {
        if (SPARE_BLADE.equals(type)) {
            return "刀片";
        }
        if (SPARE_FELT.equals(type)) {
            return "毛毡";
        }
        return type;
    }

    private String statusName(String value) {
        if ("STOPPED".equals(value)) {
            return "未使用（停用）";
        }
        return "使用中";
    }

    private String normalizeStatus(String statusCode, String statusName) {
        String text = firstNotBlank(statusCode, statusName);
        if (StrUtil.isBlank(text)) {
            return STATUS_ACTIVE;
        }
        String upper = text.trim().toUpperCase();
        if ("STOPPED".equals(upper) || text.contains("停用") || text.contains("未使用")) {
            return "STOPPED";
        }
        return STATUS_ACTIVE;
    }

    private LocalDateTime parseDateTime(String value, int rowNo, String fieldName,
                                        HcCutRoundSpareImportRespVO respVO) {
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private void addImportFailure(HcCutRoundSpareImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String trimToNull(String value) {
        String text = StrUtil.trim(value);
        return StrUtil.isBlank(text) ? null : text;
    }

    private Integer calcWarningFlag(HcCutRoundSpareDO state) {
        int useCount = state.getUseCount() == null ? 0 : state.getUseCount();
        int limitCount = state.getLimitCount() == null ? 0 : state.getLimitCount();
        return limitCount > 0 && useCount * 100 >= limitCount * 95 ? 1 : 0;
    }

    private LocalDateTime resolveLastReplaceTime(LocalDateTime requestedTime,
                                                 LocalDateTime existedTime,
                                                 LocalDateTime fallbackTime) {
        if (isValidBusinessTime(requestedTime)) {
            return requestedTime;
        }
        if (isValidBusinessTime(existedTime)) {
            return existedTime;
        }
        return fallbackTime;
    }

    private boolean isValidBusinessTime(LocalDateTime value) {
        return value != null && value.getYear() >= 2000;
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

    private static class CutRoundSpareImportRow {
        private int rowNo;
        private HcEquipmentDO equipment;
        private Long workCenterId;
        private String workCenterCode;
        private String workCenterName;
        private String spareType;
        private Integer useCount;
        private Integer limitCount;
        private Integer limitDays;
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
