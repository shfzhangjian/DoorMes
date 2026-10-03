package cn.iocoder.yudao.module.mes.service.qms.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.mes.service.qms.QmsExceptionEventService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 异常事件提报流程状态监听器。
 */
@Component
public class QmsExceptionEventBpmStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String PROCESS_KEY = "qms_exception_event";

    @Resource
    private QmsExceptionEventService qmsExceptionEventService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        qmsExceptionEventService.syncBpmProcessStatus(event.getBusinessKey(), event.getId(), event.getStatus(),
                event.getReason());
    }

}
