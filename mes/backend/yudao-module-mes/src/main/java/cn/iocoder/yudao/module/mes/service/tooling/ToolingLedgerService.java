// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.tooling.ToolingLedgerService.java
package cn.iocoder.yudao.module.mes.service.tooling;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.tooling.ToolingLedgerDO;
import cn.iocoder.yudao.module.mes.controller.admin.tooling.vo.ToolingLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.tooling.vo.ToolingLedgerPageReqVO;

import jakarta.validation.Valid;

public interface ToolingLedgerService {
    Long createToolingLedger(@Valid ToolingLedgerSaveReqVO createReqVO);
    void updateToolingLedger(@Valid ToolingLedgerSaveReqVO updateReqVO);
    void deleteToolingLedger(Long id);
    ToolingLedgerDO getToolingLedger(Long id);
    PageResult<ToolingLedgerDO> getToolingLedgerPage(ToolingLedgerPageReqVO pageReqVO);

    /** 核心防呆：校验工装寿命 */
    void validateToolingLife(String toolingCode);
}
