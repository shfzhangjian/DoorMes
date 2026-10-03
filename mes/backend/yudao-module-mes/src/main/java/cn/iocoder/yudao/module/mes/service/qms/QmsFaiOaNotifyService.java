package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;

/**
 * FAI 泛微 OA 通知服务。
 */
public interface QmsFaiOaNotifyService {

    void sendCreated(QmsFaiOrderDO order);

    void sendWaitingAudit(QmsFaiOrderDO order);

    void sendAuditPassed(QmsFaiOrderDO order);

    void sendAuditFailed(QmsFaiOrderDO order);

    void sendAuditReturned(QmsFaiOrderDO order);

}
