package cn.iocoder.yudao.module.mes.service.qms.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.mes.service.qms.QmsDispatchTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 质量任务中心流程状态监听器。
 */
@Component
public class QmsDispatchTaskBpmStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String PROCESS_KEY = "qms_quality_dispatch_task";

    @Resource
    private QmsDispatchTaskService dispatchTaskService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        dispatchTaskService.syncBpmProcessStatus(event.getBusinessKey(), event.getId(), event.getStatus(),
                event.getReason());
    }
}
