package cn.iocoder.yudao.module.mes.service.qms.coa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.FaiStandardItemResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.FaiStandardResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.MotherBatchPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.MotherBatchResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportActionReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportAuditReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportCorrectionReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportGenerateReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueApplyReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueCandidatePageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueCandidateResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.StandardItemPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.StandardItemResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateAuditReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateItemReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplatePageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateSaveReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateStatusReq;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaAuditLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaCorrectionLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportShippingRelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportSourceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaAuditLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaCorrectionLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportShippingRelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportSourceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaTemplateMapper;
import cn.iocoder.yudao.module.mes.service.hc.batchtrace.HcBatchTraceService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_ITEM_NOT_CORRECTABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_ITEM_VALUE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_MOTHER_BATCH_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_REFRESH_AFTER_CORRECTION;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_REPORT_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_SHIPPING_GATE_NOT_PASSED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_SHIPPING_NOTICE_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_STANDARD_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_TEMPLATE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_TEMPLATE_NOT_MATCHED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QMS_COA_TEMPLATE_STATUS_INVALID;

@Service
@Validated
public class QmsCoaServiceImpl implements QmsCoaService {

    private static final String TEMPLATE_DRAFT = "DRAFT";
    private static final String TEMPLATE_PENDING = "PENDING_REVIEW";
    private static final String TEMPLATE_APPROVED = "APPROVED";
    private static final String TEMPLATE_REJECTED = "REJECTED";
    private static final String REPORT_DRAFT = "DRAFT";
    private static final String REPORT_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String REPORT_PENDING_REVIEW = "PENDING_REVIEW";
    /** 历史状态，仅保留兼容已签发旧单据。 */
    private static final String REPORT_APPROVED = "APPROVED";
    private static final String REPORT_REJECTED = "REJECTED";
    private static final String REPORT_ISSUED = "ISSUED";
    private static final String REPORT_ARCHIVED = "ARCHIVED";
    private static final String REPORT_VOIDED = "VOIDED";
    private static final String BPM_COA_MODEL_ID = "qms-coa-report-model";
    private static final String BPM_COA_PROCESS_KEY = "qms_coa_report";
    private static final String BPM_NODE_FILL = "coa_fill";
    private static final String BPM_NODE_CONFIRM = "coa_confirm";
    private static final String BPM_NODE_REVIEW = "coa_review";
    private static final Object COA_NO_LOCK = new Object();
    private static final DateTimeFormatter NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Pattern SPEC_RANGE_PATTERN = Pattern.compile(
            "^(-?\\d+(?:\\.\\d+)?)~(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_TOLERANCE_PATTERN = Pattern.compile(
            "^(-?\\d+(?:\\.\\d+)?)±(\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_ASYMMETRIC_TOLERANCE_PATTERN = Pattern.compile(
            "^(-?\\d+(?:\\.\\d+)?)\\+(\\d+(?:\\.\\d+)?)/-(\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_LOWER_PATTERN = Pattern.compile(
            "^≥(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_STRICT_LOWER_PATTERN = Pattern.compile(
            "^>(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_UPPER_PATTERN = Pattern.compile(
            "^≤(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_STRICT_UPPER_PATTERN = Pattern.compile(
            "^<(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_EQUALS_PATTERN = Pattern.compile(
            "^=(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern SPEC_IN_PATTERN = Pattern.compile("(?i)^IN\\((.+)\\)$");
    private static final Pattern SPEC_NOT_IN_PATTERN = Pattern.compile("(?i)^NOTIN\\((.+)\\)$");

    @Resource private QmsCoaTemplateMapper templateMapper;
    @Resource private QmsCoaTemplateItemMapper templateItemMapper;
    @Resource private QmsCoaReportMapper reportMapper;
    @Resource private QmsCoaReportItemMapper reportItemMapper;
    @Resource private QmsCoaReportSourceMapper reportSourceMapper;
    @Resource private QmsCoaReportShippingRelMapper shippingRelMapper;
    @Resource private QmsCoaCorrectionLogMapper correctionLogMapper;
    @Resource private QmsCoaAuditLogMapper auditLogMapper;
    @Resource private QmsQualityStandardMapper qualityStandardMapper;
    @Resource private QmsQualityStandardItemMapper qualityStandardItemMapper;
    @Resource private QmsFaiOrderMapper faiOrderMapper;
    @Resource private QmsFaiItemMapper faiItemMapper;
    @Resource private QmsFaiSampleMapper faiSampleMapper;
    @Resource private QmsFqcOrderMapper fqcOrderMapper;
    @Resource private QmsFqcItemMapper fqcItemMapper;
    @Resource private HcFgShippingNoticeMapper shippingNoticeMapper;
    @Resource private HcFgShippingNoticeItemMapper shippingNoticeItemMapper;
    @Resource private HcBomMapper hcBomMapper;
    @Resource private HcBatchTraceService batchTraceService;
    @Resource private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource private BpmModelService bpmModelService;
    @Resource private BpmTaskService bpmTaskService;

    private volatile boolean coaBpmModelPublishedAfterStartup;

    @Override
    public PageResult<TemplateResp> getTemplatePage(TemplatePageReq reqVO) {
        PageResult<QmsCoaTemplateDO> page = templateMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(page.getList(), TemplateResp.class), page.getTotal());
    }

