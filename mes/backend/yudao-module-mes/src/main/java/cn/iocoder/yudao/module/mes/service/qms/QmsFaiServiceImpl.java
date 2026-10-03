package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiBindStandardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRetentionDestroyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRetentionExpireTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSheetTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiStandardItemCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemAuditHistoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemGroupAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiRecheckApplyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiReturnRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiScanRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetFieldDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetImportBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetSectionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetCellValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetStatResultDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsInspectionStandardSwitchLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemAuditHistoryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemGroupAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiRecheckApplyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiReturnRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiScanRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetImportBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetSectionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetCellValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetStatResultMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsInspectionStandardSwitchLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.service.hc.productmodel.HcProductModelService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_FINISHED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_ATTACHMENT_DISABLED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_ATTACHMENT_TOO_MANY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_AUDIT_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_AUDIT_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_ITEM_AUDIT_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_ITEM_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_OPERATOR_ITEMS_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_QA_ITEMS_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_RECHECK_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_RECHECK_APPLY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_RECHECK_APPLY_GENERATED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_RECHECK_APPLY_PENDING;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_RECHECK_APPLY_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_RETURN_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_STANDARD_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_STANDARD_ALREADY_BOUND;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_STANDARD_BIND_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_STANDARD_BIND_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_STANDARD_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_SHEET_IMPORT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_SHEET_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_WAIT_QA_REQUIRED;

@Service
@Validated
public class QmsFaiServiceImpl implements QmsFaiService {
    @jakarta.annotation.Resource
    @org.springframework.context.annotation.Lazy
    private cn.iocoder.yudao.module.mes.service.hc.processreport.HcFinishedPackagingService coaFreezePackagingService;


    private static final int MAX_ITEM_ATTACHMENT_COUNT = 10;
    private static final String APPLY_TYPE_FAI = "FAI";
    private static final Integer ENABLED = 1;
    private static final Integer AUDITED = 20;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_WAITING_QA = "WAITING_QA";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_REWORKING = "REWORKING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String RECHECK_APPLY_STATUS_PENDING_AUDIT = "PENDING_AUDIT";
    private static final String RECHECK_APPLY_STATUS_APPROVED = "APPROVED";
    private static final String RECHECK_APPLY_STATUS_REJECTED = "REJECTED";
    private static final String SCAN_TARGET_FAI_NO = "FAI_NO";
    private static final String SCAN_TARGET_WORK_ORDER_NO = "WORK_ORDER_NO";
    private static final String SCAN_TARGET_PRODUCT_BATCH_NO = "PRODUCT_BATCH_NO";
    private static final String SCAN_TARGET_MACHINE_CODE = "MACHINE_CODE";
    private static final String SCAN_TARGET_MATERIAL_CODE = "MATERIAL_CODE";
    private static final String SCAN_TARGET_TEMPLATE_CODE = "TEMPLATE_CODE";
    private static final String SCAN_TARGET_INSPECTION_ITEM = "INSPECTION_ITEM";
    private static final String SCAN_TARGET_UNKNOWN = "UNKNOWN";
    private static final String SCAN_RESULT_MATCHED_SINGLE = "MATCHED_SINGLE";
    private static final String SCAN_RESULT_MATCHED_MULTIPLE = "MATCHED_MULTIPLE";
    private static final String SCAN_RESULT_NOT_FOUND = "NOT_FOUND";
    private static final String SCAN_RESULT_STATUS_BLOCKED = "STATUS_BLOCKED";
    private static final String SCAN_RESULT_TEMPLATE_MISSING = "TEMPLATE_MISSING";
    private static final String SCAN_SCENE_LEDGER_TOOLBAR = "LEDGER_TOOLBAR";
    private static final String OPEN_TARGET_WORKBENCH = "WORKBENCH";
    private static final String OPEN_TARGET_ITEM_MODAL = "ITEM_MODAL";
    private static final String OPEN_TARGET_REPORT = "REPORT";
    private static final String OPEN_TARGET_CANDIDATE_MODAL = "CANDIDATE_MODAL";
    private static final String PROCESS_CATEGORY_WET = "WET";
    private static final String STANDARD_MATCH_PROCESS = "PROCESS";
    private static final String STANDARD_MATCH_MATERIAL = "MATERIAL";
    private static final String STANDARD_MATCH_MATERIAL_PROCESS = "MATERIAL_PROCESS";
    private static final String STANDARD_MATCH_PRODUCT_MODEL_PROCESS = "PRODUCT_MODEL_PROCESS";
    private static final String MATCH_TYPE_EXACT_MODEL = "EXACT_MODEL";
    private static final String MATCH_TYPE_FAMILY_MODEL = "FAMILY_MODEL";
    private static final String MATCH_TYPE_MATERIAL = "MATERIAL";
    private static final String MATCH_TYPE_MATERIAL_PROCESS = "MATERIAL_PROCESS";
    private static final String MATCH_TYPE_PROCESS = "PROCESS";
    private static final String MATCH_TYPE_OVERRIDE = "OVERRIDE";
    private static final int MATCH_SCORE_EXACT_MODEL = 1000;
    private static final int MATCH_SCORE_FAMILY_MODEL = 800;
    private static final int MATCH_SCORE_MATERIAL_PROCESS = 600;
    private static final int MATCH_SCORE_MATERIAL = 400;
    private static final int MATCH_SCORE_PROCESS = 200;
    private static final String SOURCE_MODULE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    /** 包装段 COA 送检属于 FQC 后的复检，不作为工序原检展示。 */
    private static final String SOURCE_MODULE_PACKAGING_COA = "PACKAGING_COA";
    private static final String TRIGGER_REASON_REWORK_RECHECK = "REWORK_RECHECK";
    private static final String FAI_PROCESS_SELF_CHECK_PROCESS_CODE = "FAI_PROCESS_SELF_CHECK";
    private static final String FAI_SELF_CHECK_ELECTRONIC_BALANCE_FORM_CODE = "FAI_SELF_CHECK_ELECTRONIC_BALANCE";
    private static final String FAI_SELF_CHECK_MICROSCOPE_FORM_CODE = "FAI_SELF_CHECK_MICROSCOPE";
    private static final String FAI_SELF_CHECK_HARDNESS_TESTER_FORM_CODE = "FAI_SELF_CHECK_HARDNESS_TESTER";
    private static final String FAI_SELF_CHECK_TENSILE_TESTER_FORM_CODE = "FAI_SELF_CHECK_TENSILE_TESTER";
    private static final String FAI_SELF_CHECK_THICKNESS_GAUGE_FORM_CODE = "FAI_SELF_CHECK_THICKNESS_GAUGE";
    private static final String FAI_SELF_CHECK_COMPRESSION_REBOUND_FORM_CODE = "FAI_SELF_CHECK_COMPRESSION_REBOUND";
    private static final String FAI_SELF_CHECK_3D_PROFILOMETER_FORM_CODE = "FAI_SELF_CHECK_3D_PROFILOMETER";
    private static final String FAI_SELF_CHECK_CONTACT_ANGLE_FORM_CODE = "FAI_SELF_CHECK_CONTACT_ANGLE";
    private static final Map<String, String> FAI_REQUIRED_SELF_CHECK_FORM_NAMES =
            buildFaiRequiredSelfCheckFormNames();
    private static final String FAI_MATERIAL_PROCESS_STANDARD_NOT_FOUND_MSG = "未找到物料编码与工段同时匹配的 FAI 检验标准";
    private static final String FAI_PRODUCT_MODEL_PROCESS_STANDARD_NOT_FOUND_MSG = "未找到产品型号与工段同时匹配的 FAI 检验标准";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_WAIT_QA = "WAIT_QA";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String ROLE_OPERATOR = "OPERATOR";
    private static final String ROLE_QA = "QA";
    private static final String ROLE_SYSTEM = "SYSTEM";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_QUALITATIVE = "QUALITATIVE";
    private static final String TEMPLATE_SINGLE_VALUE = "SINGLE_VALUE";
    private static final String TEMPLATE_DENSITY_CALC = "DENSITY_CALC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final String METRIC_RESULT_VALUE = "RESULT_VALUE";
    private static final String METRIC_DENSITY_VALUE = "DENSITY_VALUE";
    private static final String METRIC_COMPRESSION_RATE = "COMPRESSION_RATE";
    private static final String METRIC_COMPRESSION_ELASTICITY_RATE = "COMPRESSION_ELASTICITY_RATE";
    private static final BigDecimal DEFAULT_SAMPLE_DIAMETER_MM = BigDecimal.valueOf(39);
    private static final BigDecimal PI = BigDecimal.valueOf(3.14);
    private static final int CALC_SCALE = 6;
    private static final String PROCESS_PENDING = "PENDING";
    private static final String ACTION_REWORK = "REWORK";
    private static final String RELEASED = "RELEASED";
    private static final String LOCKED = "LOCKED";
    private static final String RETENTION_UNCONFIRMED = "UNCONFIRMED";
    private static final String RETENTION_RETAINED = "RETAINED";
    private static final String RETENTION_NOT_RETAINED = "NOT_RETAINED";
    private static final String RETENTION_DESTROY_WAIT = "WAIT_DESTROY";
    private static final String RETENTION_DESTROYED = "DESTROYED";
    private static final String RETENTION_UNIT_DAY = "DAY";
    private static final String RETENTION_UNIT_MONTH = "MONTH";
    private static final String TEMPLATE_STATUS_ENABLE = "ENABLE";
    private static final String ENTRY_MODE_MANUAL = "MANUAL";
    private static final String ENTRY_MODE_EXCEL_IMPORT = "EXCEL_IMPORT";
    private static final String ENTRY_MODE_HISTORICAL_IMPORT = "HISTORICAL_IMPORT";
    private static final String ENTRY_LAYOUT_PROGRAM_FORM = "PROGRAM_FORM";
    private static final String ENTRY_LAYOUT_SHEET_GRID = "SHEET_GRID";
    private static final String IMPORT_STATUS_SUCCESS = "SUCCESS";
    private static final String IMPORT_STATUS_VALIDATING = "VALIDATING";
    private static final String IMPORT_STATUS_FAILED = "FAILED";
    private static final String IMPORT_USAGE_HISTORICAL_BACKFILL = "HISTORICAL_BACKFILL";
    private static final String IMPORT_USAGE_ITEM_OVERVIEW_BATCH = "ITEM_OVERVIEW_BATCH";
    private static final String VALUE_SOURCE_MANUAL = "MANUAL";
    private static final String VALUE_SOURCE_ITEM_IMPORT = "ITEM_IMPORT";
    private static final String VALUE_SOURCE_HISTORICAL_IMPORT = "HISTORICAL_IMPORT";
    private static final String VALUE_SOURCE_CALCULATED = "CALCULATED";
    private static final String CELL_STATUS_FILLED = "FILLED";
    private static final String CELL_STATUS_CALCULATED = "CALCULATED";
    private static final String FIELD_ROLE_INPUT = "INPUT";
    private static final String FIELD_ROLE_CALCULATED = "CALCULATED";
    private static final String SECTION_TYPE_GRID_SAMPLE = "GRID_SAMPLE";
    private static final String INPUT_STATUS_EMPTY = "EMPTY";
    private static final String INPUT_STATUS_FILLING = "FILLING";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final String COMPONENT_SINGLE_VALUE_LIST = "SINGLE_VALUE_LIST";
    private static final String COMPONENT_DENSITY_GROUP = "DENSITY_GROUP";
    private static final String COMPONENT_COMPRESSION_GROUP = "COMPRESSION_GROUP";
    private static final String COMPONENT_GROOVE_DEPTH_MATRIX = "GROOVE_DEPTH_MATRIX";
    private static final String COMPONENT_QUALITATIVE_JUDGMENT = "QUALITATIVE_JUDGMENT";
    private static final String AUDIT_PASS = "PASS";
    private static final String AUDIT_REJECT = "REJECT";
    private static final String AUDIT_FAIL = "FAIL";
    private static final String ITEM_AUDIT_PENDING = "PENDING";
    private static final String ITEM_AUDIT_CONFIRM = "CONFIRM";
    private static final String ITEM_AUDIT_REJECT_RECHECK = "REJECT_RECHECK";
    private static final String ITEM_RECHECK_NONE = "NONE";
    private static final String ITEM_RECHECK_WAIT_RECHECK = "WAIT_RECHECK";
    private static final String ITEM_RECHECK_WAIT_AUDIT = "WAIT_AUDIT";
    private static final String ITEM_RECHECK_CONFIRMED = "CONFIRMED";
    private static final long FIXED_TEMPLATE_SINGLE_ID = -1001L;
    private static final long FIXED_TEMPLATE_DENSITY_ID = -1002L;
    private static final long FIXED_TEMPLATE_COMPRESSION_ID = -1003L;
    private static final String FIXED_TEMPLATE_SINGLE_CODE = "FAI_SINGLE_VALUE_ORIGIN_A1";
    private static final String FIXED_TEMPLATE_DENSITY_CODE = "FAI_DENSITY_ORIGIN_A1";
    private static final String FIXED_TEMPLATE_COMPRESSION_CODE = "FAI_COMPRESSION_ORIGIN_A1";
    private static final LocalDateTime MIN_VALID_SUBMISSION_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final int ITEM_OVERVIEW_SAMPLE_START_ROW = 7;
    private static final int ITEM_OVERVIEW_SAMPLE_COUNT = 15;
    private static final int[] ITEM_OVERVIEW_GROUP_COUNTS = {3, 3, 3, 3, 3};
    private static final String[] ITEM_OVERVIEW_GROUP_NAMES = {"L5", "L3", "圆\n心\nO", "R3", "R5"};
    private static final String ITEM_OVERVIEW_META_HASH_CELL = "I1";
    private static final String ITEM_OVERVIEW_META_FAI_ID_CELL = "I2";
    private static final String ITEM_OVERVIEW_META_FAI_NO_CELL = "I3";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter RECHECK_APPLY_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsFaiRecheckApplyMapper qmsFaiRecheckApplyMapper;
    @Resource
    private QmsFaiItemMapper qmsFaiItemMapper;
    @Resource
    private QmsFaiItemAuditHistoryMapper qmsFaiItemAuditHistoryMapper;
    @Resource
    private QmsFaiItemGroupAuditMapper qmsFaiItemGroupAuditMapper;
    @Resource
    private QmsFaiSampleMapper qmsFaiSampleMapper;
    @Resource
    private QmsFaiAbnormalMapper qmsFaiAbnormalMapper;
    @Resource
    private QmsFaiReturnRecordMapper qmsFaiReturnRecordMapper;
    @Resource
    private QmsFaiScanRecordMapper qmsFaiScanRecordMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    private QmsInspectionStandardSwitchLogMapper qmsInspectionStandardSwitchLogMapper;
    @Resource
    private QmsQualityStandardContentHashService qmsQualityStandardContentHashService;
    @Resource
    private QmsFaiSheetTemplateMapper qmsFaiSheetTemplateMapper;
    @Resource
    private QmsFaiSheetSectionMapper qmsFaiSheetSectionMapper;
    @Resource
    private QmsFaiSheetFieldMapper qmsFaiSheetFieldMapper;
    @Resource
    private QmsFaiSheetImportBatchMapper qmsFaiSheetImportBatchMapper;
    @Resource
    private QmsFaiSheetCellValueMapper qmsFaiSheetCellValueMapper;
    @Resource
    private QmsFaiSheetStatResultMapper qmsFaiSheetStatResultMapper;
    @Resource
    private QmsFaiItemWorkbookService qmsFaiItemWorkbookService;
    @Resource
    private QmsFaiSampleResultWritebackService qmsFaiSampleResultWritebackService;
    @Resource
    private QmsAuditTodoNotifyService qmsAuditTodoNotifyService;
    @Resource
    private QmsFaiOaNotifyService qmsFaiOaNotifyService;
    @Resource
    private QmsAbnormalLockService qmsAbnormalLockService;
    @Resource
    private QmsSampleAbnormalLockService qmsSampleAbnormalLockService;
    @Resource
    private QmsNoGeneratorService qmsNoGeneratorService;
    @Resource
    private HcProductModelService hcProductModelService;
    @Resource
    private HcProcessFormRecordMapper hcProcessFormRecordMapper;

    private record ProductModelMatchContext(Long exactModelId, String exactModelCode,
                                            Long familyModelId, String familyModelCode) {
    }

