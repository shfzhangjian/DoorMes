package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionDestroyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRetentionExpireTimeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardSelectReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface QmsIqcService {

    Long createIqc(@Valid QmsIqcSaveReqVO createReqVO);

    void updateIqc(@Valid QmsIqcSaveReqVO updateReqVO);

    void deleteIqc(Long id);

    void deleteIqcList(List<Long> ids);

    void submitIqc(@Valid QmsIqcSubmitReqVO submitReqVO);

    void auditIqc(@Valid QmsIqcAuditReqVO auditReqVO);

    QmsIqcRespVO saveProgramEntry(QmsIqcSaveReqVO saveReqVO);

    QmsIqcRespVO recalculateProgramEntry(QmsIqcSaveReqVO saveReqVO);

    QmsIqcRespVO submitProgramEntry(QmsIqcSaveReqVO saveReqVO);

    QmsIqcScanRespVO resolveScan(QmsIqcScanReqVO scanReqVO);

    byte[] buildItemImportTemplateExcel(Long id) throws IOException;

    byte[] buildItemExportExcel(Long id) throws IOException;

    byte[] buildCoaWord(Long id) throws IOException;

    QmsIqcImportRespVO previewItemImport(Long id, MultipartFile file) throws IOException;

    QmsIqcImportRespVO confirmItemImport(Long id, Boolean allowOverwrite, MultipartFile file) throws IOException;

    void suspendIqc(Long id);

    QmsIqcOrderDO getIqc(Long id);

    QmsIqcRespVO getIqcResp(Long id);

    PageResult<QmsIqcOrderDO> getIqcPage(QmsIqcPageReqVO pageReqVO);

    QmsIqcRespVO getIqcRetentionResp(Long id);

    QmsIqcRespVO confirmIqcRetention(@Valid QmsIqcRetentionConfirmReqVO reqVO);

    QmsIqcRespVO updateIqcRetentionExpireTime(QmsIqcRetentionExpireTimeReqVO reqVO);

    void destroyIqcRetention(QmsIqcRetentionDestroyReqVO reqVO);

    List<QmsIqcRespVO> getPendingIqcList();

    QmsIqcStandardRespVO getIqcStandardByMaterial(String materialCode);

    List<QmsInspectionStandardCandidateRespVO> getStandardCandidatesByMaterial(Long materialId, String materialCode);

    List<QmsInspectionStandardCandidateRespVO> getStandardCandidates(Long id);

    QmsIqcRespVO selectStandard(@Valid QmsInspectionStandardSelectReqVO reqVO);
}
