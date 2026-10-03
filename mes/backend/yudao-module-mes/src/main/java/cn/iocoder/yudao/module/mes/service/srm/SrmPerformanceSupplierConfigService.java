package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigSaveReqVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmPerformanceSupplierConfigService {

    Long createConfig(@Valid SrmPerformanceSupplierConfigSaveReqVO reqVO);

    void updateConfig(@Valid SrmPerformanceSupplierConfigSaveReqVO reqVO);

    void deleteConfig(Long id);

    SrmPerformanceSupplierConfigRespVO getConfig(Long id);

    PageResult<SrmPerformanceSupplierConfigRespVO> getConfigPage(SrmPerformanceSupplierConfigPageReqVO reqVO);

    List<SrmPerformanceSupplierConfigRespVO> getEnabledConfigList();

    SrmPerformanceSupplierConfigRespVO getItemConfig(Long configId, Long templateVersionId);

    void saveItemConfig(@Valid SrmPerformanceSupplierConfigReqVO reqVO);

}
