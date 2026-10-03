// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/execution/MesExecutionService.java
package cn.iocoder.yudao.module.mes.service.execution;

import cn.iocoder.yudao.module.mes.controller.admin.execution.vo.MesActionExecuteReqVO;

/**
 * MES 生产执行 Service 接口
 * 负责处理现场作业的所有动作交互 (SOP执行、报工、投料、质检等)
 */
public interface MesExecutionService {

    /**
     * 执行生产动作
     * <p>
     * 核心逻辑包括：
     * 1. 规则校验 (Q-Time, BOM等)
     * 2. 数据分流 (写入投料表、质检表等)
     * 3. 异常处理 (QMS阻断)
     * 4. 记录动作实绩快照
     *
     * @param req 动作执行请求参数 (含工单上下文、动作ID、表单数据)
     */
    void executeAction(MesActionExecuteReqVO req);

}
