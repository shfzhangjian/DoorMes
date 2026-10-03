package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierNameCheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierResourceStatusAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierResourceStatusLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.MesSupplierRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierResourceStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierResourceStatusLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import jakarta.annotation.Resource;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_REGISTERED_SUPPLIER_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_RESOURCE_CONFIRM_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_RESOURCE_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_RESOURCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_RESOURCE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_SELECTION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
public class SrmSupplierCandidateServiceImpl implements SrmSupplierCandidateService {

    private static final String SOURCE_REGISTERED = "REGISTERED";
    /**
     * 旧版未入库供应商来源标识仅保留兼容入参；供应商资源池与合格名录统一使用 mes_supplier 主表。
     */
    private static final String LEGACY_PENDING_SOURCE = "PENDING";
    private static final String PENDING_STATUS = "PENDING";
    private static final String QUALIFIED_STATUS = "QUALIFIED";
    private static final Set<String> RESOURCE_STATUSES = Set.of(PENDING_STATUS, QUALIFIED_STATUS,
            "UNQUALIFIED", "FROZEN", "ELIMINATED", "EXITED");
    private static final int INITIAL_VERSION = 0;

    @Resource
    private MesSupplierMapper supplierMapper;
    @Resource
    private SrmSupplierResourceStatusLogMapper resourceStatusLogMapper;
    @Resource
    private SrmSupplierScopeService supplierScopeService;

