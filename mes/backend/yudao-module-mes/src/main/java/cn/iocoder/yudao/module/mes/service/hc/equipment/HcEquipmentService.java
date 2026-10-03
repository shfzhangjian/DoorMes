package cn.iocoder.yudao.module.mes.service.hc.equipment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentWorkStateAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;

import java.time.LocalDateTime;
import java.util.List;

public interface HcEquipmentService {
    Long createHcEquipment(HcEquipmentSaveReqVO createReqVO);
    void updateHcEquipment(HcEquipmentSaveReqVO updateReqVO);
    void deleteHcEquipment(Long id);
    void deleteHcEquipmentListByIds(List<Long> ids);
    HcEquipmentDO getHcEquipment(Long id);
    List<HcEquipmentDO> getHcEquipmentSimpleList();
    List<HcEquipmentDO> getHcEquipmentList(HcEquipmentPageReqVO reqVO);
    PageResult<HcEquipmentDO> getHcEquipmentPage(HcEquipmentPageReqVO pageReqVO);
    void adjustWorkState(HcEquipmentWorkStateAdjustReqVO reqVO);
    void occupyEquipment(Long equipmentId, String planNo, String operationCode, String operationName,
                         LocalDateTime startTime, String operatorName, LocalDateTime recordTime);
    boolean tryOccupyEquipmentStrict(Long equipmentId, String planNo, String operationCode, String operationName,
                                     LocalDateTime startTime, String operatorName, LocalDateTime recordTime);
    void releaseEquipment(Long equipmentId, LocalDateTime endTime, String operatorName, LocalDateTime recordTime);
    void clearEquipmentBinding(Long equipmentId, String workStatus, String operatorName, LocalDateTime recordTime);
}
