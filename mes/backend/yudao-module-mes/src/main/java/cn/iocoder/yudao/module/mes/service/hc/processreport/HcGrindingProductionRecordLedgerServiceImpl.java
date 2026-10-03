package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableDefaultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordConsumableSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordLifeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
public class HcGrindingProductionRecordLedgerServiceImpl implements HcGrindingProductionRecordLedgerService {
    @Resource
    private HcGrindingConsumptionService grindingConsumptionService;

    private static final String WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_REPORT_AUTO = "REPORT_AUTO";
    private static final String ROLE_FIRST_ORIGINAL = "FIRST_ORIGINAL";
    private static final String ROLE_FIRST_ALLOCATION = "FIRST_ALLOCATION";
    private static final String ROLE_SECOND = "SECOND";
    private static final Set<String> RECORD_ROLES = Set.of(ROLE_FIRST_ORIGINAL, ROLE_FIRST_ALLOCATION, ROLE_SECOND);
    private static final Set<String> ALLOCATION_SEGMENTS = Set.of("P", "Q", "R", "S", "NONE");
    private static final String ROUGH_GRINDING_PROCESS = "ROUGH_GRINDING";
    private static final String ROUGH_GRINDING_PROCESS_NAME = "磨皮";
    private static final String SANDPAPER = "SANDPAPER";
    private static final String GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final String CONSUMABLE_IN_USE = "IN_USE";
    private static final String CONSUMABLE_EVENT_ADJUST = "ADJUST";
    private static final String CONSUMABLE_EVENT_REPLACE = "REPLACE";
    private static final String STANDALONE_SANDPAPER_REASON_BIZ = "GRINDING_RECORD_SANDPAPER_REPLACE";
    private static final String MANUAL_CONFIRM_BIZ_TYPE = "GRINDING_PRODUCTION_RECORD_CONFIRM";
    private static final String MANUAL_CONFIRM_REMARK = "研发生产记录确认同步";
    private static final String PRODUCTION_RECORD_CONSUMABLE_SYNC_BIZ_TYPE = "GRINDING_PRODUCTION_RECORD_CONSUMABLE_SYNC";
    private static final String PRODUCTION_RECORD_CONSUMABLE_SYNC_REMARK = "生产记录耗材同步";
    private static final BigDecimal DEFAULT_SANDPAPER_LIMIT_LENGTH = BigDecimal.valueOf(500);
    private static final int DEFAULT_GUIDE_CLOTH_LIMIT_COUNT = 200;
    private static final int GUIDE_CLOTH_USE_INCREMENT = 2;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private HcGrindingProductionLedgerMapper ledgerMapper;

    @Resource
    private HcEquipmentMapper equipmentMapper;
    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Resource
    private HcEquipmentConsumableStateMapper consumableStateMapper;

