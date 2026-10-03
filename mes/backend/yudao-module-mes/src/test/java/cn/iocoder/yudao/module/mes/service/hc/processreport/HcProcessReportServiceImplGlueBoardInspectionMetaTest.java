package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplGlueBoardInspectionMetaTest {

    private static final LocalDateTime REPORT_TIME = LocalDateTime.of(2026, 7, 31, 10, 34, 36);

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;

    @Test
    void shouldKeepReportNormalWhenLatestCompletedGlueBoardInspectionIsOk() throws Exception {
        HcAdhesiveGlueBoardUsageDO usage = HcAdhesiveGlueBoardUsageDO.builder().glueBoardStockId(55L).build();
        when(qmsFaiOrderMapper.selectLatestCompletedGlueBoardFaiBefore(55L, REPORT_TIME))
                .thenReturn(QmsFaiOrderDO.builder().faiNo("FAI-20260730-001").judgment("OK").build());

        Object result = resolveMeta(usage, REPORT_TIME);

        assertFalse(isAbnormal(result));
        verify(qmsFaiOrderMapper).selectLatestCompletedGlueBoardFaiBefore(55L, REPORT_TIME);
    }

    @Test
    void shouldLockReportWhenLatestCompletedGlueBoardInspectionIsNg() throws Exception {
        HcAdhesiveGlueBoardUsageDO usage = HcAdhesiveGlueBoardUsageDO.builder().glueBoardStockId(55L).build();
        when(qmsFaiOrderMapper.selectLatestCompletedGlueBoardFaiBefore(55L, REPORT_TIME))
                .thenReturn(QmsFaiOrderDO.builder().faiNo("FAI-20260731-001").judgment("NG").build());

        Object result = resolveMeta(usage, REPORT_TIME);

        assertTrue(isAbnormal(result));
    }

    private Object resolveMeta(HcAdhesiveGlueBoardUsageDO usage, LocalDateTime reportTime) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "resolveGlueBoardInspectionAbnormalMeta", HcAdhesiveGlueBoardUsageDO.class, LocalDateTime.class);
        method.setAccessible(true);
        return method.invoke(service, usage, reportTime);
    }

    private boolean isAbnormal(Object result) throws Exception {
        Field field = result.getClass().getDeclaredField("abnormal");
        field.setAccessible(true);
        return field.getBoolean(result);
    }
}
