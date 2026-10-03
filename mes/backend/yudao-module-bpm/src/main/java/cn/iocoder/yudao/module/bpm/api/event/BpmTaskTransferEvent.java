package cn.iocoder.yudao.module.bpm.api.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * 流程任务转办事件。
 */
@SuppressWarnings("ALL")
@Data
public class BpmTaskTransferEvent extends ApplicationEvent {

    /**
     * 任务编号
     */
    @NotNull(message = "任务编号不能为空")
    private String taskId;
    /**
     * 流程实例编号
     */
    @NotNull(message = "流程实例编号不能为空")
    private String processInstanceId;
    /**
     * 流程定义 Key
     */
    private String processDefinitionKey;
    /**
     * 任务定义 Key
     */
    private String taskDefinitionKey;
    /**
     * 业务标识
     */
    private String businessKey;
    /**
     * 原办理人编号
     */
    private Long fromUserId;
    /**
     * 新办理人编号
     */
    private Long toUserId;
    /**
     * 转办原因
     */
    private String reason;

    public BpmTaskTransferEvent(Object source) {
        super(source);
    }

}
