package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageMapper;
import cn.iocoder.yudao.module.mes.service.hc.finishedglueboardmap.HcFinishedGlueBoardMapService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcProcessReportGlueBoardMatchTest {
    @InjectMocks private HcProcessReportServiceImpl service;
    @Mock private HcAdhesiveGlueBoardUsageMapper usageMapper;
    @Mock private HcAdhesiveGlueBoardStockMapper stockMapper;
    @Mock private HcFinishedGlueBoardMapService mapService;

    private void usage(String process, String status) {
        when(usageMapper.selectOne(any())).thenReturn(HcAdhesiveGlueBoardUsageDO.builder()
                .id(1L).operationCode(process).usageStatus(status).glueBoardStockId(2L)
                .glueBoardBatchNo("BATCH").glueBoardMaterialCode("01.02.00020").build());
    }
    private void stock() {
        when(stockMapper.selectOne(any())).thenReturn(HcAdhesiveGlueBoardStockDO.builder()
                .id(2L).glueBoardBatchNo("BATCH").glueBoardModel("SDK").glueBoardMaterialCode("01.02.00020").build());
    }
    private void mapping(String process) {
        when(mapService.getMatchedItems("W33P0300", process)).thenReturn(List.of(
                HcFinishedGlueBoardMapItemDO.builder().glueBoardModel("SDK").glueBoardMaterialCode("01.02.00020").build()));
    }
    @Test void missingUsageCannotBypass() {
        assertThrows(ServiceException.class, () -> service.validateStrictGlueBoardMatch("ADHESIVE2", "W33P0300", null, null, null, null, true));
        verifyNoInteractions(stockMapper, mapService);
    }
    @Test void actualDatabaseStockPasses() {
        usage("ADHESIVE2", "ACTIVE"); stock(); mapping("ADHESIVE2");
        assertNotNull(service.validateStrictGlueBoardMatch("ADHESIVE2", "W33P0300", 1L, "01.02.00020", "BATCH", "SDK", true));
    }
    @Test void forgedRequestMaterialRejected() {
        usage("ADHESIVE2", "ACTIVE"); stock();
        assertThrows(ServiceException.class, () -> service.validateStrictGlueBoardMatch("ADHESIVE2", "W33P0300", 1L, "01.02.00045", "BATCH", "SDK", true));
        verifyNoInteractions(mapService);
    }
    @Test void forgedModelRejected() {
        usage("ADHESIVE2", "ACTIVE"); stock();
        assertThrows(ServiceException.class, () -> service.validateStrictGlueBoardMatch("ADHESIVE2", "W33P0300", 1L, "01.02.00020", "BATCH", "W250", true));
    }
    @Test void closedUsageCannotBeUsedForNewProduction() {
        usage("ADHESIVE2", "RETURNED");
        assertThrows(ServiceException.class, () -> service.validateStrictGlueBoardMatch("ADHESIVE2", "W33P0300", 1L, "01.02.00020", "BATCH", "SDK", true));
        verifyNoInteractions(stockMapper);
    }
    @Test void historicalAdhesiveOneUsageMayBeReturned() {
        usage("ADHESIVE", "RETURNED"); stock(); mapping("ADHESIVE1");
        assertNotNull(service.validateStrictGlueBoardMatch("ADHESIVE1", "W33P0300", 1L, "01.02.00020", "BATCH", null, false));
    }
    @Test void anotherProcessRejected() {
        usage("ADHESIVE2", "ACTIVE");
        assertThrows(ServiceException.class, () -> service.validateStrictGlueBoardMatch("ADHESIVE1", "W33P0300", 1L, "01.02.00020", "BATCH", null, false));
    }
}
