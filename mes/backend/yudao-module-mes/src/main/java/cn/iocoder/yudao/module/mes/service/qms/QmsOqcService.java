package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPendingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import java.util.List;

public interface QmsOqcService {

    Long createOqc(QmsOqcSaveReqVO createReqVO);

    Long createOqcFromShippingNotice(Long shippingNoticeItemId, Long standardId);

    Long createOqcFromShippingNoticeId(Long shippingNoticeId, Long standardId);

    void updateOqc(QmsOqcSaveReqVO updateReqVO);

    void deleteOqc(Long id);

    void deleteOqcList(List<Long> ids);

    void suspendOqc(Long id);

    QmsOqcRespVO getOqcResp(Long id);

    PageResult<QmsOqcOrderDO> getOqcPage(QmsOqcPageReqVO pageReqVO);

    List<QmsOqcPendingRespVO> getPendingOqcList(String keyword);

    QmsOqcScanRespVO resolveScan(QmsOqcScanReqVO scanReqVO);

    QmsOqcStandardRespVO getOqcStandard(String materialCode, String modelCode, Long standardId);

    QmsOqcRespVO saveProgramEntry(QmsOqcSaveReqVO saveReqVO);

    QmsOqcRespVO recalculateProgramEntry(QmsOqcSaveReqVO saveReqVO);

    QmsOqcRespVO submitProgramEntry(QmsOqcSaveReqVO saveReqVO);

    QmsOqcRespVO auditProgramEntry(QmsOqcAuditReqVO auditReqVO);
}
