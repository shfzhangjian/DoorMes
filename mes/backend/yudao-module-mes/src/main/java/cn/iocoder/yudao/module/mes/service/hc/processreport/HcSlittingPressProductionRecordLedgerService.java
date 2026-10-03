package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordInitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slittingpress.HcSlittingPressProductionRecordDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcSlittingPressProductionRecordLedgerService {
    Long create(HcSlittingPressProductionRecordSaveReqVO req);
    void update(HcSlittingPressProductionRecordSaveReqVO req);
    void delete(Long id);
    void deleteByIds(List<Long> ids);
    Integer confirm(HcSlittingPressProductionRecordConfirmReqVO req);
    HcSlittingPressProductionRecordDO get(Long id);
    PageResult<HcSlittingPressProductionRecordDO> getPage(HcSlittingPressProductionRecordPageReqVO req);
    List<HcSlittingPressProductionRecordExcelVO> buildExportList(HcSlittingPressProductionRecordPageReqVO req);
    HcSlittingPressProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException;
    HcSlittingPressProductionRecordInitRespVO initializeFromReports(HcSlittingPressProductionRecordPageReqVO req);
}
