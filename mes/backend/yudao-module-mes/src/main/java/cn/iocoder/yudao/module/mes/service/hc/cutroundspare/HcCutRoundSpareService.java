package cn.iocoder.yudao.module.mes.service.hc.cutroundspare;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSparePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRndConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcCutRoundSpareService {

    PageResult<HcCutRoundSpareDO> getPage(HcCutRoundSparePageReqVO reqVO);

    PageResult<HcCutRoundSpareRecordDO> getRecordPage(HcCutRoundSpareRecordPageReqVO reqVO);

    HcCutRoundSpareDO get(Long id);

    Long save(HcCutRoundSpareSaveReqVO reqVO);

    String saveRndConsume(HcCutRoundSpareRndConsumeSaveReqVO reqVO);

    List<HcCutRoundSpareImportExcelVO> buildExportList(HcCutRoundSparePageReqVO reqVO);

    HcCutRoundSpareImportRespVO importStateExcel(MultipartFile file, Boolean confirmClear) throws IOException;
}
