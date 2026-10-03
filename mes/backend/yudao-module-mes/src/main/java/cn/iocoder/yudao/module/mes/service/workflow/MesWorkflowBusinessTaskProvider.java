package cn.iocoder.yudao.module.mes.service.workflow;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;

import java.util.List;

/**
 * MES 业务流程任务扩展点。
 *
 * <p>BPM 只负责 Flowable 原生任务；业务会签、委托代办等虚拟任务由 MES Provider 转换成工作台兼容行。</p>
 */
public interface MesWorkflowBusinessTaskProvider {

    List<BpmTaskRespVO> getTodoTasks(Long userId, BpmTaskPageReqVO pageReqVO);

    List<BpmTaskRespVO> getDoneTasks(Long userId, BpmTaskPageReqVO pageReqVO);

}
