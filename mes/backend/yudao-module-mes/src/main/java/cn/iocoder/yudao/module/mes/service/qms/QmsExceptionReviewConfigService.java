package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcReviewConfigSaveReqVO;
import java.util.List;

public interface QmsExceptionReviewConfigService {

    Long createReviewConfig(QmsNcReviewConfigSaveReqVO createReqVO);

    void updateReviewConfig(QmsNcReviewConfigSaveReqVO updateReqVO);

    void deleteReviewConfig(Long id);

    QmsNcReviewConfigRespVO getReviewConfig(Long id);

    PageResult<QmsNcReviewConfigRespVO> getReviewConfigPage(QmsNcReviewConfigPageReqVO pageReqVO);

    List<QmsNcReviewConfigRespVO> getSimpleList();
}
