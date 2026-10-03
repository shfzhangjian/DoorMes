package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.framework.config.MesFaiOaNotifyProperties;
import cn.iocoder.yudao.module.oa.api.ecology.OaEcologyMessageApi;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyMessageSendReqDTO;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyMessageSendRespDTO;
import cn.iocoder.yudao.module.oa.api.ecology.dto.OaEcologyUserMatchRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * FAI 泛微 OA 通知服务实现。
 */
@Service
@Slf4j
public class QmsFaiOaNotifyServiceImpl implements QmsFaiOaNotifyService {

    private static final String SOURCE_MODULE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String OA_MAPPING_SYNC_STATUS_AUTO = "AUTO";

    private enum NotifyEvent {
        CREATED,
        WAITING_AUDIT,
        AUDIT_PASS,
        AUDIT_FAIL,
        AUDIT_RETURN
    }

    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private OaEcologyMessageApi oaEcologyMessageApi;
    @Resource
    private MesFaiOaNotifyProperties properties;
    @Resource(name = "applicationTaskExecutor")
    private AsyncTaskExecutor taskExecutor;

    @Override
    public void sendCreated(QmsFaiOrderDO order) {
        schedule(order, NotifyEvent.CREATED);
    }

    @Override
    public void sendWaitingAudit(QmsFaiOrderDO order) {
        schedule(order, NotifyEvent.WAITING_AUDIT);
    }

    @Override
    public void sendAuditPassed(QmsFaiOrderDO order) {
        schedule(order, NotifyEvent.AUDIT_PASS);
    }

    @Override
    public void sendAuditFailed(QmsFaiOrderDO order) {
        schedule(order, NotifyEvent.AUDIT_FAIL);
    }

    @Override
    public void sendAuditReturned(QmsFaiOrderDO order) {
        schedule(order, NotifyEvent.AUDIT_RETURN);
    }

