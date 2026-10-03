package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionExecutionRespVO;
import jakarta.validation.Valid;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionRespVO;

public interface QmsNcDispositionService {

    QmsNcClosedCorrectionRespVO getClosedCorrection(Long id);

    QmsNcClosedCorrectionRespVO previewClosedCorrection(@Valid QmsNcClosedCorrectionReqVO reqVO);

    void saveClosedCorrection(@Valid QmsNcClosedCorrectionReqVO reqVO);

    QmsNcDispositionContextRespVO getDispositionContext(Long ncRecordId);

    QmsNcDispositionExecutionRespVO confirmDispositionScope(@Valid QmsNcDispositionConfirmReqVO reqVO);
}
