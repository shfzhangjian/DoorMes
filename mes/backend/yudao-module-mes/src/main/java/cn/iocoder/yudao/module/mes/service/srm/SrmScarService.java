package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmScarSaveReqVO;
import jakarta.validation.Valid;

public interface SrmScarService {

    Long createScar(@Valid SrmScarSaveReqVO reqVO);

    void updateScar(@Valid SrmScarSaveReqVO reqVO);

    void deleteScar(Long id);

    SrmScarRespVO getScar(Long id);

    PageResult<SrmScarRespVO> getScarPage(SrmScarPageReqVO reqVO);

}
