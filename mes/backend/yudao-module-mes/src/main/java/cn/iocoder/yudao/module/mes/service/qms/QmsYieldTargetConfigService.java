package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldTargetConfigSaveReqVO;
import java.util.List;

public interface QmsYieldTargetConfigService {

    Long createTargetConfig(QmsYieldTargetConfigSaveReqVO createReqVO);

    void updateTargetConfig(QmsYieldTargetConfigSaveReqVO updateReqVO);

    void deleteTargetConfig(Long id);

    QmsYieldTargetConfigRespVO getTargetConfig(Long id);

    PageResult<QmsYieldTargetConfigRespVO> getTargetConfigPage(QmsYieldTargetConfigPageReqVO pageReqVO);

    List<QmsYieldTargetConfigRespVO> getSimpleList();

    List<QmsYieldTargetConfigExcelVO> getExportList(QmsYieldTargetConfigPageReqVO reqVO);
}
