package cn.iocoder.yudao.module.mes.service.qms;

import cn.idev.excel.FastExcelFactory;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentBoardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentBoardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsEnvironmentRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsEnvironmentStandardDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsEnvironmentRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsEnvironmentStandardMapper;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class QmsEnvironmentBoardServiceImpl implements QmsEnvironmentBoardService {

    private static final BigDecimal DEFAULT_TEMPERATURE_MIN = new BigDecimal("19.00");
    private static final BigDecimal DEFAULT_TEMPERATURE_MAX = new BigDecimal("27.00");
    private static final BigDecimal DEFAULT_HUMIDITY_MIN = new BigDecimal("45.00");
    private static final BigDecimal DEFAULT_HUMIDITY_MAX = new BigDecimal("65.00");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FLEXIBLE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-M-d")
            .toFormatter();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int REMARK_MAX_LENGTH = 255;
    private static final String STATUS_OK = "OK";
    private static final String STATUS_NG = "NG";
    private static final String STATUS_MISSING = "MISSING";
    private static final String RECORD_STATUS_WAIT_RECORD = "WAIT_RECORD";
    private static final String RECORD_STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String RECORD_STATUS_CONFIRMED = "CONFIRMED";
    private static final String CONFIRM_STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String CONFIRM_STATUS_CONFIRMED = "CONFIRMED";

    private static final ErrorCode QMS_ENVIRONMENT_MONTH_INVALID =
            new ErrorCode(1008100220, "记录月份格式必须为 yyyy-MM");
    private static final ErrorCode QMS_ENVIRONMENT_STANDARD_RANGE_INVALID =
            new ErrorCode(1008100221, "温湿度标准范围不合法，下限不能大于上限");
    private static final ErrorCode QMS_ENVIRONMENT_RECORD_NOT_EXISTS =
            new ErrorCode(1008100222, "当天温湿度记录不存在，请先记录后确认");
    private static final ErrorCode QMS_ENVIRONMENT_RECORD_VALUE_REQUIRED =
            new ErrorCode(1008100223, "请先填写当天温度和湿度后再保存");
    private static final ErrorCode QMS_ENVIRONMENT_HISTORY_LOCKED =
            new ErrorCode(1008100224, "只能保存当天温湿度记录，历史日期请点击修正");
    private static final ErrorCode QMS_ENVIRONMENT_CORRECTION_REASON_REQUIRED =
            new ErrorCode(1008100225, "修正原因不能为空");
    private static final ErrorCode QMS_ENVIRONMENT_FUTURE_DATE_INVALID =
            new ErrorCode(1008100226, "不能录入或修正未来日期的温湿度记录");

    @Resource
    private QmsEnvironmentStandardMapper environmentStandardMapper;
    @Resource
    private QmsEnvironmentRecordMapper environmentRecordMapper;

    @Override
    public QmsEnvironmentBoardRespVO getBoard(QmsEnvironmentBoardReqVO reqVO) {
        String workshopCode = normalizeCode(reqVO.getWorkshopCode());
        String workshopName = normalizeName(reqVO.getWorkshopName(), workshopCode);
        YearMonth month = parseMonth(reqVO.getRecordMonth());
        Long tenantId = currentTenantId();
        QmsEnvironmentStandardDO standard = getOrDefaultStandard(workshopCode, workshopName, tenantId);
        LocalDate startDate = month.atDay(1);
        List<QmsEnvironmentRecordDO> sourceRecords = environmentRecordMapper.selectListByMonth(
                workshopCode, startDate, month.plusMonths(1).atDay(1), tenantId);
        List<QmsEnvironmentBoardRespVO.Record> records = buildMonthRecords(month, sourceRecords, standard);
        return QmsEnvironmentBoardRespVO.builder()
                .workshopCode(workshopCode)
                .workshopName(workshopName)
                .recordMonth(MONTH_FORMATTER.format(month))
                .dayCount(month.lengthOfMonth())
                .standard(toStandardResp(standard))
                .summary(buildSummary(records))
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsEnvironmentBoardRespVO.Standard saveStandard(QmsEnvironmentStandardSaveReqVO reqVO) {
        validateRange(reqVO.getTemperatureMin(), reqVO.getTemperatureMax());
        validateRange(reqVO.getHumidityMin(), reqVO.getHumidityMax());
        Long tenantId = currentTenantId();
        String workshopCode = normalizeCode(reqVO.getWorkshopCode());
        QmsEnvironmentStandardDO existed = environmentStandardMapper.selectByWorkshop(workshopCode, tenantId);
        QmsEnvironmentStandardDO standard = QmsEnvironmentStandardDO.builder()
                .id(existed == null ? null : existed.getId())
                .workshopCode(workshopCode)
                .workshopName(normalizeName(reqVO.getWorkshopName(), workshopCode))
                .temperatureMin(reqVO.getTemperatureMin())
                .temperatureMax(reqVO.getTemperatureMax())
                .humidityMin(reqVO.getHumidityMin())
                .humidityMax(reqVO.getHumidityMax())
                .remark(trimToNull(reqVO.getRemark()))
                .tenantId(tenantId)
                .build();
        if (existed == null) {
            environmentStandardMapper.insert(standard);
        } else {
            environmentStandardMapper.updateByBusinessKey(standard);
        }
        return toStandardResp(standard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsEnvironmentBoardRespVO.Record saveRecord(QmsEnvironmentRecordSaveReqVO reqVO) {
        ensureTodayRecord(reqVO.getRecordDate());
        return saveRecordInternal(reqVO, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsEnvironmentBoardRespVO.Record correctRecord(QmsEnvironmentRecordCorrectReqVO reqVO) {
        String correctionReason = trimToNull(reqVO.getCorrectionReason());
        if (correctionReason == null) {
            throw exception(QMS_ENVIRONMENT_CORRECTION_REASON_REQUIRED);
        }
        ensureNotFutureRecord(reqVO.getRecordDate());
        return saveRecordInternal(reqVO, correctionReason);
    }

    private QmsEnvironmentBoardRespVO.Record saveRecordInternal(QmsEnvironmentRecordSaveReqVO reqVO,
                                                                String correctionReason) {
        if (reqVO.getTemperatureValue() == null || reqVO.getHumidityValue() == null) {
            throw exception(QMS_ENVIRONMENT_RECORD_VALUE_REQUIRED);
        }
        Long tenantId = currentTenantId();
        String workshopCode = normalizeCode(reqVO.getWorkshopCode());
        String workshopName = normalizeName(reqVO.getWorkshopName(), workshopCode);
        QmsEnvironmentStandardDO standard = getOrDefaultStandard(workshopCode, workshopName, tenantId);
        QmsEnvironmentRecordDO record = upsertRecord(workshopCode, workshopName, reqVO.getRecordDate(),
                reqVO.getTemperatureValue(), reqVO.getHumidityValue(), reqVO.getRecorderId(),
                reqVO.getRecorderUsername(), reqVO.getRecorderName(),
                buildRemark(reqVO.getRemark(), correctionReason), standard, tenantId);
        return toRecordResp(record, standard);
    }

    private QmsEnvironmentRecordDO upsertRecord(String workshopCode, String workshopName, LocalDate recordDate,
                                                BigDecimal temperatureValue, BigDecimal humidityValue,
                                                Long recorderId, String recorderUsername, String recorderName,
                                                String remark, QmsEnvironmentStandardDO standard, Long tenantId) {
        QmsEnvironmentRecordDO existed = environmentRecordMapper.selectByWorkshopAndDate(
                workshopCode, recordDate, tenantId);
        QmsEnvironmentRecordDO record = existed == null ? new QmsEnvironmentRecordDO() : existed;
        boolean valueChanged = existed == null
                || !sameDecimal(existed.getTemperatureValue(), temperatureValue)
                || !sameDecimal(existed.getHumidityValue(), humidityValue);
        record.setWorkshopCode(workshopCode);
        record.setWorkshopName(workshopName);
        record.setRecordMonth(MONTH_FORMATTER.format(YearMonth.from(recordDate)));
        record.setRecordDate(recordDate);
        record.setTemperatureValue(temperatureValue);
        record.setHumidityValue(humidityValue);
        applyJudgement(record, standard);
        record.setRecorderId(recorderId);
        record.setRecorderUsername(trimToNull(recorderUsername));
        record.setRecorderName(trimToNull(recorderName));
        record.setRecordTime(LocalDateTime.now());
        if (valueChanged) {
            record.setConfirmerId(null);
            record.setConfirmerUsername(null);
            record.setConfirmerName(null);
            record.setConfirmTime(null);
            record.setConfirmStatus(CONFIRM_STATUS_WAIT_CONFIRM);
        }
        record.setRemark(trimToNull(remark));
        record.setTenantId(tenantId);
        if (existed == null) {
            environmentRecordMapper.insert(record);
        } else {
            environmentRecordMapper.updateByRecordSave(record);
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsEnvironmentBoardRespVO.Record confirmRecord(QmsEnvironmentRecordConfirmReqVO reqVO) {
        Long tenantId = currentTenantId();
        String workshopCode = normalizeCode(reqVO.getWorkshopCode());
        String workshopName = normalizeName(reqVO.getWorkshopName(), workshopCode);
        QmsEnvironmentRecordDO record = environmentRecordMapper.selectByWorkshopAndDate(
                workshopCode, reqVO.getRecordDate(), tenantId);
        if (record == null || record.getTemperatureValue() == null || record.getHumidityValue() == null) {
            throw exception(QMS_ENVIRONMENT_RECORD_NOT_EXISTS);
        }
        QmsEnvironmentStandardDO standard = getOrDefaultStandard(workshopCode, workshopName, tenantId);
        record.setWorkshopName(workshopName);
        applyJudgement(record, standard);
        record.setConfirmerId(reqVO.getConfirmerId());
        record.setConfirmerUsername(trimToNull(reqVO.getConfirmerUsername()));
        record.setConfirmerName(trimToNull(reqVO.getConfirmerName()));
        record.setConfirmTime(LocalDateTime.now());
        record.setConfirmStatus(CONFIRM_STATUS_CONFIRMED);
        environmentRecordMapper.updateConfirmInfo(record);
        return toRecordResp(record, standard);
    }

    @Override
    public List<QmsEnvironmentRecordExcelVO> buildExportList(QmsEnvironmentBoardReqVO reqVO) {
        QmsEnvironmentBoardRespVO board = getBoard(reqVO);
        List<QmsEnvironmentRecordExcelVO> list = new ArrayList<>();
        for (QmsEnvironmentBoardRespVO.Record record : board.getRecords()) {
            QmsEnvironmentRecordExcelVO row = new QmsEnvironmentRecordExcelVO();
            row.setRecordDate(DATE_FORMATTER.format(record.getRecordDate()));
            row.setDay(record.getDay());
            row.setWorkshopCode(board.getWorkshopCode());
            row.setWorkshopName(board.getWorkshopName());
            row.setTemperatureValue(record.getTemperatureValue());
            row.setHumidityValue(record.getHumidityValue());
            row.setTemperatureStatus(statusText(record.getTemperatureStatus()));
            row.setHumidityStatus(statusText(record.getHumidityStatus()));
            row.setOverallStatus(statusText(record.getOverallStatus()));
            row.setRecordStatus(recordStatusText(record.getRecordStatus()));
            row.setRecorderName(record.getRecorderName());
            row.setRecordTime(formatDateTime(record.getRecordTime()));
            row.setConfirmerName(record.getConfirmerName());
            row.setConfirmTime(formatDateTime(record.getConfirmTime()));
            row.setRemark(record.getRemark());
            list.add(row);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsEnvironmentImportRespVO importRecords(String workshopCode, String workshopName, String recordMonth,
                                                    MultipartFile file, Long operatorId, String operatorUsername,
                                                    String operatorName) throws IOException {
        QmsEnvironmentImportRespVO respVO = new QmsEnvironmentImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        if (!isExcelFile(file.getOriginalFilename())) {
            addImportFailure(respVO, "仅支持 .xls 或 .xlsx 格式的温湿度记录");
            return respVO;
        }
        final List<QmsEnvironmentRecordExcelVO> excelRows;
        try (InputStream inputStream = file.getInputStream()) {
            excelRows = FastExcelFactory.read(inputStream, QmsEnvironmentRecordExcelVO.class, null)
                    .autoCloseStream(false)
                    .sheet()
                    .doReadSync();
        } catch (RuntimeException exception) {
            addImportFailure(respVO, "Excel 解析失败，请使用温湿度记录导出文件或模板格式");
            return respVO;
        }
        String normalizedWorkshopCode = normalizeCode(workshopCode);
        String normalizedWorkshopName = normalizeName(workshopName, normalizedWorkshopCode);
        YearMonth month = parseMonth(recordMonth);
        Long tenantId = currentTenantId();
        QmsEnvironmentStandardDO standard = getOrDefaultStandard(normalizedWorkshopCode, normalizedWorkshopName,
                tenantId);
        List<ImportRow> rows = new ArrayList<>();
        for (int index = 0; index < excelRows.size(); index++) {
            QmsEnvironmentRecordExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            ImportRow row = normalizeImportRow(excelRow, rowNo, month, normalizedWorkshopCode, respVO);
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.isEmpty() && respVO.getFailures().isEmpty()) {
            addImportFailure(respVO, "未读取到可导入的温湿度记录");
        }
        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入温湿度记录");
            return respVO;
        }
        String recorderName = trimToNull(operatorName);
        if (recorderName == null) {
            addImportFailure(respVO, "导入记录人不能为空，请先完成认证");
            respVO.setFailureCount(respVO.getFailures().size());
            return respVO;
        }
        for (ImportRow row : rows) {
            upsertRecord(normalizedWorkshopCode, normalizedWorkshopName, row.recordDate(),
                    row.temperatureValue(), row.humidityValue(), operatorId, operatorUsername, recorderName,
                    row.remark(), standard, tenantId);
        }
        respVO.setSuccessCount(rows.size());
        respVO.getMessages().add(String.format("温湿度记录导入完成：成功 %d 行", respVO.getSuccessCount()));
        return respVO;
    }

    private List<QmsEnvironmentBoardRespVO.Record> buildMonthRecords(YearMonth month,
                                                                    List<QmsEnvironmentRecordDO> sourceRecords,
                                                                    QmsEnvironmentStandardDO standard) {
        Map<Integer, QmsEnvironmentRecordDO> recordMap = sourceRecords.stream()
                .collect(Collectors.toMap(record -> record.getRecordDate().getDayOfMonth(), Function.identity(),
                        (left, right) -> right));
        List<QmsEnvironmentBoardRespVO.Record> records = new ArrayList<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            QmsEnvironmentRecordDO source = recordMap.get(day);
            if (source == null) {
                records.add(QmsEnvironmentBoardRespVO.Record.builder()
                        .day(day)
                        .recordDate(month.atDay(day))
                        .temperatureStatus(STATUS_MISSING)
                        .humidityStatus(STATUS_MISSING)
                        .overallStatus(STATUS_MISSING)
                        .recordStatus(RECORD_STATUS_WAIT_RECORD)
                        .confirmStatus(CONFIRM_STATUS_WAIT_CONFIRM)
                        .build());
            } else {
                records.add(toRecordResp(source, standard));
            }
        }
        return records;
    }

    private QmsEnvironmentBoardRespVO.Summary buildSummary(List<QmsEnvironmentBoardRespVO.Record> records) {
        int recorded = 0;
        int confirmed = 0;
        int abnormal = 0;
        int pendingConfirm = 0;
        for (QmsEnvironmentBoardRespVO.Record record : records) {
            if (!RECORD_STATUS_WAIT_RECORD.equals(record.getRecordStatus())) {
                recorded++;
            }
            if (RECORD_STATUS_CONFIRMED.equals(record.getRecordStatus())) {
                confirmed++;
            }
            if (STATUS_NG.equals(record.getOverallStatus())) {
                abnormal++;
            }
            if (RECORD_STATUS_WAIT_CONFIRM.equals(record.getRecordStatus())) {
                pendingConfirm++;
            }
        }
        return QmsEnvironmentBoardRespVO.Summary.builder()
                .recordedDays(recorded)
                .confirmedDays(confirmed)
                .abnormalDays(abnormal)
                .pendingConfirmDays(pendingConfirm)
                .build();
    }

    private QmsEnvironmentStandardDO getOrDefaultStandard(String workshopCode, String workshopName, Long tenantId) {
        QmsEnvironmentStandardDO existed = environmentStandardMapper.selectByWorkshop(workshopCode, tenantId);
        if (existed != null) {
            return existed;
        }
        return QmsEnvironmentStandardDO.builder()
                .workshopCode(workshopCode)
                .workshopName(workshopName)
                .temperatureMin(DEFAULT_TEMPERATURE_MIN)
                .temperatureMax(DEFAULT_TEMPERATURE_MAX)
                .humidityMin(DEFAULT_HUMIDITY_MIN)
                .humidityMax(DEFAULT_HUMIDITY_MAX)
                .tenantId(tenantId)
                .build();
    }

    private void applyJudgement(QmsEnvironmentRecordDO record, QmsEnvironmentStandardDO standard) {
        record.setTemperatureStatus(judge(record.getTemperatureValue(),
                standard.getTemperatureMin(), standard.getTemperatureMax()));
        record.setHumidityStatus(judge(record.getHumidityValue(), standard.getHumidityMin(), standard.getHumidityMax()));
        record.setOverallStatus(STATUS_OK.equals(record.getTemperatureStatus())
                && STATUS_OK.equals(record.getHumidityStatus()) ? STATUS_OK : STATUS_NG);
    }

    private QmsEnvironmentBoardRespVO.Standard toStandardResp(QmsEnvironmentStandardDO standard) {
        return QmsEnvironmentBoardRespVO.Standard.builder()
                .id(standard.getId())
                .workshopCode(standard.getWorkshopCode())
                .workshopName(standard.getWorkshopName())
                .temperatureMin(standard.getTemperatureMin())
                .temperatureMax(standard.getTemperatureMax())
                .humidityMin(standard.getHumidityMin())
                .humidityMax(standard.getHumidityMax())
                .remark(standard.getRemark())
                .build();
    }

    private QmsEnvironmentBoardRespVO.Record toRecordResp(QmsEnvironmentRecordDO record,
                                                          QmsEnvironmentStandardDO standard) {
        String temperatureStatus = judge(record.getTemperatureValue(), standard.getTemperatureMin(),
                standard.getTemperatureMax());
        String humidityStatus = judge(record.getHumidityValue(), standard.getHumidityMin(), standard.getHumidityMax());
        String overallStatus = STATUS_OK.equals(temperatureStatus) && STATUS_OK.equals(humidityStatus)
                ? STATUS_OK : STATUS_NG;
        return QmsEnvironmentBoardRespVO.Record.builder()
                .id(record.getId())
                .day(record.getRecordDate().getDayOfMonth())
                .recordDate(record.getRecordDate())
                .temperatureValue(record.getTemperatureValue())
                .humidityValue(record.getHumidityValue())
                .temperatureStatus(temperatureStatus)
                .humidityStatus(humidityStatus)
                .overallStatus(overallStatus)
                .recordStatus(resolveRecordStatus(record))
                .recorderId(record.getRecorderId())
                .recorderUsername(record.getRecorderUsername())
                .recorderName(record.getRecorderName())
                .recordTime(record.getRecordTime())
                .confirmerId(record.getConfirmerId())
                .confirmerUsername(record.getConfirmerUsername())
                .confirmerName(record.getConfirmerName())
                .confirmTime(record.getConfirmTime())
                .confirmStatus(record.getConfirmStatus())
                .remark(record.getRemark())
                .build();
    }

    private String resolveRecordStatus(QmsEnvironmentRecordDO record) {
        if (record.getTemperatureValue() == null || record.getHumidityValue() == null) {
            return RECORD_STATUS_WAIT_RECORD;
        }
        return CONFIRM_STATUS_CONFIRMED.equals(record.getConfirmStatus())
                ? RECORD_STATUS_CONFIRMED : RECORD_STATUS_WAIT_CONFIRM;
    }

    private ImportRow normalizeImportRow(QmsEnvironmentRecordExcelVO excelRow, int rowNo, YearMonth month,
                                         String workshopCode, QmsEnvironmentImportRespVO respVO) {
        LocalDate recordDate = resolveImportDate(excelRow, rowNo, month, respVO);
        if (recordDate == null) {
            return null;
        }
        if (!YearMonth.from(recordDate).equals(month)) {
            addImportFailure(respVO, String.format("第%d行：记录日期不属于当前筛选月份 %s", rowNo,
                    MONTH_FORMATTER.format(month)));
        }
        if (recordDate.isAfter(LocalDate.now())) {
            addImportFailure(respVO, String.format("第%d行：不能导入未来日期的温湿度记录", rowNo));
        }
        String rowWorkshopCode = trimToNull(excelRow.getWorkshopCode());
        if (rowWorkshopCode != null && !workshopCode.equalsIgnoreCase(rowWorkshopCode)) {
            addImportFailure(respVO, String.format("第%d行：车间编码与当前筛选车间不一致", rowNo));
        }
        if (excelRow.getTemperatureValue() == null || excelRow.getHumidityValue() == null) {
            addImportFailure(respVO, String.format("第%d行：温度和湿度不能为空", rowNo));
        }
        return new ImportRow(recordDate, excelRow.getTemperatureValue(), excelRow.getHumidityValue(),
                trimToNull(excelRow.getRemark()));
    }

    private LocalDate resolveImportDate(QmsEnvironmentRecordExcelVO excelRow, int rowNo, YearMonth month,
                                        QmsEnvironmentImportRespVO respVO) {
        String text = trimToNull(excelRow.getRecordDate());
        if (text != null) {
            LocalDate parsedDate = parseImportDate(text);
            if (parsedDate != null) {
                return parsedDate;
            }
            addImportFailure(respVO, String.format("第%d行：日期格式不正确，请填写 yyyy-MM-dd", rowNo));
            return null;
        }
        Integer day = excelRow.getDay();
        if (day == null || day < 1 || day > month.lengthOfMonth()) {
            addImportFailure(respVO, String.format("第%d行：日期不能为空，或日序号不在当前月份范围内", rowNo));
            return null;
        }
        return month.atDay(day);
    }

    private LocalDate parseImportDate(String text) {
        String normalized = text.trim().replace('/', '-');
        int blankIndex = normalized.indexOf(' ');
        if (blankIndex > 0) {
            normalized = normalized.substring(0, blankIndex);
        }
        try {
            if (normalized.matches("\\d+(\\.0+)?")) {
                long serialDay = Long.parseLong(normalized.replaceAll("\\.0+$", ""));
                return LocalDate.of(1899, 12, 30).plusDays(serialDay);
            }
            return LocalDate.parse(normalized, FLEXIBLE_DATE_FORMATTER);
        } catch (DateTimeParseException | NumberFormatException exception) {
            return null;
        }
    }

    private boolean isBlankImportRow(QmsEnvironmentRecordExcelVO row) {
        return row == null
                || (row.getTemperatureValue() == null
                && row.getHumidityValue() == null
                && !StringUtils.hasText(row.getRemark()));
    }

    private void addImportFailure(QmsEnvironmentImportRespVO respVO, String failure) {
        respVO.getFailures().add(failure);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private boolean isExcelFile(String fileName) {
        String lowerFileName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx");
    }

    private void ensureTodayRecord(LocalDate recordDate) {
        if (!LocalDate.now().equals(recordDate)) {
            throw exception(QMS_ENVIRONMENT_HISTORY_LOCKED);
        }
    }

    private void ensureNotFutureRecord(LocalDate recordDate) {
        if (recordDate.isAfter(LocalDate.now())) {
            throw exception(QMS_ENVIRONMENT_FUTURE_DATE_INVALID);
        }
    }

    private String buildRemark(String remark, String correctionReason) {
        String normalizedRemark = trimToNull(remark);
        String normalizedReason = trimToNull(correctionReason);
        if (normalizedReason == null) {
            return limitText(normalizedRemark);
        }
        String correctionRemark = "修正原因：" + normalizedReason;
        if (normalizedRemark == null) {
            return limitText(correctionRemark);
        }
        return limitText(normalizedRemark + "；" + correctionRemark);
    }

    private String statusText(String status) {
        if (STATUS_OK.equals(status)) {
            return "合格";
        }
        if (STATUS_NG.equals(status)) {
            return "异常";
        }
        if (STATUS_MISSING.equals(status)) {
            return "未记录";
        }
        return status;
    }

    private String recordStatusText(String status) {
        if (RECORD_STATUS_CONFIRMED.equals(status)) {
            return "已确认";
        }
        if (RECORD_STATUS_WAIT_CONFIRM.equals(status)) {
            return "待确认";
        }
        if (RECORD_STATUS_WAIT_RECORD.equals(status)) {
            return "待记录";
        }
        return status;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : DATE_TIME_FORMATTER.format(time);
    }

    private String limitText(String value) {
        if (value == null || value.length() <= REMARK_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, REMARK_MAX_LENGTH);
    }

    private String judge(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return STATUS_MISSING;
        }
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0 ? STATUS_OK : STATUS_NG;
    }

    private void validateRange(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw exception(QMS_ENVIRONMENT_STANDARD_RANGE_INVALID);
        }
    }

    private YearMonth parseMonth(String recordMonth) {
        try {
            return YearMonth.parse(recordMonth, MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw exception(QMS_ENVIRONMENT_MONTH_INVALID);
        }
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private String normalizeCode(String code) {
        String normalized = trimToNull(code);
        return normalized == null ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name, String fallbackCode) {
        return StringUtils.hasText(name) ? name.trim() : fallbackCode;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long currentTenantId() {
        return Objects.requireNonNullElse(TenantContextHolder.getTenantId(), 1L);
    }

    private record ImportRow(LocalDate recordDate, BigDecimal temperatureValue, BigDecimal humidityValue,
                             String remark) {
    }
}
