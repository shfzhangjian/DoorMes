package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardChangeLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardAuditSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardChangeLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardAuditSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardChangeLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.service.hc.material.HcMaterialService;
import cn.iocoder.yudao.module.mes.service.hc.productmodel.HcProductModelService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMATERIAL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRODUCTMODEL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_AUDIT_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_AUDIT_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_AUDIT_SNAPSHOT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_AUDITED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_APPLY_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_APPLY_TYPE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_BIZ_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_DATE_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ENTRY_RULE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_GLUE_BOARD_MODEL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_MATERIAL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_PROCESS_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_PRODUCT_MODEL_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_REFERENCED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_REJECT_REASON_REQUIRED;

@Service
@Validated
@Slf4j
public class QmsQualityStandardServiceImpl implements QmsQualityStandardService {

    private static final Integer AUDIT_STATUS_UNAUDITED = 10;
    private static final Integer AUDIT_STATUS_AUDITED = 20;
    private static final String AUDIT_RESULT_PASS = "PASS";
    private static final String AUDIT_RESULT_REJECT = "REJECT";
    private static final String SNAPSHOT_STATUS_PENDING = "PENDING";
    private static final String SNAPSHOT_STATUS_PASS = "PASS";
    private static final String SNAPSHOT_STATUS_REJECT = "REJECT";
    private static final String CHANGE_SCOPE_MAIN = "MAIN";
    private static final String CHANGE_SCOPE_ITEM = "ITEM";
    private static final String ITEM_TYPE_QUALITATIVE = "QUALITATIVE";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_DATE = "DATE";
    private static final String APPLY_TYPE_IQC = "IQC";
    private static final String APPLY_TYPE_FAI = "FAI";
    private static final String APPLY_TYPE_FQC = "FQC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final String TEMPLATE_OQC_CHECKLIST_YES_NO_NA = "OQC_CHECKLIST_YES_NO_NA";
    private static final String METRIC_COMPRESSION_RATE = "compressionRate";
    private static final String METRIC_COMPRESSION_ELASTICITY_RATE = "compressionElasticityRate";
    private static final Set<String> SUPPORTED_VALUE_TEMPLATES = Set.of("SINGLE_VALUE", "DENSITY_CALC", "COMPRESSION_CALC");
    private static final String APPLY_TYPE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String MODEL_LEVEL_FAMILY = "FAMILY";
    private static final Set<String> SUPPORT_APPLY_TYPES = Set.of("IQC", "FAI", "GLUE_BOARD_FAI", "IPQC", "FQC", "OQC");
    private static final TypeReference<Map<String, Object>> TEMPLATE_PARAMS_TYPE = new TypeReference<>() {};
    private static final TypeReference<QmsQualityStandardDO> STANDARD_SNAPSHOT_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<QmsQualityStandardItemDO>> STANDARD_ITEM_SNAPSHOT_TYPE = new TypeReference<>() {};

