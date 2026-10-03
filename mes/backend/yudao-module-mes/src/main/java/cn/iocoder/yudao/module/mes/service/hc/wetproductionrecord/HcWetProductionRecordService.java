package cn.iocoder.yudao.module.mes.service.hc.wetproductionrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordInitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordInitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.wetproductionrecord.HcWetProductionRecordDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcWetProductionRecordService {

    Long create(HcWetProductionRecordSaveReqVO reqVO);

    void update(HcWetProductionRecordSaveReqVO reqVO);

    void delete(Long id);

    void deleteByIds(List<Long> ids);

    Integer confirm(HcWetProductionRecordConfirmReqVO reqVO);

    HcWetProductionRecordDO get(Long id);

    PageResult<HcWetProductionRecordDO> getPage(HcWetProductionRecordPageReqVO reqVO);

    PageResult<HcWetProductionRecordRespVO> getReportPage(HcWetProductionRecordPageReqVO reqVO);

    List<HcWetProductionRecordImportExcelVO> buildExportList(HcWetProductionRecordPageReqVO reqVO);

    List<HcWetProductionRecordImportExcelVO> buildReportExportList(HcWetProductionRecordPageReqVO reqVO);

    HcWetProductionRecordImportRespVO importExcel(MultipartFile file) throws IOException;

    HcWetProductionRecordInitRespVO initializeFromReports(HcWetProductionRecordInitReqVO reqVO);
}