    private record StandardCandidateMatch(QmsQualityStandardDO standard, int score, String matchType,
                                          Long matchedModelId, String matchedModelCode, String matchReason) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFai(QmsFaiSaveReqVO createReqVO) {
        return createFaiInternal(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFaiForInspectionPush(QmsFaiSaveReqVO createReqVO) {
        return createFaiInternal(createReqVO);
    }

    private Long createFaiInternal(QmsFaiSaveReqVO createReqVO) {
        String faiNo = generateFaiNo();
        validateFaiNoUnique(null, faiNo);

        QmsQualityStandardDO standard = null;
        List<QmsQualityStandardItemDO> standardItems = Collections.emptyList();
        String pendingStandardReason = null;
        StandardCandidateMatch selectedMatch = null;
        if (createReqVO.getStandardId() != null) {
            standard = selectFaiStandard(createReqVO);
            selectedMatch = matchStandardForManualSelection(standard, createReqVO);
            if (selectedMatch == null) {
                throw exception(HCFAI_STANDARD_BIND_REASON_REQUIRED);
            }
            standardItems = selectFaiStandardItems(standard.getId(), createReqVO);
        } else {
            List<StandardCandidateMatch> candidates = findFaiStandardCandidateMatches(createReqVO);
            selectedMatch = selectUniqueHighestPriorityMatch(candidates);
            if (selectedMatch != null) {
                standard = selectedMatch.standard();
                standardItems = selectFaiStandardItems(standard.getId(), createReqVO);
            } else {
                pendingStandardReason = candidates.isEmpty()
                        ? "未找到匹配的已审核标准"
                        : "匹配到多个已审核标准，请在开始检验前选择";
            }
        }
        QmsFaiOrderDO entity = BeanUtils.toBean(createReqVO, QmsFaiOrderDO.class);
        entity.setId(null);
        entity.setFaiNo(faiNo);
        if (SOURCE_MODULE_PACKAGING_COA.equals(entity.getSourceModule())) {
            entity.setRecheckFlag(Boolean.TRUE);
        }
        boolean pendingRecheckItemSelection = requiresFaiRecheckItemSelection(entity);
        if (standard != null) {
            applyStandardSnapshot(entity, standard);
            applyStandardMatchSnapshot(entity, selectedMatch);
            entity.setStandardSnapshotHash(
                    qmsQualityStandardContentHashService.hashFaiStandardItems(standardItems));
        } else {
            clearStandardSnapshot(entity);
            entity.setRemark(buildPendingStandardRemark(entity.getRemark(), pendingStandardReason));
        }
        applyDefaultInspectionQty(entity);
        if (pendingRecheckItemSelection) {
            // 复检项目必须在选择标准后由检验员明确选择，不能按标准默认全量生成。
            entity.setRequiredItemCount(0);
            entity.setCompletedItemCount(0);
            entity.setAbnormalItemCount(0);
            entity.setEntryProgress(0);
        }
        entity.setSubmissionTime(resolveCreateSubmissionTime(createReqVO.getSubmissionTime()));
        entity.setSubmitterId(createReqVO.getSubmitterId() == null
                ? SecurityFrameworkUtils.getLoginUserId() : createReqVO.getSubmitterId());
        entity.setSubmitterName(defaultIfBlank(createReqVO.getSubmitterName(), resolveLoginUserName()));
        entity.setStatus(defaultIfBlank(createReqVO.getStatus(), STATUS_PENDING));
        entity.setJudgment(defaultIfBlank(createReqVO.getJudgment(), JUDGMENT_PENDING));
        entity.setReleaseResult(defaultIfBlank(entity.getReleaseResult(), LOCKED));
        entity.setRetentionStatus(defaultIfBlank(createReqVO.getRetentionStatus(), RETENTION_UNCONFIRMED));
        applyRetentionConfirmation(entity, createReqVO.getRetentionStatus(), entity);
        qmsFaiOrderMapper.insert(entity);
        coaFreezePackagingService.syncCoaFreezeForFai(entity.getId());

        if (!pendingRecheckItemSelection && !standardItems.isEmpty()) {
            saveStandardSnapshotDetails(entity, standardItems, createReqVO.getAbnormals());
        } else {
            saveAbnormals(entity, createReqVO.getAbnormals());
        }
        qmsFaiOaNotifyService.sendCreated(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO bindStandard(QmsFaiBindStandardReqVO bindReqVO) {
        QmsFaiOrderDO order = validateFaiExists(bindReqVO.getId());
        validateEditable(order);
        if (order.getStandardId() != null) {
            throw exception(HCFAI_STANDARD_ALREADY_BOUND);
        }
        List<QmsFaiItemDO> existingItems = qmsFaiItemMapper.selectListByFaiId(order.getId());
        if (existingItems != null && !existingItems.isEmpty()) {
            throw exception(HCFAI_STANDARD_ALREADY_BOUND);
        }

        QmsFaiSaveReqVO scopeReqVO = BeanUtils.toBean(order, QmsFaiSaveReqVO.class);
        scopeReqVO.setStandardId(bindReqVO.getStandardId());
        QmsQualityStandardDO standard = selectFaiStandardForManualBind(scopeReqVO);
        List<QmsQualityStandardItemDO> standardItems = resolveSelectedStandardItems(order,
                selectFaiStandardItems(standard.getId(), scopeReqVO), bindReqVO.getSelectedStandardItemIds());
        StandardCandidateMatch selectedMatch = matchStandardForManualSelection(standard, scopeReqVO);
        boolean scopeMatched = selectedMatch != null;
        if (!scopeMatched && !StringUtils.hasText(bindReqVO.getOverrideReason())) {
            throw exception(HCFAI_STANDARD_BIND_REASON_REQUIRED);
        }
        if (selectedMatch == null) {
            selectedMatch = buildOverrideMatch(standard, bindReqVO.getOverrideReason());
        }

        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        updateObj.setStandardId(standard.getId());
        updateObj.setStandardNo(standard.getStandardNo());
        updateObj.setStandardVersion(standard.getVersion());
        updateObj.setStandardSnapshotHash(
                qmsQualityStandardContentHashService.hashFaiStandardItems(standardItems));
        applyStandardMatchSnapshot(updateObj, selectedMatch);
        updateObj.setRequiredItemCount(standardItems.size());
        updateObj.setCompletedItemCount(0);
        updateObj.setAbnormalItemCount(0);
        updateObj.setEntryProgress(0);
        updateObj.setRemark(buildManualStandardBindRemark(order.getRemark(), standard, bindReqVO.getOverrideReason()));
        int updated = qmsFaiOrderMapper.update(updateObj, new LambdaUpdateWrapper<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getId, order.getId())
                .isNull(QmsFaiOrderDO::getStandardId));
        if (updated == 0) {
            throw exception(HCFAI_STANDARD_ALREADY_BOUND);
        }

        order.setStandardId(standard.getId());
        order.setStandardNo(standard.getStandardNo());
        order.setStandardVersion(standard.getVersion());
        order.setStandardSnapshotHash(updateObj.getStandardSnapshotHash());
        applyStandardMatchSnapshot(order, selectedMatch);
        order.setRequiredItemCount(standardItems.size());
        order.setCompletedItemCount(0);
        order.setAbnormalItemCount(0);
        order.setEntryProgress(0);
        order.setRemark(updateObj.getRemark());
        saveStandardSnapshotDetails(order, standardItems, null);
        return getFaiResp(order.getId());
    }

    @Override
    public List<QmsInspectionStandardCandidateRespVO> getStandardCandidates(Long id) {
        QmsFaiOrderDO order = validateFaiExists(id);
        QmsFaiSaveReqVO scope = BeanUtils.toBean(order, QmsFaiSaveReqVO.class);
        List<StandardCandidateMatch> matches = findFaiStandardCandidateMatches(scope);
        StandardCandidateMatch recommended = selectUniqueHighestPriorityMatch(matches);
        return matches.stream()
                .map(match -> buildStandardCandidate(match,
                        recommended != null && Objects.equals(recommended.standard().getId(), match.standard().getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<QmsFaiStandardItemCandidateRespVO> getStandardItemCandidates(Long id, Long standardId) {
        QmsFaiOrderDO order = validateFaiExists(id);
        QmsFaiSaveReqVO scope = BeanUtils.toBean(order, QmsFaiSaveReqVO.class);
        scope.setStandardId(standardId);
        QmsQualityStandardDO standard = selectFaiStandardForManualBind(scope);
        Set<Long> originalNgStandardItemIds = Collections.emptySet();
        if (order.getRejectPrevInspectionId() != null) {
            originalNgStandardItemIds = qmsFaiItemMapper.selectListByFaiId(order.getRejectPrevInspectionId()).stream()
                    .filter(item -> item.getStandardItemId() != null)
                    .filter(item -> JUDGMENT_NG.equals(item.getOperatorResult())
                            || JUDGMENT_NG.equals(item.getQaResult()))
                    .map(QmsFaiItemDO::getStandardItemId)
                    .collect(Collectors.toSet());
        }
        final Set<Long> ngItemIds = originalNgStandardItemIds;
        return selectFaiStandardItems(standard.getId(), scope).stream()
                .sorted(Comparator.comparing(QmsQualityStandardItemDO::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> buildFaiStandardItemCandidate(item, ngItemIds.contains(item.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO selectStandard(QmsInspectionStandardSelectReqVO reqVO) {
        QmsFaiOrderDO order = validateFaiExists(reqVO.getId());
        validateFaiStandardSwitchAllowed(order);
        QmsFaiSaveReqVO scope = BeanUtils.toBean(order, QmsFaiSaveReqVO.class);
        scope.setStandardId(reqVO.getStandardId());
        QmsQualityStandardDO standard = selectFaiStandardForManualBind(scope);
        StandardCandidateMatch selectedMatch = matchStandardForManualSelection(standard, scope);
        if (selectedMatch == null) {
            throw exception(HCFAI_STANDARD_BIND_REASON_REQUIRED);
        }
        List<QmsQualityStandardItemDO> standardItems = resolveSelectedStandardItems(order,
                selectFaiStandardItems(standard.getId(), scope), reqVO.getSelectedStandardItemIds());
        List<QmsFaiItemDO> oldItems = qmsFaiItemMapper.selectListByFaiId(order.getId());
        String oldHash = StringUtils.hasText(order.getStandardSnapshotHash())
                ? order.getStandardSnapshotHash()
                : qmsQualityStandardContentHashService.hashFaiSnapshotItems(oldItems);
        String newHash = qmsQualityStandardContentHashService.hashFaiStandardItems(standardItems);
        ReusableFaiStandardSnapshot reusableSnapshot = findReusableFaiStandardSnapshot(
                order, standard, standardItems, newHash);
        if (Objects.equals(order.getStandardId(), standard.getId()) && Objects.equals(oldHash, newHash)
                && hasSameStandardItemSnapshot(oldItems, standardItems)) {
            restoreReusableFaiStandardSnapshotIfSafe(order, oldItems, standardItems, reusableSnapshot);
            // 同一版本标准无需重建检验项，避免误点“重新加载”清空当前录入数据。
            return getFaiResp(order.getId());
        }
        String oldSnapshotJson = buildFaiSwitchSnapshot(order, oldItems);

        qmsFaiSheetCellValueMapper.deleteByFaiId(order.getId());
        qmsFaiSheetStatResultMapper.deleteByFaiId(order.getId());
        qmsFaiSampleMapper.deleteByFaiId(order.getId());
        qmsFaiAbnormalMapper.deleteByFaiId(order.getId());
        qmsFaiReturnRecordMapper.deleteByFaiId(order.getId());
        qmsFaiItemAuditHistoryMapper.deleteByFaiId(order.getId());
        qmsFaiItemGroupAuditMapper.deleteByFaiId(order.getId());
        qmsFaiItemMapper.deleteByFaiId(order.getId());

        LocalDateTime now = LocalDateTime.now();
        qmsFaiOrderMapper.update(null, new LambdaUpdateWrapper<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getId, order.getId())
                .set(QmsFaiOrderDO::getStandardId, standard.getId())
                .set(QmsFaiOrderDO::getStandardNo, standard.getStandardNo())
                .set(QmsFaiOrderDO::getStandardVersion, standard.getVersion())
                .set(QmsFaiOrderDO::getStandardSnapshotHash, newHash)
                .set(QmsFaiOrderDO::getStandardMatchType, selectedMatch.matchType())
                .set(QmsFaiOrderDO::getMatchedModelId, selectedMatch.matchedModelId())
                .set(QmsFaiOrderDO::getMatchedModelCode, selectedMatch.matchedModelCode())
                .set(QmsFaiOrderDO::getStandardMatchReason, selectedMatch.matchReason())
                .set(QmsFaiOrderDO::getStatus, STATUS_PENDING)
                .set(QmsFaiOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsFaiOrderDO::getOperatorId, null)
                .set(QmsFaiOrderDO::getOperatorName, null)
                .set(QmsFaiOrderDO::getOperatorTime, null)
                .set(QmsFaiOrderDO::getQaInspectorId, null)
                .set(QmsFaiOrderDO::getQaInspectorName, null)
                .set(QmsFaiOrderDO::getQaTime, null)
                .set(QmsFaiOrderDO::getInspectionTime, null)
                .set(QmsFaiOrderDO::getReleaseResult, LOCKED)
                .set(QmsFaiOrderDO::getReleaseTime, null)
                .set(QmsFaiOrderDO::getRetentionStatus, RETENTION_UNCONFIRMED)
                .set(QmsFaiOrderDO::getRetentionConfirmTime, null)
                .set(QmsFaiOrderDO::getRetentionConfirmUserId, null)
                .set(QmsFaiOrderDO::getRetentionConfirmUserName, null)
                .set(QmsFaiOrderDO::getRetentionPeriodValue, null)
                .set(QmsFaiOrderDO::getRetentionPeriodUnit, null)
                .set(QmsFaiOrderDO::getRetentionExpireTime, null)
                .set(QmsFaiOrderDO::getRetentionDestroyStatus, null)
                .set(QmsFaiOrderDO::getRetentionDestroyTime, null)
                .set(QmsFaiOrderDO::getRetentionDestroyUserId, null)
                .set(QmsFaiOrderDO::getRetentionDestroyUserName, null)
                .set(QmsFaiOrderDO::getRetentionDestroyRemark, null)
                .set(QmsFaiOrderDO::getAuditNotifyTime, null)
                .set(QmsFaiOrderDO::getAuditRemark, null)
                .set(QmsFaiOrderDO::getEntryMode, ENTRY_MODE_MANUAL)
                .set(QmsFaiOrderDO::getCurrentStepCode, null)
                .set(QmsFaiOrderDO::getEntryProgress, 0)
                .set(QmsFaiOrderDO::getRequiredItemCount, standardItems.size())
                .set(QmsFaiOrderDO::getCompletedItemCount, 0)
                .set(QmsFaiOrderDO::getAbnormalItemCount, 0)
                .set(QmsFaiOrderDO::getLastSaveTime, null)
                .set(QmsFaiOrderDO::getLastCalculateTime, null)
                .set(QmsFaiOrderDO::getLastImportBatchNo, null)
                .set(QmsFaiOrderDO::getHistoricalBackfill, false)
                .set(QmsFaiOrderDO::getSheetLocked, false)
                .set(QmsFaiOrderDO::getReturnCount, 0)
                .set(QmsFaiOrderDO::getLastReturnReason, null));
        if (reusableSnapshot != null) {
            qmsFaiOrderMapper.update(null, new LambdaUpdateWrapper<QmsFaiOrderDO>()
                    .eq(QmsFaiOrderDO::getId, order.getId())
                    .set(QmsFaiOrderDO::getSheetTemplateId, reusableSnapshot.order().getSheetTemplateId())
                    .set(QmsFaiOrderDO::getSheetTemplateCode, reusableSnapshot.order().getSheetTemplateCode())
                    .set(QmsFaiOrderDO::getSheetTemplateName, reusableSnapshot.order().getSheetTemplateName())
                    .set(QmsFaiOrderDO::getSheetTemplateVersion, reusableSnapshot.order().getSheetTemplateVersion()));
        }

        QmsFaiOrderDO refreshed = validateFaiExists(order.getId());
        saveStandardSnapshotDetails(refreshed, standardItems, null,
                reusableSnapshot == null ? Collections.emptyMap() : reusableSnapshot.itemIdsByStandardItemId());
        saveStandardSwitchLog(resolveInspectionType(order), order.getId(), order.getFaiNo(),
                order.getStandardId(), order.getStandardNo(), oldHash,
                standard.getId(), standard.getStandardNo(), newHash,
                oldSnapshotJson, reqVO.getReason(), order.getTenantId(), now);
        return getFaiResp(order.getId());
    }

    private ReusableFaiStandardSnapshot findReusableFaiStandardSnapshot(QmsFaiOrderDO order,
                                                                          QmsQualityStandardDO standard,
                                                                          List<QmsQualityStandardItemDO> standardItems,
                                                                          String contentHash) {
        if (!StringUtils.hasText(contentHash)) {
            return null;
        }
        List<QmsInspectionStandardSwitchLogDO> logs = qmsInspectionStandardSwitchLogMapper.selectList(
                new LambdaQueryWrapperX<QmsInspectionStandardSwitchLogDO>()
                        .eq(QmsInspectionStandardSwitchLogDO::getInspectionType, resolveInspectionType(order))
                        .eq(QmsInspectionStandardSwitchLogDO::getInspectionId, order.getId())
                        .eq(QmsInspectionStandardSwitchLogDO::getOldStandardId, standard.getId())
                        .eq(QmsInspectionStandardSwitchLogDO::getOldContentHash, contentHash)
                        .orderByAsc(QmsInspectionStandardSwitchLogDO::getSwitchTime)
                        .orderByAsc(QmsInspectionStandardSwitchLogDO::getId));
        for (QmsInspectionStandardSwitchLogDO log : logs) {
            try {
                QmsFaiOrderDO snapshotOrder = JsonUtils.parseObject(log.getOldSnapshotJson(), "order", QmsFaiOrderDO.class);
                List<QmsFaiItemDO> snapshotItems = JsonUtils.parseObject(
                        JsonUtils.parseTree(log.getOldSnapshotJson()).path("items").toString(),
                        new TypeReference<List<QmsFaiItemDO>>() {});
                Map<Long, Long> itemIdsByStandardItemId = snapshotItems.stream()
                        .filter(item -> item.getStandardItemId() != null && item.getId() != null)
                        .collect(Collectors.toMap(QmsFaiItemDO::getStandardItemId, QmsFaiItemDO::getId,
                                (first, ignored) -> first, LinkedHashMap::new));
                boolean complete = snapshotOrder != null
                        && itemIdsByStandardItemId.size() == standardItems.size()
                        && standardItems.stream().allMatch(item -> itemIdsByStandardItemId.containsKey(item.getId()));
                if (complete) {
                    return new ReusableFaiStandardSnapshot(snapshotOrder, itemIdsByStandardItemId);
                }
            } catch (RuntimeException ignored) {
                // 单条历史快照损坏时跳过，不影响当前标准切换。
            }
        }
        return null;
    }

    private void restoreReusableFaiStandardSnapshotIfSafe(QmsFaiOrderDO order, List<QmsFaiItemDO> currentItems,
                                                          List<QmsQualityStandardItemDO> standardItems,
                                                          ReusableFaiStandardSnapshot reusableSnapshot) {
        if (reusableSnapshot == null || isUsingReusableFaiItemIds(currentItems, reusableSnapshot)
                || hasFaiExecutionData(order, currentItems)) {
            return;
        }
        qmsFaiItemMapper.deleteByFaiId(order.getId());
        QmsFaiOrderDO snapshotOrder = reusableSnapshot.order();
        qmsFaiOrderMapper.update(null, new LambdaUpdateWrapper<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getId, order.getId())
                .set(QmsFaiOrderDO::getSheetTemplateId, snapshotOrder.getSheetTemplateId())
                .set(QmsFaiOrderDO::getSheetTemplateCode, snapshotOrder.getSheetTemplateCode())
                .set(QmsFaiOrderDO::getSheetTemplateName, snapshotOrder.getSheetTemplateName())
                .set(QmsFaiOrderDO::getSheetTemplateVersion, snapshotOrder.getSheetTemplateVersion())
                .set(QmsFaiOrderDO::getEntryLayout, snapshotOrder.getEntryLayout()));
        saveStandardSnapshotDetails(validateFaiExists(order.getId()), standardItems, null,
                reusableSnapshot.itemIdsByStandardItemId());
    }

    private boolean isUsingReusableFaiItemIds(List<QmsFaiItemDO> currentItems,
                                               ReusableFaiStandardSnapshot reusableSnapshot) {
        return currentItems.size() == reusableSnapshot.itemIdsByStandardItemId().size()
                && currentItems.stream().allMatch(item -> Objects.equals(item.getId(),
                reusableSnapshot.itemIdsByStandardItemId().get(item.getStandardItemId())));
    }

    private boolean hasFaiExecutionData(QmsFaiOrderDO order, List<QmsFaiItemDO> items) {
        if (order.getEntryProgress() != null && order.getEntryProgress() > 0) {
            return true;
        }
        if (!qmsFaiSampleMapper.selectListByFaiId(order.getId()).isEmpty()
                || !qmsFaiAbnormalMapper.selectListByFaiId(order.getId()).isEmpty()) {
            return true;
        }
        return items.stream().anyMatch(item -> !CollectionUtils.isEmpty(item.getAttachmentUrls())
                || !JUDGMENT_PENDING.equals(item.getOperatorResult())
                || !JUDGMENT_PENDING.equals(item.getQaResult()));
    }

    private record ReusableFaiStandardSnapshot(QmsFaiOrderDO order, Map<Long, Long> itemIdsByStandardItemId) {
    }

    private void clearStandardSnapshot(QmsFaiOrderDO order) {
        order.setStandardId(null);
        order.setStandardNo(null);
        order.setStandardVersion(null);
        order.setStandardSnapshotHash(null);
        order.setStandardMatchType(null);
        order.setMatchedModelId(null);
        order.setMatchedModelCode(null);
        order.setStandardMatchReason(null);
    }

    private String buildPendingStandardRemark(String oldRemark, String pendingReason) {
        String pendingRemark = "待选择检验标准"
                + (StringUtils.hasText(pendingReason) ? "：" + pendingReason.trim() : "");
        return appendRemark(oldRemark, pendingRemark);
    }

    private StandardCandidateMatch matchStandardForManualSelection(
            QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        if (isGlueBoardFaiSource(reqVO)) {
            if (StringUtils.hasText(reqVO.getGlueBoardModel())
                    && reqVO.getGlueBoardModel().equals(standard.getGlueBoardModel())) {
                return new StandardCandidateMatch(standard, MATCH_SCORE_EXACT_MODEL,
                        "GLUE_BOARD_MODEL", null, standard.getGlueBoardModel(),
                        "胶板型号与任务完全一致");
            }
            return null;
        }
        ProductModelMatchContext modelContext = resolveProductModelMatchContext(reqVO);
        StandardCandidateMatch matched = evaluateStandardMatch(standard, reqVO, modelContext);
        if (matched != null || StringUtils.hasText(reqVO.getStandardMatchMode())) {
            return matched;
        }
        StandardCandidateMatch productModelMatched = evaluateProductModelProcessMatch(standard, reqVO, modelContext);
        if (productModelMatched != null) {
            return productModelMatched;
        }
        return evaluateMaterialProcessMatch(standard, reqVO);
    }

    private StandardCandidateMatch buildOverrideMatch(QmsQualityStandardDO standard, String overrideReason) {
        String matchedModelCode = defaultIfBlank(standard.getProductModelCode(), standard.getProductModelName());
        String reason = "人工越级选择标准";
        if (StringUtils.hasText(overrideReason)) {
            reason += "：" + overrideReason.trim();
        }
        return new StandardCandidateMatch(standard, 0, MATCH_TYPE_OVERRIDE,
                standard.getProductModelId(), matchedModelCode, reason);
    }

    private QmsQualityStandardDO selectFaiStandardForManualBind(QmsFaiSaveReqVO reqVO) {
        QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(reqVO.getStandardId());
        String expectedApplyType = isGlueBoardFaiSource(reqVO) ? SOURCE_MODULE_GLUE_BOARD_FAI : APPLY_TYPE_FAI;
        if (standard == null
                || !expectedApplyType.equals(standard.getApplyType())
                || !ENABLED.equals(standard.getStatus())
                || !AUDITED.equals(standard.getAuditStatus())) {
            throw exception(HCFAI_STANDARD_NOT_EXISTS);
        }
        return standard;
    }

    private String buildManualStandardBindRemark(String oldRemark, QmsQualityStandardDO standard, String overrideReason) {
        StringBuilder bindRemark = new StringBuilder("检验执行时选择标准 ")
                .append(defaultIfBlank(standard.getStandardNo(), String.valueOf(standard.getId())))
                .append("/")
                .append(defaultIfBlank(standard.getVersion(), "-"));
        if (StringUtils.hasText(overrideReason)) {
            bindRemark.append("，选择原因：").append(overrideReason.trim());
        }
        String operatorName = resolveLoginUserName();
        if (StringUtils.hasText(operatorName)) {
            bindRemark.append("，操作人：").append(operatorName);
        }
        return appendRemark(oldRemark, bindRemark.toString());
    }

    private String appendRemark(String oldRemark, String appendedRemark) {
        final int maxLength = 500;
        String addition = defaultIfBlank(appendedRemark, "");
        if (addition.length() >= maxLength) {
            return addition.substring(0, maxLength);
        }
        if (!StringUtils.hasText(oldRemark)) {
            return addition;
        }
        int oldMaxLength = maxLength - addition.length() - 1;
        String oldPart = oldRemark.length() > oldMaxLength ? oldRemark.substring(0, oldMaxLength) : oldRemark;
        return oldPart + "；" + addition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFai(QmsFaiSaveReqVO updateReqVO) {
        QmsFaiOrderDO old = validateFaiExists(updateReqVO.getId());
        validateEditable(old);
        if (old.getStandardId() == null && updateReqVO.getStandardId() != null) {
            throw exception(HCFAI_STANDARD_BIND_REQUIRED);
        }

        QmsFaiOrderDO updateObj = BeanUtils.toBean(updateReqVO, QmsFaiOrderDO.class);
        updateObj.setFaiNo(old.getFaiNo());
        updateObj.setStandardId(old.getStandardId());
        updateObj.setStandardNo(old.getStandardNo());
        updateObj.setStandardVersion(old.getStandardVersion());
        updateObj.setStandardSnapshotHash(old.getStandardSnapshotHash());
        updateObj.setProductModelId(old.getProductModelId());
        updateObj.setStandardMatchMode(old.getStandardMatchMode());
        updateObj.setStandardMatchType(old.getStandardMatchType());
        updateObj.setMatchedModelId(old.getMatchedModelId());
        updateObj.setMatchedModelCode(old.getMatchedModelCode());
        updateObj.setStandardMatchReason(old.getStandardMatchReason());
        updateObj.setStatus(defaultIfBlank(updateReqVO.getStatus(), old.getStatus()));
        updateObj.setJudgment(defaultIfBlank(updateReqVO.getJudgment(), old.getJudgment()));
        updateObj.setRetentionStatus(defaultIfBlank(updateReqVO.getRetentionStatus(), old.getRetentionStatus()));
        applyRetentionConfirmation(updateObj, updateReqVO.getRetentionStatus(), old);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());

        if (updateReqVO.getItems() != null && !updateReqVO.getItems().isEmpty()) {
            QmsFaiOrderDO updated = validateFaiExists(updateReqVO.getId());
            updateExecutionDetails(updated, updateReqVO.getItems(), updateReqVO.getAbnormals());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFai(Long id) {
        QmsFaiOrderDO entity = validateFaiExists(id);
        validateEditable(entity);
        qmsFaiSheetCellValueMapper.deleteByFaiId(id);
        qmsFaiSheetStatResultMapper.deleteByFaiId(id);
        qmsFaiSampleMapper.deleteByFaiId(id);
        qmsFaiAbnormalMapper.deleteByFaiId(id);
        qmsFaiReturnRecordMapper.deleteByFaiId(id);
        qmsFaiItemAuditHistoryMapper.deleteByFaiId(id);
        qmsFaiItemGroupAuditMapper.deleteByFaiId(id);
        qmsFaiItemMapper.deleteByFaiId(id);
        QmsFaiOrderDO coaDeleted = qmsFaiOrderMapper.selectById(id);
        qmsFaiOrderMapper.deleteById(id);
        if (coaDeleted != null) coaFreezePackagingService.syncCoaFreezeForSegment(coaDeleted.getProductBatchNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaiList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> validateEditable(validateFaiExists(id)));
        qmsFaiSheetCellValueMapper.deleteByFaiIds(ids);
        qmsFaiSheetStatResultMapper.deleteByFaiIds(ids);
        qmsFaiSampleMapper.deleteByFaiIds(ids);
        qmsFaiAbnormalMapper.deleteByFaiIds(ids);
        qmsFaiReturnRecordMapper.deleteByFaiIds(ids);
        qmsFaiItemAuditHistoryMapper.deleteByFaiIds(ids);
        qmsFaiItemGroupAuditMapper.deleteByFaiIds(ids);
        qmsFaiItemMapper.deleteByFaiIds(ids);
        List<QmsFaiOrderDO> coaDeleted = qmsFaiOrderMapper.selectBatchIds(ids);
        qmsFaiOrderMapper.deleteBatchIds(ids);
        coaDeleted.forEach(row -> coaFreezePackagingService.syncCoaFreezeForSegment(row.getProductBatchNo()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void operatorSubmit(QmsFaiSubmitReqVO submitReqVO) {
        QmsFaiOrderDO entity = validateFaiExists(submitReqVO.getId());
        validateEditable(entity);
        ensureStandardBound(entity);
        if (submitReqVO.getItems() != null && !submitReqVO.getItems().isEmpty()) {
            updateExecutionDetails(entity, submitReqVO.getItems(), submitReqVO.getAbnormals());
        }

        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(entity.getId());
        String finalJudgment = calculateRoleJudgment(items, ROLE_OPERATOR);
        LocalDateTime now = LocalDateTime.now();

        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(entity.getId());
        updateObj.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setOperatorName(resolveLoginUserName());
        updateObj.setOperatorTime(now);
        updateObj.setRemark(StringUtils.hasText(submitReqVO.getRemark()) ? submitReqVO.getRemark() : entity.getRemark());
        updateObj.setReleaseResult(LOCKED);
        updateObj.setReleaseTime(now);
        applyInspectionTime(updateObj, submitReqVO.getInspectionTime(), now);
        applyRetentionConfirmation(updateObj, submitReqVO.getRetentionStatus(), entity);
        if (JUDGMENT_OK.equals(finalJudgment)) {
            updateObj.setStatus(STATUS_COMPLETED);
            updateObj.setJudgment(JUDGMENT_OK);
            updateObj.setReleaseResult(RELEASED);
        } else {
            updateObj.setStatus(STATUS_REWORKING);
            updateObj.setJudgment(JUDGMENT_NG);
        }
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        if (JUDGMENT_NG.equals(finalJudgment)) {
            ensureNgAbnormal(entity.getId(), entity.getFaiNo(), items, ROLE_OPERATOR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void qaSubmit(QmsFaiSubmitReqVO submitReqVO) {
        QmsFaiOrderDO entity = validateFaiExists(submitReqVO.getId());
        validateEditable(entity);
        ensureStandardBound(entity);
        if (!STATUS_WAITING_QA.equals(entity.getStatus())) {
            throw exception(HCFAI_WAIT_QA_REQUIRED);
        }
        if (submitReqVO.getItems() != null && !submitReqVO.getItems().isEmpty()) {
            updateExecutionDetails(entity, submitReqVO.getItems(), submitReqVO.getAbnormals());
        }

        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(entity.getId());
        String finalJudgment = calculateRoleJudgment(items, ROLE_QA);
        LocalDateTime now = LocalDateTime.now();

        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(entity.getId());
        updateObj.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setQaInspectorName(resolveLoginUserName());
        updateObj.setQaTime(now);
        updateObj.setRemark(StringUtils.hasText(submitReqVO.getRemark()) ? submitReqVO.getRemark() : entity.getRemark());
        updateObj.setReleaseTime(now);
        applyInspectionTime(updateObj, submitReqVO.getInspectionTime(), now);
        applyRetentionConfirmation(updateObj, submitReqVO.getRetentionStatus(), entity);
        if (JUDGMENT_OK.equals(finalJudgment)) {
            updateObj.setStatus(STATUS_COMPLETED);
            updateObj.setJudgment(JUDGMENT_OK);
            updateObj.setReleaseResult(RELEASED);
        } else {
            updateObj.setStatus(STATUS_REJECTED);
            updateObj.setJudgment(JUDGMENT_NG);
            updateObj.setReleaseResult(LOCKED);
        }
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        if (JUDGMENT_NG.equals(finalJudgment)) {
            ensureNgAbnormal(entity.getId(), entity.getFaiNo(), items, ROLE_QA);
            qmsAbnormalLockService.syncFromFai(entity.getId());
        }
        qmsSampleAbnormalLockService.syncFromFai(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendFai(Long id) {
        QmsFaiOrderDO entity = validateFaiExists(id);
        validateEditable(entity);
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SUSPENDED);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
    }

    @Override
    public QmsFaiOrderDO getFai(Long id) {
        return validateFaiExists(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO getFaiResp(Long id) {
        QmsFaiOrderDO entity = validateFaiExists(id);
        fillLatestRecheckApply(entity);
        QmsFaiRespVO respVO = BeanUtils.toBean(entity, QmsFaiRespVO.class);
        respVO.setOriginalInspectionNo(respVO.getRejectRootInspectionNo() != null
                ? respVO.getRejectRootInspectionNo() : respVO.getRejectPrevInspectionNo());
        respVO.setSubmissionTime(resolveDisplaySubmissionTime(respVO.getSubmissionTime(), respVO.getCreateTime()));
        fillDetails(respVO);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO getFaiRetentionResp(Long id) {
        QmsFaiRespVO respVO = getFaiResp(id);
        if (!RETENTION_RETAINED.equals(respVO.getRetentionStatus())
                || SOURCE_MODULE_GLUE_BOARD_FAI.equals(respVO.getSourceModule())) {
            throw exception(HCFAI_NOT_EXISTS);
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO updateFaiRetentionExpireTime(QmsFaiRetentionExpireTimeReqVO reqVO) {
        QmsFaiOrderDO order = validateFaiRetentionRecord(reqVO.getId());
        if (RETENTION_DESTROYED.equals(order.getRetentionDestroyStatus())) {
            throw exception0(HCFAI_NOT_EXISTS.getCode(), "已销毁留样不允许修改过期时间");
        }
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        updateObj.setRetentionExpireTime(reqVO.getRetentionExpireTime());
        updateObj.setRetentionDestroyStatus(defaultIfBlank(order.getRetentionDestroyStatus(), RETENTION_DESTROY_WAIT));
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        return getFaiRetentionResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void destroyFaiRetention(QmsFaiRetentionDestroyReqVO reqVO) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = reqVO.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw exception0(HCFAI_NOT_EXISTS.getCode(), "请选择要销毁的留样记录");
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = resolveLoginUserName();
        for (Long id : ids) {
            QmsFaiOrderDO order = validateFaiRetentionRecord(id);
            if (RETENTION_DESTROYED.equals(order.getRetentionDestroyStatus())) {
                throw exception0(HCFAI_NOT_EXISTS.getCode(), "留样记录【{}】已销毁", order.getFaiNo());
            }
            if (order.getRetentionExpireTime() == null || order.getRetentionExpireTime().isAfter(now)) {
                throw exception0(HCFAI_NOT_EXISTS.getCode(), "留样记录【{}】尚未过期，不能销毁", order.getFaiNo());
            }
            QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
            updateObj.setId(order.getId());
            updateObj.setRetentionDestroyStatus(RETENTION_DESTROYED);
            updateObj.setRetentionDestroyTime(now);
            updateObj.setRetentionDestroyUserId(userId);
            updateObj.setRetentionDestroyUserName(userName);
            updateObj.setRetentionDestroyRemark(defaultIfBlank(reqVO.getRemark(), ""));
            qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        }
    }

    @Override
    public PageResult<QmsFaiOrderDO> getFaiPage(QmsFaiPageReqVO pageReqVO) {
        PageResult<QmsFaiOrderDO> pageResult = qmsFaiOrderMapper.selectPage(pageReqVO);
        if (pageResult.getList() != null) {
            pageResult.getList().forEach(this::normalizeDisplaySubmissionTime);
            fillLatestRecheckApply(pageResult.getList());
        }
        return pageResult;
    }

    @Override
    public List<QmsFaiRespVO> getPendingFaiList() {
        List<QmsFaiOrderDO> list = qmsFaiOrderMapper.selectListByStatuses(
                List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_WAITING_QA, STATUS_SUSPENDED, STATUS_REWORKING),
                null, SOURCE_MODULE_GLUE_BOARD_FAI);
        list.forEach(this::normalizeDisplaySubmissionTime);
        return BeanUtils.toBean(list, QmsFaiRespVO.class);
    }

    @Override
    public List<QmsFaiRespVO> getPendingFaiListBySourceModule(String sourceModule) {
        List<QmsFaiOrderDO> list = qmsFaiOrderMapper.selectListByStatuses(
                List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_WAITING_QA, STATUS_SUSPENDED, STATUS_REWORKING),
                sourceModule, null);
        list.forEach(this::normalizeDisplaySubmissionTime);
        return BeanUtils.toBean(list, QmsFaiRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiScanRespVO resolveScan(QmsFaiScanReqVO scanReqVO) {
        String scanCode = scanReqVO.getScanCode().trim();
        String scanScene = defaultIfBlank(scanReqVO.getScanScene(), SCAN_SCENE_LEDGER_TOOLBAR);
        String sourceModule = normalizeSourceModule(scanReqVO.getSourceModule());
        QmsFaiScanRespVO respVO = new QmsFaiScanRespVO();
        respVO.setScanCode(scanCode);
        respVO.setScanScene(scanScene);
        respVO.setScanTargetType(SCAN_TARGET_UNKNOWN);
        respVO.setMatchResult(SCAN_RESULT_NOT_FOUND);
        respVO.setCandidateCount(0);
        respVO.setMessage(resolveScanNotFoundMessage(sourceModule));

        if (scanReqVO.getCurrentFaiId() != null) {
            QmsFaiItemDO item = qmsFaiItemMapper.selectByScanCode(scanReqVO.getCurrentFaiId(), scanCode);
            if (item != null) {
                QmsFaiOrderDO order = validateFaiExists(scanReqVO.getCurrentFaiId());
                if (matchesScanSourceModule(order, sourceModule)) {
                    fillSingleScanResp(respVO, order, SCAN_TARGET_INSPECTION_ITEM);
                    respVO.setMatchedFaiItemId(item.getId());
                    respVO.setMatchedStepCode(item.getStepCode());
                    respVO.setOpenTarget(isReadonlyStatus(order.getStatus()) ? OPEN_TARGET_REPORT : OPEN_TARGET_ITEM_MODAL);
                    respVO.setMessage(isReadonlyStatus(order.getStatus()) ? readonlyMessage(order.getStatus()) : "已定位到检验项：" + item.getInspectionItem());
                    saveScanRecord(scanReqVO, respVO);
                    return respVO;
                }
            }
        }

        QmsFaiOrderDO byFaiNo = qmsFaiOrderMapper.selectByFaiNo(scanCode, null, sourceModule);
        if (byFaiNo != null) {
            fillSingleScanResp(respVO, byFaiNo, SCAN_TARGET_FAI_NO);
            saveScanRecord(scanReqVO, respVO);
            return respVO;
        }

        List<QmsFaiOrderDO> candidates = findScanCandidates(scanCode, respVO, sourceModule);
        if (candidates.size() == 1) {
            fillSingleScanResp(respVO, candidates.get(0), respVO.getScanTargetType());
        } else if (candidates.size() > 1) {
            respVO.setMatchResult(SCAN_RESULT_MATCHED_MULTIPLE);
            respVO.setOpenTarget(OPEN_TARGET_CANDIDATE_MODAL);
            respVO.setCandidateCount(candidates.size());
            respVO.setCandidates(BeanUtils.toBean(candidates, QmsFaiScanRespVO.Candidate.class));
            respVO.setMessage(resolveScanMultipleMessage(sourceModule));
        }
        saveScanRecord(scanReqVO, respVO);
        return respVO;
    }

    @Override
    public QmsFaiStandardRespVO getFaiStandard(String materialCode, String operationCode, String operationName) {
        QmsFaiSaveReqVO reqVO = new QmsFaiSaveReqVO();
        reqVO.setMaterialCode(materialCode);
        reqVO.setOperationCode(operationCode);
        reqVO.setOperationName(operationName);
        QmsQualityStandardDO standard = selectFaiStandard(reqVO);
        List<QmsQualityStandardItemDO> standardItems = selectFaiStandardItems(standard.getId(), reqVO);
        QmsFaiStandardRespVO respVO = new QmsFaiStandardRespVO();
        respVO.setStandardId(standard.getId());
        respVO.setStandardNo(standard.getStandardNo());
        respVO.setStandardName(standard.getStandardName());
        respVO.setVersion(standard.getVersion());
        respVO.setApplyType(standard.getApplyType());
        respVO.setMaterialId(standard.getMaterialId());
        respVO.setMaterialCode(standard.getMaterialCode());
        respVO.setMaterialName(standard.getMaterialName());
        respVO.setSpecification(standard.getSpecification());
        respVO.setProductModelId(standard.getProductModelId());
        respVO.setProductModelCode(standard.getProductModelCode());
        respVO.setProductModelName(standard.getProductModelName());
        respVO.setProcessCode(standard.getProcessCode());
        respVO.setProcessName(standard.getProcessName());
        respVO.setItems(standardItems.stream()
                .map(this::buildStandardItem)
                .collect(Collectors.toList()));
        return respVO;
    }

    @Override
    public List<QmsFaiSheetTemplateRespVO> getSheetTemplateList(String productModel) {
        List<QmsFaiSheetTemplateRespVO> fixedTemplates = buildFixedSheetTemplates();
        try {
            List<QmsFaiSheetTemplateDO> templates = qmsFaiSheetTemplateMapper.selectEnableList(productModel);
            if (templates.isEmpty()) {
                return fixedTemplates;
            }
            List<Long> templateIds = templates.stream().map(QmsFaiSheetTemplateDO::getId).collect(Collectors.toList());
            Map<Long, List<QmsFaiSheetSectionDO>> sectionMap = qmsFaiSheetSectionMapper.selectListByTemplateIds(templateIds)
                    .stream()
                    .collect(Collectors.groupingBy(QmsFaiSheetSectionDO::getTemplateId));
            List<QmsFaiSheetTemplateRespVO> dbTemplates = templates.stream()
                    .filter(template -> fixedTemplates.stream()
                            .noneMatch(fixed -> fixed.getTemplateCode().equals(template.getTemplateCode())))
                    .map(template -> buildSheetTemplateResp(template, sectionMap.getOrDefault(template.getId(), Collections.emptyList())))
                    .collect(Collectors.toList());
            fixedTemplates.addAll(dbTemplates);
            return fixedTemplates;
        } catch (Exception ignored) {
            return fixedTemplates;
        }
    }

    @Override
    public QmsFaiSheetTemplateRespVO getSheetTemplate(Long id) {
        QmsFaiSheetTemplateRespVO fixedTemplate = findFixedSheetTemplateResp(id);
        if (fixedTemplate != null) {
            return fixedTemplate;
        }
        return buildSheetTemplateResp(validateSheetTemplate(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO selectSheetTemplate(Long id, Long templateId) {
        QmsFaiOrderDO order = validateFaiExists(id);
        validateEditable(order);
        QmsFaiSheetTemplateDO template = validateSheetTemplate(templateId);
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(id);
        applySheetTemplateSnapshot(updateObj, template);
        updateObj.setEntryMode(ENTRY_MODE_MANUAL);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        return getFaiResp(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO saveSheetEntry(QmsFaiSaveReqVO saveReqVO) {
        QmsFaiOrderDO order = validateFaiExists(saveReqVO.getId());
        validateEditable(order);
        ensureStandardBound(order);
        assertFaiProcessSelfCheckReady(order);
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        updateObj.setEntryMode(ENTRY_MODE_MANUAL);
        updateObj.setEntryLayout(ENTRY_LAYOUT_SHEET_GRID);
        updateObj.setLastSaveTime(LocalDateTime.now());
        updateObj.setSheetLocked(Boolean.FALSE);
        markInspector(updateObj);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        updateExecutionDetails(validateFaiExists(order.getId()), saveReqVO.getItems(), saveReqVO.getAbnormals());
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO saveProgramEntry(QmsFaiSaveReqVO saveReqVO) {
        QmsFaiOrderDO order = validateFaiExists(saveReqVO.getId());
        validateEditable(order);
        assertFaiProcessSelfCheckReady(order);
        updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        markIncomingItemInspectors(order.getId(), saveReqVO.getItems());
        refreshProgramProgress(order.getId(), saveReqVO.getCurrentStepCode(), false, false, saveReqVO.getLastReturnReason());
        markInspector(order.getId());
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO recalculateProgramEntry(QmsFaiSaveReqVO saveReqVO) {
        QmsFaiOrderDO order = validateFaiExists(saveReqVO.getId());
        validateEditable(order);
        assertFaiProcessSelfCheckReady(order);
        updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        refreshProgramProgress(order.getId(), saveReqVO.getCurrentStepCode(), true, false, saveReqVO.getLastReturnReason());
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO auditProgramEntryItem(QmsFaiSaveReqVO saveReqVO) {
        QmsFaiOrderDO order = validateFaiExists(saveReqVO.getId());
        validateEditable(order);
        assertFaiProcessSelfCheckReady(order);
        validateItemAudit(saveReqVO.getItems());
        updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        refreshProgramProgress(order.getId(), saveReqVO.getCurrentStepCode(), true, false, saveReqVO.getLastReturnReason());
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO submitProgramEntry(QmsFaiSaveReqVO saveReqVO) {
        QmsFaiOrderDO order = validateFaiExists(saveReqVO.getId());
        validateEditable(order);
        assertFaiProcessSelfCheckReady(order);
        updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(order.getId());
        validateAllItemsCompleted(items);
        refreshProgramProgress(order.getId(), defaultIfBlank(saveReqVO.getCurrentStepCode(), "CONFIRM"), true, true,
                saveReqVO.getLastReturnReason());
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        markInspector(updateObj);
        LocalDateTime submitTime = LocalDateTime.now();
        updateObj.setReleaseTime(submitTime);
        updateObj.setStatus(STATUS_WAITING_QA);
        updateObj.setJudgment(JUDGMENT_PENDING);
        updateObj.setReleaseResult(LOCKED);
        updateObj.setSheetLocked(Boolean.TRUE);
        applyInspectionTime(updateObj, saveReqVO.getInspectionTime(), submitTime);
        applyRetentionConfirmation(updateObj, saveReqVO.getRetentionStatus(), order);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        boolean firstAuditNotify = order.getAuditNotifyTime() == null;
        qmsAuditTodoNotifyService.sendAuditTodoIfNeeded(order.getAuditNotifyTime(),
                resolveFaiAuditBizType(order),
                order.getFaiNo(),
                buildFaiAuditBizName(order),
                resolveFaiAuditTip(order),
                submitTime,
                time -> qmsFaiOrderMapper.updateAuditNotifyTime(order.getId(), time));
        if (firstAuditNotify) {
            qmsFaiOaNotifyService.sendWaitingAudit(order);
        }
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO auditProgramEntry(QmsFaiAuditReqVO auditReqVO) {
        QmsFaiOrderDO order = validateFaiExists(auditReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus())) {
            throw exception(HCFAI_WAIT_QA_REQUIRED);
        }
        validateEditable(order);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(order.getId());
        validateAllItemsCompleted(items);

        String auditResult = StringUtils.hasText(auditReqVO.getAuditResult())
                ? auditReqVO.getAuditResult().trim()
                : "";
        if (!AUDIT_PASS.equals(auditResult) && !AUDIT_REJECT.equals(auditResult) && !AUDIT_FAIL.equals(auditResult)) {
            throw exception(HCFAI_AUDIT_RESULT_INVALID);
        }
        String rejectReason = auditReqVO.getRejectReason();
        if (AUDIT_REJECT.equals(auditResult) && !StringUtils.hasText(rejectReason)) {
            throw exception(HCFAI_RETURN_REASON_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        applyGroupAuditsIfPresent(order, items, auditReqVO, auditResult, now);
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        markQaInspector(updateObj, now);
        updateObj.setReleaseTime(now);
        if (StringUtils.hasText(rejectReason)) {
            updateObj.setAuditRemark(rejectReason.trim());
        }
        if (AUDIT_PASS.equals(auditResult)) {
            updateObj.setStatus(STATUS_COMPLETED);
            updateObj.setJudgment(JUDGMENT_OK);
            updateObj.setReleaseResult(RELEASED);
            updateObj.setSheetLocked(Boolean.TRUE);
        } else if (AUDIT_REJECT.equals(auditResult)) {
            String trimmedRejectReason = rejectReason.trim();
            updateObj.setStatus(STATUS_INSPECTING);
            updateObj.setJudgment(JUDGMENT_PENDING);
            updateObj.setReleaseResult(LOCKED);
            updateObj.setSheetLocked(Boolean.FALSE);
            updateObj.setLastReturnReason(trimmedRejectReason);
            updateObj.setReturnCount((order.getReturnCount() == null ? 0 : order.getReturnCount()) + 1);
            qmsFaiReturnRecordMapper.insert(QmsFaiReturnRecordDO.builder()
                    .faiId(order.getId())
                    .returnStepCode(order.getCurrentStepCode())
                    .returnReason(defaultIfBlank(trimmedRejectReason, "品质审核驳回"))
                    .returnUserId(SecurityFrameworkUtils.getLoginUserId())
                    .returnUserName(resolveLoginUserName())
                    .returnTime(now)
                    .beforeStatus(order.getStatus())
                    .afterStatus(STATUS_INSPECTING)
                    .build());
        } else {
            String failReason = StringUtils.hasText(rejectReason) ? rejectReason.trim() : "品质审核判定不合格";
            updateObj.setStatus(STATUS_REJECTED);
            updateObj.setJudgment(JUDGMENT_NG);
            updateObj.setReleaseResult(LOCKED);
            updateObj.setSheetLocked(Boolean.TRUE);
            updateObj.setLastReturnReason(failReason);
            updateObj.setAbnormalItemCount(ensureAuditFailAbnormal(order, items, failReason));
        }
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        qmsFaiOrderMapper.clearAuditNotifyTime(order.getId());
        if (JUDGMENT_NG.equals(updateObj.getJudgment())) {
            qmsAbnormalLockService.syncFromFai(order.getId());
        }
        qmsSampleAbnormalLockService.syncFromFai(order.getId());
        if (AUDIT_PASS.equals(auditResult)) {
            qmsFaiOaNotifyService.sendAuditPassed(order);
        } else if (AUDIT_REJECT.equals(auditResult)) {
            qmsFaiOaNotifyService.sendAuditReturned(order);
        } else {
            qmsFaiOaNotifyService.sendAuditFailed(order);
        }
        return getFaiResp(order.getId());
    }

    private void applyGroupAuditsIfPresent(QmsFaiOrderDO order, List<QmsFaiItemDO> items,
                                           QmsFaiAuditReqVO auditReqVO, String finalAuditResult,
                                           LocalDateTime auditTime) {
        List<QmsFaiAuditReqVO.FaiGroupAudit> auditGroups = auditReqVO.getGroups();
        if (auditGroups == null || auditGroups.isEmpty()) {
            return;
        }
        validateItemsExist(items);
        Map<Long, QmsFaiItemDO> itemMap = items.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QmsFaiItemDO::getId, Function.identity(), (left, right) -> left));
        List<Long> itemIds = itemMap.keySet().stream().toList();
        Map<String, SampleAuditGroup> auditableGroupMap = buildSampleAuditGroupMap(itemMap,
                qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(order.getId(), itemIds, ROLE_QA));

        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        Map<String, QmsFaiItemGroupAuditDO> existingGroupAuditMap = selectGroupAudits(order.getId()).stream()
                .collect(Collectors.toMap(this::auditGroupCompositeKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        boolean hasRejectedGroup = false;
        for (QmsFaiAuditReqVO.FaiGroupAudit auditGroup : auditGroups) {
            SampleAuditGroup group = resolveSubmittedAuditGroup(itemMap, auditableGroupMap, auditGroup);
            String groupAuditResult = normalizeGroupAuditResult(auditGroup.getAuditResult());
            String auditRemark = StringUtils.hasText(auditGroup.getAuditRemark())
                    ? auditGroup.getAuditRemark().trim()
                    : null;
            if (ITEM_AUDIT_REJECT_RECHECK.equals(groupAuditResult)) {
                hasRejectedGroup = true;
                if (!StringUtils.hasText(auditRemark)) {
                    throw exception(HCFAI_RETURN_REASON_REQUIRED);
                }
            }
        }
        if (hasRejectedGroup && !AUDIT_REJECT.equals(finalAuditResult)) {
            throw exception(HCFAI_AUDIT_RESULT_INVALID);
        }

        for (QmsFaiAuditReqVO.FaiGroupAudit auditGroup : auditGroups) {
            SampleAuditGroup group = resolveSubmittedAuditGroup(itemMap, auditableGroupMap, auditGroup);
            QmsFaiItemDO item = itemMap.get(group.itemId);
            String groupAuditResult = normalizeGroupAuditResult(auditGroup.getAuditResult());
            String auditRemark = StringUtils.hasText(auditGroup.getAuditRemark())
                    ? auditGroup.getAuditRemark().trim()
                    : null;
            QmsFaiItemGroupAuditDO existingAudit = existingGroupAuditMap.get(auditGroupCompositeKey(group));
            if (ITEM_AUDIT_REJECT_RECHECK.equals(groupAuditResult)) {
                saveRejectedGroupHistory(order, item, group, auditRemark, loginUserId, loginUserName, auditTime);
                resetRejectedGroupForRecheck(order, item, group, existingAudit,
                        auditRemark, loginUserId, loginUserName, auditTime);
            } else {
                confirmAuditGroup(order, item, group, existingAudit, auditRemark,
                        loginUserId, loginUserName, auditTime);
            }
        }
    }

    private SampleAuditGroup resolveSubmittedAuditGroup(Map<Long, QmsFaiItemDO> itemMap,
                                                       Map<String, SampleAuditGroup> auditableGroupMap,
                                                       QmsFaiAuditReqVO.FaiGroupAudit auditGroup) {
        if (auditGroup.getItemId() == null || !itemMap.containsKey(auditGroup.getItemId())) {
            throw exception(HCFAI_ITEM_SOURCE_INVALID);
        }
        String groupKey = normalizeAuditGroupKey(auditGroup.getGroupKey());
        String samplePositionKey = normalizeAuditGroupKey(auditGroup.getSamplePosition());
        SampleAuditGroup group = auditableGroupMap.get(auditGroupCompositeKey(auditGroup.getItemId(), groupKey));
        if (group == null && StringUtils.hasText(samplePositionKey)) {
            group = auditableGroupMap.get(auditGroupCompositeKey(auditGroup.getItemId(), samplePositionKey));
        }
        if (group != null) {
            return group;
        }
        String fallbackKey = StringUtils.hasText(groupKey) ? groupKey : samplePositionKey;
        if (!StringUtils.hasText(fallbackKey)) {
            throw exception(HCFAI_ITEM_AUDIT_NOT_COMPLETED);
        }
        String fallbackPosition = StringUtils.hasText(auditGroup.getSamplePosition())
                ? auditGroup.getSamplePosition().trim()
                : auditGroup.getGroupKey().trim();
        return new SampleAuditGroup(auditGroup.getItemId(), fallbackKey, fallbackPosition);
    }

    private String normalizeGroupAuditResult(String auditResult) {
        String normalized = StringUtils.hasText(auditResult)
                ? auditResult.trim().toUpperCase(Locale.ROOT)
                : "";
        if (ITEM_AUDIT_CONFIRM.equals(normalized)) {
            return ITEM_AUDIT_CONFIRM;
        }
        if (ITEM_AUDIT_REJECT_RECHECK.equals(normalized) || AUDIT_REJECT.equals(normalized)) {
            return ITEM_AUDIT_REJECT_RECHECK;
        }
        throw exception(HCFAI_AUDIT_RESULT_INVALID);
    }

    private void confirmAuditGroup(QmsFaiOrderDO order, QmsFaiItemDO item, SampleAuditGroup group,
                                   QmsFaiItemGroupAuditDO existingAudit, String auditRemark,
                                   Long loginUserId, String loginUserName, LocalDateTime auditTime) {
        String recheckStatus = existingAudit != null
                && (ITEM_RECHECK_WAIT_AUDIT.equals(existingAudit.getItemRecheckStatus())
                || ITEM_RECHECK_WAIT_RECHECK.equals(existingAudit.getItemRecheckStatus())
                || Boolean.TRUE.equals(existingAudit.getRecheckItemFlag()))
                ? ITEM_RECHECK_CONFIRMED
                : ITEM_RECHECK_NONE;
        upsertGroupAudit(order, item, group, existingAudit, ITEM_AUDIT_CONFIRM, auditRemark,
                loginUserId, loginUserName, auditTime, recheckStatus,
                Boolean.TRUE.equals(existingAudit == null ? null : existingAudit.getRecheckItemFlag()));
    }

    private void saveRejectedGroupHistory(QmsFaiOrderDO order, QmsFaiItemDO item, SampleAuditGroup group,
                                          String auditRemark, Long loginUserId, String loginUserName,
                                          LocalDateTime auditTime) {
        qmsFaiItemAuditHistoryMapper.insert(QmsFaiItemAuditHistoryDO.builder()
                .faiId(order.getId())
                .faiNo(order.getFaiNo())
                .faiItemId(item.getId())
                .inspectionItem(item.getInspectionItem())
                .groupKey(group.groupKey)
                .samplePosition(group.samplePosition)
                .auditResult(ITEM_AUDIT_REJECT_RECHECK)
                .auditRemark(auditRemark)
                .auditUserId(loginUserId)
                .auditUserName(loginUserName)
                .auditTime(auditTime)
                .itemRecheckStatus(ITEM_RECHECK_WAIT_RECHECK)
                .snapshotJson(buildRejectedGroupSnapshotJson(order, item, group))
                .tenantId(order.getTenantId())
                .build());
    }

    private String buildRejectedGroupSnapshotJson(QmsFaiOrderDO order, QmsFaiItemDO item, SampleAuditGroup group) {
        List<Long> itemIds = List.of(item.getId());
        List<QmsFaiSampleDO> samples = group.samples;
        Set<String> displayLabels = groupDisplayLabelKeys(group);
        List<QmsFaiSheetCellValueDO> cellValues = qmsFaiSheetCellValueMapper.selectListByFaiIdAndItemIds(order.getId(), itemIds)
                .stream()
                .filter(cell -> displayLabels.contains(normalizeAuditGroupKey(cell.getDisplayLabel())))
                .collect(Collectors.toList());
        List<QmsFaiSheetStatResultDO> statResults = qmsFaiSheetStatResultMapper.selectListByFaiIdAndItemIds(order.getId(), itemIds);
        QmsFaiRespVO.FaiItem itemSnapshot = BeanUtils.toBean(item, QmsFaiRespVO.FaiItem.class);
        itemSnapshot.setSamples(BeanUtils.toBean(samples, QmsFaiRespVO.FaiSample.class));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<String, Object> orderSnapshot = new LinkedHashMap<>();
        orderSnapshot.put("id", order.getId());
        orderSnapshot.put("faiNo", order.getFaiNo());
        orderSnapshot.put("status", order.getStatus());
        orderSnapshot.put("judgment", order.getJudgment());
        orderSnapshot.put("qaTime", formatDateTime(order.getQaTime()));
        snapshot.put("order", orderSnapshot);
        Map<String, Object> groupSnapshot = new LinkedHashMap<>();
        groupSnapshot.put("groupKey", group.groupKey);
        groupSnapshot.put("samplePosition", group.samplePosition);
        snapshot.put("group", groupSnapshot);
        snapshot.put("item", itemSnapshot);
        snapshot.put("sheetCellValues", cellValues.stream()
                .map(this::buildSheetCellSnapshot)
                .collect(Collectors.toList()));
        snapshot.put("sheetStatResults", statResults.stream()
                .map(this::buildSheetStatSnapshot)
                .collect(Collectors.toList()));
        return JsonUtils.toJsonString(snapshot);
    }

    private Map<String, Object> buildSheetCellSnapshot(QmsFaiSheetCellValueDO cell) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", cell.getId());
        snapshot.put("faiItemId", cell.getFaiItemId());
        snapshot.put("sectionCode", cell.getSectionCode());
        snapshot.put("sectionName", cell.getSectionName());
        snapshot.put("metricCode", cell.getMetricCode());
        snapshot.put("metricName", cell.getMetricName());
        snapshot.put("fieldCode", cell.getFieldCode());
        snapshot.put("fieldName", cell.getFieldName());
        snapshot.put("rowNo", cell.getRowNo());
        snapshot.put("columnNo", cell.getColumnNo());
        snapshot.put("sampleNo", cell.getSampleNo());
        snapshot.put("rawValue", cell.getRawValue());
        snapshot.put("numericValue", cell.getNumericValue());
        snapshot.put("textValue", cell.getTextValue());
        snapshot.put("judgmentResult", cell.getJudgmentResult());
        snapshot.put("inputUserId", cell.getInputUserId());
        snapshot.put("inputUserName", cell.getInputUserName());
        snapshot.put("inputTime", formatDateTime(cell.getInputTime()));
        return snapshot;
    }

    private Map<String, Object> buildSheetStatSnapshot(QmsFaiSheetStatResultDO stat) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", stat.getId());
        snapshot.put("faiItemId", stat.getFaiItemId());
        snapshot.put("sectionCode", stat.getSectionCode());
        snapshot.put("metricCode", stat.getMetricCode());
        snapshot.put("statFieldCode", stat.getStatFieldCode());
        snapshot.put("sampleCount", stat.getSampleCount());
        snapshot.put("avgValue", stat.getAvgValue());
        snapshot.put("stdValue", stat.getStdValue());
        snapshot.put("minValue", stat.getMinValue());
        snapshot.put("maxValue", stat.getMaxValue());
        snapshot.put("judgmentResult", stat.getJudgmentResult());
        snapshot.put("calculateTime", formatDateTime(stat.getCalculateTime()));
        return snapshot;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FORMATTER);
    }

    private void resetRejectedGroupForRecheck(QmsFaiOrderDO order, QmsFaiItemDO item, SampleAuditGroup group,
                                              QmsFaiItemGroupAuditDO existingAudit, String auditRemark,
                                              Long loginUserId, String loginUserName, LocalDateTime auditTime) {
        List<Long> sampleIds = group.samples.stream()
                .map(QmsFaiSampleDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!sampleIds.isEmpty()) {
            qmsFaiSampleMapper.deleteBatchIds(sampleIds);
        }
        List<String> displayLabels = group.samples.stream()
                .map(QmsFaiSampleDO::getSamplePosition)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (!displayLabels.isEmpty()) {
            qmsFaiSheetCellValueMapper.deleteByFaiIdAndItemIdAndDisplayLabels(order.getId(), item.getId(), displayLabels);
        }
        List<Long> itemIds = List.of(item.getId());
        qmsFaiSheetStatResultMapper.deleteByFaiIdAndItemIds(order.getId(), itemIds);
        upsertGroupAudit(order, item, group, existingAudit, ITEM_AUDIT_REJECT_RECHECK, auditRemark,
                loginUserId, loginUserName, auditTime, ITEM_RECHECK_WAIT_RECHECK, Boolean.TRUE);
        refreshItemQaStatsAfterGroupReset(order, item);
    }

    private void upsertGroupAudit(QmsFaiOrderDO order, QmsFaiItemDO item, SampleAuditGroup group,
                                  QmsFaiItemGroupAuditDO existingAudit, String auditResult, String auditRemark,
                                  Long loginUserId, String loginUserName, LocalDateTime auditTime,
                                  String recheckStatus, Boolean recheckFlag) {
        QmsFaiItemGroupAuditDO audit = QmsFaiItemGroupAuditDO.builder()
                .id(existingAudit == null ? null : existingAudit.getId())
                .faiId(order.getId())
                .faiNo(order.getFaiNo())
                .faiItemId(item.getId())
                .inspectionItem(item.getInspectionItem())
                .groupKey(group.groupKey)
                .samplePosition(group.samplePosition)
                .auditResult(auditResult)
                .auditRemark(auditRemark)
                .auditUserId(loginUserId)
                .auditUserName(loginUserName)
                .auditTime(auditTime)
                .itemRecheckStatus(recheckStatus)
                .recheckItemFlag(Boolean.TRUE.equals(recheckFlag))
                .tenantId(order.getTenantId())
                .build();
        if (audit.getId() == null) {
            qmsFaiItemGroupAuditMapper.insert(audit);
        } else {
            qmsFaiItemGroupAuditMapper.updateById(audit);
        }
    }

    private void refreshItemQaStatsAfterGroupReset(QmsFaiOrderDO order, QmsFaiItemDO item) {
        item.setQaMax(null);
        item.setQaMin(null);
        item.setQaAvg(null);
        item.setQaResult(JUDGMENT_PENDING);
        item.setQaInspectorId(null);
        item.setQaInspectorName(null);
        item.setQaTime(null);
        item.setCalculatedAvg(null);
        item.setCalculatedStd(null);
        item.setCalculatedMin(null);
        item.setCalculatedMax(null);
        item.setCellCompletedCount(0);
        item.setCompletedSampleCount(0);
        item.setAbnormalSampleCount(0);
        item.setInputStatus(INPUT_STATUS_EMPTY);
        item.setRecheckItemFlag(Boolean.TRUE);
        List<QmsFaiSampleDO> remainingSamples = qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(
                order.getId(), List.of(item.getId()), ROLE_QA);
        qmsFaiSampleResultWritebackService.applyRoleStats(item,
                BeanUtils.toBean(remainingSamples == null ? Collections.emptyList() : remainingSamples,
                        QmsFaiSaveReqVO.FaiSample.class), ROLE_QA);
        item.setRecheckItemFlag(Boolean.TRUE);
        qmsFaiItemMapper.updateById(item);
    }

    private Map<String, SampleAuditGroup> buildSampleAuditGroupMap(Map<Long, QmsFaiItemDO> itemMap,
                                                                   List<QmsFaiSampleDO> samples) {
        Map<String, SampleAuditGroup> groupMap = new LinkedHashMap<>();
        if (samples == null || samples.isEmpty()) {
            return groupMap;
        }
        for (QmsFaiSampleDO sample : samples) {
            QmsFaiItemDO item = itemMap.get(sample.getFaiItemId());
            if (item == null) {
                continue;
            }
            String samplePosition = resolveSamplePosition(sample);
            String groupKey = normalizeAuditGroupKey(samplePosition);
            String compositeKey = auditGroupCompositeKey(sample.getFaiItemId(), groupKey);
            SampleAuditGroup group = groupMap.computeIfAbsent(compositeKey,
                    key -> new SampleAuditGroup(sample.getFaiItemId(), groupKey, samplePosition));
            group.samples.add(sample);
        }
        return groupMap;
    }

    private List<QmsFaiItemGroupAuditDO> selectGroupAudits(Long faiId) {
        List<QmsFaiItemGroupAuditDO> groupAudits = qmsFaiItemGroupAuditMapper.selectListByFaiId(faiId);
        return groupAudits == null ? Collections.emptyList() : groupAudits;
    }

    private String resolveSamplePosition(QmsFaiSampleDO sample) {
        if (StringUtils.hasText(sample.getSamplePosition())) {
            return sample.getSamplePosition().trim();
        }
        if (sample.getSampleSeq() != null) {
            return "位置" + sample.getSampleSeq();
        }
        return "位置" + sample.getId();
    }

    private String normalizeAuditGroupKey(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private String auditGroupCompositeKey(QmsFaiItemGroupAuditDO audit) {
        return auditGroupCompositeKey(audit.getFaiItemId(), normalizeAuditGroupKey(audit.getGroupKey()));
    }

    private String auditGroupCompositeKey(SampleAuditGroup group) {
        return auditGroupCompositeKey(group.itemId, group.groupKey);
    }

    private String auditGroupCompositeKey(Long itemId, String groupKey) {
        if (itemId == null || !StringUtils.hasText(groupKey)) {
            return "";
        }
        return itemId + "|" + groupKey;
    }

    private Set<String> groupDisplayLabelKeys(SampleAuditGroup group) {
        Set<String> keys = group.samples.stream()
                .map(QmsFaiSampleDO::getSamplePosition)
                .filter(StringUtils::hasText)
                .map(this::normalizeAuditGroupKey)
                .collect(Collectors.toSet());
        if (StringUtils.hasText(group.samplePosition)) {
            keys.add(normalizeAuditGroupKey(group.samplePosition));
        }
        if (StringUtils.hasText(group.groupKey)) {
            keys.add(normalizeAuditGroupKey(group.groupKey));
        }
        return keys;
    }

    private static class SampleAuditGroup {

        private final Long itemId;
        private final String groupKey;
        private final String samplePosition;
        private final List<QmsFaiSampleDO> samples = new ArrayList<>();

        private SampleAuditGroup(Long itemId, String groupKey, String samplePosition) {
            this.itemId = itemId;
            this.groupKey = groupKey;
            this.samplePosition = samplePosition;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO oneClickPass(Long id) {
        QmsFaiOrderDO order = validateFaiExists(id);
        validateEditable(order);
        ensureStandardBound(order);
        // getFaiResp 会在缺少快照明细时按检验标准初始化，便于调试新建后直接一键合格。
        getFaiResp(order.getId());
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(order.getId());
        validateItemsExist(items);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        LocalDateTime now = LocalDateTime.now();
        for (QmsFaiItemDO item : items) {
            QmsFaiItemDO itemUpdateObj = new QmsFaiItemDO();
            itemUpdateObj.setId(item.getId());
            itemUpdateObj.setOperatorResult(JUDGMENT_OK);
            itemUpdateObj.setOperatorId(loginUserId);
            itemUpdateObj.setOperatorName(loginUserName);
            itemUpdateObj.setOperatorTime(now);
            itemUpdateObj.setQaResult(JUDGMENT_OK);
            itemUpdateObj.setQaInspectorId(loginUserId);
            itemUpdateObj.setQaInspectorName(loginUserName);
            itemUpdateObj.setQaTime(now);
            itemUpdateObj.setCellCompletedCount(item.getCellRequiredCount());
            itemUpdateObj.setCompletedSampleCount(item.getRequiredSampleCount());
            itemUpdateObj.setAbnormalSampleCount(0);
            itemUpdateObj.setInputStatus(INPUT_STATUS_COMPLETE);
            qmsFaiItemMapper.updateById(itemUpdateObj);
        }
        qmsFaiAbnormalMapper.deleteByFaiId(order.getId());
        refreshProgramProgress(order.getId(), "CONFIRM", true, true, null);
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        updateObj.setOperatorId(loginUserId);
        updateObj.setOperatorName(loginUserName);
        updateObj.setOperatorTime(now);
        updateObj.setInspectionTime(now);
        updateObj.setQaInspectorId(loginUserId);
        updateObj.setQaInspectorName(loginUserName);
        updateObj.setQaTime(now);
        updateObj.setReleaseTime(now);
        updateObj.setStatus(STATUS_COMPLETED);
        updateObj.setJudgment(JUDGMENT_OK);
        updateObj.setReleaseResult(RELEASED);
        updateObj.setSheetLocked(Boolean.TRUE);
        updateObj.setCurrentStepCode("CONFIRM");
        updateObj.setEntryMode(ENTRY_MODE_MANUAL);
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setEntryProgress(100);
        updateObj.setRequiredItemCount(items.size());
        updateObj.setCompletedItemCount(items.size());
        updateObj.setAbnormalItemCount(0);
        updateObj.setLastSaveTime(now);
        updateObj.setLastCalculateTime(now);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        qmsFaiOrderMapper.clearAuditNotifyTime(order.getId());
        qmsSampleAbnormalLockService.syncFromFai(order.getId());
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO oneClickFail(Long id) {
        QmsFaiOrderDO order = validateFaiExists(id);
        validateEditable(order);
        ensureStandardBound(order);
        // getFaiResp 会在缺少快照明细时按检验标准初始化，便于调试新建后直接一键不合格。
        getFaiResp(order.getId());
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(order.getId());
        validateItemsExist(items);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        LocalDateTime now = LocalDateTime.now();
        for (QmsFaiItemDO item : items) {
            QmsFaiItemDO itemUpdateObj = new QmsFaiItemDO();
            itemUpdateObj.setId(item.getId());
            itemUpdateObj.setOperatorResult(JUDGMENT_NG);
            itemUpdateObj.setOperatorId(loginUserId);
            itemUpdateObj.setOperatorName(loginUserName);
            itemUpdateObj.setOperatorTime(now);
            itemUpdateObj.setQaResult(JUDGMENT_NG);
            itemUpdateObj.setQaInspectorId(loginUserId);
            itemUpdateObj.setQaInspectorName(loginUserName);
            itemUpdateObj.setQaTime(now);
            itemUpdateObj.setCellCompletedCount(item.getCellRequiredCount());
            itemUpdateObj.setCompletedSampleCount(item.getRequiredSampleCount());
            itemUpdateObj.setAbnormalSampleCount(item.getRequiredSampleCount());
            itemUpdateObj.setInputStatus(INPUT_STATUS_ABNORMAL);
            qmsFaiItemMapper.updateById(itemUpdateObj);
        }
        qmsFaiAbnormalMapper.deleteByFaiId(order.getId());
        List<QmsFaiItemDO> failedItems = qmsFaiItemMapper.selectListByFaiId(order.getId());
        ensureNgAbnormal(order.getId(), order.getFaiNo(), failedItems, ROLE_QA);
        refreshProgramProgress(order.getId(), "CONFIRM", true, true, "一键判定不合格");
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        updateObj.setOperatorId(loginUserId);
        updateObj.setOperatorName(loginUserName);
        updateObj.setOperatorTime(now);
        updateObj.setQaInspectorId(loginUserId);
        updateObj.setQaInspectorName(loginUserName);
        updateObj.setQaTime(now);
        updateObj.setReleaseTime(now);
        updateObj.setStatus(STATUS_REJECTED);
        updateObj.setJudgment(JUDGMENT_NG);
        updateObj.setReleaseResult(LOCKED);
        updateObj.setSheetLocked(Boolean.TRUE);
        updateObj.setCurrentStepCode("CONFIRM");
        updateObj.setEntryMode(ENTRY_MODE_MANUAL);
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setEntryProgress(100);
        updateObj.setRequiredItemCount(items.size());
        updateObj.setCompletedItemCount(items.size());
        updateObj.setAbnormalItemCount(items.size());
        updateObj.setLastSaveTime(now);
        updateObj.setLastCalculateTime(now);
        updateObj.setLastReturnReason("一键判定不合格");
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        qmsFaiOrderMapper.clearAuditNotifyTime(order.getId());
        qmsAbnormalLockService.syncFromFai(order.getId());
        qmsSampleAbnormalLockService.syncFromFai(order.getId());
        return getFaiResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO createRecheckFai(Long id) {
        QmsFaiOrderDO source = validateFaiExists(id);
        validateFaiRecheckSource(source);

        String faiNo = generateFaiNo();
        validateFaiNoUnique(null, faiNo);
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();

        QmsFaiOrderDO target = BeanUtils.toBean(source, QmsFaiOrderDO.class);
        target.clean();
        target.setId(null);
        target.setFaiNo(faiNo);
        target.setTriggerReason(TRIGGER_REASON_REWORK_RECHECK);
        target.setStatus(STATUS_PENDING);
        target.setJudgment(JUDGMENT_PENDING);
        target.setReleaseResult(LOCKED);
        target.setRecheckFlag(Boolean.TRUE);
        target.setRecheckRoundNo((source.getRecheckRoundNo() == null ? 0 : source.getRecheckRoundNo()) + 1);
        target.setRejectPrevInspectionId(source.getId());
        target.setRejectPrevInspectionNo(source.getFaiNo());
        target.setRejectRootInspectionId(source.getRejectRootInspectionId() == null
                ? source.getId() : source.getRejectRootInspectionId());
        target.setRejectRootInspectionNo(defaultIfBlank(source.getRejectRootInspectionNo(), source.getFaiNo()));

        target.setOperatorId(null);
        target.setOperatorName(null);
        target.setOperatorTime(null);
        target.setQaInspectorId(null);
        target.setQaInspectorName(null);
        target.setQaTime(null);
        target.setInspectionTime(null);
        target.setReleaseTime(null);
        target.setRetentionStatus(RETENTION_UNCONFIRMED);
        target.setRetentionConfirmTime(null);
        target.setRetentionConfirmUserId(null);
        target.setRetentionConfirmUserName(null);
        target.setRetentionPeriodValue(null);
        target.setRetentionPeriodUnit(null);
        target.setRetentionExpireTime(null);
        target.setRetentionDestroyStatus(null);
        target.setRetentionDestroyTime(null);
        target.setRetentionDestroyUserId(null);
        target.setRetentionDestroyUserName(null);
        target.setRetentionDestroyRemark(null);
        target.setAuditNotifyTime(null);
        target.setAuditRemark(null);
        target.setSummaryInspectionId(null);
        target.setEntryMode(ENTRY_MODE_MANUAL);
        target.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        target.setCurrentStepCode(null);
        target.setEntryProgress(0);
        target.setRequiredItemCount(0);
        target.setCompletedItemCount(0);
        target.setAbnormalItemCount(0);
        target.setLastSaveTime(null);
        target.setLastCalculateTime(null);
        target.setLastImportBatchNo(null);
        target.setHistoricalBackfill(Boolean.FALSE);
        target.setSheetLocked(Boolean.FALSE);
        target.setReturnCount(0);
        target.setLastReturnReason(null);
        target.setLastScanCode(null);
        target.setLastScanTargetType(null);
        target.setLastScanScene(null);
        target.setLastScanTime(null);
        target.setLastScanUserId(null);
        target.setLastScanUserName(null);
        target.setRejectFlag(null);
        target.setRecheckGroupId(null);
        target.setRejectNextInspectionId(null);
        target.setRejectNextInspectionNo(null);
        target.setRejectRecheckResult(null);
        target.setRejectRecheckTime(null);
        target.setRejectReason(source.getLastReturnReason());
        target.setRejectTime(null);
        target.setRejectUserId(null);
        target.setRejectUserName(null);
        target.setSubmissionTime(now);
        target.setSubmitterId(loginUserId);
        target.setSubmitterName(loginUserName);
        target.setRemark(buildRecheckRemark(source));

        qmsFaiOrderMapper.insert(target);
        coaFreezePackagingService.syncCoaFreezeForFai(target.getId());
        qmsFaiOaNotifyService.sendCreated(target);
        return getFaiResp(target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRecheckApplyRespVO applyRecheckFai(QmsFaiRecheckApplyReqVO reqVO) {
        QmsFaiOrderDO source = validateFaiExists(reqVO.getSourceFaiId());
        validateFaiRecheckSource(source);
        QmsFaiRecheckApplyDO latestApply = qmsFaiRecheckApplyMapper.selectLatestBySourceFaiId(source.getId());
        if (latestApply != null && RECHECK_APPLY_STATUS_PENDING_AUDIT.equals(latestApply.getStatus())) {
            throw exception(HCFAI_RECHECK_APPLY_PENDING);
        }
        if (latestApply != null && RECHECK_APPLY_STATUS_APPROVED.equals(latestApply.getStatus())
                && latestApply.getGeneratedFaiId() != null) {
            throw exception(HCFAI_RECHECK_APPLY_GENERATED);
        }

        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        QmsFaiRecheckApplyDO apply = QmsFaiRecheckApplyDO.builder()
                .applyNo(generateRecheckApplyNo(source, now))
                .sourceFaiId(source.getId())
                .sourceFaiNo(source.getFaiNo())
                .sourceInspectionType(resolveSourceInspectionType(source))
                .sourceModule(source.getSourceModule())
                .sourceReportId(source.getSourceReportId())
                .sourceReportNo(source.getSourceReportNo())
                .productBatchNo(source.getProductBatchNo())
                .processCategory(source.getProcessCategory())
                .operationCode(source.getOperationCode())
                .operationName(source.getOperationName())
                .applyReason(defaultIfBlank(reqVO.getApplyReason(), "检验不合格，申请复检"))
                .applyUserId(loginUserId)
                .applyUserName(loginUserName)
                .applyTime(now)
                .status(RECHECK_APPLY_STATUS_PENDING_AUDIT)
                .build();
        qmsFaiRecheckApplyMapper.insert(apply);
        return BeanUtils.toBean(apply, QmsFaiRecheckApplyRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRecheckApplyRespVO auditRecheckFai(QmsFaiRecheckAuditReqVO reqVO) {
        QmsFaiRecheckApplyDO apply = qmsFaiRecheckApplyMapper.selectByIdForUpdate(reqVO.getId());
        if (apply == null) {
            throw exception(HCFAI_RECHECK_APPLY_NOT_EXISTS);
        }
        if (!RECHECK_APPLY_STATUS_PENDING_AUDIT.equals(apply.getStatus())) {
            throw exception(HCFAI_RECHECK_APPLY_STATUS_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        QmsFaiRecheckApplyDO updateObj = new QmsFaiRecheckApplyDO();
        updateObj.setId(apply.getId());
        updateObj.setStatus(Boolean.TRUE.equals(reqVO.getApproved())
                ? RECHECK_APPLY_STATUS_APPROVED : RECHECK_APPLY_STATUS_REJECTED);
        updateObj.setAuditUserId(loginUserId);
        updateObj.setAuditUserName(loginUserName);
        updateObj.setAuditTime(now);
        updateObj.setAuditOpinion(defaultIfBlank(reqVO.getAuditOpinion(), Boolean.TRUE.equals(reqVO.getApproved())
                ? "同意复检" : "不同意复检"));

        if (Boolean.TRUE.equals(reqVO.getApproved())) {
            QmsFaiRespVO generated = createRecheckFai(apply.getSourceFaiId());
            updateObj.setGeneratedFaiId(generated.getId());
            updateObj.setGeneratedFaiNo(generated.getFaiNo());
        }
        qmsFaiRecheckApplyMapper.updateById(updateObj);
        QmsFaiRecheckApplyDO updated = qmsFaiRecheckApplyMapper.selectById(apply.getId());
        return BeanUtils.toBean(updated, QmsFaiRecheckApplyRespVO.class);
    }

    private void validateFaiRecheckSource(QmsFaiOrderDO source) {
        if (source == null
                || Boolean.TRUE.equals(source.getRecheckFlag())
                || SOURCE_MODULE_GLUE_BOARD_FAI.equals(source.getSourceModule())
                || SOURCE_MODULE_PACKAGING_COA.equals(source.getSourceModule())
                || (!JUDGMENT_NG.equals(source.getJudgment()) && !STATUS_REJECTED.equals(source.getStatus()))) {
            throw exception(HCFAI_RECHECK_SOURCE_INVALID);
        }
    }

    private String buildRecheckRemark(QmsFaiOrderDO source) {
        String prefix = "复检申请审核通过后生成，使用原首检同一送样；来源单号：" + source.getFaiNo();
        if (!StringUtils.hasText(source.getLastReturnReason())) {
            return prefix;
        }
        return prefix + "；原不合格原因：" + source.getLastReturnReason().trim();
    }

    private String generateRecheckApplyNo(QmsFaiOrderDO source, LocalDateTime now) {
        return "FAR-" + now.format(RECHECK_APPLY_NO_FORMATTER) + "-" + source.getId();
    }

    private String resolveSourceInspectionType(QmsFaiOrderDO source) {
        if (Boolean.TRUE.equals(source.getRecheckFlag())) {
            return "RECHECK";
        }
        String sourceText = String.join(" ",
                defaultIfBlank(source.getSourceReportNo(), ""),
                defaultIfBlank(source.getTriggerReason(), ""),
                defaultIfBlank(source.getRemark(), ""));
        String upperText = sourceText.toUpperCase(Locale.ROOT);
        if (upperText.contains("PROCESS_CHECK") || sourceText.contains("过程加检") || sourceText.contains("加检")) {
            return "ADDITIONAL";
        }
        return "ORIGINAL";
    }

    private void fillLatestRecheckApply(QmsFaiOrderDO order) {
        if (order == null || order.getId() == null) {
            return;
        }
        fillLatestRecheckApply(List.of(order));
    }

    private void fillLatestRecheckApply(List<QmsFaiOrderDO> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        List<Long> ids = orders.stream()
                .map(QmsFaiOrderDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, QmsFaiRecheckApplyDO> applyMap = new LinkedHashMap<>();
        qmsFaiRecheckApplyMapper.selectLatestListBySourceFaiIds(ids).forEach(apply ->
                applyMap.putIfAbsent(apply.getSourceFaiId(), apply));
        orders.forEach(order -> applyLatestRecheckApply(order, applyMap.get(order.getId())));
    }

    private void applyLatestRecheckApply(QmsFaiOrderDO order, QmsFaiRecheckApplyDO apply) {
        if (order == null || apply == null) {
            return;
        }
        order.setLatestRecheckApplyId(apply.getId());
        order.setLatestRecheckApplyNo(apply.getApplyNo());
        order.setLatestRecheckApplyStatus(apply.getStatus());
        order.setLatestRecheckApplyReason(apply.getApplyReason());
        order.setLatestRecheckAuditOpinion(apply.getAuditOpinion());
        order.setLatestRecheckGeneratedFaiNo(apply.getGeneratedFaiNo());
    }

    private void markInspector(Long faiId) {
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(faiId);
        markInspector(updateObj);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
    }

    private void markInspector(QmsFaiOrderDO updateObj) {
        updateObj.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setOperatorName(resolveLoginUserName());
        updateObj.setOperatorTime(LocalDateTime.now());
    }

    private void markQaInspector(QmsFaiOrderDO updateObj, LocalDateTime now) {
        updateObj.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setQaInspectorName(resolveLoginUserName());
        updateObj.setQaTime(now);
    }

    private void applyInspectionTime(QmsFaiOrderDO updateObj, LocalDateTime inspectionTime, LocalDateTime fallbackTime) {
        updateObj.setInspectionTime(inspectionTime == null ? fallbackTime : inspectionTime);
    }

    private void applyRetentionConfirmation(QmsFaiOrderDO updateObj, String retentionStatus,
                                            QmsFaiOrderDO sourceOrder) {
        String normalized = normalizeRetentionStatus(retentionStatus);
        if (!RETENTION_RETAINED.equals(normalized) && !RETENTION_NOT_RETAINED.equals(normalized)) {
            return;
        }
        boolean alreadyConfirmed = sourceOrder != null
                && normalized.equals(normalizeRetentionStatus(sourceOrder.getRetentionStatus()))
                && sourceOrder.getRetentionConfirmTime() != null;
        LocalDateTime retentionConfirmTime = alreadyConfirmed
                ? sourceOrder.getRetentionConfirmTime() : LocalDateTime.now();
        updateObj.setRetentionStatus(normalized);
        updateObj.setRetentionConfirmTime(retentionConfirmTime);
        updateObj.setRetentionConfirmUserId(alreadyConfirmed
                ? sourceOrder.getRetentionConfirmUserId() : SecurityFrameworkUtils.getLoginUserId());
        updateObj.setRetentionConfirmUserName(alreadyConfirmed
                ? sourceOrder.getRetentionConfirmUserName() : resolveLoginUserName());
        if (RETENTION_RETAINED.equals(normalized)) {
            updateObj.setRetentionDestroyStatus(defaultIfBlank(
                    sourceOrder == null ? null : sourceOrder.getRetentionDestroyStatus(), RETENTION_DESTROY_WAIT));
            applyRetentionPeriodSnapshot(updateObj, sourceOrder, retentionConfirmTime);
        }
    }

    private String normalizeRetentionStatus(String retentionStatus) {
        return StringUtils.hasText(retentionStatus) ? retentionStatus.trim().toUpperCase(Locale.ROOT) : "";
    }

    private void applyRetentionPeriodSnapshot(QmsFaiOrderDO updateObj, QmsFaiOrderDO sourceOrder,
                                              LocalDateTime retentionConfirmTime) {
        HcProductModelDO productModel = resolveRetentionProductModel(updateObj, sourceOrder);
        if (productModel == null || productModel.getRetentionPeriodValue() == null
                || productModel.getRetentionPeriodValue() <= 0) {
            return;
        }
        String periodUnit = normalizeRetentionPeriodUnit(productModel.getRetentionPeriodUnit());
        if (!RETENTION_UNIT_DAY.equals(periodUnit) && !RETENTION_UNIT_MONTH.equals(periodUnit)) {
            return;
        }
        updateObj.setRetentionPeriodValue(productModel.getRetentionPeriodValue());
        updateObj.setRetentionPeriodUnit(periodUnit);
        if ((sourceOrder == null || sourceOrder.getRetentionExpireTime() == null)
                && retentionConfirmTime != null
                && !retentionConfirmTime.isBefore(MIN_VALID_SUBMISSION_TIME)) {
            updateObj.setRetentionExpireTime(calculateRetentionExpireTime(
                    retentionConfirmTime, productModel.getRetentionPeriodValue(), periodUnit));
        }
    }

    private HcProductModelDO resolveRetentionProductModel(QmsFaiOrderDO updateObj, QmsFaiOrderDO sourceOrder) {
        Long productModelId = updateObj.getProductModelId() != null
                ? updateObj.getProductModelId()
                : sourceOrder == null ? null : sourceOrder.getProductModelId();
        if (productModelId != null) {
            HcProductModelDO productModel = hcProductModelService.getHcProductModel(productModelId);
            if (productModel != null) {
                return productModel;
            }
        }
        String productModelCode = defaultIfBlank(updateObj.getProductModel(),
                sourceOrder == null ? null : sourceOrder.getProductModel());
        return hcProductModelService.getHcProductModelByCode(productModelCode);
    }

    private String normalizeRetentionPeriodUnit(String retentionPeriodUnit) {
        if (!StringUtils.hasText(retentionPeriodUnit)) {
            return "";
        }
        String text = retentionPeriodUnit.trim();
        String normalized = text.toUpperCase(Locale.ROOT);
        if ("天".equals(text) || "DAYS".equals(normalized) || RETENTION_UNIT_DAY.equals(normalized)) {
            return RETENTION_UNIT_DAY;
        }
        if ("月".equals(text) || "MONTHS".equals(normalized) || RETENTION_UNIT_MONTH.equals(normalized)) {
            return RETENTION_UNIT_MONTH;
        }
        return normalized;
    }

    private LocalDateTime calculateRetentionExpireTime(LocalDateTime retentionConfirmTime, Integer periodValue,
                                                       String periodUnit) {
        if (RETENTION_UNIT_MONTH.equals(periodUnit)) {
            return retentionConfirmTime.plusMonths(periodValue);
        }
        return retentionConfirmTime.plusDays(periodValue);
    }

    private QmsFaiOrderDO validateFaiRetentionRecord(Long id) {
        QmsFaiOrderDO order = validateFaiExists(id);
        if (!RETENTION_RETAINED.equals(order.getRetentionStatus())
                || SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())) {
            throw exception(HCFAI_NOT_EXISTS);
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiRespVO returnProgramEntry(QmsFaiSaveReqVO saveReqVO) {
        QmsFaiOrderDO order = validateFaiExists(saveReqVO.getId());
        if (!STATUS_WAITING_QA.equals(order.getStatus()) && !STATUS_INSPECTING.equals(order.getStatus())) {
            throw exception(HCFAI_WAIT_QA_REQUIRED);
        }
        if (!StringUtils.hasText(saveReqVO.getLastReturnReason())) {
            throw exception(HCFAI_RETURN_REASON_REQUIRED);
        }
        List<QmsFaiItemDO> returnedItems = resolveReturnItems(order.getId(), saveReqVO.getItems());
        resetReturnedItemAudit(order, returnedItems, saveReqVO.getLastReturnReason());
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(order.getId());
        updateObj.setStatus(STATUS_INSPECTING);
        updateObj.setJudgment(JUDGMENT_PENDING);
        updateObj.setSheetLocked(Boolean.FALSE);
        updateObj.setCurrentStepCode(saveReqVO.getCurrentStepCode());
        updateObj.setLastReturnReason(saveReqVO.getLastReturnReason());
        updateObj.setReturnCount((order.getReturnCount() == null ? 0 : order.getReturnCount()) + 1);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        qmsFaiOrderMapper.clearAuditNotifyTime(order.getId());
        qmsFaiReturnRecordMapper.insert(QmsFaiReturnRecordDO.builder()
                .faiId(order.getId())
                .returnStepCode(saveReqVO.getCurrentStepCode())
                .returnReason(defaultIfBlank(saveReqVO.getLastReturnReason(), "品质退回修改"))
                .returnUserId(SecurityFrameworkUtils.getLoginUserId())
                .returnUserName(resolveLoginUserName())
                .returnTime(LocalDateTime.now())
                .beforeStatus(order.getStatus())
                .afterStatus(STATUS_INSPECTING)
                .build());
        refreshProgramProgress(order.getId(), saveReqVO.getCurrentStepCode(), false, false, saveReqVO.getLastReturnReason());
        return getFaiResp(order.getId());
    }

    private void refreshProgramProgress(Long faiId, String currentStepCode, boolean calculated, boolean lockSheet,
                                        String returnReason) {
        QmsFaiOrderDO order = validateFaiExists(faiId);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(faiId);
        int requiredCount = items.size();
        int completedCount = (int) items.stream()
                .filter(item -> JUDGMENT_OK.equals(item.getQaResult()) || JUDGMENT_NG.equals(item.getQaResult()))
                .count();
        int abnormalCount = (int) items.stream()
                .filter(item -> JUDGMENT_NG.equals(item.getQaResult()))
                .count();
        int progress = requiredCount == 0 ? 0 : BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredCount), 0, RoundingMode.HALF_UP)
                .intValue();
        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(faiId);
        updateObj.setEntryMode(ENTRY_MODE_MANUAL);
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setCurrentStepCode(currentStepCode);
        updateObj.setEntryProgress(progress);
        updateObj.setRequiredItemCount(requiredCount);
        updateObj.setCompletedItemCount(completedCount);
        updateObj.setAbnormalItemCount(abnormalCount);
        updateObj.setLastSaveTime(LocalDateTime.now());
        updateObj.setSheetLocked(lockSheet);
        if (calculated) {
            updateObj.setLastCalculateTime(LocalDateTime.now());
        }
        if (StringUtils.hasText(returnReason)) {
            updateObj.setLastReturnReason(returnReason);
        }
        if (STATUS_PENDING.equals(order.getStatus()) || STATUS_SUSPENDED.equals(order.getStatus())) {
            updateObj.setStatus(STATUS_INSPECTING);
        }
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiImportRespVO importOriginSheet(Long id, Long templateId, String mode, MultipartFile file) throws IOException {
        QmsFaiOrderDO order = validateFaiExists(id);
        validateEditable(order);
        ensureStandardBound(order);
        QmsFaiSheetTemplateDO template = validateSheetTemplate(templateId);
        String importBatchNo = generateImportBatchNo();
        List<String> messages = new ArrayList<>();
        List<QmsFaiSaveReqVO.FaiItem> importedItems = parseOriginSheet(order, file, importBatchNo, messages);
        if (importedItems.isEmpty()) {
            throw exception(HCFAI_SHEET_IMPORT_INVALID);
        }

        QmsFaiSheetImportBatchDO batch = QmsFaiSheetImportBatchDO.builder()
                .faiId(id)
                .importBatchNo(importBatchNo)
                .fileName(file.getOriginalFilename())
                .targetFaiId(id)
                .sheetTemplateId(templateId)
                .templateCode(template.getTemplateCode())
                .templateVersion(template.getTemplateVersion())
                .status(IMPORT_STATUS_SUCCESS)
                .importUsage(IMPORT_USAGE_HISTORICAL_BACKFILL)
                .validateSummary("解析检验项目 " + importedItems.size() + " 项，校验消息 " + messages.size() + " 条")
                .errorSummary(JsonUtils.toJsonString(messages))
                .allowOverwrite("OVERWRITE".equals(mode))
                .reviewStatus(JUDGMENT_PENDING)
                .importerId(SecurityFrameworkUtils.getLoginUserId())
                .importerName(resolveLoginUserName())
                .importTime(LocalDateTime.now())
                .build();
        qmsFaiSheetImportBatchMapper.insert(batch);

        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(id);
        applySheetTemplateSnapshot(updateObj, template);
        updateObj.setEntryMode(ENTRY_MODE_HISTORICAL_IMPORT);
        updateObj.setHistoricalBackfill(Boolean.TRUE);
        updateObj.setLastImportBatchNo(importBatchNo);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        updateExecutionDetails(validateFaiExists(id), importedItems, null);

        QmsFaiImportRespVO respVO = new QmsFaiImportRespVO();
        respVO.setImportBatchNo(importBatchNo);
        respVO.setFileName(file.getOriginalFilename());
        respVO.setStatus(IMPORT_STATUS_SUCCESS);
        respVO.setValidateSummary(JsonUtils.toJsonString(messages));
        respVO.setRecord(getFaiResp(id));
        respVO.setMessages(messages);
        return respVO;
    }

    @Override
    public List<QmsFaiItemImportExcelVO> buildItemImportTemplate(Long id) {
        QmsFaiOrderDO order = validateFaiExists(id);
        validateEditable(order);
        ensureStandardBound(order);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(id);
        validateItemsExist(items);
        String templateVersionHash = qmsFaiItemWorkbookService.buildItemTemplateVersionHash(order, items);
        return qmsFaiItemWorkbookService.buildItemTemplateRows(order, items, templateVersionHash, Collections.emptyMap());
    }

    @Override
    public byte[] buildItemImportTemplateExcel(Long id) throws IOException {
        QmsFaiOrderDO order = validateFaiExists(id);
        validateEditable(order);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(id);
        validateItemsExist(items);
        return qmsFaiItemWorkbookService.buildItemOverviewWorkbook(order, items, Collections.emptyMap());
    }

    @Override
    public List<QmsFaiItemImportExcelVO> buildItemExportRows(Long id) {
        QmsFaiOrderDO order = validateFaiExists(id);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(id);
        validateItemsExist(items);
        List<Long> itemIds = items.stream().map(QmsFaiItemDO::getId).collect(Collectors.toList());
        Map<String, QmsFaiSampleDO> sampleMap = qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(id, itemIds, ROLE_QA)
                .stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        String templateVersionHash = qmsFaiItemWorkbookService.buildItemTemplateVersionHash(order, items);
        return qmsFaiItemWorkbookService.buildItemTemplateRows(order, items, templateVersionHash, sampleMap);
    }

    @Override
    public byte[] buildItemExportExcel(Long id) throws IOException {
        QmsFaiOrderDO order = validateFaiExists(id);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(id);
        validateItemsExist(items);
        List<Long> itemIds = items.stream().map(QmsFaiItemDO::getId).collect(Collectors.toList());
        Map<String, QmsFaiSampleDO> sampleMap = qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(id, itemIds, ROLE_QA)
                .stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        return qmsFaiItemWorkbookService.buildItemOverviewWorkbook(order, items, sampleMap);
    }

    @Override
    public QmsFaiImportRespVO previewItemImport(Long id, MultipartFile file) throws IOException {
        QmsFaiItemWorkbookService.ItemImportPlan plan = buildItemImportPlan(id, file, false, true);
        return qmsFaiItemWorkbookService.buildItemImportResp(plan, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsFaiImportRespVO confirmItemImport(Long id, Boolean allowOverwrite, MultipartFile file) throws IOException {
        QmsFaiItemWorkbookService.ItemImportPlan plan = buildItemImportPlan(id, file,
                Boolean.TRUE.equals(allowOverwrite), false);
        String importBatchNo = generateImportBatchNo();
        boolean applied = applyItemImport(plan, importBatchNo);
        QmsFaiImportRespVO respVO = qmsFaiItemWorkbookService.buildItemImportResp(
                plan, applied ? getFaiResp(plan.order.getId()) : null);
        respVO.setImportBatchNo(importBatchNo);
        return respVO;
    }

    private QmsFaiItemWorkbookService.ItemImportPlan buildItemImportPlan(Long id, MultipartFile file,
                                                                         boolean allowOverwrite,
                                                                         boolean previewOnly) throws IOException {
        QmsFaiOrderDO order = validateFaiExists(id);
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(id);
        validateItemsExist(items);
        List<Long> itemIds = items.stream().map(QmsFaiItemDO::getId).collect(Collectors.toList());
        Map<String, QmsFaiSampleDO> existingSampleMap = qmsFaiSampleMapper
                .selectListByFaiIdAndItemIdsAndRole(id, itemIds, ROLE_QA)
                .stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        boolean importBlocked = Boolean.TRUE.equals(order.getSheetLocked())
                || STATUS_WAITING_QA.equals(order.getStatus())
                || STATUS_COMPLETED.equals(order.getStatus())
                || STATUS_REJECTED.equals(order.getStatus())
                || STATUS_CANCELED.equals(order.getStatus());
        return qmsFaiItemWorkbookService.buildItemImportPlan(
                order, items, existingSampleMap, file, allowOverwrite, previewOnly, importBlocked);
    }

    private boolean applyItemImport(QmsFaiItemWorkbookService.ItemImportPlan plan, String importBatchNo) {
        if (plan.failureCount > 0 || plan.validRows.isEmpty()) {
            saveItemImportBatch(plan, importBatchNo, IMPORT_STATUS_FAILED);
            return false;
        }

        Map<Long, List<QmsFaiItemWorkbookService.ItemImportRow>> rowMap = plan.validRows.stream()
                .collect(Collectors.groupingBy(row -> row.item.getId(), LinkedHashMap::new, Collectors.toList()));
        List<Long> touchedItemIds = new ArrayList<>(rowMap.keySet());
        List<QmsFaiSampleDO> existingSamples = qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(
                plan.order.getId(), touchedItemIds, ROLE_QA);
        Map<String, QmsFaiSampleDO> existingSampleMap = existingSamples.stream()
                .collect(Collectors.toMap(this::sampleKey, Function.identity(), (first, ignored) -> first));
        Map<String, QmsFaiSaveReqVO.FaiSample> allTouchedSampleMap = new LinkedHashMap<>();
        for (QmsFaiSampleDO existingSample : existingSamples) {
            allTouchedSampleMap.put(sampleKey(existingSample),
                    BeanUtils.toBean(existingSample, QmsFaiSaveReqVO.FaiSample.class));
        }
        for (QmsFaiItemWorkbookService.ItemImportRow row : plan.validRows) {
            QmsFaiSaveReqVO.FaiSample sample = qmsFaiItemWorkbookService.buildImportedSample(row, importBatchNo,
                    existingSampleMap.get(sampleKey(row.item.getId(), row.sampleSeq)));
            allTouchedSampleMap.put(sampleKey(row.item.getId(), row.sampleSeq), sample);
        }

        List<QmsFaiSaveReqVO.FaiItem> incomingItems = buildItemImportItems(plan,
                groupSamplesByItem(allTouchedSampleMap));
        updateExecutionDetails(plan.order, incomingItems, null);
        markIncomingItemInspectors(plan.order.getId(), incomingItems);
        refreshProgramProgress(plan.order.getId(), plan.order.getCurrentStepCode(), true, false, null);

        QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
        updateObj.setId(plan.order.getId());
        updateObj.setEntryMode(ENTRY_MODE_EXCEL_IMPORT);
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setSheetLocked(Boolean.FALSE);
        updateObj.setLastImportBatchNo(importBatchNo);
        updateObj.setLastSaveTime(LocalDateTime.now());
        markInspector(updateObj);
        qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        saveItemImportBatch(plan, importBatchNo, IMPORT_STATUS_SUCCESS);
        return true;
    }

    private Map<Long, List<QmsFaiSaveReqVO.FaiSample>> groupSamplesByItem(
            Map<String, QmsFaiSaveReqVO.FaiSample> allTouchedSampleMap) {
        Map<Long, List<QmsFaiSaveReqVO.FaiSample>> sampleMap = new LinkedHashMap<>();
        for (Map.Entry<String, QmsFaiSaveReqVO.FaiSample> entry : allTouchedSampleMap.entrySet()) {
            Long itemId = Long.valueOf(entry.getKey().split("#")[0]);
            sampleMap.computeIfAbsent(itemId, key -> new ArrayList<>()).add(entry.getValue());
        }
        return sampleMap;
    }

    private List<QmsFaiSaveReqVO.FaiItem> buildItemImportItems(QmsFaiItemWorkbookService.ItemImportPlan plan,
                                                              Map<Long, List<QmsFaiSaveReqVO.FaiSample>> sampleMap) {
        List<QmsFaiSaveReqVO.FaiItem> incomingItems = new ArrayList<>();
        for (Map.Entry<Long, List<QmsFaiSaveReqVO.FaiSample>> entry : sampleMap.entrySet()) {
            QmsFaiItemDO item = plan.itemMap.get(entry.getKey());
            if (item == null) {
                continue;
            }
            QmsFaiSaveReqVO.FaiItem itemVO = BeanUtils.toBean(item, QmsFaiSaveReqVO.FaiItem.class);
            itemVO.setSamples(entry.getValue().stream()
                    .sorted(Comparator.comparing(QmsFaiSaveReqVO.FaiSample::getSampleSeq))
                    .collect(Collectors.toList()));
            incomingItems.add(itemVO);
        }
        return incomingItems;
    }

    private void saveItemImportBatch(QmsFaiItemWorkbookService.ItemImportPlan plan, String importBatchNo,
                                     String status) {
        QmsFaiSheetImportBatchDO batch = QmsFaiSheetImportBatchDO.builder()
                .faiId(plan.order.getId())
                .targetFaiId(plan.order.getId())
                .targetFaiNo(plan.order.getFaiNo())
                .importBatchNo(importBatchNo)
                .fileName(plan.fileName)
                .sheetTemplateId(plan.order.getSheetTemplateId())
                .templateCode(plan.order.getSheetTemplateCode())
                .templateVersion(plan.order.getSheetTemplateVersion())
                .templateVersionHash(plan.templateVersionHash)
                .status(status)
                .importUsage(IMPORT_USAGE_ITEM_OVERVIEW_BATCH)
                .validateSummary("成功 " + plan.successCount + " 行，失败 " + plan.failureCount
                        + " 行，警告 " + plan.warningCount + " 条")
                .allowOverwrite(plan.allowOverwrite)
                .reviewStatus(JUDGMENT_PENDING)
                .successCount(plan.successCount)
                .failureCount(plan.failureCount)
                .warningCount(plan.warningCount)
                .errorSummary(JsonUtils.toJsonString(plan.messages))
                .importerId(SecurityFrameworkUtils.getLoginUserId())
                .importerName(resolveLoginUserName())
                .importTime(LocalDateTime.now())
                .build();
        qmsFaiSheetImportBatchMapper.insert(batch);
    }

    private String sampleKey(QmsFaiSampleDO sample) {
        return sampleKey(sample.getFaiItemId(), sample.getSampleSeq());
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    private String resolveInspectionItemCode(QmsFaiItemDO item) {
        return defaultIfBlank(item.getMetricCode(),
                defaultIfBlank(item.getSheetMetricCode(), "FAI_ITEM_" + item.getId()));
    }

    private QmsQualityStandardDO selectFaiStandard(QmsFaiSaveReqVO reqVO) {
        if (reqVO.getStandardId() != null) {
            QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(reqVO.getStandardId());
            validateFaiStandardUsable(standard, reqVO);
            return standard;
        }

        StandardCandidateMatch selectedMatch = selectUniqueHighestPriorityMatch(
                findFaiStandardCandidateMatches(reqVO));
        if (selectedMatch == null) {
            if (isProductModelProcessStandardMatchMode(reqVO)) {
                throwFaiProductModelProcessStandardNotFound();
            }
            if (isMaterialProcessStandardMatchMode(reqVO)) {
                throwFaiMaterialProcessStandardNotFound();
            }
            throw exception(HCFAI_STANDARD_NOT_EXISTS);
        }
        return selectedMatch.standard();
    }

    private List<StandardCandidateMatch> findFaiStandardCandidateMatches(QmsFaiSaveReqVO reqVO) {
        if (isGlueBoardFaiSource(reqVO)) {
            return qmsQualityStandardMapper.selectList(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                            .eq(QmsQualityStandardDO::getApplyType, SOURCE_MODULE_GLUE_BOARD_FAI)
                            .eq(QmsQualityStandardDO::getStatus, ENABLED)
                            .eq(QmsQualityStandardDO::getAuditStatus, AUDITED)
                            .eq(QmsQualityStandardDO::getGlueBoardModel, reqVO.getGlueBoardModel())
                            .orderByDesc(QmsQualityStandardDO::getId))
                    .stream()
                    .filter(this::hasExecutableStandardItems)
                    .map(standard -> new StandardCandidateMatch(standard, MATCH_SCORE_EXACT_MODEL,
                            "GLUE_BOARD_MODEL", null, standard.getGlueBoardModel(),
                            "胶板型号与任务完全一致"))
                    .collect(Collectors.toList());
        }

        List<QmsQualityStandardDO> standards = qmsQualityStandardMapper.selectList(new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getApplyType, APPLY_TYPE_FAI)
                .eq(QmsQualityStandardDO::getStatus, ENABLED)
                .eq(QmsQualityStandardDO::getAuditStatus, AUDITED)
                .orderByDesc(QmsQualityStandardDO::getId));
        ProductModelMatchContext modelContext = resolveProductModelMatchContext(reqVO);
        return standards.stream()
                .filter(this::hasFaiApplyScope)
                .filter(this::hasExecutableStandardItems)
                .map(item -> evaluateStandardMatch(item, reqVO, modelContext))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(StandardCandidateMatch::score)
                        .thenComparing(match -> match.standard().getId())
                        .reversed())
                .collect(Collectors.toList());
    }

    private StandardCandidateMatch selectUniqueHighestPriorityMatch(List<StandardCandidateMatch> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int highestScore = candidates.get(0).score();
        List<StandardCandidateMatch> highestMatches = candidates.stream()
                .filter(candidate -> candidate.score() == highestScore)
                .toList();
        return highestMatches.size() == 1 ? highestMatches.get(0) : null;
    }

    private boolean hasExecutableStandardItems(QmsQualityStandardDO standard) {
        if (standard == null || standard.getId() == null) {
            return false;
        }
        List<QmsQualityStandardItemDO> items =
                qmsQualityStandardItemMapper.selectListByStandardId(standard.getId());
        return items != null && !items.isEmpty();
    }

    private boolean hasFaiApplyScope(QmsQualityStandardDO standard) {
        return standard != null
                && (hasMaterialScope(standard) || hasProductModelScope(standard) || hasProcessScope(standard));
    }

    private boolean hasMaterialScope(QmsQualityStandardDO standard) {
        return standard != null
                && (standard.getMaterialId() != null || StringUtils.hasText(standard.getMaterialCode()));
    }

    private boolean hasProductModelScope(QmsQualityStandardDO standard) {
        return standard != null
                && (StringUtils.hasText(standard.getProductModelName())
                || StringUtils.hasText(standard.getProductModelCode()));
    }

    private boolean hasProcessScope(QmsQualityStandardDO standard) {
        return standard != null
                && (standard.getProcessId() != null
                || StringUtils.hasText(standard.getProcessCode())
                || StringUtils.hasText(standard.getProcessName()));
    }

    private StandardCandidateMatch evaluateStandardMatch(QmsQualityStandardDO standard,
                                                         QmsFaiSaveReqVO reqVO,
                                                         ProductModelMatchContext modelContext) {
        String standardMatchMode = defaultIfBlank(reqVO.getStandardMatchMode(), STANDARD_MATCH_MATERIAL_PROCESS);
        if (STANDARD_MATCH_PROCESS.equalsIgnoreCase(standardMatchMode)) {
            if (hasProcessScope(standard) && matchesProcessScope(standard, reqVO)) {
                return new StandardCandidateMatch(standard, MATCH_SCORE_PROCESS, MATCH_TYPE_PROCESS,
                        null, null, "工序与任务匹配");
            }
            return null;
        }
        if (STANDARD_MATCH_MATERIAL.equalsIgnoreCase(standardMatchMode)) {
            if (hasMaterialScope(standard) && matchesMaterialScope(standard, reqVO)) {
                return new StandardCandidateMatch(standard, MATCH_SCORE_MATERIAL, MATCH_TYPE_MATERIAL,
                        null, null, "物料与任务匹配");
            }
            return null;
        }
        if (STANDARD_MATCH_PRODUCT_MODEL_PROCESS.equalsIgnoreCase(standardMatchMode)) {
            return evaluateProductModelProcessMatch(standard, reqVO, modelContext);
        }
        return evaluateMaterialProcessMatch(standard, reqVO);
    }

    private StandardCandidateMatch evaluateMaterialProcessMatch(QmsQualityStandardDO standard,
                                                                 QmsFaiSaveReqVO reqVO) {
        if (!matchesMaterialAndProcessStrict(standard, reqVO)) {
            return null;
        }
        return new StandardCandidateMatch(standard, MATCH_SCORE_MATERIAL_PROCESS, MATCH_TYPE_MATERIAL_PROCESS,
                null, null, "物料与工序均与任务匹配");
    }

    private StandardCandidateMatch evaluateProductModelProcessMatch(QmsQualityStandardDO standard,
                                                                     QmsFaiSaveReqVO reqVO,
                                                                     ProductModelMatchContext modelContext) {
        if (reqVO == null || !hasProductModelScope(standard) || !hasProcessScope(standard)
                || !matchesProcessScopeStrict(standard, reqVO)) {
            return null;
        }
        String standardModelCode = defaultIfBlank(standard.getProductModelCode(), standard.getProductModelName());
        boolean exactMatched = modelContext != null
                && ((modelContext.exactModelId() != null
                && Objects.equals(modelContext.exactModelId(), standard.getProductModelId()))
                || equalsModelCode(modelContext.exactModelCode(), standard.getProductModelCode())
                || equalsModelCode(modelContext.exactModelCode(), standard.getProductModelName()));
        if (exactMatched) {
            return new StandardCandidateMatch(standard, MATCH_SCORE_EXACT_MODEL, MATCH_TYPE_EXACT_MODEL,
                    modelContext.exactModelId(), modelContext.exactModelCode(),
                    "产品型号与工序完全匹配");
        }
        boolean familyMatched = modelContext != null && modelContext.familyModelId() != null
                && (Objects.equals(modelContext.familyModelId(), standard.getProductModelId())
                || equalsModelCode(modelContext.familyModelCode(), standard.getProductModelCode())
                || equalsModelCode(modelContext.familyModelCode(), standard.getProductModelName()));
        if (familyMatched) {
            return new StandardCandidateMatch(standard, MATCH_SCORE_FAMILY_MODEL, MATCH_TYPE_FAMILY_MODEL,
                    modelContext.familyModelId(), modelContext.familyModelCode(),
                    modelContext.exactModelCode() + " 归属于 " + modelContext.familyModelCode() + " 系列");
        }
        if (modelContext == null && matchesProductModelScopeStrict(standard, reqVO)) {
            return new StandardCandidateMatch(standard, MATCH_SCORE_EXACT_MODEL, MATCH_TYPE_EXACT_MODEL,
                    standard.getProductModelId(), standardModelCode, "产品型号与工序完全匹配");
        }
        return null;
    }

    private ProductModelMatchContext resolveProductModelMatchContext(QmsFaiSaveReqVO reqVO) {
        if (reqVO == null || (reqVO.getProductModelId() == null
                && !StringUtils.hasText(reqVO.getProductModel()))) {
            return null;
        }
        HcProductModelDO exactModel = null;
        if (hcProductModelService != null) {
            exactModel = reqVO.getProductModelId() != null
                    ? hcProductModelService.getHcProductModel(reqVO.getProductModelId())
                    : hcProductModelService.getHcProductModelByCode(reqVO.getProductModel());
        }
        if (exactModel == null) {
            return new ProductModelMatchContext(reqVO.getProductModelId(), reqVO.getProductModel(), null, null);
        }
        reqVO.setProductModelId(exactModel.getId());
        HcProductModelDO familyModel = null;
        if (exactModel.getParentModelId() != null && hcProductModelService != null) {
            familyModel = hcProductModelService.getHcProductModel(exactModel.getParentModelId());
        }
        return new ProductModelMatchContext(exactModel.getId(), exactModel.getModelCode(),
                familyModel == null ? null : familyModel.getId(),
                familyModel == null ? null : familyModel.getModelCode());
    }

    private boolean equalsModelCode(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right)
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean matchesMaterialAndProcessStrict(QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        return reqVO != null
                && hasMaterialScope(standard)
                && hasProcessScope(standard)
                && matchesMaterialScope(standard, reqVO)
                && matchesProcessScopeStrict(standard, reqVO);
    }

    private boolean matchesProductModelScopeStrict(QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        if (reqVO == null || !StringUtils.hasText(reqVO.getProductModel())) {
            return false;
        }
        return (StringUtils.hasText(standard.getProductModelName())
                && standard.getProductModelName().equals(reqVO.getProductModel()))
                || (StringUtils.hasText(standard.getProductModelCode())
                && standard.getProductModelCode().equals(reqVO.getProductModel()));
    }

    private boolean matchesMaterialScope(QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        boolean standardHasMaterial = standard.getMaterialId() != null || StringUtils.hasText(standard.getMaterialCode());
        if (!standardHasMaterial) {
            return true;
        }
        boolean idMatched = standard.getMaterialId() != null
                && reqVO.getMaterialId() != null
                && standard.getMaterialId().equals(reqVO.getMaterialId());
        boolean codeMatched = StringUtils.hasText(standard.getMaterialCode())
                && StringUtils.hasText(reqVO.getMaterialCode())
                && standard.getMaterialCode().equals(reqVO.getMaterialCode());
        return idMatched || codeMatched;
    }

    private boolean matchesProcessScope(QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        boolean standardHasProcess = standard.getProcessId() != null
                || StringUtils.hasText(standard.getProcessCode())
                || StringUtils.hasText(standard.getProcessName());
        if (!standardHasProcess) {
            return true;
        }
        return (StringUtils.hasText(standard.getProcessCode())
                && StringUtils.hasText(reqVO.getOperationCode())
                && standard.getProcessCode().equals(reqVO.getOperationCode()))
                || (StringUtils.hasText(standard.getProcessName())
                && StringUtils.hasText(reqVO.getOperationName())
                && (standard.getProcessName().equals(reqVO.getOperationName())
                || standard.getProcessName().contains(reqVO.getOperationName())
                || reqVO.getOperationName().contains(standard.getProcessName())));
    }

    private boolean matchesProcessScopeStrict(QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        return (StringUtils.hasText(standard.getProcessCode())
                && StringUtils.hasText(reqVO.getOperationCode())
                && standard.getProcessCode().equals(reqVO.getOperationCode()))
                || (StringUtils.hasText(standard.getProcessName())
                && StringUtils.hasText(reqVO.getOperationName())
                && standard.getProcessName().equals(reqVO.getOperationName()));
    }

    private void validateFaiStandardUsable(QmsQualityStandardDO standard, QmsFaiSaveReqVO reqVO) {
        if (isGlueBoardFaiSource(reqVO)) {
            if (standard == null
                    || !SOURCE_MODULE_GLUE_BOARD_FAI.equals(standard.getApplyType())
                    || !ENABLED.equals(standard.getStatus())
                    || !AUDITED.equals(standard.getAuditStatus())
                    || !StringUtils.hasText(reqVO.getGlueBoardModel())
                    || !reqVO.getGlueBoardModel().equals(standard.getGlueBoardModel())) {
                throw exception(HCFAI_STANDARD_NOT_EXISTS);
            }
            return;
        }
        if (standard == null
                || !APPLY_TYPE_FAI.equals(standard.getApplyType())
                || !ENABLED.equals(standard.getStatus())
                || !AUDITED.equals(standard.getAuditStatus())) {
            throw exception(HCFAI_STANDARD_NOT_EXISTS);
        }
    }

    private List<QmsQualityStandardItemDO> selectFaiStandardItems(Long standardId) {
        List<QmsQualityStandardItemDO> standardItems = qmsQualityStandardItemMapper.selectListByStandardId(standardId);
        if (standardItems == null || standardItems.isEmpty()) {
            throw exception(HCFAI_STANDARD_ITEMS_EMPTY);
        }
        return standardItems;
    }

    private List<QmsQualityStandardItemDO> selectFaiStandardItems(Long standardId, QmsFaiSaveReqVO reqVO) {
        return selectFaiStandardItems(standardId);
    }

    private List<QmsQualityStandardItemDO> resolveSelectedStandardItems(QmsFaiOrderDO order,
                                                                         List<QmsQualityStandardItemDO> allItems,
                                                                         List<Long> selectedStandardItemIds) {
        if (!requiresFaiRecheckItemSelection(order)) {
            return allItems;
        }
        if (selectedStandardItemIds == null || selectedStandardItemIds.isEmpty()) {
            throw exception0(HCFAI_STANDARD_ITEMS_EMPTY.getCode(), "复检请至少选择一个检验项目");
        }
        Set<Long> selectedIds = selectedStandardItemIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (selectedIds.size() != selectedStandardItemIds.size()) {
            throw exception0(HCFAI_STANDARD_ITEMS_EMPTY.getCode(), "复检选择的检验项目无效");
        }
        List<QmsQualityStandardItemDO> selectedItems = allItems.stream()
                .filter(item -> selectedIds.contains(item.getId()))
                .collect(Collectors.toList());
        if (selectedItems.size() != selectedIds.size()) {
            throw exception0(HCFAI_STANDARD_ITEMS_EMPTY.getCode(), "复检选择的检验项目不属于当前标准");
        }
        return selectedItems;
    }

    private boolean requiresFaiRecheckItemSelection(QmsFaiOrderDO order) {
        return order != null && Boolean.TRUE.equals(order.getRecheckFlag())
                && !SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule());
    }

    private boolean hasSameStandardItemSnapshot(List<QmsFaiItemDO> snapshotItems,
                                                 List<QmsQualityStandardItemDO> standardItems) {
        if (snapshotItems == null || snapshotItems.size() != standardItems.size()) {
            return false;
        }
        Set<Long> snapshotStandardItemIds = snapshotItems.stream()
                .map(QmsFaiItemDO::getStandardItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return snapshotStandardItemIds.size() == standardItems.size()
                && standardItems.stream().allMatch(item -> snapshotStandardItemIds.contains(item.getId()));
    }

    private boolean isMaterialProcessStandardMatchMode(QmsFaiSaveReqVO reqVO) {
        return reqVO != null
                && STANDARD_MATCH_MATERIAL_PROCESS.equalsIgnoreCase(defaultIfBlank(reqVO.getStandardMatchMode(), STANDARD_MATCH_MATERIAL_PROCESS));
    }

    private boolean isProductModelProcessStandardMatchMode(QmsFaiSaveReqVO reqVO) {
        return reqVO != null
                && STANDARD_MATCH_PRODUCT_MODEL_PROCESS.equalsIgnoreCase(defaultIfBlank(reqVO.getStandardMatchMode(), STANDARD_MATCH_MATERIAL_PROCESS));
    }

    private void throwFaiMaterialProcessStandardNotFound() {
        throw exception0(HCFAI_STANDARD_NOT_EXISTS.getCode(), FAI_MATERIAL_PROCESS_STANDARD_NOT_FOUND_MSG);
    }

    private void throwFaiProductModelProcessStandardNotFound() {
        throw exception0(HCFAI_STANDARD_NOT_EXISTS.getCode(), FAI_PRODUCT_MODEL_PROCESS_STANDARD_NOT_FOUND_MSG);
    }

    private void applyStandardSnapshot(QmsFaiOrderDO order, QmsQualityStandardDO standard) {
        order.setStandardId(standard.getId());
        order.setStandardNo(standard.getStandardNo());
        order.setStandardVersion(standard.getVersion());
        if (order.getMaterialId() == null) {
            order.setMaterialId(standard.getMaterialId());
        }
        if (!StringUtils.hasText(order.getMaterialCode())) {
            order.setMaterialCode(standard.getMaterialCode());
        }
        if (!StringUtils.hasText(order.getMaterialName())) {
            order.setMaterialName(standard.getMaterialName());
        }
        if (!StringUtils.hasText(order.getSpecification())) {
            order.setSpecification(standard.getSpecification());
        }
        if (!StringUtils.hasText(order.getProductModel())) {
            order.setProductModel(defaultIfBlank(standard.getProductModelName(), standard.getProductModelCode()));
        }
    }

    private void applyStandardMatchSnapshot(QmsFaiOrderDO order, StandardCandidateMatch selectedMatch) {
        if (order == null || selectedMatch == null) {
            return;
        }
        order.setStandardMatchType(selectedMatch.matchType());
        order.setMatchedModelId(selectedMatch.matchedModelId());
        order.setMatchedModelCode(selectedMatch.matchedModelCode());
        order.setStandardMatchReason(selectedMatch.matchReason());
    }

    private boolean isGlueBoardFaiSource(QmsFaiSaveReqVO reqVO) {
        return reqVO != null && SOURCE_MODULE_GLUE_BOARD_FAI.equals(reqVO.getSourceModule());
    }

    private void applyDefaultInspectionQty(QmsFaiOrderDO order) {
        if (order.getInspectionQty() != null || order.getSampleLength() == null
                || order.getSampleLength().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        order.setInspectionQty(order.getSampleLength());
    }

    private void saveStandardSnapshotDetails(QmsFaiOrderDO order, List<QmsQualityStandardItemDO> standardItems,
                                             List<QmsFaiSaveReqVO.FaiAbnormal> abnormals) {
        saveStandardSnapshotDetails(order, standardItems, abnormals, Collections.emptyMap());
    }

    private void saveStandardSnapshotDetails(QmsFaiOrderDO order, List<QmsQualityStandardItemDO> standardItems,
                                             List<QmsFaiSaveReqVO.FaiAbnormal> abnormals,
                                             Map<Long, Long> reusableItemIdsByStandardItemId) {
        for (int i = 0; i < standardItems.size(); i++) {
            QmsQualityStandardItemDO standardItem = standardItems.get(i);
            QmsFaiItemDO itemDO = new QmsFaiItemDO();
            // 标准切换会逻辑删除旧项目以便保留历史，历史快照中的项目主键仍受唯一约束。
            // 因此项目只能复用业务配置，不能复用数据库主键；每次均由持久层生成新主键。
            itemDO.setId(null);
            itemDO.setFaiId(order.getId());
            itemDO.setFaiNo(order.getFaiNo());
            itemDO.setStandardItemId(standardItem.getId());
            itemDO.setInspectionItem(standardItem.getInspectionItem());
            itemDO.setItemType(standardItem.getItemType());
            itemDO.setAttachmentEnabled(Boolean.TRUE.equals(standardItem.getAttachmentEnabled()));
            itemDO.setAttachmentUrls(Collections.emptyList());
            itemDO.setTargetValue(standardItem.getTargetValue());
            itemDO.setStandardDesc(standardItem.getStandardDesc());
            itemDO.setUnit(standardItem.getUnit());
            itemDO.setRuleDescription(standardItem.getRuleDescription());
            itemDO.setInspectionMethod(standardItem.getInspectionMethod());
            itemDO.setTestFrequencyJudgement(standardItem.getTestFrequencyJudgement());
            applyValueTemplateSnapshot(itemDO, standardItem);
            applySheetMetricSnapshot(itemDO, standardItem);
            itemDO.setTestTool(standardItem.getTestTool());
            itemDO.setSampleSize(standardItem.getSampleSize());
            itemDO.setMinValueLimit(standardItem.getMinValue());
            itemDO.setMaxValueLimit(standardItem.getMaxValue());
            itemDO.setOperatorResult(JUDGMENT_PENDING);
            itemDO.setQaResult(JUDGMENT_PENDING);
            itemDO.setRecheckItemFlag(Boolean.TRUE.equals(order.getRecheckFlag()));
            itemDO.setIsSpc(Boolean.TRUE.equals(standardItem.getIsSpc()));
            itemDO.setSort(standardItem.getSort() == null ? (i + 1) * 10 : standardItem.getSort());
            applyProgramItemDefaults(itemDO);
            qmsFaiItemMapper.insert(itemDO);
        }
        saveAbnormals(order, abnormals);
    }

    private void updateExecutionDetails(QmsFaiOrderDO order, List<QmsFaiSaveReqVO.FaiItem> items,
                                        List<QmsFaiSaveReqVO.FaiAbnormal> abnormals) {
        ensureStandardBound(order);
        validateItems(items);
        List<QmsFaiItemDO> existingItems = qmsFaiItemMapper.selectListByFaiId(order.getId());
        validateItemsExist(existingItems);
        for (QmsFaiSaveReqVO.FaiItem item : items) {
            if (item.getId() == null && item.getStandardItemId() == null) {
                throw exception(HCFAI_ITEM_SOURCE_INVALID);
            }
            if (!matchesExistingItem(item, existingItems)) {
                throw exception(HCFAI_ITEM_SOURCE_INVALID);
            }
        }

        List<String> touchedRoles = items.stream()
                .filter(item -> item.getSamples() != null)
                .flatMap(item -> item.getSamples().stream())
                .map(QmsFaiSaveReqVO.FaiSample::getSampleRole)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        List<Long> incomingItemIds = existingItems.stream()
                .filter(existingItem -> findIncomingItem(existingItem, items) != null)
                .map(QmsFaiItemDO::getId)
                .collect(Collectors.toList());
        if (!touchedRoles.isEmpty() && !incomingItemIds.isEmpty()) {
            qmsFaiSampleMapper.deleteByFaiIdAndItemIdsAndRoles(order.getId(), incomingItemIds, touchedRoles);
            qmsFaiSheetCellValueMapper.deleteByFaiIdAndItemIds(order.getId(), incomingItemIds);
            qmsFaiSheetStatResultMapper.deleteByFaiIdAndItemIds(order.getId(), incomingItemIds);
        }

        for (QmsFaiItemDO existingItem : existingItems) {
            QmsFaiSaveReqVO.FaiItem incomingItem = findIncomingItem(existingItem, items);
            if (incomingItem == null) {
                continue;
            }
            boolean attachmentTouched = incomingItem.getAttachmentUrls() != null;
            if (attachmentTouched) {
                applyAttachmentUrls(existingItem, incomingItem.getAttachmentUrls());
            }
            if (incomingItem.getSamples() == null || incomingItem.getSamples().isEmpty()) {
                if (attachmentTouched) {
                    qmsFaiItemMapper.updateById(existingItem);
                }
                continue;
            }
            applyIncomingProgramMeta(existingItem, incomingItem);
            boolean changed = attachmentTouched;
            boolean touchedQa = false;
            if (!filterSamples(incomingItem.getSamples(), ROLE_OPERATOR).isEmpty()) {
                qmsFaiSampleResultWritebackService.applyRoleStats(existingItem, incomingItem.getSamples(), ROLE_OPERATOR);
                changed = true;
            }
            if (!filterSamples(incomingItem.getSamples(), ROLE_QA).isEmpty()) {
                alignIncomingRecheckSamples(order, existingItem, incomingItem.getSamples());
                qmsFaiSampleResultWritebackService.applyRoleStats(existingItem, incomingItem.getSamples(), ROLE_QA);
                changed = true;
                touchedQa = true;
            }
            if (changed) {
                if (touchedQa) {
                    markIncomingRecheckGroupsWaitingAudit(order, existingItem, incomingItem.getSamples());
                }
                qmsFaiItemMapper.updateById(existingItem);
                saveSamples(order, existingItem, incomingItem.getSamples());
                qmsFaiSampleResultWritebackService.saveSheetStat(order, existingItem, incomingItem.getSamples());
            }
        }
        if (abnormals != null) {
            qmsFaiAbnormalMapper.deleteByFaiId(order.getId());
            saveAbnormals(order, abnormals);
        }
    }

    private QmsFaiSaveReqVO.FaiItem findIncomingItem(QmsFaiItemDO existingItem, List<QmsFaiSaveReqVO.FaiItem> items) {
        for (QmsFaiSaveReqVO.FaiItem item : items) {
            if (item.getId() != null && item.getId().equals(existingItem.getId())) {
                if (item.getStandardItemId() != null && !item.getStandardItemId().equals(existingItem.getStandardItemId())) {
                    throw exception(HCFAI_ITEM_SOURCE_INVALID);
                }
                return item;
            }
            if (item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId())) {
                return item;
            }
        }
        return null;
    }

    private void applyIncomingProgramMeta(QmsFaiItemDO itemDO, QmsFaiSaveReqVO.FaiItem incomingItem) {
        itemDO.setStepCode(defaultIfBlank(incomingItem.getStepCode(), defaultIfBlank(itemDO.getStepCode(), itemDO.getSheetSectionCode())));
        itemDO.setStepName(defaultIfBlank(incomingItem.getStepName(), defaultIfBlank(itemDO.getStepName(), resolveStepName(itemDO.getStepCode()))));
        itemDO.setMetricCode(defaultIfBlank(incomingItem.getMetricCode(),
                defaultIfBlank(itemDO.getMetricCode(), defaultIfBlank(itemDO.getSheetMetricCode(), itemDO.getInspectionItem()))));
        itemDO.setMetricGroupCode(defaultIfBlank(incomingItem.getMetricGroupCode(),
                defaultIfBlank(itemDO.getMetricGroupCode(), resolveMetricGroupCode(itemDO))));
        itemDO.setInputComponent(defaultIfBlank(incomingItem.getInputComponent(),
                defaultIfBlank(itemDO.getInputComponent(), resolveInputComponent(itemDO))));
        applyProgramItemDefaults(itemDO);
    }

    private void applyProgramItemDefaults(QmsFaiItemDO itemDO) {
        itemDO.setStepCode(defaultIfBlank(itemDO.getStepCode(), defaultIfBlank(itemDO.getSheetSectionCode(), "BASIC_INFO")));
        itemDO.setStepName(defaultIfBlank(itemDO.getStepName(), resolveStepName(itemDO.getStepCode())));
        itemDO.setMetricCode(defaultIfBlank(itemDO.getMetricCode(),
                defaultIfBlank(itemDO.getSheetMetricCode(), defaultIfBlank(itemDO.getInspectionItem(), "UNMAPPED"))));
        itemDO.setMetricGroupCode(defaultIfBlank(itemDO.getMetricGroupCode(), resolveMetricGroupCode(itemDO)));
        itemDO.setInputComponent(defaultIfBlank(itemDO.getInputComponent(), resolveInputComponent(itemDO)));
        int requiredCount = itemDO.getRequiredSampleCount() == null || itemDO.getRequiredSampleCount() < 1
                ? resolveTemplateExpectedSampleCount(itemDO,
                        itemDO.getSampleSize() == null || itemDO.getSampleSize() < 1 ? 1 : itemDO.getSampleSize())
                : itemDO.getRequiredSampleCount();
        itemDO.setRequiredSampleCount(requiredCount);
        itemDO.setCellRequiredCount(itemDO.getCellRequiredCount() == null || itemDO.getCellRequiredCount() < 1
                ? requiredCount : itemDO.getCellRequiredCount());
        itemDO.setCellCompletedCount(itemDO.getCellCompletedCount() == null ? 0 : itemDO.getCellCompletedCount());
        itemDO.setCompletedSampleCount(itemDO.getCompletedSampleCount() == null ? 0 : itemDO.getCompletedSampleCount());
        itemDO.setAbnormalSampleCount(itemDO.getAbnormalSampleCount() == null ? 0 : itemDO.getAbnormalSampleCount());
        itemDO.setInputStatus(defaultIfBlank(itemDO.getInputStatus(), INPUT_STATUS_EMPTY));
    }

    private String resolveMetricGroupCode(QmsFaiItemDO itemDO) {
        if (TEMPLATE_DENSITY_CALC.equals(itemDO.getValueTemplate())) {
            return "DENSITY";
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(itemDO.getValueTemplate())) {
            return "COMPRESSION";
        }
        String metric = defaultIfBlank(itemDO.getSheetMetricCode(), itemDO.getInspectionItem());
        if (StringUtils.hasText(metric) && metric.contains("沟深")) {
            return "GROOVE_DEPTH";
        }
        return metric;
    }

    private String resolveInputComponent(QmsFaiItemDO itemDO) {
        if (ITEM_TYPE_QUALITATIVE.equals(itemDO.getItemType())) {
            return COMPONENT_QUALITATIVE_JUDGMENT;
        }
        if (TEMPLATE_DENSITY_CALC.equals(itemDO.getValueTemplate())) {
            return COMPONENT_DENSITY_GROUP;
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(itemDO.getValueTemplate())) {
            return COMPONENT_COMPRESSION_GROUP;
        }
        String metric = defaultIfBlank(itemDO.getSheetMetricCode(), itemDO.getInspectionItem());
        if (StringUtils.hasText(metric) && metric.contains("沟深")) {
            return COMPONENT_GROOVE_DEPTH_MATRIX;
        }
        return COMPONENT_SINGLE_VALUE_LIST;
    }

    private String resolveStepName(String stepCode) {
        if ("NAP_RAW".equals(stepCode)) {
            return "Nap 未磨皮";
        }
        if ("NAP_POLISHED".equals(stepCode)) {
            return "Nap 磨皮后";
        }
        if ("EMBOSSING".equals(stepCode) || "BEFORE_EMBOSSING".equals(stepCode) || "AFTER_EMBOSSING".equals(stepCode)) {
            return "背胶压槽";
        }
        if ("FINAL_PRODUCTS".equals(stepCode)) {
            return "成品检验";
        }
        if ("GROOVE_DEPTH".equals(stepCode)) {
            return "沟深检验";
        }
        if ("CONFIRM".equals(stepCode)) {
            return "异常与确认";
        }
        return "基础信息";
    }

    private boolean matchesExistingItem(QmsFaiSaveReqVO.FaiItem item, List<QmsFaiItemDO> existingItems) {
        return existingItems.stream().anyMatch(existingItem ->
                (item.getId() != null && item.getId().equals(existingItem.getId()))
                        || (item.getStandardItemId() != null && item.getStandardItemId().equals(existingItem.getStandardItemId())));
    }

    private void replaceDetails(QmsFaiOrderDO order, List<QmsFaiSaveReqVO.FaiItem> items,
                                List<QmsFaiSaveReqVO.FaiAbnormal> abnormals) {
        qmsFaiSampleMapper.deleteByFaiId(order.getId());
        qmsFaiAbnormalMapper.deleteByFaiId(order.getId());
        qmsFaiItemMapper.deleteByFaiId(order.getId());
        saveDetails(order, items, abnormals);
    }

    private void saveDetails(QmsFaiOrderDO order, List<QmsFaiSaveReqVO.FaiItem> items,
                             List<QmsFaiSaveReqVO.FaiAbnormal> abnormals) {
        validateItems(items);
        for (int i = 0; i < items.size(); i++) {
            QmsFaiSaveReqVO.FaiItem item = items.get(i);
            QmsFaiItemDO itemDO = BeanUtils.toBean(item, QmsFaiItemDO.class);
            itemDO.setId(null);
            itemDO.setFaiId(order.getId());
            itemDO.setFaiNo(order.getFaiNo());
            itemDO.setSort(item.getSort() == null ? (i + 1) * 10 : item.getSort());
            itemDO.setIsSpc(Boolean.TRUE.equals(item.getIsSpc()));
            applyAttachmentUrls(itemDO, item.getAttachmentUrls());
            applyProgramItemDefaults(itemDO);
            qmsFaiSampleResultWritebackService.applyRoleStats(itemDO, item.getSamples(), ROLE_OPERATOR);
            qmsFaiSampleResultWritebackService.applyRoleStats(itemDO, item.getSamples(), ROLE_QA);
            qmsFaiItemMapper.insert(itemDO);
            saveSamples(order, itemDO, item.getSamples());
            qmsFaiSampleResultWritebackService.saveSheetStat(order, itemDO, item.getSamples());
        }
        saveAbnormals(order, abnormals);
    }

    private void applyAttachmentUrls(QmsFaiItemDO itemDO, List<String> attachmentUrls) {
        if (attachmentUrls == null) {
            return;
        }
        List<String> normalizedUrls = attachmentUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (!Boolean.TRUE.equals(itemDO.getAttachmentEnabled()) && !normalizedUrls.isEmpty()) {
            throw exception(HCFAI_ATTACHMENT_DISABLED, itemDO.getInspectionItem());
        }
        if (normalizedUrls.size() > MAX_ITEM_ATTACHMENT_COUNT) {
            throw exception(HCFAI_ATTACHMENT_TOO_MANY, itemDO.getInspectionItem());
        }
        itemDO.setAttachmentUrls(normalizedUrls);
    }

    private void saveSamples(QmsFaiOrderDO order, QmsFaiItemDO itemDO, List<QmsFaiSaveReqVO.FaiSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        List<QmsFaiSampleDO> sampleList = new ArrayList<>();
        for (QmsFaiSaveReqVO.FaiSample sample : samples) {
            QmsFaiSampleDO sampleDO = BeanUtils.toBean(sample, QmsFaiSampleDO.class);
            sampleDO.setId(null);
            sampleDO.setFaiId(order.getId());
            sampleDO.setFaiItemId(itemDO.getId());
            sampleDO.setFaiNo(order.getFaiNo());
            sampleDO.setStepCode(defaultIfBlank(sampleDO.getStepCode(), itemDO.getStepCode()));
            sampleDO.setMetricCode(defaultIfBlank(sampleDO.getMetricCode(), itemDO.getMetricCode()));
            sampleDO.setMetricGroupCode(defaultIfBlank(sampleDO.getMetricGroupCode(), itemDO.getMetricGroupCode()));
            sampleDO.setInputComponent(defaultIfBlank(sampleDO.getInputComponent(), itemDO.getInputComponent()));
            sampleDO.setValueSource(defaultIfBlank(sample.getValueSource(),
                    StringUtils.hasText(sample.getImportBatchNo()) ? VALUE_SOURCE_HISTORICAL_IMPORT : VALUE_SOURCE_MANUAL));
            sampleDO.setInputTime(LocalDateTime.now());
            sampleDO.setSampleResult(defaultIfBlank(sample.getSampleResult(), JUDGMENT_PENDING));
            sampleList.add(sampleDO);
        }
        qmsFaiSampleMapper.insertBatch(sampleList);
        qmsFaiSampleResultWritebackService.saveSheetCells(order, itemDO, sampleList, VALUE_SOURCE_MANUAL);
    }

    private void alignIncomingRecheckSamples(QmsFaiOrderDO order, QmsFaiItemDO item,
                                             List<QmsFaiSaveReqVO.FaiSample> samples) {
        List<QmsFaiSaveReqVO.FaiSample> qaSamples = filterSamples(samples, ROLE_QA);
        if (qaSamples.isEmpty()) {
            return;
        }
        List<QmsFaiItemGroupAuditDO> waitingAudits = selectGroupAudits(order.getId()).stream()
                .filter(audit -> item.getId().equals(audit.getFaiItemId()))
                .filter(audit -> ITEM_RECHECK_WAIT_RECHECK.equals(audit.getItemRecheckStatus()))
                .collect(Collectors.toList());
        if (waitingAudits.size() != 1) {
            return;
        }
        QmsFaiItemGroupAuditDO waitingAudit = waitingAudits.get(0);
        String waitingGroupKey = normalizeAuditGroupKey(waitingAudit.getGroupKey());
        boolean alreadyContainsWaitingGroup = qaSamples.stream()
                .map(sample -> normalizeAuditGroupKey(sample.getSamplePosition()))
                .anyMatch(waitingGroupKey::equals);
        if (alreadyContainsWaitingGroup) {
            return;
        }
        int expectedGroupCount = resolveRecheckGroupSampleCount(item);
        if (qaSamples.size() != expectedGroupCount) {
            return;
        }
        String samplePosition = StringUtils.hasText(waitingAudit.getSamplePosition())
                ? waitingAudit.getSamplePosition().trim()
                : waitingAudit.getGroupKey();
        for (QmsFaiSaveReqVO.FaiSample sample : qaSamples) {
            sample.setSamplePosition(samplePosition);
        }
    }

    private int resolveRecheckGroupSampleCount(QmsFaiItemDO item) {
        if (!StringUtils.hasText(item.getTemplateParams())) {
            return 1;
        }
        try {
            Map<String, Object> params = JsonUtils.parseObject(item.getTemplateParams(), Map.class);
            Integer repeatCount = QmsFaiRuleCalculationSupport.toPositiveInt(params.get("repeatCount"));
            return repeatCount == null || repeatCount < 1 ? 1 : repeatCount;
        } catch (Exception ex) {
            return 1;
        }
    }

    private void saveAbnormals(QmsFaiOrderDO order, List<QmsFaiSaveReqVO.FaiAbnormal> abnormals) {
        if (abnormals == null || abnormals.isEmpty()) {
            return;
        }
        List<QmsFaiAbnormalDO> abnormalList = new ArrayList<>();
        for (QmsFaiSaveReqVO.FaiAbnormal abnormal : abnormals) {
            QmsFaiAbnormalDO abnormalDO = BeanUtils.toBean(abnormal, QmsFaiAbnormalDO.class);
            abnormalDO.setId(null);
            abnormalDO.setFaiId(order.getId());
            abnormalDO.setFaiNo(order.getFaiNo());
            abnormalDO.setAbnormalRole(defaultIfBlank(abnormal.getAbnormalRole(), ROLE_SYSTEM));
            abnormalDO.setProcessStatus(defaultIfBlank(abnormal.getProcessStatus(), PROCESS_PENDING));
            abnormalList.add(abnormalDO);
        }
        qmsFaiAbnormalMapper.insertBatch(abnormalList);
    }

    private void applyValueTemplateSnapshot(QmsFaiItemDO itemDO, QmsQualityStandardItemDO standardItem) {
        String template = resolveValueTemplate(standardItem.getItemType(), standardItem.getValueTemplate());
        itemDO.setValueTemplate(template);
        itemDO.setValueTemplateName(defaultIfBlank(standardItem.getEntryRuleTemplateName(), resolveValueTemplateName(template)));
        itemDO.setJudgmentMetric(resolveJudgmentMetric(template, standardItem.getJudgmentMetric()));
        itemDO.setTemplateParams(standardItem.getTemplateParams());
        itemDO.setAvgMinLimit(standardItem.getAvgMinLimit());
        itemDO.setAvgMaxLimit(standardItem.getAvgMaxLimit());
        itemDO.setStdMinLimit(standardItem.getStdMinLimit());
        itemDO.setStdMaxLimit(standardItem.getStdMaxLimit());
    }

    private void applySheetMetricSnapshot(QmsFaiItemDO itemDO, QmsQualityStandardItemDO standardItem) {
        itemDO.setSheetSectionCode(standardItem.getSheetSectionCode());
        itemDO.setSheetMetricCode(standardItem.getSheetMetricCode());
        itemDO.setSheetFieldCode(standardItem.getSheetFieldCode());
    }

    private String resolveValueTemplate(String itemType, String valueTemplate) {
        return QmsFaiRuleCalculationSupport.resolveValueTemplate(itemType, valueTemplate);
    }

    private String resolveJudgmentMetric(String valueTemplate, String judgmentMetric) {
        return QmsFaiRuleCalculationSupport.resolveJudgmentMetric(valueTemplate, judgmentMetric);
    }

    private String resolveValueTemplateName(String valueTemplate) {
        return QmsFaiRuleCalculationSupport.resolveValueTemplateName(valueTemplate);
    }

    private int resolveTemplateExpectedSampleCount(QmsFaiItemDO itemDO, int fallback) {
        return QmsFaiRuleCalculationSupport.resolveTemplateExpectedSampleCount(itemDO, fallback);
    }

    private String calculateRoleJudgment(List<QmsFaiItemDO> items, String role) {
        validateItemsExist(items);
        boolean hasPending = false;
        for (QmsFaiItemDO item : items) {
            String itemResult = ROLE_OPERATOR.equals(role) ? item.getOperatorResult() : item.getQaResult();
            if (JUDGMENT_NG.equals(itemResult)) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(itemResult)) {
                hasPending = true;
            }
        }
        if (hasPending) {
            throw exception(ROLE_OPERATOR.equals(role) ? HCFAI_OPERATOR_ITEMS_NOT_COMPLETED : HCFAI_QA_ITEMS_NOT_COMPLETED);
        }
        return JUDGMENT_OK;
    }

    private void ensureNgAbnormal(Long faiId, String faiNo, List<QmsFaiItemDO> items, String role) {
        List<QmsFaiAbnormalDO> existing = qmsFaiAbnormalMapper.selectListByFaiId(faiId);
        if (!existing.isEmpty()) {
            return;
        }
        List<QmsFaiAbnormalDO> abnormalList = items.stream()
                .filter(item -> JUDGMENT_NG.equals(ROLE_OPERATOR.equals(role) ? item.getOperatorResult() : item.getQaResult()))
                .map(item -> QmsFaiAbnormalDO.builder()
                        .faiId(faiId)
                        .faiNo(faiNo)
                        .faiItemId(item.getId())
                        .abnormalRole(role)
                        .abnormalDesc("首件检验项判定不合格：" + item.getInspectionItem())
                        .processStatus(PROCESS_PENDING)
                        .actionRequired(ACTION_REWORK)
                        .build())
                .collect(Collectors.toList());
        if (!abnormalList.isEmpty()) {
            qmsFaiAbnormalMapper.insertBatch(abnormalList);
        }
    }

    private void markIncomingRecheckGroupsWaitingAudit(QmsFaiOrderDO order, QmsFaiItemDO item,
                                                       List<QmsFaiSaveReqVO.FaiSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        Set<String> incomingGroupKeys = samples.stream()
                .filter(sample -> ROLE_QA.equals(sample.getSampleRole()))
                .map(sample -> normalizeAuditGroupKey(sample.getSamplePosition()))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (incomingGroupKeys.isEmpty()) {
            return;
        }
        List<QmsFaiItemGroupAuditDO> groupAudits = selectGroupAudits(order.getId())
                .stream()
                .filter(audit -> item.getId().equals(audit.getFaiItemId()))
                .filter(audit -> ITEM_RECHECK_WAIT_RECHECK.equals(audit.getItemRecheckStatus()))
                .filter(audit -> incomingGroupKeys.contains(normalizeAuditGroupKey(audit.getGroupKey())))
                .collect(Collectors.toList());
        if (groupAudits.isEmpty()) {
            return;
        }
        for (QmsFaiItemGroupAuditDO groupAudit : groupAudits) {
            groupAudit.setAuditResult(ITEM_AUDIT_PENDING);
            groupAudit.setAuditRemark(null);
            groupAudit.setAuditUserId(null);
            groupAudit.setAuditUserName(null);
            groupAudit.setAuditTime(null);
            groupAudit.setItemRecheckStatus(ITEM_RECHECK_WAIT_AUDIT);
            groupAudit.setRecheckItemFlag(Boolean.TRUE);
            qmsFaiItemGroupAuditMapper.updateById(groupAudit);
        }
        item.setRecheckItemFlag(Boolean.TRUE);
    }

    private int ensureAuditFailAbnormal(QmsFaiOrderDO order, List<QmsFaiItemDO> items, String failReason) {
        ensureNgAbnormal(order.getId(), order.getFaiNo(), items, ROLE_QA);
        List<QmsFaiAbnormalDO> existing = qmsFaiAbnormalMapper.selectListByFaiId(order.getId());
        if (existing.isEmpty()) {
            qmsFaiAbnormalMapper.insert(QmsFaiAbnormalDO.builder()
                    .faiId(order.getId())
                    .faiNo(order.getFaiNo())
                    .abnormalRole(ROLE_QA)
                    .abnormalDesc("首件整单审核判定不合格：" + failReason)
                    .processStatus(PROCESS_PENDING)
                    .actionRequired(ACTION_REWORK)
                    .build());
            return 1;
        }
        long abnormalItemCount = existing.stream()
                .map(QmsFaiAbnormalDO::getFaiItemId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        return Math.max(1, Math.toIntExact(abnormalItemCount));
    }

    private void fillDetails(QmsFaiRespVO respVO) {
        List<QmsFaiItemDO> items = loadOrCreateSnapshotItems(respVO);
        List<QmsFaiSampleDO> samples = qmsFaiSampleMapper.selectListByFaiId(respVO.getId());
        Map<Long, List<QmsFaiRespVO.FaiSample>> sampleMap = BeanUtils.toBean(samples, QmsFaiRespVO.FaiSample.class)
                .stream()
                .collect(Collectors.groupingBy(QmsFaiRespVO.FaiSample::getFaiItemId));
        Map<Long, List<QmsFaiRespVO.FaiGroupAudit>> auditGroupMap = BeanUtils.toBean(
                        selectGroupAudits(respVO.getId()), QmsFaiRespVO.FaiGroupAudit.class)
                .stream()
                .collect(Collectors.groupingBy(QmsFaiRespVO.FaiGroupAudit::getFaiItemId));
        List<QmsFaiRespVO.FaiItem> itemRespList = BeanUtils.toBean(items, QmsFaiRespVO.FaiItem.class);
        itemRespList.forEach(item -> {
            item.setSamples(sampleMap.getOrDefault(item.getId(), Collections.emptyList()));
            item.setAuditGroups(auditGroupMap.getOrDefault(item.getId(), Collections.emptyList()));
        });
        respVO.setItems(itemRespList);
        respVO.setAbnormals(BeanUtils.toBean(qmsFaiAbnormalMapper.selectListByFaiId(respVO.getId()), QmsFaiRespVO.FaiAbnormal.class));
        fillStandardSelectionState(respVO, items);
    }

    private List<QmsFaiItemDO> loadOrCreateSnapshotItems(QmsFaiRespVO respVO) {
        List<QmsFaiItemDO> items = qmsFaiItemMapper.selectListByFaiId(respVO.getId());
        return items == null ? Collections.emptyList() : items;
    }

    private void fillStandardSelectionState(QmsFaiRespVO respVO, List<QmsFaiItemDO> snapshotItems) {
        boolean switchAllowed = isFaiStandardSwitchAllowed(respVO.getStatus());
        respVO.setStandardSwitchAllowed(switchAllowed);
        respVO.setStandardContentChanged(false);
        if (respVO.getStandardId() == null) {
            respVO.setStandardSelectionMessage(switchAllowed
                    ? "当前检验记录尚未挂接标准，请选择已审核标准后开始填写"
                    : null);
            return;
        }
        if (!switchAllowed && !STATUS_WAITING_QA.equals(respVO.getStatus())) {
            respVO.setStandardSelectionMessage(null);
            return;
        }
        QmsQualityStandardDO current = qmsQualityStandardMapper.selectById(respVO.getStandardId());
        if (current == null) {
            respVO.setStandardContentChanged(true);
            respVO.setStandardSelectionMessage("原检验标准已不存在，请联系质量管理员处理");
            return;
        }
        List<QmsQualityStandardItemDO> currentItems =
                qmsQualityStandardItemMapper.selectListByStandardId(current.getId());
        currentItems = selectCurrentItemsForSnapshotComparison(respVO, snapshotItems, currentItems);
        String snapshotHash = StringUtils.hasText(respVO.getStandardSnapshotHash())
                ? respVO.getStandardSnapshotHash()
                : qmsQualityStandardContentHashService.hashFaiSnapshotItems(snapshotItems);
        respVO.setStandardSnapshotHash(snapshotHash);
        String currentHash = qmsQualityStandardContentHashService.hashFaiStandardItems(currentItems);
        boolean changed = !Objects.equals(snapshotHash, currentHash);
        respVO.setStandardContentChanged(changed);
        if (!changed) {
            respVO.setStandardSelectionMessage(null);
            return;
        }
        if (!ENABLED.equals(current.getStatus()) || !AUDITED.equals(current.getAuditStatus())) {
            respVO.setStandardSelectionMessage(
                    "当前标准内容已变化但尚未审核启用，可等待审核后重新加载，或选择其他已审核标准");
        } else if (STATUS_WAITING_QA.equals(respVO.getStatus())) {
            respVO.setStandardSelectionMessage("标准内容已变化；当前检验单已提交待审核，退回后可重新选择标准");
        } else {
            respVO.setStandardSelectionMessage("当前已审核标准内容已变化，建议重新加载或重新选择标准");
        }
    }

    /**
     * 复检只固化检验员选择的标准项目。标准内容变化校验也必须限定在这批项目，
     * 否则标准中未选择的项目会使快照哈希必然不一致，导致页面持续误报“标准已变化”。
     */
    private List<QmsQualityStandardItemDO> selectCurrentItemsForSnapshotComparison(
            QmsFaiRespVO respVO, List<QmsFaiItemDO> snapshotItems,
            List<QmsQualityStandardItemDO> currentItems) {
        if (!requiresFaiRecheckItemSelection(respVO) || CollectionUtils.isEmpty(snapshotItems)
                || CollectionUtils.isEmpty(currentItems)) {
            return currentItems;
        }
        Set<Long> selectedStandardItemIds = snapshotItems.stream()
                .map(QmsFaiItemDO::getStandardItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (selectedStandardItemIds.isEmpty()) {
            return currentItems;
        }
        return currentItems.stream()
                .filter(item -> selectedStandardItemIds.contains(item.getId()))
                .collect(Collectors.toList());
    }

    private boolean requiresFaiRecheckItemSelection(QmsFaiRespVO respVO) {
        return respVO != null && Boolean.TRUE.equals(respVO.getRecheckFlag())
                && !SOURCE_MODULE_GLUE_BOARD_FAI.equals(respVO.getSourceModule());
    }

    private QmsInspectionStandardCandidateRespVO buildStandardCandidate(
            StandardCandidateMatch match, boolean recommended) {
        QmsQualityStandardDO standard = match.standard();
        QmsInspectionStandardCandidateRespVO resp =
                BeanUtils.toBean(standard, QmsInspectionStandardCandidateRespVO.class);
        List<QmsQualityStandardItemDO> items =
                qmsQualityStandardItemMapper.selectListByStandardId(standard.getId());
        resp.setItemCount(items == null ? 0 : items.size());
        resp.setMatchType(match.matchType());
        resp.setMatchScore(match.score());
        resp.setMatchReason(match.matchReason());
        resp.setRecommended(recommended);
        return resp;
    }

    private QmsFaiStandardItemCandidateRespVO buildFaiStandardItemCandidate(
            QmsQualityStandardItemDO standardItem, boolean originalNg) {
        QmsFaiStandardItemCandidateRespVO resp = new QmsFaiStandardItemCandidateRespVO();
        resp.setStandardItemId(standardItem.getId());
        resp.setInspectionItem(standardItem.getInspectionItem());
        resp.setStandardDesc(standardItem.getStandardDesc());
        resp.setUnit(standardItem.getUnit());
        resp.setInspectionMethod(standardItem.getInspectionMethod());
        resp.setTestFrequencyJudgement(standardItem.getTestFrequencyJudgement());
        resp.setSampleSize(standardItem.getSampleSize());
        resp.setSort(standardItem.getSort());
        resp.setOriginalNg(originalNg);
        return resp;
    }

    private void validateFaiStandardSwitchAllowed(QmsFaiOrderDO order) {
        if (!isFaiStandardSwitchAllowed(order.getStatus())) {
            if (STATUS_WAITING_QA.equals(order.getStatus())) {
                throw exception0(HCFAI_FINISHED_LOCKED.getCode(),
                        "当前检验单已提交待审核，请先由审核人退回后再重新选择标准");
            }
            throw exception(HCFAI_FINISHED_LOCKED);
        }
    }

    private boolean isFaiStandardSwitchAllowed(String status) {
        return STATUS_PENDING.equals(status)
                || STATUS_INSPECTING.equals(status)
                || STATUS_SUSPENDED.equals(status);
    }

    private String buildFaiSwitchSnapshot(QmsFaiOrderDO order, List<QmsFaiItemDO> items) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("order", order);
        snapshot.put("items", items == null ? List.of() : items);
        snapshot.put("samples", qmsFaiSampleMapper.selectListByFaiId(order.getId()));
        snapshot.put("abnormals", qmsFaiAbnormalMapper.selectListByFaiId(order.getId()));
        snapshot.put("returnRecords", qmsFaiReturnRecordMapper.selectList(
                new LambdaQueryWrapperX<QmsFaiReturnRecordDO>()
                        .eq(QmsFaiReturnRecordDO::getFaiId, order.getId())));
        snapshot.put("itemAuditHistory", qmsFaiItemAuditHistoryMapper.selectList(
                new LambdaQueryWrapperX<QmsFaiItemAuditHistoryDO>()
                        .eq(QmsFaiItemAuditHistoryDO::getFaiId, order.getId())));
        snapshot.put("groupAudits", qmsFaiItemGroupAuditMapper.selectListByFaiId(order.getId()));
        snapshot.put("sheetCellValues", qmsFaiSheetCellValueMapper.selectListByFaiId(order.getId()));
        snapshot.put("sheetStatResults", qmsFaiSheetStatResultMapper.selectListByFaiId(order.getId()));
        snapshot.put("importBatches", qmsFaiSheetImportBatchMapper.selectList(
                new LambdaQueryWrapperX<QmsFaiSheetImportBatchDO>()
                        .eq(QmsFaiSheetImportBatchDO::getFaiId, order.getId())));
        return JsonUtils.toJsonString(snapshot);
    }

    private void saveStandardSwitchLog(String inspectionType, Long inspectionId, String inspectionNo,
                                       Long oldStandardId, String oldStandardNo, String oldContentHash,
                                       Long newStandardId, String newStandardNo, String newContentHash,
                                       String oldSnapshotJson, String reason, Long tenantId, LocalDateTime switchTime) {
        qmsInspectionStandardSwitchLogMapper.insert(QmsInspectionStandardSwitchLogDO.builder()
                .inspectionType(inspectionType)
                .inspectionId(inspectionId)
                .inspectionNo(inspectionNo)
                .oldStandardId(oldStandardId)
                .oldStandardNo(oldStandardNo)
                .oldContentHash(oldContentHash)
                .newStandardId(newStandardId)
                .newStandardNo(newStandardNo)
                .newContentHash(newContentHash)
                .oldSnapshotJson(oldSnapshotJson)
                .switchReason(StringUtils.hasText(reason) ? reason.trim() : "开始检验前选择标准")
                .operatorId(SecurityFrameworkUtils.getLoginUserId())
                .operatorName(resolveLoginUserName())
                .switchTime(switchTime)
                .tenantId(tenantId)
                .build());
    }

    private String resolveInspectionType(QmsFaiOrderDO order) {
        return order != null && SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())
                ? SOURCE_MODULE_GLUE_BOARD_FAI : APPLY_TYPE_FAI;
    }

    private QmsFaiStandardRespVO.StandardItem buildStandardItem(QmsQualityStandardItemDO item) {
        QmsFaiStandardRespVO.StandardItem resp = new QmsFaiStandardRespVO.StandardItem();
        resp.setStandardItemId(item.getId());
        resp.setInspectionItem(item.getInspectionItem());
        resp.setItemType(item.getItemType());
        resp.setAttachmentEnabled(item.getAttachmentEnabled());
        resp.setTargetValue(item.getTargetValue());
        resp.setStandardDesc(item.getStandardDesc());
        resp.setUnit(item.getUnit());
        resp.setRuleDescription(item.getRuleDescription());
        resp.setInspectionMethod(item.getInspectionMethod());
        resp.setTestFrequencyJudgement(item.getTestFrequencyJudgement());
        resp.setValueTemplate(resolveValueTemplate(item.getItemType(), item.getValueTemplate()));
        resp.setValueTemplateName(defaultIfBlank(item.getEntryRuleTemplateName(), resolveValueTemplateName(resp.getValueTemplate())));
        resp.setJudgmentMetric(resolveJudgmentMetric(resp.getValueTemplate(), item.getJudgmentMetric()));
        resp.setTemplateParams(item.getTemplateParams());
        resp.setAvgMinLimit(item.getAvgMinLimit());
        resp.setAvgMaxLimit(item.getAvgMaxLimit());
        resp.setStdMinLimit(item.getStdMinLimit());
        resp.setStdMaxLimit(item.getStdMaxLimit());
        resp.setSheetTemplateId(item.getSheetTemplateId());
        resp.setSheetSectionCode(item.getSheetSectionCode());
        resp.setSheetMetricCode(item.getSheetMetricCode());
        resp.setSheetFieldCode(item.getSheetFieldCode());
        resp.setTestTool(item.getTestTool());
        resp.setSampleSize(item.getSampleSize());
        resp.setMinValueLimit(item.getMinValue());
        resp.setMaxValueLimit(item.getMaxValue());
        resp.setIsSpc(item.getIsSpc());
        resp.setSort(item.getSort());
        return resp;
    }

    private QmsFaiSheetTemplateRespVO buildSheetTemplateResp(QmsFaiSheetTemplateDO template) {
        return buildSheetTemplateResp(template, qmsFaiSheetSectionMapper.selectListByTemplateId(template.getId()));
    }

    private QmsFaiSheetTemplateRespVO buildSheetTemplateResp(QmsFaiSheetTemplateDO template, List<QmsFaiSheetSectionDO> sections) {
        QmsFaiSheetTemplateRespVO respVO = BeanUtils.toBean(template, QmsFaiSheetTemplateRespVO.class);
        if (sections == null || sections.isEmpty()) {
            respVO.setSections(Collections.emptyList());
            return respVO;
        }
        List<Long> sectionIds = sections.stream().map(QmsFaiSheetSectionDO::getId).collect(Collectors.toList());
        Map<Long, List<QmsFaiSheetFieldDO>> fieldMap = qmsFaiSheetFieldMapper.selectListBySectionIds(sectionIds)
                .stream()
                .collect(Collectors.groupingBy(QmsFaiSheetFieldDO::getSectionId));
        List<QmsFaiSheetTemplateRespVO.Section> sectionRespList = BeanUtils.toBean(sections, QmsFaiSheetTemplateRespVO.Section.class);
        sectionRespList.forEach(section -> section.setFields(BeanUtils.toBean(
                fieldMap.getOrDefault(section.getId(), Collections.emptyList()), QmsFaiSheetTemplateRespVO.Field.class)));
        respVO.setSections(sectionRespList);
        return respVO;
    }

    private QmsFaiSheetTemplateDO validateSheetTemplate(Long id) {
        QmsFaiSheetTemplateDO fixedTemplate = findFixedSheetTemplateDO(id);
        if (fixedTemplate != null) {
            return fixedTemplate;
        }
        QmsFaiSheetTemplateDO template = qmsFaiSheetTemplateMapper.selectById(id);
        if (template == null || !TEMPLATE_STATUS_ENABLE.equals(template.getStatus())) {
            throw exception(HCFAI_SHEET_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private QmsFaiSheetTemplateDO findFixedSheetTemplateDO(Long id) {
        QmsFaiSheetTemplateRespVO fixedTemplate = findFixedSheetTemplateResp(id);
        if (fixedTemplate == null) {
            return null;
        }
        return QmsFaiSheetTemplateDO.builder()
                .id(fixedTemplate.getId())
                .templateCode(fixedTemplate.getTemplateCode())
                .templateName(fixedTemplate.getTemplateName())
                .templateVersion(fixedTemplate.getTemplateVersion())
                .productModel(fixedTemplate.getProductModel())
                .sheetName(fixedTemplate.getSheetName())
                .status(fixedTemplate.getStatus())
                .remark(fixedTemplate.getRemark())
                .build();
    }

    private QmsFaiSheetTemplateRespVO findFixedSheetTemplateResp(Long id) {
        if (id == null) {
            return null;
        }
        return buildFixedSheetTemplates().stream()
                .filter(template -> id.equals(template.getId()))
                .findFirst()
                .orElse(null);
    }

    private List<QmsFaiSheetTemplateRespVO> buildFixedSheetTemplates() {
        return new ArrayList<>(List.of(
                buildSingleValueTemplate(),
                buildDensityTemplate(),
                buildCompressionTemplate()));
    }

    private QmsFaiSheetTemplateRespVO buildSingleValueTemplate() {
        QmsFaiSheetTemplateRespVO template = buildFixedTemplate(FIXED_TEMPLATE_SINGLE_ID,
                FIXED_TEMPLATE_SINGLE_CODE, "FAI单值实测原始记录表模板",
                "厚度、硬度、开孔径、孔隙率、沟深等单值类定量实测");
        QmsFaiSheetTemplateRespVO.Section section = buildFixedSection(FIXED_TEMPLATE_SINGLE_ID * 10,
                template.getId(), "SINGLE_VALUE", "单值实测区", "NORMAL_SAMPLE", 15, 1, 10);
        section.setFields(List.of(
                buildFixedField(section, "THICKNESS", "厚度", "value", "实测值", "INPUT", "mm", 10),
                buildFixedField(section, "HARDNESS", "硬度", "value", "实测值", "INPUT", null, 20),
                buildFixedField(section, "PORE_SIZE", "开孔径", "value", "实测值", "INPUT", "um", 30),
                buildFixedField(section, "POROSITY", "孔隙率", "value", "实测值", "INPUT", "%", 40),
                buildFixedField(section, "GROOVE_DEPTH", "沟深", "value", "实测值", "INPUT", "mm", 50)));
        template.setSections(List.of(section));
        return template;
    }

    private QmsFaiSheetTemplateRespVO buildDensityTemplate() {
        QmsFaiSheetTemplateRespVO template = buildFixedTemplate(FIXED_TEMPLATE_DENSITY_ID,
                FIXED_TEMPLATE_DENSITY_CODE, "FAI密度计算原始记录表模板",
                "录入厚度、重量，系统计算密度并按密度值判定");
        QmsFaiSheetTemplateRespVO.Section section = buildFixedSection(FIXED_TEMPLATE_DENSITY_ID * 10,
                template.getId(), "DENSITY", "密度计算区", "CALC_SAMPLE", 15, 2, 20);
        section.setFields(List.of(
                buildFixedField(section, "DENSITY", "密度", "thicknessMm", "厚度", "INPUT", "mm", 10),
                buildFixedField(section, "DENSITY", "密度", "weightG", "重量", "INPUT", "g", 20),
                buildFixedField(section, "DENSITY", "密度", "densityValue", "密度", "CALCULATED", "g/cm2", 30)));
        template.setSections(List.of(section));
        return template;
    }

    private QmsFaiSheetTemplateRespVO buildCompressionTemplate() {
        QmsFaiSheetTemplateRespVO template = buildFixedTemplate(FIXED_TEMPLATE_COMPRESSION_ID,
                FIXED_TEMPLATE_COMPRESSION_CODE, "FAI压缩性能原始记录表模板",
                "录入T1/T2/T3，系统计算压缩率和压缩弹性率");
        QmsFaiSheetTemplateRespVO.Section section = buildFixedSection(FIXED_TEMPLATE_COMPRESSION_ID * 10,
                template.getId(), "COMPRESSION", "压缩性能区", "CALC_SAMPLE", 15, 3, 30);
        section.setFields(List.of(
                buildFixedField(section, "COMPRESSION_RATE", "压缩率", "t1Mm", "T1", "INPUT", "mm", 10),
                buildFixedField(section, "COMPRESSION_RATE", "压缩率", "t2Mm", "T2", "INPUT", "mm", 20),
                buildFixedField(section, "COMPRESSION_RATE", "压缩率", "t3Mm", "T3", "INPUT", "mm", 30),
                buildFixedField(section, "COMPRESSION_RATE", "压缩率", "compressionRate", "压缩率", "CALCULATED", "%", 40),
                buildFixedField(section, "COMPRESSION_ELASTICITY_RATE", "压缩弹性率",
                        "compressionElasticityRate", "压缩弹性率", "CALCULATED", "%", 50)));
        template.setSections(List.of(section));
        return template;
    }

    private QmsFaiSheetTemplateRespVO buildFixedTemplate(Long id, String code, String name, String remark) {
        QmsFaiSheetTemplateRespVO template = new QmsFaiSheetTemplateRespVO();
        template.setId(id);
        template.setTemplateCode(code);
        template.setTemplateName(name);
        template.setTemplateVersion("A/1");
        template.setSheetName(name);
        template.setStatus(TEMPLATE_STATUS_ENABLE);
        template.setRemark(remark);
        template.setSections(Collections.emptyList());
        return template;
    }

    private QmsFaiSheetTemplateRespVO.Section buildFixedSection(Long id, Long templateId,
                                                                String sectionCode, String sectionName,
                                                                String sectionType, Integer expectedRows,
                                                                Integer expectedColumns, Integer sort) {
        QmsFaiSheetTemplateRespVO.Section section = new QmsFaiSheetTemplateRespVO.Section();
        section.setId(id);
        section.setTemplateId(templateId);
        section.setSectionCode(sectionCode);
        section.setSectionName(sectionName);
        section.setSectionType(sectionType);
        section.setExpectedRows(expectedRows);
        section.setExpectedColumns(expectedColumns);
        section.setSort(sort);
        section.setFields(Collections.emptyList());
        return section;
    }

    private QmsFaiSheetTemplateRespVO.Field buildFixedField(QmsFaiSheetTemplateRespVO.Section section,
                                                            String metricCode, String metricName,
                                                            String fieldCode, String fieldName,
                                                            String fieldRole, String unit, Integer sort) {
        QmsFaiSheetTemplateRespVO.Field field = new QmsFaiSheetTemplateRespVO.Field();
        field.setId(section.getId() * 100 + sort);
        field.setTemplateId(section.getTemplateId());
        field.setSectionId(section.getId());
        field.setMetricCode(metricCode);
        field.setMetricName(metricName);
        field.setFieldCode(fieldCode);
        field.setFieldName(fieldName);
        field.setFieldRole(fieldRole);
        field.setUnit(unit);
        field.setSort(sort);
        return field;
    }

    private void applySheetTemplateSnapshot(QmsFaiOrderDO order, QmsFaiSheetTemplateDO template) {
        order.setSheetTemplateId(template.getId());
        order.setSheetTemplateCode(template.getTemplateCode());
        order.setSheetTemplateName(template.getTemplateName());
        order.setSheetTemplateVersion(template.getTemplateVersion());
    }

    private List<QmsFaiSaveReqVO.FaiItem> parseOriginSheet(QmsFaiOrderDO order, MultipartFile file, String importBatchNo,
                                                           List<String> messages) throws IOException {
        List<QmsFaiItemDO> existingItems = qmsFaiItemMapper.selectListByFaiId(order.getId());
        validateItemsExist(existingItems);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<QmsFaiSaveReqVO.FaiItem> importedItems = new ArrayList<>();
            List<Long> usedItemIds = new ArrayList<>();
            for (SheetMetricImportDef def : buildW26ImportDefs()) {
                QmsFaiItemDO target = matchImportTarget(existingItems, usedItemIds, def);
                if (target == null) {
                    messages.add("未匹配到检验项：" + def.sectionName + "/" + def.metricName);
                    continue;
                }
                List<QmsFaiSaveReqVO.FaiSample> samples = buildImportedSamples(sheet, formatter, def, importBatchNo);
                if (samples.isEmpty()) {
                    messages.add("未读取到样本数据：" + def.sectionName + "/" + def.metricName);
                    continue;
                }
                QmsFaiSaveReqVO.FaiItem item = BeanUtils.toBean(target, QmsFaiSaveReqVO.FaiItem.class);
                item.setSamples(samples);
                importedItems.add(item);
                usedItemIds.add(target.getId());
                messages.add("已导入：" + def.sectionName + "/" + def.metricName + "，样本 " + samples.size() + " 组");
            }
            return importedItems;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(HCFAI_SHEET_IMPORT_INVALID);
        }
    }

    private QmsFaiItemDO matchImportTarget(List<QmsFaiItemDO> existingItems, List<Long> usedItemIds, SheetMetricImportDef def) {
        return existingItems.stream()
                .filter(item -> !usedItemIds.contains(item.getId()))
                .filter(item -> ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()))
                .filter(item -> matchesImportDef(item, def))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesImportDef(QmsFaiItemDO item, SheetMetricImportDef def) {
        if (StringUtils.hasText(item.getSheetSectionCode()) || StringUtils.hasText(item.getSheetMetricCode())) {
            return Objects.equals(item.getSheetSectionCode(), def.sectionCode)
                    && Objects.equals(item.getSheetMetricCode(), def.metricCode);
        }
        String inspectionItem = item.getInspectionItem() == null ? "" : item.getInspectionItem();
        return inspectionItem.contains(def.metricName);
    }

    private List<QmsFaiSaveReqVO.FaiSample> buildImportedSamples(Sheet sheet, DataFormatter formatter,
                                                                 SheetMetricImportDef def, String importBatchNo) {
        List<QmsFaiSaveReqVO.FaiSample> samples = new ArrayList<>();
        for (int rowIndex = def.firstRow; rowIndex <= def.lastRow; rowIndex++) {
            BigDecimal result = readDecimal(sheet, formatter, rowIndex, def.resultColumn);
            Map<String, Object> rawValues = def.readRawValues(sheet, formatter, rowIndex);
            if (result == null && rawValues.isEmpty()) {
                continue;
            }
            QmsFaiSaveReqVO.FaiSample sample = new QmsFaiSaveReqVO.FaiSample();
            sample.setSampleRole(ROLE_QA);
            sample.setSampleSeq(samples.size() + 1);
            sample.setSampleGroupNo(sample.getSampleSeq());
            sample.setSamplePosition(readString(sheet, formatter, rowIndex, def.positionColumn));
            sample.setSheetSectionCode(def.sectionCode);
            sample.setSheetMetricCode(def.metricCode);
            sample.setImportBatchNo(importBatchNo);
            sample.setRawValuesJson(rawValues.isEmpty() ? null : JsonUtils.toJsonString(rawValues));
            sample.setMeasuredValue(result);
            sample.setResultValue(result);
            sample.setSampleResult(JUDGMENT_PENDING);
            samples.add(sample);
        }
        return samples;
    }

    private List<SheetMetricImportDef> buildW26ImportDefs() {
        List<SheetMetricImportDef> defs = new ArrayList<>();
        defs.add(SheetMetricImportDef.single("NAP_RAW", "Nap未磨皮", "THICKNESS", "厚度", "A", "B"));
        defs.add(SheetMetricImportDef.density("NAP_RAW", "Nap未磨皮", "DENSITY", "密度", "C", "D", "E", "F"));
        defs.add(SheetMetricImportDef.single("NAP_POLISHED", "Nap磨皮后", "THICKNESS", "厚度", "G", "H"));
        defs.add(SheetMetricImportDef.single("NAP_POLISHED", "Nap磨皮后", "HARDNESS", "硬度", "I", "J"));
        defs.add(SheetMetricImportDef.density("NAP_POLISHED", "Nap磨皮后", "DENSITY", "密度", "K", "L", "M", "N"));
        defs.add(SheetMetricImportDef.compression("NAP_POLISHED", "Nap磨皮后", "COMPRESSION_RATE", "压缩率", "O", "P", "R", "T", "V"));
        defs.add(SheetMetricImportDef.compression("NAP_POLISHED", "Nap磨皮后", "COMPRESSION_ELASTICITY_RATE", "压缩弹性率", "O", "P", "R", "T", "X"));
        defs.add(SheetMetricImportDef.single("BEFORE_EMBOSSING", "压槽前", "THICKNESS", "厚度", "AA", "AB"));
        defs.add(SheetMetricImportDef.single("AFTER_EMBOSSING", "压槽后", "THICKNESS", "厚度", "AC", "AD"));
        defs.add(SheetMetricImportDef.single("AFTER_EMBOSSING", "压槽后", "HARDNESS", "硬度", "AE", "AF"));
        defs.add(SheetMetricImportDef.density("AFTER_EMBOSSING", "压槽后", "DENSITY", "密度", "AG", "AH", "AI", "AJ"));
        defs.add(SheetMetricImportDef.compression("AFTER_EMBOSSING", "压槽后", "COMPRESSION_RATE", "压缩率", "AK", "AL", "AN", "AP", "AR"));
        defs.add(SheetMetricImportDef.compression("AFTER_EMBOSSING", "压槽后", "COMPRESSION_ELASTICITY_RATE", "压缩弹性率", "AK", "AL", "AN", "AP", "AT"));
        defs.add(SheetMetricImportDef.single("FINAL_PRODUCTS", "成品", "THICKNESS", "厚度", "AW", "AX"));
        defs.add(SheetMetricImportDef.single("FINAL_PRODUCTS", "成品", "HARDNESS", "硬度", "AY", "AZ"));
        defs.add(SheetMetricImportDef.density("FINAL_PRODUCTS", "成品", "DENSITY", "密度", "BA", "BB", "BC", "BD"));
        defs.add(SheetMetricImportDef.compression("FINAL_PRODUCTS", "成品", "COMPRESSION_RATE", "压缩率", "BE", "BF", "BH", "BJ", "BL"));
        defs.add(SheetMetricImportDef.compression("FINAL_PRODUCTS", "成品", "COMPRESSION_ELASTICITY_RATE", "压缩弹性率", "BE", "BF", "BH", "BJ", "BN"));
        defs.add(SheetMetricImportDef.single("FINAL_PRODUCTS", "成品", "PORE_SIZE", "开孔径", "BP", "BQ"));
        defs.add(SheetMetricImportDef.single("FINAL_PRODUCTS", "成品", "POROSITY", "孔隙率", "BP", "BR"));
        return defs;
    }

    private String readString(Sheet sheet, DataFormatter formatter, int rowNumber, String column) {
        if (!StringUtils.hasText(column)) {
            return null;
        }
        Row row = sheet.getRow(rowNumber - 1);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex(column));
        String value = cell == null ? null : formatter.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal readDecimal(Sheet sheet, DataFormatter formatter, int rowNumber, String column) {
        String value = readString(sheet, formatter, rowNumber, column);
        if (!StringUtils.hasText(value) || value.startsWith("=")) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim()).longValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int columnIndex(String column) {
        int result = 0;
        for (int i = 0; i < column.length(); i++) {
            result = result * 26 + (Character.toUpperCase(column.charAt(i)) - 'A' + 1);
        }
        return result - 1;
    }

    private List<QmsFaiOrderDO> findScanCandidates(String scanCode, QmsFaiScanRespVO respVO, String sourceModule) {
        List<String> activeStatuses = List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_WAITING_QA,
                STATUS_SUSPENDED, STATUS_REWORKING);
        List<QmsFaiOrderDO> candidates = qmsFaiOrderMapper.selectListByWorkOrderNo(scanCode, activeStatuses, sourceModule);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_WORK_ORDER_NO);
            return candidates;
        }
        candidates = qmsFaiOrderMapper.selectListByProductBatchNo(scanCode, activeStatuses, sourceModule);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_PRODUCT_BATCH_NO);
            return candidates;
        }
        candidates = qmsFaiOrderMapper.selectListByMachineCode(scanCode, activeStatuses, sourceModule);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_MACHINE_CODE);
            return candidates;
        }
        candidates = qmsFaiOrderMapper.selectListByMaterialCode(scanCode, activeStatuses, sourceModule);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_MATERIAL_CODE);
            return candidates;
        }
        candidates = qmsFaiOrderMapper.selectListByProductModel(scanCode, activeStatuses, sourceModule);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_MATERIAL_CODE);
            return candidates;
        }
        candidates = qmsFaiOrderMapper.selectListBySheetTemplateCode(scanCode, activeStatuses, sourceModule);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_TEMPLATE_CODE);
            return candidates;
        }
        return Collections.emptyList();
    }

    private String normalizeSourceModule(String sourceModule) {
        return StringUtils.hasText(sourceModule) ? sourceModule.trim() : null;
    }

    private boolean matchesScanSourceModule(QmsFaiOrderDO order, String sourceModule) {
        return !StringUtils.hasText(sourceModule) || Objects.equals(sourceModule, order.getSourceModule());
    }

    private String resolveScanNotFoundMessage(String sourceModule) {
        if (SOURCE_MODULE_GLUE_BOARD_FAI.equals(sourceModule)) {
            return "未找到对应的胶板检验任务，请确认条码是否正确";
        }
        return "未找到对应的首件检验任务，请确认条码是否正确";
    }

    private String resolveScanMultipleMessage(String sourceModule) {
        if (SOURCE_MODULE_GLUE_BOARD_FAI.equals(sourceModule)) {
            return "找到多张待处理胶板检验单，请选择要填写的质检表";
        }
        return "找到多张待处理首件单，请选择要填写的质检表";
    }

    private void fillSingleScanResp(QmsFaiScanRespVO respVO, QmsFaiOrderDO order, String targetType) {
        respVO.setScanTargetType(targetType);
        respVO.setMatchedFaiId(order.getId());
        respVO.setMatchedFaiNo(order.getFaiNo());
        respVO.setMatchedStepCode(order.getCurrentStepCode());
        respVO.setCandidateCount(1);
        respVO.setRecord(getFaiResp(order.getId()));
        if (isReadonlyStatus(order.getStatus())) {
            respVO.setMatchResult(SCAN_RESULT_STATUS_BLOCKED);
            respVO.setOpenTarget(STATUS_CANCELED.equals(order.getStatus()) ? null : OPEN_TARGET_REPORT);
            respVO.setMessage(readonlyMessage(order.getStatus()));
        } else if (order.getSheetTemplateId() == null && ENTRY_LAYOUT_SHEET_GRID.equals(order.getEntryLayout())) {
            respVO.setMatchResult(SCAN_RESULT_TEMPLATE_MISSING);
            respVO.setOpenTarget(null);
            respVO.setMessage("该首件单未匹配到可用 FAI 原始记录表模板，暂不能填写");
        } else {
            respVO.setMatchResult(SCAN_RESULT_MATCHED_SINGLE);
            respVO.setOpenTarget(OPEN_TARGET_WORKBENCH);
            respVO.setMessage(StringUtils.hasText(order.getCurrentStepCode()) ? "已命中首件单，准备恢复草稿" : "已命中首件单，准备进入质检表");
        }
    }

    private boolean isReadonlyStatus(String status) {
        return STATUS_COMPLETED.equals(status) || STATUS_REJECTED.equals(status) || STATUS_CANCELED.equals(status);
    }

    private String readonlyMessage(String status) {
        if (STATUS_COMPLETED.equals(status)) {
            return "该首件检验已完成，只能查看报告";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "该首件检验已驳回，默认只读；如需重检请新建首检单";
        }
        if (STATUS_CANCELED.equals(status)) {
            return "该首件检验已取消，不能继续填写";
        }
        return "当前单据状态不允许扫码填写";
    }

    private void saveScanRecord(QmsFaiScanReqVO scanReqVO, QmsFaiScanRespVO respVO) {
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        QmsFaiScanRecordDO record = new QmsFaiScanRecordDO();
        record.setScanCode(respVO.getScanCode());
        record.setScanTargetType(defaultIfBlank(respVO.getScanTargetType(), SCAN_TARGET_UNKNOWN));
        record.setScanScene(defaultIfBlank(respVO.getScanScene(), SCAN_SCENE_LEDGER_TOOLBAR));
        record.setMatchResult(defaultIfBlank(respVO.getMatchResult(), SCAN_RESULT_NOT_FOUND));
        record.setMatchedFaiId(respVO.getMatchedFaiId());
        record.setMatchedFaiNo(respVO.getMatchedFaiNo());
        record.setMatchedFaiItemId(respVO.getMatchedFaiItemId());
        record.setMatchedStepCode(respVO.getMatchedStepCode());
        record.setCandidateCount(respVO.getCandidateCount() == null ? 0 : respVO.getCandidateCount());
        record.setCandidateIds(buildCandidateIds(respVO.getCandidates()));
        record.setBlockedReason(SCAN_RESULT_MATCHED_SINGLE.equals(respVO.getMatchResult()) || SCAN_RESULT_MATCHED_MULTIPLE.equals(respVO.getMatchResult())
                ? null : respVO.getMessage());
        record.setOpenTarget(respVO.getOpenTarget());
        record.setScanUserId(loginUserId == null ? 0L : loginUserId);
        record.setScanUserName(loginUserName);
        record.setScanTime(now);
        record.setClientType(scanReqVO.getClientType());
        record.setTerminalCode(scanReqVO.getTerminalCode());
        qmsFaiScanRecordMapper.insert(record);
        respVO.setScanRecordId(record.getId());
        respVO.setScanTime(now);
        if (respVO.getMatchedFaiId() != null) {
            QmsFaiOrderDO updateObj = new QmsFaiOrderDO();
            updateObj.setId(respVO.getMatchedFaiId());
            updateObj.setLastScanCode(respVO.getScanCode());
            updateObj.setLastScanTargetType(respVO.getScanTargetType());
            updateObj.setLastScanScene(respVO.getScanScene());
            updateObj.setLastScanTime(now);
            updateObj.setLastScanUserId(loginUserId);
            updateObj.setLastScanUserName(loginUserName);
            qmsFaiOrderMapper.updateById(updateObj);
        coaFreezePackagingService.syncCoaFreezeForFai(updateObj.getId());
        }
    }

    private String buildCandidateIds(List<QmsFaiScanRespVO.Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return JsonUtils.toJsonString(candidates.stream()
                .map(QmsFaiScanRespVO.Candidate::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private QmsFaiOrderDO validateFaiExists(Long id) {
        QmsFaiOrderDO entity = qmsFaiOrderMapper.selectById(id);
        if (entity == null) {
            throw exception(HCFAI_NOT_EXISTS);
        }
        return entity;
    }

    private void validateEditable(QmsFaiOrderDO entity) {
        if (STATUS_COMPLETED.equals(entity.getStatus())
                || STATUS_REJECTED.equals(entity.getStatus())
                || STATUS_CANCELED.equals(entity.getStatus())) {
            throw exception(HCFAI_FINISHED_LOCKED);
        }
    }

    private void validateFaiNoUnique(Long id, String faiNo) {
        QmsFaiOrderDO entity = qmsFaiOrderMapper.selectByFaiNo(faiNo, id);
        if (entity != null) {
            throw exception(HCFAI_NO_EXISTS);
        }
    }

    private void validateItems(List<QmsFaiSaveReqVO.FaiItem> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCFAI_ITEMS_EMPTY);
        }
    }

    private void validateItemAudit(List<QmsFaiSaveReqVO.FaiItem> items) {
        validateItems(items);
        for (QmsFaiSaveReqVO.FaiItem item : items) {
            if (item.getSamples() == null || filterSamples(item.getSamples(), ROLE_QA).isEmpty()) {
                throw exception(HCFAI_QA_ITEMS_NOT_COMPLETED);
            }
        }
    }

    private List<QmsFaiItemDO> resolveIncomingExistingItems(Long faiId, List<QmsFaiSaveReqVO.FaiItem> items) {
        List<QmsFaiItemDO> existingItems = qmsFaiItemMapper.selectListByFaiId(faiId);
        validateItemsExist(existingItems);
        List<QmsFaiItemDO> matchedItems = existingItems.stream()
                .filter(existingItem -> findIncomingItem(existingItem, items) != null)
                .collect(Collectors.toList());
        if (matchedItems.isEmpty()) {
            throw exception(HCFAI_ITEM_SOURCE_INVALID);
        }
        return matchedItems;
    }

    private List<QmsFaiItemDO> resolveReturnItems(Long faiId, List<QmsFaiSaveReqVO.FaiItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return resolveIncomingExistingItems(faiId, items);
    }

    private void resetReturnedItemAudit(QmsFaiOrderDO order, List<QmsFaiItemDO> returnedItems, String returnReason) {
        if (returnedItems == null || returnedItems.isEmpty()) {
            return;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        LocalDateTime now = LocalDateTime.now();
        String reason = StringUtils.hasText(returnReason) ? returnReason.trim() : "品质退回修改";
        Map<Long, QmsFaiItemDO> itemMap = returnedItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QmsFaiItemDO::getId, Function.identity(), (left, right) -> left));
        if (itemMap.isEmpty()) {
            return;
        }
        Map<String, SampleAuditGroup> groupMap = buildSampleAuditGroupMap(itemMap,
                qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(order.getId(), itemMap.keySet(), ROLE_QA));
        Map<String, QmsFaiItemGroupAuditDO> existingGroupAuditMap = selectGroupAudits(order.getId()).stream()
                .collect(Collectors.toMap(this::auditGroupCompositeKey,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        for (SampleAuditGroup group : groupMap.values()) {
            QmsFaiItemDO item = itemMap.get(group.itemId);
            saveRejectedGroupHistory(order, item, group, reason, loginUserId, loginUserName, now);
            resetRejectedGroupForRecheck(order, item, group, existingGroupAuditMap.get(auditGroupCompositeKey(group)),
                    reason, loginUserId, loginUserName, now);
        }
    }

    private void markIncomingItemInspectors(Long faiId, List<QmsFaiSaveReqVO.FaiItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<QmsFaiItemDO> existingItems = qmsFaiItemMapper.selectListByFaiId(faiId);
        validateItemsExist(existingItems);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String loginUserName = resolveLoginUserName();
        LocalDateTime now = LocalDateTime.now();
        for (QmsFaiItemDO existingItem : existingItems) {
            if (findIncomingItem(existingItem, items) == null) {
                continue;
            }
            QmsFaiItemDO itemUpdateObj = new QmsFaiItemDO();
            itemUpdateObj.setId(existingItem.getId());
            itemUpdateObj.setOperatorId(loginUserId);
            itemUpdateObj.setOperatorName(loginUserName);
            itemUpdateObj.setOperatorTime(now);
            qmsFaiItemMapper.updateById(itemUpdateObj);
        }
    }

    private void validateItemsExist(Collection<QmsFaiItemDO> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCFAI_ITEMS_EMPTY);
        }
    }

    private void ensureStandardBound(QmsFaiOrderDO order) {
        if (order == null || order.getStandardId() == null) {
            throw exception(HCFAI_STANDARD_BIND_REQUIRED);
        }
    }

    private void validateAllItemsCompleted(List<QmsFaiItemDO> items) {
        validateItemsExist(items);
        boolean hasPendingItem = items.stream().anyMatch(item ->
                !JUDGMENT_OK.equals(item.getQaResult()) && !JUDGMENT_NG.equals(item.getQaResult()));
        if (hasPendingItem) {
            throw exception(HCFAI_AUDIT_NOT_COMPLETED);
        }
    }

    private List<QmsFaiSaveReqVO.FaiSample> filterSamples(List<QmsFaiSaveReqVO.FaiSample> samples, String role) {
        if (samples == null || samples.isEmpty()) {
            return Collections.emptyList();
        }
        return samples.stream()
                .filter(sample -> role.equals(sample.getSampleRole()))
                .collect(Collectors.toList());
    }

    private String generateFaiNo() {
        return qmsNoGeneratorService.generateNo(APPLY_TYPE_FAI);
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private String generateImportBatchNo() {
        return "FAI-IMP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + (System.currentTimeMillis() % 1000);
    }

    private static class SheetMetricImportDef {
        private final String sectionCode;
        private final String sectionName;
        private final String metricCode;
        private final String metricName;
        private final String positionColumn;
        private final String thicknessColumn;
        private final String weightColumn;
        private final String t1Column;
        private final String t2Column;
        private final String t3Column;
        private final String resultColumn;
        private final int firstRow;
        private final int lastRow;

        private SheetMetricImportDef(String sectionCode, String sectionName, String metricCode, String metricName,
                                     String positionColumn, String thicknessColumn, String weightColumn,
                                     String t1Column, String t2Column, String t3Column, String resultColumn) {
            this.sectionCode = sectionCode;
            this.sectionName = sectionName;
            this.metricCode = metricCode;
            this.metricName = metricName;
            this.positionColumn = positionColumn;
            this.thicknessColumn = thicknessColumn;
            this.weightColumn = weightColumn;
            this.t1Column = t1Column;
            this.t2Column = t2Column;
            this.t3Column = t3Column;
            this.resultColumn = resultColumn;
            this.firstRow = 9;
            this.lastRow = 23;
        }

        static SheetMetricImportDef single(String sectionCode, String sectionName, String metricCode, String metricName,
                                           String positionColumn, String resultColumn) {
            return new SheetMetricImportDef(sectionCode, sectionName, metricCode, metricName,
                    positionColumn, null, null, null, null, null, resultColumn);
        }

        static SheetMetricImportDef density(String sectionCode, String sectionName, String metricCode, String metricName,
                                            String positionColumn, String thicknessColumn, String weightColumn, String resultColumn) {
            return new SheetMetricImportDef(sectionCode, sectionName, metricCode, metricName,
                    positionColumn, thicknessColumn, weightColumn, null, null, null, resultColumn);
        }

        static SheetMetricImportDef compression(String sectionCode, String sectionName, String metricCode, String metricName,
                                                String positionColumn, String t1Column, String t2Column, String t3Column, String resultColumn) {
            return new SheetMetricImportDef(sectionCode, sectionName, metricCode, metricName,
                    positionColumn, null, null, t1Column, t2Column, t3Column, resultColumn);
        }

        Map<String, Object> readRawValues(Sheet sheet, DataFormatter formatter, int rowIndex) {
            Map<String, Object> rawValues = new LinkedHashMap<>();
            putIfPresent(rawValues, "value", readDecimalValue(sheet, formatter, rowIndex, resultColumn));
            putIfPresent(rawValues, "thicknessMm", readDecimalValue(sheet, formatter, rowIndex, thicknessColumn));
            putIfPresent(rawValues, "weightG", readDecimalValue(sheet, formatter, rowIndex, weightColumn));
            putIfPresent(rawValues, "t1Mm", readDecimalValue(sheet, formatter, rowIndex, t1Column));
            putIfPresent(rawValues, "t2Mm", readDecimalValue(sheet, formatter, rowIndex, t2Column));
            putIfPresent(rawValues, "t3Mm", readDecimalValue(sheet, formatter, rowIndex, t3Column));
            if (StringUtils.hasText(thicknessColumn) || StringUtils.hasText(weightColumn)) {
                rawValues.put("diameterMm", 39);
            }
            return rawValues;
        }

        private static void putIfPresent(Map<String, Object> rawValues, String key, BigDecimal value) {
            if (value != null) {
                rawValues.put(key, value);
            }
        }

        private BigDecimal readDecimalValue(Sheet sheet, DataFormatter formatter, int rowNumber, String column) {
            if (!StringUtils.hasText(column)) {
                return null;
            }
            Row row = sheet.getRow(rowNumber - 1);
            if (row == null) {
                return null;
            }
            Cell cell = row.getCell(toColumnIndex(column));
            String value = cell == null ? null : formatter.formatCellValue(cell);
            if (!StringUtils.hasText(value) || value.startsWith("=")) {
                return null;
            }
            try {
                return new BigDecimal(value.replace(",", "").trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private int toColumnIndex(String column) {
            int result = 0;
            for (int i = 0; i < column.length(); i++) {
                result = result * 26 + (Character.toUpperCase(column.charAt(i)) - 'A' + 1);
            }
            return result - 1;
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) && !"-".equals(value) ? value : defaultValue;
    }

    private static Map<String, String> buildFaiRequiredSelfCheckFormNames() {
        Map<String, String> formNames = new LinkedHashMap<>();
        formNames.put(FAI_SELF_CHECK_ELECTRONIC_BALANCE_FORM_CODE, "电子天平日常点检表");
        formNames.put(FAI_SELF_CHECK_MICROSCOPE_FORM_CODE, "显微镜日常点检表");
        formNames.put(FAI_SELF_CHECK_HARDNESS_TESTER_FORM_CODE, "硬度计日常点检表");
        formNames.put(FAI_SELF_CHECK_TENSILE_TESTER_FORM_CODE, "拉力测试机日常点检表");
        formNames.put(FAI_SELF_CHECK_THICKNESS_GAUGE_FORM_CODE, "厚度计日常点检表");
        formNames.put(FAI_SELF_CHECK_COMPRESSION_REBOUND_FORM_CODE, "压缩回弹测试仪日常点检表");
        formNames.put(FAI_SELF_CHECK_3D_PROFILOMETER_FORM_CODE, "3D轮廓仪日常点检表");
        formNames.put(FAI_SELF_CHECK_CONTACT_ANGLE_FORM_CODE, "水接触角测量仪日常点检表");
        return Collections.unmodifiableMap(formNames);
    }

    private void assertFaiProcessSelfCheckReady(QmsFaiOrderDO order) {
        if (order == null || SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<String> missingNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : FAI_REQUIRED_SELF_CHECK_FORM_NAMES.entrySet()) {
            boolean confirmed = hcProcessFormRecordMapper.existsConfirmedByTemplateCodeAndRecordDate(
                    entry.getKey(), today);
            if (!confirmed) {
                missingNames.add(entry.getValue());
            }
        }
        if (!missingNames.isEmpty()) {
            throw exception0(HCFAI_NOT_EXISTS.getCode(),
                    "今天尚未确认" + String.join("、", missingNames) + "，不能进行首件检验录入");
        }
    }

    private String resolveFaiAuditBizType(QmsFaiOrderDO order) {
        return SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule()) ? "胶板检验" : "首件检验(FAI)";
    }

    private String resolveFaiAuditTip(QmsFaiOrderDO order) {
        if (SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())) {
            return "请进入质量管理-胶板检验，使用整单审核按钮完成审核。";
        }
        return "请进入质量管理-首件检验(FAI)，使用整单审核按钮完成审核。";
    }

    private String buildFaiAuditBizName(QmsFaiOrderDO order) {
        List<String> parts = new ArrayList<>();
        appendBizNamePart(parts, order.getProductModel());
        appendBizNamePart(parts, order.getMaterialName());
        appendBizNamePart(parts, order.getMaterialCode());
        appendBizNamePart(parts, order.getWorkOrderNo());
        appendBizNamePart(parts, order.getGlueBoardModel());
        return parts.isEmpty() ? defaultIfBlank(order.getFaiNo(), "") : String.join(" / ", parts);
    }

    private void appendBizNamePart(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private LocalDateTime resolveCreateSubmissionTime(LocalDateTime submissionTime) {
        return isInvalidSubmissionTime(submissionTime) ? LocalDateTime.now() : submissionTime;
    }

    private void normalizeDisplaySubmissionTime(QmsFaiOrderDO order) {
        if (order == null) {
            return;
        }
        order.setSubmissionTime(resolveDisplaySubmissionTime(order.getSubmissionTime(), order.getCreateTime()));
    }

    private LocalDateTime resolveDisplaySubmissionTime(LocalDateTime submissionTime, LocalDateTime createTime) {
        return isInvalidSubmissionTime(submissionTime) ? createTime : submissionTime;
    }

    private boolean isInvalidSubmissionTime(LocalDateTime submissionTime) {
        return submissionTime == null || submissionTime.isBefore(MIN_VALID_SUBMISSION_TIME);
    }

}