    @Resource
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Resource
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Resource
    private QmsQualityStandardChangeLogMapper qmsQualityStandardChangeLogMapper;
    @Resource
    private QmsQualityStandardAuditSnapshotMapper qmsQualityStandardAuditSnapshotMapper;
    @Resource
    private HcMaterialService hcMaterialService;
    @Resource
    private HcProductModelService hcProductModelService;
    @Resource
    private QmsAuditTodoNotifyService qmsAuditTodoNotifyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQualityStandard(QmsQualityStandardSaveReqVO createReqVO) {
        createReqVO.setApplyType(normalizeApplyType(createReqVO.getApplyType()));
        normalizeGlueBoardStandard(createReqVO);
        validateProductModel(createReqVO);
        validateApplyScope(createReqVO);
        validateProcessScope(createReqVO);
        validateMaterial(createReqVO);
        validateStandardNoUnique(null, createReqVO.getStandardNo());
        validateBusinessUnique(null, createReqVO);
        validateEntryRules(createReqVO.getItems(), createReqVO.getApplyType());

        QmsQualityStandardDO entity = BeanUtils.toBean(createReqVO, QmsQualityStandardDO.class);
        entity.setStandardNo(createReqVO.getStandardNo().trim());
        entity.setAuditStatus(AUDIT_STATUS_UNAUDITED);
        qmsQualityStandardMapper.insert(entity);
        saveItems(entity, createReqVO.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQualityStandard(QmsQualityStandardSaveReqVO createReqVO, String fixedApplyType) {
        createReqVO.setApplyType(normalizeApplyType(fixedApplyType));
        return createQualityStandard(createReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQualityStandard(QmsQualityStandardSaveReqVO updateReqVO) {
        QmsQualityStandardDO entity = validateQualityStandardExists(updateReqVO.getId());
        List<QmsQualityStandardItemDO> oldItems = qmsQualityStandardItemMapper.selectListByStandardId(updateReqVO.getId());
        validateSameApplyType(entity, updateReqVO.getApplyType());
        updateReqVO.setApplyType(normalizeApplyType(updateReqVO.getApplyType()));
        normalizeGlueBoardStandard(updateReqVO);
        validateProductModel(updateReqVO);
        validateApplyScope(updateReqVO);
        validateProcessScope(updateReqVO);
        validateMaterial(updateReqVO);
        validateStandardNoUnique(updateReqVO.getId(), updateReqVO.getStandardNo());
        validateBusinessUnique(updateReqVO.getId(), updateReqVO);
        validateEntryRules(updateReqVO.getItems(), updateReqVO.getApplyType());

        if (shouldCreateAuditSnapshot(entity)) {
            saveAuditSnapshot(entity, oldItems);
        }

        QmsQualityStandardDO updateObj = BeanUtils.toBean(updateReqVO, QmsQualityStandardDO.class);
        updateObj.setStandardNo(updateReqVO.getStandardNo().trim());
        updateObj.setAuditStatus(AUDIT_STATUS_UNAUDITED);
        qmsQualityStandardMapper.updateById(updateObj);
        qmsQualityStandardMapper.updateAuditInfo(updateReqVO.getId(), AUDIT_STATUS_UNAUDITED, null, null, null);

        qmsQualityStandardItemMapper.deleteByStandardId(updateReqVO.getId());
        List<QmsQualityStandardItemDO> newItems = saveItems(updateObj, updateReqVO.getItems());
        saveChangeLogs(entity, oldItems, updateObj, newItems);
        sendAuditTodoNotifyIfNeeded(entity, updateObj, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQualityStandard(QmsQualityStandardSaveReqVO updateReqVO, String fixedApplyType) {
        String applyType = normalizeApplyType(fixedApplyType);
        QmsQualityStandardDO entity = validateQualityStandardExists(updateReqVO.getId());
        validateSameApplyType(entity, applyType);
        updateReqVO.setApplyType(applyType);
        updateQualityStandard(updateReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditQualityStandard(QmsQualityStandardAuditReqVO reqVO) {
        QmsQualityStandardDO entity = validateQualityStandardExists(reqVO.getId());
        if (AUDIT_STATUS_AUDITED.equals(entity.getAuditStatus())) {
            throw exception(HCQUALITYSTANDARD_AUDITED);
        }

        String auditResult = normalizeAuditResult(reqVO.getAuditResult());
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String auditorName = SecurityFrameworkUtils.getLoginUserNickname();
        if (!StringUtils.hasText(auditorName)) {
            auditorName = loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
        }
        LocalDateTime auditTime = LocalDateTime.now();

        if (AUDIT_RESULT_REJECT.equals(auditResult)) {
            rejectAudit(entity, reqVO.getRejectReason(), loginUserId, auditorName, auditTime);
            return;
        }

        QmsQualityStandardDO updateObj = new QmsQualityStandardDO();
        updateObj.setId(reqVO.getId());
        updateObj.setAuditStatus(AUDIT_STATUS_AUDITED);
        updateObj.setAuditorId(loginUserId);
        updateObj.setAuditorName(auditorName);
        updateObj.setAuditTime(auditTime);
        qmsQualityStandardMapper.updateById(updateObj);
        qmsQualityStandardMapper.clearAuditNotifyTime(reqVO.getId());
        markLatestPendingSnapshot(entity.getId(), AUDIT_RESULT_PASS, null, loginUserId, auditorName, auditTime,
                SNAPSHOT_STATUS_PASS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditQualityStandard(QmsQualityStandardAuditReqVO reqVO, String fixedApplyType) {
        validateSameApplyType(validateQualityStandardExists(reqVO.getId()), fixedApplyType);
        auditQualityStandard(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualityStandard(Long id) {
        validateQualityStandardExists(id);
        validateQualityStandardNotReferenced(id);
        qmsQualityStandardMapper.deleteById(id);
        qmsQualityStandardItemMapper.deleteByStandardId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualityStandard(Long id, String fixedApplyType) {
        validateSameApplyType(validateQualityStandardExists(id), fixedApplyType);
        deleteQualityStandard(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualityStandardList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::validateQualityStandardExists);
        qmsQualityStandardMapper.deleteBatchIds(ids);
        qmsQualityStandardItemMapper.deleteByStandardIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualityStandardList(List<Long> ids, String fixedApplyType) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> validateSameApplyType(validateQualityStandardExists(id), fixedApplyType));
        deleteQualityStandardList(ids);
    }

    @Override
    public QmsQualityStandardDO getQualityStandard(Long id) {
        return validateQualityStandardExists(id);
    }

    @Override
    public QmsQualityStandardRespVO getQualityStandardResp(Long id) {
        QmsQualityStandardDO entity = validateQualityStandardExists(id);
        QmsQualityStandardRespVO respVO = BeanUtils.toBean(entity, QmsQualityStandardRespVO.class);
        respVO.setItems(BeanUtils.toBean(
                qmsQualityStandardItemMapper.selectListByStandardId(id),
                QmsQualityStandardRespVO.StandardItem.class));
        return respVO;
    }

    @Override
    public QmsQualityStandardRespVO getQualityStandardResp(Long id, String fixedApplyType) {
        validateSameApplyType(validateQualityStandardExists(id), fixedApplyType);
        return getQualityStandardResp(id);
    }

    @Override
    public PageResult<QmsQualityStandardDO> getQualityStandardPage(QmsQualityStandardPageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getApplyType())) {
            pageReqVO.setApplyType(normalizeApplyType(pageReqVO.getApplyType()));
        }
        return qmsQualityStandardMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<QmsQualityStandardDO> getQualityStandardPage(QmsQualityStandardPageReqVO pageReqVO, String fixedApplyType) {
        pageReqVO.setApplyType(normalizeApplyType(fixedApplyType));
        return getQualityStandardPage(pageReqVO);
    }

    @Override
    public List<QmsQualityStandardChangeLogRespVO> getQualityStandardChangeLogs(Long standardId, String applyType) {
        String normalizedApplyType = normalizeApplyType(applyType);
        validateSameApplyType(validateQualityStandardExists(standardId), normalizedApplyType);
        return BeanUtils.toBean(
                qmsQualityStandardChangeLogMapper.selectListByStandard(standardId, normalizedApplyType),
                QmsQualityStandardChangeLogRespVO.class);
    }

    private boolean shouldCreateAuditSnapshot(QmsQualityStandardDO entity) {
        return entity != null && AUDIT_STATUS_AUDITED.equals(entity.getAuditStatus());
    }

    private void saveAuditSnapshot(QmsQualityStandardDO entity, List<QmsQualityStandardItemDO> oldItems) {
        LocalDateTime now = LocalDateTime.now();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String changeUserName = resolveCurrentUserName(loginUserId);
        QmsQualityStandardAuditSnapshotDO snapshot = QmsQualityStandardAuditSnapshotDO.builder()
                .standardId(entity.getId())
                .applyType(entity.getApplyType())
                .mainSnapshotJson(JsonUtils.toJsonString(entity))
                .itemsSnapshotJson(JsonUtils.toJsonString(oldItems == null ? List.of() : oldItems))
                .beforeAuditStatus(entity.getAuditStatus())
                .beforeAuditorId(entity.getAuditorId())
                .beforeAuditorName(entity.getAuditorName())
                .beforeAuditTime(entity.getAuditTime())
                .changeUserId(loginUserId)
                .changeUserName(changeUserName)
                .changeTime(now)
                .snapshotStatus(SNAPSHOT_STATUS_PENDING)
                .tenantId(entity.getTenantId())
                .build();
        qmsQualityStandardAuditSnapshotMapper.insert(snapshot);
    }

    private String normalizeAuditResult(String auditResult) {
        if (!StringUtils.hasText(auditResult)) {
            throw exception(HCQUALITYSTANDARD_AUDIT_RESULT_INVALID);
        }
        String normalized = auditResult.trim().toUpperCase();
        if (!AUDIT_RESULT_PASS.equals(normalized) && !AUDIT_RESULT_REJECT.equals(normalized)) {
            throw exception(HCQUALITYSTANDARD_AUDIT_RESULT_INVALID);
        }
        return normalized;
    }

    private void rejectAudit(QmsQualityStandardDO entity, String rejectReason, Long auditUserId, String auditUserName,
                             LocalDateTime auditTime) {
        String reason = StringUtils.trimWhitespace(rejectReason);
        if (!StringUtils.hasText(reason)) {
            throw exception(HCQUALITYSTANDARD_REJECT_REASON_REQUIRED);
        }
        QmsQualityStandardAuditSnapshotDO snapshot =
                qmsQualityStandardAuditSnapshotMapper.selectLatestPendingByStandardId(entity.getId());
        if (snapshot == null) {
            throw exception(HCQUALITYSTANDARD_AUDIT_SNAPSHOT_NOT_EXISTS);
        }

        QmsQualityStandardDO restoredMain = JsonUtils.parseObjectQuietly(
                snapshot.getMainSnapshotJson(), STANDARD_SNAPSHOT_TYPE);
        List<QmsQualityStandardItemDO> restoredItems = JsonUtils.parseObjectQuietly(
                snapshot.getItemsSnapshotJson(), STANDARD_ITEM_SNAPSHOT_TYPE);
        if (restoredMain == null || restoredItems == null) {
            throw exception(HCQUALITYSTANDARD_AUDIT_SNAPSHOT_INVALID);
        }

        restoredMain.setId(entity.getId());
        restoredMain.setAuditStatus(snapshot.getBeforeAuditStatus());
        restoredMain.setAuditorId(snapshot.getBeforeAuditorId());
        restoredMain.setAuditorName(snapshot.getBeforeAuditorName());
        restoredMain.setAuditTime(snapshot.getBeforeAuditTime());
        qmsQualityStandardMapper.restoreMainFromSnapshot(restoredMain);

        qmsQualityStandardItemMapper.deleteByStandardId(entity.getId());
        restoreSnapshotItems(restoredMain, restoredItems);
        qmsQualityStandardMapper.clearAuditNotifyTime(entity.getId());
        qmsQualityStandardAuditSnapshotMapper.markAuditResult(snapshot.getId(), AUDIT_RESULT_REJECT, reason,
                auditUserId, auditUserName, auditTime, SNAPSHOT_STATUS_REJECT);
    }

    private void sendAuditTodoNotifyIfNeeded(QmsQualityStandardDO oldMain, QmsQualityStandardDO newMain,
                                             LocalDateTime submitTime) {
        qmsAuditTodoNotifyService.sendAuditTodoIfNeeded(oldMain.getAuditNotifyTime(),
                "检验标准定义",
                newMain.getStandardNo(),
                resolveStandardDisplayName(newMain),
                "请进入质量管理-检验标准定义，使用现有审核按钮完成审核。",
                submitTime,
                time -> qmsQualityStandardMapper.updateAuditNotifyTime(newMain.getId(), time));
    }

    private String resolveStandardDisplayName(QmsQualityStandardDO standard) {
        if (StringUtils.hasText(standard.getStandardName())) {
            return standard.getStandardName();
        }
        if (StringUtils.hasText(standard.getProcessName())) {
            return standard.getProcessName();
        }
        if (StringUtils.hasText(standard.getMaterialName())) {
            return standard.getMaterialName();
        }
        if (StringUtils.hasText(standard.getProductModelName())) {
            return standard.getProductModelName();
        }
        if (StringUtils.hasText(standard.getGlueBoardModel())) {
            return standard.getGlueBoardModel();
        }
        return "";
    }

    private void restoreSnapshotItems(QmsQualityStandardDO restoredMain, List<QmsQualityStandardItemDO> restoredItems) {
        for (QmsQualityStandardItemDO item : restoredItems) {
            item.setId(null);
            item.setStandardId(restoredMain.getId());
            item.setStandardNo(restoredMain.getStandardNo());
            item.setStandardName(restoredMain.getStandardName());
            item.setMaterialId(restoredMain.getMaterialId());
            item.setMaterialCode(restoredMain.getMaterialCode());
            item.setMaterialName(restoredMain.getMaterialName());
            item.setApplyType(restoredMain.getApplyType());
            item.setVersion(restoredMain.getVersion());
            item.setStatus(restoredMain.getStatus());
            item.setTenantId(restoredMain.getTenantId());
        }
        if (!restoredItems.isEmpty()) {
            qmsQualityStandardItemMapper.insertBatch(restoredItems);
        }
    }

    private void markLatestPendingSnapshot(Long standardId, String auditResult, String rejectReason, Long auditUserId,
                                           String auditUserName, LocalDateTime auditTime, String snapshotStatus) {
        QmsQualityStandardAuditSnapshotDO snapshot =
                qmsQualityStandardAuditSnapshotMapper.selectLatestPendingByStandardId(standardId);
        if (snapshot == null) {
            return;
        }
        qmsQualityStandardAuditSnapshotMapper.markAuditResult(snapshot.getId(), auditResult, rejectReason,
                auditUserId, auditUserName, auditTime, snapshotStatus);
    }

    private String resolveCurrentUserName(Long loginUserId) {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private List<QmsQualityStandardItemDO> saveItems(QmsQualityStandardDO standard, List<QmsQualityStandardSaveReqVO.StandardItem> items) {
        List<QmsQualityStandardItemDO> itemList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            QmsQualityStandardSaveReqVO.StandardItem item = items.get(i);
            QmsQualityStandardItemDO itemDO = BeanUtils.toBean(item, QmsQualityStandardItemDO.class);
            itemDO.setId(null);
            itemDO.setStandardId(standard.getId());
            itemDO.setSort((i + 1) * 10);
            itemDO.setEntryRuleType(StringUtils.hasText(item.getEntryRuleType()) ? item.getEntryRuleType() : "PRESET");
            itemDO.setStandardNo(standard.getStandardNo());
            itemDO.setStandardName(standard.getStandardName());
            itemDO.setMaterialId(standard.getMaterialId());
            itemDO.setMaterialCode(standard.getMaterialCode());
            itemDO.setMaterialName(standard.getMaterialName());
            itemDO.setProcessId(standard.getProcessId());
            itemDO.setProcessCode(standard.getProcessCode());
            itemDO.setProcessName(standard.getProcessName());
            itemDO.setApplyType(standard.getApplyType());
            itemDO.setVersion(standard.getVersion());
            itemDO.setStatus(standard.getStatus());
            itemList.add(itemDO);
        }
        if (!itemList.isEmpty()) {
            qmsQualityStandardItemMapper.insertBatch(itemList);
        }
        return itemList;
    }

    private void validateEntryRules(List<QmsQualityStandardSaveReqVO.StandardItem> items, String applyType) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (QmsQualityStandardSaveReqVO.StandardItem item : items) {
            // IQC、FAI 检验项附件上传为统一能力，禁止通过接口关闭。
            item.setAttachmentEnabled(isItemAttachmentAlwaysEnabled(applyType)
                    || Boolean.TRUE.equals(item.getAttachmentEnabled()));
            item.setActualValueRequired(APPLY_TYPE_FQC.equals(applyType)
                    && Boolean.TRUE.equals(item.getActualValueRequired()));
            if (ITEM_TYPE_DATE.equals(item.getItemType())) {
                if (!APPLY_TYPE_IQC.equals(applyType) || item.getExpiryDays() == null || item.getExpiryDays() < 0) {
                    throw exception(HCQUALITYSTANDARD_DATE_ITEM_INVALID);
                }
            } else {
                item.setExpiryDays(null);
            }
            try {
                normalizeAndValidateValueTemplate(item);
                if (!ITEM_TYPE_DATE.equals(item.getItemType())) {
                    normalizeCompressionEntryRule(item);
                    QmsEntryRuleFormulaSupport.validateTemplateParams(item.getTemplateParams());
                    if (QmsEntryRuleFormulaSupport.hasDataRule(item.getTemplateParams())) {
                        item.setJudgmentMetric(QmsEntryRuleFormulaSupport.resolveJudgmentMetric(
                                item.getTemplateParams(), item.getJudgmentMetric()));
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw exception(HCQUALITYSTANDARD_ENTRY_RULE_INVALID);
            }
        }
    }

    static boolean isItemAttachmentAlwaysEnabled(String applyType) {
        return APPLY_TYPE_IQC.equals(applyType) || APPLY_TYPE_FAI.equals(applyType);
    }

    private void normalizeAndValidateValueTemplate(QmsQualityStandardSaveReqVO.StandardItem item) {
        String valueTemplate = StringUtils.trimWhitespace(item.getValueTemplate());
        if (ITEM_TYPE_DATE.equals(item.getItemType())) {
            item.setValueTemplate(null);
            item.setJudgmentMetric(null);
            item.setTemplateParams(null);
            item.setEntryRuleTemplateId(null);
            item.setEntryRuleTemplateName(null);
            clearQuantitativeLimitFields(item);
            return;
        }
        if (ITEM_TYPE_QUALITATIVE.equals(item.getItemType())) {
            if (StringUtils.hasText(valueTemplate) && !TEMPLATE_OQC_CHECKLIST_YES_NO_NA.equals(valueTemplate)) {
                throw new IllegalArgumentException("qualitative item should not carry valueTemplate");
            }
            item.setValueTemplate(null);
            item.setJudgmentMetric(null);
            clearQuantitativeLimitFields(item);
            return;
        }
        if (!ITEM_TYPE_QUANTITATIVE.equals(item.getItemType()) || !StringUtils.hasText(valueTemplate)) {
            if (!ITEM_TYPE_QUANTITATIVE.equals(item.getItemType())) {
                throw new IllegalArgumentException("unsupported itemType");
            }
            return;
        }
        if (!SUPPORTED_VALUE_TEMPLATES.contains(valueTemplate)) {
            throw new IllegalArgumentException("unsupported quantitative valueTemplate");
        }
        item.setValueTemplate(valueTemplate);
    }

    private void clearQuantitativeLimitFields(QmsQualityStandardSaveReqVO.StandardItem item) {
        item.setMinValue(null);
        item.setMinValueScale(null);
        item.setMaxValue(null);
        item.setMaxValueScale(null);
        item.setAvgMinLimit(null);
        item.setAvgMinLimitScale(null);
        item.setAvgMaxLimit(null);
        item.setAvgMaxLimitScale(null);
        item.setStdMinLimit(null);
        item.setStdMinLimitScale(null);
        item.setStdMaxLimit(null);
        item.setStdMaxLimitScale(null);
    }

    private void normalizeCompressionEntryRule(QmsQualityStandardSaveReqVO.StandardItem item) {
        if (!TEMPLATE_COMPRESSION_CALC.equals(item.getValueTemplate()) || !StringUtils.hasText(item.getTemplateParams())) {
            return;
        }
        if (QmsEntryRuleFormulaSupport.hasDataRule(item.getTemplateParams())) {
            return;
        }
        Map<String, Object> params = JsonUtils.parseObjectQuietly(item.getTemplateParams(), TEMPLATE_PARAMS_TYPE);
        if (params == null || params.isEmpty()) {
            return;
        }
        String judgmentMetric = resolveCompressionJudgmentMetric(item);
        item.setJudgmentMetric(judgmentMetric);
        params.put("valueTemplate", TEMPLATE_COMPRESSION_CALC);
        params.put("judgmentMetric", judgmentMetric);
        params.putIfAbsent("entryGroupCode", resolveCompressionEntryGroupCode(item));
        params.putIfAbsent("entryGroupName", "压缩性能");

        Map<String, Object> dataRule = asMutableMap(params.get("dataRule"));
        if (!dataRule.isEmpty()) {
            dataRule.put("judgmentMetric", judgmentMetric);
            Object resultFields = dataRule.get("resultFields");
            if (resultFields instanceof List<?> fields) {
                List<Object> normalizedFields = new ArrayList<>();
                for (Object field : fields) {
                    Map<String, Object> resultField = asMutableMap(field);
                    if (resultField.isEmpty()) {
                        normalizedFields.add(field);
                        continue;
                    }
                    resultField.put("judgment", judgmentMetric.equals(String.valueOf(resultField.get("code"))));
                    normalizedFields.add(resultField);
                }
                dataRule.put("resultFields", normalizedFields);
            }
            params.put("dataRule", dataRule);
        }
        item.setTemplateParams(JsonUtils.toJsonString(params));
    }

    private String resolveCompressionJudgmentMetric(QmsQualityStandardSaveReqVO.StandardItem item) {
        String metric = normalizeCompressionMetric(item.getJudgmentMetric());
        if (StringUtils.hasText(metric)) {
            return metric;
        }
        String inspectionItem = item.getInspectionItem() == null ? "" : item.getInspectionItem();
        return inspectionItem.contains("弹性") || inspectionItem.contains("回弹")
                ? METRIC_COMPRESSION_ELASTICITY_RATE
                : METRIC_COMPRESSION_RATE;
    }

    private String normalizeCompressionMetric(String metric) {
        if (!StringUtils.hasText(metric)) {
            return null;
        }
        String normalized = metric.trim().replace('-', '_').toUpperCase();
        if ("COMPRESSION_ELASTICITY_RATE".equals(normalized)) {
            return METRIC_COMPRESSION_ELASTICITY_RATE;
        }
        if ("COMPRESSION_RATE".equals(normalized)) {
            return METRIC_COMPRESSION_RATE;
        }
        if (METRIC_COMPRESSION_ELASTICITY_RATE.equals(metric) || METRIC_COMPRESSION_RATE.equals(metric)) {
            return metric;
        }
        return null;
    }

    private String resolveCompressionEntryGroupCode(QmsQualityStandardSaveReqVO.StandardItem item) {
        String source = String.join(" ",
                defaultString(item.getInspectionItem()),
                defaultString(item.getProcessName()),
                defaultString(item.getProcessCode()));
        String upperSource = source.toUpperCase();
        if (source.contains("成品") || upperSource.contains("FINAL")) {
            return "COMPRESSION_FINAL_PRODUCTS";
        }
        if (source.contains("磨皮后") || source.contains("磨后") || upperSource.contains("NAP_POLISHED")) {
            return "COMPRESSION_NAP_POLISHED";
        }
        if (source.contains("未磨") || upperSource.contains("NAP_RAW")) {
            return "COMPRESSION_NAP_RAW";
        }
        return "COMPRESSION_PERFORMANCE";
    }

    private Map<String, Object> asMutableMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, child) -> result.put(String.valueOf(key), child));
        return result;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private void saveChangeLogs(QmsQualityStandardDO oldMain, List<QmsQualityStandardItemDO> oldItems,
                                QmsQualityStandardDO newMain, List<QmsQualityStandardItemDO> newItems) {
        List<QmsQualityStandardChangeLogDO> logs = new ArrayList<>();
        LocalDateTime changeTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        if (!StringUtils.hasText(operatorName)) {
            operatorName = operatorId == null ? "当前用户" : String.valueOf(operatorId);
        }

        for (FieldConfig<QmsQualityStandardDO> field : mainFieldConfigs()) {
            addChangeLogIfChanged(logs, oldMain.getId(), oldMain.getApplyType(), CHANGE_SCOPE_MAIN,
                    null, null, field, oldMain, newMain, operatorId, operatorName, changeTime);
        }

        Set<Long> matchedOldIds = new HashSet<>();
        Map<String, Queue<QmsQualityStandardItemDO>> oldPrimaryIndex = indexItems(oldItems, this::buildItemPrimaryKey);
        Map<String, Queue<QmsQualityStandardItemDO>> oldFallbackIndex = indexItems(oldItems, this::buildItemFallbackKey);
        for (QmsQualityStandardItemDO newItem : newItems) {
            QmsQualityStandardItemDO oldItem = pollUnmatched(oldPrimaryIndex.get(buildItemPrimaryKey(newItem)), matchedOldIds);
            if (oldItem == null) {
                oldItem = pollUnmatched(oldFallbackIndex.get(buildItemFallbackKey(newItem)), matchedOldIds);
            }
            String itemKey = buildItemPrimaryKey(newItem);
            String itemLabel = buildItemLabel(newItem);
            if (oldItem == null) {
                addItemWholeLog(logs, newMain, itemKey, itemLabel, "新增检验项",
                        "", buildItemSummary(newItem), operatorId, operatorName, changeTime);
                continue;
            }
            matchedOldIds.add(oldItem.getId());
            for (FieldConfig<QmsQualityStandardItemDO> field : itemFieldConfigs()) {
                addChangeLogIfChanged(logs, newMain.getId(), newMain.getApplyType(), CHANGE_SCOPE_ITEM,
                        itemKey, itemLabel, field, oldItem, newItem, operatorId, operatorName, changeTime);
            }
        }
        for (QmsQualityStandardItemDO oldItem : oldItems) {
            if (matchedOldIds.contains(oldItem.getId())) {
                continue;
            }
            addItemWholeLog(logs, newMain, buildItemPrimaryKey(oldItem), buildItemLabel(oldItem), "删除检验项",
                    buildItemSummary(oldItem), "", operatorId, operatorName, changeTime);
        }
        if (!logs.isEmpty()) {
            qmsQualityStandardChangeLogMapper.insertBatch(logs);
        }
    }

    private <T> void addChangeLogIfChanged(List<QmsQualityStandardChangeLogDO> logs, Long standardId, String applyType,
                                           String changeScope, String itemKey, String itemLabel, FieldConfig<T> field,
                                           T oldData, T newData, Long operatorId, String operatorName,
                                           LocalDateTime changeTime) {
        Object oldValue = field.getter().apply(oldData);
        Object newValue = field.getter().apply(newData);
        if (Objects.equals(normalizeCompareValue(oldValue), normalizeCompareValue(newValue))) {
            return;
        }
        logs.add(buildLog(standardId, applyType, changeScope, itemKey, itemLabel, field.fieldName(), field.fieldLabel(),
                formatValue(field.fieldName(), oldValue), formatValue(field.fieldName(), newValue),
                operatorId, operatorName, changeTime));
    }

    private void addItemWholeLog(List<QmsQualityStandardChangeLogDO> logs, QmsQualityStandardDO standard,
                                 String itemKey, String itemLabel, String fieldLabel, String beforeValue,
                                 String afterValue, Long operatorId, String operatorName, LocalDateTime changeTime) {
        logs.add(buildLog(standard.getId(), standard.getApplyType(), CHANGE_SCOPE_ITEM, itemKey, itemLabel,
                "item", fieldLabel, beforeValue, afterValue, operatorId, operatorName, changeTime));
    }

    private QmsQualityStandardChangeLogDO buildLog(Long standardId, String applyType, String changeScope,
                                                   String itemKey, String itemLabel, String fieldName, String fieldLabel,
                                                   String beforeValue, String afterValue, Long operatorId,
                                                   String operatorName, LocalDateTime changeTime) {
        QmsQualityStandardChangeLogDO log = new QmsQualityStandardChangeLogDO();
        log.setStandardId(standardId);
        log.setApplyType(applyType);
        log.setChangeScope(changeScope);
        log.setItemKey(itemKey);
        log.setItemLabel(itemLabel);
        log.setFieldName(fieldName);
        log.setFieldLabel(fieldLabel);
        log.setBeforeValue(beforeValue);
        log.setAfterValue(afterValue);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setChangeTime(changeTime);
        return log;
    }

    private List<FieldConfig<QmsQualityStandardDO>> mainFieldConfigs() {
        return List.of(
                new FieldConfig<>("standardNo", "标准编号", QmsQualityStandardDO::getStandardNo),
                new FieldConfig<>("standardName", "标准名称", QmsQualityStandardDO::getStandardName),
                new FieldConfig<>("version", "版本号", QmsQualityStandardDO::getVersion),
                new FieldConfig<>("glueBoardModel", "胶板型号", QmsQualityStandardDO::getGlueBoardModel),
                new FieldConfig<>("materialCode", "物料编码", QmsQualityStandardDO::getMaterialCode),
                new FieldConfig<>("materialName", "物料名称", QmsQualityStandardDO::getMaterialName),
                new FieldConfig<>("specification", "规格型号", QmsQualityStandardDO::getSpecification),
                new FieldConfig<>("productModelCode", "产品型号编码", QmsQualityStandardDO::getProductModelCode),
                new FieldConfig<>("productModelName", "产品型号名称", QmsQualityStandardDO::getProductModelName),
                new FieldConfig<>("prodTypeName", "生产类型", QmsQualityStandardDO::getProdTypeName),
                new FieldConfig<>("processCode", "工序编码", QmsQualityStandardDO::getProcessCode),
                new FieldConfig<>("processName", "工序", QmsQualityStandardDO::getProcessName),
                new FieldConfig<>("status", "状态", QmsQualityStandardDO::getStatus),
                new FieldConfig<>("remark", "备注说明", QmsQualityStandardDO::getRemark));
    }

    private List<FieldConfig<QmsQualityStandardItemDO>> itemFieldConfigs() {
        return List.of(
                new FieldConfig<>("inspectionItem", "检验项目", QmsQualityStandardItemDO::getInspectionItem),
                new FieldConfig<>("processCode", "工序编码", QmsQualityStandardItemDO::getProcessCode),
                new FieldConfig<>("processName", "工序", QmsQualityStandardItemDO::getProcessName),
                new FieldConfig<>("sort", "排序", QmsQualityStandardItemDO::getSort),
                new FieldConfig<>("itemType", "项目类型", QmsQualityStandardItemDO::getItemType),
                new FieldConfig<>("expiryDays", "过期天数", QmsQualityStandardItemDO::getExpiryDays),
                new FieldConfig<>("attachmentEnabled", "允许附件", QmsQualityStandardItemDO::getAttachmentEnabled),
                new FieldConfig<>("actualValueRequired", "必填实际值", QmsQualityStandardItemDO::getActualValueRequired),
                new FieldConfig<>("standardDesc", "标准要求", QmsQualityStandardItemDO::getStandardDesc),
                new FieldConfig<>("targetValue", "目标值", QmsQualityStandardItemDO::getTargetValue),
                new FieldConfig<>("minValue", "下限值", item ->
                        formatDecimalWithScale(item.getMinValue(), item.getMinValueScale())),
                new FieldConfig<>("maxValue", "上限值", item ->
                        formatDecimalWithScale(item.getMaxValue(), item.getMaxValueScale())),
                new FieldConfig<>("unit", "单位", QmsQualityStandardItemDO::getUnit),
                new FieldConfig<>("inspectionMethod", "检验方法", QmsQualityStandardItemDO::getInspectionMethod),
                new FieldConfig<>("testFrequencyJudgement", "位置/录入规则", QmsQualityStandardItemDO::getTestFrequencyJudgement),
                new FieldConfig<>("ruleDescription", "规则说明", QmsQualityStandardItemDO::getRuleDescription),
                new FieldConfig<>("entryRuleTemplateName", "录入规则模板", QmsQualityStandardItemDO::getEntryRuleTemplateName),
                new FieldConfig<>("valueTemplate", "录入模板", QmsQualityStandardItemDO::getValueTemplate),
                new FieldConfig<>("judgmentMetric", "判定指标", QmsQualityStandardItemDO::getJudgmentMetric),
                new FieldConfig<>("templateParams", "模板参数", QmsQualityStandardItemDO::getTemplateParams),
                new FieldConfig<>("avgMinLimit", "平均值下限", item ->
                        formatDecimalWithScale(item.getAvgMinLimit(), item.getAvgMinLimitScale())),
                new FieldConfig<>("avgMaxLimit", "平均值上限", item ->
                        formatDecimalWithScale(item.getAvgMaxLimit(), item.getAvgMaxLimitScale())),
                new FieldConfig<>("stdMinLimit", "标准差下限", item ->
                        formatDecimalWithScale(item.getStdMinLimit(), item.getStdMinLimitScale())),
                new FieldConfig<>("stdMaxLimit", "标准差上限", item ->
                        formatDecimalWithScale(item.getStdMaxLimit(), item.getStdMaxLimitScale())),
                new FieldConfig<>("sheetTemplateId", "FAI表格模板", QmsQualityStandardItemDO::getSheetTemplateId),
                new FieldConfig<>("sheetSectionCode", "表格区块", QmsQualityStandardItemDO::getSheetSectionCode),
                new FieldConfig<>("sheetMetricCode", "模板指标", QmsQualityStandardItemDO::getSheetMetricCode),
                new FieldConfig<>("sheetFieldCode", "判定字段", QmsQualityStandardItemDO::getSheetFieldCode),
                new FieldConfig<>("testTool", "检验仪器", QmsQualityStandardItemDO::getTestTool),
                new FieldConfig<>("sampleSize", "检测数", QmsQualityStandardItemDO::getSampleSize),
                new FieldConfig<>("isSpc", "SPC管控", QmsQualityStandardItemDO::getIsSpc));
    }

    private Map<String, Queue<QmsQualityStandardItemDO>> indexItems(List<QmsQualityStandardItemDO> items,
                                                                    Function<QmsQualityStandardItemDO, String> keyFunction) {
        Map<String, Queue<QmsQualityStandardItemDO>> index = new LinkedHashMap<>();
        for (QmsQualityStandardItemDO item : items) {
            index.computeIfAbsent(keyFunction.apply(item), key -> new ArrayDeque<>()).add(item);
        }
        return index;
    }

    private QmsQualityStandardItemDO pollUnmatched(Queue<QmsQualityStandardItemDO> queue, Set<Long> matchedOldIds) {
        if (queue == null) {
            return null;
        }
        while (!queue.isEmpty()) {
            QmsQualityStandardItemDO item = queue.poll();
            if (item.getId() == null || !matchedOldIds.contains(item.getId())) {
                return item;
            }
        }
        return null;
    }

    private String buildItemPrimaryKey(QmsQualityStandardItemDO item) {
        return String.join("|",
                stableSegment(item.getInspectionItem()),
                stableSegment(item.getProcessCode()),
                stableSegment(item.getProcessName()),
                stableSegment(item.getSort()));
    }

    private String buildItemFallbackKey(QmsQualityStandardItemDO item) {
        return String.join("|",
                stableSegment(item.getInspectionItem()),
                stableSegment(item.getProcessCode()),
                stableSegment(item.getProcessName()));
    }

    private String buildItemLabel(QmsQualityStandardItemDO item) {
        String process = StringUtils.hasText(item.getProcessName())
                ? item.getProcessName()
                : (StringUtils.hasText(item.getProcessCode()) ? item.getProcessCode() : "-");
        return "检验项目：" + displayText(item.getInspectionItem())
                + " / 工序：" + process
                + " / 排序：" + displayText(item.getSort());
    }

    private String buildItemSummary(QmsQualityStandardItemDO item) {
        return "检验项目=" + displayText(item.getInspectionItem())
                + "；工序=" + displayText(item.getProcessName())
                + "；标准要求=" + displayText(item.getStandardDesc())
                + "；下限=" + displayDecimalText(item.getMinValue(), item.getMinValueScale())
                + "；上限=" + displayDecimalText(item.getMaxValue(), item.getMaxValueScale())
                + "；平均值内控=" + displayDecimalText(item.getAvgMinLimit(), item.getAvgMinLimitScale())
                + "~" + displayDecimalText(item.getAvgMaxLimit(), item.getAvgMaxLimitScale())
                + "；标准差内控=" + displayDecimalText(item.getStdMinLimit(), item.getStdMinLimitScale())
                + "~" + displayDecimalText(item.getStdMaxLimit(), item.getStdMaxLimitScale())
                + "；检测数=" + displayText(item.getSampleSize())
                + "；过期天数=" + displayText(item.getExpiryDays())
                + "；单位=" + displayText(item.getUnit())
                + "；检验方法=" + displayText(item.getInspectionMethod())
                + "；允许附件=" + formatValue("attachmentEnabled", item.getAttachmentEnabled())
                + "；必填实际值=" + formatValue("actualValueRequired", item.getActualValueRequired())
                + "；SPC管控=" + formatValue("isSpc", item.getIsSpc());
    }

    private Object normalizeCompareValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof String text) {
            return text.trim();
        }
        return value;
    }

    private String formatValue(String fieldName, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (("isSpc".equals(fieldName) || "attachmentEnabled".equals(fieldName)
                || "actualValueRequired".equals(fieldName)) && value instanceof Boolean bool) {
            return bool ? "是" : "否";
        }
        if ("status".equals(fieldName) && value instanceof Integer status) {
            return Objects.equals(status, 1) ? "启用" : "停用";
        }
        if ("itemType".equals(fieldName) && value instanceof String itemType) {
            return "QUANTITATIVE".equals(itemType) ? "定量"
                    : ("QUALITATIVE".equals(itemType) ? "定性" : ("DATE".equals(itemType) ? "时间" : itemType));
        }
        if ("templateParams".equals(fieldName) && value instanceof String text) {
            return formatTemplateParamsValue(text);
        }
        return String.valueOf(value);
    }

    private String formatTemplateParamsValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        Map<String, Object> params = JsonUtils.parseObjectQuietly(value, TEMPLATE_PARAMS_TYPE);
        if (params == null || params.isEmpty()) {
            return value;
        }

        Map<String, Object> dataRule = asMutableMap(params.get("dataRule"));
        if (dataRule.isEmpty()) {
            dataRule = params;
        }
        List<String> lines = new ArrayList<>();
        String valueTemplate = displayValueTemplate(params.get("valueTemplate"));
        if (StringUtils.hasText(valueTemplate)) {
            lines.add("录入模板：" + valueTemplate);
        }

        List<?> positions = asList(params.get("positions"));
        Object repeatCount = params.get("repeatCount");
        Object sampleSize = params.get("sampleSize");
        if (!positions.isEmpty() || repeatCount != null || sampleSize != null) {
            List<String> layoutParts = new ArrayList<>();
            if (!positions.isEmpty() && repeatCount != null) {
                layoutParts.add(positions.size() + "点*" + repeatCount + "组");
            }
            if (sampleSize != null) {
                layoutParts.add("共" + sampleSize + "个样本");
            }
            if (!layoutParts.isEmpty()) {
                lines.add("检测布局：" + String.join("，", layoutParts));
            }
        }
        String positionNames = formatPositionNames(positions);
        if (StringUtils.hasText(positionNames)) {
            lines.add("检测点位：" + positionNames);
        }

        List<?> inputFields = asList(dataRule.get("inputFields"));
        String inputText = formatRuleFields(inputFields);
        if (StringUtils.hasText(inputText)) {
            lines.add("录入字段：" + inputText);
        }
        List<?> resultFields = asList(dataRule.get("resultFields"));
        String resultText = formatRuleFields(resultFields);
        if (StringUtils.hasText(resultText)) {
            lines.add("结果字段：" + resultText);
        }
        Object judgmentMetric = dataRule.getOrDefault("judgmentMetric", params.get("judgmentMetric"));
        String judgmentMetricText = formatJudgmentMetric(judgmentMetric, resultFields);
        if (StringUtils.hasText(judgmentMetricText)) {
            lines.add("判定指标：" + judgmentMetricText);
        }
        return lines.isEmpty() ? value : String.join("\n", lines);
    }

    private List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private String displayValueTemplate(Object valueTemplate) {
        if (valueTemplate == null) {
            return "";
        }
        return switch (String.valueOf(valueTemplate)) {
            case "COMPRESSION_CALC" -> "压缩性能计算模板";
            case "DENSITY_CALC" -> "密度计算模板";
            case "SINGLE_VALUE" -> "单值实测模板";
            default -> String.valueOf(valueTemplate);
        };
    }

    private String formatPositionNames(List<?> positions) {
        List<String> names = new ArrayList<>();
        for (Object position : positions) {
            Map<String, Object> positionMap = asMutableMap(position);
            Object name = positionMap.getOrDefault("name", positionMap.get("code"));
            if (name != null && StringUtils.hasText(String.valueOf(name))) {
                names.add(String.valueOf(name));
            }
        }
        return String.join(" / ", names);
    }

    private String formatRuleFields(List<?> fields) {
        List<String> names = new ArrayList<>();
        for (Object field : fields) {
            String fieldText = formatRuleField(field);
            if (StringUtils.hasText(fieldText)) {
                names.add(fieldText);
            }
        }
        return String.join("、", names);
    }

    private String formatRuleField(Object field) {
        Map<String, Object> fieldMap = asMutableMap(field);
        if (fieldMap.isEmpty()) {
            return "";
        }
        Object label = fieldMap.getOrDefault("name", fieldMap.get("code"));
        if (label == null || !StringUtils.hasText(String.valueOf(label))) {
            return "";
        }
        Object unit = fieldMap.get("unit");
        return unit != null && StringUtils.hasText(String.valueOf(unit))
                ? label + "(" + unit + ")"
                : String.valueOf(label);
    }

    private String formatJudgmentMetric(Object metric, List<?> resultFields) {
        if (metric == null || !StringUtils.hasText(String.valueOf(metric))) {
            return "";
        }
        String metricText = String.valueOf(metric);
        String metricCode = normalizeRuleCode(metricText);
        for (Object field : resultFields) {
            Map<String, Object> fieldMap = asMutableMap(field);
            Object code = fieldMap.get("code");
            Object name = fieldMap.get("name");
            if (code != null && normalizeRuleCode(String.valueOf(code)).equals(metricCode)
                    && name != null && StringUtils.hasText(String.valueOf(name))) {
                return name + "(" + metricText + ")";
            }
        }
        return metricText;
    }

    private String normalizeRuleCode(String value) {
        return value == null ? "" : value.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String formatDecimalWithScale(BigDecimal value, Integer scale) {
        if (value == null) {
            return "";
        }
        if (scale == null || scale < 0) {
            return value.stripTrailingZeros().toPlainString();
        }
        int displayScale = Math.min(scale, 30);
        return value.setScale(displayScale, RoundingMode.HALF_UP).toPlainString();
    }

    private String displayDecimalText(BigDecimal value, Integer scale) {
        String text = formatDecimalWithScale(value, scale);
        return StringUtils.hasText(text) ? text : "-";
    }

    private String stableSegment(Object value) {
        return String.valueOf(normalizeCompareValue(value));
    }

    private String displayText(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return "-";
        }
        return String.valueOf(value);
    }

    private record FieldConfig<T>(String fieldName, String fieldLabel, Function<T, Object> getter) {
    }

    private QmsQualityStandardDO validateQualityStandardExists(Long id) {
        QmsQualityStandardDO entity = qmsQualityStandardMapper.selectById(id);
        if (entity == null) {
            throw exception(HCQUALITYSTANDARD_NOT_EXISTS);
        }
        return entity;
    }

    private String normalizeApplyType(String applyType) {
        if (!StringUtils.hasText(applyType)) {
            throw exception(HCQUALITYSTANDARD_APPLY_TYPE_INVALID);
        }
        String normalized = applyType.trim().toUpperCase().replace('-', '_');
        if (!SUPPORT_APPLY_TYPES.contains(normalized)) {
            throw exception(HCQUALITYSTANDARD_APPLY_TYPE_INVALID);
        }
        return normalized;
    }

    private void validateSameApplyType(QmsQualityStandardDO entity, String fixedApplyType) {
        String applyType = normalizeApplyType(fixedApplyType);
        if (!applyType.equals(normalizeApplyType(entity.getApplyType()))) {
            throw exception(HCQUALITYSTANDARD_APPLY_TYPE_MISMATCH);
        }
    }

    private void validateQualityStandardNotReferenced(Long id) {
        Long count = qmsQualityStandardMapper.selectExecutionReferenceCount(id);
        if (count != null && count > 0) {
            throw exception(HCQUALITYSTANDARD_REFERENCED);
        }
    }

    private void validateBusinessUnique(Long id, QmsQualityStandardSaveReqVO reqVO) {
        if (isGlueBoardFai(reqVO)) {
            QmsQualityStandardDO entity = qmsQualityStandardMapper.selectByGlueBoardBusinessKey(
                    reqVO.getGlueBoardModel(), reqVO.getStandardName(), reqVO.getVersion(), reqVO.getApplyType(), id);
            if (entity != null) {
                throw exception(HCQUALITYSTANDARD_BIZ_EXISTS);
            }
            return;
        }
        QmsQualityStandardDO entity = qmsQualityStandardMapper.selectByBusinessKey(
                reqVO.getMaterialId(), reqVO.getStandardName(), reqVO.getVersion(), reqVO.getApplyType(),
                reqVO.getProcessId(), reqVO.getProductModelId(), id);
        if (entity != null) {
            throw exception(HCQUALITYSTANDARD_BIZ_EXISTS);
        }
    }

    private void validateStandardNoUnique(Long id, String standardNo) {
        Long count = qmsQualityStandardMapper.selectCountByStandardNo(standardNo.trim(), id);
        if (count != null && count > 0) {
            throw exception(HCQUALITYSTANDARD_NO_EXISTS);
        }
    }

    private void validateMaterial(QmsQualityStandardSaveReqVO reqVO) {
        if (!hasAnyMaterialValue(reqVO)) {
            return;
        }
        HcMaterialDO material = hcMaterialService.getHcMaterial(reqVO.getMaterialId());
        if (material == null) {
            throw exception(HCMATERIAL_NOT_EXISTS);
        }
    }

    private void validateProductModel(QmsQualityStandardSaveReqVO reqVO) {
        if (!hasAnyProductModelValue(reqVO)) {
            return;
        }
        if (!hasCompleteProductModelValue(reqVO)) {
            throw exception(HCQUALITYSTANDARD_PRODUCT_MODEL_REQUIRED);
        }
        HcProductModelDO productModel = hcProductModelService.getHcProductModel(reqVO.getProductModelId());
        if (productModel == null) {
            throw exception(HCPRODUCTMODEL_NOT_EXISTS);
        }
        if (!StringUtils.hasText(productModel.getModelCode()) || !StringUtils.hasText(productModel.getModelName())) {
            throw exception(HCQUALITYSTANDARD_PRODUCT_MODEL_REQUIRED);
        }
        reqVO.setProductModelCode(productModel.getModelCode());
        reqVO.setProductModelName(productModel.getModelName());
        reqVO.setProdType(productModel.getProdType());
        reqVO.setProdTypeName(productModel.getProdTypeName());
        if (MODEL_LEVEL_FAMILY.equals(productModel.getModelLevel())) {
            reqVO.setMaterialId(null);
            reqVO.setMaterialCode(null);
            reqVO.setMaterialName(null);
            reqVO.setSpecification(null);
        }
    }

    private void validateApplyScope(QmsQualityStandardSaveReqVO reqVO) {
        if (isGlueBoardFai(reqVO)) {
            return;
        }
        boolean hasAnyMaterialValue = hasAnyMaterialValue(reqVO);
        if (hasAnyMaterialValue && !hasCompleteMaterialValue(reqVO)) {
            throw exception(HCQUALITYSTANDARD_MATERIAL_REQUIRED);
        }
    }

    private void validateProcessScope(QmsQualityStandardSaveReqVO reqVO) {
        if (isGlueBoardFai(reqVO)) {
            return;
        }
        if (hasAnyProcessValue(reqVO) && !hasCompleteProcessValue(reqVO)) {
            throw exception(HCQUALITYSTANDARD_PROCESS_REQUIRED);
        }
    }

    private boolean hasAnyMaterialValue(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO.getMaterialId() != null
                || StringUtils.hasText(reqVO.getMaterialCode())
                || StringUtils.hasText(reqVO.getMaterialName());
    }

    private boolean hasCompleteMaterialValue(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO.getMaterialId() != null
                && StringUtils.hasText(reqVO.getMaterialCode())
                && StringUtils.hasText(reqVO.getMaterialName());
    }

    private boolean hasAnyProductModelValue(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO.getProductModelId() != null
                || StringUtils.hasText(reqVO.getProductModelCode())
                || StringUtils.hasText(reqVO.getProductModelName())
                || StringUtils.hasText(reqVO.getProdType())
                || StringUtils.hasText(reqVO.getProdTypeName());
    }

    private boolean hasCompleteProductModelValue(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO.getProductModelId() != null
                && StringUtils.hasText(reqVO.getProductModelCode())
                && StringUtils.hasText(reqVO.getProductModelName());
    }

    private boolean hasAnyProcessValue(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO.getProcessId() != null
                || StringUtils.hasText(reqVO.getProcessCode())
                || StringUtils.hasText(reqVO.getProcessName());
    }

    private boolean hasCompleteProcessValue(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO.getProcessId() != null
                && StringUtils.hasText(reqVO.getProcessCode())
                && StringUtils.hasText(reqVO.getProcessName());
    }

    private boolean isGlueBoardFai(QmsQualityStandardSaveReqVO reqVO) {
        return reqVO != null && APPLY_TYPE_GLUE_BOARD_FAI.equals(reqVO.getApplyType());
    }

    private void normalizeGlueBoardStandard(QmsQualityStandardSaveReqVO reqVO) {
        if (!isGlueBoardFai(reqVO)) {
            return;
        }
        reqVO.setGlueBoardModel(StringUtils.trimWhitespace(reqVO.getGlueBoardModel()));
        if (!StringUtils.hasText(reqVO.getGlueBoardModel())) {
            throw exception(HCQUALITYSTANDARD_GLUE_BOARD_MODEL_REQUIRED);
        }
        reqVO.setMaterialId(null);
        reqVO.setMaterialCode(null);
        reqVO.setMaterialName(null);
        reqVO.setSpecification(null);
        reqVO.setProductModelId(null);
        reqVO.setProductModelCode(null);
        reqVO.setProductModelName(null);
        reqVO.setProdType(null);
        reqVO.setProdTypeName(null);
        reqVO.setProcessId(null);
        reqVO.setProcessCode(null);
        reqVO.setProcessName(null);
    }
}
