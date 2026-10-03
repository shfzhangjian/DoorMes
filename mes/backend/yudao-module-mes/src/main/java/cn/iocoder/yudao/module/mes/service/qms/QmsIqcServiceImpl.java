package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionDestroyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionExpireTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsInspectionStandardSwitchLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcReturnRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcReturnRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsInspectionStandardSwitchLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_AUDIT_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_ATTACHMENT_DISABLED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_ATTACHMENT_TOO_MANY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_DATE_FUTURE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_DATE_RULE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_FINISHED_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_ITEM_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_ITEMS_NOT_COMPLETED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_RETURN_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_STANDARD_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_STANDARD_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCIQC_WAIT_CONFIRM_REQUIRED;

@Service
@Validated
public class QmsIqcServiceImpl implements QmsIqcService {

    private static final String APPLY_TYPE_IQC = "IQC";
    private static final Integer ENABLED = 1;
    private static final Integer AUDITED = 20;
    private static final String STANDARD_MATCH_MATERIAL = "MATERIAL";
    private static final String STANDARD_MATCH_UNIVERSAL = "UNIVERSAL";
    private static final int MATCH_SCORE_MATERIAL_ID = 300;
    private static final int MATCH_SCORE_MATERIAL_CODE = 200;
    private static final int MATCH_SCORE_UNIVERSAL = 100;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_WAITING_CONFIRM = "WAITING_CONFIRM";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_FINISHED_LEGACY = "FINISHED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String PROCESS_PENDING = "PENDING";
    private static final String DEFAULT_NG_FLOW = "NCR";
    private static final String AUDIT_APPROVE = "APPROVE";
    private static final String AUDIT_RETURN = "RETURN";
    private static final String SUPPLIER_STATUS_QUALIFIED = "QUALIFIED";
    private static final String IMPORT_STATUS_SUCCESS = "SUCCESS";
    private static final String IMPORT_STATUS_FAILED = "FAILED";
    private static final String RETENTION_UNCONFIRMED = "UNCONFIRMED";
    private static final String RETENTION_RETAINED = "RETAINED";
    private static final String RETENTION_NOT_RETAINED = "NOT_RETAINED";
    private static final String RETENTION_DESTROY_WAIT = "WAIT_DESTROY";
    private static final String RETENTION_DESTROYED = "DESTROYED";
    private static final String RETENTION_UNIT_MONTH = "MONTH";
    private static final int IQC_RETENTION_PERIOD_MONTHS = 13;
    private static final String RETENTION_SAMPLE_UNIT_LITER = "L";
    private static final String RETENTION_SAMPLE_UNIT_METER = "m";
    private static final int OVERVIEW_HEADER_ROW_INDEX = 5;
    private static final int OVERVIEW_DATA_START_ROW_INDEX = 6;
    private static final int OVERVIEW_SAMPLE_START_COLUMN = 10;
    private static final int LEGACY_DATA_START_ROW_INDEX = 2;
    private static final String OVERVIEW_TEMPLATE_TITLE = "IQC检验项明细概览";
    private static final String COA_WORD_TITLE = "进料检验报告详情 (COA)";
    private static final int COA_PAGE_WIDTH_TWIPS = 11906;
    private static final int COA_PAGE_HEIGHT_TWIPS = 16838;
    private static final int COA_PAGE_MARGIN_TWIPS = 850;
    private static final int COA_TABLE_WIDTH_TWIPS = COA_PAGE_WIDTH_TWIPS - COA_PAGE_MARGIN_TWIPS * 2;
    private static final int[] COA_DESCRIPTION_COLUMN_WIDTHS = {
            1650, 1752, 1650, 1752, 1650, 1752
    };
    private static final int[] COA_ITEM_COLUMN_WIDTHS = {
            606, 1415, 707, 1617, 1213, 1011, 808, 2021, 808
    };
    private static final int[] COA_SIGNATURE_COLUMN_WIDTHS = {
            3402, 3402, 3402
    };
    private static final String COA_LABEL_FILL = "F8FAFC";
    private static final List<String> COA_FOOTER_META_ITEMS = List.of(
            "制定/修订部门：材料事业部品质",
            "制定日期：2018.1.10",
            "修订日期：2025.10.16",
            "保管期限：10年");
    private static final String COA_FOOTER_NOTICE =
            "本资料为安徽禾臣新材料有限公司专有财产，非经许可，不得复制翻印或转交成其它形式使用";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SCAN_SCENE_LEDGER_TOOLBAR = "LEDGER_TOOLBAR";
    private static final String SCAN_TARGET_UNKNOWN = "UNKNOWN";
    private static final String SCAN_TARGET_IQC_NO = "IQC_NO";
    private static final String SCAN_TARGET_RECEIPT_NO = "RECEIPT_NO";
    private static final String SCAN_TARGET_BATCH_NO = "BATCH_NO";
    private static final String SCAN_TARGET_MATERIAL_CODE = "MATERIAL_CODE";
    private static final String SCAN_RESULT_NOT_FOUND = "NOT_FOUND";
    private static final String SCAN_RESULT_MATCHED_SINGLE = "MATCHED_SINGLE";
    private static final String SCAN_RESULT_MATCHED_MULTIPLE = "MATCHED_MULTIPLE";
    private static final String SCAN_RESULT_STATUS_BLOCKED = "STATUS_BLOCKED";
    private static final String OPEN_TARGET_WORKBENCH = "WORKBENCH";
    private static final String OPEN_TARGET_REPORT = "REPORT";
    private static final String OPEN_TARGET_CANDIDATE_MODAL = "CANDIDATE_MODAL";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_DATE = "DATE";
    private static final String TEMPLATE_DENSITY_CALC = "DENSITY_CALC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final BigDecimal DEFAULT_SAMPLE_DIAMETER_MM = BigDecimal.valueOf(39);
    private static final BigDecimal PI = BigDecimal.valueOf(3.14);
    private static final int CALC_SCALE = 6;
    private static final int MAX_ITEM_ATTACHMENT_COUNT = 10;
    private static final int MAX_INSPECTION_APPLY_ATTACHMENT_COUNT = 10;
    private static final int MAX_INSPECTION_APPLY_ATTACHMENT_URL_LENGTH = 2048;
    private static final Set<String> INSPECTION_APPLY_ATTACHMENT_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "pdf", "doc", "docx", "xls", "xlsx", "csv", "txt", "zip");
    private static final ZoneId BUSINESS_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private record IqcStandardCandidateMatch(QmsQualityStandardDO standard, int score, String matchType,
                                             String matchReason) {
    }

    private record RetentionSampleRule(String materialCategoryName, BigDecimal qty, String unit) {
        String description() {
            return qty.stripTrailingZeros().toPlainString() + unit + "/每来料批次";
        }
    }

    @Resource
    private QmsIqcOrderMapper qmsIqcOrderMapper;
    @Resource
    private QmsIqcItemMapper qmsIqcItemMapper;
    @Resource
    private QmsIqcSampleMapper qmsIqcSampleMapper;
    @Resource
    private QmsIqcAbnormalMapper qmsIqcAbnormalMapper;
    @Resource
    private QmsIqcReturnRecordMapper qmsIqcReturnRecordMapper;
    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    private QmsInspectionStandardSwitchLogMapper qmsInspectionStandardSwitchLogMapper;
    @Resource
    private QmsQualityStandardContentHashService qmsQualityStandardContentHashService;
    @Resource
    private QmsIqcItemWorkbookService qmsIqcItemWorkbookService;
    @Resource
    private MesSupplierMapper mesSupplierMapper;
    @Resource
    private HcMaterialMapper hcMaterialMapper;
    @Resource
    private QmsAuditTodoNotifyService qmsAuditTodoNotifyService;
    @Resource
    private QmsNoGeneratorService qmsNoGeneratorService;
    @Resource
    @Lazy
    private QmsDispatchTaskService qmsDispatchTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createIqc(QmsIqcSaveReqVO createReqVO) {
        String iqcNo = generateIqcNo();
        validateIqcNoUnique(null, iqcNo);
        validateIqcMaterialTraceFields(createReqVO);
        applyIqcSupplierSnapshot(createReqVO);
        applyInspectionApplyAttachmentUrls(createReqVO);

        QmsQualityStandardDO standard = null;
        List<QmsQualityStandardItemDO> standardItems = Collections.emptyList();
        IqcStandardCandidateMatch selectedMatch = null;
        String pendingStandardReason = null;
        if (createReqVO.getStandardId() != null) {
            standard = qmsQualityStandardMapper.selectById(createReqVO.getStandardId());
            validateIqcStandardUsable(standard);
            selectedMatch = matchIqcStandardForManualSelection(standard, createReqVO);
            standardItems = selectIqcStandardItems(standard.getId());
        } else {
            List<IqcStandardCandidateMatch> candidates = findIqcStandardCandidateMatches(createReqVO);
            selectedMatch = selectUniqueHighestPriorityMatch(candidates);
            if (selectedMatch != null) {
                standard = selectedMatch.standard();
                standardItems = selectIqcStandardItems(standard.getId());
            } else {
                pendingStandardReason = candidates.isEmpty()
                        ? "未找到匹配且包含检验项目的已审核标准"
                        : "匹配到多个同优先级已审核标准，请在开始检验前选择";
            }
        }
        QmsIqcOrderDO entity = BeanUtils.toBean(createReqVO, QmsIqcOrderDO.class);
        entity.setId(null);
        entity.setIqcNo(iqcNo);
        entity.setReceiptNo(defaultIfBlank(createReqVO.getReceiptNo(), iqcNo));
        entity.setBatchNo(createReqVO.getBatchNo().trim());
        entity.setInspectionApplyTime(createReqVO.getInspectionApplyTime() == null
                ? LocalDateTime.now() : createReqVO.getInspectionApplyTime());
        if (standard != null) {
            applyIqcStandardSnapshot(entity, standard, selectedMatch.matchType());
            entity.setStandardSnapshotHash(
                    qmsQualityStandardContentHashService.hashIqcStandardItems(standardItems));
        } else {
            clearIqcStandardSnapshot(entity);
            entity.setRemark(appendRemark(entity.getRemark(),
                    "待选择检验标准：" + pendingStandardReason));
        }
        entity.setStatus(defaultIfBlank(createReqVO.getStatus(), STATUS_PENDING));
        entity.setJudgment(defaultIfBlank(createReqVO.getJudgment(), JUDGMENT_PENDING));
        entity.setRetentionStatus(defaultIfBlank(createReqVO.getRetentionStatus(), RETENTION_UNCONFIRMED));
        applyRetentionConfirmation(entity, createReqVO.getRetentionStatus(), entity);
        qmsIqcOrderMapper.insert(entity);

        if (!standardItems.isEmpty()) {
            saveIqcStandardSnapshotDetails(entity, standardItems, createReqVO.getAbnormals());
        } else {
            saveAbnormals(entity, createReqVO.getAbnormals());
        }
        return entity.getId();
    }

    @Override
    public List<QmsInspectionStandardCandidateRespVO> getStandardCandidates(Long id) {
        QmsIqcOrderDO order = validateIqcExists(id);
        QmsIqcSaveReqVO scope = BeanUtils.toBean(order, QmsIqcSaveReqVO.class);
        return buildIqcStandardCandidateRespList(scope);
    }

    @Override
    public List<QmsInspectionStandardCandidateRespVO> getStandardCandidatesByMaterial(Long materialId,
                                                                                      String materialCode) {
        QmsIqcSaveReqVO scope = new QmsIqcSaveReqVO();
        scope.setMaterialId(materialId);
        scope.setMaterialCode(materialCode);
        return buildIqcStandardCandidateRespList(scope);
    }

    private List<QmsInspectionStandardCandidateRespVO> buildIqcStandardCandidateRespList(QmsIqcSaveReqVO scope) {
        List<IqcStandardCandidateMatch> matches = findIqcStandardCandidateMatches(scope);
        IqcStandardCandidateMatch recommended = selectUniqueHighestPriorityMatch(matches);
        return matches.stream()
                .map(match -> buildStandardCandidate(match,
                        recommended != null && Objects.equals(recommended.standard().getId(), match.standard().getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO selectStandard(QmsInspectionStandardSelectReqVO reqVO) {
        QmsIqcOrderDO order = validateIqcExists(reqVO.getId());
        validateIqcStandardSwitchAllowed(order);
        QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(reqVO.getStandardId());
        validateIqcStandardUsable(standard);
        QmsIqcSaveReqVO scope = BeanUtils.toBean(order, QmsIqcSaveReqVO.class);
        IqcStandardCandidateMatch selectedMatch = matchIqcStandardForManualSelection(standard, scope);
        List<QmsQualityStandardItemDO> standardItems = selectIqcStandardItems(standard.getId());
        List<QmsIqcItemDO> oldItems = qmsIqcItemMapper.selectListByIqcId(order.getId());
        String oldHash = StringUtils.hasText(order.getStandardSnapshotHash())
                ? order.getStandardSnapshotHash()
                : qmsQualityStandardContentHashService.hashIqcSnapshotItems(oldItems);
        String newHash = qmsQualityStandardContentHashService.hashIqcStandardItems(standardItems);
        String oldSnapshotJson = buildIqcSwitchSnapshot(order, oldItems);

        qmsIqcSampleMapper.deleteByIqcId(order.getId());
        qmsIqcAbnormalMapper.deleteByIqcId(order.getId());
        qmsIqcReturnRecordMapper.deleteByIqcId(order.getId());
        qmsIqcItemMapper.deleteByIqcId(order.getId());

        LocalDateTime now = LocalDateTime.now();
        qmsIqcOrderMapper.update(null, new LambdaUpdateWrapper<QmsIqcOrderDO>()
                .eq(QmsIqcOrderDO::getId, order.getId())
                .set(QmsIqcOrderDO::getStandardId, standard.getId())
                .set(QmsIqcOrderDO::getStandardNo, standard.getStandardNo())
                .set(QmsIqcOrderDO::getStandardName, standard.getStandardName())
                .set(QmsIqcOrderDO::getStandardVersion, standard.getVersion())
                .set(QmsIqcOrderDO::getStandardSnapshotTime, now)
                .set(QmsIqcOrderDO::getStandardMatchMode, selectedMatch.matchType())
                .set(QmsIqcOrderDO::getStandardSnapshotLocked, true)
                .set(QmsIqcOrderDO::getStandardSnapshotHash, newHash)
                .set(QmsIqcOrderDO::getStatus, STATUS_PENDING)
                .set(QmsIqcOrderDO::getJudgment, JUDGMENT_PENDING)
                .set(QmsIqcOrderDO::getInspectorId, null)
                .set(QmsIqcOrderDO::getInspectorName, null)
                .set(QmsIqcOrderDO::getInspectionTime, null)
                .set(QmsIqcOrderDO::getQaInspectorId, null)
                .set(QmsIqcOrderDO::getQaInspectorName, null)
                .set(QmsIqcOrderDO::getQaTime, null)
                .set(QmsIqcOrderDO::getAuditNotifyTime, null)
                .set(QmsIqcOrderDO::getDisposalType, null)
                .set(QmsIqcOrderDO::getReturnCount, 0)
                .set(QmsIqcOrderDO::getLastReturnReason, null));

        QmsIqcOrderDO refreshed = validateIqcExists(order.getId());
        saveIqcStandardSnapshotDetails(refreshed, standardItems, null);
        saveStandardSwitchLog(APPLY_TYPE_IQC, order.getId(), order.getIqcNo(),
                order.getStandardId(), order.getStandardNo(), oldHash,
                standard.getId(), standard.getStandardNo(), newHash,
                oldSnapshotJson, reqVO.getReason(), order.getTenantId(), now);
        return getIqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIqc(QmsIqcSaveReqVO updateReqVO) {
        QmsIqcOrderDO old = validateIqcExists(updateReqVO.getId());
        validateEditable(old);
        validateIqcMaterialTraceFields(updateReqVO);
        applyIqcSupplierSnapshot(updateReqVO);
        applyInspectionApplyAttachmentUrls(updateReqVO);
        QmsIqcOrderDO updateObj = BeanUtils.toBean(updateReqVO, QmsIqcOrderDO.class);
        updateObj.setIqcNo(old.getIqcNo());
        updateObj.setBatchNo(updateReqVO.getBatchNo().trim());
        updateObj.setStatus(defaultIfBlank(updateReqVO.getStatus(), old.getStatus()));
        updateObj.setJudgment(defaultIfBlank(updateReqVO.getJudgment(), old.getJudgment()));
        updateObj.setRetentionStatus(defaultIfBlank(updateReqVO.getRetentionStatus(), old.getRetentionStatus()));
        applyRetentionConfirmation(updateObj, updateReqVO.getRetentionStatus(), old);
        applyExistingStandardSnapshot(updateObj, old);
        qmsIqcOrderMapper.updateById(updateObj);

        updateExecutionDetails(old, updateReqVO.getItems(), updateReqVO.getAbnormals());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIqc(Long id) {
        validateIqcExists(id);
        qmsDispatchTaskService.cancelForDeletedIqc(id);
        qmsIqcSampleMapper.deleteByIqcId(id);
        qmsIqcAbnormalMapper.deleteByIqcId(id);
        qmsIqcReturnRecordMapper.deleteByIqcId(id);
        qmsIqcItemMapper.deleteByIqcId(id);
        qmsIqcOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIqcList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::validateIqcExists);
        ids.forEach(qmsDispatchTaskService::cancelForDeletedIqc);
        qmsIqcSampleMapper.deleteByIqcIds(ids);
        qmsIqcAbnormalMapper.deleteByIqcIds(ids);
        qmsIqcReturnRecordMapper.deleteByIqcIds(ids);
        qmsIqcItemMapper.deleteByIqcIds(ids);
        qmsIqcOrderMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitIqc(QmsIqcSubmitReqVO submitReqVO) {
        QmsIqcOrderDO entity = validateIqcExists(submitReqVO.getId());
        validateEditable(entity);
        if (submitReqVO.getItems() != null && !submitReqVO.getItems().isEmpty()) {
            updateExecutionDetails(entity, submitReqVO.getItems(), submitReqVO.getAbnormals());
        }

        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(entity.getId());
        recalculateStoredItems(entity.getId(), items);
        String finalJudgment = calculateFinalJudgment(items);

        LocalDateTime submitTime = LocalDateTime.now();
        QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
        updateObj.setId(entity.getId());
        updateObj.setStatus(STATUS_WAITING_CONFIRM);
        updateObj.setJudgment(finalJudgment);
        updateObj.setInspectionTime(submitTime);
        updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setInspectorName(resolveLoginUserName());
        updateObj.setDisposalType(defaultIfBlank(submitReqVO.getDisposalType(), JUDGMENT_NG.equals(finalJudgment) ? DEFAULT_NG_FLOW : null));
        updateObj.setRemark(StringUtils.hasText(submitReqVO.getRemark()) ? submitReqVO.getRemark() : entity.getRemark());
        applyRetentionConfirmation(updateObj, defaultIfBlank(submitReqVO.getRetentionStatus(), RETENTION_NOT_RETAINED), entity);
        qmsIqcOrderMapper.updateById(updateObj);
        qmsAuditTodoNotifyService.sendAuditTodoIfNeeded(entity.getAuditNotifyTime(),
                "进料检验单(IQC)",
                entity.getIqcNo(),
                buildIqcAuditBizName(entity),
                "请进入质量管理-进料检验单(IQC)，使用审核确认按钮完成审核。",
                submitTime,
                time -> qmsIqcOrderMapper.updateAuditNotifyTime(entity.getId(), time));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditIqc(QmsIqcAuditReqVO auditReqVO) {
        QmsIqcOrderDO entity = validateIqcExists(auditReqVO.getId());
        if (!STATUS_WAITING_CONFIRM.equals(entity.getStatus())) {
            throw exception(HCIQC_WAIT_CONFIRM_REQUIRED);
        }

        String auditResult = auditReqVO.getAuditResult().trim();
        if (AUDIT_RETURN.equals(auditResult)) {
            String returnReason = auditReqVO.getAuditRemark();
            if (!StringUtils.hasText(returnReason)) {
                throw exception(HCIQC_RETURN_REASON_REQUIRED);
            }
            LocalDateTime now = LocalDateTime.now();
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            String loginUserName = resolveLoginUserName();
            String trimmedReason = returnReason.trim();
            QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
            updateObj.setId(entity.getId());
            updateObj.setStatus(STATUS_INSPECTING);
            updateObj.setQaInspectorId(loginUserId);
            updateObj.setQaInspectorName(loginUserName);
            updateObj.setQaTime(now);
            updateObj.setReturnCount((entity.getReturnCount() == null ? 0 : entity.getReturnCount()) + 1);
            updateObj.setLastReturnReason(trimmedReason);
            updateObj.setRemark(trimmedReason);
            qmsIqcOrderMapper.updateById(updateObj);
            qmsIqcOrderMapper.clearAuditNotifyTime(entity.getId());
            qmsIqcReturnRecordMapper.insert(QmsIqcReturnRecordDO.builder()
                    .iqcId(entity.getId())
                    .iqcNo(entity.getIqcNo())
                    .returnReason(trimmedReason)
                    .returnUserId(loginUserId)
                    .returnUserName(loginUserName)
                    .returnTime(now)
                    .beforeStatus(entity.getStatus())
                    .afterStatus(STATUS_INSPECTING)
                    .build());
            return;
        }
        if (!AUDIT_APPROVE.equals(auditResult)) {
            throw exception(HCIQC_AUDIT_RESULT_INVALID);
        }

        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(entity.getId());
        recalculateStoredItems(entity.getId(), items);
        String finalJudgment = calculateFinalJudgment(items);
        QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
        updateObj.setId(entity.getId());
        updateObj.setStatus(JUDGMENT_NG.equals(finalJudgment) ? STATUS_REJECTED : STATUS_COMPLETED);
        updateObj.setJudgment(finalJudgment);
        updateObj.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setQaInspectorName(resolveLoginUserName());
        updateObj.setQaTime(LocalDateTime.now());
        updateObj.setDisposalType(defaultIfBlank(entity.getDisposalType(), JUDGMENT_NG.equals(finalJudgment) ? DEFAULT_NG_FLOW : null));
        updateObj.setRemark(StringUtils.hasText(auditReqVO.getAuditRemark()) ? auditReqVO.getAuditRemark() : entity.getRemark());
        qmsIqcOrderMapper.updateById(updateObj);
        qmsIqcOrderMapper.clearAuditNotifyTime(entity.getId());

        if (JUDGMENT_NG.equals(finalJudgment)) {
            ensureNgAbnormal(entity.getId(), entity.getIqcNo(), items, updateObj.getDisposalType());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO saveProgramEntry(QmsIqcSaveReqVO saveReqVO) {
        QmsIqcOrderDO order = validateIqcExists(saveReqVO.getId());
        validateEditable(order);
        updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        updateProgramEntryStatus(order.getId(), STATUS_INSPECTING, calculateCurrentJudgment(order.getId()), saveReqVO.getRemark());
        return getIqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO recalculateProgramEntry(QmsIqcSaveReqVO saveReqVO) {
        QmsIqcOrderDO order = validateIqcExists(saveReqVO.getId());
        validateEditable(order);
        updateExecutionDetails(order, saveReqVO.getItems(), saveReqVO.getAbnormals());
        updateProgramEntryStatus(order.getId(), STATUS_INSPECTING, calculateCurrentJudgment(order.getId()), saveReqVO.getRemark());
        return getIqcResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO submitProgramEntry(QmsIqcSaveReqVO saveReqVO) {
        QmsIqcSubmitReqVO submitReqVO = new QmsIqcSubmitReqVO();
        submitReqVO.setId(saveReqVO.getId());
        submitReqVO.setDisposalType(saveReqVO.getDisposalType());
        submitReqVO.setRemark(saveReqVO.getRemark());
        submitReqVO.setRetentionStatus(saveReqVO.getRetentionStatus());
        submitReqVO.setItems(saveReqVO.getItems());
        submitReqVO.setAbnormals(saveReqVO.getAbnormals());
        submitIqc(submitReqVO);
        return getIqcResp(saveReqVO.getId());
    }

    @Override
    public QmsIqcScanRespVO resolveScan(QmsIqcScanReqVO scanReqVO) {
        String scanCode = scanReqVO.getScanCode().trim();
        QmsIqcScanRespVO respVO = new QmsIqcScanRespVO();
        respVO.setScanCode(scanCode);
        respVO.setScanScene(defaultIfBlank(scanReqVO.getScanScene(), SCAN_SCENE_LEDGER_TOOLBAR));
        respVO.setScanTargetType(SCAN_TARGET_UNKNOWN);
        respVO.setMatchResult(SCAN_RESULT_NOT_FOUND);
        respVO.setCandidateCount(0);
        respVO.setScanTime(LocalDateTime.now());
        respVO.setMessage("未找到对应的进料检验单，请确认 IQC 单号、收料单号、批号或物料编码是否正确");

        QmsIqcOrderDO byIqcNo = qmsIqcOrderMapper.selectByIqcNo(scanCode, null);
        if (byIqcNo != null) {
            fillSingleScanResp(respVO, byIqcNo, SCAN_TARGET_IQC_NO);
            return respVO;
        }

        List<QmsIqcOrderDO> candidates = findScanCandidates(scanCode, respVO);
        if (candidates.size() == 1) {
            fillSingleScanResp(respVO, candidates.get(0), respVO.getScanTargetType());
        } else if (candidates.size() > 1) {
            respVO.setMatchResult(SCAN_RESULT_MATCHED_MULTIPLE);
            respVO.setOpenTarget(OPEN_TARGET_CANDIDATE_MODAL);
            respVO.setCandidateCount(candidates.size());
            respVO.setCandidates(BeanUtils.toBean(candidates, QmsIqcScanRespVO.Candidate.class));
            respVO.setMessage("找到多张进料检验单，请选择要填写的质检表");
        }
        return respVO;
    }

    @Override
    public byte[] buildItemImportTemplateExcel(Long id) throws IOException {
        QmsIqcOrderDO order = validateIqcExists(id);
        validateEditable(order);
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(id);
        validateItemsExist(items);
        return qmsIqcItemWorkbookService.buildWorkbook(order, items, Collections.emptyMap());
    }

    @Override
    public byte[] buildItemExportExcel(Long id) throws IOException {
        QmsIqcOrderDO order = validateIqcExists(id);
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(id);
        validateItemsExist(items);
        return qmsIqcItemWorkbookService.buildWorkbook(order, items, loadCurrentSampleDoMap(id));
    }

    @Override
    public byte[] buildCoaWord(Long id) throws IOException {
        QmsIqcRespVO record = getIqcResp(id);
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            applyCoaPageLayout(document);
            addCoaTitle(document);
            addCoaBasicInfo(document, record);
            addCoaItems(document, record.getItems());
            addCoaSignature(document, record);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @Override
    public QmsIqcImportRespVO previewItemImport(Long id, MultipartFile file) throws IOException {
        if (qmsIqcItemWorkbookService.supports(file)) {
            return buildDynamicItemImportPlan(id, file, true, false).toResp(null);
        }
        return buildItemImportPlan(id, file, true, false).toResp(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcImportRespVO confirmItemImport(Long id, Boolean allowOverwrite, MultipartFile file) throws IOException {
        if (qmsIqcItemWorkbookService.supports(file)) {
            return confirmDynamicItemImport(id, Boolean.TRUE.equals(allowOverwrite), file);
        }
        ItemImportPlan plan = buildItemImportPlan(id, file, false, Boolean.TRUE.equals(allowOverwrite));
        if (plan.failureCount > 0 || plan.validRows.isEmpty()) {
            return plan.toResp(null);
        }

        QmsIqcOrderDO order = validateIqcExists(id);
        validateEditable(order);
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(id);
        Map<Long, QmsIqcItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsIqcItemDO::getId, item -> item));
        Map<Long, Map<Integer, QmsIqcSaveReqVO.IqcSample>> sampleMap = loadCurrentSampleMap(id);
        for (ItemImportRow row : plan.validRows) {
            sampleMap.computeIfAbsent(row.itemId, key -> new HashMap<>()).put(row.sampleSeq, row.toSample());
        }

        List<QmsIqcSaveReqVO.IqcItem> importItems = sampleMap.entrySet().stream()
                .filter(entry -> plan.affectedItemIds.contains(entry.getKey()))
                .map(entry -> buildImportItem(itemMap.get(entry.getKey()), entry.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        updateExecutionDetails(order, importItems, null);
        updateProgramEntryStatus(order.getId(), STATUS_INSPECTING, calculateCurrentJudgment(order.getId()), null);
        return plan.toResp(getIqcResp(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendIqc(Long id) {
        QmsIqcOrderDO entity = validateIqcExists(id);
        validateEditable(entity);
        QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SUSPENDED);
        qmsIqcOrderMapper.updateById(updateObj);
    }

    @Override
    public QmsIqcOrderDO getIqc(Long id) {
        return validateIqcExists(id);
    }

    @Override
    public QmsIqcRespVO getIqcResp(Long id) {
        QmsIqcOrderDO entity = validateIqcExists(id);
        QmsIqcRespVO respVO = BeanUtils.toBean(entity, QmsIqcRespVO.class);
        fillDetails(respVO);
        return respVO;
    }

    @Override
    public PageResult<QmsIqcOrderDO> getIqcPage(QmsIqcPageReqVO pageReqVO) {
        return qmsIqcOrderMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO getIqcRetentionResp(Long id) {
        QmsIqcRespVO respVO = getIqcResp(id);
        if (!RETENTION_RETAINED.equals(respVO.getRetentionStatus())) {
            throw exception(HCIQC_NOT_EXISTS);
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO confirmIqcRetention(QmsIqcRetentionConfirmReqVO reqVO) {
        QmsIqcOrderDO order = validateIqcExists(reqVO.getId());
        if (RETENTION_RETAINED.equals(order.getRetentionStatus())) {
            return getIqcRetentionResp(order.getId());
        }
        if (RETENTION_DESTROYED.equals(order.getRetentionDestroyStatus())) {
            throw invalidParamException("已销毁留样不允许重复确认");
        }
        QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
        updateObj.setId(order.getId());
        applyRetentionConfirmation(updateObj, RETENTION_RETAINED, order);
        qmsIqcOrderMapper.updateById(updateObj);
        return getIqcRetentionResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsIqcRespVO updateIqcRetentionExpireTime(QmsIqcRetentionExpireTimeReqVO reqVO) {
        QmsIqcOrderDO order = validateIqcRetentionRecord(reqVO.getId());
        if (RETENTION_DESTROYED.equals(order.getRetentionDestroyStatus())) {
            throw invalidParamException("已销毁留样不允许修改过期时间");
        }
        QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
        updateObj.setId(order.getId());
        updateObj.setRetentionExpireTime(reqVO.getRetentionExpireTime());
        updateObj.setRetentionDestroyStatus(defaultIfBlank(order.getRetentionDestroyStatus(), RETENTION_DESTROY_WAIT));
        qmsIqcOrderMapper.updateById(updateObj);
        return getIqcRetentionResp(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void destroyIqcRetention(QmsIqcRetentionDestroyReqVO reqVO) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        List<Long> ids = reqVO.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = resolveLoginUserName();
        for (Long id : ids) {
            QmsIqcOrderDO order = validateIqcRetentionRecord(id);
            if (RETENTION_DESTROYED.equals(order.getRetentionDestroyStatus())) {
                throw invalidParamException("留样记录【" + order.getIqcNo() + "】已销毁");
            }
            if (order.getRetentionExpireTime() == null
                    || !order.getRetentionExpireTime().isBefore(tomorrowStart)) {
                throw invalidParamException("留样记录【" + order.getIqcNo() + "】尚未过期，不能销毁");
            }
            QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
            updateObj.setId(order.getId());
            updateObj.setRetentionDestroyStatus(RETENTION_DESTROYED);
            updateObj.setRetentionDestroyTime(now);
            updateObj.setRetentionDestroyUserId(userId);
            updateObj.setRetentionDestroyUserName(userName);
            updateObj.setRetentionDestroyRemark(defaultIfBlank(reqVO.getRemark(), ""));
            qmsIqcOrderMapper.updateById(updateObj);
        }
    }

    @Override
    public List<QmsIqcRespVO> getPendingIqcList() {
        List<QmsIqcOrderDO> list = qmsIqcOrderMapper.selectListByStatuses(
                List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_SUSPENDED));
        return BeanUtils.toBean(list, QmsIqcRespVO.class);
    }

    private List<QmsIqcOrderDO> findScanCandidates(String scanCode, QmsIqcScanRespVO respVO) {
        List<String> statuses = List.of(STATUS_PENDING, STATUS_INSPECTING, STATUS_SUSPENDED);
        List<QmsIqcOrderDO> candidates = qmsIqcOrderMapper.selectListByReceiptNo(scanCode, statuses);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_RECEIPT_NO);
            return candidates;
        }
        candidates = qmsIqcOrderMapper.selectListByBatchNo(scanCode, statuses);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_BATCH_NO);
            return candidates;
        }
        candidates = qmsIqcOrderMapper.selectListByMaterialCode(scanCode, statuses);
        if (!candidates.isEmpty()) {
            respVO.setScanTargetType(SCAN_TARGET_MATERIAL_CODE);
            return candidates;
        }
        return Collections.emptyList();
    }

    private void fillSingleScanResp(QmsIqcScanRespVO respVO, QmsIqcOrderDO order, String targetType) {
        respVO.setScanTargetType(targetType);
        respVO.setMatchedIqcId(order.getId());
        respVO.setMatchedIqcNo(order.getIqcNo());
        respVO.setCandidateCount(1);
        respVO.setRecord(getIqcResp(order.getId()));
        if (isReadonlyStatus(order.getStatus())) {
            respVO.setMatchResult(SCAN_RESULT_STATUS_BLOCKED);
            respVO.setOpenTarget(STATUS_CANCELED.equals(order.getStatus()) ? null : OPEN_TARGET_REPORT);
            respVO.setMessage(readonlyMessage(order.getStatus()));
        } else {
            respVO.setMatchResult(SCAN_RESULT_MATCHED_SINGLE);
            respVO.setOpenTarget(OPEN_TARGET_WORKBENCH);
            respVO.setMessage("已定位到进料检验单：" + order.getIqcNo());
        }
    }

    private boolean isReadonlyStatus(String status) {
        return STATUS_COMPLETED.equals(status)
                || STATUS_REJECTED.equals(status)
                || STATUS_WAITING_CONFIRM.equals(status)
                || STATUS_CANCELED.equals(status)
                || STATUS_FINISHED_LEGACY.equals(status);
    }

    private String readonlyMessage(String status) {
        if (STATUS_CANCELED.equals(status)) {
            return "该进料检验单已取消，不能继续录入";
        }
        if (STATUS_WAITING_CONFIRM.equals(status)) {
            return "该进料检验单已提交审核，只能查看报告";
        }
        return "该进料检验单已封单，只能查看报告";
    }

    @Override
    public QmsIqcStandardRespVO getIqcStandardByMaterial(String materialCode) {
        QmsIqcSaveReqVO reqVO = new QmsIqcSaveReqVO();
        reqVO.setMaterialCode(materialCode);
        IqcStandardCandidateMatch selectedMatch = selectIqcStandardCandidate(reqVO);
        QmsQualityStandardDO standard = selectedMatch.standard();
        List<QmsQualityStandardItemDO> standardItems = selectIqcStandardItems(standard.getId());
        QmsIqcStandardRespVO respVO = new QmsIqcStandardRespVO();
        respVO.setStandardId(standard.getId());
        respVO.setStandardNo(standard.getStandardNo());
        respVO.setStandardName(standard.getStandardName());
        respVO.setVersion(standard.getVersion());
        respVO.setApplyType(standard.getApplyType());
        respVO.setStandardMatchMode(selectedMatch.matchType());
        respVO.setMaterialId(standard.getMaterialId());
        respVO.setMaterialCode(standard.getMaterialCode());
        respVO.setMaterialName(standard.getMaterialName());
        respVO.setSpecification(standard.getSpecification());
        respVO.setProductModelId(standard.getProductModelId());
        respVO.setProductModelCode(standard.getProductModelCode());
        respVO.setProductModelName(standard.getProductModelName());
        respVO.setItems(standardItems.stream()
                .map(this::buildStandardItem)
                .collect(Collectors.toList()));
        return respVO;
    }

    private void updateProgramEntryStatus(Long id, String status, String judgment, String remark) {
        QmsIqcOrderDO updateObj = new QmsIqcOrderDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        updateObj.setJudgment(judgment);
        if (StringUtils.hasText(remark)) {
            updateObj.setRemark(remark);
        }
        qmsIqcOrderMapper.updateById(updateObj);
    }

    private String calculateCurrentJudgment(Long iqcId) {
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(iqcId);
        validateItemsExist(items);
        boolean hasPending = false;
        boolean hasJudged = false;
        for (QmsIqcItemDO item : items) {
            if (QmsIqcQuantitativeJudgment.SKIP.equals(item.getItemResult())) {
                continue;
            }
            hasJudged = true;
            if (JUDGMENT_NG.equals(item.getItemResult())) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(item.getItemResult())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : (hasJudged ? JUDGMENT_OK : QmsIqcQuantitativeJudgment.SKIP);
    }

    private byte[] buildItemWorkbook(QmsIqcOrderDO order, boolean includeValues) throws IOException {
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(order.getId());
        validateItemsExist(items);
        Map<String, QmsIqcSampleDO> sampleMap = qmsIqcSampleMapper.selectListByIqcId(order.getId()).stream()
                .collect(Collectors.toMap(sample -> sampleKey(sample.getIqcItemId(), sample.getSampleSeq()),
                        sample -> sample, (first, ignored) -> first));
        int maxSampleSize = resolveMaxSampleSize(items);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("IQC检验项明细");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle lockedStyle = createLockedStyle(workbook);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(OVERVIEW_TEMPLATE_TITLE + (includeValues ? "导出" : "导入模板"));
            titleRow.getCell(0).setCellStyle(titleStyle);
            writeInfoRow(sheet.createRow(1), "IQC ID", order.getId(), "IQC单号", order.getIqcNo());
            writeInfoRow(sheet.createRow(2), "收料单号", order.getReceiptNo(), "供应商", order.getSupplierName());
            writeInfoRow(sheet.createRow(3), "物料", defaultIfBlank(order.getMaterialCode(), "-") + " / "
                    + defaultIfBlank(order.getMaterialName(), "-"), "批号", order.getBatchNo());
            writeInfoRow(sheet.createRow(4), "检验标准", defaultIfBlank(order.getStandardNo(), "-") + " / "
                    + defaultIfBlank(order.getStandardVersion(), "-"), "取样最大列数", maxSampleSize);

            Row header = sheet.createRow(OVERVIEW_HEADER_ROW_INDEX);
            List<String> headers = new ArrayList<>();
            Collections.addAll(headers, "序号", "IQC ID", "IQC单号", "检验项ID", "检验项目", "项目类型", "单位",
                    "标准要求", "检验方法", "取样数");
            for (int seq = 1; seq <= maxSampleSize; seq++) {
                headers.add("实测" + seq);
            }
            headers.add("判定");
            headers.add("备注");
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = OVERVIEW_DATA_START_ROW_INDEX;
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                QmsIqcItemDO item = items.get(itemIndex);
                int sampleSize = item.getSampleSize() == null || item.getSampleSize() <= 0 ? 1 : item.getSampleSize();
                Row row = sheet.createRow(rowIndex++);
                writeLocked(row, 0, itemIndex + 1, lockedStyle);
                writeLocked(row, 1, order.getId(), lockedStyle);
                writeLocked(row, 2, order.getIqcNo(), lockedStyle);
                writeLocked(row, 3, item.getId(), lockedStyle);
                writeLocked(row, 4, item.getInspectionItem(), lockedStyle);
                writeLocked(row, 5, item.getItemType(), lockedStyle);
                writeLocked(row, 6, item.getUnit(), lockedStyle);
                writeLocked(row, 7, defaultIfBlank(item.getStandardDesc(), "-") + "；平均值内控："
                        + QmsIqcQuantitativeJudgment.range(item.getAvgMinLimit(), item.getAvgMaxLimit()), lockedStyle);
                writeLocked(row, 8, item.getInspectionMethod(), lockedStyle);
                writeLocked(row, 9, sampleSize, lockedStyle);
                String firstRemark = "";
                for (int seq = 1; seq <= maxSampleSize; seq++) {
                    QmsIqcSampleDO sample = sampleMap.get(sampleKey(item.getId(), seq));
                    Cell cell = row.createCell(OVERVIEW_SAMPLE_START_COLUMN + seq - 1);
                    if (includeValues && sample != null) {
                        writeSampleValue(cell, item, sample);
                        if (!StringUtils.hasText(firstRemark) && StringUtils.hasText(sample.getRemark())) {
                            firstRemark = sample.getRemark();
                        }
                    }
                }
                row.createCell(OVERVIEW_SAMPLE_START_COLUMN + maxSampleSize)
                        .setCellValue(includeValues ? resultLabel(item.getItemResult()) : "");
                row.createCell(OVERVIEW_SAMPLE_START_COLUMN + maxSampleSize + 1)
                        .setCellValue(includeValues ? appendRemark(firstRemark, item.getJudgmentReason()) : "");
            }

            sheet.setColumnHidden(1, true);
            sheet.setColumnHidden(2, true);
            sheet.setColumnHidden(3, true);
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private int resolveMaxSampleSize(List<QmsIqcItemDO> items) {
        return items.stream()
                .map(QmsIqcItemDO::getSampleSize)
                .filter(Objects::nonNull)
                .filter(sampleSize -> sampleSize > 0)
                .max(Integer::compareTo)
                .orElse(1);
    }

    private void writeInfoRow(Row row, String label1, Object value1, String label2, Object value2) {
        row.createCell(0).setCellValue(label1);
        writeCell(row.createCell(1), value1);
        row.createCell(3).setCellValue(label2);
        writeCell(row.createCell(4), value2);
    }

    private void writeCell(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        cell.setCellValue(value == null ? "" : String.valueOf(value));
    }

    private void writeSampleValue(Cell cell, QmsIqcItemDO item, QmsIqcSampleDO sample) {
        if ("QUANTITATIVE".equals(item.getItemType()) && sample.getMeasuredValue() != null) {
            cell.setCellValue(sample.getMeasuredValue().doubleValue());
            return;
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType()) && sample.getDateValue() != null) {
            cell.setCellValue(DATE_FORMATTER.format(sample.getDateValue()));
            return;
        }
        cell.setCellValue(defaultIfBlank(sample.getQualitativeValue(), ""));
    }

    private String resultLabel(String result) {
        if (QmsIqcQuantitativeJudgment.SKIP.equals(result)) return "不判定";
        if (JUDGMENT_OK.equals(result)) {
            return "合格";
        }
        if (JUDGMENT_NG.equals(result)) {
            return "不合格";
        }
        return "待判定";
    }

    private void addCoaTitle(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(180);
        XWPFRun run = createWordRun(paragraph, true, 18);
        run.setText(COA_WORD_TITLE);
    }

    private void addCoaBasicInfo(XWPFDocument document, QmsIqcRespVO record) {
        addCoaSectionTitle(document, "基本信息");
        String standardText = joinNotBlank(" / ", record.getStandardNo(), record.getStandardName(),
                record.getStandardVersion());
        String materialText = joinNotBlank(" ", record.getMaterialCode(), record.getMaterialName())
                + (StringUtils.hasText(record.getSpecification()) ? " (" + record.getSpecification() + ")" : "");
        String[][] rows = {
                {"关联收料单", text(record.getReceiptNo()), "批次号", text(record.getBatchNo()),
                        "物料信息", materialText},
                {"到货数量", formatQuantity(record.getReceiveQty(), record.getUnit()),
                        "来料日期", formatDate(record.getArrivalDate()),
                        "生产日期", formatDate(record.getProductionDate())},
                {"失效日期", formatDate(record.getExpiryDate()),
                        "供应商", joinNotBlank(" / ", record.getSupplierCode(), record.getSupplierName()),
                        "总体判定", coaJudgmentLabel(record.getJudgment())},
                {"检验标准", standardText, "检验员", text(record.getInspectorName()),
                        "确认人", text(record.getQaInspectorName())},
                {"检验时间", formatDateTime(record.getInspectionTime()),
                        "确认时间", formatDateTime(record.getQaTime()),
                        "处置方式", text(record.getDisposalType())}
        };
        XWPFTable table = document.createTable(rows.length, 6);
        applyWordTableLayout(table, COA_DESCRIPTION_COLUMN_WIDTHS);
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            for (int colIndex = 0; colIndex < rows[rowIndex].length; colIndex += 2) {
                writeWordCell(row.getCell(colIndex), rows[rowIndex][colIndex], true, true,
                        COA_DESCRIPTION_COLUMN_WIDTHS[colIndex]);
                writeWordCell(row.getCell(colIndex + 1), rows[rowIndex][colIndex + 1], false, false,
                        COA_DESCRIPTION_COLUMN_WIDTHS[colIndex + 1]);
            }
        }
    }

    private void addCoaItems(XWPFDocument document, List<QmsIqcRespVO.IqcItem> sourceItems) {
        addCoaSectionTitle(document, "检验项目与实测明细");
        List<QmsIqcRespVO.IqcItem> items = new ArrayList<>(
                sourceItems == null ? Collections.emptyList() : sourceItems);
        items.sort(Comparator
                .comparing((QmsIqcRespVO.IqcItem item) -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort())
                .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()));
        int bodyRows = Math.max(items.size(), 1);
        XWPFTable table = document.createTable(bodyRows + 1, 9);
        applyWordTableLayout(table, COA_ITEM_COLUMN_WIDTHS);
        String[] headers = {"序号", "检验项目", "单位", "标准要求", "检验方法", "仪器", "取样数", "实测数据记录", "判定"};
        XWPFTableRow header = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeWordCell(header.getCell(i), headers[i], true, true, COA_ITEM_COLUMN_WIDTHS[i]);
        }
        if (items.isEmpty()) {
            XWPFTableRow emptyRow = table.getRow(1);
            writeWordCell(emptyRow.getCell(0), "-", false, false, COA_ITEM_COLUMN_WIDTHS[0]);
            writeWordCell(emptyRow.getCell(1), "暂无检验项", false, false, COA_ITEM_COLUMN_WIDTHS[1]);
            for (int i = 2; i < headers.length; i++) {
                writeWordCell(emptyRow.getCell(i), "", false, false, COA_ITEM_COLUMN_WIDTHS[i]);
            }
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            QmsIqcRespVO.IqcItem item = items.get(index);
            XWPFTableRow row = table.getRow(index + 1);
            String[] values = {
                    String.valueOf(index + 1),
                    text(item.getInspectionItem()),
                    text(item.getUnit()),
                    text(item.getStandardDesc()) + "\n平均值内控："
                            + QmsIqcQuantitativeJudgment.range(item.getAvgMinLimit(), item.getAvgMaxLimit()),
                    text(item.getInspectionMethod()),
                    text(item.getTestTool()),
                    item.getSampleSize() == null ? "-" : String.valueOf(item.getSampleSize()),
                    buildCoaSampleText(item),
                    resultLabel(item.getItemResult()) + (StringUtils.hasText(item.getJudgmentReason())
                            ? "\n" + item.getJudgmentReason() : "")
            };
            for (int colIndex = 0; colIndex < values.length; colIndex++) {
                boolean center = colIndex == 0 || colIndex == 2 || colIndex == 6 || colIndex == 8;
                writeWordCell(row.getCell(colIndex), values[colIndex], false, false,
                        COA_ITEM_COLUMN_WIDTHS[colIndex], center);
            }
        }
    }

    private void addCoaSignature(XWPFDocument document, QmsIqcRespVO record) {
        XWPFParagraph spacer = document.createParagraph();
        spacer.setSpacingAfter(100);
        XWPFTable table = document.createTable(1, 3);
        applyWordTableLayout(table, COA_SIGNATURE_COLUMN_WIDTHS);
        XWPFTableRow row = table.getRow(0);
        writeWordCell(row.getCell(0), "IQC检验员：" + defaultIfBlank(record.getInspectorName(), ""), true, false,
                COA_SIGNATURE_COLUMN_WIDTHS[0], true);
        writeWordCell(row.getCell(1), "品质主管复核：" + defaultIfBlank(record.getQaInspectorName(), ""), true, false,
                COA_SIGNATURE_COLUMN_WIDTHS[1], true);
        writeWordCell(row.getCell(2), "日期：" + formatDateOnly(record.getQaTime(), record.getInspectionTime()),
                true, false, COA_SIGNATURE_COLUMN_WIDTHS[2], true);

        XWPFParagraph meta = document.createParagraph();
        meta.setSpacingBefore(160);
        XWPFRun metaRun = createWordRun(meta, false, 9);
        metaRun.setText(String.join("    ", COA_FOOTER_META_ITEMS));

        XWPFParagraph notice = document.createParagraph();
        notice.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun noticeRun = createWordRun(notice, false, 9);
        noticeRun.setText(COA_FOOTER_NOTICE);
    }

    private void addCoaSectionTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(120);
        paragraph.setSpacingAfter(80);
        XWPFRun run = createWordRun(paragraph, true, 11);
        run.setText(title);
    }

    private XWPFRun createWordRun(XWPFParagraph paragraph, boolean bold, int fontSize) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(fontSize);
        run.setBold(bold);
        return run;
    }

    private void applyCoaPageLayout(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(COA_PAGE_WIDTH_TWIPS));
        pageSize.setH(BigInteger.valueOf(COA_PAGE_HEIGHT_TWIPS));
        CTPageMar pageMargin = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        BigInteger margin = BigInteger.valueOf(COA_PAGE_MARGIN_TWIPS);
        pageMargin.setTop(margin);
        pageMargin.setBottom(margin);
        pageMargin.setLeft(margin);
        pageMargin.setRight(margin);
    }

    private void applyWordTableLayout(XWPFTable table, int[] columnWidths) {
        CTTblPr tablePr = table.getCTTbl().getTblPr();
        if (tablePr == null) {
            tablePr = table.getCTTbl().addNewTblPr();
        }
        CTTblWidth tableWidth = tablePr.isSetTblW() ? tablePr.getTblW() : tablePr.addNewTblW();
        tableWidth.setType(STTblWidth.DXA);
        tableWidth.setW(BigInteger.valueOf(COA_TABLE_WIDTH_TWIPS));

        CTTblGrid tableGrid = table.getCTTbl().getTblGrid();
        if (tableGrid == null) {
            tableGrid = table.getCTTbl().addNewTblGrid();
        }
        while (tableGrid.sizeOfGridColArray() > 0) {
            tableGrid.removeGridCol(0);
        }
        for (int columnWidth : columnWidths) {
            CTTblGridCol column = tableGrid.addNewGridCol();
            column.setW(BigInteger.valueOf(columnWidth));
        }
        for (XWPFTableRow row : table.getRows()) {
            for (int colIndex = 0; colIndex < Math.min(row.getTableCells().size(), columnWidths.length); colIndex++) {
                setWordCellWidth(row.getCell(colIndex), columnWidths[colIndex]);
            }
        }
    }

    private void writeWordCell(XWPFTableCell cell, String value, boolean bold, boolean shaded, int widthTwips) {
        writeWordCell(cell, value, bold, shaded, widthTwips, shaded);
    }

    private void writeWordCell(XWPFTableCell cell, String value, boolean bold, boolean shaded, int widthTwips,
                               boolean center) {
        setWordCellWidth(cell, widthTwips);
        if (shaded) {
            shadeWordCell(cell);
        }
        XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
        paragraph.setAlignment(center ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(0);
        XWPFRun run = createWordRun(paragraph, bold, 9);
        String[] lines = text(value).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i]);
        }
    }

    private void setWordCellWidth(XWPFTableCell cell, int widthTwips) {
        CTTcPr cellPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth cellWidth = cellPr.isSetTcW() ? cellPr.getTcW() : cellPr.addNewTcW();
        cellWidth.setType(STTblWidth.DXA);
        cellWidth.setW(BigInteger.valueOf(widthTwips));
    }

    private void shadeWordCell(XWPFTableCell cell) {
        CTTcPr cellPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTShd shading = cellPr.isSetShd() ? cellPr.getShd() : cellPr.addNewShd();
        shading.setFill(COA_LABEL_FILL);
    }

    private String buildCoaSampleText(QmsIqcRespVO.IqcItem item) {
        List<QmsIqcRespVO.IqcSample> samples = new ArrayList<>(
                item.getSamples() == null ? Collections.emptyList() : item.getSamples());
        samples.sort(Comparator
                .comparing((QmsIqcRespVO.IqcSample sample) ->
                        sample.getSampleSeq() == null ? Integer.MAX_VALUE : sample.getSampleSeq())
                .thenComparing(sample -> sample.getId() == null ? Long.MAX_VALUE : sample.getId()));
        List<String> values = samples.stream()
                .map(sample -> formatCoaSampleValue(item, sample))
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        String sampleText = values.isEmpty() ? "-" : String.join("、", values);
        String statsText = buildCoaSampleStats(item);
        return StringUtils.hasText(statsText) ? sampleText + "\n" + statsText : sampleText;
    }

    private String formatCoaSampleValue(QmsIqcRespVO.IqcItem item, QmsIqcRespVO.IqcSample sample) {
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType())) {
            BigDecimal value = sample.getResultValue() == null ? sample.getMeasuredValue() : sample.getResultValue();
            if (value == null) {
                return "";
            }
            return formatDecimal(value) + defaultIfBlank(item.getUnit(), "");
        }
        if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            return sample.getDateValue() == null ? "" : DATE_FORMATTER.format(sample.getDateValue());
        }
        return defaultIfBlank(sample.getQualitativeValue(), defaultIfBlank(sample.getSampleResult(), ""));
    }

    private String buildCoaSampleStats(QmsIqcRespVO.IqcItem item) {
        if (!ITEM_TYPE_QUANTITATIVE.equals(item.getItemType())) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        appendDecimalPart(parts, "Max", item.getMaxValue());
        appendDecimalPart(parts, "Min", item.getMinValue());
        appendDecimalPart(parts, "Avg", item.getAverageValue());
        return parts.isEmpty() ? "" : "（" + String.join("，", parts) + "）";
    }

    private void appendDecimalPart(List<String> parts, String label, BigDecimal value) {
        if (value != null) {
            parts.add(label + ":" + formatDecimal(value));
        }
    }

    private String coaJudgmentLabel(String judgment) {
        if (QmsIqcQuantitativeJudgment.SKIP.equals(judgment)) return "不判定";
        if (JUDGMENT_OK.equals(judgment)) {
            return "合格允收";
        }
        if (JUDGMENT_NG.equals(judgment)) {
            return "不合格拒收";
        }
        return "待判定";
    }

    private String joinNotBlank(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return parts.isEmpty() ? "-" : String.join(separator, parts);
    }

    private String text(String value) {
        return defaultIfBlank(value, "-");
    }

    private String formatQuantity(BigDecimal value, String unit) {
        if (value == null) {
            return "-";
        }
        return formatDecimal(value) + (StringUtils.hasText(unit) ? " " + unit : "");
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String formatDateOnly(LocalDateTime primary, LocalDateTime fallback) {
        LocalDateTime value = primary == null ? fallback : primary;
        return value == null ? "" : DATE_FORMATTER.format(value.toLocalDate());
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createLockedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeLocked(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private QmsIqcItemWorkbookService.ItemImportPlan buildDynamicItemImportPlan(
            Long id, MultipartFile file, boolean previewOnly, boolean allowOverwrite) throws IOException {
        QmsIqcOrderDO order = validateIqcExists(id);
        validateEditable(order);
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(id);
        validateItemsExist(items);
        return qmsIqcItemWorkbookService.buildImportPlan(order, items, loadCurrentSampleDoMap(id),
                file, allowOverwrite, previewOnly);
    }

    private QmsIqcImportRespVO confirmDynamicItemImport(
            Long id, boolean allowOverwrite, MultipartFile file) throws IOException {
        QmsIqcItemWorkbookService.ItemImportPlan plan =
                buildDynamicItemImportPlan(id, file, false, allowOverwrite);
        if (plan.failureCount > 0 || plan.validRows.isEmpty()) {
            return plan.toResp(null);
        }

        QmsIqcOrderDO order = validateIqcExists(id);
        validateEditable(order);
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(id);
        Map<Long, QmsIqcItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsIqcItemDO::getId, item -> item));
        Map<Long, Map<Integer, QmsIqcSaveReqVO.IqcSample>> sampleMap = loadCurrentSampleMap(id);
        for (QmsIqcItemWorkbookService.ItemImportRow row : plan.validRows) {
            sampleMap.computeIfAbsent(row.itemId, key -> new LinkedHashMap<>())
                    .put(row.sampleSeq, row.sample);
        }

        List<QmsIqcSaveReqVO.IqcItem> importItems = sampleMap.entrySet().stream()
                .filter(entry -> plan.affectedItemIds.contains(entry.getKey()))
                .map(entry -> buildImportItem(itemMap.get(entry.getKey()), entry.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        updateExecutionDetails(order, importItems, null);
        updateProgramEntryStatus(order.getId(), STATUS_INSPECTING,
                calculateCurrentJudgment(order.getId()), null);
        return plan.toResp(getIqcResp(id));
    }

    private Map<String, QmsIqcSampleDO> loadCurrentSampleDoMap(Long iqcId) {
        return qmsIqcSampleMapper.selectListByIqcId(iqcId).stream()
                .collect(Collectors.toMap(sample -> sampleKey(sample.getIqcItemId(), sample.getSampleSeq()),
                        sample -> sample, (first, ignored) -> first, LinkedHashMap::new));
    }

    private ItemImportPlan buildItemImportPlan(Long id, MultipartFile file, boolean previewOnly,
                                               boolean allowOverwrite) throws IOException {
        validateEditable(validateIqcExists(id));
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(id);
        validateItemsExist(items);
        Map<Long, QmsIqcItemDO> itemMap = items.stream()
                .collect(Collectors.toMap(QmsIqcItemDO::getId, item -> item));
        Map<String, QmsIqcSampleDO> existingSampleMap = qmsIqcSampleMapper.selectListByIqcId(id).stream()
                .collect(Collectors.toMap(sample -> sampleKey(sample.getIqcItemId(), sample.getSampleSeq()),
                        sample -> sample, (first, ignored) -> first));
        ItemImportPlan plan = new ItemImportPlan(file == null ? null : file.getOriginalFilename(), previewOnly);
        if (file == null || file.isEmpty()) {
            plan.addFailure("请选择要导入的 Excel 文件");
            return plan;
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            if (isOverviewTemplateSheet(sheet, formatter)) {
                readOverviewImportRows(id, sheet, formatter, itemMap, existingSampleMap, plan, previewOnly, allowOverwrite);
            } else {
                readLegacyImportRows(id, sheet, formatter, itemMap, existingSampleMap, plan, previewOnly, allowOverwrite);
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            plan.addFailure("Excel 解析失败，请确认文件来自当前 IQC 单下载模板");
        }
        if (plan.totalCount == 0) {
            plan.addFailure("未识别到可导入的检验项样本行");
        }
        return plan;
    }

    private boolean isOverviewTemplateSheet(Sheet sheet, DataFormatter formatter) {
        Row titleRow = sheet.getRow(0);
        return titleRow != null
                && formatter.formatCellValue(titleRow.getCell(0)).startsWith(OVERVIEW_TEMPLATE_TITLE);
    }

    private void readOverviewImportRows(Long id, Sheet sheet, DataFormatter formatter,
                                        Map<Long, QmsIqcItemDO> itemMap,
                                        Map<String, QmsIqcSampleDO> existingSampleMap,
                                        ItemImportPlan plan,
                                        boolean previewOnly,
                                        boolean allowOverwrite) {
        int maxSampleSize = itemMap.values().stream()
                .map(QmsIqcItemDO::getSampleSize)
                .filter(Objects::nonNull)
                .filter(sampleSize -> sampleSize > 0)
                .max(Integer::compareTo)
                .orElse(1);
        for (int rowIndex = OVERVIEW_DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Long itemId = parseLong(formatter.formatCellValue(row.getCell(3)));
            QmsIqcItemDO item = itemId == null ? null : itemMap.get(itemId);
            if (item == null) {
                if (hasAnySampleValue(row, formatter, maxSampleSize)) {
                    plan.totalCount++;
                    plan.addFailure("第 " + (rowIndex + 1) + " 行：检验项不存在");
                }
                continue;
            }
            int sampleSize = item.getSampleSize() == null || item.getSampleSize() <= 0 ? 1 : item.getSampleSize();
            String remark = trimToNull(formatter.formatCellValue(row.getCell(OVERVIEW_SAMPLE_START_COLUMN + maxSampleSize + 1)));
            for (int seq = 1; seq <= sampleSize; seq++) {
                String value = trimToNull(formatter.formatCellValue(row.getCell(OVERVIEW_SAMPLE_START_COLUMN + seq - 1)));
                if (value == null) {
                    continue;
                }
                ItemImportRow importRow = new ItemImportRow();
                importRow.iqcId = id;
                importRow.iqcNo = formatter.formatCellValue(row.getCell(2));
                importRow.itemId = itemId;
                importRow.sampleSeq = seq;
                importRow.inspectionItem = formatter.formatCellValue(row.getCell(4));
                importRow.itemType = item.getItemType();
                if ("QUANTITATIVE".equals(item.getItemType())) {
                    importRow.measuredValue = parseDecimal(value);
                } else if (ITEM_TYPE_DATE.equals(item.getItemType())) {
                    importRow.dateValue = parseLocalDate(value);
                } else {
                    importRow.qualitativeValue = value;
                }
                importRow.remark = remark;
                acceptImportRow(id, rowIndex, importRow, itemMap, existingSampleMap, plan, previewOnly, allowOverwrite);
            }
        }
    }

    private boolean hasAnySampleValue(Row row, DataFormatter formatter, int maxSampleSize) {
        for (int seq = 1; seq <= maxSampleSize; seq++) {
            if (trimToNull(formatter.formatCellValue(row.getCell(OVERVIEW_SAMPLE_START_COLUMN + seq - 1))) != null) {
                return true;
            }
        }
        return false;
    }

    private void readLegacyImportRows(Long id, Sheet sheet, DataFormatter formatter,
                                      Map<Long, QmsIqcItemDO> itemMap,
                                      Map<String, QmsIqcSampleDO> existingSampleMap,
                                      ItemImportPlan plan,
                                      boolean previewOnly,
                                      boolean allowOverwrite) {
        for (int rowIndex = LEGACY_DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                ItemImportRow importRow = readImportRow(row, formatter);
                if (importRow.isEmpty()) {
                    continue;
                }
                acceptImportRow(id, rowIndex, importRow, itemMap, existingSampleMap, plan, previewOnly, allowOverwrite);
            }
    }

    private void acceptImportRow(Long id, int rowIndex, ItemImportRow importRow,
                                 Map<Long, QmsIqcItemDO> itemMap,
                                 Map<String, QmsIqcSampleDO> existingSampleMap,
                                 ItemImportPlan plan,
                                 boolean previewOnly,
                                 boolean allowOverwrite) {
        plan.totalCount++;
        List<String> errors = validateImportRow(id, importRow, itemMap);
        QmsIqcItemDO item = itemMap.get(importRow.itemId);
        if (item != null && existingSampleMap.containsKey(sampleKey(item.getId(), importRow.sampleSeq))) {
            if (previewOnly || allowOverwrite) {
                plan.warningCount++;
                plan.messages.add("第 " + (rowIndex + 1) + " 行将覆盖已有样本值");
            } else {
                errors.add("存在已有样本值，需确认覆盖后再导入");
            }
        }
        if (!errors.isEmpty()) {
            plan.failureCount++;
            plan.messages.add("第 " + (rowIndex + 1) + " 行：" + String.join("；", errors));
            return;
        }
        plan.successCount++;
        plan.validRows.add(importRow);
        plan.affectedItemIds.add(importRow.itemId);
    }

    private ItemImportRow readImportRow(Row row, DataFormatter formatter) {
        ItemImportRow importRow = new ItemImportRow();
        importRow.iqcId = parseLong(formatter.formatCellValue(row.getCell(0)));
        importRow.iqcNo = trimToNull(formatter.formatCellValue(row.getCell(1)));
        importRow.itemId = parseLong(formatter.formatCellValue(row.getCell(2)));
        importRow.sampleSeq = parseInteger(formatter.formatCellValue(row.getCell(3)));
        importRow.inspectionItem = trimToNull(formatter.formatCellValue(row.getCell(4)));
        importRow.itemType = trimToNull(formatter.formatCellValue(row.getCell(5)));
        importRow.measuredValue = parseDecimal(formatter.formatCellValue(row.getCell(10)));
        importRow.qualitativeValue = trimToNull(formatter.formatCellValue(row.getCell(11)));
        if (ITEM_TYPE_DATE.equals(importRow.itemType)) {
            importRow.dateValue = parseLocalDate(formatter.formatCellValue(row.getCell(10)));
            importRow.measuredValue = null;
        }
        importRow.remark = trimToNull(formatter.formatCellValue(row.getCell(12)));
        return importRow;
    }

    private List<String> validateImportRow(Long id, ItemImportRow row, Map<Long, QmsIqcItemDO> itemMap) {
        List<String> errors = new ArrayList<>();
        if (!Objects.equals(id, row.iqcId)) {
            errors.add("IQC ID 不匹配");
        }
        QmsIqcItemDO item = row.itemId == null ? null : itemMap.get(row.itemId);
        if (item == null) {
            errors.add("检验项不存在");
            return errors;
        }
        int sampleSize = item.getSampleSize() == null || item.getSampleSize() <= 0 ? 1 : item.getSampleSize();
        if (row.sampleSeq == null || row.sampleSeq < 1 || row.sampleSeq > sampleSize) {
            errors.add("样本序号超出当前检验项取样范围");
        }
        if ("QUANTITATIVE".equals(item.getItemType())) {
            if (row.measuredValue == null) {
                errors.add("定量检验项必须填写实测值");
            }
        } else if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            if (row.dateValue == null) {
                errors.add("时间检验项必须填写 yyyy-MM-dd 日期");
            } else if (row.dateValue.isAfter(LocalDate.now(BUSINESS_ZONE_ID))) {
                errors.add("时间检验项日期不能晚于当前日期");
            }
            if (item.getExpiryDays() == null || item.getExpiryDays() < 0) {
                errors.add("时间检验项缺少过期天数快照");
            }
        } else if (!JUDGMENT_OK.equals(row.qualitativeValue) && !JUDGMENT_NG.equals(row.qualitativeValue)) {
            errors.add("定性检验项必须填写 OK 或 NG");
        }
        return errors;
    }

    private Map<Long, Map<Integer, QmsIqcSaveReqVO.IqcSample>> loadCurrentSampleMap(Long iqcId) {
        Map<Long, Map<Integer, QmsIqcSaveReqVO.IqcSample>> result = new LinkedHashMap<>();
        for (QmsIqcSampleDO sample : qmsIqcSampleMapper.selectListByIqcId(iqcId)) {
            QmsIqcSaveReqVO.IqcSample target = BeanUtils.toBean(sample, QmsIqcSaveReqVO.IqcSample.class);
            result.computeIfAbsent(sample.getIqcItemId(), key -> new LinkedHashMap<>()).put(sample.getSampleSeq(), target);
        }
        return result;
    }

    private QmsIqcSaveReqVO.IqcItem buildImportItem(QmsIqcItemDO item, Map<Integer, QmsIqcSaveReqVO.IqcSample> sampleMap) {
        if (item == null) {
            return null;
        }
        QmsIqcSaveReqVO.IqcItem target = new QmsIqcSaveReqVO.IqcItem();
        target.setId(item.getId());
        target.setStandardItemId(item.getStandardItemId());
        target.setInspectionItem(item.getInspectionItem());
        target.setItemType(item.getItemType());
        target.setSamples(sampleMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
        return target;
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    private Long parseLong(String text) {
        String value = trimToNull(text);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).longValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInteger(String text) {
        String value = trimToNull(text);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).intValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String text) {
        String value = trimToNull(text);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String text) {
        String value = trimToNull(text);
        if (value == null) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DATE_FORMATTER,
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"));
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported Excel/text date format.
            }
        }
        return null;
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private IqcStandardCandidateMatch selectIqcStandardCandidate(QmsIqcSaveReqVO reqVO) {
        if (reqVO.getStandardId() != null) {
            QmsQualityStandardDO standard = qmsQualityStandardMapper.selectById(reqVO.getStandardId());
            validateIqcStandardUsable(standard);
            return matchIqcStandardForManualSelection(standard, reqVO);
        }

        IqcStandardCandidateMatch selectedMatch =
                selectUniqueHighestPriorityMatch(findIqcStandardCandidateMatches(reqVO));
        if (selectedMatch == null) {
            throw exception(HCIQC_STANDARD_NOT_EXISTS);
        }
        return selectedMatch;
    }

    private List<IqcStandardCandidateMatch> findIqcStandardCandidateMatches(QmsIqcSaveReqVO reqVO) {
        List<QmsQualityStandardDO> standards = qmsQualityStandardMapper.selectList(
                new LambdaQueryWrapperX<QmsQualityStandardDO>()
                        .eq(QmsQualityStandardDO::getApplyType, APPLY_TYPE_IQC)
                        .eq(QmsQualityStandardDO::getStatus, ENABLED)
                        .eq(QmsQualityStandardDO::getAuditStatus, AUDITED)
                        .orderByDesc(QmsQualityStandardDO::getId));
        return standards.stream()
                .filter(this::hasExecutableStandardItems)
                .map(standard -> evaluateIqcStandardMatch(standard, reqVO))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(IqcStandardCandidateMatch::score)
                        .thenComparing(match -> match.standard().getId())
                        .reversed())
                .collect(Collectors.toList());
    }

    private boolean hasExecutableStandardItems(QmsQualityStandardDO standard) {
        if (standard == null || standard.getId() == null) {
            return false;
        }
        List<QmsQualityStandardItemDO> items =
                qmsQualityStandardItemMapper.selectListByStandardId(standard.getId());
        return items != null && !items.isEmpty();
    }

    private IqcStandardCandidateMatch matchIqcStandardForManualSelection(QmsQualityStandardDO standard,
                                                                        QmsIqcSaveReqVO reqVO) {
        IqcStandardCandidateMatch match = evaluateIqcStandardMatch(standard, reqVO);
        if (match == null) {
            throw invalidParamException("所选IQC检验标准与当前物料不匹配");
        }
        return match;
    }

    private IqcStandardCandidateMatch selectUniqueHighestPriorityMatch(List<IqcStandardCandidateMatch> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int highestScore = candidates.get(0).score();
        List<IqcStandardCandidateMatch> highestMatches = candidates.stream()
                .filter(candidate -> candidate.score() == highestScore)
                .toList();
        return highestMatches.size() == 1 ? highestMatches.get(0) : null;
    }

    private IqcStandardCandidateMatch evaluateIqcStandardMatch(QmsQualityStandardDO standard,
                                                              QmsIqcSaveReqVO reqVO) {
        if (matchesMaterialIdScope(standard, reqVO)) {
            return new IqcStandardCandidateMatch(standard, MATCH_SCORE_MATERIAL_ID,
                    STANDARD_MATCH_MATERIAL, "物料ID与送检物料一致");
        }
        if (matchesMaterialCodeScope(standard, reqVO)) {
            return new IqcStandardCandidateMatch(standard, MATCH_SCORE_MATERIAL_CODE,
                    STANDARD_MATCH_MATERIAL, "物料编码与送检物料一致");
        }
        if (matchesUniversalScope(standard)) {
            return new IqcStandardCandidateMatch(standard, MATCH_SCORE_UNIVERSAL,
                    STANDARD_MATCH_UNIVERSAL, "IQC通用标准，适用于全部送检物料");
        }
        return null;
    }

    private boolean matchesMaterialIdScope(QmsQualityStandardDO standard, QmsIqcSaveReqVO reqVO) {
        return standard != null
                && standard.getMaterialId() != null
                && reqVO.getMaterialId() != null
                && standard.getMaterialId().equals(reqVO.getMaterialId());
    }

    private boolean matchesMaterialCodeScope(QmsQualityStandardDO standard, QmsIqcSaveReqVO reqVO) {
        return standard != null
                && StringUtils.hasText(standard.getMaterialCode())
                && StringUtils.hasText(reqVO.getMaterialCode())
                && standard.getMaterialCode().equals(reqVO.getMaterialCode());
    }

    private boolean matchesUniversalScope(QmsQualityStandardDO standard) {
        return standard != null
                && !StringUtils.hasText(standard.getMaterialCode())
                && !StringUtils.hasText(standard.getMaterialName());
    }

    private void validateIqcStandardUsable(QmsQualityStandardDO standard) {
        if (standard == null
                || !APPLY_TYPE_IQC.equals(standard.getApplyType())
                || !ENABLED.equals(standard.getStatus())
                || !AUDITED.equals(standard.getAuditStatus())) {
            throw exception(HCIQC_STANDARD_NOT_EXISTS);
        }
    }

    private List<QmsQualityStandardItemDO> selectIqcStandardItems(Long standardId) {
        List<QmsQualityStandardItemDO> standardItems = qmsQualityStandardItemMapper.selectListByStandardId(standardId);
        if (standardItems == null || standardItems.isEmpty()) {
            throw exception(HCIQC_STANDARD_ITEMS_EMPTY);
        }
        return standardItems;
    }

    private void applyIqcStandardSnapshot(QmsIqcOrderDO order, QmsQualityStandardDO standard, String matchType) {
        order.setStandardId(standard.getId());
        order.setStandardNo(standard.getStandardNo());
        order.setStandardName(standard.getStandardName());
        order.setStandardVersion(standard.getVersion());
        order.setStandardSnapshotTime(LocalDateTime.now());
        order.setStandardMatchMode(defaultIfBlank(matchType, STANDARD_MATCH_MATERIAL));
        order.setStandardSnapshotLocked(true);
        if (STANDARD_MATCH_MATERIAL.equals(defaultIfBlank(matchType, STANDARD_MATCH_MATERIAL))) {
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
            if (order.getProductModelId() == null) {
                order.setProductModelId(standard.getProductModelId());
            }
            if (!StringUtils.hasText(order.getProductModelCode())) {
                order.setProductModelCode(standard.getProductModelCode());
            }
            if (!StringUtils.hasText(order.getProductModelName())) {
                order.setProductModelName(standard.getProductModelName());
            }
        }
    }

    private void clearIqcStandardSnapshot(QmsIqcOrderDO order) {
        order.setStandardId(null);
        order.setStandardNo(null);
        order.setStandardName(null);
        order.setStandardVersion(null);
        order.setStandardSnapshotTime(null);
        order.setStandardMatchMode(null);
        order.setStandardSnapshotLocked(false);
        order.setStandardSnapshotHash(null);
    }

    private void applyExistingStandardSnapshot(QmsIqcOrderDO updateObj, QmsIqcOrderDO old) {
        updateObj.setStandardId(old.getStandardId());
        updateObj.setStandardNo(old.getStandardNo());
        updateObj.setStandardName(old.getStandardName());
        updateObj.setStandardVersion(old.getStandardVersion());
        updateObj.setStandardSnapshotTime(old.getStandardSnapshotTime());
        updateObj.setStandardMatchMode(old.getStandardMatchMode());
        updateObj.setStandardSnapshotLocked(old.getStandardSnapshotLocked());
        updateObj.setStandardSnapshotHash(old.getStandardSnapshotHash());
    }

    private void saveIqcStandardSnapshotDetails(QmsIqcOrderDO order, List<QmsQualityStandardItemDO> standardItems,
                                                List<QmsIqcSaveReqVO.IqcAbnormal> abnormals) {
        for (int i = 0; i < standardItems.size(); i++) {
            QmsQualityStandardItemDO standardItem = standardItems.get(i);
            QmsIqcItemDO itemDO = new QmsIqcItemDO();
            itemDO.setIqcId(order.getId());
            itemDO.setIqcNo(order.getIqcNo());
            itemDO.setStandardItemId(standardItem.getId());
            itemDO.setInspectionItem(standardItem.getInspectionItem());
            itemDO.setItemType(standardItem.getItemType());
            itemDO.setExpiryDays(standardItem.getExpiryDays());
            itemDO.setAttachmentEnabled(Boolean.TRUE.equals(standardItem.getAttachmentEnabled()));
            itemDO.setAttachmentUrls(Collections.emptyList());
            itemDO.setTargetValue(standardItem.getTargetValue());
            itemDO.setStandardDesc(standardItem.getStandardDesc());
            itemDO.setUnit(standardItem.getUnit());
            itemDO.setInspectionMethod(standardItem.getInspectionMethod());
            itemDO.setTestFrequencyJudgement(standardItem.getTestFrequencyJudgement());
            itemDO.setEntryRuleType(standardItem.getEntryRuleType());
            itemDO.setRuleDescription(standardItem.getRuleDescription());
            itemDO.setValueTemplate(standardItem.getValueTemplate());
            itemDO.setValueTemplateName(resolveValueTemplateName(standardItem.getValueTemplate(),
                    standardItem.getEntryRuleTemplateName()));
            itemDO.setTemplateParams(standardItem.getTemplateParams());
            itemDO.setTestTool(standardItem.getTestTool());
            itemDO.setSampleSize(standardItem.getSampleSize() == null || standardItem.getSampleSize() <= 0 ? 1 : standardItem.getSampleSize());
            itemDO.setMinValueLimit(standardItem.getMinValue());
            itemDO.setMaxValueLimit(standardItem.getMaxValue());
            itemDO.setAvgMinLimit(standardItem.getAvgMinLimit());
            itemDO.setAvgMaxLimit(standardItem.getAvgMaxLimit());
            itemDO.setItemResult(JUDGMENT_PENDING);
            itemDO.setIsSpc(Boolean.TRUE.equals(standardItem.getIsSpc()));
            itemDO.setSort(standardItem.getSort() == null ? (i + 1) * 10 : standardItem.getSort());
            qmsIqcItemMapper.insert(itemDO);
        }
        saveAbnormals(order, abnormals);
    }

    private void updateExecutionDetails(QmsIqcOrderDO order, List<QmsIqcSaveReqVO.IqcItem> items,
                                        List<QmsIqcSaveReqVO.IqcAbnormal> abnormals) {
        if (items != null && !items.isEmpty()) {
            List<QmsIqcItemDO> existingItems = qmsIqcItemMapper.selectListByIqcId(order.getId());
            validateItemsExist(existingItems);
            for (QmsIqcSaveReqVO.IqcItem item : items) {
                if (item.getId() == null && item.getStandardItemId() == null) {
                    throw exception(HCIQC_ITEM_SOURCE_INVALID);
                }
                if (!matchesExistingItem(item, existingItems)) {
                    throw exception(HCIQC_ITEM_SOURCE_INVALID);
                }
            }

            List<Long> incomingItemIds = existingItems.stream()
                    .filter(existingItem -> {
                        QmsIqcSaveReqVO.IqcItem incomingItem = findIncomingItem(existingItem, items);
                        return incomingItem != null
                                && incomingItem.getSamples() != null;
                    })
                    .map(QmsIqcItemDO::getId)
                    .collect(Collectors.toList());
            if (!incomingItemIds.isEmpty()) {
                qmsIqcSampleMapper.deleteByIqcIdAndItemIds(order.getId(), incomingItemIds);
            }

            for (QmsIqcItemDO existingItem : existingItems) {
                QmsIqcSaveReqVO.IqcItem incomingItem = findIncomingItem(existingItem, items);
                if (incomingItem == null) {
                    continue;
                }
                applyAttachmentUrls(existingItem, incomingItem.getAttachmentUrls());
                if (incomingItem.getSamples() != null) {
                    applySampleStats(existingItem, incomingItem.getSamples());
                }
                qmsIqcItemMapper.updateById(existingItem);
                if (incomingItem.getSamples() != null) {
                    saveSamples(order, existingItem, incomingItem.getSamples());
                }
            }
        }

        if (abnormals != null) {
            qmsIqcAbnormalMapper.deleteByIqcId(order.getId());
            saveAbnormals(order, abnormals);
        }
    }

    private boolean matchesExistingItem(QmsIqcSaveReqVO.IqcItem item, List<QmsIqcItemDO> existingItems) {
        return findIncomingTarget(item, existingItems) != null;
    }

    private QmsIqcSaveReqVO.IqcItem findIncomingItem(QmsIqcItemDO existingItem, List<QmsIqcSaveReqVO.IqcItem> items) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), existingItem.getId())
                        || (item.getId() == null && Objects.equals(item.getStandardItemId(), existingItem.getStandardItemId())))
                .findFirst()
                .orElse(null);
    }

    private QmsIqcItemDO findIncomingTarget(QmsIqcSaveReqVO.IqcItem item, List<QmsIqcItemDO> existingItems) {
        return existingItems.stream()
                .filter(existingItem -> Objects.equals(item.getId(), existingItem.getId())
                        || (item.getId() == null && Objects.equals(item.getStandardItemId(), existingItem.getStandardItemId())))
                .findFirst()
                .orElse(null);
    }

    private void saveDetails(QmsIqcOrderDO order, List<QmsIqcSaveReqVO.IqcItem> items,
                             List<QmsIqcSaveReqVO.IqcAbnormal> abnormals) {
        for (int i = 0; i < items.size(); i++) {
            QmsIqcSaveReqVO.IqcItem item = items.get(i);
            QmsIqcItemDO itemDO = BeanUtils.toBean(item, QmsIqcItemDO.class);
            itemDO.setId(null);
            itemDO.setIqcId(order.getId());
            itemDO.setIqcNo(order.getIqcNo());
            itemDO.setSort(item.getSort() == null ? (i + 1) * 10 : item.getSort());
            itemDO.setIsSpc(Boolean.TRUE.equals(item.getIsSpc()));
            applyAttachmentUrls(itemDO, item.getAttachmentUrls());
            applySampleStats(itemDO, item.getSamples());
            qmsIqcItemMapper.insert(itemDO);
            saveSamples(order, itemDO, item.getSamples());
        }
        saveAbnormals(order, abnormals);
    }

    private void applyAttachmentUrls(QmsIqcItemDO itemDO, List<String> attachmentUrls) {
        if (attachmentUrls == null) {
            return;
        }
        List<String> normalizedUrls = attachmentUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (!Boolean.TRUE.equals(itemDO.getAttachmentEnabled()) && !normalizedUrls.isEmpty()) {
            throw exception(HCIQC_ATTACHMENT_DISABLED, itemDO.getInspectionItem());
        }
        if (normalizedUrls.size() > MAX_ITEM_ATTACHMENT_COUNT) {
            throw exception(HCIQC_ATTACHMENT_TOO_MANY, itemDO.getInspectionItem());
        }
        itemDO.setAttachmentUrls(normalizedUrls);
    }

    private void applyInspectionApplyAttachmentUrls(QmsIqcSaveReqVO reqVO) {
        if (reqVO.getInspectionApplyAttachmentUrls() == null) {
            return;
        }
        reqVO.setInspectionApplyAttachmentUrls(
                normalizeInspectionApplyAttachmentUrls(reqVO.getInspectionApplyAttachmentUrls()));
    }

    static List<String> normalizeInspectionApplyAttachmentUrls(List<String> attachmentUrls) {
        if (attachmentUrls == null) {
            return Collections.emptyList();
        }
        List<String> normalizedUrls = attachmentUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedUrls.size() > MAX_INSPECTION_APPLY_ATTACHMENT_COUNT) {
            throw invalidParamException("送检附件最多上传10个");
        }
        normalizedUrls.forEach(QmsIqcServiceImpl::validateInspectionApplyAttachmentUrl);
        return normalizedUrls;
    }

    private static void validateInspectionApplyAttachmentUrl(String attachmentUrl) {
        if (attachmentUrl.length() > MAX_INSPECTION_APPLY_ATTACHMENT_URL_LENGTH) {
            throw invalidParamException("送检附件地址不能超过2048个字符");
        }
        String lowerUrl = attachmentUrl.toLowerCase(Locale.ROOT);
        if (!(lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")
                || lowerUrl.startsWith("/"))) {
            throw invalidParamException("送检附件地址格式不正确");
        }
        String path = attachmentUrl;
        int queryIndex = path.indexOf('?');
        int fragmentIndex = path.indexOf('#');
        int endIndex = path.length();
        if (queryIndex >= 0) {
            endIndex = Math.min(endIndex, queryIndex);
        }
        if (fragmentIndex >= 0) {
            endIndex = Math.min(endIndex, fragmentIndex);
        }
        try {
            path = URLDecoder.decode(path.substring(0, endIndex), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw invalidParamException("送检附件地址格式不正确");
        }
        int slashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == path.length() - 1) {
            throw invalidParamException("送检附件缺少有效文件扩展名");
        }
        String extension = path.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!INSPECTION_APPLY_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw invalidParamException("送检附件不支持该文件类型：" + extension);
        }
    }

    private void saveSamples(QmsIqcOrderDO order, QmsIqcItemDO itemDO, List<QmsIqcSaveReqVO.IqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        List<QmsIqcSampleDO> sampleList = new ArrayList<>();
        for (QmsIqcSaveReqVO.IqcSample sample : samples) {
            QmsIqcSampleDO sampleDO = BeanUtils.toBean(sample, QmsIqcSampleDO.class);
            sampleDO.setId(null);
            sampleDO.setIqcId(order.getId());
            sampleDO.setIqcItemId(itemDO.getId());
            sampleDO.setIqcNo(order.getIqcNo());
            sampleDO.setSampleResult(defaultIfBlank(sample.getSampleResult(), JUDGMENT_PENDING));
            sampleList.add(sampleDO);
        }
        qmsIqcSampleMapper.insertBatch(sampleList);
    }

    private void saveAbnormals(QmsIqcOrderDO order, List<QmsIqcSaveReqVO.IqcAbnormal> abnormals) {
        if (abnormals == null || abnormals.isEmpty()) {
            return;
        }
        List<QmsIqcAbnormalDO> abnormalList = new ArrayList<>();
        for (QmsIqcSaveReqVO.IqcAbnormal abnormal : abnormals) {
            QmsIqcAbnormalDO abnormalDO = BeanUtils.toBean(abnormal, QmsIqcAbnormalDO.class);
            abnormalDO.setId(null);
            abnormalDO.setIqcId(order.getId());
            abnormalDO.setIqcNo(order.getIqcNo());
            abnormalDO.setProcessStatus(defaultIfBlank(abnormal.getProcessStatus(), PROCESS_PENDING));
            abnormalList.add(abnormalDO);
        }
        qmsIqcAbnormalMapper.insertBatch(abnormalList);
    }

    private void applySampleStats(QmsIqcItemDO itemDO, List<QmsIqcSaveReqVO.IqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            itemDO.setMaxValue(null);
            itemDO.setMinValue(null);
            itemDO.setAverageValue(null);
            itemDO.setItemResult(JUDGMENT_PENDING);
            itemDO.setJudgmentReason("检测数据未填写完整");
            return;
        }
        itemDO.setJudgmentReason("");
        itemDO.setMaxValue(null);
        itemDO.setMinValue(null);
        itemDO.setAverageValue(null);
        List<BigDecimal> resultValues = samples.stream()
                .map(sample -> calculateSampleMetric(itemDO, sample))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!resultValues.isEmpty()) {
            itemDO.setMaxValue(Collections.max(resultValues));
            itemDO.setMinValue(Collections.min(resultValues));
            BigDecimal total = resultValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            itemDO.setAverageValue(total.divide(BigDecimal.valueOf(resultValues.size()), 6, RoundingMode.HALF_UP));
        }
        itemDO.setItemResult(calculateItemResult(itemDO, samples));
    }

    private BigDecimal calculateSampleMetric(QmsIqcItemDO itemDO, QmsIqcSaveReqVO.IqcSample sample) {
        if (!ITEM_TYPE_QUANTITATIVE.equals(itemDO.getItemType())) {
            return null;
        }
        Map<String, Object> rawValues = parseRawValues(sample.getRawValuesJson());
        BigDecimal calculatedValue = calculateDataRuleMetric(itemDO, sample, rawValues);
        if (calculatedValue == null) {
            calculatedValue = calculateFixedTemplateMetric(itemDO, sample, rawValues);
        }
        if (calculatedValue == null) {
            calculatedValue = firstNonNull(sample.getResultValue(), sample.getMeasuredValue(), getDecimal(rawValues, "value", null));
        }
        sample.setResultValue(calculatedValue);
        if (!StringUtils.hasText(sample.getRawValuesJson()) && sample.getMeasuredValue() != null) {
            sample.setRawValuesJson(JsonUtils.toJsonString(Map.of("value", sample.getMeasuredValue())));
        }
        return calculatedValue;
    }

    private BigDecimal calculateDataRuleMetric(QmsIqcItemDO itemDO, QmsIqcSaveReqVO.IqcSample sample,
                                               Map<String, Object> rawValues) {
        if (rawValues.isEmpty() || !QmsEntryRuleFormulaSupport.hasDataRule(itemDO.getTemplateParams())) {
            return null;
        }
        try {
            QmsEntryRuleFormulaSupport.CalculationResult result =
                    QmsEntryRuleFormulaSupport.calculate(itemDO.getTemplateParams(), rawValues);
            BigDecimal metric = result.getJudgmentValue();
            sample.setResultValue(metric);
            return metric;
        } catch (IllegalArgumentException ex) {
            return sample.getResultValue();
        }
    }

    private BigDecimal calculateFixedTemplateMetric(QmsIqcItemDO itemDO, QmsIqcSaveReqVO.IqcSample sample,
                                                    Map<String, Object> rawValues) {
        String template = itemDO.getValueTemplate();
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            BigDecimal thicknessMm = getDecimal(rawValues, "thicknessMm", null);
            BigDecimal weightG = getDecimal(rawValues, "weightG", null);
            BigDecimal diameterMm = getDecimal(rawValues, "diameterMm", DEFAULT_SAMPLE_DIAMETER_MM);
            if (isMissingOrZero(thicknessMm) || isMissingOrZero(diameterMm) || weightG == null) {
                return sample.getResultValue();
            }
            BigDecimal denominator = thicknessMm
                    .divide(BigDecimal.TEN, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(PI)
                    .multiply(diameterMm)
                    .multiply(diameterMm)
                    .divide(BigDecimal.valueOf(4), CALC_SCALE, RoundingMode.HALF_UP);
            return isMissingOrZero(denominator)
                    ? sample.getResultValue()
                    : weightG.divide(denominator, CALC_SCALE, RoundingMode.HALF_UP);
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            BigDecimal t1Mm = getDecimal(rawValues, "t1Mm", null);
            BigDecimal t2Mm = getDecimal(rawValues, "t2Mm", null);
            BigDecimal t3Mm = getDecimal(rawValues, "t3Mm", null);
            if (isMissingOrZero(t1Mm) || t2Mm == null || t3Mm == null || t1Mm.compareTo(t2Mm) == 0) {
                return sample.getResultValue();
            }
            BigDecimal compressionRate = t1Mm.subtract(t2Mm)
                    .divide(t1Mm, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            return compressionRate;
        }
        return null;
    }

    private String calculateItemResult(QmsIqcItemDO itemDO, List<QmsIqcSaveReqVO.IqcSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return JUDGMENT_PENDING;
        }
        if (ITEM_TYPE_DATE.equals(itemDO.getItemType())) {
            return calculateDateItemResult(itemDO, samples);
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(itemDO.getItemType())) {
            return QmsIqcQuantitativeJudgment.evaluate(itemDO, samples);
        }
        boolean hasPending = false;
        for (QmsIqcSaveReqVO.IqcSample sample : samples) {
            String sampleResult = sample.getSampleResult();
            if (JUDGMENT_NG.equals(sampleResult) || JUDGMENT_NG.equals(sample.getQualitativeValue())) {
                return JUDGMENT_NG;
            }
            BigDecimal value = firstNonNull(sample.getResultValue(), sample.getMeasuredValue());
            if (value != null) {
                if (itemDO.getMinValueLimit() != null && value.compareTo(itemDO.getMinValueLimit()) < 0) {
                    return JUDGMENT_NG;
                }
                if (itemDO.getMaxValueLimit() != null && value.compareTo(itemDO.getMaxValueLimit()) > 0) {
                    return JUDGMENT_NG;
                }
                continue;
            }
            if (!JUDGMENT_OK.equals(sampleResult) && !JUDGMENT_OK.equals(sample.getQualitativeValue())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private String calculateDateItemResult(QmsIqcItemDO itemDO, List<QmsIqcSaveReqVO.IqcSample> samples) {
        if (itemDO.getExpiryDays() == null || itemDO.getExpiryDays() < 0) {
            throw exception(HCIQC_DATE_RULE_MISSING, itemDO.getInspectionItem());
        }
        LocalDate evaluationDate = LocalDate.now(BUSINESS_ZONE_ID);
        boolean hasPending = false;
        boolean hasNg = false;
        for (QmsIqcSaveReqVO.IqcSample sample : samples) {
            LocalDate dateValue = sample.getDateValue();
            if (dateValue == null) {
                sample.setEvaluationDate(null);
                sample.setSampleResult(JUDGMENT_PENDING);
                hasPending = true;
                continue;
            }
            if (dateValue.isAfter(evaluationDate)) {
                throw exception(HCIQC_DATE_FUTURE_INVALID, itemDO.getInspectionItem());
            }
            sample.setEvaluationDate(evaluationDate);
            String sampleResult = isDateExpired(dateValue, evaluationDate, itemDO.getExpiryDays())
                    ? JUDGMENT_NG : JUDGMENT_OK;
            sample.setSampleResult(sampleResult);
            hasNg |= JUDGMENT_NG.equals(sampleResult);
        }
        if (hasNg) {
            return JUDGMENT_NG;
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    static boolean isDateExpired(LocalDate dateValue, LocalDate evaluationDate, int expiryDays) {
        return ChronoUnit.DAYS.between(dateValue, evaluationDate) > expiryDays;
    }

    private Map<String, Object> parseRawValues(String rawValuesJson) {
        if (!StringUtils.hasText(rawValuesJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> values = JsonUtils.parseObjectQuietly(rawValuesJson, MAP_TYPE);
        return values == null ? Collections.emptyMap() : values;
    }

    private BigDecimal getDecimal(Map<String, Object> values, String key, BigDecimal fallback) {
        BigDecimal value = toDecimal(values.get(key));
        return value == null ? fallback : value;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isMissingOrZero(BigDecimal value) {
        return value == null || BigDecimal.ZERO.compareTo(value) == 0;
    }

    private void recalculateStoredItems(Long iqcId, List<QmsIqcItemDO> items) {
        Map<Long, List<QmsIqcSampleDO>> samples = qmsIqcSampleMapper.selectListByIqcId(iqcId).stream()
                .collect(Collectors.groupingBy(QmsIqcSampleDO::getIqcItemId));
        for (QmsIqcItemDO item : items) {
            if (!ITEM_TYPE_QUANTITATIVE.equals(item.getItemType())) {
                continue;
            }
            List<QmsIqcSaveReqVO.IqcSample> itemSamples = BeanUtils.toBean(
                    samples.getOrDefault(item.getId(), Collections.emptyList()), QmsIqcSaveReqVO.IqcSample.class);
            applySampleStats(item, itemSamples);
            qmsIqcItemMapper.updateById(item);
            for (QmsIqcSaveReqVO.IqcSample sample : itemSamples) {
                qmsIqcSampleMapper.updateById(QmsIqcSampleDO.builder().id(sample.getId())
                        .resultValue(sample.getResultValue()).sampleResult(sample.getSampleResult()).build());
            }
        }
    }

    private String calculateFinalJudgment(List<QmsIqcItemDO> items) {
        validateItemsExist(items);
        boolean hasNg = false;
        boolean hasJudged = false;
        for (QmsIqcItemDO item : items) {
            if (QmsIqcQuantitativeJudgment.SKIP.equals(item.getItemResult())) {
                continue;
            }
            hasJudged = true;
            if (!JUDGMENT_OK.equals(item.getItemResult()) && !JUDGMENT_NG.equals(item.getItemResult())) {
                throw invalidParamException(item.getInspectionItem() + "："
                        + defaultIfBlank(item.getJudgmentReason(), "检测数据未填写完整"));
            }
            hasNg |= JUDGMENT_NG.equals(item.getItemResult());
        }
        return hasNg ? JUDGMENT_NG : (hasJudged ? JUDGMENT_OK : QmsIqcQuantitativeJudgment.SKIP);
    }

    private void ensureNgAbnormal(Long iqcId, String iqcNo, List<QmsIqcItemDO> items, String suggestedFlow) {
        List<QmsIqcAbnormalDO> existing = qmsIqcAbnormalMapper.selectListByIqcId(iqcId);
        if (!existing.isEmpty()) {
            return;
        }
        List<QmsIqcAbnormalDO> abnormalList = items.stream()
                .filter(item -> JUDGMENT_NG.equals(item.getItemResult()))
                .map(item -> QmsIqcAbnormalDO.builder()
                        .iqcId(iqcId)
                        .iqcNo(iqcNo)
                        .iqcItemId(item.getId())
                        .abnormalDesc("检验项判定不合格：" + item.getInspectionItem())
                        .suggestedFlow(defaultIfBlank(suggestedFlow, DEFAULT_NG_FLOW))
                        .processStatus(PROCESS_PENDING)
                        .build())
                .collect(Collectors.toList());
        if (!abnormalList.isEmpty()) {
            qmsIqcAbnormalMapper.insertBatch(abnormalList);
        }
    }

    private void fillDetails(QmsIqcRespVO respVO) {
        List<QmsIqcItemDO> items = qmsIqcItemMapper.selectListByIqcId(respVO.getId());
        List<QmsIqcSampleDO> samples = qmsIqcSampleMapper.selectListByIqcId(respVO.getId());
        Map<Long, List<QmsIqcRespVO.IqcSample>> sampleMap = BeanUtils.toBean(samples, QmsIqcRespVO.IqcSample.class)
                .stream()
                .collect(Collectors.groupingBy(QmsIqcRespVO.IqcSample::getIqcItemId));
        List<QmsIqcRespVO.IqcItem> itemRespList = BeanUtils.toBean(items, QmsIqcRespVO.IqcItem.class);
        itemRespList.forEach(item -> item.setSamples(sampleMap.getOrDefault(item.getId(), Collections.emptyList())));
        respVO.setItems(itemRespList);
        respVO.setAbnormals(BeanUtils.toBean(qmsIqcAbnormalMapper.selectListByIqcId(respVO.getId()), QmsIqcRespVO.IqcAbnormal.class));
        respVO.setReturnRecords(BeanUtils.toBean(qmsIqcReturnRecordMapper.selectListByIqcId(respVO.getId()),
                QmsIqcRespVO.IqcReturnRecord.class));
        fillStandardSelectionState(respVO, items);
    }

    private void fillStandardSelectionState(QmsIqcRespVO respVO, List<QmsIqcItemDO> snapshotItems) {
        boolean switchAllowed = isIqcStandardSwitchAllowed(respVO.getStatus());
        respVO.setStandardSwitchAllowed(switchAllowed);
        respVO.setStandardContentChanged(false);
        if (respVO.getStandardId() == null) {
            respVO.setStandardSelectionMessage(switchAllowed
                    ? "当前检验记录尚未挂接标准，请选择已审核标准后开始填写"
                    : null);
            return;
        }
        if (!switchAllowed && !STATUS_WAITING_CONFIRM.equals(respVO.getStatus())) {
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
        String snapshotHash = StringUtils.hasText(respVO.getStandardSnapshotHash())
                ? respVO.getStandardSnapshotHash()
                : qmsQualityStandardContentHashService.hashIqcSnapshotItems(snapshotItems);
        respVO.setStandardSnapshotHash(snapshotHash);
        String currentHash = qmsQualityStandardContentHashService.hashIqcStandardItems(currentItems);
        boolean changed = !Objects.equals(snapshotHash, currentHash);
        respVO.setStandardContentChanged(changed);
        if (!changed) {
            respVO.setStandardSelectionMessage(null);
            return;
        }
        if (!ENABLED.equals(current.getStatus()) || !AUDITED.equals(current.getAuditStatus())) {
            respVO.setStandardSelectionMessage(
                    "当前标准内容已变化但尚未审核启用，可等待审核后重新加载，或选择其他已审核标准");
        } else if (STATUS_WAITING_CONFIRM.equals(respVO.getStatus())) {
            respVO.setStandardSelectionMessage("标准内容已变化；当前检验单已提交待审核，退回后可重新选择标准");
        } else {
            respVO.setStandardSelectionMessage("当前已审核标准内容已变化，建议重新加载或重新选择标准");
        }
    }

    private QmsInspectionStandardCandidateRespVO buildStandardCandidate(IqcStandardCandidateMatch match,
                                                                        boolean recommended) {
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

    private void validateIqcStandardSwitchAllowed(QmsIqcOrderDO order) {
        if (!isIqcStandardSwitchAllowed(order.getStatus())) {
            if (STATUS_WAITING_CONFIRM.equals(order.getStatus())) {
                throw invalidParamException("当前IQC检验单已提交待审核，请先由审核人退回后再重新选择标准");
            }
            throw exception(HCIQC_FINISHED_LOCKED);
        }
    }

    private boolean isIqcStandardSwitchAllowed(String status) {
        return STATUS_PENDING.equals(status)
                || STATUS_INSPECTING.equals(status)
                || STATUS_SUSPENDED.equals(status);
    }

    private String buildIqcSwitchSnapshot(QmsIqcOrderDO order, List<QmsIqcItemDO> items) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("order", order);
        snapshot.put("items", items == null ? List.of() : items);
        snapshot.put("samples", qmsIqcSampleMapper.selectListByIqcId(order.getId()));
        snapshot.put("abnormals", qmsIqcAbnormalMapper.selectListByIqcId(order.getId()));
        snapshot.put("returnRecords", qmsIqcReturnRecordMapper.selectListByIqcId(order.getId()));
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

    private String appendRemark(String oldRemark, String addition) {
        if (!StringUtils.hasText(oldRemark)) {
            return addition;
        }
        return oldRemark.contains(addition) ? oldRemark : oldRemark + "；" + addition;
    }

    private QmsIqcStandardRespVO.StandardItem buildStandardItem(QmsQualityStandardItemDO item) {
        QmsIqcStandardRespVO.StandardItem resp = new QmsIqcStandardRespVO.StandardItem();
        resp.setStandardItemId(item.getId());
        resp.setInspectionItem(item.getInspectionItem());
        resp.setItemType(item.getItemType());
        resp.setExpiryDays(item.getExpiryDays());
        resp.setAttachmentEnabled(item.getAttachmentEnabled());
        resp.setTargetValue(item.getTargetValue());
        resp.setStandardDesc(item.getStandardDesc());
        resp.setUnit(item.getUnit());
        resp.setInspectionMethod(item.getInspectionMethod());
        resp.setTestFrequencyJudgement(item.getTestFrequencyJudgement());
        resp.setEntryRuleType(item.getEntryRuleType());
        resp.setRuleDescription(item.getRuleDescription());
        resp.setValueTemplate(item.getValueTemplate());
        resp.setValueTemplateName(resolveValueTemplateName(item.getValueTemplate(), item.getEntryRuleTemplateName()));
        resp.setTemplateParams(item.getTemplateParams());
        resp.setTestTool(item.getTestTool());
        resp.setSampleSize(item.getSampleSize());
        resp.setMinValueLimit(item.getMinValue());
        resp.setMaxValueLimit(item.getMaxValue());
        resp.setAvgMinLimit(item.getAvgMinLimit());
        resp.setAvgMaxLimit(item.getAvgMaxLimit());
        resp.setIsSpc(item.getIsSpc());
        resp.setSort(item.getSort());
        return resp;
    }

    private QmsIqcOrderDO validateIqcExists(Long id) {
        QmsIqcOrderDO entity = qmsIqcOrderMapper.selectById(id);
        if (entity == null) {
            throw exception(HCIQC_NOT_EXISTS);
        }
        return entity;
    }

    private QmsIqcOrderDO validateIqcRetentionRecord(Long id) {
        QmsIqcOrderDO order = validateIqcExists(id);
        if (!RETENTION_RETAINED.equals(order.getRetentionStatus())) {
            throw exception(HCIQC_NOT_EXISTS);
        }
        return order;
    }

    private void applyRetentionConfirmation(QmsIqcOrderDO updateObj, String retentionStatus,
                                            QmsIqcOrderDO sourceOrder) {
        String normalized = normalizeRetentionStatus(retentionStatus);
        if (!RETENTION_RETAINED.equals(normalized) && !RETENTION_NOT_RETAINED.equals(normalized)) {
            return;
        }
        if (sourceOrder != null
                && RETENTION_RETAINED.equals(normalizeRetentionStatus(sourceOrder.getRetentionStatus()))
                && RETENTION_NOT_RETAINED.equals(normalized)) {
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
            applyRetentionSampleRuleSnapshot(updateObj, sourceOrder);
            applyRetentionPeriodSnapshot(updateObj, sourceOrder, retentionConfirmTime);
        } else {
            updateObj.setRetentionDestroyStatus(null);
            updateObj.setRetentionMaterialCategoryId(null);
            updateObj.setRetentionMaterialCategoryName(null);
            updateObj.setRetentionQty(null);
            updateObj.setRetentionUnit(null);
            updateObj.setRetentionRuleDesc(null);
            updateObj.setRetentionPeriodValue(null);
            updateObj.setRetentionPeriodUnit(null);
            updateObj.setRetentionExpireTime(null);
        }
    }

    private String normalizeRetentionStatus(String retentionStatus) {
        return StringUtils.hasText(retentionStatus) ? retentionStatus.trim().toUpperCase(Locale.ROOT) : "";
    }

    private void applyRetentionSampleRuleSnapshot(QmsIqcOrderDO updateObj, QmsIqcOrderDO sourceOrder) {
        HcMaterialDO material = resolveRetentionMaterial(updateObj, sourceOrder);
        String categoryName = defaultIfBlank(
                material == null ? null : material.getMaterialCategoryName(),
                firstNotBlank(sourceOrder == null ? null : sourceOrder.getRetentionMaterialCategoryName(),
                        sourceOrder == null ? null : sourceOrder.getMaterialName(),
                        updateObj.getMaterialName()));
        RetentionSampleRule rule = resolveRetentionSampleRule(categoryName,
                firstNotBlank(sourceOrder == null ? null : sourceOrder.getMaterialName(), updateObj.getMaterialName()),
                firstNotBlank(sourceOrder == null ? null : sourceOrder.getMaterialCode(), updateObj.getMaterialCode()));
        updateObj.setRetentionMaterialCategoryId(material == null ? null : material.getMaterialCategoryId());
        updateObj.setRetentionMaterialCategoryName(defaultIfBlank(rule.materialCategoryName(), categoryName));
        updateObj.setRetentionQty(rule.qty());
        updateObj.setRetentionUnit(rule.unit());
        updateObj.setRetentionRuleDesc(rule.description());
    }

    private HcMaterialDO resolveRetentionMaterial(QmsIqcOrderDO updateObj, QmsIqcOrderDO sourceOrder) {
        Long materialId = updateObj.getMaterialId() != null
                ? updateObj.getMaterialId()
                : sourceOrder == null ? null : sourceOrder.getMaterialId();
        if (materialId != null) {
            HcMaterialDO material = hcMaterialMapper.selectById(materialId);
            if (material != null) {
                return material;
            }
        }
        String materialCode = defaultIfBlank(updateObj.getMaterialCode(), sourceOrder == null ? null : sourceOrder.getMaterialCode());
        if (!StringUtils.hasText(materialCode)) {
            return null;
        }
        return hcMaterialMapper.selectOne(new LambdaQueryWrapperX<HcMaterialDO>()
                .eq(HcMaterialDO::getMaterialCode, materialCode));
    }

    private RetentionSampleRule resolveRetentionSampleRule(String categoryName, String materialName,
                                                           String materialCode) {
        String text = (defaultIfBlank(categoryName, "") + " "
                + defaultIfBlank(materialName, "") + " "
                + defaultIfBlank(materialCode, "")).toUpperCase(Locale.ROOT);
        if (text.contains("砂纸")) {
            return new RetentionSampleRule("砂纸", BigDecimal.valueOf(5), RETENTION_SAMPLE_UNIT_METER);
        }
        if (text.contains("PET")) {
            return new RetentionSampleRule("PET", BigDecimal.ONE, RETENTION_SAMPLE_UNIT_METER);
        }
        if (text.contains("胶板")) {
            return new RetentionSampleRule("胶板", BigDecimal.ONE, RETENTION_SAMPLE_UNIT_METER);
        }
        if (text.contains("DMF")) {
            return new RetentionSampleRule("DMF", BigDecimal.ONE, RETENTION_SAMPLE_UNIT_LITER);
        }
        if (text.contains("助剂")) {
            return new RetentionSampleRule("助剂", BigDecimal.ONE, RETENTION_SAMPLE_UNIT_LITER);
        }
        return new RetentionSampleRule(defaultIfBlank(categoryName, "浆料"), BigDecimal.ONE, RETENTION_SAMPLE_UNIT_LITER);
    }

    private void applyRetentionPeriodSnapshot(QmsIqcOrderDO updateObj, QmsIqcOrderDO sourceOrder,
                                              LocalDateTime retentionConfirmTime) {
        updateObj.setRetentionPeriodValue(IQC_RETENTION_PERIOD_MONTHS);
        updateObj.setRetentionPeriodUnit(RETENTION_UNIT_MONTH);
        if (retentionConfirmTime != null) {
            updateObj.setRetentionExpireTime(retentionConfirmTime.plusMonths(IQC_RETENTION_PERIOD_MONTHS));
        }
    }

    private void validateEditable(QmsIqcOrderDO entity) {
        if (STATUS_COMPLETED.equals(entity.getStatus())
                || STATUS_REJECTED.equals(entity.getStatus())
                || STATUS_WAITING_CONFIRM.equals(entity.getStatus())
                || STATUS_CANCELED.equals(entity.getStatus())
                || STATUS_FINISHED_LEGACY.equals(entity.getStatus())) {
            throw exception(HCIQC_FINISHED_LOCKED);
        }
    }

    private void validateIqcNoUnique(Long id, String iqcNo) {
        QmsIqcOrderDO entity = qmsIqcOrderMapper.selectByIqcNo(iqcNo, id);
        if (entity != null) {
            throw exception(HCIQC_NO_EXISTS);
        }
    }

    private void validateIqcMaterialTraceFields(QmsIqcSaveReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getBatchNo())) {
            throw invalidParamException("批次号不能为空");
        }
        if (reqVO.getProductionDate() != null
                && reqVO.getExpiryDate() != null
                && reqVO.getExpiryDate().isBefore(reqVO.getProductionDate())) {
            throw invalidParamException("失效日期不能早于生产日期");
        }
    }

    private void applyIqcSupplierSnapshot(QmsIqcSaveReqVO reqVO) {
        if (reqVO.getSupplierId() == null) {
            throw invalidParamException("供应商不能为空");
        }
        MesSupplierDO supplier = mesSupplierMapper.selectById(reqVO.getSupplierId());
        if (supplier == null) {
            throw invalidParamException("供应商不存在或已删除，请重新选择");
        }
        if (!SUPPLIER_STATUS_QUALIFIED.equals(supplier.getStatus())) {
            throw invalidParamException("仅允许选择合格供应商");
        }
        String materialCode = trimToNull(reqVO.getMaterialCode());
        if (materialCode == null || !materialCode.equals(trimToNull(supplier.getMaterialCode()))) {
            throw invalidParamException("所选供应商未维护当前物料编码，请重新选择");
        }
        reqVO.setSupplierCode(supplier.getSupplierCode());
        reqVO.setSupplierName(supplier.getSupplierName());
    }

    private void validateItems(List<QmsIqcSaveReqVO.IqcItem> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCIQC_ITEMS_EMPTY);
        }
    }

    private void validateItemsExist(List<QmsIqcItemDO> items) {
        if (items == null || items.isEmpty()) {
            throw exception(HCIQC_ITEMS_EMPTY);
        }
    }

    private String generateIqcNo() {
        return qmsNoGeneratorService.generateNo(APPLY_TYPE_IQC);
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private static class ItemImportRow {
        private Long iqcId;
        private String iqcNo;
        private Long itemId;
        private Integer sampleSeq;
        private String inspectionItem;
        private String itemType;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        private LocalDate dateValue;
        private String remark;

        private boolean isEmpty() {
            return iqcId == null && itemId == null && sampleSeq == null
                    && measuredValue == null && dateValue == null && !StringUtils.hasText(qualitativeValue);
        }

        private QmsIqcSaveReqVO.IqcSample toSample() {
            QmsIqcSaveReqVO.IqcSample sample = new QmsIqcSaveReqVO.IqcSample();
            sample.setSampleSeq(sampleSeq);
            sample.setMeasuredValue(measuredValue);
            sample.setQualitativeValue(qualitativeValue);
            sample.setDateValue(dateValue);
            sample.setSampleResult("QUALITATIVE".equals(itemType) ? qualitativeValue : null);
            sample.setRemark(remark);
            return sample;
        }
    }

    private static class ItemImportPlan {
        private final String fileName;
        private final boolean previewOnly;
        private int totalCount;
        private int successCount;
        private int failureCount;
        private int warningCount;
        private final List<String> messages = new ArrayList<>();
        private final List<ItemImportRow> validRows = new ArrayList<>();
        private final List<Long> affectedItemIds = new ArrayList<>();

        private ItemImportPlan(String fileName, boolean previewOnly) {
            this.fileName = fileName;
            this.previewOnly = previewOnly;
        }

        private void addFailure(String message) {
            failureCount++;
            messages.add(message);
        }

        private QmsIqcImportRespVO toResp(QmsIqcRespVO record) {
            QmsIqcImportRespVO resp = new QmsIqcImportRespVO();
            resp.setFileName(fileName);
            resp.setPreviewOnly(previewOnly);
            resp.setTotalCount(totalCount);
            resp.setSuccessCount(successCount);
            resp.setFailureCount(failureCount);
            resp.setWarningCount(warningCount);
            resp.setStatus(failureCount > 0 ? IMPORT_STATUS_FAILED : IMPORT_STATUS_SUCCESS);
            resp.setValidateSummary("成功 " + successCount + " 行，失败 " + failureCount + " 行，警告 " + warningCount + " 行");
            resp.setMessages(messages);
            resp.setRecord(record);
            return resp;
        }
    }

    private String buildIqcAuditBizName(QmsIqcOrderDO order) {
        List<String> parts = new ArrayList<>();
        appendBizNamePart(parts, order.getMaterialName());
        appendBizNamePart(parts, order.getMaterialCode());
        appendBizNamePart(parts, order.getBatchNo());
        return parts.isEmpty() ? defaultIfBlank(order.getIqcNo(), "") : String.join(" / ", parts);
    }

    private void appendBizNamePart(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveValueTemplateName(String valueTemplate, String customTemplateName) {
        if (StringUtils.hasText(customTemplateName)) {
            return customTemplateName;
        }
        if (TEMPLATE_DENSITY_CALC.equals(valueTemplate)) {
            return "密度计算模板";
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(valueTemplate)) {
            return "压缩性能计算模板";
        }
        if ("SINGLE_VALUE".equals(valueTemplate)) {
            return "单值实测模板";
        }
        return null;
    }
}
