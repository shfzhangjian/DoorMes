package cn.iocoder.yudao.module.mes.service.hc.processanalysis;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisFieldRespVO;
import java.util.List;

public interface HcProcessAnalysisService {

    List<HcProcessAnalysisFieldRespVO> getFieldCatalog();

    List<HcProcessAnalysisConfigRespVO> getVisibleConfigList();

    HcProcessAnalysisConfigRespVO getVisibleConfig(Long id);

    Long createConfig(HcProcessAnalysisConfigSaveReqVO reqVO);

    void updateConfig(HcProcessAnalysisConfigSaveReqVO reqVO);

    void deleteConfig(Long id);

}
