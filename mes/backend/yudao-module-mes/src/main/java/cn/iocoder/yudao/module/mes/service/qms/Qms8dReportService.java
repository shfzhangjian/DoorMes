package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dActionItemDoneReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportLinkSourceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportSaveReqVO;
import java.util.List;

public interface Qms8dReportService {

    PageResult<Qms8dReportRespVO> getReportPage(Qms8dReportPageReqVO pageReqVO);

    Qms8dReportRespVO getReport(Long id);

    Long createReport(Qms8dReportSaveReqVO createReqVO);

    void updateReport(Qms8dReportSaveReqVO updateReqVO);

    void handleReport(Qms8dReportHandleReqVO handleReqVO);

    void returnReport(Qms8dReportReturnReqVO returnReqVO);

    void closeReport(Qms8dReportCloseReqVO closeReqVO);

    void linkSource(Qms8dReportLinkSourceReqVO linkReqVO);

    void actionItemDone(Qms8dActionItemDoneReqVO doneReqVO);

    List<Qms8dReportRespVO.FlowLog> getFlowLogList(Long reportId);
}
