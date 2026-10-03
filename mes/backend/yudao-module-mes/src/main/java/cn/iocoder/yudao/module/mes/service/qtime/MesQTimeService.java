// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qtime/MesQTimeService.java
package cn.iocoder.yudao.module.mes.service.qtime;

public interface MesQTimeService {
    /**
     * 校验 Q-Time 规则
     * @param subOrderId 当前派工单ID
     * @param currentProcessId 当前工序ID
     * @return true=通过, false=时间未到
     */
    boolean checkQTime(Long subOrderId, Long currentProcessId);
}
