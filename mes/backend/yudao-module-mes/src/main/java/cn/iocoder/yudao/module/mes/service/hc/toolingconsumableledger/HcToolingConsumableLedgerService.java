package cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableBalanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerMarkUsedUpReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumablePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumableSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingProcessConsumableDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcToolingConsumableLedgerService {

    Long createLedger(HcToolingConsumableLedgerSaveReqVO reqVO);

    void updateLedger(HcToolingConsumableLedgerSaveReqVO reqVO);

    void updateLedgerUsageStatus(Long id, String usageStatus);

    void markLedgerUsedUp(HcToolingConsumableLedgerMarkUsedUpReqVO reqVO);

    void returnLedger(HcToolingConsumableLedgerReturnReqVO reqVO);

    void deleteLedger(Long id);

    void deleteLedgerList(List<Long> ids);

    HcToolingConsumableLedgerDO getLedger(Long id);

    PageResult<HcToolingConsumableLedgerDO> getLedgerPage(HcToolingConsumableLedgerPageReqVO reqVO);

    PageResult<HcToolingConsumableBalanceRespVO> getBalancePage(HcToolingConsumableLedgerPageReqVO reqVO);

    Long createConsume(HcToolingConsumableConsumeSaveReqVO reqVO);

    void updateConsume(HcToolingConsumableConsumeSaveReqVO reqVO);

    void deleteConsume(Long id);

    void deleteConsumeList(List<Long> ids);

    HcToolingConsumableConsumeDO getConsume(Long id);

    PageResult<HcToolingConsumableConsumeDO> getConsumePage(HcToolingConsumableConsumePageReqVO reqVO);

    PageResult<HcToolingConsumableConsumeRespVO> getConsumeRecordPage(HcToolingConsumableConsumePageReqVO reqVO);

    List<HcToolingConsumableLedgerExcelVO> buildExportList(HcToolingConsumableLedgerPageReqVO reqVO);

    HcToolingConsumableLedgerImportRespVO importExcel(MultipartFile file, String processCode) throws IOException;

    Long createProcessConsumable(HcToolingProcessConsumableSaveReqVO reqVO);

    void updateProcessConsumable(HcToolingProcessConsumableSaveReqVO reqVO);

    void deleteProcessConsumable(Long id);

    void deleteProcessConsumableList(List<Long> ids);

    HcToolingProcessConsumableDO getProcessConsumable(Long id);

    PageResult<HcToolingProcessConsumableDO> getProcessConsumablePage(HcToolingProcessConsumablePageReqVO reqVO);

    List<HcToolingProcessConsumableDO> getEnabledProcessConsumableList(String processCode);
}
