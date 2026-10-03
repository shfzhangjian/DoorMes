package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetFaiRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcPickQualificationService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplFaiReasonTest {
    @InjectMocks private HcProcessReportServiceImpl service;
    @Mock private HcProcessReportMapper hcProcessReportMapper;
    @Mock private QmsNcPickQualificationService pickQualificationService;

    @BeforeEach
    void initTable() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), HcProcessReportDO.class);
    }

    @Test
    void pendingFaiMustNotInheritPreviousNgReasonInSummaryOrHistory() throws Exception {
        QmsFaiOrderDO pending = order(1196L, "PENDING", null);
        QmsFaiOrderDO rejected = order(1195L, "REJECTED", "品质审核判定不合格");
        HcWetFaiRespVO result = summary(pending, report(), List.of(rejected, pending));
        assertEquals("PENDING", result.getFaiStatus());
        assertNull(result.getFaiRejectReason());
        assertEquals("品质审核判定不合格", result.getRecords().get(0).getFaiRejectReason());
        assertNull(result.getRecords().get(1).getFaiRejectReason());
    }

    @Test
    void currentRejectedOrReturnedFaiKeepsItsOwnReason() throws Exception {
        for (String status : List.of("REJECTED", "INSPECTING")) {
            QmsFaiOrderDO order = order(1196L, status, "本单实际审核说明");
            HcWetFaiRespVO result = summary(order, report(), List.of(order));
            assertEquals("本单实际审核说明", result.getFaiRejectReason());
            assertEquals("本单实际审核说明", result.getRecords().get(0).getFaiRejectReason());
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void syncingPendingFaiExplicitlyWritesNullReasonAndUpdatesAssociationTogether() throws Exception {
        HcProcessReportDO report = report();
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod("syncWetReportFaiSummary", HcProcessReportDO.class, QmsFaiOrderDO.class);
        method.setAccessible(true);
        method.invoke(service, report, order(1196L, "PENDING", null));
        ArgumentCaptor<HcProcessReportDO> entity = ArgumentCaptor.forClass(HcProcessReportDO.class);
        ArgumentCaptor<LambdaUpdateWrapper<HcProcessReportDO>> wrapper = ArgumentCaptor.forClass((Class) LambdaUpdateWrapper.class);
        verify(hcProcessReportMapper).update(entity.capture(), wrapper.capture());
        assertEquals(1196L, entity.getValue().getFaiId());
        assertEquals("PENDING", entity.getValue().getFaiStatus());
        assertTrue(wrapper.getValue().getSqlSet().contains("fai_reject_reason="));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue(null));
        assertTrue(wrapper.getValue().getSqlSegment().contains("id ="));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue(998L));
        assertNull(report.getFaiRejectReason());
        assertEquals(1196L, report.getFaiId());
    }

    private HcWetFaiRespVO summary(QmsFaiOrderDO order, HcProcessReportDO report, List<QmsFaiOrderDO> history) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod("buildWetFaiResp", QmsFaiOrderDO.class, HcProcessReportDO.class, List.class);
        method.setAccessible(true);
        return (HcWetFaiRespVO) method.invoke(service, order, report, history);
    }

    private QmsFaiOrderDO order(Long id, String status, String reason) {
        QmsFaiOrderDO order = new QmsFaiOrderDO();
        order.setId(id);
        order.setStatus(status);
        order.setJudgment("REJECTED".equals(status) ? "NG" : "PENDING");
        order.setLastReturnReason(reason);
        order.setProductBatchNo("W26J169AQ045A");
        return order;
    }

    private HcProcessReportDO report() {
        HcProcessReportDO report = new HcProcessReportDO();
        report.setId(998L);
        report.setFaiRejectReason("品质审核判定不合格");
        return report;
    }
}
