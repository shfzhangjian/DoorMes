package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardChangeLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import jakarta.validation.Valid;
import java.util.List;

public interface QmsQualityStandardService {

    Long createQualityStandard(@Valid QmsQualityStandardSaveReqVO createReqVO);

    Long createQualityStandard(@Valid QmsQualityStandardSaveReqVO createReqVO, String fixedApplyType);

    void updateQualityStandard(@Valid QmsQualityStandardSaveReqVO updateReqVO);

    void updateQualityStandard(@Valid QmsQualityStandardSaveReqVO updateReqVO, String fixedApplyType);

    void auditQualityStandard(@Valid QmsQualityStandardAuditReqVO reqVO);

    void auditQualityStandard(@Valid QmsQualityStandardAuditReqVO reqVO, String fixedApplyType);

    void deleteQualityStandard(Long id);

    void deleteQualityStandard(Long id, String fixedApplyType);

    void deleteQualityStandardList(List<Long> ids);

    void deleteQualityStandardList(List<Long> ids, String fixedApplyType);

    QmsQualityStandardDO getQualityStandard(Long id);

    QmsQualityStandardRespVO getQualityStandardResp(Long id);

    QmsQualityStandardRespVO getQualityStandardResp(Long id, String fixedApplyType);

    PageResult<QmsQualityStandardDO> getQualityStandardPage(QmsQualityStandardPageReqVO pageReqVO);

    PageResult<QmsQualityStandardDO> getQualityStandardPage(QmsQualityStandardPageReqVO pageReqVO, String fixedApplyType);

    List<QmsQualityStandardChangeLogRespVO> getQualityStandardChangeLogs(Long standardId, String applyType);
}
