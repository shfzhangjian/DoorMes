package cn.iocoder.yudao.module.mes.service.hc.pressslotspare;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSparePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcPressSlotSpareService {

    PageResult<HcPressSlotSpareDO> getPage(HcPressSlotSparePageReqVO reqVO);

    PageResult<HcPressSlotSpareRecordDO> getRecordPage(HcPressSlotSpareRecordPageReqVO reqVO);

    HcPressSlotSpareDO get(Long id);

    Long save(HcPressSlotSpareSaveReqVO reqVO);

    String saveRndConsume(HcPressSlotSpareRndConsumeSaveReqVO reqVO);

    List<HcPressSlotSpareImportExcelVO> buildExportList(HcPressSlotSparePageReqVO reqVO);

    HcPressSlotSpareImportRespVO importExcel(MultipartFile file, Boolean confirmClear) throws IOException;
}
