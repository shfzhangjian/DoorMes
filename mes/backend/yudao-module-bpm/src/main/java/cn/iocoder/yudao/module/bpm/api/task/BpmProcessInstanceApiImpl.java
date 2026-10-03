package cn.iocoder.yudao.module.bpm.api.task;

import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.Map;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author 芋道源码
 * @author jason
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Override
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return processInstanceService.createProcessInstance(userId, reqDTO);
    }

    @Override
    public Integer getProcessInstanceStatus(String id) {
        HistoricProcessInstance processInstance = processInstanceService.getHistoricProcessInstance(id);
        return processInstance == null ? null : FlowableUtils.getProcessInstanceStatus(processInstance);
    }

    @Override
    public Map<String, Object> getProcessInstanceVariables(String id) {
        HistoricProcessInstance processInstance = processInstanceService.getHistoricProcessInstance(id);
        return processInstance == null || processInstance.getProcessVariables() == null
                ? Collections.emptyMap() : processInstance.getProcessVariables();
    }

    @Override
    public void updateProcessInstanceVariables(String id, Map<String, Object> variables) {
        processInstanceService.updateProcessInstanceVariables(id, variables);
    }

}
