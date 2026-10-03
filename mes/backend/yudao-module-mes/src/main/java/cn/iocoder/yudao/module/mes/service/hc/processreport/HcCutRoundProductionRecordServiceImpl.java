package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordInitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundProductionRecordMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcCutRoundProductionRecordServiceImpl implements HcCutRoundProductionRecordService {

    private static final String STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_IMPORT = "IMPORT";
    private static final String SOURCE_REPORT_INIT = "REPORT_INIT";
    private static final int MIN_VALID_BUSINESS_YEAR = 2000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcCutRoundProductionRecordMapper productionRecordMapper;
    @Resource
    private HcCutRoundConsoleService hcCutRoundConsoleService;
    @Resource
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HcCutRoundProductionRecordSaveReqVO reqVO) {
        normalizeReq(reqVO);
        validateBizKeyAvailable(reqVO, null);
        HcCutRoundProductionRecordDO entity = BeanUtils.toBean(reqVO, HcCutRoundProductionRecordDO.class);
        entity.setPadType(padTypeResolver.resolveByModelCode(entity.getModelCode()));
        entity.setStatus(STATUS_WAIT_CONFIRM);
        entity.setSourceType(SOURCE_MANUAL);
        entity.setTenantId(TenantContextHolder.getTenantId());
        productionRecordMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HcCutRoundProductionRecordSaveReqVO reqVO) {
        HcCutRoundProductionRecordDO existing = validateExists(reqVO.getId());
        validateWaitConfirm(existing);
        normalizeReq(reqVO);
        validateBizKeyAvailable(reqVO, existing.getId());
        HcCutRoundProductionRecordDO updateObj = BeanUtils.toBean(reqVO, HcCutRoundProductionRecordDO.class);
        updateObj.setPadType(padTypeResolver.resolveByModelCode(updateObj.getModelCode()));
        updateObj.setStatus(STATUS_WAIT_CONFIRM);
        updateObj.setConfirmerName(null);
        updateObj.setConfirmTime(null);
        productionRecordMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        HcCutRoundProductionRecordDO existing = validateExists(id);
        validateWaitConfirm(existing);
        productionRecordMapper.physicalDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw invalidParamException("请选择要删除的裁切生产记录");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<HcCutRoundProductionRecordDO> records = productionRecordMapper.selectByRecordIds(distinctIds);
        if (records.size() != distinctIds.size()) {
            throw invalidParamException("部分裁切生产记录不存在，请刷新后重试");
        }
        records.forEach(this::validateWaitConfirm);
        productionRecordMapper.physicalDeleteByIds(distinctIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer confirm(HcCutRoundProductionRecordConfirmReqVO reqVO) {
        List<Long> distinctIds = reqVO.getIds().stream().distinct().toList();
        List<HcCutRoundProductionRecordDO> records = productionRecordMapper.selectByRecordIds(distinctIds);
        if (records.size() != distinctIds.size()) {
            throw invalidParamException("部分裁切生产记录不存在，请刷新后重试");
        }
        String confirmerName = firstNotBlank(reqVO.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname());
        if (isBlank(confirmerName)) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            confirmerName = loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
        }
        LocalDateTime confirmTime = LocalDateTime.now();
        int confirmedCount = 0;
        for (HcCutRoundProductionRecordDO record : records) {
            if (STATUS_CONFIRMED.equals(record.getStatus())) {
                continue;
            }
            HcCutRoundProductionRecordDO updateObj = new HcCutRoundProductionRecordDO();
            updateObj.setId(record.getId());
            updateObj.setStatus(STATUS_CONFIRMED);
            updateObj.setConfirmerName(confirmerName);
            updateObj.setConfirmTime(confirmTime);
            productionRecordMapper.updateById(updateObj);
            confirmedCount++;
        }
        if (confirmedCount == 0) {
            throw invalidParamException("所选裁切生产记录均已确认，无需重复确认");
        }
        return confirmedCount;
    }

    @Override
    public HcCutRoundProductionRecordDO get(Long id) {
        return productionRecordMapper.selectById(id);
    }

    @Override
    public PageResult<HcCutRoundProductionRecordDO> getPage(HcCutRoundProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        return productionRecordMapper.selectPage(reqVO);
    }

    @Override
    public List<HcCutRoundProductionRecordExcelVO> buildExportList(HcCutRoundProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        return productionRecordMapper.selectList(reqVO).stream().map(this::toExcelVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcCutRoundProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException {
        HcCutRoundProductionRecordImportRespVO respVO = new HcCutRoundProductionRecordImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        List<HcCutRoundProductionRecordExcelVO> excelRows = ExcelUtils.read(file, HcCutRoundProductionRecordExcelVO.class);
        List<ImportRow> rows = new ArrayList<>();
        Map<String, Integer> fileKeys = new HashMap<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcCutRoundProductionRecordExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            int failureCountBefore = respVO.getFailures().size();
            ImportRow row = normalizeImportRow(excelRow, rowNo, respVO);
            if (row == null || failureCountBefore != respVO.getFailures().size()) {
                continue;
            }
            String key = buildKey(row.reportDate, row.modelCode, row.productionBatchNo, row.cutSizeMm);
            Integer previousRowNo = fileKeys.putIfAbsent(key, rowNo);
            if (previousRowNo != null) {
                addImportFailure(respVO, String.format("第%d行：业务关键词与第%d行重复，日期/型号/生产批号/裁切尺寸必须唯一",
                        rowNo, previousRowNo));
                continue;
            }
            HcCutRoundProductionRecordDO existing = productionRecordMapper.selectByBizKey(row.reportDate,
                    row.modelCode, row.productionBatchNo, row.cutSizeMm);
            if (existing != null && STATUS_CONFIRMED.equals(existing.getStatus())) {
                addImportFailure(respVO, String.format("第%d行：匹配记录已确认，禁止导入覆盖。生产批号=%s，裁切尺寸=%s",
                        rowNo, row.productionBatchNo, row.cutSizeMm));
                continue;
            }
            row.existingId = existing == null ? null : existing.getId();
            rows.add(row);
        }
        if (!respVO.getFailures().isEmpty()) {
            respVO.getMessages().add("导入校验未通过，未写入任何数据");
            return respVO;
        }
        if (rows.isEmpty()) {
            addImportFailure(respVO, "导入文件没有有效数据行");
            return respVO;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        for (ImportRow row : rows) {
            HcCutRoundProductionRecordDO entity = row.toDO();
            entity.setPadType(padTypeResolver.resolveByModelCode(entity.getModelCode()));
            entity.setStatus(STATUS_WAIT_CONFIRM);
            entity.setSourceType(SOURCE_IMPORT);
            entity.setTenantId(tenantId);
            if (row.existingId == null) {
                productionRecordMapper.insert(entity);
                respVO.setCreateCount(respVO.getCreateCount() + 1);
            } else {
                entity.setId(row.existingId);
                productionRecordMapper.updateById(entity);
                respVO.setUpdateCount(respVO.getUpdateCount() + 1);
            }
        }
        respVO.getMessages().add(String.format("导入完成：新增 %d 条，更新 %d 条；导入记录均为待确认",
                respVO.getCreateCount(), respVO.getUpdateCount()));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcCutRoundProductionRecordInitRespVO initializeFromReports(HcCutRoundProductionRecordPageReqVO reqVO) {
        normalizePageReq(reqVO);
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setStatus(null);
        List<HcCutRoundProductionRecordRespVO> sourceRows = hcCutRoundConsoleService.getProductionRecordList(reqVO);
        HcCutRoundProductionRecordInitRespVO respVO = new HcCutRoundProductionRecordInitRespVO();
        respVO.setSourceCount(sourceRows.size());
        Long tenantId = TenantContextHolder.getTenantId();
        LocalDateTime initTime = LocalDateTime.now();
        String defaultRecorder = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统初始化");
        for (HcCutRoundProductionRecordRespVO source : sourceRows) {
            String modelCode = trimToNull(source.getModelCode());
            String batchNo = trimToNull(source.getProductionBatchNo());
            String cutSizeMm = normalizeCutSize(source.getCutSizeMm());
            if (source.getReportDate() == null || modelCode == null || batchNo == null || cutSizeMm == null) {
                respVO.setSkippedCount(respVO.getSkippedCount() + 1);
                respVO.getMessages().add(String.format("报工数据缺少日期/型号/生产批号/裁切尺寸，已跳过：批号=%s",
                        firstNotBlank(batchNo, "-")));
                continue;
            }
            HcCutRoundProductionRecordDO existing = productionRecordMapper.selectByBizKey(source.getReportDate(),
                    modelCode, batchNo, cutSizeMm);
            if (existing != null) {
                respVO.setSkippedCount(respVO.getSkippedCount() + 1);
                continue;
            }
            HcCutRoundProductionRecordDO entity = HcCutRoundProductionRecordDO.builder()
                    .reportDate(source.getReportDate())
                    .modelCode(modelCode)
                    .padType(padTypeResolver.resolveByModelCode(modelCode))
                    .productionBatchNo(batchNo)
                    .cutSizeMm(cutSizeMm)
                    .inputQty(source.getInputQty())
                    .outputQty(source.getOutputQty())
                    .bladeModel(trimToNull(source.getBladeModel()))
                    .bladeBatchNo(trimToNull(source.getBladeBatchNo()))
                    .bladeUseCount(source.getBladeUseCount())
                    .feltModel(trimToNull(source.getFeltModel()))
                    .feltBatchNo(trimToNull(source.getFeltBatchNo()))
                    .feltUseCount(source.getFeltUseCount())
                    .feltUseDays(source.getFeltUseDays())
                    .bladeReplaceReason(trimToNull(source.getBladeReplaceReason()))
                    .recorderName(firstNotBlank(trimToNull(source.getRecorderName()), defaultRecorder))
                    .recordTime(initTime)
                    .status(STATUS_WAIT_CONFIRM)
                    .sourceType(SOURCE_REPORT_INIT)
                    .remark(trimToNull(source.getRemark()))
                    .tenantId(tenantId)
                    .build();
            productionRecordMapper.insert(entity);
            respVO.setCreateCount(respVO.getCreateCount() + 1);
        }
        respVO.getMessages().add(String.format("报工初始化完成：读取 %d 条，新增 %d 条，跳过 %d 条",
                respVO.getSourceCount(), respVO.getCreateCount(), respVO.getSkippedCount()));
        return respVO;
    }

    private HcCutRoundProductionRecordExcelVO toExcelVO(HcCutRoundProductionRecordDO record) {
        HcCutRoundProductionRecordExcelVO target = new HcCutRoundProductionRecordExcelVO();
        target.setReportDate(record.getReportDate() == null ? null : DATE_FORMATTER.format(record.getReportDate()));
        target.setModelCode(record.getModelCode());
        target.setPadType(record.getPadType());
        target.setProductionBatchNo(record.getProductionBatchNo());
        target.setCutSizeMm(record.getCutSizeMm());
        target.setInputQty(record.getInputQty());
        target.setOutputQty(record.getOutputQty());
        target.setBladeModel(record.getBladeModel());
        target.setBladeBatchNo(record.getBladeBatchNo());
        target.setBladeUseCount(record.getBladeUseCount());
        target.setFeltModel(record.getFeltModel());
        target.setFeltBatchNo(record.getFeltBatchNo());
        target.setFeltUseCount(record.getFeltUseCount());
        target.setFeltUseDays(record.getFeltUseDays());
        target.setBladeReplaceReason(record.getBladeReplaceReason());
        target.setRecorderName(record.getRecorderName());
        target.setRecordTime(formatDateTime(record.getRecordTime()));
        target.setConfirmerName(record.getConfirmerName());
        target.setConfirmTime(formatDateTime(record.getConfirmTime()));
        target.setRemark(record.getRemark());
        return target;
    }

    private ImportRow normalizeImportRow(HcCutRoundProductionRecordExcelVO source, int rowNo,
                                         HcCutRoundProductionRecordImportRespVO respVO) {
        ImportRow row = new ImportRow();
        row.reportDate = parseDate(source.getReportDate(), rowNo, respVO);
        row.modelCode = requireText(source.getModelCode(), rowNo, "型号", respVO);
        row.productionBatchNo = requireText(source.getProductionBatchNo(), rowNo, "生产批号", respVO);
        row.cutSizeMm = normalizeCutSize(requireText(source.getCutSizeMm(), rowNo, "裁切尺寸", respVO));
        row.inputQty = source.getInputQty();
        row.outputQty = source.getOutputQty();
        row.bladeModel = trimToNull(source.getBladeModel());
        row.bladeBatchNo = trimToNull(source.getBladeBatchNo());
        row.bladeUseCount = source.getBladeUseCount();
        row.feltModel = trimToNull(source.getFeltModel());
        row.feltBatchNo = trimToNull(source.getFeltBatchNo());
        row.feltUseCount = source.getFeltUseCount();
        row.feltUseDays = source.getFeltUseDays();
        row.bladeReplaceReason = trimToNull(source.getBladeReplaceReason());
        row.recorderName = requireText(source.getRecorderName(), rowNo, "记录人", respVO);
        row.recordTime = parseDateTime(source.getRecordTime(), rowNo, respVO);
        row.remark = trimToNull(source.getRemark());
        validateNonNegative(row.inputQty, rowNo, "投入", respVO);
        validateNonNegative(row.outputQty, rowNo, "产出", respVO);
        validateNonNegative(row.bladeUseCount, rowNo, "刀片累计裁切", respVO);
        validateNonNegative(row.feltUseCount, rowNo, "裁切片数累计", respVO);
        validateNonNegative(row.feltUseDays, rowNo, "毛毡累计使用天数", respVO);
        if (row.recordTime == null || row.recordTime.getYear() < MIN_VALID_BUSINESS_YEAR) {
            addImportFailure(respVO, String.format("第%d行：记录时间不能为空或早于2000年", rowNo));
        }
        return row.reportDate == null || row.modelCode == null || row.productionBatchNo == null
                || row.cutSizeMm == null || row.recorderName == null || row.recordTime == null ? null : row;
    }

    private void normalizeReq(HcCutRoundProductionRecordSaveReqVO reqVO) {
        reqVO.setModelCode(trimToNull(reqVO.getModelCode()));
        reqVO.setProductionBatchNo(trimToNull(reqVO.getProductionBatchNo()));
        reqVO.setCutSizeMm(normalizeCutSize(reqVO.getCutSizeMm()));
        reqVO.setBladeModel(trimToNull(reqVO.getBladeModel()));
        reqVO.setBladeBatchNo(trimToNull(reqVO.getBladeBatchNo()));
        reqVO.setFeltModel(trimToNull(reqVO.getFeltModel()));
        reqVO.setFeltBatchNo(trimToNull(reqVO.getFeltBatchNo()));
        reqVO.setBladeReplaceReason(trimToNull(reqVO.getBladeReplaceReason()));
        reqVO.setRecorderName(trimToNull(reqVO.getRecorderName()));
        reqVO.setRemark(trimToNull(reqVO.getRemark()));
        validateNonNegative(reqVO.getInputQty(), "投入不能为负数");
        validateNonNegative(reqVO.getOutputQty(), "产出不能为负数");
        validateNonNegative(reqVO.getBladeUseCount(), "刀片累计裁切不能为负数");
        validateNonNegative(reqVO.getFeltUseCount(), "裁切片数累计不能为负数");
        validateNonNegative(reqVO.getFeltUseDays(), "毛毡累计使用天数不能为负数");
        if (reqVO.getRecordTime() == null || reqVO.getRecordTime().getYear() < MIN_VALID_BUSINESS_YEAR) {
            throw invalidParamException("记录时间不能为空或无效，请重新选择");
        }
    }

    private void normalizePageReq(HcCutRoundProductionRecordPageReqVO reqVO) {
        reqVO.setModelCode(trimToNull(reqVO.getModelCode()));
        String padType = padTypeResolver.normalizeFilter(reqVO.getPadType());
        if (reqVO.getPadType() != null && padType == null) {
            throw invalidParamException("类型筛选仅支持黑垫、白垫或未归类");
        }
        reqVO.setPadType(padType);
        reqVO.setProductionBatchNo(trimToNull(reqVO.getProductionBatchNo()));
        reqVO.setCutSizeMm(normalizeCutSize(reqVO.getCutSizeMm()));
        reqVO.setRecorderName(trimToNull(reqVO.getRecorderName()));
        reqVO.setStatus(trimToNull(reqVO.getStatus()));
    }

    private void validateBizKeyAvailable(HcCutRoundProductionRecordSaveReqVO reqVO, Long currentId) {
        HcCutRoundProductionRecordDO existing = productionRecordMapper.selectByBizKey(reqVO.getReportDate(),
                reqVO.getModelCode(), reqVO.getProductionBatchNo(), reqVO.getCutSizeMm());
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw invalidParamException("相同日期、型号、生产批号、裁切尺寸的记录已存在");
        }
    }

    private HcCutRoundProductionRecordDO validateExists(Long id) {
        HcCutRoundProductionRecordDO record = id == null ? null : productionRecordMapper.selectById(id);
        if (record == null) {
            throw invalidParamException("裁切生产记录不存在");
        }
        return record;
    }

    private void validateWaitConfirm(HcCutRoundProductionRecordDO record) {
        if (STATUS_CONFIRMED.equals(record.getStatus())) {
            throw invalidParamException("裁切生产记录已确认，不能修改或删除");
        }
    }

    private LocalDate parseDate(String value, int rowNo, HcCutRoundProductionRecordImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            addImportFailure(respVO, String.format("第%d行：日期不能为空", rowNo));
            return null;
        }
        String normalized = text.replace('/', '-').replace('T', ' ');
        try {
            return LocalDate.parse(normalized.substring(0, Math.min(10, normalized.length())),
                    DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：日期格式不正确，应为 yyyy-MM-dd", rowNo));
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value, int rowNo,
                                        HcCutRoundProductionRecordImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace('/', '-').replace('T', ' ');
        for (DateTimeFormatter formatter : List.of(DATE_TIME_FORMATTER, DATE_TIME_MINUTE_FORMATTER,
                DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"), DateTimeFormatter.ofPattern("yyyy-M-d H:m"))) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        addImportFailure(respVO, String.format("第%d行：记录时间格式不正确，应为 yyyy-MM-dd HH:mm:ss", rowNo));
        return null;
    }

    private boolean isBlankImportRow(HcCutRoundProductionRecordExcelVO row) {
        return row == null || isBlank(row.getReportDate()) && isBlank(row.getModelCode())
                && isBlank(row.getProductionBatchNo()) && isBlank(row.getCutSizeMm())
                && row.getInputQty() == null && row.getOutputQty() == null && row.getBladeUseCount() == null
                && isBlank(row.getBladeModel()) && isBlank(row.getBladeBatchNo())
                && isBlank(row.getFeltModel()) && isBlank(row.getFeltBatchNo())
                && row.getFeltUseCount() == null && row.getFeltUseDays() == null
                && isBlank(row.getBladeReplaceReason()) && isBlank(row.getRecorderName())
                && isBlank(row.getRecordTime()) && isBlank(row.getRemark());
    }

    private String requireText(String value, int rowNo, String fieldName,
                               HcCutRoundProductionRecordImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            addImportFailure(respVO, String.format("第%d行：%s不能为空", rowNo, fieldName));
        }
        return text;
    }

    private void validateNonNegative(Number value, int rowNo, String fieldName,
                                     HcCutRoundProductionRecordImportRespVO respVO) {
        if (value != null && new BigDecimal(value.toString()).signum() < 0) {
            addImportFailure(respVO, String.format("第%d行：%s不能为负数", rowNo, fieldName));
        }
    }

    private void validateNonNegative(Number value, String message) {
        if (value != null && new BigDecimal(value.toString()).signum() < 0) {
            throw invalidParamException(message);
        }
    }

    private void addImportFailure(HcCutRoundProductionRecordImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String buildKey(LocalDate date, String modelCode, String batchNo, String cutSizeMm) {
        return String.join("|", date.toString(), modelCode, batchNo, cutSizeMm);
    }

    private String normalizeCutSize(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return text.toLowerCase().endsWith("mm") ? text.substring(0, text.length() - 2).trim() : text;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String firstNotBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (!isBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        String text = value == null ? null : value.trim();
        return isBlank(text) ? null : text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ImportRow {
        private Long existingId;
        private LocalDate reportDate;
        private String modelCode;
        private String productionBatchNo;
        private String cutSizeMm;
        private BigDecimal inputQty;
        private BigDecimal outputQty;
        private String bladeModel;
        private String bladeBatchNo;
        private Integer bladeUseCount;
        private String feltModel;
        private String feltBatchNo;
        private Integer feltUseCount;
        private Integer feltUseDays;
        private String bladeReplaceReason;
        private String recorderName;
        private LocalDateTime recordTime;
        private String remark;

        private HcCutRoundProductionRecordDO toDO() {
            return HcCutRoundProductionRecordDO.builder()
                    .reportDate(reportDate)
                    .modelCode(modelCode)
                    .productionBatchNo(productionBatchNo)
                    .cutSizeMm(cutSizeMm)
                    .inputQty(inputQty)
                    .outputQty(outputQty)
                    .bladeModel(bladeModel)
                    .bladeBatchNo(bladeBatchNo)
                    .bladeUseCount(bladeUseCount)
                    .feltModel(feltModel)
                    .feltBatchNo(feltBatchNo)
                    .feltUseCount(feltUseCount)
                    .feltUseDays(feltUseDays)
                    .bladeReplaceReason(bladeReplaceReason)
                    .recorderName(recorderName)
                    .recordTime(recordTime)
                    .remark(remark)
                    .build();
        }
    }
}
