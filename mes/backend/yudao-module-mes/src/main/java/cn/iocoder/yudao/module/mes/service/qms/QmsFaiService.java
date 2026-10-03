package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiBindStandardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRetentionDestroyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRetentionExpireTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRecheckAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSheetTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiStandardItemCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface QmsFaiService {

    Long createFai(@Valid QmsFaiSaveReqVO createReqVO);

    Long createFaiForInspectionPush(@Valid QmsFaiSaveReqVO createReqVO);

    QmsFaiRespVO bindStandard(@Valid QmsFaiBindStandardReqVO bindReqVO);

    void updateFai(@Valid QmsFaiSaveReqVO updateReqVO);

    void deleteFai(Long id);

    void deleteFaiList(List<Long> ids);

    void operatorSubmit(@Valid QmsFaiSubmitReqVO submitReqVO);

    void qaSubmit(@Valid QmsFaiSubmitReqVO submitReqVO);

    void suspendFai(Long id);

    QmsFaiOrderDO getFai(Long id);

    QmsFaiRespVO getFaiResp(Long id);

    QmsFaiRespVO getFaiRetentionResp(Long id);

    QmsFaiRespVO updateFaiRetentionExpireTime(@Valid QmsFaiRetentionExpireTimeReqVO reqVO);

    void destroyFaiRetention(@Valid QmsFaiRetentionDestroyReqVO reqVO);

    PageResult<QmsFaiOrderDO> getFaiPage(QmsFaiPageReqVO pageReqVO);

    List<QmsFaiRespVO> getPendingFaiList();

    List<QmsFaiRespVO> getPendingFaiListBySourceModule(String sourceModule);

    QmsFaiStandardRespVO getFaiStandard(String materialCode, String operationCode, String operationName);

    List<QmsFaiSheetTemplateRespVO> getSheetTemplateList(String productModel);

    QmsFaiSheetTemplateRespVO getSheetTemplate(Long id);

    QmsFaiRespVO selectSheetTemplate(Long id, Long templateId);

    QmsFaiRespVO saveSheetEntry(@Valid QmsFaiSaveReqVO saveReqVO);

    QmsFaiRespVO saveProgramEntry(QmsFaiSaveReqVO saveReqVO);

    QmsFaiRespVO recalculateProgramEntry(QmsFaiSaveReqVO saveReqVO);

    QmsFaiRespVO auditProgramEntryItem(QmsFaiSaveReqVO saveReqVO);

    QmsFaiRespVO submitProgramEntry(QmsFaiSaveReqVO saveReqVO);

    QmsFaiRespVO auditProgramEntry(QmsFaiAuditReqVO auditReqVO);

    QmsFaiRespVO oneClickPass(Long id);

    QmsFaiRespVO oneClickFail(Long id);

    QmsFaiRespVO createRecheckFai(Long id);

    QmsFaiRecheckApplyRespVO applyRecheckFai(@Valid QmsFaiRecheckApplyReqVO reqVO);

    QmsFaiRecheckApplyRespVO auditRecheckFai(@Valid QmsFaiRecheckAuditReqVO reqVO);

    QmsFaiRespVO returnProgramEntry(QmsFaiSaveReqVO saveReqVO);

    QmsFaiImportRespVO importOriginSheet(Long id, Long templateId, String mode, MultipartFile file) throws IOException;

    List<QmsFaiItemImportExcelVO> buildItemImportTemplate(Long id);

    byte[] buildItemImportTemplateExcel(Long id) throws IOException;

    List<QmsFaiItemImportExcelVO> buildItemExportRows(Long id);

    byte[] buildItemExportExcel(Long id) throws IOException;

    QmsFaiImportRespVO previewItemImport(Long id, MultipartFile file) throws IOException;

    QmsFaiImportRespVO confirmItemImport(Long id, Boolean allowOverwrite, MultipartFile file) throws IOException;

    QmsFaiScanRespVO resolveScan(QmsFaiScanReqVO scanReqVO);

    List<QmsInspectionStandardCandidateRespVO> getStandardCandidates(Long id);

    List<QmsFaiStandardItemCandidateRespVO> getStandardItemCandidates(Long id, Long standardId);

    QmsFaiRespVO selectStandard(@Valid QmsInspectionStandardSelectReqVO reqVO);
}
