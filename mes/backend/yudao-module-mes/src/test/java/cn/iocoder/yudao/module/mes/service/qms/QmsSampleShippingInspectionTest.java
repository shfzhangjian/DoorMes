package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.*;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QmsSampleShippingInspectionTest {
    @InjectMocks private QmsFgShippingFqcServiceImpl service;
    @Mock private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Mock private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Mock private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Mock private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;
    @Mock private QmsFqcOrderMapper qmsFqcOrderMapper;
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 9, 9, 0);

    @BeforeEach void tableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "sample-inspection");
        TableInfoHelper.initTableInfo(assistant, HcFgShippingNoticeItemDO.class);
        TableInfoHelper.initTableInfo(assistant, HcFgShippingNoticePickItemDO.class);
        TableInfoHelper.initTableInfo(assistant, QmsFqcShippingDetailDO.class);
    }
    @Test void auditedSampleUsesItsActualPickSourceWithoutCustomerBatchAlignment() throws Exception {
        context("SAMPLE");
        writeBack(List.of(detail("OK")));
        var itemCapture = ArgumentCaptor.forClass(HcFgShippingNoticeItemDO.class);
        verify(hcFgShippingNoticeItemMapper).updateById(itemCapture.capture());
        assertEquals(3L, itemCapture.getValue().getId());
        assertEquals(5L, itemCapture.getValue().getActualFinishedStockId());
        assertEquals("ACTUAL-1", itemCapture.getValue().getActualSliceBatchNo());
        assertEquals("OK", itemCapture.getValue().getShippingInspectionResult());
        assertEquals("INSPECTED", itemCapture.getValue().getLockStatus());
        @SuppressWarnings("rawtypes") var wrapper = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(qmsFqcShippingDetailMapper).update(isNull(), wrapper.capture());
        wrapper.getValue().getSqlSet();
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue("NOT_REQUIRED"));
        assertFalse(wrapper.getValue().getParamNameValuePairs().containsValue("ALIGNED"));
    }
    @Test void ngNeverWritesQualifiedExecutionRow() throws Exception {
        context("SAMPLE");
        writeBack(List.of(detail("NG")));
        verify(hcFgShippingNoticeItemMapper, never()).updateById(any(HcFgShippingNoticeItemDO.class));
        verify(hcFgShippingNoticePickItemMapper, never()).updateById(any(HcFgShippingNoticePickItemDO.class));
        verify(qmsFqcShippingDetailMapper).update(isNull(), any());
    }
    @Test void changedActualSliceOrDuplicateExecutionBindingIsRejected() {
        context("SAMPLE");
        var wrong = detail("OK"); wrong.setActualSliceBatchNo("WRONG");
        assertThrows(RuntimeException.class, () -> writeBack(List.of(wrong)));
        var duplicate = detail("OK"); duplicate.setId(99L);
        assertThrows(RuntimeException.class, () -> writeBack(List.of(detail("OK"), duplicate)));
    }
    @Test void massAuditDoesNotEnterSampleWriteBack() throws Exception {
        when(hcFgShippingNoticeMapper.selectById(1L)).thenReturn(notice("MASS"));
        writeBack(List.of(detail("OK")));
        verifyNoInteractions(hcFgShippingNoticePickItemMapper, hcFgShippingNoticeItemMapper, qmsFqcShippingDetailMapper);
    }
    @Test void sampleCannotBeSubmittedToManualAlignmentApi() {
        when(hcFgShippingNoticeMapper.selectByIdForUpdate(1L)).thenReturn(notice("SAMPLE"));
        var req = new QmsFgShippingAlignmentSaveReqVO(); req.setShippingNoticeId(1L);
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.saveShippingAlignment(req));
        assertTrue(error.getMessage().contains("样品无需批号对齐"));
        verifyNoInteractions(qmsFqcOrderMapper, hcFgShippingNoticeItemMapper, qmsFqcShippingDetailMapper);
    }
    private HcFgShippingNoticeDO notice(String type) {
        return HcFgShippingNoticeDO.builder().id(1L).productType(type).noticeStatus("INSPECTED").build();
    }
    private void context(String type) {
        when(hcFgShippingNoticeMapper.selectById(1L)).thenReturn(notice(type));
        when(hcFgShippingNoticeItemMapper.selectListByNoticeId(1L)).thenReturn(List.of(
                HcFgShippingNoticeItemDO.builder().id(3L).noticeId(1L).lockedQty(1).build()));
        when(hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(1L)).thenReturn(List.of(
                HcFgShippingNoticePickItemDO.builder().id(4L).noticeId(1L).sourceNoticeItemId(3L)
                        .finishedStockId(5L).actualSliceBatchNo("ACTUAL-1").actualShipQty(1)
                        .lockStatus("INSPECTED").build()));
    }
    private QmsFqcShippingDetailDO detail(String result) {
        return QmsFqcShippingDetailDO.builder().id(6L).fqcId(2L).shippingNoticeId(1L)
                .shippingPickItemId(4L).actualSliceBatchNo("ACTUAL-1")
                .rowJudgment(result).inspectorName("测试").inspectionTime(now).build();
    }
    private void writeBack(List<QmsFqcShippingDetailDO> details) throws Exception {
        Method method = QmsFgShippingFqcServiceImpl.class.getDeclaredMethod("writeBackSampleShippingInspection",
                QmsFqcOrderDO.class, List.class, LocalDateTime.class);
        method.setAccessible(true);
        var order = QmsFqcOrderDO.builder().id(2L).sourceReportId(1L).fqcNo("FQC-SAMPLE").build();
        try { method.invoke(service, order, details, now); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }
}
