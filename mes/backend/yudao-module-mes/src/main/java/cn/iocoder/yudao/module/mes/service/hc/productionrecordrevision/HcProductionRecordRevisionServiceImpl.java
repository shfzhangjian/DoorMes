package cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionrecordrevision.vo.HcProductionRecordRevisionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionrecordrevision.HcProductionRecordRevisionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionrecordrevision.HcProductionRecordRevisionMapper;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 生产记录展示修订服务。
 *
 * <p>模块字段采用白名单，避免客户端通过修订接口更改数据来源、状态等非展示字段。</p>
 */
@Service
@Validated
public class HcProductionRecordRevisionServiceImpl implements HcProductionRecordRevisionService {

    public static final String MODULE_FORMULA = "FORMULA";
    public static final String MODULE_WET = "WET";
    public static final String MODULE_ADHESIVE1 = "ADHESIVE1";
    public static final String MODULE_ADHESIVE2 = "ADHESIVE2";
    public static final String MODULE_CUT_ROUND = "CUT_ROUND";
    public static final String MODULE_SLITTING_PRESS = "SLITTING_PRESS";

    private static final Map<String, Set<String>> REVISABLE_FIELDS = Map.ofEntries(
            Map.entry(MODULE_FORMULA, Set.of("reportDate", "modelCode", "materialCode", "batchNo", "filterBatchNo",
                    "inputWeight", "outputWeight", "mixerEquipmentCode", "batchingTankNo", "foamingEquipmentCode",
                    "defoamingTankNo", "recorderName", "recordTime", "remark")),
            Map.entry(MODULE_WET, Set.of("recordDate", "modelCode", "materialCode", "batchNo", "inputKg",
                    "outputMeter", "petModel", "petBatchNo", "guideClothBatchNo", "guideClothUseCount",
                    "guideClothChanged", "changeDesc", "recorderName", "recordTime", "remark")),
            Map.entry(MODULE_ADHESIVE1, Set.of("reportDate", "modelCode", "materialCode", "batchNo", "inputQty",
                    "outputQty", "glueBoardMaterialCode", "glueBoardBatchNo", "glueBoardConsumeQty",
                    "recorderName", "recordTime", "remark")),
            Map.entry(MODULE_ADHESIVE2, Set.of("reportDate", "modelCode", "materialCode", "batchNo", "inputQty",
                    "outputQty", "glueBoardMaterialCode", "glueBoardBatchNo", "glueBoardConsumeQty",
                    "recorderName", "recordTime", "remark")),
            Map.entry(MODULE_CUT_ROUND, Set.of("reportDate", "modelCode", "productionBatchNo", "cutSizeMm",
                    "inputQty", "outputQty", "bladeModel", "bladeBatchNo", "bladeUseCount", "feltModel",
                    "feltBatchNo", "feltUseCount", "feltUseDays", "bladeReplaceReason", "recorderName",
                    "recordTime", "remark")),
            Map.entry(MODULE_SLITTING_PRESS, Set.of("reportDate", "modelCode", "materialCode", "batchNo",
                    "slittingInputM", "slittingOutputPcs", "slittingNgPcs", "pressSlotActualInputPcs",
                    "pressSlotActualOutputPcs", "pressSlotOutputPcs", "rollerCleanAccumulatedPcs",
                    "rollerCleanUseDays", "bearingReplaceAccumulatedPcs", "bearingReplaceUseDays", "recorderName",
                    "recordTime", "remark")));

    @Resource
    private HcProductionRecordRevisionMapper revisionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRevision(HcProductionRecordRevisionCreateReqVO reqVO) {
        String moduleCode = normalizeModuleCode(reqVO.getModuleCode());
        Set<String> revisableFields = REVISABLE_FIELDS.get(moduleCode);
        validateRevisedFields(reqVO.getRevisedData(), revisableFields);
        parseRecordTime(reqVO.getRevisedData());
        String originalSnapshotJson = JsonUtils.toJsonString(reqVO.getOriginalSnapshot());
        String revisedDataJson = JsonUtils.toJsonString(reqVO.getRevisedData());
        if (originalSnapshotJson.length() > 60000 || revisedDataJson.length() > 60000) {
            throw invalidParamException("生产记录展示修订内容过长");
        }
        String recordKey = buildRecordKey(moduleCode, reqVO.getRecordId());
        Integer latestNo = revisionMapper.selectMaxRevisionNo(moduleCode, recordKey);
        HcProductionRecordRevisionDO entity = new HcProductionRecordRevisionDO();
        entity.setModuleCode(moduleCode);
        entity.setRecordKey(recordKey);
        entity.setOriginalSnapshotJson(originalSnapshotJson);
        entity.setRevisedDataJson(revisedDataJson);
        entity.setReviseReason(StrUtil.trim(reqVO.getReviseReason()));
        entity.setRevisionNo(latestNo == null ? 1 : latestNo + 1);
        entity.setReviseUserId(SecurityFrameworkUtils.getLoginUserId());
        entity.setReviseUserName(SecurityFrameworkUtils.getLoginUserNickname());
        entity.setRevisedAt(java.time.LocalDateTime.now());
        entity.setTenantId(TenantContextHolder.getTenantId());
        revisionMapper.insert(entity);
    }

