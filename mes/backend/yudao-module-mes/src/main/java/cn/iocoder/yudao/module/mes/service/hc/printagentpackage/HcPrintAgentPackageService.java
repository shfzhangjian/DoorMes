package cn.iocoder.yudao.module.mes.service.hc.printagentpackage;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo.HcPrintAgentPackageSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printagentpackage.HcPrintAgentPackageDO;
import java.util.List;

public interface HcPrintAgentPackageService {

    String DEFAULT_PACKAGE_CODE = "HC_MES_PRINT_AGENT";

    Long createHcPrintAgentPackage(HcPrintAgentPackageSaveReqVO createReqVO);

    void updateHcPrintAgentPackage(HcPrintAgentPackageSaveReqVO updateReqVO);

    void deleteHcPrintAgentPackage(Long id);

    void deleteHcPrintAgentPackageListByIds(List<Long> ids);

    HcPrintAgentPackageDO getHcPrintAgentPackage(Long id);

    PageResult<HcPrintAgentPackageDO> getHcPrintAgentPackagePage(HcPrintAgentPackagePageReqVO pageReqVO);

    void publishHcPrintAgentPackage(Long id);

    HcPrintAgentPackageDO getLatestReleasedPackage(String packageCode);

}
