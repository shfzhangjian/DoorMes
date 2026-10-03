package cn.iocoder.yudao.module.mes.service.qms;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * QMS 审核待办站内信通知服务。
 */
public interface QmsAuditTodoNotifyService {

    /**
     * 向质量主管发送审核待办通知。已存在去重标记时不重复发送；发送或标记失败只记录日志，不阻断主流程。
     *
     * @param auditNotifyTime 已发送通知时间
     * @param bizType 业务类型
     * @param bizNo 业务单号
     * @param bizName 业务名称
     * @param auditTip 审核提示
     * @param submitTime 提交时间
     * @param auditNotifyTimeUpdater 通知成功后的去重标记更新动作
     */
    void sendAuditTodoIfNeeded(LocalDateTime auditNotifyTime, String bizType, String bizNo, String bizName,
                               String auditTip, LocalDateTime submitTime,
                               Consumer<LocalDateTime> auditNotifyTimeUpdater);

}
