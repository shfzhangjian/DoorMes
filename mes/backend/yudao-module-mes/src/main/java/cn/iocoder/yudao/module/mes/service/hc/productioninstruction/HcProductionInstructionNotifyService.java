package cn.iocoder.yudao.module.mes.service.hc.productioninstruction;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 生产指令采用工序看板拉取模式，不再发送个人站内信。
 */
@Service
@Slf4j
public class HcProductionInstructionNotifyService {

    public void sendIssueNotify(HcProductionInstructionDO instruction) {
        log.debug("[sendIssueNotify][生产指令工序消息模式，不发送个人站内信，instructionNo={}]",
                instruction == null ? null : instruction.getInstructionNo());
    }

}
