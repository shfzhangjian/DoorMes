package cn.iocoder.yudao.module.mes.service.qms.coa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.FaiStandardResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportActionReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportAuditReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportCorrectionReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportGenerateReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.MotherBatchPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.MotherBatchResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueApplyReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueCandidatePageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportItemValueCandidateResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.StandardItemPageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.StandardItemResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateAuditReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplatePageReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateResp;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateSaveReq;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplateStatusReq;
import java.util.List;

public interface QmsCoaService {

    PageResult<TemplateResp> getTemplatePage(TemplatePageReq reqVO);

    TemplateResp getTemplate(Long id);

    Long createTemplate(TemplateSaveReq reqVO);

    void updateTemplate(TemplateSaveReq reqVO);

    void submitTemplate(Long id);

    void auditTemplate(TemplateAuditReq reqVO);

    void changeTemplateStatus(TemplateStatusReq reqVO);

    Long upgradeTemplate(Long id);

    List<FaiStandardResp> getFaiStandardList(String keyword);

    PageResult<StandardItemResp> getStandardItemPage(StandardItemPageReq reqVO);

    PageResult<ReportResp> getReportPage(ReportPageReq reqVO);

    PageResult<MotherBatchResp> getMotherBatchPage(MotherBatchPageReq reqVO);

    ReportResp getReport(Long id);

    Long generateReport(ReportGenerateReq reqVO);

    void refreshReport(Long id);

    void correctReportItem(ReportCorrectionReq reqVO);

    PageResult<ReportItemValueCandidateResp> getReportItemValueCandidatePage(
            ReportItemValueCandidatePageReq reqVO);

    void applyReportItemValue(ReportItemValueApplyReq reqVO);

    void submitReport(Long id);

    void confirmReport(ReportAuditReq reqVO);

    void auditReport(ReportAuditReq reqVO);

    void issueReport(ReportActionReq reqVO);

    Long createRevision(ReportActionReq reqVO);

    void voidReport(ReportActionReq reqVO);

}
