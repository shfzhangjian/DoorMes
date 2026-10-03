package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsSideFqcAuditValidationTest {

    @InjectMocks
    private QmsCutRoundFqcServiceImpl cutRoundFqcService;
    @InjectMocks
    private QmsFgShippingFqcServiceImpl fgShippingFqcService;
    @Mock
    private QmsFqcOrderMapper qmsFqcOrderMapper;

    @Test
    void shouldRejectUnsupportedCutRoundAuditResult() {
        when(qmsFqcOrderMapper.selectById(101L)).thenReturn(waitingQaOrder(101L, "CUT_ROUND_FQC"));

        assertThrows(ServiceException.class, () -> cutRoundFqcService.audit(auditRequest(101L, "INVALID", null)));
    }

    @Test
    void shouldRequireCutRoundRejectReason() {
        when(qmsFqcOrderMapper.selectById(102L)).thenReturn(waitingQaOrder(102L, "CUT_ROUND_FQC"));

        assertThrows(ServiceException.class, () -> cutRoundFqcService.audit(auditRequest(102L, "REJECT", " ")));
    }

    @Test
    void shouldRejectUnsupportedFgShippingAuditResult() {
        when(qmsFqcOrderMapper.selectById(201L)).thenReturn(waitingQaOrder(201L, "FG_SHIPPING_FQC"));

        assertThrows(ServiceException.class, () -> fgShippingFqcService.audit(auditRequest(201L, "INVALID", null)));
    }

    @Test
    void shouldRequireFgShippingRejectReason() {
        when(qmsFqcOrderMapper.selectById(202L)).thenReturn(waitingQaOrder(202L, "FG_SHIPPING_FQC"));

        assertThrows(ServiceException.class, () -> fgShippingFqcService.audit(auditRequest(202L, "REJECT", " ")));
    }

    private QmsFqcOrderDO waitingQaOrder(Long id, String sourceModule) {
        return QmsFqcOrderDO.builder()
                .id(id)
                .sourceModule(sourceModule)
                .status("WAITING_QA")
                .build();
    }

    private QmsFqcAuditReqVO auditRequest(Long id, String auditResult, String rejectReason) {
        QmsFqcAuditReqVO request = new QmsFqcAuditReqVO();
        request.setId(id);
        request.setAuditResult(auditResult);
        request.setRejectReason(rejectReason);
        return request;
    }
}
