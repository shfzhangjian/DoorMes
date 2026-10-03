package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingAlignmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticePickItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticePickItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import java.util.List;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsFgShippingFqcAlignmentSnapshotTest {

    private static final long NOTICE_ID = 1001L;
    private static final long FQC_ID = 2001L;
    private static final long FIRST_NOTICE_ITEM_ID = 3001L;
    private static final long SECOND_NOTICE_ITEM_ID = 3002L;
    private static final long FIRST_PICK_ITEM_ID = 4001L;
    private static final long SECOND_PICK_ITEM_ID = 4002L;

    @InjectMocks
    private QmsFgShippingFqcServiceImpl service;
    @Mock
    private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Mock
    private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Mock
    private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Mock
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Mock
    private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;
    @Mock
    private QmsFqcItemMapper qmsFqcItemMapper;

    @BeforeEach
    void initMybatisTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, HcFgShippingNoticeItemDO.class);
        TableInfoHelper.initTableInfo(assistant, HcFgShippingNoticePickItemDO.class);
        TableInfoHelper.initTableInfo(assistant, QmsFqcOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, QmsFqcItemDO.class);
        TableInfoHelper.initTableInfo(assistant, QmsFqcShippingDetailDO.class);
    }

    @Test
    void shouldAllowSwapOfExistingAlignmentInOneSnapshot() {
        HcFgShippingNoticeDO notice = notice("INSPECTED");
        List<HcFgShippingNoticeItemDO> noticeItems = List.of(noticeItem(FIRST_NOTICE_ITEM_ID), noticeItem(SECOND_NOTICE_ITEM_ID));
        List<HcFgShippingNoticePickItemDO> pickItems = List.of(
                pickItem(FIRST_PICK_ITEM_ID, FIRST_NOTICE_ITEM_ID, "W26G001A"),
                pickItem(SECOND_PICK_ITEM_ID, SECOND_NOTICE_ITEM_ID, "W26G002A"));
        List<QmsFqcShippingDetailDO> details = List.of(
                shippingDetail(5001L, FIRST_PICK_ITEM_ID, FIRST_NOTICE_ITEM_ID),
                shippingDetail(5002L, SECOND_PICK_ITEM_ID, SECOND_NOTICE_ITEM_ID));
        mockEditableAlignmentContext(notice, noticeItems, pickItems, details);

        assertDoesNotThrow(() -> service.saveShippingAlignment(snapshot(
                detail(FIRST_NOTICE_ITEM_ID, SECOND_PICK_ITEM_ID),
                detail(SECOND_NOTICE_ITEM_ID, FIRST_PICK_ITEM_ID))));

        verify(qmsFqcShippingDetailMapper, times(4)).update(eq(null), any());
        verify(hcFgShippingNoticeItemMapper, atLeastOnce()).update(eq(null), any());
        verify(hcFgShippingNoticePickItemMapper, atLeastOnce()).update(eq(null), any());
    }

    @Test
    void shouldRejectAlignmentAdjustmentAfterPackagingStarts() {
        when(hcFgShippingNoticeMapper.selectByIdForUpdate(NOTICE_ID)).thenReturn(notice("PACKAGED"));

        assertThrows(ServiceException.class, () -> service.saveShippingAlignment(snapshot(
                detail(FIRST_NOTICE_ITEM_ID, FIRST_PICK_ITEM_ID))));

        verify(qmsFqcOrderMapper, never()).selectListBySource(any(), any());
    }

    private void mockEditableAlignmentContext(HcFgShippingNoticeDO notice,
                                              List<HcFgShippingNoticeItemDO> noticeItems,
                                              List<HcFgShippingNoticePickItemDO> pickItems,
                                              List<QmsFqcShippingDetailDO> details) {
        QmsFqcOrderDO fqcOrder = new QmsFqcOrderDO();
        fqcOrder.setId(FQC_ID);
        fqcOrder.setStatus("COMPLETED");
        when(hcFgShippingNoticeMapper.selectByIdForUpdate(NOTICE_ID)).thenReturn(notice);
        when(hcFgShippingNoticeMapper.selectById(NOTICE_ID)).thenReturn(notice);
        when(qmsFqcOrderMapper.selectListBySource("FG_SHIPPING_FQC", NOTICE_ID)).thenReturn(List.of(fqcOrder));
        when(qmsFqcShippingDetailMapper.selectListByFqcIds(any())).thenReturn(details);
        when(hcFgShippingNoticeItemMapper.selectListByNoticeId(NOTICE_ID)).thenReturn(noticeItems);
        when(hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(NOTICE_ID)).thenReturn(pickItems);
        when(qmsFqcShippingDetailMapper.selectCount(any())).thenReturn(2L);
    }

    private HcFgShippingNoticeDO notice(String status) {
        HcFgShippingNoticeDO notice = new HcFgShippingNoticeDO();
        notice.setId(NOTICE_ID);
        notice.setNoticeNo("FG-TEST-001");
        notice.setNoticeStatus(status);
        return notice;
    }

    private HcFgShippingNoticeItemDO noticeItem(long id) {
        HcFgShippingNoticeItemDO item = new HcFgShippingNoticeItemDO();
        item.setId(id);
        item.setCustomerProductBatchNo("CUSTOMER-" + id);
        return item;
    }

    private HcFgShippingNoticePickItemDO pickItem(long id, long sourceNoticeItemId, String actualSliceBatchNo) {
        HcFgShippingNoticePickItemDO item = new HcFgShippingNoticePickItemDO();
        item.setId(id);
        item.setLockStatus("INSPECTED");
        item.setSourceNoticeItemId(sourceNoticeItemId);
        item.setActualSliceBatchNo(actualSliceBatchNo);
        return item;
    }

    private QmsFqcShippingDetailDO shippingDetail(long id, long pickItemId, long noticeItemId) {
        QmsFqcShippingDetailDO detail = new QmsFqcShippingDetailDO();
        detail.setId(id);
        detail.setFqcId(FQC_ID);
        detail.setShippingPickItemId(pickItemId);
        detail.setShippingNoticeItemId(noticeItemId);
        detail.setRowJudgment("OK");
        detail.setAlignmentStatus("ALIGNED");
        return detail;
    }

    private QmsFgShippingAlignmentSaveReqVO snapshot(QmsFgShippingAlignmentSaveReqVO.Detail... details) {
        QmsFgShippingAlignmentSaveReqVO request = new QmsFgShippingAlignmentSaveReqVO();
        request.setShippingNoticeId(NOTICE_ID);
        request.setDetails(List.of(details));
        return request;
    }

    private QmsFgShippingAlignmentSaveReqVO.Detail detail(long noticeItemId, long pickItemId) {
        QmsFgShippingAlignmentSaveReqVO.Detail detail = new QmsFgShippingAlignmentSaveReqVO.Detail();
        detail.setShippingNoticeItemId(noticeItemId);
        detail.setShippingPickItemId(pickItemId);
        return detail;
    }
}
