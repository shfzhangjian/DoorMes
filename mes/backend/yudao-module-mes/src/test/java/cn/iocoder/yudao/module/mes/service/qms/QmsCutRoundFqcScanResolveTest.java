package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcScanRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcScanRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSubmissionDetailMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsCutRoundFqcScanResolveTest {

    @InjectMocks
    private QmsCutRoundFqcServiceImpl cutRoundFqcService;

    @Mock
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Mock
    private QmsFqcSubmissionDetailMapper qmsFqcSubmissionDetailMapper;
    @Mock
    private QmsFqcScanRecordMapper qmsFqcScanRecordMapper;

    @Test
    void shouldRequireCandidateSelectionForMultipleCutRoundPiecesAndExcludeOtherFqcSources() {
        String productionBatchNo = "W26H001AP001A";
        when(qmsFqcSubmissionDetailMapper.selectListByProductionBatchNo(productionBatchNo))
                .thenReturn(List.of(
                        detail(101L, 1001L, productionBatchNo),
                        detail(102L, 1002L, productionBatchNo),
                        detail(201L, 2001L, productionBatchNo)));
        when(qmsFqcOrderMapper.selectList(any())).thenReturn(List.of(
                order(101L, "CUT_ROUND_FQC"),
                order(102L, "CUT_ROUND_FQC"),
                order(201L, "FG_SHIPPING_FQC")));

        QmsCutRoundFqcScanRespVO response = cutRoundFqcService.resolveScan(scanRequest(productionBatchNo));

        assertEquals("MATCHED_MULTIPLE", response.getMatchResult());
        assertEquals(2, response.getCandidateCount());
        assertEquals(2, response.getCandidates().size());
        assertTrue(response.getCandidates().stream().allMatch(item -> item.getFqcId() < 200L));
        verify(qmsFqcScanRecordMapper).insert(any(QmsFqcScanRecordDO.class));
    }

    private QmsFqcScanReqVO scanRequest(String scanCode) {
        QmsFqcScanReqVO request = new QmsFqcScanReqVO();
        request.setScanCode(scanCode);
        request.setClientType("PC");
        request.setScanScene("LEDGER_TOOLBAR");
        return request;
    }

    private QmsFqcOrderDO order(Long id, String sourceModule) {
        return QmsFqcOrderDO.builder()
                .id(id)
                .fqcNo("FQC-" + id)
                .sourceModule(sourceModule)
                .status("INSPECTING")
                .build();
    }

    private QmsFqcSubmissionDetailDO detail(Long fqcId, Long id, String productionBatchNo) {
        return QmsFqcSubmissionDetailDO.builder()
                .id(id)
                .fqcId(fqcId)
                .productionBatchNo(productionBatchNo)
                .build();
    }
}