    @Override
    public <T> List<T> applyRevisions(String moduleCode, List<T> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        String normalizedModuleCode = normalizeModuleCode(moduleCode);
        Map<String, T> rowMap = new LinkedHashMap<>();
        for (T row : rows) {
            Object id = BeanUtil.getFieldValue(row, "id");
            if (id != null) {
                rowMap.put(buildRecordKey(normalizedModuleCode, id), row);
            }
        }
        if (rowMap.isEmpty()) {
            return rows;
        }
        Map<String, HcProductionRecordRevisionDO> latestRevisionMap = new LinkedHashMap<>();
        for (HcProductionRecordRevisionDO revision : revisionMapper.selectLatestList(normalizedModuleCode, rowMap.keySet())) {
            latestRevisionMap.putIfAbsent(revision.getRecordKey(), revision);
        }
        if (latestRevisionMap.isEmpty()) {
            return rows;
        }
        Set<String> revisableFields = REVISABLE_FIELDS.get(normalizedModuleCode);
        for (Map.Entry<String, HcProductionRecordRevisionDO> entry : latestRevisionMap.entrySet()) {
            T target = rowMap.get(entry.getKey());
            if (target != null) {
                mergeRevisedFields(target, entry.getValue().getRevisedDataJson(), revisableFields);
            }
        }
        return rows;
    }

    private void validateRevisedFields(Map<String, Object> revisedData, Set<String> revisableFields) {
        if (revisedData == null || revisedData.isEmpty()) {
            throw invalidParamException("请至少修订一个展示字段");
        }
        Set<String> invalidFields = new LinkedHashSet<>(revisedData.keySet());
        invalidFields.removeAll(revisableFields);
        if (!invalidFields.isEmpty()) {
            throw invalidParamException("包含不允许修订的展示字段：{}", String.join("、", invalidFields));
        }
    }

    private <T> void mergeRevisedFields(T target, String revisedDataJson, Set<String> revisableFields) {
        Map<String, Object> revisedData = JsonUtils.parseObject(revisedDataJson,
                new TypeReference<Map<String, Object>>() {
                });
        if (revisedData == null || revisedData.isEmpty()) {
            return;
        }
        validateRevisedFields(revisedData, revisableFields);
        LocalDateTime recordTime = parseRecordTime(revisedData);
        // Response VO 的 JsonFormat 不会替换全局时间戳反序列化器，时间必须单独按 MES 契约解析。
        Map<String, Object> otherFields = new LinkedHashMap<>(revisedData);
        otherFields.remove("recordTime");
        Object revisedValue = JsonUtils.parseObject(JsonUtils.toJsonString(otherFields), target.getClass());
        for (String field : revisedData.keySet()) {
            try {
                // MES VO 的 Lombok 链式 setter 返回对象自身，JavaBeans Introspector 不会将其识别为 write method。
                // 使用 Hutool 按字段读写，并保留 revisedValue 已按目标类型完成的日期、数字转换。
                BeanUtil.setFieldValue(target, field, "recordTime".equals(field)
                        ? recordTime : BeanUtil.getFieldValue(revisedValue, field));
            } catch (RuntimeException e) {
                throw invalidParamException("展示修订字段不存在：{}", field);
            }
        }
    }

    private LocalDateTime parseRecordTime(Map<String, Object> revisedData) {
        if (!revisedData.containsKey("recordTime")) {
            return null;
        }
        RecordTimeValue value;
        try {
            Map<String, Object> timeField = new LinkedHashMap<>();
            timeField.put("recordTime", revisedData.get("recordTime"));
            value = JsonUtils.parseObject(JsonUtils.toJsonString(timeField), RecordTimeValue.class);
        } catch (RuntimeException e) {
            throw invalidParamException("实际完工时间无效，请填写有效的完整日期时间");
        }
        if (value.recordTime == null) {
            throw invalidParamException("实际完工时间无效，请填写有效的完整日期时间");
        }
        return value.recordTime;
    }

    private static class RecordTimeValue {
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        public LocalDateTime recordTime;
    }

    private String normalizeModuleCode(String moduleCode) {
        String value = StrUtil.trimToEmpty(moduleCode).toUpperCase(Locale.ROOT);
        if (!REVISABLE_FIELDS.containsKey(value)) {
            throw invalidParamException("不支持该生产记录模块：{}", value);
        }
        return value;
    }

    public static String buildRecordKey(String moduleCode, Object recordId) {
        return moduleCode + ":" + recordId;
    }

}
