package cn.iocoder.yudao.module.mes.service.hc.intermediatestockledger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockHistoryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockHistoryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo.HcIntermediateStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.intermediatestockledger.HcIntermediateStockLedgerDO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface HcIntermediateStockLedgerService {

    PageResult<HcIntermediateStockLedgerDO> getIntermediateStockLedgerPage(HcIntermediateStockLedgerPageReqVO pageReqVO);

    PageResult<HcIntermediateStockLedgerDO> getIntermediateStockLedgerAggregatePage(HcIntermediateStockLedgerPageReqVO pageReqVO);

    List<HcIntermediateStockHistoryImportExcelVO> buildHistoryImportTemplate(HcIntermediateStockLedgerPageReqVO pageReqVO);

    HcIntermediateStockHistoryImportRespVO importHistoryStock(MultipartFile file) throws IOException;

}
