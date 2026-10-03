package cn.iocoder.yudao.module.mes.service.qms.listener;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.api.event.BpmTaskTransferEvent;
import cn.iocoder.yudao.module.mes.service.qms.QmsExceptionEventService;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 异常事件提报流程任务转办监听器。
 */
@Component
public class QmsExceptionEventBpmTaskTransferListener implements ApplicationListener<BpmTaskTransferEvent> {

    private static final String PROCESS_KEY = "qms_exception_event";
    private static final String TASK_KEY_QUALITY_CONFIRM = "quality_confirm";

    @Resource
    private QmsExceptionEventService qmsExceptionEventService;

    @Override
    public void onApplicationEvent(BpmTaskTransferEvent event) {
        if (!StrUtil.equals(event.getProcessDefinitionKey(), PROCESS_KEY)
                || !StrUtil.equals(event.getTaskDefinitionKey(), TASK_KEY_QUALITY_CONFIRM)) {
            return;
        }
        qmsExceptionEventService.syncConfirmTaskTransfer(event.getBusinessKey(), event.getProcessInstanceId(),
                event.getTaskId(), event.getFromUserId(), event.getToUserId(), event.getReason());
    }

}