    @Resource
    private HcEquipmentConsumableEventMapper consumableEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HcGrindingProductionRecordSaveReqVO reqVO) {
        var booking = grindingConsumptionService.begin("MANUAL", reqVO.getConsumption(), reqVO);
        if (booking != null && booking.getResultId() != null) return booking.getResultId();
        normalize(reqVO);
        HcEquipmentDO equipment = validateManualEquipment(reqVO.getEquipmentId());
        validateManualPadType(reqVO, equipment);
        HcGrindingProductionRecordDO source = applyManualConsumableDefaults(reqVO, equipment.getId(), null);
        if (shouldSplitManualSandpaperRecord(reqVO, source)) {
            Long id = createManualSandpaperSplitRecords(reqVO, equipment, source);
            finishConsumption(booking, id, reqVO);
            return id;
        }
        validateBizKey(reqVO, null);
        HcGrindingProductionRecordDO entity = buildManualRecord(reqVO, equipment);
        ledgerMapper.insert(entity);
        finishConsumption(booking, entity.getId(), reqVO);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HcGrindingProductionRecordSaveReqVO reqVO) {
        HcGrindingProductionRecordDO existing = validateExists(reqVO.getId());
        validateManualMaintain(existing);
        validatePending(existing);
        if (isManualSandpaperSplitRecord(existing)) {
            throw invalidParamException("砂纸更换生成的研发双记录不能单独修改，请整组删除后重新新增");
        }
        normalize(reqVO);
        validateManualPadType(reqVO, validateExistingManualEquipment(existing));
        applyManualConsumableDefaults(reqVO, existing.getEquipmentId(), existing.getId());
        validateBizKey(reqVO, existing.getId());
        HcGrindingProductionRecordDO entity = BeanUtils.toBean(reqVO, HcGrindingProductionRecordDO.class);
        preserveEquipmentSnapshot(entity, existing);
        entity.setPassName(passName(reqVO.getPassType()));
        entity.setStatus(WAIT_CONFIRM);
        entity.setConfirmerName(null);
        entity.setConfirmTime(null);
        grindingConsumptionService.revise("MANUAL", existing.getId(), reqVO.getConsumption(),
                Boolean.TRUE.equals(reqVO.getSandpaperChanged()), reqVO.getSandpaperBatchNo(),
                Boolean.TRUE.equals(reqVO.getGuideClothChanged()), reqVO.getGuideClothBatchNo(), reqVO.getRecordTime(), null, reqVO.getBatchNo());
        ledgerMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLife(HcGrindingProductionRecordLifeUpdateReqVO reqVO) {
        HcGrindingProductionRecordDO existing = validateExists(reqVO.getId());
        if (isManualSandpaperSplitRecord(existing)) {
            throw invalidParamException("砂纸更换生成的研发双记录不能单独修正耗材寿命，请整组删除后重新新增");
        }
        validateLifeUpdate(reqVO);
        ledgerMapper.update(new HcGrindingProductionRecordDO(), new LambdaUpdateWrapper<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getId, reqVO.getId())
                .set(HcGrindingProductionRecordDO::getSandpaperLife, reqVO.getSandpaperLife())
                .set(HcGrindingProductionRecordDO::getSandpaperLifeDays, reqVO.getSandpaperLifeDays())
                .set(HcGrindingProductionRecordDO::getGuideClothLife, reqVO.getGuideClothLife())
                .set(reqVO.getReplaceReason() != null, HcGrindingProductionRecordDO::getReplaceReason,
                        reqVO.getReplaceReason()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        deleteByIds(List.of(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw invalidParamException("请选择要删除的磨皮生产记录");
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<HcGrindingProductionRecordDO> records = ledgerMapper.selectByRecordIds(distinctIds);
        if (records.size() != distinctIds.size()) throw invalidParamException("部分磨皮生产记录不存在，请刷新后重试");
        records = expandManualSandpaperSplitRecords(records);
        records.forEach(record -> {
            validateManualMaintain(record);
            validatePending(record);
        });
        records.forEach(record -> grindingConsumptionService.cancel("MANUAL", record.getId()));
        ledgerMapper.physicalDeleteByIds(records.stream().map(HcGrindingProductionRecordDO::getId).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer confirm(HcGrindingProductionRecordConfirmReqVO reqVO) {
        List<Long> ids = reqVO.getIds().stream().distinct().toList();
        List<HcGrindingProductionRecordDO> records = ledgerMapper.selectByRecordIds(ids);
        if (records.size() != ids.size()) throw invalidParamException("部分磨皮生产记录不存在，请刷新后重试");
        records = expandManualSandpaperSplitRecords(records);
        records.sort(Comparator
                .comparing((HcGrindingProductionRecordDO record) -> first(record.getManualSplitGroupNo(), String.valueOf(record.getId())))
                .thenComparing(record -> record.getSourceSegmentNo() == null ? 1 : record.getSourceSegmentNo())
                .thenComparing(HcGrindingProductionRecordDO::getId));
        String name = first(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "当前用户");
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        LocalDateTime time = LocalDateTime.now();
        List<HcGrindingProductionRecordDO> confirmedManualRecords = new ArrayList<>();
        int count = 0;
        for (HcGrindingProductionRecordDO record : records) {
            if (CONFIRMED.equals(record.getStatus())) continue;
            HcGrindingProductionRecordDO update = new HcGrindingProductionRecordDO();
            update.setId(record.getId());
            update.setStatus(CONFIRMED);
            update.setConfirmerName(name);
            update.setConfirmTime(time);
            ledgerMapper.updateById(update);
            if (SOURCE_MANUAL.equals(record.getSourceType())) {
                confirmedManualRecords.add(record);
            }
            count++;
        }
        if (count == 0) throw invalidParamException("所选磨皮生产记录均已确认，无需重复确认");
        confirmedManualRecords.forEach(record -> syncConfirmedManualConsumableState(record, operatorId, name));
        return count;
    }

    @Override
    public HcGrindingProductionRecordDO get(Long id) {
        HcGrindingProductionRecordDO row = validateExists(id);
        fillConsumption(row);
        return row;
    }

    @Override
    public HcGrindingProductionRecordConsumableDefaultRespVO getConsumableDefault(Long equipmentId,
                                                                                    LocalDateTime completionTime) {
        if (completionTime == null || completionTime.getYear() < 2000) {
            throw invalidParamException("完工日期无效，无法获取耗材默认值");
        }
        validateManualEquipment(equipmentId);
        return toConsumableDefault(ledgerMapper.selectLatestConsumableSnapshot(equipmentId, completionTime));
    }

    @Override
    public HcGrindingProductionRecordConsumableSyncRespVO getLatestConfirmedConsumableSync(Long equipmentId) {
        HcEquipmentDO equipment = validateConsumableSyncEquipment(equipmentId);
        HcGrindingProductionRecordDO record = resolveLatestConfirmedConsumableSyncRecord(equipmentId);
        return toConsumableSyncResp(record, equipment, false, "已获取该设备最近一条已确认生产记录");
    }

    /**
     * 将设备维度的最新已确认生产记录累计快照同步到磨皮报工读取的耗材状态表。
     * 砂纸天数没有独立状态字段，使用“来源完工时间 - (累计天数 - 1)”反推更换时间，
     * 因此报工端按“当天=1天”的口径计算时可与来源记录保持一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcGrindingProductionRecordConsumableSyncRespVO syncLatestConfirmedConsumableState(
            HcGrindingProductionRecordConsumableSyncReqVO reqVO) {
        HcEquipmentDO equipment = validateConsumableSyncEquipment(reqVO.getEquipmentId());
        HcGrindingProductionRecordDO record = resolveLatestConfirmedConsumableSyncRecord(equipment.getId());
        HcEquipmentConsumableStateDO sandpaperState = consumableStateMapper.selectOneByEquipmentAndType(
                equipment.getId(), ROUGH_GRINDING_PROCESS, SANDPAPER);
        HcEquipmentConsumableStateDO guideClothState = consumableStateMapper.selectOneByEquipmentAndType(
                equipment.getId(), ROUGH_GRINDING_PROCESS, GUIDE_CLOTH);
        validateConsumableSyncState(sandpaperState, record, record.getSandpaperBatchNo(), "砂纸");
        validateConsumableSyncState(guideClothState, record, record.getGuideClothBatchNo(), "导布");

        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = first(SecurityFrameworkUtils.getLoginUserNickname(), "当前用户");
        // 审计时间保留实际执行时间；同步顺序通过事件 bizId 关联来源记录的完工时间判断。
        LocalDateTime syncTime = LocalDateTime.now();
        boolean sandpaperSynchronized = syncSandpaperStateFromLatestConfirmedRecord(
                record, equipment, sandpaperState, syncTime, operatorId, operatorName);
        boolean guideClothSynchronized = syncGuideClothStateFromLatestConfirmedRecord(
                record, equipment, guideClothState, syncTime, operatorId, operatorName);
        boolean synchronizedState = sandpaperSynchronized || guideClothSynchronized;
        return toConsumableSyncResp(record, equipment, synchronizedState,
                synchronizedState ? "已同步至磨皮报工耗材状态" : "当前报工耗材状态已与来源记录一致");
    }

    @Override
    public PageResult<HcGrindingProductionRecordDO> getPage(HcGrindingProductionRecordPageReqVO reqVO) {
        normalize(reqVO);
        var page = ledgerMapper.selectPage(reqVO);
        page.getList().forEach(this::fillConsumption);
        return page;
    }

    @Override
    public List<HcGrindingProductionRecordExcelVO> buildExportList(HcGrindingProductionRecordPageReqVO reqVO) {
        normalize(reqVO);
        return ledgerMapper.selectList(reqVO).stream().map(this::toExcel).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcGrindingProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException {
        HcGrindingProductionRecordImportRespVO resp = new HcGrindingProductionRecordImportRespVO();
        if (file == null || file.isEmpty()) {
            fail(resp, "导入文件为空");
            return resp;
        }
        List<HcGrindingProductionRecordImportExcelVO> excelRows = ExcelUtils.read(file, HcGrindingProductionRecordImportExcelVO.class);
        List<ImportRow> rows = new ArrayList<>();
        Map<String, Integer> fileKeys = new HashMap<>();
        for (int i = 0; i < excelRows.size(); i++) {
            HcGrindingProductionRecordImportExcelVO excel = excelRows.get(i);
            if (blankRow(excel)) {
                resp.setSkippedRows(resp.getSkippedRows() + 1);
                continue;
            }
            int rowNo = i + 2;
            resp.setTotalRows(resp.getTotalRows() + 1);
            int before = resp.getFailureCount();
            ImportRow row = parse(excel, rowNo, resp);
            if (row == null || before != resp.getFailureCount()) continue;
            String key = key(row.reportDate, row.modelCode, row.materialCode, row.batchNo, row.passType,
                    row.recordRole, row.segmentMark, row.sandpaperBatchNo, row.guideClothBatchNo);
            Integer previous = fileKeys.putIfAbsent(key, rowNo);
            if (previous != null) {
                fail(resp, String.format("第%d行：业务关键词与第%d行重复", rowNo, previous));
                continue;
            }
            HcGrindingProductionRecordDO existing = ledgerMapper.selectByBizKey(null, row.reportDate, row.modelCode,
                    row.materialCode, row.batchNo, row.passType, row.recordRole, row.segmentMark,
                    row.sandpaperBatchNo, row.guideClothBatchNo);
            if (existing != null && isSourceRecord(existing)) {
                fail(resp, String.format("第%d行：匹配记录由磨皮报工自动生成，请在报工看板修订。批号=%s", rowNo, row.batchNo));
                continue;
            }
            if (existing != null && CONFIRMED.equals(existing.getStatus())) {
                fail(resp, String.format("第%d行：匹配记录已确认，禁止导入覆盖。批号=%s", rowNo, row.batchNo));
                continue;
            }
            row.existingId = existing == null ? null : existing.getId();
            rows.add(row);
        }
        if (!resp.getFailures().isEmpty()) {
            resp.getMessages().add("导入校验未通过，未写入任何数据");
            return resp;
        }
        if (rows.isEmpty()) {
            fail(resp, "导入文件没有有效数据行");
            return resp;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        for (ImportRow row : rows) {
            HcGrindingProductionRecordDO entity = row.toDO();
            entity.setPadType(padTypeResolver.resolveByModelCode(entity.getModelCode()));
            entity.setStatus(WAIT_CONFIRM);
            entity.setSourceType("IMPORT");
            entity.setTenantId(tenantId);
            if (row.existingId == null) {
                ledgerMapper.insert(entity);
                resp.setCreateCount(resp.getCreateCount() + 1);
            } else {
                entity.setId(row.existingId);
                ledgerMapper.updateById(entity);
                resp.setUpdateCount(resp.getUpdateCount() + 1);
            }
        }
        resp.getMessages().add(String.format("导入完成：新增 %d 条，更新 %d 条；导入记录均为待确认",
                resp.getCreateCount(), resp.getUpdateCount()));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAutoRecord(HcGrindingProductionRecordDO sourceRecord) {
        syncAutoRecords(List.of(sourceRecord));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAutoRecords(List<HcGrindingProductionRecordDO> sourceRecords) {
        if (sourceRecords == null || sourceRecords.isEmpty()) {
            throw invalidParamException("磨皮报工缺少自动生成生产记录数据");
        }
        HcGrindingProductionRecordDO firstRecord = sourceRecords.get(0);
        validateAutoSource(firstRecord);
        String passType = firstRecord.getPassType();
        String sourceBizType = firstRecord.getSourceBizType();
        Long sourceDetailId = firstRecord.getSourceDetailId();
        Set<Integer> segmentNos = new HashSet<>();
        for (HcGrindingProductionRecordDO sourceRecord : sourceRecords) {
            validateAutoSource(sourceRecord);
            applyMissingConsumableDefaults(sourceRecord);
            int segmentNo = sourceRecord.getSourceSegmentNo() == null ? 1 : sourceRecord.getSourceSegmentNo();
            if (segmentNo <= 0 || !segmentNos.add(segmentNo)) {
                throw invalidParamException("同一磨皮报工的砂纸记录拆分序号无效或重复");
            }
            if (!Objects.equals(passType, sourceRecord.getPassType())
                    || !Objects.equals(sourceBizType, sourceRecord.getSourceBizType())
                    || !Objects.equals(sourceDetailId, sourceRecord.getSourceDetailId())) {
                throw invalidParamException("同一批自动生产记录必须来自同一次磨皮报工");
            }
            sourceRecord.setSourceSegmentNo(segmentNo);
        }
        Map<Integer, HcGrindingProductionRecordDO> existingBySegment = new HashMap<>();
        for (HcGrindingProductionRecordDO existing : ledgerMapper.selectListBySource(
                sourceBizType, passType, sourceDetailId)) {
            validatePending(existing);
            existingBySegment.put(existing.getSourceSegmentNo() == null ? 1 : existing.getSourceSegmentNo(), existing);
        }
        for (HcGrindingProductionRecordDO sourceRecord : sourceRecords) {
            HcGrindingProductionRecordDO existing = existingBySegment.remove(sourceRecord.getSourceSegmentNo());
            sourceRecord.setPassName(passName(sourceRecord.getPassType()));
            sourceRecord.setStatus(WAIT_CONFIRM);
            sourceRecord.setSourceType(SOURCE_REPORT_AUTO);
            sourceRecord.setConfirmerName(null);
            sourceRecord.setConfirmTime(null);
            if (existing == null) {
                ledgerMapper.insert(sourceRecord);
            } else {
                sourceRecord.setId(existing.getId());
                ledgerMapper.updateById(sourceRecord);
            }
            syncStandaloneSandpaperReason(sourceRecord);
        }
        if (!existingBySegment.isEmpty()) {
            ledgerMapper.physicalDeleteByIds(existingBySegment.values().stream()
                    .map(HcGrindingProductionRecordDO::getId).toList());
        }
    }

    /**
     * 看板独立更换不属于报工事件，使用独立业务关联保留原始事件，避免报工撤销时将它删除。
     * 仅实际首次 USE 对应的生产记录领取原因；用事件顺序而非批号区分同批号换卷。
     */
    void syncStandaloneSandpaperReason(HcGrindingProductionRecordDO record) {
        if (record.getId() == null || record.getTenantId() == null || record.getRecordTime() == null) return;
        List<HcEquipmentConsumableEventDO> bound = consumableEventMapper.selectListByBiz(
                STANDALONE_SANDPAPER_REASON_BIZ, record.getId());
        for (HcEquipmentConsumableEventDO event : bound) {
            if (sameSandpaper(record, event)) appendStandaloneReason(record, event.getReplaceReason());
        }
        if (!bound.isEmpty()) return;
        List<HcEquipmentConsumableEventDO> usages = consumableEventMapper.selectListByBiz(
                "GRINDING_" + record.getPassType(), record.getSourceDetailId());
        for (HcEquipmentConsumableEventDO usage : usages) {
            if (!"USE".equals(usage.getEventType()) || !sameSandpaper(record, usage)
                    || usage.getEventTime() == null || usage.getEventTime().isAfter(record.getRecordTime())
                    || record.getSandpaperLife() == null || usage.getAfterUsedLength() == null
                    || record.getSandpaperLife().compareTo(usage.getAfterUsedLength()) != 0) continue;
            HcEquipmentConsumableEventDO candidate = consumableEventMapper.selectLatestBeforeByStateIdAndEventType(
                    usage.getStateId(), "REPLACE", usage.getEventTime(), usage.getId());
            if (candidate == null) continue;
            HcEquipmentConsumableEventDO replacement = consumableEventMapper.selectByIdForUpdate(
                    candidate.getId(), record.getTenantId());
            if (replacement == null || !sameSandpaper(record, replacement)
                    || trim(replacement.getBizType()) != null || replacement.getBizId() != null
                    || trim(replacement.getGrindingStage()) != null || replacement.getGrindingDetailId() != null
                    || trim(replacement.getReplaceReason()) == null) continue;
            HcEquipmentConsumableEventDO firstUse = consumableEventMapper.selectLaterUsageEvent(
                    replacement.getStateId(), replacement.getEventTime(), replacement.getId());
            if (firstUse == null || !Objects.equals(firstUse.getId(), usage.getId())) continue;
            if (consumableEventMapper.bindStandaloneReplacement(replacement.getId(), record.getTenantId(),
                    STANDALONE_SANDPAPER_REASON_BIZ, record.getId()) != 1) {
                throw invalidParamException("砂纸更换事件归属已变化，请刷新后重试");
            }
            appendStandaloneReason(record, replacement.getReplaceReason());
            return;
        }
    }

    private boolean sameSandpaper(HcGrindingProductionRecordDO record, HcEquipmentConsumableEventDO event) {
        return Objects.equals(record.getTenantId(), event.getTenantId())
                && Objects.equals(record.getEquipmentId(), event.getEquipmentId())
                && ROUGH_GRINDING_PROCESS.equals(event.getProcessCode())
                && SANDPAPER.equals(event.getConsumableType())
                && trim(record.getSandpaperBatchNo()) != null
                && Objects.equals(trim(record.getSandpaperBatchNo()), trim(event.getAfterBatchNo()));
    }

    private void appendStandaloneReason(HcGrindingProductionRecordDO record, String reason) {
        if (trim(reason) == null) return;
        String text = "砂纸：" + trim(reason);
        String existing = trim(record.getReplaceReason());
        if (existing != null && (existing.equals(text) || existing.startsWith(text + "；")
                || existing.endsWith("；" + text) || existing.contains("；" + text + "；"))) return;
        String combined = existing == null ? text : text + "；" + existing;
        record.setReplaceReason(combined);
        int updated = ledgerMapper.update(null, new LambdaUpdateWrapper<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getId, record.getId())
                .eq(HcGrindingProductionRecordDO::getTenantId, record.getTenantId())
                .set(HcGrindingProductionRecordDO::getReplaceReason, combined));
        if (updated != 1) throw invalidParamException("生产记录更换原因写入失败，请刷新后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAutoRecordTime(String passType, Long sourceDetailId, LocalDateTime recordTime) {
        String normalizedPassType = normalizePass(passType);
        syncAutoRecordTime(defaultSourceBizType(normalizedPassType), normalizedPassType, sourceDetailId, recordTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAutoRecordTime(String sourceBizType, String passType, Long sourceDetailId,
                                   LocalDateTime recordTime) {
        String normalizedPassType = normalizePass(passType);
        String normalizedSourceBizType = normalizeRecordRole(sourceBizType);
        validateSourceRoleMatchesPass(normalizedSourceBizType, normalizedPassType);
        if (normalizedPassType == null || normalizedSourceBizType == null || sourceDetailId == null
                || recordTime == null || recordTime.getYear() < 2000) {
            throw invalidParamException("磨皮生产记录自动同步的来源或完工时间无效");
        }
        List<HcGrindingProductionRecordDO> existingRecords = ledgerMapper.selectListBySource(
                normalizedSourceBizType, normalizedPassType, sourceDetailId);
        if (existingRecords.isEmpty()) {
            return;
        }
        for (HcGrindingProductionRecordDO existing : existingRecords) {
            validatePending(existing);
            HcGrindingProductionRecordDO update = new HcGrindingProductionRecordDO();
            update.setId(existing.getId());
            update.setReportDate(recordTime.toLocalDate());
            update.setRecordTime(recordTime);
            if (existing.getSandpaperLifeDays() != null && existing.getRecordTime() != null) {
                long deltaDays = ChronoUnit.DAYS.between(existing.getRecordTime().toLocalDate(), recordTime.toLocalDate());
                update.setSandpaperLifeDays((int) Math.max(0, Math.min(Integer.MAX_VALUE,
                        (long) existing.getSandpaperLifeDays() + deltaDays)));
            }
            ledgerMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecordBySource(String passType, Long sourceDetailId) {
        String normalizedPassType = normalizePass(passType);
        deleteRecordBySource(defaultSourceBizType(normalizedPassType), normalizedPassType, sourceDetailId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecordBySource(String sourceBizType, String passType, Long sourceDetailId) {
        String normalizedPassType = normalizePass(passType);
        String normalizedSourceBizType = normalizeRecordRole(sourceBizType);
        validateSourceRoleMatchesPass(normalizedSourceBizType, normalizedPassType);
        if (normalizedPassType == null || normalizedSourceBizType == null || sourceDetailId == null) {
            throw invalidParamException("磨皮生产记录来源不能为空");
        }
        List<HcGrindingProductionRecordDO> existingRecords = ledgerMapper.selectListBySource(
                normalizedSourceBizType, normalizedPassType, sourceDetailId);
        if (existingRecords.isEmpty()) {
            return;
        }
        existingRecords.forEach(this::validatePending);
        ledgerMapper.physicalDeleteByIds(existingRecords.stream().map(HcGrindingProductionRecordDO::getId).toList());
    }

    private HcGrindingProductionRecordExcelVO toExcel(HcGrindingProductionRecordDO row) {
        fillConsumption(row);
        HcGrindingProductionRecordExcelVO excel = new HcGrindingProductionRecordExcelVO();
        if (row.getConsumption() != null) {
            excel.setSandpaperConsumeQty(row.getConsumption().getSandpaperQty());
            excel.setSandpaperConsumeUnit(row.getConsumption().getSandpaperUnit());
            excel.setGuideClothConsumeQty(row.getConsumption().getGuideClothQty());
            excel.setGuideClothConsumeUnit(row.getConsumption().getGuideClothUnit());
        }
        excel.setCompletionTime(format(row.getRecordTime()));
        excel.setModelCode(row.getModelCode());
        excel.setPadType(row.getPadType());
        excel.setMaterialCode(row.getMaterialCode());
        excel.setMotherBatchNo(row.getMotherBatchNo());
        excel.setBatchNo(row.getBatchNo());
        excel.setRecordRole(row.getRecordRole());
        excel.setSourceBizType(row.getSourceBizType());
        excel.setSegmentMark(displaySegmentMark(row.getSegmentMark()));
        excel.setStartPosition(row.getStartPosition());
        excel.setFirstAllocationId(row.getFirstAllocationId());
        excel.setInputLength(row.getInputLength());
        excel.setOutputLength(row.getOutputLength());
        excel.setPassName(row.getPassName());
        excel.setSandpaperLife(row.getSandpaperLife());
        excel.setSandpaperLifeDays(row.getSandpaperLifeDays());
        excel.setSandpaperBatchNo(row.getSandpaperBatchNo());
        excel.setSourceSegmentNo(row.getSourceSegmentNo());
        excel.setGuideClothLife(row.getGuideClothLife());
        excel.setGuideClothBatchNo(row.getGuideClothBatchNo());
        excel.setReplaceReason(row.getReplaceReason());
        excel.setRecorderName(row.getRecorderName());
        excel.setConfirmerName(row.getConfirmerName());
        excel.setRemark(row.getRemark());
        return excel;
    }

    private ImportRow parse(HcGrindingProductionRecordImportExcelVO excel, int rowNo,
            HcGrindingProductionRecordImportRespVO resp) {
        ImportRow row = new ImportRow();
        row.recordTime = parseDateTime(excel.getCompletionTime(), rowNo, "完工日期", resp);
        row.reportDate = row.recordTime == null ? null : row.recordTime.toLocalDate();
        row.modelCode = required(excel.getModelCode(), rowNo, "型号", resp);
        row.materialCode = required(excel.getMaterialCode(), rowNo, "料号", resp);
        row.motherBatchNo = trim(excel.getMotherBatchNo());
        row.batchNo = required(excel.getBatchNo(), rowNo, "批号", resp);
        String passName = required(first(excel.getPassName(), excel.getLegacyPassName()), rowNo, "磨皮次数", resp);
        row.passType = normalizePass(passName);
        if (row.passType == null) {
            fail(resp, String.format("第%d行：磨皮次数仅支持一次/二次/三次/四次/FIRST/SECOND/THIRD/FOURTH", rowNo));
        }
        row.recordRole = normalizeRecordRoleOptional(excel.getRecordRole());
        if (trim(excel.getRecordRole()) != null && row.recordRole == null) {
            fail(resp, String.format("第%d行：记录角色仅支持 FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND", rowNo));
        }
        row.sourceBizType = normalizeRecordRoleOptional(excel.getSourceBizType());
        if (trim(excel.getSourceBizType()) != null && row.sourceBizType == null) {
            fail(resp, String.format("第%d行：来源业务类型仅支持 FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND", rowNo));
        }
        if (row.recordRole != null && row.sourceBizType != null
                && !Objects.equals(row.recordRole, row.sourceBizType)) {
            fail(resp, String.format("第%d行：记录角色与来源业务类型不一致", rowNo));
        }
        String effectiveRole = first(row.recordRole, row.sourceBizType);
        if ((ROLE_SECOND.equals(effectiveRole) && !"SECOND".equals(row.passType))
                || (!ROLE_SECOND.equals(effectiveRole) && effectiveRole != null && !"FIRST".equals(row.passType))) {
            fail(resp, String.format("第%d行：记录角色仅可与一次或二次磨皮匹配", rowNo));
        }
        row.segmentMark = normalizeSegmentMark(excel.getSegmentMark());
        if (trim(excel.getSegmentMark()) != null && row.segmentMark == null) {
            fail(resp, String.format("第%d行：加工单元仅支持 P/Q/R/S/NONE/不分段", rowNo));
        }
        row.startPosition = excel.getStartPosition();
        row.firstAllocationId = excel.getFirstAllocationId();
        if (row.firstAllocationId != null && row.firstAllocationId <= 0) {
            fail(resp, String.format("第%d行：一磨分配记录ID必须大于0", rowNo));
        }
        row.inputLength = excel.getInputLength();
        row.outputLength = excel.getOutputLength();
        row.sandpaperLife = excel.getSandpaperLife();
        row.sandpaperLifeDays = excel.getSandpaperLifeDays();
        row.sandpaperBatchNo = empty(excel.getSandpaperBatchNo());
        row.guideClothLife = excel.getGuideClothLife();
        row.guideClothBatchNo = empty(excel.getGuideClothBatchNo());
        row.replaceReason = trim(excel.getReplaceReason());
        row.recorderName = required(excel.getRecorderName(), rowNo, "记录人", resp);
        row.remark = trim(excel.getRemark());
        nonNegative(row.inputLength, rowNo, "投入米数", resp);
        nonNegative(row.outputLength, rowNo, "产出米数", resp);
        nonNegative(row.startPosition, rowNo, "起米位置", resp);
        nonNegative(row.sandpaperLife, rowNo, "砂纸累计寿命", resp);
        nonNegative(row.sandpaperLifeDays, rowNo, "砂纸累计天数", resp);
        nonNegative(row.guideClothLife, rowNo, "导布累计寿命", resp);
        return row.reportDate == null || row.modelCode == null || row.materialCode == null || row.batchNo == null
                || row.passType == null || row.recorderName == null || row.recordTime == null ? null : row;
    }

    private void normalize(HcGrindingProductionRecordSaveReqVO req) {
        req.setModelCode(trim(req.getModelCode()));
        String padType = padTypeResolver.normalizePadType(req.getPadType());
        if (padType == null) {
            throw invalidParamException("类型仅支持黑垫或白垫");
        }
        req.setPadType(padType);
        req.setMaterialCode(trim(req.getMaterialCode()));
        req.setBatchNo(trim(req.getBatchNo()));
        req.setPassType(normalizePass(req.getPassType()));
        req.setSandpaperBatchNo(empty(req.getSandpaperBatchNo()));
        req.setGuideClothBatchNo(empty(req.getGuideClothBatchNo()));
        req.setSandpaperChanged(Boolean.TRUE.equals(req.getSandpaperChanged()));
        req.setGuideClothChanged(Boolean.TRUE.equals(req.getGuideClothChanged()));
        req.setSandpaperReplaceReason(trim(req.getSandpaperReplaceReason()));
        req.setGuideClothReplaceReason(trim(req.getGuideClothReplaceReason()));
        req.setReplaceReason(trim(req.getReplaceReason()));
        req.setRecorderName(trim(req.getRecorderName()));
        req.setRemark(trim(req.getRemark()));
        if (req.getPassType() == null) throw invalidParamException("磨皮次数仅支持一次、二次、三次或四次");
        if (req.getRecordTime() == null || req.getRecordTime().getYear() < 2000) throw invalidParamException("完工日期无效");
        req.setReportDate(req.getRecordTime().toLocalDate());
        if (negative(req.getInputLength()) || negative(req.getOutputLength()) || negative(req.getSandpaperLife())
                || negative(req.getSandpaperLifeDays()) || negative(req.getGuideClothLife())) throw invalidParamException("投入、产出和耗材寿命不能为负数");
        if (Boolean.TRUE.equals(req.getSandpaperChanged())) {
            validateManualConsumableReplacement(req.getSandpaperBatchNo(), req.getSandpaperReplaceReason(), "砂纸");
            applyManualSandpaperReplacementUsage(req);
        }
        if (Boolean.TRUE.equals(req.getGuideClothChanged())) {
            validateManualConsumableReplacement(req.getGuideClothBatchNo(), req.getGuideClothReplaceReason(), "导布");
        }
        String replaceSummary = buildManualReplacementSummary(req);
        if (replaceSummary != null) {
            req.setReplaceReason(replaceSummary);
        }
    }

    private void validateManualConsumableReplacement(String batchNo, String reason, String consumableName) {
        if (trim(batchNo) == null) {
            throw invalidParamException(consumableName + "选择更换后必须填写新批号");
        }
        if (trim(reason) == null) {
            throw invalidParamException(consumableName + "选择更换后必须填写更换原因");
        }
    }

    private void validateLifeUpdate(HcGrindingProductionRecordLifeUpdateReqVO req) {
        if (req.getSandpaperLife() == null && req.getSandpaperLifeDays() == null
                && req.getGuideClothLife() == null && req.getReplaceReason() == null) {
            throw invalidParamException("至少填写一项耗材寿命或更换原因修正值");
        }
        if (negative(req.getSandpaperLife()) || negative(req.getSandpaperLifeDays())
                || negative(req.getGuideClothLife())) {
            throw invalidParamException("耗材寿命修正值不能为负数");
        }
    }

    private String buildManualReplacementSummary(HcGrindingProductionRecordSaveReqVO req) {
        List<String> reasons = new ArrayList<>();
        if (Boolean.TRUE.equals(req.getSandpaperChanged())) {
            reasons.add("砂纸：" + req.getSandpaperReplaceReason());
        }
        if (Boolean.TRUE.equals(req.getGuideClothChanged())) {
            reasons.add("导布：" + req.getGuideClothReplaceReason());
        }
        return reasons.isEmpty() ? null : String.join("；", reasons);
    }

    private void applyManualSandpaperReplacementUsage(HcGrindingProductionRecordSaveReqVO req) {
        if (isZeroOrNull(req.getSandpaperLife()) && positive(req.getOutputLength())) {
            req.setSandpaperLife(req.getOutputLength());
        }
        if (positive(req.getSandpaperLife()) && (req.getSandpaperLifeDays() == null || req.getSandpaperLifeDays() < 1)) {
            req.setSandpaperLifeDays(1);
        } else if (req.getSandpaperLifeDays() == null) {
            req.setSandpaperLifeDays(0);
        }
    }

    private void normalize(HcGrindingProductionRecordPageReqVO req) {
        if (req.getCompletionTimeStart() != null && req.getCompletionTimeEnd() != null
                && req.getCompletionTimeStart().isAfter(req.getCompletionTimeEnd())) {
            throw invalidParamException("完工日期开始时间不能晚于结束时间");
        }
        req.setModelCode(trim(req.getModelCode()));
        req.setMaterialCode(trim(req.getMaterialCode()));
        req.setMotherBatchNo(trim(req.getMotherBatchNo()));
        req.setBatchNo(trim(req.getBatchNo()));
        req.setRecordRole(normalizeRecordRoleOptional(req.getRecordRole()));
        req.setSourceBizType(normalizeRecordRoleOptional(req.getSourceBizType()));
        req.setSegmentMark(normalizeSegmentMark(req.getSegmentMark()));
        req.setGrindingPass(normalizePassOptional(req.getGrindingPass()));
        req.setRecorderName(trim(req.getRecorderName()));
        req.setStatus(trim(req.getStatus()));
        String padType = padTypeResolver.normalizeFilter(req.getPadType());
        if (req.getPadType() != null && padType == null) {
            throw invalidParamException("类型筛选仅支持黑垫、白垫或未归类");
        }
        req.setPadType(padType);
    }

    private void validateBizKey(HcGrindingProductionRecordSaveReqVO req, Long currentId) {
        validateBizKey(req.getEquipmentId(), req.getReportDate(), req.getModelCode(), req.getMaterialCode(), req.getBatchNo(),
                req.getPassType(), req.getSandpaperBatchNo(), req.getGuideClothBatchNo(), currentId);
    }

    private void validateBizKey(HcGrindingProductionRecordDO record, Long currentId) {
        validateBizKey(record.getEquipmentId(), record.getReportDate(), record.getModelCode(), record.getMaterialCode(),
                record.getBatchNo(), record.getPassType(), record.getSandpaperBatchNo(), record.getGuideClothBatchNo(), currentId);
    }

    private void validateBizKey(Long equipmentId, LocalDate reportDate, String modelCode, String materialCode,
                                String batchNo, String passType, String sandpaperBatchNo, String guideClothBatchNo,
                                Long currentId) {
        HcGrindingProductionRecordDO existing = ledgerMapper.selectByBizKey(equipmentId, reportDate, modelCode,
                materialCode, batchNo, passType, null, null, sandpaperBatchNo, guideClothBatchNo);
        if (existing != null && (currentId == null || !currentId.equals(existing.getId())))
            throw invalidParamException("相同日期、型号、料号、批号、磨皮次数、砂纸批号、导布批号的记录已存在");
    }

    /**
     * 手工研发记录勾选砂纸更换时，沿用报工的“先耗尽旧砂纸，再使用新砂纸”口径。
     * 首笔研发记录没有可追溯旧砂纸快照时，只保留新砂纸记录，避免伪造旧批号事实。
     */
    private boolean shouldSplitManualSandpaperRecord(HcGrindingProductionRecordSaveReqVO req,
                                                     HcGrindingProductionRecordDO source) {
        return Boolean.TRUE.equals(req.getSandpaperChanged()) && source != null
                && trim(source.getSandpaperBatchNo()) != null;
    }

    private Long createManualSandpaperSplitRecords(HcGrindingProductionRecordSaveReqVO req,
                                                   HcEquipmentDO equipment,
                                                   HcGrindingProductionRecordDO source) {
        BigDecimal oldLifeBefore = source.getSandpaperLife() == null ? BigDecimal.ZERO
                : source.getSandpaperLife().max(BigDecimal.ZERO);
        BigDecimal consumptionLength = req.getOutputLength() == null ? BigDecimal.ZERO
                : req.getOutputLength().max(BigDecimal.ZERO);
        BigDecimal remainingLength = DEFAULT_SANDPAPER_LIMIT_LENGTH.subtract(oldLifeBefore).max(BigDecimal.ZERO);
        BigDecimal oldBatchUseLength = consumptionLength.min(remainingLength);
        BigDecimal newBatchUseLength = consumptionLength.subtract(oldBatchUseLength).max(BigDecimal.ZERO);
        String splitGroupNo = "MANUAL-SP-" + UUID.randomUUID();

        HcGrindingProductionRecordDO oldRecord = buildManualRecord(req, equipment);
        oldRecord.setManualSplitGroupNo(splitGroupNo);
        oldRecord.setSourceSegmentNo(1);
        oldRecord.setSandpaperBatchNo(source.getSandpaperBatchNo());
        oldRecord.setSandpaperLife(oldLifeBefore.add(oldBatchUseLength));
        oldRecord.setSandpaperLifeDays(source.getSandpaperLifeDays());
        oldRecord.setSandpaperChanged(false);
        oldRecord.setSandpaperReplaceReason(null);
        oldRecord.setGuideClothLife(source.getGuideClothLife());
        oldRecord.setGuideClothBatchNo(first(source.getGuideClothBatchNo(), req.getGuideClothBatchNo()));
        oldRecord.setGuideClothChanged(false);
        oldRecord.setGuideClothReplaceReason(null);
        oldRecord.setReplaceReason(null);

        HcGrindingProductionRecordDO newRecord = buildManualRecord(req, equipment);
        newRecord.setManualSplitGroupNo(splitGroupNo);
        newRecord.setSourceSegmentNo(2);
        newRecord.setSandpaperLife(newBatchUseLength);
        newRecord.setSandpaperLifeDays(newBatchUseLength.signum() > 0 ? 1 : 0);

        // 两条记录都在落库前校验，避免同批号物理换卷被拆分记录自身误判为重复。
        validateBizKey(oldRecord, null);
        validateBizKey(newRecord, null);
        ledgerMapper.insert(oldRecord);
        ledgerMapper.insert(newRecord);
        return newRecord.getId();
    }

    private HcGrindingProductionRecordDO buildManualRecord(HcGrindingProductionRecordSaveReqVO req,
                                                           HcEquipmentDO equipment) {
        HcGrindingProductionRecordDO entity = BeanUtils.toBean(req, HcGrindingProductionRecordDO.class);
        applyEquipmentSnapshot(entity, equipment);
        entity.setPassName(passName(req.getPassType()));
        entity.setStatus(WAIT_CONFIRM);
        entity.setSourceType(SOURCE_MANUAL);
        entity.setTenantId(TenantContextHolder.getTenantId());
        return entity;
    }

    private List<HcGrindingProductionRecordDO> expandManualSandpaperSplitRecords(
            List<HcGrindingProductionRecordDO> selectedRecords) {
        Map<Long, HcGrindingProductionRecordDO> recordsById = new HashMap<>();
        for (HcGrindingProductionRecordDO record : selectedRecords) {
            if (isManualSandpaperSplitRecord(record)) {
                List<HcGrindingProductionRecordDO> groupRecords = ledgerMapper
                        .selectListByManualSplitGroupNo(record.getManualSplitGroupNo());
                if (groupRecords.size() != 2) {
                    throw invalidParamException("砂纸更换研发双记录数据不完整，请联系管理员处理");
                }
                groupRecords.forEach(item -> recordsById.put(item.getId(), item));
            } else {
                recordsById.put(record.getId(), record);
            }
        }
        return new ArrayList<>(recordsById.values());
    }

    private boolean isManualSandpaperSplitRecord(HcGrindingProductionRecordDO record) {
        return record != null && SOURCE_MANUAL.equals(record.getSourceType())
                && trim(record.getManualSplitGroupNo()) != null;
    }

    private HcGrindingProductionRecordDO validateExists(Long id) {
        HcGrindingProductionRecordDO row = id == null ? null : ledgerMapper.selectById(id);
        if (row == null) throw invalidParamException("磨皮生产记录不存在");
        return row;
    }

    private void validatePending(HcGrindingProductionRecordDO row) {
        if (CONFIRMED.equals(row.getStatus())) throw invalidParamException("磨皮生产记录已确认，不能修改或删除");
    }

    private void validateManualMaintain(HcGrindingProductionRecordDO row) {
        if (isSourceRecord(row)) {
            throw invalidParamException("该磨皮生产记录由报工自动生成，请在磨皮报工看板修订或删除来源报工");
        }
    }

    private void validateAutoSource(HcGrindingProductionRecordDO source) {
        if (source == null) {
            throw invalidParamException("磨皮报工缺少自动生成生产记录数据");
        }
        source.setPassType(normalizePass(source.getPassType()));
        String requestedSourceBizType = trim(source.getSourceBizType());
        String requestedRecordRole = trim(source.getRecordRole());
        String normalizedSourceBizType = normalizeRecordRoleOptional(requestedSourceBizType);
        String normalizedRecordRole = normalizeRecordRoleOptional(requestedRecordRole);
        if (requestedSourceBizType != null && normalizedSourceBizType == null) {
            throw invalidParamException("自动生产记录来源业务类型不正确");
        }
        if (requestedRecordRole != null && normalizedRecordRole == null) {
            throw invalidParamException("自动生产记录角色不正确");
        }
        source.setSourceBizType(first(normalizedSourceBizType, defaultSourceBizType(source.getPassType())));
        source.setRecordRole(first(normalizedRecordRole, source.getSourceBizType()));
        source.setSegmentMark(normalizeSegmentMark(source.getSegmentMark()));
        validateSourceRoleMatchesPass(source.getSourceBizType(), source.getPassType());
        if (!Objects.equals(source.getRecordRole(), source.getSourceBizType())) {
            throw invalidParamException("自动生产记录的记录角色与来源业务类型必须一致");
        }
        if (ROLE_FIRST_ALLOCATION.equals(source.getSourceBizType())) {
            if (source.getSegmentMark() == null) {
                throw invalidParamException("一磨前置分配生产记录缺少加工单元");
            }
            if (source.getFirstAllocationId() == null) {
                throw invalidParamException("一磨前置分配生产记录缺少一磨分配记录ID");
            }
            source.setMotherBatchNo(trim(source.getMotherBatchNo()));
            if (source.getMotherBatchNo() == null) {
                throw invalidParamException("一磨前置分配生产记录缺少加工母批号");
            }
            if (source.getInputLength() == null || source.getInputLength().signum() <= 0
                    || source.getOutputLength() == null
                    || source.getInputLength().compareTo(source.getOutputLength()) != 0) {
                throw invalidParamException("一磨前置分配生产记录的投入、产出必须等于确认米数且大于0");
            }
        } else if (ROLE_FIRST_ORIGINAL.equals(source.getSourceBizType()) && trim(source.getMotherBatchNo()) == null) {
            source.setMotherBatchNo(trim(source.getBatchNo()));
        }
        if (source.getStartPosition() != null && source.getStartPosition().signum() < 0) {
            throw invalidParamException("磨皮生产记录起米位置不能为负数");
        }
        if (source.getPassType() == null || source.getSourceBizType() == null || source.getSourceDetailId() == null
                || trim(source.getModelCode()) == null || trim(source.getMaterialCode()) == null || trim(source.getBatchNo()) == null
                || source.getEquipmentId() == null || source.getRecordTime() == null || source.getRecordTime().getYear() < 2000) {
            throw invalidParamException("磨皮报工缺少自动生成生产记录所需的设备、日期、型号、料号、批号或完工时间");
        }
        source.setModelCode(trim(source.getModelCode()));
        source.setPadType(padTypeResolver.resolveByModelCode(source.getModelCode()));
        source.setMaterialCode(trim(source.getMaterialCode()));
        source.setBatchNo(trim(source.getBatchNo()));
        source.setReportDate(source.getRecordTime().toLocalDate());
        source.setSandpaperBatchNo(empty(source.getSandpaperBatchNo()));
        source.setGuideClothBatchNo(empty(source.getGuideClothBatchNo()));
        source.setReplaceReason(trim(source.getReplaceReason()));
        source.setRecorderName(first(source.getRecorderName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        source.setRemark(trim(source.getRemark()));
        if (source.getTenantId() == null) {
            source.setTenantId(TenantContextHolder.getTenantId());
        }
    }

    /**
     * 手工记录只允许在新增时选设备；编辑时保持原设备，防止同一条记录跨设备迁移导致寿命串台。
     */
    private HcEquipmentDO validateManualEquipment(Long equipmentId) {
        if (equipmentId == null) {
            throw invalidParamException("新增磨皮生产记录必须选择设备");
        }
        HcEquipmentDO equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw invalidParamException("所选设备不存在或已删除");
        }
        return equipment;
    }

    private HcEquipmentDO validateExistingManualEquipment(HcGrindingProductionRecordDO existing) {
        HcEquipmentDO equipment = existing.getEquipmentId() == null ? null
                : equipmentMapper.selectById(existing.getEquipmentId());
        if (equipment == null) {
            throw invalidParamException("当前磨皮生产记录缺少有效设备，无法维护类型");
        }
        return equipment;
    }

    /**
     * 手工研发记录以用户选择的垫型为业务事实，只校验设备适用范围，不依赖产品型号主数据。
     */
    private void validateManualPadType(HcGrindingProductionRecordSaveReqVO req, HcEquipmentDO equipment) {
        String equipmentPadType = trim(equipment.getApplicablePadType());
        if (equipmentPadType == null) {
            throw invalidParamException("所选设备未维护适用垫型，请先维护设备台账");
        }
        equipmentPadType = equipmentPadType.toUpperCase();
        if (!"COMMON".equals(equipmentPadType) && !req.getPadType().equals(equipmentPadType)) {
            throw invalidParamException("所选设备不适用于当前类型");
        }
    }

    private void applyEquipmentSnapshot(HcGrindingProductionRecordDO entity, HcEquipmentDO equipment) {
        entity.setEquipmentId(equipment.getId());
        entity.setEquipmentCode(equipment.getEquipmentCode());
        entity.setEquipmentName(equipment.getEquipmentName());
    }

    private void preserveEquipmentSnapshot(HcGrindingProductionRecordDO entity, HcGrindingProductionRecordDO existing) {
        entity.setEquipmentId(existing.getEquipmentId());
        entity.setEquipmentCode(existing.getEquipmentCode());
        entity.setEquipmentName(existing.getEquipmentName());
    }

    private HcGrindingProductionRecordDO applyManualConsumableDefaults(HcGrindingProductionRecordSaveReqVO req,
                                                                        Long equipmentId,
                                                                        Long excludeRecordId) {
        HcGrindingProductionRecordDO source = ledgerMapper.selectLatestConsumableSnapshot(
                equipmentId, req.getRecordTime(), excludeRecordId);
        if (source != null) {
            if (req.getSandpaperLife() == null) req.setSandpaperLife(source.getSandpaperLife());
            if (req.getSandpaperLifeDays() == null) req.setSandpaperLifeDays(source.getSandpaperLifeDays());
            if (trim(req.getSandpaperBatchNo()) == null) req.setSandpaperBatchNo(source.getSandpaperBatchNo());
            if (!Boolean.TRUE.equals(req.getGuideClothChanged()) && trim(req.getGuideClothBatchNo()) == null) {
                req.setGuideClothBatchNo(source.getGuideClothBatchNo());
            }
        }
        req.setGuideClothLife(Boolean.TRUE.equals(req.getGuideClothChanged())
                ? GUIDE_CLOTH_USE_INCREMENT : nextGuideClothLife(source));
        return source;
    }

    private void applyMissingConsumableDefaults(HcGrindingProductionRecordDO target) {
        HcGrindingProductionRecordDO source = ledgerMapper.selectLatestConsumableSnapshot(
                target.getEquipmentId(), target.getRecordTime());
        if (source == null) {
            return;
        }
        if (target.getSandpaperLife() == null) target.setSandpaperLife(source.getSandpaperLife());
        if (target.getSandpaperLifeDays() == null) target.setSandpaperLifeDays(source.getSandpaperLifeDays());
        if (trim(target.getSandpaperBatchNo()) == null) target.setSandpaperBatchNo(source.getSandpaperBatchNo());
        if (target.getGuideClothLife() == null) target.setGuideClothLife(source.getGuideClothLife());
        if (trim(target.getGuideClothBatchNo()) == null) target.setGuideClothBatchNo(source.getGuideClothBatchNo());
    }

    private int nextGuideClothLife(HcGrindingProductionRecordDO source) {
        int currentLife = source == null || source.getGuideClothLife() == null
                ? 0 : Math.max(0, source.getGuideClothLife());
        long nextLife = (long) currentLife + GUIDE_CLOTH_USE_INCREMENT;
        return nextLife > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) nextLife;
    }

    private HcGrindingProductionRecordConsumableDefaultRespVO toConsumableDefault(HcGrindingProductionRecordDO source) {
        HcGrindingProductionRecordConsumableDefaultRespVO resp = new HcGrindingProductionRecordConsumableDefaultRespVO();
        resp.setGuideClothLife(nextGuideClothLife(source));
        if (source == null) {
            return resp;
        }
        resp.setSourceRecordId(source.getId());
        resp.setSourceCompletionTime(source.getRecordTime());
        resp.setSourceStatus(source.getStatus());
        resp.setSourceType(source.getSourceType());
        resp.setSandpaperLife(source.getSandpaperLife());
        resp.setSandpaperLifeDays(source.getSandpaperLifeDays());
        resp.setSandpaperBatchNo(source.getSandpaperBatchNo());
        resp.setGuideClothBatchNo(source.getGuideClothBatchNo());
        return resp;
    }

    private HcEquipmentDO validateConsumableSyncEquipment(Long equipmentId) {
        if (equipmentId == null) {
            throw invalidParamException("请选择需要同步的设备");
        }
        HcEquipmentDO equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw invalidParamException("所选设备不存在或已删除");
        }
        return equipment;
    }

    private HcGrindingProductionRecordDO resolveLatestConfirmedConsumableSyncRecord(Long equipmentId) {
        HcGrindingProductionRecordDO record = ledgerMapper.selectLatestConfirmedByEquipment(equipmentId);
        if (record == null) {
            throw invalidParamException("所选设备没有已确认的磨皮生产记录，无法同步耗材");
        }
        if (record.getRecordTime() == null || record.getRecordTime().getYear() < 2000) {
            throw invalidParamException("最近已确认生产记录的完工时间无效，无法同步耗材");
        }
        if (record.getSandpaperLife() == null || record.getSandpaperLifeDays() == null
                || record.getGuideClothLife() == null) {
            throw invalidParamException("最近已确认生产记录缺少砂纸累计寿命、砂纸累计天数或导布累计寿命，无法完整同步");
        }
        if (negative(record.getSandpaperLife()) || negative(record.getSandpaperLifeDays())
                || negative(record.getGuideClothLife())) {
            throw invalidParamException("最近已确认生产记录存在负数耗材寿命，无法同步");
        }
        return record;
    }

    private HcGrindingProductionRecordConsumableSyncRespVO toConsumableSyncResp(
            HcGrindingProductionRecordDO record, HcEquipmentDO equipment, boolean synchronizedState,
            String message) {
        HcGrindingProductionRecordConsumableSyncRespVO resp = new HcGrindingProductionRecordConsumableSyncRespVO();
        resp.setSourceRecordId(record.getId());
        resp.setEquipmentId(equipment.getId());
        resp.setEquipmentCode(equipment.getEquipmentCode());
        resp.setEquipmentName(equipment.getEquipmentName());
        resp.setSourceRecordTime(record.getRecordTime());
        resp.setSandpaperLife(record.getSandpaperLife());
        // 历史数据可能保存为0；报工页面统一按当天为第1天展示和计算。
        resp.setSandpaperLifeDays(normalizeSandpaperLifeDays(record));
        resp.setGuideClothLife(record.getGuideClothLife());
        resp.setSynced(synchronizedState);
        resp.setMessage(message);
        return resp;
    }

    private void validateConsumableSyncState(HcEquipmentConsumableStateDO state,
                                              HcGrindingProductionRecordDO record,
                                              String sourceBatchNo, String consumableName) {
        // 同步审计按来源完工顺序判断；即使累计值相同，也不能忽略后续真实使用或更换。
        if (isStateNewerThanRecord(state, record)) {
            throw invalidParamException("设备当前" + consumableName + "状态已有晚于来源生产记录的耗材事件，请先处理更晚数据");
        }
        String currentBatchNo = state == null ? null : trim(state.getBatchNo());
        String expectedBatchNo = trim(sourceBatchNo);
        if (currentBatchNo != null && !"-".equals(currentBatchNo)
                && expectedBatchNo != null && !"-".equals(expectedBatchNo)
                && !Objects.equals(currentBatchNo, expectedBatchNo)) {
            throw invalidParamException("设备当前" + consumableName + "批号与来源生产记录不一致，不能覆盖同步");
        }
    }

    private boolean syncSandpaperStateFromLatestConfirmedRecord(HcGrindingProductionRecordDO record,
                                                                  HcEquipmentDO equipment,
                                                                  HcEquipmentConsumableStateDO state,
                                                                  LocalDateTime syncTime, Long operatorId,
                                                                  String operatorName) {
        boolean newState = state == null;
        if (newState) {
            state = buildConsumableState(equipment, SANDPAPER, first(record.getSandpaperBatchNo(), "-"), syncTime);
            state.setLimitLength(DEFAULT_SANDPAPER_LIMIT_LENGTH);
        }
        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = state.getUsedLength();
        String effectiveBatchNo = first(record.getSandpaperBatchNo(), state.getBatchNo(), "-");
        LocalDateTime expectedReplaceTime = resolveSandpaperReplaceTime(record);
        boolean changed = newState
                || !sameDecimal(state.getUsedLength(), record.getSandpaperLife())
                || !Objects.equals(state.getLastReplaceTime(), expectedReplaceTime)
                || !Objects.equals(trim(state.getBatchNo()), trim(effectiveBatchNo));
        if (!changed) {
            return false;
        }
        state.setBatchNo(effectiveBatchNo);
        state.setUsedLength(record.getSandpaperLife());
        state.setLastReplaceTime(expectedReplaceTime);
        applyStateAuditFields(state, syncTime, operatorId, operatorName);
        if (newState) {
            consumableStateMapper.insert(state);
        } else {
            consumableStateMapper.updateById(state);
        }
        insertProductionRecordConsumableSyncEvent(state, record, beforeBatchNo, beforeUseCount,
                beforeUsedLength, syncTime, operatorId, operatorName);
        return true;
    }

    private boolean syncGuideClothStateFromLatestConfirmedRecord(HcGrindingProductionRecordDO record,
                                                                   HcEquipmentDO equipment,
                                                                   HcEquipmentConsumableStateDO state,
                                                                   LocalDateTime syncTime, Long operatorId,
                                                                   String operatorName) {
        boolean newState = state == null;
        if (newState) {
            state = buildConsumableState(equipment, GUIDE_CLOTH, first(record.getGuideClothBatchNo(), "-"), syncTime);
            state.setLimitCount(DEFAULT_GUIDE_CLOTH_LIMIT_COUNT);
        }
        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = state.getUsedLength();
        String effectiveBatchNo = first(record.getGuideClothBatchNo(), state.getBatchNo(), "-");
        boolean changed = newState
                || !Objects.equals(state.getUseCount(), record.getGuideClothLife())
                || !Objects.equals(trim(state.getBatchNo()), trim(effectiveBatchNo));
        if (!changed) {
            return false;
        }
        state.setBatchNo(effectiveBatchNo);
        state.setUseCount(record.getGuideClothLife());
        applyStateAuditFields(state, syncTime, operatorId, operatorName);
        if (newState) {
            consumableStateMapper.insert(state);
        } else {
            consumableStateMapper.updateById(state);
        }
        insertProductionRecordConsumableSyncEvent(state, record, beforeBatchNo, beforeUseCount,
                beforeUsedLength, syncTime, operatorId, operatorName);
        return true;
    }

    private void insertProductionRecordConsumableSyncEvent(HcEquipmentConsumableStateDO state,
                                                             HcGrindingProductionRecordDO record,
                                                             String beforeBatchNo, Integer beforeUseCount,
                                                             BigDecimal beforeUsedLength,
                                                             LocalDateTime syncTime, Long operatorId,
                                                             String operatorName) {
        BigDecimal beforeLength = beforeUsedLength == null ? BigDecimal.ZERO : beforeUsedLength;
        BigDecimal afterLength = state.getUsedLength() == null ? BigDecimal.ZERO : state.getUsedLength();
        int beforeCount = beforeUseCount == null ? 0 : beforeUseCount;
        int afterCount = state.getUseCount() == null ? 0 : state.getUseCount();
        consumableEventMapper.insert(HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(ROUGH_GRINDING_PROCESS)
                .processName(ROUGH_GRINDING_PROCESS_NAME)
                .consumableType(state.getConsumableType())
                .eventType(CONSUMABLE_EVENT_ADJUST)
                .planNo("")
                .operationCode("")
                .operationName("")
                .grindingStage("")
                .bizType(PRODUCTION_RECORD_CONSUMABLE_SYNC_BIZ_TYPE)
                .bizId(record.getId())
                .beforeBatchNo(first(beforeBatchNo, "-"))
                .beforeMaterialCode("")
                .afterBatchNo(state.getBatchNo())
                .afterMaterialCode("")
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(afterCount - beforeCount)
                .onlineQuantity(BigDecimal.ZERO)
                .offlineQuantity(BigDecimal.ZERO)
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(afterLength.subtract(beforeLength))
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(syncTime)
                .remark(PRODUCTION_RECORD_CONSUMABLE_SYNC_REMARK + "，来源生产记录ID=" + record.getId()
                        + "，来源完工时间=" + DATE_TIME.format(record.getRecordTime()))
                .tenantId(state.getTenantId())
                .build());
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        BigDecimal safeLeft = left == null ? BigDecimal.ZERO : left;
        BigDecimal safeRight = right == null ? BigDecimal.ZERO : right;
        return safeLeft.compareTo(safeRight) == 0;
    }

    /**
     * 研发手工记录确认后，按其累计快照回写设备状态。历史补录和已发生更晚报工的状态都不得倒灌。
     */
    private void syncConfirmedManualConsumableState(HcGrindingProductionRecordDO record, Long operatorId,
                                                    String operatorName) {
        if (record.getEquipmentId() == null || record.getRecordTime() == null) {
            return;
        }
        HcGrindingProductionRecordDO latest = ledgerMapper.selectLatestConfirmedByEquipment(record.getEquipmentId());
        if (!canSyncConfirmedManualRecord(record, latest)) {
            return;
        }
        HcEquipmentDO equipment = equipmentMapper.selectById(record.getEquipmentId());
        if (equipment == null) {
            return;
        }
        syncSandpaperState(record, equipment, operatorId, operatorName);
        // 旧砂纸拆分记录仅承载旧砂纸用量，导布仍由新砂纸记录统一确认，避免一笔研发记录把导布寿命重复写入两次。
        if (!isManualSandpaperSplitOldSegment(record)) {
            syncGuideClothState(record, equipment, operatorId, operatorName);
        }
    }

    /**
     * 同组旧砂纸记录与新砂纸记录具有同一完工时间。确认阶段会先将两行都置为已确认，
     * 因此“最新已确认记录”会指向序号2；此处允许序号1仅为同组序号2让行，保证旧砂纸终态
     * 和后续 REPLACE 审计事件均被写入。若存在其他更晚记录，仍严格禁止历史记录倒灌状态。
     */
    private boolean canSyncConfirmedManualRecord(HcGrindingProductionRecordDO record,
                                                   HcGrindingProductionRecordDO latest) {
        if (latest == null) {
            return false;
        }
        if (Objects.equals(latest.getId(), record.getId())) {
            return true;
        }
        return isManualSandpaperSplitOldSegment(record)
                && Objects.equals(record.getManualSplitGroupNo(), latest.getManualSplitGroupNo())
                && Integer.valueOf(2).equals(latest.getSourceSegmentNo());
    }

    private boolean isManualSandpaperSplitOldSegment(HcGrindingProductionRecordDO record) {
        return isManualSandpaperSplitRecord(record) && Integer.valueOf(1).equals(record.getSourceSegmentNo());
    }

    private void syncSandpaperState(HcGrindingProductionRecordDO record, HcEquipmentDO equipment,
                                    Long operatorId, String operatorName) {
        String batchNo = trim(record.getSandpaperBatchNo());
        if (record.getSandpaperLife() == null && batchNo == null) {
            return;
        }
        HcEquipmentConsumableStateDO state = consumableStateMapper.selectOneByEquipmentAndType(
                equipment.getId(), ROUGH_GRINDING_PROCESS, SANDPAPER);
        if (isStateNewerThanRecord(state, record)) {
            return;
        }
        boolean newState = state == null;
        if (newState) {
            state = buildConsumableState(equipment, SANDPAPER, first(batchNo, "-"), record.getRecordTime());
            state.setLimitLength(DEFAULT_SANDPAPER_LIMIT_LENGTH);
        }
        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = state.getUsedLength();
        String effectiveBatchNo = first(batchNo, state.getBatchNo(), "-");
        boolean batchChanged = !Objects.equals(trim(beforeBatchNo), trim(effectiveBatchNo));
        boolean physicalReplace = Boolean.TRUE.equals(record.getSandpaperChanged());
        boolean replaceEvent = physicalReplace || batchChanged;
        state.setBatchNo(effectiveBatchNo);
        if (record.getSandpaperLife() != null) {
            state.setUsedLength(record.getSandpaperLife());
        }
        if (newState || replaceEvent) {
            state.setLastReplaceTime(physicalReplace ? record.getRecordTime() : resolveSandpaperReplaceTime(record));
            state.setLastReplacePlanNo(null);
            state.setLastReplaceReason(manualConsumableEventReason(record, SANDPAPER, replaceEvent));
        }
        applyStateAuditFields(state, record.getRecordTime(), operatorId, operatorName);
        if (newState) {
            consumableStateMapper.insert(state);
        } else {
            consumableStateMapper.updateById(state);
        }
        insertManualConfirmConsumableEvent(state, record, beforeBatchNo, beforeUseCount, beforeUsedLength,
                replaceEvent, manualConsumableEventReason(record, SANDPAPER, replaceEvent), operatorId, operatorName);
    }

    private void syncGuideClothState(HcGrindingProductionRecordDO record, HcEquipmentDO equipment,
                                     Long operatorId, String operatorName) {
        String batchNo = trim(record.getGuideClothBatchNo());
        if (record.getGuideClothLife() == null && batchNo == null) {
            return;
        }
        HcEquipmentConsumableStateDO state = consumableStateMapper.selectOneByEquipmentAndType(
                equipment.getId(), ROUGH_GRINDING_PROCESS, GUIDE_CLOTH);
        if (isStateNewerThanRecord(state, record)) {
            return;
        }
        boolean newState = state == null;
        if (newState) {
            state = buildConsumableState(equipment, GUIDE_CLOTH, first(batchNo, "-"), record.getRecordTime());
            state.setLimitCount(DEFAULT_GUIDE_CLOTH_LIMIT_COUNT);
        }
        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = state.getUsedLength();
        String effectiveBatchNo = first(batchNo, state.getBatchNo(), "-");
        boolean batchChanged = !Objects.equals(trim(beforeBatchNo), trim(effectiveBatchNo));
        boolean physicalReplace = Boolean.TRUE.equals(record.getGuideClothChanged());
        boolean replaceEvent = physicalReplace || batchChanged;
        state.setBatchNo(effectiveBatchNo);
        if (record.getGuideClothLife() != null) {
            state.setUseCount(record.getGuideClothLife());
        }
        if (newState || replaceEvent) {
            state.setLastReplaceTime(record.getRecordTime());
            state.setLastReplacePlanNo(null);
            state.setLastReplaceReason(manualConsumableEventReason(record, GUIDE_CLOTH, replaceEvent));
        }
        applyStateAuditFields(state, record.getRecordTime(), operatorId, operatorName);
        if (newState) {
            consumableStateMapper.insert(state);
        } else {
            consumableStateMapper.updateById(state);
        }
        insertManualConfirmConsumableEvent(state, record, beforeBatchNo, beforeUseCount, beforeUsedLength,
                replaceEvent, manualConsumableEventReason(record, GUIDE_CLOTH, replaceEvent), operatorId, operatorName);
    }

    private HcEquipmentConsumableStateDO buildConsumableState(HcEquipmentDO equipment, String consumableType,
                                                               String batchNo, LocalDateTime recordTime) {
        return HcEquipmentConsumableStateDO.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(empty(equipment.getEquipmentCode()))
                .equipmentName(empty(equipment.getEquipmentName()))
                .workCenterId(equipment.getWorkCenterId())
                .workCenterCode(empty(equipment.getWorkCenterCode()))
                .workCenterName(empty(equipment.getWorkCenterName()))
                .processCode(ROUGH_GRINDING_PROCESS)
                .processName(ROUGH_GRINDING_PROCESS_NAME)
                .consumableType(consumableType)
                .batchNo(batchNo)
                .materialCode("")
                .materialName("")
                .onlineQuantity(BigDecimal.ZERO)
                .lastReplaceTime(recordTime)
                .lastReplacePlanNo("")
                .lastReplaceReason("")
                .useCount(0)
                .usedLength(BigDecimal.ZERO)
                .warningFlag(0)
                .status(CONSUMABLE_IN_USE)
                .lastOperatorName("")
                .remark("")
                .tenantId(equipment.getTenantId())
                .build();
    }

    /**
     * 生产记录同步以来源完工顺序为准；event_time / last_event_time 仍保留真实操作时间。
     * 同时检查所有候选事件，避免较晚的同步审计遮住真实 USE / REPLACE 或人工调整。
     */
    boolean isStateNewerThanRecord(HcEquipmentConsumableStateDO state,
                                   HcGrindingProductionRecordDO record) {
        if (state == null) {
            return false;
        }
        if (state.getId() == null || record.getRecordTime() == null
                || state.getTenantId() == null
                || !Objects.equals(state.getTenantId(), record.getTenantId())
                || !Objects.equals(state.getEquipmentId(), record.getEquipmentId())) {
            return true;
        }
        List<HcEquipmentConsumableEventDO> events = consumableEventMapper.selectConsumableSyncCandidates(
                state.getId(), state.getTenantId(), record.getRecordTime());
        Map<Long, HcGrindingProductionRecordDO> sources = new HashMap<>();
        boolean lastEventResolved = false;
        for (HcEquipmentConsumableEventDO event : events) {
            if (!Objects.equals(state.getTenantId(), event.getTenantId())
                    || !Objects.equals(state.getEquipmentId(), event.getEquipmentId())
                    || !Objects.equals(state.getProcessCode(), event.getProcessCode())
                    || !Objects.equals(state.getConsumableType(), event.getConsumableType())) {
                return true;
            }
            if (event.getEventTime() == null) {
                return true;
            }
            lastEventResolved |= Objects.equals(event.getEventTime(), state.getLastEventTime());
            boolean recordAdjustment = CONSUMABLE_EVENT_ADJUST.equals(event.getEventType())
                    && (PRODUCTION_RECORD_CONSUMABLE_SYNC_BIZ_TYPE.equals(event.getBizType())
                        || MANUAL_CONFIRM_BIZ_TYPE.equals(event.getBizType()));
            if (!recordAdjustment) {
                if (event.getEventTime().isAfter(record.getRecordTime())) {
                    return true;
                }
                continue;
            }
            HcGrindingProductionRecordDO source = event.getBizId() == null ? null
                    : sources.computeIfAbsent(event.getBizId(), ledgerMapper::selectById);
            // 来源缺失或跨设备/租户时不能据此放行；旧于本次完工的历史事件不额外扩大阻挡。
            if (source == null || Boolean.TRUE.equals(source.getDeleted())
                    || !CONFIRMED.equals(source.getStatus()) || source.getRecordTime() == null
                    || source.getRecordTime().getYear() < 2000
                    || !Objects.equals(source.getTenantId(), state.getTenantId())
                    || !Objects.equals(source.getEquipmentId(), state.getEquipmentId())) {
                if (!event.getEventTime().isBefore(record.getRecordTime())) {
                    return true;
                }
                continue;
            }
            if (compareConsumableSourceOrder(source, record) > 0) {
                return true;
            }
        }
        // 缺少对应流水的较新状态仍保守阻挡，不能将未知调整当成旧记录同步。
        return state.getLastEventTime() != null && state.getLastEventTime().isAfter(record.getRecordTime())
                && !lastEventResolved;
    }

    private int compareConsumableSourceOrder(HcGrindingProductionRecordDO left,
                                             HcGrindingProductionRecordDO right) {
        int timeOrder = left.getRecordTime().compareTo(right.getRecordTime());
        if (timeOrder != 0) return timeOrder;
        // 与 selectLatestConfirmedByEquipment 的 record_time、source_segment_no、id 顺序一致。
        int segmentOrder = Integer.compare(left.getSourceSegmentNo() == null ? 0 : left.getSourceSegmentNo(),
                right.getSourceSegmentNo() == null ? 0 : right.getSourceSegmentNo());
        if (segmentOrder != 0) return segmentOrder;
        return Long.compare(left.getId() == null ? 0L : left.getId(), right.getId() == null ? 0L : right.getId());
    }

    private LocalDateTime resolveSandpaperReplaceTime(HcGrindingProductionRecordDO record) {
        return record.getRecordTime().minusDays(normalizeSandpaperLifeDays(record) - 1L);
    }

    private int normalizeSandpaperLifeDays(HcGrindingProductionRecordDO record) {
        int lifeDays = record.getSandpaperLifeDays() == null ? 0 : record.getSandpaperLifeDays();
        return Math.max(1, lifeDays);
    }

    private void applyStateAuditFields(HcEquipmentConsumableStateDO state, LocalDateTime eventTime,
                                       Long operatorId, String operatorName) {
        state.setWarningFlag(calcConsumableWarningFlag(state));
        state.setStatus(CONSUMABLE_IN_USE);
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
    }

    private int calcConsumableWarningFlag(HcEquipmentConsumableStateDO state) {
        boolean lengthWarn = state.getLimitLength() != null && state.getUsedLength() != null
                && state.getUsedLength().compareTo(state.getLimitLength()) >= 0;
        boolean countWarn = state.getLimitCount() != null && state.getUseCount() != null
                && state.getUseCount() >= state.getLimitCount();
        return lengthWarn || countWarn ? 1 : 0;
    }

    private void insertManualConfirmConsumableEvent(HcEquipmentConsumableStateDO state,
                                                     HcGrindingProductionRecordDO record,
                                                     String beforeBatchNo, Integer beforeUseCount,
                                                     BigDecimal beforeUsedLength, boolean replaceEvent,
                                                     String replaceReason, Long operatorId,
                                                     String operatorName) {
        BigDecimal beforeLength = beforeUsedLength == null ? BigDecimal.ZERO : beforeUsedLength;
        BigDecimal afterLength = state.getUsedLength() == null ? BigDecimal.ZERO : state.getUsedLength();
        int beforeCount = beforeUseCount == null ? 0 : beforeUseCount;
        int afterCount = state.getUseCount() == null ? 0 : state.getUseCount();
        consumableEventMapper.insert(HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(ROUGH_GRINDING_PROCESS)
                .processName(ROUGH_GRINDING_PROCESS_NAME)
                .consumableType(state.getConsumableType())
                .eventType(replaceEvent ? CONSUMABLE_EVENT_REPLACE : CONSUMABLE_EVENT_ADJUST)
                .planNo("")
                .operationCode("")
                .operationName("")
                .grindingStage("")
                .bizType(MANUAL_CONFIRM_BIZ_TYPE)
                .bizId(record.getId())
                .beforeBatchNo(first(beforeBatchNo, "-"))
                .beforeMaterialCode("")
                .afterBatchNo(state.getBatchNo())
                .afterMaterialCode("")
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(afterCount - beforeCount)
                .onlineQuantity(BigDecimal.ZERO)
                .offlineQuantity(BigDecimal.ZERO)
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(afterLength.subtract(beforeLength))
                .replaceReason(replaceReason)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(record.getRecordTime())
                .remark(MANUAL_CONFIRM_REMARK + "，来源生产记录ID=" + record.getId()
                        + (replaceEvent ? "，按物理更换处理" : ""))
                .tenantId(state.getTenantId())
                .build());
    }

    private String manualConsumableEventReason(HcGrindingProductionRecordDO record, String consumableType,
                                                boolean replaceEvent) {
        if (!replaceEvent) {
            return MANUAL_CONFIRM_REMARK;
        }
        if (SANDPAPER.equals(consumableType)) {
            return first(record.getSandpaperReplaceReason(), record.getReplaceReason(), MANUAL_CONFIRM_REMARK);
        }
        return first(record.getGuideClothReplaceReason(), record.getReplaceReason(), MANUAL_CONFIRM_REMARK);
    }

    private boolean isSourceRecord(HcGrindingProductionRecordDO row) {
        return row != null && row.getSourceDetailId() != null;
    }

    private LocalDateTime parseDateTime(String value, int row, String field,
                                        HcGrindingProductionRecordImportRespVO resp) {
        String text = trim(value);
        if (text == null) { fail(resp, "第" + row + "行：" + field + "不能为空"); return null; }
        String normalized = text.replace('/', '-').replace('T', ' ');
        for (DateTimeFormatter formatter : List.of(DATE_TIME, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"), DateTimeFormatter.ofPattern("yyyy-M-d H:m"))) {
            try { return LocalDateTime.parse(normalized, formatter); } catch (DateTimeParseException ignored) { }
        }
        fail(resp, "第" + row + "行：" + field + "格式应为yyyy-MM-dd HH:mm:ss");
        return null;
    }

    private String required(String value, int row, String field, HcGrindingProductionRecordImportRespVO resp) {
        String text = trim(value);
        if (text == null) fail(resp, "第" + row + "行：" + field + "不能为空");
        return text;
    }

    private void nonNegative(Number value, int row, String field, HcGrindingProductionRecordImportRespVO resp) {
        if (negative(value)) fail(resp, "第" + row + "行：" + field + "不能为负数");
    }

    private boolean negative(Number value) { return value != null && new BigDecimal(value.toString()).signum() < 0; }
    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private boolean isZeroOrNull(BigDecimal value) { return value == null || value.signum() == 0; }
    private void fail(HcGrindingProductionRecordImportRespVO resp, String message) {
        resp.getFailures().add(message); resp.setFailureCount(resp.getFailures().size());
    }
    private boolean blankRow(HcGrindingProductionRecordImportExcelVO r) {
        return r == null || trim(r.getCompletionTime()) == null && trim(r.getModelCode()) == null
                && trim(r.getMaterialCode()) == null && trim(r.getMotherBatchNo()) == null
                && trim(r.getBatchNo()) == null && trim(r.getRecordRole()) == null
                && trim(r.getSourceBizType()) == null && trim(r.getSegmentMark()) == null
                && r.getStartPosition() == null && r.getFirstAllocationId() == null && r.getInputLength() == null
                && r.getOutputLength() == null && trim(r.getPassName()) == null && trim(r.getLegacyPassName()) == null
                && r.getSandpaperLife() == null
                && r.getSandpaperLifeDays() == null
                && trim(r.getSandpaperBatchNo()) == null && r.getGuideClothLife() == null
                && trim(r.getGuideClothBatchNo()) == null && trim(r.getReplaceReason()) == null
                && trim(r.getRecorderName()) == null && trim(r.getRemark()) == null;
    }
    private static String normalizePass(String value) {
        String text = trim(value);
        if (text == null) return null;
        if ("FIRST".equalsIgnoreCase(text) || "一次".equals(text) || "一磨".equals(text) || "一次磨皮".equals(text)) return "FIRST";
        if ("SECOND".equalsIgnoreCase(text) || "二次".equals(text) || "二磨".equals(text) || "二次磨皮".equals(text)) return "SECOND";
        if ("THIRD".equalsIgnoreCase(text) || "三次".equals(text) || "三磨".equals(text) || "三次磨皮".equals(text)) return "THIRD";
        if ("FOURTH".equalsIgnoreCase(text) || "四次".equals(text) || "四磨".equals(text) || "四次磨皮".equals(text)) return "FOURTH";
        return null;
    }
    private String normalizePassOptional(String value) { return trim(value) == null ? null : normalizePass(value); }
    private String normalizeRecordRole(String value) {
        String normalized = normalizeRecordRoleOptional(value);
        if (normalized == null) throw invalidParamException("生产记录来源业务类型不正确");
        return normalized;
    }
    private String normalizeRecordRoleOptional(String value) {
        String text = trim(value);
        if (text == null) return null;
        String normalized = text.toUpperCase();
        return RECORD_ROLES.contains(normalized) ? normalized : null;
    }
    private String normalizeSegmentMark(String value) {
        String text = trim(value);
        if (text == null) return null;
        String normalized = "不分段".equals(text) ? "NONE" : text.toUpperCase();
        return ALLOCATION_SEGMENTS.contains(normalized) ? normalized : null;
    }
    private String displaySegmentMark(String value) {
        return "NONE".equals(normalizeSegmentMark(value)) ? "不分段" : normalizeSegmentMark(value);
    }
    private String defaultSourceBizType(String passType) {
        if ("FIRST".equals(passType)) return ROLE_FIRST_ORIGINAL;
        if ("SECOND".equals(passType)) return ROLE_SECOND;
        return null;
    }
    private void validateSourceRoleMatchesPass(String sourceBizType, String passType) {
        if (sourceBizType == null || passType == null) return;
        if ((ROLE_SECOND.equals(sourceBizType) && !"SECOND".equals(passType))
                || (!ROLE_SECOND.equals(sourceBizType) && !"FIRST".equals(passType))) {
            throw invalidParamException("生产记录来源业务类型与磨皮次数不匹配");
        }
    }
    private static String passName(String passType) {
        return switch (passType) {
            case "FIRST" -> "一次";
            case "SECOND" -> "二次";
            case "THIRD" -> "三次";
            case "FOURTH" -> "四次";
            default -> null;
        };
    }
    private String format(LocalDateTime value) { return value == null ? null : DATE_TIME.format(value); }
    private String key(Object... values) { return java.util.Arrays.stream(values).map(String::valueOf)
            .collect(java.util.stream.Collectors.joining("|")); }
    private String empty(String value) { String text = trim(value); return text == null ? "" : text; }
    private static String trim(String value) { String text = value == null ? null : value.trim(); return text == null || text.isEmpty() ? null : text; }
    private String first(String... values) { for (String value : values) { String text = trim(value); if (text != null) return text; } return null; }

    private static class ImportRow {
        Long existingId; LocalDate reportDate; String modelCode; String materialCode; String motherBatchNo;
        String batchNo; String recordRole; String sourceBizType; String segmentMark; BigDecimal startPosition;
        Long firstAllocationId;
        BigDecimal inputLength; BigDecimal outputLength; String passType; BigDecimal sandpaperLife; Integer sandpaperLifeDays;
        String sandpaperBatchNo; Integer guideClothLife; String guideClothBatchNo; String replaceReason;
        String recorderName; LocalDateTime recordTime; String remark;
        HcGrindingProductionRecordDO toDO() {
            return HcGrindingProductionRecordDO.builder().reportDate(reportDate).modelCode(modelCode)
                    .materialCode(materialCode).motherBatchNo(motherBatchNo).batchNo(batchNo)
                    .recordRole(recordRole).sourceBizType(sourceBizType).segmentMark(segmentMark)
                    .startPosition(startPosition).firstAllocationId(firstAllocationId)
                    .inputLength(inputLength).outputLength(outputLength)
                    .passType(passType).passName(passName(passType))
                    .sandpaperLife(sandpaperLife).sandpaperLifeDays(sandpaperLifeDays)
                    .sandpaperBatchNo(sandpaperBatchNo).guideClothLife(guideClothLife)
                    .guideClothBatchNo(guideClothBatchNo).replaceReason(replaceReason).recorderName(recorderName)
                    .recordTime(recordTime).remark(remark).build();
        }
    }

    private void finishConsumption(cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingConsumptionDO booking,
                                   Long id, HcGrindingProductionRecordSaveReqVO req) {
        grindingConsumptionService.finish(booking, id, id, req.getConsumption(),
                Boolean.TRUE.equals(req.getSandpaperChanged()), req.getSandpaperBatchNo(),
                Boolean.TRUE.equals(req.getGuideClothChanged()), req.getGuideClothBatchNo(), req.getRecordTime(), null, req.getBatchNo());
    }

    private void fillConsumption(HcGrindingProductionRecordDO row) {
        if (SOURCE_MANUAL.equals(row.getSourceType())) {
            if (isManualSandpaperSplitOldSegment(row)) return;
        } else if (Integer.valueOf(1).equals(row.getSourceSegmentNo())) {
            // 只有存在新砂纸行时才跳过旧行；普通单行报工同样使用段号1。
            boolean split = ledgerMapper.selectListBySource(row.getSourceBizType(), row.getPassType(), row.getSourceDetailId())
                    .stream().anyMatch(item -> Integer.valueOf(2).equals(item.getSourceSegmentNo()));
            if (split) return;
        }
        String type = SOURCE_MANUAL.equals(row.getSourceType()) ? "MANUAL" : row.getPassType();
        row.setConsumption(grindingConsumptionService.get(type,
                SOURCE_MANUAL.equals(row.getSourceType()) ? row.getId() : row.getSourceDetailId()));
    }
}