    @Override
    public PageResult<SrmSupplierCandidateRespVO> getCandidatePage(SrmSupplierCandidatePageReqVO reqVO) {
        List<SrmSupplierCandidateRespVO> candidates = new ArrayList<>();
        String status = normalizeStatus(reqVO.getStatus());
        if (LEGACY_PENDING_SOURCE.equalsIgnoreCase(reqVO.getSourceType()) && status == null) {
            status = PENDING_STATUS;
        }
        String supplierInfo = trimToNull(reqVO.getSupplierInfo());
        LambdaQueryWrapperX<MesSupplierDO> queryWrapper = new LambdaQueryWrapperX<MesSupplierDO>()
                .likeIfPresent(MesSupplierDO::getSupplierCode, trimToNull(reqVO.getSupplierCode()))
                .likeIfPresent(MesSupplierDO::getSupplierName, trimToNull(reqVO.getSupplierName()))
                .likeIfPresent(MesSupplierDO::getUsingDepartment, trimToNull(reqVO.getUsingDepartment()))
                .likeIfPresent(MesSupplierDO::getMainProducts, trimToNull(reqVO.getMainProducts()))
                .likeIfPresent(MesSupplierDO::getMaterialCode, trimToNull(reqVO.getMaterialCode()))
                .likeIfPresent(MesSupplierDO::getModel, trimToNull(reqVO.getModel()))
                .likeIfPresent(MesSupplierDO::getApplicableProduct,
                        trimToNull(reqVO.getApplicableProduct()))
                .eqIfPresent(MesSupplierDO::getMaterialGrade, trimToNull(reqVO.getMaterialGrade()))
                .eqIfPresent(MesSupplierDO::getLevel, trimToNull(reqVO.getLevel()))
                .eqIfPresent(MesSupplierDO::getCompanyNature, trimToNull(reqVO.getCompanyNature()))
                .eqIfPresent(MesSupplierDO::getScopeId, reqVO.getScopeId())
                .eqIfPresent(MesSupplierDO::getStatus, status);
        if (StrUtil.isNotBlank(supplierInfo)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(MesSupplierDO::getSupplierCode, supplierInfo)
                    .or()
                    .like(MesSupplierDO::getSupplierName, supplierInfo));
        }
        supplierMapper.selectList(applyScopeFilter(queryWrapper
                .orderByDesc(MesSupplierDO::getId)))
                .stream().map(this::buildRegisteredCandidate).forEach(candidates::add);
        if (Boolean.TRUE.equals(reqVO.getDistinctSupplier())) {
            candidates = distinctBySupplier(candidates);
        }
        candidates.sort(Comparator
                .comparing((SrmSupplierCandidateRespVO item) -> statusOrder(item.getStatus()))
                .thenComparing(item -> StrUtil.blankToDefault(item.getSupplierName(), ""))
                .thenComparing(item -> StrUtil.blankToDefault(item.getSupplierCode(), "")));
        int pageNo = Math.max(reqVO.getPageNo(), 1);
        int pageSize = Math.max(reqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, candidates.size());
        int toIndex = Math.min(fromIndex + pageSize, candidates.size());
        return new PageResult<>(candidates.subList(fromIndex, toIndex), (long) candidates.size());
    }

    /**
     * 供应商选择弹窗专用查询：数据库内完成过滤、去重和分页，仅返回选择所需快照字段。
     * 资源池和供应商档案继续使用完整分页接口，避免改变既有列表和详情语义。
     */
    @Override
    public PageResult<SrmSupplierCandidateRespVO> getCandidateSelectPage(
            SrmSupplierCandidatePageReqVO reqVO) {
        String status = normalizeStatus(reqVO.getStatus());
        if (LEGACY_PENDING_SOURCE.equalsIgnoreCase(reqVO.getSourceType()) && status == null) {
            status = PENDING_STATUS;
        }
        Collection<Long> accessibleScopeIds = supplierScopeService.getCurrentAccessibleScopeIds();
        boolean scopeRestricted = accessibleScopeIds != null;
        if (scopeRestricted && accessibleScopeIds.isEmpty()) {
            return PageResult.empty();
        }
        int pageNo = Math.max(reqVO.getPageNo(), 1);
        int pageSize = Math.max(reqVO.getPageSize(), 1);
        long offset = (long) (pageNo - 1) * pageSize;
        Long tenantId = TenantContextHolder.getTenantId();
        String supplierCode = trimToNull(reqVO.getSupplierCode());
        String supplierName = trimToNull(reqVO.getSupplierName());
        boolean distinctSupplier = !Boolean.FALSE.equals(reqVO.getDistinctSupplier());
        Long total = supplierMapper.selectCandidateSelectCount(tenantId, supplierCode,
                supplierName, status, reqVO.getScopeId(), distinctSupplier, scopeRestricted,
                accessibleScopeIds);
        if (total == null || total == 0L) {
            return PageResult.empty();
        }
        List<SrmSupplierCandidateRespVO> list = supplierMapper.selectCandidateSelectPage(
                        tenantId, supplierCode, supplierName, status, reqVO.getScopeId(),
                        distinctSupplier, scopeRestricted, accessibleScopeIds, offset, pageSize)
                .stream()
                .map(this::buildCandidateSelectItem)
                .toList();
        return new PageResult<>(list, total);
    }

    @Override
    public SrmSupplierNameCheckRespVO checkSupplierName(String supplierName) {
        String normalizedName = normalizeSupplierName(supplierName);
        SrmSupplierNameCheckRespVO result = new SrmSupplierNameCheckRespVO();
        result.setNormalizedName(normalizedName);
        if (StrUtil.isBlank(normalizedName)) {
            result.setRegisteredMatches(List.of());
            result.setPendingMatches(List.of());
            return result;
        }
        List<SrmSupplierCandidateRespVO> matches = findRegisteredMatches(normalizedName);
        result.setRegisteredMatches(matches.stream()
                .filter(item -> !PENDING_STATUS.equals(item.getStatus()))
                .toList());
        result.setPendingMatches(matches.stream()
                .filter(item -> PENDING_STATUS.equals(item.getStatus()))
                .toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustResourceStatus(SrmSupplierResourceStatusAdjustReqVO reqVO) {
        String toStatus = StrUtil.trim(reqVO.getStatus());
        if (!RESOURCE_STATUSES.contains(toStatus)) {
            throw exception(SRM_SUPPLIER_RESOURCE_STATUS_INVALID, toStatus);
        }
        adjustRegisteredResourceStatus(reqVO.getSupplierId(), toStatus, reqVO.getReason());
    }

    @Override
    public List<SrmSupplierResourceStatusLogRespVO> getResourceStatusLogs(Long supplierId) {
        if (supplierId == null) {
            return List.of();
        }
        return resourceStatusLogMapper.selectBySupplier(supplierId)
                .stream()
                .map(this::buildStatusLogResp)
                .toList();
    }

    @Override
    public SupplierResolution resolveSurveySupplier(Boolean unregisteredSupplier, Long supplierId,
            String supplierName, Boolean confirmInitialize,
            String sourceSurveyNo) {
        if (!Boolean.TRUE.equals(unregisteredSupplier)) {
            MesSupplierDO supplier = supplierId == null ? null : supplierMapper.selectById(supplierId);
            if (supplier == null) {
                throw exception(SRM_SUPPLIER_SELECTION_REQUIRED);
            }
            supplierScopeService.assertSupplierVisible(supplier);
            return new SupplierResolution(supplier.getId(), supplier.getSupplierCode(),
                    supplier.getSupplierName(), SOURCE_REGISTERED, false);
        }
        if (supplierId != null) {
            MesSupplierDO supplier = supplierMapper.selectById(supplierId);
            if (supplier == null) {
                throw exception(SRM_SUPPLIER_RESOURCE_NOT_EXISTS);
            }
            supplierScopeService.assertSupplierVisible(supplier);
            if (StrUtil.isNotBlank(supplierName)
                    && !normalizeSupplierName(supplierName).equals(normalizeSupplierName(supplier.getSupplierName()))) {
                throw exception(SRM_SUPPLIER_SELECTION_REQUIRED);
            }
            return new SupplierResolution(supplier.getId(), supplier.getSupplierCode(),
                    supplier.getSupplierName(), SOURCE_REGISTERED, !QUALIFIED_STATUS.equals(supplier.getStatus()));
        }
        String normalizedName = normalizeSupplierName(supplierName);
        if (StrUtil.isBlank(normalizedName)) {
            throw exception(SRM_SUPPLIER_SELECTION_REQUIRED);
        }
        List<SrmSupplierCandidateRespVO> registeredMatches = findRegisteredMatches(normalizedName);
        List<SrmSupplierCandidateRespVO> pendingMatches = registeredMatches.stream()
                .filter(item -> PENDING_STATUS.equals(item.getStatus()))
                .toList();
        if (!pendingMatches.isEmpty()) {
            throw exception(SRM_SUPPLIER_RESOURCE_NAME_DUPLICATE,
                    pendingMatches.get(0).getSupplierCode());
        }
        if (!registeredMatches.isEmpty()) {
            throw exception(SRM_REGISTERED_SUPPLIER_NAME_DUPLICATE,
                    registeredMatches.get(0).getSupplierCode());
        }
        if (!Boolean.TRUE.equals(confirmInitialize)) {
            throw exception(SRM_SUPPLIER_RESOURCE_CONFIRM_REQUIRED);
        }
        MesSupplierDO supplier = new MesSupplierDO();
        supplier.setSupplierCode(buildTempSupplierCode());
        supplier.setSupplierName(StrUtil.trim(supplierName));
        supplier.setStatus(PENDING_STATUS);
        supplier.setSourceSurveyNo(sourceSurveyNo);
        supplier.setRegistrarId(SecurityFrameworkUtils.getLoginUserId());
        supplier.setRegistrarName(SecurityFrameworkUtils.getLoginUserNickname());
        supplier.setInitializationReason("基本情况调查确认初始化");
        supplier.setVersion(INITIAL_VERSION);
        supplier.setTenantId(TenantContextHolder.getTenantId());
        try {
            supplierMapper.insert(supplier);
        } catch (DuplicateKeyException ex) {
            List<SrmSupplierCandidateRespVO> duplicates = findRegisteredMatches(normalizedName);
            String duplicateCode = duplicates.isEmpty() ? "-" : duplicates.get(0).getSupplierCode();
            throw exception(SRM_SUPPLIER_RESOURCE_NAME_DUPLICATE, duplicateCode);
        }
        return new SupplierResolution(supplier.getId(), supplier.getSupplierCode(),
                supplier.getSupplierName(), SOURCE_REGISTERED, true);
    }

    @Override
    public void bindSupplierSourceToSurvey(Long supplierId, Long surveyId) {
        if (supplierId == null || surveyId == null) {
            return;
        }
        MesSupplierDO supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || supplier.getSourceSurveyId() != null) {
            return;
        }
        MesSupplierDO updateObj = new MesSupplierDO();
        updateObj.setId(supplierId);
        updateObj.setSourceSurveyId(surveyId);
        updateObj.setVersion(supplier.getVersion());
        supplierMapper.updateById(updateObj);
    }

    private List<SrmSupplierCandidateRespVO> findRegisteredMatches(String normalizedName) {
        return supplierMapper.selectList(applyScopeFilter(new LambdaQueryWrapperX<MesSupplierDO>()
                        .isNotNull(MesSupplierDO::getSupplierName)
                        .orderByDesc(MesSupplierDO::getId)))
                .stream()
                .filter(item -> normalizedName.equals(normalizeSupplierName(item.getSupplierName())))
                .map(this::buildRegisteredCandidate)
                .toList();
    }

    private SrmSupplierCandidateRespVO buildRegisteredCandidate(MesSupplierDO supplier) {
        MesSupplierRespVO supplierResp = supplierScopeService.buildSupplierResp(supplier);
        SrmSupplierCandidateRespVO item = new SrmSupplierCandidateRespVO();
        item.setCandidateKey(SOURCE_REGISTERED + ":" + supplier.getId());
        item.setSupplierId(supplierResp.getId());
        item.setSupplierCode(supplierResp.getSupplierCode());
        item.setSupplierName(supplierResp.getSupplierName());
        item.setUsingDepartment(supplierResp.getUsingDepartment());
        item.setShortName(supplierResp.getShortName());
        item.setContactPerson(supplierResp.getContactPerson());
        item.setContactPhone(supplierResp.getContactPhone());
        item.setEmail(supplierResp.getEmail());
        item.setAddress(supplierResp.getAddress());
        item.setCompanyNature(supplierResp.getCompanyNature());
        item.setLegalPerson(supplierResp.getLegalPerson());
        item.setRegisteredCapital(supplierResp.getRegisteredCapital());
        item.setEstablishDate(supplierResp.getEstablishDate());
        item.setMainProducts(supplierResp.getMainProducts());
        item.setProvidedProduct(supplierResp.getProvidedProduct());
        item.setMaterialCode(supplierResp.getMaterialCode());
        item.setModel(supplierResp.getModel());
        item.setApplicableProduct(supplierResp.getApplicableProduct());
        item.setImportDate(supplierResp.getImportDate());
        item.setMaterialCategory(supplierResp.getMaterialCategory());
        item.setMaterialGrade(supplierResp.getMaterialGrade());
        item.setPaymentTerms(supplierResp.getPaymentTerms());
        item.setDeliveryMethod(supplierResp.getDeliveryMethod());
        item.setOriginPlace(supplierResp.getOriginPlace());
        item.setOriginalFactoryInfo(supplierResp.getOriginalFactoryInfo());
        item.setLevel(supplierResp.getLevel());
        item.setRemark(supplierResp.getRemark());
        item.setSourceType(SOURCE_REGISTERED);
        item.setUnregisteredSupplier(!QUALIFIED_STATUS.equals(supplierResp.getStatus()));
        item.setStatus(supplierResp.getStatus());
        item.setSourceSurveyId(supplierResp.getSourceSurveyId());
        item.setSourceSurveyNo(supplierResp.getSourceSurveyNo());
        item.setScopeId(supplierResp.getScopeId());
        item.setScopeCode(supplierResp.getScopeCode());
        item.setScopeName(supplierResp.getScopeName());
        item.setViewPermission(supplierResp.getViewPermission());
        item.setCanEdit(supplierResp.getCanEdit());
        item.setMaskedFields(supplierResp.getMaskedFields());
        return item;
    }

    private SrmSupplierCandidateRespVO buildCandidateSelectItem(MesSupplierDO supplier) {
        SrmSupplierCandidateRespVO item = new SrmSupplierCandidateRespVO();
        item.setCandidateKey(SOURCE_REGISTERED + ":" + supplier.getId());
        item.setSupplierId(supplier.getId());
        item.setSupplierCode(supplier.getSupplierCode());
        item.setSupplierName(supplier.getSupplierName());
        item.setContactPerson(supplier.getContactPerson());
        item.setUsingDepartment(supplier.getUsingDepartment());
        item.setMainProducts(supplier.getMainProducts());
        item.setProvidedProduct(supplier.getProvidedProduct());
        item.setMaterialCode(supplier.getMaterialCode());
        item.setModel(supplier.getModel());
        item.setApplicableProduct(supplier.getApplicableProduct());
        item.setMaterialCategory(supplier.getMaterialCategory());
        item.setMaterialGrade(supplier.getMaterialGrade());
        item.setSourceType(SOURCE_REGISTERED);
        item.setUnregisteredSupplier(!QUALIFIED_STATUS.equals(supplier.getStatus()));
        item.setStatus(supplier.getStatus());
        item.setScopeId(supplier.getScopeId());
        item.setScopeCode(supplier.getScopeCode());
        item.setScopeName(supplier.getScopeName());
        return item;
    }

    private LambdaQueryWrapper<MesSupplierDO> applyScopeFilter(LambdaQueryWrapper<MesSupplierDO> wrapper) {
        Collection<Long> scopeIds = supplierScopeService.getCurrentAccessibleScopeIds();
        if (scopeIds == null) {
            return wrapper;
        }
        return wrapper.in(MesSupplierDO::getScopeId, scopeIds.isEmpty() ? List.of(-1L) : scopeIds);
    }

    private String normalizeSupplierName(String supplierName) {
        if (StrUtil.isBlank(supplierName)) {
            return "";
        }
        return Normalizer.normalize(StrUtil.trim(supplierName), Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String buildTempSupplierCode() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase(Locale.ROOT);
        return "TMP-SUP-" + date + "-" + suffix;
    }

    private List<SrmSupplierCandidateRespVO> distinctBySupplier(List<SrmSupplierCandidateRespVO> candidates) {
        LinkedHashMap<String, SrmSupplierCandidateRespVO> distinctMap = new LinkedHashMap<>();
        for (SrmSupplierCandidateRespVO candidate : candidates) {
            distinctMap.putIfAbsent(buildSupplierDistinctKey(candidate), candidate);
        }
        return new ArrayList<>(distinctMap.values());
    }

    private String buildSupplierDistinctKey(SrmSupplierCandidateRespVO candidate) {
        String supplierCode = StrUtil.trimToEmpty(candidate.getSupplierCode());
        String supplierName = StrUtil.trimToEmpty(candidate.getSupplierName());
        if (StrUtil.isBlank(supplierCode) && StrUtil.isBlank(supplierName)) {
            return StrUtil.blankToDefault(candidate.getCandidateKey(), "");
        }
        return supplierCode + "\u0001" + supplierName;
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = trimToNull(status);
        return "ALL".equalsIgnoreCase(String.valueOf(normalizedStatus)) ? null : normalizedStatus;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }

    private int statusOrder(String status) {
        if (QUALIFIED_STATUS.equals(status)) {
            return 10;
        }
        if (PENDING_STATUS.equals(status)) {
            return 20;
        }
        if ("EXITED".equals(status)) {
            return 30;
        }
        if ("UNQUALIFIED".equals(status)) {
            return 40;
        }
        return 50;
    }

    private void adjustRegisteredResourceStatus(Long supplierId, String toStatus, String reason) {
        if (supplierId == null) {
            throw exception(SRM_SUPPLIER_RESOURCE_STATUS_INVALID, toStatus);
        }
        MesSupplierDO supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw exception(SRM_SUPPLIER_SELECTION_REQUIRED);
        }
        supplierScopeService.assertSupplierEditable(supplier);
        String fromStatus = supplier.getStatus();
        MesSupplierDO updateObj = new MesSupplierDO();
        updateObj.setId(supplierId);
        updateObj.setStatus(toStatus);
        updateObj.setVersion(supplier.getVersion());
        if (supplierMapper.updateById(updateObj) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
        insertResourceStatusLog(SOURCE_REGISTERED, supplierId, supplier.getSupplierCode(),
                supplier.getSupplierName(), fromStatus, toStatus, reason);
    }

    private void insertResourceStatusLog(String sourceType, Long supplierId, String supplierCode,
            String supplierName, String fromStatus, String toStatus, String reason) {
        SrmSupplierResourceStatusLogDO log = new SrmSupplierResourceStatusLogDO();
        log.setSourceType(sourceType);
        log.setSupplierId(supplierId);
        log.setSupplierCode(supplierCode);
        log.setSupplierName(supplierName);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReason(StrUtil.trim(reason));
        log.setOperatorUserId(SecurityFrameworkUtils.getLoginUserId());
        log.setOperatorUserName(SecurityFrameworkUtils.getLoginUserNickname());
        log.setTenantId(TenantContextHolder.getTenantId());
        resourceStatusLogMapper.insert(log);
    }

    private SrmSupplierResourceStatusLogRespVO buildStatusLogResp(SrmSupplierResourceStatusLogDO log) {
        SrmSupplierResourceStatusLogRespVO respVO = new SrmSupplierResourceStatusLogRespVO();
        respVO.setId(log.getId());
        respVO.setSourceType(log.getSourceType());
        respVO.setSupplierId(log.getSupplierId());
        respVO.setSupplierCode(log.getSupplierCode());
        respVO.setSupplierName(log.getSupplierName());
        respVO.setFromStatus(log.getFromStatus());
        respVO.setToStatus(log.getToStatus());
        respVO.setReason(log.getReason());
        respVO.setOperatorUserId(log.getOperatorUserId());
        respVO.setOperatorUserName(log.getOperatorUserName());
        respVO.setCreateTime(log.getCreateTime());
        return respVO;
    }

}
