package cn.iocoder.yudao.module.mes.service.hc.processmateriallife;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeStatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeStateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableStateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotSpareRecordMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcProcessMaterialLifeServiceImpl implements HcProcessMaterialLifeService {

    private static final String PROCESS_ROUGH_GRINDING = "ROUGH_GRINDING";
    private static final String PROCESS_WET = "WET";
    private static final String PROCESS_PRESS_SLOT = "PRESS_SLOT";
    private static final String PROCESS_CUT_ROUND = "CUT_ROUND";

    private static final String TYPE_SANDPAPER = "SANDPAPER";
    private static final String TYPE_GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final String TYPE_PET = "PET";
    private static final String TYPE_PRESS_ROLLER = "PRESS_ROLLER";
    private static final String TYPE_BEARING = "BEARING";
    private static final String TYPE_CUTTING_BLADE = "CUTTING_BLADE";
    private static final String TYPE_CUTTING_FELT = "CUTTING_FELT";

    private static final String EVENT_USE = "USE";
    private static final String EVENT_REPLACE = "REPLACE";
    private static final String STATUS_IN_USE = "IN_USE";
    private static final String BIZ_TYPE_MANUAL = "PROCESS_MATERIAL_LIFE_MANUAL";

    @Resource
    private HcEquipmentMapper hcEquipmentMapper;
    @Resource
    private HcEquipmentConsumableStateMapper hcEquipmentConsumableStateMapper;
    @Resource
    private HcEquipmentConsumableEventMapper hcEquipmentConsumableEventMapper;
    @Resource
    private HcPressSlotSpareMapper hcPressSlotSpareMapper;
    @Resource
    private HcPressSlotSpareRecordMapper hcPressSlotSpareRecordMapper;
    @Resource
    private HcCutRoundSpareMapper hcCutRoundSpareMapper;
    @Resource
    private HcCutRoundSpareRecordMapper hcCutRoundSpareRecordMapper;

    @Override
    public PageResult<HcProcessMaterialLifeStateRespVO> getStatePage(HcProcessMaterialLifeStatePageReqVO reqVO) {
        String processCode = normalizeProcessCode(reqVO.getProcessCode());
        if (isGenericProcess(processCode)) {
            return getGenericStatePage(reqVO, processCode);
        }
        if (PROCESS_PRESS_SLOT.equals(processCode)) {
            return getPressSlotStatePage(reqVO);
        }
        if (PROCESS_CUT_ROUND.equals(processCode)) {
            return getCutRoundStatePage(reqVO);
        }
        return emptyPage();
    }

    @Override
    public PageResult<HcProcessMaterialLifeEventRespVO> getEventPage(HcProcessMaterialLifeEventPageReqVO reqVO) {
        String processCode = normalizeProcessCode(reqVO.getProcessCode());
        if (isGenericProcess(processCode)) {
            return getGenericEventPage(reqVO, processCode);
        }
        if (PROCESS_PRESS_SLOT.equals(processCode)) {
            return getPressSlotEventPage(reqVO);
        }
        if (PROCESS_CUT_ROUND.equals(processCode)) {
            return getCutRoundEventPage(reqVO);
        }
        return emptyPage();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(HcProcessMaterialLifeEventSaveReqVO reqVO) {
        String processCode = normalizeProcessCode(reqVO.getProcessCode());
        String consumableType = normalizeConsumableType(reqVO.getConsumableType());
        if (!supportedTypes(processCode).contains(consumableType)) {
            throw invalidParamException("当前工序不支持该耗材类型");
        }
        String eventType = normalizeEventType(reqVO.getEventType());
        if (isGenericProcess(processCode)) {
            return createGenericEvent(reqVO, processCode, consumableType, eventType);
        }
        if (PROCESS_PRESS_SLOT.equals(processCode)) {
            return createPressSlotEvent(reqVO, consumableType, eventType);
        }
        if (PROCESS_CUT_ROUND.equals(processCode)) {
            return createCutRoundEvent(reqVO, consumableType, eventType);
        }
        throw invalidParamException("不支持的工序类型");
    }

    private PageResult<HcProcessMaterialLifeStateRespVO> getGenericStatePage(
            HcProcessMaterialLifeStatePageReqVO reqVO, String processCode) {
        String consumableType = normalizeNullableConsumableType(reqVO.getConsumableType());
        List<String> supportedTypes = supportedTypes(processCode);
        if (StrUtil.isNotBlank(consumableType) && !supportedTypes.contains(consumableType)) {
            return emptyPage();
        }
        var wrapper = new LambdaQueryWrapperX<HcEquipmentConsumableStateDO>()
                .eq(HcEquipmentConsumableStateDO::getProcessCode, processCode)
                .likeIfPresent(HcEquipmentConsumableStateDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcEquipmentConsumableStateDO::getEquipmentName, reqVO.getEquipmentName())
                .eqIfPresent(HcEquipmentConsumableStateDO::getConsumableType, consumableType)
                .likeIfPresent(HcEquipmentConsumableStateDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcEquipmentConsumableStateDO::getWarningFlag, reqVO.getWarningFlag())
                .eqIfPresent(HcEquipmentConsumableStateDO::getStatus, reqVO.getStatus())
                .eq(HcEquipmentConsumableStateDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableStateDO::getWarningFlag)
                .orderByAsc(HcEquipmentConsumableStateDO::getEquipmentCode)
                .orderByAsc(HcEquipmentConsumableStateDO::getConsumableType)
                .orderByDesc(HcEquipmentConsumableStateDO::getId);
        if (StrUtil.isBlank(consumableType)) {
            wrapper.in(HcEquipmentConsumableStateDO::getConsumableType, supportedTypes);
        }
        PageResult<HcEquipmentConsumableStateDO> page = hcEquipmentConsumableStateMapper.selectPage(reqVO, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toGenericStateResp).toList(), page.getTotal());
    }

    private PageResult<HcProcessMaterialLifeEventRespVO> getGenericEventPage(
            HcProcessMaterialLifeEventPageReqVO reqVO, String processCode) {
        String consumableType = normalizeNullableConsumableType(reqVO.getConsumableType());
        List<String> supportedTypes = supportedTypes(processCode);
        if (StrUtil.isNotBlank(consumableType) && !supportedTypes.contains(consumableType)) {
            return emptyPage();
        }
        LambdaQueryWrapperX<HcEquipmentConsumableEventDO> wrapper = new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getProcessCode, processCode)
                .eqIfPresent(HcEquipmentConsumableEventDO::getStateId, reqVO.getStateId())
                .eqIfPresent(HcEquipmentConsumableEventDO::getEquipmentId, reqVO.getEquipmentId())
                .eqIfPresent(HcEquipmentConsumableEventDO::getConsumableType, consumableType)
                .eqIfPresent(HcEquipmentConsumableEventDO::getEventType, normalizeNullableEventType(reqVO.getEventType()))
                .likeIfPresent(HcEquipmentConsumableEventDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcEquipmentConsumableEventDO::getMotherBatchNo, reqVO.getMotherBatchNo())
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByDesc(HcEquipmentConsumableEventDO::getId);
        if (StrUtil.isBlank(consumableType)) {
            wrapper.in(HcEquipmentConsumableEventDO::getConsumableType, supportedTypes);
        }
        appendGenericBatchFilter(wrapper, reqVO.getBatchNo());
        PageResult<HcEquipmentConsumableEventDO> page = hcEquipmentConsumableEventMapper.selectPage(reqVO, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toGenericEventResp).toList(), page.getTotal());
    }

    private PageResult<HcProcessMaterialLifeStateRespVO> getPressSlotStatePage(HcProcessMaterialLifeStatePageReqVO reqVO) {
        String consumableType = normalizeNullableConsumableType(reqVO.getConsumableType());
        List<String> supportedTypes = supportedTypes(PROCESS_PRESS_SLOT);
        if (StrUtil.isNotBlank(consumableType) && !supportedTypes.contains(consumableType)) {
            return emptyPage();
        }
        var wrapper = new LambdaQueryWrapperX<HcPressSlotSpareDO>()
                .likeIfPresent(HcPressSlotSpareDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcPressSlotSpareDO::getEquipmentName, reqVO.getEquipmentName())
                .eqIfPresent(HcPressSlotSpareDO::getSpareType, consumableType)
                .likeIfPresent(HcPressSlotSpareDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcPressSlotSpareDO::getWarningFlag, reqVO.getWarningFlag())
                .eqIfPresent(HcPressSlotSpareDO::getStatus, reqVO.getStatus())
                .eq(HcPressSlotSpareDO::getDeleted, false)
                .orderByDesc(HcPressSlotSpareDO::getWarningFlag)
                .orderByAsc(HcPressSlotSpareDO::getEquipmentCode)
                .orderByAsc(HcPressSlotSpareDO::getSpareType)
                .orderByDesc(HcPressSlotSpareDO::getId);
        if (StrUtil.isBlank(consumableType)) {
            wrapper.in(HcPressSlotSpareDO::getSpareType, supportedTypes);
        }
        PageResult<HcPressSlotSpareDO> page = hcPressSlotSpareMapper.selectPage(reqVO, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toPressSlotStateResp).toList(), page.getTotal());
    }

    private PageResult<HcProcessMaterialLifeEventRespVO> getPressSlotEventPage(HcProcessMaterialLifeEventPageReqVO reqVO) {
        String consumableType = normalizeNullableConsumableType(reqVO.getConsumableType());
        List<String> supportedTypes = supportedTypes(PROCESS_PRESS_SLOT);
        if (StrUtil.isNotBlank(consumableType) && !supportedTypes.contains(consumableType)) {
            return emptyPage();
        }
        LambdaQueryWrapperX<HcPressSlotSpareRecordDO> wrapper = new LambdaQueryWrapperX<HcPressSlotSpareRecordDO>()
                .eqIfPresent(HcPressSlotSpareRecordDO::getSpareId, reqVO.getStateId())
                .eqIfPresent(HcPressSlotSpareRecordDO::getEquipmentId, reqVO.getEquipmentId())
                .eqIfPresent(HcPressSlotSpareRecordDO::getSpareType, consumableType)
                .eqIfPresent(HcPressSlotSpareRecordDO::getEventType, normalizeNullableEventType(reqVO.getEventType()))
                .likeIfPresent(HcPressSlotSpareRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcPressSlotSpareRecordDO::getMotherBatchNo, reqVO.getMotherBatchNo())
                .eq(HcPressSlotSpareRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotSpareRecordDO::getEventTime)
                .orderByDesc(HcPressSlotSpareRecordDO::getId);
        if (StrUtil.isBlank(consumableType)) {
            wrapper.in(HcPressSlotSpareRecordDO::getSpareType, supportedTypes);
        }
        appendPressSlotBatchFilter(wrapper, reqVO.getBatchNo());
        PageResult<HcPressSlotSpareRecordDO> page = hcPressSlotSpareRecordMapper.selectPage(reqVO, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toPressSlotEventResp).toList(), page.getTotal());
    }

    private PageResult<HcProcessMaterialLifeStateRespVO> getCutRoundStatePage(HcProcessMaterialLifeStatePageReqVO reqVO) {
        String consumableType = normalizeNullableConsumableType(reqVO.getConsumableType());
        List<String> supportedTypes = supportedTypes(PROCESS_CUT_ROUND);
        if (StrUtil.isNotBlank(consumableType) && !supportedTypes.contains(consumableType)) {
            return emptyPage();
        }
        var wrapper = new LambdaQueryWrapperX<HcCutRoundSpareDO>()
                .likeIfPresent(HcCutRoundSpareDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcCutRoundSpareDO::getEquipmentName, reqVO.getEquipmentName())
                .eqIfPresent(HcCutRoundSpareDO::getSpareType, consumableType)
                .likeIfPresent(HcCutRoundSpareDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcCutRoundSpareDO::getWarningFlag, reqVO.getWarningFlag())
                .eqIfPresent(HcCutRoundSpareDO::getStatus, reqVO.getStatus())
                .eq(HcCutRoundSpareDO::getDeleted, false)
                .orderByDesc(HcCutRoundSpareDO::getWarningFlag)
                .orderByAsc(HcCutRoundSpareDO::getEquipmentCode)
                .orderByAsc(HcCutRoundSpareDO::getSpareType)
                .orderByDesc(HcCutRoundSpareDO::getId);
        if (StrUtil.isBlank(consumableType)) {
            wrapper.in(HcCutRoundSpareDO::getSpareType, supportedTypes);
        }
        PageResult<HcCutRoundSpareDO> page = hcCutRoundSpareMapper.selectPage(reqVO, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toCutRoundStateResp).toList(), page.getTotal());
    }

    private PageResult<HcProcessMaterialLifeEventRespVO> getCutRoundEventPage(HcProcessMaterialLifeEventPageReqVO reqVO) {
        String consumableType = normalizeNullableConsumableType(reqVO.getConsumableType());
        List<String> supportedTypes = supportedTypes(PROCESS_CUT_ROUND);
        if (StrUtil.isNotBlank(consumableType) && !supportedTypes.contains(consumableType)) {
            return emptyPage();
        }
        LambdaQueryWrapperX<HcCutRoundSpareRecordDO> wrapper = new LambdaQueryWrapperX<HcCutRoundSpareRecordDO>()
                .eqIfPresent(HcCutRoundSpareRecordDO::getSpareId, reqVO.getStateId())
                .eqIfPresent(HcCutRoundSpareRecordDO::getEquipmentId, reqVO.getEquipmentId())
                .eqIfPresent(HcCutRoundSpareRecordDO::getSpareType, consumableType)
                .eqIfPresent(HcCutRoundSpareRecordDO::getEventType, normalizeNullableEventType(reqVO.getEventType()))
                .likeIfPresent(HcCutRoundSpareRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcCutRoundSpareRecordDO::getMotherBatchNo, reqVO.getMotherBatchNo())
                .eq(HcCutRoundSpareRecordDO::getDeleted, false)
                .orderByDesc(HcCutRoundSpareRecordDO::getEventTime)
                .orderByDesc(HcCutRoundSpareRecordDO::getId);
        if (StrUtil.isBlank(consumableType)) {
            wrapper.in(HcCutRoundSpareRecordDO::getSpareType, supportedTypes);
        }
        appendCutRoundBatchFilter(wrapper, reqVO.getBatchNo());
        PageResult<HcCutRoundSpareRecordDO> page = hcCutRoundSpareRecordMapper.selectPage(reqVO, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toCutRoundEventResp).toList(), page.getTotal());
    }

    private Long createGenericEvent(HcProcessMaterialLifeEventSaveReqVO reqVO, String processCode,
                                    String consumableType, String eventType) {
        LocalDateTime eventTime = defaultEventTime(reqVO.getEventTime());
        HcEquipmentConsumableStateDO state = reqVO.getStateId() == null ? null
                : hcEquipmentConsumableStateMapper.selectById(reqVO.getStateId());
        HcEquipmentDO equipment = resolveEquipment(reqVO, state == null ? null : state.getEquipmentId());
        if (state == null && equipment != null) {
            state = hcEquipmentConsumableStateMapper.selectOneByEquipmentAndType(equipment.getId(), processCode, consumableType);
        }
        if (reqVO.getStateId() == null && state != null) {
            throw duplicateStateException(processName(processCode), consumableTypeName(consumableType));
        }
        boolean newState = state == null;
        if (newState && EVENT_USE.equals(eventType)) {
            throw missingStateForUseException(processName(processCode), consumableTypeName(consumableType));
        }
        if (newState) {
            requireBatchNo(reqVO.getBatchNo());
            state = buildGenericState(equipment, processCode, consumableType, reqVO.getBatchNo(), eventTime);
            hcEquipmentConsumableStateMapper.insert(state);
        }
        validateRoughGrindingManualEvent(reqVO, processCode, eventType, newState, state);

        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = zeroIfNull(state.getUsedLength());
        Integer deltaCount = normalizeChangeUseCount(reqVO.getChangeUseCount(), eventType);
        BigDecimal deltaLength = normalizeChangeLength(reqVO.getChangeLength());
        if (PROCESS_ROUGH_GRINDING.equals(processCode) && EVENT_REPLACE.equals(eventType)) {
            deltaCount = 0;
            deltaLength = BigDecimal.ZERO;
        }
        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");

        if (EVENT_REPLACE.equals(eventType)) {
            requireBatchNo(reqVO.getBatchNo());
            state.setBatchNo(reqVO.getBatchNo());
            state.setLastReplaceTime(eventTime);
            state.setLastReplacePlanNo(reqVO.getPlanNo());
            state.setLastReplaceReason(reqVO.getReplaceReason());
            state.setUseCount(deltaCount);
            state.setUsedLength(deltaLength);
        } else {
            state.setBatchNo(firstNotBlank(reqVO.getBatchNo(), state.getBatchNo()));
            state.setUseCount(zeroIfNull(state.getUseCount()) + deltaCount);
            state.setUsedLength(beforeUsedLength.add(deltaLength));
        }
        fillGenericEquipmentSnapshot(state, equipment);
        state.setProcessCode(processCode);
        state.setProcessName(processName(processCode));
        state.setConsumableType(consumableType);
        state.setWarningFlag(calcGenericWarningFlag(state));
        state.setStatus(STATUS_IN_USE);
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark());
        hcEquipmentConsumableStateMapper.updateById(state);

        HcEquipmentConsumableEventDO event = HcEquipmentConsumableEventDO.builder()
                .stateId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .processCode(state.getProcessCode())
                .processName(state.getProcessName())
                .consumableType(state.getConsumableType())
                .eventType(eventType)
                .planNo(reqVO.getPlanNo())
                .motherBatchNo(reqVO.getMotherBatchNo())
                .bizType(BIZ_TYPE_MANUAL)
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(deltaCount)
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(deltaLength)
                .replaceReason(reqVO.getReplaceReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark())
                .tenantId(firstNonNull(state.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .build();
        hcEquipmentConsumableEventMapper.insert(event);
        return event.getId();
    }

    private void validateRoughGrindingManualEvent(HcProcessMaterialLifeEventSaveReqVO reqVO, String processCode,
                                                  String eventType, boolean newState,
                                                  HcEquipmentConsumableStateDO state) {
        if (!PROCESS_ROUGH_GRINDING.equals(processCode)) {
            return;
        }
        if (EVENT_USE.equals(eventType)) {
            if (newState) {
                throw invalidParamException("磨皮耗材在用状态不存在时，请先新建使用状态");
            }
            String requestBatchNo = StrUtil.trimToNull(reqVO.getBatchNo());
            String currentBatchNo = StrUtil.trimToNull(state.getBatchNo());
            if (StrUtil.isNotBlank(requestBatchNo) && !StrUtil.equals(requestBatchNo, currentBatchNo)) {
                throw invalidParamException("使用记录不能变更批号，请使用更换耗材");
            }
            return;
        }
        if (EVENT_REPLACE.equals(eventType) && !newState
                && StrUtil.equals(StrUtil.trimToNull(reqVO.getBatchNo()), StrUtil.trimToNull(state.getBatchNo()))) {
            throw invalidParamException("更换耗材的新批号不能与当前批号一致");
        }
    }

    private Long createPressSlotEvent(HcProcessMaterialLifeEventSaveReqVO reqVO,
                                      String consumableType, String eventType) {
        LocalDateTime eventTime = defaultEventTime(reqVO.getEventTime());
        HcPressSlotSpareDO state = reqVO.getStateId() == null ? null
                : hcPressSlotSpareMapper.selectById(reqVO.getStateId());
        HcEquipmentDO equipment = resolveEquipment(reqVO, state == null ? null : state.getEquipmentId());
        if (state == null && equipment != null) {
            state = hcPressSlotSpareMapper.selectOneByEquipmentAndType(equipment.getId(), consumableType);
        }
        if (reqVO.getStateId() == null && state != null) {
            throw duplicateStateException(processName(PROCESS_PRESS_SLOT), consumableTypeName(consumableType));
        }
        boolean newState = state == null;
        if (newState && EVENT_USE.equals(eventType)) {
            throw missingStateForUseException(processName(PROCESS_PRESS_SLOT), consumableTypeName(consumableType));
        }
        if (newState) {
            requireBatchNo(reqVO.getBatchNo());
            state = buildPressSlotState(equipment, consumableType, reqVO.getBatchNo(), eventTime);
            hcPressSlotSpareMapper.insert(state);
        }

        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = zeroIfNull(state.getUsedLength());
        Integer deltaCount = normalizeChangeUseCount(reqVO.getChangeUseCount(), eventType);
        BigDecimal deltaLength = normalizeChangeLength(reqVO.getChangeLength());
        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");

        if (EVENT_REPLACE.equals(eventType)) {
            requireBatchNo(reqVO.getBatchNo());
            state.setBatchNo(reqVO.getBatchNo());
            state.setLastReplaceTime(eventTime);
            state.setLastReplacePlanNo(reqVO.getPlanNo());
            state.setLastReplaceReason(reqVO.getReplaceReason());
            state.setUseCount(deltaCount);
            state.setUsedLength(deltaLength);
        } else {
            state.setBatchNo(firstNotBlank(reqVO.getBatchNo(), state.getBatchNo()));
            state.setUseCount(zeroIfNull(state.getUseCount()) + deltaCount);
            state.setUsedLength(beforeUsedLength.add(deltaLength));
        }
        fillPressSlotEquipmentSnapshot(state, equipment);
        state.setSpareType(consumableType);
        state.setWarningFlag(calcCountWarningFlag(state.getUseCount(), state.getLimitCount()));
        state.setStatus(STATUS_IN_USE);
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark());
        hcPressSlotSpareMapper.updateById(state);

        HcPressSlotSpareRecordDO record = HcPressSlotSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(state.getSpareType())
                .eventType(eventType)
                .planNo(reqVO.getPlanNo())
                .motherBatchNo(reqVO.getMotherBatchNo())
                .bizType(BIZ_TYPE_MANUAL)
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(deltaCount)
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(deltaLength)
                .replaceReason(reqVO.getReplaceReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark())
                .tenantId(firstNonNull(state.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .build();
        hcPressSlotSpareRecordMapper.insert(record);
        return record.getId();
    }

    private Long createCutRoundEvent(HcProcessMaterialLifeEventSaveReqVO reqVO,
                                     String consumableType, String eventType) {
        LocalDateTime eventTime = defaultEventTime(reqVO.getEventTime());
        HcCutRoundSpareDO state = reqVO.getStateId() == null ? null
                : hcCutRoundSpareMapper.selectById(reqVO.getStateId());
        HcEquipmentDO equipment = resolveEquipment(reqVO, state == null ? null : state.getEquipmentId());
        if (state == null && equipment != null) {
            state = hcCutRoundSpareMapper.selectOneByEquipmentAndType(equipment.getId(), consumableType);
        }
        if (reqVO.getStateId() == null && state != null) {
            throw duplicateStateException(processName(PROCESS_CUT_ROUND), consumableTypeName(consumableType));
        }
        boolean newState = state == null;
        if (newState && EVENT_USE.equals(eventType)) {
            throw missingStateForUseException(processName(PROCESS_CUT_ROUND), consumableTypeName(consumableType));
        }
        if (newState) {
            requireBatchNo(reqVO.getBatchNo());
            state = buildCutRoundState(equipment, consumableType, reqVO.getBatchNo(), eventTime);
            hcCutRoundSpareMapper.insert(state);
        }

        String beforeBatchNo = state.getBatchNo();
        Integer beforeUseCount = state.getUseCount();
        BigDecimal beforeUsedLength = zeroIfNull(state.getUsedLength());
        Integer deltaCount = normalizeChangeUseCount(reqVO.getChangeUseCount(), eventType);
        BigDecimal deltaLength = normalizeChangeLength(reqVO.getChangeLength());
        Long operatorId = firstNonNull(reqVO.getOperatorId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");

        if (EVENT_REPLACE.equals(eventType)) {
            requireBatchNo(reqVO.getBatchNo());
            state.setBatchNo(reqVO.getBatchNo());
            state.setLastReplaceTime(eventTime);
            state.setLastReplacePlanNo(reqVO.getPlanNo());
            state.setLastReplaceReason(reqVO.getReplaceReason());
            state.setUseCount(deltaCount);
            state.setUsedLength(deltaLength);
        } else {
            state.setBatchNo(firstNotBlank(reqVO.getBatchNo(), state.getBatchNo()));
            state.setUseCount(zeroIfNull(state.getUseCount()) + deltaCount);
            state.setUsedLength(beforeUsedLength.add(deltaLength));
        }
        fillCutRoundEquipmentSnapshot(state, equipment);
        state.setSpareType(consumableType);
        state.setWarningFlag(calcCountWarningFlag(state.getUseCount(), state.getLimitCount()));
        state.setStatus(STATUS_IN_USE);
        state.setLastOperatorId(operatorId);
        state.setLastOperatorName(operatorName);
        state.setLastEventTime(eventTime);
        state.setRemark(reqVO.getRemark());
        hcCutRoundSpareMapper.updateById(state);

        HcCutRoundSpareRecordDO record = HcCutRoundSpareRecordDO.builder()
                .spareId(state.getId())
                .equipmentId(state.getEquipmentId())
                .equipmentCode(state.getEquipmentCode())
                .equipmentName(state.getEquipmentName())
                .workCenterId(state.getWorkCenterId())
                .workCenterCode(state.getWorkCenterCode())
                .workCenterName(state.getWorkCenterName())
                .spareType(state.getSpareType())
                .eventType(eventType)
                .planNo(reqVO.getPlanNo())
                .motherBatchNo(reqVO.getMotherBatchNo())
                .bizType(BIZ_TYPE_MANUAL)
                .beforeBatchNo(beforeBatchNo)
                .afterBatchNo(state.getBatchNo())
                .beforeUseCount(beforeUseCount)
                .afterUseCount(state.getUseCount())
                .changeUseCount(deltaCount)
                .beforeUsedLength(beforeUsedLength)
                .afterUsedLength(state.getUsedLength())
                .changeLength(deltaLength)
                .replaceReason(reqVO.getReplaceReason())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventTime(eventTime)
                .remark(reqVO.getRemark())
                .tenantId(firstNonNull(state.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .build();
        hcCutRoundSpareRecordMapper.insert(record);
        return record.getId();
    }

    private HcEquipmentConsumableStateDO buildGenericState(HcEquipmentDO equipment, String processCode,
                                                           String consumableType, String batchNo,
                                                           LocalDateTime eventTime) {
        if (equipment == null) {
            throw invalidParamException("设备不存在，请填写有效设备编码");
        }
        HcEquipmentConsumableStateDO state = HcEquipmentConsumableStateDO.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .workCenterId(equipment.getWorkCenterId())
                .workCenterCode(equipment.getWorkCenterCode())
                .workCenterName(equipment.getWorkCenterName())
                .processCode(processCode)
                .processName(processName(processCode))
                .consumableType(consumableType)
                .batchNo(batchNo)
                .lastReplaceTime(eventTime)
                .useCount(0)
                .usedLength(BigDecimal.ZERO)
                .limitCount(defaultLimitCount(processCode, consumableType))
                .limitLength(defaultLimitLength(processCode, consumableType))
                .warningFlag(0)
                .status(STATUS_IN_USE)
                .lastEventTime(eventTime)
                .tenantId(firstNonNull(equipment.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .build();
        state.setWarningFlag(calcGenericWarningFlag(state));
        return state;
    }

    private HcPressSlotSpareDO buildPressSlotState(HcEquipmentDO equipment, String consumableType,
                                                   String batchNo, LocalDateTime eventTime) {
        if (equipment == null) {
            throw invalidParamException("设备不存在，请填写有效设备编码");
        }
        return HcPressSlotSpareDO.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .workCenterId(equipment.getWorkCenterId())
                .workCenterCode(equipment.getWorkCenterCode())
                .workCenterName(equipment.getWorkCenterName())
                .spareType(consumableType)
                .batchNo(batchNo)
                .lastReplaceTime(eventTime)
                .useCount(0)
                .usedLength(BigDecimal.ZERO)
                .limitCount(defaultLimitCount(PROCESS_PRESS_SLOT, consumableType))
                .warningFlag(0)
                .status(STATUS_IN_USE)
                .lastEventTime(eventTime)
                .tenantId(firstNonNull(equipment.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .build();
    }

    private HcCutRoundSpareDO buildCutRoundState(HcEquipmentDO equipment, String consumableType,
                                                 String batchNo, LocalDateTime eventTime) {
        if (equipment == null) {
            throw invalidParamException("设备不存在，请填写有效设备编码");
        }
        return HcCutRoundSpareDO.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .workCenterId(equipment.getWorkCenterId())
                .workCenterCode(equipment.getWorkCenterCode())
                .workCenterName(equipment.getWorkCenterName())
                .spareType(consumableType)
                .batchNo(batchNo)
                .lastReplaceTime(eventTime)
                .useCount(0)
                .usedLength(BigDecimal.ZERO)
                .limitCount(defaultLimitCount(PROCESS_CUT_ROUND, consumableType))
                .warningFlag(0)
                .status(STATUS_IN_USE)
                .lastEventTime(eventTime)
                .tenantId(firstNonNull(equipment.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .build();
    }

    private HcEquipmentDO resolveEquipment(HcProcessMaterialLifeEventSaveReqVO reqVO, Long fallbackEquipmentId) {
        Long equipmentId = firstNonNull(reqVO.getEquipmentId(), fallbackEquipmentId);
        HcEquipmentDO equipment = equipmentId == null ? null : hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null && StrUtil.isNotBlank(reqVO.getEquipmentCode())) {
            equipment = hcEquipmentMapper.selectOne(new LambdaQueryWrapperX<HcEquipmentDO>()
                    .eq(HcEquipmentDO::getEquipmentCode, reqVO.getEquipmentCode())
                    .eq(HcEquipmentDO::getDeleted, false)
                    .last("LIMIT 1"));
        }
        if (equipment == null && fallbackEquipmentId == null) {
            throw invalidParamException("设备不存在，请填写有效设备编码");
        }
        return equipment;
    }

    private void fillGenericEquipmentSnapshot(HcEquipmentConsumableStateDO state, HcEquipmentDO equipment) {
        if (equipment == null) {
            return;
        }
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(equipment.getEquipmentCode());
        state.setEquipmentName(equipment.getEquipmentName());
        state.setWorkCenterId(equipment.getWorkCenterId());
        state.setWorkCenterCode(equipment.getWorkCenterCode());
        state.setWorkCenterName(equipment.getWorkCenterName());
        state.setTenantId(firstNonNull(state.getTenantId(), equipment.getTenantId(), TenantContextHolder.getRequiredTenantId()));
    }

    private void fillPressSlotEquipmentSnapshot(HcPressSlotSpareDO state, HcEquipmentDO equipment) {
        if (equipment == null) {
            return;
        }
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(equipment.getEquipmentCode());
        state.setEquipmentName(equipment.getEquipmentName());
        state.setWorkCenterId(equipment.getWorkCenterId());
        state.setWorkCenterCode(equipment.getWorkCenterCode());
        state.setWorkCenterName(equipment.getWorkCenterName());
        state.setTenantId(firstNonNull(state.getTenantId(), equipment.getTenantId(), TenantContextHolder.getRequiredTenantId()));
    }

    private void fillCutRoundEquipmentSnapshot(HcCutRoundSpareDO state, HcEquipmentDO equipment) {
        if (equipment == null) {
            return;
        }
        state.setEquipmentId(equipment.getId());
        state.setEquipmentCode(equipment.getEquipmentCode());
        state.setEquipmentName(equipment.getEquipmentName());
        state.setWorkCenterId(equipment.getWorkCenterId());
        state.setWorkCenterCode(equipment.getWorkCenterCode());
        state.setWorkCenterName(equipment.getWorkCenterName());
        state.setTenantId(firstNonNull(state.getTenantId(), equipment.getTenantId(), TenantContextHolder.getRequiredTenantId()));
    }

    private HcProcessMaterialLifeStateRespVO toGenericStateResp(HcEquipmentConsumableStateDO state) {
        HcProcessMaterialLifeStateRespVO respVO = new HcProcessMaterialLifeStateRespVO();
        respVO.setId(state.getId());
        respVO.setEquipmentId(state.getEquipmentId());
        respVO.setEquipmentCode(state.getEquipmentCode());
        respVO.setEquipmentName(state.getEquipmentName());
        respVO.setWorkCenterId(state.getWorkCenterId());
        respVO.setWorkCenterCode(state.getWorkCenterCode());
        respVO.setWorkCenterName(state.getWorkCenterName());
        respVO.setProcessCode(state.getProcessCode());
        respVO.setProcessName(processName(state.getProcessCode()));
        respVO.setConsumableType(state.getConsumableType());
        respVO.setConsumableTypeName(consumableTypeName(state.getConsumableType()));
        respVO.setBatchNo(state.getBatchNo());
        respVO.setUseCount(state.getUseCount());
        respVO.setUsedLength(state.getUsedLength());
        respVO.setLimitCount(state.getLimitCount());
        respVO.setLimitLength(state.getLimitLength());
        respVO.setWarningFlag(state.getWarningFlag());
        respVO.setStatus(state.getStatus());
        respVO.setLastOperatorId(state.getLastOperatorId());
        respVO.setLastOperatorName(state.getLastOperatorName());
        respVO.setLastReplacePlanNo(state.getLastReplacePlanNo());
        respVO.setLastReplaceReason(state.getLastReplaceReason());
        respVO.setLastReplaceTime(state.getLastReplaceTime());
        respVO.setLastEventTime(state.getLastEventTime());
        respVO.setRemark(state.getRemark());
        return respVO;
    }

    private HcProcessMaterialLifeEventRespVO toGenericEventResp(HcEquipmentConsumableEventDO event) {
        HcProcessMaterialLifeEventRespVO respVO = new HcProcessMaterialLifeEventRespVO();
        respVO.setId(event.getId());
        respVO.setStateId(event.getStateId());
        respVO.setEquipmentId(event.getEquipmentId());
        respVO.setEquipmentCode(event.getEquipmentCode());
        respVO.setEquipmentName(event.getEquipmentName());
        respVO.setProcessCode(event.getProcessCode());
        respVO.setProcessName(processName(event.getProcessCode()));
        respVO.setConsumableType(event.getConsumableType());
        respVO.setConsumableTypeName(consumableTypeName(event.getConsumableType()));
        respVO.setEventType(event.getEventType());
        respVO.setPlanNo(event.getPlanNo());
        respVO.setMotherBatchNo(event.getMotherBatchNo());
        respVO.setBeforeBatchNo(event.getBeforeBatchNo());
        respVO.setAfterBatchNo(event.getAfterBatchNo());
        respVO.setBeforeUseCount(event.getBeforeUseCount());
        respVO.setAfterUseCount(event.getAfterUseCount());
        respVO.setChangeUseCount(event.getChangeUseCount());
        respVO.setBeforeUsedLength(event.getBeforeUsedLength());
        respVO.setAfterUsedLength(event.getAfterUsedLength());
        respVO.setChangeLength(event.getChangeLength());
        respVO.setReplaceReason(event.getReplaceReason());
        respVO.setOperatorId(event.getOperatorId());
        respVO.setOperatorName(event.getOperatorName());
        respVO.setEventTime(event.getEventTime());
        respVO.setRemark(event.getRemark());
        return respVO;
    }

    private HcProcessMaterialLifeStateRespVO toPressSlotStateResp(HcPressSlotSpareDO state) {
        HcProcessMaterialLifeStateRespVO respVO = baseSpareStateResp(state.getId(), state.getEquipmentId(),
                state.getEquipmentCode(), state.getEquipmentName(), state.getWorkCenterId(), state.getWorkCenterCode(),
                state.getWorkCenterName(), PROCESS_PRESS_SLOT, state.getSpareType(), state.getBatchNo(),
                state.getUseCount(), state.getUsedLength(), state.getLimitCount(), state.getWarningFlag(),
                state.getStatus(), state.getLastOperatorId(), state.getLastOperatorName(), state.getLastReplacePlanNo(),
                state.getLastReplaceReason(), state.getLastReplaceTime(), state.getLastEventTime(), state.getRemark());
        return respVO;
    }

    private HcProcessMaterialLifeEventRespVO toPressSlotEventResp(HcPressSlotSpareRecordDO record) {
        return baseSpareEventResp(record.getId(), record.getSpareId(), record.getEquipmentId(), record.getEquipmentCode(),
                record.getEquipmentName(), PROCESS_PRESS_SLOT, record.getSpareType(), record.getEventType(),
                record.getPlanNo(), record.getMotherBatchNo(), record.getBeforeBatchNo(), record.getAfterBatchNo(),
                record.getBeforeUseCount(), record.getAfterUseCount(), record.getChangeUseCount(),
                record.getBeforeUsedLength(), record.getAfterUsedLength(), record.getChangeLength(),
                record.getReplaceReason(), record.getOperatorId(), record.getOperatorName(), record.getEventTime(),
                record.getRemark());
    }

    private HcProcessMaterialLifeStateRespVO toCutRoundStateResp(HcCutRoundSpareDO state) {
        return baseSpareStateResp(state.getId(), state.getEquipmentId(), state.getEquipmentCode(),
                state.getEquipmentName(), state.getWorkCenterId(), state.getWorkCenterCode(), state.getWorkCenterName(),
                PROCESS_CUT_ROUND, state.getSpareType(), state.getBatchNo(), state.getUseCount(), state.getUsedLength(),
                state.getLimitCount(), state.getWarningFlag(), state.getStatus(), state.getLastOperatorId(),
                state.getLastOperatorName(), state.getLastReplacePlanNo(), state.getLastReplaceReason(),
                state.getLastReplaceTime(), state.getLastEventTime(), state.getRemark());
    }

    private HcProcessMaterialLifeEventRespVO toCutRoundEventResp(HcCutRoundSpareRecordDO record) {
        return baseSpareEventResp(record.getId(), record.getSpareId(), record.getEquipmentId(), record.getEquipmentCode(),
                record.getEquipmentName(), PROCESS_CUT_ROUND, record.getSpareType(), record.getEventType(),
                record.getPlanNo(), record.getMotherBatchNo(), record.getBeforeBatchNo(), record.getAfterBatchNo(),
                record.getBeforeUseCount(), record.getAfterUseCount(), record.getChangeUseCount(),
                record.getBeforeUsedLength(), record.getAfterUsedLength(), record.getChangeLength(),
                record.getReplaceReason(), record.getOperatorId(), record.getOperatorName(), record.getEventTime(),
                record.getRemark());
    }

    private HcProcessMaterialLifeStateRespVO baseSpareStateResp(Long id, Long equipmentId, String equipmentCode,
            String equipmentName, Long workCenterId, String workCenterCode, String workCenterName, String processCode,
            String consumableType, String batchNo, Integer useCount, BigDecimal usedLength, Integer limitCount,
            Integer warningFlag, String status, Long lastOperatorId, String lastOperatorName, String lastReplacePlanNo,
            String lastReplaceReason, LocalDateTime lastReplaceTime, LocalDateTime lastEventTime, String remark) {
        HcProcessMaterialLifeStateRespVO respVO = new HcProcessMaterialLifeStateRespVO();
        respVO.setId(id);
        respVO.setEquipmentId(equipmentId);
        respVO.setEquipmentCode(equipmentCode);
        respVO.setEquipmentName(equipmentName);
        respVO.setWorkCenterId(workCenterId);
        respVO.setWorkCenterCode(workCenterCode);
        respVO.setWorkCenterName(workCenterName);
        respVO.setProcessCode(processCode);
        respVO.setProcessName(processName(processCode));
        respVO.setConsumableType(consumableType);
        respVO.setConsumableTypeName(consumableTypeName(consumableType));
        respVO.setBatchNo(batchNo);
        respVO.setUseCount(useCount);
        respVO.setUsedLength(usedLength);
        respVO.setLimitCount(limitCount);
        respVO.setWarningFlag(warningFlag);
        respVO.setStatus(status);
        respVO.setLastOperatorId(lastOperatorId);
        respVO.setLastOperatorName(lastOperatorName);
        respVO.setLastReplacePlanNo(lastReplacePlanNo);
        respVO.setLastReplaceReason(lastReplaceReason);
        respVO.setLastReplaceTime(lastReplaceTime);
        respVO.setLastEventTime(lastEventTime);
        respVO.setRemark(remark);
        return respVO;
    }

    private HcProcessMaterialLifeEventRespVO baseSpareEventResp(Long id, Long stateId, Long equipmentId,
            String equipmentCode, String equipmentName, String processCode, String consumableType, String eventType,
            String planNo, String motherBatchNo, String beforeBatchNo, String afterBatchNo, Integer beforeUseCount,
            Integer afterUseCount, Integer changeUseCount, BigDecimal beforeUsedLength, BigDecimal afterUsedLength,
            BigDecimal changeLength, String replaceReason, Long operatorId, String operatorName,
            LocalDateTime eventTime, String remark) {
        HcProcessMaterialLifeEventRespVO respVO = new HcProcessMaterialLifeEventRespVO();
        respVO.setId(id);
        respVO.setStateId(stateId);
        respVO.setEquipmentId(equipmentId);
        respVO.setEquipmentCode(equipmentCode);
        respVO.setEquipmentName(equipmentName);
        respVO.setProcessCode(processCode);
        respVO.setProcessName(processName(processCode));
        respVO.setConsumableType(consumableType);
        respVO.setConsumableTypeName(consumableTypeName(consumableType));
        respVO.setEventType(eventType);
        respVO.setPlanNo(planNo);
        respVO.setMotherBatchNo(motherBatchNo);
        respVO.setBeforeBatchNo(beforeBatchNo);
        respVO.setAfterBatchNo(afterBatchNo);
        respVO.setBeforeUseCount(beforeUseCount);
        respVO.setAfterUseCount(afterUseCount);
        respVO.setChangeUseCount(changeUseCount);
        respVO.setBeforeUsedLength(beforeUsedLength);
        respVO.setAfterUsedLength(afterUsedLength);
        respVO.setChangeLength(changeLength);
        respVO.setReplaceReason(replaceReason);
        respVO.setOperatorId(operatorId);
        respVO.setOperatorName(operatorName);
        respVO.setEventTime(eventTime);
        respVO.setRemark(remark);
        return respVO;
    }

    private void appendGenericBatchFilter(LambdaQueryWrapperX<HcEquipmentConsumableEventDO> wrapper, String batchNo) {
        if (StrUtil.isBlank(batchNo)) {
            return;
        }
        wrapper.and(item -> item.like(HcEquipmentConsumableEventDO::getBeforeBatchNo, batchNo)
                .or().like(HcEquipmentConsumableEventDO::getAfterBatchNo, batchNo));
    }

    private void appendPressSlotBatchFilter(LambdaQueryWrapperX<HcPressSlotSpareRecordDO> wrapper, String batchNo) {
        if (StrUtil.isBlank(batchNo)) {
            return;
        }
        wrapper.and(item -> item.like(HcPressSlotSpareRecordDO::getBeforeBatchNo, batchNo)
                .or().like(HcPressSlotSpareRecordDO::getAfterBatchNo, batchNo));
    }

    private void appendCutRoundBatchFilter(LambdaQueryWrapperX<HcCutRoundSpareRecordDO> wrapper, String batchNo) {
        if (StrUtil.isBlank(batchNo)) {
            return;
        }
        wrapper.and(item -> item.like(HcCutRoundSpareRecordDO::getBeforeBatchNo, batchNo)
                .or().like(HcCutRoundSpareRecordDO::getAfterBatchNo, batchNo));
    }

    private Integer calcGenericWarningFlag(HcEquipmentConsumableStateDO state) {
        boolean lengthWarn = state.getLimitLength() != null
                && state.getUsedLength() != null
                && state.getUsedLength().compareTo(state.getLimitLength()) >= 0;
        boolean countWarn = state.getLimitCount() != null
                && state.getUseCount() != null
                && state.getUseCount() >= state.getLimitCount();
        return lengthWarn || countWarn ? 1 : 0;
    }

    private Integer calcCountWarningFlag(Integer useCount, Integer limitCount) {
        return useCount != null && limitCount != null && useCount >= limitCount ? 1 : 0;
    }

    private boolean isGenericProcess(String processCode) {
        return PROCESS_ROUGH_GRINDING.equals(processCode) || PROCESS_WET.equals(processCode);
    }

    private List<String> supportedTypes(String processCode) {
        if (PROCESS_WET.equals(processCode)) {
            return List.of(TYPE_PET, TYPE_GUIDE_CLOTH);
        }
        if (PROCESS_PRESS_SLOT.equals(processCode)) {
            return List.of(TYPE_PRESS_ROLLER, TYPE_BEARING);
        }
        if (PROCESS_CUT_ROUND.equals(processCode)) {
            return List.of(TYPE_CUTTING_BLADE, TYPE_CUTTING_FELT);
        }
        return List.of(TYPE_SANDPAPER, TYPE_GUIDE_CLOTH);
    }

    private Integer defaultLimitCount(String processCode, String consumableType) {
        if (PROCESS_ROUGH_GRINDING.equals(processCode) && TYPE_GUIDE_CLOTH.equals(consumableType)) {
            return 200;
        }
        if (PROCESS_WET.equals(processCode) && TYPE_GUIDE_CLOTH.equals(consumableType)) {
            return 28;
        }
        if (PROCESS_PRESS_SLOT.equals(processCode) && TYPE_PRESS_ROLLER.equals(consumableType)) {
            return 2000;
        }
        if (PROCESS_PRESS_SLOT.equals(processCode) && TYPE_BEARING.equals(consumableType)) {
            return 5000;
        }
        if (PROCESS_CUT_ROUND.equals(processCode) && TYPE_CUTTING_BLADE.equals(consumableType)) {
            return 5000;
        }
        if (PROCESS_CUT_ROUND.equals(processCode) && TYPE_CUTTING_FELT.equals(consumableType)) {
            return 2000;
        }
        return null;
    }

    private BigDecimal defaultLimitLength(String processCode, String consumableType) {
        if (PROCESS_ROUGH_GRINDING.equals(processCode) && TYPE_SANDPAPER.equals(consumableType)) {
            return new BigDecimal("500");
        }
        return null;
    }

    private String processName(String processCode) {
        if (PROCESS_WET.equals(processCode)) {
            return "湿法";
        }
        if (PROCESS_PRESS_SLOT.equals(processCode)) {
            return "压槽";
        }
        if (PROCESS_CUT_ROUND.equals(processCode)) {
            return "裁切";
        }
        return "磨皮";
    }

    private String consumableTypeName(String value) {
        if (TYPE_SANDPAPER.equals(value)) {
            return "砂纸";
        }
        if (TYPE_GUIDE_CLOTH.equals(value)) {
            return "导布";
        }
        if (TYPE_PET.equals(value)) {
            return "PET";
        }
        if (TYPE_PRESS_ROLLER.equals(value)) {
            return "压辊";
        }
        if (TYPE_BEARING.equals(value)) {
            return "轴承";
        }
        if (TYPE_CUTTING_BLADE.equals(value)) {
            return "刀片";
        }
        if (TYPE_CUTTING_FELT.equals(value)) {
            return "毛毡";
        }
        return value;
    }

    private RuntimeException duplicateStateException(String processName, String consumableTypeName) {
        return invalidParamException(String.format("该设备%s工序已存在%s使用状态，请在对应卡片登记使用或更换", processName, consumableTypeName));
    }

    private RuntimeException missingStateForUseException(String processName, String consumableTypeName) {
        return invalidParamException(String.format("%s工序%s使用状态不存在，请先新建使用状态", processName, consumableTypeName));
    }

    private String normalizeProcessCode(String value) {
        String text = StrUtil.blankToDefault(value, PROCESS_ROUGH_GRINDING).trim().toUpperCase();
        if ("ROUGH".equals(text) || "GRINDING".equals(text) || "磨皮".equals(value)) {
            return PROCESS_ROUGH_GRINDING;
        }
        if ("WET_REPORT".equals(text) || "湿法".equals(value)) {
            return PROCESS_WET;
        }
        if ("PRESS".equals(text) || "PRESS_SLOT_REPORT".equals(text) || "压槽".equals(value)) {
            return PROCESS_PRESS_SLOT;
        }
        if ("CUT".equals(text) || "CUT_ROUND_REPORT".equals(text) || "裁切".equals(value)) {
            return PROCESS_CUT_ROUND;
        }
        return text;
    }

    private String normalizeNullableConsumableType(String value) {
        return StrUtil.isBlank(value) ? null : normalizeConsumableType(value);
    }

    private String normalizeConsumableType(String value) {
        String text = StrUtil.trim(value);
        String upper = text == null ? "" : text.toUpperCase();
        if (TYPE_SANDPAPER.equals(upper) || "砂纸".equals(text)) {
            return TYPE_SANDPAPER;
        }
        if (TYPE_GUIDE_CLOTH.equals(upper) || "GUIDECLOTH".equals(upper) || "导布".equals(text)) {
            return TYPE_GUIDE_CLOTH;
        }
        if (TYPE_PET.equals(upper)) {
            return TYPE_PET;
        }
        if (TYPE_PRESS_ROLLER.equals(upper) || "ROLLER".equals(upper) || "压辊".equals(text)) {
            return TYPE_PRESS_ROLLER;
        }
        if (TYPE_BEARING.equals(upper) || "轴承".equals(text)) {
            return TYPE_BEARING;
        }
        if (TYPE_CUTTING_BLADE.equals(upper) || "BLADE".equals(upper) || "CUTTING_KNIFE".equals(upper)
                || "刀片".equals(text)) {
            return TYPE_CUTTING_BLADE;
        }
        if (TYPE_CUTTING_FELT.equals(upper) || "FELT".equals(upper) || "毛毡".equals(text)) {
            return TYPE_CUTTING_FELT;
        }
        return upper;
    }

    private String normalizeNullableEventType(String value) {
        return StrUtil.isBlank(value) ? null : normalizeEventType(value);
    }

    private String normalizeEventType(String value) {
        String text = StrUtil.blankToDefault(value, EVENT_USE).trim();
        String upper = text.toUpperCase();
        if (EVENT_REPLACE.equals(upper) || "更换".equals(text)) {
            return EVENT_REPLACE;
        }
        if (EVENT_USE.equals(upper) || "使用".equals(text)) {
            return EVENT_USE;
        }
        throw invalidParamException("记录类型仅支持使用或更换");
    }

    private void requireBatchNo(String batchNo) {
        if (StrUtil.isBlank(batchNo)) {
            throw invalidParamException("耗材批号不能为空");
        }
    }

    private LocalDateTime defaultEventTime(LocalDateTime eventTime) {
        return eventTime == null ? LocalDateTime.now() : eventTime;
    }

    private Integer normalizeChangeUseCount(Integer value, String eventType) {
        int result = value == null ? (EVENT_USE.equals(eventType) ? 1 : 0) : value;
        if (result < 0) {
            throw invalidParamException("使用次数不能为负数");
        }
        return result;
    }

    private BigDecimal normalizeChangeLength(BigDecimal value) {
        BigDecimal result = value == null ? BigDecimal.ZERO : value;
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException("使用米数不能为负数");
        }
        return result;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> PageResult<T> emptyPage() {
        return new PageResult<>(List.of(), 0L);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
