package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSheetTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface QmsFqcService {

    Long createFqc(QmsFqcSaveReqVO createReqVO);

    void updateFqc(QmsFqcSaveReqVO updateReqVO);

    void submitFqc(QmsFqcSaveReqVO submitReqVO);

    void suspendFqc(Long id);

    void deleteFqc(Long id);

    void deleteFqcList(List<Long> ids);

    QmsFqcRespVO getFqcResp(Long id);

    PageResult<QmsFqcOrderDO> getFqcPage(QmsFqcPageReqVO pageReqVO);

    List<QmsFqcRespVO> getPendingFqcList();

    List<QmsDefectCodeRespVO> getFqcDefectCodeOptions();

    QmsFqcStandardRespVO getFqcStandard(String materialCode);

    QmsFqcScanRespVO resolveScan(QmsFqcScanReqVO scanReqVO);

    List<QmsFqcSheetTemplateRespVO> getSheetTemplateList(String productModel);

    QmsFqcSheetTemplateRespVO getSheetTemplate(Long id);

    QmsFqcRespVO selectSheetTemplate(Long id, Long templateId);

    QmsFqcRespVO saveSheetEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFqcRespVO saveProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFqcRespVO recalculateProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFqcRespVO auditProgramEntryItem(QmsFqcSaveReqVO saveReqVO);

    QmsFqcRespVO submitProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFqcRespVO auditProgramEntry(QmsFqcAuditReqVO auditReqVO);

    QmsFqcRespVO returnProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsFqcImportRespVO importOriginSheet(Long id, Long templateId, String mode, MultipartFile file) throws IOException;

    List<QmsFqcItemImportExcelVO> buildItemImportTemplate(Long id);

    byte[] buildItemImportTemplateExcel(Long id) throws IOException;

    List<QmsFqcItemImportExcelVO> buildItemExportRows(Long id);

    byte[] buildItemExportExcel(Long id) throws IOException;

    QmsFqcImportRespVO previewItemImport(Long id, MultipartFile file) throws IOException;

    QmsFqcImportRespVO confirmItemImport(Long id, Boolean allowOverwrite, MultipartFile file) throws IOException;
}