    private void schedule(QmsFaiOrderDO order, NotifyEvent event) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || order == null || order.getId() == null) {
            return;
        }
        Long faiId = order.getId();
        Long tenantId = order.getTenantId() == null ? TenantContextHolder.getTenantId() : order.getTenantId();
        Runnable submitTask = () -> taskExecutor.execute(() -> executeWithTenant(faiId, tenantId, event));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submitSafely(submitTask, faiId, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submitSafely(submitTask, faiId, event);
            }
        });
    }

    private void submitSafely(Runnable task, Long faiId, NotifyEvent event) {
        try {
            task.run();
        } catch (Exception ex) {
            log.warn("[submitSafely][FAI OA 通知异步任务提交失败，faiId={}, event={}]", faiId, event, ex);
        }
    }

    private void executeWithTenant(Long faiId, Long tenantId, NotifyEvent event) {
        try {
            if (tenantId == null) {
                doSend(faiId, event);
                return;
            }
            TenantUtils.execute(tenantId, () -> doSend(faiId, event));
        } catch (Exception ex) {
            log.warn("[executeWithTenant][FAI OA 通知发送失败，faiId={}, event={}]", faiId, event, ex);
        }
    }

    private void doSend(Long faiId, NotifyEvent event) {
        QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(faiId);
        if (order == null) {
            log.warn("[doSend][FAI OA 通知未找到单据，faiId={}, event={}]", faiId, event);
            return;
        }
        String text = buildMessageText(order, event);
        List<AdminUserRespDTO> receivers = resolveReceivers(order, event);
        if (CollUtil.isEmpty(receivers)) {
            log.warn("[doSend][FAI OA 通知无可用接收人，faiNo={}, event={}]", order.getFaiNo(), event);
            return;
        }
        for (AdminUserRespDTO receiver : receivers) {
            sendOne(order, event, text, receiver);
        }
    }

    private List<AdminUserRespDTO> resolveReceivers(QmsFaiOrderDO order, NotifyEvent event) {
        return switch (event) {
            case CREATED -> resolveRoleReceivers(order, event, properties.getNewOrderRoleId(), "新检验单通知");
            case WAITING_AUDIT -> resolveRoleReceivers(order, event, properties.getAuditRoleId(), "检验审批");
            case AUDIT_PASS, AUDIT_FAIL -> resolveSingleReceiver(order.getSubmitterId(), parseUserId(order.getCreator()),
                    "送检人", order, event);
            case AUDIT_RETURN -> resolveSingleReceiver(order.getOperatorId(), null, "检验人", order, event);
        };
    }

    private List<AdminUserRespDTO> resolveRoleReceivers(QmsFaiOrderDO order, NotifyEvent event, Long roleId,
                                                        String roleName) {
        if (roleId == null) {
            log.warn("[resolveRoleReceivers][FAI OA {}角色未配置，faiNo={}, event={}]", roleName, order.getFaiNo(), event);
            return Collections.emptyList();
        }
        Set<Long> userIds;
        try {
            userIds = permissionApi.getUserRoleIdListByRoleIds(Collections.singleton(roleId));
        } catch (Exception ex) {
            log.warn("[resolveRoleReceivers][查询 FAI OA {}角色用户失败，roleId={}, faiNo={}, event={}]",
                    roleName, roleId, order.getFaiNo(), event, ex);
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return adminUserApi.getUserList(userIds).stream()
                .filter(this::isEnabledUser)
                .toList();
    }

    private List<AdminUserRespDTO> resolveSingleReceiver(Long primaryUserId, Long fallbackUserId, String receiverName,
                                                          QmsFaiOrderDO order, NotifyEvent event) {
        Long userId = primaryUserId == null ? fallbackUserId : primaryUserId;
        if (userId == null) {
            log.warn("[resolveSingleReceiver][FAI OA {}为空，faiNo={}, event={}]", receiverName, order.getFaiNo(), event);
            return Collections.emptyList();
        }
        return adminUserApi.getUserList(Collections.singleton(userId)).stream()
                .filter(this::isEnabledUser)
                .toList();
    }

    private void sendOne(QmsFaiOrderDO order, NotifyEvent event, String text, AdminUserRespDTO receiver) {
        receiver = resolveOaReceiver(order, event, receiver);
        if (receiver == null) {
            return;
        }
        try {
            OaEcologyMessageSendReqDTO reqDTO = new OaEcologyMessageSendReqDTO();
            reqDTO.setTitle("MES检验通知");
            reqDTO.setText(text);
            reqDTO.setReceiverEmployeeId(receiver.getOaEcologyUserId());
            reqDTO.setReceiverTenantKey(receiver.getOaEcologyTenantKey());
            reqDTO.setReceiverWorkCode(receiver.getOaEcologyWorkCode());
            reqDTO.setReceiverName(firstNotBlank(receiver.getNickname(), receiver.getUsername(), receiver.getMobile()));
            reqDTO.setEntityId("MES-FAI-" + order.getId() + "-" + event.name());
            reqDTO.setEntityName(firstNotBlank(order.getFaiNo(), resolveInspectionType(order)));
            reqDTO.setPcUrl(buildFaiUrl(order));
            reqDTO.setH5Url(buildFaiUrl(order));
            OaEcologyMessageSendRespDTO respDTO = oaEcologyMessageApi.sendMessage(reqDTO);
            if (!Boolean.TRUE.equals(respDTO.getSuccess())) {
                log.warn("[sendOne][FAI OA 通知返回失败，faiNo={}, event={}, userId={}, code={}, message={}]",
                        order.getFaiNo(), event, receiver.getId(), respDTO.getCode(), respDTO.getMessage());
            }
        } catch (Exception ex) {
            log.warn("[sendOne][FAI OA 通知发送异常，faiNo={}, event={}, userId={}]",
                    order.getFaiNo(), event, receiver.getId(), ex);
        }
    }

    private AdminUserRespDTO resolveOaReceiver(QmsFaiOrderDO order, NotifyEvent event, AdminUserRespDTO receiver) {
        if (receiver == null || !isEnabledUser(receiver)) {
            return null;
        }
        if (hasOaMapping(receiver)) {
            return receiver;
        }
        try {
            OaEcologyUserMatchRespDTO oaUser = oaEcologyMessageApi.findEmployeeBySystemUser(receiver.getUsername(),
                    receiver.getNickname(), receiver.getMobile());
            if (oaUser == null || !hasOaMapping(oaUser)) {
                log.warn("[resolveOaReceiver][FAI OA 未匹配到系统用户对应 OA 人员，faiNo={}, event={}, userId={}, username={}, nickname={}]",
                        order.getFaiNo(), event, receiver.getId(), receiver.getUsername(), receiver.getNickname());
                return null;
            }
            receiver.setOaEcologyUserId(oaUser.getEmployeeId());
            receiver.setOaEcologyTenantKey(oaUser.getTenantKey());
            receiver.setOaEcologyWorkCode(oaUser.getWorkCode());
            adminUserApi.updateOaEcologyUserMapping(receiver.getId(), oaUser.getEmployeeId(),
                    oaUser.getTenantKey(), oaUser.getWorkCode(), OA_MAPPING_SYNC_STATUS_AUTO);
            return receiver;
        } catch (Exception ex) {
            log.warn("[resolveOaReceiver][FAI OA 自动同步用户映射失败，faiNo={}, event={}, userId={}]",
                    order.getFaiNo(), event, receiver.getId(), ex);
            return null;
        }
    }

    private boolean hasOaMapping(AdminUserRespDTO receiver) {
        return StringUtils.hasText(receiver.getOaEcologyTenantKey())
                && (StringUtils.hasText(receiver.getOaEcologyUserId())
                || StringUtils.hasText(receiver.getOaEcologyWorkCode()));
    }

    private boolean hasOaMapping(OaEcologyUserMatchRespDTO oaUser) {
        return StringUtils.hasText(oaUser.getTenantKey())
                && (StringUtils.hasText(oaUser.getEmployeeId())
                || StringUtils.hasText(oaUser.getWorkCode()));
    }

    private boolean isEnabledUser(AdminUserRespDTO user) {
        return user != null && CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus());
    }

    private String buildMessageText(QmsFaiOrderDO order, NotifyEvent event) {
        String type = resolveInspectionType(order);
        String batch = firstNotBlank(order.getProductBatchNo(), order.getGluePlateBatchNo(), order.getWorkOrderNo(), "-");
        String model = firstNotBlank(order.getProductModel(), order.getGlueBoardModel(), order.getMaterialName(),
                order.getMaterialCode(), "-");
        String faiNo = firstNotBlank(order.getFaiNo(), "-");
        return switch (event) {
            case CREATED -> String.format("%s，%s，%s，有新检验单“%s”，请注意检验。", type, batch, model, faiNo);
            case WAITING_AUDIT -> String.format("%s，%s，%s，检验单“%s”已完成检验，请尽快审批。", type, batch, model, faiNo);
            case AUDIT_PASS -> String.format("%s，%s，%s已通过，放行。", type, batch, model);
            case AUDIT_FAIL -> String.format("%s，%s，%s，不合格，请注意尽快与检验室联系。", type, batch, model);
            case AUDIT_RETURN -> String.format("%s，%s，%s，“%s”没通过审批，需要重申，请尽快处理。",
                    type, batch, model, faiNo);
        };
    }

    private String resolveInspectionType(QmsFaiOrderDO order) {
        if (SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule())) {
            return "胶板检验";
        }
        return "首件检验";
    }

    private String buildFaiUrl(QmsFaiOrderDO order) {
        String pageUrl = properties.getFaiPageUrl();
        if (!StringUtils.hasText(pageUrl)) {
            return null;
        }
        return pageUrl + (pageUrl.contains("?") ? "&" : "?") + "id=" + order.getId();
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Long parseUserId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
