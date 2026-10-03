package cn.iocoder.yudao.module.mes.service.hc.equipmentconsumable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStateImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcEquipmentConsumableService {

    PageResult<HcEquipmentConsumableStateDO> getStatePage(HcEquipmentConsumableStatePageReqVO reqVO);

    PageResult<HcEquipmentConsumableEventDO> getEventPage(HcEquipmentConsumableEventPageReqVO reqVO);

    HcEquipmentConsumableStateDO getState(Long id);

    Long adjust(HcEquipmentConsumableAdjustReqVO reqVO);

    List<HcEquipmentConsumableStateImportExcelVO> buildStateExportList(HcEquipmentConsumableStatePageReqVO reqVO);

    HcEquipmentConsumableStateImportRespVO importStateExcel(MultipartFile file, Boolean confirmClear) throws IOException;
}
