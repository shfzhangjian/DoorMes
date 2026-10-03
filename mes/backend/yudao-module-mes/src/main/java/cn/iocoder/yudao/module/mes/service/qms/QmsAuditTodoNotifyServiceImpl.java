package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * QMS 审核待办站内信通知服务实现。
 */
@Service
@Slf4j
public class QmsAuditTodoNotifyServiceImpl implements QmsAuditTodoNotifyService {

    private static final Long QUALITY_MANAGER_ROLE_ID = 161L;
    private static final String AUDIT_TODO_NOTIFY_TEMPLATE_CODE = "MES_QMS_AUDIT_TODO";
    private static final DateTimeFormatter NOTIFY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @Override
    public void sendAuditTodoIfNeeded(LocalDateTime auditNotifyTime, String bizType, String bizNo, String bizName,
                                      String auditTip, LocalDateTime submitTime,
                                      Consumer<LocalDateTime> auditNotifyTimeUpdater) {
        if (auditNotifyTime != null) {
            return;
        }
        LocalDateTime actualSubmitTime = submitTime == null ? LocalDateTime.now() : submitTime;
        Set<Long> candidateUserIds;
        try {
            candidateUserIds = permissionApi.getUserRoleIdListByRoleIds(Collections.singleton(QUALITY_MANAGER_ROLE_ID));
        } catch (Exception ex) {
            log.warn("[sendAuditTodoIfNeeded][查询质量主管角色用户失败，bizType={}, bizNo={}]", bizType, bizNo, ex);
            return;
        }
        if (candidateUserIds == null || candidateUserIds.isEmpty()) {
            log.warn("[sendAuditTodoIfNeeded][质量主管角色无可通知用户，roleId={}, bizType={}, bizNo={}]",
                    QUALITY_MANAGER_ROLE_ID, bizType, bizNo);
            return;
        }

        List<AdminUserRespDTO> users;
        try {
            users = adminUserApi.getUserList(candidateUserIds);
        } catch (Exception ex) {
            log.warn("[sendAuditTodoIfNeeded][查询质量主管用户信息失败，roleId={}, bizType={}, bizNo={}]",
                    QUALITY_MANAGER_ROLE_ID, bizType, bizNo, ex);
            return;
        }
        if (users == null || users.isEmpty()) {
            log.warn("[sendAuditTodoIfNeeded][质量主管角色用户不存在，roleId={}, bizType={}, bizNo={}]",
                    QUALITY_MANAGER_ROLE_ID, bizType, bizNo);
            return;
        }

        Map<String, Object> templateParams = buildAuditTodoNotifyParams(bizType, bizNo, bizName, actualSubmitTime,
                auditTip);
        int successCount = 0;
        for (AdminUserRespDTO user : users) {
            if (user == null || !CommonStatusEnum.isEnable(user.getStatus())) {
                continue;
            }
            try {
                notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                        .setUserId(user.getId())
                        .setTemplateCode(AUDIT_TODO_NOTIFY_TEMPLATE_CODE)
                        .setTemplateParams(templateParams));
                successCount++;
            } catch (Exception ex) {
                log.warn("[sendAuditTodoIfNeeded][发送 QMS 审核待办失败，bizType={}, bizNo={}, userId={}]",
                        bizType, bizNo, user.getId(), ex);
            }
        }
        if (successCount <= 0 || auditNotifyTimeUpdater == null) {
            return;
        }
        try {
            auditNotifyTimeUpdater.accept(LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("[sendAuditTodoIfNeeded][更新审核待办通知去重标记失败，bizType={}, bizNo={}]", bizType, bizNo, ex);
        }
    }

    private Map<String, Object> buildAuditTodoNotifyParams(String bizType, String bizNo, String bizName,
                                                           LocalDateTime submitTime, String auditTip) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("bizType", defaultString(bizType));
        params.put("bizNo", defaultString(bizNo));
        params.put("bizName", defaultString(bizName));
        params.put("submitter", resolveCurrentUserName());
        params.put("submitTime", submitTime == null ? "" : submitTime.format(NOTIFY_TIME_FORMATTER));
        params.put("auditTip", defaultString(auditTip));
        return params;
    }

    private String resolveCurrentUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

}
