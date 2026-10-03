package cn.iocoder.yudao.module.mes.service.hc.equipment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentWorkStateAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCEQUIPMENT_EQUIPMENTCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCEQUIPMENT_NOT_EXISTS;

@Service
@Validated
public class HcEquipmentServiceImpl implements HcEquipmentService {

    private static final String WORK_STATUS_IDLE = "IDLE";

    @Resource
    private HcEquipmentMapper hcEquipmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcEquipment(HcEquipmentSaveReqVO createReqVO) {
        validateEquipmentCodeUnique(null, createReqVO.getEquipmentCode());
        normalizeApplicablePadType(createReqVO);
        HcEquipmentDO entity = BeanUtils.toBean(createReqVO, HcEquipmentDO.class);
        hcEquipmentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcEquipment(HcEquipmentSaveReqVO updateReqVO) {
        validateHcEquipmentExists(updateReqVO.getId());
        validateEquipmentCodeUnique(updateReqVO.getId(), updateReqVO.getEquipmentCode());
        normalizeApplicablePadType(updateReqVO);
        HcEquipmentDO updateObj = BeanUtils.toBean(updateReqVO, HcEquipmentDO.class);
        hcEquipmentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcEquipment(Long id) {
        validateHcEquipmentExists(id);
        hcEquipmentMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcEquipmentListByIds(List<Long> ids) {
        hcEquipmentMapper.deleteByIds(ids);
    }

    private void validateHcEquipmentExists(Long id) {
        if (hcEquipmentMapper.selectById(id) == null) {
            throw exception(HCEQUIPMENT_NOT_EXISTS);
        }
    }

    private void validateEquipmentCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcEquipmentDO entity = hcEquipmentMapper.selectOne(new LambdaQueryWrapperX<HcEquipmentDO>().eq(HcEquipmentDO::getEquipmentCode, value).neIfPresent(HcEquipmentDO::getId, id));
        if (entity != null) {
            throw exception(HCEQUIPMENT_EQUIPMENTCODE_EXISTS);
        }
    }

    private void normalizeApplicablePadType(HcEquipmentSaveReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getApplicablePadType())) {
            reqVO.setApplicablePadType(null);
            reqVO.setApplicablePadTypeName(null);
            return;
        }
        String padType = reqVO.getApplicablePadType().trim().toUpperCase();
        reqVO.setApplicablePadType(padType);
        reqVO.setApplicablePadTypeName(switch (padType) {
            case "WHITE_PAD" -> "白垫";
            case "BLACK_PAD" -> "黑垫";
            case "COMMON" -> "通用";
            default -> reqVO.getApplicablePadTypeName();
        });
    }

    @Override
    public HcEquipmentDO getHcEquipment(Long id) {
        return hcEquipmentMapper.selectById(id);
    }

    @Override
    public List<HcEquipmentDO> getHcEquipmentSimpleList() {
        LambdaQueryWrapperX<HcEquipmentDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcEquipmentDO::getStatus, 0);
        queryWrapper.orderByAsc(HcEquipmentDO::getEquipmentCode);
        queryWrapper.orderByDesc(HcEquipmentDO::getId);
        return hcEquipmentMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcEquipmentDO> getHcEquipmentList(HcEquipmentPageReqVO reqVO) {
        return hcEquipmentMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcEquipmentDO> getHcEquipmentPage(HcEquipmentPageReqVO pageReqVO) {
        return hcEquipmentMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustWorkState(HcEquipmentWorkStateAdjustReqVO reqVO) {
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(reqVO.getId());
        if (equipment == null) {
            throw exception(HCEQUIPMENT_NOT_EXISTS);
        }
        HcEquipmentDO updateObj = new HcEquipmentDO();
        updateObj.setId(reqVO.getId());
        updateObj.setWorkStatus(reqVO.getWorkStatus());
        updateObj.setCurrentRecordTime(LocalDateTime.now());
        if (StringUtils.hasText(reqVO.getRemark())) {
            updateObj.setRemark(reqVO.getRemark());
        }
        if (Boolean.TRUE.equals(reqVO.getClearOperationBinding())) {
            clearBindingFields(updateObj);
        }
        hcEquipmentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void occupyEquipment(Long equipmentId, String planNo, String operationCode, String operationName,
                                LocalDateTime startTime, String operatorName, LocalDateTime recordTime) {
        if (equipmentId == null) {
            return;
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return;
        }
        HcEquipmentDO updateObj = new HcEquipmentDO();
        updateObj.setId(equipmentId);
        updateObj.setWorkStatus("PRODUCING");
        updateObj.setCurrentPlanNo(planNo);
        updateObj.setCurrentOperationCode(operationCode);
        updateObj.setCurrentOperationName(operationName);
        updateObj.setCurrentStartTime(startTime);
        updateObj.setCurrentEndTime(null);
        updateObj.setCurrentOperatorName(operatorName);
        updateObj.setCurrentRecordTime(recordTime != null ? recordTime : LocalDateTime.now());
        hcEquipmentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryOccupyEquipmentStrict(Long equipmentId, String planNo, String operationCode,
                                            String operationName, LocalDateTime startTime,
                                            String operatorName, LocalDateTime recordTime) {
        if (equipmentId == null || !StringUtils.hasText(planNo)) {
            return false;
        }
        LocalDateTime effectiveRecordTime = recordTime != null ? recordTime : LocalDateTime.now();
        return hcEquipmentMapper.tryOccupyForPlan(equipmentId, planNo, operationCode, operationName,
                startTime != null ? startTime : effectiveRecordTime, operatorName, effectiveRecordTime) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseEquipment(Long equipmentId, LocalDateTime endTime, String operatorName, LocalDateTime recordTime) {
        if (equipmentId == null) {
            return;
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return;
        }
        HcEquipmentDO updateObj = new HcEquipmentDO();
        updateObj.setId(equipmentId);
        updateObj.setWorkStatus(WORK_STATUS_IDLE);
        updateObj.setCurrentEndTime(endTime);
        updateObj.setCurrentOperatorName(operatorName);
        updateObj.setCurrentRecordTime(recordTime != null ? recordTime : LocalDateTime.now());
        hcEquipmentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearEquipmentBinding(Long equipmentId, String workStatus, String operatorName, LocalDateTime recordTime) {
        if (equipmentId == null) {
            return;
        }
        HcEquipmentDO equipment = hcEquipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return;
        }
        HcEquipmentDO updateObj = new HcEquipmentDO();
        updateObj.setId(equipmentId);
        updateObj.setWorkStatus(StringUtils.hasText(workStatus) ? workStatus : WORK_STATUS_IDLE);
        updateObj.setCurrentOperatorName(operatorName);
        updateObj.setCurrentRecordTime(recordTime != null ? recordTime : LocalDateTime.now());
        clearBindingFields(updateObj);
        hcEquipmentMapper.updateById(updateObj);
    }

    private void clearBindingFields(HcEquipmentDO updateObj) {
        updateObj.setCurrentPlanNo("");
        updateObj.setCurrentOperationCode("");
        updateObj.setCurrentOperationName("");
        updateObj.setCurrentStartTime(null);
        updateObj.setCurrentEndTime(null);
    }

}
