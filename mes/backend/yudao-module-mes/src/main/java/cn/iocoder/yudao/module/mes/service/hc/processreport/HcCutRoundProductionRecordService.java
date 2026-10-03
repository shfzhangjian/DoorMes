package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordInitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundProductionRecordDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcCutRoundProductionRecordService {

    Long create(HcCutRoundProductionRecordSaveReqVO reqVO);
    void update(HcCutRoundProductionRecordSaveReqVO reqVO);
    void delete(Long id);
    void deleteByIds(List<Long> ids);
    Integer confirm(HcCutRoundProductionRecordConfirmReqVO reqVO);
    HcCutRoundProductionRecordDO get(Long id);
    PageResult<HcCutRoundProductionRecordDO> getPage(HcCutRoundProductionRecordPageReqVO reqVO);
    List<HcCutRoundProductionRecordExcelVO> buildExportList(HcCutRoundProductionRecordPageReqVO reqVO);
    HcCutRoundProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException;
    HcCutRoundProductionRecordInitRespVO initializeFromReports(HcCutRoundProductionRecordPageReqVO reqVO);
}
