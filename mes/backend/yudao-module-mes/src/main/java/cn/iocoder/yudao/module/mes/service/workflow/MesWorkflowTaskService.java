package cn.iocoder.yudao.module.mes.service.workflow;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;

public interface MesWorkflowTaskService {

    PageResult<BpmTaskRespVO> getTodoPage(BpmTaskPageReqVO pageReqVO);

    PageResult<BpmTaskRespVO> getDonePage(BpmTaskPageReqVO pageReqVO);

}
