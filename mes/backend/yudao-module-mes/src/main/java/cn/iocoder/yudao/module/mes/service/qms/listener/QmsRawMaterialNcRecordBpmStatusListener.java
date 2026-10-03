package cn.iocoder.yudao.module.mes.service.qms.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 原物料不合格处置单流程状态监听器。
 */
@Component
public class QmsRawMaterialNcRecordBpmStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String PROCESS_KEY = "qms_raw_material_ncr_disposition";

    @Resource
    private QmsNcRecordService qmsNcRecordService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        qmsNcRecordService.syncBpmProcessStatus(event.getBusinessKey(), event.getId(), event.getStatus(),
                event.getReason());
    }

}
