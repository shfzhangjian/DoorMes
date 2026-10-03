// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.prod.ProdCheckService.java
package cn.iocoder.yudao.module.mes.service.prod;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.prod.ProdCheckDO;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdCheckSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdCheckPageReqVO;

import jakarta.validation.Valid;

public interface ProdCheckService {
    Long createProdCheck(@Valid ProdCheckSaveReqVO createReqVO);
    ProdCheckDO getProdCheck(Long id);
    PageResult<ProdCheckDO> getProdCheckPage(ProdCheckPageReqVO pageReqVO);
}
