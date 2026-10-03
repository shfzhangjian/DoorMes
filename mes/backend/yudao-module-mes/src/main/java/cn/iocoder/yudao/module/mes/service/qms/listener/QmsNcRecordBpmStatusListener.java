package cn.iocoder.yudao.module.mes.service.qms.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * NCR 流程状态监听器。
 */
@Component
public class QmsNcRecordBpmStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String PROCESS_KEY = "qms_ncr_disposition";

    @Resource
    private QmsNcRecordService qmsNcRecordService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        qmsNcRecordService.syncBpmProcessStatus(event.getBusinessKey(), event.getId(), event.getStatus(), event.getReason());
    }

}
