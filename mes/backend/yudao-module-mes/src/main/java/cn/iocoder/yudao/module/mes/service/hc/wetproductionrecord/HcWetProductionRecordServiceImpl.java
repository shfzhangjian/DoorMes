package cn.iocoder.yudao.module.mes.service.hc.wetproductionrecord;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordInitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordInitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.wetproductionrecord.HcWetProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.guideclothrecord.HcGuideClothRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.wetproductionrecord.HcWetProductionRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionServiceImpl;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcProductionRecordPadTypeResolver;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcWetProductionRecordServiceImpl implements HcWetProductionRecordService {

    private static final String STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_MENU_CODE_FORMULA_REPORT = "FORMULA_REPORT";
    private static final String SOURCE_MENU_CODE_WET_REPORT = "WET_REPORT";
    private static final String PROCESS_CODE_WET = "WET";
    private static final String CONSUMABLE_TYPE_GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final String EVENT_TYPE_RND_MANUAL_USE = "RND_MANUAL_USE";
    private static final String RECORD_SOURCE_RND_MANUAL = "RND_MANUAL";
    private static final String DATA_SOURCE_RND_MANUAL = "研发手工登记";
    private static final String YES = "Y";
    private static final String NO = "N";
    private static final int MIN_VALID_BUSINESS_YEAR = 2000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcWetProductionRecordMapper hcWetProductionRecordMapper;

    @Resource
    private HcProcessReportMapper hcProcessReportMapper;

    @Resource
    private HcEquipmentConsumableEventMapper hcEquipmentConsumableEventMapper;

    @Resource
    private HcGuideClothRecordMapper hcGuideClothRecordMapper;

    @Resource
    private HcProductionRecordRevisionService productionRecordRevisionService;

    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HcWetProductionRecordSaveReqVO reqVO) {
        normalizeReq(reqVO);
        validateRecordTime(reqVO.getRecordTime());
        validateBizKeyAvailable(reqVO, null);
        HcWetProductionRecordDO entity = BeanUtils.toBean(reqVO, HcWetProductionRecordDO.class);
        entity.setStatus(STATUS_WAIT_CONFIRM);
        entity.setConfirmerName(null);
        entity.setConfirmTime(null);
        entity.setTenantId(TenantContextHolder.getTenantId());
        hcWetProductionRecordMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HcWetProductionRecordSaveReqVO reqVO) {
        HcWetProductionRecordDO existing = validateExists(reqVO.getId());
        validateWaitConfirm(existing);
        normalizeReq(reqVO);
        validateRecordTime(reqVO.getRecordTime());
        validateBizKeyAvailable(reqVO, existing.getId());
        HcWetProductionRecordDO updateObj = BeanUtils.toBean(reqVO, HcWetProductionRecordDO.class);
        updateObj.setStatus(STATUS_WAIT_CONFIRM);
        updateObj.setConfirmerName(null);
        updateObj.setConfirmTime(null);
        hcWetProductionRecordMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HcWetProductionRecordDO existing = validateExists(id);
        validateWaitConfirm(existing);
        hcWetProductionRecordMapper.physicalDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw invalidParamException("请选择要删除的湿法生产记录");
        }
        List<HcWetProductionRecordDO> records = hcWetProductionRecordMapper.selectByRecordIds(ids);
        if (records.size() != ids.size()) {
            throw invalidParamException("部分湿法生产记录不存在，请刷新后重试");
        }
        records.forEach(this::validateWaitConfirm);
        hcWetProductionRecordMapper.physicalDeleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer confirm(HcWetProductionRecordConfirmReqVO reqVO) {
        List<Long> ids = reqVO.getIds();
        if (ids == null || ids.isEmpty()) {
            throw invalidParamException("请选择要确认的湿法生产记录");
        }
        List<HcWetProductionRecordDO> records = hcWetProductionRecordMapper.selectByRecordIds(ids);
        if (records.size() != ids.size()) {
            throw invalidParamException("部分湿法生产记录不存在，请刷新后重试");
        }
        String confirmerName = firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (isBlank(confirmerName)) {
            confirmerName = loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
        }
        LocalDateTime confirmTime = LocalDateTime.now();
        int confirmedCount = 0;
        for (HcWetProductionRecordDO record : records) {
            if (STATUS_CONFIRMED.equals(record.getStatus())) {
                continue;
            }
            HcWetProductionRecordDO updateObj = new HcWetProductionRecordDO();
            updateObj.setId(record.getId());
            updateObj.setStatus(STATUS_CONFIRMED);
            updateObj.setConfirmerName(confirmerName);
            updateObj.setConfirmTime(confirmTime);
            hcWetProductionRecordMapper.updateById(updateObj);
            confirmedCount++;
        }
        if (confirmedCount == 0) {
            throw invalidParamException("所选湿法生产记录均已确认，无需重复确认");
        }
        return confirmedCount;
    }

    @Override
    public HcWetProductionRecordDO get(Long id) {
        return hcWetProductionRecordMapper.selectById(id);
    }

    @Override
    public PageResult<HcWetProductionRecordDO> getPage(HcWetProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        return hcWetProductionRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<HcWetProductionRecordRespVO> getReportPage(HcWetProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        List<HcWetProductionRecordRespVO> rows = buildReportRows(reqVO);
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        if (pageSize <= 0) {
            return new PageResult<>(rows, (long) rows.size());
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    @Override
    public List<HcWetProductionRecordImportExcelVO> buildExportList(HcWetProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        return hcWetProductionRecordMapper.selectList(reqVO).stream()
                .map(this::toImportExcel)
                .toList();
    }

    @Override
    public List<HcWetProductionRecordImportExcelVO> buildReportExportList(HcWetProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        return buildReportRows(reqVO).stream().map(this::toReportExcel).toList();
    }

    private List<HcWetProductionRecordRespVO> buildReportRows(HcWetProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcProcessReportDO> query = new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE_WET_REPORT)
                .eq(HcProcessReportDO::getReportType, "END")
                .eq(HcProcessReportDO::getDeleted, false)
                .geIfPresent(HcProcessReportDO::getReportDate, reqVO.getRecordDateStart())
                .leIfPresent(HcProcessReportDO::getReportDate, reqVO.getRecordDateEnd());
        query.orderByAsc(HcProcessReportDO::getReportDate)
                .orderByAsc(HcProcessReportDO::getId);
        Map<String, WetReportInitRow> grouped = new LinkedHashMap<>();
        for (HcProcessReportDO report : hcProcessReportMapper.selectList(query)) {
            JSONObject extra = parseExtra(report.getExtraJson());
            String modelCode = firstNotBlank(report.getMotherModelCode());
            String materialCode = firstNotBlank(report.getMotherMaterialCode(), report.getMaterialCode());
            String batchNo = firstNotBlank(report.getBatchNo(), report.getParentBatchNo());
            String petModel = trimToNull(extra.getStr("petModel"));
            String petBatchNo = trimToNull(extra.getStr("petBatchNo"));
            String guideBatchNo = trimToNull(extra.getStr("guideClothBatchNo"));
            String changed = normalizeYesNo(extra.getStr("guideClothChanged"));
            String changeDesc = trimToNull(extra.getStr("guideClothChangeReason"));
            if (report.getReportDate() == null || isBlank(modelCode) || isBlank(materialCode) || isBlank(batchNo)
                    || isBlank(petBatchNo) || isBlank(guideBatchNo)) {
                continue;
            }
            if (reqVO.getBatchNo() != null && !containsIgnoreCase(batchNo, reqVO.getBatchNo())
                    && !containsIgnoreCase(petBatchNo, reqVO.getBatchNo())
                    && !containsIgnoreCase(guideBatchNo, reqVO.getBatchNo())) {
                continue;
            }
            if (reqVO.getGuideClothChanged() != null
                    && !reqVO.getGuideClothChanged().equals(changed)) {
                continue;
            }
            if (reqVO.getChangeDesc() != null && !containsIgnoreCase(changeDesc, reqVO.getChangeDesc())) {
                continue;
            }
            String key = buildKey(report.getReportDate(), modelCode, materialCode, batchNo, petModel, petBatchNo,
                    guideBatchNo);
            grouped.computeIfAbsent(key, ignored -> new WetReportInitRow(report.getReportDate(), modelCode,
                    materialCode, batchNo, petModel, petBatchNo, guideBatchNo, changed))
                    .accept(report, extra, changeDesc);
        }
        fillInputKgByFormulaBatch(grouped);
        List<HcWetProductionRecordRespVO> rows = new ArrayList<>(grouped.values().stream()
                .map(this::toReportResp)
                .toList());
        rows.addAll(buildRndManualReportRows(reqVO));
        List<HcWetProductionRecordRespVO> result = rows.stream()
                .sorted(Comparator
                        .comparing((HcWetProductionRecordRespVO item) -> item.getRecordTime() != null
                                        ? item.getRecordTime()
                                        : (item.getRecordDate() == null
                                                ? null : item.getRecordDate().atStartOfDay()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcWetProductionRecordRespVO::getRecordDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HcWetProductionRecordRespVO::getModelCode,
                                Comparator.nullsLast(String::compareTo))
                .thenComparing(HcWetProductionRecordRespVO::getBatchNo,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<String, String> padTypes = padTypeResolver.resolveByModelCodes(result.stream()
                .filter(row -> !DATA_SOURCE_RND_MANUAL.equals(row.getDataSource()))
                .map(HcWetProductionRecordRespVO::getModelCode)
                .toList());
        result.stream()
                .filter(row -> !DATA_SOURCE_RND_MANUAL.equals(row.getDataSource()))
                .forEach(row -> row.setPadType(padTypes.get(row.getModelCode())));
        return productionRecordRevisionService.applyRevisions(
                        HcProductionRecordRevisionServiceImpl.MODULE_WET, result)
                .stream()
                .filter(row -> padTypeResolver.matchesFilter(reqVO.getPadType(), row.getPadType()))
                .toList();
    }

    private List<HcWetProductionRecordRespVO> buildRndManualReportRows(HcWetProductionRecordPageReqVO reqVO) {
        List<HcEquipmentConsumableEventDO> events = hcEquipmentConsumableEventMapper.selectList(
                new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                        .eq(HcEquipmentConsumableEventDO::getProcessCode, PROCESS_CODE_WET)
                        .eq(HcEquipmentConsumableEventDO::getConsumableType, CONSUMABLE_TYPE_GUIDE_CLOTH)
                        .eq(HcEquipmentConsumableEventDO::getRecordSource, RECORD_SOURCE_RND_MANUAL)
                        .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                        .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                        .orderByDesc(HcEquipmentConsumableEventDO::getId));
        Map<Long, String> padTypesByGuideClothRecordId = resolveRndPadTypesByGuideClothRecord(events);
        Map<String, HcEquipmentConsumableEventDO> replaceEventsByGroupNo = new HashMap<>();
        for (HcEquipmentConsumableEventDO event : events) {
            if ("REPLACE".equals(event.getEventType()) && !isBlank(event.getRecordGroupNo())) {
                replaceEventsByGroupNo.putIfAbsent(event.getRecordGroupNo(), event);
            }
        }
        Map<String, HcWetProductionRecordRespVO> grouped = new LinkedHashMap<>();
        for (HcEquipmentConsumableEventDO event : events) {
            if (!EVENT_TYPE_RND_MANUAL_USE.equals(event.getEventType())) {
                continue;
            }
            LocalDateTime eventTime = event.getEventTime();
            if (eventTime == null || !isValidBusinessTime(eventTime)) {
                continue;
            }
            LocalDate recordDate = eventTime.toLocalDate();
            if (reqVO.getRecordDateStart() != null && recordDate.isBefore(reqVO.getRecordDateStart())
                    || reqVO.getRecordDateEnd() != null && recordDate.isAfter(reqVO.getRecordDateEnd())) {
                continue;
            }
            String batchNo = trimToNull(event.getProductBatchNo());
            String petBatchNo = trimToNull(event.getPetBatchNo());
            String guideClothBatchNo = firstNotBlank(event.getAfterBatchNo(), event.getBeforeBatchNo());
            if (isBlank(event.getProductModelCode()) || isBlank(event.getProductMaterialCode()) || isBlank(batchNo)
                    || isBlank(event.getPetModel()) || isBlank(petBatchNo) || isBlank(guideClothBatchNo)) {
                continue;
            }
            if (reqVO.getBatchNo() != null && !containsIgnoreCase(batchNo, reqVO.getBatchNo())
                    && !containsIgnoreCase(petBatchNo, reqVO.getBatchNo())
                    && !containsIgnoreCase(guideClothBatchNo, reqVO.getBatchNo())) {
                continue;
            }
            String key = firstNotBlank(event.getRecordGroupNo(), "RND-EVENT-" + event.getId());
            HcEquipmentConsumableEventDO replaceEvent = replaceEventsByGroupNo.get(key);
            String guideClothChanged = replaceEvent == null ? NO : YES;
            String changeDesc = replaceEvent == null ? "研发样品导布消耗" : "研发样品导布更换及消耗";
            if (reqVO.getGuideClothChanged() != null && !guideClothChanged.equals(reqVO.getGuideClothChanged())
                    || reqVO.getChangeDesc() != null && !containsIgnoreCase(changeDesc, reqVO.getChangeDesc())) {
                continue;
            }
            grouped.putIfAbsent(key, toRndManualReportResp(event, recordDate, guideClothBatchNo, guideClothChanged,
                    changeDesc, replaceEvent, padTypesByGuideClothRecordId.get(event.getGuideClothRecordId())));
        }
        return new ArrayList<>(grouped.values());
    }

    private Map<Long, String> resolveRndPadTypesByGuideClothRecord(List<HcEquipmentConsumableEventDO> events) {
        List<Long> guideClothRecordIds = events.stream()
                .filter(event -> EVENT_TYPE_RND_MANUAL_USE.equals(event.getEventType()))
                .map(HcEquipmentConsumableEventDO::getGuideClothRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (guideClothRecordIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (HcGuideClothRecordDO record : hcGuideClothRecordMapper.selectBatchIds(guideClothRecordIds)) {
            String padType = padTypeResolver.resolveByGuideClothLineCode(record.getLineCode());
            if (padType != null) {
                result.put(record.getId(), padType);
            }
        }
        return result;
    }

    private HcWetProductionRecordRespVO toRndManualReportResp(HcEquipmentConsumableEventDO event,
                                                                LocalDate recordDate,
                                                                String guideClothBatchNo,
                                                                String guideClothChanged,
                                                                String changeDesc,
                                                                HcEquipmentConsumableEventDO replaceEvent,
                                                                String padType) {
        HcWetProductionRecordRespVO target = new HcWetProductionRecordRespVO();
        target.setId(event.getId());
        target.setDataSource(DATA_SOURCE_RND_MANUAL);
        target.setRecordDate(recordDate);
        target.setModelCode(event.getProductModelCode());
        target.setPadType(padType);
        target.setMaterialCode(event.getProductMaterialCode());
        target.setBatchNo(event.getProductBatchNo());
        target.setInputKg(event.getWetInputKg());
        target.setOutputMeter(event.getWetOutputMeter());
        target.setPetModel(event.getPetModel());
        target.setPetBatchNo(event.getPetBatchNo());
        target.setGuideClothBatchNo(guideClothBatchNo);
        target.setGuideClothUseCount(event.getAfterUseCount());
        target.setGuideClothChanged(guideClothChanged);
        target.setChangeDesc(changeDesc);
        target.setRecorderName(event.getOperatorName());
        target.setRecordTime(event.getEventTime());
        String remark = appendDistinct("研发手工登记", event.getRemark());
        if (replaceEvent != null) {
            remark = appendDistinct(remark, "导布更换原因：" + replaceEvent.getReplaceReason());
        }
        target.setRemark(remark);
        return target;
    }

    private HcWetProductionRecordRespVO toReportResp(WetReportInitRow source) {
        HcWetProductionRecordRespVO target = new HcWetProductionRecordRespVO();
        target.setDataSource("量产报工");
        target.setRecordDate(source.recordDate);
        target.setModelCode(source.modelCode);
        target.setMaterialCode(source.materialCode);
        target.setBatchNo(source.batchNo);
        target.setInputKg(source.inputKg);
        target.setOutputMeter(source.outputMeter);
        target.setPetModel(source.petModel);
        target.setPetBatchNo(source.petBatchNo);
        target.setGuideClothBatchNo(source.guideClothBatchNo);
        target.setGuideClothUseCount(source.guideClothUseCount);
        target.setGuideClothChanged(source.guideClothChanged);
        target.setChangeDesc(source.changeDesc);
        target.setRecorderName(source.recorderName);
        target.setRecordTime(source.recordTime);
        target.setRemark(source.remark);
        return target;
    }

    private HcWetProductionRecordImportExcelVO toReportExcel(HcWetProductionRecordRespVO source) {
        HcWetProductionRecordImportExcelVO target = new HcWetProductionRecordImportExcelVO();
        target.setDataSource(source.getDataSource());
        target.setRecordDate(source.getRecordDate() == null ? null : DATE_FORMATTER.format(source.getRecordDate()));
        target.setModelCode(source.getModelCode());
        target.setPadType(source.getPadType());
        target.setMaterialCode(source.getMaterialCode());
        target.setBatchNo(source.getBatchNo());
        target.setInputKg(source.getInputKg());
        target.setOutputMeter(source.getOutputMeter());
        target.setPetModel(source.getPetModel());
        target.setPetBatchNo(source.getPetBatchNo());
        target.setGuideClothBatchNo(source.getGuideClothBatchNo());
        target.setGuideClothUseCount(source.getGuideClothUseCount());
        target.setGuideClothChangedName(YES.equals(source.getGuideClothChanged()) ? "是" : "否");
        target.setChangeDesc(source.getChangeDesc());
        target.setRecorderName(source.getRecorderName());
        target.setRecordTime(formatDateTime(source.getRecordTime()));
        target.setRemark(source.getRemark());
        return target;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcWetProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException {
        HcWetProductionRecordImportRespVO respVO = new HcWetProductionRecordImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }

        List<HcWetProductionRecordImportExcelVO> excelRows = ExcelUtils.read(file, HcWetProductionRecordImportExcelVO.class);
        List<WetProductionImportRow> rows = new ArrayList<>();
        Map<String, Integer> fileKeys = new HashMap<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcWetProductionRecordImportExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            int failureCountBefore = respVO.getFailures().size();
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            WetProductionImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
            if (row == null || respVO.getFailures().size() > failureCountBefore) {
                continue;
            }
            String key = buildKey(row.recordDate, row.modelCode, row.materialCode, row.batchNo,
                    row.petModel, row.petBatchNo, row.guideClothBatchNo);
            Integer previousRowNo = fileKeys.putIfAbsent(key, rowNo);
            if (previousRowNo != null) {
                addImportFailure(respVO, String.format("第%d行：业务关键词与第%d行重复，日期/型号/料号/批次/PET/导布批号必须唯一",
                        rowNo, previousRowNo));
                continue;
            }
            HcWetProductionRecordDO existing = hcWetProductionRecordMapper.selectByBizKey(row.recordDate,
                    row.modelCode, row.materialCode, row.batchNo, row.petModel, row.petBatchNo, row.guideClothBatchNo);
            if (existing != null && STATUS_CONFIRMED.equals(existing.getStatus())) {
                addImportFailure(respVO, String.format("第%d行：匹配记录已确认，禁止导入覆盖。批次=%s，PET批号=%s，导布批号=%s",
                        rowNo, row.batchNo, row.petBatchNo, row.guideClothBatchNo));
                continue;
            }
            row.existingId = existing == null ? null : existing.getId();
            rows.add(row);
        }
        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入任何数据");
            return respVO;
        }
        if (rows.isEmpty()) {
            addImportFailure(respVO, "导入文件没有有效数据行");
            return respVO;
        }

        Long tenantId = TenantContextHolder.getTenantId();
        for (WetProductionImportRow row : rows) {
            HcWetProductionRecordDO entity = row.toDO();
            entity.setStatus(STATUS_WAIT_CONFIRM);
            entity.setConfirmerName(null);
            entity.setConfirmTime(null);
            entity.setTenantId(tenantId);
            if (row.existingId == null) {
                hcWetProductionRecordMapper.insert(entity);
                respVO.setCreateCount(respVO.getCreateCount() + 1);
            } else {
                entity.setId(row.existingId);
                hcWetProductionRecordMapper.updateById(entity);
                respVO.setUpdateCount(respVO.getUpdateCount() + 1);
            }
        }
        respVO.getMessages().add(String.format("导入完成：新增 %d 条，更新 %d 条；导入记录状态均为待确认",
                respVO.getCreateCount(), respVO.getUpdateCount()));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcWetProductionRecordInitRespVO initializeFromReports(HcWetProductionRecordInitReqVO reqVO) {
        normalizeInitReq(reqVO);
        LambdaQueryWrapperX<HcProcessReportDO> query = new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE_WET_REPORT)
                .eq(HcProcessReportDO::getReportType, "END")
                .eq(HcProcessReportDO::getDeleted, false)
                .geIfPresent(HcProcessReportDO::getReportDate, reqVO.getRecordDateStart())
                .leIfPresent(HcProcessReportDO::getReportDate, reqVO.getRecordDateEnd());
        query.orderByAsc(HcProcessReportDO::getReportDate)
                .orderByAsc(HcProcessReportDO::getId);
        Map<String, WetReportInitRow> grouped = new LinkedHashMap<>();
        int invalidCount = 0;
        for (HcProcessReportDO report : hcProcessReportMapper.selectList(query)) {
            JSONObject extra = parseExtra(report.getExtraJson());
            String modelCode = firstNotBlank(report.getMotherModelCode());
            String materialCode = firstNotBlank(report.getMotherMaterialCode(), report.getMaterialCode());
            String batchNo = firstNotBlank(report.getBatchNo(), report.getParentBatchNo());
            String petModel = trimToNull(extra.getStr("petModel"));
            String petBatchNo = trimToNull(extra.getStr("petBatchNo"));
            String guideBatchNo = trimToNull(extra.getStr("guideClothBatchNo"));
            String changed = normalizeYesNo(extra.getStr("guideClothChanged"));
            String changeDesc = trimToNull(extra.getStr("guideClothChangeReason"));
            if (report.getReportDate() == null || isBlank(modelCode) || isBlank(materialCode) || isBlank(batchNo)
                    || isBlank(petModel) || isBlank(petBatchNo) || isBlank(guideBatchNo)) {
                invalidCount++;
                continue;
            }
            String batchKeyword = trimToNull(reqVO.getBatchNo());
            if (batchKeyword != null && !containsIgnoreCase(batchNo, batchKeyword)
                    && !containsIgnoreCase(petBatchNo, batchKeyword)
                    && !containsIgnoreCase(guideBatchNo, batchKeyword)) {
                continue;
            }
            if (reqVO.getGuideClothChanged() != null
                    && !reqVO.getGuideClothChanged().equals(changed)) {
                continue;
            }
            if (reqVO.getChangeDesc() != null && !containsIgnoreCase(changeDesc, reqVO.getChangeDesc())) {
                continue;
            }
            String key = buildKey(report.getReportDate(), modelCode, materialCode, batchNo, petModel, petBatchNo,
                    guideBatchNo);
            grouped.computeIfAbsent(key, ignored -> new WetReportInitRow(report.getReportDate(), modelCode,
                    materialCode, batchNo, petModel, petBatchNo, guideBatchNo, changed))
                    .accept(report, extra, changeDesc);
        }
        fillInputKgByFormulaBatch(grouped);
        HcWetProductionRecordInitRespVO respVO = new HcWetProductionRecordInitRespVO();
        respVO.setSourceCount(grouped.size());
        respVO.setSkippedCount(invalidCount);
        Long tenantId = TenantContextHolder.getTenantId();
        for (WetReportInitRow source : grouped.values()) {
            HcWetProductionRecordDO existing = hcWetProductionRecordMapper.selectByBizKey(source.recordDate,
                    source.modelCode, source.materialCode, source.batchNo, source.petModel, source.petBatchNo,
                    source.guideClothBatchNo);
            if (existing != null) {
                respVO.setSkippedCount(respVO.getSkippedCount() + 1);
                continue;
            }
            HcWetProductionRecordDO entity = HcWetProductionRecordDO.builder()
                    .recordDate(source.recordDate)
                    .modelCode(source.modelCode)
                    .materialCode(source.materialCode)
                    .batchNo(source.batchNo)
                    .inputKg(source.inputKg)
                    .outputMeter(source.outputMeter)
                    .petModel(source.petModel)
                    .petBatchNo(source.petBatchNo)
                    .guideClothBatchNo(source.guideClothBatchNo)
                    .guideClothUseCount(source.guideClothUseCount)
                    .guideClothChanged(source.guideClothChanged)
                    .changeDesc(source.changeDesc)
                    .recorderName(firstNotBlank(source.recorderName, SecurityFrameworkUtils.getLoginUserNickname(),
                            "系统初始化"))
                    .recordTime(source.recordTime == null ? LocalDateTime.now() : source.recordTime)
                    .status(STATUS_WAIT_CONFIRM)
                    .remark(source.remark)
                    .tenantId(tenantId)
                    .build();
            hcWetProductionRecordMapper.insert(entity);
            respVO.setCreateCount(respVO.getCreateCount() + 1);
        }
        if (invalidCount > 0) {
            respVO.getMessages().add(String.format("%d 条湿法报工缺少PET或导布业务关键词，已跳过", invalidCount));
        }
        respVO.getMessages().add(String.format("报工初始化完成：有效汇总 %d 条，新增 %d 条，跳过 %d 条",
                respVO.getSourceCount(), respVO.getCreateCount(), respVO.getSkippedCount()));
        return respVO;
    }

    private HcWetProductionRecordImportExcelVO toImportExcel(HcWetProductionRecordDO record) {
        HcWetProductionRecordImportExcelVO excelVO = new HcWetProductionRecordImportExcelVO();
        excelVO.setRecordDate(record.getRecordDate() == null ? null : DATE_FORMATTER.format(record.getRecordDate()));
        excelVO.setModelCode(record.getModelCode());
        excelVO.setMaterialCode(record.getMaterialCode());
        excelVO.setBatchNo(record.getBatchNo());
        excelVO.setInputKg(record.getInputKg());
        excelVO.setOutputMeter(record.getOutputMeter());
        excelVO.setPetModel(record.getPetModel());
        excelVO.setPetBatchNo(record.getPetBatchNo());
        excelVO.setGuideClothBatchNo(record.getGuideClothBatchNo());
        excelVO.setGuideClothUseCount(record.getGuideClothUseCount());
        excelVO.setGuideClothChangedName(YES.equals(record.getGuideClothChanged()) ? "是" : "否");
        excelVO.setChangeDesc(record.getChangeDesc());
        excelVO.setRecorderName(record.getRecorderName());
        excelVO.setRecordTime(formatDateTime(record.getRecordTime()));
        excelVO.setConfirmerName(record.getConfirmerName());
        excelVO.setConfirmTime(formatDateTime(record.getConfirmTime()));
        excelVO.setRemark(record.getRemark());
        return excelVO;
    }

    private void fillInputKgByFormulaBatch(Map<String, WetReportInitRow> grouped) {
        if (grouped.isEmpty()) {
            return;
        }
        List<String> batchNos = grouped.values().stream()
                .map(row -> trimToNull(row.batchNo))
                .filter(batchNo -> batchNo != null)
                .distinct()
                .toList();
        if (batchNos.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> inputKgByBatchNo = new HashMap<>();
        List<HcProcessReportDO> formulaReports = hcProcessReportMapper.selectList(
                new LambdaQueryWrapperX<HcProcessReportDO>()
                        .eq(HcProcessReportDO::getSourceMenuCode, SOURCE_MENU_CODE_FORMULA_REPORT)
                        .eq(HcProcessReportDO::getReportType, "END")
                        .eq(HcProcessReportDO::getDeleted, false)
                        .in(HcProcessReportDO::getBatchNo, batchNos)
                        .orderByDesc(HcProcessReportDO::getEndTime)
                        .orderByDesc(HcProcessReportDO::getId));
        for (HcProcessReportDO formulaReport : formulaReports) {
            String batchNo = trimToNull(formulaReport.getBatchNo());
            BigDecimal inputKg = formulaReport.getFeedQty() != null
                    ? formulaReport.getFeedQty() : formulaReport.getGoodQty();
            if (batchNo != null && inputKg != null) {
                inputKgByBatchNo.putIfAbsent(batchNo, inputKg);
            }
        }
        grouped.values().forEach(row -> row.inputKg = inputKgByBatchNo.get(trimToNull(row.batchNo)));
    }

    private WetProductionImportRow normalizeImportRow(HcWetProductionRecordImportExcelVO excelRow, int rowNo,
                                                      HcWetProductionRecordImportRespVO respVO) {
        LocalDate recordDate = parseDate(excelRow.getRecordDate(), rowNo, "日期", respVO);
        String modelCode = requireText(excelRow.getModelCode(), rowNo, "型号", respVO);
        String materialCode = requireText(excelRow.getMaterialCode(), rowNo, "料号", respVO);
        String batchNo = requireText(excelRow.getBatchNo(), rowNo, "批次", respVO);
        String petModel = requireText(excelRow.getPetModel(), rowNo, "PET型号", respVO);
        String petBatchNo = requireText(excelRow.getPetBatchNo(), rowNo, "PET批号", respVO);
        String guideClothBatchNo = requireText(excelRow.getGuideClothBatchNo(), rowNo, "导布批号", respVO);
        String guideClothChanged = normalizeYesNo(excelRow.getGuideClothChangedName());
        if (guideClothChanged == null) {
            addImportFailure(respVO, String.format("第%d行：导布更换（是否）仅支持 是/否/Y/N", rowNo));
        }
        Integer guideClothUseCount = excelRow.getGuideClothUseCount();
        if (guideClothUseCount != null && guideClothUseCount < 0) {
            addImportFailure(respVO, String.format("第%d行：导布累计使用次数不能为负数", rowNo));
        }
        String recorderName = requireText(excelRow.getRecorderName(), rowNo, "记录人", respVO);
        LocalDateTime recordTime = parseDateTime(excelRow.getRecordTime(), rowNo, "记录时间", respVO);
        if (!isValidBusinessTime(recordTime)) {
            addImportFailure(respVO, String.format("第%d行：记录时间不能为空或早于2000年", rowNo));
        }
        if (recordDate == null || isBlank(modelCode) || isBlank(materialCode) || isBlank(batchNo)
                || isBlank(petModel) || isBlank(petBatchNo) || isBlank(guideClothBatchNo)
                || guideClothChanged == null || isBlank(recorderName) || !isValidBusinessTime(recordTime)) {
            return null;
        }
        WetProductionImportRow row = new WetProductionImportRow();
        row.recordDate = recordDate;
        row.modelCode = modelCode;
        row.materialCode = materialCode;
        row.batchNo = batchNo;
        row.inputKg = excelRow.getInputKg();
        row.outputMeter = excelRow.getOutputMeter();
        row.petModel = petModel;
        row.petBatchNo = petBatchNo;
        row.guideClothBatchNo = guideClothBatchNo;
        row.guideClothUseCount = guideClothUseCount;
        row.guideClothChanged = guideClothChanged;
        row.changeDesc = trimToNull(excelRow.getChangeDesc());
        row.recorderName = recorderName;
        row.recordTime = recordTime;
        row.remark = trimToNull(excelRow.getRemark());
        return row;
    }

    private boolean isBlankImportRow(HcWetProductionRecordImportExcelVO row) {
        if (row == null) {
            return true;
        }
        return isBlank(row.getRecordDate())
                && isBlank(row.getModelCode())
                && isBlank(row.getMaterialCode())
                && isBlank(row.getBatchNo())
                && row.getInputKg() == null
                && row.getOutputMeter() == null
                && isBlank(row.getPetModel())
                && isBlank(row.getPetBatchNo())
                && isBlank(row.getGuideClothBatchNo())
                && row.getGuideClothUseCount() == null
                && isBlank(row.getGuideClothChangedName())
                && isBlank(row.getChangeDesc())
                && isBlank(row.getRecorderName())
                && isBlank(row.getRecordTime())
                && isBlank(row.getRemark());
    }

    private void normalizeReq(HcWetProductionRecordSaveReqVO reqVO) {
        reqVO.setModelCode(trimToNull(reqVO.getModelCode()));
        reqVO.setMaterialCode(trimToNull(reqVO.getMaterialCode()));
        reqVO.setBatchNo(trimToNull(reqVO.getBatchNo()));
        reqVO.setPetModel(trimToNull(reqVO.getPetModel()));
        reqVO.setPetBatchNo(trimToNull(reqVO.getPetBatchNo()));
        reqVO.setGuideClothBatchNo(trimToNull(reqVO.getGuideClothBatchNo()));
        reqVO.setGuideClothChanged(normalizeYesNo(reqVO.getGuideClothChanged()));
        reqVO.setChangeDesc(trimToNull(reqVO.getChangeDesc()));
        reqVO.setRecorderName(trimToNull(reqVO.getRecorderName()));
        reqVO.setRemark(trimToNull(reqVO.getRemark()));
        if (reqVO.getGuideClothUseCount() != null && reqVO.getGuideClothUseCount() < 0) {
            throw invalidParamException("导布累计使用次数不能为负数");
        }
        if (isBlank(reqVO.getGuideClothChanged())) {
            throw invalidParamException("导布更换仅支持 是/否/Y/N");
        }
    }

    private void normalizePageReq(HcWetProductionRecordPageReqVO reqVO) {
        reqVO.setBatchNo(trimToNull(reqVO.getBatchNo()));
        reqVO.setGuideClothChanged(normalizeYesNoOptional(reqVO.getGuideClothChanged()));
        reqVO.setChangeDesc(trimToNull(reqVO.getChangeDesc()));
        reqVO.setStatus(trimToNull(reqVO.getStatus()));
    }

    private void normalizeInitReq(HcWetProductionRecordInitReqVO reqVO) {
        reqVO.setBatchNo(trimToNull(reqVO.getBatchNo()));
        reqVO.setGuideClothChanged(normalizeYesNoOptional(reqVO.getGuideClothChanged()));
        reqVO.setChangeDesc(trimToNull(reqVO.getChangeDesc()));
    }

    private void validateBizKeyAvailable(HcWetProductionRecordSaveReqVO reqVO, Long currentId) {
        HcWetProductionRecordDO existing = hcWetProductionRecordMapper.selectByBizKey(reqVO.getRecordDate(),
                reqVO.getModelCode(), reqVO.getMaterialCode(), reqVO.getBatchNo(), reqVO.getPetModel(),
                reqVO.getPetBatchNo(), reqVO.getGuideClothBatchNo());
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw invalidParamException("相同日期、型号、料号、批次、PET型号、PET批号、导布批号的记录已存在");
        }
    }

    private HcWetProductionRecordDO validateExists(Long id) {
        if (id == null) {
            throw invalidParamException("湿法生产记录不存在");
        }
        HcWetProductionRecordDO record = hcWetProductionRecordMapper.selectById(id);
        if (record == null) {
            throw invalidParamException("湿法生产记录不存在");
        }
        return record;
    }

    private void validateWaitConfirm(HcWetProductionRecordDO record) {
        if (record == null) {
            throw invalidParamException("湿法生产记录不存在");
        }
        if (STATUS_CONFIRMED.equals(record.getStatus())) {
            throw invalidParamException("湿法生产记录已确认，不能修改或删除");
        }
    }

    private void validateRecordTime(LocalDateTime recordTime) {
        if (!isValidBusinessTime(recordTime)) {
            throw invalidParamException("记录时间不能为空或无效，请重新选择");
        }
    }

    private LocalDate parseDate(String value, int rowNo, String fieldName, HcWetProductionRecordImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            addImportFailure(respVO, String.format("第%d行：%s不能为空", rowNo, fieldName));
            return null;
        }
        String normalized = text.replace('/', '-').replace('T', ' ');
        try {
            if (normalized.length() >= 10) {
                return LocalDate.parse(normalized.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd", rowNo, fieldName));
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value, int rowNo, String fieldName,
                                        HcWetProductionRecordImportRespVO respVO) {
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

    private String requireText(String value, int rowNo, String fieldName, HcWetProductionRecordImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            addImportFailure(respVO, String.format("第%d行：%s不能为空", rowNo, fieldName));
        }
        return text;
    }

    private String normalizeYesNo(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return NO;
        }
        if ("Y".equalsIgnoreCase(text) || "YES".equalsIgnoreCase(text) || "TRUE".equalsIgnoreCase(text)
                || "1".equals(text) || "是".equals(text) || "已更换".equals(text)) {
            return YES;
        }
        if ("N".equalsIgnoreCase(text) || "NO".equalsIgnoreCase(text) || "FALSE".equalsIgnoreCase(text)
                || "0".equals(text) || "否".equals(text) || "未更换".equals(text)) {
            return NO;
        }
        return null;
    }

    private String normalizeYesNoOptional(String value) {
        String text = trimToNull(value);
        return text == null ? null : normalizeYesNo(text);
    }

    private void addImportFailure(HcWetProductionRecordImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String buildKey(LocalDate recordDate, String modelCode, String materialCode, String batchNo,
                            String petModel, String petBatchNo, String guideClothBatchNo) {
        return String.join("|", recordDate.toString(), modelCode, materialCode, batchNo, petModel, petBatchNo,
                guideClothBatchNo);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private boolean isValidBusinessTime(LocalDateTime value) {
        return value != null && value.getYear() >= MIN_VALID_BUSINESS_YEAR;
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

    private String trimToNull(String value) {
        String text = value == null ? null : value.trim();
        return isBlank(text) ? null : text;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private JSONObject parseExtra(String value) {
        if (isBlank(value)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(value);
        } catch (RuntimeException ignored) {
            return new JSONObject();
        }
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && keyword != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String appendDistinct(String current, String candidate) {
        String next = trimToNull(candidate);
        if (next == null) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return next;
        }
        return current.contains(next) ? current : current + "；" + next;
    }

    private class WetReportInitRow {
        private final LocalDate recordDate;
        private final String modelCode;
        private final String materialCode;
        private final String batchNo;
        private final String petModel;
        private final String petBatchNo;
        private final String guideClothBatchNo;
        private BigDecimal inputKg;
        private BigDecimal outputMeter = BigDecimal.ZERO;
        private Integer guideClothUseCount;
        private String guideClothChanged;
        private String changeDesc;
        private String recorderName;
        private LocalDateTime recordTime;
        private String remark;

        WetReportInitRow(LocalDate recordDate, String modelCode, String materialCode, String batchNo,
                         String petModel, String petBatchNo, String guideClothBatchNo,
                         String guideClothChanged) {
            this.recordDate = recordDate;
            this.modelCode = modelCode;
            this.materialCode = materialCode;
            this.batchNo = batchNo;
            this.petModel = petModel;
            this.petBatchNo = petBatchNo;
            this.guideClothBatchNo = guideClothBatchNo;
            this.guideClothChanged = guideClothChanged;
        }

        void accept(HcProcessReportDO report, JSONObject extra, String currentChangeDesc) {
            if (report.getGoodQty() != null) {
                outputMeter = outputMeter.add(report.getGoodQty());
            }
            Integer useCount = extra.getInt("guideClothUseCount");
            if (useCount != null) {
                guideClothUseCount = guideClothUseCount == null ? useCount : Math.max(guideClothUseCount, useCount);
            }
            if (YES.equals(normalizeYesNo(extra.getStr("guideClothChanged")))) {
                guideClothChanged = YES;
            }
            changeDesc = appendDistinct(changeDesc, currentChangeDesc);
            recorderName = appendDistinct(recorderName, report.getRecorderName());
            remark = appendDistinct(remark, report.getRemark());
            LocalDateTime currentRecordTime = report.getEndTime() == null
                    ? report.getRecorderTime() : report.getEndTime();
            if (currentRecordTime != null && (recordTime == null || currentRecordTime.isAfter(recordTime))) {
                recordTime = currentRecordTime;
            }
        }
    }

    private static class WetProductionImportRow {
        private Long existingId;
        private LocalDate recordDate;
        private String modelCode;
        private String materialCode;
        private String batchNo;
        private java.math.BigDecimal inputKg;
        private java.math.BigDecimal outputMeter;
        private String petModel;
        private String petBatchNo;
        private String guideClothBatchNo;
        private Integer guideClothUseCount;
        private String guideClothChanged;
        private String changeDesc;
        private String recorderName;
        private LocalDateTime recordTime;
        private String remark;

        private HcWetProductionRecordDO toDO() {
            return HcWetProductionRecordDO.builder()
                    .recordDate(recordDate)
                    .modelCode(modelCode)
                    .materialCode(materialCode)
                    .batchNo(batchNo)
                    .inputKg(inputKg)
                    .outputMeter(outputMeter)
                    .petModel(petModel)
                    .petBatchNo(petBatchNo)
                    .guideClothBatchNo(guideClothBatchNo)
                    .guideClothUseCount(guideClothUseCount)
                    .guideClothChanged(guideClothChanged)
                    .changeDesc(changeDesc)
                    .recorderName(recorderName)
                    .recordTime(recordTime)
                    .remark(remark)
                    .build();
        }
    }
}
