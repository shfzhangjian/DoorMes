package cn.iocoder.yudao.module.mes.service.hc.stationrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingMiddleProductRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord.HcStationRecordItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingMiddleProductRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationrecord.HcStationRecordMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCSTATIONRECORD_NOT_EXISTS;

@Service
@Validated
public class HcStationRecordQueryServiceImpl implements HcStationRecordQueryService {

    private static final String FORM_MIDDLE_PRODUCT = "ROUGH_MIDDLE_PRODUCT_RECORD";
    private static final String BIZ_GRINDING_SECOND = "ROUGH_GRINDING_SECOND";

    @Resource
    private HcStationRecordMapper hcStationRecordMapper;

    @Resource
    private HcStationRecordItemMapper hcStationRecordItemMapper;

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;

    @Resource
    private HcGrindingMiddleProductRecordMapper hcGrindingMiddleProductRecordMapper;

    @Override
    public PageResult<HcStationRecordRespVO> getStationRecordPage(HcStationRecordPageReqVO reqVO) {
        PageResult<HcStationRecordDO> page = hcStationRecordMapper.selectPage(reqVO);
        Map<Long, HcPlanOrderDO> planMap = selectPlanMap(page.getList());
        return new PageResult<>(
                page.getList().stream().map(record -> toResp(record, planMap.get(record.getPlanId()))).toList(),
                page.getTotal());
    }

