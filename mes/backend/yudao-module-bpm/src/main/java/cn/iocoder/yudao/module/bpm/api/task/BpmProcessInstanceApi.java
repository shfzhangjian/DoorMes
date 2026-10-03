package cn.iocoder.yudao.module.bpm.api.task;

import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 流程实例 Api 接口
 *
 * @author 芋道源码
 */
public interface BpmProcessInstanceApi {

    /**
     * 创建流程实例（提供给内部）
     *
     * @param userId 用户编号
     * @param reqDTO 创建信息
     * @return 实例的编号
     */
    String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO);

    /**
     * 获得流程实例状态
     *
     * @param id 流程实例编号
     * @return 流程实例状态
     */
    Integer getProcessInstanceStatus(String id);

    /**
     * 获得流程实例变量。
     *
     * @param id 流程实例编号
     * @return 流程变量
     */
    Map<String, Object> getProcessInstanceVariables(String id);

    /**
     * 更新运行中流程实例的变量。
     *
     * @param id 流程实例编号
     * @param variables 流程变量
     */
    void updateProcessInstanceVariables(String id, Map<String, Object> variables);

}