    @Override
    public TemplateResp getTemplate(Long id) {
        QmsCoaTemplateDO entity = validateTemplate(id);
        TemplateResp resp = BeanUtils.toBean(entity, TemplateResp.class);
        resp.setItems(BeanUtils.toBean(templateItemMapper.selectListByTemplateId(id), QmsCoaVO.TemplateItemResp.class));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(TemplateSaveReq reqVO) {
        validateAndFillTemplateProduct(reqVO);
        QmsCoaTemplateDO entity = BeanUtils.toBean(reqVO, QmsCoaTemplateDO.class);
        entity.setId(null);
        entity.setTemplateCode(firstNotBlank(reqVO.getTemplateCode(), uniqueNo("COAT")));
        validateTemplateUnique(entity.getTemplateCode(), entity.getVersionNo(), null);
        entity.setStatus(0);
        entity.setAuditStatus(TEMPLATE_DRAFT);
        entity.setTenantId(currentTenantId());
        templateMapper.insert(entity);
        saveTemplateItems(entity, reqVO.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(TemplateSaveReq reqVO) {
        QmsCoaTemplateDO old = validateTemplate(reqVO.getId());
        if (!Set.of(TEMPLATE_DRAFT, TEMPLATE_REJECTED).contains(old.getAuditStatus())) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        validateAndFillTemplateProduct(reqVO);
        String templateCode = firstNotBlank(reqVO.getTemplateCode(), old.getTemplateCode());
        validateTemplateUnique(templateCode, reqVO.getVersionNo(), old.getId());
        QmsCoaTemplateDO update = BeanUtils.toBean(reqVO, QmsCoaTemplateDO.class);
        update.setTemplateCode(templateCode);
        update.setAuditStatus(TEMPLATE_DRAFT);
        update.setStatus(0);
        update.setAuditorId(null);
        update.setAuditorName(null);
        update.setAuditTime(null);
        update.setAuditOpinion(null);
        templateMapper.updateById(update);
        templateItemMapper.deleteByTemplateId(old.getId());
        QmsCoaTemplateDO effective = BeanUtils.toBean(reqVO, QmsCoaTemplateDO.class);
        effective.setId(old.getId());
        effective.setTemplateCode(templateCode);
        effective.setVersionNo(reqVO.getVersionNo());
        effective.setTenantId(old.getTenantId());
        saveTemplateItems(effective, reqVO.getItems());
    }

    @Override
    public void submitTemplate(Long id) {
        QmsCoaTemplateDO template = validateTemplate(id);
        if (!Set.of(TEMPLATE_DRAFT, TEMPLATE_REJECTED).contains(template.getAuditStatus())
                || templateItemMapper.selectListByTemplateId(id).isEmpty()) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        QmsCoaTemplateDO update = new QmsCoaTemplateDO();
        update.setId(id);
        update.setAuditStatus(TEMPLATE_PENDING);
        templateMapper.updateById(update);
    }

    @Override
    public void auditTemplate(TemplateAuditReq reqVO) {
        QmsCoaTemplateDO template = validateTemplate(reqVO.getId());
        if (!TEMPLATE_PENDING.equals(template.getAuditStatus())) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        boolean pass = "PASS".equalsIgnoreCase(reqVO.getResult());
        QmsCoaTemplateDO update = new QmsCoaTemplateDO();
        update.setId(template.getId());
        update.setAuditStatus(pass ? TEMPLATE_APPROVED : TEMPLATE_REJECTED);
        update.setStatus(pass ? 1 : 0);
        update.setAuditorId(currentUserId());
        update.setAuditorName(currentUserName());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditOpinion(reqVO.getOpinion());
        templateMapper.updateById(update);
    }

    @Override
    public void changeTemplateStatus(TemplateStatusReq reqVO) {
        QmsCoaTemplateDO template = validateTemplate(reqVO.getId());
        if (Objects.equals(reqVO.getStatus(), 1) && !TEMPLATE_APPROVED.equals(template.getAuditStatus())) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        QmsCoaTemplateDO update = new QmsCoaTemplateDO();
        update.setId(template.getId());
        update.setStatus(reqVO.getStatus());
        templateMapper.updateById(update);
    }

    /**
     * 版本升级始终复制已审核启用模板的头、明细快照，新版本以草稿打开供维护，旧版本立即停用。
     * 已生成的 COA 只读取报告自身快照，不会受模板状态或新版本影响。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upgradeTemplate(Long id) {
        QmsCoaTemplateDO source = validateTemplate(id);
        if (!TEMPLATE_APPROVED.equals(source.getAuditStatus()) || !Objects.equals(source.getStatus(), 1)) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        List<TemplateItemReq> items = BeanUtils.toBean(
                templateItemMapper.selectListByTemplateId(source.getId()), TemplateItemReq.class);
        if (items.isEmpty()) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }

        QmsCoaTemplateDO upgraded = BeanUtils.toBean(source, QmsCoaTemplateDO.class);
        upgraded.setId(null);
        upgraded.setVersionNo(nextTemplateVersion(source.getVersionNo()));
        validateTemplateUnique(upgraded.getTemplateCode(), upgraded.getVersionNo(), null);
        upgraded.setStatus(0);
        upgraded.setAuditStatus(TEMPLATE_DRAFT);
        upgraded.setAuditorId(null);
        upgraded.setAuditorName(null);
        upgraded.setAuditTime(null);
        upgraded.setAuditOpinion(null);
        upgraded.setCreator(null);
        upgraded.setCreateTime(null);
        upgraded.setUpdater(null);
        upgraded.setUpdateTime(null);
        templateMapper.insert(upgraded);
        saveTemplateItems(upgraded, items);

        QmsCoaTemplateDO disableSource = new QmsCoaTemplateDO();
        disableSource.setId(source.getId());
        disableSource.setStatus(0);
        templateMapper.updateById(disableSource);
        return upgraded.getId();
    }

    /**
     * 模板型号和料号只接受产品 BOM 选择器能够选出的有效组合，服务端再次确认并回填名称快照，
     * 不能仅相信前端传入的编码或名称。
     */
    private void validateAndFillTemplateProduct(TemplateSaveReq reqVO) {
        HcBomDO bom = hcBomMapper.selectEnabledByMaterialCodeAndModelCode(
                reqVO.getMaterialCode().trim(), reqVO.getProductModelCode().trim());
        if (bom == null
                || !Objects.equals(bom.getProductMaterialId(), reqVO.getMaterialId())
                || !Objects.equals(bom.getProductModelId(), reqVO.getProductModelId())) {
            throw exception(QMS_COA_TEMPLATE_NOT_MATCHED);
        }
        reqVO.setMaterialCode(bom.getProductMaterialCode());
        reqVO.setMaterialName(bom.getProductMaterialName());
        reqVO.setProductModelCode(bom.getProductModelCode());
        reqVO.setProductModelName(bom.getProductModelName());
    }

    @Override
    public List<FaiStandardResp> getFaiStandardList(String keyword) {
        LambdaQueryWrapperX<QmsQualityStandardDO> wrapper = new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .eq(QmsQualityStandardDO::getApplyType, "FAI")
                .eq(QmsQualityStandardDO::getStatus, 1)
                .eq(QmsQualityStandardDO::getAuditStatus, 20);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(QmsQualityStandardDO::getStandardNo, keyword)
                    .or().like(QmsQualityStandardDO::getStandardName, keyword)
                    .or().like(QmsQualityStandardDO::getMaterialCode, keyword)
                    .or().like(QmsQualityStandardDO::getProductModelCode, keyword));
        }
        wrapper.orderByDesc(QmsQualityStandardDO::getId).last("LIMIT 100");
        List<QmsQualityStandardDO> standards = qualityStandardMapper.selectList(wrapper);
        List<FaiStandardResp> result = BeanUtils.toBean(standards, FaiStandardResp.class);
        for (FaiStandardResp standard : result) {
            standard.setItems(BeanUtils.toBean(qualityStandardItemMapper.selectListByStandardId(standard.getId()),
                    FaiStandardItemResp.class));
        }
        return result;
    }

    @Override
    public PageResult<StandardItemResp> getStandardItemPage(StandardItemPageReq reqVO) {
        LambdaQueryWrapperX<QmsQualityStandardDO> standardWrapper = new LambdaQueryWrapperX<>();
        standardWrapper.in(QmsQualityStandardDO::getApplyType, "FAI", "FQC");
        standardWrapper.eqIfPresent(QmsQualityStandardDO::getApplyType,
                StringUtils.hasText(reqVO.getStandardApplyType())
                        ? normalize(reqVO.getStandardApplyType()) : null);
        standardWrapper.eq(QmsQualityStandardDO::getProductModelCode, reqVO.getProductModelCode().trim());
        standardWrapper.eqIfPresent(QmsQualityStandardDO::getProcessCode, reqVO.getProcessCode());
        standardWrapper.eq(QmsQualityStandardDO::getStatus, 1);
        standardWrapper.eq(QmsQualityStandardDO::getAuditStatus, 20);
        standardWrapper.orderByAsc(QmsQualityStandardDO::getApplyType);
        standardWrapper.orderByAsc(QmsQualityStandardDO::getProcessCode);
        standardWrapper.orderByDesc(QmsQualityStandardDO::getId);
        List<QmsQualityStandardDO> standards = qualityStandardMapper.selectList(standardWrapper);
        String keyword = Objects.requireNonNullElse(reqVO.getKeyword(), "").trim().toLowerCase(Locale.ROOT);
        List<StandardItemResp> all = new ArrayList<>();
        for (QmsQualityStandardDO standard : standards) {
            for (QmsQualityStandardItemDO standardItem : qualityStandardItemMapper.selectListByStandardId(standard.getId())) {
                if (StringUtils.hasText(keyword)
                        && !containsIgnoreCase(standard.getStandardNo(), keyword)
                        && !containsIgnoreCase(standard.getStandardName(), keyword)
                        && !containsIgnoreCase(standardItem.getInspectionItem(), keyword)) {
                    continue;
                }
                StandardItemResp item = BeanUtils.toBean(standardItem, StandardItemResp.class);
                item.setStandardId(standard.getId());
                item.setStandardNo(standard.getStandardNo());
                item.setStandardName(standard.getStandardName());
                item.setStandardVersion(standard.getVersion());
                item.setStandardApplyType(standard.getApplyType());
                item.setProcessId(standard.getProcessId());
                item.setProcessCode(standard.getProcessCode());
                item.setProcessName(standard.getProcessName());
                item.setProductModelCode(standard.getProductModelCode());
                item.setProductModelName(standard.getProductModelName());
                all.add(item);
            }
        }
        int total = all.size();
        int pageNo = Math.max(1, Objects.requireNonNullElse(reqVO.getPageNo(), 1));
        int pageSize = Math.max(1, Objects.requireNonNullElse(reqVO.getPageSize(), 20));
        int from = Math.min((pageNo - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return new PageResult<>(all.subList(from, to), (long) total);
    }

    @Override
    public PageResult<ReportResp> getReportPage(ReportPageReq reqVO) {
        PageResult<QmsCoaReportDO> page = reportMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(page.getList(), ReportResp.class), page.getTotal());
    }

    @Override
    public PageResult<MotherBatchResp> getMotherBatchPage(MotherBatchPageReq reqVO) {
        QmsCoaTemplateDO template = validateTemplate(reqVO.getTemplateId());
        Map<String, MotherBatchResp> batches = new LinkedHashMap<>();
        LambdaQueryWrapperX<QmsFaiOrderDO> faiQuery = new LambdaQueryWrapperX<>();
        faiQuery.eq(QmsFaiOrderDO::getProductModel, template.getProductModelCode());
        faiQuery.isNotNull(QmsFaiOrderDO::getProductBatchNo);
        faiQuery.ne(QmsFaiOrderDO::getProductBatchNo, "");
        faiQuery.ne(QmsFaiOrderDO::getStatus, "CANCELED");
        faiQuery.orderByDesc(QmsFaiOrderDO::getQaTime);
        faiQuery.orderByDesc(QmsFaiOrderDO::getId);
        faiQuery.last("LIMIT 1000");
        for (QmsFaiOrderDO order : faiOrderMapper.selectList(faiQuery)) {
            mergeMotherBatch(batches, order.getProductBatchNo(), order.getMaterialCode(), order.getMaterialName(),
                    order.getProductModel(), firstNotNull(order.getQaTime(), order.getInspectionTime(), order.getSubmissionTime(), order.getCreateTime()));
        }
        LambdaQueryWrapperX<QmsFqcOrderDO> fqcQuery = new LambdaQueryWrapperX<>();
        fqcQuery.eq(QmsFqcOrderDO::getProductModel, template.getProductModelCode());
        fqcQuery.ne(QmsFqcOrderDO::getStatus, "CANCELED");
        fqcQuery.and(wrapper -> wrapper.isNotNull(QmsFqcOrderDO::getProductBatchNo)
                .ne(QmsFqcOrderDO::getProductBatchNo, "")
                .or()
                .isNotNull(QmsFqcOrderDO::getBatchNo)
                .ne(QmsFqcOrderDO::getBatchNo, ""));
        fqcQuery.orderByDesc(QmsFqcOrderDO::getQaTime);
        fqcQuery.orderByDesc(QmsFqcOrderDO::getId);
        fqcQuery.last("LIMIT 1000");
        for (QmsFqcOrderDO order : fqcOrderMapper.selectList(fqcQuery)) {
            mergeMotherBatch(batches, firstNotBlank(order.getProductBatchNo(), order.getBatchNo()),
                    order.getMaterialCode(), order.getMaterialName(), order.getProductModel(),
                    firstNotNull(order.getQaTime(), order.getInspectionTime(), order.getSubmissionTime(), order.getCreateTime()));
        }
        String keyword = Objects.requireNonNullElse(reqVO.getKeyword(), "").trim().toLowerCase(Locale.ROOT);
        List<MotherBatchResp> all = batches.values().stream()
                .filter(item -> !StringUtils.hasText(keyword)
                        || containsIgnoreCase(item.getProductionBatchNo(), keyword)
                        || containsIgnoreCase(item.getMaterialCode(), keyword)
                        || containsIgnoreCase(item.getMaterialName(), keyword))
                .sorted(Comparator.comparing(MotherBatchResp::getLatestInspectionTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(all, reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    public ReportResp getReport(Long id) {
        QmsCoaReportDO report = validateReport(id);
        ReportResp resp = BeanUtils.toBean(report, ReportResp.class);
        resp.setItems(BeanUtils.toBean(reportItemMapper.selectListByReportId(id), QmsCoaVO.ReportItemResp.class));
        resp.setSources(BeanUtils.toBean(reportSourceMapper.selectListByReportId(id), QmsCoaVO.ReportSourceResp.class));
        resp.setShippingRelations(BeanUtils.toBean(shippingRelMapper.selectListByReportId(id), QmsCoaVO.ShippingRelResp.class));
        resp.setCorrectionLogs(BeanUtils.toBean(correctionLogMapper.selectListByReportId(id), QmsCoaVO.CorrectionLogResp.class));
        resp.setAuditLogs(BeanUtils.toBean(auditLogMapper.selectListByReportId(id), QmsCoaVO.AuditLogResp.class));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateReport(ReportGenerateReq reqVO) {
        HcFgShippingNoticeDO notice = resolveShippingNotice(reqVO.getShippingNoticeId(), reqVO.getShippingNoticeNo());
        QmsCoaTemplateDO template = resolveTemplate(reqVO.getTemplateId(), notice);
        validateMotherBatch(template, reqVO.getProductionBatchNo());
        QmsCoaReportDO report;
        synchronized (COA_NO_LOCK) {
            report = buildReportHeader(reqVO, notice, template);
            reportMapper.insert(report);
        }
        report.setRootReportId(report.getId());
        reportMapper.updateById(report);
        initializeReportData(report, template, notice);
        addAuditLog(report, "GENERATE", null, REPORT_DRAFT, null, "人工选择模板和母批次后初始化COA草稿");
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshReport(Long id) {
        QmsCoaReportDO report = validateEditableReport(id);
        if (!correctionLogMapper.selectListByReportId(id).isEmpty()) {
            throw exception(QMS_COA_REPORT_REFRESH_AFTER_CORRECTION);
        }
        QmsCoaTemplateDO template = validateTemplate(report.getTemplateId());
        HcFgShippingNoticeDO notice = report.getShippingNoticeId() == null
                ? null : shippingNoticeMapper.selectById(report.getShippingNoticeId());
        reportItemMapper.deleteByReportId(id);
        reportSourceMapper.deleteByReportId(id);
        shippingRelMapper.deleteByReportId(id);
        initializeReportData(report, template, notice);
        addAuditLog(report, "REINITIALIZE", report.getReportStatus(), report.getReportStatus(), null,
                "重新初始化模板项目，未自动带入任何检验值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctReportItem(ReportCorrectionReq reqVO) {
        QmsCoaReportDO report = validateEditableReport(reqVO.getReportId());
        QmsCoaReportItemDO item = reportItemMapper.selectById(reqVO.getReportItemId());
        if (item == null || !Objects.equals(item.getReportId(), report.getId())) {
            throw exception(QMS_COA_REPORT_ITEM_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(item.getAllowCorrectionFlag())) {
            throw exception(QMS_COA_REPORT_ITEM_NOT_CORRECTABLE);
        }
        String displayValue = formatDisplayValue(reqVO.getCorrectedValue(), item.getDecimalPlaces());
        String result = firstNotBlank(reqVO.getCorrectedResult(), judgeValue(reqVO.getCorrectedValue(), item));
        QmsCoaCorrectionLogDO log = new QmsCoaCorrectionLogDO();
        log.setReportId(report.getId());
        log.setCoaNo(report.getCoaNo());
        log.setRevisionNo(report.getRevisionNo());
        log.setReportItemId(item.getId());
        log.setMetricCode(item.getMetricCode());
        log.setItemName(item.getItemNameCn());
        log.setSourceValue(item.getSourceValue());
        log.setBeforeActualValue(item.getActualValue());
        log.setAfterActualValue(reqVO.getCorrectedValue());
        log.setBeforeDisplayValue(item.getDisplayValue());
        log.setAfterDisplayValue(displayValue);
        log.setBeforeResult(item.getResult());
        log.setAfterResult(result);
        log.setCorrectionReason(reqVO.getCorrectionReason());
        log.setCorrectorId(currentUserId());
        log.setCorrectorName(currentUserName());
        log.setCorrectionTime(LocalDateTime.now());
        log.setSourceFaiId(item.getSourceFaiId());
        log.setSourceFaiNo(item.getSourceFaiNo());
        log.setSourceFaiItemId(item.getSourceFaiItemId());
        log.setTenantId(currentTenantId());
        correctionLogMapper.insert(log);

        QmsCoaReportItemDO update = new QmsCoaReportItemDO();
        update.setId(item.getId());
        update.setActualValue(reqVO.getCorrectedValue());
        update.setDisplayValue(displayValue);
        update.setResult(result);
        update.setCoaSpecText(reqVO.getCoaSpecText());
        update.setSourceDataCompleteFlag(true);
        update.setCorrectedFlag(true);
        update.setCorrectionCount(Objects.requireNonNullElse(item.getCorrectionCount(), 0) + 1);
        update.setLastCorrectionLogId(log.getId());
        update.setLastCorrectionReason(reqVO.getCorrectionReason());
        reportItemMapper.updateById(update);
        refreshReportSummary(report.getId());
        addAuditLog(report, "CORRECT", report.getReportStatus(), report.getReportStatus(),
                reqVO.getCorrectionReason(), "修正项目：" + item.getItemNameCn());
    }

    @Override
    public PageResult<ReportItemValueCandidateResp> getReportItemValueCandidatePage(
            ReportItemValueCandidatePageReq reqVO) {
        QmsCoaReportDO report = validateReport(reqVO.getReportId());
        QmsCoaReportItemDO item = validateReportItem(report, reqVO.getReportItemId());
        return page(loadReportItemValueCandidates(report, item), reqVO.getPageNo(), reqVO.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyReportItemValue(ReportItemValueApplyReq reqVO) {
        QmsCoaReportDO report = validateEditableReport(reqVO.getReportId());
        QmsCoaReportItemDO item = validateReportItem(report, reqVO.getReportItemId());
        QmsCoaReportItemDO update = new QmsCoaReportItemDO();
        update.setId(item.getId());
        String sourceType = firstNotBlank(item.getValueSourceType(), "PROCESS_INSPECTION");
        String actualValue;
        List<ReportItemValueCandidateResp> selected = List.of();
        if ("PROCESS_INSPECTION".equalsIgnoreCase(sourceType)) {
            if (reqVO.getCandidateKeys() == null || reqVO.getCandidateKeys().isEmpty()) {
                throw exception(QMS_COA_REPORT_ITEM_VALUE_INVALID);
            }
            Map<String, ReportItemValueCandidateResp> candidateMap = loadReportItemValueCandidates(report, item).stream()
                    .collect(Collectors.toMap(ReportItemValueCandidateResp::getCandidateKey, Function.identity(), (left, right) -> left));
            selected = reqVO.getCandidateKeys().stream().distinct().map(candidateMap::get).filter(Objects::nonNull).toList();
            if (selected.size() != reqVO.getCandidateKeys().stream().distinct().count()) {
                throw exception(QMS_COA_REPORT_ITEM_VALUE_INVALID);
            }
            String strategy = firstNotBlank(reqVO.getValueStrategy(), item.getValueStrategy(), "QA_AVG");
            actualValue = aggregateCandidateValue(strategy, selected);
            if (!StringUtils.hasText(actualValue)) throw exception(QMS_COA_REPORT_ITEM_VALUE_INVALID);
            ReportItemValueCandidateResp first = selected.get(0);
            update.setValueStrategy(strategy);
            update.setSourceType(first.getSourceType());
            update.setSourceOrderId(first.getSourceOrderId());
            update.setSourceOrderNo(first.getSourceOrderNo());
            update.setSourceOrderItemId(first.getSourceItemId());
            update.setSourceValue(actualValue);
            update.setRawValueJson(JsonUtils.toJsonString(selected));
            if ("FAI".equals(first.getSourceType())) {
                update.setSourceFaiId(first.getSourceOrderId());
                update.setSourceFaiNo(first.getSourceOrderNo());
                update.setSourceFaiItemId(first.getSourceItemId());
            }
        } else {
            boolean photo = "PHOTO_UPLOAD".equalsIgnoreCase(sourceType);
            if (photo && (reqVO.getAttachmentUrls() == null || reqVO.getAttachmentUrls().isEmpty())) {
                throw exception(QMS_COA_REPORT_ITEM_VALUE_INVALID);
            }
            actualValue = firstNotBlank(reqVO.getManualValue(), photo ? "见附件" : null);
            if (!StringUtils.hasText(actualValue)) throw exception(QMS_COA_REPORT_ITEM_VALUE_INVALID);
            update.setValueStrategy(photo ? "PHOTO" : "MANUAL");
            update.setSourceType(photo ? "PHOTO" : "MANUAL");
            update.setSourceValue(null);
            update.setRawValueJson(JsonUtils.toJsonString(Map.of(
                    "sourceType", photo ? "PHOTO" : "MANUAL",
                    "manualValue", actualValue,
                    "attachmentUrls", Objects.requireNonNullElse(reqVO.getAttachmentUrls(), List.of()))));
        }
        update.setAttachmentUrls(JsonUtils.toJsonString(Objects.requireNonNullElse(reqVO.getAttachmentUrls(), List.of())));
        update.setCoaSpecText(reqVO.getCoaSpecText());
        update.setActualValue(actualValue);
        update.setDisplayValue(formatDisplayValue(actualValue, item.getDecimalPlaces()));
        update.setResult(judgeValue(actualValue, item));
        update.setSourceDataCompleteFlag(true);
        reportItemMapper.updateById(update);
        refreshReportSummary(report.getId());
        addAuditLog(report, "APPLY_VALUE", report.getReportStatus(), report.getReportStatus(), null,
                "取值项目：" + item.getItemNameCn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(Long id) {
        QmsCoaReportDO report = validateEditableReport(id);
        refreshReportSummary(id);
        report = validateReport(id);
        if (!Boolean.TRUE.equals(report.getDataCompleteFlag())) {
            throw exception(QMS_COA_REPORT_INCOMPLETE);
        }
        String before = report.getReportStatus();
        QmsCoaReportDO update = new QmsCoaReportDO();
        update.setId(id);
        update.setReportStatus(REPORT_PENDING_CONFIRM);
        update.setDataSnapshotHash(calculateDataHash(id));
        update.setInspectorId(currentUserId());
        update.setInspectorName(currentUserName());
        update.setInspectorTime(LocalDateTime.now());
        reportMapper.updateById(update);
        QmsCoaReportDO activeReport = validateReport(id);
        String processInstanceId = activeReport.getProcessInstanceId();
        if (!StringUtils.hasText(processInstanceId)) {
            processInstanceId = startCoaBpmProcess(activeReport);
            QmsCoaReportDO processUpdate = new QmsCoaReportDO();
            processUpdate.setId(id);
            processUpdate.setProcessInstanceId(processInstanceId);
            reportMapper.updateById(processUpdate);
            activeReport.setProcessInstanceId(processInstanceId);
        }
        approveCoaBpmTask(activeReport, BPM_NODE_FILL, "检验人提交COA报告");
        addAuditLog(report, "SUBMIT", before, REPORT_PENDING_CONFIRM, null, "检验填写完成，提交COA确认");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReport(ReportAuditReq reqVO) {
        QmsCoaReportDO report = validateReport(reqVO.getId());
        if (!REPORT_PENDING_CONFIRM.equals(report.getReportStatus())) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        boolean pass = "PASS".equalsIgnoreCase(reqVO.getResult());
        String after = pass ? REPORT_PENDING_REVIEW : REPORT_REJECTED;
        QmsCoaReportDO update = new QmsCoaReportDO();
        update.setId(report.getId());
        update.setReportStatus(after);
        update.setConfirmerId(currentUserId());
        update.setConfirmerName(currentUserName());
        update.setConfirmTime(LocalDateTime.now());
        update.setConfirmOpinion(reqVO.getOpinion());
        reportMapper.updateById(update);
        QmsCoaReportDO activeReport = validateReport(report.getId());
        if (pass) {
            approveCoaBpmTask(activeReport, BPM_NODE_CONFIRM, firstNotBlank(reqVO.getOpinion(), "COA确认通过"));
        } else {
            returnCoaBpmTask(activeReport, BPM_NODE_CONFIRM, BPM_NODE_FILL,
                    firstNotBlank(reqVO.getOpinion(), "COA确认退回修改"));
        }
        addAuditLog(report, pass ? "CONFIRM" : "CONFIRM_RETURN", REPORT_PENDING_CONFIRM, after,
                reqVO.getOpinion(), pass ? "COA确认通过，流转审核确认" : "COA确认退回填写人修改");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReport(ReportAuditReq reqVO) {
        QmsCoaReportDO report = validateReport(reqVO.getId());
        if (!REPORT_PENDING_REVIEW.equals(report.getReportStatus())) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        boolean pass = "PASS".equalsIgnoreCase(reqVO.getResult());
        String after = pass ? REPORT_ARCHIVED : REPORT_REJECTED;
        QmsCoaReportDO update = new QmsCoaReportDO();
        update.setId(report.getId());
        update.setReportStatus(after);
        update.setReviewerId(currentUserId());
        update.setReviewerName(currentUserName());
        update.setReviewTime(LocalDateTime.now());
        update.setReviewOpinion(reqVO.getOpinion());
        if (pass) {
            String dataHash = calculateDataHash(report.getId());
            update.setDataSnapshotHash(dataHash);
            update.setVerificationCode(dataHash.substring(0, 16).toUpperCase(Locale.ROOT));
        }
        reportMapper.updateById(update);
        QmsCoaReportDO activeReport = validateReport(report.getId());
        if (pass) {
            approveCoaBpmTask(activeReport, BPM_NODE_REVIEW, firstNotBlank(reqVO.getOpinion(), "COA审核确认，归档"));
            ReportResp printData = getReport(report.getId());
            printData.setVerificationCode(activeReport.getVerificationCode());
            QmsCoaReportDO printUpdate = new QmsCoaReportDO();
            printUpdate.setId(report.getId());
            printUpdate.setPrintSnapshotHtml(buildPrintHtml(printData));
            printUpdate.setPrintSnapshotHash(sha256(printUpdate.getPrintSnapshotHtml()));
            reportMapper.updateById(printUpdate);
        } else {
            returnCoaBpmTask(activeReport, BPM_NODE_REVIEW, BPM_NODE_FILL,
                    firstNotBlank(reqVO.getOpinion(), "COA审核退回修改"));
        }
        addAuditLog(report, pass ? "ARCHIVE" : "REVIEW_RETURN", REPORT_PENDING_REVIEW, after,
                reqVO.getOpinion(), pass ? "COA审核确认并归档" : "COA审核退回填写人修改");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueReport(ReportActionReq reqVO) {
        QmsCoaReportDO report = validateReport(reqVO.getId());
        if (!REPORT_APPROVED.equals(report.getReportStatus())) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        validateShippingGate(report.getId());
        String dataHash = calculateDataHash(report.getId());
        String verificationCode = dataHash.substring(0, 16).toUpperCase(Locale.ROOT);
        String issuerName = currentUserName();
        LocalDateTime issuedTime = LocalDateTime.now();
        ReportResp printData = getReport(report.getId());
        printData.setVerificationCode(verificationCode);
        printData.setIssuedByName(issuerName);
        printData.setIssuedTime(issuedTime);
        String html = buildPrintHtml(printData);
        String htmlHash = sha256(html);
        QmsCoaReportDO update = new QmsCoaReportDO();
        update.setId(report.getId());
        update.setReportStatus(REPORT_ISSUED);
        update.setDataSnapshotHash(dataHash);
        update.setPrintSnapshotHtml(html);
        update.setPrintSnapshotHash(htmlHash);
        update.setVerificationCode(verificationCode);
        update.setIssuedById(currentUserId());
        update.setIssuedByName(issuerName);
        update.setIssuedTime(issuedTime);
        reportMapper.updateById(update);
        addAuditLog(report, "ISSUE", REPORT_APPROVED, REPORT_ISSUED, reqVO.getReason(), "签发COA");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRevision(ReportActionReq reqVO) {
        QmsCoaReportDO source = validateReport(reqVO.getId());
        if (!Set.of(REPORT_APPROVED, REPORT_ISSUED, REPORT_ARCHIVED, REPORT_VOIDED).contains(source.getReportStatus())) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        Long rootId = Objects.requireNonNullElse(source.getRootReportId(), source.getId());
        QmsCoaReportDO latest = reportMapper.selectLatestByRootId(rootId);
        QmsCoaReportDO revision = BeanUtils.toBean(source, QmsCoaReportDO.class);
        revision.setId(null);
        revision.setPreviousReportId(source.getId());
        revision.setRootReportId(rootId);
        revision.setRevisionNo(Objects.requireNonNullElse(latest == null ? null : latest.getRevisionNo(), source.getRevisionNo()) + 1);
        revision.setReportStatus(REPORT_DRAFT);
        revision.setProcessInstanceId(null);
        revision.setInspectorId(null);
        revision.setInspectorName(null);
        revision.setInspectorTime(null);
        revision.setConfirmerId(null);
        revision.setConfirmerName(null);
        revision.setConfirmTime(null);
        revision.setConfirmOpinion(null);
        revision.setReviewerId(null);
        revision.setReviewerName(null);
        revision.setReviewTime(null);
        revision.setReviewOpinion(null);
        revision.setIssuedById(null);
        revision.setIssuedByName(null);
        revision.setIssuedTime(null);
        revision.setVoidedById(null);
        revision.setVoidedByName(null);
        revision.setVoidedTime(null);
        revision.setVoidReason(null);
        revision.setDataSnapshotHash(null);
        revision.setPrintSnapshotHtml(null);
        revision.setPrintSnapshotHash(null);
        revision.setPdfFileUrl(null);
        revision.setPdfFileHash(null);
        revision.setVerificationCode(null);
        revision.setRemark(appendRemark(source.getRemark(), "升版原因：" + firstNotBlank(reqVO.getReason(), "报告修订")));
        reportMapper.insert(revision);
        cloneReportChildren(source.getId(), revision);
        addAuditLog(revision, "REVISION", source.getReportStatus(), REPORT_DRAFT, reqVO.getReason(),
                "从 " + source.getCoaNo() + " R" + source.getRevisionNo() + " 创建新版本");
        return revision.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidReport(ReportActionReq reqVO) {
        QmsCoaReportDO report = validateReport(reqVO.getId());
        if (!Set.of(REPORT_APPROVED, REPORT_ISSUED, REPORT_ARCHIVED).contains(report.getReportStatus())
                || !StringUtils.hasText(reqVO.getReason())) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        QmsCoaReportDO update = new QmsCoaReportDO();
        update.setId(report.getId());
        update.setReportStatus(REPORT_VOIDED);
        update.setVoidedById(currentUserId());
        update.setVoidedByName(currentUserName());
        update.setVoidedTime(LocalDateTime.now());
        update.setVoidReason(reqVO.getReason());
        reportMapper.updateById(update);
        addAuditLog(report, "VOID", report.getReportStatus(), REPORT_VOIDED, reqVO.getReason(), "作废COA");
    }

    private void mergeMotherBatch(Map<String, MotherBatchResp> batches, String batchNo, String materialCode,
                                  String materialName, String productModelCode, LocalDateTime inspectionTime) {
        if (!StringUtils.hasText(batchNo)) return;
        MotherBatchResp summary = batches.computeIfAbsent(batchNo.trim(), key -> {
            MotherBatchResp result = new MotherBatchResp();
            result.setProductionBatchNo(key);
            result.setMaterialCode(materialCode);
            result.setMaterialName(materialName);
            result.setProductModelCode(productModelCode);
            result.setInspectionCount(0);
            return result;
        });
        summary.setInspectionCount(Objects.requireNonNullElse(summary.getInspectionCount(), 0) + 1);
        if (inspectionTime != null && (summary.getLatestInspectionTime() == null
                || inspectionTime.isAfter(summary.getLatestInspectionTime()))) {
            summary.setLatestInspectionTime(inspectionTime);
        }
        LocalDate manufactureDate = inspectionTime == null ? null : inspectionTime.toLocalDate();
        if (manufactureDate != null && (summary.getManufactureDate() == null
                || manufactureDate.isBefore(summary.getManufactureDate()))) {
            summary.setManufactureDate(manufactureDate);
        }
    }

    private void validateMotherBatch(QmsCoaTemplateDO template, String productionBatchNo) {
        MotherBatchPageReq pageReq = new MotherBatchPageReq();
        pageReq.setTemplateId(template.getId());
        pageReq.setPageNo(1);
        pageReq.setPageSize(1000);
        boolean found = getMotherBatchPage(pageReq).getList().stream()
                .anyMatch(batch -> equalsIgnoreCase(batch.getProductionBatchNo(), productionBatchNo));
        if (!found) {
            throw exception(QMS_COA_MOTHER_BATCH_NOT_FOUND);
        }
    }

    private <T> PageResult<T> page(List<T> all, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = Math.max(1, Objects.requireNonNullElse(pageNo, 1));
        int normalizedPageSize = Math.max(1, Objects.requireNonNullElse(pageSize, 20));
        int from = Math.min((normalizedPageNo - 1) * normalizedPageSize, all.size());
        int to = Math.min(from + normalizedPageSize, all.size());
        return new PageResult<>(all.subList(from, to), (long) all.size());
    }

    private String startCoaBpmProcess(QmsCoaReportDO report) {
        Long loginUserId = currentUserId();
        if (loginUserId == null) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        deployBuiltInCoaBpmModelOnce(loginUserId);
        BpmProcessInstanceCreateReqDTO reqDTO = new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_COA_PROCESS_KEY)
                .setBusinessKey(String.valueOf(report.getId()))
                .setVariables(buildCoaBpmVariables(report));
        return bpmProcessInstanceApi.createProcessInstance(loginUserId, reqDTO);
    }

    private void deployBuiltInCoaBpmModelOnce(Long operatorUserId) {
        if (coaBpmModelPublishedAfterStartup) {
            return;
        }
        synchronized (this) {
            if (coaBpmModelPublishedAfterStartup) {
                return;
            }
            bpmModelService.deployModel(operatorUserId, BPM_COA_MODEL_ID);
            coaBpmModelPublishedAfterStartup = true;
        }
    }

    private void approveCoaBpmTask(QmsCoaReportDO report, String taskDefinitionKey, String reason) {
        Long loginUserId = currentUserId();
        Task task = resolveCurrentCoaBpmTask(report, taskDefinitionKey, loginUserId);
        BpmTaskApproveReqVO reqVO = new BpmTaskApproveReqVO()
                .setId(task.getId())
                .setReason(reason)
                .setVariables(buildCoaBpmVariables(report));
        bpmTaskService.approveTask(loginUserId, reqVO);
    }

    private void returnCoaBpmTask(QmsCoaReportDO report, String currentTaskDefinitionKey,
                                  String targetTaskDefinitionKey, String reason) {
        Long loginUserId = currentUserId();
        Task task = resolveCurrentCoaBpmTask(report, currentTaskDefinitionKey, loginUserId);
        BpmTaskReturnReqVO reqVO = new BpmTaskReturnReqVO()
                .setId(task.getId())
                .setTargetTaskDefinitionKey(targetTaskDefinitionKey)
                .setReason(reason);
        bpmTaskService.returnTask(loginUserId, reqVO);
    }

    private Task resolveCurrentCoaBpmTask(QmsCoaReportDO report, String taskDefinitionKey, Long loginUserId) {
        if (report == null || !StringUtils.hasText(report.getProcessInstanceId()) || loginUserId == null) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        return bpmTaskService.getRunningTaskListByProcessInstanceId(
                        report.getProcessInstanceId(), true, taskDefinitionKey)
                .stream()
                .filter(task -> Objects.equals(parseBpmTaskAssignee(task), loginUserId))
                .findFirst()
                .orElseThrow(() -> exception(QMS_COA_REPORT_STATUS_INVALID));
    }

    private Long parseBpmTaskAssignee(Task task) {
        if (task == null || !StringUtils.hasText(task.getAssignee())) {
            return null;
        }
        try {
            return Long.valueOf(task.getAssignee());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> buildCoaBpmVariables(QmsCoaReportDO report) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("coaReportId", report.getId());
        variables.put("coaNo", report.getCoaNo());
        variables.put("productModelCode", report.getProductModelCode());
        variables.put("materialCode", report.getMaterialCode());
        variables.put("productionBatchNo", report.getProductionBatchNo());
        variables.put("reportStatus", report.getReportStatus());
        variables.put("inspectorId", report.getInspectorId());
        return variables;
    }

    private QmsCoaReportItemDO validateReportItem(QmsCoaReportDO report, Long reportItemId) {
        QmsCoaReportItemDO item = reportItemMapper.selectById(reportItemId);
        if (item == null || !Objects.equals(item.getReportId(), report.getId())) {
            throw exception(QMS_COA_REPORT_ITEM_NOT_EXISTS);
        }
        return item;
    }

    private List<ReportItemValueCandidateResp> loadReportItemValueCandidates(QmsCoaReportDO report,
                                                                                QmsCoaReportItemDO reportItem) {
        if (!"PROCESS_INSPECTION".equalsIgnoreCase(reportItem.getValueSourceType())) return List.of();
        List<ReportItemValueCandidateResp> result = new ArrayList<>();
        LambdaQueryWrapperX<QmsFaiOrderDO> faiQuery = new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getProductBatchNo, report.getProductionBatchNo())
                .eqIfPresent(QmsFaiOrderDO::getProductModel, report.getProductModelCode())
                .eq(QmsFaiOrderDO::getStatus, "COMPLETED")
                .orderByDesc(QmsFaiOrderDO::getQaTime)
                .orderByDesc(QmsFaiOrderDO::getId);
        for (QmsFaiOrderDO order : faiOrderMapper.selectList(faiQuery)) {
            if (!matchesReportProcess(reportItem, order.getStandardId(), order.getOperationCode(), order.getProcessCategory())) continue;
            for (QmsFaiItemDO item : faiItemMapper.selectListByFaiId(order.getId())) {
                if (!matchesReportMetric(reportItem, item.getStandardItemId(), item.getMetricCode(), item.getSheetMetricCode(),
                        item.getInspectionItem())) continue;
                result.add(buildFaiCandidate(order, item));
            }
        }
        LambdaQueryWrapperX<QmsFqcOrderDO> fqcQuery = new LambdaQueryWrapperX<>();
        fqcQuery.eqIfPresent(QmsFqcOrderDO::getProductModel, report.getProductModelCode());
        fqcQuery.eq(QmsFqcOrderDO::getStatus, "COMPLETED");
        fqcQuery.and(wrapper -> wrapper.eq(QmsFqcOrderDO::getProductBatchNo, report.getProductionBatchNo())
                .or().eq(QmsFqcOrderDO::getBatchNo, report.getProductionBatchNo()));
        fqcQuery.orderByDesc(QmsFqcOrderDO::getQaTime);
        fqcQuery.orderByDesc(QmsFqcOrderDO::getId);
        for (QmsFqcOrderDO order : fqcOrderMapper.selectList(fqcQuery)) {
            if (!matchesReportProcess(reportItem, order.getStandardId(), order.getOperationCode(), order.getSourceOperationCode())) continue;
            for (QmsFqcItemDO item : fqcItemMapper.selectListByFqcId(order.getId())) {
                if (!matchesReportMetric(reportItem, item.getStandardItemId(), item.getMetricCode(), item.getSheetMetricCode(),
                        item.getInspectionItem())) continue;
                result.add(buildFqcCandidate(order, item));
            }
        }
        return result.stream().sorted(Comparator.comparing(ReportItemValueCandidateResp::getInspectionTime,
                Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    private boolean matchesReportProcess(QmsCoaReportItemDO reportItem, Long standardId, String... processCodes) {
        if (reportItem.getSourceStandardId() != null && !Objects.equals(reportItem.getSourceStandardId(), standardId)) {
            return false;
        }
        if (!StringUtils.hasText(reportItem.getSourceProcessCode())) return true;
        return java.util.Arrays.stream(processCodes).anyMatch(code -> equalsIgnoreCase(reportItem.getSourceProcessCode(), code));
    }

    private boolean matchesReportMetric(QmsCoaReportItemDO reportItem, Long standardItemId, String... itemKeys) {
        if (reportItem.getSourceStandardItemId() != null) {
            return Objects.equals(reportItem.getSourceStandardItemId(), standardItemId);
        }
        return java.util.Arrays.stream(itemKeys).anyMatch(key -> equalsIgnoreCase(reportItem.getMetricCode(), key)
                || equalsIgnoreCase(reportItem.getItemNameCn(), key));
    }

    private ReportItemValueCandidateResp buildFaiCandidate(QmsFaiOrderDO order, QmsFaiItemDO item) {
        List<QmsFaiSampleDO> samples = faiSampleMapper.selectListByFaiId(order.getId()).stream()
                .filter(sample -> Objects.equals(sample.getFaiItemId(), item.getId())).toList();
        ReportItemValueCandidateResp result = new ReportItemValueCandidateResp();
        result.setCandidateKey("FAI:" + order.getId() + ":" + item.getId());
        result.setSourceType("FAI");
        result.setSourceOrderId(order.getId());
        result.setSourceOrderNo(order.getFaiNo());
        result.setSourceItemId(item.getId());
        result.setSourceBatchNo(order.getProductBatchNo());
        result.setProcessCode(firstNotBlank(order.getOperationCode(), order.getProcessCategory()));
        result.setProcessName(order.getOperationName());
        result.setStandardNo(order.getStandardNo());
        result.setStandardVersion(order.getStandardVersion());
        result.setInspectionItem(item.getInspectionItem());
        result.setAverageValue(firstNotNull(item.getQaAvg(), item.getCalculatedAvg()));
        result.setMinValue(firstNotNull(item.getQaMin(), item.getCalculatedMin()));
        result.setMaxValue(firstNotNull(item.getQaMax(), item.getCalculatedMax()));
        result.setResult(firstNotBlank(item.getQaResult(), order.getJudgment()));
        result.setActualValue(firstNotBlank(decimalText(result.getAverageValue()), result.getResult()));
        result.setRawValuesJson(JsonUtils.toJsonString(samples.stream().map(this::sampleValue).toList()));
        result.setInspectionTime(firstNotNull(item.getQaTime(), order.getQaTime(), order.getInspectionTime()));
        return result;
    }

    private ReportItemValueCandidateResp buildFqcCandidate(QmsFqcOrderDO order, QmsFqcItemDO item) {
        ReportItemValueCandidateResp result = new ReportItemValueCandidateResp();
        result.setCandidateKey("FQC:" + order.getId() + ":" + item.getId());
        result.setSourceType("FQC");
        result.setSourceOrderId(order.getId());
        result.setSourceOrderNo(order.getFqcNo());
        result.setSourceItemId(item.getId());
        result.setSourceBatchNo(firstNotBlank(order.getProductBatchNo(), order.getBatchNo()));
        result.setProcessCode(firstNotBlank(order.getOperationCode(), order.getSourceOperationCode()));
        result.setProcessName(firstNotBlank(order.getOperationName(), order.getSourceOperationName()));
        result.setStandardNo(order.getStandardNo());
        result.setStandardVersion(order.getStandardVersion());
        result.setInspectionItem(item.getInspectionItem());
        result.setAverageValue(firstNotNull(item.getQaAvg(), item.getCalculatedAvg(), item.getAverageValue()));
        result.setMinValue(firstNotNull(item.getQaMin(), item.getCalculatedMin(), item.getMinValue()));
        result.setMaxValue(firstNotNull(item.getQaMax(), item.getCalculatedMax(), item.getMaxValue()));
        result.setResult(firstNotBlank(item.getQaResult(), item.getItemResult(), order.getJudgment()));
        result.setActualValue(firstNotBlank(decimalText(result.getAverageValue()), item.getActualValue(), result.getResult()));
        Map<String, Object> rawValues = new LinkedHashMap<>();
        rawValues.put("actualValue", item.getActualValue());
        rawValues.put("average", decimalText(result.getAverageValue()));
        rawValues.put("min", decimalText(result.getMinValue()));
        rawValues.put("max", decimalText(result.getMaxValue()));
        rawValues.put("result", result.getResult());
        result.setRawValuesJson(JsonUtils.toJsonString(rawValues));
        result.setInspectionTime(firstNotNull(item.getQaTime(), order.getQaTime(), order.getInspectionTime()));
        return result;
    }

    private String aggregateCandidateValue(String strategy, List<ReportItemValueCandidateResp> candidates) {
        String normalized = firstNotBlank(strategy, "QA_AVG").toUpperCase(Locale.ROOT);
        List<BigDecimal> values = candidates.stream().map(ReportItemValueCandidateResp::getActualValue)
                .map(this::toDecimal).filter(Objects::nonNull).toList();
        if ("QA_RESULT".equals(normalized)) {
            return candidates.stream().map(ReportItemValueCandidateResp::getResult).filter(StringUtils::hasText).findFirst()
                    .orElseGet(() -> candidates.get(0).getActualValue());
        }
        if ("LATEST_SAMPLE".equals(normalized)) {
            return candidates.stream().max(Comparator.comparing(ReportItemValueCandidateResp::getInspectionTime,
                    Comparator.nullsFirst(Comparator.naturalOrder()))).map(ReportItemValueCandidateResp::getActualValue).orElse(null);
        }
        if (values.isEmpty()) return candidates.get(0).getActualValue();
        return switch (normalized) {
            case "QA_MIN" -> decimalText(values.stream().min(BigDecimal::compareTo).orElse(null));
            case "QA_MAX" -> decimalText(values.stream().max(BigDecimal::compareTo).orElse(null));
            case "VARIANCE" -> decimalText(variance(values));
            default -> decimalText(average(values));
        };
    }

    private BigDecimal variance(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        BigDecimal mean = average(values);
        BigDecimal total = values.stream().map(value -> value.subtract(mean).pow(2)).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal toDecimal(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void saveTemplateItems(QmsCoaTemplateDO template, List<TemplateItemReq> items) {
        Set<String> metricCodes = new HashSet<>();
        List<QmsCoaTemplateItemDO> entities = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            TemplateItemReq itemReq = items.get(index);
            String metricCode = itemReq.getMetricCode().trim().toUpperCase(Locale.ROOT);
            if (!metricCodes.add(metricCode)) {
                throw exception(QMS_COA_STANDARD_ITEM_INVALID);
            }
            QmsCoaTemplateItemDO item = BeanUtils.toBean(itemReq, QmsCoaTemplateItemDO.class);
            item.setId(null);
            item.setTemplateId(template.getId());
            item.setTemplateCode(template.getTemplateCode());
            item.setTemplateVersion(template.getVersionNo());
            item.setMetricCode(metricCode);
            item.setValueSourceType(firstNotBlank(item.getValueSourceType(),
                    "FIXED".equalsIgnoreCase(item.getValueStrategy()) ? "MANUAL_ENTRY" : "PROCESS_INSPECTION"));
            item.setValueStrategy(firstNotBlank(item.getValueStrategy(),
                    "PROCESS_INSPECTION".equalsIgnoreCase(item.getValueSourceType()) ? "QA_AVG" : "MANUAL"));
            item.setSpecSource(firstNotBlank(item.getSpecSource(), "FAI_SNAPSHOT"));
            item.setDecimalPlaces(Objects.requireNonNullElse(item.getDecimalPlaces(), 4));
            item.setRequiredFlag(!Boolean.FALSE.equals(item.getRequiredFlag()));
            item.setAllowCorrectionFlag(!Boolean.FALSE.equals(item.getAllowCorrectionFlag()));
            item.setCoaDisplayFlag(!Boolean.FALSE.equals(item.getCoaDisplayFlag()));
            item.setSortNo(item.getSortNo() == null ? (index + 1) * 10 : item.getSortNo());
            fillStandardItemSnapshot(item, template);
            applySpecLimits(item);
            item.setTenantId(template.getTenantId());
            entities.add(item);
        }
        templateItemMapper.insertBatch(entities);
    }

    private void fillStandardItemSnapshot(QmsCoaTemplateItemDO item, QmsCoaTemplateDO template) {
        if (!"PROCESS_INSPECTION".equalsIgnoreCase(item.getValueSourceType())) {
            clearInspectionSource(item);
            return;
        }
        if (item.getSourceStandardItemId() == null) {
            throw exception(QMS_COA_STANDARD_ITEM_INVALID);
        }
        QmsQualityStandardItemDO standardItem = qualityStandardItemMapper.selectById(item.getSourceStandardItemId());
        if (standardItem == null || !Set.of("FAI", "FQC").contains(normalize(standardItem.getApplyType()))) {
            throw exception(QMS_COA_STANDARD_ITEM_INVALID);
        }
        QmsQualityStandardDO standard = qualityStandardMapper.selectById(standardItem.getStandardId());
        if (standard == null || !Objects.equals(standard.getStatus(), 1) || !Objects.equals(standard.getAuditStatus(), 20)) {
            throw exception(QMS_COA_STANDARD_ITEM_INVALID);
        }
        if (!equalsIgnoreCase(template.getProductModelCode(), standard.getProductModelCode())) {
            throw exception(QMS_COA_STANDARD_ITEM_INVALID);
        }
        item.setSourceStandardId(standard.getId());
        item.setSourceStandardNo(standard.getStandardNo());
        item.setSourceStandardVersion(standard.getVersion());
        item.setSourceStandardApplyType(normalize(standard.getApplyType()));
        item.setSourceInspectionItem(standardItem.getInspectionItem());
        item.setSourceItemType(standardItem.getItemType());
        item.setSourceProcessId(standard.getProcessId());
        item.setSourceProcessCode(standard.getProcessCode());
        item.setSourceProcessName(standard.getProcessName());
        if (!StringUtils.hasText(item.getSpecText())) item.setSpecText(standardItem.getStandardDesc());
        if (item.getTargetValue() == null) item.setTargetValue(standardItem.getTargetValue());
        if (item.getLowerLimit() == null) item.setLowerLimit(standardItem.getMinValue());
        if (item.getUpperLimit() == null) item.setUpperLimit(standardItem.getMaxValue());
        if (!StringUtils.hasText(item.getUnit())) item.setUnit(standardItem.getUnit());
        if (!StringUtils.hasText(item.getInspectionMethod())) item.setInspectionMethod(standardItem.getInspectionMethod());
    }

    private void clearInspectionSource(QmsCoaTemplateItemDO item) {
        item.setSourceProcessId(null);
        item.setSourceProcessCode(null);
        item.setSourceProcessName(null);
        item.setSourceStandardId(null);
        item.setSourceStandardNo(null);
        item.setSourceStandardVersion(null);
        item.setSourceStandardApplyType(null);
        item.setSourceStandardItemId(null);
        item.setSourceInspectionItem(null);
        item.setSourceItemType(null);
    }

    /**
     * 模板内控 Spec 是录入和展示字段；保存时将可识别的数值写法同步为判定上下限。
     * 目标值是过程目标，不单独作为零公差判定条件；需判定偏差时使用“目标值±公差”。
     */
    private void applySpecLimits(QmsCoaTemplateItemDO item) {
        if (!StringUtils.hasText(item.getSpecText())) return;
        String spec = normalizeSpecFormula(item.getSpecText());
        Matcher rangeMatcher = SPEC_RANGE_PATTERN.matcher(spec);
        if (rangeMatcher.find()) {
            item.setLowerLimit(new BigDecimal(rangeMatcher.group(1)));
            item.setUpperLimit(new BigDecimal(rangeMatcher.group(2)));
            return;
        }
        Matcher toleranceMatcher = SPEC_TOLERANCE_PATTERN.matcher(spec);
        if (toleranceMatcher.find()) {
            BigDecimal target = new BigDecimal(toleranceMatcher.group(1));
            BigDecimal tolerance = new BigDecimal(toleranceMatcher.group(2));
            item.setTargetValue(item.getTargetValue() == null ? target : item.getTargetValue());
            item.setLowerLimit(target.subtract(tolerance));
            item.setUpperLimit(target.add(tolerance));
            return;
        }
        Matcher asymmetricToleranceMatcher = SPEC_ASYMMETRIC_TOLERANCE_PATTERN.matcher(spec);
        if (asymmetricToleranceMatcher.find()) {
            BigDecimal target = new BigDecimal(asymmetricToleranceMatcher.group(1));
            BigDecimal upperTolerance = new BigDecimal(asymmetricToleranceMatcher.group(2));
            BigDecimal lowerTolerance = new BigDecimal(asymmetricToleranceMatcher.group(3));
            item.setTargetValue(target);
            item.setLowerLimit(target.subtract(lowerTolerance));
            item.setUpperLimit(target.add(upperTolerance));
            return;
        }
        Matcher lowerMatcher = SPEC_LOWER_PATTERN.matcher(spec);
        if (lowerMatcher.find()) {
            item.setLowerLimit(new BigDecimal(lowerMatcher.group(1)));
            item.setUpperLimit(null);
            return;
        }
        Matcher strictLowerMatcher = SPEC_STRICT_LOWER_PATTERN.matcher(spec);
        if (strictLowerMatcher.find()) {
            item.setLowerLimit(new BigDecimal(strictLowerMatcher.group(1)));
            item.setUpperLimit(null);
            return;
        }
        Matcher upperMatcher = SPEC_UPPER_PATTERN.matcher(spec);
        if (upperMatcher.find()) {
            item.setLowerLimit(null);
            item.setUpperLimit(new BigDecimal(upperMatcher.group(1)));
            return;
        }
        Matcher strictUpperMatcher = SPEC_STRICT_UPPER_PATTERN.matcher(spec);
        if (strictUpperMatcher.find()) {
            item.setLowerLimit(null);
            item.setUpperLimit(new BigDecimal(strictUpperMatcher.group(1)));
            return;
        }
        Matcher equalsMatcher = SPEC_EQUALS_PATTERN.matcher(spec);
        if (equalsMatcher.find()) {
            BigDecimal target = new BigDecimal(equalsMatcher.group(1));
            item.setTargetValue(target);
            item.setLowerLimit(target);
            item.setUpperLimit(target);
            return;
        }
        if (SPEC_IN_PATTERN.matcher(spec).find() || SPEC_NOT_IN_PATTERN.matcher(spec).find()) {
            item.setLowerLimit(null);
            item.setUpperLimit(null);
        }
    }

    private QmsCoaReportDO buildReportHeader(ReportGenerateReq reqVO, HcFgShippingNoticeDO notice,
                                             QmsCoaTemplateDO template) {
        QmsCoaReportDO report = new QmsCoaReportDO();
        report.setCoaNo(nextCoaNo());
        report.setRevisionNo(1);
        report.setTemplateId(template.getId());
        report.setTemplateCode(template.getTemplateCode());
        report.setTemplateName(template.getTemplateName());
        report.setTemplateVersion(template.getVersionNo());
        report.setLanguageType(template.getLanguageType());
        report.setReportTitle(template.getReportTitle());
        report.setCustomerId(notice != null ? notice.getCustomerId() : template.getCustomerId());
        report.setCustomerCode(notice != null ? notice.getCustomerCode() : template.getCustomerCode());
        report.setCustomerName(notice != null ? notice.getCustomerName() : template.getCustomerName());
        report.setCustomerProductCode(firstNotBlank(notice == null ? null : notice.getExternalProductCode(), template.getCustomerProductCode()));
        report.setCustomerProductName(firstNotBlank(notice == null ? null : notice.getExternalProductInfo(), template.getCustomerProductName()));
        if (notice != null) {
            report.setShippingNoticeId(notice.getId());
            report.setShippingNoticeNo(notice.getNoticeNo());
            report.setSalesOrderNo(notice.getOrderNo());
            report.setErpOrderNo(notice.getErpOrderNo());
            report.setShippingTime(notice.getShippingTime());
            report.setMaterialCode(notice.getMaterialCode());
            report.setMaterialName(notice.getMaterialName());
            report.setProductModelCode(notice.getModelCode());
            report.setProductModelName(notice.getModelCode());
            report.setExternalProductCode(notice.getExternalProductCode());
            report.setExternalProductModel(notice.getExternalProductModel());
            report.setManufactureDate(notice.getRequiredProductionDate());
            report.setExpiryDate(notice.getRequiredExpiryDate());
        } else {
            report.setMaterialId(template.getMaterialId());
            report.setMaterialCode(template.getMaterialCode());
            report.setMaterialName(template.getMaterialName());
            report.setProductModelId(template.getProductModelId());
            report.setProductModelCode(template.getProductModelCode());
            report.setProductModelName(template.getProductModelName());
        }
        report.setProductionBatchNo(reqVO.getProductionBatchNo().trim());
        report.setCustomerBatchNo(firstNotBlank(reqVO.getCustomerBatchNo(), notice == null ? null : notice.getRequiredBatchNo()));
        report.setProductSize(firstNotBlank(reqVO.getProductSize(), notice == null ? null : notice.getProductSize()));
        report.setShelfLife(reqVO.getShelfLife());
        report.setIssueDate(reqVO.getIssueDate());
        report.setManufactureDate(firstNotNull(resolveBatchManufactureDate(template, report.getProductionBatchNo()),
                notice == null ? null : notice.getRequiredProductionDate()));
        report.setInspectorId(currentUserId());
        report.setInspectorName(currentUserName());
        report.setShippingUnit("PCS");
        report.setReportStatus(REPORT_DRAFT);
        report.setOverallResult("PENDING");
        report.setDataCompleteFlag(false);
        report.setSourceFaiCount(0);
        report.setReportItemCount(0);
        report.setRequiredIncompleteCount(0);
        report.setCorrectedItemCount(0);
        report.setFooterStatement(template.getFooterStatement());
        report.setRemark(reqVO.getRemark());
        report.setTenantId(currentTenantId());
        return report;
    }

    /**
     * 只复制模板快照，不在生成或刷新时自动选择检验记录。每个项目的 FAI/FQC 实测值由用户在报告中
     * 通过“取值”明确选回，避免同批多笔检验被系统静默挑选。
     */
    private void initializeReportData(QmsCoaReportDO report, QmsCoaTemplateDO template, HcFgShippingNoticeDO notice) {
        List<QmsCoaTemplateItemDO> templateItems = templateItemMapper.selectListByTemplateId(template.getId());
        TraceContext trace = resolveTrace(report.getProductionBatchNo());
        List<QmsCoaReportItemDO> reportItems = new ArrayList<>();
        for (QmsCoaTemplateItemDO templateItem : templateItems) {
            reportItems.add(buildReportItem(report, templateItem, null, List.of()));
        }
        if (!reportItems.isEmpty()) reportItemMapper.insertBatch(reportItems);
        List<QmsCoaReportShippingRelDO> relations = buildShippingRelations(report, notice);
        if (!relations.isEmpty()) shippingRelMapper.insertBatch(relations);

        QmsCoaReportDO traceUpdate = new QmsCoaReportDO();
        traceUpdate.setId(report.getId());
        traceUpdate.setSourceBatchTraceJson(trace.traceJson());
        traceUpdate.setSourceFaiCount(0);
        traceUpdate.setShippingItemCount(relations.size());
        traceUpdate.setShippingQuantity(relations.stream().map(QmsCoaReportShippingRelDO::getShippingQuantity)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        reportMapper.updateById(traceUpdate);
        refreshReportSummary(report.getId());
    }

    private LocalDate resolveBatchManufactureDate(QmsCoaTemplateDO template, String productionBatchNo) {
        List<LocalDateTime> times = new ArrayList<>();
        for (QmsFaiOrderDO order : faiOrderMapper.selectList(new LambdaQueryWrapperX<QmsFaiOrderDO>()
                .eq(QmsFaiOrderDO::getProductModel, template.getProductModelCode())
                .eq(QmsFaiOrderDO::getProductBatchNo, productionBatchNo))) {
            LocalDateTime time = firstNotNull(order.getSubmissionTime(), order.getInspectionTime(), order.getCreateTime());
            if (time != null) times.add(time);
        }
        for (QmsFqcOrderDO order : fqcOrderMapper.selectList(new LambdaQueryWrapperX<QmsFqcOrderDO>()
                .eq(QmsFqcOrderDO::getProductModel, template.getProductModelCode())
                .and(wrapper -> wrapper.eq(QmsFqcOrderDO::getProductBatchNo, productionBatchNo)
                        .or().eq(QmsFqcOrderDO::getBatchNo, productionBatchNo)))) {
            LocalDateTime time = firstNotNull(order.getSubmissionTime(), order.getInspectionTime(), order.getCreateTime());
            if (time != null) times.add(time);
        }
        return times.stream().min(Comparator.naturalOrder()).map(LocalDateTime::toLocalDate).orElse(null);
    }

    private TraceContext resolveTrace(String batchNo) {
        Set<String> batchNos = new LinkedHashSet<>();
        Set<String> inspectionNos = new LinkedHashSet<>();
        batchNos.add(batchNo);
        String traceJson = null;
        try {
            HcBatchTraceQueryReqVO reqVO = new HcBatchTraceQueryReqVO();
            reqVO.setBatchNo(batchNo);
            HcBatchTraceRespVO trace = batchTraceService.getTrace(reqVO);
            traceJson = JsonUtils.toJsonString(trace);
            for (HcBatchTraceRespVO.TimelineNodeRespVO node : trace.getTimelineNodes()) {
                addText(batchNos, node.getBatchNo());
                addText(batchNos, node.getSourceBatchNo());
                addText(batchNos, node.getParentBatchNo());
            }
            for (HcBatchTraceRespVO.QualityItemRespVO event : trace.getQualityEvents()) {
                if (StringUtils.hasText(event.getInspectionNo()) && !Set.of("自检", "缺陷位置", "检验事件").contains(event.getInspectionNo())) {
                    inspectionNos.add(event.getInspectionNo());
                }
            }
        } catch (RuntimeException ignored) {
            traceJson = JsonUtils.toJsonString(Map.of("inputBatchNo", batchNo, "traceStatus", "UNAVAILABLE"));
        }
        return new TraceContext(batchNos, inspectionNos, traceJson);
    }

    private List<QmsFaiOrderDO> selectEffectiveFaiOrders(TraceContext trace) {
        LambdaQueryWrapperX<QmsFaiOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(QmsFaiOrderDO::getStatus, "COMPLETED")
                .eq(QmsFaiOrderDO::getJudgment, "OK")
                .eq(QmsFaiOrderDO::getReleaseResult, "RELEASED");
        wrapper.isNull(QmsFaiOrderDO::getRejectNextInspectionId);
        wrapper.and(q -> {
            boolean hasBatch = !trace.batchNos().isEmpty();
            if (hasBatch) q.in(QmsFaiOrderDO::getProductBatchNo, trace.batchNos());
            if (!trace.inspectionNos().isEmpty()) {
                if (hasBatch) q.or();
                q.in(QmsFaiOrderDO::getFaiNo, trace.inspectionNos());
            }
        });
        wrapper.orderByDesc(QmsFaiOrderDO::getQaTime).orderByDesc(QmsFaiOrderDO::getId);
        return faiOrderMapper.selectList(wrapper);
    }

    private Map<Long, List<QmsFaiSampleDO>> loadSamples(List<QmsFaiOrderDO> orders) {
        Map<Long, List<QmsFaiSampleDO>> result = new HashMap<>();
        for (QmsFaiOrderDO order : orders) {
            result.put(order.getId(), faiSampleMapper.selectListByFaiId(order.getId()));
        }
        return result;
    }

    private QmsCoaReportSourceDO buildReportSource(QmsCoaReportDO report, QmsFaiOrderDO order) {
        QmsCoaReportSourceDO source = new QmsCoaReportSourceDO();
        source.setReportId(report.getId());
        source.setCoaNo(report.getCoaNo());
        source.setRevisionNo(report.getRevisionNo());
        source.setProductionBatchNo(report.getProductionBatchNo());
        source.setCustomerBatchNo(report.getCustomerBatchNo());
        source.setSourceType("FAI");
        source.setSourceId(order.getId());
        source.setSourceNo(order.getFaiNo());
        source.setSourceBatchNo(order.getProductBatchNo());
        source.setProcessCode(firstNotBlank(order.getOperationCode(), order.getProcessCategory(), order.getSourceOperationCode()));
        source.setProcessName(firstNotBlank(order.getOperationName(), order.getSourceOperationName()));
        source.setMaterialId(order.getMaterialId());
        source.setMaterialCode(order.getMaterialCode());
        source.setMaterialName(order.getMaterialName());
        source.setProductModelId(order.getProductModelId());
        source.setProductModelCode(order.getProductModel());
        source.setStandardId(order.getStandardId());
        source.setStandardNo(order.getStandardNo());
        source.setStandardVersion(order.getStandardVersion());
        source.setStandardSnapshotHash(order.getStandardSnapshotHash());
        source.setSourceStatus(order.getStatus());
        source.setSourceResult(order.getJudgment());
        source.setReleaseResult(order.getReleaseResult());
        source.setQaInspectorId(order.getQaInspectorId());
        source.setQaInspectorName(order.getQaInspectorName());
        source.setQaTime(order.getQaTime());
        source.setHistoricalBackfill(order.getHistoricalBackfill());
        source.setRecheckFlag(order.getRecheckFlag());
        source.setRecheckGroupId(order.getRecheckGroupId());
        source.setRecheckRoundNo(order.getRecheckRoundNo());
        source.setSelectedFlag(false);
        source.setEffectiveReason("COMPLETED + OK + RELEASED，且无后续复检单");
        source.setSourceSnapshotHash(sha256(order.getFaiNo() + "|" + order.getStandardSnapshotHash() + "|" + order.getQaTime()));
        source.setTenantId(report.getTenantId());
        return source;
    }

    private SourceMatch findSourceMatch(QmsCoaTemplateItemDO templateItem, List<QmsFaiOrderDO> orders,
                                        Map<Long, List<QmsFaiItemDO>> itemsByFai) {
        if ("FIXED".equalsIgnoreCase(templateItem.getValueStrategy())) return null;
        for (QmsFaiOrderDO order : orders) {
            if (templateItem.getSourceStandardId() != null && !Objects.equals(templateItem.getSourceStandardId(), order.getStandardId())) continue;
            if (StringUtils.hasText(templateItem.getSourceStandardVersion())
                    && !equalsIgnoreCase(templateItem.getSourceStandardVersion(), order.getStandardVersion())) continue;
            if (StringUtils.hasText(templateItem.getSourceProcessCode())
                    && !matchesProcess(templateItem.getSourceProcessCode(), order)) continue;
            for (QmsFaiItemDO item : itemsByFai.getOrDefault(order.getId(), List.of())) {
                if (templateItem.getSourceStandardItemId() != null
                        && Objects.equals(templateItem.getSourceStandardItemId(), item.getStandardItemId())) {
                    return new SourceMatch(order, item);
                }
                if (equalsIgnoreCase(templateItem.getMetricCode(), item.getMetricCode())
                        || equalsIgnoreCase(templateItem.getMetricCode(), item.getSheetMetricCode())
                        || equalsIgnoreCase(templateItem.getSourceInspectionItem(), item.getInspectionItem())) {
                    return new SourceMatch(order, item);
                }
            }
        }
        return null;
    }

    private QmsCoaReportItemDO buildReportItem(QmsCoaReportDO report, QmsCoaTemplateItemDO templateItem,
                                               SourceMatch match, List<QmsFaiSampleDO> allSamples) {
        QmsCoaReportItemDO item = new QmsCoaReportItemDO();
        item.setReportId(report.getId());
        item.setCoaNo(report.getCoaNo());
        item.setRevisionNo(report.getRevisionNo());
        item.setProductionBatchNo(report.getProductionBatchNo());
        item.setCustomerBatchNo(report.getCustomerBatchNo());
        item.setMaterialCode(report.getMaterialCode());
        item.setProductModelCode(report.getProductModelCode());
        item.setMetricCode(templateItem.getMetricCode());
        item.setItemGroup(templateItem.getItemGroup());
        item.setItemNameCn(templateItem.getItemNameCn());
        item.setItemNameEn(templateItem.getItemNameEn());
        item.setItemType(templateItem.getSourceItemType());
        item.setValueSourceType(firstNotBlank(templateItem.getValueSourceType(),
                "FIXED".equalsIgnoreCase(templateItem.getValueStrategy()) ? "MANUAL_ENTRY" : "PROCESS_INSPECTION"));
        item.setSourceStandardApplyType(templateItem.getSourceStandardApplyType());
        item.setValueStrategy(templateItem.getValueStrategy());
        item.setSpecSource(templateItem.getSpecSource());
        item.setSpecText(templateItem.getSpecText());
        item.setCoaSpecText(templateItem.getCoaSpecText());
        item.setTargetValue(templateItem.getTargetValue());
        item.setLowerLimit(templateItem.getLowerLimit());
        item.setUpperLimit(templateItem.getUpperLimit());
        item.setUnit(templateItem.getUnit());
        item.setDecimalPlaces(templateItem.getDecimalPlaces());
        item.setInspectionMethod(templateItem.getInspectionMethod());
        item.setRequiredFlag(templateItem.getRequiredFlag());
        item.setAllowCorrectionFlag(templateItem.getAllowCorrectionFlag());
        item.setCorrectedFlag(false);
        item.setCorrectionCount(0);
        item.setSortNo(templateItem.getSortNo());
        item.setSourceProcessId(templateItem.getSourceProcessId());
        item.setSourceProcessCode(templateItem.getSourceProcessCode());
        item.setSourceProcessName(templateItem.getSourceProcessName());
        item.setSourceStandardId(templateItem.getSourceStandardId());
        item.setSourceStandardNo(templateItem.getSourceStandardNo());
        item.setSourceStandardVersion(templateItem.getSourceStandardVersion());
        item.setSourceStandardItemId(templateItem.getSourceStandardItemId());
        if ("FIXED".equalsIgnoreCase(templateItem.getValueStrategy())) {
            item.setSourceValue(templateItem.getFixedValue());
            item.setActualValue(templateItem.getFixedValue());
            item.setDisplayValue(templateItem.getFixedValue());
            item.setSourceDataCompleteFlag(StringUtils.hasText(templateItem.getFixedValue()));
            item.setResult("OK");
        } else if (match != null) {
            QmsFaiOrderDO order = match.order();
            QmsFaiItemDO faiItem = match.item();
            List<QmsFaiSampleDO> samples = allSamples.stream()
                    .filter(sample -> Objects.equals(sample.getFaiItemId(), faiItem.getId()))
                    .sorted(Comparator.comparing(QmsFaiSampleDO::getSampleSeq,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            String value = resolveFaiValue(templateItem.getValueStrategy(), faiItem, samples);
            item.setRawValueJson(JsonUtils.toJsonString(samples.stream().map(this::sampleValue).toList()));
            item.setSourceValue(value);
            item.setActualValue(value);
            item.setDisplayValue(formatDisplayValue(value, item.getDecimalPlaces()));
            item.setSourceDataCompleteFlag(StringUtils.hasText(value));
            item.setInspectionMethod(faiItem.getInspectionMethod());
            item.setResult(firstNotBlank(faiItem.getQaResult(), order.getJudgment()));
            item.setSourceProcessCode(firstNotBlank(order.getOperationCode(), order.getProcessCategory()));
            item.setSourceProcessName(order.getOperationName());
            item.setSourceFaiId(order.getId());
            item.setSourceFaiNo(order.getFaiNo());
            item.setSourceFaiItemId(faiItem.getId());
            item.setSourceStandardId(order.getStandardId());
            item.setSourceStandardNo(order.getStandardNo());
            item.setSourceStandardVersion(order.getStandardVersion());
            item.setSourceStandardSnapshotHash(order.getStandardSnapshotHash());
            item.setSourceStandardItemId(faiItem.getStandardItemId());
            item.setSourceQaTime(order.getQaTime());
            if (!StringUtils.hasText(item.getSpecText())) item.setSpecText(faiItem.getStandardDesc());
            if (item.getLowerLimit() == null) item.setLowerLimit(faiItem.getMinValueLimit());
            if (item.getUpperLimit() == null) item.setUpperLimit(faiItem.getMaxValueLimit());
        } else {
            item.setSourceDataCompleteFlag(false);
            item.setResult("PENDING");
        }
        item.setTenantId(report.getTenantId());
        return item;
    }

    private String resolveFaiValue(String strategy, QmsFaiItemDO item, List<QmsFaiSampleDO> samples) {
        List<QmsFaiSampleDO> qaSamples = samples.stream().filter(s -> "QA".equalsIgnoreCase(s.getSampleRole())).toList();
        List<QmsFaiSampleDO> effectiveSamples = qaSamples.isEmpty() ? samples : qaSamples;
        List<BigDecimal> numericValues = effectiveSamples.stream().map(this::sampleNumericValue).filter(Objects::nonNull).toList();
        return switch (firstNotBlank(strategy, "QA_AVG").toUpperCase(Locale.ROOT)) {
            case "QA_MIN" -> decimalText(firstNotNull(item.getQaMin(), numericValues.stream().min(BigDecimal::compareTo).orElse(null)));
            case "QA_MAX" -> decimalText(firstNotNull(item.getQaMax(), numericValues.stream().max(BigDecimal::compareTo).orElse(null)));
            case "QA_RESULT" -> firstNotBlank(effectiveSamples.stream().map(QmsFaiSampleDO::getQualitativeValue)
                    .filter(StringUtils::hasText).findFirst().orElse(null), item.getQaResult());
            case "LATEST_SAMPLE" -> effectiveSamples.isEmpty() ? null : sampleValue(effectiveSamples.get(effectiveSamples.size() - 1));
            default -> decimalText(firstNotNull(item.getQaAvg(), average(numericValues)));
        };
    }

    private List<QmsCoaReportShippingRelDO> buildShippingRelations(QmsCoaReportDO report, HcFgShippingNoticeDO notice) {
        if (notice == null) return List.of();
        List<HcFgShippingNoticeItemDO> all = shippingNoticeItemMapper.selectListByNoticeId(notice.getId());
        List<HcFgShippingNoticeItemDO> matched = all.stream().filter(item -> matchesBatch(item, report.getProductionBatchNo())).toList();
        if (matched.isEmpty()) matched = all;
        List<QmsCoaReportShippingRelDO> result = new ArrayList<>();
        for (HcFgShippingNoticeItemDO source : matched) {
            QmsCoaReportShippingRelDO rel = new QmsCoaReportShippingRelDO();
            rel.setReportId(report.getId());
            rel.setCoaNo(report.getCoaNo());
            rel.setRevisionNo(report.getRevisionNo());
            rel.setShippingNoticeId(notice.getId());
            rel.setShippingNoticeNo(notice.getNoticeNo());
            rel.setShippingNoticeItemId(source.getId());
            rel.setFinishedStockId(firstNotNull(source.getActualFinishedStockId(), source.getFinishedStockId()));
            rel.setStockNo(firstNotBlank(source.getActualStockNo(), source.getStockNo()));
            rel.setOuterBoxNo(source.getOuterBoxNo());
            rel.setInnerUnitNo(source.getInnerUnitNo());
            rel.setSliceBatchNo(source.getSliceBatchNo());
            rel.setActualSliceBatchNo(source.getActualSliceBatchNo());
            rel.setProductionBatchNo(report.getProductionBatchNo());
            rel.setCustomerBatchNo(firstNotBlank(source.getCustomerProductBatchNo(), source.getCustomerSliceBatchNo(), report.getCustomerBatchNo()));
            rel.setCustomerModelCode(source.getCustomerModelCode());
            rel.setMaterialCode(source.getMaterialCode());
            rel.setModelCode(source.getModelCode());
            rel.setShippingQuantity(BigDecimal.valueOf(firstNotNull(source.getActualShipQty(), source.getLockedQty(), 1)));
            rel.setShippingUnit(report.getShippingUnit());
            rel.setQualityStatus(source.getQualityStatus());
            rel.setOqcOrderId(source.getOqcOrderId());
            rel.setOqcStatus(source.getOqcStatus());
            rel.setShippingInspectionResult(source.getShippingInspectionResult());
            rel.setTenantId(report.getTenantId());
            result.add(rel);
        }
        return result;
    }

    private void refreshReportSummary(Long reportId) {
        List<QmsCoaReportItemDO> items = reportItemMapper.selectListByReportId(reportId);
        int incomplete = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getRequiredFlag())
                && !StringUtils.hasText(item.getActualValue())).count();
        int corrected = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.getCorrectedFlag())).count();
        boolean pending = items.stream().anyMatch(item -> "PENDING".equalsIgnoreCase(item.getResult()));
        String overall = incomplete > 0 || pending ? "PENDING"
                : items.stream().anyMatch(item -> "NG".equalsIgnoreCase(item.getResult())) ? "NG" : "OK";
        QmsCoaReportDO update = new QmsCoaReportDO();
        update.setId(reportId);
        update.setReportItemCount(items.size());
        update.setRequiredIncompleteCount(incomplete);
        update.setCorrectedItemCount(corrected);
        update.setDataCompleteFlag(incomplete == 0);
        update.setOverallResult(overall);
        update.setDataSnapshotHash(null);
        reportMapper.updateById(update);
    }

    private void validateShippingGate(Long reportId) {
        List<QmsCoaReportShippingRelDO> relations = shippingRelMapper.selectListByReportId(reportId);
        if (relations.isEmpty()) return;
        boolean explicitNg = relations.stream().anyMatch(rel -> "NG".equalsIgnoreCase(rel.getQualityStatus())
                || "NG".equalsIgnoreCase(rel.getShippingInspectionResult()));
        boolean released = relations.stream().allMatch(rel -> "OK".equalsIgnoreCase(rel.getQualityStatus())
                || "OK".equalsIgnoreCase(rel.getShippingInspectionResult())
                || Set.of("COMPLETED", "APPROVED", "PASSED", "INSPECTED").contains(normalize(rel.getOqcStatus())));
        if (explicitNg || !released) throw exception(QMS_COA_SHIPPING_GATE_NOT_PASSED);
    }

    private void cloneReportChildren(Long sourceReportId, QmsCoaReportDO target) {
        QmsCoaReportDO sourceReport = validateReport(sourceReportId);
        List<QmsCoaReportItemDO> sourceItems = reportItemMapper.selectListByReportId(sourceReportId);
        Map<Long, String> metricBySourceItemId = sourceItems.stream()
                .collect(Collectors.toMap(QmsCoaReportItemDO::getId, QmsCoaReportItemDO::getMetricCode));
        List<QmsCoaReportItemDO> items = sourceItems.stream().map(source -> {
            QmsCoaReportItemDO clone = BeanUtils.toBean(source, QmsCoaReportItemDO.class);
            clone.setId(null); clone.setReportId(target.getId()); clone.setRevisionNo(target.getRevisionNo()); clone.setCoaNo(target.getCoaNo());
            clone.setLastCorrectionLogId(null);
            return clone;
        }).toList();
        if (!items.isEmpty()) reportItemMapper.insertBatch(items);
        Map<String, QmsCoaReportItemDO> targetItemByMetric = reportItemMapper.selectListByReportId(target.getId()).stream()
                .collect(Collectors.toMap(QmsCoaReportItemDO::getMetricCode, Function.identity()));
        List<QmsCoaCorrectionLogDO> correctionLogs = correctionLogMapper.selectListByReportId(sourceReportId).stream()
                .map(source -> {
                    String metricCode = metricBySourceItemId.get(source.getReportItemId());
                    QmsCoaReportItemDO targetItem = targetItemByMetric.get(metricCode);
                    if (targetItem == null) return null;
                    QmsCoaCorrectionLogDO clone = BeanUtils.toBean(source, QmsCoaCorrectionLogDO.class);
                    clone.setId(null);
                    clone.setReportId(target.getId());
                    clone.setCoaNo(target.getCoaNo());
                    clone.setRevisionNo(target.getRevisionNo());
                    clone.setReportItemId(targetItem.getId());
                    clone.setCorrectionReason("继承自 " + sourceReport.getCoaNo() + " R" + sourceReport.getRevisionNo()
                            + "：" + source.getCorrectionReason());
                    return clone;
                })
                .filter(Objects::nonNull)
                .toList();
        if (!correctionLogs.isEmpty()) correctionLogMapper.insertBatch(correctionLogs);
        List<QmsCoaReportSourceDO> sources = reportSourceMapper.selectListByReportId(sourceReportId).stream().map(source -> {
            QmsCoaReportSourceDO clone = BeanUtils.toBean(source, QmsCoaReportSourceDO.class);
            clone.setId(null); clone.setReportId(target.getId()); clone.setRevisionNo(target.getRevisionNo()); clone.setCoaNo(target.getCoaNo()); return clone;
        }).toList();
        if (!sources.isEmpty()) reportSourceMapper.insertBatch(sources);
        List<QmsCoaReportShippingRelDO> rels = shippingRelMapper.selectListByReportId(sourceReportId).stream().map(source -> {
            QmsCoaReportShippingRelDO clone = BeanUtils.toBean(source, QmsCoaReportShippingRelDO.class);
            clone.setId(null); clone.setReportId(target.getId()); clone.setRevisionNo(target.getRevisionNo()); clone.setCoaNo(target.getCoaNo()); return clone;
        }).toList();
        if (!rels.isEmpty()) shippingRelMapper.insertBatch(rels);
    }

    private String calculateDataHash(Long reportId) {
        QmsCoaReportDO report = validateReport(reportId);
        StringBuilder canonical = new StringBuilder()
                .append(report.getCoaNo()).append('|').append(report.getRevisionNo()).append('|')
                .append(report.getProductionBatchNo()).append('|').append(report.getCustomerBatchNo()).append('|')
                .append(report.getTemplateCode()).append('|').append(report.getTemplateVersion());
        for (QmsCoaReportItemDO item : reportItemMapper.selectListByReportId(reportId)) {
            canonical.append('\n').append(item.getMetricCode()).append('|').append(item.getSpecText()).append('|')
                    .append(item.getActualValue()).append('|').append(item.getDisplayValue()).append('|')
                    .append(item.getResult()).append('|').append(item.getSourceFaiNo()).append('|')
                    .append(item.getSourceStandardSnapshotHash());
        }
        return sha256(canonical.toString());
    }

    private String buildPrintHtml(ReportResp report) {
        StringBuilder rows = new StringBuilder();
        for (QmsCoaVO.ReportItemResp item : report.getItems()) {
            rows.append("<tr><td>").append(html(item.getItemNameCn())).append("</td><td>")
                    .append(html(item.getSpecText())).append("</td><td>").append(html(item.getDisplayValue()))
                    .append("</td><td>").append(html(item.getUnit())).append("</td><td>")
                    .append(html(item.getResult())).append("</td><td>").append(html(item.getSourceFaiNo())).append("</td></tr>");
        }
        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>" + html(report.getCoaNo())
                + "</title><style>body{font-family:Arial,'Microsoft YaHei',sans-serif;padding:28px;color:#111}h1{text-align:center}"
                + "table{width:100%;border-collapse:collapse;margin-top:18px}td,th{border:1px solid #333;padding:7px;font-size:12px}"
                + ".meta{display:grid;grid-template-columns:1fr 1fr;gap:6px;font-size:13px}.footer{margin-top:24px;font-size:12px}</style></head><body>"
                + "<h1>" + html(firstNotBlank(report.getReportTitle(), "Certificate of Analysis")) + "</h1><div class=\"meta\">"
                + meta("COA No.", report.getCoaNo() + " R" + report.getRevisionNo())
                + meta("Customer", report.getCustomerName()) + meta("Product", firstNotBlank(report.getCustomerProductName(), report.getMaterialName()))
                + meta("Model", firstNotBlank(report.getExternalProductModel(), report.getProductModelCode()))
                + meta("Production Batch", report.getProductionBatchNo()) + meta("Customer Batch", report.getCustomerBatchNo())
                + meta("Result", report.getOverallResult()) + meta("Verification", report.getVerificationCode())
                + "</div><table><thead><tr><th>检验项目</th><th>规格</th><th>结果</th><th>单位</th><th>判定</th><th>来源FAI</th></tr></thead><tbody>"
                + rows + "</tbody></table><div class=\"footer\">" + html(report.getFooterStatement())
                + "<br>审核：" + html(report.getReviewerName()) + "　签发：" + html(report.getIssuedByName()) + "</div></body></html>";
    }

    private void addAuditLog(QmsCoaReportDO report, String action, String before, String after, String reason, String opinion) {
        QmsCoaAuditLogDO log = new QmsCoaAuditLogDO();
        log.setReportId(report.getId());
        log.setCoaNo(report.getCoaNo());
        log.setRevisionNo(report.getRevisionNo());
        log.setActionType(action);
        log.setBeforeStatus(before);
        log.setAfterStatus(after);
        log.setOperatorId(currentUserId());
        log.setOperatorName(currentUserName());
        log.setActionTime(LocalDateTime.now());
        log.setOpinion(opinion);
        log.setReason(reason);
        log.setTenantId(currentTenantId());
        auditLogMapper.insert(log);
    }

    private HcFgShippingNoticeDO resolveShippingNotice(Long id, String noticeNo) {
        if (id == null && !StringUtils.hasText(noticeNo)) return null;
        HcFgShippingNoticeDO notice = id != null ? shippingNoticeMapper.selectById(id) : shippingNoticeMapper.selectByNoticeNo(noticeNo.trim());
        if (notice == null) throw exception(QMS_COA_SHIPPING_NOTICE_NOT_FOUND);
        return notice;
    }

    private QmsCoaTemplateDO resolveTemplate(Long templateId, HcFgShippingNoticeDO notice) {
        if (templateId == null) {
            throw exception(QMS_COA_TEMPLATE_NOT_MATCHED);
        }
        QmsCoaTemplateDO template = validateTemplate(templateId);
        if (!Objects.equals(template.getStatus(), 1) || !TEMPLATE_APPROVED.equals(template.getAuditStatus())) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        return template;
    }

    private int templateScore(QmsCoaTemplateDO template, HcFgShippingNoticeDO notice) {
        if (notice == null) return template.getCustomerId() == null ? 1 : -1;
        int score = 0;
        if (template.getCustomerId() != null) {
            if (!Objects.equals(template.getCustomerId(), notice.getCustomerId())) return -1;
            score += 100;
        }
        if (StringUtils.hasText(template.getCustomerProductCode())) {
            if (!equalsIgnoreCase(template.getCustomerProductCode(), notice.getExternalProductCode())) return -1;
            score += 80;
        }
        if (StringUtils.hasText(template.getProductModelCode())) {
            if (!equalsIgnoreCase(template.getProductModelCode(), notice.getModelCode())) return -1;
            score += 60;
        }
        if (StringUtils.hasText(template.getMaterialCode())) {
            if (!equalsIgnoreCase(template.getMaterialCode(), notice.getMaterialCode())) return -1;
            score += 40;
        }
        return score;
    }

    private QmsCoaTemplateDO validateTemplate(Long id) {
        QmsCoaTemplateDO entity = id == null ? null : templateMapper.selectById(id);
        if (entity == null) throw exception(QMS_COA_TEMPLATE_NOT_EXISTS);
        return entity;
    }

    private String nextTemplateVersion(String versionNo) {
        String sourceVersion = firstNotBlank(versionNo, "V0.0").trim();
        Matcher matcher = Pattern.compile("(?i)^V?(\\d+)(?:\\.\\d+)?$").matcher(sourceVersion);
        if (!matcher.matches()) {
            throw exception(QMS_COA_TEMPLATE_STATUS_INVALID);
        }
        return "V" + (Integer.parseInt(matcher.group(1)) + 1) + ".0";
    }

    private void validateTemplateUnique(String code, String version, Long excludeId) {
        if (templateMapper.selectByCodeAndVersion(code, version, excludeId) != null) throw exception(QMS_COA_TEMPLATE_DUPLICATE);
    }

    private QmsCoaReportDO validateReport(Long id) {
        QmsCoaReportDO report = id == null ? null : reportMapper.selectById(id);
        if (report == null) throw exception(QMS_COA_REPORT_NOT_EXISTS);
        return report;
    }

    private QmsCoaReportDO validateEditableReport(Long id) {
        QmsCoaReportDO report = validateReport(id);
        if (!Set.of(REPORT_DRAFT, REPORT_REJECTED).contains(report.getReportStatus())) {
            throw exception(QMS_COA_REPORT_STATUS_INVALID);
        }
        return report;
    }

    private boolean matchesProcess(String processCode, QmsFaiOrderDO order) {
        return equalsIgnoreCase(processCode, order.getOperationCode())
                || equalsIgnoreCase(processCode, order.getProcessCategory())
                || equalsIgnoreCase(processCode, order.getSourceOperationCode());
    }

    private boolean matchesBatch(HcFgShippingNoticeItemDO item, String batchNo) {
        return java.util.stream.Stream.of(item.getActualSliceBatchNo(), item.getSliceBatchNo(), item.getBatchNo(),
                        item.getCustomerProductBatchNo(), item.getCustomerSliceBatchNo())
                .filter(Objects::nonNull)
                .anyMatch(value -> value.equalsIgnoreCase(batchNo) || value.startsWith(batchNo) || batchNo.startsWith(value));
    }

    private BigDecimal sampleNumericValue(QmsFaiSampleDO sample) {
        return firstNotNull(sample.getMeasuredValue(), sample.getResultValue(), sample.getDensityValue(),
                sample.getCompressionRate(), sample.getCompressionElasticityRate());
    }

    private String sampleValue(QmsFaiSampleDO sample) {
        return firstNotBlank(decimalText(sampleNumericValue(sample)), sample.getQualitativeValue(), sample.getSampleResult());
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.HALF_UP);
    }

    private String judgeValue(String value, QmsCoaReportItemDO item) {
        if ("OK".equalsIgnoreCase(value) || "NG".equalsIgnoreCase(value)) {
            return value.toUpperCase(Locale.ROOT);
        }
        String formulaResult = judgeSpecFormula(value, item.getSpecText());
        if (formulaResult != null) return formulaResult;
        try {
            BigDecimal number = new BigDecimal(value.trim());
            if (item.getLowerLimit() != null && number.compareTo(item.getLowerLimit()) < 0) return "NG";
            if (item.getUpperLimit() != null && number.compareTo(item.getUpperLimit()) > 0) return "NG";
            return "OK";
        } catch (RuntimeException ignored) {
            return item.getLowerLimit() != null || item.getUpperLimit() != null ? "PENDING" : "OK";
        }
    }

    /** 返回 null 说明内控不是受支持的公式，继续使用已快照的标准上下限判定。 */
    private String judgeSpecFormula(String value, String specText) {
        if (!StringUtils.hasText(specText)) return null;
        String spec = normalizeSpecFormula(specText);
        Matcher rangeMatcher = SPEC_RANGE_PATTERN.matcher(spec);
        if (rangeMatcher.find()) {
            return judgeNumeric(value, new BigDecimal(rangeMatcher.group(1)), true,
                    new BigDecimal(rangeMatcher.group(2)), true);
        }
        Matcher toleranceMatcher = SPEC_TOLERANCE_PATTERN.matcher(spec);
        if (toleranceMatcher.find()) {
            BigDecimal target = new BigDecimal(toleranceMatcher.group(1));
            BigDecimal tolerance = new BigDecimal(toleranceMatcher.group(2));
            return judgeNumeric(value, target.subtract(tolerance), true, target.add(tolerance), true);
        }
        Matcher asymmetricMatcher = SPEC_ASYMMETRIC_TOLERANCE_PATTERN.matcher(spec);
        if (asymmetricMatcher.find()) {
            BigDecimal target = new BigDecimal(asymmetricMatcher.group(1));
            return judgeNumeric(value, target.subtract(new BigDecimal(asymmetricMatcher.group(3))), true,
                    target.add(new BigDecimal(asymmetricMatcher.group(2))), true);
        }
        Matcher lowerMatcher = SPEC_LOWER_PATTERN.matcher(spec);
        if (lowerMatcher.find()) return judgeNumeric(value, new BigDecimal(lowerMatcher.group(1)), true, null, true);
        Matcher strictLowerMatcher = SPEC_STRICT_LOWER_PATTERN.matcher(spec);
        if (strictLowerMatcher.find()) return judgeNumeric(value, new BigDecimal(strictLowerMatcher.group(1)), false, null, true);
        Matcher upperMatcher = SPEC_UPPER_PATTERN.matcher(spec);
        if (upperMatcher.find()) return judgeNumeric(value, null, true, new BigDecimal(upperMatcher.group(1)), true);
        Matcher strictUpperMatcher = SPEC_STRICT_UPPER_PATTERN.matcher(spec);
        if (strictUpperMatcher.find()) return judgeNumeric(value, null, true, new BigDecimal(strictUpperMatcher.group(1)), false);
        Matcher equalsMatcher = SPEC_EQUALS_PATTERN.matcher(spec);
        if (equalsMatcher.find()) {
            BigDecimal target = new BigDecimal(equalsMatcher.group(1));
            return judgeNumeric(value, target, true, target, true);
        }
        Matcher inMatcher = SPEC_IN_PATTERN.matcher(spec);
        if (inMatcher.find()) return judgeEnum(value, inMatcher.group(1), true);
        Matcher notInMatcher = SPEC_NOT_IN_PATTERN.matcher(spec);
        if (notInMatcher.find()) return judgeEnum(value, notInMatcher.group(1), false);
        return null;
    }

    private String judgeNumeric(String value, BigDecimal lowerLimit, boolean lowerInclusive,
                                BigDecimal upperLimit, boolean upperInclusive) {
        try {
            BigDecimal actual = new BigDecimal(value.trim());
            if (lowerLimit != null) {
                int compare = actual.compareTo(lowerLimit);
                if (compare < 0 || (!lowerInclusive && compare == 0)) return "NG";
            }
            if (upperLimit != null) {
                int compare = actual.compareTo(upperLimit);
                if (compare > 0 || (!upperInclusive && compare == 0)) return "NG";
            }
            return "OK";
        } catch (RuntimeException ignored) {
            return "PENDING";
        }
    }

    private String judgeEnum(String value, String configuredValues, boolean allowMatched) {
        boolean matched = java.util.stream.Stream.of(configuredValues.split("[,，|]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(candidate -> enumValueMatches(value, candidate));
        return matched == allowMatched ? "OK" : "NG";
    }

    private boolean enumValueMatches(String actualValue, String configuredValue) {
        try {
            return new BigDecimal(actualValue.trim()).compareTo(new BigDecimal(configuredValue.trim())) == 0;
        } catch (RuntimeException ignored) {
            return actualValue.trim().equalsIgnoreCase(configuredValue.trim());
        }
    }

    private String normalizeSpecFormula(String specText) {
        return specText.replaceAll("\\s+", "")
                .replace('～', '~').replace('至', '~').replace('—', '~').replace('–', '~')
                .replace("+/-", "±").replaceAll("(?i)NOT\\s*IN", "NOTIN")
                .replace(">=", "≥").replace("<=", "≤");
    }

    private String formatDisplayValue(String value, Integer scale) {
        if (!StringUtils.hasText(value)) return value;
        try {
            return new BigDecimal(value.trim()).setScale(Objects.requireNonNullElse(scale, 4), RoundingMode.HALF_UP).toPlainString();
        } catch (RuntimeException ignored) {
            return value.trim();
        }
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String sha256(Object value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算COA快照哈希", exception);
        }
    }

    private Long currentTenantId() {
        return TenantContextHolder.getTenantId();
    }

    private Long currentUserId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }

    private String currentUserName() {
        return firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private void addText(Set<String> values, String value) {
        if (StringUtils.hasText(value)) values.add(value.trim());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean containsIgnoreCase(String source, String expected) {
        return source != null && expected != null && source.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String appendRemark(String source, String addition) {
        return StringUtils.hasText(source) ? source + "；" + addition : addition;
    }

    private String uniqueNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_TIME_FORMAT)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
    }

    /**
     * 仅在 {@link #generateReport(ReportGenerateReq)} 的编号锁内调用；数据库唯一索引继续兜底跨实例冲突。
     */
    private String nextCoaNo() {
        String prefix = "COA-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        int sequence = reportMapper.selectList(new LambdaQueryWrapperX<QmsCoaReportDO>()
                        .likeRight(QmsCoaReportDO::getCoaNo, prefix))
                .stream()
                .map(QmsCoaReportDO::getCoaNo)
                .mapToInt(coaNo -> parseCoaSequence(coaNo, prefix))
                .max()
                .orElse(0) + 1;
        if (sequence > 999) {
            throw new IllegalStateException("COA当天流水已超过999，无法生成三位流水号");
        }
        return prefix + String.format(Locale.ROOT, "%03d", sequence);
    }

    private int parseCoaSequence(String coaNo, String prefix) {
        if (!StringUtils.hasText(coaNo) || !coaNo.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(coaNo.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String meta(String label, Object value) {
        return "<div><strong>" + html(label) + "：</strong>" + html(value) + "</div>";
    }

    private String html(Object value) {
        if (value == null) return "-";
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) if (value != null) return value;
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) if (StringUtils.hasText(value)) return value.trim();
        return null;
    }

    private record TraceContext(Set<String> batchNos, Set<String> inspectionNos, String traceJson) {
    }

    private record SourceMatch(QmsFaiOrderDO order, QmsFaiItemDO item) {
    }
}
