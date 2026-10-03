package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckPhotoReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsWetPoreSelfCheckRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class QmsWetPoreSelfCheckServiceImpl implements QmsWetPoreSelfCheckService {

    private static final String WET_PORE_SELF_CHECK_FORM_CODE = "WET_PORE_SELF_CHECK";
    private static final String IMAGE_STATUS_UPLOADED = "UPLOADED";
    private static final String IMAGE_STATUS_MISSING = "MISSING";
    private static final int MAX_PORE_PHOTO_COUNT = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HcStationRecordMapper hcStationRecordMapper;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;

    @Override
    public PageResult<QmsWetPoreSelfCheckRespVO> getPage(QmsWetPoreSelfCheckPageReqVO reqVO) {
        List<HcStationRecordDO> stationRecords = hcStationRecordMapper.selectList(
                new LambdaQueryWrapperX<HcStationRecordDO>()
                        .eq(HcStationRecordDO::getFormCode, WET_PORE_SELF_CHECK_FORM_CODE)
                        .eq(HcStationRecordDO::getDeleted, false)
                        .likeIfPresent(HcStationRecordDO::getPlanNo, reqVO.getPlanNo())
                        .isNotNull(HcStationRecordDO::getHeaderDataJson)
                        .ne(HcStationRecordDO::getHeaderDataJson, "")
                        .orderByDesc(HcStationRecordDO::getRecordTime)
                        .orderByDesc(HcStationRecordDO::getId));
        if (stationRecords.isEmpty()) {
            return new PageResult<>(List.of(), 0L);
        }

        Map<Long, HcPlanOrderDO> planMap = selectPlanMap(stationRecords);
        Map<Long, HcPlanOrderOperationDO> operationMap = selectOperationMap(stationRecords);
        List<QmsWetPoreSelfCheckRespVO> rows = stationRecords.stream()
                .flatMap(record -> buildRows(record, planMap.get(record.getPlanId()), operationMap.get(record.getPlanOperationId())).stream())
                .filter(row -> matches(row, reqVO))
                .sorted(rowComparator())
                .collect(Collectors.toList());
        return page(rows, reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsWetPoreSelfCheckRespVO updatePhoto(QmsWetPoreSelfCheckPhotoReqVO reqVO) {
        HcStationRecordDO stationRecord = hcStationRecordMapper.selectById(reqVO.getStationRecordId());
        if (stationRecord == null || !WET_PORE_SELF_CHECK_FORM_CODE.equals(stationRecord.getFormCode())) {
            throw invalidParamException("泡孔自检记录不存在");
        }

        ObjectNode root = parseHeaderObject(stationRecord.getHeaderDataJson());
        JsonNode recordsNode = root.get("records");
        if (!(recordsNode instanceof ArrayNode records)) {
            throw invalidParamException("泡孔自检记录数据不完整");
        }
        if (reqVO.getRecordIndex() >= records.size()) {
            throw invalidParamException("泡孔自检记录序号不存在");
        }
        JsonNode targetNode = records.get(reqVO.getRecordIndex());
        if (!(targetNode instanceof ObjectNode target)) {
            throw invalidParamException("泡孔自检记录数据格式不正确");
        }
        String savedClientKey = text(target, "clientKey");
        if (StringUtils.hasText(reqVO.getClientKey()) && StringUtils.hasText(savedClientKey)
                && !Objects.equals(savedClientKey, reqVO.getClientKey())) {
            throw invalidParamException("泡孔自检记录已变化，请刷新后重试");
        }

        List<String> photos = normalizePhotoUrls(reqVO.getPhotos(), reqVO.getPhoto());
        ArrayNode photoNodes = target.putArray("photos");
        photos.forEach(photoNodes::add);
        // 新数据统一保存为 photos；保留读取 photo 的兼容能力以支持历史记录。
        target.remove("photo");
        HcStationRecordDO updateObj = new HcStationRecordDO();
        updateObj.setId(stationRecord.getId());
        updateObj.setHeaderDataJson(JsonUtils.toJsonString(root));
        hcStationRecordMapper.updateById(updateObj);

        HcStationRecordDO updatedRecord = hcStationRecordMapper.selectById(stationRecord.getId());
        HcPlanOrderDO plan = updatedRecord.getPlanId() == null ? null : hcPlanOrderMapper.selectById(updatedRecord.getPlanId());
        HcPlanOrderOperationDO operation = updatedRecord.getPlanOperationId() == null
                ? null : hcPlanOrderOperationMapper.selectById(updatedRecord.getPlanOperationId());
        return buildRows(updatedRecord, plan, operation).stream()
                .filter(row -> Objects.equals(row.getRecordIndex(), reqVO.getRecordIndex()))
                .findFirst()
                .orElseThrow(() -> invalidParamException("泡孔自检记录不存在"));
    }

    private Map<Long, HcPlanOrderDO> selectPlanMap(List<HcStationRecordDO> stationRecords) {
        Set<Long> planIds = stationRecords.stream()
                .map(HcStationRecordDO::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (planIds.isEmpty()) {
            return new HashMap<>();
        }
        return hcPlanOrderMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(HcPlanOrderDO::getId, item -> item));
    }

    private Map<Long, HcPlanOrderOperationDO> selectOperationMap(List<HcStationRecordDO> stationRecords) {
        Set<Long> operationIds = stationRecords.stream()
                .map(HcStationRecordDO::getPlanOperationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (operationIds.isEmpty()) {
            return new HashMap<>();
        }
        return hcPlanOrderOperationMapper.selectBatchIds(operationIds).stream()
                .collect(Collectors.toMap(HcPlanOrderOperationDO::getId, item -> item));
    }

    private List<QmsWetPoreSelfCheckRespVO> buildRows(HcStationRecordDO stationRecord,
                                                       HcPlanOrderDO plan,
                                                       HcPlanOrderOperationDO operation) {
        JsonNode root = parseHeaderQuietly(stationRecord.getHeaderDataJson());
        JsonNode records = root == null ? null : root.get("records");
        if (records == null || !records.isArray() || records.isEmpty()) {
            return List.of();
        }
        List<QmsWetPoreSelfCheckRespVO> rows = new java.util.ArrayList<>();
        for (int index = 0; index < records.size(); index++) {
            JsonNode recordNode = records.get(index);
            if (!recordNode.isObject() || isEmptySelfCheckRecord(recordNode)) {
                continue;
            }
            rows.add(buildRow(stationRecord, plan, operation, recordNode, index));
        }
        return rows;
    }

    private QmsWetPoreSelfCheckRespVO buildRow(HcStationRecordDO stationRecord,
                                                HcPlanOrderDO plan,
                                                HcPlanOrderOperationDO operation,
                                                JsonNode recordNode,
                                                int recordIndex) {
        QmsWetPoreSelfCheckRespVO respVO = new QmsWetPoreSelfCheckRespVO();
        respVO.setId(stationRecord.getId() + "-" + recordIndex);
        respVO.setStationRecordId(stationRecord.getId());
        respVO.setRecordIndex(recordIndex);
        respVO.setClientKey(text(recordNode, "clientKey"));
        respVO.setPlanId(stationRecord.getPlanId());
        respVO.setPlanNo(firstNotBlank(stationRecord.getPlanNo(), plan == null ? null : plan.getPlanNo()));
        respVO.setPlanOperationId(stationRecord.getPlanOperationId());
        respVO.setOperationName(firstNotBlank(stationRecord.getOperationName(), operation == null ? null : operation.getOpName()));
        respVO.setProductModel(firstNotBlank(
                operation == null ? null : operation.getMotherModelCode(),
                plan == null ? null : plan.getMotherModelCode(),
                operation == null ? null : operation.getMotherModelName(),
                plan == null ? null : plan.getMotherModelName()));
        respVO.setProductMaterialCode(firstNotBlank(
                operation == null ? null : operation.getMotherMaterialCode(),
                plan == null ? null : plan.getMotherMaterialCode()));
        respVO.setMotherBatchNo(firstNotBlank(
                operation == null ? null : operation.getParentBatchNo(),
                operation == null ? null : operation.getParentProductionBatchNo(),
                plan == null ? null : plan.getParentProductionBatchNo(),
                operation == null ? null : operation.getProductionBatchNo(),
                plan == null ? null : plan.getProductionBatchNo(),
                operation == null ? null : operation.getBatchNo(),
                plan == null ? null : plan.getBatchNo()));
        respVO.setSelfCheckTime(parseSelfCheckTime(text(recordNode, "selfCheckTime")));
        respVO.setSelfCheckResult(text(recordNode, "selfCheckResult"));
        respVO.setInspector(text(recordNode, "inspector"));
        respVO.setRemark(text(recordNode, "remark"));
        List<String> photos = readPhotoUrls(recordNode);
        respVO.setPhotos(photos);
        respVO.setPhoto(photos.isEmpty() ? null : photos.get(0));
        respVO.setImageStatus(photos.isEmpty() ? IMAGE_STATUS_MISSING : IMAGE_STATUS_UPLOADED);
        respVO.setRecordStatus(firstNotBlank(stationRecord.getDocStatus(), stationRecord.getResultStatus()));
        respVO.setRecordUserName(stationRecord.getRecordUserName());
        respVO.setRecordTime(stationRecord.getRecordTime());
        respVO.setCreateTime(stationRecord.getCreateTime());
        return respVO;
    }

    private boolean matches(QmsWetPoreSelfCheckRespVO row, QmsWetPoreSelfCheckPageReqVO reqVO) {
        return contains(row.getPlanNo(), reqVO.getPlanNo())
                && contains(row.getProductModel(), reqVO.getProductModel())
                && contains(row.getMotherBatchNo(), reqVO.getMotherBatchNo())
                && contains(row.getProductMaterialCode(), reqVO.getProductMaterialCode())
                && matchesExact(row.getSelfCheckResult(), reqVO.getSelfCheckResult())
                && matchesExact(row.getImageStatus(), reqVO.getImageStatus())
                && matchesTimeRange(row.getSelfCheckTime(), reqVO.getSelfCheckTime())
                && matchesKeyword(row, reqVO.getKeyword());
    }

    private boolean matchesKeyword(QmsWetPoreSelfCheckRespVO row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(row.getPlanNo(), keyword)
                || contains(row.getProductModel(), keyword)
                || contains(row.getMotherBatchNo(), keyword)
                || contains(row.getProductMaterialCode(), keyword)
                || contains(row.getSelfCheckResult(), keyword)
                || contains(row.getInspector(), keyword)
                || contains(row.getRemark(), keyword)
                || contains(row.getRecordUserName(), keyword);
    }

    private boolean matchesExact(String value, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return Objects.equals(firstNotBlank(value), expected.trim());
    }

    private boolean matchesTimeRange(LocalDateTime value, LocalDateTime[] range) {
        if (range == null || range.length == 0 || (range[0] == null && (range.length < 2 || range[1] == null))) {
            return true;
        }
        if (value == null) {
            return false;
        }
        if (range[0] != null && value.isBefore(range[0])) {
            return false;
        }
        return range.length < 2 || range[1] == null || !value.isAfter(range[1]);
    }

    private boolean contains(String value, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private Comparator<QmsWetPoreSelfCheckRespVO> rowComparator() {
        return Comparator.comparing(QmsWetPoreSelfCheckRespVO::getSelfCheckTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(QmsWetPoreSelfCheckRespVO::getStationRecordId,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(QmsWetPoreSelfCheckRespVO::getRecordIndex,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private PageResult<QmsWetPoreSelfCheckRespVO> page(List<QmsWetPoreSelfCheckRespVO> rows,
                                                        QmsWetPoreSelfCheckPageReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        long total = rows.size();
        long from = (long) (pageNo - 1) * pageSize;
        if (from >= total) {
            return new PageResult<>(List.of(), total);
        }
        int fromIndex = (int) from;
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), total);
    }

    private JsonNode parseHeaderQuietly(String headerDataJson) {
        if (!StringUtils.hasText(headerDataJson)) {
            return null;
        }
        try {
            return JsonUtils.parseTree(headerDataJson);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ObjectNode parseHeaderObject(String headerDataJson) {
        if (!StringUtils.hasText(headerDataJson)) {
            throw invalidParamException("泡孔自检记录数据为空");
        }
        JsonNode root = JsonUtils.parseTree(headerDataJson);
        if (root == null || !root.isObject()) {
            throw invalidParamException("泡孔自检记录数据格式不正确");
        }
        return (ObjectNode) root;
    }

    private boolean isEmptySelfCheckRecord(JsonNode recordNode) {
        return !StringUtils.hasText(text(recordNode, "selfCheckTime"))
                && !StringUtils.hasText(text(recordNode, "selfCheckResult"))
                && !StringUtils.hasText(text(recordNode, "inspector"))
                && !StringUtils.hasText(text(recordNode, "remark"))
                && readPhotoUrls(recordNode).isEmpty();
    }

    private List<String> normalizePhotoUrls(List<String> requestedPhotos, String legacyPhoto) {
        LinkedHashSet<String> photos = new LinkedHashSet<>();
        if (requestedPhotos != null) {
            requestedPhotos.forEach(photo -> addPhotoUrl(photos, photo));
        }
        addPhotoUrl(photos, legacyPhoto);
        if (photos.size() > MAX_PORE_PHOTO_COUNT) {
            throw invalidParamException("每条泡孔自检记录最多上传 " + MAX_PORE_PHOTO_COUNT + " 张图片");
        }
        return new java.util.ArrayList<>(photos);
    }

    private List<String> readPhotoUrls(JsonNode recordNode) {
        LinkedHashSet<String> photos = new LinkedHashSet<>();
        JsonNode photosNode = recordNode == null ? null : recordNode.path("photos");
        if (photosNode != null && photosNode.isArray()) {
            photosNode.forEach(item -> addPhotoUrl(photos, item.asText()));
        } else if (photosNode != null && photosNode.isTextual()) {
            addPhotoUrl(photos, photosNode.asText());
        }
        addPhotoUrl(photos, text(recordNode, "photo"));
        return new java.util.ArrayList<>(photos);
    }

    private void addPhotoUrl(LinkedHashSet<String> photos, String photo) {
        if (StringUtils.hasText(photo)) {
            photos.add(photo.trim());
        }
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime parseSelfCheckTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        List<DateTimeFormatter> formatters = List.of(DATE_TIME_FORMATTER, MINUTE_FORMATTER, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next supported UI format.
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

}