    @Override
    public HcStationRecordDetailRespVO getStationRecordDetail(Long id) {
        HcStationRecordDO record = validateStationRecordExists(id);
        HcStationRecordDetailRespVO respVO = BeanUtils.toBean(record, HcStationRecordDetailRespVO.class);
        fillCreateUserName(respVO, record);
        fillPlanInfo(respVO, record.getPlanId() == null ? null : hcPlanOrderMapper.selectById(record.getPlanId()));
        fillRecordHeaderInfo(respVO, record);
        List<HcStationRecordItemDO> items = hcStationRecordItemMapper.selectByRecordIds(List.of(id));
        respVO.setItems(BeanUtils.toBean(items, HcStationRecordItemRespVO.class));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStationRecord(HcStationRecordSaveReqVO reqVO) {
        HcStationRecordDO old = validateStationRecordExists(reqVO.getId());
        HcStationRecordDO updateObj = new HcStationRecordDO();
        updateObj.setId(reqVO.getId());
        updateObj.setDocStatus(reqVO.getDocStatus());
        updateObj.setResultStatus(reqVO.getResultStatus());
        updateObj.setInspectionResult(reqVO.getInspectionResult());
        updateObj.setRecordUserName(reqVO.getRecordUserName());
        updateObj.setRecordTime(reqVO.getRecordTime());
        updateObj.setConfirmUserName(reqVO.getConfirmUserName());
        updateObj.setConfirmTime(reqVO.getConfirmTime());
        updateObj.setHeaderDataJson(reqVO.getHeaderDataJson());
        updateObj.setFormRemark(reqVO.getFormRemark());
        updateObj.setConfirmRemark(reqVO.getConfirmRemark());
        hcStationRecordMapper.updateById(updateObj);

        if (reqVO.getItems() != null) {
            hcStationRecordItemMapper.deleteByRecordId(reqVO.getId());
            List<HcStationRecordItemDO> createList = toItemDOList(reqVO.getId(), old.getTenantId(), reqVO.getItems());
            if (!createList.isEmpty()) {
                hcStationRecordItemMapper.insertBatch(createList);
            }
        }
    }

    private HcStationRecordDO validateStationRecordExists(Long id) {
        HcStationRecordDO record = id == null ? null : hcStationRecordMapper.selectById(id);
        if (record == null) {
            throw exception(HCSTATIONRECORD_NOT_EXISTS);
        }
        return record;
    }

    private HcStationRecordRespVO toResp(HcStationRecordDO record, HcPlanOrderDO planOrder) {
        HcStationRecordRespVO respVO = BeanUtils.toBean(record, HcStationRecordRespVO.class);
        fillCreateUserName(respVO, record);
        fillPlanInfo(respVO, planOrder);
        fillRecordHeaderInfo(respVO, record);
        return respVO;
    }

    private Map<Long, HcPlanOrderDO> selectPlanMap(List<HcStationRecordDO> records) {
        Set<Long> planIds = records.stream()
                .map(HcStationRecordDO::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, HcPlanOrderDO> planMap = new HashMap<>();
        for (HcPlanOrderDO planOrder : hcPlanOrderMapper.selectBatchIds(planIds)) {
            planMap.put(planOrder.getId(), planOrder);
        }
        return planMap;
    }

    private void fillPlanInfo(HcStationRecordRespVO respVO, HcPlanOrderDO planOrder) {
        if (planOrder == null) {
            return;
        }
        respVO.setBatchNo(firstNotBlank(planOrder.getProductionBatchNo(), planOrder.getBatchNo(), planOrder.getParentProductionBatchNo()));
        respVO.setModelCode(firstNotBlank(planOrder.getMotherModelCode(), planOrder.getModelCode()));
        respVO.setModelName(firstNotBlank(planOrder.getMotherModelName(), planOrder.getModelName()));
    }

    private void fillRecordHeaderInfo(HcStationRecordRespVO respVO, HcStationRecordDO record) {
        if (record == null) {
            return;
        }
        Map<String, Object> header = StringUtils.hasText(record.getHeaderDataJson())
                ? JsonUtils.parseObjectQuietly(record.getHeaderDataJson(), new TypeReference<Map<String, Object>>() {})
                : null;
        if (header == null) {
            header = new HashMap<>();
        }
        HcGrindingMiddleProductRecordDO middleProductRecord = selectMiddleProductRecord(record, header);
        if (middleProductRecord != null) {
            header.put("middleProductRecordId", middleProductRecord.getId());
            header.put("productionBatchNo", firstNotBlank(
                    middleProductRecord.getProductionBatchNo(),
                    toStringValue(header.get("productionBatchNo")),
                    toStringValue(header.get("batchNo"))));
            header.put("batchNo", firstNotBlank(
                    toStringValue(header.get("batchNo")),
                    middleProductRecord.getProductionBatchNo()));
            header.put("segmentMark", firstNotBlank(middleProductRecord.getSegmentMark(), toStringValue(header.get("segmentMark"))));
            header.put("segmentName", firstNotBlank(middleProductRecord.getSegmentName(), toStringValue(header.get("segmentName"))));
            header.put("passType", firstNotBlank(middleProductRecord.getPassType(), toStringValue(header.get("passType"))));
            header.put("passName", firstNotBlank(middleProductRecord.getPassName(), toStringValue(header.get("passName"))));
            respVO.setHeaderDataJson(JsonUtils.toJsonString(header));
        }
        respVO.setBatchNo(firstNotBlank(
                middleProductRecord == null ? null : middleProductRecord.getProductionBatchNo(),
                toStringValue(header.get("productionBatchNo")),
                toStringValue(header.get("batchNo")),
                respVO.getBatchNo()));
        respVO.setModelCode(firstNotBlank(
                middleProductRecord == null ? null : middleProductRecord.getMotherModelCode(),
                toStringValue(header.get("modelCode")),
                respVO.getModelCode()));
        respVO.setModelName(firstNotBlank(
                middleProductRecord == null ? null : middleProductRecord.getMotherModelName(),
                toStringValue(header.get("modelName")),
                respVO.getModelName()));
    }

    private HcGrindingMiddleProductRecordDO selectMiddleProductRecord(HcStationRecordDO record, Map<String, Object> header) {
        if (record == null || !FORM_MIDDLE_PRODUCT.equals(record.getFormCode())) {
            return null;
        }
        HcGrindingMiddleProductRecordDO middleProductRecord = selectMiddleProductRecordByIdIfMatches(
                parseLong(toStringValue(header.get("middleProductRecordId"))), record);
        if (middleProductRecord == null && StringUtils.hasText(record.getBizType()) && record.getBizId() != null) {
            middleProductRecord = hcGrindingMiddleProductRecordMapper.selectByBiz(record.getBizType(), record.getBizId());
        }
        if (middleProductRecord == null) {
            middleProductRecord = selectMiddleProductRecordByIdIfMatches(record.getBizId(), record);
        }
        String segmentMark = normalizeSegmentMark(firstNotBlank(
                toStringValue(header.get("segmentMark")),
                inferSegmentMarkFromBatchNo(firstNotBlank(
                        toStringValue(header.get("productionBatchNo")),
                        toStringValue(header.get("batchNo"))))));
        if (middleProductRecord == null && record.getPlanOperationId() != null && StringUtils.hasText(segmentMark)) {
            String passType = firstNotBlank(
                    toStringValue(header.get("passType")),
                    BIZ_GRINDING_SECOND.equals(record.getBizType()) ? "SECOND" : "FIRST");
            middleProductRecord = hcGrindingMiddleProductRecordMapper.selectBySegment(
                    record.getPlanOperationId(), passType, segmentMark);
        }
        return middleProductRecord;
    }

    private HcGrindingMiddleProductRecordDO selectMiddleProductRecordByIdIfMatches(Long recordId, HcStationRecordDO stationRecord) {
        HcGrindingMiddleProductRecordDO record = recordId == null ? null : hcGrindingMiddleProductRecordMapper.selectById(recordId);
        if (record == null || Boolean.TRUE.equals(record.getDeleted()) || !FORM_MIDDLE_PRODUCT.equals(record.getFormCode())) {
            return null;
        }
        if (stationRecord != null && stationRecord.getPlanOperationId() != null && record.getPlanOperationId() != null
                && !Objects.equals(stationRecord.getPlanOperationId(), record.getPlanOperationId())) {
            return null;
        }
        return record;
    }

    private void fillCreateUserName(HcStationRecordRespVO respVO, HcStationRecordDO record) {
        respVO.setCreateUserName(firstNotBlank(record.getRecordUserName(), record.getCreator()));
    }

    private List<HcStationRecordItemDO> toItemDOList(Long recordId, Long tenantId, List<HcStationRecordItemSaveReqVO> items) {
        List<HcStationRecordItemDO> createList = new ArrayList<>();
        int index = 1;
        for (HcStationRecordItemSaveReqVO item : items) {
            HcStationRecordItemDO itemDO = BeanUtils.toBean(item, HcStationRecordItemDO.class);
            itemDO.setId(null);
            itemDO.setRecordId(recordId);
            itemDO.setTenantId(tenantId);
            if (itemDO.getItemSeq() == null) {
                itemDO.setItemSeq(index);
            }
            createList.add(itemDO);
            index++;
        }
        return createList;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeSegmentMark(String segmentMark) {
        if (!StringUtils.hasText(segmentMark)) {
            return null;
        }
        String text = segmentMark.trim().toUpperCase();
        if (text.startsWith("P")) {
            return "P";
        }
        if (text.startsWith("Q")) {
            return "Q";
        }
        if (text.startsWith("R")) {
            return "R";
        }
        if (text.startsWith("S")) {
            return "S";
        }
        return text;
    }

    private String inferSegmentMarkFromBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return null;
        }
        String text = batchNo.trim().toUpperCase();
        if (text.endsWith("P")) {
            return "P";
        }
        if (text.endsWith("Q")) {
            return "Q";
        }
        if (text.endsWith("R")) {
            return "R";
        }
        if (text.endsWith("S")) {
            return "S";
        }
        return null;
    }
}
