package cn.iocoder.yudao.module.mes.service.hc.terminal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo.HcTerminalSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.terminal.HcTerminalDO;

import java.util.List;

public interface HcTerminalService {
    Long createHcTerminal(HcTerminalSaveReqVO createReqVO);
    void updateHcTerminal(HcTerminalSaveReqVO updateReqVO);
    void deleteHcTerminal(Long id);
    void deleteHcTerminalListByIds(List<Long> ids);
    HcTerminalDO getHcTerminal(Long id);
    List<HcTerminalDO> getHcTerminalSimpleList();
    List<HcTerminalDO> getHcTerminalList(HcTerminalPageReqVO reqVO);
    PageResult<HcTerminalDO> getHcTerminalPage(HcTerminalPageReqVO pageReqVO);
}