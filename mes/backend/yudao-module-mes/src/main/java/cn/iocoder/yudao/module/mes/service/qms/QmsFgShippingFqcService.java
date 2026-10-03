package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentNoticeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcPendingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcShippingDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import java.util.List;

public interface QmsFgShippingFqcService {

    List<QmsFgShippingFqcPendingRespVO> getPendingList(String keyword);

    QmsFgShippingFqcRespVO createFromShippingNotice(Long shippingNoticeId);

    PageResult<QmsFgShippingFqcRespVO> getPage(QmsFgShippingFqcPageReqVO pageReqVO);

    QmsFgShippingFqcRespVO get(Long id);

    List<QmsDefectCodeRespVO> getDefectCodeOptions();

    QmsFgShippingFqcRespVO saveProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFgShippingFqcRespVO submitProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFgShippingFqcRespVO saveShippingDetails(QmsFgShippingFqcShippingDetailSaveReqVO reqVO);

    List<QmsFgShippingAlignmentNoticeRespVO> getAlignmentNoticeList(String keyword);

    PageResult<QmsFgShippingAlignmentNoticeRespVO> getAlignmentNoticePage(QmsFgShippingAlignmentPageReqVO pageReqVO);

    QmsFgShippingAlignmentRespVO getAlignment(Long shippingNoticeId);

    QmsFgShippingAlignmentRespVO saveShippingAlignment(QmsFgShippingAlignmentSaveReqVO reqVO);

    QmsFgShippingFqcRespVO audit(QmsFqcAuditReqVO auditReqVO);

    QmsFgShippingFqcScanRespVO resolveScan(QmsFqcScanReqVO scanReqVO);
}
