// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qms/MesQmsService.java
package cn.iocoder.yudao.module.mes.service.qms;

public interface MesQmsService {
    /**
     * 创建不合格记录 (NCR)
     * @param subOrderId 派工单ID
     * @param actionId 触发动作ID
     * @param reason 不良原因
     */
    void createNcRecord(Long subOrderId, Long actionId, String reason);
}
